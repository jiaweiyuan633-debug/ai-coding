package com.aicoding.rpc;

import com.aicoding.api.AppRpcService;
import com.aicoding.api.dto.AppDTO;
import com.aicoding.api.dto.AppMetaDTO;
import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.model.entity.App;
import com.aicoding.service.AppVersionService;
import com.aicoding.service.CoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 应用服务 Dubbo Provider：供 ai-service 远程调用
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class AppRpcServiceImpl implements AppRpcService {

    private final com.aicoding.service.AppService appService;
    private final AppVersionService appVersionService;
    private final CoverService coverService;

    @Override
    public AppDTO getApp(long appId) {
        App app = appService.getAppById(appId);
        AppDTO dto = new AppDTO();
        dto.setId(app.getId());
        dto.setAppName(app.getAppName());
        dto.setCover(app.getCover());
        dto.setInitPrompt(app.getInitPrompt());
        dto.setCodeGenType(app.getCodeGenType());
        dto.setDeployKey(app.getDeployKey());
        dto.setUserId(app.getUserId());
        return dto;
    }

    @Override
    public boolean isOwner(long appId, long userId) {
        App app = appService.getAppById(appId);
        return userId == app.getUserId();
    }

    @Override
    public void saveGeneratedMeta(AppMetaDTO meta) {
        App app = appService.getAppById(meta.getId());
        if (meta.getAppName() != null && !meta.getAppName().isBlank()) {
            app.setAppName(meta.getAppName());
        }
        if (meta.getCodeGenType() != null && !meta.getCodeGenType().isBlank()) {
            app.setCodeGenType(meta.getCodeGenType());
        }
        if (meta.getCover() != null && !meta.getCover().isBlank()) {
            app.setCover(meta.getCover());
        } else if (app.getCover() == null || app.getCover().isBlank()) {
            coverService.generateCover(app.getId(), app.getAppName());
            app.setCover("/api/cover/" + app.getId());
        }
        appService.updateGeneratedMeta(app);
    }

    @Override
    public int createVersionSnapshot(long appId, long userId, String message, String genType) {
        return appVersionService.createSnapshot(appId, userId, message, genType).getVersion();
    }
}

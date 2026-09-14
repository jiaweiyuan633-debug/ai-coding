package com.aicoding.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import com.aicoding.core.ai.WorkspaceUtils;
import com.aicoding.mapper.AppVersionMapper;
import com.aicoding.model.entity.AppVersion;
import com.aicoding.service.AppVersionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 版本时光机服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private final AppVersionMapper appVersionMapper;
    private final WorkspaceUtils workspaceUtils;

    @Override
    public AppVersion createSnapshot(long appId, long userId, String message, String genType) {
        // 组装快照：文件路径 -> 内容
        JSONObject snapshotJson = new JSONObject();
        for (String path : workspaceUtils.listFiles(appId)) {
            String content = workspaceUtils.readFile(appId, path);
            if (content != null) {
                snapshotJson.set(path, content);
            }
        }

        Integer maxVersion = appVersionMapper.selectList(new LambdaQueryWrapper<AppVersion>()
                        .eq(AppVersion::getAppId, appId)
                        .orderByDesc(AppVersion::getVersion)
                        .last("limit 1"))
                .stream().findFirst().map(AppVersion::getVersion).orElse(0);

        AppVersion entity = new AppVersion();
        entity.setAppId(appId);
        entity.setVersion(maxVersion + 1);
        entity.setSnapshot(snapshotJson.toString());
        entity.setParentVersion(maxVersion);
        entity.setMessage(message);
        entity.setGenType(genType);
        entity.setUserId(userId);
        appVersionMapper.insert(entity);
        log.info("[Version] 应用 {} 创建版本 v{}（{} 个文件）", appId, entity.getVersion(), snapshotJson.size());
        return entity;
    }

    @Override
    public List<AppVersion> listVersions(long appId) {
        return appVersionMapper.selectList(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getAppId, appId)
                .orderByDesc(AppVersion::getVersion));
    }

    @Override
    public AppVersion rollback(long appId, int version, long userId) {
        AppVersion target = appVersionMapper.selectOne(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getAppId, appId)
                .eq(AppVersion::getVersion, version)
                .last("limit 1"));
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "版本不存在");
        }
        // 恢复快照文件
        Map<String, Object> files = JSONUtil.parseObj(target.getSnapshot());
        for (Map.Entry<String, Object> entry : files.entrySet()) {
            workspaceUtils.writeFile(appId, entry.getKey(), String.valueOf(entry.getValue()));
        }
        // 回滚本身生成一条新版本记录，保证版本树可追溯
        return createSnapshot(appId, userId, "回滚至 v" + version, target.getGenType());
    }
}

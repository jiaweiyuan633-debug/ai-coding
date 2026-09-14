package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.model.entity.App;
import com.aicoding.model.entity.AppVersion;
import com.aicoding.service.AppService;
import com.aicoding.service.AppVersionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 版本时光机接口
 */
@RestController
@RequestMapping("/app/version")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;
    private final AppService appService;

    @GetMapping("/list")
    public BaseResponse<List<AppVersion>> listVersions(@RequestParam long appId) {
        App app = appService.getAppById(appId);
        appService.checkAppOwner(app, UserContext.get());
        // 版本列表不回传快照内容（可能很大）
        appVersionService.listVersions(appId).forEach(v -> v.setSnapshot(null));
        return ResultUtils.success(appVersionService.listVersions(appId));
    }

    @PostMapping("/rollback")
    public BaseResponse<Integer> rollback(@RequestBody RollbackRequest request) {
        App app = appService.getAppById(request.getAppId());
        appService.checkAppOwner(app, UserContext.get());
        AppVersion newVersion = appVersionService.rollback(request.getAppId(), request.getVersion(),
                UserContext.get().getId());
        return ResultUtils.success(newVersion.getVersion());
    }

    @Data
    public static class RollbackRequest {
        private Long appId;
        private Integer version;
    }
}

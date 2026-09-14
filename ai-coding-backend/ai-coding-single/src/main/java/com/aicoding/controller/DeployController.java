package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.service.AppService;
import com.aicoding.service.DeployService;
import com.aicoding.model.entity.App;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用部署接口
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class DeployController {

    private final DeployService deployService;
    private final AppService appService;

    /**
     * 一键部署：发布产物到静态托管目录
     */
    @PostMapping("/{appId}/deploy")
    public BaseResponse<Map<String, Object>> deploy(@PathVariable long appId) {
        App app = deployService.deploy(appId, UserContext.get());
        Map<String, Object> result = new HashMap<>();
        result.put("deployKey", app.getDeployKey());
        result.put("deployedTime", app.getDeployedTime());
        // 本地路径访问地址（阶段 7 Nginx 泛子域名后为 http://{deployKey}.localhost）
        result.put("sharePath", "/api/s/" + app.getDeployKey() + "/");
        return ResultUtils.success(result);
    }

    /**
     * Remix：复制公开应用为自己的新应用
     */
    @PostMapping("/{appId}/remix")
    public BaseResponse<Long> remix(@PathVariable long appId) {
        return ResultUtils.success(appService.remixApp(appId, UserContext.get()));
    }
}

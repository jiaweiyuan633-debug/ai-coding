package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.PageResult;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.model.dto.app.AppAddRequest;
import com.aicoding.model.dto.app.AppQueryRequest;
import com.aicoding.model.dto.app.AppUpdateRequest;
import com.aicoding.model.dto.app.AppVO;
import com.aicoding.service.AppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 应用接口
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    @PostMapping("/add")
    public BaseResponse<Long> addApp(@Valid @RequestBody AppAddRequest request) {
        return ResultUtils.success(appService.addApp(request, UserContext.get()));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@Valid @RequestBody AppUpdateRequest request) {
        appService.updateApp(request, UserContext.get());
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        appService.deleteApp(id, UserContext.get());
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<AppVO> getApp(@RequestParam long id) {
        return ResultUtils.success(appService.getAppVO(id));
    }

    @GetMapping("/my/page")
    public BaseResponse<PageResult<AppVO>> pageMyApps(AppQueryRequest request) {
        return ResultUtils.success(appService.pageMyApps(UserContext.get(), request));
    }

    @GetMapping("/square/page")
    public BaseResponse<PageResult<AppVO>> pageSquareApps(AppQueryRequest request) {
        return ResultUtils.success(appService.pageSquareApps(request));
    }
}

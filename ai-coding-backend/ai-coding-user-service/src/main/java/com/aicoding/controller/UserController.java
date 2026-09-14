package com.aicoding.controller;

import com.aicoding.common.BaseResponse;
import com.aicoding.common.ResultUtils;
import com.aicoding.core.auth.UserContext;
import com.aicoding.model.dto.user.LoginVO;
import com.aicoding.model.dto.user.UserLoginRequest;
import com.aicoding.model.dto.user.UserRegisterRequest;
import com.aicoding.model.dto.user.UserVO;
import com.aicoding.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        return ResultUtils.success(userService.register(request));
    }

    @PostMapping("/login")
    public BaseResponse<LoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return ResultUtils.success(userService.login(request));
    }

    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser() {
        return ResultUtils.success(userService.toVO(UserContext.get()));
    }
}

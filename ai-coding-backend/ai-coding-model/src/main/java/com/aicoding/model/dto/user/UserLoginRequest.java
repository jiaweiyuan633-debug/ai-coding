package com.aicoding.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 */
@Data
public class UserLoginRequest implements Serializable {

    @NotBlank
    private String userAccount;

    @NotBlank
    private String userPassword;

    private static final long serialVersionUID = 1L;
}

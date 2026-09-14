package com.aicoding.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    @NotBlank
    private String userAccount;

    @NotBlank
    private String userPassword;

    @NotBlank
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}

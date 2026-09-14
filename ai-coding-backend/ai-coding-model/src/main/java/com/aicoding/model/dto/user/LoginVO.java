package com.aicoding.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录结果
 */
@Data
public class LoginVO implements Serializable {

    private String token;

    private UserVO user;

    private static final long serialVersionUID = 1L;
}

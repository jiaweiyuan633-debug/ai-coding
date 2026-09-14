package com.aicoding.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用信息传输对象
 */
@Data
public class AppDTO implements Serializable {

    private Long id;

    private String appName;

    private String cover;

    private String initPrompt;

    private String codeGenType;

    private String deployKey;

    private Long userId;

    private static final long serialVersionUID = 1L;
}

package com.aicoding.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建应用请求
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 初始提示词（需求描述）
     */
    @NotBlank
    private String initPrompt;

    /**
     * 应用名称（可选，不填由 AI 自动取名）
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}

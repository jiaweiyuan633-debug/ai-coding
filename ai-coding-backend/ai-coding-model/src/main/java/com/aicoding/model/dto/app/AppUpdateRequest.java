package com.aicoding.model.dto.app;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用请求
 */
@Data
public class AppUpdateRequest implements Serializable {

    @NotNull
    private Long id;

    private String appName;

    private String cover;

    private Integer priority;

    /**
     * 是否精选（仅管理员）
     */
    private Integer isFeatured;

    private static final long serialVersionUID = 1L;
}

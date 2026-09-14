package com.aicoding.model.dto.app;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图
 */
@Data
public class AppVO implements Serializable {

    private Long id;

    private String appName;

    private String cover;

    private String initPrompt;

    private String codeGenType;

    private String deployKey;

    private LocalDateTime deployedTime;

    private Integer priority;

    private Integer isFeatured;

    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 创建者信息（广场页展示）
     */
    private com.aicoding.model.dto.user.UserVO user;

    private static final long serialVersionUID = 1L;
}

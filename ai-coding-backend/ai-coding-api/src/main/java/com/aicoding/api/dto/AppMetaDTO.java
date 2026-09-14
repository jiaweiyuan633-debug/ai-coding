package com.aicoding.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用元信息更新传输对象
 */
@Data
public class AppMetaDTO implements Serializable {

    private Long id;

    private String appName;

    private String codeGenType;

    private String cover;

    private static final long serialVersionUID = 1L;
}

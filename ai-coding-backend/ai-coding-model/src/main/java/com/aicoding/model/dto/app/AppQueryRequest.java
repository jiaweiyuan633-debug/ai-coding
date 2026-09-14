package com.aicoding.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用查询请求（分页）
 */
@Data
public class AppQueryRequest implements Serializable {

    private Long id;

    private String appName;

    private String codeGenType;

    private Long userId;

    private Integer isFeatured;

    /**
     * 只查精选且置顶排序的广场模式
     */
    private Boolean featuredOnly;

    private String sortField;

    private String sortOrder;

    private int currentPage = 1;

    private int pageSize = 10;

    private static final long serialVersionUID = 1L;
}

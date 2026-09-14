package com.aicoding.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;

    private List<T> records;

    private int currentPage;

    private int pageSize;

    private static final long serialVersionUID = 1L;
}

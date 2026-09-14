package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板（模板广场）
 */
@Data
@TableName(value = "template")
public class Template implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String initPrompt;

    private String codeGenType;

    private String cover;

    private Integer sort;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

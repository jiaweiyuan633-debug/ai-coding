package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用
 */
@Data
@TableName(value = "app")
public class App implements Serializable {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime editTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

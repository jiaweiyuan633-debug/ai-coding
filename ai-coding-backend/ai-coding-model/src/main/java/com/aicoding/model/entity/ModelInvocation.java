package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型调用计量（用量中心）
 */
@Data
@TableName(value = "model_invocation")
public class ModelInvocation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long appId;

    private String model;

    /**
     * 用途：router/generate/repair/edit
     */
    private String purpose;

    private Integer inputTokens;

    private Integer outputTokens;

    private Long costMs;

    private String status;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

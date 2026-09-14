package com.aicoding.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史
 */
@Data
@TableName(value = "chat_history")
public class ChatHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String message;

    /**
     * 消息类型：user / ai
     */
    private String messageType;

    private Long appId;

    private Long userId;

    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}

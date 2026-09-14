package com.aicoding.model.dto.chat;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史视图
 */
@Data
public class ChatHistoryVO implements Serializable {

    private Long id;

    private String message;

    private String messageType;

    private Long appId;

    private Long userId;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}

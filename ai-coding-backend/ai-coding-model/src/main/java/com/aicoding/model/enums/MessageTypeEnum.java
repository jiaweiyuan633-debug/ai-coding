package com.aicoding.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 对话消息类型
 */
@Getter
public enum MessageTypeEnum {

    USER("user", "用户消息"),
    AI("ai", "AI 消息");

    private final String value;
    private final String text;

    MessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}

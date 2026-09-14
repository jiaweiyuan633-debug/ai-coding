package com.aicoding.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 代码生成类型枚举
 */
@Getter
public enum CodeGenTypeEnum {

    HTML_SINGLE("HTML_SINGLE", "HTML 单文件应用"),
    HTML_MULTI_FILE("HTML_MULTI_FILE", "HTML 多文件应用"),
    VUE_PROJECT("VUE_PROJECT", "Vue 工程项目");

    private final String value;
    private final String text;

    CodeGenTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举，未匹配时返回 null
     */
    public static CodeGenTypeEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (CodeGenTypeEnum type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return null;
    }
}

package com.aicoding.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户角色
 */
@Getter
public enum UserRoleEnum {

    USER("user", "普通用户"),
    ADMIN("admin", "管理员");

    private final String value;
    private final String text;

    UserRoleEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static UserRoleEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (UserRoleEnum role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }
}

package com.aicoding.common;

import lombok.Getter;

/**
 * 业务错误码
 */
@Getter
public enum ErrorCode {

    OK(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    PARAMS_BLANK(40001, "请求参数为空"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40300, "无权限"),
    NOT_FOUND_ERROR(40400, "资源不存在"),
    FORBIDDEN_ERROR(40301, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    AI_GENERATION_ERROR(50002, "AI 生成失败"),
    TOO_MANY_REQUEST(42900, "请求过于频繁，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

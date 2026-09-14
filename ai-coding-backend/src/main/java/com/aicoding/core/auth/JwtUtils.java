package com.aicoding.core.auth;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import com.aicoding.common.BusinessException;
import com.aicoding.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具（登录态签发与校验）
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:ai-coding-secret-key-please-change-in-prod}")
    private String secret;

    @Value("${jwt.expire-hours:72}")
    private long expireHours;

    /**
     * 生成登录 token
     */
    public String createToken(Long userId, String userRole) {
        long nowSeconds = System.currentTimeMillis() / 1000;
        Map<String, Object> payload = new HashMap<>();
        payload.put(RegisteredPayload.ISSUED_AT, nowSeconds);
        payload.put(RegisteredPayload.EXPIRES_AT, nowSeconds + expireHours * 3600);
        payload.put("userId", userId);
        payload.put("userRole", userRole);
        return JWTUtil.createToken(payload, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验并解析 token，返回 userId；失败抛出未登录异常
     */
    public Long verifyAndGetUserId(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setKey(secret.getBytes(StandardCharsets.UTF_8)).validate(0)) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            Object userId = jwt.getPayload("userId");
            if (userId == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            return Long.valueOf(userId.toString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }

    /**
     * 解析角色（校验通过后调用）
     */
    public String getRole(String token) {
        Object role = JWTUtil.parseToken(token).getPayload("userRole");
        return role == null ? "user" : role.toString();
    }
}

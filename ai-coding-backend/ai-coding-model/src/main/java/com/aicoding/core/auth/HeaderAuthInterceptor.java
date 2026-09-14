package com.aicoding.core.auth;

import com.aicoding.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 微服务内部鉴权：网关完成 JWT 校验后透传 X-User-Id / X-User-Role 头，
 * 各服务用该拦截器重建登录上下文（仅信任网关注入的头，不对公网暴露）
 */
public class HeaderAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        if (userId == null || userId.isBlank()) {
            throw new com.aicoding.common.BusinessException(com.aicoding.common.ErrorCode.NOT_LOGIN_ERROR);
        }
        User user = new User();
        user.setId(Long.valueOf(userId));
        user.setUserRole(role == null || role.isBlank() ? "user" : role);
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

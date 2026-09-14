package com.aicoding.core.auth;

import com.aicoding.model.entity.User;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class UserContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        CURRENT_USER.set(user);
    }

    public static User get() {
        return CURRENT_USER.get();
    }

    public static Long getUserId() {
        User user = CURRENT_USER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}

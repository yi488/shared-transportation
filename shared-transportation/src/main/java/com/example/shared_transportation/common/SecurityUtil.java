package com.example.shared_transportation.common;

import org.springframework.security.core.Authentication;

/**
 * 从安全上下文解析当前登录用户 id。
 * JWT 认证过滤器中 principal 被设置为用户 id 的字符串形式。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long currentUserId(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}

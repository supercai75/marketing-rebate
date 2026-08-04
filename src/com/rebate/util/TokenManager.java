package com.rebate.util;

import com.rebate.model.UserContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token管理器
 * <p>管理token与用户上下文的映射关系，支持token过期自动清理</p>
 */
public class TokenManager {

    private static final Map<String, TokenInfo> TOKEN_MAP = new ConcurrentHashMap<>();

    // Token有效期：8小时
    private static final long EXPIRE_MS = 8 * 60 * 60 * 1000L;

    private static class TokenInfo {
        UserContext userContext;
        long createTime;
        long lastAccessTime;

        TokenInfo(UserContext userContext) {
            this.userContext = userContext;
            this.createTime = System.currentTimeMillis();
            this.lastAccessTime = this.createTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > EXPIRE_MS;
        }
    }

    /**
     * 创建token并保存用户信息
     */
    public static String createToken(UserContext userContext) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TOKEN_MAP.put(token, new TokenInfo(userContext));
        cleanExpired();
        return token;
    }

    /**
     * 根据token获取用户信息，同时更新最后访问时间
     */
    public static UserContext getUser(String token) {
        if (token == null || token.isEmpty()) return null;
        TokenInfo info = TOKEN_MAP.get(token);
        if (info == null) return null;
        if (info.isExpired()) {
            TOKEN_MAP.remove(token);
            return null;
        }
        info.lastAccessTime = System.currentTimeMillis();
        return info.userContext;
    }

    /**
     * 移除token
     */
    public static void removeToken(String token) {
        if (token != null) {
            TOKEN_MAP.remove(token);
        }
    }

    /**
     * 刷新token有效期
     */
    public static void refreshToken(String token) {
        TokenInfo info = TOKEN_MAP.get(token);
        if (info != null) {
            info.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * 清理过期的token
     */
    public static void cleanExpired() {
        TOKEN_MAP.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 获取当前在线用户数
     */
    public static int getOnlineCount() {
        cleanExpired();
        return TOKEN_MAP.size();
    }

    /**
     * 清除所有token（退出所有登录）
     */
    public static void clearAll() {
        TOKEN_MAP.clear();
    }
}

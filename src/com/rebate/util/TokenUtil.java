package com.rebate.util;

import com.rebate.model.UserContext;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Token工具类
 * <p>基于Token的认证方式，替代原有的Session认证</p>
 */
public class TokenUtil {

    public static final String TOKEN_HEADER = "X-Token";

    /**
     * 登录成功，保存用户上下文到Token
     * @return 生成的token字符串
     */
    public static String createToken(UserContext userContext) {
        return TokenManager.createToken(userContext);
    }

    /**
     * 根据请求获取用户上下文
     */
    public static <T> T getLoginUser(HttpServletRequest req, Class<T> clazz) {
        String token = getTokenFromRequest(req);
        if (token == null) return null;
        Object user = TokenManager.getUser(token);
        if (user == null) return null;
        if (clazz.isInstance(user)) return clazz.cast(user);
        return null;
    }

    /**
     * 从请求中获取token
     */
    public static String getTokenFromRequest(HttpServletRequest req) {
        // 优先从Header获取
        String token = req.getHeader(TOKEN_HEADER);
        if (token != null && !token.isEmpty()) {
            return token;
        }
        // 其次从请求参数获取
        token = req.getParameter("token");
        if (token != null && !token.isEmpty()) {
            return token;
        }
        // 最后从Session获取（兼容旧方式）
        var session = req.getSession(false);
        if (session != null) {
            Object user = session.getAttribute("REBATE_LOGIN_USER");
            if (user instanceof UserContext) {
                // 兼容旧的session方式，自动升级到token
                String newToken = createToken((UserContext) user);
                return newToken;
            }
        }
        return null;
    }

    /**
     * 退出登录
     */
    public static void logout(HttpServletRequest req) {
        String token = getTokenFromRequest(req);
        TokenManager.removeToken(token);
    }

    /**
     * 刷新token有效期
     */
    public static void refreshToken(HttpServletRequest req) {
        String token = getTokenFromRequest(req);
        if (token != null) {
            TokenManager.refreshToken(token);
        }
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte v : b) sb.append(String.format("%02x", v));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    public static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

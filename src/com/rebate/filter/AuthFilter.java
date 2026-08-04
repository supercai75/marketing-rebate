package com.rebate.filter;

import com.rebate.model.UserContext;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录态校验过滤器
 * <p>白名单：登录、静态资源、API 登录方法</p>
 */
public class AuthFilter implements Filter {

    private static final Set<String> WHITE_LIST = new HashSet<>(Arrays.asList(
            "/api/auth",
            "/api/health"
    ));

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        // 静态资源、登录页等
        if (!path.startsWith("/api/") || WHITE_LIST.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        UserContext user = TokenUtil.getLoginUser(req, UserContext.class);
        if (user == null) {
            ResponseUtil.unauthorized(resp);
            return;
        }
        // 权限码校验（如果Servlet配置了 requirePerm）
        Object requirePerm = req.getAttribute("requirePerm");
        if (requirePerm != null && !user.hasPerm(String.valueOf(requirePerm))) {
            ResponseUtil.forbidden(resp);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}

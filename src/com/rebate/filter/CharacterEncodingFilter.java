package com.rebate.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 字符编码过滤器
 */
public class CharacterEncodingFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) {
        String e = filterConfig.getInitParameter("encoding");
        if (e != null && !e.isEmpty()) encoding = e;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        
        // 只对 API 请求和动态资源设置请求编码
        if (uri.contains("/api/")) {
            request.setCharacterEncoding(encoding);
            response.setCharacterEncoding(encoding);
        }
        
        HttpServletResponse hr = (HttpServletResponse) response;
        hr.setHeader("X-Content-Type-Options", "nosniff");
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}

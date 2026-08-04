package com.rebate.servlet;

import com.rebate.util.JsonUtil;
import com.rebate.util.ResponseUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 基础 Servlet，提供 JSON 解析、响应辅助
 */
public abstract class BaseServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> params = JsonUtil.readRequestMap(req);
            doAction(req, resp, params);
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.error(resp, "服务器异常: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Map<String, Object> params = new java.util.HashMap<>();
            req.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
            doAction(req, resp, params);
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.error(resp, "服务器异常: " + e.getMessage());
        }
    }

    protected abstract void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> params) throws Exception;
}

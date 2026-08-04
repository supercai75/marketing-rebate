package com.rebate.servlet;

import com.rebate.util.ResponseUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查
 */
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "UP");
        r.put("time", System.currentTimeMillis());
        ResponseUtil.ok(resp, r);
    }
}

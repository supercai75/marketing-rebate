package com.rebate.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应工具
 */
public class ResponseUtil {

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_FAIL = 1;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_ERROR = 500;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(java.sql.Date.class, (com.google.gson.JsonSerializer<java.sql.Date>) (src, typeOfSrc, context) -> {
                if (src == null) return null;
                return new com.google.gson.JsonPrimitive(src.toString());
            })
            .registerTypeAdapter(java.util.Date.class, (com.google.gson.JsonSerializer<java.util.Date>) (src, typeOfSrc, context) -> {
                if (src == null) return null;
                return new com.google.gson.JsonPrimitive(new java.text.SimpleDateFormat("yyyy-MM-dd").format(src));
            })
            .create();

    public static void writeJson(HttpServletResponse resp, int code, String msg, Object data) {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        Map<String, Object> r = new HashMap<>();
        r.put("code", code);
        r.put("msg", msg);
        r.put("data", data);
        try (PrintWriter w = resp.getWriter()) {
            w.write(GSON.toJson(r));
            w.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void ok(HttpServletResponse resp, Object data) {
        writeJson(resp, CODE_SUCCESS, "操作成功", data);
    }

    public static void ok(HttpServletResponse resp) {
        writeJson(resp, CODE_SUCCESS, "操作成功", null);
    }

    public static void fail(HttpServletResponse resp, String msg) {
        writeJson(resp, CODE_FAIL, msg, null);
    }

    public static void unauthorized(HttpServletResponse resp) {
        writeJson(resp, CODE_UNAUTHORIZED, "未登录或登录已过期", null);
    }

    public static void forbidden(HttpServletResponse resp) {
        writeJson(resp, CODE_FORBIDDEN, "无权限访问", null);
    }

    public static void error(HttpServletResponse resp, String msg) {
        writeJson(resp, CODE_ERROR, msg, null);
    }
}

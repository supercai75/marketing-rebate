package com.rebate.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON 工具（基于 Gson）
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, type, ctx) -> new Date(json.getAsJsonPrimitive().getAsLong()))
            .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (src, type, ctx) -> new JsonPrimitive(src.getTime()))
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static Map<String, Object> readRequestMap(HttpServletRequest req) throws IOException {
        Map<String, Object> result = new HashMap<>();
        // 如果是 multipart 请求，跳过读取 body（文件上传通过 getPart() 获取）
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            req.getParameterMap().forEach((k, v) -> result.put(k, v.length > 0 ? v[0] : ""));
            return result;
        }
        // 先读取 JSON 请求体（必须在 getParameterMap() 之前调用 getReader()/getInputStream()，
        // 否则 Tomcat 的参数解析会消费输入流，导致读不到 body 或阻塞超时）
        // 注意：不要使用 readLine() —— 压缩 JSON 通常没有 \n，readLine() 会等到超时。
        String s = readRequestBody(req);
        if (!s.isEmpty()) {
            try {
                Type t = new TypeToken<Map<String, Object>>() {}.getType();
                Map<String, Object> jsonParams = GSON.fromJson(s, t);
                if (jsonParams != null) {
                    result.putAll(jsonParams);
                }
            } catch (Exception e) {
                result.put("_body", s);
            }
        }
        // 再解析 URL 查询参数（body 已读完，getQueryString() 不触发 body 解析）
        String qs = req.getQueryString();
        if (qs != null && !qs.isEmpty()) {
            for (String pair : qs.split("&")) {
                int idx = pair.indexOf('=');
                String key, val;
                if (idx >= 0) {
                    try {
                        key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                        val = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    } catch (Exception e) {
                        continue;
                    }
                } else {
                    key = pair;
                    val = "";
                }
                if (!result.containsKey(key)) {
                    result.put(key, val);
                }
            }
        }
        return result;
    }

    /**
     * 按 Content-Length 精确读取请求体（字符模式），避免 readLine() 等不到换行符而阻塞。
     */
    private static String readRequestBody(HttpServletRequest req) throws IOException {
        int len = req.getContentLength();
        String encoding = req.getCharacterEncoding();
        if (encoding == null || encoding.isEmpty()) encoding = "UTF-8";
        try (ServletInputStream in = req.getInputStream()) {
            byte[] buf;
            if (len > 0) {
                buf = new byte[len];
                int off = 0;
                while (off < len) {
                    int n = in.read(buf, off, len - off);
                    if (n < 0) break;
                    off += n;
                }
                return new String(buf, 0, off, encoding);
            }
            // Content-Length < 0（chunked）：读到 EOF
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) > 0) baos.write(tmp, 0, n);
            return new String(baos.toByteArray(), encoding);
        }
    }

    public static String formatDateTime(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);
    }
}

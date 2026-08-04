package com.rebate.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
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
        // 先读取 URL 查询参数
        req.getParameterMap().forEach((k, v) -> result.put(k, v.length > 0 ? v[0] : ""));
        // 如果是 multipart 请求，跳过读取 body（文件上传通过 getPart() 获取）
        String contentType = req.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            return result;
        }
        // 再读取 JSON 请求体
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = req.getReader()) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        String s = sb.toString();
        if (!s.isEmpty()) {
            Type t = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> jsonParams = GSON.fromJson(s, t);
            if (jsonParams != null) {
                result.putAll(jsonParams);
            }
        }
        return result;
    }

    public static String formatDateTime(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);
    }
}

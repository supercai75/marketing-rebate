package com.rebate.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * Web 通用工具
 */
public class WebUtil {

    public static String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    public static String getSafeParam(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public static long getLong(Map<String, Object> m, String key, long def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { 
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            String s = String.valueOf(v);
            if (s.contains(".")) {
                return Double.valueOf(s).longValue();
            }
            return Long.parseLong(s); 
        } catch (Exception e) { return def; }
    }

    public static int getInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { 
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            String s = String.valueOf(v);
            if (s.contains(".")) {
                return Double.valueOf(s).intValue();
            }
            return Integer.parseInt(s); 
        } catch (Exception e) { return def; }
    }

    public static double getDouble(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        if (v == null) return def;
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return def; }
    }

    public static java.util.Map<String, Object> pageResult(int page, int size, long total, java.util.List<?> rows) {
        java.util.Map<String, Object> r = new java.util.HashMap<>();
        r.put("page", page);
        r.put("size", size);
        r.put("total", total);
        r.put("rows", rows);
        return r;
    }

    /**
     * 从参数中提取字符串列表。支持：
     * - JSON 数组（List）：直接提取为非空字符串列表
     * - 逗号/分号分隔的字符串：按分隔符拆分
     * - 单值 String：转为 1 个元素的列表
     * - 其他：返回空列表（不会为 null）
     */
    public static java.util.List<String> getStringList(Map<String, Object> m, String key) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (m == null) return result;
        Object v = m.get(key);
        if (v == null) return result;
        if (v instanceof java.util.Collection) {
            for (Object o : (java.util.Collection<?>) v) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) result.add(s);
            }
        } else {
            String s = String.valueOf(v).trim();
            if (s.isEmpty()) return result;
            // 支持常见分隔符（逗号 / 分号 / 顿号 / 换行 / 制表符）
            String[] parts = s.split("[,，;；、\\n\\r\\t]+");
            for (String p : parts) {
                String t = p.trim();
                if (!t.isEmpty()) result.add(t);
            }
        }
        return result;
    }
}

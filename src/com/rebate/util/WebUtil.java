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
}

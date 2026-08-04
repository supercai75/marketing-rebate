package com.rebate.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 应用配置加载器。
 * <p>配置读取顺序：</p>
 * <ol>
 *   <li>优先从 classpath:config/db.properties 加载（class下外部文件，部署时可单独覆盖）</li>
 *   <li>再尝试加载 classpath:config/logging.properties</li>
 * </ol>
 * <p>外部配置目录可通过 JVM 参数 -Dconfig.dir=/opt/rebate/conf 覆盖。</p>
 */
public class AppConfig {

    private static final Logger log = Logger.getLogger(AppConfig.class.getName());

    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        String configDir = System.getProperty("config.dir");
        if (configDir == null || configDir.isEmpty()) {
            configDir = System.getenv().getOrDefault("REBATE_CONFIG_DIR", "");
        }
        // 1) classpath
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config/db.properties")) {
            if (in != null) {
                PROPS.load(in);
                log.info("[AppConfig] loaded db.properties from classpath");
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "load db.properties from classpath failed", e);
        }
        // 2) 外部目录覆盖
        if (!configDir.isEmpty()) {
            java.io.File f = new java.io.File(configDir, "db.properties");
            if (f.exists()) {
                try (InputStream in = new java.io.FileInputStream(f)) {
                    PROPS.clear();
                    PROPS.load(in);
                    log.info("[AppConfig] overridden by external " + f.getAbsolutePath());
                } catch (IOException e) {
                    log.log(Level.WARNING, "load external db.properties failed", e);
                }
            }
        }
        loaded = true;
    }

    public static String get(String key) {
        if (!loaded) load();
        return PROPS.getProperty(key, "");
    }

    public static String get(String key, String defaultValue) {
        if (!loaded) load();
        return PROPS.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String v = get(key, "");
        if (v.isEmpty()) return defaultValue;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return defaultValue; }
    }

    public static long getLong(String key, long defaultValue) {
        String v = get(key, "");
        if (v.isEmpty()) return defaultValue;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return defaultValue; }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String v = get(key, "");
        if (v.isEmpty()) return defaultValue;
        return Boolean.parseBoolean(v.trim());
    }

    public static Properties props() {
        if (!loaded) load();
        return PROPS;
    }
}

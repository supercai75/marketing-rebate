package com.rebate.listener;

import com.rebate.config.AppConfig;
import com.rebate.util.DBUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

/**
 * 应用启动监听器
 * <p>负责：</p>
 * <ul>
 *   <li>预加载 AppConfig</li>
 *   <li>测试 DB 连接</li>
 *   <li>启动 BPM 同步定时任务（如启用）</li>
 * </ul>
 */
public class AppContextListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(AppContextListener.class.getName());
    private BpmSyncScheduler bpmScheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("[App] starting...");
        AppConfig.load();
        try (java.sql.Connection c = DBUtil.getConnection()) {
            if (c != null && !c.isClosed()) {
                log.info("[App] database connected: " + AppConfig.get("jdbc.url"));
            }
        } catch (Exception e) {
            log.warning("[App] database connection failed: " + e.getMessage());
        }
        if (AppConfig.getBoolean("bpm.sync.enabled", false)) {
            int minutes = AppConfig.getInt("bpm.sync.interval.minutes", 10);
            bpmScheduler = new BpmSyncScheduler(minutes);
            bpmScheduler.start();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("[App] shutting down...");
        if (bpmScheduler != null) bpmScheduler.stop();
    }
}

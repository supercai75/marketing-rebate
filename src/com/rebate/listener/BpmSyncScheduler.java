package com.rebate.listener;

import com.rebate.service.BpmSyncService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * BPM 同步定时任务调度器
 */
public class BpmSyncScheduler {

    private static final Logger log = Logger.getLogger(BpmSyncScheduler.class.getName());
    private final ScheduledExecutorService executor;
    private final int intervalMinutes;

    public BpmSyncScheduler(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "bpm-sync-thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        log.info("[BpmSync] scheduler started, interval=" + intervalMinutes + " min");
        executor.scheduleAtFixedRate(this::doSync, 1, intervalMinutes, TimeUnit.MINUTES);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void doSync() {
        try {
            BpmSyncService svc = new BpmSyncService();
            int p = svc.syncProjects();
            int a = svc.syncUpstreamAgreements();
            log.info("[BpmSync] projects:" + p + ", agreements:" + a);
        } catch (Exception e) {
            log.warning("[BpmSync] failed: " + e.getMessage());
        }
    }
}

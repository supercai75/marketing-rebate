package com.rebate.service;

import com.rebate.dao.ProjectDao;
import com.rebate.dao.StageMonthConfigDao;
import com.rebate.model.Project;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阶段-月份区间服务
 *
 * 规则：
 *  - 项目合作周期整12个月：默认按"自起始月份每3个月一阶段"划分(S1..S4)。
 *  - 非12个月：由用户在项目保存时定义每个阶段对应的月份区间，存入 prj_stage_month_config。
 *
 * 所有阶段达成计算(上游/下游/估算)统一通过本服务获取 stage->月份区间 映射。
 */
public final class StageMonthService {

    private static final String[] STAGE_CODES = {"S1", "S2", "S3", "S4"};
    private static final StageMonthConfigDao configDao = new StageMonthConfigDao();
    private static final ProjectDao projectDao = new ProjectDao();

    private StageMonthService() {}

    public static String[] stageCodes() { return STAGE_CODES; }

    /**
     * 计算两个日期之间的整月数(含起止月)。
     * 例: 2026-01 ~ 2026-12 = 12; 2026-01 ~ 2027-03 = 15; 2026-09 ~ 2027-06 = 10。
     */
    public static int monthCount(Date periodStart, Date periodEnd) {
        if (periodStart == null || periodEnd == null) return 0;
        Calendar s = Calendar.getInstance(); s.setTime(periodStart);
        Calendar e = Calendar.getInstance(); e.setTime(periodEnd);
        int yDiff = e.get(Calendar.YEAR) - s.get(Calendar.YEAR);
        int mDiff = e.get(Calendar.MONTH) - s.get(Calendar.MONTH);
        return yDiff * 12 + mDiff + 1;
    }

    /** 是否整12个月周期 */
    public static boolean isFullYearPeriod(Date periodStart, Date periodEnd) {
        return monthCount(periodStart, periodEnd) == 12;
    }

    /**
     * 默认区间: 自起始月份每3个月一阶段(整12个月周期适用)。
     * 返回 S1..S4 -> [startYyyymm, endYyyymm]。
     */
    public static Map<String, int[]> computeDefaultRanges(Date periodStart) {
        Map<String, int[]> r = new LinkedHashMap<>();
        if (periodStart == null) return r;
        Calendar c = Calendar.getInstance(); c.setTime(periodStart);
        int startYear = c.get(Calendar.YEAR);
        int startMonth = c.get(Calendar.MONTH) + 1; // 1-based
        for (int i = 0; i < 4; i++) {
            int from = i * 3;                 // 0,3,6,9 偏移
            int to = from + 2;                // 阶段末月偏移
            int[] start = rollMonth(startYear, startMonth, from);
            int[] end = rollMonth(startYear, startMonth, to);
            r.put(STAGE_CODES[i], new int[]{yyyymm(start[0], start[1]), yyyymm(end[0], end[1])});
        }
        return r;
    }

    /**
     * 获取项目的阶段-月份区间映射:
     * 优先读 prj_stage_month_config(非12个月自定义); 否则按默认规则(整12个月)计算。
     */
    public static Map<String, int[]> getStageRanges(Long projectId) {
        if (projectId == null) return new LinkedHashMap<>();
        Map<String, int[]> cfg = configDao.listRangesByProject(projectId);
        if (cfg != null && !cfg.isEmpty()) return cfg;
        Project p = projectDao.findById(projectId);
        return computeDefaultRanges(p == null ? null : p.getPeriodStartDate());
    }

    /** 事务内读取(供保存时校验使用) */
    public static Map<String, int[]> getStageRangesWithConn(Connection conn, Long projectId) throws java.sql.SQLException {
        if (projectId == null) return new LinkedHashMap<>();
        return configDao.listRangesByProjectWithConn(conn, projectId);
    }

    /**
     * 将月份归属到阶段: 返回 S1..S4 或 null(不在任何阶段区间内)。
     */
    public static String stageOfYyyymm(Map<String, int[]> ranges, int yyyymm) {
        if (ranges == null || ranges.isEmpty()) return null;
        for (String sc : STAGE_CODES) {
            int[] rg = ranges.get(sc);
            if (rg != null && rg.length == 2 && yyyymm >= rg[0] && yyyymm <= rg[1]) return sc;
        }
        return null;
    }

    /**
     * 汇总某阶段区间内的月度规模(monthScale key=YYYYMM 字符串)。
     */
    public static BigDecimal sumByRange(Map<String, BigDecimal> monthScale, int[] range) {
        BigDecimal sum = BigDecimal.ZERO;
        if (monthScale == null || range == null || range.length != 2) return sum;
        for (Map.Entry<String, BigDecimal> e : monthScale.entrySet()) {
            int yyyymm = parseYyyymm(e.getKey());
            if (yyyymm < 0) continue;
            if (yyyymm >= range[0] && yyyymm <= range[1]) {
                if (e.getValue() != null) sum = sum.add(e.getValue());
            }
        }
        return sum;
    }

    /**
     * 合并多个阶段的区间汇总(用于"仅2阶段/1阶段"等历史兼容场景)。
     */
    public static BigDecimal sumByRanges(Map<String, BigDecimal> monthScale, Map<String, int[]> ranges, String... stageCodes) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String sc : stageCodes) {
            sum = sum.add(sumByRange(monthScale, ranges.get(sc)));
        }
        return sum;
    }

    private static int parseYyyymm(String s) {
        if (s == null || s.length() < 6) return -1;
        try { return Integer.parseInt(s.substring(0, 6)); } catch (NumberFormatException e) { return -1; }
    }

    private static int yyyymm(int year, int month) {
        return year * 100 + month;
    }

    /** 从 (year, month[1-based]) 出发偏移 delta 月，返回 [year, month(1-based)]，处理跨年。 */
    private static int[] rollMonth(int year, int month, int delta) {
        int total = (year) * 12 + (month - 1) + delta;
        int y = total / 12;
        int m = (total % 12) + 1;
        return new int[]{y, m};
    }
}

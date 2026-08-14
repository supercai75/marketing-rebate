package com.rebate.service;

import com.rebate.dao.BaseDao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 核心业务：项目规模/阶段达成计算
 * <p>规则见业务需求 5.2、5.3。</p>
 */
public class ProjectScaleService {

    /**
     * 计算项目整体规模与各阶段达成。
     *
     * @param projectId  项目
     * @param basis      "QTY" / "AMT" 来自上游协议
     * @param months     有效的月份规模序列（key=yyyyMM, value=规模）
     * @param stage1/2/3/4 协议各阶段目标（可能为0）
     * @return 计算结果
     */
    public static Map<String, BigDecimal> computeScale(long projectId, String basis,
                                                      Map<String, BigDecimal> monthScale,
                                                      BigDecimal stage1, BigDecimal stage2,
                                                      BigDecimal stage3, BigDecimal stage4) {
        Map<String, BigDecimal> r = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        if (monthScale != null) {
            for (BigDecimal v : monthScale.values()) {
                if (v != null) total = total.add(v);
            }
        }
        r.put("totalActual", total);

        // 阶段1 = M1~M3
        // 阶段2 = M4~M6
        // 阶段3 = M7~M9
        // 阶段4 = M10~M12
        // 如果只填阶段1+2: 阶段1=M1~M6, 阶段2=M7~M12
        // 如果四个都填了: 按3月一段
        // 如果四个都没填: 不计算阶段

        boolean allFour = nz(stage1).signum() > 0 || nz(stage2).signum() > 0
                || nz(stage3).signum() > 0 || nz(stage4).signum() > 0;

        if (!allFour) {
            r.put("stage1Actual", BigDecimal.ZERO);
            r.put("stage2Actual", BigDecimal.ZERO);
            r.put("stage3Actual", BigDecimal.ZERO);
            r.put("stage4Actual", BigDecimal.ZERO);
            return r;
        }

        boolean all4Filled = nz(stage1).signum() > 0 && nz(stage2).signum() > 0
                && nz(stage3).signum() > 0 && nz(stage4).signum() > 0;
        if (all4Filled) {
            r.put("stage1Actual", sumRange(monthScale, 1, 3));
            r.put("stage2Actual", sumRange(monthScale, 4, 6));
            r.put("stage3Actual", sumRange(monthScale, 7, 9));
            r.put("stage4Actual", sumRange(monthScale, 10, 12));
        } else if (nz(stage1).signum() > 0 && nz(stage2).signum() > 0) {
            r.put("stage1Actual", sumRange(monthScale, 1, 6));
            r.put("stage2Actual", sumRange(monthScale, 7, 12));
            r.put("stage3Actual", BigDecimal.ZERO);
            r.put("stage4Actual", BigDecimal.ZERO);
        } else {
            // 只有阶段1填了: 整年算阶段1
            r.put("stage1Actual", total);
            r.put("stage2Actual", BigDecimal.ZERO);
            r.put("stage3Actual", BigDecimal.ZERO);
            r.put("stage4Actual", BigDecimal.ZERO);
        }
        return r;
    }

    /**
     * 拉取某项目各月份的有效规模，返回 month->scale 的 Map
     */
    public static Map<String, BigDecimal> loadMonthScale(long projectId, String basis) {
        return loadMonthScale(projectId, basis, null);
    }

    /**
     * 拉取某项目各月份的有效规模，支持按考核组过滤，返回 month->scale 的 Map
     */
    public static Map<String, BigDecimal> loadMonthScale(long projectId, String basis, Long assessGroupId) {
        String sumCol = basisToColumn(basis);
        String sql;
        List<Object> params = new java.util.ArrayList<>();
        params.add(projectId);
        if (assessGroupId == null) {
            sql = "SELECT month_yyyymm, COALESCE(SUM(" + sumCol + "),0) AS s " +
                    "FROM flow_upstream_record WHERE project_id=? AND is_valid=1 GROUP BY month_yyyymm";
        } else {
            sql = "SELECT month_yyyymm, COALESCE(SUM(" + sumCol + "),0) AS s " +
                    "FROM flow_upstream_record WHERE project_id=? AND is_valid=1 AND assess_group_id=? GROUP BY month_yyyymm";
            params.add(assessGroupId);
        }
        List<Map<String, Object>> rows = BaseDao.query(sql,
                rs -> { Map<String, Object> m = new HashMap<>(); m.put("m", rs.getString("month_yyyymm")); m.put("s", rs.getBigDecimal("s")); return m; },
                params.toArray());
        Map<String, BigDecimal> r = new HashMap<>();
        for (Map<String, Object> row : rows) {
            r.put((String) row.get("m"), (BigDecimal) row.get("s"));
        }
        return r;
    }

    /**
     * 根据口径枚举映射到流向表的实际求和列
     */
    public static String basisToColumn(String basis) {
        if (basis == null) return "calc_amount";
        switch (basis.toUpperCase()) {
            case "QTY": return "quantity";
            case "SALE_QTY": return "sale_qty";
            case "BID_AMT": return "bid_amount";
            case "CALC_AMT":
            case "AMT":
            default: return "calc_amount";
        }
    }

    private static BigDecimal sumRange(Map<String, BigDecimal> monthScale, int fromMonth, int toMonth) {
        BigDecimal sum = BigDecimal.ZERO;
        // 假定 monthScale 中月份可能是任意 yyyyMM；这里按"一年内的相对月份"取
        // 简单做法：要求调用方传入的 monthScale key 为 yyyyMM
        // 取所有月份按字典序排序后第 fromMonth-1 到 toMonth-1 项
        if (monthScale == null || monthScale.isEmpty()) return sum;
        java.util.List<String> keys = new java.util.ArrayList<>(monthScale.keySet());
        java.util.Collections.sort(keys);
        for (int i = fromMonth - 1; i < toMonth && i < keys.size(); i++) {
            BigDecimal v = monthScale.get(keys.get(i));
            if (v != null) sum = sum.add(v);
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    /** 汇总月度规模的所有值 */
    public static BigDecimal sumMonthScale(Map<String, BigDecimal> monthScale) {
        BigDecimal sum = BigDecimal.ZERO;
        if (monthScale != null) {
            for (BigDecimal v : monthScale.values()) {
                if (v != null) sum = sum.add(v);
            }
        }
        return sum;
    }

    /**
     * 计算达成率（百分比，保留2位小数）
     */
    public static BigDecimal rate(BigDecimal actual, BigDecimal target) {
        if (target == null || target.signum() == 0) return BigDecimal.ZERO;
        if (actual == null) actual = BigDecimal.ZERO;
        return actual.multiply(BigDecimal.valueOf(100)).divide(target, 2, java.math.RoundingMode.HALF_UP);
    }
}

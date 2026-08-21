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
        // 阶段-月份区间: 整12个月走默认(自起始月每3月一阶段), 非12个月读 prj_stage_month_config
        java.util.Map<String, int[]> ranges = StageMonthService.getStageRanges(projectId);
        if (all4Filled) {
            r.put("stage1Actual", StageMonthService.sumByRange(monthScale, ranges.get("S1")));
            r.put("stage2Actual", StageMonthService.sumByRange(monthScale, ranges.get("S2")));
            r.put("stage3Actual", StageMonthService.sumByRange(monthScale, ranges.get("S3")));
            r.put("stage4Actual", StageMonthService.sumByRange(monthScale, ranges.get("S4")));
        } else if (nz(stage1).signum() > 0 && nz(stage2).signum() > 0) {
            r.put("stage1Actual", StageMonthService.sumByRanges(monthScale, ranges, "S1", "S2"));
            r.put("stage2Actual", StageMonthService.sumByRanges(monthScale, ranges, "S3", "S4"));
            r.put("stage3Actual", BigDecimal.ZERO);
            r.put("stage4Actual", BigDecimal.ZERO);
        } else {
            // 只有阶段1填了: 整周期算阶段1
            r.put("stage1Actual", StageMonthService.sumByRanges(monthScale, ranges, "S1", "S2", "S3", "S4"));
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
     * 批量查询多个项目的流向汇总（数量+金额+各口径），单条SQL完成
     * @return projectId -> {qty, amt, saleQty, bidAmt}
     */
    public static Map<Long, Map<String, BigDecimal>> batchSumScale(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return new HashMap<>();
        StringBuilder sql = new StringBuilder(
                "SELECT project_id, COALESCE(SUM(quantity),0) AS qty, COALESCE(SUM(calc_amount),0) AS amt, " +
                "COALESCE(SUM(sale_qty),0) AS sale_qty, COALESCE(SUM(bid_amount),0) AS bid_amt, " +
                "COALESCE(SUM(tax_amount),0) AS tax_amt " +
                "FROM flow_upstream_record WHERE is_valid=1 AND project_id IN (");
        for (int i = 0; i < projectIds.size(); i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(") GROUP BY project_id");
        List<Map<String, Object>> rows = BaseDao.query(sql.toString(),
                rs -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("pid", rs.getLong("project_id"));
                    m.put("qty", rs.getBigDecimal("qty"));
                    m.put("amt", rs.getBigDecimal("amt"));
                    m.put("saleQty", rs.getBigDecimal("sale_qty"));
                    m.put("bidAmt", rs.getBigDecimal("bid_amt"));
                    m.put("taxAmt", rs.getBigDecimal("tax_amt"));
                    return m;
                },
                projectIds.toArray());
        Map<Long, Map<String, BigDecimal>> r = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = (Long) row.get("pid");
            Map<String, BigDecimal> v = new HashMap<>();
            v.put("QTY", (BigDecimal) row.get("qty"));
            v.put("AMT", (BigDecimal) row.get("amt"));
            v.put("SALE_QTY", (BigDecimal) row.get("saleQty"));
            v.put("BID_AMT", (BigDecimal) row.get("bidAmt"));
            v.put("TAX_AMT", (BigDecimal) row.get("taxAmt"));
            r.put(pid, v);
        }
        return r;
    }

    /**
     * 根据口径从批量结果中取值
     */
    public static BigDecimal pickByBasis(Map<String, BigDecimal> scaleMap, String basis) {
        if (scaleMap == null) return BigDecimal.ZERO;
        if (basis == null) return scaleMap.getOrDefault("AMT", BigDecimal.ZERO);
        switch (basis.toUpperCase()) {
            case "QTY": return scaleMap.getOrDefault("QTY", BigDecimal.ZERO);
            case "SALE_QTY": return scaleMap.getOrDefault("SALE_QTY", BigDecimal.ZERO);
            case "BID_AMT": return scaleMap.getOrDefault("BID_AMT", BigDecimal.ZERO);
            case "TAX_AMT": return scaleMap.getOrDefault("TAX_AMT", BigDecimal.ZERO);
            case "CALC_AMT":
            case "AMT":
            default: return scaleMap.getOrDefault("AMT", BigDecimal.ZERO);
        }
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
            case "TAX_AMT": return "tax_amount";
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

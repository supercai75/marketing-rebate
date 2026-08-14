package com.rebate.service;

import com.rebate.model.RebateRule;
import com.rebate.util.ExpressionUtil;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * 返利规则计算统一工具：
 *  1. 根据实际值 X 的封顶：当 threshold_high > 0 且 实际X > threshold_high -> 使用 threshold_high
 *  2. 优先使用表达式：如果 表达式(expression) 计算比例，兼容 返利比例
 *      - 表达式为空 / 表达式为纯数字 => 回退使用 rebate_ratio
 *      - 表达式支持变量 X（达成率/增长率/达成额）和 Y（完成的核算数量）
 *  3. 按规则区间选择 X 区间查找：根据 low ≤ X < high（high == 0 代表无上限）
 *  4. 支持两种计算模式：PROGRESSIVE（递进式分段累计）/ FLAT（全部计算，按匹配比例乘以整体基数）
 */
public class RebateCalcUtil {

    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * 根据规则列表与实际达成值 X，匹配区间并计算返利比例
     */
    public static BigDecimal calcRebateRatio(List<RebateRule> rules, BigDecimal actualX) {
        return calcRebateRatio(rules, actualX, BigDecimal.ZERO);
    }

    /**
     * 根据规则列表与实际达成值 X、Y，匹配区间并计算返利比例
     */
    public static BigDecimal calcRebateRatio(List<RebateRule> rules, BigDecimal actualX, BigDecimal actualY) {
        RebateRule matched = matchRule(rules, actualX);
        if (matched == null) return BigDecimal.ZERO;
        return calcRatioByRule(matched, actualX, actualY);
    }

    /**
     * 根据规则列表与实际达成值 X，按阶段 + 达成值 区间匹配
     */
    public static RebateRule matchRule(List<RebateRule> rules, BigDecimal actualX) {
        if (rules == null || rules.isEmpty() || actualX == null) return null;
        for (RebateRule r : rules) {
            BigDecimal lo = nvl(r.getThresholdLow());
            BigDecimal hi = nvl(r.getThresholdHigh());
            boolean hiOpen = (hi.compareTo(BigDecimal.ZERO) <= 0);
            if (actualX.compareTo(lo) >= 0 && (hiOpen || actualX.compareTo(hi) < 0)) {
                return r;
            }
        }
        return null;
    }

    /**
     * 按照给定规则与实际 X、Y，计算返利比例（含 X 上限封顶 + 表达式优先 / 纯数字回退 rebate_ratio）
     */
    public static BigDecimal calcRatioByRule(RebateRule rule, BigDecimal actualX) {
        return calcRatioByRule(rule, actualX, BigDecimal.ZERO);
    }

    /**
     * 按照给定规则与实际 X、Y，计算返利比例（含 X 上限封顶 + 表达式优先 / 纯数字回退 rebate_ratio）
     */
    public static BigDecimal calcRatioByRule(RebateRule rule, BigDecimal actualX, BigDecimal actualY) {
        if (rule == null) return BigDecimal.ZERO;
        BigDecimal x = capX(actualX, rule.getThresholdHigh());
        String expr = rule.getExpression();
        if (expr != null && !expr.trim().isEmpty()) {
            String trimmed = expr.trim();
            if (ExpressionUtil.isPureNumber(trimmed)) {
                BigDecimal directPct = new BigDecimal(trimmed, MC);
                return directPct.divide(BigDecimal.valueOf(100), MC);
            }
            try {
                BigDecimal v = ExpressionUtil.eval(trimmed, x, actualY);
                if (v == null) return nvl(rule.getRebateRatio());
                return v.divide(BigDecimal.valueOf(100), MC);
            } catch (Exception e) {
                return nvl(rule.getRebateRatio());
            }
        }
        return nvl(rule.getRebateRatio());
    }

    /**
     * 计算返利金额（支持递进式和全部计算两种模式）
     *
     * @param rules      规则列表（已按 sortNo 排序）
     * @param actualX    实际 X 值（达成率%/增长率%/达成额）
     * @param actualY    实际 Y 值（完成的核算数量）
     * @param baseAmount 返利计算基数（达成额/规模金额）
     * @return 返利金额
     */
    public static BigDecimal calcRebateAmount(List<RebateRule> rules, BigDecimal actualX,
                                               BigDecimal actualY, BigDecimal baseAmount) {
        if (rules == null || rules.isEmpty()) return BigDecimal.ZERO;
        BigDecimal x = nvl(actualX);
        BigDecimal y = nvl(actualY);
        BigDecimal base = nvl(baseAmount);

        // 判断计算模式：默认 PROGRESSIVE
        String calcMode = rules.get(0).getCalcMode();
        boolean isFlat = "FLAT".equalsIgnoreCase(calcMode);

        if (isFlat) {
            // 全部计算：找到匹配区间的比例，用整体基数 × 比例
            RebateRule matched = matchRule(rules, x);
            if (matched == null) return BigDecimal.ZERO;
            BigDecimal ratio = calcRatioByRule(matched, x, y);
            return base.multiply(ratio, MC);
        }

        // 递进式计算：分段累计
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal prevHigh = BigDecimal.ZERO;
        for (RebateRule r : rules) {
            BigDecimal lo = nvl(r.getThresholdLow());
            BigDecimal hi = nvl(r.getThresholdHigh());
            boolean hiOpen = (hi.compareTo(BigDecimal.ZERO) <= 0);

            // 区间有效部分：[max(low, prevHigh), min(high or X, X)]
            BigDecimal effStart = lo.compareTo(prevHigh) > 0 ? lo : prevHigh;
            BigDecimal effEnd = hiOpen ? x : (x.compareTo(hi) < 0 ? x : hi);
            if (effEnd.compareTo(effStart) <= 0) {
                if (!hiOpen) prevHigh = hi;
                continue;
            }

            BigDecimal segWidth = effEnd.subtract(effStart, MC);
            BigDecimal ratio = calcRatioByRule(r, x, y);
            String rt = r.getRewardType() == null ? "" : r.getRewardType().toUpperCase();
            if ("SCALE".equals(rt)) {
                // 双口径兼容：按 calcBasis 计算出每段区间占比，再乘以返利基数 base（rebateCalcBasis）
                // 当 calcBasis == rebateCalcBasis 时，等价于 segWidth * ratio
                BigDecimal totalX = nvl(actualX);
                if (totalX.signum() <= 0) continue;
                BigDecimal rebateSegBase = base.multiply(segWidth, MC).divide(totalX, MC);
                total = total.add(rebateSegBase.multiply(ratio, MC), MC);
            } else {
                // 按达成率/增长率：segWidth 是百分点，需 base × segWidth / 100 × ratio
                total = total.add(base.multiply(segWidth, MC).multiply(ratio, MC)
                        .divide(BigDecimal.valueOf(100), MC), MC);
            }

            if (!hiOpen) prevHigh = hi;
        }

        return total;
    }

    /**
     * X 上限封顶：如果 threshold_high > 0 且 actualX > threshold_high 则用 threshold_high
     */
    public static BigDecimal capX(BigDecimal actualX, BigDecimal thresholdHigh) {
        if (actualX == null) return BigDecimal.ZERO;
        BigDecimal hi = nvl(thresholdHigh);
        if (hi.compareTo(BigDecimal.ZERO) > 0 && actualX.compareTo(hi) > 0) {
            return hi;
        }
        return actualX;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

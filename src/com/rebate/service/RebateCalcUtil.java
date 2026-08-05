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
 *  3. 按规则区间选择 X 区间查找：根据 low ≤ X < high（= high ( high == 0 代表无上限)
 */
public class RebateCalcUtil {

    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * 根据规则列表与实际达成值 X，匹配区间并计算返利比例
     *
     * @param rules      规则列表（阈值区间
     * @param actualX  实际值（达成率/销售增长率/达成额，单位取决于 calcMethod/rewardType）
     * @return 返利比例（如 0.045 表示 4.5%），未匹配返回 0
     */
    public static BigDecimal calcRebateRatio(List<RebateRule> rules, BigDecimal actualX) {
        RebateRule matched = matchRule(rules, actualX);
        if (matched == null) return BigDecimal.ZERO;
        return calcRatioByRule(matched, actualX);
    }

    /**
     * 根据规则列表与实际达成值 X，按阶段 + 达成值 区间匹配
     */
    public static RebateRule matchRule(List<RebateRule> rules, BigDecimal actualX) {
        if (rules == null || rules.isEmpty() || actualX == null) return null;
        for (RebateRule r : rules) {
            BigDecimal lo = nvl(r.getThresholdLow());
            BigDecimal hi = nvl(r.getThresholdHigh());
            boolean hiOpen = (hi.compareTo(BigDecimal.ZERO) <= 0); // 0/NULL 视为无上限
            if (actualX.compareTo(lo) >= 0 && (hiOpen || actualX.compareTo(hi) < 0)) {
                return r;
            }
        }
        return null;
    }

    /**
     * 按照给定规则与实际 X，计算返利比例（含 X 上限封顶 + 表达式优先 / 纯数字回退 rebate_ratio）
     */
    public static BigDecimal calcRatioByRule(RebateRule rule, BigDecimal actualX) {
        if (rule == null) return BigDecimal.ZERO;
        BigDecimal x = capX(actualX, rule.getThresholdHigh());
        String expr = rule.getExpression();
        if (expr != null && !expr.trim().isEmpty()) {
            String trimmed = expr.trim();
            // 表达式为纯数字：视为百分比数值（如 4.5 表示 4.5%），需 /100 转小数
            if (ExpressionUtil.isPureNumber(trimmed)) {
                BigDecimal directPct = new BigDecimal(trimmed, MC);
                return directPct.divide(BigDecimal.valueOf(100), MC);
            }
            try {
                BigDecimal v = ExpressionUtil.eval(trimmed, x);
                if (v == null) return nvl(rule.getRebateRatio());
                // 表达式结果为百分比数值（如 4.5 表示 4.5%），需 /100 转小数
                return v.divide(BigDecimal.valueOf(100), MC);
            } catch (Exception e) {
                // 表达式解析异常，退回 rebate_ratio
                return nvl(rule.getRebateRatio());
            }
        }
        return nvl(rule.getRebateRatio());
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

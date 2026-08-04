package com.rebate.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

/**
 * 返利金额估算
 */
public class RebateEstimateService {

    public static class Rule {
        public String stage;
        public BigDecimal min;
        public BigDecimal max;
        public BigDecimal rate;
        public String type; // PERCENT:按完成百分比, AMOUNT:按完成金额
    }

    /**
     * 估算某一阶段的返利额
     * @param ruleJson 协议中的返利计算方式 JSON
     * @param stage    阶段名 ALL/S1/S2/S3/S4
     * @param actualScale 该阶段的实际达成规模
     */
    public static BigDecimal estimate(String ruleJson, String stage, BigDecimal actualScale) {
        if (ruleJson == null || ruleJson.isEmpty()) return BigDecimal.ZERO;
        if (actualScale == null) actualScale = BigDecimal.ZERO;
        Type t = new TypeToken<List<Rule>>() {}.getType();
        List<Rule> rules = new Gson().fromJson(ruleJson, t);
        if (rules == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Rule r : rules) {
            // 支持全阶段匹配
            if (!Objects.equals(r.stage, stage) && !"ALL".equals(r.stage)) continue;
            BigDecimal min = r.min == null ? BigDecimal.ZERO : r.min;
            BigDecimal max = r.max; // null = 无上限
            BigDecimal rate = r.rate == null ? BigDecimal.ZERO : r.rate;
            if (actualScale.compareTo(min) <= 0) continue;
            BigDecimal upper = max == null ? actualScale : actualScale.min(max);
            BigDecimal segScale = upper.subtract(min);
            if (segScale.signum() <= 0) continue;
            // 根据计算类型计算
            BigDecimal rebate;
            if ("AMOUNT".equals(r.type)) {
                // 按完成金额：返利 = 达成金额（rate直接作为返利值）
                rebate = segScale.multiply(BigDecimal.ONE);
            } else {
                // 按完成百分比（默认）：返利 = 达成金额 × 返利比例
                rebate = segScale.multiply(rate);
            }
            total = total.add(rebate);
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

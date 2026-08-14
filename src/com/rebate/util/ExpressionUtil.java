package com.rebate.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 简单表达式计算工具：
 *  - 支持 +  -  *  /  以及  ( )
 *  - 支持变量 X（使用 BigDecimal 计算，精度 34 位）
 *  - 支持纯数字直接返回（兼容直接写比例数值的情况）
 *
 * 使用示例：
 *   ExpressionUtil.eval("0.125*(X-60)+2", new BigDecimal("80"))
 *   => 0.125*(80-60)+2 = 4.5
 */
public class ExpressionUtil {

    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * 计算表达式，代入变量 X 和 Y
     * @param expr 表达式，可为空或纯数字
     * @param x    变量 X 的值（达成率/增长率/达成额）
     * @param y    变量 Y 的值（完成的核算数量）
     * @return 计算结果；表达式为空或空白时返回 null
     */
    public static BigDecimal eval(String expr, BigDecimal x, BigDecimal y) {
        if (expr == null) return null;
        String s = expr.trim();
        if (s.isEmpty()) return null;
        Parser p = new Parser(s, x == null ? BigDecimal.ZERO : x, y == null ? BigDecimal.ZERO : y);
        BigDecimal r = p.parseExpression();
        if (p.pos != s.length()) {
            throw new IllegalArgumentException("表达式存在意外字符，位置 " + p.pos + ": " + s);
        }
        return r;
    }

    /**
     * 计算表达式，代入变量 X（兼容旧调用，Y 默认为 0）
     */
    public static BigDecimal eval(String expr, BigDecimal x) {
        return eval(expr, x, BigDecimal.ZERO);
    }

    /**
     * 判断字符串是否像一个纯数字（兼容 0.05 / 5 这种直接写返利比例的情况）
     */
    public static boolean isPureNumber(String expr) {
        if (expr == null) return false;
        String s = expr.trim();
        if (s.isEmpty()) return false;
        try {
            new BigDecimal(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static final class Parser {
        private final String src;
        private final BigDecimal x;
        private final BigDecimal y;
        private int pos;

        Parser(String src, BigDecimal x, BigDecimal y) {
            this.src = src;
            this.x = x;
            this.y = y;
            this.pos = 0;
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }

        // 加减法
        BigDecimal parseExpression() {
            BigDecimal left = parseTerm();
            while (true) {
                skipWs();
                if (pos >= src.length()) break;
                char c = src.charAt(pos);
                if (c != '+' && c != '-') break;
                pos++;
                BigDecimal right = parseTerm();
                if (c == '+') left = left.add(right, MC);
                else left = left.subtract(right, MC);
            }
            return left;
        }

        // 乘除法
        private BigDecimal parseTerm() {
            BigDecimal left = parseFactor();
            while (true) {
                skipWs();
                if (pos >= src.length()) break;
                char c = src.charAt(pos);
                if (c != '*' && c != '/') break;
                pos++;
                BigDecimal right = parseFactor();
                if (c == '*') {
                    left = left.multiply(right, MC);
                } else {
                    if (right.signum() == 0) {
                        throw new ArithmeticException("表达式除零错误");
                    }
                    left = left.divide(right, MC);
                }
            }
            return left;
        }

        // 基础因子：(expr) / X / 数字 / -数字
        private BigDecimal parseFactor() {
            skipWs();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("表达式意外结束");
            }
            char c = src.charAt(pos);
            if (c == '(') {
                pos++;
                BigDecimal v = parseExpression();
                skipWs();
                if (pos >= src.length() || src.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return v;
            }
            if (c == '-' || c == '+') {
                pos++;
                BigDecimal v = parseFactor();
                return c == '-' ? v.negate() : v;
            }
            if (c == 'X' || c == 'x') {
                pos++;
                return x;
            }
            if (c == 'Y' || c == 'y') {
                pos++;
                return y;
            }
            // 数字
            int start = pos;
            if (c == '.') {
                throw new IllegalArgumentException("数字不能以 . 开头，位置 " + pos);
            }
            boolean hasDot = false;
            while (pos < src.length()) {
                char ch = src.charAt(pos);
                if (Character.isDigit(ch)) {
                    pos++;
                } else if (ch == '.' && !hasDot) {
                    hasDot = true;
                    pos++;
                } else {
                    break;
                }
            }
            if (start == pos) {
                throw new IllegalArgumentException("无法识别的字符 '" + c + "'，位置 " + pos);
            }
            return new BigDecimal(src.substring(start, pos), MC);
        }
    }
}

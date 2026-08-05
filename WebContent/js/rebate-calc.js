/**
 * 返利表达式/比例 统一计算工具（前端）
 * 规则：
 *   1. X = 达成率/增长率/达成额（按 rewardType 决定）
 *   2. 当 X > threshold_high (threshold_high > 0 时)，X 取 threshold_high
 *   3. 优先使用 expression 表达式；expression 为空 / 纯数字 -> 回退到 rebateRatio；纯数字也按比例数值直接使用
 *   4. 多规则阶梯式拆分时：区间有效 = [max(rule.low, prevHigh), min(rule.high or X, X))，
 *      但按比例计算返利金额时仍统一使用「区间匹配后」的公式/比例，采用「封顶X后算比例×达成基数」
 */
window.RebateCalc = (function () {
  'use strict';

  /**
   * 简易表达式求值：支持 + - * / ( ) 和变量 X
   */
  function evalExpr(expr, x) {
    var src = (expr || '').trim();
    if (!src) return null;
    var pos = 0;
    var len = src.length;
    function skipWs() { while (pos < len && /\s/.test(src.charAt(pos))) pos++; }
    function parseExpr() {
      var left = parseTerm();
      while (true) {
        skipWs();
        if (pos >= len) break;
        var c = src.charAt(pos);
        if (c !== '+' && c !== '-') break;
        pos++;
        var right = parseTerm();
        left = (c === '+') ? left + right : left - right;
      }
      return left;
    }
    function parseTerm() {
      var left = parseFactor();
      while (true) {
        skipWs();
        if (pos >= len) break;
        var c = src.charAt(pos);
        if (c !== '*' && c !== '/') break;
        pos++;
        var right = parseFactor();
        if (c === '*') left = left * right;
        else {
          if (right === 0) throw new Error('除零错误');
          left = left / right;
        }
      }
      return left;
    }
    function parseFactor() {
      skipWs();
      if (pos >= len) throw new Error('表达式意外结束');
      var c = src.charAt(pos);
      if (c === '(') {
        pos++;
        var v = parseExpr();
        skipWs();
        if (pos >= len || src.charAt(pos) !== ')') throw new Error('缺少右括号');
        pos++;
        return v;
      }
      if (c === '-' || c === '+') {
        pos++;
        var v2 = parseFactor();
        return c === '-' ? -v2 : v2;
      }
      if (c === 'X' || c === 'x') {
        pos++;
        return Number(x) || 0;
      }
      var start = pos;
      var hasDot = false;
      while (pos < len) {
        var ch = src.charAt(pos);
        if (/\d/.test(ch)) pos++;
        else if (ch === '.' && !hasDot) { hasDot = true; pos++; }
        else break;
      }
      if (start === pos) throw new Error('无法识别字符 "' + c + '"');
      return parseFloat(src.substring(start, pos));
    }
    var result = parseExpr();
    if (pos !== len) throw new Error('表达式末尾存在多余字符，位置 ' + pos);
    return result;
  }

  function isPureNumber(s) {
    if (!s) return false;
    return !isNaN(parseFloat(s)) && isFinite(s);
  }

  /**
   * 对 X 进行区间上限封顶： thresholdHigh > 0 且 X > thresholdHigh -> 取 thresholdHigh
   */
  function capX(x, thresholdHigh) {
    x = Number(x) || 0;
    var h = Number(thresholdHigh) || 0;
    if (h > 0 && x > h) return h;
    return x;
  }

  /**
   * 根据规则 + 实际 X 值，计算返利比例（返回 0.045 代表 4.5%）
   * rule: { thresholdLow, thresholdHigh, rebateRatio, expression, rewardType }
   * actualX: 根据 rewardType 决定的 X（达成率百分比数值 如 80 表示 80%，或达成额金额，或增长率百分比）
   *
   * 单位约定：
   *   - rebateRatio 存储为小数（0.05 表示 5%），直接返回
   *   - expression 计算结果为百分比数值（如 0.125*(X-60)+2 = 4.5 表示 4.5%），需 /100 转为小数
   *   - expression 为纯数字时也视为百分比数值（如 4.5 表示 4.5%），需 /100
   */
  function ratioByRule(rule, actualX) {
    if (!rule) return 0;
    var expr = (rule.expression || '').trim();
    var ratio = Number(rule.rebateRatio) || 0;
    if (expr) {
      if (isPureNumber(expr)) {
        // 用户直接写返利比例数值（百分比数值，如 4.5 表示 4.5%），需 /100 转小数
        return parseFloat(expr) / 100;
      }
      try {
        var val = evalExpr(expr, actualX);
        if (val == null || isNaN(val)) return ratio;
        // 表达式结果为百分比数值（如 4.5 表示 4.5%），需 /100 转小数
        return val / 100;
      } catch (e) {
        // 表达式解析异常，退回 rebateRatio
        return ratio;
      }
    }
    return ratio;
  }

  /**
   * 按 rewardType 计算 X（达成率/增长率/达成额）
   * 返回 { x, xLabel, base }
   *  base: 返利计算基数（按达成额/达成率/增长率都是 groupActual，按达成率计算比例时比例是 %）
   */
  function computeX(rule, groupActual, groupTarget, prevActual) {
    var type = rule.rewardType || 'PERSENT';
    if (type === 'SCALE') {
      return { x: Number(groupActual) || 0, xLabel: '达成额', base: Number(groupActual) || 0 };
    }
    if (type === 'GROWTH') {
      var p = Number(prevActual) || 0;
      var g = p > 0 ? ((Number(groupActual) || 0) / p - 1) * 100 : 0;
      return { x: g, xLabel: '增长率%', base: Number(groupActual) || 0 };
    }
    // PERSENT 默认
    var t = Number(groupTarget) || 0;
    var rate = t > 0 ? ((Number(groupActual) || 0) / t) * 100 : 0;
    return { x: rate, xLabel: '达成率%', base: Number(groupActual) || 0 };
  }

  /**
   * 单区间匹配：X >= low && (high == 0 || X < high)
   */
  function inRange(x, low, high) {
    x = Number(x) || 0;
    low = Number(low) || 0;
    high = Number(high) || 0;
    return x >= low && (high === 0 || x < high);
  }

  return {
    evalExpr: evalExpr,
    capX: capX,
    ratioByRule: ratioByRule,
    computeX: computeX,
    inRange: inRange,
    isPureNumber: isPureNumber
  };
})();

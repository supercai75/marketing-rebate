-- 上游/下游返利规则表添加表达式字段
-- 表达式中以 X 表示 达成率/销售增长率/达成额（根据 reward_type 确定）
-- 规则：
--   1. 当 X 大于区间上限（threshold_high > 0 时），则用上限代入 X；否则取实际 X
--   2. 表达式为空或表达式值为纯数字：回退使用 rebate_ratio 字段（兼容历史数据）
ALTER TABLE prj_upstream_rebate_rule ADD COLUMN expression VARCHAR(200) DEFAULT NULL COMMENT '返利表达式，如 0.125*(X-60)+2';
ALTER TABLE prj_downstream_rebate_rule ADD COLUMN expression VARCHAR(200) DEFAULT NULL COMMENT '返利表达式，如 0.125*(X-60)+2';

-- 移除 prj_project 表中的 external_coord 和 expected_expense 列
-- 执行时间: 2026-06-03

-- 如果列存在则删除
ALTER TABLE prj_project DROP COLUMN IF EXISTS external_coord;
ALTER TABLE prj_project DROP COLUMN IF EXISTS expected_expense;

-- 添加注释说明
COMMENT ON COLUMN prj_project.expected_cost IS '预计费用(元)';
COMMENT ON COLUMN prj_project.expected_rebate IS '预计收益返利金额(元)';

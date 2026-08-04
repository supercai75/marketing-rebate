-- 为考核组表增加总体目标和分阶段目标字段
ALTER TABLE prj_assess_group 
ADD COLUMN IF NOT EXISTS target_scale NUMERIC(18,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS stage1_target NUMERIC(18,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS stage2_target NUMERIC(18,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS stage3_target NUMERIC(18,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS stage4_target NUMERIC(18,2) DEFAULT 0;

COMMENT ON COLUMN prj_assess_group.target_scale IS '考核组总体目标规模';
COMMENT ON COLUMN prj_assess_group.stage1_target IS '阶段一目标';
COMMENT ON COLUMN prj_assess_group.stage2_target IS '阶段二目标';
COMMENT ON COLUMN prj_assess_group.stage3_target IS '阶段三目标';
COMMENT ON COLUMN prj_assess_group.stage4_target IS '阶段四目标';

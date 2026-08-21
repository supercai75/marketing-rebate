-- 项目阶段-月份对应关系表
-- 当项目合作周期不是整12个月时，由用户自定义每个阶段(S1~S4)对应的月份区间；
-- 整12个月时无需存记录，读取时按"自起始月份每3个月一阶段"的默认规则计算。
CREATE TABLE IF NOT EXISTS prj_stage_month_config (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    stage_code VARCHAR(10) NOT NULL,        -- S1 / S2 / S3 / S4
    start_yyyymm INT NOT NULL,              -- 起始月份(含), 形如 202604
    end_yyyymm INT NOT NULL,                -- 截止月份(含), 形如 202606
    sort_no INT DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_stage_month_config_project ON prj_stage_month_config(project_id);

COMMENT ON TABLE prj_stage_month_config IS '项目阶段与月份区间对应关系(非整12个月周期时由用户定义)';
COMMENT ON COLUMN prj_stage_month_config.project_id IS '项目ID';
COMMENT ON COLUMN prj_stage_month_config.stage_code IS '阶段编码 S1/S2/S3/S4';
COMMENT ON COLUMN prj_stage_month_config.start_yyyymm IS '阶段起始月份(含, YYYYMM)';
COMMENT ON COLUMN prj_stage_month_config.end_yyyymm IS '阶段截止月份(含, YYYYMM)';
COMMENT ON COLUMN prj_stage_month_config.sort_no IS '排序号';

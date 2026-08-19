-- 批次7：流向表新增含税金额列 + proj_flow_set 隐藏表
ALTER TABLE flow_upstream_record ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN flow_upstream_record.tax_amount IS '含税金额';

ALTER TABLE flow_downstream_record ADD COLUMN IF NOT EXISTS tax_amount NUMERIC(18,4) DEFAULT 0;
COMMENT ON COLUMN flow_downstream_record.tax_amount IS '含税金额';

CREATE TABLE IF NOT EXISTS proj_flow_set (
    project_id BIGINT PRIMARY KEY
);
COMMENT ON TABLE proj_flow_set IS '注册项目直接导入下游流向，不使用分解上游';

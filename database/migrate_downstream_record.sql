-- 修改 flow_downstream_record 表，添加 upstream_flow_record_id 字段
-- 同时修改 batch_id 允许为 null
-- 执行前请先备份数据库

-- 1. 添加上游记录ID字段
ALTER TABLE flow_downstream_record ADD COLUMN upstream_flow_record_id BIGINT;

-- 2. 添加外键约束
ALTER TABLE flow_downstream_record ADD CONSTRAINT fk_downstream_upstream 
    FOREIGN KEY (upstream_flow_record_id) REFERENCES flow_upstream_record(id);

-- 3. 添加索引
CREATE INDEX idx_downstream_upstream_id ON flow_downstream_record(upstream_flow_record_id);

-- 4. 修改 batch_id 允许为 null
ALTER TABLE flow_downstream_record ALTER COLUMN batch_id DROP NOT NULL;


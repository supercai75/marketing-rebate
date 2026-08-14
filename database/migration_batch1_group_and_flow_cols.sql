-- ============================================================
-- 数据库迁移脚本（第一批：需求1、3）
-- 数据库：PostgreSQL
-- 执行前请先备份数据库
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 需求1：项目分组
-- ------------------------------------------------------------
-- 1.1 项目分组字典表
CREATE TABLE IF NOT EXISTS prj_project_group (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL UNIQUE,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE prj_project_group IS '项目分组字典';
COMMENT ON COLUMN prj_project_group.name IS '分组名称';

-- 1.2 项目主表新增分组外键
ALTER TABLE prj_project ADD COLUMN IF NOT EXISTS project_group_id BIGINT
    REFERENCES prj_project_group(id) ON DELETE SET NULL;
COMMENT ON COLUMN prj_project.project_group_id IS '所属项目分组ID';

CREATE INDEX IF NOT EXISTS idx_prj_project_group_id ON prj_project(project_group_id);

-- ------------------------------------------------------------
-- 需求3：流向表新增 4 列（客户等级、销售数量、无税金额、中标金额）
--   上游流向表   flow_upstream_record
--   下游流向表   flow_downstream_record
-- ------------------------------------------------------------
-- 上游流向
ALTER TABLE flow_upstream_record ADD COLUMN IF NOT EXISTS customer_level   VARCHAR(50);
ALTER TABLE flow_upstream_record ADD COLUMN IF NOT EXISTS sale_qty         NUMERIC(20,4);
ALTER TABLE flow_upstream_record ADD COLUMN IF NOT EXISTS no_tax_amount    NUMERIC(20,2);
ALTER TABLE flow_upstream_record ADD COLUMN IF NOT EXISTS bid_amount       NUMERIC(20,2);
COMMENT ON COLUMN flow_upstream_record.customer_level IS '客户等级';
COMMENT ON COLUMN flow_upstream_record.sale_qty       IS '销售数量';
COMMENT ON COLUMN flow_upstream_record.no_tax_amount  IS '无税金额';
COMMENT ON COLUMN flow_upstream_record.bid_amount     IS '中标金额';

-- 下游流向
ALTER TABLE flow_downstream_record ADD COLUMN IF NOT EXISTS customer_level VARCHAR(50);
ALTER TABLE flow_downstream_record ADD COLUMN IF NOT EXISTS sale_qty       NUMERIC(20,4);
ALTER TABLE flow_downstream_record ADD COLUMN IF NOT EXISTS no_tax_amount  NUMERIC(20,2);
ALTER TABLE flow_downstream_record ADD COLUMN IF NOT EXISTS bid_amount     NUMERIC(20,2);
COMMENT ON COLUMN flow_downstream_record.customer_level IS '客户等级（由上游分解/导入时带入）';
COMMENT ON COLUMN flow_downstream_record.sale_qty       IS '销售数量';
COMMENT ON COLUMN flow_downstream_record.no_tax_amount  IS '无税金额';
COMMENT ON COLUMN flow_downstream_record.bid_amount     IS '中标金额';

COMMIT;

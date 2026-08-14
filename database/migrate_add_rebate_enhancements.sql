-- 需求4: 返利计算增强
-- 1. 协议表新增"返利计算依据"字段（数量/销售数量/核算金额/中标金额）
ALTER TABLE prj_upstream_agreement ADD COLUMN IF NOT EXISTS rebate_calc_basis VARCHAR(16) DEFAULT 'QTY';
ALTER TABLE prj_downstream_agreement ADD COLUMN IF NOT EXISTS rebate_calc_basis VARCHAR(16) DEFAULT 'QTY';

-- 2. 返利规则表新增"计算模式"和"共享考核组"字段
ALTER TABLE prj_upstream_rebate_rule ADD COLUMN IF NOT EXISTS calc_mode VARCHAR(16) DEFAULT 'PROGRESSIVE';
ALTER TABLE prj_upstream_rebate_rule ADD COLUMN IF NOT EXISTS shared_group_ids VARCHAR(256);
ALTER TABLE prj_downstream_rebate_rule ADD COLUMN IF NOT EXISTS calc_mode VARCHAR(16) DEFAULT 'PROGRESSIVE';
ALTER TABLE prj_downstream_rebate_rule ADD COLUMN IF NOT EXISTS shared_group_ids VARCHAR(256);

-- 3. 下游返利规则表补加 expression 字段（如未存在）
ALTER TABLE prj_downstream_rebate_rule ADD COLUMN IF NOT EXISTS expression TEXT;

-- 需求6: 应收/应付考核明细表（依据考核应收/应付展开为表格）
CREATE TABLE IF NOT EXISTS prj_receivable_assess_item (
    id              BIGSERIAL PRIMARY KEY,
    receivable_id   BIGINT NOT NULL,
    item_type       VARCHAR(32) NOT NULL,  -- STAFF_COUNT/VISIT_COUNT/MEETING/OTHER
    item_name       VARCHAR(128),
    remark          VARCHAR(512),
    target_value    NUMERIC(18,4),
    actual_value    NUMERIC(18,4),
    reward_amount   NUMERIC(18,4),
    attach_file_id  BIGINT,
    sort_no         INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recv_assess_item ON prj_receivable_assess_item(receivable_id);

CREATE TABLE IF NOT EXISTS prj_payable_assess_item (
    id              BIGSERIAL PRIMARY KEY,
    payable_id      BIGINT NOT NULL,
    item_type       VARCHAR(32) NOT NULL,
    item_name       VARCHAR(128),
    remark          VARCHAR(512),
    target_value    NUMERIC(18,4),
    actual_value    NUMERIC(18,4),
    reward_amount   NUMERIC(18,4),
    attach_file_id  BIGINT,
    sort_no         INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pay_assess_item ON prj_payable_assess_item(payable_id);

-- ============================================================
-- 营销返利项目管理系统 - PostgreSQL 数据库表结构
-- 数据库: rebate_db
-- 说明: 包含用户管理、立项、协议、流向、费用、人工、应收/应付、实收/实付、概览、平衡表等
-- ============================================================

-- 1. 部门/公司字典
CREATE TABLE IF NOT EXISTS sys_department (
    id              BIGSERIAL PRIMARY KEY,
    dept_code       VARCHAR(64)  NOT NULL UNIQUE,
    dept_name       VARCHAR(128) NOT NULL,
    parent_id       BIGINT       DEFAULT 0,
    sort_no         INT          DEFAULT 0,
    status          SMALLINT     DEFAULT 1, -- 1启用 0停用
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_department IS '部门表';

CREATE TABLE IF NOT EXISTS sys_company (
    id              BIGSERIAL PRIMARY KEY,
    company_code    VARCHAR(64)  NOT NULL UNIQUE,
    company_name    VARCHAR(128) NOT NULL,
    status          SMALLINT     DEFAULT 1,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_company IS '所属公司表';

-- 2. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL PRIMARY KEY,
    work_no         VARCHAR(64)  NOT NULL UNIQUE,
    name            VARCHAR(64)  NOT NULL,
    login_name      VARCHAR(64)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    dept_id         BIGINT,
    company_id      BIGINT,
    phone           VARCHAR(32),
    email           VARCHAR(128),
    status          SMALLINT     DEFAULT 1, -- 1启用 0停用
    is_admin        SMALLINT     DEFAULT 0,
    role_id         BIGINT,
    last_login_time TIMESTAMP,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_user IS '系统用户表';
CREATE INDEX idx_user_dept ON sys_user(dept_id);

-- 3. 角色/功能权限
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGSERIAL PRIMARY KEY,
    role_code       VARCHAR(64) NOT NULL UNIQUE,
    role_name       VARCHAR(128) NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE sys_role IS '角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id              BIGSERIAL PRIMARY KEY,
    perm_code       VARCHAR(128) NOT NULL UNIQUE,  -- e.g. project:edit
    perm_name       VARCHAR(128) NOT NULL,
    module          VARCHAR(64),
    description     VARCHAR(255)
);
COMMENT ON TABLE sys_permission IS '权限/功能点表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id         BIGINT NOT NULL,
    perm_id         BIGINT NOT NULL,
    PRIMARY KEY (role_id, perm_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 4. 项目立项主表
CREATE TABLE IF NOT EXISTS prj_project (
    id                  BIGSERIAL PRIMARY KEY,
    project_code        VARCHAR(64),                 -- 内部项目编号
    project_name        VARCHAR(255) NOT NULL,        -- 项目名称（与BPM同步）
    brand               VARCHAR(255),                 -- 签约厂牌
    co_product          VARCHAR(255),                 -- 合作品种
    co_mode             VARCHAR(64),                  -- 合作模式
    co_year             VARCHAR(8),                  -- 合作年度 4位数字如2024
    period_start_date   DATE,
    period_end_date     DATE,
    region              VARCHAR(255),                 -- 覆盖地区
    target_scale        NUMERIC(18,2) DEFAULT 0,      -- 项目目标规模
    expected_rebate     NUMERIC(18,2) DEFAULT 0,      -- 预计收益返利金额
    expected_cost       NUMERIC(18,2) DEFAULT 0,      -- 预计费用
    description         TEXT,
    bpm_process_id      VARCHAR(64),                  -- BPM流程实例ID
    bpm_project_id      VARCHAR(64),                  -- BPM立项ID
    bpm_synced          SMALLINT     DEFAULT 0,       -- 是否已从BPM同步
    status              VARCHAR(32)  DEFAULT 'NEW',   -- NEW / RUNNING / CLOSED
    owner_user_id       BIGINT,                       -- 项目负责人
    created_by          BIGINT,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_name, period_start_date)
);
COMMENT ON TABLE prj_project IS '营销项目主表';

-- 5. 项目作业人员
CREATE TABLE IF NOT EXISTS prj_project_person (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    work_no             VARCHAR(64),
    name                VARCHAR(64) NOT NULL,
    dept_name           VARCHAR(128),
    position            VARCHAR(128),
    work_type           VARCHAR(16) DEFAULT 'FULL',  -- FULL(全职) / PART(兼职) / OUTSOURCE(外包)
    labor_cost_ratio    NUMERIC(5,2) DEFAULT 0,     -- 人工费用比例(%)
    expense_ratio       NUMERIC(5,2) DEFAULT 0,     -- 报销费用比例(%)
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_person_project ON prj_project_person(project_id);
CREATE INDEX idx_person_workno  ON prj_project_person(work_no);
COMMENT ON TABLE prj_project_person IS '项目作业人员表（人员可跨项目，按比例分摊费用）';

-- 6. 上游协议（含版本）
CREATE TABLE IF NOT EXISTS prj_upstream_agreement (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    version             INT     NOT NULL DEFAULT 1,   -- 版本号
    is_current          SMALLINT DEFAULT 1,            -- 是否最新版本 1=是 0=否
    bpm_agree_id        VARCHAR(64),                   -- BPM 协议ID
    agreement_name      VARCHAR(255) NOT NULL,
    agreement_no        VARCHAR(128) NOT NULL,
    period_start_date   DATE,
    period_end_date     DATE,
    region              VARCHAR(255),
    target_terminal     VARCHAR(255),
    calc_basis          VARCHAR(16),  -- QTY(数量) / AMT(金额)
    target_scale        NUMERIC(18,2) DEFAULT 0,
    calc_method         TEXT,
    supplier            VARCHAR(255),  -- 合作上游企业
    target_dept         VARCHAR(128),  -- 指标承接部门
    flow_contact        VARCHAR(64),
    flow_phone          VARCHAR(32),
    flow_channel        VARCHAR(128),
    flow_provide_method VARCHAR(128),
    stage1_target       NUMERIC(18,2) DEFAULT 0,
    stage2_target       NUMERIC(18,2) DEFAULT 0,
    stage3_target       NUMERIC(18,2) DEFAULT 0,
    stage4_target       NUMERIC(18,2) DEFAULT 0,
    owner_user_id       BIGINT,
    policy_detail       TEXT,           -- 营销政策明细
    rebate_calc_rule    TEXT,           -- 返利计算方式（分阶段+比例+达成规模）
    settle_basis        VARCHAR(64),    -- 预付/按阶段/全年
    settle_ratio        VARCHAR(255),   -- 各支付时点比例 JSON: [{"point":"期初","ratio":0.2}]
    rebate_pay_type     VARCHAR(32),    -- 票折/服务费
    rebate_pay_time     VARCHAR(255),
    team_assess_settle  TEXT,           -- 专职团队考核结算方式
    required_staff_num  INT DEFAULT 0,
    formal_count        INT DEFAULT 0,
    formal_names        TEXT,
    informal_count      INT DEFAULT 0,
    informal_names      TEXT,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_upstream_project ON prj_upstream_agreement(project_id);
CREATE UNIQUE INDEX uq_upstream_version ON prj_upstream_agreement(project_id, agreement_no, version);
COMMENT ON TABLE prj_upstream_agreement IS '上游协议表（含版本与历史）';

-- 上游协议 - 专职团队考核目标子表
CREATE TABLE IF NOT EXISTS prj_upstream_team_target (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    target_name     VARCHAR(255) NOT NULL,
    owner           VARCHAR(64),
    requirement     TEXT,           -- 1000字以内
    calc_standard   TEXT,
    reward_standard TEXT,
    sort_no         INT DEFAULT 0
);
CREATE INDEX idx_upstream_team_agree ON prj_upstream_team_target(agreement_id);

-- 上游协议 - 备注附件
CREATE TABLE IF NOT EXISTS prj_upstream_remark_file (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    file_type       VARCHAR(32) NOT NULL, -- PRODUCT/ HOSPITAL/ BLACKLIST / OTHER
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 上游协议 - 协议附件
CREATE TABLE IF NOT EXISTS prj_upstream_attach (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    attach_type     VARCHAR(32) NOT NULL, -- MAIN(合作协议) / SUPP(补充协议)
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 上游协议 - 返利计算规则子表（分阶段、分达成规模，按比例计算）
CREATE TABLE IF NOT EXISTS prj_upstream_rebate_rule (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    stage_code      VARCHAR(16) NOT NULL,     -- S1/S2/S3/S4
    threshold_low   NUMERIC(18,4) DEFAULT 0,  -- 达成规模下限（含）
    threshold_high  NUMERIC(18,4) DEFAULT 0,  -- 达成规模上限（不含，0表示无穷大）
    rebate_ratio    NUMERIC(8,4) NOT NULL,    -- 返利比例（如 0.05 表示 5%）
    reward_type     VARCHAR(16) DEFAULT 'SCALE', -- SCALE(按规模) / ASSESS(按考核)
    assess_group_id BIGINT,                   -- 关联考核组ID（可为空）
    sort_no         INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_rebate_rule_agreement ON prj_upstream_rebate_rule(agreement_id);
COMMENT ON TABLE prj_upstream_rebate_rule IS '上游协议返利计算规则子表';

-- 考核组表（固定考核维度）
CREATE TABLE IF NOT EXISTS prj_assess_group (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    group_code      VARCHAR(64) NOT NULL,
    group_name      VARCHAR(128) NOT NULL,
    description     VARCHAR(512),
    target_scale    NUMERIC(18,2) DEFAULT 0,
    stage1_target   NUMERIC(18,2) DEFAULT 0,
    stage2_target   NUMERIC(18,2) DEFAULT 0,
    stage3_target   NUMERIC(18,2) DEFAULT 0,
    stage4_target   NUMERIC(18,2) DEFAULT 0,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_assess_group_project ON prj_assess_group(project_id);
CREATE UNIQUE INDEX uq_assess_group_project_code ON prj_assess_group(project_id, group_code);
COMMENT ON TABLE prj_assess_group IS '考核组表（按项目隔离）';
COMMENT ON COLUMN prj_assess_group.target_scale IS '考核组总体目标规模';
COMMENT ON COLUMN prj_assess_group.stage1_target IS '阶段一目标';
COMMENT ON COLUMN prj_assess_group.stage2_target IS '阶段二目标';
COMMENT ON COLUMN prj_assess_group.stage3_target IS '阶段三目标';
COMMENT ON COLUMN prj_assess_group.stage4_target IS '阶段四目标';

-- 考核组指标项
CREATE TABLE IF NOT EXISTS prj_assess_item (
    id              BIGSERIAL PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    item_code       VARCHAR(64) NOT NULL,
    item_name       VARCHAR(128) NOT NULL,
    calc_basis      VARCHAR(16) DEFAULT 'AMOUNT', -- AMOUNT(金额) / QTY(数量) / RATE(比率)
    target_value    NUMERIC(18,4),               -- 目标值
    weight          NUMERIC(5,2) DEFAULT 1,      -- 权重
    sort_no         INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_assess_item_group ON prj_assess_item(group_id);
COMMENT ON TABLE prj_assess_item IS '考核组指标项';

-- 7. 下游协议
CREATE TABLE IF NOT EXISTS prj_downstream_agreement (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    upstream_id         BIGINT NOT NULL,          -- 关联上游协议ID
    version             INT     NOT NULL DEFAULT 1,
    is_current          SMALLINT DEFAULT 1,
    bpm_agree_id        VARCHAR(64),
    upstream_name       VARCHAR(255),
    upstream_no         VARCHAR(128),
    agreement_name      VARCHAR(255) NOT NULL,
    agreement_no        VARCHAR(128) NOT NULL,
    period_start_date   DATE,
    period_end_date     DATE,
    region              VARCHAR(255),
    target_terminal     VARCHAR(255),
    calc_basis          VARCHAR(16),  -- 从上游带过来
    target_scale        NUMERIC(18,2) DEFAULT 0,
    calc_method         TEXT,
    distributor         VARCHAR(255),  -- 承接分销企业
    distributor_type    VARCHAR(32),   -- 内部公司 / 外部公司
    target_dept         VARCHAR(128),
    flow_contact        VARCHAR(64),
    flow_phone          VARCHAR(32),
    flow_channel        VARCHAR(128),
    flow_provide_method VARCHAR(128),
    stage1_target       NUMERIC(18,2) DEFAULT 0,
    stage2_target       NUMERIC(18,2) DEFAULT 0,
    stage3_target       NUMERIC(18,2) DEFAULT 0,
    stage4_target       NUMERIC(18,2) DEFAULT 0,
    owner_user_id       BIGINT,
    policy_detail       TEXT,
    rebate_calc_rule    TEXT,
    settle_basis        VARCHAR(64),
    settle_ratio        VARCHAR(255),
    rebate_pay_type     VARCHAR(32),
    rebate_pay_time     VARCHAR(255),
    team_assess_settle  TEXT,
    required_staff_num  INT DEFAULT 0,
    formal_count        INT DEFAULT 0,
    formal_names        TEXT,
    informal_count      INT DEFAULT 0,
    informal_names      TEXT,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_downstream_project ON prj_downstream_agreement(project_id);
CREATE INDEX idx_downstream_upstream ON prj_downstream_agreement(upstream_id);
CREATE UNIQUE INDEX uq_downstream_version ON prj_downstream_agreement(project_id, agreement_no, version);

-- 下游协议 - 专职团队考核
CREATE TABLE IF NOT EXISTS prj_downstream_team_target (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    target_name     VARCHAR(255) NOT NULL,
    owner           VARCHAR(64),
    requirement     TEXT,
    calc_standard   TEXT,
    reward_standard TEXT,
    sort_no         INT DEFAULT 0
);

-- 下游协议 - 备注附件
CREATE TABLE IF NOT EXISTS prj_downstream_remark_file (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    file_type       VARCHAR(32) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 下游协议 - 协议附件
CREATE TABLE IF NOT EXISTS prj_downstream_attach (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    attach_type     VARCHAR(32) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT,
    uploaded_by     BIGINT,
    uploaded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 下游协议 - 返利计算规则子表
CREATE TABLE IF NOT EXISTS prj_downstream_rebate_rule (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    stage_code      VARCHAR(16) NOT NULL,
    threshold_low   NUMERIC(18,4) DEFAULT 0,
    threshold_high  NUMERIC(18,4) DEFAULT 0,
    rebate_ratio    NUMERIC(8,4) NOT NULL,
    reward_type     VARCHAR(16) DEFAULT 'SCALE',
    assess_group_id BIGINT,
    sort_no         INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_downstream_rebate_rule ON prj_downstream_rebate_rule(agreement_id);

-- 8. 上游流向
-- 8.1 流向批次（每次导入一个批次）
CREATE TABLE IF NOT EXISTS flow_upstream_batch (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    batch_code      VARCHAR(64),                -- 系统生成批次号
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512),
    import_user     BIGINT,
    import_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    month_summary   VARCHAR(255),                -- 该批次覆盖的月份描述
    remark          VARCHAR(255)
);
CREATE INDEX idx_upstream_batch_project ON flow_upstream_batch(project_id);

-- 8.2 流向明细（一个文件可能覆盖1~n月）
CREATE TABLE IF NOT EXISTS flow_upstream_record (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    month_yyyymm    VARCHAR(8) NOT NULL,         -- yyyyMM
    business_date   DATE,
    product_name    VARCHAR(255),
    spec            VARCHAR(128),
    seller_name     VARCHAR(255),
    seller_city     VARCHAR(64),
    calc_price      NUMERIC(18,4) DEFAULT 0,
    quantity        NUMERIC(18,4) DEFAULT 0,
    calc_amount     NUMERIC(18,4) DEFAULT 0,
    buyer_name      VARCHAR(255),
    assess_group_id BIGINT,                      -- 考核组ID
    is_valid        SMALLINT DEFAULT 1,          -- 1有效 0失效（同月后续导入置为失效）
    is_final        SMALLINT DEFAULT 0,          -- 1终版 营销手动设定
    raw_row         TEXT,                        -- 原始行JSON（<150列）
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_upstream_flow_project_month ON flow_upstream_record(project_id, month_yyyymm);
CREATE INDEX idx_upstream_flow_batch ON flow_upstream_record(batch_id);
COMMENT ON TABLE flow_upstream_record IS '上游流向明细表（每月份保留最近一次导入的版本，前置版本is_valid=0）';

-- 8.3 月份级终版设定
CREATE TABLE IF NOT EXISTS flow_upstream_final (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    month_yyyymm    VARCHAR(8) NOT NULL,
    set_user        BIGINT,
    set_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, month_yyyymm)
);
COMMENT ON TABLE flow_upstream_final IS '上游流向月份终版设置';

-- 9. 下游流向（结构同上游）
CREATE TABLE IF NOT EXISTS flow_downstream_batch (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    agreement_id    BIGINT NOT NULL,
    batch_code      VARCHAR(64),
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512),
    import_user     BIGINT,
    import_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    month_summary   VARCHAR(255),
    remark          VARCHAR(255)
);
CREATE INDEX idx_downstream_batch_project ON flow_downstream_batch(project_id);
CREATE INDEX idx_downstream_batch_agree ON flow_downstream_batch(agreement_id);

CREATE TABLE IF NOT EXISTS flow_downstream_record (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    agreement_id    BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    month_yyyymm    VARCHAR(8) NOT NULL,
    business_date   DATE,
    product_name    VARCHAR(255),
    spec            VARCHAR(128),
    seller_name     VARCHAR(255),
    seller_city     VARCHAR(64),
    calc_price      NUMERIC(18,4) DEFAULT 0,
    quantity        NUMERIC(18,4) DEFAULT 0,
    calc_amount     NUMERIC(18,4) DEFAULT 0,
    buyer_name      VARCHAR(255),
    assess_group_id BIGINT,                      -- 考核组ID
    is_valid        SMALLINT DEFAULT 1,
    is_final        SMALLINT DEFAULT 0,
    raw_row         TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_downstream_flow_agree_month ON flow_downstream_record(agreement_id, month_yyyymm);

CREATE TABLE IF NOT EXISTS flow_downstream_final (
    id              BIGSERIAL PRIMARY KEY,
    agreement_id    BIGINT NOT NULL,
    month_yyyymm    VARCHAR(8) NOT NULL,
    set_user        BIGINT,
    set_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agreement_id, month_yyyymm)
);

-- 10. 分解记录（营销手动分解上游流向到下游分销商）
CREATE TABLE IF NOT EXISTS flow_split_record (
    id              BIGSERIAL PRIMARY KEY,
    upstream_id     BIGINT NOT NULL,        -- 上游record.id
    project_id      BIGINT NOT NULL,
    agreement_id    BIGINT NOT NULL,        -- 下游协议id
    split_qty       NUMERIC(18,4) DEFAULT 0,
    split_amount    NUMERIC(18,4) DEFAULT 0,
    split_user      BIGINT,
    split_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_split_upstream ON flow_split_record(upstream_id);
CREATE INDEX idx_split_agree ON flow_split_record(agreement_id);

-- 11. 项目费用投入（财务导入）
CREATE TABLE IF NOT EXISTS fin_project_expense (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT,                 -- 系统分摊后写入
    reimburse_date  DATE NOT NULL,
    expense_type    VARCHAR(64),
    work_no         VARCHAR(64),
    name            VARCHAR(64),
    description     VARCHAR(512),
    amount          NUMERIC(18,2) DEFAULT 0,
    allocated_amount NUMERIC(18,2) DEFAULT 0,    -- 本项目分摊金额
    source          VARCHAR(16) DEFAULT 'IMPORT',  -- IMPORT/INPUT
    raw_project_name VARCHAR(255),           -- 导入时填写的项目名
    doc_no          VARCHAR(128),
    matched_type    VARCHAR(32),             -- PROJECT_NAME / PERSON_SPLIT / UNMATCHED
    import_user     BIGINT,
    import_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(255)
);
CREATE INDEX idx_expense_project ON fin_project_expense(project_id);
CREATE INDEX idx_expense_workno ON fin_project_expense(work_no);
CREATE INDEX idx_expense_date ON fin_project_expense(reimburse_date);

-- 12. 项目人工投入（人力导入）
CREATE TABLE IF NOT EXISTS fin_project_labor (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT,
    month_yyyymm    VARCHAR(8) NOT NULL,
    work_no         VARCHAR(64),
    name            VARCHAR(64),
    work_type       VARCHAR(16) DEFAULT 'FULL',  -- FULL/PART/OUTSOURCE
    salary          NUMERIC(18,2) DEFAULT 0,
    welfare         NUMERIC(18,2) DEFAULT 0,
    other_cost      NUMERIC(18,2) DEFAULT 0,
    total_cost      NUMERIC(18,2) DEFAULT 0,
    allocated_amount NUMERIC(18,2) DEFAULT 0,    -- 本项目分摊金额
    alloc_ratio     NUMERIC(8,4) DEFAULT 1,     -- 本项目分摊比例
    source          VARCHAR(16) DEFAULT 'IMPORT',
    matched_type    VARCHAR(32),
    import_user     BIGINT,
    import_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(255)
);
CREATE INDEX idx_labor_project ON fin_project_labor(project_id);
CREATE INDEX idx_labor_workno ON fin_project_labor(work_no);

-- 13. 应收
CREATE TABLE IF NOT EXISTS prj_receivable (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage               VARCHAR(16) NOT NULL,         -- STAGE1/2/3/4
    scale_amount        NUMERIC(18,2) DEFAULT 0,      -- 依据规模
    assess_amount       NUMERIC(18,2) DEFAULT 0,      -- 依据考核
    total_amount        NUMERIC(18,2) DEFAULT 0,
    estimate_amount     NUMERIC(18,2) DEFAULT 0,      -- 系统估算
    tax_rate            NUMERIC(5,2) DEFAULT 0,        -- 税率(百分比)
    status              VARCHAR(32) DEFAULT 'DRAFT',  -- DRAFT/AUDIT/FINAL
    fill_user           BIGINT,
    fill_time           TIMESTAMP,
    audit_user          BIGINT,
    audit_time          TIMESTAMP,
    remark              VARCHAR(512)
);
CREATE INDEX idx_receivable_project ON prj_receivable(project_id);

-- 14. 应付
CREATE TABLE IF NOT EXISTS prj_payable (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    agreement_id        BIGINT NOT NULL,                -- 下游协议
    stage               VARCHAR(16) NOT NULL,
    scale_amount        NUMERIC(18,2) DEFAULT 0,
    assess_amount       NUMERIC(18,2) DEFAULT 0,
    total_amount        NUMERIC(18,2) DEFAULT 0,
    estimate_amount     NUMERIC(18,2) DEFAULT 0,
    tax_rate            NUMERIC(5,2) DEFAULT 0,        -- 税率(百分比)
    status              VARCHAR(32) DEFAULT 'DRAFT',
    fill_user           BIGINT,
    fill_time           TIMESTAMP,
    audit_user          BIGINT,
    audit_time          TIMESTAMP,
    confirm_user        BIGINT,                          -- 财务确认
    confirm_time        TIMESTAMP,
    remark              VARCHAR(512)
);
CREATE INDEX idx_payable_project ON prj_payable(project_id);
CREATE INDEX idx_payable_agreement ON prj_payable(agreement_id);

-- 15. 实收
CREATE TABLE IF NOT EXISTS prj_received (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    stage               VARCHAR(16),
    rebate_type         VARCHAR(16),                     -- TICKET(票折) / SERVICE(服务费)
    applicant           VARCHAR(64),
    apply_dept          VARCHAR(128),
    apply_date          DATE,
    finance_code        VARCHAR(64),
    rebate_amount       NUMERIC(18,2) DEFAULT 0,
    tax_rate            NUMERIC(5,2) DEFAULT 0,
    total_price_tax     NUMERIC(18,2) DEFAULT 0,         -- 价税合计
    dept_share          NUMERIC(18,2) DEFAULT 0,         -- 本部门应得
    invoice_no          VARCHAR(64),
    receive_dept        VARCHAR(128),
    status              VARCHAR(32) DEFAULT 'DRAFT',     -- DRAFT/PURCHASE_OK/OP_OK/FIN_OK/FINAL
    bpm_process_id      VARCHAR(64),
    purchase_user       BIGINT,
    purchase_time       TIMESTAMP,
    op_user             BIGINT,
    op_time             TIMESTAMP,
    finance_user        BIGINT,
    finance_time        TIMESTAMP,
    final_time          TIMESTAMP,
    remark              VARCHAR(512)
);
CREATE INDEX idx_received_project ON prj_received(project_id);

-- 16. 实付
CREATE TABLE IF NOT EXISTS prj_paid (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL,
    agreement_id        BIGINT,
    stage               VARCHAR(16),
    rebate_type         VARCHAR(16),
    applicant           VARCHAR(64),
    apply_dept          VARCHAR(128),
    apply_date          DATE,
    receive_dept        VARCHAR(128),
    customer_name       VARCHAR(255),
    total_rebate        NUMERIC(18,2) DEFAULT 0,
    actual_rebate       NUMERIC(18,2) DEFAULT 0,
    diff_amount         NUMERIC(18,2) DEFAULT 0,
    execute_status      VARCHAR(32) DEFAULT 'DRAFT',
    bpm_process_id      VARCHAR(64),
    op_user             BIGINT,
    op_time             TIMESTAMP,
    finance_user        BIGINT,
    finance_time        TIMESTAMP,
    final_time          TIMESTAMP,
    remark              VARCHAR(512)
);
CREATE INDEX idx_paid_project ON prj_paid(project_id);
CREATE INDEX idx_paid_agreement ON prj_paid(agreement_id);

-- 17. 操作日志
CREATE TABLE IF NOT EXISTS sys_op_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    login_name      VARCHAR(64),
    module          VARCHAR(64),
    action          VARCHAR(64),
    content         TEXT,
    ip              VARCHAR(64),
    op_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_op_log_user ON sys_op_log(user_id);

-- ============================================================
-- 批次2：共享考核组移至考核组 + 计算模式移至协议整体
-- ============================================================

-- 1. 考核组表新增"共享考核组"字段（存储共享目标的其他考核组ID列表）
ALTER TABLE prj_assess_group ADD COLUMN IF NOT EXISTS shared_group_ids VARCHAR(256);
COMMENT ON COLUMN prj_assess_group.shared_group_ids IS '共享考核组ID列表（逗号分隔，如 "2,3"，表示本组与这些组共享目标，达成率按合计实际值/合计目标值计算）';

-- 2. 上游/下游协议表新增"计算模式"字段（PROGRESSIVE 递进式 / FLAT 全部计算）
ALTER TABLE prj_upstream_agreement ADD COLUMN IF NOT EXISTS calc_mode VARCHAR(16) DEFAULT 'PROGRESSIVE';
ALTER TABLE prj_downstream_agreement ADD COLUMN IF NOT EXISTS calc_mode VARCHAR(16) DEFAULT 'PROGRESSIVE';
COMMENT ON COLUMN prj_upstream_agreement.calc_mode IS '返利计算模式：PROGRESSIVE(递进式分段累计) / FLAT(全部按匹配区间比例计算)';
COMMENT ON COLUMN prj_downstream_agreement.calc_mode IS '返利计算模式：PROGRESSIVE(递进式分段累计) / FLAT(全部按匹配区间比例计算)';

-- 3. 数据迁移：将原有 RebateRule.shared_group_ids 迁移到 AssessGroup.shared_group_ids
--    规则：对于每个 assess_group_id 非空的考核组，如果其规则上配置了 shared_group_ids，则将该值同步到考核组
UPDATE prj_assess_group g SET shared_group_ids = sub.sids
FROM (
    SELECT DISTINCT assess_group_id, shared_group_ids AS sids
    FROM prj_upstream_rebate_rule
    WHERE assess_group_id IS NOT NULL AND shared_group_ids IS NOT NULL AND shared_group_ids != ''
) sub
WHERE g.id = sub.assess_group_id AND (g.shared_group_ids IS NULL OR g.shared_group_ids = '');

UPDATE prj_assess_group g SET shared_group_ids = sub.sids
FROM (
    SELECT DISTINCT assess_group_id, shared_group_ids AS sids
    FROM prj_downstream_rebate_rule
    WHERE assess_group_id IS NOT NULL AND shared_group_ids IS NOT NULL AND shared_group_ids != ''
) sub
WHERE g.id = sub.assess_group_id AND (g.shared_group_ids IS NULL OR g.shared_group_ids = '');

-- 4. 数据迁移：将原有 RebateRule.calc_mode 迁移到协议表（取每协议下第一条非空规则的模式）
UPDATE prj_upstream_agreement a SET calc_mode = sub.cm
FROM (
    SELECT agreement_id, MIN(calc_mode) AS cm
    FROM prj_upstream_rebate_rule
    WHERE calc_mode IS NOT NULL AND calc_mode != ''
    GROUP BY agreement_id
) sub
WHERE a.id = sub.agreement_id AND (a.calc_mode IS NULL OR a.calc_mode = '');

UPDATE prj_downstream_agreement a SET calc_mode = sub.cm
FROM (
    SELECT agreement_id, MIN(calc_mode) AS cm
    FROM prj_downstream_rebate_rule
    WHERE calc_mode IS NOT NULL AND calc_mode != ''
    GROUP BY agreement_id
) sub
WHERE a.id = sub.agreement_id AND (a.calc_mode IS NULL OR a.calc_mode = '');

-- 5. 规则表的 shared_group_ids / calc_mode 字段保留（向后兼容），但新版本不再在规则行编辑
--    计算逻辑改为优先读 AssessGroup.shared_group_ids 和 Agreement.calc_mode

-- 说明：本次不删除规则表上的 shared_group_ids / calc_mode 列，避免影响旧数据读取；
--       RebateCalcService 改造后，规则字段仅作回退读取。

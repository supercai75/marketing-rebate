-- ============================================================
-- 下游协议新版本更替历史数据修复脚本
-- 功能：将各业务表的 agreement_id，统一更新为「同项目+同协议编号」下 is_current=1 的版本ID
-- 使用说明：
--   1. 先部署代码（doAdd 中会自动迁移新的版本更替）
--   2. 对历史已发生多次版本更替、已经出现数据错位的库，执行本脚本一次
--   3. 建议执行前先备份数据库
-- 数据库：PostgreSQL
-- ============================================================

BEGIN;

-- 1. 构建临时映射表：每个旧协议ID -> 对应同编号同项目下 current 版本的新ID
CREATE TEMP TABLE ds_agree_migrate_map AS
SELECT
    old.id                  AS old_id,
    cur.id                  AS new_id,
    old.project_id,
    old.agreement_no
FROM prj_downstream_agreement old
JOIN prj_downstream_agreement cur
  ON cur.project_id = old.project_id
 AND cur.agreement_no = old.agreement_no
 AND cur.is_current = 1
WHERE old.id <> cur.id;

-- 2. 验证映射数量（可注释掉）
-- SELECT count(*) AS need_migrate_count FROM ds_agree_migrate_map;

-- 3. 迁移各业务表
-- 下游流向批次
UPDATE flow_downstream_batch t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 下游流向记录
UPDATE flow_downstream_record t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 下游流向定案
UPDATE flow_downstream_final t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 分解记录
UPDATE flow_split_record t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 应付
UPDATE prj_payable t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 实付
UPDATE prj_paid t
   SET agreement_id = m.new_id
  FROM ds_agree_migrate_map m
 WHERE t.agreement_id = m.old_id;

-- 4. 清理临时表
DROP TABLE ds_agree_migrate_map;

COMMIT;

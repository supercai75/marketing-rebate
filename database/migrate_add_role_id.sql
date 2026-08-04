-- 添加 role_id 列到 sys_user 表
-- 执行时间: 2026-06-03

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS role_id BIGINT;

COMMENT ON COLUMN sys_user.role_id IS '关联角色ID';

-- 添加费用编辑和人工编辑权限
-- 执行日期: 2026-07-13

-- 1. 添加缺失的权限点
INSERT INTO sys_permission (perm_code, perm_name, module) VALUES
('expense:edit', '编辑费用', '费用管理'),
('labor:edit', '编辑人工', '人工管理')
ON CONFLICT (perm_code) DO NOTHING;

-- 2. 给 FIN_USER (财务用户) 添加费用编辑权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='FIN_USER' AND p.perm_code IN ('expense:edit', 'labor:edit')
ON CONFLICT DO NOTHING;

-- 3. 给 HR_USER (人力用户) 添加人工编辑权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='HR_USER' AND p.perm_code = 'labor:edit'
ON CONFLICT DO NOTHING;
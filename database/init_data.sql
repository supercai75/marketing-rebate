-- ============================================================
-- 营销返利项目管理系统 - 初始化数据
-- ============================================================

-- 默认部门
INSERT INTO sys_department (dept_code, dept_name) VALUES
('OPS', '运营部门'),
('MKT', '营销部门'),
('FIN', '财务部门'),
('PUR', '采购部门'),
('HR', '人力部门')
ON CONFLICT (dept_code) DO NOTHING;

-- 默认公司
INSERT INTO sys_company (company_code, company_name) VALUES
('HQ', '总部')
ON CONFLICT (company_code) DO NOTHING;

-- 默认角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '拥有全部功能'),
('MKT_USER', '营销用户', '项目/协议/流向/应收应付'),
('OPS_USER', '运营用户', '审核/确认'),
('FIN_USER', '财务用户', '费用/人工/实收实付'),
('HR_USER', '人力用户', '人工成本'),
('PUR_USER', '采购用户', '实收确认')
ON CONFLICT (role_code) DO NOTHING;

-- 默认权限点
INSERT INTO sys_permission (perm_code, perm_name, module) VALUES
('user:view', '查看用户', '用户管理'),
('user:edit', '编辑用户', '用户管理'),
('project:view', '查看项目', '项目管理'),
('project:edit', '编辑项目', '项目管理'),
('project:import', '引入立项', '项目管理'),
('agreement:view', '查看协议', '协议管理'),
('agreement:edit', '编辑协议', '协议管理'),
('agreement:import', '引入协议', '协议管理'),
('flow:view', '查看流向', '流向管理'),
('flow:import', '导入流向', '流向管理'),
('flow:split', '分解流向', '流向管理'),
('flow:final', '设定终版', '流向管理'),
('expense:view', '查看费用', '费用管理'),
('expense:edit', '编辑费用', '费用管理'),
('expense:import', '导入费用', '费用管理'),
('labor:view', '查看人工', '人工管理'),
('labor:edit', '编辑人工', '人工管理'),
('labor:import', '导入人工', '人工管理'),
('receivable:view', '查看应收', '应收管理'),
('receivable:edit', '编辑应收', '应收管理'),
('receivable:audit', '审核应收', '应收管理'),
('payable:view', '查看应付', '应付管理'),
('payable:edit', '编辑应付', '应付管理'),
('payable:audit', '审核应付', '应付管理'),
('received:view', '查看实收', '实收管理'),
('received:edit', '编辑实收', '实收管理'),
('paid:view', '查看实付', '实付管理'),
('paid:edit', '编辑实付', '实付管理'),
('overview:view', '查看项目概览', '项目概览'),
('balance:view', '查看平衡表', '平衡表')
ON CONFLICT (perm_code) DO NOTHING;

-- 默认管理员（密码: 123456 -> bcrypt暂用明文+盐生产环境再hash）
-- 演示用，正式环境请使用BCrypt
INSERT INTO sys_user (work_no, name, login_name, password, dept_id, company_id, is_admin, status)
SELECT 'admin', '系统管理员', 'admin', '123456', d.id, c.id, 1, 1
FROM sys_department d, sys_company c
WHERE d.dept_code='OPS' AND c.company_code='HQ'
ON CONFLICT (login_name) DO NOTHING;

-- 角色-权限关联
-- ADMIN角色：拥有所有权限（通过is_admin字段控制）

-- MKT_USER (营销用户)：项目/协议/流向/应收应付
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='MKT_USER' AND p.perm_code IN (
    'project:view', 'project:edit', 'project:import',
    'agreement:view', 'agreement:edit', 'agreement:import',
    'flow:view', 'flow:import', 'flow:split', 'flow:final',
    'receivable:view', 'receivable:edit', 'receivable:audit',
    'payable:view', 'payable:edit', 'payable:audit',
    'overview:view', 'balance:view'
)
ON CONFLICT DO NOTHING;

-- OPS_USER (运营用户)：审核/确认
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='OPS_USER' AND p.perm_code IN (
    'project:view',
    'agreement:view',
    'flow:view', 'flow:final',
    'receivable:view', 'receivable:audit',
    'payable:view', 'payable:audit',
    'overview:view', 'balance:view'
)
ON CONFLICT DO NOTHING;

-- FIN_USER (财务用户)：费用/人工/实收实付
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='FIN_USER' AND p.perm_code IN (
    'project:view',
    'expense:view', 'expense:edit', 'expense:import',
    'labor:view', 'labor:edit', 'labor:import',
    'receivable:view',
    'payable:view', 'payable:audit',
    'received:view', 'received:edit',
    'paid:view', 'paid:edit',
    'balance:view'
)
ON CONFLICT DO NOTHING;

-- HR_USER (人力用户)：人工成本
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='HR_USER' AND p.perm_code IN (
    'project:view',
    'labor:view', 'labor:edit', 'labor:import'
)
ON CONFLICT DO NOTHING;

-- PUR_USER (采购用户)：实收确认
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p 
WHERE r.role_code='PUR_USER' AND p.perm_code IN (
    'project:view',
    'received:view', 'received:edit'
)
ON CONFLICT DO NOTHING;

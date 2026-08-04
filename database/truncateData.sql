-- ============================================================
-- 清除除角色、用户之外的所有业务数据
-- 数据库: PostgreSQL
-- 说明: 按外键依赖从子表到父表的顺序删除
-- 保留的表: sys_role, sys_permission, sys_role_permission, sys_user, sys_user_role, sys_department, sys_company
-- ============================================================

-- 17. 操作日志
TRUNCATE TABLE sys_op_log RESTART IDENTITY CASCADE;

-- 16. 实付
TRUNCATE TABLE prj_paid RESTART IDENTITY CASCADE;

-- 15. 实收
TRUNCATE TABLE prj_received RESTART IDENTITY CASCADE;

-- 14. 应付
TRUNCATE TABLE prj_payable RESTART IDENTITY CASCADE;

-- 13. 应收
TRUNCATE TABLE prj_receivable RESTART IDENTITY CASCADE;

-- 12. 人工投入
TRUNCATE TABLE fin_project_labor RESTART IDENTITY CASCADE;

-- 11. 费用投入
TRUNCATE TABLE fin_project_expense RESTART IDENTITY CASCADE;

-- 10. 分解记录
TRUNCATE TABLE flow_split_record RESTART IDENTITY CASCADE;

-- 9. 下游流向
TRUNCATE TABLE flow_downstream_final RESTART IDENTITY CASCADE;
TRUNCATE TABLE flow_downstream_record RESTART IDENTITY CASCADE;
TRUNCATE TABLE flow_downstream_batch RESTART IDENTITY CASCADE;

-- 8. 上游流向
TRUNCATE TABLE flow_upstream_final RESTART IDENTITY CASCADE;
TRUNCATE TABLE flow_upstream_record RESTART IDENTITY CASCADE;
TRUNCATE TABLE flow_upstream_batch RESTART IDENTITY CASCADE;

-- 7. 下游协议相关
TRUNCATE TABLE prj_downstream_rebate_rule RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_downstream_attach RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_downstream_remark_file RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_downstream_team_target RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_downstream_agreement RESTART IDENTITY CASCADE;

-- 6. 考核组
TRUNCATE TABLE prj_assess_item RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_assess_group RESTART IDENTITY CASCADE;

-- 5. 上游协议相关
TRUNCATE TABLE prj_upstream_rebate_rule RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_upstream_attach RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_upstream_remark_file RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_upstream_team_target RESTART IDENTITY CASCADE;
TRUNCATE TABLE prj_upstream_agreement RESTART IDENTITY CASCADE;

-- 4. 项目作业人员
TRUNCATE TABLE prj_project_staff RESTART IDENTITY CASCADE;

-- 3. 项目主表
TRUNCATE TABLE prj_project RESTART IDENTITY CASCADE;

-- ============================================================
-- 保留以下表的数据（用户/角色/权限/部门/公司）:
--   sys_department, sys_company, sys_user, sys_role,
--   sys_permission, sys_role_permission, sys_user_role
-- ============================================================
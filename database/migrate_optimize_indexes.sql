-- ============================================================
-- 营销返利项目管理系统 - 数据库优化脚本
-- 包含：索引优化、表注释完善、列注释完善
-- 执行前请备份数据库
-- ============================================================

-- 1. 添加缺失的索引 =========================================

-- prj_project 表：按年度查询
CREATE INDEX IF NOT EXISTS idx_project_co_year ON prj_project(co_year);
-- prj_project 表：按状态查询
CREATE INDEX IF NOT EXISTS idx_project_status ON prj_project(status);
-- prj_project 表：按项目负责人查询
CREATE INDEX IF NOT EXISTS idx_project_owner ON prj_project(owner_user_id);

-- prj_upstream_agreement 表：按项目和是否当前版本查询
CREATE INDEX IF NOT EXISTS idx_upstream_current ON prj_upstream_agreement(project_id, is_current);
-- prj_upstream_agreement 表：按负责人查询
CREATE INDEX IF NOT EXISTS idx_upstream_owner ON prj_upstream_agreement(owner_user_id);

-- prj_downstream_agreement 表：按项目和是否当前版本查询
CREATE INDEX IF NOT EXISTS idx_downstream_current ON prj_downstream_agreement(project_id, is_current);
-- prj_downstream_agreement 表：按负责人查询
CREATE INDEX IF NOT EXISTS idx_downstream_owner ON prj_downstream_agreement(owner_user_id);

-- flow_upstream_record 表：按项目和是否有效查询（明细数据查询）
CREATE INDEX IF NOT EXISTS idx_upstream_record_valid ON flow_upstream_record(project_id, is_valid);
-- flow_upstream_record 表：按考核组查询
CREATE INDEX IF NOT EXISTS idx_upstream_record_assess ON flow_upstream_record(assess_group_id);
-- flow_upstream_record 表：按上游批次和业务日期范围查询
CREATE INDEX IF NOT EXISTS idx_upstream_record_batch_date ON flow_upstream_record(batch_id, business_date);

-- flow_downstream_record 表：按项目和是否有效查询
CREATE INDEX IF NOT EXISTS idx_downstream_record_valid ON flow_downstream_record(project_id, is_valid);
-- flow_downstream_record 表：按考核组查询
CREATE INDEX IF NOT EXISTS idx_downstream_record_assess ON flow_downstream_record(assess_group_id);
-- flow_downstream_record 表：按协议和业务日期范围查询
CREATE INDEX IF NOT EXISTS idx_downstream_record_agree_date ON flow_downstream_record(agreement_id, business_date);

-- fin_project_expense 表：按项目和时间范围查询
CREATE INDEX IF NOT EXISTS idx_expense_project_date ON fin_project_expense(project_id, reimburse_date);
-- fin_project_expense 表：按费用类型查询
CREATE INDEX IF NOT EXISTS idx_expense_type ON fin_project_expense(expense_type);

-- fin_project_labor 表：按项目和月份查询
CREATE INDEX IF NOT EXISTS idx_labor_project_month ON fin_project_labor(project_id, month_yyyymm);

-- prj_receivable 表：按项目和阶段查询
CREATE INDEX IF NOT EXISTS idx_receivable_stage ON prj_receivable(project_id, stage);
-- prj_receivable 表：按状态查询
CREATE INDEX IF NOT EXISTS idx_receivable_status ON prj_receivable(status);

-- prj_payable 表：按项目和阶段查询
CREATE INDEX IF NOT EXISTS idx_payable_stage ON prj_payable(project_id, stage);
-- prj_payable 表：按状态查询
CREATE INDEX IF NOT EXISTS idx_payable_status ON prj_payable(status);

-- prj_received 表：按项目和阶段查询
CREATE INDEX IF NOT EXISTS idx_received_stage ON prj_received(project_id, stage);
-- prj_received 表：按状态查询
CREATE INDEX IF NOT EXISTS idx_received_status ON prj_received(status);

-- prj_paid 表：按项目和阶段查询
CREATE INDEX IF NOT EXISTS idx_paid_stage ON prj_paid(project_id, stage);

-- prj_project_person 表：按项目和工号查询
CREATE INDEX IF NOT EXISTS idx_person_project_workno ON prj_project_person(project_id, work_no);

-- 2. 添加表和列注释 =========================================

-- sys_department 表注释
COMMENT ON TABLE sys_department IS '部门表';
COMMENT ON COLUMN sys_department.id IS '部门ID';
COMMENT ON COLUMN sys_department.dept_code IS '部门编码';
COMMENT ON COLUMN sys_department.dept_name IS '部门名称';
COMMENT ON COLUMN sys_department.parent_id IS '父部门ID，0表示顶级';
COMMENT ON COLUMN sys_department.sort_no IS '排序号';
COMMENT ON COLUMN sys_department.status IS '状态：1启用 0停用';
COMMENT ON COLUMN sys_department.created_at IS '创建时间';

-- sys_company 表注释
COMMENT ON TABLE sys_company IS '所属公司表';
COMMENT ON COLUMN sys_company.id IS '公司ID';
COMMENT ON COLUMN sys_company.company_code IS '公司编码';
COMMENT ON COLUMN sys_company.company_name IS '公司名称';
COMMENT ON COLUMN sys_company.status IS '状态：1启用 0停用';
COMMENT ON COLUMN sys_company.created_at IS '创建时间';

-- sys_user 表注释
COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.work_no IS '工号';
COMMENT ON COLUMN sys_user.name IS '用户姓名';
COMMENT ON COLUMN sys_user.login_name IS '登录名';
COMMENT ON COLUMN sys_user.password IS '密码（加密存储）';
COMMENT ON COLUMN sys_user.dept_id IS '所属部门ID';
COMMENT ON COLUMN sys_user.company_id IS '所属公司ID';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.status IS '状态：1启用 0停用';
COMMENT ON COLUMN sys_user.is_admin IS '是否管理员：1是 0否';
COMMENT ON COLUMN sys_user.role_id IS '角色ID';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';

-- sys_role 表注释
COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.created_at IS '创建时间';

-- sys_permission 表注释
COMMENT ON TABLE sys_permission IS '权限/功能点表';
COMMENT ON COLUMN sys_permission.id IS '权限ID';
COMMENT ON COLUMN sys_permission.perm_code IS '权限编码，格式如 project:edit';
COMMENT ON COLUMN sys_permission.perm_name IS '权限名称';
COMMENT ON COLUMN sys_permission.module IS '所属模块';
COMMENT ON COLUMN sys_permission.description IS '权限描述';

-- sys_role_permission 表注释
COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_permission.perm_id IS '权限ID';

-- sys_user_role 表注释
COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

-- prj_project 表注释
COMMENT ON TABLE prj_project IS '营销项目主表';
COMMENT ON COLUMN prj_project.id IS '项目ID';
COMMENT ON COLUMN prj_project.project_code IS '内部项目编号';
COMMENT ON COLUMN prj_project.project_name IS '项目名称（与BPM同步）';
COMMENT ON COLUMN prj_project.brand IS '签约厂牌';
COMMENT ON COLUMN prj_project.co_product IS '合作品种';
COMMENT ON COLUMN prj_project.co_mode IS '合作模式';
COMMENT ON COLUMN prj_project.co_year IS '合作年度，4位数字如2024';
COMMENT ON COLUMN prj_project.period_start_date IS '合作周期开始日期';
COMMENT ON COLUMN prj_project.period_end_date IS '合作周期结束日期';
COMMENT ON COLUMN prj_project.region IS '覆盖地区';
COMMENT ON COLUMN prj_project.target_scale IS '项目目标规模';
COMMENT ON COLUMN prj_project.expected_rebate IS '预计收益返利金额';
COMMENT ON COLUMN prj_project.expected_cost IS '预计费用';
COMMENT ON COLUMN prj_project.description IS '项目描述';
COMMENT ON COLUMN prj_project.bpm_process_id IS 'BPM流程实例ID';
COMMENT ON COLUMN prj_project.bpm_project_id IS 'BPM立项ID';
COMMENT ON COLUMN prj_project.bpm_synced IS '是否已从BPM同步：0否 1是';
COMMENT ON COLUMN prj_project.status IS '项目状态：NEW新建/RUNNING进行中/CLOSED已关闭';
COMMENT ON COLUMN prj_project.owner_user_id IS '项目负责人用户ID';
COMMENT ON COLUMN prj_project.created_by IS '创建人用户ID';
COMMENT ON COLUMN prj_project.created_at IS '创建时间';
COMMENT ON COLUMN prj_project.updated_at IS '更新时间';

-- prj_project_person 表注释
COMMENT ON TABLE prj_project_person IS '项目作业人员表（人员可跨项目，按比例分摊费用）';
COMMENT ON COLUMN prj_project_person.id IS '记录ID';
COMMENT ON COLUMN prj_project_person.project_id IS '项目ID';
COMMENT ON COLUMN prj_project_person.work_no IS '工号';
COMMENT ON COLUMN prj_project_person.name IS '姓名';
COMMENT ON COLUMN prj_project_person.dept_name IS '部门名称';
COMMENT ON COLUMN prj_project_person.position IS '职位';
COMMENT ON COLUMN prj_project_person.work_type IS '用工类型：FULL全职/PART兼职/OUTSOURCE外包';
COMMENT ON COLUMN prj_project_person.labor_cost_ratio IS '人工费用比例(%)';
COMMENT ON COLUMN prj_project_person.expense_ratio IS '报销费用比例(%)';
COMMENT ON COLUMN prj_project_person.created_at IS '创建时间';

-- prj_upstream_agreement 表注释
COMMENT ON TABLE prj_upstream_agreement IS '上游协议表（含版本与历史）';
COMMENT ON COLUMN prj_upstream_agreement.id IS '协议ID';
COMMENT ON COLUMN prj_upstream_agreement.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_upstream_agreement.version IS '版本号';
COMMENT ON COLUMN prj_upstream_agreement.is_current IS '是否最新版本：1是 0否';
COMMENT ON COLUMN prj_upstream_agreement.bpm_agree_id IS 'BPM协议ID';
COMMENT ON COLUMN prj_upstream_agreement.agreement_name IS '协议名称';
COMMENT ON COLUMN prj_upstream_agreement.agreement_no IS '协议编号';
COMMENT ON COLUMN prj_upstream_agreement.period_start_date IS '协议周期开始日期';
COMMENT ON COLUMN prj_upstream_agreement.period_end_date IS '协议周期结束日期';
COMMENT ON COLUMN prj_upstream_agreement.region IS '覆盖地区';
COMMENT ON COLUMN prj_upstream_agreement.target_terminal IS '目标终端';
COMMENT ON COLUMN prj_upstream_agreement.calc_basis IS '核算基准：QTY数量/AMT金额';
COMMENT ON COLUMN prj_upstream_agreement.target_scale IS '目标规模';
COMMENT ON COLUMN prj_upstream_agreement.calc_method IS '计算方式说明';
COMMENT ON COLUMN prj_upstream_agreement.supplier IS '合作上游企业';
COMMENT ON COLUMN prj_upstream_agreement.target_dept IS '指标承接部门';
COMMENT ON COLUMN prj_upstream_agreement.flow_contact IS '流向联系人';
COMMENT ON COLUMN prj_upstream_agreement.flow_phone IS '流向联系电话';
COMMENT ON COLUMN prj_upstream_agreement.flow_channel IS '流向渠道';
COMMENT ON COLUMN prj_upstream_agreement.flow_provide_method IS '流向提供方式';
COMMENT ON COLUMN prj_upstream_agreement.stage1_target IS '阶段一目标规模';
COMMENT ON COLUMN prj_upstream_agreement.stage2_target IS '阶段二目标规模';
COMMENT ON COLUMN prj_upstream_agreement.stage3_target IS '阶段三目标规模';
COMMENT ON COLUMN prj_upstream_agreement.stage4_target IS '阶段四目标规模';
COMMENT ON COLUMN prj_upstream_agreement.owner_user_id IS '协议负责人用户ID';
COMMENT ON COLUMN prj_upstream_agreement.policy_detail IS '营销政策明细';
COMMENT ON COLUMN prj_upstream_agreement.rebate_calc_rule IS '返利计算方式（分阶段+比例+达成规模）';
COMMENT ON COLUMN prj_upstream_agreement.settle_basis IS '结算基准：预付/按阶段/全年';
COMMENT ON COLUMN prj_upstream_agreement.settle_ratio IS '各支付时点比例，JSON格式';
COMMENT ON COLUMN prj_upstream_agreement.rebate_pay_type IS '返利支付类型：票折/服务费';
COMMENT ON COLUMN prj_upstream_agreement.rebate_pay_time IS '返利支付时间';
COMMENT ON COLUMN prj_upstream_agreement.team_assess_settle IS '专职团队考核结算方式';
COMMENT ON COLUMN prj_upstream_agreement.required_staff_num IS '要求人员数量';
COMMENT ON COLUMN prj_upstream_agreement.formal_count IS '正式人员数量';
COMMENT ON COLUMN prj_upstream_agreement.formal_names IS '正式人员名单';
COMMENT ON COLUMN prj_upstream_agreement.informal_count IS '非正式人员数量';
COMMENT ON COLUMN prj_upstream_agreement.informal_names IS '非正式人员名单';
COMMENT ON COLUMN prj_upstream_agreement.created_by IS '创建人用户ID';
COMMENT ON COLUMN prj_upstream_agreement.created_at IS '创建时间';
COMMENT ON COLUMN prj_upstream_agreement.updated_at IS '更新时间';

-- prj_upstream_team_target 表注释
COMMENT ON TABLE prj_upstream_team_target IS '上游协议-专职团队考核目标子表';
COMMENT ON COLUMN prj_upstream_team_target.id IS '目标ID';
COMMENT ON COLUMN prj_upstream_team_target.agreement_id IS '关联上游协议ID';
COMMENT ON COLUMN prj_upstream_team_target.target_name IS '目标名称';
COMMENT ON COLUMN prj_upstream_team_target.owner IS '负责人';
COMMENT ON COLUMN prj_upstream_team_target.requirement IS '要求（1000字以内）';
COMMENT ON COLUMN prj_upstream_team_target.calc_standard IS '计算标准';
COMMENT ON COLUMN prj_upstream_team_target.reward_standard IS '奖励标准';
COMMENT ON COLUMN prj_upstream_team_target.sort_no IS '排序号';

-- prj_upstream_remark_file 表注释
COMMENT ON TABLE prj_upstream_remark_file IS '上游协议-备注附件表';
COMMENT ON COLUMN prj_upstream_remark_file.id IS '附件ID';
COMMENT ON COLUMN prj_upstream_remark_file.agreement_id IS '关联上游协议ID';
COMMENT ON COLUMN prj_upstream_remark_file.file_type IS '文件类型：PRODUCT产品/HOSPITAL医院/BLACKLIST黑名单/OTHER其他';
COMMENT ON COLUMN prj_upstream_remark_file.file_name IS '文件名';
COMMENT ON COLUMN prj_upstream_remark_file.file_path IS '文件路径';
COMMENT ON COLUMN prj_upstream_remark_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN prj_upstream_remark_file.uploaded_by IS '上传人用户ID';
COMMENT ON COLUMN prj_upstream_remark_file.uploaded_at IS '上传时间';

-- prj_upstream_attach 表注释
COMMENT ON TABLE prj_upstream_attach IS '上游协议-协议附件表';
COMMENT ON COLUMN prj_upstream_attach.id IS '附件ID';
COMMENT ON COLUMN prj_upstream_attach.agreement_id IS '关联上游协议ID';
COMMENT ON COLUMN prj_upstream_attach.attach_type IS '附件类型：MAIN合作协议/SUPP补充协议';
COMMENT ON COLUMN prj_upstream_attach.file_name IS '文件名';
COMMENT ON COLUMN prj_upstream_attach.file_path IS '文件路径';
COMMENT ON COLUMN prj_upstream_attach.file_size IS '文件大小（字节）';
COMMENT ON COLUMN prj_upstream_attach.uploaded_by IS '上传人用户ID';
COMMENT ON COLUMN prj_upstream_attach.uploaded_at IS '上传时间';

-- prj_upstream_rebate_rule 表注释
COMMENT ON COLUMN prj_upstream_rebate_rule.id IS '规则ID';
COMMENT ON COLUMN prj_upstream_rebate_rule.agreement_id IS '关联上游协议ID';
COMMENT ON COLUMN prj_upstream_rebate_rule.stage_code IS '阶段代码：S1/S2/S3/S4';
COMMENT ON COLUMN prj_upstream_rebate_rule.threshold_low IS '达成规模下限（含）';
COMMENT ON COLUMN prj_upstream_rebate_rule.threshold_high IS '达成规模上限（不含），0表示无穷大';
COMMENT ON COLUMN prj_upstream_rebate_rule.rebate_ratio IS '返利比例（如0.05表示5%）';
COMMENT ON COLUMN prj_upstream_rebate_rule.reward_type IS '奖励类型：SCALE按规模/ASSESS按考核';
COMMENT ON COLUMN prj_upstream_rebate_rule.assess_group_id IS '关联考核组ID（可为空）';
COMMENT ON COLUMN prj_upstream_rebate_rule.sort_no IS '排序号';
COMMENT ON COLUMN prj_upstream_rebate_rule.created_at IS '创建时间';

-- prj_assess_group 表注释
COMMENT ON COLUMN prj_assess_group.id IS '考核组ID';
COMMENT ON COLUMN prj_assess_group.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_assess_group.group_code IS '考核组编码';
COMMENT ON COLUMN prj_assess_group.group_name IS '考核组名称';
COMMENT ON COLUMN prj_assess_group.description IS '考核组描述';
COMMENT ON COLUMN prj_assess_group.created_by IS '创建人用户ID';
COMMENT ON COLUMN prj_assess_group.created_at IS '创建时间';

-- prj_assess_item 表注释
COMMENT ON COLUMN prj_assess_item.id IS '指标项ID';
COMMENT ON COLUMN prj_assess_item.group_id IS '关联考核组ID';
COMMENT ON COLUMN prj_assess_item.item_code IS '指标编码';
COMMENT ON COLUMN prj_assess_item.item_name IS '指标名称';
COMMENT ON COLUMN prj_assess_item.calc_basis IS '核算基准：AMOUNT金额/QTY数量/RATE比率';
COMMENT ON COLUMN prj_assess_item.target_value IS '目标值';
COMMENT ON COLUMN prj_assess_item.weight IS '权重';
COMMENT ON COLUMN prj_assess_item.sort_no IS '排序号';
COMMENT ON COLUMN prj_assess_item.created_at IS '创建时间';

-- prj_downstream_agreement 表注释
COMMENT ON TABLE prj_downstream_agreement IS '下游协议表（含版本与历史）';
COMMENT ON COLUMN prj_downstream_agreement.id IS '协议ID';
COMMENT ON COLUMN prj_downstream_agreement.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_downstream_agreement.upstream_id IS '关联上游协议ID';
COMMENT ON COLUMN prj_downstream_agreement.version IS '版本号';
COMMENT ON COLUMN prj_downstream_agreement.is_current IS '是否最新版本：1是 0否';
COMMENT ON COLUMN prj_downstream_agreement.bpm_agree_id IS 'BPM协议ID';
COMMENT ON COLUMN prj_downstream_agreement.upstream_name IS '上游协议名称';
COMMENT ON COLUMN prj_downstream_agreement.upstream_no IS '上游协议编号';
COMMENT ON COLUMN prj_downstream_agreement.agreement_name IS '协议名称';
COMMENT ON COLUMN prj_downstream_agreement.agreement_no IS '协议编号';
COMMENT ON COLUMN prj_downstream_agreement.period_start_date IS '协议周期开始日期';
COMMENT ON COLUMN prj_downstream_agreement.period_end_date IS '协议周期结束日期';
COMMENT ON COLUMN prj_downstream_agreement.region IS '覆盖地区';
COMMENT ON COLUMN prj_downstream_agreement.target_terminal IS '目标终端';
COMMENT ON COLUMN prj_downstream_agreement.calc_basis IS '核算基准：QTY数量/AMT金额（从上游带过来）';
COMMENT ON COLUMN prj_downstream_agreement.target_scale IS '目标规模';
COMMENT ON COLUMN prj_downstream_agreement.calc_method IS '计算方式说明';
COMMENT ON COLUMN prj_downstream_agreement.distributor IS '承接分销企业';
COMMENT ON COLUMN prj_downstream_agreement.distributor_type IS '分销企业类型：内部公司/外部公司';
COMMENT ON COLUMN prj_downstream_agreement.target_dept IS '指标承接部门';
COMMENT ON COLUMN prj_downstream_agreement.flow_contact IS '流向联系人';
COMMENT ON COLUMN prj_downstream_agreement.flow_phone IS '流向联系电话';
COMMENT ON COLUMN prj_downstream_agreement.flow_channel IS '流向渠道';
COMMENT ON COLUMN prj_downstream_agreement.flow_provide_method IS '流向提供方式';
COMMENT ON COLUMN prj_downstream_agreement.stage1_target IS '阶段一目标规模';
COMMENT ON COLUMN prj_downstream_agreement.stage2_target IS '阶段二目标规模';
COMMENT ON COLUMN prj_downstream_agreement.stage3_target IS '阶段三目标规模';
COMMENT ON COLUMN prj_downstream_agreement.stage4_target IS '阶段四目标规模';
COMMENT ON COLUMN prj_downstream_agreement.owner_user_id IS '协议负责人用户ID';
COMMENT ON COLUMN prj_downstream_agreement.policy_detail IS '营销政策明细';
COMMENT ON COLUMN prj_downstream_agreement.rebate_calc_rule IS '返利计算方式';
COMMENT ON COLUMN prj_downstream_agreement.settle_basis IS '结算基准';
COMMENT ON COLUMN prj_downstream_agreement.settle_ratio IS '各支付时点比例';
COMMENT ON COLUMN prj_downstream_agreement.rebate_pay_type IS '返利支付类型';
COMMENT ON COLUMN prj_downstream_agreement.rebate_pay_time IS '返利支付时间';
COMMENT ON COLUMN prj_downstream_agreement.team_assess_settle IS '专职团队考核结算方式';
COMMENT ON COLUMN prj_downstream_agreement.required_staff_num IS '要求人员数量';
COMMENT ON COLUMN prj_downstream_agreement.formal_count IS '正式人员数量';
COMMENT ON COLUMN prj_downstream_agreement.formal_names IS '正式人员名单';
COMMENT ON COLUMN prj_downstream_agreement.informal_count IS '非正式人员数量';
COMMENT ON COLUMN prj_downstream_agreement.informal_names IS '非正式人员名单';
COMMENT ON COLUMN prj_downstream_agreement.created_by IS '创建人用户ID';
COMMENT ON COLUMN prj_downstream_agreement.created_at IS '创建时间';
COMMENT ON COLUMN prj_downstream_agreement.updated_at IS '更新时间';

-- prj_downstream_team_target 表注释
COMMENT ON TABLE prj_downstream_team_target IS '下游协议-专职团队考核目标子表';
COMMENT ON COLUMN prj_downstream_team_target.id IS '目标ID';
COMMENT ON COLUMN prj_downstream_team_target.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_downstream_team_target.target_name IS '目标名称';
COMMENT ON COLUMN prj_downstream_team_target.owner IS '负责人';
COMMENT ON COLUMN prj_downstream_team_target.requirement IS '要求';
COMMENT ON COLUMN prj_downstream_team_target.calc_standard IS '计算标准';
COMMENT ON COLUMN prj_downstream_team_target.reward_standard IS '奖励标准';
COMMENT ON COLUMN prj_downstream_team_target.sort_no IS '排序号';

-- prj_downstream_remark_file 表注释
COMMENT ON TABLE prj_downstream_remark_file IS '下游协议-备注附件表';
COMMENT ON COLUMN prj_downstream_remark_file.id IS '附件ID';
COMMENT ON COLUMN prj_downstream_remark_file.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_downstream_remark_file.file_type IS '文件类型';
COMMENT ON COLUMN prj_downstream_remark_file.file_name IS '文件名';
COMMENT ON COLUMN prj_downstream_remark_file.file_path IS '文件路径';
COMMENT ON COLUMN prj_downstream_remark_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN prj_downstream_remark_file.uploaded_by IS '上传人用户ID';
COMMENT ON COLUMN prj_downstream_remark_file.uploaded_at IS '上传时间';

-- prj_downstream_attach 表注释
COMMENT ON TABLE prj_downstream_attach IS '下游协议-协议附件表';
COMMENT ON COLUMN prj_downstream_attach.id IS '附件ID';
COMMENT ON COLUMN prj_downstream_attach.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_downstream_attach.attach_type IS '附件类型';
COMMENT ON COLUMN prj_downstream_attach.file_name IS '文件名';
COMMENT ON COLUMN prj_downstream_attach.file_path IS '文件路径';
COMMENT ON COLUMN prj_downstream_attach.file_size IS '文件大小（字节）';
COMMENT ON COLUMN prj_downstream_attach.uploaded_by IS '上传人用户ID';
COMMENT ON COLUMN prj_downstream_attach.uploaded_at IS '上传时间';

-- prj_downstream_rebate_rule 表注释
COMMENT ON TABLE prj_downstream_rebate_rule IS '下游协议-返利计算规则子表';
COMMENT ON COLUMN prj_downstream_rebate_rule.id IS '规则ID';
COMMENT ON COLUMN prj_downstream_rebate_rule.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_downstream_rebate_rule.stage_code IS '阶段代码：S1/S2/S3/S4';
COMMENT ON COLUMN prj_downstream_rebate_rule.threshold_low IS '达成规模下限（含）';
COMMENT ON COLUMN prj_downstream_rebate_rule.threshold_high IS '达成规模上限（不含），0表示无穷大';
COMMENT ON COLUMN prj_downstream_rebate_rule.rebate_ratio IS '返利比例';
COMMENT ON COLUMN prj_downstream_rebate_rule.reward_type IS '奖励类型：SCALE按规模/ASSESS按考核';
COMMENT ON COLUMN prj_downstream_rebate_rule.assess_group_id IS '关联考核组ID';
COMMENT ON COLUMN prj_downstream_rebate_rule.sort_no IS '排序号';
COMMENT ON COLUMN prj_downstream_rebate_rule.created_at IS '创建时间';

-- flow_upstream_batch 表注释
COMMENT ON TABLE flow_upstream_batch IS '上游流向批次表（每次导入一个批次）';
COMMENT ON COLUMN flow_upstream_batch.id IS '批次ID';
COMMENT ON COLUMN flow_upstream_batch.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_upstream_batch.batch_code IS '系统生成的批次号';
COMMENT ON COLUMN flow_upstream_batch.file_name IS '导入文件名';
COMMENT ON COLUMN flow_upstream_batch.file_path IS '文件存储路径';
COMMENT ON COLUMN flow_upstream_batch.import_user IS '导入人用户ID';
COMMENT ON COLUMN flow_upstream_batch.import_time IS '导入时间';
COMMENT ON COLUMN flow_upstream_batch.month_summary IS '该批次覆盖的月份描述';
COMMENT ON COLUMN flow_upstream_batch.remark IS '备注';

-- flow_upstream_record 表注释
COMMENT ON COLUMN flow_upstream_record.id IS '记录ID';
COMMENT ON COLUMN flow_upstream_record.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_upstream_record.batch_id IS '关联批次ID';
COMMENT ON COLUMN flow_upstream_record.month_yyyymm IS '数据所属月份，格式yyyyMM';
COMMENT ON COLUMN flow_upstream_record.business_date IS '业务日期';
COMMENT ON COLUMN flow_upstream_record.product_name IS '产品名称';
COMMENT ON COLUMN flow_upstream_record.spec IS '规格';
COMMENT ON COLUMN flow_upstream_record.seller_name IS '销售方名称';
COMMENT ON COLUMN flow_upstream_record.seller_city IS '销售方城市';
COMMENT ON COLUMN flow_upstream_record.calc_price IS '核算价格';
COMMENT ON COLUMN flow_upstream_record.quantity IS '数量';
COMMENT ON COLUMN flow_upstream_record.calc_amount IS '核算金额';
COMMENT ON COLUMN flow_upstream_record.buyer_name IS '采购方名称';
COMMENT ON COLUMN flow_upstream_record.assess_group_id IS '关联考核组ID';
COMMENT ON COLUMN flow_upstream_record.is_valid IS '是否有效：1有效 0失效（同月后续导入置为失效）';
COMMENT ON COLUMN flow_upstream_record.is_final IS '是否终版：1是 0否（营销手动设定）';
COMMENT ON COLUMN flow_upstream_record.raw_row IS '原始行JSON数据（<150列）';
COMMENT ON COLUMN flow_upstream_record.created_at IS '创建时间';

-- flow_upstream_final 表注释
COMMENT ON COLUMN flow_upstream_final.id IS '记录ID';
COMMENT ON COLUMN flow_upstream_final.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_upstream_final.month_yyyymm IS '月份，格式yyyyMM';
COMMENT ON COLUMN flow_upstream_final.set_user IS '设置人用户ID';
COMMENT ON COLUMN flow_upstream_final.set_time IS '设置时间';

-- flow_downstream_batch 表注释
COMMENT ON TABLE flow_downstream_batch IS '下游流向批次表';
COMMENT ON COLUMN flow_downstream_batch.id IS '批次ID';
COMMENT ON COLUMN flow_downstream_batch.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_downstream_batch.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN flow_downstream_batch.batch_code IS '批次号';
COMMENT ON COLUMN flow_downstream_batch.file_name IS '导入文件名';
COMMENT ON COLUMN flow_downstream_batch.file_path IS '文件存储路径';
COMMENT ON COLUMN flow_downstream_batch.import_user IS '导入人用户ID';
COMMENT ON COLUMN flow_downstream_batch.import_time IS '导入时间';
COMMENT ON COLUMN flow_downstream_batch.month_summary IS '月份描述';
COMMENT ON COLUMN flow_downstream_batch.remark IS '备注';

-- flow_downstream_record 表注释
COMMENT ON TABLE flow_downstream_record IS '下游流向明细表';
COMMENT ON COLUMN flow_downstream_record.id IS '记录ID';
COMMENT ON COLUMN flow_downstream_record.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_downstream_record.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN flow_downstream_record.batch_id IS '关联批次ID';
COMMENT ON COLUMN flow_downstream_record.month_yyyymm IS '数据所属月份';
COMMENT ON COLUMN flow_downstream_record.business_date IS '业务日期';
COMMENT ON COLUMN flow_downstream_record.product_name IS '产品名称';
COMMENT ON COLUMN flow_downstream_record.spec IS '规格';
COMMENT ON COLUMN flow_downstream_record.seller_name IS '销售方名称';
COMMENT ON COLUMN flow_downstream_record.seller_city IS '销售方城市';
COMMENT ON COLUMN flow_downstream_record.calc_price IS '核算价格';
COMMENT ON COLUMN flow_downstream_record.quantity IS '数量';
COMMENT ON COLUMN flow_downstream_record.calc_amount IS '核算金额';
COMMENT ON COLUMN flow_downstream_record.buyer_name IS '采购方名称';
COMMENT ON COLUMN flow_downstream_record.assess_group_id IS '关联考核组ID';
COMMENT ON COLUMN flow_downstream_record.is_valid IS '是否有效：1有效 0失效';
COMMENT ON COLUMN flow_downstream_record.is_final IS '是否终版：1是 0否';
COMMENT ON COLUMN flow_downstream_record.raw_row IS '原始行JSON数据';
COMMENT ON COLUMN flow_downstream_record.created_at IS '创建时间';

-- flow_downstream_final 表注释
COMMENT ON TABLE flow_downstream_final IS '下游流向月份终版设置表';
COMMENT ON COLUMN flow_downstream_final.id IS '记录ID';
COMMENT ON COLUMN flow_downstream_final.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN flow_downstream_final.month_yyyymm IS '月份';
COMMENT ON COLUMN flow_downstream_final.set_user IS '设置人用户ID';
COMMENT ON COLUMN flow_downstream_final.set_time IS '设置时间';

-- flow_split_record 表注释
COMMENT ON TABLE flow_split_record IS '流向分解记录表（营销手动分解上游流向到下游分销商）';
COMMENT ON COLUMN flow_split_record.id IS '记录ID';
COMMENT ON COLUMN flow_split_record.upstream_id IS '上游record.id';
COMMENT ON COLUMN flow_split_record.project_id IS '关联项目ID';
COMMENT ON COLUMN flow_split_record.agreement_id IS '下游协议id';
COMMENT ON COLUMN flow_split_record.split_qty IS '分解数量';
COMMENT ON COLUMN flow_split_record.split_amount IS '分解金额';
COMMENT ON COLUMN flow_split_record.split_user IS '分解人用户ID';
COMMENT ON COLUMN flow_split_record.split_time IS '分解时间';

-- fin_project_expense 表注释
COMMENT ON TABLE fin_project_expense IS '项目费用投入表（财务导入）';
COMMENT ON COLUMN fin_project_expense.id IS '记录ID';
COMMENT ON COLUMN fin_project_expense.project_id IS '关联项目ID（系统分摊后写入）';
COMMENT ON COLUMN fin_project_expense.reimburse_date IS '报销日期';
COMMENT ON COLUMN fin_project_expense.expense_type IS '费用类型';
COMMENT ON COLUMN fin_project_expense.work_no IS '报销人工号';
COMMENT ON COLUMN fin_project_expense.name IS '报销人姓名';
COMMENT ON COLUMN fin_project_expense.description IS '费用描述';
COMMENT ON COLUMN fin_project_expense.amount IS '发票金额';
COMMENT ON COLUMN fin_project_expense.allocated_amount IS '本项目分摊金额';
COMMENT ON COLUMN fin_project_expense.source IS '来源：IMPORT导入/INPUT手工录入';
COMMENT ON COLUMN fin_project_expense.raw_project_name IS '导入时填写的项目名称';
COMMENT ON COLUMN fin_project_expense.doc_no IS '发票号';
COMMENT ON COLUMN fin_project_expense.matched_type IS '匹配类型：PROJECT_NAME按项目名/PERSON_SPLIT按人员分摊/UNMATCHED未匹配';
COMMENT ON COLUMN fin_project_expense.import_user IS '导入人用户ID';
COMMENT ON COLUMN fin_project_expense.import_time IS '导入时间';
COMMENT ON COLUMN fin_project_expense.remark IS '备注';

-- fin_project_labor 表注释
COMMENT ON TABLE fin_project_labor IS '项目人工投入表（人力导入）';
COMMENT ON COLUMN fin_project_labor.id IS '记录ID';
COMMENT ON COLUMN fin_project_labor.project_id IS '关联项目ID';
COMMENT ON COLUMN fin_project_labor.month_yyyymm IS '数据月份';
COMMENT ON COLUMN fin_project_labor.work_no IS '工号';
COMMENT ON COLUMN fin_project_labor.name IS '姓名';
COMMENT ON COLUMN fin_project_labor.work_type IS '用工类型：FULL全职/PART兼职/OUTSOURCE外包';
COMMENT ON COLUMN fin_project_labor.salary IS '应发工资';
COMMENT ON COLUMN fin_project_labor.welfare IS '福利等其它人工费用';
COMMENT ON COLUMN fin_project_labor.other_cost IS '其他费用';
COMMENT ON COLUMN fin_project_labor.total_cost IS '费用合计';
COMMENT ON COLUMN fin_project_labor.allocated_amount IS '本项目分摊金额';
COMMENT ON COLUMN fin_project_labor.alloc_ratio IS '本项目分摊比例';
COMMENT ON COLUMN fin_project_labor.source IS '来源：IMPORT导入';
COMMENT ON COLUMN fin_project_labor.matched_type IS '匹配类型';
COMMENT ON COLUMN fin_project_labor.import_user IS '导入人用户ID';
COMMENT ON COLUMN fin_project_labor.import_time IS '导入时间';
COMMENT ON COLUMN fin_project_labor.remark IS '备注';

-- prj_receivable 表注释
COMMENT ON TABLE prj_receivable IS '项目应收表';
COMMENT ON COLUMN prj_receivable.id IS '记录ID';
COMMENT ON COLUMN prj_receivable.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_receivable.stage IS '阶段：STAGE1/2/3/4';
COMMENT ON COLUMN prj_receivable.scale_amount IS '依据规模计算的应收金额';
COMMENT ON COLUMN prj_receivable.assess_amount IS '依据考核计算的应收金额';
COMMENT ON COLUMN prj_receivable.total_amount IS '合计应收金额';
COMMENT ON COLUMN prj_receivable.estimate_amount IS '系统估算金额';
COMMENT ON COLUMN prj_receivable.status IS '状态：DRAFT草稿/AUDIT审核中/FINAL已完成';
COMMENT ON COLUMN prj_receivable.fill_user IS '填报人用户ID';
COMMENT ON COLUMN prj_receivable.fill_time IS '填报时间';
COMMENT ON COLUMN prj_receivable.audit_user IS '审核人用户ID';
COMMENT ON COLUMN prj_receivable.audit_time IS '审核时间';
COMMENT ON COLUMN prj_receivable.remark IS '备注';

-- prj_payable 表注释
COMMENT ON TABLE prj_payable IS '项目应付表';
COMMENT ON COLUMN prj_payable.id IS '记录ID';
COMMENT ON COLUMN prj_payable.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_payable.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_payable.stage IS '阶段';
COMMENT ON COLUMN prj_payable.scale_amount IS '依据规模应付金额';
COMMENT ON COLUMN prj_payable.assess_amount IS '依据考核应付金额';
COMMENT ON COLUMN prj_payable.total_amount IS '合计应付金额';
COMMENT ON COLUMN prj_payable.estimate_amount IS '系统估算金额';
COMMENT ON COLUMN prj_payable.status IS '状态';
COMMENT ON COLUMN prj_payable.fill_user IS '填报人用户ID';
COMMENT ON COLUMN prj_payable.fill_time IS '填报时间';
COMMENT ON COLUMN prj_payable.audit_user IS '审核人用户ID';
COMMENT ON COLUMN prj_payable.audit_time IS '审核时间';
COMMENT ON COLUMN prj_payable.confirm_user IS '财务确认人用户ID';
COMMENT ON COLUMN prj_payable.confirm_time IS '财务确认时间';
COMMENT ON COLUMN prj_payable.remark IS '备注';

-- prj_received 表注释
COMMENT ON TABLE prj_received IS '项目实收表';
COMMENT ON COLUMN prj_received.id IS '记录ID';
COMMENT ON COLUMN prj_received.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_received.stage IS '阶段';
COMMENT ON COLUMN prj_received.rebate_type IS '返利类型：TICKET票折/SERVICE服务费';
COMMENT ON COLUMN prj_received.applicant IS '申请人';
COMMENT ON COLUMN prj_received.apply_dept IS '申请部门';
COMMENT ON COLUMN prj_received.apply_date IS '申请日期';
COMMENT ON COLUMN prj_received.finance_code IS '财务编码';
COMMENT ON COLUMN prj_received.rebate_amount IS '返利金额';
COMMENT ON COLUMN prj_received.tax_rate IS '税率';
COMMENT ON COLUMN prj_received.total_price_tax IS '价税合计';
COMMENT ON COLUMN prj_received.dept_share IS '本部门应得';
COMMENT ON COLUMN prj_received.invoice_no IS '发票号';
COMMENT ON COLUMN prj_received.receive_dept IS '收款部门';
COMMENT ON COLUMN prj_received.status IS '状态：DRAFT草稿/PURCHASE_OK采购认可/OP_OK运营确认/FIN_OK财务确认/FINAL已完成';
COMMENT ON COLUMN prj_received.bpm_process_id IS 'BPM流程实例ID';
COMMENT ON COLUMN prj_received.purchase_user IS '采购确认人用户ID';
COMMENT ON COLUMN prj_received.purchase_time IS '采购确认时间';
COMMENT ON COLUMN prj_received.op_user IS '运营确认人用户ID';
COMMENT ON COLUMN prj_received.op_time IS '运营确认时间';
COMMENT ON COLUMN prj_received.finance_user IS '财务确认人用户ID';
COMMENT ON COLUMN prj_received.finance_time IS '财务确认时间';
COMMENT ON COLUMN prj_received.final_time IS '最终完成时间';
COMMENT ON COLUMN prj_received.remark IS '备注';

-- prj_paid 表注释
COMMENT ON TABLE prj_paid IS '项目实付表';
COMMENT ON COLUMN prj_paid.id IS '记录ID';
COMMENT ON COLUMN prj_paid.project_id IS '关联项目ID';
COMMENT ON COLUMN prj_paid.agreement_id IS '关联下游协议ID';
COMMENT ON COLUMN prj_paid.stage IS '阶段';
COMMENT ON COLUMN prj_paid.rebate_type IS '返利类型';
COMMENT ON COLUMN prj_paid.applicant IS '申请人';
COMMENT ON COLUMN prj_paid.apply_dept IS '申请部门';
COMMENT ON COLUMN prj_paid.apply_date IS '申请日期';
COMMENT ON COLUMN prj_paid.receive_dept IS '收款部门';
COMMENT ON COLUMN prj_paid.customer_name IS '客户名称';
COMMENT ON COLUMN prj_paid.total_rebate IS '应付返利金额';
COMMENT ON COLUMN prj_paid.actual_rebate IS '实付返利金额';
COMMENT ON COLUMN prj_paid.diff_amount IS '差异金额';
COMMENT ON COLUMN prj_paid.execute_status IS '执行状态';
COMMENT ON COLUMN prj_paid.bpm_process_id IS 'BPM流程实例ID';
COMMENT ON COLUMN prj_paid.op_user IS '运营确认人用户ID';
COMMENT ON COLUMN prj_paid.op_time IS '运营确认时间';
COMMENT ON COLUMN prj_paid.finance_user IS '财务确认人用户ID';
COMMENT ON COLUMN prj_paid.finance_time IS '财务确认时间';
COMMENT ON COLUMN prj_paid.final_time IS '最终完成时间';
COMMENT ON COLUMN prj_paid.remark IS '备注';

-- sys_op_log 表注释
COMMENT ON TABLE sys_op_log IS '系统操作日志表';
COMMENT ON COLUMN sys_op_log.id IS '日志ID';
COMMENT ON COLUMN sys_op_log.user_id IS '操作用户ID';
COMMENT ON COLUMN sys_op_log.login_name IS '登录名';
COMMENT ON COLUMN sys_op_log.module IS '操作模块';
COMMENT ON COLUMN sys_op_log.action IS '操作动作';
COMMENT ON COLUMN sys_op_log.content IS '操作内容';
COMMENT ON COLUMN sys_op_log.ip IS 'IP地址';
COMMENT ON COLUMN sys_op_log.op_time IS '操作时间';

-- 3. 分析并输出索引优化建议 =================================
-- 以下是针对慢查询的额外建议索引（可选，根据实际运行情况添加）

-- 4. 清理重复索引（如果存在）
-- 以下索引如果已存在会被忽略，PostgreSQL的CREATE INDEX支持IF NOT EXISTS

-- 输出完成信息
SELECT '数据库优化脚本执行完成' AS status;

package com.rebate.service;

import com.rebate.config.AppConfig;
import com.rebate.dao.ProjectDao;
import com.rebate.dao.UpstreamAgreementDao;
import com.rebate.model.Project;
import com.rebate.model.UpstreamAgreement;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BPM 同步服务
 * <p>通过 db.properties 中的 bpm.jdbc.* 配置连接到 BPM 库（只读），按视图规范取数。</p>
 * <p>视图规范见 docs/BPM_VIEW_REQUIREMENTS.md。</p>
 */
public class BpmSyncService {

    private final ProjectDao projectDao = new ProjectDao();
    private final UpstreamAgreementDao upstreamDao = new UpstreamAgreementDao();

    public int syncProjects() {
        if (!AppConfig.getBoolean("bpm.sync.enabled", false)) return 0;
        String url = AppConfig.get("bpm.jdbc.url");
        if (url.isEmpty()) return 0;
        String sql = "SELECT project_bpm_id, process_instance_id, project_name, brand, co_product, co_mode, " +
                "co_period, period_start_date, period_end_date, region, target_scale, expected_rebate, " +
                "expected_cost, description, owner_user_no " +
                "FROM v_rebate_bpm_project WHERE approve_status='APPROVED' AND approve_time > ?";
        Timestamp since = getLastSync("project");
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, since);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                String bpmId = rs.getString("project_bpm_id");
                if (projectDao.findByBpmId(bpmId) != null) continue; // 跳过
                Project p = new Project();
                p.setBpmProjectId(bpmId);
                p.setBpmProcessId(rs.getString("process_instance_id"));
                p.setProjectName(rs.getString("project_name"));
                p.setBrand(rs.getString("brand"));
                p.setCoProduct(rs.getString("co_product"));
                p.setCoMode(rs.getString("co_mode"));
                p.setCoYear(rs.getString("co_period"));
                p.setPeriodStartDate(rs.getDate("period_start_date"));
                p.setPeriodEndDate(rs.getDate("period_end_date"));
                p.setRegion(rs.getString("region"));
                p.setTargetScale(rs.getBigDecimal("target_scale"));
                p.setExpectedRebate(rs.getBigDecimal("expected_rebate"));
                p.setExpectedCost(rs.getBigDecimal("expected_cost"));
                p.setDescription(rs.getString("description"));
                p.setBpmSynced(1);
                p.setStatus("NEW");
                projectDao.insert(p);
                count++;
            }
            markSyncTime("project");
            return count;
        } catch (SQLException e) {
            throw new RuntimeException("BPM 项目同步失败: " + e.getMessage(), e);
        }
    }

    public int syncUpstreamAgreements() {
        if (!AppConfig.getBoolean("bpm.sync.enabled", false)) return 0;
        String url = AppConfig.get("bpm.jdbc.url");
        if (url.isEmpty()) return 0;
        String sql = "SELECT agree_bpm_id, project_bpm_id, process_instance_id, agreement_name, agreement_no, " +
                "period_start_date, period_end_date, region, target_terminal, calc_basis, target_scale, calc_method, " +
                "supplier, target_dept, flow_contact, flow_phone, flow_channel, flow_provide_method, " +
                "stage1_target, stage2_target, stage3_target, stage4_target, owner_user_no, " +
                "policy_detail, rebate_calc_rule, settle_basis, settle_ratio, rebate_pay_type, rebate_pay_time, " +
                "team_assess_settle, required_staff_num, approve_status, approve_time " +
                "FROM v_rebate_bpm_upstream_agreement WHERE approve_status='APPROVED' AND approve_time > ? " +
                "ORDER BY agreement_no, approve_time";
        Timestamp since = getLastSync("agreement");
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, since);
            ResultSet rs = ps.executeQuery();
            // 按 project 分组
            int count = 0;
            while (rs.next()) {
                String projectBpmId = rs.getString("project_bpm_id");
                Project project = projectDao.findByBpmId(projectBpmId);
                if (project == null) continue;
                // 计算版本号
                String agreementNo = rs.getString("agreement_no");
                Integer version = nextVersion(project.getId(), agreementNo);
                UpstreamAgreement a = new UpstreamAgreement();
                a.setProjectId(project.getId());
                a.setVersion(version);
                a.setIsCurrent(1);
                a.setBpmAgreeId(rs.getString("agree_bpm_id"));
                a.setAgreementName(rs.getString("agreement_name"));
                a.setAgreementNo(agreementNo);
                a.setPeriodStartDate(rs.getDate("period_start_date"));
                a.setPeriodEndDate(rs.getDate("period_end_date"));
                a.setRegion(rs.getString("region"));
                a.setTargetTerminal(rs.getString("target_terminal"));
                a.setCalcBasis(rs.getString("calc_basis"));
                a.setTargetScale(rs.getBigDecimal("target_scale"));
                a.setCalcMethod(rs.getString("calc_method"));
                a.setSupplier(rs.getString("supplier"));
                a.setTargetDept(rs.getString("target_dept"));
                a.setFlowContact(rs.getString("flow_contact"));
                a.setFlowPhone(rs.getString("flow_phone"));
                a.setFlowChannel(rs.getString("flow_channel"));
                a.setFlowProvideMethod(rs.getString("flow_provide_method"));
                a.setStage1Target(rs.getBigDecimal("stage1_target"));
                a.setStage2Target(rs.getBigDecimal("stage2_target"));
                a.setStage3Target(rs.getBigDecimal("stage3_target"));
                a.setStage4Target(rs.getBigDecimal("stage4_target"));
                a.setPolicyDetail(rs.getString("policy_detail"));
                a.setRebateCalcRule(rs.getString("rebate_calc_rule"));
                a.setSettleBasis(rs.getString("settle_basis"));
                a.setSettleRatio(rs.getString("settle_ratio"));
                a.setRebatePayType(rs.getString("rebate_pay_type"));
                a.setRebatePayTime(rs.getString("rebate_pay_time"));
                a.setTeamAssessSettle(rs.getString("team_assess_settle"));
                a.setRequiredStaffNum(rs.getObject("required_staff_num") == null ? 0 : rs.getInt("required_staff_num"));
                Long id = upstreamDao.insert(a);
                // 旧版本置非当前
                if (id != null) upstreamDao.markNotCurrent(project.getId(), id);
                count++;
            }
            markSyncTime("agreement");
            return count;
        } catch (SQLException e) {
            throw new RuntimeException("BPM 上游协议同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算下一版本号
     */
    private Integer nextVersion(long projectId, String agreementNo) {
        String sql = "SELECT COALESCE(MAX(version), 0) + 1 FROM prj_upstream_agreement WHERE project_id=? AND agreement_no=?";
        return com.rebate.dao.BaseDao.queryOne(sql, rs -> rs.getInt(1), projectId, agreementNo);
    }

    private Timestamp getLastSync(String type) {
        // 简化处理：取 1970；实际可放 sys_config 表
        return Timestamp.valueOf("1970-01-01 00:00:00");
    }

    private void markSyncTime(String type) {
        // TODO: 写入 sys_config
    }
}

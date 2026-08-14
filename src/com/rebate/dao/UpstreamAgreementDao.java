package com.rebate.dao;

import com.rebate.config.AppConfig;
import com.rebate.model.UpstreamAgreement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 上游协议 DAO
 */
public class UpstreamAgreementDao {

    public Long insert(UpstreamAgreement a) {
        String sql = "INSERT INTO prj_upstream_agreement(project_id, version, is_current, bpm_agree_id, " +
                "agreement_name, agreement_no, period_start_date, period_end_date, region, target_terminal, " +
                "calc_basis, target_scale, calc_method, rebate_calc_basis, supplier, target_dept, flow_contact, flow_phone, " +
                "flow_channel, flow_provide_method, stage1_target, stage2_target, stage3_target, stage4_target, " +
                "owner_user_id, policy_detail, rebate_calc_rule, settle_basis, settle_ratio, rebate_pay_type, " +
                "rebate_pay_time, team_assess_settle, required_staff_num, formal_count, formal_names, " +
                "informal_count, informal_names, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql,
                a.getProjectId(), a.getVersion(), a.getIsCurrent(), a.getBpmAgreeId(),
                a.getAgreementName(), a.getAgreementNo(), a.getPeriodStartDate(), a.getPeriodEndDate(),
                a.getRegion(), a.getTargetTerminal(), a.getCalcBasis(), a.getTargetScale(), a.getCalcMethod(),
                a.getRebateCalcBasis(),
                a.getSupplier(), a.getTargetDept(), a.getFlowContact(), a.getFlowPhone(), a.getFlowChannel(),
                a.getFlowProvideMethod(), a.getStage1Target(), a.getStage2Target(), a.getStage3Target(), a.getStage4Target(),
                a.getOwnerUserId(), a.getPolicyDetail(), a.getRebateCalcRule(), a.getSettleBasis(), a.getSettleRatio(),
                a.getRebatePayType(), a.getRebatePayTime(), a.getTeamAssessSettle(), a.getRequiredStaffNum(),
                a.getFormalCount(), a.getFormalNames(), a.getInformalCount(), a.getInformalNames(), a.getCreatedBy());
    }

    public int update(UpstreamAgreement a) {
        String sql = "UPDATE prj_upstream_agreement SET agreement_name=?, agreement_no=?, period_start_date=?, " +
                "period_end_date=?, region=?, target_terminal=?, calc_basis=?, target_scale=?, calc_method=?, rebate_calc_basis=?, " +
                "supplier=?, target_dept=?, flow_contact=?, flow_phone=?, flow_channel=?, flow_provide_method=?, " +
                "stage1_target=?, stage2_target=?, stage3_target=?, stage4_target=?, owner_user_id=?, " +
                "policy_detail=?, rebate_calc_rule=?, settle_basis=?, settle_ratio=?, rebate_pay_type=?, " +
                "rebate_pay_time=?, team_assess_settle=?, required_staff_num=?, formal_count=?, formal_names=?, " +
                "informal_count=?, informal_names=? WHERE id=?";
        return BaseDao.update(sql,
                a.getAgreementName(), a.getAgreementNo(), a.getPeriodStartDate(), a.getPeriodEndDate(),
                a.getRegion(), a.getTargetTerminal(), a.getCalcBasis(), a.getTargetScale(), a.getCalcMethod(),
                a.getRebateCalcBasis(),
                a.getSupplier(), a.getTargetDept(), a.getFlowContact(), a.getFlowPhone(), a.getFlowChannel(),
                a.getFlowProvideMethod(), a.getStage1Target(), a.getStage2Target(), a.getStage3Target(), a.getStage4Target(),
                a.getOwnerUserId(), a.getPolicyDetail(), a.getRebateCalcRule(), a.getSettleBasis(), a.getSettleRatio(),
                a.getRebatePayType(), a.getRebatePayTime(), a.getTeamAssessSettle(), a.getRequiredStaffNum(),
                a.getFormalCount(), a.getFormalNames(), a.getInformalCount(), a.getInformalNames(), a.getId());
    }

    public int markNotCurrent(long projectId, long excludeId) {
        return BaseDao.update("UPDATE prj_upstream_agreement SET is_current=0 WHERE project_id=? AND id<>?",
                projectId, excludeId);
    }

    public int delete(long id) {
        return BaseDao.update("DELETE FROM prj_upstream_agreement WHERE id=?", id);
    }

    public UpstreamAgreement findById(long id) {
        return BaseDao.queryOne("SELECT a.*, p.project_name, u.name AS owner_name FROM prj_upstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id WHERE a.id=?", this::map, id);
    }

    public UpstreamAgreement findCurrentByProject(long projectId) {
        return BaseDao.queryOne("SELECT a.*, p.project_name, u.name AS owner_name FROM prj_upstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id WHERE a.project_id=? AND a.is_current=1", this::map, projectId);
    }

    public List<UpstreamAgreement> listByProject(long projectId, boolean currentOnly) {
        String sql = "SELECT a.*, p.project_name, u.name AS owner_name FROM prj_upstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id " +
                "WHERE a.project_id=?" + (currentOnly ? " AND a.is_current=1" : "") + " ORDER BY a.version DESC";
        return BaseDao.query(sql, this::map, projectId);
    }

    public List<UpstreamAgreement> listAllCurrent() {
        return BaseDao.query("SELECT a.*, p.project_name, u.name AS owner_name FROM prj_upstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id WHERE a.is_current=1 ORDER BY a.id DESC", this::map);
    }

    /**
     * 按项目+协议编号查询历史版本（按version降序）
     */
    public List<UpstreamAgreement> listByProjectAndAgreementNo(long projectId, String agreementNo) {
        if (agreementNo == null || agreementNo.isEmpty()) return new ArrayList<>();
        return BaseDao.query(
                "SELECT * FROM prj_upstream_agreement WHERE project_id=? AND agreement_no=? ORDER BY version DESC",
                this::map, projectId, agreementNo);
    }

    /**
     * 把指定id的协议标记为非当前版本
     */
    public int markNotCurrentById(long id) {
        return BaseDao.update(
                "UPDATE prj_upstream_agreement SET is_current=0 WHERE id=?", id);
    }

    /**
     * 从BPM查询当前项目的上游协议列表
     * @param projectCode 当前项目的项目编号
     */
    public List<Map<String, Object>> listBpmUpstreamAgreements(String projectCode) {
        String url = AppConfig.get("bpm.jdbc.url");
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("BPM数据库未配置(bpm.jdbc.url)");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT 协议名称, 协议编号, 协议起始时间, 协议终止时间, 协议指定区域, " +
                "协议目标终端, 合作上游企业 FROM bpm_rebate_agreement_v " +
                "WHERE 项目编号 = ? ORDER BY 协议起始时间 DESC";
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("agreementName", rs.getString("协议名称"));
                m.put("agreementNo", rs.getString("协议编号"));
                m.put("periodStartDate", rs.getDate("协议起始时间") == null ? null : rs.getDate("协议起始时间").toString());
                m.put("periodEndDate", rs.getDate("协议终止时间") == null ? null : rs.getDate("协议终止时间").toString());
                m.put("region", rs.getString("协议指定区域"));
                m.put("targetTerminal", rs.getString("协议目标终端"));
                m.put("supplier", rs.getString("合作上游企业"));
                list.add(m);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("从BPM获取上游协议数据失败: " + e.getMessage(), e);
        }
    }

    public int countByAgreementNo(String agreementNo, long projectId, Integer excludeVersion) {
        String sql = "SELECT COUNT(*) FROM prj_upstream_agreement WHERE agreement_no=? AND project_id=? AND version<>?";
        return (int) BaseDao.count(sql, agreementNo, projectId, excludeVersion == null ? -1 : excludeVersion);
    }

    private UpstreamAgreement map(ResultSet rs) throws SQLException {
        UpstreamAgreement a = new UpstreamAgreement();
        a.setId(rs.getLong("id"));
        a.setProjectId(rs.getLong("project_id"));
        a.setVersion(rs.getInt("version"));
        a.setIsCurrent(rs.getInt("is_current"));
        a.setBpmAgreeId(rs.getString("bpm_agree_id"));
        a.setAgreementName(rs.getString("agreement_name"));
        a.setAgreementNo(rs.getString("agreement_no"));
        a.setPeriodStartDate(rs.getDate("period_start_date"));
        a.setPeriodEndDate(rs.getDate("period_end_date"));
        a.setRegion(rs.getString("region"));
        a.setTargetTerminal(rs.getString("target_terminal"));
        a.setCalcBasis(rs.getString("calc_basis"));
        a.setTargetScale(BaseDao.toBigDecimal(rs.getObject("target_scale")));
        a.setCalcMethod(rs.getString("calc_method"));
        try { a.setRebateCalcBasis(rs.getString("rebate_calc_basis")); } catch (SQLException ignored) {}
        a.setSupplier(rs.getString("supplier"));
        a.setTargetDept(rs.getString("target_dept"));
        a.setFlowContact(rs.getString("flow_contact"));
        a.setFlowPhone(rs.getString("flow_phone"));
        a.setFlowChannel(rs.getString("flow_channel"));
        a.setFlowProvideMethod(rs.getString("flow_provide_method"));
        a.setStage1Target(BaseDao.toBigDecimal(rs.getObject("stage1_target")));
        a.setStage2Target(BaseDao.toBigDecimal(rs.getObject("stage2_target")));
        a.setStage3Target(BaseDao.toBigDecimal(rs.getObject("stage3_target")));
        a.setStage4Target(BaseDao.toBigDecimal(rs.getObject("stage4_target")));
        a.setOwnerUserId(rs.getObject("owner_user_id") == null ? null : rs.getLong("owner_user_id"));
        a.setPolicyDetail(rs.getString("policy_detail"));
        a.setRebateCalcRule(rs.getString("rebate_calc_rule"));
        a.setSettleBasis(rs.getString("settle_basis"));
        a.setSettleRatio(rs.getString("settle_ratio"));
        a.setRebatePayType(rs.getString("rebate_pay_type"));
        a.setRebatePayTime(rs.getString("rebate_pay_time"));
        a.setTeamAssessSettle(rs.getString("team_assess_settle"));
        a.setRequiredStaffNum(rs.getInt("required_staff_num"));
        a.setFormalCount(rs.getInt("formal_count"));
        a.setFormalNames(rs.getString("formal_names"));
        a.setInformalCount(rs.getInt("informal_count"));
        a.setInformalNames(rs.getString("informal_names"));
        a.setCreatedBy(rs.getObject("created_by") == null ? null : rs.getLong("created_by"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        a.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { a.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        try { a.setCoYear(rs.getString("co_year")); } catch (Exception ignore) {}
        try { a.setOwnerName(rs.getString("owner_name")); } catch (Exception ignore) {}
        return a;
    }
    
    /**
     * 批量查询多个项目的当前上游协议
     */
    public List<UpstreamAgreement> listByProjects(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyList();
        String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT * FROM prj_upstream_agreement WHERE is_current=1 AND project_id IN (" + placeholders + ")";
        List<Object> params = new ArrayList<>(projectIds);
        return BaseDao.query(sql, this::map, params.toArray());
    }
}

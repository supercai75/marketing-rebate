package com.rebate.dao;

import com.rebate.model.DownstreamAgreement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 下游协议 DAO
 */
public class DownstreamAgreementDao {

    public Long insert(DownstreamAgreement a) {
        String sql = "INSERT INTO prj_downstream_agreement(project_id, upstream_id, version, is_current, bpm_agree_id, " +
                "upstream_name, upstream_no, agreement_name, agreement_no, period_start_date, period_end_date, " +
                "region, target_terminal, calc_basis, target_scale, calc_method, distributor, distributor_type, " +
                "target_dept, flow_contact, flow_phone, flow_channel, flow_provide_method, " +
                "stage1_target, stage2_target, stage3_target, stage4_target, owner_user_id, " +
                "policy_detail, rebate_calc_rule, settle_basis, settle_ratio, rebate_pay_type, rebate_pay_time, " +
                "team_assess_settle, required_staff_num, formal_count, formal_names, informal_count, informal_names, " +
                "created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql,
                a.getProjectId(), a.getUpstreamId(), a.getVersion(), a.getIsCurrent(), a.getBpmAgreeId(),
                a.getUpstreamName(), a.getUpstreamNo(), a.getAgreementName(), a.getAgreementNo(),
                a.getPeriodStartDate(), a.getPeriodEndDate(), a.getRegion(), a.getTargetTerminal(),
                a.getCalcBasis(), a.getTargetScale(), a.getCalcMethod(), a.getDistributor(), a.getDistributorType(),
                a.getTargetDept(), a.getFlowContact(), a.getFlowPhone(), a.getFlowChannel(), a.getFlowProvideMethod(),
                a.getStage1Target(), a.getStage2Target(), a.getStage3Target(), a.getStage4Target(), a.getOwnerUserId(),
                a.getPolicyDetail(), a.getRebateCalcRule(), a.getSettleBasis(), a.getSettleRatio(),
                a.getRebatePayType(), a.getRebatePayTime(), a.getTeamAssessSettle(), a.getRequiredStaffNum(),
                a.getFormalCount(), a.getFormalNames(), a.getInformalCount(), a.getInformalNames(), a.getCreatedBy());
    }

    public int update(DownstreamAgreement a) {
        String sql = "UPDATE prj_downstream_agreement SET upstream_id=?, upstream_name=?, upstream_no=?, " +
                "agreement_name=?, agreement_no=?, period_start_date=?, period_end_date=?, region=?, target_terminal=?, " +
                "calc_basis=?, target_scale=?, calc_method=?, distributor=?, distributor_type=?, target_dept=?, " +
                "flow_contact=?, flow_phone=?, flow_channel=?, flow_provide_method=?, " +
                "stage1_target=?, stage2_target=?, stage3_target=?, stage4_target=?, owner_user_id=?, " +
                "policy_detail=?, rebate_calc_rule=?, settle_basis=?, settle_ratio=?, rebate_pay_type=?, " +
                "rebate_pay_time=?, team_assess_settle=?, required_staff_num=?, formal_count=?, formal_names=?, " +
                "informal_count=?, informal_names=? WHERE id=?";
        return BaseDao.update(sql,
                a.getUpstreamId(), a.getUpstreamName(), a.getUpstreamNo(),
                a.getAgreementName(), a.getAgreementNo(), a.getPeriodStartDate(), a.getPeriodEndDate(),
                a.getRegion(), a.getTargetTerminal(), a.getCalcBasis(), a.getTargetScale(), a.getCalcMethod(),
                a.getDistributor(), a.getDistributorType(), a.getTargetDept(), a.getFlowContact(), a.getFlowPhone(),
                a.getFlowChannel(), a.getFlowProvideMethod(),
                a.getStage1Target(), a.getStage2Target(), a.getStage3Target(), a.getStage4Target(), a.getOwnerUserId(),
                a.getPolicyDetail(), a.getRebateCalcRule(), a.getSettleBasis(), a.getSettleRatio(),
                a.getRebatePayType(), a.getRebatePayTime(), a.getTeamAssessSettle(), a.getRequiredStaffNum(),
                a.getFormalCount(), a.getFormalNames(), a.getInformalCount(), a.getInformalNames(), a.getId());
    }

    public int delete(long id) {
        return BaseDao.update("DELETE FROM prj_downstream_agreement WHERE id=?", id);
    }

    public int markNotCurrent(long projectId, String agreementNo, long excludeId) {
        return BaseDao.update("UPDATE prj_downstream_agreement SET is_current=0 WHERE project_id=? AND agreement_no=? AND id<>?",
                projectId, agreementNo, excludeId);
    }

    public int findMaxVersion(long projectId, String agreementNo) {
        String sql = "SELECT COALESCE(MAX(version), 0) FROM prj_downstream_agreement WHERE project_id=? AND agreement_no=?";
        return (int) BaseDao.count(sql, projectId, agreementNo);
    }

    public DownstreamAgreement findById(long id) {
        return BaseDao.queryOne("SELECT a.*, p.project_name, u.name AS owner_name FROM prj_downstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id WHERE a.id=?", this::map, id);
    }

    public List<DownstreamAgreement> listByProject(long projectId, boolean currentOnly) {
        String sql = "SELECT a.*, p.project_name, u.name AS owner_name FROM prj_downstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id " +
                "WHERE a.project_id=?" + (currentOnly ? " AND a.is_current=1" : "") + " ORDER BY a.version DESC";
        return BaseDao.query(sql, this::map, projectId);
    }

    public List<DownstreamAgreement> listByUpstream(long upstreamId) {
        return BaseDao.query("SELECT a.*, p.project_name, u.name AS owner_name FROM prj_downstream_agreement a " +
                "LEFT JOIN prj_project p ON a.project_id=p.id " +
                "LEFT JOIN sys_user u ON a.owner_user_id=u.id " +
                "WHERE a.upstream_id=? AND a.is_current=1 ORDER BY a.id", this::map, upstreamId);
    }

    private DownstreamAgreement map(ResultSet rs) throws SQLException {
        DownstreamAgreement a = new DownstreamAgreement();
        a.setId(rs.getLong("id"));
        a.setProjectId(rs.getLong("project_id"));
        a.setUpstreamId(rs.getLong("upstream_id"));
        a.setVersion(rs.getInt("version"));
        a.setIsCurrent(rs.getInt("is_current"));
        a.setBpmAgreeId(rs.getString("bpm_agree_id"));
        a.setUpstreamName(rs.getString("upstream_name"));
        a.setUpstreamNo(rs.getString("upstream_no"));
        a.setAgreementName(rs.getString("agreement_name"));
        a.setAgreementNo(rs.getString("agreement_no"));
        a.setPeriodStartDate(rs.getDate("period_start_date"));
        a.setPeriodEndDate(rs.getDate("period_end_date"));
        a.setRegion(rs.getString("region"));
        a.setTargetTerminal(rs.getString("target_terminal"));
        a.setCalcBasis(rs.getString("calc_basis"));
        a.setTargetScale(BaseDao.toBigDecimal(rs.getObject("target_scale")));
        a.setCalcMethod(rs.getString("calc_method"));
        a.setDistributor(rs.getString("distributor"));
        a.setDistributorType(rs.getString("distributor_type"));
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
}

package com.rebate.dao;

import com.rebate.model.Payable;
import com.rebate.model.Receivable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * 应收/应付 DAO
 */
public class ReceivablePayableDao {

    /* 应收 */
    public Long insertReceivable(Receivable r) {
        return BaseDao.insertReturnId("INSERT INTO prj_receivable(project_id, stage, scale_amount, assess_amount, " +
                "total_amount, estimate_amount, status, fill_user, fill_time, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.getProjectId(), r.getStage(), r.getScaleAmount(), r.getAssessAmount(),
                r.getTotalAmount(), r.getEstimateAmount(), r.getStatus(), r.getFillUser(),
                r.getFillTime(), r.getRemark());
    }

    public int updateReceivable(Receivable r) {
        return BaseDao.update("UPDATE prj_receivable SET stage=?, scale_amount=?, assess_amount=?, " +
                "total_amount=?, estimate_amount=?, status=?, fill_user=?, fill_time=?, remark=? WHERE id=?",
                r.getStage(), r.getScaleAmount(), r.getAssessAmount(), r.getTotalAmount(), r.getEstimateAmount(),
                r.getStatus(), r.getFillUser(), r.getFillTime(), r.getRemark(), r.getId());
    }

    public int auditReceivable(long id, long auditUser, String newStatus) {
        return BaseDao.update("UPDATE prj_receivable SET status=?, audit_user=?, audit_time=? WHERE id=?",
                newStatus, auditUser, new Timestamp(System.currentTimeMillis()), id);
    }

    public int deleteReceivable(long id) {
        return BaseDao.update("DELETE FROM prj_receivable WHERE id=?", id);
    }

    public Receivable findReceivable(long id) {
        return BaseDao.queryOne("SELECT r.*, p.project_name, fu.name AS fill_user_name, au.name AS audit_user_name " +
                "FROM prj_receivable r LEFT JOIN prj_project p ON r.project_id=p.id " +
                "LEFT JOIN sys_user fu ON r.fill_user=fu.id " +
                "LEFT JOIN sys_user au ON r.audit_user=au.id WHERE r.id=?", this::mapReceivable, id);
    }

    public List<Receivable> listReceivableByProject(long projectId) {
        return BaseDao.query("SELECT r.*, p.project_name, fu.name AS fill_user_name, au.name AS audit_user_name " +
                "FROM prj_receivable r LEFT JOIN prj_project p ON r.project_id=p.id " +
                "LEFT JOIN sys_user fu ON r.fill_user=fu.id " +
                "LEFT JOIN sys_user au ON r.audit_user=au.id WHERE r.project_id=? ORDER BY r.stage", this::mapReceivable, projectId);
    }

    /* 应付 */
    public Long insertPayable(Payable p) {
        return BaseDao.insertReturnId("INSERT INTO prj_payable(project_id, agreement_id, stage, scale_amount, assess_amount, " +
                "total_amount, estimate_amount, status, fill_user, fill_time, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getProjectId(), p.getAgreementId(), p.getStage(), p.getScaleAmount(), p.getAssessAmount(),
                p.getTotalAmount(), p.getEstimateAmount(), p.getStatus(), p.getFillUser(), p.getFillTime(), p.getRemark());
    }

    public int updatePayable(Payable p) {
        return BaseDao.update("UPDATE prj_payable SET stage=?, scale_amount=?, assess_amount=?, " +
                "total_amount=?, estimate_amount=?, status=?, fill_user=?, fill_time=?, remark=? WHERE id=?",
                p.getStage(), p.getScaleAmount(), p.getAssessAmount(), p.getTotalAmount(), p.getEstimateAmount(),
                p.getStatus(), p.getFillUser(), p.getFillTime(), p.getRemark(), p.getId());
    }

    public int auditPayable(long id, long auditUser, String newStatus) {
        return BaseDao.update("UPDATE prj_payable SET status=?, audit_user=?, audit_time=? WHERE id=?",
                newStatus, auditUser, new Timestamp(System.currentTimeMillis()), id);
    }

    public int confirmPayable(long id, long userId, String newStatus) {
        return BaseDao.update("UPDATE prj_payable SET status=?, confirm_user=?, confirm_time=? WHERE id=?",
                newStatus, userId, new Timestamp(System.currentTimeMillis()), id);
    }

    public Payable findPayable(long id) {
        return BaseDao.queryOne("SELECT p.*, prj.project_name, a.agreement_name, a.distributor, a.distributor_type, " +
                "fu.name AS fill_user_name, au.name AS audit_user_name, cu.name AS confirm_user_name " +
                "FROM prj_payable p " +
                "LEFT JOIN prj_project prj ON p.project_id=prj.id " +
                "LEFT JOIN prj_downstream_agreement a ON p.agreement_id=a.id " +
                "LEFT JOIN sys_user fu ON p.fill_user=fu.id " +
                "LEFT JOIN sys_user au ON p.audit_user=au.id " +
                "LEFT JOIN sys_user cu ON p.confirm_user=cu.id WHERE p.id=?", this::mapPayable, id);
    }

    public List<Payable> listPayableByProject(long projectId, Long agreementId, String stage, String status) {
        StringBuilder sql = new StringBuilder("SELECT p.*, prj.project_name, a.agreement_name, a.distributor, a.distributor_type, " +
                "fu.name AS fill_user_name, au.name AS audit_user_name, cu.name AS confirm_user_name " +
                "FROM prj_payable p " +
                "LEFT JOIN prj_project prj ON p.project_id=prj.id " +
                "LEFT JOIN prj_downstream_agreement a ON p.agreement_id=a.id " +
                "LEFT JOIN sys_user fu ON p.fill_user=fu.id " +
                "LEFT JOIN sys_user au ON p.audit_user=au.id " +
                "LEFT JOIN sys_user cu ON p.confirm_user=cu.id WHERE p.project_id=?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(projectId);
        if (agreementId != null && agreementId > 0) { sql.append(" AND p.agreement_id=?"); params.add(agreementId); }
        if (stage != null && !stage.isEmpty()) { sql.append(" AND p.stage=?"); params.add(stage); }
        if (status != null && !status.isEmpty()) { sql.append(" AND p.status=?"); params.add(status); }
        sql.append(" ORDER BY p.stage, p.agreement_id");
        return BaseDao.query(sql.toString(), this::mapPayable, params.toArray());
    }

    /* 统计 */
    public java.math.BigDecimal sumReceivable(long projectId) {
        return BaseDao.queryOne("SELECT COALESCE(SUM(total_amount),0) FROM prj_receivable WHERE project_id=?",
                rs -> rs.getBigDecimal(1), projectId);
    }

    public java.math.BigDecimal sumPayable(long projectId, String distributorTypeInclude) {
        String sql = "SELECT COALESCE(SUM(p.total_amount),0) FROM prj_payable p ";
        if (distributorTypeInclude != null) {
            sql += "INNER JOIN prj_downstream_agreement a ON p.agreement_id=a.id AND a.distributor_type=? ";
            sql += "WHERE p.project_id=?";
            return BaseDao.queryOne(sql, rs -> rs.getBigDecimal(1), distributorTypeInclude, projectId);
        } else {
            sql += "WHERE p.project_id=?";
            return BaseDao.queryOne(sql, rs -> rs.getBigDecimal(1), projectId);
        }
    }

    private Receivable mapReceivable(ResultSet rs) throws SQLException {
        Receivable r = new Receivable();
        r.setId(rs.getLong("id"));
        r.setProjectId(rs.getLong("project_id"));
        r.setStage(rs.getString("stage"));
        r.setScaleAmount(BaseDao.toBigDecimal(rs.getObject("scale_amount")));
        r.setAssessAmount(BaseDao.toBigDecimal(rs.getObject("assess_amount")));
        r.setTotalAmount(BaseDao.toBigDecimal(rs.getObject("total_amount")));
        r.setEstimateAmount(BaseDao.toBigDecimal(rs.getObject("estimate_amount")));
        r.setStatus(rs.getString("status"));
        r.setFillUser(rs.getObject("fill_user") == null ? null : rs.getLong("fill_user"));
        r.setFillTime(rs.getTimestamp("fill_time"));
        r.setAuditUser(rs.getObject("audit_user") == null ? null : rs.getLong("audit_user"));
        r.setAuditTime(rs.getTimestamp("audit_time"));
        r.setRemark(rs.getString("remark"));
        try { r.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        try { r.setFillUserName(rs.getString("fill_user_name")); } catch (Exception ignore) {}
        try { r.setAuditUserName(rs.getString("audit_user_name")); } catch (Exception ignore) {}
        return r;
    }

    private Payable mapPayable(ResultSet rs) throws SQLException {
        Payable p = new Payable();
        p.setId(rs.getLong("id"));
        p.setProjectId(rs.getLong("project_id"));
        p.setAgreementId(rs.getLong("agreement_id"));
        p.setStage(rs.getString("stage"));
        p.setScaleAmount(BaseDao.toBigDecimal(rs.getObject("scale_amount")));
        p.setAssessAmount(BaseDao.toBigDecimal(rs.getObject("assess_amount")));
        p.setTotalAmount(BaseDao.toBigDecimal(rs.getObject("total_amount")));
        p.setEstimateAmount(BaseDao.toBigDecimal(rs.getObject("estimate_amount")));
        p.setStatus(rs.getString("status"));
        p.setFillUser(rs.getObject("fill_user") == null ? null : rs.getLong("fill_user"));
        p.setFillTime(rs.getTimestamp("fill_time"));
        p.setAuditUser(rs.getObject("audit_user") == null ? null : rs.getLong("audit_user"));
        p.setAuditTime(rs.getTimestamp("audit_time"));
        p.setConfirmUser(rs.getObject("confirm_user") == null ? null : rs.getLong("confirm_user"));
        p.setConfirmTime(rs.getTimestamp("confirm_time"));
        p.setRemark(rs.getString("remark"));
        try { p.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        try { p.setAgreementName(rs.getString("agreement_name")); } catch (Exception ignore) {}
        try { p.setDistributor(rs.getString("distributor")); } catch (Exception ignore) {}
        try { p.setDistributorType(rs.getString("distributor_type")); } catch (Exception ignore) {}
        try { p.setFillUserName(rs.getString("fill_user_name")); } catch (Exception ignore) {}
        try { p.setAuditUserName(rs.getString("audit_user_name")); } catch (Exception ignore) {}
        try { p.setConfirmUserName(rs.getString("confirm_user_name")); } catch (Exception ignore) {}
        return p;
    }
    
    /**
     * 批量查询多个项目的应收总额（从应收台账表）
     * @param projectIds 项目ID列表
     * @return Map: key=projectId, value=Map with receivableTotal
     */
    public Map<Long, Map<String, java.math.BigDecimal>> sumReceivableBatch(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT project_id, " +
                "COALESCE(SUM(total_amount), 0) as receivable_total " +
                "FROM prj_receivable WHERE project_id IN (" + placeholders + ") AND status IN ('DRAFT','AUDIT','FINAL','CONFIRMED') " +
                "GROUP BY project_id";
        List<Object> params = new ArrayList<>(projectIds);
        return BaseDao.query(sql, (rs) -> {
            Map<String, java.math.BigDecimal> m = new HashMap<>();
            m.put("receivableTotal", rs.getBigDecimal("receivable_total"));
            return new AbstractMap.SimpleEntry<>(rs.getLong("project_id"), m);
        }, params.toArray()).stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }
    
    /**
     * 批量查询多个项目的外部应付总额（从应付台账表，只统计分销商类型为外部公司的）
     * @param projectIds 项目ID列表
     * @return Map: key=projectId, value=Map with externalPayable
     */
    public Map<Long, Map<String, java.math.BigDecimal>> sumPayableBatch(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT p.project_id, " +
                "COALESCE(SUM(p.total_amount), 0) as external_payable " +
                "FROM prj_payable p " +
                "INNER JOIN prj_downstream_agreement a ON p.agreement_id = a.id AND a.distributor_type = '外部公司' " +
                "WHERE p.project_id IN (" + placeholders + ") AND p.status IN ('DRAFT','AUDIT','FINAL','CONFIRMED') " +
                "GROUP BY p.project_id";
        List<Object> params = new ArrayList<>(projectIds);
        return BaseDao.query(sql, (rs) -> {
            Map<String, java.math.BigDecimal> m = new HashMap<>();
            m.put("externalPayable", rs.getBigDecimal("external_payable"));
            return new AbstractMap.SimpleEntry<>(rs.getLong("project_id"), m);
        }, params.toArray()).stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }
}

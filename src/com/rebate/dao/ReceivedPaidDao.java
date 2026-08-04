package com.rebate.dao;

import com.rebate.config.AppConfig;
import com.rebate.model.Paid;
import com.rebate.model.Received;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实收/实付 DAO
 */
public class ReceivedPaidDao {

    /**
     * 从BPM数据库按项目编号获取可用的实收数据列表
     * @param projectCode 项目编号
     */
    public List<Map<String, Object>> listBpmReceived(String projectCode) {
        String url = AppConfig.get("bpm.jdbc.url");
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("BPM数据库未配置(bpm.jdbc.url)");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT 返利类型, 记账日期, 财务编码, 发票号码, 返利金额, 销售税率, " +
                "价税合计, 部门应得, 收款部门, 项目编号 " +
                "FROM rebate_received WHERE 项目编号 = ? " +
                "ORDER BY 记账日期 DESC";
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, projectCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("rebateType", rs.getString("返利类型"));
                m.put("applyDate", rs.getDate("记账日期") == null ? null : rs.getDate("记账日期").toString());
                m.put("financeCode", rs.getString("财务编码"));
                m.put("invoiceNo", rs.getString("发票号码"));
                m.put("rebateAmount", rs.getBigDecimal("返利金额"));
                m.put("taxRate", rs.getBigDecimal("销售税率"));
                m.put("totalPriceTax", rs.getBigDecimal("价税合计"));
                m.put("deptShare", rs.getBigDecimal("部门应得"));
                m.put("receiveDept", rs.getString("收款部门"));
                m.put("projectBpmId", rs.getString("项目编号"));
                list.add(m);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("从BPM获取实收数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按BPM来源+财务编码查询是否已引入（避免重复）
     */
    public int countByBpmFinanceCode(String financeCode) {
        if (financeCode == null || financeCode.isEmpty()) return 0;
        Long cnt = BaseDao.queryOne(
                "SELECT COUNT(*) FROM prj_received WHERE finance_code=?",
                rs -> rs.getLong(1), financeCode);
        return cnt == null ? 0 : cnt.intValue();
    }

    /**
     * 批量查询已引入的财务编码（用于前端标记）
     * @param financeCodes 财务编码列表
     * @return 已存在的财务编码Set
     */
    public java.util.Set<String> findImportedFinanceCodes(List<String> financeCodes) {
        if (financeCodes == null || financeCodes.isEmpty()) {
            return new java.util.HashSet<>();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(financeCodes.size(), "?"));
        String sql = "SELECT finance_code FROM prj_received WHERE finance_code IN (" + placeholders + ")";
        List<String> result = BaseDao.query(sql,
                (java.sql.ResultSet rs) -> rs.getString("finance_code"),
                financeCodes.toArray());
        return new java.util.HashSet<>(result);
    }

    public Long insertReceived(Received r) {
        return BaseDao.insertReturnId("INSERT INTO prj_received(project_id, stage, rebate_type, applicant, apply_dept, " +
                "apply_date, finance_code, rebate_amount, tax_rate, total_price_tax, dept_share, invoice_no, " +
                "receive_dept, status, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.getProjectId(), r.getStage(), r.getRebateType(), r.getApplicant(), r.getApplyDept(),
                r.getApplyDate(), r.getFinanceCode(), r.getRebateAmount(), r.getTaxRate(), r.getTotalPriceTax(),
                r.getDeptShare(), r.getInvoiceNo(), r.getReceiveDept(), r.getStatus() == null ? "DRAFT" : r.getStatus(),
                r.getRemark());
    }

    public int updateReceived(Received r) {
        return BaseDao.update("UPDATE prj_received SET stage=?, rebate_type=?, applicant=?, apply_dept=?, " +
                "apply_date=?, finance_code=?, rebate_amount=?, tax_rate=?, total_price_tax=?, dept_share=?, " +
                "invoice_no=?, receive_dept=?, status=?, remark=? WHERE id=?",
                r.getStage(), r.getRebateType(), r.getApplicant(), r.getApplyDept(), r.getApplyDate(), r.getFinanceCode(),
                r.getRebateAmount(), r.getTaxRate(), r.getTotalPriceTax(), r.getDeptShare(), r.getInvoiceNo(),
                r.getReceiveDept(), r.getStatus(), r.getRemark(), r.getId());
    }

    /**
     * 确认环节：BPM流程触发后回写状态
     * @param step PURCHASE / OP / FINANCE
     */
    public int confirmReceived(long id, String step, long userId) {
        String sql;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        switch (step) {
            case "PURCHASE":
                sql = "UPDATE prj_received SET purchase_user=?, purchase_time=?, status='PURCHASE' WHERE id=?";
                return BaseDao.update(sql, userId, now, id);
            case "OP":
                sql = "UPDATE prj_received SET op_user=?, op_time=?, status='OP' WHERE id=?";
                return BaseDao.update(sql, userId, now, id);
            case "FINANCE":
                sql = "UPDATE prj_received SET finance_user=?, finance_time=?, status='FINANCE' WHERE id=?";
                return BaseDao.update(sql, userId, now, id);
            default:
                return 0;
        }
    }

    public List<Received> listReceivedByProject(long projectId, String stage, String rebateType) {
        StringBuilder sql = new StringBuilder("SELECT r.*, p.project_name FROM prj_received r " +
                "LEFT JOIN prj_project p ON r.project_id=p.id WHERE r.project_id=?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(projectId);
        if (stage != null && !stage.isEmpty()) { sql.append(" AND r.stage=?"); params.add(stage); }
        if (rebateType != null && !rebateType.isEmpty()) { sql.append(" AND r.rebate_type=?"); params.add(rebateType); }
        sql.append(" ORDER BY r.apply_date DESC, r.id DESC");
        return BaseDao.query(sql.toString(), this::mapReceived, params.toArray());
    }

    public List<Paid> listPaidByProject(long projectId, String stage, String rebateType) {
        StringBuilder sql = new StringBuilder("SELECT p.*, prj.project_name, a.agreement_name FROM prj_paid p " +
                "LEFT JOIN prj_project prj ON p.project_id=prj.id " +
                "LEFT JOIN prj_downstream_agreement a ON p.agreement_id=a.id WHERE p.project_id=?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(projectId);
        if (stage != null && !stage.isEmpty()) { sql.append(" AND p.stage=?"); params.add(stage); }
        if (rebateType != null && !rebateType.isEmpty()) { sql.append(" AND p.rebate_type=?"); params.add(rebateType); }
        sql.append(" ORDER BY p.apply_date DESC, p.id DESC");
        return BaseDao.query(sql.toString(), this::mapPaid, params.toArray());
    }

    public int deleteReceived(long id) {
        return BaseDao.update("DELETE FROM prj_received WHERE id=?", id);
    }

    public int deletePaid(long id) {
        return BaseDao.update("DELETE FROM prj_paid WHERE id=?", id);
    }

    public Long insertPaid(Paid p) {
        return BaseDao.insertReturnId("INSERT INTO prj_paid(project_id, agreement_id, stage, rebate_type, " +
                "applicant, apply_dept, apply_date, receive_dept, customer_name, total_rebate, actual_rebate, " +
                "diff_amount, execute_status, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getProjectId(), p.getAgreementId(), p.getStage(), p.getRebateType(), p.getApplicant(), p.getApplyDept(),
                p.getApplyDate(), p.getReceiveDept(), p.getCustomerName(), p.getTotalRebate(), p.getActualRebate(),
                p.getDiffAmount(), p.getExecuteStatus() == null ? "DRAFT" : p.getExecuteStatus(), p.getRemark());
    }

    public int updatePaid(Paid p) {
        return BaseDao.update("UPDATE prj_paid SET agreement_id=?, stage=?, rebate_type=?, applicant=?, apply_dept=?, " +
                "apply_date=?, receive_dept=?, customer_name=?, total_rebate=?, actual_rebate=?, diff_amount=?, " +
                "execute_status=?, remark=? WHERE id=?",
                p.getAgreementId(), p.getStage(), p.getRebateType(), p.getApplicant(), p.getApplyDept(), p.getApplyDate(),
                p.getReceiveDept(), p.getCustomerName(), p.getTotalRebate(), p.getActualRebate(), p.getDiffAmount(),
                p.getExecuteStatus(), p.getRemark(), p.getId());
    }

    public int confirmPaid(long id, String step, long userId) {
        String sql;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        switch (step) {
            case "OP":
                sql = "UPDATE prj_paid SET op_user=?, op_time=?, execute_status='OP' WHERE id=?";
                return BaseDao.update(sql, userId, now, id);
            case "FINANCE":
                sql = "UPDATE prj_paid SET finance_user=?, finance_time=?, execute_status='FINANCE' WHERE id=?";
                return BaseDao.update(sql, userId, now, id);
            default:
                return 0;
        }
    }

    public List<Paid> listPaidByProject(long projectId) {
        return BaseDao.query("SELECT p.*, prj.project_name, a.agreement_name FROM prj_paid p " +
                "LEFT JOIN prj_project prj ON p.project_id=prj.id " +
                "LEFT JOIN prj_downstream_agreement a ON p.agreement_id=a.id " +
                "WHERE p.project_id=? ORDER BY p.apply_date DESC, p.id DESC", this::mapPaid, projectId);
    }

    public BigDecimal sumReceived(long projectId) {
        return BaseDao.queryOne("SELECT COALESCE(SUM(dept_share),0) FROM prj_received WHERE project_id=?",
                rs -> rs.getBigDecimal(1), projectId);
    }

    public BigDecimal sumPaid(long projectId, boolean externalOnly) {
        String sql = "SELECT COALESCE(SUM(p.actual_rebate),0) FROM prj_paid p ";
        if (externalOnly) {
            sql += "INNER JOIN prj_downstream_agreement a ON p.agreement_id=a.id AND a.distributor_type='外部公司' ";
            sql += "WHERE p.project_id=?";
        } else {
            sql += "WHERE p.project_id=?";
        }
        return BaseDao.queryOne(sql, rs -> rs.getBigDecimal(1), projectId);
    }
    
    /**
     * 批量查询多个项目的实收总额（从实收表）
     * @param projectIds 项目ID列表
     * @return Map: key=projectId, value=Map with receivedTotal
     */
    public Map<Long, Map<String, BigDecimal>> sumReceivedBatch(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return new HashMap<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT project_id, COALESCE(SUM(dept_share), 0) as received_total " +
                "FROM prj_received WHERE project_id IN (" + placeholders + ") GROUP BY project_id";
        List<Object> params = new ArrayList<>(projectIds);
        return BaseDao.query(sql, (rs) -> {
            Map<String, BigDecimal> m = new HashMap<>();
            m.put("receivedTotal", rs.getBigDecimal("received_total"));
            return new java.util.AbstractMap.SimpleEntry<>(rs.getLong("project_id"), m);
        }, params.toArray()).stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }
    
    /**
     * 批量查询多个项目的外部实付总额（从实付表，只统计分销商类型为外部公司的）
     * @param projectIds 项目ID列表
     * @return Map: key=projectId, value=Map with externalPaid
     */
    public Map<Long, Map<String, BigDecimal>> sumPaidBatch(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return new HashMap<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(projectIds.size(), "?"));
        String sql = "SELECT p.project_id, COALESCE(SUM(p.actual_rebate), 0) as external_paid " +
                "FROM prj_paid p " +
                "INNER JOIN prj_downstream_agreement a ON p.agreement_id = a.id AND a.distributor_type = '外部公司' " +
                "WHERE p.project_id IN (" + placeholders + ") GROUP BY p.project_id";
        List<Object> params = new ArrayList<>(projectIds);
        return BaseDao.query(sql, (rs) -> {
            Map<String, BigDecimal> m = new HashMap<>();
            m.put("externalPaid", rs.getBigDecimal("external_paid"));
            return new java.util.AbstractMap.SimpleEntry<>(rs.getLong("project_id"), m);
        }, params.toArray()).stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    private Received mapReceived(ResultSet rs) throws SQLException {
        Received r = new Received();
        r.setId(rs.getLong("id"));
        r.setProjectId(rs.getLong("project_id"));
        r.setStage(rs.getString("stage"));
        r.setRebateType(rs.getString("rebate_type"));
        r.setApplicant(rs.getString("applicant"));
        r.setApplyDept(rs.getString("apply_dept"));
        r.setApplyDate(rs.getDate("apply_date"));
        r.setFinanceCode(rs.getString("finance_code"));
        r.setRebateAmount(BaseDao.toBigDecimal(rs.getObject("rebate_amount")));
        r.setTaxRate(BaseDao.toBigDecimal(rs.getObject("tax_rate")));
        r.setTotalPriceTax(BaseDao.toBigDecimal(rs.getObject("total_price_tax")));
        r.setDeptShare(BaseDao.toBigDecimal(rs.getObject("dept_share")));
        r.setInvoiceNo(rs.getString("invoice_no"));
        r.setReceiveDept(rs.getString("receive_dept"));
        r.setStatus(rs.getString("status"));
        r.setBpmProcessId(rs.getString("bpm_process_id"));
        r.setPurchaseUser(rs.getObject("purchase_user") == null ? null : rs.getLong("purchase_user"));
        r.setPurchaseTime(rs.getTimestamp("purchase_time"));
        r.setOpUser(rs.getObject("op_user") == null ? null : rs.getLong("op_user"));
        r.setOpTime(rs.getTimestamp("op_time"));
        r.setFinanceUser(rs.getObject("finance_user") == null ? null : rs.getLong("finance_user"));
        r.setFinanceTime(rs.getTimestamp("finance_time"));
        r.setFinalTime(rs.getTimestamp("final_time"));
        r.setRemark(rs.getString("remark"));
        try { r.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        return r;
    }

    private Paid mapPaid(ResultSet rs) throws SQLException {
        Paid p = new Paid();
        p.setId(rs.getLong("id"));
        p.setProjectId(rs.getLong("project_id"));
        p.setAgreementId(rs.getObject("agreement_id") == null ? null : rs.getLong("agreement_id"));
        p.setStage(rs.getString("stage"));
        p.setRebateType(rs.getString("rebate_type"));
        p.setApplicant(rs.getString("applicant"));
        p.setApplyDept(rs.getString("apply_dept"));
        p.setApplyDate(rs.getDate("apply_date"));
        p.setReceiveDept(rs.getString("receive_dept"));
        p.setCustomerName(rs.getString("customer_name"));
        p.setTotalRebate(BaseDao.toBigDecimal(rs.getObject("total_rebate")));
        p.setActualRebate(BaseDao.toBigDecimal(rs.getObject("actual_rebate")));
        p.setDiffAmount(BaseDao.toBigDecimal(rs.getObject("diff_amount")));
        p.setExecuteStatus(rs.getString("execute_status"));
        p.setBpmProcessId(rs.getString("bpm_process_id"));
        p.setOpUser(rs.getObject("op_user") == null ? null : rs.getLong("op_user"));
        p.setOpTime(rs.getTimestamp("op_time"));
        p.setFinanceUser(rs.getObject("finance_user") == null ? null : rs.getLong("finance_user"));
        p.setFinanceTime(rs.getTimestamp("finance_time"));
        p.setFinalTime(rs.getTimestamp("final_time"));
        p.setRemark(rs.getString("remark"));
        try { p.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        try { p.setAgreementName(rs.getString("agreement_name")); } catch (Exception ignore) {}
        return p;
    }
}

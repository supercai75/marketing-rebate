package com.rebate.dao;

import com.rebate.model.ProjectExpense;
import com.rebate.model.ProjectLabor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用/人工 DAO
 */
public class CostDao {

    /* 项目费用 */
    public Long insertExpense(ProjectExpense e) {
        return BaseDao.insertReturnId("INSERT INTO fin_project_expense(project_id, reimburse_date, expense_type, " +
                "work_no, name, description, amount, allocated_amount, source, raw_project_name, doc_no, " +
                "matched_type, import_user, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getProjectId(), e.getReimburseDate(), e.getExpenseType(), e.getWorkNo(), e.getName(),
                e.getDescription(), e.getAmount(), e.getAllocatedAmount() == null ? e.getAmount() : e.getAllocatedAmount(),
                e.getSource(), e.getRawProjectName(), e.getDocNo(), e.getMatchedType(),
                e.getImportUser(), e.getRemark());
    }

    public Long insertExpenseWithConn(Connection conn, ProjectExpense e) throws SQLException {
        return BaseDao.insertReturnIdWithConn(conn, "INSERT INTO fin_project_expense(project_id, reimburse_date, expense_type, " +
                "work_no, name, description, amount, allocated_amount, source, raw_project_name, doc_no, " +
                "matched_type, import_user, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                e.getProjectId(), e.getReimburseDate(), e.getExpenseType(), e.getWorkNo(), e.getName(),
                e.getDescription(), e.getAmount(), e.getAllocatedAmount() == null ? e.getAmount() : e.getAllocatedAmount(),
                e.getSource(), e.getRawProjectName(), e.getDocNo(), e.getMatchedType(),
                e.getImportUser(), e.getRemark());
    }

    public int updateExpense(ProjectExpense e) {
        return BaseDao.update("UPDATE fin_project_expense SET reimburse_date=?, expense_type=?, work_no=?, name=?, " +
                "description=?, amount=?, allocated_amount=?, doc_no=?, remark=? WHERE id=?",
                e.getReimburseDate(), e.getExpenseType(), e.getWorkNo(), e.getName(), e.getDescription(),
                e.getAmount(), e.getAllocatedAmount() == null ? e.getAmount() : e.getAllocatedAmount(),
                e.getDocNo(), e.getRemark(), e.getId());
    }

    public int deleteExpense(long id) {
        return BaseDao.update("DELETE FROM fin_project_expense WHERE id=?", id);
    }

    public ProjectExpense findExpense(long id) {
        return BaseDao.queryOne("SELECT * FROM fin_project_expense WHERE id=?", this::mapExpense, id);
    }

    public int updateExpenseProject(long expenseId, long projectId, String matchedType) {
        return BaseDao.update("UPDATE fin_project_expense SET project_id=?, matched_type=? WHERE id=?",
                projectId, matchedType, expenseId);
    }

    public List<ProjectExpense> listExpenses(Long projectId, String workNo, String expenseType, String docNo, String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("SELECT e.*, p.project_name FROM fin_project_expense e " +
                "LEFT JOIN prj_project p ON e.project_id=p.id " +
                "WHERE (? IS NULL OR e.project_id=?) ");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        params.add(projectId);
        
        if (workNo != null && !workNo.isEmpty()) {
            sql.append("AND (e.work_no LIKE ? OR e.name LIKE ?) ");
            params.add("%" + workNo + "%");
            params.add("%" + workNo + "%");
        }
        if (expenseType != null && !expenseType.isEmpty()) {
            sql.append("AND e.expense_type=? ");
            params.add(expenseType);
        }
        if (docNo != null && !docNo.isEmpty()) {
            sql.append("AND e.doc_no LIKE ? ");
            params.add("%" + docNo + "%");
        }
        if (startDate != null && !startDate.isEmpty()) {
            sql.append("AND e.reimburse_date >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append("AND e.reimburse_date <= ? ");
            params.add(Date.valueOf(endDate));
        }
        
        sql.append("ORDER BY e.reimburse_date DESC, e.id DESC LIMIT 10000");
        return BaseDao.query(sql.toString(), this::mapExpense, params.toArray());
    }

    /* 项目人工 */
    public Long insertLabor(ProjectLabor l) {
        return BaseDao.insertReturnId("INSERT INTO fin_project_labor(project_id, month_yyyymm, work_no, name, work_type, " +
                "salary, welfare, other_cost, total_cost, allocated_amount, alloc_ratio, " +
                "source, matched_type, import_user, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                l.getProjectId(), l.getMonthYyyymm(), l.getWorkNo(), l.getName(), l.getWorkType(),
                l.getSalary(), l.getWelfare(), l.getOtherCost(), l.getTotalCost(),
                l.getAllocatedAmount() == null ? l.getTotalCost() : l.getAllocatedAmount(),
                l.getAllocRatio(),
                l.getSource(), l.getMatchedType(), l.getImportUser(), l.getRemark());
    }

    public Long insertLaborWithConn(Connection conn, ProjectLabor l) throws SQLException {
        return BaseDao.insertReturnIdWithConn(conn, "INSERT INTO fin_project_labor(project_id, month_yyyymm, work_no, name, work_type, " +
                "salary, welfare, other_cost, total_cost, allocated_amount, alloc_ratio, " +
                "source, matched_type, import_user, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                l.getProjectId(), l.getMonthYyyymm(), l.getWorkNo(), l.getName(), l.getWorkType(),
                l.getSalary(), l.getWelfare(), l.getOtherCost(), l.getTotalCost(),
                l.getAllocatedAmount() == null ? l.getTotalCost() : l.getAllocatedAmount(),
                l.getAllocRatio(),
                l.getSource(), l.getMatchedType(), l.getImportUser(), l.getRemark());
    }

    public int updateLabor(ProjectLabor l) {
        return BaseDao.update("UPDATE fin_project_labor SET month_yyyymm=?, work_no=?, name=?, work_type=?, " +
                "salary=?, welfare=?, other_cost=?, total_cost=?, allocated_amount=?, alloc_ratio=?, remark=? WHERE id=?",
                l.getMonthYyyymm(), l.getWorkNo(), l.getName(), l.getWorkType(),
                l.getSalary(), l.getWelfare(), l.getOtherCost(), l.getTotalCost(),
                l.getAllocatedAmount() == null ? l.getTotalCost() : l.getAllocatedAmount(),
                l.getAllocRatio(), l.getRemark(), l.getId());
    }

    public int deleteLabor(long id) {
        return BaseDao.update("DELETE FROM fin_project_labor WHERE id=?", id);
    }

    public ProjectLabor findLabor(long id) {
        return BaseDao.queryOne("SELECT * FROM fin_project_labor WHERE id=?", this::mapLabor, id);
    }

    public int updateLaborProject(long laborId, long projectId, String matchedType) {
        return BaseDao.update("UPDATE fin_project_labor SET project_id=?, matched_type=? WHERE id=?",
                projectId, matchedType, laborId);
    }

    public List<ProjectLabor> listLabors(Long projectId, String workNo) {
        String sql = "SELECT l.*, p.project_name FROM fin_project_labor l " +
                "LEFT JOIN prj_project p ON l.project_id=p.id " +
                "WHERE (? IS NULL OR l.project_id=?) AND (?='' OR l.work_no=?) ORDER BY l.month_yyyymm DESC, l.id DESC LIMIT 1000";
        return BaseDao.query(sql, this::mapLabor, projectId, projectId, workNo == null ? "" : workNo, workNo == null ? "" : workNo);
    }

    public java.math.BigDecimal sumExpenseByProject(long projectId) {
        return BaseDao.queryOne("SELECT COALESCE(SUM(allocated_amount),0) FROM fin_project_expense WHERE project_id=?",
                rs -> rs.getBigDecimal(1), projectId);
    }

    public java.math.BigDecimal sumLaborByProject(long projectId) {
        return BaseDao.queryOne("SELECT COALESCE(SUM(allocated_amount),0) FROM fin_project_labor WHERE project_id=?",
                rs -> rs.getBigDecimal(1), projectId);
    }

    /**
     * 费用分月汇总
     */
    public List<Map<String, Object>> sumExpenseByMonth(long projectId) {
        String sql = "SELECT to_char(reimburse_date, 'yyyyMM') as month_yyyymm, " +
                "COALESCE(SUM(allocated_amount), 0) as total_amount " +
                "FROM fin_project_expense WHERE project_id = ? " +
                "GROUP BY to_char(reimburse_date, 'yyyyMM') ORDER BY month_yyyymm";
        return BaseDao.query(sql, (rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("amount", rs.getBigDecimal("total_amount"));
            return m;
        }, projectId);
    }

    /**
     * 费用分月分类型汇总
     */
    public List<Map<String, Object>> sumExpenseByMonthAndType(long projectId) {
        String sql = "SELECT to_char(reimburse_date, 'yyyyMM') as month_yyyymm, expense_type, " +
                "COALESCE(SUM(amount), 0) as invoice_amount, " +
                "COALESCE(SUM(allocated_amount), 0) as allocated_amount " +
                "FROM fin_project_expense WHERE project_id = ? " +
                "GROUP BY to_char(reimburse_date, 'yyyyMM'), expense_type ORDER BY month_yyyymm, expense_type";
        return BaseDao.query(sql, (rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("expenseType", rs.getString("expense_type"));
            m.put("invoiceAmount", rs.getBigDecimal("invoice_amount"));
            m.put("amount", rs.getBigDecimal("allocated_amount"));
            return m;
        }, projectId);
    }

    /**
     * 人工分月汇总
     */
    public List<Map<String, Object>> sumLaborByMonth(long projectId) {
        String sql = "SELECT month_yyyymm, COALESCE(SUM(allocated_amount), 0) as total_amount " +
                "FROM fin_project_labor WHERE project_id = ? " +
                "GROUP BY month_yyyymm ORDER BY month_yyyymm";
        return BaseDao.query(sql, (rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("amount", rs.getBigDecimal("total_amount"));
            return m;
        }, projectId);
    }

    /**
     * 人工分月汇总（含费用合计）
     */
    public List<Map<String, Object>> sumLaborByMonthWithDetail(long projectId) {
        String sql = "SELECT month_yyyymm, COALESCE(SUM(total_cost), 0) as total_cost, " +
                "COALESCE(SUM(allocated_amount), 0) as allocated_amount " +
                "FROM fin_project_labor WHERE project_id = ? " +
                "GROUP BY month_yyyymm ORDER BY month_yyyymm";
        return BaseDao.query(sql, (rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("expenseType", "人工及福利投入");
            m.put("invoiceAmount", rs.getBigDecimal("total_cost"));
            m.put("amount", rs.getBigDecimal("allocated_amount"));
            return m;
        }, projectId);
    }

    private ProjectExpense mapExpense(ResultSet rs) throws SQLException {
        ProjectExpense e = new ProjectExpense();
        e.setId(rs.getLong("id"));
        e.setProjectId(rs.getObject("project_id") == null ? null : rs.getLong("project_id"));
        e.setReimburseDate(rs.getDate("reimburse_date"));
        e.setExpenseType(rs.getString("expense_type"));
        e.setWorkNo(rs.getString("work_no"));
        e.setName(rs.getString("name"));
        e.setDescription(rs.getString("description"));
        e.setAmount(BaseDao.toBigDecimal(rs.getObject("amount")));
        e.setAllocatedAmount(BaseDao.toBigDecimal(rs.getObject("allocated_amount")));
        e.setSource(rs.getString("source"));
        e.setRawProjectName(rs.getString("raw_project_name"));
        try { e.setDocNo(rs.getString("doc_no")); } catch (Exception ignore) {}
        e.setMatchedType(rs.getString("matched_type"));
        e.setImportUser(rs.getObject("import_user") == null ? null : rs.getLong("import_user"));
        e.setImportTime(rs.getTimestamp("import_time"));
        e.setRemark(rs.getString("remark"));
        try { e.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        return e;
    }

    private ProjectLabor mapLabor(ResultSet rs) throws SQLException {
        ProjectLabor l = new ProjectLabor();
        l.setId(rs.getLong("id"));
        l.setProjectId(rs.getObject("project_id") == null ? null : rs.getLong("project_id"));
        l.setMonthYyyymm(rs.getString("month_yyyymm"));
        l.setWorkNo(rs.getString("work_no"));
        l.setName(rs.getString("name"));
        try { l.setWorkType(rs.getString("work_type")); } catch (Exception ignore) {}
        l.setSalary(BaseDao.toBigDecimal(rs.getObject("salary")));
        l.setWelfare(BaseDao.toBigDecimal(rs.getObject("welfare")));
        l.setOtherCost(BaseDao.toBigDecimal(rs.getObject("other_cost")));
        l.setTotalCost(BaseDao.toBigDecimal(rs.getObject("total_cost")));
        try { l.setAllocatedAmount(BaseDao.toBigDecimal(rs.getObject("allocated_amount"))); } catch (Exception ignore) {}
        try { l.setAllocRatio(BaseDao.toBigDecimal(rs.getObject("alloc_ratio"))); } catch (Exception ignore) {}
        try { l.setSource(rs.getString("source")); } catch (Exception ignore) {}
        l.setMatchedType(rs.getString("matched_type"));
        l.setImportUser(rs.getObject("import_user") == null ? null : rs.getLong("import_user"));
        l.setImportTime(rs.getTimestamp("import_time"));
        l.setRemark(rs.getString("remark"));
        try { l.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        return l;
    }
    
    /**
     * 批量查询多个项目的投入汇总 (expense + labor)
     * @param projectIds 项目ID列表
     * @return Map: key=projectId, value=Map with investTotal
     */
    public Map<Long, Map<String, java.math.BigDecimal>> sumCostBatch(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) return Collections.emptyMap();
        String placeholders = String.join(",", Collections.nCopies(projectIds.size(), "?"));
        
        // 费用投入
        String sqlExpense = "SELECT project_id, COALESCE(SUM(allocated_amount), 0) as expense_total " +
                "FROM fin_project_expense WHERE project_id IN (" + placeholders + ") GROUP BY project_id";
        List<Object> params = new ArrayList<>(projectIds);
        Map<Long, java.math.BigDecimal> expenseMap = BaseDao.query(sqlExpense, (rs) -> {
            return new AbstractMap.SimpleEntry<>(rs.getLong("project_id"), rs.getBigDecimal("expense_total"));
        }, params.toArray()).stream().collect(java.util.HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        
        // 人工投入
        String sqlLabor = "SELECT project_id, COALESCE(SUM(allocated_amount), 0) as labor_total " +
                "FROM fin_project_labor WHERE project_id IN (" + placeholders + ") GROUP BY project_id";
        Map<Long, java.math.BigDecimal> laborMap = BaseDao.query(sqlLabor, (rs) -> {
            return new AbstractMap.SimpleEntry<>(rs.getLong("project_id"), rs.getBigDecimal("labor_total"));
        }, params.toArray()).stream().collect(java.util.HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        
        // 合并结果
        Map<Long, Map<String, java.math.BigDecimal>> result = new java.util.HashMap<>();
        for (Long pid : projectIds) {
            java.math.BigDecimal expense = expenseMap.getOrDefault(pid, java.math.BigDecimal.ZERO);
            java.math.BigDecimal labor = laborMap.getOrDefault(pid, java.math.BigDecimal.ZERO);
            Map<String, java.math.BigDecimal> m = new java.util.HashMap<>();
            m.put("investTotal", expense.add(labor));
            result.put(pid, m);
        }
        return result;
    }
}

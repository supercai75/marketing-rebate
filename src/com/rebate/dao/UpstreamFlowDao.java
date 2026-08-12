package com.rebate.dao;

import com.rebate.model.UpstreamFlowBatch;
import com.rebate.model.UpstreamFlowRecord;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上游流向 DAO
 */
public class UpstreamFlowDao {

    /**
     * 向 SQL 追加 AND column IN (?, ?, ?) 子句（如果 inList 非空）。
     * 如果 inList 为空，不追加任何内容（不影响 SQL）。
     */
    private static void appendInClause(StringBuilder sb, List<Object> params, String column, List<String> inList) {
        if (inList == null || inList.isEmpty()) return;
        sb.append(" AND ").append(column).append(" IN (");
        for (int i = 0; i < inList.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
            params.add(inList.get(i));
        }
        sb.append(')');
    }

    public Long insertBatch(UpstreamFlowBatch b) {
        return BaseDao.insertReturnId("INSERT INTO flow_upstream_batch(project_id, batch_code, file_name, file_path, " +
                "import_user, month_summary, remark) VALUES (?, ?, ?, ?, ?, ?, ?)",
                b.getProjectId(), b.getBatchCode(), b.getFileName(), b.getFilePath(), b.getImportUser(),
                b.getMonthSummary(), b.getRemark());
    }

    /**
     * 使用外部Connection插入批次（用于事务）
     */
    public Long insertBatchWithConn(java.sql.Connection conn, UpstreamFlowBatch b) throws SQLException {
        return BaseDao.insertReturnIdWithConn(conn, "INSERT INTO flow_upstream_batch(project_id, batch_code, file_name, file_path, " +
                "import_user, month_summary, remark) VALUES (?, ?, ?, ?, ?, ?, ?)",
                b.getProjectId(), b.getBatchCode(), b.getFileName(), b.getFilePath(), b.getImportUser(),
                b.getMonthSummary(), b.getRemark());
    }

    /**
     * 同月份已有数据全部置为失效，返回受影响的记录数
     */
    public int invalidateExisting(long projectId, List<String> months) {
        if (months == null || months.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder("UPDATE flow_upstream_record SET is_valid=0 WHERE project_id=? AND is_final=0 AND month_yyyymm IN (");
        for (int i = 0; i < months.size(); i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(")");
        Object[] params = new Object[months.size() + 1];
        params[0] = projectId;
        for (int i = 0; i < months.size(); i++) params[i + 1] = months.get(i);
        return BaseDao.update(sb.toString(), params);
    }

    /**
     * 使用外部Connection失效旧数据（用于事务）
     */
    public int invalidateExistingWithConn(java.sql.Connection conn, long projectId, List<String> months) throws SQLException {
        if (months == null || months.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder("UPDATE flow_upstream_record SET is_valid=0 WHERE project_id=? AND is_final=0 AND month_yyyymm IN (");
        for (int i = 0; i < months.size(); i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(")");
        Object[] params = new Object[months.size() + 1];
        params[0] = projectId;
        for (int i = 0; i < months.size(); i++) params[i + 1] = months.get(i);
        return BaseDao.updateWithConn(conn, sb.toString(), params);
    }

    public int insertRecord(UpstreamFlowRecord r) {
        return BaseDao.update("INSERT INTO flow_upstream_record(project_id, batch_id, month_yyyymm, business_date, " +
                "product_name, spec, seller_name, seller_city, calc_price, quantity, calc_amount, buyer_name, buyer_city, " +
                "assess_group_id, is_valid, is_final, raw_row) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)",
                r.getProjectId(), r.getBatchId(), r.getMonthYyyymm(), r.getBusinessDate(),
                r.getProductName(), r.getSpec(), r.getSellerName(), r.getSellerCity(),
                r.getCalcPrice(), r.getQuantity(), r.getCalcAmount(), r.getBuyerName(),
                r.getBuyerCity(), r.getAssessGroupId(), r.getRawRow());
    }

    /**
     * 使用外部Connection插入记录（用于事务）
     */
    public int insertRecordWithConn(java.sql.Connection conn, UpstreamFlowRecord r) throws SQLException {
        return BaseDao.updateWithConn(conn, "INSERT INTO flow_upstream_record(project_id, batch_id, month_yyyymm, business_date, " +
                "product_name, spec, seller_name, seller_city, calc_price, quantity, calc_amount, buyer_name, buyer_city, " +
                "assess_group_id, is_valid, is_final, raw_row) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)",
                r.getProjectId(), r.getBatchId(), r.getMonthYyyymm(), r.getBusinessDate(),
                r.getProductName(), r.getSpec(), r.getSellerName(), r.getSellerCity(),
                r.getCalcPrice(), r.getQuantity(), r.getCalcAmount(), r.getBuyerName(),
                r.getBuyerCity(), r.getAssessGroupId(), r.getRawRow());
    }

    public List<UpstreamFlowBatch> listBatches(long projectId) {
        String sql = "SELECT b.*, p.project_name, u.name AS import_user_name, " +
                "CASE " +
                "  WHEN COUNT(r.id) = 0 THEN 1 " + // 无记录视为失效
                "  WHEN COUNT(r.id) = SUM(CASE WHEN r.is_final = 1 THEN 1 ELSE 0 END) THEN 3 " + // 全部终版
                "  WHEN SUM(CASE WHEN r.is_valid = 1 AND r.is_final = 0 THEN 1 ELSE 0 END) = 0 THEN 1 " + // 全部失效（无有效非终版）
                "  WHEN SUM(CASE WHEN r.is_valid = 0 THEN 1 ELSE 0 END) > 0 THEN 2 " + // 部分失效
                "  ELSE 0 END AS status " + // 默认生效
                "FROM flow_upstream_batch b " +
                "LEFT JOIN prj_project p ON b.project_id=p.id " +
                "LEFT JOIN sys_user u ON b.import_user=u.id " +
                "LEFT JOIN flow_upstream_record r ON b.id = r.batch_id " +
                "WHERE b.project_id=? " +
                "GROUP BY b.id, p.project_name, u.name " +
                "ORDER BY b.import_time DESC";
        return BaseDao.query(sql, this::mapBatch, projectId);
    }

    public UpstreamFlowBatch findBatch(long id) {
        String sql = "SELECT b.*, p.project_name, u.name AS import_user_name, " +
                "CASE " +
                "  WHEN COUNT(r.id) = 0 THEN 1 " +
                "  WHEN COUNT(r.id) = SUM(CASE WHEN r.is_final = 1 THEN 1 ELSE 0 END) THEN 3 " +
                "  WHEN SUM(CASE WHEN r.is_valid = 1 AND r.is_final = 0 THEN 1 ELSE 0 END) = 0 THEN 1 " +
                "  WHEN SUM(CASE WHEN r.is_valid = 0 THEN 1 ELSE 0 END) > 0 THEN 2 " +
                "  ELSE 0 END AS status " +
                "FROM flow_upstream_batch b " +
                "LEFT JOIN prj_project p ON b.project_id=p.id " +
                "LEFT JOIN sys_user u ON b.import_user=u.id " +
                "LEFT JOIN flow_upstream_record r ON b.id = r.batch_id " +
                "WHERE b.id=? " +
                "GROUP BY b.id, p.project_name, u.name";
        return BaseDao.queryOne(sql, this::mapBatch, id);
    }

    public List<UpstreamFlowRecord> listRecords(long projectId, String month, Integer isValid) {
        StringBuilder sb = new StringBuilder("SELECT r.*, g.group_name as assessGroupName FROM flow_upstream_record r " +
                "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id WHERE r.project_id = ? ");
        Object[] params;
        if (month != null && !month.isEmpty()) {
            sb.append("AND r.month_yyyymm = ? ");
            if (isValid != null) {
                sb.append("AND r.is_valid = ? ");
                params = new Object[]{projectId, month, isValid};
            } else {
                params = new Object[]{projectId, month};
            }
        } else if (isValid != null) {
            sb.append("AND r.is_valid = ? ");
            params = new Object[]{projectId, isValid};
        } else {
            params = new Object[]{projectId};
        }
        sb.append("ORDER BY r.month_yyyymm, r.id");
        return BaseDao.query(sb.toString(), this::mapRecordWithGroup, params);
    }

    public long countRecords(long projectId, String month, Integer isValid) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM flow_upstream_record r WHERE r.project_id = ? ");
        Object[] params;
        if (month != null && !month.isEmpty()) {
            sb.append("AND r.month_yyyymm = ? ");
            if (isValid != null) {
                sb.append("AND r.is_valid = ? ");
                params = new Object[]{projectId, month, isValid};
            } else {
                params = new Object[]{projectId, month};
            }
        } else if (isValid != null) {
            sb.append("AND r.is_valid = ? ");
            params = new Object[]{projectId, isValid};
        } else {
            params = new Object[]{projectId};
        }
        Long count = BaseDao.queryOne(sb.toString(), rs -> rs.getLong(1), params);
        return count != null ? count : 0L;
    }

    public List<UpstreamFlowRecord> listRecordsPage(long projectId, String month, Integer isValid, int page, int pageSize) {
        StringBuilder sb = new StringBuilder("SELECT r.*, g.group_name as assessGroupName FROM flow_upstream_record r " +
                "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id WHERE r.project_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        if (month != null && !month.isEmpty()) {
            sb.append("AND r.month_yyyymm = ? ");
            params.add(month);
        }
        if (isValid != null) {
            sb.append("AND r.is_valid = ? ");
            params.add(isValid);
        }
        sb.append("ORDER BY r.month_yyyymm, r.id LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        return BaseDao.query(sb.toString(), this::mapRecordWithGroup, params.toArray());
    }

    public List<UpstreamFlowRecord> listRecordsByBatch(long batchId) {
        String sql = "SELECT r.*, g.group_name as assessGroupName FROM flow_upstream_record r " +
                "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id WHERE r.batch_id=? ORDER BY r.id";
        return BaseDao.query(sql, this::mapRecordWithGroup, batchId);
    }
    
    /**
     * 获取有效或最终稿的记录ID列表（用于全选功能）
     */
    public List<Long> listValidRecordIds(long projectId) {
        String sql = "SELECT id FROM flow_upstream_record WHERE project_id=? AND (is_valid=1 OR is_final=1)";
        return BaseDao.query(sql, (ResultSet rs) -> rs.getLong("id"), projectId);
    }

    public List<Long> listValidRecordIdsWithFilters(long projectId, String month, String productName, String spec, String sellerName, String buyerName) {
        StringBuilder sb = new StringBuilder("SELECT id FROM flow_upstream_record WHERE project_id=? AND (is_valid=1 OR is_final=1)");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        if (month != null && !month.isEmpty()) { sb.append(" AND month_yyyymm=?"); params.add(month); }
        if (productName != null && !productName.isEmpty()) { sb.append(" AND product_name LIKE ?"); params.add("%" + productName + "%"); }
        if (spec != null && !spec.isEmpty()) { sb.append(" AND spec LIKE ?"); params.add("%" + spec + "%"); }
        if (sellerName != null && !sellerName.isEmpty()) { sb.append(" AND seller_name LIKE ?"); params.add("%" + sellerName + "%"); }
        if (buyerName != null && !buyerName.isEmpty()) { sb.append(" AND buyer_name LIKE ?"); params.add("%" + buyerName + "%"); }
        return BaseDao.query(sb.toString(), (ResultSet rs) -> rs.getLong("id"), params.toArray());
    }
    
    /**
     * 获取可以分解的上游流向记录（未被分解到下游协议的有效记录）
     * 同时支持 LIKE 模糊和 IN 精确列表；IN 和 LIKE 都写时用 AND 组合。
     */
    public List<UpstreamFlowRecord> listCanSplitRecords(long projectId, String month,
                                                         String productName, List<String> productNameIn,
                                                         String spec,
                                                         String sellerName, List<String> sellerNameIn,
                                                         String buyerName, List<String> buyerNameIn,
                                                         String buyerCity, List<String> buyerCityIn,
                                                         Integer page, Integer pageSize) {
        StringBuilder sb = new StringBuilder("SELECT r.*, g.group_name as assessGroupName FROM flow_upstream_record r " +
                "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id WHERE r.project_id=? AND r.is_valid=1 " +
                "AND r.id NOT IN (SELECT upstream_flow_record_id FROM flow_downstream_record WHERE project_id=?)");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        params.add(projectId);
        if (month != null && !month.isEmpty()) { sb.append(" AND r.month_yyyymm=?"); params.add(month); }
        if (productName != null && !productName.isEmpty()) { sb.append(" AND r.product_name LIKE ?"); params.add("%" + productName + "%"); }
        appendInClause(sb, params, "r.product_name", productNameIn);
        if (spec != null && !spec.isEmpty()) { sb.append(" AND r.spec LIKE ?"); params.add("%" + spec + "%"); }
        if (sellerName != null && !sellerName.isEmpty()) { sb.append(" AND r.seller_name LIKE ?"); params.add("%" + sellerName + "%"); }
        appendInClause(sb, params, "r.seller_name", sellerNameIn);
        if (buyerName != null && !buyerName.isEmpty()) { sb.append(" AND r.buyer_name LIKE ?"); params.add("%" + buyerName + "%"); }
        appendInClause(sb, params, "r.buyer_name", buyerNameIn);
        if (buyerCity != null && !buyerCity.isEmpty()) { sb.append(" AND r.buyer_city LIKE ?"); params.add("%" + buyerCity + "%"); }
        appendInClause(sb, params, "r.buyer_city", buyerCityIn);
        sb.append(" ORDER BY r.month_yyyymm, r.id");
        if (page != null && pageSize != null) {
            sb.append(" LIMIT ").append(pageSize).append(" OFFSET ").append((page - 1) * pageSize);
        } else {
            sb.append(" LIMIT 10000");
        }
        return BaseDao.query(sb.toString(), this::mapRecordWithGroup, params.toArray());
    }

    /**
     * 获取可以分解的上游流向记录ID总数
     */
    public int countCanSplitRecords(long projectId, String month,
                                    String productName, List<String> productNameIn,
                                    String spec,
                                    String sellerName, List<String> sellerNameIn,
                                    String buyerName, List<String> buyerNameIn,
                                    String buyerCity, List<String> buyerCityIn) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM flow_upstream_record r WHERE r.project_id=? AND r.is_valid=1 " +
                "AND r.id NOT IN (SELECT upstream_flow_record_id FROM flow_downstream_record WHERE project_id=?)");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        params.add(projectId);
        if (month != null && !month.isEmpty()) { sb.append(" AND r.month_yyyymm=?"); params.add(month); }
        if (productName != null && !productName.isEmpty()) { sb.append(" AND r.product_name LIKE ?"); params.add("%" + productName + "%"); }
        appendInClause(sb, params, "r.product_name", productNameIn);
        if (spec != null && !spec.isEmpty()) { sb.append(" AND r.spec LIKE ?"); params.add("%" + spec + "%"); }
        if (sellerName != null && !sellerName.isEmpty()) { sb.append(" AND r.seller_name LIKE ?"); params.add("%" + sellerName + "%"); }
        appendInClause(sb, params, "r.seller_name", sellerNameIn);
        if (buyerName != null && !buyerName.isEmpty()) { sb.append(" AND r.buyer_name LIKE ?"); params.add("%" + buyerName + "%"); }
        appendInClause(sb, params, "r.buyer_name", buyerNameIn);
        if (buyerCity != null && !buyerCity.isEmpty()) { sb.append(" AND r.buyer_city LIKE ?"); params.add("%" + buyerCity + "%"); }
        appendInClause(sb, params, "r.buyer_city", buyerCityIn);
        Long count = BaseDao.queryOne(sb.toString(), (ResultSet rs) -> rs.getLong(1), params.toArray());
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取可以分解的上游流向记录ID列表（用于全选功能）
     */
    public List<Long> listCanSplitIds(long projectId, String month,
                                      String productName, List<String> productNameIn,
                                      String spec,
                                      String sellerName, List<String> sellerNameIn,
                                      String buyerName, List<String> buyerNameIn,
                                      String buyerCity, List<String> buyerCityIn) {
        StringBuilder sb = new StringBuilder("SELECT id FROM flow_upstream_record WHERE project_id=? AND is_valid=1 " +
                "AND id NOT IN (SELECT upstream_flow_record_id FROM flow_downstream_record WHERE project_id=?)");
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        params.add(projectId);
        if (month != null && !month.isEmpty()) { sb.append(" AND month_yyyymm=?"); params.add(month); }
        if (productName != null && !productName.isEmpty()) { sb.append(" AND product_name LIKE ?"); params.add("%" + productName + "%"); }
        appendInClause(sb, params, "product_name", productNameIn);
        if (spec != null && !spec.isEmpty()) { sb.append(" AND spec LIKE ?"); params.add("%" + spec + "%"); }
        if (sellerName != null && !sellerName.isEmpty()) { sb.append(" AND seller_name LIKE ?"); params.add("%" + sellerName + "%"); }
        appendInClause(sb, params, "seller_name", sellerNameIn);
        if (buyerName != null && !buyerName.isEmpty()) { sb.append(" AND buyer_name LIKE ?"); params.add("%" + buyerName + "%"); }
        appendInClause(sb, params, "buyer_name", buyerNameIn);
        if (buyerCity != null && !buyerCity.isEmpty()) { sb.append(" AND buyer_city LIKE ?"); params.add("%" + buyerCity + "%"); }
        appendInClause(sb, params, "buyer_city", buyerCityIn);
        return BaseDao.query(sb.toString(), (ResultSet rs) -> rs.getLong("id"), params.toArray());
    }

    private UpstreamFlowRecord mapRecordWithGroup(ResultSet rs) throws SQLException {
        UpstreamFlowRecord r = mapRecord(rs);
        try { r.setAssessGroupName(rs.getString("assessGroupName")); } catch (Exception ignore) {}
        return r;
    }

    /**
     * 按月份聚合（valid 流向）
     */
    public List<Map<String, Object>> sumByMonth(long projectId, String basis) {
        String sumCol = "AMT".equalsIgnoreCase(basis) ? "calc_amount" : "quantity";
        String sql = "SELECT r.month_yyyymm, " +
                "SUM(r.quantity) AS total_qty, " +
                "SUM(r.calc_amount) AS total_amt, " +
                "SUM(r." + sumCol + ") AS scale, " +
                "COUNT(*) AS cnt, " +
                "CASE WHEN f.month_yyyymm IS NOT NULL THEN 1 ELSE 0 END AS is_final " +
                "FROM flow_upstream_record r " +
                "LEFT JOIN flow_upstream_final f ON r.project_id = f.project_id AND r.month_yyyymm = f.month_yyyymm " +
                "WHERE r.project_id=? AND r.is_valid=1 " +
                "GROUP BY r.month_yyyymm, f.month_yyyymm " +
                "ORDER BY r.month_yyyymm";
        return BaseDao.query(sql, (ResultSet rs) -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("qtyCount", rs.getBigDecimal("total_qty"));
            m.put("totalAmt", rs.getBigDecimal("total_amt"));
            m.put("scale", rs.getBigDecimal("scale"));
            m.put("count", rs.getLong("cnt"));
            m.put("isFinal", rs.getInt("is_final") == 1 ? "Y" : "N");
            return m;
        }, projectId);
    }

    public int setFinalMonth(long projectId, String month, Long userId) {
        return BaseDao.update("INSERT INTO flow_upstream_final(project_id, month_yyyymm, set_user) VALUES (?, ?, ?) " +
                "ON CONFLICT (project_id, month_yyyymm) DO NOTHING", projectId, month, userId);
    }

    public int cancelFinalMonth(long projectId, String month) {
        return BaseDao.update("DELETE FROM flow_upstream_final WHERE project_id=? AND month_yyyymm=?", projectId, month);
    }

    public List<String> listFinalMonths(long projectId) {
        return BaseDao.query("SELECT month_yyyymm FROM flow_upstream_final WHERE project_id=?",
                (ResultSet rs) -> rs.getString("month_yyyymm"), projectId);
    }

    /**
     * 获取已存在流向数据的月份列表（有效或终版）
     */
    public java.util.List<String> listExistingMonths(long projectId) {
        return BaseDao.query("SELECT DISTINCT month_yyyymm FROM flow_upstream_record WHERE project_id=? AND is_valid=1 ORDER BY month_yyyymm",
                (ResultSet rs) -> rs.getString("month_yyyymm"), projectId);
    }

    /**
     * 检查指定月份是否已存在且为终版
     */
    public boolean hasFinalMonth(long projectId, String month) {
        Long count = BaseDao.queryOne("SELECT COUNT(*) FROM flow_upstream_final WHERE project_id=? AND month_yyyymm=?",
                (ResultSet rs) -> rs.getLong(1), projectId, month);
        return count != null && count > 0;
    }

    public int markFinalInRecords(long projectId, String month) {
        return BaseDao.update("UPDATE flow_upstream_record SET is_final=1 WHERE project_id=? AND month_yyyymm=? AND is_valid=1",
                projectId, month);
    }

    public int unmarkFinalInRecords(long projectId, String month) {
        return BaseDao.update("UPDATE flow_upstream_record SET is_final=0 WHERE project_id=? AND month_yyyymm=?", projectId, month);
    }

    private UpstreamFlowBatch mapBatch(ResultSet rs) throws SQLException {
        UpstreamFlowBatch b = new UpstreamFlowBatch();
        b.setId(rs.getLong("id"));
        b.setProjectId(rs.getLong("project_id"));
        b.setBatchCode(rs.getString("batch_code"));
        b.setFileName(rs.getString("file_name"));
        b.setFilePath(rs.getString("file_path"));
        b.setImportUser(rs.getObject("import_user") == null ? null : rs.getLong("import_user"));
        b.setImportTime(rs.getTimestamp("import_time"));
        b.setMonthSummary(rs.getString("month_summary"));
        b.setRemark(rs.getString("remark"));
        try { b.setProjectName(rs.getString("project_name")); } catch (Exception ignore) {}
        try { b.setImportUserName(rs.getString("import_user_name")); } catch (Exception ignore) {}
        try { b.setStatus(rs.getInt("status")); } catch (Exception ignore) {}
        return b;
    }

    private UpstreamFlowRecord mapRecord(ResultSet rs) throws SQLException {
        UpstreamFlowRecord r = new UpstreamFlowRecord();
        r.setId(rs.getLong("id"));
        r.setProjectId(rs.getLong("project_id"));
        r.setBatchId(rs.getLong("batch_id"));
        r.setMonthYyyymm(rs.getString("month_yyyymm"));
        r.setBusinessDate(rs.getDate("business_date"));
        r.setProductName(rs.getString("product_name"));
        r.setSpec(rs.getString("spec"));
        r.setSellerName(rs.getString("seller_name"));
        r.setSellerCity(rs.getString("seller_city"));
        r.setCalcPrice(BaseDao.toBigDecimal(rs.getObject("calc_price")));
        r.setQuantity(BaseDao.toBigDecimal(rs.getObject("quantity")));
        r.setCalcAmount(BaseDao.toBigDecimal(rs.getObject("calc_amount")));
        r.setBuyerName(rs.getString("buyer_name"));
        r.setBuyerCity(rs.getString("buyer_city"));
        r.setAssessGroupId(rs.getObject("assess_group_id") == null ? null : rs.getLong("assess_group_id"));
        r.setIsValid(rs.getInt("is_valid"));
        r.setIsFinal(rs.getInt("is_final"));
        r.setRawRow(rs.getString("raw_row"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}

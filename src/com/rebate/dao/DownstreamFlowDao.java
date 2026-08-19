package com.rebate.dao;

import com.rebate.model.DownstreamFlowRecord;
import com.rebate.service.ProjectScaleService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 下游流向 DAO
 */
public class DownstreamFlowDao {

    public List<DownstreamFlowRecord> listRecords(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                                                   String productName, String spec, String sellerName, String sellerCity) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.*, g.group_name as assessGroupName, b.batch_code as sourceBatchCode " +
            "FROM flow_downstream_record r " +
            "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id " +
            "LEFT JOIN flow_downstream_batch b ON r.batch_id = b.id " +
            "WHERE r.project_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND r.month_yyyymm = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND r.product_name LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND r.spec LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND r.seller_name LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND r.seller_city LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND r.buyer_name LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND r.buyer_city LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND r.customer_level = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND r.is_valid = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND r.agreement_id = ? ");
            params.add(agreementId);
        }
        sql.append("ORDER BY r.created_at DESC");

        return BaseDao.query(sql.toString(), this::mapRecordWithGroup, params.toArray());
    }

    public long countRecords(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                              String productName, String spec, String sellerName, String sellerCity) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM flow_downstream_record r WHERE r.project_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND r.month_yyyymm = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND r.product_name LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND r.spec LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND r.seller_name LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND r.seller_city LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND r.buyer_name LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND r.buyer_city LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND r.customer_level = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND r.is_valid = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND r.agreement_id = ? ");
            params.add(agreementId);
        }

        Long count = BaseDao.queryOne(sql.toString(), rs -> rs.getLong(1), params.toArray());
        return count != null ? count : 0L;
    }

    public List<DownstreamFlowRecord> listRecordsPage(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                                                       String productName, String spec, String sellerName, String sellerCity, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(
            "SELECT r.*, g.group_name as assessGroupName, b.batch_code as sourceBatchCode " +
            "FROM flow_downstream_record r " +
            "LEFT JOIN prj_assess_group g ON r.assess_group_id = g.id " +
            "LEFT JOIN flow_downstream_batch b ON r.batch_id = b.id " +
            "WHERE r.project_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND r.month_yyyymm = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND r.product_name LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND r.spec LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND r.seller_name LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND r.seller_city LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND r.buyer_name LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND r.buyer_city LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND r.customer_level = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND r.is_valid = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND r.agreement_id = ? ");
            params.add(agreementId);
        }
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        return BaseDao.query(sql.toString(), this::mapRecordWithGroup, params.toArray());
    }

    private DownstreamFlowRecord mapRecordWithGroup(ResultSet rs) throws SQLException {
        DownstreamFlowRecord r = mapRecord(rs);
        try { r.setAssessGroupName(rs.getString("assessGroupName")); } catch (Exception ignore) {}
        try { r.setSourceBatchCode(rs.getString("sourceBatchCode")); } catch (Exception ignore) {}
        return r;
    }

    private DownstreamFlowRecord mapRecord(ResultSet rs) throws SQLException {
        DownstreamFlowRecord r = new DownstreamFlowRecord();
        r.setId(rs.getLong("id"));
        r.setProjectId(rs.getLong("project_id"));
        r.setAgreementId(rs.getLong("agreement_id"));
        r.setBatchId(rs.getObject("batch_id") == null ? null : rs.getLong("batch_id"));
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
        r.setCustomerLevel(rs.getString("customer_level"));
        r.setSaleQty(BaseDao.toBigDecimal(rs.getObject("sale_qty")));
        r.setNoTaxAmount(BaseDao.toBigDecimal(rs.getObject("no_tax_amount")));
        r.setTaxAmount(BaseDao.toBigDecimal(rs.getObject("tax_amount")));
        r.setBidAmount(BaseDao.toBigDecimal(rs.getObject("bid_amount")));
        r.setAssessGroupId(rs.getObject("assess_group_id") == null ? null : rs.getLong("assess_group_id"));
        r.setInvalidReason(rs.getString("invalid_reason"));
        r.setInvalidTime(rs.getTimestamp("invalid_time"));
        r.setIsValid(rs.getInt("is_valid"));
        r.setIsFinal(rs.getInt("is_final"));
        r.setRawRow(rs.getString("raw_row"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }

    public int updateAssessGroup(Long recordId, Long assessGroupId) {
        return BaseDao.update("UPDATE flow_downstream_record SET assess_group_id = ? WHERE id = ?", assessGroupId, recordId);
    }

    public List<Map<String, Object>> sumByMonth(Long projectId, String basis, Long assessGroupId) {
        return sumByMonth(projectId, basis, assessGroupId, null);
    }
    
    public List<Map<String, Object>> sumByMonth(Long projectId, String basis, Long assessGroupId, Long agreementId) {
        String sumCol = ProjectScaleService.basisToColumn(basis);
        StringBuilder sql = new StringBuilder(
            "SELECT month_yyyymm, SUM(" + sumCol + ") as month_scale, COUNT(*) as cnt " +
            "FROM flow_downstream_record WHERE project_id = ? AND is_valid = 1 "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);
        
        // 处理考核组过滤: ID > 0 时按具体 ID 匹配，ID 为 null 或 0 时匹配未分组记录 (NULL 或 0)
        if (assessGroupId != null && assessGroupId > 0) {
            sql.append("AND assess_group_id = ? ");
            params.add(assessGroupId);
        } else {
            // assessGroupId 为 null 或 0 时，匹配 assess_group_id IS NULL 或 0 的记录
            sql.append("AND (assess_group_id IS NULL OR assess_group_id = 0) ");
        }
        
        if (agreementId != null) {
            sql.append("AND agreement_id = ? ");
            params.add(agreementId);
        }
        
        sql.append("GROUP BY month_yyyymm ORDER BY month_yyyymm");
        
        return BaseDao.query(sql.toString(), (ResultSet rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("scale", rs.getBigDecimal("month_scale"));
            m.put("count", rs.getLong("cnt"));
            return m;
        }, params.toArray());
    }
    
    /**
     * 按下游协议聚合
     */
    public List<Map<String, Object>> sumByAgreement(Long agreementId, String basis) {
        String sumCol = ProjectScaleService.basisToColumn(basis);
        String sql = "SELECT r.month_yyyymm, " +
                "SUM(r.quantity) AS total_qty, " +
                "SUM(r.calc_amount) AS total_amt, " +
                "SUM(r." + sumCol + ") AS scale, " +
                "COUNT(*) AS cnt, " +
                "CASE WHEN f.month_yyyymm IS NOT NULL THEN 1 ELSE 0 END AS is_final " +
                "FROM flow_downstream_record r " +
                "LEFT JOIN flow_upstream_final f ON r.project_id = f.project_id AND r.month_yyyymm = f.month_yyyymm " +
                "WHERE r.agreement_id = ? AND r.is_valid = 1 " +
                "GROUP BY r.month_yyyymm, f.month_yyyymm " +
                "ORDER BY r.month_yyyymm";
        return BaseDao.query(sql, (ResultSet rs) -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", rs.getString("month_yyyymm"));
            m.put("qtyCount", rs.getBigDecimal("total_qty"));
            m.put("scale", rs.getBigDecimal("scale"));
            m.put("count", rs.getLong("cnt"));
            m.put("isFinal", rs.getInt("is_final") == 1 ? "Y" : "N");
            return m;
        }, agreementId);
    }

    public List<Long> listSplitUpstreamIds(Long projectId) {
        String sql = "SELECT DISTINCT upstream_flow_record_id FROM flow_downstream_record WHERE project_id = ?";
        return BaseDao.query(sql, (ResultSet rs) -> rs.getLong("upstream_flow_record_id"), projectId);
    }

    /**
     * 删除单条下游流向记录（剔除后对应上游记录可被重新选择）
     */
    public int deleteRecord(long recordId) {
        return BaseDao.update("DELETE FROM flow_downstream_record WHERE id = ?", recordId);
    }

    /**
     * 删除指定项目 + 协议下的所有有效下游流向记录（全部剔除）
     * 如果 agreementId 为 null，则删除该项目下所有有效下游流向记录
     */
    public int deleteAllValidRecords(long projectId, Long agreementId) {
        if (agreementId == null || agreementId == 0) {
            return BaseDao.update("DELETE FROM flow_downstream_record WHERE project_id = ? AND is_valid = 1", projectId);
        }
        return BaseDao.update("DELETE FROM flow_downstream_record WHERE project_id = ? AND agreement_id = ? AND is_valid = 1", projectId, agreementId);
    }

    /** 检查项目是否注册了直接导入下游流向 */
    public boolean isDirectImportProject(long projectId) {
        Long count = BaseDao.queryOne("SELECT COUNT(*) FROM proj_flow_set WHERE project_id = ?",
                (ResultSet rs) -> rs.getLong(1), projectId);
        return count != null && count > 0;
    }

    /** 直接插入下游流向记录（不经过上游分解） */
    public int insertDirectRecord(DownstreamFlowRecord r) {
        return BaseDao.update("INSERT INTO flow_downstream_record(project_id, agreement_id, batch_id, upstream_flow_record_id, " +
                "month_yyyymm, business_date, product_name, spec, seller_name, seller_city, calc_price, quantity, " +
                "calc_amount, buyer_name, buyer_city, customer_level, sale_qty, no_tax_amount, tax_amount, bid_amount, " +
                "assess_group_id, is_valid, is_final, raw_row) " +
                "VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)",
                r.getProjectId(), r.getAgreementId(), r.getBatchId(),
                r.getMonthYyyymm(), r.getBusinessDate(), r.getProductName(), r.getSpec(),
                r.getSellerName(), r.getSellerCity(), r.getCalcPrice(), r.getQuantity(),
                r.getCalcAmount(), r.getBuyerName(), r.getBuyerCity(), r.getCustomerLevel(),
                r.getSaleQty(), r.getNoTaxAmount(), r.getTaxAmount(), r.getBidAmount(),
                r.getAssessGroupId(), r.getRawRow());
    }

    /** 使用外部Connection直接插入下游流向记录（用于事务） */
    public int insertDirectRecordWithConn(java.sql.Connection conn, DownstreamFlowRecord r) throws SQLException {
        return BaseDao.updateWithConn(conn, "INSERT INTO flow_downstream_record(project_id, agreement_id, batch_id, upstream_flow_record_id, " +
                "month_yyyymm, business_date, product_name, spec, seller_name, seller_city, calc_price, quantity, " +
                "calc_amount, buyer_name, buyer_city, customer_level, sale_qty, no_tax_amount, tax_amount, bid_amount, " +
                "assess_group_id, is_valid, is_final, raw_row) " +
                "VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)",
                r.getProjectId(), r.getAgreementId(), r.getBatchId(),
                r.getMonthYyyymm(), r.getBusinessDate(), r.getProductName(), r.getSpec(),
                r.getSellerName(), r.getSellerCity(), r.getCalcPrice(), r.getQuantity(),
                r.getCalcAmount(), r.getBuyerName(), r.getBuyerCity(), r.getCustomerLevel(),
                r.getSaleQty(), r.getNoTaxAmount(), r.getTaxAmount(), r.getBidAmount(),
                r.getAssessGroupId(), r.getRawRow());
    }

    /** 插入下游流向批次 */
    public Long insertDownstreamBatch(long projectId, long agreementId, String batchCode, String fileName, String filePath, long importUser, String monthSummary, String remark) {
        return BaseDao.insertReturnId("INSERT INTO flow_downstream_batch(project_id, agreement_id, batch_code, file_name, file_path, " +
                "import_user, month_summary, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                projectId, agreementId, batchCode, fileName, filePath, importUser, monthSummary, remark);
    }

    /** 使用外部Connection插入下游流向批次（用于事务） */
    public Long insertDownstreamBatchWithConn(java.sql.Connection conn, long projectId, long agreementId, String batchCode, String fileName, String filePath, long importUser, String monthSummary, String remark) throws SQLException {
        return BaseDao.insertReturnIdWithConn(conn, "INSERT INTO flow_downstream_batch(project_id, agreement_id, batch_code, file_name, file_path, " +
                "import_user, month_summary, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                projectId, agreementId, batchCode, fileName, filePath, importUser, monthSummary, remark);
    }

    /** 使用外部Connection失效同项目+协议下指定月份的旧有效下游记录（用于事务） */
    public int invalidateExistingWithConn(java.sql.Connection conn, long projectId, long agreementId, java.util.List<String> months) throws SQLException {
        if (months == null || months.isEmpty()) return 0;
        StringBuilder sb = new StringBuilder("UPDATE flow_downstream_record SET is_valid=0 WHERE project_id=? AND agreement_id=? AND is_final=0 AND month_yyyymm IN (");
        for (int i = 0; i < months.size(); i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(")");
        Object[] params = new Object[months.size() + 2];
        params[0] = projectId;
        params[1] = agreementId;
        for (int i = 0; i < months.size(); i++) params[i + 2] = months.get(i);
        return BaseDao.updateWithConn(conn, sb.toString(), params);
    }

    
    /**
     * 分解上游流向到下游协议
     */
    public int decompose(long projectId, long agreementId, List<Long> upstreamRecordIds) {
        if (upstreamRecordIds == null || upstreamRecordIds.isEmpty()) return 0;

        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < upstreamRecordIds.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
        }

        // SQL参数顺序: agreementId, upstream_ids..., projectId(where), projectId(not exists), agreementId(not exists)
        // 共 1 + N + 3 = N + 4 个
        String insertSql = "INSERT INTO flow_downstream_record(project_id, agreement_id, batch_id, " +
                "upstream_flow_record_id, month_yyyymm, business_date, product_name, spec, seller_name, " +
                "seller_city, calc_price, quantity, calc_amount, buyer_name, buyer_city, " +
                "customer_level, sale_qty, no_tax_amount, tax_amount, bid_amount, assess_group_id, is_valid, " +
                "is_final, raw_row) " +
                "SELECT u.project_id, ?, u.batch_id, u.id, u.month_yyyymm, u.business_date, " +
                "u.product_name, u.spec, u.seller_name, u.seller_city, u.calc_price, u.quantity, " +
                "u.calc_amount, u.buyer_name, u.buyer_city, " +
                "u.customer_level, u.sale_qty, u.no_tax_amount, u.tax_amount, u.bid_amount, " +
                "u.assess_group_id, 1, 0, u.raw_row " +
                "FROM flow_upstream_record u " +
                "WHERE u.id IN (" + inClause + ") " +
                "AND u.project_id = ? " +
                "AND NOT EXISTS (SELECT 1 FROM flow_downstream_record d " +
                "WHERE d.project_id = ? AND d.agreement_id = ? AND d.upstream_flow_record_id = u.id)";

        Object[] allParams = new Object[upstreamRecordIds.size() + 4];
        allParams[0] = agreementId;
        for (int i = 0; i < upstreamRecordIds.size(); i++) {
            allParams[1 + i] = upstreamRecordIds.get(i);
        }
        allParams[1 + upstreamRecordIds.size()] = projectId;
        allParams[2 + upstreamRecordIds.size()] = projectId;
        allParams[3 + upstreamRecordIds.size()] = agreementId;

        return BaseDao.update(insertSql, allParams);
    }
    
    /**
     * 列出下游协议的记录，关联上游流向
     */
    public List<com.rebate.model.DownstreamFlowRecord> listRecordsWithUpstream(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                                                                                String productName, String spec, String sellerName, String sellerCity) {
        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.project_id, d.agreement_id, d.batch_id, d.upstream_flow_record_id, " +
            "COALESCE(u.month_yyyymm, d.month_yyyymm) AS month_yyyymm, " +
            "COALESCE(u.business_date, d.business_date) AS business_date, " +
            "COALESCE(u.product_name, d.product_name) AS product_name, " +
            "COALESCE(u.spec, d.spec) AS spec, " +
            "COALESCE(u.seller_name, d.seller_name) AS seller_name, " +
            "COALESCE(u.seller_city, d.seller_city) AS seller_city, " +
            "COALESCE(u.calc_price, d.calc_price) AS calc_price, " +
            "COALESCE(u.quantity, d.quantity) AS quantity, " +
            "COALESCE(u.calc_amount, d.calc_amount) AS calc_amount, " +
            "COALESCE(u.buyer_name, d.buyer_name) AS buyer_name, " +
            "COALESCE(u.buyer_city, d.buyer_city) AS buyer_city, " +
            "COALESCE(u.customer_level, d.customer_level) AS customer_level, " +
            "COALESCE(u.sale_qty, d.sale_qty) AS sale_qty, " +
            "COALESCE(u.no_tax_amount, d.no_tax_amount) AS no_tax_amount, " +
            "COALESCE(u.tax_amount, d.tax_amount) AS tax_amount, " +
            "COALESCE(u.bid_amount, d.bid_amount) AS bid_amount, " +
            "COALESCE(u.assess_group_id, d.assess_group_id) AS assess_group_id, " +
            "COALESCE(u.is_valid, d.is_valid) AS is_valid, " +
            "COALESCE(u.is_final, d.is_final) AS is_final, " +
            "g.group_name as assessGroupName, b.batch_code as sourceBatchCode " +
            "FROM flow_downstream_record d " +
            "LEFT JOIN flow_upstream_record u ON d.upstream_flow_record_id = u.id " +
            "LEFT JOIN prj_assess_group g ON d.assess_group_id = g.id " +
            "LEFT JOIN flow_downstream_batch b ON d.batch_id = b.id " +
            "WHERE d.project_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND COALESCE(u.month_yyyymm, d.month_yyyymm) = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND COALESCE(u.product_name, d.product_name) LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND COALESCE(u.spec, d.spec) LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND COALESCE(u.seller_name, d.seller_name) LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND COALESCE(u.seller_city, d.seller_city) LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_name, d.buyer_name) LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_city, d.buyer_city) LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND COALESCE(u.customer_level, d.customer_level) = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND COALESCE(u.is_valid, d.is_valid) = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND d.agreement_id = ? ");
            params.add(agreementId);
        }
        sql.append("ORDER BY d.created_at DESC");

        return BaseDao.query(sql.toString(), this::mapRecordWithUpstream, params.toArray());
    }

    public long countRecordsWithUpstream(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                                         String productName, String spec, String sellerName, String sellerCity) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM flow_downstream_record d " +
            "LEFT JOIN flow_upstream_record u ON d.upstream_flow_record_id = u.id " +
            "WHERE d.project_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND COALESCE(u.month_yyyymm, d.month_yyyymm) = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND COALESCE(u.product_name, d.product_name) LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND COALESCE(u.spec, d.spec) LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND COALESCE(u.seller_name, d.seller_name) LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND COALESCE(u.seller_city, d.seller_city) LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_name, d.buyer_name) LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_city, d.buyer_city) LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND COALESCE(u.customer_level, d.customer_level) = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND COALESCE(u.is_valid, d.is_valid) = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND d.agreement_id = ? ");
            params.add(agreementId);
        }

        Long count = BaseDao.queryOne(sql.toString(), rs -> rs.getLong(1), params.toArray());
        return count != null ? count : 0L;
    }

    public List<com.rebate.model.DownstreamFlowRecord> listRecordsWithUpstreamPage(Long projectId, String month, String buyerName, String buyerCity, String customerLevel, Integer isValid, Long agreementId,
                                                                                     String productName, String spec, String sellerName, String sellerCity, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(
            "SELECT d.id, d.project_id, d.agreement_id, d.batch_id, d.upstream_flow_record_id, " +
            "COALESCE(u.month_yyyymm, d.month_yyyymm) AS month_yyyymm, " +
            "COALESCE(u.business_date, d.business_date) AS business_date, " +
            "COALESCE(u.product_name, d.product_name) AS product_name, " +
            "COALESCE(u.spec, d.spec) AS spec, " +
            "COALESCE(u.seller_name, d.seller_name) AS seller_name, " +
            "COALESCE(u.seller_city, d.seller_city) AS seller_city, " +
            "COALESCE(u.calc_price, d.calc_price) AS calc_price, " +
            "COALESCE(u.quantity, d.quantity) AS quantity, " +
            "COALESCE(u.calc_amount, d.calc_amount) AS calc_amount, " +
            "COALESCE(u.buyer_name, d.buyer_name) AS buyer_name, " +
            "COALESCE(u.buyer_city, d.buyer_city) AS buyer_city, " +
            "COALESCE(u.customer_level, d.customer_level) AS customer_level, " +
            "COALESCE(u.sale_qty, d.sale_qty) AS sale_qty, " +
            "COALESCE(u.no_tax_amount, d.no_tax_amount) AS no_tax_amount, " +
            "COALESCE(u.tax_amount, d.tax_amount) AS tax_amount, " +
            "COALESCE(u.bid_amount, d.bid_amount) AS bid_amount, " +
            "COALESCE(u.assess_group_id, d.assess_group_id) AS assess_group_id, " +
            "COALESCE(u.is_valid, d.is_valid) AS is_valid, " +
            "COALESCE(u.is_final, d.is_final) AS is_final, " +
            "g.group_name as assessGroupName, b.batch_code as sourceBatchCode " +
            "FROM flow_downstream_record d " +
            "LEFT JOIN flow_upstream_record u ON d.upstream_flow_record_id = u.id " +
            "LEFT JOIN prj_assess_group g ON d.assess_group_id = g.id " +
            "LEFT JOIN flow_downstream_batch b ON d.batch_id = b.id " +
            "WHERE d.project_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(projectId);

        if (month != null && !month.isEmpty()) {
            sql.append("AND COALESCE(u.month_yyyymm, d.month_yyyymm) = ? ");
            params.add(month);
        }
        if (productName != null && !productName.isEmpty()) {
            sql.append("AND COALESCE(u.product_name, d.product_name) LIKE ? ");
            params.add("%" + productName + "%");
        }
        if (spec != null && !spec.isEmpty()) {
            sql.append("AND COALESCE(u.spec, d.spec) LIKE ? ");
            params.add("%" + spec + "%");
        }
        if (sellerName != null && !sellerName.isEmpty()) {
            sql.append("AND COALESCE(u.seller_name, d.seller_name) LIKE ? ");
            params.add("%" + sellerName + "%");
        }
        if (sellerCity != null && !sellerCity.isEmpty()) {
            sql.append("AND COALESCE(u.seller_city, d.seller_city) LIKE ? ");
            params.add("%" + sellerCity + "%");
        }
        if (buyerName != null && !buyerName.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_name, d.buyer_name) LIKE ? ");
            params.add("%" + buyerName + "%");
        }
        if (buyerCity != null && !buyerCity.isEmpty()) {
            sql.append("AND COALESCE(u.buyer_city, d.buyer_city) LIKE ? ");
            params.add("%" + buyerCity + "%");
        }
        if (customerLevel != null && !customerLevel.isEmpty()) {
            sql.append("AND COALESCE(u.customer_level, d.customer_level) = ? ");
            params.add(customerLevel);
        }
        if (isValid != null) {
            sql.append("AND COALESCE(u.is_valid, d.is_valid) = ? ");
            params.add(isValid);
        }
        if (agreementId != null) {
            sql.append("AND d.agreement_id = ? ");
            params.add(agreementId);
        }
        sql.append("ORDER BY d.created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        return BaseDao.query(sql.toString(), this::mapRecordWithUpstream, params.toArray());
    }

    private com.rebate.model.DownstreamFlowRecord mapRecordWithUpstream(ResultSet rs) throws SQLException {
        com.rebate.model.DownstreamFlowRecord r = new com.rebate.model.DownstreamFlowRecord();
        r.setId(rs.getLong("id"));
        r.setProjectId(rs.getLong("project_id"));
        r.setAgreementId(rs.getLong("agreement_id"));
        r.setBatchId(rs.getObject("batch_id") == null ? null : rs.getLong("batch_id"));
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
        r.setCustomerLevel(rs.getString("customer_level"));
        r.setSaleQty(BaseDao.toBigDecimal(rs.getObject("sale_qty")));
        r.setNoTaxAmount(BaseDao.toBigDecimal(rs.getObject("no_tax_amount")));
        r.setTaxAmount(BaseDao.toBigDecimal(rs.getObject("tax_amount")));
        r.setBidAmount(BaseDao.toBigDecimal(rs.getObject("bid_amount")));
        r.setAssessGroupId(rs.getObject("assess_group_id") == null ? null : rs.getLong("assess_group_id"));
        r.setIsValid(rs.getInt("is_valid"));
        r.setIsFinal(rs.getInt("is_final"));
        try { r.setAssessGroupName(rs.getString("assessGroupName")); } catch (Exception ignore) {}
        try { r.setSourceBatchCode(rs.getString("sourceBatchCode")); } catch (Exception ignore) {}
        return r;
    }
}

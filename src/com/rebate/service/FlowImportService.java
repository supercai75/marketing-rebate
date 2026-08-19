package com.rebate.service;

import com.rebate.dao.DownstreamFlowDao;
import com.rebate.dao.UpstreamFlowDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.model.AssessGroup;
import com.rebate.model.DownstreamFlowRecord;
import com.rebate.model.UpstreamFlowBatch;
import com.rebate.model.UpstreamFlowRecord;
import com.rebate.util.DBUtil;
import com.rebate.util.ExcelUtil;
import com.rebate.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 流向导入核心服务
 * <p>处理：Excel 解析 → 校验必填列 → 逐行严格校验（数值/月份/业务日期/长度）→
 * 同月份失效旧版本 → 写入新批次与明细。</p>
 * <p>任一数据行校验不通过时整体失败回滚，不允许部分数据写入。</p>
 */
public class FlowImportService {

    private final UpstreamFlowDao flowDao = new UpstreamFlowDao();
    private final DownstreamFlowDao downstreamFlowDao = new DownstreamFlowDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();

    private static final String[] REQUIRED_HEADERS = {
            "月份", "业务日期", "产品名称", "规格", "销售方名称", "销售方城市",
            "核算价格", "数量", "核算金额", "采购方名称"
    };

    /** 数值列：非空时必须是数字 */
    private static final String[] NUMERIC_COLUMNS = {
            "核算价格", "数量", "核算金额", "销售数量", "中标金额", "无税金额", "含税金额"
    };

    /** 文本列与数据库定义长度：超出则报错 */
    private static final Map<String, Integer> TEXT_COLUMN_MAX_LEN = new LinkedHashMap<>();
    static {
        TEXT_COLUMN_MAX_LEN.put("月份", 8);
        TEXT_COLUMN_MAX_LEN.put("产品名称", 255);
        TEXT_COLUMN_MAX_LEN.put("规格", 128);
        TEXT_COLUMN_MAX_LEN.put("销售方名称", 255);
        TEXT_COLUMN_MAX_LEN.put("销售方城市", 64);
        TEXT_COLUMN_MAX_LEN.put("采购方名称", 255);
        TEXT_COLUMN_MAX_LEN.put("采购方城市", 100);
        TEXT_COLUMN_MAX_LEN.put("客户等级", 50);
    }

    /**
     * @return 导入结果（batchId, totalCount, monthSummary, errorRows）
     */
    public Map<String, Object> importUpstream(long projectId, long userId, String fileName, InputStream in, List<String> selectedMonths) throws Exception {
        // 将 InputStream 读取为 byte[]，以便多次使用
        byte[] fileBytes = in.readAllBytes();
        
        // 1) 保存文件
        String subDir = "flow/upstream/" + projectId;
        String relPath = FileUtil.save(new ByteArrayInputStream(fileBytes), subDir, fileName);

        // 2) 解析 Excel
        List<Map<String, String>> rows = ExcelUtil.readSheetAsMap(new ByteArrayInputStream(fileBytes));
        if (rows.isEmpty()) throw new RuntimeException("Excel 文件为空");

        // 3) 校验必填列
        Set<String> headers = rows.get(0).keySet();
        List<String> missing = new ArrayList<>();
        for (String req : REQUIRED_HEADERS) {
            boolean found = false;
            for (String h : headers) if (req.equals(h) || req.equals(h.trim())) { found = true; break; }
            if (!found) missing.add(req);
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Excel 缺少必填列: " + String.join(",", missing));
        }

        // 4) 逐行严格校验（先于任何数据库操作），收集所有错误行
        List<String> errs = new ArrayList<>();
        List<Map<String, String>> validRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String rowMonth = row.get("月份") == null ? null : row.get("月份").trim();
            // 如果指定了月份，则只校验并处理指定月份的数据
            if (selectedMonths != null && !selectedMonths.isEmpty()) {
                String m = normalizeMonthStrict(rowMonth);
                if (m == null || !selectedMonths.contains(m)) {
                    continue;
                }
            }
            validRows.add(row);
            validateRow(i + 2, row, errs);
        }
        // 5) 任意一行校验不通过：整体失败，不写入任何数据
        if (!errs.isEmpty()) {
            StringBuilder sb = new StringBuilder("导入数据存在错误，已取消导入，共 " + errs.size() + " 处：\n");
            for (String e : errs) sb.append(e).append("\n");
            throw new RuntimeException(sb.toString().trim());
        }

        // 6) 抽取月份集合（此时所有有效行月份均为合法 yyyyMM）
        Set<String> months;
        if (selectedMonths != null && !selectedMonths.isEmpty()) {
            months = new TreeSet<>(selectedMonths);
        } else {
            months = new TreeSet<>();
            for (Map<String, String> row : validRows) {
                months.add(normalizeMonthStrict(row.get("月份")));
            }
        }
        if (months.isEmpty()) throw new RuntimeException("未解析到任何有效月份");

        // 7) 加载考核组（提前加载，不在事务中执行）
        Map<String, Long> assessGroupMap = new HashMap<>();
        List<AssessGroup> assessGroups = ruleDao.listAssessGroups(projectId);
        for (AssessGroup ag : assessGroups) {
            assessGroupMap.put(ag.getGroupName().trim(), ag.getId());
        }
        
        // 8) 准备要导入的数据（校验已通过，直接转换）
        List<UpstreamFlowRecord> validRecords = new ArrayList<>();
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
        
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String rowMonth = row.get("月份") == null ? null : row.get("月份").trim();
            if (selectedMonths != null && !selectedMonths.isEmpty()) {
                String m = normalizeMonthStrict(rowMonth);
                if (m == null || !selectedMonths.contains(m)) {
                    continue;
                }
            }
            
            UpstreamFlowRecord r = new UpstreamFlowRecord();
            r.setProjectId(projectId);
            r.setMonthYyyymm(normalizeMonthStrict(rowMonth));
            String bd = row.get("业务日期");
            if (bd != null && !bd.isEmpty()) {
                try { r.setBusinessDate(new Date(ymd.parse(bd.trim()).getTime())); } catch (Exception e) { /* 已在校验阶段拦截 */ }
            }
            r.setProductName(row.get("产品名称"));
            r.setSpec(row.get("规格"));
            r.setSellerName(row.get("销售方名称"));
            r.setSellerCity(row.get("销售方城市"));
            r.setCalcPrice(safeBd(row.get("核算价格")));
            r.setQuantity(safeBd(row.get("数量")));
            r.setCalcAmount(safeBd(row.get("核算金额")));
            r.setBuyerName(row.get("采购方名称"));
            String buyerCity = row.get("采购方城市");
            if (buyerCity != null && !buyerCity.trim().isEmpty()) {
                r.setBuyerCity(buyerCity.trim());
            }
            String customerLevel = row.get("客户等级");
            if (customerLevel != null && !customerLevel.trim().isEmpty()) {
                r.setCustomerLevel(customerLevel.trim());
            }
            r.setSaleQty(safeBd(row.get("销售数量")));
            r.setNoTaxAmount(safeBd(row.get("无税金额")));
            r.setTaxAmount(safeBd(row.get("含税金额")));
            r.setBidAmount(safeBd(row.get("中标金额")));
            
            // 处理考核组列（从内存Map获取）
            String assessGroupName = row.get("考核组");
            if (assessGroupName != null && !assessGroupName.trim().isEmpty()) {
                Long groupId = assessGroupMap.get(assessGroupName.trim());
                if (groupId != null) {
                    r.setAssessGroupId(groupId);
                }
            }
            
            // rawRow 存所有列
            r.setRawRow(new com.google.gson.Gson().toJson(row));
            validRecords.add(r);
        }

        // 9) 在事务中执行数据库操作，统一提交，失败整体回滚
        Connection conn = null;
        Long batchId = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);
            
            // 失效同月份旧数据（只失效非终版的）
            flowDao.invalidateExistingWithConn(conn, projectId, new ArrayList<>(months));

            // 写入批次
            UpstreamFlowBatch batch = new UpstreamFlowBatch();
            batch.setProjectId(projectId);
            batch.setBatchCode("B" + System.currentTimeMillis());
            batch.setFileName(fileName);
            batch.setFilePath(relPath);
            batch.setImportUser(userId);
            batch.setMonthSummary(String.join(",", months));
            batchId = flowDao.insertBatchWithConn(conn, batch);

            // 写入明细
            for (UpstreamFlowRecord r : validRecords) {
                r.setBatchId(batchId);
                flowDao.insertRecordWithConn(conn, r);
            }
            
            // 提交事务
            conn.commit();
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignore) {}
            }
            throw new RuntimeException("导入失败，已整体回滚，未写入任何数据: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            if (conn != null) {
                try { conn.close(); } catch (Exception ignore) {}
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("totalCount", validRecords.size());
        result.put("monthSummary", String.join(",", months));
        result.put("errorRows", errs);
        return result;
    }

    /**
     * 直接导入下游流向（不经过上游分解）。
     * 逻辑与 importUpstream 类似，但写入 flow_downstream_record 表，并携带 agreementId。
     *
     * @param projectId      项目ID
     * @param agreementId    下游协议ID
     * @param userId         导入用户ID
     * @param fileName       原始文件名
     * @param in             Excel 文件输入流
     * @param selectedMonths 仅导入这些月份（可为 null/空表示全部）
     * @return 导入结果（batchId, totalCount, monthSummary, errorRows）
     */
    public Map<String, Object> importDirectDownstream(long projectId, long agreementId, long userId, String fileName, InputStream in, List<String> selectedMonths) throws Exception {
        // 将 InputStream 读取为 byte[]，以便多次使用
        byte[] fileBytes = in.readAllBytes();

        // 1) 保存文件
        String subDir = "flow/downstream/" + projectId;
        String relPath = FileUtil.save(new ByteArrayInputStream(fileBytes), subDir, fileName);

        // 2) 解析 Excel
        List<Map<String, String>> rows = ExcelUtil.readSheetAsMap(new ByteArrayInputStream(fileBytes));
        if (rows.isEmpty()) throw new RuntimeException("Excel 文件为空");

        // 3) 校验必填列
        Set<String> headers = rows.get(0).keySet();
        List<String> missing = new ArrayList<>();
        for (String req : REQUIRED_HEADERS) {
            boolean found = false;
            for (String h : headers) if (req.equals(h) || req.equals(h.trim())) { found = true; break; }
            if (!found) missing.add(req);
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Excel 缺少必填列: " + String.join(",", missing));
        }

        // 4) 逐行严格校验（先于任何数据库操作），收集所有错误行
        List<String> errs = new ArrayList<>();
        List<Map<String, String>> validRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String rowMonth = row.get("月份") == null ? null : row.get("月份").trim();
            // 如果指定了月份，则只校验并处理指定月份的数据
            if (selectedMonths != null && !selectedMonths.isEmpty()) {
                String m = normalizeMonthStrict(rowMonth);
                if (m == null || !selectedMonths.contains(m)) {
                    continue;
                }
            }
            validRows.add(row);
            validateRow(i + 2, row, errs);
        }
        // 5) 任意一行校验不通过：整体失败，不写入任何数据
        if (!errs.isEmpty()) {
            StringBuilder sb = new StringBuilder("导入数据存在错误，已取消导入，共 " + errs.size() + " 处：\n");
            for (String e : errs) sb.append(e).append("\n");
            throw new RuntimeException(sb.toString().trim());
        }

        // 6) 抽取月份集合（此时所有有效行月份均为合法 yyyyMM）
        Set<String> months;
        if (selectedMonths != null && !selectedMonths.isEmpty()) {
            months = new TreeSet<>(selectedMonths);
        } else {
            months = new TreeSet<>();
            for (Map<String, String> row : validRows) {
                months.add(normalizeMonthStrict(row.get("月份")));
            }
        }
        if (months.isEmpty()) throw new RuntimeException("未解析到任何有效月份");

        // 7) 加载考核组（提前加载，不在事务中执行）
        Map<String, Long> assessGroupMap = new HashMap<>();
        List<AssessGroup> assessGroups = ruleDao.listAssessGroups(projectId);
        for (AssessGroup ag : assessGroups) {
            assessGroupMap.put(ag.getGroupName().trim(), ag.getId());
        }

        // 8) 准备要导入的数据（校验已通过，直接转换为 DownstreamFlowRecord）
        List<DownstreamFlowRecord> validRecords = new ArrayList<>();
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String rowMonth = row.get("月份") == null ? null : row.get("月份").trim();
            if (selectedMonths != null && !selectedMonths.isEmpty()) {
                String m = normalizeMonthStrict(rowMonth);
                if (m == null || !selectedMonths.contains(m)) {
                    continue;
                }
            }

            DownstreamFlowRecord r = new DownstreamFlowRecord();
            r.setProjectId(projectId);
            r.setAgreementId(agreementId);
            r.setMonthYyyymm(normalizeMonthStrict(rowMonth));
            String bd = row.get("业务日期");
            if (bd != null && !bd.isEmpty()) {
                try { r.setBusinessDate(new Date(ymd.parse(bd.trim()).getTime())); } catch (Exception e) { /* 已在校验阶段拦截 */ }
            }
            r.setProductName(row.get("产品名称"));
            r.setSpec(row.get("规格"));
            r.setSellerName(row.get("销售方名称"));
            r.setSellerCity(row.get("销售方城市"));
            r.setCalcPrice(safeBd(row.get("核算价格")));
            r.setQuantity(safeBd(row.get("数量")));
            r.setCalcAmount(safeBd(row.get("核算金额")));
            r.setBuyerName(row.get("采购方名称"));
            String buyerCity = row.get("采购方城市");
            if (buyerCity != null && !buyerCity.trim().isEmpty()) {
                r.setBuyerCity(buyerCity.trim());
            }
            String customerLevel = row.get("客户等级");
            if (customerLevel != null && !customerLevel.trim().isEmpty()) {
                r.setCustomerLevel(customerLevel.trim());
            }
            r.setSaleQty(safeBd(row.get("销售数量")));
            r.setNoTaxAmount(safeBd(row.get("无税金额")));
            r.setTaxAmount(safeBd(row.get("含税金额")));
            r.setBidAmount(safeBd(row.get("中标金额")));

            // 处理考核组列（从内存Map获取）
            String assessGroupName = row.get("考核组");
            if (assessGroupName != null && !assessGroupName.trim().isEmpty()) {
                Long groupId = assessGroupMap.get(assessGroupName.trim());
                if (groupId != null) {
                    r.setAssessGroupId(groupId);
                }
            }

            // rawRow 存所有列
            r.setRawRow(new com.google.gson.Gson().toJson(row));
            validRecords.add(r);
        }

        // 9) 在事务中执行数据库操作，统一提交，失败整体回滚
        //    直接导入下游流向：失效旧记录 + 创建下游批次 + 逐条插入 flow_downstream_record
        Connection conn = null;
        Long batchId = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 失效同项目+协议下同月份的旧有效下游记录（非终版），避免重复累计
            downstreamFlowDao.invalidateExistingWithConn(conn, projectId, agreementId, new ArrayList<>(months));

            // 写入下游批次
            batchId = downstreamFlowDao.insertDownstreamBatchWithConn(conn, projectId, agreementId,
                    "B" + System.currentTimeMillis(), fileName, relPath, userId,
                    String.join(",", months), null);

            // 写入明细
            for (DownstreamFlowRecord r : validRecords) {
                r.setBatchId(batchId);
                downstreamFlowDao.insertDirectRecordWithConn(conn, r);
            }

            // 提交事务
            conn.commit();
        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ignore) {}
            }
            throw new RuntimeException("导入失败，已整体回滚，未写入任何数据: " + e.getMessage(), e);
        } finally {
            // 关闭连接
            if (conn != null) {
                try { conn.close(); } catch (Exception ignore) {}
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("totalCount", validRecords.size());
        result.put("monthSummary", String.join(",", months));
        result.put("errorRows", errs);
        return result;
    }

    /**
     * 严格校验一行数据，错误信息追加到 errs。
     * @param rowNo Excel 行号（从 2 开始，含表头行）
     */
    private void validateRow(int rowNo, Map<String, String> row, List<String> errs) {
        String brief = briefRow(row);

        // 月份：必须 yyyyMM 且不能为空
        String month = row.get("月份");
        if (month == null || month.trim().isEmpty()) {
            errs.add("第" + rowNo + "行：月份不能为空；该行内容：" + brief);
        } else if (!month.trim().matches("\\d{6}")) {
            errs.add("第" + rowNo + "行：月份必须为yyyyMM格式（当前值：" + month.trim() + "）；该行内容：" + brief);
        } else if (!isValidMonth(month.trim())) {
            errs.add("第" + rowNo + "行：月份不是合法年月（当前值：" + month.trim() + "）；该行内容：" + brief);
        }

        // 业务日期：非空时必须是 yyyy-MM-dd 且在操作日期之后
        String bd = row.get("业务日期");
        if (bd != null && !bd.trim().isEmpty()) {
            if (!bd.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
                errs.add("第" + rowNo + "行：业务日期必须为yyyy-MM-dd格式（当前值：" + bd.trim() + "）；该行内容：" + brief);
            } else {
                Date d = parseDate(bd.trim());
                if (d == null) {
                    errs.add("第" + rowNo + "行：业务日期不是合法日期（当前值：" + bd.trim() + "）；该行内容：" + brief);
                } else if (!d.after(new Date(System.currentTimeMillis()))) {
                    errs.add("第" + rowNo + "行：业务日期必须在操作日期（今天）之后（当前值：" + bd.trim() + "）；该行内容：" + brief);
                }
            }
        }

        // 数值列：非空时必须是数字
        for (String col : NUMERIC_COLUMNS) {
            String v = row.get(col);
            if (v != null && !v.trim().isEmpty() && !isNumber(v.trim())) {
                errs.add("第" + rowNo + "行：" + col + " 必须为数字（当前值：" + v.trim() + "）；该行内容：" + brief);
            }
        }

        // 其它文本列：长度不能超出数据库定义长度
        for (Map.Entry<String, Integer> entry : TEXT_COLUMN_MAX_LEN.entrySet()) {
            String v = row.get(entry.getKey());
            if (v != null && v.length() > entry.getValue()) {
                errs.add("第" + rowNo + "行：" + entry.getKey() + " 长度(" + v.length() + ")超出数据库定义长度(" + entry.getValue() + ")（当前值：" + truncate(v, 20) + "）；该行内容：" + brief);
            }
        }
    }

    /** 该行的大致内容摘要 */
    private String briefRow(Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
        String[] keys = {"月份", "业务日期", "产品名称", "规格", "销售方名称", "销售方城市",
                "核算价格", "数量", "核算金额", "采购方名称", "采购方城市", "客户等级", "销售数量", "无税金额", "含税金额", "中标金额", "考核组"};
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.trim().isEmpty()) {
                if (sb.length() > 0) sb.append("；");
                sb.append(k).append("=").append(v.trim());
            }
        }
        String s = sb.toString();
        return s.length() > 150 ? s.substring(0, 150) + "…" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    /** 严格将月份规范为 yyyyMM，非法返回 null */
    private String normalizeMonthStrict(String m) {
        if (m == null) return null;
        m = m.trim();
        if (!m.matches("\\d{6}")) return null;
        if (!isValidMonth(m)) return null;
        return m;
    }

    private boolean isValidMonth(String yyyymm) {
        try {
            int year = Integer.parseInt(yyyymm.substring(0, 4));
            int month = Integer.parseInt(yyyymm.substring(4, 6));
            return year >= 1900 && year <= 2100 && month >= 1 && month <= 12;
        } catch (Exception e) {
            return false;
        }
    }

    private Date parseDate(String s) {
        try {
            return Date.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.matches("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");
    }

    private BigDecimal safeBd(String s) {
        if (s == null || s.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}

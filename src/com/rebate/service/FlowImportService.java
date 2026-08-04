package com.rebate.service;

import com.rebate.dao.UpstreamFlowDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.model.AssessGroup;
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
 * <p>处理：Excel 解析 → 校验必填列 → 同月份失效旧版本 → 写入新批次与明细。</p>
 */
public class FlowImportService {

    private final UpstreamFlowDao flowDao = new UpstreamFlowDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();

    private static final String[] REQUIRED_HEADERS = {
            "月份", "业务日期", "产品名称", "规格", "销售方名称", "销售方城市",
            "核算价格", "数量", "核算金额", "采购方名称"
    };

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

        // 4) 抽取月份集合（如果指定了月份，则只处理指定月份）
        Set<String> months;
        if (selectedMonths != null && !selectedMonths.isEmpty()) {
            months = new TreeSet<>(selectedMonths);
        } else {
            months = new TreeSet<>();
            for (Map<String, String> row : rows) {
                String m = normMonth(row.get("月份"));
                if (m != null) months.add(m);
            }
        }
        if (months.isEmpty()) throw new RuntimeException("未解析到任何有效月份");

        // 5) 加载考核组（提前加载，不在事务中执行）
        Map<String, Long> assessGroupMap = new HashMap<>();
        List<AssessGroup> assessGroups = ruleDao.listAssessGroups(projectId);
        for (AssessGroup ag : assessGroups) {
            assessGroupMap.put(ag.getGroupName().trim(), ag.getId());
        }
        
        // 6) 准备要导入的数据（提前准备，不在事务中执行）
        List<UpstreamFlowRecord> validRecords = new ArrayList<>();
        List<String> errs = new ArrayList<>();
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
        
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                String rowMonth = normMonth(row.get("月份"));
                // 如果指定了月份，则只处理指定月份的数据
                if (selectedMonths != null && !selectedMonths.isEmpty()) {
                    if (rowMonth == null || !selectedMonths.contains(rowMonth)) {
                        continue;
                    }
                }
                
                UpstreamFlowRecord r = new UpstreamFlowRecord();
                r.setProjectId(projectId);
                r.setMonthYyyymm(rowMonth);
                String bd = row.get("业务日期");
                if (bd != null && !bd.isEmpty()) {
                    try { r.setBusinessDate(new Date(ymd.parse(bd).getTime())); } catch (Exception e) { /* skip */ }
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
            } catch (Exception ex) {
                errs.add("第" + (i + 2) + "行: " + ex.getMessage());
            }
        }

        // 7) 在事务中执行数据库操作
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
            throw new RuntimeException("导入失败，已回滚: " + e.getMessage(), e);
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

    private String normMonth(String m) {
        if (m == null) return null;
        m = m.trim().replaceAll("[^0-9]", "");
        if (m.length() == 6) return m;
        if (m.length() == 8) return m.substring(0, 6);
        if (m.length() == 4) return m + "01";
        return null;
    }

    private BigDecimal safeBd(String s) {
        if (s == null || s.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
}

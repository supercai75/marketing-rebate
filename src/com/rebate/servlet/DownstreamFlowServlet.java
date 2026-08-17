package com.rebate.servlet;

import com.rebate.dao.DownstreamFlowDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.model.DownstreamFlowRecord;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

import java.math.BigDecimal;

/**
 * 下游流向管理
 */
public class DownstreamFlowServlet extends BaseServlet {

    private final DownstreamFlowDao dao = new DownstreamFlowDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "listRecords";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "listRecords": doListRecords(req, resp, p); break;
            case "listAssessGroups": doListAssessGroups(req, resp); break;
            case "updateRecordAssessGroup": doUpdateRecordAssessGroup(req, resp, p); break;
            case "listSplitUpstreamIds": doListSplitUpstreamIds(req, resp, p); break;
            case "decompose": doDecompose(req, resp, p, u); break;
            case "removeRecord": doRemoveRecord(req, resp, p); break;
            case "removeAllValid": doRemoveAllValid(req, resp, p); break;
            case "agreementOverview": doAgreementOverview(req, resp, p); break;
            case "exportCurrent": doExportCurrent(req, resp, p); break;
            case "exportInvalid": doExportInvalid(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "listRecords":
            case "listAssessGroups":
            case "listSplitUpstreamIds":
            case "agreementOverview":
            case "exportCurrent":
            case "exportInvalid":
                return u.hasPerm("flow:view");
            case "updateRecordAssessGroup":
                return u.hasPerm("flow:view");
            case "decompose":
            case "removeRecord":
            case "removeAllValid":
                return u.hasPerm("flow:split");
            default:
                return true;
        }
    }

    private void doListRecords(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long projectId = WebUtil.getLong(p, "projectId", 0L);
        String month = WebUtil.getSafeParam(p, "month");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String buyerCity = WebUtil.getSafeParam(p, "buyerCity");
        String customerLevel = WebUtil.getSafeParam(p, "customerLevel");
        String isValidStr = WebUtil.getSafeParam(p, "isValid");
        Integer isValid = null;
        if (isValidStr != null && !isValidStr.isEmpty()) {
            try {
                isValid = Integer.parseInt(isValidStr);
            } catch (NumberFormatException e) {
                try {
                    isValid = (int) Math.round(Double.parseDouble(isValidStr));
                } catch (NumberFormatException e2) {
                    // ignore
                }
            }
        }
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        if (agreementId == 0) agreementId = null;
        Integer page = WebUtil.getInt(p, "page", 1);
        Integer pageSize = WebUtil.getInt(p, "pageSize", 20);
        
        List<DownstreamFlowRecord> pageRecords;
        long total;
        
        total = dao.countRecordsWithUpstream(projectId, month, buyerName, buyerCity, customerLevel, isValid, agreementId);
        pageRecords = dao.listRecordsWithUpstreamPage(projectId, month, buyerName, buyerCity, customerLevel, isValid, agreementId, page, pageSize);
        
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageRecords);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        ResponseUtil.ok(resp, result);
    }
    
    private void doDecompose(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        Long projectId = WebUtil.getLong(p, "projectId", 0L);
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        String recordIdsStr = WebUtil.getSafeParam(p, "recordIds");
        
        if (projectId <= 0 || agreementId <= 0 || recordIdsStr == null || recordIdsStr.isEmpty()) {
            ResponseUtil.fail(resp, "参数错误");
            return;
        }
        
        List<Long> recordIds = new ArrayList<>();
        String[] idStrs = recordIdsStr.split(",");
        for (String idStr : idStrs) {
            idStr = idStr.trim();
            if (!idStr.isEmpty()) {
                recordIds.add(Long.parseLong(idStr));
            }
        }
        
        int count = dao.decompose(projectId, agreementId, recordIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        ResponseUtil.ok(resp, result);
    }
    
    private void doAgreementOverview(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        if (agreementId <= 0) {
            ResponseUtil.ok(resp, Collections.emptyMap());
            return;
        }
        
        // 获取下游协议
        com.rebate.dao.DownstreamAgreementDao agreementDao = new com.rebate.dao.DownstreamAgreementDao();
        com.rebate.model.DownstreamAgreement agreement = agreementDao.findById(agreementId);
        
        if (agreement == null) {
            ResponseUtil.ok(resp, Collections.emptyMap());
            return;
        }
        
        // 获取协议的计算依据
        String calcBasis = agreement.getCalcBasis();
        boolean useQuantity = "QTY".equalsIgnoreCase(calcBasis);
        
        // 获取记录并聚合
        List<DownstreamFlowRecord> allRecords = dao.listRecordsWithUpstream(agreement.getProjectId(), null, null, null, null, 1, agreementId);
        
        java.math.BigDecimal totalActual = java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage1Actual = java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage2Actual = java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage3Actual = java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage4Actual = java.math.BigDecimal.ZERO;
        
        for (DownstreamFlowRecord r : allRecords) {
            // 根据计算依据选择使用数量或金额
            java.math.BigDecimal value = useQuantity 
                ? (r.getQuantity() != null ? r.getQuantity() : java.math.BigDecimal.ZERO)
                : (r.getCalcAmount() != null ? r.getCalcAmount() : java.math.BigDecimal.ZERO);
            totalActual = totalActual.add(value);
            
            // 按阶段聚合（根据月份判断）
            String month = r.getMonthYyyymm();
            if (month != null && month.length() == 6) {
                int m = Integer.parseInt(month.substring(4, 6));
                if (m >= 1 && m <= 3) {
                    stage1Actual = stage1Actual.add(value);
                } else if (m >= 4 && m <= 6) {
                    stage2Actual = stage2Actual.add(value);
                } else if (m >= 7 && m <= 9) {
                    stage3Actual = stage3Actual.add(value);
                } else {
                    stage4Actual = stage4Actual.add(value);
                }
            }
        }
        
        // 计算达成率
        java.math.BigDecimal totalTarget = agreement.getTargetScale() != null ? agreement.getTargetScale() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage1Target = agreement.getStage1Target() != null ? agreement.getStage1Target() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage2Target = agreement.getStage2Target() != null ? agreement.getStage2Target() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage3Target = agreement.getStage3Target() != null ? agreement.getStage3Target() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal stage4Target = agreement.getStage4Target() != null ? agreement.getStage4Target() : java.math.BigDecimal.ZERO;
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agreement", agreement);
        result.put("totalActual", totalActual);
        result.put("totalTarget", totalTarget);
        result.put("totalRate", calculateRate(totalActual, totalTarget));
        result.put("stage1Actual", stage1Actual);
        result.put("stage2Actual", stage2Actual);
        result.put("stage3Actual", stage3Actual);
        result.put("stage4Actual", stage4Actual);
        result.put("stage1Target", stage1Target);
        result.put("stage2Target", stage2Target);
        result.put("stage3Target", stage3Target);
        result.put("stage4Target", stage4Target);
        result.put("stage1Rate", calculateRate(stage1Actual, stage1Target));
        result.put("stage2Rate", calculateRate(stage2Actual, stage2Target));
        result.put("stage3Rate", calculateRate(stage3Actual, stage3Target));
        result.put("stage4Rate", calculateRate(stage4Actual, stage4Target));
        
        ResponseUtil.ok(resp, result);
    }
    
    private String calculateRate(java.math.BigDecimal actual, java.math.BigDecimal target) {
        if (target == null || target.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "0.00";
        }
        java.math.BigDecimal rate = actual.multiply(new java.math.BigDecimal("100")).divide(target, 2, java.math.RoundingMode.HALF_UP);
        return rate.toString();
    }

    private void doListAssessGroups(HttpServletRequest req, HttpServletResponse resp) {
        long projectId = 0;
        try { projectId = Long.parseLong(req.getParameter("projectId")); } catch (Exception ignore) {}
        ResponseUtil.ok(resp, ruleDao.listAssessGroups(projectId));
    }

    private void doUpdateRecordAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long recordId = WebUtil.getLong(p, "id", 0L);
        String groupIdStr = WebUtil.getSafeParam(p, "assessGroupId");
        Long assessGroupId = (groupIdStr == null || groupIdStr.isEmpty() || "0".equals(groupIdStr)) ? null : Long.parseLong(groupIdStr);
        dao.updateAssessGroup(recordId, assessGroupId);
        ResponseUtil.ok(resp);
    }

    private void doListSplitUpstreamIds(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long projectId = WebUtil.getLong(p, "projectId", 0L);
        ResponseUtil.ok(resp, dao.listSplitUpstreamIds(projectId));
    }

    /** 剔除单条下游流向记录 */
    private void doRemoveRecord(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long recordId = WebUtil.getLong(p, "id", 0L);
        if (recordId <= 0) { ResponseUtil.fail(resp, "id 必填"); return; }
        int n = dao.deleteRecord(recordId);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("affected", n));
    }

    /** 全部剔除：清空当前生效流向（按项目+协议） */
    private void doRemoveAllValid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0L);
        if (projectId <= 0) { ResponseUtil.fail(resp, "projectId 必填"); return; }
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        if (agreementId == 0) agreementId = null;
        int n = dao.deleteAllValidRecords(projectId, agreementId);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("affected", n));
    }

    private void doExportCurrent(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        Long projectId = WebUtil.getLong(p, "projectId", 0L);
        String month = WebUtil.getSafeParam(p, "month");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String buyerCity = WebUtil.getSafeParam(p, "buyerCity");
        String customerLevel = WebUtil.getSafeParam(p, "customerLevel");
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        if (agreementId == 0) agreementId = null;

        List<DownstreamFlowRecord> records = dao.listRecordsWithUpstream(projectId, month, buyerName, buyerCity, customerLevel, 1, agreementId);

        List<String> headers = Arrays.asList("月份", "业务日期", "产品名称", "规格", "销售方", "销售城市",
                "核算价格", "数量", "销售数量", "核算金额", "中标价金额", "无税金额",
                "采购方", "采购方城市", "客户等级", "考核组");
        List<List<String>> rows = new ArrayList<>();
        for (DownstreamFlowRecord r : records) {
            List<String> row = new ArrayList<>();
            row.add(r.getMonthYyyymm() != null ? r.getMonthYyyymm() : "");
            row.add(r.getBusinessDate() != null ? r.getBusinessDate().toString() : "");
            row.add(r.getProductName() != null ? r.getProductName() : "");
            row.add(r.getSpec() != null ? r.getSpec() : "");
            row.add(r.getSellerName() != null ? r.getSellerName() : "");
            row.add(r.getSellerCity() != null ? r.getSellerCity() : "");
            row.add(r.getCalcPrice() != null ? r.getCalcPrice().toString() : "");
            row.add(r.getQuantity() != null ? r.getQuantity().toString() : "");
            row.add(r.getSaleQty() != null ? r.getSaleQty().toString() : "");
            row.add(r.getCalcAmount() != null ? r.getCalcAmount().toString() : "");
            row.add(r.getBidAmount() != null ? r.getBidAmount().toString() : "");
            row.add(r.getNoTaxAmount() != null ? r.getNoTaxAmount().toString() : "");
            row.add(r.getBuyerName() != null ? r.getBuyerName() : "");
            row.add(r.getBuyerCity() != null ? r.getBuyerCity() : "");
            row.add(r.getCustomerLevel() != null ? r.getCustomerLevel() : "");
            row.add(r.getAssessGroupName() != null ? r.getAssessGroupName() : "");
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "下游流向当前生效_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportInvalid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        Long projectId = WebUtil.getLong(p, "projectId", 0L);
        String month = WebUtil.getSafeParam(p, "month");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String buyerCity = WebUtil.getSafeParam(p, "buyerCity");
        String customerLevel = WebUtil.getSafeParam(p, "customerLevel");
        Long agreementId = WebUtil.getLong(p, "agreementId", 0L);
        if (agreementId == 0) agreementId = null;

        List<DownstreamFlowRecord> records = dao.listRecordsWithUpstream(projectId, month, buyerName, buyerCity, customerLevel, 0, agreementId);

        List<String> headers = Arrays.asList("月份", "业务日期", "产品名称", "规格", "销售方", "销售城市",
                "核算价格", "数量", "销售数量", "核算金额", "中标价金额", "无税金额",
                "采购方", "采购方城市", "客户等级", "考核组");
        List<List<String>> rows = new ArrayList<>();
        for (DownstreamFlowRecord r : records) {
            List<String> row = new ArrayList<>();
            row.add(r.getMonthYyyymm() != null ? r.getMonthYyyymm() : "");
            row.add(r.getBusinessDate() != null ? r.getBusinessDate().toString() : "");
            row.add(r.getProductName() != null ? r.getProductName() : "");
            row.add(r.getSpec() != null ? r.getSpec() : "");
            row.add(r.getSellerName() != null ? r.getSellerName() : "");
            row.add(r.getSellerCity() != null ? r.getSellerCity() : "");
            row.add(r.getCalcPrice() != null ? r.getCalcPrice().toString() : "");
            row.add(r.getQuantity() != null ? r.getQuantity().toString() : "");
            row.add(r.getSaleQty() != null ? r.getSaleQty().toString() : "");
            row.add(r.getCalcAmount() != null ? r.getCalcAmount().toString() : "");
            row.add(r.getBidAmount() != null ? r.getBidAmount().toString() : "");
            row.add(r.getNoTaxAmount() != null ? r.getNoTaxAmount().toString() : "");
            row.add(r.getBuyerName() != null ? r.getBuyerName() : "");
            row.add(r.getBuyerCity() != null ? r.getBuyerCity() : "");
            row.add(r.getCustomerLevel() != null ? r.getCustomerLevel() : "");
            row.add(r.getAssessGroupName() != null ? r.getAssessGroupName() : "");
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "下游流向已作废_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }
}

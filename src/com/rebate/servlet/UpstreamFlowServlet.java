package com.rebate.servlet;

import com.rebate.dao.UpstreamFlowDao;
import com.rebate.dao.RebateRuleDao;
import com.rebate.model.UpstreamFlowBatch;
import com.rebate.model.UpstreamFlowRecord;
import com.rebate.service.FlowImportService;
import com.rebate.service.ProjectScaleService;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import com.rebate.dao.BaseDao;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.poi.ss.usermodel.Workbook;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上游流向管理
 */
@MultipartConfig
public class UpstreamFlowServlet extends BaseServlet {

    private final UpstreamFlowDao dao = new UpstreamFlowDao();
    private final RebateRuleDao ruleDao = new RebateRuleDao();
    private final FlowImportService importService = new FlowImportService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // 检查是否是 multipart 请求
            String contentType = req.getContentType();
            Map<String, Object> params;
            if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
                // multipart 请求：使用 request.getParameter() 获取参数
                params = new HashMap<>();
                req.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
            } else {
                // 普通 JSON 请求
                params = com.rebate.util.JsonUtil.readRequestMap(req);
            }
            doAction(req, resp, params);
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.error(resp, "服务器异常: " + e.getMessage());
        }
    }

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "listBatches";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "listBatches": doListBatches(req, resp, p); break;
            case "listRecords": doListRecords(req, resp, p); break;
            case "countRecords": doCountRecords(req, resp, p); break;
            case "listAllIds": doListAllIds(req, resp, p); break;
            case "listValidIds": doListValidIds(req, resp, p); break;
            case "listValidIdsWithFilters": doListValidIdsWithFilters(req, resp, p); break;
            case "listCanSplit": doListCanSplit(req, resp, p); break;
            case "listCanSplitIds": doListCanSplitIds(req, resp, p); break;
            case "importExcel": doImport(req, resp, u); break;
            case "monthSummary": doMonthSummary(req, resp, p); break;
            case "setFinal": doSetFinal(req, resp, p, u); break;
            case "cancelFinal": doCancelFinal(req, resp, p); break;
            case "listFinalMonths": doListFinal(req, resp, p); break;
            case "listAssessGroups": doListAssessGroups(req, resp); break;
            case "updateRecordAssessGroup": doUpdateRecordAssessGroup(req, resp, p); break;
            case "checkExistingMonths": doCheckExistingMonths(req, resp, p); break;
            case "exportRecords": doExportRecords(req, resp, p); break;
            case "exportMonthSummary": doExportMonthSummary(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private void doListBatches(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, dao.listBatches(pid));
    }

    private void doListRecords(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        String productName = WebUtil.getSafeParam(p, "productName");
        String spec = WebUtil.getSafeParam(p, "spec");
        String sellerName = WebUtil.getSafeParam(p, "sellerName");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String isValidStr = WebUtil.getSafeParam(p, "isValid");
        Integer isValid = null;
        if (isValidStr != null && !isValidStr.isEmpty()) {
            try {
                isValid = Integer.parseInt(isValidStr);
            } catch (NumberFormatException e) {
                // 尝试作为浮点数处理
                try {
                    isValid = (int) Math.round(Double.parseDouble(isValidStr));
                } catch (NumberFormatException e2) {
                    // 忽略无效值
                }
            }
        }
        Integer page = WebUtil.getInt(p, "page", 1);
        Integer pageSize = WebUtil.getInt(p, "pageSize", 20);
        Long batchId = WebUtil.getLong(p, "batchId", 0);
        
        // 获取分页后的记录
        List<com.rebate.model.UpstreamFlowRecord> pageRecords;
        long total;
        if (batchId > 0) {
            // 按批次查询时，仍然先获取所有记录再分页
            List<com.rebate.model.UpstreamFlowRecord> allRecords = dao.listRecordsByBatch(batchId);
            // 如果有其他过滤条件，再手动过滤
            List<com.rebate.model.UpstreamFlowRecord> filtered = new ArrayList<>();
            for (com.rebate.model.UpstreamFlowRecord r : allRecords) {
                if (productName != null && !productName.isEmpty() && 
                    (r.getProductName() == null || !r.getProductName().contains(productName))) continue;
                if (spec != null && !spec.isEmpty() && 
                    (r.getSpec() == null || !r.getSpec().contains(spec))) continue;
                if (sellerName != null && !sellerName.isEmpty() && 
                    (r.getSellerName() == null || !r.getSellerName().contains(sellerName))) continue;
                if (buyerName != null && !buyerName.isEmpty() && 
                    (r.getBuyerName() == null || !r.getBuyerName().contains(buyerName))) continue;
                filtered.add(r);
            }
            total = filtered.size();
            int totalPages = (int) ((total + pageSize - 1) / pageSize);
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, (int) total);
            pageRecords = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();
        } else {
            // 普通查询使用数据库分页
            if (productName != null && !productName.isEmpty() || spec != null && !spec.isEmpty() 
                || sellerName != null && !sellerName.isEmpty() || buyerName != null && !buyerName.isEmpty()) {
                // 如果有产品名称等过滤条件，仍然先获取所有记录再过滤
                List<com.rebate.model.UpstreamFlowRecord> allRecords = dao.listRecords(pid, month, isValid);
                List<com.rebate.model.UpstreamFlowRecord> filtered = new ArrayList<>();
                for (com.rebate.model.UpstreamFlowRecord r : allRecords) {
                    if (productName != null && !productName.isEmpty() && 
                        (r.getProductName() == null || !r.getProductName().contains(productName))) continue;
                    if (spec != null && !spec.isEmpty() && 
                        (r.getSpec() == null || !r.getSpec().contains(spec))) continue;
                    if (sellerName != null && !sellerName.isEmpty() && 
                        (r.getSellerName() == null || !r.getSellerName().contains(sellerName))) continue;
                    if (buyerName != null && !buyerName.isEmpty() && 
                        (r.getBuyerName() == null || !r.getBuyerName().contains(buyerName))) continue;
                    filtered.add(r);
                }
                total = filtered.size();
                int totalPages = (int) ((total + pageSize - 1) / pageSize);
                int fromIndex = (page - 1) * pageSize;
                int toIndex = Math.min(fromIndex + pageSize, (int) total);
                pageRecords = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();
            } else {
                // 没有复杂过滤条件时使用数据库分页
                total = dao.countRecords(pid, month, isValid);
                pageRecords = dao.listRecordsPage(pid, month, isValid, page, pageSize);
            }
        }
        
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageRecords);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        ResponseUtil.ok(resp, result);
    }

    private void doListValidIds(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, dao.listValidRecordIds(pid));
    }

    private void doListValidIdsWithFilters(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        String productName = WebUtil.getSafeParam(p, "productName");
        String spec = WebUtil.getSafeParam(p, "spec");
        String sellerName = WebUtil.getSafeParam(p, "sellerName");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        ResponseUtil.ok(resp, dao.listValidRecordIdsWithFilters(pid, month, productName, spec, sellerName, buyerName));
    }
    
    private void doListCanSplit(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        String productName = WebUtil.getSafeParam(p, "productName");
        String spec = WebUtil.getSafeParam(p, "spec");
        String sellerName = WebUtil.getSafeParam(p, "sellerName");
        String sellerCity = WebUtil.getSafeParam(p, "sellerCity");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String buyerCity = WebUtil.getSafeParam(p, "buyerCity");
        String customerLevel = WebUtil.getSafeParam(p, "customerLevel");
        java.util.List<String> productNameIn = WebUtil.getStringList(p, "productNameIn");
        java.util.List<String> sellerNameIn = WebUtil.getStringList(p, "sellerNameIn");
        java.util.List<String> buyerNameIn = WebUtil.getStringList(p, "buyerNameIn");
        java.util.List<String> buyerCityIn = WebUtil.getStringList(p, "buyerCityIn");
        Integer page = WebUtil.getInt(p, "page", 1);
        Integer pageSize = WebUtil.getInt(p, "pageSize", 20);

        List<com.rebate.model.UpstreamFlowRecord> records = dao.listCanSplitRecords(pid, month, productName, productNameIn, spec, sellerName, sellerNameIn, sellerCity, buyerName, buyerNameIn, buyerCity, buyerCityIn, customerLevel, page, pageSize);
        int total = dao.countCanSplitRecords(pid, month, productName, productNameIn, spec, sellerName, sellerNameIn, sellerCity, buyerName, buyerNameIn, buyerCity, buyerCityIn, customerLevel);

        Map<String, Object> result = new HashMap<>();
        result.put("list", records);
        result.put("total", total);
        result.put("totalPages", (total + pageSize - 1) / pageSize);
        result.put("page", page);
        result.put("pageSize", pageSize);

        ResponseUtil.ok(resp, result);
    }

    private void doListCanSplitIds(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        String productName = WebUtil.getSafeParam(p, "productName");
        String spec = WebUtil.getSafeParam(p, "spec");
        String sellerName = WebUtil.getSafeParam(p, "sellerName");
        String sellerCity = WebUtil.getSafeParam(p, "sellerCity");
        String buyerName = WebUtil.getSafeParam(p, "buyerName");
        String buyerCity = WebUtil.getSafeParam(p, "buyerCity");
        String customerLevel = WebUtil.getSafeParam(p, "customerLevel");
        java.util.List<String> productNameIn = WebUtil.getStringList(p, "productNameIn");
        java.util.List<String> sellerNameIn = WebUtil.getStringList(p, "sellerNameIn");
        java.util.List<String> buyerNameIn = WebUtil.getStringList(p, "buyerNameIn");
        java.util.List<String> buyerCityIn = WebUtil.getStringList(p, "buyerCityIn");
        ResponseUtil.ok(resp, dao.listCanSplitIds(pid, month, productName, productNameIn, spec, sellerName, sellerNameIn, sellerCity, buyerName, buyerNameIn, buyerCity, buyerCityIn, customerLevel));
    }

    private void doImport(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        long pid = 0;
        try { pid = Long.parseLong(req.getParameter("projectId")); } catch (Exception ignore) {}
        if (pid <= 0) { ResponseUtil.fail(resp, "projectId 必填"); return; }
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择 Excel 文件"); return; }
        
        // 获取选中的月份列表
        String monthsStr = req.getParameter("selectedMonths");
        List<String> selectedMonths = null;
        if (monthsStr != null && !monthsStr.isEmpty()) {
            selectedMonths = Arrays.asList(monthsStr.split(","));
        }
        
        Map<String, Object> r = importService.importUpstream(pid, u.getId(), file.getSubmittedFileName(), file.getInputStream(), selectedMonths);
        ResponseUtil.ok(resp, r);
    }

    private void doMonthSummary(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String basis = WebUtil.getSafeParam(p, "basis");
        if (basis == null) basis = "AMT";
        ResponseUtil.ok(resp, dao.sumByMonth(pid, basis));
    }

    private void doSetFinal(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        if (pid <= 0 || month == null || month.isEmpty()) { ResponseUtil.fail(resp, "参数错误"); return; }
        dao.setFinalMonth(pid, month, u.getId());
        dao.markFinalInRecords(pid, month);
        ResponseUtil.ok(resp);
    }

    private void doCancelFinal(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        dao.cancelFinalMonth(pid, month);
        dao.unmarkFinalInRecords(pid, month);
        ResponseUtil.ok(resp);
    }

    private void doListFinal(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, dao.listFinalMonths(pid));
    }

    private void doCheckExistingMonths(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<String> existingMonths = dao.listExistingMonths(pid);
        List<String> finalMonths = dao.listFinalMonths(pid);
        
        Map<String, Object> result = new HashMap<>();
        result.put("existingMonths", existingMonths);
        result.put("finalMonths", finalMonths);
        ResponseUtil.ok(resp, result);
    }

    private void doListAssessGroups(HttpServletRequest req, HttpServletResponse resp) {
        long projectId = 0;
        try { projectId = Long.parseLong(req.getParameter("projectId")); } catch (Exception ignore) {}
        ResponseUtil.ok(resp, ruleDao.listAssessGroups(projectId));
    }

    private void doUpdateRecordAssessGroup(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long recordId = WebUtil.getLong(p, "id", 0);
        String groupIdStr = WebUtil.getSafeParam(p, "assessGroupId");
        Long assessGroupId = (groupIdStr == null || groupIdStr.isEmpty() || "0".equals(groupIdStr)) ? null : Long.parseLong(groupIdStr);
        BaseDao.update("UPDATE flow_upstream_record SET assess_group_id = ? WHERE id = ?", assessGroupId, recordId);
        ResponseUtil.ok(resp);
    }
    
    private void doCountRecords(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0L);
        String sql = "SELECT COUNT(*) FROM flow_upstream_record WHERE project_id = ? AND is_valid = 1";
        Long count = BaseDao.queryOne(sql, rs -> rs.getLong(1), projectId);
        ResponseUtil.ok(resp, count);
    }
    
    private void doListAllIds(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0L);
        String sql = "SELECT id FROM flow_upstream_record WHERE project_id = ? AND is_valid = 1 ORDER BY id";
        List<Long> ids = BaseDao.query(sql, rs -> rs.getLong("id"), projectId);
        ResponseUtil.ok(resp, ids);
    }

    private void doExportRecords(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        if (pid <= 0) { ResponseUtil.fail(resp, "projectId 必填"); return; }
        String month = WebUtil.getSafeParam(p, "month");
        String showInvalid = WebUtil.getSafeParam(p, "showInvalid");
        Integer isValid = ("true".equals(showInvalid) || "1".equals(showInvalid)) ? null : 1;

        List<UpstreamFlowRecord> records = dao.listRecords(pid, month, isValid);

        List<String> headers = Arrays.asList("月份", "业务日期", "产品名称", "规格", "销售方", "销售城市",
                "核算价格", "数量", "销售数量", "核算金额", "中标金额", "无税金额", "含税金额",
                "采购方", "采购方城市", "考核组", "状态");
        List<List<String>> rows = new ArrayList<>();
        for (UpstreamFlowRecord r : records) {
            String state = r.getIsFinal() == 1 ? "终版" : (r.getIsValid() == 1 ? "有效" : "失效");
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
            row.add(r.getTaxAmount() != null ? r.getTaxAmount().toString() : "");
            row.add(r.getBuyerName() != null ? r.getBuyerName() : "");
            row.add(r.getBuyerCity() != null ? r.getBuyerCity() : "");
            row.add(r.getAssessGroupName() != null ? r.getAssessGroupName() : "");
            row.add(state);
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "上游流向明细_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportMonthSummary(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        if (pid <= 0) { ResponseUtil.fail(resp, "projectId 必填"); return; }
        String basis = WebUtil.getSafeParam(p, "basis");
        if (basis == null) basis = "AMT";

        List<Map<String, Object>> summary = dao.sumByMonth(pid, basis);

        List<String> headers = Arrays.asList("月份", "数量", "金额", "记录数", "状态");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> m : summary) {
            String status = "1".equals(String.valueOf(m.get("isFinal"))) ? "终版" : "可修改";
            List<String> row = new ArrayList<>();
            row.add(m.get("month") != null ? m.get("month").toString() : "");
            row.add(m.get("qtyCount") != null ? m.get("qtyCount").toString() : "");
            row.add(m.get("scale") != null ? m.get("scale").toString() : "");
            row.add(m.get("count") != null ? m.get("count").toString() : "");
            row.add(status);
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "上游流向月份汇总_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "listBatches":
            case "listRecords":
            case "countRecords":
            case "listAllIds":
            case "listValidIds":
            case "listValidIdsWithFilters":
            case "listCanSplit":
            case "listCanSplitIds":
            case "monthSummary":
            case "listFinalMonths":
            case "listAssessGroups":
            case "checkExistingMonths":
            case "exportRecords":
            case "exportMonthSummary":
                return u.hasPerm("flow:view");
            case "importExcel":
                return u.hasPerm("flow:import");
            case "setFinal":
            case "cancelFinal":
                return u.hasPerm("flow:final");
            case "updateRecordAssessGroup":
                return u.hasPerm("flow:view");
            default:
                return true;
        }
    }
}

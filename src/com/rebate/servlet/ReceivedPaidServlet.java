package com.rebate.servlet;

import com.rebate.dao.BaseDao;
import com.rebate.dao.ProjectDao;
import com.rebate.dao.ReceivedPaidDao;
import com.rebate.model.Paid;
import com.rebate.model.Project;
import com.rebate.model.Received;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 实收/实付
 */
public class ReceivedPaidServlet extends BaseServlet {

    private final ReceivedPaidDao dao = new ReceivedPaidDao();
    private final ProjectDao projectDao = new ProjectDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "listReceived";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "listReceived": doListRecv(req, resp, p); break;
            case "saveReceived": doSaveRecv(req, resp, p); break;
            case "confirmReceived": doConfirmRecv(req, resp, p, u); break;
            case "deleteReceived": doDeleteRecv(req, resp, p); break;
            case "listPaid": doListPaid(req, resp, p); break;
            case "savePaid": doSavePaid(req, resp, p); break;
            case "confirmPaid": doConfirmPaid(req, resp, p, u); break;
            case "deletePaid": doDeletePaid(req, resp, p); break;
            case "exportReceived": doExportReceived(req, resp, p); break;
            case "exportPaid": doExportPaid(req, resp, p); break;
            case "listBpmReceived": doListBpmReceived(req, resp, p); break;
            case "importFromBpm": doImportFromBpm(req, resp, p, u); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "listReceived":
            case "exportReceived":
            case "listBpmReceived":
                return u.hasPerm("received:view");
            case "saveReceived":
            case "deleteReceived":
            case "importFromBpm":
                return u.hasPerm("received:edit");
            case "confirmReceived":
                return u.hasPerm("received:confirm");
            case "listPaid":
            case "exportPaid":
                return u.hasPerm("paid:view");
            case "savePaid":
            case "deletePaid":
                return u.hasPerm("paid:edit");
            case "confirmPaid":
                return u.hasPerm("paid:confirm");
            default:
                return true;
        }
    }

    private void doListRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        ResponseUtil.ok(resp, dao.listReceivedByProject(
                WebUtil.getLong(p, "projectId", 0),
                WebUtil.getSafeParam(p, "stage"),
                WebUtil.getSafeParam(p, "rebateType")));
    }

    private void doSaveRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Received r = new Received();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setRebateType(WebUtil.getSafeParam(p, "rebateType"));
        r.setApplicant(WebUtil.getSafeParam(p, "applicant"));
        r.setApplyDept(WebUtil.getSafeParam(p, "applyDept"));
        String ad = WebUtil.getSafeParam(p, "applyDate");
        if (ad != null && !ad.isEmpty()) r.setApplyDate(Date.valueOf(ad));
        r.setFinanceCode(WebUtil.getSafeParam(p, "financeCode"));
        r.setRebateAmount(toBd(p.get("rebateAmount")));
        r.setTaxRate(toBd(p.get("taxRate")));
        r.setTotalPriceTax(toBd(p.get("totalPriceTax")));
        r.setDeptShare(toBd(p.get("deptShare")));
        r.setInvoiceNo(WebUtil.getSafeParam(p, "invoiceNo"));
        r.setReceiveDept(WebUtil.getSafeParam(p, "receiveDept"));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        r.setStatus("DRAFT");
        if (r.getId() == null) r.setId(dao.insertReceived(r));
        else dao.updateReceived(r);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", r.getId()));
    }

    private void doConfirmRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String step = WebUtil.getSafeParam(p, "step"); // PURCHASE/OP/FINANCE
        dao.confirmReceived(id, step, u.getId());
        ResponseUtil.ok(resp);
    }

    private void doDeleteRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.deleteReceived(id);
        ResponseUtil.ok(resp);
    }

    private void doListPaid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        ResponseUtil.ok(resp, dao.listPaidByProject(
                WebUtil.getLong(p, "projectId", 0),
                WebUtil.getSafeParam(p, "stage"),
                WebUtil.getSafeParam(p, "rebateType")));
    }

    private void doSavePaid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Paid r = new Paid();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setAgreementId(WebUtil.getLong(p, "agreementId", 0) == 0 ? null : WebUtil.getLong(p, "agreementId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setRebateType(WebUtil.getSafeParam(p, "rebateType"));
        r.setApplicant(WebUtil.getSafeParam(p, "applicant"));
        r.setApplyDept(WebUtil.getSafeParam(p, "applyDept"));
        String ad = WebUtil.getSafeParam(p, "applyDate");
        if (ad != null && !ad.isEmpty()) r.setApplyDate(Date.valueOf(ad));
        r.setReceiveDept(WebUtil.getSafeParam(p, "receiveDept"));
        r.setCustomerName(WebUtil.getSafeParam(p, "customerName"));
        r.setTotalRebate(toBd(p.get("totalRebate")));
        r.setActualRebate(toBd(p.get("actualRebate")));
        r.setDiffAmount(r.getTotalRebate().subtract(r.getActualRebate()));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        r.setExecuteStatus("DRAFT");
        if (r.getId() == null) r.setId(dao.insertPaid(r));
        else dao.updatePaid(r);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", r.getId()));
    }

    private void doConfirmPaid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String step = WebUtil.getSafeParam(p, "step");
        dao.confirmPaid(id, step, u.getId());
        ResponseUtil.ok(resp);
    }

    private void doDeletePaid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.deletePaid(id);
        ResponseUtil.ok(resp);
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void doExportReceived(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Received> list = dao.listReceivedByProject(pid, null, null);

        List<String> headers = Arrays.asList("阶段", "返利类型", "申请人", "申请部门", "申请日期",
                "财务编码", "返利金额", "税率", "价税合计", "部门分摊", "发票号", "收款部门", "状态", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Received r : list) {
            rows.add(Arrays.asList(
                r.getStage() != null ? r.getStage() : "",
                r.getRebateType() != null ? r.getRebateType() : "",
                r.getApplicant() != null ? r.getApplicant() : "",
                r.getApplyDept() != null ? r.getApplyDept() : "",
                r.getApplyDate() != null ? r.getApplyDate().toString() : "",
                r.getFinanceCode() != null ? r.getFinanceCode() : "",
                r.getRebateAmount() != null ? r.getRebateAmount().toString() : "",
                r.getTaxRate() != null ? r.getTaxRate().toString() : "",
                r.getTotalPriceTax() != null ? r.getTotalPriceTax().toString() : "",
                r.getDeptShare() != null ? r.getDeptShare().toString() : "",
                r.getInvoiceNo() != null ? r.getInvoiceNo() : "",
                r.getReceiveDept() != null ? r.getReceiveDept() : "",
                r.getStatus() != null ? r.getStatus() : "",
                r.getRemark() != null ? r.getRemark() : ""
            ));
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目实收_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportPaid(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Paid> list = dao.listPaidByProject(pid, null, null);

        List<String> headers = Arrays.asList("阶段", "下游协议", "返利类型", "申请人", "申请部门", "申请日期",
                "收款部门", "客户名称", "应付返利", "实付返利", "差异", "状态", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Paid r : list) {
            List<String> row = new ArrayList<>();
            row.add(r.getStage() != null ? r.getStage() : "");
            row.add(r.getAgreementId() != null ? r.getAgreementId().toString() : "");
            row.add(r.getRebateType() != null ? r.getRebateType() : "");
            row.add(r.getApplicant() != null ? r.getApplicant() : "");
            row.add(r.getApplyDept() != null ? r.getApplyDept() : "");
            row.add(r.getApplyDate() != null ? r.getApplyDate().toString() : "");
            row.add(r.getReceiveDept() != null ? r.getReceiveDept() : "");
            row.add(r.getCustomerName() != null ? r.getCustomerName() : "");
            row.add(r.getTotalRebate() != null ? r.getTotalRebate().toString() : "");
            row.add(r.getActualRebate() != null ? r.getActualRebate().toString() : "");
            row.add(r.getDiffAmount() != null ? r.getDiffAmount().toString() : "");
            row.add(r.getExecuteStatus() != null ? r.getExecuteStatus() : "");
            row.add(r.getRemark() != null ? r.getRemark() : "");
            rows.add(row);
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目实付_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    // ====================== 从BPM引入实收 ======================

    /**
     * 从BPM(Oracle)按时间区间+发票号查询可用实收数据
     * 参数：startDate, endDate, invoiceNo（可选）
     */
    private void doListBpmReceived(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String startDate = WebUtil.getSafeParam(p, "startDate");
        String endDate = WebUtil.getSafeParam(p, "endDate");
        String invoiceNo = WebUtil.getSafeParam(p, "invoiceNo");
        if (startDate == null || startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
            ResponseUtil.fail(resp, "请填写时间区间");
            return;
        }
        List<Map<String, Object>> list = dao.listBpmReceived(startDate, endDate, invoiceNo);
        ResponseUtil.ok(resp, list);
    }

    /**
     * 批量从BPM引入实收
     * 参数：projectId, stage, items(JSON数组)
     * 每个item: bpmProcessId, applicant, applyDept, applyDate, financeCode, belongToYear,
     *           secondaryRebateAmount, rebateAmount, supplier, rebateProject, invoiceNo,
     *           taxRate, deptShare, rebateType
     *
     * stage: 阶段一/阶段二/阶段三/阶段四/全年, 必填。
     * rebateType: 优先取BPM接口返回/用户在前端选择的值; 空值时默认"票折"。
     */
    @SuppressWarnings("unchecked")
    private void doImportFromBpm(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p,
                                 com.rebate.model.UserContext u) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId <= 0) {
            ResponseUtil.fail(resp, "请选择项目");
            return;
        }
        String stage = WebUtil.getSafeParam(p, "stage");
        if (stage == null || stage.isEmpty()) {
            ResponseUtil.fail(resp, "请选择引入阶段(阶段一~四或全年)");
            return;
        }
        if (!java.util.Arrays.asList("阶段一", "阶段二", "阶段三", "阶段四", "全年").contains(stage)) {
            ResponseUtil.fail(resp, "阶段值不合法");
            return;
        }
        Object itemsObj = p.get("items");
        if (itemsObj == null) {
            ResponseUtil.fail(resp, "请选择要引入的记录");
            return;
        }
        final List<Map<String, Object>> items;
        if (itemsObj instanceof String) {
            // JSON 字符串解析
            try {
                items = com.rebate.util.JsonUtil.parseList((String) itemsObj);
            } catch (Exception e) {
                ResponseUtil.fail(resp, "items格式错误: " + e.getMessage());
                return;
            }
        } else if (itemsObj instanceof List) {
            items = (List<Map<String, Object>>) itemsObj;
        } else {
            ResponseUtil.fail(resp, "items格式错误");
            return;
        }
        if (items.isEmpty()) {
            ResponseUtil.fail(resp, "请选择要引入的记录");
            return;
        }

        final Timestamp now = new Timestamp(System.currentTimeMillis());
        final long userId = u.getId();
        final List<Long> ids = new ArrayList<>();
        final int[] count = {0};
        BaseDao.<Void>executeInTransaction((Connection conn) -> {
            for (Map<String, Object> item : items) {
                Received r = new Received();
                r.setProjectId(projectId);
                r.setStage(stage);
                String rt = getStr(item, "rebateType");
                // 返利类型: BPM没返回或传空时, 兜底默认"票折", 不再在所有场景下硬编码固定值
                if (rt == null || rt.trim().isEmpty()) rt = "票折";
                r.setRebateType(rt);
                r.setApplicant(getStr(item, "applicant"));
                r.setApplyDept(getStr(item, "applyDept"));
                String ad = getStr(item, "applyDate");
                if (ad != null && !ad.isEmpty()) {
                    try { r.setApplyDate(Date.valueOf(ad)); } catch (Exception ignore) {}
                }
                r.setFinanceCode(getStr(item, "financeCode"));
                r.setRebateAmount(toBd(item.get("rebateAmount")));
                r.setTaxRate(toBd(item.get("taxRate")));
                r.setTotalPriceTax(toBd(item.get("rebateAmount"))); // 返利金额存入价税合计
                r.setDeptShare(toBd(item.get("deptShare")));
                r.setInvoiceNo(getStr(item, "invoiceNo"));
                r.setReceiveDept(getStr(item, "applyDept"));
                r.setStatus("FINAL");
                r.setBpmProcessId(getStr(item, "bpmProcessId"));
                r.setPurchaseUser(userId);
                r.setPurchaseTime(now);
                r.setOpUser(userId);
                r.setOpTime(now);
                r.setFinanceUser(userId);
                r.setFinanceTime(now);
                r.setFinalTime(now);
                r.setRemark("从BPM引入");
                Long id = dao.insertReceivedFromBpmWithConn(conn, r);
                if (id != null) { count[0]++; ids.add(id); }
            }
            return null;
        });
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("insertedCount", count[0]);
        result.put("ids", ids);
        ResponseUtil.ok(resp, result);
    }

    private String getStr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }
}

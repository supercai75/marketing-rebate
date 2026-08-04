package com.rebate.servlet;

import com.rebate.dao.ProjectDao;
import com.rebate.dao.ReceivablePayableDao;
import com.rebate.dao.UpstreamFlowDao;
import com.rebate.model.Payable;
import com.rebate.model.Project;
import com.rebate.model.Receivable;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.stream.Collectors;

/**
 * 应收/应付
 */
public class ReceivablePayableServlet extends BaseServlet {

    private final ReceivablePayableDao dao = new ReceivablePayableDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "listReceivable";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "listReceivable": doListRecv(req, resp, p); break;
            case "getReceivable": doGetRecv(req, resp, p); break;
            case "saveReceivable": doSaveRecv(req, resp, p, u); break;
            case "auditReceivable": doAuditRecv(req, resp, p, u); break;
            case "deleteReceivable": doDeleteRecv(req, resp, p); break;
            case "listPayable": doListPay(req, resp, p); break;
            case "savePayable": doSavePay(req, resp, p, u); break;
            case "auditPayable": doAuditPay(req, resp, p, u); break;
            case "confirmPayable": doConfirmPay(req, resp, p, u); break;
            case "exportReceivable": doExportReceivable(req, resp, p); break;
            case "exportPayable": doExportPayable(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "listReceivable":
            case "getReceivable":
            case "exportReceivable":
                return u.hasPerm("receivable:view");
            case "saveReceivable":
            case "deleteReceivable":
                return u.hasPerm("receivable:edit");
            case "auditReceivable":
                return u.hasPerm("receivable:audit");
            case "listPayable":
            case "exportPayable":
                return u.hasPerm("payable:view");
            case "savePayable":
                return u.hasPerm("payable:edit");
            case "auditPayable":
            case "confirmPayable":
                return u.hasPerm("payable:audit");
            default:
                return true;
        }
    }

    private void doListRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, dao.listReceivableByProject(pid));
    }

    private void doGetRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        ResponseUtil.ok(resp, dao.findReceivable(id));
    }

    private void doSaveRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        Receivable r = new Receivable();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setScaleAmount(toBd(p.get("scaleAmount")));
        r.setAssessAmount(toBd(p.get("assessAmount")));
        r.setTotalAmount(r.getScaleAmount().add(r.getAssessAmount()));
        r.setEstimateAmount(toBd(p.get("estimateAmount")));
        r.setStatus("DRAFT");
        r.setFillUser(u.getId());
        r.setFillTime(new Timestamp(System.currentTimeMillis()));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        if (r.getId() == null) {
            // 检查是否已存在
            // 简化：直接插入
            r.setId(dao.insertReceivable(r));
        } else {
            dao.updateReceivable(r);
        }
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", r.getId()));
    }

    private void doAuditRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String action = WebUtil.getSafeParam(p, "action"); // PASS / REJECT
        String newStatus = "PASS".equals(action) ? "FINAL" : "REJECTED";
        dao.auditReceivable(id, u.getId(), newStatus);
        
        // 如果审核通过，将上游流向对应月份置为终稿
        if ("PASS".equals(action)) {
            Receivable r = dao.findReceivable(id);
            if (r != null && r.getStage() != null) {
                markUpstreamFlowFinal(r.getProjectId(), r.getStage(), u.getId());
            }
        }
        
        ResponseUtil.ok(resp);
    }

    /**
     * 将上游流向对应月份置为终稿
     * 月份计算规则：项目起始时间+阶段开始月份到项目起始时间+阶段截止月份
     */
    private void markUpstreamFlowFinal(long projectId, String stage, long userId) {
        ProjectDao projectDao = new ProjectDao();
        Project project = projectDao.findById(projectId);
        if (project == null || project.getPeriodStartDate() == null) return;
        
        // 计算阶段对应的月份范围
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(project.getPeriodStartDate());
        int startYear = cal.get(java.util.Calendar.YEAR);
        int startMonth = cal.get(java.util.Calendar.MONTH) + 1;
        
        // 阶段对应的相对月份范围（每个阶段3个月）
        int stageIndex = 0;
        if ("阶段一".equals(stage)) stageIndex = 0;
        else if ("阶段二".equals(stage)) stageIndex = 1;
        else if ("阶段三".equals(stage)) stageIndex = 2;
        else if ("阶段四".equals(stage)) stageIndex = 3;
        else return; // 全年或其他不处理
        
        int stageStartMonth = stageIndex * 3; // 相对月份偏移
        int stageEndMonth = stageStartMonth + 2; // 阶段结束相对月份
        
        // 计算实际的年月
        int actualStartYear = startYear + (startMonth + stageStartMonth - 1) / 12;
        int actualStartMonth = (startMonth + stageStartMonth - 1) % 12 + 1;
        int actualEndYear = startYear + (startMonth + stageEndMonth - 1) / 12;
        int actualEndMonth = (startMonth + stageEndMonth - 1) % 12 + 1;
        
        // 生成月份列表
        List<String> months = new ArrayList<>();
        int y = actualStartYear, m = actualStartMonth;
        while (y < actualEndYear || (y == actualEndYear && m <= actualEndMonth)) {
            months.add(String.format("%04d%02d", y, m));
            m++;
            if (m > 12) { m = 1; y++; }
        }
        
        // 将这些月份的上游流向置为终稿
        UpstreamFlowDao upstreamFlowDao = new UpstreamFlowDao();
        for (String month : months) {
            upstreamFlowDao.setFinalMonth(projectId, month, userId);
            upstreamFlowDao.markFinalInRecords(projectId, month);
        }
    }

    private void doDeleteRecv(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.deleteReceivable(id);
        ResponseUtil.ok(resp);
    }

    private void doListPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        Long agId = WebUtil.getLong(p, "agreementId", 0);
        if (agId != null && agId == 0) agId = null;
        ResponseUtil.ok(resp, dao.listPayableByProject(pid, agId, WebUtil.getSafeParam(p, "stage"), WebUtil.getSafeParam(p, "status")));
    }

    private void doSavePay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        Payable r = new Payable();
        r.setId(WebUtil.getLong(p, "id", 0) == 0 ? null : WebUtil.getLong(p, "id", 0));
        r.setProjectId(WebUtil.getLong(p, "projectId", 0));
        r.setAgreementId(WebUtil.getLong(p, "agreementId", 0));
        r.setStage(WebUtil.getSafeParam(p, "stage"));
        r.setScaleAmount(toBd(p.get("scaleAmount")));
        r.setAssessAmount(toBd(p.get("assessAmount")));
        r.setTotalAmount(r.getScaleAmount().add(r.getAssessAmount()));
        r.setEstimateAmount(toBd(p.get("estimateAmount")));
        r.setStatus("DRAFT");
        r.setFillUser(u.getId());
        r.setFillTime(new Timestamp(System.currentTimeMillis()));
        r.setRemark(WebUtil.getSafeParam(p, "remark"));
        if (r.getId() == null) {
            r.setId(dao.insertPayable(r));
        } else {
            dao.updatePayable(r);
        }
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", r.getId()));
    }

    private void doAuditPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String action = WebUtil.getSafeParam(p, "action");
        String newStatus = "PASS".equals(action) ? "AUDIT" : "REJECTED";
        dao.auditPayable(id, u.getId(), newStatus);
        ResponseUtil.ok(resp);
    }

    private void doConfirmPay(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.confirmPayable(id, u.getId(), "FINAL");
        ResponseUtil.ok(resp);
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void doExportReceivable(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Receivable> list = dao.listReceivableByProject(pid);

        List<String> headers = Arrays.asList("阶段", "依据规模应收", "依据考核应收", "合计应收",
                "系统估算", "状态", "填报人", "填报时间", "审核人", "审核时间", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Receivable r : list) {
            rows.add(Arrays.asList(
                r.getStage() != null ? r.getStage() : "",
                r.getScaleAmount() != null ? r.getScaleAmount().toString() : "",
                r.getAssessAmount() != null ? r.getAssessAmount().toString() : "",
                r.getTotalAmount() != null ? r.getTotalAmount().toString() : "",
                r.getEstimateAmount() != null ? r.getEstimateAmount().toString() : "",
                r.getStatus() != null ? r.getStatus() : "",
                r.getFillUser() != null ? r.getFillUser().toString() : "",
                r.getFillTime() != null ? r.getFillTime().toString() : "",
                r.getAuditUser() != null ? r.getAuditUser().toString() : "",
                r.getAuditTime() != null ? r.getAuditTime().toString() : "",
                r.getRemark() != null ? r.getRemark() : ""
            ));
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目应收_" + sdf.format(new Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportPayable(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        List<Payable> list = dao.listPayableByProject(pid, null, null, null);

        List<String> headers = Arrays.asList("阶段", "依据规模应付", "依据考核应付", "合计应付",
                "系统估算", "状态", "填报人", "填报时间", "审核人", "审核时间", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (Payable r : list) {
            rows.add(Arrays.asList(
                r.getStage() != null ? r.getStage() : "",
                r.getScaleAmount() != null ? r.getScaleAmount().toString() : "",
                r.getAssessAmount() != null ? r.getAssessAmount().toString() : "",
                r.getTotalAmount() != null ? r.getTotalAmount().toString() : "",
                r.getEstimateAmount() != null ? r.getEstimateAmount().toString() : "",
                r.getStatus() != null ? r.getStatus() : "",
                r.getFillUser() != null ? r.getFillUser().toString() : "",
                r.getFillTime() != null ? r.getFillTime().toString() : "",
                r.getAuditUser() != null ? r.getAuditUser().toString() : "",
                r.getAuditTime() != null ? r.getAuditTime().toString() : "",
                r.getRemark() != null ? r.getRemark() : ""
            ));
        }

        Workbook wb = ExcelUtil.exportSimple(headers, rows);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileName = "项目应付_" + sdf.format(new Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }
}

package com.rebate.servlet;

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
     * 按项目编号从BPM数据库查询可用的实收数据（供用户选择）
     */
    private void doListBpmReceived(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId <= 0) {
            ResponseUtil.fail(resp, "请选择项目");
            return;
        }
        Project project = projectDao.findById(projectId);
        if (project == null) {
            ResponseUtil.fail(resp, "项目不存在");
            return;
        }
        String projectCode = project.getProjectCode();
        if (projectCode == null || projectCode.isEmpty()) {
            ResponseUtil.ok(resp, Collections.emptyList());
            return;
        }
        List<Map<String, Object>> list = dao.listBpmReceived(projectCode);
        // 批量查询已引入的财务编码，避免N次单条查询
        List<String> financeCodes = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Object fc = row.get("financeCode");
            if (fc != null && !fc.toString().trim().isEmpty()) {
                financeCodes.add(fc.toString());
            }
        }
        java.util.Set<String> importedSet = dao.findImportedFinanceCodes(financeCodes);
        // 附加 isImported 标记
        for (Map<String, Object> row : list) {
            Object fc = row.get("financeCode");
            row.put("isImported", fc != null && importedSet.contains(fc.toString()));
        }
        ResponseUtil.ok(resp, list);
    }

    /**
     * 用户确认引入：将一条BPM数据按阶段拆分为多条实收记录（终稿状态）
     *
     * 请求参数：
     *   projectId: 项目ID
     *   rebateType: 返利类型（票折/服务费）
     *   applyDate: 申请日期
     *   financeCode: 财务编码
     *   invoiceNo: 发票号码
     *   rebateAmount: 返利金额
     *   taxRate: 销售税率
     *   totalPriceTax: 价税合计
     *   receiveDept: 收款部门
     *   stageOne: 阶段一金额（部门应得分配到阶段一的金额）
     *   stageTwo: 阶段二金额
     *   stageThree: 阶段三金额
     *   stageFour: 阶段四金额
     *   (四个阶段之和必须等于BPM的部门应得)
     */
    private void doImportFromBpm(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p,
                                 com.rebate.model.UserContext u) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId <= 0) {
            ResponseUtil.fail(resp, "请选择项目");
            return;
        }

        String rebateType = WebUtil.getSafeParam(p, "rebateType");
        String applyDate = WebUtil.getSafeParam(p, "applyDate");
        String financeCode = WebUtil.getSafeParam(p, "financeCode");
        String invoiceNo = WebUtil.getSafeParam(p, "invoiceNo");
        BigDecimal rebateAmount = toBd(p.get("rebateAmount"));
        BigDecimal taxRate = toBd(p.get("taxRate"));
        BigDecimal totalPriceTax = toBd(p.get("totalPriceTax"));
        String receiveDeptStr = WebUtil.getSafeParam(p, "receiveDept");

        BigDecimal stageOne = toBd(p.get("stageOne"));
        BigDecimal stageTwo = toBd(p.get("stageTwo"));
        BigDecimal stageThree = toBd(p.get("stageThree"));
        BigDecimal stageFour = toBd(p.get("stageFour"));
        BigDecimal deptShareBpm = toBd(p.get("deptShare"));

        // 校验：各阶段之和 == 部门应得
        BigDecimal stageSum = stageOne.add(stageTwo).add(stageThree).add(stageFour);
        if (stageSum.compareTo(deptShareBpm) != 0) {
            ResponseUtil.fail(resp, "各阶段金额之和(" + stageSum + ")必须等于BPM部门应得(" + deptShareBpm + ")");
            return;
        }

        // 校验：至少有一个阶段有金额
        if (stageSum.compareTo(BigDecimal.ZERO) == 0) {
            ResponseUtil.fail(resp, "各阶段金额之和不能为0");
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());

        // 按阶段有值则写入一条实收记录（终稿状态FINAL）
        String[] stages = {"阶段一", "阶段二", "阶段三", "阶段四"};
        BigDecimal[] stageAmounts = {stageOne, stageTwo, stageThree, stageFour};
        List<Long> insertedIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (stageAmounts[i] == null || stageAmounts[i].compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            Received r = new Received();
            r.setProjectId(projectId);
            r.setStage(stages[i]);
            r.setRebateType(rebateType);
            r.setApplicant(u.getName() != null ? u.getName() : (u.getLoginName() != null ? u.getLoginName() : "SYSTEM"));
            r.setApplyDept("营销中心");
            if (applyDate != null && !applyDate.isEmpty()) {
                try {
                    r.setApplyDate(Date.valueOf(applyDate));
                } catch (Exception ignore) {}
            }
            r.setFinanceCode(financeCode);
            r.setInvoiceNo(invoiceNo);
            r.setRebateAmount(rebateAmount);
            r.setTaxRate(taxRate);
            r.setTotalPriceTax(totalPriceTax);
            r.setDeptShare(stageAmounts[i]);
            r.setReceiveDept(receiveDeptStr);
            // 从BPM引入的直接为终稿状态
            r.setStatus("FINAL");
            r.setFinanceUser(u.getId());
            r.setFinanceTime(now);
            r.setFinalTime(now);
            r.setRemark("从BPM引入，原始财务编码: " + (financeCode != null ? financeCode : ""));
            Long id = dao.insertReceived(r);
            if (id != null) insertedIds.add(id);
        }

        ResponseUtil.ok(resp, new HashMap<String, Object>() {{
            put("insertedCount", insertedIds.size());
            put("ids", insertedIds);
        }});
    }
}

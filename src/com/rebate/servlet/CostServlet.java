package com.rebate.servlet;

import com.rebate.dao.CostDao;
import com.rebate.model.ProjectExpense;
import com.rebate.model.ProjectLabor;
import com.rebate.service.CostAllocateService;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * 项目费用 / 人工 导入
 */
@MultipartConfig
public class CostServlet extends BaseServlet {

    private final CostDao dao = new CostDao();
    private final CostAllocateService alloc = new CostAllocateService();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, com.rebate.model.UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "list";
        
        if (!checkPerm(u, op, p)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "list": doList(req, resp, p); break;
            case "listExpense": doListExpense(req, resp, p); break;
            case "listLabor": doListLabor(req, resp, p); break;
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p, u); break;
            case "update": doUpdate(req, resp, p, u); break;
            case "delete": doDelete(req, resp, p); break;
            case "addExpense": doAddExpense(req, resp, p, u); break;
            case "addLabor": doAddLabor(req, resp, p, u); break;
            case "importExpenseExcel": doImportExpense(req, resp, u); break;
            case "importLaborExcel": doImportLabor(req, resp, u); break;
            case "reallocate": doReallocate(req, resp); break;
            case "exportExpense": doExportExpense(req, resp, p); break;
            case "exportLabor": doExportLabor(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }
    
    private boolean checkPerm(com.rebate.model.UserContext u, String op, Map<String, Object> p) {
        if (u.isAdmin()) return true;
        String costType = WebUtil.getSafeParam(p, "costType");
        boolean isLabor = "LABOR".equalsIgnoreCase(costType);
        switch (op) {
            case "list":
                if (isLabor) return u.hasPerm("labor:view");
                return u.hasPerm("expense:view");
            case "listExpense":
                return u.hasPerm("expense:view");
            case "get":
                if (isLabor) return u.hasPerm("labor:view");
                return u.hasPerm("expense:view");
            case "exportExpense":
                return u.hasPerm("expense:view");
            case "addExpense":
                return u.hasPerm("expense:edit");
            case "importExpenseExcel":
                return u.hasPerm("expense:import");
            case "listLabor":
            case "exportLabor":
                return u.hasPerm("labor:view");
            case "addLabor":
                return u.hasPerm("labor:edit");
            case "importLaborExcel":
                return u.hasPerm("labor:import");
            case "add":
            case "update":
            case "delete":
                if (isLabor) return u.hasPerm("labor:edit");
                return u.hasPerm("expense:edit");
            case "reallocate":
                if (isLabor) return u.hasPerm("labor:edit");
                return u.hasPerm("expense:edit");
            default:
                return false;
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String costType = WebUtil.getSafeParam(p, "costType");
        int page = WebUtil.getInt(p, "page", 1);
        int pageSize = WebUtil.getInt(p, "pageSize", 20);
        
        String workNo = WebUtil.getSafeParam(p, "workNo");
        String expenseType = WebUtil.getSafeParam(p, "expenseType");
        String docNo = WebUtil.getSafeParam(p, "docNo");
        String startDate = WebUtil.getSafeParam(p, "startDate");
        String endDate = WebUtil.getSafeParam(p, "endDate");
        
        List<Map<String, Object>> allRows = new ArrayList<>();
        if ("LABOR".equals(costType)) {
            List<ProjectLabor> labors = dao.listLabors(pid, null);
            for (ProjectLabor l : labors) {
                Map<String, Object> m = toLaborMap(l, pid);
                allRows.add(m);
            }
        } else if ("EXPENSE".equals(costType)) {
            List<ProjectExpense> expenses = dao.listExpenses(pid, workNo, expenseType, docNo, startDate, endDate);
            for (ProjectExpense e : expenses) {
                Map<String, Object> m = toExpenseMap(e, pid);
                allRows.add(m);
            }
        }
        
        int total = allRows.size();
        int totalPages = (total + pageSize - 1) / pageSize;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<Map<String, Object>> pageRows = fromIndex < total ? allRows.subList(fromIndex, toIndex) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageRows);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        ResponseUtil.ok(resp, result);
    }

    private void doListExpense(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long pid = WebUtil.getLong(p, "projectId", 0) == 0 ? null : WebUtil.getLong(p, "projectId", 0);
        ResponseUtil.ok(resp, dao.listExpenses(pid, WebUtil.getSafeParam(p, "workNo"), null, null, null, null));
    }

    private void doListLabor(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        Long pid = WebUtil.getLong(p, "projectId", 0) == 0 ? null : WebUtil.getLong(p, "projectId", 0);
        String month = WebUtil.getSafeParam(p, "month");
        ResponseUtil.ok(resp, dao.listLabors(pid, WebUtil.getSafeParam(p, "workNo")));
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        String costType = WebUtil.getSafeParam(p, "costType");
        if (costType == null || costType.isEmpty()) costType = "EXPENSE";
        if ("LABOR".equals(costType)) {
            ProjectLabor l = dao.findLabor(id);
            if (l == null) { ResponseUtil.fail(resp, "记录不存在"); return; }
            ResponseUtil.ok(resp, toLaborMap(l, l.getProjectId() == null ? 0 : l.getProjectId()));
        } else {
            ProjectExpense e = dao.findExpense(id);
            if (e == null) { ResponseUtil.fail(resp, "记录不存在"); return; }
            ResponseUtil.ok(resp, toExpenseMap(e, e.getProjectId() == null ? 0 : e.getProjectId()));
        }
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        String costType = WebUtil.getSafeParam(p, "costType");
        if (costType == null || costType.isEmpty()) costType = "EXPENSE";
        if ("LABOR".equals(costType)) {
            ProjectLabor l = parseLabor(p);
            l.setImportUser(u.getId());
            l.setSource("INPUT");
            // 使用新的分摊规则
            List<ProjectLabor> labors = alloc.allocLaborByRule(l);
            List<Long> ids = new ArrayList<>();
            for (ProjectLabor labor : labors) {
                Long id = dao.insertLabor(labor);
                ids.add(id);
            }
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("ids", ids));
        } else {
            ProjectExpense e = parseExpense(p);
            e.setImportUser(u.getId());
            e.setSource("INPUT");
            // 使用新的分摊规则
            List<ProjectExpense> expenses = alloc.allocateExpenseByRule(e);
            List<Long> ids = new ArrayList<>();
            for (ProjectExpense exp : expenses) {
                Long id = dao.insertExpense(exp);
                ids.add(id);
            }
            ResponseUtil.ok(resp, java.util.Collections.singletonMap("ids", ids));
        }
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        long id = WebUtil.getLong(p, "id", 0);
        String costType = WebUtil.getSafeParam(p, "costType");
        if (costType == null || costType.isEmpty()) costType = "EXPENSE";
        if ("LABOR".equals(costType)) {
            ProjectLabor l = parseLabor(p);
            l.setId(id);
            l.setImportUser(u.getId());
            l = alloc.allocateLabor(l);
            dao.updateLabor(l);
            ResponseUtil.ok(resp);
        } else {
            ProjectExpense e = parseExpense(p);
            e.setId(id);
            e.setImportUser(u.getId());
            e = alloc.allocateExpense(e);
            dao.updateExpense(e);
            ResponseUtil.ok(resp);
        }
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        String costType = WebUtil.getSafeParam(p, "costType");
        if (costType == null || costType.isEmpty()) costType = "EXPENSE";
        if ("LABOR".equals(costType)) dao.deleteLabor(id);
        else dao.deleteExpense(id);
        ResponseUtil.ok(resp);
    }

    private Map<String, Object> toLaborMap(ProjectLabor l, long projectId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("projectId", projectId);
        m.put("month", l.getMonthYyyymm());
        m.put("userCode", l.getWorkNo());
        m.put("userName", l.getName());
        m.put("workType", l.getWorkType() == null ? "FULL" : l.getWorkType());
        m.put("salary", l.getSalary());
        m.put("welfare", l.getWelfare());
        m.put("totalAmount", l.getTotalCost());
        m.put("allocatedAmount", l.getAllocatedAmount());
        m.put("allocRatio", l.getAllocRatio());
        m.put("description", l.getRemark());
        return m;
    }

    private Map<String, Object> toExpenseMap(ProjectExpense e, long projectId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("projectId", projectId);
        m.put("expenseType", e.getExpenseType());
        m.put("costType", e.getExpenseType());
        m.put("invoiceAmount", e.getAmount());
        m.put("amount", e.getAmount());
        m.put("allocatedAmount", e.getAllocatedAmount() == null ? e.getAmount() : e.getAllocatedAmount());
        m.put("reimburseDate", e.getReimburseDate());
        m.put("occurDate", e.getReimburseDate());
        m.put("remark", e.getRemark());
        m.put("description", e.getDescription());
        m.put("docNo", e.getDocNo());
        m.put("workNo", e.getWorkNo());
        m.put("userName", e.getName());
        m.put("importTime", e.getImportTime());
        return m;
    }

    private BigDecimal orZero(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private void doAddExpense(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        ProjectExpense e = parseExpense(p);
        e.setImportUser(u.getId());
        e.setSource("INPUT");
        // 使用新的分摊规则
        List<ProjectExpense> expenses = alloc.allocateExpenseByRule(e);
        List<Long> ids = new ArrayList<>();
        for (ProjectExpense exp : expenses) {
            Long id = dao.insertExpense(exp);
            ids.add(id);
        }
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("ids", ids));
    }

    private void doAddLabor(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p, com.rebate.model.UserContext u) {
        ProjectLabor l = parseLabor(p);
        l.setImportUser(u.getId());
        l.setSource("INPUT");
        // 使用新的分摊规则
        List<ProjectLabor> labors = alloc.allocLaborByRule(l);
        List<Long> ids = new ArrayList<>();
        for (ProjectLabor labor : labors) {
            Long id = dao.insertLabor(labor);
            ids.add(id);
        }
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("ids", ids));
    }

    private void doImportLabor(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择 Excel"); return; }
        List<Map<String, String>> rows = ExcelUtil.readSheetAsMap(file.getInputStream());
        int ok = 0, fail = 0;
        List<String> errs = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                Map<String, String> row = rows.get(i);
                ProjectLabor l = new ProjectLabor();
                
                String month = row.get("月度");
                if (month != null && month.length() >= 7) {
                    l.setMonthYyyymm(month.substring(0, 7));
                }
                l.setWorkNo(row.get("人员工号"));
                l.setName(row.get("姓名"));
                String salary = row.get("应发工资");
                l.setSalary(salary == null || salary.isEmpty() ? BigDecimal.ZERO : new BigDecimal(salary));
                String welfare = row.get("福利等其它人工费用");
                l.setWelfare(welfare == null || welfare.isEmpty() ? BigDecimal.ZERO : new BigDecimal(welfare));
                String total = row.get("费用合计");
                l.setTotalCost(total == null || total.isEmpty() ? l.getSalary().add(l.getWelfare()) : new BigDecimal(total));
                l.setSource("IMPORT");
                l.setImportUser(u.getId());
                
                // 使用新的分摊规则
                List<ProjectLabor> labors = alloc.allocLaborByRule(l);
                for (ProjectLabor labor : labors) {
                    dao.insertLabor(labor);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                errs.add("第" + (i + 2) + "行: " + ex.getMessage());
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("ok", ok);
        r.put("fail", fail);
        r.put("errors", errs);
        ResponseUtil.ok(resp, r);
    }

    private void doImportExpense(HttpServletRequest req, HttpServletResponse resp, com.rebate.model.UserContext u) throws Exception {
        Part file = req.getPart("file");
        String projectIdStr = req.getParameter("projectId");
        if (file == null) { ResponseUtil.fail(resp, "请选择 Excel"); return; }
        List<Map<String, String>> rows = ExcelUtil.readSheetAsMap(file.getInputStream());
        int ok = 0, fail = 0;
        List<String> errs = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            try {
                Map<String, String> row = rows.get(i);
                ProjectExpense e = new ProjectExpense();
                
                // 注意：这里不要设置 projectId，让分摊逻辑自动处理
                String rd = row.get("报销时间");
                if (rd != null && !rd.isEmpty()) {
                    try { e.setReimburseDate(Date.valueOf(rd)); } catch (Exception ex) { 
                        try { e.setReimburseDate(Date.valueOf(rd.substring(0, 10))); } catch (Exception ex2) {}
                    }
                }
                e.setExpenseType(row.get("费用类型"));
                if (e.getExpenseType() == null) e.setExpenseType(row.get("费用类别"));
                e.setWorkNo(row.get("报销人工号"));
                if (e.getWorkNo() == null) e.setWorkNo(row.get("人员工号"));
                e.setName(row.get("报销人姓名"));
                if (e.getName() == null) e.setName(row.get("姓名"));
                e.setRemark(row.get("备注"));
                e.setDocNo(row.get("发票号"));
                String a = row.get("发票金额");
                if (a == null || a.isEmpty()) a = row.get("费用金额");
                e.setAmount(a == null || a.isEmpty() ? BigDecimal.ZERO : new BigDecimal(a));
                e.setSource("IMPORT");
                e.setImportUser(u.getId());
                
                // 使用新的分摊规则
                List<ProjectExpense> expenses = alloc.allocateExpenseByRule(e);
                for (ProjectExpense exp : expenses) {
                    dao.insertExpense(exp);
                }
                ok++;
            } catch (Exception ex) {
                fail++;
                errs.add("第" + (i + 2) + "行: " + ex.getMessage());
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("ok", ok);
        r.put("fail", fail);
        r.put("errors", errs);
        ResponseUtil.ok(resp, r);
    }

    private void doReallocate(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, "OK");
    }

    private ProjectExpense parseExpense(Map<String, Object> p) {
        ProjectExpense e = new ProjectExpense();
        long pid = WebUtil.getLong(p, "projectId", 0);
        e.setProjectId(pid == 0 ? null : pid);
        String rd = WebUtil.getSafeParam(p, "reimburseDate");
        if (rd == null) rd = WebUtil.getSafeParam(p, "occurDate");
        if (rd != null && !rd.isEmpty()) e.setReimburseDate(Date.valueOf(rd));
        e.setExpenseType(WebUtil.getSafeParam(p, "expenseType"));
        if (e.getExpenseType() == null) e.setExpenseType(WebUtil.getSafeParam(p, "costType"));
        e.setWorkNo(WebUtil.getSafeParam(p, "workNo"));
        if (e.getWorkNo() == null) e.setWorkNo(WebUtil.getSafeParam(p, "userCode"));
        e.setName(WebUtil.getSafeParam(p, "name"));
        if (e.getName() == null) e.setName(WebUtil.getSafeParam(p, "userName"));
        e.setDescription(WebUtil.getSafeParam(p, "description"));
        e.setAmount(toBd(p.get("amount")));
        e.setRawProjectName(WebUtil.getSafeParam(p, "rawProjectName"));
        e.setDocNo(WebUtil.getSafeParam(p, "docNo"));
        e.setRemark(WebUtil.getSafeParam(p, "remark"));
        return e;
    }

    private ProjectLabor parseLabor(Map<String, Object> p) {
        ProjectLabor l = new ProjectLabor();
        String month = WebUtil.getSafeParam(p, "monthYyyymm");
        if (month == null) month = WebUtil.getSafeParam(p, "month");
        if (month != null && month.length() > 7) month = month.substring(0, 7);
        l.setMonthYyyymm(month);
        l.setWorkNo(WebUtil.getSafeParam(p, "workNo"));
        if (l.getWorkNo() == null) l.setWorkNo(WebUtil.getSafeParam(p, "userCode"));
        l.setName(WebUtil.getSafeParam(p, "name"));
        if (l.getName() == null) l.setName(WebUtil.getSafeParam(p, "userName"));
        l.setWorkType(WebUtil.getSafeParam(p, "workType"));
        l.setSalary(toBd(p.get("salary")));
        l.setWelfare(toBd(p.get("welfare")));
        l.setOtherCost(toBd(p.get("otherCost")));
        BigDecimal total = toBd(p.get("totalAmount"));
        if (total == null || total.signum() == 0) total = toBd(p.get("totalCost"));
        if (total == null || total.signum() == 0) total = BigDecimal.ZERO;
        l.setTotalCost(total);
        l.setRemark(WebUtil.getSafeParam(p, "remark"));
        if (l.getRemark() == null) l.setRemark(WebUtil.getSafeParam(p, "description"));
        return l;
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private void doExportExpense(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String workNo = WebUtil.getSafeParam(p, "workNo");
        String expenseType = WebUtil.getSafeParam(p, "expenseType");
        String docNo = WebUtil.getSafeParam(p, "docNo");
        String startDate = WebUtil.getSafeParam(p, "startDate");
        String endDate = WebUtil.getSafeParam(p, "endDate");

        List<ProjectExpense> expenses = dao.listExpenses(pid, workNo, expenseType, docNo, startDate, endDate);

        List<String> headers = Arrays.asList("报销人工号", "报销人姓名", "报销时间", "费用类型", "发票号",
                "发票金额", "本项目分摊金额", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (ProjectExpense e : expenses) {
            rows.add(Arrays.asList(
                e.getWorkNo() != null ? e.getWorkNo() : "",
                e.getName() != null ? e.getName() : "",
                e.getReimburseDate() != null ? e.getReimburseDate().toString() : "",
                e.getExpenseType() != null ? e.getExpenseType() : "",
                e.getDocNo() != null ? e.getDocNo() : "",
                e.getAmount() != null ? e.getAmount().toString() : "",
                e.getAllocatedAmount() != null ? e.getAllocatedAmount().toString() : "",
                e.getRemark() != null ? e.getRemark() : ""
            ));
        }

        org.apache.poi.ss.usermodel.Workbook wb = ExcelUtil.exportSimple(headers, rows);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        String fileName = "费用投入_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doExportLabor(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long pid = WebUtil.getLong(p, "projectId", 0);
        String workNo = WebUtil.getSafeParam(p, "workNo");

        List<ProjectLabor> labors = dao.listLabors(pid, workNo);

        List<String> headers = Arrays.asList("月份", "人员工号", "姓名", "用工类型", "应发工资",
                "福利等其它人工费用", "费用合计", "本项目分摊金额", "分摊比例", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (ProjectLabor l : labors) {
            rows.add(Arrays.asList(
                l.getMonthYyyymm() != null ? l.getMonthYyyymm() : "",
                l.getWorkNo() != null ? l.getWorkNo() : "",
                l.getName() != null ? l.getName() : "",
                l.getWorkType() != null ? l.getWorkType() : "",
                l.getSalary() != null ? l.getSalary().toString() : "",
                l.getWelfare() != null ? l.getWelfare().toString() : "",
                l.getTotalCost() != null ? l.getTotalCost().toString() : "",
                l.getAllocatedAmount() != null ? l.getAllocatedAmount().toString() : "",
                l.getAllocRatio() != null ? l.getAllocRatio().toString() : "",
                l.getRemark() != null ? l.getRemark() : ""
            ));
        }

        org.apache.poi.ss.usermodel.Workbook wb = ExcelUtil.exportSimple(headers, rows);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd");
        String fileName = "人工投入_" + sdf.format(new java.util.Date()) + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }
}

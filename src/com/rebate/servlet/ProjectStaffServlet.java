package com.rebate.servlet;

import com.rebate.dao.BaseDao;
import com.rebate.dao.ProjectStaffDao;
import com.rebate.model.ProjectStaff;
import com.rebate.model.UserContext;
import com.rebate.util.ExcelUtil;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.math.BigDecimal;
import java.util.*;

/**
 * 项目作业人员管理
 */
@MultipartConfig
public class ProjectStaffServlet extends BaseServlet {

    private final ProjectStaffDao dao = new ProjectStaffDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "list";
        
        if (!checkPerm(u, op)) {
            ResponseUtil.forbidden(resp);
            return;
        }
        
        switch (op) {
            case "list": doList(resp, p); break;
            case "get": doGet(resp, p); break;
            case "add": doAdd(resp, p); break;
            case "update": doUpdate(resp, p); break;
            case "delete": doDelete(resp, p); break;
            case "importStaffExcel": doImportStaff(req, resp, p); break;
            case "downloadStaffTemplate": doDownloadTemplate(resp); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private boolean checkPerm(UserContext u, String op) {
        if (u.isAdmin()) return true;
        switch (op) {
            case "list":
            case "get":
                return u.hasPerm("project:view");
            case "add":
            case "update":
            case "delete":
            case "importStaffExcel":
            case "downloadStaffTemplate":
                return u.hasPerm("project:edit");
            default:
                return false;
        }
    }

    private void doList(HttpServletResponse resp, Map<String, Object> p) {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        int page = WebUtil.getInt(p, "page", 1);
        int pageSize = WebUtil.getInt(p, "pageSize", 10);
        long total = dao.countByProject(projectId);
        List<ProjectStaff> list = dao.pageByProject(projectId, page, pageSize);
        var result = new java.util.HashMap<String, Object>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        ResponseUtil.ok(resp, result);
    }

    private void doGet(HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        ProjectStaff s = dao.findById(id);
        ResponseUtil.ok(resp, s);
    }

    private void doAdd(HttpServletResponse resp, Map<String, Object> p) {
        ProjectStaff s = new ProjectStaff();
        s.setProjectId(WebUtil.getLong(p, "projectId", 0));
        s.setUserName(WebUtil.getSafeParam(p, "userName"));
        s.setUserCode(WebUtil.getSafeParam(p, "userCode"));
        s.setDeptName(WebUtil.getSafeParam(p, "deptName"));
        s.setPosition(WebUtil.getSafeParam(p, "position"));
        s.setWorkType(WebUtil.getSafeParam(p, "workType"));
        s.setLaborCostRatio(toBd(p.get("laborCostRatio")));
        s.setExpenseRatio(toBd(p.get("expenseRatio")));
        Long id = dao.insert(s);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletResponse resp, Map<String, Object> p) {
        ProjectStaff s = new ProjectStaff();
        s.setId(WebUtil.getLong(p, "id", 0));
        s.setUserName(WebUtil.getSafeParam(p, "userName"));
        s.setUserCode(WebUtil.getSafeParam(p, "userCode"));
        s.setDeptName(WebUtil.getSafeParam(p, "deptName"));
        s.setPosition(WebUtil.getSafeParam(p, "position"));
        s.setWorkType(WebUtil.getSafeParam(p, "workType"));
        s.setLaborCostRatio(toBd(p.get("laborCostRatio")));
        s.setExpenseRatio(toBd(p.get("expenseRatio")));
        dao.update(s);
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        dao.delete(id);
        ResponseUtil.ok(resp);
    }

    // ============== 导入模板 & 导入 ==============

    private static final List<String> STAFF_TEMPLATE_HEADERS = Arrays.asList(
            "人员姓名", "工号", "部门", "岗位", "用工类型(全职/兼职/外包)",
            "人工分摊比例(%)", "费用分摊比例(%)"
    );

    private void doDownloadTemplate(HttpServletResponse resp) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        // 1行示例数据，实际导入会被用户删除/覆盖
        rows.add(Arrays.asList("张三", "E001", "市场部", "销售主管", "全职", "100", "100"));
        org.apache.poi.ss.usermodel.Workbook wb = ExcelUtil.exportSimple(STAFF_TEMPLATE_HEADERS, rows);
        String fileName = "项目作业人员导入模板.xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
        wb.write(resp.getOutputStream());
    }

    private void doImportStaff(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        long projectId = WebUtil.getLong(p, "projectId", 0);
        if (projectId == 0) {
            // 兼容从 multipart 的 query 里取 projectId（有些浏览器放在 URL query）
            String qPid = req.getParameter("projectId");
            if (qPid != null && !qPid.isEmpty()) {
                try { projectId = Long.parseLong(qPid); } catch (Exception ignore) {}
            }
        }
        if (projectId == 0) { ResponseUtil.fail(resp, "请先选择项目"); return; }

        Part file = req.getPart("file");
        if (file == null) { ResponseUtil.fail(resp, "请选择要导入的Excel文件"); return; }

        List<Map<String, String>> rows = ExcelUtil.readSheetAsMap(file.getInputStream());
        List<String> errs = new ArrayList<>();
        List<ProjectStaff> toInsert = new ArrayList<>();
        Map<String, String> workTypeMap = new HashMap<>();
        workTypeMap.put("全职", "FULL"); workTypeMap.put("FULL", "FULL");
        workTypeMap.put("兼职", "PART"); workTypeMap.put("PART", "PART");
        workTypeMap.put("外包", "OUTSOURCE"); workTypeMap.put("OUTSOURCE", "OUTSOURCE");

        for (int i = 0; i < rows.size(); i++) {
            try {
                Map<String, String> row = rows.get(i);
                // 需求8：导入数据中所有项目均为必填项
                String name = notEmpty(row, "人员姓名", i);
                String code = notEmpty(row, "工号", i);
                String dept = notEmpty(row, "部门", i);
                String pos  = notEmpty(row, "岗位", i);
                String wtRaw = notEmpty(row, "用工类型", i, "用工类型(全职/兼职/外包)");
                String wt = workTypeMap.get(wtRaw.trim());
                if (wt == null) throw new IllegalArgumentException("用工类型只能填写「全职/兼职/外包」，实际为：" + wtRaw);
                BigDecimal labor = ratio(notEmpty(row, "人工分摊比例", i, "人工分摊比例(%)"), "人工分摊比例", i);
                BigDecimal expense = ratio(notEmpty(row, "费用分摊比例", i, "费用分摊比例(%)"), "费用分摊比例", i);

                ProjectStaff s = new ProjectStaff();
                s.setProjectId(projectId);
                s.setUserName(name);
                s.setUserCode(code);
                s.setDeptName(dept);
                s.setPosition(pos);
                s.setWorkType(wt);
                s.setLaborCostRatio(labor);
                s.setExpenseRatio(expense);
                toInsert.add(s);
            } catch (Exception ex) {
                errs.add("第" + (i + 2) + "行：" + ex.getMessage());
            }
        }
        Map<String, Object> r = new HashMap<>();
        if (!errs.isEmpty()) {
            // 有错误行则整体回滚（未执行任何插入），返回错误报告
            r.put("ok", 0);
            r.put("fail", errs.size());
            r.put("errors", errs);
            r.put("message", "存在错误行，全部回滚，未导入任何数据");
            ResponseUtil.ok(resp, r);
            return;
        }
        int ok = BaseDao.executeInTransaction(conn -> {
            for (ProjectStaff s : toInsert) {
                dao.insertWithConn(conn, s);
            }
            return toInsert.size();
        });
        r.put("ok", ok); r.put("fail", 0); r.put("errors", errs);
        ResponseUtil.ok(resp, r);
    }

    private static String notEmpty(Map<String, String> row, String field, int rowIdx) throws IllegalArgumentException {
        return notEmpty(row, field, rowIdx, new String[0]);
    }

    private static String notEmpty(Map<String, String> row, String field, int rowIdx, String... altNames) throws IllegalArgumentException {
        String v = row.get(field);
        if (v != null && !v.trim().isEmpty()) return v.trim();
        if (altNames != null) for (String a : altNames) {
            String av = row.get(a);
            if (av != null && !av.trim().isEmpty()) return av.trim();
        }
        throw new IllegalArgumentException("第" + (rowIdx + 2) + "行：" + field + " 必填");
    }

    private static BigDecimal ratio(String v, String field, int rowIdx) {
        try {
            BigDecimal r = new BigDecimal(v);
            if (r.compareTo(BigDecimal.ZERO) < 0 || r.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException(field + " 必须在 0~100 之间");
            }
            return r;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) {
            throw new IllegalArgumentException(field + " 格式不正确：" + v);
        }
    }

    private BigDecimal toBd(Object o) {
        if (o == null) return null;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}

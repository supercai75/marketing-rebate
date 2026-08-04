package com.rebate.servlet;

import com.rebate.dao.ProjectStaffDao;
import com.rebate.model.ProjectStaff;
import com.rebate.model.UserContext;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 项目作业人员管理
 */
@WebServlet("/api/project-staff")
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

    private BigDecimal toBd(Object o) {
        if (o == null) return null;
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}

package com.rebate.servlet;

import com.rebate.dao.UserDao;
import com.rebate.model.SysUser;
import com.rebate.model.UserContext;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 用户管理
 */
public class UserServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        UserContext u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        if (!u.hasPerm("user:view") && !"page".equals(WebUtil.getSafeParam(p, "op"))) {
            ResponseUtil.forbidden(resp); return;
        }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "page";
        switch (op) {
            case "page": doPage(req, resp, p); break;
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p); break;
            case "update": doUpdate(req, resp, p); break;
            case "delete": doDelete(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private void doPage(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        int page = Math.max(1, WebUtil.getInt(p, "page", 1));
        int size = Math.max(1, Math.min(100, WebUtil.getInt(p, "size", 20)));
        String kw = WebUtil.getSafeParam(p, "keyword");
        List<SysUser> rows = userDao.page(kw, page, size);
        long total = userDao.count(kw);
        ResponseUtil.ok(resp, WebUtil.pageResult(page, size, total, rows));
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        SysUser u = userDao.findById(id);
        ResponseUtil.ok(resp, u);
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        SysUser u = new SysUser();
        u.setWorkNo(WebUtil.getSafeParam(p, "workNo"));
        u.setName(WebUtil.getSafeParam(p, "name"));
        u.setLoginName(WebUtil.getSafeParam(p, "loginName"));
        u.setPassword(WebUtil.getSafeParam(p, "password"));
        if (u.getPassword() == null || u.getPassword().isEmpty()) u.setPassword("123456");
        u.setDeptId(WebUtil.getLong(p, "deptId", 0) == 0 ? null : WebUtil.getLong(p, "deptId", 0));
        u.setCompanyId(WebUtil.getLong(p, "companyId", 0) == 0 ? null : WebUtil.getLong(p, "companyId", 0));
        u.setPhone(WebUtil.getSafeParam(p, "phone"));
        u.setEmail(WebUtil.getSafeParam(p, "email"));
        u.setStatus(WebUtil.getInt(p, "status", 1));
        u.setRoleId(WebUtil.getLong(p, "roleId", 0) == 0 ? null : WebUtil.getLong(p, "roleId", 0));
        Long id = userDao.insert(u);
        ResponseUtil.ok(resp, java.util.Collections.singletonMap("id", id));
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        SysUser u = userDao.findById(id);
        if (u == null) { ResponseUtil.fail(resp, "用户不存在"); return; }
        u.setWorkNo(WebUtil.getSafeParam(p, "workNo"));
        u.setName(WebUtil.getSafeParam(p, "name"));
        u.setLoginName(WebUtil.getSafeParam(p, "loginName"));
        u.setDeptId(WebUtil.getLong(p, "deptId", 0) == 0 ? null : WebUtil.getLong(p, "deptId", 0));
        u.setCompanyId(WebUtil.getLong(p, "companyId", 0) == 0 ? null : WebUtil.getLong(p, "companyId", 0));
        u.setPhone(WebUtil.getSafeParam(p, "phone"));
        u.setEmail(WebUtil.getSafeParam(p, "email"));
        u.setStatus(WebUtil.getInt(p, "status", 1));
        u.setRoleId(WebUtil.getLong(p, "roleId", 0) == 0 ? null : WebUtil.getLong(p, "roleId", 0));
        u.setId(id);
        userDao.update(u);
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        userDao.delete(id);
        ResponseUtil.ok(resp);
    }
}

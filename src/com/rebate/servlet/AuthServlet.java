package com.rebate.servlet;

import com.rebate.dao.UserDao;
import com.rebate.model.SysUser;
import com.rebate.model.UserContext;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录/注销/获取当前用户
 */
public class AuthServlet extends BaseServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> params) throws Exception {
        String op = WebUtil.getSafeParam(params, "op");
        if (op == null || op.isEmpty()) op = "current";
        switch (op) {
            case "login": doLogin(req, resp, params); break;
            case "logout": doLogout(req, resp); break;
            case "current": doCurrent(req, resp); break;
            case "changePassword": doChangePwd(req, resp, params); break;
            case "departments": doDepartments(req, resp); break;
            case "companies": doCompanies(req, resp); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String loginName = WebUtil.getSafeParam(p, "loginName");
        String password = WebUtil.getSafeParam(p, "password");
        if (loginName == null || loginName.isEmpty() || password == null || password.isEmpty()) {
            ResponseUtil.fail(resp, "请填写登录名和密码");
            return;
        }
        SysUser u = userDao.findByLoginName(loginName);
        if (u == null || !u.getPassword().equals(password)) {
            ResponseUtil.fail(resp, "用户名或密码错误");
            return;
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            ResponseUtil.fail(resp, "账号已停用");
            return;
        }
        userDao.updateLastLogin(u.getId());
        UserContext ctx = userDao.loadContext(u.getId());
        String token = TokenUtil.createToken(ctx);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", ctx);
        ResponseUtil.ok(resp, data);
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp) {
        TokenUtil.logout(req);
        ResponseUtil.ok(resp);
    }

    private void doCurrent(HttpServletRequest req, HttpServletResponse resp) {
        UserContext u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        ResponseUtil.ok(resp, u);
    }

    private void doChangePwd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        UserContext u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        String oldPwd = WebUtil.getSafeParam(p, "oldPassword");
        String newPwd = WebUtil.getSafeParam(p, "newPassword");
        SysUser user = userDao.findById(u.getId());
        if (user == null || !user.getPassword().equals(oldPwd)) {
            ResponseUtil.fail(resp, "原密码错误");
            return;
        }
        userDao.updatePassword(u.getId(), newPwd);
        ResponseUtil.ok(resp);
    }

    private void doDepartments(HttpServletRequest req, HttpServletResponse resp) {
        List<Map<String, Object>> depts = userDao.listDepartments();
        ResponseUtil.ok(resp, depts);
    }

    private void doCompanies(HttpServletRequest req, HttpServletResponse resp) {
        List<Map<String, Object>> companies = userDao.listCompanies();
        ResponseUtil.ok(resp, companies);
    }
}

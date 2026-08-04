package com.rebate.servlet;

import com.rebate.model.SysRole;
import com.rebate.model.SysPermission;
import com.rebate.model.UserContext;
import com.rebate.dao.RoleDao;
import com.rebate.util.ResponseUtil;
import com.rebate.util.TokenUtil;
import com.rebate.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
public class RoleServlet extends BaseServlet {

    private final RoleDao dao = new RoleDao();

    @Override
    protected void doAction(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) throws Exception {
        var u = TokenUtil.getLoginUser(req, UserContext.class);
        if (u == null) { ResponseUtil.unauthorized(resp); return; }
        if (!u.isAdmin()) {
            ResponseUtil.forbidden(resp);
            return;
        }
        String op = WebUtil.getSafeParam(p, "op");
        if (op == null) op = "list";
        switch (op) {
            case "list": doList(req, resp); break;
            case "get": doGet(req, resp, p); break;
            case "add": doAdd(req, resp, p); break;
            case "update": doUpdate(req, resp, p); break;
            case "delete": doDelete(req, resp, p); break;
            case "getPermissions": doGetPermissions(req, resp, p); break;
            case "updatePermissions": doUpdatePermissions(req, resp, p); break;
            default: ResponseUtil.fail(resp, "未知操作: " + op);
        }
    }

    private void doList(HttpServletRequest req, HttpServletResponse resp) {
        ResponseUtil.ok(resp, dao.listAll());
    }

    private void doGet(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        SysRole role = dao.findById(id);
        if (role == null) { ResponseUtil.fail(resp, "角色不存在"); return; }
        ResponseUtil.ok(resp, role);
    }

    private void doAdd(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        String code = WebUtil.getSafeParam(p, "roleCode");
        String name = WebUtil.getSafeParam(p, "roleName");
        String desc = WebUtil.getSafeParam(p, "description");
        if (code == null || code.isEmpty()) { ResponseUtil.fail(resp, "角色编码必填"); return; }
        if (name == null || name.isEmpty()) { ResponseUtil.fail(resp, "角色名称必填"); return; }
        
        SysRole role = new SysRole();
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription(desc);
        
        Long id = dao.insert(role);
        
        String permIds = WebUtil.getSafeParam(p, "permIds");
        if (permIds != null && !permIds.isEmpty()) {
            String[] ids = permIds.split(",");
            for (String pid : ids) {
                try {
                    dao.grantPermission(id, Long.parseLong(pid.trim()));
                } catch (Exception ignore) {}
            }
        }
        
        ResponseUtil.ok(resp, id);
    }

    private void doUpdate(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        if (id <= 0) { ResponseUtil.fail(resp, "ID错误"); return; }
        
        String code = WebUtil.getSafeParam(p, "roleCode");
        String name = WebUtil.getSafeParam(p, "roleName");
        String desc = WebUtil.getSafeParam(p, "description");
        if (code == null || code.isEmpty()) { ResponseUtil.fail(resp, "角色编码必填"); return; }
        if (name == null || name.isEmpty()) { ResponseUtil.fail(resp, "角色名称必填"); return; }
        
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription(desc);
        
        dao.update(role);
        
        String permIds = WebUtil.getSafeParam(p, "permIds");
        dao.revokeAllPermissions(id);
        if (permIds != null && !permIds.isEmpty()) {
            String[] ids = permIds.split(",");
            for (String pid : ids) {
                try {
                    dao.grantPermission(id, Long.parseLong(pid.trim()));
                } catch (Exception ignore) {}
            }
        }
        
        ResponseUtil.ok(resp);
    }

    private void doDelete(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long id = WebUtil.getLong(p, "id", 0);
        if (id <= 0) { ResponseUtil.fail(resp, "ID错误"); return; }
        dao.delete(id);
        ResponseUtil.ok(resp);
    }

    private void doGetPermissions(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long roleId = WebUtil.getLong(p, "roleId", 0);
        
        List<SysPermission> allPerms = dao.listAllPermissions();
        List<Long> rolePerms = dao.listRolePermissionIds(roleId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("allPerms", allPerms);
        result.put("rolePerms", rolePerms);
        
        ResponseUtil.ok(resp, result);
    }

    private void doUpdatePermissions(HttpServletRequest req, HttpServletResponse resp, Map<String, Object> p) {
        long roleId = WebUtil.getLong(p, "roleId", 0);
        String permIds = WebUtil.getSafeParam(p, "permIds");
        
        dao.revokeAllPermissions(roleId);
        if (permIds != null && !permIds.isEmpty()) {
            String[] ids = permIds.split(",");
            for (String pid : ids) {
                try {
                    dao.grantPermission(roleId, Long.parseLong(pid.trim()));
                } catch (Exception ignore) {}
            }
        }
        
        ResponseUtil.ok(resp);
    }
}

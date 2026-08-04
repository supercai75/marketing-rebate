package com.rebate.dao;

import com.rebate.model.SysRole;
import com.rebate.model.SysPermission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 角色 DAO
 */
public class RoleDao {

    private SysRole map(ResultSet rs) throws SQLException {
        SysRole r = new SysRole();
        r.setId(rs.getLong("id"));
        r.setRoleCode(rs.getString("role_code"));
        r.setRoleName(rs.getString("role_name"));
        r.setDescription(rs.getString("description"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }

    public List<SysRole> listAll() {
        return BaseDao.query("SELECT * FROM sys_role ORDER BY id", this::map);
    }

    public SysRole findById(Long id) {
        return BaseDao.queryOne("SELECT * FROM sys_role WHERE id=?", this::map, id);
    }

    public SysRole findByCode(String code) {
        return BaseDao.queryOne("SELECT * FROM sys_role WHERE role_code=?", this::map, code);
    }

    public Long insert(SysRole role) {
        String sql = "INSERT INTO sys_role(role_code, role_name, description) VALUES(?, ?, ?)";
        return BaseDao.insertReturnId(sql, role.getRoleCode(), role.getRoleName(), role.getDescription());
    }

    public int update(SysRole role) {
        String sql = "UPDATE sys_role SET role_code=?, role_name=?, description=? WHERE id=?";
        return BaseDao.update(sql, role.getRoleCode(), role.getRoleName(), role.getDescription(), role.getId());
    }

    public int delete(Long id) {
        return BaseDao.update("DELETE FROM sys_role WHERE id=?", id);
    }

    public List<String> getPermCodesByRole(Long roleId) {
        String sql = "SELECT p.perm_code FROM sys_role_permission rp JOIN sys_permission p ON rp.perm_id=p.id WHERE rp.role_id=?";
        return BaseDao.query(sql, rs -> rs.getString("perm_code"), roleId);
    }

    public List<String> getPermCodesByUser(Long userId) {
        String sql = "SELECT DISTINCT p.perm_code FROM sys_user_role ur " +
                "JOIN sys_role_permission rp ON ur.role_id=rp.role_id " +
                "JOIN sys_permission p ON rp.perm_id=p.id WHERE ur.user_id=?";
        return BaseDao.query(sql, rs -> rs.getString("perm_code"), userId);
    }

    public int addRolePermission(Long roleId, Long permId) {
        return BaseDao.update("INSERT INTO sys_role_permission(role_id, perm_id) VALUES(?, ?)", roleId, permId);
    }

    public int removeRolePermission(Long roleId, Long permId) {
        return BaseDao.update("DELETE FROM sys_role_permission WHERE role_id=? AND perm_id=?", roleId, permId);
    }

    public int addUserRole(Long userId, Long roleId) {
        return BaseDao.update("INSERT INTO sys_user_role(user_id, role_id) VALUES(?, ?)", userId, roleId);
    }

    public int removeUserRole(Long userId, Long roleId) {
        return BaseDao.update("DELETE FROM sys_user_role WHERE user_id=? AND role_id=?", userId, roleId);
    }

    private SysPermission mapPermission(ResultSet rs) throws SQLException {
        SysPermission p = new SysPermission();
        p.setId(rs.getLong("id"));
        p.setPermCode(rs.getString("perm_code"));
        p.setPermName(rs.getString("perm_name"));
        p.setModule(rs.getString("module"));
        p.setDescription(rs.getString("description"));
        return p;
    }

    public List<SysPermission> listAllPermissions() {
        return BaseDao.query("SELECT * FROM sys_permission ORDER BY module, id", this::mapPermission);
    }

    public List<Long> listRolePermissionIds(Long roleId) {
        return BaseDao.query("SELECT perm_id FROM sys_role_permission WHERE role_id=?", rs -> rs.getLong("perm_id"), roleId);
    }

    public void grantPermission(Long roleId, Long permId) {
        BaseDao.update("INSERT INTO sys_role_permission(role_id, perm_id) VALUES(?, ?)", roleId, permId);
    }

    public void revokeAllPermissions(Long roleId) {
        BaseDao.update("DELETE FROM sys_role_permission WHERE role_id=?", roleId);
    }
}

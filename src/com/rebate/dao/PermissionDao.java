package com.rebate.dao;

import com.rebate.model.SysPermission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 权限 DAO
 */
public class PermissionDao {

    private SysPermission map(ResultSet rs) throws SQLException {
        SysPermission p = new SysPermission();
        p.setId(rs.getLong("id"));
        p.setPermCode(rs.getString("perm_code"));
        p.setPermName(rs.getString("perm_name"));
        p.setModule(rs.getString("module"));
        p.setDescription(rs.getString("description"));
        return p;
    }

    public List<SysPermission> listAll() {
        return BaseDao.query("SELECT * FROM sys_permission ORDER BY module, id", this::map);
    }

    public List<SysPermission> listByModule(String module) {
        return BaseDao.query("SELECT * FROM sys_permission WHERE module=? ORDER BY id", this::map, module);
    }

    public SysPermission findById(Long id) {
        return BaseDao.queryOne("SELECT * FROM sys_permission WHERE id=?", this::map, id);
    }

    public SysPermission findByCode(String code) {
        return BaseDao.queryOne("SELECT * FROM sys_permission WHERE perm_code=?", this::map, code);
    }

    public Long insert(SysPermission perm) {
        String sql = "INSERT INTO sys_permission(perm_code, perm_name, module, description) VALUES(?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, perm.getPermCode(), perm.getPermName(), perm.getModule(), perm.getDescription());
    }

    public int update(SysPermission perm) {
        String sql = "UPDATE sys_permission SET perm_code=?, perm_name=?, module=?, description=? WHERE id=?";
        return BaseDao.update(sql, perm.getPermCode(), perm.getPermName(), perm.getModule(), perm.getDescription(), perm.getId());
    }

    public int delete(Long id) {
        return BaseDao.update("DELETE FROM sys_permission WHERE id=?", id);
    }
}

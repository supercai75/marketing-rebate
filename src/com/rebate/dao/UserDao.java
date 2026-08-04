package com.rebate.dao;

import com.rebate.model.SysUser;
import com.rebate.model.UserContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户 DAO
 */
public class UserDao {

    public SysUser findByLoginName(String loginName) {
        String sql = "SELECT * FROM sys_user WHERE login_name = ?";
        return BaseDao.queryOne(sql, this::mapUser, loginName);
    }

    public SysUser findById(long id) {
        return BaseDao.queryOne("SELECT * FROM sys_user WHERE id=?", this::mapUser, id);
    }

    public List<SysUser> page(String keyword, int page, int size) {
        String sql = "SELECT u.*, d.dept_name, c.company_name FROM sys_user u " +
                "LEFT JOIN sys_department d ON u.dept_id=d.id " +
                "LEFT JOIN sys_company c ON u.company_id=c.id " +
                "WHERE (? = '' OR u.name LIKE ? OR u.work_no LIKE ? OR u.login_name LIKE ?) " +
                "ORDER BY u.id DESC LIMIT ? OFFSET ?";
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        int offset = (page - 1) * size;
        return BaseDao.query(sql, this::mapUserWithDept, kw, kw, kw, kw, size, offset);
    }

    public long count(String keyword) {
        String sql = "SELECT COUNT(*) FROM sys_user WHERE (?='' OR name LIKE ? OR work_no LIKE ? OR login_name LIKE ?)";
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        return BaseDao.count(sql, kw, kw, kw, kw);
    }

    public Long insert(SysUser u) {
        String sql = "INSERT INTO sys_user(work_no, name, login_name, password, dept_id, company_id, phone, email, status, is_admin, role_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, u.getWorkNo(), u.getName(), u.getLoginName(), u.getPassword(),
                u.getDeptId(), u.getCompanyId(), u.getPhone(), u.getEmail(), u.getStatus(), u.getIsAdmin(), u.getRoleId());
    }

    public int update(SysUser u) {
        return BaseDao.update("UPDATE sys_user SET work_no=?, name=?, login_name=?, dept_id=?, company_id=?, phone=?, email=?, status=?, is_admin=?, role_id=? WHERE id=?",
                u.getWorkNo(), u.getName(), u.getLoginName(), u.getDeptId(), u.getCompanyId(), u.getPhone(), u.getEmail(), u.getStatus(), u.getIsAdmin(), u.getRoleId(), u.getId());
    }

    public int delete(long id) {
        return BaseDao.update("DELETE FROM sys_user WHERE id=?", id);
    }

    public int updatePassword(long id, String newPwd) {
        return BaseDao.update("UPDATE sys_user SET password=? WHERE id=?", newPwd, id);
    }

    public int updateLastLogin(long id) {
        return BaseDao.update("UPDATE sys_user SET last_login_time=? WHERE id=?", new Timestamp(System.currentTimeMillis()), id);
    }

    public UserContext loadContext(long userId) {
        SysUser u = findById(userId);
        if (u == null) return null;
        UserContext ctx = new UserContext();
        ctx.setId(u.getId());
        ctx.setWorkNo(u.getWorkNo());
        ctx.setName(u.getName());
        ctx.setLoginName(u.getLoginName());
        ctx.setDeptId(u.getDeptId());
        ctx.setCompanyId(u.getCompanyId());
        ctx.setIsAdmin(u.getIsAdmin());

        Set<String> perms = new HashSet<>();
        Set<String> roles = new HashSet<>();
        if (u.getIsAdmin() != null && u.getIsAdmin() == 1) {
            // admin 拥有所有权限（前端控制）
        } else {
            // 从用户直接关联的 role_id 加载权限
            if (u.getRoleId() != null) {
                String sql = "SELECT p.perm_code, r.role_code FROM sys_role r " +
                        "LEFT JOIN sys_role_permission rp ON rp.role_id=r.id " +
                        "LEFT JOIN sys_permission p ON p.id=rp.perm_id " +
                        "WHERE r.id=?";
                BaseDao.query(sql, rs -> {
                    if (rs.getString("perm_code") != null) perms.add(rs.getString("perm_code"));
                    if (rs.getString("role_code") != null) roles.add(rs.getString("role_code"));
                    return null;
                }, u.getRoleId());
            }
            // 同时也兼容 sys_user_role 表的方式
            String sql2 = "SELECT p.perm_code, r.role_code FROM sys_user_role ur " +
                    "JOIN sys_role r ON ur.role_id=r.id " +
                    "LEFT JOIN sys_role_permission rp ON rp.role_id=r.id " +
                    "LEFT JOIN sys_permission p ON p.id=rp.perm_id " +
                    "WHERE ur.user_id=?";
            BaseDao.query(sql2, rs -> {
                if (rs.getString("perm_code") != null) perms.add(rs.getString("perm_code"));
                if (rs.getString("role_code") != null) roles.add(rs.getString("role_code"));
                return null;
            }, userId);
        }
        ctx.setPermCodes(perms);
        ctx.setRoleCodes(roles);
        return ctx;
    }

    private SysUser mapUser(ResultSet rs) throws SQLException {
        SysUser u = new SysUser();
        u.setId(rs.getLong("id"));
        u.setWorkNo(rs.getString("work_no"));
        u.setName(rs.getString("name"));
        u.setLoginName(rs.getString("login_name"));
        u.setPassword(rs.getString("password"));
        u.setDeptId(rs.getObject("dept_id") == null ? null : rs.getLong("dept_id"));
        u.setCompanyId(rs.getObject("company_id") == null ? null : rs.getLong("company_id"));
        u.setPhone(rs.getString("phone"));
        u.setEmail(rs.getString("email"));
        u.setStatus(rs.getInt("status"));
        u.setIsAdmin(rs.getInt("is_admin"));
        u.setRoleId(rs.getObject("role_id") == null ? null : rs.getLong("role_id"));
        u.setLastLoginTime(rs.getTimestamp("last_login_time"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }

    private SysUser mapUserWithDept(ResultSet rs) throws SQLException {
        SysUser u = mapUser(rs);
        u.setDeptName(rs.getString("dept_name"));
        u.setCompanyName(rs.getString("company_name"));
        return u;
    }

    public List<Map<String, Object>> listDepartments() {
        String sql = "SELECT id, dept_code, dept_name FROM sys_department WHERE status=1 ORDER BY sort_no, id";
        List<Map<String, Object>> result = new ArrayList<>();
        BaseDao.query(sql, rs -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("code", rs.getString("dept_code"));
            item.put("name", rs.getString("dept_name"));
            result.add(item);
            return null;
        });
        return result;
    }

    public List<Map<String, Object>> listCompanies() {
        String sql = "SELECT id, company_code, company_name FROM sys_company WHERE status=1 ORDER BY id";
        List<Map<String, Object>> result = new ArrayList<>();
        BaseDao.query(sql, rs -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", rs.getLong("id"));
            item.put("code", rs.getString("company_code"));
            item.put("name", rs.getString("company_name"));
            result.add(item);
            return null;
        });
        return result;
    }
}

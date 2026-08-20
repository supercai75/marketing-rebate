package com.rebate.dao;

import com.rebate.config.AppConfig;
import com.rebate.model.Project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目 DAO
 */
public class ProjectDao {

    private static final String BASE_SELECT = "SELECT p.*, ou.name AS owner_name, cu.name AS created_by_name, g.name AS project_group_name FROM prj_project p " +
            "LEFT JOIN sys_user ou ON p.owner_user_id=ou.id " +
            "LEFT JOIN sys_user cu ON p.created_by=cu.id " +
            "LEFT JOIN prj_project_group g ON p.project_group_id = g.id ";

    public Long insert(Project p) {
        String sql = "INSERT INTO prj_project(project_code, project_name, brand, co_product, co_mode, co_year, " +
                "period_start_date, period_end_date, region, target_scale, expected_rebate, expected_cost, " +
                "description, bpm_process_id, bpm_project_id, bpm_synced, " +
                "status, owner_user_id, created_by, project_group_id, undertaking_dept) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, p.getProjectCode(), p.getProjectName(), p.getBrand(), p.getCoProduct(), p.getCoMode(),
                p.getCoYear(), p.getPeriodStartDate(), p.getPeriodEndDate(), p.getRegion(), p.getTargetScale(),
                p.getExpectedRebate(), p.getExpectedCost(),
                p.getDescription(), p.getBpmProcessId(), p.getBpmProjectId(), p.getBpmSynced(),
                p.getStatus(), p.getOwnerUserId(), p.getCreatedBy(), p.getProjectGroupId(), p.getUndertakingDept());
    }

    public Long insertWithConn(Connection conn, Project p) throws SQLException {
        String sql = "INSERT INTO prj_project(project_code, project_name, brand, co_product, co_mode, co_year, " +
                "period_start_date, period_end_date, region, target_scale, expected_rebate, expected_cost, " +
                "description, bpm_process_id, bpm_project_id, bpm_synced, " +
                "status, owner_user_id, created_by, project_group_id, undertaking_dept) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnIdWithConn(conn, sql, p.getProjectCode(), p.getProjectName(), p.getBrand(), p.getCoProduct(), p.getCoMode(),
                p.getCoYear(), p.getPeriodStartDate(), p.getPeriodEndDate(), p.getRegion(), p.getTargetScale(),
                p.getExpectedRebate(), p.getExpectedCost(),
                p.getDescription(), p.getBpmProcessId(), p.getBpmProjectId(), p.getBpmSynced(),
                p.getStatus(), p.getOwnerUserId(), p.getCreatedBy(), p.getProjectGroupId(), p.getUndertakingDept());
    }

    public int update(Project p) {
        String sql = "UPDATE prj_project SET project_code=?, project_name=?, brand=?, co_product=?, co_mode=?, " +
                "co_year=?, period_start_date=?, period_end_date=?, region=?, target_scale=?, " +
                "expected_rebate=?, expected_cost=?, description=?, " +
                "status=?, owner_user_id=?, project_group_id=?, undertaking_dept=? WHERE id=?";
        return BaseDao.update(sql, p.getProjectCode(), p.getProjectName(), p.getBrand(), p.getCoProduct(), p.getCoMode(),
                p.getCoYear(), p.getPeriodStartDate(), p.getPeriodEndDate(), p.getRegion(), p.getTargetScale(),
                p.getExpectedRebate(), p.getExpectedCost(), p.getDescription(),
                p.getStatus(), p.getOwnerUserId(), p.getProjectGroupId(), p.getUndertakingDept(), p.getId());
    }

    public int updateStatus(long id, String status) {
        return BaseDao.update("UPDATE prj_project SET status=? WHERE id=?", status, id);
    }

    public int delete(long id) {
        return BaseDao.update("DELETE FROM prj_project WHERE id=?", id);
    }

    public Project findById(long id) {
        return BaseDao.queryOne(BASE_SELECT + " WHERE p.id=?", this::map, id);
    }

    public Project findByBpmId(String bpmId) {
        return BaseDao.queryOne(BASE_SELECT + " WHERE p.bpm_project_id=?", this::map, bpmId);
    }

    public List<Project> page(String keyword, String status, String coYear, Long projectGroupId, int page, int size) {
        int offset = (page - 1) * size;
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        String sql = BASE_SELECT + " WHERE (? = '' OR p.project_name LIKE ? OR p.brand LIKE ? OR p.project_code LIKE ?) " +
                "AND (? = '' OR p.status = ?) " +
                "AND (? = '' OR p.co_year = ?) " +
                "AND (CAST(? AS BIGINT) IS NULL OR p.project_group_id = ?) " +
                "ORDER BY p.id DESC LIMIT ? OFFSET ?";
        return BaseDao.query(sql, this::map, kw, kw, kw, kw,
                status == null ? "" : status, status == null ? "" : status,
                coYear == null ? "" : coYear, coYear == null ? "" : coYear,
                projectGroupId, projectGroupId,
                size, offset);
    }

    public long count(String keyword, String status, String coYear, Long projectGroupId) {
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        String sql = "SELECT COUNT(*) FROM prj_project p WHERE (? = '' OR p.project_name LIKE ? OR p.brand LIKE ? OR p.project_code LIKE ?) " +
                "AND (? = '' OR p.status = ?) " +
                "AND (? = '' OR p.co_year = ?) " +
                "AND (CAST(? AS BIGINT) IS NULL OR p.project_group_id = ?)";
        return BaseDao.count(sql, kw, kw, kw, kw,
                status == null ? "" : status, status == null ? "" : status,
                coYear == null ? "" : coYear, coYear == null ? "" : coYear,
                projectGroupId, projectGroupId);
    }

    public List<Project> listAll() {
        return listByFilters(null, null, null, null, null);
    }

    public List<Project> listByFilters(String coYear, Long projectGroupId, String keyword, String status, String undertakingDept) {
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE 1=1 ");
        if (coYear != null && !coYear.isEmpty()) {
            sql.append(" AND p.co_year = ? ");
            args.add(coYear);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND p.status = ? ");
            args.add(status);
        }
        if (projectGroupId != null && projectGroupId > 0) {
            sql.append(" AND p.project_group_id = ? ");
            args.add(projectGroupId);
        }
        if (undertakingDept != null && !undertakingDept.isEmpty()) {
            sql.append(" AND p.undertaking_dept = ? ");
            args.add(undertakingDept);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (p.project_name LIKE ? OR p.brand LIKE ? OR p.project_code LIKE ?) ");
            args.add(kw); args.add(kw); args.add(kw);
        }
        sql.append(" ORDER BY p.id DESC");
        return BaseDao.query(sql.toString(), this::map, args.toArray());
    }

    public List<Project> listByYear(String coYear) {
        return listByFilters(coYear, null, null, null, null);
    }

    /**
     * 获取所有项目年度（倒序）
     */
    public java.util.List<String> listAllYears() {
        String sql = "SELECT DISTINCT co_year FROM prj_project WHERE co_year IS NOT NULL AND co_year != '' ORDER BY co_year DESC";
        return BaseDao.query(sql, (ResultSet rs) -> rs.getString("co_year"));
    }

    /**
     * 获取所有承接部门（去重，按名称排序）
     */
    public java.util.List<String> listAllUndertakingDepts() {
        String sql = "SELECT DISTINCT undertaking_dept FROM prj_project WHERE undertaking_dept IS NOT NULL AND undertaking_dept != '' ORDER BY undertaking_dept";
        return BaseDao.query(sql, (ResultSet rs) -> rs.getString("undertaking_dept"));
    }

    public List<Project> findByNameAndYear(String projectName, String coYear) {
        String sql = BASE_SELECT + " WHERE p.project_name = ? AND p.co_year = ? ORDER BY id DESC";
        return BaseDao.query(sql, this::map, projectName, coYear);
    }

    /**
     * 按项目编号查询本地项目
     */
    public Project findByProjectCode(String projectCode) {
        if (projectCode == null || projectCode.isEmpty()) return null;
        return BaseDao.queryOne(BASE_SELECT + " WHERE p.project_code = ?", this::map, projectCode);
    }

    // ============== 项目分组字典 ==============

    public List<Map<String, Object>> listGroups() {
        String sql = "SELECT id, name FROM prj_project_group ORDER BY name";
        return BaseDao.query(sql, (ResultSet rs) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("name", rs.getString("name"));
            return m;
        });
    }

    /**
     * 如果 name 存在 -> 返回 id；不存在则插入后返回新 id。空串返回 null
     */
    public Long ensureGroup(String name, Long userId) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;
        Long existing = BaseDao.queryOne("SELECT id FROM prj_project_group WHERE name = ?",
                (ResultSet rs) -> rs.getLong("id"), n);
        if (existing != null) return existing;
        return BaseDao.insertReturnId("INSERT INTO prj_project_group(name, created_by) VALUES (?, ?)",
                n, userId);
    }

    /**
     * 事务内版本：如果 name 存在 -> 返回 id；不存在则插入后返回新 id。空串返回 null
     */
    public Long ensureGroupWithConn(Connection conn, String name, Long userId) throws SQLException {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;
        Long existing = BaseDao.queryOneWithConn(conn, "SELECT id FROM prj_project_group WHERE name = ?",
                (ResultSet rs) -> rs.getLong("id"), n);
        if (existing != null) return existing;
        return BaseDao.insertReturnIdWithConn(conn, "INSERT INTO prj_project_group(name, created_by) VALUES (?, ?)",
                n, userId);
    }

    /**
     * 从BPM(Oracle)数据库查询近一年的立项列表
     * @param projectNameFilter 项目名称筛选（模糊匹配，可为空）
     */
    public List<Map<String, Object>> listBpmProjects(String projectNameFilter) {
        String url = AppConfig.get("bpm.jdbc.url");
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("BPM数据库未配置(bpm.jdbc.url)");
        }
        // 加载 Oracle 驱动
        String driver = AppConfig.get("bpm.jdbc.driver");
        if (driver != null && !driver.isEmpty()) {
            try { Class.forName(driver); } catch (ClassNotFoundException e) {
                throw new RuntimeException("Oracle JDBC驱动未找到: " + e.getMessage(), e);
            }
        }

        // 1. 先从本地查最近1年内已引入的 BPM流程实例ID
        List<String> importedIds = listImportedBpmProcessIds();

        // 2. 构造 Oracle SQL
        StringBuilder sql = new StringBuilder(
                "SELECT ID as bpm_process_id, PROJECT_CODE, PROJECT_NAME, " +
                "SIGN_FACTORY_BRAND, COOP_BREED, COOP_MODE, COOP_PERIOD, " +
                "COOP_BEGIN_TIME, COOP_END_TIME, COVER_AREA, " +
                "PROJECT_TARGET, EXPECT_PROJECT_INCOME, EXPECT_MONEY, PROJECT_CONTENT " +
                "FROM BO_EU_MARKET_APPROVAL " +
                "WHERE STATUS = 2 AND OVER_DATE > sysDate - 365 ");
        List<Object> params = new ArrayList<>();
        if (projectNameFilter != null && !projectNameFilter.isEmpty()) {
            sql.append("AND PROJECT_NAME LIKE ? ");
            params.add("%" + projectNameFilter + "%");
        }
        if (!importedIds.isEmpty()) {
            String placeholders = String.join(",", java.util.Collections.nCopies(importedIds.size(), "?"));
            sql.append("AND ID NOT IN (").append(placeholders).append(") ");
            params.addAll(importedIds);
        }
        sql.append("ORDER BY OVER_DATE DESC");

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("bpmProcessId", rs.getString("bpm_process_id"));
                m.put("projectCode", rs.getString("PROJECT_CODE"));
                m.put("projectName", rs.getString("PROJECT_NAME"));
                m.put("brand", rs.getString("SIGN_FACTORY_BRAND"));
                m.put("coProduct", rs.getString("COOP_BREED"));
                m.put("coMode", rs.getString("COOP_MODE"));
                m.put("coYear", rs.getString("COOP_PERIOD"));
                m.put("periodStartDate", rs.getDate("COOP_BEGIN_TIME") == null ? null : rs.getDate("COOP_BEGIN_TIME").toString());
                m.put("periodEndDate", rs.getDate("COOP_END_TIME") == null ? null : rs.getDate("COOP_END_TIME").toString());
                m.put("region", rs.getString("COVER_AREA"));
                m.put("targetScale", rs.getBigDecimal("PROJECT_TARGET"));
                m.put("expectedRebate", rs.getBigDecimal("EXPECT_PROJECT_INCOME"));
                m.put("expectedCost", rs.getBigDecimal("EXPECT_MONEY"));
                m.put("description", rs.getString("PROJECT_CONTENT"));
                list.add(m);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("从BPM(Oracle)获取立项数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询本地最近1年内已引入的 BPM流程实例ID 列表（用于排除重复引入）
     */
    public List<String> listImportedBpmProcessIds() {
        return BaseDao.query(
                "SELECT DISTINCT bpm_process_id FROM prj_project " +
                "WHERE bpm_process_id IS NOT NULL AND bpm_process_id != '' " +
                "AND created_at > NOW() - INTERVAL '1 year'",
                rs -> rs.getString("bpm_process_id"));
    }

    private Project map(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getLong("id"));
        p.setProjectCode(rs.getString("project_code"));
        p.setProjectName(rs.getString("project_name"));
        p.setBrand(rs.getString("brand"));
        p.setCoProduct(rs.getString("co_product"));
        p.setCoMode(rs.getString("co_mode"));
        p.setCoYear(rs.getString("co_year"));
        p.setPeriodStartDate(rs.getDate("period_start_date"));
        p.setPeriodEndDate(rs.getDate("period_end_date"));
        p.setRegion(rs.getString("region"));
        p.setTargetScale(BaseDao.toBigDecimal(rs.getObject("target_scale")));
        p.setExpectedRebate(BaseDao.toBigDecimal(rs.getObject("expected_rebate")));
        p.setExpectedCost(BaseDao.toBigDecimal(rs.getObject("expected_cost")));
        p.setDescription(rs.getString("description"));
        p.setBpmProcessId(rs.getString("bpm_process_id"));
        p.setBpmProjectId(rs.getString("bpm_project_id"));
        p.setBpmSynced(rs.getInt("bpm_synced"));
        p.setStatus(rs.getString("status"));
        p.setOwnerUserId(rs.getObject("owner_user_id") == null ? null : rs.getLong("owner_user_id"));
        p.setCreatedBy(rs.getObject("created_by") == null ? null : rs.getLong("created_by"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        p.setProjectGroupId(rs.getObject("project_group_id") == null ? null : rs.getLong("project_group_id"));
        p.setUndertakingDept(rs.getString("undertaking_dept"));
        try { p.setOwnerName(rs.getString("owner_name")); } catch (Exception ignore) {}
        try { p.setCreatedByName(rs.getString("created_by_name")); } catch (Exception ignore) {}
        try { p.setProjectGroupName(rs.getString("project_group_name")); } catch (Exception ignore) {}
        return p;
    }
}

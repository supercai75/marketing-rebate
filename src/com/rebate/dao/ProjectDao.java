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

    private static final String BASE_SELECT = "SELECT p.*, ou.name AS owner_name, cu.name AS created_by_name FROM prj_project p " +
            "LEFT JOIN sys_user ou ON p.owner_user_id=ou.id " +
            "LEFT JOIN sys_user cu ON p.created_by=cu.id ";

    public Long insert(Project p) {
        String sql = "INSERT INTO prj_project(project_code, project_name, brand, co_product, co_mode, co_year, " +
                "period_start_date, period_end_date, region, target_scale, expected_rebate, expected_cost, " +
                "description, bpm_process_id, bpm_project_id, bpm_synced, " +
                "status, owner_user_id, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, p.getProjectCode(), p.getProjectName(), p.getBrand(), p.getCoProduct(), p.getCoMode(),
                p.getCoYear(), p.getPeriodStartDate(), p.getPeriodEndDate(), p.getRegion(), p.getTargetScale(),
                p.getExpectedRebate(), p.getExpectedCost(),
                p.getDescription(), p.getBpmProcessId(), p.getBpmProjectId(), p.getBpmSynced(),
                p.getStatus(), p.getOwnerUserId(), p.getCreatedBy());
    }

    public int update(Project p) {
        String sql = "UPDATE prj_project SET project_code=?, project_name=?, brand=?, co_product=?, co_mode=?, " +
                "co_year=?, period_start_date=?, period_end_date=?, region=?, target_scale=?, " +
                "expected_rebate=?, expected_cost=?, description=?, " +
                "status=?, owner_user_id=? WHERE id=?";
        return BaseDao.update(sql, p.getProjectCode(), p.getProjectName(), p.getBrand(), p.getCoProduct(), p.getCoMode(),
                p.getCoYear(), p.getPeriodStartDate(), p.getPeriodEndDate(), p.getRegion(), p.getTargetScale(),
                p.getExpectedRebate(), p.getExpectedCost(), p.getDescription(),
                p.getStatus(), p.getOwnerUserId(), p.getId());
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

    public List<Project> page(String keyword, String status, String coYear, int page, int size) {
        int offset = (page - 1) * size;
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        String sql = BASE_SELECT + " WHERE (? = '' OR p.project_name LIKE ? OR p.brand LIKE ? OR p.project_code LIKE ?) " +
                "AND (? = '' OR p.status = ?) " +
                "AND (? = '' OR p.co_year = ?) ORDER BY p.id DESC LIMIT ? OFFSET ?";
        return BaseDao.query(sql, this::map, kw, kw, kw, kw, status == null ? "" : status, status == null ? "" : status,
                coYear == null ? "" : coYear, coYear == null ? "" : coYear, size, offset);
    }

    public long count(String keyword, String status, String coYear) {
        String kw = "%" + (keyword == null ? "" : keyword) + "%";
        String sql = "SELECT COUNT(*) FROM prj_project p WHERE (? = '' OR p.project_name LIKE ? OR p.brand LIKE ? OR p.project_code LIKE ?) " +
                "AND (? = '' OR p.status = ?) " +
                "AND (? = '' OR p.co_year = ?)";
        return BaseDao.count(sql, kw, kw, kw, kw, status == null ? "" : status, status == null ? "" : status,
                coYear == null ? "" : coYear, coYear == null ? "" : coYear);
    }

    public List<Project> listAll() {
        return BaseDao.query(BASE_SELECT + " ORDER BY id DESC", this::map);
    }
    
    public List<Project> listByYear(String coYear) {
        if (coYear == null || coYear.isEmpty()) {
            return listAll();
        }
        String sql = BASE_SELECT + " WHERE p.co_year = ? ORDER BY id DESC";
        return BaseDao.query(sql, this::map, coYear);
    }

    /**
     * 获取所有项目年度（倒序）
     */
    public java.util.List<String> listAllYears() {
        String sql = "SELECT DISTINCT co_year FROM prj_project WHERE co_year IS NOT NULL AND co_year != '' ORDER BY co_year DESC";
        return BaseDao.query(sql, (ResultSet rs) -> rs.getString("co_year"));
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

    /**
     * 从BPM数据库查询近一年的立项列表（供用户选择引入）
     */
    public List<Map<String, Object>> listBpmProjects() {
        String url = AppConfig.get("bpm.jdbc.url");
        if (url == null || url.isEmpty()) {
            throw new RuntimeException("BPM数据库未配置(bpm.jdbc.url)");
        }
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT 项目编号, 项目名称, 签约厂牌, 合作品种, 合作模式, 合作年度, " +
                "起始日期, 终止日期, 覆盖地区, 项目目标规模, 预计收益返利金额, 预计费用, 项目信息简述 " +
                "FROM rebate_bpm_project WHERE createdTime > SYSDATE - 365 ORDER BY createdTime DESC";
        try (Connection c = DriverManager.getConnection(url,
                AppConfig.get("bpm.jdbc.username"), AppConfig.get("bpm.jdbc.password"));
             PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("projectCode", rs.getString("项目编号"));
                m.put("projectName", rs.getString("项目名称"));
                m.put("brand", rs.getString("签约厂牌"));
                m.put("coProduct", rs.getString("合作品种"));
                m.put("coMode", rs.getString("合作模式"));
                m.put("coYear", rs.getString("合作年度"));
                m.put("periodStartDate", rs.getDate("起始日期") == null ? null : rs.getDate("起始日期").toString());
                m.put("periodEndDate", rs.getDate("终止日期") == null ? null : rs.getDate("终止日期").toString());
                m.put("region", rs.getString("覆盖地区"));
                m.put("targetScale", rs.getBigDecimal("项目目标规模"));
                m.put("expectedRebate", rs.getBigDecimal("预计收益返利金额"));
                m.put("expectedCost", rs.getBigDecimal("预计费用"));
                m.put("description", rs.getString("项目信息简述"));
                list.add(m);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("从BPM获取立项数据失败: " + e.getMessage(), e);
        }
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
        try { p.setOwnerName(rs.getString("owner_name")); } catch (Exception ignore) {}
        try { p.setCreatedByName(rs.getString("created_by_name")); } catch (Exception ignore) {}
        return p;
    }
}

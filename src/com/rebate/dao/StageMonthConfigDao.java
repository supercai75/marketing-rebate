package com.rebate.dao;

import com.rebate.model.StageMonthConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目阶段-月份区间配置 DAO
 */
public class StageMonthConfigDao {

    private static final String[] STAGE_ORDER = {"S1", "S2", "S3", "S4"};

    public List<StageMonthConfig> listByProject(Long projectId) {
        if (projectId == null) return new ArrayList<>();
        return BaseDao.query(
                "SELECT * FROM prj_stage_month_config WHERE project_id=? ORDER BY sort_no, stage_code",
                this::map, projectId);
    }

    /**
     * 返回 stageCode -> [startYyyymm, endYyyymm] 的区间映射
     */
    public Map<String, int[]> listRangesByProject(Long projectId) {
        Map<String, int[]> r = new LinkedHashMap<>();
        if (projectId == null) return r;
        for (StageMonthConfig c : listByProject(projectId)) {
            if (c.getStageCode() != null && c.getStartYyyymm() != null && c.getEndYyyymm() != null) {
                r.put(c.getStageCode(), new int[]{c.getStartYyyymm(), c.getEndYyyymm()});
            }
        }
        return r;
    }

    public Map<String, int[]> listRangesByProjectWithConn(Connection conn, Long projectId) throws SQLException {
        Map<String, int[]> r = new LinkedHashMap<>();
        if (projectId == null) return r;
        List<StageMonthConfig> list = BaseDao.queryWithConn(conn,
                "SELECT * FROM prj_stage_month_config WHERE project_id=? ORDER BY sort_no, stage_code",
                this::map, projectId);
        for (StageMonthConfig c : list) {
            if (c.getStageCode() != null && c.getStartYyyymm() != null && c.getEndYyyymm() != null) {
                r.put(c.getStageCode(), new int[]{c.getStartYyyymm(), c.getEndYyyymm()});
            }
        }
        return r;
    }

    public int deleteByProject(Long projectId) {
        return BaseDao.update("DELETE FROM prj_stage_month_config WHERE project_id=?", projectId);
    }

    public int deleteByProjectWithConn(Connection conn, Long projectId) throws SQLException {
        return BaseDao.updateWithConn(conn, "DELETE FROM prj_stage_month_config WHERE project_id=?", projectId);
    }

    public Long insertWithConn(Connection conn, StageMonthConfig c) throws SQLException {
        int sortNo = c.getSortNo() != null ? c.getSortNo() : indexOfStage(c.getStageCode());
        return BaseDao.insertReturnIdWithConn(conn,
                "INSERT INTO prj_stage_month_config(project_id, stage_code, start_yyyymm, end_yyyymm, sort_no) VALUES(?, ?, ?, ?, ?)",
                c.getProjectId(), c.getStageCode(), c.getStartYyyymm(), c.getEndYyyymm(), sortNo);
    }

    private int indexOfStage(String code) {
        if (code == null) return 99;
        for (int i = 0; i < STAGE_ORDER.length; i++) {
            if (STAGE_ORDER[i].equalsIgnoreCase(code)) return i + 1;
        }
        return 99;
    }

    private StageMonthConfig map(ResultSet rs) throws SQLException {
        StageMonthConfig c = new StageMonthConfig();
        c.setId(rs.getLong("id"));
        c.setProjectId(rs.getLong("project_id"));
        c.setStageCode(rs.getString("stage_code"));
        c.setStartYyyymm(rs.getInt("start_yyyymm"));
        c.setEndYyyymm(rs.getInt("end_yyyymm"));
        try { c.setSortNo(rs.getInt("sort_no")); } catch (SQLException ignored) {}
        return c;
    }
}

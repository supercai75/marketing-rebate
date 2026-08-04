package com.rebate.dao;

import com.rebate.model.ProjectStaff;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 项目作业人员 DAO
 */
public class ProjectStaffDao {

    private ProjectStaff map(ResultSet rs) throws SQLException {
        ProjectStaff s = new ProjectStaff();
        s.setId(rs.getLong("id"));
        s.setProjectId(rs.getLong("project_id"));
        s.setUserName(rs.getString("user_name"));
        s.setUserCode(rs.getString("user_code"));
        s.setDeptName(rs.getString("dept_name"));
        s.setPosition(rs.getString("position"));
        s.setWorkType(rs.getString("work_type"));
        s.setLaborCostRatio(BaseDao.toBigDecimal(rs.getObject("labor_cost_ratio")));
        s.setExpenseRatio(BaseDao.toBigDecimal(rs.getObject("expense_ratio")));
        return s;
    }

    public List<ProjectStaff> listByProject(long projectId) {
        return BaseDao.query("SELECT * FROM prj_project_staff WHERE project_id=? ORDER BY id", this::map, projectId);
    }

    public List<ProjectStaff> pageByProject(long projectId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return BaseDao.query("SELECT * FROM prj_project_staff WHERE project_id=? ORDER BY id LIMIT ? OFFSET ?", this::map, projectId, pageSize, offset);
    }

    public long countByProject(long projectId) {
        return BaseDao.count("SELECT COUNT(*) FROM prj_project_staff WHERE project_id=?", projectId);
    }

    public ProjectStaff findById(long id) {
        return BaseDao.queryOne("SELECT * FROM prj_project_staff WHERE id=?", this::map, id);
    }

    public Long insert(ProjectStaff s) {
        String sql = "INSERT INTO prj_project_staff(project_id, user_name, user_code, dept_name, position, work_type, labor_cost_ratio, expense_ratio) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        return BaseDao.insertReturnId(sql, s.getProjectId(), s.getUserName(), s.getUserCode(), s.getDeptName(),
                s.getPosition(), s.getWorkType(), s.getLaborCostRatio(), s.getExpenseRatio());
    }

    public int update(ProjectStaff s) {
        String sql = "UPDATE prj_project_staff SET user_name=?, user_code=?, dept_name=?, position=?, work_type=?, labor_cost_ratio=?, expense_ratio=? WHERE id=?";
        return BaseDao.update(sql, s.getUserName(), s.getUserCode(), s.getDeptName(), s.getPosition(),
                s.getWorkType(), s.getLaborCostRatio(), s.getExpenseRatio(), s.getId());
    }

    public int delete(long id) {
        return BaseDao.update("DELETE FROM prj_project_staff WHERE id=?", id);
    }
}

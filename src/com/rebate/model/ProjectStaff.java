package com.rebate.model;

import java.math.BigDecimal;

/**
 * 项目作业人员
 */
public class ProjectStaff {
    private Long id;
    private Long projectId;
    private String userName;
    private String userCode;
    private String deptName;
    private String position;
    private String workType; // FULL:全职, PART:兼职, OUTSOURCE:外包
    private BigDecimal laborCostRatio;
    private BigDecimal expenseRatio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserCode() { return userCode; }
    public void setUserCode(String userCode) { this.userCode = userCode; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public BigDecimal getLaborCostRatio() { return laborCostRatio; }
    public void setLaborCostRatio(BigDecimal laborCostRatio) { this.laborCostRatio = laborCostRatio; }
    public BigDecimal getExpenseRatio() { return expenseRatio; }
    public void setExpenseRatio(BigDecimal expenseRatio) { this.expenseRatio = expenseRatio; }
}

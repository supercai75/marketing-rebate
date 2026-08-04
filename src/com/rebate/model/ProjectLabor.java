package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 项目人工成本
 */
public class ProjectLabor {
    private Long id;
    private Long projectId;
    private String monthYyyymm;
    private String workNo;
    private String name;
    private String workType;
    private BigDecimal salary;
    private BigDecimal welfare;
    private BigDecimal otherCost;
    private BigDecimal totalCost;
    private BigDecimal allocatedAmount;
    private BigDecimal allocRatio;
    private String matchedType;
    private Long importUser;
    private Timestamp importTime;
    private String source;
    private String remark;
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getMonthYyyymm() { return monthYyyymm; }
    public void setMonthYyyymm(String monthYyyymm) { this.monthYyyymm = monthYyyymm; }
    public String getWorkNo() { return workNo; }
    public void setWorkNo(String workNo) { this.workNo = workNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public BigDecimal getWelfare() { return welfare; }
    public void setWelfare(BigDecimal welfare) { this.welfare = welfare; }
    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    public BigDecimal getAllocRatio() { return allocRatio; }
    public void setAllocRatio(BigDecimal allocRatio) { this.allocRatio = allocRatio; }
    public String getMatchedType() { return matchedType; }
    public void setMatchedType(String matchedType) { this.matchedType = matchedType; }
    public Long getImportUser() { return importUser; }
    public void setImportUser(Long importUser) { this.importUser = importUser; }
    public Timestamp getImportTime() { return importTime; }
    public void setImportTime(Timestamp importTime) { this.importTime = importTime; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}

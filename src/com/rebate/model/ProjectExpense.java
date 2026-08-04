package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 项目费用
 */
public class ProjectExpense {
    private Long id;
    private Long projectId;
    private Date reimburseDate;
    private String expenseType;
    private String workNo;
    private String name;
    private String description;
    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private String source;
    private String rawProjectName;
    private String matchedType;
    private Long importUser;
    private Timestamp importTime;
    private String docNo;
    private String remark;
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Date getReimburseDate() { return reimburseDate; }
    public void setReimburseDate(Date reimburseDate) { this.reimburseDate = reimburseDate; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    public String getWorkNo() { return workNo; }
    public void setWorkNo(String workNo) { this.workNo = workNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRawProjectName() { return rawProjectName; }
    public void setRawProjectName(String rawProjectName) { this.rawProjectName = rawProjectName; }
    public String getMatchedType() { return matchedType; }
    public void setMatchedType(String matchedType) { this.matchedType = matchedType; }
    public Long getImportUser() { return importUser; }
    public void setImportUser(Long importUser) { this.importUser = importUser; }
    public Timestamp getImportTime() { return importTime; }
    public void setImportTime(Timestamp importTime) { this.importTime = importTime; }
    public String getDocNo() { return docNo; }
    public void setDocNo(String docNo) { this.docNo = docNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
}

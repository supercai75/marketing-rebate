package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 应收
 */
public class Receivable {
    private Long id;
    private Long projectId;
    private String stage;
    private BigDecimal scaleAmount;
    private BigDecimal assessAmount;
    private BigDecimal totalAmount;
    private BigDecimal estimateAmount;
    private String status;
    private Long fillUser;
    private Timestamp fillTime;
    private Long auditUser;
    private Timestamp auditTime;
    private String remark;
    private String projectName;
    private String fillUserName;
    private String auditUserName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public BigDecimal getScaleAmount() { return scaleAmount; }
    public void setScaleAmount(BigDecimal scaleAmount) { this.scaleAmount = scaleAmount; }
    public BigDecimal getAssessAmount() { return assessAmount; }
    public void setAssessAmount(BigDecimal assessAmount) { this.assessAmount = assessAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getEstimateAmount() { return estimateAmount; }
    public void setEstimateAmount(BigDecimal estimateAmount) { this.estimateAmount = estimateAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getFillUser() { return fillUser; }
    public void setFillUser(Long fillUser) { this.fillUser = fillUser; }
    public Timestamp getFillTime() { return fillTime; }
    public void setFillTime(Timestamp fillTime) { this.fillTime = fillTime; }
    public Long getAuditUser() { return auditUser; }
    public void setAuditUser(Long auditUser) { this.auditUser = auditUser; }
    public Timestamp getAuditTime() { return auditTime; }
    public void setAuditTime(Timestamp auditTime) { this.auditTime = auditTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getFillUserName() { return fillUserName; }
    public void setFillUserName(String fillUserName) { this.fillUserName = fillUserName; }
    public String getAuditUserName() { return auditUserName; }
    public void setAuditUserName(String auditUserName) { this.auditUserName = auditUserName; }
}

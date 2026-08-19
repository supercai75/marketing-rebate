package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 应付
 */
public class Payable {
    private Long id;
    private Long projectId;
    private Long agreementId;
    private String stage;
    private BigDecimal scaleAmount;
    private BigDecimal assessAmount;
    private BigDecimal totalAmount;
    private BigDecimal estimateAmount;
    private BigDecimal taxRate;
    private String status;
    private Long fillUser;
    private Timestamp fillTime;
    private Long auditUser;
    private Timestamp auditTime;
    private Long confirmUser;
    private Timestamp confirmTime;
    private String remark;
    private String projectName;
    private String agreementName;
    private String distributor;
    private String distributorType;
    private String fillUserName;
    private String auditUserName;
    private String confirmUserName;
    private java.util.List<AssessItemDetail> assessItems;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
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
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
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
    public Long getConfirmUser() { return confirmUser; }
    public void setConfirmUser(Long confirmUser) { this.confirmUser = confirmUser; }
    public Timestamp getConfirmTime() { return confirmTime; }
    public void setConfirmTime(Timestamp confirmTime) { this.confirmTime = confirmTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getAgreementName() { return agreementName; }
    public void setAgreementName(String agreementName) { this.agreementName = agreementName; }
    public String getDistributor() { return distributor; }
    public void setDistributor(String distributor) { this.distributor = distributor; }
    public String getDistributorType() { return distributorType; }
    public void setDistributorType(String distributorType) { this.distributorType = distributorType; }
    public String getFillUserName() { return fillUserName; }
    public void setFillUserName(String fillUserName) { this.fillUserName = fillUserName; }
    public String getAuditUserName() { return auditUserName; }
    public void setAuditUserName(String auditUserName) { this.auditUserName = auditUserName; }
    public String getConfirmUserName() { return confirmUserName; }
    public void setConfirmUserName(String confirmUserName) { this.confirmUserName = confirmUserName; }
    public java.util.List<AssessItemDetail> getAssessItems() { return assessItems; }
    public void setAssessItems(java.util.List<AssessItemDetail> assessItems) { this.assessItems = assessItems; }
}

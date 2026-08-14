package com.rebate.model;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 项目主表
 */
public class Project {
    private Long id;
    private String projectCode;
    private String projectName;
    private String brand;
    private String coProduct;
    private String coMode;
    private String coYear;
    private Date periodStartDate;
    private Date periodEndDate;
    private String region;
    private java.math.BigDecimal targetScale;
    private java.math.BigDecimal expectedRebate;
    private java.math.BigDecimal expectedCost;
    private String description;
    private String bpmProcessId;
    private String bpmProjectId;
    private Integer bpmSynced;
    private String status;
    private Long ownerUserId;
    private Long createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String ownerName;
    private String createdByName;
    private Long projectGroupId;
    private String projectGroupName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCoProduct() { return coProduct; }
    public void setCoProduct(String coProduct) { this.coProduct = coProduct; }
    public String getCoMode() { return coMode; }
    public void setCoMode(String coMode) { this.coMode = coMode; }
    public String getCoYear() { return coYear; }
    public void setCoYear(String coYear) { this.coYear = coYear; }
    public Date getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(Date periodStartDate) { this.periodStartDate = periodStartDate; }
    public Date getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(Date periodEndDate) { this.periodEndDate = periodEndDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public java.math.BigDecimal getTargetScale() { return targetScale; }
    public void setTargetScale(java.math.BigDecimal targetScale) { this.targetScale = targetScale; }
    public java.math.BigDecimal getExpectedRebate() { return expectedRebate; }
    public void setExpectedRebate(java.math.BigDecimal expectedRebate) { this.expectedRebate = expectedRebate; }
    public java.math.BigDecimal getExpectedCost() { return expectedCost; }
    public void setExpectedCost(java.math.BigDecimal expectedCost) { this.expectedCost = expectedCost; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBpmProcessId() { return bpmProcessId; }
    public void setBpmProcessId(String bpmProcessId) { this.bpmProcessId = bpmProcessId; }
    public String getBpmProjectId() { return bpmProjectId; }
    public void setBpmProjectId(String bpmProjectId) { this.bpmProjectId = bpmProjectId; }
    public Integer getBpmSynced() { return bpmSynced; }
    public void setBpmSynced(Integer bpmSynced) { this.bpmSynced = bpmSynced; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public Long getProjectGroupId() { return projectGroupId; }
    public void setProjectGroupId(Long projectGroupId) { this.projectGroupId = projectGroupId; }
    public String getProjectGroupName() { return projectGroupName; }
    public void setProjectGroupName(String projectGroupName) { this.projectGroupName = projectGroupName; }
}

package com.rebate.model;

import java.math.BigDecimal;

/**
 * 考核组（按项目隔离）
 */
public class AssessGroup {
    private Long id;
    private Long projectId;
    private String groupCode;
    private String groupName;
    private String description;
    private BigDecimal targetScale;
    private BigDecimal stage1Target;
    private BigDecimal stage2Target;
    private BigDecimal stage3Target;
    private BigDecimal stage4Target;
    private String sharedGroupIds; // 共享考核组ID列表（逗号分隔，如 "2,3"）
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTargetScale() { return targetScale; }
    public void setTargetScale(BigDecimal targetScale) { this.targetScale = targetScale; }
    public BigDecimal getStage1Target() { return stage1Target; }
    public void setStage1Target(BigDecimal stage1Target) { this.stage1Target = stage1Target; }
    public BigDecimal getStage2Target() { return stage2Target; }
    public void setStage2Target(BigDecimal stage2Target) { this.stage2Target = stage2Target; }
    public BigDecimal getStage3Target() { return stage3Target; }
    public void setStage3Target(BigDecimal stage3Target) { this.stage3Target = stage3Target; }
    public BigDecimal getStage4Target() { return stage4Target; }
    public void setStage4Target(BigDecimal stage4Target) { this.stage4Target = stage4Target; }
    public String getSharedGroupIds() { return sharedGroupIds; }
    public void setSharedGroupIds(String sharedGroupIds) { this.sharedGroupIds = sharedGroupIds; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}

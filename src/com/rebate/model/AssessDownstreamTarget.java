package com.rebate.model;

import java.math.BigDecimal;

/**
 * 下游协议考核目标
 * 表: prj_assess_downstream_target
 * 说明: 每个下游协议的每个考核组，存储各阶段目标和总体目标
 *       考核组名称/编码来自 prj_assess_group（只读引用）
 *       目标数据每个下游协议独立设置
 */
public class AssessDownstreamTarget {
    private Long id;
    private Long agreementId;          // 下游协议ID
    private Long assessGroupId;        // 考核组ID（关联 prj_assess_group.id）
    private String groupName;          // 考核组名称（冗余存储，来源于 prj_assess_group）
    private String groupCode;         // 考核组编码（同上）
    private BigDecimal totalTarget;    // 总目标
    private BigDecimal stage1Target;  // 阶段一目标
    private BigDecimal stage2Target;  // 阶段二目标
    private BigDecimal stage3Target;  // 阶段三目标
    private BigDecimal stage4Target;  // 阶段四目标
    private String remark;            // 备注

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public Long getAssessGroupId() { return assessGroupId; }
    public void setAssessGroupId(Long assessGroupId) { this.assessGroupId = assessGroupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }
    public BigDecimal getTotalTarget() { return totalTarget; }
    public void setTotalTarget(BigDecimal totalTarget) { this.totalTarget = totalTarget; }
    public BigDecimal getStage1Target() { return stage1Target; }
    public void setStage1Target(BigDecimal stage1Target) { this.stage1Target = stage1Target; }
    public BigDecimal getStage2Target() { return stage2Target; }
    public void setStage2Target(BigDecimal stage2Target) { this.stage2Target = stage2Target; }
    public BigDecimal getStage3Target() { return stage3Target; }
    public void setStage3Target(BigDecimal stage3Target) { this.stage3Target = stage3Target; }
    public BigDecimal getStage4Target() { return stage4Target; }
    public void setStage4Target(BigDecimal stage4Target) { this.stage4Target = stage4Target; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

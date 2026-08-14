package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * 上游协议
 */
public class UpstreamAgreement {
    private Long id;
    private Long projectId;
    private Integer version;
    private Integer isCurrent;
    private String bpmAgreeId;
    private String agreementName;
    private String agreementNo;
    private Date periodStartDate;
    private Date periodEndDate;
    private String region;
    private String targetTerminal;
    private String calcBasis;
    private BigDecimal targetScale;
    private String calcMethod;
    private String calcMode; // 返利计算模式: PROGRESSIVE(递进式) / FLAT(全部计算)
    private String rebateCalcBasis; // 返利计算依据: QTY/SALE_QTY/CALC_AMT/BID_AMT
    private String supplier;
    private String targetDept;
    private String flowContact;
    private String flowPhone;
    private String flowChannel;
    private String flowProvideMethod;
    private BigDecimal stage1Target;
    private BigDecimal stage2Target;
    private BigDecimal stage3Target;
    private BigDecimal stage4Target;
    private Long ownerUserId;
    private String policyDetail;
    private String rebateCalcRule;
    private String settleBasis;
    private String settleRatio;
    private String rebatePayType;
    private String rebatePayTime;
    private String teamAssessSettle;
    private Integer requiredStaffNum;
    private Integer formalCount;
    private String formalNames;
    private Integer informalCount;
    private String informalNames;
    private Long createdBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String projectName;
    private String coYear;
    private String ownerName;
    private List<TeamTarget> teamTargets;
    private List<AttachFile> remarkFiles;
    private List<AttachFile> attachFiles;
    private List<com.rebate.model.RebateRule> rebateRules;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Integer isCurrent) { this.isCurrent = isCurrent; }
    public String getBpmAgreeId() { return bpmAgreeId; }
    public void setBpmAgreeId(String bpmAgreeId) { this.bpmAgreeId = bpmAgreeId; }
    public String getAgreementName() { return agreementName; }
    public void setAgreementName(String agreementName) { this.agreementName = agreementName; }
    public String getAgreementNo() { return agreementNo; }
    public void setAgreementNo(String agreementNo) { this.agreementNo = agreementNo; }
    public Date getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(Date periodStartDate) { this.periodStartDate = periodStartDate; }
    public Date getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(Date periodEndDate) { this.periodEndDate = periodEndDate; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getTargetTerminal() { return targetTerminal; }
    public void setTargetTerminal(String targetTerminal) { this.targetTerminal = targetTerminal; }
    public String getCalcBasis() { return calcBasis; }
    public void setCalcBasis(String calcBasis) { this.calcBasis = calcBasis; }
    public BigDecimal getTargetScale() { return targetScale; }
    public void setTargetScale(BigDecimal targetScale) { this.targetScale = targetScale; }
    public String getCalcMethod() { return calcMethod; }
    public void setCalcMethod(String calcMethod) { this.calcMethod = calcMethod; }
    public String getCalcMode() { return calcMode; }
    public void setCalcMode(String calcMode) { this.calcMode = calcMode; }
    public String getRebateCalcBasis() { return rebateCalcBasis; }
    public void setRebateCalcBasis(String rebateCalcBasis) { this.rebateCalcBasis = rebateCalcBasis; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getTargetDept() { return targetDept; }
    public void setTargetDept(String targetDept) { this.targetDept = targetDept; }
    public String getFlowContact() { return flowContact; }
    public void setFlowContact(String flowContact) { this.flowContact = flowContact; }
    public String getFlowPhone() { return flowPhone; }
    public void setFlowPhone(String flowPhone) { this.flowPhone = flowPhone; }
    public String getFlowChannel() { return flowChannel; }
    public void setFlowChannel(String flowChannel) { this.flowChannel = flowChannel; }
    public String getFlowProvideMethod() { return flowProvideMethod; }
    public void setFlowProvideMethod(String flowProvideMethod) { this.flowProvideMethod = flowProvideMethod; }
    public BigDecimal getStage1Target() { return stage1Target; }
    public void setStage1Target(BigDecimal stage1Target) { this.stage1Target = stage1Target; }
    public BigDecimal getStage2Target() { return stage2Target; }
    public void setStage2Target(BigDecimal stage2Target) { this.stage2Target = stage2Target; }
    public BigDecimal getStage3Target() { return stage3Target; }
    public void setStage3Target(BigDecimal stage3Target) { this.stage3Target = stage3Target; }
    public BigDecimal getStage4Target() { return stage4Target; }
    public void setStage4Target(BigDecimal stage4Target) { this.stage4Target = stage4Target; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getPolicyDetail() { return policyDetail; }
    public void setPolicyDetail(String policyDetail) { this.policyDetail = policyDetail; }
    public String getRebateCalcRule() { return rebateCalcRule; }
    public void setRebateCalcRule(String rebateCalcRule) { this.rebateCalcRule = rebateCalcRule; }
    public String getSettleBasis() { return settleBasis; }
    public void setSettleBasis(String settleBasis) { this.settleBasis = settleBasis; }
    public String getSettleRatio() { return settleRatio; }
    public void setSettleRatio(String settleRatio) { this.settleRatio = settleRatio; }
    public String getRebatePayType() { return rebatePayType; }
    public void setRebatePayType(String rebatePayType) { this.rebatePayType = rebatePayType; }
    public String getRebatePayTime() { return rebatePayTime; }
    public void setRebatePayTime(String rebatePayTime) { this.rebatePayTime = rebatePayTime; }
    public String getTeamAssessSettle() { return teamAssessSettle; }
    public void setTeamAssessSettle(String teamAssessSettle) { this.teamAssessSettle = teamAssessSettle; }
    public Integer getRequiredStaffNum() { return requiredStaffNum; }
    public void setRequiredStaffNum(Integer requiredStaffNum) { this.requiredStaffNum = requiredStaffNum; }
    public Integer getFormalCount() { return formalCount; }
    public void setFormalCount(Integer formalCount) { this.formalCount = formalCount; }
    public String getFormalNames() { return formalNames; }
    public void setFormalNames(String formalNames) { this.formalNames = formalNames; }
    public Integer getInformalCount() { return informalCount; }
    public void setInformalCount(Integer informalCount) { this.informalCount = informalCount; }
    public String getInformalNames() { return informalNames; }
    public void setInformalNames(String informalNames) { this.informalNames = informalNames; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getCoYear() { return coYear; }
    public void setCoYear(String coYear) { this.coYear = coYear; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public List<TeamTarget> getTeamTargets() { return teamTargets; }
    public void setTeamTargets(List<TeamTarget> teamTargets) { this.teamTargets = teamTargets; }
    public List<AttachFile> getRemarkFiles() { return remarkFiles; }
    public void setRemarkFiles(List<AttachFile> remarkFiles) { this.remarkFiles = remarkFiles; }
    public List<AttachFile> getAttachFiles() { return attachFiles; }
    public void setAttachFiles(List<AttachFile> attachFiles) { this.attachFiles = attachFiles; }
    public List<com.rebate.model.RebateRule> getRebateRules() { return rebateRules; }
    public void setRebateRules(List<com.rebate.model.RebateRule> rebateRules) { this.rebateRules = rebateRules; }
}

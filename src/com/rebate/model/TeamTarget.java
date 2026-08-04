package com.rebate.model;

import java.sql.Timestamp;

/**
 * 专职团队考核目标
 */
public class TeamTarget {
    private Long id;
    private Long agreementId;
    private String agreementType; // UPSTREAM / DOWNSTREAM
    private String targetName;
    private String owner;
    private String requirement;
    private String calcStandard;
    private String rewardStandard;
    private Integer sortNo;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String agreementType) { this.agreementType = agreementType; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }
    public String getCalcStandard() { return calcStandard; }
    public void setCalcStandard(String calcStandard) { this.calcStandard = calcStandard; }
    public String getRewardStandard() { return rewardStandard; }
    public void setRewardStandard(String rewardStandard) { this.rewardStandard = rewardStandard; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

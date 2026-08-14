package com.rebate.model;

import java.math.BigDecimal;

/**
 * 返利计算规则
 */
public class RebateRule {
    private Long id;
    private Long agreementId;
    private String stageCode;
    private BigDecimal thresholdLow;
    private BigDecimal thresholdHigh;
    private BigDecimal rebateRatio;
    private String rewardType;
    private Long assessGroupId;
    private Integer sortNo;
    private String expression;
    private String calcMode;       // PROGRESSIVE(递进式) / FLAT(全部计算)
    private String sharedGroupIds; // 共享考核组ID列表（逗号分隔，多考核组共享目标时使用）

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public BigDecimal getThresholdLow() { return thresholdLow; }
    public void setThresholdLow(BigDecimal thresholdLow) { this.thresholdLow = thresholdLow; }
    public BigDecimal getThresholdHigh() { return thresholdHigh; }
    public void setThresholdHigh(BigDecimal thresholdHigh) { this.thresholdHigh = thresholdHigh; }
    public BigDecimal getRebateRatio() { return rebateRatio; }
    public void setRebateRatio(BigDecimal rebateRatio) { this.rebateRatio = rebateRatio; }
    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }
    public Long getAssessGroupId() { return assessGroupId; }
    public void setAssessGroupId(Long assessGroupId) { this.assessGroupId = assessGroupId; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getCalcMode() { return calcMode; }
    public void setCalcMode(String calcMode) { this.calcMode = calcMode; }
    public String getSharedGroupIds() { return sharedGroupIds; }
    public void setSharedGroupIds(String sharedGroupIds) { this.sharedGroupIds = sharedGroupIds; }
}

package com.rebate.model;

import java.math.BigDecimal;

/**
 * 项目规模/阶段达成情况（用于概览与平衡表）
 */
public class ProjectScaleSummary {
    private Long projectId;
    private String projectName;
    private String coPeriod;
    private java.sql.Date periodStartDate;
    private java.sql.Date periodEndDate;
    /** 协议依据 QTY / AMT */
    private String calcBasis;
    /** 协议指标规模数 */
    private BigDecimal targetScale;
    /** 项目预计收益 */
    private BigDecimal expectedRebate;
    /** 实际达成总规模（按 calcBasis 累加 valid 流向） */
    private BigDecimal actualTotalScale;
    /** 各阶段达成 */
    private BigDecimal stage1Actual;
    private BigDecimal stage2Actual;
    private BigDecimal stage3Actual;
    private BigDecimal stage4Actual;
    /** 各阶段目标 */
    private BigDecimal stage1Target;
    private BigDecimal stage2Target;
    private BigDecimal stage3Target;
    private BigDecimal stage4Target;
    /** 达成率 */
    private BigDecimal totalScaleRate;
    private BigDecimal stage1Rate;
    private BigDecimal stage2Rate;
    private BigDecimal stage3Rate;
    private BigDecimal stage4Rate;
    /** 应收 */
    private BigDecimal receivableTotal;
    private BigDecimal receivedTotal;
    private BigDecimal payableTotal;
    private BigDecimal paidTotal;
    /** 投入 */
    private BigDecimal expenseTotal;
    private BigDecimal laborTotal;
    private BigDecimal investTotal;
    /** 派生 */
    private BigDecimal receivableRate;     // (应收-投入-对外应付) / 预计收益
    private BigDecimal actualIncomeRate;   // (实收-投入-对外实付) / 预计收益
    private BigDecimal cashRate;           // 实收/应收

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getCoPeriod() { return coPeriod; }
    public void setCoPeriod(String coPeriod) { this.coPeriod = coPeriod; }
    public java.sql.Date getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(java.sql.Date periodStartDate) { this.periodStartDate = periodStartDate; }
    public java.sql.Date getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(java.sql.Date periodEndDate) { this.periodEndDate = periodEndDate; }
    public String getCalcBasis() { return calcBasis; }
    public void setCalcBasis(String calcBasis) { this.calcBasis = calcBasis; }
    public BigDecimal getTargetScale() { return targetScale; }
    public void setTargetScale(BigDecimal targetScale) { this.targetScale = targetScale; }
    public BigDecimal getExpectedRebate() { return expectedRebate; }
    public void setExpectedRebate(BigDecimal expectedRebate) { this.expectedRebate = expectedRebate; }
    public BigDecimal getActualTotalScale() { return actualTotalScale; }
    public void setActualTotalScale(BigDecimal actualTotalScale) { this.actualTotalScale = actualTotalScale; }
    public BigDecimal getStage1Actual() { return stage1Actual; }
    public void setStage1Actual(BigDecimal stage1Actual) { this.stage1Actual = stage1Actual; }
    public BigDecimal getStage2Actual() { return stage2Actual; }
    public void setStage2Actual(BigDecimal stage2Actual) { this.stage2Actual = stage2Actual; }
    public BigDecimal getStage3Actual() { return stage3Actual; }
    public void setStage3Actual(BigDecimal stage3Actual) { this.stage3Actual = stage3Actual; }
    public BigDecimal getStage4Actual() { return stage4Actual; }
    public void setStage4Actual(BigDecimal stage4Actual) { this.stage4Actual = stage4Actual; }
    public BigDecimal getStage1Target() { return stage1Target; }
    public void setStage1Target(BigDecimal stage1Target) { this.stage1Target = stage1Target; }
    public BigDecimal getStage2Target() { return stage2Target; }
    public void setStage2Target(BigDecimal stage2Target) { this.stage2Target = stage2Target; }
    public BigDecimal getStage3Target() { return stage3Target; }
    public void setStage3Target(BigDecimal stage3Target) { this.stage3Target = stage3Target; }
    public BigDecimal getStage4Target() { return stage4Target; }
    public void setStage4Target(BigDecimal stage4Target) { this.stage4Target = stage4Target; }
    public BigDecimal getTotalScaleRate() { return totalScaleRate; }
    public void setTotalScaleRate(BigDecimal totalScaleRate) { this.totalScaleRate = totalScaleRate; }
    public BigDecimal getStage1Rate() { return stage1Rate; }
    public void setStage1Rate(BigDecimal stage1Rate) { this.stage1Rate = stage1Rate; }
    public BigDecimal getStage2Rate() { return stage2Rate; }
    public void setStage2Rate(BigDecimal stage2Rate) { this.stage2Rate = stage2Rate; }
    public BigDecimal getStage3Rate() { return stage3Rate; }
    public void setStage3Rate(BigDecimal stage3Rate) { this.stage3Rate = stage3Rate; }
    public BigDecimal getStage4Rate() { return stage4Rate; }
    public void setStage4Rate(BigDecimal stage4Rate) { this.stage4Rate = stage4Rate; }
    public BigDecimal getReceivableTotal() { return receivableTotal; }
    public void setReceivableTotal(BigDecimal receivableTotal) { this.receivableTotal = receivableTotal; }
    public BigDecimal getReceivedTotal() { return receivedTotal; }
    public void setReceivedTotal(BigDecimal receivedTotal) { this.receivedTotal = receivedTotal; }
    public BigDecimal getPayableTotal() { return payableTotal; }
    public void setPayableTotal(BigDecimal payableTotal) { this.payableTotal = payableTotal; }
    public BigDecimal getPaidTotal() { return paidTotal; }
    public void setPaidTotal(BigDecimal paidTotal) { this.paidTotal = paidTotal; }
    public BigDecimal getExpenseTotal() { return expenseTotal; }
    public void setExpenseTotal(BigDecimal expenseTotal) { this.expenseTotal = expenseTotal; }
    public BigDecimal getLaborTotal() { return laborTotal; }
    public void setLaborTotal(BigDecimal laborTotal) { this.laborTotal = laborTotal; }
    public BigDecimal getInvestTotal() { return investTotal; }
    public void setInvestTotal(BigDecimal investTotal) { this.investTotal = investTotal; }
    public BigDecimal getReceivableRate() { return receivableRate; }
    public void setReceivableRate(BigDecimal receivableRate) { this.receivableRate = receivableRate; }
    public BigDecimal getActualIncomeRate() { return actualIncomeRate; }
    public void setActualIncomeRate(BigDecimal actualIncomeRate) { this.actualIncomeRate = actualIncomeRate; }
    public BigDecimal getCashRate() { return cashRate; }
    public void setCashRate(BigDecimal cashRate) { this.cashRate = cashRate; }
}

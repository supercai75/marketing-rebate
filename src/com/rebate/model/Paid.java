package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 实付
 */
public class Paid {
    private Long id;
    private Long projectId;
    private Long agreementId;
    private String stage;
    private String rebateType;
    private String applicant;
    private String applyDept;
    private Date applyDate;
    private String receiveDept;
    private String customerName;
    private BigDecimal totalRebate;
    private BigDecimal actualRebate;
    private BigDecimal diffAmount;
    private String executeStatus;
    private String bpmProcessId;
    private Long opUser;
    private Timestamp opTime;
    private Long financeUser;
    private Timestamp financeTime;
    private Timestamp finalTime;
    private String remark;
    private String projectName;
    private String agreementName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getRebateType() { return rebateType; }
    public void setRebateType(String rebateType) { this.rebateType = rebateType; }
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }
    public String getApplyDept() { return applyDept; }
    public void setApplyDept(String applyDept) { this.applyDept = applyDept; }
    public Date getApplyDate() { return applyDate; }
    public void setApplyDate(Date applyDate) { this.applyDate = applyDate; }
    public String getReceiveDept() { return receiveDept; }
    public void setReceiveDept(String receiveDept) { this.receiveDept = receiveDept; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getTotalRebate() { return totalRebate; }
    public void setTotalRebate(BigDecimal totalRebate) { this.totalRebate = totalRebate; }
    public BigDecimal getActualRebate() { return actualRebate; }
    public void setActualRebate(BigDecimal actualRebate) { this.actualRebate = actualRebate; }
    public BigDecimal getDiffAmount() { return diffAmount; }
    public void setDiffAmount(BigDecimal diffAmount) { this.diffAmount = diffAmount; }
    public String getExecuteStatus() { return executeStatus; }
    public void setExecuteStatus(String executeStatus) { this.executeStatus = executeStatus; }
    public String getBpmProcessId() { return bpmProcessId; }
    public void setBpmProcessId(String bpmProcessId) { this.bpmProcessId = bpmProcessId; }
    public Long getOpUser() { return opUser; }
    public void setOpUser(Long opUser) { this.opUser = opUser; }
    public Timestamp getOpTime() { return opTime; }
    public void setOpTime(Timestamp opTime) { this.opTime = opTime; }
    public Long getFinanceUser() { return financeUser; }
    public void setFinanceUser(Long financeUser) { this.financeUser = financeUser; }
    public Timestamp getFinanceTime() { return financeTime; }
    public void setFinanceTime(Timestamp financeTime) { this.financeTime = financeTime; }
    public Timestamp getFinalTime() { return finalTime; }
    public void setFinalTime(Timestamp finalTime) { this.finalTime = finalTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getAgreementName() { return agreementName; }
    public void setAgreementName(String agreementName) { this.agreementName = agreementName; }
}

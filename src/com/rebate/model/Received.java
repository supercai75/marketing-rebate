package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 实收
 */
public class Received {
    private Long id;
    private Long projectId;
    private String stage;
    private String rebateType;
    private String applicant;
    private String applyDept;
    private Date applyDate;
    private String financeCode;
    private BigDecimal rebateAmount;
    private BigDecimal taxRate;
    private BigDecimal totalPriceTax;
    private BigDecimal deptShare;
    private String invoiceNo;
    private String receiveDept;
    private String status;
    private String bpmProcessId;
    private Long purchaseUser;
    private Timestamp purchaseTime;
    private Long opUser;
    private Timestamp opTime;
    private Long financeUser;
    private Timestamp financeTime;
    private Timestamp finalTime;
    private String remark;
    private String projectName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
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
    public String getFinanceCode() { return financeCode; }
    public void setFinanceCode(String financeCode) { this.financeCode = financeCode; }
    public BigDecimal getRebateAmount() { return rebateAmount; }
    public void setRebateAmount(BigDecimal rebateAmount) { this.rebateAmount = rebateAmount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getTotalPriceTax() { return totalPriceTax; }
    public void setTotalPriceTax(BigDecimal totalPriceTax) { this.totalPriceTax = totalPriceTax; }
    public BigDecimal getDeptShare() { return deptShare; }
    public void setDeptShare(BigDecimal deptShare) { this.deptShare = deptShare; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getReceiveDept() { return receiveDept; }
    public void setReceiveDept(String receiveDept) { this.receiveDept = receiveDept; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBpmProcessId() { return bpmProcessId; }
    public void setBpmProcessId(String bpmProcessId) { this.bpmProcessId = bpmProcessId; }
    public Long getPurchaseUser() { return purchaseUser; }
    public void setPurchaseUser(Long purchaseUser) { this.purchaseUser = purchaseUser; }
    public Timestamp getPurchaseTime() { return purchaseTime; }
    public void setPurchaseTime(Timestamp purchaseTime) { this.purchaseTime = purchaseTime; }
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
}

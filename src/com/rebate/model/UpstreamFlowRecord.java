package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 上游流向明细
 */
public class UpstreamFlowRecord {
    private Long id;
    private Long projectId;
    private Long batchId;
    private String monthYyyymm;
    private Date businessDate;
    private String productName;
    private String spec;
    private String sellerName;
    private String sellerCity;
    private BigDecimal calcPrice;
    private BigDecimal quantity;
    private BigDecimal calcAmount;
    private String buyerName;
    private String buyerCity;
    private Long assessGroupId;
    private String assessGroupName;
    private Integer isValid;
    private Integer isFinal;
    private String rawRow;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getMonthYyyymm() { return monthYyyymm; }
    public void setMonthYyyymm(String monthYyyymm) { this.monthYyyymm = monthYyyymm; }
    public Date getBusinessDate() { return businessDate; }
    public void setBusinessDate(Date businessDate) { this.businessDate = businessDate; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSellerCity() { return sellerCity; }
    public void setSellerCity(String sellerCity) { this.sellerCity = sellerCity; }
    public BigDecimal getCalcPrice() { return calcPrice; }
    public void setCalcPrice(BigDecimal calcPrice) { this.calcPrice = calcPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getCalcAmount() { return calcAmount; }
    public void setCalcAmount(BigDecimal calcAmount) { this.calcAmount = calcAmount; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getBuyerCity() { return buyerCity; }
    public void setBuyerCity(String buyerCity) { this.buyerCity = buyerCity; }
    public Long getAssessGroupId() { return assessGroupId; }
    public void setAssessGroupId(Long assessGroupId) { this.assessGroupId = assessGroupId; }
    public String getAssessGroupName() { return assessGroupName; }
    public void setAssessGroupName(String assessGroupName) { this.assessGroupName = assessGroupName; }
    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }
    public Integer getIsFinal() { return isFinal; }
    public void setIsFinal(Integer isFinal) { this.isFinal = isFinal; }
    public String getRawRow() { return rawRow; }
    public void setRawRow(String rawRow) { this.rawRow = rawRow; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

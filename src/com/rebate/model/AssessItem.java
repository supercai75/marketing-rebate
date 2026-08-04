package com.rebate.model;

import java.math.BigDecimal;

/**
 * 考核组指标项
 */
public class AssessItem {
    private Long id;
    private Long groupId;
    private String itemCode;
    private String itemName;
    private String calcBasis;
    private BigDecimal targetValue;
    private BigDecimal weight;
    private Integer sortNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCalcBasis() { return calcBasis; }
    public void setCalcBasis(String calcBasis) { this.calcBasis = calcBasis; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}

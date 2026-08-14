package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 应收/应付考核明细
 */
public class AssessItemDetail {
    private Long id;
    private Long receivableId;
    private Long payableId;
    private String itemType;       // STAFF_COUNT(作业人数) / VISIT_COUNT(拜访次数) / MEETING(会议活动) / OTHER(其它)
    private String itemName;
    private String remark;
    private BigDecimal targetValue;
    private BigDecimal actualValue;
    private BigDecimal rewardAmount;
    private Long attachFileId;
    private Integer sortNo;
    private Timestamp createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReceivableId() { return receivableId; }
    public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(BigDecimal rewardAmount) { this.rewardAmount = rewardAmount; }
    public Long getAttachFileId() { return attachFileId; }
    public void setAttachFileId(Long attachFileId) { this.attachFileId = attachFileId; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

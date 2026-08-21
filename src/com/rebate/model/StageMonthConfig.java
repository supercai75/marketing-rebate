package com.rebate.model;

/**
 * 项目阶段-月份区间对应关系（仅合作周期非整12个月时由用户定义）
 */
public class StageMonthConfig {
    private Long id;
    private Long projectId;
    private String stageCode;       // S1 / S2 / S3 / S4
    private Integer startYyyymm;    // 起始月份(含) YYYYMM
    private Integer endYyyymm;       // 截止月份(含) YYYYMM
    private Integer sortNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public Integer getStartYyyymm() { return startYyyymm; }
    public void setStartYyyymm(Integer startYyyymm) { this.startYyyymm = startYyyymm; }
    public Integer getEndYyyymm() { return endYyyymm; }
    public void setEndYyyymm(Integer endYyyymm) { this.endYyyymm = endYyyymm; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}

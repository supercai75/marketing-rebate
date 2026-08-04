package com.rebate.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 上游流向导入批次
 */
public class UpstreamFlowBatch {
    private Long id;
    private Long projectId;
    private String batchCode;
    private String fileName;
    private String filePath;
    private Long importUser;
    private Timestamp importTime;
    private String monthSummary;
    private String remark;
    private String projectName;
    private String importUserName;
    private Integer status; // 0-生效, 1-失效, 2-部分失效, 3-最终稿

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getImportUser() { return importUser; }
    public void setImportUser(Long importUser) { this.importUser = importUser; }
    public Timestamp getImportTime() { return importTime; }
    public void setImportTime(Timestamp importTime) { this.importTime = importTime; }
    public String getMonthSummary() { return monthSummary; }
    public void setMonthSummary(String monthSummary) { this.monthSummary = monthSummary; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getImportUserName() { return importUserName; }
    public void setImportUserName(String importUserName) { this.importUserName = importUserName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}

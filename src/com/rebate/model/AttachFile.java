package com.rebate.model;

import java.sql.Timestamp;

/**
 * 附件
 */
public class AttachFile {
    private Long id;
    private Long agreementId;
    private String agreementType; // UPSTREAM / DOWNSTREAM
    private String fileType;      // PRODUCT/HOSPITAL/BLACKLIST/OTHER
    private String attachType;    // MAIN/SUPP
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Long uploadedBy;
    private Timestamp uploadedAt;
    private String downloadUrl;
    private String fileUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String agreementType) { this.agreementType = agreementType; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getAttachType() { return attachType; }
    public void setAttachType(String attachType) { this.attachType = attachType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}

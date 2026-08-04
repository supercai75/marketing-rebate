package com.rebate.model;

import java.sql.Timestamp;

/**
 * 操作日志
 */
public class OpLog {
    private Long id;
    private Long userId;
    private String loginName;
    private String module;
    private String action;
    private String content;
    private String ip;
    private Timestamp opTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Timestamp getOpTime() { return opTime; }
    public void setOpTime(Timestamp opTime) { this.opTime = opTime; }
}

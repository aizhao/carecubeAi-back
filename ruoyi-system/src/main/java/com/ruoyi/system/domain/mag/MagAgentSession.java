package com.ruoyi.system.domain.mag;

import java.util.Date;

public class MagAgentSession
{
    private String sessionId;
    private String agentCode;
    private String ragflowSessionId;
    private Long userId;
    private String sessionTitle;
    private String status;
    private Date lastMessageTime;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getRagflowSessionId() { return ragflowSessionId; }
    public void setRagflowSessionId(String ragflowSessionId) { this.ragflowSessionId = ragflowSessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}

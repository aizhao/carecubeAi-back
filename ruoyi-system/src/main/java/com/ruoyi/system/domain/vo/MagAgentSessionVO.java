package com.ruoyi.system.domain.vo;

import java.util.Date;
import java.util.List;

public class MagAgentSessionVO
{
    private String sessionId;
    private String agentCode;
    private String sessionTitle;
    private String status;
    private Date lastMessageTime;
    private Date createTime;
    private List<MagAgentMessageVO> messages;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public List<MagAgentMessageVO> getMessages() { return messages; }
    public void setMessages(List<MagAgentMessageVO> messages) { this.messages = messages; }
}

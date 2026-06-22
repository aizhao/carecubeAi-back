package com.ruoyi.system.domain.mag;

import java.util.Date;

public class MagAgentMessage
{
    private String messageId;
    private String sessionId;
    private String agentCode;
    private Long userId;
    private String role;
    private String content;
    private String eventType;
    private String structuredJson;
    private String referenceJson;
    private String attachmentJson;
    private String ragflowMessageId;
    private Date createTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getStructuredJson() { return structuredJson; }
    public void setStructuredJson(String structuredJson) { this.structuredJson = structuredJson; }
    public String getReferenceJson() { return referenceJson; }
    public void setReferenceJson(String referenceJson) { this.referenceJson = referenceJson; }
    public String getAttachmentJson() { return attachmentJson; }
    public void setAttachmentJson(String attachmentJson) { this.attachmentJson = attachmentJson; }
    public String getRagflowMessageId() { return ragflowMessageId; }
    public void setRagflowMessageId(String ragflowMessageId) { this.ragflowMessageId = ragflowMessageId; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}

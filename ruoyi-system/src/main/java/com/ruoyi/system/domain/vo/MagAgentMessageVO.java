package com.ruoyi.system.domain.vo;

import java.util.Date;

public class MagAgentMessageVO
{
    private String messageId;
    private String role;
    private String content;
    private String eventType;
    private String structuredJson;
    private String referenceJson;
    private String attachmentJson;
    private Date createTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
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
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}

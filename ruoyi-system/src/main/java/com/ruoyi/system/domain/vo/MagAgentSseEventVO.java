package com.ruoyi.system.domain.vo;

public class MagAgentSseEventVO
{
    private String type;
    private String sessionId;
    private String messageId;
    private String content;
    private Object data;

    public static MagAgentSseEventVO of(String type, String sessionId, String messageId, String content, Object data)
    {
        MagAgentSseEventVO vo = new MagAgentSseEventVO();
        vo.setType(type);
        vo.setSessionId(sessionId);
        vo.setMessageId(messageId);
        vo.setContent(content);
        vo.setData(data);
        return vo;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}

package com.ruoyi.system.domain.dto;

import java.util.Map;

public class MagAgentChatRequest
{
    private String sessionId;
    private String message;
    private Map<String, String> inputValues;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, String> getInputValues() { return inputValues; }
    public void setInputValues(Map<String, String> inputValues) { this.inputValues = inputValues; }
}

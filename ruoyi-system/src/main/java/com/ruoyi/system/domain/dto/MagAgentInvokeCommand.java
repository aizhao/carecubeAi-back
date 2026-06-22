package com.ruoyi.system.domain.dto;

import java.util.List;

public class MagAgentInvokeCommand
{
    private String agentCode;
    private String agentId;
    private String release;
    private String businessSessionId;
    private String ragflowSessionId;
    private Long userId;
    private String userKey;
    private String message;
    private List<MagAgentInputItemDTO> inputItems;

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }
    public String getBusinessSessionId() { return businessSessionId; }
    public void setBusinessSessionId(String businessSessionId) { this.businessSessionId = businessSessionId; }
    public String getRagflowSessionId() { return ragflowSessionId; }
    public void setRagflowSessionId(String ragflowSessionId) { this.ragflowSessionId = ragflowSessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<MagAgentInputItemDTO> getInputItems() { return inputItems; }
    public void setInputItems(List<MagAgentInputItemDTO> inputItems) { this.inputItems = inputItems; }
}

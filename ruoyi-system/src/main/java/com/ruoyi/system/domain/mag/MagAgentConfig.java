package com.ruoyi.system.domain.mag;

import com.ruoyi.common.core.domain.BaseEntity;

public class MagAgentConfig extends BaseEntity
{
    private Long id;
    private String agentCode;
    private String agentName;
    private String agentId;
    private String releaseValue;
    private String enabled;
    private String description;
    private Integer sort;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getReleaseValue() { return releaseValue; }
    public void setReleaseValue(String releaseValue) { this.releaseValue = releaseValue; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
}

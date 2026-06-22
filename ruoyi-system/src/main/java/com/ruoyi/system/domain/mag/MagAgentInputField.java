package com.ruoyi.system.domain.mag;

public class MagAgentInputField
{
    private Long id;
    private String agentCode;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String required;
    private String optionsJson;
    private String defaultValue;
    private String ragflowInputKey;
    private Integer sort;
    private String enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getRagflowInputKey() { return ragflowInputKey; }
    public void setRagflowInputKey(String ragflowInputKey) { this.ragflowInputKey = ragflowInputKey; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}

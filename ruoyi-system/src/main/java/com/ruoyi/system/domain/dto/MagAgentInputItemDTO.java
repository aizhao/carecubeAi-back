package com.ruoyi.system.domain.dto;

public class MagAgentInputItemDTO
{
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private String value;
    private String ragflowInputKey;
    private Boolean required;

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getRagflowInputKey() { return ragflowInputKey; }
    public void setRagflowInputKey(String ragflowInputKey) { this.ragflowInputKey = ragflowInputKey; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
}

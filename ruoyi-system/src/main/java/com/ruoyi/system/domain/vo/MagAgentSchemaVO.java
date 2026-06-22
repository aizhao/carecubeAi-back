package com.ruoyi.system.domain.vo;

import java.util.List;

public class MagAgentSchemaVO
{
    private String agentCode;
    private String agentName;
    private List<InputFieldVO> fields;

    public static class InputFieldVO
    {
        private String fieldKey;
        private String fieldLabel;
        private String fieldType;
        private Boolean required;
        private String optionsJson;
        private String defaultValue;

        public String getFieldKey() { return fieldKey; }
        public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
        public String getFieldLabel() { return fieldLabel; }
        public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
        public String getFieldType() { return fieldType; }
        public void setFieldType(String fieldType) { this.fieldType = fieldType; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
        public String getOptionsJson() { return optionsJson; }
        public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public List<InputFieldVO> getFields() { return fields; }
    public void setFields(List<InputFieldVO> fields) { this.fields = fields; }
}

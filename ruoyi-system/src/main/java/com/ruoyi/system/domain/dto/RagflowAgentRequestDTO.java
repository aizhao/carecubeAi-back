package com.ruoyi.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class RagflowAgentRequestDTO
{
    private String question;
    private Boolean stream;
    @JsonProperty("session_id")
    private String session_id;
    @JsonProperty("user_id")
    private String user_id;
    @JsonProperty("return_trace")
    private Boolean return_trace;
    private Object release;
    private Map<String, Object> inputs;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
    public String getSession_id() { return session_id; }
    public void setSession_id(String session_id) { this.session_id = session_id; }
    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public Boolean getReturn_trace() { return return_trace; }
    public void setReturn_trace(Boolean return_trace) { this.return_trace = return_trace; }
    public Object getRelease() { return release; }
    public void setRelease(Object release) { this.release = release; }
    public Map<String, Object> getInputs() { return inputs; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }
}

package com.ruoyi.system.ragflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.dto.MagAgentInputItemDTO;
import com.ruoyi.system.domain.dto.MagAgentInvokeCommand;
import com.ruoyi.system.domain.dto.RagflowAgentRequestDTO;
import com.ruoyi.system.domain.vo.MagAgentAttachmentVO;
import com.ruoyi.system.domain.vo.MagAgentReferenceVO;
import com.ruoyi.system.domain.vo.MagAgentSseEventVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class RagflowAgentAdapter
{
    private static final Logger log = LoggerFactory.getLogger(RagflowAgentAdapter.class);
    private static final String DEFAULT_QUESTION = "请基于结构化患者资料进行辅助分析和风险提示。";

    @Autowired
    private RagflowAgentClient ragflowAgentClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void stream(MagAgentInvokeCommand command, Consumer<MagAgentSseEventVO> onEvent)
    {
        RagflowAgentRequestDTO request = buildRequest(command);
        ragflowAgentClient.streamCompletions(command.getAgentId(), request, line -> handleLine(command, line, onEvent));
    }

    private RagflowAgentRequestDTO buildRequest(MagAgentInvokeCommand command)
    {
        Map<String, Object> inputs = new LinkedHashMap<>();
        String query = StringUtils.isNotBlank(command.getMessage()) ? command.getMessage() : null;
        if (command.getInputItems() != null)
        {
            for (MagAgentInputItemDTO item : command.getInputItems())
            {
                if (StringUtils.isNotBlank(item.getRagflowInputKey()))
                {
                    if (StringUtils.isBlank(query)
                            && ("query".equals(item.getRagflowInputKey()) || "query".equals(item.getFieldKey()))
                            && StringUtils.isNotBlank(item.getValue()))
                    {
                        query = item.getValue();
                    }
                    inputs.put(item.getRagflowInputKey(), wrapInput(item.getFieldType(), item.getValue()));
                }
            }
        }
        if (StringUtils.isBlank(query))
        {
            query = DEFAULT_QUESTION;
        }
        if (!inputs.containsKey("query"))
        {
            inputs.put("query", wrapInput("input", query));
        }

        RagflowAgentRequestDTO request = new RagflowAgentRequestDTO();
        request.setQuestion(query);
        request.setStream(true);
        request.setSession_id(command.getRagflowSessionId());
        request.setUser_id(command.getUserKey());
        request.setReturn_trace(false);
        request.setRelease(parseRelease(command.getRelease()));
        request.setInputs(inputs);
        return request;
    }

    private Map<String, Object> wrapInput(String fieldType, Object value)
    {
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("type", toRagflowInputType(fieldType));
        wrapped.put("value", value == null ? "" : value);
        return wrapped;
    }

    private String toRagflowInputType(String fieldType)
    {
        if (StringUtils.isBlank(fieldType))
        {
            return "line";
        }
        switch (fieldType)
        {
            case "textarea":
                return "paragraph";
            case "select":
                return "options";
            case "date":
                return "line";
            default:
                return "line";
        }
    }

    private void handleLine(MagAgentInvokeCommand command, String line, Consumer<MagAgentSseEventVO> onEvent)
    {
        if (StringUtils.isBlank(line) || !line.startsWith("data:"))
        {
            if (StringUtils.isNotBlank(line))
            {
                log.debug("RAGFlow non-data line (skipped): {}", line.length() > 120 ? line.substring(0, 120) + "..." : line);
            }
            return;
        }
        String payload = line.substring(5).trim();
        if (StringUtils.isBlank(payload))
        {
            return;
        }
        if ("[DONE]".equals(payload))
        {
            log.debug("RAGFlow stream: [DONE]");
            onEvent.accept(MagAgentSseEventVO.of("done", command.getBusinessSessionId(), null, null, null));
            return;
        }

        try
        {
            Map<String, Object> raw = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            String eventType = firstText(raw.get("event"), asMap(raw.get("data")).get("event"), raw.get("type"));
            if (StringUtils.isBlank(eventType))
            {
                eventType = "status";
            }
            log.debug("RAGFlow event: {} (line {} chars)", eventType, line.length());
            convertEvent(command, raw, onEvent);
        }
        catch (Exception e)
        {
            log.error("RAGFlow response parse error (line {} chars): {}", line.length(), e.getMessage());
            throw new ServiceException("智能分析响应解析失败");
        }
    }

    @SuppressWarnings("unchecked")
    private void convertEvent(MagAgentInvokeCommand command, Map<String, Object> raw, Consumer<MagAgentSseEventVO> onEvent)
    {
        Map<String, Object> data = asMap(raw.get("data"));
        String event = firstText(raw.get("event"), data.get("event"), raw.get("type"));
        if (StringUtils.isBlank(event))
        {
            event = "status";
        }

        if ("message".equals(event))
        {
            String content = firstText(data.get("content"));
            if (StringUtils.isBlank(content))
            {
                return;
            }
            onEvent.accept(MagAgentSseEventVO.of("message", command.getBusinessSessionId(),
                    firstText(raw.get("message_id")), content, buildSessionData(raw)));
        }
        else if ("message_end".equals(event))
        {
            Map<String, Object> payload = buildSessionData(raw);
            payload.put("references", normalizeReferences(data.get("reference")));
            payload.put("attachments", normalizeAttachments(firstPresent(data.get("attachment"), data.get("attachments"))));
            onEvent.accept(MagAgentSseEventVO.of("message_end", command.getBusinessSessionId(),
                    firstText(raw.get("message_id")), null, payload));
        }
        else if ("node_finished".equals(event))
        {
            String componentName = firstText(data.get("component_name"));
            String componentType = firstText(data.get("component_type"));
            String status = friendlyStatus(componentType, componentName);
            if (StringUtils.isNotBlank(status))
            {
                onEvent.accept(MagAgentSseEventVO.of("status", command.getBusinessSessionId(),
                        firstText(raw.get("message_id")), status, buildSessionData(raw)));
            }

            Map<String, Object> outputs = asMap(data.get("outputs"));
            if (outputs.containsKey("structured"))
            {
                onEvent.accept(MagAgentSseEventVO.of("structured_result", command.getBusinessSessionId(),
                        firstText(raw.get("message_id")), null, outputs.get("structured")));
            }

            log.debug("RAGFlow node_finished: {} (type={}, hasStructured={})",
                    componentName,
                    componentType,
                    outputs.containsKey("structured"));
        }
        else if ("error".equals(event))
        {
            onEvent.accept(MagAgentSseEventVO.of("error", command.getBusinessSessionId(),
                    firstText(raw.get("message_id")), firstText(data.get("error"), raw.get("error"), "智能分析服务异常"), null));
        }
        else
        {
            log.debug("RAGFlow event: {}", event);
            onEvent.accept(MagAgentSseEventVO.of("status", command.getBusinessSessionId(),
                    firstText(raw.get("message_id")), "智能分析处理中", buildSessionData(raw)));
        }
    }

    private Map<String, Object> buildSessionData(Map<String, Object> raw)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("ragflowSessionId", firstText(raw.get("session_id")));
        data.put("taskId", firstText(raw.get("task_id")));
        return data;
    }

    private String friendlyStatus(String componentType, String componentName)
    {
        if (StringUtils.isNotBlank(componentType))
        {
            switch (componentType)
            {
                case "Begin":
                    return "已读取患者资料";
                case "Retrieval":
                    return "知识库检索完成";
                case "Agent":
                    return "辅助分析处理中";
                case "Message":
                    return "正在输出分析结果";
                default:
                    break;
            }
        }
        if (StringUtils.isNotBlank(componentName))
        {
            return componentName + "完成";
        }
        return null;
    }

    private List<MagAgentReferenceVO> normalizeReferences(Object value)
    {
        List<MagAgentReferenceVO> result = new ArrayList<>();
        collectReferences(value, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void collectReferences(Object value, List<MagAgentReferenceVO> result)
    {
        if (value instanceof List)
        {
            for (Object item : (List<?>) value)
            {
                collectReferences(item, result);
            }
            return;
        }
        if (!(value instanceof Map))
        {
            return;
        }

        Map<String, Object> map = (Map<String, Object>) value;
        Object chunks = firstPresent(map.get("chunks"), map.get("doc_aggs"));
        if (chunks instanceof List)
        {
            collectReferences(chunks, result);
            return;
        }
        if (chunks instanceof Map)
        {
            collectReferences(new ArrayList<>(((Map<?, ?>) chunks).values()), result);
            return;
        }

        MagAgentReferenceVO vo = new MagAgentReferenceVO();
        vo.setSourceId(firstText(map.get("document_id"), map.get("doc_id"), map.get("id")));
        vo.setSourceName(firstText(map.get("document_name"), map.get("doc_name"), map.get("name"), map.get("filename")));
        vo.setContent(firstText(map.get("content"), map.get("text")));
        vo.setScore(toDouble(firstPresent(map.get("similarity"), map.get("score"), map.get("relevance"))));
        if (StringUtils.isNotBlank(vo.getSourceName()) || StringUtils.isNotBlank(vo.getContent()))
        {
            result.add(vo);
        }
    }

    private List<MagAgentAttachmentVO> normalizeAttachments(Object value)
    {
        List<MagAgentAttachmentVO> result = new ArrayList<>();
        if (value instanceof List)
        {
            for (Object item : (List<?>) value)
            {
                MagAgentAttachmentVO vo = toAttachment(item);
                if (vo != null)
                {
                    result.add(vo);
                }
            }
        }
        else
        {
            MagAgentAttachmentVO vo = toAttachment(value);
            if (vo != null)
            {
                result.add(vo);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private MagAgentAttachmentVO toAttachment(Object value)
    {
        if (!(value instanceof Map))
        {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) value;
        MagAgentAttachmentVO vo = new MagAgentAttachmentVO();
        vo.setAttachmentId(firstText(map.get("id"), map.get("attachment_id"), map.get("doc_id")));
        vo.setName(firstText(map.get("name"), map.get("filename"), map.get("file_name")));
        vo.setUrl(firstText(map.get("url"), map.get("download_url")));
        vo.setType(firstText(map.get("type"), map.get("mime_type"), map.get("format")));
        return vo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value)
    {
        if (value instanceof Map)
        {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    private Object firstPresent(Object... values)
    {
        for (Object value : values)
        {
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    private Object parseRelease(String release)
    {
        if (StringUtils.isBlank(release))
        {
            return null;
        }
        if ("true".equalsIgnoreCase(release) || "false".equalsIgnoreCase(release))
        {
            return Boolean.valueOf(release);
        }
        return release;
    }

    private String firstText(Object... values)
    {
        Object value = firstPresent(values);
        return value == null ? null : value.toString();
    }

    private Double toDouble(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).doubleValue();
        }
        if (value != null)
        {
            try
            {
                return Double.valueOf(value.toString());
            }
            catch (NumberFormatException ignored)
            {
                return null;
            }
        }
        return null;
    }
}

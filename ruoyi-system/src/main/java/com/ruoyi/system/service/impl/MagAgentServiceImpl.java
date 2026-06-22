package com.ruoyi.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.dto.MagAgentChatRequest;
import com.ruoyi.system.domain.dto.MagAgentInputItemDTO;
import com.ruoyi.system.domain.dto.MagAgentInvokeCommand;
import com.ruoyi.system.domain.mag.MagAgentConfig;
import com.ruoyi.system.domain.mag.MagAgentInputField;
import com.ruoyi.system.domain.mag.MagAgentMessage;
import com.ruoyi.system.domain.mag.MagAgentPatientSnapshot;
import com.ruoyi.system.domain.mag.MagAgentSession;
import com.ruoyi.system.domain.vo.MagAgentMessageVO;
import com.ruoyi.system.domain.vo.MagAgentSseEventVO;
import com.ruoyi.system.domain.vo.MagAgentSchemaVO;
import com.ruoyi.system.domain.vo.MagAgentSessionVO;
import com.ruoyi.system.domain.vo.MagAgentVO;
import com.ruoyi.system.mapper.MagAgentConfigMapper;
import com.ruoyi.system.mapper.MagAgentInputFieldMapper;
import com.ruoyi.system.mapper.MagAgentMessageMapper;
import com.ruoyi.system.mapper.MagAgentPatientSnapshotMapper;
import com.ruoyi.system.mapper.MagAgentSessionMapper;
import com.ruoyi.system.ragflow.RagflowAgentAdapter;
import com.ruoyi.system.service.IMagAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class MagAgentServiceImpl implements IMagAgentService
{
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    @Autowired
    private MagAgentConfigMapper agentConfigMapper;

    @Autowired
    private MagAgentInputFieldMapper inputFieldMapper;

    @Autowired
    private MagAgentSessionMapper sessionMapper;

    @Autowired
    private MagAgentPatientSnapshotMapper snapshotMapper;

    @Autowired
    private MagAgentMessageMapper messageMapper;

    @Autowired
    private RagflowAgentAdapter ragflowAgentAdapter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<MagAgentVO> listAgents()
    {
        List<MagAgentVO> result = new ArrayList<>();
        for (MagAgentConfig config : agentConfigMapper.selectEnabledList())
        {
            MagAgentVO vo = new MagAgentVO();
            vo.setAgentCode(config.getAgentCode());
            vo.setAgentName(config.getAgentName());
            vo.setDescription(config.getDescription());
            result.add(vo);
        }
        return result;
    }

    @Override
    public MagAgentSchemaVO getSchema(String agentCode)
    {
        MagAgentConfig config = requireConfig(agentCode);
        MagAgentSchemaVO vo = new MagAgentSchemaVO();
        vo.setAgentCode(config.getAgentCode());
        vo.setAgentName(config.getAgentName());

        List<MagAgentSchemaVO.InputFieldVO> fields = new ArrayList<>();
        for (MagAgentInputField field : inputFieldMapper.selectEnabledByAgentCode(agentCode))
        {
            MagAgentSchemaVO.InputFieldVO fieldVO = new MagAgentSchemaVO.InputFieldVO();
            fieldVO.setFieldKey(field.getFieldKey());
            fieldVO.setFieldLabel(field.getFieldLabel());
            fieldVO.setFieldType(field.getFieldType());
            fieldVO.setRequired("1".equals(field.getRequired()));
            fieldVO.setOptionsJson(field.getOptionsJson());
            fieldVO.setDefaultValue(field.getDefaultValue());
            fields.add(fieldVO);
        }
        vo.setFields(fields);
        return vo;
    }

    @Override
    @Transactional
    public void streamChat(String agentCode, MagAgentChatRequest request, Long userId, String username, Consumer<MagAgentSseEventVO> onEvent)
    {
        MagAgentConfig config = requireConfig(agentCode);
        MagAgentSession session = loadOrCreateSession(config, request, userId, username);
        List<MagAgentInputItemDTO> inputItems = buildInputItems(agentCode, request);
        saveSnapshotIfNeeded(session, config, inputItems);
        saveUserMessageIfNeeded(session, config, request);

        MagAgentInvokeCommand command = buildCommand(config, session, request, userId, inputItems);
        StreamPersistContext context = new StreamPersistContext(session, config, username);

        try
        {
            ragflowAgentAdapter.stream(command, event -> handleStreamEvent(event, context, onEvent));
        }
        catch (ServiceException e)
        {
            onEvent.accept(MagAgentSseEventVO.of("error", session.getSessionId(), null, e.getMessage(), null));
        }
        catch (Exception e)
        {
            onEvent.accept(MagAgentSseEventVO.of("error", session.getSessionId(), null, "智能分析服务异常", null));
        }
    }

    @Override
    public List<MagAgentSessionVO> listSessions(Long userId, String agentCode)
    {
        List<MagAgentSessionVO> result = new ArrayList<>();
        for (MagAgentSession session : sessionMapper.selectUserSessions(userId, agentCode))
        {
            result.add(toSessionVO(session, false));
        }
        return result;
    }

    @Override
    public MagAgentSessionVO getSession(String sessionId, Long userId)
    {
        MagAgentSession session = sessionMapper.selectBySessionIdAndUser(sessionId, userId);
        if (session == null)
        {
            throw new ServiceException("会话不存在或无权访问");
        }
        return toSessionVO(session, true);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId)
    {
        messageMapper.deleteBySessionIdAndUser(sessionId, userId);
        sessionMapper.deleteBySessionIdAndUser(sessionId, userId);
    }

    private MagAgentConfig requireConfig(String agentCode)
    {
        if (StringUtils.isBlank(agentCode))
        {
            throw new ServiceException("智能分析服务编码不能为空");
        }
        MagAgentConfig config = agentConfigMapper.selectByAgentCode(agentCode);
        if (config == null)
        {
            throw new ServiceException("智能分析服务不存在或未启用");
        }
        return config;
    }

    private MagAgentSession loadOrCreateSession(MagAgentConfig config, MagAgentChatRequest request, Long userId, String username)
    {
        if (request != null && StringUtils.isNotBlank(request.getSessionId()))
        {
            MagAgentSession session = sessionMapper.selectBySessionIdAndUser(request.getSessionId(), userId);
            if (session == null)
            {
                throw new ServiceException("会话不存在或无权访问");
            }
            if (!config.getAgentCode().equals(session.getAgentCode()))
            {
                throw new ServiceException("会话与智能分析服务不匹配");
            }
            return session;
        }

        Date now = new Date();
        MagAgentSession session = new MagAgentSession();
        session.setSessionId(IdUtils.simpleUUID());
        session.setAgentCode(config.getAgentCode());
        session.setUserId(userId);
        session.setSessionTitle(buildSessionTitle(request));
        session.setStatus("active");
        session.setLastMessageTime(now);
        session.setCreateBy(username);
        session.setCreateTime(now);
        sessionMapper.insertSession(session);
        return session;
    }

    private String buildSessionTitle(MagAgentChatRequest request)
    {
        String message = request == null ? null : request.getMessage();
        if (StringUtils.isBlank(message))
        {
            return "辅助分析";
        }
        return message.length() > 30 ? message.substring(0, 30) : message;
    }

    private List<MagAgentInputItemDTO> buildInputItems(String agentCode, MagAgentChatRequest request)
    {
        Map<String, String> inputValues = request == null || request.getInputValues() == null
                ? new HashMap<>() : request.getInputValues();
        boolean followUp = request != null && StringUtils.isNotBlank(request.getSessionId());
        List<MagAgentInputItemDTO> result = new ArrayList<>();
        for (MagAgentInputField field : inputFieldMapper.selectEnabledByAgentCode(agentCode))
        {
            if (followUp && !inputValues.containsKey(field.getFieldKey()))
            {
                continue;
            }
            String value = inputValues.get(field.getFieldKey());
            if (!followUp && StringUtils.isBlank(value))
            {
                value = field.getDefaultValue();
            }
            if (!followUp && "1".equals(field.getRequired()) && StringUtils.isBlank(value))
            {
                throw new ServiceException(field.getFieldLabel() + "不能为空");
            }

            MagAgentInputItemDTO item = new MagAgentInputItemDTO();
            item.setFieldKey(field.getFieldKey());
            item.setFieldLabel(field.getFieldLabel());
            item.setFieldType(field.getFieldType());
            item.setValue(value);
            item.setRagflowInputKey(field.getRagflowInputKey());
            item.setRequired("1".equals(field.getRequired()));
            result.add(item);
        }
        return result;
    }

    private void saveSnapshotIfNeeded(MagAgentSession session, MagAgentConfig config, List<MagAgentInputItemDTO> inputItems)
    {
        if (inputItems == null || inputItems.isEmpty())
        {
            return;
        }
        MagAgentPatientSnapshot snapshot = new MagAgentPatientSnapshot();
        snapshot.setSnapshotId(IdUtils.simpleUUID());
        snapshot.setSessionId(session.getSessionId());
        snapshot.setAgentCode(config.getAgentCode());
        snapshot.setUserId(session.getUserId());
        snapshot.setInputSummary("结构化字段数:" + inputItems.size());
        snapshot.setInputJson(toJson(inputItems));
        snapshot.setCreateTime(new Date());
        snapshotMapper.insertSnapshot(snapshot);
    }

    private void saveUserMessageIfNeeded(MagAgentSession session, MagAgentConfig config, MagAgentChatRequest request)
    {
        if (request == null || StringUtils.isBlank(request.getMessage()))
        {
            return;
        }
        insertMessage(session, config, ROLE_USER, "message", request.getMessage(), null, null, null, null);
    }

    private MagAgentInvokeCommand buildCommand(MagAgentConfig config, MagAgentSession session, MagAgentChatRequest request,
                                               Long userId, List<MagAgentInputItemDTO> inputItems)
    {
        MagAgentInvokeCommand command = new MagAgentInvokeCommand();
        command.setAgentCode(config.getAgentCode());
        command.setAgentId(config.getAgentId());
        command.setRelease(config.getReleaseValue());
        command.setBusinessSessionId(session.getSessionId());
        command.setRagflowSessionId(session.getRagflowSessionId());
        command.setUserId(userId);
        command.setUserKey("ruoyi-" + userId);
        command.setMessage(request == null ? null : request.getMessage());
        command.setInputItems(inputItems);
        return command;
    }

    @SuppressWarnings("unchecked")
    private void handleStreamEvent(MagAgentSseEventVO event, StreamPersistContext context, Consumer<MagAgentSseEventVO> onEvent)
    {
        Map<String, Object> raw = event.getData() instanceof Map ? (Map<String, Object>) event.getData() : null;
        syncRagflowSessionId(context, raw);

        if ("message".equals(event.getType()))
        {
            rememberRagflowMessageId(context, event.getMessageId());
            String content = event.getContent();
            if (StringUtils.isNotBlank(content))
            {
                context.answer.append(content);
            }
            onEvent.accept(event);
            return;
        }

        if ("message_end".equals(event.getType()))
        {
            rememberRagflowMessageId(context, event.getMessageId());
            if (raw != null)
            {
                context.referenceJson = toJson(raw.get("references"));
                context.attachmentJson = toJson(raw.get("attachments"));
            }
            onEvent.accept(event);
            return;
        }

        if ("structured_result".equals(event.getType()))
        {
            rememberRagflowMessageId(context, event.getMessageId());
            if (event.getData() != null)
            {
                insertMessage(context.session, context.config, ROLE_ASSISTANT, "structured_result",
                        null, toJson(event.getData()), null, null, event.getMessageId());
            }
            onEvent.accept(event);
            return;
        }

        if ("done".equals(event.getType()))
        {
            saveAssistantAnswer(context);
            touchSession(context);
            onEvent.accept(event);
            return;
        }

        onEvent.accept(event);
    }

    private void syncRagflowSessionId(StreamPersistContext context, Map<String, Object> data)
    {
        if (data == null || StringUtils.isNotBlank(context.session.getRagflowSessionId()))
        {
            return;
        }
        Object ragflowSessionId = data.get("ragflowSessionId");
        if (ragflowSessionId != null && StringUtils.isNotBlank(ragflowSessionId.toString()))
        {
            context.session.setRagflowSessionId(ragflowSessionId.toString());
        }
    }

    private void rememberRagflowMessageId(StreamPersistContext context, String messageId)
    {
        if (StringUtils.isNotBlank(messageId))
        {
            context.ragflowMessageId = messageId;
        }
    }

    private void saveAssistantAnswer(StreamPersistContext context)
    {
        String content = context.answer.toString().trim();
        if (StringUtils.isBlank(content))
        {
            return;
        }
        insertMessage(context.session, context.config, ROLE_ASSISTANT, "message", content,
                null, context.referenceJson, context.attachmentJson, context.ragflowMessageId);
    }

    private void touchSession(StreamPersistContext context)
    {
        context.session.setLastMessageTime(new Date());
        context.session.setUpdateBy(context.username);
        context.session.setUpdateTime(new Date());
        sessionMapper.updateSession(context.session);
    }

    private void insertMessage(MagAgentSession session, MagAgentConfig config, String role, String eventType, String content,
                               String structuredJson, String referenceJson, String attachmentJson, String ragflowMessageId)
    {
        MagAgentMessage message = new MagAgentMessage();
        message.setMessageId(IdUtils.simpleUUID());
        message.setSessionId(session.getSessionId());
        message.setAgentCode(config.getAgentCode());
        message.setUserId(session.getUserId());
        message.setRole(role);
        message.setContent(content);
        message.setEventType(eventType);
        message.setStructuredJson(structuredJson);
        message.setReferenceJson(referenceJson);
        message.setAttachmentJson(attachmentJson);
        message.setRagflowMessageId(ragflowMessageId);
        message.setCreateTime(new Date());
        messageMapper.insertMessage(message);
    }

    private MagAgentSessionVO toSessionVO(MagAgentSession session, boolean withMessages)
    {
        MagAgentSessionVO vo = new MagAgentSessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setAgentCode(session.getAgentCode());
        vo.setSessionTitle(session.getSessionTitle());
        vo.setStatus(session.getStatus());
        vo.setLastMessageTime(session.getLastMessageTime());
        vo.setCreateTime(session.getCreateTime());
        if (withMessages)
        {
            List<MagAgentMessageVO> messages = new ArrayList<>();
            for (MagAgentMessage message : messageMapper.selectBySessionId(session.getSessionId(), session.getUserId()))
            {
                MagAgentMessageVO messageVO = new MagAgentMessageVO();
                messageVO.setMessageId(message.getMessageId());
                messageVO.setRole(message.getRole());
                messageVO.setContent(message.getContent());
                messageVO.setEventType(message.getEventType());
                messageVO.setStructuredJson(message.getStructuredJson());
                messageVO.setReferenceJson(message.getReferenceJson());
                messageVO.setAttachmentJson(message.getAttachmentJson());
                messageVO.setCreateTime(message.getCreateTime());
                messages.add(messageVO);
            }
            vo.setMessages(messages);
        }
        return vo;
    }

    private String toJson(Object value)
    {
        if (value == null)
        {
            return null;
        }
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("智能分析数据保存失败");
        }
    }

    private static class StreamPersistContext
    {
        private final MagAgentSession session;
        private final MagAgentConfig config;
        private final String username;
        private final StringBuilder answer = new StringBuilder();
        private String referenceJson;
        private String attachmentJson;
        private String ragflowMessageId;

        private StreamPersistContext(MagAgentSession session, MagAgentConfig config, String username)
        {
            this.session = session;
            this.config = config;
            this.username = username;
        }
    }
}

package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.MagAgentChatRequest;
import com.ruoyi.system.domain.vo.MagAgentSseEventVO;
import com.ruoyi.system.domain.vo.MagAgentSchemaVO;
import com.ruoyi.system.domain.vo.MagAgentSessionVO;
import com.ruoyi.system.domain.vo.MagAgentVO;

import java.util.List;
import java.util.function.Consumer;

public interface IMagAgentService
{
    List<MagAgentVO> listAgents();

    MagAgentSchemaVO getSchema(String agentCode);

    void streamChat(String agentCode, MagAgentChatRequest request, Long userId, String username, Consumer<MagAgentSseEventVO> onEvent);

    List<MagAgentSessionVO> listSessions(Long userId, String agentCode);

    MagAgentSessionVO getSession(String sessionId, Long userId);

    void deleteSession(String sessionId, Long userId);
}

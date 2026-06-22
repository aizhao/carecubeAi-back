package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mag.MagAgentSession;

public interface MagAgentSessionMapper
{
    int insertSession(MagAgentSession session);
    int updateSession(MagAgentSession session);
    MagAgentSession selectBySessionId(@Param("sessionId") String sessionId);
    MagAgentSession selectBySessionIdAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
    List<MagAgentSession> selectUserSessions(@Param("userId") Long userId, @Param("agentCode") String agentCode);
    int deleteBySessionIdAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}

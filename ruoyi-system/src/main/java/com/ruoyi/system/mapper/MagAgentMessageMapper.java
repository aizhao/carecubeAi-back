package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mag.MagAgentMessage;

public interface MagAgentMessageMapper
{
    int insertMessage(MagAgentMessage message);
    List<MagAgentMessage> selectBySessionId(@Param("sessionId") String sessionId, @Param("userId") Long userId);
    int deleteBySessionIdAndUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}

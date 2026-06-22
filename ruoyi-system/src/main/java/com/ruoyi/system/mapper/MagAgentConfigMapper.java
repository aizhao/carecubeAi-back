package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.mag.MagAgentConfig;
import org.apache.ibatis.annotations.Param;

public interface MagAgentConfigMapper
{
    List<MagAgentConfig> selectEnabledList();
    MagAgentConfig selectByAgentCode(@Param("agentCode") String agentCode);
}

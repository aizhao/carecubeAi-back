package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.mag.MagAgentInputField;
import org.apache.ibatis.annotations.Param;

public interface MagAgentInputFieldMapper
{
    List<MagAgentInputField> selectEnabledByAgentCode(@Param("agentCode") String agentCode);
}

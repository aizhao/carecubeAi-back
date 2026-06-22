package com.ruoyi.web.controller.mag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.dto.MagAgentChatRequest;
import com.ruoyi.system.domain.vo.MagAgentSseEventVO;
import com.ruoyi.system.service.IMagAgentService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/mag/agent")
public class MagAgentController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(MagAgentController.class);

    @Autowired
    private IMagAgentService magAgentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PreAuthorize("@ss.hasPermi('mag:agent:list')")
    @GetMapping("/list")
    public AjaxResult list()
    {
        return success(magAgentService.listAgents());
    }

    @PreAuthorize("@ss.hasPermi('mag:agent:query')")
    @GetMapping("/{agentCode}/schema")
    public AjaxResult schema(@PathVariable String agentCode)
    {
        return success(magAgentService.getSchema(agentCode));
    }

    @PreAuthorize("@ss.hasPermi('mag:agent:chat')")
    @PostMapping(value = "/{agentCode}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamChat(@PathVariable String agentCode, @RequestBody MagAgentChatRequest request, HttpServletResponse response) throws IOException
    {
        prepareSse(response);
        try
        {
            Long userId = SecurityUtils.getUserId();
            String username = SecurityUtils.getUsername();
            magAgentService.streamChat(agentCode, request, userId, username, event -> writeEvent(response, event));
        }
        catch (ServiceException e)
        {
            writeEvent(response, MagAgentSseEventVO.of("error", null, null, e.getMessage(), null));
        }
        catch (Exception e)
        {
            writeEvent(response, MagAgentSseEventVO.of("error", null, null, "智能分析服务异常", null));
        }
    }

    @PreAuthorize("@ss.hasPermi('mag:agent:query')")
    @GetMapping("/session/list")
    public AjaxResult sessionList(@RequestParam(required = false) String agentCode)
    {
        return success(magAgentService.listSessions(SecurityUtils.getUserId(), agentCode));
    }

    @PreAuthorize("@ss.hasPermi('mag:agent:query')")
    @GetMapping("/session/{sessionId}")
    public AjaxResult sessionDetail(@PathVariable String sessionId)
    {
        return success(magAgentService.getSession(sessionId, SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('mag:agent:remove')")
    @Log(title = "智能分析会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/session/{sessionId}")
    public AjaxResult deleteSession(@PathVariable String sessionId)
    {
        magAgentService.deleteSession(sessionId, SecurityUtils.getUserId());
        return success();
    }

    private void prepareSse(HttpServletResponse response)
    {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private void writeEvent(HttpServletResponse response, MagAgentSseEventVO event)
    {
        try
        {
            String json = objectMapper.writeValueAsString(event);
            log.debug("SSE write: type={}, jsonLen={}, contentLen={}",
                    event.getType(), json.length(),
                    event.getContent() != null ? event.getContent().length() : 0);
            response.getWriter().write("event: " + event.getType() + "\n");
            response.getWriter().write("data: " + json + "\n\n");
            response.getWriter().flush();
        }
        catch (IOException e)
        {
            log.warn("SSE write failed for event type={}: {}", event.getType(), e.getMessage());
        }
    }
}

package com.ruoyi.web.controller.mag;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.IChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/mag/chat")
public class RagFlowChatController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(RagFlowChatController.class);

    @Autowired
    private IChatService chatService;

    // ========== Chat CRUD ==========

    @PreAuthorize("@ss.hasPermi('mag:chat:list')")
    @GetMapping("/list")
    public TableDataInfo listChats(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keywords)
    {
        try
        {
            Map<String, Object> result = chatService.listChats(page, pageSize, keywords);
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            Object data = result.get("data");
            if (data instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                rspData.setRows((List<?>) dataMap.getOrDefault("chats", Collections.emptyList()));
                Object totalObj = dataMap.get("total");
                rspData.setTotal(totalObj instanceof Number ? ((Number) totalObj).longValue() : 0);
            }
            return rspData;
        }
        catch (ServiceException e)
        {
            log.warn("查询聊天助手列表失败: {}", e.getMessage());
            return emptyTableData();
        }
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:query')")
    @GetMapping("/{chatId}")
    public AjaxResult getChat(@PathVariable String chatId)
    {
        Map<String, Object> result = chatService.getChat(chatId);
        return success(result.get("data"));
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:add')")
    @Log(title = "聊天助手", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult createChat(@RequestBody Map<String, Object> params)
    {
        Map<String, Object> result = chatService.createChat(params);
        return success(result.get("data"));
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:edit')")
    @Log(title = "聊天助手", businessType = BusinessType.UPDATE)
    @PutMapping("/{chatId}")
    public AjaxResult updateChat(@PathVariable String chatId, @RequestBody Map<String, Object> params)
    {
        Map<String, Object> result = chatService.updateChat(chatId, params);
        return success(result.get("data"));
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:remove')")
    @Log(title = "聊天助手", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult deleteChats(@PathVariable String ids)
    {
        chatService.deleteChats(Arrays.asList(ids.split(",")));
        return success();
    }

    // ========== Session Management ==========

    @PreAuthorize("@ss.hasPermi('mag:chat:query')")
    @GetMapping("/{chatId}/sessions")
    public TableDataInfo listSessions(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        String userId = String.valueOf(SecurityUtils.getUserId());
        try
        {
            Map<String, Object> result = chatService.listSessions(chatId, page, pageSize, userId);
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            Object data = result.get("data");
            if (data instanceof List)
            {
                @SuppressWarnings("unchecked")
                List<?> list = (List<?>) data;
                rspData.setRows(list);
                rspData.setTotal(list.size());
            }
            return rspData;
        }
        catch (ServiceException e)
        {
            log.warn("查询会话列表失败: {}", e.getMessage());
            return emptyTableData();
        }
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:add')")
    @Log(title = "聊天会话", businessType = BusinessType.INSERT)
    @PostMapping("/{chatId}/sessions")
    public AjaxResult createSession(@PathVariable String chatId, @RequestBody Map<String, Object> params)
    {
        String userId = String.valueOf(SecurityUtils.getUserId());
        String name = (String) params.getOrDefault("name", "新会话");
        Map<String, Object> result = chatService.createSession(chatId, name, userId);
        return success(result.get("data"));
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:query')")
    @GetMapping("/{chatId}/sessions/{sessionId}")
    public AjaxResult getSession(@PathVariable String chatId, @PathVariable String sessionId)
    {
        Map<String, Object> result = chatService.getSession(chatId, sessionId);
        return success(result.get("data"));
    }

    @PreAuthorize("@ss.hasPermi('mag:chat:remove')")
    @Log(title = "聊天会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/{chatId}/sessions/{ids}")
    public AjaxResult deleteSessions(@PathVariable String chatId, @PathVariable String ids)
    {
        chatService.deleteSessions(chatId, Arrays.asList(ids.split(",")));
        return success();
    }

    // ========== Document Download ==========

    @PreAuthorize("@ss.hasPermi('mag:chat:query')")
    @GetMapping("/document/download/{datasetId}/{documentId}")
    public void downloadDocument(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            HttpServletResponse response) throws IOException
    {
        byte[] content = chatService.downloadDocument(datasetId, documentId);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + documentId + "\"");
        response.setContentLength(content != null ? content.length : 0);
        if (content != null)
        {
            response.getOutputStream().write(content);
        }
        response.getOutputStream().flush();
    }

    // ========== SSE Streaming Chat ==========

    @PreAuthorize("@ss.hasPermi('mag:chat:query')")
    @PostMapping("/{chatId}/send")
    public void sendMessage(
            @PathVariable String chatId,
            @RequestBody Map<String, Object> params,
            HttpServletResponse response) throws IOException
    {
        String sessionId = (String) params.get("sessionId");
        String question = (String) params.get("question");

        if (sessionId == null || sessionId.isBlank())
        {
            throw new ServiceException("sessionId不能为空");
        }
        if (question == null || question.isBlank())
        {
            throw new ServiceException("question不能为空");
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        try
        {
            chatService.sendMessage(chatId, sessionId, question, line -> {
                try
                {
                    if (line.startsWith("data:"))
                    {
                        response.getWriter().write(line + "\n\n");
                        response.getWriter().flush();
                    }
                    else if (!line.isEmpty())
                    {
                        response.getWriter().write("data:" + line + "\n\n");
                        response.getWriter().flush();
                    }
                }
                catch (IOException e)
                {
                    // Client disconnected, stop processing
                    throw new RuntimeException(e);
                }
            });
            response.getWriter().write("data:[DONE]\n\n");
            response.getWriter().flush();
        }
        catch (ServiceException e)
        {
            try
            {
                response.getWriter().write("data:{\"error\":\"" + e.getMessage() + "\"}\n\n");
                response.getWriter().flush();
            }
            catch (IOException ignored)
            {
                // Client may have disconnected
            }
        }
        catch (RuntimeException e)
        {
            if (!(e.getCause() instanceof IOException))
            {
                throw e;
            }
            // Client disconnected, no-op
        }
    }

    private TableDataInfo emptyTableData()
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(Collections.emptyList());
        rspData.setTotal(0);
        return rspData;
    }
}

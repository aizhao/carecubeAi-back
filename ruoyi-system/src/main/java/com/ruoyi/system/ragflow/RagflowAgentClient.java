package com.ruoyi.system.ragflow;

import com.ruoyi.common.config.RagFlowConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.dto.RagflowAgentRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Component
public class RagflowAgentClient
{
    private static final Logger log = LoggerFactory.getLogger(RagflowAgentClient.class);
    private static final MappingJackson2HttpMessageConverter jsonConverter =
            new MappingJackson2HttpMessageConverter();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RagFlowConfig ragFlowConfig;

    public void streamCompletions(String agentId, RagflowAgentRequestDTO requestBody, Consumer<String> onLine)
    {
        if (StringUtils.isBlank(ragFlowConfig.getUrl()) || StringUtils.isBlank(ragFlowConfig.getApiKey()))
        {
            throw new ServiceException("智能分析服务配置不完整");
        }
        if (StringUtils.isBlank(agentId))
        {
            throw new ServiceException("智能分析服务未配置智能体");
        }

        String url = ragFlowConfig.getUrl() + "/api/v1/agents/" + agentId + "/completions";
        try
        {
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        request.getHeaders().setBearerAuth(ragFlowConfig.getApiKey());
                        request.getHeaders().set("Connection", "keep-alive");
                        jsonConverter.write(requestBody, MediaType.APPLICATION_JSON, request);
                    },
                    response -> {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
                        String line;
                        StringBuilder dataBlock = null;
                        while ((line = reader.readLine()) != null)
                        {
                            if (line.startsWith("data:"))
                            {
                                // New data block starts — flush previous if any
                                if (dataBlock != null)
                                {
                                    onLine.accept(dataBlock.toString().trim());
                                    dataBlock = null;
                                }
                                String payload = line.substring(5);
                                // Check if this is a self-contained single-line data (compact JSON or [DONE])
                                if (payload.trim().startsWith("{"))
                                {
                                    // Could be compact JSON or the start of multi-line JSON
                                    // Remove leading whitespace and check if it's complete
                                    String trimmed = payload.trim();
                                    if (isCompleteJson(trimmed))
                                    {
                                        // Compact single-line JSON
                                        onLine.accept("data: " + trimmed);
                                    }
                                    else
                                    {
                                        // Start of multi-line JSON — accumulate
                                        dataBlock = new StringBuilder("data: " + trimmed);
                                    }
                                }
                                else
                                {
                                    // [DONE] or other non-JSON
                                    onLine.accept(line);
                                }
                            }
                            else if (dataBlock != null)
                            {
                                // Continuation of a multi-line data block
                                String trimmed = line.trim();
                                if (trimmed.isEmpty())
                                {
                                    // Blank line — block complete
                                    onLine.accept(dataBlock.toString().trim());
                                    dataBlock = null;
                                }
                                else
                                {
                                    dataBlock.append(' ').append(trimmed);
                                }
                            }
                            // else: lines outside data blocks are skipped (e.g., "event:" lines)
                        }
                        // Flush remaining block at stream end
                        if (dataBlock != null)
                        {
                            onLine.accept(dataBlock.toString().trim());
                        }
                        return null;
                    }
            );
        }
        catch (RestClientException e)
        {
            log.error("调用 RAGFlow Agent Completions 失败，agentCode 内部映射已隐藏", e);
            throw new ServiceException("智能分析服务调用失败");
        }
    }

    /**
     * Quick check whether a JSON string is complete by counting braces.
     * This avoids attempting to accumulate single-line compact JSON unnecessarily.
     */
    private boolean isCompleteJson(String s)
    {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (escaped)
            {
                escaped = false;
                continue;
            }
            if (c == '\\')
            {
                escaped = true;
                continue;
            }
            if (c == '"')
            {
                inString = !inString;
                continue;
            }
            if (!inString)
            {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
            }
        }
        return depth == 0 && s.charAt(0) == '{';
    }
}

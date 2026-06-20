package com.ruoyi.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.config.RagFlowConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Service
public class ChatServiceImpl implements IChatService
{
    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RagFlowConfig ragFlowConfig;

    private static final MappingJackson2HttpMessageConverter jsonConverter =
            new MappingJackson2HttpMessageConverter();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String apiUrl() {
        return ragFlowConfig.getUrl() + "/api/v1";
    }

    // ========== Chat CRUD ==========

    @Override
    public Map<String, Object> listChats(int page, int pageSize, String keywords)
    {
        StringBuilder url = new StringBuilder(apiUrl() + "/chats?page=" + page + "&page_size=" + pageSize);
        if (keywords != null && !keywords.isEmpty()) {
            try {
                url.append("&keywords=").append(URLEncoder.encode(keywords, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                url.append("&keywords=").append(keywords);
            }
        }
        return executeGet(url.toString());
    }

    @Override
    public Map<String, Object> getChat(String chatId)
    {
        return executeGet(apiUrl() + "/chats/" + chatId);
    }

    @Override
    public Map<String, Object> createChat(Map<String, Object> params)
    {
        return executePost(apiUrl() + "/chats", params);
    }

    @Override
    public Map<String, Object> updateChat(String chatId, Map<String, Object> params)
    {
        return executePatch(apiUrl() + "/chats/" + chatId, params);
    }

    @Override
    public void deleteChats(List<String> ids)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        executeDelete(apiUrl() + "/chats", body);
    }

    // ========== Session Management ==========

    @Override
    public Map<String, Object> listSessions(String chatId, int page, int pageSize, String userId)
    {
        StringBuilder url = new StringBuilder(apiUrl() + "/chats/" + chatId + "/sessions?page=" + page + "&page_size=" + pageSize);
        if (userId != null && !userId.isEmpty()) {
            try {
                url.append("&user_id=").append(URLEncoder.encode(userId, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                url.append("&user_id=").append(userId);
            }
        }
        return executeGet(url.toString());
    }

    @Override
    public Map<String, Object> createSession(String chatId, String name, String userId)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("user_id", userId);
        return executePost(apiUrl() + "/chats/" + chatId + "/sessions", body);
    }

    @Override
    public Map<String, Object> getSession(String chatId, String sessionId)
    {
        return executeGet(apiUrl() + "/chats/" + chatId + "/sessions/" + sessionId);
    }

    @Override
    public void deleteSessions(String chatId, List<String> sessionIds)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", sessionIds);
        executeDelete(apiUrl() + "/chats/" + chatId + "/sessions", body);
    }

    // ========== SSE Streaming ==========

    @Override
    public void sendMessage(String chatId, String sessionId, String question, Consumer<String> onLine)
    {
        String url = apiUrl() + "/chats/" + chatId + "/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        body.put("stream", true);
        body.put("session_id", sessionId);

        try
        {
            restTemplate.execute(
                    url,
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        request.getHeaders().setBearerAuth(ragFlowConfig.getApiKey());
                        request.getHeaders().set("Connection", "keep-alive");
                        jsonConverter.write(body, MediaType.APPLICATION_JSON, request);
                    },
                    clientHttpResponse -> {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null)
                        {
                            onLine.accept(line);
                        }
                        return null;
                    }
            );
        }
        catch (RestClientException e)
        {
            log.error("调用 RAGFlow Chat Completions 失败", e);
            throw new ServiceException("聊天服务调用失败: " + e.getMessage());
        }
    }

    // ========== Document Download ==========

    @Override
    public byte[] downloadDocument(String datasetId, String documentId)
    {
        byte[] content = downloadDocumentFromDataset(datasetId, documentId);
        Map<String, Object> errorBody = parseRagFlowError(content);
        if (errorBody == null)
        {
            return content;
        }

        String resolvedDatasetId = resolveDatasetIdByDocument(documentId);
        if (resolvedDatasetId != null && !resolvedDatasetId.equals(datasetId))
        {
            content = downloadDocumentFromDataset(resolvedDatasetId, documentId);
            errorBody = parseRagFlowError(content);
            if (errorBody == null)
            {
                return content;
            }
        }

        Object message = errorBody.get("message");
        throw new ServiceException("下载文档失败: " + (message != null ? message.toString() : "RAGFlow 返回错误"));
    }

    private byte[] downloadDocumentFromDataset(String datasetId, String documentId)
    {
        if (datasetId == null || datasetId.isBlank())
        {
            throw new ServiceException("下载文档失败: datasetId 为空");
        }
        String url = apiUrl() + "/datasets/" + datasetId + "/documents/" + documentId;
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(ragFlowConfig.getApiKey());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, byte[].class);
            return response.getBody();
        }
        catch (RestClientException e)
        {
            log.error("下载文档失败: {}", url, e);
            throw new ServiceException("下载文档失败: " + e.getMessage());
        }
    }

    private Map<String, Object> parseRagFlowError(byte[] content)
    {
        if (content == null || content.length == 0)
        {
            return null;
        }
        int first = 0;
        while (first < content.length && Character.isWhitespace((char) content[first]))
        {
            first++;
        }
        if (first >= content.length || content[first] != '{')
        {
            return null;
        }
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(content, Map.class);
            Object code = body.get("code");
            if (code instanceof Number && ((Number) code).intValue() != 0)
            {
                return body;
            }
            return null;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String resolveDatasetIdByDocument(String documentId)
    {
        Map<String, Object> datasetsResponse = executeGet(apiUrl() + "/datasets?page=1&page_size=200");
        Object data = datasetsResponse.get("data");
        if (!(data instanceof List))
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> datasets = (List<Map<String, Object>>) data;
        for (Map<String, Object> dataset : datasets)
        {
            Object id = dataset.get("id");
            if (id == null)
            {
                continue;
            }
            String datasetId = id.toString();
            try
            {
                String url = apiUrl() + "/datasets/" + datasetId + "/documents?page=1&page_size=1&id=" + encode(documentId);
                Map<String, Object> docsResponse = executeGet(url);
                Object docsData = docsResponse.get("data");
                if (docsData instanceof Map)
                {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> docsMap = (Map<String, Object>) docsData;
                    Object docs = docsMap.get("docs");
                    if (docs instanceof List && !((List<?>) docs).isEmpty())
                    {
                        return datasetId;
                    }
                }
            }
            catch (ServiceException e)
            {
                log.debug("按数据集 {} 查询文档 {} 失败: {}", datasetId, documentId, e.getMessage());
            }
        }
        return null;
    }

    private String encode(String value)
    {
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            return value;
        }
    }

    // ========== HTTP Helpers ==========

    private HttpHeaders buildHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ragFlowConfig.getApiKey());
        return headers;
    }

    private void checkRagFlowResponse(Map<String, Object> response)
    {
        if (response == null)
        {
            throw new ServiceException("聊天助手服务返回空响应");
        }
        Object code = response.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0)
        {
            Object message = response.get("message");
            throw new ServiceException("聊天助手服务错误: " + (message != null ? message.toString() : "未知错误"));
        }
    }

    private Map<String, Object> executeGet(String url)
    {
        try
        {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            checkRagFlowResponse(body);
            return body;
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow GET 请求失败: {}", url, e);
            throw new ServiceException("调用聊天助手服务失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executePost(String url, Map<String, Object> requestBody)
    {
        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            checkRagFlowResponse(body);
            return body;
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow POST 请求失败: {}", url, e);
            throw new ServiceException("调用聊天助手服务失败: " + e.getMessage());
        }
    }

    private Map<String, Object> executePatch(String url, Map<String, Object> requestBody)
    {
        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> body = response.getBody();
            checkRagFlowResponse(body);
            return body;
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow PATCH 请求失败: {}", url, e);
            throw new ServiceException("调用聊天助手服务失败: " + e.getMessage());
        }
    }

    private void executeDelete(String url, Map<String, Object> body)
    {
        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            checkRagFlowResponse(response.getBody());
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow DELETE 请求失败: {}", url, e);
            throw new ServiceException("调用聊天助手服务失败: " + e.getMessage());
        }
    }
}

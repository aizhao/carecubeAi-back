package com.ruoyi.system.service.impl;

import com.ruoyi.common.config.RagFlowConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IKnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 知识库管理 Service — 代理 RAGFlow HTTP API
 *
 * @author ruoyi
 */
@Service
public class KnowledgeBaseServiceImpl implements IKnowledgeBaseService
{
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RagFlowConfig ragFlowConfig;

    // ========== 知识库（Dataset）CRUD ==========

    @Override
    public List<Map<String, Object>> listDatasets(int pageNum, int pageSize, String name, String id)
    {
        StringBuilder url = new StringBuilder(ragFlowConfig.getUrl())
                .append("/api/v1/datasets?page=").append(pageNum)
                .append("&page_size=").append(pageSize)
                .append("&orderby=create_time&desc=true");
        appendParam(url, "name", name);
        appendParam(url, "id", id);

        Map<String, Object> response = executeGet(url.toString());
        // RAGFlow 返回格式: {"code": 0, "data": [...], "total_datasets": N}
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("data");
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public long countDatasets(String name, String id)
    {
        StringBuilder url = new StringBuilder(ragFlowConfig.getUrl())
                .append("/api/v1/datasets?page=1&page_size=1");
        appendParam(url, "name", name);
        appendParam(url, "id", id);

        Map<String, Object> response = executeGet(url.toString());
        Object total = response.get("total_datasets");
        if (total instanceof Number) {
            return ((Number) total).longValue();
        }
        return 0;
    }

    @Override
    public Map<String, Object> getDataset(String datasetId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets?id=" + datasetId;
        Map<String, Object> response = executeGet(url);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("data");
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        throw new ServiceException("知识库不存在");
    }

    @Override
    public Map<String, Object> createDataset(Map<String, Object> params)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets";
        Map<String, Object> ragFlowBody = new HashMap<>();
        ragFlowBody.put("name", params.getOrDefault("name", ""));
        ragFlowBody.put("description", params.getOrDefault("description", ""));
        ragFlowBody.put("chunk_method", params.getOrDefault("chunk_method", "naive"));
        ragFlowBody.put("permission", params.getOrDefault("permission", "me"));

        Map<String, Object> response = executePost(url, ragFlowBody);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return data != null ? data : new HashMap<>();
    }

    @Override
    public void updateDataset(String datasetId, Map<String, Object> params)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId;
        Map<String, Object> ragFlowBody = new HashMap<>();
        if (params.containsKey("name")) {
            ragFlowBody.put("name", params.get("name"));
        }
        if (params.containsKey("description")) {
            ragFlowBody.put("description", params.get("description"));
        }
        if (params.containsKey("chunk_method")) {
            ragFlowBody.put("chunk_method", params.get("chunk_method"));
        }
        if (params.containsKey("permission")) {
            ragFlowBody.put("permission", params.get("permission"));
        }
        executePut(url, ragFlowBody);
    }

    @Override
    public void deleteDatasets(List<String> ids)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets";
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        executeDelete(url, body);
    }

    // ========== 文件（Document）CRUD ==========

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listDocuments(String datasetId, int pageNum, int pageSize,
                                                    String keywords, String name, String id)
    {
        StringBuilder url = new StringBuilder(ragFlowConfig.getUrl())
                .append("/api/v1/datasets/").append(datasetId)
                .append("/documents?page=").append(pageNum)
                .append("&page_size=").append(pageSize);
        appendParam(url, "keywords", keywords);
        appendParam(url, "name", name);
        appendParam(url, "id", id);

        Map<String, Object> response = executeGet(url.toString());
        // RAGFlow 返回格式: {"code": 0, "data": {"docs": [...], "total": N}}
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
            List<Map<String, Object>> docs = (List<Map<String, Object>>) data.get("docs");
            return docs != null ? docs : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public long countDocuments(String datasetId, String keywords, String name, String id)
    {
        StringBuilder url = new StringBuilder(ragFlowConfig.getUrl())
                .append("/api/v1/datasets/").append(datasetId)
                .append("/documents?page=1&page_size=1");
        appendParam(url, "keywords", keywords);
        appendParam(url, "name", name);
        appendParam(url, "id", id);

        Map<String, Object> response = executeGet(url.toString());
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
            Object total = data.get("total");
            if (total instanceof Number) {
                return ((Number) total).longValue();
            }
        }
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadDocument(String datasetId, MultipartFile file)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId + "/documents";

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(ragFlowConfig.getApiKey());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes())
            {
                @Override
                public String getFilename()
                {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            checkRagFlowResponse(responseBody);

            // RAGFlow 返回: {"code": 0, "data": [{...}]}
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
            if (dataList != null && !dataList.isEmpty()) {
                return dataList.get(0);
            }
            return new HashMap<>();
        }
        catch (Exception e)
        {
            log.error("上传文件到RAGFlow失败", e);
            throw new ServiceException("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocuments(String datasetId, List<String> ids)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId + "/documents";
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        executeDelete(url, body);
    }

    @Override
    public byte[] downloadDocument(String datasetId, String documentId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId + "/documents/" + documentId;
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
            log.error("下载RAGFlow文件失败", e);
            throw new ServiceException("下载文件失败: " + e.getMessage());
        }
    }

    // ========== 文档高级操作 ==========

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDocument(String datasetId, String documentId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId
                + "/documents?id=" + documentId;
        Map<String, Object> response = executeGet(url);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
            List<Map<String, Object>> docs = (List<Map<String, Object>>) data.get("docs");
            if (docs != null && !docs.isEmpty()) {
                return docs.get(0);
            }
        }
        throw new ServiceException("文档不存在");
    }

    @Override
    public void updateDocument(String datasetId, String documentId, Map<String, Object> params)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId
                + "/documents/" + documentId;
        Map<String, Object> body = new HashMap<>();
        // 支持更新 name, enabled, chunk_method, parser_config 等字段
        if (params.containsKey("name")) {
            body.put("name", params.get("name"));
        }
        if (params.containsKey("enabled")) {
            body.put("enabled", params.get("enabled"));
        }
        if (params.containsKey("chunk_method")) {
            body.put("chunk_method", params.get("chunk_method"));
        }
        if (params.containsKey("parser_config")) {
            body.put("parser_config", params.get("parser_config"));
        }
        if (params.containsKey("chunk_count")) {
            body.put("chunk_count", params.get("chunk_count"));
        }
        executePut(url, body);
    }

    @Override
    public void parseDocuments(String datasetId, List<String> documentIds)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId + "/chunks";
        Map<String, Object> body = new HashMap<>();
        body.put("document_ids", documentIds);
        executePost(url, body);
    }

    @Override
    public void stopParsing(String datasetId, List<String> documentIds)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/datasets/" + datasetId + "/chunks";
        Map<String, Object> body = new HashMap<>();
        body.put("document_ids", documentIds);
        executeDelete(url, body);
    }

    // ========== 内部辅助方法 ==========

    private String encode(String value) {
        if (value == null || value.isEmpty()) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void appendParam(StringBuilder url, String name, String value) {
        if (value != null && !value.isEmpty()) {
            url.append("&").append(name).append("=").append(encode(value));
        }
    }

    private HttpHeaders buildHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ragFlowConfig.getApiKey());
        return headers;
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
            throw new ServiceException("知识库服务请求失败: " + e.getMessage());
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
            throw new ServiceException("知识库服务请求失败: " + e.getMessage());
        }
    }

    private void executePut(String url, Map<String, Object> requestBody)
    {
        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            checkRagFlowResponse(response.getBody());
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow PUT 请求失败: {}", url, e);
            throw new ServiceException("知识库服务请求失败: " + e.getMessage());
        }
    }

    private void executeDelete(String url, Map<String, Object> requestBody)
    {
        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            checkRagFlowResponse(response.getBody());
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow DELETE 请求失败: {}", url, e);
            throw new ServiceException("知识库服务请求失败: " + e.getMessage());
        }
    }

    /**
     * 检查 RAGFlow 响应：code=0 表示成功，否则抛异常
     */
    private void checkRagFlowResponse(Map<String, Object> response)
    {
        if (response == null)
        {
            throw new ServiceException("知识库服务返回空响应");
        }
        Object code = response.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0)
        {
            Object message = response.get("message");
            throw new ServiceException("知识库服务错误: " + (message != null ? message.toString() : "未知错误"));
        }
    }
}

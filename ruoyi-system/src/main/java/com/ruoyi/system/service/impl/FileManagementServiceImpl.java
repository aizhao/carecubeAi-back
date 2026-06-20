package com.ruoyi.system.service.impl;

import com.ruoyi.common.config.RagFlowConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IFileManagementService;
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

import java.util.*;

/**
 * 文件管理 Service — 代理 RAGFlow File Management API
 *
 * @author ruoyi
 */
@Service
public class FileManagementServiceImpl implements IFileManagementService
{
    private static final Logger log = LoggerFactory.getLogger(FileManagementServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RagFlowConfig ragFlowConfig;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> listFiles(String parentId, int page, int pageSize, String keywords)
    {
        StringBuilder url = new StringBuilder(ragFlowConfig.getUrl())
                .append("/api/v1/files?page=").append(page)
                .append("&page_size=").append(pageSize);
        if (parentId != null && !parentId.isEmpty()) {
            url.append("&parent_id=").append(parentId);
        }
        if (keywords != null && !keywords.isEmpty()) {
            url.append("&keywords=").append(keywords);
        }

        Map<String, Object> response = executeGet(url.toString());
        // RAGFlow 返回: {"code": 0, "data": {"total": N, "files": [...], "parent_folder": {...}}}
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return data != null ? data : Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> uploadFiles(String parentId, MultipartFile[] files)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files";

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(ragFlowConfig.getApiKey());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile file : files)
            {
                body.add("file", new ByteArrayResource(file.getBytes())
                {
                    @Override
                    public String getFilename()
                    {
                        return file.getOriginalFilename();
                    }
                });
            }
            if (parentId != null && !parentId.isEmpty())
            {
                body.add("parent_id", parentId);
            }

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> responseBody = response.getBody();
            checkRagFlowResponse(responseBody);

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
            return dataList != null ? dataList : Collections.emptyList();
        }
        catch (Exception e)
        {
            log.error("上传文件到RAGFlow失败", e);
            throw new ServiceException("上传文件失败: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createFolder(String name, String parentId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("type", "folder");
        if (parentId != null && !parentId.isEmpty())
        {
            requestBody.put("parent_id", parentId);
        }

        Map<String, Object> response = executePost(url, requestBody);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return data != null ? data : Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int deleteFiles(List<String> ids)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ids", ids);

        try
        {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, buildHeaders());
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> body = response.getBody();
            // code=0 完全成功, code=102 部分成功
            if (body != null)
            {
                Object code = body.get("code");
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null && data.get("success_count") instanceof Number)
                {
                    int success = ((Number) data.get("success_count")).intValue();
                    // 部分成功时记录错误，但不抛异常
                    if (code instanceof Number && ((Number) code).intValue() == 102)
                    {
                        log.warn("部分删除成功: {}/{} 个文件被删除", success, ids.size());
                    }
                    return success;
                }
            }
            return 0;
        }
        catch (RestClientException e)
        {
            log.error("RAGFlow DELETE 请求失败: {}", url, e);
            throw new ServiceException("删除文件失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadFile(String fileId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files/" + fileId;
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
            log.error("下载文件失败", e);
            throw new ServiceException("下载文件失败: " + e.getMessage());
        }
    }

    @Override
    public void moveFiles(List<String> srcFileIds, String destFileId, String newName)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files/move";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("src_file_ids", srcFileIds);
        if (destFileId != null && !destFileId.isEmpty())
        {
            requestBody.put("dest_file_id", destFileId);
        }
        if (newName != null && !newName.isEmpty())
        {
            requestBody.put("new_name", newName);
        }

        executePost(url, requestBody);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> linkToDatasets(List<String> fileIds, List<String> kbIds)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files/link-to-datasets";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("file_ids", fileIds);
        requestBody.put("kb_ids", kbIds);

        Map<String, Object> response = executePost(url, requestBody);
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data != null ? data : Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAncestors(String fileId)
    {
        String url = ragFlowConfig.getUrl() + "/api/v1/files/" + fileId + "/ancestors";
        Map<String, Object> response = executeGet(url.toString());
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return data != null ? data : Collections.emptyMap();
    }

    // ========== 内部辅助方法 ==========

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
            throw new ServiceException("文件管理服务请求失败: " + e.getMessage());
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
            throw new ServiceException("文件管理服务请求失败: " + e.getMessage());
        }
    }

    private void checkRagFlowResponse(Map<String, Object> response)
    {
        if (response == null)
        {
            throw new ServiceException("文件管理服务返回空响应");
        }
        Object code = response.get("code");
        if (code instanceof Number && ((Number) code).intValue() != 0)
        {
            Object message = response.get("message");
            throw new ServiceException("文件管理服务错误: " + (message != null ? message.toString() : "未知错误"));
        }
    }
}

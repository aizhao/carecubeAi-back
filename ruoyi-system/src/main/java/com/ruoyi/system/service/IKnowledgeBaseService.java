package com.ruoyi.system.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理 Service 接口 — 代理 RAGFlow API
 *
 * @author ruoyi
 */
public interface IKnowledgeBaseService
{
    // ========== 知识库（Dataset）CRUD ==========

    /**
     * 查询知识库列表
     */
    List<Map<String, Object>> listDatasets(int pageNum, int pageSize, String name, String id);

    /**
     * 获取知识库总数
     */
    long countDatasets(String name, String id);

    /**
     * 获取知识库详情
     */
    Map<String, Object> getDataset(String datasetId);

    /**
     * 创建知识库
     */
    Map<String, Object> createDataset(Map<String, Object> params);

    /**
     * 更新知识库
     */
    void updateDataset(String datasetId, Map<String, Object> params);

    /**
     * 删除知识库
     */
    void deleteDatasets(List<String> ids);

    // ========== 文件（Document）CRUD ==========

    /**
     * 查询文件列表
     */
    List<Map<String, Object>> listDocuments(String datasetId, int pageNum, int pageSize, String keywords, String name, String id);

    /**
     * 获取文件总数
     */
    long countDocuments(String datasetId, String keywords, String name, String id);

    /**
     * 上传文件到知识库
     */
    Map<String, Object> uploadDocument(String datasetId, MultipartFile file);

    /**
     * 删除文件
     */
    void deleteDocuments(String datasetId, List<String> ids);

    /**
     * 下载文件
     */
    byte[] downloadDocument(String datasetId, String documentId);

    /**
     * 获取文档详情
     */
    Map<String, Object> getDocument(String datasetId, String documentId);

    /**
     * 更新文档（重命名、修改分块配置等）
     */
    void updateDocument(String datasetId, String documentId, Map<String, Object> params);

    /**
     * 解析文档（触发分块）
     */
    void parseDocuments(String datasetId, List<String> documentIds);

    /**
     * 停止解析文档
     */
    void stopParsing(String datasetId, List<String> documentIds);
}

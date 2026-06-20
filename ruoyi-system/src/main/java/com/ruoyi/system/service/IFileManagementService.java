package com.ruoyi.system.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 文件管理 Service — 代理 RAGFlow File Management API
 *
 * @author ruoyi
 */
public interface IFileManagementService
{
    /**
     * 列出指定文件夹下的文件和子文件夹
     */
    Map<String, Object> listFiles(String parentId, int page, int pageSize, String keywords);

    /**
     * 上传文件到指定文件夹
     */
    List<Map<String, Object>> uploadFiles(String parentId, MultipartFile[] files);

    /**
     * 创建文件夹
     */
    Map<String, Object> createFolder(String name, String parentId);

    /**
     * 删除文件/文件夹
     */
    int deleteFiles(List<String> ids);

    /**
     * 下载文件
     */
    byte[] downloadFile(String fileId);

    /**
     * 移动或重命名文件/文件夹
     */
    void moveFiles(List<String> srcFileIds, String destFileId, String newName);

    /**
     * 将文件关联到知识库（转换为Document）
     */
    List<Map<String, Object>> linkToDatasets(List<String> fileIds, List<String> kbIds);

    /**
     * 获取文件/文件夹的祖先路径（面包屑）
     */
    Map<String, Object> getAncestors(String fileId);
}

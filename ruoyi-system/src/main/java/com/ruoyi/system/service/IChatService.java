package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Chat Assistant Service — proxies RAGFlow Chat API
 *
 * @author ruoyi
 */
public interface IChatService
{
    /**
     * 查询聊天列表
     */
    public Map<String, Object> listChats(int page, int pageSize, String keywords);

    /**
     * 获取聊天详情
     */
    public Map<String, Object> getChat(String chatId);

    /**
     * 创建聊天
     */
    public Map<String, Object> createChat(Map<String, Object> params);

    /**
     * 更新聊天
     */
    public Map<String, Object> updateChat(String chatId, Map<String, Object> params);

    /**
     * 删除聊天
     */
    public void deleteChats(List<String> ids);

    /**
     * 查询会话列表
     */
    public Map<String, Object> listSessions(String chatId, int page, int pageSize, String userId);

    /**
     * 创建会话
     */
    public Map<String, Object> createSession(String chatId, String name, String userId);

    /**
     * 获取会话详情
     */
    public Map<String, Object> getSession(String chatId, String sessionId);

    /**
     * 删除会话
     */
    public void deleteSessions(String chatId, List<String> sessionIds);

    /**
     * 发送消息（SSE 流式）— 通过回调逐行返回，避免连接关闭导致流中断
     */
    public void sendMessage(String chatId, String sessionId, String question, Consumer<String> onLine);

    /**
     * 下载知识库文档
     */
    public byte[] downloadDocument(String datasetId, String documentId);

    /**
     * 预览知识库文档
     */
    public Map<String, Object> previewDocument(String documentId);
}

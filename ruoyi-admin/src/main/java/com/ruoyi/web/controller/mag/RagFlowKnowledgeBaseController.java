package com.ruoyi.web.controller.mag;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.system.service.IKnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 知识库管理 — 代理 RAGFlow API
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/mag/kb")
public class RagFlowKnowledgeBaseController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(RagFlowKnowledgeBaseController.class);

    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    // ========== 知识库（Dataset）管理 ==========

    /**
     * 查询知识库列表
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:list')")
    @GetMapping("/dataset/list")
    public TableDataInfo datasetList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String id)
    {
        try
        {
            List<Map<String, Object>> list = knowledgeBaseService.listDatasets(pageNum, pageSize, name, id);
            long total = knowledgeBaseService.countDatasets(name, id);
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            rspData.setRows(list);
            rspData.setTotal(total);
            return rspData;
        }
        catch (ServiceException e)
        {
            log.warn("查询知识库列表失败，返回空结果: {}", e.getMessage());
            TableDataInfo rspData = new TableDataInfo();
            rspData.setCode(HttpStatus.SUCCESS);
            rspData.setMsg("查询成功");
            rspData.setRows(Collections.emptyList());
            rspData.setTotal(0);
            return rspData;
        }
    }

    /**
     * 获取知识库详情
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:query')")
    @GetMapping("/dataset/{datasetId}")
    public AjaxResult getDataset(@PathVariable String datasetId)
    {
        return success(knowledgeBaseService.getDataset(datasetId));
    }

    /**
     * 新增知识库
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:add')")
    @Log(title = "知识库管理", businessType = BusinessType.INSERT)
    @PostMapping("/dataset")
    public AjaxResult addDataset(@RequestBody Map<String, Object> params)
    {
        Map<String, Object> result = knowledgeBaseService.createDataset(params);
        return success(result);
    }

    /**
     * 修改知识库
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:edit')")
    @Log(title = "知识库管理", businessType = BusinessType.UPDATE)
    @PutMapping("/dataset")
    public AjaxResult updateDataset(@RequestBody Map<String, Object> params)
    {
        String datasetId = (String) params.remove("id");
        if (datasetId == null || datasetId.isEmpty())
        {
            return error("知识库ID不能为空");
        }
        knowledgeBaseService.updateDataset(datasetId, params);
        return success();
    }

    /**
     * 删除知识库
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:remove')")
    @Log(title = "知识库管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/dataset/{ids}")
    public AjaxResult delDataset(@PathVariable String[] ids)
    {
        knowledgeBaseService.deleteDatasets(Arrays.asList(ids));
        return success();
    }

    // ========== 文件（Document）管理 ==========

    /**
     * 查询知识库下的文件列表
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:list')")
    @GetMapping("/doc/list/{datasetId}")
    public TableDataInfo docList(
            @PathVariable String datasetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String id)
    {
        List<Map<String, Object>> list = knowledgeBaseService.listDocuments(
                datasetId, pageNum, pageSize, keywords, name, id);
        long total = knowledgeBaseService.countDocuments(datasetId, keywords, name, id);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 上传文件到知识库
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:add')")
    @Log(title = "知识库文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/doc/upload/{datasetId}")
    public AjaxResult uploadDoc(
            @PathVariable String datasetId,
            @RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return error("上传文件不能为空");
        }
        Map<String, Object> result = knowledgeBaseService.uploadDocument(datasetId, file);
        return success(result);
    }

    /**
     * 删除知识库下的文件
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:remove')")
    @Log(title = "知识库文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/doc/{datasetId}/{ids}")
    public AjaxResult delDoc(
            @PathVariable String datasetId,
            @PathVariable String[] ids)
    {
        knowledgeBaseService.deleteDocuments(datasetId, Arrays.asList(ids));
        return success();
    }

    /**
     * 下载文件
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:query')")
    @GetMapping("/doc/download/{datasetId}/{documentId}")
    public void downloadDoc(
            @PathVariable String datasetId,
            @PathVariable String documentId,
            HttpServletResponse response) throws IOException
    {
        byte[] data = knowledgeBaseService.downloadDocument(datasetId, documentId);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + documentId);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // ========== 文档高级操作 ==========

    /**
     * 获取文档详情
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:query')")
    @GetMapping("/doc/{datasetId}/{docId}")
    public AjaxResult getDoc(
            @PathVariable String datasetId,
            @PathVariable String docId)
    {
        return success(knowledgeBaseService.getDocument(datasetId, docId));
    }

    /**
     * 更新文档（重命名/修改配置等）
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:edit')")
    @Log(title = "知识库文件管理", businessType = BusinessType.UPDATE)
    @PutMapping("/doc/{datasetId}/{docId}")
    public AjaxResult updateDoc(
            @PathVariable String datasetId,
            @PathVariable String docId,
            @RequestBody Map<String, Object> params)
    {
        knowledgeBaseService.updateDocument(datasetId, docId, params);
        return success();
    }

    /**
     * 解析文档（触发分块）
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:edit')")
    @Log(title = "知识库文件管理", businessType = BusinessType.UPDATE)
    @PostMapping("/doc/parse/{datasetId}")
    public AjaxResult parseDocs(
            @PathVariable String datasetId,
            @RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<String> docIds = (List<String>) params.get("document_ids");
        if (docIds == null || docIds.isEmpty())
        {
            return error("请选择要解析的文档");
        }
        knowledgeBaseService.parseDocuments(datasetId, docIds);
        return success();
    }

    /**
     * 停止解析文档
     */
    @PreAuthorize("@ss.hasPermi('mag:kb:edit')")
    @Log(title = "知识库文件管理", businessType = BusinessType.UPDATE)
    @DeleteMapping("/doc/parse/{datasetId}")
    public AjaxResult stopParsing(
            @PathVariable String datasetId,
            @RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<String> docIds = (List<String>) params.get("document_ids");
        if (docIds == null || docIds.isEmpty())
        {
            return error("请选择要停止的文档");
        }
        knowledgeBaseService.stopParsing(datasetId, docIds);
        return success();
    }
}

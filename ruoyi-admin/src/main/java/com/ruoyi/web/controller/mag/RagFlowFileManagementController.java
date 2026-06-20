package com.ruoyi.web.controller.mag;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.system.service.IFileManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 文件管理 — 代理 RAGFlow File Management API
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/mag/file")
public class RagFlowFileManagementController extends BaseController
{
    @Autowired
    private IFileManagementService fileManagementService;

    /**
     * 列出文件夹内容
     */
    @PreAuthorize("@ss.hasPermi('mag:file:list')")
    @GetMapping("/list")
    @SuppressWarnings("unchecked")
    public TableDataInfo listFiles(
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String keywords)
    {
        Map<String, Object> data = fileManagementService.listFiles(parentId, pageNum, pageSize, keywords);
        List<Map<String, Object>> files = (List<Map<String, Object>>) data.get("files");
        if (files == null) files = Collections.emptyList();
        Object total = data.get("total");
        long count = total instanceof Number ? ((Number) total).longValue() : files.size();

        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(files);
        rspData.setTotal(count);
        return rspData;
    }

    /**
     * 上传文件
     */
    @PreAuthorize("@ss.hasPermi('mag:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadFiles(
            @RequestParam(required = false) String parentId,
            @RequestParam("file") MultipartFile[] files)
    {
        if (files == null || files.length == 0)
        {
            return error("上传文件不能为空");
        }
        List<Map<String, Object>> result = fileManagementService.uploadFiles(parentId, files);
        return success(result);
    }

    /**
     * 创建文件夹
     */
    @PreAuthorize("@ss.hasPermi('mag:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/folder")
    public AjaxResult createFolder(@RequestBody Map<String, Object> params)
    {
        String name = (String) params.get("name");
        if (name == null || name.isEmpty())
        {
            return error("文件夹名称不能为空");
        }
        String parentId = (String) params.get("parent_id");
        Map<String, Object> result = fileManagementService.createFolder(name, parentId);
        return success(result);
    }

    /**
     * 删除文件/文件夹
     */
    @PreAuthorize("@ss.hasPermi('mag:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping
    public AjaxResult deleteFiles(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) params.get("ids");
        if (ids == null || ids.isEmpty())
        {
            return error("请选择要删除的文件");
        }
        int successCount = fileManagementService.deleteFiles(ids);
        return success("成功删除 " + successCount + " 个文件");
    }

    /**
     * 下载文件
     */
    @PreAuthorize("@ss.hasPermi('mag:file:query')")
    @GetMapping("/download/{fileId}")
    public void downloadFile(
            @PathVariable String fileId,
            HttpServletResponse response) throws IOException
    {
        byte[] data = fileManagementService.downloadFile(fileId);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileId);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    /**
     * 移动或重命名文件/文件夹
     */
    @PreAuthorize("@ss.hasPermi('mag:file:edit')")
    @Log(title = "文件管理", businessType = BusinessType.UPDATE)
    @PostMapping("/move")
    public AjaxResult moveFiles(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<String> srcFileIds = (List<String>) params.get("src_file_ids");
        if (srcFileIds == null || srcFileIds.isEmpty())
        {
            return error("请选择要移动的文件");
        }
        String destFileId = (String) params.get("dest_file_id");
        String newName = (String) params.get("new_name");
        if ((destFileId == null || destFileId.isEmpty()) && (newName == null || newName.isEmpty()))
        {
            return error("必须指定目标文件夹或新名称");
        }
        fileManagementService.moveFiles(srcFileIds, destFileId, newName);
        return success();
    }

    /**
     * 关联文件到知识库
     */
    @PreAuthorize("@ss.hasPermi('mag:file:edit')")
    @Log(title = "文件管理", businessType = BusinessType.UPDATE)
    @PostMapping("/link-to-datasets")
    public AjaxResult linkToDatasets(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<String> fileIds = (List<String>) params.get("file_ids");
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) params.get("kb_ids");
        if (fileIds == null || fileIds.isEmpty())
        {
            return error("请选择要关联的文件");
        }
        if (kbIds == null || kbIds.isEmpty())
        {
            return error("请选择目标知识库");
        }
        List<Map<String, Object>> result = fileManagementService.linkToDatasets(fileIds, kbIds);
        return success(result);
    }

    /**
     * 获取祖先路径（面包屑）
     */
    @PreAuthorize("@ss.hasPermi('mag:file:query')")
    @GetMapping("/ancestors/{fileId}")
    public AjaxResult getAncestors(@PathVariable String fileId)
    {
        return success(fileManagementService.getAncestors(fileId));
    }
}

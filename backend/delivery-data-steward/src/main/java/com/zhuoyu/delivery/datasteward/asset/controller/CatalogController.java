package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.CatalogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AuditContextResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogDirectoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogProjectResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.api.PageResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/catalog")
public class CatalogController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final CatalogApplicationService catalogApplicationService;

    public CatalogController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        CatalogApplicationService catalogApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.catalogApplicationService = catalogApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @GetMapping("/projects")
    public ApiResponse<List<CatalogProjectResponse>> listProjects(
        @RequestParam(required = false) String assetSource
    ) {
        Long userId = currentUserId();
        return ApiResponse.success(catalogApplicationService.listCatalogProjects(userId, assetSource));
    }

    @GetMapping("/directories")
    public ApiResponse<List<CatalogDirectoryResponse>> listDirectories(
        @RequestParam Long projectId
    ) {
        Long userId = currentUserId();
        return ApiResponse.success(catalogApplicationService.listCatalogDirectories(userId, projectId));
    }

    @GetMapping("/files")
    public ApiResponse<PageResponse<CatalogFileResponse>> listFiles(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String directoryPath,
        @RequestParam(required = false) String fileExt,
        @RequestParam(required = false) String fileKind,
        @RequestParam(required = false) String disciplineCode,
        @RequestParam(required = false) String version,
        @RequestParam(required = false) String qualityIssue,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        Long userId = currentUserId();
        return ApiResponse.success(catalogApplicationService.listCatalogFiles(
            userId, projectId, keyword, directoryPath, fileExt, fileKind, disciplineCode, version, qualityIssue, page, pageSize));
    }

    @GetMapping("/files/{fileId}")
    public ApiResponse<CatalogFileDetailResponse> getFileDetail(
        @PathVariable Long fileId
    ) {
        Long userId = currentUserId();
        CatalogFileDetailResponse detail = catalogApplicationService.getCatalogFileDetail(userId, fileId);
        if (detail == null) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        return ApiResponse.success(detail);
    }

    @GetMapping("/files/{fileId}/audit-context")
    public ApiResponse<AuditContextResponse> getFileAuditContext(
        @PathVariable Long fileId
    ) {
        Long userId = currentUserId();
        AuditContextResponse context = catalogApplicationService.getFileAuditContext(userId, fileId);
        if (context == null) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        return ApiResponse.success(context);
    }
}

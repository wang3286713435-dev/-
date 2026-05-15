package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.CatalogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionProofCheckRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionProofResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/catalog")
public class PermissionProofController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final CatalogApplicationService catalogApplicationService;

    public PermissionProofController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        CatalogApplicationService catalogApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.catalogApplicationService = catalogApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @GetMapping("/files/{fileId}/permission-proof")
    public ApiResponse<PermissionProofResponse> getFilePermissionProof(
        @PathVariable Long fileId
    ) {
        Long userId = currentUserId();
        PermissionProofResponse result = catalogApplicationService.checkFilePermission(userId, fileId);
        catalogApplicationService.writePermissionProofAudit(userId, fileId, result);
        return ApiResponse.success(result);
    }

    @PostMapping("/permission-proofs:check")
    public ApiResponse<List<PermissionProofResponse>> checkPermissionProofs(
        @Valid @RequestBody PermissionProofCheckRequest request
    ) {
        Long userId = currentUserId();
        List<PermissionProofResponse> results = catalogApplicationService.checkBulkPermission(
            userId, request.fileIds(), request.actorType());
        for (int i = 0; i < results.size(); i++) {
            Long fileId = request.fileIds().get(i);
            catalogApplicationService.writePermissionProofAudit(userId, fileId, results.get(i));
        }
        return ApiResponse.success(results);
    }
}

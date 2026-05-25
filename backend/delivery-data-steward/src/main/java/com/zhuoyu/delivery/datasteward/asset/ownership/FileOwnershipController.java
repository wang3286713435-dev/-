package com.zhuoyu.delivery.datasteward.asset.ownership;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipApplyRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipApplyResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipCoverageResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipFileRow;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationRow;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipTreeResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/projects/{projectId}/file-ownership")
public class FileOwnershipController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final FileOwnershipApplicationService fileOwnershipApplicationService;

    public FileOwnershipController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        FileOwnershipApplicationService fileOwnershipApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.fileOwnershipApplicationService = fileOwnershipApplicationService;
    }

    @GetMapping("/coverage")
    public ApiResponse<FileOwnershipCoverageResponse> coverage(@PathVariable Long projectId) {
        return ApiResponse.success(fileOwnershipApplicationService.coverage(currentUserId(), projectId));
    }

    @GetMapping("/tree")
    public ApiResponse<FileOwnershipTreeResponse> tree(@PathVariable Long projectId) {
        return ApiResponse.success(fileOwnershipApplicationService.tree(currentUserId(), projectId));
    }

    @GetMapping("/files")
    public ApiResponse<PageResponse<FileOwnershipFileRow>> files(
        @PathVariable Long projectId,
        @RequestParam(required = false) String nodePath,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(fileOwnershipApplicationService.files(currentUserId(), projectId, nodePath, status, page, pageSize));
    }

    @GetMapping("/unassigned")
    public ApiResponse<PageResponse<FileOwnershipRecommendationRow>> unassigned(
        @PathVariable Long projectId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(fileOwnershipApplicationService.unassigned(currentUserId(), projectId, page, pageSize));
    }

    @PostMapping("/recommendations")
    public ApiResponse<FileOwnershipRecommendationResponse> recommendations(
        @PathVariable Long projectId,
        @RequestBody(required = false) FileOwnershipRecommendationRequest request
    ) {
        return ApiResponse.success(fileOwnershipApplicationService.recommendations(currentUserId(), projectId, request));
    }

    @PostMapping("/recommendations:apply")
    public ApiResponse<FileOwnershipApplyResponse> applyRecommendations(
        @PathVariable Long projectId,
        @Valid @RequestBody FileOwnershipApplyRequest request
    ) {
        return ApiResponse.success(fileOwnershipApplicationService.apply(currentUserId(), projectId, request));
    }

    @PutMapping("/assignments:batch")
    public ApiResponse<FileOwnershipApplyResponse> batchAssign(
        @PathVariable Long projectId,
        @Valid @RequestBody FileOwnershipApplyRequest request
    ) {
        return ApiResponse.success(fileOwnershipApplicationService.apply(currentUserId(), projectId, request));
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }
}

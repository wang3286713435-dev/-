package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationSummaryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderReadinessResponse;
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
@RequestMapping("/api/data-steward")
public class StorageMigrationController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final StorageMigrationApplicationService storageMigrationApplicationService;

    public StorageMigrationController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        StorageMigrationApplicationService storageMigrationApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.storageMigrationApplicationService = storageMigrationApplicationService;
    }

    @PostMapping("/projects/{projectId}/storage-migration-tasks")
    public ApiResponse<StorageMigrationTaskDetailResponse> createTask(
        @PathVariable Long projectId,
        @Valid @RequestBody StorageMigrationTaskCreateRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.createTask(currentUserId(), projectId, request));
    }

    @GetMapping("/projects/{projectId}/storage-migration-tasks")
    public ApiResponse<List<StorageMigrationTaskListItemResponse>> listTasks(@PathVariable Long projectId) {
        return ApiResponse.success(storageMigrationApplicationService.listTasks(currentUserId(), projectId));
    }

    @GetMapping("/projects/{projectId}/storage-migration-summary")
    public ApiResponse<StorageMigrationSummaryResponse> summary(@PathVariable Long projectId) {
        return ApiResponse.success(storageMigrationApplicationService.summary(currentUserId(), projectId));
    }

    @GetMapping("/storage-provider-readiness")
    public ApiResponse<StorageProviderReadinessResponse> readiness() {
        return ApiResponse.success(storageMigrationApplicationService.minioReadiness(currentUserId()));
    }

    @GetMapping("/storage-objectification-inventory")
    public ApiResponse<StorageObjectificationInventoryResponse> allProjectInventory() {
        return ApiResponse.success(storageMigrationApplicationService.inventory(currentUserId(), null));
    }

    @GetMapping("/projects/{projectId}/storage-objectification-inventory")
    public ApiResponse<StorageObjectificationInventoryResponse> projectInventory(@PathVariable Long projectId) {
        return ApiResponse.success(storageMigrationApplicationService.inventory(currentUserId(), projectId));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-plans:dry-run")
    public ApiResponse<StorageObjectificationPlanDryRunResponse> dryRunObjectificationPlan(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationPlanDryRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.dryRunObjectificationPlan(
            currentUserId(), projectId, request));
    }

    @GetMapping("/storage-migration-tasks/{taskId}")
    public ApiResponse<StorageMigrationTaskDetailResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(storageMigrationApplicationService.getTask(currentUserId(), taskId));
    }

    @PostMapping("/storage-migration-tasks/{taskId}:retry")
    public ApiResponse<StorageMigrationTaskDetailResponse> retryTask(@PathVariable Long taskId) {
        return ApiResponse.success(storageMigrationApplicationService.retryTask(currentUserId(), taskId));
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }
}

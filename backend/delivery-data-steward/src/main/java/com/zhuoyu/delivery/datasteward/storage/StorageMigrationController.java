package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
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

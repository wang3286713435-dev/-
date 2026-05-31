package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationExecuteRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationExecuteResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationFullPlanRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationFullPlanResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationLongRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationLongRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationSummaryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunOverviewResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunProjectsResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityRepairRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityRepairResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderReadinessResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageReadPolicyResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveCandidatesResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveExecuteRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveReportsResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/storage-read-policy")
    public ApiResponse<StorageReadPolicyResponse> readPolicy() {
        return ApiResponse.success(storageMigrationApplicationService.readPolicy(currentUserId()));
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

    @PostMapping("/projects/{projectId}/storage-objectification-full-plan")
    public ApiResponse<StorageObjectificationFullPlanResponse> fullObjectificationPlan(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationFullPlanRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.fullObjectificationPlan(
            currentUserId(), projectId, request));
    }

    @GetMapping("/projects/{projectId}/storage-objectification-long-run")
    public ApiResponse<StorageObjectificationLongRunResponse> longRunStatus(@PathVariable Long projectId) {
        return ApiResponse.success(storageMigrationApplicationService.longRunStatus(currentUserId(), projectId));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-long-run:start")
    public ApiResponse<StorageObjectificationLongRunResponse> startLongRun(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationLongRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.startLongRun(currentUserId(), projectId, request));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-long-run:pause")
    public ApiResponse<StorageObjectificationLongRunResponse> pauseLongRun(@PathVariable Long projectId) {
        return ApiResponse.success(storageMigrationApplicationService.pauseLongRun(currentUserId(), projectId));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-long-run:resume")
    public ApiResponse<StorageObjectificationLongRunResponse> resumeLongRun(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationLongRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.resumeLongRun(currentUserId(), projectId, request));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-long-run:retry-failures")
    public ApiResponse<StorageObjectificationLongRunResponse> retryLongRunFailures(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationLongRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.retryLongRunFailures(currentUserId(), projectId, request));
    }

    @GetMapping("/projects/{projectId}/storage-objectification-integrity")
    public ApiResponse<StorageObjectificationIntegrityResponse> objectificationIntegrity(
        @PathVariable Long projectId,
        @RequestParam(required = false) Integer sampleLimit
    ) {
        return ApiResponse.success(storageMigrationApplicationService.objectificationIntegrity(
            currentUserId(), projectId, sampleLimit));
    }

    @PostMapping("/projects/{projectId}/storage-objectification-integrity:repair")
    public ApiResponse<StorageObjectificationIntegrityRepairResponse> repairObjectificationIntegrity(
        @PathVariable Long projectId,
        @RequestBody(required = false) StorageObjectificationIntegrityRepairRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.repairObjectificationIntegrity(
            currentUserId(), projectId, request));
    }

    @PostMapping("/storage-objectification-plans:dry-run")
    public ApiResponse<MultiProjectStorageObjectificationPlanDryRunResponse> dryRunMultiProjectObjectificationPlan(
        @RequestBody(required = false) MultiProjectStorageObjectificationPlanDryRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.dryRunMultiProjectObjectificationPlan(
            currentUserId(), request));
    }

    @PostMapping("/storage-objectification-plans:execute")
    public ApiResponse<MultiProjectStorageObjectificationExecuteResponse> executeMultiProjectObjectificationPlan(
        @RequestBody MultiProjectStorageObjectificationExecuteRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.executeMultiProjectObjectificationPlan(
            currentUserId(), request));
    }

    @GetMapping("/storage-objectification-wave/candidates")
    public ApiResponse<StorageObjectificationWaveCandidatesResponse> wave1Candidates() {
        return ApiResponse.success(storageMigrationApplicationService.wave1Candidates(currentUserId()));
    }

    @PostMapping("/storage-objectification-wave:dry-run")
    public ApiResponse<MultiProjectStorageObjectificationPlanDryRunResponse> dryRunWave1Objectification(
        @RequestBody(required = false) StorageObjectificationWaveDryRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.dryRunWave1Objectification(
            currentUserId(), request));
    }

    @PostMapping("/storage-objectification-wave:execute")
    public ApiResponse<MultiProjectStorageObjectificationExecuteResponse> executeWave1Objectification(
        @RequestBody StorageObjectificationWaveExecuteRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.executeWave1Objectification(
            currentUserId(), request));
    }

    @GetMapping("/storage-objectification-wave/reports")
    public ApiResponse<StorageObjectificationWaveReportsResponse> wave1Reports() {
        return ApiResponse.success(storageMigrationApplicationService.wave1Reports(currentUserId()));
    }

    @GetMapping("/storage-objectification-run/overview")
    public ApiResponse<StorageObjectificationRunOverviewResponse> objectificationRunOverview() {
        return ApiResponse.success(storageMigrationApplicationService.objectificationRunOverview(currentUserId()));
    }

    @GetMapping("/storage-objectification-run/projects")
    public ApiResponse<StorageObjectificationRunProjectsResponse> objectificationRunProjects() {
        return ApiResponse.success(storageMigrationApplicationService.objectificationRunProjects(currentUserId()));
    }

    @PostMapping("/storage-objectification-run:dry-run")
    public ApiResponse<MultiProjectStorageObjectificationPlanDryRunResponse> dryRunObjectificationRun(
        @RequestBody(required = false) StorageObjectificationRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.dryRunObjectificationRun(
            currentUserId(), request));
    }

    @PostMapping("/storage-objectification-run:start")
    public ApiResponse<MultiProjectStorageObjectificationExecuteResponse> startObjectificationRun(
        @RequestBody StorageObjectificationRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.startObjectificationRun(
            currentUserId(), request));
    }

    @PostMapping("/storage-objectification-run:continue")
    public ApiResponse<MultiProjectStorageObjectificationExecuteResponse> continueObjectificationRun(
        @RequestBody StorageObjectificationRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.continueObjectificationRun(
            currentUserId(), request));
    }

    @PostMapping("/storage-objectification-run:pause")
    public ApiResponse<StorageObjectificationRunOverviewResponse> pauseObjectificationRun() {
        return ApiResponse.success(storageMigrationApplicationService.pauseObjectificationRun(currentUserId()));
    }

    @PostMapping("/storage-objectification-run/retry-failed")
    public ApiResponse<MultiProjectStorageObjectificationExecuteResponse> retryFailedObjectificationRun(
        @RequestBody(required = false) StorageObjectificationRunRequest request
    ) {
        return ApiResponse.success(storageMigrationApplicationService.retryFailedObjectificationRun(
            currentUserId(), request));
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

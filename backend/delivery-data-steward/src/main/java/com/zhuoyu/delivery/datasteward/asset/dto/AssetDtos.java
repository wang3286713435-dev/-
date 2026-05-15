package com.zhuoyu.delivery.datasteward.asset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AssetDtos {

    private AssetDtos() {
    }

    // ===== project import =====

    public record ProjectBatchImportRequest(
        String sourceName,
        String csvText,
        @Valid
        List<ProjectImportRow> rows
    ) {
    }

    public record ProjectImportRow(
        @NotBlank(message = "项目编码不能为空")
        String code,
        @NotBlank(message = "项目名称不能为空")
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String ownerOrgName,
        String assetSource
    ) {
    }

    public record ImportResultResponse(
        Long jobId,
        String jobType,
        String sourceName,
        String status,
        int totalCount,
        int successCount,
        int failureCount,
        List<ImportRowError> rowErrors
    ) {
    }

    public record ImportRowError(
        int rowNo,
        String rawPreview,
        String errorCode,
        String errorMessage
    ) {
    }

    // ===== project asset =====

    public record AssetProjectCreateRequest(
        @NotBlank(message = "项目编码不能为空")
        String code,
        @NotBlank(message = "项目名称不能为空")
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String ownerOrgName,
        String assetSource
    ) {
    }

    public record AssetProjectUpdateRequest(
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String ownerOrgName,
        String assetSource,
        String assetStatus
    ) {
    }

    public record AssetProjectResponse(
        Long projectId,
        String code,
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String assetStatus,
        String assetSource,
        Integer modelCount,
        Long totalSizeBytes,
        Instant lastModelUpdatedAt,
        List<String> permissionTags,
        String projectScope,
        String confidentialityLevel,
        Instant lastSeenAt,
        String lifecycleStatus,
        String indexEligibility
    ) {
    }

    // ===== path mapping =====

    public record PathMappingRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId,
        @NotBlank(message = "NAS路径不能为空")
        String nasPath,
        String providerCode,
        String matchStrategy,
        Integer sortOrder,
        String remark
    ) {
    }

    public record PathMappingUpdateRequest(
        String nasPath,
        String matchStrategy,
        Boolean enabled,
        Integer sortOrder,
        String remark
    ) {
    }

    public record PathMappingResponse(
        Long id,
        Long projectId,
        String projectCode,
        String projectName,
        String providerCode,
        String nasPath,
        String matchStrategy,
        Boolean enabled,
        Integer sortOrder,
        String remark,
        Instant createdAt
    ) {
    }

    // ===== NAS project discovery =====

    public record NasProjectDiscoveryRequest(
        @NotBlank(message = "NAS 根路径不能为空")
        String rootPath,
        String providerCode,
        String industryType,
        String projectStage,
        String assetSource,
        Boolean createMissingProjects,
        Boolean createPathMappings,
        Boolean dryRun
    ) {
    }

    public record NasProjectDiscoveryResponse(
        String rootPath,
        Boolean dryRun,
        Integer totalDirectories,
        Integer createdProjects,
        Integer updatedProjects,
        Integer createdMappings,
        Integer existingMappings,
        Integer pendingCodeReview,
        List<NasProjectDiscoveryRow> rows
    ) {
    }

    public record NasProjectDiscoveryRow(
        String directoryName,
        String nasPath,
        String projectCode,
        String suggestedProjectCode,
        String projectName,
        Long projectId,
        Long mappingId,
        Boolean projectCreated,
        Boolean mappingCreated,
        String status,
        String message,
        Boolean requiresManualReview,
        String reviewReason,
        String directoryType,
        List<String> matchedStandardFolders
    ) {
    }

    // ===== nonstandard NAS directory governance =====

    public record NonstandardDirectoryDiscoverRequest(
        @NotBlank(message = "NAS 根路径不能为空")
        String rootPath,
        String providerCode,
        List<String> deferredProjectCodes,
        String ownerName
    ) {
    }

    public record NonstandardDirectoryDiscoverResponse(
        String rootPath,
        Integer discoveredCount,
        Integer createdOrUpdatedCount,
        List<NonstandardDirectoryResponse> rows
    ) {
    }

    public record NonstandardDirectoryUpdateRequest(
        String governanceStatus,
        String riskType,
        String suggestedProjectCode,
        String suggestedProjectName,
        String reviewReason,
        String agentSuggestion,
        String manualDecision,
        String decisionReason,
        String ownerName
    ) {
    }

    public record NonstandardDirectoryResponse(
        Long id,
        String providerCode,
        String rootPath,
        String directoryName,
        String nasPath,
        String directoryType,
        String riskType,
        String governanceStatus,
        String suggestedProjectCode,
        String suggestedProjectName,
        String duplicateBaseCode,
        String standardFoldersJson,
        String reviewReason,
        String agentSuggestion,
        String manualDecision,
        String decisionReason,
        String ownerName,
        Long decidedBy,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy
    ) {
    }

    // ===== NAS scan =====

    public record NasScanRequest(
        @NotBlank(message = "存储根编码不能为空")
        String rootCode,
        @NotBlank(message = "NAS 根路径不能为空")
        String rootPath,
        Long projectId,
        String projectCode,
        Boolean recursive,
        List<String> extensions,
        Boolean skipLowValueDirectories,
        List<String> skipDirectoryKeywords,
        String versionNo,
        String discipline,
        String sourceName
    ) {
    }

    public record ScanTaskResponse(
        Long id,
        String rootCode,
        String rootPath,
        Long projectId,
        String projectCode,
        Boolean recursive,
        String extensions,
        Boolean skipLowValueDirectories,
        String skipDirectoryKeywords,
        String status,
        String progressMessage,
        Integer progressCurrent,
        Integer progressTotal,
        BigDecimal progressPercent,
        Boolean cancelRequested,
        Integer totalScanned,
        Integer autoIngested,
        Integer pendingReview,
        Integer failedCount,
        Integer skippedLowValue,
        Integer skippedDirectories,
        String lastScannedPath,
        String failureReason,
        String scanReportJson,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy
    ) {
    }

    public record ScanReportResponse(
        Long scanTaskId,
        String rootCode,
        String rootPath,
        Long projectId,
        String projectCode,
        String status,
        Integer progressCurrent,
        Integer progressTotal,
        BigDecimal progressPercent,
        String progressMessage,
        Integer totalScanned,
        Integer autoIngested,
        Integer pendingReview,
        Integer failedCount,
        Integer skippedLowValue,
        Integer skippedDirectories,
        String lastScannedPath,
        String failureReason,
        String scanReportJson,
        Instant startedAt,
        Instant completedAt
    ) {
    }

    // ===== scan candidate / review =====

    public record ScanCandidateResponse(
        Long id,
        Long scanTaskId,
        Long matchedProjectId,
        String matchedProjectCode,
        String rawPath,
        String fileName,
        String fileExt,
        Long sizeBytes,
        Instant lastModifiedAt,
        String detectedFileKind,
        String detectedDiscipline,
        String detectedVersionNo,
        String confidenceLevel,
        String reviewStatus,
        String reviewMessage,
        String reviewedBy,
        Instant reviewedAt,
        Long createdFileResourceId
    ) {
    }

    public record ReviewUpdateRequest(
        Long matchedProjectId,
        String matchedProjectCode,
        String detectedFileKind,
        String detectedDiscipline,
        String detectedVersionNo,
        String reviewMessage
    ) {
    }

    // ===== file asset =====

    public record FileAssetResponse(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String fileName,
        String fileExt,
        String fileKind,
        String discipline,
        String versionNo,
        Long sizeBytes,
        String checksum,
        String storageProvider,
        String storagePath,
        String logicalPath,
        String sourceType,
        String processStatus,
        String reviewStatus,
        String confidenceLevel,
        Instant createdAt,
        Instant updatedAt,
        List<String> permissionTags,
        String projectScope,
        String confidentialityLevel,
        Instant lastSeenAt,
        String lifecycleStatus,
        String indexEligibility
    ) {
    }

    public record FileAssetMetadataUpdateRequest(
        String fileKind,
        String discipline,
        String versionNo,
        String confidenceLevel,
        String reviewStatus
    ) {
    }

    public record FilePreviewResponse(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String fileName,
        String fileExt,
        String fileKind,
        String previewStatus,
        String previewMode,
        Boolean previewAvailable,
        String conversionStatus,
        Boolean conversionRequired,
        String message,
        List<String> supportedActions,
        Boolean previewAllowed,
        Boolean downloadAllowed,
        String accessPolicyMessage,
        String viewerRoute,
        Instant updatedAt
    ) {
    }

    // ===== model asset (existing, keep) =====

    public record ModelAssetResponse(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String originalName,
        Long sizeBytes,
        String versionNo,
        String processStatus,
        String storageProvider,
        String logicalPath,
        String discipline,
        String sourceType,
        Instant lastVerifiedAt,
        String accessUrl,
        List<String> permissionTags,
        String projectScope,
        String confidentialityLevel,
        Instant lastSeenAt,
        String lifecycleStatus,
        String indexEligibility
    ) {
    }

    // ===== capacity =====

    public record CapacityDashboardResponse(
        Integer projectCount,
        Integer modelFileCount,
        Long totalSizeBytes,
        List<CapacityByDiscipline> byDiscipline,
        List<CapacityByProject> topProjects
    ) {
    }

    public record CapacityByDiscipline(
        String discipline,
        Integer fileCount,
        Long totalSizeBytes
    ) {
    }

    public record CapacityByProject(
        Long projectId,
        String projectCode,
        String projectName,
        Integer fileCount,
        Long totalSizeBytes
    ) {
    }

    // ===== discipline =====

    public record DisciplineResponse(
        Long id,
        String code,
        String name,
        Long projectId,
        String scope,
        Integer sortOrder
    ) {
    }

    // ===== import job =====

    public record ImportJobResponse(
        Long id,
        String jobType,
        String sourceName,
        String status,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        String reportJson,
        Instant createdAt
    ) {
    }

    // ===== storage =====

    public record StorageAccessResponse(
        Long fileId,
        String providerCode,
        String accessUrl,
        Boolean downloadable,
        String message
    ) {
    }

    public record StorageRoot(
        @NotNull
        Long id,
        String providerCode,
        String rootCode,
        String displayName,
        String rootPath
    ) {
    }

    public record StorageRootRequest(
        @NotBlank(message = "存储提供方编码不能为空")
        String providerCode,
        @NotBlank(message = "根编码不能为空")
        String rootCode,
        @NotBlank(message = "显示名称不能为空")
        String displayName,
        @NotBlank(message = "根路径不能为空")
        String rootPath
    ) {
    }

    // ===== batch 2: jobs =====

    public record JobCreateRequest(
        @NotBlank(message = "任务类型不能为空")
        String jobType,
        Long projectId,
        String targetType,
        Long targetId,
        String requestPayload
    ) {
    }

    public record JobResponse(
        Long id,
        String jobType,
        String status,
        Long projectId,
        String targetType,
        Long targetId,
        String requestPayload,
        Integer progressCurrent,
        Integer progressTotal,
        java.math.BigDecimal progressPercent,
        String progressMessage,
        String failureReason,
        Integer retryCount,
        Integer maxRetries,
        Long createdBy,
        java.time.Instant startedAt,
        java.time.Instant completedAt,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }

    // ===== batch 2: checksum =====

    public record ChecksumJobRequest(
        @NotNull(message = "文件资产ID不能为空")
        Long fileId
    ) {
    }

    public record BatchChecksumRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId
    ) {
    }

    // ===== batch 2: capacity statistics =====

    public record CapacityStatisticsResponse(
        Integer projectCount,
        Integer fileCount,
        Integer modelFileCount,
        Integer drawingFileCount,
        Long totalSizeBytes,
        java.util.List<CapacityByFileKind> byFileKind,
        java.util.List<CapacityByDiscipline> byDiscipline,
        java.util.List<CapacityByProject> topProjects,
        java.time.Instant lastUpdatedAt
    ) {
    }

    public record CapacityByFileKind(
        String fileKind,
        Integer fileCount,
        Long totalSizeBytes
    ) {
    }

    // ===== asset quality overview =====

    public record AssetQualityOverviewResponse(
        Long riskSignalCount,
        Long pendingReviewCount,
        Long failedScanCount,
        Long runningScanCount,
        Long missingChecksumCount,
        Long missingConfidenceCount,
        Long missingDisciplineCount,
        Long missingVersionCount,
        Long missingStoragePathCount,
        Long zeroSizeFileCount,
        Long nonstandardPendingCount,
        Long nonstandardApprovedCount,
        java.time.Instant latestAssetUpdatedAt,
        java.time.Instant latestEventAt,
        java.util.List<AssetQualityMetric> metrics,
        java.util.List<AssetQualityProjectRisk> topRiskProjects,
        java.util.List<EventResponse> recentEvents
    ) {
    }

    public record AssetQualityMetric(
        String code,
        String label,
        String severity,
        Long count,
        String description
    ) {
    }

    public record AssetQualityProjectRisk(
        Long projectId,
        String projectCode,
        String projectName,
        Long missingChecksumCount,
        Long missingConfidenceCount,
        Long missingDisciplineCount,
        Long missingVersionCount,
        Long missingStoragePathCount,
        Long zeroSizeFileCount,
        Long pendingReviewCount,
        Long failedScanCount,
        Long totalRiskCount
    ) {
    }

    // ===== batch 2: events =====

    public record EventQueryRequest(
        Long afterEventId,
        java.time.Instant fromTime,
        java.time.Instant toTime,
        Long projectId,
        String eventType,
        String actionCode,
        Integer limit
    ) {
    }

    public record EventResponse(
        Long id,
        String eventType,
        Long projectId,
        String aggregateType,
        String aggregateId,
        String actionCode,
        Long operatorId,
        String sourceType,
        String summary,
        String payloadJson,
        String traceId,
        java.time.Instant createdAt,
        List<String> permissionTags,
        String projectScope,
        String confidentialityLevel,
        java.time.Instant lastSeenAt,
        String lifecycleStatus,
        String indexEligibility
    ) {
    }

    // ===== batch 3: agent API key =====

    public record AgentApiKeyCreateRequest(
        @NotBlank(message = "Key名称不能为空")
        String keyName,
        @NotBlank(message = "授权范围不能为空")
        String scopeType,
        java.util.List<Long> projectIds,
        java.time.Instant expiresAt,
        String remark
    ) {
    }

    public record AgentApiKeyResponse(
        Long id,
        String keyName,
        String keyPrefix,
        String status,
        String scopeType,
        java.util.List<Long> projectIds,
        java.time.Instant expiresAt,
        java.time.Instant lastUsedAt,
        String lastUsedIp,
        Long createdBy,
        Long revokedBy,
        java.time.Instant revokedAt,
        String remark,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }

    public record AgentApiKeyCreateResponse(
        Long id,
        String keyName,
        String keyPrefix,
        String plainApiKey,
        String status,
        String scopeType,
        java.util.List<Long> projectIds,
        java.time.Instant expiresAt,
        String remark,
        java.time.Instant createdAt
    ) {
    }

    // ===== batch 3: agent annotation =====

    public record AgentAnnotationRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId,
        @NotBlank(message = "目标类型不能为空")
        String targetType,
        @NotNull(message = "目标ID不能为空")
        Long targetId,
        @NotBlank(message = "标注内容不能为空")
        String content
    ) {
    }

    public record AgentAnnotationResponse(
        Long id,
        Long apiKeyId,
        Long projectId,
        String targetType,
        Long targetId,
        String content,
        String status,
        java.time.Instant createdAt
    ) {
    }

    // ===== batch 3: agent query params =====

    public record AgentFileQueryParams(
        Long projectId,
        String fileKind,
        String discipline,
        String fileName,
        String fileExt,
        String sourceType,
        String keyword
    ) {
    }

    public record AgentEventQueryParams(
        Long afterEventId,
        java.time.Instant fromTime,
        java.time.Instant toTime,
        Long projectId,
        String eventType,
        String actionCode,
        Integer limit
    ) {
    }

    public record AgentJobQueryParams(
        Long projectId,
        String jobType,
        String status
    ) {
    }

    // ===== batch 3: delete request =====

    public record DeleteRequestCreateRequest(
        @NotNull(message = "项目ID不能为空")
        Long projectId,
        @NotNull(message = "文件ID不能为空")
        Long fileId,
        @NotBlank(message = "删除类型不能为空")
        String deleteType,
        @NotBlank(message = "删除原因不能为空")
        String reason
    ) {
    }

    public record DeleteRequestResponse(
        Long id,
        String requestNo,
        Long projectId,
        Long fileId,
        String deleteType,
        String reason,
        String status,
        String requestedByType,
        Long requestedBy,
        Long approvedBy,
        java.time.Instant approvedAt,
        Long rejectedBy,
        java.time.Instant rejectedAt,
        Long executedBy,
        java.time.Instant executedAt,
        String failureReason,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }

    public record DeleteApprovalRequest(
        String comment
    ) {
    }

    // ===== batch 3: quarantine record =====

    public record QuarantineRecordResponse(
        Long id,
        Long deleteRequestId,
        Long projectId,
        Long fileId,
        String originalPath,
        String quarantinePath,
        String status,
        java.time.Instant quarantineUntil,
        String requestedByType,
        Long requestedBy,
        Long approvedBy,
        Long executedBy,
        Long restoredBy,
        Long permanentDeletedBy,
        String failureReason,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {
    }

    // ===== phase2 batch1: readonly catalog =====

    public record CatalogProjectResponse(
        Long projectId,
        String projectCode,
        String projectName,
        String projectStage,
        String assetSource,
        Integer fileCount,
        Long totalSizeBytes,
        String confidenceLevel
    ) {
    }

    public record CatalogDirectoryResponse(
        String directoryPath,
        Long projectId,
        String projectCode,
        Integer fileCount,
        Long totalSizeBytes
    ) {
    }

    public record CatalogFileResponse(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String fileName,
        String fileExt,
        String fileKind,
        String disciplineCode,
        String disciplineName,
        String version,
        Long sizeBytes,
        String checksum,
        String status,
        String confidenceLevel,
        String storageProvider,
        String logicalPath,
        Boolean storagePathVisible,
        String storagePathVisibilityReason,
        List<String> qualityFlags,
        java.time.Instant lastVerifiedAt,
        java.time.Instant updatedAt,
        Boolean agentReadable,
        String agentReadReason,
        List<String> agentContractView
    ) {
    }

    public record CatalogFileDetailResponse(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String fileName,
        String fileExt,
        String fileKind,
        String disciplineCode,
        String disciplineName,
        String version,
        Long sizeBytes,
        String checksum,
        String status,
        String confidenceLevel,
        String storageProvider,
        String logicalPath,
        String storagePath,
        Boolean storagePathVisible,
        String storagePathVisibilityReason,
        List<String> qualityFlags,
        java.time.Instant lastVerifiedAt,
        java.time.Instant updatedAt,
        Boolean agentReadable,
        String agentReadReason,
        List<String> agentContractView
    ) {
    }

    public record AuditContextResponse(
        Long fileId,
        Integer totalEventCount,
        List<CatalogEventSummary> recentEvents
    ) {
    }

    public record CatalogEventSummary(
        Long eventId,
        String eventType,
        String actionCode,
        String summary,
        java.time.Instant createdAt
    ) {
    }

    // ===== phase2 batch1: permission proof =====

    public record PermissionProofResponse(
        Boolean allowed,
        String decision,
        String actorType,
        String projectScope,
        String reasonCode,
        String reasonText,
        List<PermissionEvidence> evidence,
        String traceId,
        java.time.Instant checkedAt
    ) {
    }

    public record PermissionEvidence(
        String type,
        String label,
        String value,
        Boolean sensitive
    ) {
    }

    public record PermissionProofCheckRequest(
        @NotNull(message = "文件ID列表不能为空")
        List<Long> fileIds,
        String actorType
    ) {
    }

    // ===== phase2 batch4: file access tickets =====

    public record AccessTicketCreateRequest(
        @NotBlank(message = "动作不能为空")
        String action
    ) {
    }

    public record AccessTicketResponse(
        Long ticketId,
        String ticket,
        String accessUrl,
        java.time.Instant expiresAt,
        String action,
        Long fileId,
        String fileName,
        Boolean previewable,
        Boolean downloadable,
        String message
    ) {
    }
}

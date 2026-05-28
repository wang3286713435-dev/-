package com.zhuoyu.delivery.visualization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class VisualizationDtos {

    private VisualizationDtos() {
    }

    public record VisualizationContextResponse(
        Long projectId,
        Integer publishedModelCount,
        Integer managedObjectCount,
        List<ModelContextItem> models,
        List<ManagedObjectContextItem> objects
    ) {
    }

    public record ModelContextItem(
        Long id,
        String name,
        String versionNo,
        String status,
        Integer componentCount
    ) {
    }

    public record LightweightStatusResponse(
        Long projectId,
        Long integrationId,
        Long modelFileId,
        String modelName,
        String modelFormat,
        String integrationStatus,
        String engineMode,
        Boolean engineConnected,
        String lightweightStatus,
        Boolean viewerAvailable,
        String taskStatus,
        Boolean conversionRequired,
        String componentIndexStatus,
        String previewMode,
        String statusLabel,
        String actionHint,
        String blockedReason,
        List<String> supportedOperations,
        List<String> forbiddenOperations
    ) {
    }

    public record LightweightPlanResponse(
        Long projectId,
        Long integrationId,
        Long modelFileId,
        String modelName,
        String modelFormat,
        String engineMode,
        Boolean dryRun,
        Boolean taskCreated,
        Boolean engineBindingRequired,
        Boolean realConversionExecuted,
        Boolean nasFileTouched,
        Boolean viewerAvailable,
        List<String> requiredConditions,
        List<String> futureSteps,
        List<String> riskWarnings,
        List<String> supportedOperations,
        List<String> forbiddenOperations
    ) {
    }

    public record LightweightJobCreateResponse(
        Long projectId,
        Long integrationId,
        Long modelFileId,
        String modelName,
        String modelFormat,
        String jobId,
        String engineMode,
        Boolean taskCreated,
        String taskStatus,
        String statusLabel,
        String actionHint,
        String blockedReason,
        Boolean realUploadExecuted,
        Boolean realConversionExecuted,
        Boolean modelBodyRead,
        Boolean nasFileTouched,
        Boolean viewerAvailable,
        List<String> supportedOperations,
        List<String> forbiddenOperations
    ) {
    }

    public record LightweightJobResponse(
        Long projectId,
        String jobId,
        String engineMode,
        String taskStatus,
        Integer progressPercent,
        String statusLabel,
        String blockedReason,
        Boolean viewerAvailable,
        Boolean realUploadExecuted,
        Boolean realConversionExecuted,
        Instant updatedAt
    ) {
    }

    public record LightweightViewerTicketResponse(
        Long projectId,
        String jobId,
        String engineMode,
        Boolean viewerAvailable,
        Boolean ticketIssued,
        String viewerTicket,
        Instant expiresAt,
        String launchUrl,
        String statusLabel,
        String blockedReason,
        List<String> supportedOperations,
        List<String> forbiddenOperations
    ) {
    }

    public record ManagedObjectContextItem(
        Long id,
        String code,
        String name,
        String objectType,
        Long sectionNodeId
    ) {
    }

    public record LocateResponse(
        Long projectId,
        Long managedObjectId,
        String adapterCommand,
        String cameraHint,
        String status
    ) {
    }

    public record HighlightRequest(String color, Integer durationSeconds) {
    }

    public record HighlightResponse(
        Long projectId,
        Long managedObjectId,
        String color,
        Integer durationSeconds,
        String adapterCommand,
        String status
    ) {
    }

    public record LinkageRequest(
        @NotNull(message = "文件资源不能为空")
        Long fileResourceId,
        @NotNull(message = "管理对象不能为空")
        Long managedObjectId,
        @NotBlank(message = "联动动作不能为空")
        String action
    ) {
    }

    public record LinkageResponse(
        Long projectId,
        Long fileResourceId,
        Long managedObjectId,
        String action,
        String adapterCommand,
        String status
    ) {
    }

    public record ContextInjectRequest(
        Long sectionNodeId,
        Long managedObjectId,
        String source
    ) {
    }

    public record ContextInjectResponse(
        Long projectId,
        Long sectionNodeId,
        Long managedObjectId,
        String source,
        String adapterCommand,
        String status
    ) {
    }

    public record DigitalTwinDashboardResponse(
        ProjectSnapshot project,
        AssetSummary assetSummary,
        DeliverySummary deliverySummary,
        QualitySummary qualitySummary,
        ModelSummary modelSummary,
        ActivitySummary activity,
        CollaborationOperationsSummary operationsSummary,
        SafetyBoundary safetyBoundary
    ) {
    }

    public record ProjectSnapshot(
        Long projectId,
        String code,
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String assetStatus,
        String onboardingStatus
    ) {
    }

    public record AssetSummary(
        Integer projectCount,
        Integer fileCount,
        Integer modelFileCount,
        Integer drawingFileCount,
        Long totalSizeBytes,
        List<DistributionItem> byFileKind,
        List<DistributionItem> byDiscipline
    ) {
    }

    public record DistributionItem(
        String code,
        String label,
        Integer count,
        Long totalSizeBytes
    ) {
    }

    public record DeliverySummary(
        Boolean standardReady,
        Integer totalRequired,
        Integer completedCount,
        Integer missingCount,
        Integer draftCount,
        Integer pendingReviewCount,
        Integer approvedCount,
        Integer rejectedCount,
        Integer openRectificationCount,
        Double completionRate,
        Double approvedRate,
        String nextActionCode,
        String nextActionText,
        List<String> readinessIssues
    ) {
    }

    public record QualitySummary(
        Long riskSignalCount,
        Long pendingReviewCount,
        Long failedScanCount,
        Long runningScanCount,
        Long missingChecksumCount,
        Long missingDisciplineCount,
        Long missingVersionCount,
        Long zeroSizeFileCount,
        List<QualityMetricItem> metrics
    ) {
    }

    public record QualityMetricItem(
        String code,
        String label,
        String severity,
        Long count
    ) {
    }

    public record ModelSummary(
        Integer modelIntegrationCount,
        Integer publishedModelCount,
        Integer managedObjectCount,
        String engineMode,
        Boolean engineConnected,
        String lightweightStatus,
        Boolean viewerAvailable,
        String statusLabel,
        String actionHint,
        List<ModelSceneItem> models,
        List<ObjectSceneItem> objects
    ) {
    }

    public record ModelSceneItem(
        Long id,
        Long modelFileId,
        String name,
        String modelFormat,
        String versionNo,
        String status,
        Integer componentCount,
        String previewStatus,
        String previewMode,
        String conversionStatus,
        Boolean viewerAvailable,
        String statusLabel,
        String actionHint
    ) {
    }

    public record ObjectSceneItem(
        Long id,
        String code,
        String name,
        String objectType,
        Long sectionNodeId,
        String discipline,
        String status
    ) {
    }

    public record CollaborationOperationsSummary(
        Integer equipmentCount,
        Integer spaceObjectCount,
        Integer systemObjectCount,
        Integer componentPlaceholderCount,
        Integer sectionNodeCount,
        Integer linkedObjectCount,
        Integer unlinkedObjectCount,
        List<DistributionItem> byObjectType,
        List<DistributionItem> byDiscipline,
        List<SystemSummaryItem> systems,
        List<SpaceSummaryItem> spaces,
        List<WorkItemSummary> workItems,
        List<String> unavailableModules
    ) {
    }

    public record SystemSummaryItem(
        Long id,
        String code,
        String name,
        String discipline,
        Integer linkedEquipmentCount,
        String status,
        String source
    ) {
    }

    public record SpaceSummaryItem(
        Long id,
        Long parentId,
        String code,
        String name,
        Integer level,
        String path,
        Integer objectCount,
        Integer equipmentCount,
        Integer systemCount
    ) {
    }

    public record WorkItemSummary(
        String id,
        String category,
        String title,
        String status,
        String source,
        String updatedAt
    ) {
    }

    public record ActivitySummary(
        Instant latestAssetUpdatedAt,
        Instant latestEventAt,
        List<ActivityItem> recentEvents,
        List<ScanTaskActivityItem> scanTasks
    ) {
    }

    public record ActivityItem(
        Long id,
        Long projectId,
        String actionCode,
        String summary,
        Instant createdAt
    ) {
    }

    public record ScanTaskActivityItem(
        Long id,
        Long projectId,
        String projectCode,
        String status,
        java.math.BigDecimal progressPercent,
        Integer totalScanned,
        Integer autoIngested,
        Integer pendingReview,
        Integer failedCount,
        Instant updatedAt
    ) {
    }

    public record SafetyBoundary(
        List<String> guarantees,
        List<String> blockedOperations,
        String viewerMessage
    ) {
    }
}

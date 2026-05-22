package com.zhuoyu.delivery.workcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class WorkCenterDtos {

    private WorkCenterDtos() {
    }

    // ---- review ----

    public record RejectRequest(
        @NotBlank(message = "驳回原因不能为空")
        String reason
    ) {
    }

    public record ReviewRecordResponse(
        Long id,
        Long bindingId,
        String action,
        String comment,
        Long reviewerUserId,
        LocalDateTime createdAt
    ) {
    }

    // ---- rectification ----

    public record RectificationRequest(
        String title,
        String description,
        String reason,
        String severity,
        Long assigneeUserId,
        LocalDate dueDate
    ) {
    }

    public record ResolveRequest(
        @NotBlank(message = "处理说明不能为空")
        String resolutionNote
    ) {
    }

    public record RectificationResponse(
        Long id,
        Long projectId,
        String sourceType,
        Long sourceId,
        Long bindingId,
        String title,
        String description,
        String reason,
        String status,
        String severity,
        Long assigneeUserId,
        String resolutionNote,
        LocalDate dueDate,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        Long createdBy,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // joined fields
        String bindingViewType,
        String bindingFileName,
        String bindingDeliverableTypeName,
        String bindingSectionNodeName
    ) {
    }

    // ---- delivery binding ----

    public record DeliveryBindingRequest(
        @NotBlank(message = "交付视图类型不能为空")
        String viewType,
        Long sectionNodeId,
        Long managedObjectId,
        @NotNull(message = "交付物类型不能为空")
        Long deliverableTypeId,
        @NotNull(message = "文件资源不能为空")
        Long fileResourceId,
        String bindingStatus,
        String reviewStatus,
        Integer sortOrder,
        String remark
    ) {
    }

    public record DeliveryBindingResponse(
        Long id,
        Long projectId,
        String viewType,
        Long sectionNodeId,
        String sectionNodeName,
        Long managedObjectId,
        String managedObjectName,
        Long deliverableTypeId,
        String deliverableTypeName,
        String deliverableDefinitionName,
        Long fileResourceId,
        String fileName,
        String fileKind,
        String versionNo,
        String processStatus,
        String bindingStatus,
        String reviewStatus,
        Integer sortOrder,
        String remark
    ) {
    }

    public record DeliveryViewResponse(
        Long projectId,
        String viewType,
        Integer totalCount,
        Integer boundCount,
        List<DeliveryBindingResponse> rows
    ) {
    }

    public record DashboardSummaryResponse(
        Long projectId,
        Integer sectionNodeCount,
        Integer deliverableDefinitionCount,
        Integer fileCount,
        Integer documentFileCount,
        Integer drawingFileCount,
        Integer modelFileCount,
        Integer modelIntegrationCount,
        Integer publishedModelCount,
        Integer managedObjectCount,
        Integer documentBindingCount,
        Integer drawingBindingCount
    ) {
    }

    public record DeliveryCompletenessRow(
        String targetType,
        Long targetId,
        String targetCode,
        String targetName,
        Long deliverableDefinitionId,
        String deliverableDefinitionCode,
        String deliverableDefinitionName,
        Long deliverableTypeId,
        String deliverableTypeCode,
        String deliverableTypeName,
        String fileKind,
        Boolean required,
        Boolean completed,
        Long bindingId,
        Long fileResourceId,
        String fileName,
        String versionNo,
        String reviewStatus,
        String missingReason
    ) {
    }

    public record DeliveryCompletenessResponse(
        Long projectId,
        String viewType,
        String targetType,
        Boolean standardReady,
        java.util.List<String> readinessIssues,
        Integer totalRequired,
        Integer completedCount,
        Integer missingCount,
        Integer draftCount,
        Integer pendingReviewCount,
        Integer approvedCount,
        Integer rejectedCount,
        Integer reviewReadyCount,
        Double completionRate,
        Double approvedRate,
        String nextActionCode,
        String nextActionText,
        java.util.List<DeliveryCompletenessRow> rows
    ) {
    }

    // ---- batch delivery binding ----

    public record BatchDeliveryBindingRequest(
        @NotBlank(message = "交付视图类型不能为空")
        String viewType,
        Long sectionNodeId,
        Long managedObjectId,
        @NotNull(message = "交付物类型不能为空")
        Long deliverableTypeId,
        @NotNull(message = "文件资源列表不能为空")
        java.util.List<Long> fileResourceIds,
        String bindingStatus,
        String reviewStatus,
        String remark
    ) {
    }

    public enum BatchBindingRowStatus {CREATED, SKIPPED, FAILED}

    public record BatchBindingRowResult(
        Long fileResourceId,
        BatchBindingRowStatus status,
        Long bindingId,
        String message
    ) {
    }

    public record BatchDeliveryBindingResponse(
        Long projectId,
        String viewType,
        Integer requestedCount,
        Integer createdCount,
        Integer skippedCount,
        Integer failedCount,
        java.util.List<DeliveryBindingResponse> createdBindings,
        java.util.List<BatchBindingRowResult> results
    ) {
    }

    // ---- delivery package summary ----

    public record DeliveryPackageViewSummary(
        Integer totalRequired,
        Integer boundCount,
        Integer missingCount,
        Integer pendingReviewCount,
        Integer approvedCount,
        Integer rejectedCount,
        Double completionRate,
        Integer reviewReadyCount
    ) {
    }

    public record DeliveryPackageSummaryRow(
        // deliverable def / type
        Long deliverableDefinitionId,
        String deliverableDefinitionName,
        Long deliverableTypeId,
        String deliverableTypeName,
        // target
        String targetType,
        Long targetId,
        String targetName,
        // file / binding
        Long bindingId,
        Long fileResourceId,
        String fileName,
        String fileKind,
        String versionNo,
        String reviewStatus,
        // readiness
        String readinessStatus
    ) {
    }

    public record DeliveryPackageSummaryResponse(
        Long projectId,
        DeliveryPackageViewSummary documentSummary,
        DeliveryPackageViewSummary drawingSummary,
        Integer totalRowCount,
        java.util.List<DeliveryPackageSummaryRow> rows
    ) {
    }

    // ---- delivery package export precheck ----

    public record ExportPrecheckRow(
        // deliverable
        Long deliverableDefinitionId,
        String deliverableDefinitionName,
        Long deliverableTypeId,
        String deliverableTypeName,
        // target
        String targetType,
        Long targetId,
        String targetName,
        // file / binding
        Long bindingId,
        Long fileResourceId,
        String fileName,
        String fileKind,
        String versionNo,
        String fileExt,
        // review
        String reviewStatus,
        String readinessStatus,
        // preview
        String previewStatus,
        String previewMode,
        String conversionStatus,
        Boolean conversionRequired,
        Boolean downloadOnly,
        String statusLabel,
        String actionHint,
        String riskLevel,
        // export
        String exportStatus,
        String blockReason
    ) {
    }

    public record ExportPrecheckResponse(
        Long projectId,
        String viewType,
        String targetType,
        Boolean dryRun,
        Boolean packageGenerated,
        Integer totalCount,
        Integer readyCount,
        Integer blockedCount,
        Integer missingCount,
        Integer pendingReviewCount,
        Integer rejectedCount,
        Integer conversionRequiredCount,
        Integer unsupportedPreviewCount,
        java.util.List<ExportPrecheckRow> rows
    ) {
    }

    // ---- Agent guided delivery governance ----

    public record AgentGovernanceStandardStatus(
        Boolean hasSectionTree,
        Boolean hasNodeTypes,
        Boolean nodeTypesLocked,
        Boolean deliverableStandardReady,
        Integer sectionNodeCount,
        Integer nodeTypeCount,
        Integer deliverableDefinitionCount,
        Integer deliverableTypeCount,
        Integer directoryTemplateCount
    ) {
    }

    public record AgentGovernanceDeliveryStatus(
        String viewType,
        Integer totalRequired,
        Integer completedCount,
        Integer missingCount,
        Double completionRate,
        Integer pendingReviewCount,
        Integer rejectedCount
    ) {
    }

    public record AgentGovernanceExportPrecheckSummary(
        Integer totalCount,
        Integer readyCount,
        Integer blockedCount,
        Integer missingCount,
        Integer pendingReviewCount,
        Integer rejectedCount,
        Integer conversionRequiredCount,
        Integer unsupportedPreviewCount
    ) {
    }

    public record AgentGovernanceOverviewResponse(
        Long projectId,
        AgentGovernanceStandardStatus standardStatus,
        AgentGovernanceDeliveryStatus documentDelivery,
        AgentGovernanceDeliveryStatus drawingDelivery,
        Integer pendingReviewCount,
        Integer rejectedCount,
        Integer rectificationPendingCount,
        String packageStatus,
        AgentGovernanceExportPrecheckSummary exportPrecheckSummary,
        String summaryText,
        java.util.List<String> nextActions
    ) {
    }

    public record AgentGovernanceMissingItemsResponse(
        Long projectId,
        String targetType,
        Integer totalCount,
        java.util.List<AgentGovernanceMissingItem> rows
    ) {
    }

    public record AgentGovernanceMissingItem(
        String missingItemKey,
        String viewType,
        String targetType,
        Long targetId,
        String targetName,
        Long deliverableDefinitionId,
        String deliverableDefinitionName,
        Long deliverableTypeId,
        String deliverableTypeName,
        String fileKind,
        String missingReason,
        String expectedFileKind,
        String explanation
    ) {
    }

    public record AgentBindingRecommendationRequest(
        String viewType,
        String targetType,
        Integer limitPerMissingItem
    ) {
    }

    public record AgentBindingRecommendationResponse(
        Long projectId,
        String viewType,
        String targetType,
        Integer totalCount,
        java.util.List<AgentBindingRecommendation> rows
    ) {
    }

    public record AgentBindingRecommendation(
        String recommendationId,
        String missingItemKey,
        String viewType,
        String targetType,
        Long targetId,
        String targetName,
        Long deliverableTypeId,
        String deliverableTypeName,
        Long fileResourceId,
        String fileName,
        String fileKind,
        String fileExt,
        String versionNo,
        String processStatus,
        String previewStatus,
        String statusLabel,
        String recommendationReason,
        String confidence,
        java.util.List<String> riskWarnings,
        Boolean metadataGovernanceRequired
    ) {
    }

    public record ApplyAgentRecommendationsRequest(
        @NotNull(message = "必须人工确认后才能应用推荐")
        Boolean confirmed,
        @NotBlank(message = "交付视图类型不能为空")
        String viewType,
        String targetType,
        @NotNull(message = "推荐项不能为空")
        java.util.List<ApplyAgentRecommendationItem> items
    ) {
    }

    public record ApplyAgentRecommendationItem(
        String recommendationId,
        String missingItemKey,
        String targetType,
        Long targetId,
        @NotNull(message = "交付物类型不能为空")
        Long deliverableTypeId,
        @NotNull(message = "文件资源不能为空")
        Long fileResourceId
    ) {
    }

    public record ApplyAgentRecommendationsResponse(
        Long projectId,
        String viewType,
        Integer requestedCount,
        Integer createdCount,
        Integer skippedCount,
        Integer failedCount,
        java.util.List<ApplyAgentRecommendationRowResult> results
    ) {
    }

    public record ApplyAgentRecommendationRowResult(
        String recommendationId,
        String missingItemKey,
        Long fileResourceId,
        Long bindingId,
        String status,
        String message
    ) {
    }

    public record AgentGovernanceCandidateFile(
        Long fileResourceId,
        String fileName,
        String fileKind,
        String fileExt,
        String versionNo,
        String processStatus,
        String businessTag,
        Boolean checksumPresent
    ) {
    }
}

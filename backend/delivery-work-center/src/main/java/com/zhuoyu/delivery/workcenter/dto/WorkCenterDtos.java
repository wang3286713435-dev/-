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
        Double completionRate,
        java.util.List<DeliveryCompletenessRow> rows
    ) {
    }
}

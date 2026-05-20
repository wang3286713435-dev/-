package com.zhuoyu.delivery.visualization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
}

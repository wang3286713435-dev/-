package com.zhuoyu.delivery.datasteward.asset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class AssetDtos {

    private AssetDtos() {
    }

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

    public record NasScanRequest(
        @NotBlank(message = "存储根编码不能为空")
        String rootCode,
        @NotBlank(message = "NAS 根路径不能为空")
        String rootPath,
        Long projectId,
        String projectCode,
        Boolean recursive,
        List<String> extensions,
        String versionNo,
        String discipline,
        String sourceName
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
        Instant lastModelUpdatedAt
    ) {
    }

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
        String accessUrl
    ) {
    }

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
        Integer modelFileCount,
        Long totalSizeBytes
    ) {
    }

    public record CapacityByProject(
        Long projectId,
        String projectCode,
        String projectName,
        Integer modelFileCount,
        Long totalSizeBytes
    ) {
    }

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
}

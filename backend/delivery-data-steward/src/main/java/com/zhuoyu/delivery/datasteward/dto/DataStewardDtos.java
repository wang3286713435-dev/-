package com.zhuoyu.delivery.datasteward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public final class DataStewardDtos {

    private DataStewardDtos() {
    }

    public record FileResourceRequest(
        @NotBlank(message = "文件名称不能为空")
        String originalName,
        @NotBlank(message = "文件类型不能为空")
        String fileKind,
        String mimeType,
        @PositiveOrZero(message = "文件大小不能为负数")
        Long sizeBytes,
        @NotBlank(message = "存储地址不能为空")
        String storageUri,
        String checksum,
        String businessTag,
        String versionNo,
        String processStatus
    ) {
    }

    public record ProcessFileRequest(String processStatus) {
    }

    public record FileResourceResponse(
        Long id,
        String assetUuid,
        Long projectId,
        String originalName,
        String fileKind,
        String mimeType,
        Long sizeBytes,
        String storageUri,
        String checksum,
        String businessTag,
        String versionNo,
        String processStatus,
        Instant processedAt
    ) {
    }

    public record ModelIntegrationRequest(
        @NotBlank(message = "模型集成名称不能为空")
        String name,
        @NotNull(message = "模型文件不能为空")
        Long modelFileId,
        String versionNo,
        Integer componentCount,
        String adapterPayloadJson
    ) {
    }

    public record ModelIntegrationResponse(
        Long id,
        Long projectId,
        String name,
        Long modelFileId,
        String versionNo,
        String status,
        Integer componentCount,
        Instant publishedAt,
        Long publishedBy,
        String adapterPayloadJson
    ) {
    }

    public record ManagedObjectRequest(
        @NotNull(message = "模型集成不能为空")
        Long modelIntegrationId,
        Long sectionNodeId,
        @NotBlank(message = "管理对象编码不能为空")
        String code,
        @NotBlank(message = "管理对象名称不能为空")
        String name,
        String objectType,
        String externalId,
        String discipline,
        String status,
        String propertiesJson
    ) {
    }

    public record ManagedObjectResponse(
        Long id,
        Long projectId,
        Long modelIntegrationId,
        Long sectionNodeId,
        String code,
        String name,
        String objectType,
        String externalId,
        String discipline,
        String status,
        String propertiesJson
    ) {
    }
}

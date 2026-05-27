package com.zhuoyu.delivery.datasteward.nas.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class ControlledNasDtos {

    private ControlledNasDtos() {
    }

    public record DirectoryCreateRequest(
        String parentPath,
        @NotBlank(message = "文件夹名称不能为空")
        String name
    ) {
    }

    public record DirectoryRenameRequest(
        @NotBlank(message = "请选择文件夹")
        String sourcePath,
        @NotBlank(message = "新名称不能为空")
        String newName
    ) {
    }

    public record DirectoryMoveRequest(
        @NotBlank(message = "请选择文件夹")
        String sourcePath,
        String targetDirectory
    ) {
    }

    public record DirectoryQuarantineRequest(
        @NotBlank(message = "请选择文件夹")
        String sourcePath,
        String reason
    ) {
    }

    public record FileRenameRequest(
        @NotBlank(message = "新名称不能为空")
        String newName
    ) {
    }

    public record FileMoveRequest(
        String targetDirectory
    ) {
    }

    public record FileQuarantineRequest(
        String reason
    ) {
    }

    public record NasWriteTrialConfigRequest(
        Boolean enabled,
        List<String> allowedRelativeRoots,
        List<String> allowedRoleCodes,
        List<Long> allowedUserIds,
        String trialModeNotice
    ) {
    }

    public record NasWriteTrialStatusResponse(
        Long projectId,
        boolean enabled,
        List<String> allowedRelativeRoots,
        List<String> allowedRoleCodes,
        List<Long> allowedUserIds,
        String trialModeNotice,
        String currentUserRoleCode,
        boolean roleAllowed,
        boolean accountAllowed,
        boolean directoryAllowed,
        boolean canWrite,
        String checkedDirectory,
        String disabledReason,
        String traceId,
        Instant updatedAt
    ) {
    }

    public record NasOperationResponse(
        Long operationId,
        Long projectId,
        String operationType,
        String targetType,
        Long targetId,
        Long fileId,
        Long directoryId,
        Long quarantineRecordId,
        String status,
        String displayName,
        String displayPath,
        String pathHint,
        String message,
        String traceId,
        Instant createdAt,
        String assetUuid,
        String checksum,
        String storageStatus,
        String storageProvider,
        Long sizeBytes
    ) {
        public NasOperationResponse(
            Long operationId,
            Long projectId,
            String operationType,
            String targetType,
            Long targetId,
            Long fileId,
            Long directoryId,
            Long quarantineRecordId,
            String status,
            String displayName,
            String displayPath,
            String pathHint,
            String message,
            String traceId,
            Instant createdAt
        ) {
            this(operationId, projectId, operationType, targetType, targetId, fileId, directoryId,
                quarantineRecordId, status, displayName, displayPath, pathHint, message, traceId,
                createdAt, null, null, null, null, null);
        }
    }

    public record NasOperationRecordResponse(
        Long operationId,
        Long projectId,
        String operationType,
        String targetType,
        Long targetId,
        Long fileId,
        Long directoryId,
        Long quarantineRecordId,
        String sourceDisplayPath,
        String targetDisplayPath,
        String status,
        String message,
        String failureReason,
        String traceId,
        Instant createdAt,
        Long createdBy
    ) {
    }

    public record NasQuarantineRecordResponse(
        Long quarantineRecordId,
        Long projectId,
        String targetType,
        Long fileId,
        Long directoryId,
        String originalDisplayPath,
        String displayName,
        String status,
        String reason,
        Instant quarantineUntil,
        Instant createdAt,
        Long createdBy,
        Long restoredBy,
        Instant restoredAt,
        String failureReason
    ) {
    }

    public record NasOperationListResponse(
        List<NasOperationRecordResponse> operations
    ) {
    }
}

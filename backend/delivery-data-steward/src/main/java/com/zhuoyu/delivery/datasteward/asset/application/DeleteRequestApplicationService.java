package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.QuarantineRecordResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AgentApiKeyRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetDeleteRequestRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetQuarantineRecordRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteRequestApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeleteRequestApplicationService.class);

    private final AssetDeleteRequestRepository deleteRequestRepository;
    private final AssetQuarantineRecordRepository quarantineRepository;
    private final BimAssetRepository bimAssetRepository;
    private final AgentApiKeyRepository agentApiKeyRepository;
    private final EventApplicationService eventApplicationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${delivery.asset.quarantine.root-path:/tmp/delivery-asset-quarantine}")
    private String quarantineRootPath;

    public DeleteRequestApplicationService(
        AssetDeleteRequestRepository deleteRequestRepository,
        AssetQuarantineRecordRepository quarantineRepository,
        BimAssetRepository bimAssetRepository,
        AgentApiKeyRepository agentApiKeyRepository,
        EventApplicationService eventApplicationService,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.deleteRequestRepository = deleteRequestRepository;
        this.quarantineRepository = quarantineRepository;
        this.bimAssetRepository = bimAssetRepository;
        this.agentApiKeyRepository = agentApiKeyRepository;
        this.eventApplicationService = eventApplicationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ===== create delete request =====

    @Transactional
    public DeleteRequestResponse createRequest(Long userId, DeleteRequestCreateRequest request) {
        requireProjectAccess(userId, request.projectId());

        // Verify file exists and belongs to the project
        List<FileAssetResponse> files = bimAssetRepository.listFileById(userId, request.fileId());
        if (files.isEmpty() || !files.getFirst().projectId().equals(request.projectId())) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在或不属于该项目", HttpStatus.NOT_FOUND);
        }

        String deleteType = request.deleteType();
        if (!"LOGICAL".equals(deleteType) && !"PHYSICAL".equals(deleteType)) {
            throw new BusinessException("DELETE_TYPE_INVALID",
                "删除类型无效，必须为 LOGICAL 或 PHYSICAL", HttpStatus.BAD_REQUEST);
        }

        String requestNo = deleteRequestRepository.generateRequestNo();
        Long drId = deleteRequestRepository.insert(requestNo, request.projectId(),
            request.fileId(), deleteType, request.reason(), "USER", userId);

        eventApplicationService.record("DELETE_REQUEST", request.projectId(), "DELETE_REQUEST",
            String.valueOf(drId), "delete.request", userId, "API",
            "提交删除申请: type=" + deleteType + " file=" + request.fileId(), null);

        return deleteRequestRepository.findById(drId).orElseThrow();
    }

    // ===== list / get =====

    public List<DeleteRequestResponse> listRequests(Long userId, Long projectId, String status) {
        requireProjectAccess(userId, projectId);
        return deleteRequestRepository.listByProject(projectId, status);
    }

    public DeleteRequestResponse getRequest(Long userId, Long requestId) {
        DeleteRequestResponse dr = deleteRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException("DELETE_REQUEST_NOT_FOUND",
                "删除申请不存在", HttpStatus.NOT_FOUND));
        requireProjectAccess(userId, dr.projectId());
        return dr;
    }

    // ===== approve =====

    @Transactional
    public DeleteRequestResponse approve(Long userId, Long requestId) {
        DeleteRequestResponse dr = validateTransition(requestId, "PENDING");
        requireProjectAccess(userId, dr.projectId());

        // Cannot approve own request
        if ("USER".equals(dr.requestedByType()) && dr.requestedBy().equals(userId)) {
            throw new BusinessException("DELETE_APPROVAL_SELF_FORBIDDEN",
                "不能审批自己提交的删除申请", HttpStatus.FORBIDDEN);
        }
        if ("AGENT".equals(dr.requestedByType())) {
            // requested_by is apiKeyId; check if the key's creator matches approver
            var keyOpt = agentApiKeyRepository.findById(dr.requestedBy());
            if (keyOpt.isPresent() && keyOpt.get().createdBy().equals(userId)) {
                throw new BusinessException("DELETE_APPROVAL_SELF_FORBIDDEN",
                    "不能审批自己 Agent 提交的删除申请", HttpStatus.FORBIDDEN);
            }
        }

        if (!deleteRequestRepository.approve(requestId, userId)) {
            throw new BusinessException("DELETE_REQUEST_STATUS_INVALID",
                "删除申请状态不允许审批", HttpStatus.BAD_REQUEST);
        }

        eventApplicationService.record("DELETE_APPROVAL", dr.projectId(), "DELETE_REQUEST",
            String.valueOf(requestId), "delete.approve", userId, "API",
            "审批通过删除申请: " + dr.requestNo(), null);

        return deleteRequestRepository.findById(requestId).orElseThrow();
    }

    // ===== reject =====

    @Transactional
    public DeleteRequestResponse reject(Long userId, Long requestId) {
        DeleteRequestResponse dr = validateTransition(requestId, "PENDING");
        requireProjectAccess(userId, dr.projectId());

        if (!deleteRequestRepository.reject(requestId, userId)) {
            throw new BusinessException("DELETE_REQUEST_STATUS_INVALID",
                "删除申请状态不允许驳回", HttpStatus.BAD_REQUEST);
        }

        eventApplicationService.record("DELETE_APPROVAL", dr.projectId(), "DELETE_REQUEST",
            String.valueOf(requestId), "delete.reject", userId, "API",
            "驳回删除申请: " + dr.requestNo(), null);

        return deleteRequestRepository.findById(requestId).orElseThrow();
    }

    // ===== execute (logical or physical) =====

    @Transactional
    public DeleteRequestResponse executeRequest(Long userId, Long requestId) {
        DeleteRequestResponse dr = validateTransition(requestId, "APPROVED");
        requireProjectAccess(userId, dr.projectId());

        if ("LOGICAL".equals(dr.deleteType())) {
            return executeLogicalDelete(userId, dr);
        } else {
            return executePhysicalQuarantine(userId, dr);
        }
    }

    private DeleteRequestResponse executeLogicalDelete(Long userId, DeleteRequestResponse dr) {
        // Logically delete: mark platform record as deleted, DO NOT touch NAS files
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET deleted = 1, delete_token = id, updated_by = :userId
            WHERE id = :fileId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", dr.fileId())
            .addValue("userId", userId));

        deleteRequestRepository.markExecuted(dr.id(), userId, null);

        eventApplicationService.record("DELETE_LOGICAL", dr.projectId(), "FILE_RESOURCE",
            String.valueOf(dr.fileId()), "delete.logical", userId, "API",
            "逻辑删除文件: fileId=" + dr.fileId() + " request=" + dr.requestNo(), null);

        return deleteRequestRepository.findById(dr.id()).orElseThrow();
    }

    private DeleteRequestResponse executePhysicalQuarantine(Long userId, DeleteRequestResponse dr) {
        // Get file details
        List<FileAssetResponse> files = bimAssetRepository.listFileById(userId, dr.fileId());
        if (files.isEmpty()) {
            String failReason = "文件不存在，无法隔离";
            deleteRequestRepository.markExecuted(dr.id(), userId, failReason);
            return deleteRequestRepository.findById(dr.id()).orElseThrow();
        }

        FileAssetResponse file = files.getFirst();
        String storagePath = file.storagePath();
        if (storagePath == null || !storagePath.startsWith("nas://")) {
            String failReason = "文件路径无效，无法隔离: " + storagePath;
            deleteRequestRepository.markExecuted(dr.id(), userId, failReason);
            return deleteRequestRepository.findById(dr.id()).orElseThrow();
        }

        String originalPath = storagePath.substring("nas://".length());
        Path source = Paths.get(originalPath);
        if (!Files.exists(source)) {
            String failReason = "原文件不存在于 NAS: " + originalPath;
            deleteRequestRepository.markExecuted(dr.id(), userId, failReason);
            return deleteRequestRepository.findById(dr.id()).orElseThrow();
        }

        // Create quarantine directory and move file
        try {
            Instant quarantineUntil = Instant.now().plus(30, ChronoUnit.DAYS);
            Path quarantineDir = Paths.get(quarantineRootPath, dr.projectId().toString());
            Files.createDirectories(quarantineDir);

            String quarantineFileName = dr.requestNo() + "_" + file.fileName();
            Path quarantinePath = quarantineDir.resolve(quarantineFileName);
            Files.move(source, quarantinePath, StandardCopyOption.REPLACE_EXISTING);

            // Create quarantine record
            Long qId = quarantineRepository.insert(dr.id(), dr.projectId(), dr.fileId(),
                originalPath, quarantinePath.toString(), quarantineUntil,
                dr.requestedByType(), dr.requestedBy(), dr.approvedBy(), userId);

            // Mark file as deleted in platform
            jdbcTemplate.update("""
                UPDATE data_file_resources
                SET deleted = 1, delete_token = id, updated_by = :userId
                WHERE id = :fileId AND deleted = 0
                """, new MapSqlParameterSource()
                .addValue("fileId", dr.fileId())
                .addValue("userId", userId));

            deleteRequestRepository.markExecuted(dr.id(), userId, null);

            eventApplicationService.record("DELETE_QUARANTINE", dr.projectId(), "QUARANTINE_RECORD",
                String.valueOf(qId), "delete.quarantine", userId, "API",
                "物理隔离文件: " + originalPath + " -> " + quarantinePath + " request=" + dr.requestNo(),
                null);

            return deleteRequestRepository.findById(dr.id()).orElseThrow();
        } catch (IOException e) {
            String failReason = "文件隔离失败: " + e.getMessage();
            log.error("Quarantine failed for request {}: {}", dr.id(), e.getMessage());
            deleteRequestRepository.markExecuted(dr.id(), userId, failReason);
            return deleteRequestRepository.findById(dr.id()).orElseThrow();
        }
    }

    // ===== quarantine queries =====

    public List<QuarantineRecordResponse> listQuarantineRecords(Long userId, Long projectId, String status) {
        requireProjectAccess(userId, projectId);
        return quarantineRepository.listByProject(projectId, status);
    }

    public QuarantineRecordResponse getQuarantineRecord(Long userId, Long id) {
        QuarantineRecordResponse qr = quarantineRepository.findById(id)
            .orElseThrow(() -> new BusinessException("QUARANTINE_RECORD_NOT_FOUND",
                "隔离记录不存在", HttpStatus.NOT_FOUND));
        requireProjectAccess(userId, qr.projectId());
        return qr;
    }

    // ===== restore from quarantine =====

    @Transactional
    public QuarantineRecordResponse restoreQuarantine(Long userId, Long quarantineId) {
        QuarantineRecordResponse qr = quarantineRepository.findById(quarantineId)
            .orElseThrow(() -> new BusinessException("QUARANTINE_RECORD_NOT_FOUND",
                "隔离记录不存在", HttpStatus.NOT_FOUND));
        requireProjectAccess(userId, qr.projectId());

        if (!"QUARANTINED".equals(qr.status())) {
            throw new BusinessException("QUARANTINE_STATUS_INVALID",
                "隔离记录状态不允许恢复: " + qr.status(), HttpStatus.BAD_REQUEST);
        }

        Path source = Paths.get(qr.quarantinePath());
        Path target = Paths.get(qr.originalPath());

        if (!Files.exists(source)) {
            String failReason = "隔离文件不存在: " + qr.quarantinePath();
            quarantineRepository.markRestored(quarantineId, userId, failReason);
            return quarantineRepository.findById(quarantineId).orElseThrow();
        }

        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            quarantineRepository.markRestored(quarantineId, userId, null);

            // Restore file in platform
            jdbcTemplate.update("""
                UPDATE data_file_resources
                SET deleted = 0, delete_token = 0, updated_by = :userId
                WHERE id = :fileId AND deleted = 1
                """, new MapSqlParameterSource()
                .addValue("fileId", qr.fileId())
                .addValue("userId", userId));

            eventApplicationService.record("DELETE_RESTORE", qr.projectId(), "QUARANTINE_RECORD",
                String.valueOf(quarantineId), "delete.restore", userId, "API",
                "恢复隔离文件: " + qr.originalPath() + " from " + qr.quarantinePath(), null);

            return quarantineRepository.findById(quarantineId).orElseThrow();
        } catch (IOException e) {
            String failReason = "文件恢复失败: " + e.getMessage();
            log.error("Restore failed for quarantine {}: {}", quarantineId, e.getMessage());
            quarantineRepository.markRestored(quarantineId, userId, failReason);
            return quarantineRepository.findById(quarantineId).orElseThrow();
        }
    }

    // ===== permanent delete from quarantine =====

    @Transactional
    public QuarantineRecordResponse permanentDelete(Long userId, Long quarantineId) {
        QuarantineRecordResponse qr = quarantineRepository.findById(quarantineId)
            .orElseThrow(() -> new BusinessException("QUARANTINE_RECORD_NOT_FOUND",
                "隔离记录不存在", HttpStatus.NOT_FOUND));
        requireProjectAccess(userId, qr.projectId());

        if (!"QUARANTINED".equals(qr.status())) {
            throw new BusinessException("QUARANTINE_STATUS_INVALID",
                "隔离记录状态不允许永久删除: " + qr.status(), HttpStatus.BAD_REQUEST);
        }

        // Check quarantine retention period (30 days)
        if (qr.quarantineUntil() != null && qr.quarantineUntil().isAfter(Instant.now())) {
            throw new BusinessException("QUARANTINE_RETENTION_NOT_EXPIRED",
                "隔离保留期未满，不允许永久删除。到期时间: " + qr.quarantineUntil(), HttpStatus.BAD_REQUEST);
        }

        Path quarantineFile = Paths.get(qr.quarantinePath());

        if (!Files.exists(quarantineFile)) {
            // File already gone, mark as deleted anyway
            quarantineRepository.markPermanentDeleted(quarantineId, userId, null);
            eventApplicationService.record("DELETE_PERMANENT", qr.projectId(), "QUARANTINE_RECORD",
                String.valueOf(quarantineId), "delete.permanent", userId, "API",
                "永久删除隔离文件(文件已不存在): " + qr.quarantinePath(), null);
            return quarantineRepository.findById(quarantineId).orElseThrow();
        }

        try {
            // Only delete from quarantine path, never from original NAS path
            Files.delete(quarantineFile);
            quarantineRepository.markPermanentDeleted(quarantineId, userId, null);

            eventApplicationService.record("DELETE_PERMANENT", qr.projectId(), "QUARANTINE_RECORD",
                String.valueOf(quarantineId), "delete.permanent", userId, "API",
                "永久删除隔离文件: " + qr.quarantinePath(), null);

            return quarantineRepository.findById(quarantineId).orElseThrow();
        } catch (IOException e) {
            String failReason = "永久删除文件失败: " + e.getMessage();
            log.error("Permanent delete failed for quarantine {}: {}", quarantineId, e.getMessage());
            quarantineRepository.markPermanentDeleted(quarantineId, userId, failReason);
            return quarantineRepository.findById(quarantineId).orElseThrow();
        }
    }

    // ===== helpers =====

    private void requireProjectAccess(Long userId, Long projectId) {
        List<?> projects = bimAssetRepository.listProjects(userId, null);
        boolean hasAccess = projects.stream()
            .anyMatch(p -> ((com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse) p).projectId().equals(projectId));
        if (!hasAccess) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED",
                "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }

    private DeleteRequestResponse validateTransition(Long requestId, String expectedStatus) {
        DeleteRequestResponse dr = deleteRequestRepository.findById(requestId)
            .orElseThrow(() -> new BusinessException("DELETE_REQUEST_NOT_FOUND",
                "删除申请不存在", HttpStatus.NOT_FOUND));
        if (!expectedStatus.equals(dr.status())) {
            throw new BusinessException("DELETE_REQUEST_STATUS_INVALID",
                "删除申请状态不允许此操作: " + dr.status(), HttpStatus.BAD_REQUEST);
        }
        return dr;
    }
}

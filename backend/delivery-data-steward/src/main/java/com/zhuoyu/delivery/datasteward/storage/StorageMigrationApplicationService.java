package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskRowResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationSummaryResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

@Service
public class StorageMigrationApplicationService {

    private static final String MODULE_CODE = "data-steward";
    private static final int DEFAULT_MAX_FILES = 10;
    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AssetApplicationService assetApplicationService;
    private final StorageService storageService;
    private final AuditLogApplicationService auditLogApplicationService;

    public StorageMigrationApplicationService(
        NamedParameterJdbcTemplate jdbcTemplate,
        AssetApplicationService assetApplicationService,
        StorageService storageService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetApplicationService = assetApplicationService;
        this.storageService = storageService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public StorageMigrationTaskDetailResponse createTask(
        Long userId,
        Long projectId,
        StorageMigrationTaskCreateRequest request
    ) {
        ensureProjectAccess(userId, projectId);
        String targetProvider = normalizeTargetProvider(request == null ? null : request.targetProvider());
        List<Long> fileIds = normalizeFileIds(request == null ? null : request.fileIds());
        Long batchId = insertBatch(projectId, targetProvider, fileIds.size(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "storage.migration.create",
            "STORAGE_MIGRATION_TASK", String.valueOf(batchId), userId,
            Map.of("taskId", batchId, "fileCount", fileIds.size(), "targetProvider", targetProvider));

        for (Long fileId : fileIds) {
            Long rowId = insertRow(batchId, projectId, fileId, targetProvider, userId);
            processRow(userId, projectId, batchId, rowId, fileId, targetProvider);
        }
        finalizeBatch(batchId, projectId, userId);
        return getTask(userId, batchId);
    }

    public List<StorageMigrationTaskListItemResponse> listTasks(Long userId, Long projectId) {
        ensureProjectAccess(userId, projectId);
        return jdbcTemplate.query("""
            SELECT id, project_id, target_provider, task_status, storage_state,
                   total_count, success_count, failure_count, skipped_count,
                   failure_reason, started_at, completed_at, created_at, updated_at
            FROM data_object_migration_task_batches
            WHERE project_id = :projectId
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 50
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new StorageMigrationTaskListItemResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("target_provider"),
            rs.getString("task_status"),
            rs.getString("storage_state"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            rs.getInt("skipped_count"),
            safeMessage(rs.getString("failure_reason"), rs.getString("task_status")),
            instant(rs.getTimestamp("started_at")),
            instant(rs.getTimestamp("completed_at")),
            instant(rs.getTimestamp("created_at")),
            instant(rs.getTimestamp("updated_at"))
        ));
    }

    public StorageMigrationSummaryResponse summary(Long userId, Long projectId) {
        ensureProjectAccess(userId, projectId);
        MigrationSummaryCounts counts = jdbcTemplate.queryForObject("""
            SELECT
                COUNT(f.id) AS total_file_count,
                SUM(CASE WHEN fov.file_id IS NOT NULL THEN 1 ELSE 0 END) AS object_stored_count
            FROM data_file_resources f
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) fov ON fov.file_id = f.id
            WHERE f.project_id = :projectId
              AND f.deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new MigrationSummaryCounts(
            rs.getLong("total_file_count"),
            rs.getLong("object_stored_count")
        ));
        if (counts == null) {
            counts = new MigrationSummaryCounts(0L, 0L);
        }
        MigrationTaskSummary taskSummary = jdbcTemplate.queryForObject("""
            SELECT
                SUM(CASE WHEN task_status = 'RUNNING' THEN 1 ELSE 0 END) AS running_task_count,
                SUM(CASE WHEN task_status IN ('FAILED', 'PARTIAL_FAILED') THEN 1 ELSE 0 END) AS failed_task_count,
                MAX(id) AS latest_task_id,
                MAX(updated_at) AS latest_task_updated_at
            FROM data_object_migration_task_batches
            WHERE project_id = :projectId
              AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> {
            Long latestTaskId = rs.getLong("latest_task_id");
            if (rs.wasNull()) {
                latestTaskId = null;
            }
            return new MigrationTaskSummary(
                rs.getLong("running_task_count"),
                rs.getLong("failed_task_count"),
                latestTaskId,
                instant(rs.getTimestamp("latest_task_updated_at"))
            );
        });
        if (taskSummary == null) {
            taskSummary = new MigrationTaskSummary(0L, 0L, null, null);
        }
        long objectStoredCount = counts.objectStoredCount() == null ? 0L : counts.objectStoredCount();
        long totalFileCount = counts.totalFileCount() == null ? 0L : counts.totalFileCount();
        return new StorageMigrationSummaryResponse(
            projectId,
            totalFileCount,
            objectStoredCount,
            Math.max(0L, totalFileCount - objectStoredCount),
            taskSummary.runningTaskCount() == null ? 0L : taskSummary.runningTaskCount(),
            taskSummary.failedTaskCount() == null ? 0L : taskSummary.failedTaskCount(),
            taskSummary.latestTaskId(),
            taskSummary.latestTaskUpdatedAt(),
            DEFAULT_MAX_FILES,
            DEFAULT_MAX_FILE_SIZE_BYTES,
            "当前只允许显式选择文件做对象存储镜像；NAS 原文件保留，不生成语义证据。"
        );
    }

    public StorageMigrationTaskDetailResponse getTask(Long userId, Long taskId) {
        BatchRow batch = findBatch(taskId);
        ensureProjectAccess(userId, batch.projectId());
        List<StorageMigrationTaskRowResponse> rows = listRows(taskId);
        return new StorageMigrationTaskDetailResponse(
            batch.id(),
            batch.projectId(),
            batch.targetProvider(),
            batch.taskStatus(),
            batch.storageState(),
            batch.totalCount(),
            batch.successCount(),
            batch.failureCount(),
            batch.skippedCount(),
            safeMessage(batch.failureReason(), batch.taskStatus()),
            batch.startedAt(),
            batch.completedAt(),
            batch.createdAt(),
            batch.updatedAt(),
            rows
        );
    }

    public StorageMigrationTaskDetailResponse retryTask(Long userId, Long taskId) {
        BatchRow batch = findBatch(taskId);
        ensureProjectAccess(userId, batch.projectId());
        List<Long> fileIds = jdbcTemplate.query("""
            SELECT file_id
            FROM data_object_migration_tasks
            WHERE task_batch_id = :taskId
              AND deleted = 0
              AND file_id IS NOT NULL
              AND migration_status = 'FAILED'
            ORDER BY id
            """, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> rs.getLong("file_id"));
        if (fileIds.isEmpty()) {
            fileIds = jdbcTemplate.query("""
                SELECT file_id
                FROM data_object_migration_tasks
                WHERE task_batch_id = :taskId
                  AND deleted = 0
                  AND file_id IS NOT NULL
                ORDER BY id
                """, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> rs.getLong("file_id"));
        }
        StorageMigrationTaskDetailResponse retried = createTask(userId, batch.projectId(),
            new StorageMigrationTaskCreateRequest(fileIds, batch.targetProvider()));
        auditLogApplicationService.record(batch.projectId(), MODULE_CODE, "storage.migration.retry",
            "STORAGE_MIGRATION_TASK", String.valueOf(taskId), userId,
            Map.of("sourceTaskId", taskId, "newTaskId", retried.taskId(), "fileCount", fileIds.size()));
        return retried;
    }

    private void processRow(
        Long userId,
        Long projectId,
        Long batchId,
        Long rowId,
        Long fileId,
        String targetProvider
    ) {
        markRowRunning(rowId);
        try {
            FileAssetResponse file = assetApplicationService.getFileById(userId, fileId);
            if (!Objects.equals(projectId, file.projectId())) {
                throw new BusinessException("STORAGE_MIGRATION_FILE_PROJECT_MISMATCH",
                    "文件不属于当前项目", HttpStatus.BAD_REQUEST);
            }
            validateLifecycle(file);
            validateSize(file);
            FileStorageStatusResponse status = storageService.fileStorageStatus(file);
            if (Boolean.TRUE.equals(status.objectStored())) {
                markRowSkipped(rowId, "ALREADY_STORED", "文件已有对象存储镜像，已跳过。");
                auditMigrationFile(projectId, fileId, userId, "storage.migration.file.skipped", "ALREADY_STORED");
                return;
            }
            StorageService.ObjectMirrorResult mirror = storageService.mirrorNasFileToObject(file, targetProvider);
            Long objectId = upsertStorageObject(mirror, userId);
            upsertFileObjectVersion(file, objectId, mirror, userId);
            updateFileChecksum(file.fileId(), mirror.checksum(), userId);
            markRowCompleted(rowId, objectId, mirror, "MIRRORED", "文件已建立对象存储镜像。");
            auditMigrationFile(projectId, fileId, userId, "storage.migration.file.completed", "MIRRORED");
        } catch (BusinessException exception) {
            markRowFailed(rowId, exception.getCode(), safeFailureMessage(exception));
            auditMigrationFile(projectId, fileId, userId, "storage.migration.file.failed", exception.getCode());
        } catch (Exception exception) {
            markRowFailed(rowId, "STORAGE_MIGRATION_FAILED", "对象存储镜像迁移失败。");
            auditMigrationFile(projectId, fileId, userId, "storage.migration.file.failed", "STORAGE_MIGRATION_FAILED");
        }
    }

    private Long upsertStorageObject(StorageService.ObjectMirrorResult mirror, Long userId) {
        jdbcTemplate.update("""
            INSERT INTO data_storage_objects (
                provider, bucket, object_key, etag, checksum, content_type, size_bytes,
                source_provider, source_uri_digest, source_path_digest,
                storage_state, migration_status, last_verified_at, created_by, updated_by
            ) VALUES (
                :provider, :bucket, :objectKey, :etag, :checksum, :contentType, :sizeBytes,
                :sourceProvider, :sourceUriDigest, :sourcePathDigest,
                'OBJECT_STORED', 'COMPLETED', :verifiedAt, :userId, :userId
            )
            ON DUPLICATE KEY UPDATE
                etag = VALUES(etag),
                checksum = VALUES(checksum),
                content_type = VALUES(content_type),
                size_bytes = VALUES(size_bytes),
                source_provider = VALUES(source_provider),
                source_uri_digest = VALUES(source_uri_digest),
                source_path_digest = VALUES(source_path_digest),
                storage_state = 'OBJECT_STORED',
                migration_status = 'COMPLETED',
                last_verified_at = VALUES(last_verified_at),
                updated_by = VALUES(updated_by),
                deleted = 0,
                delete_token = 0
            """, new MapSqlParameterSource()
            .addValue("provider", mirror.provider())
            .addValue("bucket", mirror.bucket())
            .addValue("objectKey", mirror.objectKey())
            .addValue("etag", mirror.etag())
            .addValue("checksum", mirror.checksum())
            .addValue("contentType", mirror.contentType())
            .addValue("sizeBytes", mirror.sizeBytes())
            .addValue("sourceProvider", mirror.sourceProvider())
            .addValue("sourceUriDigest", mirror.sourceUriDigest())
            .addValue("sourcePathDigest", mirror.sourcePathDigest())
            .addValue("verifiedAt", Timestamp.from(mirror.verifiedAt()))
            .addValue("userId", userId));
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM data_storage_objects
            WHERE provider = :provider
              AND bucket = :bucket
              AND object_key = :objectKey
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("provider", mirror.provider())
            .addValue("bucket", mirror.bucket())
            .addValue("objectKey", mirror.objectKey()), (rs, rowNum) -> rs.getLong("id"));
        if (rows.isEmpty()) {
            throw new BusinessException("STORAGE_MIGRATION_OBJECT_RECORD_FAILED",
                "对象存储记录写入失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return rows.getFirst();
    }

    private void upsertFileObjectVersion(
        FileAssetResponse file,
        Long objectId,
        StorageService.ObjectMirrorResult mirror,
        Long userId
    ) {
        Integer existing = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_object_versions
            WHERE file_id = :fileId
              AND storage_object_id = :objectId
              AND active = 1
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", file.fileId())
            .addValue("objectId", objectId), Integer.class);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update("""
            UPDATE data_file_object_versions
            SET active = 0,
                updated_by = :userId
            WHERE file_id = :fileId
              AND active = 1
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", file.fileId())
            .addValue("userId", userId));
        jdbcTemplate.update("""
            INSERT INTO data_file_object_versions (
                file_id, storage_object_id, version_no, active,
                storage_state, migration_status, checksum, content_type, size_bytes,
                last_verified_at, created_by, updated_by
            ) VALUES (
                :fileId, :objectId, :versionNo, 1,
                'OBJECT_STORED', 'COMPLETED', :checksum, :contentType, :sizeBytes,
                :verifiedAt, :userId, :userId
            )
            """, new MapSqlParameterSource()
            .addValue("fileId", file.fileId())
            .addValue("objectId", objectId)
            .addValue("versionNo", file.versionNo())
            .addValue("checksum", mirror.checksum())
            .addValue("contentType", mirror.contentType())
            .addValue("sizeBytes", mirror.sizeBytes())
            .addValue("verifiedAt", Timestamp.from(mirror.verifiedAt()))
            .addValue("userId", userId));
    }

    private void updateFileChecksum(Long fileId, String checksum, Long userId) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET checksum = COALESCE(checksum, :checksum),
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :userId
            WHERE id = :fileId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", fileId)
            .addValue("checksum", checksum)
            .addValue("userId", userId));
    }

    private Long insertBatch(Long projectId, String targetProvider, int totalCount, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_object_migration_task_batches (
                project_id, target_provider, task_status, storage_state,
                total_count, requested_by, started_at, created_by, updated_by
            ) VALUES (
                :projectId, :targetProvider, 'RUNNING', 'MIGRATION_PENDING',
                :totalCount, :userId, CURRENT_TIMESTAMP, :userId, :userId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("targetProvider", targetProvider)
            .addValue("totalCount", totalCount)
            .addValue("userId", userId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private Long insertRow(Long batchId, Long projectId, Long fileId, String targetProvider, Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_object_migration_tasks (
                task_batch_id, project_id, file_id, source_provider, target_provider,
                migration_status, storage_state, requested_by, created_by, updated_by
            ) VALUES (
                :batchId, :projectId, :fileId, 'NAS', :targetProvider,
                'PENDING', 'MIGRATION_PENDING', :userId, :userId, :userId
            )
            """, new MapSqlParameterSource()
            .addValue("batchId", batchId)
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("targetProvider", targetProvider)
            .addValue("userId", userId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void markRowRunning(Long rowId) {
        jdbcTemplate.update("""
            UPDATE data_object_migration_tasks
            SET migration_status = 'RUNNING',
                storage_state = 'MIGRATION_PENDING',
                started_at = CURRENT_TIMESTAMP
            WHERE id = :rowId
            """, new MapSqlParameterSource("rowId", rowId));
    }

    private void markRowSkipped(Long rowId, String resultCode, String message) {
        jdbcTemplate.update("""
            UPDATE data_object_migration_tasks
            SET migration_status = 'SKIPPED',
                storage_state = 'OBJECT_STORED',
                failure_reason = :message,
                completed_at = CURRENT_TIMESTAMP,
                last_verified_at = CURRENT_TIMESTAMP
            WHERE id = :rowId
            """, new MapSqlParameterSource()
            .addValue("rowId", rowId)
            .addValue("message", resultCode + ":" + message));
    }

    private void markRowCompleted(
        Long rowId,
        Long objectId,
        StorageService.ObjectMirrorResult mirror,
        String resultCode,
        String message
    ) {
        jdbcTemplate.update("""
            UPDATE data_object_migration_tasks
            SET target_object_id = :objectId,
                source_uri_digest = :sourceUriDigest,
                source_path_digest = :sourcePathDigest,
                migration_status = 'COMPLETED',
                storage_state = 'OBJECT_STORED',
                failure_reason = :message,
                completed_at = CURRENT_TIMESTAMP,
                last_verified_at = :verifiedAt
            WHERE id = :rowId
            """, new MapSqlParameterSource()
            .addValue("rowId", rowId)
            .addValue("objectId", objectId)
            .addValue("sourceUriDigest", mirror.sourceUriDigest())
            .addValue("sourcePathDigest", mirror.sourcePathDigest())
            .addValue("verifiedAt", Timestamp.from(mirror.verifiedAt()))
            .addValue("message", resultCode + ":" + message));
    }

    private void markRowFailed(Long rowId, String resultCode, String message) {
        jdbcTemplate.update("""
            UPDATE data_object_migration_tasks
            SET migration_status = 'FAILED',
                storage_state = 'MIGRATION_FAILED',
                failure_reason = :message,
                completed_at = CURRENT_TIMESTAMP
            WHERE id = :rowId
            """, new MapSqlParameterSource()
            .addValue("rowId", rowId)
            .addValue("message", resultCode + ":" + message));
    }

    private void finalizeBatch(Long batchId, Long projectId, Long userId) {
        Counts counts = countRows(batchId);
        String status;
        String storageState;
        String message = null;
        if (counts.failureCount() > 0 && counts.successCount() + counts.skippedCount() == 0) {
            status = "FAILED";
            storageState = "MIGRATION_FAILED";
            message = "全部文件迁移失败。";
        } else if (counts.failureCount() > 0) {
            status = "PARTIAL_FAILED";
            storageState = "MIGRATION_PARTIAL";
            message = "部分文件迁移失败。";
        } else {
            status = "COMPLETED";
            storageState = "OBJECT_STORED";
            message = "迁移任务已完成。";
        }
        jdbcTemplate.update("""
            UPDATE data_object_migration_task_batches
            SET task_status = :status,
                storage_state = :storageState,
                success_count = :successCount,
                failure_count = :failureCount,
                skipped_count = :skippedCount,
                failure_reason = :message,
                completed_at = CURRENT_TIMESTAMP,
                updated_by = :userId
            WHERE id = :batchId
            """, new MapSqlParameterSource()
            .addValue("batchId", batchId)
            .addValue("status", status)
            .addValue("storageState", storageState)
            .addValue("successCount", counts.successCount())
            .addValue("failureCount", counts.failureCount())
            .addValue("skippedCount", counts.skippedCount())
            .addValue("message", message)
            .addValue("userId", userId));
        auditLogApplicationService.record(projectId, MODULE_CODE, "storage.migration.completed",
            "STORAGE_MIGRATION_TASK", String.valueOf(batchId), userId,
            Map.of(
                "taskId", batchId,
                "status", status,
                "successCount", counts.successCount(),
                "failureCount", counts.failureCount(),
                "skippedCount", counts.skippedCount()
            ));
    }

    private Counts countRows(Long batchId) {
        return jdbcTemplate.queryForObject("""
            SELECT
                SUM(CASE WHEN migration_status = 'COMPLETED' THEN 1 ELSE 0 END) AS success_count,
                SUM(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failure_count,
                SUM(CASE WHEN migration_status = 'SKIPPED' THEN 1 ELSE 0 END) AS skipped_count
            FROM data_object_migration_tasks
            WHERE task_batch_id = :batchId
              AND deleted = 0
            """, new MapSqlParameterSource("batchId", batchId), (rs, rowNum) -> new Counts(
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            rs.getInt("skipped_count")
        ));
    }

    private BatchRow findBatch(Long taskId) {
        List<BatchRow> rows = jdbcTemplate.query("""
            SELECT id, project_id, target_provider, task_status, storage_state,
                   total_count, success_count, failure_count, skipped_count,
                   failure_reason, started_at, completed_at, created_at, updated_at
            FROM data_object_migration_task_batches
            WHERE id = :taskId
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> new BatchRow(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("target_provider"),
            rs.getString("task_status"),
            rs.getString("storage_state"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            rs.getInt("skipped_count"),
            rs.getString("failure_reason"),
            instant(rs.getTimestamp("started_at")),
            instant(rs.getTimestamp("completed_at")),
            instant(rs.getTimestamp("created_at")),
            instant(rs.getTimestamp("updated_at"))
        ));
        if (rows.isEmpty()) {
            throw new BusinessException("STORAGE_MIGRATION_TASK_NOT_FOUND",
                "迁移任务不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    private List<StorageMigrationTaskRowResponse> listRows(Long taskId) {
        return jdbcTemplate.query("""
            SELECT t.id, t.file_id, f.asset_uuid, f.original_name, f.file_kind, f.size_bytes,
                   t.migration_status, t.storage_state, t.failure_reason,
                   t.target_object_id, t.started_at, t.completed_at, t.last_verified_at
            FROM data_object_migration_tasks t
            LEFT JOIN data_file_resources f ON f.id = t.file_id
            WHERE t.task_batch_id = :taskId
              AND t.deleted = 0
            ORDER BY t.id
            """, new MapSqlParameterSource("taskId", taskId), (rs, rowNum) -> {
            String message = rs.getString("failure_reason");
            ResultMessage resultMessage = splitResultMessage(message);
            return new StorageMigrationTaskRowResponse(
                rs.getLong("id"),
                rs.getLong("file_id"),
                rs.getString("asset_uuid"),
                rs.getString("original_name"),
                rs.getString("file_kind"),
                rs.getLong("size_bytes"),
                rs.getString("migration_status"),
                rs.getString("storage_state"),
                resultMessage.resultCode(),
                resultMessage.message(),
                rs.getLong("target_object_id") > 0 || "OBJECT_STORED".equalsIgnoreCase(rs.getString("storage_state")),
                instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("completed_at")),
                instant(rs.getTimestamp("last_verified_at"))
            );
        });
    }

    private void ensureProjectAccess(Long userId, Long projectId) {
        if (userId == null || projectId == null) {
            throw new BusinessException("PROJECT_ACCESS_DENIED", "当前账号无项目权限", HttpStatus.FORBIDDEN);
        }
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles
            WHERE user_id = :userId
              AND project_id = :projectId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId), Integer.class);
        if (count == null || count == 0) {
            throw new BusinessException("PROJECT_ACCESS_DENIED", "当前账号无项目权限", HttpStatus.FORBIDDEN);
        }
    }

    private List<Long> normalizeFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_IDS_REQUIRED",
                "请明确选择要迁移的小样本文件", HttpStatus.BAD_REQUEST);
        }
        if (fileIds.size() > DEFAULT_MAX_FILES) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_LIMIT_EXCEEDED",
                "单次小样本迁移最多允许 10 个文件", HttpStatus.BAD_REQUEST);
        }
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long fileId : fileIds) {
            if (fileId == null || fileId <= 0) {
                throw new BusinessException("STORAGE_MIGRATION_FILE_ID_INVALID",
                    "文件ID不合法", HttpStatus.BAD_REQUEST);
            }
            unique.add(fileId);
        }
        if (unique.size() != fileIds.size()) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_IDS_DUPLICATED",
                "迁移文件列表存在重复项", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(unique);
    }

    private void validateLifecycle(FileAssetResponse file) {
        String status = file.lifecycleStatus() == null ? "" : file.lifecycleStatus().toUpperCase(Locale.ROOT);
        if (List.of("DELETED", "QUARANTINED").contains(status)) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_NOT_ACTIVE",
                "已删除或已移入回收站的文件不能迁移", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private void validateSize(FileAssetResponse file) {
        Long size = file.sizeBytes();
        if (size != null && size > DEFAULT_MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_TOO_LARGE",
                "文件超过本轮小样本迁移大小限制", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private String normalizeTargetProvider(String value) {
        if (value == null || value.isBlank()) {
            return "MINIO";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("S3".equals(normalized) || "OSS".equals(normalized)) {
            return "S3_COMPATIBLE";
        }
        if (!List.of("MINIO", "S3_COMPATIBLE").contains(normalized)) {
            throw new BusinessException("STORAGE_MIGRATION_TARGET_PROVIDER_INVALID",
                "对象存储目标只能是 MINIO 或 S3_COMPATIBLE", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void auditMigrationFile(Long projectId, Long fileId, Long userId, String actionCode, String resultCode) {
        auditLogApplicationService.record(projectId, MODULE_CODE, actionCode,
            "FILE_RESOURCE", String.valueOf(fileId), userId,
            Map.of("fileId", fileId, "resultCode", resultCode));
    }

    private String safeFailureMessage(BusinessException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "迁移失败。";
        }
        if (message.contains("/") || message.toLowerCase(Locale.ROOT).contains("storage_uri")) {
            return "迁移失败，请检查受控文件状态。";
        }
        return message;
    }

    private String safeMessage(String message, String status) {
        if (message == null || message.isBlank()) {
            return switch (status == null ? "" : status.toUpperCase(Locale.ROOT)) {
                case "COMPLETED" -> "迁移任务已完成。";
                case "FAILED" -> "迁移任务失败。";
                case "PARTIAL_FAILED" -> "部分文件迁移失败。";
                case "RUNNING" -> "迁移任务执行中。";
                default -> "迁移任务已记录。";
            };
        }
        ResultMessage resultMessage = splitResultMessage(message);
        return resultMessage.message();
    }

    private ResultMessage splitResultMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ResultMessage(null, "迁移任务已记录。");
        }
        int colon = raw.indexOf(':');
        if (colon <= 0 || colon == raw.length() - 1) {
            return new ResultMessage(null, raw);
        }
        return new ResultMessage(raw.substring(0, colon), raw.substring(colon + 1));
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record Counts(int successCount, int failureCount, int skippedCount) {
    }

    private record ResultMessage(String resultCode, String message) {
    }

    private record MigrationSummaryCounts(Long totalFileCount, Long objectStoredCount) {
    }

    private record MigrationTaskSummary(
        Long runningTaskCount,
        Long failedTaskCount,
        Long latestTaskId,
        Instant latestTaskUpdatedAt
    ) {
    }

    private record BatchRow(
        Long id,
        Long projectId,
        String targetProvider,
        String taskStatus,
        String storageState,
        Integer totalCount,
        Integer successCount,
        Integer failureCount,
        Integer skippedCount,
        String failureReason,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

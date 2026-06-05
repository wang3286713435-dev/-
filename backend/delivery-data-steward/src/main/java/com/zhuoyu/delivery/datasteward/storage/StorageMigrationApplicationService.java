package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationExecuteRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationExecuteResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationExecutionProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectStorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectStorageObjectificationCoverageResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationClosureAssessmentResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationCoverageReportResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationCoverageSummaryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationDistributionItem;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationFailureSummary;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationFullPlanRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationFullPlanResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityIssue;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityRepairRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityRepairResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationIntegrityResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationLongRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationLongRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanSampleItem;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunOverviewResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunProjectsResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveCandidatesResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveExecuteRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationWaveReportsResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskRowResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationSummaryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderReadinessResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageReadPolicyResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private static final long LARGE_FILE_RISK_BYTES = 500L * 1024L * 1024L;
    private static final int DEFAULT_DRY_RUN_LIMIT = 500;
    private static final int MAX_DRY_RUN_LIMIT = 5000;
    private static final int CONTROLLED_EXPANSION_MAX_PROJECTS = 5;
    private static final int CONTROLLED_EXPANSION_MAX_FILES_TOTAL = 15;
    private static final int CONTROLLED_EXPANSION_MAX_FILES_PER_PROJECT = 15;
    private static final long CONTROLLED_EXPANSION_MAX_BYTES_PER_PROJECT = 50L * 1024L * 1024L;
    private static final long CONTROLLED_EXPANSION_MAX_BYTES_TOTAL = 100L * 1024L * 1024L;
    private static final String CONTROLLED_EXPANSION_TASK_SOURCE = "MULTI_PROJECT_CONTROLLED_EXECUTION";
    private static final long FULL_PLAN_DEFAULT_BATCH_BYTES = 50L * 1024L * 1024L;
    private static final long LONG_RUN_PILOT_PROJECT_ID = 503L;
    private static final int LONG_RUN_DEFAULT_BATCH_FILE_LIMIT = 5;
    private static final int LONG_RUN_DEFAULT_CONTINUOUS_BATCHES = 1;
    private static final int LONG_RUN_MAX_CONTINUOUS_BATCHES = 5;
    private static final long LONG_RUN_MAX_BATCH_BYTES = 512L * 1024L * 1024L;
    private static final long LONG_RUN_MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L;
    private static final int INTEGRITY_DEFAULT_SAMPLE_LIMIT = 5000;
    private static final int INTEGRITY_MAX_SAMPLE_LIMIT = 5000;
    private static final int INTEGRITY_REPAIR_DEFAULT_BATCH_FILE_LIMIT = 50;
    private static final int INTEGRITY_REPAIR_MAX_BATCH_FILE_LIMIT = 50;
    private static final long INTEGRITY_REPAIR_DEFAULT_BATCH_BYTES = 1024L * 1024L * 1024L;
    private static final long INTEGRITY_REPAIR_MAX_BATCH_BYTES = 1024L * 1024L * 1024L;
    private static final int INTEGRITY_REPAIR_MAX_WORKERS = 4;
    private static final String WAVE1_CODE = "M3G-7-WAVE1";
    private static final int WAVE1_MAX_PROJECTS = 3;
    private static final int WAVE1_MAX_TOTAL_FILES = CONTROLLED_EXPANSION_MAX_FILES_TOTAL;
    private static final long WAVE1_MAX_TOTAL_BYTES = CONTROLLED_EXPANSION_MAX_BYTES_TOTAL;
    private static final int WAVE1_MAX_FILES_PER_PROJECT = CONTROLLED_EXPANSION_MAX_FILES_PER_PROJECT;
    private static final long WAVE1_MAX_BYTES_PER_PROJECT = CONTROLLED_EXPANSION_MAX_BYTES_PER_PROJECT;
    private static final long WAVE1_MAX_FILE_SIZE_BYTES = DEFAULT_MAX_FILE_SIZE_BYTES;
    private static final long WAVE1_PROJECT_TOTAL_FILE_SOFT_LIMIT = 1000L;
    private static final long WAVE1_PROJECT_NAS_BYTES_SOFT_LIMIT = 10L * 1024L * 1024L * 1024L;
    private static final Set<String> WAVE1_EXCLUDED_PROJECT_CODES = Set.of("105", "95", "98", "99");
    private static final String RUN_CODE = "M3G-7R";
    private static final String COVERAGE_REPORT_CODE = "M3G-9";
    private static final int RUN_MAX_PROJECTS = 5;
    private static final int RUN_MAX_TOTAL_FILES = 200;
    private static final int RUN_MAX_FILES_PER_PROJECT = 50;
    private static final long RUN_MAX_TOTAL_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final long RUN_MAX_BYTES_PER_PROJECT = 2L * 1024L * 1024L * 1024L;
    private static final long RUN_MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L;
    private static final int RUN_MAX_CONTINUOUS_BATCHES = 3;
    private static final Set<String> RUN_GOVERNANCE_PROJECT_CODES = Set.of("95", "98", "99");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AssetApplicationService assetApplicationService;
    private final StorageService storageService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final Map<Long, Instant> pausedLongRuns = new ConcurrentHashMap<>();
    private volatile Instant objectificationRunPausedAt;

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
        return createTask(userId, projectId, request, DEFAULT_MAX_FILES, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    private StorageMigrationTaskDetailResponse createTask(
        Long userId,
        Long projectId,
        StorageMigrationTaskCreateRequest request,
        int maxFiles,
        long maxFileSizeBytes
    ) {
        ensureProjectAccess(userId, projectId);
        String targetProvider = normalizeTargetProvider(request == null ? null : request.targetProvider());
        List<Long> fileIds = normalizeFileIds(request == null ? null : request.fileIds(), maxFiles);
        Long batchId = insertBatch(projectId, targetProvider, fileIds.size(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "storage.migration.create",
            "STORAGE_MIGRATION_TASK", String.valueOf(batchId), userId,
            Map.of("taskId", batchId, "fileCount", fileIds.size(), "targetProvider", targetProvider));

        for (Long fileId : fileIds) {
            Long rowId = insertRow(batchId, projectId, fileId, targetProvider, userId);
            processRow(userId, projectId, batchId, rowId, fileId, targetProvider, maxFileSizeBytes);
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

    public StorageProviderReadinessResponse minioReadiness(Long userId) {
        ensureAnyProjectAccess(userId);
        return storageService.minioReadiness();
    }

    public StorageReadPolicyResponse readPolicy(Long userId) {
        ensureAnyProjectAccess(userId);
        StorageReadPolicyCounts counts = jdbcTemplate.queryForObject("""
            SELECT
                COUNT(f.id) AS total_file_count,
                COALESCE(SUM(CASE WHEN COALESCE(obj.object_stored, 0) = 1 THEN 1 ELSE 0 END), 0) AS object_stored_count,
                COALESCE(SUM(CASE WHEN COALESCE(obj.object_unreadable, 0) = 1 THEN 1 ELSE 0 END), 0) AS object_unreadable_count,
                COALESCE(SUM(CASE WHEN COALESCE(obj.object_stored, 0) = 0 AND COALESCE(obj.object_unreadable, 0) = 0
                    AND COALESCE(mig.pending, 0) = 0 AND COALESCE(mig.failed, 0) = 0
                    THEN 1 ELSE 0 END), 0) AS nas_only_count,
                COALESCE(SUM(CASE WHEN COALESCE(obj.object_stored, 0) = 0 AND COALESCE(mig.pending, 0) = 1
                    THEN 1 ELSE 0 END), 0) AS migration_pending_count,
                COALESCE(SUM(CASE WHEN COALESCE(obj.object_stored, 0) = 0 AND COALESCE(mig.failed, 0) = 1
                    THEN 1 ELSE 0 END), 0) AS migration_failed_count
            FROM data_file_resources f
            JOIN core_user_project_roles upr ON upr.project_id = f.project_id
                AND upr.user_id = :userId
                AND upr.deleted = 0
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN storage_state = 'OBJECT_STORED' THEN 1 ELSE 0 END) AS object_stored,
                       MAX(CASE WHEN storage_state = 'OBJECT_UNREADABLE' THEN 1 ELSE 0 END) AS object_unreadable
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                GROUP BY file_id
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING', 'QUEUED') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status IN ('FAILED', 'PARTIAL_FAILED') THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            WHERE f.deleted = 0
            """, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> new StorageReadPolicyCounts(
            rs.getLong("total_file_count"),
            rs.getLong("object_stored_count"),
            rs.getLong("nas_only_count"),
            rs.getLong("migration_pending_count"),
            rs.getLong("migration_failed_count"),
            rs.getLong("object_unreadable_count")
        ));
        if (counts == null) {
            counts = new StorageReadPolicyCounts(0L, 0L, 0L, 0L, 0L, 0L);
        }
        Long recentObjectReadFailureCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_audit_logs a
            JOIN core_user_project_roles upr ON upr.project_id = a.project_id
                AND upr.user_id = :userId
                AND upr.deleted = 0
            WHERE a.module_code = :moduleCode
              AND a.action_code = 'asset.file.access.failed'
              AND a.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
              AND CAST(a.details_json AS CHAR) LIKE '%ASSET_OBJECT_NOT_READABLE%'
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("moduleCode", MODULE_CODE), Long.class);
        Long recentNasFallbackCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_audit_logs a
            JOIN core_user_project_roles upr ON upr.project_id = a.project_id
                AND upr.user_id = :userId
                AND upr.deleted = 0
            WHERE a.module_code = :moduleCode
              AND a.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
              AND CAST(a.details_json AS CHAR) LIKE '%"fallbackUsed":true%'
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("moduleCode", MODULE_CODE), Long.class);
        return new StorageReadPolicyResponse(
            storageService.objectFirstReadEnabled(),
            storageService.nasFallbackEnabled(),
            counts.totalFileCount(),
            counts.objectStoredCount(),
            counts.nasOnlyCount(),
            counts.migrationPendingCount(),
            counts.migrationFailedCount(),
            counts.objectUnreadableCount(),
            recentObjectReadFailureCount == null ? 0L : recentObjectReadFailureCount,
            recentNasFallbackCount == null ? 0L : recentNasFallbackCount,
            storageService.nasFallbackEnabled()
                ? "对象优先读取已启用，NAS fallback 如被使用必须在票据、响应头和审计中显式标记。"
                : "对象优先读取已启用；对象副本不可读时不会静默回退 NAS，会返回明确错误。"
        );
    }

    public StorageObjectificationInventoryResponse inventory(Long userId, Long projectId) {
        if (projectId != null) {
            ensureProjectAccess(userId, projectId);
        } else {
            ensureAnyProjectAccess(userId);
        }
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        String projectFilter = "";
        if (projectId != null) {
            projectFilter = " AND p.id = :projectId\n";
            params.addValue("projectId", projectId);
        }
        List<ProjectStorageObjectificationInventoryResponse> projects = jdbcTemplate.query("""
            SELECT
                p.id AS project_id,
                p.code AS project_code,
                p.name AS project_name,
                p.project_stage,
                p.asset_source,
                COUNT(f.id) AS total_files,
                COALESCE(SUM(COALESCE(f.size_bytes, 0)), 0) AS total_bytes,
                COALESCE(SUM(CASE WHEN obj.file_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS object_stored_files,
                COALESCE(SUM(CASE WHEN obj.file_id IS NOT NULL THEN COALESCE(obj.size_bytes, f.size_bytes, 0) ELSE 0 END), 0) AS object_stored_bytes,
                COALESCE(SUM(CASE WHEN f.id IS NOT NULL AND obj.file_id IS NULL THEN 1 ELSE 0 END), 0) AS nas_only_files,
                COALESCE(SUM(CASE WHEN f.id IS NOT NULL AND obj.file_id IS NULL THEN COALESCE(f.size_bytes, 0) ELSE 0 END), 0) AS nas_only_bytes,
                COALESCE(SUM(CASE WHEN f.id IS NOT NULL AND obj.file_id IS NULL AND mig.pending = 1 THEN 1 ELSE 0 END), 0) AS migration_pending_files,
                COALESCE(SUM(CASE WHEN f.id IS NOT NULL AND obj.file_id IS NULL AND mig.failed = 1 THEN 1 ELSE 0 END), 0) AS migration_failed_files,
                COALESCE(SUM(CASE WHEN f.checksum IS NOT NULL AND f.checksum <> '' THEN 1 ELSE 0 END), 0) AS checksum_covered_files,
                COALESCE(SUM(CASE WHEN f.file_kind = 'MODEL' THEN 1 ELSE 0 END), 0) AS model_files,
                COALESCE(SUM(CASE WHEN f.file_kind = 'DRAWING' THEN 1 ELSE 0 END), 0) AS drawing_files,
                COALESCE(SUM(CASE WHEN f.file_kind = 'DOCUMENT' THEN 1 ELSE 0 END), 0) AS document_files,
                COALESCE(SUM(CASE WHEN COALESCE(f.size_bytes, 0) >= :largeFileBytes THEN 1 ELSE 0 END), 0) AS large_file_count,
                COALESCE(SUM(CASE WHEN f.id IS NOT NULL AND obj.file_id IS NULL
                    AND (f.storage_uri IS NULL OR f.storage_uri = ''
                         OR (UPPER(COALESCE(f.storage_provider, '')) NOT IN ('NAS', 'METADATA') AND f.storage_uri NOT LIKE 'nas://%' AND f.storage_uri NOT LIKE '/%'))
                    THEN 1 ELSE 0 END), 0) AS unreadable_path_files
            FROM core_projects p
            JOIN (
                SELECT DISTINCT project_id
                FROM core_user_project_roles
                WHERE user_id = :userId
                  AND deleted = 0
            ) upr ON upr.project_id = p.id
            LEFT JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            LEFT JOIN (
                SELECT file_id, MAX(size_bytes) AS size_bytes
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
                GROUP BY file_id
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            WHERE p.deleted = 0
            """ + projectFilter + """
            GROUP BY p.id, p.code, p.name, p.project_stage, p.asset_source
            ORDER BY total_files DESC, p.id DESC
            """, params.addValue("largeFileBytes", LARGE_FILE_RISK_BYTES), (rs, rowNum) -> {
            long totalFiles = rs.getLong("total_files");
            long objectStoredFiles = rs.getLong("object_stored_files");
            long checksumCoveredFiles = rs.getLong("checksum_covered_files");
            long largeFileCount = rs.getLong("large_file_count");
            long failedFiles = rs.getLong("migration_failed_files");
            long unreadablePathFiles = rs.getLong("unreadable_path_files");
            BigDecimal objectCoverage = percentage(objectStoredFiles, totalFiles);
            BigDecimal checksumCoverage = percentage(checksumCoveredFiles, totalFiles);
            List<String> riskMessages = inventoryRiskMessages(
                totalFiles,
                objectCoverage,
                checksumCoverage,
                largeFileCount,
                failedFiles,
                unreadablePathFiles
            );
            String assetSource = rs.getString("asset_source");
            String projectCategory = projectCategory(assetSource, rs.getString("project_code"), rs.getString("project_stage"));
            return new ProjectStorageObjectificationInventoryResponse(
                rs.getLong("project_id"),
                rs.getString("project_code"),
                rs.getString("project_name"),
                rs.getString("project_stage"),
                assetSource,
                projectCategory,
                Boolean.valueOf("REAL_NAS".equals(projectCategory)),
                totalFiles,
                rs.getLong("total_bytes"),
                objectStoredFiles,
                rs.getLong("object_stored_bytes"),
                rs.getLong("nas_only_files"),
                rs.getLong("nas_only_bytes"),
                rs.getLong("nas_only_bytes"),
                rs.getLong("migration_pending_files"),
                failedFiles,
                checksumCoveredFiles,
                checksumCoverage,
                rs.getLong("model_files"),
                rs.getLong("drawing_files"),
                rs.getLong("document_files"),
                largeFileCount,
                unreadablePathFiles,
                objectCoverage,
                riskLevel(totalFiles, objectCoverage, checksumCoverage, largeFileCount, failedFiles, unreadablePathFiles),
                riskMessages,
                List.of(),
                List.of()
            );
        });
        if (!projects.isEmpty()) {
            List<Long> projectIds = projects.stream().map(ProjectStorageObjectificationInventoryResponse::projectId).toList();
            Map<Long, List<StorageObjectificationDistributionItem>> fileKindDistributions =
                distributionBy(projectIds, "file_kind");
            Map<Long, List<StorageObjectificationDistributionItem>> extensionDistributions =
                distributionBy(projectIds, "LOWER(SUBSTRING_INDEX(original_name, '.', -1))");
            projects = projects.stream()
                .map(row -> withDistributions(row, fileKindDistributions.get(row.projectId()),
                    extensionDistributions.get(row.projectId())))
                .toList();
        }
        long totalProjects = projects.size();
        long totalFiles = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::totalFiles).sum();
        long totalBytes = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::totalBytes).sum();
        long objectStoredFiles = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::objectStoredFiles).sum();
        long objectStoredBytes = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::objectStoredBytes).sum();
        long nasOnlyFiles = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::nasOnlyFiles).sum();
        long pendingFiles = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::migrationPendingFiles).sum();
        long failedFiles = projects.stream().mapToLong(ProjectStorageObjectificationInventoryResponse::migrationFailedFiles).sum();
        ProjectStorageObjectificationInventoryResponse currentProject = projectId == null || projects.isEmpty()
            ? null
            : projects.getFirst();
        return new StorageObjectificationInventoryResponse(
            projectId == null,
            projectId,
            currentProject == null ? null : currentProject.projectCode(),
            currentProject == null ? null : currentProject.projectName(),
            totalProjects,
            totalFiles,
            totalBytes,
            objectStoredFiles,
            objectStoredBytes,
            nasOnlyFiles,
            pendingFiles,
            failedFiles,
            percentage(objectStoredFiles, totalFiles),
            projects
        );
    }

    public StorageObjectificationCoverageReportResponse objectificationCoverage(Long userId) {
        StorageObjectificationInventoryResponse inventory = inventory(userId, null);
        List<ProjectStorageObjectificationInventoryResponse> inventoryProjects = inventory.projects() == null
            ? List.of()
            : inventory.projects();
        List<Long> projectIds = inventoryProjects.stream()
            .map(ProjectStorageObjectificationInventoryResponse::projectId)
            .filter(Objects::nonNull)
            .toList();
        Map<Long, Instant> lastObjectifiedAt = lastObjectifiedAtByProject(projectIds);
        Map<Long, List<StorageObjectificationFailureSummary>> failures = coverageFailureSummariesByProject(projectIds);
        List<ProjectStorageObjectificationCoverageResponse> projects = inventoryProjects.stream()
            .map(row -> toCoverageProject(
                row,
                lastObjectifiedAt.get(row.projectId()),
                failures.getOrDefault(row.projectId(), List.of())
            ))
            .toList();
        StorageObjectificationCoverageSummaryResponse summary = coverageSummary(inventoryProjects, projects);
        StorageObjectificationClosureAssessmentResponse closureAssessment = closureAssessment(summary, projects);
        return new StorageObjectificationCoverageReportResponse(
            true,
            COVERAGE_REPORT_CODE,
            summary,
            closureAssessment,
            projects
        );
    }

    public StorageObjectificationPlanDryRunResponse dryRunObjectificationPlan(
        Long userId,
        Long projectId,
        StorageObjectificationPlanDryRunRequest request
    ) {
        ensureProjectAccess(userId, projectId);
        DryRunFilters filters = normalizeDryRunFilters(request);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("limit", filters.limit());
        StringBuilder where = new StringBuilder("""
            WHERE f.project_id = :projectId
              AND f.deleted = 0
            """);
        if (filters.directoryPath() != null) {
            where.append("\n AND (f.logical_path = :directoryPath OR f.logical_path LIKE :directoryPrefix)");
            params.addValue("directoryPath", filters.directoryPath());
            params.addValue("directoryPrefix", filters.directoryPath() + "/%");
        }
        if (!filters.fileKinds().isEmpty()) {
            where.append("\n AND f.file_kind IN (:fileKinds)");
            params.addValue("fileKinds", filters.fileKinds());
        }
        if (!filters.extensions().isEmpty()) {
            where.append("\n AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN (:extensions)");
            params.addValue("extensions", filters.extensions());
        }
        if (filters.minSizeBytes() != null) {
            where.append("\n AND COALESCE(f.size_bytes, 0) >= :minSizeBytes");
            params.addValue("minSizeBytes", filters.minSizeBytes());
        }
        if (filters.maxSizeBytes() != null) {
            where.append("\n AND COALESCE(f.size_bytes, 0) <= :maxSizeBytes");
            params.addValue("maxSizeBytes", filters.maxSizeBytes());
        }
        if ("HAS_CHECKSUM".equals(filters.checksumState())) {
            where.append("\n AND f.checksum IS NOT NULL AND f.checksum <> ''");
        } else if ("MISSING_CHECKSUM".equals(filters.checksumState())) {
            where.append("\n AND (f.checksum IS NULL OR f.checksum = '')");
        }
        if ("NAS_ONLY".equals(filters.storageState())) {
            where.append("\n AND obj.file_id IS NULL AND COALESCE(mig.failed, 0) = 0");
        } else if ("MIGRATION_FAILED".equals(filters.storageState())) {
            where.append("\n AND obj.file_id IS NULL AND COALESCE(mig.failed, 0) = 1");
        }
        List<DryRunCandidate> rows = jdbcTemplate.query("""
            SELECT
                f.id AS file_id,
                f.asset_uuid,
                f.original_name,
                f.file_kind,
                LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS extension,
                COALESCE(f.size_bytes, 0) AS size_bytes,
                f.checksum,
                f.storage_provider,
                CASE WHEN f.storage_uri IS NULL OR f.storage_uri = '' THEN 1 ELSE 0 END AS storage_reference_missing,
                CASE WHEN obj.file_id IS NOT NULL THEN 1 ELSE 0 END AS object_stored,
                COALESCE(mig.pending, 0) AS migration_pending,
                COALESCE(mig.failed, 0) AS migration_failed
            FROM data_file_resources f
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            """ + where + "\n" + """
            ORDER BY f.id
            LIMIT :limit
            """, params, (rs, rowNum) -> new DryRunCandidate(
            rs.getLong("file_id"),
            rs.getString("asset_uuid"),
            rs.getString("original_name"),
            rs.getString("file_kind"),
            normalizeExtension(rs.getString("extension")),
            rs.getLong("size_bytes"),
            rs.getString("checksum"),
            rs.getString("storage_provider"),
            rs.getInt("storage_reference_missing") == 1,
            rs.getInt("object_stored") == 1,
            rs.getInt("migration_pending") == 1,
            rs.getInt("migration_failed") == 1
        ));
        long selectedFileCount = 0L;
        long selectedTotalBytes = 0L;
        long objectStoredSkipCount = 0L;
        long missingChecksumCount = 0L;
        long oversizedCount = 0L;
        long unreadableRiskCount = 0L;
        boolean totalBytesCapped = false;
        List<StorageObjectificationPlanSampleItem> sampleItems = new ArrayList<>();
        for (DryRunCandidate row : rows) {
            String storageStatus = storageStatus(row);
            boolean missingChecksum = !hasText(row.checksum());
            boolean oversized = row.sizeBytes() >= LARGE_FILE_RISK_BYTES;
            boolean unreadableRisk = unreadableRisk(row);
            if (row.objectStored()) {
                objectStoredSkipCount++;
            }
            if (missingChecksum && !row.objectStored()) {
                missingChecksumCount++;
            }
            if (oversized && !row.objectStored()) {
                oversizedCount++;
            }
            if (unreadableRisk && !row.objectStored()) {
                unreadableRiskCount++;
            }
            String reason = dryRunReason(row, missingChecksum, oversized, unreadableRisk);
            boolean eligible = !row.objectStored() && !unreadableRisk;
            if (eligible && filters.maxTotalBytes() != null
                && selectedTotalBytes + row.sizeBytes() > filters.maxTotalBytes()) {
                eligible = false;
                totalBytesCapped = true;
                reason = "TOTAL_BYTES_CAP_EXCEEDED";
            }
            if (eligible) {
                selectedFileCount++;
                selectedTotalBytes += row.sizeBytes();
            }
            if (sampleItems.size() < 20) {
                sampleItems.add(new StorageObjectificationPlanSampleItem(
                    row.fileId(),
                    row.assetUuid(),
                    row.fileName(),
                    row.fileKind(),
                    row.extension(),
                    row.sizeBytes(),
                    missingChecksum ? "MISSING_CHECKSUM" : "HAS_CHECKSUM",
                    storageStatus,
                    reason
                ));
            }
        }
        List<String> riskMessages = new ArrayList<>();
        riskMessages.add("dry-run 仅生成计划，不复制文件、不修改 NAS、不启动迁移任务。");
        if (objectStoredSkipCount > 0) {
            riskMessages.add("已有对象版本的文件会在真实执行时跳过。");
        }
        if (missingChecksumCount > 0) {
            riskMessages.add("部分文件缺少 checksum，真实对象化前需计算或补齐校验值。");
        }
        if (oversizedCount > 0) {
            riskMessages.add("存在大文件，后续应拆批并关注 NAS 侧 MinIO 下载/上传性能。");
        }
        if (unreadableRiskCount > 0) {
            riskMessages.add("部分文件缺少受控存储引用或不在 NAS 源链路，真实迁移前需人工核查。");
        }
        if (totalBytesCapped) {
            riskMessages.add("已按 maxTotalBytes 截断选择范围，剩余文件未纳入本次计划。");
        }
        long estimatedBatches = selectedFileCount == 0 ? 0 : (long) Math.ceil(selectedFileCount / (double) DEFAULT_MAX_FILES);
        return new StorageObjectificationPlanDryRunResponse(
            true,
            false,
            projectId,
            selectedFileCount,
            selectedTotalBytes,
            objectStoredSkipCount,
            missingChecksumCount,
            oversizedCount,
            unreadableRiskCount,
            estimatedBatches,
            riskMessages,
            sampleItems
        );
    }

    public StorageObjectificationFullPlanResponse fullObjectificationPlan(
        Long userId,
        Long projectId,
        StorageObjectificationFullPlanRequest request
    ) {
        return fullObjectificationPlan(userId, projectId, request,
            DEFAULT_MAX_FILE_SIZE_BYTES, CONTROLLED_EXPANSION_MAX_BYTES_TOTAL);
    }

    private StorageObjectificationFullPlanResponse fullObjectificationPlan(
        Long userId,
        Long projectId,
        StorageObjectificationFullPlanRequest request,
        long maxFileSizeBytes,
        long maxBatchBytesLimit
    ) {
        ensureProjectAccess(userId, projectId);
        StorageObjectificationInventoryResponse inventory = inventory(userId, projectId);
        if (inventory.projects().isEmpty()) {
            throw new BusinessException("STORAGE_FULL_PLAN_PROJECT_NOT_FOUND",
                "项目对象化盘点不存在", HttpStatus.NOT_FOUND);
        }
        ProjectStorageObjectificationInventoryResponse project = inventory.projects().getFirst();
        int batchFileLimit = normalizeFullPlanBatchFileLimit(request == null ? null : request.batchFileLimit());
        long executionMaxFileSizeBytes = normalizeFullPlanMaxFileSize(maxFileSizeBytes);
        long batchBytesLimit = normalizeFullPlanBatchBytesLimit(
            request == null ? null : request.batchBytesLimit(), maxBatchBytesLimit);
        DryRunFilters filters = normalizeDryRunFilters(new StorageObjectificationPlanDryRunRequest(
            request == null ? null : request.directoryPath(),
            request == null ? null : request.fileKinds(),
            request == null ? null : request.extensions(),
            request == null ? null : request.minSizeBytes(),
            request == null ? null : request.maxSizeBytes(),
            request == null ? null : request.checksumState(),
            "ANY",
            MAX_DRY_RUN_LIMIT,
            null
        ));
        List<MultiProjectDryRunCandidate> rows = queryMultiProjectCandidates(
            userId,
            List.of(projectId),
            null,
            filters,
            MAX_DRY_RUN_LIMIT
        );

        long eligibleRemainingCount = 0L;
        long eligibleRemainingBytes = 0L;
        long nextBatchTotalBytes = 0L;
        List<StorageObjectificationPlanSampleItem> nextBatchItems = new ArrayList<>();
        List<StorageObjectificationPlanSampleItem> governanceItems = new ArrayList<>();
        for (MultiProjectDryRunCandidate row : rows) {
            String storageStatus = storageStatus(row);
            if ("OBJECT_STORED".equals(storageStatus)) {
                continue;
            }
            boolean missingChecksum = !hasText(row.checksum());
            String governanceReason = fullPlanGovernanceReason(row, executionMaxFileSizeBytes);
            if (governanceReason != null) {
                if (governanceItems.size() < 30) {
                    governanceItems.add(fullPlanItem(row, storageStatus, governanceReason));
                }
                continue;
            }
            eligibleRemainingCount++;
            eligibleRemainingBytes += row.sizeBytes();
            if (nextBatchItems.size() < batchFileLimit
                && nextBatchTotalBytes + row.sizeBytes() <= batchBytesLimit) {
                String reason = missingChecksum ? "MISSING_CHECKSUM" : "ELIGIBLE_NEXT_BATCH";
                nextBatchItems.add(fullPlanItem(row, storageStatus, reason));
                nextBatchTotalBytes += row.sizeBytes();
            }
        }

        LatestTaskSummary latestTask = latestTask(projectId);
        List<StorageObjectificationFailureSummary> failureReasons = failureSummaries(projectId);
        long skippedCount = skippedFileCount(projectId);
        List<String> riskMessages = fullPlanRiskMessages(project, eligibleRemainingCount, governanceItems, failureReasons);
        List<String> suggestions = fullPlanSuggestions(eligibleRemainingCount, nextBatchItems.size(), failureReasons);
        long estimatedRemainingBatches = eligibleRemainingCount == 0
            ? 0
            : (long) Math.ceil(eligibleRemainingCount / (double) batchFileLimit);

        return new StorageObjectificationFullPlanResponse(
            true,
            false,
            project.projectId(),
            project.projectCode(),
            project.projectName(),
            project.totalFiles(),
            project.totalBytes(),
            project.objectStoredFiles(),
            project.objectStoredBytes(),
            project.nasOnlyFiles(),
            project.nasOnlyBytes(),
            project.migrationPendingFiles(),
            project.migrationFailedFiles(),
            skippedCount,
            project.checksumCoveredFiles(),
            project.checksumCoverageRate(),
            project.objectificationCoverageRate(),
            eligibleRemainingCount,
            eligibleRemainingBytes,
            CONTROLLED_EXPANSION_MAX_FILES_TOTAL,
            executionMaxFileSizeBytes,
            batchFileLimit,
            batchBytesLimit,
            (long) nextBatchItems.size(),
            nextBatchTotalBytes,
            !nextBatchItems.isEmpty(),
            estimatedRemainingBatches,
            latestTask == null ? null : latestTask.taskId(),
            latestTask == null ? null : latestTask.taskStatus(),
            latestTask == null ? null : latestTask.successCount(),
            latestTask == null ? null : latestTask.skippedCount(),
            latestTask == null ? null : latestTask.failureCount(),
            latestTask == null ? null : latestTask.updatedAt(),
            failureReasons,
            riskMessages,
            suggestions,
            nextBatchItems,
            governanceItems
        );
    }

    public StorageObjectificationLongRunResponse longRunStatus(Long userId, Long projectId) {
        ensureLongRunPilot(userId, projectId);
        LongRunConfig config = normalizeLongRunConfig(null);
        StorageObjectificationFullPlanResponse plan = fullObjectificationPlan(userId, projectId,
            new StorageObjectificationFullPlanRequest(null, null, null, null, null, "ANY",
                config.batchFileLimit(), config.batchBytesLimit()),
            config.maxFileSizeBytes(), LONG_RUN_MAX_BATCH_BYTES);
        return buildLongRunResponse(
            plan,
            projectId,
            config,
            false,
            0,
            0L,
            0,
            List.of(),
            0,
            0,
            List.of(),
            List.of("105 长跑状态来自对象化任务表聚合；未启动后台无限循环。")
        );
    }

    public StorageObjectificationLongRunResponse startLongRun(
        Long userId,
        Long projectId,
        StorageObjectificationLongRunRequest request
    ) {
        return executeLongRun(userId, projectId, request, false);
    }

    public StorageObjectificationLongRunResponse pauseLongRun(Long userId, Long projectId) {
        ensureLongRunPilot(userId, projectId);
        pausedLongRuns.put(projectId, Instant.now());
        auditLogApplicationService.record(projectId, MODULE_CODE, "storage.objectification.long-run.pause",
            "PROJECT", String.valueOf(projectId), userId,
            Map.of("projectId", projectId, "runState", "PAUSED"));
        LongRunConfig config = normalizeLongRunConfig(null);
        StorageObjectificationFullPlanResponse plan = fullObjectificationPlan(userId, projectId,
            new StorageObjectificationFullPlanRequest(null, null, null, null, null, "ANY",
                config.batchFileLimit(), config.batchBytesLimit()),
            config.maxFileSizeBytes(), LONG_RUN_MAX_BATCH_BYTES);
        return buildLongRunResponse(
            plan,
            projectId,
            config,
            false,
            0,
            0L,
            0,
            List.of(),
            0,
            0,
            List.of(),
            List.of("长跑已暂停；后端不会继续推进下一批，需显式调用继续。")
        );
    }

    public StorageObjectificationLongRunResponse resumeLongRun(
        Long userId,
        Long projectId,
        StorageObjectificationLongRunRequest request
    ) {
        pausedLongRuns.remove(projectId);
        return executeLongRun(userId, projectId, request, true);
    }

    public StorageObjectificationLongRunResponse retryLongRunFailures(
        Long userId,
        Long projectId,
        StorageObjectificationLongRunRequest request
    ) {
        ensureLongRunPilot(userId, projectId);
        requireLongRunConfirmed(request);
        if (pausedLongRuns.containsKey(projectId)) {
            throw new BusinessException("STORAGE_LONG_RUN_PAUSED",
                "105 对象化长跑已暂停，请先继续后再重试失败项。", HttpStatus.CONFLICT);
        }
        StorageProviderReadinessResponse readiness = storageService.minioReadiness();
        if (!Boolean.TRUE.equals(readiness.writable()) || !"READY".equals(readiness.readinessStatus())) {
            throw new BusinessException("STORAGE_LONG_RUN_OBJECT_STORE_NOT_READY",
                "NAS 侧 MinIO 尚未就绪，不能重试失败项。", HttpStatus.PRECONDITION_FAILED);
        }

        LongRunConfig config = normalizeLongRunConfig(request);
        List<Long> retryFileIds = failedFileIdsForRetry(projectId, config.batchFileLimit(), config.maxFileSizeBytes());
        List<Long> createdTaskIds = new ArrayList<>();
        List<MultiProjectStorageObjectificationExecutionProject> batchResults = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        int processedBatchCount = 0;
        long processedFileCount = 0L;
        List<String> warnings = new ArrayList<>();
        if (retryFileIds.isEmpty()) {
            warnings.add("当前没有符合本轮大小和路径要求的失败文件可重试；请查看治理清单。");
        } else {
            MultiProjectStorageObjectificationExecuteResponse result = executeLongRunBatch(
                userId, projectId, retryFileIds, config, request == null ? null : request.targetProvider());
            processedBatchCount = 1;
            processedFileCount = result.selectedFileCount();
            createdTaskIds.addAll(result.createdTaskIds());
            batchResults.addAll(result.projectResults());
            createdCount += nullToZero(result.createdCount());
            skippedCount += nullToZero(result.skippedCount());
            failedCount += nullToZero(result.failedCount());
            warnings.add("已按受控小批口径重试失败项；已对象化文件会幂等跳过。");
        }

        StorageObjectificationFullPlanResponse plan = fullObjectificationPlan(userId, projectId,
            new StorageObjectificationFullPlanRequest(null, null, null, null, null, "ANY",
                config.batchFileLimit(), config.batchBytesLimit()),
            config.maxFileSizeBytes(), LONG_RUN_MAX_BATCH_BYTES);
        return buildLongRunResponse(
            plan,
            projectId,
            config,
            !retryFileIds.isEmpty(),
            processedBatchCount,
            processedFileCount,
            createdTaskIds.size(),
            createdTaskIds,
            createdCount,
            skippedCount,
            batchResults,
            appendLongRunSafetyWarnings(warnings, config)
        );
    }

    public StorageObjectificationIntegrityResponse objectificationIntegrity(Long userId, Long projectId, Integer sampleLimit) {
        ensureLongRunPilot(userId, projectId);
        int limit = normalizeIntegritySampleLimit(sampleLimit);
        String targetProvider = "MINIO";
        String targetBucket = storageService.defaultBucketFor(targetProvider);
        long totalObjectStoredCount = countActiveObjectStored(projectId);
        List<ActiveObjectRow> rows = queryActiveObjectRows(projectId, limit, targetProvider, targetBucket);
        IntegrityScan scan = scanIntegrityRows(rows, targetProvider, targetBucket, 20);
        return new StorageObjectificationIntegrityResponse(
            projectId,
            totalObjectStoredCount,
            scan.verifiedCount(),
            scan.missingCount(),
            scan.unreadableCount(),
            scan.sizeMismatchCount(),
            (long) rows.size(),
            scan.governanceItemCount(),
            scan.governanceItemCount() == 0 && rows.size() == totalObjectStoredCount,
            scan.latestFailureReason(),
            scan.issues(),
            List.of(
                "对象实体校验只做对象存储 stat 和大小比对，不读取文件正文。",
                "响应只返回脱敏状态，不返回底层对象定位信息或真实路径。"
            )
        );
    }

    public StorageObjectificationIntegrityRepairResponse repairObjectificationIntegrity(
        Long userId,
        Long projectId,
        StorageObjectificationIntegrityRepairRequest request
    ) {
        ensureLongRunPilot(userId, projectId);
        if (!Boolean.TRUE.equals(request == null ? null : request.confirmed())) {
            throw new BusinessException("STORAGE_INTEGRITY_REPAIR_CONFIRM_REQUIRED",
                "对象版本对齐修复必须 confirmed=true。", HttpStatus.BAD_REQUEST);
        }
        StorageProviderReadinessResponse readiness = storageService.minioReadiness();
        if (!Boolean.TRUE.equals(readiness.writable()) || !"READY".equals(readiness.readinessStatus())) {
            throw new BusinessException("STORAGE_INTEGRITY_OBJECT_STORE_NOT_READY",
                "NAS 侧 MinIO 尚未就绪，不能修复历史对象版本。", HttpStatus.PRECONDITION_FAILED);
        }

        IntegrityRepairConfig config = normalizeIntegrityRepairConfig(request);
        String targetProvider = normalizeTargetProvider(request == null ? null : request.targetProvider());
        String targetBucket = storageService.defaultBucketFor(targetProvider);
        List<ActiveObjectRow> rows = queryActiveObjectRows(projectId, INTEGRITY_MAX_SAMPLE_LIMIT, targetProvider, targetBucket);
        List<StorageObjectificationIntegrityIssue> results = new ArrayList<>();
        List<ActiveObjectRow> selectedRows = new ArrayList<>();
        long checkedCount = 0L;
        long missingBeforeCount = 0L;
        long skippedCount = 0L;
        long selectedBytes = 0L;
        String latestFailureReason = null;

        for (ActiveObjectRow row : rows) {
            checkedCount++;
            boolean aligned = objectLocationAligned(row, targetProvider, targetBucket);
            StorageService.ObjectProbeResult probe = aligned
                ? storageService.probeObject(row.provider(), row.bucket(), row.objectKey(), row.expectedSizeBytes())
                : null;
            if ("OBJECT_STORED".equals(row.storageState()) && probe != null && probe.verified()) {
                skippedCount++;
                continue;
            }
            missingBeforeCount++;
            long fileBytes = Math.max(0L, row.fileSizeBytes() == null ? 0L : row.fileSizeBytes());
            if (selectedRows.size() >= config.batchFileLimit()) {
                break;
            }
            if (fileBytes > config.maxFileSizeBytes()) {
                latestFailureReason = "文件超过本次修复单文件上限。";
                results.add(toIntegrityIssue(row, "FILE_TOO_LARGE_FOR_REPAIR", latestFailureReason, false));
                markActiveObjectVersionRepairFailed(row.fileId(), userId);
                continue;
            }
            if (selectedBytes + fileBytes > config.batchBytesLimit() && !selectedRows.isEmpty()) {
                break;
            }
            selectedRows.add(row);
            selectedBytes += fileBytes;
        }

        List<IntegrityRepairResult> repairResults = repairIntegrityRowsConcurrently(
            userId, projectId, targetProvider, config, selectedRows);
        long repairedCount = 0L;
        long failedCount = 0L;
        for (IntegrityRepairResult result : repairResults) {
            results.add(result.issue());
            if (result.repaired()) {
                repairedCount++;
            } else {
                failedCount++;
                latestFailureReason = result.issue().issueMessage();
            }
        }

        StorageObjectificationIntegrityResponse after = objectificationIntegrity(userId, projectId, INTEGRITY_DEFAULT_SAMPLE_LIMIT);
        auditLogApplicationService.record(projectId, MODULE_CODE, "storage.integrity.repair",
            "PROJECT", String.valueOf(projectId), userId,
            Map.of("checked", checkedCount, "missingBefore", missingBeforeCount,
                "repaired", repairedCount, "failed", failedCount));

        return new StorageObjectificationIntegrityRepairResponse(
            true,
            projectId,
            checkedCount,
            missingBeforeCount,
            repairedCount,
            skippedCount,
            failedCount,
            after.governanceItemCount(),
            after.totalObjectStoredCount(),
            after.verifiedObjectCount(),
            after.missingObjectCount(),
            after.unreadableObjectCount(),
            after.checksumMismatchCount(),
            latestFailureReason == null ? after.latestFailureReason() : latestFailureReason,
            results,
            List.of(
                "修复只复制对象存储副本，不移动、不删除、不重命名、不覆盖 NAS 原文件。",
                "file-access 不做 NAS 兜底掩盖；对象缺失必须修复或进入治理。"
            )
        );
    }

    public StorageObjectificationWaveCandidatesResponse wave1Candidates(Long userId) {
        StorageObjectificationInventoryResponse inventory = inventory(userId, null);
        List<StorageObjectificationWaveProject> candidates = new ArrayList<>();
        List<StorageObjectificationWaveProject> excluded = new ArrayList<>();
        for (ProjectStorageObjectificationInventoryResponse row : inventory.projects()) {
            StorageObjectificationWaveProject waveProject = toWaveProject(row);
            if (Boolean.TRUE.equals(waveProject.executable())) {
                candidates.add(waveProject);
            } else {
                excluded.add(waveProject);
            }
        }
        candidates = candidates.stream()
            .sorted(Comparator
                .comparingLong((StorageObjectificationWaveProject row) -> nullToZero(row.nasOnlyFiles()))
                .thenComparingLong(row -> nullToZero(row.nasOnlyBytes()))
                .thenComparing(StorageObjectificationWaveProject::projectId))
            .limit(WAVE1_MAX_PROJECTS)
            .toList();
        return new StorageObjectificationWaveCandidatesResponse(
            true,
            WAVE1_CODE,
            WAVE1_MAX_PROJECTS,
            WAVE1_MAX_TOTAL_FILES,
            WAVE1_MAX_TOTAL_BYTES,
            WAVE1_MAX_FILES_PER_PROJECT,
            WAVE1_MAX_BYTES_PER_PROJECT,
            WAVE1_MAX_FILE_SIZE_BYTES,
            waveWarnings(),
            candidates,
            excluded
        );
    }

    public MultiProjectStorageObjectificationPlanDryRunResponse dryRunWave1Objectification(
        Long userId,
        StorageObjectificationWaveDryRunRequest request
    ) {
        List<Long> projectIds = resolveWaveProjectIds(userId, request == null ? null : request.projectIds(),
            request == null ? null : request.maxProjects());
        MultiProjectStorageObjectificationPlanDryRunRequest delegate = new MultiProjectStorageObjectificationPlanDryRunRequest(
            projectIds,
            null,
            true,
            null,
            null,
            request == null ? null : request.extensions(),
            null,
            WAVE1_MAX_FILE_SIZE_BYTES,
            "ANY",
            "NAS_ONLY",
            Math.min(normalizeWaveTotalLimit(request == null ? null : request.limit()), WAVE1_MAX_TOTAL_FILES),
            Math.min(normalizeWaveTotalBytes(request == null ? null : request.maxTotalBytes()), WAVE1_MAX_TOTAL_BYTES),
            Math.min(normalizeWaveFilesPerProject(request == null ? null : request.maxFilesPerProject()), WAVE1_MAX_FILES_PER_PROJECT),
            Math.min(normalizeWaveBytesPerProject(request == null ? null : request.maxBytesPerProject()), WAVE1_MAX_BYTES_PER_PROJECT),
            1,
            null
        );
        return dryRunMultiProjectObjectificationPlan(userId, delegate);
    }

    public MultiProjectStorageObjectificationExecuteResponse executeWave1Objectification(
        Long userId,
        StorageObjectificationWaveExecuteRequest request
    ) {
        if (!Boolean.TRUE.equals(request == null ? null : request.confirmed())) {
            throw new BusinessException("STORAGE_WAVE_EXECUTION_CONFIRM_REQUIRED",
                "M3G-7 Wave 1 执行必须 confirmed=true。", HttpStatus.BAD_REQUEST);
        }
        List<Long> fileIds = normalizeExecutionFileIds(request == null ? null : request.fileIds());
        List<Long> projectIds = resolveWaveProjectIds(userId, request == null ? null : request.projectIds(), WAVE1_MAX_PROJECTS);
        MultiProjectStorageObjectificationExecuteRequest delegate = new MultiProjectStorageObjectificationExecuteRequest(
            projectIds,
            fileIds,
            true,
            null,
            null,
            null,
            null,
            WAVE1_MAX_FILE_SIZE_BYTES,
            "ANY",
            "NAS_ONLY",
            Math.min(normalizeWaveTotalLimit(request == null ? null : request.limit()), WAVE1_MAX_TOTAL_FILES),
            Math.min(normalizeWaveTotalBytes(request == null ? null : request.maxTotalBytes()), WAVE1_MAX_TOTAL_BYTES),
            Math.min(normalizeWaveFilesPerProject(request == null ? null : request.maxFilesPerProject()), WAVE1_MAX_FILES_PER_PROJECT),
            Math.min(normalizeWaveBytesPerProject(request == null ? null : request.maxBytesPerProject()), WAVE1_MAX_BYTES_PER_PROJECT),
            1,
            null,
            true,
            request == null ? null : request.targetProvider()
        );
        MultiProjectStorageObjectificationExecuteResponse response =
            executeMultiProjectObjectificationPlan(userId, delegate);
        auditLogApplicationService.record(null, MODULE_CODE, "storage.objectification.wave1.execute",
            "PROJECT_SET", WAVE1_CODE, userId,
            Map.of("projectIds", projectIds, "fileCount", fileIds.size(),
                "created", response.createdCount(), "skipped", response.skippedCount(),
                "failed", response.failedCount()));
        return response;
    }

    public StorageObjectificationWaveReportsResponse wave1Reports(Long userId) {
        StorageObjectificationInventoryResponse inventory = inventory(userId, null);
        List<StorageObjectificationWaveProject> projects = inventory.projects().stream()
            .map(this::toWaveProject)
            .filter(row -> Boolean.TRUE.equals(row.realNasProject())
                || "EXCLUDED_PROJECT_CODE".equals(row.exclusionReason()))
            .sorted(Comparator
                .comparing((StorageObjectificationWaveProject row) -> Boolean.TRUE.equals(row.executable()) ? 0 : 1)
                .thenComparing(StorageObjectificationWaveProject::projectId))
            .toList();
        long totalFiles = projects.stream().mapToLong(row -> nullToZero(row.totalFiles())).sum();
        long objectStoredFiles = projects.stream().mapToLong(row -> nullToZero(row.objectStoredFiles())).sum();
        long nasOnlyFiles = projects.stream().mapToLong(row -> nullToZero(row.nasOnlyFiles())).sum();
        long failedFiles = projects.stream().mapToLong(row -> nullToZero(row.migrationFailedFiles())).sum();
        return new StorageObjectificationWaveReportsResponse(
            true,
            WAVE1_CODE,
            projects.size(),
            totalFiles,
            objectStoredFiles,
            nasOnlyFiles,
            failedFiles,
            percentage(objectStoredFiles, totalFiles),
            waveWarnings(),
            projects
        );
    }

    private List<Long> resolveWaveProjectIds(Long userId, List<Long> requestedProjectIds, Integer requestedMaxProjects) {
        int maxProjects = Math.min(normalizeWaveProjectLimit(requestedMaxProjects), WAVE1_MAX_PROJECTS);
        List<StorageObjectificationWaveProject> executableProjects = wave1Candidates(userId).candidates();
        if (executableProjects.isEmpty()) {
            throw new BusinessException("STORAGE_WAVE_CANDIDATES_EMPTY",
                "当前没有符合 M3G-7 Wave 1 条件的真实项目。", HttpStatus.PRECONDITION_FAILED);
        }
        List<Long> normalized = normalizeOptionalProjectIds(requestedProjectIds);
        if (normalized.isEmpty()) {
            return executableProjects.stream()
                .limit(maxProjects)
                .map(StorageObjectificationWaveProject::projectId)
                .toList();
        }
        Map<Long, StorageObjectificationWaveProject> executableById = new HashMap<>();
        executableProjects.forEach(row -> executableById.put(row.projectId(), row));
        List<Long> result = new ArrayList<>();
        for (Long projectId : normalized) {
            if (!executableById.containsKey(projectId)) {
                throw new BusinessException("STORAGE_WAVE_PROJECT_NOT_EXECUTABLE",
                    "项目不在 M3G-7 Wave 1 可执行候选范围。", HttpStatus.BAD_REQUEST);
            }
            result.add(projectId);
        }
        if (result.size() > maxProjects) {
            throw new BusinessException("STORAGE_WAVE_PROJECT_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 最多选择 3 个真实项目。", HttpStatus.BAD_REQUEST);
        }
        return result;
    }

    private StorageObjectificationWaveProject toWaveProject(ProjectStorageObjectificationInventoryResponse row) {
        String exclusionReason = waveExclusionReason(row);
        boolean executable = exclusionReason == null;
        List<String> riskMessages = new ArrayList<>(row.riskMessages() == null ? List.of() : row.riskMessages());
        if (!executable) {
            riskMessages.add(waveExclusionMessage(exclusionReason));
        }
        if (riskMessages.isEmpty()) {
            riskMessages.add("当前项目适合 Wave 1 小批试点；执行仍受项目数、文件数和容量硬上限保护。");
        }
        long recommendedFileCount = executable
            ? Math.min(nullToZero(row.nasOnlyFiles()), (long) WAVE1_MAX_FILES_PER_PROJECT)
            : 0L;
        long recommendedBytes = executable
            ? Math.min(nullToZero(row.nasOnlyBytes()), WAVE1_MAX_BYTES_PER_PROJECT)
            : 0L;
        return new StorageObjectificationWaveProject(
            row.projectId(),
            row.projectCode(),
            row.projectName(),
            row.projectCategory(),
            row.realNasProject(),
            executable,
            executable ? "CANDIDATE" : "EXCLUDED",
            exclusionReason,
            row.totalFiles(),
            row.totalBytes(),
            row.objectStoredFiles(),
            row.nasOnlyFiles(),
            row.nasOnlyBytes(),
            row.migrationFailedFiles(),
            row.unreadablePathFiles(),
            recommendedFileCount,
            recommendedBytes,
            row.objectificationCoverageRate(),
            row.checksumCoverageRate(),
            riskMessages
        );
    }

    private String waveExclusionReason(ProjectStorageObjectificationInventoryResponse row) {
        if (!Boolean.TRUE.equals(row.realNasProject())) {
            return "NON_REAL_NAS_PROJECT";
        }
        String code = row.projectCode() == null ? "" : row.projectCode().trim();
        if (WAVE1_EXCLUDED_PROJECT_CODES.contains(code)) {
            return "EXCLUDED_PROJECT_CODE";
        }
        if (isTestOrSampleProject(row)) {
            return "TEST_OR_SAMPLE_PROJECT";
        }
        if (row.totalFiles() == null || row.totalFiles() <= 0) {
            return "NO_REGISTERED_FILES";
        }
        if (row.nasOnlyFiles() == null || row.nasOnlyFiles() <= 0) {
            return "ALREADY_OBJECTIFIED";
        }
        if (row.unreadablePathFiles() != null && row.unreadablePathFiles() > 0) {
            return "SOURCE_REFERENCE_REVIEW_REQUIRED";
        }
        if (row.migrationFailedFiles() != null && row.migrationFailedFiles() > 0) {
            return "HAS_FAILED_MIGRATION";
        }
        if (row.totalFiles() != null && row.totalFiles() > WAVE1_PROJECT_TOTAL_FILE_SOFT_LIMIT) {
            return "PROJECT_TOO_LARGE_FOR_WAVE1";
        }
        if (row.nasOnlyBytes() != null && row.nasOnlyBytes() > WAVE1_PROJECT_NAS_BYTES_SOFT_LIMIT) {
            return "PROJECT_BYTES_TOO_LARGE_FOR_WAVE1";
        }
        return null;
    }

    private String waveExclusionMessage(String reason) {
        if ("NON_REAL_NAS_PROJECT".equals(reason)) {
            return "非真实 NAS 项目，不进入 Wave 1。";
        }
        if ("EXCLUDED_PROJECT_CODE".equals(reason)) {
            return "105 已完成或 95/98/99 属于待治理项目，默认排除。";
        }
        if ("TEST_OR_SAMPLE_PROJECT".equals(reason)) {
            return "测试、样例、冒烟或归档项目不进入 Wave 1。";
        }
        if ("NO_REGISTERED_FILES".equals(reason)) {
            return "没有已登记文件，不进入 Wave 1。";
        }
        if ("ALREADY_OBJECTIFIED".equals(reason)) {
            return "已无 NAS_ONLY 文件，本轮无需执行。";
        }
        if ("SOURCE_REFERENCE_REVIEW_REQUIRED".equals(reason)) {
            return "存在存储引用风险，需人工治理后再对象化。";
        }
        if ("HAS_FAILED_MIGRATION".equals(reason)) {
            return "存在历史迁移失败项，需先治理失败原因。";
        }
        if ("PROJECT_TOO_LARGE_FOR_WAVE1".equals(reason)) {
            return "项目文件数超出 Wave 1 低风险范围，后续单独拆批。";
        }
        if ("PROJECT_BYTES_TOO_LARGE_FOR_WAVE1".equals(reason)) {
            return "待对象化容量超出 Wave 1 低风险范围，后续单独拆批。";
        }
        return "不符合 Wave 1 候选条件。";
    }

    private boolean isTestOrSampleProject(ProjectStorageObjectificationInventoryResponse row) {
        String category = row.projectCategory() == null ? "" : row.projectCategory().trim().toUpperCase(Locale.ROOT);
        if ("TEST_OR_SAMPLE".equals(category) || "ARCHIVED".equals(category)) {
            return true;
        }
        String text = String.join(" ",
            row.projectCode() == null ? "" : row.projectCode(),
            row.projectName() == null ? "" : row.projectName()
        ).toUpperCase(Locale.ROOT);
        return text.contains("SMOKE")
            || text.contains("TEST")
            || text.contains("SAMPLE")
            || text.contains("DEMO")
            || text.contains("冒烟")
            || text.contains("测试")
            || text.contains("样例")
            || text.contains("示例")
            || text.contains("归档");
    }

    private List<String> waveWarnings() {
        return List.of(
            "M3G-7 Wave 1 只选择非 105 的低风险真实项目小批对象化。",
            "本轮复用现有更保守硬上限：最多 3 个项目、总 15 个文件、总 100MB、单文件 10MB。",
            "执行只复制对象存储副本，不移动、不删除、不重命名、不覆盖 NAS 原文件。"
        );
    }

    private int normalizeWaveProjectLimit(Integer value) {
        if (value == null) {
            return WAVE1_MAX_PROJECTS;
        }
        if (value <= 0 || value > WAVE1_MAX_PROJECTS) {
            throw new BusinessException("STORAGE_WAVE_PROJECT_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 最多选择 3 个真实项目。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeWaveTotalLimit(Integer value) {
        if (value == null) {
            return WAVE1_MAX_TOTAL_FILES;
        }
        if (value <= 0 || value > WAVE1_MAX_TOTAL_FILES) {
            throw new BusinessException("STORAGE_WAVE_TOTAL_FILE_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 总文件数超过受控上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeWaveFilesPerProject(Integer value) {
        if (value == null) {
            return WAVE1_MAX_FILES_PER_PROJECT;
        }
        if (value <= 0 || value > WAVE1_MAX_FILES_PER_PROJECT) {
            throw new BusinessException("STORAGE_WAVE_PROJECT_FILE_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 单项目文件数超过受控上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeWaveTotalBytes(Long value) {
        if (value == null) {
            return WAVE1_MAX_TOTAL_BYTES;
        }
        if (value <= 0 || value > WAVE1_MAX_TOTAL_BYTES) {
            throw new BusinessException("STORAGE_WAVE_TOTAL_BYTES_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 总容量超过受控上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeWaveBytesPerProject(Long value) {
        if (value == null) {
            return WAVE1_MAX_BYTES_PER_PROJECT;
        }
        if (value <= 0 || value > WAVE1_MAX_BYTES_PER_PROJECT) {
            throw new BusinessException("STORAGE_WAVE_PROJECT_BYTES_LIMIT_EXCEEDED",
                "M3G-7 Wave 1 单项目容量超过受控上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    public StorageObjectificationRunOverviewResponse objectificationRunOverview(Long userId) {
        ensureAnyProjectAccess(userId);
        List<StorageObjectificationRunProject> projects = objectificationRunProjectRows(userId);
        long totalFiles = projects.stream().mapToLong(row -> nullToZero(row.totalFiles())).sum();
        long objectStoredFiles = projects.stream().mapToLong(row -> nullToZero(row.objectStoredFiles())).sum();
        long nasOnlyFiles = projects.stream().mapToLong(row -> nullToZero(row.nasOnlyFiles())).sum();
        long migrationFailedFiles = projects.stream().mapToLong(row -> nullToZero(row.migrationFailedFiles())).sum();
        long governanceItemCount = projects.stream().mapToLong(row -> nullToZero(row.governanceItemCount())).sum();
        long checksumCoveredFiles = projects.stream().mapToLong(row -> nullToZero(row.checksumCoveredFiles())).sum();
        return new StorageObjectificationRunOverviewResponse(
            true,
            RUN_CODE,
            objectificationRunPausedAt == null ? "READY" : "PAUSED",
            objectificationRunPausedAt != null,
            totalFiles,
            objectStoredFiles,
            nasOnlyFiles,
            migrationFailedFiles,
            governanceItemCount,
            checksumCoveredFiles,
            percentage(objectStoredFiles, totalFiles),
            percentage(checksumCoveredFiles, totalFiles),
            countRunProjects(projects, "EXECUTABLE"),
            countRunProjects(projects, "GOVERNANCE_REQUIRED"),
            countRunProjects(projects, "COMPLETED"),
            countRunProjects(projects, "SKIPPED"),
            RUN_MAX_PROJECTS,
            RUN_MAX_TOTAL_FILES,
            RUN_MAX_TOTAL_BYTES,
            RUN_MAX_FILES_PER_PROJECT,
            RUN_MAX_BYTES_PER_PROJECT,
            RUN_MAX_FILE_SIZE_BYTES,
            RUN_MAX_CONTINUOUS_BATCHES,
            objectificationRunWarnings(),
            projects
        );
    }

    public StorageObjectificationRunProjectsResponse objectificationRunProjects(Long userId) {
        ensureAnyProjectAccess(userId);
        List<StorageObjectificationRunProject> projects = objectificationRunProjectRows(userId);
        return new StorageObjectificationRunProjectsResponse(
            RUN_CODE,
            countRunProjects(projects, "EXECUTABLE"),
            countRunProjects(projects, "GOVERNANCE_REQUIRED"),
            countRunProjects(projects, "COMPLETED"),
            countRunProjects(projects, "SKIPPED"),
            objectificationRunWarnings(),
            projects
        );
    }

    public MultiProjectStorageObjectificationPlanDryRunResponse dryRunObjectificationRun(
        Long userId,
        StorageObjectificationRunRequest request
    ) {
        ensureAnyProjectAccess(userId);
        ObjectificationRunConfig config = normalizeRunConfig(request);
        ObjectificationRunSelection selection = selectObjectificationRunFiles(userId, config, false);
        return buildObjectificationRunDryRunResponse(selection, config, false);
    }

    public MultiProjectStorageObjectificationExecuteResponse startObjectificationRun(
        Long userId,
        StorageObjectificationRunRequest request
    ) {
        if (objectificationRunPausedAt != null) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PAUSED",
                "全项目对象化跑批已暂停；请使用 continue 接口继续。", HttpStatus.CONFLICT);
        }
        return executeObjectificationRun(userId, request, "storage.objectification.run.start", false);
    }

    public MultiProjectStorageObjectificationExecuteResponse continueObjectificationRun(
        Long userId,
        StorageObjectificationRunRequest request
    ) {
        objectificationRunPausedAt = null;
        return executeObjectificationRun(userId, request, "storage.objectification.run.continue", false);
    }

    public StorageObjectificationRunOverviewResponse pauseObjectificationRun(Long userId) {
        ensureAnyProjectAccess(userId);
        objectificationRunPausedAt = Instant.now();
        auditLogApplicationService.record(null, MODULE_CODE, "storage.objectification.run.pause",
            "PROJECT_SET", RUN_CODE, userId, Map.of("runCode", RUN_CODE));
        return objectificationRunOverview(userId);
    }

    public MultiProjectStorageObjectificationExecuteResponse retryFailedObjectificationRun(
        Long userId,
        StorageObjectificationRunRequest request
    ) {
        return executeObjectificationRun(userId, request, "storage.objectification.run.retry-failed", true);
    }

    private MultiProjectStorageObjectificationExecuteResponse executeObjectificationRun(
        Long userId,
        StorageObjectificationRunRequest request,
        String auditAction,
        boolean retryFailed
    ) {
        ensureAnyProjectAccess(userId);
        requireRunConfirmed(request);
        if (objectificationRunPausedAt != null) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PAUSED",
                "全项目对象化跑批已暂停；请先继续或取消暂停。", HttpStatus.CONFLICT);
        }
        StorageProviderReadinessResponse readiness = storageService.minioReadiness();
        if (!Boolean.TRUE.equals(readiness.writable()) || !"READY".equals(readiness.readinessStatus())) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_STORE_NOT_READY",
                "NAS 侧 MinIO 尚未就绪，不能执行全项目对象化跑批。", HttpStatus.PRECONDITION_FAILED);
        }
        ObjectificationRunConfig config = normalizeRunConfig(request);
        int remainingFiles = config.maxTotalFiles();
        long remainingBytes = config.maxTotalBytes();
        int processedBatches = 0;
        long selectedFileCount = 0L;
        long selectedTotalBytes = 0L;
        int createdTaskCount = 0;
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<Long> createdTaskIds = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        List<MultiProjectStorageObjectificationExecutionProject> projectResults = new ArrayList<>();

        for (int batch = 0; batch < config.maxContinuousBatches(); batch++) {
            if (remainingFiles <= 0 || remainingBytes <= 0) {
                break;
            }
            ObjectificationRunConfig roundConfig = config.withRemaining(remainingFiles, remainingBytes);
            ObjectificationRunSelection selection = selectObjectificationRunFiles(userId, roundConfig, retryFailed);
            if (selection.selectedFileCount() == 0) {
                break;
            }
            MultiProjectStorageObjectificationExecuteResponse response =
                executeObjectificationRunSelection(userId, selection, roundConfig, retryFailed);
            processedBatches++;
            selectedFileCount += nullToZero(response.selectedFileCount());
            selectedTotalBytes += nullToZero(response.selectedTotalBytes());
            createdTaskCount += nullToZero(response.createdTaskCount());
            createdTaskIds.addAll(response.createdTaskIds());
            createdCount += nullToZero(response.createdCount());
            skippedCount += nullToZero(response.skippedCount());
            failedCount += nullToZero(response.failedCount());
            failureReasons.addAll(response.failureReasons());
            projectResults.addAll(response.projectResults());
            remainingFiles -= nullToZero(response.selectedFileCount());
            remainingBytes -= nullToZero(response.selectedTotalBytes());
            if (nullToZero(response.failedCount()) > 0 && !config.continueOnFailure()) {
                break;
            }
        }

        List<String> warnings = objectificationRunWarnings();
        if (processedBatches == 0) {
            warnings.add(retryFailed ? "当前没有可重试失败文件；请查看治理清单。" : "当前没有符合本轮上限的可执行文件；请查看治理清单或覆盖率报告。");
        }
        if (failedCount > 0) {
            warnings.add("本轮存在失败文件，已保留任务失败原因并进入治理/重试链路。");
        }
        auditLogApplicationService.record(null, MODULE_CODE, auditAction,
            "PROJECT_SET", RUN_CODE, userId,
            Map.of(
                "runCode", RUN_CODE,
                "retryFailed", retryFailed,
                "processedBatches", processedBatches,
                "selectedFileCount", selectedFileCount,
                "selectedTotalBytes", selectedTotalBytes,
                "createdCount", createdCount,
                "skippedCount", skippedCount,
                "failedCount", failedCount
            ));
        return new MultiProjectStorageObjectificationExecuteResponse(
            false,
            true,
            retryFailed ? "ALL_PROJECT_OBJECTIFICATION_RETRY_FAILED" : "ALL_PROJECT_OBJECTIFICATION_RUN",
            projectResults.size(),
            selectedFileCount,
            selectedTotalBytes,
            config.maxTotalBytes(),
            config.maxFilesPerProject(),
            config.maxBytesPerProject(),
            createdTaskCount,
            createdTaskIds,
            createdCount,
            skippedCount,
            failedCount,
            failureReasons.stream().distinct().toList(),
            warnings,
            projectResults
        );
    }

    private MultiProjectStorageObjectificationExecuteResponse executeObjectificationRunSelection(
        Long userId,
        ObjectificationRunSelection selection,
        ObjectificationRunConfig config,
        boolean retryFailed
    ) {
        List<Long> createdTaskIds = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        List<MultiProjectStorageObjectificationExecutionProject> projectResults = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        for (ObjectificationRunProjectSelection project : selection.projects()) {
            if (project.fileIds().isEmpty()) {
                continue;
            }
            StorageMigrationTaskDetailResponse detail = createTask(userId, project.projectId(),
                new StorageMigrationTaskCreateRequest(project.fileIds(), config.targetProvider()),
                Math.max(project.fileIds().size(), config.maxFilesPerProject()),
                config.maxFileSizeBytes());
            createdTaskIds.add(detail.taskId());
            createdCount += nullToZero(detail.successCount());
            skippedCount += nullToZero(detail.skippedCount());
            failedCount += nullToZero(detail.failureCount());
            if (nullToZero(detail.failureCount()) > 0 && hasText(detail.message())) {
                failureReasons.add(detail.message());
            }
            projectResults.add(new MultiProjectStorageObjectificationExecutionProject(
                project.projectId(),
                project.projectCode(),
                project.projectName(),
                (long) project.fileIds().size(),
                project.selectedTotalBytes(),
                detail.taskId(),
                detail.taskStatus(),
                detail.successCount(),
                detail.skippedCount(),
                detail.failureCount(),
                detail.message(),
                project.fileIds()
            ));
        }
        return new MultiProjectStorageObjectificationExecuteResponse(
            false,
            true,
            retryFailed ? "ALL_PROJECT_OBJECTIFICATION_RETRY_FAILED" : "ALL_PROJECT_OBJECTIFICATION_RUN",
            projectResults.size(),
            selection.selectedFileCount(),
            selection.selectedTotalBytes(),
            config.maxTotalBytes(),
            config.maxFilesPerProject(),
            config.maxBytesPerProject(),
            createdTaskIds.size(),
            createdTaskIds,
            createdCount,
            skippedCount,
            failedCount,
            failureReasons,
            objectificationRunWarnings(),
            projectResults
        );
    }

    private MultiProjectStorageObjectificationPlanDryRunResponse buildObjectificationRunDryRunResponse(
        ObjectificationRunSelection selection,
        ObjectificationRunConfig config,
        boolean retryFailed
    ) {
        List<String> warnings = objectificationRunWarnings();
        warnings.add(retryFailed
            ? "本次 dry-run 只规划失败重试候选，不创建任务、不复制文件。"
            : "本次 dry-run 只规划全项目对象化跑批，不创建任务、不复制文件。");
        if (selection.governanceItemCount() > 0) {
            warnings.add("存在治理项，失败或不可读文件不会阻塞其他项目继续推进。");
        }
        return new MultiProjectStorageObjectificationPlanDryRunResponse(
            true,
            false,
            retryFailed ? "ALL_PROJECT_OBJECTIFICATION_RETRY_FAILED_DRY_RUN" : "ALL_PROJECT_OBJECTIFICATION_RUN_DRY_RUN",
            selection.requestedProjectCount(),
            selection.projects().size(),
            selection.selectedFileCount(),
            selection.selectedTotalBytes(),
            selection.objectStoredSkipCount(),
            selection.missingChecksumCount(),
            selection.oversizedCount(),
            selection.unreadableRiskCount(),
            selection.selectedFileCount() == 0 ? 0 : (long) Math.ceil(selection.selectedFileCount() / (double) Math.max(config.maxFilesPerProject(), 1)),
            config.maxFilesPerProject(),
            config.maxFileSizeBytes(),
            config.maxTotalBytes(),
            config.maxFilesPerProject(),
            config.maxBytesPerProject(),
            1,
            config.rateLimitBytesPerMinute(),
            warnings,
            selection.projects().stream().map(ObjectificationRunProjectSelection::toResponse).toList()
        );
    }

    private ObjectificationRunSelection selectObjectificationRunFiles(
        Long userId,
        ObjectificationRunConfig config,
        boolean retryFailed
    ) {
        List<StorageObjectificationRunProject> executableProjects = objectificationRunProjectRows(userId).stream()
            .filter(row -> Boolean.TRUE.equals(row.executable()))
            .filter(row -> config.projectIds().isEmpty() || config.projectIds().contains(row.projectId()))
            .sorted(Comparator
                .comparing((StorageObjectificationRunProject row) -> row.objectificationCoverageRate() == null ? BigDecimal.ZERO : row.objectificationCoverageRate())
                .thenComparingLong(row -> nullToZero(row.nasOnlyFiles()))
                .thenComparing(StorageObjectificationRunProject::projectId))
            .limit(config.maxProjects())
            .toList();
        if (!config.projectIds().isEmpty() && executableProjects.size() != config.projectIds().size()) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PROJECT_NOT_EXECUTABLE",
                "请求项目不在全项目对象化可执行队列中。", HttpStatus.BAD_REQUEST);
        }

        List<ObjectificationRunProjectSelection> projectSelections = new ArrayList<>();
        long selectedFileCount = 0L;
        long selectedTotalBytes = 0L;
        long objectStoredSkipCount = 0L;
        long missingChecksumCount = 0L;
        long oversizedCount = 0L;
        long unreadableRiskCount = 0L;
        long governanceItemCount = 0L;
        boolean totalFileCapped = false;
        boolean totalBytesCapped = false;
        for (StorageObjectificationRunProject project : executableProjects) {
            ObjectificationRunProjectSelection selection = new ObjectificationRunProjectSelection(project);
            DryRunFilters filters = new DryRunFilters(
                null,
                List.of(),
                List.of(),
                null,
                config.maxFileSizeBytes(),
                "ANY",
                retryFailed ? "MIGRATION_FAILED" : "NAS_ONLY",
                config.maxFilesPerProject() * 4,
                config.maxBytesPerProject()
            );
            List<MultiProjectDryRunCandidate> candidates = queryMultiProjectCandidates(
                userId,
                List.of(project.projectId()),
                null,
                filters,
                Math.min(Math.max(config.maxFilesPerProject() * 4, config.maxFilesPerProject()), MAX_DRY_RUN_LIMIT)
            );
            for (MultiProjectDryRunCandidate candidate : candidates) {
                String storageStatus = storageStatus(candidate);
                boolean missingChecksum = !hasText(candidate.checksum());
                boolean unreadableRisk = unreadableRisk(candidate);
                boolean oversized = candidate.sizeBytes() != null && candidate.sizeBytes() > config.maxFileSizeBytes();
                if (candidate.objectStored()) {
                    objectStoredSkipCount++;
                    selection.objectStoredSkipCount++;
                }
                if (missingChecksum && !candidate.objectStored()) {
                    missingChecksumCount++;
                    selection.missingChecksumCount++;
                }
                if (oversized && !candidate.objectStored()) {
                    oversizedCount++;
                    selection.oversizedCount++;
                }
                if ((unreadableRisk || candidate.migrationFailed()) && !candidate.objectStored()) {
                    governanceItemCount++;
                    selection.governanceItemCount++;
                    if (unreadableRisk) {
                        unreadableRiskCount++;
                        selection.unreadableRiskCount++;
                    }
                }
                String reason = runDryRunReason(candidate, missingChecksum, oversized, unreadableRisk, retryFailed);
                boolean eligible = !candidate.objectStored()
                    && !unreadableRisk
                    && !oversized
                    && !candidate.migrationPending()
                    && (!candidate.migrationFailed() || retryFailed);
                if (eligible && selectedFileCount >= config.maxTotalFiles()) {
                    eligible = false;
                    totalFileCapped = true;
                    reason = "TOTAL_FILE_LIMIT_EXCEEDED";
                }
                if (eligible && selectedTotalBytes + nullToZero(candidate.sizeBytes()) > config.maxTotalBytes()) {
                    eligible = false;
                    totalBytesCapped = true;
                    reason = "TOTAL_BYTES_CAP_EXCEEDED";
                }
                if (eligible && selection.selectedFileCount >= config.maxFilesPerProject()) {
                    eligible = false;
                    reason = "PROJECT_FILE_LIMIT_EXCEEDED";
                }
                if (eligible && selection.selectedTotalBytes + nullToZero(candidate.sizeBytes()) > config.maxBytesPerProject()) {
                    eligible = false;
                    reason = "PROJECT_BYTES_CAP_EXCEEDED";
                }
                if (eligible) {
                    selection.fileIds.add(candidate.fileId());
                    selection.selectedFileCount++;
                    selection.selectedTotalBytes += nullToZero(candidate.sizeBytes());
                    selectedFileCount++;
                    selectedTotalBytes += nullToZero(candidate.sizeBytes());
                }
                if (selection.sampleItems.size() < 20) {
                    selection.sampleItems.add(new StorageObjectificationPlanSampleItem(
                        candidate.fileId(),
                        candidate.assetUuid(),
                        candidate.fileName(),
                        candidate.fileKind(),
                        candidate.extension(),
                        candidate.sizeBytes(),
                        missingChecksum ? "MISSING_CHECKSUM" : "HAS_CHECKSUM",
                        storageStatus,
                        reason
                    ));
                }
            }
            if (!selection.sampleItems.isEmpty() || selection.selectedFileCount > 0) {
                projectSelections.add(selection);
            }
        }
        List<String> caps = new ArrayList<>();
        if (totalFileCapped) {
            caps.add("已按单次总文件数上限截断。");
        }
        if (totalBytesCapped) {
            caps.add("已按单次总容量上限截断。");
        }
        return new ObjectificationRunSelection(
            executableProjects.size(),
            selectedFileCount,
            selectedTotalBytes,
            objectStoredSkipCount,
            missingChecksumCount,
            oversizedCount,
            unreadableRiskCount,
            governanceItemCount,
            caps,
            projectSelections
        );
    }

    private List<StorageObjectificationRunProject> objectificationRunProjectRows(Long userId) {
        StorageObjectificationInventoryResponse inventory = inventory(userId, null);
        return inventory.projects().stream()
            .map(this::toRunProject)
            .sorted(Comparator
                .comparing((StorageObjectificationRunProject row) -> queueSortOrder(row.queueStatus()))
                .thenComparing(StorageObjectificationRunProject::projectId))
            .toList();
    }

    private StorageObjectificationRunProject toRunProject(ProjectStorageObjectificationInventoryResponse row) {
        RunQueue queue = runQueue(row);
        List<String> riskMessages = new ArrayList<>(row.riskMessages() == null ? List.of() : row.riskMessages());
        riskMessages.add(queue.message());
        long governanceItemCount = nullToZero(row.migrationFailedFiles()) + nullToZero(row.unreadablePathFiles());
        return new StorageObjectificationRunProject(
            row.projectId(),
            row.projectCode(),
            row.projectName(),
            row.projectCategory(),
            queue.status(),
            queue.reason(),
            row.realNasProject(),
            "EXECUTABLE".equals(queue.status()),
            row.totalFiles(),
            row.totalBytes(),
            row.objectStoredFiles(),
            row.nasOnlyFiles(),
            row.nasOnlyBytes(),
            row.migrationFailedFiles(),
            row.unreadablePathFiles(),
            governanceItemCount,
            row.checksumCoveredFiles(),
            row.objectificationCoverageRate(),
            row.checksumCoverageRate(),
            riskMessages.stream().distinct().toList()
        );
    }

    private ProjectStorageObjectificationCoverageResponse toCoverageProject(
        ProjectStorageObjectificationInventoryResponse row,
        Instant lastObjectifiedAt,
        List<StorageObjectificationFailureSummary> recordedFailures
    ) {
        String status = coverageStatus(row);
        long migrationFailedCount = nullToZero(row.migrationFailedFiles());
        long unreadableCount = nullToZero(row.unreadablePathFiles());
        long governanceCount = migrationFailedCount + unreadableCount;
        List<StorageObjectificationFailureSummary> failureSummary = coverageFailureSummary(
            recordedFailures,
            migrationFailedCount,
            unreadableCount
        );
        List<String> warnings = coverageProjectWarnings(row, status);
        List<String> nextActions = coverageProjectNextActions(row, status);
        return new ProjectStorageObjectificationCoverageResponse(
            row.projectId(),
            row.projectCode(),
            row.projectName(),
            row.projectStage(),
            row.assetSource(),
            row.projectCategory(),
            coverageOnboardingStatus(row, status),
            row.totalFiles(),
            row.objectStoredFiles(),
            row.nasOnlyFiles(),
            migrationFailedCount,
            governanceCount,
            unreadableCount,
            row.checksumCoverageRate(),
            row.objectificationCoverageRate(),
            row.totalBytes(),
            row.objectStoredBytes(),
            lastObjectifiedAt,
            readStrategySummary(row, status),
            status,
            failureSummary,
            warnings,
            nextActions
        );
    }

    private StorageObjectificationCoverageSummaryResponse coverageSummary(
        List<ProjectStorageObjectificationInventoryResponse> inventoryProjects,
        List<ProjectStorageObjectificationCoverageResponse> projects
    ) {
        long totalProjects = projects.size();
        long completedProjects = countCoverageProjects(projects, "COMPLETED");
        long partialProjects = countCoverageProjects(projects, "PARTIAL");
        long nasOnlyProjects = countCoverageProjects(projects, "NAS_ONLY");
        long failedOrGovernanceProjects = countCoverageProjects(projects, "FAILED_NEEDS_GOVERNANCE");
        long excludedProjects = countCoverageProjects(projects, "EXCLUDED");
        long totalFiles = projects.stream().mapToLong(row -> nullToZero(row.totalFiles())).sum();
        long objectStoredFiles = projects.stream().mapToLong(row -> nullToZero(row.objectStoredCount())).sum();
        long nasOnlyFiles = projects.stream().mapToLong(row -> nullToZero(row.nasOnlyCount())).sum();
        long failedFiles = projects.stream().mapToLong(row -> nullToZero(row.migrationFailedCount())).sum();
        long totalSizeBytes = projects.stream().mapToLong(row -> nullToZero(row.totalSizeBytes())).sum();
        long objectStoredSizeBytes = projects.stream().mapToLong(row -> nullToZero(row.objectStoredSizeBytes())).sum();
        long checksumCoveredFiles = inventoryProjects.stream()
            .mapToLong(row -> nullToZero(row.checksumCoveredFiles()))
            .sum();
        return new StorageObjectificationCoverageSummaryResponse(
            totalProjects,
            completedProjects,
            partialProjects,
            nasOnlyProjects,
            failedOrGovernanceProjects,
            excludedProjects,
            totalFiles,
            objectStoredFiles,
            nasOnlyFiles,
            failedFiles,
            percentage(objectStoredFiles, totalFiles),
            totalSizeBytes,
            objectStoredSizeBytes,
            percentage(checksumCoveredFiles, totalFiles)
        );
    }

    private StorageObjectificationClosureAssessmentResponse closureAssessment(
        StorageObjectificationCoverageSummaryResponse summary,
        List<ProjectStorageObjectificationCoverageResponse> projects
    ) {
        List<String> blockingReasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> nextActions = new ArrayList<>();
        ProjectStorageObjectificationCoverageResponse project105 = projects.stream()
            .filter(row -> Objects.equals(row.projectId(), LONG_RUN_PILOT_PROJECT_ID)
                || "105".equals(String.valueOf(row.projectCode()).trim()))
            .findFirst()
            .orElse(null);
        if (projects.isEmpty()) {
            blockingReasons.add("当前用户没有可解释的项目对象化盘点结果。");
        }
        if (project105 == null) {
            blockingReasons.add("未找到 105 / projectId=503 样板项目，无法证明 M3 全量对象化样板。");
        } else if (!"COMPLETED".equals(project105.status())) {
            blockingReasons.add("105 / projectId=503 尚未达到对象化 100%，不能作为 M3 完整样板项目。");
        }
        if (nullToZero(summary.failedOrGovernanceProjects()) > 0) {
            blockingReasons.add("仍有项目存在迁移失败或存储引用治理项，需先解释或治理后再做最终收口裁决。");
        }
        if (nullToZero(summary.objectStoredFiles()) == 0) {
            blockingReasons.add("当前没有任何已对象化文件，不能证明对象存储主链路可用。");
        }
        if (nullToZero(summary.partialProjects()) > 0) {
            warnings.add("存在部分对象化项目，说明主链路可用，但剩余文件仍需纳入后续批量计划。");
            nextActions.add("对 PARTIAL 项目按容量、文件数和风险分批生成 dry-run 计划。");
        }
        if (nullToZero(summary.nasOnlyProjects()) > 0) {
            warnings.add("存在 NAS_ONLY 项目，本报告只说明其状态可解释，不表示这些项目已完成对象化。");
            nextActions.add("对 NAS_ONLY 项目先做对象化 dry-run，再按低风险项目优先推进。");
        }
        if (nullToZero(summary.excludedProjects()) > 0) {
            warnings.add("存在非真实 NAS、测试样例或无登记文件项目，已按 EXCLUDED 单独解释。");
        }
        if (nextActions.isEmpty()) {
            nextActions.add("保持对象优先读取回归和文件访问安全回归，等待主 agent 做最终收口判断。");
        }
        return new StorageObjectificationClosureAssessmentResponse(
            blockingReasons.isEmpty(),
            blockingReasons,
            warnings,
            nextActions
        );
    }

    private String coverageStatus(ProjectStorageObjectificationInventoryResponse row) {
        long totalFiles = nullToZero(row.totalFiles());
        long objectStoredFiles = nullToZero(row.objectStoredFiles());
        if (!Boolean.TRUE.equals(row.realNasProject()) || isTestOrSampleProject(row) || totalFiles == 0) {
            return "EXCLUDED";
        }
        if (nullToZero(row.unreadablePathFiles()) > 0 || nullToZero(row.migrationFailedFiles()) > 0) {
            return "FAILED_NEEDS_GOVERNANCE";
        }
        if (objectStoredFiles >= totalFiles) {
            return "COMPLETED";
        }
        if (objectStoredFiles == 0) {
            return "NAS_ONLY";
        }
        return "PARTIAL";
    }

    private String coverageOnboardingStatus(ProjectStorageObjectificationInventoryResponse row, String status) {
        if (!Boolean.TRUE.equals(row.realNasProject())) {
            return "EXCLUDED_NON_REAL_NAS";
        }
        if (isTestOrSampleProject(row)) {
            return "EXCLUDED_TEST_OR_SAMPLE";
        }
        if (nullToZero(row.totalFiles()) == 0) {
            return "NO_REGISTERED_FILES";
        }
        return switch (status) {
            case "COMPLETED" -> "OBJECTIFICATION_COMPLETED";
            case "PARTIAL" -> "OBJECTIFICATION_PARTIAL";
            case "NAS_ONLY" -> "WAITING_OBJECTIFICATION";
            case "FAILED_NEEDS_GOVERNANCE" -> "GOVERNANCE_REQUIRED";
            default -> "EXCLUDED";
        };
    }

    private String readStrategySummary(ProjectStorageObjectificationInventoryResponse row, String status) {
        if ("EXCLUDED".equals(status)) {
            return "EXCLUDED";
        }
        long totalFiles = nullToZero(row.totalFiles());
        long objectStoredFiles = nullToZero(row.objectStoredFiles());
        if (totalFiles > 0 && objectStoredFiles >= totalFiles) {
            return "OBJECT_FIRST";
        }
        if (objectStoredFiles == 0) {
            return "LEGACY_NAS";
        }
        return "MIXED";
    }

    private List<String> coverageProjectWarnings(ProjectStorageObjectificationInventoryResponse row, String status) {
        List<String> warnings = new ArrayList<>();
        if ("NAS_ONLY".equals(status)) {
            warnings.add("该项目仍是 NAS_ONLY；当前可解释状态，不代表对象化已完成。");
        }
        if ("PARTIAL".equals(status)) {
            warnings.add("该项目处于 MIXED；已对象化文件走对象优先读取，剩余文件仍走受控历史 NAS。");
        }
        if ("FAILED_NEEDS_GOVERNANCE".equals(status)) {
            warnings.add("该项目存在失败或存储引用治理项，需先人工治理后再继续对象化。");
        }
        if ("EXCLUDED".equals(status)) {
            warnings.add("该项目不纳入 M3 主线对象化覆盖率完成口径。");
        }
        return warnings;
    }

    private List<String> coverageProjectNextActions(ProjectStorageObjectificationInventoryResponse row, String status) {
        if ("COMPLETED".equals(status)) {
            return List.of("继续走对象优先读取与文件访问安全回归。");
        }
        if ("PARTIAL".equals(status) || "NAS_ONLY".equals(status)) {
            return List.of("后续按 dry-run 计划分批对象化，仍不移动、不删除、不覆盖 NAS 原文件。");
        }
        if ("FAILED_NEEDS_GOVERNANCE".equals(status)) {
            return List.of("先处理迁移失败和存储引用治理项，再重新生成 dry-run。");
        }
        if (nullToZero(row.totalFiles()) == 0) {
            return List.of("如该项目需要纳入对象化，需先完成文件登记。");
        }
        return List.of("保留为解释性排除项。");
    }

    private List<StorageObjectificationFailureSummary> coverageFailureSummary(
        List<StorageObjectificationFailureSummary> recordedFailures,
        long migrationFailedCount,
        long unreadableCount
    ) {
        Map<String, StorageObjectificationFailureSummary> merged = new LinkedHashMap<>();
        for (StorageObjectificationFailureSummary failure : recordedFailures == null ? List.<StorageObjectificationFailureSummary>of() : recordedFailures) {
            String reasonCode = normalizeFailureReasonCode(failure.reasonCode());
            merged.put(reasonCode, new StorageObjectificationFailureSummary(
                reasonCode,
                coverageFailureMessage(reasonCode),
                nullToZero(failure.fileCount())
            ));
        }
        if (migrationFailedCount > 0 && merged.isEmpty()) {
            merged.put("HAS_FAILED_MIGRATION", new StorageObjectificationFailureSummary(
                "HAS_FAILED_MIGRATION",
                coverageFailureMessage("HAS_FAILED_MIGRATION"),
                migrationFailedCount
            ));
        }
        if (unreadableCount > 0) {
            merged.put("SOURCE_REFERENCE_REVIEW_REQUIRED", new StorageObjectificationFailureSummary(
                "SOURCE_REFERENCE_REVIEW_REQUIRED",
                coverageFailureMessage("SOURCE_REFERENCE_REVIEW_REQUIRED"),
                unreadableCount
            ));
        }
        return new ArrayList<>(merged.values());
    }

    private long countCoverageProjects(List<ProjectStorageObjectificationCoverageResponse> projects, String status) {
        return projects.stream().filter(row -> status.equals(row.status())).count();
    }

    private Map<Long, Instant> lastObjectifiedAtByProject(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Instant> result = new HashMap<>();
        jdbcTemplate.query("""
            SELECT f.project_id,
                   MAX(COALESCE(fov.last_verified_at, so.last_verified_at, fov.created_at)) AS last_objectified_at
            FROM data_file_resources f
            JOIN data_file_object_versions fov
              ON fov.file_id = f.id
             AND fov.active = 1
             AND fov.deleted = 0
             AND fov.storage_state = 'OBJECT_STORED'
            LEFT JOIN data_storage_objects so
              ON so.id = fov.storage_object_id
             AND so.deleted = 0
            WHERE f.deleted = 0
              AND f.project_id IN (:projectIds)
            GROUP BY f.project_id
            """, new MapSqlParameterSource("projectIds", projectIds), rs -> {
            result.put(rs.getLong("project_id"), instant(rs.getTimestamp("last_objectified_at")));
        });
        return result;
    }

    private Map<Long, List<StorageObjectificationFailureSummary>> coverageFailureSummariesByProject(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<StorageObjectificationFailureSummary>> result = new HashMap<>();
        jdbcTemplate.query("""
            SELECT project_id,
                   COALESCE(NULLIF(SUBSTRING_INDEX(failure_reason, ':', 1), ''), 'UNKNOWN') AS reason_code,
                   COUNT(DISTINCT file_id) AS file_count
            FROM data_object_migration_tasks
            WHERE project_id IN (:projectIds)
              AND migration_status = 'FAILED'
              AND deleted = 0
            GROUP BY project_id, reason_code
            ORDER BY file_count DESC, reason_code
            """, new MapSqlParameterSource("projectIds", projectIds), rs -> {
            long projectId = rs.getLong("project_id");
            String reasonCode = normalizeFailureReasonCode(rs.getString("reason_code"));
            result.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                .add(new StorageObjectificationFailureSummary(
                    reasonCode,
                    coverageFailureMessage(reasonCode),
                    rs.getLong("file_count")
                ));
        });
        return result;
    }

    private String normalizeFailureReasonCode(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_");
        if (normalized.length() > 80) {
            return normalized.substring(0, 80);
        }
        return normalized;
    }

    private String coverageFailureMessage(String reasonCode) {
        return switch (reasonCode == null ? "UNKNOWN" : reasonCode) {
            case "SOURCE_REFERENCE_REVIEW_REQUIRED" -> "存在缺少受控存储引用或路径不可读文件，需要人工治理。";
            case "HAS_FAILED_MIGRATION", "MIGRATION_FAILED" -> "存在历史对象化失败任务，需要重试或人工治理。";
            case "OBJECT_WRITE_FAILED" -> "对象副本写入失败，需要检查对象存储连接和文件状态。";
            case "CHECKSUM_MISMATCH" -> "对象副本校验不一致，需要重新核验后再处理。";
            default -> "存在对象化失败记录，已按原因分组隐藏底层路径细节。";
        };
    }

    private RunQueue runQueue(ProjectStorageObjectificationInventoryResponse row) {
        String code = row.projectCode() == null ? "" : row.projectCode().trim();
        if (!Boolean.TRUE.equals(row.realNasProject())) {
            return new RunQueue("SKIPPED", "NON_REAL_NAS_PROJECT", "非真实 NAS 项目，跳过跑批。");
        }
        if (isTestOrSampleProject(row)) {
            return new RunQueue("SKIPPED", "TEST_OR_SAMPLE_PROJECT", "测试、样例、冒烟或归档项目不进入全项目对象化跑批。");
        }
        if (RUN_GOVERNANCE_PROJECT_CODES.contains(code)) {
            return new RunQueue("GOVERNANCE_REQUIRED", "NONSTANDARD_PROJECT_CODE", "95 / 98 / 99 等不标准项目需先治理。");
        }
        if (nullToZero(row.totalFiles()) == 0) {
            return new RunQueue("SKIPPED", "NO_REGISTERED_FILES", "没有已登记文件，跳过跑批。");
        }
        if (nullToZero(row.unreadablePathFiles()) > 0) {
            return new RunQueue("GOVERNANCE_REQUIRED", "SOURCE_REFERENCE_REVIEW_REQUIRED", "存在缺少受控存储引用或路径不可读文件，需治理。");
        }
        if (nullToZero(row.migrationFailedFiles()) > 0) {
            return new RunQueue("GOVERNANCE_REQUIRED", "HAS_FAILED_MIGRATION", "存在历史迁移失败项，需重试或人工治理。");
        }
        if (nullToZero(row.nasOnlyFiles()) == 0) {
            return new RunQueue("COMPLETED", "ALL_OBJECT_STORED", "项目已完成对象化或当前无待对象化文件。");
        }
        return new RunQueue("EXECUTABLE", "READY", "项目可进入全项目对象化受控跑批。");
    }

    private String runDryRunReason(
        MultiProjectDryRunCandidate candidate,
        boolean missingChecksum,
        boolean oversized,
        boolean unreadableRisk,
        boolean retryFailed
    ) {
        if (candidate.objectStored()) {
            return "ALREADY_OBJECT_STORED";
        }
        if (candidate.migrationPending()) {
            return "MIGRATION_PENDING";
        }
        if (unreadableRisk) {
            return "SOURCE_REFERENCE_REVIEW_REQUIRED";
        }
        if (oversized) {
            return "FILE_TOO_LARGE_FOR_RUN";
        }
        if (candidate.migrationFailed() && !retryFailed) {
            return "MIGRATION_FAILED_REQUIRES_RETRY";
        }
        return missingChecksum ? "MISSING_CHECKSUM" : "ELIGIBLE_DRY_RUN";
    }

    private ObjectificationRunConfig normalizeRunConfig(StorageObjectificationRunRequest request) {
        return new ObjectificationRunConfig(
            normalizeRunProjectIds(request == null ? null : request.projectIds()),
            normalizeRunMaxProjects(request == null ? null : request.maxProjects()),
            normalizeRunMaxTotalFiles(request == null ? null : request.maxTotalFiles()),
            normalizeRunMaxTotalBytes(request == null ? null : request.maxTotalBytes()),
            normalizeRunMaxFilesPerProject(request == null ? null : request.maxFilesPerProject()),
            normalizeRunMaxBytesPerProject(request == null ? null : request.maxBytesPerProject()),
            normalizeRunMaxFileSizeBytes(request == null ? null : request.maxFileSizeBytes()),
            normalizeRunMaxContinuousBatches(request == null ? null : request.maxContinuousBatches()),
            normalizeRunRateLimit(request == null ? null : request.rateLimitBytesPerMinute()),
            !Boolean.FALSE.equals(request == null ? null : request.continueOnFailure()),
            normalizeTargetProvider(request == null ? null : request.targetProvider())
        );
    }

    private List<Long> normalizeRunProjectIds(List<Long> projectIds) {
        List<Long> normalized = normalizeOptionalProjectIds(projectIds);
        if (normalized.size() > RUN_MAX_PROJECTS) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PROJECT_LIMIT_EXCEEDED",
                "全项目对象化单次最多选择 5 个项目。", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private int normalizeRunMaxProjects(Integer value) {
        if (value == null) {
            return RUN_MAX_PROJECTS;
        }
        if (value <= 0 || value > RUN_MAX_PROJECTS) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PROJECT_LIMIT_EXCEEDED",
                "全项目对象化单次最多选择 5 个项目。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeRunMaxTotalFiles(Integer value) {
        if (value == null) {
            return RUN_MAX_TOTAL_FILES;
        }
        if (value <= 0 || value > RUN_MAX_TOTAL_FILES) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_TOTAL_FILE_LIMIT_EXCEEDED",
                "全项目对象化单次总文件数超过 200。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeRunMaxTotalBytes(Long value) {
        if (value == null) {
            return RUN_MAX_TOTAL_BYTES;
        }
        if (value <= 0 || value > RUN_MAX_TOTAL_BYTES) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_TOTAL_BYTES_LIMIT_EXCEEDED",
                "全项目对象化单次总容量超过 2GB。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeRunMaxFilesPerProject(Integer value) {
        if (value == null) {
            return RUN_MAX_FILES_PER_PROJECT;
        }
        if (value <= 0 || value > RUN_MAX_FILES_PER_PROJECT) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PROJECT_FILE_LIMIT_EXCEEDED",
                "全项目对象化单项目文件数超过 50。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeRunMaxBytesPerProject(Long value) {
        if (value == null) {
            return RUN_MAX_BYTES_PER_PROJECT;
        }
        if (value <= 0 || value > RUN_MAX_BYTES_PER_PROJECT) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_PROJECT_BYTES_LIMIT_EXCEEDED",
                "全项目对象化单项目容量超过 2GB。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeRunMaxFileSizeBytes(Long value) {
        if (value == null) {
            return RUN_MAX_FILE_SIZE_BYTES;
        }
        if (value <= 0 || value > RUN_MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_FILE_SIZE_LIMIT_EXCEEDED",
                "全项目对象化单文件大小超过 500MB。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeRunMaxContinuousBatches(Integer value) {
        if (value == null) {
            return 1;
        }
        if (value <= 0 || value > RUN_MAX_CONTINUOUS_BATCHES) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_CONTINUOUS_LIMIT_EXCEEDED",
                "全项目对象化连续批次数最多为 3。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private Long normalizeRunRateLimit(Long value) {
        if (value != null && value < 0) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_RATE_LIMIT_INVALID",
                "速率限制不能小于 0。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private void requireRunConfirmed(StorageObjectificationRunRequest request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.confirmed())) {
            throw new BusinessException("STORAGE_OBJECTIFICATION_RUN_CONFIRM_REQUIRED",
                "全项目对象化跑批必须 confirmed=true。", HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> objectificationRunWarnings() {
        return new ArrayList<>(List.of(
            "M3G-7R 全项目对象化跑批只复制对象存储副本，不移动、不删除、不重命名、不覆盖 NAS 原文件。",
            "本轮不读取文件正文，不写语义索引，不触发 Hermes 或 BIM 解析。",
            "执行受硬上限保护：最多 5 个项目、总 200 个文件、总 2GB、单文件 500MB、连续 3 批。"
        ));
    }

    private int countRunProjects(List<StorageObjectificationRunProject> projects, String status) {
        return (int) projects.stream().filter(row -> status.equals(row.queueStatus())).count();
    }

    private int queueSortOrder(String status) {
        if ("EXECUTABLE".equals(status)) {
            return 0;
        }
        if ("GOVERNANCE_REQUIRED".equals(status)) {
            return 1;
        }
        if ("COMPLETED".equals(status)) {
            return 2;
        }
        return 3;
    }

    private List<IntegrityRepairResult> repairIntegrityRowsConcurrently(
        Long userId,
        Long projectId,
        String targetProvider,
        IntegrityRepairConfig config,
        List<ActiveObjectRow> rows
    ) {
        if (rows.isEmpty()) {
            return List.of();
        }
        int workers = Math.min(INTEGRITY_REPAIR_MAX_WORKERS, rows.size());
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<IntegrityRepairResult>> futures = rows.stream()
                .map(row -> executor.submit(() -> repairIntegrityRow(userId, projectId, targetProvider, config, row)))
                .toList();
            List<IntegrityRepairResult> results = new ArrayList<>();
            for (Future<IntegrityRepairResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("STORAGE_INTEGRITY_REPAIR_INTERRUPTED",
                        "对象实体修复被中断。", HttpStatus.INTERNAL_SERVER_ERROR);
                } catch (ExecutionException exception) {
                    results.add(new IntegrityRepairResult(
                        false,
                        new StorageObjectificationIntegrityIssue(
                            null, null, null, null,
                            "STORAGE_INTEGRITY_REPAIR_FAILED",
                            "对象实体修复失败。",
                            false
                        )
                    ));
                }
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private IntegrityRepairResult repairIntegrityRow(
        Long userId,
        Long projectId,
        String targetProvider,
        IntegrityRepairConfig config,
        ActiveObjectRow row
    ) {
        try {
            FileAssetResponse file = assetApplicationService.getFileById(userId, row.fileId());
            if (!Objects.equals(projectId, file.projectId())) {
                throw new BusinessException("STORAGE_INTEGRITY_FILE_PROJECT_MISMATCH",
                    "文件不属于当前项目", HttpStatus.BAD_REQUEST);
            }
            validateLifecycle(file);
            validateSize(file, config.maxFileSizeBytes());
            String targetBucket = storageService.defaultBucketFor(targetProvider);
            if (!objectLocationAligned(row, targetProvider, targetBucket) && hasText(row.objectKey())) {
                StorageService.ObjectProbeResult existingTarget = storageService.probeObject(
                    targetProvider, targetBucket, row.objectKey(), row.expectedSizeBytes());
                if (existingTarget.verified()) {
                    StorageService.ObjectMirrorResult mirror = new StorageService.ObjectMirrorResult(
                        targetProvider,
                        targetBucket,
                        row.objectKey(),
                        existingTarget.etag(),
                        hasText(row.checksum()) ? row.checksum() : file.checksum(),
                        hasText(row.contentType()) ? row.contentType() : "application/octet-stream",
                        row.expectedSizeBytes(),
                        hasText(row.sourceProvider()) ? row.sourceProvider() : "NAS",
                        row.sourceUriDigest(),
                        row.sourcePathDigest(),
                        Instant.now()
                    );
                    Long objectId = upsertStorageObject(mirror, userId);
                    upsertFileObjectVersion(file, objectId, mirror, userId);
                    updateFileChecksum(file.fileId(), mirror.checksum(), userId);
                    auditMigrationFile(projectId, row.fileId(), userId,
                        "storage.integrity.repair.completed", "ADOPTED_EXISTING_OBJECT");
                    return new IntegrityRepairResult(true,
                        toIntegrityIssue(row, "REPAIRED", "对象实体已在当前 NAS 侧 MinIO 验证并重新挂接。", true));
                }
            }
            StorageService.ObjectMirrorResult mirror = storageService.mirrorNasFileToObject(file, targetProvider);
            Long objectId = upsertStorageObject(mirror, userId);
            upsertFileObjectVersion(file, objectId, mirror, userId);
            updateFileChecksum(file.fileId(), mirror.checksum(), userId);
            StorageService.ObjectProbeResult repairedProbe = storageService.probeObject(
                mirror.provider(), mirror.bucket(), mirror.objectKey(), mirror.sizeBytes());
            if (!repairedProbe.verified()) {
                throw new BusinessException("STORAGE_INTEGRITY_REPAIR_VERIFY_FAILED",
                    "修复后对象实体仍不可验证", HttpStatus.PRECONDITION_FAILED);
            }
            auditMigrationFile(projectId, row.fileId(), userId, "storage.integrity.repair.completed", "REPAIRED");
            return new IntegrityRepairResult(true,
                toIntegrityIssue(row, "REPAIRED", "对象实体已重新复制到当前 NAS 侧 MinIO。", true));
        } catch (BusinessException exception) {
            String message = safeFailureMessage(exception);
            auditMigrationFile(projectId, row.fileId(), userId, "storage.integrity.repair.failed", exception.getCode());
            return new IntegrityRepairResult(false, toIntegrityIssue(row, exception.getCode(), message, false));
        } catch (Exception exception) {
            String message = "对象实体修复失败。";
            auditMigrationFile(projectId, row.fileId(), userId, "storage.integrity.repair.failed", "STORAGE_INTEGRITY_REPAIR_FAILED");
            return new IntegrityRepairResult(false, toIntegrityIssue(row, "STORAGE_INTEGRITY_REPAIR_FAILED", message, false));
        }
    }

    private StorageObjectificationLongRunResponse executeLongRun(
        Long userId,
        Long projectId,
        StorageObjectificationLongRunRequest request,
        boolean resume
    ) {
        ensureLongRunPilot(userId, projectId);
        requireLongRunConfirmed(request);
        LongRunConfig config = normalizeLongRunConfig(request);
        if (pausedLongRuns.containsKey(projectId)) {
            throw new BusinessException("STORAGE_LONG_RUN_PAUSED",
                "105 对象化长跑已暂停；请使用继续接口从剩余可执行项推进。", HttpStatus.CONFLICT);
        }
        StorageProviderReadinessResponse readiness = storageService.minioReadiness();
        if (!Boolean.TRUE.equals(readiness.writable()) || !"READY".equals(readiness.readinessStatus())) {
            throw new BusinessException("STORAGE_LONG_RUN_OBJECT_STORE_NOT_READY",
                "NAS 侧 MinIO 尚未就绪，不能执行 105 长跑对象化。", HttpStatus.PRECONDITION_FAILED);
        }

        List<Long> createdTaskIds = new ArrayList<>();
        List<MultiProjectStorageObjectificationExecutionProject> batchResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        int processedBatchCount = 0;
        long processedFileCount = 0L;

        for (int index = 0; index < config.maxContinuousBatches(); index++) {
            if (pausedLongRuns.containsKey(projectId)) {
                warnings.add("执行过程中检测到暂停标记，已停止继续提交新批次。");
                break;
            }
            StorageObjectificationFullPlanResponse plan = fullObjectificationPlan(userId, projectId,
                new StorageObjectificationFullPlanRequest(null, null, null, null, null, "ANY",
                    config.batchFileLimit(), config.batchBytesLimit()),
                config.maxFileSizeBytes(), LONG_RUN_MAX_BATCH_BYTES);
            List<Long> fileIds = plan.nextBatchItems().stream()
                .filter(item -> item.sizeBytes() == null || item.sizeBytes() <= config.maxFileSizeBytes())
                .map(StorageObjectificationPlanSampleItem::fileId)
                .filter(Objects::nonNull)
                .toList();
            if (fileIds.isEmpty()) {
                warnings.add("当前没有可执行下一批；剩余项请查看治理清单或确认已完成。");
                break;
            }
            MultiProjectStorageObjectificationExecuteResponse result = executeLongRunBatch(
                userId, projectId, fileIds, config, request == null ? null : request.targetProvider());
            processedBatchCount++;
            processedFileCount += result.selectedFileCount();
            createdTaskIds.addAll(result.createdTaskIds());
            batchResults.addAll(result.projectResults());
            createdCount += nullToZero(result.createdCount());
            skippedCount += nullToZero(result.skippedCount());
            failedCount += nullToZero(result.failedCount());
            if (nullToZero(result.failedCount()) > 0 && !config.continueOnFailure()) {
                warnings.add("本批出现失败且 continueOnFailure=false，已停止提交后续批次。");
                break;
            }
        }

        auditLogApplicationService.record(projectId, MODULE_CODE,
            resume ? "storage.objectification.long-run.resume" : "storage.objectification.long-run.start",
            "PROJECT", String.valueOf(projectId), userId,
            Map.of(
                "projectId", projectId,
                "processedBatchCount", processedBatchCount,
                "processedFileCount", processedFileCount,
                "maxContinuousBatches", config.maxContinuousBatches(),
                "batchFileLimit", config.batchFileLimit()
            ));

        StorageObjectificationFullPlanResponse finalPlan = fullObjectificationPlan(userId, projectId,
            new StorageObjectificationFullPlanRequest(null, null, null, null, null, "ANY",
                config.batchFileLimit(), config.batchBytesLimit()),
            config.maxFileSizeBytes(), LONG_RUN_MAX_BATCH_BYTES);
        return buildLongRunResponse(
            finalPlan,
            projectId,
            config,
            processedBatchCount > 0,
            processedBatchCount,
            processedFileCount,
            createdTaskIds.size(),
            createdTaskIds,
            createdCount,
            skippedCount,
            batchResults,
            appendLongRunSafetyWarnings(warnings, config)
        );
    }

    private MultiProjectStorageObjectificationExecuteResponse executeLongRunBatch(
        Long userId,
        Long projectId,
        List<Long> fileIds,
        LongRunConfig config,
        String targetProvider
    ) {
        List<MultiProjectDryRunCandidate> candidates = queryMultiProjectCandidates(
            userId,
            List.of(projectId),
            fileIds,
            null,
            fileIds.size()
        );
        if (candidates.size() != fileIds.size()) {
            throw new BusinessException("STORAGE_LONG_RUN_FILE_SCOPE_INVALID",
                "存在不属于 105 或不可访问的长跑文件。", HttpStatus.BAD_REQUEST);
        }
        long selectedTotalBytes = 0L;
        for (MultiProjectDryRunCandidate candidate : candidates) {
            if (unreadableRisk(candidate)) {
                throw new BusinessException("STORAGE_LONG_RUN_SOURCE_REVIEW_REQUIRED",
                    "存在缺少受控存储引用的文件，不能执行真实对象化。", HttpStatus.PRECONDITION_FAILED);
            }
            if (candidate.sizeBytes() != null && candidate.sizeBytes() > config.maxFileSizeBytes()) {
                throw new BusinessException("STORAGE_LONG_RUN_FILE_TOO_LARGE",
                    "单文件超过本轮长跑大小上限。", HttpStatus.PRECONDITION_FAILED);
            }
            selectedTotalBytes += nullToZero(candidate.sizeBytes());
        }
        if (selectedTotalBytes > config.batchBytesLimit()) {
            throw new BusinessException("STORAGE_LONG_RUN_BATCH_BYTES_LIMIT_EXCEEDED",
                "本轮批次容量超过长跑配置上限。", HttpStatus.BAD_REQUEST);
        }
        StorageMigrationTaskDetailResponse detail = createTask(userId, projectId,
            new StorageMigrationTaskCreateRequest(fileIds, targetProvider),
            config.batchFileLimit(),
            config.maxFileSizeBytes());
        MultiProjectDryRunCandidate first = candidates.getFirst();
        List<String> failureReasons = new ArrayList<>();
        if (nullToZero(detail.failureCount()) > 0 && hasText(detail.message())) {
            failureReasons.add(detail.message());
        }
        List<String> warnings = appendLongRunSafetyWarnings(new ArrayList<>(), config);
        return new MultiProjectStorageObjectificationExecuteResponse(
            false,
            true,
            "105_OBJECTIFICATION_LONG_RUN",
            1,
            (long) fileIds.size(),
            selectedTotalBytes,
            config.batchBytesLimit(),
            config.batchFileLimit(),
            config.batchBytesLimit(),
            1,
            List.of(detail.taskId()),
            detail.successCount(),
            detail.skippedCount(),
            detail.failureCount(),
            failureReasons,
            warnings,
            List.of(new MultiProjectStorageObjectificationExecutionProject(
                projectId,
                first.projectCode(),
                first.projectName(),
                (long) fileIds.size(),
                selectedTotalBytes,
                detail.taskId(),
                detail.taskStatus(),
                detail.successCount(),
                detail.skippedCount(),
                detail.failureCount(),
                detail.message(),
                List.copyOf(fileIds)
            ))
        );
    }

    public MultiProjectStorageObjectificationPlanDryRunResponse dryRunMultiProjectObjectificationPlan(
        Long userId,
        MultiProjectStorageObjectificationPlanDryRunRequest request
    ) {
        ensureAnyProjectAccess(userId);
        DryRunFilters filters = normalizeDryRunFilters(new StorageObjectificationPlanDryRunRequest(
            request == null ? null : request.directoryPath(),
            request == null ? null : request.fileKinds(),
            request == null ? null : request.extensions(),
            request == null ? null : request.minSizeBytes(),
            request == null ? null : request.maxSizeBytes(),
            request == null ? null : request.checksumState(),
            request == null ? null : request.storageState(),
            request == null ? null : request.limit(),
            request == null ? null : request.maxTotalBytes()
        ));
        List<Long> scopedProjectIds = accessibleProjectIds(userId, request == null ? null : request.projectIds(),
            Boolean.TRUE.equals(request == null ? null : request.realProjectsOnly()));
        if (scopedProjectIds.isEmpty()) {
            throw new BusinessException("STORAGE_MULTI_PLAN_PROJECTS_REQUIRED",
                "没有可规划的项目范围", HttpStatus.BAD_REQUEST);
        }
        int maxFilesPerProject = normalizePositiveInt(request == null ? null : request.maxFilesPerProject(),
            DEFAULT_MAX_FILES, MAX_DRY_RUN_LIMIT, "STORAGE_MULTI_PLAN_PROJECT_FILE_LIMIT_INVALID");
        long maxBytesPerProject = normalizePositiveLong(request == null ? null : request.maxBytesPerProject(),
            Long.MAX_VALUE, "STORAGE_MULTI_PLAN_PROJECT_BYTES_LIMIT_INVALID");
        int concurrencyLimit = normalizePositiveInt(request == null ? null : request.concurrencyLimit(),
            1, 8, "STORAGE_MULTI_PLAN_CONCURRENCY_INVALID");
        Long rateLimitBytesPerMinute = request == null ? null : request.rateLimitBytesPerMinute();
        if (rateLimitBytesPerMinute != null && rateLimitBytesPerMinute < 0) {
            throw new BusinessException("STORAGE_MULTI_PLAN_RATE_LIMIT_INVALID",
                "速率限制不能小于 0", HttpStatus.BAD_REQUEST);
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectIds", scopedProjectIds)
            .addValue("limit", Math.min(filters.limit(), MAX_DRY_RUN_LIMIT));
        StringBuilder where = new StringBuilder("""
            WHERE f.project_id IN (:projectIds)
              AND f.deleted = 0
              AND p.deleted = 0
            """);
        appendDryRunFilters(where, params, filters);
        List<MultiProjectDryRunCandidate> rows = jdbcTemplate.query("""
            SELECT
                p.id AS project_id,
                p.code AS project_code,
                p.name AS project_name,
                p.asset_source,
                p.project_stage,
                f.id AS file_id,
                f.asset_uuid,
                f.original_name,
                f.file_kind,
                LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS extension,
                COALESCE(f.size_bytes, 0) AS size_bytes,
                f.checksum,
                f.storage_provider,
                CASE WHEN f.storage_uri IS NULL OR f.storage_uri = '' THEN 1 ELSE 0 END AS storage_reference_missing,
                CASE WHEN obj.file_id IS NOT NULL THEN 1 ELSE 0 END AS object_stored,
                COALESCE(mig.pending, 0) AS migration_pending,
                COALESCE(mig.failed, 0) AS migration_failed
            FROM core_projects p
            JOIN (
                SELECT DISTINCT project_id
                FROM core_user_project_roles
                WHERE user_id = :userId
                  AND deleted = 0
            ) upr ON upr.project_id = p.id
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            """ + where + "\n" + """
            ORDER BY p.id, f.id
            LIMIT :limit
            """, params.addValue("userId", userId), (rs, rowNum) -> {
            String assetSource = rs.getString("asset_source");
            String category = projectCategory(assetSource, rs.getString("project_code"), rs.getString("project_stage"));
            return new MultiProjectDryRunCandidate(
                rs.getLong("project_id"),
                rs.getString("project_code"),
                rs.getString("project_name"),
                assetSource,
                category,
                "REAL_NAS".equals(category),
                rs.getLong("file_id"),
                rs.getString("asset_uuid"),
                rs.getString("original_name"),
                rs.getString("file_kind"),
                normalizeExtension(rs.getString("extension")),
                rs.getLong("size_bytes"),
                rs.getString("checksum"),
                rs.getString("storage_provider"),
                rs.getInt("storage_reference_missing") == 1,
                rs.getInt("object_stored") == 1,
                rs.getInt("migration_pending") == 1,
                rs.getInt("migration_failed") == 1
            );
        });

        LinkedHashMap<Long, MultiProjectPlanAccumulator> projectPlans = new LinkedHashMap<>();
        long selectedFileCount = 0L;
        long selectedTotalBytes = 0L;
        long objectStoredSkipCount = 0L;
        long missingChecksumCount = 0L;
        long oversizedCount = 0L;
        long unreadableRiskCount = 0L;
        boolean totalFileCapped = false;
        boolean totalBytesCapped = false;
        boolean projectFileCapped = false;
        boolean projectBytesCapped = false;

        for (MultiProjectDryRunCandidate row : rows) {
            MultiProjectPlanAccumulator project = projectPlans.computeIfAbsent(row.projectId(),
                ignored -> new MultiProjectPlanAccumulator(row));
            String storageStatus = storageStatus(row);
            boolean missingChecksum = !hasText(row.checksum());
            boolean oversized = row.sizeBytes() >= LARGE_FILE_RISK_BYTES;
            boolean unreadableRisk = unreadableRisk(row);
            if (row.objectStored()) {
                objectStoredSkipCount++;
                project.objectStoredSkipCount++;
            }
            if (missingChecksum && !row.objectStored()) {
                missingChecksumCount++;
                project.missingChecksumCount++;
            }
            if (oversized && !row.objectStored()) {
                oversizedCount++;
                project.oversizedCount++;
            }
            if (unreadableRisk && !row.objectStored()) {
                unreadableRiskCount++;
                project.unreadableRiskCount++;
            }
            String reason = dryRunReason(row, missingChecksum, oversized, unreadableRisk);
            boolean eligible = !row.objectStored() && !unreadableRisk;
            if (eligible && selectedFileCount >= filters.limit()) {
                eligible = false;
                totalFileCapped = true;
                reason = "TOTAL_FILE_LIMIT_EXCEEDED";
            }
            if (eligible && filters.maxTotalBytes() != null
                && selectedTotalBytes + row.sizeBytes() > filters.maxTotalBytes()) {
                eligible = false;
                totalBytesCapped = true;
                reason = "TOTAL_BYTES_CAP_EXCEEDED";
            }
            if (eligible && project.selectedFileCount >= maxFilesPerProject) {
                eligible = false;
                projectFileCapped = true;
                reason = "PROJECT_FILE_LIMIT_EXCEEDED";
            }
            if (eligible && project.selectedTotalBytes + row.sizeBytes() > maxBytesPerProject) {
                eligible = false;
                projectBytesCapped = true;
                reason = "PROJECT_BYTES_CAP_EXCEEDED";
            }
            if (eligible) {
                selectedFileCount++;
                selectedTotalBytes += row.sizeBytes();
                project.selectedFileCount++;
                project.selectedTotalBytes += row.sizeBytes();
            }
            if (project.sampleItems.size() < 20) {
                project.sampleItems.add(new StorageObjectificationPlanSampleItem(
                    row.fileId(),
                    row.assetUuid(),
                    row.fileName(),
                    row.fileKind(),
                    row.extension(),
                    row.sizeBytes(),
                    missingChecksum ? "MISSING_CHECKSUM" : "HAS_CHECKSUM",
                    storageStatus,
                    reason
                ));
            }
        }

        List<String> riskMessages = new ArrayList<>();
        riskMessages.add("多项目 dry-run 仅生成计划，不复制文件、不修改 NAS、不创建迁移任务。");
        if (totalFileCapped) {
            riskMessages.add("已按总文件数上限截断计划。");
        }
        if (totalBytesCapped) {
            riskMessages.add("已按总容量上限截断计划。");
        }
        if (projectFileCapped) {
            riskMessages.add("部分项目已按单项目文件数上限截断。");
        }
        if (projectBytesCapped) {
            riskMessages.add("部分项目已按单项目容量上限截断。");
        }
        if (unreadableRiskCount > 0) {
            riskMessages.add("存在缺少受控存储引用或非 NAS 源链路的风险文件。");
        }
        if (riskMessages.size() == 1) {
            riskMessages.add("当前多项目计划未发现阻塞级风险。");
        }

        List<MultiProjectStorageObjectificationPlanProject> projectResponses = projectPlans.values().stream()
            .map(MultiProjectPlanAccumulator::toResponse)
            .toList();
        return new MultiProjectStorageObjectificationPlanDryRunResponse(
            true,
            false,
            "MULTI_PROJECT_DRY_RUN",
            scopedProjectIds.size(),
            projectResponses.size(),
            selectedFileCount,
            selectedTotalBytes,
            objectStoredSkipCount,
            missingChecksumCount,
            oversizedCount,
            unreadableRiskCount,
            selectedFileCount == 0 ? 0 : (long) Math.ceil(selectedFileCount / (double) DEFAULT_MAX_FILES),
            DEFAULT_MAX_FILES,
            DEFAULT_MAX_FILE_SIZE_BYTES,
            filters.maxTotalBytes(),
            maxFilesPerProject,
            maxBytesPerProject == Long.MAX_VALUE ? null : maxBytesPerProject,
            concurrencyLimit,
            rateLimitBytesPerMinute,
            riskMessages,
            projectResponses
        );
    }

    public MultiProjectStorageObjectificationExecuteResponse executeMultiProjectObjectificationPlan(
        Long userId,
        MultiProjectStorageObjectificationExecuteRequest request
    ) {
        ensureAnyProjectAccess(userId);
        if (!Boolean.TRUE.equals(request == null ? null : request.confirmed())) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_CONFIRM_REQUIRED",
                "受控多项目对象化执行必须 confirmed=true", HttpStatus.BAD_REQUEST);
        }
        StorageProviderReadinessResponse readiness = storageService.minioReadiness();
        if (!Boolean.TRUE.equals(readiness.writable()) || !"READY".equals(readiness.readinessStatus())) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_OBJECT_STORE_NOT_READY",
                "NAS 侧 MinIO 尚未就绪，不能执行真实对象化。", HttpStatus.PRECONDITION_FAILED);
        }

        List<Long> requestedProjectIds = normalizeExecutionProjectIds(request == null ? null : request.projectIds());
        List<Long> scopedProjectIds = accessibleProjectIds(userId, requestedProjectIds, true);
        if (scopedProjectIds.size() != requestedProjectIds.size()) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_SCOPE_INVALID",
                "只能对当前账号可访问的真实 NAS 项目执行小批对象化。", HttpStatus.BAD_REQUEST);
        }
        List<Long> explicitFileIds = normalizeExecutionFileIds(request == null ? null : request.fileIds());
        int limit = normalizeExecutionLimit(request == null ? null : request.limit());
        int maxFilesPerProject = normalizeExecutionMaxFilesPerProject(request == null ? null : request.maxFilesPerProject());
        long maxBytesPerProject = normalizeExecutionMaxBytesPerProject(request == null ? null : request.maxBytesPerProject());
        long maxTotalBytes = normalizeExecutionMaxTotalBytes(request == null ? null : request.maxTotalBytes());
        String targetProvider = normalizeTargetProvider(request == null ? null : request.targetProvider());

        List<MultiProjectDryRunCandidate> candidates = queryMultiProjectCandidates(
            userId,
            scopedProjectIds,
            explicitFileIds,
            null,
            explicitFileIds.size()
        );
        if (candidates.size() != explicitFileIds.size()) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_FILE_SCOPE_INVALID",
                "存在不属于本次真实项目范围或不可访问的文件。", HttpStatus.BAD_REQUEST);
        }

        LinkedHashMap<Long, ExecutionProjectSelection> selections = new LinkedHashMap<>();
        long selectedFileCount = 0L;
        long selectedTotalBytes = 0L;
        for (MultiProjectDryRunCandidate candidate : candidates) {
            if (!Boolean.TRUE.equals(candidate.realNasProject())) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_REAL_PROJECT_REQUIRED",
                    "受控执行只允许真实 NAS 项目。", HttpStatus.BAD_REQUEST);
            }
            boolean alreadyStored = candidate.objectStored();
            if (!alreadyStored && unreadableRisk(candidate)) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_SOURCE_REVIEW_REQUIRED",
                    "存在缺少受控存储引用的文件，不能执行真实对象化。", HttpStatus.PRECONDITION_FAILED);
            }
            if (!alreadyStored && candidate.sizeBytes() > DEFAULT_MAX_FILE_SIZE_BYTES) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_FILE_TOO_LARGE",
                    "单文件超过本轮小批对象化大小限制。", HttpStatus.PRECONDITION_FAILED);
            }
            ExecutionProjectSelection selection = selections.computeIfAbsent(candidate.projectId(),
                ignored -> new ExecutionProjectSelection(candidate));
            if (selection.fileIds.size() >= maxFilesPerProject) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_FILE_LIMIT_EXCEEDED",
                    "单项目文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
            }
            if (selection.selectedTotalBytes + candidate.sizeBytes() > maxBytesPerProject) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_BYTES_LIMIT_EXCEEDED",
                    "单项目容量超过本轮执行上限。", HttpStatus.BAD_REQUEST);
            }
            if (selectedFileCount + 1 > limit || selectedFileCount + 1 > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_TOTAL_FILE_LIMIT_EXCEEDED",
                    "总文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
            }
            if (selectedTotalBytes + candidate.sizeBytes() > maxTotalBytes) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_TOTAL_BYTES_LIMIT_EXCEEDED",
                    "总容量超过本轮执行上限。", HttpStatus.BAD_REQUEST);
            }
            selection.fileIds.add(candidate.fileId());
            selection.selectedTotalBytes += candidate.sizeBytes();
            selectedFileCount++;
            selectedTotalBytes += candidate.sizeBytes();
        }
        if (selectedFileCount == 0) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_FILES_REQUIRED",
                "请先从 dry-run 计划中选择要执行的小批文件。", HttpStatus.BAD_REQUEST);
        }

        List<Long> createdTaskIds = new ArrayList<>();
        List<MultiProjectStorageObjectificationExecutionProject> projectResults = new ArrayList<>();
        List<String> failureReasons = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (ExecutionProjectSelection selection : selections.values()) {
            StorageMigrationTaskDetailResponse detail = createTask(userId, selection.projectId,
                new StorageMigrationTaskCreateRequest(selection.fileIds, targetProvider));
            createdTaskIds.add(detail.taskId());
            createdCount += nullToZero(detail.successCount());
            skippedCount += nullToZero(detail.skippedCount());
            failedCount += nullToZero(detail.failureCount());
            if (nullToZero(detail.failureCount()) > 0 && hasText(detail.message())) {
                failureReasons.add(detail.message());
            }
            projectResults.add(new MultiProjectStorageObjectificationExecutionProject(
                selection.projectId,
                selection.projectCode,
                selection.projectName,
                (long) selection.fileIds.size(),
                selection.selectedTotalBytes,
                detail.taskId(),
                detail.taskStatus(),
                detail.successCount(),
                detail.skippedCount(),
                detail.failureCount(),
                detail.message(),
                List.copyOf(selection.fileIds)
            ));
            auditLogApplicationService.record(selection.projectId, MODULE_CODE, "storage.objectification.multi.execute",
                "STORAGE_MIGRATION_TASK", String.valueOf(detail.taskId()), userId,
                Map.of(
                    "taskSource", CONTROLLED_EXPANSION_TASK_SOURCE,
                    "taskId", detail.taskId(),
                    "fileCount", selection.fileIds.size(),
                    "selectedTotalBytes", selection.selectedTotalBytes,
                    "targetProvider", targetProvider
                ));
        }

        List<String> warnings = new ArrayList<>();
        warnings.add("本次为受控小批对象化执行：只复制文件副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件。");
        warnings.add("未读取文件正文，未写语义索引，未触发 Hermes。");
        if (skippedCount > 0) {
            warnings.add("重复执行中已有对象版本的文件会按幂等策略跳过。");
        }
        return new MultiProjectStorageObjectificationExecuteResponse(
            false,
            true,
            CONTROLLED_EXPANSION_TASK_SOURCE,
            selections.size(),
            selectedFileCount,
            selectedTotalBytes,
            maxTotalBytes,
            maxFilesPerProject,
            maxBytesPerProject,
            createdTaskIds.size(),
            createdTaskIds,
            createdCount,
            skippedCount,
            failedCount,
            failureReasons,
            warnings,
            projectResults
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
        String targetProvider,
        long maxFileSizeBytes
    ) {
        markRowRunning(rowId);
        try {
            FileAssetResponse file = assetApplicationService.getFileById(userId, fileId);
            if (!Objects.equals(projectId, file.projectId())) {
                throw new BusinessException("STORAGE_MIGRATION_FILE_PROJECT_MISMATCH",
                    "文件不属于当前项目", HttpStatus.BAD_REQUEST);
            }
            validateLifecycle(file);
            validateSize(file, maxFileSizeBytes);
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
            jdbcTemplate.update("""
                UPDATE data_file_object_versions
                SET storage_state = 'OBJECT_STORED',
                    migration_status = 'COMPLETED',
                    checksum = :checksum,
                    content_type = :contentType,
                    size_bytes = :sizeBytes,
                    last_verified_at = :verifiedAt,
                    updated_by = :userId
                WHERE file_id = :fileId
                  AND storage_object_id = :objectId
                  AND active = 1
                  AND deleted = 0
                """, new MapSqlParameterSource()
                .addValue("fileId", file.fileId())
                .addValue("objectId", objectId)
                .addValue("checksum", mirror.checksum())
                .addValue("contentType", mirror.contentType())
                .addValue("sizeBytes", mirror.sizeBytes())
                .addValue("verifiedAt", Timestamp.from(mirror.verifiedAt()))
                .addValue("userId", userId));
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

    private void ensureAnyProjectAccess(Long userId) {
        if (userId == null) {
            throw new BusinessException("PROJECT_ACCESS_DENIED", "当前账号无项目权限", HttpStatus.FORBIDDEN);
        }
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles
            WHERE user_id = :userId
              AND deleted = 0
            """, new MapSqlParameterSource("userId", userId), Integer.class);
        if (count == null || count == 0) {
            throw new BusinessException("PROJECT_ACCESS_DENIED", "当前账号无项目权限", HttpStatus.FORBIDDEN);
        }
    }

    private DryRunFilters normalizeDryRunFilters(StorageObjectificationPlanDryRunRequest request) {
        String directoryPath = normalizeDirectoryPath(request == null ? null : request.directoryPath());
        List<String> fileKinds = normalizeUpperList(request == null ? null : request.fileKinds());
        List<String> extensions = normalizeExtensions(request == null ? null : request.extensions());
        Long minSizeBytes = request == null ? null : request.minSizeBytes();
        Long maxSizeBytes = request == null ? null : request.maxSizeBytes();
        if (minSizeBytes != null && minSizeBytes < 0) {
            throw new BusinessException("STORAGE_PLAN_SIZE_FILTER_INVALID", "最小文件大小不能小于 0", HttpStatus.BAD_REQUEST);
        }
        if (maxSizeBytes != null && maxSizeBytes < 0) {
            throw new BusinessException("STORAGE_PLAN_SIZE_FILTER_INVALID", "最大文件大小不能小于 0", HttpStatus.BAD_REQUEST);
        }
        if (minSizeBytes != null && maxSizeBytes != null && minSizeBytes > maxSizeBytes) {
            throw new BusinessException("STORAGE_PLAN_SIZE_FILTER_INVALID", "最小文件大小不能大于最大文件大小", HttpStatus.BAD_REQUEST);
        }
        String checksumState = normalizeEnum(
            request == null ? null : request.checksumState(),
            List.of("ANY", "HAS_CHECKSUM", "MISSING_CHECKSUM"),
            "ANY",
            "STORAGE_PLAN_CHECKSUM_STATE_INVALID"
        );
        String storageState = normalizeEnum(
            request == null ? null : request.storageState(),
            List.of("ANY", "NAS_ONLY", "MIGRATION_FAILED"),
            "ANY",
            "STORAGE_PLAN_STORAGE_STATE_INVALID"
        );
        Integer limit = request == null ? null : request.limit();
        if (limit == null || limit <= 0) {
            limit = DEFAULT_DRY_RUN_LIMIT;
        }
        limit = Math.min(limit, MAX_DRY_RUN_LIMIT);
        Long maxTotalBytes = request == null ? null : request.maxTotalBytes();
        if (maxTotalBytes != null && maxTotalBytes < 0) {
            throw new BusinessException("STORAGE_PLAN_TOTAL_BYTES_INVALID", "总容量上限不能小于 0", HttpStatus.BAD_REQUEST);
        }
        return new DryRunFilters(
            directoryPath,
            fileKinds,
            extensions,
            minSizeBytes,
            maxSizeBytes,
            checksumState,
            storageState,
            limit,
            maxTotalBytes
        );
    }

    private String normalizeDirectoryPath(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace("\\", "/");
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("/volumes")
            || lower.contains("/users")
            || lower.startsWith("smb://")
            || lower.startsWith("nas://")
            || lower.contains("storage_uri")) {
            throw new BusinessException("STORAGE_PLAN_DIRECTORY_FILTER_INVALID",
                "目录筛选只能使用平台逻辑目录，不能使用真实存储路径", HttpStatus.BAD_REQUEST);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private List<String> normalizeUpperList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (hasText(value)) {
                normalized.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> normalizeExtensions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            String item = value.trim().toLowerCase(Locale.ROOT);
            while (item.startsWith(".")) {
                item = item.substring(1);
            }
            if (!item.isBlank()) {
                normalized.add(item);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeEnum(String value, List<String> allowed, String fallback, String errorCode) {
        if (!hasText(value)) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(errorCode, "筛选条件不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String riskLevel(
        long totalFiles,
        BigDecimal objectCoverage,
        BigDecimal checksumCoverage,
        long largeFileCount,
        long failedFiles,
        long unreadablePathFiles
    ) {
        if (totalFiles == 0) {
            return "LOW";
        }
        if (failedFiles > 0 || largeFileCount > 0 || unreadablePathFiles > 0
            || objectCoverage.compareTo(BigDecimal.valueOf(20)) < 0
            || checksumCoverage.compareTo(BigDecimal.valueOf(60)) < 0) {
            return "HIGH";
        }
        if (objectCoverage.compareTo(BigDecimal.valueOf(80)) < 0
            || checksumCoverage.compareTo(BigDecimal.valueOf(90)) < 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<String> inventoryRiskMessages(
        long totalFiles,
        BigDecimal objectCoverage,
        BigDecimal checksumCoverage,
        long largeFileCount,
        long failedFiles,
        long unreadablePathFiles
    ) {
        List<String> messages = new ArrayList<>();
        if (totalFiles == 0) {
            messages.add("当前项目没有登记文件。");
            return messages;
        }
        if (objectCoverage.compareTo(BigDecimal.valueOf(80)) < 0) {
            messages.add("对象化覆盖率未达到 80%，后续需要分批 dry-run。");
        }
        if (checksumCoverage.compareTo(BigDecimal.valueOf(90)) < 0) {
            messages.add("checksum 覆盖率不足，真实对象化前需关注校验补齐。");
        }
        if (largeFileCount > 0) {
            messages.add("存在大文件，后续迁移需要拆批和性能评估。");
        }
        if (failedFiles > 0) {
            messages.add("存在历史迁移失败记录，需先查看失败原因。");
        }
        if (unreadablePathFiles > 0) {
            messages.add("存在缺少受控存储引用或非 NAS 源链路的文件，需人工核查。");
        }
        if (messages.isEmpty()) {
            messages.add("当前未发现阻塞级风险。");
        }
        return messages;
    }

    private void appendDryRunFilters(StringBuilder where, MapSqlParameterSource params, DryRunFilters filters) {
        if (filters.directoryPath() != null) {
            where.append("\n AND (f.logical_path = :directoryPath OR f.logical_path LIKE :directoryPrefix)");
            params.addValue("directoryPath", filters.directoryPath());
            params.addValue("directoryPrefix", filters.directoryPath() + "/%");
        }
        if (!filters.fileKinds().isEmpty()) {
            where.append("\n AND f.file_kind IN (:fileKinds)");
            params.addValue("fileKinds", filters.fileKinds());
        }
        if (!filters.extensions().isEmpty()) {
            where.append("\n AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN (:extensions)");
            params.addValue("extensions", filters.extensions());
        }
        if (filters.minSizeBytes() != null) {
            where.append("\n AND COALESCE(f.size_bytes, 0) >= :minSizeBytes");
            params.addValue("minSizeBytes", filters.minSizeBytes());
        }
        if (filters.maxSizeBytes() != null) {
            where.append("\n AND COALESCE(f.size_bytes, 0) <= :maxSizeBytes");
            params.addValue("maxSizeBytes", filters.maxSizeBytes());
        }
        if ("HAS_CHECKSUM".equals(filters.checksumState())) {
            where.append("\n AND f.checksum IS NOT NULL AND f.checksum <> ''");
        } else if ("MISSING_CHECKSUM".equals(filters.checksumState())) {
            where.append("\n AND (f.checksum IS NULL OR f.checksum = '')");
        }
        if ("NAS_ONLY".equals(filters.storageState())) {
            where.append("\n AND obj.file_id IS NULL AND COALESCE(mig.failed, 0) = 0");
        } else if ("MIGRATION_FAILED".equals(filters.storageState())) {
            where.append("\n AND obj.file_id IS NULL AND COALESCE(mig.failed, 0) = 1");
        }
    }

    private List<MultiProjectDryRunCandidate> queryMultiProjectCandidates(
        Long userId,
        List<Long> projectIds,
        List<Long> fileIds,
        DryRunFilters filters,
        int limit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectIds", projectIds)
            .addValue("limit", Math.min(Math.max(limit, 1), MAX_DRY_RUN_LIMIT));
        StringBuilder where = new StringBuilder("""
            WHERE f.project_id IN (:projectIds)
              AND f.deleted = 0
              AND p.deleted = 0
            """);
        if (fileIds != null && !fileIds.isEmpty()) {
            where.append("\n AND f.id IN (:fileIds)");
            params.addValue("fileIds", fileIds);
        } else if (filters != null) {
            appendDryRunFilters(where, params, filters);
        }
        return jdbcTemplate.query("""
            SELECT
                p.id AS project_id,
                p.code AS project_code,
                p.name AS project_name,
                p.asset_source,
                p.project_stage,
                f.id AS file_id,
                f.asset_uuid,
                f.original_name,
                f.file_kind,
                LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS extension,
                COALESCE(f.size_bytes, 0) AS size_bytes,
                f.checksum,
                f.storage_provider,
                CASE WHEN f.storage_uri IS NULL OR f.storage_uri = '' THEN 1 ELSE 0 END AS storage_reference_missing,
                CASE WHEN obj.file_id IS NOT NULL THEN 1 ELSE 0 END AS object_stored,
                COALESCE(mig.pending, 0) AS migration_pending,
                COALESCE(mig.failed, 0) AS migration_failed
            FROM core_projects p
            JOIN (
                SELECT DISTINCT project_id
                FROM core_user_project_roles
                WHERE user_id = :userId
                  AND deleted = 0
            ) upr ON upr.project_id = p.id
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            """ + where + "\n" + """
            ORDER BY p.id, f.id
            LIMIT :limit
            """, params, (rs, rowNum) -> {
            String assetSource = rs.getString("asset_source");
            String category = projectCategory(assetSource, rs.getString("project_code"), rs.getString("project_stage"));
            return new MultiProjectDryRunCandidate(
                rs.getLong("project_id"),
                rs.getString("project_code"),
                rs.getString("project_name"),
                assetSource,
                category,
                "REAL_NAS".equals(category),
                rs.getLong("file_id"),
                rs.getString("asset_uuid"),
                rs.getString("original_name"),
                rs.getString("file_kind"),
                normalizeExtension(rs.getString("extension")),
                rs.getLong("size_bytes"),
                rs.getString("checksum"),
                rs.getString("storage_provider"),
                rs.getInt("storage_reference_missing") == 1,
                rs.getInt("object_stored") == 1,
                rs.getInt("migration_pending") == 1,
                rs.getInt("migration_failed") == 1
            );
        });
    }

    private List<Long> accessibleProjectIds(Long userId, List<Long> requestedProjectIds, boolean realProjectsOnly) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        String filter = "";
        List<Long> normalized = normalizeOptionalProjectIds(requestedProjectIds);
        if (!normalized.isEmpty()) {
            filter = "\n AND p.id IN (:projectIds)";
            params.addValue("projectIds", normalized);
        }
        List<ProjectScopeRow> rows = jdbcTemplate.query("""
            SELECT p.id, p.code, p.project_stage, p.asset_source
            FROM core_projects p
            JOIN core_user_project_roles upr ON upr.project_id = p.id AND upr.deleted = 0
            WHERE upr.user_id = :userId
              AND p.deleted = 0
            """ + filter + """
            GROUP BY p.id, p.code, p.project_stage, p.asset_source
            ORDER BY p.id
            LIMIT 50
            """, params, (rs, rowNum) -> new ProjectScopeRow(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("project_stage"),
            rs.getString("asset_source")
        ));
        return rows.stream()
            .filter(row -> !realProjectsOnly || "REAL_NAS".equals(projectCategory(row.assetSource(), row.code(), row.projectStage())))
            .map(ProjectScopeRow::projectId)
            .toList();
    }

    private int normalizeIntegritySampleLimit(Integer sampleLimit) {
        if (sampleLimit == null) {
            return INTEGRITY_DEFAULT_SAMPLE_LIMIT;
        }
        return Math.min(Math.max(sampleLimit, 1), INTEGRITY_MAX_SAMPLE_LIMIT);
    }

    private IntegrityRepairConfig normalizeIntegrityRepairConfig(StorageObjectificationIntegrityRepairRequest request) {
        int batchFileLimit = request == null || request.batchFileLimit() == null
            ? INTEGRITY_REPAIR_DEFAULT_BATCH_FILE_LIMIT
            : request.batchFileLimit();
        if (batchFileLimit <= 0 || batchFileLimit > INTEGRITY_REPAIR_MAX_BATCH_FILE_LIMIT) {
            throw new BusinessException("STORAGE_INTEGRITY_REPAIR_FILE_LIMIT_EXCEEDED",
                "对象实体修复单批文件数超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        long batchBytesLimit = request == null || request.batchBytesLimit() == null
            ? INTEGRITY_REPAIR_DEFAULT_BATCH_BYTES
            : request.batchBytesLimit();
        if (batchBytesLimit <= 0 || batchBytesLimit > INTEGRITY_REPAIR_MAX_BATCH_BYTES) {
            throw new BusinessException("STORAGE_INTEGRITY_REPAIR_BYTES_LIMIT_EXCEEDED",
                "对象实体修复单批容量超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        long maxFileSizeBytes = request == null || request.maxFileSizeBytes() == null
            ? LONG_RUN_MAX_FILE_SIZE_BYTES
            : request.maxFileSizeBytes();
        if (maxFileSizeBytes <= 0 || maxFileSizeBytes > LONG_RUN_MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("STORAGE_INTEGRITY_REPAIR_SIZE_LIMIT_EXCEEDED",
                "对象实体修复单文件大小超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        return new IntegrityRepairConfig(batchFileLimit, batchBytesLimit, maxFileSizeBytes);
    }

    private long countActiveObjectStored(Long projectId) {
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources f
            JOIN data_file_object_versions fov ON fov.file_id = f.id
              AND fov.active = 1
              AND fov.deleted = 0
              AND fov.storage_state = 'OBJECT_STORED'
            JOIN data_storage_objects so ON so.id = fov.storage_object_id
              AND so.deleted = 0
            WHERE f.project_id = :projectId
              AND f.deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Long.class);
        return count == null ? 0L : count;
    }

    private List<ActiveObjectRow> queryActiveObjectRows(
        Long projectId,
        int limit,
        String targetProvider,
        String targetBucket
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("limit", limit)
            .addValue("targetProvider", targetProvider)
            .addValue("targetBucket", targetBucket);
        return jdbcTemplate.query("""
            SELECT
                f.id AS file_id,
                f.asset_uuid,
                f.original_name,
                f.file_kind,
                COALESCE(f.size_bytes, 0) AS file_size_bytes,
                fov.storage_object_id,
                fov.storage_state,
                so.provider,
                so.bucket,
                so.object_key,
                COALESCE(fov.checksum, so.checksum, f.checksum) AS checksum,
                COALESCE(fov.content_type, so.content_type, 'application/octet-stream') AS content_type,
                COALESCE(so.source_provider, 'NAS') AS source_provider,
                so.source_uri_digest,
                so.source_path_digest,
                COALESCE(fov.size_bytes, so.size_bytes, f.size_bytes, 0) AS expected_size_bytes
            FROM data_file_resources f
            JOIN data_file_object_versions fov ON fov.file_id = f.id
              AND fov.active = 1
              AND fov.deleted = 0
              AND fov.storage_state IN ('OBJECT_STORED', 'MIGRATION_FAILED')
            JOIN data_storage_objects so ON so.id = fov.storage_object_id
              AND so.deleted = 0
            WHERE f.project_id = :projectId
              AND f.deleted = 0
            ORDER BY
              CASE
                WHEN fov.storage_state <> 'OBJECT_STORED' THEN -1
                WHEN :targetProvider IS NULL THEN 0
                WHEN so.provider <> :targetProvider OR COALESCE(so.bucket, '') <> COALESCE(:targetBucket, '') THEN 0
                ELSE 1
              END,
              COALESCE(f.size_bytes, 0) ASC,
              f.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> new ActiveObjectRow(
            rs.getLong("file_id"),
            rs.getString("asset_uuid"),
            rs.getString("original_name"),
            rs.getString("file_kind"),
            rs.getLong("file_size_bytes"),
            rs.getLong("storage_object_id"),
            rs.getString("storage_state"),
            rs.getString("provider"),
            rs.getString("bucket"),
            rs.getString("object_key"),
            rs.getString("checksum"),
            rs.getString("content_type"),
            rs.getString("source_provider"),
            rs.getString("source_uri_digest"),
            rs.getString("source_path_digest"),
            rs.getLong("expected_size_bytes")
        ));
    }

    private IntegrityScan scanIntegrityRows(
        List<ActiveObjectRow> rows,
        String targetProvider,
        String targetBucket,
        int issueLimit
    ) {
        long verified = 0L;
        long missing = 0L;
        long unreadable = 0L;
        long sizeMismatch = 0L;
        long issueCount = 0L;
        String latestFailureReason = null;
        List<StorageObjectificationIntegrityIssue> issues = new ArrayList<>();
        for (ActiveObjectRow row : rows) {
            if (!"OBJECT_STORED".equals(row.storageState())) {
                issueCount++;
                latestFailureReason = "历史对象版本处于失败状态，需要重新对齐到当前 NAS 侧 MinIO。";
                if (!objectLocationAligned(row, targetProvider, targetBucket)) {
                    missing++;
                    unreadable++;
                }
                if (issues.size() < issueLimit) {
                    issues.add(toIntegrityIssue(row, "OBJECT_VERSION_REPAIR_REQUIRED", latestFailureReason, true));
                }
                continue;
            }
            if (!objectLocationAligned(row, targetProvider, targetBucket)) {
                missing++;
                unreadable++;
                issueCount++;
                latestFailureReason = "历史对象版本尚未对齐到当前 NAS 侧 MinIO。";
                if (issues.size() < issueLimit) {
                    issues.add(toIntegrityIssue(row, "OBJECT_LOCATION_NOT_ALIGNED", latestFailureReason, true));
                }
                continue;
            }
            StorageService.ObjectProbeResult probe = storageService.probeObject(
                row.provider(), row.bucket(), row.objectKey(), row.expectedSizeBytes());
            if (probe.verified()) {
                verified++;
                continue;
            }
            String issueCode;
            String issueMessage;
            if ("SIZE_MISMATCH".equals(probe.failureReason())) {
                sizeMismatch++;
                issueCode = "SIZE_MISMATCH";
                issueMessage = "对象实体大小与平台登记不一致。";
            } else {
                if (probe.missing()) {
                    missing++;
                }
                if (!probe.readable()) {
                    unreadable++;
                }
                issueCode = "OBJECT_NOT_READABLE";
                issueMessage = "对象实体在当前 NAS 侧 MinIO 不存在或不可读取。";
            }
            issueCount++;
            latestFailureReason = issueMessage;
            if (issues.size() < issueLimit) {
                issues.add(toIntegrityIssue(row, issueCode, issueMessage, true));
            }
        }
        return new IntegrityScan(verified, missing, unreadable, sizeMismatch, issueCount, latestFailureReason, issues);
    }

    private boolean objectLocationAligned(ActiveObjectRow row, String targetProvider, String targetBucket) {
        return Objects.equals(row.provider(), targetProvider) && Objects.equals(row.bucket(), targetBucket);
    }

    private StorageObjectificationIntegrityIssue toIntegrityIssue(
        ActiveObjectRow row,
        String issueCode,
        String issueMessage,
        boolean repairable
    ) {
        return new StorageObjectificationIntegrityIssue(
            row.fileId(),
            row.assetUuid(),
            row.fileName(),
            row.fileKind(),
            issueCode,
            issueMessage,
            repairable
        );
    }

    private void markActiveObjectVersionRepairFailed(Long fileId, Long userId) {
        jdbcTemplate.update("""
            UPDATE data_file_object_versions
            SET storage_state = 'MIGRATION_FAILED',
                migration_status = 'FAILED',
                updated_by = :userId
            WHERE file_id = :fileId
              AND active = 1
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", fileId)
            .addValue("userId", userId));
    }

    private void ensureLongRunPilot(Long userId, Long projectId) {
        ensureProjectAccess(userId, projectId);
        if (!Objects.equals(projectId, LONG_RUN_PILOT_PROJECT_ID)) {
            throw new BusinessException("STORAGE_LONG_RUN_PROJECT_NOT_ALLOWED",
                "当前长跑控制仅开放 105 试点项目。", HttpStatus.BAD_REQUEST);
        }
    }

    private void requireLongRunConfirmed(StorageObjectificationLongRunRequest request) {
        if (!Boolean.TRUE.equals(request == null ? null : request.confirmed())) {
            throw new BusinessException("STORAGE_LONG_RUN_CONFIRM_REQUIRED",
                "105 对象化长跑执行必须 confirmed=true。", HttpStatus.BAD_REQUEST);
        }
    }

    private LongRunConfig normalizeLongRunConfig(StorageObjectificationLongRunRequest request) {
        int batchFileLimit = request == null || request.batchFileLimit() == null
            ? LONG_RUN_DEFAULT_BATCH_FILE_LIMIT
            : request.batchFileLimit();
        if (batchFileLimit <= 0 || batchFileLimit > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
            throw new BusinessException("STORAGE_LONG_RUN_BATCH_FILE_LIMIT_EXCEEDED",
                "105 长跑单批文件数超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        long batchBytesLimit = request == null || request.batchBytesLimit() == null
            ? FULL_PLAN_DEFAULT_BATCH_BYTES
            : request.batchBytesLimit();
        if (batchBytesLimit <= 0 || batchBytesLimit > LONG_RUN_MAX_BATCH_BYTES) {
            throw new BusinessException("STORAGE_LONG_RUN_BATCH_BYTES_LIMIT_EXCEEDED",
                "105 长跑单批容量超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        long maxFileSizeBytes = request == null || request.maxFileSizeBytes() == null
            ? DEFAULT_MAX_FILE_SIZE_BYTES
            : request.maxFileSizeBytes();
        if (maxFileSizeBytes <= 0 || maxFileSizeBytes > LONG_RUN_MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("STORAGE_LONG_RUN_FILE_SIZE_LIMIT_EXCEEDED",
                "105 长跑单文件大小超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        int maxContinuousBatches = request == null || request.maxContinuousBatches() == null
            ? LONG_RUN_DEFAULT_CONTINUOUS_BATCHES
            : request.maxContinuousBatches();
        if (maxContinuousBatches <= 0 || maxContinuousBatches > LONG_RUN_MAX_CONTINUOUS_BATCHES) {
            throw new BusinessException("STORAGE_LONG_RUN_CONTINUOUS_BATCH_LIMIT_EXCEEDED",
                "105 长跑连续批次数超过后端硬上限。", HttpStatus.BAD_REQUEST);
        }
        boolean continueOnFailure = request == null || request.continueOnFailure() == null
            || Boolean.TRUE.equals(request.continueOnFailure());
        return new LongRunConfig(
            batchFileLimit,
            batchBytesLimit,
            maxFileSizeBytes,
            maxContinuousBatches,
            continueOnFailure
        );
    }

    private StorageObjectificationLongRunResponse buildLongRunResponse(
        StorageObjectificationFullPlanResponse plan,
        Long projectId,
        LongRunConfig config,
        boolean executionStarted,
        int processedBatchCount,
        long processedFileCount,
        int createdTaskCount,
        List<Long> createdTaskIds,
        int createdCount,
        int skippedThisRun,
        List<MultiProjectStorageObjectificationExecutionProject> batchResults,
        List<String> warnings
    ) {
        boolean paused = pausedLongRuns.containsKey(projectId);
        long governanceItemCount = countGovernanceItems(projectId, config.maxFileSizeBytes());
        List<StorageObjectificationFailureSummary> governanceReasons =
            governanceSummaries(projectId, config.maxFileSizeBytes());
        String runState = deriveLongRunState(plan, paused, governanceItemCount);
        String lastFailureReason = latestFailureMessage(projectId);
        return new StorageObjectificationLongRunResponse(
            true,
            executionStarted,
            plan.projectId(),
            plan.projectCode(),
            plan.projectName(),
            runState,
            paused,
            plan.totalFileCount(),
            plan.totalBytes(),
            plan.objectStoredCount(),
            plan.objectStoredBytes(),
            plan.nasOnlyCount(),
            plan.nasOnlyBytes(),
            plan.migrationFailedCount(),
            plan.skippedCount(),
            plan.checksumCoveredFiles(),
            plan.checksumCoverageRate(),
            plan.objectificationCoverageRate(),
            plan.eligibleRemainingCount(),
            plan.eligibleRemainingBytes(),
            governanceItemCount,
            config.batchFileLimit(),
            config.batchBytesLimit(),
            config.maxFileSizeBytes(),
            config.maxContinuousBatches(),
            config.continueOnFailure(),
            processedBatchCount,
            processedFileCount,
            createdTaskCount,
            List.copyOf(createdTaskIds),
            createdCount,
            skippedThisRun,
            batchResults.stream().mapToInt(row -> nullToZero(row.failureCount())).sum(),
            plan.latestTaskUpdatedAt(),
            lastFailureReason,
            plan.latestTaskId(),
            plan.latestTaskStatus(),
            plan.latestTaskSuccessCount(),
            plan.latestTaskSkippedCount(),
            plan.latestTaskFailureCount(),
            plan.failureReasons(),
            governanceReasons,
            warnings,
            plan.nextBatchSuggestions(),
            plan.governanceItems(),
            List.copyOf(batchResults)
        );
    }

    private String deriveLongRunState(
        StorageObjectificationFullPlanResponse plan,
        boolean paused,
        long governanceItemCount
    ) {
        if (paused) {
            return "PAUSED";
        }
        if ("RUNNING".equals(plan.latestTaskStatus())) {
            return "RUNNING";
        }
        if (nullToZero(plan.totalFileCount()) == 0) {
            return "IDLE";
        }
        if (nullToZero(plan.nasOnlyCount()) == 0) {
            return "COMPLETED";
        }
        if ("FAILED".equals(plan.latestTaskStatus()) && nullToZero(plan.eligibleRemainingCount()) == 0) {
            return "FAILED";
        }
        if (nullToZero(plan.eligibleRemainingCount()) == 0
            && (nullToZero(plan.migrationFailedCount()) > 0 || governanceItemCount > 0)) {
            return "PARTIAL_WITH_FAILURES";
        }
        return "IDLE";
    }

    private List<String> appendLongRunSafetyWarnings(List<String> warnings, LongRunConfig config) {
        List<String> result = new ArrayList<>(warnings);
        result.add("本次长跑按受控批次同步推进，不启动无边界后台任务。");
        result.add("NAS 原文件保留，不移动、不删除、不改名、不覆盖；不会读取正文或写语义索引。");
        result.add("后端硬上限：单批最多 " + CONTROLLED_EXPANSION_MAX_FILES_TOTAL
            + " 个文件，单批最多 " + LONG_RUN_MAX_BATCH_BYTES + " bytes，单文件最多 "
            + LONG_RUN_MAX_FILE_SIZE_BYTES + " bytes，连续最多 " + LONG_RUN_MAX_CONTINUOUS_BATCHES + " 批。");
        result.add("当前请求：单批 " + config.batchFileLimit() + " 个 / "
            + config.batchBytesLimit() + " bytes，连续 " + config.maxContinuousBatches() + " 批。");
        return result;
    }

    private long countGovernanceItems(Long projectId, long maxFileSizeBytes) {
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources f
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) obj ON obj.file_id = f.id
            LEFT JOIN (
                SELECT file_id,
                       MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                       MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                FROM data_object_migration_tasks
                WHERE deleted = 0
                GROUP BY file_id
            ) mig ON mig.file_id = f.id
            WHERE f.project_id = :projectId
              AND f.deleted = 0
              AND obj.file_id IS NULL
              AND (
                  COALESCE(mig.pending, 0) = 1
                  OR COALESCE(mig.failed, 0) = 1
                  OR f.storage_uri IS NULL
                  OR f.storage_uri = ''
                  OR (UPPER(COALESCE(f.storage_provider, '')) NOT IN ('NAS', 'METADATA')
                      AND f.storage_uri NOT LIKE 'nas://%'
                      AND f.storage_uri NOT LIKE '/%')
                  OR COALESCE(f.size_bytes, 0) > :maxFileSizeBytes
              )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("maxFileSizeBytes", maxFileSizeBytes), Long.class);
        return count == null ? 0L : count;
    }

    private List<StorageObjectificationFailureSummary> governanceSummaries(Long projectId, long maxFileSizeBytes) {
        return jdbcTemplate.query("""
            SELECT reason_code, COUNT(1) AS file_count
            FROM (
                SELECT
                    CASE
                        WHEN COALESCE(mig.pending, 0) = 1 THEN 'MIGRATION_PENDING'
                        WHEN COALESCE(mig.failed, 0) = 1 THEN 'RETRY_AFTER_FAILURE_REVIEW'
                        WHEN f.storage_uri IS NULL OR f.storage_uri = ''
                            OR (UPPER(COALESCE(f.storage_provider, '')) NOT IN ('NAS', 'METADATA')
                                AND f.storage_uri NOT LIKE 'nas://%'
                                AND f.storage_uri NOT LIKE '/%')
                            THEN 'SOURCE_REFERENCE_REVIEW_REQUIRED'
                        WHEN COALESCE(f.size_bytes, 0) > :maxFileSizeBytes THEN 'FILE_TOO_LARGE_FOR_PROFILE'
                        ELSE 'READY_FOR_NEXT_PROFILE'
                    END AS reason_code
                FROM data_file_resources f
                LEFT JOIN (
                    SELECT DISTINCT file_id
                    FROM data_file_object_versions
                    WHERE active = 1
                      AND deleted = 0
                      AND storage_state = 'OBJECT_STORED'
                ) obj ON obj.file_id = f.id
                LEFT JOIN (
                    SELECT file_id,
                           MAX(CASE WHEN migration_status IN ('PENDING', 'RUNNING') THEN 1 ELSE 0 END) AS pending,
                           MAX(CASE WHEN migration_status = 'FAILED' THEN 1 ELSE 0 END) AS failed
                    FROM data_object_migration_tasks
                    WHERE deleted = 0
                    GROUP BY file_id
                ) mig ON mig.file_id = f.id
                WHERE f.project_id = :projectId
                  AND f.deleted = 0
                  AND obj.file_id IS NULL
            ) grouped
            WHERE reason_code <> 'READY_FOR_NEXT_PROFILE'
            GROUP BY reason_code
            ORDER BY file_count DESC, reason_code
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("maxFileSizeBytes", maxFileSizeBytes), (rs, rowNum) -> new StorageObjectificationFailureSummary(
            rs.getString("reason_code"),
            governanceReasonMessage(rs.getString("reason_code")),
            rs.getLong("file_count")
        ));
    }

    private String governanceReasonMessage(String reasonCode) {
        return switch (reasonCode == null ? "" : reasonCode) {
            case "MIGRATION_PENDING" -> "文件已有执行中的对象化任务，等待任务完成后再判断。";
            case "RETRY_AFTER_FAILURE_REVIEW" -> "文件曾对象化失败，需要按失败原因重试或人工治理。";
            case "SOURCE_REFERENCE_REVIEW_REQUIRED" -> "文件缺少可控 NAS 存储引用或来源状态异常，需要人工核查。";
            case "FILE_TOO_LARGE_FOR_PROFILE" -> "文件超过当前分层单文件上限，本轮不硬冲。";
            default -> "文件需要人工治理后再继续对象化。";
        };
    }

    private String latestFailureMessage(Long projectId) {
        List<String> rows = jdbcTemplate.query("""
            SELECT failure_reason
            FROM data_object_migration_tasks
            WHERE project_id = :projectId
              AND migration_status = 'FAILED'
              AND deleted = 0
            ORDER BY updated_at DESC, id DESC
            LIMIT 1
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) ->
            safeMessage(rs.getString("failure_reason"), "FAILED"));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private List<Long> failedFileIdsForRetry(Long projectId, int limit, long maxFileSizeBytes) {
        return jdbcTemplate.query("""
            SELECT DISTINCT t.file_id
            FROM data_object_migration_tasks t
            JOIN data_file_resources f ON f.id = t.file_id
               AND f.project_id = t.project_id
               AND f.deleted = 0
            LEFT JOIN (
                SELECT DISTINCT file_id
                FROM data_file_object_versions
                WHERE active = 1
                  AND deleted = 0
                  AND storage_state = 'OBJECT_STORED'
            ) obj ON obj.file_id = f.id
            WHERE t.project_id = :projectId
              AND t.migration_status = 'FAILED'
              AND t.deleted = 0
              AND obj.file_id IS NULL
              AND COALESCE(f.size_bytes, 0) <= :maxFileSizeBytes
              AND f.storage_uri IS NOT NULL
              AND f.storage_uri <> ''
              AND (
                  UPPER(COALESCE(f.storage_provider, '')) IN ('NAS', 'METADATA')
                  OR f.storage_uri LIKE 'nas://%'
                  OR f.storage_uri LIKE '/%'
              )
            ORDER BY t.file_id
            LIMIT :limit
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("maxFileSizeBytes", maxFileSizeBytes)
            .addValue("limit", limit), (rs, rowNum) -> rs.getLong("file_id"));
    }

    private int normalizeFullPlanBatchFileLimit(Integer value) {
        if (value == null) {
            return CONTROLLED_EXPANSION_MAX_FILES_TOTAL;
        }
        if (value <= 0 || value > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
            throw new BusinessException("STORAGE_FULL_PLAN_BATCH_FILE_LIMIT_EXCEEDED",
                "单批文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeFullPlanBatchBytesLimit(Long value) {
        return normalizeFullPlanBatchBytesLimit(value, CONTROLLED_EXPANSION_MAX_BYTES_TOTAL);
    }

    private long normalizeFullPlanBatchBytesLimit(Long value, long maxBatchBytesLimit) {
        if (value == null) {
            return FULL_PLAN_DEFAULT_BATCH_BYTES;
        }
        if (value <= 0 || value > maxBatchBytesLimit) {
            throw new BusinessException("STORAGE_FULL_PLAN_BATCH_BYTES_LIMIT_EXCEEDED",
                "单批容量超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeFullPlanMaxFileSize(long value) {
        if (value <= 0 || value > LONG_RUN_MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("STORAGE_FULL_PLAN_FILE_SIZE_LIMIT_EXCEEDED",
                "单文件大小上限超过受控范围。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String fullPlanGovernanceReason(MultiProjectDryRunCandidate row, long maxFileSizeBytes) {
        if (row.migrationPending()) {
            return "MIGRATION_PENDING";
        }
        if (row.migrationFailed()) {
            return "RETRY_AFTER_FAILURE_REVIEW";
        }
        if (unreadableRisk(row)) {
            return "SOURCE_REFERENCE_REVIEW_REQUIRED";
        }
        if (row.sizeBytes() != null && row.sizeBytes() > maxFileSizeBytes) {
            return "FILE_TOO_LARGE_FOR_BATCH";
        }
        return null;
    }

    private StorageObjectificationPlanSampleItem fullPlanItem(
        MultiProjectDryRunCandidate row,
        String storageStatus,
        String reason
    ) {
        return new StorageObjectificationPlanSampleItem(
            row.fileId(),
            row.assetUuid(),
            row.fileName(),
            row.fileKind(),
            row.extension(),
            row.sizeBytes(),
            hasText(row.checksum()) ? "HAS_CHECKSUM" : "MISSING_CHECKSUM",
            storageStatus,
            reason
        );
    }

    private List<String> fullPlanRiskMessages(
        ProjectStorageObjectificationInventoryResponse project,
        long eligibleRemainingCount,
        List<StorageObjectificationPlanSampleItem> governanceItems,
        List<StorageObjectificationFailureSummary> failureReasons
    ) {
        List<String> messages = new ArrayList<>();
        messages.add("105 全量计划只生成对象化批次建议，不移动、不删除、不改名 NAS 原文件。");
        if (project.totalFiles() > 0 && project.objectStoredFiles().equals(project.totalFiles())) {
            messages.add("当前项目已全部对象化。");
        }
        if (eligibleRemainingCount > 0) {
            messages.add("仍有可继续分批对象化的 NAS_ONLY 文件。");
        }
        if (project.migrationFailedFiles() > 0 || !failureReasons.isEmpty()) {
            messages.add("存在迁移失败文件，需按失败原因重试或人工治理。");
        }
        if (!governanceItems.isEmpty()) {
            messages.add("存在路径、大小、待执行或失败状态的治理项，不阻塞后续可执行批次。");
        }
        if (project.checksumCoverageRate().compareTo(BigDecimal.valueOf(90)) < 0) {
            messages.add("checksum 覆盖率不足，执行对象化时会补齐可计算文件的校验值。");
        }
        return messages;
    }

    private List<String> fullPlanSuggestions(
        long eligibleRemainingCount,
        int nextBatchCount,
        List<StorageObjectificationFailureSummary> failureReasons
    ) {
        List<String> suggestions = new ArrayList<>();
        if (nextBatchCount > 0) {
            suggestions.add("执行下一批 " + nextBatchCount + " 个文件，完成后刷新计划继续推进。");
        }
        if (eligibleRemainingCount > nextBatchCount) {
            suggestions.add("下一批完成后继续生成计划，直到可执行文件清零。");
        }
        if (!failureReasons.isEmpty()) {
            suggestions.add("对失败文件先查看任务详情，再按失败原因重试或人工治理。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("当前没有可执行下一批；请查看治理清单或确认项目已完成对象化。");
        }
        return suggestions;
    }

    private LatestTaskSummary latestTask(Long projectId) {
        List<LatestTaskSummary> rows = jdbcTemplate.query("""
            SELECT id, task_status, success_count, skipped_count, failure_count, updated_at
            FROM data_object_migration_task_batches
            WHERE project_id = :projectId
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new LatestTaskSummary(
            rs.getLong("id"),
            rs.getString("task_status"),
            rs.getInt("success_count"),
            rs.getInt("skipped_count"),
            rs.getInt("failure_count"),
            instant(rs.getTimestamp("updated_at"))
        ));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private long skippedFileCount(Long projectId) {
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT file_id)
            FROM data_object_migration_tasks
            WHERE project_id = :projectId
              AND migration_status = 'SKIPPED'
              AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Long.class);
        return count == null ? 0L : count;
    }

    private List<StorageObjectificationFailureSummary> failureSummaries(Long projectId) {
        return jdbcTemplate.query("""
            SELECT
                COALESCE(NULLIF(SUBSTRING_INDEX(failure_reason, ':', 1), ''), 'UNKNOWN') AS reason_code,
                MAX(failure_reason) AS sample_message,
                COUNT(DISTINCT file_id) AS file_count
            FROM data_object_migration_tasks
            WHERE project_id = :projectId
              AND migration_status = 'FAILED'
              AND deleted = 0
            GROUP BY reason_code
            ORDER BY file_count DESC, reason_code
            LIMIT 8
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> {
            ResultMessage resultMessage = splitResultMessage(rs.getString("sample_message"));
            return new StorageObjectificationFailureSummary(
                rs.getString("reason_code"),
                resultMessage.message(),
                rs.getLong("file_count")
            );
        });
    }

    private List<Long> normalizeExecutionProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECTS_REQUIRED",
                "受控执行必须明确选择真实项目。", HttpStatus.BAD_REQUEST);
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long projectId : projectIds) {
            if (projectId == null || projectId <= 0) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_ID_INVALID",
                    "项目ID不合法", HttpStatus.BAD_REQUEST);
            }
            normalized.add(projectId);
        }
        if (normalized.size() > CONTROLLED_EXPANSION_MAX_PROJECTS) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_LIMIT_EXCEEDED",
                "单次最多允许 5 个真实项目。", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(normalized);
    }

    private List<Long> normalizeExecutionFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_FILE_IDS_REQUIRED",
                "受控执行必须显式传入 dry-run 选中的文件ID。", HttpStatus.BAD_REQUEST);
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long fileId : fileIds) {
            if (fileId == null || fileId <= 0) {
                throw new BusinessException("STORAGE_MULTI_EXECUTION_FILE_ID_INVALID",
                    "文件ID不合法", HttpStatus.BAD_REQUEST);
            }
            normalized.add(fileId);
        }
        if (normalized.size() != fileIds.size()) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_FILE_IDS_DUPLICATED",
                "执行文件列表存在重复项。", HttpStatus.BAD_REQUEST);
        }
        if (normalized.size() > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_TOTAL_FILE_LIMIT_EXCEEDED",
                "总文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(normalized);
    }

    private int normalizeExecutionLimit(Integer value) {
        if (value == null) {
            return CONTROLLED_EXPANSION_MAX_FILES_TOTAL;
        }
        if (value <= 0 || value > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_TOTAL_FILE_LIMIT_EXCEEDED",
                "总文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private int normalizeExecutionMaxFilesPerProject(Integer value) {
        if (value == null) {
            return CONTROLLED_EXPANSION_MAX_FILES_PER_PROJECT;
        }
        if (value <= 0 || value > CONTROLLED_EXPANSION_MAX_FILES_PER_PROJECT) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_FILE_LIMIT_EXCEEDED",
                "单项目文件数超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeExecutionMaxBytesPerProject(Long value) {
        if (value == null) {
            return CONTROLLED_EXPANSION_MAX_BYTES_PER_PROJECT;
        }
        if (value <= 0 || value > CONTROLLED_EXPANSION_MAX_BYTES_PER_PROJECT) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_PROJECT_BYTES_LIMIT_EXCEEDED",
                "单项目容量超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizeExecutionMaxTotalBytes(Long value) {
        if (value == null) {
            return CONTROLLED_EXPANSION_MAX_BYTES_TOTAL;
        }
        if (value <= 0 || value > CONTROLLED_EXPANSION_MAX_BYTES_TOTAL) {
            throw new BusinessException("STORAGE_MULTI_EXECUTION_TOTAL_BYTES_LIMIT_EXCEEDED",
                "总容量超过本轮执行上限。", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private List<Long> normalizeOptionalProjectIds(List<Long> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long projectId : projectIds) {
            if (projectId == null || projectId <= 0) {
                throw new BusinessException("STORAGE_MULTI_PLAN_PROJECT_ID_INVALID",
                    "项目ID不合法", HttpStatus.BAD_REQUEST);
            }
            normalized.add(projectId);
        }
        if (normalized.size() > 20) {
            throw new BusinessException("STORAGE_MULTI_PLAN_PROJECT_LIMIT_EXCEEDED",
                "单次 dry-run 最多允许 20 个项目", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(normalized);
    }

    private int normalizePositiveInt(Integer value, int fallback, int max, String errorCode) {
        if (value == null) {
            return fallback;
        }
        if (value <= 0 || value > max) {
            throw new BusinessException(errorCode, "数值超出允许范围", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private long normalizePositiveLong(Long value, long fallback, String errorCode) {
        if (value == null) {
            return fallback;
        }
        if (value <= 0) {
            throw new BusinessException(errorCode, "容量上限必须大于 0", HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String projectCategory(String assetSource, String projectCode, String projectStage) {
        String source = assetSource == null ? "" : assetSource.toUpperCase(Locale.ROOT);
        String code = projectCode == null ? "" : projectCode.toUpperCase(Locale.ROOT);
        String stage = projectStage == null ? "" : projectStage.toUpperCase(Locale.ROOT);
        if (source.contains("TEST") || source.contains("SAMPLE") || source.contains("DEMO")
            || source.contains("AGENT") || code.contains("TEST") || code.contains("DEMO")) {
            return "TEST_OR_SAMPLE";
        }
        if (source.contains("ARCHIVE") || stage.contains("ARCHIVE")) {
            return "ARCHIVED";
        }
        if (source.contains("REAL") || source.contains("NAS") || code.matches("\\d{2,}")) {
            return "REAL_NAS";
        }
        return "UNKNOWN";
    }

    private Map<Long, List<StorageObjectificationDistributionItem>> distributionBy(List<Long> projectIds, String expression) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<StorageObjectificationDistributionItem>> result = new HashMap<>();
        jdbcTemplate.query("""
            SELECT project_id,
                   COALESCE(NULLIF(""" + expression + """
                   , ''), 'UNKNOWN') AS item_code,
                   COUNT(1) AS file_count,
                   COALESCE(SUM(COALESCE(size_bytes, 0)), 0) AS total_bytes
            FROM data_file_resources
            WHERE project_id IN (:projectIds)
              AND deleted = 0
            GROUP BY project_id, item_code
            ORDER BY project_id, file_count DESC
            """, new MapSqlParameterSource("projectIds", projectIds), rs -> {
            Long projectId = rs.getLong("project_id");
            result.computeIfAbsent(projectId, ignored -> new ArrayList<>())
                .add(new StorageObjectificationDistributionItem(
                    rs.getString("item_code"),
                    rs.getLong("file_count"),
                    rs.getLong("total_bytes")
                ));
        });
        result.replaceAll((projectId, rows) -> rows.stream().limit(8).toList());
        return result;
    }

    private ProjectStorageObjectificationInventoryResponse withDistributions(
        ProjectStorageObjectificationInventoryResponse row,
        List<StorageObjectificationDistributionItem> fileKindDistribution,
        List<StorageObjectificationDistributionItem> extensionDistribution
    ) {
        return new ProjectStorageObjectificationInventoryResponse(
            row.projectId(),
            row.projectCode(),
            row.projectName(),
            row.projectStage(),
            row.assetSource(),
            row.projectCategory(),
            row.realNasProject(),
            row.totalFiles(),
            row.totalBytes(),
            row.objectStoredFiles(),
            row.objectStoredBytes(),
            row.nasOnlyFiles(),
            row.nasOnlyBytes(),
            row.estimatedObjectificationBytes(),
            row.migrationPendingFiles(),
            row.migrationFailedFiles(),
            row.checksumCoveredFiles(),
            row.checksumCoverageRate(),
            row.modelFiles(),
            row.drawingFiles(),
            row.documentFiles(),
            row.largeFileCount(),
            row.unreadablePathFiles(),
            row.objectificationCoverageRate(),
            row.riskLevel(),
            row.riskMessages(),
            fileKindDistribution == null ? List.of() : fileKindDistribution,
            extensionDistribution == null ? List.of() : extensionDistribution
        );
    }

    private String storageStatus(DryRunCandidate row) {
        if (row.objectStored()) {
            return "OBJECT_STORED";
        }
        if (row.migrationFailed()) {
            return "MIGRATION_FAILED";
        }
        if (row.migrationPending()) {
            return "MIGRATION_PENDING";
        }
        return "NAS_ONLY";
    }

    private String storageStatus(MultiProjectDryRunCandidate row) {
        if (row.objectStored()) {
            return "OBJECT_STORED";
        }
        if (row.migrationFailed()) {
            return "MIGRATION_FAILED";
        }
        if (row.migrationPending()) {
            return "MIGRATION_PENDING";
        }
        return "NAS_ONLY";
    }

    private boolean unreadableRisk(DryRunCandidate row) {
        String provider = row.storageProvider() == null ? "" : row.storageProvider().toUpperCase(Locale.ROOT);
        return row.storageReferenceMissing()
            || (!row.objectStored() && hasText(provider) && !List.of("NAS", "METADATA").contains(provider));
    }

    private boolean unreadableRisk(MultiProjectDryRunCandidate row) {
        String provider = row.storageProvider() == null ? "" : row.storageProvider().toUpperCase(Locale.ROOT);
        return row.storageReferenceMissing()
            || (!row.objectStored() && hasText(provider) && !List.of("NAS", "METADATA").contains(provider));
    }

    private String dryRunReason(
        DryRunCandidate row,
        boolean missingChecksum,
        boolean oversized,
        boolean unreadableRisk
    ) {
        if (row.objectStored()) {
            return "ALREADY_OBJECT_STORED";
        }
        if (unreadableRisk) {
            return "SOURCE_REFERENCE_REVIEW_REQUIRED";
        }
        if (row.migrationFailed()) {
            return "RETRY_AFTER_FAILURE_REVIEW";
        }
        if (oversized) {
            return "LARGE_FILE_RISK";
        }
        if (missingChecksum) {
            return "MISSING_CHECKSUM";
        }
        return "ELIGIBLE_DRY_RUN";
    }

    private String dryRunReason(
        MultiProjectDryRunCandidate row,
        boolean missingChecksum,
        boolean oversized,
        boolean unreadableRisk
    ) {
        if (row.objectStored()) {
            return "ALREADY_OBJECT_STORED";
        }
        if (unreadableRisk) {
            return "SOURCE_REFERENCE_REVIEW_REQUIRED";
        }
        if (row.migrationFailed()) {
            return "RETRY_AFTER_FAILURE_REVIEW";
        }
        if (oversized) {
            return "LARGE_FILE_RISK";
        }
        if (missingChecksum) {
            return "MISSING_CHECKSUM";
        }
        return "ELIGIBLE_DRY_RUN";
    }

    private String normalizeExtension(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private List<Long> normalizeFileIds(List<Long> fileIds) {
        return normalizeFileIds(fileIds, DEFAULT_MAX_FILES);
    }

    private List<Long> normalizeFileIds(List<Long> fileIds, int maxFiles) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_IDS_REQUIRED",
                "请明确选择要迁移的小样本文件", HttpStatus.BAD_REQUEST);
        }
        if (maxFiles <= 0 || maxFiles > CONTROLLED_EXPANSION_MAX_FILES_TOTAL) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_LIMIT_INVALID",
                "迁移文件数量上限不合法", HttpStatus.BAD_REQUEST);
        }
        if (fileIds.size() > maxFiles) {
            throw new BusinessException("STORAGE_MIGRATION_FILE_LIMIT_EXCEEDED",
                "单次小样本迁移超过受控文件数上限", HttpStatus.BAD_REQUEST);
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
        validateSize(file, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    private void validateSize(FileAssetResponse file, long maxFileSizeBytes) {
        Long size = file.sizeBytes();
        if (size != null && size > maxFileSizeBytes) {
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

    private record LatestTaskSummary(
        Long taskId,
        String taskStatus,
        Integer successCount,
        Integer skippedCount,
        Integer failureCount,
        Instant updatedAt
    ) {
    }

    private record LongRunConfig(
        Integer batchFileLimit,
        Long batchBytesLimit,
        Long maxFileSizeBytes,
        Integer maxContinuousBatches,
        Boolean continueOnFailure
    ) {
    }

    private record IntegrityRepairConfig(
        Integer batchFileLimit,
        Long batchBytesLimit,
        Long maxFileSizeBytes
    ) {
    }

    private record ActiveObjectRow(
        Long fileId,
        String assetUuid,
        String fileName,
        String fileKind,
        Long fileSizeBytes,
        Long storageObjectId,
        String storageState,
        String provider,
        String bucket,
        String objectKey,
        String checksum,
        String contentType,
        String sourceProvider,
        String sourceUriDigest,
        String sourcePathDigest,
        Long expectedSizeBytes
    ) {
    }

    private record IntegrityScan(
        Long verifiedCount,
        Long missingCount,
        Long unreadableCount,
        Long sizeMismatchCount,
        Long governanceItemCount,
        String latestFailureReason,
        List<StorageObjectificationIntegrityIssue> issues
    ) {
    }

    private record IntegrityRepairResult(
        boolean repaired,
        StorageObjectificationIntegrityIssue issue
    ) {
    }

    private record DryRunFilters(
        String directoryPath,
        List<String> fileKinds,
        List<String> extensions,
        Long minSizeBytes,
        Long maxSizeBytes,
        String checksumState,
        String storageState,
        Integer limit,
        Long maxTotalBytes
    ) {
    }

    private record DryRunCandidate(
        Long fileId,
        String assetUuid,
        String fileName,
        String fileKind,
        String extension,
        Long sizeBytes,
        String checksum,
        String storageProvider,
        boolean storageReferenceMissing,
        boolean objectStored,
        boolean migrationPending,
        boolean migrationFailed
    ) {
    }

    private record ProjectScopeRow(
        Long projectId,
        String code,
        String projectStage,
        String assetSource
    ) {
    }

    private record MultiProjectDryRunCandidate(
        Long projectId,
        String projectCode,
        String projectName,
        String assetSource,
        String projectCategory,
        Boolean realNasProject,
        Long fileId,
        String assetUuid,
        String fileName,
        String fileKind,
        String extension,
        Long sizeBytes,
        String checksum,
        String storageProvider,
        boolean storageReferenceMissing,
        boolean objectStored,
        boolean migrationPending,
        boolean migrationFailed
    ) {
    }

    private static final class MultiProjectPlanAccumulator {
        private final Long projectId;
        private final String projectCode;
        private final String projectName;
        private final String assetSource;
        private final String projectCategory;
        private final Boolean realNasProject;
        private long selectedFileCount;
        private long selectedTotalBytes;
        private long objectStoredSkipCount;
        private long missingChecksumCount;
        private long oversizedCount;
        private long unreadableRiskCount;
        private final List<StorageObjectificationPlanSampleItem> sampleItems = new ArrayList<>();

        private MultiProjectPlanAccumulator(MultiProjectDryRunCandidate row) {
            this.projectId = row.projectId();
            this.projectCode = row.projectCode();
            this.projectName = row.projectName();
            this.assetSource = row.assetSource();
            this.projectCategory = row.projectCategory();
            this.realNasProject = row.realNasProject();
        }

        private MultiProjectStorageObjectificationPlanProject toResponse() {
            List<String> riskMessages = new ArrayList<>();
            if (selectedFileCount == 0) {
                riskMessages.add("当前项目在本次筛选和上限下没有选中可迁移文件。");
            }
            if (objectStoredSkipCount > 0) {
                riskMessages.add("已有对象版本的文件会跳过。");
            }
            if (missingChecksumCount > 0) {
                riskMessages.add("存在缺少 checksum 的文件，真实执行会在对象化时计算校验值。");
            }
            if (oversizedCount > 0) {
                riskMessages.add("存在大文件，后续应拆批观察。");
            }
            if (unreadableRiskCount > 0) {
                riskMessages.add("存在存储引用风险文件，需人工核查。");
            }
            if (riskMessages.isEmpty()) {
                riskMessages.add("当前项目 dry-run 未发现阻塞级风险。");
            }
            return new MultiProjectStorageObjectificationPlanProject(
                projectId,
                projectCode,
                projectName,
                assetSource,
                projectCategory,
                realNasProject,
                selectedFileCount,
                selectedTotalBytes,
                objectStoredSkipCount,
                missingChecksumCount,
                oversizedCount,
                unreadableRiskCount,
                selectedFileCount == 0 ? 0 : (long) Math.ceil(selectedFileCount / (double) DEFAULT_MAX_FILES),
                riskMessages,
                sampleItems
            );
        }
    }

    private static final class ExecutionProjectSelection {
        private final Long projectId;
        private final String projectCode;
        private final String projectName;
        private long selectedTotalBytes;
        private final List<Long> fileIds = new ArrayList<>();

        private ExecutionProjectSelection(MultiProjectDryRunCandidate row) {
            this.projectId = row.projectId();
            this.projectCode = row.projectCode();
            this.projectName = row.projectName();
        }
    }

    private record RunQueue(
        String status,
        String reason,
        String message
    ) {
    }

    private record ObjectificationRunConfig(
        List<Long> projectIds,
        int maxProjects,
        int maxTotalFiles,
        long maxTotalBytes,
        int maxFilesPerProject,
        long maxBytesPerProject,
        long maxFileSizeBytes,
        int maxContinuousBatches,
        Long rateLimitBytesPerMinute,
        boolean continueOnFailure,
        String targetProvider
    ) {
        private ObjectificationRunConfig withRemaining(int remainingFiles, long remainingBytes) {
            return new ObjectificationRunConfig(
                projectIds,
                maxProjects,
                Math.min(maxTotalFiles, remainingFiles),
                Math.min(maxTotalBytes, remainingBytes),
                maxFilesPerProject,
                maxBytesPerProject,
                maxFileSizeBytes,
                1,
                rateLimitBytesPerMinute,
                continueOnFailure,
                targetProvider
            );
        }
    }

    private record ObjectificationRunSelection(
        int requestedProjectCount,
        long selectedFileCount,
        long selectedTotalBytes,
        long objectStoredSkipCount,
        long missingChecksumCount,
        long oversizedCount,
        long unreadableRiskCount,
        long governanceItemCount,
        List<String> caps,
        List<ObjectificationRunProjectSelection> projects
    ) {
    }

    private record StorageReadPolicyCounts(
        Long totalFileCount,
        Long objectStoredCount,
        Long nasOnlyCount,
        Long migrationPendingCount,
        Long migrationFailedCount,
        Long objectUnreadableCount
    ) {
    }

    private static final class ObjectificationRunProjectSelection {
        private final Long projectId;
        private final String projectCode;
        private final String projectName;
        private final String projectCategory;
        private long selectedFileCount;
        private long selectedTotalBytes;
        private long objectStoredSkipCount;
        private long missingChecksumCount;
        private long oversizedCount;
        private long unreadableRiskCount;
        private long governanceItemCount;
        private final List<Long> fileIds = new ArrayList<>();
        private final List<StorageObjectificationPlanSampleItem> sampleItems = new ArrayList<>();

        private ObjectificationRunProjectSelection(StorageObjectificationRunProject project) {
            this.projectId = project.projectId();
            this.projectCode = project.projectCode();
            this.projectName = project.projectName();
            this.projectCategory = project.projectCategory();
        }

        private Long projectId() {
            return projectId;
        }

        private String projectCode() {
            return projectCode;
        }

        private String projectName() {
            return projectName;
        }

        private long selectedTotalBytes() {
            return selectedTotalBytes;
        }

        private List<Long> fileIds() {
            return List.copyOf(fileIds);
        }

        private MultiProjectStorageObjectificationPlanProject toResponse() {
            List<String> riskMessages = new ArrayList<>();
            if (selectedFileCount == 0) {
                riskMessages.add("当前项目在本轮上限内没有选中可执行文件。");
            }
            if (missingChecksumCount > 0) {
                riskMessages.add("存在缺少 checksum 的文件，执行时会计算并回写校验值。");
            }
            if (oversizedCount > 0) {
                riskMessages.add("存在超过本轮单文件上限的文件，已进入治理提示。");
            }
            if (unreadableRiskCount > 0) {
                riskMessages.add("存在源文件引用风险，需人工治理。");
            }
            if (governanceItemCount > 0) {
                riskMessages.add("存在失败或治理项，不阻塞其他项目继续推进。");
            }
            if (riskMessages.isEmpty()) {
                riskMessages.add("当前项目可执行文件已进入本轮计划。");
            }
            return new MultiProjectStorageObjectificationPlanProject(
                projectId,
                projectCode,
                projectName,
                null,
                projectCategory,
                true,
                selectedFileCount,
                selectedTotalBytes,
                objectStoredSkipCount,
                missingChecksumCount,
                oversizedCount,
                unreadableRiskCount,
                selectedFileCount == 0 ? 0 : 1L,
                riskMessages,
                sampleItems
            );
        }
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

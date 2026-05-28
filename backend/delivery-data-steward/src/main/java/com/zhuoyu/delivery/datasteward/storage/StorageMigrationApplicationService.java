package com.zhuoyu.delivery.datasteward.storage;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.MultiProjectStorageObjectificationPlanProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectStorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationDistributionItem;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationInventoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanDryRunResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageObjectificationPlanSampleItem;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskListItemResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationTaskRowResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageMigrationSummaryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageProviderReadinessResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final long LARGE_FILE_RISK_BYTES = 500L * 1024L * 1024L;
    private static final int DEFAULT_DRY_RUN_LIMIT = 500;
    private static final int MAX_DRY_RUN_LIMIT = 5000;

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

    public StorageProviderReadinessResponse minioReadiness(Long userId) {
        ensureAnyProjectAccess(userId);
        return storageService.minioReadiness();
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

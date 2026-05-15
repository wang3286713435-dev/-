package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetScanTaskRepository {

    private static final RowMapper<ScanTaskResponse> ROW_MAPPER = AssetScanTaskRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetScanTaskRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String rootCode, String rootPath, Long projectId, String projectCode,
                        Boolean recursive, String extensions, Boolean skipLowValueDirectories,
                        String skipDirectoryKeywords, Long operatorId) {
        String sql = """
            INSERT INTO data_asset_scan_tasks (
                root_code, root_path, project_id, project_code, `recursive`, extensions,
                skip_low_value_directories, skip_directory_keywords,
                status, created_by, updated_by
            ) VALUES (
                :rootCode, :rootPath, :projectId, :projectCode, :recursive, :extensions,
                :skipLowValueDirectories, :skipDirectoryKeywords,
                'PENDING', :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("rootCode", rootCode)
            .addValue("rootPath", rootPath)
            .addValue("projectId", projectId)
            .addValue("projectCode", projectCode)
            .addValue("recursive", Boolean.TRUE.equals(recursive) ? 1 : 0)
            .addValue("extensions", extensions)
            .addValue("skipLowValueDirectories", Boolean.TRUE.equals(skipLowValueDirectories) ? 1 : 0)
            .addValue("skipDirectoryKeywords", skipDirectoryKeywords)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updateStatus(Long taskId, String status, String progressMessage) {
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET status = :status, progress_message = :progressMessage
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("status", status)
            .addValue("progressMessage", progressMessage));
    }

    public void markRunning(Long taskId) {
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET status = 'RUNNING',
                cancel_requested = 0,
                progress_message = '扫描启动',
                progress_current = 0,
                progress_total = 0,
                progress_percent = 0.00,
                started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
                completed_at = NULL
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource("taskId", taskId));
    }

    public void updateProgress(Long taskId, int progressCurrent, int progressTotal,
                                int totalScanned, int autoIngested, int pendingReview,
                                int failedCount, int skippedLowValue, int skippedDirectories,
                                String lastScannedPath, String progressMessage) {
        java.math.BigDecimal pct = progressTotal > 0
            ? java.math.BigDecimal.valueOf(progressCurrent * 100.0 / progressTotal)
                .setScale(2, java.math.RoundingMode.HALF_UP)
            : java.math.BigDecimal.ZERO;
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET progress_current = :progressCurrent,
                progress_total = :progressTotal,
                progress_percent = :progressPercent,
                progress_message = :progressMessage,
                total_scanned = :totalScanned,
                auto_ingested = :autoIngested,
                pending_review = :pendingReview,
                failed_count = :failedCount,
                skipped_low_value = :skippedLowValue,
                skipped_directories = :skippedDirectories,
                last_scanned_path = :lastScannedPath
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("progressCurrent", progressCurrent)
            .addValue("progressTotal", progressTotal)
            .addValue("progressPercent", pct)
            .addValue("progressMessage", progressMessage)
            .addValue("totalScanned", totalScanned)
            .addValue("autoIngested", autoIngested)
            .addValue("pendingReview", pendingReview)
            .addValue("failedCount", failedCount)
            .addValue("skippedLowValue", skippedLowValue)
            .addValue("skippedDirectories", skippedDirectories)
            .addValue("lastScannedPath", truncate(lastScannedPath, 1024)));
    }

    public void markCompleted(Long taskId, int totalScanned, int autoIngested, int pendingReview,
                               int failedCount, int skippedLowValue, int skippedDirectories,
                               String lastScannedPath, String failureReason, String scanReportJson) {
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET status = :status,
                progress_current = :totalScanned,
                progress_total = :totalScanned,
                progress_percent = 100.00,
                progress_message = :progressMessage,
                total_scanned = :totalScanned,
                auto_ingested = :autoIngested,
                pending_review = :pendingReview,
                failed_count = :failedCount,
                skipped_low_value = :skippedLowValue,
                skipped_directories = :skippedDirectories,
                last_scanned_path = :lastScannedPath,
                failure_reason = :failureReason,
                scan_report_json = CAST(:scanReportJson AS JSON),
                completed_at = CURRENT_TIMESTAMP
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("status", failedCount > 0 ? "FAILED" : "SUCCEEDED")
            .addValue("progressMessage", failedCount > 0 ? "扫描失败" : "扫描完成")
            .addValue("totalScanned", totalScanned)
            .addValue("autoIngested", autoIngested)
            .addValue("pendingReview", pendingReview)
            .addValue("failedCount", failedCount)
            .addValue("skippedLowValue", skippedLowValue)
            .addValue("skippedDirectories", skippedDirectories)
            .addValue("lastScannedPath", truncate(lastScannedPath, 1024))
            .addValue("failureReason", failureReason)
            .addValue("scanReportJson", scanReportJson));
    }

    public void requestCancel(Long taskId) {
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET cancel_requested = 1,
                status = CASE WHEN status = 'PENDING' THEN 'CANCELED' ELSE status END,
                progress_message = CASE WHEN status = 'PENDING' THEN '已取消' ELSE '取消请求已提交' END,
                completed_at = CASE WHEN status = 'PENDING' THEN CURRENT_TIMESTAMP ELSE completed_at END
            WHERE id = :taskId AND deleted = 0
              AND status IN ('PENDING', 'RUNNING')
            """, new MapSqlParameterSource("taskId", taskId));
    }

    public void markCanceled(Long taskId, int totalScanned, int autoIngested, int pendingReview,
                              int failedCount, int skippedLowValue, int skippedDirectories,
                              String lastScannedPath, String scanReportJson) {
        jdbcTemplate.update("""
            UPDATE data_asset_scan_tasks
            SET status = 'CANCELED',
                cancel_requested = 1,
                progress_message = '扫描已取消',
                progress_current = :totalScanned,
                progress_total = :totalScanned,
                total_scanned = :totalScanned,
                auto_ingested = :autoIngested,
                pending_review = :pendingReview,
                failed_count = :failedCount,
                skipped_low_value = :skippedLowValue,
                skipped_directories = :skippedDirectories,
                last_scanned_path = :lastScannedPath,
                scan_report_json = CAST(:scanReportJson AS JSON),
                completed_at = CURRENT_TIMESTAMP
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("taskId", taskId)
            .addValue("totalScanned", totalScanned)
            .addValue("autoIngested", autoIngested)
            .addValue("pendingReview", pendingReview)
            .addValue("failedCount", failedCount)
            .addValue("skippedLowValue", skippedLowValue)
            .addValue("skippedDirectories", skippedDirectories)
            .addValue("lastScannedPath", truncate(lastScannedPath, 1024))
            .addValue("scanReportJson", scanReportJson));
    }

    public boolean isCancelRequested(Long taskId) {
        Boolean requested = jdbcTemplate.queryForObject("""
            SELECT cancel_requested = 1
            FROM data_asset_scan_tasks
            WHERE id = :taskId AND deleted = 0
            """, new MapSqlParameterSource("taskId", taskId), Boolean.class);
        return Boolean.TRUE.equals(requested);
    }

    public ScanTaskResponse requireById(Long taskId) {
        List<ScanTaskResponse> rows = jdbcTemplate.query("""
            SELECT task.id, task.root_code, task.root_path, task.project_id,
                   COALESCE(NULLIF(task.project_code, ''), project.code) AS project_code,
                   task.`recursive`, task.extensions, task.skip_low_value_directories,
                   task.skip_directory_keywords, task.status, task.progress_message,
                   task.progress_current, task.progress_total, task.progress_percent,
                   task.cancel_requested, task.total_scanned, task.auto_ingested,
                   task.pending_review, task.failed_count, task.skipped_low_value,
                   task.skipped_directories, task.last_scanned_path, task.failure_reason,
                   task.scan_report_json, task.started_at, task.completed_at,
                   task.created_at, task.updated_at, task.created_by
            FROM data_asset_scan_tasks task
            LEFT JOIN core_projects project ON project.id = task.project_id AND project.deleted = 0
            WHERE task.id = :taskId AND task.deleted = 0
            """, new MapSqlParameterSource("taskId", taskId), ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_SCAN_TASK_NOT_FOUND", "扫描任务不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    public List<ScanTaskResponse> listLatest(int limit) {
        return jdbcTemplate.query("""
            SELECT task.id, task.root_code, task.root_path, task.project_id,
                   COALESCE(NULLIF(task.project_code, ''), project.code) AS project_code,
                   task.`recursive`, task.extensions, task.skip_low_value_directories,
                   task.skip_directory_keywords, task.status, task.progress_message,
                   task.progress_current, task.progress_total, task.progress_percent,
                   task.cancel_requested, task.total_scanned, task.auto_ingested,
                   task.pending_review, task.failed_count, task.skipped_low_value,
                   task.skipped_directories, task.last_scanned_path, task.failure_reason,
                   task.scan_report_json, task.started_at, task.completed_at,
                   task.created_at, task.updated_at, task.created_by
            FROM data_asset_scan_tasks task
            LEFT JOIN core_projects project ON project.id = task.project_id AND project.deleted = 0
            WHERE task.deleted = 0
            ORDER BY task.id DESC
            LIMIT :limit
            """, new MapSqlParameterSource("limit", Math.max(1, Math.min(limit, 100))), ROW_MAPPER);
    }

    private static ScanTaskResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp completedAt = rs.getTimestamp("completed_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        String extensions = rs.getString("extensions");
        String status = rs.getString("status");
        int totalScanned = rs.getInt("total_scanned");
        int progressCurrent = rs.getInt("progress_current");
        int progressTotal = rs.getInt("progress_total");
        BigDecimal progressPercent = rs.getBigDecimal("progress_percent");
        if ("SUCCEEDED".equals(status) && (progressPercent == null || progressPercent.compareTo(BigDecimal.valueOf(100)) < 0)) {
            progressCurrent = totalScanned;
            progressTotal = totalScanned;
            progressPercent = BigDecimal.valueOf(100).setScale(2);
        }
        return new ScanTaskResponse(
            rs.getLong("id"),
            rs.getString("root_code"),
            rs.getString("root_path"),
            rs.getObject("project_id", Long.class),
            rs.getString("project_code"),
            rs.getInt("recursive") == 1,
            extensions,
            rs.getInt("skip_low_value_directories") == 1,
            rs.getString("skip_directory_keywords"),
            status,
            rs.getString("progress_message"),
            progressCurrent,
            progressTotal,
            progressPercent,
            rs.getInt("cancel_requested") == 1,
            totalScanned,
            rs.getInt("auto_ingested"),
            rs.getInt("pending_review"),
            rs.getInt("failed_count"),
            rs.getInt("skipped_low_value"),
            rs.getInt("skipped_directories"),
            rs.getString("last_scanned_path"),
            rs.getString("failure_reason"),
            rs.getString("scan_report_json"),
            startedAt == null ? null : startedAt.toInstant(),
            completedAt == null ? null : completedAt.toInstant(),
            createdAt == null ? null : createdAt.toInstant(),
            updatedAt == null ? null : updatedAt.toInstant(),
            rs.getObject("created_by", Long.class)
        );
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}

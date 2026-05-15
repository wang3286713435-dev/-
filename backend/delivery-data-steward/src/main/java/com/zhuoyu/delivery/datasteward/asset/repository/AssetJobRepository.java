package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetJobRepository {

    private static final RowMapper<JobResponse> JOB_ROW_MAPPER = AssetJobRepository::mapJob;

    private static final String SELECT_COLS = """
        SELECT id, job_type, status, project_id, target_type, target_id,
               request_payload, progress_current, progress_total, progress_percent,
               progress_message, failure_reason, retry_count, max_retries,
               created_by, started_at, completed_at, created_at, updated_at
        FROM data_asset_jobs
        WHERE deleted = 0
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String jobType, Long projectId, String targetType, Long targetId,
                        String requestPayload, int maxRetries, Long createdBy) {
        String sql = """
            INSERT INTO data_asset_jobs (job_type, status, project_id, target_type, target_id,
                request_payload, progress_current, progress_total, progress_percent, max_retries, created_by)
            VALUES (:jobType, 'PENDING', :projectId, :targetType, :targetId,
                :requestPayload, 0, 0, 0.00, :maxRetries, :createdBy)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("jobType", jobType)
            .addValue("projectId", projectId)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("requestPayload", requestPayload)
            .addValue("maxRetries", maxRetries)
            .addValue("createdBy", createdBy), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<JobResponse> listForUser(Long userId, Set<Long> accessibleProjectIds,
                                          String jobType, String status, Long projectId, int limit) {
        StringBuilder sb = new StringBuilder(SELECT_COLS);
        sb.append(" AND (created_by = :userId");
        if (accessibleProjectIds != null && !accessibleProjectIds.isEmpty()) {
            sb.append(" OR project_id IN (:accessibleProjectIds)");
        }
        sb.append(")");
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (accessibleProjectIds != null && !accessibleProjectIds.isEmpty()) {
            params.addValue("accessibleProjectIds", accessibleProjectIds);
        }
        if (jobType != null && !jobType.isBlank()) {
            sb.append(" AND job_type = :jobType");
            params.addValue("jobType", jobType.trim());
        }
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = :status");
            params.addValue("status", status.trim());
        }
        if (projectId != null) {
            sb.append(" AND project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        sb.append(" ORDER BY created_at DESC LIMIT :limit");
        params.addValue("limit", limit);
        return jdbcTemplate.query(sb.toString(), params, JOB_ROW_MAPPER);
    }

    public JobResponse requireById(Long id) {
        List<JobResponse> rows = jdbcTemplate.query(
            SELECT_COLS + " AND id = :id",
            new MapSqlParameterSource("id", id), JOB_ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_JOB_NOT_FOUND", "任务不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    public JobResponse findById(Long id) {
        List<JobResponse> rows = jdbcTemplate.query(
            SELECT_COLS + " AND id = :id",
            new MapSqlParameterSource("id", id), JOB_ROW_MAPPER);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<JobResponse> listForAgent(List<Long> accessibleProjectIds, Long userId,
                                           Long projectId, String jobType, String status) {
        StringBuilder sb = new StringBuilder(SELECT_COLS);
        sb.append(" AND (created_by = :userId");
        if (accessibleProjectIds != null && !accessibleProjectIds.isEmpty()) {
            sb.append(" OR project_id IN (:accessibleProjectIds)");
        }
        sb.append(")");
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (accessibleProjectIds != null && !accessibleProjectIds.isEmpty()) {
            params.addValue("accessibleProjectIds", accessibleProjectIds);
        }
        if (jobType != null && !jobType.isBlank()) {
            sb.append(" AND job_type = :jobType");
            params.addValue("jobType", jobType.trim());
        }
        if (status != null && !status.isBlank()) {
            sb.append(" AND status = :status");
            params.addValue("status", status.trim());
        }
        if (projectId != null) {
            sb.append(" AND project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        sb.append(" ORDER BY created_at DESC LIMIT 200");
        return jdbcTemplate.query(sb.toString(), params, JOB_ROW_MAPPER);
    }

    public List<JobResponse> findPending(int limit) {
        return jdbcTemplate.query(
            SELECT_COLS + " AND status = 'PENDING' ORDER BY created_at ASC LIMIT :limit",
            new MapSqlParameterSource("limit", limit), JOB_ROW_MAPPER);
    }

    public List<JobResponse> findByTarget(String targetType, Long targetId) {
        return jdbcTemplate.query(
            SELECT_COLS + " AND target_type = :targetType AND target_id = :targetId ORDER BY created_at DESC",
            new MapSqlParameterSource()
                .addValue("targetType", targetType)
                .addValue("targetId", targetId),
            JOB_ROW_MAPPER);
    }

    public boolean markRunning(Long id) {
        int updated = jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET status = 'RUNNING', started_at = CURRENT_TIMESTAMP,
                progress_current = 0, progress_message = '执行中'
            WHERE id = :id AND status = 'PENDING' AND deleted = 0
            """, new MapSqlParameterSource("id", id));
        return updated > 0;
    }

    public void updateProgress(Long id, int current, int total, String message) {
        java.math.BigDecimal pct = total > 0
            ? java.math.BigDecimal.valueOf(current * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP)
            : java.math.BigDecimal.ZERO;
        jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET progress_current = :current, progress_total = :total,
                progress_percent = :pct, progress_message = :message
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("current", current)
            .addValue("total", total)
            .addValue("pct", pct)
            .addValue("message", message));
    }

    public void markSucceeded(Long id, String message) {
        java.math.BigDecimal pct = java.math.BigDecimal.valueOf(100.00);
        jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET status = 'SUCCEEDED', progress_percent = :pct,
                progress_message = :message, completed_at = CURRENT_TIMESTAMP
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("pct", pct)
            .addValue("message", message));
    }

    public void markFailed(Long id, String failureReason) {
        jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET status = 'FAILED', failure_reason = :failureReason,
                completed_at = CURRENT_TIMESTAMP, progress_message = '执行失败'
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("failureReason", failureReason));
    }

    public void markCanceled(Long id) {
        jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET status = 'CANCELED', completed_at = CURRENT_TIMESTAMP,
                progress_message = '已取消'
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource("id", id));
    }

    public boolean retry(Long id) {
        int updated = jdbcTemplate.update("""
            UPDATE data_asset_jobs
            SET status = 'PENDING', retry_count = retry_count + 1,
                failure_reason = NULL, started_at = NULL, completed_at = NULL,
                progress_current = 0, progress_total = 0, progress_percent = 0.00,
                progress_message = NULL
            WHERE id = :id AND status = 'FAILED' AND deleted = 0
              AND retry_count < max_retries
            """, new MapSqlParameterSource("id", id));
        return updated > 0;
    }

    private static JobResponse mapJob(ResultSet rs, int rowNum) throws SQLException {
        Timestamp sa = rs.getTimestamp("started_at");
        Timestamp ca = rs.getTimestamp("completed_at");
        Timestamp cra = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        return new JobResponse(
            rs.getLong("id"),
            rs.getString("job_type"),
            rs.getString("status"),
            rs.getObject("project_id", Long.class),
            rs.getString("target_type"),
            rs.getObject("target_id", Long.class),
            rs.getString("request_payload"),
            rs.getInt("progress_current"),
            rs.getInt("progress_total"),
            rs.getBigDecimal("progress_percent"),
            rs.getString("progress_message"),
            rs.getString("failure_reason"),
            rs.getInt("retry_count"),
            rs.getInt("max_retries"),
            rs.getObject("created_by", Long.class),
            sa == null ? null : sa.toInstant(),
            ca == null ? null : ca.toInstant(),
            cra == null ? null : cra.toInstant(),
            ua == null ? null : ua.toInstant()
        );
    }
}

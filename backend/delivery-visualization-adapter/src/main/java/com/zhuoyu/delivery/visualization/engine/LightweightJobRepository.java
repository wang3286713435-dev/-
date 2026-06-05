package com.zhuoyu.delivery.visualization.engine;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class LightweightJobRepository {

    private static final RowMapper<LightweightJobRecord> ROW_MAPPER = LightweightJobRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LightweightJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<LightweightJobRecord> findReusable(Long projectId, Long fileId) {
        String sql = """
            SELECT *
            FROM visualization_lightweight_jobs
            WHERE project_id = :projectId
              AND file_id = :fileId
              AND deleted = 0
              AND status IN ('SUBMITTED', 'UPLOADED', 'RUNNING', 'READY')
            ORDER BY id DESC
            LIMIT 1
            """;
        List<LightweightJobRecord> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<LightweightJobRecord> findLatest(Long projectId, Long fileId) {
        String sql = """
            SELECT *
            FROM visualization_lightweight_jobs
            WHERE project_id = :projectId
              AND file_id = :fileId
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """;
        List<LightweightJobRecord> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<LightweightJobRecord> findById(Long projectId, Long jobId) {
        String sql = """
            SELECT *
            FROM visualization_lightweight_jobs
            WHERE project_id = :projectId
              AND id = :jobId
              AND deleted = 0
            """;
        List<LightweightJobRecord> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("jobId", jobId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public long countViewerReady(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM visualization_lightweight_jobs
            WHERE project_id = :projectId
              AND deleted = 0
              AND status = 'READY'
              AND viewer_available = 1
              AND model_access_address IS NOT NULL
              AND model_access_address <> ''
            """;
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId), Long.class);
        return count == null ? 0L : count;
    }

    public Long insertSubmitting(
        Long projectId,
        Long integrationId,
        Long fileId,
        String assetUuid,
        Integer engineType,
        String lightweightName,
        String uniqueCode,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO visualization_lightweight_jobs (
                project_id, integration_id, file_id, asset_uuid, engine_provider, engine_type,
                lightweight_name, unique_code, status, progress_percent, viewer_available,
                created_by, updated_by
            ) VALUES (
                :projectId, :integrationId, :fileId, :assetUuid, 'GLANDAR', :engineType,
                :lightweightName, :uniqueCode, 'SUBMITTED', 0, 0,
                :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("integrationId", integrationId)
            .addValue("fileId", fileId)
            .addValue("assetUuid", assetUuid)
            .addValue("engineType", engineType)
            .addValue("lightweightName", lightweightName)
            .addValue("uniqueCode", uniqueCode)
            .addValue("operatorId", operatorId), keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void markUploaded(Long jobId, String lightweightName, String stationRecordJson, Long operatorId) {
        String sql = """
            UPDATE visualization_lightweight_jobs
            SET lightweight_name = :lightweightName,
                status = 'UPLOADED',
                progress_percent = 20,
                station_record_json = CAST(:stationRecordJson AS JSON),
                last_error_code = NULL,
                last_error_message = NULL,
                updated_by = :operatorId
            WHERE id = :jobId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("jobId", jobId)
            .addValue("lightweightName", lightweightName)
            .addValue("stationRecordJson", stationRecordJson)
            .addValue("operatorId", operatorId));
    }

    public void markFailed(Long jobId, String code, String message, Long operatorId) {
        String sql = """
            UPDATE visualization_lightweight_jobs
            SET status = 'FAILED',
                progress_percent = 0,
                viewer_available = 0,
                last_error_code = :code,
                last_error_message = :message,
                updated_by = :operatorId
            WHERE id = :jobId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("jobId", jobId)
            .addValue("code", truncate(code, 64))
            .addValue("message", truncate(message, 512))
            .addValue("operatorId", operatorId));
    }

    public void markSuperseded(Long projectId, Long fileId, Long operatorId) {
        String sql = """
            UPDATE visualization_lightweight_jobs
            SET status = 'SUPERSEDED',
                viewer_available = 0,
                last_error_code = 'SUPERSEDED_BY_FORCE_RETRY',
                last_error_message = '已由人工强制重新提交替代',
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND file_id = :fileId
              AND deleted = 0
              AND status IN ('SUBMITTED', 'UPLOADED', 'RUNNING')
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("operatorId", operatorId));
    }

    public void updateStationStatus(
        Long jobId,
        String status,
        Integer progressPercent,
        String modelAccessAddress,
        Boolean viewerAvailable,
        String errorCode,
        String errorMessage,
        String stationRecordJson
    ) {
        String sql = """
            UPDATE visualization_lightweight_jobs
            SET status = :status,
                progress_percent = :progressPercent,
                model_access_address = :modelAccessAddress,
                viewer_available = :viewerAvailable,
                last_error_code = :errorCode,
                last_error_message = :errorMessage,
                station_record_json = CAST(:stationRecordJson AS JSON)
            WHERE id = :jobId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("jobId", jobId)
            .addValue("status", status)
            .addValue("progressPercent", progressPercent)
            .addValue("modelAccessAddress", truncate(modelAccessAddress, 512))
            .addValue("viewerAvailable", Boolean.TRUE.equals(viewerAvailable) ? 1 : 0)
            .addValue("errorCode", truncate(errorCode, 64))
            .addValue("errorMessage", truncate(errorMessage, 512))
            .addValue("stationRecordJson", stationRecordJson));
    }

    private static LightweightJobRecord map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new LightweightJobRecord(
            rs.getLong("id"),
            rs.getLong("project_id"),
            getNullableLong(rs, "integration_id"),
            rs.getLong("file_id"),
            rs.getString("asset_uuid"),
            rs.getString("engine_provider"),
            rs.getInt("engine_type"),
            rs.getString("lightweight_name"),
            rs.getString("unique_code"),
            rs.getString("status"),
            rs.getInt("progress_percent"),
            rs.getString("model_access_address"),
            rs.getBoolean("viewer_available"),
            rs.getString("last_error_code"),
            rs.getString("last_error_message"),
            rs.getString("station_record_json"),
            getNullableLong(rs, "created_by"),
            createdAt == null ? null : createdAt.toInstant(),
            updatedAt == null ? null : updatedAt.toInstant()
        );
    }

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record LightweightJobRecord(
        Long id,
        Long projectId,
        Long integrationId,
        Long fileId,
        String assetUuid,
        String engineProvider,
        Integer engineType,
        String lightweightName,
        String uniqueCode,
        String status,
        Integer progressPercent,
        String modelAccessAddress,
        Boolean viewerAvailable,
        String lastErrorCode,
        String lastErrorMessage,
        String stationRecordJson,
        Long createdBy,
        Instant createdAt,
        Instant updatedAt
    ) {
    }
}

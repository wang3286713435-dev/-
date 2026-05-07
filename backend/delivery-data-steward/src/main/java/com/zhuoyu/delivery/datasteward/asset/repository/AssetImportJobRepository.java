package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ImportJobResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetImportJobRepository {

    private static final RowMapper<ImportJobResponse> ROW_MAPPER = AssetImportJobRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetImportJobRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        String jobType,
        String sourceName,
        String status,
        int totalCount,
        int successCount,
        int failureCount,
        String reportJson,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO data_asset_import_jobs (
                job_type, source_name, status, total_count, success_count, failure_count,
                report_json, created_by, updated_by
            ) VALUES (
                :jobType, :sourceName, :status, :totalCount, :successCount, :failureCount,
                :reportJson, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("jobType", jobType)
            .addValue("sourceName", sourceName)
            .addValue("status", status)
            .addValue("totalCount", totalCount)
            .addValue("successCount", successCount)
            .addValue("failureCount", failureCount)
            .addValue("reportJson", reportJson)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public ImportJobResponse requireById(Long jobId) {
        List<ImportJobResponse> rows = jdbcTemplate.query("""
            SELECT id, job_type, source_name, status, total_count, success_count, failure_count,
                   report_json, created_at
            FROM data_asset_import_jobs
            WHERE id = :jobId AND deleted = 0
            """, new MapSqlParameterSource("jobId", jobId), ROW_MAPPER);
        return rows.getFirst();
    }

    public List<ImportJobResponse> latest(int limit) {
        return jdbcTemplate.query("""
            SELECT id, job_type, source_name, status, total_count, success_count, failure_count,
                   report_json, created_at
            FROM data_asset_import_jobs
            WHERE deleted = 0
            ORDER BY id DESC
            LIMIT :limit
            """, new MapSqlParameterSource("limit", Math.max(1, Math.min(limit, 100))), ROW_MAPPER);
    }

    private static ImportJobResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ImportJobResponse(
            rs.getLong("id"),
            rs.getString("job_type"),
            rs.getString("source_name"),
            rs.getString("status"),
            rs.getInt("total_count"),
            rs.getInt("success_count"),
            rs.getInt("failure_count"),
            rs.getString("report_json"),
            createdAt == null ? null : createdAt.toInstant()
        );
    }
}

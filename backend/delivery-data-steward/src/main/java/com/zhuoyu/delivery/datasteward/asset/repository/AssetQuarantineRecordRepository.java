package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.QuarantineRecordResponse;
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
public class AssetQuarantineRecordRepository {

    private static final RowMapper<QuarantineRecordResponse> ROW_MAPPER = AssetQuarantineRecordRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetQuarantineRecordRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Long deleteRequestId, Long projectId, Long fileId,
                        String originalPath, String quarantinePath, Instant quarantineUntil,
                        String requestedByType, Long requestedBy,
                        Long approvedBy, Long executedBy) {
        String sql = """
            INSERT INTO data_asset_quarantine_records (delete_request_id, project_id, file_id,
                original_path, quarantine_path, status, quarantine_until,
                requested_by_type, requested_by, approved_by, executed_by)
            VALUES (:deleteRequestId, :projectId, :fileId,
                :originalPath, :quarantinePath, 'QUARANTINED', :quarantineUntil,
                :requestedByType, :requestedBy, :approvedBy, :executedBy)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("deleteRequestId", deleteRequestId)
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("originalPath", originalPath)
            .addValue("quarantinePath", quarantinePath)
            .addValue("quarantineUntil", Timestamp.from(quarantineUntil))
            .addValue("requestedByType", requestedByType)
            .addValue("requestedBy", requestedBy)
            .addValue("approvedBy", approvedBy)
            .addValue("executedBy", executedBy), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<QuarantineRecordResponse> findById(Long id) {
        List<QuarantineRecordResponse> rows = jdbcTemplate.query("""
            SELECT id, delete_request_id, project_id, file_id, original_path, quarantine_path,
                   status, quarantine_until, requested_by_type, requested_by,
                   approved_by, executed_by, restored_by, permanent_deleted_by,
                   failure_reason, created_at, updated_at
            FROM data_asset_quarantine_records WHERE id = :id
            """, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<QuarantineRecordResponse> listByProject(Long projectId, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource("projectId", projectId);
        String sql = """
            SELECT id, delete_request_id, project_id, file_id, original_path, quarantine_path,
                   status, quarantine_until, requested_by_type, requested_by,
                   approved_by, executed_by, restored_by, permanent_deleted_by,
                   failure_reason, created_at, updated_at
            FROM data_asset_quarantine_records WHERE project_id = :projectId
            """;
        if (status != null && !status.isBlank()) {
            sql += " AND status = :status";
            params.addValue("status", status.trim());
        }
        sql += " ORDER BY created_at DESC LIMIT 200";
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public boolean markRestored(Long id, Long restoredBy, String failureReason) {
        String status = failureReason != null ? "FAILED" : "RESTORED";
        int rows = jdbcTemplate.update("""
            UPDATE data_asset_quarantine_records
            SET status = :status, restored_by = :restoredBy, failure_reason = :failureReason
            WHERE id = :id AND status = 'QUARANTINED'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("restoredBy", restoredBy)
            .addValue("status", status)
            .addValue("failureReason", failureReason));
        return rows > 0;
    }

    public boolean markPermanentDeleted(Long id, Long deletedBy, String failureReason) {
        String status = failureReason != null ? "FAILED" : "PERMANENT_DELETED";
        int rows = jdbcTemplate.update("""
            UPDATE data_asset_quarantine_records
            SET status = :status, permanent_deleted_by = :deletedBy, failure_reason = :failureReason
            WHERE id = :id AND status = 'QUARANTINED'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("deletedBy", deletedBy)
            .addValue("status", status)
            .addValue("failureReason", failureReason));
        return rows > 0;
    }

    private static QuarantineRecordResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp qu = rs.getTimestamp("quarantine_until");
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        return new QuarantineRecordResponse(
            rs.getLong("id"),
            rs.getLong("delete_request_id"),
            rs.getLong("project_id"),
            rs.getLong("file_id"),
            rs.getString("original_path"),
            rs.getString("quarantine_path"),
            rs.getString("status"),
            qu != null ? qu.toInstant() : null,
            rs.getString("requested_by_type"),
            rs.getLong("requested_by"),
            rs.getObject("approved_by", Long.class),
            rs.getObject("executed_by", Long.class),
            rs.getObject("restored_by", Long.class),
            rs.getObject("permanent_deleted_by", Long.class),
            rs.getString("failure_reason"),
            ca != null ? ca.toInstant() : null,
            ua != null ? ua.toInstant() : null
        );
    }
}

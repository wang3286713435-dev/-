package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetDeleteRequestRepository {

    private static final RowMapper<DeleteRequestResponse> ROW_MAPPER = AssetDeleteRequestRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetDeleteRequestRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String requestNo, Long projectId, Long fileId, String deleteType,
                        String reason, String requestedByType, Long requestedBy) {
        String sql = """
            INSERT INTO data_asset_delete_requests (request_no, project_id, file_id, delete_type,
                reason, status, requested_by_type, requested_by)
            VALUES (:requestNo, :projectId, :fileId, :deleteType, :reason, 'PENDING',
                :requestedByType, :requestedBy)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("requestNo", requestNo)
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("deleteType", deleteType)
            .addValue("reason", reason)
            .addValue("requestedByType", requestedByType)
            .addValue("requestedBy", requestedBy), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<DeleteRequestResponse> findById(Long id) {
        List<DeleteRequestResponse> rows = jdbcTemplate.query("""
            SELECT id, request_no, project_id, file_id, delete_type, reason, status,
                   requested_by_type, requested_by, approved_by, approved_at,
                   rejected_by, rejected_at, executed_by, executed_at, failure_reason,
                   created_at, updated_at
            FROM data_asset_delete_requests
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<DeleteRequestResponse> listByProject(Long projectId, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource("projectId", projectId);
        String sql = """
            SELECT id, request_no, project_id, file_id, delete_type, reason, status,
                   requested_by_type, requested_by, approved_by, approved_at,
                   rejected_by, rejected_at, executed_by, executed_at, failure_reason,
                   created_at, updated_at
            FROM data_asset_delete_requests
            WHERE project_id = :projectId AND deleted = 0
            """;
        if (status != null && !status.isBlank()) {
            sql += " AND status = :status";
            params.addValue("status", status.trim());
        }
        sql += " ORDER BY created_at DESC LIMIT 200";
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public boolean approve(Long id, Long approvedBy) {
        int rows = jdbcTemplate.update("""
            UPDATE data_asset_delete_requests
            SET status = 'APPROVED', approved_by = :approvedBy, approved_at = NOW()
            WHERE id = :id AND deleted = 0 AND status = 'PENDING'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("approvedBy", approvedBy));
        return rows > 0;
    }

    public boolean reject(Long id, Long rejectedBy) {
        int rows = jdbcTemplate.update("""
            UPDATE data_asset_delete_requests
            SET status = 'REJECTED', rejected_by = :rejectedBy, rejected_at = NOW()
            WHERE id = :id AND deleted = 0 AND status = 'PENDING'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("rejectedBy", rejectedBy));
        return rows > 0;
    }

    public boolean markExecuted(Long id, Long executedBy, String failureReason) {
        String status = failureReason != null ? "FAILED" : "EXECUTED";
        int rows = jdbcTemplate.update("""
            UPDATE data_asset_delete_requests
            SET status = :status, executed_by = :executedBy, executed_at = NOW(),
                failure_reason = :failureReason
            WHERE id = :id AND deleted = 0 AND status = 'APPROVED'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("executedBy", executedBy)
            .addValue("status", status)
            .addValue("failureReason", failureReason));
        return rows > 0;
    }

    public String generateRequestNo() {
        // Format: DEL-YYYYMMDD-XXXXXX (with random suffix for uniqueness)
        String today = java.time.LocalDate.now().toString().replace("-", "");
        String random = String.format("%06d", (long)(Math.random() * 999999));
        return "DEL-" + today + "-" + random;
    }

    private static DeleteRequestResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp aa = rs.getTimestamp("approved_at");
        Timestamp ra = rs.getTimestamp("rejected_at");
        Timestamp ea = rs.getTimestamp("executed_at");
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        return new DeleteRequestResponse(
            rs.getLong("id"),
            rs.getString("request_no"),
            rs.getLong("project_id"),
            rs.getLong("file_id"),
            rs.getString("delete_type"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getString("requested_by_type"),
            rs.getLong("requested_by"),
            rs.getObject("approved_by", Long.class),
            aa != null ? aa.toInstant() : null,
            rs.getObject("rejected_by", Long.class),
            ra != null ? ra.toInstant() : null,
            rs.getObject("executed_by", Long.class),
            ea != null ? ea.toInstant() : null,
            rs.getString("failure_reason"),
            ca != null ? ca.toInstant() : null,
            ua != null ? ua.toInstant() : null
        );
    }
}

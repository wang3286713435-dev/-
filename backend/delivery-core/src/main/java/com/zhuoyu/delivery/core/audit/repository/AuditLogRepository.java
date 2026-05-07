package com.zhuoyu.delivery.core.audit.repository;

import com.zhuoyu.delivery.core.audit.dto.AuditLogResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {

    private static final RowMapper<AuditLogResponse> ROW_MAPPER = AuditLogRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuditLogRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
        Long projectId,
        String moduleCode,
        String actionCode,
        String targetType,
        String targetId,
        Long operatorId,
        String traceId,
        String detailsJson
    ) {
        String sql = """
            INSERT INTO core_audit_logs (
                project_id, module_code, action_code, target_type, target_id, operator_id, trace_id, details_json
            ) VALUES (
                :projectId, :moduleCode, :actionCode, :targetType, :targetId, :operatorId, :traceId, :detailsJson
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("moduleCode", moduleCode)
            .addValue("actionCode", actionCode)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("operatorId", operatorId)
            .addValue("traceId", traceId)
            .addValue("detailsJson", detailsJson);
        jdbcTemplate.update(sql, parameters);
    }

    public List<AuditLogResponse> findLatest(Long projectId, String moduleCode, int limit) {
        String sql = """
            SELECT id, project_id, module_code, action_code, target_type, target_id,
                   operator_id, trace_id, details_json, created_at
            FROM core_audit_logs
            WHERE (:projectId IS NULL OR project_id = :projectId)
              AND (:moduleCode IS NULL OR module_code = :moduleCode)
            ORDER BY id DESC
            LIMIT :limit
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("moduleCode", moduleCode)
            .addValue("limit", Math.max(1, Math.min(limit, 200))), ROW_MAPPER);
    }

    private static AuditLogResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Long projectId = rs.getObject("project_id") == null ? null : rs.getLong("project_id");
        Long operatorId = rs.getObject("operator_id") == null ? null : rs.getLong("operator_id");
        return new AuditLogResponse(
            rs.getLong("id"),
            projectId,
            rs.getString("module_code"),
            rs.getString("action_code"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            operatorId,
            rs.getString("trace_id"),
            rs.getString("details_json"),
            createdAt == null ? null : createdAt.toInstant()
        );
    }
}

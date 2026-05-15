package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentAnnotationResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AgentAnnotationRepository {

    private static final RowMapper<AgentAnnotationResponse> ROW_MAPPER = AgentAnnotationRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AgentAnnotationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Long apiKeyId, Long projectId, String targetType, Long targetId, String content) {
        String sql = """
            INSERT INTO data_agent_annotations (api_key_id, project_id, target_type, target_id, content)
            VALUES (:apiKeyId, :projectId, :targetType, :targetId, :content)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("apiKeyId", apiKeyId)
            .addValue("projectId", projectId)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("content", content), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<AgentAnnotationResponse> listByProject(Long projectId) {
        return jdbcTemplate.query("""
            SELECT id, api_key_id, project_id, target_type, target_id, content, status, created_at
            FROM data_agent_annotations
            WHERE project_id = :projectId
            ORDER BY created_at DESC LIMIT 200
            """, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    private static AgentAnnotationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp ca = rs.getTimestamp("created_at");
        return new AgentAnnotationResponse(
            rs.getLong("id"),
            rs.getLong("api_key_id"),
            rs.getLong("project_id"),
            rs.getString("target_type"),
            rs.getLong("target_id"),
            rs.getString("content"),
            rs.getString("status"),
            ca != null ? ca.toInstant() : null
        );
    }
}

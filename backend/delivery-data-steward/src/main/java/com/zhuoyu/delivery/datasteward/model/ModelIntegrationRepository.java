package com.zhuoyu.delivery.datasteward.model;

import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ModelIntegrationRepository {

    private static final RowMapper<ModelIntegrationResponse> ROW_MAPPER = ModelIntegrationRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ModelIntegrationRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        String name,
        Long modelFileId,
        String versionNo,
        Integer componentCount,
        String adapterPayloadJson,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO data_model_integrations (
                project_id, name, model_file_id, version_no, component_count,
                adapter_payload_json, created_by, updated_by
            ) VALUES (
                :projectId, :name, :modelFileId, :versionNo, :componentCount,
                :adapterPayloadJson, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("name", name)
            .addValue("modelFileId", modelFileId)
            .addValue("versionNo", versionNo)
            .addValue("componentCount", componentCount)
            .addValue("adapterPayloadJson", adapterPayloadJson)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void publish(Long projectId, Long integrationId, Long operatorId) {
        String sql = """
            UPDATE data_model_integrations
            SET status = 'PUBLISHED',
                published_at = CURRENT_TIMESTAMP,
                published_by = :operatorId,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :integrationId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("integrationId", integrationId)
            .addValue("operatorId", operatorId));
    }

    public List<ModelIntegrationResponse> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, name, model_file_id, version_no, status,
                   component_count, published_at, published_by, adapter_payload_json
            FROM data_model_integrations
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY id DESC
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<ModelIntegrationResponse> findByProjectAndId(Long projectId, Long integrationId) {
        String sql = """
            SELECT id, project_id, name, model_file_id, version_no, status,
                   component_count, published_at, published_by, adapter_payload_json
            FROM data_model_integrations
            WHERE project_id = :projectId AND id = :integrationId AND deleted = 0
            """;
        List<ModelIntegrationResponse> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("integrationId", integrationId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int countPublishedByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_model_integrations
            WHERE project_id = :projectId AND status = 'PUBLISHED' AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    public int countByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_model_integrations
            WHERE project_id = :projectId AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static ModelIntegrationResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp publishedAt = rs.getTimestamp("published_at");
        Long publishedBy = rs.getObject("published_by") == null ? null : rs.getLong("published_by");
        return new ModelIntegrationResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("name"),
            rs.getLong("model_file_id"),
            rs.getString("version_no"),
            rs.getString("status"),
            rs.getInt("component_count"),
            publishedAt == null ? null : publishedAt.toInstant(),
            publishedBy,
            rs.getString("adapter_payload_json")
        );
    }
}

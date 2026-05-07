package com.zhuoyu.delivery.datasteward.object;

import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class ManagedObjectRepository {

    private static final RowMapper<ManagedObjectResponse> ROW_MAPPER = ManagedObjectRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ManagedObjectRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        Long modelIntegrationId,
        Long sectionNodeId,
        String code,
        String name,
        String objectType,
        String externalId,
        String discipline,
        String status,
        String propertiesJson,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO data_managed_objects (
                project_id, model_integration_id, section_node_id, code, name, object_type,
                external_id, discipline, status, properties_json, created_by, updated_by
            ) VALUES (
                :projectId, :modelIntegrationId, :sectionNodeId, :code, :name, :objectType,
                :externalId, :discipline, :status, :propertiesJson, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("modelIntegrationId", modelIntegrationId)
            .addValue("sectionNodeId", sectionNodeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("objectType", objectType)
            .addValue("externalId", externalId)
            .addValue("discipline", discipline)
            .addValue("status", status)
            .addValue("propertiesJson", propertiesJson)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long objectId,
        Long sectionNodeId,
        String code,
        String name,
        String objectType,
        String externalId,
        String discipline,
        String status,
        String propertiesJson,
        Long operatorId
    ) {
        String sql = """
            UPDATE data_managed_objects
            SET section_node_id = :sectionNodeId,
                code = :code,
                name = :name,
                object_type = :objectType,
                external_id = :externalId,
                discipline = :discipline,
                status = :status,
                properties_json = :propertiesJson,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :objectId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("objectId", objectId)
            .addValue("sectionNodeId", sectionNodeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("objectType", objectType)
            .addValue("externalId", externalId)
            .addValue("discipline", discipline)
            .addValue("status", status)
            .addValue("propertiesJson", propertiesJson)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long objectId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE data_managed_objects
            SET deleted = 1, delete_token = id, status = 'DISABLED', updated_by = :operatorId
            WHERE project_id = :projectId AND id = :objectId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("objectId", objectId)
            .addValue("operatorId", operatorId));
    }

    public List<ManagedObjectResponse> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, model_integration_id, section_node_id, code, name, object_type,
                   external_id, discipline, status, properties_json
            FROM data_managed_objects
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY id DESC
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<ManagedObjectResponse> findByProjectAndId(Long projectId, Long objectId) {
        String sql = """
            SELECT id, project_id, model_integration_id, section_node_id, code, name, object_type,
                   external_id, discipline, status, properties_json
            FROM data_managed_objects
            WHERE project_id = :projectId AND id = :objectId AND deleted = 0
            """;
        List<ManagedObjectResponse> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("objectId", objectId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_managed_objects
            WHERE project_id = :projectId
              AND code = :code
              AND deleted = 0
              AND (:excludedId IS NULL OR id <> :excludedId)
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("code", code)
            .addValue("excludedId", excludedId), Integer.class);
        return count != null && count > 0;
    }

    public boolean sectionExists(Long projectId, Long sectionNodeId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM masterdata_section_nodes
            WHERE project_id = :projectId AND id = :sectionNodeId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sectionNodeId", sectionNodeId), Integer.class);
        return count != null && count > 0;
    }

    public int countByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_managed_objects
            WHERE project_id = :projectId AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static ManagedObjectResponse map(ResultSet rs, int rowNum) throws SQLException {
        Long sectionNodeId = rs.getObject("section_node_id") == null ? null : rs.getLong("section_node_id");
        return new ManagedObjectResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("model_integration_id"),
            sectionNodeId,
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("object_type"),
            rs.getString("external_id"),
            rs.getString("discipline"),
            rs.getString("status"),
            rs.getString("properties_json")
        );
    }
}

package com.zhuoyu.delivery.masterdata.deliverable.repository;

import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableDefinition;
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
public class DeliverableDefinitionRepository {

    private static final RowMapper<DeliverableDefinition> ROW_MAPPER = DeliverableDefinitionRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliverableDefinitionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        Long nodeTypeId,
        String code,
        String name,
        String category,
        Boolean required,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_deliverable_definitions (
                project_id, node_type_id, code, name, category, required, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :nodeTypeId, :code, :name, :category, :required, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("nodeTypeId", nodeTypeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("category", category)
            .addValue("required", required != null && required ? 1 : 0)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long definitionId,
        String code,
        String name,
        String category,
        Boolean required,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_deliverable_definitions
            SET code = :code,
                name = :name,
                category = :category,
                required = :required,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :definitionId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("definitionId", definitionId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("category", category)
            .addValue("required", required != null && required ? 1 : 0)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long definitionId, Long operatorId) {
        String sql = """
            UPDATE masterdata_deliverable_definitions
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :definitionId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("definitionId", definitionId)
            .addValue("operatorId", operatorId));
    }

    public List<DeliverableDefinition> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, node_type_id, code, name, category, required, sort_order, status
            FROM masterdata_deliverable_definitions
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<DeliverableDefinition> findByProjectAndId(Long projectId, Long definitionId) {
        String sql = """
            SELECT id, project_id, node_type_id, code, name, category, required, sort_order, status
            FROM masterdata_deliverable_definitions
            WHERE project_id = :projectId AND id = :definitionId AND deleted = 0
            """;
        List<DeliverableDefinition> list = jdbcTemplate.query(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("definitionId", definitionId),
            ROW_MAPPER);
        return list.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_deliverable_definitions
            WHERE project_id = :projectId
              AND code = :code
              AND deleted = 0
              AND (:excludedId IS NULL OR id <> :excludedId)
            """;
        Integer count = jdbcTemplate.queryForObject(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("code", code)
                .addValue("excludedId", excludedId),
            Integer.class);
        return count != null && count > 0;
    }

    public int countByProject(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_deliverable_definitions
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static DeliverableDefinition map(ResultSet rs, int rowNum) throws SQLException {
        return new DeliverableDefinition(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("node_type_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBoolean("required"),
            rs.getInt("sort_order"),
            rs.getString("status")
        );
    }
}

package com.zhuoyu.delivery.masterdata.nodetype.repository;

import com.zhuoyu.delivery.masterdata.nodetype.domain.NodeType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
public class NodeTypeRepository {

    private static final RowMapper<NodeType> ROW_MAPPER = NodeTypeRepository::mapNodeType;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NodeTypeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        String code,
        String name,
        Integer scopeLevel,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_node_types (
                project_id, code, name, scope_level, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :code, :name, :scopeLevel, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("scopeLevel", scopeLevel)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long nodeTypeId,
        String code,
        String name,
        Integer scopeLevel,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_node_types
            SET code = :code,
                name = :name,
                scope_level = :scopeLevel,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :nodeTypeId AND deleted = 0
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeTypeId", nodeTypeId)
                .addValue("code", code)
                .addValue("name", name)
                .addValue("scopeLevel", scopeLevel)
                .addValue("sortOrder", sortOrder)
                .addValue("status", status)
                .addValue("operatorId", operatorId)
        );
    }

    public void lock(Long projectId, Long nodeTypeId, Long operatorId) {
        String sql = """
            UPDATE masterdata_node_types
            SET locked = 1,
                locked_at = COALESCE(locked_at, CURRENT_TIMESTAMP),
                locked_by = COALESCE(locked_by, :operatorId),
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :nodeTypeId AND deleted = 0
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeTypeId", nodeTypeId)
                .addValue("operatorId", operatorId)
        );
    }

    public void lockAll(Long projectId, Long operatorId) {
        String sql = """
            UPDATE masterdata_node_types
            SET locked = 1,
                locked_at = COALESCE(locked_at, CURRENT_TIMESTAMP),
                locked_by = COALESCE(locked_by, :operatorId),
                updated_by = :operatorId
            WHERE project_id = :projectId AND deleted = 0
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("operatorId", operatorId)
        );
    }

    public List<NodeType> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, code, name, scope_level, sort_order, status, locked, locked_at, locked_by
            FROM masterdata_node_types
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY scope_level, sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<NodeType> findByProjectAndId(Long projectId, Long nodeTypeId) {
        String sql = """
            SELECT id, project_id, code, name, scope_level, sort_order, status, locked, locked_at, locked_by
            FROM masterdata_node_types
            WHERE project_id = :projectId AND id = :nodeTypeId AND deleted = 0
            """;
        List<NodeType> nodeTypes = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeTypeId", nodeTypeId),
            ROW_MAPPER
        );
        return nodeTypes.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedNodeTypeId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_node_types
            WHERE project_id = :projectId
              AND code = :code
              AND deleted = 0
              AND (:excludedNodeTypeId IS NULL OR id <> :excludedNodeTypeId)
            """;
        Integer count = jdbcTemplate.queryForObject(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("code", code)
                .addValue("excludedNodeTypeId", excludedNodeTypeId),
            Integer.class
        );
        return count != null && count > 0;
    }

    public int countByProject(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_node_types
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    public int countLockedByProject(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_node_types
            WHERE project_id = :projectId AND deleted = 0 AND locked = 1
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static NodeType mapNodeType(ResultSet resultSet, int rowNum) throws SQLException {
        Timestamp lockedAt = resultSet.getTimestamp("locked_at");
        return new NodeType(
            resultSet.getLong("id"),
            resultSet.getLong("project_id"),
            resultSet.getString("code"),
            resultSet.getString("name"),
            resultSet.getInt("scope_level"),
            resultSet.getInt("sort_order"),
            resultSet.getString("status"),
            resultSet.getBoolean("locked"),
            lockedAt == null ? null : Instant.ofEpochMilli(lockedAt.getTime()),
            resultSet.getObject("locked_by", Long.class)
        );
    }
}

package com.zhuoyu.delivery.masterdata.section.repository;

import com.zhuoyu.delivery.masterdata.section.domain.SectionNode;
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
public class SectionNodeRepository {

    private static final RowMapper<SectionNode> ROW_MAPPER = SectionNodeRepository::mapNode;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SectionNodeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        Long parentId,
        String code,
        String name,
        Integer level,
        String path,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_section_nodes (
                project_id, parent_id, code, name, level, path, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :parentId, :code, :name, :level, :path, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("parentId", parentId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("level", level)
            .addValue("path", path)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updatePath(Long projectId, Long nodeId, String path, Long operatorId) {
        String sql = """
            UPDATE masterdata_section_nodes
            SET path = :path, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :nodeId AND deleted = 0
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeId", nodeId)
                .addValue("path", path)
                .addValue("operatorId", operatorId)
        );
    }

    public void updateNode(
        Long projectId,
        Long nodeId,
        Long parentId,
        String code,
        String name,
        Integer level,
        String path,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_section_nodes
            SET parent_id = :parentId,
                code = :code,
                name = :name,
                level = :level,
                path = :path,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :nodeId AND deleted = 0
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeId", nodeId)
                .addValue("parentId", parentId)
                .addValue("code", code)
                .addValue("name", name)
                .addValue("level", level)
                .addValue("path", path)
                .addValue("sortOrder", sortOrder)
                .addValue("status", status)
                .addValue("operatorId", operatorId)
        );
    }

    public void updateDescendantPaths(
        Long projectId,
        String oldPath,
        String newPath,
        Integer levelDelta,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_section_nodes
            SET path = CONCAT(:newPath, SUBSTRING(path, :suffixStart)),
                level = level + :levelDelta,
                updated_by = :operatorId
            WHERE project_id = :projectId AND deleted = 0 AND path LIKE :childPathPattern
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("oldPath", oldPath)
                .addValue("newPath", newPath)
                .addValue("suffixStart", oldPath.length() + 1)
                .addValue("levelDelta", levelDelta)
                .addValue("operatorId", operatorId)
                .addValue("childPathPattern", oldPath + "/%")
        );
    }

    public void markDeletedByPath(Long projectId, String path, Long operatorId) {
        String sql = """
            UPDATE masterdata_section_nodes
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId
              AND deleted = 0
              AND (path = :path OR path LIKE :childPathPattern)
            """;
        jdbcTemplate.update(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("path", path)
                .addValue("childPathPattern", path + "/%")
                .addValue("operatorId", operatorId)
        );
    }

    public List<SectionNode> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, parent_id, code, name, level, path, sort_order, status
            FROM masterdata_section_nodes
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY level, sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<SectionNode> findByProjectAndId(Long projectId, Long nodeId) {
        String sql = """
            SELECT id, project_id, parent_id, code, name, level, path, sort_order, status
            FROM masterdata_section_nodes
            WHERE project_id = :projectId AND id = :nodeId AND deleted = 0
            """;
        List<SectionNode> nodes = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("nodeId", nodeId),
            ROW_MAPPER
        );
        return nodes.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedNodeId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_section_nodes
            WHERE project_id = :projectId
              AND code = :code
              AND deleted = 0
              AND (:excludedNodeId IS NULL OR id <> :excludedNodeId)
            """;
        Integer count = jdbcTemplate.queryForObject(
            sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("code", code)
                .addValue("excludedNodeId", excludedNodeId),
            Integer.class
        );
        return count != null && count > 0;
    }

    public boolean hasAnyNode(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_section_nodes
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count != null && count > 0;
    }

    public int countByProject(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_section_nodes
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static SectionNode mapNode(ResultSet resultSet, int rowNum) throws SQLException {
        return new SectionNode(
            resultSet.getLong("id"),
            resultSet.getLong("project_id"),
            resultSet.getObject("parent_id", Long.class),
            resultSet.getString("code"),
            resultSet.getString("name"),
            resultSet.getInt("level"),
            resultSet.getString("path"),
            resultSet.getInt("sort_order"),
            resultSet.getString("status")
        );
    }
}

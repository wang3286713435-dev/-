package com.zhuoyu.delivery.masterdata.template.repository;

import com.zhuoyu.delivery.masterdata.template.domain.DirectoryTemplate;
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
public class DirectoryTemplateRepository {

    private static final RowMapper<DirectoryTemplate> ROW_MAPPER = DirectoryTemplateRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DirectoryTemplateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        String templateType,
        String name,
        String rootNodeJson,
        String sourceType,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_directory_templates (
                project_id, template_type, name, root_node_json, source_type, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :templateType, :name, :rootNodeJson, :sourceType, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("templateType", templateType)
            .addValue("name", name)
            .addValue("rootNodeJson", rootNodeJson)
            .addValue("sourceType", sourceType)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long templateId,
        String templateType,
        String name,
        String rootNodeJson,
        String sourceType,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_directory_templates
            SET template_type = :templateType,
                name = :name,
                root_node_json = :rootNodeJson,
                source_type = :sourceType,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :templateId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("templateId", templateId)
            .addValue("templateType", templateType)
            .addValue("name", name)
            .addValue("rootNodeJson", rootNodeJson)
            .addValue("sourceType", sourceType)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long templateId, Long operatorId) {
        String sql = """
            UPDATE masterdata_directory_templates
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :templateId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("templateId", templateId)
            .addValue("operatorId", operatorId));
    }

    public List<DirectoryTemplate> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, template_type, name, root_node_json, source_type, sort_order, status
            FROM masterdata_directory_templates
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public Optional<DirectoryTemplate> findByProjectAndId(Long projectId, Long templateId) {
        String sql = """
            SELECT id, project_id, template_type, name, root_node_json, source_type, sort_order, status
            FROM masterdata_directory_templates
            WHERE project_id = :projectId AND id = :templateId AND deleted = 0
            """;
        List<DirectoryTemplate> list = jdbcTemplate.query(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("templateId", templateId),
            ROW_MAPPER);
        return list.stream().findFirst();
    }

    public boolean existsName(Long projectId, String name, Long excludedId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_directory_templates
            WHERE project_id = :projectId
              AND name = :name
              AND deleted = 0
              AND (:excludedId IS NULL OR id <> :excludedId)
            """;
        Integer count = jdbcTemplate.queryForObject(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("name", name)
                .addValue("excludedId", excludedId),
            Integer.class);
        return count != null && count > 0;
    }

    public int countByProject(Long projectId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_directory_templates
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static DirectoryTemplate map(ResultSet rs, int rowNum) throws SQLException {
        return new DirectoryTemplate(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("template_type"),
            rs.getString("name"),
            rs.getString("root_node_json"),
            rs.getString("source_type"),
            rs.getInt("sort_order"),
            rs.getString("status")
        );
    }
}

package com.zhuoyu.delivery.masterdata.deliverable.repository;

import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableAttribute;
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
public class DeliverableAttributeRepository {

    private static final RowMapper<DeliverableAttribute> ROW_MAPPER = DeliverableAttributeRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliverableAttributeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        Long deliverableTypeId,
        String code,
        String name,
        String valueType,
        String unit,
        Boolean required,
        String exampleValue,
        String enumOptions,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_deliverable_attributes (
                project_id, deliverable_type_id, code, name, value_type, unit, required, example_value, enum_options, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :deliverableTypeId, :code, :name, :valueType, :unit, :required, :exampleValue, :enumOptions, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("deliverableTypeId", deliverableTypeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("valueType", valueType)
            .addValue("unit", unit)
            .addValue("required", required != null && required ? 1 : 0)
            .addValue("exampleValue", exampleValue)
            .addValue("enumOptions", enumOptions)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long attributeId,
        String code,
        String name,
        String valueType,
        String unit,
        Boolean required,
        String exampleValue,
        String enumOptions,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_deliverable_attributes
            SET code = :code,
                name = :name,
                value_type = :valueType,
                unit = :unit,
                required = :required,
                example_value = :exampleValue,
                enum_options = :enumOptions,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :attributeId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("attributeId", attributeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("valueType", valueType)
            .addValue("unit", unit)
            .addValue("required", required != null && required ? 1 : 0)
            .addValue("exampleValue", exampleValue)
            .addValue("enumOptions", enumOptions)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long attributeId, Long operatorId) {
        String sql = """
            UPDATE masterdata_deliverable_attributes
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :attributeId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("attributeId", attributeId)
            .addValue("operatorId", operatorId));
    }

    public void markDeletedByTypeId(Long projectId, Long typeId, Long operatorId) {
        String sql = """
            UPDATE masterdata_deliverable_attributes
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND deliverable_type_id = :typeId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("typeId", typeId)
            .addValue("operatorId", operatorId));
    }

    public List<DeliverableAttribute> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, deliverable_type_id, code, name, value_type, unit, required, example_value, enum_options, sort_order, status
            FROM masterdata_deliverable_attributes
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public List<DeliverableAttribute> findByTypeId(Long projectId, Long typeId) {
        String sql = """
            SELECT id, project_id, deliverable_type_id, code, name, value_type, unit, required, example_value, enum_options, sort_order, status
            FROM masterdata_deliverable_attributes
            WHERE project_id = :projectId AND deliverable_type_id = :typeId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("typeId", typeId), ROW_MAPPER);
    }

    public Optional<DeliverableAttribute> findByProjectAndId(Long projectId, Long attributeId) {
        String sql = """
            SELECT id, project_id, deliverable_type_id, code, name, value_type, unit, required, example_value, enum_options, sort_order, status
            FROM masterdata_deliverable_attributes
            WHERE project_id = :projectId AND id = :attributeId AND deleted = 0
            """;
        List<DeliverableAttribute> list = jdbcTemplate.query(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("attributeId", attributeId),
            ROW_MAPPER);
        return list.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_deliverable_attributes
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
            FROM masterdata_deliverable_attributes
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static DeliverableAttribute map(ResultSet rs, int rowNum) throws SQLException {
        return new DeliverableAttribute(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("deliverable_type_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("value_type"),
            rs.getString("unit"),
            rs.getBoolean("required"),
            rs.getString("example_value"),
            rs.getString("enum_options"),
            rs.getInt("sort_order"),
            rs.getString("status")
        );
    }
}

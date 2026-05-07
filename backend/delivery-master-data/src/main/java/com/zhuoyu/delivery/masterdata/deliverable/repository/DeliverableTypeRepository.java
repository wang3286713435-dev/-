package com.zhuoyu.delivery.masterdata.deliverable.repository;

import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableType;
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
public class DeliverableTypeRepository {

    private static final RowMapper<DeliverableType> ROW_MAPPER = DeliverableTypeRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliverableTypeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        Long deliverableDefinitionId,
        String code,
        String name,
        String fileKind,
        String bindingStrategy,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO masterdata_deliverable_types (
                project_id, deliverable_definition_id, code, name, file_kind, binding_strategy, sort_order, status, created_by, updated_by
            ) VALUES (
                :projectId, :deliverableDefinitionId, :code, :name, :fileKind, :bindingStrategy, :sortOrder, :status, :operatorId, :operatorId
            )
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("deliverableDefinitionId", deliverableDefinitionId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("fileKind", fileKind)
            .addValue("bindingStrategy", bindingStrategy)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, parameters, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void update(
        Long projectId,
        Long typeId,
        String code,
        String name,
        String fileKind,
        String bindingStrategy,
        Integer sortOrder,
        String status,
        Long operatorId
    ) {
        String sql = """
            UPDATE masterdata_deliverable_types
            SET code = :code,
                name = :name,
                file_kind = :fileKind,
                binding_strategy = :bindingStrategy,
                sort_order = :sortOrder,
                status = :status,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :typeId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("typeId", typeId)
            .addValue("code", code)
            .addValue("name", name)
            .addValue("fileKind", fileKind)
            .addValue("bindingStrategy", bindingStrategy)
            .addValue("sortOrder", sortOrder)
            .addValue("status", status)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long typeId, Long operatorId) {
        String sql = """
            UPDATE masterdata_deliverable_types
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :typeId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("typeId", typeId)
            .addValue("operatorId", operatorId));
    }

    public void markDeletedByDefinitionId(Long projectId, Long definitionId, Long operatorId) {
        String sql = """
            UPDATE masterdata_deliverable_types
            SET status = 'DISABLED', deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND deliverable_definition_id = :definitionId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("definitionId", definitionId)
            .addValue("operatorId", operatorId));
    }

    public List<DeliverableType> findByProject(Long projectId) {
        String sql = """
            SELECT id, project_id, deliverable_definition_id, code, name, file_kind, binding_strategy, sort_order, status
            FROM masterdata_deliverable_types
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public List<DeliverableType> findByDefinitionId(Long projectId, Long definitionId) {
        String sql = """
            SELECT id, project_id, deliverable_definition_id, code, name, file_kind, binding_strategy, sort_order, status
            FROM masterdata_deliverable_types
            WHERE project_id = :projectId AND deliverable_definition_id = :definitionId AND deleted = 0
            ORDER BY sort_order, id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("definitionId", definitionId), ROW_MAPPER);
    }

    public Optional<DeliverableType> findByProjectAndId(Long projectId, Long typeId) {
        String sql = """
            SELECT id, project_id, deliverable_definition_id, code, name, file_kind, binding_strategy, sort_order, status
            FROM masterdata_deliverable_types
            WHERE project_id = :projectId AND id = :typeId AND deleted = 0
            """;
        List<DeliverableType> list = jdbcTemplate.query(sql,
            new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("typeId", typeId),
            ROW_MAPPER);
        return list.stream().findFirst();
    }

    public boolean existsCode(Long projectId, String code, Long excludedId) {
        String sql = """
            SELECT COUNT(1)
            FROM masterdata_deliverable_types
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
            FROM masterdata_deliverable_types
            WHERE project_id = :projectId AND deleted = 0
            """;
        Integer count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private static DeliverableType map(ResultSet rs, int rowNum) throws SQLException {
        return new DeliverableType(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("deliverable_definition_id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("file_kind"),
            rs.getString("binding_strategy"),
            rs.getInt("sort_order"),
            rs.getString("status")
        );
    }
}

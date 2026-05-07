package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
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
public class DeliveryBindingRepository {

    private static final RowMapper<DeliveryBindingResponse> BINDING_ROW_MAPPER = DeliveryBindingRepository::mapBinding;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliveryBindingRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        String viewType,
        Long sectionNodeId,
        Long managedObjectId,
        Long deliverableTypeId,
        Long fileResourceId,
        String bindingStatus,
        String reviewStatus,
        Integer sortOrder,
        String remark,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO work_delivery_bindings (
                project_id, view_type, section_node_id, managed_object_id, deliverable_type_id,
                file_resource_id, binding_status, review_status, sort_order, remark, created_by, updated_by
            ) VALUES (
                :projectId, :viewType, :sectionNodeId, :managedObjectId, :deliverableTypeId,
                :fileResourceId, :bindingStatus, :reviewStatus, :sortOrder, :remark, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType)
            .addValue("sectionNodeId", sectionNodeId)
            .addValue("managedObjectId", managedObjectId)
            .addValue("deliverableTypeId", deliverableTypeId)
            .addValue("fileResourceId", fileResourceId)
            .addValue("bindingStatus", bindingStatus)
            .addValue("reviewStatus", reviewStatus)
            .addValue("sortOrder", sortOrder)
            .addValue("remark", remark)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void markDeleted(Long projectId, Long bindingId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE work_delivery_bindings
            SET deleted = 1, delete_token = id, binding_status = 'UNBOUND', updated_by = :operatorId
            WHERE project_id = :projectId AND id = :bindingId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId)
            .addValue("operatorId", operatorId));
    }

    public Optional<DeliveryBindingResponse> findByProjectAndId(Long projectId, Long bindingId) {
        List<DeliveryBindingResponse> rows = jdbcTemplate.query(bindingSql("""
            WHERE b.project_id = :projectId AND b.id = :bindingId AND b.deleted = 0
            """), new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId), BINDING_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<DeliveryBindingResponse> findByProjectAndViewType(Long projectId, String viewType) {
        return jdbcTemplate.query(bindingSql("""
            WHERE b.project_id = :projectId AND b.view_type = :viewType AND b.deleted = 0
            ORDER BY b.sort_order, b.id DESC
            """), new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType), BINDING_ROW_MAPPER);
    }

    public boolean deliverableTypeExists(Long projectId, Long deliverableTypeId, String expectedFileKind) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM masterdata_deliverable_types
            WHERE project_id = :projectId
              AND id = :deliverableTypeId
              AND file_kind = :expectedFileKind
              AND deleted = 0
              AND status = 'ACTIVE'
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("deliverableTypeId", deliverableTypeId)
            .addValue("expectedFileKind", expectedFileKind), Integer.class);
        return count != null && count > 0;
    }

    public boolean fileExists(Long projectId, Long fileResourceId, String expectedFileKind) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId
              AND id = :fileResourceId
              AND file_kind = :expectedFileKind
              AND process_status = 'PROCESSED'
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileResourceId", fileResourceId)
            .addValue("expectedFileKind", expectedFileKind), Integer.class);
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

    public boolean objectExists(Long projectId, Long managedObjectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_managed_objects
            WHERE project_id = :projectId AND id = :managedObjectId AND deleted = 0 AND status = 'ACTIVE'
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("managedObjectId", managedObjectId), Integer.class);
        return count != null && count > 0;
    }

    public DashboardSummaryResponse dashboardSummary(Long projectId) {
        return new DashboardSummaryResponse(
            projectId,
            count("masterdata_section_nodes", projectId, null),
            count("masterdata_deliverable_definitions", projectId, null),
            count("data_file_resources", projectId, null),
            countFilesByKind(projectId, "DOCUMENT"),
            countFilesByKind(projectId, "DRAWING"),
            countFilesByKind(projectId, "MODEL"),
            count("data_model_integrations", projectId, null),
            count("data_model_integrations", projectId, "status = 'PUBLISHED'"),
            count("data_managed_objects", projectId, null),
            countBindings(projectId, "DOCUMENT"),
            countBindings(projectId, "DRAWING")
        );
    }

    private int count(String tableName, Long projectId, String extraWhere) {
        String extra = extraWhere == null ? "" : " AND " + extraWhere;
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM %s
            WHERE project_id = :projectId AND deleted = 0%s
            """.formatted(tableName, extra), new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private int countFilesByKind(Long projectId, String fileKind) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId AND file_kind = :fileKind AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind), Integer.class);
        return count == null ? 0 : count;
    }

    private int countBindings(Long projectId, String viewType) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM work_delivery_bindings
            WHERE project_id = :projectId AND view_type = :viewType AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType), Integer.class);
        return count == null ? 0 : count;
    }

    private static String bindingSql(String whereClause) {
        return """
            SELECT b.id,
                   b.project_id,
                   b.view_type,
                   b.section_node_id,
                   s.name AS section_node_name,
                   b.managed_object_id,
                   o.name AS managed_object_name,
                   b.deliverable_type_id,
                   dt.name AS deliverable_type_name,
                   dd.name AS deliverable_definition_name,
                   b.file_resource_id,
                   f.original_name AS file_name,
                   f.file_kind,
                   f.version_no,
                   f.process_status,
                   b.binding_status,
                   b.review_status,
                   b.sort_order,
                   b.remark
            FROM work_delivery_bindings b
            JOIN masterdata_deliverable_types dt
              ON dt.id = b.deliverable_type_id AND dt.project_id = b.project_id AND dt.deleted = 0
            JOIN masterdata_deliverable_definitions dd
              ON dd.id = dt.deliverable_definition_id AND dd.project_id = b.project_id AND dd.deleted = 0
            JOIN data_file_resources f
              ON f.id = b.file_resource_id AND f.project_id = b.project_id AND f.deleted = 0
            LEFT JOIN masterdata_section_nodes s
              ON s.id = b.section_node_id AND s.project_id = b.project_id AND s.deleted = 0
            LEFT JOIN data_managed_objects o
              ON o.id = b.managed_object_id AND o.project_id = b.project_id AND o.deleted = 0
            %s
            """.formatted(whereClause);
    }

    private static DeliveryBindingResponse mapBinding(ResultSet rs, int rowNum) throws SQLException {
        Long sectionNodeId = rs.getObject("section_node_id") == null ? null : rs.getLong("section_node_id");
        Long managedObjectId = rs.getObject("managed_object_id") == null ? null : rs.getLong("managed_object_id");
        return new DeliveryBindingResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("view_type"),
            sectionNodeId,
            rs.getString("section_node_name"),
            managedObjectId,
            rs.getString("managed_object_name"),
            rs.getLong("deliverable_type_id"),
            rs.getString("deliverable_type_name"),
            rs.getString("deliverable_definition_name"),
            rs.getLong("file_resource_id"),
            rs.getString("file_name"),
            rs.getString("file_kind"),
            rs.getString("version_no"),
            rs.getString("process_status"),
            rs.getString("binding_status"),
            rs.getString("review_status"),
            rs.getInt("sort_order"),
            rs.getString("remark")
        );
    }
}

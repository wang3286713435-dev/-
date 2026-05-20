package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageSummaryRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ExportPrecheckRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceCandidateFile;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ReviewRecordResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // ---- completeness queries ----

    public List<Long> activeSectionNodeIds(Long projectId) {
        return jdbcTemplate.queryForList("""
            SELECT id FROM masterdata_section_nodes
            WHERE project_id = :projectId AND deleted = 0 AND status = 'ACTIVE'
            ORDER BY sort_order, id
            """, new MapSqlParameterSource("projectId", projectId), Long.class);
    }

    public List<Long> activeObjectIds(Long projectId) {
        return jdbcTemplate.queryForList("""
            SELECT id FROM data_managed_objects
            WHERE project_id = :projectId AND deleted = 0 AND status = 'ACTIVE'
            ORDER BY id
            """, new MapSqlParameterSource("projectId", projectId), Long.class);
    }

    public int countOpenRectifications(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM work_rectifications
            WHERE project_id = :projectId
              AND deleted = 0
              AND status IN ('OPEN', 'REOPENED')
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    public List<AgentGovernanceCandidateFile> findAgentCandidateFiles(Long projectId, String fileKind, int limit) {
        return jdbcTemplate.query("""
            SELECT id,
                   original_name,
                   file_kind,
                   LOWER(SUBSTRING_INDEX(original_name, '.', -1)) AS file_ext,
                   version_no,
                   process_status,
                   business_tag,
                   CASE WHEN checksum IS NULL OR checksum = '' THEN 0 ELSE 1 END AS checksum_present
            FROM data_file_resources
            WHERE project_id = :projectId
              AND deleted = 0
              AND file_kind = :fileKind
            ORDER BY
              CASE WHEN process_status = 'PROCESSED' THEN 0 ELSE 1 END,
              id DESC
            LIMIT :limit
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind)
            .addValue("limit", Math.max(1, limit)),
            (rs, rowNum) -> new AgentGovernanceCandidateFile(
                rs.getLong("id"),
                rs.getString("original_name"),
                rs.getString("file_kind"),
                rs.getString("file_ext"),
                rs.getString("version_no"),
                rs.getString("process_status"),
                rs.getString("business_tag"),
                rs.getInt("checksum_present") == 1
            ));
    }

    public List<String> findBoundDeliverableTypeFileKeys(Long projectId, String viewType) {
        return jdbcTemplate.queryForList("""
            SELECT CONCAT(deliverable_type_id, ':', file_resource_id)
            FROM work_delivery_bindings
            WHERE project_id = :projectId
              AND view_type = :viewType
              AND binding_status = 'BOUND'
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType), String.class);
    }

    public List<DeliveryCompletenessRow> findRequiredDeliverables(
        Long projectId, String fileKind, String targetType, String viewType
    ) {
        boolean isSection = "SECTION".equals(targetType);
        String bindingStrategyFilter = isSection
            ? "AND dt.binding_strategy = 'SECTION_NODE'"
            : "AND dt.binding_strategy IN ('MANAGED_OBJECT', 'OBJECT')";

        String targetJoin;
        String targetIdCol;
        String targetCodeCol;
        String targetNameCol;
        if (isSection) {
            targetJoin = """
                CROSS JOIN masterdata_section_nodes sn
                WHERE sn.project_id = :projectId AND sn.deleted = 0 AND sn.status = 'ACTIVE'
                """;
            targetIdCol = "sn.id";
            targetCodeCol = "sn.code";
            targetNameCol = "sn.name";
        } else {
            targetJoin = """
                CROSS JOIN data_managed_objects mo
                WHERE mo.project_id = :projectId AND mo.deleted = 0 AND mo.status = 'ACTIVE'
                """;
            targetIdCol = "mo.id";
            targetCodeCol = "mo.code";
            targetNameCol = "mo.name";
        }

        String sql = """
            SELECT
                :targetType AS target_type,
                %s AS target_id,
                %s AS target_code,
                %s AS target_name,
                dd.id AS dd_id,
                dd.code AS dd_code,
                dd.name AS dd_name,
                dt.id AS dt_id,
                dt.code AS dt_code,
                dt.name AS dt_name,
                dt.file_kind
            FROM masterdata_deliverable_definitions dd
            JOIN masterdata_deliverable_types dt
              ON dt.deliverable_definition_id = dd.id
             AND dt.project_id = dd.project_id
             AND dt.deleted = 0
             AND dt.status = 'ACTIVE'
             AND dt.file_kind = :fileKind
             %s
            %s
            AND dd.project_id = :projectId
            AND dd.deleted = 0
            AND dd.status = 'ACTIVE'
            AND dd.required = 1
            ORDER BY target_code, dd.sort_order, dt.sort_order
            """.formatted(targetIdCol, targetCodeCol, targetNameCol, bindingStrategyFilter, targetJoin);

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind)
            .addValue("viewType", viewType)
            .addValue("targetType", targetType), (rs, rowNum) -> new DeliveryCompletenessRow(
                rs.getString("target_type"),
                rs.getLong("target_id"),
                rs.getString("target_code"),
                rs.getString("target_name"),
                rs.getLong("dd_id"),
                rs.getString("dd_code"),
                rs.getString("dd_name"),
                rs.getLong("dt_id"),
                rs.getString("dt_code"),
                rs.getString("dt_name"),
                rs.getString("file_kind"),
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null
            ));
    }

    public List<DeliveryCompletenessRow> findCompletedBindings(
        Long projectId, String viewType, String targetType
    ) {
        boolean isSection = "SECTION".equals(targetType);
        String targetCol = isSection ? "b.section_node_id" : "b.managed_object_id";

        String sql = """
            SELECT
                :targetType AS target_type,
                %s AS target_id,
                b.id AS binding_id,
                b.deliverable_type_id,
                b.file_resource_id,
                f.original_name AS file_name,
                f.file_kind,
                f.version_no,
                b.review_status
            FROM work_delivery_bindings b
            JOIN data_file_resources f
              ON f.id = b.file_resource_id
             AND f.project_id = b.project_id
             AND f.deleted = 0
            WHERE b.project_id = :projectId
              AND b.view_type = :viewType
              AND b.deleted = 0
              AND b.binding_status = 'BOUND'
              AND %s IS NOT NULL
            ORDER BY b.sort_order, b.id DESC
            """.formatted(targetCol, targetCol);

        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType)
            .addValue("targetType", targetType), (rs, rowNum) -> {
            Long bindingId = rs.getLong("binding_id");
            Long targetId = rs.getLong("target_id");
            Long deliverableTypeId = rs.getLong("deliverable_type_id");
            Long fileResourceId = rs.getLong("file_resource_id");
            String fileName = rs.getString("file_name");
            String fileKind = rs.getString("file_kind");
            String versionNo = rs.getString("version_no");
            String reviewStatus = rs.getString("review_status");
            return new DeliveryCompletenessRow(
                rs.getString("target_type"),
                targetId,
                null, null,
                null, null, null,
                deliverableTypeId,
                null, null,
                fileKind,
                true, true,
                bindingId,
                fileResourceId,
                fileName,
                versionNo,
                reviewStatus,
                null
            );
        });
    }

    public List<String> readinessIssues(
        Long projectId,
        boolean hasSectionTree, boolean hasNodeTypes, boolean nodeTypesLocked,
        boolean hasDeliverableDefinitions, boolean hasDeliverableTypesForKind,
        boolean hasDirectoryTemplates
    ) {
        List<String> issues = new ArrayList<>();
        if (!hasSectionTree) {
            issues.add("缺少工程部位树");
        }
        if (!hasNodeTypes) {
            issues.add("缺少节点类型定义");
        } else if (!nodeTypesLocked) {
            issues.add("节点类型尚未全部锁定");
        }
        if (!hasDeliverableDefinitions) {
            issues.add("缺少交付物标准定义");
        }
        if (!hasDeliverableTypesForKind) {
            issues.add("缺少当前视图类型的交付物类型");
        }
        if (!hasDirectoryTemplates) {
            issues.add("缺少目录模板");
        }
        return issues;
    }

    public boolean hasDeliverableTypesForKind(Long projectId, String fileKind) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM masterdata_deliverable_types dt
            JOIN masterdata_deliverable_definitions dd
              ON dd.id = dt.deliverable_definition_id
             AND dd.project_id = dt.project_id
             AND dd.deleted = 0
             AND dd.status = 'ACTIVE'
             AND dd.required = 1
            WHERE dt.project_id = :projectId
              AND dt.file_kind = :fileKind
              AND dt.deleted = 0
              AND dt.status = 'ACTIVE'
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind), Integer.class);
        return count != null && count > 0;
    }

    // ---- review ----

    public void updateReviewStatus(Long projectId, Long bindingId, String reviewStatus) {
        jdbcTemplate.update("""
            UPDATE work_delivery_bindings
            SET review_status = :reviewStatus
            WHERE project_id = :projectId AND id = :bindingId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId)
            .addValue("reviewStatus", reviewStatus));
    }

    public void insertReviewRecord(Long projectId, Long bindingId, String action, String comment, Long reviewerUserId) {
        jdbcTemplate.update("""
            INSERT INTO work_review_records (project_id, binding_id, action, comment, reviewer_user_id)
            VALUES (:projectId, :bindingId, :action, :comment, :reviewerUserId)
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId)
            .addValue("action", action)
            .addValue("comment", comment)
            .addValue("reviewerUserId", reviewerUserId));
    }

    public List<ReviewRecordResponse> findReviewRecords(Long projectId, Long bindingId) {
        return jdbcTemplate.query("""
            SELECT id, binding_id, action, comment, reviewer_user_id, created_at
            FROM work_review_records
            WHERE project_id = :projectId AND binding_id = :bindingId
            ORDER BY created_at DESC
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId),
            (rs, rowNum) -> new ReviewRecordResponse(
                rs.getLong("id"),
                rs.getLong("binding_id"),
                rs.getString("action"),
                rs.getString("comment"),
                rs.getLong("reviewer_user_id"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ));
    }

    // ---- rectification ----

    public Long insertRectification(Long projectId, Long bindingId, String title, String reason, Long operatorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO work_rectifications (
                project_id, source_type, source_id, binding_id, title, reason, status, severity, created_by, updated_by
            ) VALUES (
                :projectId, 'DELIVERY_BINDING', :bindingId, :bindingId, :title, :reason, 'OPEN', 'NORMAL', :operatorId, :operatorId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("bindingId", bindingId)
            .addValue("title", title)
            .addValue("reason", reason)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<RectificationResponse> findRectificationsByProject(Long projectId, String statusFilter) {
        String whereStatus = (statusFilter != null && !statusFilter.isBlank())
            ? "AND r.status = :statusFilter" : "";
        var params = new MapSqlParameterSource("projectId", projectId);
        if (statusFilter != null && !statusFilter.isBlank()) {
            params.addValue("statusFilter", statusFilter.toUpperCase());
        }
        return jdbcTemplate.query("""
            SELECT r.id, r.project_id, r.source_type, r.source_id, r.binding_id,
                   r.title, r.description, r.reason, r.status, r.severity,
                   r.assignee_user_id, r.resolution_note, r.due_date,
                   r.resolved_at, r.closed_at, r.created_by, r.updated_by,
                   r.created_at, r.updated_at,
                   b.view_type AS binding_view_type,
                   f.original_name AS binding_file_name,
                   dt.name AS binding_deliverable_type_name,
                   s.name AS binding_section_node_name
            FROM work_rectifications r
            JOIN work_delivery_bindings b ON b.id = r.binding_id AND b.project_id = r.project_id AND b.deleted = 0
            LEFT JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            LEFT JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.deleted = 0
            LEFT JOIN masterdata_section_nodes s ON s.id = b.section_node_id AND s.deleted = 0
            WHERE r.project_id = :projectId AND r.deleted = 0
            %s
            ORDER BY r.created_at DESC
            """.formatted(whereStatus), params, (rs, rowNum) -> mapRectification(rs));
    }

    public Optional<RectificationResponse> findRectificationById(Long projectId, Long rectificationId) {
        var rows = jdbcTemplate.query("""
            SELECT r.id, r.project_id, r.source_type, r.source_id, r.binding_id,
                   r.title, r.description, r.reason, r.status, r.severity,
                   r.assignee_user_id, r.resolution_note, r.due_date,
                   r.resolved_at, r.closed_at, r.created_by, r.updated_by,
                   r.created_at, r.updated_at,
                   b.view_type AS binding_view_type,
                   f.original_name AS binding_file_name,
                   dt.name AS binding_deliverable_type_name,
                   s.name AS binding_section_node_name
            FROM work_rectifications r
            JOIN work_delivery_bindings b ON b.id = r.binding_id AND b.project_id = r.project_id AND b.deleted = 0
            LEFT JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            LEFT JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.deleted = 0
            LEFT JOIN masterdata_section_nodes s ON s.id = b.section_node_id AND s.deleted = 0
            WHERE r.project_id = :projectId AND r.id = :rectificationId AND r.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("rectificationId", rectificationId),
            (rs, rowNum) -> mapRectification(rs));
        return rows.stream().findFirst();
    }

    public void updateRectification(Long projectId, Long rectificationId, String title, String description,
                                     String severity, Long assigneeUserId, LocalDate dueDate, Long updatedBy) {
        jdbcTemplate.update("""
            UPDATE work_rectifications SET
                title = COALESCE(:title, title),
                description = COALESCE(:description, description),
                severity = COALESCE(:severity, severity),
                assignee_user_id = COALESCE(:assigneeUserId, assignee_user_id),
                due_date = COALESCE(:dueDate, due_date),
                updated_by = :updatedBy
            WHERE project_id = :projectId AND id = :rectificationId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("rectificationId", rectificationId)
            .addValue("title", title)
            .addValue("description", description)
            .addValue("severity", severity)
            .addValue("assigneeUserId", assigneeUserId)
            .addValue("dueDate", dueDate)
            .addValue("updatedBy", updatedBy));
    }

    public void updateRectificationStatus(Long projectId, Long rectificationId, String status,
                                           String resolutionNote, Long updatedBy) {
        String resolvedSet = "RESOLVED".equals(status) ? ", resolved_at = NOW()" : "";
        String closedSet = "CLOSED".equals(status) ? ", closed_at = NOW()" : "";
        String reopenSet = "REOPENED".equals(status) ? ", resolved_at = NULL, closed_at = NULL" : "";
        jdbcTemplate.update(("""
            UPDATE work_rectifications SET
                status = :status,
                resolution_note = COALESCE(:resolutionNote, resolution_note)%s%s%s,
                updated_by = :updatedBy
            WHERE project_id = :projectId AND id = :rectificationId AND deleted = 0
            """).formatted(resolvedSet, closedSet, reopenSet),
            new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("rectificationId", rectificationId)
            .addValue("status", status)
            .addValue("resolutionNote", resolutionNote)
            .addValue("updatedBy", updatedBy));
    }

    // ---- CSV exports ----

    public List<Map<String, Object>> findCompletenessCsvData(Long projectId) {
        return jdbcTemplate.queryForList("""
            SELECT b.project_id, b.view_type, b.id AS binding_id,
                   f.original_name AS file_name, b.review_status, b.binding_status,
                   dt.name AS deliverable_type_name,
                   COALESCE(s.name, o.name) AS target_name
            FROM work_delivery_bindings b
            JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.deleted = 0
            LEFT JOIN masterdata_section_nodes s ON s.id = b.section_node_id AND s.deleted = 0
            LEFT JOIN data_managed_objects o ON o.id = b.managed_object_id AND o.deleted = 0
            WHERE b.project_id = :projectId AND b.deleted = 0
            ORDER BY b.view_type, b.id
            """, new MapSqlParameterSource("projectId", projectId));
    }

    public List<Map<String, Object>> findReviewSummaryCsvData(Long projectId) {
        return jdbcTemplate.queryForList("""
            SELECT b.id AS binding_id, b.view_type, b.review_status,
                   f.original_name AS file_name,
                   dt.name AS deliverable_type_name,
                   COUNT(rr.id) AS review_count,
                   MAX(rr.created_at) AS last_review_at
            FROM work_delivery_bindings b
            JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.deleted = 0
            LEFT JOIN work_review_records rr ON rr.binding_id = b.id
            WHERE b.project_id = :projectId AND b.deleted = 0
            GROUP BY b.id, b.view_type, b.review_status, f.original_name, dt.name
            ORDER BY b.view_type, b.id
            """, new MapSqlParameterSource("projectId", projectId));
    }

    public List<Map<String, Object>> findRectificationsCsvData(Long projectId) {
        return jdbcTemplate.queryForList("""
            SELECT r.id, r.title, r.reason, r.status, r.severity,
                   f.original_name AS binding_file_name,
                   r.resolution_note, r.created_at, r.resolved_at, r.closed_at
            FROM work_rectifications r
            JOIN work_delivery_bindings b ON b.id = r.binding_id AND b.deleted = 0
            LEFT JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            WHERE r.project_id = :projectId AND r.deleted = 0
            ORDER BY r.created_at DESC
            """, new MapSqlParameterSource("projectId", projectId));
    }

    private static RectificationResponse mapRectification(ResultSet rs) throws SQLException {
        Long assigneeId = rs.getObject("assignee_user_id") == null ? null : rs.getLong("assignee_user_id");
        Long createdBy = rs.getObject("created_by") == null ? null : rs.getLong("created_by");
        Long updatedBy = rs.getObject("updated_by") == null ? null : rs.getLong("updated_by");
        java.sql.Timestamp resolvedTs = rs.getTimestamp("resolved_at");
        java.sql.Timestamp closedTs = rs.getTimestamp("closed_at");
        java.sql.Date dueDate = rs.getDate("due_date");
        return new RectificationResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("source_type"),
            rs.getLong("source_id"),
            rs.getLong("binding_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("reason"),
            rs.getString("status"),
            rs.getString("severity"),
            assigneeId,
            rs.getString("resolution_note"),
            dueDate != null ? dueDate.toLocalDate() : null,
            resolvedTs != null ? resolvedTs.toLocalDateTime() : null,
            closedTs != null ? closedTs.toLocalDateTime() : null,
            createdBy,
            updatedBy,
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime(),
            rs.getString("binding_view_type"),
            rs.getString("binding_file_name"),
            rs.getString("binding_deliverable_type_name"),
            rs.getString("binding_section_node_name")
        );
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

    // ---- batch binding existence check ----

    public boolean bindingExists(Long projectId, String viewType, Long sectionNodeId,
                                 Long managedObjectId, Long deliverableTypeId, Long fileResourceId) {
        String targetClause;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType)
            .addValue("deliverableTypeId", deliverableTypeId)
            .addValue("fileResourceId", fileResourceId);
        if (sectionNodeId != null) {
            targetClause = "AND section_node_id = :targetId";
            params.addValue("targetId", sectionNodeId);
        } else if (managedObjectId != null) {
            targetClause = "AND managed_object_id = :targetId";
            params.addValue("targetId", managedObjectId);
        } else {
            targetClause = "AND section_node_id IS NULL AND managed_object_id IS NULL";
        }
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1) FROM work_delivery_bindings
            WHERE project_id = :projectId
              AND view_type = :viewType
              AND deliverable_type_id = :deliverableTypeId
              AND file_resource_id = :fileResourceId
              %s
              AND deleted = 0
            """.formatted(targetClause), params, Integer.class);
        return count != null && count > 0;
    }

    public Long findExistingBindingId(Long projectId, String viewType, Long sectionNodeId,
                                       Long managedObjectId, Long deliverableTypeId, Long fileResourceId) {
        String targetClause;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType)
            .addValue("deliverableTypeId", deliverableTypeId)
            .addValue("fileResourceId", fileResourceId);
        if (sectionNodeId != null) {
            targetClause = "AND section_node_id = :targetId";
            params.addValue("targetId", sectionNodeId);
        } else if (managedObjectId != null) {
            targetClause = "AND managed_object_id = :targetId";
            params.addValue("targetId", managedObjectId);
        } else {
            targetClause = "AND section_node_id IS NULL AND managed_object_id IS NULL";
        }
        List<Long> ids = jdbcTemplate.queryForList("""
            SELECT id FROM work_delivery_bindings
            WHERE project_id = :projectId
              AND view_type = :viewType
              AND deliverable_type_id = :deliverableTypeId
              AND file_resource_id = :fileResourceId
              %s
              AND deleted = 0
            LIMIT 1
            """.formatted(targetClause), params, Long.class);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    // ---- delivery package summary ----

    public List<DeliveryPackageSummaryRow> findPackageSummaryRows(Long projectId, String viewType, String targetType) {
        boolean sectionTarget = "SECTION".equals(targetType);
        String targetIdExpression = sectionTarget ? "sn.id" : "mo.id";
        String targetNameExpression = sectionTarget ? "sn.name" : "mo.name";
        String targetFilter = sectionTarget ? "AND b.section_node_id IS NOT NULL" : "AND b.managed_object_id IS NOT NULL";
        String sql = """
            SELECT
                dd.id AS def_id, dd.name AS def_name,
                dt.id AS type_id, dt.name AS type_name,
                :targetType AS target_type,
                %s AS target_id,
                %s AS target_name,
                b.id AS binding_id,
                b.file_resource_id,
                f.original_name AS file_name,
                f.file_kind,
                f.version_no,
                b.review_status
            FROM work_delivery_bindings b
            JOIN data_file_resources f ON f.id = b.file_resource_id AND f.project_id = b.project_id AND f.deleted = 0
            JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.project_id = b.project_id AND dt.deleted = 0
            JOIN masterdata_deliverable_definitions dd ON dd.id = dt.deliverable_definition_id AND dd.project_id = b.project_id AND dd.deleted = 0
            LEFT JOIN masterdata_section_nodes sn ON sn.id = b.section_node_id AND sn.project_id = b.project_id AND sn.deleted = 0
            LEFT JOIN data_managed_objects mo ON mo.id = b.managed_object_id AND mo.project_id = b.project_id AND mo.deleted = 0
            WHERE b.project_id = :projectId
              AND b.view_type = :viewType
              AND b.deleted = 0
              AND b.binding_status = 'BOUND'
              %s
            ORDER BY dd.sort_order, dt.sort_order, b.id
            """.formatted(targetIdExpression, targetNameExpression, targetFilter);
        return jdbcTemplate.query(sql,
            new MapSqlParameterSource("projectId", projectId)
                .addValue("viewType", viewType)
                .addValue("targetType", targetType),
            (rs, rowNum) -> {
                String reviewStatus = rs.getString("review_status");
                String targetTypeValue = rs.getString("target_type");
                String readiness = computeReadiness(reviewStatus, rs.getObject("binding_id") != null);
                return new DeliveryPackageSummaryRow(
                    rs.getObject("def_id") != null ? rs.getLong("def_id") : null,
                    rs.getString("def_name"),
                    rs.getObject("type_id") != null ? rs.getLong("type_id") : null,
                    rs.getString("type_name"),
                    targetTypeValue,
                    rs.getObject("target_id") != null ? rs.getLong("target_id") : null,
                    rs.getString("target_name"),
                    rs.getObject("binding_id") != null ? rs.getLong("binding_id") : null,
                    rs.getObject("file_resource_id") != null ? rs.getLong("file_resource_id") : null,
                    rs.getString("file_name"),
                    rs.getString("file_kind"),
                    rs.getString("version_no"),
                    reviewStatus,
                    readiness
                );
            });
    }

    private static String computeReadiness(String reviewStatus, boolean hasBinding) {
        if (!hasBinding) return "MISSING";
        if ("APPROVED".equals(reviewStatus)) return "READY";
        if ("PENDING".equals(reviewStatus) || "DRAFT".equals(reviewStatus)) return "PENDING_REVIEW";
        if ("REJECTED".equals(reviewStatus)) return "REJECTED";
        return "PENDING_REVIEW";
    }

    public List<DeliveryCompletenessRow> findMissingRequiredRows(Long projectId, String viewType, String targetType) {
        // Reuse the completeness logic: find all required rows, then filter those without bindings
        String fileKind = "DRAWING".equals(viewType) ? "DRAWING" : "DOCUMENT";
        List<DeliveryCompletenessRow> required = findRequiredDeliverables(projectId, fileKind, targetType, viewType);
        List<DeliveryCompletenessRow> completed = findCompletedBindings(projectId, viewType, targetType);

        var completedMap = new LinkedHashMap<String, DeliveryCompletenessRow>();
        for (var c : completed) {
            String key = c.targetId() + "_" + c.deliverableTypeId();
            completedMap.putIfAbsent(key, c);
        }

        List<DeliveryCompletenessRow> missing = new ArrayList<>();
        for (var req : required) {
            String key = req.targetId() + "_" + req.deliverableTypeId();
            if (!completedMap.containsKey(key)) {
                missing.add(req);
            }
        }
        return missing;
    }

    // ---- export precheck ----

    public List<ExportPrecheckRow> findExportPrecheckBoundRows(Long projectId, String viewType, String targetType) {
        boolean sectionTarget = "SECTION".equals(targetType);
        String targetIdExpression = sectionTarget ? "sn.id" : "mo.id";
        String targetNameExpression = sectionTarget ? "sn.name" : "mo.name";
        String targetFilter = sectionTarget ? "AND b.section_node_id IS NOT NULL" : "AND b.managed_object_id IS NOT NULL";
        String sql = """
            SELECT
                dd.id AS def_id, dd.name AS def_name,
                dt.id AS type_id, dt.name AS type_name,
                :targetType AS target_type,
                %s AS target_id,
                %s AS target_name,
                b.id AS binding_id,
                b.file_resource_id,
                f.original_name AS file_name,
                f.file_kind,
                f.version_no,
                LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS file_ext,
                b.review_status
            FROM work_delivery_bindings b
            JOIN data_file_resources f ON f.id = b.file_resource_id AND f.project_id = b.project_id AND f.deleted = 0
            JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.project_id = b.project_id AND dt.deleted = 0
            JOIN masterdata_deliverable_definitions dd ON dd.id = dt.deliverable_definition_id AND dd.project_id = b.project_id AND dd.deleted = 0
            LEFT JOIN masterdata_section_nodes sn ON sn.id = b.section_node_id AND sn.project_id = b.project_id AND sn.deleted = 0
            LEFT JOIN data_managed_objects mo ON mo.id = b.managed_object_id AND mo.project_id = b.project_id AND mo.deleted = 0
            WHERE b.project_id = :projectId
              AND b.view_type = :viewType
              AND b.deleted = 0
              AND b.binding_status = 'BOUND'
              %s
            ORDER BY dd.sort_order, dt.sort_order, b.id
            """.formatted(targetIdExpression, targetNameExpression, targetFilter);
        return jdbcTemplate.query(sql,
            new MapSqlParameterSource("projectId", projectId)
                .addValue("viewType", viewType)
                .addValue("targetType", targetType),
            (rs, rowNum) -> {
                String reviewStatus = rs.getString("review_status");
                return new ExportPrecheckRow(
                    rs.getObject("def_id") != null ? rs.getLong("def_id") : null,
                    rs.getString("def_name"),
                    rs.getObject("type_id") != null ? rs.getLong("type_id") : null,
                    rs.getString("type_name"),
                    rs.getString("target_type"),
                    rs.getObject("target_id") != null ? rs.getLong("target_id") : null,
                    rs.getString("target_name"),
                    rs.getObject("binding_id") != null ? rs.getLong("binding_id") : null,
                    rs.getObject("file_resource_id") != null ? rs.getLong("file_resource_id") : null,
                    rs.getString("file_name"),
                    rs.getString("file_kind"),
                    rs.getString("version_no"),
                    rs.getString("file_ext"),
                    reviewStatus,
                    null, // readinessStatus filled by service
                    null, null, null, null, // preview fields filled by service
                    null, null, null, null, // preview display fields filled by service
                    null, null // export fields filled by service
                );
            });
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

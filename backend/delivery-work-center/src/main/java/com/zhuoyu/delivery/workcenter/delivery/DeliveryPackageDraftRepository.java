package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageArchiveItemResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageDraftSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryPackageDraftRepository {

    private static final RowMapper<DeliveryPackageDraftSummaryResponse> DRAFT_ROW_MAPPER =
        DeliveryPackageDraftRepository::mapDraft;

    private static final RowMapper<DeliveryPackageArchiveItemResponse> ITEM_ROW_MAPPER =
        DeliveryPackageDraftRepository::mapItem;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DeliveryPackageDraftRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insertDraft(
        Long projectId,
        String viewType,
        String targetType,
        int totalCount,
        int readyCount,
        int blockedCount,
        int missingCount,
        int pendingReviewCount,
        int rejectedCount,
        int conversionRequiredCount,
        int unsupportedPreviewCount,
        Long createdBy
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO work_delivery_package_drafts (
                project_id, view_type, target_type,
                total_count, ready_count, blocked_count, missing_count,
                pending_review_count, rejected_count, conversion_required_count, unsupported_preview_count,
                dry_run, physical_package_generated, nas_file_copied, created_by
            ) VALUES (
                :projectId, :viewType, :targetType,
                :totalCount, :readyCount, :blockedCount, :missingCount,
                :pendingReviewCount, :rejectedCount, :conversionRequiredCount, :unsupportedPreviewCount,
                1, 0, 0, :createdBy
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("viewType", viewType)
            .addValue("targetType", targetType)
            .addValue("totalCount", totalCount)
            .addValue("readyCount", readyCount)
            .addValue("blockedCount", blockedCount)
            .addValue("missingCount", missingCount)
            .addValue("pendingReviewCount", pendingReviewCount)
            .addValue("rejectedCount", rejectedCount)
            .addValue("conversionRequiredCount", conversionRequiredCount)
            .addValue("unsupportedPreviewCount", unsupportedPreviewCount)
            .addValue("createdBy", createdBy), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void insertItems(Long draftId, Long projectId, List<DeliveryPackageArchiveItemResponse> items) {
        if (items.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = new SqlParameterSource[items.size()];
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            batch[i] = new MapSqlParameterSource()
                .addValue("draftId", draftId)
                .addValue("projectId", projectId)
                .addValue("sortOrder", i)
                .addValue("targetType", item.targetType())
                .addValue("targetId", item.targetId())
                .addValue("targetName", item.targetName())
                .addValue("deliverableDefinitionId", item.deliverableDefinitionId())
                .addValue("deliverableDefinitionName", item.deliverableDefinitionName())
                .addValue("deliverableTypeId", item.deliverableTypeId())
                .addValue("deliverableTypeName", item.deliverableTypeName())
                .addValue("bindingId", item.bindingId())
                .addValue("fileId", item.fileId())
                .addValue("fileName", item.fileName())
                .addValue("fileKind", item.fileKind())
                .addValue("versionNo", item.versionNo())
                .addValue("reviewStatus", item.reviewStatus())
                .addValue("previewStatus", item.previewStatus())
                .addValue("exportStatus", item.exportStatus())
                .addValue("blockReason", item.blockReason())
                .addValue("archiveDirectoryPath", item.archiveDirectoryPath());
        }
        jdbcTemplate.batchUpdate("""
            INSERT INTO work_delivery_package_draft_items (
                draft_id, project_id, sort_order,
                target_type, target_id, target_name,
                deliverable_definition_id, deliverable_definition_name,
                deliverable_type_id, deliverable_type_name,
                binding_id, file_id, file_name, file_kind, version_no,
                review_status, preview_status, export_status, block_reason, archive_directory_path
            ) VALUES (
                :draftId, :projectId, :sortOrder,
                :targetType, :targetId, :targetName,
                :deliverableDefinitionId, :deliverableDefinitionName,
                :deliverableTypeId, :deliverableTypeName,
                :bindingId, :fileId, :fileName, :fileKind, :versionNo,
                :reviewStatus, :previewStatus, :exportStatus, :blockReason, :archiveDirectoryPath
            )
            """, batch);
    }

    public List<DeliveryPackageDraftSummaryResponse> findDrafts(Long projectId) {
        return jdbcTemplate.query(draftSql("""
            WHERE project_id = :projectId AND deleted = 0
            ORDER BY created_at DESC, id DESC
            LIMIT 50
            """), new MapSqlParameterSource("projectId", projectId), DRAFT_ROW_MAPPER);
    }

    public Optional<DeliveryPackageDraftSummaryResponse> findDraft(Long projectId, Long draftId) {
        List<DeliveryPackageDraftSummaryResponse> rows = jdbcTemplate.query(draftSql("""
            WHERE project_id = :projectId AND id = :draftId AND deleted = 0
            """), new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("draftId", draftId), DRAFT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<DeliveryPackageArchiveItemResponse> findItems(Long projectId, Long draftId) {
        return jdbcTemplate.query("""
            SELECT
                id,
                target_type,
                target_id,
                target_name,
                deliverable_definition_id,
                deliverable_definition_name,
                deliverable_type_id,
                deliverable_type_name,
                binding_id,
                file_id,
                file_name,
                file_kind,
                version_no,
                review_status,
                preview_status,
                export_status,
                block_reason,
                archive_directory_path
            FROM work_delivery_package_draft_items
            WHERE project_id = :projectId AND draft_id = :draftId
            ORDER BY sort_order, id
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("draftId", draftId), ITEM_ROW_MAPPER);
    }

    private static String draftSql(String whereClause) {
        return """
            SELECT
                project_id,
                id AS draft_id,
                view_type,
                target_type,
                total_count,
                ready_count,
                blocked_count,
                missing_count,
                pending_review_count,
                rejected_count,
                conversion_required_count,
                unsupported_preview_count,
                dry_run,
                physical_package_generated,
                nas_file_copied,
                created_by,
                created_at
            FROM work_delivery_package_drafts
            %s
            """.formatted(whereClause);
    }

    private static DeliveryPackageDraftSummaryResponse mapDraft(ResultSet rs, int rowNum) throws SQLException {
        return new DeliveryPackageDraftSummaryResponse(
            rs.getLong("project_id"),
            rs.getLong("draft_id"),
            rs.getString("view_type"),
            rs.getString("target_type"),
            rs.getInt("total_count"),
            rs.getInt("ready_count"),
            rs.getInt("blocked_count"),
            rs.getInt("missing_count"),
            rs.getInt("pending_review_count"),
            rs.getInt("rejected_count"),
            rs.getInt("conversion_required_count"),
            rs.getInt("unsupported_preview_count"),
            rs.getInt("dry_run") == 1,
            rs.getInt("physical_package_generated") == 1,
            rs.getInt("nas_file_copied") == 1,
            nullableLong(rs, "created_by"),
            rs.getObject("created_at", LocalDateTime.class)
        );
    }

    private static DeliveryPackageArchiveItemResponse mapItem(ResultSet rs, int rowNum) throws SQLException {
        return new DeliveryPackageArchiveItemResponse(
            rs.getLong("id"),
            rs.getString("target_type"),
            nullableLong(rs, "target_id"),
            rs.getString("target_name"),
            nullableLong(rs, "deliverable_definition_id"),
            rs.getString("deliverable_definition_name"),
            nullableLong(rs, "deliverable_type_id"),
            rs.getString("deliverable_type_name"),
            nullableLong(rs, "binding_id"),
            nullableLong(rs, "file_id"),
            rs.getString("file_name"),
            rs.getString("file_kind"),
            rs.getString("version_no"),
            rs.getString("review_status"),
            rs.getString("preview_status"),
            rs.getString("export_status"),
            rs.getString("block_reason"),
            rs.getString("archive_directory_path")
        );
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : rs.getLong(column);
    }
}

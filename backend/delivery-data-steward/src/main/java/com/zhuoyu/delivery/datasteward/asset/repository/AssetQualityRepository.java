package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityProjectRisk;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetQualityRepository {

    private static final RowMapper<AssetQualityProjectRisk> PROJECT_RISK_ROW_MAPPER =
        AssetQualityRepository::mapProjectRisk;
    private static final RowMapper<EventResponse> EVENT_ROW_MAPPER = AssetQualityRepository::mapEvent;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetQualityRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countFilesByCondition(Long userId, Long projectId, String assetSource, String condition) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND (
            """).append(condition).append("""
              )
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        appendProjectAndSource(sql, params, projectId, assetSource);
        return count(sql.toString(), params);
    }

    public long countPendingReviewCandidates(Long userId, Long projectId, String assetSource) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM data_asset_scan_candidates c
            JOIN data_asset_scan_tasks t ON t.id = c.scan_task_id AND t.deleted = 0
            LEFT JOIN core_projects p ON p.id = c.matched_project_id AND p.deleted = 0
            LEFT JOIN core_user_project_roles upr
              ON upr.project_id = c.matched_project_id
             AND upr.user_id = :userId
             AND upr.deleted = 0
            WHERE c.deleted = 0
              AND c.review_status = 'PENDING'
              AND (
                  (c.matched_project_id IS NOT NULL AND upr.id IS NOT NULL)
                  OR (c.matched_project_id IS NULL AND t.created_by = :userId)
              )
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (projectId != null) {
            sql.append(" AND c.matched_project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource, true);
        return count(sql.toString(), params);
    }

    public long countScanTasksByStatus(Long userId, Long projectId, String assetSource, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM data_asset_scan_tasks t
            LEFT JOIN core_projects p ON p.id = t.project_id AND p.deleted = 0
            LEFT JOIN core_user_project_roles upr
              ON upr.project_id = t.project_id
             AND upr.user_id = :userId
             AND upr.deleted = 0
            WHERE t.deleted = 0
              AND t.status IN (:statuses)
              AND (
                  (t.project_id IS NOT NULL AND upr.id IS NOT NULL)
                  OR (t.project_id IS NULL AND t.created_by = :userId)
              )
            """);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("statuses", statuses);
        if (projectId != null) {
            sql.append(" AND t.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource, true);
        return count(sql.toString(), params);
    }

    public long countNonstandardDirectories(Long userId, String governanceStatus) {
        return count("""
            SELECT COUNT(1)
            FROM data_asset_nonstandard_directories
            WHERE deleted = 0
              AND created_by = :userId
              AND governance_status = :governanceStatus
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("governanceStatus", governanceStatus));
    }

    public Instant latestEventAt(Long userId, Long projectId, String assetSource) {
        StringBuilder sql = new StringBuilder("""
            SELECT MAX(e.created_at)
            FROM data_asset_events e
            LEFT JOIN core_projects p ON p.id = e.project_id AND p.deleted = 0
            LEFT JOIN core_user_project_roles upr
              ON upr.project_id = e.project_id
             AND upr.user_id = :userId
             AND upr.deleted = 0
            WHERE (
                (e.project_id IS NOT NULL AND upr.id IS NOT NULL)
                OR (e.project_id IS NULL AND e.operator_id = :userId)
            )
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (projectId != null) {
            sql.append(" AND e.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource, true);
        Timestamp ts = jdbcTemplate.queryForObject(sql.toString(), params, Timestamp.class);
        return ts == null ? null : ts.toInstant();
    }

    public List<EventResponse> recentEvents(Long userId, Long projectId, String assetSource, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT e.id, e.event_type, e.project_id, e.aggregate_type, e.aggregate_id,
                   e.action_code, e.operator_id, e.source_type, e.summary,
                   e.payload_json, e.trace_id, e.created_at
            FROM data_asset_events e
            LEFT JOIN core_projects p ON p.id = e.project_id AND p.deleted = 0
            LEFT JOIN core_user_project_roles upr
              ON upr.project_id = e.project_id
             AND upr.user_id = :userId
             AND upr.deleted = 0
            WHERE (
                (e.project_id IS NOT NULL AND upr.id IS NOT NULL)
                OR (e.project_id IS NULL AND e.operator_id = :userId)
            )
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (projectId != null) {
            sql.append(" AND e.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource, true);
        sql.append(" ORDER BY e.id DESC LIMIT :limit");
        params.addValue("limit", Math.max(1, Math.min(limit, 50)));
        return jdbcTemplate.query(sql.toString(), params, EVENT_ROW_MAPPER);
    }

    public List<AssetQualityProjectRisk> topRiskProjects(Long userId, Long projectId, String assetSource, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id AS project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   COALESCE(fq.missing_checksum_count, 0) AS missing_checksum_count,
                   COALESCE(fq.missing_confidence_count, 0) AS missing_confidence_count,
                   COALESCE(fq.missing_discipline_count, 0) AS missing_discipline_count,
                   COALESCE(fq.missing_version_count, 0) AS missing_version_count,
                   COALESCE(fq.missing_storage_path_count, 0) AS missing_storage_path_count,
                   COALESCE(fq.zero_size_file_count, 0) AS zero_size_file_count,
                   COALESCE(cq.pending_review_count, 0) AS pending_review_count,
                   COALESCE(sq.failed_scan_count, 0) AS failed_scan_count,
                   (
                       COALESCE(fq.missing_checksum_count, 0)
                       + COALESCE(fq.missing_confidence_count, 0)
                       + COALESCE(fq.missing_discipline_count, 0)
                       + COALESCE(fq.missing_version_count, 0)
                       + COALESCE(fq.missing_storage_path_count, 0)
                       + COALESCE(fq.zero_size_file_count, 0)
                       + COALESCE(cq.pending_review_count, 0)
                       + COALESCE(sq.failed_scan_count, 0)
                   ) AS total_risk_count
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            LEFT JOIN (
                SELECT project_id,
                       SUM(CASE WHEN checksum IS NULL OR checksum = '' THEN 1 ELSE 0 END) AS missing_checksum_count,
                       SUM(CASE WHEN confidence_level IS NULL OR confidence_level = '' THEN 1 ELSE 0 END) AS missing_confidence_count,
                       SUM(CASE WHEN discipline IS NULL OR discipline = '' OR discipline = 'OTHER' THEN 1 ELSE 0 END) AS missing_discipline_count,
                       SUM(CASE WHEN version_no IS NULL OR version_no = '' THEN 1 ELSE 0 END) AS missing_version_count,
                       SUM(CASE WHEN storage_uri IS NULL OR storage_uri = '' THEN 1 ELSE 0 END) AS missing_storage_path_count,
                       SUM(CASE WHEN COALESCE(size_bytes, 0) <= 0 THEN 1 ELSE 0 END) AS zero_size_file_count
                FROM data_file_resources
                WHERE deleted = 0
                GROUP BY project_id
            ) fq ON fq.project_id = p.id
            LEFT JOIN (
                SELECT matched_project_id AS project_id, COUNT(1) AS pending_review_count
                FROM data_asset_scan_candidates
                WHERE deleted = 0
                  AND review_status = 'PENDING'
                  AND matched_project_id IS NOT NULL
                GROUP BY matched_project_id
            ) cq ON cq.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS failed_scan_count
                FROM data_asset_scan_tasks
                WHERE deleted = 0
                  AND status = 'FAILED'
                  AND project_id IS NOT NULL
                GROUP BY project_id
            ) sq ON sq.project_id = p.id
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        appendProjectAndSource(sql, params, projectId, assetSource);
        sql.append("""
            HAVING total_risk_count > 0
            ORDER BY total_risk_count DESC, p.id DESC
            LIMIT :limit
            """);
        params.addValue("limit", Math.max(1, Math.min(limit, 50)));
        return jdbcTemplate.query(sql.toString(), params, PROJECT_RISK_ROW_MAPPER);
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    private void appendProjectAndSource(StringBuilder sql, MapSqlParameterSource params,
                                        Long projectId, String assetSource) {
        if (projectId != null) {
            sql.append(" AND p.id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource, false);
    }

    private void appendAssetSourceFilter(StringBuilder sql, MapSqlParameterSource params,
                                         String tableAlias, String assetSource, boolean requireProject) {
        String source = blankToNull(assetSource);
        if (source == null) {
            return;
        }
        if (requireProject) {
            sql.append(" AND ").append(tableAlias).append(".id IS NOT NULL ");
        }
        if (source.endsWith("*")) {
            sql.append(" AND ").append(tableAlias).append(".asset_source LIKE :assetSourceLike ");
            params.addValue("assetSourceLike", source.substring(0, source.length() - 1) + "%");
            return;
        }
        sql.append(" AND ").append(tableAlias).append(".asset_source = :assetSource ");
        params.addValue("assetSource", source);
    }

    private static AssetQualityProjectRisk mapProjectRisk(ResultSet rs, int rowNum) throws SQLException {
        return new AssetQualityProjectRisk(
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getLong("missing_checksum_count"),
            rs.getLong("missing_confidence_count"),
            rs.getLong("missing_discipline_count"),
            rs.getLong("missing_version_count"),
            rs.getLong("missing_storage_path_count"),
            rs.getLong("zero_size_file_count"),
            rs.getLong("pending_review_count"),
            rs.getLong("failed_scan_count"),
            rs.getLong("total_risk_count")
        );
    }

    private static EventResponse mapEvent(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Long projectId = rs.getObject("project_id", Long.class);
        Instant eventTime = createdAt == null ? null : createdAt.toInstant();
        return new EventResponse(
            rs.getLong("id"),
            rs.getString("event_type"),
            projectId,
            rs.getString("aggregate_type"),
            rs.getString("aggregate_id"),
            rs.getString("action_code"),
            rs.getObject("operator_id", Long.class),
            rs.getString("source_type"),
            rs.getString("summary"),
            rs.getString("payload_json"),
            rs.getString("trace_id"),
            eventTime,
            permissionTags(projectId),
            null,
            "UNKNOWN",
            eventTime,
            "active",
            "catalog_only"
        );
    }

    private static List<String> permissionTags(Long projectId) {
        return List.of(
            "SOURCE_SYSTEM:delivery_platform",
            "SOURCE_VIEW:AuditEventView",
            "ASSET_KIND:AUDIT_EVENT",
            "PROJECT:" + (projectId == null ? "UNKNOWN" : projectId),
            "CONFIDENTIALITY:UNKNOWN",
            "INDEX_ELIGIBILITY:catalog_only"
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

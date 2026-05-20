package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentAssetContextResolver {

    private static final Set<String> ALLOWED_SOURCE_VIEWS = Set.of(
        "ProjectAssetView",
        "FileAssetView",
        "ModelAssetView",
        "AuditEventView"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AgentAssetContextResolver(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentAssetContext resolve(HermesChatRequest request) {
        Long projectId = request.projectId();
        String sourceView = normalizeSourceView(request.sourceView());
        if (projectId == null) {
            return AgentAssetContext.denied(sourceView, null, request.assetId(),
                "MISSING_PROJECT_SCOPE", "缺少项目范围");
        }
        if (!ALLOWED_SOURCE_VIEWS.contains(sourceView)) {
            return AgentAssetContext.denied(sourceView, projectId, request.assetId(),
                "UNSUPPORTED_SOURCE_VIEW", "不支持的资产来源");
        }

        Optional<AssetGovernance> governance = request.assetId() == null
            ? findProjectGovernance(projectId)
            : findAssetGovernance(sourceView, request.assetId());
        if (governance.isEmpty()) {
            return AgentAssetContext.denied(sourceView, projectId, request.assetId(),
                "ASSET_CONTEXT_NOT_FOUND", "资产上下文不存在或不可用");
        }

        AssetGovernance asset = governance.get();
        if (asset.projectId() == null || !asset.projectId().equals(projectId)) {
            return AgentAssetContext.denied(sourceView, projectId, request.assetId(),
                "ASSET_PROJECT_SCOPE_MISMATCH", "资产不属于当前授权项目");
        }
        if (asset.permissionTags() == null || asset.permissionTags().isBlank()) {
            return AgentAssetContext.denied(sourceView, projectId, request.assetId(),
                "PERMISSION_TAGS_MISSING", "权限标签缺失，已按 fail closed 拒绝");
        }
        return new AgentAssetContext(
            true,
            sourceView,
            projectId,
            request.assetId(),
            asset.confidentialityLevel(),
            asset.lifecycleStatus(),
            asset.indexEligibility(),
            true,
            null,
            null
        );
    }

    private String normalizeSourceView(String sourceView) {
        if (sourceView == null || sourceView.isBlank()) {
            return "ProjectAssetView";
        }
        for (String allowed : ALLOWED_SOURCE_VIEWS) {
            if (allowed.equalsIgnoreCase(sourceView.trim())) {
                return allowed;
            }
        }
        return sourceView.trim();
    }

    private Optional<AssetGovernance> findProjectGovernance(Long projectId) {
        return first("""
            SELECT project_id, permission_tags, confidentiality_level, lifecycle_status, index_eligibility
            FROM ProjectAssetView
            WHERE project_id = :projectId
            LIMIT 1
            """, Map.of("projectId", projectId));
    }

    private Optional<AssetGovernance> findAssetGovernance(String sourceView, Long assetId) {
        return switch (sourceView) {
            case "ProjectAssetView" -> first("""
                SELECT project_id, permission_tags, confidentiality_level, lifecycle_status, index_eligibility
                FROM ProjectAssetView
                WHERE project_id = :assetId
                LIMIT 1
                """, Map.of("assetId", assetId));
            case "FileAssetView" -> first("""
                SELECT project_id, permission_tags, confidentiality_level, lifecycle_status, index_eligibility
                FROM FileAssetView
                WHERE file_id = :assetId
                LIMIT 1
                """, Map.of("assetId", assetId));
            case "ModelAssetView" -> first("""
                SELECT project_id, permission_tags, confidentiality_level, lifecycle_status, index_eligibility
                FROM ModelAssetView
                WHERE model_id = :assetId
                LIMIT 1
                """, Map.of("assetId", assetId));
            case "AuditEventView" -> first("""
                SELECT project_id,
                       CONCAT('SOURCE_SYSTEM:delivery_platform,SOURCE_VIEW:AuditEventView,ASSET_KIND:AUDIT_EVENT,PROJECT:', COALESCE(project_id, 'UNKNOWN')) AS permission_tags,
                       'UNKNOWN' AS confidentiality_level,
                       'active' AS lifecycle_status,
                       'catalog_only' AS index_eligibility
                FROM AuditEventView
                WHERE event_id = :assetId
                LIMIT 1
                """, Map.of("assetId", assetId));
            default -> Optional.empty();
        };
    }

    private Optional<AssetGovernance> first(String sql, Map<String, ?> parameters) {
        List<AssetGovernance> rows = jdbcTemplate.query(sql, new MapSqlParameterSource(parameters),
            (rs, rowNum) -> new AssetGovernance(
                rs.getObject("project_id") == null ? null : rs.getLong("project_id"),
                rs.getString("permission_tags"),
                upperOrUnknown(rs.getString("confidentiality_level")),
                lowerOrUnknown(rs.getString("lifecycle_status")),
                lowerOrCatalogOnly(rs.getString("index_eligibility"))
            ));
        return rows.stream().findFirst();
    }

    private static String upperOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String lowerOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String lowerOrCatalogOnly(String value) {
        if (value == null || value.isBlank()) {
            return "catalog_only";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record AgentAssetContext(
        boolean allowed,
        String sourceView,
        Long projectId,
        Long assetId,
        String confidentialityLevel,
        String lifecycleStatus,
        String indexEligibility,
        boolean permissionTagsPresent,
        String denialReasonCode,
        String denialReasonText
    ) {
        static AgentAssetContext denied(
            String sourceView,
            Long projectId,
            Long assetId,
            String denialReasonCode,
            String denialReasonText
        ) {
            return new AgentAssetContext(false, sourceView, projectId, assetId,
                "UNKNOWN", "unknown", "catalog_only", false, denialReasonCode, denialReasonText);
        }
    }

    private record AssetGovernance(
        Long projectId,
        String permissionTags,
        String confidentialityLevel,
        String lifecycleStatus,
        String indexEligibility
    ) {
    }
}

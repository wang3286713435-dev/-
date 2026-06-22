package com.zhuoyu.delivery.datasteward.search.application;

import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
import com.zhuoyu.delivery.datasteward.search.dto.GlobalSearchDtos.GlobalSearchGroup;
import com.zhuoyu.delivery.datasteward.search.dto.GlobalSearchDtos.GlobalSearchItem;
import com.zhuoyu.delivery.datasteward.search.dto.GlobalSearchDtos.GlobalSearchResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class GlobalSearchApplicationService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProjectAccessApplicationService projectAccessApplicationService;

    public GlobalSearchApplicationService(
        NamedParameterJdbcTemplate jdbcTemplate,
        ProjectAccessApplicationService projectAccessApplicationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectAccessApplicationService = projectAccessApplicationService;
    }

    public GlobalSearchResponse search(Long userId, String keyword, Long projectId, Integer requestedLimit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int limit = normalizeLimit(requestedLimit);
        if (normalizedKeyword.isBlank()) {
            return new GlobalSearchResponse("", projectId, 0, emptyGroups());
        }

        List<AccessibleProject> projects = accessibleProjects(userId, projectId);
        if (projects.isEmpty()) {
            return new GlobalSearchResponse(normalizedKeyword, projectId, 0, emptyGroups());
        }

        List<Long> projectIds = projects.stream().map(AccessibleProject::id).toList();
        String like = "%" + escapeLike(normalizedKeyword) + "%";
        MapSqlParameterSource baseParams = new MapSqlParameterSource()
            .addValue("projectIds", projectIds)
            .addValue("like", like)
            .addValue("limit", limit);

        List<GlobalSearchGroup> groups = new ArrayList<>();
        groups.add(new GlobalSearchGroup("PROJECT", "项目", 0, searchProjects(baseParams)));
        groups.add(new GlobalSearchGroup("FILE", "文件", 0, searchFiles(baseParams)));
        groups.add(new GlobalSearchGroup("MODEL", "模型", 0, searchModels(baseParams)));
        groups.add(new GlobalSearchGroup("DELIVERY", "交付事项", 0, searchDelivery(baseParams)));
        groups.add(new GlobalSearchGroup("RECTIFICATION", "整改项", 0, searchRectifications(baseParams)));

        List<GlobalSearchGroup> normalizedGroups = groups.stream()
            .map(group -> new GlobalSearchGroup(group.type(), group.label(), group.items().size(), group.items()))
            .toList();
        int totalCount = normalizedGroups.stream().mapToInt(GlobalSearchGroup::count).sum();
        return new GlobalSearchResponse(normalizedKeyword, projectId, totalCount, normalizedGroups);
    }

    private List<AccessibleProject> accessibleProjects(Long userId, Long projectId) {
        if (projectId != null) {
            return List.of(projectAccessApplicationService.requireAccessibleProject(userId, projectId));
        }
        return projectAccessApplicationService.listAccessibleProjects(userId);
    }

    private List<GlobalSearchGroup> emptyGroups() {
        return List.of(
            new GlobalSearchGroup("PROJECT", "项目", 0, List.of()),
            new GlobalSearchGroup("FILE", "文件", 0, List.of()),
            new GlobalSearchGroup("MODEL", "模型", 0, List.of()),
            new GlobalSearchGroup("DELIVERY", "交付事项", 0, List.of()),
            new GlobalSearchGroup("RECTIFICATION", "整改项", 0, List.of())
        );
    }

    private List<GlobalSearchItem> searchProjects(MapSqlParameterSource params) {
        return jdbcTemplate.query("""
            SELECT p.id, p.code, p.name, p.status, p.project_manager_name
            FROM core_projects p
            WHERE p.id IN (:projectIds)
              AND p.deleted = 0
              AND (
                p.code LIKE :like ESCAPE '\\\\'
                OR p.name LIKE :like ESCAPE '\\\\'
                OR COALESCE(p.project_manager_name, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(p.industry_type, '') LIKE :like ESCAPE '\\\\'
              )
            ORDER BY p.updated_at DESC, p.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> item(
                "PROJECT",
                String.valueOf(rs.getLong("id")),
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("name"),
                "项目编码 " + rs.getString("code"),
                rs.getString("status"),
                "data-steward-asset-detail",
                Map.of("projectId", rs.getLong("id")),
                Map.of("tab", "dashboard")
            ));
    }

    private List<GlobalSearchItem> searchFiles(MapSqlParameterSource params) {
        return jdbcTemplate.query("""
            SELECT f.id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.version_no, f.process_status
            FROM data_file_resources f
            JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
            WHERE f.project_id IN (:projectIds)
              AND f.deleted = 0
              AND (
                f.original_name LIKE :like ESCAPE '\\\\'
                OR COALESCE(f.asset_uuid, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(f.file_kind, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(f.version_no, '') LIKE :like ESCAPE '\\\\'
              )
            ORDER BY f.updated_at DESC, f.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> {
            Long projectId = rs.getLong("project_id");
            String fileName = rs.getString("original_name");
            return item(
                "FILE",
                String.valueOf(rs.getLong("id")),
                projectId,
                rs.getString("project_code"),
                rs.getString("project_name"),
                fileName,
                rs.getString("file_kind") + " · " + safe(rs.getString("version_no"), "V1"),
                rs.getString("process_status"),
                "data-steward-asset-detail",
                Map.of("projectId", projectId),
                linkedMap(
                    "tab", "files",
                    "fileKeyword", fileName,
                    "lastFileId", rs.getLong("id")
                )
            );
        });
    }

    private List<GlobalSearchItem> searchModels(MapSqlParameterSource params) {
        return jdbcTemplate.query("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.version_no,
                   lj.id AS job_id, lj.status AS job_status, lj.viewer_available
            FROM data_file_resources f
            JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
            LEFT JOIN visualization_lightweight_jobs lj ON lj.id = (
                SELECT MAX(j.id)
                FROM visualization_lightweight_jobs j
                WHERE j.project_id = f.project_id
                  AND j.file_id = f.id
                  AND j.deleted = 0
            )
            WHERE f.project_id IN (:projectIds)
              AND f.deleted = 0
              AND f.file_kind = 'MODEL'
              AND (
                f.original_name LIKE :like ESCAPE '\\\\'
                OR COALESCE(f.asset_uuid, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(f.version_no, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(lj.status, '') LIKE :like ESCAPE '\\\\'
              )
            ORDER BY COALESCE(lj.updated_at, f.updated_at) DESC, f.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> {
            Long projectId = rs.getLong("project_id");
            Long fileId = rs.getLong("file_id");
            Long jobId = rs.getObject("job_id") == null ? null : rs.getLong("job_id");
            boolean viewerAvailable = rs.getInt("viewer_available") == 1;
            String fileName = rs.getString("original_name");
            String routeName = jobId != null && viewerAvailable ? "glandar-model-preview" : "bim-collaboration";
            Map<String, Object> routeQuery = jobId != null && viewerAvailable
                ? linkedMap("projectId", projectId, "jobId", jobId, "fileName", fileName, "modelFileId", fileId)
                : Map.of("projectId", projectId);
            return item(
                "MODEL",
                String.valueOf(fileId),
                projectId,
                rs.getString("project_code"),
                rs.getString("project_name"),
                fileName,
                viewerAvailable ? "已轻量化，可预览" : "模型文件 · 待轻量化或处理中",
                safe(rs.getString("job_status"), "CATALOG_ONLY"),
                routeName,
                Map.of(),
                routeQuery
            );
        });
    }

    private List<GlobalSearchItem> searchDelivery(MapSqlParameterSource params) {
        return jdbcTemplate.query("""
            SELECT b.id, b.project_id, p.code AS project_code, p.name AS project_name,
                   b.view_type, b.review_status, b.binding_status,
                   f.original_name AS file_name,
                   dt.name AS deliverable_type_name
            FROM work_delivery_bindings b
            JOIN core_projects p ON p.id = b.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.id = b.file_resource_id AND f.deleted = 0
            JOIN masterdata_deliverable_types dt ON dt.id = b.deliverable_type_id AND dt.deleted = 0
            WHERE b.project_id IN (:projectIds)
              AND b.deleted = 0
              AND (
                COALESCE(f.original_name, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(dt.name, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(b.view_type, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(b.review_status, '') LIKE :like ESCAPE '\\\\'
              )
            ORDER BY b.updated_at DESC, b.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> {
            Long projectId = rs.getLong("project_id");
            String viewType = rs.getString("view_type");
            boolean drawing = "DRAWING".equalsIgnoreCase(viewType);
            return item(
                "DELIVERY",
                String.valueOf(rs.getLong("id")),
                projectId,
                rs.getString("project_code"),
                rs.getString("project_name"),
                safe(rs.getString("deliverable_type_name"), "交付事项"),
                safe(rs.getString("file_name"), "已挂接文件") + " · " + viewType,
                rs.getString("review_status"),
                drawing ? "project-work-drawing-delivery" : "project-work-document-delivery",
                Map.of("projectId", projectId),
                Map.of()
            );
        });
    }

    private List<GlobalSearchItem> searchRectifications(MapSqlParameterSource params) {
        return jdbcTemplate.query("""
            SELECT r.id, r.project_id, p.code AS project_code, p.name AS project_name,
                   r.title, r.reason, r.status, r.severity
            FROM work_rectifications r
            JOIN core_projects p ON p.id = r.project_id AND p.deleted = 0
            WHERE r.project_id IN (:projectIds)
              AND r.deleted = 0
              AND (
                COALESCE(r.title, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(r.reason, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(r.status, '') LIKE :like ESCAPE '\\\\'
                OR COALESCE(r.severity, '') LIKE :like ESCAPE '\\\\'
              )
            ORDER BY r.updated_at DESC, r.id DESC
            LIMIT :limit
            """, params, (rs, rowNum) -> {
            Long projectId = rs.getLong("project_id");
            return item(
                "RECTIFICATION",
                String.valueOf(rs.getLong("id")),
                projectId,
                rs.getString("project_code"),
                rs.getString("project_name"),
                rs.getString("title"),
                safe(rs.getString("reason"), "整改项"),
                rs.getString("status"),
                "project-work-rectifications",
                Map.of("projectId", projectId),
                Map.of("rectificationId", rs.getLong("id"))
            );
        });
    }

    private GlobalSearchItem item(
        String type,
        String id,
        Long projectId,
        String projectCode,
        String projectName,
        String title,
        String subtitle,
        String status,
        String routeName,
        Map<String, Object> routeParams,
        Map<String, Object> routeQuery
    ) {
        return new GlobalSearchItem(
            type,
            id,
            projectId,
            projectCode,
            projectName,
            title,
            subtitle,
            status,
            routeName,
            routeParams,
            routeQuery
        );
    }

    private static int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) return DEFAULT_LIMIT;
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private static String escapeLike(String keyword) {
        return keyword
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Map<String, Object> linkedMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

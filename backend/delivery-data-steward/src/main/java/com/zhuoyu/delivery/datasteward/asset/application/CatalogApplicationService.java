package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AuditContextResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogDirectoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogEventSummary;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogSearchRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogSearchResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogSearchResult;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogSearchSafety;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogAuthorityHealth;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionEvidence;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionProofResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.api.PageResponse;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogApplicationService {

    private static final int MAX_PHYSICAL_DIRECTORY_COUNT = 2_000;
    private static final int MAX_PHYSICAL_DIRECTORY_DEPTH = 4;

    private final BimAssetRepository bimAssetRepository;
    private final AssetPathMappingRepository pathMappingRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AuditLogApplicationService auditLogApplicationService;

    public CatalogApplicationService(
        BimAssetRepository bimAssetRepository,
        AssetPathMappingRepository pathMappingRepository,
        NamedParameterJdbcTemplate jdbcTemplate,
        SecurityPrincipalAccessor securityPrincipalAccessor,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.bimAssetRepository = bimAssetRepository;
        this.pathMappingRepository = pathMappingRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    // ===== catalog projects =====

    public List<CatalogProjectResponse> listCatalogProjects(Long userId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.code, p.name, p.project_stage, p.asset_source,
                   COUNT(f.id) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            LEFT JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (assetSource != null && !assetSource.isBlank()) {
            if (assetSource.endsWith("*")) {
                sql.append(" AND p.asset_source LIKE :assetSource");
                params.addValue("assetSource", assetSource.substring(0, assetSource.length() - 1) + "%");
            } else {
                sql.append(" AND p.asset_source = :assetSource");
                params.addValue("assetSource", assetSource);
            }
        }
        sql.append(" GROUP BY p.id, p.code, p.name, p.project_stage, p.asset_source ORDER BY p.id ASC");
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new CatalogProjectResponse(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("project_stage"),
            rs.getString("asset_source"),
            rs.getInt("file_count"),
            rs.getLong("total_size_bytes"),
            "HIGH"
        ));
    }

    // ===== catalog directories =====

    public List<CatalogDirectoryResponse> listCatalogDirectories(Long userId, Long projectId) {
        if (projectId == null) {
            return Collections.emptyList();
        }
        if (!hasProjectAccess(userId, projectId)) {
            return Collections.emptyList();
        }
        String directoryPathExpression = directoryPathExpression("f.logical_path");
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId);
        String sql = """
            SELECT %s AS directory_path,
                   f.project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   COUNT(f.id) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
              AND f.project_id = :projectId
              AND f.logical_path IS NOT NULL
              AND f.logical_path != ''
            GROUP BY %s, f.project_id, p.code, p.name
            ORDER BY directory_path ASC
            """.formatted(directoryPathExpression, directoryPathExpression);
        List<CatalogDirectoryResponse> directories = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String rawPath = rs.getString("directory_path");
            Long pId = rs.getLong("project_id");
            String projectCode = rs.getString("project_code");
            String projectName = rs.getString("project_name");
            String safePath = rawPath == null || rawPath.isBlank() ? "" : safeCatalogDirectoryPath(pId, projectCode, projectName, rawPath);
            return new CatalogDirectoryResponse(
                safePath,
                pId,
                projectCode,
                rs.getInt("file_count"),
                rs.getLong("total_size_bytes")
            );
        });
        Map<String, DirectoryAccumulator> byPath = new LinkedHashMap<>();
        for (CatalogDirectoryResponse directory : deduplicateDirectories(directories)) {
            putDirectory(byPath, directory.directoryPath(), directory.projectId(), directory.projectCode(),
                directory.fileCount(), directory.totalSizeBytes());
        }
        jdbcTemplate.query("""
            SELECT relative_path
            FROM data_nas_directory_records
            WHERE project_id = :projectId
              AND status = 'ACTIVE'
              AND deleted = 0
            ORDER BY relative_path ASC
            """, params, (rs, rowNum) -> rs.getString("relative_path")).forEach(relativePath ->
            putDirectory(byPath, normalizeCatalogDirectoryPath(relativePath), projectId,
                directories.stream().findFirst().map(CatalogDirectoryResponse::projectCode).orElse(null), 0, 0L));
        return byPath.values().stream()
            .sorted(Comparator.comparing(DirectoryAccumulator::directoryPath))
            .map(DirectoryAccumulator::toResponse)
            .toList();
    }

    // ===== catalog files =====

    public PageResponse<CatalogFileResponse> listCatalogFiles(
        Long userId, Long projectId, String keyword, String directoryPath, String fileExt,
        String fileKind, String disciplineCode, String version, String qualityIssue,
        boolean directOnly, int page, int pageSize
    ) {
        int offset = Math.max(0, (page - 1) * pageSize);
        int limit = Math.max(1, Math.min(pageSize, 200));

        StringBuilder sb = new StringBuilder("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline AS discipline_code,
                   f.version_no, f.size_bytes, f.checksum, f.storage_provider,
                   f.storage_uri, f.logical_path, f.process_status, f.confidence_level,
                   f.last_verified_at, f.updated_at, f.source_type, f.review_status
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        if (projectId != null) {
            sb.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND (p.code LIKE :likeKw OR p.name LIKE :likeKw OR f.original_name LIKE :likeKw OR f.storage_uri LIKE :likeKw)");
            params.addValue("likeKw", "%" + keyword.trim() + "%");
        }
        String normalizedDirectoryPath = normalizeCatalogDirectoryPath(directoryPath);
        if (directOnly) {
            appendDirectDirectoryFilter(sb, params, projectId, normalizedDirectoryPath);
        } else if (!normalizedDirectoryPath.isBlank()) {
            appendDirectoryFilter(sb, params, projectId, normalizedDirectoryPath);
        }
        if (fileExt != null && !fileExt.isBlank()) {
            String ext = fileExt.startsWith(".") ? fileExt.substring(1) : fileExt;
            sb.append(" AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) = :fileExt");
            params.addValue("fileExt", ext.toLowerCase());
        }
        if (fileKind != null && !fileKind.isBlank()) {
            sb.append(" AND f.file_kind = :fileKind");
            params.addValue("fileKind", fileKind.toUpperCase());
        }
        if (disciplineCode != null && !disciplineCode.isBlank()) {
            sb.append(" AND f.discipline = :disciplineCode");
            params.addValue("disciplineCode", disciplineCode);
        }
        if (version != null && !version.isBlank()) {
            sb.append(" AND LOWER(f.version_no) = LOWER(:version)");
            params.addValue("version", version.trim());
        }
        appendCatalogQualityFilter(sb, qualityIssue);

        // count total
        String countSql = "SELECT COUNT(1) FROM (" + sb + ") _cnt";
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        total = total == null ? 0L : total;

        sb.append(" ORDER BY f.updated_at DESC, f.id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        List<CatalogFileResponse> rows = jdbcTemplate.query(sb.toString(), params,
            catalogFileRowMapper(userId));
        return new PageResponse<>(rows, page, pageSize, total);
    }

    public CatalogSearchResponse searchCatalog(Long userId, CatalogSearchRequest request) {
        String queryId = UUID.randomUUID().toString().replace("-", "");
        int limit = catalogSearchLimit(request);
        int offset = catalogSearchOffset(request);
        List<Long> scopedProjectIds = resolveAccessibleProjectFilters(userId,
            request == null ? null : request.untrustedProjectFilters());
        if (scopedProjectIds.isEmpty()) {
            writeCatalogSearchAudit(userId, null, request, 0, false);
            return catalogSearchResponse(queryId, List.of(), null);
        }
        Set<String> indexEligibilityFilter = normalizedUpperSet(
            request.filters() == null ? null : request.filters().indexEligibility());
        if (!indexEligibilityFilter.isEmpty() && !indexEligibilityFilter.contains("CATALOG_ONLY")) {
            writeCatalogSearchAudit(userId, scopedProjectIds.get(0), request, 0, false);
            return catalogSearchResponse(queryId, List.of(), null);
        }

        String lifecycleExpression = lifecycleExpression("f");
        StringBuilder sql = new StringBuilder("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline AS discipline_code, f.version_no,
                   f.size_bytes, f.process_status, f.source_type, f.last_verified_at, f.updated_at,
                   f.logical_path,
                   LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS file_ext,
                   %s AS lifecycle_status
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND f.project_id IN (:projectIds)
            """.formatted(lifecycleExpression));
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectIds", scopedProjectIds);

        String query = request == null ? null : request.query();
        if (query != null && !query.isBlank()) {
            sql.append("""
                 AND (p.code LIKE :keyword
                   OR p.name LIKE :keyword
                   OR f.original_name LIKE :keyword
                   OR f.logical_path LIKE :keyword)
                """);
            params.addValue("keyword", "%" + query.trim() + "%");
        }

        Set<String> assetKinds = normalizedUpperSet(request.filters() == null ? null : request.filters().assetKind());
        if (!assetKinds.isEmpty() && !assetKinds.contains("FILE")) {
            sql.append(" AND f.file_kind IN (:assetKinds)");
            params.addValue("assetKinds", assetKinds);
        }

        Set<String> fileExts = normalizedLowerSet(request.filters() == null ? null : request.filters().fileExt());
        if (!fileExts.isEmpty()) {
            sql.append(" AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN (:fileExts)");
            params.addValue("fileExts", fileExts);
        }

        Set<String> lifecycleStatuses = normalizedLowerSet(
            request.filters() == null ? null : request.filters().lifecycleStatus());
        if (!lifecycleStatuses.isEmpty()) {
            sql.append(" AND ").append(lifecycleExpression).append(" IN (:lifecycleStatuses)");
            params.addValue("lifecycleStatuses", lifecycleStatuses);
        }

        sql.append(" ORDER BY f.updated_at DESC, f.id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", limit + 1);
        params.addValue("offset", offset);

        List<CatalogSearchResult> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            String displayPath = safeCatalogLogicalPath(
                rs.getLong("project_id"),
                rs.getString("project_code"),
                rs.getString("project_name"),
                rs.getString("logical_path"),
                rs.getString("original_name")
            );
            return new CatalogSearchResult(
                "file:" + rs.getLong("file_id"),
                rs.getString("file_kind"),
                "FileAssetView",
                rs.getLong("file_id"),
                null,
                rs.getLong("project_id"),
                rs.getString("project_code"),
                rs.getString("project_name"),
                rs.getString("original_name"),
                displayPath,
                pathHint(displayPath, rs.getString("original_name")),
                rs.getString("file_ext"),
                rs.getString("discipline_code"),
                rs.getString("version_no"),
                sizeBucket(rs.getLong("size_bytes")),
                rs.getString("lifecycle_status"),
                "catalog_only",
                false,
                catalogSearchMissingEvidence(rs.getString("file_ext"), rs.getString("file_kind")),
                toInstant(rs, "updated_at")
            );
        });

        boolean hasNext = rows.size() > limit;
        List<CatalogSearchResult> visibleRows = hasNext ? rows.subList(0, limit) : rows;
        writeCatalogSearchAudit(userId, scopedProjectIds.size() == 1 ? scopedProjectIds.get(0) : null,
            request, visibleRows.size(), hasNext);
        return catalogSearchResponse(
            queryId,
            visibleRows,
            hasNext ? String.valueOf(offset + limit) : null
        );
    }

    public Long resolveAccessibleProjectFilter(Long userId, List<String> projectFilters) {
        List<Long> ids = resolveAccessibleProjectFilters(userId, projectFilters);
        return ids.stream().findFirst().orElse(null);
    }

    private CatalogSearchResponse catalogSearchResponse(
        String queryId,
        List<CatalogSearchResult> results,
        String nextCursor
    ) {
        return new CatalogSearchResponse(
            queryId,
            queryId,
            true,
            "allowed",
            "catalog_only",
            results,
            nextCursor,
            new CatalogSearchSafety(false, false, false),
            catalogAuthorityHealth()
        );
    }

    private static CatalogAuthorityHealth catalogAuthorityHealth() {
        return new CatalogAuthorityHealth(
            "green",
            "staged",
            "orange",
            "openai_compatible_gateway_wrapped"
        );
    }

    private String pathHint(String displayPath, String fileName) {
        if (displayPath == null || displayPath.isBlank()) {
            return "path_not_exposable";
        }
        if (displayPath.equals(fileName)) {
            return "file_name_only";
        }
        int slashIndex = displayPath.lastIndexOf('/');
        if (slashIndex <= 0) {
            return "project_relative_path";
        }
        return "project_relative_directory:" + displayPath.substring(0, slashIndex);
    }

    private List<String> catalogSearchMissingEvidence(String fileExt, String fileKind) {
        ArrayList<String> reasons = new ArrayList<>();
        reasons.add("asset_catalog_only");
        String ext = fileExt == null ? "" : fileExt.toLowerCase(Locale.ROOT);
        if ("dwg".equals(ext) || "dxf".equals(ext)) {
            reasons.add("dwg_parse_evidence_missing");
        }
        if ("rvt".equals(ext) || "ifc".equals(ext) || "MODEL".equalsIgnoreCase(fileKind)) {
            reasons.add("model_parse_evidence_missing");
            reasons.add("component_evidence_missing");
        }
        if (Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx").contains(ext)) {
            reasons.add("full_text_evidence_missing");
        }
        return reasons;
    }

    private RowMapper<CatalogFileResponse> catalogFileRowMapper(Long userId) {
        return (rs, rowNum) -> {
            Long fileId = rs.getLong("file_id");
            Long pId = rs.getLong("project_id");
            String storageUri = rs.getString("storage_uri");
            PathVisibility pathVisibility = pathVisibility(userId, pId, storageUri);

            List<String> qualityFlags = buildQualityFlags(rs);

            String status = deriveStatus(rs.getString("process_status"), rs.getString("review_status"));
            String fileName = rs.getString("original_name");
            String ext = extensionOf(fileName);
            String safeLogicalPath = safeCatalogLogicalPath(
                pId,
                rs.getString("project_code"),
                rs.getString("project_name"),
                rs.getString("logical_path"),
                fileName
            );

            return new CatalogFileResponse(
                fileId, pId,
                rs.getString("project_code"), rs.getString("project_name"),
                fileName, ext,
                rs.getString("file_kind"),
                rs.getString("discipline_code"), rs.getString("discipline_code"),
                rs.getString("version_no"),
                rs.getLong("size_bytes"),
                rs.getString("checksum"),
                status,
                rs.getString("confidence_level"),
                rs.getString("storage_provider"),
                safeLogicalPath,
                pathVisibility.visible(), pathVisibility.reason(),
                qualityFlags,
                toInstant(rs, "last_verified_at"),
                toInstant(rs, "updated_at"),
                true, "FORMAL_ASSET_IN_SCOPE",
                List.of("fileName", "fileExt", "fileKind", "disciplineCode", "disciplineName",
                    "version", "sizeBytes", "checksum", "status", "confidenceLevel",
                    "storageProvider", "logicalPath", "qualityFlags", "lastVerifiedAt", "updatedAt")
            );
        };
    }

    // ===== catalog file detail =====

    public CatalogFileDetailResponse getCatalogFileDetail(Long userId, Long fileId) {
        List<CatalogFileDetailResponse> rows = jdbcTemplate.query("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline AS discipline_code,
                   f.version_no, f.size_bytes, f.checksum, f.storage_provider,
                   f.storage_uri, f.logical_path, f.process_status, f.confidence_level,
                   f.last_verified_at, f.updated_at, f.source_type, f.review_status
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.id = :fileId
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("fileId", fileId),
            (rs, rowNum) -> {
                String storageUri = rs.getString("storage_uri");
                Long pId = rs.getLong("project_id");
                PathVisibility pathVisibility = pathVisibility(userId, pId, storageUri);

                List<String> qualityFlags = buildQualityFlags(rs);
                String status = deriveStatus(rs.getString("process_status"), rs.getString("review_status"));
                String fileName = rs.getString("original_name");
                String ext = extensionOf(fileName);
                String safeLogicalPath = safeCatalogLogicalPath(
                    pId,
                    rs.getString("project_code"),
                    rs.getString("project_name"),
                    rs.getString("logical_path"),
                    fileName
                );

                return new CatalogFileDetailResponse(
                    rs.getLong("file_id"), pId,
                    rs.getString("project_code"), rs.getString("project_name"),
                    fileName, ext,
                    rs.getString("file_kind"),
                    rs.getString("discipline_code"), rs.getString("discipline_code"),
                    rs.getString("version_no"),
                    rs.getLong("size_bytes"),
                    rs.getString("checksum"),
                    status,
                    rs.getString("confidence_level"),
                    rs.getString("storage_provider"),
                    safeLogicalPath,
                    pathVisibility.visible() ? storageUri : null,
                    pathVisibility.visible(),
                    pathVisibility.reason(),
                    qualityFlags,
                    toInstant(rs, "last_verified_at"),
                    toInstant(rs, "updated_at"),
                    true, "FORMAL_ASSET_IN_SCOPE",
                    catalogContractFields(pathVisibility.visible())
                );
            });
        return rows.stream().findFirst().orElse(null);
    }

    // ===== audit context =====

    public AuditContextResponse getFileAuditContext(Long userId, Long fileId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("fileId", fileId);

        // Query as list to avoid EmptyResultDataAccessException when user has no access or file doesn't exist
        List<Long> projectIds = jdbcTemplate.query("""
            SELECT f.project_id
            FROM core_user_project_roles upr
            JOIN data_file_resources f ON f.project_id = upr.project_id AND f.deleted = 0 AND f.id = :fileId
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, params, (rs, rowNum) -> rs.getLong("project_id"));

        if (projectIds.isEmpty()) {
            return null;
        }
        Long projectId = projectIds.get(0);

        MapSqlParameterSource auditParams = new MapSqlParameterSource()
            .addValue("projectId", projectId);

        Integer total = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_audit_logs
            WHERE project_id = :projectId
            """, auditParams, Integer.class);
        int totalCount = total == null ? 0 : total;

        List<CatalogEventSummary> events = jdbcTemplate.query("""
            SELECT id, module_code, action_code, details_json, created_at
            FROM core_audit_logs
            WHERE project_id = :projectId
            ORDER BY created_at DESC
            LIMIT 20
            """, auditParams, (rs, rowNum) -> new CatalogEventSummary(
            rs.getLong("id"),
            rs.getString("module_code"),
            rs.getString("action_code"),
            rs.getString("details_json"),
            toInstant(rs, "created_at")
        ));

        return new AuditContextResponse(fileId, totalCount, events);
    }

    // ===== permission proof =====

    public PermissionProofResponse checkFilePermission(Long userId, Long fileId) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();

        // Check project access first
        List<Long> projectIds = jdbcTemplate.query("""
            SELECT f.project_id
            FROM data_file_resources f
            WHERE f.id = :fileId AND f.deleted = 0
            """, new MapSqlParameterSource("fileId", fileId),
            (rs, rowNum) -> rs.getLong("project_id"));

        if (projectIds.isEmpty()) {
            return new PermissionProofResponse(
                false, "DENIED", "USER", null,
                "FILE_NOT_FOUND", "文件不存在或已删除",
                List.of(new PermissionEvidence("file_id", "文件ID", String.valueOf(fileId), false)),
                traceId, now
            );
        }

        Long projectId = projectIds.get(0);
        Integer hasAccess = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            WHERE upr.user_id = :userId AND upr.project_id = :projectId AND upr.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId), Integer.class);

        boolean allowed = hasAccess != null && hasAccess > 0;

        if (allowed) {
            return new PermissionProofResponse(
                true, "ALLOWED", "USER", "PROJECT:" + projectId,
                "PROJECT_MEMBER", "当前用户是该项目成员，允许访问文件元数据",
                List.of(
                    new PermissionEvidence("project_membership", "项目成员关系", "project_id=" + projectId, false),
                    new PermissionEvidence("role", "角色", "PROJECT_MEMBER", false)
                ),
                traceId, now
            );
        }

        return new PermissionProofResponse(
            false, "DENIED", "USER", "PROJECT:" + projectId,
            "NO_PROJECT_ACCESS", "当前用户不是该项目成员，拒绝访问",
            List.of(new PermissionEvidence("project_membership", "项目成员关系", "无成员关系", false)),
            traceId, now
        );
    }

    public List<PermissionProofResponse> checkBulkPermission(Long userId, List<Long> fileIds, String actorType) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return fileIds.stream()
            .map(fileId -> checkFilePermission(userId, fileId))
            .toList();
    }

    public void writePermissionProofAudit(Long userId, Long fileId, PermissionProofResponse result) {
        try {
            jdbcTemplate.update("""
                INSERT INTO core_audit_logs (module_code, action_code, operator_id, project_id, trace_id, details_json, target_type, target_id)
                VALUES (:moduleCode, :actionCode, :operatorId, :projectId, :traceId, :detailsJson, :targetType, :targetId)
                """, new MapSqlParameterSource()
                .addValue("moduleCode", "data-steward")
                .addValue("actionCode", "PERMISSION_PROOF_" + result.decision())
                .addValue("operatorId", userId)
                .addValue("projectId", parseProjectId(result.projectScope()))
                .addValue("traceId", result.traceId())
                .addValue("detailsJson", "{\"reasonCode\":\"" + result.reasonCode() + "\",\"reasonText\":\"" + result.reasonText() + "\"}")
                .addValue("targetType", "FILE")
                .addValue("targetId", String.valueOf(fileId)));
        } catch (Exception ignored) {
            // audit write failure must not block permission check response
        }
    }

    // ===== helpers =====

    private Long parseProjectId(String projectScope) {
        if (projectScope == null) return null;
        try {
            return Long.parseLong(projectScope.replace("PROJECT:", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasProjectAccess(Long userId, Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.project_id = :projectId
              AND upr.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId), Integer.class);
        return count != null && count > 0;
    }

    private List<Long> resolveAccessibleProjectFilters(Long userId, List<String> projectFilters) {
        if (projectFilters == null || projectFilters.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> filters = projectFilters.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .limit(50)
            .toList();
        if (filters.isEmpty()) {
            return Collections.emptyList();
        }
        return jdbcTemplate.query("""
            SELECT DISTINCT p.id
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND (CAST(p.id AS CHAR) IN (:filters) OR p.code IN (:filters))
            ORDER BY p.id ASC
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("filters", filters), (rs, rowNum) -> rs.getLong("id"));
    }

    private int catalogSearchLimit(CatalogSearchRequest request) {
        Integer requested = request == null || request.page() == null ? null : request.page().limit();
        if (requested == null) {
            return 20;
        }
        return Math.max(1, Math.min(requested, 50));
    }

    private int catalogSearchOffset(CatalogSearchRequest request) {
        String cursor = request == null || request.page() == null ? null : request.page().cursor();
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursor.trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Set<String> normalizedUpperSet(List<String> values) {
        return normalizedSet(values, true);
    }

    private Set<String> normalizedLowerSet(List<String> values) {
        return normalizedSet(values, false);
    }

    private Set<String> normalizedSet(List<String> values, boolean upper) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(value -> value.replace(".", "").trim())
            .filter(value -> !value.isBlank())
            .map(value -> upper ? value.toUpperCase() : value.toLowerCase())
            .collect(Collectors.toCollection(HashSet::new));
    }

    private String lifecycleExpression(String alias) {
        return "CASE " +
            "WHEN UPPER(COALESCE(" + alias + ".process_status, '')) IN ('ARCHIVED') THEN 'archived' " +
            "WHEN UPPER(COALESCE(" + alias + ".process_status, '')) IN ('DELETE_REQUESTED', 'DELETED', 'PENDING_DELETE', 'QUARANTINED') THEN 'deleted_candidate' " +
            "WHEN " + alias + ".last_verified_at IS NULL AND UPPER(COALESCE(" + alias + ".source_type, '')) IN ('NAS_SCAN', 'REVIEW') THEN 'stale_unverified' " +
            "WHEN " + alias + ".process_status IS NULL THEN 'unknown' " +
            "ELSE 'active' END";
    }

    private String sizeBucket(long sizeBytes) {
        if (sizeBytes >= 1024L * 1024L * 1024L) {
            return "gte_1gb";
        }
        if (sizeBytes >= 100L * 1024L * 1024L) {
            return "gte_100mb";
        }
        if (sizeBytes >= 10L * 1024L * 1024L) {
            return "gte_10mb";
        }
        if (sizeBytes > 0) {
            return "lt_10mb";
        }
        return "unknown";
    }

    private void writeCatalogSearchAudit(
        Long userId,
        Long projectId,
        CatalogSearchRequest request,
        int resultCount,
        boolean hasNext
    ) {
        try {
            auditLogApplicationService.record(
                projectId,
                "data-steward",
                "agent.hermes.catalog.search",
                "AGENT_CATALOG_SEARCH",
                UUID.randomUUID().toString().replace("-", ""),
                userId,
                Map.of(
                    "queryLength", request == null || request.query() == null ? 0 : request.query().length(),
                    "projectScopeType", "generated_by_platform_gateway",
                    "projectFilterCount", request == null || request.untrustedProjectFilters() == null ? 0 : request.untrustedProjectFilters().size(),
                    "legacyProjectScopeProvided", request != null && request.legacyProjectScopeProvided(),
                    "resultCount", resultCount,
                    "hasNext", hasNext,
                    "mode", "catalog_only"
                )
            );
        } catch (RuntimeException ignored) {
            // Catalog preview must not fail only because audit is temporarily unavailable.
        }
    }

    private List<CatalogDirectoryResponse> mergePhysicalDirectories(
        Long projectId,
        List<CatalogDirectoryResponse> metadataDirectories
    ) {
        Map<String, DirectoryAccumulator> byPath = new LinkedHashMap<>();
        for (CatalogDirectoryResponse directory : metadataDirectories) {
            String path = normalizeCatalogDirectoryPath(directory.directoryPath());
            if (!path.isBlank()) {
                putDirectory(byPath, path, directory.projectId(), directory.projectCode(),
                    directory.fileCount(), directory.totalSizeBytes());
            }
        }

        for (PathMappingResponse mapping : pathMappingRepository.list(projectId, true)) {
            mergePhysicalMappingDirectories(byPath, mapping);
        }

        return byPath.values().stream()
            .sorted(Comparator.comparing(DirectoryAccumulator::directoryPath))
            .map(DirectoryAccumulator::toResponse)
            .toList();
    }

    private void mergePhysicalMappingDirectories(
        Map<String, DirectoryAccumulator> byPath,
        PathMappingResponse mapping
    ) {
        String rootPath = normalizeCatalogDirectoryPath(mapping.nasPath());
        if (rootPath.isBlank()) {
            return;
        }
        File root = new File(rootPath);
        if (!root.isDirectory()) {
            return;
        }

        Deque<PhysicalDirectoryVisit> stack = new ArrayDeque<>();
        stack.push(new PhysicalDirectoryVisit(root, 0));
        int visited = 0;
        while (!stack.isEmpty() && visited < MAX_PHYSICAL_DIRECTORY_COUNT) {
            PhysicalDirectoryVisit visit = stack.pop();
            File directory = visit.directory();
            visited += 1;
            String directoryPath = normalizeCatalogDirectoryPath(directory.getAbsolutePath());
            if (!directoryPath.isBlank()) {
                putDirectory(byPath, directoryPath, mapping.projectId(), mapping.projectCode(), 0, 0L);
            }
            if (visit.depth() >= MAX_PHYSICAL_DIRECTORY_DEPTH) {
                continue;
            }

            File[] children = directory.listFiles((child) ->
                child.isDirectory() && !java.nio.file.Files.isSymbolicLink(child.toPath()));
            if (children == null || children.length == 0) {
                continue;
            }
            Arrays.sort(children, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER).reversed());
            for (File child : children) {
                stack.push(new PhysicalDirectoryVisit(child, visit.depth() + 1));
            }
        }
    }

    private void putDirectory(
        Map<String, DirectoryAccumulator> byPath,
        String directoryPath,
        Long projectId,
        String projectCode,
        Integer fileCount,
        Long totalSizeBytes
    ) {
        DirectoryAccumulator accumulator = byPath.computeIfAbsent(directoryPath,
            path -> new DirectoryAccumulator(path, projectId, projectCode));
        accumulator.add(fileCount, totalSizeBytes);
    }

    private String normalizeCatalogDirectoryPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private PathVisibility pathVisibility(Long userId, Long projectId, String storageUri) {
        if (storageUri == null || storageUri.isBlank()) {
            return new PathVisibility(false, "PATH_EMPTY");
        }
        return new PathVisibility(false, "PATH_NOT_EXPOSABLE_CATALOG_ONLY");
    }

    private String safeCatalogLogicalPath(
        Long projectId,
        String projectCode,
        String projectName,
        String logicalPath,
        String fileName
    ) {
        if (logicalPath == null || logicalPath.isBlank()) {
            return fileName;
        }

        String normalized = normalizeProviderPath(logicalPath);
        String mappedRelativePath = relativePathByMapping(projectId, normalized);
        if (!mappedRelativePath.isBlank()) {
            return mappedRelativePath;
        }

        String projectRelativePath = relativePathByProjectMarker(normalized, projectCode, projectName);
        if (!projectRelativePath.isBlank()) {
            return projectRelativePath;
        }

        if (looksLikePhysicalPath(normalized)) {
            return fileName;
        }
        return trimLeadingSlash(normalized);
    }

    private String safeCatalogDirectoryPath(
        Long projectId,
        String projectCode,
        String projectName,
        String directoryPath
    ) {
        if (directoryPath == null || directoryPath.isBlank()) {
            return "";
        }
        String normalized = normalizeProviderPath(directoryPath);
        String mappedRelativePath = relativePathByMapping(projectId, normalized);
        if (!mappedRelativePath.isBlank()) {
            return mappedRelativePath;
        }
        String projectRelativePath = relativePathByProjectMarker(normalized, projectCode, projectName);
        if (!projectRelativePath.isBlank()) {
            return projectRelativePath;
        }
        if (looksLikePhysicalPath(normalized)) {
            String[] segments = trimLeadingSlash(normalized).split("/");
            String lastSegment = segments.length > 0 ? segments[segments.length - 1] : "";
            return lastSegment.isBlank() ? "" : lastSegment;
        }
        return trimLeadingSlash(normalized);
    }

    private List<CatalogDirectoryResponse> deduplicateDirectories(List<CatalogDirectoryResponse> directories) {
        Map<String, CatalogDirectoryResponse> byPath = new LinkedHashMap<>();
        for (CatalogDirectoryResponse dir : directories) {
            String path = dir.directoryPath() == null ? "" : dir.directoryPath();
            CatalogDirectoryResponse existing = byPath.get(path);
            if (existing != null) {
                byPath.put(path, new CatalogDirectoryResponse(
                    path,
                    existing.projectId(),
                    existing.projectCode(),
                    existing.fileCount() + dir.fileCount(),
                    existing.totalSizeBytes() + dir.totalSizeBytes()
                ));
            } else {
                byPath.put(path, dir);
            }
        }
        return new ArrayList<>(byPath.values());
    }

    private String relativePathByMapping(Long projectId, String normalizedPath) {
        if (projectId == null || normalizedPath.isBlank()) {
            return "";
        }
        return pathMappingRepository.list(projectId, true).stream()
            .map(PathMappingResponse::nasPath)
            .filter(Objects::nonNull)
            .map(this::normalizeProviderPath)
            .filter(root -> !root.isBlank())
            .sorted((left, right) -> Integer.compare(right.length(), left.length()))
            .filter(root -> samePathOrChild(normalizedPath, root))
            .findFirst()
            .map(root -> trimLeadingSlash(normalizedPath.substring(Math.min(root.length(), normalizedPath.length()))))
            .filter(relative -> !relative.isBlank())
            .orElse("");
    }

    private String relativePathByProjectMarker(String normalizedPath, String projectCode, String projectName) {
        List<String> segments = Arrays.stream(trimLeadingSlash(normalizedPath).split("/"))
            .filter(segment -> !segment.isBlank())
            .toList();
        if (segments.isEmpty()) {
            return "";
        }
        for (int index = 0; index < segments.size(); index += 1) {
            String segment = segments.get(index);
            boolean matchesCode = projectCode != null && !projectCode.isBlank() && segment.contains(projectCode);
            boolean matchesName = projectName != null && !projectName.isBlank() && segment.contains(projectName);
            if ((matchesCode || matchesName) && index + 1 < segments.size()) {
                return String.join("/", segments.subList(index + 1, segments.size()));
            }
        }
        return "";
    }

    private String normalizeProviderPath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = normalizeCatalogDirectoryPath(path).replace('\\', '/');
        if (normalized.startsWith("nas://")) {
            normalized = normalized.substring("nas://".length());
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
        }
        return normalizeCatalogDirectoryPath(normalized);
    }

    private boolean samePathOrChild(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private boolean looksLikePhysicalPath(String path) {
        return path.startsWith("/Volumes/")
            || path.startsWith("/Users/")
            || path.startsWith("/tmp/")
            || path.startsWith("/private/")
            || path.startsWith("/var/")
            || path.startsWith("/mnt/")
            || path.startsWith("/data/")
            || path.startsWith("//")
            || path.startsWith("\\\\");
    }

    private String trimLeadingSlash(String value) {
        String next = value == null ? "" : value.trim().replace('\\', '/');
        while (next.startsWith("/")) {
            next = next.substring(1);
        }
        return next;
    }

    private List<String> catalogContractFields(boolean storagePathVisible) {
        List<String> fields = new ArrayList<>(List.of("fileName", "fileExt", "fileKind",
            "disciplineCode", "disciplineName", "version", "sizeBytes", "checksum", "status",
            "confidenceLevel", "storageProvider", "logicalPath", "qualityFlags",
            "lastVerifiedAt", "updatedAt"));
        if (storagePathVisible) {
            fields.add("storagePath");
        }
        return fields;
    }

    private record PathVisibility(Boolean visible, String reason) {
    }

    private record PhysicalDirectoryVisit(File directory, int depth) {
    }

    private static final class DirectoryAccumulator {
        private final String directoryPath;
        private final Long projectId;
        private final String projectCode;
        private int fileCount;
        private long totalSizeBytes;

        private DirectoryAccumulator(String directoryPath, Long projectId, String projectCode) {
            this.directoryPath = directoryPath;
            this.projectId = projectId;
            this.projectCode = projectCode;
        }

        private String directoryPath() {
            return directoryPath;
        }

        private void add(Integer nextFileCount, Long nextTotalSizeBytes) {
            fileCount += nextFileCount == null ? 0 : nextFileCount;
            totalSizeBytes += nextTotalSizeBytes == null ? 0L : nextTotalSizeBytes;
        }

        private CatalogDirectoryResponse toResponse() {
            return new CatalogDirectoryResponse(directoryPath, projectId, projectCode, fileCount, totalSizeBytes);
        }
    }

    private List<String> buildQualityFlags(ResultSet rs) throws SQLException {
        List<String> flags = new ArrayList<>();
        String checksum = rs.getString("checksum");
        if (checksum == null || checksum.isBlank()) flags.add("MISSING_CHECKSUM");
        String confidence = rs.getString("confidence_level");
        if (confidence == null || confidence.isBlank()) flags.add("MISSING_CONFIDENCE");
        String discipline = rs.getString("discipline_code");
        if (discipline == null || discipline.isBlank() || "OTHER".equalsIgnoreCase(discipline)) {
            flags.add("MISSING_DISCIPLINE");
        }
        String version = rs.getString("version_no");
        if (version == null || version.isBlank()) flags.add("MISSING_VERSION");
        String storageUri = rs.getString("storage_uri");
        if (storageUri == null || storageUri.isBlank()) flags.add("MISSING_STORAGE_PATH");
        long size = rs.getLong("size_bytes");
        if (size <= 0) flags.add("ZERO_SIZE_FILE");
        return flags;
    }

    private String deriveStatus(String processStatus, String reviewStatus) {
        if ("PENDING_REVIEW".equalsIgnoreCase(reviewStatus)) return "PENDING_REVIEW";
        if ("PROCESSED".equalsIgnoreCase(processStatus)) return "ACTIVE";
        if ("FAILED".equalsIgnoreCase(processStatus)) return "FAILED";
        return processStatus == null ? "UNKNOWN" : processStatus.toUpperCase();
    }

    private String directoryPathExpression(String column) {
        return "CASE " +
            "WHEN " + column + " IS NULL OR " + column + " = '' THEN '' " +
            "WHEN LOCATE('/', REVERSE(" + column + ")) > 0 THEN LEFT(" + column + ", CHAR_LENGTH(" + column + ") - LOCATE('/', REVERSE(" + column + "))) " +
            "ELSE '' END";
    }

    private void appendDirectoryFilter(StringBuilder sb, MapSqlParameterSource params, Long projectId, String safeDirectoryPath) {
        List<String> nasRoots = pathMappingRepository.list(projectId, true).stream()
            .map(PathMappingResponse::nasPath)
            .filter(Objects::nonNull)
            .map(this::normalizeProviderPath)
            .filter(root -> !root.isBlank())
            .toList();

        sb.append(" AND (");
        // Match against raw logical_path using known NAS roots
        for (int i = 0; i < nasRoots.size(); i++) {
            String rawPrefix = nasRoots.get(i) + "/" + safeDirectoryPath;
            String paramFull = "dirPathRaw" + i;
            String paramPrefix = "dirPathRawPrefix" + i;
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("(f.logical_path = :").append(paramFull)
              .append(" OR f.logical_path LIKE :").append(paramPrefix).append(")");
            params.addValue(paramFull, rawPrefix);
            params.addValue(paramPrefix, rawPrefix + "/%");
        }
        // Fallback: suffix match on the safe directory path (handles project-marker-based conversion)
        if (!nasRoots.isEmpty()) {
            sb.append(" OR ");
        }
        sb.append("f.logical_path LIKE :dirPathSuffix OR f.logical_path LIKE :dirPathSuffixSlash");
        params.addValue("dirPathSuffix", "%/" + safeDirectoryPath);
        params.addValue("dirPathSuffixSlash", "%/" + safeDirectoryPath + "/%");
        sb.append(")");
    }

    private void appendDirectDirectoryFilter(StringBuilder sb, MapSqlParameterSource params, Long projectId, String safeDirectoryPath) {
        String parentExpression = directoryPathExpression("f.logical_path");
        List<String> nasRoots = projectId == null ? List.of() : pathMappingRepository.list(projectId, true).stream()
            .map(PathMappingResponse::nasPath)
            .filter(Objects::nonNull)
            .map(this::normalizeProviderPath)
            .filter(root -> !root.isBlank())
            .toList();

        sb.append(" AND (");
        boolean appended = false;

        if (safeDirectoryPath.isBlank()) {
            sb.append(parentExpression).append(" = :directRootPath");
            params.addValue("directRootPath", "");
            appended = true;
            for (int i = 0; i < nasRoots.size(); i++) {
                String param = "directRootRaw" + i;
                sb.append(" OR ").append(parentExpression).append(" = :").append(param);
                params.addValue(param, nasRoots.get(i));
            }
        } else {
            for (int i = 0; i < nasRoots.size(); i++) {
                String param = "directRaw" + i;
                if (appended) {
                    sb.append(" OR ");
                }
                sb.append(parentExpression).append(" = :").append(param);
                params.addValue(param, nasRoots.get(i) + "/" + safeDirectoryPath);
                appended = true;
            }
            if (appended) {
                sb.append(" OR ");
            }
            sb.append(parentExpression).append(" = :directSafePath")
              .append(" OR ").append(parentExpression).append(" LIKE :directSafePathSuffix");
            params.addValue("directSafePath", safeDirectoryPath);
            params.addValue("directSafePathSuffix", "%/" + safeDirectoryPath);
        }

        sb.append(")");
    }

    private void appendCatalogQualityFilter(StringBuilder sb, String qualityIssue) {
        String issue = qualityIssue == null || qualityIssue.isBlank() ? null : qualityIssue.trim();
        if (issue == null) return;
        switch (issue.toUpperCase()) {
            case "MISSING_CHECKSUM" -> sb.append(" AND (f.checksum IS NULL OR f.checksum = '')");
            case "MISSING_CONFIDENCE" -> sb.append(" AND (f.confidence_level IS NULL OR f.confidence_level = '')");
            case "MISSING_DISCIPLINE" -> sb.append(" AND (f.discipline IS NULL OR f.discipline = '' OR f.discipline = 'OTHER')");
            case "MISSING_VERSION" -> sb.append(" AND (f.version_no IS NULL OR f.version_no = '')");
            case "MISSING_STORAGE_PATH" -> sb.append(" AND (f.storage_uri IS NULL OR f.storage_uri = '')");
            case "ZERO_SIZE_FILE" -> sb.append(" AND COALESCE(f.size_bytes, 0) <= 0");
            default -> { /* unknown filter ignored */ }
        }
    }

    private static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static Instant toInstant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}

package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AuditContextResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogDirectoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogEventSummary;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileDetailResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogFileResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CatalogProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionEvidence;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PermissionProofResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.api.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogApplicationService {

    private final BimAssetRepository bimAssetRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SecurityPrincipalAccessor securityPrincipalAccessor;

    public CatalogApplicationService(
        BimAssetRepository bimAssetRepository,
        NamedParameterJdbcTemplate jdbcTemplate,
        SecurityPrincipalAccessor securityPrincipalAccessor
    ) {
        this.bimAssetRepository = bimAssetRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.securityPrincipalAccessor = securityPrincipalAccessor;
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
        String directoryPathExpression = directoryPathExpression("f.logical_path");
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId);
        String sql = """
            SELECT %s AS directory_path,
                   f.project_id,
                   p.code AS project_code,
                   COUNT(f.id) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
              AND f.project_id = :projectId
              AND f.logical_path IS NOT NULL
              AND f.logical_path != ''
            GROUP BY %s, f.project_id, p.code
            ORDER BY directory_path ASC
            """.formatted(directoryPathExpression, directoryPathExpression);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new CatalogDirectoryResponse(
            rs.getString("directory_path"),
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getInt("file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    // ===== catalog files =====

    public PageResponse<CatalogFileResponse> listCatalogFiles(
        Long userId, Long projectId, String keyword, String directoryPath, String fileExt,
        String fileKind, String disciplineCode, String version, String qualityIssue,
        int page, int pageSize
    ) {
        int offset = Math.max(0, (page - 1) * pageSize);
        int limit = Math.max(1, Math.min(pageSize, 200));
        String directoryPathExpression = directoryPathExpression("f.logical_path");

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
        if (directoryPath != null && !directoryPath.isBlank()) {
            sb.append(" AND ").append(directoryPathExpression).append(" = :directoryPath");
            params.addValue("directoryPath", directoryPath.trim());
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
                rs.getString("logical_path"),
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
                    rs.getString("logical_path"),
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

    private PathVisibility pathVisibility(Long userId, Long projectId, String storageUri) {
        if (storageUri == null || storageUri.isBlank()) {
            return new PathVisibility(false, "PATH_EMPTY");
        }
        if (isProjectAdmin(userId, projectId)) {
            return new PathVisibility(true, "PROJECT_ADMIN");
        }
        return new PathVisibility(false, "PATH_HIDDEN_BY_PERMISSION");
    }

    private boolean isProjectAdmin(Long userId, Long projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.project_id = :projectId
              AND upr.deleted = 0
              AND r.code = 'PROJECT_ADMIN'
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId), Integer.class);
        return count != null && count > 0;
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

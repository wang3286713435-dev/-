package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByDiscipline;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByFileKind;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ModelAssetResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
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
public class BimAssetRepository {

    private static final RowMapper<AssetProjectResponse> PROJECT_ROW_MAPPER = BimAssetRepository::mapProject;
    private static final RowMapper<ModelAssetResponse> MODEL_ROW_MAPPER = BimAssetRepository::mapModel;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BimAssetRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long upsertProject(
        String code,
        String name,
        String industryType,
        String projectStage,
        String projectManagerName,
        String ownerOrgName,
        String assetSource,
        Long operatorId
    ) {
        Optional<Long> existingId = findProjectIdByCode(code);
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE core_projects
                SET name = COALESCE(:name, name),
                    industry_type = COALESCE(:industryType, industry_type),
                    project_stage = COALESCE(:projectStage, project_stage),
                    project_manager_name = COALESCE(:projectManagerName, project_manager_name),
                    owner_org_name = COALESCE(:ownerOrgName, owner_org_name),
                    asset_status = 'ACTIVE',
                    asset_source = COALESCE(:assetSource, asset_source),
                    updated_by = :operatorId
                WHERE id = :projectId AND deleted = 0
                """, new MapSqlParameterSource()
                .addValue("projectId", existingId.get())
                .addValue("name", name)
                .addValue("industryType", industryType)
                .addValue("projectStage", projectStage)
                .addValue("projectManagerName", projectManagerName)
                .addValue("ownerOrgName", ownerOrgName)
                .addValue("assetSource", assetSource)
                .addValue("operatorId", operatorId));
            return existingId.get();
        }
        String sql = """
            INSERT INTO core_projects (
                code, name, industry_type, project_stage, project_manager_name,
                owner_org_name, asset_status, asset_source, created_by, updated_by
            ) VALUES (
                :code, :name, COALESCE(:industryType, 'OTHER'), COALESCE(:projectStage, 'UNKNOWN'),
                COALESCE(:projectManagerName, ''),
                COALESCE(:ownerOrgName, ''),
                'ACTIVE', COALESCE(:assetSource, 'API'), :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("code", code)
            .addValue("name", name)
            .addValue("industryType", industryType)
            .addValue("projectStage", projectStage)
            .addValue("projectManagerName", projectManagerName)
            .addValue("ownerOrgName", ownerOrgName)
            .addValue("assetSource", assetSource)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void grantProjectAdmin(Long userId, Long projectId, Long operatorId) {
        Long roleId = jdbcTemplate.queryForObject("""
            SELECT id FROM core_roles WHERE code = 'PROJECT_ADMIN' AND deleted = 0
            """, new MapSqlParameterSource(), Long.class);
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles
            WHERE user_id = :userId AND project_id = :projectId AND role_id = :roleId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("roleId", roleId), Integer.class);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update("""
            INSERT INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by)
            VALUES (:userId, :projectId, :roleId, :operatorId, :operatorId)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("roleId", roleId)
            .addValue("operatorId", operatorId));
    }

    public Optional<Long> findProjectIdByCode(String code) {
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM core_projects
            WHERE code = :code AND deleted = 0
            """, new MapSqlParameterSource("code", code), (rs, rowNum) -> rs.getLong("id"));
        return rows.stream().findFirst();
    }

    public List<AssetProjectResponse> listProjects(Long userId, String keyword) {
        return listProjects(userId, keyword, null);
    }

    public List<AssetProjectResponse> listProjects(Long userId, String keyword, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("keyword", blankToNull(keyword))
            .addValue("likeKeyword", likeKeyword(keyword));
        StringBuilder sql = new StringBuilder("""
            SELECT p.id,
                   p.code,
                   p.name,
                   p.industry_type,
                   p.project_stage,
                   p.project_manager_name,
                   p.asset_status,
                   p.asset_source,
                   COALESCE(fa.model_count, 0) AS model_count,
                   COALESCE(fa.file_count, 0) AS file_count,
                   COALESCE(fa.total_size_bytes, 0) AS total_size_bytes,
                   fa.last_model_updated_at,
                   fa.last_asset_verified_at,
                   fa.dominant_file_kinds,
                   sa.last_scan_at,
                   COALESCE(pm.path_mapping_count, 0) AS path_mapping_count,
                   CASE WHEN COALESCE(md.master_data_count, 0) > 0 THEN 1 ELSE 0 END AS has_master_data,
                   CASE WHEN COALESCE(ds.delivery_standard_count, 0) > 0 THEN 1 ELSE 0 END AS has_delivery_standard,
                   CASE WHEN COALESCE(wb.binding_count, 0) > 0 THEN 1 ELSE 0 END AS has_delivery_governance,
                   p.updated_at AS project_updated_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            LEFT JOIN (
                SELECT project_id,
                       COUNT(1) AS file_count,
                       SUM(CASE WHEN file_kind = 'MODEL' THEN 1 ELSE 0 END) AS model_count,
                       COALESCE(SUM(size_bytes), 0) AS total_size_bytes,
                       MAX(CASE WHEN file_kind = 'MODEL' THEN updated_at ELSE NULL END) AS last_model_updated_at,
                       MAX(last_verified_at) AS last_asset_verified_at,
                       GROUP_CONCAT(DISTINCT file_kind ORDER BY file_kind SEPARATOR ',') AS dominant_file_kinds
                FROM data_file_resources
                WHERE deleted = 0
                GROUP BY project_id
            ) fa ON fa.project_id = p.id
            LEFT JOIN (
                SELECT project_id, MAX(COALESCE(completed_at, started_at, updated_at)) AS last_scan_at
                FROM data_asset_scan_tasks
                WHERE deleted = 0
                GROUP BY project_id
            ) sa ON sa.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS path_mapping_count
                FROM data_asset_project_path_mappings
                WHERE deleted = 0 AND enabled = 1
                GROUP BY project_id
            ) pm ON pm.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS master_data_count
                FROM (
                    SELECT project_id FROM masterdata_section_nodes WHERE deleted = 0
                    UNION ALL
                    SELECT project_id FROM masterdata_node_types WHERE deleted = 0
                ) rows_for_master_data
                GROUP BY project_id
            ) md ON md.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS delivery_standard_count
                FROM (
                    SELECT project_id FROM masterdata_deliverable_definitions WHERE deleted = 0
                    UNION ALL
                    SELECT project_id FROM masterdata_directory_templates WHERE deleted = 0
                ) rows_for_delivery_standard
                GROUP BY project_id
            ) ds ON ds.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS binding_count
                FROM work_delivery_bindings
                WHERE deleted = 0
                GROUP BY project_id
            ) wb ON wb.project_id = p.id
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND (
                  :keyword IS NULL
                  OR p.code LIKE :likeKeyword
                  OR p.name LIKE :likeKeyword
                  OR p.project_manager_name LIKE :likeKeyword
              )
            """);
        appendAssetSourceFilter(sql, params, "p", assetSource);
        sql.append("""
            ORDER BY total_size_bytes DESC, p.id DESC
            """);
        return jdbcTemplate.query(sql.toString(), params, PROJECT_ROW_MAPPER);
    }

    public Long insertFileAsset(
        Long projectId,
        String originalName,
        String fileKind,
        Long sizeBytes,
        String storageUri,
        String storageProvider,
        String storageKey,
        String digest,
        String discipline,
        String versionNo,
        String sourceType,
        String confidenceLevel,
        Long operatorId
    ) {
        return upsertFileAsset(projectId, originalName, fileKind, sizeBytes, storageUri,
            storageProvider, storageKey, digest, discipline, versionNo, sourceType,
            "APPROVED", confidenceLevel, operatorId);
    }

    public Long upsertFileAsset(
        Long projectId,
        String originalName,
        String fileKind,
        Long sizeBytes,
        String storageUri,
        String storageProvider,
        String storageKey,
        String digest,
        String discipline,
        String versionNo,
        String sourceType,
        String reviewStatus,
        String confidenceLevel,
        Long operatorId
    ) {
        Optional<Long> existingId = findFileIdByStorageUri(projectId, storageUri);
        if (existingId.isPresent()) {
            jdbcTemplate.update("""
                UPDATE data_file_resources
                SET original_name = :originalName,
                    file_kind = :fileKind,
                    size_bytes = :sizeBytes,
                    storage_provider = :storageProvider,
                    storage_key = :storageKey,
                    source_path_digest = :digest,
                    business_tag = :discipline,
                    discipline = :discipline,
                    source_type = :sourceType,
                    version_no = :versionNo,
                    process_status = 'PROCESSED',
                    processed_at = CURRENT_TIMESTAMP,
                    last_verified_at = CURRENT_TIMESTAMP,
                    review_status = :reviewStatus,
                    confidence_level = :confidenceLevel,
                    updated_by = :operatorId
                WHERE id = :fileId AND deleted = 0
                """, new MapSqlParameterSource()
                .addValue("fileId", existingId.get())
                .addValue("originalName", originalName)
                .addValue("fileKind", fileKind != null && !fileKind.isBlank() ? fileKind.toUpperCase() : "OTHER")
                .addValue("sizeBytes", sizeBytes)
                .addValue("storageProvider", storageProvider)
                .addValue("storageKey", storageKey)
                .addValue("digest", digest)
                .addValue("discipline", discipline)
                .addValue("sourceType", sourceType)
                .addValue("versionNo", versionNo)
                .addValue("reviewStatus", reviewStatus)
                .addValue("confidenceLevel", confidenceLevel)
                .addValue("operatorId", operatorId));
            return existingId.get();
        }
        String kind = fileKind != null && !fileKind.isBlank() ? fileKind.toUpperCase() : "OTHER";
        String sql = """
            INSERT INTO data_file_resources (
                project_id, original_name, file_kind, mime_type, size_bytes, storage_uri,
                storage_provider, storage_key, logical_path, source_path_digest, business_tag,
                discipline, source_type, version_no, process_status, processed_at,
                last_verified_at, review_status, confidence_level, created_by, updated_by
            ) VALUES (
                :projectId, :originalName, :fileKind, 'application/octet-stream', :sizeBytes, :storageUri,
                :storageProvider, :storageKey, :storageKey, :digest, :discipline,
                :discipline, :sourceType, :versionNo, 'PROCESSED', CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, :reviewStatus, :confidenceLevel, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("originalName", originalName)
            .addValue("fileKind", kind)
            .addValue("sizeBytes", sizeBytes)
            .addValue("storageUri", storageUri)
            .addValue("storageProvider", storageProvider)
            .addValue("storageKey", storageKey)
            .addValue("digest", digest)
            .addValue("discipline", discipline)
            .addValue("sourceType", sourceType)
            .addValue("versionNo", versionNo)
            .addValue("reviewStatus", reviewStatus)
            .addValue("confidenceLevel", confidenceLevel)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void verifyExistingModel(Long projectId, String storageUri, Long sizeBytes, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET size_bytes = :sizeBytes,
                process_status = 'PROCESSED',
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :operatorId
            WHERE project_id = :projectId AND storage_uri = :storageUri AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("storageUri", storageUri)
            .addValue("sizeBytes", sizeBytes)
            .addValue("operatorId", operatorId));
    }

    public boolean existsModelStorageUri(Long projectId, String storageUri) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId AND storage_uri = :storageUri AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("storageUri", storageUri), Integer.class);
        return count != null && count > 0;
    }

    public Optional<Long> findFileIdByStorageUri(Long projectId, String storageUri) {
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM data_file_resources
            WHERE project_id = :projectId
              AND storage_uri = :storageUri
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("storageUri", storageUri), (rs, rowNum) -> rs.getLong("id"));
        return rows.stream().findFirst();
    }

    public List<ModelAssetResponse> listModels(Long userId, String keyword, Long projectId, String discipline) {
        return jdbcTemplate.query("""
            SELECT f.id AS file_id,
                   p.id AS project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   f.original_name,
                   f.size_bytes,
                   f.version_no,
                   f.process_status,
                   f.storage_provider,
                   f.storage_key,
                   f.discipline,
                   f.source_type,
                   f.last_verified_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND (:projectId IS NULL OR p.id = :projectId)
              AND (:discipline IS NULL OR f.discipline = :discipline)
              AND (
                  :keyword IS NULL
                  OR p.code LIKE :likeKeyword
                  OR p.name LIKE :likeKeyword
                  OR f.original_name LIKE :likeKeyword
                  OR f.storage_key LIKE :likeKeyword
              )
            ORDER BY f.updated_at DESC, f.id DESC
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("discipline", blankToNull(discipline))
            .addValue("keyword", blankToNull(keyword))
            .addValue("likeKeyword", likeKeyword(keyword)), MODEL_ROW_MAPPER);
    }

    public Optional<ModelAssetResponse> findModel(Long userId, Long projectId, Long fileId) {
        List<ModelAssetResponse> rows = jdbcTemplate.query("""
            SELECT f.id AS file_id,
                   p.id AS project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   f.original_name,
                   f.size_bytes,
                   f.version_no,
                   f.process_status,
                   f.storage_provider,
                   f.storage_key,
                   f.discipline,
                   f.source_type,
                   f.last_verified_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND p.id = :projectId
              AND f.id = :fileId
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("fileId", fileId), MODEL_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int countAssetProjects(Long userId) {
        return countAssetProjects(userId, null, null);
    }

    public int countAssetProjects(Long userId, Long projectId) {
        return countAssetProjects(userId, projectId, null);
    }

    public int countAssetProjects(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(DISTINCT p.id)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND p.id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count == null ? 0 : count;
    }

    public int countModelFiles(Long userId) {
        return countModelFiles(userId, null, null);
    }

    public int countModelFiles(Long userId, Long projectId) {
        return countModelFiles(userId, projectId, null);
    }

    public int countModelFiles(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count == null ? 0 : count;
    }

    public long totalAllFileSize(Long userId) {
        return totalAllFileSize(userId, null, null);
    }

    public long totalAllFileSize(Long userId, Long projectId) {
        return totalAllFileSize(userId, projectId, null);
    }

    public long totalAllFileSize(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COALESCE(SUM(f.size_bytes), 0)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Long total = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        return total == null ? 0L : total;
    }

    public List<CapacityByDiscipline> capacityByDiscipline(Long userId) {
        return capacityByDiscipline(userId, null, null);
    }

    public List<CapacityByDiscipline> capacityByDiscipline(Long userId, Long projectId) {
        return capacityByDiscipline(userId, projectId, null);
    }

    public List<CapacityByDiscipline> capacityByDiscipline(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COALESCE(f.discipline, '未分类') AS discipline,
                   COUNT(1) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        sql.append("""
             GROUP BY COALESCE(f.discipline, '未分类')
             ORDER BY total_size_bytes DESC
            """);
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new CapacityByDiscipline(
            rs.getString("discipline"),
            rs.getInt("file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    public List<CapacityByProject> topProjectCapacity(Long userId) {
        return topProjectCapacity(userId, null, null);
    }

    public List<CapacityByProject> topProjectCapacity(Long userId, Long projectId) {
        return topProjectCapacity(userId, projectId, null);
    }

    public List<CapacityByProject> topProjectCapacity(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT p.id AS project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   COUNT(f.id) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND p.id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        sql.append("""
             GROUP BY p.id, p.code, p.name
             ORDER BY total_size_bytes DESC
             LIMIT 10
            """);
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new CapacityByProject(
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getInt("file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    private static AssetProjectResponse mapProject(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastModelUpdatedAt = rs.getTimestamp("last_model_updated_at");
        Timestamp lastAssetVerifiedAt = rs.getTimestamp("last_asset_verified_at");
        Timestamp lastScanAt = rs.getTimestamp("last_scan_at");
        Timestamp projectUpdatedAt = rs.getTimestamp("project_updated_at");
        String assetSource = rs.getString("asset_source");
        String confidentialityLevel = projectConfidentialityLevel(assetSource);
        String indexEligibility = projectIndexEligibility(assetSource);
        int fileCount = rs.getInt("file_count");
        boolean hasMasterData = rs.getInt("has_master_data") > 0;
        boolean hasDeliveryStandard = rs.getInt("has_delivery_standard") > 0;
        boolean hasDeliveryGovernance = rs.getInt("has_delivery_governance") > 0;
        int pathMappingCount = rs.getInt("path_mapping_count");
        Instant lastSeenAt = lastAssetVerifiedAt == null
            ? (lastModelUpdatedAt == null
                ? (projectUpdatedAt == null ? null : projectUpdatedAt.toInstant())
                : lastModelUpdatedAt.toInstant())
            : lastAssetVerifiedAt.toInstant();
        Long projectId = rs.getLong("id");
        return new AssetProjectResponse(
            projectId,
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("industry_type"),
            rs.getString("project_stage"),
            rs.getString("project_manager_name"),
            rs.getString("asset_status"),
            assetSource,
            rs.getInt("model_count"),
            rs.getLong("total_size_bytes"),
            lastModelUpdatedAt == null ? null : lastModelUpdatedAt.toInstant(),
            projectSource(assetSource, rs.getString("code"), rs.getString("name")),
            projectCategory(rs.getString("code"), rs.getString("name"), assetSource, fileCount, pathMappingCount),
            onboardingStatus(fileCount, hasMasterData, hasDeliveryStandard, hasDeliveryGovernance, pathMappingCount),
            fileCount,
            splitFileKinds(rs.getString("dominant_file_kinds")),
            lastScanAt == null ? null : lastScanAt.toInstant(),
            hasMasterData,
            hasDeliveryStandard,
            hasMasterData && hasDeliveryStandard,
            permissionTags("ProjectAssetView", "PROJECT", projectId, confidentialityLevel, indexEligibility),
            null,
            confidentialityLevel,
            lastSeenAt,
            lifecycleStatus(rs.getString("asset_status"), null, null, null),
            indexEligibility
        );
    }

    private static List<String> splitFileKinds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .limit(5)
            .toList();
    }

    private static String projectSource(String assetSource, String code, String name) {
        String text = ((code == null ? "" : code) + " " + (name == null ? "" : name)).toLowerCase();
        if (isTestProjectText(text)) {
            return "TEST";
        }
        String source = blankToNull(assetSource);
        if (source == null) {
            return "MANUAL";
        }
        String upper = source.toUpperCase();
        if (upper.startsWith("NAS_REAL")) {
            return "REAL_NAS";
        }
        if (upper.contains("SAMPLE") || upper.contains("TEMPLATE")) {
            return "SAMPLE_TEMPLATE";
        }
        if (upper.contains("TEST") || upper.contains("AGENT_TEST")) {
            return "TEST";
        }
        if (upper.contains("ARCHIVE") || upper.contains("HISTORY")) {
            return "ARCHIVED_HISTORY";
        }
        if (upper.contains("API") || upper.contains("MANUAL")) {
            return "MANUAL";
        }
        return source;
    }

    private static String projectCategory(String code, String name, String assetSource, int fileCount, int pathMappingCount) {
        String source = projectSource(assetSource, code, name);
        String text = ((code == null ? "" : code) + " " + (name == null ? "" : name)).toLowerCase();
        if ("TEST".equals(source) || isTestProjectText(text)) {
            return "TEST_PROJECT";
        }
        if ("SAMPLE_TEMPLATE".equals(source) || text.contains("样例") || text.contains("模板") || text.contains("sample")) {
            return "SAMPLE_TEMPLATE";
        }
        if ("ARCHIVED_HISTORY".equals(source) || text.contains("归档") || text.contains("历史")) {
            return "ARCHIVED_HISTORY";
        }
        if ("REAL_NAS".equals(source) || fileCount > 0 || pathMappingCount > 0) {
            return "REAL_NAS_PROJECT";
        }
        return "UNFINISHED_ONBOARDING";
    }

    private static boolean isTestProjectText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("test")
            || text.contains("测试")
            || text.contains("smoke")
            || text.startsWith("ph2")
            || text.startsWith("phase2")
            || text.startsWith("b6a-smoke")
            || text.contains(" phase2-")
            || text.contains(" ph2");
    }

    private static String onboardingStatus(
        int fileCount,
        boolean hasMasterData,
        boolean hasDeliveryStandard,
        boolean hasDeliveryGovernance,
        int pathMappingCount
    ) {
        if (hasDeliveryGovernance) {
            return "GOVERNANCE_READY";
        }
        if (hasMasterData || hasDeliveryStandard) {
            return "MASTERDATA_INITIALIZED";
        }
        if (fileCount > 0) {
            return "ASSETS_REGISTERED";
        }
        if (pathMappingCount > 0) {
            return "PATH_MAPPED";
        }
        return "NOT_ONBOARDED";
    }

    private static ModelAssetResponse mapModel(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastVerifiedAt = rs.getTimestamp("last_verified_at");
        Long projectId = rs.getLong("project_id");
        Long fileId = rs.getLong("file_id");
        String originalName = rs.getString("original_name");
        String sourceType = rs.getString("source_type");
        String confidentialityLevel = fileConfidentialityLevel(sourceType);
        String indexEligibility = fileIndexEligibility(sourceType, originalName);
        return new ModelAssetResponse(
            fileId,
            projectId,
            rs.getString("project_code"),
            rs.getString("project_name"),
            originalName,
            rs.getLong("size_bytes"),
            rs.getString("version_no"),
            rs.getString("process_status"),
            rs.getString("storage_provider"),
            rs.getString("storage_key"),
            rs.getString("discipline"),
            sourceType,
            lastVerifiedAt == null ? null : lastVerifiedAt.toInstant(),
            "/api/data-steward/projects/" + projectId + "/asset-files/" + fileId + "/content",
            permissionTags("ModelAssetView", "MODEL", projectId, confidentialityLevel, indexEligibility),
            null,
            confidentialityLevel,
            lastVerifiedAt == null ? null : lastVerifiedAt.toInstant(),
            lifecycleStatus(null, rs.getString("process_status"), rs.getString("source_type"), lastVerifiedAt),
            indexEligibility
        );
    }

    public List<FileAssetResponse> listFileById(Long userId, Long fileId) {
        return jdbcTemplate.query("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline, f.version_no, f.size_bytes,
                   f.checksum, f.storage_provider, f.storage_uri, f.logical_path,
                   f.source_type, f.process_status, f.review_status, f.confidence_level,
                   f.created_at, f.updated_at, f.last_verified_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.id = :fileId
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("fileId", fileId), (rs, rowNum) -> {
                Timestamp ca = rs.getTimestamp("created_at");
                Timestamp ua = rs.getTimestamp("updated_at");
                Timestamp lastVerifiedAt = rs.getTimestamp("last_verified_at");
                String originalName = rs.getString("original_name");
                String sourceType = rs.getString("source_type");
                String confidentialityLevel = fileConfidentialityLevel(sourceType);
                String indexEligibility = fileIndexEligibility(sourceType, originalName);
                Instant lastSeenAt = lastVerifiedAt == null
                    ? (ua == null ? null : ua.toInstant())
                    : lastVerifiedAt.toInstant();
                return new FileAssetResponse(
                    rs.getLong("file_id"), rs.getLong("project_id"),
                    rs.getString("project_code"), rs.getString("project_name"),
                    originalName,
                    extensionOf(originalName),
                    rs.getString("file_kind"), rs.getString("discipline"),
                    rs.getString("version_no"), rs.getLong("size_bytes"),
                    rs.getString("checksum"), rs.getString("storage_provider"),
                    rs.getString("storage_uri"), rs.getString("logical_path"),
                    sourceType, rs.getString("process_status"),
                    rs.getString("review_status"), rs.getString("confidence_level"),
                    ca == null ? null : ca.toInstant(),
                    ua == null ? null : ua.toInstant(),
                    permissionTags("FileAssetView", "FILE", rs.getLong("project_id"), confidentialityLevel, indexEligibility),
                    null,
                    confidentialityLevel,
                    lastSeenAt,
                    lifecycleStatus(null, rs.getString("process_status"), sourceType, lastVerifiedAt),
                    indexEligibility);
            });
    }

    public List<FileAssetResponse> listFiles(Long userId, Long projectId, String fileKind,
                                               String discipline, String fileName, String fileExt,
                                               String sourceType, String keyword) {
        return listFiles(userId, projectId, fileKind, discipline, fileName, fileExt,
            sourceType, keyword, null, null, 0, 200);
    }

    public List<FileAssetResponse> listFiles(Long userId, Long projectId, String fileKind,
                                               String discipline, String fileName, String fileExt,
                                               String sourceType, String keyword, String assetSource,
                                               String qualityIssue, int offset, int limit) {
        StringBuilder sb = new StringBuilder("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline, f.version_no, f.size_bytes,
                   f.checksum, f.storage_provider, f.storage_uri, f.logical_path,
                   f.source_type, f.process_status, f.review_status, f.confidence_level,
                   f.created_at, f.updated_at, f.last_verified_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        appendFileFilters(sb, params, projectId, fileKind, discipline, fileName,
            fileExt, sourceType, keyword, assetSource, qualityIssue);
        sb.append(" ORDER BY f.updated_at DESC, f.id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", Math.max(1, Math.min(limit, 500)));
        params.addValue("offset", Math.max(0, offset));
        return jdbcTemplate.query(sb.toString(), params, (rs, rowNum) -> {
            Timestamp ca = rs.getTimestamp("created_at");
            Timestamp ua = rs.getTimestamp("updated_at");
            Timestamp lastVerifiedAt = rs.getTimestamp("last_verified_at");
            String originalName = rs.getString("original_name");
            String rowSourceType = rs.getString("source_type");
            String confidentialityLevel = fileConfidentialityLevel(rowSourceType);
            String indexEligibility = fileIndexEligibility(rowSourceType, originalName);
            Instant lastSeenAt = lastVerifiedAt == null
                ? (ua == null ? null : ua.toInstant())
                : lastVerifiedAt.toInstant();
            return new FileAssetResponse(
                rs.getLong("file_id"), rs.getLong("project_id"),
                rs.getString("project_code"), rs.getString("project_name"),
                originalName,
                extensionOf(originalName),
                rs.getString("file_kind"), rs.getString("discipline"),
                rs.getString("version_no"), rs.getLong("size_bytes"),
                rs.getString("checksum"), rs.getString("storage_provider"),
                rs.getString("storage_uri"), rs.getString("logical_path"),
                rowSourceType, rs.getString("process_status"),
                rs.getString("review_status"), rs.getString("confidence_level"),
                ca == null ? null : ca.toInstant(),
                ua == null ? null : ua.toInstant(),
                permissionTags("FileAssetView", "FILE", rs.getLong("project_id"), confidentialityLevel, indexEligibility),
                null,
                confidentialityLevel,
                lastSeenAt,
                lifecycleStatus(null, rs.getString("process_status"), rowSourceType, lastVerifiedAt),
                indexEligibility);
        });
    }

    public long countFiles(Long userId, Long projectId, String fileKind,
                           String discipline, String fileName, String fileExt,
                           String sourceType, String keyword, String assetSource,
                           String qualityIssue) {
        StringBuilder sb = new StringBuilder("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        appendFileFilters(sb, params, projectId, fileKind, discipline, fileName,
            fileExt, sourceType, keyword, assetSource, qualityIssue);
        Long count = jdbcTemplate.queryForObject(sb.toString(), params, Long.class);
        return count == null ? 0L : count;
    }

    public int countAllFiles(Long userId) {
        return countAllFiles(userId, null, null);
    }

    public int countAllFiles(Long userId, Long projectId) {
        return countAllFiles(userId, projectId, null);
    }

    public int countAllFiles(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count == null ? 0 : count;
    }

    public int countDrawingFiles(Long userId) {
        return countDrawingFiles(userId, null, null);
    }

    public int countDrawingFiles(Long userId, Long projectId) {
        return countDrawingFiles(userId, projectId, null);
    }

    public int countDrawingFiles(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.file_kind = 'DRAWING'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count == null ? 0 : count;
    }

    public List<CapacityByFileKind> capacityByFileKind(Long userId) {
        return capacityByFileKind(userId, null, null);
    }

    public List<CapacityByFileKind> capacityByFileKind(Long userId, Long projectId) {
        return capacityByFileKind(userId, projectId, null);
    }

    public List<CapacityByFileKind> capacityByFileKind(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT COALESCE(f.file_kind, 'OTHER') AS file_kind,
                   COUNT(1) AS file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        sql.append("""
             GROUP BY COALESCE(f.file_kind, 'OTHER')
             ORDER BY total_size_bytes DESC
            """);
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new CapacityByFileKind(
            rs.getString("file_kind"),
            rs.getInt("file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    public java.util.Optional<FileAssetResponse> getFileByIdPlain(Long fileId) {
        List<FileAssetResponse> rows = jdbcTemplate.query("""
            SELECT f.id AS file_id, f.project_id, '' AS project_code, '' AS project_name,
                   f.original_name, f.file_kind, f.discipline, f.version_no, f.size_bytes,
                   f.checksum, f.storage_provider, f.storage_uri, f.logical_path,
                   f.source_type, f.process_status, f.review_status, f.confidence_level,
                   f.created_at, f.updated_at, f.last_verified_at
            FROM data_file_resources f
            WHERE f.id = :fileId AND f.deleted = 0
            """, new MapSqlParameterSource("fileId", fileId), (rs, rowNum) -> {
                Timestamp ca = rs.getTimestamp("created_at");
                Timestamp ua = rs.getTimestamp("updated_at");
                Timestamp lastVerifiedAt = rs.getTimestamp("last_verified_at");
                String originalName = rs.getString("original_name");
                String sourceType = rs.getString("source_type");
                String confidentialityLevel = fileConfidentialityLevel(sourceType);
                String indexEligibility = fileIndexEligibility(sourceType, originalName);
                Instant lastSeenAt = lastVerifiedAt == null
                    ? (ua == null ? null : ua.toInstant())
                    : lastVerifiedAt.toInstant();
                return new FileAssetResponse(
                    rs.getLong("file_id"), rs.getLong("project_id"),
                    rs.getString("project_code"), rs.getString("project_name"),
                    originalName,
                    extensionOf(originalName),
                    rs.getString("file_kind"), rs.getString("discipline"),
                    rs.getString("version_no"), rs.getLong("size_bytes"),
                    rs.getString("checksum"), rs.getString("storage_provider"),
                    rs.getString("storage_uri"), rs.getString("logical_path"),
                    sourceType, rs.getString("process_status"),
                    rs.getString("review_status"), rs.getString("confidence_level"),
                    ca == null ? null : ca.toInstant(),
                    ua == null ? null : ua.toInstant(),
                    permissionTags("FileAssetView", "FILE", rs.getLong("project_id"), confidentialityLevel, indexEligibility),
                    null,
                    confidentialityLevel,
                    lastSeenAt,
                    lifecycleStatus(null, rs.getString("process_status"), sourceType, lastVerifiedAt),
                    indexEligibility);
            });
        return rows.stream().findFirst();
    }

    public void updateChecksum(Long fileId, String checksum) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET checksum = :checksum, updated_by = 0
            WHERE id = :fileId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", fileId)
            .addValue("checksum", checksum));
    }

    public void updateFileMetadata(Long fileId, String fileKind, String discipline, String versionNo,
                                   String confidenceLevel, String reviewStatus, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET file_kind = COALESCE(:fileKind, file_kind),
                discipline = COALESCE(:discipline, discipline),
                business_tag = COALESCE(:discipline, business_tag),
                version_no = COALESCE(:versionNo, version_no),
                confidence_level = COALESCE(:confidenceLevel, confidence_level),
                review_status = COALESCE(:reviewStatus, review_status),
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :operatorId
            WHERE id = :fileId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("fileId", fileId)
            .addValue("fileKind", fileKind)
            .addValue("discipline", discipline)
            .addValue("versionNo", versionNo)
            .addValue("confidenceLevel", confidenceLevel)
            .addValue("reviewStatus", reviewStatus)
            .addValue("operatorId", operatorId));
    }

    public Instant findLastUpdated(Long userId) {
        return findLastUpdated(userId, null, null);
    }

    public Instant findLastUpdated(Long userId, Long projectId) {
        return findLastUpdated(userId, projectId, null);
    }

    public Instant findLastUpdated(Long userId, Long projectId, String assetSource) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        StringBuilder sql = new StringBuilder("""
            SELECT MAX(f.updated_at)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """);
        if (projectId != null) {
            sql.append(" AND f.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        appendAssetSourceFilter(sql, params, "p", assetSource);
        Timestamp ts = jdbcTemplate.queryForObject(sql.toString(), params, Timestamp.class);
        return ts == null ? null : ts.toInstant();
    }

    private void appendIf(StringBuilder sb, MapSqlParameterSource params, String clause, String key, Object value) {
        if (value != null) {
            sb.append(' ').append(clause);
            params.addValue(key, value);
        }
    }

    private void appendFileFilters(StringBuilder sb, MapSqlParameterSource params,
                                   Long projectId, String fileKind, String discipline,
                                   String fileName, String fileExt, String sourceType,
                                   String keyword, String assetSource, String qualityIssue) {
        appendIf(sb, params, "AND p.id = :projectId", "projectId", projectId);
        appendIf(sb, params, "AND f.file_kind = :fileKind", "fileKind", blankToNull(fileKind));
        appendIf(sb, params, "AND f.discipline = :discipline", "discipline", blankToNull(discipline));
        appendIf(sb, params, "AND f.original_name = :fileName", "fileName", blankToNull(fileName));
        String normalizedExt = normalizeFileExt(fileExt);
        if (normalizedExt != null) {
            sb.append(" AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) = :fileExt");
            params.addValue("fileExt", normalizedExt);
        }
        appendIf(sb, params, "AND f.source_type = :sourceType", "sourceType", blankToNull(sourceType));
        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND (p.code LIKE :likeKeyword OR p.name LIKE :likeKeyword OR f.original_name LIKE :likeKeyword OR f.storage_uri LIKE :likeKeyword)");
            params.addValue("likeKeyword", likeKeyword(keyword));
        }
        appendQualityIssueFilter(sb, qualityIssue);
        appendAssetSourceFilter(sb, params, "p", assetSource);
    }

    private void appendQualityIssueFilter(StringBuilder sb, String qualityIssue) {
        String issue = blankToNull(qualityIssue);
        if (issue == null) {
            return;
        }
        switch (issue.toUpperCase()) {
            case "MISSING_CHECKSUM" -> sb.append(" AND (f.checksum IS NULL OR f.checksum = '')");
            case "MISSING_CONFIDENCE" -> sb.append(" AND (f.confidence_level IS NULL OR f.confidence_level = '')");
            case "MISSING_DISCIPLINE" -> sb.append(" AND (f.discipline IS NULL OR f.discipline = '' OR f.discipline = 'OTHER')");
            case "MISSING_VERSION" -> sb.append(" AND (f.version_no IS NULL OR f.version_no = '')");
            case "MISSING_STORAGE_PATH" -> sb.append(" AND (f.storage_uri IS NULL OR f.storage_uri = '')");
            case "ZERO_SIZE_FILE" -> sb.append(" AND COALESCE(f.size_bytes, 0) <= 0");
            default -> {
                // Unknown quality filters are ignored to keep existing file search behavior stable.
            }
        }
    }

    private void appendAssetSourceFilter(StringBuilder sb, MapSqlParameterSource params, String tableAlias, String assetSource) {
        String source = blankToNull(assetSource);
        if (source == null) {
            return;
        }
        if (source.endsWith("*")) {
            sb.append(" AND ").append(tableAlias).append(".asset_source LIKE :assetSourceLike ");
            params.addValue("assetSourceLike", source.substring(0, source.length() - 1) + "%");
            appendRealProjectNameFilter(sb, tableAlias, source);
            return;
        }
        sb.append(" AND ").append(tableAlias).append(".asset_source = :assetSource ");
        params.addValue("assetSource", source);
        appendRealProjectNameFilter(sb, tableAlias, source);
    }

    private void appendRealProjectNameFilter(StringBuilder sb, String tableAlias, String assetSource) {
        if (assetSource == null || !assetSource.toUpperCase().startsWith("NAS_REAL")) {
            return;
        }
        sb.append(" AND LOWER(CONCAT(COALESCE(").append(tableAlias).append(".code, ''), ' ', COALESCE(")
            .append(tableAlias).append(".name, ''))) NOT LIKE '%test%' ");
        sb.append(" AND LOWER(CONCAT(COALESCE(").append(tableAlias).append(".code, ''), ' ', COALESCE(")
            .append(tableAlias).append(".name, ''))) NOT LIKE '%smoke%' ");
        sb.append(" AND LOWER(CONCAT(COALESCE(").append(tableAlias).append(".code, ''), ' ', COALESCE(")
            .append(tableAlias).append(".name, ''))) NOT LIKE 'ph2%' ");
        sb.append(" AND LOWER(CONCAT(COALESCE(").append(tableAlias).append(".code, ''), ' ', COALESCE(")
            .append(tableAlias).append(".name, ''))) NOT LIKE 'phase2%' ");
        sb.append(" AND CONCAT(COALESCE(").append(tableAlias).append(".code, ''), ' ', COALESCE(")
            .append(tableAlias).append(".name, '')) NOT LIKE '%测试%' ");
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static String normalizeFileExt(String value) {
        String ext = blankToNull(value);
        if (ext == null) {
            return null;
        }
        while (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        return ext.toLowerCase();
    }

    private static List<String> permissionTags(String sourceView, String assetKind, Long projectId,
                                               String confidentialityLevel, String indexEligibility) {
        return List.of(
            "SOURCE_SYSTEM:delivery_platform",
            "SOURCE_VIEW:" + sourceView,
            "ASSET_KIND:" + assetKind,
            "PROJECT:" + (projectId == null ? "UNKNOWN" : projectId),
            "CONFIDENTIALITY:" + confidentialityLevel,
            "INDEX_ELIGIBILITY:" + indexEligibility
        );
    }

    private static String projectConfidentialityLevel(String assetSource) {
        return "AGENT_TEST".equalsIgnoreCase(blankToNull(assetSource)) ? "INTERNAL" : "UNKNOWN";
    }

    private static String fileConfidentialityLevel(String sourceType) {
        return "AGENT_TEST".equalsIgnoreCase(blankToNull(sourceType)) ? "INTERNAL" : "UNKNOWN";
    }

    private static String projectIndexEligibility(String assetSource) {
        return "AGENT_TEST".equalsIgnoreCase(blankToNull(assetSource)) ? "preview_allowed" : "catalog_only";
    }

    private static String fileIndexEligibility(String sourceType, String originalName) {
        if (!"AGENT_TEST".equalsIgnoreCase(blankToNull(sourceType))) {
            return "catalog_only";
        }
        return switch (extensionOf(originalName)) {
            case "txt", "pdf", "doc", "docx" -> "full_text_allowed";
            case "xls", "xlsx" -> "preview_allowed";
            default -> "preview_allowed";
        };
    }

    private static String lifecycleStatus(String assetStatus, String processStatus,
                                          String sourceType, Timestamp lastVerifiedAt) {
        String asset = blankToNull(assetStatus);
        if ("ARCHIVED".equalsIgnoreCase(asset)) {
            return "archived";
        }

        String process = blankToNull(processStatus);
        if (process != null) {
            String normalized = process.toUpperCase();
            if ("ARCHIVED".equals(normalized)) {
                return "archived";
            }
            if ("DELETE_REQUESTED".equals(normalized)
                || "DELETED".equals(normalized)
                || "PENDING_DELETE".equals(normalized)) {
                return "deleted_candidate";
            }
        }

        if (lastVerifiedAt == null && isManagedNasSource(sourceType)) {
            return "stale_unverified";
        }
        if (asset == null && process == null) {
            return "unknown";
        }
        return "active";
    }

    private static boolean isManagedNasSource(String sourceType) {
        String source = blankToNull(sourceType);
        return "NAS_SCAN".equalsIgnoreCase(source) || "REVIEW".equalsIgnoreCase(source);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String likeKeyword(String value) {
        return value == null || value.isBlank() ? null : "%" + value.trim() + "%";
    }
}

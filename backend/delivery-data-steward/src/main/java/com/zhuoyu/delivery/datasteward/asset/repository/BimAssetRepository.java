package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByDiscipline;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ModelAssetResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
                SET name = :name,
                    industry_type = :industryType,
                    project_stage = :projectStage,
                    project_manager_name = :projectManagerName,
                    owner_org_name = :ownerOrgName,
                    asset_status = 'ACTIVE',
                    asset_source = :assetSource,
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
                :code, :name, :industryType, :projectStage, :projectManagerName,
                :ownerOrgName, 'ACTIVE', :assetSource, :operatorId, :operatorId
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
        return jdbcTemplate.query("""
            SELECT p.id,
                   p.code,
                   p.name,
                   p.industry_type,
                   p.project_stage,
                   p.project_manager_name,
                   p.asset_status,
                   p.asset_source,
                   COUNT(f.id) AS model_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes,
                   MAX(f.updated_at) AS last_model_updated_at
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            LEFT JOIN data_file_resources f
              ON f.project_id = p.id
             AND f.deleted = 0
             AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId
              AND upr.deleted = 0
              AND (
                  :keyword IS NULL
                  OR p.code LIKE :likeKeyword
                  OR p.name LIKE :likeKeyword
                  OR p.project_manager_name LIKE :likeKeyword
              )
            GROUP BY p.id, p.code, p.name, p.industry_type, p.project_stage,
                     p.project_manager_name, p.asset_status, p.asset_source
            ORDER BY total_size_bytes DESC, p.id DESC
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("keyword", blankToNull(keyword))
            .addValue("likeKeyword", likeKeyword(keyword)), PROJECT_ROW_MAPPER);
    }

    public Long insertModelFile(
        Long projectId,
        String originalName,
        Long sizeBytes,
        String storageUri,
        String storageProvider,
        String storageKey,
        String digest,
        String discipline,
        String versionNo,
        String sourceType,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO data_file_resources (
                project_id, original_name, file_kind, mime_type, size_bytes, storage_uri,
                storage_provider, storage_key, source_path_digest, business_tag, discipline,
                source_type, version_no, process_status, processed_at, last_verified_at,
                created_by, updated_by
            ) VALUES (
                :projectId, :originalName, 'MODEL', 'application/octet-stream', :sizeBytes, :storageUri,
                :storageProvider, :storageKey, :digest, :discipline, :discipline,
                :sourceType, :versionNo, 'PROCESSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("originalName", originalName)
            .addValue("sizeBytes", sizeBytes)
            .addValue("storageUri", storageUri)
            .addValue("storageProvider", storageProvider)
            .addValue("storageKey", storageKey)
            .addValue("digest", digest)
            .addValue("discipline", discipline)
            .addValue("sourceType", sourceType)
            .addValue("versionNo", versionNo)
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
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT p.id)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public int countModelFiles(Long userId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN data_file_resources f ON f.project_id = upr.project_id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource("userId", userId), Integer.class);
        return count == null ? 0 : count;
    }

    public long totalModelSize(Long userId) {
        Long total = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(f.size_bytes), 0)
            FROM core_user_project_roles upr
            JOIN data_file_resources f ON f.project_id = upr.project_id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource("userId", userId), Long.class);
        return total == null ? 0L : total;
    }

    public List<CapacityByDiscipline> capacityByDiscipline(Long userId) {
        return jdbcTemplate.query("""
            SELECT COALESCE(f.discipline, '未分类') AS discipline,
                   COUNT(1) AS model_file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN data_file_resources f ON f.project_id = upr.project_id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            GROUP BY COALESCE(f.discipline, '未分类')
            ORDER BY total_size_bytes DESC
            """, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> new CapacityByDiscipline(
            rs.getString("discipline"),
            rs.getInt("model_file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    public List<CapacityByProject> topProjectCapacity(Long userId) {
        return jdbcTemplate.query("""
            SELECT p.id AS project_id,
                   p.code AS project_code,
                   p.name AS project_name,
                   COUNT(f.id) AS model_file_count,
                   COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN data_file_resources f ON f.project_id = p.id AND f.deleted = 0 AND f.file_kind = 'MODEL'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            GROUP BY p.id, p.code, p.name
            ORDER BY total_size_bytes DESC
            LIMIT 10
            """, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> new CapacityByProject(
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getInt("model_file_count"),
            rs.getLong("total_size_bytes")
        ));
    }

    private static AssetProjectResponse mapProject(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastModelUpdatedAt = rs.getTimestamp("last_model_updated_at");
        return new AssetProjectResponse(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("industry_type"),
            rs.getString("project_stage"),
            rs.getString("project_manager_name"),
            rs.getString("asset_status"),
            rs.getString("asset_source"),
            rs.getInt("model_count"),
            rs.getLong("total_size_bytes"),
            lastModelUpdatedAt == null ? null : lastModelUpdatedAt.toInstant()
        );
    }

    private static ModelAssetResponse mapModel(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastVerifiedAt = rs.getTimestamp("last_verified_at");
        Long projectId = rs.getLong("project_id");
        Long fileId = rs.getLong("file_id");
        return new ModelAssetResponse(
            fileId,
            projectId,
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getString("original_name"),
            rs.getLong("size_bytes"),
            rs.getString("version_no"),
            rs.getString("process_status"),
            rs.getString("storage_provider"),
            rs.getString("storage_key"),
            rs.getString("discipline"),
            rs.getString("source_type"),
            lastVerifiedAt == null ? null : lastVerifiedAt.toInstant(),
            "/api/data-steward/projects/" + projectId + "/asset-files/" + fileId + "/content"
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String likeKeyword(String value) {
        return value == null || value.isBlank() ? null : "%" + value.trim() + "%";
    }
}

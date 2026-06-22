package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetPathMappingRepository {

    private static final RowMapper<PathMappingResponse> ROW_MAPPER = AssetPathMappingRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetPathMappingRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Long projectId, String providerCode, String nasPath, String matchStrategy,
                        Integer sortOrder, String remark, Long operatorId) {
        String sql = """
            INSERT INTO data_asset_project_path_mappings (
                project_id, provider_code, nas_path, match_strategy, enabled, sort_order,
                remark, created_by, updated_by
            ) VALUES (
                :projectId, :providerCode, :nasPath, :matchStrategy, 1, :sortOrder,
                :remark, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("providerCode", providerCode)
            .addValue("nasPath", nasPath)
            .addValue("matchStrategy", matchStrategy)
            .addValue("sortOrder", sortOrder)
            .addValue("remark", remark)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Long insertIfAbsent(Long projectId, String providerCode, String nasPath, String matchStrategy,
                                Integer sortOrder, String remark, Long operatorId) {
        PathMappingResponse existing = findByProjectAndNasPath(projectId, nasPath);
        if (existing != null) {
            return existing.id();
        }
        return insert(projectId, providerCode, nasPath, matchStrategy, sortOrder, remark, operatorId);
    }

    public void update(Long mappingId, String nasPath, String matchStrategy,
                        Boolean enabled, Integer sortOrder, String remark, Long operatorId) {
        StringBuilder sb = new StringBuilder("UPDATE data_asset_project_path_mappings SET updated_by = :operatorId");
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("mappingId", mappingId)
            .addValue("operatorId", operatorId);

        if (nasPath != null) {
            sb.append(", nas_path = :nasPath");
            params.addValue("nasPath", nasPath);
        }
        if (matchStrategy != null) {
            sb.append(", match_strategy = :matchStrategy");
            params.addValue("matchStrategy", matchStrategy);
        }
        if (enabled != null) {
            sb.append(", enabled = :enabled");
            params.addValue("enabled", enabled ? 1 : 0);
        }
        if (sortOrder != null) {
            sb.append(", sort_order = :sortOrder");
            params.addValue("sortOrder", sortOrder);
        }
        if (remark != null) {
            sb.append(", remark = :remark");
            params.addValue("remark", remark);
        }
        sb.append(" WHERE id = :mappingId AND deleted = 0");
        jdbcTemplate.update(sb.toString(), params);
    }

    public void markDeleted(Long mappingId) {
        jdbcTemplate.update("""
            UPDATE data_asset_project_path_mappings
            SET deleted = 1, delete_token = id
            WHERE id = :mappingId AND deleted = 0
            """, new MapSqlParameterSource("mappingId", mappingId));
    }

    public PathMappingResponse requireById(Long mappingId) {
        List<PathMappingResponse> rows = jdbcTemplate.query("""
            SELECT m.id, m.project_id, p.code AS project_code, p.name AS project_name,
                   m.provider_code, m.nas_path, m.match_strategy, m.enabled,
                   m.sort_order, m.remark, m.created_at
            FROM data_asset_project_path_mappings m
            JOIN core_projects p ON p.id = m.project_id AND p.deleted = 0
            WHERE m.id = :mappingId AND m.deleted = 0
            """, new MapSqlParameterSource("mappingId", mappingId), ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_PATH_MAPPING_NOT_FOUND", "路径映射不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    public PathMappingResponse findByProjectAndNasPath(Long projectId, String nasPath) {
        List<PathMappingResponse> rows = jdbcTemplate.query("""
            SELECT m.id, m.project_id, p.code AS project_code, p.name AS project_name,
                   m.provider_code, m.nas_path, m.match_strategy, m.enabled,
                   m.sort_order, m.remark, m.created_at
            FROM data_asset_project_path_mappings m
            JOIN core_projects p ON p.id = m.project_id AND p.deleted = 0
            WHERE m.project_id = :projectId
              AND m.nas_path = :nasPath
              AND m.deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("nasPath", nasPath), ROW_MAPPER);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<PathMappingResponse> list(Long projectId, Boolean enabled) {
        StringBuilder sb = new StringBuilder("""
            SELECT m.id, m.project_id, p.code AS project_code, p.name AS project_name,
                   m.provider_code, m.nas_path, m.match_strategy, m.enabled,
                   m.sort_order, m.remark, m.created_at
            FROM data_asset_project_path_mappings m
            JOIN core_projects p ON p.id = m.project_id AND p.deleted = 0
            WHERE m.deleted = 0
            """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (projectId != null) {
            sb.append(" AND m.project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        if (enabled != null) {
            sb.append(" AND m.enabled = :enabled");
            params.addValue("enabled", enabled ? 1 : 0);
        }
        sb.append(" ORDER BY m.sort_order, m.id");
        return jdbcTemplate.query(sb.toString(), params, ROW_MAPPER);
    }

    public List<PathMappingResponse> listEnabledPatrolMappings(int limit) {
        return jdbcTemplate.query("""
            SELECT m.id, m.project_id, p.code AS project_code, p.name AS project_name,
                   m.provider_code, m.nas_path, m.match_strategy, m.enabled,
                   m.sort_order, m.remark, m.created_at
            FROM data_asset_project_path_mappings m
            JOIN core_projects p ON p.id = m.project_id AND p.deleted = 0
            WHERE m.deleted = 0
              AND m.enabled = 1
              AND COALESCE(UPPER(p.status), 'ACTIVE') NOT IN ('ARCHIVED', 'DELETED')
              AND COALESCE(UPPER(p.asset_status), 'ACTIVE') NOT IN ('ARCHIVED', 'DELETED')
              AND COALESCE(UPPER(p.asset_source), '') NOT IN ('SAMPLE', 'TEMPLATE', 'TEST', 'AGENT_TEST')
              AND UPPER(p.name) NOT LIKE '%样例%'
              AND UPPER(p.name) NOT LIKE '%模板%'
              AND UPPER(p.name) NOT LIKE '%测试%'
            ORDER BY p.id, m.sort_order, m.id
            LIMIT :limit
            """, new MapSqlParameterSource("limit", Math.max(1, Math.min(limit, 200))), ROW_MAPPER);
    }

    public List<PathMappingResponse> findEnabledByNasPath(String nasPath) {
        return jdbcTemplate.query("""
            SELECT m.id, m.project_id, p.code AS project_code, p.name AS project_name,
                   m.provider_code, m.nas_path, m.match_strategy, m.enabled,
                   m.sort_order, m.remark, m.created_at
            FROM data_asset_project_path_mappings m
            JOIN core_projects p ON p.id = m.project_id AND p.deleted = 0
            WHERE m.deleted = 0 AND m.enabled = 1
              AND :nasPath LIKE CONCAT(m.nas_path, '%')
            ORDER BY LENGTH(m.nas_path) DESC
            LIMIT 1
            """, new MapSqlParameterSource("nasPath", nasPath), ROW_MAPPER);
    }

    private static PathMappingResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new PathMappingResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getString("provider_code"),
            rs.getString("nas_path"),
            rs.getString("match_strategy"),
            rs.getInt("enabled") == 1,
            rs.getInt("sort_order"),
            rs.getString("remark"),
            createdAt == null ? null : createdAt.toInstant()
        );
    }
}

package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AgentApiKeyRepository {

    private static final RowMapper<AgentApiKeyResponse> ROW_MAPPER = AgentApiKeyRepository::mapRow;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AgentApiKeyRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String keyName, String keyPrefix, String keyHash,
                        String scopeType, Instant expiresAt, Long createdBy, String remark) {
        String sql = """
            INSERT INTO data_agent_api_keys (key_name, key_prefix, key_hash, status, scope_type,
                expires_at, created_by, remark)
            VALUES (:keyName, :keyPrefix, :keyHash, 'ACTIVE', :scopeType, :expiresAt, :createdBy, :remark)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("keyName", keyName)
            .addValue("keyPrefix", keyPrefix)
            .addValue("keyHash", keyHash)
            .addValue("scopeType", scopeType)
            .addValue("expiresAt", expiresAt != null ? Timestamp.from(expiresAt) : null)
            .addValue("createdBy", createdBy)
            .addValue("remark", remark), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void insertProject(Long apiKeyId, Long projectId) {
        jdbcTemplate.update("""
            INSERT INTO data_agent_api_key_projects (api_key_id, project_id)
            VALUES (:apiKeyId, :projectId)
            """, new MapSqlParameterSource()
            .addValue("apiKeyId", apiKeyId)
            .addValue("projectId", projectId));
    }

    public List<Long> findAuthorizedProjectIds(Long apiKeyId) {
        return jdbcTemplate.query("""
            SELECT project_id FROM data_agent_api_key_projects WHERE api_key_id = :apiKeyId
            """, new MapSqlParameterSource("apiKeyId", apiKeyId),
            (rs, rowNum) -> rs.getLong("project_id"));
    }

    public Optional<AgentApiKeyResponse> findById(Long id) {
        List<AgentApiKeyResponse> rows = jdbcTemplate.query("""
            SELECT id, key_name, key_prefix, status, scope_type, expires_at,
                   last_used_at, last_used_ip, created_by, revoked_by, revoked_at,
                   remark, created_at, updated_at
            FROM data_agent_api_keys WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource("id", id), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public Optional<Long> findIdByKeyHash(String keyHash) {
        List<Long> rows = jdbcTemplate.query("""
            SELECT id FROM data_agent_api_keys
            WHERE key_hash = :keyHash AND status = 'ACTIVE' AND deleted = 0
            """, new MapSqlParameterSource("keyHash", keyHash),
            (rs, rowNum) -> rs.getLong("id"));
        return rows.stream().findFirst();
    }

    public List<AgentApiKeyResponse> listByUser(Long userId) {
        List<AgentApiKeyResponse> keys = jdbcTemplate.query("""
            SELECT id, key_name, key_prefix, status, scope_type, expires_at,
                   last_used_at, last_used_ip, created_by, revoked_by, revoked_at,
                   remark, created_at, updated_at
            FROM data_agent_api_keys
            WHERE created_by = :userId AND deleted = 0
            ORDER BY created_at DESC
            """, new MapSqlParameterSource("userId", userId), ROW_MAPPER);
        for (AgentApiKeyResponse key : keys) {
            // project IDs will be populated by caller
        }
        return keys;
    }

    public boolean revoke(Long id, Long revokedBy) {
        int rows = jdbcTemplate.update("""
            UPDATE data_agent_api_keys
            SET status = 'REVOKED', revoked_by = :revokedBy, revoked_at = NOW()
            WHERE id = :id AND deleted = 0 AND status = 'ACTIVE'
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("revokedBy", revokedBy));
        return rows > 0;
    }

    public void updateLastUsed(Long id, String ip) {
        jdbcTemplate.update("""
            UPDATE data_agent_api_keys
            SET last_used_at = NOW(), last_used_ip = :ip
            WHERE id = :id AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("ip", ip));
    }

    public boolean hasProjectAdminRoleOnAllActiveProjects(Long userId) {
        Integer totalActive = jdbcTemplate.queryForObject("""
            SELECT COUNT(1) FROM core_projects
            WHERE deleted = 0 AND status = 'ACTIVE'
            """, new MapSqlParameterSource(), Integer.class);
        if (totalActive == null || totalActive == 0) {
            return false;
        }
        Integer adminCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT upr.project_id)
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0 AND p.status = 'ACTIVE'
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0 AND r.code = 'PROJECT_ADMIN'
            WHERE upr.user_id = :userId AND upr.deleted = 0
            """, new MapSqlParameterSource("userId", userId), Integer.class);
        return adminCount != null && adminCount.equals(totalActive);
    }

    public boolean isValidKey(Long id) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1) FROM data_agent_api_keys
            WHERE id = :id AND status = 'ACTIVE' AND deleted = 0
              AND (expires_at IS NULL OR expires_at > NOW())
            """, new MapSqlParameterSource("id", id), Integer.class);
        return count != null && count > 0;
    }

    private static AgentApiKeyResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp exp = rs.getTimestamp("expires_at");
        Timestamp lu = rs.getTimestamp("last_used_at");
        Timestamp ra = rs.getTimestamp("revoked_at");
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp ua = rs.getTimestamp("updated_at");
        return new AgentApiKeyResponse(
            rs.getLong("id"),
            rs.getString("key_name"),
            rs.getString("key_prefix"),
            rs.getString("status"),
            rs.getString("scope_type"),
            Collections.emptyList(), // filled by caller
            exp != null ? exp.toInstant() : null,
            lu != null ? lu.toInstant() : null,
            rs.getString("last_used_ip"),
            rs.getLong("created_by"),
            rs.getObject("revoked_by", Long.class),
            ra != null ? ra.toInstant() : null,
            rs.getString("remark"),
            ca != null ? ca.toInstant() : null,
            ua != null ? ua.toInstant() : null
        );
    }
}

package com.zhuoyu.delivery.core.rbac.repository;

import com.zhuoyu.delivery.core.rbac.domain.PermissionGrant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepository {

    private static final RowMapper<PermissionGrant> PERMISSION_ROW_MAPPER = PermissionRepository::mapPermission;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PermissionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PermissionGrant> findByUserAndProject(Long userId, Long projectId) {
        String sql = """
            SELECT DISTINCT p.code, p.name
            FROM core_user_project_roles upr
            JOIN core_role_permissions rp ON rp.role_id = upr.role_id AND rp.deleted = 0
            JOIN core_permissions p ON p.id = rp.permission_id AND p.deleted = 0
            WHERE upr.user_id = :userId AND upr.project_id = :projectId AND upr.deleted = 0
            ORDER BY p.code
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId);
        return jdbcTemplate.query(sql, parameters, PERMISSION_ROW_MAPPER);
    }

    private static PermissionGrant mapPermission(ResultSet resultSet, int rowNum) throws SQLException {
        return new PermissionGrant(resultSet.getString("code"), resultSet.getString("name"));
    }
}

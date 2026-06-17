package com.zhuoyu.delivery.core.user.repository;

import com.zhuoyu.delivery.core.user.dto.AssignableProjectResponse;
import com.zhuoyu.delivery.core.user.dto.EmployeeProjectRoleAssignment;
import com.zhuoyu.delivery.core.user.dto.EmployeeProjectRoleItem;
import com.zhuoyu.delivery.core.user.dto.EmployeeSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeManagementRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EmployeeManagementRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EmployeeSummaryResponse> searchEmployees(String keyword, String status) {
        String sql = """
            SELECT u.id, u.username, u.phone_number, u.display_name, u.department_name,
                   u.status, u.last_login_at, u.created_at, u.updated_at,
                   COUNT(DISTINCT CASE WHEN upr.deleted = 0 AND p.id IS NOT NULL THEN upr.project_id ELSE NULL END) AS project_count
            FROM core_users u
            LEFT JOIN core_user_project_roles upr ON upr.user_id = u.id
            LEFT JOIN core_projects p ON p.id = upr.project_id
                AND p.deleted = 0
                AND p.status = 'ACTIVE'
                AND COALESCE(p.asset_status, 'ACTIVE') <> 'ARCHIVED'
            WHERE u.deleted = 0
              AND (:status IS NULL OR u.status = :status)
              AND (
                  :keyword IS NULL
                  OR u.username LIKE :likeKeyword
                  OR u.phone_number LIKE :likeKeyword
                  OR u.display_name LIKE :likeKeyword
                  OR u.department_name LIKE :likeKeyword
              )
            GROUP BY u.id, u.username, u.phone_number, u.display_name, u.department_name,
                     u.status, u.last_login_at, u.created_at, u.updated_at
            ORDER BY u.id DESC
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("keyword", blankToNull(keyword))
            .addValue("likeKeyword", likeKeyword(keyword))
            .addValue("status", blankToNull(status));
        return jdbcTemplate.query(sql, parameters, EmployeeManagementRepository::mapEmployeeSummary);
    }

    public Optional<EmployeeBaseRecord> findEmployeeBase(Long userId) {
        String sql = """
            SELECT id, username, phone_number, display_name, department_name,
                   status, last_login_at, created_at, updated_at
            FROM core_users
            WHERE id = :userId AND deleted = 0
            """;
        List<EmployeeBaseRecord> rows = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("userId", userId),
            EmployeeManagementRepository::mapEmployeeBase
        );
        return rows.stream().findFirst();
    }

    public List<EmployeeProjectRoleAssignment> findProjectRoleAssignments(Long userId) {
        String sql = """
            SELECT p.id AS project_id, p.code AS project_code, p.name AS project_name,
                   r.code AS role_code, r.name AS role_name
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id
                AND p.deleted = 0
                AND p.status = 'ACTIVE'
                AND COALESCE(p.asset_status, 'ACTIVE') <> 'ARCHIVED'
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            ORDER BY p.id
            """;
        return jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("userId", userId),
            EmployeeManagementRepository::mapProjectRoleAssignment
        );
    }

    public void updateUserStatus(Long userId, String status, Long operatorId) {
        String sql = """
            UPDATE core_users
            SET status = :status,
                updated_by = :operatorId
            WHERE id = :userId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("status", status)
            .addValue("operatorId", operatorId));
    }

    public Long insertEmployee(
        String username,
        String phoneNumber,
        String passwordHash,
        String displayName,
        String departmentName,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO core_users (
                username, phone_number, password_hash, display_name,
                department_name, status, created_by, updated_by
            ) VALUES (
                :username, :phoneNumber, :passwordHash, :displayName,
                :departmentName, 'ACTIVE', :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("username", username)
            .addValue("phoneNumber", phoneNumber)
            .addValue("passwordHash", passwordHash)
            .addValue("displayName", displayName)
            .addValue("departmentName", blankToNull(departmentName))
            .addValue("operatorId", operatorId), keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void softDeleteUser(Long userId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE core_users
            SET status = 'DISABLED',
                deleted = 1,
                updated_by = :operatorId
            WHERE id = :userId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("operatorId", operatorId));
        jdbcTemplate.update("""
            UPDATE core_user_project_roles
            SET deleted = 1,
                updated_by = :operatorId
            WHERE user_id = :userId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("operatorId", operatorId));
    }

    public List<AssignableProjectResponse> findAssignableProjects(Long adminUserId) {
        String sql = """
            SELECT DISTINCT p.id, p.code, p.name, p.industry_type, p.status
            FROM core_user_project_roles upr
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            JOIN core_projects p ON p.id = upr.project_id
                AND p.deleted = 0
                AND p.status = 'ACTIVE'
                AND COALESCE(p.asset_status, 'ACTIVE') <> 'ARCHIVED'
            WHERE upr.user_id = :adminUserId
              AND upr.deleted = 0
              AND r.code = 'PROJECT_ADMIN'
            ORDER BY p.id
            """;
        return jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("adminUserId", adminUserId),
            EmployeeManagementRepository::mapAssignableProject
        );
    }

    public List<AssignableProjectResponse> findAllActiveProjects() {
        String sql = """
            SELECT p.id, p.code, p.name, p.industry_type, p.status
            FROM core_projects p
            WHERE p.deleted = 0
              AND p.status = 'ACTIVE'
              AND COALESCE(p.asset_status, 'ACTIVE') <> 'ARCHIVED'
            ORDER BY p.id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), EmployeeManagementRepository::mapAssignableProject);
    }

    public List<Long> findAssignableProjectIds(Long adminUserId) {
        return findAssignableProjects(adminUserId).stream()
            .map(AssignableProjectResponse::id)
            .toList();
    }

    public List<Long> findAllActiveProjectIds() {
        return findAllActiveProjects().stream()
            .map(AssignableProjectResponse::id)
            .toList();
    }

    public Map<String, Long> findRoleIds(Collection<String> roleCodes) {
        String sql = """
            SELECT id, code
            FROM core_roles
            WHERE deleted = 0 AND code IN (:roleCodes)
            """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("roleCodes", roleCodes),
                (rs, rowNum) -> Map.entry(rs.getString("code"), rs.getLong("id"))
            )
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void replaceProjectRoles(Long userId, List<EmployeeProjectRoleItem> assignments, Map<String, Long> roleIds, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE core_user_project_roles
            SET deleted = 1,
                updated_by = :operatorId
            WHERE user_id = :userId AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("operatorId", operatorId));

        for (EmployeeProjectRoleItem item : assignments) {
            jdbcTemplate.update("""
                INSERT INTO core_user_project_roles (user_id, project_id, role_id, created_by, updated_by, deleted)
                VALUES (:userId, :projectId, :roleId, :operatorId, :operatorId, 0)
                ON DUPLICATE KEY UPDATE
                    deleted = 0,
                    updated_by = :operatorId
                """, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("projectId", item.projectId())
                .addValue("roleId", roleIds.get(item.roleCode()))
                .addValue("operatorId", operatorId));
        }
    }

    public record EmployeeBaseRecord(
        Long userId,
        String username,
        String phoneNumber,
        String displayName,
        String departmentName,
        String status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    private static EmployeeSummaryResponse mapEmployeeSummary(ResultSet rs, int rowNum) throws SQLException {
        return new EmployeeSummaryResponse(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("phone_number"),
            rs.getString("display_name"),
            rs.getString("department_name"),
            rs.getString("status"),
            rs.getInt("project_count"),
            timestampToInstant(rs.getTimestamp("last_login_at")),
            timestampToInstant(rs.getTimestamp("created_at")),
            timestampToInstant(rs.getTimestamp("updated_at"))
        );
    }

    private static EmployeeBaseRecord mapEmployeeBase(ResultSet rs, int rowNum) throws SQLException {
        return new EmployeeBaseRecord(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("phone_number"),
            rs.getString("display_name"),
            rs.getString("department_name"),
            rs.getString("status"),
            timestampToInstant(rs.getTimestamp("last_login_at")),
            timestampToInstant(rs.getTimestamp("created_at")),
            timestampToInstant(rs.getTimestamp("updated_at"))
        );
    }

    private static EmployeeProjectRoleAssignment mapProjectRoleAssignment(ResultSet rs, int rowNum) throws SQLException {
        return new EmployeeProjectRoleAssignment(
            rs.getLong("project_id"),
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getString("role_code"),
            rs.getString("role_name")
        );
    }

    private static AssignableProjectResponse mapAssignableProject(ResultSet rs, int rowNum) throws SQLException {
        return new AssignableProjectResponse(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("industry_type"),
            rs.getString("status")
        );
    }

    private static Instant timestampToInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String likeKeyword(String keyword) {
        String value = blankToNull(keyword);
        return value == null ? null : "%" + value + "%";
    }
}

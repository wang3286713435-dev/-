package com.zhuoyu.delivery.core.project.repository;

import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectAccessRepository {

    private static final RowMapper<AccessibleProject> PROJECT_ROW_MAPPER = ProjectAccessRepository::mapProject;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProjectAccessRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccessibleProject> findAccessibleProjects(Long userId) {
        String sql = """
            SELECT p.id, p.code, p.name, p.industry_type, p.status, p.project_manager_name,
                   r.code AS role_code, r.name AS role_name
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId AND upr.deleted = 0
            ORDER BY p.id
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), PROJECT_ROW_MAPPER);
    }

    public Optional<AccessibleProject> findAccessibleProject(Long userId, Long projectId) {
        String sql = """
            SELECT p.id, p.code, p.name, p.industry_type, p.status, p.project_manager_name,
                   r.code AS role_code, r.name AS role_name
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId AND upr.project_id = :projectId AND upr.deleted = 0
            """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId);
        List<AccessibleProject> projects = jdbcTemplate.query(sql, parameters, PROJECT_ROW_MAPPER);
        return projects.stream().findFirst();
    }

    private static AccessibleProject mapProject(ResultSet resultSet, int rowNum) throws SQLException {
        return new AccessibleProject(
            resultSet.getLong("id"),
            resultSet.getString("code"),
            resultSet.getString("name"),
            resultSet.getString("industry_type"),
            resultSet.getString("status"),
            resultSet.getString("project_manager_name"),
            resultSet.getString("role_code"),
            resultSet.getString("role_name")
        );
    }
}

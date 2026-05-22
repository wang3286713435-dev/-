package com.zhuoyu.delivery.core.user.repository;

import com.zhuoyu.delivery.core.user.domain.UserAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepository {

    private static final RowMapper<UserAccount> USER_ROW_MAPPER = UserAccountRepository::mapUser;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        String sql = """
            SELECT id, username, password_hash, display_name, status,
                   phone_number, department_name, last_login_at
            FROM core_users
            WHERE username = :username AND deleted = 0
            """;
        List<UserAccount> users = jdbcTemplate.query(sql, new MapSqlParameterSource("username", username), USER_ROW_MAPPER);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findByPhoneNumber(String phoneNumber) {
        String sql = """
            SELECT id, username, password_hash, display_name, status,
                   phone_number, department_name, last_login_at
            FROM core_users
            WHERE phone_number = :phoneNumber AND deleted = 0
            """;
        List<UserAccount> users = jdbcTemplate.query(
            sql,
            new MapSqlParameterSource("phoneNumber", phoneNumber),
            USER_ROW_MAPPER
        );
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findById(Long id) {
        String sql = """
            SELECT id, username, password_hash, display_name, status,
                   phone_number, department_name, last_login_at
            FROM core_users
            WHERE id = :id AND deleted = 0
            """;
        List<UserAccount> users = jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), USER_ROW_MAPPER);
        return users.stream().findFirst();
    }

    public Long insertRegisteredEmployee(
        String phoneNumber,
        String passwordHash,
        String displayName,
        String departmentName
    ) {
        String sql = """
            INSERT INTO core_users (
                username, phone_number, password_hash, display_name,
                department_name, status, created_by, updated_by
            ) VALUES (
                :phoneNumber, :phoneNumber, :passwordHash, :displayName,
                :departmentName, 'ACTIVE', NULL, NULL
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("phoneNumber", phoneNumber)
            .addValue("passwordHash", passwordHash)
            .addValue("displayName", displayName)
            .addValue("departmentName", blankToNull(departmentName)), keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void updateLastLoginAt(Long userId) {
        String sql = """
            UPDATE core_users
            SET last_login_at = CURRENT_TIMESTAMP,
                updated_by = :userId
            WHERE id = :userId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("userId", userId));
    }

    private static UserAccount mapUser(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserAccount(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("password_hash"),
            resultSet.getString("display_name"),
            resultSet.getString("status"),
            resultSet.getString("phone_number"),
            resultSet.getString("department_name"),
            timestampToInstant(resultSet.getTimestamp("last_login_at"))
        );
    }

    private static Instant timestampToInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.zhuoyu.delivery.core.user.repository;

import com.zhuoyu.delivery.core.user.domain.UserAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
            SELECT id, username, password_hash, display_name, status
            FROM core_users
            WHERE username = :username AND deleted = 0
            """;
        List<UserAccount> users = jdbcTemplate.query(sql, new MapSqlParameterSource("username", username), USER_ROW_MAPPER);
        return users.stream().findFirst();
    }

    public Optional<UserAccount> findById(Long id) {
        String sql = """
            SELECT id, username, password_hash, display_name, status
            FROM core_users
            WHERE id = :id AND deleted = 0
            """;
        List<UserAccount> users = jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), USER_ROW_MAPPER);
        return users.stream().findFirst();
    }

    private static UserAccount mapUser(ResultSet resultSet, int rowNum) throws SQLException {
        return new UserAccount(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("password_hash"),
            resultSet.getString("display_name"),
            resultSet.getString("status")
        );
    }
}

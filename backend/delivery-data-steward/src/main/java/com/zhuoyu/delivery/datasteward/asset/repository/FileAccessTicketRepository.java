package com.zhuoyu.delivery.datasteward.asset.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class FileAccessTicketRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FileAccessTicketRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String ticket, Long fileId, Long projectId, Long userId,
                        String action, Instant expiresAt) {
        String sql = """
            INSERT INTO data_file_access_tickets (ticket, file_id, project_id, user_id, action, status, expires_at)
            VALUES (:ticket, :fileId, :projectId, :userId, :action, 'ACTIVE', :expiresAt)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("ticket", ticket)
            .addValue("fileId", fileId)
            .addValue("projectId", projectId)
            .addValue("userId", userId)
            .addValue("action", action)
            .addValue("expiresAt", Timestamp.from(expiresAt)), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public Optional<AccessTicketRow> findByTicket(String ticket) {
        var rows = jdbcTemplate.query("""
            SELECT id, ticket, file_id, project_id, user_id, action, status, expires_at, used_at, created_at
            FROM data_file_access_tickets
            WHERE ticket = :ticket
            """, new MapSqlParameterSource("ticket", ticket), (rs, rowNum) -> {
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                Timestamp usedAt = rs.getTimestamp("used_at");
                Timestamp createdAt = rs.getTimestamp("created_at");
                return new AccessTicketRow(
                    rs.getLong("id"),
                    rs.getString("ticket"),
                    rs.getLong("file_id"),
                    rs.getLong("project_id"),
                    rs.getLong("user_id"),
                    rs.getString("action"),
                    rs.getString("status"),
                    expiresAt == null ? null : expiresAt.toInstant(),
                    usedAt == null ? null : usedAt.toInstant(),
                    createdAt == null ? null : createdAt.toInstant()
                );
            });
        return rows.stream().findFirst();
    }

    public void markUsed(Long id) {
        jdbcTemplate.update("""
            UPDATE data_file_access_tickets
            SET used_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, new MapSqlParameterSource("id", id));
    }

    public void markExpired(Long id) {
        jdbcTemplate.update("""
            UPDATE data_file_access_tickets
            SET status = 'EXPIRED'
            WHERE id = :id AND status = 'ACTIVE'
            """, new MapSqlParameterSource("id", id));
    }

    public record AccessTicketRow(
        Long id,
        String ticket,
        Long fileId,
        Long projectId,
        Long userId,
        String action,
        String status,
        Instant expiresAt,
        Instant usedAt,
        Instant createdAt
    ) {
    }
}

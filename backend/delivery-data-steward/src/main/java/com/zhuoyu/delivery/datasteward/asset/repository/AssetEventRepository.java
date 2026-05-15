package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AssetEventRepository {

    private static final RowMapper<EventResponse> EVENT_ROW_MAPPER = AssetEventRepository::mapEvent;

    private static final String SELECT_COLS = """
        SELECT id, event_type, project_id, aggregate_type, aggregate_id,
               action_code, operator_id, source_type, summary, payload_json, trace_id, created_at
        FROM data_asset_events
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(String eventType, Long projectId, String aggregateType, String aggregateId,
                        String actionCode, Long operatorId, String sourceType,
                        String summary, String payloadJson, String traceId) {
        String sql = """
            INSERT INTO data_asset_events (event_type, project_id, aggregate_type, aggregate_id,
                action_code, operator_id, source_type, summary, payload_json, trace_id)
            VALUES (:eventType, :projectId, :aggregateType, :aggregateId,
                :actionCode, :operatorId, :sourceType, :summary, :payloadJson, :traceId)
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("eventType", eventType)
            .addValue("projectId", projectId)
            .addValue("aggregateType", aggregateType)
            .addValue("aggregateId", aggregateId)
            .addValue("actionCode", actionCode)
            .addValue("operatorId", operatorId)
            .addValue("sourceType", sourceType)
            .addValue("summary", summary != null && summary.length() > 500 ? summary.substring(0, 500) : summary)
            .addValue("payloadJson", payloadJson)
            .addValue("traceId", traceId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<EventResponse> query(Long afterEventId, Instant fromTime, Instant toTime,
                                      Long projectId, String eventType, String actionCode, int limit) {
        StringBuilder sb = new StringBuilder(SELECT_COLS).append(" WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (afterEventId != null) {
            sb.append(" AND id > :afterEventId");
            params.addValue("afterEventId", afterEventId);
        }
        if (fromTime != null) {
            sb.append(" AND created_at >= :fromTime");
            params.addValue("fromTime", Timestamp.from(fromTime));
        }
        if (toTime != null) {
            sb.append(" AND created_at <= :toTime");
            params.addValue("toTime", Timestamp.from(toTime));
        }
        if (projectId != null) {
            sb.append(" AND project_id = :projectId");
            params.addValue("projectId", projectId);
        }
        if (eventType != null && !eventType.isBlank()) {
            sb.append(" AND event_type = :eventType");
            params.addValue("eventType", eventType.trim());
        }
        if (actionCode != null && !actionCode.isBlank()) {
            sb.append(" AND action_code = :actionCode");
            params.addValue("actionCode", actionCode.trim());
        }
        sb.append(" ORDER BY id ASC LIMIT :limit");
        params.addValue("limit", Math.min(limit, 200));
        return jdbcTemplate.query(sb.toString(), params, EVENT_ROW_MAPPER);
    }

    private static EventResponse mapEvent(ResultSet rs, int rowNum) throws SQLException {
        Timestamp ca = rs.getTimestamp("created_at");
        Long projectId = rs.getObject("project_id", Long.class);
        Instant createdAt = ca == null ? null : ca.toInstant();
        return new EventResponse(
            rs.getLong("id"),
            rs.getString("event_type"),
            projectId,
            rs.getString("aggregate_type"),
            rs.getString("aggregate_id"),
            rs.getString("action_code"),
            rs.getObject("operator_id", Long.class),
            rs.getString("source_type"),
            rs.getString("summary"),
            rs.getString("payload_json"),
            rs.getString("trace_id"),
            createdAt,
            permissionTags(projectId),
            null,
            "UNKNOWN",
            createdAt,
            "active",
            "catalog_only"
        );
    }

    private static List<String> permissionTags(Long projectId) {
        return List.of(
            "SOURCE_SYSTEM:delivery_platform",
            "SOURCE_VIEW:AuditEventView",
            "ASSET_KIND:AUDIT_EVENT",
            "PROJECT:" + (projectId == null ? "UNKNOWN" : projectId),
            "CONFIDENTIALITY:UNKNOWN",
            "INDEX_ELIGIBILITY:catalog_only"
        );
    }
}

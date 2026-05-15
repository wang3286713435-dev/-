package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
public class NonstandardDirectoryRepository {

    private static final RowMapper<NonstandardDirectoryResponse> ROW_MAPPER = NonstandardDirectoryRepository::map;

    private static final String SELECT_COLUMNS = """
        SELECT id, provider_code, root_path, directory_name, nas_path, directory_type,
               risk_type, governance_status, suggested_project_code, suggested_project_name,
               duplicate_base_code, standard_folders_json, review_reason, agent_suggestion,
               manual_decision, decision_reason, owner_name, decided_by, decided_at,
               created_at, updated_at, created_by
        FROM data_asset_nonstandard_directories
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NonstandardDirectoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long upsert(String providerCode, String rootPath, String directoryName, String nasPath,
                       String directoryType, String riskType, String governanceStatus,
                       String suggestedProjectCode, String suggestedProjectName,
                       String duplicateBaseCode, String standardFoldersJson,
                       String reviewReason, String ownerName, Long operatorId) {
        String sql = """
            INSERT INTO data_asset_nonstandard_directories (
                provider_code, root_path, directory_name, nas_path, directory_type,
                risk_type, governance_status, suggested_project_code, suggested_project_name,
                duplicate_base_code, standard_folders_json, review_reason, owner_name,
                created_by, updated_by
            ) VALUES (
                :providerCode, :rootPath, :directoryName, :nasPath, :directoryType,
                :riskType, :governanceStatus, :suggestedProjectCode, :suggestedProjectName,
                :duplicateBaseCode, :standardFoldersJson, :reviewReason, :ownerName,
                :operatorId, :operatorId
            )
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                root_path = VALUES(root_path),
                directory_name = VALUES(directory_name),
                directory_type = VALUES(directory_type),
                risk_type = VALUES(risk_type),
                suggested_project_code = VALUES(suggested_project_code),
                suggested_project_name = VALUES(suggested_project_name),
                duplicate_base_code = VALUES(duplicate_base_code),
                standard_folders_json = VALUES(standard_folders_json),
                review_reason = VALUES(review_reason),
                owner_name = COALESCE(VALUES(owner_name), owner_name),
                updated_by = VALUES(updated_by),
                deleted = 0,
                delete_token = 0
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("providerCode", providerCode)
            .addValue("rootPath", rootPath)
            .addValue("directoryName", directoryName)
            .addValue("nasPath", nasPath)
            .addValue("directoryType", directoryType)
            .addValue("riskType", riskType)
            .addValue("governanceStatus", governanceStatus)
            .addValue("suggestedProjectCode", suggestedProjectCode)
            .addValue("suggestedProjectName", suggestedProjectName)
            .addValue("duplicateBaseCode", duplicateBaseCode)
            .addValue("standardFoldersJson", standardFoldersJson)
            .addValue("reviewReason", reviewReason)
            .addValue("ownerName", ownerName)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public List<NonstandardDirectoryResponse> list(Long userId, String governanceStatus,
                                                   String riskType, String keyword, int limit) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append("""
            WHERE deleted = 0
              AND created_by = :userId
            """);
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        if (governanceStatus != null && !governanceStatus.isBlank()) {
            sql.append(" AND governance_status = :governanceStatus");
            params.addValue("governanceStatus", governanceStatus.trim().toUpperCase());
        }
        if (riskType != null && !riskType.isBlank()) {
            sql.append(" AND risk_type = :riskType");
            params.addValue("riskType", riskType.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                AND (
                    directory_name LIKE :keyword
                    OR nas_path LIKE :keyword
                    OR suggested_project_code LIKE :keyword
                    OR suggested_project_name LIKE :keyword
                    OR review_reason LIKE :keyword
                )
                """);
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT :limit");
        params.addValue("limit", Math.min(Math.max(limit, 1), 500));
        return jdbcTemplate.query(sql.toString(), params, ROW_MAPPER);
    }

    public NonstandardDirectoryResponse requireForUser(Long userId, Long id) {
        List<NonstandardDirectoryResponse> rows = jdbcTemplate.query(SELECT_COLUMNS + """
            WHERE deleted = 0
              AND id = :id
              AND created_by = :userId
            """, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId), ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_NONSTANDARD_DIRECTORY_NOT_FOUND", "非标准目录记录不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    public void update(Long userId, Long id, String governanceStatus, String riskType,
                       String suggestedProjectCode, String suggestedProjectName,
                       String reviewReason, String agentSuggestion,
                       String manualDecision, String decisionReason, String ownerName) {
        String sql = """
            UPDATE data_asset_nonstandard_directories
            SET governance_status = COALESCE(:governanceStatus, governance_status),
                risk_type = COALESCE(:riskType, risk_type),
                suggested_project_code = COALESCE(:suggestedProjectCode, suggested_project_code),
                suggested_project_name = COALESCE(:suggestedProjectName, suggested_project_name),
                review_reason = COALESCE(:reviewReason, review_reason),
                agent_suggestion = COALESCE(:agentSuggestion, agent_suggestion),
                manual_decision = COALESCE(:manualDecision, manual_decision),
                decision_reason = COALESCE(:decisionReason, decision_reason),
                owner_name = COALESCE(:ownerName, owner_name),
                decided_by = CASE
                    WHEN :manualDecision IS NOT NULL OR :decisionReason IS NOT NULL THEN :userId
                    ELSE decided_by
                END,
                decided_at = CASE
                    WHEN :manualDecision IS NOT NULL OR :decisionReason IS NOT NULL THEN CURRENT_TIMESTAMP
                    ELSE decided_at
                END,
                updated_by = :userId
            WHERE id = :id
              AND created_by = :userId
              AND deleted = 0
            """;
        int updated = jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)
            .addValue("governanceStatus", emptyToNull(governanceStatus))
            .addValue("riskType", emptyToNull(riskType))
            .addValue("suggestedProjectCode", emptyToNull(suggestedProjectCode))
            .addValue("suggestedProjectName", emptyToNull(suggestedProjectName))
            .addValue("reviewReason", emptyToNull(reviewReason))
            .addValue("agentSuggestion", emptyToNull(agentSuggestion))
            .addValue("manualDecision", emptyToNull(manualDecision))
            .addValue("decisionReason", emptyToNull(decisionReason))
            .addValue("ownerName", emptyToNull(ownerName)));
        if (updated == 0) {
            throw new BusinessException("ASSET_NONSTANDARD_DIRECTORY_NOT_FOUND", "非标准目录记录不存在", HttpStatus.NOT_FOUND);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static NonstandardDirectoryResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new NonstandardDirectoryResponse(
            rs.getLong("id"),
            rs.getString("provider_code"),
            rs.getString("root_path"),
            rs.getString("directory_name"),
            rs.getString("nas_path"),
            rs.getString("directory_type"),
            rs.getString("risk_type"),
            rs.getString("governance_status"),
            rs.getString("suggested_project_code"),
            rs.getString("suggested_project_name"),
            rs.getString("duplicate_base_code"),
            rs.getString("standard_folders_json"),
            rs.getString("review_reason"),
            rs.getString("agent_suggestion"),
            rs.getString("manual_decision"),
            rs.getString("decision_reason"),
            rs.getString("owner_name"),
            rs.getObject("decided_by", Long.class),
            toInstant(rs.getTimestamp("decided_at")),
            toInstant(rs.getTimestamp("created_at")),
            toInstant(rs.getTimestamp("updated_at")),
            rs.getObject("created_by", Long.class)
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

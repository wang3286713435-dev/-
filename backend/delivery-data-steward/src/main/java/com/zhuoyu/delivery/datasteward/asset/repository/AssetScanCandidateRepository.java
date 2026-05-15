package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanCandidateResponse;
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
public class AssetScanCandidateRepository {

    private static final RowMapper<ScanCandidateResponse> ROW_MAPPER = AssetScanCandidateRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetScanCandidateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Long scanTaskId, Long matchedProjectId, String matchedProjectCode,
                        String rawPath, String fileName, String fileExt, Long sizeBytes,
                        Long lastModifiedMs, String detectedFileKind, String detectedDiscipline,
                        String detectedVersionNo, String confidenceLevel, String reviewStatus,
                        Long operatorId) {
        String sql = """
            INSERT INTO data_asset_scan_candidates (
                scan_task_id, matched_project_id, matched_project_code, raw_path, file_name,
                file_ext, size_bytes, last_modified_at, detected_file_kind, detected_discipline,
                detected_version_no, confidence_level, review_status, created_by, updated_by
            ) VALUES (
                :scanTaskId, :matchedProjectId, :matchedProjectCode, :rawPath, :fileName,
                :fileExt, :sizeBytes, :lastModifiedAt, :detectedFileKind, :detectedDiscipline,
                :detectedVersionNo, :confidenceLevel, :reviewStatus, :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Timestamp lastModifiedAt = lastModifiedMs != null ? new Timestamp(lastModifiedMs) : null;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("scanTaskId", scanTaskId)
            .addValue("matchedProjectId", matchedProjectId)
            .addValue("matchedProjectCode", matchedProjectCode)
            .addValue("rawPath", rawPath)
            .addValue("fileName", fileName)
            .addValue("fileExt", fileExt)
            .addValue("sizeBytes", sizeBytes)
            .addValue("lastModifiedAt", lastModifiedAt)
            .addValue("detectedFileKind", detectedFileKind)
            .addValue("detectedDiscipline", detectedDiscipline)
            .addValue("detectedVersionNo", detectedVersionNo)
            .addValue("confidenceLevel", confidenceLevel)
            .addValue("reviewStatus", reviewStatus)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updateReview(Long candidateId, Long matchedProjectId, String matchedProjectCode,
                              String detectedFileKind, String detectedDiscipline,
                              String detectedVersionNo, String reviewMessage,
                              String reviewStatus, Long reviewerId, Long createdFileResourceId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("candidateId", candidateId);

        StringBuilder sb = new StringBuilder("""
            UPDATE data_asset_scan_candidates
            SET review_status = :reviewStatus,
                reviewed_by = :reviewerId,
                reviewed_at = CURRENT_TIMESTAMP,
                updated_by = :reviewerId
            """);
        params.addValue("reviewStatus", reviewStatus);
        params.addValue("reviewerId", reviewerId);

        if (matchedProjectId != null) {
            sb.append(", matched_project_id = :matchedProjectId");
            params.addValue("matchedProjectId", matchedProjectId);
        }
        if (matchedProjectCode != null) {
            sb.append(", matched_project_code = :matchedProjectCode");
            params.addValue("matchedProjectCode", matchedProjectCode);
        }
        if (detectedFileKind != null) {
            sb.append(", detected_file_kind = :detectedFileKind");
            params.addValue("detectedFileKind", detectedFileKind);
        }
        if (detectedDiscipline != null) {
            sb.append(", detected_discipline = :detectedDiscipline");
            params.addValue("detectedDiscipline", detectedDiscipline);
        }
        if (detectedVersionNo != null) {
            sb.append(", detected_version_no = :detectedVersionNo");
            params.addValue("detectedVersionNo", detectedVersionNo);
        }
        if (reviewMessage != null) {
            sb.append(", review_message = :reviewMessage");
            params.addValue("reviewMessage", reviewMessage);
        }
        if (createdFileResourceId != null) {
            sb.append(", created_file_resource_id = :createdFileResourceId");
            params.addValue("createdFileResourceId", createdFileResourceId);
        }

        sb.append(" WHERE id = :candidateId AND deleted = 0");
        jdbcTemplate.update(sb.toString(), params);
    }

    public ScanCandidateResponse requireById(Long candidateId) {
        List<ScanCandidateResponse> rows = query("""
            WHERE c.id = :candidateId AND c.deleted = 0
            """, new MapSqlParameterSource("candidateId", candidateId));
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_CANDIDATE_NOT_FOUND", "扫描候选不存在", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    public List<ScanCandidateResponse> listByScanTask(Long scanTaskId) {
        return query("""
            WHERE c.scan_task_id = :scanTaskId AND c.deleted = 0
            ORDER BY c.confidence_level, c.id
            """, new MapSqlParameterSource("scanTaskId", scanTaskId));
    }

    public List<ScanCandidateResponse> listPendingReview(String reviewStatus, int limit) {
        return query("""
            WHERE c.deleted = 0 AND c.review_status = :reviewStatus
            ORDER BY c.id DESC
            LIMIT :limit
            """, new MapSqlParameterSource()
            .addValue("reviewStatus", reviewStatus)
            .addValue("limit", Math.max(1, Math.min(limit, 200))));
    }

    public List<ScanCandidateResponse> listByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return query("""
            WHERE c.id IN (:ids) AND c.deleted = 0
            ORDER BY c.id
            """, new MapSqlParameterSource("ids", ids));
    }

    public boolean existsByScanTaskAndRawPath(Long scanTaskId, String rawPath) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM data_asset_scan_candidates
            WHERE scan_task_id = :scanTaskId
              AND raw_path = :rawPath
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("scanTaskId", scanTaskId)
            .addValue("rawPath", rawPath), Integer.class);
        return count != null && count > 0;
    }

    private List<ScanCandidateResponse> query(String whereClause, MapSqlParameterSource params) {
        String sql = """
            SELECT c.id, c.scan_task_id, c.matched_project_id, c.matched_project_code,
                   c.raw_path, c.file_name, c.file_ext, c.size_bytes, c.last_modified_at,
                   c.detected_file_kind, c.detected_discipline, c.detected_version_no,
                   c.confidence_level, c.review_status, c.review_message,
                   c.reviewed_by, c.reviewed_at, c.created_file_resource_id
            FROM data_asset_scan_candidates c
            """ + whereClause;
        return jdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    private static ScanCandidateResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastModifiedAt = rs.getTimestamp("last_modified_at");
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        return new ScanCandidateResponse(
            rs.getLong("id"),
            rs.getLong("scan_task_id"),
            rs.getObject("matched_project_id", Long.class),
            rs.getString("matched_project_code"),
            rs.getString("raw_path"),
            rs.getString("file_name"),
            rs.getString("file_ext"),
            rs.getLong("size_bytes"),
            lastModifiedAt == null ? null : lastModifiedAt.toInstant(),
            rs.getString("detected_file_kind"),
            rs.getString("detected_discipline"),
            rs.getString("detected_version_no"),
            rs.getString("confidence_level"),
            rs.getString("review_status"),
            rs.getString("review_message"),
            rs.getString("reviewed_by"),
            reviewedAt == null ? null : reviewedAt.toInstant(),
            rs.getObject("created_file_resource_id", Long.class)
        );
    }
}

package com.zhuoyu.delivery.datasteward.file;

import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class FileResourceRepository {

    private static final RowMapper<FileResourceResponse> ROW_MAPPER = FileResourceRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FileResourceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(
        Long projectId,
        String originalName,
        String fileKind,
        String mimeType,
        Long sizeBytes,
        String storageUri,
        String checksum,
        String businessTag,
        String versionNo,
        String processStatus,
        Long operatorId
    ) {
        String sql = """
            INSERT INTO data_file_resources (
                project_id, original_name, file_kind, mime_type, size_bytes, storage_uri,
                checksum, business_tag, version_no, process_status, processed_at, created_by, updated_by
            ) VALUES (
                :projectId, :originalName, :fileKind, :mimeType, :sizeBytes, :storageUri,
                :checksum, :businessTag, :versionNo, :processStatus,
                CASE WHEN :processStatus = 'PROCESSED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                :operatorId, :operatorId
            )
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("originalName", originalName)
            .addValue("fileKind", fileKind)
            .addValue("mimeType", mimeType)
            .addValue("sizeBytes", sizeBytes)
            .addValue("storageUri", storageUri)
            .addValue("checksum", checksum)
            .addValue("businessTag", businessTag)
            .addValue("versionNo", versionNo)
            .addValue("processStatus", processStatus)
            .addValue("operatorId", operatorId), keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    public void updateProcessStatus(Long projectId, Long fileId, String processStatus, Long operatorId) {
        String sql = """
            UPDATE data_file_resources
            SET process_status = :processStatus,
                processed_at = CASE WHEN :processStatus = 'PROCESSED' THEN CURRENT_TIMESTAMP ELSE processed_at END,
                updated_by = :operatorId
            WHERE project_id = :projectId AND id = :fileId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("processStatus", processStatus)
            .addValue("operatorId", operatorId));
    }

    public void markDeleted(Long projectId, Long fileId, Long operatorId) {
        String sql = """
            UPDATE data_file_resources
            SET deleted = 1, delete_token = id, updated_by = :operatorId
            WHERE project_id = :projectId AND id = :fileId AND deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("operatorId", operatorId));
    }

    public List<FileResourceResponse> findByProject(Long projectId, String fileKind) {
        String sql = """
            SELECT id, project_id, original_name, file_kind, mime_type, size_bytes, storage_uri,
                   checksum, business_tag, version_no, process_status, processed_at
            FROM data_file_resources
            WHERE project_id = :projectId
              AND deleted = 0
              AND (:fileKind IS NULL OR file_kind = :fileKind)
            ORDER BY id DESC
            """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind), ROW_MAPPER);
    }

    public Optional<FileResourceResponse> findByProjectAndId(Long projectId, Long fileId) {
        String sql = """
            SELECT id, project_id, original_name, file_kind, mime_type, size_bytes, storage_uri,
                   checksum, business_tag, version_no, process_status, processed_at
            FROM data_file_resources
            WHERE project_id = :projectId AND id = :fileId AND deleted = 0
            """;
        List<FileResourceResponse> rows = jdbcTemplate.query(sql, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId), ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public int countByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    public int countByProjectAndKind(Long projectId, String fileKind) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId AND file_kind = :fileKind AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileKind", fileKind), Integer.class);
        return count == null ? 0 : count;
    }

    private static FileResourceResponse map(ResultSet rs, int rowNum) throws SQLException {
        Timestamp processedAt = rs.getTimestamp("processed_at");
        return new FileResourceResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("original_name"),
            rs.getString("file_kind"),
            rs.getString("mime_type"),
            rs.getLong("size_bytes"),
            rs.getString("storage_uri"),
            rs.getString("checksum"),
            rs.getString("business_tag"),
            rs.getString("version_no"),
            rs.getString("process_status"),
            processedAt == null ? null : processedAt.toInstant()
        );
    }
}

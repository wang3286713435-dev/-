package com.zhuoyu.delivery.datasteward.nas.repository;

import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasQuarantineRecordResponse;
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
public class ControlledNasRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ControlledNasRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insertDirectory(
        Long projectId,
        String relativePath,
        String displayName,
        String parentRelativePath,
        Long operatorId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_nas_directory_records (
                project_id, relative_path, display_name, parent_relative_path,
                status, created_by, updated_by
            ) VALUES (
                :projectId, :relativePath, :displayName, :parentRelativePath,
                'ACTIVE', :operatorId, :operatorId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("relativePath", relativePath)
            .addValue("displayName", displayName)
            .addValue("parentRelativePath", blankToNull(parentRelativePath))
            .addValue("operatorId", operatorId), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<DirectoryRecord> findDirectory(Long projectId, String relativePath) {
        List<DirectoryRecord> rows = jdbcTemplate.query("""
            SELECT id, project_id, relative_path, display_name, parent_relative_path,
                   status, quarantine_record_id
            FROM data_nas_directory_records
            WHERE project_id = :projectId
              AND relative_path = :relativePath
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("relativePath", relativePath), ControlledNasRepository::mapDirectory);
        return rows.stream().findFirst();
    }

    public boolean activeDirectoryExists(Long projectId, String relativePath) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_nas_directory_records
            WHERE project_id = :projectId
              AND relative_path = :relativePath
              AND status = 'ACTIVE'
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("relativePath", relativePath), Integer.class);
        return count != null && count > 0;
    }

    public void updateDirectoryPrefix(
        Long projectId,
        String sourceRelativePath,
        String targetRelativePath,
        String targetDisplayName,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_nas_directory_records
            SET relative_path = CASE
                    WHEN relative_path = :sourcePath THEN :targetPath
                    ELSE CONCAT(:targetPath, SUBSTRING(relative_path, CHAR_LENGTH(:sourcePath) + 1))
                END,
                display_name = CASE
                    WHEN relative_path = :sourcePath THEN :targetDisplayName
                    ELSE display_name
                END,
                status = 'ACTIVE',
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND deleted = 0
              AND status = 'ACTIVE'
              AND (relative_path = :sourcePath OR relative_path LIKE :sourcePathLike)
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sourcePath", sourceRelativePath)
            .addValue("sourcePathLike", sourceRelativePath + "/%")
            .addValue("targetPath", targetRelativePath)
            .addValue("targetDisplayName", targetDisplayName)
            .addValue("operatorId", operatorId));
    }

    public void markDirectoryPrefixQuarantined(
        Long projectId,
        String sourceRelativePath,
        Long quarantineRecordId,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_nas_directory_records
            SET status = 'QUARANTINED',
                quarantine_record_id = :quarantineRecordId,
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND deleted = 0
              AND (relative_path = :sourcePath OR relative_path LIKE :sourcePathLike)
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sourcePath", sourceRelativePath)
            .addValue("sourcePathLike", sourceRelativePath + "/%")
            .addValue("quarantineRecordId", quarantineRecordId)
            .addValue("operatorId", operatorId));
    }

    public void restoreDirectoryPrefix(Long projectId, Long quarantineRecordId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE data_nas_directory_records
            SET status = 'ACTIVE',
                quarantine_record_id = NULL,
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND quarantine_record_id = :quarantineRecordId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("quarantineRecordId", quarantineRecordId)
            .addValue("operatorId", operatorId));
    }

    public Optional<FileRecord> findFile(Long projectId, Long fileId) {
        List<FileRecord> rows = jdbcTemplate.query("""
            SELECT id, project_id, original_name, file_kind, mime_type, size_bytes,
                   storage_uri, logical_path, checksum, business_tag, discipline,
                   version_no, process_status
            FROM data_file_resources
            WHERE project_id = :projectId
              AND id = :fileId
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId), ControlledNasRepository::mapFile);
        return rows.stream().findFirst();
    }

    public Long insertUploadedFile(
        Long projectId,
        String originalName,
        String fileKind,
        String mimeType,
        Long sizeBytes,
        String storageUri,
        String logicalPath,
        String discipline,
        String versionNo,
        Long operatorId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_file_resources (
                project_id, original_name, file_kind, mime_type, size_bytes,
                storage_uri, storage_provider, storage_key, logical_path, source_path_digest,
                checksum, business_tag, discipline, source_type, version_no,
                process_status, processed_at, review_status, confidence_level,
                last_verified_at, created_by, updated_by
            ) VALUES (
                :projectId, :originalName, :fileKind, :mimeType, :sizeBytes,
                :storageUri, 'NAS', NULL, :logicalPath, SHA2(:storageUri, 256),
                NULL, :discipline, :discipline, 'USER_UPLOAD', :versionNo,
                'PROCESSED', CURRENT_TIMESTAMP, 'APPROVED', 'HIGH',
                CURRENT_TIMESTAMP, :operatorId, :operatorId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("originalName", originalName)
            .addValue("fileKind", fileKind)
            .addValue("mimeType", blankToNull(mimeType))
            .addValue("sizeBytes", sizeBytes)
            .addValue("storageUri", storageUri)
            .addValue("logicalPath", logicalPath)
            .addValue("discipline", blankToNull(discipline))
            .addValue("versionNo", versionNo)
            .addValue("operatorId", operatorId), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateFilePathAndName(
        Long projectId,
        Long fileId,
        String originalName,
        String storageUri,
        String logicalPath,
        Long sizeBytes,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET original_name = :originalName,
                storage_uri = :storageUri,
                logical_path = :logicalPath,
                source_path_digest = SHA2(:storageUri, 256),
                size_bytes = :sizeBytes,
                process_status = 'PROCESSED',
                review_status = 'APPROVED',
                confidence_level = 'HIGH',
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND id = :fileId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("originalName", originalName)
            .addValue("storageUri", storageUri)
            .addValue("logicalPath", logicalPath)
            .addValue("sizeBytes", sizeBytes)
            .addValue("operatorId", operatorId));
    }

    public void updateFilePrefix(
        Long projectId,
        String sourceStoragePrefix,
        String targetStoragePrefix,
        String sourceLogicalPrefix,
        String targetLogicalPrefix,
        String processStatus,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET storage_uri = CASE
                    WHEN storage_uri = :sourceStorage THEN :targetStorage
                    ELSE CONCAT(:targetStorage, SUBSTRING(storage_uri, CHAR_LENGTH(:sourceStorage) + 1))
                END,
                logical_path = CASE
                    WHEN logical_path = :sourceLogical THEN :targetLogical
                    ELSE CONCAT(:targetLogical, SUBSTRING(logical_path, CHAR_LENGTH(:sourceLogical) + 1))
                END,
                source_path_digest = SHA2(CASE
                    WHEN storage_uri = :sourceStorage THEN :targetStorage
                    ELSE CONCAT(:targetStorage, SUBSTRING(storage_uri, CHAR_LENGTH(:sourceStorage) + 1))
                END, 256),
                process_status = :processStatus,
                review_status = CASE WHEN :processStatus = 'PROCESSED' THEN 'APPROVED' ELSE review_status END,
                confidence_level = 'HIGH',
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND deleted = 0
              AND (logical_path = :sourceLogical OR logical_path LIKE :sourceLogicalLike)
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sourceStorage", sourceStoragePrefix)
            .addValue("targetStorage", targetStoragePrefix)
            .addValue("sourceLogical", sourceLogicalPrefix)
            .addValue("targetLogical", targetLogicalPrefix)
            .addValue("sourceLogicalLike", sourceLogicalPrefix + "/%")
            .addValue("processStatus", processStatus)
            .addValue("operatorId", operatorId));
    }

    public void markFileQuarantined(
        Long projectId,
        Long fileId,
        String quarantineStorageUri,
        String quarantineLogicalPath,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET storage_uri = :storageUri,
                logical_path = :logicalPath,
                source_path_digest = SHA2(:storageUri, 256),
                process_status = 'QUARANTINED',
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND id = :fileId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("storageUri", quarantineStorageUri)
            .addValue("logicalPath", quarantineLogicalPath)
            .addValue("operatorId", operatorId));
    }

    public void restoreFile(
        Long projectId,
        Long fileId,
        String storageUri,
        String logicalPath,
        Long sizeBytes,
        Long operatorId
    ) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET storage_uri = :storageUri,
                logical_path = :logicalPath,
                source_path_digest = SHA2(:storageUri, 256),
                size_bytes = :sizeBytes,
                process_status = 'PROCESSED',
                review_status = 'APPROVED',
                confidence_level = 'HIGH',
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND id = :fileId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("storageUri", storageUri)
            .addValue("logicalPath", logicalPath)
            .addValue("sizeBytes", sizeBytes)
            .addValue("operatorId", operatorId));
    }

    public void markBindingsFileQuarantined(Long projectId, Long fileId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE work_delivery_bindings
            SET binding_status = 'FILE_QUARANTINED',
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND file_resource_id = :fileId
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("operatorId", operatorId));
    }

    public void restoreBindingsForFile(Long projectId, Long fileId, Long operatorId) {
        jdbcTemplate.update("""
            UPDATE work_delivery_bindings
            SET binding_status = 'BOUND',
                updated_by = :operatorId
            WHERE project_id = :projectId
              AND file_resource_id = :fileId
              AND binding_status = 'FILE_QUARANTINED'
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("operatorId", operatorId));
    }

    public Long insertQuarantineRecord(
        Long projectId,
        String targetType,
        Long fileId,
        Long directoryId,
        String originalRelativePath,
        String quarantineRelativePath,
        String displayName,
        String reason,
        Instant quarantineUntil,
        Long operatorId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_nas_quarantine_records (
                project_id, target_type, file_id, directory_id,
                original_relative_path, quarantine_relative_path, display_name,
                status, reason, quarantine_until, created_by
            ) VALUES (
                :projectId, :targetType, :fileId, :directoryId,
                :originalPath, :quarantinePath, :displayName,
                'QUARANTINED', :reason, :quarantineUntil, :operatorId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("targetType", targetType)
            .addValue("fileId", fileId)
            .addValue("directoryId", directoryId)
            .addValue("originalPath", originalRelativePath)
            .addValue("quarantinePath", quarantineRelativePath)
            .addValue("displayName", displayName)
            .addValue("reason", blankToNull(reason))
            .addValue("quarantineUntil", Timestamp.from(quarantineUntil))
            .addValue("operatorId", operatorId), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<QuarantineRecord> findQuarantineRecord(Long projectId, Long quarantineRecordId) {
        List<QuarantineRecord> rows = jdbcTemplate.query("""
            SELECT id, project_id, target_type, file_id, directory_id,
                   original_relative_path, quarantine_relative_path, display_name,
                   status, reason, quarantine_until, created_by
            FROM data_nas_quarantine_records
            WHERE project_id = :projectId
              AND id = :id
            LIMIT 1
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("id", quarantineRecordId), ControlledNasRepository::mapQuarantineRecord);
        return rows.stream().findFirst();
    }

    public void markQuarantineRestored(Long projectId, Long quarantineRecordId, Long operatorId, String failureReason) {
        jdbcTemplate.update("""
            UPDATE data_nas_quarantine_records
            SET status = CASE WHEN :failureReason IS NULL THEN 'RESTORED' ELSE 'FAILED' END,
                restored_by = :operatorId,
                restored_at = CURRENT_TIMESTAMP,
                failure_reason = :failureReason
            WHERE project_id = :projectId
              AND id = :id
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("id", quarantineRecordId)
            .addValue("operatorId", operatorId)
            .addValue("failureReason", blankToNull(failureReason)));
    }

    public Long insertOperation(
        Long projectId,
        String operationType,
        String targetType,
        Long targetId,
        Long fileId,
        Long directoryId,
        Long quarantineRecordId,
        String sourcePathHash,
        String targetPathHash,
        String sourceDisplayPath,
        String targetDisplayPath,
        String status,
        String message,
        String failureReason,
        String traceId,
        Long operatorId
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update("""
            INSERT INTO data_nas_operation_records (
                project_id, operation_type, target_type, target_id, file_id, directory_id,
                quarantine_record_id, source_path_hash, target_path_hash, source_display_path,
                target_display_path, status, message, failure_reason, trace_id, created_by
            ) VALUES (
                :projectId, :operationType, :targetType, :targetId, :fileId, :directoryId,
                :quarantineRecordId, :sourcePathHash, :targetPathHash, :sourceDisplayPath,
                :targetDisplayPath, :status, :message, :failureReason, :traceId, :operatorId
            )
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("operationType", operationType)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("fileId", fileId)
            .addValue("directoryId", directoryId)
            .addValue("quarantineRecordId", quarantineRecordId)
            .addValue("sourcePathHash", sourcePathHash)
            .addValue("targetPathHash", targetPathHash)
            .addValue("sourceDisplayPath", blankToNull(sourceDisplayPath))
            .addValue("targetDisplayPath", blankToNull(targetDisplayPath))
            .addValue("status", status)
            .addValue("message", blankToNull(message))
            .addValue("failureReason", blankToNull(failureReason))
            .addValue("traceId", traceId)
            .addValue("operatorId", operatorId), keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<NasOperationRecordResponse> listOperations(Long projectId, int limit) {
        return jdbcTemplate.query("""
            SELECT id, project_id, operation_type, target_type, target_id, file_id,
                   directory_id, quarantine_record_id, source_display_path,
                   target_display_path, status, message, failure_reason,
                   trace_id, created_at, created_by
            FROM data_nas_operation_records
            WHERE project_id = :projectId
            ORDER BY id DESC
            LIMIT :limit
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("limit", Math.max(1, Math.min(limit, 100))), ControlledNasRepository::mapOperationResponse);
    }

    public List<NasQuarantineRecordResponse> listQuarantineRecords(Long projectId, String status, int limit) {
        return jdbcTemplate.query("""
            SELECT id, project_id, target_type, file_id, directory_id,
                   original_relative_path, display_name, status, reason,
                   quarantine_until, created_at, created_by, restored_by,
                   restored_at, failure_reason
            FROM data_nas_quarantine_records
            WHERE project_id = :projectId
              AND (:status IS NULL OR status = :status)
            ORDER BY id DESC
            LIMIT :limit
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("status", blankToNull(status))
            .addValue("limit", Math.max(1, Math.min(limit, 100))), ControlledNasRepository::mapQuarantineResponse);
    }

    private static DirectoryRecord mapDirectory(ResultSet rs, int rowNum) throws SQLException {
        Long quarantineRecordId = rs.getObject("quarantine_record_id") == null ? null : rs.getLong("quarantine_record_id");
        return new DirectoryRecord(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("relative_path"),
            rs.getString("display_name"),
            rs.getString("parent_relative_path"),
            rs.getString("status"),
            quarantineRecordId
        );
    }

    private static FileRecord mapFile(ResultSet rs, int rowNum) throws SQLException {
        return new FileRecord(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("original_name"),
            rs.getString("file_kind"),
            rs.getString("mime_type"),
            rs.getLong("size_bytes"),
            rs.getString("storage_uri"),
            rs.getString("logical_path"),
            rs.getString("checksum"),
            rs.getString("business_tag"),
            rs.getString("discipline"),
            rs.getString("version_no"),
            rs.getString("process_status")
        );
    }

    private static QuarantineRecord mapQuarantineRecord(ResultSet rs, int rowNum) throws SQLException {
        Long fileId = rs.getObject("file_id") == null ? null : rs.getLong("file_id");
        Long directoryId = rs.getObject("directory_id") == null ? null : rs.getLong("directory_id");
        Timestamp quarantineUntil = rs.getTimestamp("quarantine_until");
        return new QuarantineRecord(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("target_type"),
            fileId,
            directoryId,
            rs.getString("original_relative_path"),
            rs.getString("quarantine_relative_path"),
            rs.getString("display_name"),
            rs.getString("status"),
            rs.getString("reason"),
            quarantineUntil == null ? null : quarantineUntil.toInstant(),
            rs.getLong("created_by")
        );
    }

    private static NasOperationRecordResponse mapOperationResponse(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Long targetId = rs.getObject("target_id") == null ? null : rs.getLong("target_id");
        Long fileId = rs.getObject("file_id") == null ? null : rs.getLong("file_id");
        Long directoryId = rs.getObject("directory_id") == null ? null : rs.getLong("directory_id");
        Long quarantineRecordId = rs.getObject("quarantine_record_id") == null ? null : rs.getLong("quarantine_record_id");
        Long createdBy = rs.getObject("created_by") == null ? null : rs.getLong("created_by");
        return new NasOperationRecordResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("operation_type"),
            rs.getString("target_type"),
            targetId,
            fileId,
            directoryId,
            quarantineRecordId,
            rs.getString("source_display_path"),
            rs.getString("target_display_path"),
            rs.getString("status"),
            rs.getString("message"),
            rs.getString("failure_reason"),
            rs.getString("trace_id"),
            createdAt == null ? null : createdAt.toInstant(),
            createdBy
        );
    }

    private static NasQuarantineRecordResponse mapQuarantineResponse(ResultSet rs, int rowNum) throws SQLException {
        Timestamp quarantineUntil = rs.getTimestamp("quarantine_until");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp restoredAt = rs.getTimestamp("restored_at");
        Long fileId = rs.getObject("file_id") == null ? null : rs.getLong("file_id");
        Long directoryId = rs.getObject("directory_id") == null ? null : rs.getLong("directory_id");
        Long createdBy = rs.getObject("created_by") == null ? null : rs.getLong("created_by");
        Long restoredBy = rs.getObject("restored_by") == null ? null : rs.getLong("restored_by");
        return new NasQuarantineRecordResponse(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getString("target_type"),
            fileId,
            directoryId,
            rs.getString("original_relative_path"),
            rs.getString("display_name"),
            rs.getString("status"),
            rs.getString("reason"),
            quarantineUntil == null ? null : quarantineUntil.toInstant(),
            createdAt == null ? null : createdAt.toInstant(),
            createdBy,
            restoredBy,
            restoredAt == null ? null : restoredAt.toInstant(),
            rs.getString("failure_reason")
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DirectoryRecord(
        Long id,
        Long projectId,
        String relativePath,
        String displayName,
        String parentRelativePath,
        String status,
        Long quarantineRecordId
    ) {
    }

    public record FileRecord(
        Long id,
        Long projectId,
        String originalName,
        String fileKind,
        String mimeType,
        Long sizeBytes,
        String storageUri,
        String logicalPath,
        String checksum,
        String businessTag,
        String discipline,
        String versionNo,
        String processStatus
    ) {
    }

    public record QuarantineRecord(
        Long id,
        Long projectId,
        String targetType,
        Long fileId,
        Long directoryId,
        String originalRelativePath,
        String quarantineRelativePath,
        String displayName,
        String status,
        String reason,
        Instant quarantineUntil,
        Long createdBy
    ) {
    }
}

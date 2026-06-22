package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.core.rbac.repository.PermissionRepository;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AccessTicketCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AccessTicketResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectArchiveRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectArchiveResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DisciplineResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetMetadataUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FilePreviewResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ImportResultResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ImportRowError;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NasProjectDiscoveryRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NasProjectDiscoveryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NasProjectDiscoveryRow;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryDiscoverRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryDiscoverResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PreviewArtifactResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectLifecycleCreateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ReviewUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanCandidateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanReportResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetDisciplineRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetImportJobRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetImportRowRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetScanCandidateRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetScanTaskRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.FileAccessTicketRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.NonstandardDirectoryRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.StorageRootRepository;
import com.zhuoyu.delivery.datasteward.storage.StorageService;
import com.zhuoyu.delivery.shared.api.PageResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.preview.FilePreviewPolicy;
import com.zhuoyu.delivery.shared.preview.PreviewDecision;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetApplicationService {

    private static final String MODULE_CODE = "data-steward";
    private static final String PERMISSION_FILE_PREVIEW = "DATA_STEWARD_FILE_PREVIEW";
    private static final String PERMISSION_FILE_DOWNLOAD = "DATA_STEWARD_FILE_DOWNLOAD";
    private static final String DEFAULT_SCAN_EXTENSIONS = ".rvt,.dwg,.ifc,.nwd,.nwc,.dxf,.pdf,.doc,.docx,.wps,.xls,.xlsx,.ppt,.pptx,.glb,.gltf,.zip,.rar";
    private static final List<String> DEFAULT_SKIP_DIRECTORY_KEYWORDS = List.of(
        ".git", ".svn", ".hg", ".idea", ".vscode", "node_modules",
        "__MACOSX", ".TemporaryItems", ".Spotlight-V100", "$RECYCLE.BIN",
        "temp", "tmp", "cache", "临时", "临时文件", "新建文件夹", "转换"
    );
    private static final Pattern LEADING_PROJECT_NO = Pattern.compile("^(\\d{2,4})(?:[-_、\\s].*)?$");
    private static final List<String> DEFAULT_DEFERRED_PROJECT_CODES = List.of("95", "98", "99");
    private static final List<String> NONSTANDARD_STATUSES = List.of(
        "PENDING_AGENT", "HUMAN_REVIEW", "APPROVED_FOR_IMPORT", "IGNORED", "DEFERRED"
    );
    private static final List<String> NONSTANDARD_RISK_TYPES = List.of(
        "USER_DEFERRED", "DUPLICATE_CODE", "REFERENCE", "UNKNOWN_CODE", "TEMP", "MIXED_COLLECTION", "OTHER"
    );
    private static final List<String> FILE_KINDS = List.of(
        "MODEL", "DRAWING", "DOCUMENT", "SPREADSHEET", "PRESENTATION", "MODEL_VIEWER", "ARCHIVE", "OTHER"
    );
    private static final List<String> CONFIDENCE_LEVELS = List.of("HIGH", "MEDIUM", "LOW");
    private static final List<String> FILE_REVIEW_STATUSES = List.of("APPROVED", "PENDING", "REJECTED", "AUTO_INGESTED");

    private final BimAssetRepository bimAssetRepository;
    private final AssetPathMappingRepository pathMappingRepository;
    private final AssetScanTaskRepository scanTaskRepository;
    private final AssetScanCandidateRepository scanCandidateRepository;
    private final AssetDisciplineRepository disciplineRepository;
    private final AssetImportJobRepository importJobRepository;
    private final AssetImportRowRepository importRowRepository;
    private final NonstandardDirectoryRepository nonstandardDirectoryRepository;
    private final StorageRootRepository storageRootRepository;
    private final FileAccessTicketRepository fileAccessTicketRepository;
    private final PermissionRepository permissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogApplicationService auditLogApplicationService;
    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final EventApplicationService eventApplicationService;
    private final StorageService storageService;

    public AssetApplicationService(
        BimAssetRepository bimAssetRepository,
        AssetPathMappingRepository pathMappingRepository,
        AssetScanTaskRepository scanTaskRepository,
        AssetScanCandidateRepository scanCandidateRepository,
        AssetDisciplineRepository disciplineRepository,
        AssetImportJobRepository importJobRepository,
        AssetImportRowRepository importRowRepository,
        NonstandardDirectoryRepository nonstandardDirectoryRepository,
        StorageRootRepository storageRootRepository,
        FileAccessTicketRepository fileAccessTicketRepository,
        PermissionRepository permissionRepository,
        JdbcTemplate jdbcTemplate,
        AuditLogApplicationService auditLogApplicationService,
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        EventApplicationService eventApplicationService,
        StorageService storageService
    ) {
        this.bimAssetRepository = bimAssetRepository;
        this.pathMappingRepository = pathMappingRepository;
        this.scanTaskRepository = scanTaskRepository;
        this.scanCandidateRepository = scanCandidateRepository;
        this.disciplineRepository = disciplineRepository;
        this.importJobRepository = importJobRepository;
        this.importRowRepository = importRowRepository;
        this.nonstandardDirectoryRepository = nonstandardDirectoryRepository;
        this.storageRootRepository = storageRootRepository;
        this.fileAccessTicketRepository = fileAccessTicketRepository;
        this.permissionRepository = permissionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogApplicationService = auditLogApplicationService;
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.eventApplicationService = eventApplicationService;
        this.storageService = storageService;
    }

    // ===== disciplines =====

    public List<DisciplineResponse> listDisciplines(Long userId, Long projectId) {
        if (projectId == null) {
            return disciplineRepository.listBuiltin();
        }
        requireProjectAccess(userId, projectId);
        return disciplineRepository.listByProject(projectId);
    }

    // ===== project assets =====

    @Transactional
    public AssetProjectResponse createProject(Long userId, AssetProjectCreateRequest request) {
        return createProjectLifecycle(userId, request).project();
    }

    @Transactional
    public ProjectLifecycleCreateResponse createProjectLifecycle(Long userId, AssetProjectCreateRequest request) {
        requireSuperAdmin(userId);
        String code = normalizeProjectCode(request.code());
        String name = normalizeProjectName(request.name());
        assertProjectCodeAvailable(code);
        Long projectId = bimAssetRepository.upsertProject(
            code, name,
            defaultString(request.industryType(), "OTHER"), defaultString(request.projectStage(), "UNKNOWN"),
            blankToNull(request.projectManagerName()), blankToNull(request.ownerOrgName()),
            defaultString(request.assetSource(), "MANUAL"), userId);
        bimAssetRepository.grantProjectAdmin(userId, projectId, userId);
        initializeProjectStorageWorkspace(projectId, userId);
        SectionRootInitializationResult sectionRoot = ensureSectionRoot(projectId, name, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "asset.project.create",
            "PROJECT", String.valueOf(projectId), userId,
            Map.of(
                "code", code,
                "name", name,
                "storageWorkspaceStatus", "CREATED",
                "sectionRootStatus", sectionRoot.status()
            ));
        eventApplicationService.record("PROJECT", projectId, "PROJECT", String.valueOf(projectId),
            "project.create", userId, "API", "创建项目: " + code + " - " + name, null);
        AssetProjectResponse project = bimAssetRepository.listProjects(userId, null).stream()
            .filter(p -> p.projectId().equals(projectId)).findFirst().orElseThrow();
        return new ProjectLifecycleCreateResponse(
            projectId,
            project.code(),
            project.name(),
            true,
            "CREATED",
            sectionRoot.status(),
            sectionRoot.nodeId(),
            project
        );
    }

    @Transactional
    public AssetProjectArchiveResponse archiveProject(
        Long userId,
        Long projectId,
        AssetProjectArchiveRequest request
    ) {
        requireSuperAdmin(userId);
        ProjectCoreRecord project = requireActiveProject(projectId);
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessException("ASSET_PROJECT_ARCHIVE_CONFIRM_REQUIRED",
                "归档项目必须二次确认", HttpStatus.BAD_REQUEST);
        }
        String confirmText = blankToNull(request.confirmText());
        if (confirmText == null
            || (!project.code().equalsIgnoreCase(confirmText) && !project.name().equals(confirmText))) {
            throw new BusinessException("ASSET_PROJECT_ARCHIVE_CONFIRM_TEXT_INVALID",
                "请输入项目编码或项目名称确认归档", HttpStatus.BAD_REQUEST);
        }
        jdbcTemplate.update("""
            UPDATE core_projects
            SET asset_status = 'ARCHIVED',
                status = 'INACTIVE',
                deleted = 1,
                updated_by = ?
            WHERE id = ?
              AND deleted = 0
            """, userId, projectId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "asset.project.archive",
            "PROJECT", String.valueOf(projectId), userId,
            Map.of("code", project.code(), "name", project.name(), "archiveStatus", "ARCHIVED"));
        eventApplicationService.record("PROJECT", projectId, "PROJECT", String.valueOf(projectId),
            "project.archive", userId, "API", "归档项目: " + project.code() + " - " + project.name(), null);
        return new AssetProjectArchiveResponse(
            projectId,
            project.code(),
            project.name(),
            true,
            "ARCHIVED",
            false,
            false
        );
    }

    @Transactional
    public AssetProjectResponse updateProject(Long userId, Long projectId, AssetProjectUpdateRequest request) {
        if (request.assetStatus() != null) {
            String status = request.assetStatus().toUpperCase();
            if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
                throw new BusinessException("ASSET_PROJECT_STATUS_INVALID", "项目资产状态只能是 ACTIVE 或 INACTIVE", HttpStatus.BAD_REQUEST);
            }
        }
        AssetProjectResponse existing = bimAssetRepository.listProjects(userId, null).stream()
            .filter(p -> p.projectId().equals(projectId)).findFirst()
            .orElseThrow(() -> new BusinessException("CORE_PROJECT_NOT_FOUND", "项目不存在", HttpStatus.NOT_FOUND));
        bimAssetRepository.upsertProject(
            existing.code(), defaultString(request.name(), existing.name()),
            defaultString(request.industryType(), existing.industryType()),
            defaultString(request.projectStage(), existing.projectStage()),
            defaultString(request.projectManagerName(), existing.projectManagerName()),
            defaultString(request.ownerOrgName(), ""),
            defaultString(request.assetSource(), existing.assetSource()),
            userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "asset.project.update",
            "PROJECT", String.valueOf(projectId), userId, Map.of());
        eventApplicationService.record("PROJECT", projectId, "PROJECT", String.valueOf(projectId),
            "project.update", userId, "API", "更新项目: " + existing.code(), null);
        return bimAssetRepository.listProjects(userId, null).stream()
            .filter(p -> p.projectId().equals(projectId)).findFirst().orElseThrow();
    }

    public List<AssetProjectResponse> listProjects(Long userId, String keyword) {
        return bimAssetRepository.listProjects(userId, keyword);
    }

    public List<AssetProjectResponse> listProjects(Long userId, String keyword, String assetSource) {
        return bimAssetRepository.listProjects(userId, keyword, assetSource);
    }

    private void requireSuperAdmin(Long userId) {
        List<String> rows = jdbcTemplate.query("""
            SELECT username
            FROM core_users
            WHERE id = ?
              AND status = 'ACTIVE'
              AND deleted = 0
            LIMIT 1
            """, (rs, rowNum) -> rs.getString("username"), userId);
        String username = rows.isEmpty() ? null : rows.getFirst();
        if (!"admin".equalsIgnoreCase(username)) {
            throw new BusinessException("ASSET_PROJECT_LIFECYCLE_FORBIDDEN",
                "仅超级管理员可以创建或归档项目", HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeProjectCode(String code) {
        String normalized = requireText(code, "ASSET_PROJECT_CODE_REQUIRED", "项目编码不能为空");
        if (normalized.length() > 64) {
            throw new BusinessException("ASSET_PROJECT_CODE_TOO_LONG",
                "项目编码不能超过 64 个字符", HttpStatus.BAD_REQUEST);
        }
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains(":")) {
            throw new BusinessException("ASSET_PROJECT_CODE_INVALID",
                "项目编码不能包含路径分隔符", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeProjectName(String name) {
        String normalized = requireText(name, "ASSET_PROJECT_NAME_REQUIRED", "项目名称不能为空");
        if (normalized.length() > 255) {
            throw new BusinessException("ASSET_PROJECT_NAME_TOO_LONG",
                "项目名称不能超过 255 个字符", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void assertProjectCodeAvailable(String code) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_projects
            WHERE code = ?
            """, Integer.class, code);
        if (count != null && count > 0) {
            throw new BusinessException("ASSET_PROJECT_CODE_DUPLICATED",
                "项目编码已存在，请更换编码", HttpStatus.CONFLICT);
        }
    }

    private void initializeProjectStorageWorkspace(Long projectId, Long userId) {
        byte[] payload = ("delivery-project-workspace:" + projectId).getBytes(StandardCharsets.UTF_8);
        String assetUuid = UUID.nameUUIDFromBytes(("project-workspace:" + projectId).getBytes(StandardCharsets.UTF_8)).toString();
        StorageService.ObjectWriteResult writeResult = storageService.writeUploadToObject(
            projectId,
            assetUuid,
            ".workspace-keep",
            stableDigest(new String(payload, StandardCharsets.UTF_8)),
            "text/plain",
            (long) payload.length,
            new ByteArrayInputStream(payload),
            "MINIO"
        );
        upsertUploadStorageObject(writeResult, userId);
    }

    private Long upsertUploadStorageObject(StorageService.ObjectWriteResult writeResult, Long userId) {
        jdbcTemplate.update("""
            INSERT INTO data_storage_objects (
                provider, bucket, object_key, etag, checksum, content_type, size_bytes,
                source_provider, source_uri_digest, source_path_digest,
                storage_state, migration_status, last_verified_at, created_by, updated_by
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                'PLATFORM', ?, NULL,
                'OBJECT_STORED', 'COMPLETED', ?, ?, ?
            )
            ON DUPLICATE KEY UPDATE
                etag = VALUES(etag),
                checksum = VALUES(checksum),
                content_type = VALUES(content_type),
                size_bytes = VALUES(size_bytes),
                source_provider = 'PLATFORM',
                source_uri_digest = VALUES(source_uri_digest),
                storage_state = 'OBJECT_STORED',
                migration_status = 'COMPLETED',
                last_verified_at = VALUES(last_verified_at),
                updated_by = VALUES(updated_by),
                deleted = 0,
                delete_token = 0
            """,
            writeResult.provider(), writeResult.bucket(), writeResult.objectKey(), writeResult.etag(),
            writeResult.checksum(), writeResult.contentType(), writeResult.sizeBytes(),
            writeResult.sourceUriDigest(), timestamp(writeResult.verifiedAt()), userId, userId);
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM data_storage_objects
            WHERE provider = ?
              AND bucket = ?
              AND object_key = ?
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"),
            writeResult.provider(), writeResult.bucket(), writeResult.objectKey());
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_PROJECT_WORKSPACE_RECORD_FAILED",
                "项目对象存储工作区记录写入失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return rows.getFirst();
    }

    private SectionRootInitializationResult ensureSectionRoot(Long projectId, String projectName, Long userId) {
        List<Long> existingRoot = jdbcTemplate.query("""
            SELECT id
            FROM masterdata_section_nodes
            WHERE project_id = ?
              AND parent_id IS NULL
              AND deleted = 0
            ORDER BY id
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), projectId);
        if (!existingRoot.isEmpty()) {
            return new SectionRootInitializationResult("EXISTING", existingRoot.getFirst());
        }
        jdbcTemplate.update("""
            INSERT INTO masterdata_section_nodes (
                project_id, parent_id, code, name, level, path,
                sort_order, status, created_by, updated_by
            ) VALUES (
                ?, NULL, 'ROOT', ?, 1, '',
                0, 'ACTIVE', ?, ?
            )
            """, projectId, projectName, userId, userId);
        Long nodeId = jdbcTemplate.query("""
            SELECT id
            FROM masterdata_section_nodes
            WHERE project_id = ?
              AND code = 'ROOT'
              AND deleted = 0
            ORDER BY id
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), projectId).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException("ASSET_PROJECT_SECTION_ROOT_FAILED",
                "工程树根节点初始化失败", HttpStatus.INTERNAL_SERVER_ERROR));
        jdbcTemplate.update("""
            UPDATE masterdata_section_nodes
            SET path = ?,
                updated_by = ?
            WHERE id = ?
            """, "/" + nodeId, userId, nodeId);
        return new SectionRootInitializationResult("CREATED", nodeId);
    }

    private ProjectCoreRecord requireActiveProject(Long projectId) {
        List<ProjectCoreRecord> rows = jdbcTemplate.query("""
            SELECT id, code, name
            FROM core_projects
            WHERE id = ?
              AND deleted = 0
            LIMIT 1
            """, (rs, rowNum) -> new ProjectCoreRecord(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name")
        ), projectId);
        if (rows.isEmpty()) {
            throw new BusinessException("CORE_PROJECT_NOT_FOUND", "项目不存在或已归档", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    private record SectionRootInitializationResult(String status, Long nodeId) {
    }

    private record ProjectCoreRecord(Long projectId, String code, String name) {
    }

    // ===== path mappings =====

    @Transactional
    public Long createPathMapping(Long userId, Long projectId, String providerCode,
                                   String nasPath, String matchStrategy, Integer sortOrder, String remark) {
        if (projectId == null) {
            throw new BusinessException("ASSET_PROJECT_REQUIRED", "路径映射必须指定项目", HttpStatus.BAD_REQUEST);
        }
        requireProjectAccess(userId, projectId);
        requireText(nasPath, "ASSET_PATH_REQUIRED", "NAS路径不能为空");
        Long mappingId = pathMappingRepository.insert(projectId,
            defaultString(providerCode, "NAS"), nasPath,
            defaultString(matchStrategy, "PREFIX"),
            sortOrder == null ? 0 : sortOrder, remark, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "asset.pathMapping.create",
            "PATH_MAPPING", String.valueOf(mappingId), userId,
            Map.of("nasPath", nasPath));
        eventApplicationService.record("MAPPING", projectId, "PATH_MAPPING", String.valueOf(mappingId),
            "mapping.create", userId, "API", "创建路径映射: " + nasPath, null);
        return mappingId;
    }

    @Transactional
    public void updatePathMapping(Long userId, Long mappingId, String nasPath,
                                   String matchStrategy, Boolean enabled, Integer sortOrder, String remark) {
        PathMappingResponse existing = pathMappingRepository.requireById(mappingId);
        requireProjectAccess(userId, existing.projectId());
        pathMappingRepository.update(mappingId, nasPath, matchStrategy, enabled, sortOrder, remark, userId);
        auditLogApplicationService.record(existing.projectId(), MODULE_CODE, "asset.pathMapping.update",
            "PATH_MAPPING", String.valueOf(mappingId), userId, Map.of());
        eventApplicationService.record("MAPPING", existing.projectId(), "PATH_MAPPING", String.valueOf(mappingId),
            "mapping.update", userId, "API", "更新路径映射", null);
    }

    @Transactional
    public void deletePathMapping(Long userId, Long mappingId) {
        PathMappingResponse existing = pathMappingRepository.requireById(mappingId);
        requireProjectAccess(userId, existing.projectId());
        pathMappingRepository.markDeleted(mappingId);
        auditLogApplicationService.record(existing.projectId(), MODULE_CODE, "asset.pathMapping.delete",
            "PATH_MAPPING", String.valueOf(mappingId), userId, Map.of());
        eventApplicationService.record("MAPPING", existing.projectId(), "PATH_MAPPING", String.valueOf(mappingId),
            "mapping.delete", userId, "API", "删除路径映射", null);
    }

    // ===== CSV import projects =====

    @Transactional
    public ImportResultResponse importProjectsFromCsv(Long userId, String sourceName, String csvText) {
        if (csvText == null || csvText.isBlank()) {
            throw new BusinessException("ASSET_IMPORT_CSV_EMPTY", "导入文本不能为空", HttpStatus.BAD_REQUEST);
        }
        String[] lines = csvText.strip().split("\\R");
        if (lines.length < 2) {
            throw new BusinessException("ASSET_IMPORT_CSV_NO_DATA", "CSV至少需要表头和一行数据", HttpStatus.BAD_REQUEST);
        }
        Long jobId = importJobRepository.insert("PROJECT_IMPORT", sourceName, "RUNNING", 0, 0, 0, null, userId);
        int totalCount = 0;
        int successCount = 0;
        int failureCount = 0;
        for (int i = 1; i < lines.length; i++) {
            totalCount++;
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            try {
                String[] parts = parseCsvLine(line);
                if (parts.length < 2) {
                    failureCount++;
                    importRowRepository.insert(jobId, i + 1, line, false, "PROJECT", null, "CSV_COLUMNS_INSUFFICIENT", "至少需要项目编码和名称两列");
                    continue;
                }
                String code = parts[0].strip();
                String name = parts.length > 1 ? parts[1].strip() : "";
                if (code.isEmpty() || name.isEmpty()) {
                    failureCount++;
                    importRowRepository.insert(jobId, i + 1, line, false, "PROJECT", null, "CSV_FIELD_EMPTY", "项目编码或名称不能为空");
                    continue;
                }
                Long projectId = bimAssetRepository.upsertProject(code, name,
                    parts.length > 2 ? blankToNull(parts[2]) : null,
                    parts.length > 3 ? blankToNull(parts[3]) : null,
                    parts.length > 4 ? blankToNull(parts[4]) : null,
                    parts.length > 5 ? blankToNull(parts[5]) : null,
                    parts.length > 6 ? blankToNull(parts[6]) : null,
                    userId);
                bimAssetRepository.grantProjectAdmin(userId, projectId, userId);
                importRowRepository.insert(jobId, i + 1, line, true, "PROJECT", projectId, null, null);
                successCount++;
                auditLogApplicationService.record(projectId, MODULE_CODE, "asset.project.import",
                    "PROJECT", String.valueOf(projectId), userId, Map.of("code", code));
                eventApplicationService.record("PROJECT", projectId, "PROJECT", String.valueOf(projectId),
                    "project.import", userId, "API", "导入项目: " + code, null);
            } catch (Exception e) {
                failureCount++;
                importRowRepository.insert(jobId, i + 1, line, false, "PROJECT", null, "IMPORT_ROW_ERROR", truncate(e.getMessage(), 500));
            }
        }
        String status = failureCount > 0 ? "PARTIAL" : "SUCCESS";
        jdbcTemplate.update("UPDATE data_asset_import_jobs SET status=?, total_count=?, success_count=?, failure_count=? WHERE id=?",
            status, totalCount, successCount, failureCount, jobId);
        return buildImportResult(jobId, sourceName, status, totalCount, successCount, failureCount);
    }

    // ===== NAS scan =====

    @Transactional
    public ScanTaskResponse createScan(Long userId, String rootCode, String rootPath,
                                        Long projectId, String projectCode,
                                        Boolean recursive, List<String> extensions,
                                        Boolean skipLowValueDirectories,
                                        List<String> skipDirectoryKeywords) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
        }
        String extensionsStr = extensions != null && !extensions.isEmpty()
            ? String.join(",", extensions) : DEFAULT_SCAN_EXTENSIONS;
        String skipDirectoryKeywordsStr = skipDirectoryKeywords != null && !skipDirectoryKeywords.isEmpty()
            ? String.join(",", skipDirectoryKeywords) : null;
        Long taskId = scanTaskRepository.insert(rootCode, rootPath, projectId,
            blankToNull(projectCode), Boolean.TRUE.equals(recursive), extensionsStr,
            Boolean.TRUE.equals(skipLowValueDirectories), skipDirectoryKeywordsStr, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "asset.scan.create",
            "SCAN_TASK", String.valueOf(taskId), userId,
            Map.of("rootCode", rootCode, "rootPath", rootPath));
        eventApplicationService.record("SCAN", projectId, "SCAN_TASK", String.valueOf(taskId),
            "scan.create", userId, "API", "创建扫描任务: " + rootCode + " - " + rootPath, null);
        return scanTaskRepository.requireById(taskId);
    }

    public ScanTaskResponse runScan(Long userId, Long taskId) {
        ScanTaskResponse task = getScanForUser(userId, taskId);
        if ("RUNNING".equals(task.status())) {
            throw new BusinessException("ASSET_SCAN_ALREADY_RUNNING", "扫描任务正在运行", HttpStatus.BAD_REQUEST);
        }
        if ("SUCCEEDED".equals(task.status())) {
            throw new BusinessException("ASSET_SCAN_ALREADY_SUCCEEDED", "扫描任务已完成，如需重扫请创建新任务", HttpStatus.BAD_REQUEST);
        }
        scanTaskRepository.markRunning(taskId);
        auditLogApplicationService.record(task.projectId(), MODULE_CODE, "asset.scan.run",
            "SCAN_TASK", String.valueOf(taskId), userId, Map.of());
        eventApplicationService.record("SCAN", task.projectId(), "SCAN_TASK", String.valueOf(taskId),
            "scan.start", userId, "API", "开始扫描任务: " + task.rootCode(), null);

        java.util.Set<Long> allowedIds = accessibleProjectIds(userId);
        List<PathMappingResponse> scopedMappings;
        if (task.projectId() != null) {
            // scan pinned to one project — only that project's mappings
            if (!allowedIds.contains(task.projectId())) {
                throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该扫描任务的项目", HttpStatus.FORBIDDEN);
            }
            scopedMappings = pathMappingRepository.list(task.projectId(), true);
        } else {
            // global scan — only mappings belonging to user's projects
            scopedMappings = new java.util.ArrayList<>();
            for (Long pid : allowedIds) {
                scopedMappings.addAll(pathMappingRepository.list(pid, true));
            }
        }
        return executeScan(task, scopedMappings, userId, allowedIds);
    }

    public ScanTaskResponse createAndRunPatrolScan(Long systemUserId, PathMappingResponse mapping) {
        if (systemUserId == null || mapping == null || mapping.projectId() == null || mapping.nasPath() == null) {
            throw new BusinessException("ASSET_SCAN_PATROL_INVALID", "巡检扫描参数不完整", HttpStatus.BAD_REQUEST);
        }
        String rootCode = truncate("HOURLY_PATROL_" + valueOrDash(mapping.projectCode()), 64);
        Long taskId = scanTaskRepository.insert(rootCode, mapping.nasPath(), mapping.projectId(),
            blankToNull(mapping.projectCode()), true, DEFAULT_SCAN_EXTENSIONS,
            true, null, systemUserId);
        auditLogApplicationService.record(mapping.projectId(), MODULE_CODE, "asset.scan.patrol.create",
            "SCAN_TASK", String.valueOf(taskId), systemUserId,
            Map.of("pathMappingId", mapping.id(), "projectCode", valueOrDash(mapping.projectCode())));
        eventApplicationService.record("SCAN", mapping.projectId(), "SCAN_TASK", String.valueOf(taskId),
            "scan.patrol.create", systemUserId, "SYSTEM", "创建项目资产巡检任务: " + valueOrDash(mapping.projectCode()), null);

        scanTaskRepository.markRunning(taskId);
        ScanTaskResponse task = scanTaskRepository.requireById(taskId);
        auditLogApplicationService.record(mapping.projectId(), MODULE_CODE, "asset.scan.patrol.run",
            "SCAN_TASK", String.valueOf(taskId), systemUserId,
            Map.of("pathMappingId", mapping.id(), "projectCode", valueOrDash(mapping.projectCode())));
        eventApplicationService.record("SCAN", mapping.projectId(), "SCAN_TASK", String.valueOf(taskId),
            "scan.patrol.start", systemUserId, "SYSTEM", "开始项目资产巡检: " + valueOrDash(mapping.projectCode()), null);
        return executeScan(task, List.of(mapping), systemUserId, java.util.Set.of(mapping.projectId()));
    }

    private ScanTaskResponse executeScan(ScanTaskResponse task, List<PathMappingResponse> mappings,
                                           Long userId, java.util.Set<Long> allowedIds) {
        ScanCounters counters = new ScanCounters();
        StringBuilder failureMsgs = new StringBuilder();
        try {
            String scanDir = task.rootPath();
            java.io.File dir = new java.io.File(scanDir);
            if (!dir.exists() || !dir.isDirectory()) {
                counters.failedCount++;
                failureMsgs.append("扫描目录不存在或不合法: ").append(scanDir);
            } else {
                updateScanProgress(task.id(), counters, "扫描目录: " + dir.getName());
                processScanDirectory(task, dir, mappings, userId, allowedIds, counters, failureMsgs,
                    parseSkipDirectoryKeywords(task), Boolean.TRUE.equals(task.recursive()));
            }
        } catch (Exception e) {
            counters.failedCount = Math.max(1, counters.failedCount);
            failureMsgs.append("扫描异常: ").append(e.getMessage());
        }
        String reportJson = buildScanReportJson(task, counters);
        if (counters.canceled) {
            scanTaskRepository.markCanceled(task.id(), counters.totalScanned, counters.autoIngested,
                counters.pendingReview, counters.failedCount, counters.skippedLowValue,
                counters.skippedDirectories, counters.lastScannedPath, reportJson);
            auditLogApplicationService.record(task.projectId(), MODULE_CODE, "asset.scan.cancel",
                "SCAN_TASK", String.valueOf(task.id()), userId,
                Map.of("totalScanned", counters.totalScanned));
            eventApplicationService.record("SCAN", task.projectId(), "SCAN_TASK", String.valueOf(task.id()),
                "scan.cancel", userId, "SYSTEM", "扫描取消: " + task.rootCode(), null);
            return scanTaskRepository.requireById(task.id());
        }
        scanTaskRepository.markCompleted(task.id(), counters.totalScanned, counters.autoIngested,
            counters.pendingReview, counters.failedCount, counters.skippedLowValue,
            counters.skippedDirectories, counters.lastScannedPath,
            failureMsgs.length() > 0 ? truncate(failureMsgs.toString(), 1000) : null, reportJson);
        if (counters.failedCount > 0 || counters.skippedLowValue > 0 || counters.skippedDirectories > 0) {
            auditLogApplicationService.record(task.projectId(), MODULE_CODE, "asset.scan.failed",
                "SCAN_TASK", String.valueOf(task.id()), userId,
                Map.of("failedCount", counters.failedCount,
                    "ignoredLowValue", counters.skippedLowValue,
                    "skippedDirectories", counters.skippedDirectories,
                    "reason", truncate(failureMsgs.toString(), 500)));
        }
        String scanResultSummary = String.format("扫描完成: 共%d 自动入库%d 待审核%d 失败%d",
            counters.totalScanned, counters.autoIngested, counters.pendingReview, counters.failedCount);
        eventApplicationService.record("SCAN", task.projectId(), "SCAN_TASK", String.valueOf(task.id()),
            counters.failedCount > 0 ? "scan.fail" : "scan.success", userId, "SYSTEM", scanResultSummary, null);
        return scanTaskRepository.requireById(task.id());
    }

    public ScanTaskResponse cancelScan(Long userId, Long taskId) {
        ScanTaskResponse task = getScanForUser(userId, taskId);
        if (!List.of("PENDING", "RUNNING").contains(task.status())) {
            throw new BusinessException("ASSET_SCAN_NOT_CANCELABLE", "只有待执行或运行中的扫描任务可以取消", HttpStatus.BAD_REQUEST);
        }
        scanTaskRepository.requestCancel(taskId);
        auditLogApplicationService.record(task.projectId(), MODULE_CODE, "asset.scan.cancelRequest",
            "SCAN_TASK", String.valueOf(taskId), userId, Map.of());
        eventApplicationService.record("SCAN", task.projectId(), "SCAN_TASK", String.valueOf(taskId),
            "scan.cancelRequest", userId, "API", "请求取消扫描: " + task.rootCode(), null);
        return scanTaskRepository.requireById(taskId);
    }

    public ScanTaskResponse resumeScan(Long userId, Long taskId) {
        ScanTaskResponse task = getScanForUser(userId, taskId);
        if (!List.of("CANCELED", "FAILED").contains(task.status())) {
            throw new BusinessException("ASSET_SCAN_NOT_RESUMABLE", "只有已取消或失败的扫描任务可以续扫", HttpStatus.BAD_REQUEST);
        }
        auditLogApplicationService.record(task.projectId(), MODULE_CODE, "asset.scan.resume",
            "SCAN_TASK", String.valueOf(taskId), userId, Map.of());
        eventApplicationService.record("SCAN", task.projectId(), "SCAN_TASK", String.valueOf(taskId),
            "scan.resume", userId, "API", "续扫任务: " + task.rootCode(), null);
        return runScan(userId, taskId);
    }

    public ScanReportResponse getScanReport(Long userId, Long taskId) {
        ScanTaskResponse task = getScanForUser(userId, taskId);
        return new ScanReportResponse(
            task.id(), task.rootCode(), task.rootPath(), task.projectId(), task.projectCode(),
            task.status(), task.progressCurrent(), task.progressTotal(), task.progressPercent(),
            task.progressMessage(), task.totalScanned(), task.autoIngested(), task.pendingReview(),
            task.failedCount(), task.skippedLowValue(), task.skippedDirectories(),
            task.lastScannedPath(), task.failureReason(), task.scanReportJson(),
            task.startedAt(), task.completedAt()
        );
    }

    // ===== review =====

    public List<ScanCandidateResponse> listReviewCandidates(Long userId, String reviewStatus) {
        List<ScanCandidateResponse> all = scanCandidateRepository.listPendingReview(
            reviewStatus != null ? reviewStatus : "PENDING", 200);
        return all.stream()
            .filter(c -> canAccessCandidate(userId, c))
            .toList();
    }

    @Transactional
    public ScanCandidateResponse updateReviewCandidate(Long userId, Long candidateId,
                                                        ReviewUpdateRequest request) {
        ScanCandidateResponse candidate = scanCandidateRepository.requireById(candidateId);
        if (!"PENDING".equals(candidate.reviewStatus())) {
            throw new BusinessException("ASSET_CANDIDATE_NOT_PENDING", "只能修改待审核的候选记录", HttpStatus.BAD_REQUEST);
        }
        requireCandidateAccess(userId, candidate);
        if (request.matchedProjectId() != null) {
            requireProjectAccess(userId, request.matchedProjectId());
        }
        scanCandidateRepository.updateReview(candidateId,
            request.matchedProjectId(), request.matchedProjectCode(),
            request.detectedFileKind(), request.detectedDiscipline(),
            request.detectedVersionNo(), request.reviewMessage(),
            "PENDING", userId, null);
        auditLogApplicationService.record(candidate.matchedProjectId(), MODULE_CODE,
            "asset.review.update", "SCAN_CANDIDATE", String.valueOf(candidateId), userId, Map.of());
        eventApplicationService.record("REVIEW", candidate.matchedProjectId(), "SCAN_CANDIDATE",
            String.valueOf(candidateId), "review.update", userId, "API",
            "更新待审核: " + candidate.fileName(), null);
        return scanCandidateRepository.requireById(candidateId);
    }

    @Transactional
    public ScanCandidateResponse approveCandidate(Long userId, Long candidateId) {
        ScanCandidateResponse candidate = scanCandidateRepository.requireById(candidateId);
        if (!"PENDING".equals(candidate.reviewStatus())) {
            throw new BusinessException("ASSET_CANDIDATE_NOT_PENDING", "只能审核待审核的候选记录", HttpStatus.BAD_REQUEST);
        }
        requireCandidateAccess(userId, candidate);
        if (candidate.matchedProjectId() == null) {
            throw new BusinessException("ASSET_CANDIDATE_NO_PROJECT", "候选记录未归属项目，无法入库", HttpStatus.BAD_REQUEST);
        }
        requireProjectAccess(userId, candidate.matchedProjectId());
        Long fileId = bimAssetRepository.insertFileAsset(
            candidate.matchedProjectId(), candidate.fileName(),
            candidate.detectedFileKind(), candidate.sizeBytes(),
            "nas://" + candidate.rawPath(), "NAS", candidate.rawPath(),
            null, candidate.detectedDiscipline(), candidate.detectedVersionNo(),
            "REVIEW", candidate.confidenceLevel(), userId);
        scanCandidateRepository.updateReview(candidateId,
            candidate.matchedProjectId(), candidate.matchedProjectCode(),
            candidate.detectedFileKind(), candidate.detectedDiscipline(),
            candidate.detectedVersionNo(), candidate.reviewMessage(),
            "APPROVED", userId, fileId);
        auditLogApplicationService.record(candidate.matchedProjectId(), MODULE_CODE,
            "asset.review.approve", "SCAN_CANDIDATE", String.valueOf(candidateId), userId,
            Map.of("fileId", fileId));
        eventApplicationService.record("REVIEW", candidate.matchedProjectId(), "SCAN_CANDIDATE",
            String.valueOf(candidateId), "review.approve", userId, "REVIEW",
            "审核通过: " + candidate.fileName(), null);
        eventApplicationService.record("FILE", candidate.matchedProjectId(), "FILE_RESOURCE",
            String.valueOf(fileId), "file.create", userId, "REVIEW",
            "审核入库: " + candidate.fileName(), null);
        return scanCandidateRepository.requireById(candidateId);
    }

    @Transactional
    public ScanCandidateResponse rejectCandidate(Long userId, Long candidateId, String reviewMessage) {
        ScanCandidateResponse candidate = scanCandidateRepository.requireById(candidateId);
        if (!"PENDING".equals(candidate.reviewStatus())) {
            throw new BusinessException("ASSET_CANDIDATE_NOT_PENDING", "只能审核待审核的候选记录", HttpStatus.BAD_REQUEST);
        }
        requireCandidateAccess(userId, candidate);
        scanCandidateRepository.updateReview(candidateId, null, null, null, null, null,
            reviewMessage, "REJECTED", userId, null);
        auditLogApplicationService.record(candidate.matchedProjectId(), MODULE_CODE,
            "asset.review.reject", "SCAN_CANDIDATE", String.valueOf(candidateId), userId,
            Map.of("reason", reviewMessage));
        eventApplicationService.record("REVIEW", candidate.matchedProjectId(), "SCAN_CANDIDATE",
            String.valueOf(candidateId), "review.reject", userId, "REVIEW",
            "审核驳回: " + candidate.fileName() + " - " + (reviewMessage != null ? reviewMessage : ""), null);
        return scanCandidateRepository.requireById(candidateId);
    }

    @Transactional
    public void bulkApproveCandidates(Long userId, List<Long> candidateIds) {
        List<String> failures = new java.util.ArrayList<>();
        for (Long id : candidateIds) {
            try {
                approveCandidate(userId, id);
            } catch (BusinessException e) {
                failures.add("candidate " + id + ": " + e.getCode() + " - " + e.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            throw new BusinessException("ASSET_BULK_APPROVE_PARTIAL", "部分审批失败: " + String.join("; ", failures), HttpStatus.BAD_REQUEST);
        }
    }

    // ===== path mappings (permission-bounded) =====

    public List<PathMappingResponse> listPathMappingsForUser(Long userId, Long projectId, Boolean enabled) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
            return pathMappingRepository.list(projectId, enabled);
        }
        // null projectId — only return mappings for user's accessible projects
        java.util.Set<Long> allowedIds = accessibleProjectIds(userId);
        List<PathMappingResponse> merged = new java.util.ArrayList<>();
        for (Long pid : allowedIds) {
            merged.addAll(pathMappingRepository.list(pid, enabled));
        }
        return merged;
    }

    public List<ScanTaskResponse> listScansForUser(Long userId) {
        java.util.Set<Long> allowedIds = accessibleProjectIds(userId);
        List<ScanTaskResponse> all = scanTaskRepository.listLatest(50);
        return all.stream()
            .filter(t -> canAccessScan(userId, allowedIds, t))
            .toList();
    }

    public ScanTaskResponse getScanForUser(Long userId, Long taskId) {
        ScanTaskResponse task = scanTaskRepository.requireById(taskId);
        if (!canAccessScan(userId, accessibleProjectIds(userId), task)) {
            throw new BusinessException("ASSET_SCAN_TASK_ACCESS_DENIED", "无权访问该扫描任务", HttpStatus.FORBIDDEN);
        }
        return task;
    }

    // ===== file asset queries =====

    public List<FileAssetResponse> listFileAssets(Long userId, Long projectId, String fileKind,
                                                   String discipline, String fileName, String fileExt,
                                                   String sourceType, String keyword, String qualityIssue) {
        return bimAssetRepository.listFiles(userId, projectId, fileKind, discipline,
            fileName, fileExt, sourceType, keyword, null, qualityIssue, 0, 200);
    }

    public List<FileAssetResponse> listFileAssetsForDisplay(Long userId, Long projectId, String fileKind,
                                                             String discipline, String fileName, String fileExt,
                                                             String sourceType, String keyword, String qualityIssue) {
        return listFileAssets(userId, projectId, fileKind, discipline, fileName, fileExt, sourceType, keyword, qualityIssue)
            .stream()
            .map(file -> maskStoragePathIfNeeded(userId, file))
            .toList();
    }

    public PageResponse<FileAssetResponse> listFileAssetsPage(Long userId, Long projectId, String fileKind,
                                                              String discipline, String fileName, String fileExt,
                                                              String sourceType, String keyword, String assetSource,
                                                              String qualityIssue, Integer pageNo, Integer pageSize) {
        int safePageNo = pageNo == null ? 1 : Math.max(1, pageNo);
        int safePageSize = pageSize == null ? 50 : Math.max(1, Math.min(pageSize, 500));
        int offset = (safePageNo - 1) * safePageSize;
        long total = bimAssetRepository.countFiles(userId, projectId, fileKind, discipline,
            fileName, fileExt, sourceType, keyword, assetSource, qualityIssue);
        List<FileAssetResponse> items = bimAssetRepository.listFiles(userId, projectId, fileKind,
            discipline, fileName, fileExt, sourceType, keyword, assetSource, qualityIssue, offset, safePageSize);
        return new PageResponse<>(items, safePageNo, safePageSize, total);
    }

    public PageResponse<FileAssetResponse> listFileAssetsPageForDisplay(Long userId, Long projectId, String fileKind,
                                                                         String discipline, String fileName, String fileExt,
                                                                         String sourceType, String keyword, String assetSource,
                                                                         String qualityIssue, Integer pageNo, Integer pageSize) {
        PageResponse<FileAssetResponse> page = listFileAssetsPage(userId, projectId, fileKind, discipline,
            fileName, fileExt, sourceType, keyword, assetSource, qualityIssue, pageNo, pageSize);
        return new PageResponse<>(
            page.items().stream().map(file -> maskStoragePathIfNeeded(userId, file)).toList(),
            page.pageNo(),
            page.pageSize(),
            page.total()
        );
    }

    public FileAssetResponse getFileById(Long userId, Long fileId) {
        List<FileAssetResponse> files = bimAssetRepository.listFileById(userId, fileId);
        if (files.isEmpty()) {
            throw new BusinessException("ASSET_FILE_NOT_FOUND", "文件不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        return files.getFirst();
    }

    public FileAssetResponse getFileByIdForDisplay(Long userId, Long fileId) {
        return maskStoragePathIfNeeded(userId, getFileById(userId, fileId));
    }

    public FilePreviewResponse getFilePreview(Long userId, Long fileId) {
        FileAssetResponse file = getFileById(userId, fileId);
        String ext = normalizePreviewExt(file.fileExt(), file.fileName());
        String lifecycleStatus = file.lifecycleStatus() == null ? "" : file.lifecycleStatus().toUpperCase();
        boolean previewPermission = hasFilePermission(userId, file.projectId(), PERMISSION_FILE_PREVIEW);
        boolean downloadPermission = hasFilePermission(userId, file.projectId(), PERMISSION_FILE_DOWNLOAD);
        PreviewDecision decision;
        if (file.storagePath() == null || file.storagePath().isBlank()) {
            decision = new PreviewDecision(
                "BLOCKED",
                "NONE",
                false,
                "NOT_STARTED",
                false,
                "文件缺少存储路径，先完成路径治理后才能接入预览。",
                List.of("FIX_METADATA"),
                false,
                "文件不可用",
                "文件缺少存储路径，先完成路径治理后才能接入预览。",
                "DANGER"
            );
        } else if (List.of("DELETED", "QUARANTINED").contains(lifecycleStatus)) {
            decision = new PreviewDecision(
                "BLOCKED",
                "NONE",
                false,
                "NOT_STARTED",
                false,
                "文件已停用或处于隔离状态，不能打开预览。",
                List.of("VIEW_AUDIT"),
                false,
                "文件不可用",
                "文件已停用或处于隔离状态，不能打开预览。",
                "DANGER"
            );
        } else {
            decision = decidePreview(ext, file.fileKind());
        }
        String accessPolicyMessage = accessPolicyMessage(previewPermission, downloadPermission, decision);
        return new FilePreviewResponse(
            file.fileId(),
            file.projectId(),
            file.projectCode(),
            file.projectName(),
            file.fileName(),
            ext,
            file.fileKind(),
            decision.previewStatus(),
            decision.previewMode(),
            decision.previewAvailable(),
            decision.conversionStatus(),
            decision.conversionRequired(),
            decision.message(),
            decision.supportedActions(),
            decision.downloadOnly(),
            decision.statusLabel(),
            decision.actionHint(),
            decision.riskLevel(),
            previewPermission,
            downloadPermission,
            accessPolicyMessage,
            "/data-steward/assets/" + file.projectId() + "?fileId=" + file.fileId() + "&preview=1",
            file.updatedAt()
        );
    }

    public List<PreviewArtifactResponse> listPreviewArtifacts(Long userId, Long fileId) {
        FileAssetResponse file = getFileById(userId, fileId);
        PreviewDecision decision = decidePreview(normalizePreviewExt(file.fileExt(), file.fileName()), file.fileKind());
        List<PreviewArtifactResponse> rows = jdbcTemplate.query("""
            SELECT pa.file_id,
                   pa.artifact_type,
                   pa.preview_status,
                   pa.storage_state,
                   COALESCE(d.generation_status, 'NOT_STARTED') AS generation_status,
                   COALESCE(pa.content_type, d.content_type) AS content_type,
                   COALESCE(pa.size_bytes, d.size_bytes) AS size_bytes,
                   COALESCE(pa.last_verified_at, d.last_verified_at) AS last_verified_at
            FROM data_preview_artifacts pa
            LEFT JOIN data_file_derivatives d ON d.id = pa.derivative_id AND d.deleted = 0
            WHERE pa.file_id = ?
              AND pa.deleted = 0
            ORDER BY pa.updated_at DESC, pa.id DESC
            """, (rs, rowNum) -> new PreviewArtifactResponse(
            file.fileId(),
            file.assetUuid(),
            file.projectId(),
            rs.getString("artifact_type"),
            rs.getString("preview_status"),
            Boolean.TRUE.equals(decision.conversionRequired()),
            rs.getString("generation_status"),
            rs.getString("storage_state"),
            rs.getString("content_type"),
            nullableLong(rs.getObject("size_bytes")),
            instant(rs.getTimestamp("last_verified_at")),
            previewArtifactMessage(rs.getString("artifact_type"), rs.getString("preview_status"),
                rs.getString("storage_state"), rs.getString("generation_status"), decision)
        ), file.fileId());
        if (!rows.isEmpty()) {
            return rows;
        }
        ActiveObjectVersion activeObject = findActiveObjectVersion(file.fileId());
        return List.of(initialPreviewArtifact(file, decision, activeObject));
    }

    @Transactional
    public PreviewArtifactResponse preparePreviewArtifact(Long userId, Long fileId) {
        FileAssetResponse file = getFileById(userId, fileId);
        validateFileLifecycle(file);
        PreviewDecision decision = decidePreview(normalizePreviewExt(file.fileExt(), file.fileName()), file.fileKind());
        ActiveObjectVersion activeObject = findActiveObjectVersion(file.fileId());
        if ("BROWSER_NATIVE_PREVIEW".equals(artifactType(decision))) {
            activeObject = ensureReadableNativePreviewObject(file, activeObject, userId);
        }
        PreparedPreviewArtifact prepared = preparePreviewArtifactRecord(file, decision, activeObject, userId);
        auditLogApplicationService.record(file.projectId(), MODULE_CODE, "asset.file.preview_artifact.prepare",
            "FILE_RESOURCE", String.valueOf(file.fileId()), userId,
            Map.of(
                "fileId", file.fileId(),
                "artifactType", prepared.artifactType(),
                "previewStatus", prepared.previewStatus(),
                "storageState", prepared.storageState(),
                "generationStatus", prepared.generationStatus()
            ));
        return new PreviewArtifactResponse(
            file.fileId(),
            file.assetUuid(),
            file.projectId(),
            prepared.artifactType(),
            prepared.previewStatus(),
            Boolean.TRUE.equals(decision.conversionRequired()),
            prepared.generationStatus(),
            prepared.storageState(),
            prepared.contentType(),
            prepared.sizeBytes(),
            prepared.lastVerifiedAt(),
            prepared.message()
        );
    }

    public AccessTicketResponse createFileAccessTicket(Long userId, Long fileId, AccessTicketCreateRequest request) {
        String action = normalizeAccessAction(request == null ? null : request.action());
        FileAssetResponse file = getFileById(userId, fileId);
        boolean previewable = isBrowserNativePreview(file);
        boolean downloadable = hasFilePermission(userId, file.projectId(), PERMISSION_FILE_DOWNLOAD);
        validateFileLifecycle(file);

        if ("PREVIEW".equals(action)) {
            if (!hasFilePermission(userId, file.projectId(), PERMISSION_FILE_PREVIEW)) {
                recordFileAccessAudit(file, "asset.file.access.denied", userId, "PREVIEW_FORBIDDEN");
                throw new BusinessException("ASSET_FILE_PREVIEW_FORBIDDEN", "当前账号没有预览该文件的权限", HttpStatus.FORBIDDEN);
            }
            if (!previewable) {
                recordFileAccessAudit(file, "asset.file.access.denied", userId, "PREVIEW_UNSUPPORTED");
                PreviewDecision decision = decidePreview(normalizePreviewExt(file.fileExt(), file.fileName()), file.fileKind());
                String message = decision.actionHint() != null && !decision.actionHint().isBlank()
                    ? decision.actionHint()
                    : "当前文件格式暂不支持直接预览";
                throw new BusinessException("ASSET_FILE_PREVIEW_UNSUPPORTED", message, HttpStatus.PRECONDITION_FAILED);
            }
        } else if (!downloadable) {
            recordFileAccessAudit(file, "asset.file.access.denied", userId, "DOWNLOAD_FORBIDDEN");
            throw new BusinessException("ASSET_FILE_DOWNLOAD_FORBIDDEN", "当前账号没有下载该文件的权限", HttpStatus.FORBIDDEN);
        }

        StorageService.ReadDecision readDecision;
        try {
            readDecision = storageService.ensureReadable(file);
        } catch (BusinessException exception) {
            recordFileAccessAudit(file, "asset.file.access.failed", userId, exception.getCode());
            throw exception;
        }

        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        String ticket = UUID.randomUUID().toString().replace("-", "");
        Long ticketId = fileAccessTicketRepository.insert(ticket, file.fileId(), file.projectId(), userId, action, expiresAt);
        recordFileAccessAudit(file,
            "PREVIEW".equals(action) ? "asset.file.preview.ticket.create" : "asset.file.download.ticket.create",
            userId, "ticket=" + ticketId, readDecision);
        return new AccessTicketResponse(
            ticketId,
            ticket,
            "/api/data-steward/assets/file-access/" + ticket,
            expiresAt,
            action,
            file.fileId(),
            file.fileName(),
            previewable,
            downloadable,
            readDecision.storageStatus(),
            readDecision.readSource(),
            readDecision.fallbackUsed(),
            readDecision.fallbackReason(),
            readDecision.storageHealth(),
            readDecision.objectReadable(),
            readDecision.userMessage(),
            "PREVIEW".equals(action) ? "预览票据已创建，5分钟内有效。" : "下载票据已创建，5分钟内有效。"
        );
    }

    public FileAccessResource openFileAccessTicket(String ticket) {
        String normalizedTicket = requireText(ticket, "ASSET_FILE_ACCESS_TICKET_REQUIRED", "访问票据不能为空");
        var row = fileAccessTicketRepository.findByTicket(normalizedTicket)
            .orElseThrow(() -> new BusinessException("ASSET_FILE_ACCESS_TICKET_NOT_FOUND", "访问票据不存在", HttpStatus.NOT_FOUND));
        if (!"ACTIVE".equalsIgnoreCase(row.status())) {
            throw new BusinessException("ASSET_FILE_ACCESS_TICKET_INVALID", "访问票据已失效", HttpStatus.FORBIDDEN);
        }
        if (row.expiresAt() == null || row.expiresAt().isBefore(Instant.now())) {
            fileAccessTicketRepository.markExpired(row.id());
            throw new BusinessException("ASSET_FILE_ACCESS_TICKET_EXPIRED", "访问票据已过期", HttpStatus.FORBIDDEN);
        }

        FileAssetResponse file = getFileById(row.userId(), row.fileId());
        validateFileLifecycle(file);
        if (!hasFilePermission(row.userId(), file.projectId(),
            "PREVIEW".equalsIgnoreCase(row.action()) ? PERMISSION_FILE_PREVIEW : PERMISSION_FILE_DOWNLOAD)) {
            recordFileAccessAudit(file, "asset.file.access.denied", row.userId(), row.action() + "_PERMISSION_REVOKED");
            throw new BusinessException("ASSET_FILE_ACCESS_FORBIDDEN", "当前票据对应账号已无文件访问权限", HttpStatus.FORBIDDEN);
        }
        if ("PREVIEW".equalsIgnoreCase(row.action()) && !isBrowserNativePreview(file)) {
            recordFileAccessAudit(file, "asset.file.access.denied", row.userId(), "PREVIEW_UNSUPPORTED");
            throw new BusinessException("ASSET_FILE_PREVIEW_UNSUPPORTED", "当前文件格式暂不支持直接预览", HttpStatus.PRECONDITION_FAILED);
        }

        StorageService.StoredResource storedResource;
        try {
            storedResource = storageService.openReadable(file);
        } catch (BusinessException exception) {
            recordFileAccessAudit(file, "asset.file.access.failed", row.userId(), exception.getCode());
            throw exception;
        }
        fileAccessTicketRepository.markUsed(row.id());
        String actionCode = "PREVIEW".equalsIgnoreCase(row.action()) ? "asset.file.preview.open" : "asset.file.download.open";
        recordFileAccessAudit(file, actionCode, row.userId(), "ticket=" + row.id(), storedResource);
        return new FileAccessResource(
            storedResource.resource(),
            storedResource.contentType(),
            "PREVIEW".equalsIgnoreCase(row.action()) ? "inline" : "attachment",
            file.fileName(),
            storedResource.contentLength(),
            storedResource.storageStatus(),
            storedResource.readSource(),
            storedResource.fallbackUsed(),
            storedResource.fallbackReason(),
            storedResource.storageHealth(),
            storedResource.objectReadable()
        );
    }

    public FileStorageStatusResponse getFileStorageStatus(Long userId, Long fileId) {
        FileAssetResponse file = getFileById(userId, fileId);
        return storageService.fileStorageStatus(file);
    }

    private PreviewArtifactResponse initialPreviewArtifact(
        FileAssetResponse file,
        PreviewDecision decision,
        ActiveObjectVersion activeObject
    ) {
        String artifactType = artifactType(decision);
        boolean browserNativeReady = "BROWSER_NATIVE_PREVIEW".equals(artifactType) && activeObject != null;
        String previewStatus = browserNativeReady ? "AVAILABLE" : initialPreviewStatus(decision);
        String storageState = browserNativeReady ? "OBJECT_STORED" : initialStorageState(decision);
        String generationStatus = "UNSUPPORTED".equals(decision.previewStatus()) ? "SKIPPED" : "NOT_STARTED";
        return new PreviewArtifactResponse(
            file.fileId(),
            file.assetUuid(),
            file.projectId(),
            artifactType,
            previewStatus,
            Boolean.TRUE.equals(decision.conversionRequired()),
            generationStatus,
            storageState,
            activeObject == null ? null : activeObject.contentType(),
            activeObject == null ? null : activeObject.sizeBytes(),
            activeObject == null ? null : activeObject.lastVerifiedAt(),
            previewArtifactMessage(artifactType, previewStatus, storageState, generationStatus, decision)
        );
    }

    private PreparedPreviewArtifact preparePreviewArtifactRecord(
        FileAssetResponse file,
        PreviewDecision decision,
        ActiveObjectVersion activeObject,
        Long userId
    ) {
        String artifactType = artifactType(decision);
        if ("BROWSER_NATIVE_PREVIEW".equals(artifactType) && activeObject != null) {
            Long derivativeId = upsertPreviewDerivative(file.fileId(), activeObject.storageObjectId(), artifactType,
                activeObject.provider(), activeObject.contentType(), activeObject.checksum(), activeObject.sizeBytes(),
                "OBJECT_STORED", "COMPLETED", activeObject.lastVerifiedAt(), userId);
            upsertPreviewArtifact(file.fileId(), derivativeId, activeObject.storageObjectId(), artifactType,
                activeObject.provider(), activeObject.contentType(), activeObject.checksum(), activeObject.sizeBytes(),
                "AVAILABLE", "OBJECT_STORED", activeObject.lastVerifiedAt(), userId);
            return new PreparedPreviewArtifact(artifactType, "AVAILABLE", "COMPLETED", "OBJECT_STORED",
                activeObject.contentType(), activeObject.sizeBytes(), activeObject.lastVerifiedAt(),
                previewArtifactMessage(artifactType, "AVAILABLE", "OBJECT_STORED", "COMPLETED", decision));
        }

        String previewStatus = initialPreviewStatus(decision);
        String storageState = initialStorageState(decision);
        String generationStatus = "UNSUPPORTED".equals(decision.previewStatus()) ? "SKIPPED" : "NOT_STARTED";
        Long derivativeId = upsertPreviewDerivative(file.fileId(), null, artifactType, null, null, null, null,
            storageState, generationStatus, null, userId);
        upsertPreviewArtifact(file.fileId(), derivativeId, null, artifactType, null, null, null, null,
            previewStatus, storageState, null, userId);
        return new PreparedPreviewArtifact(artifactType, previewStatus, generationStatus, storageState,
            null, null, null, previewArtifactMessage(artifactType, previewStatus, storageState, generationStatus, decision));
    }

    private ActiveObjectVersion ensureReadableNativePreviewObject(
        FileAssetResponse file,
        ActiveObjectVersion activeObject,
        Long userId
    ) {
        if (activeObject != null) {
            try {
                storageService.ensureReadable(file);
                return activeObject;
            } catch (BusinessException exception) {
                if (!"ASSET_FILE_NOT_READABLE".equals(exception.getCode())
                    && !"ASSET_OBJECT_NOT_READABLE".equals(exception.getCode())) {
                    throw exception;
                }
            }
        }
        StorageService.ObjectMirrorResult mirror = storageService.mirrorNasFileToObject(file, "MINIO");
        Long objectId = upsertStorageObject(mirror, userId);
        upsertFileObjectVersion(file, objectId, mirror, userId);
        updateFileChecksum(file.fileId(), mirror.checksum(), userId);
        auditLogApplicationService.record(file.projectId(), MODULE_CODE, "asset.file.preview_artifact.object_repaired",
            "FILE_RESOURCE", String.valueOf(file.fileId()), userId,
            Map.of("fileId", file.fileId(), "reason", activeObject == null ? "NO_ACTIVE_OBJECT" : "ACTIVE_OBJECT_NOT_READABLE"));
        ActiveObjectVersion repaired = findActiveObjectVersion(file.fileId());
        if (repaired == null) {
            throw new BusinessException("ASSET_PREVIEW_OBJECT_REPAIR_FAILED",
                "预览对象化准备失败，请检查受控文件状态", HttpStatus.PRECONDITION_FAILED);
        }
        return repaired;
    }

    private Long upsertStorageObject(StorageService.ObjectMirrorResult mirror, Long userId) {
        jdbcTemplate.update("""
            INSERT INTO data_storage_objects (
                provider, bucket, object_key, etag, checksum, content_type, size_bytes,
                source_provider, source_uri_digest, source_path_digest,
                storage_state, migration_status, last_verified_at, created_by, updated_by
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?,
                'OBJECT_STORED', 'COMPLETED', ?, ?, ?
            )
            ON DUPLICATE KEY UPDATE
                etag = VALUES(etag),
                checksum = VALUES(checksum),
                content_type = VALUES(content_type),
                size_bytes = VALUES(size_bytes),
                source_provider = VALUES(source_provider),
                source_uri_digest = VALUES(source_uri_digest),
                source_path_digest = VALUES(source_path_digest),
                storage_state = 'OBJECT_STORED',
                migration_status = 'COMPLETED',
                last_verified_at = VALUES(last_verified_at),
                updated_by = VALUES(updated_by),
                deleted = 0,
                delete_token = 0
            """,
            mirror.provider(), mirror.bucket(), mirror.objectKey(), mirror.etag(), mirror.checksum(),
            mirror.contentType(), mirror.sizeBytes(), mirror.sourceProvider(), mirror.sourceUriDigest(),
            mirror.sourcePathDigest(), timestamp(mirror.verifiedAt()), userId, userId);
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM data_storage_objects
            WHERE provider = ?
              AND bucket = ?
              AND object_key = ?
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), mirror.provider(), mirror.bucket(), mirror.objectKey());
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_PREVIEW_OBJECT_RECORD_FAILED",
                "预览对象存储记录写入失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return rows.getFirst();
    }

    private void upsertFileObjectVersion(
        FileAssetResponse file,
        Long objectId,
        StorageService.ObjectMirrorResult mirror,
        Long userId
    ) {
        Integer existing = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_object_versions
            WHERE file_id = ?
              AND storage_object_id = ?
              AND active = 1
              AND deleted = 0
            """, Integer.class, file.fileId(), objectId);
        if (existing != null && existing > 0) {
            return;
        }
        jdbcTemplate.update("""
            UPDATE data_file_object_versions
            SET active = 0,
                updated_by = ?
            WHERE file_id = ?
              AND active = 1
              AND deleted = 0
            """, userId, file.fileId());
        jdbcTemplate.update("""
            INSERT INTO data_file_object_versions (
                file_id, storage_object_id, version_no, active,
                storage_state, migration_status, checksum, content_type, size_bytes,
                last_verified_at, created_by, updated_by
            ) VALUES (
                ?, ?, ?, 1,
                'OBJECT_STORED', 'COMPLETED', ?, ?, ?,
                ?, ?, ?
            )
            """, file.fileId(), objectId, file.versionNo(), mirror.checksum(), mirror.contentType(),
            mirror.sizeBytes(), timestamp(mirror.verifiedAt()), userId, userId);
    }

    private void updateFileChecksum(Long fileId, String checksum, Long userId) {
        jdbcTemplate.update("""
            UPDATE data_file_resources
            SET checksum = COALESCE(checksum, ?),
                last_verified_at = CURRENT_TIMESTAMP,
                updated_by = ?
            WHERE id = ?
              AND deleted = 0
            """, checksum, userId, fileId);
    }

    private Long upsertPreviewDerivative(
        Long fileId,
        Long storageObjectId,
        String derivativeType,
        String provider,
        String contentType,
        String checksum,
        Long sizeBytes,
        String storageState,
        String generationStatus,
        Instant lastVerifiedAt,
        Long userId
    ) {
        jdbcTemplate.update("""
            INSERT INTO data_file_derivatives (
                file_id, storage_object_id, derivative_type, provider, content_type, checksum, size_bytes,
                storage_state, generation_status, last_verified_at, created_by, updated_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                storage_object_id = VALUES(storage_object_id),
                provider = VALUES(provider),
                content_type = VALUES(content_type),
                checksum = VALUES(checksum),
                size_bytes = VALUES(size_bytes),
                storage_state = VALUES(storage_state),
                generation_status = VALUES(generation_status),
                last_verified_at = VALUES(last_verified_at),
                updated_by = VALUES(updated_by),
                deleted = 0,
                delete_token = 0
            """, fileId, storageObjectId, derivativeType, provider, contentType, checksum, sizeBytes,
            storageState, generationStatus, timestamp(lastVerifiedAt), userId, userId);
        List<Long> rows = jdbcTemplate.query("""
            SELECT id
            FROM data_file_derivatives
            WHERE file_id = ?
              AND derivative_type = ?
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), fileId, derivativeType);
        if (rows.isEmpty()) {
            throw new BusinessException("ASSET_PREVIEW_DERIVATIVE_RECORD_FAILED",
                "预览衍生产物记录写入失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return rows.getFirst();
    }

    private void upsertPreviewArtifact(
        Long fileId,
        Long derivativeId,
        Long storageObjectId,
        String artifactType,
        String provider,
        String contentType,
        String checksum,
        Long sizeBytes,
        String previewStatus,
        String storageState,
        Instant lastVerifiedAt,
        Long userId
    ) {
        List<Long> existing = jdbcTemplate.query("""
            SELECT id
            FROM data_preview_artifacts
            WHERE file_id = ?
              AND artifact_type = ?
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """, (rs, rowNum) -> rs.getLong("id"), fileId, artifactType);
        if (existing.isEmpty()) {
            jdbcTemplate.update("""
                INSERT INTO data_preview_artifacts (
                    file_id, derivative_id, storage_object_id, artifact_type, provider, content_type,
                    checksum, size_bytes, preview_status, storage_state, last_verified_at, created_by, updated_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fileId, derivativeId, storageObjectId, artifactType, provider, contentType,
                checksum, sizeBytes, previewStatus, storageState, timestamp(lastVerifiedAt), userId, userId);
            return;
        }
        jdbcTemplate.update("""
            UPDATE data_preview_artifacts
            SET derivative_id = ?,
                storage_object_id = ?,
                provider = ?,
                content_type = ?,
                checksum = ?,
                size_bytes = ?,
                preview_status = ?,
                storage_state = ?,
                last_verified_at = ?,
                updated_by = ?
            WHERE id = ?
            """, derivativeId, storageObjectId, provider, contentType, checksum, sizeBytes,
            previewStatus, storageState, timestamp(lastVerifiedAt), userId, existing.getFirst());
    }

    private ActiveObjectVersion findActiveObjectVersion(Long fileId) {
        List<ActiveObjectVersion> rows = jdbcTemplate.query("""
            SELECT so.id AS storage_object_id,
                   so.provider,
                   COALESCE(fov.content_type, so.content_type) AS content_type,
                   COALESCE(fov.checksum, so.checksum) AS checksum,
                   COALESCE(fov.size_bytes, so.size_bytes) AS size_bytes,
                   COALESCE(fov.last_verified_at, so.last_verified_at) AS last_verified_at
            FROM data_file_object_versions fov
            JOIN data_storage_objects so ON so.id = fov.storage_object_id AND so.deleted = 0
            WHERE fov.file_id = ?
              AND fov.active = 1
              AND fov.deleted = 0
              AND fov.storage_state = 'OBJECT_STORED'
            ORDER BY fov.id DESC
            LIMIT 1
            """, (rs, rowNum) -> new ActiveObjectVersion(
            rs.getLong("storage_object_id"),
            rs.getString("provider"),
            rs.getString("content_type"),
            rs.getString("checksum"),
            nullableLong(rs.getObject("size_bytes")),
            instant(rs.getTimestamp("last_verified_at"))
        ), fileId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String artifactType(PreviewDecision decision) {
        return switch (decision.previewMode()) {
            case "BROWSER_NATIVE" -> "BROWSER_NATIVE_PREVIEW";
            case "OFFICE_CONVERSION" -> "OFFICE_PREVIEW_PLACEHOLDER";
            case "CAD_CONVERSION" -> "CAD_PREVIEW_PLACEHOLDER";
            case "BIM_LIGHTWEIGHT" -> "BIM_LIGHTWEIGHT_PLACEHOLDER";
            case "DOWNLOAD_ONLY" -> "DOWNLOAD_ONLY_PLACEHOLDER";
            default -> "UNSUPPORTED_PREVIEW_PLACEHOLDER";
        };
    }

    private String initialPreviewStatus(PreviewDecision decision) {
        if ("AVAILABLE".equals(decision.previewStatus())) {
            return "NOT_STARTED";
        }
        return decision.previewStatus();
    }

    private String initialStorageState(PreviewDecision decision) {
        if ("UNSUPPORTED".equals(decision.previewStatus())) {
            return "NOT_REQUIRED";
        }
        return "PENDING";
    }

    private String previewArtifactMessage(
        String artifactType,
        String previewStatus,
        String storageState,
        String generationStatus,
        PreviewDecision decision
    ) {
        if ("BROWSER_NATIVE_PREVIEW".equals(artifactType) && "AVAILABLE".equals(previewStatus)
            && "OBJECT_STORED".equals(storageState)) {
            return "浏览器原生预览产物已对象化；实际打开仍通过平台受控预览票据。";
        }
        if ("BROWSER_NATIVE_PREVIEW".equals(artifactType)) {
            return "源文件尚未完成对象存储镜像，预览产物保持待准备；平台不会读取文件正文。";
        }
        if (Boolean.TRUE.equals(decision.conversionRequired())) {
            return decision.actionHint() + " 当前仅登记转换占位，不读取文件正文，不生成转换产物。";
        }
        if ("UNSUPPORTED".equals(previewStatus) || "SKIPPED".equals(generationStatus)) {
            return "当前格式不支持在线预览，未生成对象化预览产物。";
        }
        return decision.message();
    }

    private static Long nullableLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    @Transactional
    public FileAssetResponse updateFileMetadata(Long userId, Long fileId, FileAssetMetadataUpdateRequest request) {
        FileAssetResponse existing = getFileById(userId, fileId);
        String fileKind = normalizeOptionalEnum(
            request == null ? null : request.fileKind(),
            FILE_KINDS,
            "ASSET_FILE_KIND_INVALID",
            "文件类型不合法"
        );
        String confidenceLevel = normalizeOptionalEnum(
            request == null ? null : request.confidenceLevel(),
            CONFIDENCE_LEVELS,
            "ASSET_FILE_CONFIDENCE_INVALID",
            "置信度只能是 HIGH、MEDIUM 或 LOW"
        );
        String reviewStatus = normalizeOptionalEnum(
            request == null ? null : request.reviewStatus(),
            FILE_REVIEW_STATUSES,
            "ASSET_FILE_REVIEW_STATUS_INVALID",
            "审核状态不合法"
        );
        String discipline = blankToNull(request == null ? null : request.discipline());
        if (discipline != null) {
            discipline = discipline.toUpperCase();
        }
        String versionNo = blankToNull(request == null ? null : request.versionNo());

        bimAssetRepository.updateFileMetadata(fileId, fileKind, discipline, versionNo,
            confidenceLevel, reviewStatus, userId);
        FileAssetResponse updated = getFileById(userId, fileId);
        auditLogApplicationService.record(updated.projectId(), MODULE_CODE, "asset.file.metadata.update",
            "FILE_RESOURCE", String.valueOf(fileId), userId,
            Map.of(
                "fileKind", valueOrDash(fileKind),
                "discipline", valueOrDash(discipline),
                "versionNo", valueOrDash(versionNo),
                "confidenceLevel", valueOrDash(confidenceLevel),
                "reviewStatus", valueOrDash(reviewStatus)
            ));
        eventApplicationService.record("FILE", updated.projectId(), "FILE_RESOURCE", String.valueOf(fileId),
            "file.metadata.update", userId, "API", "人工更新文件元数据: " + updated.fileName(), null);
        return updated;
    }

    // ===== permission helpers =====

    private java.util.Set<Long> accessibleProjectIds(Long userId) {
        return new java.util.HashSet<>(bimAssetRepository.listProjects(userId, null).stream()
            .map(AssetProjectResponse::projectId).toList());
    }

    private void requireProjectAccess(Long userId, Long projectId) {
        if (projectId == null) return;
        if (!canAccessProject(userId, projectId)) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }

    private boolean canAccessProject(Long userId, Long projectId) {
        if (projectId == null) return false;
        return accessibleProjectIds(userId).contains(projectId);
    }

    private String normalizeOptionalEnum(String value, List<String> allowed, String code, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private FileAssetResponse maskStoragePathIfNeeded(Long userId, FileAssetResponse file) {
        if (file == null || canViewStoragePath(userId, file.projectId())) {
            return file;
        }
        return new FileAssetResponse(
            file.fileId(),
            file.assetUuid(),
            file.projectId(),
            file.projectCode(),
            file.projectName(),
            file.fileName(),
            file.fileExt(),
            file.fileKind(),
            file.discipline(),
            file.versionNo(),
            file.sizeBytes(),
            file.checksum(),
            file.storageProvider(),
            null,
            file.logicalPath(),
            file.sourceType(),
            file.processStatus(),
            file.reviewStatus(),
            file.confidenceLevel(),
            file.createdAt(),
            file.updatedAt(),
            file.permissionTags(),
            file.projectScope(),
            file.confidentialityLevel(),
            file.lastSeenAt(),
            file.lifecycleStatus(),
            file.indexEligibility()
        );
    }

    private boolean canViewStoragePath(Long userId, Long projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = ?
              AND upr.project_id = ?
              AND upr.deleted = 0
              AND r.code = 'PROJECT_ADMIN'
            """, Integer.class, userId, projectId);
        return count != null && count > 0;
    }

    private boolean hasFilePermission(Long userId, Long projectId, String permissionCode) {
        if (userId == null || projectId == null || permissionCode == null) {
            return false;
        }
        return permissionRepository.findByUserAndProject(userId, projectId).stream()
            .anyMatch(permission -> permissionCode.equals(permission.code()));
    }

    private String accessPolicyMessage(boolean previewAllowed, boolean downloadAllowed, PreviewDecision decision) {
        if (!previewAllowed && !downloadAllowed) {
            return "当前账号只能查看文件元数据，不能预览或下载。";
        }
        if (previewAllowed && !downloadAllowed) {
            if (Boolean.TRUE.equals(decision.previewAvailable())) {
                return "当前账号可预览文件，但没有下载权限。";
            }
            return Boolean.TRUE.equals(decision.conversionRequired())
                ? "当前账号有预览权限；在线预览需要后续转换服务。"
                : "当前账号有预览权限；当前格式暂不支持在线预览。";
        }
        if (!previewAllowed) {
            return "当前账号可下载文件，但没有在线预览权限。";
        }
        return "当前账号具备预览和下载权限。";
    }

    private String normalizeAccessAction(String action) {
        String normalized = blankToNull(action);
        if (normalized == null) {
            throw new BusinessException("ASSET_FILE_ACCESS_ACTION_REQUIRED", "访问动作不能为空", HttpStatus.BAD_REQUEST);
        }
        normalized = normalized.toUpperCase();
        if (!List.of("PREVIEW", "DOWNLOAD").contains(normalized)) {
            throw new BusinessException("ASSET_FILE_ACCESS_ACTION_INVALID", "访问动作只能是 PREVIEW 或 DOWNLOAD", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private boolean isBrowserNativePreview(FileAssetResponse file) {
        return FilePreviewPolicy.isBrowserNative(normalizePreviewExt(file.fileExt(), file.fileName()));
    }

    private void validateFileLifecycle(FileAssetResponse file) {
        String lifecycleStatus = file.lifecycleStatus() == null ? "" : file.lifecycleStatus().toUpperCase();
        if (List.of("DELETED", "QUARANTINED").contains(lifecycleStatus)) {
            throw new BusinessException("ASSET_FILE_ACCESS_BLOCKED", "文件已停用或处于隔离状态，不能访问。", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private Path resolveReadablePath(FileAssetResponse file) {
        String storagePath = blankToNull(file.storagePath());
        if (storagePath == null) {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "文件缺少存储路径", HttpStatus.PRECONDITION_FAILED);
        }
        String rawPath;
        if (storagePath.startsWith("nas://")) {
            rawPath = storagePath.substring("nas://".length());
            if (!rawPath.startsWith("/")) {
                rawPath = "/" + rawPath;
            }
        } else if (storagePath.startsWith("minio://") || storagePath.startsWith("s3://") || storagePath.startsWith("oss://")) {
            throw new BusinessException("STORAGE_PROVIDER_UNSUPPORTED", "当前存储提供方尚未接入受控读取", HttpStatus.PRECONDITION_FAILED);
        } else if (storagePath.startsWith("/")) {
            rawPath = storagePath;
        } else {
            throw new BusinessException("ASSET_FILE_PATH_INVALID", "存储路径格式不受支持", HttpStatus.PRECONDITION_FAILED);
        }
        Path path = Paths.get(rawPath).normalize().toAbsolutePath();
        if (!Files.exists(path) || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE", "文件不存在或不可读取", HttpStatus.PRECONDITION_FAILED);
        }
        return path;
    }

    private String detectContentType(Path path, FileAssetResponse file) {
        try {
            String probed = Files.probeContentType(path);
            if (probed != null && !probed.isBlank()) {
                return probed;
            }
        } catch (IOException ignored) {
            // Fall back by extension below.
        }
        return switch (normalizePreviewExt(file.fileExt(), file.fileName())) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }

    private Long readableSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new BusinessException("ASSET_FILE_NOT_READABLE", "文件大小读取失败", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private void recordFileAccessAudit(FileAssetResponse file, String actionCode, Long userId, String reason) {
        auditLogApplicationService.record(file.projectId(), MODULE_CODE, actionCode,
            "FILE_RESOURCE", String.valueOf(file.fileId()), userId,
            Map.of(
                "fileName", valueOrDash(file.fileName()),
                "reason", valueOrDash(reason)
            ));
    }

    private void recordFileAccessAudit(
        FileAssetResponse file,
        String actionCode,
        Long userId,
        String reason,
        StorageService.ReadDecision decision
    ) {
        auditLogApplicationService.record(file.projectId(), MODULE_CODE, actionCode,
            "FILE_RESOURCE", String.valueOf(file.fileId()), userId,
            Map.of(
                "fileName", valueOrDash(file.fileName()),
                "reason", valueOrDash(reason),
                "storageStatus", valueOrDash(decision == null ? null : decision.storageStatus()),
                "readSource", valueOrDash(decision == null ? null : decision.readSource()),
                "fallbackUsed", Boolean.TRUE.equals(decision != null && Boolean.TRUE.equals(decision.fallbackUsed())),
                "fallbackReason", valueOrDash(decision == null ? null : decision.fallbackReason())
            ));
    }

    private void recordFileAccessAudit(
        FileAssetResponse file,
        String actionCode,
        Long userId,
        String reason,
        StorageService.StoredResource resource
    ) {
        auditLogApplicationService.record(file.projectId(), MODULE_CODE, actionCode,
            "FILE_RESOURCE", String.valueOf(file.fileId()), userId,
            Map.of(
                "fileName", valueOrDash(file.fileName()),
                "reason", valueOrDash(reason),
                "storageStatus", valueOrDash(resource == null ? null : resource.storageStatus()),
                "readSource", valueOrDash(resource == null ? null : resource.readSource()),
                "fallbackUsed", Boolean.TRUE.equals(resource != null && Boolean.TRUE.equals(resource.fallbackUsed())),
                "fallbackReason", valueOrDash(resource == null ? null : resource.fallbackReason())
            ));
    }

    private boolean canAccessScan(Long userId, java.util.Set<Long> allowedIds, ScanTaskResponse task) {
        if (task.projectId() != null) {
            return allowedIds.contains(task.projectId());
        }
        return task.createdBy() != null && task.createdBy().equals(userId);
    }

    private boolean canAccessCandidate(Long userId, ScanCandidateResponse candidate) {
        try {
            requireCandidateAccess(userId, candidate);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private void requireCandidateAccess(Long userId, ScanCandidateResponse candidate) {
        if (candidate.matchedProjectId() != null) {
            requireProjectAccess(userId, candidate.matchedProjectId());
            return;
        }
        ScanTaskResponse task = scanTaskRepository.requireById(candidate.scanTaskId());
        if (task.createdBy() == null || !task.createdBy().equals(userId)) {
            throw new BusinessException("ASSET_CANDIDATE_ACCESS_DENIED", "无权访问该待审核候选", HttpStatus.FORBIDDEN);
        }
    }

    private void processScanDirectory(ScanTaskResponse task, File dir, List<PathMappingResponse> mappings,
                                       Long userId, java.util.Set<Long> allowedIds, ScanCounters counters,
                                       StringBuilder failureMsgs, List<String> skipDirectoryKeywords,
                                       boolean recursive) {
        if (counters.canceled || scanTaskRepository.isCancelRequested(task.id())) {
            counters.canceled = true;
            return;
        }
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File entry : entries) {
            if (counters.canceled || scanTaskRepository.isCancelRequested(task.id())) {
                counters.canceled = true;
                return;
            }
            if (entry.isDirectory()) {
                if (!recursive) {
                    continue;
                }
                if (Boolean.TRUE.equals(task.skipLowValueDirectories())
                    && shouldSkipDirectory(entry.getName(), skipDirectoryKeywords)) {
                    counters.skippedDirectories++;
                    counters.lastScannedPath = entry.getAbsolutePath();
                    updateScanProgress(task.id(), counters, "跳过低价值目录: " + entry.getName());
                    continue;
                }
                processScanDirectory(task, entry, mappings, userId, allowedIds, counters,
                    failureMsgs, skipDirectoryKeywords, true);
            } else if (entry.isFile()) {
                processScanFile(task, entry, mappings, userId, allowedIds, counters, failureMsgs);
            }
        }
    }

    private void processScanFile(ScanTaskResponse task, File file, List<PathMappingResponse> mappings,
                                  Long userId, java.util.Set<Long> allowedIds, ScanCounters counters,
                                  StringBuilder failureMsgs) {
        counters.totalScanned++;
        counters.lastScannedPath = file.getAbsolutePath();
        String name = file.getName();
        String filePath = file.getAbsolutePath();
        long fileSize = file.length();

        if (isLowValueFile(name, fileSize)) {
            counters.skippedLowValue++;
            maybeUpdateScanProgress(task.id(), counters, "跳过低价值文件: " + name);
            return;
        }

        String ext = extensionOf(name);
        if (!isAllowedExtension(ext, task.extensions())) {
            maybeUpdateScanProgress(task.id(), counters, "扫描中: " + name);
            return;
        }

        if (scanCandidateRepository.existsByScanTaskAndRawPath(task.id(), filePath)) {
            counters.skippedExisting++;
            maybeUpdateScanProgress(task.id(), counters, "续扫跳过已处理文件: " + name);
            return;
        }

        try {
            String detectedKind = classifyFileKind(ext);
            String detectedVersion = detectVersionFromName(name);
            String detectedDiscipline = null;
            Long matchedProjectId = null;
            String matchedProjectCode = null;
            String confidence = "LOW";
            for (PathMappingResponse m : mappings) {
                if (filePath.startsWith(m.nasPath())) {
                    matchedProjectId = m.projectId();
                    matchedProjectCode = m.projectCode();
                    confidence = "HIGH";
                    break;
                }
            }
            if (confidence.equals("LOW")) {
                for (PathMappingResponse m : mappings) {
                    if (m.projectCode() != null && matchesProjectCodeInPath(filePath, m.projectCode())) {
                        matchedProjectId = m.projectId();
                        matchedProjectCode = m.projectCode();
                        confidence = "HIGH";
                        break;
                    }
                }
            }
            if ("HIGH".equals(confidence) && matchedProjectId != null && !allowedIds.contains(matchedProjectId)) {
                confidence = "LOW";
            }
            if ("HIGH".equals(confidence) && isTempDirectory(filePath)) {
                confidence = "LOW";
            }
            if ("LOW".equals(confidence) && matchedProjectId != null && isHighConfidenceDirectory(filePath)) {
                confidence = "HIGH";
            }
            String reviewStatus = "HIGH".equals(confidence) ? "AUTO_INGESTED" : "PENDING";
            Long candidateId = scanCandidateRepository.insert(
                task.id(), matchedProjectId, matchedProjectCode,
                filePath, name, ext, fileSize,
                file.lastModified(), detectedKind, detectedDiscipline,
                detectedVersion, confidence, reviewStatus, userId);
            if ("HIGH".equals(confidence)) {
                Long fileId = bimAssetRepository.insertFileAsset(
                    matchedProjectId, name, detectedKind, fileSize,
                    "nas://" + filePath, "NAS",
                    filePath,
                    null, detectedDiscipline, detectedVersion,
                    "NAS_SCAN", "HIGH", userId);
                scanCandidateRepository.updateReview(
                    candidateId, matchedProjectId, matchedProjectCode,
                    detectedKind, detectedDiscipline, detectedVersion,
                    "自动入库", "AUTO_INGESTED", userId, fileId);
                counters.autoIngested++;
                auditLogApplicationService.record(matchedProjectId, MODULE_CODE,
                    "asset.scan.autoIngest", "FILE_RESOURCE",
                    String.valueOf(fileId), userId,
                    Map.of("candidateId", candidateId, "rawPath", filePath));
                eventApplicationService.record("FILE", matchedProjectId, "FILE_RESOURCE",
                    String.valueOf(fileId), "file.create", userId, "SCAN",
                    "自动入库: " + name, null);
            } else {
                counters.pendingReview++;
            }
        } catch (Exception e) {
            counters.failedCount++;
            failureMsgs.append(file.getName()).append(": ").append(e.getMessage()).append("; ");
        }
        maybeUpdateScanProgress(task.id(), counters, "扫描中: " + name);
    }

    private void maybeUpdateScanProgress(Long taskId, ScanCounters counters, String message) {
        long now = System.currentTimeMillis();
        if (counters.totalScanned % 100 == 0 || now - counters.lastProgressAtMs > 2000) {
            updateScanProgress(taskId, counters, message);
            counters.lastProgressAtMs = now;
        }
    }

    private void updateScanProgress(Long taskId, ScanCounters counters, String message) {
        scanTaskRepository.updateProgress(taskId, counters.totalScanned, 0,
            counters.totalScanned, counters.autoIngested, counters.pendingReview,
            counters.failedCount, counters.skippedLowValue, counters.skippedDirectories,
            counters.lastScannedPath, message);
    }

    private List<String> parseSkipDirectoryKeywords(ScanTaskResponse task) {
        java.util.ArrayList<String> keywords = new java.util.ArrayList<>(DEFAULT_SKIP_DIRECTORY_KEYWORDS);
        if (task.skipDirectoryKeywords() != null && !task.skipDirectoryKeywords().isBlank()) {
            for (String kw : task.skipDirectoryKeywords().split(",")) {
                String trimmed = kw.trim();
                if (!trimmed.isEmpty()) {
                    keywords.add(trimmed);
                }
            }
        }
        return keywords;
    }

    private boolean shouldSkipDirectory(String dirName, List<String> keywords) {
        if (dirName == null || dirName.isBlank()) return false;
        String lower = dirName.toLowerCase();
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            String kw = keyword.toLowerCase();
            if (lower.equals(kw) || lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String buildScanReportJson(ScanTaskResponse task, ScanCounters counters) {
        return "{"
            + "\"scanTaskId\":" + task.id()
            + ",\"rootCode\":\"" + jsonEscape(task.rootCode()) + "\""
            + ",\"projectCode\":\"" + jsonEscape(task.projectCode()) + "\""
            + ",\"totalScanned\":" + counters.totalScanned
            + ",\"autoIngested\":" + counters.autoIngested
            + ",\"pendingReview\":" + counters.pendingReview
            + ",\"failedCount\":" + counters.failedCount
            + ",\"skippedLowValue\":" + counters.skippedLowValue
            + ",\"skippedDirectories\":" + counters.skippedDirectories
            + ",\"skippedExisting\":" + counters.skippedExisting
            + ",\"canceled\":" + counters.canceled
            + ",\"lastScannedPath\":\"" + jsonEscape(counters.lastScannedPath) + "\""
            + "}";
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ===== helpers =====

    private static final class ScanCounters {
        private int totalScanned;
        private int autoIngested;
        private int pendingReview;
        private int failedCount;
        private int skippedLowValue;
        private int skippedDirectories;
        private int skippedExisting;
        private boolean canceled;
        private String lastScannedPath;
        private long lastProgressAtMs;
    }

    static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? "." + fileName.substring(dot + 1).toLowerCase() : "";
    }

    private String normalizePreviewExt(String ext, String fileName) {
        String next = blankToNull(ext);
        if (next == null) {
            return fileName == null ? "" : extensionOf(fileName);
        }
        next = next.toLowerCase();
        return next.startsWith(".") ? next : "." + next;
    }

    private static PreviewDecision decidePreview(String ext, String fileKind) {
        return FilePreviewPolicy.decide(ext, fileKind);
    }

    public record FileAccessResource(
        Resource resource,
        String contentType,
        String dispositionType,
        String fileName,
        Long contentLength,
        String storageStatus,
        String readSource,
        Boolean fallbackUsed,
        String fallbackReason,
        String storageHealth,
        Boolean objectReadable
    ) {
    }

    private record ActiveObjectVersion(
        Long storageObjectId,
        String provider,
        String contentType,
        String checksum,
        Long sizeBytes,
        Instant lastVerifiedAt
    ) {
    }

    private record PreparedPreviewArtifact(
        String artifactType,
        String previewStatus,
        String generationStatus,
        String storageState,
        String contentType,
        Long sizeBytes,
        Instant lastVerifiedAt,
        String message
    ) {
    }

    static boolean isAllowedExtension(String ext, String extensions) {
        if (ext.isEmpty()) return false;
        if (extensions == null || extensions.isBlank()) {
            return List.of(".rvt", ".dwg", ".ifc", ".nwd", ".nwc", ".dxf", ".pdf",
                ".doc", ".docx", ".wps", ".xls", ".xlsx", ".ppt", ".pptx",
                ".glb", ".gltf", ".zip", ".rar").contains(ext);
        }
        for (String e : extensions.split(",")) {
            if (ext.equalsIgnoreCase(e.trim())) return true;
        }
        return false;
    }

    static String classifyFileKind(String ext) {
        return switch (ext) {
            case ".rvt", ".ifc", ".nwd", ".nwc" -> "MODEL";
            case ".dwg", ".dxf", ".pdf" -> "DRAWING";
            case ".doc", ".docx", ".wps" -> "DOCUMENT";
            case ".xls", ".xlsx" -> "SPREADSHEET";
            case ".ppt", ".pptx" -> "PRESENTATION";
            case ".glb", ".gltf" -> "MODEL_VIEWER";
            case ".zip", ".rar" -> "ARCHIVE";
            default -> "OTHER";
        };
    }

    static boolean isLowValueFile(String fileName, long sizeBytes) {
        if (sizeBytes == 0) return true;
        String name = fileName != null ? fileName : "";
        if (name.equals(".DS_Store") || name.equals("Thumbs.db") || name.equals("desktop.ini")) return true;
        if (name.startsWith("~$")) return true;
        return false;
    }

    static boolean isTempDirectory(String filePath) {
        if (filePath == null) return false;
        java.io.File current = new java.io.File(filePath);
        java.io.File dir = current.isDirectory() ? current : current.getParentFile();
        while (dir != null) {
            String dirPath = dir.getAbsolutePath();
            // Skip system temp root directories to avoid false positives
            // (e.g. /tmp, /private/tmp on macOS, /var/tmp on Linux)
            if (!isSystemTempRoot(dirPath)) {
                String dirName = dir.getName().toLowerCase();
                for (String kw : TEMP_KEYWORDS) {
                    if (dirName.equals(kw.toLowerCase())) return true;
                }
            }
            dir = dir.getParentFile();
        }
        return false;
    }

    private static boolean isSystemTempRoot(String path) {
        return "/tmp".equals(path) || "/private/tmp".equals(path) || "/var/tmp".equals(path);
    }

    static boolean matchesProjectCodeInPath(String filePath, String projectCode) {
        if (filePath == null || projectCode == null || projectCode.isEmpty()) return false;
        String code = projectCode;
        int codeLen = code.length();
        String[] segments = filePath.replace('\\', '/').split("/");
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            if (seg.equals(code)) return true;
            if (seg.length() > codeLen && seg.startsWith(code)) {
                char next = seg.charAt(codeLen);
                if (next == '-' || next == '_' || next == '.') return true;
            }
        }
        return false;
    }

    static boolean isHighConfidenceDirectory(String filePath) {
        if (filePath == null) return false;
        String lower = filePath.toLowerCase().replace('\\', '/');
        for (String kw : java.util.List.of("05_发布文件", "06_归档文件", "成果文件", "发布文件", "归档文件")) {
            if (lower.contains("/" + kw + "/") || lower.contains("/" + kw)
                || lower.endsWith("/" + kw)) {
                return true;
            }
        }
        return false;
    }

    static String detectVersionFromName(String fileName) {
        String base = fileName.replaceFirst("\\.[^.]+$", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "([Vv](\\d+(\\.\\d+)?))|(Rev[A-Za-z])|(R(\\d+))").matcher(base);
        if (m.find()) {
            return m.group().toUpperCase();
        }
        return "V1";
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void collectFiles(java.io.File dir, java.util.List<java.io.File> out, boolean recursive) {
        java.io.File[] entries = dir.listFiles();
        if (entries == null) return;
        for (java.io.File f : entries) {
            if (f.isFile()) {
                out.add(f);
            } else if (f.isDirectory() && recursive) {
                collectFiles(f, out, true);
            }
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String[] parseCsvLine(String line) {
        // simple CSV: split by comma, trim quotes
        java.util.List<String> parts = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts.toArray(new String[0]);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 3) + "...";
    }

    private ImportResultResponse buildImportResult(Long jobId, String sourceName, String status,
                                                     int totalCount, int successCount, int failureCount) {
        List<ImportRowError> errors = importRowRepository.findErrorsByJobId(jobId);
        return new ImportResultResponse(jobId, "PROJECT_IMPORT", sourceName, status,
            totalCount, successCount, failureCount, errors);
    }

    @Transactional
    public ImportResultResponse importPathMappingsFromCsv(Long userId, String sourceName, String csvText) {
        if (csvText == null || csvText.isBlank()) {
            throw new BusinessException("ASSET_IMPORT_CSV_EMPTY", "导入文本不能为空", HttpStatus.BAD_REQUEST);
        }
        String[] lines = csvText.strip().split("\\R");
        if (lines.length < 2) {
            throw new BusinessException("ASSET_IMPORT_CSV_NO_DATA", "CSV至少需要表头和一行数据", HttpStatus.BAD_REQUEST);
        }
        Long jobId = importJobRepository.insert("PATH_MAPPING_IMPORT", sourceName, "RUNNING", 0, 0, 0, null, userId);
        int totalCount = 0, successCount = 0, failureCount = 0;
        for (int i = 1; i < lines.length; i++) {
            totalCount++;
            String line = lines[i].strip();
            if (line.isEmpty()) continue;
            try {
                String[] parts = parseCsvLine(line);
                if (parts.length < 3) {
                    failureCount++;
                    importRowRepository.insert(jobId, i + 1, line, false, "PATH_MAPPING", null, "CSV_COLUMNS_INSUFFICIENT", "至少需要项目编码、项目名称和NAS路径三列");
                    continue;
                }
                String projectCode = parts[0].strip();
                String projectName = parts.length > 1 ? parts[1].strip() : "";
                String nasPath = parts.length > 2 ? parts[2].strip() : "";
                if (projectCode.isEmpty() || nasPath.isEmpty()) {
                    failureCount++;
                    importRowRepository.insert(jobId, i + 1, line, false, "PATH_MAPPING", null, "CSV_FIELD_EMPTY", "项目编码或NAS路径不能为空");
                    continue;
                }
                Long projectId = bimAssetRepository.upsertProject(projectCode, projectName,
                    null, null, null, null, null, userId);
                bimAssetRepository.grantProjectAdmin(userId, projectId, userId);
                requireProjectAccess(userId, projectId);
                Long mappingId = pathMappingRepository.insert(projectId, "NAS", nasPath, "PREFIX", 0, null, userId);
                importRowRepository.insert(jobId, i + 1, line, true, "PATH_MAPPING", mappingId, null, null);
                successCount++;
                auditLogApplicationService.record(projectId, MODULE_CODE, "asset.pathMapping.import",
                    "PATH_MAPPING", String.valueOf(mappingId), userId, Map.of("nasPath", nasPath));
            } catch (Exception e) {
                failureCount++;
                importRowRepository.insert(jobId, i + 1, line, false, "PATH_MAPPING", null, "IMPORT_ROW_ERROR", truncate(e.getMessage(), 500));
            }
        }
        String status = failureCount > 0 ? "PARTIAL" : "SUCCESS";
        jdbcTemplate.update("UPDATE data_asset_import_jobs SET status=?, total_count=?, success_count=?, failure_count=? WHERE id=?",
            status, totalCount, successCount, failureCount, jobId);
        List<ImportRowError> errors = importRowRepository.findErrorsByJobId(jobId);
        return new ImportResultResponse(jobId, "PATH_MAPPING_IMPORT", sourceName, status,
            totalCount, successCount, failureCount, errors);
    }

    private Optional<String> leadingProjectNo(String directoryName) {
        Matcher matcher = LEADING_PROJECT_NO.matcher(directoryName);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String deriveProjectCode(String directoryName, Map<String, Long> leadingCodeCounts) {
        Optional<String> leadingCode = leadingProjectNo(directoryName);
        if (leadingCode.isPresent() && leadingCodeCounts.getOrDefault(leadingCode.get(), 0L) == 1L) {
            return leadingCode.get();
        }
        String prefix = leadingCode.orElse("NAS");
        return truncate(prefix + "-" + stableDigest(directoryName).substring(0, 8), 64);
    }

    private String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (java.io.IOException e) {
            return file.getAbsolutePath();
        }
    }

    private String stableDigest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ===== NAS project discovery =====

    private static final java.util.Set<String> STANDARD_FOLDERS = java.util.Set.of(
        "00_工作进度", "01_文件收发", "02_项目资源", "03_过程文件",
        "04_共享文件", "05_发布文件", "06_归档文件", "07_浏览动画"
    );

    private static final java.util.Set<String> REFERENCE_KEYWORDS = java.util.Set.of(
        "投标", "参考", "样板", "标准", "专题", "综合", "资料"
    );

    private static final java.util.Set<String> TEMP_KEYWORDS = java.util.Set.of(
        "新建文件夹", "临时文件", "临时", "转换", "temp", "tmp"
    );

    @Transactional
    public NasProjectDiscoveryResponse discoverNasProjects(Long userId, NasProjectDiscoveryRequest request) {
        String rootPath = requireText(request.rootPath(), "ASSET_NAS_ROOT_REQUIRED", "NAS根路径不能为空");
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new BusinessException("ASSET_NAS_ROOT_INVALID", "NAS根路径不存在或不是目录", HttpStatus.BAD_REQUEST);
        }
        File[] entries = rootDir.listFiles(File::isDirectory);
        if (entries == null) {
            entries = new File[0];
        }
        List<File> directories = java.util.Arrays.stream(entries)
            .sorted(Comparator.comparing(File::getName))
            .toList();
        Map<String, Long> leadingCodeCounts = directories.stream()
            .map(dir -> leadingProjectNo(dir.getName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(java.util.stream.Collectors.groupingBy(code -> code, java.util.stream.Collectors.counting()));

        boolean dryRun = Boolean.TRUE.equals(request.dryRun());
        boolean createProjects = request.createMissingProjects() == null || Boolean.TRUE.equals(request.createMissingProjects());
        boolean createMappings = request.createPathMappings() == null || Boolean.TRUE.equals(request.createPathMappings());
        String providerCode = defaultString(request.providerCode(), "NAS");
        String industryType = defaultString(request.industryType(), "BUILDING_MEP");
        String projectStage = defaultString(request.projectStage(), "UNKNOWN");
        String assetSource = defaultString(request.assetSource(), "NAS_DISCOVERY");

        Long jobId = null;
        if (!dryRun) {
            jobId = importJobRepository.insert("NAS_PROJECT_DISCOVERY", rootDir.getName(), "RUNNING", 0, 0, 0, null, userId);
        }

        List<NasProjectDiscoveryRow> rows = new java.util.ArrayList<>();
        int createdProjects = 0;
        int updatedProjects = 0;
        int createdMappings = 0;
        int existingMappings = 0;
        int pendingCodeReview = 0;
        int rowNo = 1;

        for (File dir : directories) {
            String directoryName = dir.getName();
            String nasPath = dir.getAbsolutePath();
            String dirType = classifyDirectoryType(directoryName);
            List<String> standardFolders = detectStandardFolders(dir);
            Optional<String> leadingCode = leadingProjectNo(directoryName);
            long sameCodeCount = leadingCode.map(leadingCodeCounts::get).orElse(0L);

            String projectCode;
            String suggestedProjectCode = null;
            String status;
            boolean requiresManualReview = false;
            String reviewReason = null;
            String message;

            // Determine status and project code
            if ("TEMP".equals(dirType)) {
                projectCode = deriveProjectCode(directoryName, leadingCodeCounts);
                suggestedProjectCode = null;
                status = "IGNORED";
                requiresManualReview = false;
                reviewReason = "临时目录，不自动创建项目";
                message = "临时目录，已跳过";
            } else if ("REFERENCE".equals(dirType)) {
                projectCode = deriveProjectCode(directoryName, leadingCodeCounts);
                suggestedProjectCode = null;
                status = "REFERENCE";
                requiresManualReview = true;
                reviewReason = "参考/投标/样板/资料目录，需人工确认是否创建项目";
                message = "参考/投标/样板/资料目录";
            } else if ("UNKNOWN".equals(dirType)) {
                projectCode = deriveProjectCode(directoryName, leadingCodeCounts);
                suggestedProjectCode = null;
                status = "NEEDS_CODE_REVIEW";
                requiresManualReview = true;
                reviewReason = "目录名称不包含有效项目编号";
                message = "目录未包含前置编号，已生成临时代码，建议人工确认";
                pendingCodeReview++;
            } else if (leadingCode.isPresent() && sameCodeCount > 1) {
                // Duplicate leading code — conflict
                String code = leadingCode.get();
                projectCode = code + "-" + stableDigest(directoryName).substring(0, 6);
                suggestedProjectCode = code;
                status = "CONFLICT";
                requiresManualReview = true;
                reviewReason = "编号 " + code + " 存在 " + sameCodeCount + " 个目录，需人工分配唯一项目编码";
                message = "编号冲突: " + code + " 被 " + sameCodeCount + " 个目录使用";
                pendingCodeReview++;
            } else if (leadingCode.isPresent()) {
                projectCode = leadingCode.get();
                suggestedProjectCode = null;
                status = "READY";
                requiresManualReview = false;
                reviewReason = null;
                message = "目录编号已识别";
            } else {
                projectCode = deriveProjectCode(directoryName, leadingCodeCounts);
                suggestedProjectCode = null;
                status = "NEEDS_CODE_REVIEW";
                requiresManualReview = true;
                reviewReason = "目录名称不包含有效项目编号";
                message = "目录未包含前置编号，已生成临时代码，建议人工确认";
                pendingCodeReview++;
            }

            Long projectId = null;
            Long mappingId = null;
            boolean projectCreated = false;
            boolean mappingCreated = false;

            try {
                if (!dryRun && "READY".equals(status) && createProjects) {
                    Optional<Long> existingProjectId = bimAssetRepository.findProjectIdByCode(projectCode);
                    projectId = bimAssetRepository.upsertProject(projectCode, directoryName,
                        industryType, projectStage, null, null, assetSource, userId);
                    bimAssetRepository.grantProjectAdmin(userId, projectId, userId);
                    projectCreated = existingProjectId.isEmpty();
                    if (projectCreated) {
                        createdProjects++;
                    } else {
                        updatedProjects++;
                    }
                    if (createMappings) {
                        PathMappingResponse existingMapping = pathMappingRepository.findByProjectAndNasPath(projectId, nasPath);
                        if (existingMapping == null) {
                            mappingId = pathMappingRepository.insert(projectId, providerCode, nasPath,
                                "PREFIX", 0, "NAS目录发现自动生成", userId);
                            mappingCreated = true;
                            createdMappings++;
                        } else {
                            mappingId = existingMapping.id();
                            existingMappings++;
                        }
                    }
                    auditLogApplicationService.record(projectId, MODULE_CODE, "asset.nasProject.discover",
                        "PROJECT", String.valueOf(projectId), userId,
                        Map.of("directoryName", directoryName, "nasPath", nasPath, "status", status));
                    eventApplicationService.record("PROJECT", projectId, "PROJECT", String.valueOf(projectId),
                        projectCreated ? "project.create" : "project.update", userId, "NAS_DISCOVERY",
                        "NAS目录发现" + (projectCreated ? "创建" : "更新") + "项目: " + projectCode, null);
                } else if (!dryRun) {
                    if (leadingCode.isPresent()) {
                        projectId = bimAssetRepository.findProjectIdByCode(projectCode).orElse(null);
                    }
                    // For CONFLICT, try to find by suggested code
                    if (projectId == null && suggestedProjectCode != null) {
                        projectId = bimAssetRepository.findProjectIdByCode(projectCode).orElse(null);
                    }
                } else {
                    if (leadingCode.isPresent() && "READY".equals(status)) {
                        projectId = bimAssetRepository.findProjectIdByCode(projectCode).orElse(null);
                    }
                }
                if (!dryRun && jobId != null) {
                    importRowRepository.insert(jobId, rowNo, directoryName + "," + nasPath,
                        true, "NAS_PROJECT", projectId, null, null);
                }
            } catch (Exception e) {
                status = "FAILED";
                message = truncate(e.getMessage(), 500);
                requiresManualReview = true;
                reviewReason = "处理异常: " + message;
                if (!dryRun && jobId != null) {
                    importRowRepository.insert(jobId, rowNo, directoryName + "," + nasPath,
                        false, "NAS_PROJECT", null, "NAS_DISCOVERY_ROW_ERROR", message);
                }
            }
            rows.add(new NasProjectDiscoveryRow(directoryName, nasPath, projectCode,
                suggestedProjectCode, directoryName, projectId, mappingId,
                projectCreated, mappingCreated, status, message, requiresManualReview,
                reviewReason, dirType, standardFolders));
            rowNo++;
        }

        if (!dryRun && jobId != null) {
            int failures = (int) rows.stream().filter(row -> "FAILED".equals(row.status())).count();
            String status = failures > 0 ? "PARTIAL" : "SUCCESS";
            jdbcTemplate.update("UPDATE data_asset_import_jobs SET status=?, total_count=?, success_count=?, failure_count=? WHERE id=?",
                status, rows.size(), rows.size() - failures, failures, jobId);
        }

        return new NasProjectDiscoveryResponse(rootDir.getAbsolutePath(), dryRun, rows.size(),
            createdProjects, updatedProjects, createdMappings, existingMappings,
            pendingCodeReview, rows);
    }

    public List<NonstandardDirectoryResponse> listNonstandardDirectories(
        Long userId,
        String governanceStatus,
        String riskType,
        String keyword,
        Integer limit
    ) {
        String normalizedStatus = normalizeOptional(governanceStatus, NONSTANDARD_STATUSES,
            "ASSET_NONSTANDARD_STATUS_INVALID", "治理状态不合法");
        String normalizedRisk = normalizeOptional(riskType, NONSTANDARD_RISK_TYPES,
            "ASSET_NONSTANDARD_RISK_INVALID", "风险类型不合法");
        return nonstandardDirectoryRepository.list(userId, normalizedStatus, normalizedRisk, keyword,
            limit != null ? limit : 200);
    }

    @Transactional
    public NonstandardDirectoryDiscoverResponse discoverNonstandardDirectories(
        Long userId,
        NonstandardDirectoryDiscoverRequest request
    ) {
        String rootPath = requireText(request.rootPath(), "ASSET_NAS_ROOT_REQUIRED", "NAS根路径不能为空");
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new BusinessException("ASSET_NAS_ROOT_INVALID", "NAS根路径不存在或不是目录", HttpStatus.BAD_REQUEST);
        }

        File[] entries = rootDir.listFiles(File::isDirectory);
        if (entries == null) {
            entries = new File[0];
        }
        List<File> directories = java.util.Arrays.stream(entries)
            .sorted(Comparator.comparing(File::getName))
            .toList();
        Map<String, Long> leadingCodeCounts = directories.stream()
            .map(dir -> leadingProjectNo(dir.getName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(java.util.stream.Collectors.groupingBy(code -> code, java.util.stream.Collectors.counting()));

        List<String> deferredCodes = request.deferredProjectCodes() == null || request.deferredProjectCodes().isEmpty()
            ? DEFAULT_DEFERRED_PROJECT_CODES
            : request.deferredProjectCodes().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        String providerCode = defaultString(request.providerCode(), "NAS");
        String canonicalRootPath = canonicalPath(rootDir);
        List<NonstandardDirectoryResponse> rows = new java.util.ArrayList<>();

        for (File dir : directories) {
            String directoryName = dir.getName();
            Optional<String> leadingCode = leadingProjectNo(directoryName);
            String dirType = classifyDirectoryType(directoryName);
            long sameCodeCount = leadingCode.map(leadingCodeCounts::get).orElse(0L);
            boolean userDeferred = leadingCode.isPresent() && deferredCodes.contains(leadingCode.get());
            String riskType = nonstandardRiskType(dirType, leadingCode.orElse(null), sameCodeCount, userDeferred);
            if (riskType == null) {
                continue;
            }

            String suggestedProjectCode = leadingCode.orElseGet(() -> deriveProjectCode(directoryName, leadingCodeCounts));
            String duplicateBaseCode = sameCodeCount > 1 ? leadingCode.orElse(null) : null;
            String governanceStatus = "TEMP".equals(riskType) ? "IGNORED" : "PENDING_AGENT";
            String reviewReason = nonstandardReviewReason(riskType, directoryName, duplicateBaseCode);
            List<String> standardFolders = detectStandardFolders(dir);
            Long id = nonstandardDirectoryRepository.upsert(
                providerCode,
                canonicalRootPath,
                directoryName,
                canonicalPath(dir),
                dirType,
                riskType,
                governanceStatus,
                suggestedProjectCode,
                directoryName,
                duplicateBaseCode,
                toJsonArray(standardFolders),
                reviewReason,
                blankToNull(request.ownerName()),
                userId
            );
            NonstandardDirectoryResponse saved = nonstandardDirectoryRepository.requireForUser(userId, id);
            rows.add(saved);
            auditLogApplicationService.record(null, MODULE_CODE, "asset.nonstandardDirectory.discover",
                "NONSTANDARD_DIRECTORY", String.valueOf(id), userId,
                Map.of("directoryName", directoryName, "nasPath", saved.nasPath(), "riskType", riskType));
            eventApplicationService.record("GOVERNANCE", null, "NONSTANDARD_DIRECTORY", String.valueOf(id),
                "nonstandard.discover", userId, "NAS_DISCOVERY",
                "发现非标准目录: " + directoryName, null);
        }
        return new NonstandardDirectoryDiscoverResponse(canonicalRootPath, rows.size(), rows.size(), rows);
    }

    @Transactional
    public NonstandardDirectoryResponse updateNonstandardDirectory(
        Long userId,
        Long id,
        NonstandardDirectoryUpdateRequest request
    ) {
        nonstandardDirectoryRepository.requireForUser(userId, id);
        String normalizedStatus = normalizeOptional(request.governanceStatus(), NONSTANDARD_STATUSES,
            "ASSET_NONSTANDARD_STATUS_INVALID", "治理状态不合法");
        String normalizedRisk = normalizeOptional(request.riskType(), NONSTANDARD_RISK_TYPES,
            "ASSET_NONSTANDARD_RISK_INVALID", "风险类型不合法");
        nonstandardDirectoryRepository.update(
            userId,
            id,
            normalizedStatus,
            normalizedRisk,
            request.suggestedProjectCode(),
            request.suggestedProjectName(),
            request.reviewReason(),
            request.agentSuggestion(),
            request.manualDecision(),
            request.decisionReason(),
            request.ownerName()
        );
        NonstandardDirectoryResponse updated = nonstandardDirectoryRepository.requireForUser(userId, id);
        auditLogApplicationService.record(null, MODULE_CODE, "asset.nonstandardDirectory.update",
            "NONSTANDARD_DIRECTORY", String.valueOf(id), userId,
            Map.of("governanceStatus", updated.governanceStatus(), "riskType", updated.riskType()));
        eventApplicationService.record("GOVERNANCE", null, "NONSTANDARD_DIRECTORY", String.valueOf(id),
            "nonstandard.update", userId, "API",
            "更新非标准目录治理状态: " + updated.directoryName(), null);
        return updated;
    }

    private String nonstandardRiskType(String dirType, String leadingCode, long sameCodeCount, boolean userDeferred) {
        if (sameCodeCount > 1) return "DUPLICATE_CODE";
        if (userDeferred) return "USER_DEFERRED";
        if ("REFERENCE".equals(dirType)) return "REFERENCE";
        if ("TEMP".equals(dirType)) return "TEMP";
        if ("UNKNOWN".equals(dirType)) return "UNKNOWN_CODE";
        return null;
    }

    private String nonstandardReviewReason(String riskType, String directoryName, String duplicateBaseCode) {
        return switch (riskType) {
            case "USER_DEFERRED" -> "用户裁决暂缓入库，等待 agent 辅助治理和人工确认";
            case "DUPLICATE_CODE" -> "编号 " + duplicateBaseCode + " 存在重复目录，需人工确认唯一项目编码";
            case "REFERENCE" -> "参考/投标/样板/资料目录，需确认是否属于正式交付项目";
            case "UNKNOWN_CODE" -> "目录名称不包含有效项目编号，需人工确认项目归属";
            case "TEMP" -> "临时目录或中间资料，不进入正式资产库";
            default -> "非标准目录需治理: " + directoryName;
        };
    }

    private String normalizeOptional(String value, List<String> allowed, String errorCode, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(errorCode, errorMessage, HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
            .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
            .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String classifyDirectoryType(String directoryName) {
        String lower = directoryName.toLowerCase().trim();

        // Temp directories
        for (String kw : TEMP_KEYWORDS) {
            if (lower.contains(kw)) return "TEMP";
        }

        // Reference/bidding/template directories
        for (String kw : REFERENCE_KEYWORDS) {
            if (lower.contains(kw)) return "REFERENCE";
        }

        // Check for leading project number
        if (leadingProjectNo(directoryName).isPresent()) {
            // It has a number prefix — check if the rest looks like a project name
            String afterNum = directoryName.replaceFirst("^\\d{2,4}[-_、\\s]*", "");
            if (afterNum.length() >= 1 && !afterNum.matches("^[\\d\\s\\-_、]+$")) {
                return "PROJECT";
            }
            return "UNKNOWN";
        }

        return "UNKNOWN";
    }

    private List<String> detectStandardFolders(File projectDir) {
        File[] children = projectDir.listFiles(File::isDirectory);
        if (children == null) return List.of();
        return java.util.Arrays.stream(children)
            .map(File::getName)
            .filter(STANDARD_FOLDERS::contains)
            .sorted()
            .toList();
    }
}

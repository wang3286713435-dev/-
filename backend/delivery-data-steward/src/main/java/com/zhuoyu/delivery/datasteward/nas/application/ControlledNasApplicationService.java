package com.zhuoyu.delivery.datasteward.nas.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryCreateRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryMoveRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryQuarantineRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryRenameRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileMoveRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileQuarantineRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileRenameRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasQuarantineRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.DirectoryRecord;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.FileRecord;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.QuarantineRecord;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ControlledNasApplicationService {

    private static final String MODULE_CODE = "data-steward";
    private static final Set<String> WRITE_ROLES = Set.of("DELIVERY_ENGINEER", "PROJECT_ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("PROJECT_ADMIN");
    private static final Set<String> FILE_KINDS = Set.of(
        "MODEL", "DRAWING", "DOCUMENT", "SPREADSHEET", "PRESENTATION", "ARCHIVE", "MODEL_VIEWER", "OTHER"
    );
    private static final String QUARANTINE_DIR = ".delivery-quarantine";

    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final AssetPathMappingRepository pathMappingRepository;
    private final ControlledNasRepository repository;
    private final AuditLogApplicationService auditLogApplicationService;

    public ControlledNasApplicationService(
        ProjectAccessApplicationService projectAccessApplicationService,
        AssetPathMappingRepository pathMappingRepository,
        ControlledNasRepository repository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.pathMappingRepository = pathMappingRepository;
        this.repository = repository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public NasOperationResponse createDirectory(Long userId, Long projectId, DirectoryCreateRequest request) {
        requireWriteRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        String parentPath = normalizeRelativePath(request == null ? null : request.parentPath(), true);
        String name = normalizeName(request == null ? null : request.name(), "NAS_DIRECTORY_NAME_REQUIRED", "文件夹名称不能为空");
        String targetRelativePath = joinRelative(parentPath, name);
        Path parent = resolveExistingDirectory(root, parentPath);
        Path target = resolveNewChild(root, parent, name);
        ensureTargetAvailable(target, "NAS_DIRECTORY_ALREADY_EXISTS", "同名文件夹已存在");
        try {
            Files.createDirectory(target);
            Long directoryId = repository.insertDirectory(projectId, targetRelativePath, name, parentPath, userId);
            return recordSuccess(userId, projectId, "DIRECTORY_CREATE", "DIRECTORY", directoryId, null,
                directoryId, null, null, target, name, targetRelativePath, "文件夹已创建");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_DIRECTORY_CREATE_FAILED", "文件夹创建失败，请确认目录可写后重试");
        }
    }

    @Transactional
    public NasOperationResponse uploadFile(
        Long userId,
        Long projectId,
        String parentPath,
        String fileKind,
        String discipline,
        String versionNo,
        MultipartFile file
    ) {
        requireWriteRole(userId, projectId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("NAS_UPLOAD_FILE_REQUIRED", "请选择要上传的文件", HttpStatus.BAD_REQUEST);
        }
        ProjectRoot root = resolveProjectRoot(projectId);
        String normalizedParentPath = normalizeRelativePath(parentPath, true);
        String fileName = normalizeUploadName(file.getOriginalFilename());
        Path parent = resolveExistingDirectory(root, normalizedParentPath);
        Path target = resolveNewChild(root, parent, fileName);
        ensureTargetAvailable(target, "NAS_UPLOAD_FILE_EXISTS", "同名文件已存在，当前批次不覆盖文件");
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target);
            String targetRelativePath = joinRelative(normalizedParentPath, fileName);
            Long fileId = repository.insertUploadedFile(
                projectId,
                fileName,
                normalizeFileKind(fileKind, fileName),
                file.getContentType(),
                Files.size(target),
                storageUri(target),
                logicalPath(target),
                blankToNull(discipline),
                defaultVersion(versionNo),
                userId
            );
            return recordSuccess(userId, projectId, "FILE_UPLOAD", "FILE", fileId, fileId,
                null, null, null, target, fileName, targetRelativePath, "文件已上传到公司 NAS");
        } catch (IOException exception) {
            safeDeleteIfExists(target);
            throw sanitizedIo("NAS_UPLOAD_FAILED", "文件上传失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse renameFile(Long userId, Long projectId, Long fileId, FileRenameRequest request) {
        requireWriteRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        Path source = resolveExistingFileFromRecord(root, file);
        String newName = normalizeName(request == null ? null : request.newName(), "NAS_FILE_NAME_REQUIRED", "文件名称不能为空");
        Path target = resolveNewChild(root, source.getParent(), newName);
        ensureTargetAvailable(target, "NAS_FILE_RENAME_EXISTS", "同名文件已存在，当前批次不覆盖文件");
        try {
            Files.move(source, target);
            repository.updateFilePathAndName(projectId, fileId, newName, storageUri(target), logicalPath(target), Files.size(target), userId);
            String targetRelativePath = root.relativePath(target);
            return recordSuccess(userId, projectId, "FILE_RENAME", "FILE", fileId, fileId, null, null,
                source, target, newName, targetRelativePath, "文件已重命名");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_FILE_RENAME_FAILED", "文件重命名失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse moveFile(Long userId, Long projectId, Long fileId, FileMoveRequest request) {
        requireWriteRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        Path source = resolveExistingFileFromRecord(root, file);
        String targetDirectory = normalizeRelativePath(request == null ? null : request.targetDirectory(), true);
        Path targetParent = resolveExistingDirectory(root, targetDirectory);
        Path target = resolveNewChild(root, targetParent, file.originalName());
        ensureTargetAvailable(target, "NAS_FILE_MOVE_EXISTS", "目标目录已有同名文件，当前批次不覆盖文件");
        try {
            Files.move(source, target);
            repository.updateFilePathAndName(projectId, fileId, file.originalName(), storageUri(target), logicalPath(target), Files.size(target), userId);
            String targetRelativePath = root.relativePath(target);
            return recordSuccess(userId, projectId, "FILE_MOVE", "FILE", fileId, fileId, null, null,
                source, target, file.originalName(), targetRelativePath, "文件已移动");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_FILE_MOVE_FAILED", "文件移动失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse quarantineFile(Long userId, Long projectId, Long fileId, FileQuarantineRequest request) {
        requireAdminRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        if ("QUARANTINED".equalsIgnoreCase(file.processStatus())) {
            throw new BusinessException("NAS_FILE_ALREADY_QUARANTINED", "文件已在隔离区", HttpStatus.BAD_REQUEST);
        }
        Path source = resolveExistingFileFromRecord(root, file);
        String originalRelativePath = root.relativePath(source);
        Path quarantineTarget = quarantineTarget(root, file.originalName());
        try {
            Files.createDirectories(quarantineTarget.getParent());
            Files.move(source, quarantineTarget);
            String quarantineRelativePath = root.relativePath(quarantineTarget);
            Long quarantineId = repository.insertQuarantineRecord(projectId, "FILE", fileId, null,
                originalRelativePath, quarantineRelativePath, file.originalName(),
                request == null ? null : request.reason(), Instant.now().plus(30, ChronoUnit.DAYS), userId);
            repository.markFileQuarantined(projectId, fileId, storageUri(quarantineTarget), logicalPath(quarantineTarget), userId);
            repository.markBindingsFileQuarantined(projectId, fileId, userId);
            return recordSuccess(userId, projectId, "FILE_QUARANTINE", "FILE", fileId, fileId,
                null, quarantineId, source, quarantineTarget, file.originalName(), originalRelativePath,
                "文件已移入隔离区，可在保留期内恢复");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_FILE_QUARANTINE_FAILED", "文件移入隔离区失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse renameDirectory(Long userId, Long projectId, DirectoryRenameRequest request) {
        requireWriteRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        String newName = normalizeName(request == null ? null : request.newName(), "NAS_DIRECTORY_NAME_REQUIRED", "文件夹名称不能为空");
        Path source = resolveExistingDirectory(root, sourceRelativePath);
        Path target = resolveNewChild(root, source.getParent(), newName);
        ensureTargetAvailable(target, "NAS_DIRECTORY_RENAME_EXISTS", "同名文件夹已存在");
        try {
            Files.move(source, target);
            String targetRelativePath = root.relativePath(target);
            updateDirectoryMetadata(projectId, sourceRelativePath, targetRelativePath, newName, userId);
            repository.updateFilePrefix(projectId, storageUri(source), storageUri(target),
                logicalPath(source), logicalPath(target), "PROCESSED", userId);
            return recordSuccess(userId, projectId, "DIRECTORY_RENAME", "DIRECTORY", null, null,
                repository.findDirectory(projectId, targetRelativePath).map(DirectoryRecord::id).orElse(null),
                null, source, target, newName, targetRelativePath, "文件夹已重命名");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_DIRECTORY_RENAME_FAILED", "文件夹重命名失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse moveDirectory(Long userId, Long projectId, DirectoryMoveRequest request) {
        requireWriteRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        String targetParentRelativePath = normalizeRelativePath(request == null ? null : request.targetDirectory(), true);
        String displayName = leafName(sourceRelativePath);
        Path source = resolveExistingDirectory(root, sourceRelativePath);
        Path targetParent = resolveExistingDirectory(root, targetParentRelativePath);
        String targetRelativePath = joinRelative(targetParentRelativePath, displayName);
        if (sameOrChild(targetRelativePath, sourceRelativePath)) {
            throw new BusinessException("NAS_DIRECTORY_MOVE_INTO_SELF", "不能把文件夹移动到自身或子文件夹内", HttpStatus.BAD_REQUEST);
        }
        Path target = resolveNewChild(root, targetParent, displayName);
        ensureTargetAvailable(target, "NAS_DIRECTORY_MOVE_EXISTS", "目标目录已有同名文件夹");
        try {
            Files.move(source, target);
            updateDirectoryMetadata(projectId, sourceRelativePath, targetRelativePath, displayName, userId);
            repository.updateFilePrefix(projectId, storageUri(source), storageUri(target),
                logicalPath(source), logicalPath(target), "PROCESSED", userId);
            return recordSuccess(userId, projectId, "DIRECTORY_MOVE", "DIRECTORY", null, null,
                repository.findDirectory(projectId, targetRelativePath).map(DirectoryRecord::id).orElse(null),
                null, source, target, displayName, targetRelativePath, "文件夹已移动");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_DIRECTORY_MOVE_FAILED", "文件夹移动失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse quarantineDirectory(Long userId, Long projectId, DirectoryQuarantineRequest request) {
        requireAdminRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        String displayName = leafName(sourceRelativePath);
        Path source = resolveExistingDirectory(root, sourceRelativePath);
        Path quarantineTarget = quarantineTarget(root, displayName);
        try {
            Files.createDirectories(quarantineTarget.getParent());
            Files.move(source, quarantineTarget);
            Long directoryId = repository.findDirectory(projectId, sourceRelativePath).map(DirectoryRecord::id).orElse(null);
            String quarantineRelativePath = root.relativePath(quarantineTarget);
            Long quarantineId = repository.insertQuarantineRecord(projectId, "DIRECTORY", null, directoryId,
                sourceRelativePath, quarantineRelativePath, displayName,
                request == null ? null : request.reason(), Instant.now().plus(30, ChronoUnit.DAYS), userId);
            repository.markDirectoryPrefixQuarantined(projectId, sourceRelativePath, quarantineId, userId);
            repository.updateFilePrefix(projectId, storageUri(source), storageUri(quarantineTarget),
                logicalPath(source), logicalPath(quarantineTarget), "QUARANTINED", userId);
            return recordSuccess(userId, projectId, "DIRECTORY_QUARANTINE", "DIRECTORY", directoryId, null,
                directoryId, quarantineId, source, quarantineTarget, displayName, sourceRelativePath,
                "文件夹已移入隔离区，可在保留期内恢复");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_DIRECTORY_QUARANTINE_FAILED", "文件夹移入隔离区失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse restoreQuarantine(Long userId, Long projectId, Long quarantineRecordId) {
        requireAdminRole(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        QuarantineRecord record = repository.findQuarantineRecord(projectId, quarantineRecordId)
            .orElseThrow(() -> new BusinessException("NAS_QUARANTINE_NOT_FOUND", "隔离记录不存在", HttpStatus.NOT_FOUND));
        if (!"QUARANTINED".equalsIgnoreCase(record.status())) {
            throw new BusinessException("NAS_QUARANTINE_STATUS_INVALID", "当前隔离记录不能恢复", HttpStatus.BAD_REQUEST);
        }
        Path source = resolveExistingPath(root, record.quarantineRelativePath());
        Path target = resolveTargetPath(root, record.originalRelativePath());
        if (Files.exists(target)) {
            throw new BusinessException("NAS_RESTORE_TARGET_EXISTS", "原位置已有同名文件或文件夹，请先人工处理冲突", HttpStatus.CONFLICT);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.move(source, target);
            if ("FILE".equalsIgnoreCase(record.targetType()) && record.fileId() != null) {
                repository.restoreFile(projectId, record.fileId(), storageUri(target), logicalPath(target), Files.size(target), userId);
                repository.restoreBindingsForFile(projectId, record.fileId(), userId);
            } else {
                repository.restoreDirectoryPrefix(projectId, record.id(), userId);
                repository.updateFilePrefix(projectId, storageUri(source), storageUri(target),
                    logicalPath(source), logicalPath(target), "PROCESSED", userId);
            }
            repository.markQuarantineRestored(projectId, quarantineRecordId, userId, null);
            return recordSuccess(userId, projectId, "QUARANTINE_RESTORE", record.targetType(),
                record.targetType().equalsIgnoreCase("FILE") ? record.fileId() : record.directoryId(),
                record.fileId(), record.directoryId(), quarantineRecordId, source, target,
                record.displayName(), record.originalRelativePath(), "隔离项已恢复到原位置");
        } catch (IOException exception) {
            repository.markQuarantineRestored(projectId, quarantineRecordId, userId, "恢复失败");
            throw sanitizedIo("NAS_RESTORE_FAILED", "隔离项恢复失败，请稍后重试");
        }
    }

    public List<NasOperationRecordResponse> listOperations(Long userId, Long projectId, Integer limit) {
        projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        return repository.listOperations(projectId, limit == null ? 50 : limit);
    }

    public List<NasQuarantineRecordResponse> listQuarantineRecords(Long userId, Long projectId, String status, Integer limit) {
        projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        return repository.listQuarantineRecords(projectId, blankToNull(status), limit == null ? 50 : limit);
    }

    private void updateDirectoryMetadata(
        Long projectId,
        String sourceRelativePath,
        String targetRelativePath,
        String displayName,
        Long userId
    ) {
        if (repository.findDirectory(projectId, sourceRelativePath).isPresent()) {
            repository.updateDirectoryPrefix(projectId, sourceRelativePath, targetRelativePath, displayName, userId);
        } else if (!repository.activeDirectoryExists(projectId, targetRelativePath)) {
            repository.insertDirectory(projectId, targetRelativePath, displayName, parentPath(targetRelativePath), userId);
        }
    }

    private NasOperationResponse recordSuccess(
        Long userId,
        Long projectId,
        String operationType,
        String targetType,
        Long targetId,
        Long fileId,
        Long directoryId,
        Long quarantineRecordId,
        Path source,
        Path target,
        String displayName,
        String displayPath,
        String message
    ) {
        String targetHash = target == null ? null : sha256(target.toString());
        String sourceHash = source == null ? null : sha256(source.toString());
        Long operationId = repository.insertOperation(projectId, operationType, targetType, targetId, fileId,
            directoryId, quarantineRecordId, sourceHash, targetHash,
            source == null ? null : safeDisplayPath(source),
            displayPath, "SUCCEEDED", message, null, TraceIdHolder.getTraceId(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, actionCode(operationType), "NAS_OPERATION",
            String.valueOf(operationId), userId, Map.of(
                "operationType", operationType,
                "targetType", targetType,
                "targetId", targetId == null ? "" : targetId,
                "displayPath", displayPath == null ? "" : displayPath,
                "quarantineRecordId", quarantineRecordId == null ? "" : quarantineRecordId
            ));
        return new NasOperationResponse(operationId, projectId, operationType, targetType, targetId, fileId,
            directoryId, quarantineRecordId, "SUCCEEDED", displayName, displayPath,
            pathHint(displayPath), message, TraceIdHolder.getTraceId(), Instant.now());
    }

    private String actionCode(String operationType) {
        return "nas." + operationType.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private AccessibleProject requireWriteRole(Long userId, Long projectId) {
        AccessibleProject project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        if (!WRITE_ROLES.contains(project.roleCode())) {
            throw new BusinessException("NAS_WRITE_FORBIDDEN", "当前项目角色只能查看，不能操作公司 NAS 文件", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private AccessibleProject requireAdminRole(Long userId, Long projectId) {
        AccessibleProject project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        if (!ADMIN_ROLES.contains(project.roleCode())) {
            throw new BusinessException("NAS_ADMIN_WRITE_FORBIDDEN", "删除到隔离区和恢复仅限项目管理员", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private ProjectRoot resolveProjectRoot(Long projectId) {
        List<PathMappingResponse> mappings = pathMappingRepository.list(projectId, true);
        if (mappings.isEmpty()) {
            throw new BusinessException("NAS_PROJECT_ROOT_MISSING", "当前项目尚未配置 NAS 根目录映射", HttpStatus.BAD_REQUEST);
        }
        String configured = stripNasScheme(mappings.getFirst().nasPath());
        try {
            Path root = Path.of(configured).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                throw new BusinessException("NAS_PROJECT_ROOT_UNAVAILABLE", "当前项目 NAS 根目录不可用", HttpStatus.BAD_REQUEST);
            }
            Path realRoot = root.toRealPath();
            return new ProjectRoot(realRoot);
        } catch (IOException exception) {
            throw new BusinessException("NAS_PROJECT_ROOT_UNAVAILABLE", "当前项目 NAS 根目录不可用", HttpStatus.BAD_REQUEST);
        }
    }

    private FileRecord requireFile(Long projectId, Long fileId) {
        return repository.findFile(projectId, fileId)
            .orElseThrow(() -> new BusinessException("NAS_FILE_NOT_FOUND", "文件不存在或不属于当前项目", HttpStatus.NOT_FOUND));
    }

    private Path resolveExistingFileFromRecord(ProjectRoot root, FileRecord file) {
        if ("QUARANTINED".equalsIgnoreCase(file.processStatus())) {
            throw new BusinessException("NAS_FILE_QUARANTINED", "文件已在隔离区，请先恢复后再操作", HttpStatus.BAD_REQUEST);
        }
        Path source = root.resolveStorageUri(file.storageUri());
        if (!Files.isRegularFile(source) || Files.isSymbolicLink(source)) {
            throw new BusinessException("NAS_FILE_UNAVAILABLE", "文件当前不可用或不是普通文件", HttpStatus.BAD_REQUEST);
        }
        return source;
    }

    private Path resolveExistingDirectory(ProjectRoot root, String relativePath) {
        Path path = root.resolveRelative(relativePath);
        if (!Files.isDirectory(path) || Files.isSymbolicLink(path)) {
            throw new BusinessException("NAS_DIRECTORY_NOT_FOUND", "目标文件夹不存在或不可用", HttpStatus.NOT_FOUND);
        }
        ensureParentInsideRoot(root, path);
        return path;
    }

    private Path resolveExistingPath(ProjectRoot root, String relativePath) {
        Path path = root.resolveRelative(relativePath);
        if (!Files.exists(path)) {
            throw new BusinessException("NAS_QUARANTINE_SOURCE_MISSING", "隔离区文件不存在", HttpStatus.BAD_REQUEST);
        }
        ensureParentInsideRoot(root, path);
        return path;
    }

    private Path resolveTargetPath(ProjectRoot root, String relativePath) {
        Path path = root.resolveRelative(relativePath);
        ensureParentInsideRoot(root, path);
        return path;
    }

    private Path resolveNewChild(ProjectRoot root, Path parent, String childName) {
        Path target = parent.resolve(childName).normalize();
        if (!target.startsWith(root.realRoot())) {
            throw new BusinessException("NAS_PATH_OUT_OF_SCOPE", "路径不在当前项目目录内", HttpStatus.BAD_REQUEST);
        }
        ensureParentInsideRoot(root, target);
        return target;
    }

    private void ensureParentInsideRoot(ProjectRoot root, Path target) {
        try {
            if (target.equals(root.realRoot())) {
                return;
            }
            Path parent = target.getParent();
            if (parent == null || !parent.toRealPath().startsWith(root.realRoot())) {
                throw new BusinessException("NAS_PATH_OUT_OF_SCOPE", "路径不在当前项目目录内", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException exception) {
            throw new BusinessException("NAS_PARENT_DIRECTORY_UNAVAILABLE", "目标上级文件夹不可用", HttpStatus.BAD_REQUEST);
        }
    }

    private void ensureTargetAvailable(Path target, String code, String message) {
        if (Files.exists(target)) {
            throw new BusinessException(code, message, HttpStatus.CONFLICT);
        }
        if (Files.isSymbolicLink(target)) {
            throw new BusinessException("NAS_TARGET_SYMLINK_FORBIDDEN", "目标路径不允许使用符号链接", HttpStatus.BAD_REQUEST);
        }
    }

    private Path quarantineTarget(ProjectRoot root, String displayName) {
        String safeName = displayName.replace('/', '_').replace('\\', '_');
        String unique = UUID.randomUUID().toString().replace("-", "");
        return root.realRoot().resolve(QUARANTINE_DIR).resolve(unique + "-" + safeName).normalize();
    }

    private String normalizeRelativePath(String value, boolean allowRoot) {
        if (value == null || value.isBlank()) {
            if (allowRoot) return "";
            throw new BusinessException("NAS_DIRECTORY_REQUIRED", "请选择文件夹", HttpStatus.BAD_REQUEST);
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.isBlank()) {
            if (allowRoot) return "";
            throw new BusinessException("NAS_DIRECTORY_REQUIRED", "请选择文件夹", HttpStatus.BAD_REQUEST);
        }
        if (normalized.startsWith("/") || normalized.startsWith("~") || normalized.contains(":")
            || normalized.contains("\u0000") || normalized.contains("//")) {
            throw new BusinessException("NAS_PATH_INVALID", "路径格式不合法", HttpStatus.BAD_REQUEST);
        }
        for (String segment : normalized.split("/")) {
            validateSegment(segment);
        }
        if (sameOrChild(normalized, QUARANTINE_DIR)) {
            throw new BusinessException("NAS_QUARANTINE_DIRECTORY_FORBIDDEN", "隔离区目录不能作为操作目标", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeName(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        String name = value.trim();
        if (name.contains("/") || name.contains("\\") || name.contains("\u0000") || name.contains(":")) {
            throw new BusinessException("NAS_NAME_INVALID", "名称不能包含路径分隔符或特殊符号", HttpStatus.BAD_REQUEST);
        }
        validateSegment(name);
        if (QUARANTINE_DIR.equals(name)) {
            throw new BusinessException("NAS_NAME_RESERVED", "该名称为平台隔离区保留名称", HttpStatus.BAD_REQUEST);
        }
        return name;
    }

    private void validateSegment(String segment) {
        if (segment == null || segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
            throw new BusinessException("NAS_PATH_INVALID", "路径格式不合法", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeUploadName(String originalName) {
        String normalized = originalName == null ? "" : originalName.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return normalizeName(name, "NAS_UPLOAD_FILE_NAME_REQUIRED", "上传文件名称不能为空");
    }

    private String normalizeFileKind(String fileKind, String fileName) {
        String normalized = fileKind == null || fileKind.isBlank() ? inferFileKind(fileName) : fileKind.trim().toUpperCase(Locale.ROOT);
        if (!FILE_KINDS.contains(normalized)) {
            throw new BusinessException("NAS_FILE_KIND_INVALID", "文件类型不在允许范围内", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String inferFileKind(String fileName) {
        String ext = extension(fileName);
        if (Set.of("rvt", "ifc", "nwd", "nwc").contains(ext)) return "MODEL";
        if (Set.of("dwg", "dxf", "pdf").contains(ext)) return "DRAWING";
        if (Set.of("doc", "docx", "wps").contains(ext)) return "DOCUMENT";
        if (Set.of("xls", "xlsx").contains(ext)) return "SPREADSHEET";
        if (Set.of("ppt", "pptx").contains(ext)) return "PRESENTATION";
        if (Set.of("zip", "rar", "7z").contains(ext)) return "ARCHIVE";
        if (Set.of("glb", "gltf").contains(ext)) return "MODEL_VIEWER";
        return "OTHER";
    }

    private String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private String defaultVersion(String versionNo) {
        return versionNo == null || versionNo.isBlank() ? "V1" : versionNo.trim();
    }

    private String parentPath(String relativePath) {
        int slash = relativePath == null ? -1 : relativePath.lastIndexOf('/');
        return slash <= 0 ? "" : relativePath.substring(0, slash);
    }

    private String leafName(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
    }

    private String joinRelative(String parent, String child) {
        String safeParent = parent == null ? "" : parent.trim();
        return safeParent.isBlank() ? child : safeParent + "/" + child;
    }

    private boolean sameOrChild(String path, String root) {
        return Objects.equals(path, root) || (path != null && root != null && path.startsWith(root + "/"));
    }

    private String stripNasScheme(String value) {
        String next = value == null ? "" : value.trim();
        if (next.startsWith("nas://")) {
            next = next.substring("nas://".length());
        }
        return next;
    }

    private String storageUri(Path path) {
        return "nas://" + path.toAbsolutePath().normalize();
    }

    private String logicalPath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private String safeDisplayPath(Path path) {
        return path == null ? "" : path.getFileName().toString();
    }

    private String pathHint(String displayPath) {
        if (displayPath == null || displayPath.isBlank()) {
            return "project_root";
        }
        return "project_relative_path";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return null;
        }
    }

    private void safeDeleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup for failed upload only.
        }
    }

    private BusinessException sanitizedIo(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ProjectRoot(Path realRoot) {
        Path resolveRelative(String relativePath) {
            Path path = relativePath == null || relativePath.isBlank()
                ? realRoot
                : realRoot.resolve(relativePath).normalize();
            if (!path.startsWith(realRoot)) {
                throw new BusinessException("NAS_PATH_OUT_OF_SCOPE", "路径不在当前项目目录内", HttpStatus.BAD_REQUEST);
            }
            return path;
        }

        Path resolveStorageUri(String storageUri) {
            String raw = storageUri == null ? "" : storageUri.trim();
            if (raw.startsWith("nas://")) {
                raw = raw.substring("nas://".length());
            }
            if (raw.isBlank()) {
                throw new BusinessException("NAS_FILE_STORAGE_MISSING", "文件存储位置缺失", HttpStatus.BAD_REQUEST);
            }
            Path path = Path.of(raw).toAbsolutePath().normalize();
            if (!path.startsWith(realRoot)) {
                throw new BusinessException("NAS_FILE_OUT_OF_PROJECT", "文件不在当前项目目录内", HttpStatus.FORBIDDEN);
            }
            return path;
        }

        String relativePath(Path path) {
            Path normalized = path.toAbsolutePath().normalize();
            if (!normalized.startsWith(realRoot)) {
                throw new BusinessException("NAS_PATH_OUT_OF_SCOPE", "路径不在当前项目目录内", HttpStatus.BAD_REQUEST);
            }
            String value = realRoot.relativize(normalized).toString().replace('\\', '/');
            return value.equals(".") ? "" : value;
        }
    }
}

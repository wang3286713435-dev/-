package com.zhuoyu.delivery.datasteward.nas.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasWriteTrialConfigRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasWriteTrialStatusResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasQuarantineRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.DirectoryRecord;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.FileRecord;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.QuarantineRecord;
import com.zhuoyu.delivery.datasteward.nas.repository.ControlledNasRepository.TrialConfigRecord;
import com.zhuoyu.delivery.datasteward.storage.StorageService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashSet;
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
    private static final List<String> DEFAULT_TRIAL_ROLES = List.of("DELIVERY_ENGINEER", "PROJECT_ADMIN");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };
    private static final Set<String> FILE_KINDS = Set.of(
        "MODEL", "DRAWING", "DOCUMENT", "SPREADSHEET", "PRESENTATION", "ARCHIVE", "MODEL_VIEWER", "OTHER"
    );
    private static final String QUARANTINE_DIR = ".delivery-quarantine";

    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final AssetPathMappingRepository pathMappingRepository;
    private final ControlledNasRepository repository;
    private final AuditLogApplicationService auditLogApplicationService;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public ControlledNasApplicationService(
        ProjectAccessApplicationService projectAccessApplicationService,
        AssetPathMappingRepository pathMappingRepository,
        ControlledNasRepository repository,
        AuditLogApplicationService auditLogApplicationService,
        ObjectMapper objectMapper,
        StorageService storageService
    ) {
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.pathMappingRepository = pathMappingRepository;
        this.repository = repository;
        this.auditLogApplicationService = auditLogApplicationService;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public NasWriteTrialStatusResponse getWriteTrialStatus(Long userId, Long projectId, String directoryPath) {
        AccessibleProject project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        String checkedDirectory = normalizeRelativePath(directoryPath, true);
        TrialConfig config = loadTrialConfig(projectId);
        return toTrialStatus(userId, project, config, checkedDirectory);
    }

    @Transactional
    public NasWriteTrialStatusResponse updateWriteTrialConfig(
        Long userId,
        Long projectId,
        NasWriteTrialConfigRequest request
    ) {
        requireAdminRoleWithoutTrial(userId, projectId);
        TrialConfig config = normalizeTrialConfig(projectId, request, userId);
        repository.upsertTrialConfig(
            projectId,
            config.enabled(),
            toJson(config.allowedRelativeRoots()),
            toJson(config.allowedRoleCodes()),
            toJson(config.allowedUserIds()),
            config.trialModeNotice(),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "nas.write.trial.update", "NAS_WRITE_TRIAL",
            String.valueOf(projectId), userId, Map.of(
                "enabled", config.enabled(),
                "allowedRelativeRoots", config.allowedRelativeRoots(),
                "allowedRoleCodes", config.allowedRoleCodes(),
                "allowedUserIds", config.allowedUserIds()
            ));
        AccessibleProject project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        return toTrialStatus(userId, project, loadTrialConfig(projectId), "");
    }

    @Transactional
    public NasOperationResponse createDirectory(Long userId, Long projectId, DirectoryCreateRequest request) {
        String parentPath = normalizeRelativePath(request == null ? null : request.parentPath(), true);
        String name = normalizeName(request == null ? null : request.name(), "NAS_DIRECTORY_NAME_REQUIRED", "文件夹名称不能为空");
        String targetRelativePath = joinRelative(parentPath, name);
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(targetRelativePath));
        ProjectRoot root = resolveProjectRoot(projectId);
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
        if (file == null || file.isEmpty()) {
            throw new BusinessException("NAS_UPLOAD_FILE_REQUIRED", "请选择要上传的文件", HttpStatus.BAD_REQUEST);
        }
        String normalizedParentPath = normalizeRelativePath(parentPath, true);
        String fileName = normalizeUploadName(file.getOriginalFilename());
        String targetRelativePath = joinRelative(normalizedParentPath, fileName);
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(targetRelativePath));
        ProjectRoot root = resolveProjectRoot(projectId);
        Path parent = resolveExistingDirectory(root, normalizedParentPath);
        Path target = resolveNewChild(root, parent, fileName);
        ensureTargetAvailable(target, "NAS_UPLOAD_FILE_EXISTS", "同名文件已存在，当前批次不覆盖文件");
        if (repository.activeLogicalFileExists(projectId, targetRelativePath)) {
            throw new BusinessException("NAS_UPLOAD_FILE_EXISTS", "同名文件已登记，当前批次不覆盖文件", HttpStatus.CONFLICT);
        }
        String assetUuid = UUID.randomUUID().toString();
        String checksum = checksum(file);
        String contentType = uploadContentType(file, fileName);
        String version = defaultVersion(versionNo);
        StorageService.ObjectWriteResult object = writeUploadObject(projectId, assetUuid, fileName,
            checksum, contentType, file.getSize(), file);
        Long fileId = repository.insertObjectUploadedFile(
            assetUuid,
            projectId,
            fileName,
            normalizeFileKind(fileKind, fileName),
            contentType,
            object.sizeBytes(),
            objectStorageUri(assetUuid),
            targetRelativePath,
            object.checksum(),
            blankToNull(discipline),
            version,
            userId
        );
        Long objectId = repository.upsertStorageObject(object, userId);
        if (objectId == null) {
            throw new BusinessException("OBJECT_UPLOAD_RECORD_FAILED", "对象存储记录写入失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        repository.insertActiveObjectVersion(fileId, objectId, version, object, userId);
        return recordSuccess(userId, projectId, "FILE_UPLOAD", "FILE", fileId, fileId,
            null, null, null, null, fileName, targetRelativePath,
            "文件已写入对象存储，未在真实 NAS 目录生成文件本体",
            new UploadStorageInfo(assetUuid, object.checksum(), "OBJECT_STORED", "OBJECT_STORAGE", object.sizeBytes()));
    }

    private StorageService.ObjectWriteResult writeUploadObject(
        Long projectId,
        String assetUuid,
        String fileName,
        String checksum,
        String contentType,
        Long sizeBytes,
        MultipartFile file
    ) {
        try {
            return storageService.writeUploadToObject(projectId, assetUuid, fileName, checksum,
                contentType, sizeBytes, file.getInputStream(), null);
        } catch (IOException exception) {
            throw sanitizedIo("OBJECT_UPLOAD_READ_FAILED", "上传文件读取失败，新增文件未写入");
        }
    }

    @Transactional
    public NasOperationResponse renameFile(Long userId, Long projectId, Long fileId, FileRenameRequest request) {
        requireWriteRoleWithoutTrial(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        Path source = resolveExistingFileFromRecord(root, file);
        String newName = normalizeName(request == null ? null : request.newName(), "NAS_FILE_NAME_REQUIRED", "文件名称不能为空");
        Path target = resolveNewChild(root, source.getParent(), newName);
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(root.relativePath(source), root.relativePath(target)));
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
        requireWriteRoleWithoutTrial(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        Path source = resolveExistingFileFromRecord(root, file);
        String targetDirectory = normalizeRelativePath(request == null ? null : request.targetDirectory(), true);
        Path targetParent = resolveExistingDirectory(root, targetDirectory);
        Path target = resolveNewChild(root, targetParent, file.originalName());
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(root.relativePath(source), root.relativePath(target)));
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
        requireAdminRoleWithoutTrial(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        FileRecord file = requireFile(projectId, fileId);
        if ("QUARANTINED".equalsIgnoreCase(file.processStatus())) {
            throw new BusinessException("NAS_FILE_ALREADY_QUARANTINED", "文件已在回收站", HttpStatus.BAD_REQUEST);
        }
        Path source = resolveExistingFileFromRecord(root, file);
        String originalRelativePath = root.relativePath(source);
        requireTrialWriteAllowed(userId, projectId, ADMIN_ROLES, List.of(originalRelativePath));
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
                "文件已移入回收站，可在保留期内恢复");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_FILE_QUARANTINE_FAILED", "文件移入回收站失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse renameDirectory(Long userId, Long projectId, DirectoryRenameRequest request) {
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        String newName = normalizeName(request == null ? null : request.newName(), "NAS_DIRECTORY_NAME_REQUIRED", "文件夹名称不能为空");
        String targetRelativePath = parentPath(sourceRelativePath).isBlank()
            ? newName
            : parentPath(sourceRelativePath) + "/" + newName;
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(sourceRelativePath, targetRelativePath));
        ProjectRoot root = resolveProjectRoot(projectId);
        Path source = resolveExistingDirectory(root, sourceRelativePath);
        Path target = resolveNewChild(root, source.getParent(), newName);
        ensureTargetAvailable(target, "NAS_DIRECTORY_RENAME_EXISTS", "同名文件夹已存在");
        try {
            Files.move(source, target);
            targetRelativePath = root.relativePath(target);
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
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        String targetParentRelativePath = normalizeRelativePath(request == null ? null : request.targetDirectory(), true);
        String displayName = leafName(sourceRelativePath);
        String targetRelativePath = joinRelative(targetParentRelativePath, displayName);
        requireTrialWriteAllowed(userId, projectId, WRITE_ROLES, List.of(sourceRelativePath, targetRelativePath));
        ProjectRoot root = resolveProjectRoot(projectId);
        Path source = resolveExistingDirectory(root, sourceRelativePath);
        Path targetParent = resolveExistingDirectory(root, targetParentRelativePath);
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
        String sourceRelativePath = normalizeRelativePath(request == null ? null : request.sourcePath(), false);
        requireTrialWriteAllowed(userId, projectId, ADMIN_ROLES, List.of(sourceRelativePath));
        ProjectRoot root = resolveProjectRoot(projectId);
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
                "文件夹已移入回收站，可在保留期内恢复");
        } catch (IOException exception) {
            throw sanitizedIo("NAS_DIRECTORY_QUARANTINE_FAILED", "文件夹移入回收站失败，请稍后重试");
        }
    }

    @Transactional
    public NasOperationResponse restoreQuarantine(Long userId, Long projectId, Long quarantineRecordId) {
        requireAdminRoleWithoutTrial(userId, projectId);
        ProjectRoot root = resolveProjectRoot(projectId);
        QuarantineRecord record = repository.findQuarantineRecord(projectId, quarantineRecordId)
            .orElseThrow(() -> new BusinessException("NAS_QUARANTINE_NOT_FOUND", "回收站记录不存在", HttpStatus.NOT_FOUND));
        if (!"QUARANTINED".equalsIgnoreCase(record.status())) {
            throw new BusinessException("NAS_QUARANTINE_STATUS_INVALID", "当前回收站记录不能恢复", HttpStatus.BAD_REQUEST);
        }
        requireTrialWriteAllowed(userId, projectId, ADMIN_ROLES, List.of(record.originalRelativePath()));
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
                record.displayName(), record.originalRelativePath(), "回收站项目已恢复到原位置");
        } catch (IOException exception) {
            repository.markQuarantineRestored(projectId, quarantineRecordId, userId, "恢复失败");
            throw sanitizedIo("NAS_RESTORE_FAILED", "回收站项目恢复失败，请稍后重试");
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
        return recordSuccess(userId, projectId, operationType, targetType, targetId, fileId,
            directoryId, quarantineRecordId, source, target, displayName, displayPath, message, null);
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
        String message,
        UploadStorageInfo storageInfo
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
            pathHint(displayPath), message, TraceIdHolder.getTraceId(), Instant.now(),
            storageInfo == null ? null : storageInfo.assetUuid(),
            storageInfo == null ? null : storageInfo.checksum(),
            storageInfo == null ? null : storageInfo.storageStatus(),
            storageInfo == null ? null : storageInfo.storageProvider(),
            storageInfo == null ? null : storageInfo.sizeBytes());
    }

    private String actionCode(String operationType) {
        return "nas." + operationType.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private TrialConfig normalizeTrialConfig(Long projectId, NasWriteTrialConfigRequest request, Long operatorId) {
        boolean enabled = request != null && Boolean.TRUE.equals(request.enabled());
        List<String> roots = normalizeTrialRoots(request == null ? null : request.allowedRelativeRoots());
        List<String> roleCodes = normalizeTrialRoleCodes(request == null ? null : request.allowedRoleCodes());
        List<Long> userIds = normalizeTrialUserIds(request == null ? null : request.allowedUserIds());
        String notice = blankToNull(request == null ? null : request.trialModeNotice());
        if (notice != null && notice.length() > 512) {
            throw new BusinessException("NAS_WRITE_TRIAL_NOTICE_TOO_LONG", "灰度提示不能超过 512 个字符", HttpStatus.BAD_REQUEST);
        }
        return new TrialConfig(projectId, enabled, roots, roleCodes, userIds, notice, Instant.now(), operatorId);
    }

    private NasWriteTrialStatusResponse toTrialStatus(
        Long userId,
        AccessibleProject project,
        TrialConfig config,
        String checkedDirectory
    ) {
        boolean baseRoleAllowed = WRITE_ROLES.contains(project.roleCode());
        boolean roleAllowed = config.allowedRoleCodes().contains(project.roleCode());
        boolean accountAllowed = config.allowedUserIds().isEmpty() || config.allowedUserIds().contains(userId);
        boolean directoryAllowed = isAllowedByTrialRoots(checkedDirectory, config.allowedRelativeRoots());
        boolean canWrite = config.enabled() && baseRoleAllowed && roleAllowed && accountAllowed && directoryAllowed;
        return new NasWriteTrialStatusResponse(
            project.id(),
            config.enabled(),
            config.allowedRelativeRoots(),
            config.allowedRoleCodes(),
            config.allowedUserIds(),
            config.trialModeNotice(),
            project.roleCode(),
            roleAllowed,
            accountAllowed,
            directoryAllowed,
            canWrite,
            checkedDirectory,
            trialDisabledReason(config, baseRoleAllowed, roleAllowed, accountAllowed, directoryAllowed),
            TraceIdHolder.getTraceId(),
            config.updatedAt()
        );
    }

    private String trialDisabledReason(
        TrialConfig config,
        boolean baseRoleAllowed,
        boolean roleAllowed,
        boolean accountAllowed,
        boolean directoryAllowed
    ) {
        if (!config.enabled()) return "当前项目未开启真实 NAS 写入灰度。";
        if (!baseRoleAllowed) return "当前项目角色只能查看，不能操作公司 NAS 文件。";
        if (!roleAllowed) return "当前项目角色未纳入 NAS 写入灰度角色范围。";
        if (!accountAllowed) return "当前账号未纳入 NAS 写入灰度账号范围。";
        if (!directoryAllowed) return "当前目录不在本项目 NAS 写入灰度允许范围内。";
        return "";
    }

    private List<String> normalizeTrialRoots(List<String> roots) {
        if (roots == null || roots.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String root : roots) {
            normalized.add(normalizeRelativePath(root, true));
        }
        return List.copyOf(normalized);
    }

    private List<String> normalizeTrialRoleCodes(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return DEFAULT_TRIAL_ROLES;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            String next = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
            if (!WRITE_ROLES.contains(next)) {
                throw new BusinessException("NAS_WRITE_TRIAL_ROLE_INVALID", "灰度角色只能是 DELIVERY_ENGINEER 或 PROJECT_ADMIN", HttpStatus.BAD_REQUEST);
            }
            normalized.add(next);
        }
        return List.copyOf(normalized);
    }

    private List<Long> normalizeTrialUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null || userId <= 0) {
                throw new BusinessException("NAS_WRITE_TRIAL_USER_INVALID", "灰度账号 ID 不合法", HttpStatus.BAD_REQUEST);
            }
            normalized.add(userId);
        }
        return List.copyOf(normalized);
    }

    private void requireTrialWriteAllowed(Long userId, Long projectId, Set<String> baseRoles, List<String> relativePaths) {
        AccessibleProject project = requireBaseRole(userId, projectId, baseRoles);
        TrialConfig config = loadTrialConfig(projectId);
        if (!config.enabled()) {
            throw new BusinessException("NAS_WRITE_TRIAL_DISABLED", "当前项目未开启真实 NAS 写入灰度，平台未执行任何 NAS 写操作", HttpStatus.FORBIDDEN);
        }
        if (!config.allowedRoleCodes().contains(project.roleCode())) {
            throw new BusinessException("NAS_WRITE_TRIAL_ROLE_FORBIDDEN", "当前项目角色未纳入 NAS 写入灰度范围", HttpStatus.FORBIDDEN);
        }
        if (!config.allowedUserIds().isEmpty() && !config.allowedUserIds().contains(userId)) {
            throw new BusinessException("NAS_WRITE_TRIAL_ACCOUNT_FORBIDDEN", "当前账号未纳入 NAS 写入灰度范围", HttpStatus.FORBIDDEN);
        }
        for (String path : relativePaths == null ? List.<String>of() : relativePaths) {
            String normalized = normalizeRelativePath(path, true);
            if (!isAllowedByTrialRoots(normalized, config.allowedRelativeRoots())) {
                throw new BusinessException("NAS_WRITE_TRIAL_DIRECTORY_FORBIDDEN", "目标目录不在本项目 NAS 写入灰度允许范围内", HttpStatus.FORBIDDEN);
            }
        }
    }

    private boolean isAllowedByTrialRoots(String relativePath, List<String> allowedRoots) {
        if (allowedRoots == null || allowedRoots.isEmpty()) {
            return false;
        }
        String normalized = normalizeRelativePath(relativePath, true);
        for (String root : allowedRoots) {
            if (root == null) continue;
            String allowedRoot = normalizeRelativePath(root, true);
            if (allowedRoot.isBlank() || sameOrChild(normalized, allowedRoot)) {
                return true;
            }
        }
        return false;
    }

    private TrialConfig loadTrialConfig(Long projectId) {
        return repository.findTrialConfig(projectId)
            .map(this::toTrialConfig)
            .orElseGet(() -> new TrialConfig(
                projectId,
                false,
                List.of(),
                DEFAULT_TRIAL_ROLES,
                List.of(),
                "当前项目未开启真实 NAS 写入灰度。",
                null,
                null
            ));
    }

    private TrialConfig toTrialConfig(TrialConfigRecord record) {
        return new TrialConfig(
            record.projectId(),
            record.enabled(),
            parseStringList(record.allowedRelativeRootsJson()),
            parseStringList(record.allowedRoleCodesJson()),
            parseLongList(record.allowedUserIdsJson()),
            record.trialModeNotice(),
            record.updatedAt(),
            record.updatedBy()
        );
    }

    private List<String> parseStringList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("NAS_WRITE_TRIAL_CONFIG_INVALID", "NAS 写入灰度配置格式错误", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Long> parseLongList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, LONG_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("NAS_WRITE_TRIAL_CONFIG_INVALID", "NAS 写入灰度配置格式错误", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("NAS_WRITE_TRIAL_CONFIG_SERIALIZE_FAILED", "NAS 写入灰度配置保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private AccessibleProject requireWriteRoleWithoutTrial(Long userId, Long projectId) {
        return requireBaseRole(userId, projectId, WRITE_ROLES);
    }

    private AccessibleProject requireAdminRoleWithoutTrial(Long userId, Long projectId) {
        return requireBaseRole(userId, projectId, ADMIN_ROLES);
    }

    private AccessibleProject requireBaseRole(Long userId, Long projectId, Set<String> allowedRoles) {
        AccessibleProject project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        if (allowedRoles.equals(ADMIN_ROLES) && !ADMIN_ROLES.contains(project.roleCode())) {
            throw new BusinessException("NAS_ADMIN_WRITE_FORBIDDEN", "删除到回收站和恢复仅限项目管理员", HttpStatus.FORBIDDEN);
        }
        if (!allowedRoles.equals(ADMIN_ROLES) && !allowedRoles.contains(project.roleCode())) {
            throw new BusinessException("NAS_WRITE_FORBIDDEN", "当前项目角色只能查看，不能操作公司 NAS 文件", HttpStatus.FORBIDDEN);
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
            throw new BusinessException("NAS_FILE_QUARANTINED", "文件已在回收站，请先恢复后再操作", HttpStatus.BAD_REQUEST);
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
            throw new BusinessException("NAS_QUARANTINE_SOURCE_MISSING", "回收站文件不存在", HttpStatus.BAD_REQUEST);
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
            throw new BusinessException("NAS_QUARANTINE_DIRECTORY_FORBIDDEN", "回收站目录不能作为操作目标", HttpStatus.BAD_REQUEST);
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
            throw new BusinessException("NAS_NAME_RESERVED", "该名称为平台回收站保留名称", HttpStatus.BAD_REQUEST);
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

    private String checksum(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw sanitizedIo("OBJECT_UPLOAD_READ_FAILED", "上传文件读取失败，新增文件未写入");
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException("OBJECT_UPLOAD_CHECKSUM_FAILED", "上传文件校验值计算失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String uploadContentType(MultipartFile file, String fileName) {
        String contentType = blankToNull(file.getContentType());
        if (contentType != null) {
            return contentType;
        }
        return switch (extension(fileName)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    private String objectStorageUri(String assetUuid) {
        return "object://asset/" + assetUuid;
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

    private BusinessException sanitizedIo(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record TrialConfig(
        Long projectId,
        boolean enabled,
        List<String> allowedRelativeRoots,
        List<String> allowedRoleCodes,
        List<Long> allowedUserIds,
        String trialModeNotice,
        Instant updatedAt,
        Long updatedBy
    ) {
    }

    private record UploadStorageInfo(
        String assetUuid,
        String checksum,
        String storageStatus,
        String storageProvider,
        Long sizeBytes
    ) {
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
            if (raw.startsWith("object://")) {
                throw new BusinessException("NAS_OPERATION_OBJECT_STORED_UNSUPPORTED",
                    "对象存储文件不能通过 NAS 文件操作移动、重命名或移入回收站", HttpStatus.PRECONDITION_FAILED);
            }
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

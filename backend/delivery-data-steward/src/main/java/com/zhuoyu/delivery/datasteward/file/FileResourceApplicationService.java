package com.zhuoyu.delivery.datasteward.file;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ProcessFileRequest;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileResourceApplicationService {

    private static final String MODULE_CODE = "data-steward";

    private final FileResourceRepository fileResourceRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public FileResourceApplicationService(
        FileResourceRepository fileResourceRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.fileResourceRepository = fileResourceRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public FileResourceResponse create(Long userId, Long projectId, FileResourceRequest request) {
        String fileKind = normalizeFileKind(request.fileKind());
        String processStatus = normalizeProcessStatus(defaultString(request.processStatus(), "PENDING"));
        Long fileId = fileResourceRepository.insert(
            projectId,
            requireText(request.originalName(), "DATA_FILE_NAME_REQUIRED", "文件名称不能为空"),
            fileKind,
            blankToNull(request.mimeType()),
            request.sizeBytes() == null ? 0L : request.sizeBytes(),
            requireText(request.storageUri(), "DATA_FILE_STORAGE_URI_REQUIRED", "存储地址不能为空"),
            blankToNull(request.checksum()),
            blankToNull(request.businessTag()),
            defaultString(request.versionNo(), "V1"),
            processStatus,
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file.create", "FILE_RESOURCE",
            String.valueOf(fileId), userId, Map.of("fileKind", fileKind));
        return requireFile(projectId, fileId);
    }

    public List<FileResourceResponse> list(Long projectId, String fileKind) {
        return fileResourceRepository.findByProject(projectId, normalizeNullableFileKind(fileKind));
    }

    @Transactional
    public FileResourceResponse process(Long userId, Long projectId, Long fileId, ProcessFileRequest request) {
        requireFile(projectId, fileId);
        String processStatus = normalizeProcessStatus(defaultString(request == null ? null : request.processStatus(), "PROCESSED"));
        fileResourceRepository.updateProcessStatus(projectId, fileId, processStatus, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file.process", "FILE_RESOURCE",
            String.valueOf(fileId), userId, Map.of("processStatus", processStatus));
        return requireFile(projectId, fileId);
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long fileId) {
        requireFile(projectId, fileId);
        fileResourceRepository.markDeleted(projectId, fileId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file.delete", "FILE_RESOURCE",
            String.valueOf(fileId), userId, Map.of());
    }

    public FileResourceResponse requireFile(Long projectId, Long fileId) {
        return fileResourceRepository.findByProjectAndId(projectId, fileId)
            .orElseThrow(() -> new BusinessException("DATA_FILE_NOT_FOUND", "文件资源不存在", HttpStatus.NOT_FOUND));
    }

    public int countByProject(Long projectId) {
        return fileResourceRepository.countByProject(projectId);
    }

    public int countByProjectAndKind(Long projectId, String fileKind) {
        return fileResourceRepository.countByProjectAndKind(projectId, fileKind);
    }

    private String normalizeNullableFileKind(String fileKind) {
        return fileKind == null || fileKind.isBlank() ? null : normalizeFileKind(fileKind);
    }

    private String normalizeFileKind(String fileKind) {
        String normalized = requireText(fileKind, "DATA_FILE_KIND_REQUIRED", "文件类型不能为空").toUpperCase();
        if (!List.of("DOCUMENT", "DRAWING", "MODEL").contains(normalized)) {
            throw new BusinessException("DATA_FILE_KIND_INVALID", "文件类型只能是 DOCUMENT、DRAWING 或 MODEL", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeProcessStatus(String processStatus) {
        String normalized = requireText(processStatus, "DATA_FILE_PROCESS_STATUS_REQUIRED", "处理状态不能为空").toUpperCase();
        if (!List.of("PENDING", "PROCESSING", "PROCESSED", "FAILED").contains(normalized)) {
            throw new BusinessException("DATA_FILE_PROCESS_STATUS_INVALID", "处理状态不合法", HttpStatus.BAD_REQUEST);
        }
        return normalized;
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

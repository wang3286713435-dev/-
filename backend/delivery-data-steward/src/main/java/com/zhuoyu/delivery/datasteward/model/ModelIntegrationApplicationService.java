package com.zhuoyu.delivery.datasteward.model;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.datasteward.file.FileResourceApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelIntegrationApplicationService {

    private static final String MODULE_CODE = "data-steward";

    private final ModelIntegrationRepository modelIntegrationRepository;
    private final FileResourceApplicationService fileResourceApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ModelIntegrationApplicationService(
        ModelIntegrationRepository modelIntegrationRepository,
        FileResourceApplicationService fileResourceApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.modelIntegrationRepository = modelIntegrationRepository;
        this.fileResourceApplicationService = fileResourceApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public ModelIntegrationResponse create(Long userId, Long projectId, ModelIntegrationRequest request) {
        var file = fileResourceApplicationService.requireFile(projectId, request.modelFileId());
        if (!"MODEL".equals(file.fileKind())) {
            throw new BusinessException("DATA_MODEL_FILE_KIND_INVALID", "模型集成只能选择模型文件", HttpStatus.BAD_REQUEST);
        }
        if (!"PROCESSED".equals(file.processStatus())) {
            throw new BusinessException("DATA_MODEL_FILE_NOT_PROCESSED", "模型文件处理完成后才能集成", HttpStatus.PRECONDITION_FAILED);
        }
        Long integrationId = modelIntegrationRepository.insert(
            projectId,
            requireText(request.name(), "DATA_MODEL_NAME_REQUIRED", "模型集成名称不能为空"),
            request.modelFileId(),
            defaultString(request.versionNo(), "V1"),
            request.componentCount() == null ? 0 : request.componentCount(),
            blankToNull(request.adapterPayloadJson()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.model.create", "MODEL_INTEGRATION",
            String.valueOf(integrationId), userId, Map.of("modelFileId", request.modelFileId()));
        return requireIntegration(projectId, integrationId);
    }

    public List<ModelIntegrationResponse> list(Long projectId) {
        return modelIntegrationRepository.findByProject(projectId);
    }

    @Transactional
    public ModelIntegrationResponse publish(Long userId, Long projectId, Long integrationId) {
        requireIntegration(projectId, integrationId);
        modelIntegrationRepository.publish(projectId, integrationId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.model.publish", "MODEL_INTEGRATION",
            String.valueOf(integrationId), userId, Map.of());
        return requireIntegration(projectId, integrationId);
    }

    public ModelIntegrationResponse requireIntegration(Long projectId, Long integrationId) {
        return modelIntegrationRepository.findByProjectAndId(projectId, integrationId)
            .orElseThrow(() -> new BusinessException("DATA_MODEL_NOT_FOUND", "模型集成不存在", HttpStatus.NOT_FOUND));
    }

    public int countByProject(Long projectId) {
        return modelIntegrationRepository.countByProject(projectId);
    }

    public int countPublishedByProject(Long projectId) {
        return modelIntegrationRepository.countPublishedByProject(projectId);
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

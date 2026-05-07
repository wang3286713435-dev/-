package com.zhuoyu.delivery.datasteward.object;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import com.zhuoyu.delivery.datasteward.model.ModelIntegrationApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagedObjectApplicationService {

    private static final String MODULE_CODE = "data-steward";

    private final ManagedObjectRepository managedObjectRepository;
    private final ModelIntegrationApplicationService modelIntegrationApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ManagedObjectApplicationService(
        ManagedObjectRepository managedObjectRepository,
        ModelIntegrationApplicationService modelIntegrationApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.managedObjectRepository = managedObjectRepository;
        this.modelIntegrationApplicationService = modelIntegrationApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public ManagedObjectResponse create(Long userId, Long projectId, ManagedObjectRequest request) {
        requirePublishedModel(projectId, request.modelIntegrationId());
        requireSection(projectId, request.sectionNodeId());
        String code = requireText(request.code(), "DATA_OBJECT_CODE_REQUIRED", "管理对象编码不能为空");
        if (managedObjectRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("DATA_OBJECT_CODE_DUPLICATED", "管理对象编码已存在", HttpStatus.CONFLICT);
        }
        Long objectId = managedObjectRepository.insert(
            projectId,
            request.modelIntegrationId(),
            request.sectionNodeId(),
            code,
            requireText(request.name(), "DATA_OBJECT_NAME_REQUIRED", "管理对象名称不能为空"),
            defaultString(request.objectType(), "EQUIPMENT"),
            blankToNull(request.externalId()),
            blankToNull(request.discipline()),
            normalizeStatus(defaultString(request.status(), "ACTIVE")),
            blankToNull(request.propertiesJson()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.object.create", "MANAGED_OBJECT",
            String.valueOf(objectId), userId, Map.of("code", code));
        return requireObject(projectId, objectId);
    }

    public List<ManagedObjectResponse> list(Long projectId) {
        return managedObjectRepository.findByProject(projectId);
    }

    @Transactional
    public ManagedObjectResponse update(Long userId, Long projectId, Long objectId, ManagedObjectRequest request) {
        ManagedObjectResponse current = requireObject(projectId, objectId);
        requireSection(projectId, request.sectionNodeId());
        String code = requireText(request.code(), "DATA_OBJECT_CODE_REQUIRED", "管理对象编码不能为空");
        if (managedObjectRepository.existsCode(projectId, code, objectId)) {
            throw new BusinessException("DATA_OBJECT_CODE_DUPLICATED", "管理对象编码已存在", HttpStatus.CONFLICT);
        }
        managedObjectRepository.update(
            projectId,
            objectId,
            request.sectionNodeId(),
            code,
            requireText(request.name(), "DATA_OBJECT_NAME_REQUIRED", "管理对象名称不能为空"),
            defaultString(request.objectType(), current.objectType()),
            blankToNull(request.externalId()),
            blankToNull(request.discipline()),
            normalizeStatus(defaultString(request.status(), current.status())),
            blankToNull(request.propertiesJson()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.object.update", "MANAGED_OBJECT",
            String.valueOf(objectId), userId, Map.of("code", code));
        return requireObject(projectId, objectId);
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long objectId) {
        requireObject(projectId, objectId);
        managedObjectRepository.markDeleted(projectId, objectId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.object.delete", "MANAGED_OBJECT",
            String.valueOf(objectId), userId, Map.of());
    }

    public ManagedObjectResponse requireObject(Long projectId, Long objectId) {
        return managedObjectRepository.findByProjectAndId(projectId, objectId)
            .orElseThrow(() -> new BusinessException("DATA_OBJECT_NOT_FOUND", "管理对象不存在", HttpStatus.NOT_FOUND));
    }

    public int countByProject(Long projectId) {
        return managedObjectRepository.countByProject(projectId);
    }

    private void requirePublishedModel(Long projectId, Long modelIntegrationId) {
        var model = modelIntegrationApplicationService.requireIntegration(projectId, modelIntegrationId);
        if (!"PUBLISHED".equals(model.status())) {
            throw new BusinessException("DATA_MODEL_NOT_PUBLISHED", "模型集成发布后才能生成管理对象", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private void requireSection(Long projectId, Long sectionNodeId) {
        if (sectionNodeId != null && !managedObjectRepository.sectionExists(projectId, sectionNodeId)) {
            throw new BusinessException("MASTERDATA_SECTION_NOT_FOUND", "工程管理部位不存在", HttpStatus.NOT_FOUND);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = requireText(status, "DATA_OBJECT_STATUS_REQUIRED", "状态不能为空").toUpperCase();
        if (!List.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw new BusinessException("DATA_OBJECT_STATUS_INVALID", "状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
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

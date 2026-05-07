package com.zhuoyu.delivery.masterdata.deliverable.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableType;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableTypeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableTypeResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableTypeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableAttributeRepository;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableDefinitionRepository;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableTypeRepository;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliverableTypeApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final DeliverableTypeRepository typeRepository;
    private final DeliverableDefinitionRepository definitionRepository;
    private final DeliverableAttributeRepository attributeRepository;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public DeliverableTypeApplicationService(
        DeliverableTypeRepository typeRepository,
        DeliverableDefinitionRepository definitionRepository,
        DeliverableAttributeRepository attributeRepository,
        NodeTypeApplicationService nodeTypeApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.typeRepository = typeRepository;
        this.definitionRepository = definitionRepository;
        this.attributeRepository = attributeRepository;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public DeliverableTypeResponse create(Long userId, Long projectId, CreateDeliverableTypeRequest request) {
        requireNodeTypesLocked(projectId);
        requireDefinition(projectId, request.deliverableDefinitionId());
        String code = requireText(request.code(), "MASTERDATA_DELIVERABLE_TYPE_CODE_REQUIRED", "交付物类型编码不能为空");
        String name = requireText(request.name(), "MASTERDATA_DELIVERABLE_TYPE_NAME_REQUIRED", "交付物类型名称不能为空");
        if (typeRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_TYPE_CODE_DUPLICATED", "交付物类型编码已存在", HttpStatus.CONFLICT);
        }
        Long typeId = typeRepository.insert(
            projectId,
            request.deliverableDefinitionId(),
            code,
            name,
            defaultString(request.fileKind(), "DOCUMENT"),
            defaultString(request.bindingStrategy(), "SECTION_NODE"),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-type.create",
            "DELIVERABLE_TYPE", String.valueOf(typeId), userId,
            Map.of("code", code, "name", name));
        return toResponse(requireType(projectId, typeId));
    }

    public List<DeliverableTypeResponse> listByDefinition(Long projectId, Long definitionId) {
        if (definitionId != null) {
            requireDefinition(projectId, definitionId);
            return typeRepository.findByDefinitionId(projectId, definitionId).stream().map(this::toResponse).toList();
        }
        return typeRepository.findByProject(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DeliverableTypeResponse update(Long userId, Long projectId, Long typeId, UpdateDeliverableTypeRequest request) {
        requireNodeTypesLocked(projectId);
        DeliverableType current = requireType(projectId, typeId);
        String nextCode = request.code() == null ? current.code() : requireText(request.code(), "MASTERDATA_DELIVERABLE_TYPE_CODE_REQUIRED", "交付物类型编码不能为空");
        String nextName = request.name() == null ? current.name() : requireText(request.name(), "MASTERDATA_DELIVERABLE_TYPE_NAME_REQUIRED", "交付物类型名称不能为空");
        if (typeRepository.existsCode(projectId, nextCode, typeId)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_TYPE_CODE_DUPLICATED", "交付物类型编码已存在", HttpStatus.CONFLICT);
        }
        typeRepository.update(projectId, typeId, nextCode, nextName,
            defaultString(request.fileKind(), current.fileKind()),
            defaultString(request.bindingStrategy(), current.bindingStrategy()),
            request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
            request.status() == null ? current.status() : normalizeStatus(request.status()),
            userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-type.update",
            "DELIVERABLE_TYPE", String.valueOf(typeId), userId,
            Map.of("code", nextCode, "name", nextName));
        return toResponse(requireType(projectId, typeId));
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long typeId) {
        requireNodeTypesLocked(projectId);
        DeliverableType current = requireType(projectId, typeId);
        attributeRepository.markDeletedByTypeId(projectId, typeId, userId);
        typeRepository.markDeleted(projectId, typeId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-type.delete",
            "DELIVERABLE_TYPE", String.valueOf(typeId), userId,
            Map.of("code", current.code(), "name", current.name()));
    }

    public int countByProject(Long projectId) {
        return typeRepository.countByProject(projectId);
    }

    private void requireNodeTypesLocked(Long projectId) {
        if (!nodeTypeApplicationService.allNodeTypesLocked(projectId)) {
            throw new BusinessException("MASTERDATA_NODE_TYPES_NOT_LOCKED",
                "请先在节点类型页面锁定全部节点类型，才能维护交付物标准", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private DeliverableType requireType(Long projectId, Long typeId) {
        return typeRepository.findByProjectAndId(projectId, typeId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DELIVERABLE_TYPE_NOT_FOUND", "交付物类型不存在", HttpStatus.NOT_FOUND));
    }

    private void requireDefinition(Long projectId, Long definitionId) {
        definitionRepository.findByProjectAndId(projectId, definitionId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DELIVERABLE_DEF_NOT_FOUND", "交付物定义不存在", HttpStatus.NOT_FOUND));
    }

    private DeliverableTypeResponse toResponse(DeliverableType type) {
        return new DeliverableTypeResponse(type.id(), type.projectId(), type.deliverableDefinitionId(), type.code(),
            type.name(), type.fileKind(), type.bindingStrategy(), type.sortOrder(), type.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return STATUS_ACTIVE;
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_TYPE_STATUS_INVALID", "状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        return value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

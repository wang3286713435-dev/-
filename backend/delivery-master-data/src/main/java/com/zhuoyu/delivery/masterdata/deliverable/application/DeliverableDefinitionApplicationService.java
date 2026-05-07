package com.zhuoyu.delivery.masterdata.deliverable.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableDefinition;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableDefinitionRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableDefinitionResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableDefinitionRequest;
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
public class DeliverableDefinitionApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final DeliverableDefinitionRepository definitionRepository;
    private final DeliverableTypeRepository typeRepository;
    private final DeliverableAttributeRepository attributeRepository;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public DeliverableDefinitionApplicationService(
        DeliverableDefinitionRepository definitionRepository,
        DeliverableTypeRepository typeRepository,
        DeliverableAttributeRepository attributeRepository,
        NodeTypeApplicationService nodeTypeApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.definitionRepository = definitionRepository;
        this.typeRepository = typeRepository;
        this.attributeRepository = attributeRepository;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public DeliverableDefinitionResponse create(Long userId, Long projectId, CreateDeliverableDefinitionRequest request) {
        requireNodeTypesLocked(projectId);
        nodeTypeApplicationService.requireNodeTypeExists(projectId, request.nodeTypeId());
        String code = requireText(request.code(), "MASTERDATA_DELIVERABLE_DEF_CODE_REQUIRED", "交付物定义编码不能为空");
        String name = requireText(request.name(), "MASTERDATA_DELIVERABLE_DEF_NAME_REQUIRED", "交付物定义名称不能为空");
        if (definitionRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_DEF_CODE_DUPLICATED", "交付物定义编码已存在", HttpStatus.CONFLICT);
        }
        Long definitionId = definitionRepository.insert(
            projectId,
            request.nodeTypeId(),
            code,
            name,
            defaultString(request.category(), "DOCUMENT"),
            request.required() != null && request.required(),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-definition.create",
            "DELIVERABLE_DEFINITION", String.valueOf(definitionId), userId,
            Map.of("code", code, "name", name));
        return toResponse(requireDefinition(projectId, definitionId));
    }

    public List<DeliverableDefinitionResponse> list(Long projectId) {
        return definitionRepository.findByProject(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public DeliverableDefinitionResponse update(Long userId, Long projectId, Long definitionId, UpdateDeliverableDefinitionRequest request) {
        requireNodeTypesLocked(projectId);
        DeliverableDefinition current = requireDefinition(projectId, definitionId);
        String nextCode = request.code() == null ? current.code() : requireText(request.code(), "MASTERDATA_DELIVERABLE_DEF_CODE_REQUIRED", "交付物定义编码不能为空");
        String nextName = request.name() == null ? current.name() : requireText(request.name(), "MASTERDATA_DELIVERABLE_DEF_NAME_REQUIRED", "交付物定义名称不能为空");
        if (definitionRepository.existsCode(projectId, nextCode, definitionId)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_DEF_CODE_DUPLICATED", "交付物定义编码已存在", HttpStatus.CONFLICT);
        }
        definitionRepository.update(projectId, definitionId, nextCode, nextName,
            defaultString(request.category(), current.category()),
            request.required() == null ? current.required() : request.required(),
            request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
            request.status() == null ? current.status() : normalizeStatus(request.status()),
            userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-definition.update",
            "DELIVERABLE_DEFINITION", String.valueOf(definitionId), userId,
            Map.of("code", nextCode, "name", nextName));
        return toResponse(requireDefinition(projectId, definitionId));
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long definitionId) {
        requireNodeTypesLocked(projectId);
        DeliverableDefinition current = requireDefinition(projectId, definitionId);
        // Cascade delete types and their attributes
        List<Long> typeIds = typeRepository.findByDefinitionId(projectId, definitionId).stream()
            .map(t -> t.id())
            .toList();
        for (Long typeId : typeIds) {
            attributeRepository.markDeletedByTypeId(projectId, typeId, userId);
        }
        typeRepository.markDeletedByDefinitionId(projectId, definitionId, userId);
        definitionRepository.markDeleted(projectId, definitionId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-definition.delete",
            "DELIVERABLE_DEFINITION", String.valueOf(definitionId), userId,
            Map.of("code", current.code(), "name", current.name()));
    }

    public int countByProject(Long projectId) {
        return definitionRepository.countByProject(projectId);
    }

    private void requireNodeTypesLocked(Long projectId) {
        if (!nodeTypeApplicationService.allNodeTypesLocked(projectId)) {
            throw new BusinessException("MASTERDATA_NODE_TYPES_NOT_LOCKED",
                "请先在节点类型页面锁定全部节点类型，才能维护交付物标准", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private DeliverableDefinition requireDefinition(Long projectId, Long definitionId) {
        return definitionRepository.findByProjectAndId(projectId, definitionId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DELIVERABLE_DEF_NOT_FOUND", "交付物定义不存在", HttpStatus.NOT_FOUND));
    }

    private DeliverableDefinitionResponse toResponse(DeliverableDefinition def) {
        return new DeliverableDefinitionResponse(def.id(), def.projectId(), def.nodeTypeId(), def.code(),
            def.name(), def.category(), def.required(), def.sortOrder(), def.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return STATUS_ACTIVE;
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_DEF_STATUS_INVALID", "状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
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

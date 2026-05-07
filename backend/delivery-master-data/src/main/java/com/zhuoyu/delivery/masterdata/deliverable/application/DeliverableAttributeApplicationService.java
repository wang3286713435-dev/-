package com.zhuoyu.delivery.masterdata.deliverable.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableAttribute;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableAttributeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableAttributeResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableAttributeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableAttributeRepository;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableTypeRepository;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliverableAttributeApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final DeliverableAttributeRepository attributeRepository;
    private final DeliverableTypeRepository typeRepository;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public DeliverableAttributeApplicationService(
        DeliverableAttributeRepository attributeRepository,
        DeliverableTypeRepository typeRepository,
        NodeTypeApplicationService nodeTypeApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.attributeRepository = attributeRepository;
        this.typeRepository = typeRepository;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public DeliverableAttributeResponse create(Long userId, Long projectId, CreateDeliverableAttributeRequest request) {
        requireNodeTypesLocked(projectId);
        requireType(projectId, request.deliverableTypeId());
        String code = requireText(request.code(), "MASTERDATA_DELIVERABLE_ATTR_CODE_REQUIRED", "交付物属性编码不能为空");
        String name = requireText(request.name(), "MASTERDATA_DELIVERABLE_ATTR_NAME_REQUIRED", "交付物属性名称不能为空");
        if (attributeRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_ATTR_CODE_DUPLICATED", "交付物属性编码已存在", HttpStatus.CONFLICT);
        }
        Long attributeId = attributeRepository.insert(
            projectId,
            request.deliverableTypeId(),
            code,
            name,
            defaultString(request.valueType(), "TEXT"),
            request.unit(),
            request.required() != null && request.required(),
            request.exampleValue(),
            request.enumOptions(),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-attribute.create",
            "DELIVERABLE_ATTRIBUTE", String.valueOf(attributeId), userId,
            Map.of("code", code, "name", name));
        return toResponse(requireAttribute(projectId, attributeId));
    }

    public List<DeliverableAttributeResponse> listByType(Long projectId, Long typeId) {
        if (typeId != null) {
            requireType(projectId, typeId);
            return attributeRepository.findByTypeId(projectId, typeId).stream().map(this::toResponse).toList();
        }
        return attributeRepository.findByProject(projectId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DeliverableAttributeResponse update(Long userId, Long projectId, Long attributeId, UpdateDeliverableAttributeRequest request) {
        requireNodeTypesLocked(projectId);
        DeliverableAttribute current = requireAttribute(projectId, attributeId);
        String nextCode = request.code() == null ? current.code() : requireText(request.code(), "MASTERDATA_DELIVERABLE_ATTR_CODE_REQUIRED", "交付物属性编码不能为空");
        String nextName = request.name() == null ? current.name() : requireText(request.name(), "MASTERDATA_DELIVERABLE_ATTR_NAME_REQUIRED", "交付物属性名称不能为空");
        if (attributeRepository.existsCode(projectId, nextCode, attributeId)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_ATTR_CODE_DUPLICATED", "交付物属性编码已存在", HttpStatus.CONFLICT);
        }
        attributeRepository.update(projectId, attributeId, nextCode, nextName,
            defaultString(request.valueType(), current.valueType()),
            request.unit() == null ? current.unit() : request.unit(),
            request.required() == null ? current.required() : request.required(),
            request.exampleValue() == null ? current.exampleValue() : request.exampleValue(),
            request.enumOptions() == null ? current.enumOptions() : request.enumOptions(),
            request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
            request.status() == null ? current.status() : normalizeStatus(request.status()),
            userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-attribute.update",
            "DELIVERABLE_ATTRIBUTE", String.valueOf(attributeId), userId,
            Map.of("code", nextCode, "name", nextName));
        return toResponse(requireAttribute(projectId, attributeId));
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long attributeId) {
        requireNodeTypesLocked(projectId);
        DeliverableAttribute current = requireAttribute(projectId, attributeId);
        attributeRepository.markDeleted(projectId, attributeId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.deliverable-attribute.delete",
            "DELIVERABLE_ATTRIBUTE", String.valueOf(attributeId), userId,
            Map.of("code", current.code(), "name", current.name()));
    }

    public int countByProject(Long projectId) {
        return attributeRepository.countByProject(projectId);
    }

    private void requireNodeTypesLocked(Long projectId) {
        if (!nodeTypeApplicationService.allNodeTypesLocked(projectId)) {
            throw new BusinessException("MASTERDATA_NODE_TYPES_NOT_LOCKED",
                "请先在节点类型页面锁定全部节点类型，才能维护交付物标准", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private DeliverableAttribute requireAttribute(Long projectId, Long attributeId) {
        return attributeRepository.findByProjectAndId(projectId, attributeId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DELIVERABLE_ATTR_NOT_FOUND", "交付物属性不存在", HttpStatus.NOT_FOUND));
    }

    private void requireType(Long projectId, Long typeId) {
        typeRepository.findByProjectAndId(projectId, typeId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DELIVERABLE_TYPE_NOT_FOUND", "交付物类型不存在", HttpStatus.NOT_FOUND));
    }

    private DeliverableAttributeResponse toResponse(DeliverableAttribute attr) {
        return new DeliverableAttributeResponse(attr.id(), attr.projectId(), attr.deliverableTypeId(), attr.code(),
            attr.name(), attr.valueType(), attr.unit(), attr.required(), attr.exampleValue(), attr.enumOptions(),
            attr.sortOrder(), attr.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return STATUS_ACTIVE;
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_DELIVERABLE_ATTR_STATUS_INVALID", "状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
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

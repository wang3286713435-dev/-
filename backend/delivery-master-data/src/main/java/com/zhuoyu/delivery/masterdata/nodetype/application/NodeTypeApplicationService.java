package com.zhuoyu.delivery.masterdata.nodetype.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.domain.NodeType;
import com.zhuoyu.delivery.masterdata.nodetype.dto.CreateNodeTypeRequest;
import com.zhuoyu.delivery.masterdata.nodetype.dto.NodeTypeLockStatusResponse;
import com.zhuoyu.delivery.masterdata.nodetype.dto.NodeTypeResponse;
import com.zhuoyu.delivery.masterdata.nodetype.dto.UpdateNodeTypeRequest;
import com.zhuoyu.delivery.masterdata.nodetype.repository.NodeTypeRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NodeTypeApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final NodeTypeRepository nodeTypeRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public NodeTypeApplicationService(
        NodeTypeRepository nodeTypeRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.nodeTypeRepository = nodeTypeRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public NodeTypeResponse create(Long userId, Long projectId, CreateNodeTypeRequest request) {
        String code = requireText(request.code(), "MASTERDATA_NODE_TYPE_CODE_REQUIRED", "节点类型编码不能为空");
        String name = requireText(request.name(), "MASTERDATA_NODE_TYPE_NAME_REQUIRED", "节点类型名称不能为空");
        if (nodeTypeRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("MASTERDATA_NODE_TYPE_CODE_DUPLICATED", "节点类型编码已存在", HttpStatus.CONFLICT);
        }
        Long nodeTypeId = nodeTypeRepository.insert(
            projectId,
            code,
            name,
            request.scopeLevel(),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.node-type.create",
            "NODE_TYPE",
            String.valueOf(nodeTypeId),
            userId,
            Map.of("code", code, "name", name)
        );
        return toResponse(requireNodeType(projectId, nodeTypeId));
    }

    public List<NodeTypeResponse> list(Long projectId) {
        return nodeTypeRepository.findByProject(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public NodeTypeResponse update(Long userId, Long projectId, Long nodeTypeId, UpdateNodeTypeRequest request) {
        NodeType current = requireNodeType(projectId, nodeTypeId);
        if (Boolean.TRUE.equals(current.locked())) {
            throw new BusinessException("MASTERDATA_NODE_TYPE_LOCKED", "节点类型已锁定，不允许编辑", HttpStatus.CONFLICT);
        }
        String nextCode = request.code() == null
            ? current.code()
            : requireText(request.code(), "MASTERDATA_NODE_TYPE_CODE_REQUIRED", "节点类型编码不能为空");
        String nextName = request.name() == null
            ? current.name()
            : requireText(request.name(), "MASTERDATA_NODE_TYPE_NAME_REQUIRED", "节点类型名称不能为空");
        if (nodeTypeRepository.existsCode(projectId, nextCode, nodeTypeId)) {
            throw new BusinessException("MASTERDATA_NODE_TYPE_CODE_DUPLICATED", "节点类型编码已存在", HttpStatus.CONFLICT);
        }
        nodeTypeRepository.update(
            projectId,
            nodeTypeId,
            nextCode,
            nextName,
            request.scopeLevel() == null ? current.scopeLevel() : request.scopeLevel(),
            request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
            request.status() == null ? current.status() : normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.node-type.update",
            "NODE_TYPE",
            String.valueOf(nodeTypeId),
            userId,
            Map.of("code", nextCode, "name", nextName)
        );
        return toResponse(requireNodeType(projectId, nodeTypeId));
    }

    @Transactional
    public NodeTypeResponse lock(Long userId, Long projectId, Long nodeTypeId) {
        NodeType current = requireNodeType(projectId, nodeTypeId);
        nodeTypeRepository.lock(projectId, nodeTypeId, userId);
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.node-type.lock",
            "NODE_TYPE",
            String.valueOf(nodeTypeId),
            userId,
            Map.of("code", current.code(), "name", current.name())
        );
        return toResponse(requireNodeType(projectId, nodeTypeId));
    }

    @Transactional
    public NodeTypeLockStatusResponse lockAll(Long userId, Long projectId) {
        if (nodeTypeRepository.countByProject(projectId) == 0) {
            throw new BusinessException("MASTERDATA_NODE_TYPE_EMPTY", "请先创建节点类型", HttpStatus.BAD_REQUEST);
        }
        nodeTypeRepository.lockAll(projectId, userId);
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.node-type.lock-all",
            "NODE_TYPE",
            String.valueOf(projectId),
            userId,
            Map.of("projectId", projectId)
        );
        return projectLockStatus(projectId);
    }

    public NodeTypeLockStatusResponse lockStatus(Long projectId, Long nodeTypeId) {
        NodeType nodeType = requireNodeType(projectId, nodeTypeId);
        int total = nodeTypeRepository.countByProject(projectId);
        int locked = nodeTypeRepository.countLockedByProject(projectId);
        return new NodeTypeLockStatusResponse(
            projectId,
            nodeType.id(),
            nodeType.locked(),
            nodeType.lockedAt(),
            nodeType.lockedBy(),
            total > 0,
            total > 0 && total == locked,
            total
        );
    }

    public NodeTypeLockStatusResponse projectLockStatus(Long projectId) {
        int total = nodeTypeRepository.countByProject(projectId);
        int locked = nodeTypeRepository.countLockedByProject(projectId);
        return new NodeTypeLockStatusResponse(
            projectId,
            null,
            total > 0 && total == locked,
            null,
            null,
            total > 0,
            total > 0 && total == locked,
            total
        );
    }

    public boolean hasAnyNodeType(Long projectId) {
        return nodeTypeRepository.countByProject(projectId) > 0;
    }

    public boolean allNodeTypesLocked(Long projectId) {
        int total = nodeTypeRepository.countByProject(projectId);
        return total > 0 && total == nodeTypeRepository.countLockedByProject(projectId);
    }

    public int countByProject(Long projectId) {
        return nodeTypeRepository.countByProject(projectId);
    }

    public void requireNodeTypeExists(Long projectId, Long nodeTypeId) {
        requireNodeType(projectId, nodeTypeId);
    }

    private NodeType requireNodeType(Long projectId, Long nodeTypeId) {
        return nodeTypeRepository.findByProjectAndId(projectId, nodeTypeId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_NODE_TYPE_NOT_FOUND", "节点类型不存在", HttpStatus.NOT_FOUND));
    }

    private NodeTypeResponse toResponse(NodeType nodeType) {
        return new NodeTypeResponse(
            nodeType.id(),
            nodeType.projectId(),
            nodeType.code(),
            nodeType.name(),
            nodeType.scopeLevel(),
            nodeType.sortOrder(),
            nodeType.status(),
            nodeType.locked(),
            nodeType.lockedAt(),
            nodeType.lockedBy()
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_NODE_TYPE_STATUS_INVALID", "节点类型状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }
}

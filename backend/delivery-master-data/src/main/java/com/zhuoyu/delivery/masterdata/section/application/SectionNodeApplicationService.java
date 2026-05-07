package com.zhuoyu.delivery.masterdata.section.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.section.domain.SectionNode;
import com.zhuoyu.delivery.masterdata.section.dto.CreateSectionNodeRequest;
import com.zhuoyu.delivery.masterdata.section.dto.SectionNodeResponse;
import com.zhuoyu.delivery.masterdata.section.dto.UpdateSectionNodeRequest;
import com.zhuoyu.delivery.masterdata.section.repository.SectionNodeRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SectionNodeApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final SectionNodeRepository sectionNodeRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public SectionNodeApplicationService(
        SectionNodeRepository sectionNodeRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.sectionNodeRepository = sectionNodeRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public SectionNodeResponse create(Long userId, Long projectId, CreateSectionNodeRequest request) {
        String code = requireText(request.code(), "MASTERDATA_SECTION_CODE_REQUIRED", "部位编码不能为空");
        String name = requireText(request.name(), "MASTERDATA_SECTION_NAME_REQUIRED", "部位名称不能为空");
        if (sectionNodeRepository.existsCode(projectId, code, null)) {
            throw new BusinessException("MASTERDATA_SECTION_CODE_DUPLICATED", "部位编码已存在", HttpStatus.CONFLICT);
        }

        SectionNode parent = null;
        if (request.parentId() != null) {
            parent = requireNode(projectId, request.parentId());
        }
        int level = parent == null ? 1 : parent.level() + 1;
        String status = normalizeStatus(request.status());
        Integer sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        Long nodeId = sectionNodeRepository.insert(
            projectId,
            parent == null ? null : parent.id(),
            code,
            name,
            level,
            "",
            sortOrder,
            status,
            userId
        );
        String path = parent == null ? "/" + nodeId : parent.path() + "/" + nodeId;
        sectionNodeRepository.updatePath(projectId, nodeId, path, userId);
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.section-node.create",
            "SECTION_NODE",
            String.valueOf(nodeId),
            userId,
            Map.of("code", code, "name", name)
        );
        return toResponse(requireNode(projectId, nodeId), List.of());
    }

    public List<SectionNodeResponse> tree(Long projectId) {
        List<SectionNode> nodes = sectionNodeRepository.findByProject(projectId);
        return buildChildren(nodes, null);
    }

    @Transactional
    public SectionNodeResponse update(Long userId, Long projectId, Long nodeId, UpdateSectionNodeRequest request) {
        SectionNode current = requireNode(projectId, nodeId);
        String nextCode = request.code() == null
            ? current.code()
            : requireText(request.code(), "MASTERDATA_SECTION_CODE_REQUIRED", "部位编码不能为空");
        String nextName = request.name() == null
            ? current.name()
            : requireText(request.name(), "MASTERDATA_SECTION_NAME_REQUIRED", "部位名称不能为空");
        if (sectionNodeRepository.existsCode(projectId, nextCode, nodeId)) {
            throw new BusinessException("MASTERDATA_SECTION_CODE_DUPLICATED", "部位编码已存在", HttpStatus.CONFLICT);
        }

        Long nextParentId = request.parentId() == null ? current.parentId() : request.parentId();
        SectionNode nextParent = null;
        if (nextParentId != null) {
            if (nextParentId.equals(nodeId)) {
                throw new BusinessException("MASTERDATA_SECTION_PARENT_INVALID", "不能把部位移动到自身下级", HttpStatus.BAD_REQUEST);
            }
            nextParent = requireNode(projectId, nextParentId);
            if (nextParent.path().startsWith(current.path() + "/")) {
                throw new BusinessException("MASTERDATA_SECTION_PARENT_INVALID", "不能把部位移动到自身子级下", HttpStatus.BAD_REQUEST);
            }
        }
        int nextLevel = nextParent == null ? 1 : nextParent.level() + 1;
        String nextPath = nextParent == null ? "/" + nodeId : nextParent.path() + "/" + nodeId;
        Integer nextSortOrder = request.sortOrder() == null ? current.sortOrder() : request.sortOrder();
        String nextStatus = request.status() == null ? current.status() : normalizeStatus(request.status());

        sectionNodeRepository.updateNode(
            projectId,
            nodeId,
            nextParentId,
            nextCode,
            nextName,
            nextLevel,
            nextPath,
            nextSortOrder,
            nextStatus,
            userId
        );
        if (!current.path().equals(nextPath) || !Objects.equals(current.level(), nextLevel)) {
            sectionNodeRepository.updateDescendantPaths(projectId, current.path(), nextPath, nextLevel - current.level(), userId);
        }
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.section-node.update",
            "SECTION_NODE",
            String.valueOf(nodeId),
            userId,
            Map.of("code", nextCode, "name", nextName, "status", nextStatus)
        );
        return toResponse(requireNode(projectId, nodeId), List.of());
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long nodeId) {
        SectionNode node = requireNode(projectId, nodeId);
        sectionNodeRepository.markDeletedByPath(projectId, node.path(), userId);
        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            "masterdata.section-node.delete",
            "SECTION_NODE",
            String.valueOf(nodeId),
            userId,
            Map.of("code", node.code(), "name", node.name())
        );
    }

    public boolean hasAnyNode(Long projectId) {
        return sectionNodeRepository.hasAnyNode(projectId);
    }

    public int countByProject(Long projectId) {
        return sectionNodeRepository.countByProject(projectId);
    }

    private SectionNode requireNode(Long projectId, Long nodeId) {
        return sectionNodeRepository.findByProjectAndId(projectId, nodeId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_SECTION_NODE_NOT_FOUND", "工程管理部位不存在", HttpStatus.NOT_FOUND));
    }

    private List<SectionNodeResponse> buildChildren(List<SectionNode> nodes, Long parentId) {
        return nodes.stream()
            .filter(node -> Objects.equals(node.parentId(), parentId))
            .sorted(Comparator.comparing(SectionNode::sortOrder).thenComparing(SectionNode::id))
            .map(node -> toResponse(node, buildChildren(nodes, node.id())))
            .toList();
    }

    private SectionNodeResponse toResponse(SectionNode node, List<SectionNodeResponse> children) {
        return new SectionNodeResponse(
            node.id(),
            node.projectId(),
            node.parentId(),
            node.code(),
            node.name(),
            node.level(),
            node.path(),
            node.sortOrder(),
            node.status(),
            children
        );
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_SECTION_STATUS_INVALID", "部位状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
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

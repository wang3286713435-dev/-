package com.zhuoyu.delivery.masterdata.template.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.masterdata.template.domain.DirectoryTemplate;
import com.zhuoyu.delivery.masterdata.template.dto.CreateDirectoryTemplateRequest;
import com.zhuoyu.delivery.masterdata.template.dto.DirectoryTemplateResponse;
import com.zhuoyu.delivery.masterdata.template.dto.UpdateDirectoryTemplateRequest;
import com.zhuoyu.delivery.masterdata.template.repository.DirectoryTemplateRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectoryTemplateApplicationService {

    private static final String MODULE_CODE = "master-data";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final DirectoryTemplateRepository templateRepository;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public DirectoryTemplateApplicationService(
        DirectoryTemplateRepository templateRepository,
        NodeTypeApplicationService nodeTypeApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.templateRepository = templateRepository;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @Transactional
    public DirectoryTemplateResponse create(Long userId, Long projectId, CreateDirectoryTemplateRequest request) {
        requireNodeTypesLocked(projectId);
        String name = requireText(request.name(), "MASTERDATA_DIR_TEMPLATE_NAME_REQUIRED", "目录模板名称不能为空");
        if (templateRepository.existsName(projectId, name, null)) {
            throw new BusinessException("MASTERDATA_DIR_TEMPLATE_NAME_DUPLICATED", "目录模板名称已存在", HttpStatus.CONFLICT);
        }
        Long templateId = templateRepository.insert(
            projectId,
            defaultString(request.templateType(), "DOCUMENT"),
            name,
            request.rootNodeJson(),
            defaultString(request.sourceType(), "MANUAL"),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            normalizeStatus(request.status()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.directory-template.create",
            "DIRECTORY_TEMPLATE", String.valueOf(templateId), userId,
            Map.of("name", name));
        return toResponse(requireTemplate(projectId, templateId));
    }

    public List<DirectoryTemplateResponse> list(Long projectId) {
        return templateRepository.findByProject(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public DirectoryTemplateResponse update(Long userId, Long projectId, Long templateId, UpdateDirectoryTemplateRequest request) {
        requireNodeTypesLocked(projectId);
        DirectoryTemplate current = requireTemplate(projectId, templateId);
        String nextName = request.name() == null ? current.name() : requireText(request.name(), "MASTERDATA_DIR_TEMPLATE_NAME_REQUIRED", "目录模板名称不能为空");
        if (templateRepository.existsName(projectId, nextName, templateId)) {
            throw new BusinessException("MASTERDATA_DIR_TEMPLATE_NAME_DUPLICATED", "目录模板名称已存在", HttpStatus.CONFLICT);
        }
        templateRepository.update(projectId, templateId,
            defaultString(request.templateType(), current.templateType()),
            nextName,
            request.rootNodeJson() == null ? current.rootNodeJson() : request.rootNodeJson(),
            defaultString(request.sourceType(), current.sourceType()),
            request.sortOrder() == null ? current.sortOrder() : request.sortOrder(),
            request.status() == null ? current.status() : normalizeStatus(request.status()),
            userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.directory-template.update",
            "DIRECTORY_TEMPLATE", String.valueOf(templateId), userId,
            Map.of("name", nextName));
        return toResponse(requireTemplate(projectId, templateId));
    }

    @Transactional
    public void delete(Long userId, Long projectId, Long templateId) {
        requireNodeTypesLocked(projectId);
        DirectoryTemplate current = requireTemplate(projectId, templateId);
        templateRepository.markDeleted(projectId, templateId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "masterdata.directory-template.delete",
            "DIRECTORY_TEMPLATE", String.valueOf(templateId), userId,
            Map.of("name", current.name()));
    }

    public int countByProject(Long projectId) {
        return templateRepository.countByProject(projectId);
    }

    private void requireNodeTypesLocked(Long projectId) {
        if (!nodeTypeApplicationService.allNodeTypesLocked(projectId)) {
            throw new BusinessException("MASTERDATA_NODE_TYPES_NOT_LOCKED",
                "请先在节点类型页面锁定全部节点类型，才能维护交付物标准", HttpStatus.PRECONDITION_FAILED);
        }
    }

    private DirectoryTemplate requireTemplate(Long projectId, Long templateId) {
        return templateRepository.findByProjectAndId(projectId, templateId)
            .orElseThrow(() -> new BusinessException("MASTERDATA_DIR_TEMPLATE_NOT_FOUND", "目录模板不存在", HttpStatus.NOT_FOUND));
    }

    private DirectoryTemplateResponse toResponse(DirectoryTemplate template) {
        return new DirectoryTemplateResponse(template.id(), template.projectId(), template.templateType(),
            template.name(), template.rootNodeJson(), template.sourceType(), template.sortOrder(), template.status());
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return STATUS_ACTIVE;
        String normalized = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new BusinessException("MASTERDATA_DIR_TEMPLATE_STATUS_INVALID", "状态只能是 ACTIVE 或 DISABLED", HttpStatus.BAD_REQUEST);
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

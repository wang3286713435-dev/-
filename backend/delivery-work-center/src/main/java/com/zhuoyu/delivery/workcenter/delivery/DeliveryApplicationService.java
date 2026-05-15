package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableDefinitionApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableTypeApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.masterdata.section.application.SectionNodeApplicationService;
import com.zhuoyu.delivery.masterdata.template.application.DirectoryTemplateApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryViewResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RejectRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ReviewRecordResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryApplicationService {

    private static final String MODULE_CODE = "work-center";

    private final DeliveryBindingRepository deliveryBindingRepository;
    private final AuditLogApplicationService auditLogApplicationService;
    private final SectionNodeApplicationService sectionNodeApplicationService;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final DeliverableDefinitionApplicationService definitionApplicationService;
    private final DeliverableTypeApplicationService typeApplicationService;
    private final DirectoryTemplateApplicationService templateApplicationService;

    public DeliveryApplicationService(
        DeliveryBindingRepository deliveryBindingRepository,
        AuditLogApplicationService auditLogApplicationService,
        SectionNodeApplicationService sectionNodeApplicationService,
        NodeTypeApplicationService nodeTypeApplicationService,
        DeliverableDefinitionApplicationService definitionApplicationService,
        DeliverableTypeApplicationService typeApplicationService,
        DirectoryTemplateApplicationService templateApplicationService
    ) {
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.auditLogApplicationService = auditLogApplicationService;
        this.sectionNodeApplicationService = sectionNodeApplicationService;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.definitionApplicationService = definitionApplicationService;
        this.typeApplicationService = typeApplicationService;
        this.templateApplicationService = templateApplicationService;
    }

    @Transactional
    public DeliveryBindingResponse createBinding(Long userId, Long projectId, DeliveryBindingRequest request) {
        String viewType = normalizeViewType(request.viewType());
        String expectedFileKind = expectedFileKind(viewType);
        if (!deliveryBindingRepository.deliverableTypeExists(projectId, request.deliverableTypeId(), expectedFileKind)) {
            throw new BusinessException("WORK_DELIVERABLE_TYPE_NOT_FOUND", "交付物类型不存在或文件类型不匹配", HttpStatus.NOT_FOUND);
        }
        if (!deliveryBindingRepository.fileExists(projectId, request.fileResourceId(), expectedFileKind)) {
            throw new BusinessException("WORK_FILE_NOT_READY", "文件不存在、类型不匹配或尚未处理完成", HttpStatus.PRECONDITION_FAILED);
        }
        requireSection(projectId, request.sectionNodeId());
        requireObject(projectId, request.managedObjectId());
        if (request.sectionNodeId() == null && request.managedObjectId() == null) {
            throw new BusinessException("WORK_BINDING_TARGET_REQUIRED", "交付绑定必须选择部位或管理对象", HttpStatus.BAD_REQUEST);
        }
        Long bindingId = deliveryBindingRepository.insert(
            projectId,
            viewType,
            request.sectionNodeId(),
            request.managedObjectId(),
            request.deliverableTypeId(),
            request.fileResourceId(),
            defaultString(request.bindingStatus(), "BOUND"),
            defaultString(request.reviewStatus(), "PENDING"),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            blankToNull(request.remark()),
            userId
        );
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-binding.create", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of("viewType", viewType));
        return requireBinding(projectId, bindingId);
    }

    public DeliveryViewResponse deliveryView(Long projectId, String viewType) {
        String normalized = normalizeViewType(viewType);
        List<DeliveryBindingResponse> rows = deliveryBindingRepository.findByProjectAndViewType(projectId, normalized);
        int boundCount = (int) rows.stream().filter(row -> "BOUND".equals(row.bindingStatus())).count();
        return new DeliveryViewResponse(projectId, normalized, rows.size(), boundCount, rows);
    }

    public DashboardSummaryResponse dashboardSummary(Long projectId) {
        return deliveryBindingRepository.dashboardSummary(projectId);
    }

    @Transactional
    public void deleteBinding(Long userId, Long projectId, Long bindingId) {
        requireBinding(projectId, bindingId);
        deliveryBindingRepository.markDeleted(projectId, bindingId, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-binding.delete", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of());
    }

    // ---- review ----

    @Transactional
    public DeliveryBindingResponse submitReview(Long userId, Long projectId, Long bindingId) {
        var binding = requireBinding(projectId, bindingId);
        String currentStatus = binding.reviewStatus();
        if (!"DRAFT".equals(currentStatus) && !"PENDING".equals(currentStatus)) {
            throw new BusinessException("WORK_REVIEW_INVALID_STATUS",
                "当前审核状态不允许提交审核: " + currentStatus, HttpStatus.CONFLICT);
        }
        String newStatus = "PENDING";
        deliveryBindingRepository.updateReviewStatus(projectId, bindingId, newStatus);
        deliveryBindingRepository.insertReviewRecord(projectId, bindingId, "SUBMITTED", "提交审核", userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.review.submit", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of("from", currentStatus, "to", newStatus));
        return requireBinding(projectId, bindingId);
    }

    @Transactional
    public DeliveryBindingResponse approve(Long userId, Long projectId, Long bindingId) {
        var binding = requireBinding(projectId, bindingId);
        String currentStatus = binding.reviewStatus();
        if (!"PENDING".equals(currentStatus) && !"REJECTED".equals(currentStatus)) {
            throw new BusinessException("WORK_REVIEW_INVALID_STATUS",
                "当前审核状态不允许通过: " + currentStatus, HttpStatus.CONFLICT);
        }
        String newStatus = "APPROVED";
        deliveryBindingRepository.updateReviewStatus(projectId, bindingId, newStatus);
        deliveryBindingRepository.insertReviewRecord(projectId, bindingId, "APPROVED", null, userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.review.approve", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of("from", currentStatus, "to", newStatus));
        return requireBinding(projectId, bindingId);
    }

    @Transactional
    public DeliveryBindingResponse reject(Long userId, Long projectId, Long bindingId, RejectRequest request) {
        var binding = requireBinding(projectId, bindingId);
        String currentStatus = binding.reviewStatus();
        if (!"PENDING".equals(currentStatus) && !"REJECTED".equals(currentStatus)) {
            throw new BusinessException("WORK_REVIEW_INVALID_STATUS",
                "当前审核状态不允许驳回: " + currentStatus, HttpStatus.CONFLICT);
        }
        String newStatus = "REJECTED";
        deliveryBindingRepository.updateReviewStatus(projectId, bindingId, newStatus);
        deliveryBindingRepository.insertReviewRecord(projectId, bindingId, "REJECTED", request.reason(), userId);
        // Auto-create rectification item
        String rectTitle = "交付审核驳回: " + (binding.fileName() != null ? binding.fileName() : "绑定#" + bindingId);
        Long rectId = deliveryBindingRepository.insertRectification(projectId, bindingId, rectTitle, request.reason(), userId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.review.reject", "DELIVERY_BINDING",
            String.valueOf(bindingId), userId, Map.of("from", currentStatus, "to", newStatus, "reason", request.reason(), "rectificationId", rectId));
        return requireBinding(projectId, bindingId);
    }

    public List<ReviewRecordResponse> getReviewRecords(Long projectId, Long bindingId) {
        requireBinding(projectId, bindingId);
        return deliveryBindingRepository.findReviewRecords(projectId, bindingId);
    }

    public DeliveryCompletenessResponse deliveryCompleteness(Long projectId, String viewType, String targetType, boolean onlyMissing) {
        String normalizedView = normalizeViewType(viewType);
        String normalizedTarget = normalizeTargetType(targetType);
        String fileKind = expectedFileKind(normalizedView);

        // standard readiness
        int sectionNodeCount = sectionNodeApplicationService.countByProject(projectId);
        int nodeTypeCount = nodeTypeApplicationService.countByProject(projectId);
        boolean hasSectionTree = sectionNodeCount > 0;
        boolean hasNodeTypes = nodeTypeCount > 0;
        boolean nodeTypesLocked = hasNodeTypes && nodeTypeApplicationService.allNodeTypesLocked(projectId);
        int definitionCount = definitionApplicationService.countByProject(projectId);
        boolean hasDefinitions = definitionCount > 0;
        boolean hasTypesForKind = deliveryBindingRepository.hasDeliverableTypesForKind(projectId, fileKind);
        int templateCount = templateApplicationService.countByProject(projectId);
        boolean hasTemplates = templateCount > 0;

        List<String> issues = deliveryBindingRepository.readinessIssues(
            projectId, hasSectionTree, hasNodeTypes, nodeTypesLocked,
            hasDefinitions, hasTypesForKind, hasTemplates);

        boolean standardReady = hasSectionTree && nodeTypesLocked && hasDefinitions && hasTypesForKind && hasTemplates;

        if (!standardReady) {
            return new DeliveryCompletenessResponse(
                projectId, normalizedView, normalizedTarget,
                false, issues, 0, 0, 0, 0.0, List.of());
        }

        // required rows: section/object × deliverable type cross product
        List<DeliveryCompletenessRow> required = deliveryBindingRepository.findRequiredDeliverables(
            projectId, fileKind, normalizedTarget, normalizedView);

        if (required.isEmpty()) {
            return new DeliveryCompletenessResponse(
                projectId, normalizedView, normalizedTarget,
                true, List.of(), 0, 0, 0, 1.0, List.of());
        }

        // completed bindings
        List<DeliveryCompletenessRow> completed = deliveryBindingRepository.findCompletedBindings(
            projectId, normalizedView, normalizedTarget);

        // build a map keyed by (targetId, deliverableTypeId) -> best completed binding
        var completedMap = new LinkedHashMap<String, DeliveryCompletenessRow>();
        for (var c : completed) {
            String key = c.targetId() + "_" + c.deliverableTypeId();
            completedMap.putIfAbsent(key, c);
        }

        // merge: enrich required rows with completed data
        List<DeliveryCompletenessRow> rows = new ArrayList<>();
        int completedCount = 0;
        for (var req : required) {
            String key = req.targetId() + "_" + req.deliverableTypeId();
            var match = completedMap.get(key);
            if (match != null) {
                completedCount++;
                rows.add(new DeliveryCompletenessRow(
                    req.targetType(), req.targetId(), req.targetCode(), req.targetName(),
                    req.deliverableDefinitionId(), req.deliverableDefinitionCode(), req.deliverableDefinitionName(),
                    req.deliverableTypeId(), req.deliverableTypeCode(), req.deliverableTypeName(),
                    req.fileKind(), true, true,
                    match.bindingId(), match.fileResourceId(), match.fileName(),
                    match.versionNo(), match.reviewStatus(), null));
            } else {
                rows.add(new DeliveryCompletenessRow(
                    req.targetType(), req.targetId(), req.targetCode(), req.targetName(),
                    req.deliverableDefinitionId(), req.deliverableDefinitionCode(), req.deliverableDefinitionName(),
                    req.deliverableTypeId(), req.deliverableTypeCode(), req.deliverableTypeName(),
                    req.fileKind(), true, false,
                    null, null, null, null, null,
                    "尚未挂接文件"));
            }
        }

        int totalRequired = rows.size();
        int missingCount = totalRequired - completedCount;
        double rate = totalRequired == 0 ? 1.0 : (double) completedCount / totalRequired;

        List<DeliveryCompletenessRow> result = onlyMissing
            ? rows.stream().filter(r -> !r.completed()).toList()
            : rows;

        return new DeliveryCompletenessResponse(
            projectId, normalizedView, normalizedTarget,
            true, List.of(), totalRequired, completedCount, missingCount, rate, result);
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) return "SECTION";
        String normalized = targetType.trim().toUpperCase();
        if (!List.of("SECTION", "OBJECT").contains(normalized)) {
            throw new BusinessException("WORK_TARGET_TYPE_INVALID", "目标类型只能是 SECTION 或 OBJECT", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private DeliveryBindingResponse requireBinding(Long projectId, Long bindingId) {
        return deliveryBindingRepository.findByProjectAndId(projectId, bindingId)
            .orElseThrow(() -> new BusinessException("WORK_BINDING_NOT_FOUND", "交付绑定不存在", HttpStatus.NOT_FOUND));
    }

    private void requireSection(Long projectId, Long sectionNodeId) {
        if (sectionNodeId != null && !deliveryBindingRepository.sectionExists(projectId, sectionNodeId)) {
            throw new BusinessException("MASTERDATA_SECTION_NOT_FOUND", "工程管理部位不存在", HttpStatus.NOT_FOUND);
        }
    }

    private void requireObject(Long projectId, Long managedObjectId) {
        if (managedObjectId != null && !deliveryBindingRepository.objectExists(projectId, managedObjectId)) {
            throw new BusinessException("DATA_OBJECT_NOT_FOUND", "管理对象不存在", HttpStatus.NOT_FOUND);
        }
    }

    private String normalizeViewType(String viewType) {
        String normalized = requireText(viewType, "WORK_VIEW_TYPE_REQUIRED", "交付视图类型不能为空").toUpperCase();
        if (!List.of("DOCUMENT", "DRAWING").contains(normalized)) {
            throw new BusinessException("WORK_VIEW_TYPE_INVALID", "交付视图类型只能是 DOCUMENT 或 DRAWING", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String expectedFileKind(String viewType) {
        return "DRAWING".equals(viewType) ? "DRAWING" : "DOCUMENT";
    }

    private String requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

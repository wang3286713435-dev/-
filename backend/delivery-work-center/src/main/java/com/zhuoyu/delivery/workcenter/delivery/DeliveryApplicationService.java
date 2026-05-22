package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableDefinitionApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableTypeApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.masterdata.section.application.SectionNodeApplicationService;
import com.zhuoyu.delivery.masterdata.template.application.DirectoryTemplateApplicationService;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.preview.FilePreviewPolicy;
import com.zhuoyu.delivery.shared.preview.PreviewDecision;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchBindingRowResult;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchBindingRowStatus;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.CreateDeliveryPackageDraftRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageArchiveItemResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageDraftDetailResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageDraftSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageManifestResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackagePrepareResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageSummaryRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageViewSummary;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryViewResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ExportPrecheckResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ExportPrecheckRow;
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
    private final DeliveryPackageDraftRepository deliveryPackageDraftRepository;
    private final AuditLogApplicationService auditLogApplicationService;
    private final SectionNodeApplicationService sectionNodeApplicationService;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final DeliverableDefinitionApplicationService definitionApplicationService;
    private final DeliverableTypeApplicationService typeApplicationService;
    private final DirectoryTemplateApplicationService templateApplicationService;

    public DeliveryApplicationService(
        DeliveryBindingRepository deliveryBindingRepository,
        DeliveryPackageDraftRepository deliveryPackageDraftRepository,
        AuditLogApplicationService auditLogApplicationService,
        SectionNodeApplicationService sectionNodeApplicationService,
        NodeTypeApplicationService nodeTypeApplicationService,
        DeliverableDefinitionApplicationService definitionApplicationService,
        DeliverableTypeApplicationService typeApplicationService,
        DirectoryTemplateApplicationService templateApplicationService
    ) {
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.deliveryPackageDraftRepository = deliveryPackageDraftRepository;
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
                false, issues, 0, 0, 0, 0, 0, 0, 0, 0,
                0.0, 0.0, "COMPLETE_STANDARD", "先补齐工程主数据和交付物标准，再生成应交项。", List.of());
        }

        // required rows: section/object × deliverable type cross product
        List<DeliveryCompletenessRow> required = deliveryBindingRepository.findRequiredDeliverables(
            projectId, fileKind, normalizedTarget, normalizedView);

        if (required.isEmpty()) {
            return new DeliveryCompletenessResponse(
                projectId, normalizedView, normalizedTarget,
                true, List.of(), 0, 0, 0, 0, 0, 0, 0, 0,
                1.0, 1.0, "DEFINE_DELIVERABLES", "当前视图没有应交项，请确认交付物标准是否覆盖文档/图纸要求。", List.of());
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
        int draftCount = (int) rows.stream().filter(row -> "DRAFT".equals(row.reviewStatus())).count();
        int pendingReviewCount = (int) rows.stream().filter(row -> "PENDING".equals(row.reviewStatus())).count();
        int approvedCount = (int) rows.stream().filter(row -> "APPROVED".equals(row.reviewStatus())).count();
        int rejectedCount = (int) rows.stream().filter(row -> "REJECTED".equals(row.reviewStatus())).count();
        int reviewReadyCount = approvedCount;
        double rate = totalRequired == 0 ? 1.0 : (double) completedCount / totalRequired;
        double approvedRate = totalRequired == 0 ? 1.0 : (double) approvedCount / totalRequired;
        NextAction nextAction = deliveryNextAction(totalRequired, missingCount, draftCount, pendingReviewCount, rejectedCount, approvedCount);

        List<DeliveryCompletenessRow> result = onlyMissing
            ? rows.stream().filter(r -> !r.completed()).toList()
            : rows;

        return new DeliveryCompletenessResponse(
            projectId, normalizedView, normalizedTarget,
            true, List.of(), totalRequired, completedCount, missingCount,
            draftCount, pendingReviewCount, approvedCount, rejectedCount, reviewReadyCount,
            rate, approvedRate, nextAction.code(), nextAction.text(), result);
    }

    private NextAction deliveryNextAction(
        int totalRequired,
        int missingCount,
        int draftCount,
        int pendingReviewCount,
        int rejectedCount,
        int approvedCount
    ) {
        if (totalRequired <= 0) {
            return new NextAction("DEFINE_DELIVERABLES", "当前视图没有应交项，请确认交付物标准是否覆盖文档/图纸要求。");
        }
        if (rejectedCount > 0) {
            return new NextAction("HANDLE_RECTIFICATION", "存在已驳回资料，请处理整改后复审或重新补交。");
        }
        if (draftCount > 0) {
            return new NextAction("SUBMIT_REVIEW", "已有补交文件处于草稿状态，请提交审核。");
        }
        if (pendingReviewCount > 0) {
            return new NextAction("REVIEW_PENDING", "已有资料待审核，请审核通过或驳回并生成整改。");
        }
        if (missingCount > 0) {
            return new NextAction("BIND_MISSING_FILES", "先从缺失项中选择项目资产目录里的文件完成补交。");
        }
        if (approvedCount >= totalRequired) {
            return new NextAction("EXPORT_PRECHECK", "应交项已通过审核，可以执行导出预检查。");
        }
        return new NextAction("REVIEW_PENDING", "请继续检查审核状态并刷新交付准备视图。");
    }

    // ---- batch binding ----

    @Transactional
    public BatchDeliveryBindingResponse createBatchBinding(Long userId, Long projectId, BatchDeliveryBindingRequest request) {
        String viewType = normalizeViewType(request.viewType());
        String expectedFileKind = expectedFileKind(viewType);
        if (!deliveryBindingRepository.deliverableTypeExists(projectId, request.deliverableTypeId(), expectedFileKind)) {
            throw new BusinessException("WORK_DELIVERABLE_TYPE_NOT_FOUND", "交付物类型不存在或文件类型不匹配", HttpStatus.NOT_FOUND);
        }
        requireSection(projectId, request.sectionNodeId());
        requireObject(projectId, request.managedObjectId());
        if (request.sectionNodeId() == null && request.managedObjectId() == null) {
            throw new BusinessException("WORK_BINDING_TARGET_REQUIRED", "交付绑定必须选择部位或管理对象", HttpStatus.BAD_REQUEST);
        }

        String bindingStatus = defaultString(request.bindingStatus(), "BOUND");
        String reviewStatus = defaultString(request.reviewStatus(), "PENDING");
        String remark = blankToNull(request.remark());

        List<DeliveryBindingResponse> createdBindings = new ArrayList<>();
        List<BatchBindingRowResult> results = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Long fileResourceId : request.fileResourceIds()) {
            try {
                if (!deliveryBindingRepository.fileExists(projectId, fileResourceId, expectedFileKind)) {
                    failedCount++;
                    results.add(new BatchBindingRowResult(fileResourceId, BatchBindingRowStatus.FAILED, null,
                        "文件不存在、类型不匹配或尚未处理完成"));
                    continue;
                }
                if (deliveryBindingRepository.bindingExists(projectId, viewType, request.sectionNodeId(),
                        request.managedObjectId(), request.deliverableTypeId(), fileResourceId)) {
                    Long existingId = deliveryBindingRepository.findExistingBindingId(projectId, viewType,
                        request.sectionNodeId(), request.managedObjectId(), request.deliverableTypeId(), fileResourceId);
                    skippedCount++;
                    results.add(new BatchBindingRowResult(fileResourceId, BatchBindingRowStatus.SKIPPED, existingId,
                        "该文件已挂接到当前交付目标"));
                    continue;
                }
                Long bindingId = deliveryBindingRepository.insert(
                    projectId, viewType,
                    request.sectionNodeId(), request.managedObjectId(),
                    request.deliverableTypeId(), fileResourceId,
                    bindingStatus, reviewStatus, 0, remark, userId);
                createdCount++;
                var binding = requireBinding(projectId, bindingId);
                createdBindings.add(binding);
                results.add(new BatchBindingRowResult(fileResourceId, BatchBindingRowStatus.CREATED, bindingId, "已挂接"));
            } catch (BusinessException e) {
                failedCount++;
                results.add(new BatchBindingRowResult(fileResourceId, BatchBindingRowStatus.FAILED, null, e.getMessage()));
            }
        }

        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-binding.batch-create", "DELIVERY_BINDING",
            "batch-" + request.fileResourceIds().size(), userId,
            Map.of("viewType", viewType, "requested", request.fileResourceIds().size(),
                "created", createdCount, "skipped", skippedCount, "failed", failedCount));

        return new BatchDeliveryBindingResponse(
            projectId, viewType, request.fileResourceIds().size(),
            createdCount, skippedCount, failedCount, createdBindings, results);
    }

    // ---- delivery package summary ----

    public DeliveryPackageSummaryResponse deliveryPackageSummary(Long projectId, String viewType, String targetType) {
        String normalizedTarget = normalizeTargetType(targetType);
        List<String> viewTypes;
        if (viewType == null || viewType.isBlank()) {
            viewTypes = List.of("DOCUMENT", "DRAWING");
        } else {
            String normalized = normalizeViewType(viewType);
            viewTypes = List.of(normalized);
        }

        List<DeliveryPackageSummaryRow> allRows = new ArrayList<>();
        for (String vt : viewTypes) {
            List<DeliveryPackageSummaryRow> boundRows = deliveryBindingRepository.findPackageSummaryRows(projectId, vt, normalizedTarget);
            allRows.addAll(boundRows);
            // Also include missing required rows
            List<DeliveryCompletenessRow> missingRows = deliveryBindingRepository.findMissingRequiredRows(projectId, vt, normalizedTarget);
            for (var mr : missingRows) {
                allRows.add(new DeliveryPackageSummaryRow(
                    mr.deliverableDefinitionId(), mr.deliverableDefinitionName(),
                    mr.deliverableTypeId(), mr.deliverableTypeName(),
                    mr.targetType(), mr.targetId(), mr.targetName(),
                    null, null, null, mr.fileKind(), null, null, "MISSING"));
            }
        }

        DeliveryPackageViewSummary docSummary = computePackageViewSummary(allRows, "DOCUMENT");
        DeliveryPackageViewSummary drawingSummary = computePackageViewSummary(allRows, "DRAWING");

        return new DeliveryPackageSummaryResponse(projectId, docSummary, drawingSummary, allRows.size(), allRows);
    }

    private DeliveryPackageViewSummary computePackageViewSummary(List<DeliveryPackageSummaryRow> rows, String fileKind) {
        List<DeliveryPackageSummaryRow> kindRows = rows.stream()
            .filter(r -> fileKind.equals(r.fileKind()))
            .toList();
        int totalRequired = kindRows.size();
        int boundCount = (int) kindRows.stream().filter(r -> r.bindingId() != null).count();
        int missingCount = totalRequired - boundCount;
        int pendingReviewCount = (int) kindRows.stream().filter(r -> "PENDING_REVIEW".equals(r.readinessStatus())).count();
        int approvedCount = (int) kindRows.stream().filter(r -> "READY".equals(r.readinessStatus())).count();
        int rejectedCount = (int) kindRows.stream().filter(r -> "REJECTED".equals(r.readinessStatus())).count();
        double rate = totalRequired == 0 ? 1.0 : (double) boundCount / totalRequired;
        int reviewReadyCount = (int) kindRows.stream().filter(r -> "READY".equals(r.readinessStatus())).count();
        return new DeliveryPackageViewSummary(totalRequired, boundCount, missingCount,
            pendingReviewCount, approvedCount, rejectedCount, rate, reviewReadyCount);
    }

    private record NextAction(String code, String text) {
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

    // ---- export precheck ----

    public ExportPrecheckResponse exportPrecheck(Long projectId, String viewType, String targetType) {
        String normalizedTarget = normalizeTargetType(targetType);
        List<String> viewTypes;
        if (viewType == null || viewType.isBlank()) {
            viewTypes = List.of("DOCUMENT", "DRAWING");
        } else {
            viewTypes = List.of(normalizeViewType(viewType));
        }

        List<ExportPrecheckRow> allRows = new ArrayList<>();
        for (String vt : viewTypes) {
            List<ExportPrecheckRow> boundRows = deliveryBindingRepository.findExportPrecheckBoundRows(projectId, vt, normalizedTarget);
            for (ExportPrecheckRow row : boundRows) {
                ExportPrecheckRow enriched = enrichPrecheckRow(row);
                allRows.add(enriched);
            }
            // Also include missing required rows
            List<DeliveryCompletenessRow> missingRows = deliveryBindingRepository.findMissingRequiredRows(projectId, vt, normalizedTarget);
            for (var mr : missingRows) {
                allRows.add(new ExportPrecheckRow(
                    mr.deliverableDefinitionId(), mr.deliverableDefinitionName(),
                    mr.deliverableTypeId(), mr.deliverableTypeName(),
                    mr.targetType(), mr.targetId(), mr.targetName(),
                    (Long) null, (Long) null, (String) null, mr.fileKind(), (String) null, (String) null,
                    (String) null, "MISSING",
                    "UNSUPPORTED", "NONE", "NOT_SUPPORTED", false,
                    false, "缺失文件", "尚未挂接文件，无法判断在线预览能力。", "DANGER",
                    "MISSING", "尚未挂接文件"
                ));
            }
        }

        String effectiveViewType = viewType != null && !viewType.isBlank() ? normalizeViewType(viewType) : "ALL";

        int totalCount = allRows.size();
        int readyCount = (int) allRows.stream().filter(r -> "READY".equals(r.exportStatus())).count();
        int blockedCount = totalCount - readyCount;
        int missingCount = (int) allRows.stream().filter(r -> "MISSING".equals(r.exportStatus())).count();
        int pendingReviewCount = (int) allRows.stream().filter(r -> "REVIEW_REQUIRED".equals(r.exportStatus())).count();
        int rejectedCount = (int) allRows.stream().filter(r -> "REJECTED".equals(r.exportStatus())).count();
        int conversionRequiredCount = (int) allRows.stream().filter(r -> Boolean.TRUE.equals(r.conversionRequired())).count();
        int unsupportedPreviewCount = (int) allRows.stream()
            .filter(r -> r.fileResourceId() != null)
            .filter(r -> "UNSUPPORTED".equals(r.previewStatus()))
            .count();

        return new ExportPrecheckResponse(
            projectId, effectiveViewType, normalizedTarget,
            true, false,
            totalCount, readyCount, blockedCount, missingCount,
            pendingReviewCount, rejectedCount, conversionRequiredCount, unsupportedPreviewCount,
            allRows
        );
    }

    // ---- delivery package draft / archive directory ----

    public DeliveryPackagePrepareResponse prepareDeliveryPackage(Long projectId, String viewType, String targetType) {
        String normalizedTarget = normalizeTargetType(targetType);
        String normalizedView = normalizePackageViewType(viewType);
        ExportPrecheckResponse precheck = exportPrecheck(
            projectId,
            "ALL".equals(normalizedView) ? null : normalizedView,
            normalizedTarget
        );
        List<DeliveryPackageArchiveItemResponse> rows = precheck.rows().stream()
            .map(row -> toArchiveItem(null, row))
            .toList();
        return new DeliveryPackagePrepareResponse(
            projectId,
            normalizedView,
            normalizedTarget,
            true,
            false,
            false,
            precheck.totalCount(),
            precheck.readyCount(),
            precheck.blockedCount(),
            precheck.missingCount(),
            precheck.pendingReviewCount(),
            precheck.rejectedCount(),
            precheck.conversionRequiredCount(),
            precheck.unsupportedPreviewCount(),
            rows
        );
    }

    @Transactional
    public DeliveryPackageDraftDetailResponse createDeliveryPackageDraft(
        Long userId,
        Long projectId,
        CreateDeliveryPackageDraftRequest request
    ) {
        String viewType = request == null ? null : request.viewType();
        String targetType = request == null ? null : request.targetType();
        DeliveryPackagePrepareResponse prepare = prepareDeliveryPackage(projectId, viewType, targetType);
        Long draftId = deliveryPackageDraftRepository.insertDraft(
            projectId,
            prepare.viewType(),
            prepare.targetType(),
            prepare.totalCount(),
            prepare.readyCount(),
            prepare.blockedCount(),
            prepare.missingCount(),
            prepare.pendingReviewCount(),
            prepare.rejectedCount(),
            prepare.conversionRequiredCount(),
            prepare.unsupportedPreviewCount(),
            userId
        );
        deliveryPackageDraftRepository.insertItems(draftId, projectId, prepare.rows());
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-package-draft.create",
            "DELIVERY_PACKAGE_DRAFT", String.valueOf(draftId), userId,
            Map.of(
                "viewType", prepare.viewType(),
                "targetType", prepare.targetType(),
                "dryRun", true,
                "physicalPackageGenerated", false,
                "nasFileCopied", false,
                "totalCount", prepare.totalCount()
            ));
        return getDeliveryPackageDraft(projectId, draftId);
    }

    public List<DeliveryPackageDraftSummaryResponse> listDeliveryPackageDrafts(Long projectId) {
        return deliveryPackageDraftRepository.findDrafts(projectId);
    }

    public DeliveryPackageDraftDetailResponse getDeliveryPackageDraft(Long projectId, Long draftId) {
        DeliveryPackageDraftSummaryResponse summary = requireDeliveryPackageDraft(projectId, draftId);
        return toDraftDetail(summary, deliveryPackageDraftRepository.findItems(projectId, draftId));
    }

    public List<DeliveryPackageArchiveItemResponse> getDeliveryPackageDraftItems(Long projectId, Long draftId) {
        requireDeliveryPackageDraft(projectId, draftId);
        return deliveryPackageDraftRepository.findItems(projectId, draftId);
    }

    @Transactional
    public DeliveryPackageManifestResponse exportDeliveryPackageManifest(Long userId, Long projectId, Long draftId) {
        DeliveryPackageDraftDetailResponse draft = getDeliveryPackageDraft(projectId, draftId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.delivery-package-draft.export-manifest",
            "DELIVERY_PACKAGE_DRAFT", String.valueOf(draftId), userId,
            Map.of(
                "viewType", draft.viewType(),
                "targetType", draft.targetType(),
                "dryRun", true,
                "physicalPackageGenerated", false,
                "nasFileCopied", false,
                "totalCount", draft.totalCount()
            ));
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF')
            .append(csvLine(
                "草案ID", "项目ID", "视图类型", "目标类型", "目标名称",
                "交付定义", "交付物类型", "文件ID", "文件名", "文件类型", "版本",
                "审核状态", "预览状态", "导出状态", "阻塞原因", "档案目录",
                "dryRun", "physicalPackageGenerated", "nasFileCopied"
            ))
            .append('\n');
        for (DeliveryPackageArchiveItemResponse item : draft.rows()) {
            csv.append(csvLine(
                draft.draftId(),
                draft.projectId(),
                draft.viewType(),
                item.targetType(),
                item.targetName(),
                item.deliverableDefinitionName(),
                item.deliverableTypeName(),
                item.fileId(),
                item.fileName(),
                item.fileKind(),
                item.versionNo(),
                item.reviewStatus(),
                item.previewStatus(),
                item.exportStatus(),
                item.blockReason(),
                item.archiveDirectoryPath(),
                draft.dryRun(),
                draft.physicalPackageGenerated(),
                draft.nasFileCopied()
            )).append('\n');
        }
        return new DeliveryPackageManifestResponse(
            projectId,
            draftId,
            "delivery-package-manifest-" + projectId + "-" + draftId + ".csv",
            "text/csv;charset=UTF-8",
            true,
            false,
            false,
            draft.rows().size(),
            csv.toString()
        );
    }

    private DeliveryPackageDraftSummaryResponse requireDeliveryPackageDraft(Long projectId, Long draftId) {
        return deliveryPackageDraftRepository.findDraft(projectId, draftId)
            .orElseThrow(() -> new BusinessException("WORK_DELIVERY_PACKAGE_DRAFT_NOT_FOUND",
                "交付包草案不存在", HttpStatus.NOT_FOUND));
    }

    private DeliveryPackageDraftDetailResponse toDraftDetail(
        DeliveryPackageDraftSummaryResponse summary,
        List<DeliveryPackageArchiveItemResponse> rows
    ) {
        return new DeliveryPackageDraftDetailResponse(
            summary.projectId(),
            summary.draftId(),
            summary.viewType(),
            summary.targetType(),
            summary.totalCount(),
            summary.readyCount(),
            summary.blockedCount(),
            summary.missingCount(),
            summary.pendingReviewCount(),
            summary.rejectedCount(),
            summary.conversionRequiredCount(),
            summary.unsupportedPreviewCount(),
            summary.dryRun(),
            summary.physicalPackageGenerated(),
            summary.nasFileCopied(),
            summary.createdBy(),
            summary.createdAt(),
            rows
        );
    }

    private DeliveryPackageArchiveItemResponse toArchiveItem(Long itemId, ExportPrecheckRow row) {
        return new DeliveryPackageArchiveItemResponse(
            itemId,
            row.targetType(),
            row.targetId(),
            row.targetName(),
            row.deliverableDefinitionId(),
            row.deliverableDefinitionName(),
            row.deliverableTypeId(),
            row.deliverableTypeName(),
            row.bindingId(),
            row.fileResourceId(),
            row.fileName(),
            row.fileKind(),
            row.versionNo(),
            row.reviewStatus(),
            row.previewStatus(),
            row.exportStatus(),
            row.blockReason(),
            archiveDirectoryPath(row)
        );
    }

    private String archiveDirectoryPath(ExportPrecheckRow row) {
        String viewLabel = "DRAWING".equals(row.fileKind()) ? "图纸交付" : "文档交付";
        String targetLabel = "OBJECT".equals(row.targetType()) ? "管理对象" : "工程部位";
        String target = safeArchiveSegment(row.targetName(), "未命名目标");
        String definition = safeArchiveSegment(row.deliverableDefinitionName(), "未命名交付定义");
        String type = safeArchiveSegment(row.deliverableTypeName(), "未命名交付物类型");
        String file = row.fileName() == null || row.fileName().isBlank()
            ? "未挂接文件"
            : safeArchiveSegment(row.fileName(), "文件名已脱敏");
        return "交付档案/" + viewLabel + "/" + targetLabel + "/" + target + "/" + definition + "/" + type + "/" + file;
    }

    private String safeArchiveSegment(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (lower.contains("/volumes")
            || lower.contains("smb://")
            || lower.contains("nas://")
            || lower.contains("storage_path")
            || lower.contains("storage_uri")
            || lower.matches("^[a-z]:\\\\.*")
            || trimmed.contains("/")
            || trimmed.contains("\\")) {
            return fallback;
        }
        String sanitized = trimmed
            .replaceAll("[\\r\\n\\t]+", " ")
            .replaceAll("[<>:\"|?*]+", "-")
            .replaceAll("\\s{2,}", " ")
            .trim();
        if (sanitized.isBlank()) {
            return fallback;
        }
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private String normalizePackageViewType(String viewType) {
        if (viewType == null || viewType.isBlank() || "ALL".equalsIgnoreCase(viewType.trim())) {
            return "ALL";
        }
        return normalizeViewType(viewType);
    }

    private static String csvLine(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i] == null ? "" : values[i].toString();
            if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }

    private ExportPrecheckRow enrichPrecheckRow(ExportPrecheckRow row) {
        String ext = normalizeFileExt(row.fileExt(), row.fileName());
        PreviewDecision pd = FilePreviewPolicy.decide(ext, row.fileKind());
        String readinessStatus = computeExportReadiness(row.reviewStatus(), row.bindingId() != null);
        String exportStatus = computeExportStatus(readinessStatus);
        String blockReason = computeBlockReason(exportStatus, row.reviewStatus(), pd);
        return new ExportPrecheckRow(
            row.deliverableDefinitionId(), row.deliverableDefinitionName(),
            row.deliverableTypeId(), row.deliverableTypeName(),
            row.targetType(), row.targetId(), row.targetName(),
            row.bindingId(), row.fileResourceId(), row.fileName(),
            row.fileKind(), row.versionNo(), row.fileExt(),
            row.reviewStatus(), readinessStatus,
            pd.previewStatus(), pd.previewMode(), pd.conversionStatus(), pd.conversionRequired(),
            pd.downloadOnly(), pd.statusLabel(), pd.actionHint(), pd.riskLevel(),
            exportStatus, blockReason
        );
    }

    private String computeExportReadiness(String reviewStatus, boolean hasBinding) {
        if (!hasBinding) return "MISSING";
        if ("APPROVED".equals(reviewStatus)) return "READY";
        if ("PENDING".equals(reviewStatus) || "DRAFT".equals(reviewStatus)) return "PENDING_REVIEW";
        if ("REJECTED".equals(reviewStatus)) return "REJECTED";
        return "PENDING_REVIEW";
    }

    private String computeExportStatus(String readiness) {
        return switch (readiness) {
            case "READY" -> "READY";
            case "MISSING" -> "MISSING";
            case "PENDING_REVIEW" -> "REVIEW_REQUIRED";
            case "REJECTED" -> "REJECTED";
            default -> "BLOCKED";
        };
    }

    private String computeBlockReason(String exportStatus, String reviewStatus, PreviewDecision pd) {
        if ("READY".equals(exportStatus)) return null;
        if ("MISSING".equals(exportStatus)) return "尚未挂接文件";
        if ("REVIEW_REQUIRED".equals(exportStatus)) return "文件已挂接但尚未通过审核（当前审核状态：" + (reviewStatus != null ? reviewStatus : "未知") + "）";
        if ("REJECTED".equals(exportStatus)) return "审核已驳回，请处理整改后重新提交审核";
        return "未知阻塞原因";
    }

    private String normalizeFileExt(String fileExt, String fileName) {
        if (fileExt != null && !fileExt.isBlank()) {
            String normalized = fileExt.trim().toLowerCase();
            return normalized.startsWith(".") ? normalized : "." + normalized;
        }
        if (fileName != null && fileName.contains(".")) {
            return "." + fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        }
        return "";
    }
}

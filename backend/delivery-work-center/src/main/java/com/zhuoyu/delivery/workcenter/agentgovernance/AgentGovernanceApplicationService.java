package com.zhuoyu.delivery.workcenter.agentgovernance;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.status.application.StandardStatusApplicationService;
import com.zhuoyu.delivery.masterdata.status.dto.StandardStatusResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.preview.FilePreviewPolicy;
import com.zhuoyu.delivery.shared.preview.PreviewDecision;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryApplicationService;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryBindingRepository;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentBindingRecommendation;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentBindingRecommendationRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentBindingRecommendationResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceCandidateFile;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceDeliveryStatus;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceExportPrecheckSummary;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceMissingItem;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceMissingItemsResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceOverviewResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceStandardStatus;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationItem;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationRowResult;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationsRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationsResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessRow;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ExportPrecheckResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AgentGovernanceApplicationService {

    private static final String MODULE_CODE = "work-center";

    private final DeliveryApplicationService deliveryApplicationService;
    private final DeliveryBindingRepository deliveryBindingRepository;
    private final StandardStatusApplicationService standardStatusApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AgentGovernanceApplicationService(
        DeliveryApplicationService deliveryApplicationService,
        DeliveryBindingRepository deliveryBindingRepository,
        StandardStatusApplicationService standardStatusApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.deliveryApplicationService = deliveryApplicationService;
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.standardStatusApplicationService = standardStatusApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public AgentGovernanceOverviewResponse overview(Long projectId) {
        StandardStatusResponse standard = standardStatusApplicationService.getStatus(projectId);
        DeliveryCompletenessResponse document = deliveryApplicationService.deliveryCompleteness(projectId, "DOCUMENT", "SECTION", false);
        DeliveryCompletenessResponse drawing = deliveryApplicationService.deliveryCompleteness(projectId, "DRAWING", "SECTION", false);
        DeliveryPackageSummaryResponse packageSummary = deliveryApplicationService.deliveryPackageSummary(projectId, null, "SECTION");
        ExportPrecheckResponse precheck = deliveryApplicationService.exportPrecheck(projectId, null, "SECTION");

        int pendingReviewCount = safe(packageSummary.documentSummary().pendingReviewCount())
            + safe(packageSummary.drawingSummary().pendingReviewCount());
        int rejectedCount = safe(packageSummary.documentSummary().rejectedCount())
            + safe(packageSummary.drawingSummary().rejectedCount());
        int rectificationPendingCount = deliveryBindingRepository.countOpenRectifications(projectId);
        String packageStatus = safe(precheck.blockedCount()) == 0 ? "READY" : "NEEDS_ACTION";

        List<String> nextActions = nextActions(standard, document, drawing, rectificationPendingCount, precheck);
        return new AgentGovernanceOverviewResponse(
            projectId,
            new AgentGovernanceStandardStatus(
                standard.hasSectionTree(),
                standard.hasNodeTypes(),
                standard.nodeTypesLocked(),
                standard.deliverableStandardReady(),
                standard.sectionNodeCount(),
                standard.nodeTypeCount(),
                standard.deliverableDefinitionCount(),
                standard.deliverableTypeCount(),
                standard.directoryTemplateCount()
            ),
            deliveryStatus("DOCUMENT", document, packageSummary.documentSummary().pendingReviewCount(), packageSummary.documentSummary().rejectedCount()),
            deliveryStatus("DRAWING", drawing, packageSummary.drawingSummary().pendingReviewCount(), packageSummary.drawingSummary().rejectedCount()),
            pendingReviewCount,
            rejectedCount,
            rectificationPendingCount,
            packageStatus,
            new AgentGovernanceExportPrecheckSummary(
                precheck.totalCount(),
                precheck.readyCount(),
                precheck.blockedCount(),
                precheck.missingCount(),
                precheck.pendingReviewCount(),
                precheck.rejectedCount(),
                precheck.conversionRequiredCount(),
                precheck.unsupportedPreviewCount()
            ),
            summaryText(standard, document, drawing, rectificationPendingCount, precheck),
            nextActions
        );
    }

    public AgentGovernanceMissingItemsResponse missingItems(Long projectId, String viewType, String targetType) {
        String normalizedTarget = normalizeTargetType(targetType);
        List<String> viewTypes = viewType == null || viewType.isBlank()
            ? List.of("DOCUMENT", "DRAWING")
            : List.of(normalizeViewType(viewType));
        List<AgentGovernanceMissingItem> rows = new ArrayList<>();
        for (String vt : viewTypes) {
            rows.addAll(missingItemsForView(projectId, vt, normalizedTarget));
        }
        return new AgentGovernanceMissingItemsResponse(projectId, normalizedTarget, rows.size(), rows);
    }

    public AgentBindingRecommendationResponse recommendBindings(Long userId, Long projectId, AgentBindingRecommendationRequest request) {
        String viewType = normalizeViewType(request == null ? null : request.viewType());
        String targetType = normalizeTargetType(request == null ? null : request.targetType());
        int limitPerMissingItem = request == null || request.limitPerMissingItem() == null
            ? 3
            : Math.min(Math.max(1, request.limitPerMissingItem()), 5);

        List<AgentGovernanceMissingItem> missingItems = missingItemsForView(projectId, viewType, targetType);
        List<AgentGovernanceCandidateFile> candidates = deliveryBindingRepository.findAgentCandidateFiles(
            projectId, expectedFileKind(viewType), 200);
        Set<String> occupiedFileKeys = new LinkedHashSet<>(
            deliveryBindingRepository.findBoundDeliverableTypeFileKeys(projectId, viewType));

        List<AgentBindingRecommendation> rows = new ArrayList<>();
        for (AgentGovernanceMissingItem missing : missingItems) {
            rows.addAll(candidates.stream()
                .filter(file -> !occupiedFileKeys.contains(bindingFileKey(missing.deliverableTypeId(), file.fileResourceId())))
                .map(file -> recommend(projectId, missing, file))
                .sorted(Comparator.comparing(ScoredRecommendation::score).reversed())
                .limit(limitPerMissingItem)
                .map(ScoredRecommendation::recommendation)
                .toList());
        }

        auditLogApplicationService.record(projectId, MODULE_CODE, "work.agent-governance.recommend", "PROJECT",
            String.valueOf(projectId), userId,
            Map.of("viewType", viewType, "targetType", targetType, "missingCount", missingItems.size(), "recommendationCount", rows.size()));

        return new AgentBindingRecommendationResponse(projectId, viewType, targetType, rows.size(), rows);
    }

    public ApplyAgentRecommendationsResponse applyRecommendations(Long userId, Long projectId, ApplyAgentRecommendationsRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessException("WORK_AGENT_GOVERNANCE_CONFIRM_REQUIRED",
                "必须由用户勾选并确认后，平台才能应用推荐挂接", HttpStatus.BAD_REQUEST);
        }
        String viewType = normalizeViewType(request.viewType());
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("WORK_AGENT_GOVERNANCE_ITEMS_REQUIRED",
                "请至少选择一条推荐项", HttpStatus.BAD_REQUEST);
        }

        Map<GroupKey, List<ApplyAgentRecommendationItem>> groups = new LinkedHashMap<>();
        for (ApplyAgentRecommendationItem item : request.items()) {
            String targetType = normalizeTargetType(item.targetType() == null ? request.targetType() : item.targetType());
            if (item.targetId() == null) {
                throw new BusinessException("WORK_AGENT_GOVERNANCE_TARGET_REQUIRED",
                    "推荐项缺少挂接目标", HttpStatus.BAD_REQUEST);
            }
            GroupKey key = new GroupKey(targetType, item.targetId(), item.deliverableTypeId());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }

        List<ApplyAgentRecommendationRowResult> results = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (var entry : groups.entrySet()) {
            GroupKey key = entry.getKey();
            List<ApplyAgentRecommendationItem> items = entry.getValue();
            List<Long> fileIds = items.stream().map(ApplyAgentRecommendationItem::fileResourceId).toList();
            try {
                BatchDeliveryBindingResponse batch = deliveryApplicationService.createBatchBinding(
                    userId,
                    projectId,
                    new BatchDeliveryBindingRequest(
                        viewType,
                        "SECTION".equals(key.targetType()) ? key.targetId() : null,
                        "OBJECT".equals(key.targetType()) ? key.targetId() : null,
                        key.deliverableTypeId(),
                        fileIds,
                        "BOUND",
                        "PENDING",
                        "交付治理助手人工确认挂接"
                    )
                );
                created += safe(batch.createdCount());
                skipped += safe(batch.skippedCount());
                failed += safe(batch.failedCount());
                for (var row : batch.results()) {
                    ApplyAgentRecommendationItem item = findItem(items, row.fileResourceId());
                    results.add(new ApplyAgentRecommendationRowResult(
                        item == null ? null : item.recommendationId(),
                        item == null ? null : item.missingItemKey(),
                        row.fileResourceId(),
                        row.bindingId(),
                        row.status().name(),
                        row.message()
                    ));
                }
            } catch (BusinessException e) {
                failed += items.size();
                for (ApplyAgentRecommendationItem item : items) {
                    results.add(new ApplyAgentRecommendationRowResult(
                        item.recommendationId(),
                        item.missingItemKey(),
                        item.fileResourceId(),
                        null,
                        "FAILED",
                        e.getMessage()
                    ));
                }
            } catch (DataAccessException e) {
                failed += items.size();
                for (ApplyAgentRecommendationItem item : items) {
                    results.add(new ApplyAgentRecommendationRowResult(
                        item.recommendationId(),
                        item.missingItemKey(),
                        item.fileResourceId(),
                        null,
                        "FAILED",
                        "挂接写入失败，可能该文件已被同一交付类型占用，请刷新推荐后重试"
                    ));
                }
            }
        }

        auditLogApplicationService.record(projectId, MODULE_CODE, "work.agent-governance.apply", "PROJECT",
            String.valueOf(projectId), userId,
            Map.of("viewType", viewType, "requested", request.items().size(),
                "created", created, "skipped", skipped, "failed", failed));

        return new ApplyAgentRecommendationsResponse(projectId, viewType, request.items().size(), created, skipped, failed, results);
    }

    private AgentGovernanceDeliveryStatus deliveryStatus(
        String viewType,
        DeliveryCompletenessResponse completeness,
        Integer pendingReviewCount,
        Integer rejectedCount
    ) {
        return new AgentGovernanceDeliveryStatus(
            viewType,
            completeness.totalRequired(),
            completeness.completedCount(),
            completeness.missingCount(),
            completeness.completionRate(),
            pendingReviewCount,
            rejectedCount
        );
    }

    private List<AgentGovernanceMissingItem> missingItemsForView(Long projectId, String viewType, String targetType) {
        DeliveryCompletenessResponse completeness = deliveryApplicationService.deliveryCompleteness(projectId, viewType, targetType, true);
        return completeness.rows().stream()
            .map(row -> toMissingItem(viewType, row))
            .toList();
    }

    private AgentGovernanceMissingItem toMissingItem(String viewType, DeliveryCompletenessRow row) {
        String expectedFileKind = expectedFileKind(viewType);
        String key = missingItemKey(viewType, row.targetType(), row.targetId(), row.deliverableTypeId());
        String reason = row.missingReason() == null || row.missingReason().isBlank() ? "尚未挂接文件" : row.missingReason();
        String viewLabel = "DRAWING".equals(viewType) ? "图纸" : "文档";
        String explanation = "%s还缺少“%s / %s”。平台会推荐当前项目内登记为%s的候选文件，用户确认后才会挂接。"
            .formatted(row.targetName(), row.deliverableDefinitionName(), row.deliverableTypeName(), viewLabel);
        return new AgentGovernanceMissingItem(
            key,
            viewType,
            row.targetType(),
            row.targetId(),
            row.targetName(),
            row.deliverableDefinitionId(),
            row.deliverableDefinitionName(),
            row.deliverableTypeId(),
            row.deliverableTypeName(),
            row.fileKind(),
            reason,
            expectedFileKind,
            explanation
        );
    }

    private ScoredRecommendation recommend(Long projectId, AgentGovernanceMissingItem missing, AgentGovernanceCandidateFile file) {
        String fileName = lower(file.fileName());
        String typeName = lower(missing.deliverableTypeName());
        String definitionName = lower(missing.deliverableDefinitionName());
        String targetName = lower(missing.targetName());
        String businessTag = lower(file.businessTag());
        double score = 0.35;
        List<String> reasons = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        if (fileName.contains(typeName) || tokenHit(fileName, typeName)) {
            score += 0.25;
            reasons.add("文件名和交付物类型接近");
        }
        if (fileName.contains(definitionName) || tokenHit(fileName, definitionName)) {
            score += 0.15;
            reasons.add("文件名命中交付定义");
        }
        if (fileName.contains(targetName) || tokenHit(fileName, targetName)) {
            score += 0.15;
            reasons.add("文件名包含目标名称");
        }
        if (businessTag != null
            && ((typeName != null && typeName.contains(businessTag))
                || (definitionName != null && definitionName.contains(businessTag)))) {
            score += 0.10;
            reasons.add("专业或业务标签接近");
        }
        if (file.versionNo() != null && !file.versionNo().isBlank()) {
            score += 0.05;
        } else {
            risks.add("文件缺少版本字段，建议先治理元数据。");
        }
        if ("PROCESSED".equals(file.processStatus())) {
            score += 0.10;
        } else {
            risks.add("文件尚未处理完成，当前不能直接挂接。");
        }
        if (!Boolean.TRUE.equals(file.checksumPresent())) {
            risks.add("文件缺少 checksum，建议补齐后再作为正式交付依据。");
        }

        PreviewDecision preview = FilePreviewPolicy.decide(file.fileExt(), file.fileKind());
        if (Boolean.TRUE.equals(preview.conversionRequired())) {
            risks.add(preview.actionHint());
        }

        String confidence = confidence(score);
        if ("LOW".equals(confidence)) {
            risks.add("命中信息较少，需要人工确认是否确实属于该缺失项。");
        }
        if (reasons.isEmpty()) {
            reasons.add("文件类型符合当前缺失项要求，可作为人工候选。");
        }
        boolean metadataGovernanceRequired = !risks.isEmpty();
        String recommendationId = "REC-" + projectId + "-" + missing.missingItemKey() + "-" + file.fileResourceId();
        AgentBindingRecommendation recommendation = new AgentBindingRecommendation(
            recommendationId,
            missing.missingItemKey(),
            missing.viewType(),
            missing.targetType(),
            missing.targetId(),
            missing.targetName(),
            missing.deliverableTypeId(),
            missing.deliverableTypeName(),
            file.fileResourceId(),
            file.fileName(),
            file.fileKind(),
            file.fileExt(),
            file.versionNo(),
            file.processStatus(),
            preview.previewStatus(),
            preview.statusLabel(),
            String.join("；", reasons),
            confidence,
            dedupe(risks),
            metadataGovernanceRequired
        );
        return new ScoredRecommendation(Math.min(score, 0.95), recommendation);
    }

    private List<String> nextActions(
        StandardStatusResponse standard,
        DeliveryCompletenessResponse document,
        DeliveryCompletenessResponse drawing,
        int rectificationPendingCount,
        ExportPrecheckResponse precheck
    ) {
        List<String> actions = new ArrayList<>();
        if (!Boolean.TRUE.equals(standard.hasSectionTree())) {
            actions.add("先建立部位树，平台才知道资料应该交到哪个楼层或系统。");
        }
        if (!Boolean.TRUE.equals(standard.nodeTypesLocked())) {
            actions.add("先检查并锁定节点类型，避免交付规则继续变化。");
        }
        if (!Boolean.TRUE.equals(standard.deliverableStandardReady())) {
            actions.add("补齐交付物标准和目录模板，再开始批量补交。");
        }
        if (safe(document.missingCount()) > 0) {
            actions.add("查看文档缺失项，生成候选文件推荐。");
        }
        if (safe(drawing.missingCount()) > 0) {
            actions.add("查看图纸缺失项，生成候选文件推荐。");
        }
        if (rectificationPendingCount > 0) {
            actions.add("处理未关闭的整改项，再刷新交付包准备状态。");
        }
        if (safe(precheck.blockedCount()) > 0) {
            actions.add("查看交付包预检查，优先处理阻塞原因。");
        }
        if (actions.isEmpty()) {
            actions.add("当前项目已具备交付包准备条件，可进入导出预检查复核。");
        }
        return actions;
    }

    private String summaryText(
        StandardStatusResponse standard,
        DeliveryCompletenessResponse document,
        DeliveryCompletenessResponse drawing,
        int rectificationPendingCount,
        ExportPrecheckResponse precheck
    ) {
        if (!Boolean.TRUE.equals(standard.hasSectionTree())) {
            return "部位树还没建好，平台还不知道资料应该交到哪个楼层或系统。";
        }
        if (!Boolean.TRUE.equals(standard.nodeTypesLocked())) {
            return "节点类型尚未全部锁定，说明交付规则还可能变化，建议先锁定后再批量挂接。";
        }
        int missing = safe(document.missingCount()) + safe(drawing.missingCount());
        if (missing > 0) {
            return "当前项目还有 " + missing + " 个文档或图纸应交项未挂接，可以先生成候选文件推荐，再由人工确认挂接。";
        }
        if (rectificationPendingCount > 0 || safe(precheck.blockedCount()) > 0) {
            return "资料已基本挂接，但仍有审核、整改或交付包预检查阻塞项需要处理。";
        }
        return "当前项目交付准备状态较完整，可以进入交付包预检查和正式验收判断。";
    }

    private ApplyAgentRecommendationItem findItem(List<ApplyAgentRecommendationItem> items, Long fileResourceId) {
        return items.stream().filter(item -> item.fileResourceId().equals(fileResourceId)).findFirst().orElse(null);
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private String confidence(double score) {
        if (score >= 0.75) return "HIGH";
        if (score >= 0.50) return "MEDIUM";
        return "LOW";
    }

    private boolean tokenHit(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isBlank()) {
            return false;
        }
        for (String token : needle.split("[\\s_\\-./]+")) {
            if (token.length() >= 2 && haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String missingItemKey(String viewType, String targetType, Long targetId, Long deliverableTypeId) {
        return viewType + "-" + targetType + "-" + targetId + "-" + deliverableTypeId;
    }

    private String bindingFileKey(Long deliverableTypeId, Long fileResourceId) {
        return deliverableTypeId + ":" + fileResourceId;
    }

    private String normalizeViewType(String viewType) {
        String normalized = viewType == null || viewType.isBlank() ? "DOCUMENT" : viewType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("DOCUMENT", "DRAWING").contains(normalized)) {
            throw new BusinessException("WORK_VIEW_TYPE_INVALID", "交付视图类型只能是 DOCUMENT 或 DRAWING", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeTargetType(String targetType) {
        String normalized = targetType == null || targetType.isBlank() ? "SECTION" : targetType.trim().toUpperCase(Locale.ROOT);
        if (!List.of("SECTION", "OBJECT").contains(normalized)) {
            throw new BusinessException("WORK_TARGET_TYPE_INVALID", "目标类型只能是 SECTION 或 OBJECT", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String expectedFileKind(String viewType) {
        return "DRAWING".equals(viewType) ? "DRAWING" : "DOCUMENT";
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private record GroupKey(String targetType, Long targetId, Long deliverableTypeId) {
    }

    private record ScoredRecommendation(double score, AgentBindingRecommendation recommendation) {
    }
}

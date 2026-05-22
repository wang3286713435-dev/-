package com.zhuoyu.delivery.visualization.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetQualityApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.StatisticsApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityMetric;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityOverviewResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByDiscipline;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByFileKind;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityStatisticsResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.datasteward.file.FileResourceApplicationService;
import com.zhuoyu.delivery.datasteward.model.ModelIntegrationApplicationService;
import com.zhuoyu.delivery.datasteward.object.ManagedObjectApplicationService;
import com.zhuoyu.delivery.masterdata.section.application.SectionNodeApplicationService;
import com.zhuoyu.delivery.masterdata.section.dto.SectionNodeResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ActivityItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ActivitySummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.CollaborationOperationsSummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.DeliverySummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.DigitalTwinDashboardResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.DistributionItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightPlanResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightStatusResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LocateResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ManagedObjectContextItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ModelSceneItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ModelSummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ModelContextItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ObjectSceneItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ProjectSnapshot;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.QualityMetricItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.QualitySummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.AssetSummary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.SafetyBoundary;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ScanTaskActivityItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.SpaceSummaryItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.SystemSummaryItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.VisualizationContextResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.WorkItemSummary;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryApplicationService;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationResponse;
import com.zhuoyu.delivery.workcenter.rectification.RectificationApplicationService;
import com.zhuoyu.delivery.shared.preview.FilePreviewPolicy;
import com.zhuoyu.delivery.shared.preview.PreviewDecision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisualizationAdapterApplicationService {

    private static final String MODULE_CODE = "visualization-adapter";
    private static final List<String> LIGHTWEIGHT_SUPPORTED_OPERATIONS = List.of(
        "VIEW_LIGHTWEIGHT_STATUS",
        "VIEW_LIGHTWEIGHT_PLAN",
        "VIEW_MODEL_METADATA"
    );
    private static final List<String> LIGHTWEIGHT_FORBIDDEN_OPERATIONS = List.of(
        "CREATE_REAL_CONVERSION_TASK",
        "READ_MODEL_BODY",
        "TOUCH_NAS_FILE",
        "WRITE_LIGHTWEIGHT_CACHE",
        "OPEN_REAL_3D_VIEWER",
        "WRITE_HERMES_MEMORY"
    );

    private final ModelIntegrationApplicationService modelIntegrationApplicationService;
    private final ManagedObjectApplicationService managedObjectApplicationService;
    private final FileResourceApplicationService fileResourceApplicationService;
    private final AssetApplicationService assetApplicationService;
    private final StatisticsApplicationService statisticsApplicationService;
    private final AssetQualityApplicationService assetQualityApplicationService;
    private final DeliveryApplicationService deliveryApplicationService;
    private final RectificationApplicationService rectificationApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final SectionNodeApplicationService sectionNodeApplicationService;

    public VisualizationAdapterApplicationService(
        ModelIntegrationApplicationService modelIntegrationApplicationService,
        ManagedObjectApplicationService managedObjectApplicationService,
        FileResourceApplicationService fileResourceApplicationService,
        AssetApplicationService assetApplicationService,
        StatisticsApplicationService statisticsApplicationService,
        AssetQualityApplicationService assetQualityApplicationService,
        DeliveryApplicationService deliveryApplicationService,
        RectificationApplicationService rectificationApplicationService,
        AuditLogApplicationService auditLogApplicationService,
        SectionNodeApplicationService sectionNodeApplicationService
    ) {
        this.modelIntegrationApplicationService = modelIntegrationApplicationService;
        this.managedObjectApplicationService = managedObjectApplicationService;
        this.fileResourceApplicationService = fileResourceApplicationService;
        this.assetApplicationService = assetApplicationService;
        this.statisticsApplicationService = statisticsApplicationService;
        this.assetQualityApplicationService = assetQualityApplicationService;
        this.deliveryApplicationService = deliveryApplicationService;
        this.rectificationApplicationService = rectificationApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
        this.sectionNodeApplicationService = sectionNodeApplicationService;
    }

    public VisualizationContextResponse context(Long projectId) {
        List<ModelIntegrationResponse> models = modelIntegrationApplicationService.list(projectId);
        List<ManagedObjectResponse> objects = managedObjectApplicationService.list(projectId);
        List<ModelContextItem> modelItems = models.stream()
            .map(model -> new ModelContextItem(model.id(), model.name(), model.versionNo(), model.status(), model.componentCount()))
            .toList();
        List<ManagedObjectContextItem> objectItems = objects.stream()
            .map(object -> new ManagedObjectContextItem(object.id(), object.code(), object.name(), object.objectType(), object.sectionNodeId()))
            .toList();
        int published = (int) models.stream().filter(model -> "PUBLISHED".equals(model.status())).count();
        return new VisualizationContextResponse(projectId, published, objects.size(), modelItems, objectItems);
    }

    public DigitalTwinDashboardResponse digitalTwinDashboard(Long userId, Long projectId) {
        AssetProjectResponse project = assetApplicationService.listProjects(userId, null).stream()
            .filter(item -> item.projectId().equals(projectId))
            .findFirst()
            .orElse(null);
        CapacityStatisticsResponse assetStatistics = statisticsApplicationService.getStatistics(userId, projectId);
        AssetQualityOverviewResponse qualityOverview = assetQualityApplicationService.getOverview(userId, projectId, null);
        DeliveryCompletenessResponse documentCompleteness = deliveryApplicationService.deliveryCompleteness(
            projectId, "DOCUMENT", "SECTION", false);
        DeliveryCompletenessResponse drawingCompleteness = deliveryApplicationService.deliveryCompleteness(
            projectId, "DRAWING", "SECTION", false);
        List<RectificationResponse> rectifications = rectificationApplicationService.list(projectId, null);
        List<ModelIntegrationResponse> models = modelIntegrationApplicationService.list(projectId);
        List<ManagedObjectResponse> objects = managedObjectApplicationService.list(projectId);
        List<SectionNodeResponse> sections = sectionNodeApplicationService.tree(projectId);
        List<ScanTaskResponse> scans = assetApplicationService.listScansForUser(userId).stream()
            .filter(task -> projectId.equals(task.projectId()))
            .limit(6)
            .toList();
        DeliverySummary deliverySummary = toDeliverySummary(documentCompleteness, drawingCompleteness, rectifications);
        QualitySummary qualitySummary = toQualitySummary(qualityOverview);
        ModelSummary modelSummary = toModelSummary(models, objects);
        ActivitySummary activitySummary = toActivitySummary(qualityOverview, scans);

        return new DigitalTwinDashboardResponse(
            toProjectSnapshot(projectId, project),
            toAssetSummary(assetStatistics),
            deliverySummary,
            qualitySummary,
            modelSummary,
            activitySummary,
            toOperationsSummary(objects, sections, rectifications, scans),
            defaultSafetyBoundary()
        );
    }

    public LightweightStatusResponse lightweightStatus(Long projectId, Long integrationId) {
        ModelIntegrationResponse integration = modelIntegrationApplicationService.requireIntegration(projectId, integrationId);
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(projectId, integration.modelFileId());
        String format = modelFormat(modelFile.originalName());
        return new LightweightStatusResponse(
            projectId,
            integrationId,
            integration.modelFileId(),
            modelFile.originalName(),
            format,
            integration.status(),
            "METADATA_ADAPTER",
            false,
            "NOT_CONNECTED",
            false,
            "NOT_CREATED",
            true,
            "NOT_STARTED",
            "BIM_LIGHTWEIGHT",
            "元数据适配",
            "当前为元数据适配，未执行真实轻量化转换；接入真实 BIM 引擎后才能打开 3D 预览。",
            "未配置真实 BIM 轻量化引擎适配器",
            LIGHTWEIGHT_SUPPORTED_OPERATIONS,
            LIGHTWEIGHT_FORBIDDEN_OPERATIONS
        );
    }

    public LightweightPlanResponse lightweightPlan(Long projectId, Long integrationId) {
        ModelIntegrationResponse integration = modelIntegrationApplicationService.requireIntegration(projectId, integrationId);
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(projectId, integration.modelFileId());
        String format = modelFormat(modelFile.originalName());
        return new LightweightPlanResponse(
            projectId,
            integrationId,
            integration.modelFileId(),
            modelFile.originalName(),
            format,
            "METADATA_ADAPTER",
            true,
            false,
            true,
            false,
            false,
            false,
            List.of(
                "配置真实 BIM 引擎适配器和租户级连接参数",
                "确认模型格式支持矩阵和版本兼容规则",
                "定义轻量化输出存储策略与权限隔离",
                "建立转换任务、重试、超时和失败回滚策略",
                "补齐权限审计、路径脱敏和预览授权校验"
            ),
            List.of(
                "引擎适配器连通性校验",
                "模型格式白名单与风险提示",
                "轻量化产物登记与访问票据联动",
                "转换任务状态流转和失败告警",
                "前端 3D Viewer 真实入口启用"
            ),
            List.of(
                "当前返回仅用于准备检查，不代表真实预览可用",
                "本接口不读取模型正文，不解析构件，不生成轻量化产物",
                "未接入真实引擎前，禁止把适配状态包装为可预览"
            ),
            LIGHTWEIGHT_SUPPORTED_OPERATIONS,
            LIGHTWEIGHT_FORBIDDEN_OPERATIONS
        );
    }

    @Transactional
    public ModelIntegrationResponse publishModel(Long userId, Long projectId, Long integrationId) {
        ModelIntegrationResponse response = modelIntegrationApplicationService.publish(userId, projectId, integrationId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.model.publish", "MODEL_INTEGRATION",
            String.valueOf(integrationId), userId, Map.of("adapter", "mock"));
        return response;
    }

    public LocateResponse locate(Long userId, Long projectId, Long managedObjectId) {
        managedObjectApplicationService.requireObject(projectId, managedObjectId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.object.locate", "MANAGED_OBJECT",
            String.valueOf(managedObjectId), userId, Map.of("adapterCommand", "locate-component"));
        return new LocateResponse(projectId, managedObjectId, "locate-component", "fit-selected-object", "READY");
    }

    public HighlightResponse highlight(Long userId, Long projectId, Long managedObjectId, HighlightRequest request) {
        managedObjectApplicationService.requireObject(projectId, managedObjectId);
        String color = request == null || request.color() == null || request.color().isBlank() ? "#2563eb" : request.color().trim();
        Integer duration = request == null || request.durationSeconds() == null ? 5 : request.durationSeconds();
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.object.highlight", "MANAGED_OBJECT",
            String.valueOf(managedObjectId), userId, Map.of("color", color));
        return new HighlightResponse(projectId, managedObjectId, color, duration, "highlight-component", "READY");
    }

    public LinkageResponse syncLinkage(Long userId, Long projectId, LinkageRequest request) {
        managedObjectApplicationService.requireObject(projectId, request.managedObjectId());
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.linkage.sync", "MANAGED_OBJECT",
            String.valueOf(request.managedObjectId()), userId, Map.of("action", request.action()));
        return new LinkageResponse(projectId, request.fileResourceId(), request.managedObjectId(),
            request.action(), "sync-drawing-model", "READY");
    }

    public ContextInjectResponse injectContext(Long userId, Long projectId, ContextInjectRequest request) {
        Long managedObjectId = request == null ? null : request.managedObjectId();
        if (managedObjectId != null) {
            managedObjectApplicationService.requireObject(projectId, managedObjectId);
        }
        Long sectionNodeId = request == null ? null : request.sectionNodeId();
        String source = request == null || request.source() == null || request.source().isBlank() ? "WORK_CENTER" : request.source().trim();
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.context.inject", "PROJECT",
            String.valueOf(projectId), userId, Map.of("source", source));
        return new ContextInjectResponse(projectId, sectionNodeId, managedObjectId, source, "inject-project-context", "READY");
    }

    private String modelFormat(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "UNKNOWN";
        }
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return "UNKNOWN";
        }
        return originalName.substring(index + 1).toUpperCase();
    }

    private ProjectSnapshot toProjectSnapshot(Long projectId, AssetProjectResponse project) {
        if (project == null) {
            return new ProjectSnapshot(projectId, "PROJECT-" + projectId, "项目 " + projectId,
                null, null, null, null, null);
        }
        return new ProjectSnapshot(
            project.projectId(),
            project.code(),
            project.name(),
            project.industryType(),
            project.projectStage(),
            project.projectManagerName(),
            project.assetStatus(),
            project.onboardingStatus()
        );
    }

    private AssetSummary toAssetSummary(CapacityStatisticsResponse statistics) {
        return new AssetSummary(
            statistics.projectCount(),
            statistics.fileCount(),
            statistics.modelFileCount(),
            statistics.drawingFileCount(),
            statistics.totalSizeBytes(),
            statistics.byFileKind().stream().map(this::toDistribution).toList(),
            statistics.byDiscipline().stream().map(this::toDistribution).toList()
        );
    }

    private DistributionItem toDistribution(CapacityByFileKind item) {
        String code = item.fileKind() == null || item.fileKind().isBlank() ? "UNKNOWN" : item.fileKind();
        return new DistributionItem(code, fileKindLabel(code), item.fileCount(), item.totalSizeBytes());
    }

    private DistributionItem toDistribution(CapacityByDiscipline item) {
        String code = item.discipline() == null || item.discipline().isBlank() ? "UNKNOWN" : item.discipline();
        return new DistributionItem(code, disciplineLabel(code), item.fileCount(), item.totalSizeBytes());
    }

    private DeliverySummary toDeliverySummary(
        DeliveryCompletenessResponse document,
        DeliveryCompletenessResponse drawing,
        List<RectificationResponse> rectifications
    ) {
        int totalRequired = intValue(document.totalRequired()) + intValue(drawing.totalRequired());
        int completedCount = intValue(document.completedCount()) + intValue(drawing.completedCount());
        int missingCount = intValue(document.missingCount()) + intValue(drawing.missingCount());
        int draftCount = intValue(document.draftCount()) + intValue(drawing.draftCount());
        int pendingReviewCount = intValue(document.pendingReviewCount()) + intValue(drawing.pendingReviewCount());
        int approvedCount = intValue(document.approvedCount()) + intValue(drawing.approvedCount());
        int rejectedCount = intValue(document.rejectedCount()) + intValue(drawing.rejectedCount());
        int openRectificationCount = (int) rectifications.stream()
            .filter(item -> item.status() == null || !"CLOSED".equals(item.status()))
            .count();
        double completionRate = totalRequired == 0 ? 0.0 : (double) completedCount / totalRequired;
        double approvedRate = totalRequired == 0 ? 0.0 : (double) approvedCount / totalRequired;
        List<String> readinessIssues = List.of(document, drawing).stream()
            .flatMap(item -> item.readinessIssues().stream())
            .distinct()
            .toList();
        String nextActionCode = chooseNextActionCode(document, drawing, openRectificationCount);
        String nextActionText = chooseNextActionText(document, drawing, openRectificationCount);
        return new DeliverySummary(
            Boolean.TRUE.equals(document.standardReady()) && Boolean.TRUE.equals(drawing.standardReady()),
            totalRequired,
            completedCount,
            missingCount,
            draftCount,
            pendingReviewCount,
            approvedCount,
            rejectedCount,
            openRectificationCount,
            completionRate,
            approvedRate,
            nextActionCode,
            nextActionText,
            readinessIssues
        );
    }

    private QualitySummary toQualitySummary(AssetQualityOverviewResponse overview) {
        List<QualityMetricItem> metrics = overview.metrics().stream()
            .map(this::toQualityMetric)
            .toList();
        return new QualitySummary(
            overview.riskSignalCount(),
            overview.pendingReviewCount(),
            overview.failedScanCount(),
            overview.runningScanCount(),
            overview.missingChecksumCount(),
            overview.missingDisciplineCount(),
            overview.missingVersionCount(),
            overview.zeroSizeFileCount(),
            metrics
        );
    }

    private QualityMetricItem toQualityMetric(AssetQualityMetric metric) {
        if ("MISSING_STORAGE_PATH".equals(metric.code())) {
            return new QualityMetricItem("MISSING_TRACEABILITY", "溯源信息待完善", metric.severity(), metric.count());
        }
        return new QualityMetricItem(metric.code(), metric.label(), metric.severity(), metric.count());
    }

    private ModelSummary toModelSummary(List<ModelIntegrationResponse> models, List<ManagedObjectResponse> objects) {
        int published = (int) models.stream().filter(model -> "PUBLISHED".equals(model.status())).count();
        String lightweightStatus = models.isEmpty() ? "NOT_STARTED" : "NOT_CONNECTED";
        String statusLabel = models.isEmpty() ? "暂无模型集成" : "真实 Viewer 未接入";
        String actionHint = models.isEmpty()
            ? "当前项目还没有模型集成元数据，可先在数据管家登记模型。"
            : "当前只展示模型元数据和适配状态，未执行真实轻量化转换。";
        return new ModelSummary(
            models.size(),
            published,
            objects.size(),
            "METADATA_ADAPTER",
            false,
            lightweightStatus,
            false,
            statusLabel,
            actionHint,
            models.stream()
                .map(this::toModelSceneItem)
                .toList(),
            objects.stream()
                .map(object -> new ObjectSceneItem(
                    object.id(),
                    object.code(),
                    object.name(),
                    object.objectType(),
                    object.sectionNodeId(),
                    object.discipline(),
                    object.status()
                ))
                .toList()
        );
    }

    private CollaborationOperationsSummary toOperationsSummary(
        List<ManagedObjectResponse> objects,
        List<SectionNodeResponse> sectionTree,
        List<RectificationResponse> rectifications,
        List<ScanTaskResponse> scans
    ) {
        List<SectionNodeResponse> sections = flattenSections(sectionTree);
        List<ManagedObjectResponse> activeObjects = objects.stream()
            .filter(item -> item.status() == null || "ACTIVE".equalsIgnoreCase(item.status()))
            .toList();
        int equipmentCount = countObjects(activeObjects, "EQUIPMENT");
        int spaceObjectCount = countObjects(activeObjects, "SPACE");
        int systemObjectCount = countObjects(activeObjects, "SYSTEM");
        int componentPlaceholderCount = countObjects(activeObjects, "COMPONENT_PLACEHOLDER");
        int linkedObjectCount = (int) activeObjects.stream().filter(item -> item.sectionNodeId() != null).count();
        return new CollaborationOperationsSummary(
            equipmentCount,
            spaceObjectCount,
            systemObjectCount,
            componentPlaceholderCount,
            sections.size(),
            linkedObjectCount,
            Math.max(0, activeObjects.size() - linkedObjectCount),
            objectTypeDistribution(activeObjects),
            objectDisciplineDistribution(activeObjects),
            systemItems(activeObjects),
            spaceItems(sections, activeObjects),
            workItems(rectifications, scans),
            List.of(
                "未接入真实设备在线率、传感器实时点位和环境监测",
                "未接入真实水电气冷热能耗采集",
                "未接入真实巡检保养工单系统"
            )
        );
    }

    private List<SectionNodeResponse> flattenSections(List<SectionNodeResponse> roots) {
        List<SectionNodeResponse> result = new ArrayList<>();
        appendSections(roots, result);
        return result;
    }

    private void appendSections(List<SectionNodeResponse> nodes, List<SectionNodeResponse> result) {
        if (nodes == null) {
            return;
        }
        nodes.forEach(node -> {
            result.add(node);
            appendSections(node.children(), result);
        });
    }

    private int countObjects(List<ManagedObjectResponse> objects, String objectType) {
        return (int) objects.stream().filter(item -> objectType.equalsIgnoreCase(defaultText(item.objectType(), "UNKNOWN"))).count();
    }

    private List<DistributionItem> objectTypeDistribution(List<ManagedObjectResponse> objects) {
        return objects.stream()
            .collect(Collectors.groupingBy(item -> defaultText(item.objectType(), "UNKNOWN"), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(item -> new DistributionItem(item.getKey(), objectTypeLabel(item.getKey()), item.getValue().intValue(), 0L))
            .toList();
    }

    private List<DistributionItem> objectDisciplineDistribution(List<ManagedObjectResponse> objects) {
        return objects.stream()
            .collect(Collectors.groupingBy(item -> defaultText(item.discipline(), "UNSPECIFIED"), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(item -> new DistributionItem(item.getKey(), disciplineLabel(item.getKey()), item.getValue().intValue(), 0L))
            .toList();
    }

    private List<SystemSummaryItem> systemItems(List<ManagedObjectResponse> objects) {
        List<ManagedObjectResponse> activeEquipment = objects.stream()
            .filter(item -> "EQUIPMENT".equalsIgnoreCase(defaultText(item.objectType(), "")))
            .toList();
        return objects.stream()
            .filter(item -> "SYSTEM".equalsIgnoreCase(defaultText(item.objectType(), "")))
            .sorted(Comparator.comparing(ManagedObjectResponse::id))
            .map(system -> {
                String discipline = defaultText(system.discipline(), "");
                int linkedEquipment = discipline.isBlank()
                    ? 0
                    : (int) activeEquipment.stream()
                        .filter(item -> discipline.equalsIgnoreCase(defaultText(item.discipline(), "")))
                        .count();
                return new SystemSummaryItem(
                    system.id(),
                    system.code(),
                    system.name(),
                    discipline,
                    linkedEquipment,
                    system.status(),
                    "平台管理对象"
                );
            })
            .toList();
    }

    private List<SpaceSummaryItem> spaceItems(List<SectionNodeResponse> sections, List<ManagedObjectResponse> objects) {
        Map<Long, List<ManagedObjectResponse>> objectsBySection = objects.stream()
            .filter(item -> item.sectionNodeId() != null)
            .collect(Collectors.groupingBy(ManagedObjectResponse::sectionNodeId));
        return sections.stream()
            .limit(80)
            .map(section -> {
                List<ManagedObjectResponse> linkedObjects = objectsBySection.getOrDefault(section.id(), List.of());
                return new SpaceSummaryItem(
                    section.id(),
                    section.parentId(),
                    section.code(),
                    section.name(),
                    section.level(),
                    section.path(),
                    linkedObjects.size(),
                    countObjects(linkedObjects, "EQUIPMENT"),
                    countObjects(linkedObjects, "SYSTEM")
                );
            })
            .toList();
    }

    private List<WorkItemSummary> workItems(List<RectificationResponse> rectifications, List<ScanTaskResponse> scans) {
        List<WorkItemSummary> items = new ArrayList<>();
        rectifications.stream()
            .filter(item -> item.status() == null || !"CLOSED".equalsIgnoreCase(item.status()))
            .limit(8)
            .map(item -> new WorkItemSummary(
                "rectification-" + item.id(),
                "整改",
                item.title(),
                item.status(),
                "交付闭环",
                item.updatedAt() == null ? null : item.updatedAt().toString()
            ))
            .forEach(items::add);
        scans.stream()
            .filter(item -> item.failedCount() != null && item.failedCount() > 0)
            .limit(6)
            .map(item -> new WorkItemSummary(
                "scan-" + item.id(),
                "扫描治理",
                "扫描任务存在失败项",
                item.status(),
                item.projectCode(),
                item.updatedAt() == null ? null : item.updatedAt().toString()
            ))
            .forEach(items::add);
        return items.stream()
            .filter(Objects::nonNull)
            .limit(12)
            .toList();
    }

    private ModelSceneItem toModelSceneItem(ModelIntegrationResponse model) {
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(model.projectId(), model.modelFileId());
        String modelFormat = modelFormat(modelFile.originalName());
        PreviewDecision preview = FilePreviewPolicy.decide("." + modelFormat.toLowerCase(Locale.ROOT), modelFile.fileKind());
        return new ModelSceneItem(
            model.id(),
            model.modelFileId(),
            model.name(),
            modelFormat,
            model.versionNo(),
            model.status(),
            model.componentCount(),
            preview.previewStatus(),
            preview.previewMode(),
            preview.conversionStatus(),
            false,
            modelStatusLabel(modelFormat, preview),
            modelActionHint(modelFormat, preview)
        );
    }

    private String modelStatusLabel(String modelFormat, PreviewDecision preview) {
        if ("RVT".equals(modelFormat)) {
            return "RVT 待轻量化";
        }
        return preview.statusLabel();
    }

    private String modelActionHint(String modelFormat, PreviewDecision preview) {
        if ("RVT".equals(modelFormat)) {
            return "RVT 原始文件已入库，本轮只展示模型状态；需完成 IFC/XKT/Fragments 等轻量化产物后才能在线预览。";
        }
        return preview.actionHint();
    }

    private ActivitySummary toActivitySummary(AssetQualityOverviewResponse overview, List<ScanTaskResponse> scans) {
        return new ActivitySummary(
            overview.latestAssetUpdatedAt(),
            overview.latestEventAt(),
            overview.recentEvents().stream().limit(6).map(this::toActivity).toList(),
            scans.stream().map(this::toScanActivity).toList()
        );
    }

    private ActivityItem toActivity(EventResponse event) {
        return new ActivityItem(event.id(), event.projectId(), event.actionCode(), event.summary(), event.createdAt());
    }

    private ScanTaskActivityItem toScanActivity(ScanTaskResponse task) {
        return new ScanTaskActivityItem(
            task.id(),
            task.projectId(),
            task.projectCode(),
            task.status(),
            task.progressPercent(),
            task.totalScanned(),
            task.autoIngested(),
            task.pendingReview(),
            task.failedCount(),
            task.updatedAt()
        );
    }

    private SafetyBoundary defaultSafetyBoundary() {
        return new SafetyBoundary(
            List.of(
                "仅读取平台业务元数据和统计结果",
                "不返回真实 NAS 路径或底层存储字段",
                "不读取模型正文、族、构件属性或图纸正文"
            ),
            List.of(
                "真实 BIM 引擎接入",
                "轻量化转换任务创建",
                "构件级解析或搜索",
                "NAS 文件写入、移动、删除"
            ),
            "当前中央场景为模型元数据驱动的 BIM 协同视图，真实 3D Viewer 待 BIM 引擎选型后接入。"
        );
    }

    private String chooseNextActionCode(
        DeliveryCompletenessResponse document,
        DeliveryCompletenessResponse drawing,
        int openRectificationCount
    ) {
        if (openRectificationCount > 0) {
            return "HANDLE_RECTIFICATION";
        }
        if (!Boolean.TRUE.equals(document.standardReady()) || !Boolean.TRUE.equals(drawing.standardReady())) {
            return "COMPLETE_STANDARD";
        }
        if (intValue(document.missingCount()) + intValue(drawing.missingCount()) > 0) {
            return "BIND_MISSING_FILES";
        }
        if (intValue(document.pendingReviewCount()) + intValue(drawing.pendingReviewCount()) > 0) {
            return "REVIEW_PENDING";
        }
        if (intValue(document.approvedCount()) + intValue(drawing.approvedCount()) > 0) {
            return "EXPORT_PRECHECK";
        }
        return document.nextActionCode() != null ? document.nextActionCode() : "CHECK_DELIVERY";
    }

    private String chooseNextActionText(
        DeliveryCompletenessResponse document,
        DeliveryCompletenessResponse drawing,
        int openRectificationCount
    ) {
        if (openRectificationCount > 0) {
            return "存在未关闭整改项，请先处理整改闭环。";
        }
        if (!Boolean.TRUE.equals(document.standardReady()) || !Boolean.TRUE.equals(drawing.standardReady())) {
            return "先补齐工程主数据和交付物标准，再生成应交项。";
        }
        if (intValue(document.missingCount()) + intValue(drawing.missingCount()) > 0) {
            return "存在缺失应交项，请从资产目录选择文件完成补交。";
        }
        if (intValue(document.pendingReviewCount()) + intValue(drawing.pendingReviewCount()) > 0) {
            return "已有资料待审核，请继续完成审核或驳回整改。";
        }
        if (intValue(document.approvedCount()) + intValue(drawing.approvedCount()) > 0) {
            return "交付资料已具备基础完整度，可以执行导出预检查。";
        }
        return document.nextActionText() != null ? document.nextActionText() : "请继续检查交付准备状态。";
    }

    private String fileKindLabel(String code) {
        return switch (code) {
            case "MODEL" -> "模型";
            case "DRAWING" -> "图纸";
            case "DOCUMENT" -> "文档";
            case "SPREADSHEET" -> "表格";
            case "PRESENTATION" -> "演示";
            case "MODEL_VIEWER" -> "展示模型";
            case "ARCHIVE" -> "归档包";
            default -> code;
        };
    }

    private String disciplineLabel(String code) {
        return switch (code) {
            case "HVAC" -> "暖通";
            case "ELECTRICAL" -> "电气";
            case "PLUMBING" -> "给排水";
            case "FIRE" -> "消防";
            case "ARCHITECTURE" -> "建筑";
            case "STRUCTURE" -> "结构";
            case "OTHER" -> "其他";
            case "UNKNOWN" -> "未标注";
            default -> code;
        };
    }

    private String objectTypeLabel(String code) {
        return switch (code) {
            case "EQUIPMENT" -> "设备设施";
            case "SYSTEM" -> "专业系统";
            case "SPACE" -> "房屋空间";
            case "COMPONENT_PLACEHOLDER" -> "构件占位";
            case "UNKNOWN" -> "未分类";
            default -> code;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}

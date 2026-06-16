package com.zhuoyu.delivery.visualization.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
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
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.datasteward.file.FileResourceApplicationService;
import com.zhuoyu.delivery.datasteward.model.ModelIntegrationApplicationService;
import com.zhuoyu.delivery.datasteward.object.ManagedObjectApplicationService;
import com.zhuoyu.delivery.datasteward.storage.StorageService;
import com.zhuoyu.delivery.datasteward.storage.StorageService.StoredResource;
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
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarComponentPropertyGroup;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarComponentPropertyItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarComponentPropertyResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarModelFileResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarReadyModelProjectResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.GlandarRvtPilotFileResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightJobCreateResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightJobResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightPlanResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightStatusResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightViewerTicketResponse;
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
import com.zhuoyu.delivery.visualization.engine.GlandarEngineSettings;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.ComponentPropertyResult;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.ComponentPropertyRow;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.ModelOutputProbe;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.QueryResult;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.UploadCommand;
import com.zhuoyu.delivery.visualization.engine.GlandarStationClient.UploadResult;
import com.zhuoyu.delivery.visualization.engine.LightweightJobRepository;
import com.zhuoyu.delivery.visualization.engine.LightweightJobRepository.LightweightJobRecord;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryApplicationService;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationResponse;
import com.zhuoyu.delivery.workcenter.rectification.RectificationApplicationService;
import com.zhuoyu.delivery.shared.preview.FilePreviewPolicy;
import com.zhuoyu.delivery.shared.preview.PreviewDecision;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhuoyu.delivery.shared.exception.BusinessException;

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
        "CALL_STATION_SPLIT_UPLOAD",
        "READ_MODEL_BODY",
        "TOUCH_NAS_FILE",
        "WRITE_LIGHTWEIGHT_CACHE",
        "OPEN_REAL_3D_VIEWER",
        "WRITE_HERMES_MEMORY"
    );
    private static final List<String> GLANDAR_SUPPORTED_OPERATIONS = List.of(
        "CREATE_RVT_CONVERSION_TASK",
        "CALL_STATION_SPLIT_UPLOAD",
        "QUERY_STATION_TASK",
        "ISSUE_CONTROLLED_VIEWER_ENTRY",
        "PICK_FEATURE",
        "BLOW_MODEL",
        "QUERY_COMPONENT_PROPERTIES"
    );
    private static final List<String> GLANDAR_FORBIDDEN_OPERATIONS = List.of(
        "TOUCH_NAS_FILE",
        "EXPOSE_STORAGE_LOCATION",
        "EXPOSE_OBJECT_LOCATOR",
        "WRITE_HERMES_MEMORY",
        "WRITE_DOCUMENT_CHUNKS",
        "AUTO_APPROVE_DELIVERY"
    );
    private static final Long GLANDAR_RVT_PILOT_PROJECT_ID = 503L;
    private static final List<Long> GLANDAR_RVT_PILOT_FILE_IDS = List.of(
        1257L, 1261L, 1264L, 3730L, 1258L, 1251L, 1259L, 1262L, 3729L, 1243L
    );
    private static final List<String> GLANDAR_MODEL_EXTENSIONS = List.of("RVT", "IFC", "NWD", "NWC", "GLB", "GLTF");
    private static final List<String> GLANDAR_SUBMIT_SUPPORTED_EXTENSIONS = List.of("RVT");

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
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final GlandarEngineSettings glandarEngineSettings;
    private final GlandarStationClient glandarStationClient;
    private final LightweightJobRepository lightweightJobRepository;
    private final StorageService storageService;

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
        SectionNodeApplicationService sectionNodeApplicationService,
        ProjectAccessApplicationService projectAccessApplicationService,
        GlandarEngineSettings glandarEngineSettings,
        GlandarStationClient glandarStationClient,
        LightweightJobRepository lightweightJobRepository,
        StorageService storageService
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
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.glandarEngineSettings = glandarEngineSettings;
        this.glandarStationClient = glandarStationClient;
        this.lightweightJobRepository = lightweightJobRepository;
        this.storageService = storageService;
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
        ModelSummary modelSummary = toModelSummary(projectId, models, objects);
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
        String engineMode = engineMode();
        return new LightweightStatusResponse(
            projectId,
            integrationId,
            integration.modelFileId(),
            modelFile.originalName(),
            format,
            integration.status(),
            engineMode,
            false,
            lightweightStatusCode(engineMode),
            false,
            "NOT_CREATED",
            true,
            "NOT_STARTED",
            "BIM_LIGHTWEIGHT",
            lightweightStatusLabel(engineMode),
            lightweightActionHint(engineMode),
            lightweightBlockedReason(engineMode),
            LIGHTWEIGHT_SUPPORTED_OPERATIONS,
            LIGHTWEIGHT_FORBIDDEN_OPERATIONS
        );
    }

    public LightweightPlanResponse lightweightPlan(Long projectId, Long integrationId) {
        ModelIntegrationResponse integration = modelIntegrationApplicationService.requireIntegration(projectId, integrationId);
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(projectId, integration.modelFileId());
        String format = modelFormat(modelFile.originalName());
        String engineMode = engineMode();
        return new LightweightPlanResponse(
            projectId,
            integrationId,
            integration.modelFileId(),
            modelFile.originalName(),
            format,
            engineMode,
            true,
            false,
            true,
            false,
            false,
            false,
            List.of(
                "配置真实 BIM 引擎适配器和租户级连接参数",
                "配置葛兰岱尔 Station API、Station Web 和安全凭据",
                "确认模型格式支持矩阵和版本兼容规则",
                "定义平台到葛兰岱尔的分片上传、查询和 Viewer 会话契约",
                "定义轻量化输出存储策略、viewer ticket 和权限隔离",
                "建立转换任务、重试、超时、失败回滚和审计策略",
                "补齐权限审计、路径脱敏和预览授权校验"
            ),
            List.of(
                "葛兰岱尔适配器连通性校验",
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

    public LightweightJobCreateResponse createLightweightJob(Long userId, Long projectId, Long integrationId) {
        ModelIntegrationResponse integration = modelIntegrationApplicationService.requireIntegration(projectId, integrationId);
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(projectId, integration.modelFileId());
        if (!glandarRealModeReady()) {
            return lightweightJobSkeleton(userId, projectId, integrationId, modelFile);
        }
        return submitGlandarJob(userId, projectId, integrationId, modelFile, false);
    }

    public LightweightJobCreateResponse createLightweightJobForFile(Long userId, Long projectId, Long fileId, Boolean force) {
        FileResourceResponse modelFile = fileResourceApplicationService.requireFile(projectId, fileId);
        requireSupportedGlandarModel(modelFile);
        if (!glandarRealModeReady()) {
            throw new BusinessException("GLANDAR_ENGINE_NOT_CONFIGURED",
                "葛兰岱尔 Station 未完整配置，平台未提交真实轻量化任务", HttpStatus.PRECONDITION_FAILED);
        }
        return submitGlandarJob(userId, projectId, null, modelFile, Boolean.TRUE.equals(force));
    }

    public List<GlandarRvtPilotFileResponse> glandarRvtPilotFiles(Long projectId) {
        if (!GLANDAR_RVT_PILOT_PROJECT_ID.equals(projectId)) {
            return List.of();
        }
        List<GlandarRvtPilotFileResponse> rows = new ArrayList<>();
        for (int index = 0; index < GLANDAR_RVT_PILOT_FILE_IDS.size(); index++) {
            Long fileId = GLANDAR_RVT_PILOT_FILE_IDS.get(index);
            FileResourceResponse file = fileResourceApplicationService.requireFile(projectId, fileId);
            LightweightJobRecord latest = lightweightJobRepository.findLatest(projectId, fileId)
                .map(this::refreshGlandarJobForList)
                .orElse(null);
            rows.add(toGlandarRvtPilotFileResponse(projectId, file, latest, index + 1));
        }
        return rows;
    }

    public List<GlandarModelFileResponse> glandarModelFiles(Long projectId) {
        return fileResourceApplicationService.list(projectId, "MODEL").stream()
            .filter(this::isGlandarModelCandidate)
            .sorted(Comparator
                .comparing((FileResourceResponse file) -> !isSubmitSupportedGlandarFormat(modelFormat(file.originalName())))
                .thenComparing(FileResourceResponse::id))
            .map(file -> {
                LightweightJobRecord latest = lightweightJobRepository.findLatest(projectId, file.id())
                    .map(this::refreshGlandarJobForList)
                    .orElse(null);
                return toGlandarModelFileResponse(projectId, file, latest);
            })
            .toList();
    }

    public List<GlandarReadyModelProjectResponse> glandarReadyModelCatalog(Long userId) {
        List<AccessibleProject> projects = projectAccessApplicationService.listAccessibleProjects(userId);
        if (projects.isEmpty()) {
            return List.of();
        }
        Map<Long, List<GlandarModelFileResponse>> readyByProject = lightweightJobRepository
            .findReadyModels(projects.stream().map(AccessibleProject::id).toList())
            .stream()
            .collect(Collectors.groupingBy(
                LightweightJobRepository.ReadyModelRecord::projectId,
                LinkedHashMap::new,
                Collectors.mapping(this::toReadyModelFileResponse, Collectors.toList())
            ));
        return projects.stream()
            .map(project -> {
                List<GlandarModelFileResponse> readyModels = readyByProject.getOrDefault(project.id(), List.of());
                return readyModels.isEmpty() ? null : toReadyModelProject(project, readyModels);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private GlandarModelFileResponse toReadyModelFileResponse(LightweightJobRepository.ReadyModelRecord record) {
        String extension = modelFormat(record.fileName());
        return new GlandarModelFileResponse(
            record.projectId(),
            record.fileId(),
            record.assetUuid(),
            record.fileName(),
            extension,
            record.fileKind(),
            record.sizeBytes(),
            defaultText(record.versionNo(), "V1"),
            null,
            record.status(),
            String.valueOf(record.jobId()),
            record.status(),
            record.progressPercent(),
            null,
            record.viewerAvailable(),
            true,
            null,
            lightweightRecordStatusLabel(record.status(), record.viewerAvailable()),
            "模型已轻量化，可打开受控 Viewer",
            record.updatedAt()
        );
    }

    private GlandarReadyModelProjectResponse toReadyModelProject(
        AccessibleProject project,
        List<GlandarModelFileResponse> readyModels
    ) {
        return new GlandarReadyModelProjectResponse(
            project.id(),
            project.code(),
            project.name(),
            project.projectManagerName(),
            project.roleName(),
            readyModels.size(),
            readyModels
        );
    }

    public List<GlandarRvtPilotFileResponse> submitGlandarRvtPilotFiles(Long userId, Long projectId, Boolean force) {
        if (!GLANDAR_RVT_PILOT_PROJECT_ID.equals(projectId)) {
            throw new BusinessException("GLANDAR_PILOT_PROJECT_NOT_ALLOWED", "当前项目未纳入葛兰岱尔 RVT 试点",
                HttpStatus.BAD_REQUEST);
        }
        for (Long fileId : GLANDAR_RVT_PILOT_FILE_IDS) {
            createLightweightJobForFile(userId, projectId, fileId, force);
        }
        return glandarRvtPilotFiles(projectId);
    }

    public LightweightJobResponse lightweightJob(Long projectId, String jobId) {
        String engineMode = engineMode();
        Long numericJobId = parseJobId(jobId);
        if (numericJobId != null) {
            return lightweightJobRepository.findById(projectId, numericJobId)
                .map(this::refreshGlandarJob)
                .map(this::toLightweightJobResponse)
                .orElseThrow(() -> new BusinessException("LIGHTWEIGHT_JOB_NOT_FOUND", "轻量化任务不存在",
                    HttpStatus.NOT_FOUND));
        }
        return new LightweightJobResponse(
            projectId,
            defaultText(jobId, safeJobId(engineMode, projectId, 0L)),
            null,
            null,
            null,
            null,
            engineMode,
            glandarEngineSettings.readyForHandshake() ? "READY_FOR_GD2" : "NOT_CREATED",
            0,
            lightweightJobStatusLabel(engineMode),
            lightweightBlockedReason(engineMode),
            false,
            false,
            false,
            null,
            null,
            Instant.now()
        );
    }

    public LightweightViewerTicketResponse lightweightViewerTicket(Long userId, Long projectId, String jobId) {
        String engineMode = engineMode();
        Long numericJobId = parseJobId(jobId);
        if (numericJobId != null) {
            LightweightJobRecord record = lightweightJobRepository.findById(projectId, numericJobId)
                .orElseThrow(() -> new BusinessException("LIGHTWEIGHT_JOB_NOT_FOUND", "轻量化任务不存在",
                    HttpStatus.NOT_FOUND));
            LightweightJobRecord refreshed = refreshGlandarJob(record);
            String engineStaticBase = glandarEngineStaticBase(refreshed.modelAccessAddress());
            boolean stationModelReady = "READY".equals(refreshed.status()) && Boolean.TRUE.equals(refreshed.viewerAvailable())
                && hasText(refreshed.modelAccessAddress());
            ModelOutputProbe outputProbe = stationModelReady
                ? glandarStationClient.probeModelOutput(refreshed.modelAccessAddress())
                : null;
            boolean ready = stationModelReady && hasText(engineStaticBase)
                && outputProbe != null && Boolean.TRUE.equals(outputProbe.readable());
            auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.lightweight.viewer-ticket.issue",
                "LIGHTWEIGHT_JOB", String.valueOf(refreshed.id()), userId, Map.of(
                    "engineMode", "GLANDAR",
                    "ticketIssued", ready,
                    "status", refreshed.status()
                ));
            return new LightweightViewerTicketResponse(
                projectId,
                String.valueOf(refreshed.id()),
                "GLANDAR",
                ready,
                ready,
                ready ? "viewer-" + refreshed.id() : null,
                ready ? Instant.now().plusSeconds(900) : null,
                ready ? refreshed.modelAccessAddress() : null,
                refreshed.lightweightName(),
                ready ? refreshed.modelAccessAddress() : null,
                ready ? engineStaticBase : null,
                ready ? "Viewer 可打开" : "Viewer 暂不可用",
                ready ? null : viewerTicketBlockedReason(refreshed, engineStaticBase, outputProbe),
                GLANDAR_SUPPORTED_OPERATIONS,
                GLANDAR_FORBIDDEN_OPERATIONS,
                ready,
                ready,
                ready
            );
        }
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.lightweight.viewer-ticket.prepare", "PROJECT",
            String.valueOf(projectId), userId, Map.of("engineMode", engineMode, "ticketIssued", false));
        return new LightweightViewerTicketResponse(
            projectId,
            defaultText(jobId, safeJobId(engineMode, projectId, 0L)),
            engineMode,
            false,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            "Viewer 未开放",
            "8B-GD1 仅提供平台接口骨架；真实葛兰岱尔 Viewer ticket 留到 8B-GD2/8C-GD。",
            LIGHTWEIGHT_SUPPORTED_OPERATIONS,
            LIGHTWEIGHT_FORBIDDEN_OPERATIONS,
            false,
            false,
            false
        );
    }

    public GlandarComponentPropertyResponse glandarComponentProperties(
        Long userId,
        Long projectId,
        String jobId,
        String featureId,
        String revitId
    ) {
        Long numericJobId = parseJobId(jobId);
        if (numericJobId == null) {
            throw new BusinessException("LIGHTWEIGHT_JOB_NOT_FOUND", "轻量化任务不存在", HttpStatus.NOT_FOUND);
        }
        LightweightJobRecord record = lightweightJobRepository.findById(projectId, numericJobId)
            .orElseThrow(() -> new BusinessException("LIGHTWEIGHT_JOB_NOT_FOUND", "轻量化任务不存在",
                HttpStatus.NOT_FOUND));
        LightweightJobRecord refreshed = refreshGlandarJob(record);
        if (!"GLANDAR".equals(refreshed.engineProvider()) || !"READY".equals(refreshed.status())
            || !Boolean.TRUE.equals(refreshed.viewerAvailable()) || !hasText(refreshed.lightweightName())) {
            return new GlandarComponentPropertyResponse(
                projectId,
                String.valueOf(refreshed.id()),
                refreshed.lightweightName(),
                safeExternalId(featureId),
                safeExternalId(revitId),
                "GLANDAR_COMPONENT_PROPERTY_API",
                false,
                "模型尚未达到可查询构件属性的 READY 状态",
                0,
                List.of()
            );
        }
        String externalId = firstNonBlank(safeExternalId(featureId), safeExternalId(revitId));
        if (!hasText(externalId)) {
            throw new BusinessException("FEATURE_ID_REQUIRED", "缺少构件 ID，无法查询构件属性", HttpStatus.BAD_REQUEST);
        }
        ComponentPropertyResult result = glandarStationClient.componentProperties(refreshed.lightweightName(), externalId);
        List<GlandarComponentPropertyGroup> groups = toPropertyGroups(result.rows());
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.glandar.component-properties.query",
            "LIGHTWEIGHT_JOB", String.valueOf(refreshed.id()), userId, Map.of(
                "featureId", externalId,
                "propertyCount", result.rows().size()
            ));
        return new GlandarComponentPropertyResponse(
            projectId,
            String.valueOf(refreshed.id()),
            refreshed.lightweightName(),
            externalId,
            safeExternalId(revitId),
            "GLANDAR_COMPONENT_PROPERTY_API",
            !groups.isEmpty(),
            groups.isEmpty() ? "葛兰岱尔未返回该构件属性，可能是当前模型未生成属性库或构件 ID 不匹配" : null,
            result.rows().size(),
            groups
        );
    }

    private GlandarRvtPilotFileResponse toGlandarRvtPilotFileResponse(
        Long projectId,
        FileResourceResponse file,
        LightweightJobRecord latest,
        int pilotRank
    ) {
        String taskStatus = latest == null ? "NOT_STARTED" : latest.status();
        Integer progress = latest == null ? 0 : latest.progressPercent();
        Boolean viewerAvailable = latest != null && Boolean.TRUE.equals(latest.viewerAvailable());
        return new GlandarRvtPilotFileResponse(
            projectId,
            file.id(),
            file.assetUuid(),
            file.originalName(),
            modelFormat(file.originalName()),
            file.sizeBytes(),
            pilotRank,
            true,
            latest == null ? null : String.valueOf(latest.id()),
            latest == null ? null : latest.lightweightName(),
            taskStatus,
            progress,
            viewerAvailable,
            glandarPilotStatusLabel(latest),
            glandarPilotActionHint(latest),
            latest == null ? null : failedReason(latest),
            latest == null ? null : latest.updatedAt()
        );
    }

    private GlandarModelFileResponse toGlandarModelFileResponse(
        Long projectId,
        FileResourceResponse file,
        LightweightJobRecord latest
    ) {
        String format = modelFormat(file.originalName());
        boolean supported = isSubmitSupportedGlandarFormat(format);
        String taskStatus = latest == null ? "NOT_STARTED" : latest.status();
        String lightweightStatus = lightweightStatus(latest, supported);
        String unsupportedReason = supported ? null : unsupportedGlandarReason(format);
        return new GlandarModelFileResponse(
            projectId,
            file.id(),
            file.assetUuid(),
            file.originalName(),
            format,
            file.fileKind(),
            file.sizeBytes(),
            defaultText(file.versionNo(), "V1"),
            relativePathHint(file),
            lightweightStatus,
            latest == null ? null : String.valueOf(latest.id()),
            taskStatus,
            latest == null ? 0 : latest.progressPercent(),
            latest == null ? null : failedReason(latest),
            latest != null && "READY".equals(latest.status()) && Boolean.TRUE.equals(latest.viewerAvailable()),
            supported,
            unsupportedReason,
            glandarModelStatusLabel(latest, supported),
            glandarModelActionHint(latest, supported, unsupportedReason),
            latest == null ? null : latest.updatedAt()
        );
    }

    private LightweightJobRecord refreshGlandarJobForList(LightweightJobRecord record) {
        if (!glandarRealModeReady()) {
            return record;
        }
        return refreshGlandarJob(record);
    }

    private String glandarPilotStatusLabel(LightweightJobRecord latest) {
        if (latest == null) {
            return "需轻量化";
        }
        return lightweightRecordStatusLabel(latest);
    }

    private String glandarPilotActionHint(LightweightJobRecord latest) {
        if (latest == null) {
            return "已纳入 105 RVT 试点，可提交葛兰岱尔轻量化转换。";
        }
        return switch (defaultText(latest.status(), "RUNNING")) {
            case "READY" -> "已轻量化，双击可在平台内 BIM Viewer 打开。";
            case "FAILED" -> "轻量化失败，可人工检查失败原因后重试。";
            case "UPLOADED", "SUBMITTED", "RUNNING" -> "轻量化处理中，可刷新状态。";
            default -> "可继续查看轻量化任务状态。";
        };
    }

    private String lightweightStatus(LightweightJobRecord latest, boolean supported) {
        if (!supported) {
            return "UNSUPPORTED";
        }
        if (latest == null) {
            return "CONVERSION_REQUIRED";
        }
        return switch (defaultText(latest.status(), "RUNNING")) {
            case "READY" -> "READY";
            case "FAILED" -> "FAILED";
            case "SUBMITTED", "UPLOADED", "RUNNING" -> "RUNNING";
            default -> "CONVERSION_REQUIRED";
        };
    }

    private String glandarModelStatusLabel(LightweightJobRecord latest, boolean supported) {
        if (!supported) {
            return "暂不支持轻量化";
        }
        if (latest == null) {
            return "未轻量化";
        }
        return lightweightRecordStatusLabel(latest);
    }

    private String glandarModelActionHint(LightweightJobRecord latest, boolean supported, String unsupportedReason) {
        if (!supported) {
            return unsupportedReason;
        }
        if (latest == null) {
            return "可由有权限用户提交葛兰岱尔轻量化任务；平台不会暴露 NAS 路径或引擎凭据。";
        }
        return switch (defaultText(latest.status(), "RUNNING")) {
            case "READY" -> "已轻量化，READY 状态可申请短期 Viewer 入口。";
            case "FAILED" -> "轻量化失败，可查看失败原因后人工重试。";
            case "SUBMITTED", "UPLOADED", "RUNNING" -> "轻量化任务处理中，可刷新状态。";
            default -> "可继续查看轻量化任务状态。";
        };
    }

    private String glandarEngineStaticBase(String modelAccessAddress) {
        String stationWebBase = glandarEngineSettings.stationWebBase();
        if (!hasText(stationWebBase)) {
            stationWebBase = inferGlandarStationWebBase(modelAccessAddress);
        }
        if (!hasText(stationWebBase)) {
            return null;
        }
        String trimmed = stationWebBase.endsWith("/") ? stationWebBase.substring(0, stationWebBase.length() - 1) : stationWebBase;
        if (trimmed.endsWith("/static/ThreeJsEngine")) {
            return trimmed;
        }
        return trimmed + "/static/ThreeJsEngine";
    }

    private String inferGlandarStationWebBase(String modelAccessAddress) {
        if (!hasText(modelAccessAddress)) {
            return null;
        }
        try {
            URI uri = new URI(modelAccessAddress);
            if (!hasText(uri.getScheme()) || !hasText(uri.getHost())) {
                return null;
            }
            int port = uri.getPort();
            int webPort = port == 18086 ? 18087 : port;
            if (webPort <= 0) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + webPort;
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private String viewerTicketBlockedReason(
        LightweightJobRecord record,
        String engineStaticBase,
        ModelOutputProbe outputProbe
    ) {
        if (!"READY".equals(record.status()) || !Boolean.TRUE.equals(record.viewerAvailable())
            || !hasText(record.modelAccessAddress())) {
            return defaultText(record.lastErrorMessage(), "模型仍在转换或引擎尚未返回 Viewer 地址");
        }
        if (!hasText(engineStaticBase)) {
            return "葛兰岱尔 Viewer 静态资源地址未配置，请检查 GLANDAR_STATION_WEB_BASE。";
        }
        if (outputProbe != null && !Boolean.TRUE.equals(outputProbe.readable())) {
            return defaultText(outputProbe.message(), "葛兰岱尔模型输出服务暂不可用，请检查 Station 模型输出服务。");
        }
        return defaultText(record.lastErrorMessage(), "Viewer 暂不可用，请刷新后重试。");
    }

    private LightweightJobCreateResponse lightweightJobSkeleton(
        Long userId,
        Long projectId,
        Long integrationId,
        FileResourceResponse modelFile
    ) {
        String format = modelFormat(modelFile.originalName());
        String engineMode = engineMode();
        String jobId = safeJobId(engineMode, projectId, integrationId == null ? modelFile.id() : integrationId);
        String status = glandarEngineSettings.readyForHandshake() ? "READY_FOR_GD2" : "BLOCKED";
        auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.lightweight.job.prepare",
            integrationId == null ? "FILE_RESOURCE" : "MODEL_INTEGRATION",
            String.valueOf(integrationId == null ? modelFile.id() : integrationId),
            userId, Map.of("engineMode", engineMode, "taskCreated", false));
        return new LightweightJobCreateResponse(
            projectId,
            integrationId,
            modelFile.id(),
            modelFile.originalName(),
            format,
            jobId,
            null,
            null,
            null,
            engineMode,
            false,
            status,
            0,
            lightweightJobStatusLabel(engineMode),
            lightweightJobActionHint(engineMode),
            lightweightBlockedReason(engineMode),
            false,
            false,
            false,
            false,
            false,
            LIGHTWEIGHT_SUPPORTED_OPERATIONS,
            LIGHTWEIGHT_FORBIDDEN_OPERATIONS
        );
    }

    private LightweightJobCreateResponse submitGlandarJob(
        Long userId,
        Long projectId,
        Long integrationId,
        FileResourceResponse modelFile,
        boolean force
    ) {
        String format = modelFormat(modelFile.originalName());
        requireSupportedGlandarModel(modelFile);
        if (force) {
            lightweightJobRepository.markSuperseded(projectId, modelFile.id(), userId);
        }
        LightweightJobRecord existing = force ? null : lightweightJobRepository.findReusable(projectId, modelFile.id()).orElse(null);
        if (existing != null) {
            LightweightJobRecord refreshed = refreshGlandarJob(existing);
            return toLightweightJobCreateResponse(refreshed, modelFile, "复用已有葛兰岱尔轻量化任务");
        }

        String lightweightName = lightweightName(modelFile);
        String uniqueCode = "delivery-file-" + modelFile.id();
        Long jobId = lightweightJobRepository.insertSubmitting(
            projectId,
            integrationId,
            modelFile.id(),
            modelFile.assetUuid(),
            2,
            lightweightName,
            uniqueCode,
            userId
        );
        try {
            StoredResource resource = storageService.openReadable(toFileAsset(projectId, modelFile, format));
            UploadResult upload = glandarStationClient.upload(new UploadCommand(
                stableStationFileName(lightweightName, format),
                lightweightName,
                uniqueCode,
                "projectId=%s;fileId=%s;assetUuid=%s".formatted(projectId, modelFile.id(), defaultText(modelFile.assetUuid(), ""))
            ), resource.resource().getInputStream(), defaultLong(resource.contentLength(), modelFile.sizeBytes()));
            lightweightJobRepository.markUploaded(jobId, upload.lightweightName(), upload.stationRecordJson(), userId);
            LightweightJobRecord uploaded = lightweightJobRepository.findById(projectId, jobId)
                .orElseThrow(() -> new BusinessException("LIGHTWEIGHT_JOB_NOT_FOUND", "轻量化任务不存在", HttpStatus.NOT_FOUND));
            LightweightJobRecord refreshed = refreshGlandarJob(uploaded);
            auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.lightweight.job.submit",
                "LIGHTWEIGHT_JOB", String.valueOf(jobId), userId, Map.of(
                    "engineMode", "GLANDAR",
                    "fileId", modelFile.id(),
                    "status", refreshed.status()
                ));
            return toLightweightJobCreateResponse(refreshed, modelFile, "已提交葛兰岱尔轻量化任务");
        } catch (BusinessException exception) {
            lightweightJobRepository.markFailed(jobId, exception.getCode(), exception.getMessage(), userId);
            auditLogApplicationService.record(projectId, MODULE_CODE, "visualization.lightweight.job.failed",
                "LIGHTWEIGHT_JOB", String.valueOf(jobId), userId, Map.of(
                    "engineMode", "GLANDAR",
                    "fileId", modelFile.id(),
                    "errorCode", exception.getCode()
                ));
            LightweightJobRecord failed = lightweightJobRepository.findById(projectId, jobId)
                .orElseThrow(() -> exception);
            return toLightweightJobCreateResponse(failed, modelFile, "葛兰岱尔轻量化任务提交失败");
        } catch (Exception exception) {
            lightweightJobRepository.markFailed(jobId, "ENGINE_UPLOAD_FAILED", "轻量化任务提交失败", userId);
            LightweightJobRecord failed = lightweightJobRepository.findById(projectId, jobId)
                .orElseThrow(() -> new BusinessException("ENGINE_UPLOAD_FAILED", "轻量化任务提交失败",
                    HttpStatus.PRECONDITION_FAILED));
            return toLightweightJobCreateResponse(failed, modelFile, "葛兰岱尔轻量化任务提交失败");
        }
    }

    private LightweightJobRecord refreshGlandarJob(LightweightJobRecord record) {
        if (!"GLANDAR".equals(record.engineProvider()) || !hasText(record.lightweightName())
            || "FAILED".equals(record.status()) || !glandarRealModeReady()) {
            return record;
        }
        try {
            QueryResult query = glandarStationClient.query(record.lightweightName());
            lightweightJobRepository.updateStationStatus(
                record.id(),
                query.status(),
                query.progressPercent(),
                query.modelAccessAddress(),
                query.viewerAvailable(),
                query.errorCode(),
                query.message(),
                query.stationRecordJson()
            );
            return lightweightJobRepository.findById(record.projectId(), record.id()).orElse(record);
        } catch (BusinessException exception) {
            if ("ENGINE_API_UNREACHABLE".equals(exception.getCode())) {
                return record;
            }
            lightweightJobRepository.updateStationStatus(
                record.id(),
                "FAILED",
                0,
                null,
                false,
                exception.getCode(),
                exception.getMessage(),
                record.stationRecordJson()
            );
            return lightweightJobRepository.findById(record.projectId(), record.id()).orElse(record);
        }
    }

    private LightweightJobCreateResponse toLightweightJobCreateResponse(
        LightweightJobRecord record,
        FileResourceResponse file,
        String actionHint
    ) {
        return new LightweightJobCreateResponse(
            record.projectId(),
            record.integrationId(),
            record.fileId(),
            file.originalName(),
            modelFormat(file.originalName()),
            String.valueOf(record.id()),
            record.lightweightName(),
            record.uniqueCode(),
            safeViewerAddress(record.modelAccessAddress()),
            "GLANDAR",
            true,
            record.status(),
            record.progressPercent(),
            lightweightRecordStatusLabel(record),
            actionHint,
            failedReason(record),
            true,
            true,
            true,
            false,
            Boolean.TRUE.equals(record.viewerAvailable()),
            GLANDAR_SUPPORTED_OPERATIONS,
            GLANDAR_FORBIDDEN_OPERATIONS
        );
    }

    private LightweightJobResponse toLightweightJobResponse(LightweightJobRecord record) {
        return new LightweightJobResponse(
            record.projectId(),
            String.valueOf(record.id()),
            record.fileId(),
            record.lightweightName(),
            record.uniqueCode(),
            safeViewerAddress(record.modelAccessAddress()),
            "GLANDAR",
            record.status(),
            record.progressPercent(),
            lightweightRecordStatusLabel(record),
            failedReason(record),
            Boolean.TRUE.equals(record.viewerAvailable()),
            true,
            true,
            record.lastErrorCode(),
            record.lastErrorMessage(),
            record.updatedAt()
        );
    }

    private FileAssetResponse toFileAsset(Long projectId, FileResourceResponse file, String format) {
        return new FileAssetResponse(
            file.id(),
            file.assetUuid(),
            projectId,
            null,
            null,
            file.originalName(),
            format,
            file.fileKind(),
            null,
            file.versionNo(),
            file.sizeBytes(),
            file.checksum(),
            null,
            file.storageUri(),
            null,
            "CATALOG",
            file.processStatus(),
            null,
            null,
            null,
            file.processedAt(),
            List.of(),
            "PROJECT",
            "INTERNAL",
            file.processedAt(),
            "ACTIVE",
            "CATALOG_ONLY"
        );
    }

    private void requireSupportedGlandarModel(FileResourceResponse file) {
        String format = modelFormat(file.originalName());
        if (!"MODEL".equalsIgnoreCase(defaultText(file.fileKind(), "")) || !isSubmitSupportedGlandarFormat(format)) {
            throw new BusinessException("UNSUPPORTED_FILE_TYPE",
                "当前葛兰岱尔接入只开放已登记 RVT 模型轻量化，其他模型格式仅展示状态", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean glandarRealModeReady() {
        return "GLANDAR".equals(engineMode()) && glandarEngineSettings.readyForHandshake();
    }

    private Long parseJobId(String jobId) {
        if (!hasText(jobId)) {
            return null;
        }
        try {
            return Long.valueOf(jobId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<GlandarComponentPropertyGroup> toPropertyGroups(List<ComponentPropertyRow> rows) {
        Map<String, List<GlandarComponentPropertyItem>> grouped = new LinkedHashMap<>();
        for (ComponentPropertyRow row : rows) {
            String groupName = defaultText(safePropertyText(row.propertyTypeName()), "基础属性");
            String setName = defaultText(safePropertyText(row.propertySetName()), defaultText(safePropertyText(row.groupName()), "默认分组"));
            String propertyName = safePropertyText(row.propertyName());
            String propertyValue = safePropertyText(row.value());
            if (!hasText(propertyName) && !hasText(propertyValue)) {
                continue;
            }
            String key = groupName + "\u0000" + setName;
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(new GlandarComponentPropertyItem(
                    defaultText(propertyName, "未命名属性"),
                    defaultText(propertyValue, "-"),
                    groupName
                ));
        }
        return grouped.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split("\u0000", 2);
                return new GlandarComponentPropertyGroup(parts[0], parts.length > 1 ? parts[1] : "默认分组", entry.getValue());
            })
            .toList();
    }

    private String safeExternalId(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 256) {
            trimmed = trimmed.substring(0, 256);
        }
        return trimmed.replaceAll("[\\r\\n\\t]", "");
    }

    private String safePropertyText(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            trimmed = trimmed.substring(0, 500) + "...";
        }
        return trimmed
            .replaceAll("(?i)(/Volumes|smb://|nas://|minio://|s3://|oss://)[^\\s,，;；]*", "[已脱敏]")
            .replaceAll("[\\r\\n\\t]", " ");
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private String lightweightName(FileResourceResponse file) {
        String compactUuid = defaultText(file.assetUuid(), "file" + file.id()).replace("-", "");
        if (compactUuid.length() > 16) {
            compactUuid = compactUuid.substring(0, 16);
        }
        return "gd_file_%s_%s".formatted(file.id(), compactUuid);
    }

    private String stableStationFileName(String lightweightName, String format) {
        String extension = defaultText(format, "RVT").toLowerCase(Locale.ROOT);
        return lightweightName + "." + extension;
    }

    private String lightweightRecordStatusLabel(LightweightJobRecord record) {
        return switch (defaultText(record.status(), "RUNNING")) {
            case "READY" -> "模型轻量化完成";
            case "FAILED" -> "模型轻量化失败";
            case "UPLOADED" -> "模型已提交，等待引擎处理";
            case "RUNNING" -> "模型轻量化处理中";
            default -> "模型轻量化任务已创建";
        };
    }

    private String lightweightRecordStatusLabel(String status, Boolean viewerAvailable) {
        if ("READY".equals(defaultText(status, "RUNNING")) && Boolean.TRUE.equals(viewerAvailable)) {
            return "模型轻量化完成";
        }
        return switch (defaultText(status, "RUNNING")) {
            case "READY" -> "模型轻量化完成，等待 Viewer 确认";
            case "FAILED" -> "模型轻量化失败";
            case "UPLOADED" -> "模型已提交，等待引擎处理";
            case "RUNNING" -> "模型轻量化处理中";
            default -> "模型轻量化任务已创建";
        };
    }

    private String failedReason(LightweightJobRecord record) {
        if (!"FAILED".equals(record.status())) {
            return null;
        }
        return defaultText(record.lastErrorMessage(), "葛兰岱尔引擎未返回详细失败原因");
    }

    private String safeViewerAddress(String address) {
        if (!hasText(address)) {
            return null;
        }
        if (address.contains("127.0.0.1") || address.contains("localhost")) {
            return null;
        }
        return address;
    }

    private long defaultLong(Long primary, Long fallback) {
        if (primary != null && primary >= 0) {
            return primary;
        }
        return fallback == null ? -1L : fallback;
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

    private String engineMode() {
        return glandarEngineSettings.provider();
    }

    private String lightweightStatusCode(String engineMode) {
        return "GLANDAR".equals(engineMode) && glandarEngineSettings.readyForHandshake()
            ? "ADAPTER_READY"
            : "NOT_CONNECTED";
    }

    private String lightweightStatusLabel(String engineMode) {
        if ("GLANDAR".equals(engineMode)) {
            return glandarEngineSettings.readyForHandshake() ? "葛兰岱尔适配骨架已就绪" : "葛兰岱尔配置不完整";
        }
        return "Mock 轻量化适配";
    }

    private String lightweightActionHint(String engineMode) {
        if ("GLANDAR".equals(engineMode)) {
            if (glandarEngineSettings.readyForHandshake()) {
                return "葛兰岱尔 Station 配置已具备真实提交条件；平台仍只通过受控接口上传模型流，不暴露路径或凭据。";
            }
            return "葛兰岱尔适配器未完整配置，当前仅返回平台侧安全骨架；不会调用 Station 上传或转换接口。";
        }
        return "当前为 Mock 适配，未执行真实轻量化转换；接入葛兰岱尔引擎后才能打开 3D 预览。";
    }

    private String lightweightBlockedReason(String engineMode) {
        if (!"GLANDAR".equals(engineMode)) {
            return "BIM_ENGINE_PROVIDER 未启用 GLANDAR，当前保持 Mock 安全模式";
        }
        List<String> missing = glandarEngineSettings.missingConfiguration();
        return missing.isEmpty() ? "葛兰岱尔 Station 当前不可用" : String.join("；", missing);
    }

    private String lightweightJobStatusLabel(String engineMode) {
        if ("GLANDAR".equals(engineMode) && glandarEngineSettings.readyForHandshake()) {
            return "可提交葛兰岱尔轻量化任务";
        }
        if ("GLANDAR".equals(engineMode)) {
            return "葛兰岱尔任务被配置拦截";
        }
        return "Mock 模式未创建真实任务";
    }

    private String lightweightJobActionHint(String engineMode) {
        if ("GLANDAR".equals(engineMode) && glandarEngineSettings.readyForHandshake()) {
            return "平台将通过受控 StorageService 读取模型流并提交 Station；不会返回 NAS 路径、对象定位符或凭据。";
        }
        return lightweightActionHint(engineMode);
    }

    private String safeJobId(String engineMode, Long projectId, Long integrationId) {
        return "%s-%s-%s".formatted(engineMode.toLowerCase(Locale.ROOT), projectId, integrationId);
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

    private boolean isGlandarModelCandidate(FileResourceResponse file) {
        return "MODEL".equalsIgnoreCase(defaultText(file.fileKind(), ""))
            && GLANDAR_MODEL_EXTENSIONS.contains(modelFormat(file.originalName()));
    }

    private boolean isSubmitSupportedGlandarFormat(String format) {
        return GLANDAR_SUBMIT_SUPPORTED_EXTENSIONS.contains(defaultText(format, "UNKNOWN").toUpperCase(Locale.ROOT));
    }

    private String unsupportedGlandarReason(String format) {
        String normalized = defaultText(format, "UNKNOWN").toUpperCase(Locale.ROOT);
        if (GLANDAR_MODEL_EXTENSIONS.contains(normalized)) {
            return "%s 模型已识别，但当前葛兰岱尔 Station 接入只开放 RVT 轻量化提交。".formatted(normalized);
        }
        return "当前文件格式未纳入葛兰岱尔轻量化支持矩阵。";
    }

    private String relativePathHint(FileResourceResponse file) {
        if (!hasText(file.originalName())) {
            return "项目模型资产";
        }
        return "项目模型资产 / " + file.originalName();
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

    private ModelSummary toModelSummary(
        Long projectId,
        List<ModelIntegrationResponse> models,
        List<ManagedObjectResponse> objects
    ) {
        int published = (int) models.stream().filter(model -> "PUBLISHED".equals(model.status())).count();
        long readyViewerCount = lightweightJobRepository.countViewerReady(projectId);
        boolean viewerAvailable = readyViewerCount > 0;
        String lightweightStatus = viewerAvailable ? "READY" : (models.isEmpty() ? "NOT_STARTED" : "NOT_CONNECTED");
        String statusLabel = viewerAvailable ? "真实 Viewer 可用" : (models.isEmpty() ? "暂无模型集成" : "真实 Viewer 未接入");
        String actionHint = viewerAvailable
            ? "已检测到 " + readyViewerCount + " 个葛兰岱尔 READY 模型，可在 BIM 协同窗口中打开。"
            : (models.isEmpty()
                ? "当前项目还没有模型集成元数据，可先在数据管家登记模型。"
                : "当前只展示模型元数据和适配状态，未执行真实轻量化转换。");
        return new ModelSummary(
            models.size(),
            Math.max(published, (int) Math.min(readyViewerCount, Integer.MAX_VALUE)),
            objects.size(),
            viewerAvailable ? "GLANDAR" : "METADATA_ADAPTER",
            viewerAvailable,
            lightweightStatus,
            viewerAvailable,
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }
}

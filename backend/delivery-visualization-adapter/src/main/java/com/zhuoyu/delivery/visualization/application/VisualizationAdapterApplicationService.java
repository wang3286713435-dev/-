package com.zhuoyu.delivery.visualization.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.datasteward.file.FileResourceApplicationService;
import com.zhuoyu.delivery.datasteward.model.ModelIntegrationApplicationService;
import com.zhuoyu.delivery.datasteward.object.ManagedObjectApplicationService;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightPlanResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LightweightStatusResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LocateResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ManagedObjectContextItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ModelContextItem;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.VisualizationContextResponse;
import java.util.List;
import java.util.Map;
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
    private final AuditLogApplicationService auditLogApplicationService;

    public VisualizationAdapterApplicationService(
        ModelIntegrationApplicationService modelIntegrationApplicationService,
        ManagedObjectApplicationService managedObjectApplicationService,
        FileResourceApplicationService fileResourceApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.modelIntegrationApplicationService = modelIntegrationApplicationService;
        this.managedObjectApplicationService = managedObjectApplicationService;
        this.fileResourceApplicationService = fileResourceApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
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
            "MOCK",
            false,
            "NOT_CONNECTED",
            false,
            "NOT_CREATED",
            true,
            "NOT_STARTED",
            "BIM_LIGHTWEIGHT",
            "Mock 适配未连接",
            "当前为 Mock 适配，未执行真实轻量化转换；接入真实 BIM 引擎后才能打开 3D 预览。",
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
            "MOCK",
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
                "未接入真实引擎前，禁止把 Mock 状态包装为可预览"
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
}

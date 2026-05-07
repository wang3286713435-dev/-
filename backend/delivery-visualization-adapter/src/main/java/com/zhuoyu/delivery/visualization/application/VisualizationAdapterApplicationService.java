package com.zhuoyu.delivery.visualization.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.datasteward.model.ModelIntegrationApplicationService;
import com.zhuoyu.delivery.datasteward.object.ManagedObjectApplicationService;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.HighlightResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.LinkageResponse;
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

    private final ModelIntegrationApplicationService modelIntegrationApplicationService;
    private final ManagedObjectApplicationService managedObjectApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public VisualizationAdapterApplicationService(
        ModelIntegrationApplicationService modelIntegrationApplicationService,
        ManagedObjectApplicationService managedObjectApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.modelIntegrationApplicationService = modelIntegrationApplicationService;
        this.managedObjectApplicationService = managedObjectApplicationService;
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
}

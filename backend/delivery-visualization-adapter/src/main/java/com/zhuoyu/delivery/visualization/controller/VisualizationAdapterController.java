package com.zhuoyu.delivery.visualization.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.visualization.application.VisualizationAdapterApplicationService;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectRequest;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.ContextInjectResponse;
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.DigitalTwinDashboardResponse;
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
import com.zhuoyu.delivery.visualization.dto.VisualizationDtos.VisualizationContextResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visualization-adapter/projects/{projectId}")
public class VisualizationAdapterController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final VisualizationAdapterApplicationService visualizationAdapterApplicationService;

    public VisualizationAdapterController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        VisualizationAdapterApplicationService visualizationAdapterApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.visualizationAdapterApplicationService = visualizationAdapterApplicationService;
    }

    @GetMapping("/context")
    public ApiResponse<VisualizationContextResponse> context(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.context(projectId));
    }

    @GetMapping("/digital-twin-dashboard")
    public ApiResponse<DigitalTwinDashboardResponse> digitalTwinDashboard(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.digitalTwinDashboard(principal.userId(), projectId));
    }

    @PostMapping("/model-integrations/{integrationId}:publish")
    public ApiResponse<ModelIntegrationResponse> publishModel(
        @PathVariable Long projectId,
        @PathVariable Long integrationId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.publishModel(principal.userId(), projectId, integrationId));
    }

    @GetMapping("/model-integrations/{integrationId}/lightweight-status")
    public ApiResponse<LightweightStatusResponse> lightweightStatus(
        @PathVariable Long projectId,
        @PathVariable Long integrationId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.lightweightStatus(projectId, integrationId));
    }

    @GetMapping("/model-integrations/{integrationId}/lightweight-plan")
    public ApiResponse<LightweightPlanResponse> lightweightPlan(
        @PathVariable Long projectId,
        @PathVariable Long integrationId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.lightweightPlan(projectId, integrationId));
    }

    @PostMapping("/model-integrations/{integrationId}/lightweight-jobs")
    public ApiResponse<LightweightJobCreateResponse> createLightweightJob(
        @PathVariable Long projectId,
        @PathVariable Long integrationId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.createLightweightJob(
            principal.userId(), projectId, integrationId));
    }

    @PostMapping("/files/{fileId}/lightweight-jobs")
    public ApiResponse<LightweightJobCreateResponse> createLightweightJobForFile(
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @RequestParam(required = false, defaultValue = "false") Boolean force
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.createLightweightJobForFile(
            principal.userId(), projectId, fileId, force));
    }

    @GetMapping("/lightweight-jobs/{jobId}")
    public ApiResponse<LightweightJobResponse> lightweightJob(
        @PathVariable Long projectId,
        @PathVariable String jobId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.lightweightJob(projectId, jobId));
    }

    @PostMapping("/lightweight-jobs/{jobId}:viewer-ticket")
    public ApiResponse<LightweightViewerTicketResponse> lightweightViewerTicket(
        @PathVariable Long projectId,
        @PathVariable String jobId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.lightweightViewerTicket(
            principal.userId(), projectId, jobId));
    }

    @GetMapping("/glandar/rvt-pilot-files")
    public ApiResponse<List<GlandarRvtPilotFileResponse>> glandarRvtPilotFiles(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.glandarRvtPilotFiles(projectId));
    }

    @PostMapping("/glandar/rvt-pilot-files:submit")
    public ApiResponse<List<GlandarRvtPilotFileResponse>> submitGlandarRvtPilotFiles(
        @PathVariable Long projectId,
        @RequestParam(required = false, defaultValue = "false") Boolean force
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.submitGlandarRvtPilotFiles(
            principal.userId(), projectId, force));
    }

    @PostMapping("/managed-objects/{objectId}:locate")
    public ApiResponse<LocateResponse> locate(@PathVariable Long projectId, @PathVariable Long objectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.locate(principal.userId(), projectId, objectId));
    }

    @PostMapping("/managed-objects/{objectId}:highlight")
    public ApiResponse<HighlightResponse> highlight(
        @PathVariable Long projectId,
        @PathVariable Long objectId,
        @RequestBody(required = false) HighlightRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.highlight(principal.userId(), projectId, objectId, request));
    }

    @PostMapping("/linkage:sync")
    public ApiResponse<LinkageResponse> syncLinkage(
        @PathVariable Long projectId,
        @Valid @RequestBody LinkageRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.syncLinkage(principal.userId(), projectId, request));
    }

    @PostMapping("/context:inject")
    public ApiResponse<ContextInjectResponse> injectContext(
        @PathVariable Long projectId,
        @RequestBody(required = false) ContextInjectRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(visualizationAdapterApplicationService.injectContext(principal.userId(), projectId, request));
    }
}

package com.zhuoyu.delivery.workcenter.agentgovernance;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentBindingRecommendationRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentBindingRecommendationResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceMissingItemsResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.AgentGovernanceOverviewResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationsRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ApplyAgentRecommendationsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-center/projects/{projectId}/agent-governance")
public class AgentGovernanceController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final AgentGovernanceApplicationService agentGovernanceApplicationService;

    public AgentGovernanceController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        AgentGovernanceApplicationService agentGovernanceApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.agentGovernanceApplicationService = agentGovernanceApplicationService;
    }

    @GetMapping("/overview")
    public ApiResponse<AgentGovernanceOverviewResponse> overview(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(agentGovernanceApplicationService.overview(projectId));
    }

    @GetMapping("/missing-items")
    public ApiResponse<AgentGovernanceMissingItemsResponse> missingItems(
        @PathVariable Long projectId,
        @RequestParam(required = false) String viewType,
        @RequestParam(defaultValue = "SECTION") String targetType
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(agentGovernanceApplicationService.missingItems(projectId, viewType, targetType));
    }

    @PostMapping("/recommend-bindings")
    public ApiResponse<AgentBindingRecommendationResponse> recommendBindings(
        @PathVariable Long projectId,
        @RequestBody(required = false) AgentBindingRecommendationRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(agentGovernanceApplicationService.recommendBindings(principal.userId(), projectId, request));
    }

    @PostMapping("/recommendations:apply")
    public ApiResponse<ApplyAgentRecommendationsResponse> applyRecommendations(
        @PathVariable Long projectId,
        @Valid @RequestBody ApplyAgentRecommendationsRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(agentGovernanceApplicationService.applyRecommendations(principal.userId(), projectId, request));
    }
}

package com.zhuoyu.delivery.datasteward.model;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ModelIntegrationResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/projects/{projectId}/model-integrations")
public class ModelIntegrationController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final ModelIntegrationApplicationService modelIntegrationApplicationService;

    public ModelIntegrationController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        ModelIntegrationApplicationService modelIntegrationApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.modelIntegrationApplicationService = modelIntegrationApplicationService;
    }

    @PostMapping
    public ApiResponse<ModelIntegrationResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody ModelIntegrationRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(modelIntegrationApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<ModelIntegrationResponse>> list(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(modelIntegrationApplicationService.list(projectId));
    }

    @PatchMapping("/{integrationId}:publish")
    public ApiResponse<ModelIntegrationResponse> publish(
        @PathVariable Long projectId,
        @PathVariable Long integrationId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(modelIntegrationApplicationService.publish(principal.userId(), projectId, integrationId));
    }
}

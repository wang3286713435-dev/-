package com.zhuoyu.delivery.masterdata.deliverable.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableDefinitionApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableDefinitionRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableDefinitionResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableDefinitionRequest;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data/projects/{projectId}/deliverable-definitions")
public class DeliverableDefinitionController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final DeliverableDefinitionApplicationService definitionApplicationService;

    public DeliverableDefinitionController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        DeliverableDefinitionApplicationService definitionApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.definitionApplicationService = definitionApplicationService;
    }

    @PostMapping
    public ApiResponse<DeliverableDefinitionResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateDeliverableDefinitionRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(definitionApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<DeliverableDefinitionResponse>> list(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(definitionApplicationService.list(projectId));
    }

    @PatchMapping("/{definitionId}")
    public ApiResponse<DeliverableDefinitionResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long definitionId,
        @Valid @RequestBody UpdateDeliverableDefinitionRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(definitionApplicationService.update(principal.userId(), projectId, definitionId, request));
    }

    @DeleteMapping("/{definitionId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long definitionId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        definitionApplicationService.delete(principal.userId(), projectId, definitionId);
        return ApiResponse.success(null);
    }
}

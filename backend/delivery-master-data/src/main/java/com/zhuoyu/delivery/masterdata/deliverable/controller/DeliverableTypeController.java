package com.zhuoyu.delivery.masterdata.deliverable.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableTypeApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableTypeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableTypeResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableTypeRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data/projects/{projectId}/deliverable-types")
public class DeliverableTypeController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final DeliverableTypeApplicationService typeApplicationService;

    public DeliverableTypeController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        DeliverableTypeApplicationService typeApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.typeApplicationService = typeApplicationService;
    }

    @PostMapping
    public ApiResponse<DeliverableTypeResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateDeliverableTypeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(typeApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<DeliverableTypeResponse>> list(
        @PathVariable Long projectId,
        @RequestParam(required = false) Long definitionId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(typeApplicationService.listByDefinition(projectId, definitionId));
    }

    @PatchMapping("/{typeId}")
    public ApiResponse<DeliverableTypeResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long typeId,
        @Valid @RequestBody UpdateDeliverableTypeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(typeApplicationService.update(principal.userId(), projectId, typeId, request));
    }

    @DeleteMapping("/{typeId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long typeId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        typeApplicationService.delete(principal.userId(), projectId, typeId);
        return ApiResponse.success(null);
    }
}

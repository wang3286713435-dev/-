package com.zhuoyu.delivery.masterdata.deliverable.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableAttributeApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.dto.CreateDeliverableAttributeRequest;
import com.zhuoyu.delivery.masterdata.deliverable.dto.DeliverableAttributeResponse;
import com.zhuoyu.delivery.masterdata.deliverable.dto.UpdateDeliverableAttributeRequest;
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
@RequestMapping("/api/master-data/projects/{projectId}/deliverable-attributes")
public class DeliverableAttributeController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final DeliverableAttributeApplicationService attributeApplicationService;

    public DeliverableAttributeController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        DeliverableAttributeApplicationService attributeApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.attributeApplicationService = attributeApplicationService;
    }

    @PostMapping
    public ApiResponse<DeliverableAttributeResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateDeliverableAttributeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(attributeApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<DeliverableAttributeResponse>> list(
        @PathVariable Long projectId,
        @RequestParam(required = false) Long typeId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(attributeApplicationService.listByType(projectId, typeId));
    }

    @PatchMapping("/{attributeId}")
    public ApiResponse<DeliverableAttributeResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long attributeId,
        @Valid @RequestBody UpdateDeliverableAttributeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(attributeApplicationService.update(principal.userId(), projectId, attributeId, request));
    }

    @DeleteMapping("/{attributeId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long attributeId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        attributeApplicationService.delete(principal.userId(), projectId, attributeId);
        return ApiResponse.success(null);
    }
}

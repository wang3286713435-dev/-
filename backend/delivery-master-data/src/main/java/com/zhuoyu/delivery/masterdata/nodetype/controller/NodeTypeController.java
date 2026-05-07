package com.zhuoyu.delivery.masterdata.nodetype.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.dto.CreateNodeTypeRequest;
import com.zhuoyu.delivery.masterdata.nodetype.dto.NodeTypeLockStatusResponse;
import com.zhuoyu.delivery.masterdata.nodetype.dto.NodeTypeResponse;
import com.zhuoyu.delivery.masterdata.nodetype.dto.UpdateNodeTypeRequest;
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
@RequestMapping("/api/master-data/projects/{projectId}")
public class NodeTypeController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final NodeTypeApplicationService nodeTypeApplicationService;

    public NodeTypeController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        NodeTypeApplicationService nodeTypeApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
    }

    @PostMapping("/node-types")
    public ApiResponse<NodeTypeResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateNodeTypeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping("/node-types")
    public ApiResponse<List<NodeTypeResponse>> list(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.list(projectId));
    }

    @PatchMapping("/node-types/{nodeTypeId}")
    public ApiResponse<NodeTypeResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long nodeTypeId,
        @Valid @RequestBody UpdateNodeTypeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.update(principal.userId(), projectId, nodeTypeId, request));
    }

    @PostMapping("/node-types/{nodeTypeId}:lock")
    public ApiResponse<NodeTypeResponse> lock(@PathVariable Long projectId, @PathVariable Long nodeTypeId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.lock(principal.userId(), projectId, nodeTypeId));
    }

    @PostMapping("/node-types:lock")
    public ApiResponse<NodeTypeLockStatusResponse> lockAll(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.lockAll(principal.userId(), projectId));
    }

    @GetMapping("/node-types/{nodeTypeId}/lock-status")
    public ApiResponse<NodeTypeLockStatusResponse> lockStatus(
        @PathVariable Long projectId,
        @PathVariable Long nodeTypeId
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.lockStatus(projectId, nodeTypeId));
    }

    @GetMapping("/node-types/lock-status")
    public ApiResponse<NodeTypeLockStatusResponse> projectLockStatus(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(nodeTypeApplicationService.projectLockStatus(projectId));
    }
}

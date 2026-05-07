package com.zhuoyu.delivery.masterdata.section.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.section.application.SectionNodeApplicationService;
import com.zhuoyu.delivery.masterdata.section.dto.CreateSectionNodeRequest;
import com.zhuoyu.delivery.masterdata.section.dto.SectionNodeResponse;
import com.zhuoyu.delivery.masterdata.section.dto.UpdateSectionNodeRequest;
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
@RequestMapping("/api/master-data/projects/{projectId}/section-nodes")
public class SectionNodeController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final SectionNodeApplicationService sectionNodeApplicationService;

    public SectionNodeController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        SectionNodeApplicationService sectionNodeApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.sectionNodeApplicationService = sectionNodeApplicationService;
    }

    @PostMapping
    public ApiResponse<SectionNodeResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateSectionNodeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(sectionNodeApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping("/tree")
    public ApiResponse<List<SectionNodeResponse>> tree(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(sectionNodeApplicationService.tree(projectId));
    }

    @PatchMapping("/{nodeId}")
    public ApiResponse<SectionNodeResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long nodeId,
        @Valid @RequestBody UpdateSectionNodeRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(sectionNodeApplicationService.update(principal.userId(), projectId, nodeId, request));
    }

    @DeleteMapping("/{nodeId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long nodeId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        sectionNodeApplicationService.delete(principal.userId(), projectId, nodeId);
        return ApiResponse.success();
    }
}

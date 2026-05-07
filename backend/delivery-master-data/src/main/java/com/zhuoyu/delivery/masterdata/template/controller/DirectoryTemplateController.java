package com.zhuoyu.delivery.masterdata.template.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.template.application.DirectoryTemplateApplicationService;
import com.zhuoyu.delivery.masterdata.template.dto.CreateDirectoryTemplateRequest;
import com.zhuoyu.delivery.masterdata.template.dto.DirectoryTemplateResponse;
import com.zhuoyu.delivery.masterdata.template.dto.UpdateDirectoryTemplateRequest;
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
@RequestMapping("/api/master-data/projects/{projectId}/directory-templates")
public class DirectoryTemplateController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final DirectoryTemplateApplicationService templateApplicationService;

    public DirectoryTemplateController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        DirectoryTemplateApplicationService templateApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.templateApplicationService = templateApplicationService;
    }

    @PostMapping
    public ApiResponse<DirectoryTemplateResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateDirectoryTemplateRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(templateApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<DirectoryTemplateResponse>> list(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(templateApplicationService.list(projectId));
    }

    @PatchMapping("/{templateId}")
    public ApiResponse<DirectoryTemplateResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long templateId,
        @Valid @RequestBody UpdateDirectoryTemplateRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(templateApplicationService.update(principal.userId(), projectId, templateId, request));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long templateId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        templateApplicationService.delete(principal.userId(), projectId, templateId);
        return ApiResponse.success(null);
    }
}

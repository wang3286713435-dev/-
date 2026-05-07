package com.zhuoyu.delivery.masterdata.status.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.status.application.StandardStatusApplicationService;
import com.zhuoyu.delivery.masterdata.status.dto.StandardStatusResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data/projects/{projectId}/standard-status")
public class StandardStatusController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final StandardStatusApplicationService standardStatusApplicationService;

    public StandardStatusController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        StandardStatusApplicationService standardStatusApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.standardStatusApplicationService = standardStatusApplicationService;
    }

    @GetMapping
    public ApiResponse<StandardStatusResponse> status(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(standardStatusApplicationService.getStatus(projectId));
    }
}

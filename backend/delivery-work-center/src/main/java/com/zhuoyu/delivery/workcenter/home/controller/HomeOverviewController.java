package com.zhuoyu.delivery.workcenter.home.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.workcenter.home.application.HomeOverviewApplicationService;
import com.zhuoyu.delivery.workcenter.home.dto.HomeOverviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-center")
public class HomeOverviewController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final HomeOverviewApplicationService homeOverviewApplicationService;

    public HomeOverviewController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        HomeOverviewApplicationService homeOverviewApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.homeOverviewApplicationService = homeOverviewApplicationService;
    }

    @GetMapping("/projects/{projectId}/home/overview")
    public ApiResponse<HomeOverviewResponse> overview(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        if (!projectId.equals(principal.currentProjectId())) {
            throw new BusinessException("CORE_PROJECT_CONTEXT_MISMATCH", "请先切换到目标项目", HttpStatus.FORBIDDEN);
        }
        return ApiResponse.success(homeOverviewApplicationService.getOverview(principal.userId(), projectId));
    }
}

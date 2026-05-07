package com.zhuoyu.delivery.core.user.controller;

import com.zhuoyu.delivery.core.auth.application.AuthApplicationService;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.auth.dto.SessionTokenResponse;
import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.core.project.dto.ProjectSummaryResponse;
import com.zhuoyu.delivery.core.user.application.CurrentUserApplicationService;
import com.zhuoyu.delivery.core.user.dto.CurrentUserResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class CurrentUserController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final CurrentUserApplicationService currentUserApplicationService;
    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final AuthApplicationService authApplicationService;

    public CurrentUserController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        CurrentUserApplicationService currentUserApplicationService,
        ProjectAccessApplicationService projectAccessApplicationService,
        AuthApplicationService authApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.currentUserApplicationService = currentUserApplicationService;
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.authApplicationService = authApplicationService;
    }

    @GetMapping("/users/me")
    public ApiResponse<CurrentUserResponse> currentUser() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(
            currentUserApplicationService.getCurrentUser(principal.userId(), principal.currentProjectId())
        );
    }

    @GetMapping("/projects")
    public ApiResponse<List<ProjectSummaryResponse>> projects() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        List<ProjectSummaryResponse> projects = projectAccessApplicationService.listAccessibleProjects(principal.userId()).stream()
            .map(projectAccessApplicationService::toResponse)
            .toList();
        return ApiResponse.success(projects);
    }

    @PostMapping("/projects/{projectId}:switch")
    public ApiResponse<SessionTokenResponse> switchProject(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(authApplicationService.switchProject(principal.userId(), projectId));
    }
}

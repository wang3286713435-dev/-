package com.zhuoyu.delivery.datasteward.project.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectBusinessProfileResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectBusinessProfileUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectMembersSummaryResponse;
import com.zhuoyu.delivery.datasteward.project.application.ProjectBusinessProfileApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/projects/{projectId}")
public class ProjectBusinessProfileController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectBusinessProfileApplicationService applicationService;

    public ProjectBusinessProfileController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectBusinessProfileApplicationService applicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.applicationService = applicationService;
    }

    @GetMapping("/business-profile")
    public ApiResponse<ProjectBusinessProfileResponse> getBusinessProfile(@PathVariable Long projectId) {
        return ApiResponse.success(applicationService.getBusinessProfile(currentUserId(), projectId));
    }

    @PutMapping("/business-profile")
    public ApiResponse<ProjectBusinessProfileResponse> updateBusinessProfile(
        @PathVariable Long projectId,
        @RequestBody ProjectBusinessProfileUpdateRequest request
    ) {
        return ApiResponse.success(applicationService.updateBusinessProfile(currentUserId(), projectId, request));
    }

    @GetMapping("/members-summary")
    public ApiResponse<ProjectMembersSummaryResponse> getMembersSummary(@PathVariable Long projectId) {
        return ApiResponse.success(applicationService.membersSummary(currentUserId(), projectId));
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }
}

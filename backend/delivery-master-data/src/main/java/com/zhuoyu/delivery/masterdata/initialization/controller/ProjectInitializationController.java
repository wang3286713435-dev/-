package com.zhuoyu.delivery.masterdata.initialization.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.masterdata.common.application.MasterDataProjectContextService;
import com.zhuoyu.delivery.masterdata.initialization.application.ProjectInitializationApplicationService;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.InitializationStatusResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingApplyRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingApplyResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingAssessmentResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingConfirmRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingConfirmResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDraftPreviewResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.StandardTemplateDetailResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.StandardTemplateSummaryResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateApplyRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateApplyResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplatePreviewRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplatePreviewResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master-data")
public class ProjectInitializationController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final MasterDataProjectContextService projectContextService;
    private final ProjectInitializationApplicationService initializationApplicationService;

    public ProjectInitializationController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        MasterDataProjectContextService projectContextService,
        ProjectInitializationApplicationService initializationApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextService = projectContextService;
        this.initializationApplicationService = initializationApplicationService;
    }

    @GetMapping("/projects/{projectId}/initialization/status")
    public ApiResponse<InitializationStatusResponse> status(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.status(projectId));
    }

    @GetMapping("/projects/{projectId}/onboarding/assessment")
    public ApiResponse<OnboardingAssessmentResponse> onboardingAssessment(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.onboardingAssessment(projectId));
    }

    @GetMapping("/projects/{projectId}/onboarding/preview")
    public ApiResponse<OnboardingDraftPreviewResponse> onboardingPreview(
        @PathVariable Long projectId,
        @RequestParam(required = false) String templateCode
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.onboardingPreview(projectId, templateCode));
    }

    @PostMapping("/projects/{projectId}/onboarding/apply")
    public ApiResponse<OnboardingApplyResponse> onboardingApply(
        @PathVariable Long projectId,
        @RequestBody OnboardingApplyRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.applyOnboarding(principal.userId(), projectId, request));
    }

    @PostMapping("/projects/{projectId}/onboarding/confirm")
    public ApiResponse<OnboardingConfirmResponse> onboardingConfirm(
        @PathVariable Long projectId,
        @RequestBody OnboardingConfirmRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.confirmOnboarding(principal.userId(), projectId, request));
    }

    @GetMapping("/standard-templates")
    public ApiResponse<List<StandardTemplateSummaryResponse>> templates() {
        securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(initializationApplicationService.listTemplates());
    }

    @GetMapping("/standard-templates/{templateCode}")
    public ApiResponse<StandardTemplateDetailResponse> template(@PathVariable String templateCode) {
        securityPrincipalAccessor.requireCurrentPrincipal();
        return ApiResponse.success(initializationApplicationService.templateDetail(templateCode));
    }

    @PostMapping("/projects/{projectId}/initialization:preview-template")
    public ApiResponse<TemplatePreviewResponse> preview(
        @PathVariable Long projectId,
        @Valid @RequestBody TemplatePreviewRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.preview(projectId, request));
    }

    @PostMapping("/projects/{projectId}/initialization:apply-template")
    public ApiResponse<TemplateApplyResponse> apply(
        @PathVariable Long projectId,
        @Valid @RequestBody TemplateApplyRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(initializationApplicationService.apply(principal.userId(), projectId, request));
    }
}

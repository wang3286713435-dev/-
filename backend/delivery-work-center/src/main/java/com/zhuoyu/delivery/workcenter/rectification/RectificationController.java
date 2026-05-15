package com.zhuoyu.delivery.workcenter.rectification;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RectificationResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ResolveRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-center/projects/{projectId}")
public class RectificationController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final RectificationApplicationService rectificationApplicationService;

    public RectificationController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        RectificationApplicationService rectificationApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.rectificationApplicationService = rectificationApplicationService;
    }

    @GetMapping("/rectifications")
    public ApiResponse<List<RectificationResponse>> list(
        @PathVariable Long projectId,
        @RequestParam(required = false) String status
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.list(projectId, status));
    }

    @GetMapping("/rectifications/{rectificationId}")
    public ApiResponse<RectificationResponse> detail(@PathVariable Long projectId, @PathVariable Long rectificationId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.detail(projectId, rectificationId));
    }

    @PatchMapping("/rectifications/{rectificationId}")
    public ApiResponse<RectificationResponse> update(
        @PathVariable Long projectId, @PathVariable Long rectificationId,
        @RequestBody RectificationRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.update(principal.userId(), projectId, rectificationId, request));
    }

    @PostMapping("/rectifications/{rectificationId}:resolve")
    public ApiResponse<RectificationResponse> resolve(
        @PathVariable Long projectId, @PathVariable Long rectificationId,
        @Valid @RequestBody ResolveRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.resolve(principal.userId(), projectId, rectificationId, request));
    }

    @PostMapping("/rectifications/{rectificationId}:close")
    public ApiResponse<RectificationResponse> close(@PathVariable Long projectId, @PathVariable Long rectificationId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.close(principal.userId(), projectId, rectificationId));
    }

    @PostMapping("/rectifications/{rectificationId}:reopen")
    public ApiResponse<RectificationResponse> reopen(@PathVariable Long projectId, @PathVariable Long rectificationId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(rectificationApplicationService.reopen(principal.userId(), projectId, rectificationId));
    }
}

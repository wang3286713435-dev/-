package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryViewResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-center/projects/{projectId}")
public class DeliveryController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final DeliveryApplicationService deliveryApplicationService;

    public DeliveryController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        DeliveryApplicationService deliveryApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.deliveryApplicationService = deliveryApplicationService;
    }

    @PostMapping("/delivery-bindings")
    public ApiResponse<DeliveryBindingResponse> createBinding(
        @PathVariable Long projectId,
        @Valid @RequestBody DeliveryBindingRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.createBinding(principal.userId(), projectId, request));
    }

    @GetMapping("/delivery-views")
    public ApiResponse<DeliveryViewResponse> deliveryView(
        @PathVariable Long projectId,
        @RequestParam(defaultValue = "DOCUMENT") String viewType
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.deliveryView(projectId, viewType));
    }

    @DeleteMapping("/delivery-bindings/{bindingId}")
    public ApiResponse<Void> deleteBinding(@PathVariable Long projectId, @PathVariable Long bindingId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        deliveryApplicationService.deleteBinding(principal.userId(), projectId, bindingId);
        return ApiResponse.success();
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<DashboardSummaryResponse> dashboardSummary(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.dashboardSummary(projectId));
    }
}

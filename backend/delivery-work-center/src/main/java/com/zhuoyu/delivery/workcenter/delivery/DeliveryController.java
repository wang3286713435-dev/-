package com.zhuoyu.delivery.workcenter.delivery;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.BatchDeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DashboardSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryBindingResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryCompletenessResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryPackageSummaryResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.DeliveryViewResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ExportPrecheckResponse;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.RejectRequest;
import com.zhuoyu.delivery.workcenter.dto.WorkCenterDtos.ReviewRecordResponse;
import jakarta.validation.Valid;
import java.util.List;
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

    @GetMapping("/delivery-completeness")
    public ApiResponse<DeliveryCompletenessResponse> deliveryCompleteness(
        @PathVariable Long projectId,
        @RequestParam(defaultValue = "DOCUMENT") String viewType,
        @RequestParam(defaultValue = "SECTION") String targetType,
        @RequestParam(defaultValue = "false") boolean onlyMissing
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.deliveryCompleteness(projectId, viewType, targetType, onlyMissing));
    }

    @PostMapping("/delivery-bindings:batch")
    public ApiResponse<BatchDeliveryBindingResponse> createBatchBinding(
        @PathVariable Long projectId,
        @Valid @RequestBody BatchDeliveryBindingRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.createBatchBinding(principal.userId(), projectId, request));
    }

    @GetMapping("/delivery-package/summary")
    public ApiResponse<DeliveryPackageSummaryResponse> deliveryPackageSummary(
        @PathVariable Long projectId,
        @RequestParam(required = false) String viewType,
        @RequestParam(defaultValue = "SECTION") String targetType
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.deliveryPackageSummary(projectId, viewType, targetType));
    }

    @GetMapping("/delivery-package/export-precheck")
    public ApiResponse<ExportPrecheckResponse> exportPrecheck(
        @PathVariable Long projectId,
        @RequestParam(required = false) String viewType,
        @RequestParam(defaultValue = "SECTION") String targetType
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.exportPrecheck(projectId, viewType, targetType));
    }

    // ---- review ----

    @PostMapping("/delivery-bindings/{bindingId}:submit-review")
    public ApiResponse<DeliveryBindingResponse> submitReview(@PathVariable Long projectId, @PathVariable Long bindingId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.submitReview(principal.userId(), projectId, bindingId));
    }

    @PostMapping("/delivery-bindings/{bindingId}:approve")
    public ApiResponse<DeliveryBindingResponse> approve(@PathVariable Long projectId, @PathVariable Long bindingId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.approve(principal.userId(), projectId, bindingId));
    }

    @PostMapping("/delivery-bindings/{bindingId}:reject")
    public ApiResponse<DeliveryBindingResponse> reject(
        @PathVariable Long projectId, @PathVariable Long bindingId,
        @Valid @RequestBody RejectRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.reject(principal.userId(), projectId, bindingId, request));
    }

    @GetMapping("/delivery-bindings/{bindingId}/review-records")
    public ApiResponse<List<ReviewRecordResponse>> reviewRecords(@PathVariable Long projectId, @PathVariable Long bindingId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(deliveryApplicationService.getReviewRecords(projectId, bindingId));
    }
}

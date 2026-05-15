package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.DeleteRequestApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.QuarantineRecordResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets")
public class DeleteRequestController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final DeleteRequestApplicationService deleteRequestService;

    public DeleteRequestController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        DeleteRequestApplicationService deleteRequestService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.deleteRequestService = deleteRequestService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    // ===== delete requests =====

    @PostMapping("/delete-requests")
    public ApiResponse<DeleteRequestResponse> createRequest(
        @Valid @RequestBody DeleteRequestCreateRequest request
    ) {
        return ApiResponse.success(deleteRequestService.createRequest(currentUserId(), request));
    }

    @GetMapping("/delete-requests")
    public ApiResponse<List<DeleteRequestResponse>> listRequests(
        @RequestParam Long projectId,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(deleteRequestService.listRequests(currentUserId(), projectId, status));
    }

    @GetMapping("/delete-requests/{id}")
    public ApiResponse<DeleteRequestResponse> getRequest(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.getRequest(currentUserId(), id));
    }

    @PostMapping("/delete-requests/{id}:approve")
    public ApiResponse<DeleteRequestResponse> approveRequest(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.approve(currentUserId(), id));
    }

    @PostMapping("/delete-requests/{id}:reject")
    public ApiResponse<DeleteRequestResponse> rejectRequest(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.reject(currentUserId(), id));
    }

    @PostMapping("/delete-requests/{id}:execute")
    public ApiResponse<DeleteRequestResponse> executeRequest(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.executeRequest(currentUserId(), id));
    }

    // ===== quarantine records =====

    @GetMapping("/quarantine-records")
    public ApiResponse<List<QuarantineRecordResponse>> listQuarantineRecords(
        @RequestParam Long projectId,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(deleteRequestService.listQuarantineRecords(currentUserId(), projectId, status));
    }

    @GetMapping("/quarantine-records/{id}")
    public ApiResponse<QuarantineRecordResponse> getQuarantineRecord(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.getQuarantineRecord(currentUserId(), id));
    }

    @PostMapping("/quarantine-records/{id}:restore")
    public ApiResponse<QuarantineRecordResponse> restoreQuarantine(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.restoreQuarantine(currentUserId(), id));
    }

    @PostMapping("/quarantine-records/{id}:permanent-delete")
    public ApiResponse<QuarantineRecordResponse> permanentDelete(@PathVariable Long id) {
        return ApiResponse.success(deleteRequestService.permanentDelete(currentUserId(), id));
    }
}

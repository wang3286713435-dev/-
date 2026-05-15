package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.ChecksumApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.BatchChecksumRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ChecksumJobRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets/checksum-jobs")
public class ChecksumController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ChecksumApplicationService checksumApplicationService;

    public ChecksumController(SecurityPrincipalAccessor securityPrincipalAccessor,
                               ChecksumApplicationService checksumApplicationService) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.checksumApplicationService = checksumApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @PostMapping
    public ApiResponse<JobResponse> createSingleChecksum(@Valid @RequestBody ChecksumJobRequest request) {
        return ApiResponse.success(
            checksumApplicationService.createSingleFileChecksum(currentUserId(), request));
    }

    @PostMapping("/batch")
    public ApiResponse<Integer> createBatchChecksum(@Valid @RequestBody BatchChecksumRequest request) {
        int count = checksumApplicationService.createBatchChecksum(currentUserId(), request);
        return ApiResponse.success(count);
    }
}

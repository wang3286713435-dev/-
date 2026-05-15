package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.JobApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets/jobs")
public class JobController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final JobApplicationService jobApplicationService;

    public JobController(SecurityPrincipalAccessor securityPrincipalAccessor,
                          JobApplicationService jobApplicationService) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.jobApplicationService = jobApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @GetMapping
    public ApiResponse<List<JobResponse>> listJobs(
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
            jobApplicationService.listJobs(currentUserId(), jobType, status, projectId, limit));
    }

    @GetMapping("/{jobId}")
    public ApiResponse<JobResponse> getJob(@PathVariable Long jobId) {
        return ApiResponse.success(jobApplicationService.getJob(currentUserId(), jobId));
    }

    @PostMapping("/{jobId}:retry")
    public ApiResponse<Void> retryJob(@PathVariable Long jobId) {
        jobApplicationService.retryJob(currentUserId(), jobId);
        return ApiResponse.success();
    }
}

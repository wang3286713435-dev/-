package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetJobRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {

    private final AssetJobRepository jobRepository;
    private final BimAssetRepository bimAssetRepository;
    private final EventApplicationService eventService;

    public JobApplicationService(AssetJobRepository jobRepository, BimAssetRepository bimAssetRepository,
                                  EventApplicationService eventService) {
        this.jobRepository = jobRepository;
        this.bimAssetRepository = bimAssetRepository;
        this.eventService = eventService;
    }

    @Transactional
    public Long createJob(Long userId, String jobType, Long projectId,
                           String targetType, Long targetId, String requestPayload) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
        }
        return jobRepository.insert(jobType, projectId, targetType, targetId, requestPayload, 3, userId);
    }

    public List<JobResponse> listJobs(Long userId, String jobType, String status, Long projectId, Integer limit) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
        }
        Set<Long> accessibleIds = accessibleProjectIds(userId);
        List<JobResponse> jobs = jobRepository.listForUser(userId, accessibleIds,
            jobType, status, projectId, limit != null ? limit : 50);
        return jobs.stream()
            .filter(j -> {
                if (j.projectId() == null) {
                    return j.createdBy() != null && j.createdBy().equals(userId);
                }
                return accessibleIds.contains(j.projectId());
            })
            .map(AssetJobResponseSanitizer::sanitize)
            .toList();
    }

    public JobResponse getJob(Long userId, Long jobId) {
        JobResponse job = jobRepository.requireById(jobId);
        if (!canAccessJob(userId, job)) {
            throw new BusinessException("ASSET_JOB_ACCESS_DENIED", "无权访问该任务", HttpStatus.FORBIDDEN);
        }
        return AssetJobResponseSanitizer.sanitize(job);
    }

    @Transactional
    public void retryJob(Long userId, Long jobId) {
        JobResponse job = getJob(userId, jobId);
        if (!"FAILED".equals(job.status())) {
            throw new BusinessException("ASSET_JOB_NOT_FAILED", "只能重试失败状态的任务", HttpStatus.BAD_REQUEST);
        }
        boolean retried = jobRepository.retry(jobId);
        if (!retried) {
            throw new BusinessException("ASSET_JOB_MAX_RETRIES", "任务已达最大重试次数", HttpStatus.BAD_REQUEST);
        }
        eventService.record("JOB", job.projectId(), "JOB", String.valueOf(jobId),
            "job.retry", userId, "USER", "任务重试: " + job.jobType(), null);
    }

    private boolean canAccessJob(Long userId, JobResponse job) {
        if (job.projectId() != null) {
            return accessibleProjectIds(userId).contains(job.projectId());
        }
        return job.createdBy() != null && job.createdBy().equals(userId);
    }

    private Set<Long> accessibleProjectIds(Long userId) {
        return bimAssetRepository.listProjects(userId, null).stream()
            .map(p -> p.projectId()).collect(Collectors.toSet());
    }

    private void requireProjectAccess(Long userId, Long projectId) {
        if (!accessibleProjectIds(userId).contains(projectId)) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }
}

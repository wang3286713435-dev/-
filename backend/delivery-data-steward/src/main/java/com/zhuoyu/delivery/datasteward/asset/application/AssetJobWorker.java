package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetJobRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssetJobWorker {

    private static final Logger log = LoggerFactory.getLogger(AssetJobWorker.class);
    private static final int POLL_INTERVAL_SECONDS = 3;

    private final AssetJobRepository jobRepository;
    private final BimAssetRepository bimAssetRepository;
    private final EventApplicationService eventService;
    private ScheduledExecutorService scheduler;

    public AssetJobWorker(AssetJobRepository jobRepository, BimAssetRepository bimAssetRepository,
                           EventApplicationService eventService) {
        this.jobRepository = jobRepository;
        this.bimAssetRepository = bimAssetRepository;
        this.eventService = eventService;
    }

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "asset-job-worker");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::pollAndExecute, 5, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Asset job worker started (poll interval: {}s)", POLL_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            log.info("Asset job worker stopped");
        }
    }

    void pollAndExecute() {
        try {
            List<JobResponse> pendingJobs = jobRepository.findPending(5);
            for (JobResponse job : pendingJobs) {
                try {
                    executeJob(job);
                } catch (Exception e) {
                    log.error("Failed to execute job {}: {}", job.id(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Job worker poll error: {}", e.getMessage());
        }
    }

    void executeJob(JobResponse job) {
        if (!jobRepository.markRunning(job.id())) {
            return; // another worker or manual action already took this job
        }
        log.info("Executing job {}: type={}, targetType={}, targetId={}",
            job.id(), job.jobType(), job.targetType(), job.targetId());

        eventService.record("JOB", job.projectId(), "JOB", String.valueOf(job.id()),
            "job.start", null, "SYSTEM", "任务开始: " + job.jobType(), null);

        try {
            switch (job.jobType()) {
                case "CHECKSUM_CALC" -> executeChecksumCalc(job);
                default -> {
                    log.warn("Unknown job type: {} for job {}", job.jobType(), job.id());
                    failJob(job, "不支持的任务类型: " + job.jobType());
                }
            }
        } catch (Exception e) {
            log.error("Job {} execution error: {}", job.id(), e.getMessage());
            failJob(job, truncate(e.getMessage(), 1000));
        }
    }

    private void executeChecksumCalc(JobResponse job) {
        if (job.targetId() == null || !"FILE_RESOURCE".equals(job.targetType())) {
            failJob(job, "无效的checksum任务参数");
            eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
                job.targetId() != null ? String.valueOf(job.targetId()) : null,
                "checksum.fail", null, "SYSTEM", "checksum任务参数无效: 缺少targetId", null);
            return;
        }

        // Use permission-bypass lookup for system worker
        Long fileId = job.targetId();
        var fileOpt = bimAssetRepository.getFileByIdPlain(fileId);
        if (fileOpt.isEmpty()) {
            failJob(job, "文件资源不存在: " + fileId);
            eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
                String.valueOf(fileId), "checksum.fail", null, "SYSTEM",
                "checksum计算失败: 文件资源不存在", null);
            return;
        }

        var file = fileOpt.get();
        String storagePath = file.storagePath();
        if (storagePath == null || storagePath.isBlank()) {
            failJob(job, "文件存储路径为空");
            eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
                String.valueOf(fileId), "checksum.fail", null, "SYSTEM",
                "checksum计算失败: 存储路径为空", null);
            return;
        }

        String filePath = resolveLocalPath(storagePath);

        java.io.File diskFile = new java.io.File(filePath);
        if (!diskFile.exists() || !diskFile.isFile()) {
            String failureReason = "文件不存在: " + filePath;
            failJob(job, failureReason);
            eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
                String.valueOf(fileId), "checksum.fail", null, "SYSTEM",
                "checksum计算失败: " + failureReason, null);
            return;
        }

        jobRepository.updateProgress(job.id(), 1, 2, "正在计算SHA-256...");

        String checksum = ChecksumApplicationService.computeSha256(filePath);
        if (checksum == null || checksum.isBlank()) {
            String failureReason = "SHA-256计算异常: " + file.fileName();
            failJob(job, failureReason);
            eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
                String.valueOf(fileId), "checksum.fail", null, "SYSTEM",
                "checksum计算失败: " + failureReason, null);
            return;
        }

        jobRepository.updateProgress(job.id(), 2, 2, "写入checksum...");

        // Write checksum back to file resource
        bimAssetRepository.updateChecksum(fileId, checksum);

        jobRepository.markSucceeded(job.id(), "SHA-256计算完成");
        eventService.record("JOB", job.projectId(), "JOB", String.valueOf(job.id()),
            "job.success", null, "SYSTEM", "任务成功: " + job.jobType(), null);
        eventService.record("CHECKSUM", job.projectId(), "FILE_RESOURCE",
            String.valueOf(fileId), "checksum.success", null, "SYSTEM",
            "checksum计算成功: " + file.fileName() + " (SHA-256)", null);
        log.info("Checksum calc succeeded for file {}: {}", fileId, checksum);
    }

    private void failJob(JobResponse job, String reason) {
        jobRepository.markFailed(job.id(), reason);
        eventService.record("JOB", job.projectId(), "JOB", String.valueOf(job.id()),
            "job.fail", null, "SYSTEM", "任务失败: " + job.jobType() + " - " + reason, null);
    }

    static String resolveLocalPath(String storagePath) {
        if (storagePath == null) {
            return null;
        }
        if (!storagePath.startsWith("nas://")) {
            return storagePath;
        }
        try {
            java.net.URI uri = java.net.URI.create(storagePath);
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                return path;
            }
            String authority = uri.getAuthority();
            if (authority != null && !authority.isBlank()) {
                return "/" + authority;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to prefix stripping below for legacy or malformed values.
        }
        String raw = storagePath.substring("nas://".length());
        return raw.startsWith("/") ? raw : "/" + raw;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 3) + "...";
    }
}

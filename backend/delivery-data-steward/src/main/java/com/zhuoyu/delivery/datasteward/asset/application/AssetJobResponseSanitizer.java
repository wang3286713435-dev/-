package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import java.util.regex.Pattern;

final class AssetJobResponseSanitizer {

    private static final Pattern FORBIDDEN_FIELD_OR_PATH = Pattern.compile(
        "(?i)(nas://|smb://|afp://|/Volumes/|/Users/|/tmp/|/private/|/var/|storage_path|storage_uri|storagePath|storageUri|token|secret|password|raw row|select\\s+|insert\\s+|update\\s+|delete\\s+)");

    private AssetJobResponseSanitizer() {
    }

    static JobResponse sanitize(JobResponse job) {
        if (job == null) {
            return null;
        }
        return new JobResponse(
            job.id(),
            job.jobType(),
            job.status(),
            job.projectId(),
            job.targetType(),
            job.targetId(),
            sanitizePayload(job.requestPayload()),
            job.progressCurrent(),
            job.progressTotal(),
            job.progressPercent(),
            sanitizeText(job.progressMessage()),
            sanitizeText(job.failureReason()),
            job.retryCount(),
            job.maxRetries(),
            job.createdBy(),
            job.startedAt(),
            job.completedAt(),
            job.createdAt(),
            job.updatedAt()
        );
    }

    private static String sanitizePayload(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "{\"sanitized\":true,\"reason\":\"path_not_exposable\"}";
    }

    private static String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (!FORBIDDEN_FIELD_OR_PATH.matcher(value).find()) {
            return value;
        }
        if (value.contains("文件不存在")) {
            return "文件不存在，底层路径已隐藏";
        }
        if (value.contains("路径为空") || value.contains("存储路径为空")) {
            return "文件存储位置不可用，底层路径已隐藏";
        }
        return "任务信息已脱敏，底层路径不在前端展示";
    }
}

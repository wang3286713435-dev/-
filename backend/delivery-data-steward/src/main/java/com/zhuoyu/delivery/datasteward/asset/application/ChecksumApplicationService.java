package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.BatchChecksumRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ChecksumJobRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetJobRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecksumApplicationService {

    private final AssetJobRepository jobRepository;
    private final BimAssetRepository bimAssetRepository;
    private final EventApplicationService eventService;

    public ChecksumApplicationService(AssetJobRepository jobRepository, BimAssetRepository bimAssetRepository,
                                       EventApplicationService eventService) {
        this.jobRepository = jobRepository;
        this.bimAssetRepository = bimAssetRepository;
        this.eventService = eventService;
    }

    @Transactional
    public JobResponse createSingleFileChecksum(Long userId, ChecksumJobRequest request) {
        FileAssetResponse file = bimAssetRepository.listFileById(userId, request.fileId()).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException("ASSET_FILE_NOT_FOUND", "文件不存在或无权访问", HttpStatus.NOT_FOUND));

        if (file.checksum() != null && !file.checksum().isBlank()) {
            throw new BusinessException("ASSET_FILE_CHECKSUM_EXISTS", "文件已有checksum，无需重新计算", HttpStatus.BAD_REQUEST);
        }

        Long jobId = jobRepository.insert("CHECKSUM_CALC", file.projectId(),
            "FILE_RESOURCE", request.fileId(), "{\"storagePath\":\"" + escapeJson(file.storagePath()) + "\"}", 3, userId);

        eventService.record("CHECKSUM", file.projectId(), "FILE_RESOURCE", String.valueOf(request.fileId()),
            "checksum.create", userId, "API", "创建checksum任务: " + file.fileName(), null);

        return AssetJobResponseSanitizer.sanitize(jobRepository.requireById(jobId));
    }

    @Transactional
    public int createBatchChecksum(Long userId, BatchChecksumRequest request) {
        requireProjectAccess(userId, request.projectId());

        List<FileAssetResponse> filesWithoutChecksum = bimAssetRepository.listFiles(userId, request.projectId(),
            null, null, null, null, null, null, null, "MISSING_CHECKSUM", 0, 500).stream()
            .filter(f -> f.checksum() == null || f.checksum().isBlank())
            .toList();

        if (filesWithoutChecksum.isEmpty()) {
            return 0;
        }

        int created = 0;
        for (FileAssetResponse file : filesWithoutChecksum) {
            String payload = "{\"storagePath\":\"" + escapeJson(file.storagePath()) + "\"}";
            jobRepository.insert("CHECKSUM_CALC", file.projectId(),
                "FILE_RESOURCE", file.fileId(), payload, 3, userId);
            created++;
        }
        return created;
    }

    public static String computeSha256(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || !file.isFile()) {
                return null;
            }
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (java.io.InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void requireProjectAccess(Long userId, Long projectId) {
        boolean hasAccess = bimAssetRepository.listProjects(userId, null).stream()
            .anyMatch(p -> p.projectId().equals(projectId));
        if (!hasAccess) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityMetric;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetQualityOverviewResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetQualityRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AssetQualityApplicationService {

    private final AssetQualityRepository assetQualityRepository;
    private final BimAssetRepository bimAssetRepository;

    public AssetQualityApplicationService(AssetQualityRepository assetQualityRepository,
                                          BimAssetRepository bimAssetRepository) {
        this.assetQualityRepository = assetQualityRepository;
        this.bimAssetRepository = bimAssetRepository;
    }

    public AssetQualityOverviewResponse getOverview(Long userId, Long projectId, String assetSource) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
        }

        long pendingReviewCount = assetQualityRepository.countPendingReviewCandidates(userId, projectId, assetSource);
        long failedScanCount = assetQualityRepository.countScanTasksByStatus(userId, projectId, assetSource, List.of("FAILED"));
        long runningScanCount = assetQualityRepository.countScanTasksByStatus(userId, projectId, assetSource, List.of("PENDING", "RUNNING"));
        long missingChecksumCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "f.checksum IS NULL OR f.checksum = ''");
        long missingConfidenceCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "f.confidence_level IS NULL OR f.confidence_level = ''");
        long missingDisciplineCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "f.discipline IS NULL OR f.discipline = '' OR f.discipline = 'OTHER'");
        long missingVersionCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "f.version_no IS NULL OR f.version_no = ''");
        long missingStoragePathCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "f.storage_uri IS NULL OR f.storage_uri = ''");
        long zeroSizeFileCount = assetQualityRepository.countFilesByCondition(userId, projectId, assetSource,
            "COALESCE(f.size_bytes, 0) <= 0");
        long nonstandardPendingCount = assetQualityRepository.countNonstandardDirectories(userId, "PENDING_AGENT")
            + assetQualityRepository.countNonstandardDirectories(userId, "HUMAN_REVIEW")
            + assetQualityRepository.countNonstandardDirectories(userId, "DEFERRED");
        long nonstandardApprovedCount = assetQualityRepository.countNonstandardDirectories(userId, "APPROVED_FOR_IMPORT");

        long riskSignalCount = pendingReviewCount
            + failedScanCount
            + missingChecksumCount
            + missingConfidenceCount
            + missingDisciplineCount
            + missingVersionCount
            + missingStoragePathCount
            + zeroSizeFileCount
            + nonstandardPendingCount;

        List<AssetQualityMetric> metrics = List.of(
            metric("PENDING_REVIEW", "待人工审核", "HIGH", pendingReviewCount, "扫描后未能高置信自动入库的文件，需要人工确认项目、类型、专业或版本"),
            metric("FAILED_SCAN", "失败扫描任务", "HIGH", failedScanCount, "扫描任务失败会影响资产完整性，需要查看失败原因后重跑或修复路径"),
            metric("MISSING_STORAGE_PATH", "缺少存储路径", "HIGH", missingStoragePathCount, "文件记录没有可追溯路径，会导致后续找不到文件"),
            metric("ZERO_SIZE_FILE", "零大小文件", "MEDIUM", zeroSizeFileCount, "文件大小为 0 或缺失，通常需要复核 NAS 文件或登记数据"),
            metric("MISSING_CHECKSUM", "缺少 checksum", "MEDIUM", missingChecksumCount, "尚未完成内容指纹补齐，影响重复识别和完整性校验"),
            metric("MISSING_DISCIPLINE", "专业待完善", "MEDIUM", missingDisciplineCount, "专业为空或为其他，影响检索、统计和企业 agent 问答命中"),
            metric("MISSING_VERSION", "版本待完善", "LOW", missingVersionCount, "版本号缺失会影响模型/图纸版本追溯"),
            metric("MISSING_CONFIDENCE", "置信度缺失", "LOW", missingConfidenceCount, "缺少自动入库或人工审核的置信标记"),
            metric("RUNNING_SCAN", "运行中扫描", "INFO", runningScanCount, "正在等待或执行的扫描任务"),
            metric("NONSTANDARD_PENDING", "非标准资料待治理", "MEDIUM", nonstandardPendingCount, "暂缓入库目录仍需 agent 或人工治理"),
            metric("NONSTANDARD_APPROVED", "非标准资料可导入", "INFO", nonstandardApprovedCount, "已治理并允许后续导入的非标准目录")
        );

        return new AssetQualityOverviewResponse(
            riskSignalCount,
            pendingReviewCount,
            failedScanCount,
            runningScanCount,
            missingChecksumCount,
            missingConfidenceCount,
            missingDisciplineCount,
            missingVersionCount,
            missingStoragePathCount,
            zeroSizeFileCount,
            nonstandardPendingCount,
            nonstandardApprovedCount,
            bimAssetRepository.findLastUpdated(userId, projectId, assetSource),
            assetQualityRepository.latestEventAt(userId, projectId, assetSource),
            metrics,
            assetQualityRepository.topRiskProjects(userId, projectId, assetSource, 10),
            assetQualityRepository.recentEvents(userId, projectId, assetSource, 10)
        );
    }

    private void requireProjectAccess(Long userId, Long projectId) {
        boolean hasAccess = bimAssetRepository.listProjects(userId, null).stream()
            .anyMatch(p -> p.projectId().equals(projectId));
        if (!hasAccess) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }

    private static AssetQualityMetric metric(String code, String label, String severity,
                                             long count, String description) {
        return new AssetQualityMetric(code, label, severity, count, description);
    }
}

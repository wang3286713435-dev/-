package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityStatisticsResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class StatisticsApplicationService {

    private final BimAssetRepository bimAssetRepository;

    public StatisticsApplicationService(BimAssetRepository bimAssetRepository) {
        this.bimAssetRepository = bimAssetRepository;
    }

    public CapacityStatisticsResponse getStatistics(Long userId, Long projectId) {
        return getStatistics(userId, projectId, null);
    }

    public CapacityStatisticsResponse getStatistics(Long userId, Long projectId, String assetSource) {
        if (projectId != null) {
            requireProjectAccess(userId, projectId);
        }

        int projectCount = bimAssetRepository.countAssetProjects(userId, projectId, assetSource);
        int fileCount = bimAssetRepository.countAllFiles(userId, projectId, assetSource);
        int modelFileCount = bimAssetRepository.countModelFiles(userId, projectId, assetSource);
        int drawingFileCount = bimAssetRepository.countDrawingFiles(userId, projectId, assetSource);
        long totalSizeBytes = bimAssetRepository.totalAllFileSize(userId, projectId, assetSource);
        var byFileKind = bimAssetRepository.capacityByFileKind(userId, projectId, assetSource);
        var byDiscipline = bimAssetRepository.capacityByDiscipline(userId, projectId, assetSource);
        var topProjects = bimAssetRepository.topProjectCapacity(userId, projectId, assetSource);
        Instant lastUpdatedAt = bimAssetRepository.findLastUpdated(userId, projectId, assetSource);

        return new CapacityStatisticsResponse(
            projectCount, fileCount, modelFileCount, drawingFileCount,
            totalSizeBytes, byFileKind, byDiscipline, topProjects, lastUpdatedAt);
    }

    private void requireProjectAccess(Long userId, Long projectId) {
        boolean hasAccess = bimAssetRepository.listProjects(userId, null).stream()
            .anyMatch(p -> p.projectId().equals(projectId));
        if (!hasAccess) {
            throw new BusinessException(
                "ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }
    }
}

package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetScanTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssetNasScanPatrolWorker {

    private static final Logger log = LoggerFactory.getLogger(AssetNasScanPatrolWorker.class);

    private final AssetPathMappingRepository pathMappingRepository;
    private final AssetScanTaskRepository scanTaskRepository;
    private final AssetApplicationService assetApplicationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${delivery.asset.nas-scan-patrol.enabled:true}")
    private boolean enabled;

    @Value("${delivery.asset.nas-scan-patrol.interval-ms:3600000}")
    private long intervalMs;

    @Value("${delivery.asset.nas-scan-patrol.max-mappings-per-run:10}")
    private int maxMappingsPerRun;

    public AssetNasScanPatrolWorker(
        AssetPathMappingRepository pathMappingRepository,
        AssetScanTaskRepository scanTaskRepository,
        AssetApplicationService assetApplicationService,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.pathMappingRepository = pathMappingRepository;
        this.scanTaskRepository = scanTaskRepository;
        this.assetApplicationService = assetApplicationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(
        initialDelayString = "${delivery.asset.nas-scan-patrol.initial-delay-ms:300000}",
        fixedDelayString = "${delivery.asset.nas-scan-patrol.interval-ms:3600000}"
    )
    public void patrol() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("NAS scan patrol is still running; skip this tick");
            return;
        }
        try {
            Optional<Long> systemUserId = findSystemUserId();
            if (systemUserId.isEmpty()) {
                log.warn("NAS scan patrol skipped because no active admin system user was found");
                return;
            }

            int hardLimit = Math.max(1, Math.min(maxMappingsPerRun, 50));
            int candidateLimit = Math.min(200, Math.max(hardLimit * 4, hardLimit));
            Instant cutoff = Instant.now().minusMillis(Math.max(intervalMs, 60_000L));
            List<PathMappingResponse> candidates = pathMappingRepository.listEnabledPatrolMappings(candidateLimit);

            int started = 0;
            for (PathMappingResponse mapping : candidates) {
                if (started >= hardLimit) {
                    break;
                }
                if (mapping.projectId() == null || mapping.nasPath() == null || mapping.nasPath().isBlank()) {
                    continue;
                }
                if (scanTaskRepository.hasActiveOrRecentMappingScan(mapping.projectId(), mapping.nasPath(), cutoff)) {
                    continue;
                }
                try {
                    assetApplicationService.createAndRunPatrolScan(systemUserId.get(), mapping);
                    started++;
                } catch (Exception exception) {
                    log.warn("NAS scan patrol failed for projectId={} mappingId={}: {}",
                        mapping.projectId(), mapping.id(), exception.getMessage());
                }
            }
            if (started > 0) {
                log.info("NAS scan patrol started {} scan task(s)", started);
            }
        } finally {
            running.set(false);
        }
    }

    private Optional<Long> findSystemUserId() {
        List<Long> ids = jdbcTemplate.query("""
            SELECT id
            FROM core_users
            WHERE deleted = 0
              AND status = 'ACTIVE'
              AND LOWER(username) IN ('admin', 'platform.admin')
            ORDER BY CASE LOWER(username)
                WHEN 'admin' THEN 0
                WHEN 'platform.admin' THEN 1
                ELSE 2
            END
            LIMIT 1
            """, new MapSqlParameterSource(), (rs, rowNum) -> rs.getLong("id"));
        return ids.stream().findFirst();
    }
}

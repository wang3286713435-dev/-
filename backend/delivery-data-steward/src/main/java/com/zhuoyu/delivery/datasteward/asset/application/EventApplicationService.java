package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetEventRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventApplicationService {

    private final AssetEventRepository eventRepository;
    private final BimAssetRepository bimAssetRepository;

    public EventApplicationService(AssetEventRepository eventRepository, BimAssetRepository bimAssetRepository) {
        this.eventRepository = eventRepository;
        this.bimAssetRepository = bimAssetRepository;
    }

    @Transactional
    public long record(String eventType, Long projectId, String aggregateType, String aggregateId,
                        String actionCode, Long operatorId, String sourceType,
                        String summary, String payloadJson) {
        return eventRepository.insert(eventType, projectId, aggregateType, aggregateId,
            actionCode, operatorId, sourceType, summary, payloadJson, TraceIdHolder.getTraceId());
    }

    public List<EventResponse> queryEvents(Long userId, Long afterEventId, Instant fromTime, Instant toTime,
                                            Long projectId, String eventType, String actionCode, Integer limit) {
        List<Long> accessibleProjectIds = bimAssetRepository.listProjects(userId, null).stream()
            .map(p -> p.projectId()).toList();

        if (projectId != null && !accessibleProjectIds.contains(projectId)) {
            throw new BusinessException("ASSET_PROJECT_ACCESS_DENIED", "无权访问该项目", HttpStatus.FORBIDDEN);
        }

        List<EventResponse> events = eventRepository.query(afterEventId, fromTime, toTime,
            projectId, eventType, actionCode, limit != null ? limit : 50);

        // Filter by accessible projects: project-scoped events require project permission.
        // Global events (projectId == null) only visible to the operator who created them.
        // Global events with null operatorId are never returned.
        return events.stream()
            .filter(e -> {
                if (e.projectId() != null) {
                    return accessibleProjectIds.contains(e.projectId());
                }
                return e.operatorId() != null && e.operatorId().equals(userId);
            })
            .toList();
    }
}

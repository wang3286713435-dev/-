package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.EventApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/assets/events")
public class EventController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final EventApplicationService eventApplicationService;

    public EventController(SecurityPrincipalAccessor securityPrincipalAccessor,
                            EventApplicationService eventApplicationService) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.eventApplicationService = eventApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> queryEvents(
            @RequestParam(required = false) Long afterEventId,
            @RequestParam(required = false) Long fromTime,
            @RequestParam(required = false) Long toTime,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) Integer limit) {
        Instant from = fromTime != null ? Instant.ofEpochMilli(fromTime) : null;
        Instant to = toTime != null ? Instant.ofEpochMilli(toTime) : null;
        return ApiResponse.success(
            eventApplicationService.queryEvents(currentUserId(), afterEventId, from, to,
                projectId, eventType, actionCode, limit));
    }
}

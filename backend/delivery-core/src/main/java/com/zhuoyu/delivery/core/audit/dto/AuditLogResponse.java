package com.zhuoyu.delivery.core.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
    Long id,
    Long projectId,
    String moduleCode,
    String actionCode,
    String targetType,
    String targetId,
    Long operatorId,
    String traceId,
    String detailsJson,
    Instant createdAt
) {
}

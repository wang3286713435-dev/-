package com.zhuoyu.delivery.masterdata.nodetype.dto;

import java.time.Instant;

public record NodeTypeResponse(
    Long id,
    Long projectId,
    String code,
    String name,
    Integer scopeLevel,
    Integer sortOrder,
    String status,
    Boolean locked,
    Instant lockedAt,
    Long lockedBy
) {
}

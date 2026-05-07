package com.zhuoyu.delivery.masterdata.nodetype.domain;

import java.time.Instant;

public record NodeType(
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

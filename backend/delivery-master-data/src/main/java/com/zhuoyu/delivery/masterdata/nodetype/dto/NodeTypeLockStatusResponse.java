package com.zhuoyu.delivery.masterdata.nodetype.dto;

import java.time.Instant;

public record NodeTypeLockStatusResponse(
    Long projectId,
    Long nodeTypeId,
    Boolean locked,
    Instant lockedAt,
    Long lockedBy,
    Boolean hasNodeTypes,
    Boolean allNodeTypesLocked,
    Integer nodeTypeCount
) {
}

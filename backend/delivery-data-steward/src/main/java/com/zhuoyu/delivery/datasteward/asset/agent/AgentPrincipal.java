package com.zhuoyu.delivery.datasteward.asset.agent;

import java.util.Collections;
import java.util.List;

public record AgentPrincipal(
    Long apiKeyId,
    String keyName,
    String scopeType,
    List<Long> authorizedProjectIds,
    Long createdBy
) {
    public AgentPrincipal {
        authorizedProjectIds = authorizedProjectIds == null
            ? Collections.emptyList()
            : List.copyOf(authorizedProjectIds);
    }

    public boolean canAccessProject(Long projectId) {
        if (projectId == null) return false;
        return "ALL_PROJECTS".equals(scopeType)
            || authorizedProjectIds.contains(projectId);
    }
}

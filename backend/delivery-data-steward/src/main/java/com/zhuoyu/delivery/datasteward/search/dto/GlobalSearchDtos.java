package com.zhuoyu.delivery.datasteward.search.dto;

import java.util.List;
import java.util.Map;

public final class GlobalSearchDtos {

    private GlobalSearchDtos() {
    }

    public record GlobalSearchResponse(
        String keyword,
        Long projectId,
        int totalCount,
        List<GlobalSearchGroup> groups
    ) {
    }

    public record GlobalSearchGroup(
        String type,
        String label,
        int count,
        List<GlobalSearchItem> items
    ) {
    }

    public record GlobalSearchItem(
        String type,
        String id,
        Long projectId,
        String projectCode,
        String projectName,
        String title,
        String subtitle,
        String status,
        String routeName,
        Map<String, Object> routeParams,
        Map<String, Object> routeQuery
    ) {
    }
}

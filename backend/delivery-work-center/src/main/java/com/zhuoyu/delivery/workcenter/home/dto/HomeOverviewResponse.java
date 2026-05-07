package com.zhuoyu.delivery.workcenter.home.dto;

import java.util.List;

public record HomeOverviewResponse(
    Long projectId,
    String projectCode,
    String projectName,
    List<MetricItem> metrics,
    List<String> notices
) {
    public record MetricItem(String label, int value, String unit) {
    }
}

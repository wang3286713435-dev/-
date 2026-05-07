package com.zhuoyu.delivery.core.project.dto;

public record ProjectSummaryResponse(
    Long id,
    String code,
    String name,
    String industryType,
    String status,
    String roleCode,
    String roleName
) {
}

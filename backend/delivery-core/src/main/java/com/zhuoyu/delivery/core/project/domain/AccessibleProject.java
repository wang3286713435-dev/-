package com.zhuoyu.delivery.core.project.domain;

public record AccessibleProject(
    Long id,
    String code,
    String name,
    String industryType,
    String status,
    String roleCode,
    String roleName
) {
}

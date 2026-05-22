package com.zhuoyu.delivery.core.user.dto;

public record EmployeeProjectRoleAssignment(
    Long projectId,
    String projectCode,
    String projectName,
    String roleCode,
    String roleName
) {
}

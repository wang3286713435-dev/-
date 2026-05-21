package com.zhuoyu.delivery.core.user.dto;

import java.time.Instant;
import java.util.List;

public record EmployeeDetailResponse(
    Long userId,
    String username,
    String phoneNumber,
    String displayName,
    String departmentName,
    String status,
    Instant lastLoginAt,
    Instant createdAt,
    Instant updatedAt,
    List<EmployeeProjectRoleAssignment> projectRoles
) {
}

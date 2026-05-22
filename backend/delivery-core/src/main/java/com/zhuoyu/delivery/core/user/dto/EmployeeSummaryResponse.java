package com.zhuoyu.delivery.core.user.dto;

import java.time.Instant;

public record EmployeeSummaryResponse(
    Long userId,
    String username,
    String phoneNumber,
    String displayName,
    String departmentName,
    String status,
    Integer projectCount,
    Instant lastLoginAt,
    Instant createdAt,
    Instant updatedAt
) {
}

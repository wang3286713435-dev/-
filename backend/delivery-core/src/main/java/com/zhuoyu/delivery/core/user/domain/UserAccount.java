package com.zhuoyu.delivery.core.user.domain;

public record UserAccount(
    Long id,
    String username,
    String passwordHash,
    String displayName,
    String status,
    String phoneNumber,
    String departmentName,
    java.time.Instant lastLoginAt
) {
}

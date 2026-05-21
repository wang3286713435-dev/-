package com.zhuoyu.delivery.core.auth.dto;

public record RegisterResponse(
    Long userId,
    String username,
    String phoneNumber,
    String displayName,
    String departmentName,
    String status,
    boolean projectAuthorized
) {
}

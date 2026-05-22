package com.zhuoyu.delivery.core.user.dto;

public record AssignableProjectResponse(
    Long id,
    String code,
    String name,
    String industryType,
    String status
) {
}

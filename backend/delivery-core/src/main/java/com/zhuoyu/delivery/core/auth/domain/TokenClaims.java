package com.zhuoyu.delivery.core.auth.domain;

public record TokenClaims(
    Long userId,
    String username,
    Long currentProjectId,
    String tokenType
) {
}

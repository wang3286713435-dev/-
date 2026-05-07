package com.zhuoyu.delivery.core.auth.domain;

public record AuthenticatedPrincipal(
    Long userId,
    String username,
    Long currentProjectId
) {
}

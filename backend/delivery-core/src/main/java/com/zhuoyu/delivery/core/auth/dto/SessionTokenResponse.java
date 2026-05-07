package com.zhuoyu.delivery.core.auth.dto;

import java.time.Instant;

public record SessionTokenResponse(
    String tokenType,
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt,
    Long currentProjectId
) {
}

package com.zhuoyu.delivery.core.auth.infrastructure;

import com.zhuoyu.delivery.core.auth.domain.TokenClaims;
import com.zhuoyu.delivery.core.auth.dto.SessionTokenResponse;
import com.zhuoyu.delivery.core.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_PROJECT_ID = "projectId";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String ACCESS = "ACCESS";
    private static final String REFRESH = "REFRESH";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public SessionTokenResponse issueTokens(Long userId, String username, Long currentProjectId) {
        Instant accessExpiresAt = Instant.now().plus(properties.getAccessTokenTtl());
        Instant refreshExpiresAt = Instant.now().plus(properties.getRefreshTokenTtl());
        return new SessionTokenResponse(
            "Bearer",
            buildToken(userId, username, currentProjectId, ACCESS, accessExpiresAt),
            accessExpiresAt,
            buildToken(userId, username, currentProjectId, REFRESH, refreshExpiresAt),
            refreshExpiresAt,
            currentProjectId
        );
    }

    public TokenClaims parseAccessToken(String token) {
        return parseToken(token, ACCESS);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parseToken(token, REFRESH);
    }

    private String buildToken(Long userId, String username, Long currentProjectId, String tokenType, Instant expiresAt) {
        return Jwts.builder()
            .issuer(properties.getIssuer())
            .subject(String.valueOf(userId))
            .claim(CLAIM_USERNAME, username)
            .claim(CLAIM_PROJECT_ID, currentProjectId)
            .claim(CLAIM_TOKEN_TYPE, tokenType)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();
    }

    private TokenClaims parseToken(String token, String expectedType) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("invalid token type");
        }
        Number projectId = claims.get(CLAIM_PROJECT_ID, Number.class);
        return new TokenClaims(
            Long.parseLong(claims.getSubject()),
            claims.get(CLAIM_USERNAME, String.class),
            projectId == null ? null : projectId.longValue(),
            tokenType
        );
    }
}

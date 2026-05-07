package com.zhuoyu.delivery.core.auth.controller;

import com.zhuoyu.delivery.core.auth.application.AuthApplicationService;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.auth.dto.LoginRequest;
import com.zhuoyu.delivery.core.auth.dto.RefreshTokenRequest;
import com.zhuoyu.delivery.core.auth.dto.SessionTokenResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final SecurityPrincipalAccessor securityPrincipalAccessor;

    public AuthController(
        AuthApplicationService authApplicationService,
        SecurityPrincipalAccessor securityPrincipalAccessor
    ) {
        this.authApplicationService = authApplicationService;
        this.securityPrincipalAccessor = securityPrincipalAccessor;
    }

    @GetMapping("/system/health")
    public ApiResponse<Object> health() {
        return ApiResponse.success(java.util.Map.of("status", "UP", "service", "delivery-app"));
    }

    @PostMapping("/auth/login")
    public ApiResponse<SessionTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authApplicationService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<SessionTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authApplicationService.refresh(request));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        authApplicationService.logout(principal.userId(), principal.currentProjectId());
        return ApiResponse.success();
    }
}

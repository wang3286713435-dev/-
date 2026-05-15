package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.AgentApiKeyApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyCreateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentApiKeyResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/agent/api-keys")
public class ApiKeyController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AgentApiKeyApplicationService apiKeyService;

    public ApiKeyController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        AgentApiKeyApplicationService apiKeyService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.apiKeyService = apiKeyService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    @PostMapping
    public ApiResponse<AgentApiKeyCreateResponse> createApiKey(
        @Valid @RequestBody AgentApiKeyCreateRequest request
    ) {
        return ApiResponse.success(apiKeyService.createKey(currentUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<AgentApiKeyResponse>> listApiKeys() {
        return ApiResponse.success(apiKeyService.listKeys(currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AgentApiKeyResponse> getApiKey(@PathVariable Long id) {
        return ApiResponse.success(apiKeyService.getKey(currentUserId(), id));
    }

    @PostMapping("/{id}:revoke")
    public ApiResponse<Void> revokeApiKey(@PathVariable Long id) {
        apiKeyService.revokeKey(currentUserId(), id);
        return ApiResponse.success();
    }
}

package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesCapabilitiesResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesHealthResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal compatibility endpoints. New frontend integrations must use /api/data-steward/*.
 */
@RestController
@RequestMapping("/api/agent/hermes")
@Deprecated(forRemoval = false)
public class AgentGatewayController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AgentGatewayApplicationService agentGatewayApplicationService;

    public AgentGatewayController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        AgentGatewayApplicationService agentGatewayApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.agentGatewayApplicationService = agentGatewayApplicationService;
    }

    @GetMapping("/capabilities")
    public ApiResponse<HermesCapabilitiesResponse> capabilities() {
        return ApiResponse.success(agentGatewayApplicationService.capabilities());
    }

    @GetMapping("/health")
    public ApiResponse<HermesHealthResponse> health() {
        return ApiResponse.success(agentGatewayApplicationService.health());
    }

    @PostMapping("/chat")
    public ApiResponse<HermesChatResponse> chat(@Valid @RequestBody HermesChatRequest request) {
        return ApiResponse.success(agentGatewayApplicationService.chat(
            securityPrincipalAccessor.requireCurrentPrincipal(),
            request
        ));
    }
}

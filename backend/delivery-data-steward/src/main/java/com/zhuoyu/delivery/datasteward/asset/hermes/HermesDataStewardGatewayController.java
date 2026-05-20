package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.datasteward.asset.application.CatalogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.DataStewardChatRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesCapabilitiesResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesHealthResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward")
public class HermesDataStewardGatewayController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AgentGatewayApplicationService agentGatewayApplicationService;
    private final CatalogApplicationService catalogApplicationService;

    public HermesDataStewardGatewayController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        AgentGatewayApplicationService agentGatewayApplicationService,
        CatalogApplicationService catalogApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.agentGatewayApplicationService = agentGatewayApplicationService;
        this.catalogApplicationService = catalogApplicationService;
    }

    @GetMapping("/hermes/health")
    public ApiResponse<HermesHealthResponse> health() {
        return ApiResponse.success(agentGatewayApplicationService.health());
    }

    @GetMapping("/hermes/capabilities")
    public ApiResponse<HermesCapabilitiesResponse> capabilities() {
        return ApiResponse.success(agentGatewayApplicationService.capabilities());
    }

    @PostMapping("/chat")
    public ApiResponse<HermesChatResponse> chat(@Valid @RequestBody DataStewardChatRequest request) {
        AuthenticatedPrincipal principal = securityPrincipalAccessor.requireCurrentPrincipal();
        if (request.normalizedQuestion().isBlank()) {
            throw new BusinessException("AGENT_MESSAGE_REQUIRED", "问题不能为空", HttpStatus.BAD_REQUEST);
        }
        Long projectId = request.projectId() == null
            ? catalogApplicationService.resolveAccessibleProjectFilter(principal.userId(), request.projectFilters())
            : request.projectId();
        HermesChatRequest normalized = request.toHermesChatRequest(projectId);
        return ApiResponse.success(agentGatewayApplicationService.chat(principal, normalized));
    }
}

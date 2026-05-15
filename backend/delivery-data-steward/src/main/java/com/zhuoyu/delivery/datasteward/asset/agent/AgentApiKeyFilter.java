package com.zhuoyu.delivery.datasteward.asset.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuoyu.delivery.datasteward.asset.application.AgentApiKeyApplicationService;
import com.zhuoyu.delivery.datasteward.asset.repository.AgentApiKeyRepository;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.trace.TraceIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AgentApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentApiKeyFilter.class);
    private static final String AGENT_PATH_PREFIX = "/api/data-steward/agent";
    private static final String API_KEY_HEADER = "X-Agent-Api-Key";

    private final AgentApiKeyApplicationService apiKeyService;
    private final AgentApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;

    public AgentApiKeyFilter(
        AgentApiKeyApplicationService apiKeyService,
        AgentApiKeyRepository apiKeyRepository,
        ObjectMapper objectMapper
    ) {
        this.apiKeyService = apiKeyService;
        this.apiKeyRepository = apiKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // Only intercept agent paths
        if (!requestPath.startsWith(AGENT_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // API key management paths (list, create, get, revoke) use JWT auth, not agent key
        // These are under /api/data-steward/agent/api-keys (with optional /{id} or /{id}:revoke suffix)
        if (requestPath.startsWith(AGENT_PATH_PREFIX + "/api-keys")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, "AGENT_KEY_MISSING", "缺少 X-Agent-Api-Key 请求头");
            return;
        }

        String keyHash = apiKeyService.hashKey(apiKey);
        Long keyId = apiKeyService.findKeyIdByHash(keyHash);
        if (keyId == null) {
            writeError(response, "AGENT_KEY_INVALID", "API Key 无效");
            return;
        }

        if (!apiKeyRepository.isValidKey(keyId)) {
            writeError(response, "AGENT_KEY_EXPIRED", "API Key 已过期或已撤销");
            return;
        }

        // Load key details for authorization
        var keyOpt = apiKeyRepository.findById(keyId);
        if (keyOpt.isEmpty()) {
            writeError(response, "AGENT_KEY_INVALID", "API Key 无效");
            return;
        }

        var key = keyOpt.get();
        List<Long> projectIds = apiKeyRepository.findAuthorizedProjectIds(keyId);

        AgentPrincipal agentPrincipal = new AgentPrincipal(
            keyId, key.keyName(), key.scopeType(), projectIds, key.createdBy());

        var authentication = new UsernamePasswordAuthenticationToken(
            agentPrincipal, apiKey, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Update last used
        apiKeyService.touchLastUsed(keyId, request.getRemoteAddr());

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
    }
}

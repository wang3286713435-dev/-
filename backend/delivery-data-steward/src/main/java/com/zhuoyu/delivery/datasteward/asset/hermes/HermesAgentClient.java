package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesCitation;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesMissingEvidence;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOperationAction;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOperationPlan;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOutboundRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesPermissionResult;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesTrace;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HermesAgentClient {

    private final HermesGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public HermesAgentClient(HermesGatewayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public HermesChatResponse chat(HermesOutboundRequest request) {
        RestTemplate restTemplate = restTemplate();
        URI uri = URI.create(stripTrailingSlash(properties.getBaseUrl()) + normalizePath(properties.getChatPath()));
        if (!isAllowedLocalTarget(uri)) {
            return null;
        }
        try {
            if (isOpenAiCompatible()) {
                Map<?, ?> response = restTemplate.postForObject(uri, new org.springframework.http.HttpEntity<>(
                    openAiChatRequest(request),
                    headers()
                ), Map.class);
                return openAiChatResponse(request, response);
            }
            return restTemplate.postForObject(uri, new org.springframework.http.HttpEntity<>(
                request,
                headers()
            ), HermesChatResponse.class);
        } catch (RestClientException ex) {
            return null;
        }
    }

    private Map<String, Object> openAiChatRequest(HermesOutboundRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", safeModel());
        body.put("stream", false);
        body.put("messages", List.of(
            Map.of(
                "role", "system",
                "content", openAiSystemPrompt(request)
            ),
            Map.of(
                "role", "user",
                "content", stringValue(request.query().get("text"))
            )
        ));
        return body;
    }

    private String openAiSystemPrompt(HermesOutboundRequest request) {
        Map<String, Object> platformContext = new LinkedHashMap<>();
        platformContext.put("request_id", request.requestId());
        platformContext.put("timestamp", request.timestamp() == null ? Instant.now().toString() : request.timestamp().toString());
        platformContext.put("user_context", request.userContext());
        platformContext.put("page_context", request.pageContext());
        platformContext.put("permission_context", request.permissionContext());
        platformContext.put("query", request.query());
        platformContext.put("response_requirements", request.responseRequirements());
        return """
            你是 Hermes Agent，正在通过数字化交付平台后端网关提供只读目录级辅助。
            必须遵守：
            1. 只基于 platform_context 中的资产目录、项目上下文和权限证明回答。
            2. 不得声称已读取 PDF、Office、DWG、RVT、IFC 或其他文件正文。
            3. 不得暴露密钥、token、SQL、raw row、request_id、trace id、字段名或内部配置。
            4. 不得执行数据库、NAS、审批、整改、索引或写入动作。
            5. 如问题需要正文证据，必须说明当前缺少可引用正文证据。
            6. 只有当 platform_context.page_context.project_path_context.path_answer_allowed=true 时，才可以回答 controlled_paths 中的 display_path/path_hint；不得推断或展开底层路径。
            7. 用简洁中文回答，避免输出 JSON。

            platform_context:
            %s
            """.formatted(toJson(platformContext));
    }

    private HermesChatResponse openAiChatResponse(HermesOutboundRequest request, Map<?, ?> response) {
        String answer = extractOpenAiAnswer(response);
        if (answer.isBlank()) {
            return null;
        }
        return new HermesChatResponse(
            "catalog_only",
            "catalog_only",
            true,
            request.requestId(),
            request.requestId(),
            stringValue(request.pageContext().get("source_view")),
            parseRefId(request.pageContext().get("asset_ref"), "asset:", "FileAssetView".equals(stringValue(request.pageContext().get("source_view")))),
            parseRefId(request.pageContext().get("asset_ref"), "asset:", "ModelAssetView".equals(stringValue(request.pageContext().get("source_view")))),
            List.of(),
            answer,
            List.of(new HermesCitation(
                "catalog_metadata",
                stringValue(request.pageContext().get("source_view")),
                stringValue(request.pageContext().get("asset_ref")),
                stringValue(request.pageContext().get("project_ref")),
                "Hermes OpenAI-compatible catalog answer",
                true
            )),
            new HermesPermissionResult(
                stringValue(request.permissionContext().getOrDefault("permission_status", "allowed")),
                true,
                Boolean.TRUE.equals(request.permissionContext().get("permission_tags_checked")),
                false,
                null
            ),
            List.of(),
            new HermesOperationPlan(
                true,
                true,
                List.of(new HermesOperationAction("manual_review_required", "draft_only"))
            ),
            new HermesTrace(request.requestId(), "openai_compatible_catalog_only", false)
        );
    }

    private String extractOpenAiAnswer(Map<?, ?> response) {
        if (response == null) {
            return "";
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstChoice = choices.getFirst();
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return "";
        }
        Object messageValue = choice.get("message");
        if (!(messageValue instanceof Map<?, ?> message)) {
            return "";
        }
        return stringValue(message.get("content")).trim();
    }

    private boolean isOpenAiCompatible() {
        return "openai_compatible".equalsIgnoreCase(properties.getMode())
            || normalizePath(properties.getChatPath()).startsWith("/v1/");
    }

    public HermesHealthProbe health() {
        URI uri = URI.create(stripTrailingSlash(properties.getBaseUrl()) + normalizePath(properties.getHealthPath()));
        if (!isAllowedLocalTarget(uri)) {
            return new HermesHealthProbe(false, "TARGET_NOT_ALLOWED");
        }
        try {
            restTemplate().getForObject(uri, Map.class);
            if (isOpenAiCompatible()) {
                if (properties.getServiceToken() == null || properties.getServiceToken().isBlank()) {
                    return new HermesHealthProbe(false, "HERMES_TOKEN_MISSING");
                }
                URI modelsUri = URI.create(stripTrailingSlash(properties.getBaseUrl()) + "/v1/models");
                restTemplate().exchange(modelsUri, HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
            }
            return new HermesHealthProbe(true, "");
        } catch (RestClientException ex) {
            return new HermesHealthProbe(false, "HERMES_UNAVAILABLE_OR_AUTH_FAILED");
        }
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(Math.min(properties.getTimeout().toMillis(), Integer.MAX_VALUE));
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return new RestTemplate(requestFactory);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getServiceToken() != null && !properties.getServiceToken().isBlank()) {
            headers.setBearerAuth(properties.getServiceToken());
        }
        return headers;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8642";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/v1/chat/completions";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private String safeModel() {
        return properties.getModel() == null || properties.getModel().isBlank()
            ? "hermes-agent"
            : properties.getModel().trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long parseRefId(Object value, String prefix, boolean enabled) {
        if (!enabled) {
            return null;
        }
        String text = stringValue(value);
        if (!text.startsWith(prefix)) {
            return null;
        }
        try {
            return Long.parseLong(text.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isAllowedLocalTarget(URI uri) {
        String host = uri.getHost();
        return "localhost".equalsIgnoreCase(host)
            || "127.0.0.1".equals(host)
            || "::1".equals(host);
    }

    public record HermesHealthProbe(
        boolean available,
        String unavailableReason
    ) {
    }
}

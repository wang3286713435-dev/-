package com.zhuoyu.delivery.datasteward.asset.hermes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "delivery.hermes-agent-gateway")
public class HermesGatewayProperties {

    private boolean enabled = false;
    private String mode = "local_test";
    private String baseUrl = "http://127.0.0.1:8000";
    private String chatPath = "/api/data-steward/agent/chat";
    private String healthPath = "/health";
    private String model = "hermes-agent";
    private Duration timeout = Duration.ofSeconds(30);
    private boolean readonly = true;
    private boolean catalogOnlyDefault = true;
    private String contractVersion = "delivery_platform.asset_views.v1.1";
    private String serviceToken = "";
    private List<String> allowedActions = new ArrayList<>(List.of(
        "catalog_query",
        "agent_catalog_assist",
        "operation_plan_draft"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatPath() {
        return chatPath;
    }

    public void setChatPath(String chatPath) {
        this.chatPath = chatPath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isCatalogOnlyDefault() {
        return catalogOnlyDefault;
    }

    public void setCatalogOnlyDefault(boolean catalogOnlyDefault) {
        this.catalogOnlyDefault = catalogOnlyDefault;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public void setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions == null ? new ArrayList<>() : new ArrayList<>(allowedActions);
    }
}

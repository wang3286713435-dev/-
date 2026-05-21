package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class HermesGatewayDtos {

    private HermesGatewayDtos() {
    }

    public record HermesCapabilitiesResponse(
        String agentName,
        String mode,
        String contractVersion,
        HermesSupports supports,
        HermesSafety safety,
        HermesAuthorityHealth authorityHealth
    ) {
    }

    public record HermesHealthResponse(
        String status,
        Boolean hermesAvailable,
        String mode,
        String contractVersion,
        Boolean gatewayEnabled,
        Boolean readonly,
        Boolean runtimeWriteEnabled,
        Boolean agentAnswerIntegrationEnabled,
        String unavailableReason,
        Instant checkedAt,
        HermesAuthorityHealth authorityHealth
    ) {
    }

    public record HermesAuthorityHealth(
        String safetyHealth,
        String capabilityHealth,
        String architectureAuthorityHealth,
        String mode
    ) {
    }

    public record HermesSupports(
        Boolean catalogQuery,
        Boolean missingEvidence,
        Boolean operationPlanDraft,
        Boolean documentContentAnswer,
        Boolean dbCrud,
        Boolean nasCrud,
        Boolean fullBimParse,
        Boolean productionRollout
    ) {
    }

    public record HermesSafety(
        Boolean failClosed,
        Boolean requiresProjectScope,
        Boolean requiresCitationForContentAnswer
    ) {
    }

    public record HermesChatRequest(
        @NotBlank(message = "pageType 不能为空")
        String pageType,
        Long projectId,
        Long assetId,
        String sourceView,
        String currentRoute,
        String projectCode,
        String projectName,
        String pageTitle,
        @JsonAlias("session_id")
        String sessionId,
        @JsonAlias("thread_id")
        String threadId,
        @JsonAlias("previous_response_id")
        String previousResponseId,
        @JsonAlias("sanitized_context_refs")
        List<Map<String, Object>> sanitizedContextRefs,
        @NotBlank(message = "question 不能为空")
        String question
    ) {
    }

    public record DataStewardChatRequest(
        @JsonAlias("session_id")
        String sessionId,
        @JsonAlias("thread_id")
        String threadId,
        @JsonAlias("previous_response_id")
        String previousResponseId,
        @JsonAlias("sanitized_context_refs")
        List<Map<String, Object>> sanitizedContextRefs,
        String message,
        @JsonAlias("project_filters")
        List<String> projectFilters,
        String mode,
        String pageType,
        Long projectId,
        Long assetId,
        String sourceView,
        String currentRoute,
        String projectCode,
        String projectName,
        String pageTitle,
        String question
    ) {
        public String normalizedQuestion() {
            if (question != null && !question.isBlank()) {
                return question.trim();
            }
            return message == null ? "" : message.trim();
        }

        public HermesChatRequest toHermesChatRequest(Long resolvedProjectId) {
            return new HermesChatRequest(
                pageType == null || pageType.isBlank() ? "data_steward_v3_gateway" : pageType.trim(),
                resolvedProjectId,
                assetId,
                sourceView == null || sourceView.isBlank() ? "ProjectAssetView" : sourceView.trim(),
                currentRoute == null ? "" : currentRoute.trim(),
                projectCode == null ? "" : projectCode.trim(),
                projectName == null ? "" : projectName.trim(),
                pageTitle == null ? "" : pageTitle.trim(),
                sessionId == null ? "" : sessionId.trim(),
                threadId == null ? "" : threadId.trim(),
                previousResponseId == null ? "" : previousResponseId.trim(),
                sanitizedContextRefs == null ? List.of() : sanitizedContextRefs,
                normalizedQuestion()
            );
        }
    }

    public record HermesChatResponse(
        String responseId,
        String status,
        String evidenceMode,
        Boolean assetCatalogOnly,
        String queryId,
        String traceId,
        String sourceView,
        Long fileId,
        Long modelId,
        List<HermesPathHint> pathHints,
        String answer,
        List<HermesCitation> citations,
        HermesPermissionResult permission,
        List<HermesMissingEvidence> missingEvidence,
        HermesOperationPlan operationPlan,
        HermesTrace trace,
        String sessionRef,
        String threadRef,
        String previousResponseRef,
        HermesAuthorityHealth authorityHealth,
        List<Map<String, Object>> safeMemoryCandidates,
        List<Map<String, Object>> sanitizedContextRefs
    ) {
    }

    public record HermesPathHint(
        String displayPath,
        String pathHint,
        String provider,
        String matchStrategy
    ) {
    }

    public record HermesCitation(
        String citationType,
        String sourceView,
        String assetRef,
        String projectRef,
        String displayLabel,
        Boolean safeToOpen
    ) {
    }

    public record HermesPermissionResult(
        String permissionStatus,
        Boolean projectScopeChecked,
        Boolean permissionTagsChecked,
        Boolean failClosedApplied,
        String reasonCode
    ) {
    }

    public record HermesMissingEvidence(
        String reason,
        String message
    ) {
    }

    public record HermesOperationPlan(
        Boolean available,
        Boolean requiresHumanApproval,
        List<HermesOperationAction> actions
    ) {
    }

    public record HermesOperationAction(
        String actionType,
        String status
    ) {
    }

    public record HermesTrace(
        String requestId,
        String agentMode,
        Boolean productionRollout
    ) {
    }

    public record HermesOutboundRequest(
        String requestId,
        Instant timestamp,
        Map<String, Object> userContext,
        Map<String, Object> pageContext,
        Map<String, Object> permissionContext,
        Map<String, Object> query,
        Map<String, Object> responseRequirements
    ) {
    }
}

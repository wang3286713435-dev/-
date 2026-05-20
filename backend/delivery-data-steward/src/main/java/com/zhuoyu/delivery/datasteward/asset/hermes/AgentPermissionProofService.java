package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.datasteward.asset.hermes.AgentAssetContextResolver.AgentAssetContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AgentPermissionProofService {

    private static final List<String> REQUIRED_ACTIONS = List.of("catalog_query", "agent_catalog_assist");
    private static final Set<String> SAFE_ACTIONS = Set.of(
        "catalog_query",
        "agent_catalog_assist",
        "operation_plan_draft"
    );

    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final HermesGatewayProperties properties;

    public AgentPermissionProofService(
        ProjectAccessApplicationService projectAccessApplicationService,
        HermesGatewayProperties properties
    ) {
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.properties = properties;
    }

    public PermissionProof build(Long userId, AgentAssetContext context) {
        List<String> safeAllowedActions = safeAllowedActions();
        if (!context.allowed()) {
            return PermissionProof.denied(context.projectId(), context.sourceView(), safeAllowedActions,
                context.denialReasonCode(), context.denialReasonText(), context.permissionTagsPresent());
        }
        if (!properties.isReadonly()) {
            return PermissionProof.denied(context.projectId(), context.sourceView(), safeAllowedActions,
                "GATEWAY_NOT_READONLY", "Gateway 未处于只读模式", context.permissionTagsPresent());
        }
        if (!safeAllowedActions.containsAll(REQUIRED_ACTIONS)) {
            return PermissionProof.denied(context.projectId(), context.sourceView(), safeAllowedActions,
                "REQUIRED_ACTIONS_MISSING", "缺少目录查询所需动作", context.permissionTagsPresent());
        }
        try {
            projectAccessApplicationService.requireAccessibleProject(userId, context.projectId());
        } catch (RuntimeException ex) {
            return PermissionProof.denied(context.projectId(), context.sourceView(), safeAllowedActions,
                "PROJECT_SCOPE_DENIED", "当前用户无权访问该项目", context.permissionTagsPresent());
        }
        return new PermissionProof(
            true,
            "SPECIFIC_PROJECTS",
            List.of(projectRef(context.projectId())),
            safeAllowedActions,
            Instant.now().plus(30, ChronoUnit.MINUTES),
            "platform_agent_gateway",
            context.permissionTagsPresent(),
            null,
            null
        );
    }

    private List<String> safeAllowedActions() {
        return properties.getAllowedActions().stream()
            .filter(SAFE_ACTIONS::contains)
            .distinct()
            .toList();
    }

    private static String projectRef(Long projectId) {
        return projectId == null ? "project:unknown" : "project:" + projectId;
    }

    public record PermissionProof(
        boolean allowed,
        String scopeType,
        List<String> authorizedProjectRefs,
        List<String> allowedActions,
        Instant expiresAt,
        String issuedBy,
        boolean permissionTagsChecked,
        String denialReasonCode,
        String denialReasonText
    ) {
        static PermissionProof denied(
            Long projectId,
            String sourceView,
            List<String> allowedActions,
            String denialReasonCode,
            String denialReasonText,
            boolean permissionTagsChecked
        ) {
            return new PermissionProof(false, "SPECIFIC_PROJECTS",
                projectId == null ? List.of() : List.of(projectRef(projectId)),
                allowedActions == null ? List.of() : List.copyOf(allowedActions),
                Instant.now(),
                "platform_agent_gateway",
                permissionTagsChecked,
                denialReasonCode == null ? "DENIED" : denialReasonCode,
                denialReasonText == null ? "权限校验未通过" : denialReasonText
            );
        }
    }
}

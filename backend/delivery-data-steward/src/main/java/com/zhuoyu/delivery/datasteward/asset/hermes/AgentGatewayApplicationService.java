package com.zhuoyu.delivery.datasteward.asset.hermes;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.CapacityByFileKind;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.AgentAssetContextResolver.AgentAssetContext;
import com.zhuoyu.delivery.datasteward.asset.hermes.AgentPermissionProofService.PermissionProof;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesCapabilitiesResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesChatResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesCitation;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesHealthResponse;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesMissingEvidence;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOperationAction;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOperationPlan;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesOutboundRequest;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesPathHint;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesPermissionResult;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesSafety;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesSupports;
import com.zhuoyu.delivery.datasteward.asset.hermes.HermesGatewayDtos.HermesTrace;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AgentGatewayApplicationService {

    private static final List<String> CONTENT_KEYWORDS = List.of(
        "正文", "内容", "条款", "摘要", "总结", "阅读", "解析", "提取", "翻译", "全文", "文件里", "文档里",
        "模型内容", "图纸内容", "rvt", "dwg", "bim", "构件", "图层", "参数", "标题栏", "图框", "外参",
        "块属性", "level", "grid", "sheet", "view", "family", "type", "lod", "loi", "component"
    );
    private static final List<String> PROJECT_PATH_KEYWORDS = List.of(
        "项目路径", "路径", "目录", "文件夹", "共享盘", "nas", "NAS", "位置", "在哪", "哪里", "path", "folder", "directory"
    );
    private static final String MODULE_CODE = "data-steward";
    private static final Pattern NAS_URI_PATTERN = Pattern.compile("(?i)nas://[^\\s\"'<>]+");
    private static final Pattern VOLUMES_PATH_PATTERN = Pattern.compile("(?i)/Volumes/[^\\s\"'<>]+");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(bearer\\s+)?(token|secret)\\s*[:=]\\s*[^\\s\"'<>]+");
    private static final Pattern INTERNAL_FIELD_PATTERN = Pattern.compile("(?i)\\b(storage_path|storage_uri|storagePath|storageUri|raw row|request_id|trace_id|trace id)\\b");
    private static final Pattern SQL_PATTERN = Pattern.compile("(?i)\\b(select\\s+.+?\\s+from|insert\\s+into|update\\s+.+?\\s+set|delete\\s+from)\\b");

    private final HermesGatewayProperties properties;
    private final AgentAssetContextResolver assetContextResolver;
    private final AgentPermissionProofService permissionProofService;
    private final HermesAgentClient hermesAgentClient;
    private final AuditLogApplicationService auditLogApplicationService;
    private final AssetPathMappingRepository pathMappingRepository;
    private final BimAssetRepository bimAssetRepository;

    public AgentGatewayApplicationService(
        HermesGatewayProperties properties,
        AgentAssetContextResolver assetContextResolver,
        AgentPermissionProofService permissionProofService,
        HermesAgentClient hermesAgentClient,
        AuditLogApplicationService auditLogApplicationService,
        AssetPathMappingRepository pathMappingRepository,
        BimAssetRepository bimAssetRepository
    ) {
        this.properties = properties;
        this.assetContextResolver = assetContextResolver;
        this.permissionProofService = permissionProofService;
        this.hermesAgentClient = hermesAgentClient;
        this.auditLogApplicationService = auditLogApplicationService;
        this.pathMappingRepository = pathMappingRepository;
        this.bimAssetRepository = bimAssetRepository;
    }

    public HermesCapabilitiesResponse capabilities() {
        return new HermesCapabilitiesResponse(
            "Hermes",
            "catalog_only",
            properties.getContractVersion(),
            new HermesSupports(true, true, true, false, false, false, false, false),
            new HermesSafety(true, true, true)
        );
    }

    public HermesHealthResponse health() {
        HermesAgentClient.HermesHealthProbe probe = properties.isEnabled()
            ? hermesAgentClient.health()
            : new HermesAgentClient.HermesHealthProbe(false, "GATEWAY_DISABLED_LOCAL_FALLBACK");
        boolean available = properties.isEnabled() && probe.available();
        return new HermesHealthResponse(
            available ? "ok" : "degraded",
            available,
            "read_only_gateway",
            properties.getContractVersion(),
            properties.isEnabled(),
            properties.isReadonly(),
            false,
            available,
            available ? "" : sanitizeText(probe.unavailableReason()),
            Instant.now()
        );
    }

    public HermesChatResponse chat(AuthenticatedPrincipal principal, HermesChatRequest request) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        long startedAt = System.nanoTime();
        AgentAssetContext context = null;
        PermissionProof proof = null;
        HermesChatResponse response = null;
        ProjectPathContext pathContext = ProjectPathContext.empty();
        try {
            context = assetContextResolver.resolve(request);
            proof = permissionProofService.build(principal.userId(), context);
            if (!proof.allowed()) {
                response = denied(requestId, context, proof);
                return response;
            }
            pathContext = projectPathContext(context.projectId(), request.question());

            HermesChatResponse externalResponse = properties.isEnabled()
                ? hermesAgentClient.chat(outboundRequest(requestId, principal, request, context, proof, pathContext))
                : null;
            if (externalResponse != null) {
                response = asksForContent(request.question())
                    ? localCatalogOnlyFallback(requestId, request, context, proof, pathContext)
                    : sanitizeExternalResponse(requestId, externalResponse, context, proof, pathContext);
                return response;
            }
            if (properties.isEnabled()) {
                response = localCatalogOnlyFallback(requestId, request, context, proof, pathContext);
                return response;
            }
            response = localCatalogOnlyFallback(requestId, request, context, proof, pathContext);
            return response;
        } finally {
            recordHermesAudit(principal, request, context, proof, response, elapsedMillis(startedAt));
        }
    }

    private HermesOutboundRequest outboundRequest(
        String requestId,
        AuthenticatedPrincipal principal,
        HermesChatRequest request,
        AgentAssetContext context,
        PermissionProof proof,
        ProjectPathContext pathContext
    ) {
        Map<String, Object> pageContext = new LinkedHashMap<>();
        pageContext.put("page_type", request.pageType());
        pageContext.put("project_ref", "project:" + context.projectId());
        pageContext.put("asset_ref", context.assetId() == null ? "" : "asset:" + context.assetId());
        pageContext.put("source_view", context.sourceView());
        pageContext.put("current_route", sanitizeText(request.currentRoute()));
        pageContext.put("page_title", sanitizeText(request.pageTitle()));
        pageContext.put("project_code_hint", sanitizeText(request.projectCode()));
        pageContext.put("project_name_hint", sanitizeText(request.projectName()));
        pageContext.put("project_scope_generated_by", "platform_gateway");
        pageContext.put("catalog_summary", catalogSummary(principal.userId(), context.projectId(), request.question()));
        if (pathContext.query() && !pathContext.paths().isEmpty()) {
            pageContext.put("project_path_context", Map.of(
                "path_answer_allowed", true,
                "scope", "current_authorized_project_only",
                "controlled_paths", pathContext.paths().stream()
                    .map(path -> controlledPathPayload(context.projectId(), path))
                    .toList()
            ));
        }

        Map<String, Object> responseRequirements = new LinkedHashMap<>();
        responseRequirements.put("allowed_evidence_modes", List.of("catalog_only", "missing_evidence"));
        responseRequirements.put("allowed_statuses", List.of("catalog_only", "missing_evidence", "denied", "error"));
        responseRequirements.put("operation_plan_policy", "draft_only_requires_human_approval");
        responseRequirements.put("must_show_missing_evidence", true);
        responseRequirements.put("must_not_expose_true_nas_path", true);
        responseRequirements.put("controlled_project_path_answer_allowed", pathContext.query() && !pathContext.paths().isEmpty());
        responseRequirements.put("must_only_use_controlled_project_paths", true);
        responseRequirements.put("must_not_expose_secret", true);
        responseRequirements.put("must_not_use_catalog_as_content_evidence", true);
        responseRequirements.put("must_not_execute_actions", true);

        return new HermesOutboundRequest(
            requestId,
            Instant.now(),
            Map.of(
                "user_ref", "user:" + principal.userId()
            ),
            pageContext,
            Map.of(
                "contract_version", properties.getContractVersion(),
                "permission_status", "allowed",
                "project_scope", Map.of(
                    "scope_type", proof.scopeType(),
                    "authorized_project_refs", proof.authorizedProjectRefs()
                ),
                "allowed_actions", proof.allowedActions(),
                "permission_tags_checked", proof.permissionTagsChecked(),
                "expires_at", proof.expiresAt().toString(),
                "issued_by", proof.issuedBy()
            ),
            Map.of(
                "text", request.question(),
                "mode", "catalog_only",
                "language", "zh-CN"
            ),
            responseRequirements
        );
    }

    private HermesChatResponse denied(String requestId, AgentAssetContext context, PermissionProof proof) {
        return new HermesChatResponse(
            "denied",
            "missing_evidence",
            true,
            requestId,
            requestId,
            context == null ? "" : context.sourceView(),
            fileId(context),
            modelId(context),
            List.of(),
            "当前权限证明未通过，数据管家不能访问该项目或资产。",
            List.of(),
            permission("denied", true, proof.permissionTagsChecked(), true, proof.denialReasonCode()),
            List.of(new HermesMissingEvidence("permission_denied", safeReason(proof.denialReasonText()))),
            operationPlan(false),
            trace(requestId)
        );
    }

    private HermesChatResponse errorFallback(String requestId, AgentAssetContext context, PermissionProof proof) {
        return new HermesChatResponse(
            "error",
            "missing_evidence",
            true,
            requestId,
            requestId,
            context == null ? "" : context.sourceView(),
            fileId(context),
            modelId(context),
            List.of(),
            "Hermes 当前不可用，平台已按安全策略停止本次回答。",
            List.of(),
            permission("allowed", true, proof.permissionTagsChecked(), true, null),
            List.of(new HermesMissingEvidence("agent_unavailable", "无法取得可引用证据，请稍后重试。")),
            operationPlan(false),
            trace(requestId)
        );
    }

    private HermesChatResponse localCatalogOnlyFallback(
        String requestId,
        HermesChatRequest request,
        AgentAssetContext context,
        PermissionProof proof,
        ProjectPathContext pathContext
    ) {
        boolean contentQuestion = asksForContent(request.question());
        boolean guidanceQuestion = asksForPageGuidance(request.question());
        boolean active = "active".equalsIgnoreCase(context.lifecycleStatus());
        boolean catalogOnly = "catalog_only".equalsIgnoreCase(context.indexEligibility());
        boolean unknownConfidentiality = "UNKNOWN".equalsIgnoreCase(context.confidentialityLevel());
        boolean missingPath = pathContext.query() && pathContext.paths().isEmpty();
        String status = contentQuestion || !active || missingPath ? "missing_evidence" : "catalog_only";
        String answer = pathContext.query() && !pathContext.paths().isEmpty()
            ? projectPathAnswer(pathContext)
            : contentQuestion
            ? missingEvidenceAnswer(request.question())
            : guidanceQuestion
            ? pageGuidanceAnswer(request, context)
            : "Hermes 当前回答仅基于资产目录和权限上下文，不包含文件正文证据。";
        return new HermesChatResponse(
            status,
            status,
            true,
            requestId,
            requestId,
            context.sourceView(),
            fileId(context),
            modelId(context),
            pathHints(pathContext),
            answer,
            List.of(new HermesCitation(
                "catalog_metadata",
                context.sourceView(),
                context.assetId() == null ? "" : "asset:" + context.assetId(),
                "project:" + context.projectId(),
                "由平台前端按当前用户权限渲染",
                true
            )),
            permission("allowed", true, proof.permissionTagsChecked(), false, null),
            missingEvidence(request.question(), contentQuestion, active, catalogOnly, unknownConfidentiality, missingPath),
            operationPlan(true),
            trace(requestId)
        );
    }

    private List<HermesMissingEvidence> missingEvidence(
        String question,
        boolean contentQuestion,
        boolean active,
        boolean catalogOnly,
        boolean unknownConfidentiality,
        boolean missingPath
    ) {
        ArrayList<HermesMissingEvidence> items = new ArrayList<>();
        if (contentQuestion) {
            items.add(new HermesMissingEvidence("asset_catalog_only", "当前只有资产目录信息，缺少可引用正文、图纸解析、模型解析或构件证据。"));
            items.addAll(parseEvidenceMissingItems(question));
        }
        if (!active) {
            items.add(new HermesMissingEvidence("lifecycle_not_active", "资产生命周期不是 active，需要人工复核。"));
        }
        if (catalogOnly) {
            items.add(new HermesMissingEvidence("index_eligibility_catalog_only", "当前资产仅允许目录级辅助，不允许正文问答。"));
        }
        if (unknownConfidentiality) {
            items.add(new HermesMissingEvidence("confidentiality_unknown", "密级为 UNKNOWN，不能自动升级为正文可读。"));
        }
        if (missingPath) {
            items.add(new HermesMissingEvidence("project_path_mapping_missing", "当前项目没有可用于自然语言回答的已启用路径映射。"));
        }
        if (items.isEmpty()) {
            items.add(new HermesMissingEvidence("asset_catalog_only", "当前未写入正文证据，因此不能基于文件正文回答。"));
        }
        return items;
    }

    private HermesChatResponse sanitizeExternalResponse(
        String requestId,
        HermesChatResponse response,
        AgentAssetContext context,
        PermissionProof proof,
        ProjectPathContext pathContext
    ) {
        String status = safeStatus(response.status());
        String evidenceMode = safeEvidenceMode(response.evidenceMode());
        String answer = appendProjectPathsIfNeeded(sanitizeText(response.answer(), pathContext.allowedPathValues()), pathContext);
        return new HermesChatResponse(
            status,
            evidenceMode,
            true,
            requestId,
            requestId,
            context.sourceView(),
            fileId(context),
            modelId(context),
            pathHints(pathContext),
            answer,
            sanitizeCitations(response.citations(), pathContext),
            response.permission() == null
                ? permission(proof.allowed() ? "allowed" : "denied", true, proof.permissionTagsChecked(), false, proof.denialReasonCode())
                : permission(
                    sanitizeText(response.permission().permissionStatus()),
                    response.permission().projectScopeChecked(),
                    response.permission().permissionTagsChecked(),
                    response.permission().failClosedApplied(),
                    sanitizeText(response.permission().reasonCode())
                ),
            sanitizeMissingEvidence(response.missingEvidence(), pathContext),
            sanitizeOperationPlan(response.operationPlan()),
            trace(requestId, "openai_compatible_catalog_only")
        );
    }

    private List<HermesCitation> sanitizeCitations(List<HermesCitation> citations, ProjectPathContext pathContext) {
        if (citations == null || citations.isEmpty()) {
            return List.of();
        }
        return citations.stream()
            .map(citation -> new HermesCitation(
                sanitizeText(citation.citationType(), pathContext.allowedPathValues()),
                sanitizeText(citation.sourceView(), pathContext.allowedPathValues()),
                sanitizeText(citation.assetRef(), pathContext.allowedPathValues()),
                sanitizeText(citation.projectRef(), pathContext.allowedPathValues()),
                sanitizeText(citation.displayLabel(), pathContext.allowedPathValues()),
                citation.safeToOpen()
            ))
            .toList();
    }

    private static List<HermesPathHint> pathHints(ProjectPathContext pathContext) {
        if (pathContext == null || !pathContext.query() || pathContext.paths().isEmpty()) {
            return List.of();
        }
        return pathContext.paths().stream()
            .map(path -> new HermesPathHint(
                sanitizeText(path.displayPath()),
                sanitizeText(path.pathHint()),
                sanitizeText(path.providerCode()),
                sanitizeText(path.matchStrategy())
            ))
            .toList();
    }

    private List<HermesMissingEvidence> sanitizeMissingEvidence(List<HermesMissingEvidence> items, ProjectPathContext pathContext) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
            .filter(item -> item != null)
            .filter(item -> !"agent_answer_catalog_only".equalsIgnoreCase(item.reason()))
            .map(item -> new HermesMissingEvidence(
                sanitizeText(item.reason(), pathContext.allowedPathValues()),
                sanitizeText(item.message(), pathContext.allowedPathValues())
            ))
            .toList();
    }

    private HermesOperationPlan sanitizeOperationPlan(HermesOperationPlan plan) {
        if (plan == null || !Boolean.TRUE.equals(plan.available())) {
            return operationPlan(false);
        }
        List<HermesOperationAction> actions = plan.actions() == null ? List.of() : plan.actions().stream()
            .map(action -> new HermesOperationAction(safeDraftAction(action.actionType()), "draft_only"))
            .toList();
        return new HermesOperationPlan(true, true, actions.isEmpty() ? List.of(new HermesOperationAction("manual_review_required", "draft_only")) : actions);
    }

    private void recordHermesAudit(
        AuthenticatedPrincipal principal,
        HermesChatRequest request,
        AgentAssetContext context,
        PermissionProof proof,
        HermesChatResponse response,
        long latencyMillis
    ) {
        Long projectId = context == null ? request.projectId() : context.projectId();
        String requestId = response == null || response.trace() == null ? "" : response.trace().requestId();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("requestId", requestId);
        details.put("userRef", "user:" + principal.userId());
        details.put("projectRef", projectId == null ? "project:unknown" : "project:" + projectId);
        details.put("sourceView", context == null ? normalizeForAudit(request.sourceView()) : normalizeForAudit(context.sourceView()));
        details.put("evidenceMode", response == null ? "unknown" : safeEvidenceMode(response.evidenceMode()));
        details.put("permissionStatus", response == null || response.permission() == null
            ? (proof == null || !proof.allowed() ? "denied" : "allowed")
            : sanitizeText(response.permission().permissionStatus()));
        details.put("status", response == null ? "error" : safeStatus(response.status()));
        details.put("latencyMillis", latencyMillis);
        details.put("pageType", normalizeForAudit(request.pageType()));
        details.put("pageTitle", normalizeForAudit(request.pageTitle()));
        details.put("currentRoute", normalizeForAudit(request.currentRoute()));
        details.put("assetRef", request.assetId() == null ? "" : "asset:" + request.assetId());
        details.put("questionLength", request.question() == null ? 0 : request.question().length());
        details.put("contentQuestion", asksForContent(request.question()));

        auditLogApplicationService.record(
            projectId,
            MODULE_CODE,
            auditActionCode(response),
            "AGENT_CHAT",
            requestId.isBlank() ? "unknown" : requestId,
            principal.userId(),
            details
        );
    }

    private String auditActionCode(HermesChatResponse response) {
        if (response == null) {
            return "agent.hermes.chat.unavailable";
        }
        String status = safeStatus(response.status());
        if ("denied".equals(status)) {
            return "agent.hermes.chat.denied";
        }
        if ("error".equals(status)) {
            return "agent.hermes.chat.unavailable";
        }
        if ("missing_evidence".equals(status)) {
            return "agent.hermes.chat.missing_evidence";
        }
        return "agent.hermes.chat.catalog_only";
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private static String safeStatus(String value) {
        if ("denied".equalsIgnoreCase(value)) {
            return "denied";
        }
        if ("error".equalsIgnoreCase(value)) {
            return "error";
        }
        if ("missing_evidence".equalsIgnoreCase(value)) {
            return "missing_evidence";
        }
        return "catalog_only";
    }

    private static String safeEvidenceMode(String value) {
        if ("missing_evidence".equalsIgnoreCase(value)) {
            return "missing_evidence";
        }
        return "catalog_only";
    }

    private static String safeDraftAction(String actionType) {
        return "manual_review_required";
    }

    private static String missingEvidenceAnswer(String question) {
        if (needsModelOrDrawingParseEvidence(question)) {
            return "Hermes 当前只开放资产目录级辅助，缺少 RVT/DWG/BIM 解析证据、图纸解析证据或构件级证据，不能判断模型内容、图层、参数、标题栏或构件信息。";
        }
        return "Hermes 当前只开放资产目录级辅助，尚无可引用的文件正文证据，不能基于正文回答。";
    }

    private static List<HermesMissingEvidence> parseEvidenceMissingItems(String question) {
        ArrayList<HermesMissingEvidence> items = new ArrayList<>();
        String lower = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (lower.contains("rvt") || lower.contains("revit")) {
            items.add(new HermesMissingEvidence("rvt_parse_evidence_missing", "缺少 RVT/Revit 解析证据，不能判断 Level、Grid、Sheet、View、Family、Type 或模型内部内容。"));
        }
        if (lower.contains("dwg") || lower.contains("dxf") || lower.contains("图层") || lower.contains("标题栏") || lower.contains("图框") || lower.contains("外参") || lower.contains("块属性") || lower.contains("图纸内容")) {
            items.add(new HermesMissingEvidence("dwg_parse_evidence_missing", "缺少 DWG/图纸解析证据，不能判断图层、标题栏、图框、外参、块属性或图纸内部内容。"));
        }
        if (lower.contains("bim") || lower.contains("模型") || lower.contains("构件") || lower.contains("参数") || lower.contains("component") || lower.contains("lod") || lower.contains("loi")) {
            items.add(new HermesMissingEvidence("model_parse_evidence_missing", "缺少模型解析证据，不能判断模型内部内容或参数。"));
            items.add(new HermesMissingEvidence("component_evidence_missing", "缺少构件级证据，不能确认构件存在性、构件参数、LOD 或 LOI。"));
        }
        return items;
    }

    private ProjectPathContext projectPathContext(Long projectId, String question) {
        if (!asksForProjectPath(question) || projectId == null) {
            return ProjectPathContext.empty();
        }
        List<ProjectPathItem> paths = pathMappingRepository.list(projectId, true).stream()
            .filter(mapping -> mapping.nasPath() != null && !mapping.nasPath().isBlank())
            .limit(10)
            .map(mapping -> new ProjectPathItem(
                mapping.providerCode() == null || mapping.providerCode().isBlank() ? "NAS" : mapping.providerCode(),
                displayPath(mapping.projectCode(), mapping.projectName(), mapping.nasPath()),
                pathHint(mapping.providerCode(), mapping.matchStrategy()),
                mapping.matchStrategy() == null || mapping.matchStrategy().isBlank() ? "PREFIX" : mapping.matchStrategy(),
                Boolean.TRUE.equals(mapping.enabled())
            ))
            .toList();
        return new ProjectPathContext(true, paths);
    }

    private static String displayPath(String projectCode, String projectName, String rawPath) {
        String segment = lastSegment(rawPath);
        if (projectName != null && !projectName.isBlank()) {
            return "项目目录：" + projectName.trim();
        }
        if (projectCode != null && !projectCode.isBlank()) {
            return "项目目录：" + projectCode.trim();
        }
        if (!segment.isBlank() && !looksLikeSensitiveSegment(segment)) {
            return "项目目录：" + segment;
        }
        return "项目目录：已登记路径";
    }

    private static String pathHint(String providerCode, String matchStrategy) {
        String provider = providerCode == null || providerCode.isBlank() ? "NAS" : providerCode.trim();
        String strategy = matchStrategy == null || matchStrategy.isBlank() ? "PREFIX" : matchStrategy.trim();
        return provider + "/" + strategy + "/底层路径已隐藏";
    }

    private static String lastSegment(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String normalized = rawPath.trim().replace('\\', '/');
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private static boolean looksLikeSensitiveSegment(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("nas:")
            || lower.startsWith("smb:")
            || lower.contains("storage")
            || lower.contains("token")
            || lower.contains("secret");
    }

    private static Map<String, Object> controlledPathPayload(Long projectId, ProjectPathItem path) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project_ref", projectId == null ? "project:unknown" : "project:" + projectId);
        payload.put("provider", path.providerCode());
        payload.put("display_path", path.displayPath());
        payload.put("path_hint", path.pathHint());
        payload.put("match_strategy", path.matchStrategy());
        payload.put("enabled", path.enabled());
        return payload;
    }

    private static String projectPathAnswer(ProjectPathContext pathContext) {
        if (pathContext.paths().isEmpty()) {
            return "当前项目没有已启用的路径映射。";
        }
        StringBuilder answer = new StringBuilder("当前项目已登记的受控项目路径：");
        for (int i = 0; i < pathContext.paths().size(); i += 1) {
            ProjectPathItem item = pathContext.paths().get(i);
            answer.append("\n").append(i + 1).append(". ")
                .append(item.displayPath())
                .append("；提示：").append(item.pathHint())
                .append("（").append(item.providerCode())
                .append(" / ").append(item.matchStrategy())
                .append("）");
        }
        answer.append("\n这些路径提示来自当前授权项目的路径映射，只用于目录定位，不暴露底层 NAS 原始路径，也不代表已读取文件正文。");
        return answer.toString();
    }

    private static String pageGuidanceAnswer(HermesChatRequest request, AgentAssetContext context) {
        String pageType = request.pageType() == null ? "" : request.pageType().toLowerCase(Locale.ROOT);
        String route = request.currentRoute() == null ? "" : request.currentRoute().toLowerCase(Locale.ROOT);
        String project = request.projectCode() != null && !request.projectCode().isBlank()
            ? request.projectCode().trim()
            : request.projectName() != null && !request.projectName().isBlank()
            ? request.projectName().trim()
            : "当前项目";
        if (pageType.contains("assets_overview") || route.contains("/data-steward/assets") && !route.contains("/work/") && !route.contains("/master-data/")) {
            return "这是 G2 真实项目治理入口。你可以先在资产总览中确认哪些是真实 NAS 项目，再沿着“资产目录、接入评估、工程主数据草案、交付治理助手、缺失项解释、人工确认挂接”的路径推进。当前回答只基于目录和权限上下文。";
        }
        if (pageType.contains("project_detail")) {
            return project + " 的项目工作台用于查看资产目录、文件类型、扫描状态、路径映射提示和治理风险。下一步通常是先确认资产目录，再进入接入评估或交付治理助手。当前不会读取文件正文，也不会暴露底层 NAS 路径。";
        }
        if (pageType.contains("onboarding") || pageType.contains("initialization") || route.contains("/master-data/initialization")) {
            return project + " 当前处于真实项目接入和工程主数据准备阶段。模板生成的是待人工确认的草案，不代表正式交付标准已经锁定；请按接入评估补齐部位树、节点类型和交付定义后再进入治理。";
        }
        if (pageType.contains("governance") || route.contains("/work/agent-governance")) {
            return project + " 的交付治理助手只做体检、缺失项解释、候选文件推荐和人工确认挂接。它不会自动审批、不会自动整改、不会读取 DWG/RVT/PDF/Office 正文；涉及模型内容或图纸内容的问题会返回 Missing Evidence。";
        }
        return "当前页面已接入 Hermes 只读目录级辅助。你可以询问项目路径提示、已登记文件、治理缺口和下一步动作；涉及正文、图纸解析、模型解析或构件级信息时需要 Missing Evidence。";
    }

    private static String appendProjectPathsIfNeeded(String answer, ProjectPathContext pathContext) {
        if (!pathContext.query() || pathContext.paths().isEmpty()) {
            return answer;
        }
        boolean containsDisplayPath = pathContext.displayPathValues().stream().anyMatch(answer::contains);
        if (containsDisplayPath) {
            return answer;
        }
        if (answer == null || answer.isBlank()) {
            return projectPathAnswer(pathContext);
        }
        return answer.stripTrailing() + "\n\n" + projectPathAnswer(pathContext);
    }

    private Map<String, Object> catalogSummary(Long userId, Long projectId, String question) {
        if (projectId == null) {
            return Map.of(
                "available", false,
                "reason", "project_scope_missing"
            );
        }
        long totalCount = bimAssetRepository.countFiles(userId, projectId, null, null, null, null, null, null, null, null);
        List<Map<String, Object>> byKind = bimAssetRepository.capacityByFileKind(userId, projectId).stream()
            .limit(8)
            .map(this::fileKindSummary)
            .toList();
        String keyword = catalogKeyword(question);
        List<FileAssetResponse> samples = bimAssetRepository.listFiles(
            userId, projectId, null, null, null, null, null, keyword, null, null, 0, 8);
        String sampleStrategy = keyword == null || keyword.isBlank() ? "recent_files" : "keyword:" + sanitizeText(keyword);
        if (samples.isEmpty() && keyword != null && !keyword.isBlank()) {
            samples = bimAssetRepository.listFiles(
                userId, projectId, null, null, null, null, null, null, null, null, 0, 8);
            sampleStrategy = "recent_files_keyword_no_match";
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("available", true);
        summary.put("scope", "current_authorized_project_only");
        summary.put("source_view", "FileAssetView");
        summary.put("total_file_count", totalCount);
        summary.put("by_file_kind", byKind);
        summary.put("sample_strategy", sampleStrategy);
        summary.put("sample_files", samples.stream().map(this::fileSample).toList());
        summary.put("true_path_included", false);
        summary.put("content_evidence_included", false);
        summary.put("updated_at_semantics", "platform_catalog_metadata_updated_at_not_nas_mtime");
        return summary;
    }

    private Map<String, Object> fileKindSummary(CapacityByFileKind item) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("file_kind", sanitizeText(item.fileKind()));
        summary.put("file_count", item.fileCount() == null ? 0 : item.fileCount());
        summary.put("total_size_bytes", item.totalSizeBytes() == null ? 0L : item.totalSizeBytes());
        return summary;
    }

    private Map<String, Object> fileSample(FileAssetResponse file) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("file_id", file.fileId());
        sample.put("file_name", sanitizeText(file.fileName()));
        sample.put("file_ext", sanitizeText(file.fileExt()));
        sample.put("file_kind", sanitizeText(file.fileKind()));
        sample.put("discipline", sanitizeText(file.discipline()));
        sample.put("version", sanitizeText(file.versionNo()));
        sample.put("process_status", sanitizeText(file.processStatus()));
        sample.put("review_status", sanitizeText(file.reviewStatus()));
        sample.put("confidence_level", sanitizeText(file.confidenceLevel()));
        sample.put("size_bucket", sizeBucket(file.sizeBytes()));
        sample.put("updated_at", file.updatedAt() == null ? "" : file.updatedAt().toString());
        return sample;
    }

    private static String catalogKeyword(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        for (String ext : List.of("rvt", "ifc", "nwd", "nwc", "dwg", "dxf", "pdf", "excel", "xlsx", "word", "docx")) {
            if (lower.contains(ext)) {
                return ext;
            }
        }
        return null;
    }

    private static String sizeBucket(Long sizeBytes) {
        long value = sizeBytes == null ? 0L : sizeBytes;
        if (value >= 1024L * 1024L * 1024L) {
            return "GB级";
        }
        if (value >= 100L * 1024L * 1024L) {
            return "百MB级";
        }
        if (value >= 1024L * 1024L) {
            return "MB级";
        }
        if (value > 0) {
            return "KB级";
        }
        return "未知";
    }

    private static String sanitizeText(String value) {
        return sanitizeText(value, List.of());
    }

    private static String sanitizeText(String value, List<String> allowedProjectPaths) {
        if (value == null) {
            return "";
        }
        String sanitized = value;
        Map<String, String> placeholders = new LinkedHashMap<>();
        List<String> paths = allowedProjectPaths == null ? List.of() : allowedProjectPaths.stream()
            .filter(path -> path != null && !path.isBlank())
            .sorted((left, right) -> Integer.compare(right.length(), left.length()))
            .toList();
        for (int i = 0; i < paths.size(); i += 1) {
            String placeholder = "__CONTROLLED_PROJECT_PATH_" + i + "__";
            placeholders.put(placeholder, paths.get(i));
            sanitized = sanitized.replace(paths.get(i), placeholder);
        }
        sanitized = NAS_URI_PATTERN.matcher(sanitized).replaceAll("[路径已隐藏]");
        sanitized = VOLUMES_PATH_PATTERN.matcher(sanitized).replaceAll("[路径已隐藏]");
        sanitized = SECRET_PATTERN.matcher(sanitized).replaceAll("[敏感信息已隐藏]");
        sanitized = INTERNAL_FIELD_PATTERN.matcher(sanitized).replaceAll("[内部字段已隐藏]");
        sanitized = SQL_PATTERN.matcher(sanitized).replaceAll("[SQL已隐藏]");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), entry.getValue());
        }
        return sanitized;
    }

    private static String normalizeForAudit(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return sanitizeText(value.trim());
    }

    private HermesPermissionResult permission(
        String status,
        boolean projectScopeChecked,
        boolean permissionTagsChecked,
        boolean failClosedApplied,
        String reasonCode
    ) {
        return new HermesPermissionResult(status, projectScopeChecked, permissionTagsChecked, failClosedApplied, reasonCode);
    }

    private HermesOperationPlan operationPlan(boolean available) {
        return new HermesOperationPlan(
            available,
            true,
            available ? List.of(new HermesOperationAction("manual_review_required", "draft_only")) : List.of()
        );
    }

    private HermesTrace trace(String requestId) {
        return trace(requestId, "catalog_only");
    }

    private HermesTrace trace(String requestId, String agentMode) {
        return new HermesTrace(requestId, agentMode, false);
    }

    private static boolean asksForContent(String question) {
        if (question == null) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        return CONTENT_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private static boolean needsModelOrDrawingParseEvidence(String question) {
        if (question == null) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.contains("rvt")
            || lower.contains("revit")
            || lower.contains("dwg")
            || lower.contains("dxf")
            || lower.contains("bim")
            || lower.contains("模型内容")
            || lower.contains("图纸内容")
            || lower.contains("构件")
            || lower.contains("图层")
            || lower.contains("参数")
            || lower.contains("标题栏")
            || lower.contains("图框")
            || lower.contains("外参")
            || lower.contains("块属性")
            || lower.contains("component")
            || lower.contains("level")
            || lower.contains("grid")
            || lower.contains("sheet")
            || lower.contains("family")
            || lower.contains("type")
            || lower.contains("lod")
            || lower.contains("loi");
    }

    private static boolean asksForProjectPath(String question) {
        if (question == null) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        return PROJECT_PATH_KEYWORDS.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private static boolean asksForPageGuidance(String question) {
        if (question == null) {
            return false;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.contains("这个页面")
            || lower.contains("当前页面")
            || lower.contains("做什么")
            || lower.contains("下一步")
            || lower.contains("怎么做")
            || lower.contains("处于哪一步")
            || lower.contains("治理助手")
            || lower.contains("模板只是草案")
            || lower.contains("主数据缺")
            || lower.contains("资料还缺")
            || lower.contains("缺少证据")
            || lower.contains("缺证据")
            || lower.contains("what should i do")
            || lower.contains("next step");
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "权限校验未通过。";
        }
        return reason;
    }

    private static Long fileId(AgentAssetContext context) {
        if (context == null || context.assetId() == null) {
            return null;
        }
        return "FileAssetView".equals(context.sourceView()) ? context.assetId() : null;
    }

    private static Long modelId(AgentAssetContext context) {
        if (context == null || context.assetId() == null) {
            return null;
        }
        return "ModelAssetView".equals(context.sourceView()) ? context.assetId() : null;
    }

    private record ProjectPathContext(
        boolean query,
        List<ProjectPathItem> paths
    ) {
        static ProjectPathContext empty() {
            return new ProjectPathContext(false, List.of());
        }

        List<String> allowedPathValues() {
            return List.of();
        }

        List<String> displayPathValues() {
            return paths.stream().map(ProjectPathItem::displayPath).toList();
        }
    }

    private record ProjectPathItem(
        String providerCode,
        String displayPath,
        String pathHint,
        String matchStrategy,
        boolean enabled
    ) {
    }
}

package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.datasteward.asset.agent.AgentPrincipal;
import com.zhuoyu.delivery.datasteward.asset.application.AgentApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentAnnotationRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentAnnotationResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ModelAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/agent")
public class AgentController {

    private final AgentApplicationService agentApplicationService;

    public AgentController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    private AgentPrincipal currentAgent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AgentPrincipal agent) {
            return agent;
        }
        throw new BusinessException("AGENT_AUTH_REQUIRED", "需要 Agent API Key 认证", HttpStatus.UNAUTHORIZED);
    }

    // ===== assets =====

    @GetMapping("/assets/projects")
    public ApiResponse<List<AssetProjectResponse>> listProjects() {
        return ApiResponse.success(agentApplicationService.listProjects(currentAgent()));
    }

    @GetMapping("/assets/files")
    public ApiResponse<List<FileAssetResponse>> listFiles(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String fileKind,
        @RequestParam(required = false) String discipline,
        @RequestParam(required = false) String fileName,
        @RequestParam(required = false) String fileExt,
        @RequestParam(required = false) String sourceType,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(agentApplicationService.listFiles(
            currentAgent(), projectId, fileKind, discipline, fileName, fileExt, sourceType, keyword));
    }

    @GetMapping("/assets/files/{fileId}")
    public ApiResponse<FileAssetResponse> getFile(@PathVariable Long fileId) {
        return ApiResponse.success(agentApplicationService.getFile(currentAgent(), fileId));
    }

    @GetMapping("/assets/models")
    public ApiResponse<List<ModelAssetResponse>> listModels(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String discipline
    ) {
        return ApiResponse.success(agentApplicationService.listModels(currentAgent(), projectId, discipline));
    }

    // ===== events =====

    @GetMapping("/events")
    public ApiResponse<List<EventResponse>> listEvents(
        @RequestParam(required = false) Long afterEventId,
        @RequestParam(required = false) Long fromTime,
        @RequestParam(required = false) Long toTime,
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String eventType,
        @RequestParam(required = false) String actionCode,
        @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        java.time.Instant from = fromTime != null ? java.time.Instant.ofEpochMilli(fromTime) : null;
        java.time.Instant to = toTime != null ? java.time.Instant.ofEpochMilli(toTime) : null;
        return ApiResponse.success(agentApplicationService.listEvents(
            currentAgent(), afterEventId, from, to, projectId, eventType, actionCode, limit));
    }

    // ===== jobs =====

    @GetMapping("/jobs")
    public ApiResponse<List<JobResponse>> listJobs(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String jobType,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(agentApplicationService.listJobs(currentAgent(), projectId, jobType, status));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<JobResponse> getJob(@PathVariable Long jobId) {
        return ApiResponse.success(agentApplicationService.getJob(currentAgent(), jobId));
    }

    // ===== task triggers =====

    @PostMapping("/checksum-jobs")
    public ApiResponse<JobResponse> createChecksumJob(@RequestBody Map<String, Long> body) {
        Long fileId = body.get("fileId");
        if (fileId == null) {
            throw new BusinessException("INVALID_PARAM", "fileId 不能为空", HttpStatus.BAD_REQUEST);
        }
        return ApiResponse.success(agentApplicationService.triggerChecksum(currentAgent(), fileId));
    }

    @PostMapping("/nas-scans")
    public ApiResponse<ScanTaskResponse> createNasScan(@RequestBody Map<String, Object> body) {
        String rootCode = (String) body.getOrDefault("rootCode", "AGENT-SCAN");
        String rootPath = (String) body.get("rootPath");
        if (rootPath == null) {
            throw new BusinessException("INVALID_PARAM", "rootPath 不能为空", HttpStatus.BAD_REQUEST);
        }
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        String projectCode = (String) body.getOrDefault("projectCode", null);
        return ApiResponse.success(agentApplicationService.triggerNasScan(
            currentAgent(), rootCode, rootPath, projectId, projectCode));
    }

    // ===== annotations =====

    @PostMapping("/annotations")
    public ApiResponse<AgentAnnotationResponse> submitAnnotation(
        @Valid @RequestBody AgentAnnotationRequest request
    ) {
        return ApiResponse.success(agentApplicationService.submitAnnotation(currentAgent(), request));
    }

    // ===== delete requests (agent-submitted) =====

    @PostMapping("/delete-requests")
    public ApiResponse<DeleteRequestResponse> submitDeleteRequest(@RequestBody Map<String, Object> body) {
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        Long fileId = body.get("fileId") instanceof Number n ? n.longValue() : null;
        String deleteType = (String) body.get("deleteType");
        String reason = (String) body.get("reason");
        if (projectId == null || fileId == null || deleteType == null || reason == null) {
            throw new BusinessException("INVALID_PARAM",
                "projectId, fileId, deleteType, reason 不能为空", HttpStatus.BAD_REQUEST);
        }
        return ApiResponse.success(agentApplicationService.submitDeleteRequest(
            currentAgent(), projectId, fileId, deleteType, reason));
    }
}

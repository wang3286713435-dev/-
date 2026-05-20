package com.zhuoyu.delivery.datasteward.asset.application;

import com.zhuoyu.delivery.datasteward.asset.agent.AgentPrincipal;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentAnnotationRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AgentAnnotationResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DeleteRequestResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.EventResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.JobResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ModelAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AgentAnnotationRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetDeleteRequestRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetEventRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetJobRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetScanTaskRepository;
import com.zhuoyu.delivery.datasteward.asset.repository.BimAssetRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentApplicationService {

    private final BimAssetRepository bimAssetRepository;
    private final AssetEventRepository eventRepository;
    private final AssetJobRepository jobRepository;
    private final AgentAnnotationRepository annotationRepository;
    private final AssetDeleteRequestRepository deleteRequestRepository;
    private final AssetScanTaskRepository scanTaskRepository;
    private final AssetPathMappingRepository pathMappingRepository;
    private final EventApplicationService eventApplicationService;
    private final ChecksumApplicationService checksumApplicationService;

    public AgentApplicationService(
        BimAssetRepository bimAssetRepository,
        AssetEventRepository eventRepository,
        AssetJobRepository jobRepository,
        AgentAnnotationRepository annotationRepository,
        AssetDeleteRequestRepository deleteRequestRepository,
        AssetScanTaskRepository scanTaskRepository,
        AssetPathMappingRepository pathMappingRepository,
        EventApplicationService eventApplicationService,
        ChecksumApplicationService checksumApplicationService
    ) {
        this.bimAssetRepository = bimAssetRepository;
        this.eventRepository = eventRepository;
        this.jobRepository = jobRepository;
        this.annotationRepository = annotationRepository;
        this.deleteRequestRepository = deleteRequestRepository;
        this.scanTaskRepository = scanTaskRepository;
        this.pathMappingRepository = pathMappingRepository;
        this.eventApplicationService = eventApplicationService;
        this.checksumApplicationService = checksumApplicationService;
    }

    private void requireProjectAccess(AgentPrincipal agent, Long projectId) {
        if (!agent.canAccessProject(projectId)) {
            throw new BusinessException("AGENT_PROJECT_FORBIDDEN",
                "Agent 无权访问项目 " + projectId, HttpStatus.FORBIDDEN);
        }
    }

    private List<Long> filterProjectIds(AgentPrincipal agent) {
        List<Long> all = bimAssetRepository.listProjects(agent.createdBy(), null).stream()
            .map(p -> p.projectId()).toList();
        if ("ALL_PROJECTS".equals(agent.scopeType())) {
            return all;
        }
        Set<Long> allowed = Set.copyOf(agent.authorizedProjectIds());
        return all.stream().filter(allowed::contains).toList();
    }

    // ===== project queries =====

    public List<AssetProjectResponse> listProjects(AgentPrincipal agent) {
        List<AssetProjectResponse> all = bimAssetRepository.listProjects(agent.createdBy(), null);
        if ("ALL_PROJECTS".equals(agent.scopeType())) {
            auditQuery(agent, null, "agent.query.projects");
            return all.stream().map(p -> withProjectScope(p, agent)).toList();
        }
        Set<Long> allowed = Set.copyOf(agent.authorizedProjectIds());
        List<AssetProjectResponse> filtered = all.stream()
            .filter(p -> allowed.contains(p.projectId())).toList();
        auditQuery(agent, null, "agent.query.projects");
        return filtered.stream().map(p -> withProjectScope(p, agent)).toList();
    }

    // ===== file queries =====

    public List<FileAssetResponse> listFiles(AgentPrincipal agent, Long projectId, String fileKind,
                                              String discipline, String fileName, String fileExt,
                                              String sourceType, String keyword) {
        if (projectId != null) {
            requireProjectAccess(agent, projectId);
        }
        // Query files visible to the agent's created_by user and filter by project
        List<FileAssetResponse> files = bimAssetRepository.listFiles(
            agent.createdBy(), projectId, fileKind, discipline, fileName, fileExt, sourceType, keyword);
        // Further filter by agent project scope
        if (!"ALL_PROJECTS".equals(agent.scopeType())) {
            Set<Long> allowed = Set.copyOf(agent.authorizedProjectIds());
            files = files.stream().filter(f -> allowed.contains(f.projectId())).toList();
        }
        auditQuery(agent, projectId, "agent.query.files");
        return files.stream().map(f -> withProjectScope(f, agent)).toList();
    }

    public FileAssetResponse getFile(AgentPrincipal agent, Long fileId) {
        List<FileAssetResponse> files = bimAssetRepository.listFileById(agent.createdBy(), fileId);
        if (files.isEmpty()) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在", HttpStatus.NOT_FOUND);
        }
        FileAssetResponse file = files.getFirst();
        requireProjectAccess(agent, file.projectId());
        auditQuery(agent, file.projectId(), "agent.query.file");
        return withProjectScope(file, agent);
    }

    // ===== model queries =====

    public List<ModelAssetResponse> listModels(AgentPrincipal agent, Long projectId, String discipline) {
        if (projectId != null) requireProjectAccess(agent, projectId);
        auditQuery(agent, projectId, "agent.query.models");
        List<ModelAssetResponse> models = bimAssetRepository.listModels(agent.createdBy(), null, projectId, discipline);
        if (!"ALL_PROJECTS".equals(agent.scopeType())) {
            Set<Long> allowed = Set.copyOf(agent.authorizedProjectIds());
            models = models.stream().filter(m -> allowed.contains(m.projectId())).toList();
        }
        return models.stream().map(m -> withProjectScope(m, agent)).toList();
    }

    // ===== event queries =====

    public List<EventResponse> listEvents(AgentPrincipal agent, Long afterEventId, Instant fromTime,
                                           Instant toTime, Long projectId, String eventType,
                                           String actionCode, Integer limit) {
        if (projectId != null) requireProjectAccess(agent, projectId);
        auditQuery(agent, projectId, "agent.query.events");
        List<Long> accessibleProjectIds = filterProjectIds(agent);
        List<EventResponse> events = eventRepository.query(afterEventId, fromTime, toTime,
            projectId, eventType, actionCode, limit != null ? limit : 50);
        return events.stream().filter(e -> {
            if (e.projectId() != null) {
                return accessibleProjectIds.contains(e.projectId());
            }
            return e.operatorId() != null && e.operatorId().equals(agent.createdBy());
        }).map(e -> withProjectScope(e, agent)).toList();
    }

    // ===== job queries =====

    public List<JobResponse> listJobs(AgentPrincipal agent, Long projectId, String jobType, String status) {
        if (projectId != null) requireProjectAccess(agent, projectId);
        auditQuery(agent, projectId, "agent.query.jobs");
        List<Long> accessibleProjectIds = filterProjectIds(agent);
        return jobRepository.listForAgent(accessibleProjectIds, agent.createdBy(),
            projectId, jobType, status);
    }

    public JobResponse getJob(AgentPrincipal agent, Long jobId) {
        JobResponse job = jobRepository.findById(jobId);
        if (job == null) {
            throw new BusinessException("JOB_NOT_FOUND", "任务不存在", HttpStatus.NOT_FOUND);
        }
        if (job.projectId() == null) {
            // Global tasks: only the API key's creator can read
            if (!agent.createdBy().equals(job.createdBy())) {
                throw new BusinessException("AGENT_PROJECT_FORBIDDEN",
                    "Agent 无权访问该全局任务", HttpStatus.FORBIDDEN);
            }
        } else {
            // Project-scoped tasks: must be in agent's actual authorized project set
            List<Long> accessibleIds = filterProjectIds(agent);
            if (!accessibleIds.contains(job.projectId())) {
                throw new BusinessException("AGENT_PROJECT_FORBIDDEN",
                    "Agent 无权访问项目 " + job.projectId() + " 的任务", HttpStatus.FORBIDDEN);
            }
        }
        auditQuery(agent, job.projectId(), "agent.query.job");
        return job;
    }

    // ===== trigger checksum =====

    public JobResponse triggerChecksum(AgentPrincipal agent, Long fileId) {
        List<FileAssetResponse> files = bimAssetRepository.listFileById(agent.createdBy(), fileId);
        if (files.isEmpty()) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在", HttpStatus.NOT_FOUND);
        }
        FileAssetResponse file = files.getFirst();
        requireProjectAccess(agent, file.projectId());

        AssetDtos.ChecksumJobRequest req = new AssetDtos.ChecksumJobRequest(fileId);
        JobResponse job = checksumApplicationService.createSingleFileChecksum(agent.createdBy(), req);
        eventApplicationService.record("AGENT_TASK", file.projectId(), "JOB",
            String.valueOf(job.id()), "agent.checksum.create", agent.createdBy(), "AGENT",
            "Agent 触发 checksum: fileId=" + fileId + " key=" + agent.keyName(), null);
        return job;
    }

    // ===== trigger NAS scan =====

    public ScanTaskResponse triggerNasScan(AgentPrincipal agent, String rootCode, String rootPath,
                                            Long projectId, String projectCode) {
        if (projectId == null) {
            throw new BusinessException("AGENT_SCAN_PROJECT_REQUIRED",
                "Agent 触发 NAS 扫描必须提供 projectId", HttpStatus.BAD_REQUEST);
        }
        // Verify project is in agent's actual authorized set, not just ALL_PROJECTS shortcut
        List<Long> accessibleIds = filterProjectIds(agent);
        if (!accessibleIds.contains(projectId)) {
            throw new BusinessException("AGENT_PROJECT_FORBIDDEN",
                "Agent 无权访问项目 " + projectId, HttpStatus.FORBIDDEN);
        }
        // Validate rootPath is under at least one enabled path mapping for this project
        List<PathMappingResponse> mappings = pathMappingRepository.list(projectId, true);
        Path normalizedRoot = Paths.get(rootPath).normalize().toAbsolutePath();
        boolean pathAllowed = mappings.stream().anyMatch(m -> {
            Path nasPath = Paths.get(m.nasPath()).normalize().toAbsolutePath();
            return normalizedRoot.startsWith(nasPath);
        });
        if (!pathAllowed) {
            throw new BusinessException("AGENT_SCAN_PATH_FORBIDDEN",
                "扫描路径 " + rootPath + " 不在项目 " + projectId + " 的启用路径映射内", HttpStatus.FORBIDDEN);
        }

        Long scanId = scanTaskRepository.insert(rootCode, rootPath, projectId, projectCode,
            true, null, false, null, agent.createdBy());
        eventApplicationService.record("AGENT_TASK", projectId, "SCAN_TASK",
            String.valueOf(scanId), "agent.scan.create", agent.createdBy(), "AGENT",
            "Agent 触发 NAS 扫描: root=" + rootPath + " project=" + projectId + " key=" + agent.keyName(), null);
        return scanTaskRepository.requireById(scanId);
    }

    // ===== annotations =====

    @Transactional
    public AgentAnnotationResponse submitAnnotation(AgentPrincipal agent, AgentAnnotationRequest request) {
        // Verify project access using actual authorized project set
        List<Long> accessibleIds = filterProjectIds(agent);
        if (!accessibleIds.contains(request.projectId())) {
            throw new BusinessException("AGENT_PROJECT_FORBIDDEN",
                "Agent 无权访问项目 " + request.projectId(), HttpStatus.FORBIDDEN);
        }
        // Validate target ownership
        if ("FILE_RESOURCE".equals(request.targetType())) {
            List<FileAssetResponse> files = bimAssetRepository.listFileById(agent.createdBy(), request.targetId());
            if (files.isEmpty() || !files.getFirst().projectId().equals(request.projectId())) {
                throw new BusinessException("ANNOTATION_TARGET_INVALID",
                    "标注目标文件不存在或不属于该项目", HttpStatus.BAD_REQUEST);
            }
        } else {
            throw new BusinessException("ANNOTATION_TARGET_TYPE_UNSUPPORTED",
                "不支持的目标类型: " + request.targetType() + "，当前仅支持 FILE_RESOURCE", HttpStatus.BAD_REQUEST);
        }
        Long annotationId = annotationRepository.insert(
            agent.apiKeyId(), request.projectId(), request.targetType(),
            request.targetId(), request.content());
        eventApplicationService.record("AGENT_ANNOTATION", request.projectId(), "AGENT_ANNOTATION",
            String.valueOf(annotationId), "agent.annotation.create", agent.createdBy(), "AGENT",
            "Agent 提交标注: target=" + request.targetType() + "/" + request.targetId(), null);
        // Note: annotations do NOT directly modify formal asset metadata
        return new AgentAnnotationResponse(annotationId, agent.apiKeyId(), request.projectId(),
            request.targetType(), request.targetId(), request.content(), "SUBMITTED", Instant.now());
    }

    // ===== delete requests =====

    @Transactional
    public DeleteRequestResponse submitDeleteRequest(AgentPrincipal agent, Long projectId,
                                                      Long fileId, String deleteType, String reason) {
        requireProjectAccess(agent, projectId);
        // Verify file belongs to the project
        List<FileAssetResponse> files = bimAssetRepository.listFileById(agent.createdBy(), fileId);
        if (files.isEmpty() || !files.getFirst().projectId().equals(projectId)) {
            throw new BusinessException("FILE_NOT_FOUND", "文件不存在或不属于该项目", HttpStatus.NOT_FOUND);
        }
        String requestNo = deleteRequestRepository.generateRequestNo();
        Long drId = deleteRequestRepository.insert(requestNo, projectId, fileId, deleteType, reason,
            "AGENT", agent.apiKeyId());
        eventApplicationService.record("DELETE_REQUEST", projectId, "DELETE_REQUEST",
            String.valueOf(drId), "agent.delete.request", agent.createdBy(), "AGENT",
            "Agent 提交删除申请: type=" + deleteType + " file=" + fileId, null);
        return deleteRequestRepository.findById(drId).orElseThrow();
    }

    // ===== audit helpers =====

    private void auditQuery(AgentPrincipal agent, Long projectId, String action) {
        eventApplicationService.record("AGENT_QUERY", projectId, "AGENT_QUERY",
            String.valueOf(agent.apiKeyId()), action, agent.createdBy(), "AGENT",
            "Agent 查询: action=" + action, null);
    }

    private AssetProjectResponse withProjectScope(AssetProjectResponse project, AgentPrincipal agent) {
        return new AssetProjectResponse(
            project.projectId(),
            project.code(),
            project.name(),
            project.industryType(),
            project.projectStage(),
            project.projectManagerName(),
            project.assetStatus(),
            project.assetSource(),
            project.modelCount(),
            project.totalSizeBytes(),
            project.lastModelUpdatedAt(),
            project.projectSource(),
            project.projectCategory(),
            project.onboardingStatus(),
            project.fileCount(),
            project.dominantFileKinds(),
            project.lastScanAt(),
            project.hasMasterData(),
            project.hasDeliveryStandard(),
            project.governanceReady(),
            project.permissionTags(),
            agent.scopeType(),
            project.confidentialityLevel(),
            project.lastSeenAt(),
            project.lifecycleStatus(),
            project.indexEligibility()
        );
    }

    private FileAssetResponse withProjectScope(FileAssetResponse file, AgentPrincipal agent) {
        return new FileAssetResponse(
            file.fileId(),
            file.projectId(),
            file.projectCode(),
            file.projectName(),
            file.fileName(),
            file.fileExt(),
            file.fileKind(),
            file.discipline(),
            file.versionNo(),
            file.sizeBytes(),
            file.checksum(),
            file.storageProvider(),
            file.storagePath(),
            file.logicalPath(),
            file.sourceType(),
            file.processStatus(),
            file.reviewStatus(),
            file.confidenceLevel(),
            file.createdAt(),
            file.updatedAt(),
            file.permissionTags(),
            agent.scopeType(),
            file.confidentialityLevel(),
            file.lastSeenAt(),
            file.lifecycleStatus(),
            file.indexEligibility()
        );
    }

    private ModelAssetResponse withProjectScope(ModelAssetResponse model, AgentPrincipal agent) {
        return new ModelAssetResponse(
            model.fileId(),
            model.projectId(),
            model.projectCode(),
            model.projectName(),
            model.originalName(),
            model.sizeBytes(),
            model.versionNo(),
            model.processStatus(),
            model.storageProvider(),
            model.logicalPath(),
            model.discipline(),
            model.sourceType(),
            model.lastVerifiedAt(),
            model.accessUrl(),
            model.permissionTags(),
            agent.scopeType(),
            model.confidentialityLevel(),
            model.lastSeenAt(),
            model.lifecycleStatus(),
            model.indexEligibility()
        );
    }

    private EventResponse withProjectScope(EventResponse event, AgentPrincipal agent) {
        return new EventResponse(
            event.id(),
            event.eventType(),
            event.projectId(),
            event.aggregateType(),
            event.aggregateId(),
            event.actionCode(),
            event.operatorId(),
            event.sourceType(),
            event.summary(),
            event.payloadJson(),
            event.traceId(),
            event.createdAt(),
            event.permissionTags(),
            agent.scopeType(),
            event.confidentialityLevel(),
            event.lastSeenAt(),
            event.lifecycleStatus(),
            event.indexEligibility()
        );
    }
}

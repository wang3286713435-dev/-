package com.zhuoyu.delivery.datasteward.asset.ownership;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipApplyRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipApplyResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipApplyRowResult;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipAssignmentInput;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipBatchReviewRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipCoverageResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipFileRow;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipRecommendationRow;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipStatusSummary;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipTreeNode;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipTreeResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileOwnershipTypeSummary;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.repository.AssetPathMappingRepository;
import com.zhuoyu.delivery.shared.api.PageResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileOwnershipApplicationService {

    private static final String MODULE_CODE = "data-steward";
    private static final int MAX_RECOMMENDATION_LIMIT = 500;
    private static final Pattern UNSAFE_NODE_KEY = Pattern.compile("[^A-Za-z0-9_:-]+");
    private static final Set<String> WRITE_ROLES = Set.of("DELIVERY_ENGINEER", "PROJECT_ADMIN");
    private static final Logger LOGGER = LoggerFactory.getLogger(FileOwnershipApplicationService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AssetPathMappingRepository pathMappingRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public FileOwnershipApplicationService(
        NamedParameterJdbcTemplate jdbcTemplate,
        AssetPathMappingRepository pathMappingRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.pathMappingRepository = pathMappingRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public FileOwnershipCoverageResponse coverage(Long userId, Long projectId) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        int totalFiles = countProjectFiles(projectId);
        int assignedFiles = countAssignedFiles(projectId, null);
        int confirmedFiles = countAssignedFiles(projectId, "CONFIRMED");
        int suggestedFiles = countAssignedFiles(projectId, "SUGGESTED");
        int rejectedFiles = countAssignedFiles(projectId, "REJECTED");
        int unassignedFiles = Math.max(0, totalFiles - assignedFiles);
        BigDecimal rate = totalFiles == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(assignedFiles)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalFiles), 2, RoundingMode.HALF_UP);
        return new FileOwnershipCoverageResponse(
            projectId,
            project.code(),
            project.name(),
            totalFiles,
            assignedFiles,
            confirmedFiles,
            suggestedFiles,
            rejectedFiles,
            unassignedFiles,
            rate,
            ownershipTypeSummary(projectId),
            ownershipStatusSummary(projectId, unassignedFiles)
        );
    }

    public FileOwnershipTreeResponse tree(Long userId, Long projectId) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        FileOwnershipCoverageResponse coverage = coverage(userId, projectId);
        Map<String, TreeAccumulator> nodes = new LinkedHashMap<>();
        String rootPath = project.name();
        TreeAccumulator root = nodes.computeIfAbsent(rootPath, ignored ->
            new TreeAccumulator("ROOT:" + projectId, project.name(), rootPath, "PROJECT", "CONFIRMED", "PLATFORM", null));

        List<AssignmentAggregate> aggregates = jdbcTemplate.query("""
            SELECT node_key, node_label, node_path, ownership_type, status, source, section_node_id,
                   COUNT(1) AS file_count,
                   SUM(CASE WHEN status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmed_count,
                   SUM(CASE WHEN status = 'SUGGESTED' THEN 1 ELSE 0 END) AS suggested_count
            FROM data_file_ownership_assignments
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY node_key, node_label, node_path, ownership_type, status, source, section_node_id
            ORDER BY node_path ASC
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new AssignmentAggregate(
            rs.getString("node_key"),
            rs.getString("node_label"),
            rs.getString("node_path"),
            rs.getString("ownership_type"),
            rs.getString("status"),
            rs.getString("source"),
            rs.getObject("section_node_id", Long.class),
            rs.getInt("file_count"),
            rs.getInt("confirmed_count"),
            rs.getInt("suggested_count")
        ));

        for (AssignmentAggregate aggregate : aggregates) {
            List<String> parts = splitNodePath(aggregate.nodePath());
            if (!parts.isEmpty() && parts.getFirst().equals(project.name())) {
                parts = parts.subList(1, parts.size());
            }
            String currentPath = rootPath;
            TreeAccumulator parent = root;
            for (int i = 0; i < parts.size(); i++) {
                String part = parts.get(i);
                currentPath = currentPath + "/" + part;
                boolean leaf = i == parts.size() - 1;
                String nodePath = currentPath;
                TreeAccumulator node = nodes.computeIfAbsent(nodePath, path -> new TreeAccumulator(
                    leaf ? aggregate.nodeKey() : "GROUP:" + stableKey(path),
                    part,
                    path,
                    leaf ? aggregate.ownershipType() : "GROUP",
                    leaf ? aggregate.status() : "CONFIRMED",
                    leaf ? aggregate.source() : "PLATFORM",
                    leaf ? aggregate.sectionNodeId() : null
                ));
                if (node != root && !parent.children.containsKey(node.nodePath)) {
                    parent.children.put(node.nodePath, node);
                }
                if (i == parts.size() - 1) {
                    node.add(aggregate.fileCount(), aggregate.confirmedCount(), aggregate.suggestedCount());
                }
                parent = node;
            }
        }
        root.fileCount = coverage.assignedFiles();
        root.confirmedFileCount = coverage.confirmedFiles();
        root.suggestedFileCount = coverage.suggestedFiles();
        root.unassignedFileCount = coverage.unassignedFiles();
        return new FileOwnershipTreeResponse(
            projectId,
            project.code(),
            project.name(),
            coverage.totalFiles(),
            coverage.assignedFiles(),
            coverage.unassignedFiles(),
            List.of(root.toResponse())
        );
    }

    public PageResponse<FileOwnershipFileRow> files(
        Long userId,
        Long projectId,
        String nodePath,
        String status,
        String ownershipType,
        boolean reviewOnly,
        int page,
        int pageSize
    ) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 200));
        int offset = (safePage - 1) * safeSize;
        String safeNodePath = nodePath == null || nodePath.isBlank() ? "" : sanitizeNodePath(nodePath, project.name());
        String safeStatus = normalizeReadStatus(status);
        String safeOwnershipType = normalizeReadOwnershipType(ownershipType);

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("limit", safeSize)
            .addValue("offset", offset);

        StringBuilder where = new StringBuilder("""
            FROM data_file_ownership_assignments own
            JOIN data_file_resources f ON f.id = own.file_id AND f.deleted = 0
            WHERE own.project_id = :projectId AND own.deleted = 0
            """);
        if (!safeNodePath.isBlank() && !safeNodePath.equals(project.name())) {
            where.append(" AND (own.node_path = :nodePath OR own.node_path LIKE :nodePathPrefix) ");
            params.addValue("nodePath", safeNodePath);
            params.addValue("nodePathPrefix", safeNodePath + "/%");
        }
        if (safeStatus != null) {
            where.append(" AND own.status = :status ");
            params.addValue("status", safeStatus);
        }
        if (safeOwnershipType != null) {
            where.append(" AND own.ownership_type = :ownershipType ");
            params.addValue("ownershipType", safeOwnershipType);
        }
        if (reviewOnly) {
            where.append("""
                AND (own.status <> 'CONFIRMED'
                     OR own.ownership_type = 'PENDING_REVIEW'
                     OR own.confidence = 'LOW')
                """);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) " + where, params, Long.class);
        List<PathMappingResponse> mappings = pathMappingRepository.list(projectId, true);
        List<FileOwnershipFileRow> rows = jdbcTemplate.query("""
            SELECT f.id AS file_id, f.original_name, f.file_kind, f.discipline AS discipline_code,
                   f.version_no, f.logical_path, LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS file_ext,
                   own.status, own.ownership_type, own.node_key, own.node_label, own.node_path,
                   own.confidence, own.source, own.reason, own.evidence_summary
            """ + where + """
            ORDER BY own.node_path ASC, f.id ASC
            LIMIT :limit OFFSET :offset
            """, params, (rs, rowNum) -> new FileOwnershipFileRow(
            rs.getLong("file_id"),
            rs.getString("original_name"),
            rs.getString("file_kind"),
            rs.getString("file_ext"),
            rs.getString("discipline_code"),
            rs.getString("version_no"),
            safeLogicalPath(rs.getString("logical_path"), rs.getString("original_name"), project.code(), project.name(), mappings),
            rs.getString("status"),
            rs.getString("ownership_type"),
            rs.getString("node_key"),
            sanitizeDisplayText(rs.getString("node_label"), "待判定"),
            sanitizeNodePath(rs.getString("node_path"), project.name()),
            rs.getString("confidence"),
            rs.getString("source"),
            sanitizeDisplayText(rs.getString("reason"), "目录级元数据归属说明"),
            sanitizeDisplayText(rs.getString("evidence_summary"), "目录级元数据，未读取文件正文")
        ));
        return new PageResponse<>(rows, safePage, safeSize, total == null ? 0L : total);
    }

    @Transactional
    public FileOwnershipApplyResponse reviewBatch(Long userId, Long projectId, FileOwnershipBatchReviewRequest request) {
        ProjectInfo project = requireProjectWriteAccess(userId, projectId);
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessException("FILE_OWNERSHIP_CONFIRM_REQUIRED",
                "必须由用户确认后，平台才能写入文件归属复核结果", HttpStatus.BAD_REQUEST);
        }
        List<Long> fileIds = request.fileIds() == null ? List.of() : request.fileIds().stream()
            .filter(Objects::nonNull)
            .distinct()
            .limit(MAX_RECOMMENDATION_LIMIT)
            .toList();
        if (fileIds.isEmpty()) {
            throw new BusinessException("FILE_OWNERSHIP_ITEMS_REQUIRED",
                "请至少选择一个文件", HttpStatus.BAD_REQUEST);
        }
        String action = normalizeReviewAction(request.action());
        String ownershipType = normalizeOwnershipType(request.ownershipType());
        String nodePath = sanitizeNodePath(request.nodePath(), project.name());
        String nodeLabel = sanitizeDisplayText(safeText(request.nodeLabel(), nodePathLeaf(nodePath)), "待判定");
        String nodeKey = stableKey(safeText(request.nodeKey(), nodePath));
        String reason = sanitizeDisplayText(request.reason(), reviewReason(action, ownershipType, nodePath));
        String evidenceSummary = "人工复核操作；仅更新目录级归属元数据，未读取文件正文，未移动或修改 NAS 文件。";

        if (requiresOwnershipType(action) && (request.ownershipType() == null || request.ownershipType().isBlank())) {
            throw new BusinessException("FILE_OWNERSHIP_TYPE_REQUIRED",
                "批量修改归属类型时必须选择目标类型", HttpStatus.BAD_REQUEST);
        }
        if (requiresNode(action) && (request.nodePath() == null || request.nodePath().isBlank())) {
            throw new BusinessException("FILE_OWNERSHIP_NODE_REQUIRED",
                "批量移动工程节点时必须选择目标节点", HttpStatus.BAD_REQUEST);
        }

        Map<Long, CurrentAssignment> assignments = currentAssignments(projectId, fileIds);
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<FileOwnershipApplyRowResult> results = new ArrayList<>();
        for (Long fileId : fileIds) {
            CurrentAssignment current = assignments.get(fileId);
            if (current == null) {
                skipped += 1;
                results.add(new FileOwnershipApplyRowResult(fileId, "", "SKIPPED",
                    "该文件不属于当前项目或尚未进入归属体系", null, null));
                continue;
            }
            try {
                int affected = updateAssignmentForReview(projectId, fileId, userId, action, ownershipType,
                    nodeKey, nodeLabel, nodePath, reason, evidenceSummary);
                if (affected > 0) {
                    updated += 1;
                    results.add(new FileOwnershipApplyRowResult(fileId, current.fileName(), "UPDATED",
                        reviewSuccessMessage(action, ownershipType, nodeLabel), nodeKeyForResult(action, current, nodeKey),
                        nodeLabelForResult(action, current, nodeLabel)));
                } else {
                    skipped += 1;
                    results.add(new FileOwnershipApplyRowResult(fileId, current.fileName(), "SKIPPED",
                        "归属记录已变化，请刷新后重试", current.nodeKey(), current.nodeLabel()));
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("File ownership review failed, projectId={}, fileId={}, action={}", projectId, fileId, action, ex);
                failed += 1;
                results.add(new FileOwnershipApplyRowResult(fileId, current.fileName(), "FAILED",
                    "归属复核写入失败，请刷新后重试", current.nodeKey(), current.nodeLabel()));
            }
        }
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file-ownership.review-batch", "PROJECT",
            String.valueOf(projectId), userId, Map.of(
                "action", action,
                "requested", fileIds.size(),
                "updated", updated,
                "skipped", skipped,
                "failed", failed
            ));
        return new FileOwnershipApplyResponse(projectId, fileIds.size(), 0, updated, skipped, failed, results);
    }

    public PageResponse<FileOwnershipRecommendationRow> unassigned(Long userId, Long projectId, int page, int pageSize) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(pageSize, 200));
        int offset = (safePage - 1) * safeSize;
        Long total = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources f
            LEFT JOIN data_file_ownership_assignments own ON own.file_id = f.id AND own.deleted = 0
            WHERE f.project_id = :projectId AND f.deleted = 0 AND own.id IS NULL
            """, new MapSqlParameterSource("projectId", projectId), Long.class);
        List<FileForOwnership> files = queryFiles(projectId, false, null, safeSize, offset);
        List<FileOwnershipRecommendationRow> rows = files.stream()
            .map(file -> recommend(project, file, "RULE"))
            .toList();
        return new PageResponse<>(rows, safePage, safeSize, total == null ? 0L : total);
    }

    public FileOwnershipRecommendationResponse recommendations(
        Long userId,
        Long projectId,
        FileOwnershipRecommendationRequest request
    ) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        int limit = request == null || request.limit() == null
            ? 80
            : Math.max(1, Math.min(request.limit(), MAX_RECOMMENDATION_LIMIT));
        boolean includeAssigned = request != null && Boolean.TRUE.equals(request.includeAssigned());
        String source = normalizeSource(request == null ? null : request.source());
        List<FileForOwnership> files = queryFiles(projectId, includeAssigned, request == null ? null : request.fileIds(), limit, 0);
        List<FileOwnershipRecommendationRow> rows = files.stream()
            .map(file -> recommend(project, file, source))
            .toList();
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file-ownership.recommend", "PROJECT",
            String.valueOf(projectId), userId, Map.of("count", rows.size(), "source", source));
        return new FileOwnershipRecommendationResponse(projectId, rows.size(), rows);
    }

    @Transactional
    public FileOwnershipApplyResponse apply(Long userId, Long projectId, FileOwnershipApplyRequest request) {
        ProjectInfo project = requireProjectWriteAccess(userId, projectId);
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessException("FILE_OWNERSHIP_CONFIRM_REQUIRED",
                "必须由用户确认后，平台才能写入文件归属", HttpStatus.BAD_REQUEST);
        }

        List<FileOwnershipRecommendationRow> rows = resolveApplyRows(project, request);
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        List<FileOwnershipApplyRowResult> results = new ArrayList<>();
        for (FileOwnershipRecommendationRow row : rows) {
            try {
                boolean existed = assignmentExists(row.fileId());
                upsertAssignment(projectId, row, userId);
                if (existed) {
                    updated += 1;
                } else {
                    created += 1;
                }
                results.add(new FileOwnershipApplyRowResult(
                    row.fileId(), row.fileName(), existed ? "UPDATED" : "CREATED",
                    "已确认归属到“" + row.suggestedNodeLabel() + "”", row.suggestedNodeKey(), row.suggestedNodeLabel()
                ));
            } catch (RuntimeException ex) {
                failed += 1;
                results.add(new FileOwnershipApplyRowResult(
                    row.fileId(), row.fileName(), "FAILED",
                    "归属写入失败，请刷新后重试", row.suggestedNodeKey(), row.suggestedNodeLabel()
                ));
            }
        }
        auditLogApplicationService.record(projectId, MODULE_CODE, "data.file-ownership.apply", "PROJECT",
            String.valueOf(projectId), userId, Map.of("requested", rows.size(),
                "created", created, "updated", updated, "skipped", skipped, "failed", failed));
        return new FileOwnershipApplyResponse(projectId, rows.size(), created, updated, skipped, failed, results);
    }

    private List<FileOwnershipRecommendationRow> resolveApplyRows(ProjectInfo project, FileOwnershipApplyRequest request) {
        if (Boolean.TRUE.equals(request.applyAllUnassigned())) {
            return queryFiles(project.id(), false, null, Integer.MAX_VALUE, 0).stream()
                .map(file -> recommend(project, file, normalizeSource(request.source())))
                .toList();
        }
        if (request.recommendations() != null && !request.recommendations().isEmpty()) {
            return resolveRecommendationRowsFromClient(project, request.recommendations());
        }
        if (request.items() != null && !request.items().isEmpty()) {
            Map<Long, FileForOwnership> files = queryFiles(project.id(), true,
                request.items().stream().map(FileOwnershipAssignmentInput::fileId).filter(Objects::nonNull).toList(),
                request.items().size(), 0).stream().collect(HashMap::new, (map, file) -> map.put(file.fileId(), file), HashMap::putAll);
            List<FileOwnershipRecommendationRow> rows = request.items().stream()
                .filter(item -> files.containsKey(item.fileId()))
                .map(item -> toRecommendation(project, files.get(item.fileId()), item))
                .toList();
            if (rows.isEmpty()) {
                throw new BusinessException("FILE_OWNERSHIP_FILE_NOT_FOUND",
                    "所选文件不属于当前项目或已不可用", HttpStatus.BAD_REQUEST);
            }
            return rows;
        }
        throw new BusinessException("FILE_OWNERSHIP_ITEMS_REQUIRED",
            "请至少选择一个文件或使用 applyAllUnassigned", HttpStatus.BAD_REQUEST);
    }

    private List<FileOwnershipRecommendationRow> resolveRecommendationRowsFromClient(
        ProjectInfo project,
        List<FileOwnershipRecommendationRow> recommendations
    ) {
        List<Long> fileIds = recommendations.stream()
            .map(FileOwnershipRecommendationRow::fileId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (fileIds.isEmpty()) {
            throw new BusinessException("FILE_OWNERSHIP_ITEMS_REQUIRED",
                "请至少选择一个文件或使用 applyAllUnassigned", HttpStatus.BAD_REQUEST);
        }
        Map<Long, FileForOwnership> files = queryFiles(project.id(), true, fileIds, fileIds.size(), 0).stream()
            .collect(HashMap::new, (map, file) -> map.put(file.fileId(), file), HashMap::putAll);
        List<FileOwnershipRecommendationRow> rows = recommendations.stream()
            .filter(row -> row.fileId() != null && files.containsKey(row.fileId()))
            .map(row -> toRecommendation(project, files.get(row.fileId()), row))
            .toList();
        if (rows.isEmpty()) {
            throw new BusinessException("FILE_OWNERSHIP_FILE_NOT_FOUND",
                "所选文件不属于当前项目或已不可用", HttpStatus.BAD_REQUEST);
        }
        return rows;
    }

    private FileOwnershipRecommendationRow toRecommendation(ProjectInfo project, FileForOwnership file, FileOwnershipAssignmentInput item) {
        String nodeKey = safeText(item.nodeKey(), "PENDING_REVIEW");
        String nodeLabel = safeText(item.nodeLabel(), "待判定");
        String nodePath = safeText(item.nodePath(), project.name() + "/待判定");
        return new FileOwnershipRecommendationRow(
            "OWN-" + project.id() + "-" + file.fileId(),
            file.fileId(),
            file.fileName(),
            file.fileKind(),
            file.fileExt(),
            file.disciplineCode(),
            file.version(),
            file.displayPath(),
            stableKey(nodeKey),
            sanitizeDisplayText(nodeLabel, "待判定"),
            sanitizeNodePath(nodePath, project.name()),
            normalizeOwnershipType(item.ownershipType()),
            normalizeConfidence(item.confidence()),
            normalizeSource(item.source()),
            sanitizeDisplayText(item.reason(), "人工指定归属节点"),
            sanitizeDisplayText(item.evidenceSummary(), "人工确认"),
            false,
            List.of()
        );
    }

    private FileOwnershipRecommendationRow toRecommendation(
        ProjectInfo project,
        FileForOwnership file,
        FileOwnershipRecommendationRow row
    ) {
        String fallbackPath = project.name() + "/待判定资料/通用/项目通用";
        return new FileOwnershipRecommendationRow(
            "OWN-" + project.id() + "-" + file.fileId(),
            file.fileId(),
            file.fileName(),
            file.fileKind(),
            file.fileExt(),
            file.disciplineCode(),
            file.version(),
            file.displayPath(),
            stableKey(sanitizeDisplayText(row.suggestedNodeKey(), "PENDING_REVIEW")),
            sanitizeDisplayText(row.suggestedNodeLabel(), "待判定"),
            sanitizeNodePath(safeText(row.suggestedNodePath(), fallbackPath), project.name()),
            normalizeOwnershipType(row.ownershipType()),
            normalizeConfidence(row.confidence()),
            normalizeSource(row.source()),
            sanitizeDisplayText(row.reason(), "用户确认平台推荐归属"),
            sanitizeDisplayText(row.evidenceSummary(), "目录级元数据推荐，未读取文件正文"),
            Boolean.TRUE.equals(row.metadataGovernanceRequired()),
            sanitizeRisks(row.risks())
        );
    }

    private FileOwnershipRecommendationRow recommend(ProjectInfo project, FileForOwnership file, String source) {
        String path = safeText(file.displayPath(), file.fileName());
        String haystack = (path + " " + file.fileName() + " " + file.fileKind() + " " + file.fileExt()).toLowerCase(Locale.ROOT);
        String ownershipType = inferOwnershipType(file, haystack);
        String primaryLabel = ownershipTypeLabel(ownershipType);
        String disciplineLabel = inferDisciplineLabel(file, haystack);
        String areaLabel = inferAreaLabel(haystack);
        String nodePath = project.name() + "/" + primaryLabel + "/" + disciplineLabel + "/" + areaLabel;
        String nodeLabel = areaLabel;
        String nodeKey = stableKey(ownershipType + ":" + disciplineLabel + ":" + areaLabel);
        String confidence = inferConfidence(file, haystack, ownershipType);
        List<String> risks = risks(file, ownershipType, confidence);
        String reason = "依据文件类型“%s”、专业线索“%s”和目录线索“%s”，建议归属到“%s”。"
            .formatted(safeText(file.fileKind(), file.fileExt()), disciplineLabel, pathHint(path), nodePath);
        String evidenceSummary = "文件名、扩展名、文件类型、专业字段和项目内相对路径；未读取文件正文。";
        return new FileOwnershipRecommendationRow(
            "OWN-" + project.id() + "-" + file.fileId(),
            file.fileId(),
            file.fileName(),
            file.fileKind(),
            file.fileExt(),
            file.disciplineCode(),
            file.version(),
            path,
            nodeKey,
            nodeLabel,
            nodePath,
            ownershipType,
            confidence,
            source,
            reason,
            evidenceSummary,
            !risks.isEmpty(),
            risks
        );
    }

    private void upsertAssignment(Long projectId, FileOwnershipRecommendationRow row, Long userId) {
        jdbcTemplate.update("""
            INSERT INTO data_file_ownership_assignments (
                project_id, file_id, section_node_id, node_key, node_label, node_path,
                ownership_type, status, confidence, source, reason, evidence_summary,
                confirmed_by, confirmed_at, created_by, updated_by
            ) VALUES (
                :projectId, :fileId, NULL, :nodeKey, :nodeLabel, :nodePath,
                :ownershipType, 'CONFIRMED', :confidence, :source, :reason, :evidenceSummary,
                :userId, CURRENT_TIMESTAMP, :userId, :userId
            )
            ON DUPLICATE KEY UPDATE
                node_key = VALUES(node_key),
                node_label = VALUES(node_label),
                node_path = VALUES(node_path),
                ownership_type = VALUES(ownership_type),
                status = 'CONFIRMED',
                confidence = VALUES(confidence),
                source = VALUES(source),
                reason = VALUES(reason),
                evidence_summary = VALUES(evidence_summary),
                confirmed_by = :userId,
                confirmed_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                deleted = 0,
                delete_token = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", row.fileId())
            .addValue("nodeKey", row.suggestedNodeKey())
            .addValue("nodeLabel", row.suggestedNodeLabel())
            .addValue("nodePath", row.suggestedNodePath())
            .addValue("ownershipType", row.ownershipType())
            .addValue("confidence", row.confidence())
            .addValue("source", row.source())
            .addValue("reason", row.reason())
            .addValue("evidenceSummary", row.evidenceSummary())
            .addValue("userId", userId));
    }

    private Map<Long, CurrentAssignment> currentAssignments(Long projectId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CurrentAssignment> rows = new HashMap<>();
        jdbcTemplate.query("""
            SELECT own.file_id, f.original_name, own.node_key, own.node_label, own.node_path,
                   own.ownership_type, own.status
            FROM data_file_ownership_assignments own
            JOIN data_file_resources f ON f.id = own.file_id AND f.deleted = 0
            WHERE own.project_id = :projectId
              AND own.file_id IN (:fileIds)
              AND own.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileIds", fileIds), rs -> {
                rows.put(rs.getLong("file_id"), new CurrentAssignment(
                    rs.getLong("file_id"),
                    sanitizeDisplayText(rs.getString("original_name"), "未命名文件"),
                    rs.getString("node_key"),
                    sanitizeDisplayText(rs.getString("node_label"), "待判定"),
                    rs.getString("node_path"),
                    rs.getString("ownership_type"),
                    rs.getString("status")
                ));
            });
        return rows;
    }

    private int updateAssignmentForReview(
        Long projectId,
        Long fileId,
        Long userId,
        String action,
        String ownershipType,
        String nodeKey,
        String nodeLabel,
        String nodePath,
        String reason,
        String evidenceSummary
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("fileId", fileId)
            .addValue("userId", userId)
            .addValue("ownershipType", ownershipType)
            .addValue("nodeKey", nodeKey)
            .addValue("nodeLabel", nodeLabel)
            .addValue("nodePath", nodePath)
            .addValue("reason", reason)
            .addValue("evidenceSummary", evidenceSummary);
        String setClause = switch (action) {
            case "CONFIRM" -> """
                status = 'CONFIRMED',
                source = 'MANUAL',
                reason = :reason,
                evidence_summary = :evidenceSummary,
                confirmed_by = :userId,
                confirmed_at = CURRENT_TIMESTAMP,
                rejected_by = NULL,
                rejected_at = NULL,
                updated_by = :userId,
                updated_at = CURRENT_TIMESTAMP
                """;
            case "REJECT" -> """
                status = 'REJECTED',
                source = 'MANUAL',
                reason = :reason,
                evidence_summary = :evidenceSummary,
                confirmed_by = NULL,
                confirmed_at = NULL,
                rejected_by = :userId,
                rejected_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                updated_at = CURRENT_TIMESTAMP
                """;
            case "UPDATE_TYPE" -> """
                ownership_type = :ownershipType,
                status = 'CONFIRMED',
                source = 'MANUAL',
                reason = :reason,
                evidence_summary = :evidenceSummary,
                confirmed_by = :userId,
                confirmed_at = CURRENT_TIMESTAMP,
                rejected_by = NULL,
                rejected_at = NULL,
                updated_by = :userId,
                updated_at = CURRENT_TIMESTAMP
                """;
            case "MOVE_NODE" -> """
                node_key = :nodeKey,
                node_label = :nodeLabel,
                node_path = :nodePath,
                status = 'CONFIRMED',
                source = 'MANUAL',
                reason = :reason,
                evidence_summary = :evidenceSummary,
                confirmed_by = :userId,
                confirmed_at = CURRENT_TIMESTAMP,
                rejected_by = NULL,
                rejected_at = NULL,
                updated_by = :userId,
                updated_at = CURRENT_TIMESTAMP
                """;
            case "UPDATE_NODE_AND_TYPE" -> """
                node_key = :nodeKey,
                node_label = :nodeLabel,
                node_path = :nodePath,
                ownership_type = :ownershipType,
                status = 'CONFIRMED',
                source = 'MANUAL',
                reason = :reason,
                evidence_summary = :evidenceSummary,
                confirmed_by = :userId,
                confirmed_at = CURRENT_TIMESTAMP,
                rejected_by = NULL,
                rejected_at = NULL,
                updated_by = :userId,
                updated_at = CURRENT_TIMESTAMP
                """;
            default -> throw new BusinessException("FILE_OWNERSHIP_ACTION_UNSUPPORTED",
                "不支持的归属复核动作", HttpStatus.BAD_REQUEST);
        };
        return jdbcTemplate.update("""
            UPDATE data_file_ownership_assignments
            SET
            """ + setClause + """
            WHERE project_id = :projectId
              AND file_id = :fileId
              AND deleted = 0
            """, params);
    }

    private boolean assignmentExists(Long fileId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_ownership_assignments
            WHERE file_id = :fileId AND deleted = 0
            """, new MapSqlParameterSource("fileId", fileId), Integer.class);
        return count != null && count > 0;
    }

    private List<FileForOwnership> queryFiles(Long projectId, boolean includeAssigned, List<Long> fileIds, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("limit", limit <= 0 || limit == Integer.MAX_VALUE ? 100_000 : Math.min(limit, 100_000))
            .addValue("offset", Math.max(0, offset));
        StringBuilder sql = new StringBuilder("""
            SELECT f.id AS file_id, f.project_id, p.code AS project_code, p.name AS project_name,
                   f.original_name, f.file_kind, f.discipline AS discipline_code, f.version_no,
                   f.size_bytes, f.logical_path, f.process_status,
                   LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS file_ext
            FROM data_file_resources f
            JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
            LEFT JOIN data_file_ownership_assignments own ON own.file_id = f.id AND own.deleted = 0
            WHERE f.project_id = :projectId AND f.deleted = 0
            """);
        if (!includeAssigned) {
            sql.append(" AND own.id IS NULL");
        }
        if (fileIds != null && !fileIds.isEmpty()) {
            sql.append(" AND f.id IN (:fileIds)");
            params.addValue("fileIds", fileIds);
        }
        sql.append(" ORDER BY f.id ASC LIMIT :limit OFFSET :offset");
        List<PathMappingResponse> mappings = pathMappingRepository.list(projectId, true);
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> mapFileForOwnership(rs, mappings));
    }

    private FileForOwnership mapFileForOwnership(ResultSet rs, List<PathMappingResponse> mappings) throws SQLException {
        Long projectId = rs.getLong("project_id");
        String projectCode = rs.getString("project_code");
        String projectName = rs.getString("project_name");
        String fileName = rs.getString("original_name");
        String displayPath = safeLogicalPath(rs.getString("logical_path"), fileName, projectCode, projectName, mappings);
        return new FileForOwnership(
            rs.getLong("file_id"),
            projectId,
            projectCode,
            projectName,
            fileName,
            rs.getString("file_kind"),
            rs.getString("file_ext"),
            rs.getString("discipline_code"),
            rs.getString("version_no"),
            rs.getLong("size_bytes"),
            displayPath,
            rs.getString("process_status")
        );
    }

    private ProjectInfo requireProjectAccess(Long userId, Long projectId) {
        List<ProjectInfo> rows = jdbcTemplate.query("""
            SELECT p.id, p.code, p.name, r.code AS role_code
            FROM core_user_project_roles upr
            JOIN core_projects p ON p.id = upr.project_id AND p.deleted = 0
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.project_id = :projectId
              AND upr.deleted = 0
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId), (rs, rowNum) -> new ProjectInfo(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("role_code")
        ));
        if (rows.isEmpty()) {
            throw new BusinessException("FILE_OWNERSHIP_PROJECT_ACCESS_DENIED", "无权访问该项目文件归属", HttpStatus.FORBIDDEN);
        }
        return rows.getFirst();
    }

    private ProjectInfo requireProjectWriteAccess(Long userId, Long projectId) {
        ProjectInfo project = requireProjectAccess(userId, projectId);
        if (!WRITE_ROLES.contains(project.roleCode())) {
            throw new BusinessException("FILE_OWNERSHIP_WRITE_DENIED",
                "当前项目角色只能查看文件归属，不能写入归属结果", HttpStatus.FORBIDDEN);
        }
        return project;
    }

    private int countProjectFiles(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM data_file_resources
            WHERE project_id = :projectId AND deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), Integer.class);
        return count == null ? 0 : count;
    }

    private int countAssignedFiles(Long projectId, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource("projectId", projectId);
        String sql = """
            SELECT COUNT(1)
            FROM data_file_ownership_assignments
            WHERE project_id = :projectId AND deleted = 0
            """;
        if (status != null) {
            sql += " AND status = :status";
            params.addValue("status", status);
        }
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count == null ? 0 : count;
    }

    private List<FileOwnershipTypeSummary> ownershipTypeSummary(Long projectId) {
        return jdbcTemplate.query("""
            SELECT ownership_type, COUNT(1) AS file_count
            FROM data_file_ownership_assignments
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY ownership_type
            ORDER BY file_count DESC, ownership_type ASC
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new FileOwnershipTypeSummary(
            rs.getString("ownership_type"),
            ownershipTypeLabel(rs.getString("ownership_type")),
            rs.getInt("file_count")
        ));
    }

    private List<FileOwnershipStatusSummary> ownershipStatusSummary(Long projectId, int unassignedFiles) {
        List<FileOwnershipStatusSummary> rows = new ArrayList<>(jdbcTemplate.query("""
            SELECT status, COUNT(1) AS file_count
            FROM data_file_ownership_assignments
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY status
            ORDER BY status ASC
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new FileOwnershipStatusSummary(
            rs.getString("status"),
            statusLabel(rs.getString("status")),
            rs.getInt("file_count")
        )));
        rows.add(new FileOwnershipStatusSummary("UNASSIGNED", "未归属", unassignedFiles));
        return rows;
    }

    private String inferOwnershipType(FileForOwnership file, String haystack) {
        String ext = safeText(file.fileExt(), "").toLowerCase(Locale.ROOT);
        String kind = safeText(file.fileKind(), "").toUpperCase(Locale.ROOT);
        if ("MODEL".equals(kind) || List.of("rvt", "ifc", "nwd", "nwc", "glb", "gltf", "obj", "fbx").contains(ext)) {
            return "MODEL";
        }
        if (containsAny(haystack, "回收", "作废", "旧版", "历史", "备份", "归档")) {
            return "ARCHIVE";
        }
        if (containsAny(haystack, "收发", "图纸", "收图", "发图", "来图", "出图") || "DRAWING".equals(kind) || List.of("dwg", "dxf").contains(ext)) {
            return "DRAWING_EXCHANGE";
        }
        if (containsAny(haystack, "交付", "报审", "成果", "竣工", "正式")) {
            return "DELIVERY";
        }
        if (containsAny(haystack, "过程", "会议", "函", "联系单", "变更", "深化", "提资", "确认")) {
            return "PROCESS";
        }
        if (containsAny(haystack, "参考", "规范", "标准", "说明", "资料", "方案")) {
            return "REFERENCE";
        }
        return "PENDING_REVIEW";
    }

    private String inferDisciplineLabel(FileForOwnership file, String haystack) {
        String code = safeText(file.disciplineCode(), "").toUpperCase(Locale.ROOT);
        Map<String, String> codeMap = Map.ofEntries(
            Map.entry("ARCHITECTURE", "建筑专业"),
            Map.entry("STRUCTURE", "结构专业"),
            Map.entry("ELECTRICAL", "电气专业"),
            Map.entry("PLUMBING", "给排水专业"),
            Map.entry("HVAC", "暖通专业"),
            Map.entry("FIRE_PROTECTION", "消防专业"),
            Map.entry("INTELLIGENT", "智能化专业"),
            Map.entry("GAS", "燃气专业")
        );
        if (codeMap.containsKey(code)) {
            return codeMap.get(code);
        }
        if (containsAny(haystack, "建筑", "建施")) return "建筑专业";
        if (containsAny(haystack, "结构", "结施")) return "结构专业";
        if (containsAny(haystack, "电气", "电施", "强电", "弱电", "配电")) return "电气专业";
        if (containsAny(haystack, "给排水", "给水", "排水", "水施")) return "给排水专业";
        if (containsAny(haystack, "暖通", "空调", "通风", "防排烟")) return "暖通专业";
        if (containsAny(haystack, "消防", "喷淋", "消火栓")) return "消防专业";
        if (containsAny(haystack, "智能化", "弱电", "安防", "网络")) return "智能化专业";
        if (containsAny(haystack, "燃气")) return "燃气专业";
        if (containsAny(haystack, "机电")) return "机电综合";
        if (containsAny(haystack, "内装", "室内", "装修")) return "内装专业";
        return "通用/待判定专业";
    }

    private String inferAreaLabel(String haystack) {
        if (containsAny(haystack, "地下室", "b1", "b2", "b3", "地下")) return "地下室";
        if (containsAny(haystack, "首层", "一层", "1层", "1f")) return "首层";
        if (containsAny(haystack, "标准层", "塔楼")) return "标准层";
        if (containsAny(haystack, "屋面", "天面")) return "屋面";
        if (containsAny(haystack, "公区", "公共区")) return "公区";
        if (containsAny(haystack, "户型", "户内")) return "户型";
        if (containsAny(haystack, "机房", "设备房")) return "机房/设备房";
        return "项目通用";
    }

    private String inferConfidence(FileForOwnership file, String haystack, String ownershipType) {
        int score = 0;
        if (file.fileKind() != null && !file.fileKind().isBlank()) score += 2;
        if (file.fileExt() != null && !file.fileExt().isBlank()) score += 1;
        if (file.disciplineCode() != null && !file.disciplineCode().isBlank() && !"OTHER".equalsIgnoreCase(file.disciplineCode())) score += 2;
        if (file.displayPath() != null && file.displayPath().contains("/")) score += 2;
        if (!"PENDING_REVIEW".equals(ownershipType)) score += 2;
        if (containsAny(haystack, "待判", "未知", "临时")) score -= 2;
        if (score >= 7) return "HIGH";
        if (score >= 4) return "MEDIUM";
        return "LOW";
    }

    private List<String> risks(FileForOwnership file, String ownershipType, String confidence) {
        List<String> risks = new ArrayList<>();
        if ("PENDING_REVIEW".equals(ownershipType)) {
            risks.add("目录和文件名线索不足，建议人工确认后再作为正式依据。");
        }
        if ("LOW".equals(confidence)) {
            risks.add("推荐置信度较低，需要人工复核。");
        }
        if (file.version() == null || file.version().isBlank()) {
            risks.add("文件缺少版本字段。");
        }
        if (!"PROCESSED".equalsIgnoreCase(file.processStatus())) {
            risks.add("文件处理状态不是已处理。");
        }
        return risks;
    }

    private boolean containsAny(String haystack, String... needles) {
        if (haystack == null) return false;
        for (String needle : needles) {
            if (haystack.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String ownershipTypeLabel(String type) {
        return switch (safeText(type, "").toUpperCase(Locale.ROOT)) {
            case "DELIVERY" -> "正式交付资料";
            case "PROCESS" -> "过程资料";
            case "MODEL" -> "BIM/模型资料";
            case "DRAWING_EXCHANGE" -> "图纸收发资料";
            case "REFERENCE" -> "参考资料";
            case "ARCHIVE" -> "归档资料";
            default -> "待判定资料";
        };
    }

    private String statusLabel(String status) {
        return switch (safeText(status, "").toUpperCase(Locale.ROOT)) {
            case "CONFIRMED" -> "已确认";
            case "SUGGESTED" -> "建议中";
            case "REJECTED" -> "已驳回";
            default -> "未归属";
        };
    }

    private String normalizeOwnershipType(String type) {
        String normalized = safeText(type, "PENDING_REVIEW").toUpperCase(Locale.ROOT);
        if (List.of("DELIVERY", "PROCESS", "MODEL", "DRAWING_EXCHANGE", "REFERENCE", "ARCHIVE", "PENDING_REVIEW").contains(normalized)) {
            return normalized;
        }
        return "PENDING_REVIEW";
    }

    private String normalizeConfidence(String confidence) {
        String normalized = safeText(confidence, "MEDIUM").toUpperCase(Locale.ROOT);
        if (List.of("HIGH", "MEDIUM", "LOW").contains(normalized)) {
            return normalized;
        }
        return "MEDIUM";
    }

    private String normalizeSource(String source) {
        String normalized = safeText(source, "RULE").toUpperCase(Locale.ROOT);
        if (List.of("RULE", "HERMES", "MANUAL").contains(normalized)) {
            return normalized;
        }
        return "RULE";
    }

    private String normalizeReadOwnershipType(String type) {
        String normalized = type == null || type.isBlank() ? null : type.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || "ALL".equals(normalized)) {
            return null;
        }
        if (List.of("DELIVERY", "PROCESS", "MODEL", "DRAWING_EXCHANGE", "REFERENCE", "ARCHIVE", "PENDING_REVIEW").contains(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeReadStatus(String status) {
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || "ALL".equals(normalized)) {
            return null;
        }
        if (List.of("CONFIRMED", "SUGGESTED", "REJECTED").contains(normalized)) {
            return normalized;
        }
        return null;
    }

    private String normalizeReviewAction(String action) {
        String normalized = safeText(action, "").toUpperCase(Locale.ROOT);
        if (List.of("CONFIRM", "REJECT", "UPDATE_TYPE", "MOVE_NODE", "UPDATE_NODE_AND_TYPE").contains(normalized)) {
            return normalized;
        }
        throw new BusinessException("FILE_OWNERSHIP_ACTION_UNSUPPORTED",
            "不支持的归属复核动作", HttpStatus.BAD_REQUEST);
    }

    private boolean requiresOwnershipType(String action) {
        return "UPDATE_TYPE".equals(action) || "UPDATE_NODE_AND_TYPE".equals(action);
    }

    private boolean requiresNode(String action) {
        return "MOVE_NODE".equals(action) || "UPDATE_NODE_AND_TYPE".equals(action);
    }

    private String reviewReason(String action, String ownershipType, String nodePath) {
        return switch (action) {
            case "CONFIRM" -> "人工复核确认当前归属。";
            case "REJECT" -> "人工复核驳回当前归属，需重新判断。";
            case "UPDATE_TYPE" -> "人工复核调整归属类型为“" + ownershipTypeLabel(ownershipType) + "”。";
            case "MOVE_NODE" -> "人工复核移动到工程节点“" + nodePath + "”。";
            case "UPDATE_NODE_AND_TYPE" -> "人工复核调整到工程节点“" + nodePath + "”，归属类型为“" + ownershipTypeLabel(ownershipType) + "”。";
            default -> "人工复核文件归属。";
        };
    }

    private String reviewSuccessMessage(String action, String ownershipType, String nodeLabel) {
        return switch (action) {
            case "CONFIRM" -> "已人工确认当前归属";
            case "REJECT" -> "已人工驳回当前归属";
            case "UPDATE_TYPE" -> "已调整为“" + ownershipTypeLabel(ownershipType) + "”";
            case "MOVE_NODE" -> "已移动到“" + nodeLabel + "”";
            case "UPDATE_NODE_AND_TYPE" -> "已移动到“" + nodeLabel + "”并调整为“" + ownershipTypeLabel(ownershipType) + "”";
            default -> "已更新归属复核结果";
        };
    }

    private String nodeKeyForResult(String action, CurrentAssignment current, String nextNodeKey) {
        return requiresNode(action) ? nextNodeKey : current.nodeKey();
    }

    private String nodeLabelForResult(String action, CurrentAssignment current, String nextNodeLabel) {
        return requiresNode(action) ? nextNodeLabel : current.nodeLabel();
    }

    private String safeLogicalPath(
        String logicalPath,
        String fileName,
        String projectCode,
        String projectName,
        List<PathMappingResponse> mappings
    ) {
        if (logicalPath == null || logicalPath.isBlank()) {
            return fileName;
        }
        String normalized = normalizeProviderPath(logicalPath);
        for (PathMappingResponse mapping : mappings) {
            String root = normalizeProviderPath(mapping.nasPath());
            if (!root.isBlank() && (normalized.equals(root) || normalized.startsWith(root + "/"))) {
                String relative = normalized.length() == root.length() ? "" : normalized.substring(root.length() + 1);
                return relative.isBlank() ? fileName : relative;
            }
        }
        String projectRelative = relativeByMarker(normalized, projectCode);
        if (!projectRelative.isBlank()) return projectRelative;
        projectRelative = relativeByMarker(normalized, projectName);
        if (!projectRelative.isBlank()) return projectRelative;
        if (looksLikePhysicalPath(normalized)) return fileName;
        return trimLeadingSlash(normalized);
    }

    private String relativeByMarker(String path, String marker) {
        if (path == null || marker == null || marker.isBlank()) return "";
        int index = path.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
        if (index < 0) return "";
        String suffix = path.substring(index + marker.length());
        return trimLeadingSlash(suffix);
    }

    private String normalizeProviderPath(String path) {
        if (path == null) return "";
        String normalized = path.trim().replace('\\', '/');
        if (normalized.startsWith("nas://")) normalized = normalized.substring("nas://".length());
        while (normalized.contains("//")) normalized = normalized.replace("//", "/");
        while (normalized.length() > 1 && normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private boolean looksLikePhysicalPath(String path) {
        if (path == null) return false;
        String lower = path.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.startsWith("/volumes/")
            || lower.startsWith("volumes/")
            || lower.startsWith("/users/")
            || lower.startsWith("users/")
            || lower.startsWith("/tmp/")
            || lower.startsWith("tmp/")
            || lower.startsWith("/private/")
            || lower.startsWith("private/")
            || lower.startsWith("/var/")
            || lower.startsWith("var/")
            || lower.startsWith("/mnt/")
            || lower.startsWith("mnt/")
            || lower.startsWith("/data/")
            || lower.startsWith("data/")
            || lower.startsWith("//")
            || lower.startsWith("\\\\");
    }

    private String trimLeadingSlash(String value) {
        String next = value == null ? "" : value.trim().replace('\\', '/');
        while (next.startsWith("/")) {
            next = next.substring(1);
        }
        return next;
    }

    private String pathHint(String path) {
        if (path == null || path.isBlank()) return "文件名";
        int slash = path.lastIndexOf('/');
        if (slash <= 0) return path;
        return path.substring(0, slash);
    }

    private String nodePathLeaf(String path) {
        if (path == null || path.isBlank()) return "待判定";
        String normalized = trimLeadingSlash(path);
        int slash = normalized.lastIndexOf('/');
        if (slash < 0 || slash == normalized.length() - 1) return safeText(normalized, "待判定");
        return safeText(normalized.substring(slash + 1), "待判定");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String sanitizeDisplayText(String value, String fallback) {
        String text = safeText(value, fallback)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .trim();
        if (containsForbiddenText(text)) {
            return fallback;
        }
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private List<String> sanitizeRisks(List<String> risks) {
        if (risks == null || risks.isEmpty()) {
            return List.of();
        }
        return risks.stream()
            .filter(Objects::nonNull)
            .map(risk -> sanitizeDisplayText(risk, "存在不可展示风险说明，已脱敏"))
            .limit(10)
            .toList();
    }

    private boolean containsForbiddenText(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.contains("/volumes/")
            || lower.startsWith("volumes/")
            || lower.contains("/users/")
            || lower.startsWith("users/")
            || lower.contains("smb://")
            || lower.contains("nas://")
            || lower.contains("afp://")
            || lower.contains("storage_path")
            || lower.contains("storage_uri")
            || lower.contains("storagepath")
            || lower.contains("storageuri")
            || lower.contains("raw_path")
            || lower.contains("nas_path")
            || lower.contains(" raw row")
            || lower.contains("raw db row")
            || lower.contains(" token")
            || lower.contains(" secret")
            || lower.contains(" password")
            || lower.matches(".*\\bselect\\s+.+\\s+from\\b.*")
            || lower.matches(".*\\binsert\\s+into\\b.*")
            || lower.matches(".*\\bupdate\\s+.+\\s+set\\b.*")
            || lower.matches(".*\\bdelete\\s+from\\b.*");
    }

    private String stableKey(String value) {
        String source = safeText(value, "NODE");
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD);
        String ascii = UNSAFE_NODE_KEY.matcher(normalized).replaceAll("_");
        while (ascii.contains("__")) ascii = ascii.replace("__", "_");
        ascii = ascii.replaceAll("^_+|_+$", "");
        if (ascii.isBlank()) {
            ascii = "NODE";
        }
        String suffix = Integer.toUnsignedString(source.hashCode(), 36);
        String candidate = ascii + "-" + suffix;
        return candidate.length() > 120 ? candidate.substring(0, 112) + "-" + suffix : candidate;
    }

    private String sanitizeNodePath(String path, String projectName) {
        String raw = path == null ? "" : path.trim().replace('\\', '/');
        if (containsForbiddenText(raw) || looksLikePhysicalPath(raw)) {
            return projectName + "/待判定资料/通用/项目通用";
        }
        String normalized = raw;
        while (normalized.contains("//")) normalized = normalized.replace("//", "/");
        normalized = trimLeadingSlash(normalized);
        if (containsForbiddenText(normalized) || looksLikePhysicalPath(normalized)) {
            return projectName + "/待判定资料/通用/项目通用";
        }
        return normalized.isBlank() ? projectName + "/待判定资料/通用/项目通用" : normalized;
    }

    private List<String> splitNodePath(String nodePath) {
        return List.of(safeText(nodePath, "").split("/")).stream()
            .map(String::trim)
            .filter(part -> !part.isBlank())
            .toList();
    }

    private record ProjectInfo(Long id, String code, String name, String roleCode) {
    }

    private record FileForOwnership(
        Long fileId,
        Long projectId,
        String projectCode,
        String projectName,
        String fileName,
        String fileKind,
        String fileExt,
        String disciplineCode,
        String version,
        Long sizeBytes,
        String displayPath,
        String processStatus
    ) {
    }

    private record AssignmentAggregate(
        String nodeKey,
        String nodeLabel,
        String nodePath,
        String ownershipType,
        String status,
        String source,
        Long sectionNodeId,
        int fileCount,
        int confirmedCount,
        int suggestedCount
    ) {
    }

    private record CurrentAssignment(
        Long fileId,
        String fileName,
        String nodeKey,
        String nodeLabel,
        String nodePath,
        String ownershipType,
        String status
    ) {
    }

    private static final class TreeAccumulator {
        private final String nodeKey;
        private final String nodeLabel;
        private final String nodePath;
        private final String ownershipType;
        private final String status;
        private final String source;
        private final Long sectionNodeId;
        private final Map<String, TreeAccumulator> children = new LinkedHashMap<>();
        private int fileCount;
        private int confirmedFileCount;
        private int suggestedFileCount;
        private int unassignedFileCount;

        private TreeAccumulator(
            String nodeKey,
            String nodeLabel,
            String nodePath,
            String ownershipType,
            String status,
            String source,
            Long sectionNodeId
        ) {
            this.nodeKey = nodeKey;
            this.nodeLabel = nodeLabel;
            this.nodePath = nodePath;
            this.ownershipType = ownershipType;
            this.status = status;
            this.source = source;
            this.sectionNodeId = sectionNodeId;
        }

        private void add(int files, int confirmed, int suggested) {
            fileCount += files;
            confirmedFileCount += confirmed;
            suggestedFileCount += suggested;
        }

        private FileOwnershipTreeNode toResponse() {
            int childFileCount = children.values().stream().mapToInt(child -> child.toResponse().fileCount()).sum();
            int childConfirmed = children.values().stream().mapToInt(child -> child.toResponse().confirmedFileCount()).sum();
            int childSuggested = children.values().stream().mapToInt(child -> child.toResponse().suggestedFileCount()).sum();
            List<FileOwnershipTreeNode> childRows = children.values().stream()
                .sorted(Comparator.comparing(child -> child.nodePath))
                .map(TreeAccumulator::toResponse)
                .toList();
            return new FileOwnershipTreeNode(
                nodeKey,
                nodeLabel,
                nodePath,
                ownershipType,
                status,
                source,
                sectionNodeId,
                Math.max(fileCount, childFileCount),
                Math.max(confirmedFileCount, childConfirmed),
                Math.max(suggestedFileCount, childSuggested),
                unassignedFileCount,
                0,
                0,
                0,
                childRows
            );
        }
    }
}

package com.zhuoyu.delivery.datasteward.asset.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService;
import com.zhuoyu.delivery.datasteward.asset.application.AssetApplicationService.FileAccessResource;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AccessTicketCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AccessTicketResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectArchiveRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectArchiveResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectCreateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.AssetProjectUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DisciplineResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetMetadataUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FilePreviewResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileAssetResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.FileStorageStatusResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ImportResultResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NasProjectDiscoveryRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NasProjectDiscoveryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryDiscoverRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryDiscoverResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.NonstandardDirectoryUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PathMappingUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.PreviewArtifactResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectLifecycleCreateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ReviewUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanCandidateResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanReportResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ScanTaskResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import com.zhuoyu.delivery.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data-steward/assets")
public class AssetController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final AssetApplicationService assetApplicationService;

    public AssetController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        AssetApplicationService assetApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.assetApplicationService = assetApplicationService;
    }

    private Long currentUserId() {
        return securityPrincipalAccessor.requireCurrentPrincipal().userId();
    }

    // ===== disciplines =====

    @GetMapping("/disciplines")
    public ApiResponse<List<DisciplineResponse>> listDisciplines(@RequestParam(required = false) Long projectId) {
        return ApiResponse.success(assetApplicationService.listDisciplines(currentUserId(), projectId));
    }

    // ===== project assets =====

    @GetMapping("/projects")
    public ApiResponse<List<AssetProjectResponse>> listProjects(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String assetSource
    ) {
        return ApiResponse.success(assetApplicationService.listProjects(currentUserId(), keyword, assetSource));
    }

    @PostMapping("/projects")
    public ApiResponse<AssetProjectResponse> createProject(@Valid @RequestBody AssetProjectCreateRequest request) {
        return ApiResponse.success(assetApplicationService.createProject(currentUserId(), request));
    }

    @PostMapping("/projects:lifecycle-create")
    public ApiResponse<ProjectLifecycleCreateResponse> createProjectLifecycle(
        @Valid @RequestBody AssetProjectCreateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.createProjectLifecycle(currentUserId(), request));
    }

    @PostMapping("/projects/{projectId}:archive")
    public ApiResponse<AssetProjectArchiveResponse> archiveProject(
        @PathVariable Long projectId,
        @RequestBody AssetProjectArchiveRequest request
    ) {
        return ApiResponse.success(assetApplicationService.archiveProject(currentUserId(), projectId, request));
    }

    @PatchMapping("/projects/{projectId}")
    public ApiResponse<AssetProjectResponse> updateProject(
        @PathVariable Long projectId,
        @Valid @RequestBody AssetProjectUpdateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.updateProject(currentUserId(), projectId, request));
    }

    @PostMapping(path = "/projects:import", consumes = "multipart/form-data")
    public ApiResponse<ImportResultResponse> importProjectsMultipart(
        @RequestParam(required = false) String sourceName,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        String csvText = readImportFile(file);
        return ApiResponse.success(assetApplicationService.importProjectsFromCsv(
            currentUserId(), sourceName != null ? sourceName : fileName(file), csvText));
    }

    @PostMapping("/projects:import")
    public ApiResponse<ImportResultResponse> importProjects(
        @RequestParam(required = false) String sourceName,
        @RequestBody String csvText
    ) {
        return ApiResponse.success(assetApplicationService.importProjectsFromCsv(
            currentUserId(), sourceName != null ? sourceName : "CSV_IMPORT", csvText));
    }

    // ===== path mappings =====

    @GetMapping("/path-mappings")
    public ApiResponse<List<PathMappingResponse>> listPathMappings(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) Boolean enabled
    ) {
        return ApiResponse.success(assetApplicationService.listPathMappingsForUser(currentUserId(), projectId, enabled));
    }

    @PostMapping("/path-mappings")
    public ApiResponse<Long> createPathMapping(@Valid @RequestBody PathMappingRequest request) {
        return ApiResponse.success(assetApplicationService.createPathMapping(
            currentUserId(), request.projectId(), request.providerCode(),
            request.nasPath(), request.matchStrategy(),
            request.sortOrder(), request.remark()));
    }

    @PatchMapping("/path-mappings/{mappingId}")
    public ApiResponse<Void> updatePathMapping(
        @PathVariable Long mappingId,
        @Valid @RequestBody PathMappingUpdateRequest request
    ) {
        assetApplicationService.updatePathMapping(currentUserId(), mappingId,
            request.nasPath(), request.matchStrategy(), request.enabled(),
            request.sortOrder(), request.remark());
        return ApiResponse.success();
    }

    @DeleteMapping("/path-mappings/{mappingId}")
    public ApiResponse<Void> deletePathMapping(@PathVariable Long mappingId) {
        assetApplicationService.deletePathMapping(currentUserId(), mappingId);
        return ApiResponse.success();
    }

    @PostMapping(path = "/path-mappings:import", consumes = "multipart/form-data")
    public ApiResponse<ImportResultResponse> importPathMappingsMultipart(
        @RequestParam(required = false) String sourceName,
        @RequestParam("file") MultipartFile file
    ) throws IOException {
        String csvText = readImportFile(file);
        return ApiResponse.success(assetApplicationService.importPathMappingsFromCsv(
            currentUserId(), sourceName != null ? sourceName : fileName(file), csvText));
    }

    @PostMapping("/path-mappings:import")
    public ApiResponse<ImportResultResponse> importPathMappings(
        @RequestParam(required = false) String sourceName,
        @RequestBody String csvText
    ) {
        return ApiResponse.success(assetApplicationService.importPathMappingsFromCsv(
            currentUserId(), sourceName != null ? sourceName : "PATH_MAPPING_IMPORT", csvText));
    }

    // ===== NAS project discovery =====

    @PostMapping("/nas-projects:discover")
    public ApiResponse<NasProjectDiscoveryResponse> discoverNasProjects(
        @Valid @RequestBody NasProjectDiscoveryRequest request
    ) {
        return ApiResponse.success(assetApplicationService.discoverNasProjects(currentUserId(), request));
    }

    // ===== nonstandard NAS directory governance =====

    @GetMapping("/nonstandard-directories")
    public ApiResponse<List<NonstandardDirectoryResponse>> listNonstandardDirectories(
        @RequestParam(required = false) String governanceStatus,
        @RequestParam(required = false) String riskType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(assetApplicationService.listNonstandardDirectories(
            currentUserId(), governanceStatus, riskType, keyword, limit));
    }

    @PostMapping("/nonstandard-directories:discover")
    public ApiResponse<NonstandardDirectoryDiscoverResponse> discoverNonstandardDirectories(
        @Valid @RequestBody NonstandardDirectoryDiscoverRequest request
    ) {
        return ApiResponse.success(assetApplicationService.discoverNonstandardDirectories(currentUserId(), request));
    }

    @PatchMapping("/nonstandard-directories/{directoryId}")
    public ApiResponse<NonstandardDirectoryResponse> updateNonstandardDirectory(
        @PathVariable Long directoryId,
        @Valid @RequestBody NonstandardDirectoryUpdateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.updateNonstandardDirectory(
            currentUserId(), directoryId, request));
    }

    // ===== NAS scans =====

    @PostMapping("/nas-scans")
    public ApiResponse<ScanTaskResponse> createScan(@RequestBody AssetDtos.NasScanRequest request) {
        return ApiResponse.success(assetApplicationService.createScan(
            currentUserId(), request.rootCode(), request.rootPath(),
            request.projectId(), request.projectCode(),
            request.recursive(), request.extensions(),
            request.skipLowValueDirectories(), request.skipDirectoryKeywords()));
    }

    @GetMapping("/nas-scans")
    public ApiResponse<List<ScanTaskResponse>> listScans() {
        return ApiResponse.success(assetApplicationService.listScansForUser(currentUserId()));
    }

    @GetMapping("/nas-scans/{scanTaskId}")
    public ApiResponse<ScanTaskResponse> getScan(@PathVariable Long scanTaskId) {
        return ApiResponse.success(assetApplicationService.getScanForUser(currentUserId(), scanTaskId));
    }

    @PostMapping("/nas-scans/{scanTaskId}:run")
    public ApiResponse<ScanTaskResponse> runScan(@PathVariable Long scanTaskId) {
        return ApiResponse.success(assetApplicationService.runScan(currentUserId(), scanTaskId));
    }

    @PostMapping("/nas-scans/{scanTaskId}:cancel")
    public ApiResponse<ScanTaskResponse> cancelScan(@PathVariable Long scanTaskId) {
        return ApiResponse.success(assetApplicationService.cancelScan(currentUserId(), scanTaskId));
    }

    @PostMapping("/nas-scans/{scanTaskId}:resume")
    public ApiResponse<ScanTaskResponse> resumeScan(@PathVariable Long scanTaskId) {
        return ApiResponse.success(assetApplicationService.resumeScan(currentUserId(), scanTaskId));
    }

    @GetMapping("/nas-scans/{scanTaskId}/report")
    public ApiResponse<ScanReportResponse> getScanReport(@PathVariable Long scanTaskId) {
        return ApiResponse.success(assetApplicationService.getScanReport(currentUserId(), scanTaskId));
    }

    // ===== review candidates =====

    @GetMapping("/review-candidates")
    public ApiResponse<List<ScanCandidateResponse>> listReviewCandidates(
        @RequestParam(required = false) String reviewStatus
    ) {
        return ApiResponse.success(assetApplicationService.listReviewCandidates(currentUserId(), reviewStatus));
    }

    @PatchMapping("/review-candidates/{candidateId}")
    public ApiResponse<ScanCandidateResponse> updateReviewCandidate(
        @PathVariable Long candidateId,
        @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.updateReviewCandidate(
            currentUserId(), candidateId, request));
    }

    @PostMapping("/review-candidates/{candidateId}:approve")
    public ApiResponse<ScanCandidateResponse> approveCandidate(@PathVariable Long candidateId) {
        return ApiResponse.success(assetApplicationService.approveCandidate(currentUserId(), candidateId));
    }

    @PostMapping("/review-candidates/{candidateId}:reject")
    public ApiResponse<ScanCandidateResponse> rejectCandidate(
        @PathVariable Long candidateId,
        @RequestBody(required = false) ReviewUpdateRequest request
    ) {
        String msg = request != null ? request.reviewMessage() : null;
        return ApiResponse.success(assetApplicationService.rejectCandidate(currentUserId(), candidateId, msg));
    }

    @PostMapping("/review-candidates:bulk-approve")
    public ApiResponse<Void> bulkApprove(@RequestBody List<Long> candidateIds) {
        assetApplicationService.bulkApproveCandidates(currentUserId(), candidateIds);
        return ApiResponse.success();
    }

    // ===== file assets =====

    @GetMapping("/files")
    public ApiResponse<List<FileAssetResponse>> listFiles(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String fileKind,
        @RequestParam(required = false) String discipline,
        @RequestParam(required = false) String fileName,
        @RequestParam(required = false) String fileExt,
        @RequestParam(required = false) String sourceType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String qualityIssue
    ) {
        return ApiResponse.success(assetApplicationService.listFileAssetsForDisplay(
            currentUserId(), projectId, fileKind, discipline,
            fileName, fileExt, sourceType, keyword, qualityIssue));
    }

    @GetMapping("/files:page")
    public ApiResponse<PageResponse<FileAssetResponse>> listFilesPage(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String fileKind,
        @RequestParam(required = false) String discipline,
        @RequestParam(required = false) String fileName,
        @RequestParam(required = false) String fileExt,
        @RequestParam(required = false) String sourceType,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String assetSource,
        @RequestParam(required = false) String qualityIssue,
        @RequestParam(required = false, defaultValue = "1") Integer pageNo,
        @RequestParam(required = false, defaultValue = "50") Integer pageSize
    ) {
        return ApiResponse.success(assetApplicationService.listFileAssetsPageForDisplay(
            currentUserId(), projectId, fileKind, discipline,
            fileName, fileExt, sourceType, keyword, assetSource, qualityIssue, pageNo, pageSize));
    }

    @GetMapping("/files/{fileId}")
    public ApiResponse<FileAssetResponse> getFile(@PathVariable Long fileId) {
        return ApiResponse.success(assetApplicationService.getFileByIdForDisplay(currentUserId(), fileId));
    }

    @GetMapping("/files/{fileId}/preview")
    public ApiResponse<FilePreviewResponse> getFilePreview(@PathVariable Long fileId) {
        return ApiResponse.success(assetApplicationService.getFilePreview(currentUserId(), fileId));
    }

    @GetMapping("/files/{fileId}/storage-status")
    public ApiResponse<FileStorageStatusResponse> getFileStorageStatus(@PathVariable Long fileId) {
        return ApiResponse.success(assetApplicationService.getFileStorageStatus(currentUserId(), fileId));
    }

    @GetMapping("/files/{fileId}/preview-artifacts")
    public ApiResponse<List<PreviewArtifactResponse>> listPreviewArtifacts(@PathVariable Long fileId) {
        return ApiResponse.success(assetApplicationService.listPreviewArtifacts(currentUserId(), fileId));
    }

    @PostMapping("/files/{fileId}/preview-artifacts:prepare")
    public ApiResponse<PreviewArtifactResponse> preparePreviewArtifact(@PathVariable Long fileId) {
        return ApiResponse.success(assetApplicationService.preparePreviewArtifact(currentUserId(), fileId));
    }

    @PostMapping("/files/{fileId}/access-tickets")
    public ApiResponse<AccessTicketResponse> createFileAccessTicket(
        @PathVariable Long fileId,
        @Valid @RequestBody AccessTicketCreateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.createFileAccessTicket(currentUserId(), fileId, request));
    }

    @GetMapping("/file-access/{ticket}")
    public ResponseEntity<Resource> openFileAccess(@PathVariable String ticket) {
        FileAccessResource access = assetApplicationService.openFileAccessTicket(ticket);
        ContentDisposition disposition = ContentDisposition
            .builder(access.dispositionType())
            .filename(access.fileName(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(access.contentType()))
            .contentLength(access.contentLength())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header("X-Delivery-Storage-Status", safeHeader(access.storageStatus()))
            .header("X-Delivery-Read-Source", safeHeader(access.readSource()))
            .header("X-Delivery-Fallback-Used", String.valueOf(Boolean.TRUE.equals(access.fallbackUsed())))
            .header("X-Delivery-Fallback-Reason", safeHeader(access.fallbackReason()))
            .header("X-Delivery-Storage-Health", safeHeader(access.storageHealth()))
            .header("X-Delivery-Object-Readable", String.valueOf(Boolean.TRUE.equals(access.objectReadable())))
            .body(access.resource());
    }

    @PatchMapping("/files/{fileId}/metadata")
    public ApiResponse<FileAssetResponse> updateFileMetadata(
        @PathVariable Long fileId,
        @RequestBody FileAssetMetadataUpdateRequest request
    ) {
        return ApiResponse.success(assetApplicationService.updateFileMetadata(currentUserId(), fileId, request));
    }

    // ===== import file helpers =====

    private static String readImportFile(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name != null && name.toLowerCase().endsWith(".xlsx")) {
            return readXlsxFirstSheet(file);
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private static String readXlsxFirstSheet(MultipartFile file) throws IOException {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                List<String> cells = new ArrayList<>();
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cells.add(cellValue(cell));
                }
                sb.append(String.join(",", cells));
                if (i < sheet.getLastRowNum()) sb.append('\n');
            }
            return sb.toString();
        }
    }

    private static String cellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) yield String.valueOf((long) v);
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static String fileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && !name.isBlank() ? name : "UPLOAD";
    }

    private static String safeHeader(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        return value.replaceAll("[\\r\\n]+", " ").trim();
    }
}

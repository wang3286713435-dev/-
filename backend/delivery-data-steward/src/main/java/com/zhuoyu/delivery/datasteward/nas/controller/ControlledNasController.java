package com.zhuoyu.delivery.datasteward.nas.controller;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.datasteward.nas.application.ControlledNasApplicationService;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryCreateRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryMoveRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryQuarantineRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.DirectoryRenameRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileMoveRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileQuarantineRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.FileRenameRequest;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationRecordResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasOperationResponse;
import com.zhuoyu.delivery.datasteward.nas.dto.ControlledNasDtos.NasQuarantineRecordResponse;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/data-steward/projects/{projectId}/nas")
public class ControlledNasController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ControlledNasApplicationService controlledNasApplicationService;

    public ControlledNasController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ControlledNasApplicationService controlledNasApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.controlledNasApplicationService = controlledNasApplicationService;
    }

    @PostMapping("/directories")
    public ApiResponse<NasOperationResponse> createDirectory(
        @PathVariable Long projectId,
        @Valid @RequestBody DirectoryCreateRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.createDirectory(principal.userId(), projectId, request));
    }

    @PatchMapping("/directories:rename")
    public ApiResponse<NasOperationResponse> renameDirectory(
        @PathVariable Long projectId,
        @Valid @RequestBody DirectoryRenameRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.renameDirectory(principal.userId(), projectId, request));
    }

    @PostMapping("/directories:move")
    public ApiResponse<NasOperationResponse> moveDirectory(
        @PathVariable Long projectId,
        @Valid @RequestBody DirectoryMoveRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.moveDirectory(principal.userId(), projectId, request));
    }

    @PostMapping("/directories:quarantine")
    public ApiResponse<NasOperationResponse> quarantineDirectory(
        @PathVariable Long projectId,
        @Valid @RequestBody DirectoryQuarantineRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.quarantineDirectory(principal.userId(), projectId, request));
    }

    @PostMapping(path = "/files:upload", consumes = "multipart/form-data")
    public ApiResponse<NasOperationResponse> uploadFile(
        @PathVariable Long projectId,
        @RequestParam(required = false) String parentPath,
        @RequestParam(required = false) String fileKind,
        @RequestParam(required = false) String discipline,
        @RequestParam(required = false) String versionNo,
        @RequestParam("file") MultipartFile file
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.uploadFile(
            principal.userId(), projectId, parentPath, fileKind, discipline, versionNo, file));
    }

    @PatchMapping("/files/{fileId}:rename")
    public ApiResponse<NasOperationResponse> renameFile(
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @Valid @RequestBody FileRenameRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.renameFile(principal.userId(), projectId, fileId, request));
    }

    @PostMapping("/files/{fileId}:move")
    public ApiResponse<NasOperationResponse> moveFile(
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @RequestBody(required = false) FileMoveRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.moveFile(principal.userId(), projectId, fileId, request));
    }

    @PostMapping("/files/{fileId}:quarantine")
    public ApiResponse<NasOperationResponse> quarantineFile(
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @RequestBody(required = false) FileQuarantineRequest request
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.quarantineFile(principal.userId(), projectId, fileId, request));
    }

    @PostMapping("/quarantine/{recordId}:restore")
    public ApiResponse<NasOperationResponse> restoreQuarantine(
        @PathVariable Long projectId,
        @PathVariable Long recordId
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.restoreQuarantine(principal.userId(), projectId, recordId));
    }

    @GetMapping("/quarantine")
    public ApiResponse<List<NasQuarantineRecordResponse>> listQuarantine(
        @PathVariable Long projectId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.listQuarantineRecords(
            principal.userId(), projectId, status, limit));
    }

    @GetMapping("/operations")
    public ApiResponse<List<NasOperationRecordResponse>> listOperations(
        @PathVariable Long projectId,
        @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        var principal = requirePrincipal();
        return ApiResponse.success(controlledNasApplicationService.listOperations(principal.userId(), projectId, limit));
    }

    private com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal requirePrincipal() {
        return securityPrincipalAccessor.requireCurrentPrincipal();
    }
}

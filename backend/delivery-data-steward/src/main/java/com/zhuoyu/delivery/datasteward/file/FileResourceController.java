package com.zhuoyu.delivery.datasteward.file;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.FileResourceResponse;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ProcessFileRequest;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/projects/{projectId}/file-resources")
public class FileResourceController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final FileResourceApplicationService fileResourceApplicationService;

    public FileResourceController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        FileResourceApplicationService fileResourceApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.fileResourceApplicationService = fileResourceApplicationService;
    }

    @PostMapping
    public ApiResponse<FileResourceResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody FileResourceRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(fileResourceApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<FileResourceResponse>> list(
        @PathVariable Long projectId,
        @RequestParam(required = false) String fileKind
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(fileResourceApplicationService.list(projectId, fileKind));
    }

    @PatchMapping("/{fileId}:process")
    public ApiResponse<FileResourceResponse> process(
        @PathVariable Long projectId,
        @PathVariable Long fileId,
        @RequestBody(required = false) ProcessFileRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(fileResourceApplicationService.process(principal.userId(), projectId, fileId, request));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long fileId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        fileResourceApplicationService.delete(principal.userId(), projectId, fileId);
        return ApiResponse.success();
    }
}

package com.zhuoyu.delivery.datasteward.object;

import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectRequest;
import com.zhuoyu.delivery.datasteward.dto.DataStewardDtos.ManagedObjectResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-steward/projects/{projectId}/managed-objects")
public class ManagedObjectController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final ManagedObjectApplicationService managedObjectApplicationService;

    public ManagedObjectController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        ManagedObjectApplicationService managedObjectApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.managedObjectApplicationService = managedObjectApplicationService;
    }

    @PostMapping
    public ApiResponse<ManagedObjectResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody ManagedObjectRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(managedObjectApplicationService.create(principal.userId(), projectId, request));
    }

    @GetMapping
    public ApiResponse<List<ManagedObjectResponse>> list(@PathVariable Long projectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(managedObjectApplicationService.list(projectId));
    }

    @PatchMapping("/{objectId}")
    public ApiResponse<ManagedObjectResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long objectId,
        @Valid @RequestBody ManagedObjectRequest request
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(managedObjectApplicationService.update(principal.userId(), projectId, objectId, request));
    }

    @DeleteMapping("/{objectId}")
    public ApiResponse<Void> delete(@PathVariable Long projectId, @PathVariable Long objectId) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        managedObjectApplicationService.delete(principal.userId(), projectId, objectId);
        return ApiResponse.success();
    }
}

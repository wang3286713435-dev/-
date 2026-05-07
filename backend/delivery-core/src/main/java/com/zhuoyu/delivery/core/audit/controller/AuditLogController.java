package com.zhuoyu.delivery.core.audit.controller;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.audit.dto.AuditLogResponse;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.shared.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/projects/{projectId}/audit-logs")
public class AuditLogController {

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public AuditLogController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> list(
        @PathVariable Long projectId,
        @RequestParam(required = false) String moduleCode,
        @RequestParam(defaultValue = "50") int limit
    ) {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        return ApiResponse.success(auditLogApplicationService.listLatest(projectId, moduleCode, limit));
    }
}

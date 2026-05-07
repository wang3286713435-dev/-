package com.zhuoyu.delivery.core.project.application;

import com.zhuoyu.delivery.core.auth.domain.AuthenticatedPrincipal;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProjectContextApplicationService {

    private final ProjectAccessApplicationService projectAccessApplicationService;

    public ProjectContextApplicationService(ProjectAccessApplicationService projectAccessApplicationService) {
        this.projectAccessApplicationService = projectAccessApplicationService;
    }

    public void requireCurrentProject(AuthenticatedPrincipal principal, Long projectId) {
        if (!projectId.equals(principal.currentProjectId())) {
            throw new BusinessException("CORE_PROJECT_CONTEXT_MISMATCH", "请先切换到目标项目", HttpStatus.FORBIDDEN);
        }
        projectAccessApplicationService.requireAccessibleProject(principal.userId(), projectId);
    }
}

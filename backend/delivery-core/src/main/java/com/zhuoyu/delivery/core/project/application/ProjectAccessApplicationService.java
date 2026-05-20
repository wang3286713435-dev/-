package com.zhuoyu.delivery.core.project.application;

import com.zhuoyu.delivery.core.project.domain.AccessibleProject;
import com.zhuoyu.delivery.core.project.dto.ProjectSummaryResponse;
import com.zhuoyu.delivery.core.project.repository.ProjectAccessRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProjectAccessApplicationService {

    private final ProjectAccessRepository projectAccessRepository;

    public ProjectAccessApplicationService(ProjectAccessRepository projectAccessRepository) {
        this.projectAccessRepository = projectAccessRepository;
    }

    public List<AccessibleProject> listAccessibleProjects(Long userId) {
        return projectAccessRepository.findAccessibleProjects(userId);
    }

    public AccessibleProject requireAccessibleProject(Long userId, Long projectId) {
        return projectAccessRepository.findAccessibleProject(userId, projectId)
            .orElseThrow(() -> new BusinessException("CORE_PROJECT_NOT_FOUND", "当前项目不存在或无权访问", HttpStatus.NOT_FOUND));
    }

    public ProjectSummaryResponse toResponse(AccessibleProject project) {
        return new ProjectSummaryResponse(
            project.id(),
            project.code(),
            project.name(),
            project.industryType(),
            project.status(),
            project.projectManagerName(),
            project.roleCode(),
            project.roleName()
        );
    }
}

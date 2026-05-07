package com.zhuoyu.delivery.core.user.dto;

import com.zhuoyu.delivery.core.project.dto.ProjectSummaryResponse;
import java.util.List;

public record CurrentUserResponse(
    Long userId,
    String username,
    String displayName,
    ProjectSummaryResponse currentProject,
    List<ProjectSummaryResponse> projects,
    List<String> permissions,
    List<MenuItemResponse> menus
) {
}

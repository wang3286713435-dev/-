package com.zhuoyu.delivery.core.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeeProjectRoleItem(
    @NotNull(message = "请选择项目")
    Long projectId,

    @NotBlank(message = "请选择项目角色")
    String roleCode
) {
}

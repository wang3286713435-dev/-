package com.zhuoyu.delivery.core.user.dto;

import jakarta.validation.Valid;
import java.util.List;

public record EmployeeProjectRoleUpdateRequest(
    List<@Valid EmployeeProjectRoleItem> assignments
) {
}

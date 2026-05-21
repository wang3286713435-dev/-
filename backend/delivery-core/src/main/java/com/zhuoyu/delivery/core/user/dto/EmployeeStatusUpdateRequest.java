package com.zhuoyu.delivery.core.user.dto;

import jakarta.validation.constraints.NotBlank;

public record EmployeeStatusUpdateRequest(
    @NotBlank(message = "请选择账号状态")
    String status
) {
}

package com.zhuoyu.delivery.core.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @NotBlank(message = "请输入姓名")
    @Size(max = 128, message = "姓名不能超过 128 个字符")
    String displayName,

    @Size(max = 128, message = "部门不能超过 128 个字符")
    String departmentName
) {
}

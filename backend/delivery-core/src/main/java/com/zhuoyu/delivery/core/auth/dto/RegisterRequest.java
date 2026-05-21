package com.zhuoyu.delivery.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    String phoneNumber,

    @NotBlank(message = "请输入姓名")
    @Size(max = 128, message = "姓名不能超过 128 个字符")
    String displayName,

    @Size(max = 128, message = "部门不能超过 128 个字符")
    String departmentName,

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
    String password
) {
}

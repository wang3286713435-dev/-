package com.zhuoyu.delivery.core.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9._-]{2,31}$", message = "用户名需以字母开头，支持字母、数字、点、下划线和短横线，长度 3-32 位")
    String username,

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    String phoneNumber,

    @NotBlank(message = "请输入姓名")
    @Size(max = 128, message = "姓名不能超过 128 个字符")
    String displayName,

    @Size(max = 128, message = "部门不能超过 128 个字符")
    String departmentName,

    @NotBlank(message = "请输入初始密码")
    @Size(min = 6, max = 64, message = "密码长度应为 6-64 位")
    String password
) {
}

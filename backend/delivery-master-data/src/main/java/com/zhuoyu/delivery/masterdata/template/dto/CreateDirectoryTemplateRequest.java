package com.zhuoyu.delivery.masterdata.template.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDirectoryTemplateRequest(
    @NotBlank(message = "模板类型不能为空")
    String templateType,
    @NotBlank(message = "名称不能为空")
    String name,
    String rootNodeJson,
    String sourceType,
    Integer sortOrder,
    String status
) {
}

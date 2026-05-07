package com.zhuoyu.delivery.masterdata.section.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionNodeRequest(
    Long parentId,

    @NotBlank(message = "部位编码不能为空")
    @Size(max = 64, message = "部位编码不能超过 64 个字符")
    String code,

    @NotBlank(message = "部位名称不能为空")
    @Size(max = 128, message = "部位名称不能超过 128 个字符")
    String name,

    @Min(value = 0, message = "排序值不能小于 0")
    Integer sortOrder,

    String status
) {
}

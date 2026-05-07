package com.zhuoyu.delivery.masterdata.section.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSectionNodeRequest(
    Long parentId,

    @Size(max = 64, message = "部位编码不能超过 64 个字符")
    String code,

    @Size(max = 128, message = "部位名称不能超过 128 个字符")
    String name,

    @Min(value = 0, message = "排序值不能小于 0")
    Integer sortOrder,

    String status
) {
}

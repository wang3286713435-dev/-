package com.zhuoyu.delivery.masterdata.nodetype.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateNodeTypeRequest(
    @Size(max = 64, message = "节点类型编码不能超过 64 个字符")
    String code,

    @Size(max = 128, message = "节点类型名称不能超过 128 个字符")
    String name,

    @Min(value = 1, message = "适用层级不能小于 1")
    Integer scopeLevel,

    @Min(value = 0, message = "排序值不能小于 0")
    Integer sortOrder,

    String status
) {
}

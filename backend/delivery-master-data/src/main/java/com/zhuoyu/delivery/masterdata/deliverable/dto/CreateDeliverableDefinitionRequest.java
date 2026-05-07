package com.zhuoyu.delivery.masterdata.deliverable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliverableDefinitionRequest(
    @NotNull(message = "节点类型ID不能为空")
    Long nodeTypeId,
    @NotBlank(message = "编码不能为空")
    String code,
    @NotBlank(message = "名称不能为空")
    String name,
    String category,
    Boolean required,
    Integer sortOrder,
    String status
) {
}

package com.zhuoyu.delivery.masterdata.deliverable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliverableTypeRequest(
    @NotNull(message = "交付物定义ID不能为空")
    Long deliverableDefinitionId,
    @NotBlank(message = "编码不能为空")
    String code,
    @NotBlank(message = "名称不能为空")
    String name,
    String fileKind,
    String bindingStrategy,
    Integer sortOrder,
    String status
) {
}

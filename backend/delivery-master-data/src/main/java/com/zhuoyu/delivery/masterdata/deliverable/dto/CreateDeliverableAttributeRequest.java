package com.zhuoyu.delivery.masterdata.deliverable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliverableAttributeRequest(
    @NotNull(message = "交付物类型ID不能为空")
    Long deliverableTypeId,
    @NotBlank(message = "编码不能为空")
    String code,
    @NotBlank(message = "名称不能为空")
    String name,
    String valueType,
    String unit,
    Boolean required,
    String exampleValue,
    String enumOptions,
    Integer sortOrder,
    String status
) {
}

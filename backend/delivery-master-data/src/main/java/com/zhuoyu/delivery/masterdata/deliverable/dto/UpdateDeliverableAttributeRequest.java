package com.zhuoyu.delivery.masterdata.deliverable.dto;

public record UpdateDeliverableAttributeRequest(
    String code,
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

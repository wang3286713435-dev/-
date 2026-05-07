package com.zhuoyu.delivery.masterdata.deliverable.dto;

public record UpdateDeliverableDefinitionRequest(
    String code,
    String name,
    String category,
    Boolean required,
    Integer sortOrder,
    String status
) {
}

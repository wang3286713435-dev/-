package com.zhuoyu.delivery.masterdata.deliverable.dto;

public record DeliverableDefinitionResponse(
    Long id,
    Long projectId,
    Long nodeTypeId,
    String code,
    String name,
    String category,
    Boolean required,
    Integer sortOrder,
    String status
) {
}

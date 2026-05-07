package com.zhuoyu.delivery.masterdata.deliverable.domain;

public record DeliverableDefinition(
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

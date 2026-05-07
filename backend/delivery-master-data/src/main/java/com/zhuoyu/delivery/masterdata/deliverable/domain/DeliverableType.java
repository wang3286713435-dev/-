package com.zhuoyu.delivery.masterdata.deliverable.domain;

public record DeliverableType(
    Long id,
    Long projectId,
    Long deliverableDefinitionId,
    String code,
    String name,
    String fileKind,
    String bindingStrategy,
    Integer sortOrder,
    String status
) {
}

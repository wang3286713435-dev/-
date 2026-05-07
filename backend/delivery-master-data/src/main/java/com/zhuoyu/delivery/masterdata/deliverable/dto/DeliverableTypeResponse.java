package com.zhuoyu.delivery.masterdata.deliverable.dto;

public record DeliverableTypeResponse(
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

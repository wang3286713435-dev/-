package com.zhuoyu.delivery.masterdata.deliverable.dto;

public record UpdateDeliverableTypeRequest(
    String code,
    String name,
    String fileKind,
    String bindingStrategy,
    Integer sortOrder,
    String status
) {
}

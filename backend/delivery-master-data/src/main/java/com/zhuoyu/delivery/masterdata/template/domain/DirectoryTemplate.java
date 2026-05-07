package com.zhuoyu.delivery.masterdata.template.domain;

public record DirectoryTemplate(
    Long id,
    Long projectId,
    String templateType,
    String name,
    String rootNodeJson,
    String sourceType,
    Integer sortOrder,
    String status
) {
}

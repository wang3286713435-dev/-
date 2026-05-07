package com.zhuoyu.delivery.masterdata.template.dto;

public record DirectoryTemplateResponse(
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

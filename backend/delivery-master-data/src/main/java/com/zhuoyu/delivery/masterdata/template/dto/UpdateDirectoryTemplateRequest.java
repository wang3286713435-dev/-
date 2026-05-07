package com.zhuoyu.delivery.masterdata.template.dto;

public record UpdateDirectoryTemplateRequest(
    String templateType,
    String name,
    String rootNodeJson,
    String sourceType,
    Integer sortOrder,
    String status
) {
}

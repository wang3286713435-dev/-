package com.zhuoyu.delivery.masterdata.section.domain;

public record SectionNode(
    Long id,
    Long projectId,
    Long parentId,
    String code,
    String name,
    Integer level,
    String path,
    Integer sortOrder,
    String status
) {
}

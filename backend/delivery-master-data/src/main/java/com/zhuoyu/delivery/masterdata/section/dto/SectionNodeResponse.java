package com.zhuoyu.delivery.masterdata.section.dto;

import java.util.List;

public record SectionNodeResponse(
    Long id,
    Long projectId,
    Long parentId,
    String code,
    String name,
    Integer level,
    String path,
    Integer sortOrder,
    String status,
    List<SectionNodeResponse> children
) {
}

package com.zhuoyu.delivery.masterdata.status.dto;

public record StandardStatusResponse(
    Long projectId,
    Boolean hasSectionTree,
    Boolean hasNodeTypes,
    Boolean nodeTypesLocked,
    Integer sectionNodeCount,
    Integer nodeTypeCount,
    Boolean hasDeliverableDefinitions,
    Boolean hasDeliverableTypes,
    Boolean hasDeliverableAttributes,
    Boolean hasDirectoryTemplates,
    Boolean deliverableStandardReady,
    Integer deliverableDefinitionCount,
    Integer deliverableTypeCount,
    Integer deliverableAttributeCount,
    Integer directoryTemplateCount
) {
}

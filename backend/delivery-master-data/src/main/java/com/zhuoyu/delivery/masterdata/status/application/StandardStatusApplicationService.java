package com.zhuoyu.delivery.masterdata.status.application;

import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableAttributeApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableDefinitionApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.application.DeliverableTypeApplicationService;
import com.zhuoyu.delivery.masterdata.nodetype.application.NodeTypeApplicationService;
import com.zhuoyu.delivery.masterdata.section.application.SectionNodeApplicationService;
import com.zhuoyu.delivery.masterdata.status.dto.StandardStatusResponse;
import com.zhuoyu.delivery.masterdata.template.application.DirectoryTemplateApplicationService;
import org.springframework.stereotype.Service;

@Service
public class StandardStatusApplicationService {

    private final SectionNodeApplicationService sectionNodeApplicationService;
    private final NodeTypeApplicationService nodeTypeApplicationService;
    private final DeliverableDefinitionApplicationService definitionApplicationService;
    private final DeliverableTypeApplicationService typeApplicationService;
    private final DeliverableAttributeApplicationService attributeApplicationService;
    private final DirectoryTemplateApplicationService templateApplicationService;

    public StandardStatusApplicationService(
        SectionNodeApplicationService sectionNodeApplicationService,
        NodeTypeApplicationService nodeTypeApplicationService,
        DeliverableDefinitionApplicationService definitionApplicationService,
        DeliverableTypeApplicationService typeApplicationService,
        DeliverableAttributeApplicationService attributeApplicationService,
        DirectoryTemplateApplicationService templateApplicationService
    ) {
        this.sectionNodeApplicationService = sectionNodeApplicationService;
        this.nodeTypeApplicationService = nodeTypeApplicationService;
        this.definitionApplicationService = definitionApplicationService;
        this.typeApplicationService = typeApplicationService;
        this.attributeApplicationService = attributeApplicationService;
        this.templateApplicationService = templateApplicationService;
    }

    public StandardStatusResponse getStatus(Long projectId) {
        int sectionNodeCount = sectionNodeApplicationService.countByProject(projectId);
        int nodeTypeCount = nodeTypeApplicationService.countByProject(projectId);
        boolean hasSectionTree = sectionNodeCount > 0;
        boolean hasNodeTypes = nodeTypeCount > 0;
        boolean nodeTypesLocked = hasNodeTypes && nodeTypeApplicationService.allNodeTypesLocked(projectId);

        int deliverableDefinitionCount = definitionApplicationService.countByProject(projectId);
        int deliverableTypeCount = typeApplicationService.countByProject(projectId);
        int deliverableAttributeCount = attributeApplicationService.countByProject(projectId);
        int directoryTemplateCount = templateApplicationService.countByProject(projectId);

        boolean hasDeliverableDefinitions = deliverableDefinitionCount > 0;
        boolean hasDeliverableTypes = deliverableTypeCount > 0;
        boolean hasDeliverableAttributes = deliverableAttributeCount > 0;
        boolean hasDirectoryTemplates = directoryTemplateCount > 0;

        boolean deliverableStandardReady = nodeTypesLocked
            && hasDeliverableDefinitions
            && hasDeliverableTypes
            && hasDeliverableAttributes
            && hasDirectoryTemplates;

        return new StandardStatusResponse(
            projectId,
            hasSectionTree,
            hasNodeTypes,
            nodeTypesLocked,
            sectionNodeCount,
            nodeTypeCount,
            hasDeliverableDefinitions,
            hasDeliverableTypes,
            hasDeliverableAttributes,
            hasDirectoryTemplates,
            deliverableStandardReady,
            deliverableDefinitionCount,
            deliverableTypeCount,
            deliverableAttributeCount,
            directoryTemplateCount
        );
    }
}

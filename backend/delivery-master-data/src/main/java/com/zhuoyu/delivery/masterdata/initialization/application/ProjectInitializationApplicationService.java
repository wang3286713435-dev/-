package com.zhuoyu.delivery.masterdata.initialization.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableAttribute;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableDefinition;
import com.zhuoyu.delivery.masterdata.deliverable.domain.DeliverableType;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableAttributeRepository;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableDefinitionRepository;
import com.zhuoyu.delivery.masterdata.deliverable.repository.DeliverableTypeRepository;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingApplyRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingApplyResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingAssetSummary;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingAssessmentResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDraftItem;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDraftPreviewResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingEvidenceClue;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingGap;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.InitializationStatusResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.StandardTemplateDetailResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.StandardTemplateSummaryResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateApplyRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateApplyResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateCounts;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplateItemResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplatePreviewItemResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplatePreviewRequest;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.TemplatePreviewResponse;
import com.zhuoyu.delivery.masterdata.nodetype.domain.NodeType;
import com.zhuoyu.delivery.masterdata.nodetype.repository.NodeTypeRepository;
import com.zhuoyu.delivery.masterdata.section.domain.SectionNode;
import com.zhuoyu.delivery.masterdata.section.repository.SectionNodeRepository;
import com.zhuoyu.delivery.masterdata.status.application.StandardStatusApplicationService;
import com.zhuoyu.delivery.masterdata.status.dto.StandardStatusResponse;
import com.zhuoyu.delivery.masterdata.template.domain.DirectoryTemplate;
import com.zhuoyu.delivery.masterdata.template.repository.DirectoryTemplateRepository;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectInitializationApplicationService {

    public static final String MEP_BIM_BASIC = "MEP_BIM_BASIC";

    private static final StandardTemplateSpec BUILTIN_TEMPLATE = new StandardTemplateSpec(
        MEP_BIM_BASIC,
        "建筑机电 BIM 交付基础模板",
        "建筑机电/BIM交付",
        "内置基础模板，用于快速生成项目部位、节点类型、交付物、属性和交付目录。",
        List.of(
            new SectionSpec("PROJECT", "项目", null, 1, 10),
            new SectionSpec("BUILDING", "单体", "PROJECT", 2, 20),
            new SectionSpec("FLOOR", "楼层", "BUILDING", 3, 30),
            new SectionSpec("SYSTEM", "系统", "FLOOR", 4, 40)
        ),
        List.of(
            new NodeTypeSpec("PROJECT", "项目", 1, 10),
            new NodeTypeSpec("BUILDING", "单体", 2, 20),
            new NodeTypeSpec("FLOOR", "楼层", 3, 30),
            new NodeTypeSpec("SYSTEM", "系统", 4, 40),
            new NodeTypeSpec("EQUIPMENT", "设备", 5, 50)
        ),
        List.of(
            new DefinitionSpec("MODEL_DELIVERY", "模型交付", "MODEL", "SYSTEM", true, 10),
            new DefinitionSpec("DRAWING_DELIVERY", "图纸交付", "DRAWING", "SYSTEM", true, 20),
            new DefinitionSpec("DOCUMENT_DELIVERY", "文档交付", "DOCUMENT", "PROJECT", true, 30)
        ),
        List.of(
            new TypeSpec("RVT_MODEL", "RVT 模型", "MODEL_DELIVERY", "MODEL", "SECTION_NODE", 10),
            new TypeSpec("IFC_MODEL", "IFC 模型", "MODEL_DELIVERY", "MODEL", "SECTION_NODE", 20),
            new TypeSpec("NWD_MODEL", "NWD/NWC 模型", "MODEL_DELIVERY", "MODEL", "SECTION_NODE", 30),
            new TypeSpec("DWG_DRAWING", "DWG 图纸", "DRAWING_DELIVERY", "DRAWING", "SECTION_NODE", 40),
            new TypeSpec("PDF_DRAWING", "PDF 图纸", "DRAWING_DELIVERY", "DRAWING", "SECTION_NODE", 50),
            new TypeSpec("PDF_DOCUMENT", "PDF 文档", "DOCUMENT_DELIVERY", "DOCUMENT", "PROJECT", 60),
            new TypeSpec("OFFICE_DOCUMENT", "Office 文档", "DOCUMENT_DELIVERY", "DOCUMENT", "PROJECT", 70)
        ),
        List.of(
            new AttributeSpec("DISCIPLINE", "专业", "RVT_MODEL", "ENUM", null, true, "HVAC", "[\"HVAC\",\"ELECTRICAL\",\"PLUMBING\",\"FIRE\",\"WEAK_CURRENT\",\"OTHER\"]", 10),
            new AttributeSpec("VERSION", "版本", "RVT_MODEL", "TEXT", null, true, "V1", null, 20),
            new AttributeSpec("PHASE", "阶段", "RVT_MODEL", "TEXT", null, false, "施工图", null, 30),
            new AttributeSpec("OWNER", "责任人", "RVT_MODEL", "TEXT", null, false, "项目负责人", null, 40),
            new AttributeSpec("REVIEW_STATUS", "审核状态", "RVT_MODEL", "ENUM", null, false, "待审核", "[\"待审核\",\"已通过\",\"需整改\"]", 50)
        ),
        List.of(
            new DirectoryTemplateSpec("MODEL", "建筑机电模型目录", "{\"name\":\"模型\",\"children\":[{\"name\":\"RVT\"},{\"name\":\"IFC\"},{\"name\":\"NWD-NWC\"}]}", 10),
            new DirectoryTemplateSpec("DRAWING", "建筑机电图纸目录", "{\"name\":\"图纸\",\"children\":[{\"name\":\"DWG\"},{\"name\":\"PDF\"}]}", 20),
            new DirectoryTemplateSpec("DOCUMENT", "建筑机电文档目录", "{\"name\":\"文档\",\"children\":[{\"name\":\"交付说明\"},{\"name\":\"审核记录\"}]}", 30)
        )
    );

    private final SectionNodeRepository sectionNodeRepository;
    private final NodeTypeRepository nodeTypeRepository;
    private final DeliverableDefinitionRepository definitionRepository;
    private final DeliverableTypeRepository typeRepository;
    private final DeliverableAttributeRepository attributeRepository;
    private final DirectoryTemplateRepository directoryTemplateRepository;
    private final StandardStatusApplicationService standardStatusApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProjectInitializationApplicationService(
        SectionNodeRepository sectionNodeRepository,
        NodeTypeRepository nodeTypeRepository,
        DeliverableDefinitionRepository definitionRepository,
        DeliverableTypeRepository typeRepository,
        DeliverableAttributeRepository attributeRepository,
        DirectoryTemplateRepository directoryTemplateRepository,
        StandardStatusApplicationService standardStatusApplicationService,
        AuditLogApplicationService auditLogApplicationService,
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.sectionNodeRepository = sectionNodeRepository;
        this.nodeTypeRepository = nodeTypeRepository;
        this.definitionRepository = definitionRepository;
        this.typeRepository = typeRepository;
        this.attributeRepository = attributeRepository;
        this.directoryTemplateRepository = directoryTemplateRepository;
        this.standardStatusApplicationService = standardStatusApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public InitializationStatusResponse status(Long projectId) {
        StandardStatusResponse status = standardStatusApplicationService.getStatus(projectId);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String currentStep = "READY";

        if (!Boolean.TRUE.equals(status.hasSectionTree())) {
            blockers.add("尚未建立工程部位树");
            currentStep = "SECTION_TREE";
        }
        if (!Boolean.TRUE.equals(status.hasNodeTypes())) {
            blockers.add("尚未配置节点类型");
            if ("READY".equals(currentStep)) {
                currentStep = "NODE_TYPES";
            }
        }
        if (Boolean.TRUE.equals(status.hasNodeTypes()) && !Boolean.TRUE.equals(status.nodeTypesLocked())) {
            blockers.add("节点类型尚未锁定");
            if ("READY".equals(currentStep)) {
                currentStep = "NODE_TYPE_LOCK";
            }
        }
        if (!Boolean.TRUE.equals(status.hasDeliverableDefinitions())
            || !Boolean.TRUE.equals(status.hasDeliverableTypes())
            || !Boolean.TRUE.equals(status.hasDeliverableAttributes())
            || !Boolean.TRUE.equals(status.hasDirectoryTemplates())) {
            blockers.add("交付物标准尚未完整配置");
            if ("READY".equals(currentStep)) {
                currentStep = "DELIVERABLE_STANDARD";
            }
        }
        if (Boolean.TRUE.equals(status.deliverableStandardReady())) {
            currentStep = "READY";
            warnings.add("标准底座已可用，重复应用模板只会补齐缺失项，不会覆盖现有数据");
        }

        return new InitializationStatusResponse(
            projectId,
            status,
            currentStep,
            Boolean.TRUE.equals(status.deliverableStandardReady()),
            blockers,
            warnings,
            nextActions(status)
        );
    }

    public List<StandardTemplateSummaryResponse> listTemplates() {
        return List.of(toSummary(BUILTIN_TEMPLATE));
    }

    public StandardTemplateDetailResponse templateDetail(String templateCode) {
        StandardTemplateSpec template = requireTemplate(templateCode);
        return new StandardTemplateDetailResponse(
            template.code(),
            template.name(),
            template.industry(),
            template.description(),
            template.counts(),
            template.items()
        );
    }

    public TemplatePreviewResponse preview(Long projectId, TemplatePreviewRequest request) {
        StandardTemplateSpec template = requireTemplate(request.templateCode());
        ExistingState existing = loadExisting(projectId);
        PreviewAccumulator preview = buildPreview(template, existing);
        return preview.toResponse(template);
    }

    public OnboardingAssessmentResponse onboardingAssessment(Long projectId) {
        StandardStatusResponse status = standardStatusApplicationService.getStatus(projectId);
        AssetOnboardingSnapshot snapshot = loadAssetOnboardingSnapshot(projectId);
        return new OnboardingAssessmentResponse(
            projectId,
            true,
            "catalog_only",
            onboardingStatus(snapshot, status),
            snapshot.toSummary(),
            status,
            onboardingEvidenceClues(snapshot),
            onboardingGaps(snapshot, status),
            onboardingNextActions(snapshot, status)
        );
    }

    public OnboardingDraftPreviewResponse onboardingPreview(Long projectId, String templateCode) {
        StandardTemplateSpec template = requireTemplate(defaultTemplateCode(templateCode));
        AssetOnboardingSnapshot snapshot = loadAssetOnboardingSnapshot(projectId);
        TemplatePreviewResponse templatePreview = preview(projectId, new TemplatePreviewRequest(template.code()));
        return new OnboardingDraftPreviewResponse(
            projectId,
            true,
            true,
            false,
            false,
            "catalog_only",
            template.code(),
            template.name(),
            snapshot.toSummary(),
            templatePreview,
            onboardingDraftItems(snapshot, templatePreview),
            List.of(
                "本预览只读取项目目录元数据和标准配置状态，不读取 PDF/Office/DWG/RVT/IFC 正文。",
                "草案不会触碰、复制、移动、改名或删除 NAS 文件。",
                "真实工程主数据仍需人工确认；目录证据不能替代模型解析或正文证据。"
            )
        );
    }

    @Transactional
    public TemplateApplyResponse apply(Long operatorId, Long projectId, TemplateApplyRequest request) {
        if (!Boolean.TRUE.equals(request.confirmApply())) {
            throw new BusinessException("MASTERDATA_INITIALIZATION_CONFIRM_REQUIRED", "应用模板前需要确认", HttpStatus.BAD_REQUEST);
        }
        StandardTemplateSpec template = requireTemplate(request.templateCode());
        TemplatePreviewResponse preview = preview(projectId, new TemplatePreviewRequest(template.code()));
        if (Boolean.TRUE.equals(preview.blocked())) {
            throw new BusinessException("MASTERDATA_INITIALIZATION_TEMPLATE_BLOCKED", "模板存在冲突或前置阻塞，无法应用", HttpStatus.CONFLICT);
        }

        ExistingState existing = loadExisting(projectId);
        CountAccumulator created = new CountAccumulator();
        CountAccumulator skipped = new CountAccumulator();

        applySectionNodes(projectId, operatorId, template, existing, created, skipped);
        applyNodeTypes(projectId, operatorId, template, existing, created, skipped);
        applyDeliverableDefinitions(projectId, operatorId, template, existing, created, skipped);
        applyDeliverableTypes(projectId, operatorId, template, existing, created, skipped);
        applyDeliverableAttributes(projectId, operatorId, template, existing, created, skipped);
        applyDirectoryTemplates(projectId, operatorId, template, existing, created, skipped);

        StandardStatusResponse afterStatus = standardStatusApplicationService.getStatus(projectId);
        auditLogApplicationService.record(
            projectId,
            "master-data",
            "masterdata.initialization.template-apply",
            "STANDARD_TEMPLATE",
            template.code(),
            operatorId,
            Map.of(
                "templateCode", template.code(),
                "templateName", template.name(),
                "created", created.toCounts(),
                "skipped", skipped.toCounts()
            )
        );

        return new TemplateApplyResponse(
            template.code(),
            template.name(),
            created.toCounts(),
            skipped.toCounts(),
            preview.conflicts().size(),
            afterStatus,
            nextActions(afterStatus)
        );
    }

    @Transactional
    public OnboardingApplyResponse applyOnboarding(Long operatorId, Long projectId, OnboardingApplyRequest request) {
        if (request == null || !Boolean.TRUE.equals(request.confirmed())) {
            throw new BusinessException("REAL_PROJECT_ONBOARDING_CONFIRM_REQUIRED", "应用真实项目接入草案前需要人工确认", HttpStatus.BAD_REQUEST);
        }
        String templateCode = defaultTemplateCode(request.templateCode());
        TemplateApplyResponse templateResult = apply(operatorId, projectId, new TemplateApplyRequest(templateCode, true));
        return new OnboardingApplyResponse(
            projectId,
            true,
            false,
            false,
            true,
            "catalog_only",
            templateResult,
            templateResult.nextActions()
        );
    }

    private void applySectionNodes(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        for (SectionSpec spec : template.sectionNodes()) {
            if (existing.sectionByCode().containsKey(spec.code())) {
                skipped.sectionNodes++;
                continue;
            }
            SectionNode parent = spec.parentCode() == null ? null : existing.sectionByCode().get(spec.parentCode());
            Long nodeId = sectionNodeRepository.insert(
                projectId,
                parent == null ? null : parent.id(),
                spec.code(),
                spec.name(),
                parent == null ? spec.level() : parent.level() + 1,
                "",
                spec.sortOrder(),
                "ACTIVE",
                operatorId
            );
            String path = parent == null ? String.valueOf(nodeId) : parent.path() + "/" + nodeId;
            sectionNodeRepository.updatePath(projectId, nodeId, path, operatorId);
            existing.sectionByCode().put(
                spec.code(),
                new SectionNode(nodeId, projectId, parent == null ? null : parent.id(), spec.code(), spec.name(), parent == null ? spec.level() : parent.level() + 1, path, spec.sortOrder(), "ACTIVE")
            );
            created.sectionNodes++;
        }
    }

    private void applyNodeTypes(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        boolean inserted = false;
        for (NodeTypeSpec spec : template.nodeTypes()) {
            if (existing.nodeTypeByCode().containsKey(spec.code())) {
                skipped.nodeTypes++;
                continue;
            }
            Long id = nodeTypeRepository.insert(projectId, spec.code(), spec.name(), spec.scopeLevel(), spec.sortOrder(), "ACTIVE", operatorId);
            existing.nodeTypeByCode().put(spec.code(), new NodeType(id, projectId, spec.code(), spec.name(), spec.scopeLevel(), spec.sortOrder(), "ACTIVE", false, null, null));
            created.nodeTypes++;
            inserted = true;
        }
        if (inserted || !existing.nodeTypeByCode().isEmpty()) {
            nodeTypeRepository.lockAll(projectId, operatorId);
            existing.nodeTypeByCode().clear();
            existing.nodeTypeByCode().putAll(mapByCode(nodeTypeRepository.findByProject(projectId), NodeType::code));
        }
    }

    private void applyDeliverableDefinitions(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        for (DefinitionSpec spec : template.definitions()) {
            if (existing.definitionByCode().containsKey(spec.code())) {
                skipped.deliverableDefinitions++;
                continue;
            }
            NodeType nodeType = existing.nodeTypeByCode().get(spec.nodeTypeCode());
            if (nodeType == null) {
                throw new BusinessException("MASTERDATA_INITIALIZATION_NODE_TYPE_MISSING", "节点类型缺失，无法创建交付物定义", HttpStatus.CONFLICT);
            }
            Long id = definitionRepository.insert(projectId, nodeType.id(), spec.code(), spec.name(), spec.category(), spec.required(), spec.sortOrder(), "ACTIVE", operatorId);
            existing.definitionByCode().put(spec.code(), new DeliverableDefinition(id, projectId, nodeType.id(), spec.code(), spec.name(), spec.category(), spec.required(), spec.sortOrder(), "ACTIVE"));
            created.deliverableDefinitions++;
        }
    }

    private void applyDeliverableTypes(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        for (TypeSpec spec : template.types()) {
            if (existing.typeByCode().containsKey(spec.code())) {
                skipped.deliverableTypes++;
                continue;
            }
            DeliverableDefinition definition = existing.definitionByCode().get(spec.definitionCode());
            if (definition == null) {
                throw new BusinessException("MASTERDATA_INITIALIZATION_DEFINITION_MISSING", "交付物定义缺失，无法创建交付物类型", HttpStatus.CONFLICT);
            }
            Long id = typeRepository.insert(projectId, definition.id(), spec.code(), spec.name(), spec.fileKind(), spec.bindingStrategy(), spec.sortOrder(), "ACTIVE", operatorId);
            existing.typeByCode().put(spec.code(), new DeliverableType(id, projectId, definition.id(), spec.code(), spec.name(), spec.fileKind(), spec.bindingStrategy(), spec.sortOrder(), "ACTIVE"));
            created.deliverableTypes++;
        }
    }

    private void applyDeliverableAttributes(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        for (AttributeSpec spec : template.attributes()) {
            if (existing.attributeByCode().containsKey(spec.code())) {
                skipped.deliverableAttributes++;
                continue;
            }
            DeliverableType type = existing.typeByCode().get(spec.typeCode());
            if (type == null) {
                throw new BusinessException("MASTERDATA_INITIALIZATION_TYPE_MISSING", "交付物类型缺失，无法创建交付物属性", HttpStatus.CONFLICT);
            }
            Long id = attributeRepository.insert(
                projectId,
                type.id(),
                spec.code(),
                spec.name(),
                spec.valueType(),
                spec.unit(),
                spec.required(),
                spec.exampleValue(),
                spec.enumOptions(),
                spec.sortOrder(),
                "ACTIVE",
                operatorId
            );
            existing.attributeByCode().put(
                spec.code(),
                new DeliverableAttribute(id, projectId, type.id(), spec.code(), spec.name(), spec.valueType(), spec.unit(), spec.required(), spec.exampleValue(), spec.enumOptions(), spec.sortOrder(), "ACTIVE")
            );
            created.deliverableAttributes++;
        }
    }

    private void applyDirectoryTemplates(
        Long projectId,
        Long operatorId,
        StandardTemplateSpec template,
        ExistingState existing,
        CountAccumulator created,
        CountAccumulator skipped
    ) {
        for (DirectoryTemplateSpec spec : template.directoryTemplates()) {
            if (existing.directoryTemplateByName().containsKey(spec.name())) {
                skipped.directoryTemplates++;
                continue;
            }
            Long id = directoryTemplateRepository.insert(projectId, spec.templateType(), spec.name(), spec.rootNodeJson(), "BUILTIN_TEMPLATE", spec.sortOrder(), "ACTIVE", operatorId);
            existing.directoryTemplateByName().put(spec.name(), new DirectoryTemplate(id, projectId, spec.templateType(), spec.name(), spec.rootNodeJson(), "BUILTIN_TEMPLATE", spec.sortOrder(), "ACTIVE"));
            created.directoryTemplates++;
        }
    }

    private PreviewAccumulator buildPreview(StandardTemplateSpec template, ExistingState existing) {
        PreviewAccumulator preview = new PreviewAccumulator();
        boolean existingLocked = existing.nodeTypeByCode().values().stream().anyMatch(nodeType -> Boolean.TRUE.equals(nodeType.locked()));
        boolean hasMissingNodeTypes = template.nodeTypes().stream().anyMatch(spec -> !existing.nodeTypeByCode().containsKey(spec.code()));
        if (existingLocked && hasMissingNodeTypes) {
            preview.blockReasons.add("当前项目节点类型已锁定，不能再自动补充新的节点类型");
        }

        for (SectionSpec spec : template.sectionNodes()) {
            SectionNode existingNode = existing.sectionByCode().get(spec.code());
            if (existingNode == null) {
                preview.create.sectionNodes++;
                preview.addItem("SECTION_NODE", spec.code(), spec.name(), "CREATE", "将创建部位节点");
            } else if (!sameText(existingNode.name(), spec.name())) {
                preview.conflicts.add("部位节点编码 " + spec.code() + " 已存在，但名称不同");
                preview.addItem("SECTION_NODE", spec.code(), spec.name(), "CONFLICT", "编码已存在但名称不同");
            } else {
                preview.skip.sectionNodes++;
                preview.addItem("SECTION_NODE", spec.code(), spec.name(), "SKIP", "已存在，跳过");
            }
        }

        for (NodeTypeSpec spec : template.nodeTypes()) {
            NodeType existingNodeType = existing.nodeTypeByCode().get(spec.code());
            if (existingNodeType == null) {
                preview.create.nodeTypes++;
                preview.addItem("NODE_TYPE", spec.code(), spec.name(), "CREATE", "将创建并随模板锁定");
            } else if (!sameText(existingNodeType.name(), spec.name())) {
                preview.conflicts.add("节点类型编码 " + spec.code() + " 已存在，但名称不同");
                preview.addItem("NODE_TYPE", spec.code(), spec.name(), "CONFLICT", "编码已存在但名称不同");
            } else {
                preview.skip.nodeTypes++;
                preview.addItem("NODE_TYPE", spec.code(), spec.name(), "SKIP", "已存在，跳过");
            }
        }

        for (DefinitionSpec spec : template.definitions()) {
            DeliverableDefinition existingDefinition = existing.definitionByCode().get(spec.code());
            if (existingDefinition == null) {
                preview.create.deliverableDefinitions++;
                preview.addItem("DELIVERABLE_DEFINITION", spec.code(), spec.name(), "CREATE", "将创建交付物定义");
            } else if (!sameText(existingDefinition.name(), spec.name())) {
                preview.conflicts.add("交付物定义编码 " + spec.code() + " 已存在，但名称不同");
                preview.addItem("DELIVERABLE_DEFINITION", spec.code(), spec.name(), "CONFLICT", "编码已存在但名称不同");
            } else {
                preview.skip.deliverableDefinitions++;
                preview.addItem("DELIVERABLE_DEFINITION", spec.code(), spec.name(), "SKIP", "已存在，跳过");
            }
        }

        for (TypeSpec spec : template.types()) {
            DeliverableType existingType = existing.typeByCode().get(spec.code());
            if (existingType == null) {
                preview.create.deliverableTypes++;
                preview.addItem("DELIVERABLE_TYPE", spec.code(), spec.name(), "CREATE", "将创建交付物类型");
            } else if (!sameText(existingType.name(), spec.name())) {
                preview.conflicts.add("交付物类型编码 " + spec.code() + " 已存在，但名称不同");
                preview.addItem("DELIVERABLE_TYPE", spec.code(), spec.name(), "CONFLICT", "编码已存在但名称不同");
            } else {
                preview.skip.deliverableTypes++;
                preview.addItem("DELIVERABLE_TYPE", spec.code(), spec.name(), "SKIP", "已存在，跳过");
            }
        }

        for (AttributeSpec spec : template.attributes()) {
            DeliverableAttribute existingAttribute = existing.attributeByCode().get(spec.code());
            if (existingAttribute == null) {
                preview.create.deliverableAttributes++;
                preview.addItem("DELIVERABLE_ATTRIBUTE", spec.code(), spec.name(), "CREATE", "将创建交付物属性");
            } else if (!sameText(existingAttribute.name(), spec.name())) {
                preview.conflicts.add("交付物属性编码 " + spec.code() + " 已存在，但名称不同");
                preview.addItem("DELIVERABLE_ATTRIBUTE", spec.code(), spec.name(), "CONFLICT", "编码已存在但名称不同");
            } else {
                preview.skip.deliverableAttributes++;
                preview.addItem("DELIVERABLE_ATTRIBUTE", spec.code(), spec.name(), "SKIP", "已存在，跳过");
            }
        }

        for (DirectoryTemplateSpec spec : template.directoryTemplates()) {
            DirectoryTemplate existingTemplate = existing.directoryTemplateByName().get(spec.name());
            if (existingTemplate == null) {
                preview.create.directoryTemplates++;
                preview.addItem("DIRECTORY_TEMPLATE", spec.name(), spec.name(), "CREATE", "将创建目录模板");
            } else if (!sameText(existingTemplate.templateType(), spec.templateType())) {
                preview.conflicts.add("目录模板 " + spec.name() + " 已存在，但类型不同");
                preview.addItem("DIRECTORY_TEMPLATE", spec.name(), spec.name(), "CONFLICT", "名称已存在但类型不同");
            } else {
                preview.skip.directoryTemplates++;
                preview.addItem("DIRECTORY_TEMPLATE", spec.name(), spec.name(), "SKIP", "已存在，跳过");
            }
        }
        return preview;
    }

    private AssetOnboardingSnapshot loadAssetOnboardingSnapshot(Long projectId) {
        return jdbcTemplate.queryForObject("""
            SELECT COALESCE(file_stats.file_count, 0) AS file_count,
                   COALESCE(file_stats.model_file_count, 0) AS model_file_count,
                   COALESCE(file_stats.drawing_file_count, 0) AS drawing_file_count,
                   COALESCE(file_stats.document_file_count, 0) AS document_file_count,
                   COALESCE(path_stats.path_mapping_count, 0) AS path_mapping_count,
                   file_stats.dominant_file_kinds,
                   file_stats.last_asset_seen_at,
                   scan_stats.last_scan_at
            FROM (SELECT :projectId AS project_id) base
            LEFT JOIN (
                SELECT project_id,
                       COUNT(1) AS file_count,
                       SUM(CASE WHEN file_kind = 'MODEL' THEN 1 ELSE 0 END) AS model_file_count,
                       SUM(CASE WHEN file_kind = 'DRAWING' THEN 1 ELSE 0 END) AS drawing_file_count,
                       SUM(CASE WHEN file_kind = 'DOCUMENT' THEN 1 ELSE 0 END) AS document_file_count,
                       GROUP_CONCAT(DISTINCT file_kind ORDER BY file_kind SEPARATOR ',') AS dominant_file_kinds,
                       MAX(COALESCE(last_verified_at, updated_at, created_at)) AS last_asset_seen_at
                FROM data_file_resources
                WHERE project_id = :projectId AND deleted = 0
                GROUP BY project_id
            ) file_stats ON file_stats.project_id = base.project_id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS path_mapping_count
                FROM data_asset_project_path_mappings
                WHERE project_id = :projectId AND deleted = 0 AND enabled = 1
                GROUP BY project_id
            ) path_stats ON path_stats.project_id = base.project_id
            LEFT JOIN (
                SELECT project_id, MAX(COALESCE(completed_at, started_at, updated_at)) AS last_scan_at
                FROM data_asset_scan_tasks
                WHERE project_id = :projectId AND deleted = 0
                GROUP BY project_id
            ) scan_stats ON scan_stats.project_id = base.project_id
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new AssetOnboardingSnapshot(
            rs.getInt("file_count"),
            rs.getInt("model_file_count"),
            rs.getInt("drawing_file_count"),
            rs.getInt("document_file_count"),
            rs.getInt("path_mapping_count"),
            splitCsv(rs.getString("dominant_file_kinds")),
            toInstant(rs.getTimestamp("last_asset_seen_at")),
            toInstant(rs.getTimestamp("last_scan_at"))
        ));
    }

    private List<OnboardingEvidenceClue> onboardingEvidenceClues(AssetOnboardingSnapshot snapshot) {
        List<OnboardingEvidenceClue> clues = new ArrayList<>();
        if (snapshot.pathMappingCount() > 0) {
            clues.add(new OnboardingEvidenceClue(
                "PATH_MAPPING",
                "已存在受控项目路径映射",
                "catalog_only",
                true,
                "平台只记录路径映射是否存在，前端和 Hermes 不返回 NAS 原始路径。"
            ));
        }
        if (snapshot.fileCount() > 0) {
            clues.add(new OnboardingEvidenceClue(
                "FILE_METADATA",
                "已登记文件目录元数据",
                "catalog_only",
                true,
                "可用于判断文件类型、数量、版本和目录级状态，不能证明文件正文或模型内部内容。"
            ));
        }
        if (snapshot.modelFileCount() > 0) {
            clues.add(new OnboardingEvidenceClue(
                "MODEL_FILE_METADATA",
                "已登记模型文件线索",
                "catalog_only",
                true,
                "只能说明目录中存在模型资产，不能判断 RVT/DWG/IFC 内部构件、图层或参数。"
            ));
        }
        if (snapshot.drawingFileCount() > 0) {
            clues.add(new OnboardingEvidenceClue(
                "DRAWING_FILE_METADATA",
                "已登记图纸文件线索",
                "catalog_only",
                true,
                "只能说明目录中存在图纸资产，不能判断图纸标题栏、图层或图纸正文内容。"
            ));
        }
        if (clues.isEmpty()) {
            clues.add(new OnboardingEvidenceClue(
                "NO_CATALOG_ASSET",
                "尚未发现可用目录资产",
                "catalog_only",
                true,
                "当前只能基于项目与标准配置做接入评估，缺少文件目录线索。"
            ));
        }
        return clues;
    }

    private List<OnboardingGap> onboardingGaps(AssetOnboardingSnapshot snapshot, StandardStatusResponse status) {
        List<OnboardingGap> gaps = new ArrayList<>();
        if (snapshot.fileCount() <= 0 && snapshot.pathMappingCount() <= 0) {
            gaps.add(new OnboardingGap(
                "ASSET_CATALOG_MISSING",
                "warning",
                "项目尚未形成可用于接入评估的文件目录线索。",
                "asset_catalog_only"
            ));
        }
        if (!Boolean.TRUE.equals(status.hasSectionTree()) || !Boolean.TRUE.equals(status.hasNodeTypes())) {
            gaps.add(new OnboardingGap(
                "MASTER_DATA_MISSING",
                "warning",
                "工程部位树或节点类型尚未完整建立。",
                "master_data_missing"
            ));
        }
        if (!Boolean.TRUE.equals(status.deliverableStandardReady())) {
            gaps.add(new OnboardingGap(
                "DELIVERY_STANDARD_MISSING",
                "warning",
                "交付定义、交付类型、交付属性或目录模板尚未就绪。",
                "delivery_standard_missing"
            ));
        }
        if (snapshot.modelFileCount() > 0 || snapshot.drawingFileCount() > 0) {
            gaps.add(new OnboardingGap(
                "MODEL_OR_DRAWING_PARSE_EVIDENCE_MISSING",
                "info",
                "当前没有启用 RVT/DWG/图纸解析证据，不能确认模型构件、图层、参数或图纸正文。",
                "rvt_parse_evidence_missing,dwg_parse_evidence_missing,component_evidence_missing,model_parse_evidence_missing"
            ));
        }
        return gaps;
    }

    private List<OnboardingDraftItem> onboardingDraftItems(AssetOnboardingSnapshot snapshot, TemplatePreviewResponse preview) {
        return preview.items().stream()
            .map(item -> new OnboardingDraftItem(
                item.category(),
                item.name(),
                item.reason(),
                assetClueSupports(item.category(), snapshot),
                true,
                true
            ))
            .toList();
    }

    private List<String> onboardingNextActions(AssetOnboardingSnapshot snapshot, StandardStatusResponse status) {
        List<String> actions = new ArrayList<>();
        if (snapshot.pathMappingCount() <= 0 && snapshot.fileCount() <= 0) {
            actions.add("先完成真实项目路径映射或只读资产目录登记");
        }
        if (!Boolean.TRUE.equals(status.hasSectionTree()) || !Boolean.TRUE.equals(status.hasNodeTypes())) {
            actions.add("生成真实项目接入草案并人工确认工程部位/节点类型");
        }
        if (!Boolean.TRUE.equals(status.deliverableStandardReady())) {
            actions.add("补齐交付定义、交付类型、交付属性与目录模板");
        }
        if (actions.isEmpty()) {
            actions.add("进入 Agent 引导式交付治理，继续保持 catalog-only / read-only 边界");
        }
        return actions;
    }

    private static boolean assetClueSupports(String category, AssetOnboardingSnapshot snapshot) {
        if ("DELIVERABLE_TYPE".equals(category)) {
            return snapshot.fileCount() > 0;
        }
        if ("DIRECTORY_TEMPLATE".equals(category)) {
            return snapshot.pathMappingCount() > 0 || snapshot.fileCount() > 0;
        }
        return false;
    }

    private static String onboardingStatus(AssetOnboardingSnapshot snapshot, StandardStatusResponse status) {
        if (Boolean.TRUE.equals(status.deliverableStandardReady())) {
            return "GOVERNANCE_READY";
        }
        if (Boolean.TRUE.equals(status.hasSectionTree()) || Boolean.TRUE.equals(status.hasNodeTypes())) {
            return "MASTERDATA_INITIALIZED";
        }
        if (snapshot.fileCount() > 0) {
            return "ASSETS_REGISTERED";
        }
        if (snapshot.pathMappingCount() > 0) {
            return "PATH_MAPPED";
        }
        return "NOT_ONBOARDED";
    }

    private ExistingState loadExisting(Long projectId) {
        return new ExistingState(
            mapByCode(sectionNodeRepository.findByProject(projectId), SectionNode::code),
            mapByCode(nodeTypeRepository.findByProject(projectId), NodeType::code),
            mapByCode(definitionRepository.findByProject(projectId), DeliverableDefinition::code),
            mapByCode(typeRepository.findByProject(projectId), DeliverableType::code),
            mapByCode(attributeRepository.findByProject(projectId), DeliverableAttribute::code),
            directoryTemplateRepository.findByProject(projectId).stream()
                .collect(Collectors.toMap(DirectoryTemplate::name, Function.identity(), (left, right) -> left, LinkedHashMap::new))
        );
    }

    private StandardTemplateSpec requireTemplate(String templateCode) {
        String normalized = templateCode == null ? "" : templateCode.trim().toUpperCase(Locale.ROOT);
        if (!MEP_BIM_BASIC.equals(normalized)) {
            throw new BusinessException("MASTERDATA_INITIALIZATION_TEMPLATE_NOT_FOUND", "标准模板不存在", HttpStatus.NOT_FOUND);
        }
        return BUILTIN_TEMPLATE;
    }

    private static String defaultTemplateCode(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return MEP_BIM_BASIC;
        }
        return templateCode.trim().toUpperCase(Locale.ROOT);
    }

    private StandardTemplateSummaryResponse toSummary(StandardTemplateSpec template) {
        return new StandardTemplateSummaryResponse(template.code(), template.name(), template.industry(), template.description(), template.counts());
    }

    private List<String> nextActions(StandardStatusResponse status) {
        List<String> actions = new ArrayList<>();
        if (!Boolean.TRUE.equals(status.hasSectionTree())) {
            actions.add("建立工程部位树");
        }
        if (!Boolean.TRUE.equals(status.hasNodeTypes()) || !Boolean.TRUE.equals(status.nodeTypesLocked())) {
            actions.add("配置并锁定节点类型");
        }
        if (!Boolean.TRUE.equals(status.deliverableStandardReady())) {
            actions.add("补齐交付物标准和目录模板");
        }
        if (actions.isEmpty()) {
            actions.add("进入文件管理和交付视图");
        }
        return actions;
    }

    private static boolean sameText(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .limit(8)
            .toList();
    }

    private static <T> Map<String, T> mapByCode(List<T> rows, Function<T, String> codeExtractor) {
        return rows.stream()
            .collect(Collectors.toMap(codeExtractor, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private record ExistingState(
        Map<String, SectionNode> sectionByCode,
        Map<String, NodeType> nodeTypeByCode,
        Map<String, DeliverableDefinition> definitionByCode,
        Map<String, DeliverableType> typeByCode,
        Map<String, DeliverableAttribute> attributeByCode,
        Map<String, DirectoryTemplate> directoryTemplateByName
    ) {
    }

    private record AssetOnboardingSnapshot(
        int fileCount,
        int modelFileCount,
        int drawingFileCount,
        int documentFileCount,
        int pathMappingCount,
        List<String> dominantFileKinds,
        Instant lastAssetSeenAt,
        Instant lastScanAt
    ) {
        OnboardingAssetSummary toSummary() {
            return new OnboardingAssetSummary(
                fileCount,
                modelFileCount,
                drawingFileCount,
                documentFileCount,
                pathMappingCount,
                dominantFileKinds,
                lastAssetSeenAt,
                lastScanAt
            );
        }
    }

    private record StandardTemplateSpec(
        String code,
        String name,
        String industry,
        String description,
        List<SectionSpec> sectionNodes,
        List<NodeTypeSpec> nodeTypes,
        List<DefinitionSpec> definitions,
        List<TypeSpec> types,
        List<AttributeSpec> attributes,
        List<DirectoryTemplateSpec> directoryTemplates
    ) {
        TemplateCounts counts() {
            return new TemplateCounts(sectionNodes.size(), nodeTypes.size(), definitions.size(), types.size(), attributes.size(), directoryTemplates.size());
        }

        List<TemplateItemResponse> items() {
            List<TemplateItemResponse> rows = new ArrayList<>();
            sectionNodes.forEach(item -> rows.add(new TemplateItemResponse("SECTION_NODE", item.code(), item.name(), item.parentCode(), null, null, null)));
            nodeTypes.forEach(item -> rows.add(new TemplateItemResponse("NODE_TYPE", item.code(), item.name(), null, null, null, null)));
            definitions.forEach(item -> rows.add(new TemplateItemResponse("DELIVERABLE_DEFINITION", item.code(), item.name(), null, item.nodeTypeCode(), null, item.required())));
            types.forEach(item -> rows.add(new TemplateItemResponse("DELIVERABLE_TYPE", item.code(), item.name(), null, item.definitionCode(), item.fileKind(), null)));
            attributes.forEach(item -> rows.add(new TemplateItemResponse("DELIVERABLE_ATTRIBUTE", item.code(), item.name(), null, item.typeCode(), null, item.required())));
            directoryTemplates.forEach(item -> rows.add(new TemplateItemResponse("DIRECTORY_TEMPLATE", item.name(), item.name(), null, null, item.templateType(), null)));
            return rows;
        }
    }

    private record SectionSpec(String code, String name, String parentCode, Integer level, Integer sortOrder) {
    }

    private record NodeTypeSpec(String code, String name, Integer scopeLevel, Integer sortOrder) {
    }

    private record DefinitionSpec(String code, String name, String category, String nodeTypeCode, Boolean required, Integer sortOrder) {
    }

    private record TypeSpec(String code, String name, String definitionCode, String fileKind, String bindingStrategy, Integer sortOrder) {
    }

    private record AttributeSpec(
        String code,
        String name,
        String typeCode,
        String valueType,
        String unit,
        Boolean required,
        String exampleValue,
        String enumOptions,
        Integer sortOrder
    ) {
    }

    private record DirectoryTemplateSpec(String templateType, String name, String rootNodeJson, Integer sortOrder) {
    }

    private static final class PreviewAccumulator {
        private final CountAccumulator create = new CountAccumulator();
        private final CountAccumulator skip = new CountAccumulator();
        private final List<String> blockReasons = new ArrayList<>();
        private final List<String> conflicts = new ArrayList<>();
        private final List<TemplatePreviewItemResponse> items = new ArrayList<>();

        void addItem(String category, String code, String name, String action, String reason) {
            items.add(new TemplatePreviewItemResponse(category, code, name, action, reason));
        }

        TemplatePreviewResponse toResponse(StandardTemplateSpec template) {
            boolean blocked = !blockReasons.isEmpty() || !conflicts.isEmpty();
            return new TemplatePreviewResponse(
                template.code(),
                template.name(),
                blocked,
                List.copyOf(blockReasons),
                List.copyOf(conflicts),
                create.toCounts(),
                skip.toCounts(),
                List.copyOf(items)
            );
        }
    }

    private static final class CountAccumulator {
        private int sectionNodes;
        private int nodeTypes;
        private int deliverableDefinitions;
        private int deliverableTypes;
        private int deliverableAttributes;
        private int directoryTemplates;

        TemplateCounts toCounts() {
            return new TemplateCounts(sectionNodes, nodeTypes, deliverableDefinitions, deliverableTypes, deliverableAttributes, directoryTemplates);
        }
    }
}

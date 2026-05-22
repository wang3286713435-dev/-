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
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDistributionItem;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDraftItem;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingDraftPreviewResponse;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingEvidenceClue;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingGap;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingGovernanceRisk;
import com.zhuoyu.delivery.masterdata.initialization.dto.InitializationDtos.OnboardingMissingEvidence;
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
            snapshot.projectCode(),
            snapshot.projectName(),
            snapshot.assetSource(),
            snapshot.realNasProject(),
            true,
            "catalog_only",
            onboardingStatus(snapshot, status),
            snapshot.toSummary(),
            status,
            onboardingEvidenceClues(snapshot),
            onboardingGaps(snapshot, status),
            missingEvidence(snapshot),
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
            true,
            "catalog_only",
            template.code(),
            template.name(),
            snapshot.toSummary(),
            templatePreview,
            onboardingDraftItems(snapshot, templatePreview),
            missingEvidence(snapshot),
            List.of(
                "本预览只读取项目目录元数据、项目来源和标准配置状态，不读取 PDF/Office/DWG/RVT/IFC 正文。",
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
        AssetOnboardingSnapshot snapshot = loadAssetOnboardingSnapshot(projectId);
        if (snapshot.disableDirectTemplateApply()) {
            throw new BusinessException(
                "REAL_PROJECT_TEMPLATE_APPLY_DISABLED",
                "真实 NAS 项目不能通过一键模板直接生成就绪标准，请先使用接入评估草案并由项目负责人逐项确认。",
                HttpStatus.CONFLICT
            );
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
        AssetOnboardingSnapshot snapshot = loadAssetOnboardingSnapshot(projectId);
        if (snapshot.disableDirectTemplateApply()) {
            StandardStatusResponse status = standardStatusApplicationService.getStatus(projectId);
            auditLogApplicationService.record(
                projectId,
                "master-data",
                "masterdata.onboarding.real-project-draft-confirm",
                "REAL_PROJECT_ONBOARDING_DRAFT",
                templateCode,
                operatorId,
                Map.of(
                    "templateCode", templateCode,
                    "assetCatalogOnly", true,
                    "draftApplied", false,
                    "reason", "real_nas_project_requires_manual_masterdata_confirmation"
                )
            );
            return new OnboardingApplyResponse(
                projectId,
                true,
                false,
                false,
                false,
                "catalog_only",
                null,
                onboardingNextActions(snapshot, status)
            );
        }
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
        AssetOnboardingSnapshot snapshot = jdbcTemplate.queryForObject("""
            SELECT p.code AS project_code,
                   p.name AS project_name,
                   p.asset_source,
                   COALESCE(file_stats.file_count, 0) AS file_count,
                   COALESCE(file_stats.model_file_count, 0) AS model_file_count,
                   COALESCE(file_stats.drawing_file_count, 0) AS drawing_file_count,
                   COALESCE(file_stats.document_file_count, 0) AS document_file_count,
                   COALESCE(file_stats.spreadsheet_file_count, 0) AS spreadsheet_file_count,
                   COALESCE(path_stats.path_mapping_count, 0) AS path_mapping_count,
                   COALESCE(scan_stats.scan_task_count, 0) AS scan_task_count,
                   COALESCE(file_stats.missing_checksum_count, 0) AS missing_checksum_count,
                   COALESCE(file_stats.missing_discipline_count, 0) AS missing_discipline_count,
                   COALESCE(file_stats.low_confidence_count, 0) AS low_confidence_count,
                   file_stats.dominant_file_kinds,
                   file_stats.dominant_file_extensions,
                   file_stats.dominant_disciplines,
                   file_stats.directory_clues,
                   file_stats.last_asset_seen_at,
                   scan_stats.last_scan_at
            FROM core_projects p
            LEFT JOIN (
                SELECT project_id,
                       COUNT(1) AS file_count,
                       SUM(CASE WHEN file_kind = 'MODEL' THEN 1 ELSE 0 END) AS model_file_count,
                       SUM(CASE WHEN file_kind = 'DRAWING' THEN 1 ELSE 0 END) AS drawing_file_count,
                       SUM(CASE
                               WHEN file_kind IN ('DOCUMENT', 'PRESENTATION')
                                    OR UPPER(SUBSTRING_INDEX(original_name, '.', -1)) IN ('PDF', 'DOC', 'DOCX', 'PPT', 'PPTX')
                               THEN 1 ELSE 0
                           END) AS document_file_count,
                       SUM(CASE
                               WHEN file_kind = 'SPREADSHEET'
                                    OR UPPER(SUBSTRING_INDEX(original_name, '.', -1)) IN ('XLS', 'XLSX', 'CSV')
                               THEN 1 ELSE 0
                           END) AS spreadsheet_file_count,
                       SUM(CASE WHEN checksum IS NULL OR checksum = '' THEN 1 ELSE 0 END) AS missing_checksum_count,
                       SUM(CASE WHEN discipline IS NULL OR discipline = '' OR discipline IN ('OTHER', 'UNKNOWN', '未分类') THEN 1 ELSE 0 END) AS missing_discipline_count,
                       SUM(CASE WHEN confidence_level IS NULL OR confidence_level = '' OR confidence_level = 'LOW' THEN 1 ELSE 0 END) AS low_confidence_count,
                       GROUP_CONCAT(DISTINCT file_kind ORDER BY file_kind SEPARATOR ',') AS dominant_file_kinds,
                       GROUP_CONCAT(DISTINCT UPPER(SUBSTRING_INDEX(original_name, '.', -1)) ORDER BY UPPER(SUBSTRING_INDEX(original_name, '.', -1)) SEPARATOR ',') AS dominant_file_extensions,
                       GROUP_CONCAT(DISTINCT discipline ORDER BY discipline SEPARATOR ',') AS dominant_disciplines,
                       GROUP_CONCAT(DISTINCT
                           CASE
                               WHEN logical_path IS NULL OR logical_path = '' THEN NULL
                               ELSE SUBSTRING_INDEX(TRIM(BOTH '/' FROM logical_path), '/', 1)
                           END
                           ORDER BY
                           CASE
                               WHEN logical_path IS NULL OR logical_path = '' THEN NULL
                               ELSE SUBSTRING_INDEX(TRIM(BOTH '/' FROM logical_path), '/', 1)
                           END
                           SEPARATOR ',') AS directory_clues,
                       MAX(COALESCE(last_verified_at, updated_at, created_at)) AS last_asset_seen_at
                FROM data_file_resources
                WHERE project_id = :projectId AND deleted = 0
                GROUP BY project_id
            ) file_stats ON file_stats.project_id = p.id
            LEFT JOIN (
                SELECT project_id, COUNT(1) AS path_mapping_count
                FROM data_asset_project_path_mappings
                WHERE project_id = :projectId AND deleted = 0 AND enabled = 1
                GROUP BY project_id
            ) path_stats ON path_stats.project_id = p.id
            LEFT JOIN (
                SELECT project_id,
                       COUNT(1) AS scan_task_count,
                       MAX(COALESCE(completed_at, started_at, updated_at)) AS last_scan_at
                FROM data_asset_scan_tasks
                WHERE project_id = :projectId AND deleted = 0
                GROUP BY project_id
            ) scan_stats ON scan_stats.project_id = p.id
            WHERE p.id = :projectId AND p.deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> new AssetOnboardingSnapshot(
            rs.getString("project_code"),
            rs.getString("project_name"),
            rs.getString("asset_source"),
            rs.getInt("file_count"),
            rs.getInt("model_file_count"),
            rs.getInt("drawing_file_count"),
            rs.getInt("document_file_count"),
            rs.getInt("spreadsheet_file_count"),
            rs.getInt("path_mapping_count"),
            rs.getInt("scan_task_count"),
            rs.getInt("missing_checksum_count"),
            rs.getInt("missing_discipline_count"),
            rs.getInt("low_confidence_count"),
            splitCsv(rs.getString("dominant_file_kinds")),
            splitCsv(rs.getString("dominant_file_extensions")),
            splitCsv(rs.getString("dominant_disciplines")).stream().map(ProjectInitializationApplicationService::disciplineLabel).toList(),
            safeDirectoryClues(rs.getString("directory_clues")),
            List.of(),
            List.of(),
            List.of(),
            toInstant(rs.getTimestamp("last_asset_seen_at")),
            toInstant(rs.getTimestamp("last_scan_at"))
        ));
        return snapshot.withDistributions(
            loadFileKindDistribution(projectId, snapshot.fileCount()),
            loadExtensionDistribution(projectId, snapshot.fileCount()),
            loadDisciplineDistribution(projectId, snapshot.fileCount())
        );
    }

    private List<OnboardingDistributionItem> loadFileKindDistribution(Long projectId, int totalCount) {
        return jdbcTemplate.query("""
            SELECT COALESCE(NULLIF(file_kind, ''), 'UNKNOWN') AS item_code,
                   COALESCE(NULLIF(file_kind, ''), 'UNKNOWN') AS item_label,
                   COUNT(1) AS item_count
            FROM data_file_resources
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY COALESCE(NULLIF(file_kind, ''), 'UNKNOWN')
            ORDER BY item_count DESC, item_code
            LIMIT 12
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> distributionItem(
            rs.getString("item_code"),
            fileKindLabel(rs.getString("item_label")),
            rs.getInt("item_count"),
            totalCount
        ));
    }

    private List<OnboardingDistributionItem> loadExtensionDistribution(Long projectId, int totalCount) {
        return jdbcTemplate.query("""
            SELECT CASE
                       WHEN original_name IS NULL OR original_name = '' OR original_name NOT LIKE '%.%' THEN 'UNKNOWN'
                       ELSE UPPER(SUBSTRING_INDEX(original_name, '.', -1))
                   END AS item_code,
                   COUNT(1) AS item_count
            FROM data_file_resources
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY CASE
                         WHEN original_name IS NULL OR original_name = '' OR original_name NOT LIKE '%.%' THEN 'UNKNOWN'
                         ELSE UPPER(SUBSTRING_INDEX(original_name, '.', -1))
                     END
            ORDER BY item_count DESC, item_code
            LIMIT 16
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> distributionItem(
            rs.getString("item_code"),
            extensionLabel(rs.getString("item_code")),
            rs.getInt("item_count"),
            totalCount
        ));
    }

    private List<OnboardingDistributionItem> loadDisciplineDistribution(Long projectId, int totalCount) {
        return jdbcTemplate.query("""
            SELECT COALESCE(NULLIF(discipline, ''), 'UNSPECIFIED') AS item_code,
                   COUNT(1) AS item_count
            FROM data_file_resources
            WHERE project_id = :projectId AND deleted = 0
            GROUP BY COALESCE(NULLIF(discipline, ''), 'UNSPECIFIED')
            ORDER BY item_count DESC, item_code
            LIMIT 16
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) -> distributionItem(
            rs.getString("item_code"),
            disciplineLabel(rs.getString("item_code")),
            rs.getInt("item_count"),
            totalCount
        ));
    }

    private static OnboardingDistributionItem distributionItem(String code, String label, int count, int totalCount) {
        double ratio = totalCount <= 0 ? 0D : Math.round((count * 10000D) / totalCount) / 10000D;
        return new OnboardingDistributionItem(code, label, count, ratio);
    }

    private List<OnboardingEvidenceClue> onboardingEvidenceClues(AssetOnboardingSnapshot snapshot) {
        List<OnboardingEvidenceClue> clues = new ArrayList<>();
        clues.add(new OnboardingEvidenceClue(
            snapshot.realNasProject() ? "REAL_NAS_PROJECT" : "PROJECT_SOURCE",
            snapshot.realNasProject() ? "真实 NAS 项目" : "非真实 NAS 项目或来源待确认",
            "catalog_only",
            true,
            snapshot.realNasProject()
                ? "项目来源标记为 " + snapshot.assetSource() + "，评估只使用目录级元数据和标准配置状态。"
                : "当前项目来源不是 NAS_REAL*，不能按真实 NAS 项目完成接入结论。"
        ));
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
        if (!snapshot.dominantFileExtensions().isEmpty()) {
            clues.add(new OnboardingEvidenceClue(
                "FILE_EXTENSION_METADATA",
                "主要扩展名线索：" + String.join(" / ", snapshot.dominantFileExtensions()),
                "catalog_only",
                true,
                "扩展名来自文件名元数据，只能辅助判断交付资料类型，不能代表文件内容已解析。"
            ));
        }
        if (!snapshot.dominantDisciplines().isEmpty()) {
            clues.add(new OnboardingEvidenceClue(
                "DISCIPLINE_METADATA",
                "主要专业线索：" + String.join(" / ", snapshot.dominantDisciplines()),
                "catalog_only",
                true,
                "专业来自目录入库或人工治理字段，仍需项目负责人复核。"
            ));
        }
        if (!snapshot.directoryClues().isEmpty()) {
            clues.add(new OnboardingEvidenceClue(
                "DIRECTORY_METADATA",
                "主要目录线索：" + String.join(" / ", snapshot.directoryClues()),
                "catalog_only",
                true,
                "目录名只作为结构线索，不等于真实工程部位树，也不暴露 NAS 原始路径。"
            ));
        }
        if (snapshot.scanTaskCount() > 0) {
            clues.add(new OnboardingEvidenceClue(
                "SCAN_TASK_METADATA",
                "已有只读扫描记录",
                "catalog_only",
                true,
                "平台记录到 " + snapshot.scanTaskCount() + " 条扫描任务，可辅助判断资产登记时间，但不代表读取了文件正文。"
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
        if (!snapshot.realNasProject()) {
            gaps.add(new OnboardingGap(
                "REAL_NAS_SOURCE_NOT_CONFIRMED",
                "warning",
                "当前项目来源不是 NAS_REAL*，不能作为真实 NAS 项目接入结论。",
                "real_nas_project_source_missing"
            ));
        }
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

    private static List<OnboardingGovernanceRisk> governanceRisks(AssetOnboardingSnapshot snapshot) {
        List<OnboardingGovernanceRisk> risks = new ArrayList<>();
        if (snapshot.missingChecksumCount() > 0) {
            risks.add(new OnboardingGovernanceRisk(
                "MISSING_CHECKSUM",
                "warning",
                snapshot.missingChecksumCount(),
                "部分文件尚未形成 checksum，暂不能作为最终交付一致性依据。",
                "catalog_only",
                "checksum_metadata_missing"
            ));
        }
        if (snapshot.missingDisciplineCount() > 0) {
            risks.add(new OnboardingGovernanceRisk(
                "MISSING_DISCIPLINE",
                "warning",
                snapshot.missingDisciplineCount(),
                "部分文件缺少专业标注或仍为通用/未分类，需要人工治理后再形成正式标准。",
                "catalog_only",
                "discipline_metadata_missing"
            ));
        }
        if (snapshot.lowConfidenceCount() > 0) {
            risks.add(new OnboardingGovernanceRisk(
                "LOW_CONFIDENCE",
                "info",
                snapshot.lowConfidenceCount(),
                "部分文件目录识别置信度偏低，草案只能作为候选线索。",
                "catalog_only",
                "classification_confidence_low"
            ));
        }
        if (snapshot.fileCount() > 0 && risks.isEmpty()) {
            risks.add(new OnboardingGovernanceRisk(
                "CATALOG_REVIEW_REQUIRED",
                "info",
                0,
                "目录治理字段暂无明显风险，但真实主数据仍需项目负责人确认。",
                "catalog_only",
                "manual_confirmation_required"
            ));
        }
        return risks;
    }

    private static List<OnboardingMissingEvidence> missingEvidence(AssetOnboardingSnapshot snapshot) {
        List<OnboardingMissingEvidence> evidence = new ArrayList<>();
        evidence.add(new OnboardingMissingEvidence(
            "ASSET_CATALOG_ONLY",
            "当前评估只基于文件目录、扩展名、专业字段和扫描记录，不能把目录元数据当作文件正文或模型内部证据。",
            "full_text_evidence / drawing_parse_evidence / model_parse_evidence",
            "catalog_only"
        ));
        if (snapshot.modelFileCount() > 0) {
            evidence.add(new OnboardingMissingEvidence(
                "MODEL_PARSE_EVIDENCE_MISSING",
                "目录中存在模型文件，但没有 RVT/IFC/NWD 解析结果，不能确认 Level、Grid、Family、Type 或构件参数。",
                "rvt_parse_evidence / component_evidence / model_parse_evidence",
                "catalog_only"
            ));
        }
        if (snapshot.drawingFileCount() > 0) {
            evidence.add(new OnboardingMissingEvidence(
                "DRAWING_PARSE_EVIDENCE_MISSING",
                "目录中存在图纸文件，但没有 DWG/PDF 图纸解析结果，不能确认图层、标题栏、外参或图纸正文。",
                "dwg_parse_evidence / drawing_parse_evidence",
                "catalog_only"
            ));
        }
        if (snapshot.documentFileCount() > 0 || snapshot.spreadsheetFileCount() > 0) {
            evidence.add(new OnboardingMissingEvidence(
                "DOCUMENT_TEXT_EVIDENCE_MISSING",
                "目录中存在文档或清单文件，但没有 PDF/Office 正文证据，不能总结条款或表格内容。",
                "full_text_evidence / office_parse_evidence",
                "catalog_only"
            ));
        }
        return evidence;
    }

    private List<OnboardingDraftItem> onboardingDraftItems(AssetOnboardingSnapshot snapshot, TemplatePreviewResponse preview) {
        List<OnboardingDraftItem> items = new ArrayList<>();
        snapshot.disciplineDistribution().stream()
            .filter(item -> item.count() != null && item.count() > 0)
            .limit(8)
            .forEach(item -> items.add(new OnboardingDraftItem(
                "DISCIPLINE_CANDIDATE",
                item.label(),
                "真实资产目录中出现 " + item.count() + " 个该专业文件，可作为专业候选，但需要项目负责人确认。",
                "catalog_only",
                "CATALOG_DISCIPLINE_DISTRIBUTION",
                item.count() >= 100 ? "HIGH" : "MEDIUM",
                "专业字段来自目录入库或人工治理，不代表文件正文、图纸内容或模型构件已经被解析。",
                true,
                false,
                true
            )));
        snapshot.extensionDistribution().stream()
            .filter(item -> item.count() != null && item.count() > 0)
            .filter(item -> List.of("RVT", "DWG", "PDF", "XLS", "XLSX", "CSV").contains(item.code()))
            .forEach(item -> items.add(new OnboardingDraftItem(
                "DELIVERABLE_TYPE_CANDIDATE",
                item.label(),
                "真实资产目录中出现 " + item.count() + " 个 " + item.code() + " 文件，可作为交付类型候选。",
                "catalog_only",
                "CATALOG_EXTENSION_DISTRIBUTION",
                item.count() >= 50 ? "HIGH" : "MEDIUM",
                extensionRiskHint(item.code()),
                true,
                false,
                true
            )));
        if (snapshot.fileCount() > 0) {
            items.add(new OnboardingDraftItem(
                "TARGET_CANDIDATE",
                "项目级交付对象",
                "真实资产已按项目归集，可先建立项目级交付对象，再逐步拆分到专业、部位或文件类型。",
                "catalog_only",
                "CATALOG_PROJECT_ASSET_SUMMARY",
                "MEDIUM",
                "项目级对象只能作为接入草案入口，不能替代正式部位树和节点类型。",
                true,
                false,
                true
            ));
        }
        if (!snapshot.disciplineDistribution().isEmpty()) {
            items.add(new OnboardingDraftItem(
                "TARGET_CANDIDATE",
                "专业级交付对象",
                "真实资产目录中已有专业分布，可将专业作为人工确认的候选维度。",
                "catalog_only",
                "CATALOG_DISCIPLINE_DISTRIBUTION",
                "MEDIUM",
                "专业维度需要与真实工程部位树和交付责任人确认后才能成为正式规则。",
                true,
                false,
                true
            ));
        }
        if (!snapshot.extensionDistribution().isEmpty()) {
            items.add(new OnboardingDraftItem(
                "TARGET_CANDIDATE",
                "文件类型级交付对象",
                "真实资产目录中已有 DWG/PDF/RVT/Excel 等文件类型线索，可辅助规划交付物类型。",
                "catalog_only",
                "CATALOG_EXTENSION_DISTRIBUTION",
                "MEDIUM",
                "文件扩展名只能说明目录层面的文件类型，不能证明文件内容符合交付要求。",
                true,
                false,
                true
            ));
        }
        preview.items().stream()
            .map(item -> new OnboardingDraftItem(
                item.category(),
                item.name(),
                item.reason(),
                "catalog_only",
                draftEvidenceSource(item, snapshot),
                draftConfidence(item, snapshot),
                draftRiskHint(item, snapshot),
                assetClueSupports(item.category(), snapshot),
                true,
                true
            ))
            .forEach(items::add);
        return items;
    }

    private List<String> onboardingNextActions(AssetOnboardingSnapshot snapshot, StandardStatusResponse status) {
        List<String> actions = new ArrayList<>();
        if (snapshot.pathMappingCount() <= 0 && snapshot.fileCount() <= 0) {
            actions.add("先完成真实项目路径映射或只读资产目录登记");
        }
        if (!Boolean.TRUE.equals(status.hasSectionTree()) || !Boolean.TRUE.equals(status.hasNodeTypes())) {
            actions.add(snapshot.disableDirectTemplateApply()
                ? "基于真实资产线索人工确认工程部位树和节点类型，不直接套用模板"
                : "生成真实项目接入草案并人工确认工程部位/节点类型");
        }
        if (!Boolean.TRUE.equals(status.deliverableStandardReady())) {
            actions.add(snapshot.disableDirectTemplateApply()
                ? "根据 DWG/PDF/RVT/Excel 线索逐项配置交付定义、交付类型和目录模板"
                : "补齐交付定义、交付类型、交付属性与目录模板");
        }
        if (actions.isEmpty()) {
            actions.add("进入文档交付和图纸交付页面，继续人工复核挂接状态与导出预检查");
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

    private static String draftEvidenceSource(TemplatePreviewItemResponse item, AssetOnboardingSnapshot snapshot) {
        if ("SKIP".equals(item.action())) {
            return "EXISTING_PROJECT_MASTERDATA";
        }
        if ("DIRECTORY_TEMPLATE".equals(item.category()) && !snapshot.directoryClues().isEmpty()) {
            return "CATALOG_DIRECTORY_CLUE";
        }
        if ("DELIVERABLE_TYPE".equals(item.category()) && assetClueSupports(item.category(), snapshot)) {
            return "CATALOG_FILE_KIND_CLUE";
        }
        return "TEMPLATE_SKELETON";
    }

    private static String draftConfidence(TemplatePreviewItemResponse item, AssetOnboardingSnapshot snapshot) {
        if ("SKIP".equals(item.action())) {
            return "EXISTING";
        }
        if (assetClueSupports(item.category(), snapshot)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static String draftRiskHint(TemplatePreviewItemResponse item, AssetOnboardingSnapshot snapshot) {
        if ("SKIP".equals(item.action())) {
            return "项目中已存在同编码/同名标准项，本次不会覆盖。仍建议项目负责人复核是否符合真实项目结构。";
        }
        if ("DIRECTORY_TEMPLATE".equals(item.category()) && !snapshot.directoryClues().isEmpty()) {
            return "存在目录线索可辅助组织交付目录，但目录名不能替代真实工程部位或正式交付标准。";
        }
        if ("DELIVERABLE_TYPE".equals(item.category()) && assetClueSupports(item.category(), snapshot)) {
            return "目录中存在相关文件类型线索，但没有读取文件正文、图纸内容或模型构件证据。";
        }
        if ("NODE_TYPE".equals(item.category()) || "SECTION_NODE".equals(item.category())) {
            return "该项来自模板默认骨架，不代表真实楼栋、楼层或系统已经被平台识别。";
        }
        return "该项来自建筑机电/BIM交付基础模板，应用后仍需人工确认和项目级调整。";
    }

    private static String extensionRiskHint(String extension) {
        String normalized = extension == null ? "" : extension.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RVT" -> "当前只有 RVT 文件目录线索，没有模型解析证据，不能判断 Level/Grid/Family/Type 或构件参数。";
            case "DWG" -> "当前只有 DWG 文件目录线索，没有图纸解析证据，不能判断图层、标题栏、外参或图纸内容。";
            case "PDF" -> "当前只有 PDF 文件目录线索，没有正文/图纸解析证据，不能总结条款或确认图纸内容。";
            case "XLS", "XLSX", "CSV" -> "当前只有清单文件目录线索，没有表格正文证据，不能确认清单内容。";
            default -> "当前只有文件扩展名线索，不能替代文件正文或模型解析证据。";
        };
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

    private static String fileKindLabel(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MODEL" -> "模型";
            case "DRAWING" -> "图纸";
            case "DOCUMENT" -> "文档";
            case "SPREADSHEET" -> "清单/表格";
            case "PRESENTATION" -> "演示文档";
            case "ARCHIVE" -> "压缩包";
            case "IMAGE" -> "图片";
            default -> "未分类";
        };
    }

    private static String extensionLabel(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RVT" -> "RVT 模型";
            case "IFC" -> "IFC 模型";
            case "NWD", "NWC" -> "Navisworks 模型";
            case "DWG" -> "DWG 图纸";
            case "PDF" -> "PDF 图纸/文档";
            case "XLS", "XLSX", "CSV" -> "Excel/清单";
            default -> normalized.isBlank() || "UNKNOWN".equals(normalized) ? "未知格式" : normalized + " 文件";
        };
    }

    private static String disciplineLabel(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ARCHITECTURE", "ARCH", "建筑" -> "建筑";
            case "STRUCTURE", "STRUCT", "结构" -> "结构";
            case "ELECTRICAL", "ELEC", "电气" -> "电气";
            case "PLUMBING", "给排水" -> "给排水";
            case "FIRE", "FIRE_PROTECTION", "消防" -> "消防";
            case "WEAK_CURRENT", "INTELLIGENT", "SMART", "智能化" -> "智能化";
            case "HVAC", "暖通" -> "暖通";
            case "GAS", "燃气" -> "燃气";
            case "GENERAL", "COMPREHENSIVE", "OTHER", "UNSPECIFIED", "UNKNOWN", "未分类", "" -> "通用/未标注";
            default -> value;
        };
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

    private static List<String> safeDirectoryClues(String value) {
        List<String> rows = splitCsv(value);
        if (rows.isEmpty()) {
            return rows;
        }
        List<String> safe = rows.stream()
            .filter(item -> !isRawPathLike(item))
            .map(ProjectInitializationApplicationService::trimDirectoryClue)
            .filter(item -> !item.isBlank())
            .distinct()
            .limit(8)
            .toList();
        if (safe.isEmpty()) {
            return List.of("项目内目录线索已脱敏");
        }
        return safe;
    }

    private static boolean isRawPathLike(String value) {
        String text = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return text.contains("storage_path")
            || text.contains("storage_uri")
            || text.contains("nas://")
            || text.contains("smb://")
            || text.contains("afp://")
            || text.contains("/volumes/")
            || text.contains("/users/")
            || "volumes".equals(text)
            || "users".equals(text)
            || "storage".equals(text)
            || "nas".equals(text)
            || "smb".equals(text);
    }

    private static String trimDirectoryClue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
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
        String projectCode,
        String projectName,
        String assetSource,
        int fileCount,
        int modelFileCount,
        int drawingFileCount,
        int documentFileCount,
        int spreadsheetFileCount,
        int pathMappingCount,
        int scanTaskCount,
        int missingChecksumCount,
        int missingDisciplineCount,
        int lowConfidenceCount,
        List<String> dominantFileKinds,
        List<String> dominantFileExtensions,
        List<String> dominantDisciplines,
        List<String> directoryClues,
        List<OnboardingDistributionItem> fileKindDistribution,
        List<OnboardingDistributionItem> extensionDistribution,
        List<OnboardingDistributionItem> disciplineDistribution,
        Instant lastAssetSeenAt,
        Instant lastScanAt
    ) {
        OnboardingAssetSummary toSummary() {
            return new OnboardingAssetSummary(
                fileCount,
                modelFileCount,
                drawingFileCount,
                documentFileCount,
                spreadsheetFileCount,
                pathMappingCount,
                scanTaskCount,
                dominantFileKinds,
                dominantFileExtensions,
                dominantDisciplines,
                directoryClues,
                fileKindDistribution,
                extensionDistribution,
                disciplineDistribution,
                governanceRisks(this),
                missingEvidence(this),
                lastAssetSeenAt,
                lastScanAt
            );
        }

        AssetOnboardingSnapshot withDistributions(
            List<OnboardingDistributionItem> nextFileKindDistribution,
            List<OnboardingDistributionItem> nextExtensionDistribution,
            List<OnboardingDistributionItem> nextDisciplineDistribution
        ) {
            return new AssetOnboardingSnapshot(
                projectCode,
                projectName,
                assetSource,
                fileCount,
                modelFileCount,
                drawingFileCount,
                documentFileCount,
                spreadsheetFileCount,
                pathMappingCount,
                scanTaskCount,
                missingChecksumCount,
                missingDisciplineCount,
                lowConfidenceCount,
                dominantFileKinds,
                dominantFileExtensions,
                dominantDisciplines,
                directoryClues,
                nextFileKindDistribution,
                nextExtensionDistribution,
                nextDisciplineDistribution,
                lastAssetSeenAt,
                lastScanAt
            );
        }

        boolean realNasProject() {
            return assetSource != null && assetSource.toUpperCase(Locale.ROOT).startsWith("NAS_REAL");
        }

        boolean disableDirectTemplateApply() {
            String source = assetSource == null ? "" : assetSource.toUpperCase(Locale.ROOT);
            return source.startsWith("NAS_REAL") && !source.contains("SMOKE");
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

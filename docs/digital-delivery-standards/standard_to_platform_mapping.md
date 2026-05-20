# 标准到平台映射 V0.1-beta

本文件只给出标准字段到稳定 View/API 的映射建议，不写死真实底表。Hermes 只能通过只读 Catalog Tool / Gateway / View Contract 获取资产目录，不直接访问业务底表，不生成 SQL。

## 映射原则

- `FileAssetView` / `ModelAssetView` 是 Hermes 对接的稳定读模型建议。
- MySQL / Data Steward 是工程资产事实层，Hermes 不直接访问底表。
- raw `storage_path` 是内部敏感字段，不默认对外暴露。
- `display_path` / `path_hint` 应进入稳定 View/API，但当前仍为 backlog。
- Catalog API 的 `logicalPath` 可作为当前页面展示用的脱敏项目内路径，但不等同于稳定 View 字段 `display_path` / `path_hint`。
- `GENERAL` / 空专业 / 弱命名文件必须允许低置信度线索或 Missing Evidence。
- 现有 `discipline` 字段不应反向决定公司专业标准。V0.1-beta 建议把 `discipline_code`、`discipline_name_cn`、`discipline_alias_cn` 作为标准业务字典草案；平台现有枚举只做映射和过渡。
- 工务署标准作为公司数字化交付标准底座主干。平台后续应把工务署标准来源、版本、建设阶段分类编码、运维阶段分类编码和公司扩展编码纳入标准配置或稳定字典，而不是散落在脚本、前端下拉框或 Agent prompt 中。

| 标准字段 | 建议来源 | 当前状态 | 备注 |
|---|---|---|---|
| `file_id` | `FileAssetView.file_id` / 稳定文件资产 ID | current | 文件资产 join key。 |
| `model_id` | `ModelAssetView.model_id` | current | 模型资产 join key。 |
| `project_id` | `FileAssetView.project_id` / `ModelAssetView.project_id` | current | 项目维度基础字段。 |
| `project_code` | `FileAssetView.project_code` / `ModelAssetView.project_code` | current | 项目编码线索。 |
| `project_name` | `FileAssetView.project_name`；`ModelAssetView.project_name` 待补 | File current / Model backlog | 当前 `ModelAssetView` 无稳定 `project_name` 字段，模型侧需通过 `project_id` / `project_code` 回查项目名称。 |
| `asset_type` | `file_kind` / `file_ext` / `model_format` 映射 | current | 可做 RVT/PDF/DWG/IFC/NWC/NWD/DXF/Office 类型线索；统一标准资产类型字段仍需治理。 |
| `file_format` | `file_ext` / `model_format` | current | 文件格式白名单基础；MIME 如后续进入稳定 API/View 再补充。 |
| `file_name` | `FileAssetView.file_name` | current | filename evidence。 |
| `model_name` | `ModelAssetView.model_name` | current | 模型名称或文件名线索。 |
| `file_kind` | `FileAssetView.file_kind` | current | 平台现有文件类型字段，不等同完整标准资产类型。 |
| `discipline` | `FileAssetView.discipline` / `ModelAssetView.discipline` / filename / path | current / current-lineage / needs standardization | View 字段可用但当前业务口径较弱；filename/path 只能作为 lineage 线索；`GENERAL` 和空值多，不能完全依赖。后续应向标准 `discipline_code` 收敛。 |
| `discipline_code` | 标准专业字典：ARCH / STR / PLMB / HVAC / ELEC / FIRE / INT / DEC / LAND / CURT / GAS / SEIS / CIVIL / OTHER | 标准 current / 平台 backlog | V0.1-beta 先定义公司业务标准；平台是否新增字段、枚举或映射层需平台 Agent 评审。 |
| `discipline_name_cn` | 标准专业字典中文名 | 标准 current / 平台 backlog | 用于界面、报表、Manifest、人工补录和交付清单。 |
| `discipline_alias_cn` | 标准专业字典中文别名 / 文件名关键词 | current-lineage / platform-backlog | 用于历史文件识别，如建施、结施、水施、暖通施、电施、智施、消水等；不能直接作为稳定事实。 |
| `platform_discipline_value` | 当前平台枚举映射 | current/backlog | 例如 ARCH -> ARCHITECTURE；DEC/LAND/CURT/SEIS 等可能暂映射 GENERAL / TBD，需后续扩展。 |
| `discipline_source` | TBD | backlog | 建议记录 manual / metadata / filename / path_hint / manifest。 |
| `discipline_confidence` | TBD | backlog | 建议记录 confirmed / high / medium / low / missing；避免把文件名线索写成专业事实。 |
| `stage` | delivery context / Manifest / filename / path keyword | backlog / current-lineage | 当前无稳定文件级阶段字段；filename/path keyword 只能作为 lineage 线索。 |
| `stage_source` | TBD | backlog | 建议记录阶段来源和置信度。 |
| `version_no` | `FileAssetView.version_no` / `ModelAssetView.version_no` / filename | current / current-lineage | View 字段为 current；文件名版本和日期批次为 lineage 线索，需标准化。 |
| `building_or_zone` | filename / path keyword | backlog / current-lineage | 稳定字段待补；历史目录/文件名只能作为线索。 |
| `drawing_number` / `model_number` | filename / project config | backlog / current-lineage | 仅作为目录级编号线索。 |
| `external_code_system` | project config / manual evidence | backlog | 外部顾问/甲方编码体系。 |
| `project_override` / `owner_override` | project standard binding | backlog | 标准冲突和覆盖记录。 |
| `standard_binding_id` | 项目标准绑定配置 | backlog | 新项目默认绑定公司基础标准版本；未稳定落地前只作为流程要求。 |
| `company_standard_version` | 项目标准绑定配置 | backlog | 建议记录 `V0.1-beta`、后续 `V0.1` / `V0.2`。 |
| `standard_profile_id` | 平台标准配置档案 | backlog | 用于项目初始化继承命名、专业、阶段、Manifest、交付包和族库规则。 |
| `override_source` | 项目覆盖依据 | backlog | 甲方、工务署、地方、顾问或合同来源。 |
| `override_scope` | 项目覆盖范围 | backlog | 专业、阶段、命名、Manifest、族库等。 |
| `legacy_source_id` | 历史标准或旧项目依据 | backlog | 用于历史项目和 SMB 现行标准回拉矩阵。 |
| `replacement_status` | 标准替代状态 | backlog | 建议值：legacy / active / replaced / reference。 |
| `standard_source_id` | 标准配置 / 项目级标准绑定 | 标准 current / 平台 backlog | 建议取值如 `GWS-DG-2024`、`GWS-BIM-DATA-2023`、`GWS-CONSTRUCTION-CODE-2023`、`GWS-OM-CODE-2023`。 |
| `standard_source_version` | 标准配置 / 项目级标准绑定 | backlog | 用于记录工务署标准版本、公司标准版本和项目适用版本。 |
| `gws_stage_code` | 工务署建设阶段项目阶段分类 `43` / 项目配置 / Manifest | backlog/current-lineage | 如 `43-12.11.00` 方案设计、`43-12.13.00` 施工图设计；文件名/路径仅作线索，不得自动固化。 |
| `gws_stage_node` | 工务署交付节点 / 项目配置 / Manifest | backlog/current-lineage | 勘察、方案设计、初步设计、施工图设计、地基与基础施工、主体结构施工、机电安装施工、幕墙施工、建筑装饰装修施工、竣工。 |
| `gws_classification_code` | 工务署建设阶段/运维阶段分类编码 | backlog/future | 泛化字段，具体可拆为建筑、空间、系统、构件、材质、属性、项目阶段、分部分项等。 |
| `gws_building_class_code` | 工务署建设阶段建筑分类 `10` / 运维建筑分类 | backlog | 当前不作为 catalog-only 必需字段。 |
| `gws_space_class_code` | 工务署建设阶段空间分类 `12` / 运维空间分类 | backlog/future | 需要项目结构或模型解析/人工确认。 |
| `gws_system_class_code` | 工务署建设阶段系统分类 `16` / 运维设备系统分类 | backlog/future | 需要模型/设备对象证据。 |
| `gws_component_class_code` | 工务署建设阶段构件分类 `30` / 运维构件设备分类 | future | 当前不得基于资产目录推断构件编码。 |
| `gws_work_breakdown_code` | 工务署分部分项工程分类 `44` | backlog | 可作为后续成本/清单/工程量扩展主干。 |
| `company_project_instance_code` | 公司项目实例编码配置 | backlog | 在工务署分类编码基础上扩展，不改写工务署编码。 |
| `enterprise_asset_code` | 企业资产台账 / 运维对象编码 | backlog | 依附运维阶段建筑、空间、设备分类编码。 |
| `cost_code` | 成本/分部分项/清单配置 | backlog | 建议依附 `44` 分部分项工程或构件分类。 |
| `procurement_code` | 采购/物料/设备配置 | backlog | 建议依附产品预制采购阶段和设备分类。 |
| `family_library_code` | Revit 族库 / 构件模板配置 | future | 需 BIM/CAD 负责人确认并依附构件/设备分类。 |
| `storage_path` | 内部存储字段 | current-sensitive | 不默认暴露，不进入 Hermes memory。 |
| `logical_path` | `FileAssetView.logical_path` / API `logicalPath` | current-internal / API-safe-current | `FileAssetView.logical_path` 本身不直接暴露给 Hermes；仅允许使用 Gateway/API 脱敏后的项目内相对路径。 |
| `display_path` | Gateway/API 脱敏结果，建议补稳定 View 字段 | backlog | Catalog API 的 `logicalPath` 可作为当前展示路径；稳定 View/Gateway contract 字段仍为 backlog。 |
| `path_hint` | Gateway/API 脱敏路径提示，建议补稳定 View 字段 | backlog | Hermes 默认使用方向；无脱敏路径时不得展示 raw path。 |
| `updated_at` | 目录记录更新时间 | current | 不等于 NAS 文件本体 mtime。 |
| `last_seen_at` | 扫描或同步任务字段 | current | 扫描/验证线索。 |
| `source_modified_at` / `file_modified_at` | TBD | backlog | 当前不能稳定采集；不得用 `updated_at` 替代。 |
| `checksum` | `FileAssetView.checksum` / 异步补算任务 | current nullable | 空值不能解释为文件无变化。 |
| `checksum_status` | TBD | backlog | 需稳定校验状态。 |
| `process_status` | 平台处理状态字段 | current | 不等于 `semantic_index_status`。 |
| `index_eligibility` | 平台策略字段 | current | 当前用于 catalog_only。 |
| `semantic_index_status` | TBD Semantic Index View/API | future | 当前不得承诺全文问答或语义搜索。 |
| `component_index_status` | `ModelAssetView.component_index_status` / placeholder | future / placeholder | **字段出现不等于构件索引真实可用；`NOT_REQUIRED` 不得解释为完成。** |
| `preview_available` | `ModelAssetView.preview_available` | placeholder/backlog | 字段存在但当前可为占位，不代表真实预览能力。 |
| `preview_status` / `preview_mode` / `conversion_status` | Preview API / export-precheck | current API / backlog View | REST 预览状态和导出预检查可作为当前 API 状态；不来自稳定 SQL View，且预览状态不是正文证据。 |
| `permission_tags` | 权限服务 / 读模型字段 | current | 支撑 permission-aware。 |
| `confidentiality_level` | 权限服务 / 读模型字段 | current | public/internal/confidential/restricted。 |
| `delivery_package_id` | DeliveryPackage View/API | backlog | 当前无稳定 View。 |
| `manifest_id` | Manifest View/API | backlog | 当前无稳定结构化 Manifest View。 |
| `required_optional` | Manifest View/API | backlog | 必交/选交。 |
| `delivery_status` | DeliveryPackage / Manifest View/API | backlog | 交付状态。 |
| `check_status` | Manifest / rule check result | backlog | 检查状态。 |
| `field_source` / `confidence` | TBD | backlog | 字段来源与置信度机制。 |

## 待平台评审项

- `display_path` / `path_hint` 是否进入稳定 View/API。
- `stage` 是否由交付上下文、Manifest、目录关键词或文件名解析生成。
- 是否新增或映射 `discipline_code` / `discipline_name_cn` / `discipline_alias_cn` / `platform_discipline_value`。
- 专业字段如何处理 `GENERAL` 和空值，以及是否允许 DEC / LAND / CURT / SEIS / CIVIL 等从 `GENERAL` 中逐步拆分。
- `消水`、`弱电` 等容易跨专业的中文别名应由 BIM/CAD 负责人确认后再固化映射。
- 结构化 Manifest / DeliveryPackageView 是否进入 V0.2。
- `source_modified_at`、`checksum_status`、稳定 View 预览状态的采集策略。
- 三层标准体系的项目级标准绑定和 owner/project override 机制。
- 工务署标准来源、版本、建设阶段编码、运维阶段编码和公司扩展编码是否进入标准字典/配置中心。
- 公司扩展编码字段是否采用 `company_project_instance_code`、`enterprise_asset_code`、`cost_code`、`procurement_code`、`family_library_code` 等命名。
- 项目初始化是否补 `standard_binding_id`、`company_standard_version`、`standard_profile_id` 和 override 来源字段。
- 历史标准回拉矩阵是否需要 `legacy_source_id` 与 `replacement_status` 支撑。

## Gateway Contract 待明确项

- `safe logicalPath` / `displayPath` / `pathHint` 的语义边界，以及与内部 `logical_path` / `storage_path` 的区别。
- `contentEvidenceAvailable=false` 的含义：表示缺少内容级证据，不代表内容事实为否。
- `previewStatus` / `previewMode` / `conversionStatus` 是预览能力状态，不是正文、图纸内容或模型构件证据。
- `indexEligibility=catalog_only` 不等于语义索引完成。

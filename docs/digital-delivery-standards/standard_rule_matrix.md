# 标准规则矩阵 V0.1-beta

本矩阵用于数据库 / 平台 Agent 与 Hermes Agent 共同评审。规则阶段必须保持 current / current-lineage / backlog / future，证据不足时不得过度推断。

`owner_review_needed` 建议固定枚举：`no` / `platform` / `Hermes` / `BIM-CAD` / `delivery-owner` / `owner-project` / `architecture`。

当前矩阵包含 R001-R050；新增规则必须显式标注 current / current-lineage / backlog / future，并同步更新平台映射与 Hermes 边界。

| rule_id | rule_name | asset_type | phase | evidence_level | current_action | missing_evidence_policy | owner_review_needed |
|---|---|---|---|---|---|---|---|
| R001 | 文件格式白名单检查 | ALL | current | `metadata_evidence` / `filename_evidence` | 基于 `file_ext` / `model_format` 判断 RVT/PDF/DWG/IFC/NWC/NWD/DXF/Office 等类型。 | 格式未知时标记 OTHER，不推断内容。 | no |
| R002 | 资产是否进入目录 | ALL | current | `catalog_evidence` | 判断是否存在资产目录记录。 | 无目录记录时返回未入目录或 Missing Evidence。 | no |
| R003 | file_id 存在性 | FILE | current | `catalog_evidence` | 检查文件资产稳定 ID。 | 缺失时不可作为可追踪资产回答。 | no |
| R004 | model_id 存在性 | RVT/BIM | current | `catalog_evidence` | 检查模型资产稳定 ID。 | 缺失时只能退回 file_id 或 Missing Evidence。 | no |
| R005 | 标准专业编码与文件名专业线索识别 | DWG/RVT/PDF | current-lineage | `filename_evidence` / `metadata_evidence` / `path_evidence` | 以标准专业字典 ARCH/STR/PLMB/HVAC/ELEC/FIRE/INT/DEC/LAND/CURT/GAS/SEIS/CIVIL/OTHER 为目标，识别建筑/建施、结构/结施、给排水/水施、暖通、电气、消防、智能化等线索。 | GENERAL/空专业时提示低置信度或 Missing Evidence；文件名/路径别名不得直接写成稳定专业字段。 | BIM-CAD |
| R006 | 文件名阶段线索识别 | DWG/RVT/PDF | current-lineage | `filename_evidence` / `path_evidence` | 识别施工图、蓝图、审图/报审、招标/Tender、过程文件、发布文件、BIM模型、竣工等关键词。 | 不能识别时标记 OTHER 或 Missing Evidence，不要求历史资料改名。 | BIM-CAD |
| R007 | 版本字段 / 文件名版本线索识别 | ALL | current | `metadata_evidence` / `filename_evidence` | `version_no` 为 current；V1.0/V3.0/V3.1/V4.0、日期批次等文件名线索为 current-lineage。 | 空值或冲突时提示不确定，不判断最新版本。 | no |
| R008 | 历史 DWG 编码兼容识别 | DWG | current-lineage | `filename_evidence` | 识别建施/结施/智施、电施、GS/JS/DS、A-101、外部顾问编码等。 | 仅作为线索，不证明图纸内容或标题栏合规。 | BIM-CAD |
| R009 | 泛名文件标记 | DWG/PDF | current-lineage | `filename_evidence` | 标记底图、图框、通用图、大样图等泛名文件。 | 可识别但不作为完整交付资产合规命名正例。 | BIM-CAD |
| R010 | 弱 RVT 命名标记 | RVT | current-lineage | `filename_evidence` | 标记 `model_数字.rvt` 等弱命名 / 扫描型命名。 | 不作为公司命名标准样例，仅提示后续治理。 | BIM-CAD |
| R011 | 目录项目层级线索识别 | ALL | current-lineage | `path_evidence` | 识别项目编号-项目名等项目目录线索。 | 只有 raw path 时拒绝展示并返回路径证据不可用；不作为稳定项目字段。 | no |
| R012 | 文件流转目录线索识别 | ALL | current-lineage | `path_evidence` | 识别文件收发、收/发/IN、日期/批次、收图纸、收文件。 | 只作为流转/批次线索。 | no |
| R013 | 过程/发布/提资目录线索识别 | ALL | current-lineage | `path_evidence` | 识别过程文件、发布文件、提资文件。 | 不推断正式交付状态。 | no |
| R014 | 图纸/模型目录线索识别 | DWG/PDF/RVT/BIM | current-lineage | `path_evidence` | 识别图纸、CAD、PDF、模型、BIM模型等目录关键词。 | 不推断内容合规。 | no |
| R015 | 权限标签检查 | ALL | current | `metadata_evidence` | 检查 `permission_tags` 或权限 scope。 | 缺失时返回 `permission_denied_or_unproven`，默认 fail-closed。 | no |
| R016 | 保密等级检查 | ALL | current | `metadata_evidence` | 检查 `confidentiality_level`。 | 缺失时按更保守策略处理。 | no |
| R017 | storage_path 默认不暴露 | ALL | current | policy / `metadata_evidence` | 对外输出不得包含 raw `storage_path`。 | 只有 raw path 时返回路径证据不可用。 | no |
| R018 | catalog metadata 不作为正文 evidence | ALL | current | policy | 回答中明确 catalog metadata 只能证明目录事实。 | 涉及正文/内容/构件时 Missing Evidence。 | no |
| R019 | process_status 边界检查 | ALL | current | `metadata_evidence` | 可解释平台处理状态。 | 不得解释为 semantic index status。 | no |
| R020 | index_eligibility 检查 | ALL | current | `metadata_evidence` | 可说明 catalog_only 索引资格。 | 不得解释为语义索引完成。 | no |
| R021 | checksum 值存在性 | FILE | current | `metadata_evidence` | 可说明已有 checksum 值。 | `checksum=null` 不得解释为文件无变化。 | no |
| R022 | Manifest / 清单候选文件存在性 | OFFICE/PDF/ALL | current-lineage | `filename_evidence` / `path_evidence` / `catalog_evidence` | 基于文件名、目录关键词或资产目录识别交付清单/Manifest 候选文件是否存在。 | 不判断结构化 Manifest 完整性、必交/选交或正式交付包状态。 | delivery-owner |
| R023 | source_modified_at / file_modified_at | ALL | backlog | `metadata_evidence` | 暂不执行。 | 字段缺失时 Missing Evidence；`updated_at` 不得替代。 | platform |
| R024 | checksum_status 检查 | FILE | backlog | `metadata_evidence` | 需稳定校验状态。 | 字段缺失时 Missing Evidence。 | platform |
| R025 | display_path / path_hint 稳定字段 | ALL | backlog | `path_evidence` | 需稳定 View/API 字段。 | 无脱敏路径时不得展示 raw path。 | platform |
| R026 | delivery Manifest 完整性 | MANIFEST | backlog | `metadata_evidence` | 检查 required_optional、delivery_status、check_status 等。 | 无结构化 Manifest 时 Missing Evidence。 | delivery-owner |
| R027 | 交付包状态检查 | PACKAGE | backlog | `metadata_evidence` | 需 DeliveryPackageView / ManifestView。 | 无稳定 View 时 Missing Evidence。 | delivery-owner |
| R028 | preview_status / preview_mode / conversion_status | DWG/PDF/RVT/BIM | current API / backlog View | `preview_evidence` | REST Preview API / export-precheck 可解释预览状态；稳定 View 字段仍待补。 | 预览状态不能替代正文、图纸内容或模型构件证据。 | platform |
| R029 | 字段来源与置信度 | ALL | backlog | `metadata_evidence` | 记录 manual/metadata/filename/path_hint 来源和 confidence。 | 缺失时只能按线索回答。 | platform |
| R030 | 项目/业主/顾问标准覆盖 | ALL | backlog | `manual_evidence` / `metadata_evidence` | 记录 project_override / owner_override。 | Hermes 不裁决冲突，返回 Needs Review。 | owner-project |
| R031 | DWG 图层检查 | DWG | future | `dwg_parse_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R032 | DWG 图框 / 标题栏检查 | DWG | future | `dwg_parse_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R033 | DWG 外部参照检查 | DWG | future | `dwg_parse_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R034 | DWG 块属性检查 | DWG | future | `dwg_parse_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R035 | RVT sheets/views/levels/families 检查 | RVT/BIM | future | `rvt_parse_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R036 | RVT Family / Type 命名检查 | RVT/BIM | future | `rvt_parse_evidence` / `component_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R037 | BIM 构件参数检查 | RVT/BIM | future | `component_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R038 | BIM LOD / LOI 检查 | RVT/BIM | future | `component_evidence` / `manual_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | BIM-CAD |
| R039 | PDF / Office 正文问答 | PDF/OFFICE | future | `full_text_evidence` | 当前不执行。 | 当前必须 Missing Evidence。 | Hermes |
| R040 | NAS semantic collection | ALL | future | semantic index evidence | 当前不执行。 | 当前必须 Missing Evidence。 | architecture |
| R041 | 工程语义搜索 | ALL | future | `full_text_evidence` / semantic index evidence | 当前不执行。 | 当前必须 Missing Evidence。 | architecture |
| R042 | 工务署主干标准来源绑定 | ALL | current | policy / `manual_evidence` | 标准文档层明确以工务署 2024 导则、BIM 数据交付标准、建设阶段编码标准、运维阶段编码标准为主干。 | 若项目适用标准未绑定，Hermes 只能说明公司默认标准来源，不能替项目裁决。 | platform |
| R043 | 工务署分类编码保持不变 | ALL | current | policy | 平台和公司扩展编码不得改写工务署表代码、分类对象编码、建设/运维阶段映射关系。 | 缺少具体分类字段时不得自动生成工务署编码。 | architecture |
| R044 | 公司扩展编码命名空间 | ALL | backlog | `metadata_evidence` / `manual_evidence` | 后续建立项目实例、企业资产、成本、采购、族库等扩展编码字段或配置。 | 无稳定字段时 Missing Evidence / Needs Review。 | platform |
| R045 | 建设阶段与运维阶段编码映射 | BIM/OM | backlog/future | `metadata_evidence` / `component_evidence` / `manual_evidence` | 后续建立 `gws_construction_code` 与 `gws_om_code` 映射。 | 当前不能基于目录或文件名推断运维对象、设备编码或构件映射。 | BIM-CAD |
| R046 | 标准包镜像一致性检查 | ALL | current | policy | 标准变更后运行镜像比对，确保平台和 Hermes 镜像与 canonical source 一致。 | 镜像不一致时不得基于镜像继续开发。 | architecture |
| R047 | 无标准不开发门禁 | ALL | current | policy | 新增字段、Agent 能力、治理规则、项目模板、族库规则前必须先回链标准。 | 缺少标准回链时标记 Needs Review，不写成 current。 | architecture |
| R048 | 项目标准默认绑定 | PROJECT/PACKAGE | backlog | `metadata_evidence` / `manual_evidence` | 新项目默认继承公司基础标准版本，差异通过 `project_override` / `owner_override` 管理。 | 当前无稳定绑定字段时只作为流程要求，不宣称平台已自动执行。 | delivery-owner |
| R049 | 族库治理入口 | BIM/FAMILY | backlog/future | `manual_evidence` / `component_evidence` | 族库分类、命名、参数、制作、审核、发布、权限、版本管理纳入公司标准底座。 | 无 `rvt_parse_evidence` / `component_evidence` / `manual_evidence` 时不得判定族库合规。 | BIM-CAD |
| R050 | 历史项目非强制整改 | ALL | current | policy / `catalog_evidence` | 历史 NAS 文件以识别、标注、整改建议为主，不自动重命名、不自动迁移目录。 | 不得把历史文件不符合推荐命名直接判定为平台验收失败。 | delivery-owner |

## 维护规则

- current 规则只能基于稳定 catalog、metadata、权限策略和 Gateway/API 状态。
- current-lineage 规则只能作为历史文件名、目录关键词或脱敏路径线索提示，不等同稳定结构化字段或强制合规判断。
- 专业标准字典可作为 V0.1-beta 的业务标准草案；平台现有 `discipline` 字段和历史文件名别名只是输入线索，不应反向决定公司专业标准。
- `消水`、`弱电` 等跨专业别名在 BIM/CAD 负责人确认前只能作为低置信度线索，不得自动固化为 PLMB/FIRE 或 ELEC/INT。
- R001-R004、R007、R015-R021 可转成受限目录级回答；R005-R006、R008-R014、R022 只能作为 current-lineage 线索回答。
- R023-R027、R029-R030 多数应返回 backlog Missing Evidence 或 Needs Review；R028 仅可解释当前 Preview API 状态，不可解释为内容证据。
- R031-R041 必须返回 future Missing Evidence。
- R042-R043 是标准治理 policy，可用于解释标准来源和编码不变原则；R044-R045 是后续平台/专业扩展能力，不得写成当前已落地。
- R046-R047 是标准执行门禁；R048-R049 是项目绑定和族库治理落地入口；R050 是历史资料处理边界。
- future 规则不得写成当前平台或 Hermes 已支持能力。

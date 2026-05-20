# Standards Changelog

## 2026-05-19 族库专项目录

- 在 DigitalDeliveryProject 根目录下新增数字化交付标准结构化目录。
- 新增 `06 公司族库标准化管理专项/`，覆盖族库分类体系、族命名规则、族参数标准、族制作标准、族审核与发布流程、族版本管理、族库权限与维护机制、族库平台/目录管理。
- 明确族库专项为标准目录建立，不代表平台或 Hermes 已具备族库自动治理、族参数解析或 BIM 构件级检查能力。

## V0.1-beta

- 吸收数据库 Agent 基于真实 NAS 元数据的评审意见。
- 将深圳市建筑工务署数字化交付系列标准设为公司数字化交付标准底座主干，新增工务署标准对齐矩阵。
- 明确已开发标准不另起体系、不废弃，通过对齐矩阵回拉、映射、修订和固化。
- 明确工务署建设阶段和运维阶段分类编码保持不变，公司仅扩展项目实例编码、企业资产编码、成本编码、采购编码、族库编码等企业级编码。
- 新增共享标准同源契约，约束 DigitalDeliveryProject、数据库 / 平台 Agent、Hermes Agent 使用同一套标准包。
- 新增强制对齐工作流，确立“无标准不开发”门禁：平台字段、Hermes 能力、治理规则、项目模板和族库规则必须先回链共享标准。
- 新增项目标准绑定规则，明确新项目默认继承公司基础标准版本，项目差异只能通过 `project_override` / `owner_override` 管理。
- 新增标准包镜像一致性校验脚本 `tools/verify_standard_package_sync.sh`。
- 将早期运维目录归位：标准流程文件进入 `05 平台协同与流程标准/`，本地启动辅助进入 `agent-briefings/`，校验脚本进入 `tools/`，取消旧共享文档时代的独立运维抽屉。
- 在规则矩阵中新增 R046-R050，覆盖镜像一致性、无标准不开发、项目标准绑定、族库治理入口和历史项目非强制整改。
- 调整阶段编码为“工务署项目阶段分类 `43` 主干 + 公司历史短码/目录关键词别名”的结构。
- 补充工务署导则中的三阶段十交付节点、三维模型/文档/数据交付对象、命名字段、交付格式对公司资产分类、命名和 Manifest 路线的影响。
- 明确当前已入库资产规模约 68,904 条，V0.1 优先治理 RVT/BIM 模型、PDF 图纸、DWG 图纸，Office / Excel 清单保留为后续 Manifest 能力。
- 补充标准适用策略：历史存量文件以识别、标注、提示整改为主；新建项目和新交付文件逐步执行推荐命名、目录和 Manifest 标准。
- 补充 `current-lineage` 定义，明确其只是历史目录/文件名/路径关键词线索，不等同稳定字段或强制合规判断。
- 调整阶段编码，增加审图/报审、招标/Tender、蓝图/归档、过程文件、发布文件、BIM模型等真实目录习惯。
- 调整专业编码，增加中文别名、平台字段映射和目录/文件名关键词映射，并说明 `GENERAL` 和空专业不能单独作为专业事实。
- 补充专业字段治理策略：由于当前平台专业字段尚未形成稳定业务使用场景，V0.1-beta 将标准专业编码、中文专业名和中文别名作为公司基础标准草案；现有 `discipline` 仅作为现状输入和映射对象，后续平台字段、人工补录和新项目录入应逐步向标准码表收敛。
- 在平台映射中补充 `discipline_code`、`discipline_name_cn`、`discipline_alias_cn`、`platform_discipline_value`、`discipline_confidence` 等建议字段，并标注为“标准 current / 平台 backlog”。
- 调整 DWG/RVT 命名标准，从单一推荐格式改为推荐格式 + 历史识别 + 外部顾问编码兼容 + 泛名文件标记。
- 明确 RVT 当前无统一公司级命名标准，历史文件以识别、归类、标注为主，`model_数字.rvt` 不作为公司标准样例。
- 调整目录结构标准，兼容“项目 -> 文件流转 -> 日期/批次 -> 专业 -> CAD/PDF/RVT”的真实 NAS 结构。
- 明确 V0.1 优先治理模型、图纸、PDF 出图。
- 明确 CAD 图层标准和 Revit 建模标准需要后续 BIM/CAD 负责人确认。
- 明确三层标准体系：公司基础标准、项目级标准、甲方/业主/地方/顾问标准。
- 补充评审责任：数据库/平台 Agent 评审字段支撑，Hermes Agent 评审回答边界，BIM/CAD 负责人评审专业与 future 标准，项目交付负责人评审交付包与 Manifest。
- 确认 `ModelAssetView` 当前不含稳定 `project_name`，模型侧项目名称需通过 `project_id` / `project_code` 回查；`FileAssetView.project_name` 保持 current。
- 确认 `preview_status` / `preview_mode` / `conversion_status` 已有 REST Preview API / export-precheck 支撑，但未进入稳定 View；`ModelAssetView.preview_available` 仍按 placeholder/backlog 处理。
- 确认 Manifest / DeliveryPackage 结构化字段继续 backlog；当前仅允许把文件名、目录关键词或目录记录中的 Manifest/清单识别为 `current-lineage` 候选线索。
- 将 R005-R006、R008-R014 和 R022 收紧为 `current-lineage`，明确历史文件名/路径关键词只能用于低置信度识别和提示，不作为稳定字段或强制合规判断。
- 统一 `owner_review_needed` 建议枚举为 `no` / `platform` / `Hermes` / `BIM-CAD` / `delivery-owner` / `owner-project` / `architecture`。
- 将 Hermes 边界中 A004-A010 的 `can_answer` 收紧为 `sometimes`，统一要求以“目录/文件名/路径线索显示……”表达，不得写成事实确认。
- 补充 DWG/RVT/BIM/PDF/Office 的 Missing Evidence 模板和 current-lineage 回答模板。
- 补充 Hermes memory 安全边界：`related_file_ids`、`query_id`、`project_id` 仅作低敏引用，跨项目或跨 tenant 使用前必须重新校验权限。
- 补充 Catalog Tool 契约：只通过 Platform Gateway / View Contract，不裸连 DB，不返回 raw catalog row。
- 吸收 Hermes catalog-only、Tool、Memory 边界与 Missing Evidence 规则。
- 明确 Hermes 不直接查底表、不生成 SQL、不读取真实 NAS 文件正文、不写 DB、不写 NAS。
- 明确 Hermes memory 只允许记录 `related_file_ids`、`query_id`、`project_id`、用户确认/反馈标签和低敏偏好，禁止记录 raw `storage_path`、raw catalog row、DWG/RVT 文件内容、正文内容和客户敏感材料。
- 更新 `standard_rule_matrix.md`，新增 `owner_review_needed` 字段。
- 更新 `standard_to_platform_mapping.md`，按 current / backlog / future / placeholder 标注平台字段支撑情况。
- 更新 `standard_to_agent_boundary.md`，按 question_type 映射 Hermes 当前回答边界。

## 2026-05-19

### V0.1-alpha

- 将骨架版扩展为可评审草案。
- 补充资产分类、编码、命名、目录结构、DWG/RVT 目录标准、交付包、Manifest、元数据、权限、生命周期、检查项、证据等级、Hermes 边界。
- 保持 catalog-only / read-only / permission-aware / Missing Evidence / draft-only 主线。
- 明确 DWG 图纸内容理解、RVT/BIM 构件级解析、工程语义搜索、NAS semantic collection 均为 future phase。
- 同步更新 `standard_rule_matrix.md`、`standard_to_platform_mapping.md`、`standard_to_agent_boundary.md`。
- 同步更新 `delivery_manifest_standard.md`、`evidence_level_standard.md`、`naming_standard.md` 的 V0.1-alpha 口径。
- 吸收数据库 / 平台 Agent 评审意见：将 current 收紧为目录级线索 current，将 Manifest 结构化完整性、正式交付包关联、`source_modified_at`、`checksum_status`、`preview_available/preview_status`、字段来源置信度降级为 backlog。
- 明确 `display_path/path_hint` 当前主要依赖 Gateway/API 脱敏方向，稳定 View 字段仍需补充；`component_index_status=NOT_REQUIRED` 不代表构件索引完成。
- 吸收 Hermes Agent 评审意见：将规则矩阵分为可目录级回答、current-lineage 线索回答、backlog Missing Evidence、future Missing Evidence 和标准治理 policy；补充 fail-closed 权限策略、路径不可暴露策略、生命周期历史枚举映射和标准化回复模板。

### V0.1 skeleton

- 创建数字化交付标准 V0.1 草案骨架。
- 建立证据等级标准。
- 建立标准规则矩阵。
- 建立 DWG 与 RVT/BIM 标准路线图。
- 明确 current / backlog / future 分级要求。

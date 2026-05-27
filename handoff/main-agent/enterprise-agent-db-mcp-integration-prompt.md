# 企业 Agent 开发团队数据库/MCP 对接 Prompt

## 使用方式

请将以下 Prompt 原文发送给企业 agent 开发团队。该 Prompt 用于锁定双方边界：卓羽智能数据中台负责项目资产数据库、NAS 扫描、审核、审计、事件流和稳定读模型；企业 agent 团队负责把这些稳定能力封装为 agent 可用的工具能力，优先采用 MCP Server 适配层。

---

# Prompt: 企业 Agent 与卓羽智能数据中台数据库/MCP 对接任务

你是企业内核级 agent 开发团队，负责将企业 agent 接入“卓羽智能数据中台”的项目资产数据能力。请严格按本 Prompt 执行，不要重复开发平台已经承担的资产治理能力，也不要绕过平台后端的权限、审计和审核边界。

## 1. 背景

卓羽智能数据中台当前一期目标是：把公司内部几百个 BIM 项目和约 10TB NAS 文件资产接入平台后端数据库，使企业 agent 能可靠检索项目、模型、图纸、文档、路径、checksum、任务状态和审计事件。

平台当前采用后端数据治理优先路线，前端资产页面不作为当前阻塞项。人工运维与审核当前优先通过 REST API、Swagger/OpenAPI 和脚本完成。

真实 NAS 根路径为：

`/Volumes/zyzn/卓羽智能项目`

平台已经规划并实现/推进以下后端能力：

- 项目台账：`core_projects`
- NAS 路径映射：`data_asset_project_path_mappings`
- 扫描任务：`data_asset_scan_tasks`
- 扫描候选：`data_asset_scan_candidates`
- 正式文件资产：`data_file_resources`
- 导入任务：`data_asset_import_jobs`
- 导入行结果：`data_asset_import_rows`
- 异步任务、checksum、容量统计、事件流：Batch 2 范围
- API Key、agent 项目范围授权、变更申请、删除审批：Batch 3 范围
- 稳定 SQL View：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`

## 2. 总体技术路径

推荐对接链路是：

`企业 agent -> MCP Server -> 平台 SQL View / 平台 REST API -> 后端数据库 / NAS 元数据`

当前一期首轮 DB-2 联调按本机同环境执行：平台数据库运行在本机 Docker MySQL，企业 agent 后续更新到本地 Hermes 后也运行在同一台机器。本机联调使用 `localhost:3306` 的 `delivery_platform` 数据库和专用只读账号 `hermes_agent_ro`。shared-dev / staging 当前不是阻塞项，只有多人远程协作、持续测试、演示或客户交付前类生产验证时再由平台/运维开通。

请注意：

- MCP Server 是企业 agent 的工具适配层。
- MCP Server 不应直接依赖平台业务底表。
- MCP Server 查询数据时优先读取平台稳定 SQL View。
- MCP Server 执行动作时必须调用平台 REST/OpenAPI。
- MCP Server 不得直接写平台数据库正式业务表。
- MCP Server 不得直接移动、删除、改名 NAS 原文件。

当前平台技术路径不是“裸数据库直接给 agent 操作”，而是“稳定读模型 + REST 动作接口 + 事件流 + 审计”。MCP 应建立在这套边界之上。

## 3. 职责边界

### 平台团队负责

- NAS 只读扫描。
- 项目目录发现与项目台账生成。
- NAS 路径映射维护。
- 文件类型识别与候选记录生成。
- 高置信自动入库。
- 低置信候选人工审核。
- 正式资产库维护。
- checksum 异步补齐。
- 容量统计。
- 审计日志与事件流。
- API Key 与项目范围授权。
- 删除申请、人工审批、隔离、恢复、永久删除。
- SQL View 与 REST/OpenAPI 契约稳定。

### 企业 Agent 团队负责

- 实现 MCP Server 或等价工具适配层。
- 将平台 SQL View 封装成只读查询 tools。
- 将平台 REST/OpenAPI 封装成动作 tools。
- 维护 agent 自身的工具描述、参数校验、结果摘要和错误处理。
- 必要时在 agent 侧建立搜索索引或向量索引，但索引源必须来自平台稳定读模型、事件流或授权 API。
- 与平台团队确认 API Key、项目授权范围、事件同步方式和路径访问方式。

### 禁止重复开发

请不要重复实现以下能力：

- 不要重新写一套 NAS 扫描器作为正式数据源。
- 不要重新建项目资产数据库。
- 不要绕过 `data_asset_scan_candidates` 和审核队列做正式入库。
- 不要直接操作 `data_file_resources` 等正式业务表。
- 不要直接解析平台底表结构并耦合字段。
- 不要绕过平台 API Key、项目权限和审计机制。
- 不要实现独立删除 NAS 文件的能力。
- 不要把业务变更只记录在 agent 自己的库里，正式变更必须回到平台。

## 4. 数据读取协议

一期数据读取优先使用 SQL View。企业 agent 可以通过只读数据库账号读取以下视图。

### `ProjectAssetView`

用途：项目级资产检索与项目总览。

关键字段：

- `project_id`
- `project_code`
- `project_name`
- `project_stage`
- `discipline_scope`
- `manager_name`
- `owner_org_name`
- `asset_status`
- `model_file_count`
- `total_size_bytes`
- `last_asset_updated_at`

典型问题：

- 查询某项目是否已接入。
- 查询某项目资产规模。
- 查询最近更新的项目。
- 按项目名称、编码、阶段、业主筛选项目。

### `FileAssetView`

用途：文件级资产检索，是企业 agent 最核心的检索入口。

关键字段：

- `file_id`
- `project_id`
- `project_code`
- `project_name`
- `file_name`
- `file_ext`
- `file_kind`
- `discipline`
- `version_no`
- `size_bytes`
- `checksum`
- `storage_provider`
- `storage_path`
- `logical_path`
- `source_type`
- `process_status`
- `created_at`
- `updated_at`

典型问题：

- 某项目有哪些模型、图纸、文档、汇报文件。
- 查找某个文件名或扩展名。
- 查找某个专业的文件。
- 查询 NAS 路径。
- 判断文件是否已登记、是否有 checksum。

### `ModelAssetView`

用途：模型文件与模型集成相关查询。

当前要求：该视图必须同时覆盖已进入模型集成流程的模型，以及一期通过 NAS 扫描登记但尚未进入模型集成流程的模型文件。企业 agent 不需要再退回 `FileAssetView WHERE file_kind='MODEL'` 才能查到 NAS 模型，但仍可用 `FileAssetView` 查询完整文件清单。

关键字段：

- `model_id`
- `file_id`
- `project_code`
- `model_name`
- `model_format`
- `discipline`
- `version_no`
- `preview_available`
- `lightweight_status`
- `component_index_status`
- `storage_path`
- `updated_at`

典型问题：

- 某项目有哪些模型。
- 模型格式是什么。
- 模型是否已有轻量化预览。
- 模型是否已有构件索引。

### `AuditEventView`

用途：增量同步、审计追踪和事件回放。

关键字段：

- `event_id`
- `project_id`
- `module_code`
- `action_code`
- `target_type`
- `target_id`
- `operator_id`
- `summary`
- `created_at`

典型问题：

- 从上次同步点之后发生了哪些变化。
- 某项目最近有哪些扫描、导入、审核、checksum、删除申请事件。
- 某个文件是谁审核或修改的。

## 5. REST/OpenAPI 工具调用协议

企业 agent 执行动作必须通过平台 REST/OpenAPI。后续 Batch 3 会正式固化 API Key 与 agent 授权。

MCP Server 可把以下平台 API 能力包装成 tools：

- 查询项目资产。
- 查询文件资产。
- 查询模型资产。
- 搜索文件元数据。
- 查询 NAS 扫描任务。
- 触发 NAS 扫描任务。
- 查询 checksum 任务。
- 触发 checksum 或重试 checksum。
- 查询容量统计。
- 查询审计/事件流。
- 提交候选标注申请。
- 提交删除申请。
- 查询删除申请状态。

所有写动作必须满足：

- 使用平台发放的 API Key。
- API Key 必须绑定 agent 身份和项目范围。
- 请求必须进入平台审计。
- 正式数据变更必须人工审核。
- 删除类动作只允许提交申请，不允许直接删除。

## 6. 建议 MCP Tools

请优先实现以下 MCP tools。工具名可以按你们规范调整，但语义不要偏离。

### `search_project_assets`

用途：查询项目资产概览。

输入：

- `keyword`：项目名、项目编码、业主或负责人关键词，可选。
- `project_stage`：项目阶段，可选。
- `limit`：返回条数，默认 20。

数据源：

- `ProjectAssetView`

输出：

- 项目编码、项目名称、阶段、负责人、业主、模型数量、资产总大小、最近更新时间。

### `search_file_assets`

用途：查询文件资产。

输入：

- `project_code`：项目编码，可选。
- `keyword`：文件名或路径关键词，可选。
- `file_kind`：`MODEL`、`DRAWING`、`DOCUMENT`、`SPREADSHEET`、`PRESENTATION`、`ARCHIVE`、`OTHER`，可选。
- `file_ext`：扩展名，可选。
- `discipline`：专业，可选。
- `updated_after`：增量筛选时间，可选。
- `limit`：返回条数，默认 50。

数据源：

- `FileAssetView`

输出：

- 文件 ID、项目编码、文件名、类型、扩展名、专业、版本、大小、checksum、NAS 路径、更新时间。

### `get_model_assets`

用途：查询模型资产。

输入：

- `project_code`：项目编码，建议必填。
- `discipline`：专业，可选。
- `model_format`：模型格式，可选。

数据源：

- `ModelAssetView`

输出：

- 模型 ID、文件 ID、模型名、格式、专业、版本、轻量化状态、构件索引状态、路径。

### `get_asset_events`

用途：拉取增量事件。

输入：

- `after_event_id`：上次同步到的事件 ID，可选。
- `created_after`：时间窗口起点，可选。
- `project_id`：项目 ID，可选。
- `module_code`：模块，可选。
- `limit`：默认 100。

数据源：

- `AuditEventView` 或平台事件流 API。

输出：

- 事件 ID、项目 ID、模块、动作、目标类型、目标 ID、摘要、发生时间。

### `trigger_nas_scan`

用途：触发平台 NAS 扫描任务。

输入：

- `root_code`
- `root_path`
- `project_id` 或 `project_code`
- `recursive`
- `extensions`

动作方式：

- 调用平台 REST/OpenAPI。

限制：

- agent 只能触发授权项目范围内的扫描。
- 扫描器必须只读 NAS。
- 扫描结果先进入平台候选/正式资产链路。

### `trigger_checksum`

用途：触发 checksum 计算或重试。

输入：

- `file_id` 或 `project_id`
- `force_recheck`

动作方式：

- 调用平台 REST/OpenAPI。

限制：

- checksum 是平台异步任务，不应由 agent 自己扫描大文件后写库。

### `submit_asset_annotation`

用途：提交文件归属、类型、专业、版本修正建议。

输入：

- `file_id` 或 `candidate_id`
- `suggested_project_id`
- `suggested_file_kind`
- `suggested_discipline`
- `suggested_version_no`
- `reason`

动作方式：

- 调用平台 REST/OpenAPI。

限制：

- 该 tool 只能提交申请或候选修正，正式变更由平台审核。

### `submit_delete_request`

用途：提交删除申请。

输入：

- `file_id`
- `reason`

动作方式：

- 调用平台 REST/OpenAPI。

限制：

- agent 不允许直接删除 NAS 文件。
- 删除必须经过人工审核、隔离、恢复期和永久删除任务。

## 7. 推荐 MCP 架构

建议企业 agent 团队单独实现一个 MCP Server，职责是协议适配，不承载平台业务治理。

推荐结构：

- `dbClient`：只读连接 MySQL View。
- `platformApiClient`：调用平台 REST/OpenAPI。
- `authContext`：保存 agent API Key、项目授权范围和调用 trace。
- `tools/projectAssets`：项目检索 tools。
- `tools/fileAssets`：文件检索 tools。
- `tools/modelAssets`：模型检索 tools。
- `tools/events`：事件流 tools。
- `tools/tasks`：扫描/checksum 任务 tools。
- `tools/requests`：标注申请/删除申请 tools。

MCP Server 不应包含：

- NAS 文件扫描业务。
- 正式资产入库 SQL。
- 审核逻辑。
- 删除 NAS 文件逻辑。
- 直接写业务底表逻辑。

## 8. 权限与安全要求

- 数据库连接必须使用只读账号。
- 只读账号只允许读稳定 SQL View，默认不开放业务底表权限。
- REST 动作必须使用平台 API Key。
- API Key 必须按项目范围授权。
- 所有 tool 调用必须记录 trace。
- 所有写动作必须能在平台审计中查到。
- 不允许 agent 绕过平台调用 NAS 删除、移动、改名。
- 一期内部可返回真实 NAS 路径，但必须确认企业 agent 是否有读取该路径的系统权限。
- 客户版后续默认不直接暴露底层路径，应改为平台授权下载或访问入口。

## 9. 增量同步策略

不要让 agent 每次全量扫描全部项目文件。推荐策略：

1. 首次同步读取 `ProjectAssetView`、`FileAssetView`、`ModelAssetView` 全量快照。
2. 记录当前最大 `event_id` 或同步时间。
3. 后续定时读取 `AuditEventView` 或事件流 API。
4. 根据事件中的 `target_type`、`target_id` 回查对应 View 或 REST API。
5. checksum 变化、审核状态变化、扫描完成、删除申请、隔离恢复等都应作为重要事件处理。

## 10. 当前不要做的事情

为了避免重复开发和后续路线摇摆，请企业 agent 团队当前不要做：

- 不要重新设计项目/文件/模型资产主库。
- 不要直接扫描 `/Volumes/zyzn/卓羽智能项目` 作为正式数据源。
- 不要把 agent 自己的索引结果反向当作平台正式资产。
- 不要实现自己的文件删除审批。
- 不要绕过平台后端直接更新项目、文件、扫描任务、审核记录。
- 不要依赖 `data_*` 业务底表字段作为长期协议。
- 不要提前做 PDF/Office 正文解析作为一期阻塞项，除非平台主 agent 明确裁决。
- 不要提前做 BIM 构件级解析作为一期阻塞项。

## 11. 需要企业 Agent 团队反馈的问题

请企业 agent 团队在开工前反馈以下问题，后续由数据库/平台对接负责人继续对齐：

1. 你们的 agent 运行环境是什么，是否原生支持 MCP Server？
2. 你们是否确认首轮部署在本地 Hermes，与平台数据库同机运行？
3. 你们是否接受只读 MySQL View 作为一期主要数据源？
4. 你们是否需要平台提供同步库、搜索引擎索引或向量库，而不是直接读 MySQL？
5. 你们的增量同步倾向是平台事件流拉取、平台主动推送，还是 agent 定时拉取？
6. 你们是否需要读取真实 NAS 路径？agent 运行环境是否能访问该 NAS？
7. 你们是否需要平台提供文件下载 API，而不是直接打开 NAS 路径？
8. 你们一期是否需要 PDF/Office 正文检索？
9. 你们一期是否需要 BIM 构件属性检索？
10. 你们的 API Key 管理、安全审计和调用日志有什么要求？

## 12. 交付物要求

请企业 agent 团队提交以下交付物：

- MCP Server 技术方案。
- Tool 清单、输入参数、输出字段和错误码。
- 数据源说明：每个 tool 读哪个 SQL View 或调哪个 REST API。
- 权限设计：API Key、项目范围、只读数据库账号。
- 增量同步设计：同步点、事件处理、失败重试。
- 不重复开发说明：明确哪些能力复用平台，哪些能力由 agent 侧实现。
- 联调计划：至少覆盖项目查询、文件查询、模型查询、事件同步、扫描触发、checksum 触发、标注申请、删除申请。
- 风险清单：路径权限、数据量、事件延迟、字段变更、agent 越权、NAS 不可达。

## 13. 联调验收标准

联调通过标准：

- agent 能通过 MCP tool 查询项目资产。
- agent 能通过 MCP tool 查询文件资产，并返回 NAS 路径或平台访问入口。
- agent 能查询模型资产。
- agent 能按事件增量同步变化。
- agent 能触发授权范围内的扫描或 checksum 任务。
- agent 能提交标注/删除申请，但不能直接修改正式资产或删除 NAS 文件。
- 未授权项目不可查、不可触发任务、不可提交申请。
- 平台审计中能查到 agent 身份、动作、项目范围、目标对象和结果。
- agent 不依赖平台业务底表，只依赖 SQL View、REST/OpenAPI 和事件流协议。

## 14. 当前推荐结论

企业 agent 对接不要推翻平台现有路线。正确做法是：

`平台继续沉淀资产数据库与治理流程；企业 agent 团队实现 MCP Server，把平台 SQL View 与 REST/OpenAPI 包装成 agent tools。`

这样既能让 agent 很快具备项目资产检索能力，也能避免重复开发 NAS 扫描、资产入库、审核、审计和删除安全能力。

---

## 平台侧联系人/后续对接方式

后续数据库视图、字段含义、事件流、API Key、MCP tool 边界和验收口径，由平台侧数据库/agent 对接负责人继续与企业 agent 团队确认。任何涉及平台数据库表结构、正式资产变更、NAS 文件操作、删除审批和权限边界的调整，必须先回到平台主 agent 做裁决，不允许企业 agent 团队单方面改变对接路径。

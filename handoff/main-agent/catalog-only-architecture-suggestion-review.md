# Catalog-only Architecture Suggestion Review

日期：2026-05-18

来源：

- `/Users/vc/Downloads/catalog_only_architecture_pr_review_prompt.md`

定位：

- 本文件是外部架构建议评审，不是开发指令。
- 本轮不修改底表、SQL View、Flyway 迁移、API 或前端实现。
- 当前主线仍保持 `catalog-only / read-only / permission-aware / Missing Evidence / draft-only`。

## Overall Verdict

当前项目整体判断：`Green`

是否需要刹停：不需要。

是否需要修改底层架构：不需要。只建议小幅补契约说明与 backlog，不建议在当前批次改底层表、稳定 View 或 Hermes/Jarvis 接入方式。

核心结论：

- 当前稳定读模型已经符合 catalog-only 主线：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均已存在，且 V17 已补齐 `permission_tags`、`confidentiality_level`、`last_seen_at`、`lifecycle_status`、`index_eligibility` 等治理字段。
- 当前 Jarvis/Hermes 网关已经按只读目录助手接入，支持 Missing Evidence、权限证明、草案型操作建议和审计，不允许 DB CRUD、NAS CRUD、正文证据伪造或生产级自动动作。
- PR 中大部分建议与当前主线一致，适合转成“契约口径”和“后续 backlog”，不适合现在直接改架构。

## Current Architecture Health

健康项：

- `FileAssetView` 基于 `data_file_resources` 作为文件资产事实表，适合当前 NAS 元数据治理主线。
- `ModelAssetView` 已通过 V13/V17 覆盖 NAS 扫描登记的模型文件，不再只依赖模型集成记录。
- `index_eligibility` 当前稳定为 `catalog_only`，避免把目录资产误当正文或语义索引。
- `AgentAssetContextResolver` 只允许从四个稳定 View 建立上下文。
- `AgentGatewayApplicationService` 已在出站请求中明确：
  - `mode = catalog_only`
  - `must_not_expose_true_nas_path = true`
  - `must_not_use_catalog_as_content_evidence = true`
  - `must_not_execute_actions = true`
- `AgentPermissionProofService` 只允许 `catalog_query`、`agent_catalog_assist`、`operation_plan_draft` 这类安全动作。

需要注意的风险：

- SQL View 层仍暴露 `storage_path`，本机内部只读联调可接受，但 shared-dev、staging 和客户环境默认不应直接暴露真实 NAS 路径。
- `updated_at` 目前是资产记录更新时间，不等于 NAS 文件本体 mtime；Jarvis 回答中不应把它说成“文件修改时间”。
- `checksum = null` 现在无法从 View 直接区分 pending / failed / skipped，只能理解为“暂不可用或尚未计算完成”。
- 当前还没有 query feedback 模型，平台审计能记录 Jarvis 查询行为，但还不是完整的搜索反馈闭环。

## Suggestion-by-Suggestion Review

### 1. Hermes 只依赖稳定 View / API，而不是底表

结论：已满足主线，建议补充契约表述。

原因：

- 当前平台对 Hermes/Jarvis 的上下文解析只允许 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
- 前端与平台后端通过 REST/Gateway 接入，不需要 Hermes 直接读取业务底表。
- 平台内部 repository 仍会读取 `data_file_resources`，这是业务系统自身实现细节，不等于 Hermes 对外耦合底表。

是否需要代码或文档调整：

- 当前不需要改代码。
- 建议在后续 Agent 对接文档中明确：Hermes 外部集成只能依赖稳定 View 或只读 API，不能依赖业务底表字段。

### 2. `file_id` 是否作为后续统一关联键

结论：建议采纳并文档化。

file_id 稳定性说明：

- 当前 `file_id` 对应 `data_file_resources.id` 和 `FileAssetView.file_id`。
- checksum 补齐、版本号补录、专业补录、状态更新等元数据更新不会改变 `file_id`。
- 同项目同 `storage_uri` 的重扫按现有幂等规则更新同一资产记录，不应重复插入。
- 如果未来发生真实物理移动并导致 `storage_uri` 改变，可能产生新资产记录；这时应把它视为新的资产生命周期或通过后续迁移/移动记录建立关系。

是否建议文档化：建议。

是否需要改代码：当前不需要。

额外裁决：

- 后续跨模块关联、Jarvis 查询结果、用户反馈、查询日志、预览回指、未来索引回指都应优先使用 `file_id`。
- 不建议使用 `storage_path` 作为跨模块主关联键。

### 3. `storage_uri / storage_path` 是否需要默认受控暴露

结论：当前 API/UI 已部分满足，View 层仍有受控风险，建议进入 backlog。

当前路径暴露策略：

- DB-2 本机内部联调允许只读账号读取真实路径，用于字段握手和资产定位。
- 平台 API/UI 已开始隐藏或转换真实 NAS 路径，例如目录、导出、任务失败原因等对用户侧展示走项目内路径或脱敏文本。
- Jarvis 网关出站和返回内容均有路径脱敏要求和字符串清洗。

是否存在安全风险：

- 有中等风险。`FileAssetView.storage_path` 和 `ModelAssetView.storage_path` 仍是原始 `storage_uri`，只适合受控内部只读环境。
- shared-dev、staging、客户环境默认不应把该字段直接进入 Agent memory、向量库、外部日志或客户材料。

建议进入当前阶段 / backlog / future：

- 当前阶段不改 View。
- backlog：增加 `display_path`、`path_hint`、`masked_storage_path` 或 API 层 `include_sensitive_path=false` 语义。
- 客户环境：默认只给授权访问入口或脱敏路径。

### 4. `updated_at / last_seen_at / mtime` 语义区分

结论：建议采纳为契约说明，字段补充进入 backlog。

当前字段语义：

- `updated_at`：资产记录更新时间。
- `last_seen_at`：当前 View 中通过 `last_verified_at` 和 `updated_at` 派生，表达最近被平台扫描或看到的时间。
- `source_modified_at / file_modified_at`：当前正式资产 View 没有稳定字段。

是否建议补字段：

- 建议后续补 `source_modified_at` 或 `file_modified_at`，用于表达 NAS 文件本体最后修改时间。
- 当前不建议为此修改主线 View，避免打断 6A。

是否影响一期 catalog-only：不影响。

风险等级：中。

Jarvis 口径要求：

- 当前回答中不要说“文件最后修改时间”，应说“资产记录更新时间”或“最近扫描/校验时间”。

### 5. checksum 异步补齐状态语义

结论：建议进入 backlog。

checksum 当前语义：

- `checksum` 有值：已经计算出文件摘要。
- `checksum = null`：只能说明当前不可用，可能是尚未计算、任务失败、被跳过或文件不可访问。
- 当前可通过任务表和事件流追踪具体失败原因，但稳定 View 本身没有 `checksum_status`。

是否建议增加状态字段：

- 建议后续在 API 或 View 层补 `checksum_status`，取值可为 `pending / available / failed / skipped`。
- 当前不作为 6A 或 catalog-only 接入阻塞项。

是否进入 backlog：是。

### 6. `process_status / component_index_status` 不等同 semantic index status

结论：已满足主线，建议补文档口径。

当前状态字段语义：

- `process_status`：平台文件处理/治理状态，不代表向量索引状态。
- `component_index_status`：BIM 构件索引占位，当前为 `NOT_REQUIRED`。
- `index_eligibility = catalog_only`：当前只允许资产目录级辅助。
- 当前没有 `semantic_index_status`、`semantic_index_version`、`semantic_indexed_at`、`embedding_model`。

是否有误用风险：

- 有低到中风险。后续开发 agent 或企业 agent 容易把 `process_status` 理解为“已经索引”。

是否建议文档化：建议。

是否进入 future phase：是。语义索引字段应等进入 selective indexing / semantic index 阶段再设计。

### 7. DWG/RVT 能力边界是否暴露给 Hermes / 前端

结论：当前边界基本满足，建议后续补 capability 字段。

当前能力边界表达方式：

- Jarvis/Hermes capabilities 已表达：
  - `catalogQuery = true`
  - `documentContentAnswer = false`
  - `dbCrud = false`
  - `nasCrud = false`
  - `fullBimParse = false`
  - `productionRollout = false`
- `ModelAssetView.preview_available = 0`，`lightweight_status = NOT_REQUIRED`，`component_index_status = NOT_REQUIRED`。
- 当前前端和网关对正文类问题返回 Missing Evidence。

是否建议增加 capability 信息：

- 建议后续在 catalog 文件响应或 Jarvis 上下文中补更细颗粒度能力标记：
  - `catalog_search`
  - `preview_available`
  - `full_text_search`
  - `semantic_search`
  - `component_search`

是否影响一期：不影响。

当前注意：

- DWG/RVT 当前只能作为资产目录对象被检索，不代表平台已经理解图纸内容、模型构件或工程语义。

### 8. 当前是否应避免新增生产级 NAS 向量库

结论：已满足，继续禁止。

当前是否符合 catalog-only：符合。

是否存在越界开发：

- 当前代码和文档没有发现写 OpenSearch、Qdrant、MinIO、长期 memory 或生产级 NAS 向量库的授权。
- decisions 已明确本阶段不写搜索引擎、不写向量库、不写长期 memory。

是否建议补 decisions：

- 当前 decisions 已足够。
- 可在后续路线中补一句：未来如做 NAS semantic collection，也必须是独立、可审计、可脱敏、可撤销的受控索引，不得混入 Hermes 长期 memory。

### 9. Hermes 当前定位为 Catalog Query Agent，而非 Engineering Semantic Agent

结论：已满足，继续保持。

Hermes 当前能力定位：

- 当前应称为 `Catalog Query Agent / 贾维斯数据管家资产目录助手`。
- 可回答资产目录、项目范围、文件类型、专业、状态、缺证据、权限证明等问题。
- 不应承诺 DWG 图纸内容理解、RVT 构件理解、工程语义搜索或构件级搜索。

是否需要调整 prompt / tool description：

- 当前后端网关已经严格要求 catalog-only 和 Missing Evidence。
- 后续如果企业 Agent prompt 里出现“读图纸内容 / 理解模型构件”之类描述，应立即修正。

是否需要调整前端文案：

- 当前不阻塞。
- 后续可继续强化“资产目录辅助，非正文问答”的文案，避免客户误解。

### 10. 查询日志与用户反馈闭环，但不写长期 memory

结论：建议进入 backlog，不影响当前主线。

当前日志能力：

- 平台已有 Jarvis 审计，记录 requestId、userRef、projectRef、sourceView、evidenceMode、permissionStatus、status、latencyMillis、pageType、assetRef、questionLength、contentQuestion。
- 这已经满足只读网关的安全审计要求。

是否建议预留 query_id：

- 建议。未来可以补一个独立的 catalog query feedback 模型，包含：
  - `query_id`
  - `user_query`
  - `filters`
  - `returned_file_ids`
  - `selected_file_id`
  - `feedback`
  - `created_at`

是否建议进入 backlog：是。

是否影响当前主线：不影响。

注意：

- 查询日志和用户反馈不是长期 memory，也不意味着可以把真实项目名、文件名、路径或正文写入向量库/外部日志。

## A. 总体判断

```text
当前项目是否健康：Green
是否需要刹停：不需要
是否需要修改底层架构：不需要，小幅补契约和 backlog 即可
```

## B. 可立即采纳但不改底层的建议

1. 明确 Hermes/Jarvis 外部接入只能依赖稳定 View 或只读 API，不读取业务底表。
2. 明确 `file_id` 是后续跨模块、查询结果、反馈、预览、未来索引回指的主关联键。
3. 明确 `storage_path` 只允许在受控内部只读环境出现，客户环境默认隐藏真实路径。
4. 明确 `updated_at` 是资产记录更新时间，不是文件本体 mtime。
5. 明确 `checksum = null` 不能解释为文件无变化，只能解释为 checksum 暂不可用。
6. 明确 `process_status`、`component_index_status` 和未来 `semantic_index_status` 是三套不同概念。
7. 明确 DWG/RVT 当前只做目录级资产检索，不做工程语义理解。

## C. 可以进入 Backlog 的建议

1. 增加 `display_path / path_hint / masked_storage_path` 或 API 层 `include_sensitive_path=false`。
2. 增加 `source_modified_at / file_modified_at`，区分文件本体 mtime 与资产记录更新时间。
3. 增加 `checksum_status`，区分 `pending / available / failed / skipped`。
4. 增加文件能力标记：`catalog_search / preview_available / full_text_search / semantic_search / component_search`。
5. 增加 catalog query feedback 模型，用于记录 query、返回文件、用户选择和反馈，但不得写长期 memory。
6. 未来进入 semantic 阶段时单独设计 `semantic_index_status / semantic_index_version / semantic_indexed_at / embedding_model`。

## D. 不建议当前采纳的建议

1. 不建议现在重构 `FileAssetView` / `ModelAssetView` 或改 Flyway 迁移。
2. 不建议现在新增生产级 NAS 向量库。
3. 不建议把 DWG/RVT 当普通文本 chunk 写入 Hermes memory。
4. 不建议把 catalog metadata 当文件正文 evidence。
5. 不建议当前让 Hermes 直接读取或持久化真实 NAS 文件正文。
6. 不建议当前让 Agent 触发真实 NAS 写操作、自动审批、自动删除或自动修复。

## E. 风险提示

| 风险 | 当前等级 | 判断 |
|---|---:|---|
| NAS 内容写入 Hermes memory | 低 | 当前 decisions 和网关均禁止，但后续开发必须继续守住 |
| DWG/RVT 被当普通文本 chunk | 低 | 当前无正文抽取和向量化，需在后续 selective indexing 前继续提醒 |
| `updated_at` 被当 mtime | 中 | View 目前无文件本体 mtime，Jarvis 口径必须避免误说 |
| `process_status` 被当 semantic index status | 中 | 当前无 semantic 字段，应在后续文档/prompt 里明确区分 |
| 真实路径暴露给 Agent / 客户 | 中 | DB View 仍有 `storage_path`，API/UI/Jarvis 已做隐藏；客户环境需要默认脱敏 |
| Hermes 过度承诺工程语义能力 | 低到中 | 当前 Missing Evidence 已控制，后续前端文案和 Agent prompt 需持续约束 |

## Main Agent Decision

本 PR 建议整体方向正确，但不改变当前执行顺序。

近期仍继续推进：

1. `批次 6A：项目初始化评估 + 建筑机电/BIM交付基础模板预览/套用`
2. 文件预览转换与批量交付增强
3. BIM 轻量化适配层
4. 构件级解析、搜索与交付联动

Jarvis/Hermes 仍保持：

- 当前是资产目录助手，不是工程语义 Agent。
- 当前是只读网关，不是自动治理执行器。
- 当前是 catalog-only，不是正文知识库。

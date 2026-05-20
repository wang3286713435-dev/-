# 数据库 / 平台 Agent 风险提醒评审

日期：2026-05-18

来源：

- `/Users/vc/Downloads/database_platform_agent_risk_notes.md`

## 结论

建议采纳。

该文件不是重构指令，也不要求当前修改底表、Flyway migration、SQL View 或核心 API。它与当前项目已经形成的 `catalog-only / read-only / permission-aware / Missing Evidence / draft-only` 主线一致，应作为后续 Hermes / 贾维斯、数据库只读视图、资产目录查询和未来 selective indexing 的安全边界。

当前主线不需要刹停，不需要调整底层架构。

## 风险采纳判断

| 风险 | 是否值得关注 | 优先级 | 当前判断 |
| --- | --- | --- | --- |
| catalog metadata 被误当正文 evidence | 是 | P0 guardrail | 当前 Gateway 和前端已基本控制，但后续任何 Jarvis 回答、前端文案、客户演示都必须继续坚持 Missing Evidence。 |
| 真实 NAS 路径泄露 | 是 | P0/P1 | 当前 catalog/search 和 Jarvis 侧不返回真实路径；SQL View 仍有 storage_path，只适合本机/受控只读联调。客户环境必须默认脱敏。 |
| updated_at / last_seen_at 被当 NAS 文件 mtime | 是 | P1 | 当前没有稳定 file_modified_at/source_modified_at。后续回答和 UI 只能说“资产记录更新时间”或“最近扫描/校验时间”。 |
| process_status / component_index_status 被当 semantic index | 是 | P1 | 当前应继续展示“目录可查、正文不可查、语义搜索未开放、构件搜索未开放”。 |
| checksum=null 被误解 | 是 | P1 | 当前 checksum=null 只能表示暂不可用，不能推出文件无变化或一定异常。checksum_status 已在 backlog。 |
| query feedback 变成长期 memory 或敏感日志 | 是 | P1 | 后续做查询反馈时必须和 Hermes long-term memory、向量库、外部日志分离。 |
| selective indexing 与 catalog-only 混线 | 是 | Phase gate | 未来进入全文、语义、构件索引前必须单独设计索引分层、审计、脱敏、撤销策略。 |
| 前端/客户材料过度承诺 DWG/RVT 能力 | 是 | P1 | 当前只能称“资产目录助手/项目文件资产检索”，不能宣称已读懂图纸或 BIM 模型。 |

## 当前已覆盖

- `AgentGatewayApplicationService` 已向 Hermes 发送安全约束：
  - `mode = catalog_only`
  - `must_not_expose_true_nas_path = true`
  - `must_not_use_catalog_as_content_evidence = true`
  - `must_not_execute_actions = true`
- `CatalogApplicationService.searchCatalog` 当前只返回目录预览字段，不返回 `storage_path / storage_uri`。
- 前端 `DataStewardPanel` 已明确展示“仅展示目录元数据，不代表已读取文件正文”和“无正文证据”。
- `handoff/main-agent/backlog.md` 已记录 `display_path / path_hint / masked_storage_path`、`source_modified_at / file_modified_at`、`checksum_status`、能力标记和 query feedback 等后续补强项。

## 仍需守住的缺口

1. SQL View 层仍暴露 `storage_path`，仅允许在本机/内部受控只读环境使用。
2. 当前没有稳定的 NAS 文件本体修改时间字段。
3. 当前没有稳定的 `checksum_status` 字段。
4. 当前没有正式 semantic / full-text / component index 状态体系。
5. 后续客户演示和前端文案必须避免“AI 读懂图纸 / 模型理解 / BIM 构件问答”等过度承诺。

## 后续执行要求

后续所有 Hermes / 贾维斯 / selective indexing / 文件预览增强相关开发 prompt，必须继续写明：

- 不把 catalog metadata 当正文证据。
- 不默认返回真实 NAS 路径。
- 不让模型生成 SQL 或直接访问业务底表。
- 不写长期 memory、向量库或搜索引擎，除非进入单独批准的索引批次。
- 不把 DWG/RVT 当普通文本 chunk。
- 不承诺当前已具备文件正文理解、图纸理解、BIM 构件理解能力。

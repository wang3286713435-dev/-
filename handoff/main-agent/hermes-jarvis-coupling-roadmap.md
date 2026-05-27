# Hermes / 贾维斯数据管家耦合路线

更新时间：2026-05-18

## 1. 来源

本路线基于用户提供的内核 Hermes 耦合文档：

`/Users/vc/Downloads/HERMES_DATA_STEWARD_PLATFORM_COUPLING_GUIDE.md`

并已吸收 V3 增量联调文档：

`/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`

这些文档定位为：平台内嵌企业 Agent / Hermes 数据管家接入方案，不是 production rollout 授权。

## 2. 产品定位

对外和平台前端口径：

`贾维斯数据管家`

技术内核口径：

`Hermes Memory / Hermes Data Steward`

二期客户交付版中，贾维斯应作为平台最重要的产品亮点之一，但必须先以安全、只读、权限感知的方式进入平台，不得因为追求亮点而突破权限、审计和文件安全红线。

## 3. 当前 Hermes 状态

Hermes 当前主线阶段已由旧的 2.86 口径更新为 V3 口径：

`phase-2.89-test-machine-runtime-preflight-handoff-baseline`

已完成：

- DB / NAS asset catalog v1.1 合同对齐。
- 真实 DB v1.1 structure-only smoke。
- v1.1 LIMIT 30 脱敏统计 smoke。
- 只读 adapter / DTO / fake adapter / contract tests 对齐 v1.1 字段形态。
- Catalog query preview 已实现为资产目录 metadata preview。
- Phase 2.88 runtime preflight runner 已具备测试机交接能力。
- 测试机可进入 `preflight_ready_for_operator_stop`。

未完成或未授权：

- 未授权真实写入 Hermes `documents`、`chunks`、`document_versions`。
- 未写 OpenSearch。
- 未写 Qdrant。
- 未写 MinIO。
- 未开放基于 NAS 正文的正式问答。
- 未开放 Agent DB CRUD。
- 未开放 Agent NAS CRUD。
- 未进入 production rollout。
- 未授权 parser、NAS copy、index write 或 Agent answer integration 进入生产前端。

## 4. 当前平台已有基础

当前卓羽智能数据中台已经具备并通过 5A 验收的基础接入能力：

- 后端已有 `AgentGatewayController`：
  - `GET /api/agent/hermes/capabilities`
  - `POST /api/agent/hermes/chat`
- 后端已有：
  - `AgentGatewayApplicationService`
  - `AgentPermissionProofService`
  - `AgentAssetContextResolver`
  - `HermesAgentClient`
  - `HermesGatewayProperties`
- 配置已存在：
  - `HERMES_AGENT_GATEWAY_ENABLED`
  - `HERMES_MEMORY_BASE_URL`
  - `HERMES_AGENT_GATEWAY_READONLY`
  - `HERMES_AGENT_GATEWAY_CATALOG_ONLY_DEFAULT`
  - `HERMES_AGENT_GATEWAY_SERVICE_TOKEN`
- 稳定读模型已升级到 asset views v1.1：
  - `ProjectAssetView`
  - `FileAssetView`
  - `ModelAssetView`
  - `AuditEventView`
- 前端已有组件：
  - `DataStewardPanel`
  - `DataStewardAnswerCard`
  - `EvidenceModeBadge`
  - `MissingEvidenceNotice`
  - `OperationPlanPreview`
- 前端已有入口：
  - 项目详情页 `问贾维斯`
  - 文件详情 / 资产目录详情 `问贾维斯`
- 5A 已通过测试 agent 专项复验：
  - capabilities 返回 `贾维斯数据管家` 和 catalog-only 能力。
  - 正文类问题返回 Missing Evidence。
  - 无效项目范围 fail closed。
  - Hermes 不可用返回 `agent_unavailable`。
  - 审计已落到 `agent.jarvis.chat.*`。
  - 4R 文件访问安全闭环未回归。
- 5A.1 已通过测试 agent 专项复验：
  - `GET /api/data-steward/hermes/health` 可用。
  - `POST /api/data-steward/chat` 可用。
  - 前端已展示健康状态、只读网关模式、运行时写入未开放、正式正文回答未开放。
  - Hermes 不可用时 health 与 chat 安全降级，不返回 500。
  - 平台侧审计可关联 `pageType / requestId / projectRef / sourceView / evidenceMode / permissionStatus`。

## 5. 当前接入边界

近期允许：

- 平台前端嵌入 `贾维斯数据管家` 入口。
- 平台后端作为唯一 Agent Gateway。
- Gateway 生成当前用户、当前项目、当前页面、当前资产和 `project_scope` 权限证明。
- Hermes 基于 asset catalog / metadata 做辅助问答。
- 展示 `catalog_only`、`missing_evidence`、`denied`、`operation_plan_draft`。
- 操作建议只能是草案，并明确需要人工审批。

近期禁止：

- Agent 直接执行数据库增删改。
- Agent 直接扫描、移动、删除、修改 NAS 文件。
- Agent 全量解析 NAS / BIM 文件。
- Agent 绕过平台权限读取数据。
- 把 catalog metadata 伪装成文件正文 evidence。
- Agent 自动审批、自动整改、自动删除。
- 写 OpenSearch、Qdrant、MinIO 或长期 memory。
- production rollout。

## 6. 二期路线调整

近期优先批次：

`批次 5A：贾维斯数据管家内嵌 v0`

放置位置：

- 在 `批次 4R：文件访问安全闭环复验与收口` 之后。
- 在 `批次 5B：数据管家客户版模块补齐` 之前或与其前端体验并行推进。

当前状态：

- 已通过测试 agent 复验并正式收口。
- 当前仅代表“前端嵌入 / 平台 Gateway / 只读目录问答壳 / 权限证明 / Missing Evidence / 审计 trace”可用。
- 不代表完整数据管家、正文问答、索引写入或 production rollout 就绪。

原因：

- 文件访问安全闭环是客户交付前的底线，必须先收口路径隐藏和下载权限。
- 贾维斯 v0 只做 catalog-only，不读取文件正文，不写库，不改 NAS，因此可以尽快成为前端亮点。
- 数据管家客户版模块补齐时，应围绕贾维斯入口设计，而不是后期再强行塞入。

## 7. 批次 5A 范围

### 后端

必须完成：

- 复核 `POST /api/agent/hermes/chat` 请求 DTO 与耦合指南一致。
- 复核 outbound request 包含：
  - `request_id`
  - `user_context`
  - `page_context`
  - `permission_context`
  - `query`
  - `response_requirements`
- 复核 `project_scope` fail closed。
- 复核 `allowed_actions` 仅包含只读能力。
- 复核 Hermes Client 只允许本机或 allowlist 地址。
- 增加平台侧只读审计，至少记录 request id、用户 ref、项目 ref、source view、evidence mode、permission status、status、latency。
- 审计和日志不得记录 secret、raw row、真实 NAS path、未授权文件名、正文内容。
- 输出专项脚本 `scripts/dev/check-hermes-jarvis-gateway.sh`。

不做：

- 不新增 DB 写操作。
- 不让 Hermes 触发真实扫描或 checksum。
- 不读取文件正文。
- 不写向量库或搜索引擎。

### 前端

必须完成：

- 将用户可见名称从技术口径 `Hermes` 收束为 `贾维斯数据管家`。
- 项目工作台第一屏保留醒目的 `问贾维斯` 入口。
- 文件行或文件详情中保留 `问贾维斯` 入口。
- 展示能力状态：
  - 当前模式：资产目录辅助。
  - 正文问答：未开放。
  - 生产发布：未开放。
- 回答卡片必须明确展示：
  - catalog-only。
  - missing evidence。
  - denied。
  - operation plan draft。
  - 权限证明状态。
- 明确文案：当前不读取文件正文，不执行数据库或 NAS 写操作。

不做：

- 不展示真实 NAS 路径。
- 不让用户误以为贾维斯已经读过文件正文。
- 不提供自动执行按钮。

## 8. 批次 5A 验收标准

必须通过：

- 登录用户在项目详情页可以打开 `问贾维斯`。
- `GET /api/agent/hermes/capabilities` 返回 catalog-only 能力。
- 有权项目可返回 catalog-only 或 missing-evidence 响应。
- 无权项目或缺少权限标签时 fail closed。
- 请求文件正文类问题时返回 Missing Evidence，而不是伪造正文答案。
- 返回结果不包含真实 NAS 路径。
- 返回结果不包含 secret、token、raw DB row。
- operation plan 只能展示草案，不执行动作。
- 平台侧审计存在，且日志不含敏感字段。
- Hermes 未启动或不可用时，平台安全返回 `agent_unavailable`，不白屏、不 500。
- 前端构建、后端构建、健康检查和专项脚本通过。

P0：

- Agent 绕过权限回答无权资产。
- Agent 输出真实 NAS 路径给普通用户。
- Agent 把目录元数据伪装成文件正文证据。
- Agent 自动执行写库、扫描、删除、移动或审批。
- 前端暗示已经完成正文问答，但实际没有 evidence。

P1：

- 缺少 Missing Evidence 展示。
- 缺少 capabilities 展示。
- 缺少平台侧审计。
- Hermes 不可用时体验不清楚。
- 前端仍大量使用 Hermes 技术名，用户感知不到公司“贾维斯”亮点。

## 9. 后续解锁条件

只有 Hermes 完成对应阶段后，才允许进入更深能力：

### Hermes Phase 2.87 后

可做小样本 evidence 写入联调：

- 1 个授权 asset。
- 最多 20 chunks。
- 非敏感小样本。
- 平台前端标记为联调样本。

### Hermes Phase 2.88 后

可做 OpenSearch / Qdrant 小范围索引联调：

- 展示 evidence_ready。
- 展示 document citation。
- 区分 catalog citation 和 document citation。

### Hermes Phase 2.89 后

可做前端受控正文问答：

- 必须带 citation。
- 必须保留权限证明。
- 必须保留审计。
- 仍禁止自动改 DB、自动改 NAS、自动投产、自动扩大权限。

## 10. 主 agent 裁决

贾维斯是二期客户交付版的重要亮点，但不是放开 Agent 自动动作的理由。

近期目标不是“让贾维斯无所不能”，而是先让客户看到：

1. 平台已经把项目资产目录、权限、审计和 Agent 接口打通。
2. 贾维斯能在权限范围内解释资产目录、指出缺少证据、给出人工审批草案。
3. 平台清楚知道什么不能回答、什么不能自动执行。

这条路线应作为二期 5A 的优先任务纳入开发计划。

## 11. V3 后续开发调整

V3 文档确认 Hermes Phase 2.89 可以继续推进只读 Gateway 联调，但仍明确禁止写入、索引和生产发布。因此 5A 后续不应直接跳到正文问答，而应先补一个小批次：

`批次 5A.1：贾维斯 Gateway 合同对齐与联调增强`

### 5A.1 目标

- 对齐 V3 推荐的 Gateway 形态。
- 补齐 Hermes health 展示和平台语义 chat 入口。
- 让平台 trace 在审计中可关联。
- 给后续 selective indexing / 正文 evidence 接入预留清晰灰度开关，但默认关闭。

状态：已通过测试 agent 专项复验并正式收口。

### 5A.1 允许

- 新增平台侧健康检查接口，例如 `GET /api/data-steward/hermes/health`。
- 保留现有 `/api/agent/hermes/*`，同时可按平台数据管家语义增加只读别名接口，例如 `POST /api/data-steward/chat`。
- 新增只读 catalog search 壳：`POST /api/data-steward/catalog/search`，只返回权限过滤后的 metadata preview。2026-05-18 已补齐该接口，并在前端贾维斯面板展示“资产目录预览”。
- 前端展示贾维斯健康状态、当前模式、平台 trace、Hermes trace。
- 审计中记录 platform trace 与 hermes trace 的关联。
- 在前端展示 `asset_catalog_preview / missing_evidence / permission_denied / requires_human_review`。

### 5A.1 禁止

- Agent DB CRUD。
- Agent NAS CRUD。
- 自动扫描 NAS。
- 自动解析大批文件。
- 自动写 Hermes `documents / chunks`。
- 自动写 OpenSearch / Qdrant / MinIO。
- 自动 selective indexing。
- 正文 evidence 回答进入生产前端。
- production rollout。

### 5A.1 与 5B 的关系

- 5A.1 是贾维斯 Gateway 合同增强，可以作为 5B 前的小补丁。
- 5B 仍然是数据管家客户版模块补齐：模型集成、管理对象、事项列表、任务列表、导出列表、文件服务。
- 如果用户希望更快看到业务页面，5A.1 只做后端 health + 前端健康状态 + 脚本，不扩大到完整搜索体验。

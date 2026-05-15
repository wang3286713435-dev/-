# 二期批次一：只读资产目录与 Agent 对接预览层规划

日期：2026-05-12

## 1. 文档定位

本文件是二期启动前的主 agent 规划文档，不是开发完成报告。

二期批次一只做“一期资产底座之上的只读目录与权限证明”，用于把现有真实 NAS 元数据、稳定读模型和后续企业 Agent 对接方式在前端与 REST 层展示清楚。当前不进入模型轻量化、构件级解析、正文抽取、向量化或自动治理。

## 2. 主 Agent 裁决

二期批次一名称固定为：

`只读资产目录与 Agent 对接预览层`

本批只允许包含四类能力：

1. 前端资产目录。
2. REST 权限证明。
3. 只读 catalog API。
4. Agent preview / audit-ready 页面。

本批明确禁止：

1. Agent 直接增删改数据库。
2. Agent 自动移动、删除、修复 NAS 文件。
3. Agent 自动把 BIM、PDF、Office 或其他文件正文写入向量库。
4. Agent 自动下结论或自动审批。
5. 多 Agent 调度真实业务动作。
6. 面向客户的生产级权限体系承诺。

## 3. 与一期的关系

二期批次一直接复用一期已经完成的能力：

1. 项目资产台账。
2. NAS 元数据资产库。
3. 文件资产分页查询。
4. 数据质量状态。
5. 审计事件与事件流。
6. 企业 Agent 稳定 View：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
7. 本机 `hermes_agent_ro` 只读账号与 DB-2 元数据合同。

本批不重新导入真实 NAS，不重新治理 `95`、`98`、`99` 等非标准目录，不强制补齐剩余 checksum，不读取文件正文，不修改 NAS 原文件。

## 4. 产品形态

### 4.1 前端资产目录

建议入口：`数据管家 / 资产目录`。

建议路由：`/data-steward/catalog`。

页面目标是让用户按“目录”的方式浏览已登记资产，而不是只看项目列表。首版建议包含：

1. 项目筛选：真实 NAS 项目、样板项目、后续治理项目。
2. 目录路径筛选：按逻辑目录、原始 NAS 目录或文件路径片段查询。
3. 文件筛选：文件名、文件类型、扩展名、专业、版本、大小、checksum 状态、质量问题。
4. 只读详情：展示项目、文件、路径、版本、专业、质量状态、审计摘要和 Agent 可见性。
5. 权限提示：明确当前用户为什么可以或不可以看到路径、下载入口或 Agent 预览信息。

### 4.2 只读 Catalog API

建议 REST 契约：

1. `GET /api/data-steward/catalog/projects`
2. `GET /api/data-steward/catalog/directories`
3. `GET /api/data-steward/catalog/files`
4. `GET /api/data-steward/catalog/files/{fileId}`
5. `GET /api/data-steward/catalog/files/{fileId}/audit-context`

核心返回字段应稳定、清晰，至少包含：

1. 项目：`projectId`、`projectCode`、`projectName`、`projectStage`、`assetSource`。
2. 文件：`fileId`、`fileName`、`fileExt`、`fileKind`、`disciplineCode`、`disciplineName`、`version`、`sizeBytes`、`checksum`、`status`、`confidenceLevel`。
3. 存储：`storageProvider`、`logicalPath`、`storagePathVisible`、`storagePathVisibilityReason`。
4. 治理：`qualityFlags`、`lastVerifiedAt`、`updatedAt`。
5. Agent 预览：`agentReadable`、`agentReadReason`、`agentContractView`。

本批 catalog API 必须只读。除权限证明类接口可以产生审计事件外，不得改变项目、文件、路径、任务、审核或 NAS 数据。

### 4.3 REST 权限证明

建议 REST 契约：

1. `GET /api/data-steward/catalog/files/{fileId}/permission-proof`
2. `POST /api/data-steward/catalog/permission-proofs:check`

权限证明不是授权体系重写，而是把当前平台为什么允许或拒绝访问说清楚。返回建议包含：

1. `allowed`：是否允许。
2. `decision`：`ALLOWED` 或 `DENIED`。
3. `actorType`：`USER` 或 `AGENT_KEY`。
4. `projectScope`：命中的项目范围。
5. `reasonCode`：稳定原因码。
6. `reasonText`：面向运维和测试的解释。
7. `evidence`：角色、项目成员关系、API Key 范围等非敏感证据。
8. `traceId`：统一追踪 ID。
9. `checkedAt`：检查时间。

权限证明不得泄漏密钥、token、完整权限表或内部实现细节。

### 4.4 Agent Preview / Audit-ready 页面

建议入口：`数据管家 / Agent 预览`。

建议路由：`/data-steward/agent-preview`。

页面目标不是让 Agent 真实执行治理动作，而是向平台团队、企业 Agent 团队和测试人员证明：

1. Agent 能看到哪些项目和文件元数据。
2. Agent 看不到哪些未授权资产。
3. 哪些字段会进入 Agent 候选上下文。
4. NAS 路径何时可见，何时被隐藏。
5. 哪些操作当前明确禁止。
6. 权限证明和预览行为是否能留下审计线索。

首版页面建议包含四个区块：

1. Catalog 样例：只读展示项目、目录、文件元数据。
2. 权限证明：输入文件 ID 或项目编码，查看允许/拒绝原因。
3. Agent 可见字段：展示字段合同和脱敏规则。
4. 禁止动作清单：明确本批不做写库、移动文件、正文入库、自动审批。

## 5. 权限与安全边界

本批必须维持一期安全边界：

1. 用户 JWT 继续按平台项目权限校验。
2. Agent API Key 继续按授权项目范围校验。
3. 没有项目权限时，目录、文件、路径和权限证明均默认拒绝。
4. NAS 真实路径在本机内部试运行中可对授权管理员显示；普通用户和客户环境默认隐藏或按策略返回。
5. 所有响应继续使用统一响应结构并返回 `traceId`。
6. OpenAPI 必须包含本批新增接口。
7. 权限证明、Agent preview 查询和关键拒绝场景应写入轻量审计事件，普通目录列表不要求逐条审计。

## 6. 验收标准

二期批次一通过标准：

1. 前端能打开资产目录和 Agent 预览页面。
2. Catalog API 只读可用，能分页查询项目、目录、文件和详情。
3. Catalog API 对无权限项目返回拒绝或空结果，不泄漏文件路径。
4. 权限证明接口能解释授权与拒绝原因，且带 `traceId`。
5. Agent preview 页面能展示“Agent 当前可见什么、不可做什么”。
6. 审计或事件中可追踪权限证明和 Agent preview 关键行为。
7. OpenAPI 可访问并包含新增接口。
8. 前端构建、后端构建、健康检查通过。
9. 验收脚本能证明本批所有接口是只读的，调用前后正式资产数量、项目数量和 NAS 文件均不被改变。

P0 不允许通过：

1. 无权限用户可以看到未授权项目文件或真实 NAS 路径。
2. Catalog API 或 Agent preview 接口产生项目、文件、路径、任务或审核状态变更。
3. Agent preview 暗含自动审批、自动修复、自动删除或自动写库能力。
4. OpenAPI 缺失新增接口。
5. 权限证明返回敏感凭证或内部密钥信息。

## 7. 开发 Agent 约束

后续启动开发 agent 前，主 agent 应把本文件转写为 `handoff/dev-agent/current-prompt.md`。

开发 agent 必须使用 Ralph Loop 推进，但 Ralph story 只能围绕本批范围拆分：

1. 只读 catalog API。
2. REST 权限证明。
3. 前端资产目录。
4. Agent preview / audit-ready 页面。
5. 验收脚本和报告。

Ralph Loop 完成承诺必须明确：

`只有当接口只读、权限不泄漏、页面可打开、OpenAPI 可见、构建通过、专项脚本通过、报告写入 handoff/dev-agent/latest-report.md 时，才算完成。`

开发 agent 不得扩展到：

1. Agent 写库。
2. 文件正文抽取。
3. 向量库或搜索引擎写入。
4. 真实 NAS 移动、删除、修复。
5. 模型轻量化或构件级解析。
6. 客户生产级权限承诺。

## 8. 测试 Agent 约束

后续测试 agent prompt 应重点验证：

1. 普通用户无法读取无权限项目的 catalog。
2. 普通用户无法看到无权限真实 NAS 路径。
3. 管理员可查询授权范围内真实 NAS 元数据。
4. 权限证明能解释允许和拒绝原因。
5. Agent preview 页面不暴露禁止动作入口。
6. 调用本批接口前后项目数、文件数、扫描任务数、删除申请数不异常变化。
7. OpenAPI 包含新增接口。
8. 前后端构建、后端健康检查通过。

## 9. 后续批次顺序

二期批次一通过并与企业 Agent 团队对齐后，再进入：

1. selective indexing。
2. NAS 文件正文只读抽取。
3. Agent workflow / review suggestion。
4. 受控写操作。

受控写操作必须最后做，且必须重新走需求确认、权限设计、审计设计、验收设计和安全确认。

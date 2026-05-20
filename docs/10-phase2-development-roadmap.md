# 二期客户交付版开发路线

更新时间：2026-05-15

## 1. 文档定位

本文档用于把项目主视角从 `一期内部 BIM 资产管理试点` 切换到 `二期客户交付完整版`。

后续二期开发时，主 agent、开发 agent、测试 agent 优先读取：

1. `docs/07-complete-delivery-prd.md`
2. `docs/08-acceptance-and-agent-integration.md`
3. `docs/03-architecture-and-system-design.md`
4. `docs/10-phase2-development-roadmap.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
7. `handoff/main-agent/status.md`
8. `handoff/dev-agent/latest-report.md`
9. `handoff/test-agent/latest-report.md`

`docs/01` 到 `docs/06` 仍保留为竞品、早期 MVP 和一期基线资料，默认不再作为二期日常开发上下文加载，除非需要追溯历史决策。

## 2. 当前基线

### 2.1 一期已收口能力

一期已经完成内部 BIM/CAD 资产治理底座：

- 后端模块化单体已成型：`core`、`master-data`、`data-steward`、`work-center`、`visualization-adapter`。
- 数据库迁移已推进到 `V20`，包括 BIM 资产、NAS 扫描、任务事件、agent、删除隔离、审核整改、文件访问票据等基础表。
- 真实 NAS 试点已完成：公司内部真实项目文件可以只读扫描、登记元数据、维护路径映射、处理低置信候选、统计容量和查询 SQL View。
- 企业 agent DB-2 对接底座已冻结：一期为本机同环境 MySQL View 只读合同，agent 不直接依赖业务底表。
- 关键脚本已形成回归网：
  - `check-bim-asset-batch1.sh`
  - `check-bim-asset-batch1-tail.sh`
  - `check-bim-asset-batch2.sh`
  - `check-bim-asset-batch3.sh`
  - `check-agent-db2-contract.sh`
  - `check-scan-task-control.sh`

### 2.2 二期已完成能力

二期已经不是空白阶段，当前已完成并收口：

- 批次一：只读资产目录、REST 权限证明、Agent preview / audit-ready 页面。
- 批次二：标准驱动交付闭环最小可用版，覆盖部位树、节点类型、交付物标准、文档/图纸挂接、完整率和缺失项。
- 批次三：人工审核、整改闭环和基础 CSV 报表导出。
- 标准驱动交付新手可用性补丁。
- 项目工作台导航重组：登录主入口改为资产总览，进入项目后围绕当前项目工作。
- 数据管家前端重做批次一：项目资产驾驶舱、RealBIM 风格文件管理、左目录树右文件表。
- 文件管理可用性补强：空文件夹保留、目录双击进入、左侧菜单可读、目录树宽度可拖拽、缺 checksum 受控补算入口、更多菜单去重。
- Hermes / 贾维斯数据管家接入底座：平台后端已有 Agent Gateway，前端已有 `问数据管家` 入口和 catalog-only 展示组件。

### 2.3 尚未正式收口的二期能力

`二期批次四：文件预览与下载权限分离最小闭环` 已有规划和部分实现基础，但需要重新按当前项目工作台和数据管家页面做正式收口判断。

批次四必须重点复核：

- 普通用户是否仍能看到真实 NAS 路径。
- 查看权限与下载权限是否真正分离。
- 短时访问票据是否可审计。
- PDF/图片预览和下载是否走平台受控入口。
- `scripts/dev/check-phase2-batch4-file-access.sh` 是否持续通过。

在批次四正式收口前，不建议继续进入模型轻量化或构件级能力。

## 3. 二期开发主线

二期目标不是继续做内部试点页面，而是形成可给客户交付、可培训、可验收、可私有化部署的完整平台。

二期主线按以下顺序推进：

1. 先稳住当前项目工作台和数据管家主入口。
2. 再收口文件预览、下载权限和路径安全。
3. 尽快把 Hermes 内核包装为公司 `贾维斯数据管家`，以 catalog-only、只读、权限感知方式成为平台亮点。
4. 然后补齐数据管家客户版模块面貌。
5. 再强化标准驱动交付和客户项目初始化。
6. 再进入 BIM 轻量化和构件级能力。
7. 最后做客户部署、运维、文档和演示项目包。

## 4. 后续批次规划

### 批次 4R：文件访问安全闭环复验与收口

定位：二期安全闸门。

目标：

- 正式收口文件预览、下载权限分离、短时票据、路径隐藏和审计。
- 确认客户版默认不暴露真实 NAS 路径。

只做：

- 复核并修正 `catalog`、项目文件管理、详情抽屉、预览弹窗中的路径展示。
- 复核 `PROJECT_ADMIN`、`DELIVERY_ENGINEER`、`PROJECT_VIEWER` 等角色的预览/下载差异。
- 补强测试脚本和页面短回归。
- 输出批次四收口报告。

不做：

- Office/CAD/BIM 转换。
- 模型轻量化。
- 文件正文抽取。
- 向量库。
- 真实 NAS 写操作。

完成标准：

- `check-phase2-batch4-file-access.sh` 通过。
- `check-file-preview-shell.sh` 通过。
- 批次一到三核心脚本回归通过。
- 测试 agent 确认无 P0/P1。

### 批次 5：贾维斯与数据管家客户版模块补齐

本批拆为 `5A`、`5A.1` 和 `5B`。

### 批次 5A：贾维斯数据管家内嵌 v0

定位：二期客户交付版的核心亮点入口。

状态：已通过测试 agent 专项复验并正式收口。

用户可见名称：

`贾维斯数据管家`

技术内核：

`Hermes Memory / Hermes Data Steward`

目标：

- 把当前平台已有 Hermes Gateway 和前端问答组件正式收束为“贾维斯”能力。
- 第一阶段只做 `catalog-only / read-only / permission-aware / Missing Evidence / operation plan draft`。
- 让用户在项目工作台、文件详情和资产目录中可以询问贾维斯，但明确知道当前不读取文件正文、不执行写操作。

当前已有基础：

- `GET /api/agent/hermes/capabilities`
- `POST /api/agent/hermes/chat`
- `AgentGatewayApplicationService`
- `AgentPermissionProofService`
- `AgentAssetContextResolver`
- `HermesAgentClient`
- `DataStewardPanel`
- `DataStewardAnswerCard`
- `EvidenceModeBadge`
- `MissingEvidenceNotice`
- `OperationPlanPreview`

必须补齐：

- 用户界面品牌从技术名 `Hermes` 收束为 `贾维斯数据管家`。
- 平台侧只读审计：request id、用户 ref、项目 ref、source view、evidence mode、permission status、status、latency。
- 审计和日志不得包含 secret、raw DB row、真实 NAS path、未授权文件名或正文内容。
- Hermes 不可用时 fail closed，并显示可理解的 `agent_unavailable`。
- 专项验收脚本：`scripts/dev/check-hermes-jarvis-gateway.sh`。

必须保持：

- catalog-only 不伪装成正文 evidence。
- operation plan 只能是草案，必须显示需要人工审批。
- 无项目权限、缺少权限标签、项目不匹配、scope 过期时 fail closed。
- 前端不直接访问 Hermes，不直接持有 Hermes secret。
- Gateway 只允许本机或受控 allowlist 目标。

不做：

- Agent 直接增删改平台数据库。
- Agent 直接扫描、移动、删除、修改 NAS 文件。
- Agent 全量解析 NAS / BIM 文件。
- 真实 NAS 正文问答。
- OpenSearch / Qdrant / MinIO 写入。
- production rollout。

完成标准：

- 有权项目可返回 catalog-only 或 missing-evidence。
- 正文类问题返回 Missing Evidence，不编造正文答案。
- 无权项目返回 denied。
- 返回结果不包含真实 NAS 路径、secret、token、raw DB row。
- 操作建议只展示草案，不执行动作。
- 前后端构建、健康检查和专项脚本通过。

### 批次 5A.1：贾维斯 Gateway 合同对齐与联调增强

定位：吸收 Hermes Phase 2.89 / V3 联调口径，补齐 5A 后的 Gateway 合同细节。

状态：已通过测试 agent 专项复验并正式收口。

来源：

`/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`

当前 Hermes 口径：

- Hermes 已到 `phase-2.89-test-machine-runtime-preflight-handoff-baseline`。
- 可以继续做前端嵌入、平台后端 Gateway、只读资产目录联调、权限上下文传递和审计 trace 对齐。
- 仍不授权真实 DB 写入、parser、NAS copy、index write、Agent answer integration 或 production rollout。

目标：

- 补齐 Hermes health 展示。
- 补齐平台语义 chat 入口。
- 对齐 platform trace 与审计 trace。
- 展示 feature flag / runtime 状态，明确写入和生产发布未开放。

建议接口：

- `GET /api/data-steward/hermes/health`
- `POST /api/data-steward/chat`
- 可选只读目录搜索：`POST /api/data-steward/catalog/search`，本次收口未实现，后续 Hermes 合并或 selective indexing 前再做。

必须保持：

- 前端不直连 Hermes。
- 前端不持有 Hermes token、DB 凭据、NAS 凭据或 `.env`。
- Gateway 负责登录态、项目权限、project_scope、permission proof、审计和二次安全检查。
- catalog metadata 只能显示为 `asset_catalog_preview`，不能标记为正文 evidence。

不做：

- Agent DB CRUD。
- Agent NAS CRUD。
- 自动扫描 NAS。
- 自动解析大批文件。
- 自动写 Hermes `documents / chunks`。
- 自动写 OpenSearch / Qdrant / MinIO。
- 自动 selective indexing。
- 正文 evidence 回答进入生产前端。
- production rollout。

### 批次 5B：数据管家客户版模块补齐

定位：把数据管家从“项目资产驾驶舱 + 文件管理”补成客户可理解的完整工作区。

目标：

- 复刻 RealBIM 数据管家的核心信息架构，但不复制品牌视觉。
- 让客户进入项目后能看到资产状态、文件、模型、管理对象、事项、任务、导出和文件服务。

范围：

- 资产驾驶舱继续完善：扫描、质量、容量、最近文件、治理风险。
- 文件管理继续完善：目录树、搜索、高级搜索、分页、版本、质量问题、权限状态。
- 模型集成：先做模型文件归集、处理状态、预览状态、集成记录列表，不做真实轻量化。
- 管理对象：先做对象台账与文件/模型关联，不做构件级解析。
- 事项列表、任务列表：先围绕整改、审核和数据治理任务展示，不做复杂流程引擎。
- 导出列表：展示报表导出和异步任务导出记录。
- 文件服务：展示存储根、路径映射、文件访问状态和只读/受控写能力边界。

完成标准：

- 数据管家左侧模块结构清晰。
- 每个模块都有真实数据或明确空状态。
- 文件管理大项目不卡死、不横向撑爆。
- 不开放上传、移动、删除、新建文件夹等真实 NAS 写动作。
- 贾维斯入口在数据管家工作区内保持明显但不越权。

### 批次 6：客户项目初始化与标准模板化

定位：把现有标准驱动交付能力变成客户可落地的初始化流程。

目标：

- 客户新建项目后，可以按模板初始化项目结构、部位、节点类型、交付物标准和目录模板。

范围：

- 项目初始化向导。
- 交付标准模板库。
- 目录模板预览和套用。
- 标准锁定前检查。
- 标准锁定后版本化调整规则。
- 标准缺口修复入口。

完成标准：

- 一个新项目能从空项目走到“标准已就绪”。
- 文档/图纸交付页能直接基于模板生成应交项。
- 标准锁定后不能静默破坏已有交付数据。

### 批次 7：文件预览转换与批量交付增强

定位：客户交付版文件体验补齐。

目标：

- 在已收口的受控访问基础上，补齐更多格式预览策略和批量交付能力。

范围：

- PDF、图片预览稳定化。
- Office 预览转换策略。
- CAD/DWG 预览策略。
- 批量下载、打包下载和导出审计。
- 文件不可预览时的清晰原因和处理建议。

完成标准：

- 常见客户资料不再只显示“不可预览”。
- 查看权限和下载权限仍然分离。
- 大文件下载不进入数据库，不一次性读入内存。

### 批次 8：BIM 轻量化适配层

定位：二期核心差异化能力开始落地。

目标：

- 接入一个 BIM/三维引擎或转换服务，但业务层只依赖适配层。

范围：

- 模型发布任务。
- 轻量化状态。
- Web 模型预览入口。
- 多专业模型集成展示。
- 模型处理失败原因和重试。
- `visualization-adapter` 模块接口落地。

完成标准：

- 至少一种真实模型格式可完成离线转换后在线预览。
- 上层业务不绑定具体厂商 API。
- 预览失败可追踪、可重试、可审计。

### 批次 9：构件级解析、搜索与交付联动

定位：二期完整客户交付验收的核心 BIM 能力。

目标：

- 支持构件属性查询、构件搜索、定位、高亮和交付对象关联。

范围：

- 构件索引状态。
- 构件属性查询。
- 构件搜索。
- 构件定位、高亮、隐藏、隔离。
- 管理对象与构件关联。
- 整改项与模型构件关联。

完成标准：

- 用户能从构件搜索定位到模型。
- 用户能从文件/对象/整改项跳到相关构件。
- 构件能力仍通过适配层实现，不锁死厂商。

### 批次 10：客户部署、运维和交付文档包

定位：从可用产品变成可交付产品。

目标：

- 形成客户现场可安装、可备份、可恢复、可诊断、可培训、可验收的交付包。

范围：

- 私有化部署手册。
- 初始化向导。
- 演示项目模板。
- 备份恢复脚本。
- 日志诊断和健康检查。
- API 手册。
- 用户操作手册。
- 验收脚本矩阵。

完成标准：

- 新环境可以按文档启动。
- 客户能按演示项目跑完整链路。
- P0/P1 清零后才允许标记客户交付候选版本。

## 5. 暂缓到三期或后置的能力

以下能力不应在近期二期批次中混入：

- Agent 自动审批、自动整改、自动删除、自动写库。
- 文件正文自动写入向量库、搜索引擎或长期 memory。
- 多 agent 调度真实业务动作。
- 真实 NAS 文件自动移动、删除、改名。
- 高级碰撞检查原生计算。
- 模型版本对比。
- 图模联动。
- AI 审核和客户侧智能问答。
- 多行业模板扩展。

这些能力只有在二期客户版基础能力稳定后，才作为三期增值服务独立设计和验收。

## 6. 二期上下文减负规则

后续日常开发不再默认加载全部历史文件。

### 必读

- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/03-architecture-and-system-design.md`
- `docs/10-phase2-development-roadmap.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
- `handoff/main-agent/status.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

### 按需读取

- `docs/01-competitor-analysis.md`：复刻 RealBIM 交互时读取。
- `docs/02-v1-prd.md`：追溯早期 MVP 时读取。
- `docs/04-rollout-and-agent-prompts.md`：重写 agent 协作规则时读取。
- `docs/05-phase1-dev-baseline.md`、`docs/06-phase1-backlog-and-readiness.md`：只在迁移或重建一期环境时读取。
- `handoff/main-agent/real-nas-*`：只在处理真实 NAS 数据治理时读取。
- `handoff/main-agent/enterprise-agent-*`：只在企业 agent 合并或联调时读取。

## 7. 当前下一步

测试 agent 最新短回归已确认文件管理三项优化通过，当前无 P0/P1/P2。

当前已更新：批次 4R、批次 5A、批次 5A.1 均已通过专项复验并收口。建议下一步不是继续零散修页面，而是执行以下顺序：

1. 进入 `批次 5B：数据管家客户版模块补齐`。
2. 完成 5B 后进入 `批次 6：客户项目初始化与标准模板化`。
3. 再进入文件预览转换、BIM 轻量化适配层和构件级能力。

当前仍不得进入正文抽取、向量库、Agent 自动写库、Agent 自动 NAS 操作、BIM 轻量化或构件级能力，除非用户单独批准对应批次。

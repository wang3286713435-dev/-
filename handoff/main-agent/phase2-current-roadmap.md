# 二期当前路线交接

更新时间：2026-05-20

## 2026-05-20 路线更新：暂停 G4，冻结 Hermes，恢复平台主线

用户裁决：

- G4 暂停开发。
- Hermes 定位已经出现偏移，必须立刻冻结。
- 后续 Hermes 重新对齐后，通过独立分支继续完善。
- 主线不能继续卡死在 Hermes，必须恢复平台功能本身的完善。

Git 治理结果：

- 已创建冻结分支 `codex/hermes-g3-g4-freeze`。
- 已将 `main` 快进到当前平台成果点。
- `codex/nas-real-project-import-pr` 保留为历史开发分支，不再作为主线继续推进。

当前 active 主线状态：

`M1A：平台主线功能基线审计与交付闭环缺口收束` 已收口。

主线健康度：

- 当前为 `绿灯可继续主线开发`。
- Git 已回到主线治理口径，Hermes/G3/G4 已有冻结分支。
- M1A 已完成平台本体基线审计与 P1 修复。
- 但绿灯不代表客户交付准备完成，也不代表进入 9A。
- 后续仍按 M 系列继续补齐平台本体能力。

M1A 已验证并修补：

- 项目工作台是否清晰。
- 资产总览和文件管理是否适合真实项目使用。
- 工程主数据是否能被普通员工理解和维护。
- 标准驱动交付链路是否能跑通。
- 文档 / 图纸交付、审核、整改、导出预检查是否稳定。
- BIM Mock 入口是否只是安全占位。
- 权限、审计、路径脱敏是否没有回归。

当前批次状态：

`M1B：项目工作台与数据管家可用性收口` 已收口。

测试 agent 验收通过，当前无 P0 / P1 / P2。

M1B 已完成：

- 优化资产总览 Hero 区和项目状态表达。
- 修复真实项目统计显示明显不准的问题。
- 让项目来源、接入状态、主数据状态、交付状态和下一步动作更清楚。
- 在项目工作台解释数据管家 / 工程主数据 / 工作中心的用途。

M1B 未进入 Hermes 新能力、G4、8B / 8C / 9A。

当前批次状态：

`M1C：工程主数据真实项目落地` 已收口。

测试 agent 验收通过，当前无 P0 / P1 / P2。

M1C 已完成：

- 将真实项目接入向导、初始化向导、部位树、节点类型和交付物标准，从“模板演示”收敛为真实 NAS 项目可用的工程主数据落地流程。
- 明确展示真实资产证据、catalog-only 边界、缺口、草案预览、人工确认和后续维护入口。
- 验证 105/503 与 93/506 两个真实 NAS 项目，不允许为 105 写死。
- 保持 Hermes/G4/8B/8C/9A 冻结边界。

当前批次状态：

`M1D：标准驱动交付闭环强化` 已收口。

测试 agent 复核通过，当前无 P0 / P1 / P2。

M1D 已完成：

- 把工程主数据、文档交付、图纸交付、批量挂接、审核、整改、复审、完整率刷新和导出预检查串成普通员工可执行闭环。
- 补齐项目工作台信息结构：`项目资产 -> 工程主数据 -> 交付工作中心`。
- 工作中心不是工程主数据子功能，但必须在项目工作台下排在工程主数据之后；工程主数据未就绪时，提示先生成 / 确认工程主数据草案。
- 不依赖 Hermes，员工也能完成一次标准驱动交付流程。
- 验证 105/503 与 93/506 或另一个真实 NAS 项目。

M1 平台本体稳定期可以进入阶段复盘。

下一步建议二选一：

1. `M1E：文件管理连续工作体验与后台任务可追踪性收口`。
2. `M1 阶段复盘与 M2A 准备`。

是否进入 M1E 或 M2A，均需用户再次确认。

当前禁止：

- 继续 G4。
- 新增 Hermes 能力。
- 进入 8B / 8C / 9A。
- 开放真实 NAS 增删改查。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 写 Hermes memory、OpenSearch、Qdrant、MinIO documents/chunks。
- Agent 自动审批、自动整改、自动挂接。

主线治理说明：

- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`

后续路线：

- `M1：平台本体稳定期`，完成 M1A/M1B/M1C/M1D。
- `M2：客户版核心业务补齐期`，补齐数据管家、权限审计、交付包和档案目录。
- `M3：BIM 引擎前置准备期`，先做 `8B-0`，再根据引擎选型进入真实接入。
- `M4：客户交付准备期`，9A 后置到平台功能和 BIM 路线稳定后。

## 2026-05-20 路线更新：进入 G4，8B 继续后置

状态：已暂停。以下为历史记录，不再作为当前 active 开发批次。

G3 已完成收口与 Git checkpoint。用户确认当前不自动进入 8B / 8C / 9A。

当前下一批固定为：

`G4：真实项目交付闭环试运行与问题修补`

G4 目标是把当前已有能力放到真实 NAS 项目里跑通，而不是继续堆新模块。主样本为 105 项目，同时必须至少抽查另一个真实 NAS 项目，确保能力不是为 105 硬编码。

G4 必须验证并修补：

- 真实项目从资产总览进入项目工作台。
- Hermes 解释当前项目如何开始数字化交付。
- Hermes 生成工程主数据补齐计划。
- Hermes 生成交付缺失项补交 / 文件挂接推荐方案。
- 用户未确认时不能执行写动作。
- 用户确认后通过平台既有能力执行推荐挂接。
- 文档 / 图纸交付完整率和缺失项状态刷新。
- 审核、整改、导出预检查和审计链路可用。

G4 不进入：

- `8B / 8C / 9A`
- BIM 轻量化引擎
- 真实模型转换
- 构件级解析
- 正文抽取
- selective indexing
- NAS 增删改查
- Hermes memory 写入
- 未确认自动写库、自动挂接、自动审批、自动整改

8B 当前继续后置，待 G4 真实项目交付闭环试运行稳定后再评估。

G4 交接文件：

- `handoff/main-agent/phase2-g4-real-project-delivery-trial-plan.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`

## 2026-05-20 路线记录：G3 已收口，8B 后置

G2 已完成收口与 Git checkpoint。用户明确要求当前不要继续增加平台内容，避免平台越来越大而空。

当前下一批固定为：

`G3：Hermes 平台工作型 Agent MVP`

G3 目标是让 Hermes 从“平台问答 AI”升级为“通过平台受控能力替员工做事的 Agent”。

G3 必须让 Hermes 能：

- 读取当前真实项目状态。
- 生成工程主数据补齐计划。
- 生成交付缺失项补交 / 文件挂接推荐方案。
- 在用户人工确认后调用平台已有能力执行推荐挂接。
- 展示执行结果并留审计。

G3 不进入：

- `8B / 8C / 9A`
- BIM 轻量化引擎
- 真实模型转换
- 构件级解析
- 正文抽取
- selective indexing
- NAS 增删改查
- Hermes memory 写入
- 未确认自动写库、自动挂接、自动审批、自动整改

8B 当前后置，待 G3 以及后续真实项目交付闭环试运行稳定后再评估。

G3 交接文件：

- `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
- G3 开发 / 测试 prompt 已被后续 G4 prompt 替换，G3 历史以 Git commit `71ac64a` 和报告为准。

## 当前结论

一期已收口。后续主视角切到 `二期客户交付版`。

二期当前不是从零开始，已完成：

- 批次一：只读资产目录、REST 权限证明、Agent preview。
- 批次二：标准驱动交付闭环最小可用版。
- 批次三：人工审核、整改闭环、基础报表导出。
- 项目工作台导航重组。
- 数据管家资产驾驶舱和文件管理重做第一批。
- 文件管理三项优化短回归：目录树宽度调节、缺 checksum 入口、更多菜单去重，测试通过。
- 批次 4R：文件访问安全闭环已正式收口。
- 批次 5A：贾维斯数据管家内嵌 v0 已通过测试 agent 专项复验并正式收口。
- 批次 5A.1：贾维斯 Gateway 合同对齐与联调增强已通过测试 agent 专项复验并正式收口。
- 批次 5B：数据管家客户版模块补齐已通过测试 agent 短回归并正式收口。
- 批次 6A：客户项目初始化与标准模板化已通过测试 agent 专项验收并正式收口。
- 批次 6B：批量挂接交付与交付包准备视图已通过测试 agent 专项短回归并正式收口。
- 批次 7A：文件预览策略统一与交付包导出预检查已通过测试 agent 专项验收并正式收口。
- 批次 7B：文件预览转换体验增强已通过测试 agent 专项验收并正式收口。
- 批次 8A：BIM 轻量化适配层与 Mock 预览入口已通过测试 agent 专项验收并正式收口。
- 插入批次 G1：Agent 引导式交付治理 MVP 已通过测试 agent 专项验收并正式收口。

当前必须避免继续按一期口径推进。后续开发以 `docs/10-phase2-development-roadmap.md` 为二期路线入口。

## 必读入口

开发或测试二期功能前，优先读取：

1. `docs/07-complete-delivery-prd.md`
2. `docs/08-acceptance-and-agent-integration.md`
3. `docs/03-architecture-and-system-design.md`
4. `docs/10-phase2-development-roadmap.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
7. `handoff/main-agent/status.md`
8. `handoff/dev-agent/latest-report.md`
9. `handoff/test-agent/latest-report.md`

除非需要追溯历史，不再默认读取大量一期报告。

## 当前插入批次

二期插入批次 G1、G2、G3 均已正式收口。G4 已暂停并仅作为历史方案保留。当前 active 主线为：

`M1A：平台主线功能基线审计与交付闭环缺口收束`

M1A 目标：

- 验收资产总览、项目工作台、文件管理、工程主数据、文档/图纸交付、审核整改、导出预检查。
- 确认当前平台不依赖 Hermes 也能被普通员工使用。
- 用 105 和至少另一个真实 NAS 项目抽查基础链路。
- 发现并修复 P0/P1，必要时处理非常小的 P2。

M1A 仍禁止：

- 继续 G4。
- 新增 Hermes 能力。
- 进入 8B / 8C / 9A。
- 真实 NAS 增删改查。
- 真实 BIM 轻量化。
- 文件正文抽取、selective indexing、Hermes memory 或向量库写入。
- Agent 自动审批、自动整改、自动写库。

交接文件：

- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`

后续是否进入 8B / 8C / 9A、NAS 受控增删改查、BIM 真实轻量化、构件级能力或客户部署交付文档包，均需用户再次确认。

## 后续候选顺序

### 1. 候选批次 8B-0：BIM 引擎选型与接入前置评估

状态：尚未进入。

目标：

- 在真实 BIM 引擎厂商未确定前，先完成选型、部署、授权、格式支持和 PoC 标准评估。

范围：

- 比较至少 2-3 个候选引擎。
- 输出 RVT、IFC、NWD/NWC、DWG、GLB/GLTF、3D Tiles 格式支持矩阵。
- 明确内网私有化、本机转换服务、离线转换、授权方式。
- 明确 NAS、MinIO、对象存储或引擎自带缓存的产物存储策略。
- 明确 Viewer 能力：模型加载、构件定位、高亮、剖切、测量、漫游、视点。
- 明确成本、授权和客户现场风险。
- 只做方案、PoC 标准和接口对齐，不写死厂商 SDK。

### 2. 后续批次

- `8B：轻量化任务编排与引擎适配骨架`，前置条件是 8B-0 已完成且用户确认引擎方向。
- `8C：真实 BIM 引擎小样本接入`，只做少量模型格式兼容验证。
- `8D：构件属性、定位、高亮、基础搜索`。
- `8E：图模联动与碰撞结果接入`。
- `9A：客户交付准备`，必须等 M1/M2 与 8B-0 条件满足后再启动。

## 已收口基线

### 插入批次 G1：Agent 引导式交付治理 MVP

状态：已正式收口。

目标：

- 让真实 NAS 项目可以通过项目交付体检、缺失项解释、候选文件推荐和人工确认批量挂接，更顺畅地进入数字化交付闭环。

验收结论：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 后端健康检查通过。
- `git diff --check` 通过。
- `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh` 通过，`PASS=34 FAIL=0`。
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh` 通过，`PASS=11 FAIL=0`。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch3-review-rectification-report.sh` 通过，`通过=52 失败=0`。

已确认能力：

- `GET /api/work-center/projects/{projectId}/agent-governance/overview` 已可用。
- `GET /api/work-center/projects/{projectId}/agent-governance/missing-items` 已可用。
- `POST /api/work-center/projects/{projectId}/agent-governance/recommend-bindings` 已可用。
- `POST /api/work-center/projects/{projectId}/agent-governance/recommendations:apply` 已可用。
- 项目工作台已有 `交付治理助手` 入口。
- 项目详情页已有 `开始交付治理` 入口。
- 页面可显示项目体检、Agent 总结、工程主数据状态、文档/图纸完整率、缺失项解释和推荐挂接方案。
- 未选择推荐、未勾选人工确认时不能挂接。
- 勾选人工确认后可执行批量挂接并显示创建、跳过、失败结果。
- 响应和页面未发现真实 NAS 路径、底层存储字段、raw row 或 SQL 泄露。

非阻塞 P2：

- 前端生产构建仍有既有 Vite chunk size warning。
- G1 专项脚本和页面验收会留下 `PHASE2-G1-*` 元数据与测试挂接，后续建议补脚本自清理或隔离测试项目。

收口报告：

`handoff/main-agent/phase2-insert-g1-agent-delivery-governance-mvp-closure.md`

### 批次 8A：BIM 轻量化适配层与 Mock 预览入口

状态：已正式收口。

目标：

- 建立 BIM 轻量化适配层合同。
- 提供模型集成轻量化状态查询。
- 提供轻量化准备计划 dry-run。
- 在模型集成页展示 Mock 预览入口和安全提示。

验收结论：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 后端健康检查通过。
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh` 通过，`PASS=11 FAIL=0`。
- `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh` 通过，`PASS=20 FAIL=0`。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
- `git diff --check` 通过。

已确认能力：

- `GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status` 已可用。
- `GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan` 已可用。
- 返回 `engineMode=MOCK`、`engineConnected=false`、`viewerAvailable=false`、`taskStatus=NOT_CREATED`。
- 准备计划返回 `dryRun=true`、`taskCreated=false`、`realConversionExecuted=false`、`nasFileTouched=false`。
- 模型集成页展示轻量化状态、适配模式 / 引擎、查看轻量化准备和打开 3D 预览入口。
- 3D 入口只提示 Mock 状态，不跳转伪造三维页面。
- 响应和页面未发现真实 NAS 路径、底层存储字段、raw row 或 SQL 泄露。

收口报告：

`handoff/main-agent/phase2-batch8a-bim-lightweight-adapter-closure.md`

### 批次 7B：文件预览转换体验增强

状态：已正式收口。

目标：

- 统一数据管家与工作中心的文件预览转换状态展示。
- 让用户清楚区分“在线预览能力”和“原始文件交付/下载能力”。
- 为 Office、CAD、BIM、压缩包、未知格式提供稳定状态标签和业务提示。

验收结论：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 后端健康检查通过。
- `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh` 通过，`PASS=20 FAIL=0`。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
- `git diff --check` 通过。

已确认能力：

- PDF / 图片显示可在线预览。
- Office 显示需 Office 转换服务。
- CAD 显示需 CAD 图纸转换或查看引擎。
- BIM 显示需 BIM 轻量化。
- 压缩包显示仅下载原文件。
- 未知格式显示暂不支持在线预览。
- 不可直接在线预览文件不会被误创建 `PREVIEW` 访问票据。
- 文档/图纸交付页继续区分预览状态与导出状态。
- 新增响应未发现真实 NAS 路径或底层存储字段泄露。

### 批次 7A：文件预览策略统一与交付包导出预检查

状态：已正式收口。

目标：

- 统一数据管家与工作中心的文件预览状态口径。
- 提供只读交付包导出预检查接口和页面展示。
- 明确预检查是 dry-run，不生成真实交付包，不访问、不复制 NAS 文件。

验收结论：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 后端健康检查通过。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
- `git diff --check` 通过。

已确认能力：

- `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck` 已纳入 OpenAPI。
- DOCUMENT / DRAWING / ALL 三种口径均返回 `dryRun=true`、`packageGenerated=false`。
- 页面可展示统计卡片和明细表，包括文件 ID、预览状态、预览方式、导出状态和阻塞原因。
- 响应未发现 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storageUri`、`raw row`、`SQL` 等禁出字段。
- 文档/图纸交付页仍保持项目工作台导航、批量补交、审核/驳回/记录和分页远程文件选择能力。

### 批次 4R：文件访问安全闭环复验与收口

状态：已正式收口。

目标：

- 正式确认路径隐藏、查看/下载权限分离、短时访问票据、审计留痕。

必须验证：

- `scripts/dev/check-phase2-batch4-file-access.sh`
- `scripts/dev/check-file-preview-shell.sh`
- 二期批次一到三关键脚本回归。
- 普通用户不能在 catalog、项目文件管理、详情抽屉、预览弹窗中看到真实 NAS 路径。

本批只做安全闭环，不做新格式转换。

### 批次 5A：贾维斯数据管家内嵌 v0

目标：

- 将 Hermes 内核包装成公司平台内的 `贾维斯数据管家`。
- 尽快作为二期前端重要亮点接入项目工作台和文件详情。
- 当前只做 catalog-only、只读、权限感知、Missing Evidence 和操作建议草案。

状态：已正式收口。

验收结论：

- 前端入口已收束为 `问贾维斯 / 贾维斯数据管家`。
- capabilities 返回 catalog-only 能力。
- 正文类问题返回 Missing Evidence。
- 无效项目范围 fail closed。
- Hermes 不可用返回 `agent_unavailable`。
- 审计已落到 `agent.jarvis.chat.*`，未发现路径、token、secret 泄露。
- 批次 4R 文件访问安全闭环未回归。

已有基础：

- 后端已有 `/api/agent/hermes/capabilities` 和 `/api/agent/hermes/chat`。
- 后端已有 Agent Gateway、权限证明、资产上下文解析和 Hermes Client。
- 前端已有 `DataStewardPanel`、回答卡片、Missing Evidence、Operation Plan 展示。
- 项目详情和资产目录已有 `问数据管家` 入口。

必须补齐：

- 用户可见名称统一为 `贾维斯数据管家`，技术名 Hermes 只保留在接口和配置层。
- 增加平台侧只读审计，且日志不包含 secret、raw row、真实 NAS path、未授权文件名或正文内容。
- 增加专项脚本 `scripts/dev/check-hermes-jarvis-gateway.sh`。
- Hermes 不可用时 fail closed，不白屏、不 500。
- 前端清楚展示 catalog-only、missing-evidence、denied 和 operation-plan-draft。

边界：

- 不做正文问答。
- 不写 OpenSearch / Qdrant / MinIO。
- 不做 Agent DB CRUD 或 NAS CRUD。
- 不做自动审批、自动整改、自动删除。

详细路线见：

`handoff/main-agent/hermes-jarvis-coupling-roadmap.md`

### 批次 5A.1：贾维斯 Gateway 合同对齐与联调增强

来源：

- `/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`

目标：

- 按 V3 文档补齐平台 Gateway 健康检查、只读 catalog search 壳、平台 trace 与 Hermes trace 关联。
- 在前端展示贾维斯健康状态、当前模式和灰度开关状态。
- 继续保持只读、catalog-only、Missing Evidence 和人工审批草案。

建议范围：

- `GET /api/data-steward/hermes/health`
- 可选只读别名：`POST /api/data-steward/chat`
- 可选只读 catalog search：`POST /api/data-steward/catalog/search`
- 前端在贾维斯抽屉展示健康状态、模式、contract version、runtime write enabled=false、agent answer integration enabled=false。
- 审计记录 platform trace 与 hermes trace 的关联。
- 专项脚本覆盖 health、catalog search、trace、feature flag 和 5A/4R 回归。

状态：已正式收口。

实际收口范围：

- 已实现 `GET /api/data-steward/hermes/health`。
- 已实现 `POST /api/data-steward/chat`。
- 已实现前端健康状态和只读 runtime flags 展示。
- 已增强 `scripts/dev/check-hermes-jarvis-gateway.sh`。
- 未实现 `POST /api/data-steward/catalog/search`，该接口保留到后续 Hermes 合并或 selective indexing 前置批次。

边界：

- 不写 Hermes `documents / chunks`。
- 不写 OpenSearch / Qdrant / MinIO。
- 不读文件正文。
- 不做 Agent DB/NAS CRUD。
- 不做 production rollout。

## 待开发批次详情

### 批次 5B：数据管家客户版模块补齐

状态：已正式收口。

目标：

- 将数据管家补成客户能理解的完整工作区。

范围：

- 资产驾驶舱。
- 文件管理。
- 模型集成。
- 管理对象。
- 事项列表。
- 任务列表。
- 导出列表。
- 文件服务。

边界：

- 不做真实 NAS 写操作。
- 不做模型轻量化。
- 不做 agent 自动治理。

验收结论：

- 模型集成、管理对象、事项列表、任务列表、导出列表、文件服务均已补齐。
- 首轮 P0“导出列表泄露真实 NAS 绝对路径”已返修。
- `catalog/files.logicalPath` 已改为项目内相对路径，导出列表和 CSV 不再包含 `/Volumes/...`。
- `scripts/dev/check-phase2-batch5b-data-steward-modules.sh` 通过，结果 `PASS=16 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过，结果 `PASS=18 FAIL=0`。
- 测试 agent 短回归未发现新的 `P0 / P1 / P2`。

### 批次 6：客户项目初始化与标准模板化

目标：

- 让客户新项目可以从空项目初始化到标准就绪。

范围：

- 项目初始化向导。
- 标准模板库。
- 目录模板预览和套用。
- 标准锁定和版本化调整规则。

状态：已正式收口。

收口报告：

`handoff/main-agent/phase2-batch6a-project-initialization-closure.md`

### 批次 6B：批量挂接交付与交付包准备视图

目标：

- 让用户可以从缺失项批量选择文件并挂接到同一交付目标。
- 提供只读交付包准备状态，避免用户误以为当前已生成真实交付包。

范围：

- 批量挂接接口。
- 文档/图纸交付页批量补交弹窗。
- 交付包准备汇总。
- 幂等跳过重复挂接。
- 错误类型与跨项目文件拒绝。
- 禁止返回真实 NAS 路径和存储字段。

状态：已正式收口。

收口报告：

`handoff/main-agent/phase2-batch6b-batch-delivery-package-closure.md`

### M1 / M2 / M3 / M4 后续路线

当前不再沿用“批次 8 后直接批次 9/10”的旧路线。

- `M1：平台本体稳定期`，先完成 M1A/M1B/M1C/M1D，确保平台不依赖 Hermes 也能跑通真实项目交付闭环。
- `M2：客户版核心业务补齐期`，补齐数据管家客户版、权限审计、文件访问、交付包与档案目录。
- `M3：BIM 引擎前置准备期`，先做 `8B-0：BIM 引擎选型与接入前置评估`，再决定是否进入真实 8B/8C/8D/8E。
- `M4：客户交付准备期`，9A 必须后置到 M1/M2 完成、8B-0 完成且真实项目闭环可验收之后。

旧的 `批次 9：构件级解析、搜索与交付联动` 和 `批次 10：客户部署、运维和交付文档包` 不再作为近期直接后续批次。

## 后置到三期

近期不得混入：

- Agent 自动审批、自动整改、自动删除、自动写库。
- 文件正文自动进入向量库或长期 memory。
- 多 agent 调度真实业务动作。
- 真实 NAS 自动移动、删除、改名。
- 模型版本对比。
- 图模联动。
- AI 审核。
- 客户侧智能问答。

## 主 agent 监控规则

每个二期批次必须：

- 明确范围和禁止事项。
- 给开发 agent 写 `handoff/dev-agent/current-prompt.md`。
- 给测试 agent 写 `handoff/test-agent/current-prompt.md`。
- 要求开发 agent 使用 Ralph Loop。
- 要求开发 agent 写 `handoff/dev-agent/latest-report.md`。
- 要求测试 agent 写 `handoff/test-agent/latest-report.md`。
- 主 agent 审计通过后才能收口。

后续所有批次必须围绕 PRD 二期客户交付要求推进，不再以一期内部试点标准作为完成标准。

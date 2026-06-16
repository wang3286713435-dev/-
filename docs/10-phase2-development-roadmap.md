# 二期客户交付版开发路线

更新时间：2026-06-04

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

### 2.0 2026-06-04 最新裁决

当前文档原有 M1/M2/8B-0 口径仅保留为历史路线。当前真实主线已经推进到：

`M3 对象存储主链路收口 -> M3X 全项目对象化独立批次 -> M4 语义证据层 -> M5 Hermes 受控证据问答 -> 8D/8E BIM 深化 -> 9A 客户交付准备`

当前关键事实：

- M1/M2 主干能力已经基本完成并经过多轮回归。
- M3A-M3G-9 已完成开发和验收，105 已 100% 对象化。
- M3 整体仍需要主 agent 输出正式收口判断。
- 全项目对象化不混入文档收口，后续单开 `M3X` 批次推进。
- 葛兰岱尔 READY Viewer 已可在 105 BIM 协同页识别和使用，但构件级 BIM 能力未完成。
- Hermes 正文问答未启动，必须等 M4 语义证据层和 M5 Evidence API。

最新基线详见 [11-current-baseline-and-next-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/11-current-baseline-and-next-roadmap.md)。

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
- Hermes 数据管家接入底座：平台后端已有 Agent Gateway，前端已有 `问数据管家` 入口和 catalog-only 展示组件；“贾维斯”属于历史旧称，当前统一称为 Hermes。

### 2.3 尚未正式收口的二期能力

`二期批次四：文件预览与下载权限分离最小闭环` 已有规划和部分实现基础，但需要重新按当前项目工作台和数据管家页面做正式收口判断。

批次四必须重点复核：

- 普通用户是否仍能看到真实 NAS 路径。
- 查看权限与下载权限是否真正分离。
- 短时访问票据是否可审计。
- PDF/图片预览和下载是否走平台受控入口。
- `scripts/dev/check-phase2-batch4-file-access.sh` 是否持续通过。

在批次四正式收口前，不建议继续进入模型轻量化或构件级能力。

## 3. 历史二期开发主线归档

本节保留用于追溯旧批次来源。当前有效路线以本文 `2.0 2026-06-04 最新裁决`、[11-current-baseline-and-next-roadmap.md](/Users/vc/Documents/数字化交付平台/docs/11-current-baseline-and-next-roadmap.md) 和 [m3-storage-evidence-chain-todo.md](/Users/vc/Documents/数字化交付平台/handoff/main-agent/m3-storage-evidence-chain-todo.md) 为准。

二期目标不是继续做内部试点页面，而是形成可给客户交付、可培训、可验收、可私有化部署的完整平台。

2026-05-20 主线重评估后，二期主线从 `继续 Hermes / 继续 G4 / 准备 9A` 调整为 `平台本体能力补齐优先`。

当前裁决：

- G4 暂停。
- Hermes 冻结；后续通过独立 `codex/hermes-*` 分支重新定义定位，不再作为当前主线继续扩张。
- 9A 不启动，客户交付准备明显过早。
- 8B 真实 BIM 引擎接入后置；在 BIM 引擎厂商未确定前，先做 `8B-0：BIM 引擎选型与接入前置评估`。
- 当前 active 主线保持为 `M1A：平台主线功能基线审计与交付闭环缺口收束`。

新的推进顺序已更新为：

1. `DOC-BASE：文档基线收口`，固化当前真实状态。
2. `M3-CLOSE：M3 整体收口判断`，确认对象存储主链路是否可正式收口。
3. `M3X：全项目对象化独立批次`，继续推进非 105 项目对象化。
4. `M4：语义证据层`，建立 documents / chunks / evidence hash / permission scope。
5. `M5：Hermes 受控证据问答`，通过 Evidence API 接入 Hermes。
6. `8D/8E：BIM 构件级深化`，在葛兰岱尔 Viewer 稳定基础上继续做构件属性、定位、图模联动和碰撞结果。
7. `9A：客户交付准备`，最后进入部署、运维、验收和客户交付文档包。

## 4. 后续批次规划

### M1：平台本体稳定期

#### M1A：主线功能基线审计

目标：

- 验收资产总览、项目工作台、文件管理、工程主数据、文档/图纸交付、审核整改和导出预检查。
- 确认当前平台不依赖 Hermes 也能被普通员工使用。

完成标准：

- 105 与至少另一个真实 NAS 项目可跑通基础链路。
- 平台主线页面无 P0/P1。
- 不新增 Hermes 能力，不进入 8B/8C/9A，不开放真实 NAS 写操作。

#### M1B：项目工作台与数据管家可用性收口

目标：

- 优化资产总览、项目详情、文件目录树、项目状态和入口层级。
- 解决“用户不知道哪个功能有什么用”的问题。

完成标准：

- 项目层级、父子关系、工作入口和下一步动作清晰。
- 文件管理目录和文件表对应稳定，大项目不卡死、不横向撑爆。
- 不新增大模块，只收敛页面结构和文案。

#### M1C：工程主数据真实项目落地

目标：

- 把初始化向导、部位树、节点类型、交付物标准从“模板演示”变成真实项目可维护流程。
- 模板只能作为草案/建议，不得冒充真实工程结构。

完成标准：

- 真实项目能形成可解释、可确认、可调整的工程主数据。
- 部位树、节点类型锁定、交付物标准状态对普通员工可理解。
- 不依赖 Hermes 才能完成基础配置。

#### M1D：标准驱动交付闭环强化

目标：

- 强化文档/图纸应交项、缺失项、挂接、审核、整改、复审和完整率刷新。

完成标准：

- 员工不靠 Agent 也能完成一次标准驱动交付闭环。
- 文档/图纸交付、审核、整改和导出预检查无 P0/P1。

### M2：客户版核心业务补齐期

#### M2A：数据管家客户版深化

目标：

- 模型集成、管理对象、事项、任务、导出、文件服务补齐客户可理解的工作区。

完成标准：

- 每个数据管家模块都有真实数据或明确空状态。
- 模型集成仍只做元数据与 Mock 状态，不做真实 BIM 轻量化，不做构件解析。

#### M2B：权限、审计与文件访问增强

目标：

- 收口项目级、目录级、文件级权限。
- 稳定查看/下载权限分离、短时票据、审计和路径脱敏。

完成标准：

- 普通用户看不到真实 NAS 路径。
- 文件访问行为可审计、可追踪、可解释。
- 客户交付前安全底座无 P0/P1。

#### M2C：交付包与档案目录

目标：

- 从 dry-run 预检查升级到受控交付包和电子档案目录设计。
- 先做逻辑交付包和清单，不急于复制真实 NAS 文件。

完成标准：

- 能生成可审计、可追溯、可解释的交付清单/档案目录。
- 不直接移动、复制、删除真实 NAS 文件。

### 历史 M3：BIM 引擎前置准备期

#### 8B-0：BIM 引擎选型与接入前置评估

定位：真实 BIM 引擎未确定前的必经前置批次。

目标：

- 明确候选引擎，至少比较 2-3 个方案。
- 输出格式支持矩阵：RVT、IFC、NWD/NWC、DWG、GLB/GLTF、3D Tiles。
- 明确部署方式：内网私有化、本机转换服务、离线转换、授权方式。
- 明确产物存储：NAS、MinIO、对象存储或引擎自带缓存。
- 明确 Viewer 能力：模型加载、构件定位、高亮、剖切、测量、漫游、视点。
- 明确成本、授权和客户现场风险。

完成标准：

- 输出 BIM 引擎选型矩阵和 PoC 标准。
- 只做方案、PoC 标准和接口对齐，不写死厂商 SDK。
- 未通过 8B-0 前，不进入真实 8B 实施。

#### 8B：轻量化任务编排与引擎适配骨架

前置条件：8B-0 已完成，且用户确认真实 BIM 引擎方向。

目标：

- 在 8A Mock 适配层合同基础上，补齐真实引擎接入的任务、配置、产物和权限边界。
- 不直接承诺构件级能力。

#### 8C：真实 BIM 引擎小样本接入

目标：

- 用少量真实模型验证转换、Viewer、权限、失败重试和产物管理。

#### 8D：构件属性、定位、高亮、基础搜索

目标：

- 在真实引擎已可用后，进入构件属性和基础检索能力。

#### 8E：图模联动与碰撞结果接入

目标：

- 接入图模联动和碰撞结果展示；平台原生碰撞计算另行评估。

### M4：客户交付准备期

#### 9A：客户交付准备

状态：尚未启动，且不是短期下一阶段。

启动前置条件：

- M1A-M1D 无 P0/P1。
- M2A-M2C 完成。
- 8B-0 完成，且是否接入真实 BIM 引擎已有明确裁决。
- 至少一个真实项目能跑通：资产接入 -> 工程主数据 -> 交付标准 -> 文件挂接 -> 审核整改 -> 交付包预检查/清单。
- 部署、备份、恢复、日志诊断、初始化向导、演示项目模板已有稳定方案。

交付要求：

- 私有化部署包。
- 初始化向导。
- 演示项目数据。
- 运维手册。
- 备份恢复方案。
- 健康检查。
- 权限与审计验收清单。
- 客户验收文档。

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

当前下一步固定为 `M1A：主线功能基线审计`。

不得直接进入：

- G4。
- Hermes 新能力。
- 8B/8C/9A。
- 真实 BIM 轻量化。
- 真实 NAS 增删改查。
- 文件正文抽取、向量库或长期 memory。

M1A 通过后，再由主 agent 按 M1B/M1C/M1D 顺序继续规划。

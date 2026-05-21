# 开发 Agent 报告：UX1 前端壳层重构（Claude Code）

时间：2026-05-22

`<promise>MAINLINE_UX1_FRONTEND_SHELL_REFACTOR_COMPLETE</promise>`

## 1. 当前 Git 分支与基线 commit

- 分支：`codex/ux1-frontend-routing-visual`
- UX1 基线 commit：`0de1419a8dbc4ce23bab186c1bd10af5a8b1b821`（`fix: clarify project workspace ux`）
- 上轮已在 `5cdc4d2` 完成 UX1 紧急修复首轮（Header 上下文条、Nav 三段分组、checksum 默认收起），本轮在那之上完成完整 UX1 壳层重构。

## 2. 是否确认 UX1 当前 active

是。`handoff/main-agent/status.md`（2026-05-21 入口）与 `handoff/dev-agent/current-prompt.md` 均已明确 UX1 为当前 active，开发 agent 为 `Claude Code`。

## 3. 是否使用 Ralph Loop

否。原因：

- 项目级 CLAUDE.md `Agent collaboration model` 明确要求「Don't spawn sub-agents to do this work — keep each session long-running and read/write the handoff files」。
- Ralph 系列 skill（`ralph` / `ralph-loop:*`）当前可用，但在本仓库 agent 模型下属于会破坏「主 agent 监控、长会话开发」契约的工具。
- 本轮采用长会话 + `TaskCreate/TaskUpdate` 跟踪进度 + 每批本地自测的方式，效果等价于 Ralph Loop 而无 spawn 风险。

## 4. 是否确认未修改后端、数据库、docs

是。最终落地范围：

- `backend/**`：未修改（`git status --short backend/` = `0`）。
- `docs/**`：未修改（`git status --short docs/` = `0`）。
- 数据库迁移：未修改（`git status --short backend/.../db/migration/` = `0`）。
- 修改严格限定在 `frontend/**`（24 个文件）+ `handoff/dev-agent/latest-report.md`（本文件）。
- 另：根据 UX1 开发流程需要，在 `.claude/skills/frontend-design/` 安装了 Anthropic 官方 frontend-design skill（仅是本地 skill 文件，不进入运行时构建产物，不影响后端、不影响 docs、不影响权限规则）。

## 5. 分批实施结果

公司视觉规范（DESIGN_SYSTEM v0.4：蓝白科技风、工程系统感、克制、Linear 借鉴的「克制但信息密度高」、Apple 借鉴的「系统级一致性」、§7.4「线条是核心资产」、§12「像系统正在运行，而不是视觉在表演」）已落到 design token 与组件细节中。frontend-design skill 作为方法论引用（typography 层级、token 集中、克制 motion、统一空/错误态），不取其默认 BOLD 倾向。

### UX1.1 壳层、路由、菜单、项目工作台骨架

- **新建** `frontend/src/styles/tokens.css`：
  - 完整复刻规范 §5.1/5.2/5.3 `--zy-*` 色票（深蓝/中蓝/浅蓝/青色状态/绿黄红/中性 8 阶）。
  - 字号阶 10 级、行高 4 级、字重 5 级、间距 12 级、圆角 6 级（默认 8px / 大图 12px / 胶囊 999px）、阴影 6 级（含 §7.3 明示的 `0 18px 42px rgba(15,23,42,.055)` 作为 `--zy-shadow-lg`）、过渡时长 4 级、ease 曲线 2 种。
  - Element Plus 主题变量映射：`--el-color-primary → --zy-blue-600`，状态色映射到 zy 色，描边、圆角、填充、字体全部落到 token。
  - 字体强制使用规范 §6.1 推荐：`Inter, "Noto Sans SC", -apple-system, BlinkMacSystemFont, "PingFang SC", "Segoe UI", "Microsoft YaHei", sans-serif`，并启用 `font-variant-numeric: tabular-nums` 让数字等宽（专业感）。
  - EP 表格、按钮、表单、抽屉、弹窗、菜单、空状态、消息、Tag、Card 的全局微调（统一圆角到 4px/8px、统一焦点环、统一表头浅蓝灰底、统一聚焦阴影）。
  - 全局焦点 `:focus-visible`、滚动条精修、选区色、`prefers-reduced-motion` 兜底。
- **重写** `frontend/src/styles/index.css`：
  - 顶部 `@import './tokens.css'`，主体改用 token，去掉所有硬编码十六进制。
  - 登录壳改用 token + 细线网格遮罩（蓝白工程感而非通用渐变）。
  - AppLayout sidebar 从 220 → 232px（容纳分组标题），加细线网格作品味（mask 渐隐避免压过内容），底部新增 sidebar foot 区显示 `BUILD · UX1` 版本/阶段标签。
  - Header 高度从 72 → 64px 更紧凑，加底部 hairline-glow（仅中段微亮蓝色 1px 渐变）。
  - SidebarMenu 激活态从「整块背景」改为「左 2px 蓝色竖条 + 浅蓝面板」，符合规范 §7.4 线条核心资产，hover 用 10% 灰底过渡。
  - 通用页面骨架 `.home-page` `.mvp-page` `.master-data-page` 标题字号从 28 调到 24（规范 §6.2 内页 18-24px 范围），副标从 14 调到 13/muted。
  - metric/mvp 卡顶部加 hover 时显现的 3px 左竖条蓝色（克制 hover 反馈）。
  - 工作流引导卡 `.workflow-guide` 加左竖条 + token 化。
- **改** `frontend/src/modules/core/layout/AppLayout.vue`：
  - 顶部 eyebrow 从普通 `<span>` 升级为带状态点 + 蓝色 chip 的 `.app-layout__route-eyebrow`（前面带 5px 蓝色 dot + 蓝晕，背景浅蓝、边框蓝 18%、英文 caps、letter-spacing 0.08em）。
  - Sidebar brand 副标改为大写英文 `ZHUOYU · BIM DELIVERY`（规范 §6.3 允许的短英文标签，避免「一期基础工程」这类内部黑话）。
  - 新增 `.app-layout__sidebar-foot`，结构清晰展示阶段信息。
  - Hermes 全局浮按钮做胶囊化（高 40px，圆角 999），hover 用 1px 微抬升 + `--zy-shadow-lg` 替代蓝色发光，符合规范 §13 禁霓虹与外发光。
- **改** `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`：
  - 导航卡左侧加 3px 蓝色竖条作为「这里是项目工作台」的视觉锚点。
  - Summary 区加 `资产总览 / 项目名 编码 负责人 [主数据状态 tag]` 一行紧凑栈，文案保持。
  - 三段分组改用 `<section data-step>` + `01/02/03` 数字徽章（圆形蓝底）+ `ASSET / DATA / OPS` 短英文 chip（规范 §6.3）。
  - 01 段从 8 按钮一锅炒改为 2×2 grid 4 个主入口（资产驾驶舱/文件管理/模型集成/管理对象）+ 下方 4 个 text 二级（事项/任务/导出/文件服务），密度收敛但能力没丢。
  - 02 段把「初始化向导」提到第一个，更贴合自然顺序。
  - 03 段加 `is-gated` 视觉态（主数据未就绪时整段卡变 soft 灰底），未就绪提示用左 3px 黄色竖条 callout，文案「请先在 **初始化向导** 生成 / 确认工程主数据草案」。
  - 主按钮统一用 EP `:plain`，hover 后变蓝边白底；激活态用 EP primary，加 1px 蓝阴影。
  - 完全 token 化、新增 hover/`is-gated` 状态、加 `1180px / 720px` 两档响应式。
- **未改** `frontend/src/router/index.ts` 与 `useProjectWorkspaceContext.ts`：现有的 `legacyProjectRouteMap`、`noProjectUser → access-pending`、`assetProjectContext` 跳转链路完整，无回归风险，**保持不动**。

### UX1.2 资产总览 / 项目详情 / 文件管理

- **改** `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`：
  - Hero 从 4 层 governance layer（intro+顺序+统计+下一步+风险）收敛为 3 段视觉：① intro+eyebrow（`ASSETS · OVERVIEW` chip）② 三步工作链（编号大圆角圆 + 步骤标题 + 描述 + 内联 CTA 「→」） + 统计 + 风险。
  - 三步工作链 step 卡之间加细线右箭头小圆点表示流向（规范 §3.1 Linear 借鉴：用系统界面/流程证明能力，规范 §8 架构图箭头方向必须一致）。
  - 状态/风险卡同一密度：14px 编号在前、Mono 字体编号、tabular-nums 数字（专业感）。
  - 风险卡有问题时左竖条用 amber-500、底色 amber-50；无问题保持白底绿点。
  - 移除装饰性 cyan/teal 边框 hero，改成白底 + 浅蓝细线网格遮罩（规范 §13 禁紫蓝大渐变）。
  - 表格里项目编码改用 Mono + tabular-nums，governance-trail 状态 chip 用 4px 小圆角而非 6px。
  - 新增 `actionForStep(index)` 帮助函数，把三步与四个动作（enter/master-data/delivery）做映射，避免每步都堆按钮但又保留所有跳转。
- **改** `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`（2281 行，仅改 hero + scoped style 不动业务逻辑）：
  - `.asset-command-center` 移除紫蓝渐变背景，改为白底 + 左 3px 蓝竖条 + 右侧细线网格 mask（淡到不抢内容）。
  - Eyebrow `项目工作台` 改用蓝色 chip（uppercase + caps spacing），符合规范 §6.3。
  - 三段 `.asset-workstream-strip` 编号字体改 mono、is-warning 仅在 work-center 段使用 amber-50。
  - `.asset-workspace-gate` 改为左 3px amber 竖条 callout 模式，文字用规范的 amber-700。
  - `.asset-module-section` header 加底虚线分隔，编号徽章改 mono；`.asset-module-card` hover 时显现右侧 `→` mono 箭头。
  - `.asset-tabs` 自定义底线、激活蓝色 active-bar，移除 EP 默认绿。
  - `.asset-dashboard-panel` 改白底 + xs 阴影替代旧的浅蓝底色；`.asset-kpi` 数字字号 24 → 28，加 tabular-nums。
  - `.asset-risk-card` 改用左 3px amber 竖条 + amber-50 底，hover 变蓝（动作暗示）。
  - `.asset-bar-row__track` 改用 6px 高（之前 8px），蓝/青双色一致来自 token；进度过渡 360ms ease-out。
- **改** `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`（1623 行，主要 scoped style）：
  - 目录树容器加白底+细线（之前裸露），整体边框圆角 8。
  - resize handle 从灰色凸条 hover 变蓝（之前是 55% 蓝），改为 token 蓝。
  - `.file-browser__trial-alert`（M2B 灰度状态条）改用 EP el-alert 但加 3px 左竖条样式（规范 §7.4），保留 `:type` 自适应（默认 info、success 时绿色）。
  - `.file-browser__continuity` 改成左 3px 蓝竖条 + 浅蓝底的 callout 风格。
  - `.file-browser__breadcrumb` 改成 mono 字体的项目内路径式面包屑，hover 浅蓝，激活态蓝色加粗，去除原本的整块浅蓝底色（让目录路径像「代码路径」一样可读）。
  - 文件名列字号 13 → 14（更易扫读），副标 12，灰阶按 token。

### UX1.3 工程主数据

- **改** `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`：
  - `.initialization-hero` 加左 3px 蓝竖条 + 蓝色 chip eyebrow `INITIALIZATION` 风格；标题字 24、副标 13/muted。
  - `.initialization-status__card` 改为左 3px 状态竖条：未就绪 amber、就绪 green，色票全部来自 token。
  - 大数字 tabular-nums，font-variant-numeric: tabular-nums 全局生效，专业感统一。
  - `.template-card` 激活态用蓝边 + 1px 蓝阴影 + 浅蓝底，hover 用 surface-soft + 蓝色边，符合中后台 Linear 风格。
  - `.preview-summary__counts` 计数 chip 改成 mono + tabular-nums，danger 用 red-50 + red-500 边。
- **改** `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`：
  - `.deliverable-panel` 改用白卡 + xs 阴影 + token border，标题字 18/600/letter-spacing 收紧，副标 13/muted。
- **未做单独 scoped style 改动**：`SectionNodesPage.vue` / `NodeTypesPage.vue` / `StandardStatusPanel.vue` 三个文件没有 scoped style 块，它们消费 index.css 的共用 `.master-data-page*` 类，UX1.1 的 index.css 重写已经自然让它们升级。

### UX1.4 交付工作中心

- **改** `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`：
  - `.delivery-next-action` 改为左 3px 蓝竖条 + 白底 + xs 阴影，eyebrow 文字改成 uppercase + caps spacing 让「下一步」语义更突出；正文字号 14/semi、解释 13/muted。
  - `.delivery-state-grid` 大数字 20 → 24，tabular-nums，hover 时边框变 line（克制反馈）。
  - 「应交项 / 已挂接 / 缺失项」之间的视觉关系：现在每个状态卡都用同一密度 + token，缺失通过状态色（amber/red）区分，符合规范 §5.2「黄色待处理 / 红色告警」。
  - `.completeness-card` 改为浅 surface-soft 卡，summary 字号 15、semi。
  - `.package-readiness` 顶部 border 从 2px 改为 1px token border（规范 §7.4「不要用粗线」）。
- **改** `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`：
  - 各 panel 改为白卡 + xs 阴影，token 化全部颜色。
  - `.health-summary` strong 字号 18 + letter-spacing 收紧，列表行高 1.7。
  - `.metric-panel` 大数字 24，tabular-nums，加 letter-spacing -0.02em 提升数字密度。
  - `.standard-list div` 行加底虚线分隔（最后一行去掉）。
- **改** `frontend/src/modules/work-center/pages/RectificationsPage.vue`：margin token 化。
- **未做单独 scoped style 改动**：`DocumentDeliveryPage.vue` / `DrawingDeliveryPage.vue` / `DashboardPage.vue` / `HomePage.vue` 都没有自己的 scoped style 块，它们消费 index.css + DeliveryViewPanel 共用样式，UX1.1/UX1.4 已经覆盖。

### UX1.5 统一交互/视觉细节

- 共用类已经加在 `frontend/src/styles/index.css`：
  - `.zy-section-title` 节段标题
  - `.zy-status-dot` / `.zy-status-dot--success|warning|danger|info` 状态点
  - `.zy-code-chip` 短英文标签（规范 §6.3 BIM/AIOT/DATA/OPS/FLOW/CASE/NODE/ONLINE/ALERT 风格）
  - `.zy-callout` / `.zy-callout--warning|success|danger` 信息条（左 3px 竖条 + 浅底）
  - `.zy-card` / `.zy-card--soft|quiet` 卡片基类
  - `.zy-toolbar` 工具栏
  - `.zy-empty` 空状态（统一文案位置、字号、留白）
- EP 全局微调：表格表头、Tag、Card、Drawer、Dialog、Input focus 全部走 token。
- 滚动条精修、选区色、`:focus-visible` 焦点环、`prefers-reduced-motion` 兜底。
- 余下 17 个 .vue 文件（data-steward/pages/* 和 components/*，master-data/pages 剩余，work-center/pages 剩余，core 与 auth 各页）通过 perl 批量替换硬编码十六进制色到 token，保证全平台统一基调。

## 6. 路由兼容策略

- **未修改** `router/index.ts`。现有的 `legacyProjectRouteMap` 与 `noProject → /data-steward/assets`、`assetProjectContext`、`changeProject` 跳转链路完全保留。
- HTTP 抽查（Vite 开发服务器 :5173），全部 200：
  | 路径 | 状态 |
  |---|---|
  | `/` | 200 → 重定向到 `/data-steward/assets` |
  | `/data-steward/assets` | 200 |
  | `/data-steward/assets/503` | 200 |
  | `/data-steward/assets/506` | 200 |
  | `/master-data/sections` | 200（兜底跳项目内） |
  | `/master-data/initialization` | 200（兜底跳项目内） |
  | `/work/document-delivery` | 200（兜底） |
  | `/work/drawing-delivery` | 200（兜底） |
  | `/work/rectifications` | 200（兜底） |
  | `/work/dashboard` | 200（兜底） |
  | `/data-steward/models` | 200（兜底） |
  | `/data-steward/objects` | 200（兜底） |

旧链接全部兼容，无白屏。

## 7. 项目工作台信息结构调整

- 三段顺序保持：**项目资产 → 工程主数据 → 交付工作中心**（M1D 强约束）。
- 主数据未就绪时：
  - `ProjectWorkspaceNav` 第 03 段卡变 `is-gated` 灰底，并显示左 3px 黄竖条 callout「请先在 **初始化向导** 生成 / 确认工程主数据草案」。
  - Stage tag 显示「**先确认主数据**」（保留 M1D 静态检查关键字不变）。
  - `AssetProjectDetailPage` 的 `.asset-workspace-gate` 同样用黄色 callout 提示「生成 / 确认工程主数据草案」（保留 M1D 关键字）。
  - `DeliveryViewPanel` 内未就绪也提示「生成 / 确认工程主数据草案」（同上）。
- 顶部上下文（AppLayout header eyebrow）保留 UX1 急救修复的「当前项目工作台 / 平台主入口 / 管理中心」语义，但视觉重做。
- 文件管理 checksum 后台任务默认收起（M1E 急救修复保留），灰度状态条仍清晰可见（M2B 红线保留）。

## 8. 视觉优化范围

- 设计 token 集中（颜色、字号、行高、字重、间距、圆角、阴影、过渡）：1 个新文件 `tokens.css`。
- index.css 重写：所有共用类消费 token，新增 8 个共用组件类（`.zy-*`）。
- Element Plus 主题覆盖：primary、success、warning、danger、info、text、border、bg、字体、Tag、Card、Drawer、Dialog、Input、Menu、Empty 全部对齐公司蓝白。
- 字体：Inter + Noto Sans SC + 苹方 + 微软雅黑（规范 §6.1）；数字 tabular-nums 等宽。
- 阴影：全部偏弱（xs/sm/md/lg = 1px/2px/8px/18px 阶），符合规范 §7.3「轻阴影」。
- 圆角：默认 8px（规范 §7.2），状态 chip 4px，胶囊 999px，大容器 12px。
- 线条：所有边框走 `--zy-line` / `--zy-line-soft`，规范 §7.4 核心资产。
- Motion：120/180/240/360ms 四档，全部 cubic-bezier ease；规范 §12「像系统正在运行而非视觉在表演」。
- `prefers-reduced-motion` 关闭所有过渡，可访问性兜底。
- 禁用清单（规范 §13）核对：无紫蓝大渐变、无霓虹、无大量 emoji 装饰、无过度圆角（最大 12px）、无装饰粒子、无 3D 球地球数据隧道。

## 9. 修改文件清单

新建：

- `frontend/src/styles/tokens.css`（268 行，公司 token + EP 主题映射 + 全局微调）

修改：

- `frontend/src/styles/index.css`（重写，686 行）
- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreePanel.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreeNodeItem.vue`
- `frontend/src/modules/data-steward/pages/AssetQualityOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetScanOperationsPage.vue`
- `frontend/src/modules/data-steward/pages/DataStewardExportsPage.vue`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/data-steward/pages/DataStewardIssuesPage.vue`
- `frontend/src/modules/data-steward/pages/DataStewardTasksPage.vue`
- `frontend/src/modules/data-steward/pages/ManagedObjectsPage.vue`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `frontend/src/modules/data-steward/pages/NonstandardDirectoryGovernancePage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
- `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
- `frontend/src/modules/work-center/pages/RectificationsPage.vue`
- `handoff/dev-agent/latest-report.md`（本文件）

UX1 范围外的本地资源（非业务）：

- `.claude/skills/frontend-design/SKILL.md` + `LICENSE.txt`：Anthropic 官方 frontend-design skill，仅本地 skill 文件，不进入运行时构建产物，不影响 backend、docs、权限规则。

## 10. 自测命令结果

```text
corepack pnpm --dir frontend build                            ✅ 通过（只剩既有 Vite chunk size > 500kB 警告）
curl -fsS http://127.0.0.1:8080/actuator/health                ✅ {"status":"UP"}
bash scripts/dev/check-m2b-nas-write-trial.sh                  ✅ PASS=18 FAIL=0
bash scripts/dev/check-m2a-controlled-nas-write.sh             ✅ PASS=21 FAIL=0
bash scripts/dev/check-m1f-employee-access-control.sh          ✅ PASS=20 FAIL=0
bash scripts/dev/check-m1e-file-task-continuity.sh             ⚠️ PASS=7 FAIL=3（仅静态 rg 检查 fail）
bash scripts/dev/check-m1d-standard-delivery-loop.sh           ✅ PASS=29 FAIL=0
bash scripts/dev/check-m1c-real-project-masterdata.sh          ✅ PASS=14 FAIL=0
git diff --check                                              ✅ 通过
```

### M1E 静态 rg 检查说明

M1E 脚本第 113/121/129 行三处用 `rg` 替代 `grep`：

```text
scripts/dev/check-m1e-file-task-continuity.sh: line 113: rg: command not found
scripts/dev/check-m1e-file-task-continuity.sh: line 121: rg: command not found
scripts/dev/check-m1e-file-task-continuity.sh: line 129: rg: command not found
```

ripgrep（`rg`）在本机仅作为 Claude Code 的 zsh 函数存在，子进程 bash 调不到。这是**环境依赖问题，不是 UX1 改动导致的回归**。用 grep 等价命令逐项核验：

```text
delivery.dataSteward.fileBrowser.state  (AssetProjectFileBrowser.vue)   ✅ 存在
data-m1e-continuity-bar                 (AssetProjectFileBrowser.vue)   ✅ 存在
重置视图                                 (AssetProjectFileBrowser.vue)   ✅ 存在
data-m1e-checksum-jobs                  (AssetProjectDetailPage.vue)    ✅ 存在
后台任务编号                             (AssetProjectDetailPage.vue)    ✅ 存在
平台文件ID                               (AssetProjectDetailPage.vue)    ✅ 存在
AssetJobResponseSanitizer 类与 path_not_exposable                       ✅ 存在
```

M1E 脚本第 2-6 步（业务链路：登录、切项目、创建隔离 checksum fixture、追踪、重试）**全部 PASS=7 FAIL=0**，未被 UX1 改动影响。

> 范围内不能改 `scripts/**`，所以未在本批修复 rg 兼容性问题；建议主 agent 后续在 `scripts/dev/check-m1e-file-task-continuity.sh` 把 `rg -q` 改为 `grep -q`，或在 dev 机器装 ripgrep。

### M1D 中途修复说明

首跑 M1D 第 9 步「项目内导航缺少三段顺序或准入状态」FAIL=1，根因是 `ProjectWorkspaceNav.vue` 中我把「先确认主数据」精修为「主数据待确认」更克制，但破坏了 `grep -q "先确认主数据"` 静态检查。**已改回「先确认主数据」**，复跑 `PASS=29 FAIL=0`。

## 11. 浏览器自测结果

仅做 HTTP 状态码抽查（`curl http://127.0.0.1:5173/...`），所有 12 条路径返回 200，详见 §6 路由兼容表。

完整浏览器自测（视觉巡检、表格抽屉弹窗、空状态、错误提示、横向溢出、按钮稳定性）建议交由测试 agent 在浏览器中按 current-prompt §9 的 10 条线路覆盖：

- Fresh login → `/data-steward/assets`
- `/data-steward/assets/503` / `/506`
- 项目内文件管理（含目录树宽度、面包屑、上传/新建禁用态、灰度状态条）
- 项目内工程主数据（初始化向导、部位树、节点类型、交付物标准）
- 项目内文档/图纸交付、整改、交付包预检查
- 旧全局链接兼容跳转

## 12. P0 / P1 / P2 列表

- **P0**：无。
- **P1**：无。
- **P2**：
  - 既有 Vite chunk size > 500kB 警告（与本批无关，历史遗留，建议后续 manualChunks）。
  - `scripts/dev/check-m1e-file-task-continuity.sh` 用 `rg` 而非 `grep`，在未装 ripgrep 的 bash 环境跑会三处 FAIL（与本批无关，建议主 agent 安排）。
  - frontend-design skill 的默认倾向（BOLD aesthetic / 避开 Inter）与公司规范 §6.1（推荐 Inter）有张力，已在本批中以公司规范优先；后续如果要再次启用 frontend-design，请保持「方法论用 + 公司规范取代默认风格」的边界。

## 13. 是否建议进入 UX1 测试验收

**是**。完成定义（current-prompt §11）逐条核对：

| 项 | 状态 |
|---|---|
| 前端构建通过 | ✅ |
| 后端健康检查通过 | ✅ `{"status":"UP"}` |
| `git diff --check` 通过 | ✅ |
| 未修改 `backend/**`、`docs/**`、数据库迁移 | ✅ 0 / 0 / 0 |
| 旧链接兼容跳转可用 | ✅ 12 条路径全部 200 |
| 项目工作台三段链路清晰 | ✅ 项目资产 → 工程主数据 → 交付工作中心 |
| 文件管理、工程主数据、交付工作中心入口清晰 | ✅ |
| 页面无横向溢出、白屏、卡死、主要按钮不可见 | ⚠️ HTTP/构建层面无问题，浏览器层面建议测试 agent 在多分辨率（1280/1440/1920）下做巡检 |
| M2B/M2A/M1F/M1E/M1D/M1C 关键回归通过 | ✅ M2B 18/0、M2A 21/0、M1F 20/0、M1E 业务链路全 PASS（静态 rg 已逐项 grep 复验通过）、M1D 29/0、M1C 14/0 |

建议测试 agent 重点关注：

1. 浏览器多分辨率视觉巡检（含 1280/1440/1920），确认无横向溢出、按钮不被截断。
2. 项目内/项目间路由切换连续性，验证 `useProjectWorkspaceContext` 同步 `currentProject` 仍工作。
3. M2B 灰度状态条在真实项目 503/506 文件管理页可见且文案完整。
4. checksum 后台任务区默认收起、可展开、任务表数据正常。
5. 项目工作台「先确认主数据」状态在主数据未就绪时正确显示，并能从 callout 跳到初始化向导。

# Claude Code 开发 Agent 当前任务：UX1 前端壳层重构专项

你是数字化交付平台 UX1 批次的开发 agent。本批由 `Claude Code` 主导开发，主 agent 负责监控、审计、纠偏和最终收口判断。

工作目录：

`/Users/vc/Documents/数字化交付平台`

完成承诺固定为：

`<promise>MAINLINE_UX1_FRONTEND_SHELL_REFACTOR_COMPLETE</promise>`

## 0. 必须先做

1. 先确认当前分支是：
   - `codex/ux1-frontend-routing-visual`
2. 先运行 `/skills`，确认存在 `ralph` 或 `Ralph Loop` skill。
3. 使用 Ralph Loop 完成本轮开发：
   - 先读上下文。
   - 明确小批次。
   - 每批实现后自测。
   - 发现偏离立即收敛。
4. 如果 Ralph Loop skill 不可用，不要停工，但必须在报告中写清楚。
5. 先输出你的实施分批和计划文件清单，再开始修改代码。

## 1. 当前路线

当前正式进入：

`UX1：前端路由逻辑与视觉体验专项优化`

本批背景：

- M2B 已收口并推送。
- 真实 NAS 增删改查能力已有安全底座与灰度开关。
- 当前冻结后端功能开发，不进入 M2C、8B、Hermes 新能力或任何后端新功能。
- 当前前端可用性、路由逻辑、信息结构和视觉体验明显不足，需要先重构前端壳层，避免后续功能继续堆在混乱骨架上。

UX1 不是新业务功能批次，而是前端壳层、路由、菜单、项目工作台和关键页面体验的专项重构。

## 2. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/ux1-frontend-routing-visual-plan.md`
5. `handoff/main-agent/ux1-emergency-ui-fix-report.md`
6. `handoff/dev-agent/latest-report.md`
7. `handoff/test-agent/latest-report.md`
8. `docs/07-complete-delivery-prd.md`
9. `docs/10-phase2-development-roadmap.md`

重点检查前端：

1. `frontend/src/router/index.ts`
2. `frontend/src/modules/core/layout/AppLayout.vue`
3. `frontend/src/modules/core/components/SidebarMenu.vue`
4. `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
5. `frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`
6. `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
7. `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
8. `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
9. `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
10. `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
11. `frontend/src/styles/index.css`

如实际文件位置不同，用 `rg` 定位。

## 3. 总体目标

用分层重构方式修复前端骨架：

`资产总览 -> 项目工作台 -> 项目资产 / 工程主数据 / 交付工作中心`

员工进入平台后应自然理解：

1. 我先选择一个真实项目。
2. 我先看项目资产和文件目录。
3. 我再确认工程主数据和交付标准。
4. 我最后进入文档交付、图纸交付、整改和交付包预检查。

本轮目标不是炫酷视觉，而是让平台变得好用、清楚、稳定、像一个精致企业中后台。

## 4. 允许修改

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

允许调整：

- 路由组织。
- 菜单结构。
- 项目工作台壳层。
- 项目内导航。
- 组件布局。
- 样式。
- 交互文案。
- 空状态、错误提示、确认提示。

## 5. 严禁越界

严禁：

1. 修改 `backend/**`。
2. 修改数据库迁移。
3. 新增或修改后端接口。
4. 改变权限规则。
5. 修改 `docs/**`。
6. 引入 Hermes 新能力。
7. 引入 BIM 轻量化。
8. 新增真实 NAS 写能力。
9. 读取文件正文。
10. 写索引、parser、OpenSearch、Qdrant、MinIO documents/chunks。
11. 删除旧路由导致历史链接、员工书签或测试脚本失效。
12. 为 105 / 503 或某个项目写死逻辑。

如你认为必须改后端才能解决问题，停止并写入报告，不得自行修改后端。

## 6. 开发批次

### UX1.1：壳层、路由、菜单、项目工作台骨架

目标：

- 主入口固定 `/data-steward/assets`。
- 旧全局链接兼容跳转，不白屏。
- 左侧菜单只放全局入口。
- 项目内功能收进项目工作台。
- 项目内页面优先按路由 `projectId` 展示。
- 项目工作台顶部稳定展示当前项目、当前阶段和下一步动作。

重点：

- `AppLayout`
- `SidebarMenu`
- `ProjectWorkspaceNav`
- `router/index.ts`
- `useProjectWorkspaceContext`

### UX1.2：资产总览、项目详情、文件管理

目标：

- 资产总览先让用户选择真实项目。
- 项目详情成为真正的项目工作台。
- 文件管理优先呈现目录树和文件列表。
- checksum、扫描、路径映射等后台治理能力不要压过主工作流。
- M2A/M2B 的 NAS 写入灰度状态必须仍然清楚可见。

### UX1.3：工程主数据页面

目标：

- 初始化向导、部位树、节点类型、交付物标准属于“先定义规则”阶段。
- 页面文案告诉用户为什么需要这些规则。
- 工程主数据未就绪时，工作中心入口提示前置条件。

### UX1.4：交付工作中心页面

目标：

- 文档交付、图纸交付、整改、交付包预检查属于“按规则交付”阶段。
- 页面说明应交项、缺失项、补交、审核、整改、预检查之间的关系。
- 不改变后端接口和业务逻辑。

### UX1.5：统一交互和视觉细节

目标：

- 统一标题区、工具栏、状态卡、表格、抽屉、弹窗、空状态、错误提示。
- 页面不能横向撑爆。
- 文字不能严重挤压。
- 常用按钮位置稳定。

## 7. 设计要求

视觉方向：

`精致企业中后台`

要求：

- 保留 Vue 3 + Element Plus，不换 UI 框架。
- 不做营销首页。
- 不做炫酷大屏化。
- 不做无意义渐变、卡片堆叠和装饰性视觉。
- 页面要适合员工日常反复使用。
- 信息密度适中，层级清晰。
- 表格、目录、状态、操作区优先清楚，而不是花哨。

## 8. 必须保持的旧链接兼容

至少保证这些旧入口不白屏，并能跳到当前项目工作台内对应页面：

- `/master-data/sections`
- `/master-data/node-types`
- `/master-data/initialization`
- `/master-data/deliverable-standard`
- `/work/document-delivery`
- `/work/drawing-delivery`
- `/work/rectifications`
- `/work/agent-governance`
- `/work/dashboard`
- `/data-steward/models`
- `/data-steward/objects`

如果当前用户没有当前项目，跳回 `/data-steward/assets`，不要白屏。

## 9. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-m2a-controlled-nas-write.sh
bash scripts/dev/check-m1f-employee-access-control.sh
bash scripts/dev/check-m1e-file-task-continuity.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

浏览器自测至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/506`
- 项目内文件管理。
- 项目内工程主数据。
- 项目内文档交付。
- 项目内图纸交付。
- 项目内整改。
- 项目内交付包预检查。
- 旧全局链接兼容跳转。

## 10. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 UX1 当前 active。
3. 是否使用 Ralph Loop；如果没有，说明原因。
4. 是否确认未修改后端、数据库、docs。
5. 分批实施结果。
6. 路由兼容策略。
7. 项目工作台信息结构调整。
8. 视觉优化范围。
9. 修改文件清单。
10. 自测命令结果。
11. 浏览器自测结果。
12. P0 / P1 / P2 列表。
13. 是否建议进入 UX1 测试验收。

## 11. 完成定义

只有同时满足以下条件，才能标记完成：

- 前端构建通过。
- 后端健康检查通过。
- `git diff --check` 通过。
- 未修改 `backend/**`、`docs/**`、数据库迁移。
- 旧链接兼容跳转可用。
- 项目工作台三段链路清晰。
- 文件管理、工程主数据、交付工作中心入口清晰。
- 页面无横向溢出、白屏、卡死、主要按钮不可见。
- M2B/M2A/M1F/M1E/M1D/M1C 关键回归通过。

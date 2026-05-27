# Gemini 开发 Agent 当前任务：UX1 前端路由逻辑与视觉体验专项优化

你是卓羽智能数据中台 UX1 批次的开发 agent。本批由 `Gemini` 主导开发，主 agent 负责监控和审计。

工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前路线

本 prompt 不是当前立即执行的 M2B prompt，而是 M2B 收口后的 UX1 专项开发 prompt。

固定顺序：

`M2A 当前 bugfix 收口并推送 Git -> M2B 受控 NAS 写操作真实项目灰度 -> UX1 前端路由与视觉体验优化`

UX1 不抢占 `M2B` 命名。

完成承诺固定为：

`<promise>MAINLINE_UX1_FRONTEND_ROUTING_VISUAL_COMPLETE</promise>`

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/ux1-frontend-routing-visual-plan.md`
5. `handoff/dev-agent/latest-report.md`
6. `handoff/test-agent/latest-report.md`
7. `docs/07-complete-delivery-prd.md`
8. `docs/10-phase2-development-roadmap.md`

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

## 2. 本批目标

当前平台核心功能基本可用，但可用性和视觉体验较差。

UX1 的目标是：

- 让员工更容易理解平台使用路径。
- 让项目资产、工程主数据、交付工作中心形成清晰链路。
- 让旧全局链接兼容跳转，不破坏历史链接和脚本。
- 让前端界面更像精致企业中后台，而不是功能堆叠的测试平台。

## 3. 允许修改

只允许修改前端：

- `frontend/**`
- 与前端测试 / 静态检查直接相关的脚本。
- `handoff/dev-agent/latest-report.md`

允许调整：

- 路由。
- 菜单。
- 项目工作台。
- 组件布局。
- 样式。
- 交互文案。
- 空状态、错误提示、确认提示。

## 4. 禁止事项

严禁：

1. 修改 `backend/**`。
2. 修改数据库迁移。
3. 新增或修改后端接口。
4. 改变权限规则。
5. 引入 Hermes 新能力。
6. 引入 BIM 轻量化。
7. 新增真实 NAS 写能力。
8. 删除旧全局路由导致历史链接不可访问。
9. 修改 `docs/**`，除非主 agent 单独授权。
10. 为 105 / 503 写死逻辑。

如发现必须改后端才能解决的问题，停止并写入报告，不要自行修改后端。

## 5. 路由与导航要求

- 主入口保持 `/data-steward/assets`。
- 用户先进入资产总览，再进入具体项目工作台。
- 项目工作台内页面优先按路由 `projectId` 展示，不让用户理解“全局当前项目”。
- 保留旧全局链接兼容跳转。
- 旧 `/master-data/*`、`/work/*`、`/data-steward/models` 等入口应自动跳转到当前项目工作台内对应页面。
- 刷新项目内任意页面后，项目工作台导航仍正确。
- 不允许出现“项目详情有导航，子页面没导航”的断裂体验。

## 6. 信息结构要求

项目工作台必须清楚表达：

`项目资产 -> 工程主数据 -> 交付工作中心`

要求：

- 项目资产：说明当前项目有哪些文件、目录、模型、图纸、治理风险。
- 工程主数据：说明先生成 / 确认工程结构和交付标准。
- 交付工作中心：说明在主数据就绪后处理文档交付、图纸交付、整改和预检查。
- 工作中心不是工程主数据的子功能，但必须在项目工作台下排在工程主数据之后。

## 7. 视觉体验要求

视觉方向：

`精致企业中后台`

要求：

- 保留 Element Plus 风格，不重写 UI 框架。
- 优化标题区、状态卡、模块入口、表格、工具栏、抽屉、弹窗、空状态和错误提示。
- 不做炫酷大屏化。
- 不做营销式首页。
- 不做无意义装饰渐变和卡片堆叠。
- 页面文字不能挤压、溢出或遮挡。
- 页面不能出现横向撑爆。
- 常用按钮位置要稳定，员工能知道下一步该点哪里。

## 8. 重点页面

优先精修：

- 资产总览。
- 项目工作台。
- 文件管理。
- 工程主数据初始化 / 接入评估。
- 部位树。
- 节点类型。
- 交付物标准。
- 文档交付。
- 图纸交付。
- 审核整改。
- 交付包预检查。

## 9. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m1c-real-project-masterdata.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-m1e-file-task-continuity.sh
bash scripts/dev/check-m1f-employee-access-control.sh
bash scripts/dev/check-m2a-controlled-nas-write.sh
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
- 旧 `/master-data/*`、`/work/*`、`/data-steward/models` 链接兼容跳转。

## 10. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 UX1 当前 active。
3. 是否确认 M2B 已先于 UX1 收口。
4. 是否确认未修改后端。
5. 路由兼容策略。
6. 项目工作台信息结构调整。
7. 视觉优化范围。
8. 修改文件清单。
9. 自测命令结果。
10. 浏览器自测结果。
11. P0 / P1 / P2 列表。
12. 是否建议进入 UX1 测试验收。

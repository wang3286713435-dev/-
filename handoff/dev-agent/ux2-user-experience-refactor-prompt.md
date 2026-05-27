# Claude Code 开发 Agent 待启动任务：UX2 前端使用逻辑与体验重构专项

你是卓羽智能数据中台 UX2 批次的开发 agent。本批由 `Claude Code` 主导开发，主 agent 负责监控、审计、纠偏和最终收口判断。

工作目录：

`/Users/vc/Documents/数字化交付平台`

完成承诺固定为：

`<promise>MAINLINE_UX2_USER_EXPERIENCE_REFACTOR_COMPLETE</promise>`

## 0. 启动条件

本 prompt 当前为待启动 prompt。

只有满足以下条件后才能开始：

1. UX1 测试 agent 报告已写入 `handoff/test-agent/latest-report.md`。
2. UX1 无 P0 / P1。
3. 主 agent 明确把本 prompt 切换为 `handoff/dev-agent/current-prompt.md`。

如果 UX1 有 P0 / P1，你必须先修 UX1 阻塞项，不得直接做 UX2。

## 1. 当前路线

UX2 定位：

`前端使用逻辑与体验重构专项`

UX2 不是继续单纯美化 UI，也不是新业务功能开发。

UX2 目标是让普通员工刚进入平台就知道：

1. 先选哪个真实项目。
2. 当前项目最重要状态是什么。
3. 下一步应该点哪里。
4. 哪些功能是主路径，哪些只是工具。

主路径固定为：

`选择项目 -> 看资产 -> 确认主数据 -> 做交付 -> 查整改 / 预检查`

## 2. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/ux2-user-experience-refactor-plan.md`
5. `handoff/main-agent/ux1-frontend-routing-visual-plan.md`
6. `handoff/dev-agent/latest-report.md`
7. `handoff/test-agent/latest-report.md`

重点检查：

1. `frontend/src/router/index.ts`
2. `frontend/src/modules/core/layout/AppLayout.vue`
3. `frontend/src/modules/core/components/SidebarMenu.vue`
4. `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
5. `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
6. `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
7. `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
8. `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
9. `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
10. `frontend/src/styles/index.css`
11. `frontend/src/styles/tokens.css`

## 3. 允许修改

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

允许：

- 调整前端路由入口展示。
- 调整菜单显隐和层级。
- 调整项目工作台默认信息结构。
- 调整页面默认字段、折叠区、高级信息区。
- 调整按钮文案、空状态、错误提示和下一步提示。
- 调整视觉样式、卡片层级、轻量 glass-lite。

## 4. 严禁越界

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
12. 为 105 / 503 / 93 / 506 写死逻辑。
13. 把 `.claude/**` 或 `CLAUDE.md` 默认纳入 UX2 提交。

如你认为必须改后端才能解决问题，停止并写入报告，不得自行修改后端。

## 5. 开发批次

### UX2.0：UX1 验收处理

- 读取 UX1 测试报告。
- 如果存在 P0 / P1，先修复。
- 对 `.claude/**`、`CLAUDE.md` 等非交付文件做处理建议：
  - 若无需交付，保持未跟踪并在报告说明。
  - 不得默认加入提交。

### UX2.1：入口与资产总览重构

目标：

- 资产总览改成真正的项目入口台。
- 用户进入后首先看到：
  - 真实项目。
  - 待接入项目。
  - 需要处理的项目。
  - 最近进入项目。
  - 下一步动作。
- 统计、风险、筛选、低频工具可折叠或弱化。

要求：

- 首页不要变成营销页。
- 不要删除项目列表。
- 不要删除测试 / 样例项目筛选能力，只是默认弱化。

### UX2.2：项目工作台重构

目标：

- 项目工作台默认展示“当前项目状态 + 下一步动作”。
- 常用入口前置：
  - 文件管理。
  - 初始化向导 / 工程主数据。
  - 文档交付。
  - 图纸交付。
  - 整改 / 预检查。
- 低频入口进入“更多工具”。

要求：

- 项目资产、工程主数据、交付工作中心仍保持清晰三段链路。
- 不要一屏平铺所有入口。
- 每个关键页面保留返回项目工作台能力。

### UX2.3：关键页面减负

目标：

- 默认视图面向员工，不直接堆技术字段。
- 文件管理默认字段：
  - 文件名。
  - 类型。
  - 版本。
  - 大小。
  - 专业。
  - 状态。
  - 操作。
- 技术字段进入“技术信息 / 诊断信息”：
  - 平台内部 ID。
  - checksum 细节。
  - 扫描任务编号。
  - trace / 诊断编号。
  - 处理状态码。

要求：

- 不删除数据能力。
- 不影响测试依赖的 DOM 标识、关键文案和功能入口。
- 不泄露真实 NAS 路径。

### UX2.4：视觉质感提升

目标：

- 在壳层、资产总览 Hero、项目状态卡、下一步行动卡加入轻量 glass-lite 和 Apple 式卡片层级。
- 表格、表单、权限、NAS 操作、交付审核保持清晰企业中后台风格。
- 使用柔和边框、浅阴影、现代留白、层叠卡片，提升“愿意用”的感觉。

禁止：

- 大面积玻璃化。
- 强模糊。
- 炫光、霓虹、重渐变。
- 营销风 landing page。
- 为好看牺牲可读性。

## 6. 必须保持的旧链接兼容

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

## 7. 自测要求

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
- `/data-steward/assets/503?tab=files`
- `/data-steward/assets/506?tab=files`
- 项目内工程主数据。
- 项目内文档交付。
- 项目内图纸交付。
- 项目内整改。
- 项目内交付包预检查。
- 旧全局链接兼容跳转。
- 1280 / 1440 / 1920 多分辨率。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 UX2 当前 active。
3. UX1 测试报告是否已通过；如未通过，修了哪些 UX1 阻塞项。
4. 是否确认未修改后端、数据库、docs。
5. UX2.0 - UX2.4 分批实施结果。
6. 入口与资产总览如何变得更容易理解。
7. 项目工作台如何展示“下一步动作”。
8. 哪些字段被默认隐藏 / 折叠 / 移入详情。
9. 哪些低频入口被移入更多工具。
10. 视觉质感提升范围。
11. 修改文件清单。
12. 自测命令结果。
13. 浏览器自测结果。
14. P0 / P1 / P2 列表。
15. 是否建议进入 UX2 测试验收。

## 9. 完成定义

只有同时满足以下条件，才能标记完成：

- 前端构建通过。
- 后端健康检查通过。
- `git diff --check` 通过。
- 未修改 `backend/**`、`docs/**`、数据库迁移。
- Fresh login 后用户能在 10 秒内理解下一步。
- 503 / 506 项目工作台能清楚看到“当前项目状态”和“下一步动作”。
- 文件管理默认不被技术字段、后台任务、扫描信息干扰。
- 文档 / 图纸交付能看懂应交、缺失、补交、审核、整改、预检查关系。
- 旧链接兼容跳转仍可用。
- 1280 / 1440 / 1920 多分辨率无横向溢出、按钮丢失、文字挤压。
- M2B / M2A / M1F / M1E / M1D / M1C 关键回归通过。

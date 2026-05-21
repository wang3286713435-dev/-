# Codex 开发 Agent 当前任务：UX2 使用逻辑与字段减负续接

你是数字化交付平台 UX2 批次的开发 agent。本轮从 Claude Code 已完成的“高级感视觉升级”之后继续，不重新开始，不回滚视觉升级。

工作目录：

`/Users/vc/Documents/数字化交付平台`

完成承诺固定为：

`<promise>MAINLINE_UX2_USER_EXPERIENCE_REFACTOR_COMPLETE</promise>`

## 0. 当前状态

UX2 已经进入开发中。

Claude Code 已完成一个 UX2 视觉子批次：

- 登录页加入官网风格 lightfield / spotlight / 粒子视觉。
- 资产总览 hero 加入高级视觉层。
- 项目工作台命令中心加入高级视觉层。
- 新增或修改：
  - `frontend/src/styles/effects.css`
  - `frontend/src/modules/core/components/ParticleField.vue`
  - `frontend/src/modules/core/composables/useSpotlight.ts`
  - `frontend/src/styles/tokens.css`
  - `frontend/src/styles/index.css`
  - `frontend/src/modules/auth/pages/LoginPage.vue`
  - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
  - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`

你必须把这些改动当成当前基线，不要回滚。除非发现明确 P0/P1，可做最小修复并在报告中说明。

本轮继续完成 UX2 的真正主目标：

`让普通员工进入平台后看得懂、知道下一步、少被技术字段干扰。`

## 1. 本轮定位

这不是后端功能开发，也不是继续单纯美化 UI。

本轮重点是：

1. 优化用户使用路径。
2. 强化下一步动作。
3. 降低技术字段干扰。
4. 把低频功能收进“更多工具”或折叠区。
5. 保留旧路由兼容。

主路径固定为：

`选择项目 -> 看资产 -> 确认主数据 -> 做交付 -> 查整改 / 预检查`

## 2. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/ux2-user-experience-refactor-plan.md`
4. `handoff/dev-agent/latest-report.md`
5. `handoff/test-agent/latest-report.md`

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
10. `frontend/src/modules/work-center/pages/RectificationsPage.vue`
11. `frontend/src/styles/index.css`
12. `frontend/src/styles/tokens.css`
13. `frontend/src/styles/effects.css`

## 3. 允许修改

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

允许：

- 调整前端路由入口展示，但不能删除旧路由。
- 调整菜单显隐和层级。
- 调整项目工作台默认信息结构。
- 调整页面默认字段、折叠区、高级信息区。
- 调整按钮文案、空状态、错误提示和下一步提示。
- 继续微调视觉样式，但不能把本轮变成纯视觉升级。

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

## 5. 本轮必须完成

### A. 资产总览使用逻辑收敛

目标：

- 资产总览是“项目入口台”，不是统计堆叠页。
- 用户进入后优先看到：
  - 真实项目。
  - 最近进入项目。
  - 待处理项目。
  - 下一步动作。

要求：

- 默认视图弱化测试 / 样例 / 历史项目。
- 不删除测试 / 样例项目筛选能力。
- 低频统计、扫描、导出、文件服务等入口不抢主视觉。
- 项目列表必须仍然可用。

### B. 项目工作台下一步引导

目标：

- 项目工作台默认展示当前项目状态和下一步动作。
- 员工能理解：
  - 先看资产。
  - 再确认工程主数据。
  - 再做文档 / 图纸交付。
  - 最后查整改和预检查。

要求：

- 保留三段链路：`项目资产 -> 工程主数据 -> 交付工作中心`。
- 不要一屏平铺所有入口。
- 常用入口前置：
  - 文件管理。
  - 初始化向导 / 工程主数据。
  - 文档交付。
  - 图纸交付。
  - 整改 / 预检查。
- 低频入口进入“更多工具”或折叠区域。

### C. 文件管理字段减负

目标：

- 文件管理默认聚焦目录树和文件表。
- 默认表格面向员工，不直接堆技术字段。

默认优先展示：

- 文件名。
- 类型。
- 版本。
- 大小。
- 专业。
- 状态。
- 操作。

技术字段进入“技术信息 / 诊断信息”：

- 平台内部 ID。
- checksum 细节。
- 扫描任务编号。
- trace / 诊断编号。
- 处理状态码。

注意：

- 不删除数据能力。
- 不影响现有操作：预览、详情、治理、补 checksum、更多操作。
- 不泄露真实 NAS 路径。

### D. 工程主数据和交付页业务解释

目标：

- 工程主数据页面要讲清楚“先定义规则”。
- 文档 / 图纸交付页面要讲清楚“按规则交付”。

要求：

- 在初始化向导、部位树、节点类型、交付物标准页面增加更清楚的业务说明和下一步动作。
- 在文档 / 图纸交付页面强化：
  - 应交。
  - 缺失。
  - 补交。
  - 审核。
  - 整改。
  - 预检查。
- 不改变交付业务逻辑。

### E. 旧链接兼容

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

## 6. 自测要求

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

如果后端未启动，按项目既有脚本启动后再检查健康。

## 7. 浏览器自测

至少手动检查：

1. Fresh login 后资产总览是否清楚。
2. 资产总览是否能在 10 秒内让用户知道下一步。
3. 503 / 506 项目工作台是否有清楚的项目状态和下一步动作。
4. 文件管理默认是否聚焦目录树和文件表。
5. 工程主数据是否说明“先定义规则”。
6. 文档 / 图纸交付是否说明“按规则交付”。
7. 旧链接是否不白屏。
8. 1280 / 1440 / 1920 宽度下是否无横向溢出、按钮丢失、文字挤压。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 是否保留 Claude 视觉升级。
2. 本轮完成了哪些使用逻辑优化。
3. 本轮完成了哪些字段减负。
4. 本轮完成了哪些下一步动作提示。
5. 旧链接兼容结果。
6. 自测命令结果。
7. 浏览器自测结果。
8. 是否修改后端 / docs / 数据库迁移。
9. P0 / P1 / P2。
10. 是否建议进入 UX2 整体验收。

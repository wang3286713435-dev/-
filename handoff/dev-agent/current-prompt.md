# 开发 Agent 当前任务：UX3 主视图聚焦与认知减负

你是数字化交付平台 UX3 批次的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

完成承诺固定为：

`<promise>MAINLINE_UX3_MAIN_VIEW_FOCUS_COMPLETE</promise>`

## 0. 当前状态

UX2 已验收通过并推送，当前从 UX2 基线切出 UX3 分支：

`codex/ux3-main-view-focus`

UX3 是 UX 分支内的小批次，不进入 M2C，不新增业务能力。

用户明确反馈：

- 当前主视图过于混乱。
- 用户进入后不知道应该干什么。
- 数据管家最重要的是文件管理、项目管理与可视化。
- 主视图不要留太多教程与文字。
- 应通过交互逻辑和 UI 引导用户使用平台。
- 当前平台内容并不复杂，最主要功能应集中在用户视野中。

## 1. 本轮目标

本轮只做主视图减法和入口聚焦：

1. 资产总览改成更直接的“项目启动台”。
2. 项目工作台首屏聚焦三个核心入口：
   - 文件管理。
   - 项目资产驾驶舱 / 项目可视化。
   - 交付状态。
3. 大段教程、长说明、偏技术状态从主视图移到折叠区、详情区或空状态中。
4. 保留旧路由兼容，不删除功能。
5. 保留 UX2 视觉升级，不回滚 lightfield / spotlight / glass-lite。

## 2. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/ux3-main-view-focus-plan.md`
4. `handoff/dev-agent/latest-report.md`
5. `handoff/test-agent/latest-report.md`

重点检查：

1. `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
2. `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
3. `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
4. `frontend/src/modules/core/components/SidebarMenu.vue`
5. `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
6. `frontend/src/styles/index.css`
7. `frontend/src/styles/effects.css`
8. `frontend/src/styles/tokens.css`

## 3. 允许修改

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

允许做：

- 调整资产总览首屏结构。
- 调整项目工作台首屏结构。
- 调整项目内导航层级。
- 折叠或降级低频入口。
- 压缩说明文字。
- 把偏技术字段移到详情 / 技术信息区。
- 微调样式以强化核心入口。

## 4. 严禁越界

严禁：

1. 修改 `backend/**`。
2. 修改数据库迁移。
3. 修改 `docs/**`。
4. 新增或修改后端接口。
5. 改变权限规则。
6. 新增 Hermes 能力。
7. 新增 BIM 轻量化能力。
8. 新增真实 NAS 写能力。
9. 读取文件正文。
10. 删除旧路由导致书签或测试脚本失效。
11. 物理删除功能入口或破坏既有业务页面。
12. 为 105 / 503 / 93 / 506 写死逻辑。
13. 把 `.claude/**`、`CLAUDE.md`、`tmp/**` 纳入提交。

如你认为必须改后端才能解决问题，停止并写入报告，不得自行修改后端。

## 5. 具体开发要求

### A. 资产总览：项目启动台

目标：

- 用户进入后第一眼知道：先选项目。
- 主视图核心是项目管理，不是教程页。

要求：

- 删除或折叠 `FLOW / STATE / ALERT` 等大块教程式首屏内容。
- 首屏保留并强化：
  - 项目搜索。
  - 真实项目列表。
  - 推荐进入项目。
  - 最近项目。
  - 核心动作：进入项目、查看文件管理、查看项目可视化 / 资产驾驶舱。
- 统计和风险提醒可以保留，但必须降级为紧凑摘要或折叠区，不占据主视觉。
- 测试 / 样例 / 历史项目筛选能力保留，但默认不抢主视图。

### B. 项目工作台：核心入口优先

目标：

- 用户进入项目后第一眼看到核心工作入口。

要求：

- 顶部去掉或弱化偏技术标签的强展示：
  - `平台内部ID`
  - `NAS_REAL_PILOT`
  - `ACTIVE`
- 这些字段如仍需保留，移入“项目详情 / 技术信息”折叠区。
- 首屏核心入口固定为：
  - 文件管理。
  - 项目资产驾驶舱 / 可视化。
  - 交付状态。
- 工程主数据入口保留，但不要比上述核心入口更抢眼。
- Hermes、模型集成、管理对象、事项、任务、导出、文件服务等低频入口放入更多工具。

### C. 项目内导航：主入口 + 更多

目标：

- 不再一眼看到大量同级按钮。

要求：

- `ProjectWorkspaceNav` 继续保留三段语义，但视觉上压缩为主入口 + 更多入口。
- 保留旧页面入口和跳转能力。
- 当前项目上下文必须持续可见。
- 不要让主导航像“功能清单墙”。

### D. 文件管理入口

目标：

- 文件管理作为数据管家最重要功能之一，必须更容易进入。

要求：

- 资产总览的项目行或推荐卡应能快速进入文件管理。
- 项目工作台首屏必须有明显文件管理入口。
- 不改变文件管理业务逻辑。

### E. 项目可视化入口

目标：

- 当前未接真实 BIM 引擎前，项目可视化可以对应资产驾驶舱 / 项目数据可视化，不伪装成真实 3D。

要求：

- 文案使用“项目可视化 / 资产驾驶舱”，不要承诺真实 BIM 轻量化。
- 不进入 8B / BIM 引擎能力。

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

浏览器至少检查：

1. Fresh login 后进入资产总览，5 秒内能看懂先选项目。
2. 资产总览首屏核心是项目搜索、项目列表、推荐进入、文件管理、项目可视化。
3. 503 / 506 项目工作台首屏最显眼的是文件管理、资产驾驶舱 / 可视化、交付状态。
4. 大段教程文字不再占据主视图。
5. 技术标签不再抢主视觉。
6. 旧链接不白屏。
7. 1280 / 1440 / 1920 宽度下无横向溢出、按钮丢失、文字挤压。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 主视图减法做了什么。
2. 资产总览如何变成项目启动台。
3. 项目工作台核心入口如何调整。
4. 哪些教程 / 技术信息被折叠或降级。
5. 文件管理和项目可视化入口是否更明显。
6. 旧链接兼容结果。
7. 自测命令结果。
8. 是否修改后端 / docs / 数据库迁移。
9. P0 / P1 / P2。
10. 是否建议进入 UX3 测试验收。

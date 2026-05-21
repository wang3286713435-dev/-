# UX1 紧急 UI / UX 修复首轮报告

更新时间：2026-05-21

## 1. 当前分支与基线

- 当前分支：`codex/ux1-frontend-routing-visual`
- UX1 基线 commit：`5cdc4d2`
- M2B 已先于 UX1 完成 Git checkpoint 并推送。

## 2. 本轮修复定位

本轮是 UX1 的紧急急救型修复，不是完整视觉重做。

目标：

- 降低项目工作台的理解成本。
- 让员工知道当前页面处于哪个项目和哪条工作路径。
- 降低 checksum 后台任务区对文件目录的干扰。
- 不修改后端、数据库、接口语义或权限规则。

## 3. 修改范围

仅修改前端与交接报告。

已修改：

- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/styles/index.css`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`

未修改：

- `backend/**`
- `docs/**`
- 数据库迁移
- 权限逻辑
- Hermes / BIM / NAS 写能力边界

## 4. 具体修复

### 4.1 顶部工作上下文

全局顶部不再是空白区域，改为显示：

- 当前项目工作台 / 平台主入口 / 管理中心。
- 当前项目名或当前页面名。
- 页面下一步说明。

这样员工进入项目内页面时，可以看到自己正在具体项目中工作，而不是在一组割裂的全局功能里跳转。

### 4.2 项目工作台导航

项目工作台导航继续保持：

`项目资产 -> 工程主数据 -> 交付工作中心`

但展示方式从按钮堆叠改为更清晰的三段分组：

- 每组有编号、标题和说明。
- 子入口用更稳定的紧凑按钮展示。
- 工作中心继续显示“主数据已就绪 / 先确认主数据”的状态。

### 4.3 文件管理中的 checksum 后台任务

文件管理页中，checksum 后台任务区默认收起。

原因：

- 文件目录和文件列表是文件管理主任务。
- checksum 任务是排查和追踪能力，不应在默认视图中压过目录结构。

现在页面只展示任务摘要和“查看任务”按钮，用户需要排查时再展开任务表。

## 5. 验证结果

已执行：

- `corepack pnpm --dir frontend build`
- `curl -fsS http://127.0.0.1:8080/actuator/health`
- `git diff --check`

结果：

- 前端构建通过。
- 仅保留既有 Vite chunk size warning。
- 后端健康检查返回 `UP`。
- `git diff --check` 通过。

## 6. 风险与未做项

本轮未做完整 UX1。

仍需后续 UX1 覆盖：

- 旧全局路由兼容跳转的浏览器验证。
- 资产总览、项目详情、文件管理、工程主数据、文档 / 图纸交付、整改、预检查的完整视觉巡检。
- 表格、抽屉、弹窗、空状态和错误提示的系统性统一。
- Gemini 主导的完整 UX1 批次开发与测试 agent 复验。

## 7. 主 Agent 判断

本轮急救修复可以作为 UX1 的第一步。

不建议把它视为 UX1 完整收口。下一步应让测试 agent 做一次极短回归，重点看：

- 项目工作台顶部上下文是否清晰。
- 项目工作台三段导航是否可读。
- 文件管理默认是否优先展示目录与文件。
- checksum 后台任务是否默认收起且可展开。
- 是否出现横向溢出。

# 测试 Agent 当前任务：UX1 前端路由逻辑与视觉体验专项验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`UX1：前端路由逻辑与视觉体验专项优化`

注意：

- UX1 应在 M2B 收口后启动。
- UX1 由 Gemini 主导开发，主 agent 负责监控。
- UX1 只能修改前端路由、菜单、布局、样式和文案。
- UX1 不允许修改后端业务逻辑、数据库迁移、权限规则或接口语义。

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/ux1-gemini-frontend-routing-visual-prompt.md`
3. `handoff/main-agent/ux1-frontend-routing-visual-plan.md`
4. `handoff/main-agent/status.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/test-agent/latest-report.md`

## 1. 验收目标

确认 UX1 做到了：

1. 路由逻辑更清晰。
2. 旧全局链接兼容跳转。
3. 项目工作台围绕路由项目展示。
4. 项目资产、工程主数据、交付工作中心层级清楚。
5. 页面视觉更精致、更像企业中后台。
6. 页面文案能帮助员工理解功能用途和下一步。
7. 没有修改后端能力边界。

## 2. 必跑命令

执行：

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

同时检查：

- `git diff --name-only` 中不得出现 `backend/**`。
- 不得出现新的 Flyway migration。

## 3. 路由验收

必须验证：

- Fresh login 后进入 `/data-steward/assets`。
- 从资产总览进入 `/data-steward/assets/503`。
- 从资产总览进入 `/data-steward/assets/506`。
- 刷新项目工作台后项目上下文不丢失。
- 刷新项目内工程主数据页面后项目上下文不丢失。
- 刷新项目内文档交付页面后项目上下文不丢失。
- 刷新项目内图纸交付页面后项目上下文不丢失。
- 旧 `/master-data/sections` 兼容跳转。
- 旧 `/master-data/node-types` 兼容跳转。
- 旧 `/master-data/deliverable-standard` 兼容跳转。
- 旧 `/work/document-delivery` 兼容跳转。
- 旧 `/work/drawing-delivery` 兼容跳转。
- 旧 `/work/rectifications` 兼容跳转。
- 旧 `/data-steward/models` 兼容跳转。

## 4. 页面体验验收

至少检查：

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

要求：

- 页面无白屏。
- 页面无横向撑爆。
- 页面无主要按钮不可见。
- 页面无文字严重挤压。
- 页面无大项目卡死。
- 页面结构能看出 `项目资产 -> 工程主数据 -> 交付工作中心`。
- 员工能从标题、说明、空状态、提示语理解当前页面用途。

## 5. 安全与边界验收

不得出现：

- 后端文件改动。
- 数据库迁移改动。
- 新后端接口。
- 权限逻辑改动。
- Hermes 新能力。
- BIM 轻量化能力。
- 新真实 NAS 写能力。
- 真实 NAS 路径泄露。
- `storage_uri`、raw row、SQL、token、secret、password 泄露。

## 6. P0 / P1 / P2 判定

P0：

- Fresh login 失败。
- 项目工作台打不开。
- 旧链接无法兼容跳转。
- 项目内页面刷新后丢项目上下文。
- 修改了后端业务逻辑。
- 泄露真实 NAS 路径。

P1：

- 项目资产、工程主数据、交付工作中心层级仍然混乱。
- 主要页面视觉没有明显改善。
- 页面出现横向溢出、按钮遮挡或文字严重挤压。
- 105/503 可用但 93/506 不可用。

P2：

- 局部文案、间距、图标、颜色仍可继续优化，但不影响主链路。

## 7. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 UX1 当前 active。
4. 是否确认 M2B 已先于 UX1 收口。
5. P0 / P1 / P2 列表。
6. 必跑命令结果。
7. 路由兼容验收结果。
8. 503 / 506 项目工作台验收结果。
9. 页面体验验收结果。
10. 后端未改动检查结果。
11. 安全边界检查结果。
12. 是否建议收口 UX1。

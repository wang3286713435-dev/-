# 测试 Agent 待启动任务：UX2 前端使用逻辑与体验重构验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`UX2：前端使用逻辑与体验重构专项`

## 0. 启动条件

只有主 agent 明确 UX2 进入测试验收后，才使用本 prompt。

## 1. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/main-agent/status.md`
4. `handoff/main-agent/development-log.md`
5. `handoff/main-agent/ux2-user-experience-refactor-plan.md`
6. `handoff/test-agent/latest-report.md`

## 2. 验收目标

确认 UX2 做到：

1. Fresh login 进入资产总览后，用户能在 10 秒内理解下一步。
2. 资产总览突出真实项目、待处理项目、最近项目和下一步动作。
3. 项目工作台默认展示当前项目状态和下一步动作。
4. 项目资产、工程主数据、交付工作中心保持清晰三段链路。
5. 文件管理默认不被技术字段、后台任务、扫描信息干扰。
6. 文档 / 图纸交付能看懂应交、缺失、补交、审核、整改、预检查关系。
7. 低频入口没有抢主视觉，但旧链接兼容跳转仍可用。
8. 视觉有现代卡片层级和轻量 glass-lite，但不影响可读性。
9. 后端能力、权限、NAS 写安全、路径脱敏不回归。

## 3. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

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

## 4. 越界检查

必须执行并记录：

```bash
git diff --name-only
git status --short
git status --short backend docs
git status --short backend/delivery-app/src/main/resources/db/migration
```

P0：

- 有 `backend/**`、`docs/**`、数据库迁移改动。
- 有接口语义、权限、Hermes、BIM、NAS 能力扩展。
- 页面泄露真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret。

## 5. 浏览器验收

分辨率至少覆盖：

- `1280 x 800`
- `1440 x 900`
- `1920 x 1080`

重点检查：

- Fresh login 后资产总览是否清楚。
- 是否能在 10 秒内理解下一步。
- 503 / 506 项目工作台是否有清楚的项目状态和下一步动作。
- 文件管理默认是否聚焦目录树和文件表。
- 工程主数据是否说明“先定义规则”。
- 文档 / 图纸交付是否说明“按规则交付”。
- 低频入口是否被降级但仍可找到。
- 页面是否有横向溢出、文字挤压、按钮丢失、表格撑爆。
- 轻量 glass-lite 是否克制，不影响可读性。

## 6. 旧链接兼容验收

打开：

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

要求：

- 不白屏。
- 不 404。
- 不丢项目上下文。
- 如果无法确定当前项目，应回到 `/data-steward/assets`。

## 7. P0 / P1 / P2 判定

P0：

- 修改后端、docs、数据库迁移。
- 旧路由白屏。
- 项目上下文串项目。
- 真实 NAS 路径泄露。
- M2B / M2A / M1F / M1E / M1D / M1C 回归失败。

P1：

- 用户进入资产总览仍不知道下一步。
- 项目工作台仍然平铺所有入口，没有主次。
- 文件管理仍被技术字段或后台任务压过主路径。
- 关键页面在 1280 宽度下横向溢出。
- 低频入口被隐藏到找不到，影响主链路。

P2：

- 个别视觉细节、文案、间距可继续打磨。
- Vite chunk size warning。
- 轻量 glass-lite 局部不够惊艳但不影响使用。

## 8. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 UX2 当前 active。
4. 必跑命令结果。
5. 越界检查结果。
6. 多分辨率视觉巡检结果。
7. 资产总览验收结果。
8. 项目工作台验收结果。
9. 文件管理验收结果。
10. 工程主数据验收结果。
11. 交付工作中心验收结果。
12. 旧链接兼容结果。
13. P0 / P1 / P2 列表。
14. 是否建议主 agent 收口 UX2。

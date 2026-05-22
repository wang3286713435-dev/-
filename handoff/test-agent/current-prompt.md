# 测试 Agent 当前任务：UX3 主视图聚焦与认知减负验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`UX3：主视图聚焦与认知减负`

## 1. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/main-agent/status.md`
4. `handoff/main-agent/development-log.md`
5. `handoff/main-agent/ux3-main-view-focus-plan.md`

## 2. 验收目标

确认 UX3 做到：

1. Fresh login 后进入资产总览，5 秒内能看懂先选项目。
2. 数据管家核心功能聚焦在项目管理、文件管理、项目可视化。
3. 资产总览不再被大段教程、`FLOW / STATE / ALERT` 等内容占满首屏。
4. 项目工作台首屏最显眼的是文件管理、资产驾驶舱 / 可视化、交付状态。
5. 技术标签不再抢主视觉，例如 `平台内部ID`、`NAS_REAL_PILOT`、`ACTIVE`。
6. 低频入口仍可找到，但不抢主视觉。
7. 旧链接继续兼容，不白屏、不 404、不丢项目上下文。
8. 后端能力、权限、NAS 写安全、路径脱敏不回归。

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

P1：

- 运行期前端文件仍处于未跟踪状态，例如被页面或样式引用的 `.vue`、`.ts`、`.css` 文件显示为 `??`。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 属于本地非交付文件，不因未跟踪直接判失败，但必须记录。

## 5. 浏览器验收

分辨率至少覆盖：

- `1280 x 800`
- `1440 x 900`
- `1920 x 1080`

重点检查：

- Fresh login 后资产总览是否清楚。
- 资产总览首屏是否聚焦项目搜索、真实项目列表、推荐进入、最近项目。
- 是否能快速进入文件管理。
- 是否能快速进入项目可视化 / 资产驾驶舱。
- 项目工作台首屏是否聚焦文件管理、资产驾驶舱 / 可视化、交付状态。
- 工程主数据、Hermes、模型集成、事项、任务、导出、文件服务等低频入口是否降级但仍可找到。
- 页面是否有横向溢出、文字挤压、按钮丢失、表格撑爆。
- 轻量视觉效果是否仍可读，不遮挡主要内容。

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

- 资产总览首屏仍然像教程页，用户看不到核心项目入口。
- 项目工作台首屏仍然被技术标签或说明文字占据。
- 文件管理和项目可视化入口不明显。
- 低频入口被隐藏到找不到，影响主链路。
- 关键页面在 1280 宽度下横向溢出。

P2：

- 个别视觉细节、文案、间距可继续打磨。
- Vite chunk size warning。
- 仍有少量技术字段存在但不抢主视觉。

## 8. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 UX3 当前 active。
4. 必跑命令结果。
5. 越界检查结果。
6. 多分辨率视觉巡检结果。
7. 资产总览验收结果。
8. 项目工作台验收结果。
9. 文件管理 / 项目可视化入口验收结果。
10. 旧链接兼容结果。
11. P0 / P1 / P2 列表。
12. 是否建议主 agent 收口 UX3。

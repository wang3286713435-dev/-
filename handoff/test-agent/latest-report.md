# UX3 主视图聚焦与认知减负验收报告

生成时间：2026-05-22 11:18 CST

## 1. 测试结论

结论：通过。

UX3 已达到本轮验收目标：Fresh login 后进入 `/data-steward/assets`，约 3.1 秒内展示项目启动台、推荐项目、文件管理、项目可视化、真实项目列表和下一步动作；资产总览不再被 `FLOW / STATE / ALERT` 类教程块占据；503 / 506 项目工作台首屏聚焦文件管理、项目可视化 / 资产驾驶舱、交付状态；低频入口仍可通过更多工具或旧链接访问；旧链接兼容稳定。

当前未发现 P0 / P1。发现 3 个 P2，可后续继续打磨，不阻塞 UX3 收口。

建议主 agent 收口 UX3。

## 2. 当前分支和 commit

- 工作目录：`/Users/vc/Documents/数字化交付平台`
- 当前分支：`codex/ux3-main-view-focus`
- 当前 commit：`b5cfb01`
- UX3 当前 active：已确认。`handoff/main-agent/status.md`、`handoff/dev-agent/current-prompt.md`、`handoff/main-agent/ux3-main-view-focus-plan.md` 均指向 UX3。

## 3. 必读文档

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/ux3-main-view-focus-plan.md`

## 4. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2B：`bash scripts/dev/check-m2b-nas-write-trial.sh`，通过，`PASS=18 FAIL=0`。
- M2A：`bash scripts/dev/check-m2a-controlled-nas-write.sh`，通过，`PASS=21 FAIL=0`。
- M1F：`bash scripts/dev/check-m1f-employee-access-control.sh`，通过，`PASS=20 FAIL=0`。
- M1E：`bash scripts/dev/check-m1e-file-task-continuity.sh`，通过，`PASS=10 FAIL=0`。
- M1D：`bash scripts/dev/check-m1d-standard-delivery-loop.sh`，通过，`PASS=29 FAIL=0`。
- M1C：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 5. 越界检查结果

已执行：

- `git diff --name-only`
- `git status --short`
- `git status --short backend docs`
- `git status --short backend/delivery-app/src/main/resources/db/migration`

结果：

- `backend/**`：无改动。
- `docs/**`：无改动。
- 数据库迁移：无改动。
- 本轮变更集中在 `frontend/**` 和 handoff 文件。
- 暂存/修改文件包括：`AssetOverviewPage.vue`、`AssetProjectDetailPage.vue`、`ProjectWorkspaceNav.vue`、`AppLayout.vue` 及 UX3 handoff 文件。
- 未跟踪非交付文件仍存在：`.claude/skills/frontend-design/`、`CLAUDE.md`、`tmp/**`。按本轮测试 prompt 不判失败，但建议主 agent 收口提交时继续排除。
- 未发现接口语义、权限、Hermes、BIM、NAS 能力扩展。

## 6. 多分辨率视觉巡检结果

覆盖分辨率：

- `1280 x 800`
- `1440 x 900`
- `1920 x 1080`

覆盖页面：

- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/506`
- `/data-steward/assets/503?tab=files`
- `/data-steward/assets/503/work/dashboard`

结果：

- 页面均非白屏。
- 页面级横向溢出为 `0`。
- 顶部当前项目上下文可读。
- 左侧菜单文字可读。
- 文件管理、项目可视化、交付状态入口可见。
- 未发现按钮丢失、文字明显挤压、表格撑爆。
- lightfield / spotlight / glass-lite 视觉没有遮挡主要内容。
- 未发现 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、`storageUri`、raw row、SQL、secret、token、password 泄露。

## 7. 资产总览验收结果

Fresh login 使用 `platform.admin / Admin@123` 后自动进入 `/data-steward/assets`。

结果：

- 登录后约 3.1 秒内出现项目启动台和真实项目列表，满足 5 秒内理解“先选项目”的目标。
- 首屏核心信息为：`项目启动台`、`选择项目，直接开始工作`、推荐项目、文件管理、项目可视化、待处理项目、最近项目、项目列表。
- 默认聚焦真实 NAS 项目，显示 16 个可见项目、40,936 份已登记文件。
- 推荐项目卡片提供 `进入项目`、`文件管理`、`项目可视化` 三个直接动作。
- 项目列表每行保留 `工作台`、`文件`、`可视化` 快捷操作。
- `FLOW / STATE / ALERT` 类大块教程未再占据首屏。
- 统计与风险摘要降级到折叠区，不影响先选项目。

## 8. 项目工作台验收结果

已打开：

- `/data-steward/assets/503`
- `/data-steward/assets/506`

结果：

- 503 显示启航华居项目 / 编码 105 上下文。
- 506 显示中建八局国交酒店项目 / 编码 93 上下文。
- 页面保留 `项目资产 -> 工程主数据 -> 交付工作中心` 三段链路。
- 首屏核心入口聚焦：项目可视化、文件管理、交付状态。
- 顶部主操作提供：文件管理、项目可视化、交付状态、项目详情、刷新。
- 工程主数据仍可进入，但没有比三项核心入口更抢眼。
- Hermes、模型集成、管理对象、事项、任务、导出、文件服务等低频能力没有从系统消失，可通过更多工具或旧链接访问。

## 9. 文件管理 / 项目可视化入口验收结果

文件管理入口：

- 资产总览推荐项目卡片可直接进入文件管理。
- 资产总览项目列表每行有 `文件` 快捷操作。
- 项目工作台顶部有 `文件管理` 主按钮。
- 项目工作台核心入口卡片包含 `文件管理`。
- 项目内导航项目资产段保留 `文件管理`。

项目可视化入口：

- 资产总览推荐项目卡片可直接进入项目可视化。
- 资产总览项目列表每行有 `可视化` 快捷操作。
- 项目工作台顶部有 `项目可视化` 主按钮。
- 项目工作台核心入口卡片包含 `项目可视化 / 资产驾驶舱`。
- 文案没有伪装成真实 BIM 轻量化或 3D 引擎能力。

文件管理页面：

- `/data-steward/assets/503?tab=files` 正常打开。
- 未出现全局横向溢出。
- 未触发真实 NAS 写操作。
- 未发现真实 NAS 路径或敏感字段泄露。

## 10. 旧链接兼容结果

已打开：

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

结果：

- 全部未白屏。
- 全部未 404。
- 在当前有项目上下文时，均跳转到 `/data-steward/assets/503/...` 项目工作台内对应页面。
- 跳转后仍保留当前项目上下文和三段导航。
- 未发现旧链接导致项目上下文丢失或串项目。

## 11. P0 / P1 / P2 列表

P0：无。

P1：无。

P2：

- 既有 Vite chunk size warning，未阻塞构建。
- 项目工作台仍可见少量偏技术文本，例如 `INTERNAL_PILOT`、`项目 503`、以及 `项目详情 / 技术信息平台内部 ID、资产来源、工作链路说明已收起`。这些内容没有抢过文件管理、项目可视化、交付状态主路径，也没有泄露真实 NAS 路径，但仍建议后续继续弱化或移入真正的详情抽屉 / 折叠内容。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 未跟踪非交付文件仍存在，需主 agent 收口提交时继续排除。

## 12. 是否建议主 agent 收口 UX3

建议收口 UX3。

理由：本轮必跑命令和主线回归全部通过；浏览器验收覆盖 fresh login、资产总览、503 / 506 项目工作台、文件管理、项目可视化入口、旧链接兼容和多分辨率布局，未发现 P0 / P1。剩余问题均为 P2，不影响 UX3 整体收口。

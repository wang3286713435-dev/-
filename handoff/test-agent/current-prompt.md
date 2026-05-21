# 测试 Agent 当前任务：UX1 前端壳层重构多分辨率视觉与路由连续性验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`UX1：前端路由逻辑与视觉体验专项优化`

本轮重点不是后端功能验收，而是确认 Claude Code 完成的前端壳层重构没有破坏主线，并且在真实项目工作流里真正改善了可用性。

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/main-agent/status.md`
4. `handoff/main-agent/development-log.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/main-agent/ux1-frontend-routing-visual-plan.md`
7. `handoff/main-agent/ux1-emergency-ui-fix-report.md`

重点确认：

- 当前分支是否为 `codex/ux1-frontend-routing-visual`。
- UX1 是否只修改前端和 handoff 报告。
- 是否有 `backend/**`、`docs/**`、数据库迁移改动。
- 是否存在未跟踪的 `.claude/**`、`CLAUDE.md` 等非运行文件，报告中要说明它们是否会被误提交或影响验收。

## 1. 验收目标

确认 UX1 做到：

1. Fresh login 后仍进入 `/data-steward/assets`。
2. 前端主路径清晰：`资产总览 -> 项目工作台 -> 项目资产 / 工程主数据 / 交付工作中心`。
3. 项目内页面以路由 `projectId` 为准，不再让用户困惑于全局当前项目。
4. 项目工作台三段导航可读：`项目资产 -> 工程主数据 -> 交付工作中心`。
5. 旧全局链接兼容跳转，不白屏、不丢项目上下文。
6. 503/105 与 506/93 两个真实项目之间连续切换、刷新、直达链接都稳定。
7. 文件管理、工程主数据、文档交付、图纸交付、整改、交付包预检查入口清晰。
8. 页面在多分辨率下无横向溢出、文字严重挤压、按钮不可见、表格撑爆、卡死。
9. M2B 灰度状态条、M2A NAS 写操作安全边界、M1F 员工权限、M1E 文件任务、M1D 交付闭环、M1C 主数据链路不回归。

## 2. 必跑命令

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

如果 `check-m1e-file-task-continuity.sh` 因本机缺少 `rg` 报静态检查失败：

- 不要直接判 UX1 P1。
- 先记录原始失败。
- 再用等价 `grep` 检查报告中列出的 M1E 关键字：
  - `delivery.dataSteward.fileBrowser.state`
  - `data-m1e-continuity-bar`
  - `重置视图`
  - `data-m1e-checksum-jobs`
  - `后台任务编号`
  - `平台文件ID`
  - `path_not_exposable`
- 若等价检查通过，可判为环境 P2，不阻塞 UX1。

## 3. 越界检查

必须执行并记录：

```bash
git diff --name-only
git status --short
git status --short backend docs
git status --short backend/delivery-app/src/main/resources/db/migration
```

判定：

- 若有 `backend/**`、`docs/**`、数据库迁移改动，直接 P0。
- 若 `.claude/**`、`CLAUDE.md` 等非运行文件为未跟踪状态，记录为“需主 agent 决定是否清理 / 忽略”，不直接判 P0，但不得建议纳入 UX1 提交。
- 若 UX1 页面泄露真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret，直接 P0。

## 4. 浏览器多分辨率视觉巡检

使用浏览器打开本地前端：

`http://127.0.0.1:5173`

至少覆盖这些分辨率：

- `1280 x 800`
- `1440 x 900`
- `1920 x 1080`

如果时间允许，补一个窄屏：

- `390 x 844`

每个分辨率至少检查：

1. 页面是否白屏。
2. `document.documentElement.scrollWidth - window.innerWidth` 是否为 0 或接近 0。
3. 顶部当前工作上下文是否可读。
4. 左侧菜单是否可读，字体和背景不冲突。
5. 项目工作台三段导航是否不挤压、不重叠。
6. 主要按钮是否可见。
7. 表格是否只在局部容器滚动，不撑爆整页。
8. 抽屉、弹窗、空状态、错误提示是否不遮挡主内容。

## 5. 页面路径验收

### 5.1 Fresh login

- 退出或使用 fresh session。
- 登录 `platform.admin`。
- 确认进入 `/data-steward/assets`。
- 确认资产总览能看到真实项目，并能进入项目工作台。

### 5.2 真实项目工作台

分别打开：

- `/data-steward/assets/503`
- `/data-steward/assets/506`

检查：

- 顶部项目名、项目上下文、下一步说明是否正确。
- 项目工作台三段链路是否清楚。
- `项目资产`、`工程主数据`、`交付工作中心`入口是否都可见。
- 页面无横向溢出。

### 5.3 文件管理

分别打开：

- `/data-steward/assets/503?tab=files`
- `/data-steward/assets/506?tab=files`

检查：

- 左侧目录树和右侧文件表优先可见。
- 目录树不被 checksum 后台任务压住。
- checksum 后台任务默认收起，可以展开。
- M2B 灰度状态条可见，灰度关闭时写按钮禁用且说明原因。
- 不泄露真实 NAS 路径。

### 5.4 工程主数据

检查：

- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/master-data/node-types`
- `/data-steward/assets/503/master-data/deliverable-standard`

要求：

- 页面顶部仍有项目工作台上下文。
- 页面能看懂“先定义规则”的用途。
- 初始化向导不表现为真实自动结论，仍保留 catalog-only / 人工确认边界。

### 5.5 交付工作中心

检查：

- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/503/work/dashboard`

要求：

- 页面顶部仍有项目工作台上下文。
- 文档 / 图纸交付能看懂应交、缺失、补交、审核、整改、预检查关系。
- 文件选择器仍是分页远程查询。
- 交付包预检查仍是 dry-run，不生成真实包、不访问 NAS 文件。

## 6. 项目间路由切换连续性测试

必须覆盖：

1. 从 `/data-steward/assets/503?tab=files` 切到 `/data-steward/assets/506?tab=files`。
2. 从 506 的工程主数据页面切回 503 的文档交付页面。
3. 直接刷新 `/data-steward/assets/503/work/document-delivery`。
4. 直接刷新 `/data-steward/assets/506/master-data/initialization`。
5. 浏览器后退 / 前进后项目标题、项目导航、页面内容是否匹配。

P0：

- 页面显示 503 项目标题，但接口或内容加载 506。
- 路由 projectId 和页面 projectId 不一致。
- 刷新后项目导航消失。
- 旧请求回写导致页面闪回另一个项目。

## 7. 旧全局链接兼容验收

打开以下旧链接，确认不白屏、不 404、不丢项目上下文：

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

如果当前用户已有当前项目，应跳到该项目工作台内对应页面。

如果无法确定当前项目，应回到 `/data-steward/assets`，不能白屏。

## 8. P0 / P1 / P2 判定

P0：

- 修改 `backend/**`、`docs/**`、数据库迁移。
- 旧路由删除或白屏。
- 项目内页面丢失项目上下文。
- 503/506 项目切换后内容串项目。
- 泄露真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret。
- 破坏 M2B/M2A/M1F/M1E/M1D/M1C 主链路。

P1：

- 多分辨率下主要页面横向溢出。
- 项目工作台三段链路仍然不可读。
- 文件管理默认被后台任务、工具条或装饰视觉压住。
- 工作中心入口与工程主数据前置关系不清。
- 旧链接虽然不白屏，但跳错项目或缺项目导航。

P2：

- 个别文案、间距、颜色、hover 状态仍可优化。
- Vite chunk size warning。
- 本机 `rg` 环境缺失导致脚本静态检查误报，但等价检查通过。
- 未跟踪的 `.claude/**`、`CLAUDE.md` 等非运行文件存在，且未纳入提交。

## 9. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 测试结论：通过 / 不通过。
2. 当前分支和 commit。
3. 是否确认 UX1 当前 active。
4. 是否确认无后端、docs、数据库迁移改动。
5. 必跑命令结果。
6. 越界检查结果。
7. 多分辨率视觉巡检结果。
8. 503/506 项目工作台验收结果。
9. 文件管理验收结果。
10. 工程主数据验收结果。
11. 交付工作中心验收结果。
12. 项目间路由切换连续性结果。
13. 旧全局链接兼容结果。
14. P0 / P1 / P2 列表。
15. 是否建议主 agent 收口 UX1。

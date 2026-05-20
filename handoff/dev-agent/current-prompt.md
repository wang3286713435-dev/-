# 开发 Agent 当前任务：M1B 项目工作台与数据管家可用性收口

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 当前路线

M1A 已通过测试 agent 复验并收口，主线健康度已从 `黄灯可控` 调整为 `绿灯可继续主线开发`。

当前 active 批次：

`M1B：项目工作台与数据管家可用性收口`

完成承诺固定为：

`<promise>MAINLINE_M1B_WORKBENCH_USABILITY_COMPLETE</promise>`

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
5. `handoff/main-agent/m1a-platform-baseline-closure.md`
6. `handoff/main-agent/m1b-project-workbench-usability-plan.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

重点检查：

1. 资产总览：
   - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
2. 项目工作台：
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
3. 文件管理：
   - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
   - `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
4. 工程主数据入口：
   - `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
   - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
   - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
   - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
5. 工作中心入口：
   - `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
   - `frontend/src/modules/work-center/pages/RectificationsPage.vue`
6. 数据管家后端聚合或统计口径：
   - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/**`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮目标

本轮不是继续开发 Hermes，也不是继续 G4，不进入 8B / 8C / 9A。

目标是解决一个产品可用性问题：

平台能力已经很多，但普通员工进入页面后仍不容易理解“当前项目在哪里、每个入口有什么用、下一步应该做什么”。

重点完成：

1. 资产总览 Hero 区更清晰。
2. 项目卡片 / 列表能解释项目来源、接入状态、主数据状态、交付状态和下一步动作。
3. 修复 `/data-steward/assets` 默认真实项目统计显示明显不准的问题。
4. 项目工作台顶部能解释三类能力：
   - 数据管家：看项目文件资产。
   - 工程主数据：维护部位、节点类型和交付标准。
   - 工作中心：做文档 / 图纸交付、审核、整改和预检查。
5. 空状态、提示文案和按钮文案更适合业务用户。
6. 不依赖 Hermes，用户也能知道基础操作路径。

## 3. 允许做什么

允许：

- 前端可用性和信息层级调整。
- 资产总览 / 项目工作台 / 文件管理 / 项目内导航文案优化。
- 修复真实项目统计或接入状态展示问题。
- 增加轻量状态标签、说明、下一步按钮。
- 小范围只读接口或统计口径修复。
- 补充最小 smoke 脚本，如果能稳定覆盖本轮页面。

## 4. 禁止做什么

禁止：

1. 新增 Hermes 能力。
2. 继续 G4。
3. 进入 8B / 8C / 9A。
4. 做真实 BIM 轻量化。
5. 做构件级解析。
6. 读取 PDF / Office / DWG / RVT / IFC 正文。
7. 做 selective indexing。
8. 写 Hermes memory。
9. 写 OpenSearch / Qdrant / MinIO documents/chunks。
10. 开放真实 NAS 增删改查。
11. Agent 自动审批、自动整改、自动挂接。
12. 把 catalog metadata 冒充正文 evidence。
13. 返回真实 NAS 路径、raw row、SQL、token、secret、password。
14. 为 105 / 503 写死特殊逻辑。
15. 修改 `docs/**`，除非主 agent 单独授权。

## 5. 建议执行方式

先审计现状，再做小范围收口：

1. 打开资产总览，确认真实项目统计、项目列表、筛选和项目卡片文案。
2. 抽查 503 / 105 和 506 / 93 两个真实 NAS 项目。
3. 记录用户看不懂的入口、空状态、状态词和下一步按钮。
4. 修复统计明显不准或状态误导。
5. 优化页面结构和文案，不大改路由，不新增大模块。
6. 确认文件管理、工程主数据、文档交付、图纸交付入口不回归。

## 6. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6a-project-initialization.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
git diff --check
```

浏览器自测至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/506`
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 M1A 已收口、M1B 当前 active。
3. 是否确认 G4 / Hermes / 8B / 9A 冻结。
4. 修复或优化了哪些可用性问题。
5. 是否修复真实项目统计显示问题。
6. 503 与 506 抽查结果。
7. 修改文件清单。
8. 是否触碰后端；如果触碰，说明是否仅为只读统计或展示口径。
9. 自测命令结果。
10. P0 / P1 / P2 列表。
11. 是否建议进入 M1B 测试验收。

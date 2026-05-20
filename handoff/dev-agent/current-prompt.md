# 开发 Agent 当前任务：M1A 平台主线功能基线审计与交付闭环缺口收束

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 路线冻结

当前用户裁决：

- G4 暂停开发。
- Hermes 定位冻结。
- 后续 Hermes 必须重新对齐后，通过独立分支继续完善。
- 主线不能继续卡死在 Hermes 功能中。
- 当前主线恢复到平台功能本身的完善。

当前 active 主线：

`M1A：平台主线功能基线审计与交付闭环缺口收束`

完成承诺固定为：

`<promise>MAINLINE_M1A_PLATFORM_BASELINE_AUDIT_COMPLETE</promise>`

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
5. `handoff/dev-agent/latest-report.md`
6. `handoff/test-agent/latest-report.md`
7. `docs/07-complete-delivery-prd.md`
8. `docs/08-acceptance-and-agent-integration.md`
9. `docs/10-phase2-development-roadmap.md`

重点检查：

1. 项目工作台：
   - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
2. 文件管理：
   - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
   - `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
3. 工程主数据：
   - `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
   - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
   - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
   - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
4. 工作中心：
   - `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
   - `frontend/src/modules/work-center/pages/RectificationsPage.vue`
5. 后端相关：
   - `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/**`
   - `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/**`
   - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/**`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮目标

本轮不是继续开发 Hermes，也不是继续 G4。

本轮目标是回到平台主线，做一次平台功能基线审计，找出阻塞真实项目交付闭环的缺口，并只做小范围修补。

重点关注：

1. 项目工作台是否清晰。
2. 资产总览和文件管理是否适合真实项目使用。
3. 工程主数据是否能被普通员工理解和维护。
4. 标准驱动交付链路是否能跑通。
5. 文档 / 图纸交付、审核、整改、导出预检查是否稳定。
6. BIM Mock 入口是否只是安全占位，未误导成真实轻量化。
7. 权限、审计、路径脱敏是否没有回归。

## 3. 本轮允许做什么

允许：

- 修复项目工作台、文件管理、工程主数据、交付页面中的 P0/P1/P2。
- 修复页面跳转、项目上下文、刷新、空状态、错误提示。
- 修复真实项目使用中明显不清楚的文案和入口。
- 修复主线平台功能脚本或补最小 smoke。
- 修复不影响底层模型的小型只读接口问题。
- 修复权限、审计、路径脱敏回归。

## 4. 本轮禁止做什么

禁止：

1. 继续 G4。
2. 新增 Hermes 能力。
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
14. 为 105 写死特殊逻辑。
15. 修改 `docs/**`，除非主 agent 单独授权。

如发现 Hermes 现有能力阻塞主线，只允许做 P0/P1 修复，不允许借修复继续扩 Hermes。

## 5. 建议执行方式

先做审计，再决定是否小修：

1. 跑一遍当前平台主线 smoke。
2. 用 105 和另一个真实 NAS 项目抽查：
   - 资产总览。
   - 项目工作台。
   - 文件管理。
   - 初始化向导。
   - 部位树。
   - 节点类型。
   - 交付物标准。
   - 文档交付。
   - 图纸交付。
   - 审核 / 整改。
   - 导出预检查。
3. 列出 P0 / P1 / P2。
4. 只修 P0/P1 和非常小的 P2。
5. 不新增大模块。

## 6. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch6a-project-initialization.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

如果补了新的主线 smoke，报告中说明脚本名和覆盖范围。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 G4 暂停、Hermes 冻结。
3. 审计了哪些平台主线页面和接口。
4. 105 与另一个真实 NAS 项目抽查结果。
5. P0 / P1 / P2 列表。
6. 修复了哪些文件。
7. 是否触碰 Hermes，若触碰，说明是否仅为 P0/P1 修复。
8. 自测命令结果。
9. 是否建议进入下一轮主线平台功能开发。

# 开发 Agent 当前任务：M2D 真实项目工程主数据接入草案增强

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前批次

`M2D：真实项目工程主数据接入草案增强`

本轮不是新增大功能，也不是继续 BIM / Hermes / NAS 写操作。目标是修复当前真实项目接入的核心问题：工程主数据不能再靠模板演示冒充真实交付标准。

105 项目已经由主 agent 清空演示模板数据，当前状态：

- 真实项目：`503 / 105 / 深圳市二十八高项目`
- 文件资产：2928 条
- 主要文件：DWG 2487、PDF 242、RVT 198、XLSX 1
- 主要专业线索：建筑、结构、电气、给排水、消防、智能化、暖通、燃气、通用
- 工程主数据：已重置为空
- 交付标准：已重置为空

## 1. 必须先阅读

- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/project-105-template-reset-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

重点检查代码：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/controller/ProjectInitializationController.java`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/master-data/api/masterData.ts`
- `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
- `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
- `frontend/src/modules/work-center/pages/DocumentDeliveryPage.vue`
- `frontend/src/modules/work-center/pages/DrawingDeliveryPage.vue`

## 2. 问题定义

当前初始化向导虽然已经叫“真实项目接入向导”，但底层仍然很大程度依赖内置模板骨架。对于 105 这类真实 NAS 项目，如果员工点击确认应用，就可能再次生成演示式工程主数据，并让平台看起来像“标准已就绪”。

这会继续误导用户。

本轮要把主流程改成：

`真实资产目录 -> 接入评估 -> 工程主数据草案 -> 人工确认 -> 再进入标准驱动交付`

而不是：

`点击模板 -> 直接变成已完成交付标准`

## 3. 本轮目标

### A. 接入评估增强

增强现有接口：

- `GET /api/master-data/projects/{projectId}/onboarding/assessment`
- `GET /api/master-data/projects/{projectId}/onboarding/preview`

让返回结果更适合真实项目接入：

- 文件总数、模型数、图纸数、文档/表格数
- 扩展名分布
- 专业分布
- 目录线索，必须是脱敏/相对目录语义，不得返回 `/Volumes`、`nas://`、`smb://`、`storage_uri`
- 治理风险：缺 checksum、缺专业、低置信度等
- 证据模式：始终明确 `catalog_only`
- Missing Evidence：不得把目录元数据说成文件正文证据

### B. 草案从真实资产出发

草案项必须能体现 105 的真实资产线索，而不是纯模板骨架。

至少建议生成这些候选：

- 专业候选：建筑、结构、给排水、暖通、电气、消防、智能化、燃气、通用
- 交付类型候选：RVT 模型、DWG 图纸、PDF 图纸/文档、Excel 清单
- 交付对象候选：项目级、专业级、文件类型级

每条草案项必须有：

- `evidenceSource`
- `evidenceMode=catalog_only`
- `confidenceLevel`
- `riskHint`
- `pendingConfirmation=true`
- 是否来自真实资产线索
- 是否来自模板骨架

### C. 禁止真实 NAS 项目一键套模板变成“标准已就绪”

对 `asset_source` 为 `NAS_REAL*` 的真实项目：

- 前端主流程不再出现“确认应用模板后就完成”的表达。
- 后端 `onboarding/apply` 不得简单调用 `apply-template` 让真实项目直接生成并锁定节点类型。
- 如果保留 `apply-template` 兼容接口，真实 NAS 项目必须要求更强确认，或返回明确错误，防止误操作。
- 不要破坏历史测试项目/脚本的模板兼容能力；只收紧真实 NAS 项目主流程。

优先方案：本轮让真实 NAS 项目的 onboarding apply 只允许“生成/展示草案，不直接落成已就绪标准”。如确实需要落库，必须保证不会自动锁定节点类型，且页面仍显示“待人工确认”。

### D. 前端体验

重点改 `ProjectInitializationPage.vue`：

- 105 首屏显示：真实资产已接入，工程主数据未确认。
- 明确展示文件类型、专业、目录线索、治理风险。
- 文案从“模板”降级为“草案骨架 / 行业参考”。
- 主按钮不应诱导用户“一键应用模板”。
- 给出清楚下一步：先确认部位树，再确认节点类型，再确认交付物标准。
- 技术字段默认不要抢主视觉。

## 4. 明确禁止事项

本轮严禁：

- 不要修改 `docs/**`。
- 不要触碰真实 NAS 文件。
- 不要新增真实 NAS 写能力。
- 不要读取 PDF / Office / DWG / RVT / IFC 正文。
- 不要接入 Hermes 新能力。
- 不要接入 BIM 引擎。
- 不要自动挂接文件。
- 不要自动审核、整改、交付。
- 不要把目录元数据当正文 evidence。
- 不要暴露真实 NAS 路径、`storage_uri`、SQL、token、secret。

## 5. 验收标准

必须满足：

1. 105 项目工程主数据页不再显示“标准已就绪”的误导状态。
2. 105 接入评估展示真实资产统计、专业分布、扩展名分布和治理风险。
3. 草案项能明显区分资产线索与模板骨架。
4. 草案项有证据来源、证据模式、置信度、风险提示和人工确认标记。
5. 真实 NAS 项目不能一键套模板后直接变成 `deliverableStandardReady=true`。
6. 文档/图纸交付在主数据未确认时继续提示先确认工程主数据。
7. 响应和页面不泄露真实 NAS 路径。
8. 不触碰真实 NAS 文件。
9. M2C / M2B / M2A / M1F / M1E / M1D / M1C 回归通过。

## 6. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2c-delivery-package-archive.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-m2a-controlled-nas-write.sh
bash scripts/dev/check-m1f-employee-access-control.sh
bash scripts/dev/check-m1e-file-task-continuity.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

建议新增专项脚本：

`scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`

覆盖 105 项目：

- assessment/preview 响应。
- forbidden fields 扫描。
- 真实 NAS 项目不能一键模板变 ready。
- 文档/图纸交付在未确认主数据时为空或阻塞提示。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改了哪些文件。
- 接入评估如何变得更真实。
- 草案如何从 105 资产线索生成。
- 如何防止真实 NAS 项目再次被模板误导。
- 是否有数据库迁移。
- 是否触碰真实 NAS 文件。
- 自测结果。
- 已知风险。

## 8. 完成定义

只有当 105 项目能清楚展示“真实资产已接入，但工程主数据待确认”，并且不会再被一键模板误导成“交付标准已就绪”，才算完成。

# 开发 Agent 当前任务：M1C 工程主数据真实项目落地

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 当前路线

M1A 已收口，M1B 已通过测试 agent 验收并收口。主线当前为绿灯，但这只代表可以继续平台本体开发，不代表可以进入 9A 客户交付。

当前 active 批次：

`M1C：工程主数据真实项目落地`

完成承诺固定为：

`<promise>MAINLINE_M1C_REAL_PROJECT_MASTERDATA_COMPLETE</promise>`

本批目标不是新增大模块，而是把“真实项目接入向导 / 初始化向导 / 工程主数据 / 交付物标准”从模板演示能力收敛成真实 NAS 项目可用流程。

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
5. `handoff/main-agent/m1b-project-workbench-usability-closure.md`
6. `handoff/main-agent/m1c-real-project-masterdata-plan.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

重点检查：

1. 真实项目接入向导：
   - `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
   - `frontend/src/modules/master-data/api/masterData.ts`
2. 工程主数据页面：
   - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
   - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
   - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
3. 项目工作台入口：
   - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
4. 初始化后端：
   - `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
   - `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/controller/ProjectInitializationController.java`
   - `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`
5. 标准状态与交付链路：
   - `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/status/**`
   - `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/**`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮必须解决的问题

当前真实项目接入仍有几个产品风险：

1. 初始化向导仍容易让用户误以为“套了模板就是真实工程主数据”。
2. 部位树、节点类型、交付标准和真实 NAS 项目资产之间的证据关系不够清楚。
3. 模板提供的是建筑机电/BIM交付基础骨架，不能冒充真实项目结构。
4. 用户需要看懂：
   - 平台从 catalog metadata 看到了什么。
   - 平台缺少什么证据。
   - 草案会创建什么。
   - 哪些内容需要项目负责人复核。
5. 105 项目只能作为样本，后续其他真实 NAS 项目也必须同样适用。

## 3. 本轮目标

完成一条真实项目接入到工程主数据的可解释流程：

`真实项目资产 -> 接入评估 -> 草案预览 -> 人工确认 -> 创建/补齐工程主数据 -> 进入交付闭环`

### A. 接入评估可读

接入评估必须清楚展示：

- 项目是否是真实 NAS 项目。
- 是否已有资产登记。
- 文件数量、模型数、图纸数、文档数。
- 主要扩展名、主要专业线索、主要目录线索。
- 是否已有路径映射或扫描记录。
- 是否已有部位树、节点类型、交付物标准、目录模板。
- 当前阻塞点。
- 下一步动作。

前端文案必须清楚说明：

- 只使用 catalog metadata。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不解析 BIM 构件。
- 不触碰 NAS 文件。
- 目录线索只是辅助判断，不等于真实工程结构。

### B. 草案预览有证据和风险

草案预览不能只是模板清单。需要让用户看懂每类草案项：

- 是模板默认骨架，还是从目录/文件名/项目资产里得到的线索。
- 证据模式是 catalog-only。
- 置信度或风险提示。
- 是否需要人工确认。

如果后端已有字段，前端必须展示；如果字段不足，可以补只读响应字段，但不要大改数据库。

### C. 应用草案必须人工确认且幂等

必须保证：

- 未传 `confirmApply=true` 或等价确认字段时必须拒绝。
- 不覆盖已存在数据。
- 重复应用不会重复创建同名同编码标准项。
- 节点类型已锁定时，不能自动补充会破坏锁定语义的数据。
- 写动作必须留审计。

### D. 应用后进入可维护工程主数据

草案应用后，用户必须能继续进入：

- 部位树。
- 节点类型。
- 交付物标准。
- 文档交付。
- 图纸交付。

页面需要提示“草案已生成，仍需项目负责人复核”，不能表现为真实工程结构已经完全可信。

### E. 多真实项目适用

本批必须至少抽查：

- 105 对应项目。
- 另一个真实 NAS 项目，例如 93/506 或当前环境可用的其他真实项目。

所有逻辑必须基于项目资产、标准状态和权限动态判断，不允许为 105/503 写死。

## 4. 允许做什么

允许：

- 优化真实项目接入向导页面结构、文案和状态表达。
- 补充接入评估只读字段。
- 补充草案预览只读字段。
- 修复草案应用幂等性、确认校验和审计问题。
- 小范围调整项目工作台到初始化向导的入口文案。
- 新增或增强专项脚本，例如 `scripts/dev/check-m1c-real-project-masterdata.sh`。

## 5. 禁止做什么

禁止：

1. 新增 Hermes 能力。
2. 继续 G4。
3. 进入 8B / 8C / 9A。
4. 开放真实 NAS 增删改查。
5. 读取 PDF / Office / DWG / RVT / IFC 正文。
6. 做真实 BIM 轻量化。
7. 做构件级解析。
8. 做 selective indexing。
9. 写 Hermes memory。
10. 写 OpenSearch / Qdrant / MinIO documents/chunks。
11. Agent 自动审批、自动整改、自动挂接。
12. 把 catalog metadata 冒充正文 evidence。
13. 返回真实 NAS 路径、raw row、SQL、token、secret、password。
14. 为 105 / 503 写死特殊逻辑。
15. 修改 `docs/**`，除非主 agent 单独授权。

## 6. 建议执行方式

建议按这个顺序做：

1. 先审计当前 `ProjectInitializationPage.vue` 和 `ProjectInitializationApplicationService` 已有 onboarding 能力。
2. 对照 105/503 与 93/506 两个真实项目，看接入评估是否真实可读。
3. 修复评估摘要和草案预览中的模糊字段，补充证据来源、catalog-only、风险提示和人工确认要求。
4. 检查 `onboarding/apply` 未确认拒绝、重复应用不重复创建、节点锁定时不破坏规则。
5. 优化应用结果和后续入口，让用户能继续维护部位树、节点类型和交付标准。
6. 加一个最小专项脚本覆盖核心合同和安全红线。

## 7. 自测要求

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
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-mock.sh
git diff --check
```

如果新增 `scripts/dev/check-m1c-real-project-masterdata.sh`，必须执行并记录。

浏览器自测至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/506/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/master-data/node-types`
- `/data-steward/assets/503/master-data/deliverable-standard`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`

说明：业务编码 `105` 对应内部项目 ID `503`，业务编码 `93` 对应内部项目 ID `506`。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 M1C 当前 active。
3. 是否确认 G4 / Hermes / 8B / 9A 冻结。
4. 接入评估改了什么。
5. 草案预览如何体现证据来源、catalog-only 和人工确认。
6. 应用草案如何保证确认校验、幂等和不覆盖。
7. 105/503 与 93/506 抽查结果。
8. 是否触碰后端；如果触碰，说明是否只在 master-data/onboarding 范围。
9. 修改文件清单。
10. 自测命令结果。
11. P0 / P1 / P2 列表。
12. 是否建议进入 M1C 测试验收。

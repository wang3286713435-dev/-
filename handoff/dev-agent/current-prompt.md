# 开发 Agent 当前任务：G3 Hermes 工程主数据交付路径完善 MVP

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

本轮是 G2 收口后的主线可用性修复批次：

`G3：Hermes 工程主数据交付路径完善 MVP`

## 0. 命名与路线冻结

- G2 已收口并完成 Git checkpoint。
- 当前进入 `G3`。
- 不进入 `8B / 8C / 9A`。
- 8B BIM 轻量化任务编排已后置。
- 本轮目标不是增加平台大功能，而是把已有工程主数据、真实项目接入、交付治理和 Hermes 串成可用路径。

完成承诺固定为：

`<promise>PHASE2_G3_HERMES_MASTERDATA_GUIDANCE_COMPLETE</promise>`

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
5. `handoff/main-agent/hermes-layered-integration-decision.md`
6. `handoff/dev-agent/latest-report.md`
7. `handoff/test-agent/latest-report.md`
8. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
9. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
10. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
11. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`

重点检查：

1. `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
2. `frontend/src/modules/data-steward/components/DataStewardAnswerCard.vue`
3. `frontend/src/modules/data-steward/api/dataSteward.ts`
4. `frontend/src/modules/core/layout/AppLayout.vue`
5. `frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`
6. `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
7. `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
8. `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
9. `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
10. `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
11. `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
12. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/*`
13. `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/*`
14. `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/*`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 背景

当前平台已经有：

- 真实 NAS 项目资产目录。
- 项目工作台。
- 真实项目接入向导。
- 工程主数据：部位树、节点类型、交付物标准。
- 交付治理助手。
- 文档 / 图纸交付。
- 批量挂接、审核、整改、导出预检查。
- Hermes 常驻入口和真实外部 Hermes catalog-only 回答。

但用户仍然不容易理解：

- 当前页面有什么用。
- 工程主数据为什么要配置。
- 部位树、节点类型、交付物标准之间是什么关系。
- 当前项目离交付闭环还差什么。
- 下一步应该点哪里。

本轮不是继续做新模块，而是让 Hermes 成为平台内“懂当前项目、懂当前页面、懂工程主数据交付路径”的常驻引导者。

## 3. 本轮核心目标

让用户进入 105 或其他真实 NAS 项目后，即使不看说明书，也可以通过 Hermes 理解：

- 当前项目处于真实接入 / 工程主数据 / 交付治理哪一步。
- 当前页面是干什么的。
- 当前页面下一步应该做什么。
- 工程主数据缺什么。
- 为什么模板只是草案。
- 为什么要建部位树。
- 为什么节点类型要锁定。
- 交付物标准如何服务文档 / 图纸交付。
- 哪些问题只能目录级回答，哪些问题缺少正文证据。

## 4. 必须完成

### A. Hermes 问题分类修正

Hermes 必须区分两类问题：

1. 平台使用 / 当前项目流程问题
   - 例如：
     - `这个页面是干什么的？`
     - `我下一步应该做什么？`
     - `部位树有什么用？`
     - `节点类型为什么要锁定？`
     - `交付物标准和图纸交付是什么关系？`
   - 这些问题可以基于平台上下文、项目状态、工程主数据状态回答。
   - 不应因为没有文件正文而返回 Missing Evidence。

2. 文件正文 / 图纸 / 模型内部内容问题
   - 例如：
     - `这个 PDF 第三页写了什么？`
     - `这个 DWG 里面有哪些设备？`
     - `这个 RVT 里面有哪些构件参数？`
   - 当前必须返回 Missing Evidence。
   - 不得编造正文、图层、构件、参数或模型内部内容。

### B. 页面级 Hermes 上下文

至少覆盖以下页面：

- `/data-steward/assets`
- `/data-steward/assets/:projectId`
- `/data-steward/assets/:projectId/master-data/initialization`
- `/data-steward/assets/:projectId/master-data/sections`
- `/data-steward/assets/:projectId/master-data/node-types`
- `/data-steward/assets/:projectId/master-data/deliverable-standard`
- `/data-steward/assets/:projectId/work/agent-governance`
- `/data-steward/assets/:projectId/work/document-delivery`
- `/data-steward/assets/:projectId/work/drawing-delivery`
- `/data-steward/assets/:projectId?tab=files`

Hermes 请求必须带：

- 当前路由。
- 页面类型。
- 页面标题。
- 项目 ID。
- 项目编码 / 名称。
- 当前页面业务用途。
- 当前页面下一步建议。

前端仍只能请求平台后端 `/api/data-steward/chat`，禁止直连外部 Hermes。

### C. 工程主数据交付路径引导

Hermes 必须能解释以下路径：

`真实项目资产 -> 接入评估 -> 主数据草案 -> 部位树确认 -> 节点类型锁定 -> 交付物标准确认 -> 文档/图纸缺失项 -> 候选文件推荐 -> 人工确认挂接 -> 审核/整改 -> 导出预检查`

回答必须面向业务用户，不堆技术字段。

### D. 只读项目状态摘要

如 Hermes 需要项目状态，请优先复用已有只读接口或服务：

- 真实项目接入 assessment / preview。
- G1 交付治理 overview / missing-items。
- 文档 / 图纸交付完整率。
- 交付包导出预检查。
- catalog-only 文件目录摘要。

如确实需要新增后端聚合，只能新增只读、脱敏、项目权限校验后的上下文摘要；不得新增数据库结构。

### E.  route-aware 快捷问题

Hermes 面板快捷问题应根据当前页面变化。

示例：

- 接入向导页：
  - `这个项目接入到哪一步了？`
  - `为什么这里显示草案？`
  - `我现在能不能应用模板？`
- 部位树页：
  - `部位树有什么用？`
  - `这个项目部位树还缺什么？`
  - `下一步是不是节点类型？`
- 节点类型页：
  - `节点类型为什么要锁定？`
  - `没锁定会影响什么？`
- 交付物标准页：
  - `交付物标准和文档图纸交付有什么关系？`
  - `标准不完整会影响哪里？`
- 交付治理助手页：
  - `当前项目离交付完成还差什么？`
  - `哪些文件可能可以补交？`

### F. 通用真实项目

105 只是验收样本，不允许硬编码。

至少保证另一个真实 NAS 项目也能使用同一套 Hermes 引导能力。

## 5. 禁止事项

本轮严禁：

1. 不进入 8B / 8C / 9A。
2. 不做 BIM 轻量化引擎。
3. 不做真实模型转换。
4. 不做构件级解析。
5. 不读取 PDF / Office / DWG / RVT / IFC 正文。
6. 不做 selective indexing。
7. 不写 Hermes memory。
8. 不写 OpenSearch / Qdrant / MinIO documents/chunks。
9. 不做真实 NAS 增删改查。
10. 不让 Agent 自动写库。
11. 不让 Agent 自动创建部位树、节点类型或交付物标准。
12. 不让 Agent 自动挂接、自动审批、自动整改。
13. 不新增大量页面。
14. 不把 catalog metadata 冒充正文 evidence。
15. 不返回真实 NAS 路径、raw row、SQL、token、secret、password。

## 6. 验收标准

至少满足：

1. Hermes 在工程主数据、接入向导、交付治理、文档/图纸交付页面均可用。
2. 问 `这个页面是干什么的？` 能得到当前页面相关回答。
3. 问 `我下一步应该做什么？` 能给出符合当前页面和当前项目状态的建议。
4. 问 `部位树有什么用？`、`节点类型为什么要锁定？`、`交付物标准有什么用？` 能得到业务化解释。
5. 问当前项目交付缺口时，能基于现有项目状态给出 catalog-only / platform-context 回答。
6. 问 PDF / DWG / RVT / BIM 构件正文类问题，仍返回 Missing Evidence。
7. 不泄露真实 NAS 路径、raw row、SQL、token。
8. 105 和另一个真实 NAS 项目均可用。
9. G2 / G1 / 8A 回归不破坏。

## 7. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh
bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

如果新增 G3 专项脚本，命名建议：

`scripts/dev/check-phase2-insert-g3-hermes-masterdata-guidance.sh`

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 修改了哪些文件。
2. Hermes 如何区分平台流程问题和正文证据问题。
3. 页面上下文如何传递。
4. 工程主数据路径如何解释。
5. 105 和另一个真实项目的验证结果。
6. 是否新增只读聚合接口。
7. 是否有任何 Missing Evidence 边界变化。
8. 自测命令结果。
9. 未完成事项和风险。
10. 完成承诺：

`<promise>PHASE2_G3_HERMES_MASTERDATA_GUIDANCE_COMPLETE</promise>`

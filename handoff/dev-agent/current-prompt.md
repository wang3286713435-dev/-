# 开发 Agent 当前任务：G3 Hermes 平台工作型 Agent MVP

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

本轮是 G2 收口后的 Hermes 主线强化批次：

`G3：Hermes 平台工作型 Agent MVP`

## 0. 路线冻结

- G2 已收口并完成 Git checkpoint。
- 当前进入 `G3`。
- 不进入 `8B / 8C / 9A`。
- 8B BIM 轻量化任务编排继续后置。
- 本轮重点不是继续增加平台页面，而是让 Hermes 从“平台问答 AI”升级为“能通过平台受控能力替员工做事的 Agent”。

完成承诺固定为：

`<promise>PHASE2_G3_HERMES_WORKING_AGENT_COMPLETE</promise>`

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
5. `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
6. `frontend/src/modules/work-center/api/delivery.ts`
7. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/*`
8. `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/*`
9. `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/*`
10. `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/*`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 核心产品判断

Hermes 不能只是问答框。它必须能在平台内完成受控工作：

- 读当前项目状态。
- 看工程主数据缺口。
- 生成补齐计划。
- 生成交付缺失项补交方案。
- 推荐候选文件。
- 在用户人工确认后调用平台已有能力执行。
- 执行后给出结果并留审计。

关键原则：

`Hermes 可以通过平台做事，但不能绕过平台做事。`

## 3. 本轮必须完成

### A. Hermes Action Center

在 Hermes 面板中增加清晰的 Agent 工作区。

至少区分：

- `回答`
- `操作草案`
- `待人工确认`
- `执行结果`

用户要能一眼看出：

- Hermes 现在只是解释。
- Hermes 生成了一个可执行方案。
- 这个方案还没执行。
- 用户确认后才会执行。
- 执行成功 / 跳过 / 失败多少条。

### B. 受控工具模型

后端 Gateway 或业务服务中建立受控工具口径。可以是最小实现，不必做复杂插件系统。

至少提供这些逻辑能力：

1. Read Tool：读取项目交付状态。
2. Read Tool：读取工程主数据状态。
3. Read Tool：读取交付缺失项。
4. Plan Tool：生成工程主数据补齐计划。
5. Plan Tool：生成缺失项补交 / 文件挂接推荐方案。
6. Action Tool：人工确认后应用推荐挂接。

可以复用已有 G1 接口：

- `GET /api/work-center/projects/{projectId}/agent-governance/overview`
- `GET /api/work-center/projects/{projectId}/agent-governance/missing-items`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommend-bindings`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommendations:apply`

如新增接口，必须仍在平台后端，不允许前端直连 Hermes。

### C. 工程主数据 Agent Plan

Hermes 必须能生成工程主数据补齐计划。

计划内容至少包含：

- 当前项目是否完成真实接入。
- 是否存在部位树。
- 节点类型是否存在、是否锁定。
- 交付物标准是否存在。
- 文档 / 图纸交付是否可以进入治理。
- 下一步建议。
- 对应页面入口。

注意：

- 本轮不允许 Hermes 自动创建部位树、节点类型或交付物标准。
- 只能生成计划和跳转建议。

### D. 交付缺失项 Agent Plan

Hermes 必须能基于现有交付治理能力生成缺失项补交方案。

方案内容至少包含：

- 缺失项。
- 推荐文件。
- 推荐理由。
- 置信度。
- 风险。
- 是否需要先治理元数据。

### E. 人工确认后执行推荐挂接

用户必须明确确认后才能执行。

要求：

- 前端展示执行前摘要。
- 用户必须勾选或点击确认。
- 请求必须带 `confirmed=true` 或等价确认字段。
- 后端必须重新校验项目权限和项目上下文。
- 执行结果展示创建、跳过、失败和失败原因。
- 审计必须可追踪。

可以优先复用 G1 已有推荐挂接应用能力，不重复造一套挂接逻辑。

### F. 问题分类仍要正确

Hermes 必须区分：

- 平台流程 / 工程主数据 / 交付治理问题：可以回答并生成计划。
- 文件正文 / DWG / RVT / BIM 构件问题：必须 Missing Evidence，不得编造。

## 4. 禁止事项

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
10. 不让 Hermes 直接写数据库。
11. 不让 Hermes 绕过平台权限。
12. 不让 Hermes 未经确认自动挂接、自动审批、自动整改。
13. 不把 catalog metadata 冒充正文 evidence。
14. 不返回真实 NAS 路径、raw row、SQL、token、secret、password。
15. 不为 105 写死特殊逻辑。

## 5. 验收标准

至少满足：

1. Hermes 面板中存在可执行建议 / 操作草案 / 人工确认执行区域。
2. Hermes 能生成工程主数据补齐计划。
3. Hermes 能生成交付缺失项补交 / 文件挂接推荐方案。
4. 用户未确认时，任何写动作都不能执行。
5. 用户确认后，可以通过平台既有能力执行推荐挂接。
6. 执行结果能展示创建、跳过、失败及失败原因。
7. 执行动作有审计。
8. 105 和另一个真实 NAS 项目均可用。
9. 正文 / DWG / RVT / BIM 构件类问题仍返回 Missing Evidence。
10. G2 / G1 / 8A 回归不破坏。

## 6. 自测要求

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

建议新增 G3 专项脚本：

`scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`

专项脚本至少覆盖：

- 未确认不能执行。
- 确认后可应用推荐挂接。
- 响应不泄露敏感字段。
- 正文类问题 Missing Evidence。
- 105 和另一个真实项目均可生成计划。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 修改了哪些文件。
2. Hermes Action Center 如何工作。
3. 工程主数据补齐计划如何生成。
4. 缺失项补交方案如何生成。
5. 人工确认后执行调用了哪个既有平台能力。
6. 审计如何留痕。
7. 105 和另一个真实项目的验证结果。
8. Missing Evidence 边界是否保持。
9. 自测命令结果。
10. 未完成事项和风险。
11. 完成承诺：

`<promise>PHASE2_G3_HERMES_WORKING_AGENT_COMPLETE</promise>`

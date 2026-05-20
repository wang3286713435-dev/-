# 开发 Agent 当前任务：G4 真实项目交付闭环试运行与问题修补

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

本轮是 G3 收口后的真实项目试运行批次：

`G4：真实项目交付闭环试运行与问题修补`

## 0. 路线冻结

- G3 已收口。
- 当前进入 `G4`。
- 不进入 `8B / 8C / 9A`。
- 8B BIM 轻量化任务编排继续后置。
- G4 不是新功能扩张批次，而是用真实 NAS 项目跑通现有数字化交付闭环，并修复真实使用中暴露的 P0/P1/P2。

完成承诺固定为：

`<promise>PHASE2_G4_REAL_PROJECT_DELIVERY_TRIAL_COMPLETE</promise>`

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/phase2-g4-real-project-delivery-trial-plan.md`
5. `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
6. `handoff/main-agent/hermes-layered-integration-decision.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`

共享文档只读参考：

1. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
2. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
3. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
4. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`

重点检查：

1. `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
2. `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
3. `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
4. `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
5. `frontend/src/modules/work-center/pages/DocumentDeliveryPage.vue`
6. `frontend/src/modules/work-center/pages/DrawingDeliveryPage.vue`
7. `frontend/src/modules/work-center/pages/RectificationsPage.vue`
8. `frontend/src/modules/work-center/api/delivery.ts`
9. `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/*`
10. `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/*`
11. `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/*`
12. `scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`
13. `scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
14. `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮核心判断

G4 不问“再加什么功能”，只问“真实员工能不能用现有功能跑完一条数字化交付闭环”。

必须用真实 NAS 项目验证：

- `105` 项目作为主样本。
- 至少另一个真实 NAS 项目作为泛化样本。

禁止为 105 写死逻辑。

## 3. 本轮必须完成

### A. 真实项目试运行

至少跑通并记录这条链路：

1. 从 `/data-steward/assets` 进入真实 NAS 项目。
2. 查看项目接入状态和资产概况。
3. 进入项目工作台。
4. 打开 Hermes，询问当前项目如何进入数字化交付。
5. 使用 Hermes Action Center 生成工程主数据补齐计划。
6. 检查工程主数据页面：部位树、节点类型、交付物标准。
7. 使用 Hermes 生成交付缺失项补交 / 文件挂接推荐方案。
8. 人工确认后调用现有推荐挂接能力。
9. 查看文档交付 / 图纸交付完整率和缺失项状态。
10. 进入审核 / 整改 / 导出预检查。
11. 查看审计记录。

### B. 只修试运行中暴露的问题

允许修：

- 真实项目入口、跳转、项目上下文丢失。
- Hermes 状态、提示、Action Center 结果不清楚。
- 工程主数据页面缺少从真实项目回来的入口。
- 推荐挂接后页面不刷新或结果表达不清楚。
- 文档 / 图纸交付页面状态与推荐挂接结果不一致。
- 审核 / 整改 / 导出预检查入口不清楚。
- 空状态、失败提示、权限提示、Missing Evidence 提示不适合普通员工。
- G4 专项脚本缺失或无法复跑。
- 不改变底层模型的小型只读聚合或前端展示修复。

不允许把 G4 扩成新平台模块。

### C. 补 G4 专项脚本

新增或增强：

`scripts/dev/check-phase2-insert-g4-real-project-delivery-trial.sh`

脚本至少覆盖：

- 真实项目存在性检查。
- 105 与另一个真实 NAS 项目的基本可查。
- agent-governance overview / missing-items / recommend-bindings / apply 的安全口径。
- 未确认 apply 必须被拒绝。
- 导出预检查 dry-run。
- 不泄露 `/Volumes`、`smb://`、`nas://`、`storage_path`、raw row、SQL、token、secret、password。

如脚本需要创建测试数据，必须使用可识别前缀，并在报告里说明是否清理。

### D. 保持 Hermes 边界

Hermes 可以：

- 解释当前项目状态。
- 生成工程主数据补齐计划。
- 生成交付缺失项补交 / 文件挂接推荐方案。
- 在用户人工确认后，通过平台既有接口执行推荐挂接。

Hermes 不可以：

- 自动写库。
- 自动挂接。
- 自动审批。
- 自动整改。
- 读取文件正文。
- 伪造 BIM / DWG / RVT / 构件内容。

## 4. 明确禁止事项

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
16. 不修改 `docs/**`。

## 5. 验收标准

至少满足：

1. 105 项目可以完成真实项目交付闭环试运行。
2. 至少另一个真实 NAS 项目可以完成同类抽查。
3. Hermes 能解释项目接入状态、工程主数据状态、交付缺失项和下一步动作。
4. Hermes 能生成工程主数据补齐计划和交付缺失项补交推荐方案。
5. 未人工确认时不能执行写动作。
6. 人工确认后推荐挂接能通过平台既有能力执行。
7. 文档 / 图纸交付完整率和缺失项状态能刷新。
8. 审核、整改、导出预检查可进入并保持项目上下文。
9. 关键动作有审计。
10. 正文 / DWG / RVT / BIM 构件类问题仍返回 Missing Evidence。
11. 不泄露真实 NAS 路径、raw row、SQL、token、secret、password。
12. G3 / G2 / G1 / 8A 回归不破坏。

## 6. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-insert-g4-real-project-delivery-trial.sh
bash scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh
bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh
bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

如果本机服务未启动，按项目既有脚本启动。不要在报告中输出 token、cookie、密码或真实 NAS 路径。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 根因 / 试运行发现。
2. 修改了哪些文件。
3. 105 项目试运行结果。
4. 另一个真实 NAS 项目抽查结果。
5. Hermes 是否能给出工程主数据和交付缺失项可执行计划。
6. 未确认不能执行、确认后执行的验证结果。
7. 文档 / 图纸交付、审核、整改、导出预检查回归结果。
8. 安全边界检查结果。
9. 自测命令结果。
10. 是否仍有 P0 / P1 / P2。

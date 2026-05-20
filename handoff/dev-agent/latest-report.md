# 开发 Agent 报告：G3 Hermes 平台工作型 Agent MVP

时间：2026-05-20

## 1. 本轮目标

本轮按 `G3：Hermes 平台工作型 Agent MVP` 开发。目标是让前端 Hermes 不只停留在问答，而是在 `catalog-only / read-only / permission-aware / Missing Evidence` 边界内，具备平台受控的“计划、人工确认、执行结果”工作区。

命名保持 `G3`。未进入 8B / 8C / 9A，未新增 H1/R1 等临时命名。

## 2. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
- `handoff/main-agent/hermes-layered-integration-decision.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- 共享文档：
  - `agent-briefings/hermes_capability_handoff.md`
  - `integration-contracts/platform_to_hermes_contract.md`
  - `integration-contracts/gateway_response_contract.md`
  - `integration-contracts/missing_evidence_policy.md`

未修改共享文档空间，未修改 `docs/**`。

## 3. 修改文件

- `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
- `scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`
- `handoff/dev-agent/latest-report.md`

未修改后端源码，未新增数据库迁移，未修改旧 Flyway migration。

当前 worktree 仍有非本轮产生的既有改动：

- `.claude/ralph-loop.local.md`
- `.claude/ralph/progress.txt`
- `tmp/run-logs/backend.pid`
- `tmp/run-logs/frontend.pid`
- `tmp/jar-inspect/`
- `tmp/run-logs/hermes-gateway.pid`

本轮未回退这些既有改动。

## 4. Action Center

`DataStewardPanel.vue` 新增 `Hermes Action Center`，清晰区分：

- `回答`：继续保留自然语言问答，仍按 catalog-only / Missing Evidence 边界。
- `操作草案`：生成工程主数据补齐计划、缺失交付补交方案。
- `待人工确认`：展示推荐挂接项、风险、置信度和人工确认复选框。
- `执行结果`：展示平台挂接执行后的创建、跳过、失败结果。

页面明确文案：

- 计划只通过平台受控接口生成。
- 不创建主数据。
- 不读取文件正文。
- 不自动挂接。
- 必须人工勾选确认后才调用平台能力。

## 5. 工程主数据计划

Action Center 复用只读接口：

- `GET /api/master-data/projects/{projectId}/onboarding/assessment`
- `GET /api/work-center/projects/{projectId}/agent-governance/overview`

前端生成的主数据计划包含：

- 接入状态。
- 部位树状态和节点数量。
- 节点类型是否存在、是否锁定。
- 交付标准状态、交付定义和交付类型数量。
- 文档 / 图纸准备度。
- 下一步建议。
- 页面入口：真实项目接入向导、部位树、节点类型、交付物标准、文档交付、图纸交付。

未调用 `onboarding/apply`，未自动创建主数据。

## 6. 缺失交付计划

Action Center 复用 G1 已收口接口：

- `GET /api/work-center/projects/{projectId}/agent-governance/missing-items`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommend-bindings`

前端展示：

- 缺失项数量。
- 推荐文件数量。
- 缺失目标、交付类型、解释。
- 推荐文件、版本、推荐原因。
- 置信度。
- 风险提示。
- 是否建议先治理元数据。

推荐仍只看当前项目目录元数据，不读取 PDF / Office / DWG / RVT / IFC 正文，不把 catalog metadata 当正文 evidence。

## 7. 人工确认与执行

Action Center 的执行动作复用 G1 已收口接口：

- `POST /api/work-center/projects/{projectId}/agent-governance/recommendations:apply`

执行要求：

- 前端必须选择推荐项。
- 前端必须勾选人工确认。
- 请求体显式带 `confirmed=true`。
- 后端 `AgentGovernanceController` 继续通过 `requireCurrentProject` 校验项目上下文。
- 后端 `AgentGovernanceApplicationService.applyRecommendations` 继续拒绝 `confirmed=false`。
- 后端继续复用批量挂接能力，并记录 `work.agent-governance.apply` 审计。

本轮没有绕过平台权限，没有让 Hermes 直接写 DB，没有让 Hermes 自动挂接。

## 8. 后端与审计

本轮未新增后端接口，原因是 G1 已经提供了符合 G3 最小受控工具模型的后端能力：

- 读取项目交付治理状态。
- 读取缺失交付项。
- 生成推荐挂接方案。
- 人工确认后应用推荐。
- 权限校验。
- 审计记录。

G3 前端 Action Center 只把这些受控平台能力组合成 Hermes 工作区。审计链路通过既有 `work.agent-governance.recommend` 与 `work.agent-governance.apply` 保持。

## 9. 安全边界

确认保持：

- 未让 Hermes 裸连 DB。
- 未让 Agent 生成 SQL。
- 未做 Agent DB CRUD。
- 未做 NAS scan。
- 未做 parser / writer / indexing。
- 未写 Hermes memory。
- 未写 OpenSearch / Qdrant / MinIO。
- 未做 BIM 轻量化、模型转换或构件解析。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未触碰真实 NAS 文件。
- 未创建、移动、删除、重命名或上传 NAS 文件。
- 未暴露 raw `storage_path` / `storage_uri` / NAS 原始路径。
- 未暴露 raw DB row、SQL、secret、token、password。
- 未做 production rollout。

## 10. 新增 G3 Smoke

新增：

`scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`

覆盖：

- Hermes 面板包含 Action Center、操作草案、待人工确认、执行结果。
- 105 项目和另一个真实项目都能读取主数据计划输入。
- 105 项目和另一个真实项目都能读取交付治理状态、缺失项、文档 / 图纸推荐方案。
- RVT / DWG / BIM / 构件内容类问题返回 Missing Evidence。
- 未人工确认时拒绝执行挂接。
- 人工确认后才调用平台挂接能力。
- 审计日志包含 apply 记录。
- 响应未发现 raw path、storage 字段、SQL、密钥痕迹。

执行结果：

- `bash scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`
- `PASS=21 FAIL=0`

## 11. 浏览器回归

通过浏览器短回归验证：

- 页面：`/data-steward/assets/503/work/agent-governance`
- Hermes 抽屉可打开。
- `Hermes Action Center` 可见。
- `操作草案` 可生成工程主数据补齐计划。
- `待人工确认` 区展示“不会自动挂接，必须勾选推荐、勾选人工确认，再由平台后端二次校验后执行”。
- Hermes 面板文本未发现 `storage_path / storage_uri / storagePath / storageUri / nas:// / smb:// / /Volumes/`。

## 12. 验证结果

- 后端构建：通过。
  - `./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过。
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 健康检查：通过。
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - `{"status":"UP"}`
- Hermes Gateway 回归：通过。
  - `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh`
  - `PASS=13 FAIL=0`
- G3 专项：通过。
  - `bash scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`
  - `PASS=21 FAIL=0`
- G2 回归：通过。
  - `bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
  - `PASS=11 FAIL=0`
- G1 回归：通过。
  - `bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`
  - `PASS=34 FAIL=0`
- 8A 回归：通过。
  - `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
  - `PASS=11 FAIL=0`
- `git diff --check`：通过。

## 13. 已知风险与未完成事项

- 当前 G3 是平台受控工作区 MVP，不是 Hermes 自主多工具编排，也不是 production rollout。
- Action Center 的执行动作仍是用户点选按钮触发，不是让 Hermes 自动决定并执行治理。
- 推荐依据仍为目录元数据和平台已有规则，置信度不足或元数据缺失时仍需要人工判断。
- 如果当前项目没有候选文件，待人工确认区会为空，需要先治理文件元数据。
- 后续若要让 Hermes 侧继续深度对接，建议先定义 Hermes 返回的 action intent / draft action schema，但执行仍必须落到平台 Gateway 和人工确认链路。

## 14. 给测试 Agent 的测试 Prompt

````md
# 测试 Agent Prompt：G3 Hermes 平台工作型 Agent MVP 验收

工作目录：`/Users/vc/Documents/数字化交付平台`

本轮只验收 `G3：Hermes 平台工作型 Agent MVP`。G3 是 G2 之后的平台受控工作区能力，不代表进入 8B / 8C / 9A，不测试生产发布，不测试真实 NAS 写操作，不测试正文解析、BIM 构件解析、selective indexing 或 Hermes memory 写入。

必须先读：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
- `handoff/main-agent/hermes-layered-integration-decision.md`
- 共享文档中的 Hermes capability handoff、platform_to_hermes_contract、gateway_response_contract、missing_evidence_policy

重点验收：

1. Hermes 面板
   - `/data-steward/assets/503/work/agent-governance` 打开 Hermes。
   - 可见 `Hermes Action Center`。
   - 明确区分 `回答 / 操作草案 / 待人工确认 / 执行结果`。
2. 操作草案
   - 生成主数据补齐计划。
   - 计划包含接入状态、部位树、节点类型、交付标准、文档/图纸准备度、下一步和页面入口。
   - 不得自动创建主数据。
   - 生成文档 / 图纸缺失交付方案。
   - 方案包含缺失项、推荐文件、原因、置信度、风险、是否需要元数据治理。
3. 人工确认
   - 未勾选推荐和人工确认时不能执行。
   - 请求必须带 `confirmed=true` 后才可调用平台挂接。
   - 后端必须继续校验项目权限和上下文。
   - 执行结果必须展示创建、跳过、失败和原因。
   - 审计日志必须有 recommend / apply 记录。
4. Hermes 边界
   - 正文 / DWG / RVT / BIM / 构件问题必须 Missing Evidence。
   - 不得编造模型内容、图层、构件参数、PDF/Office 正文。
   - 不得出现 raw `storage_path`、`storage_uri`、NAS 原始路径、raw DB row、SQL、secret/token/password。
5. 回归
   - G2、G1、8A 仍通过。

必须执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh
bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh
bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

浏览器建议：

- 打开 `/data-steward/assets/503/work/agent-governance`。
- 打开 Hermes。
- 点击 `操作草案`，生成主数据计划。
- 切换文档 / 图纸，生成缺失交付方案。
- 进入 `待人工确认`，检查必须勾选后才可确认执行。
- 提问：`这个 RVT 里面有哪些构件参数、DWG 图层和模型内容？`，必须 Missing Evidence。
````

<promise>PHASE2_G3_HERMES_WORKING_AGENT_COMPLETE</promise>

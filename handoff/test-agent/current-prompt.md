# 测试 Agent 当前任务：G2-B 既有真实项目治理可用性补丁验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`G2-B：既有真实项目治理可用性补丁`

注意：

- 仍属于 G2。
- 不进入 8B / 8C / 9A。
- 不测试真实 BIM 轻量化。
- 不测试真实 NAS 增删改查。
- 不测试正文抽取、selective indexing 或 Agent 自动治理。
- 105 只是验收样本，不能作为硬编码能力。

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/test-agent/latest-report.md`
4. `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
5. `handoff/main-agent/phase2-g2-naming-freeze.md`
6. `handoff/main-agent/hermes-layered-integration-decision.md`
7. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
8. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
9. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`
10. `handoff/main-agent/status.md`
11. `handoff/main-agent/phase2-current-roadmap.md`

## 1. 验收目标

确认 G2-B 是否真正解决：

1. 资产总览 Hero 区父子结构不清晰。
2. 用户不知道各功能有什么用。
3. 真实项目治理路径不够明确。
4. Hermes 还不是平台常驻式助手。
5. 105 只是样本，能力必须适用于其他真实 NAS 项目。
6. Hermes 接入仍停留在 Catalog Layer，未偷跑 Evidence / Memory / Orchestration。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh
bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

如果外部 Hermes token 未注入，额外 Hermes 真服务检查失败可记录为环境 P2；本地 Gateway 降级链路必须通过。

## 3. 页面验收

### A. 资产总览 Hero 区

打开：

`http://127.0.0.1:5173/data-steward/assets`

必须确认：

- Hero 区说明平台目标：真实项目接入、工程主数据准备、交付治理闭环。
- Hero 区能区分项目状态：
  - 真实 NAS 项目。
  - 已登记资产。
  - 已初始化主数据。
  - 已进入交付治理。
  - 待接入 / 待治理。
- Hero 区提供下一步动作：
  - 进入真实项目。
  - 查看接入评估。
  - 完善工程主数据。
  - 进入交付治理助手。
- Hero 区展示风险提醒：
  - 缺工程主数据。
  - 缺交付标准。
  - 待审核。
  - 缺 checksum。
  - 低置信度。
- 文案能说明“这个功能有什么用”。
- 页面无横向撑爆。

### B. 通用真实项目治理路径

验证 105 项目，同时再选至少 1 个其他真实 NAS 项目，例如 `100 / 104 / 108 / 109` 中任意一个。

必须确认两个项目都能看到通用治理路径：

`资产目录 -> 接入评估 -> 工程主数据草案 -> 交付治理助手 -> 缺失项解释 -> 人工确认挂接`

确认：

- 当前项目在哪一步可读。
- 下一步动作可读。
- 为什么要做这一步可读。
- 不存在 105 专属硬编码痕迹。

### C. Hermes 常驻入口

必须在以下页面检查 Hermes 常驻入口：

- `/data-steward/assets`
- 任一真实项目工作台。
- 真实项目接入向导。
- 交付治理助手。

确认：

- 常驻入口可见。
- 可打开 Hermes 面板。
- 提问时带当前页面上下文。
- 在项目页提问时带当前项目上下文。
- Hermes 能回答或引导：
  - 这个页面是干什么的？
  - 我下一步应该做什么？
  - 当前项目处于哪一步？
  - 为什么模板只是草案？
  - 工程主数据缺什么？
  - 交付治理助手能做什么？
- Hermes 外部不可用时安全降级，不白屏、不 500。

同时确认 Hermes 分层边界：

- 当前只做 Catalog Layer。
- 正文 / DWG / RVT / BIM / 构件类问题仍 Missing Evidence。
- 没有 `document_evidence_search`。
- 没有 PDF / Office 正文问答。
- 没有 DWG / RVT / BIM 内容理解。
- 没有 Hermes long-term memory 写入 raw path、raw row、catalog row 或文件正文。
- 没有多 Agent 编排或自动治理。

### D. smoke 项目分类

在“全部”筛选中检查历史 smoke 项目：

- `B6A-SMOKE-*`
- `PHASE2-*`
- `PH2*`
- 包含 `SMOKE`
- 包含 `TEST`
- 包含 `测试`

必须确认这些项目归类为测试项目，而不是手工/API 或真实项目。

默认真实项目视图不得混入这些项目。

## 4. 禁止项检查

必须确认没有：

- 进入 8B / 8C / 9A。
- 真实 NAS 增删改查。
- 文件移动、删除、重命名、上传。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- BIM 构件级解析。
- selective indexing。
- 写 Hermes memory。
- 写 OpenSearch / Qdrant / MinIO documents/chunks。
- Agent 自动审批。
- Agent 自动整改。
- Agent 自动创建真实交付结论。
- 前端直连 Hermes。
- 为 105 写死特殊逻辑。
- 实现或伪实现 Evidence Layer / Memory Layer / Orchestration Layer。

## 5. P0 / P1 / P2 判定

P0：

- 真实项目默认视图混入测试 / 样例项目。
- 105 之外的真实项目无法看到治理路径。
- Hermes 常驻入口导致页面白屏或 500。
- Hermes 前端直连外部服务或泄露 token。
- 暴露真实 NAS 路径、raw row、SQL、token、cookie、password。
- Agent 自动写库、自动审批、自动整改或触碰 NAS。

P1：

- Hero 区仍看不懂，用户无法判断各入口作用。
- 父子结构仍不清楚。
- Hermes 常驻入口只在单页出现，不覆盖核心页面。
- Hermes 不携带当前项目 / 当前页面上下文。
- Hermes 对正文 / BIM / 构件问题未返回 Missing Evidence。
- smoke 项目仍被归类为手工/API 或真实项目。
- 发现 105 硬编码。

P2：

- 文案仍可润色，但不影响用户理解。
- 既有 Vite chunk size warning。
- 外部 Hermes token 未注入，但本地 Gateway 降级链路可用。

## 6. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 测试结论。
2. P0 / P1 / P2 列表。
3. 必跑命令结果。
4. Hero 区验收结果。
5. 105 项目治理路径验收结果。
6. 另一个真实 NAS 项目治理路径验收结果。
7. Hermes 常驻入口验收结果。
8. Hermes 当前页面 / 当前项目上下文验收结果。
9. Hermes 分层边界验收结果。
10. smoke 项目分类验收结果。
11. 禁止项检查结果。
12. 是否建议主 agent 收口 G2-B。
13. 是否建议整个 G2 收口并进入 Git checkpoint。

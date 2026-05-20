# G2 收口与 Git Checkpoint 前变更审计

生成时间：2026-05-20

## 1. 当前结论

- `G2-B：既有真实项目治理可用性补丁` 已通过测试 agent 短回归。
- Hermes 前端问答超时 P1 已关闭。
- G2-B 当前无 P0 / P1 / 新增 P2。
- 可以进入 G2 整体收口。
- 但当前工作区不是一个干净的小补丁，存在多批次历史改动和未跟踪文件，不能直接全量提交。

## 2. Git 基线

- 当前分支：`codex/nas-real-project-import-pr`
- 跟踪远端：`origin/codex/nas-real-project-import-pr`
- 最近提交：`a1db154 chore: sync current workspace state`
- 远端仓库：`origin git@github.com:wang3286713435-dev/-.git`

判断：

- 当前仓库已经配置远端，且当前分支已有远端跟踪分支。
- 不是“从未推送过”的状态。
- 但当前本地工作区包含大量未提交改动，不能把“已推送过”理解为“当前最新状态已安全保存到远端”。

## 3. 当前改动规模

`git diff --stat` 显示：

- 已跟踪文件变更约 49 个。
- 变更量约 `8111 insertions / 1253 deletions`。
- 另有大量未跟踪文件，覆盖后端模块、前端页面组件、脚本、handoff 和 docs。

这说明当前工作区累积了多个阶段：

- 项目工作台与导航重组。
- 数据管家文件管理与目录树。
- Hermes Gateway 与常驻入口。
- G1 Agent 引导式交付治理。
- 6A / 6B / 7A / 7B / 8A 相关能力与脚本。
- G2-A / G2-B 真实项目接入与治理可用性修复。
- 本机运行态文件。

## 4. 建议提交分组

### A. 应作为 G2 checkpoint 主体的改动

这一组可以作为 G2 收口提交的核心候选：

- `handoff/main-agent/phase2-g2-naming-freeze.md`
- `handoff/main-agent/phase2-insert-g2-real-project-onboarding-masterdata-mapping-plan.md`
- `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
- `handoff/main-agent/hermes-layered-integration-decision.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/status.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/test-agent/latest-report.md`
- `scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
- `scripts/dev/check-hermes-jarvis-gateway.sh`
- Hermes Gateway 相关后端文件：
  - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/**`
  - `backend/delivery-app/src/main/resources/application.yml`
- G2 相关资产总览、真实项目接入、Hermes 常驻入口前端文件：
  - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
  - `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
  - `frontend/src/modules/data-steward/components/DataStewardAnswerCard.vue`
  - `frontend/src/modules/data-steward/api/dataSteward.ts`
  - 相关项目工作台上下文文件。

注意：

- 以上文件里可能混有 G1、5A、5B、6A、7A、8A 的历史改动，需要提交前按 diff 再做一次人工确认。

### B. 应作为“历史二期基线同步”而不是 G2 单独提交的改动

这些文件明显来自 G2 之前的多个二期批次。如果要 checkpoint，建议另开一个较大的“二期阶段基线同步”提交，或者先不拆提交但在提交说明里承认这是阶段性基线。

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/**`
- `backend/delivery-shared/src/main/java/com/zhuoyu/delivery/shared/preview/**`
- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/**`
- `backend/delivery-visualization-adapter/**`
- `backend/delivery-work-center/**`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
- `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
- `scripts/dev/check-phase2-batch6a-project-initialization.sh`
- `scripts/dev/check-phase2-batch6b-delivery-package.sh`
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
- `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh`
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
- `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`

建议：

- 如果目标是保存当前可运行全量状态，可以做一个“二期阶段基线 checkpoint”。
- 如果目标是精确 PR，则需要进一步拆分，成本会比较高。

### C. 需要明确是否纳入仓库的 docs 改动

当前存在 `docs/README.md` 修改和大量 `docs/**` 未跟踪文件：

- `docs/10-phase2-development-roadmap.md`
- `docs/digital-delivery-standards/**`

注意：

- G2 当前开发规则曾多次要求“不修改 `docs/**`”，但这些 docs 改动来自早前阶段路线与标准工作。
- 它们不是 G2-B 本次修复的必要代码改动。
- 如果纳入 checkpoint，应作为“文档基线同步”，不要混入纯 G2 修复提交。

建议：

- 暂不在 G2-B 小提交中纳入 `docs/**`。
- 如用户希望保留阶段路线和标准草案，可以单独做 `docs: sync phase2 roadmap and standards baseline`。

### D. 不建议提交的运行态 / 临时文件

这些文件是本机运行态或临时解包结果，不应进入 Git checkpoint：

- `tmp/run-logs/backend.pid`
- `tmp/run-logs/frontend.pid`
- `tmp/run-logs/hermes-gateway.pid`
- `tmp/jar-inspect/**`

建议：

- 从提交范围排除。
- 后续可考虑加入 `.gitignore`，但本次不主动改忽略规则，避免扩大范围。

### E. 需要谨慎处理的本地 agent 状态

- `.claude/ralph-loop.local.md`
- `.claude/ralph/progress.txt`

判断：

- 这些记录可能有交接价值，但不是平台业务代码。
- 如果作为历史研发记录保留，应单独提交或明确说明。
- 如果只是本地 agent 工作态，不建议混入业务 checkpoint。

## 5. 当前不建议做的事情

- 不建议直接 `git add . && git commit`。
- 不建议把 `tmp/**` 运行态文件提交。
- 不建议把 G2 修复、历史二期能力、docs 标准草案、agent 本地状态混成一个没有说明的大提交。
- 不建议在 G2 收口时顺手进入 8B / 8C / 9A。

## 6. 推荐 checkpoint 路径

### 路径 1：保存当前完整可运行状态

适合目标：先把当前机器上的可运行状态安全保存下来，降低丢失风险。

建议提交：

1. `chore: checkpoint phase2 current working baseline`
   - 纳入前后端业务代码、脚本、handoff。
   - 排除 `tmp/**`。
   - docs 是否纳入需用户确认。

优点：

- 快速保护当前成果。
- 后续迁移或换机器风险低。

缺点：

- 提交很大，难以按单一 PR 审查。

### 路径 2：分三次 checkpoint

适合目标：既保护成果，又让后续审查更清晰。

建议拆为：

1. `feat: sync phase2 delivery platform baseline`
   - 二期通用能力、项目工作台、交付、预览、8A 等。
2. `feat: add g2 real project onboarding and hermes catalog governance`
   - G2-A / G2-B、Hermes 真实接入、常驻入口、真实项目治理路径。
3. `docs: sync phase2 roadmap and standards baseline`
   - `docs/**` 与路线文档。

优点：

- 更清楚。
- 后续查问题更容易。

缺点：

- 当前改动交织较深，拆分需要更多人工确认。

## 7. 主 agent 建议

建议采用路径 2，但先做第 1 步与第 2 步的边界复核。

如果用户希望尽快保护成果，可以退而采用路径 1，但必须明确：

- 这是“阶段 checkpoint”，不是干净的小 PR。
- 不进入生产发布。
- 不代表 8B / 8C / 9A 已开始。

## 8. 当前收口裁决

- G2-B：可收口。
- G2 整体：建议收口。
- 下一步：执行 Git checkpoint 前，请先确认采用“完整 checkpoint”还是“分组 checkpoint”。

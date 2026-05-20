# 二期 G2 批次命名冻结

更新时间：2026-05-19

## 冻结结论

1. 当前批次固定命名为 `G2`。
2. `G2 = 真实项目接入与工程主数据映射修复 MVP`。
3. G2 是 `G1 Agent 引导式交付治理 MVP` 之后的真实项目接入纠偏批次。
4. G2 不代表 9A 已完成，也不代表进入了 9A 后续治理。
5. `8B / 8C / 9A` 均尚未正式进入。
6. 不再新增 `H1` / `R1` 等临时命名，避免进一步混乱。
7. 后续所有 prompt、报告、脚本、状态文档必须统一使用 `G2`。
8. 本轮只修 handoff 与 agent prompt，不修改 `docs/**`。

## 当前 G2 交接文件

- `handoff/main-agent/phase2-insert-g2-real-project-onboarding-masterdata-mapping-plan.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/development-log.md`

## G2 专项脚本命名

本批新增专项脚本固定为：

`scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`

已有历史回归脚本保留原批次命名，例如：

- `scripts/dev/check-hermes-jarvis-gateway.sh`
- `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`

这些历史脚本只作为 G2 回归依赖，不改名、不冒充 G2。

## 禁止混用

当前 G2 交接中不得把任何非 G2 代号描述为启动、进入、开发、测试或收口状态。

非 G2 代号包括：`H1 / R1 / A9 / 9A / 8B / 8C`。

如后续发现上述表述，应改为：

`当前执行 G2；G2 是 G1 后的真实项目接入纠偏批次；8B / 8C / 9A 均尚未正式进入。`

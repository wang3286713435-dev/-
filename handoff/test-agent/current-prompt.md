# 测试 Agent 当前任务：M3G-3 多真实项目分批对象化策略与任务中心增强验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收目标：

`M3G-3：多真实项目分批对象化策略与任务中心增强`

## 0. 本轮只验收什么

本轮只验收多项目对象化的规划、dry-run 和任务中心增强能力。

默认不执行真实多项目对象化。

重点：

- NAS 侧 MinIO readiness。
- 全项目对象化盘点增强。
- 多项目 dry-run 只读计划。
- 文件数 / 容量 / 项目范围限制。
- 任务中心展示增强。
- 不泄露敏感字段。
- 不触碰真实 NAS 原文件。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g3-multi-project-objectification-task-center-plan.md`
- `scripts/dev/check-m3g3-multi-project-objectification-planning.sh`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

除非主 agent 明确允许，不要运行执行型 `check-m3g2-105-objectification-gray.sh`。

## 3. 重点断言

必须确认：

1. readiness 为 `NAS_SIDE_MINIO / READY`。
2. M3G-3 专项脚本通过。
3. 全项目对象化盘点可查。
4. 真实项目和样例 / 测试 / 归档项目不会混成不可区分的一类。
5. 多项目 dry-run 能生成按项目分组的计划。
6. dry-run 不创建迁移任务。
7. 文件数 / 容量上限生效。
8. 响应不泄露真实 NAS 路径、endpoint 原文、bucket、object key、storage URI、SQL、raw row、token、secret。
9. M3G-1 / M3E / M3F / M3C / file-access 回归通过。

如开发 agent 实现了受控执行入口，额外确认：

- 默认不执行。
- 只对明确指定项目执行。
- 不越过上限。
- NAS 原文件未被移动、删除、改名、覆盖。
- 重复执行幂等。

## 4. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、覆盖或改名。
- 迁移范围越过明确项目范围。
- dry-run 实际创建迁移任务。
- 真实路径、bucket、object key、secret、token 泄露。
- file-access 权限链路回归失败。

P1：

- readiness 不是 `NAS_SIDE_MINIO / READY`。
- M3G-3 专项失败。
- 多项目 dry-run 不可用。
- dry-run 结果不能按项目分组。
- 文件数 / 容量限制不生效。
- M3G-1 / M3E / M3F / M3C / file-access 关键回归失败。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。
- 前端页面只做轻量展示但不影响规划链路。

## 5. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2。
- 必跑命令结果。
- 多项目 dry-run 计划结果摘要。
- 是否创建真实迁移任务。
- 是否触碰真实 NAS 原文件。
- 是否出现禁出字段。
- 是否建议主 agent 收口 M3G-3。

# 测试 Agent 当前任务：M3G-4 受控多项目小批对象化执行验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收目标：

`M3G-4：受控多项目小批对象化执行`

## 0. 本轮只验收什么

本轮允许真实小批对象化，但必须非常小、非常受控。

重点：

- NAS 侧 MinIO readiness。
- confirmed=false 必须拒绝。
- 后端项目数 / 文件数 / 容量上限必须生效。
- 只允许真实 NAS 项目。
- 小批执行能成功。
- 重复执行幂等。
- 已对象化文件能通过受控 file-access 读取。
- 不泄露敏感字段。
- 不破坏真实 NAS 原文件。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g4-controlled-multi-project-objectification-plan.md`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

不要运行 M3G-2 执行型脚本。

## 3. 重点断言

必须确认：

1. readiness 为 `NAS_SIDE_MINIO / READY`。
2. M3G-4 专项脚本通过。
3. `confirmed=false` 或缺失时拒绝执行。
4. 超过后端硬上限时拒绝执行。
5. 只执行真实 NAS 项目。
6. 执行规模符合小批上限。
7. 执行后返回任务 / 结果摘要。
8. 已对象化文件 `storage-status=OBJECT_STORED`。
9. 已对象化文件可通过受控 `file-access` 读取。
10. 重复执行返回幂等跳过或不污染 active object version。
11. NAS 原文件未被移动、删除、改名、覆盖。
12. M3G-3 / M3G-1 / M3F / M3E / M3C / file-access 回归通过。
13. 响应不泄露真实 NAS 路径、endpoint 原文、bucket、object key、storage URI、SQL、raw row、token、secret。

## 4. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、覆盖或改名。
- 执行范围越过明确项目范围。
- 未确认也能执行。
- 超限仍能执行。
- 真实路径、bucket、object key、secret、token 泄露。
- file-access 权限链路回归失败。

P1：

- readiness 不是 `NAS_SIDE_MINIO / READY`。
- M3G-4 专项失败。
- 小批执行不可用。
- 重复执行不幂等。
- 已对象化文件无法读取。
- M3G-3 / M3G-1 / M3F / M3E / M3C / file-access 关键回归失败。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。
- 前端页面只做轻量展示但不影响执行链路。

## 5. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2。
- 必跑命令结果。
- M3G-4 执行摘要：项目数、文件数、容量、成功 / 跳过 / 失败。
- 是否出现未确认执行。
- 是否出现超限执行。
- 是否触碰真实 NAS 原文件。
- 是否出现禁出字段。
- 是否建议主 agent 收口 M3G-4。

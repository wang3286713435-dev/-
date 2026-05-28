# 测试 Agent 当前任务：M3G-2 105 项目历史文件对象化上传灰度验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收目标：

`M3G-2：105 项目历史文件对象化上传灰度`

## 0. 本轮只验收什么

本轮只验收 105 / `projectId=503` 的小批量历史文件对象化上传灰度。

重要安全提醒：

- `scripts/dev/check-m3g2-105-objectification-gray.sh` 是执行型脚本，不是只读脚本。
- 每次运行可能继续从 105 的 `NAS_ONLY` 文件中选择一批新文件并复制副本到 NAS 侧 MinIO。
- 如果开发 agent 和主 agent 已经运行过该脚本，测试 agent 默认不要再次运行它，除非用户或主 agent 明确允许“再追加一批对象化”。
- 默认验收方式改为：先基于报告、API、数据库状态、任务详情和 read-only 回归确认本轮结果。

验收重点：

- NAS 侧 MinIO readiness。
- 105 dry-run 计划。
- 小批量对象化迁移任务真实执行。
- 已对象化文件走受控 `file-access`。
- 未对象化文件仍走 NAS_ONLY 链路。
- 真实 NAS 原文件未被移动、删除、改名。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g2-105-objectification-gray-plan.md`
- `scripts/dev/check-m3g2-105-objectification-gray.sh`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

除非明确允许追加对象化，否则不要运行：

```bash
bash scripts/dev/check-m3g2-105-objectification-gray.sh
```

## 3. 重点断言

必须确认：

1. readiness 为 `NAS_SIDE_MINIO / READY`。
2. M3G-2 专项脚本已纳入 Git，且开发 / 主 agent 最近运行记录通过。
3. 105 dry-run 能生成计划。
4. 105 小批量迁移任务已创建、执行、记录结果。
5. 已对象化文件显示 `OBJECT_STORED`。
6. 已对象化文件通过受控 `file-access` 可读取。
7. 未对象化文件仍显示 / 解释为 `NAS_ONLY`，并可继续通过 NAS 链路访问。
8. 重复执行同一批次不会产生重复 active object version 污染。
9. 真实 NAS 原项目文件未被复制以外的方式改动：未移动、未删除、未改名、未覆盖。
10. 响应不泄露真实 NAS 路径、endpoint 原文、bucket、object key、storage URI、SQL、raw row、token、secret。

## 4. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、覆盖或改名。
- 迁移范围越过 105 项目。
- 真实路径、bucket、object key、secret、token 泄露。
- file-access 权限链路回归失败。
- 对象副本失败后平台静默 fallback 到 NAS 并仍声称 `OBJECT_STORED`。

P1：

- readiness 不是 `NAS_SIDE_MINIO / READY`。
- M3G-2 专项失败。
- 105 无法创建小批量对象化任务。
- `OBJECT_STORED` 文件无法通过受控 file-access 读取。
- NAS_ONLY 文件不可用。
- 重复执行污染 active object version。
- M3G-1 / M3E / M3F 关键回归失败。
- M3C / file-access 关键回归失败。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。

## 5. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2。
- 必跑命令结果。
- 105 实际对象化数量、跳过数量、失败数量。
- 105 覆盖率变化。
- 已对象化文件读取路径是否通过受控 `file-access`。
- 未对象化文件 NAS_ONLY 链路是否保留。
- 是否触碰真实 NAS 原文件。
- 是否出现禁出字段。
- 是否建议主 agent 收口 M3G-2。

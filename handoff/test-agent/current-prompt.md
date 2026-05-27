# 测试 Agent 当前任务：M3G-1 P1 修复极短复验

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收目标：

`M3G-1 P1 修复极短复验`

## 0. 本轮只验收

上一轮 M3G-1 正式验收有 2 个 P1：

1. M3E 回归失败：NAS 侧 MinIO 环境下，既有对象化预览产物 file-access 读取失败。
2. M3F 回归脚本失败：脚本仍按本机 MinIO 停止来模拟不可用，不适配 NAS 侧 MinIO。

本轮只看这些 P1 是否关闭，并确认 M3G-1 主能力不回归。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3f-object-storage-first-write.sh`

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

## 3. 重点断言

必须确认：

1. readiness 为 `NAS_SIDE_MINIO / READY`。
2. M3G-1 专项脚本通过。
3. M3E 脚本通过，PDF / 图片对象化预览可通过受控 file-access 读取。
4. M3F 脚本通过，并且在 NAS 侧 MinIO 环境下不再错误暂停本机 MinIO 来判定 fail-closed。
5. 单项目对象化盘点 `projectCode / projectName` 不为 null。
6. dry-run 仍是 `dryRun=true`、`migrationStarted=false`。
7. 未创建真实历史迁移任务。
8. 未复制、移动、删除、改名真实 NAS 原项目文件。
9. 响应不泄露真实 NAS 路径、endpoint 原文、bucket、object key、storage URI、SQL、raw row、token、secret。

## 4. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、覆盖或改名。
- dry-run 实际启动迁移或复制文件。
- 真实路径、bucket、object key、secret、token 泄露。
- file-access 权限链路回归失败。

P1：

- readiness 不是 `NAS_SIDE_MINIO / READY`。
- M3E 回归仍失败。
- M3F 回归仍失败。
- 单项目对象化盘点仍返回空项目名 / 编码。
- M3G-1 专项失败。
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
- M3E 是否恢复。
- M3F 是否适配 NAS 侧 MinIO。
- M3G-1 是否仍通过。
- 单项目盘点字段结果。
- 是否发生真实迁移。
- 是否建议主 agent 收口 M3G-1。

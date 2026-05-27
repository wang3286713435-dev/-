# M3G-1 P1 修复极短复验报告

生成时间：2026-05-27

## 1. 测试结论

结论：M3G-1 P1 修复复验通过，上一轮 2 个 P1 均已关闭，建议主 agent 进入 M3G-1 收口判断。

本轮只复验上一轮阻塞项：

- M3E 回归失败：NAS 侧 MinIO 环境下，既有对象化预览产物 file-access 读取失败。
- M3F 回归脚本失败：脚本仍按本机 MinIO 停止来模拟不可用，不适配 NAS 侧 MinIO。

复验结果：

- M3E 已恢复，`check-m3e-preview-artifacts-object-storage.sh` 通过，PDF / 图片对象化预览可通过受控 file-access 读取。
- M3F 已适配 NAS 侧 MinIO，`check-m3f-object-storage-first-write.sh` 通过，并明确在 `NAS_SIDE_MINIO / READY` 环境下不再暂停本机 MinIO 容器。
- M3G-1 专项仍通过，readiness 为 `NAS_SIDE_MINIO / READY`，单项目 `projectCode/projectName` 已补齐，dry-run 未启动真实迁移。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮复验。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项未纳入本轮判断。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3f-object-storage-first-write.sh`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m3f-object-storage-first-write.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 5. M3E 是否恢复

已恢复。

`check-m3e-preview-artifacts-object-storage.sh` 覆盖并通过：

- 管理员登录并切换项目 503。
- 选择对象化原生预览样本 `fileId=993`。
- `preview-artifacts:prepare` 返回 PDF / 图片原生预览产物对象化状态。
- `GET preview-artifacts` 可返回已准备的原生预览产物。
- 原生预览通过受控 file-access 入口打开。
- DWG / RVT / Office 等仍只生成转换占位，未伪造预览产物。
- 交付包导出预检查字段未回归。

上一轮 `ASSET_FILE_NOT_READABLE` 已未复现。

## 6. M3F 是否适配 NAS 侧 MinIO

已适配。

`check-m3f-object-storage-first-write.sh` 覆盖并通过：

- 对象存储 readiness 已识别为 `NAS_SIDE_MINIO / READY`。
- 新上传文件走对象存储优先，返回 `OBJECT_STORED`。
- 新上传文件生成稳定 `assetUuid`。
- 新增文件本体未写入隔离 NAS 目录。
- storage-status 返回 `OBJECT_STORED`。
- 数据库存在且仅存在一个 active object version。
- 受控 file-access 可读取对象存储新增文件内容。
- 文件管理目录列表可见新文件且显示对象存储业务状态。
- 在 NAS 侧 MinIO 环境下，脚本不再暂停本机容器，而是通过 readiness 和对象优先上传链路完成验证。

上一轮“停止本机 MinIO 后误判对象存储不可用”的问题已关闭。

## 7. M3G-1 是否仍通过

仍通过。

`check-m3g-nas-minio-readiness-inventory.sh` 结果为 `PASS=9 FAIL=0`。

关键点：

- readiness 返回 provider / endpointType / readinessStatus 且不泄露底层配置。
- endpointType / readinessStatus 为 `NAS_SIDE_MINIO / READY`。
- 全项目对象化盘点可查，且不泄露底层路径。
- 对象化盘点包含 105 / 503 或真实项目聚合。
- 单项目对象化盘点返回 `projectCode/projectName`。
- dry-run 返回计划但未创建迁移任务、未启动真实迁移。
- dry-run 返回风险说明和计划边界。
- M3G-1 专项脚本已纳入 Git 跟踪。

## 8. 单项目盘点字段结果

直接 API 抽查：

```json
{
  "projectId": 503,
  "projectCode": "105",
  "projectName": "启航华居项目",
  "totalFiles": 2928,
  "objectStoredFiles": 3,
  "nasOnlyFiles": 2925
}
```

`projects[0]` 中也返回：

```json
{
  "projectId": 503,
  "projectCode": "105",
  "projectName": "启航华居项目"
}
```

上一轮单项目 `projectCode/projectName=null` 已修复。

## 9. readiness 与 dry-run 抽查

直接 API 抽查：

```json
{
  "endpointType": "NAS_SIDE_MINIO",
  "readinessStatus": "READY",
  "configured": true,
  "reachable": true,
  "readable": true,
  "writable": true
}
```

dry-run 抽查：

```json
{
  "dryRun": true,
  "migrationStarted": false,
  "projectId": 503,
  "selectedFileCount": 24,
  "objectStoredSkipCount": 1,
  "missingChecksumCount": 24,
  "estimatedBatches": 3
}
```

迁移任务数量：

- dry-run 前：14
- dry-run 后：14

结论：dry-run 未创建真实历史迁移任务。

## 10. 是否发生真实迁移

否。

本轮未执行历史文件真实批量迁移，未复制、移动、删除、覆盖或改名真实 NAS 原项目文件。

说明：

- M3E 预览修复链路可能按开发报告描述对当前 provider 下不可读的原生预览对象做受控对象修复，但这是按显式预览准备触发的对象体补齐，不是历史文件批量迁移。
- M3G-1 dry-run 前后迁移任务数量一致。

## 11. 安全与禁出字段

通过。

本轮脚本均包含 forbidden-field scan，未发现：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- bucket 真值
- `object_key`
- `objectKey`
- raw row
- SQL 语句
- token / secret / password / access key

file-access 权限链路回归通过，`check-phase2-batch4-file-access.sh` 为 `PASS=18 FAIL=0`。

## 12. 是否建议主 agent 收口 M3G-1

建议主 agent 进入 M3G-1 收口判断。

上一轮 2 个 P1 均已关闭，M3G-1 专项、M3E、M3F、M3C、file-access 和 `git diff --check` 均通过；当前仅剩 P2 记录项，不阻塞收口。

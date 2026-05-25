# 开发 Agent 当前任务：M3B 105 小样本对象存储镜像迁移

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3b-object-storage-mirror-trial`

本轮批次：

`M3B：105 小样本对象存储镜像迁移`

本轮只做少量文件的对象存储镜像迁移闭环，不做全量 NAS 搬迁、不做语义解析、不做 Hermes 正文问答。

## 0. 必须先阅读

- `handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`
- `handoff/main-agent/m3a-storage-service-foundation-closure.md`
- `handoff/main-agent/status.md`
- `handoff/dev-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `scripts/dev/check-m3a-storage-service-foundation.sh`

## 1. 本轮目标

完成一个可控的小样本镜像迁移闭环：

`105 项目少量 NAS 文件 -> 上传 MinIO / S3-compatible -> 校验对象 -> 写对象版本记录 -> storage status 显示 OBJECT_STORED -> 仍通过受控 file-access 访问`

M3B 不追求大范围迁移，只证明这条链路可以安全工作。

## 2. 必做能力

### A. 迁移任务 API

新增或补齐：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`

创建任务必须支持明确传入 `fileIds`。本轮优先只支持显式文件 ID 小样本，不要做项目全量、目录全量或类型全量。

### B. 迁移任务限制

必须设置安全限制：

- 单次任务文件数量上限，建议默认不超过 10。
- 单文件大小上限，避免超大文件试运行压垮本机。
- 只允许当前用户有项目权限的项目。
- 只允许文件属于当前项目。
- 已删除、回收站、路径不可读文件不得迁移。

超过限制必须返回业务化错误，不得静默全量迁移。

### C. 单文件迁移流程

每个文件：

1. 校验权限和项目归属。
2. 校验文件仍可读取。
3. 计算或复用 checksum。
4. 生成稳定对象 key，建议包含 projectId / fileId / checksum 或版本信息，但不得返回前端。
5. 上传到 MinIO。
6. 校验对象 size / etag / checksum。
7. 写入或复用 `data_storage_objects`。
8. 写入 `data_file_object_versions`。
9. 更新或映射当前文件 storage status 为 `OBJECT_STORED`。
10. 写审计事件和任务进度。

### D. 幂等

同一文件重复迁移：

- 不应重复生成多条不可解释的对象记录。
- 不应重复污染 file object versions。
- 可以返回 skipped / reused / alreadyStored 之类状态。
- 必须在任务结果里说明。

### E. 受控访问

迁移后：

- `GET /api/data-steward/assets/files/{fileId}/storage-status` 能显示对象已存储。
- 该文件仍通过现有 `PREVIEW` / `DOWNLOAD` access ticket 访问。
- 前端/API 不拿 bucket、object key、storage URI。

## 3. 禁止事项

严禁：

1. 不做全量 NAS 迁移。
2. 不支持按项目一键全量迁移。
3. 不移动、删除、重命名真实 NAS 文件。
4. 不读取 PDF / Office / DWG / RVT / IFC 正文。
5. 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
6. 不新增 Hermes 正文问答。
7. 不启动真实 BIM 轻量化。
8. 不把对象存储迁移说成语义理解。
9. 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、SQL、raw row、token、secret。
10. 不修改 `docs/**`，除非主 agent 单独授权。

## 4. 建议新增脚本

新增：

`scripts/dev/check-m3b-object-storage-mirror-trial.sh`

脚本必须使用 105 / 503 中少量安全文件，或者创建隔离临时小文件资源。优先避免触碰真实业务 NAS 文件内容；如读取真实文件，只读，不写、不移动、不删除。

脚本至少验证：

- 创建小样本迁移任务。
- 文件数量上限生效。
- 迁移任务状态可查。
- 成功迁移后 storage status 为 `OBJECT_STORED`。
- 重跑迁移不重复污染对象记录。
- 迁移失败有失败原因。
- 迁移后 file-access 仍可受控读取。
- API 响应禁出字段扫描通过。

## 5. 必跑验证

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 6. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改动文件清单。
- 迁移任务 API 说明。
- 迁移任务限制说明。
- 单文件迁移流程。
- 幂等策略。
- storage status 变化。
- file-access 回归结果。
- 禁出字段扫描结果。
- 必跑命令结果。
- 是否触碰真实 NAS 文件。
- 是否修改 `docs/**`。
- 已知风险和 M3C 建议。

## 7. 完成定义

同时满足以下条件才算完成：

- 小样本迁移任务可创建。
- 迁移后对象记录和文件版本记录正确。
- storage status 可显示 `OBJECT_STORED`。
- 重跑不重复污染记录。
- 失败有原因。
- NAS 原文件不被移动、删除、重命名。
- 受控 file-access 不回归。
- M3B 专项脚本和核心回归通过。
- `handoff/dev-agent/latest-report.md` 已写。

# M3B：105 小样本对象存储镜像迁移收口

时间：2026-05-26

## 1. 收口结论

`M3B：105 小样本对象存储镜像迁移` 正式收口。

测试 agent 已完成验收：

- 当前 P0：无。
- 当前 P1：无。
- M3B 专项脚本通过，`PASS=11 FAIL=0`。
- M3A / M2J / M2I / M2H / M2F / file-access 回归全部通过。

## 2. 已完成能力

本批完成了小样本对象存储镜像闭环：

- 新增 `V29__m3b_object_storage_mirror_trial.sql`。
- 新增迁移任务批次表 `data_object_migration_task_batches`。
- 复用 `data_object_migration_tasks` 记录单文件迁移结果。
- 新增小样本迁移 API：
  - `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
  - `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
  - `GET /api/data-steward/storage-migration-tasks/{taskId}`
  - `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`
- 支持显式 `fileIds` 小样本迁移。
- 成功迁移后 `storage-status` 显示 `OBJECT_STORED / MINIO`。
- 迁移后受控 `file-access` 能读取 MinIO 镜像。
- 重复迁移返回 `ALREADY_STORED`，不重复污染对象版本记录。
- 失败任务有业务化失败原因，并可重试。

## 3. 安全边界

本批坚持：

- 只支持显式 `fileIds`，不支持项目 / 目录 / 类型全量迁移。
- 单次最多 10 个文件。
- 单文件默认 10MB 限制。
- 跨项目文件拒绝。
- 已删除、回收站、路径不可读文件拒绝。
- NAS 原文件不移动、不删除、不重命名。
- 前端和 API 响应不返回真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret。

本批未做：

- 全量 NAS 搬迁。
- 真实业务 NAS 目录写操作。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答。
- 真实 BIM 引擎接入。
- 语义解析或向量索引。

## 4. 验收依据

测试 agent 已确认：

- 小样本迁移任务可创建。
- 单次 10 个文件上限生效。
- 跨项目文件不会迁移。
- 删除后的文件资源不会迁移。
- 路径不可读文件迁移失败并保留业务化失败原因。
- `data_storage_objects` 与 `data_file_object_versions` 有可解释记录。
- 重跑同一文件不会重复污染对象版本记录。
- 删除临时 NAS 源文件后，受控 `file-access` 仍可读取 MinIO 镜像。
- 禁出字段扫描通过。
- 未发现全量 NAS 迁移、真实 NAS 改动、正文解析、Hermes 正文问答或 BIM 引擎越界。

必跑结果：

- 后端构建通过。
- 前端构建通过，仅既有 Vite chunk warning。
- 健康检查通过。
- `scripts/dev/check-m3b-object-storage-mirror-trial.sh` 通过。
- `scripts/dev/check-m3a-storage-service-foundation.sh` 通过。
- `scripts/dev/check-m2j-105-ownership-review.sh` 通过。
- `scripts/dev/check-m2i-105-file-ownership-governance.sh` 通过。
- `scripts/dev/check-m2h-windows-file-manager.sh` 通过。
- `scripts/dev/check-m2f-real-project-delivery-loop.sh` 通过。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过。
- `git diff --check` 通过。

## 5. 剩余提醒

P2：

- 前端构建仍有既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为非交付未跟踪项，提交时继续排除。

## 6. 下一步建议

下一批建议进入：

`M3C：对象存储迁移任务中心与批量策略`

M3C 才考虑：

- 异步 worker。
- 更完整的任务中心。
- 批量策略。
- 限速。
- 失败重试队列。
- 更完整的迁移进度 UI。
- 更大范围项目 / 目录 / 文件类型筛选。

M3C 未经用户确认不得自动启动。

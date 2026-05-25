# 开发 Agent 报告：M3B 105 小样本对象存储镜像迁移

时间：2026-05-25 CST

## 1. 本轮目标

本轮按 `M3B：105 小样本对象存储镜像迁移` 执行。

目标是验证一个受控的小样本闭环：105 / projectId=503 下明确选择少量文件，保留 NAS 原件，只读读取源文件，镜像上传到 MinIO，校验对象，写入对象与文件版本记录，让 `storage-status` 显示 `OBJECT_STORED`，并继续通过现有受控 file-access 访问。

本轮不做全量 NAS 搬迁，不做目录 / 项目一键迁移，不做语义解析，不做 Hermes 正文问答，不做 BIM/parser/indexing。

## 2. 改动文件清单

- `backend/delivery-app/src/main/resources/db/migration/V29__m3b_object_storage_mirror_trial.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `scripts/dev/check-m3b-object-storage-mirror-trial.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未修改前端业务代码。

## 3. 数据库迁移

新增 `V29__m3b_object_storage_mirror_trial.sql`：

- 新增 `data_object_migration_task_batches`，用于表示一次 API 创建的小样本迁移批次。
- 给 `data_object_migration_tasks` 增加 `task_batch_id`，继续复用 M3A 的单文件迁移任务表记录每个文件结果。

这样 `POST fileIds` 可以有稳定的任务 ID，同时每个文件仍可单独记录成功、失败、跳过和校验状态。

## 4. 迁移任务 API

新增：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`

响应只返回任务状态、计数、文件 ID、文件名、文件类型、大小、迁移状态、结果码和业务化消息，不返回真实 NAS 路径、底层对象定位信息、SQL、raw row 或密钥。

## 5. 迁移任务限制

- 只支持显式 `fileIds`。
- 单次最多 10 个文件。
- 不支持项目全量、目录全量、类型全量迁移。
- 当前用户必须有项目权限。
- 文件必须属于当前项目。
- 已删除 / 回收站文件拒绝迁移。
- 单文件默认限制 10MB，避免本机试运行压垮。
- 超过文件数量上限会业务化拒绝，不会静默执行。

## 6. 单文件迁移流程

每个文件按固定流程处理：

1. 校验用户项目权限。
2. 校验文件项目归属和生命周期。
3. 检查当前 `storage-status`，已是对象存储则跳过。
4. 只读打开 NAS 源文件。
5. 计算 SHA-256 checksum。
6. 生成稳定对象 key，内部包含项目 / 文件 / checksum 信息，但不返回前端。
7. 上传到 MinIO 默认桶。
8. stat 校验对象大小。
9. 写入或复用 `data_storage_objects`。
10. 写入 `data_file_object_versions` 并置为 active。
11. 更新文件 checksum / verified 时间。
12. 写单文件和任务审计。

## 7. 幂等策略

- 同一文件已有 active 对象版本时，重复迁移直接返回 `ALREADY_STORED` / `SKIPPED`。
- 重复迁移不会再次新增 `data_file_object_versions` active 记录。
- 对象 key 基于 projectId / fileId / checksum / 文件名生成，同名不同文件不会误合并。
- `data_storage_objects` 使用 provider + bucket + object key 唯一约束复用对象记录。

## 8. Storage Status 与 File-Access

- `StorageService` 已调整为优先使用 active `data_file_object_versions` 对应的对象存储引用。
- 迁移后 `GET /api/data-steward/assets/files/{fileId}/storage-status` 返回 `OBJECT_STORED` / `MINIO`。
- 迁移后原有 `PREVIEW` / `DOWNLOAD` access ticket 继续可用。
- M3B 专项脚本删除了临时 NAS 源文件后，仍能通过受控 preview ticket 读取 MinIO 镜像，证明访问链路已走对象镜像。

## 9. 禁出字段扫描

M3B 专项脚本对迁移创建、列表、详情、失败、重试、storage-status 响应执行 forbidden-field scan。

未发现以下字段或真值泄露：

- `/Volumes`
- `/tmp`
- `nas://`
- `smb://`
- `storage_path`
- `storage_uri`
- bucket / object key
- raw DB row
- SQL
- secret / password

## 10. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3B 对象存储镜像迁移专项                                     PASS=11 FAIL=0
M3A StorageService 回归                                      PASS=8 FAIL=0
M2J 105 归属复核回归                                         PASS=6 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
M2F 真实项目交付闭环回归                                     PASS=6 FAIL=0
Phase2 batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

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

## 11. 是否触碰真实 NAS 文件

否。

M3B 专项脚本创建的是 `/tmp` 下隔离临时小文件资源，并在迁移后删除该临时源文件来验证对象镜像访问。没有移动、删除、重命名、上传或改写真实业务 NAS 目录中的文件。

## 12. 已知风险与 M3C 建议

- M3B 当前是同步小样本任务，适合验证链路，不适合大批量迁移。
- 当前只默认验证 MinIO；S3-compatible 配置启用后需要单独验。
- M3C 建议再做任务中心、异步 worker、批量策略、限速、失败重试队列和更完整的操作界面。
- 大范围迁移前仍需明确灰度开关、容量阈值、对象生命周期策略和回滚策略。

## 13. 服务状态

- 后端已运行在 `http://127.0.0.1:8080`
- 前端 dev server 保持运行，地址为 `http://127.0.0.1:5173`
- 按用户要求，本轮完成后未关闭项目服务。

# 测试 Agent 报告：M3B 105 小样本对象存储镜像迁移验收

时间：2026-05-26 CST

## 1. 总结论

结论：通过。

本轮按 `handoff/test-agent/current-prompt.md` 验收 `M3B：105 小样本对象存储镜像迁移`。验收范围仅限 105 项目小样本对象存储镜像迁移，不验收全量 NAS 搬迁、语义解析、Hermes 正文问答或真实 BIM 引擎。

已确认：

- 小样本迁移任务可创建。
- 创建任务必须显式传入少量 `fileIds`。
- 单次 10 个文件上限生效，不能一键全量迁移。
- 跨项目文件不会被迁移，行级失败为 `STORAGE_MIGRATION_FILE_PROJECT_MISMATCH`。
- 删除后的文件资源不会迁移，行级失败为 `ASSET_FILE_NOT_FOUND`。
- 路径不可读文件迁移失败并保留业务化失败原因。
- 迁移成功后 `storage-status` 显示 `OBJECT_STORED` / `MINIO`。
- `data_storage_objects` 与 `data_file_object_versions` 写入可解释记录。
- 重跑同一文件返回 `ALREADY_STORED`，未重复污染对象版本记录。
- 删除临时 NAS 源文件后，受控 `file-access` 仍可读取 MinIO 镜像。
- NAS 原文件未被移动、删除、重命名；本轮只操作 `/tmp` 隔离临时文件。
- 禁出字段扫描通过。
- 未发现越界进入 Hermes / parser / indexing / BIM / 全量迁移。

当前无 P0 / P1。建议主 agent 进入 M3B 收口判断。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为非交付未跟踪项，本轮不判失败，收口提交时继续排除。

## 3. 必跑命令结果

```text
cd backend && ./mvnw -pl delivery-app -am -DskipTests package
结果：PASS，BUILD SUCCESS

corepack pnpm --dir frontend build
结果：PASS，仅既有 Vite chunk size warning

curl -fsS http://127.0.0.1:8080/actuator/health
结果：PASS，{"status":"UP"}

bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
结果：PASS=11 FAIL=0

bash scripts/dev/check-m3a-storage-service-foundation.sh
结果：PASS=8 FAIL=0

bash scripts/dev/check-m2j-105-ownership-review.sh
结果：PASS=6 FAIL=0

bash scripts/dev/check-m2i-105-file-ownership-governance.sh
结果：PASS=8 FAIL=0

bash scripts/dev/check-m2h-windows-file-manager.sh
结果：PASS=53 FAIL=0

bash scripts/dev/check-m2f-real-project-delivery-loop.sh
结果：PASS=6 FAIL=0

bash scripts/dev/check-phase2-batch4-file-access.sh
结果：PASS=18 FAIL=0，phase2 batch4 file access check ok

git diff --check
结果：PASS
```

## 4. M3B 专项脚本结果

`check-m3b-object-storage-mirror-trial.sh` 通过，关键覆盖如下：

- 管理员登录并切换到 `105 / projectId=503`。
- 在 `/tmp` 创建隔离测试小文件资源，不触碰真实业务 NAS 目录。
- 单次超过 10 个文件被业务化拒绝。
- 小样本迁移任务创建并成功镜像到对象存储。
- 迁移任务列表和详情可查，且不暴露底层存储定位。
- 迁移后 `storage-status` 显示 `OBJECT_STORED`。
- 对象版本记录已写入且数量正确。
- 删除临时 NAS 源文件后，受控 `file-access` 仍可读取对象镜像。
- 重复迁移识别为已镜像，未重复污染版本记录。
- 迁移失败有业务化失败原因。
- 失败任务可重试并保留失败状态。

## 5. 小样本迁移任务验证结果

专项脚本创建 1 个 `/tmp` 隔离小样本文件资源，并调用：

```text
POST /api/data-steward/projects/503/storage-migration-tasks
```

结果：

- `taskStatus=COMPLETED`
- `successCount=1`
- `failureCount=0`
- 行级 `resultCode=MIRRORED`
- 行级 `objectStored=true`

补充负向抽查：

- 未传 `fileIds`：拒绝，`code=CORE_REQUEST_INVALID`。
- 空 `fileIds=[]`：拒绝，`code=STORAGE_MIGRATION_FILE_IDS_REQUIRED`。
- 跨项目 fileId：任务 fail-closed，行级 `resultCode=STORAGE_MIGRATION_FILE_PROJECT_MISMATCH`，未迁移。
- 删除后的临时文件资源：任务 fail-closed，行级 `resultCode=ASSET_FILE_NOT_FOUND`，未迁移。

补充抽查产生的临时失败任务和临时文件资源已清理。

## 6. Storage Status 结果

迁移成功后：

```text
GET /api/data-steward/assets/files/{fileId}/storage-status
```

结果：

- `storageState=OBJECT_STORED`
- `activeProvider=MINIO`
- `objectStored=true`
- 响应未返回真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret、password。

## 7. 幂等验证结果

对同一个已镜像文件再次创建迁移任务：

- 返回 `taskStatus=COMPLETED`。
- `skippedCount=1`。
- 行级 `resultCode=ALREADY_STORED`。
- `data_file_object_versions` 中该文件的有效对象版本记录仍为 1 条。

判定：重跑同一文件未重复污染对象版本记录。

## 8. File-Access 回归结果

M3B 专项脚本验证：

- 临时 NAS 源文件删除后，仍可通过现有 `PREVIEW` access ticket 读取 MinIO 镜像内容。
- 说明迁移后读取链路已走对象镜像，且仍复用平台受控 file-access。

`check-phase2-batch4-file-access.sh` 回归通过：

- 管理员可预览 / 下载。
- 查看者可预览但不能下载。
- 交付工程师可预览 / 下载。
- 跨项目与缺失文件被拒绝。
- catalog 列表和详情不暴露 NAS 路径。
- 预览、下载、拒绝、失败动作均有审计。

判定：file-access 权限、生命周期、审计链路未回归。

## 9. NAS 原文件未被改动确认

本轮未对真实业务 NAS 文件执行：

- 移动
- 删除
- 重命名
- 上传
- 正文读取

M3B 专项脚本只创建 `/tmp/delivery-m3b.*` 下的隔离临时文件资源。脚本为了证明对象镜像读取能力，会删除该临时源文件，再通过受控 file-access 读取 MinIO 镜像；这不涉及真实业务 NAS 目录。

## 10. 禁出字段扫描结果

已对以下响应进行禁出字段扫描：

- M3B 迁移任务创建响应。
- M3B 迁移任务列表响应。
- M3B 迁移任务详情响应。
- M3B 迁移失败响应。
- M3B retry 响应。
- 迁移后 storage-status 响应。
- file-access 回归相关响应。

未发现以下字段直接返回到 API 响应：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- `storage_path`
- `storagePath`
- `bucket`
- `object_key`
- `objectKey`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`
- `access_key`
- `secret_key`

备注：服务端内部数据库字段和脚本内部清理逻辑会使用 bucket / object key / SQL 等内部定位信息；本轮未发现这些信息直接泄露到 API 响应。

## 11. 越界检查

已执行：

```text
git diff --name-only
git status --short
```

未发现：

- `docs/**` 修改。
- 旧 Flyway 迁移被修改。
- 全量 NAS 迁移入口。
- 真实 NAS 文件移动、删除、重命名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答能力。
- 真实 BIM 引擎接入。

当前 M3B 核心交付文件已纳入暂存 / 跟踪范围：

- `backend/delivery-app/src/main/resources/db/migration/V29__m3b_object_storage_mirror_trial.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `scripts/dev/check-m3b-object-storage-mirror-trial.sh`
- `handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`

非交付未跟踪项：

- `.claude/**`
- `CLAUDE.md`
- `tmp/**`

## 12. 最终建议

M3B 验收通过，当前无 P0 / P1。建议主 agent 进入 M3B 收口判断。

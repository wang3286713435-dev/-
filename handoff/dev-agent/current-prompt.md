# 开发 Agent 当前任务：M3C 对象存储迁移任务中心与批量策略

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前建议分支：

`codex/m3c-storage-migration-task-center`

本轮批次：

`M3C：对象存储迁移任务中心与批量策略`

本轮只把 M3A / M3B / M3C-1 的对象存储能力补成受控任务中心，不做全量 NAS 搬迁、不做 Hermes 正文问答、不做 parser / indexing / BIM 轻量化。

## 0. 必须先阅读

- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c-0-storage-evidence-contract-freeze-closure.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`
- `handoff/main-agent/m3c-object-storage-migration-task-center-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-app/src/main/resources/db/migration/V28__m3a_storage_objects_foundation.sql`
- `backend/delivery-app/src/main/resources/db/migration/V29__m3b_object_storage_mirror_trial.sql`
- `backend/delivery-app/src/main/resources/db/migration/V30__m3c1_asset_uuid_storage_status.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/router/index.ts`

## 1. 当前基础

M3A 已完成：

- 对象存储元数据表。
- StorageService。
- provider health。
- file storage status。
- file-access 内部走 StorageService。

M3B 已完成：

- 显式 fileIds 小样本迁移。
- MinIO 镜像上传。
- 对象校验。
- 对象版本记录。
- 幂等和重试。

M3C-1 已完成：

- `assetUuid`。
- `FileAssetView` / `ModelAssetView` 输出 `asset_uuid`。
- storage status 单文件状态统一。
- M3C-1 专项脚本通过。

## 2. 本轮目标

把对象存储迁移从“接口级小样本 smoke”补成平台可用的任务中心：

- 管理员可创建受控迁移任务。
- 管理员可查看任务列表。
- 管理员可查看任务详情和行级结果。
- 管理员可重试失败或可重试任务。
- 页面和接口能解释成功、失败、跳过、已对象化、不可迁移原因。
- 任务行必须带 `assetUuid`。
- storage-status 与任务结果一致。
- 响应和页面不暴露底层路径或对象定位。

## 3. 严格边界

严禁：

1. 不做全量 NAS 搬迁。
2. 不做目录一键全量迁移。
3. 不自动扫描并迁移整个项目。
4. 不移动、删除、重命名真实 NAS 文件。
5. 不读取 PDF / Office / DWG / RVT / IFC 正文。
6. 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
7. 不新增 Hermes 正文问答。
8. 不启动 BIM 轻量化或 parser。
9. 不把对象存储迁移说成语义理解完成。
10. 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、SQL、raw row、token、secret。
11. 不修改仓库 `docs/**`。
12. 不把 `.claude/**`、`CLAUDE.md`、`tmp/**` 纳入提交。
13. 不创建子 agent。

## 4. 后端要求

优先复用现有 M3B 服务和表：

- `StorageMigrationApplicationService`
- `StorageMigrationController`
- `data_object_migration_task_batches`
- `data_object_migration_tasks`
- `data_storage_objects`
- `data_file_object_versions`
- `StorageService`

如现有表能满足任务中心，不强行新增迁移。若确实需要新增字段，只能追加新 Flyway 迁移，不修改旧迁移。

任务策略要求：

- 任务只能通过显式 `fileIds` 创建。
- 文件必须属于当前项目。
- 当前用户必须有项目权限。
- 单次数量上限保守，默认沿用 M3B 的 10 个，可配置但不得无限制。
- 单文件大小上限保守，可配置。
- 已删除、回收站、路径不可读文件必须拒绝或跳过，并返回业务原因。
- 已有 active 对象版本的文件必须返回 `ALREADY_STORED` / `SKIPPED`，不得重复污染 active 版本。
- 任务详情和行级结果必须包含 `assetUuid`。
- 创建、重试、失败、跳过都要有审计。

接口可以复用或增强：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`

必要时可新增只读汇总接口，例如：

- `GET /api/data-steward/projects/{projectId}/storage-migration-summary`

但不要新增危险全量执行接口。

## 5. 前端要求

新增或增强对象存储迁移任务中心页面，建议入口：

`/data-steward/storage-migration`

也可以作为“文件服务 / 存储服务”页面中的 tab，但必须有清晰入口。

页面至少包含：

- 任务列表。
- 任务详情抽屉。
- 行级结果表。
- 创建迁移任务入口。
- 项目内文件选择入口。
- 重试失败任务按钮。
- storage status 汇总。

任务行展示：

- 平台资产ID `assetUuid`。
- 文件名。
- 文件类型。
- 大小。
- 迁移状态。
- 存储状态。
- 结果码。
- 业务化失败/跳过原因。
- 开始/完成/校验时间。

文案必须明确：

- NAS 原文件保留。
- 当前只做对象存储镜像。
- 不生成语义证据。
- 不代表文件内容已被 Hermes 理解。
- 不暴露底层路径、bucket、object key。

前端不要把本地假结果伪装成真实迁移结果。

## 6. 专项脚本

新增：

`scripts/dev/check-m3c-storage-migration-task-center.sh`

脚本至少覆盖：

1. 管理员登录和项目切换。
2. 创建隔离临时文件资源。
3. 创建迁移任务。
4. 查询任务列表。
5. 查询任务详情。
6. 校验行级 `assetUuid`、`storageState`、`resultCode`。
7. 重复迁移幂等，不重复污染 active 对象版本。
8. 失败文件任务和重试。
9. forbidden-field scan。
10. 本轮新增关键文件已纳入 Git 跟踪，尤其是 Flyway migration 和专项脚本不能是 `??`。

脚本不得触碰真实业务 NAS 文件，只能使用 `/tmp` 下隔离临时文件或测试资源。

## 7. 必跑验证

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 本轮目标。
- 改动文件清单。
- 是否新增数据库迁移。
- 新增/调整接口清单。
- 前端新增页面/入口。
- 任务创建、列表、详情、重试如何实现。
- 批量策略、上限、幂等策略。
- `assetUuid` 在任务中心中的展示与响应情况。
- 禁出字段扫描结果。
- 必跑命令结果。
- 是否触碰真实 NAS 文件。
- 是否修改 `docs/**`。
- 已知风险和下一步建议。

## 9. 完成定义

同时满足以下条件才算完成：

- 任务中心可创建、查看列表、查看详情、重试。
- 任务行包含 `assetUuid`。
- 成功 / 失败 / 跳过 / 已对象化原因可解释。
- 幂等验证通过。
- storage-status 与任务结果一致。
- 响应不暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
- 不触碰真实业务 NAS 文件。
- 后端构建、前端构建、M3C 专项、M3C-1 / M3B / M3A / M2H / file-access 回归通过。
- 本轮新增关键文件已纳入 Git 跟踪。
- `handoff/dev-agent/latest-report.md` 已写。

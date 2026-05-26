# 开发 Agent 报告：M3C 对象存储迁移任务中心与批量策略

时间：2026-05-26 CST

## 1. 本轮目标

本轮按 `M3C：对象存储迁移任务中心与批量策略` 执行。

目标是在 M3A StorageService、M3B 显式小样本对象镜像、M3C-1 `assetUuid` / 单文件存储状态统一的基础上，补齐平台可用的对象存储迁移任务中心：管理员可以显式选择项目文件创建迁移任务，查看任务列表、任务详情、行级结果，并重试失败任务。

本轮没有做全量 NAS 搬迁、目录一键迁移、parser/indexing、Hermes 正文问答或 BIM 轻量化。

## 2. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `frontend/src/router/index.ts`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`
- `handoff/dev-agent/latest-report.md`

当前工作区仍包含 M3C-1 已跟踪文件：

- `backend/delivery-app/src/main/resources/db/migration/V30__m3c1_asset_uuid_storage_status.sql`
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh`

未修改仓库 `docs/**`。

## 3. 数据库迁移

本轮 M3C 未新增 Flyway 迁移。

实现复用既有表：

- `data_object_migration_task_batches`
- `data_object_migration_tasks`
- `data_storage_objects`
- `data_file_object_versions`
- `data_file_resources.asset_uuid`（M3C-1）

## 4. 接口清单

复用 / 增强：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`

新增只读汇总：

- `GET /api/data-steward/projects/{projectId}/storage-migration-summary`

汇总接口返回项目文件总数、已对象化数量、仍在 NAS 数量、运行中任务数、失败任务数、最新任务、单次文件上限、单文件大小上限和策略说明。

## 5. 前端入口

新增项目内对象存储任务中心入口：

- 路由：`/data-steward/assets/:projectId/data-steward/storage-migration`
- 项目工作台导航：`项目资产 / 更多入口 / 对象存储`
- 项目详情页更多工具卡片：`对象存储`

`DataStewardFileServicePage.vue` 增强为双 tab：

- `对象存储迁移`
- `文件访问安全`

对象存储迁移页包含 storage status 汇总、显式文件选择、创建任务、任务列表、任务详情抽屉、失败任务重试。

## 6. 任务策略

- 只能通过显式 `fileIds` 创建任务。
- 当前用户必须拥有项目权限。
- 文件必须属于当前项目。
- 单次最多 10 个文件。
- 单文件上限沿用 M3B 的 10MB。
- 已删除 / 回收站 / 不可读源文件返回业务化失败原因。
- 已有 active 对象版本的文件返回 `ALREADY_STORED` / `SKIPPED`，不会重复污染 active 对象版本。
- 创建、完成、失败、跳过、重试均保留审计。

## 7. assetUuid 与安全字段

任务详情和行级结果已返回：

- `assetUuid`
- `fileId`
- 文件名
- 文件类型
- 文件大小
- `migrationStatus`
- `storageState`
- `resultCode`
- 业务化说明
- 开始 / 完成 / 校验时间

页面展示“平台资产ID `assetUuid`”，底层 bucket、object key、`storage_uri`、真实 NAS 路径不返回、不展示。

## 8. 禁出字段扫描

`scripts/dev/check-m3c-storage-migration-task-center.sh` 已覆盖 summary、create、list、detail、rerun、failure、retry 响应扫描。

未发现以下真值泄露：

- `/Volumes`
- `/Users`
- `nas://`
- `smb://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- bucket / object key
- raw DB row
- SQL
- secret / password / token

## 9. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3C 对象存储迁移任务中心专项                                 PASS=9 FAIL=0
M3C-1 asset UUID / storage status 回归                        PASS=15 FAIL=0
M3B 对象存储镜像迁移回归                                     PASS=11 FAIL=0
M3A StorageService 回归                                      PASS=8 FAIL=0
M2J 105 归属复核回归                                         PASS=6 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
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

## 10. 真实 NAS 与 docs 边界

- 是否触碰真实业务 NAS 文件：否。
- 是否移动、删除、重命名真实 NAS 文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否写 documents / chunks / OpenSearch / Qdrant / Hermes memory：否。
- 是否修改 `docs/**`：否。

专项脚本只使用 `/tmp` 下隔离临时文件资源验证对象镜像、幂等和失败重试。

## 11. Git 跟踪状态

本轮新增专项脚本已纳入 Git 跟踪：

- `scripts/dev/check-m3c-storage-migration-task-center.sh`

M3C-1 的 P1 文件也保持已跟踪：

- `backend/delivery-app/src/main/resources/db/migration/V30__m3c1_asset_uuid_storage_status.sql`
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh`

## 12. 服务状态

- 后端运行中：`http://127.0.0.1:8080`
- 前端 dev server 运行中：`http://127.0.0.1:5173`

按用户要求，本轮完成后未关闭项目服务。

## 13. 已知风险与下一步建议

- 当前任务执行仍是同步小样本策略，适合受控 MVP；后续大批量需要异步 worker、队列、进度刷新和速率限制。
- 当前文件选择器使用 catalog 分页查询，未做目录级迁移，也没有项目全量迁移入口，符合本轮边界。
- 后续可以补“按 `assetUuid` 输入 / 查询”的只读入口，减少员工接触内部数字 `fileId`。

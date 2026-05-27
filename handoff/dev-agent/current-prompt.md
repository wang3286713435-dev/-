# 开发 Agent 当前任务：M3G-1 NAS 侧 MinIO 就绪检查与对象化盘点

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 本批定位

本批是 M3G 的首个可验收子批次：

`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`

M3G 总目标是把真实项目文件从“NAS 文件夹路径管理”升级为“NAS 侧 MinIO 对象存储治理”。

但本批不直接执行全量迁移。本批先回答三个问题：

1. NAS 侧 MinIO 是否已经可被平台作为正式对象存储 endpoint 使用？
2. 当前所有真实项目对象化覆盖率、容量和风险是什么？
3. 如果开始历史文件对象化，按项目 / 目录 / 类型 / 大小筛选后会迁移多少文件、多少容量、有哪些风险？

## 1. 必须先阅读

开始前先阅读：

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md`
- `handoff/main-agent/m3f-object-storage-first-write-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`

## 2. 严格边界

本批允许：

- 新增或扩展只读 readiness / inventory / dry-run API。
- 前端展示 NAS 侧 MinIO 就绪状态、对象化覆盖率、dry-run 计划。
- 必要时追加 Flyway 迁移；不得修改旧迁移。
- 新增专项脚本。
- 更新 handoff 报告。

本批禁止：

- 不执行真实全量历史文件迁移。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不新增 Hermes 正文问答。
- 不让 Hermes 直接访问 NAS 或 MinIO 底层目录。
- 不把本机 Docker MinIO 说成正式 NAS 侧 MinIO。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、raw row、SQL、token、secret。
- 不修改 `docs/**`。

## 3. M3G-1 后端目标

### A. NAS 侧 MinIO readiness

提供只读 readiness 能力，用于判断当前对象存储配置是否满足 M3G。

可复用或扩展现有 provider health，建议提供业务化字段：

- provider code。
- configured。
- reachable。
- readable。
- writable。
- endpointType：`LOCAL_DEV_MINIO` / `NAS_SIDE_MINIO` / `UNKNOWN`。
- readinessStatus：`READY` / `NOT_CONFIGURED` / `UNREACHABLE` / `LOCAL_DEV_ONLY` / `WRITE_UNAVAILABLE`。
- message。

响应不得包含：

- endpoint 原文。
- access key。
- secret key。
- bucket。
- object key。

如果当前仍是本机 Docker MinIO，可以显示“本机开发对象存储可用，但未确认 NAS 侧 MinIO”，不能假装 M3G 已具备正式条件。

### B. 全项目对象化盘点

新增全项目 / 单项目对象化覆盖率查询。

至少返回项目级聚合：

- projectId。
- projectCode。
- projectName。
- totalFiles。
- totalBytes。
- objectStoredFiles。
- objectStoredBytes。
- nasOnlyFiles。
- migrationPendingFiles。
- migrationFailedFiles。
- checksumCoveredFiles。
- checksumCoverageRate。
- modelFiles。
- drawingFiles。
- documentFiles。
- largeFileCount。
- objectificationCoverageRate。
- riskLevel。
- riskMessages。

只基于 MySQL 台账和对象版本表做统计，不递归扫描真实 NAS。

### C. 对象化 dry-run 计划

新增 dry-run 计划接口，不复制文件。

建议接口：

- `POST /api/data-steward/projects/{projectId}/storage-objectification-plans:dry-run`

请求可支持：

- directoryPath。
- fileKinds。
- extensions。
- minSizeBytes / maxSizeBytes。
- checksumState：`ANY` / `HAS_CHECKSUM` / `MISSING_CHECKSUM`。
- storageState：`ANY` / `NAS_ONLY` / `MIGRATION_FAILED`。
- limit。
- maxTotalBytes。

响应必须包含：

- dryRun=true。
- migrationStarted=false。
- projectId。
- selectedFileCount。
- selectedTotalBytes。
- objectStoredSkipCount。
- missingChecksumCount。
- oversizedCount。
- unreadableRiskCount。
- estimatedBatches。
- riskMessages。
- sampleItems，最多 20 条。

sampleItems 可包含：

- fileId。
- assetUuid。
- fileName。
- fileKind。
- extension。
- sizeBytes。
- checksumStatus。
- storageStatus。
- reason。

不得包含真实 NAS 路径、bucket、object key。

### D. Hermes 副本取用契约只做状态预留

本批可以在 readiness / plan 文案中标明：

- Hermes 后续只能通过平台授权的 `assetUuid / objectVersion` 复制副本。
- 当前不发放真实 Hermes copy task。
- 当前不写 semantic evidence。

不要新增真实 Hermes 文件拉取执行能力。

## 4. 前端目标

优先在已有对象存储 / 文件服务 / 迁移任务页面上轻量接入，不要大改 UI。

至少展示：

- 当前对象存储就绪状态。
- 是否已确认 NAS 侧 MinIO。
- 项目对象化覆盖率。
- 已对象化 / 未对象化 / 失败 / pending。
- checksum 覆盖率。
- 容量预估。
- dry-run 计划入口。
- dry-run 结果说明：不会复制文件、不会修改 NAS、不会启动迁移。

如果当前 endpoint 还是本机 MinIO，页面要明确提示：

`当前对象存储仍是本机开发环境，尚未确认 NAS 侧 MinIO，不能启动真实全项目对象化。`

## 5. 专项脚本

新增：

`scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`

脚本至少验证：

1. 登录管理员。
2. 调用 NAS MinIO readiness / provider health。
3. 响应不泄露 endpoint、bucket、object key、secret。
4. 查询全项目对象化覆盖率。
5. 至少包含 105 / 503 或真实项目聚合。
6. 对 105 / 503 做 dry-run。
7. dry-run 返回 `dryRun=true`、`migrationStarted=false`。
8. dry-run 不创建迁移任务、不复制文件。
9. sampleItems 不含真实路径或对象 key。
10. M3F / M3E / M3C / file-access 回归通过。

## 6. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改动文件清单。
- 是否新增迁移。
- NAS 侧 MinIO readiness 如何判断。
- 当前环境是否仍是本机 MinIO。
- 全项目对象化盘点能力。
- dry-run 计划能力。
- 是否执行了真实迁移，必须为否。
- 是否触碰真实 NAS 文件，必须为否。
- 禁出字段扫描结果。
- 自测命令结果。
- 未完成事项和风险。

## 8. 完成定义

只有同时满足以下条件，才可交给测试 agent：

1. 平台能业务化展示 NAS 侧 MinIO readiness。
2. 当前对象存储是本机还是 NAS 侧不会被混淆。
3. 全项目对象化覆盖率可查。
4. 单项目 dry-run 可生成计划。
5. dry-run 不启动真实迁移。
6. 响应不泄露真实路径、bucket、object key、secret。
7. 前后端构建、专项脚本、回归脚本和 `git diff --check` 通过。

# 开发 Agent 当前任务：M3G-2 105 项目历史文件对象化上传灰度

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 当前结论与目标

M3G-1 已正式收口：

- 平台能识别 NAS 侧 MinIO：`NAS_SIDE_MINIO / READY`。
- 全项目对象化覆盖率可查。
- 503 / 105 项目 dry-run 可生成计划。
- M3E / M3F / M3C / file-access 回归均已通过。

当前进入：

`M3G-2：105 项目历史文件对象化上传灰度`

本批目标是让 105 项目少量历史 NAS 文件真实上传到 NAS 侧 MinIO，并验证读取链路切换。

重要：这里的“上传 MinIO”是复制副本到对象存储，不是移动、删除或改名 NAS 原文件。

## 1. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/m3g1-task-graph.md`
- `handoff/main-agent/m3g1-nas-minio-readiness-inventory-closure.md`
- `handoff/main-agent/m3g2-105-objectification-gray-plan.md`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `frontend/src/modules/data-steward/pages/FileServicePage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`

## 2. 严格边界

本轮允许：

- 只针对 105 项目，即本地 `projectId=503`，执行小批量对象化上传灰度。
- 复用或扩展已有对象迁移任务中心。
- 从 dry-run 结果生成受控迁移任务。
- 让 105 覆盖率页面展示对象化前后变化。
- 增加 M3G-2 专项脚本。
- 更新 `handoff/dev-agent/latest-report.md`。

本轮禁止：

- 不全量迁移全部项目。
- 不一键迁移 NAS 根目录。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不接入真实 BIM 引擎。
- 不把 `OBJECT_STORED` 但对象读取失败的文件静默 fallback 到 NAS 并伪装成功。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、endpoint 原文、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 3. 必须明确当前降级策略

当前平台读取策略应保持：

1. 文件有 active object version 且状态 `OBJECT_STORED`：优先读取对象存储。
2. 文件没有 active object version：读取原 NAS 台账路径。
3. 文件标记 `OBJECT_STORED` 但对象副本不可读：fail-closed，提示对象副本异常；不得偷偷读 NAS。

第 3 条不能改。这是对象存储治理的可信边界。

## 4. 本轮必须完成

### A. 105 小批量对象化执行

请基于已有 dry-run / migration task 能力完成：

- 确认 readiness 是 `NAS_SIDE_MINIO / READY`。
- 对 105 / `projectId=503` 生成 dry-run 计划。
- 从 dry-run 计划选择一批文件创建对象化迁移任务。
- 每批保持小批量，建议沿用现有后端安全上限，不要为了本轮放大到全项目。
- 任务执行后写入 `data_storage_objects` 和 active `data_file_object_versions`。
- 任务失败时必须保留失败原因。
- 重复执行必须幂等，不能污染 active object version。

如果当前接口已经支持明确 `fileIds` 创建任务，可以优先复用；如前端缺少“从 dry-run 执行”的入口，可在前端补一个最小受控入口。

### B. 读取链路验证

必须证明：

- 已对象化文件 `storage-status=OBJECT_STORED`。
- 已对象化文件通过受控 `file-access` 可读取。
- 未对象化文件仍显示 `NAS_ONLY`，并通过原 NAS 链路可用。
- 对象副本异常时不静默 fallback。

### C. 前端可见性

文件服务 / 对象存储页面至少能让用户看懂：

- 当前是 NAS 侧 MinIO。
- 105 当前对象化覆盖率。
- dry-run 将选择多少文件、多少容量。
- 执行灰度后成功 / 跳过 / 失败数量。
- 对象化完成后，哪些文件已是 `OBJECT_STORED`。

文案要清楚说明：NAS 原文件保留，平台只是复制副本到对象存储。

### D. 新增专项脚本

新增：

`scripts/dev/check-m3g2-105-objectification-gray.sh`

脚本至少验证：

1. readiness 是 `NAS_SIDE_MINIO / READY`。
2. 105 对象化盘点可查。
3. dry-run 可生成计划。
4. 小批量迁移任务可创建并完成。
5. `OBJECT_STORED` 数量增加或重复运行幂等跳过。
6. 已对象化文件可通过受控 `file-access` 读取。
7. 未对象化文件仍可用，并仍解释为 `NAS_ONLY`。
8. NAS 原项目文件未被移动、删除、改名。
9. 响应禁出字段扫描通过。

## 5. 推荐灰度范围

默认只选 105 项目安全小批：

- 优先 PDF / 图片 / 小型 Office / 小型 DWG。
- 第一批不要包含超大文件。
- 第一批不要超过现有迁移任务安全上限。
- 如果 dry-run 没有合适样本，脚本应明确失败原因，不要扩大到其他项目。

## 6. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g2-105-objectification-gray.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

1. 本轮实际对象化了多少个 105 文件。
2. 成功 / 跳过 / 失败数量。
3. 105 覆盖率变化。
4. 已对象化文件是否经由 `file-access` 从对象存储读取。
5. 未对象化文件是否仍按 NAS_ONLY 可用。
6. NAS 原文件是否被移动、删除、改名，必须明确回答“否”。
7. 是否读取正文、写语义索引、触发 Hermes，必须明确回答“否”。
8. 自测结果。
9. 未完成事项。

## 8. 完成定义

只有同时满足以下条件，才能标记完成：

- M3G-2 专项通过。
- 105 至少一批真实历史文件对象化成功，或已对象化样本幂等跳过且脚本能证明读取链路。
- 已对象化文件显示 `OBJECT_STORED`。
- 未对象化文件仍显示 / 解释为 `NAS_ONLY`。
- file-access 回归通过。
- 未触碰真实 NAS 原项目文件。
- 未修改 `docs/**`。

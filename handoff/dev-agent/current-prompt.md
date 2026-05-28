# 开发 Agent 当前任务：M3G-3 多真实项目分批对象化策略与任务中心增强

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 当前结论与目标

M3G-2 已正式收口：

- 105 / `projectId=503` 已完成小批历史文件对象化。
- 已对象化文件可通过受控 `file-access` 读取。
- 未对象化文件仍保持 `NAS_ONLY` 并继续可用。
- NAS 原文件未移动、未删除、未改名。

当前进入：

`M3G-3：多真实项目分批对象化策略与任务中心增强`

本批不是全量迁移。本批目标是让平台具备多真实项目对象化的规划、容量评估、筛选、限额、任务中心增强能力，为后续扩大对象化范围做准备。

## 1. 必须先阅读

- `handoff/main-agent/m3g3-multi-project-objectification-task-center-plan.md`
- `handoff/main-agent/m3g2-105-objectification-gray-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `scripts/dev/check-m3g2-105-objectification-gray.sh`

## 2. 严格边界

本轮允许：

- 增强多真实项目对象化盘点。
- 增强 dry-run 计划能力。
- 增强迁移任务中心展示和接口契约。
- 增加文件数 / 容量 / 项目范围限制。
- 预留速率限制、并发上限、暂停 / 继续字段。
- 增加前端多项目对象化规划与任务中心视图。
- 新增 M3G-3 专项脚本。
- 更新 `handoff/dev-agent/latest-report.md`。

本轮禁止：

- 不全量迁移所有项目。
- 不一键迁移 NAS 根目录。
- 不默认真实执行多项目对象化。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不接入真实 BIM 引擎。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、endpoint 原文、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 3. 本轮必须完成

### A. 多项目对象化盘点增强

在现有全项目对象化盘点基础上，补齐或确认可返回：

- 项目 ID / 编码 / 名称。
- 项目来源或项目类型，能区分真实 NAS 项目与样例 / 测试 / 归档项目。
- 总文件数。
- 已对象化数。
- NAS_ONLY 数。
- MIGRATION_FAILED 数。
- checksum 覆盖率。
- 总容量。
- 已对象化容量。
- 预计待对象化容量。
- 文件类型分布或扩展名分布。
- 超大文件数。
- 路径不可读数。

响应不得返回真实路径、bucket、object key。

### B. 多项目 dry-run 计划

实现或扩展一个多项目 dry-run 能力。

要求：

- 支持多个 projectId。
- 支持 storageState、checksumState、extensions、minSizeBytes、maxSizeBytes、maxTotalBytes、limit 等筛选。
- 支持总文件数上限和总容量上限。
- 结果按项目分组。
- 返回 selected / skipped / risk / reason。
- dry-run 必须只读，不能创建迁移任务。

如果你判断复用现有单项目 dry-run 更稳，可以先实现“前端聚合多个单项目 dry-run”的方案；但专项脚本必须能验证多项目计划口径。

### C. 任务中心增强

增强文件服务 / 对象存储页面：

- 展示多项目对象化规划入口。
- 展示项目风险和覆盖率。
- 展示 dry-run 分组结果。
- 展示任务来源：手动 fileIds / dry-run / 多项目计划。
- 展示策略字段：项目范围、筛选条件、文件数上限、容量上限。
- 预留并发上限 / 速率限制 / 暂停继续字段。

本批不要求真的实现复杂 worker 暂停；如只预留字段和展示，请在报告里说明。

### D. 受控执行边界

默认不要执行多项目真实迁移。

如需要创建任务来验证接口，只能：

- 明确指定小范围项目。
- 使用小上限。
- 不超过现有安全上限。
- 不触碰 NAS 原文件。
- 保持幂等。

### E. 新增专项脚本

新增：

`scripts/dev/check-m3g3-multi-project-objectification-planning.sh`

默认必须是只读脚本，不执行真实对象化。

脚本至少验证：

1. readiness 为 `NAS_SIDE_MINIO / READY`。
2. 全项目对象化盘点可查。
3. 真实项目和样例 / 测试 / 归档项目可区分或过滤。
4. 多项目 dry-run 可生成计划。
5. dry-run 未创建迁移任务。
6. 结果按项目分组。
7. 文件数 / 容量上限生效。
8. 禁出字段扫描通过。
9. 脚本已纳入 Git。

## 4. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

注意：不要默认运行 M3G-2 执行型脚本，避免继续对象化 105 文件。

## 5. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

1. 多项目盘点增强了哪些字段。
2. 多项目 dry-run 如何工作。
3. 是否创建真实迁移任务；如有，必须说明范围、数量和原因。
4. 是否触碰真实 NAS 原文件，必须明确回答“否”。
5. 是否读取正文、写语义索引、触发 Hermes，必须明确回答“否”。
6. 前端任务中心增强点。
7. 自测结果。
8. 未完成事项和后续扩展。

## 6. 完成定义

只有同时满足以下条件，才能标记完成：

- M3G-3 专项通过。
- 多项目对象化规划能力可用。
- dry-run 默认只读且不创建迁移任务。
- 任务中心能展示项目范围、筛选条件、上限和状态。
- 真实 NAS 原文件未被破坏。
- 未修改 `docs/**`。

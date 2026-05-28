# 开发 Agent 当前任务：M3G-4 受控多项目小批对象化执行

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 当前结论与目标

M3G-3 已正式收口：

- NAS 侧 MinIO readiness 为 `NAS_SIDE_MINIO / READY`。
- 全项目对象化盘点可查。
- 多项目 dry-run 可生成分项目计划。
- dry-run 不创建迁移任务、不复制文件、不触碰真实 NAS。

当前进入：

`M3G-4：受控多项目小批对象化执行`

本批开始允许真实小批对象化，但必须严格受控：少量真实项目、少量文件、人工确认、后端硬上限、可审计、可幂等。

## 1. 必须先阅读

- `handoff/main-agent/m3g4-controlled-multi-project-objectification-plan.md`
- `handoff/main-agent/m3g3-multi-project-objectification-task-center-closure.md`
- `handoff/main-agent/m3g2-105-objectification-gray-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m3g3-multi-project-objectification-planning.sh`

## 2. 严格边界

本轮允许：

- 增加多项目小批对象化执行接口。
- 复用 M3G-3 dry-run 选择逻辑。
- 复用现有对象迁移任务中心和对象化流程。
- 对真实 NAS 项目做小批文件对象化副本上传到 NAS 侧 MinIO。
- 写对象台账和 active object version。
- 标记已对象化文件为 `OBJECT_STORED`。
- 前端文件服务页增加“确认执行小批对象化”入口。
- 新增 M3G-4 专项脚本。
- 更新 `handoff/dev-agent/latest-report.md`。

本轮禁止：

- 不全量迁移所有项目。
- 不迁移 NAS 根目录。
- 不迁移测试 / 样例 / 归档项目。
- 不默认真实执行；必须 `confirmed=true`。
- 不越过后端硬上限。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不接入真实 BIM 引擎。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、endpoint 原文、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 3. 后端硬上限

必须在后端强制，不只靠前端：

- 单次最多 `3` 个真实项目。
- 单项目最多 `3` 个文件。
- 总文件数最多 `9` 个。
- 单项目容量最多 `50MB`。
- 总容量最多 `100MB`。
- 只允许 `realNasProject=true` 的项目。
- 必须要求 `confirmed=true`。

超过上限必须返回业务错误，不能截断后悄悄执行。

## 4. 本轮必须完成

### A. 多项目执行接口

新增或扩展接口，建议：

```http
POST /api/data-steward/storage-objectification-plans:execute
```

要求：

- 请求参数尽量复用 M3G-3 dry-run。
- `confirmed=false` 或缺失时必须拒绝。
- `realProjectsOnly` 必须为真，或后端必须强制真实项目过滤。
- 只处理 `NAS_ONLY` 候选。
- 按项目分组选择文件。
- 创建或复用现有迁移任务中心记录。
- 每个文件走现有安全流程：
  - 权限校验。
  - 生命周期校验。
  - NAS 文件可读性校验。
  - checksum 计算或复用。
  - 上传 NAS 侧 MinIO。
  - 校验 size / checksum / etag。
  - 写 `data_storage_objects`。
  - 写 active `data_file_object_versions`。
  - 写审计与任务进度。
- 重复执行必须幂等，不能重复污染 active object version。

### B. 任务中心增强

任务或执行结果需能说明：

- 来源：`MULTI_PROJECT_CONTROLLED_EXECUTION`。
- 项目范围。
- 文件数 / 容量限制。
- 成功数。
- 跳过数。
- 失败数。
- 每个失败原因。
- 是否触发真实迁移。

### C. 前端小批执行入口

在文件服务页现有“多项目对象化规划”区域增加：

- “确认执行小批对象化”按钮。
- 执行前风险提示。
- 执行结果摘要。
- 创建任务或执行结果列表。
- 对象存储不可用时禁用执行。

文案必须说明：

- 只复制文件副本到 NAS 侧 MinIO。
- 不移动、不删除、不改名 NAS 原文件。
- 执行范围受控。

### D. 新增专项脚本

新增：

`scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

脚本可以执行真实小批对象化，但必须严格限制：

- 自动选择最多 `2-3` 个真实项目。
- 每项目最多 `1` 个文件。
- 总文件数最多 `3` 个。
- 总容量不超过 `100MB`。
- 先验证 `confirmed=false` 被拒绝。
- 再用 `confirmed=true` 执行。
- 验证重复执行幂等跳过。
- 验证已对象化文件可通过受控 `file-access` 读取。
- 验证禁出字段。

## 5. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

注意：不要运行 M3G-2 执行型脚本。

## 6. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

1. 新增 / 修改了哪些接口。
2. 后端硬上限如何实现。
3. 本轮真实执行了哪些项目、多少文件、多少容量。
4. 是否移动、删除、重命名、覆盖真实 NAS 原文件，必须明确回答“否”。
5. 是否读取正文、写语义索引、触发 Hermes，必须明确回答“否”。
6. 重复执行幂等结果。
7. 前端小批执行入口如何使用。
8. 自测结果。
9. 未完成事项和后续风险。

## 7. 完成定义

只有同时满足以下条件，才能标记完成：

- M3G-4 专项通过。
- 未确认执行被拒绝。
- 超限执行被拒绝。
- 小批真实对象化成功。
- 重复执行幂等。
- 已对象化文件通过受控 `file-access` 读取。
- NAS 原文件未被破坏。
- 未修改 `docs/**`。

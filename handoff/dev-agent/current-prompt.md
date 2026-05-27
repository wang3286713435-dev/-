# 开发 Agent 当前任务：M3F 新文件对象存储优先写入

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3f-object-storage-first-write`

## 0. 本批定位

本批是：

`M3F：新文件对象存储优先写入与 NAS 兼容回退`

M3A-M3E 已完成：

- StorageService 与对象存储基础表。
- assetUuid / storage-status。
- 对象存储迁移任务中心。
- 105 真实 NAS 小样本对象存储镜像。
- PDF / 图片预览产物对象化，以及 DWG / RVT / Office 转换占位。

M3F 的目标是让**新增上传文件**优先进入对象存储，让后续新数据天然走对象存储底座；历史 NAS 文件仍按既有台账和迁移任务逐步处理。

本批不是全量历史项目迁移，也不是 Hermes 正文问答。

## 1. 必须先阅读

开始前先阅读：

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3f-object-storage-first-write-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/repository/ControlledNasRepository.java`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`

重点先确认现状：

1. 文件管理器上传当前是否仍走 `ControlledNasApplicationService.uploadFile(...)` 写 NAS。
2. `StorageService` 目前是否只有 NAS 文件镜像到对象存储能力，是否缺“直接写入对象存储”的能力。
3. `file-access` 是否能读取 active object version。
4. 新上传文件如何创建 `data_file_resources`、`asset_uuid`、`data_storage_objects`、`data_file_object_versions`。

## 2. 严格边界

本批允许：

- 修改后端文件上传链路，使新增文件内容优先写入对象存储。
- 扩展 `StorageService`，增加受控对象写入能力。
- 必要时追加 Flyway 迁移；不得修改旧迁移。
- 更新文件管理器上传后的状态展示。
- 新增专项脚本。
- 修改 handoff 报告。

本批禁止：

- 不做全量 NAS 搬迁。
- 不批量迁移所有项目历史文件。
- 不移动、删除、重命名、覆盖真实 NAS 文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不新增 Hermes 正文问答。
- 不进入 BIM 引擎真实接入。
- 不开放永久删除。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、raw row、SQL、token、secret。
- 不修改 `docs/**`。

## 3. 推荐实现口径

### A. 新上传文件默认对象存储优先

文件管理器上传文件时，目标口径是：

1. 校验项目权限、灰度写开关和当前目录权限。
2. 接收 multipart 文件。
3. 计算 checksum / size / contentType。
4. 上传到对象存储 active provider（MinIO / S3-compatible）。
5. 写入 `data_storage_objects`。
6. 写入 `data_file_resources` 业务台账。
7. 写入 `data_file_object_versions`，并标记 active。
8. 单文件 `storage-status` 返回 `OBJECT_STORED`。
9. `file-access` 预览 / 下载可读取对象存储内容。

新增文件不应再为了保存内容而先写入真实业务 NAS 目录。

### B. NAS 兼容回退必须受控

如果对象存储未配置或不可用，请不要静默假成功。

推荐策略：

- 默认返回清晰业务错误：`对象存储暂不可用，新增文件未写入`。
- 如代码中已有明确兼容配置，可支持显式 `OBJECT_FIRST_WITH_NAS_FALLBACK`，但必须：
  - 写审计。
  - response / operation log 显示发生 NAS fallback。
  - storage-status 为 `NAS_ONLY`，不能伪装成 `OBJECT_STORED`。

不要把对象存储失败时的 NAS fallback 做成用户无感的默认行为。

### C. 空文件夹仍按现有 NAS 目录能力处理

对象存储天然没有真实空目录。本批不要求重做目录模型。

保持：

- 新建文件夹仍走现有受控 NAS 目录能力。
- 历史 NAS 文件仍按既有文件管理器和迁移任务工作。
- 本批只改变“新增文件内容”的默认落点。

如需在对象存储中表达逻辑目录，请先在报告中说明，不要大改目录系统。

### D. 业务台账必须继续稳定

新上传对象存储文件仍必须进入 `data_file_resources`，并具备：

- `assetUuid`
- `projectId`
- `fileName`
- `fileKind`
- `extension`
- `sizeBytes`
- `checksum`
- `version`
- `discipline`
- 当前目录逻辑路径或相对路径提示
- `processStatus`
- `confidenceLevel`

前端和 API 不得返回底层对象定位。用户只能看到平台资产 ID、文件名、类型、大小、预览状态、对象存储状态等业务字段。

### E. 审计与操作记录

新上传对象存储文件必须写审计或操作记录，至少表达：

- 谁上传。
- 上传到哪个项目。
- 文件名。
- 写入对象存储是否成功。
- 是否发生 fallback。

审计 / 操作记录不得包含 bucket、object_key、真实 NAS 路径。

## 4. 建议新增或调整接口

优先复用现有上传接口：

- `POST /api/data-steward/projects/{projectId}/nas/files:upload`

外部契约尽量不变，但行为改为对象存储优先。

如你认为接口名称里的 `nas` 会造成误解，本批不要直接删除旧接口；可以：

- 保留旧接口兼容。
- 内部实现转为 object-first。
- 可新增别名接口，但必须保证旧前端和测试不回归。

上传响应建议返回：

- `fileId`
- `assetUuid`
- `projectId`
- `fileName`
- `fileKind`
- `extension`
- `sizeBytes`
- `checksum`
- `storageStatus=OBJECT_STORED`
- `storageProvider=OBJECT_STORAGE` 或业务化 provider label
- `message`

绝不返回：

- bucket
- object key
- `storage_uri`
- 真实 NAS 路径
- raw row
- SQL
- token / secret

## 5. 前端要求

最小前端接入即可，不要大改 UI：

- 文件管理器上传后刷新当前目录。
- 新上传文件在列表中可见。
- 新上传文件的存储状态能显示为“对象存储”或 `OBJECT_STORED` 对应业务文案。
- 文件详情 / 技术信息中可看到平台资产 ID，但不暴露对象 key。
- 预览 / 下载仍走受控 `file-access`。
- 对象存储不可用时显示友好错误，不要只弹英文堆栈。

## 6. 专项脚本

新增：

`scripts/dev/check-m3f-object-storage-first-write.sh`

脚本至少验证：

1. 登录管理员。
2. 准备隔离测试项目或安全测试目录，不能写真实业务目录。
3. 通过现有上传接口上传一个小文件。
4. 新文件生成 `assetUuid`。
5. 新文件 `storage-status=OBJECT_STORED`。
6. 数据库存在 active object version。
7. 受控 `file-access` 可读取上传内容。
8. 响应和访问结果不包含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、SQL、raw row、token、secret。
9. 对象存储不可用场景返回业务错误或明确 fallback 状态，不 500，不假成功。
10. 未在真实业务 NAS 目录生成新增文件本体。

必须回归：

- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`
- `scripts/dev/check-m3b-object-storage-mirror-trial.sh`
- `scripts/dev/check-m3a-storage-service-foundation.sh`
- `scripts/dev/check-phase2-batch4-file-access.sh`

## 7. 自测要求

完成后至少执行并记录：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如后端未启动，请按项目已有方式启动后再重试健康检查和脚本。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改动文件清单。
- 是否新增迁移。
- 新上传文件写入对象存储的完整流程。
- 是否保留旧上传接口兼容。
- 是否存在 NAS fallback，以及 fallback 是否显式。
- 新文件如何写入 `data_file_resources` / `data_storage_objects` / `data_file_object_versions`。
- file-access 是否可读取对象存储新增文件。
- 前端显示变化。
- 禁出字段扫描结果。
- 自测命令结果。
- 未完成事项和风险。

## 9. 完成定义

只有同时满足以下条件，才可交给测试 agent：

1. 新上传文件默认进入对象存储。
2. 新上传文件有稳定 `assetUuid`。
3. 新上传文件 `storage-status=OBJECT_STORED`。
4. `file-access` 可受控读取对象存储新增文件。
5. 响应不暴露底层对象定位或真实 NAS 路径。
6. 对象存储不可用时不假成功。
7. 历史 NAS 文件、对象迁移任务、预览产物不回归。
8. 前后端构建、专项脚本、回归脚本和 `git diff --check` 通过。

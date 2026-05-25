# 开发 Agent 当前任务：M3A 对象存储与 StorageService 基线

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮进入主线新批次：

`M3A：对象存储与 StorageService 基线`

本轮只做存储底座，不做全量 NAS 迁移、不做语义解析、不做 Hermes 正文问答。

## 0. 必须先阅读

开始前先阅读：

- `handoff/main-agent/m3a-storage-service-foundation-plan.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/dev-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/StorageRootRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/FileAccessTicketRepository.java`
- `infra/docker-compose.yml`

重点确认当前事实：

- `data_file_resources` 已有 `storage_provider / storage_uri / storage_key / checksum` 等字段。
- 当前 `file-access` 受控读取在 `minio://`、`s3://`、`oss://` 时会返回 `STORAGE_PROVIDER_UNSUPPORTED`。
- Docker 基础设施中已有 MinIO，但平台主链路尚未接入对象存储读取。

## 1. 本轮目标

建立可持续扩展的受控存储底座：

1. 新增对象存储元数据模型。
2. 新增 `StorageService` 适配层。
3. 让现有 `file-access` 预览 / 下载内部通过 `StorageService` 读取。
4. 保持 NAS 文件访问不回归。
5. 支持 MinIO / S3-compatible 的基础健康检查和受控测试读取。
6. 输出 provider health 与文件 storage status API。
7. 不向前端暴露真实路径、bucket、object key、storage URI。

## 2. 数据模型要求

追加 Flyway 迁移，不修改旧迁移。

当前最后迁移为 `V27__m2i_file_ownership_governance.sql`，本轮建议新增：

- `V28__m3a_storage_objects_foundation.sql`

建议新增表：

- `data_storage_objects`
- `data_file_object_versions`
- `data_object_migration_tasks`
- `data_file_derivatives`
- `data_preview_artifacts`

最小字段建议：

- provider：`NAS` / `MINIO` / `S3_COMPATIBLE`
- bucket
- object_key
- object_version
- etag
- checksum
- content_type
- size_bytes
- source_provider
- source_uri_digest
- source_path_digest
- storage_state
- migration_status
- last_verified_at
- created_by / updated_by / created_at / updated_at / deleted

注意：

- `data_file_resources` 仍是业务资产主表，不被新表替代。
- bucket / object_key 可以入库，但不得直接返回前端。
- NAS 源路径只能通过 digest 或内部字段审计保留，不能对普通接口暴露。

## 3. StorageService 要求

新增统一存储适配层，命名可按项目风格调整，但必须表达清楚职责。

必须支持：

- NAS 读取。
- MinIO / S3-compatible 基础读取。
- 文件存在性校验。
- 内容类型推断。
- 流式读取。
- 校验失败时返回业务化错误。

建议接口能力：

- `openReadable(fileResource)`
- `exists(storageReference)`
- `probeContentType(storageReference)`
- `providerHealth()`

要求：

- 现有 `file-access` 预览 / 下载内部改为走 `StorageService`。
- 不能破坏现有 `PREVIEW` / `DOWNLOAD` ticket 契约。
- 不能绕过现有项目权限、文件权限、生命周期校验和审计。
- `minio://` 或 `s3://` 测试对象可通过受控路径读取。
- 未配置对象存储时返回业务化不可用原因，不 500。

## 4. API 要求

新增接口：

### A. Provider health

`GET /api/data-steward/storage/providers/health`

返回：

- provider code
- display name
- configured
- available
- readonly / writable 基础状态
- unavailable reason

禁止返回：

- endpoint 密钥
- access key / secret key
- bucket key
- 真实 NAS 路径
- raw storage URI

### B. File storage status

`GET /api/data-steward/assets/files/{fileId}/storage-status`

返回：

- fileId
- projectId
- storageState：`NAS_ONLY` / `OBJECT_STORED` / `MIGRATION_PENDING` / `MIGRATION_FAILED`
- activeProvider
- objectStored
- checksumAvailable
- lastVerifiedAt
- migrationStatus
- business message

禁止返回：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- bucket
- object_key
- SQL
- raw row
- token / secret

## 5. 明确禁止事项

本轮严禁：

1. 不做全量 NAS 迁移。
2. 不移动、删除、重命名真实 NAS 文件。
3. 不读取 PDF / Office / DWG / RVT / IFC 正文。
4. 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
5. 不新增 Hermes 正文问答。
6. 不启动真实 BIM 轻量化。
7. 不把对象存储接入说成“语义理解完成”。
8. 不把 catalog-only 元数据当正文证据。
9. 不暴露真实 NAS 路径、bucket、object key、storage_uri、raw row、SQL、token、secret。
10. 不修改 `docs/**`，除非主 agent 单独授权。

## 6. 建议新增脚本

新增：

`scripts/dev/check-m3a-storage-service-foundation.sh`

脚本至少验证：

1. Provider health 接口返回统一响应。
2. MinIO 容器存在时能返回 configured / available 状态。
3. 未配置对象存储时不 500，返回业务化原因。
4. 文件 storage-status 不泄露禁出字段。
5. NAS 文件现有受控预览 / 下载仍可用。
6. minio/s3 测试对象如可创建，则可通过后端受控读取。
7. 所有响应不得包含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、SQL、raw row、token、secret。

## 7. 必跑验证

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如果某个回归脚本因本地服务状态或环境原因无法执行，必须在报告里写明原因，不得静默跳过。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 本轮改动文件清单。
- 新增表和字段说明。
- StorageService 职责边界。
- `file-access` 如何接入 StorageService。
- NAS 访问回归结果。
- MinIO / S3-compatible 能力状态。
- Provider health 接口结果。
- File storage status 接口结果。
- 禁出字段扫描结果。
- 必跑脚本结果。
- 是否修改 `docs/**`。
- 是否触碰真实 NAS 文件。
- 已知风险和下一批 M3B 建议。

## 9. 完成定义

只有同时满足以下条件，才能标记完成：

- 对象存储元数据模型已追加迁移。
- StorageService 基线可用。
- NAS 原有预览 / 下载不回归。
- 对象存储 provider health 可查。
- 文件 storage status 可查。
- `STORAGE_PROVIDER_UNSUPPORTED` 的架构缺口有受控处理路径。
- 响应不泄露真实路径、bucket、object key、storage_uri。
- 前后端构建、健康检查、M3A 专项、核心回归、`git diff --check` 通过。
- `handoff/dev-agent/latest-report.md` 已写。

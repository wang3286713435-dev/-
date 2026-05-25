# M3A：对象存储与 StorageService 基线计划

## 1. 批次定位

`M3A` 是 M3 对象存储路线的第一批，只做存储底座和受控读取架构，不做 NAS 全量迁移、不做语义解析、不做 Hermes 正文问答。

当前平台仍以 `NAS + MySQL 台账` 为主：

- NAS 保存真实文件本体。
- MySQL 保存项目、资产、权限、交付、审核、整改、归属、审计等业务台账。
- MinIO 已在 Docker 基础设施中存在，但主链路尚未正式接入。
- 当前受控访问遇到 `minio://` / `s3://` / `oss://` 会返回 `STORAGE_PROVIDER_UNSUPPORTED`。

M3A 目标是建立“未来能承接对象存储”的稳定存储抽象，并保持现有 NAS 文件访问不回归。

## 2. M3A 必做

### 数据模型

追加 Flyway 迁移，不修改旧迁移。

建议新增或预留：

- `data_storage_objects`
- `data_file_object_versions`
- `data_object_migration_tasks`
- `data_file_derivatives`
- `data_preview_artifacts`

`data_file_resources` 继续作为业务资产台账，不被对象存储表取代。

对象存储表至少应能表达：

- provider：`NAS` / `MINIO` / `S3_COMPATIBLE`
- bucket / object key / object version / etag / checksum / content type / size
- source provider / source path digest / source uri digest
- storage state / migration status / last verified at
- created / updated / deleted 标识

### StorageService

新增统一存储适配层：

- NAS 读取保持可用。
- MinIO / S3-compatible 基础读写探测可用。
- 现有 `file-access` 预览 / 下载内部改走 StorageService。
- 外部响应保持原有受控访问契约。
- 前端不得获得 bucket、object key、storage URI 或真实 NAS 路径。

### API

新增最小只读接口：

- `GET /api/data-steward/storage/providers/health`
- `GET /api/data-steward/assets/files/{fileId}/storage-status`

接口必须返回统一响应和 traceId，不返回 endpoint 密钥、bucket key、真实路径、SQL、raw row。

### Handoff / ADR

开发 agent 需要在 `handoff/dev-agent/latest-report.md` 写清：

- NAS 是源头和过渡来源。
- 对象存储是镜像承接、预览产物、交付归档和后续语义处理底座。
- MySQL 仍是业务、权限、交付、审计中心。
- Hermes 后续只能通过平台 Gateway 获取脱敏证据，不直连 MySQL / NAS / MinIO / 向量库 / 搜索引擎。

## 3. M3A 禁止

- 不做全量 NAS 迁移。
- 不移动、删除、重命名真实 NAS 文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
- 不新增 Hermes 正文问答。
- 不启动真实 BIM 轻量化。
- 不把对象存储接入说成“平台已经理解文件语义”。
- 不把 catalog-only 元数据当正文证据。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、token、secret、SQL、raw row。

## 4. 后续路线

M3A 通过后，再进入：

1. `M3B：105 小样本对象存储镜像迁移`
2. `M3C：迁移任务中心与批量策略`
3. `M3D：预览产物与交付包归档对象化`
4. `M4：语义证据层`
5. `M5：Hermes 受控证据问答`

M3A 不允许提前混入 M3B-M5 的实现。

## 5. 验收口径

M3A 收口必须满足：

- 后端构建、前端构建、健康检查通过。
- MinIO 可用时 provider health 返回可用；未配置时返回业务化不可用原因，不 500。
- NAS 文件原有预览 / 下载不回归。
- `minio://` 或对象存储测试对象能通过后端受控读取，不暴露 bucket/key。
- 新增专项脚本 `scripts/dev/check-m3a-storage-service-foundation.sh` 通过。
- M2J / M2I / M2H / M2F / M2B / 文件访问安全回归通过。
- `git diff --check` 通过。

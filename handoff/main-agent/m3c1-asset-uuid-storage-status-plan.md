# M3C-1：资产 UUID 与存储状态统一计划

日期：2026-05-26

## 目标

在进入对象存储迁移任务中心之前，先为文件资产补齐稳定公开 ID 和统一存储状态口径。

本批只解决：

```text
内部 file_id -> 平台公开 assetUuid
NAS / MinIO 状态 -> 统一 storageState
FileAssetView / ModelAssetView / API -> 可安全引用 assetUuid
```

不做全量迁移、不做 Hermes 正文问答、不写向量库、不读取文件正文。

## 范围

### 1. 数据库

追加新 Flyway 迁移，预计版本：

```text
V30__m3c1_asset_uuid_storage_status.sql
```

要求：

- 给 `data_file_resources` 新增 `asset_uuid`，建议 `CHAR(36)` 或等价稳定格式。
- 既有文件一次性补齐 UUID。
- 新增唯一索引，保证全局唯一。
- 不修改旧迁移。
- 如需更新 `FileAssetView` / `ModelAssetView`，必须通过新迁移 `CREATE OR REPLACE VIEW`。

### 2. 后端写入路径

所有新建文件资产入口都必须生成 `asset_uuid`：

- NAS 扫描自动入库 / 人工审核通过。
- 手工创建文件资源。
- 受控 NAS 上传 / 新增文件。
- 测试夹具或样板数据新增文件。

### 3. 只读接口和 DTO

优先在这些输出中提供 `assetUuid`：

- 文件资源详情 / 列表。
- Catalog 文件列表 / 详情。
- FileAssetView。
- ModelAssetView 中模型文件对应的 asset UUID。
- storage-status。
- file-access 相关审计 / 结果如已有文件摘要，可带安全 asset UUID。

注意：

- `fileId` 仍保留为内部主键和排障 ID。
- 前端展示“平台资产ID”优先用 `assetUuid`。
- 不要把 `fileId` 文案描述为长期公开 ID。

### 4. 存储状态统一

对外状态统一为：

- `NAS_ONLY`
- `MIGRATION_PENDING`
- `OBJECT_STORED`
- `MIGRATION_FAILED`

内部如已有批次级 `MIGRATION_PARTIAL`，可以保留在任务批次表，但单文件 storage status 不应混乱输出。

## 禁止事项

- 不迁移全量 NAS。
- 不新增 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不移动、删除、重命名真实 NAS 文件。
- 不暴露真实 NAS 路径、bucket、object key、`storage_uri`、raw row、SQL、token、secret。
- 不修改仓库 `docs/**`。

## 验收重点

- 既有文件均有 `asset_uuid`。
- 新建文件均自动生成 `asset_uuid`。
- `asset_uuid` 全局唯一。
- `FileAssetView` / `ModelAssetView` 可查到 asset UUID。
- 相关 API 返回 `assetUuid`，且禁出字段扫描通过。
- storage status 状态口径稳定。
- M3A / M3B / M2J / M2I / M2H / file-access 回归通过。

# M3F：新文件对象存储优先写入与 NAS 兼容回退计划

## 1. 批次裁决

当前启动：

`M3F：新文件对象存储优先写入与 NAS 兼容回退`

定位：

- 让后续通过平台上传的新文件优先进入对象存储。
- 历史 NAS 文件仍保留原位，继续通过迁移任务逐步对象化。
- 不做全量 NAS 搬迁。
- 不进入 Hermes 正文问答、语义索引或 BIM 引擎接入。

后续另开：

`M3G：NAS 侧 MinIO 对象存储接管真实项目文件`

M3G 才负责把对象存储主链路切换到 NAS 侧 MinIO，并按项目 / 目录 / 文件类型 / 大小范围逐步把历史项目副本镜像到 NAS 侧 MinIO。

## 2. 为什么插入 M3F

M3A-M3E 已证明对象存储能力可用，但目前主链路仍是：

```text
新增文件 -> NAS 写入 -> MySQL 台账 -> 后续再对象化
```

这会导致后续数据继续沉淀在 NAS。M3F 需要把新数据的入口改成：

```text
新增文件 -> 对象存储写入 -> MySQL 台账 -> file-access 受控访问
```

这样才能支撑后续语义证据层、预览转换产物、Hermes 受控问答和非结构化数据治理。

## 3. 能力边界

M3F 做：

- 文件管理器上传文件对象存储优先。
- 新增文件生成 `assetUuid`。
- 新增文件写入 `data_file_resources`。
- 新增对象写入 `data_storage_objects`。
- 新增 active 对象版本写入 `data_file_object_versions`。
- 新增文件 `storage-status=OBJECT_STORED`。
- `file-access` 能读取对象存储新增文件。
- 对象存储不可用时不假成功。
- 前端展示对象存储状态。

M3F 不做：

- 不全量迁移历史 NAS 文件。
- 不移动、删除、重命名真实 NAS 文件。
- 不读取文件正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不新增 Hermes 正文问答。
- 不接真实 BIM 引擎。
- 不开放永久删除。

## 4. 关键产品口径

M3F 完成后，可以对内说：

> 平台已支持对象存储作为新增文件的主存储，历史 NAS 文件仍通过对象化迁移任务逐步镜像。

M3F 完成后仍不能说：

> 所有历史项目文件都已经完全迁入对象存储。

只有 M3G 以后，按项目完成 NAS 侧 MinIO 副本升级并验收后，才能逐步对外宣称对应项目已经对象存储化。

## 5. 验收重点

1. 新上传文件默认 `OBJECT_STORED`。
2. 新上传文件可通过受控 `file-access` 下载 / 预览。
3. 历史 NAS 文件继续可用。
4. 对象存储不可用时返回业务错误或明确 fallback 状态。
5. API / 前端不返回真实 NAS 路径、bucket、object key、storage URI。
6. M3E / M3D / M3C / M3B / M3A / file-access 回归通过。

## 6. 当前交接文件

- 开发 prompt：`handoff/dev-agent/current-prompt.md`
- 测试 prompt：`handoff/test-agent/current-prompt.md`
- 总任务图：`handoff/main-agent/m3-storage-evidence-chain-todo.md`

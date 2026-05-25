# M3B：105 小样本对象存储镜像迁移计划

时间：2026-05-25

## 1. 批次定位

`M3B` 是 M3 对象存储路线的第二批，承接已收口的 `M3A：StorageService 与对象存储模型基线`。

本批只做 105 项目少量安全样本文件的对象存储镜像迁移，目标是验证：

`NAS 原文件保留 -> 读取 NAS 文件 -> 计算/复用 checksum -> 上传 MinIO -> 校验对象 -> 写入对象版本记录 -> 标记 OBJECT_STORED -> 仍通过受控 file-access 访问`

本批不是全量 NAS 搬迁，不做语义解析，不做 Hermes 正文问答。

## 2. 当前基础

M3A 已完成：

- `data_storage_objects`
- `data_file_object_versions`
- `data_object_migration_tasks`
- `data_file_derivatives`
- `data_preview_artifacts`
- `StorageService`
- provider health
- file storage status
- MinIO 受控测试读取

M3B 应在这些能力上继续，而不是重新设计存储底座。

## 3. M3B 必做

### 小样本迁移任务

新增 105 项目小样本迁移能力：

- 只允许按明确的 `fileIds` 或严格受控筛选条件创建迁移任务。
- 默认限制样本数量和单文件大小，避免误触全量迁移。
- 迁移任务必须记录：
  - 项目
  - 文件数量
  - 状态
  - 进度
  - 成功数
  - 失败数
  - 跳过数
  - 失败原因
  - 创建人
  - 更新时间

### 镜像流程

每个文件迁移流程必须固定为：

1. 校验项目权限。
2. 校验文件属于当前项目。
3. 校验文件生命周期未删除、未进回收站。
4. 校验当前文件仍是 NAS 源文件或可读源。
5. 计算或复用 checksum。
6. 上传 MinIO / S3-compatible。
7. 校验 etag / checksum / size。
8. 写入 `data_storage_objects`。
9. 写入 `data_file_object_versions`。
10. 标记文件为 `OBJECT_STORED` 或等价状态。
11. 写审计事件。

### 幂等与安全

- 重跑同一个文件不得重复产生脏对象记录。
- 同 checksum + 同 size 可复用对象或标记 duplicate/reference，具体实现可按项目最小方案处理，但必须可解释。
- 同名不同文件不能被误判为同一对象。
- 文件迁移失败必须保留失败原因。
- 迁移任务可重试。
- NAS 原文件必须保留，不移动、不删除、不改名。

### API

新增或补齐 M3B 接口：

- `POST /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/projects/{projectId}/storage-migration-tasks`
- `GET /api/data-steward/storage-migration-tasks/{taskId}`
- `POST /api/data-steward/storage-migration-tasks/{taskId}:retry`

接口响应不得返回真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret。

## 4. 禁止事项

本批严禁：

- 不做全量 NAS 迁移。
- 不移动、删除、重命名真实 NAS 文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
- 不新增 Hermes 正文问答。
- 不启动真实 BIM 轻量化。
- 不把对象存储迁移说成“语义理解完成”。
- 不让前端获得真实 NAS 路径、bucket、object key、storage URI。
- 不绕过现有 file-access 权限、审计和生命周期控制。

## 5. 验收口径

M3B 收口必须满足：

- 105 小样本迁移任务可创建。
- 迁移后 storage status 能显示 `OBJECT_STORED`。
- 重跑迁移不重复污染对象记录。
- 失败场景能记录失败原因。
- 迁移后文件仍可通过受控 `file-access` 访问。
- NAS 原文件没有移动、删除、重命名。
- 禁出字段扫描通过。
- M3B 专项脚本通过。
- M3A / M2J / M2I / M2H / M2F / file-access 回归通过。
- 后端构建、前端构建、健康检查、`git diff --check` 通过。

## 6. 下一步

M3B 完成后才考虑进入：

`M3C：对象存储迁移任务中心与批量策略`

M3C 才允许讨论更大范围的项目 / 目录 / 类型批量迁移策略。

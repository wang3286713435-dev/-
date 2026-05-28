# M3G-4 受控多项目小批对象化执行计划

创建时间：2026-05-28

## 1. 批次定位

`M3G-4：受控多项目小批对象化执行`

M3G-3 已经完成多真实项目对象化 dry-run 规划。本批开始从“只读规划”进入“受控小批真实执行”。

本批不是全量迁移，也不是正式把所有项目一次性对象化。目标是验证多项目、小数量、有限容量、可审计、可幂等的真实对象化执行闭环。

## 2. 本批目标

- 基于 M3G-3 的 dry-run 计划，支持人工确认后生成受控执行。
- 一次只允许少量真实项目、少量文件进入对象化。
- 继续保证 NAS 原文件不移动、不删除、不改名、不覆盖。
- 已对象化文件默认从 NAS 侧 MinIO 读取。
- 未对象化文件继续保持 `NAS_ONLY` 并走原 NAS 台账链路。
- 任务和结果必须可追踪、可重试、可审计、可解释。

## 3. 建议安全上限

开发实现必须在后端强制上限，不只依赖前端。

默认上限：

- 单次最多 `3` 个真实项目。
- 单项目最多 `3` 个文件。
- 总文件数最多 `9` 个。
- 单项目容量最多 `50MB`。
- 总容量最多 `100MB`。
- 只允许 `realNasProject=true` 的项目。
- 必须要求 `confirmed=true`。

如后续需要扩大范围，必须由主 agent 单独启动新批次或明确修改上限。

## 4. 允许范围

允许：

- 新增多项目对象化执行接口。
- 复用或扩展现有对象迁移任务中心。
- 记录任务来源为 `MULTI_PROJECT_CONTROLLED_EXECUTION`。
- 按 dry-run 条件选择小批文件。
- 将选中文件复制副本到 NAS 侧 MinIO。
- 写入 `data_storage_objects` 与 active `data_file_object_versions`。
- 更新文件存储状态为 `OBJECT_STORED`。
- 对重复执行返回幂等跳过。
- 前端在文件服务页提供“小批执行”入口和明确风险提示。
- 新增 M3G-4 专项脚本。

## 5. 禁止范围

禁止：

- 全量迁移所有项目。
- 迁移 NAS 根目录。
- 迁移测试 / 样例 / 归档项目。
- 默认执行真实迁移。
- 未确认执行。
- 超过后端硬上限。
- 移动、删除、重命名、覆盖真实 NAS 文件。
- 静默 fallback：对象版本已存在但对象读取失败时，不得假装从 NAS 读取成功。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- Hermes 正文问答。
- BIM 引擎、parser、indexing。
- 暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
- 修改 `docs/**`。

## 6. 后端接口建议

可新增：

```http
POST /api/data-steward/storage-objectification-plans:execute
```

请求建议：

- `projectIds`
- `realProjectsOnly=true`
- `storageState=NAS_ONLY`
- `checksumState`
- `extensions`
- `minSizeBytes`
- `maxSizeBytes`
- `maxTotalBytes`
- `maxFilesPerProject`
- `maxBytesPerProject`
- `concurrencyLimit`
- `rateLimitBytesPerMinute`
- `confirmed=true`

响应建议：

- `executionStarted`
- `dryRun=false`
- `taskSource=MULTI_PROJECT_CONTROLLED_EXECUTION`
- `selectedProjectCount`
- `selectedFileCount`
- `selectedTotalBytes`
- `createdTaskIds`
- `projectResults`
- `createdCount`
- `skippedCount`
- `failedCount`
- `failureReasons`
- `warnings`

必须注意：

- `confirmed` 不是 `true` 时必须拒绝。
- 超过项目数、文件数、容量上限时必须拒绝。
- 项目不是真实 NAS 项目时必须拒绝或自动排除并返回原因。
- 响应不得返回底层路径或对象 key。

## 7. 前端要求

在文件服务页现有“多项目对象化规划”基础上增加：

- “确认执行小批对象化”入口。
- 明确提示：
  - 只复制文件副本到 NAS 侧 MinIO。
  - 不移动、不删除、不改名 NAS 原文件。
  - 执行范围受项目数、文件数、容量限制。
  - 执行后可在任务中心查看结果。
- 展示执行摘要：
  - 项目数。
  - 文件数。
  - 总容量。
  - 创建任务数。
  - 成功 / 跳过 / 失败数量。
- 对象存储不可用时禁用执行按钮。

## 8. 专项脚本

新增：

`scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

脚本允许做真实小批对象化，但必须强限制：

- 自动选择最多 `2-3` 个真实项目。
- 每项目最多 `1` 个文件。
- 总文件数最多 `3` 个。
- 总容量限制建议不超过 `100MB`。
- 必须验证 `confirmed=false` 被拒绝。
- 必须验证 `confirmed=true` 才执行。
- 必须验证重复执行幂等跳过。
- 必须验证已对象化文件可通过受控 `file-access` 读取。
- 必须验证未对象化文件仍可走 NAS_ONLY 链路。
- 必须验证禁出字段。

## 9. 验收标准

- 后端构建、前端构建、健康检查通过。
- M3G-4 专项通过。
- M3G-3 / M3G-1 / M3F / M3E / M3C / file-access 回归通过。
- 未确认执行被拒绝。
- 超限执行被拒绝。
- 小批真实对象化执行成功。
- 重复执行幂等。
- 已对象化文件显示 `OBJECT_STORED`，并通过受控 `file-access` 读取。
- NAS 原文件未被移动、删除、重命名、覆盖。
- 响应禁出字段扫描通过。

## 10. 完成定义

M3G-4 完成后，平台应具备：

> 基于 dry-run 计划，对少量真实项目进行受控、多项目、小批历史文件对象化执行的能力。

它仍不代表：

- 已全量对象化公司项目。
- 已完成语义解析。
- Hermes 已能正文问答。
- BIM / CAD / RVT 已被深度理解。

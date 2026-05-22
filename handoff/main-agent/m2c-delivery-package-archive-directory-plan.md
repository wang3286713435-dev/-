# M2C：交付包与档案目录能力计划

## 1. 批次定位

`M2C` 是 UX1/UX2/UX3 合并回主线后的第一个平台本体能力批次。

本批目标不是继续优化界面，也不是进入 BIM 引擎或 Hermes，而是把当前已有的 `导出预检查 dry-run` 推进为可被项目负责人理解和验收的 `交付包草案 / 档案目录`。

## 2. 当前前置状态

- `main` 已合并 UX3。
- M1C / M1D / M1E / M1F / M2A / M2B 均已通过回归。
- 真实 NAS 写操作仍处于项目级灰度控制下。
- 8B / 8C / 9A 尚未进入。
- Hermes 继续冻结，不作为 M2C 主线能力。

## 3. M2C 目标

让用户能在当前项目内生成一份可审计、可解释、可导出的交付成果清单：

- 哪些交付项已经具备交付条件。
- 哪些交付项缺文件。
- 哪些交付项待审核或已驳回。
- 哪些交付项因为预览 / 转换 / 元数据问题被阻塞。
- 交付包内建议的档案目录结构是什么。
- 当前生成的是清单 / 草案，不复制、不移动、不打包真实 NAS 文件。

## 4. 本批允许

- 新增或补充后端只读 / 草案型交付包接口。
- 新增交付包草案、交付包条目等数据库表，必须追加 Flyway 迁移，不改旧迁移。
- 在项目工作台 / 交付工作中心增加 `交付包 / 档案目录` 页面或区域。
- 支持保存交付包草案、查看历史草案、导出清单文件。
- 支持 CSV / XLSX / JSON 等清单导出；导出内容不得包含真实 NAS 绝对路径。
- 写审计日志：生成草案、导出清单、查看详情等关键动作。

## 5. 本批禁止

- 不复制真实 NAS 文件。
- 不移动、删除、重命名真实 NAS 文件。
- 不生成真实压缩包。
- 不开放永久删除。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不接入真实 BIM 引擎。
- 不新增 Hermes 能力。
- 不写 OpenSearch / Qdrant / MinIO documents/chunks。
- 不泄露 `storage_uri`、真实 NAS 路径、raw row、SQL、token、secret。

## 6. 推荐实现拆分

### M2C-1：交付包准备接口收束

- 复用 7A export-precheck 和 6B delivery-package 既有口径。
- 输出交付包准备摘要：
  - totalCount
  - readyCount
  - blockedCount
  - missingCount
  - pendingReviewCount
  - rejectedCount
  - conversionRequiredCount
  - unsupportedPreviewCount
- 输出条目：
  - targetType
  - targetName
  - deliverableDefinitionName
  - deliverableTypeName
  - fileId
  - fileName
  - fileKind
  - version
  - reviewStatus
  - previewStatus
  - exportStatus
  - blockReason
  - archiveDirectoryPath

### M2C-2：交付包草案保存

- 新增草案记录。
- 用户点击 `生成交付包草案` 后，只保存清单快照。
- 草案必须标记：
  - dryRun=true
  - physicalPackageGenerated=false
  - nasFileCopied=false
- 草案详情可查看条目和阻塞原因。

### M2C-3：档案目录 / 清单导出

- 根据项目、交付类型、目标对象生成档案目录。
- 支持导出清单，不导出真实文件。
- 导出清单字段必须脱敏。
- 前端文案明确：这是交付清单，不是物理文件包。

## 7. 验收标准

- 105/503 与 93/506 或另一个真实 NAS 项目均可打开交付包页面。
- 能生成交付包草案。
- 草案中能区分可交付、缺失、待审核、已驳回、需转换、暂不支持预览。
- 档案目录路径是项目内交付目录，不是真实 NAS 路径。
- 导出清单不包含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、`storage_path`、raw row、SQL。
- 未生成真实压缩包。
- 未复制真实 NAS 文件。
- 未触发真实 NAS 写操作。
- 现有 M2B / M2A / M1F / M1E / M1D / M1C 回归通过。

## 8. 主 agent 裁决

M2C 可以启动。

真实 BIM 引擎、Hermes 增强和客户交付部署继续后置。

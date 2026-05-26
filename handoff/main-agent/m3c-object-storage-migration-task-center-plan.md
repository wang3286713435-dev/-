# M3C 计划：对象存储迁移任务中心与批量策略

时间：2026-05-26 CST

## 1. 批次定位

`M3C：对象存储迁移任务中心与批量策略`

本批是在 M3A / M3B / M3C-1 基础上，把“小样本迁移接口”补成平台员工可理解、管理员可控制、测试可回归的任务中心。

本批不是全量 NAS 搬迁，不是 Hermes 正文问答，不是语义索引，不是 BIM/parser。

## 2. 已有基础

- M3A：对象存储表、StorageService、provider health、storage-status、受控 file-access 已完成。
- M3B：显式 fileIds 小样本迁移、对象校验、对象版本记录、幂等和重试已完成。
- M3C-1：`assetUuid`、FileAssetView / ModelAssetView、单文件 storage status 口径已完成。

## 3. 本批目标

建立可用的迁移任务中心：

- 管理员能创建受控迁移任务。
- 管理员能查看任务列表、详情、行级结果。
- 管理员能重试失败/可重试任务。
- 任务中心能解释成功、失败、跳过、已对象化、不可迁移原因。
- 前端不暴露底层对象定位。
- 批量策略受控：项目灰度、数量上限、大小上限、生命周期限制、权限限制。

## 4. 后端范围

优先复用现有 M3B：

- `StorageMigrationApplicationService`
- `StorageMigrationController`
- `data_object_migration_task_batches`
- `data_object_migration_tasks`
- `data_storage_objects`
- `data_file_object_versions`
- `StorageService`

允许追加新 Flyway 迁移，补充任务中心需要的字段，例如：

- task display name / source。
- created_by / requested_by 展示补齐。
- progress / retry_count / last_error_code。
- policy snapshot / dryRun flag。

如果现有表已足够，不强行新增迁移。

## 5. 前端范围

新增或增强对象存储迁移任务中心页面，建议入口：

`/data-steward/storage-migration`

或作为文件服务 / 存储服务页面中的 tab。

页面应包含：

- 任务列表。
- 任务详情抽屉。
- 行级结果表。
- 创建迁移任务入口。
- 选择项目内文件的受控入口。
- 重试失败任务。
- storage status 汇总。

文案必须明确：

- NAS 原文件保留。
- 当前只做对象存储镜像。
- 不生成语义证据。
- 不代表文件内容已被 Hermes 理解。
- 不暴露底层路径、bucket、object key。

## 6. 策略边界

任务创建必须受控：

- 只能显式选择 `fileIds`。
- 文件必须属于当前项目。
- 用户必须有项目权限和迁移权限。
- 单次数量上限保守，默认沿用 M3B 的 10 个，可配置但不得无限制。
- 单文件大小上限保守，可配置。
- 已删除、回收站、路径不可读文件必须拒绝或跳过并给业务原因。
- 已有 active 对象版本的文件应返回 `ALREADY_STORED` / `SKIPPED`，不得重复污染 active 版本。

## 7. 禁止事项

严禁：

- 全量 NAS 搬迁。
- 目录一键全量迁移。
- 自动扫描并迁移整个项目。
- 移动、删除、重命名真实 NAS 文件。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
- Hermes 正文问答。
- BIM 轻量化或 parser。
- 暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、SQL、raw row、token、secret。
- 修改仓库 `docs/**`。

## 8. 验收重点

- 任务中心可创建任务。
- 任务列表可展示。
- 任务详情可展示行级结果。
- 成功 / 失败 / 跳过 / 已对象化原因可解释。
- 重试逻辑可用，且幂等。
- storage-status 与任务结果一致。
- `assetUuid` 在任务行、详情和前端展示中可见。
- 禁出字段扫描通过。
- M3A / M3B / M3C-1 回归通过。
- 真实 NAS 文件不被改动。

## 9. 专项脚本

新增：

`scripts/dev/check-m3c-storage-migration-task-center.sh`

脚本至少覆盖：

- 登录和项目切换。
- 创建隔离临时文件资源。
- 创建迁移任务。
- 查询任务列表。
- 查询任务详情。
- 校验行级 `assetUuid`、`storageState`、`resultCode`。
- 重复迁移幂等。
- 失败文件任务和重试。
- forbidden-field scan。
- 本轮新增关键文件已纳入 Git 跟踪。

## 10. 完成定义

同时满足：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- M3C 专项脚本通过。
- M3B / M3A / M3C-1 回归通过。
- M2H / file-access 回归通过。
- `git diff --check` 通过。
- 报告写入 `handoff/dev-agent/latest-report.md`。
- 无真实 NAS 写破坏。
- 无底层路径/对象定位泄露。

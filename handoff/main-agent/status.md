# 主 Agent 项目状态

## 2026-05-27 M3F 启动：新文件对象存储优先写入

- 用户确认：先做 `M3F`，后续再做 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`。
- 当前分支：`codex/m3f-object-storage-first-write`。
- 当前 active 批次：`M3F：新文件对象存储优先写入与 NAS 兼容回退`。
- M3F 定位：
  - 让后续通过平台上传的新文件优先进入对象存储。
  - 新文件仍写入 MySQL 业务台账，并生成 `assetUuid`。
  - 新文件应写入 `data_storage_objects` 与 active `data_file_object_versions`。
  - 新文件 `storage-status` 应显示 `OBJECT_STORED`。
  - 预览 / 下载仍走受控 `file-access`。
- M3F 不做：
  - 不做全量历史 NAS 项目迁移。
  - 不移动、删除、重命名真实 NAS 文件。
  - 不读取文件正文。
  - 不进入 Hermes 正文问答。
  - 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 不接入真实 BIM 引擎。
- 后续批次裁决：
  - `M3G：NAS 侧 MinIO 对象存储接管真实项目文件` 才负责将对象存储主链路切换到 NAS 侧 MinIO，并按项目 / 目录 / 文件类型 / 大小范围分批对象化历史真实项目。
- 已写入：
  - M3F 计划：`handoff/main-agent/m3f-object-storage-first-write-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
  - 任务图更新：`handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 当前裁决：
  - 交给开发 agent 按 M3F prompt 执行。
  - M3F 通过前，不进入 M3G、M4A 或 Hermes 正文问答。

## 2026-05-27 M3F 正式收口

- 测试 agent 已完成 M3F 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
  - `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md` 随 M3F checkpoint 一并提交，作为后续路线文件，不代表 M3G 已启动。
- 已确认：
  - 新上传文件默认写入对象存储。
  - 新上传文件生成 `assetUuid`。
  - 新上传文件创建 active object version。
  - 新上传文件 `storage-status=OBJECT_STORED`。
  - 新上传文件可通过受控 `file-access` 读取。
  - 对象存储不可用时 fail-closed，不静默回退真实 NAS。
  - M3E / M3D / M3C / M3B / M3A / file-access 回归通过。
  - 未新增 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、BIM 引擎。
  - 未修改 `docs/**`。
- 主 agent 裁决：`M3F：新文件对象存储优先写入与 NAS 兼容回退` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步候选：
  - `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`。
  - M3G 需要单独启动，不自动进入。

## 2026-05-27 M3G-1 启动：NAS 侧 MinIO readiness 与对象化盘点

- M3F 已合并回 `main` 并推送，`main` 最新进度为 M3F。
- 已从最新 `main` 创建分支：
  - `codex/m3g-nas-minio-real-project-object-storage`
- 当前 active 批次：
  - `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`
- M3G 总目标：
  - 将正式文件本体治理升级为 NAS 侧 MinIO 对象存储。
  - 原 NAS 项目资料区冻结为只读备份和回滚来源。
  - 平台通过 MySQL 台账管理业务关系、权限、版本、checksum 和交付关系。
  - Hermes 后续只能通过平台授权，从 NAS 侧 MinIO 复制文件副本到本机工作区解析。
- M3G-1 只做：
  - NAS 侧 MinIO readiness。
  - 全项目对象化覆盖率盘点。
  - 单项目对象化 dry-run 计划。
- M3G-1 不做：
  - 不执行真实历史文件批量迁移。
  - 不移动、删除、重命名真实 NAS 文件。
  - 不做 Hermes 正文问答。
  - 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 已写入：
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
  - M3G 计划：`handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md`
  - M3G-1 任务图：`handoff/main-agent/m3g1-task-graph.md`
  - NAS 侧 MinIO 配置交接：`handoff/main-agent/m3g1-nas-minio-ops-preparation.md`
- 当前 NAS 侧 MinIO 探测：
  - 初次探测时 `192.168.1.181:9000/9001` 未响应。
  - 用户已在 Synology NAS 上部署并启动 MinIO。
  - 当前 `http://192.168.1.181:9000/minio/health/ready` 可达。
  - 当前 `9001` MinIO Console 可达。
  - bucket `zy-datahub-assets-prod` 已创建。
  - 用户已在本机终端临时注入 NAS MinIO 环境变量并重启平台后端，后端健康检查 `UP`。
  - 注意：当前为本机会话级环境变量注入，不代表生产持久化部署完成；开发 agent 仍需实现 readiness，明确区分 `LOCAL_DEV_MINIO` 与 `NAS_SIDE_MINIO`。
- 当前裁决：
  - M3G-1 已于 2026-05-27 正式收口。
  - 当前 active 批次：`M3G-2：105 项目历史文件对象化上传灰度`。
  - 本批只针对 105 / `projectId=503` 小批量执行真实历史文件对象化。
  - 其他项目和全量迁移继续等待后续批次。

## 2026-05-27 M3G-1 正式收口

- 测试 agent 已完成 M3G-1 P1 修复复验，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 已确认：
  - readiness 为 `NAS_SIDE_MINIO / READY`。
  - 全项目对象化覆盖率盘点可查。
  - 503 / 105 单项目 dry-run 可生成计划。
  - dry-run 不创建真实迁移任务、不复制文件、不修改 NAS。
  - M3E 预览产物对象化回归已恢复。
  - M3F 对象存储优先上传脚本已适配 NAS 侧 MinIO。
  - M3C 与 file-access 回归通过。
  - 未执行历史文件真实批量迁移。
  - 未移动、删除、重命名或覆盖真实 NAS 原项目文件。
  - 未新增 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、真实 BIM 引擎。
- 主 agent 裁决：`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划` 正式收口。

## 2026-05-25 M3A 启动：对象存储与 StorageService 基线

- 用户确认执行 `M3 执行计划：对象存储底座先行，语义与 Hermes 后置`。
- 主 agent 裁决：当前 active 批次切换为 `M3A：对象存储与 StorageService 基线`。
- M3A 定位：
  - 只建立对象存储元数据模型和统一 `StorageService` 适配层。
  - 现有 NAS 文件仍是源头和主链路。
  - MySQL 继续作为业务台账、权限、交付、审计中心。
  - MinIO / S3-compatible 只做基础接入和受控读取能力，不做全量迁移。
- M3A 必做：
  - 追加 Flyway 迁移，新增对象存储相关表。
  - 新增 StorageService。
  - 现有 `file-access` 预览 / 下载内部改走 StorageService，但外部契约保持兼容。
  - 新增 provider health 和 file storage status 只读接口。
  - 新增 `scripts/dev/check-m3a-storage-service-foundation.sh`。
- M3A 禁止：
  - 不做全量 NAS 迁移。
  - 不移动、删除、重命名真实 NAS 文件。
  - 不读取 PDF / Office / DWG / RVT / IFC 正文。
  - 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
  - 不新增 Hermes 正文问答。
  - 不启动真实 BIM 轻量化。
  - 不暴露真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret。
- 已写入：
  - M3A 计划：`handoff/main-agent/m3a-storage-service-foundation-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- 当前裁决：
  - M3A 已于 2026-05-25 收口。
  - M3B / M3C / M3D / M4 / M5 均未进入。

## 2026-05-25 M3A 正式收口

- 测试 agent 已完成 M3A 完整验收和 P1 极短复验，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 已确认：
  - 后端构建、前端构建、健康检查通过。
  - `scripts/dev/check-m3a-storage-service-foundation.sh` 通过，`PASS=8 FAIL=0`。
  - M2J / M2I / M2H / M2F / M2B / file-access 回归通过。
  - provider health、storage-status、MinIO 受控读取和 NAS 既有预览 / 下载链路可用。
  - 禁出字段扫描通过，未发现真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret 泄露。
  - M3A 核心新增文件已纳入 Git 暂存。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 未被纳入暂存。
- 主 agent 裁决：`M3A：对象存储与 StorageService 基线` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 先提交 / 推送 M3A。
  - 后续如继续 M3 路线，进入 `M3B：105 小样本对象存储镜像迁移`。

## 2026-05-25 M3B 启动：105 小样本对象存储镜像迁移

- 用户确认进入 `M3B：105 小样本对象存储镜像迁移`。
- 当前分支：`codex/m3b-object-storage-mirror-trial`。
- M3B 定位：
  - 只做 105 项目少量安全样本文件的对象存储镜像迁移。
  - 验证 NAS 原件保留、MinIO 镜像上传、对象校验、对象版本记录和受控访问闭环。
  - 不做全量 NAS 搬迁。
  - 不做语义解析、Hermes 正文问答或真实 BIM 引擎接入。
- 已写入：
  - M3B 计划：`handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- 当前 active 批次：`M3B：105 小样本对象存储镜像迁移`。
- 下一步：
  - 交给开发 agent 按 M3B prompt 执行。
  - 主 agent 负责监控、审计、纠偏和收口判断。

## 2026-05-26 M3B 正式收口

- 测试 agent 已完成 `M3B：105 小样本对象存储镜像迁移` 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 已确认：
  - M3B 专项脚本通过，`PASS=11 FAIL=0`。
  - M3A / M2J / M2I / M2H / M2F / file-access 回归全部通过。
  - 小样本迁移任务可创建。
  - 单次 10 个文件上限生效。
  - 跨项目 / 删除文件 / 路径不可读文件被拒绝。
  - 迁移后 `storage-status` 显示 `OBJECT_STORED / MINIO`。
  - 重跑同一文件返回 `ALREADY_STORED`，不重复污染对象版本。
  - 迁移后受控 `file-access` 可读取 MinIO 镜像。
  - API 响应未泄露真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret。
  - 未发现全量 NAS 迁移、真实 NAS 改动、正文解析、Hermes 正文问答或 BIM 引擎越界。
- 主 agent 裁决：`M3B：105 小样本对象存储镜像迁移` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 先提交 / 推送 M3B。
  - 后续如继续 M3 路线，进入 `M3C：对象存储迁移任务中心与批量策略`。

## 2026-05-26 M3-M5 任务图冻结

- 用户提供 `NAS 台账治理升级为对象存储与 Hermes 语义证据链` 路线。
- 主 agent 已将路线落为任务图 TODO：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 后续 M3-M5 批次规划以该任务图为准。
- 当前已勾选：
  - `M3A：对象存储与 StorageService 基线`
  - `M3B：105 小样本对象存储镜像迁移`
- 下一步候选：
  - `M3C-0：资产存储与证据链契约冻结`
  - `M3C-1：资产 UUID 与存储状态统一`
  - `M3C：对象存储迁移任务中心与批量策略`
- 当前裁决：
  - 不直接进入 Hermes 正文问答。
  - 不把对象存储迁移等同于语义理解完成。
  - 不修改 `docs/**`。

## 2026-05-26 M3C-0 契约冻结完成

- `M3C-0：资产存储与证据链契约冻结` 已完成。
- 已同步共享文档：
  - `DigitalDeliveryProject/integration-contracts/asset_storage_evidence_chain_contract.md`
  - `DigitalDeliveryProject/adr/ADR-007-object-storage-evidence-chain-boundary.md`
  - `DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
  - `DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
  - `DigitalDeliveryProject/docs/01_capability_matrix.md`
- 本仓库同步更新：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
  - `handoff/main-agent/m3c-0-storage-evidence-contract-freeze-closure.md`
- 当前下一步建议：
  - 优先进入 `M3C-1：资产 UUID 与存储状态统一`。
  - 然后进入 `M3C：对象存储迁移任务中心与批量策略`。
- 当前仍禁止：
  - Hermes 正文问答。
  - 向量库写入。
  - 全量 NAS 搬迁。
  - 暴露 raw NAS path / bucket / object key / storage_uri。

## 2026-05-26 M3C-1 启动：资产 UUID 与存储状态统一

- 用户确认按任务图执行下一步。
- 当前 active 批次：`M3C-1：资产 UUID 与存储状态统一`。
- 已写入：
  - `handoff/main-agent/m3c1-asset-uuid-storage-status-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 本批定位：
  - 为 `data_file_resources` 建立稳定 `asset_uuid`。
  - 保留数值 `fileId` 作为内部主键和排障 ID。
  - 让 FileAssetView / ModelAssetView / API 能输出安全 `assetUuid`。
  - 统一单文件 storage status 为 `NAS_ONLY / MIGRATION_PENDING / OBJECT_STORED / MIGRATION_FAILED`。
- 本批禁止：
  - 全量 NAS 迁移。
  - 迁移任务中心。
  - Hermes 正文问答。
  - documents / chunks / 向量库 / 搜索索引。
  - 修改仓库 `docs/**`。

## 2026-05-26 M3C-1 正式收口

- 测试 agent 已完成 `M3C-1：资产 UUID 与存储状态统一` 验收和 P1 极短复核，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 已确认：
  - `data_file_resources.asset_uuid` 已新增、回填并唯一。
  - 新建文件入口由后端生成 `assetUuid`，数据库默认值兜底。
  - `FileAssetView` / `ModelAssetView` 已输出 `asset_uuid`。
  - 文件资源、文件资产、catalog list/detail/search、storage-status、M3B 迁移行已输出 `assetUuid`。
  - 前端主展示改为“平台资产ID”，内部数字 ID 保留为诊断信息。
  - 单文件 storage status 统一为 `NAS_ONLY / MIGRATION_PENDING / OBJECT_STORED / MIGRATION_FAILED`。
  - M3C-1 专项脚本通过，`PASS=15 FAIL=0`。
  - M3B / M3A / M2J / M2I / M2H / M2F / file-access 回归通过。
  - 禁出字段扫描通过，未发现真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret 泄露。
  - 原 P1：V30 迁移和 M3C-1 专项脚本未跟踪，已复核为 `A` 且 `git ls-files` 可识别。
- 主 agent 裁决：`M3C-1：资产 UUID 与存储状态统一` 正式收口。

## 2026-05-26 M3C 启动：对象存储迁移任务中心与批量策略

- 用户确认继续下一步。
- 当前 active 批次：`M3C：对象存储迁移任务中心与批量策略`。
- 已写入：
  - `handoff/main-agent/m3c-object-storage-migration-task-center-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 本批定位：
  - 基于 M3A / M3B / M3C-1，把对象存储小样本迁移补成受控任务中心。
  - 提供任务创建、列表、详情、行级结果、重试、批量策略、幂等和审计。
- 本批禁止：
  - 全量 NAS 搬迁。
  - 目录一键全量迁移。
  - Hermes 正文问答。
  - documents / chunks / 向量库 / 搜索索引。
  - parser / BIM 轻量化。
  - 修改仓库 `docs/**`。

## 2026-05-26 M3C 正式收口：对象存储迁移任务中心与批量策略

- 测试 agent 已完成 M3C 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk warning。
  - 非交付未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**` 继续排除。
  - 页面安全说明中出现 `bucket / object key` 字面词，但未泄露真实底层定位值。
- 已确认：
  - 后端构建、前端构建、健康检查通过。
  - M3C 专项脚本通过，`PASS=9 FAIL=0`。
  - M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
  - 任务可创建、列表可查、详情可查、失败可重试。
  - 任务详情行级结果返回 `assetUuid` 和对象化状态。
  - 重复迁移幂等跳过，不重复污染 active 对象版本。
  - 未发现真实 NAS 文件被移动、删除、重命名或改写。
  - 未新增 Hermes 正文问答。
  - 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未修改仓库 `docs/**`。
- 主 agent 裁决：`M3C：对象存储迁移任务中心与批量策略` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 先提交 / 推送 M3C。
  - 后续如继续 M3 路线，进入 `M3D：真实 NAS 小范围灰度镜像`。

## 2026-05-26 M3C 合并主线并启动 M3D

- 已将 `codex/m3c-storage-migration-task-center` 快进合并回 `main`。
- 已推送 `main`，当前主线最新进度为 M3C。
- 已从最新 `main` 新开分支：
  - `codex/m3d-real-nas-object-mirror-gray`
- 当前 active 批次：`M3D：真实 NAS 小范围灰度镜像`。
- 已写入：
  - `handoff/main-agent/m3d-real-nas-object-mirror-gray-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 本批定位：
  - 使用 105 / 503 少量真实业务文件验证对象存储镜像灰度。
  - 覆盖 PDF / DWG / RVT 或模型类小样本。
  - NAS 原文件保留。
  - file-access 继续作为受控访问入口。
- 本批禁止：
  - 全量 NAS 搬迁。
  - 目录一键迁移。
  - Hermes 正文问答。
  - documents / chunks / 向量库 / 搜索索引。
  - parser / BIM 轻量化。
  - 修改仓库 `docs/**`。

## 2026-05-26 M3D 正式收口：真实 NAS 小范围灰度镜像

- 测试 agent 已完成 M3D 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk warning。
  - 非交付未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**` 继续排除。
  - `handoff/main-agent/m3d-real-nas-object-mirror-gray-plan.md` 建议纳入本批提交。
- 已确认：
  - 后端构建、前端构建、健康检查通过。
  - M3D 专项脚本通过，`PASS=19 FAIL=0`。
  - M3C / M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
  - 105 / 503 真实业务文件 PDF / DWG / RVT 样本均覆盖。
  - 三份样本均为 `OBJECT_STORED`。
  - checksum 覆盖率 `3/3`。
  - file-access 可读取对象镜像。
  - NAS 原文件未被移动、删除、改名或写入。
  - 未新增 Hermes 正文问答。
  - 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未启动 BIM parser 或真实轻量化。
  - 未修改仓库 `docs/**`。
- 主 agent 裁决：`M3D：真实 NAS 小范围灰度镜像` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 先提交 / 推送 M3D。
  - 后续如继续 M3 路线，进入 `M3E：预览与转换产物对象化`。

## 2026-05-23 M2H 开发完成，进入测试验收

- 开发 agent 已完成 `M2H：Windows 风格文件管理器交互升级`，报告写入 `handoff/dev-agent/latest-report.md`。
- 本批定位：
  - 文件管理器核心交互升级。
  - 让文件 / 文件夹操作更接近 Windows 文件资源管理器。
  - 不新增后端能力，不扩大 NAS 写边界，不接入真实 BIM 引擎。
- 已实现内容：
  - 文件夹 + 文件统一列表，文件夹排在文件前。
  - 左键单选、Ctrl / Command 多选、Shift 连选、空白处取消选择。
  - 右键上下文菜单，按文件 / 文件夹 / 多选动态展示操作。
  - 双击文件夹进入目录。
  - 双击 PDF / 图片走受控预览票据。
  - Office / CAD 走现有预览状态。
  - RVT / IFC / NWD / NWC / GLB / GLTF 等模型文件打开“模型预览占位”，不伪装成真实渲染。
  - 多选批量移动、批量移入回收站、批量下载入口清单。
  - 批量下载只逐个创建 DOWNLOAD ticket，不生成 ZIP，不复制 NAS 文件。
- 主 agent 初审通过：
  - 前端构建通过，仅既有 Vite chunk warning。
  - 健康检查通过。
  - M2H 专项脚本通过，`PASS=23 FAIL=0`。
  - M2G 回归通过，`PASS=26 FAIL=0`。
  - M2B 回归通过，`PASS=18 FAIL=0`。
  - 文件访问安全回归通过，`PASS=18 FAIL=0`。
  - `git diff --check` 通过。
- 当前确认：
  - 未修改 `backend/**`、`docs/**`、数据库迁移。
  - 未新增 Hermes / BIM / parser / indexing / 文件正文读取能力。
  - 未开放永久删除。
  - 未执行真实业务目录写操作。
  - 未发现 raw NAS path / `storage_uri` 泄露。
- 当前裁决：
  - M2H 可以进入测试 agent 轻量验收。
  - M2H 尚未收口，等待测试报告。
  - 非交付未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**` 继续排除。

## 2026-05-23 M2H 测试不通过，进入 P1 修复

- 测试 agent 已完成 `M2H：Windows 风格文件管理器交互升级` 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：不通过。
- 当前 P0：无。
- 当前 P1：
  - `P1-1 PDF 双击受控预览未通过`。
  - 在 `503` 文件管理页搜索 `pdf` 后双击 PDF 文件，未打开受控预览入口，并出现前端事件错误 `cancel`。
  - 模型文件双击占位正常，文件夹双击进入目录正常。
- 当前 P2：
  - 既有 Vite chunk warning。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 仍是未跟踪文件，收口前需纳入 Git。
- 已确认：
  - M2H / M2G / M2B / 文件访问安全自动化脚本均通过。
  - 未发现 P0、安全红线或后端越界。
- 主 agent 裁决：
  - 暂不收口 M2H。
  - 已将 `handoff/dev-agent/current-prompt.md` 更新为 `M2H P1 修复 PDF / 图片双击受控预览入口`。
  - 修复后只做极短复验。

## 2026-05-23 M2H P1 修复完成，进入极短复验

- 开发 agent 已完成 `M2H P1：PDF / 图片双击受控预览入口修复`，报告写入 `handoff/dev-agent/latest-report.md`。
- 修复内容：
  - 双击 PDF / 图片等 `BROWSER_NATIVE` 文件走平台受控 `PREVIEW` access ticket。
  - 右键 `打开 / 预览` 与双击共用同一路径。
  - 原生事件异步路径统一包住，用户取消类 `cancel` 不再冒成未处理错误。
  - 浏览器拦截新窗口时展示友好 fallback，可手动打开受控入口。
- 主 agent 初验：
  - 健康检查通过。
  - M2H 专项脚本通过，`PASS=30 FAIL=0`。
  - `git diff --check` 通过。
- 当前确认：
  - 未修改 `backend/**`、`docs/**`、数据库迁移。
  - 未新增 Hermes / BIM / parser / indexing / 文件正文读取能力。
  - 未执行真实业务目录写操作。
- 当前裁决：
  - 进入测试 agent 极短复验。
  - M2H 尚未正式收口，等待测试报告。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 属于本批交付物，收口提交时需纳入 Git。

## 2026-05-23 M2H 正式收口

- 测试 agent 已完成 `M2H P1 PDF / 图片受控预览极短复验`，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- 当前 P2：
  - 既有 Vite chunk warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件继续排除。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 是 M2H 交付脚本，收口提交时必须纳入 Git。
- 已确认：
  - PDF 双击可创建受控预览票据，并通过 `/api/data-steward/assets/file-access/...` 打开。
  - 右键 `打开 / 预览` 与双击共用受控预览路径。
  - 未再出现未处理的 `cancel` 或 native event handler 错误。
  - 文件夹双击进入目录不回归。
  - RVT / IFC 等模型文件仍打开“模型预览占位”，未伪装成真实 3D。
  - M2H 专项脚本通过，`PASS=30 FAIL=0`。
  - M2G 回归通过，`PASS=26 FAIL=0`。
  - 文件访问安全回归通过，`PASS=18 FAIL=0`。
  - 前端构建、健康检查和 `git diff --check` 通过。
  - 未执行真实业务目录写操作。
  - 未泄露真实 NAS 路径或敏感字段。
  - 未新增 Hermes / 真实 BIM / parser / indexing / 文件正文读取能力。
- 主 agent 裁决：`M2H：Windows 风格文件管理器交互升级` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 先选择性提交并推送 M2H。
  - 后续可进入 `M2I：多真实项目复制试点`，验证文件管理器和交付闭环在更多真实项目上是否自然可用。

## 2026-05-23 M2H 返工：文件管理器目录直达子项口径

- 用户在 105 项目真实使用文件管理器时发现：
  - 左侧文件夹显示不全，只显示了 `01`、`03`、`05` 等部分文件夹。
  - 根目录右侧显示了不属于根目录的深层文件。
  - 任意目录右侧都应该只显示该目录的直接子文件夹和直接子文件。
- 主 agent 初步定位：
  - 当前根目录下 `loadFiles()` 没有传目录过滤，容易拿到整个项目文件。
  - 后端 `catalog/files` 的目录过滤更接近“当前目录及其子目录”，不适合文件管理器右侧主列表。
  - 左侧目录不全需要同时检查 `catalog/directories`、`data_nas_directory_records`、已登记文件父目录和 `buildDirectoryTree(...)`。
- 裁决：
  - M2H 需要返工修复，不应在此问题解决前提交 / 推送收口。
  - 已更新 `handoff/dev-agent/current-prompt.md`，要求开发 agent 修复目录直达子项语义。
  - 本轮允许小范围后端 catalog files 接口改动，但必须保持旧递归语义兼容，不得改 `docs/**`、不得新增 Hermes / BIM / parser / indexing。

## 2026-05-24 M2H 目录直达子项返工完成，进入极短验收

- 开发 agent 已完成 M2H 目录直达子项返工，报告写入 `handoff/dev-agent/latest-report.md`。
- 修复内容：
  - `catalog/files` 新增 `directOnly` 参数，默认 `false` 保持旧递归兼容。
  - 文件管理器调用 `directOnly=true`，根目录和子目录只显示直接文件。
  - 目录树取消公共前缀压缩，保留真实目录层级。
  - M2H 专项脚本增强 direct-only 与旧递归兼容断言。
- 主 agent 初验：
  - 健康检查通过。
  - `bash scripts/dev/check-m2h-windows-file-manager.sh`：`PASS=51 FAIL=0`。
  - `git diff --check`：通过。
- 当前确认：
  - 未修改 `docs/**`。
  - 未新增数据库迁移。
  - 未新增 Hermes / 真实 BIM / parser / indexing / 正文读取能力。
  - 未执行真实业务 NAS 破坏性写操作。
- 当前裁决：
  - 进入测试 agent 极短验收。
  - M2H 返工尚未正式收口，等待测试报告。

## 2026-05-24 M2H 返工正式收口

- 测试 agent 已完成 `M2H 目录直达子项返工极短验收`，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- 当前 P2：
  - 既有 Vite chunk warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件继续排除。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 是 M2H 交付脚本，收口提交时必须纳入 Git。
- 已确认：
  - 105 / 503 根目录右侧只显示直接子文件夹和直接文件，不再铺满深层文件。
  - 进入 `01_文件收发` 等目录后，右侧只显示当前目录直接子项。
  - 左侧目录树完整，能看到真实下级目录，不再只剩 `01`、`03`、`05`。
  - `directOnly=true` 根目录和子目录直达子项语义通过脚本验证。
  - 默认不传 `directOnly` 时旧递归目录行为保留。
  - PDF 受控预览、RVT 模型占位、文件夹双击进入目录未回归。
  - 后端构建、前端构建、健康检查、M2H 专项、M2G、M2B、文件访问安全回归均通过。
  - 未发现真实 NAS 路径、SQL、raw row、token、secret 泄露。
  - 未执行真实业务 NAS 写操作。
  - 未新增 Hermes / 真实 BIM / parser / indexing / 正文读取能力。
  - 未修改 `docs/**`，未新增数据库迁移。
- 主 agent 裁决：`M2H：Windows 风格文件管理器交互升级 + 目录直达子项返工` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：
  - 选择性提交并推送 M2H，必须纳入 `scripts/dev/check-m2h-windows-file-manager.sh`。
  - 后续可进入 `M2I：多真实项目复制试点`，验证文件管理器、工程主数据和交付闭环在更多真实项目上是否自然可用。

## 2026-05-23 M2G 正式收口

- 测试 agent 已完成 `M2G：真实 NAS 文件管理器灰度完善` 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件继续排除。
  - 数字孪生 / 可视化并行分支改动继续作为观察项，不混入 M2G 提交。
- 已确认：
  - M2G 专项脚本通过，`PASS=26 FAIL=0`。
  - M2B 回归通过，`PASS=18 FAIL=0`。
  - 文件访问安全回归通过，`PASS=18 FAIL=0`。
  - 真实项目文件管理页可打开，目录树、文件表、工具栏、回收站、操作记录和当前目录可写状态清楚。
  - 上传、新建、重命名、移动、移入回收站、恢复闭环只在隔离测试项目和 `/tmp/delivery-m2g-nas-*` 临时目录中执行。
  - 未触碰真实业务目录，未开放普通用户永久删除，未读取正文，未新增 Hermes / BIM / parser / indexing 能力。
  - 未发现真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret 泄露。
- 主 agent 裁决：`M2G：真实 NAS 文件管理器灰度完善` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：`M2H：多真实项目复制试点`，把 105 / 503 的交付闭环和文件管理器体验复制到更多真实项目验证。

## 2026-05-23 M2G 启动

- 用户确认进入下一批次 M2G，重点优化当前平台文件管理器。
- 主 agent 裁决：当前进入 `M2G：真实 NAS 文件管理器灰度完善`。
- M2G 定位：
  - 文件管理器产品化专项。
  - 类 Windows / macOS 文件管理器体验。
  - 融合必要工程资料字段。
  - 不扩展成网盘，不启动 BIM / Hermes 新能力。
- M2G 目标：
  - 新建文件夹不再出现项目上下文误判。
  - 上传 / 新建 / 重命名 / 移动 / 删除到回收站 / 恢复形成顺畅单项闭环。
  - 当前目录可写状态和不可写原因清楚。
  - 操作成功后文件列表、目录树、回收站和操作记录能自然刷新。
  - 回收站命名统一，不暴露“隔离区”等技术词。
  - 操作记录和失败原因脱敏，不泄露真实 NAS 路径。
- M2G 禁止：
  - 永久删除作为普通员工能力开放。
  - 绕过灰度开关、项目权限、角色权限或审计。
  - 读取文件正文。
  - 新增 Hermes / BIM / parser / indexing。
  - 修改数字孪生主线能力。
- 当前交接：
  - 计划：`handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-23 M2G 开发完成，进入测试前审计

- 开发 agent 已提交 M2G 完成报告，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已完成第一轮审计：
  - M2G 文件管理器专项脚本通过，`PASS=26 FAIL=0`。
  - `git diff --check` 通过。
  - 文件管理器本批核心改动集中在 `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`。
  - 新增 M2G 专项脚本 `scripts/dev/check-m2g-nas-file-manager-polish.sh`。
- 当前发现并行分支改动：
  - 工作区同时存在 `backend/delivery-visualization-adapter/**` 和 `frontend/src/modules/visualization/**` 的数字孪生 / 可视化相关改动。
  - 用户已确认这些改动来自另一个 agent 在另一个分支上的数字孪生平台可视化界面开发。
  - 这些改动不作为 M2G 文件管理器交付内容，不因其存在阻塞 M2G 测试；但若导致构建或回归失败，仍需按回归问题处理。
  - M2G 收口提交时应选择性提交 M2G 文件管理器、专项脚本和 handoff 文件，不应把数字孪生 / 可视化改动混入 M2G 提交。
- 当前裁决：
  - M2G 文件管理器主链路可以进入测试 agent 轻量验收。
  - 数字孪生 / 可视化改动归属已确认，可作为并行观察项记录，不阻塞 M2G。

## 2026-05-23 M2F 正式收口

- 测试 agent 已完成 `M2F：真实项目交付闭环试运行` 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件继续排除。
- 已确认：
  - 105 / 503 已确认的正式工程主数据能驱动文档 / 图纸交付视图。
  - 文档 22 个应交项、图纸 22 个应交项，当前均待人工补交。
  - 缺失项解释能说明目标部位、交付定义、交付类型和需要补交的文件类型。
  - 推荐挂接仍需要人工确认。
  - 审核 / 整改查询和交付包草案 dry-run 链路未回归。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing 能力。
- 主 agent 裁决：`M2F：真实项目交付闭环试运行` 正式收口。
- 当前 active 批次：`待用户确认`。

## 2026-05-23 M2F 开发完成，进入测试验收

- 开发 agent 已完成 `M2F：真实项目交付闭环试运行`，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已审计开发报告、关键代码和专项脚本。
- 本轮改动集中在：
  - `DeliveryApplicationService`：补齐 `delivery-completeness` 与交付包草案中的业务化缺失 / 阻塞说明。
  - `AgentGovernanceApplicationService`：缺失项解释补充交付定义。
  - `scripts/dev/check-m2f-real-project-delivery-loop.sh`：新增 M2F 专项脚本。
- 主 agent 初验通过：
  - 后端健康检查：通过。
  - `scripts/dev/check-m2f-real-project-delivery-loop.sh`：`PASS=6 FAIL=0`。
  - `git diff --check`：通过。
- 已确认：
  - 105 正式工程主数据能驱动文档 / 图纸应交项。
  - 当前文档 22 个应交项、图纸 22 个应交项，均处于待人工补交。
  - 交付包草案 dry-run 共 44 条，均因未挂接文件而阻塞。
  - 缺失 / 阻塞原因能写出目标、交付定义、交付类型和需要补交的文件类型。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing。
- 当前裁决：进入测试 agent 轻量验收，尚未收口 M2F。

## 2026-05-22 M2F 启动

- 用户已确认主 agent 的下一步计划，并允许执行。
- 主 agent 裁决：当前进入 `M2F：真实项目交付闭环试运行`。
- 启动原因：
  - M2E 已正式收口。
  - 105 / 503 已完成真实资产接入和人工确认后的正式工程主数据。
  - 当前需要验证这些规则是否真的能驱动文档 / 图纸交付、缺失项、挂接、审核整改和交付包草案。
- M2F 核心链路：
  - `真实资产 -> 工程主数据 -> 交付标准 -> 文档/图纸应交项 -> 文件挂接 -> 审核/整改 -> 交付包草案`
- M2F 目标：
  - 105 正式工程主数据能驱动文档 / 图纸交付链路。
  - 缺失项和阻塞原因对员工可理解。
  - 人工挂接、审核整改、交付包草案链路不回归。
- M2F 禁止：
  - 真实 NAS 文件写入、移动、删除、重命名、复制。
  - 读取 PDF / Office / DWG / RVT / IFC 正文。
  - 新增 Hermes / BIM / parser / writer / indexing 能力。
  - 自动挂接、自动审核、自动整改。
  - 启动 8B / 8C / 9A。
- 当前交接：
  - 计划：`handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-22 M2E 正式收口

- 测试 agent 已完成 `M2E P1 selectedDraftItemIds 契约极短复验`，报告写入 `handoff/test-agent/latest-report.md`。
- 复验结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 仅既有 Vite chunk size warning，不阻塞。
- 已确认：
  - 空 `selectedDraftItemIds=[]` 被后端拒绝。
  - 不存在或失效的草案项 ID 被后端拒绝。
  - 合法小选择只处理所选草案项及必要依赖，不再全量返回 40 项。
  - 依赖项通过 `DEPENDENCY+...` 标识，没有伪装成用户直接选择。
  - 初始化页已确认状态文案不再误导为“工程主数据未确认”。
  - 初始化页可见“查看节点类型”入口。
  - 未发现真实 NAS 路径或敏感字段泄露。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing 能力。
- 主 agent 裁决：`M2E：真实项目工程主数据人工确认与交付规则落地` 正式收口。
- 当前 active 批次：`待用户确认`。

## 2026-05-22 M2E P1 修复完成，进入极短复验

- 开发 agent 已完成 `M2E P1 修复 selectedDraftItemIds 契约`，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已完成代码和脚本初验：
  - 后端确认接口现在会读取并校验 `selectedDraftItemIds`。
  - 空选择会返回 `REAL_PROJECT_DRAFT_SELECTION_REQUIRED`。
  - 无效草案 ID 会返回 `REAL_PROJECT_DRAFT_SELECTION_INVALID`。
  - 合法小选择只处理所选草案项及必要依赖，不再全量返回 40 项生成 / 跳过结果。
  - 依赖项通过 `DEPENDENCY+...` 标识，避免伪装成用户直接选择。
- 主 agent 已执行：
  - `curl -fsS http://127.0.0.1:8080/actuator/health`：通过。
  - `bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh`：`PASS=11 FAIL=0`。
  - `git diff --check`：通过。
- 已将测试 agent prompt 收窄为 `M2E P1 selectedDraftItemIds 契约极短复验`。
- 当前裁决：
  - P1 从主 agent 初验看已修复。
  - M2E 尚未正式收口，等待测试 agent 极短复验。

## 2026-05-22 M2E 测试不通过，进入 P1 修复

- 测试 agent 已完成 M2E 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：不通过。
- P0：无。
- P1：
  - `selectedDraftItemIds` 未生效。
  - 前端支持逐项勾选草案项，但后端忽略该字段。
  - 空选择或无效选择仍返回 OK 并按全量规则处理。
- P2：
  - 既有 Vite chunk warning。
  - 105 已是确认后状态，无法复测 first-run。
  - 初始化页部分文案仍停留在 M2D 草案状态。
  - 缺少明确“查看节点类型”入口。
- 主 agent 裁决：
  - M2E 暂不收口。
  - 当前进入 `M2E P1 修复 selectedDraftItemIds 契约`。
  - 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。

## 2026-05-22 M2E 开发完成，进入测试验收

- 开发 agent 已完成 `M2E：真实项目工程主数据人工确认与交付规则落地`，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已完成初验：
  - `scripts/dev/check-m2e-real-project-masterdata-confirmation.sh` 通过，`PASS=7 FAIL=0`。
  - `scripts/dev/check-m2d-real-project-masterdata-onboarding.sh` 通过，`PASS=8 FAIL=0`。
  - `scripts/dev/check-m2c-delivery-package-archive.sh` 通过，`PASS=11 FAIL=0`。
  - `scripts/dev/check-m1c-real-project-masterdata.sh` 通过，`PASS=14 FAIL=0`。
  - 健康检查通过。
  - `git diff --check` 通过。
- 已确认：
  - 新增 `POST /api/master-data/projects/{projectId}/onboarding/confirm`。
  - 未带 `confirmed=true` 时拒绝。
  - 105 / 503 已通过人工确认生成正式初始主数据。
  - 文档 / 图纸交付和交付包准备视图已基于正式规则工作。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing。
- 主 agent 关注点：
  - 前端传入 `selectedDraftItemIds`，需要测试 agent 确认后端是否真正按勾选项影响生成；如果只是 UI 勾选但后端忽略，应记为 P1 或要求开发 agent 收窄为“策略确认”而非“逐项选择”。
- 当前状态：进入测试 agent 验收，尚未收口。

## 2026-05-22 M2E 启动

- 用户确认支持主 agent 的下一批次申请。
- 主 agent 裁决：当前进入 `M2E：真实项目工程主数据人工确认与交付规则落地`。
- 启动原因：
  - M2D 已让 105 进入“真实资产已接入、工程主数据未确认、草案可预览”的状态。
  - 105 仍需要经过人工确认，才能生成正式部位树、节点类型和交付物标准。
  - 平台必须从“管文件 + 看草案”推进到“按人工确认的规则做真实交付”。
- M2E 目标：
  - 新增或强化人工确认接口。
  - 人工确认后生成最小可用工程主数据。
  - 正式数据必须能解释来源：资产线索、人工确认、行业参考。
  - 生成后文档/图纸交付和交付包草案能基于正式规则工作。
- M2E 禁止：
  - 触碰真实 NAS 文件。
  - 读取文件正文。
  - 新增 Hermes / BIM / parser / indexing 能力。
  - 自动挂接、审核、整改或生成真实交付结论。
  - 让真实项目回到“一键模板已完成”的老路。
- 当前交接：
  - 计划：`handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-22 M2D 收口

- 测试 agent 已完成 `M2D：真实项目工程主数据接入草案增强` 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：`通过`。
- 当前未发现 P0 / P1。
- P2：
  - 既有 Vite chunk size warning。
  - 轻量策略下未做全量浏览器逐页验收。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件，以及 M2A / M2B / M2C 既有改动，提交时需要确认归属。
- 已通过：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2D 专项脚本，`PASS=8 FAIL=0`。
  - M1C 回归。
  - M2C 回归。
  - `git diff --check`。
- 已确认：
  - `503 / 105` 保持 `deliverableStandardReady=false`。
  - 105 真实资产已接入，工程主数据未确认。
  - 接入评估返回真实资产统计、扩展名分布、专业分布、治理风险和 Missing Evidence。
  - 草案预览返回 catalog-only 证据、证据来源、置信度、风险提示和人工确认标记。
  - 文档 / 图纸交付与交付包接口没有生成虚假应交项或虚假交付包。
  - 未泄露真实 NAS 路径或敏感字段。
  - 未触碰真实 NAS 文件。
  - 未新增 Hermes、BIM、parser、writer、indexing 能力。
- 主 agent 裁决：M2D 正式收口。
- 当前 active 批次：`待用户确认`。

## 2026-05-22 M2D 启动

- 用户确认继续下一步。
- 主 agent 裁决：当前进入 `M2D：真实项目工程主数据接入草案增强`。
- 启动原因：
  - 105 项目演示模板数据已软删除。
  - 105 仍有 2928 条真实资产文件。
  - 当前必须把真实资产目录和工程主数据接起来，不能再靠模板演示冒充真实项目交付标准。
- 105 当前资产线索：
  - DWG：2487。
  - PDF：242。
  - RVT：198。
  - XLSX：1。
  - 专业线索：建筑、结构、电气、给排水、消防、智能化、暖通、燃气、通用。
- M2D 目标：
  - 强化接入评估。
  - 生成真实资产驱动的工程主数据草案。
  - 区分资产线索与模板骨架。
  - 明确 catalog-only 证据边界。
  - 防止真实 NAS 项目一键套模板后直接变成 `deliverableStandardReady=true`。
- M2D 禁止：
  - 触碰真实 NAS 文件。
  - 读取文件正文。
  - 新增 Hermes / BIM / parser / indexing 能力。
  - 自动挂接、审核、整改或生成真实交付结论。
- 当前交接：
  - 计划：`handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-22 M2D 开发完成，进入测试验收

- 开发 agent 已完成 `M2D：真实项目工程主数据接入草案增强`，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已审计开发报告和核心代码改动。
- 已确认：
  - 105 真实项目仍保持 `deliverableStandardReady=false`。
  - `onboarding/assessment` 返回真实资产统计、专业分布、扩展名分布、治理风险和 Missing Evidence。
  - `onboarding/preview` 返回 catalog-only 草案项，包含证据来源、置信度、风险提示和人工确认标记。
  - 真实 NAS 项目直接调用 `initialization:apply-template` 会被阻断。
  - 真实 NAS 项目调用 `onboarding/apply` 只确认草案，不写成正式标准，`draftApplied=false`。
  - 未新增数据库迁移。
  - 未触碰真实 NAS 文件。
  - 未新增 Hermes / BIM / parser / writer / indexing 能力。
- 主 agent 已补强测试 prompt，要求测试 agent 额外验证页面下一步入口：
  - 去人工确认部位树。
  - 去人工确认节点类型。
  - 去人工配置交付物标准。
- 当前状态：进入测试 agent 验收，尚未收口。

## 2026-05-22 测试策略调整

- 用户要求：测试 agent 每次不再花大量时间逐页浏览器点击，只测试代码可用性和是否偏离主线。
- 主 agent 已新增轻量测试策略：
  - `handoff/main-agent/lightweight-test-strategy.md`
- 后续默认测试范围：
  - 构建。
  - 健康检查。
  - 当前批次专项脚本。
  - 1-3 条必要回归脚本。
  - 核心 API 抽查。
  - forbidden field / 路径泄露 / 敏感字段扫描。
  - `git diff --check`。
  - 越界检查。
- 浏览器验收仅在 UI/UX、路由、页面问题或主 agent 明确要求时执行。
- 已同步简化 M2D 测试 prompt，默认不做大范围浏览器逐页点击。

## 2026-05-22 105 项目演示模板数据重置

- 用户确认：105 项目中的工程主数据模板属于演示数据，没有真实数字化交付作用，继续保留会误导员工认为项目已经完成交付规则配置。
- 已处理真实项目：`503 / 105 / 深圳市二十八高项目`。
- 处理方式：
  - 软删除 105 项目下演示工程主数据、交付物标准、目录模板、交付挂接、整改记录和交付包草案。
  - 不触碰真实 NAS 文件。
  - 不删除文件资产元数据。
  - 保留审计和可追溯备份。
- 处理后状态：
  - 工程部位树：0。
  - 节点类型：0。
  - 交付物定义 / 类型 / 属性：0。
  - 目录模板：0。
  - 有效交付挂接：0。
  - 文件资产仍保留：2928 条。
  - 105 项目初始化状态回到 `SECTION_TREE`，`deliverableStandardReady=false`。
- 审计：
  - `action_code=master-data.template-demo.reset`
  - `trace_id=manual-reset-105-template`
- 报告：
  - `handoff/main-agent/project-105-template-reset-report.md`

## 2026-05-22 M2C 收口

- 测试 agent 已完成 `M2C：交付包与档案目录能力` 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：`通过`。
- 当前未发现 P0 / P1。
- P2：
  - 既有 Vite chunk size warning。
  - 506 / 93 当前无应交项，交付包页面展示 0 条空状态。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件需要收口提交时排除。
- 已通过：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2C 专项脚本。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归。
  - `git diff --check`。
  - 当前库和临时干净库 Flyway 迁移。
- 已确认：
  - 交付包草案、草案明细、档案目录、CSV 清单导出可用。
  - 页面与接口未泄露真实 NAS 路径或敏感字段。
  - 未触发真实打包、复制、移动、删除、改名或正文读取。
  - 未新增 Hermes、BIM、parser、writer、indexing 能力。
- 主 agent 裁决：M2C 可以正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步不自动启动；候选方向见 `handoff/main-agent/phase2-current-roadmap.md`。

## 2026-05-22 MAIN-0 主线回归与 M2C 启动

- 当前分支：`main`。
- UX1 / UX2 / UX3 已完成验收并合并回主线，主线继续保持 `绿灯`。
- 当前 active 批次正式切换为：`M2C：交付包与档案目录能力`。
- M2C 目标：
  - 将已有 `导出预检查 dry-run` 推进为可保存、可查看、可导出清单的 `交付包草案 / 档案目录`。
  - 让项目负责人清楚看到哪些交付项可交付、缺失、待审核、已驳回或被阻塞。
  - 当前只生成清单 / 草案，不生成真实物理文件包。
- M2C 禁止：
  - 复制、移动、删除、重命名真实 NAS 文件。
  - 生成真实压缩包。
  - 泄露真实 NAS 路径或 `storage_uri`。
  - 新增 Hermes 能力。
  - 接入真实 BIM 引擎。
  - 进入 8B / 8C / 9A。
- 真实 NAS 写入灰度：
  - 项目 `504 / 100 / 深圳市二十八高项目` 允许进入受控灰度试运行。
  - 灰度仍为项目级、目录级、角色级、账号级边界，不代表全项目或全公司默认开放。
  - 用户可见命名从 `隔离区` 收束为 `回收站`；内部接口和数据库仍保持 quarantine 命名以兼容既有契约。
- 当前交接：
  - M2C 计划：`handoff/main-agent/m2c-delivery-package-archive-directory-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-22 UX3 启动

- 当前 active 批次：`UX3：主视图聚焦与认知减负`。
- 当前分支：`codex/ux3-main-view-focus`。
- 启动依据：
  - UX2 已验收通过并推送。
  - 用户反馈主视图仍然过于混乱，数据管家核心功能没有足够聚焦。
- UX3 定位：
  - UX 分支内的小批次。
  - 不进入 M2C。
  - 不新增业务能力。
  - 只做主视图减法、核心入口重排和低频入口降级。
- UX3 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- UX3 测试 prompt：`handoff/test-agent/current-prompt.md`。
- UX3 计划：`handoff/main-agent/ux3-main-view-focus-plan.md`。
- UX3 重点：
  - 资产总览改成项目启动台。
  - 首屏聚焦项目搜索、真实项目列表、推荐进入、文件管理、项目可视化。
  - 项目工作台首屏聚焦文件管理、资产驾驶舱 / 可视化、交付状态。
  - 技术标签和长教程降级到详情、折叠区或空状态。
- UX3 边界：
  - 允许修改 `frontend/**` 和 `handoff/dev-agent/latest-report.md`。
  - 禁止修改 `backend/**`、数据库迁移、后端接口、权限规则和 `docs/**`。
  - 禁止新增 Hermes、BIM、NAS 写能力或文件正文读取。

## 2026-05-22 UX3 进入测试验收

- 开发 agent 已完成 UX3 主视图聚焦与认知减负，并写入 `handoff/dev-agent/latest-report.md`。
- 开发报告显示：
  - 资产总览改成“项目启动台”。
  - 项目工作台首屏固定突出文件管理、项目可视化、交付状态。
  - 技术标签移入“项目详情 / 技术信息”折叠区。
  - 项目内导航压缩为“主入口 + 更多入口”。
  - 侧边栏版本标识更新为 `BUILD · UX3`。
- 主 agent 初审：
  - 页面改动集中在 `frontend/**`。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 改动。
  - 可以进入 UX3 测试 agent 验收。

## 2026-05-22 UX3 收口

- UX3 测试 agent 已完成验收，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：`通过`。
- 当前无 P0 / P1。
- 已验证：
  - 后端构建、前端构建、健康检查通过。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归通过。
  - `git diff --check` 通过。
  - Fresh login 后资产总览约 3.1 秒内展示项目启动台、推荐项目、文件管理、项目可视化、真实项目列表和下一步动作。
  - 503 / 506 项目工作台首屏聚焦文件管理、项目可视化 / 资产驾驶舱、交付状态。
  - 旧链接兼容稳定，多分辨率检查通过。
  - 未发现真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret 泄露。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 变更。
- P2：
  - 既有 Vite chunk size warning。
  - 项目工作台仍有少量偏技术文本，例如 `INTERNAL_PILOT`、`项目 503`、`平台内部 ID`。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为非交付未跟踪文件，收口提交时继续排除。
- 主 agent 裁决：
  - `UX3：主视图聚焦与认知减负` 可以正式收口。
  - UX3 收口后建议做 Git checkpoint，再回到平台主线规划。

## 2026-05-22 UX2 启动

- 当前 active 批次：`UX2：前端使用逻辑与体验重构专项`。
- 当前分支：`codex/ux2-user-experience-refactor`。
- 启动依据：
  - UX1 已收口，无 P0 / P1 / P2。
  - UX1 Git checkpoint 已推送。
  - 用户确认进入 UX2。
- UX2 开发 agent：`Claude Code`，由用户在 CLI 中启动。
- 主 agent 职责：维护 prompt、监控边界、审计是否越界、安排测试 agent 验收和最终收口判断。
- 当前开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 当前测试 prompt：`handoff/test-agent/current-prompt.md`。
- UX2 目标：优化前端使用逻辑、信息层级、字段减负、下一步动作提示和视觉体验，让普通员工进入平台后能看懂、会用、用得舒服。
- UX2 边界：
  - 允许修改 `frontend/**` 和 `handoff/dev-agent/latest-report.md`。
  - 禁止修改 `backend/**`、数据库迁移、接口语义、权限规则和 `docs/**`。
  - 禁止新增 Hermes、BIM 轻量化、NAS 写能力、文件正文读取、索引、parser。
  - 旧路由必须兼容跳转，不允许删除导致书签或测试脚本失效。

## 2026-05-22 UX2 续接安排

- Claude Code 已完成 UX2 视觉子批次：登录页、资产总览、项目工作台命令中心的官网风格 lightfield / spotlight / 粒子 / glass-lite 升级。
- 用户确认：UX2 后续功能由原 Codex 开发 agent 继续完成，完善 UX2 后再进入整体验收。
- 主 agent 判断：
  - Claude 视觉升级不等于 UX2 收口。
  - 后续重点回到 UX2 原目标：使用逻辑、字段减负、下一步动作、低频入口降级。
  - 当前不能只按视觉升级进入最终验收。
- 已将 `handoff/dev-agent/current-prompt.md` 改为 Codex 开发 agent 续接任务。
- 续接任务要求：
  - 保留 Claude 已完成的视觉升级，不得无故回滚。
  - 继续优化资产总览、项目工作台、文件管理、工程主数据、文档 / 图纸交付的用户理解路径。
  - 继续保持前端专项边界，不修改后端、数据库迁移、接口语义、权限规则或 `docs/**`。

## 2026-05-22 UX2 进入整体验收

- Codex 开发 agent 已完成 UX2 使用逻辑与字段减负续接，并写入 `handoff/dev-agent/latest-report.md`。
- 开发报告显示：
  - 保留 Claude 视觉升级。
  - 资产总览改为项目入口台。
  - 项目工作台前置推荐下一步动作。
  - 文件管理默认隐藏平台 ID、扩展名、置信度、更新时间等技术字段。
  - 工程主数据补齐“先定义规则，再按规则交付”的提示。
  - 整改中心补齐闭环说明。
  - 旧链接抽查未白屏。
- 主 agent 初审：
  - 变更范围仍集中在 `frontend/**` 和 handoff 文件。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 改动。
  - 可以进入 UX2 测试 agent 整体验收。

## 2026-05-22 UX2 收口

- UX2 测试 agent 已完成整体验收，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：`通过`。
- 当前无 P0 / P1。
- 已验证：
  - 后端构建、前端构建、健康检查通过。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归通过。
  - `git diff --check` 通过。
  - 资产总览、503 / 506 项目工作台、文件管理、工程主数据、交付工作中心、旧链接兼容和多分辨率检查通过。
  - 未发现真实 NAS 路径、`storage_uri`、raw row、SQL、token、secret 泄露。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 变更。
- P2：
  - 既有 Vite chunk size warning。
  - 项目工作台仍有少量偏技术字段显眼，例如 `平台内部ID`、`NAS_REAL_PILOT`、`ACTIVE`。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为非交付未跟踪文件，收口提交时继续排除。
- 主 agent 裁决：
  - `UX2：前端使用逻辑与体验重构专项` 可以正式收口。
  - UX2 收口不代表 UI/UX 完全终局，P2 可后续在 UX polish 或客户演示前继续打磨。

## 2026-05-22 UX1 收口

- UX1 tokens.css P1 已完成极短复验并通过。
- 当前 UX1 无 P0 / P1 / P2。
- 已确认：
  - `frontend/src/styles/tokens.css` 已纳入暂存范围，不再是未跟踪运行期样式文件。
  - `frontend/src/styles/index.css` 仍正确引用 `./tokens.css`。
  - 前端构建通过。
  - `git diff --check` 通过。
  - UX1 变更范围保持在 `frontend/**` 与 handoff 文件内，未发现 `backend/**`、数据库迁移、`docs/**` 被修改。
- 主 agent 裁决：
  - `UX1：前端路由逻辑与视觉体验专项优化` 可以正式收口。
  - UX1 只代表前端壳层、路由、菜单、项目工作台与基础视觉优化收口，不代表 UX2 使用逻辑重构已启动。
  - 下一步可在完成 Git checkpoint 后进入 `UX2：前端使用逻辑与体验重构专项`。

## 2026-05-22 UX1 验收 P1 处理

- UX1 测试 agent 已完成正式验收，结论：`不通过，但无 P0`。
- 当前唯一阻塞项：`frontend/src/styles/tokens.css` 被 `frontend/src/styles/index.css` 作为运行期样式引用，但仍处于未跟踪状态。
- 主 agent 判断：该文件属于 UX1 运行必需文件，必须纳入 UX1 提交；否则回滚、拉取仓库或新环境启动时会缺失样式 token。
- 处理原则：
  - 纳入 `frontend/src/styles/tokens.css`。
  - 不纳入 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付文件。
  - UX2 仍保持待启动；必须等 UX1 极短复验通过后再切换当前 prompt。

## 2026-05-22 UX2 前端使用逻辑重构待启动

- 用户确认：UX1 主要改善了视觉 token、壳层样式和三段导航，但仍未真正解决“用户进入平台后不知道看什么、点什么、下一步做什么”的核心问题。
- UX2 定位：`前端使用逻辑与体验重构专项`。
- UX2 当前状态：`待启动`。
- UX2 启动条件：
  - UX1 测试 agent 报告已写入 `handoff/test-agent/latest-report.md`。
  - UX1 无 P0 / P1。
  - 若 UX1 有 P0 / P1，必须先由 Claude Code 修复 UX1 阻塞项，再进入 UX2。
- UX2 开发方式仍由 `Claude Code` 主导，主 agent 监控。
- UX2 只允许修改 `frontend/**` 和 `handoff/dev-agent/latest-report.md`。
- UX2 禁止修改 `backend/**`、数据库迁移、接口语义、权限规则和 `docs/**`。
- UX2 计划：`handoff/main-agent/ux2-user-experience-refactor-plan.md`。
- UX2 待启动开发 prompt：`handoff/dev-agent/ux2-user-experience-refactor-prompt.md`。
- UX2 待启动测试 prompt：`handoff/test-agent/ux2-user-experience-refactor-test-prompt.md`。
- 在 UX1 测试报告返回前，不切换 `handoff/dev-agent/current-prompt.md`，不启动 UX2。

## 2026-05-21 UX1 正式进入 Claude Code 前端壳层重构

- 用户确认：冻结当前后端功能开发，先紧急修复前端框架，避免后续继续在混乱骨架上堆功能。
- 当前 active 批次：`UX1：前端路由逻辑与视觉体验专项优化`。
- 当前分支：`codex/ux1-frontend-routing-visual`。
- UX1 开发 agent：`Claude Code`。
- 主 agent 职责：监控 Claude、审计是否越界、维护 prompt、判断是否进入测试验收。
- UX1 开发方式：分层重构。
  - UX1.1：壳层、路由、菜单、项目工作台骨架。
  - UX1.2：资产总览、项目详情、文件管理。
  - UX1.3：工程主数据初始化、部位树、节点类型、交付物标准。
  - UX1.4：文档交付、图纸交付、整改、交付包预检查。
  - UX1.5：统一空状态、错误提示、按钮文案、表格工具栏、抽屉和弹窗样式。
- UX1 禁止：
  - 修改 `backend/**`。
  - 修改数据库迁移。
  - 新增或修改后端接口。
  - 改变权限规则。
  - 修改 `docs/**`。
  - 引入 Hermes 新能力、BIM 轻量化、NAS 新写能力、文件正文读取或索引。
- 当前开发 prompt 已写入：`handoff/dev-agent/current-prompt.md`。

## 2026-05-22 UX1 进入测试验收

- Claude Code 已完成 UX1 前端壳层完整重构，并写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 初步审计：
  - 变更主要集中在 `frontend/**` 和 handoff 报告。
  - 未发现 `backend/**`、`docs/**`、数据库迁移被修改。
  - 存在未跟踪 `.claude/**`、`CLAUDE.md` 等非运行文件，测试 agent 需记录并由主 agent 决定是否清理或忽略，不能默认纳入 UX1 提交。
- 测试重点：
  - 浏览器多分辨率视觉巡检。
  - 503/506 项目间路由切换连续性。
  - 旧全局链接兼容跳转。
  - 文件管理、工程主数据、交付工作中心入口清晰度。
  - M2B/M2A/M1F/M1E/M1D/M1C 回归。
- UX1 测试 prompt 已写入：`handoff/test-agent/current-prompt.md`。

## 2026-05-21 UX1 紧急 UI / UX 修复首轮

- 已从 M2B 收口提交点切出前端专项分支：`codex/ux1-frontend-routing-visual`。
- 本轮是 UX1 急救型修复，不代表 UX1 完整收口。
- 修改范围仅限前端和 handoff，不修改后端、数据库、接口语义或权限规则。
- 已完成：
  - 顶部工作上下文显示当前项目 / 页面和下一步说明。
  - 项目工作台导航改为更清晰的 `项目资产 -> 工程主数据 -> 交付工作中心` 三段分组。
  - 文件管理页 checksum 后台任务默认收起，避免压过目录结构和文件列表。
- 验证：
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `git diff --check` 通过。
- 当前仍需 UX1 后续完整回归，尤其是旧路由兼容、项目工作台页面巡检和移动 / 窄屏布局。
- 急救报告：`handoff/main-agent/ux1-emergency-ui-fix-report.md`。

## 2026-05-21 M2B 收口

- `M2B：受控 NAS 写操作真实项目灰度试运行与安全开关` 已通过测试 agent 复核并正式收口。
- 当前无 P0 / P1 / P2。
- 已验证：
  - 默认无灰度配置时，真实项目写操作关闭。
  - M2B 专项脚本连续两次通过，结果 `PASS=18 FAIL=0`。
  - M2A / M1F / M1E / M1D / M1C 回归均通过。
  - 真实项目 UI 只做只读冒烟，未触发真实业务目录写入。
  - 未发现 raw NAS 路径、`storage_uri`、敏感字段、Hermes/G4/8B/8C/9A 扩展或 parser/indexing 边界突破。
- 主 agent 裁决：
  - M2B 可以正式收口。
  - 主线健康度维持 `绿灯`。
  - M2B 不代表默认开放真实项目 NAS 写操作；真实写入仍必须显式开启项目灰度并限制目录、角色和账号。
  - 永久删除、跨项目移动、批量不可逆操作、Agent / Hermes 触发 NAS 写操作仍禁止。
  - 下一步按既定顺序建议先做 Git checkpoint / push，再进入 `UX1：前端路由逻辑与视觉体验专项优化批次`。
- 收口报告：`handoff/main-agent/m2b-nas-write-trial-closure.md`。

## 2026-05-21 M2B 启动

- 用户确认：M2A 完整度可以进入 M2B，但需先完成 Git checkpoint / push。
- 当前 active 批次：`M2B：受控 NAS 写操作真实项目灰度试运行与安全开关`。
- M2B 目标：把 M2A 已完成的受控 NAS 写能力放入真实项目灰度试运行边界，而不是继续扩展完整 NAS CRUD。
- M2B 必须实现或确认：
  - 项目级 NAS 写操作灰度开关。
  - 项目内允许写入目录范围限制。
  - 可操作角色 / 账号边界。
  - 文件管理页展示灰度状态、可写范围、风险提示和禁用原因。
  - M2A 写接口在执行前检查灰度开关和目录范围。
  - 操作记录和审计继续可查。
- M2B 禁止：
  - 永久物理删除。
  - 跨项目移动。
  - 批量删除 / 批量移动 / 批量重命名。
  - Agent / Hermes 触发 NAS 写操作。
  - 前端传入或展示真实 NAS 绝对路径。
  - 文件正文读取、BIM 轻量化、构件解析、索引。
  - UX1 前端视觉专项改造。
- M2B 主 agent 计划：`handoff/main-agent/m2b-nas-write-trial-plan.md`。
- M2B 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- M2B 测试 prompt：`handoff/test-agent/current-prompt.md`。

## 2026-05-21 M2A 收口

- `M2A：NAS 受控文件操作安全底座` 已通过测试 agent 复核并正式收口。
- 当前无 P0 / P1 / P2。
- M2A 专项脚本 `scripts/dev/check-m2a-controlled-nas-write.sh` 通过，结果 `PASS=20 FAIL=0`。
- M1F / M1E / M1D / M1C / Phase2 batch4 文件访问安全回归均通过。
- 已验证能力：
  - 项目内新建文件夹。
  - 项目内上传文件。
  - 项目内文件 / 文件夹重命名。
  - 项目内文件 / 文件夹移动。
  - 删除到回收站。
  - 回收站恢复。
  - 操作记录与审计。
  - 操作后元数据同步。
- 测试边界：
  - 自动化写操作只发生在脚本创建的 `/tmp` 隔离项目根目录内。
  - 真实项目页面只做只读检查。
  - 未在真实业务 NAS 目录执行上传、移动、删除、重命名等写操作。
- 未发现 raw NAS 路径、`storage_uri`、`storage_path`、raw row、SQL、token、secret、password 泄露。
- M2A 收口不代表全量开放完整 NAS CRUD；永久删除、跨项目移动、批量不可逆操作仍禁止。
- 主线健康度：`绿灯可继续主线开发`。
- 收口报告：`handoff/main-agent/m2a-controlled-nas-write-closure.md`。
- 下一步建议：`M2B：受控 NAS 写操作真实项目灰度试运行与安全开关`，需用户确认后再启动。

## 2026-05-21 UX1 后续批次冻结

- 用户确认：当前平台核心功能基本完善，但前端可用性、跳转逻辑、用户使用方式和视觉质感仍明显不足。
- UX1 固定为 `前端路由逻辑与视觉体验专项优化批次`。
- 批次顺序固定为：`M2A 当前 bugfix 收口并推送 Git -> M2B 受控 NAS 写操作真实项目灰度 -> UX1 前端路由与视觉体验优化`。
- UX1 由 `Gemini` 主导开发，主 agent 负责监控、审计、纠偏和最终收口判断。
- UX1 只允许调整前端：
  - 路由。
  - 菜单。
  - 项目工作台。
  - 组件布局。
  - 样式。
  - 交互文案。
  - 页面体验。
- UX1 禁止：
  - 修改后端业务逻辑。
  - 新增后端接口。
  - 修改数据库迁移。
  - 改变权限规则。
  - 引入 Hermes 新能力。
  - 引入 BIM 轻量化。
  - 新增真实 NAS 写能力。
- 旧链接必须兼容跳转，不能直接删除导致员工书签或测试脚本失效。
- UX1 不抢占 M2B 命名，必须等 M2B 收口后再启动。
- UX1 主 agent 计划：`handoff/main-agent/ux1-frontend-routing-visual-plan.md`。
- UX1 Gemini 开发 prompt：`handoff/dev-agent/ux1-gemini-frontend-routing-visual-prompt.md`。
- UX1 测试 prompt：`handoff/test-agent/ux1-frontend-routing-visual-test-prompt.md`。

## 2026-05-21 M2A 启动

- 用户确认进入 `M2A：NAS 受控文件操作安全底座`。
- 当前 active 批次：`M2A`。
- M2A 第一批范围固定为：`低风险写操作 + 删除到回收站，不开放永久删除与批量操作`。
- 本批目标：在员工注册、项目授权和局域网试运行通过后，开始让平台具备真实 NAS 操作能力，但必须先建立权限、路径、审计、隔离和恢复底座。
- 允许范围：
  - 项目内新建文件夹。
  - 项目内上传文件。
  - 项目内文件 / 文件夹重命名。
  - 项目内文件 / 文件夹移动。
  - 删除到回收站。
  - 回收站恢复。
  - 操作记录与审计。
  - 操作后元数据同步。
- 禁止范围：
  - 永久物理删除。
  - 跨项目移动。
  - 批量删除 / 批量移动 / 批量重命名。
  - Agent / Hermes 触发 NAS 写操作。
  - 前端传入或展示真实 NAS 绝对路径。
  - 读取文件正文、真实 BIM 轻量化、构件解析。
  - 进入 G4 / Hermes 新能力 / 8B / 8C / 9A。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 主 agent 计划：`handoff/main-agent/m2a-controlled-nas-write-plan.md`。
- 主线健康度：`绿灯，但 M2A 属于高风险能力，必须按受控试点验收`。

## 2026-05-21 M1F 收口

- `M1F：员工注册、项目权限管理与局域网试运行` 已通过测试 agent 复核并正式收口。
- 当前无 P0 / P1 / P2。
- 已验证：
  - 员工手机号注册。
  - 无项目等待授权。
  - 管理员启用 / 停用 / 删除员工。
  - 管理员为员工分配项目和项目角色。
  - 普通员工越权拒绝。
  - 停用 / 删除后不可继续使用。
  - 局域网 `*:5173` 监听与 `192.168.1.66:5173` 访问。
  - M1C / M1D / M1E 回归通过。
- 未触碰真实 NAS 文件，未修改 `docs/**`。
- 主线健康度：`绿灯可继续主线开发`。
- 下一步用户确认：进入 `M2A：NAS 受控文件操作安全底座`。

## 2026-05-21 M1E 收口

- `M1E：文件管理连续工作体验与后台任务可追踪性收口` 已通过测试 agent 验收并正式收口。
- 当前无 P0 / P1 / P2。
- 文件管理已能按项目恢复目录、筛选、分页和最近文件上下文。
- 用户可通过 `重置视图` 回到项目默认文件视图。
- 文件详情和任务区已将文件 ID / 项目内部 ID / checksum 任务 ID 业务化展示。
- checksum 后台任务可见、可追踪、可查看脱敏失败原因、可受控重试。
- 普通用户不能跨项目读取任务或任务详情。
- 未泄露真实 NAS 路径、raw row、SQL、token、secret、password。
- 本轮未新增 Hermes 能力，未继续 G4，未进入 8B / 8C / 9A，未触碰真实 NAS 写操作。
- 主线健康度：`绿灯可继续主线开发`。
- 收口报告：`handoff/main-agent/m1e-file-task-continuity-closure.md`。
- 历史下一步建议：`M1 阶段复盘与 M2A 准备`；现已由 M1F 收口和 M2A 启动取代。

## 2026-05-20 M1E 启动

- 用户确认进入 `M1E：文件管理连续工作体验与后台任务可追踪性收口`。
- 当前 active 批次：`M1E`。
- M1E 目标：让文件管理按项目记住目录、筛选、分页和最近文件上下文，并让 checksum 后台任务在项目内可见、可理解、可重试。
- M1E 不是 M2A，不新增大模块，不开放真实 NAS 写操作。
- M1E 禁止：新增 Hermes 能力、继续 G4、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 主 agent 计划：`handoff/main-agent/m1e-file-task-continuity-plan.md`。

## 2026-05-20 M1D 收口

- `M1D：标准驱动交付闭环强化` 已通过测试 agent 复核并正式收口。
- 当前无 P0 / P1 / P2。
- 项目工作台信息结构已纠偏为 `项目资产 -> 工程主数据 -> 交付工作中心`。
- 工程主数据未就绪时，工作中心会提示 `请先生成 / 确认工程主数据草案`。
- 文档/图纸交付闭环、缺失项解释、远程分页补交、审核整改闭环和 dry-run 导出预检查均通过。
- 未泄露真实 NAS 路径、raw row、SQL、token、secret、password。
- 本轮未新增 Hermes 能力，未继续 G4，未进入 8B / 8C / 9A，未触碰真实 NAS 写操作。
- 主线健康度：`绿灯可继续主线开发`。
- 收口报告：`handoff/main-agent/m1d-standard-delivery-loop-closure.md`。
- 下一步建议：M1 阶段复盘，或经用户确认后进入 `M1E：文件管理连续工作体验与后台任务可追踪性收口`。

## 2026-05-20 M1D 启动

- 用户确认进入 `M1D：标准驱动交付闭环强化`。
- 当前 active 批次：`M1D`。
- M1D 目标：把工程主数据、文档交付、图纸交付、批量挂接、审核、整改、复审、完整率刷新和导出预检查串成普通员工可执行的完整闭环。
- M1D 主链路：`工程主数据就绪 -> 文档/图纸应交项 -> 缺失项 -> 补交文件 -> 提交审核 -> 审核通过/驳回 -> 整改处理 -> 复审或重新补交 -> 完整率刷新 -> 导出预检查`。
- M1D 补充信息结构裁决：项目工作台必须按 `项目资产 -> 工程主数据 -> 交付工作中心` 呈现；工作中心不是工程主数据子功能，但必须排在工程主数据之后；工程主数据未就绪时，工作中心提示先生成 / 确认工程主数据草案。
- 105 项目只作为样本之一；能力必须同样适用于 93/506 或其他真实 NAS 项目，不允许为 105/503 写死逻辑。
- M1D 禁止：新增 Hermes 能力、继续 G4、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 主 agent 计划：`handoff/main-agent/m1d-standard-delivery-loop-plan.md`。
- 信息结构补充裁决：`handoff/main-agent/m1d-workspace-ia-correction.md`。

## 2026-05-20 M1C 收口

- `M1C：工程主数据真实项目落地` 已通过测试 agent 验收并正式收口。
- 当前无 P0 / P1 / P2。
- 已确认 105/503 与 93/506 两个真实 NAS 项目均可完成接入评估与草案预览。
- 草案预览已显示 catalog-only、证据来源、置信度、风险提示和人工确认要求。
- 未确认 apply 会被拒绝；确认 apply 后保持幂等，不覆盖已有主数据。
- 未泄露真实 NAS 路径、raw row、SQL、token、secret、password。
- 本轮未新增 Hermes 能力，未继续 G4，未进入 8B / 8C / 9A，未触碰真实 NAS 写操作。
- 主线健康度：`绿灯可继续主线开发`。
- 下一批建议：`M1D：标准驱动交付闭环强化`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1c-real-project-masterdata-closure.md`。

## 2026-05-20 M1C 启动

- 用户确认进入 `M1C：工程主数据真实项目落地`。
- 当前 active 批次：`M1C`。
- M1C 目标：把真实项目接入向导、初始化向导、部位树、节点类型和交付物标准，从“模板演示”收敛为真实 NAS 项目可用的工程主数据落地流程。
- M1C 主链路：`真实项目资产 -> 接入评估 -> 草案预览 -> 人工确认 -> 创建/补齐工程主数据 -> 进入交付闭环`。
- 105 项目只作为样本之一；能力必须同样适用于 93/506 或其他真实 NAS 项目，不允许为 105/503 写死逻辑。
- M1C 禁止：新增 Hermes 能力、继续 G4、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 主 agent 计划：`handoff/main-agent/m1c-real-project-masterdata-plan.md`。

## 2026-05-20 M1B 收口

- `M1B：项目工作台与数据管家可用性收口` 已通过测试 agent 验收并正式收口。
- 当前无 P0 / P1 / P2。
- 资产总览默认真实项目统计已恢复可读，不再明显为 0。
- 项目工作台主路径已从 Hermes / 交付治理助手收回到平台本体：
  - 数据管家。
  - 工程主数据。
  - 工作中心。
- Hermes 继续冻结，仅保留既有辅助入口；不作为当前主线继续扩展。
- 当前不进入 G4 / 8B / 8C / 9A。
- 主线健康度：`绿灯可继续主线开发`。
- 下一批建议：`M1C：工程主数据真实项目落地`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1b-project-workbench-usability-closure.md`。

## 2026-05-20 M1B 启动

- 用户确认进入 M1B。
- 当前 active 批次：`M1B：项目工作台与数据管家可用性收口`。
- M1B 目标：让普通员工能看懂资产总览、项目状态、项目工作台入口和下一步动作。
- M1B 重点：
  - 资产总览 Hero 区和项目状态表达。
  - 真实项目统计显示问题。
  - 项目来源、接入状态、主数据状态、交付状态和下一步动作。
  - 项目工作台内数据管家 / 工程主数据 / 工作中心用途说明。
- M1B 禁止：新增 Hermes 能力、继续 G4、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 主 agent 计划：`handoff/main-agent/m1b-project-workbench-usability-plan.md`。

## 2026-05-20 M1A 收口与主线健康恢复

- `M1A：平台主线功能基线审计与交付闭环缺口收束` 已通过测试 agent 复验并正式收口。
- 当前无 P0 / P1。
- 已关闭：
  - 登录超时 P1。
  - Catalog 详情路径脱敏 P1。
  - 旧文件资源路径脱敏 P1。
- 主线健康度：`绿灯可继续主线开发`。
- 注意：绿灯只表示主线恢复健康，可以继续 M 系列；不表示平台已达到客户交付完整度，不表示进入 9A。
- 当前不进入 G4 / Hermes 新能力 / 8B / 8C / 9A。
- 下一批建议：`M1B：项目工作台与数据管家可用性收口`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1a-platform-baseline-closure.md`。

## 2026-05-20 Git 主线治理与 Hermes 冻结裁决

- 用户裁决：暂停 G4 开发。
- 用户裁决：Hermes 定位已经出现偏移，必须立刻冻结；后续 Hermes 重新对齐后，通过独立分支继续完善。
- 用户裁决：主线不能继续卡死在 Hermes，必须恢复平台功能本身的完善。
- Git 治理已执行：
  - 创建冻结分支 `codex/hermes-g3-g4-freeze`，保留当前 Hermes/G3/G4 状态。
  - 将 `main` 快进到当前平台成果点。
  - `codex/nas-real-project-import-pr` 保留为历史开发分支，不再作为主线继续推进。
- 当前 active 主线不再是 G4；G4 计划仅保留为历史方案。
- 当前建议主线进入 `M1A：平台主线功能基线审计与交付闭环缺口收束`。
- M1A 重点：项目工作台、资产总览、文件管理、工程主数据、标准驱动交付、文档/图纸交付、审核整改、导出预检查、权限审计和路径脱敏。
- M1A 禁止：新增 Hermes 能力、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、BIM 真实轻量化、Agent 自动治理。
- 主线治理说明：`handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`。
- 当前开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 当前测试 prompt：`handoff/test-agent/current-prompt.md`。

## 2026-05-20 主线后续开发路线重评估

- 当前主线从 `继续 Hermes / 继续 G4 / 准备 9A` 正式改为 `平台本体能力补齐优先`。
- 主线健康度：`黄灯可控`。
  - Git 已回到 `main`。
  - 现有能力基线较多。
  - 但路线文档存在旧口径残留，且平台闭环尚未完成最新 M1A 验证。
- `9A 客户交付准备` 现在不启动；平台功能还远未达到客户交付完整度。
- `8B 真实 BIM 引擎接入` 后置；在 BIM 引擎厂商未确定前，先做 `8B-0：BIM 引擎选型与接入前置评估`。
- 当前路线：
  - `M1：平台本体稳定期`，覆盖 M1A/M1B/M1C/M1D。
  - `M2：客户版核心业务补齐期`，覆盖数据管家深化、权限审计、交付包与档案目录。
  - `M3：BIM 引擎前置准备期`，先做 8B-0，再根据引擎选型进入真实接入。
  - `M4：客户交付准备期`，9A 后置到平台功能和 BIM 路线稳定后。
- Hermes 保留现有入口但继续冻结；后续通过独立 `codex/hermes-*` 分支重新定义定位和验收边界。
- 后续平台开发默认从 `main` 拉 `codex/platform-*` 分支。

## 2026-05-20 G4 路线裁决

- 状态：已暂停。以下为历史记录，不再作为当前 active 开发批次。
- G3 已收口并完成 Git checkpoint。
- 当前不进入 8B / 8C / 9A。
- 8B BIM 轻量化任务编排继续后置。
- 当前下一批固定为 `G4：真实项目交付闭环试运行与问题修补`。
- G4 定位：不是继续增加平台内容，而是用 105 和至少另一个真实 NAS 项目跑通现有数字化交付闭环，发现并修复阻塞真实员工使用的 P0/P1/P2。
- G4 主链路：真实项目资产入口 -> 项目工作台 -> Hermes 工程主数据计划 -> Hermes 交付缺失项推荐 -> 人工确认推荐挂接 -> 文档/图纸交付状态刷新 -> 审核/整改 -> 导出预检查 dry-run -> 审计留痕。
- G4 允许修补：项目上下文、真实项目入口、页面刷新、交互提示、空状态、权限/Missing Evidence 解释、专项脚本和小范围只读聚合。
- G4 禁止：真实 BIM 轻量化、构件级解析、正文抽取、selective indexing、真实 NAS 增删改查、Hermes memory / 向量库写入、Agent 未确认自动治理、为 105 写死逻辑。
- G4 主 agent 规划：`handoff/main-agent/phase2-g4-real-project-delivery-trial-plan.md`。
- G4 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- G4 测试 prompt：`handoff/test-agent/current-prompt.md`。

## 2026-05-20 G3 路线裁决

- G2 已收口并完成 Git checkpoint。
- 当前不进入 8B / 8C / 9A。
- 8B BIM 轻量化任务编排后置。
- 用户裁决：Hermes 不能长期只是平台问答 AI，必须作为嵌入平台的 Agent，真实帮助员工完成数字化交付工作。
- 当前下一批固定为 `G3：Hermes 平台工作型 Agent MVP`。
- G3 目标：让 Hermes 能读取项目状态、生成工程主数据补齐计划、生成交付缺失项补交方案，并在人工确认后调用平台已有能力执行受控动作。
- G3 不是新平台模块扩张批次；不得做 BIM 轻量化、真实模型转换、构件级解析、正文抽取、selective indexing、NAS 增删改查、Hermes memory 写入或未确认自动治理。
- G3 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- G3 测试 prompt：`handoff/test-agent/current-prompt.md`。
- G3 主 agent 规划：`handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`。

## 2026-05-20 G3 收口裁决

- G3 已通过测试 agent 验收。
- 当前无 P0 / P1。
- 仅保留既有前端 Vite chunk size warning，不阻塞。
- G3 已实现 Hermes Action Center，包含回答、操作草案、待人工确认和执行结果。
- Hermes 可生成工程主数据补齐计划、交付缺失项补交 / 文件挂接推荐方案。
- 未人工确认时不能执行写动作；人工确认后通过平台既有推荐挂接能力执行。
- 执行链路保留平台权限校验和审计。
- Missing Evidence 边界保持：正文 / DWG / RVT / BIM 构件类问题不编造。
- 未进入 8B / 8C / 9A，未做真实 BIM 轻量化、正文抽取、NAS 增删改查、Hermes memory 或向量库写入。
- 主 agent 裁决：G3 可以收口；下一阶段不自动进入 8B / 8C / 9A，需用户另行确认。

## 2026-05-20 G2-B 收口裁决

- `G2-B：既有真实项目治理可用性补丁` 已通过测试 agent 短回归。
- 上一轮 Hermes 前端问答 `timeout of 10000ms exceeded` P1 已关闭。
- 当前 G2-B 无 P0 / P1 / 新增 P2。
- 真实 Hermes 已保持外部接入，10 秒以上回答可正常展示；前端仍通过平台后端 `/api/data-steward/chat`，未直连 Hermes。
- 正文 / DWG / RVT / BIM / 构件类问题仍返回 Missing Evidence，不编造正文或模型内容。
- G2-B 可以收口；下一步建议进入 G2 整体收口与 Git checkpoint 前的变更分组审计。
- 注意：当前工作区存在较多历史未提交改动和未跟踪文件，Git checkpoint 前必须先分组确认，不建议直接全量提交。
- Git checkpoint 前审计文件：`handoff/main-agent/g2-git-checkpoint-audit.md`。
- 主 agent 建议：优先采用分组 checkpoint；如用户更重视快速保护当前成果，可采用完整阶段 checkpoint，但需明确这不是干净小 PR。

## 2026-05-15 当前主视角

项目主视角已从 `一期内部 BIM 资产管理试点` 切换到 `二期客户交付版`。

一期后端数据库、NAS 资产接管、SQL View、事件流、权限审计和删除隔离等能力已经收口，后续一期只处理 P0/P1 回归或真实 NAS 数据治理必要修复。

二期当前已完成并可作为基线的能力：

- 批次一：只读资产目录、REST 权限证明、Agent preview / audit-ready 页面。
- 批次二：标准驱动交付闭环最小可用版。
- 批次三：人工审核、整改闭环、基础报表导出。
- 项目工作台导航重组。
- 数据管家资产驾驶舱和 RealBIM 风格文件管理第一批。
- 文件管理三项优化短回归已通过：目录树宽度可调、缺 checksum 受控补算入口、更多菜单去重，当前无 P0/P1/P2。
- 批次 4R：文件访问安全闭环复验与收口已通过测试 agent 专项复验，当前无 P0/P1，仅有既有 Vite chunk size warning P2。
- 批次 5A：Hermes 数据管家内嵌 v0 已通过测试 agent 专项复验并正式收口。
- 批次 5A.1：Hermes Gateway 合同对齐与联调增强已通过测试 agent 专项复验并正式收口。
- 批次 5B：数据管家客户版模块补齐已通过测试 agent 短回归并正式收口。
- 批次 6A：项目初始化评估与建筑机电/BIM交付基础模板预览/套用已通过测试 agent 专项验收并正式收口。
- 批次 6B：批量挂接交付与交付包准备视图已通过测试 agent 专项短回归并正式收口。
- 批次 7A：文件预览策略统一与交付包导出预检查已通过测试 agent 专项验收并正式收口。
- 批次 7B：文件预览转换体验增强已通过测试 agent 专项验收并正式收口。
- 批次 8A：BIM 轻量化适配层与 Mock 预览入口已通过测试 agent 专项验收并正式收口。
- 插入批次 G1：Agent 引导式交付治理 MVP 已通过测试 agent 专项验收并正式收口。

二期新的路线入口：

- `docs/10-phase2-development-roadmap.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`

当前插入批次：

1. 插入批次 G1 已正式收口。
2. G2 已正式收口并完成 Git checkpoint。
3. G3 已正式收口并完成 Git checkpoint。
4. G4 已暂停，计划仅作为历史方案保留。
5. 当前 active 主线为 `M1A：平台主线功能基线审计与交付闭环缺口收束`。
6. 105 项目仅作为抽查样本之一，能力必须适用于所有已接管真实 NAS 项目。
7. 本轮仍不进入 8B / 8C / 9A，不开放真实 NAS 增删改查，不做真实 BIM 轻量化，不做正文抽取，不做 selective indexing，不允许 Agent 自动治理。
8. 后续是否进入 8B / 8C / 9A、NAS 受控增删改查、构件级能力、Hermes selective indexing、正文抽取和受控写操作，均需用户再次明确确认。
9. Hermes 后续接入按 `Catalog Layer -> Evidence Layer -> Memory Layer -> Orchestration Layer` 推进。G4 只验证并修补当前 catalog-only + 受控平台动作闭环，不进入正文证据、长期记忆或多 Agent 编排实现。

后续日常开发默认读取 `docs/07`、`docs/08`、`docs/03`、`docs/10`、`handoff/main-agent/phase2-current-roadmap.md`、`handoff/main-agent/hermes-jarvis-coupling-roadmap.md`、`handoff/dev-agent/latest-report.md`、`handoff/test-agent/latest-report.md`。`docs/01` 到 `docs/06` 和大量一期 handoff 报告默认作为历史资料，不再反复加载。

## 当前阶段

文档与验收口径已升级为三期路线，当前已完成 `一期：内部 BIM 资产管理试点` 的主线收口。`一期内部试运行可用性与验收完整度短回归` 已通过测试 agent 极短复验，当前无 P0/P1/P2。`二期批次一：只读资产目录与 Agent 对接预览层` 已通过测试 agent 短回归，正式收口。`二期批次二：标准驱动交付闭环最小可用版` 已通过测试 agent 验收，当前无 P0/P1/P2，正式收口。`二期批次三：人工审核、整改闭环与基础报表导出` 已通过测试 agent 正式验收，当前无 P0/P1，仅有不阻塞收口的 Vite chunk size warning P2，正式收口。`标准驱动交付流程新手可用性补丁` 已通过测试 agent 短回归，上一轮 P1 已关闭，正式收口。`导航与项目工作台重组` 已完成开发、返修与短回归，作为当前前端项目工作台基线保留。`二期批次四/4R：文件访问安全闭环` 已通过测试 agent 专项复验，正式收口。

当前最新裁决：二期批次 5B 已通过测试 agent 短回归并正式收口。用户提供的 V3 Hermes 前端网关耦合文档已纳入路线，Hermes 当前口径更新为 `phase-2.89-test-machine-runtime-preflight-handoff-baseline`。5B 首轮 P0“导出列表和 `catalog/files.logicalPath` 泄露 `/Volumes/...`”已关闭，`logicalPath` 已统一转为项目内路径，导出和任务失败原因已做兜底脱敏。2026-05-18 已按 V3 文档补强平台侧 Hermes 接入：`/api/data-steward/chat` 兼容 V3 请求格式，新增 `/api/data-steward/catalog/search` 只读资产目录 metadata preview，并在前端 Hermes 面板展示资产目录预览；Hermes 正式命名已收束，前端用户侧、后端 capabilities 和新审计 action 均统一为 Hermes；前端不再向 catalog search 发送可信 `project_scope`，只发送普通 `projectFilters`，可信范围由平台后端 Gateway 生成；专项脚本 `check-hermes-jarvis-gateway.sh` 已通过 `PASS=11 FAIL=0`。`批次 6A：项目初始化评估 + 建筑机电/BIM交付基础模板预览/套用` 已通过测试 agent 专项验收并正式收口，收口报告为 `handoff/main-agent/phase2-batch6a-project-initialization-closure.md`。

## 当前判断

平台基础工程、`master-data`、数据管家 MVP、工作中心 MVP、智慧大屏和 3D 适配层 mock 已形成可运行闭环。现在项目重点不再是继续扩早期 MVP，而是把公司内部几百个 BIM 项目和约 10TB 模型资产先接入平台，并提前为企业内核级 agent 做数据库检索和接口对接准备。

最新裁决：一期后端数据治理正式收口，一期内部试运行可用性验收收口。前端已补齐资产总览、项目资产详情、扫描任务、非标准资料治理、数据质量、文件详情、人工治理和文件预览状态外壳等一期试运行页面；测试 agent 极短复验确认 DB-2 只读合同、首页项目下拉和扫描脚本自清理均通过，当前无 P0/P1/P2。后续不再继续扩大一期后端治理范围，除非发现 P0/P1 回归。

当前最新主线：二期客户交付版继续推进。导航与项目工作台重组已作为前端基线完成，文件访问安全闭环 4R、Hermes 数据管家 5A、Hermes Gateway 合同对齐 5A.1、数据管家客户版模块补齐 5B、项目初始化与标准模板化 6A、批量挂接交付与交付包准备视图 6B、文件预览策略统一与交付包导出预检查 7A、文件预览转换体验增强 7B、BIM 轻量化适配层与 Mock 预览入口 8A、Agent 引导式交付治理 MVP G1 均已正式收口。G1 用于降低真实 NAS 项目进入交付闭环的门槛；继续禁止真实 NAS 写操作、正文抽取、真实模型轻量化和 Agent 自动治理，除非进入后续明确批次。根因已经确认：平台曾同时存在 token 当前项目、顶部全局项目切换器、项目详情路由和全局主数据/工作中心菜单，导致用户感受不到“先进入项目，再在项目内工作”。主 agent 已产出规划与 agent prompt，并已完成开发、审计和测试报告 P1/P2 返修：

- 规划文件：`handoff/main-agent/navigation-project-workbench-reorg-plan.md`
- 开发 prompt：`handoff/dev-agent/current-prompt.md`
- 测试 prompt：`handoff/test-agent/current-prompt.md`

本轮推荐方案是“项目壳层改造”：`/data-steward/assets` 作为默认入口，`/data-steward/assets/:projectId` 升级为项目工作台，工作台顶部承载工程主数据和工作中心入口，文件资产区复用 catalog 目录能力改为左目录右文件，顶部全局项目切换器移除或隐藏。当前 P1/P2 返修结果：左侧全局菜单已进一步收紧，不再展示 `首页 / 工程主数据 / 工作中心` 顶级入口；项目工作台顶栏已展示负责人，为空时显示 `待维护`。

二期批次一最新状态：Claude Code 开发 agent 已完成实现并写入 `handoff/dev-agent/latest-report.md`；主 agent 审计发现并修复过一次无权限 `audit-context` 返回 500 的问题。测试 agent 首轮验收无 P0，但指出 P1：前端资产目录页缺少“按目录浏览 + 版本筛选”的完整页面交互。该 P1 首次补齐后，测试 agent 二次短回归又发现 2 个真实联调 P1：前端分页字段未按真实 `items/pageNo/pageSize/total` 读取，目录接口返回文件路径级记录而非目录聚合。当前这些 P1 已全部修复，并已通过测试 agent 2026-05-13 短回归。最终验证：专项脚本 `scripts/dev/check-phase2-batch1-readonly-catalog.sh` `25/25 PASS`，真实页面不再空表，`catalog/directories` 返回目录聚合，目录浏览和版本筛选可用。收口报告：`handoff/main-agent/phase2-batch1-readonly-catalog-closure.md`。

当前最新状态：`批次 1：数据底座与 NAS 扫描入库`、`批次 1 尾巴：真实 NAS 目录发现、扩展文件分类、低价值文件治理、重扫幂等与历史数据补齐`、`批次 2：异步任务、checksum、统计与事件流`、`批次 3：企业 agent、审批与受控物理删除` 均已通过测试 agent 复验，当前无 P0/P1/P2。真实 NAS 已完成 16 个项目接管，登记文件 `40,935` 份，扫描任务进度、取消、续扫、低价值目录跳过和报告能力已补强并通过专项脚本验证。最新极短复验确认：`check-agent-db2-contract.sh` 通过，首页项目下拉 `18` 个项目且无 `SCANCTRL-*`，`check-scan-task-control.sh` 复跑后不再污染项目下拉。

## 最新文档基线

- 完整 PRD 与三期路线：`docs/07-complete-delivery-prd.md`
- 验收矩阵与企业 agent 对接：`docs/08-acceptance-and-agent-integration.md`
- 总体架构与系统设计：`docs/03-architecture-and-system-design.md`
- 实施路线与 agent 手册：`docs/04-rollout-and-agent-prompts.md`

## 一期业务目标

- 项目资产台账：几百个 BIM 项目统一建档，优先通过后端 API 和批量导入维护。
- NAS 原地接管：不搬迁大文件，只登记元数据和路径。
- 真实 NAS 根路径：`/Volumes/zyzn/卓羽智能项目`，采用只读影子导入，不移动、不改名、不删除原文件。
- 文件范围：一期扫描 `.rvt`、`.dwg`、`.ifc`、`.nwd`、`.nwc`、`.dxf`、`.pdf`。
- 模型/图纸资源库：记录文件名、格式、大小、版本、专业、checksum、状态、更新时间和项目归属置信度。
- 审核队列：高置信自动入库，低置信进入待审核，人工可修正项目归属、文件类型、专业和版本。
- 异步任务：扫描、checksum、回收站清理、永久删除采用数据库任务表 + 应用内 worker。
- 搜索与看板：支持按项目、文件名、专业、阶段检索，展示容量统计。
- 审计与事件流：导入、扫描、审核、修改、删除申请、agent 行为和任务状态变化必须留痕，并支持事件流增量同步。
- 企业 agent：提供 MySQL SQL View、REST/OpenAPI、API Key 项目范围授权，支持 `元数据 + 路径` 检索。
- 删除安全：逻辑删除不碰 NAS 文件；物理删除必须申请、审核、隔离 30 天、支持恢复后再永久删除。

## 一期后端批次

1. 批次 1：数据底座与 NAS 扫描入库。覆盖项目清单、NAS 路径映射、CSV/xlsx 导入、扫描任务、待审核队列、正式资产库和 SQL View 基础。状态：测试 agent 复验通过，无 P0。
2. 批次 1 尾巴：真实 NAS 目录发现、文件类型扩展、低价值文件治理、重扫幂等与历史 `confidence_level` 补齐。状态：主 agent 审计通过；`check-bim-asset-batch1.sh` 与 `check-bim-asset-batch1-tail.sh` 均已通过。
3. 批次 2：异步任务、checksum、统计与事件流。覆盖数据库任务表、应用内 worker、checksum 异步补齐、容量统计和审计/事件流增量同步。状态：测试 agent 复验通过，无 P0。
4. 批次 3：企业 agent、审批与受控物理删除。覆盖 API Key、项目范围授权、agent 提交申请、人工审批、逻辑删除、隔离恢复和受控永久删除。状态：测试 agent 再次验证通过，当前无 P0/P1/P2。

一期后端数据治理收口报告：`handoff/main-agent/phase1-backend-data-governance-closure.md`

## 二期客户版目标

- 标准驱动交付完整闭环。
- 文件预览、下载权限分离、审核流。
- 模型轻量化在线预览。
- 构件级解析、搜索、定位、高亮和基础查看工具。
- 项目驾驶舱、报表、大屏、移动端查看。
- 客户私有化部署、备份恢复、日志诊断、健康检查和完整交付文档。

## 二期批次一状态

- 批次名称：`只读资产目录与 Agent 对接预览层`。
- 规划文档：`handoff/main-agent/phase2-batch1-readonly-catalog-plan.md`。
- 本批只做：
  - 前端资产目录。
  - REST 权限证明。
  - 只读 catalog API。
  - Agent preview / audit-ready 页面。
- 本批不做：
  - Agent 直接增删改数据库。
  - Agent 自动移动、删除、修复 NAS 文件。
  - Agent 自动把 BIM、PDF、Office 或其他文件正文写入向量库。
  - Agent 自动下结论或自动审批。
  - 多 Agent 调度真实业务动作。
  - 面向客户的生产级权限体系承诺。
- 二期批次一开发 prompt 与测试 prompt 已写入：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 开发 agent 已按 Ralph Loop 完成本批只读目录能力，进度记录：`.claude/ralph/progress.txt`。
- 开发 agent 报告：`handoff/dev-agent/latest-report.md`。
- 主 agent 审计结果：通过。已修复无权限 `audit-context` 返回 500 的问题，并在专项脚本中补充回归断言。
- 测试 agent 首轮验收结果：无 P0，但发现 P1：前端资产目录页未完整实现目录浏览和版本筛选。
- P1 修复状态：已完成并通过测试 agent 短回归。`/data-steward/catalog` 已支持左侧目录浏览、目录点击过滤和版本筛选；前端文件列表已对齐真实分页字段 `items/pageNo/pageSize/total`；后端目录接口已改为父目录聚合；后端 catalog 文件查询已支持 `directoryPath` 与 `version` 参数；专项脚本已补充断言并 `25/25 PASS`。
- 收口状态：二期批次一正式收口，当前无 P0/P1/P2。
- 下一步裁决：用户已确认进入二期批次二规划，但仍不直接进入 selective indexing、正文抽取、Agent workflow、受控写操作、模型轻量化或构件级能力。

## 二期批次二状态

- 批次名称：`标准驱动交付闭环最小可用版`。
- 规划文档：`handoff/main-agent/phase2-batch2-standard-delivery-plan.md`。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 本批核心链路：`项目上下文 -> 部位树 -> 节点类型锁定 -> 交付物标准 -> 交付物类型 -> 文件资源 -> 文档/图纸挂接 -> 交付完整率 -> 缺失项清单 -> 审计留痕`。
- 本批只做：交付完整率接口、标准前置缺口提示、文档/图纸缺失项列表、从缺失项发起挂接、文件选择分页/检索、专项验收脚本。
- 本批不做：模型轻量化、构件级解析、正文抽取、向量库、企业 Agent 写操作、自动审批、完整审核流、真实 NAS 文件移动/删除/改名。
- 开发 agent 必须使用 Ralph Loop，完成承诺固定为 `<promise>PHASE2_BATCH2_STANDARD_DELIVERY_COMPLETE</promise>`。
- 当前状态：正式收口。Claude Code 开发 agent 已使用 Ralph Loop 完成本批开发并写入 `handoff/dev-agent/latest-report.md`。主 agent 审计时中断过两次偏移：一次阻止进入 core 项目切换接口，一次恢复专项脚本中节点类型锁定的真实失败断言。主 agent 复核后补回前端 `DashboardSummary` 类型定义，避免前端构建依赖隐式缺失类型。
- 主 agent 本轮验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh` 已加固为复跑优先复用已有节点类型，并清理本轮创建的部位节点；脚本通过，`38/38 PASS`。
  - `git diff --check` 通过。
- 测试 agent 正式验收通过，当前无 P0/P1/P2。收口报告：`handoff/main-agent/phase2-batch2-standard-delivery-closure.md`。
- 下一步建议：进入二期批次三规划，优先考虑 `审核流、整改闭环、报表导出`，仍不进入模型轻量化、构件级解析、正文抽取、Agent workflow 或受控写操作。

## 二期批次三状态

- 批次名称：`人工审核、整改闭环与基础报表导出`。
- 规划文档：`handoff/main-agent/phase2-batch3-review-rectification-report-plan.md`。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 本批核心链路：`交付挂接 -> 人工审核 -> 驳回产生整改 -> 整改处理 -> 复审关闭 -> 报表导出 -> 审计留痕`。
- 本批只做：交付绑定人工审核、审核记录、驳回原因、整改项、整改状态闭环、基础 CSV 报表导出、权限审计和专项验收脚本。
- 本批不做：完整 BPM/流程引擎、多级会签、客户生产级权限承诺、Agent 自动审批/自动整改/自动写库、模型轻量化、构件级解析、正文抽取、向量库、真实 NAS 文件移动/删除/改名。
- 开发 agent 必须使用 Ralph Loop，完成承诺固定为 `<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`。
- 当前状态：正式收口。开发 agent 已使用 Ralph Loop 完成本批开发并写入 `handoff/dev-agent/latest-report.md`；主 agent 已完成代码审计和本地复核，未发现 P0/P1。测试 agent 已完成正式验收并建议收口，当前无 P0/P1，仅有 Vite chunk size warning P2。收口报告：`handoff/main-agent/phase2-batch3-review-rectification-report-closure.md`。
- 主 agent 本轮验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh` 通过，`52/52 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh` 回归通过，`38/38 PASS`。
  - `git diff --check` 通过。
- 测试 agent 正式验收：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过。
  - `git diff --check` 通过。
  - 批次三专项脚本 `52/52 PASS`。
  - 批次二回归 `38/38 PASS`。
  - 批次一只读目录回归 `25/25 PASS`。
  - DB-2 只读合同回归通过。
  - `/work/document-delivery`、`/work/drawing-delivery`、`/work/rectifications` 页面验收通过，页面级横向溢出为 `0`。
- 当前裁决：二期批次三正式收口。下一步建议进入二期批次四规划，优先考虑 `文件预览与下载权限分离最小闭环`，仍不进入模型轻量化、构件级解析、正文抽取、向量库、Agent workflow 或受控写操作。

## 标准驱动交付流程新手可用性补丁

- 触发原因：测试 agent 报告指出标准驱动交付链路对新手缺少业务解释、顺序引导和修复入口提示，优先级为 P1。
- 当前状态：正式收口。测试 agent 短回归通过，当前无 P0/P1，仅有既有 Vite chunk size warning P2。
- 开发报告：`handoff/main-agent/standard-delivery-usability-patch-report.md`。
- 收口报告：`handoff/main-agent/standard-delivery-usability-patch-closure.md`。
- 开发报告：`handoff/dev-agent/latest-report.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 已处理页面：
  - `/master-data/sections`
  - `/master-data/node-types`
  - `/master-data/deliverable-standard`
  - `/work/document-delivery`
  - `/work/drawing-delivery`
- 修复内容：
  - 增加第 1、2、3 步业务说明。
  - `StandardStatusPanel` 增加下一步提示。
  - 解释部位树、节点类型锁定、交付物标准四块配置关系。
  - 交付页解释应交项、已挂接和缺失项。
  - 标准未就绪时增加修复入口。
  - 将“去挂接”调整为“选择文件补交”。
  - 补交弹窗增加字段辅助说明。
- 本轮验证：
  - 前端构建通过。
  - 后端健康检查 `UP`。
  - 五个目标路由均返回 HTTP 200。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `git diff --check` 通过。
- 测试 agent 短回归：
  - 五个目标页面逐项通过。
  - 交付页横向溢出为 `0`。
  - 文件选择仍为分页远程加载。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
- 未做项：
  - 未清理样板项目历史测试命名数据。
  - 未补齐样板项目文档类标准数据。
  - 未做标准配置向导或客户版帮助中心。
- 用户最新裁决：样板项目仍需用于业务链路测试，暂不清理、暂不重置、暂不隐藏历史测试命名。
- 下一步建议：恢复二期主线推进，进入 `二期批次四：文件预览与下载权限分离最小闭环` 规划；样板项目整理仅作为后续演示观感优化保留，不作为当前阻塞项。

## 二期批次四状态

- 批次名称：`文件预览与下载权限分离最小闭环`。
- 规划文档：`handoff/main-agent/phase2-batch4-file-preview-download-permission-plan.md`。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 本批核心链路：`文件详情 -> 判断预览/下载权限 -> 生成短时访问票据 -> 平台受控读取文件 -> 浏览器预览或下载 -> 审计留痕`。
- 本批只做：PDF/图片浏览器原生预览入口、下载权限与预览权限分离、短时访问票据、NAS 只读流式读取、访问审计和专项验收脚本。
- 本批不做：样板项目清理、模型轻量化、构件级解析、Office/CAD/BIM 转换、正文抽取、向量库、Agent workflow、自动审批、真实 NAS 写操作或客户生产级权限体系承诺。
- 开发 agent 必须使用 Ralph Loop，完成承诺固定为 `<promise>PHASE2_BATCH4_FILE_ACCESS_COMPLETE</promise>`。
- 当前状态：正式收口。测试 agent 已完成 4R 文件访问安全闭环专项复验，结论为通过，当前无 P0/P1，仅有既有 Vite chunk size warning P2。
- P0 返修内容：
  - `catalog/files` 列表和 `catalog/files/{fileId}` 详情按项目角色控制路径可见性。
  - 仅 `PROJECT_ADMIN` 可见真实 `storagePath`。
  - `PROJECT_VIEWER`、`DELIVERY_ENGINEER` 等普通项目角色返回 `storagePathVisible=false`、`storagePath=null`、`storagePathVisibilityReason=PATH_HIDDEN_BY_PERMISSION`。
  - `agentContractView` 在路径隐藏时不再声明 `storagePath` 字段可用。
  - `scripts/dev/check-phase2-batch4-file-access.sh` 已补充查看者和交付工程师的路径隐藏断言。
- 主 agent P0 返修验证结果：
  - 后端构建通过。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=14 FAIL=0`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `git diff --check`：通过。
- 4R 测试 agent 复验结果：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过，实际验收端口为 `8080`。
  - 局部 `git diff --check` 通过。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`18/18 PASS`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：通过。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：通过。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：通过。
  - 普通项目用户不会在 catalog 列表、catalog 详情、项目文件详情中看到真实 NAS 路径。
  - 管理员必要路径可见性未被误伤。
  - 预览/下载权限分离有效，预览票据不会跨动作变成下载响应。
  - 文件访问成功、拒绝、失败均有审计，未发现 secret、真实路径或 NAS 绝对路径泄露。
- 当前裁决：二期批次四/4R 正式收口。下一步可进入 `批次 5A：贾维斯数据管家内嵌 v0`，或若用户希望先补业务模块，则进入 `批次 5B：数据管家客户版模块补齐`。

## 企业 Agent 对接裁决

- agent 权限高，可以读取稳定 SQL View，但不得直接耦合业务底表。
- agent 不应直接耦合平台业务底表。
- 一期必须提供稳定读模型：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
- agent REST/OpenAPI 接入采用 API Key。
- API Key 按项目范围授权，可授权全部项目或指定项目。
- agent 可查询、触发扫描/checksum 任务、提交标注或删除申请；正式变更必须人工审核。
- 平台保留 REST/OpenAPI 作为长期集成边界。
- 增量同步采用审计/事件流，不只依赖 `updated_at`。
- 一期落地前主 agent 必须再次确认企业 agent 技术栈、读库方式、索引方式、路径访问方式和事件流同步策略。

## 当前协作规则

- 主 agent 维护需求、架构、验收、prompt 和交接文档。
- 开发 agent 和测试 agent 仍作为长期独立会话运行。
- 主 agent 不通过创建临时子 agent 代替开发 agent。
- 后续启动 Claude Code 开发 agent 时默认使用 Claude CLI 可识别的 Pro 型号 `deepseek-v4-pro`；用户口径中的 `deepseek-pro` 按 Pro 能力要求理解，不再使用 `deepseek-chat` 作为正式开发模型。
- 后续开发 agent prompt 必须引用 `docs/07` 和 `docs/08`，不能只按旧 v1 MVP 口径继续开发。
- 一期后端批次 1、批次 1 尾巴、批次 2、批次 3 已全部通过测试 agent 复验，一期后端数据治理正式收口。批次 3 已实现 API Key、agent 边界、删除申请审批、逻辑删除、隔离恢复和受控永久删除；后续不得在无新指令情况下继续扩大一期后端范围，不得扩前端、不得做二期 BIM 能力、不得绕过审批直接删除 NAS 原文件。

## 样板信息

- 样板账号：`platform.admin`
- 样板密码：`Admin@123`
- 样板项目：`SAMPLE-MEP-001 / 机电交付样板项目`

## 当前机器与迁移注意事项

当前继续在这台 Mac 上开发，上一轮已确认原生 JDK 21、后端构建、后端启动和健康检查可用。后续若迁移到新 Mac 或 Windows，仍需整理项目基线、数据库迁移说明、启动命令、已实现能力、未实现能力、企业 agent 对接待确认清单，避免跨系统开发断层。

## 真实 NAS 试点状态

- 真实 NAS 根路径：`/Volumes/zyzn/卓羽智能项目`。
- 用户明确要求：只允许读取数据，禁止修改或删除 NAS 数据。
- 主 agent 已完成第一轮只读发现，报告：`handoff/main-agent/real-nas-pilot-discovery-report.md`。
- 一级目录共 `27` 个；已发现 `95`、`99` 两组重复项目编号，不能直接按编号全量自动入库。
- 第一批正式小样本建议：`105-启航华居项目`、`100-深圳市二十八高项目`、`101-C塔`。
- `99-丰图既有建模项目` 文件量大且含大量临时/转换路径，暂不建议第一批正式写库；应先做 dry-run 和待审核策略验证。
- 平台 `dryRun=true` 的 NAS 项目发现接口已完成：总目录 `27`，`READY 17`，`CONFLICT 4`，`REFERENCE 3`，`NEEDS_CODE_REVIEW 3`；创建项目数与路径映射数均为 `0`。
- 第一批真实 NAS 元数据写入已完成，报告：`handoff/main-agent/real-nas-pilot-import-report.md`。
- 已完成项目：
  - `105-启航华居项目`：扫描任务 `414`，自动入库 `2927`，待审核 `0`，失败 `0`。
  - `100-深圳市二十八高项目`：扫描任务 `415`，自动入库 `3668`，待审核 `293`，失败 `0`。
- `101-C塔` 此前曾暂停导入：当时开发库已有历史测试项目编码 `101`，为避免真实 NAS 数据写入旧测试项目，扫描任务 `416` 已取消，误建路径映射 `683` 已删除，真实 `101` 文件资产写入数为 `0`。
- `100` 的 `293` 条待审核候选已全部审核通过，待审核清零；审核后保留 `LOW` 置信度和 `REVIEW` 来源。
- P1 已修复：新增 `V13__model_asset_view_include_nas_files.sql`，`ModelAssetView` 现在覆盖 NAS 扫描登记但尚未进入模型集成流程的模型文件。
- 验证结果：`ModelAssetView` 可查询 `100` 的 `156` 个模型、`105` 的 `198` 个模型。
- 本轮报告：`handoff/main-agent/real-nas-review-and-p1-report.md`。
- 真实 NAS 试点验收已完成并通过，报告：`handoff/main-agent/real-nas-pilot-acceptance-report.md`。
- 验收结论：`100`、`105` 已满足一期内部试点的核心要求：项目建档、元数据入库、路径追溯、检索统计、权限隔离、审计事件、SQL View 真实数据。
- 企业 agent 协议尚未确定，agent 联调暂缓。
- `101-C塔` 已完成清理与真实入库，报告：`handoff/main-agent/real-101-cleanup-and-import-report.md`。
- 历史测试项目 ID `99` 已归档为 `TEST-101-ARCHIVED-99` 并软删除；真实项目 ID `505` 复用编码 `101`。
- 真实 `101` 扫描任务 `417` 已成功：扫描 `5666`，自动入库 `5457`，待审核 `0`，失败 `0`。
- 真实 `101` 当前正式资产：模型 `114`，图纸 `5343`，总容量 `28,486,737,158 bytes`。
- 真实 `101` 路径抽样 `30/30` 存在且大小一致；`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均可查询。
- `delivery.engineer` 当前无法访问 `101` 项目和文件，权限隔离通过。
- 第二批真实 NAS 小样本已完成，报告：`handoff/main-agent/real-nas-second-batch-import-report.md`。
- 第二批项目：`93-中建八局国交酒店项目`、`96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`、`104-佛山顺德妇幼医院项目`。
- 第二批扫描结果：
  - `93`：扫描 `6305`，自动入库 `5912`，待审核 `0`，失败 `0`，模型 `588`，图纸 `5324`，容量 `77,923,356,233 bytes`。
  - `96`：扫描 `367`，自动入库 `243`，待审核 `0`，失败 `0`，模型 `23`，图纸 `220`，容量 `3,039,265,913 bytes`。
  - `104`：扫描 `1780`，自动入库 `1699`，待审核 `0`，失败 `0`，模型 `187`，图纸 `1512`，容量 `30,194,811,835 bytes`。
- 第二批路径抽样 `60/60` 存在且大小一致；SQL View、权限隔离、审计事件均通过。
- 当前真实试点已完成 `100`、`101`、`105`、`93`、`96`、`104` 共 `6` 个项目。
- 扫描任务可控性已补强，报告：`handoff/main-agent/scan-task-control-upgrade-report.md`。
- 已新增扫描进度、取消、续扫、低价值目录跳过、扫描报告和续扫去重保护；专项脚本 `scripts/dev/check-scan-task-control.sh` 已通过。
- 用户要求直接使用当前最大的真实 NAS 项目测试，已用 `93-中建八局国交酒店项目` 完成真实扫描控制验证，报告：`handoff/main-agent/real-largest-nas-scan-control-report.md`。
- 最大项目测试结果：扫描任务 `423` 可见进度、可取消、可续扫，最终 `6305/6305`，失败 `0`，正式资产数量和容量未重复增加。
- 用户要求暂时跳过 `95` 和 `99` 这类项目，继续导入其他未导入项目；第三批真实 NAS 导入已完成，报告：`handoff/main-agent/real-nas-third-batch-import-report.md`。
- 第三批新增 `97`、`108`、`109`、`110`、`111`、`112`、`113`、`114`、`115`、`116` 共 `10` 个真实项目，新增正式文件资产 `20736`，其中模型 `1338`、图纸 `19398`，总容量 `101,002,506,275 bytes`。
- 第三批扫描任务 `424-433` 全部 `SUCCEEDED`，失败 `0`，待审核 `0`；路径抽样 `100/100` 通过，SQL View 和权限隔离通过。
- 当前真实入库项目累计 `16` 个：`93`、`96`、`97`、`100`、`101`、`104`、`105`、`108`、`109`、`110`、`111`、`112`、`113`、`114`、`115`、`116`。
- 用户最新裁决：`98`、`95`、`99` 以及投标、参考、未知命名目录先不导入数据库，暂时忽略，不进入正式资产库。
- 这类非标准数据后续作为平台“Agent 辅助数据治理”能力处理：企业 agent 先做目录识别、文件归类、项目拆分、重复编号建议和风险提示，再由人工确认后导入。
- 非标准目录在人工确认前不得进入 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`，避免污染正式资产和企业 agent 检索结果。
- 一期真实资产接管总览已生成，报告：`handoff/main-agent/phase1-real-asset-overview.md`。
- 当前已接管 `16` 个真实项目，正式登记文件 `40,935`，其中模型 `2,604`、图纸 `38,331`，总容量 `317,849,121,433 bytes`，约 `296.02 GiB`。
- 前端已新增“资产总览”和“项目资产详情”：
  - 入口：数据管家 / 资产总览，路由 `/data-steward/assets`。
  - 默认只展示 `NAS_REAL*` 来源的真实 NAS 接管项目，避免批次验收测试项目污染业务视图。
  - 项目详情可查看文件资产、扫描任务和路径映射。
- 后端已增强一期资产查询：
  - 项目资产和容量统计支持 `assetSource=NAS_REAL*` 来源过滤。
  - 文件资产新增分页查询 `/api/data-steward/assets/files:page`。
  - 文件扩展名过滤兼容 `.rvt` 和 `rvt` 两种写法。
- 企业 Agent DB-2 本机 dev 握手文档已补齐：
  - 文件：`handoff/main-agent/enterprise-agent-db2-coupling-handoff.md`。
  - 本机 dev 只读账号 `hermes_agent_ro` 已创建，只能读四个稳定 View，不能读业务底表。
  - `hermes_agent_ro` 本机密码已轮换，当前密码不写入仓库文档，保存于 macOS 钥匙串 `delivery-platform-hermes-agent-ro-local-dev`。
  - DB-2 对接裁决矩阵已冻结并同步到 `docs/08-acceptance-and-agent-integration.md`、`handoff/main-agent/enterprise-agent-db2-coupling-handoff.md` 和 `handoff/main-agent/decisions.md`。
  - 当前明确：Agent 直连 MySQL View；不走同步库/搜索引擎/向量库；事件流由 Agent 按 `AuditEventView.event_id` 拉取；权限缺失默认 `DENIED`。
  - 当前明确：本机内部联调可临时读取真实项目名、文件名、NAS 路径，但不得进入长期 memory、向量库、搜索库、外部日志或客户材料；一期 DB-2 不读取 PDF/Office 正文。
  - 最新裁决：企业 Agent 后续会更新到本地 Hermes，并与平台数据库运行在同一台本机环境中；本机 `delivery-mysql` + `hermes_agent_ro` 是一期 DB-2 首轮正式联调路径。
  - 当前不需要立即获取 shared-dev / staging 账号；只有多人远程协作、持续测试、演示或客户交付前类生产验证时才触发运维开通。
  - 本机 dev 样例允许内部授权 `LIMIT 30`，但会暴露真实项目名、文件名和 NAS 路径。
  - shared-dev / staging / 客户环境已固化为默认 `STRUCTURE_ONLY`；未完成平台/运维 DSN 执行单、只读账号安全交付和业务负责人、数据负责人书面确认前，不允许真实样例读取。
  - shared-dev / staging 后置执行单：`handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md`。
  - Hermes mirror 层在四个 View 缺少稳定权限字段时必须默认 `DENIED`，真实项目名、文件名和 NAS 路径不得外发或写入外部云服务、向量库、搜索库、长期 memory 或外部观测日志。
- 当前本地验证：
  - 前端构建通过。
  - 后端构建通过。
  - 后端健康检查通过。
  - 管理员菜单可见资产总览入口。
  - 真实 NAS 过滤口径返回 `16` 项目、`40,935` 文件、`2,604` 模型、`38,331` 图纸。
- 下一步建议继续做“扫描任务运维页”或“非标准资料治理清单页”；企业 agent 协议确定前，不推进 agent 联调。
- 扫描任务运维页已完成：
  - 入口：数据管家 / 扫描任务，路由 `/data-steward/scans`。
  - 能力：扫描任务列表、状态筛选、项目筛选、关键字检索、创建任务、运行、取消、续扫、报告查看。
  - 已通过前端构建、后端构建、后端健康检查、菜单接口抽查和 `scripts/dev/check-scan-task-control.sh`。
- 扫描任务页显示修复已完成：
  - 新增 `V16__backfill_scan_task_display_fields.sql` 回填历史成功任务进度和缺失项目编码。
  - 历史 `SUCCEEDED` 扫描任务不再显示 `0%`，前端和后端均有完成态兜底。
  - 未绑定项目的历史/全局扫描任务显示为 `全局扫描 / 未绑定具体项目`，不再空白。
  - 抽查结果：`succeeded_zero_progress=0`、`project_id_without_code=0`、接口返回 `bad_succeeded_progress=0`。
- 非标准资料治理清单已完成，报告：`handoff/main-agent/nonstandard-directory-governance-report.md`。
  - 入口：数据管家 / 非标准资料治理，路由 `/data-steward/nonstandard-directories`。
  - 能力：只读发现暂缓入库、重复编号、投标/参考、临时资料、未知编码等一级 NAS 目录，并记录治理状态、风险类型、Agent 建议和人工结论。
  - 边界：不创建正式项目、不创建路径映射、不扫描文件、不写入正式文件资产，不污染 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`。
  - 验证：临时 NAS 目录发现、查询、人工更新、正式资产零泄露、事件和审计均已通过；临时测试治理记录已清理。
- 真实 NAS 非标准资料治理发现已执行，报告：`handoff/main-agent/real-nas-nonstandard-directory-governance-report.md`。
  - 根路径：`/Volumes/zyzn/卓羽智能项目`。
  - 已登记 `11` 个非标准目录：重复编号 `4`、投标/参考 `3`、未知编码 `3`、用户暂缓 `1`。
  - 重点目录：`95-*`、`99-*`、`98-深圳口岸项目`、投标资料、清华斯维尔围标项目、深城交等。
  - 防污染校验通过：正式项目数 `519 -> 519`，正式文件资产数 `48842 -> 48842`。
- 企业 Agent DB-2 只读合同自检脚本已完成，报告：`handoff/main-agent/agent-db2-contract-check-report.md`。
  - 脚本：`scripts/dev/check-agent-db2-contract.sh`，已写入 `scripts/README.md`。
  - 本机只读账号 `hermes_agent_ro` 自检通过，只能看到 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 四个稳定 View。
  - 当前 View 数量：项目 `519`，文件 `48842`，模型 `10322`，审计事件 `59849`。
  - 业务底表 `core_projects`、`data_file_resources`、`core_audit_logs` 均不可读。
  - 默认只验证结构、数量、事件游标和权限，不打印真实项目名、文件名或 NAS 路径。
- 一期数据质量体检能力已完成，报告：`handoff/main-agent/asset-quality-overview-report.md`。
  - 新增接口：`GET /api/data-steward/assets/quality/overview`。
  - 新增页面：数据管家 / 数据质量，路由 `/data-steward/quality`。
  - 覆盖待审核、失败扫描、缺 checksum、缺置信度、专业待完善、版本缺失、存储路径缺失、零大小文件、非标准资料治理状态、风险项目排行和最近治理事件。
  - 新增脚本：`scripts/dev/check-asset-quality-overview.sh`，已写入 `scripts/README.md`。
  - 验证通过：后端构建、前端构建、健康检查、数据质量专项脚本和 DB-2 只读合同复核。
  - 本轮只读取平台元数据，不读取文件正文，不做模型轻量化，不做构件级解析，不修改或删除 NAS 文件。
- 一期数据质量治理跳转增强已完成：
  - 数据质量页的体检项可跳转扫描任务、非标准资料治理或项目资产详情。
  - 项目资产详情支持按 `qualityIssue` 筛选缺 checksum、缺置信度、专业待完善、版本缺失、路径缺失和零大小文件。
  - 后端文件列表接口已支持 `qualityIssue` 参数，专项脚本已补充问题筛选断言并通过。
  - 当前验证结果：`MISSING_CHECKSUM total=40935`、`MISSING_DISCIPLINE total=40935`，其他质量问题当前为 `0`，符合现有真实 NAS 元数据状态。
- 项目 `512 / 108-福城南产业片区11-20-02宗地` 专业字段补录已完成，报告：`handoff/main-agent/project-512-discipline-backfill-report.md`。
  - 补录前缺专业 `6751` 条，补录后 `0` 条。
  - 补录分布：结构 `2344`、建筑 `1473`、电气 `1121`、给排水 `620`、暖通 `493`、智能化 `353`、消防 `225`、综合 `68`、燃气 `54`。
  - 已为该项目新增项目扩展专业 `GAS / 燃气`。
  - 前端项目详情页已将专业编码显示为中文标签。
  - 已修复项目详情页在同组件内切换项目或质量筛选时可能残留上一项目表格数据的问题。
  - 当前复验：`/data-steward/assets/512` 可显示真实专业标签；`?qualityIssue=MISSING_DISCIPLINE` 为空列表，符合缺专业数量归零。
  - 本轮只修改平台数据库元数据，不修改 NAS 原文件。
- 真实 NAS 已导入项目专业字段全量补录已完成，报告：`handoff/main-agent/real-nas-all-discipline-backfill-report.md`。
  - 除项目 `512` 外，其余 `15` 个真实 NAS 项目补录 `34184` 条文件专业。
  - 真实 NAS 全量 `40935` 份文件当前专业缺失数为 `0`。
  - 真实 NAS 全量专业分布：结构 `8882`、电气 `7333`、综合 `7307`、建筑 `5599`、暖通 `4197`、消防 `3267`、给排水 `2880`、智能化 `1284`、燃气 `186`。
  - 历史脚本/接口测试/遗留文件中剩余 `10338` 条缺专业数据已统一补为 `GENERAL / 综合`，避免全局数据质量页继续误报“专业待完善”。
  - 全局数据质量接口当前 `missingDisciplineCount = 0`，`qualityIssue=MISSING_DISCIPLINE` 返回 `OK 0 0`。
  - 专项验证通过：`scripts/dev/check-asset-quality-overview.sh`、`scripts/dev/check-agent-db2-contract.sh`。
  - 已写入审计与事件流：`manual-real-nas-discipline-backfill-20260511`、`manual-remaining-discipline-general-backfill-20260511`。
  - 本轮只修改平台数据库元数据，不修改 NAS 原文件。
- 真实 NAS checksum 小批量试点已完成，报告：`handoff/main-agent/real-nas-checksum-small-batch-report.md`。
  - 试点项目：`516 / 112-歌剧院项目`。
  - 执行前：`21` 个文件，约 `246,694,735 bytes`，路径缺失 `0`，大小不一致 `0`，缺 checksum `21`。
  - 通过平台 API `POST /api/data-steward/assets/checksum-jobs/batch` 创建 `21` 个异步任务。
  - 执行后：`CHECKSUM_CALC SUCCEEDED = 21`，失败 `0`，有效 SHA-256 `21`，缺 checksum `0`。
  - 项目级数据质量：`missingChecksumCount = 0`，`qualityIssue=MISSING_CHECKSUM` 返回 `OK 0 0`。
  - 事件流：`CHECKSUM/checksum.success = 21`、`JOB/job.start = 21`、`JOB/job.success = 21`。
  - 本轮只读取 NAS 文件内容计算 SHA-256，不修改、不移动、不删除 NAS 原文件。
  - 下一步建议按容量逐级扩大，优先 `96` 项目 `243` 个文件、约 `2.83 GiB`，不要直接全量跑 10TB。
- 真实 NAS checksum 中等规模试点已完成，报告：`handoff/main-agent/real-nas-checksum-medium-batch-report.md`。
  - 试点项目：`507 / 96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`。
  - 执行前：`243` 个文件，`3,039,265,913 bytes`，路径缺失 `0`，大小不一致 `0`，缺 checksum `243`。
  - 第一轮创建 `200` 个 checksum 异步任务并全部成功；第二轮创建剩余 `43` 个任务并全部成功。
  - 执行后：`CHECKSUM_CALC SUCCEEDED = 243`，失败 `0`，有效 SHA-256 `243`，缺 checksum `0`。
  - 修复批量 checksum 接口问题：原逻辑先取最近更新 `200` 条再过滤缺 checksum，导致第二轮误返回 `0`；已改为直接按 `MISSING_CHECKSUM` 数据库条件查询。
  - 验证通过：后端构建、后端健康检查、项目级 `missingChecksumCount = 0`、`qualityIssue=MISSING_CHECKSUM` 返回 `OK 0 0`、`scripts/dev/check-asset-quality-overview.sh`。
  - 当前真实 NAS 全局剩余缺 checksum：`40671`。
  - 下一步建议继续按容量逐级扩大，优先 `111` 或 `116`，不要直接全量跑 10TB。
- 真实 NAS checksum 第三轮试点已完成，报告：`handoff/main-agent/real-nas-checksum-third-batch-report.md`。
  - 试点项目：`515 / 111-蛇口影剧院项目`。
  - 执行前：`290` 个文件，`1,666,194,107 bytes`，路径缺失 `0`，大小不一致 `0`，缺 checksum `290`。
  - 通过平台 API 创建 `290` 个 checksum 异步任务。
  - 执行后：`CHECKSUM_CALC SUCCEEDED = 290`，失败 `0`，有效 SHA-256 `290`，缺 checksum `0`。
  - 项目级数据质量：`missingChecksumCount = 0`，`qualityIssue=MISSING_CHECKSUM` 返回 `OK 0 0`。
  - 事件流：`CHECKSUM/checksum.success = 290`、`JOB/job.start = 290`、`JOB/job.success = 290`。
  - 专项验证通过：后端健康检查、`scripts/dev/check-asset-quality-overview.sh`。
  - 当前真实 NAS 全局已完成 checksum：`554`，剩余缺 checksum：`40381`，剩余缺 checksum 项目：`13`。
  - 下一步建议优先 `116-港中文（深圳）医学院智能化`，`487` 个文件、约 `1.50 GiB`。
- 真实 NAS checksum 第四轮试点已完成，报告：`handoff/main-agent/real-nas-checksum-fourth-batch-report.md`。
  - 试点项目：`520 / 116-港中文（深圳）医学院智能化`。
  - 执行前：`487` 个文件，`1,610,199,282 bytes`，路径缺失 `0`，大小不一致 `0`，缺 checksum `487`。
  - 通过平台 API 创建 `487` 个 checksum 异步任务。
  - 执行后：`CHECKSUM_CALC SUCCEEDED = 487`，失败 `0`，有效 SHA-256 `487`，缺 checksum `0`。
  - 项目级数据质量：`missingChecksumCount = 0`，`qualityIssue=MISSING_CHECKSUM` 返回 `OK 0 0`。
  - 事件流：`CHECKSUM/checksum.success = 487`、`JOB/job.start = 487`、`JOB/job.success = 487`。
  - 专项验证通过：后端健康检查、`scripts/dev/check-asset-quality-overview.sh`。
  - 当前真实 NAS 全局已完成 checksum：`1041`，剩余缺 checksum：`39894`，剩余缺 checksum 项目：`12`。
  - 下一步建议优先 `115-深圳宝安国际机场T2...`，`396` 个文件、约 `5.49 GiB`。
- 真实 NAS checksum 第五轮试点已完成，报告：`handoff/main-agent/real-nas-checksum-fifth-batch-report.md`。
  - 试点项目：`519 / 115-深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段`。
  - 执行前：`396` 个文件，`5,897,285,554 bytes`，路径缺失 `0`，大小不一致 `0`，缺 checksum `396`。
  - 通过平台 API 创建 `396` 个 checksum 异步任务，traceId：`8780ef4a275c40698dffd1a52f3716ab`。
  - 执行后：`CHECKSUM_CALC SUCCEEDED = 396`，失败 `0`，有效 SHA-256 `396`，缺 checksum `0`。
  - 项目级数据质量：`missingChecksumCount = 0`，`qualityIssue=MISSING_CHECKSUM` 返回 `OK 0 0`。
  - 事件流：`CHECKSUM/checksum.success = 396`、`JOB/job.start = 396`、`JOB/job.success = 396`。
  - 专项验证通过：后端健康检查、`scripts/dev/check-asset-quality-overview.sh`。
  - 当前真实 NAS 全局已完成 checksum：`1437`，剩余缺 checksum：`39498`，剩余缺 checksum 项目：`11`。
  - 下一步建议优先 `109-华润三九银湖科创中心项目`，`1457` 个文件、约 `7.11 GiB`；或 `110-龙华区观湖街道观城城市更新单元第一期10地块`，`2614` 个文件、约 `8.08 GiB`。
- 一期主线验收收口已完成，报告：`handoff/main-agent/phase1-mainline-acceptance-closure.md`。
  - 裁决：一期不再继续扩功能，当前进入用户验收/内部试运行准备；只修 P0/P1 和必要可用性问题。
  - 验证通过：后端健康检查、后端构建、前端构建、`check-minimal-chain.sh`、`check-master-data-chain.sh`、`check-deliverable-standard-chain.sh`、`check-mvp-chain.sh`、`check-scan-task-control.sh`、`check-asset-quality-overview.sh`、`check-agent-db2-contract.sh`。
  - 真实 NAS 当前口径：`16` 个真实项目、`40935` 个文件、模型 `2604`、图纸 `38331`、总容量约 `296.02 GiB`。
  - 真实 NAS 关键质量字段：缺专业 `0`、缺路径 `0`、零大小或缺大小 `0`、待审核候选 `0`。
  - 非标准目录：`11` 条仍在治理区，均为 `PENDING_AGENT`，不进入正式资产库。
  - checksum 裁决：能力已通过五轮试点，剩余 `39498` 个缺 checksum 不阻塞一期上线，后续按后台治理任务分批补齐。
  - 本轮未重跑 `check-bim-asset-batch1/2/3` 全量批次脚本；这些脚本已由测试 agent 复验通过，且会持续制造大量测试项目。本轮以主线脚本、专项脚本和真实 NAS 数据抽查作为收口依据。
- 当前项目下拉清理与资产总览排序已完成，报告：`handoff/main-agent/project-dropdown-cleanup-and-asset-sort-report.md`。
  - 本机 dev 数据库中 `503` 个测试项目已软归档，原始信息保存在 `dev_archived_test_projects_20260511`。
  - `/api/core/users/me` 当前只返回 `18` 个项目：`2` 个样板项目 + `16` 个真实 NAS 项目。
  - 保留样板项目是为了不影响主数据、交付标准和 MVP 样板链路演示。
  - 后端已补强：旧 token 中的当前项目如果被归档，会自动回退到第一个可访问项目。
  - 资产总览页已新增 `项目ID升序 / 项目ID降序` 选择，默认升序，仅前端本地排序。
  - 验证通过：后端构建、前端构建、后端健康检查、`/home` 和 `/data-steward/assets` 页面 HTTP 200、真实 NAS 资产接口仍返回 `16` 个项目。
- 一期内部试运行验收清单已完成：`handoff/main-agent/phase1-internal-trial-acceptance-checklist.md`。
  - 用于用户按页面试运行验收，不作为二期客户交付验收。
  - 当前验收口径：下拉 `18` 个项目，真实 NAS `16` 个项目，真实文件 `40935` 份，非标准目录 `11` 条。
  - 通过标准：真实项目可查、文件元数据可看、路径可复制、专业/路径/大小关键字段不缺失、非标准目录不污染正式库、普通用户不越权、DB-2 稳定 View 准备好。
  - 阻塞标准：权限泄漏、找不到真实文件、正式资产污染、页面主链路打不开。
- 当前导航与项目工作台重组已完成主 agent 审计，并已按测试 agent 报告完成 P1/P2 返修，待测试 agent 极短回归。
  - 默认入口、登录后跳转和兜底路由已改为 `/data-steward/assets`。
  - 项目详情已改为项目工作台，顶部提供工程主数据和工作中心项目内入口。
  - 文件资产区已改为左目录树 + 右文件表，并保留详情、治理、预览、checksum、复制路径等操作。
  - 顶部全局“当前项目”切换器已移除，项目内路由进入前会同步当前项目上下文。
  - P1 返修：左侧全局菜单过滤已收紧，页面侧边栏不再展示 `首页 / 工程主数据 / 工作中心` 顶级入口，保留 `数据管家` 作为平台级主入口。
  - P2 返修：项目工作台顶栏已展示负责人；后端当前用户项目摘要已补充 `projectManagerName` 字段，空值显示为 `待维护`。
  - 验证通过：前端构建、后端构建、临时 `18080` 后端接口抽查、`git diff --check`。
  - 短回归入口：`handoff/test-agent/current-prompt.md`。

## 2026-05-28 M3G-2 正式收口：105 项目历史文件对象化上传灰度

- `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划` 已正式收口。
- `M3G-2：105 项目历史文件对象化上传灰度` 已正式收口。
- 本批已完成：
  - 开发 agent 首批对象化 `fileId=936/937/938`。
  - 重复执行任务幂等跳过。
  - 主 agent 复验时追加对象化 `fileId=939/940/941`。
  - 当前 105 对象化状态约为 `OBJECT_STORED=9 / NAS_ONLY=2919 / coverage=0.31%`。
- 当前读取降级策略已确认：
  - 有 active object version 且 `OBJECT_STORED`：优先读取 NAS 侧 MinIO。
  - 无 active object version：继续读取原 NAS 台账路径。
  - 已标记对象化但对象副本不可读：fail-closed，不静默回退 NAS。
- 已确认：
  - NAS 原文件保留，未移动、未删除、未改名、未覆盖。
  - 未读正文、未写语义索引、未做 Hermes 正文问答。
  - 未修改 `docs/**`。
- 收口记录：
  - `handoff/main-agent/m3g2-105-objectification-gray-closure.md`
- 当前 active 批次：`待用户确认`。
- 下一步候选：`M3G-3：多真实项目分批对象化策略与任务中心增强`。

## 2026-05-28 M3G-3 启动：多真实项目分批对象化策略与任务中心增强

- 用户确认进入下一步。
- 主 agent 裁决当前 active 批次：
  - `M3G-3：多真实项目分批对象化策略与任务中心增强`
- 本批定位：
  - 不做全量迁移。
  - 不默认执行多项目真实对象化。
  - 先增强多项目盘点、dry-run、容量估算、限制条件和任务中心。
- 本批边界：
  - NAS 原文件保留，不移动、不删除、不改名。
  - 不读正文、不写语义索引、不做 Hermes 正文问答。
  - 不修改 `docs/**`。
- 已写入：
  - `handoff/main-agent/m3g3-multi-project-objectification-task-center-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`

## 2026-05-28 M3G-3 正式收口：多真实项目分批对象化策略与任务中心增强

- 测试 agent 已完成 `M3G-3：多真实项目分批对象化策略与任务中心增强` 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 收口结论：通过。
- 当前 P0：无。
- 当前 P1：无。
- P2：
  - 既有 Vite chunk size warning。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 已确认：
  - readiness 为 `NAS_SIDE_MINIO / READY`。
  - 全项目对象化盘点可查，并能区分真实项目与测试 / 样例项目。
  - 多项目 dry-run 可生成按项目分组的计划。
  - dry-run 支持项目范围、真实项目过滤、总量、单项目、并发和限速策略字段。
  - dry-run 未创建迁移任务、未复制文件、未触碰真实 NAS 原文件。
  - M3G-1 / M3E / M3F / M3C / file-access 回归通过。
  - 未新增 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、BIM 引擎或文件正文读取。
- 主 agent 裁决：
  - `M3G-3：多真实项目分批对象化策略与任务中心增强` 正式收口。
  - 当前 active 批次：`待用户确认`。
- 下一步候选：
  - `M3G-4：受控多项目小批对象化执行`。
  - 或先启动 `M4A：documents / chunks 语义证据契约`。

## 2026-05-28 M3G-4 启动：受控多项目小批对象化执行

- 用户确认进入下一步。
- 主 agent 裁决当前 active 批次：
  - `M3G-4：受控多项目小批对象化执行`
- 本批定位：
  - 从 M3G-3 dry-run 规划进入真实小批执行。
  - 仅允许少量真实项目、少量文件、有限容量、人工确认、后端硬上限。
  - 目标是验证多项目对象化真实执行闭环，不是全量迁移。
- 本批默认硬上限：
  - 单次最多 `3` 个真实项目。
  - 单项目最多 `3` 个文件。
  - 总文件数最多 `9` 个。
  - 单项目容量最多 `50MB`。
  - 总容量最多 `100MB`。
  - 必须 `confirmed=true`。
- 本批边界：
  - NAS 原文件保留，不移动、不删除、不改名、不覆盖。
  - 不迁移测试 / 样例 / 归档项目。
  - 不读正文、不写语义索引、不做 Hermes 正文问答。
  - 不修改 `docs/**`。
- 已写入：
  - `handoff/main-agent/m3g4-controlled-multi-project-objectification-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`

## 跨机器交接入口

## 2026-05-26 M3E 启动

- `M3D：真实 NAS 小范围灰度镜像` 已合并回 `main`，`main` 最新进度为 M3D。
- 已从最新 `main` 创建分支：`codex/m3e-preview-artifacts-object-storage`。
- 当前 active 批次：`M3E：预览与转换产物对象化`。
- M3E 定位：
  - 复用对象存储底座。
  - 接入 `data_file_derivatives` / `data_preview_artifacts`。
  - 让 PDF / 图片等浏览器原生预览产物具备对象化状态。
  - 让 DWG / RVT / Office 等文件明确显示需转换状态。
- M3E 禁止：
  - 不做真实转换器。
  - 不读取正文。
  - 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 不新增 Hermes 正文问答。
  - 不暴露真实路径、bucket、object_key、`storage_uri`。
- 已写入开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
- 分支计划文档：`handoff/main-agent/m3e-preview-artifacts-object-storage-plan.md`。

## 2026-05-27 M3E 正式收口

- 测试 agent 已完成 M3E 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- P2：
  - 既有 Vite chunk size warning。
  - 未暂存的品牌命名口径修改与 `.claude/**`、`CLAUDE.md`、`tmp/**` 不属于 M3E。
- M3E 已完成：
  - PDF / 图片对象化预览产物状态。
  - DWG / RVT / Office 转换占位状态。
  - `preview-artifacts` 查询与 prepare 接口。
  - 文件管理器“预览产物”列与准备入口。
  - M3E 专项脚本。
- 已确认：
  - file-access 不回归。
  - 交付包预检查不回归。
  - 未读取正文。
  - 未做真实转换。
  - 未写 semantic documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未暴露真实路径、bucket、object_key、`storage_uri`。
- 主 agent 裁决：`M3E：预览与转换产物对象化` 正式收口。
- 收口记录：`handoff/main-agent/m3e-preview-artifacts-object-storage-closure.md`。
- 原下一步建议为 `M4A：documents / chunks 语义证据契约`；2026-05-27 已根据用户新裁决插入 `M3F：新文件对象存储优先写入` 与后续 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`，M4A 顺延。

## 跨机器交接入口

后续迁移时，接手 agent 请从以下文件开始：

- `handoff/main-agent/status.md`
- `handoff/main-agent/backlog.md`
- `handoff/main-agent/decisions.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/mac-agent/latest-report.md`

完成后必须回写：

- 对应迁移目录下的 `latest-report.md`

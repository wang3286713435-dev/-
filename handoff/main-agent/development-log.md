# 主 Agent 开发监控日志

## 2026-05-27：M3F 启动

- 用户确认：先做 M3F，后续再做 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`。
- 主 agent 已将 active 批次切换为：
  - `M3F：新文件对象存储优先写入与 NAS 兼容回退`
- 当前分支：
  - `codex/m3f-object-storage-first-write`
- M3F 目标：
  - 新增上传文件默认写入对象存储。
  - 新增文件仍进入 `data_file_resources` 业务台账。
  - 新增文件生成 `assetUuid`。
  - 新增文件写入 `data_storage_objects` 和 active `data_file_object_versions`。
  - 新增文件 `storage-status=OBJECT_STORED`。
  - 新增文件通过受控 `file-access` 访问。
- M3F 禁止：
  - 全量 NAS 搬迁。
  - 真实 NAS 文件移动、删除、重命名。
  - 文件正文读取。
  - Hermes 正文问答。
  - documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 真实 BIM 引擎接入。
- 已写入：
  - `handoff/main-agent/m3f-object-storage-first-write-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 当前裁决：
  - 交给开发 agent 实现。
  - M3F 未验收通过前，不进入 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`。

## 2026-05-27：M3G 架构口径更新

- 用户提供更新后的 M3G PR Prompt。
- 主 agent 已将 M3G 口径从“全量项目副本升级对象存储”更新为：
  - `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`
- 新架构裁决：
  - MinIO 服务部署在 NAS 上，不以本机 Docker MinIO 作为正式业务对象存储。
  - NAS 原项目资料区冻结为只读备份和回滚来源。
  - NAS 侧 MinIO 成为真实文件本体主读取入口。
  - MySQL 继续作为业务台账、权限、版本、checksum、交付关系中心。
  - 本机 Hermes 后续通过平台授权，从 NAS 侧 MinIO 复制文件副本到本机工作区解析。
  - M3G 不做 Hermes 正文问答，不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 已更新：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
  - `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/status.md`

## 2026-05-27：M3F 收口裁决

- 开发 agent 完成 M3F，报告写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 完成 M3F 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：
  - 当前 P0：无。
  - 当前 P1：无。
  - M3F 专项脚本通过，`PASS=10 FAIL=0`。
  - M3E / M3D / M3C / M3B / M3A / file-access 回归均通过。
- 主 agent 审计确认：
  - 新上传文件默认写入对象存储。
  - 新上传文件进入 MySQL 业务台账。
  - 新上传文件具备 `assetUuid`、active object version、`OBJECT_STORED` 状态。
  - 受控 `file-access` 可读取对象存储新增文件。
  - 对象存储不可用时 fail-closed，不静默写 NAS fallback。
  - 未发现全量 NAS 搬迁、Hermes 正文问答、documents / chunks、Qdrant / OpenSearch、BIM 引擎、parser / indexing 越界。
- 主 agent 裁决：
  - `M3F：新文件对象存储优先写入与 NAS 兼容回退` 正式收口。
  - M3G 计划文件可随 M3F checkpoint 一并提交，但 M3G 不自动启动。

## 2026-05-27：M3F 合并主线，M3G-1 启动

- 已将 `codex/m3f-object-storage-first-write` 推送到远端。
- 已将 M3F 快进合并到 `main` 并推送。
- 已从最新 `main` 创建：
  - `codex/m3g-nas-minio-real-project-object-storage`
- 当前 active 批次：
  - `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`
- 本批只做：
  - readiness。
  - inventory。
  - dry-run plan。
- 本批不做：
  - 历史文件真实批量迁移。
  - 全量 NAS 搬迁。
  - Hermes 正文问答。
  - documents / chunks / Qdrant / OpenSearch。
  - 文件正文读取。
- 已更新：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
  - `handoff/main-agent/status.md`

## 2026-05-25：M3A 启动

- 用户确认执行对象存储路线规划。
- 主 agent 裁决：启动 `M3A：对象存储与 StorageService 基线`。
- 当前路线：
  - M3A 先做存储模型和 StorageService。
  - M3B 再做 105 小样本对象存储镜像迁移。
  - M3C 再做迁移任务中心与批量策略。
  - M3D 再做预览产物与交付包归档对象化。
  - M4 才进入语义证据层。
  - M5 才进入 Hermes 受控证据问答。
- M3A 不做：
  - 全量 NAS 迁移。
  - 文件正文读取。
  - Hermes 正文问答。
  - 真实 BIM 轻量化。
  - 向量库 / 搜索库 / Hermes memory 写入。
  - 真实 NAS 文件移动、删除、重命名。
- 已更新：
  - `handoff/main-agent/m3a-storage-service-foundation-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
- 当前裁决：
  - 交给开发 agent 按 M3A prompt 开发。
  - 开发完成后测试 agent 按 M3A prompt 验收。
  - M3A 通过前，不进入 M3B、语义解析或 Hermes 正文问答。

## 2026-05-25：M3A 收口裁决

- 测试 agent 完成 M3A 完整验收：
  - 功能验收通过。
  - 后端构建、前端构建、健康检查通过。
  - M3A 专项脚本通过。
  - M2J / M2I / M2H / M2F / M2B / file-access 回归通过。
  - provider health、storage-status、MinIO 受控读取和 NAS 既有预览 / 下载链路均未回归。
- 首轮唯一 P1：
  - M3A 核心交付文件未纳入 Git 跟踪。
- P1 极短复验：
  - 6 个 M3A 核心新增文件已暂存为 `A`。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 未被纳入。
  - `git diff --check` 通过。
  - M3A 专项脚本继续通过。
- 当前 P0：无。
- 当前 P1：无。
- 主 agent 裁决：
  - `M3A：对象存储与 StorageService 基线` 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 不自动进入 M3B。
  - 下一步建议为先提交 / 推送 M3A，再由用户确认是否进入 `M3B：105 小样本对象存储镜像迁移`。

## 2026-05-25：M3B 启动

- M3A 已提交并合并到远端 `main`。
- 用户确认进入下一步。
- 主 agent 新建分支：`codex/m3b-object-storage-mirror-trial`。
- M3B 目标：
  - 105 项目少量文件小样本迁移到 MinIO / S3-compatible。
  - 写对象记录和文件对象版本。
  - `storage-status` 显示对象已存储。
  - 迁移后仍通过现有受控 `file-access` 访问。
  - 重跑不重复污染对象记录。
  - 失败可追踪原因。
- M3B 禁止：
  - 全量 NAS 迁移。
  - 真实 NAS 文件移动、删除、重命名。
  - 正文读取、语义索引、Hermes 正文问答、真实 BIM 接入。
- 已写入：
  - `handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：
  - 交给开发 agent 实现。
  - M3B 未通过前，不进入 M3C。

## 2026-05-26：M3B 收口裁决

- 开发 agent 完成 M3B，报告写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 完成 M3B 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结果：
  - 当前 P0：无。
  - 当前 P1：无。
  - M3B 专项脚本 `PASS=11 FAIL=0`。
  - M3A / M2J / M2I / M2H / M2F / file-access 回归全部通过。
- 主 agent 审计确认：
  - 迁移入口只支持显式 `fileIds`。
  - 单次最多 10 个文件。
  - 未发现项目 / 目录 / 类型全量迁移入口。
  - 测试只使用 `/tmp` 隔离文件资源，不触碰真实业务 NAS 文件。
  - 未发现真实 NAS 路径、bucket、object key、storage URI、SQL、raw row、token、secret 泄露。
  - 未发现 Hermes 正文问答、parser/indexing、真实 BIM 引擎等越界。
- 主 agent 裁决：
  - `M3B：105 小样本对象存储镜像迁移` 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 不自动进入 M3C。
  - 下一步建议为先提交 / 推送 M3B，再由用户确认是否进入 `M3C：对象存储迁移任务中心与批量策略`。

## 2026-05-26：M3-M5 任务图落地

- 用户给出 `NAS 台账治理升级为对象存储与 Hermes 语义证据链` 路线。
- 主 agent 按要求将任务图落为 TODO 文档：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 任务图包含：
  - 契约与边界冻结。
  - 平台资产 UUID 与文件状态统一。
  - 对象存储迁移任务中心。
  - 真实 NAS 小范围灰度镜像。
  - 预览与转换产物对象化。
  - documents / chunks 语义契约。
  - 向量库与关键词索引试点。
  - Hermes 受控 Evidence API。
  - 工程主数据与交付治理 Agent 化。
- 当前已完成项：
  - M3A。
  - M3B。
- 当前裁决：
  - 后续 M3 系列批次按该任务图规划。
  - 每完成一项由主 agent 更新复选框。
  - 不修改 `docs/**`。
  - 不跳过对象存储、权限、证据契约直接进入 Hermes 正文问答。

## 2026-05-26：M3C-0 资产存储与证据链契约冻结完成

- 按 `handoff/main-agent/m3-storage-evidence-chain-todo.md` 执行第一项。
- 新增共享契约：
  - `DigitalDeliveryProject/integration-contracts/asset_storage_evidence_chain_contract.md`
- 新增 ADR：
  - `DigitalDeliveryProject/adr/ADR-007-object-storage-evidence-chain-boundary.md`
- 同步更新共享文档：
  - `DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
  - `DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
  - `DigitalDeliveryProject/docs/01_capability_matrix.md`
- 本仓库更新：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md` 标记阶段 0 完成。
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/m3c-0-storage-evidence-contract-freeze-closure.md`
- 边界：
  - 未修改业务代码。
  - 未修改仓库 `docs/**`。
  - 未进入 Hermes 正文问答。
  - 未进入向量库 / 语义解析 / 全量 NAS 迁移。

## 2026-05-26：M3C-1 启动

- 用户确认进入任务图下一步。
- 主 agent 将 active 批次切换为 `M3C-1：资产 UUID 与存储状态统一`。
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 已写入主 agent 计划：
  - `handoff/main-agent/m3c1-asset-uuid-storage-status-plan.md`
- 当前边界：
  - 不做全量 NAS 迁移。
  - 不做迁移任务中心。
  - 不做 Hermes 正文问答。
  - 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 不修改仓库 `docs/**`。

## 2026-05-26：M3C 正式收口

- 测试 agent 完成 M3C 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：通过，未发现 P0 / P1。
- 已确认：
  - `scripts/dev/check-m3c-storage-migration-task-center.sh` 通过，`PASS=9 FAIL=0`。
  - M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
  - Git 跟踪检查通过。
  - 浏览器短验通过。
- 主 agent 判定：
  - `M3C：对象存储迁移任务中心与批量策略` 正式收口。
  - 后续进入 `M3D：真实 NAS 小范围灰度镜像` 前，应先提交 / 推送 M3C。
- 边界：
  - 未做全量 NAS 搬迁。
  - 未触碰真实业务 NAS 文件。
  - 未新增 Hermes 正文问答。
  - 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未修改仓库 `docs/**`。

## 2026-05-26：M3C 合并 main，M3D 启动

- 已将 `codex/m3c-storage-migration-task-center` 快进合并到 `main`。
- 已推送 `main`。
- 已从最新 `main` 创建：
  - `codex/m3d-real-nas-object-mirror-gray`
- 已写入 M3D 开发 / 测试交接：
  - `handoff/main-agent/m3d-real-nas-object-mirror-gray-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- M3D 目标：
  - 用 105 / 503 少量真实业务文件进行对象存储灰度镜像。
  - 验证 PDF / DWG / RVT 或模型类样本。
  - 保证 NAS 原文件不被移动、删除、改名。
  - 保证 file-access 仍然受控。
- 边界：
  - 不全量迁移 NAS。
  - 不做 Hermes 正文问答。
  - 不写语义索引。
  - 不修改仓库 `docs/**`。

## 2026-05-26：M3D 正式收口

- 测试 agent 完成 M3D 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：通过，未发现 P0 / P1。
- 已确认：
  - `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh` 通过，`PASS=19 FAIL=0`。
  - M3C / M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
  - PDF / DWG / RVT 样本均覆盖。
  - 三份样本均已对象化，复跑按幂等策略跳过。
  - NAS 原文件 size / mtime 未变化。
  - 禁出字段扫描通过。
- 主 agent 判定：
  - `M3D：真实 NAS 小范围灰度镜像` 正式收口。
  - 后续进入 `M3E：预览与转换产物对象化` 前，应先提交 / 推送 M3D。
- 边界：
  - 未做全量 NAS 搬迁。
  - 未触碰真实业务 NAS 文件。
  - 未新增 Hermes 正文问答。
  - 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未启动 BIM parser / 真实轻量化。
  - 未修改仓库 `docs/**`。

## 2026-05-22：M2E 测试不通过，转入 P1 修复

- 测试 agent 完成 M2E 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：不通过，无 P0，有 1 个 P1。
- P1 内容：
  - 前端提供草案项勾选并提交 `selectedDraftItemIds`。
  - 后端定义了该字段，但 `confirmOnboarding(...)` 未读取。
  - 空选择或无效选择仍 OK，且按全量规则处理。
- 主 agent 裁决：
  - 暂不收口 M2E。
  - 开发 agent 下一步只修复 `selectedDraftItemIds` 契约。
  - 推荐实现方案 A：后端真正按选择项过滤，并拒绝空选择 / 无效选择。
  - 同步修复两个轻量 P2：确认后状态文案、补“查看节点类型”入口。
- 已更新：
  - `handoff/dev-agent/current-prompt.md`

## 2026-05-22：M2E 开发审计

- 开发 agent 已完成 M2E，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 初验结果：
  - M2E 专项脚本通过，`PASS=7 FAIL=0`。
  - M2D 回归通过，`PASS=8 FAIL=0`。
  - M2C 回归通过，`PASS=11 FAIL=0`。
  - M1C 回归通过，`PASS=14 FAIL=0`。
  - 后端健康检查通过。
  - `git diff --check` 通过。
- 初验确认：
  - 105 / 503 已进入人工确认后的正式初始规则状态。
  - `onboarding/confirm` 未带 `confirmed=true` 时拒绝。
  - 交付链路和交付包准备视图已基于正式规则产生待挂接项。
  - 未发现真实 NAS 操作、正文读取、Hermes / BIM / parser / indexing 越界。
- 待测试 agent 重点复核：
  - 前端 `selectedDraftItemIds` 是否真实影响后端生成；若后端忽略该字段，应作为 P1 处理，或要求开发 agent 将页面文案改为策略确认，不表达为逐项勾选采纳。
- 当前裁决：
  - 可以进入 M2E 测试 agent 验收。
  - 尚不收口 M2E。

## 2026-05-22：M2E 启动

- 用户确认支持进入下一批次。
- 主 agent 裁决：启动 `M2E：真实项目工程主数据人工确认与交付规则落地`。
- 当前基础：
  - M2D 已收口。
  - 105 / 503 保持 `deliverableStandardReady=false`。
  - 105 已有真实资产评估和 catalog-only 草案预览。
  - 105 仍未生成正式工程主数据和交付规则。
- M2E 核心目标：
  - 将 M2D 草案通过人工确认转成正式可维护的工程主数据。
  - 生成最小可用部位树、节点类型、交付物定义、交付物类型和目录线索。
  - 让文档/图纸交付、交付包 / 档案目录基于正式规则工作。
- 关键边界：
  - 真实 NAS 项目不得未经人工确认直接生成标准。
  - 不触碰真实 NAS 文件。
  - 不读取正文。
  - 不新增 Hermes、BIM、parser、writer、indexing。
  - 不自动挂接、审核、整改或生成正式交付结论。
- 已写入：
  - M2E 计划：`handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-22：M2D 收口裁决

- 测试 agent 已完成 `M2D：真实项目工程主数据接入草案增强` 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过，当前未发现 P0 / P1。
- 已通过：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2D 专项脚本，结果 `PASS=8 FAIL=0`。
  - M1C 回归。
  - M2C 回归。
  - `git diff --check`。
- 已确认：
  - `503 / 105` 保持 `deliverableStandardReady=false`，未被模板误导为交付标准已就绪。
  - 105 保持真实资产已接入、工程主数据未确认状态。
  - 接入评估和草案预览均基于 catalog metadata，不读取正文，不生成虚假交付结论。
  - 文档 / 图纸交付和交付包接口未生成虚假应交项或虚假交付包。
  - 未泄露真实 NAS 路径或敏感字段。
  - 未触碰真实 NAS 文件。
  - 未新增 Hermes、BIM、parser、writer、indexing 能力。
- P2：
  - 既有 Vite chunk size warning。
  - 轻量测试策略下未做全量浏览器逐页验收。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件，以及 M2A / M2B / M2C 既有改动，提交时需要确认归属。
- 主 agent 裁决：
  - M2D 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 不自动进入 M2E、M2F、8B-0、8B / 8C 或 9A。

## 2026-05-22：M2D 启动

- 用户确认继续下一步。
- 主 agent 裁决：启动 `M2D：真实项目工程主数据接入草案增强`。
- 背景：
  - 105 项目演示模板数据已软删除。
  - 105 仍保留 2928 条真实资产文件。
  - 真实资产线索显示 DWG、PDF、RVT、XLSX 均已登记，专业线索覆盖建筑、结构、电气、给排水、消防、智能化、暖通、燃气、通用。
- 当前问题：
  - 初始化向导已有接入评估和草案预览雏形，但仍可能让真实 NAS 项目通过内置模板变成“交付标准已就绪”。
  - 这与用户要求的真实项目交付路径不一致。
- M2D 目标：
  - 强化 `onboarding/assessment` 和 `onboarding/preview`。
  - 草案必须从真实资产线索出发，并区分资产线索与模板骨架。
  - 每条草案项必须具备证据来源、证据模式、置信度、风险提示和人工确认标记。
  - 真实 NAS 项目不得一键套模板后直接变成 `deliverableStandardReady=true`。
- 已写入：
  - 计划：`handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- 边界：
  - 不触碰真实 NAS 文件。
  - 不读取正文。
  - 不新增 Hermes、BIM、parser、writer、indexing。
  - 不自动挂接、审核、整改或交付。

## 2026-05-22：M2D 开发审计

- 开发 agent 已完成 M2D，报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 审计重点：
  - 是否防止 105 再次通过模板进入假 ready。
  - 是否泄露真实 NAS 路径。
  - 是否越界新增 Hermes、BIM、parser、writer、indexing 或真实 NAS 操作。
- 主 agent 抽查结果：
  - `scripts/dev/check-m2d-real-project-masterdata-onboarding.sh` 通过，结果 `PASS=8 FAIL=0`。
  - 真实项目 `503 / 105` 直接 `apply-template` 被阻断。
  - `onboarding/apply` 只确认草案，不写正式标准。
  - `deliverableStandardReady` 在草案确认后仍为 `false`。
  - 草案项包含 `evidenceSource / confidenceLevel / riskHint / pendingConfirmation`。
  - 未发现本批新增数据库迁移。
  - 未发现 M2D 范围内真实 NAS 文件操作。
- 已补强 `handoff/test-agent/current-prompt.md`：
  - 要求测试 agent 验证页面下一步入口是否清晰。
  - 要求验证页面主按钮不会让真实项目一键套模板成功。
  - 要求验证文档/图纸交付/交付包页面不显示演示模板虚假应交项。
- 当前裁决：
  - 可以进入 M2D 测试 agent 验收。
  - 尚不收口 M2D。

## 2026-05-22：测试策略轻量化

- 用户要求：测试 agent 后续不再每批大量浏览器逐页点击，只测试代码可用性和是否偏离主线。
- 主 agent 裁决：
  - 默认采用轻量测试策略。
  - 浏览器验收只在 UI/UX、路由、页面问题或主 agent 明确要求时执行。
  - 普通后端 / 接口 / 主线纠偏批次只跑构建、健康检查、专项脚本、少量必要回归、接口抽查、越界检查和 `git diff --check`。
- 已新增：
  - `handoff/main-agent/lightweight-test-strategy.md`
- 已更新：
  - `handoff/test-agent/current-prompt.md`
- M2D 测试已改为轻量验收：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2D 专项脚本。
  - M2C 与 M1C 必要回归。
  - forbidden field 扫描。
  - 越界检查。
  - 默认不做逐页浏览器点击。

## 2026-05-22：105 项目演示模板数据重置

- 用户指出：当前工程主数据仍处于架空状态，没有实际应用到项目交付；105 项目中的模板应删除，避免误导员工认为项目已经完成交付。
- 主 agent 确认真实项目为 `503 / 105 / 深圳市二十八高项目`。
- 执行前备份：
  - `tmp/backups/project-503-before-template-reset-20260522153500.sql`
- 本次采用软删除，不触碰真实 NAS 文件，不删除资产文件元数据。
- 已软删除 105 项目有效业务数据：
  - 工程部位树。
  - 节点类型。
  - 交付物定义 / 类型 / 属性。
  - 目录模板。
  - 交付挂接。
  - 整改记录。
  - 交付包草案。
- 保留：
  - 105 项目文件资产元数据，当前仍有 2928 条。
  - 审计记录。
  - 历史 review record。
- 验证：
  - `standard-status` 返回 `deliverableStandardReady=false`。
  - 初始化状态回到 `SECTION_TREE`。
  - 初始化阻塞项提示需要建立部位树、配置节点类型、补齐交付物标准和目录模板。
  - 文档 / 图纸交付包准备均返回 `totalCount=0`。
- 已写入审计：
  - `action_code=master-data.template-demo.reset`
  - `trace_id=manual-reset-105-template`
- 详细报告：
  - `handoff/main-agent/project-105-template-reset-report.md`
- 主 agent 裁决：
  - 105 不再保留演示模板交付假象。
  - 后续 105 应作为真实项目接入样本，从资产目录推导工程主数据草案并人工确认。

## 2026-05-22：M2C 收口裁决

- 测试 agent 已完成 `M2C：交付包与档案目录能力` 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结果：
  - 后端构建通过。
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 健康检查通过。
  - M2C 专项脚本通过。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归通过。
  - 当前库与临时干净库 Flyway 迁移均通过。
  - `git diff --check` 通过。
- 功能验收：
  - `prepare / drafts / draft detail / items / export-manifest` 可用。
  - 草案保存、草案明细、档案目录预览、CSV 清单导出可用。
  - 503 / 105 页面可展示交付包草案能力；506 / 93 当前无应交项，可安全展示空状态。
- 红线验收：
  - 未生成真实压缩包。
  - 未复制、移动、删除、重命名真实 NAS 文件。
  - 未读取 PDF / Office / DWG / RVT / IFC 正文。
  - 未泄露真实 NAS 路径、`storage_uri`、SQL、token、secret。
  - 未新增 Hermes、BIM、parser、writer、indexing 能力。
- P2：
  - 既有 Vite chunk size warning。
  - 506 / 93 无应交项，客户演示建议优先使用 503 / 105 或已具备交付标准的项目。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 非交付未跟踪文件需要提交时排除。
- 主 agent 裁决：
  - M2C 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 下一步不自动进入 M2D、M2E、8B-0、8B/8C 或 9A。

## 2026-05-22：MAIN-0 主线回归与 M2C 启动

- 用户确认按主线规划进入下一步，并要求：
  - 开启真实 NAS 写入灰度开关。
  - 将用户可见的 `隔离区` 统一改为 `回收站`。
- 主 agent 裁决：
  - 当前不进入 8B / 8C / 9A。
  - 当前不继续 Hermes / G4。
  - 当前 active 批次切换为 `M2C：交付包与档案目录能力`。
  - M2C 只做交付包草案、档案目录和交付清单导出，不复制、不移动、不打包真实 NAS 文件。
- 真实 NAS 写入灰度：
  - 项目 `504 / 100 / 深圳市二十八高项目` 作为当前受控灰度项目。
  - 灰度必须继续受项目、目录、角色、账号边界控制。
  - 不做全公司、全项目、全目录默认开放。
- 命名收束：
  - 前端用户可见文案统一为 `回收站`。
  - 后端用户可见错误 / 成功消息同步为 `回收站`。
  - 内部接口、数据库字段、状态码仍保留 `quarantine` / `QUARANTINED`，避免破坏既有契约。
- 已写入：
  - M2C 计划：`handoff/main-agent/m2c-delivery-package-archive-directory-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
  - 当前路线：`handoff/main-agent/phase2-current-roadmap.md`

## 2026-05-22：UX2 前端使用逻辑重构专项待启动

- 用户确认 UX2 方向：
  - UX1 仍偏 UI 美化，没有彻底解决用户使用逻辑。
  - UX2 要从“用户刚进入平台知道怎么用”出发，重排入口、字段、主次关系和下一步动作。
  - 可以引入轻量拟态玻璃和 Apple 式卡片层级，但不能牺牲表格、权限、NAS 操作、交付审核的可读性。
- 主 agent 裁决：
  - UX2 必须等待 UX1 测试报告。
  - 若 UX1 有 P0 / P1，先修 UX1。
  - 当前仅准备 UX2 待启动计划和 prompt，不切换 `current-prompt.md`。
- 已写入：
  - UX2 计划：`handoff/main-agent/ux2-user-experience-refactor-plan.md`
  - UX2 待启动 Claude prompt：`handoff/dev-agent/ux2-user-experience-refactor-prompt.md`
  - UX2 待启动测试 prompt：`handoff/test-agent/ux2-user-experience-refactor-test-prompt.md`

## 2026-05-21：UX1 正式进入 Claude Code 前端壳层重构

- 用户确认执行 UX1 前端壳层重构专项计划。
- 主 agent 已将当前开发 prompt 切换为 Claude Code 专用 UX1 prompt。
- 本批由 `Claude Code` 主导开发，主 agent 负责监控、纠偏和验收判断。
- 当前分支：`codex/ux1-frontend-routing-visual`。
- 当前策略：
  - 冻结后端功能开发。
  - 不进入 M2C、8B、Hermes 新能力或任何后端新功能。
  - 先重构前端壳层、路由、菜单、项目工作台和页面层级。
- Claude 边界：
  - 只允许修改 `frontend/**` 和 `handoff/dev-agent/latest-report.md`。
  - 禁止修改 `backend/**`、数据库迁移、接口语义、权限规则和 `docs/**`。
  - 如需要后端改动，必须停止并报告，不得自行修改。
- 开发 prompt：`handoff/dev-agent/current-prompt.md`。

## 2026-05-22：UX1 完整重构进入测试验收

- Claude Code 报告 UX1 前端壳层完整重构已完成。
- 主 agent 已读取 `handoff/dev-agent/latest-report.md`。
- 初步审计：
  - 改动集中在 `frontend/**` 与 handoff。
  - `backend/**`、`docs/**`、数据库迁移未见修改。
  - 发现未跟踪 `.claude/**`、`CLAUDE.md` 等非运行文件，后续收口前需决定清理或明确不纳入提交。
- 已将测试 agent prompt 切换为 UX1 验收：
  - 多分辨率视觉巡检。
  - 项目间路由切换连续性。
  - 旧链接兼容。
  - 文件管理 / 工程主数据 / 交付工作中心主链路。
  - M2B/M2A/M1F/M1E/M1D/M1C 回归。
- 测试 prompt：`handoff/test-agent/current-prompt.md`。

## 2026-05-21：UX1 紧急 UI / UX 修复首轮

- M2B 已完成 Git checkpoint 并推送。
- 已切出前端专项分支：`codex/ux1-frontend-routing-visual`。
- 本轮先做 UX1 急救型修复，不作为完整 UX1 收口。
- 已修改：
  - `AppLayout.vue`：顶部补充当前工作上下文、页面标题和下一步说明。
  - `ProjectWorkspaceNav.vue`：项目工作台导航从按钮堆叠优化为三段清晰分组。
  - `AssetProjectDetailPage.vue`：checksum 后台任务默认收起，避免压过文件目录和文件列表。
  - `index.css`：补充顶部上下文样式。
- 未修改：
  - `backend/**`
  - `docs/**`
  - 数据库迁移
  - 权限规则
  - Hermes / BIM / NAS 写能力边界
- 验证：
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `git diff --check` 通过。
- 急救报告：`handoff/main-agent/ux1-emergency-ui-fix-report.md`。

## 2026-05-21：M2B 受控 NAS 写操作真实项目灰度收口

- 测试 agent 已完成 M2B 复核。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - 默认无灰度配置时，真实项目写操作关闭。
  - M2B 专项脚本连续两次通过，结果 `PASS=18 FAIL=0`。
  - M2A / M1F / M1E / M1D / M1C 回归均通过。
  - 真实项目 UI 只做只读冒烟，未触发真实业务目录写入。
  - 未发现 raw path、敏感字段、Hermes/G4/8B/8C/9A 扩展或 parser/indexing 边界突破。
- 主 agent 裁决：
  - M2B 可以正式收口。
  - 主线健康度维持 `绿灯`。
  - M2B 不代表默认开放真实项目 NAS 写操作；真实写入仍必须通过项目级灰度开关、目录范围、角色 / 账号边界和后端校验。
  - 下一步按既定顺序建议先 Git checkpoint / push，再进入 `UX1：前端路由逻辑与视觉体验专项优化`。
- 收口报告：`handoff/main-agent/m2b-nas-write-trial-closure.md`。

## 2026-05-21：M2B 受控 NAS 写操作真实项目灰度启动

- 用户确认：M2A 完整度可以进入 M2B。
- 主 agent 复查：
  - 后端健康检查 `UP`。
  - `git diff --check` 通过。
  - M2A 专项脚本通过，结果 `PASS=21 FAIL=0`。
  - M2A bugfix 已覆盖“项目工作台路由项目可直接执行 NAS 操作，不要求 JWT 当前项目先切换”。
- 主 agent 裁决：
  - M2A 功能完整度足够进入 M2B。
  - M2B 不是继续扩展完整 NAS CRUD，而是给真实项目试运行增加灰度开关、可写目录范围和安全提示。
  - 进入 M2B 前必须完成当前成果 Git checkpoint / push。
- 已写入：
  - M2B 计划：`handoff/main-agent/m2b-nas-write-trial-plan.md`
  - M2B 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - M2B 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-21：UX1 前端路由逻辑与视觉体验专项批次冻结

- 用户反馈：当前平台核心功能做得不错且基本完善，但可用性、跳转逻辑、用户使用方式和视觉质感较差。
- 用户裁决：
  - 当前批次收口并推送 Git 后，主线后续需要新开一个专门优化前端路由逻辑和视觉的批次。
  - UX1 由 `Gemini` 主导开发，主 agent 负责监控、审计、纠偏和最终收口判断。
  - UX1 可以调整前端路由、菜单、组件布局、样式和交互文案。
  - UX1 不得修改后端任何功能实现。
  - 旧链接必须兼容跳转，不能直接删除导致用户书签或测试脚本失效。
- 主 agent 裁决：
  - UX1 不抢占 M2B 命名。
  - 批次顺序固定为：`M2A 当前 bugfix 收口并推送 Git -> M2B 受控 NAS 写操作真实项目灰度 -> UX1 前端路由与视觉体验优化`。
  - UX1 视觉方向为 `精致企业中后台`，保留 Vue 3 + Element Plus 体系，不做炫酷大屏化。
  - UX1 只做前端，不修改 `backend/**`、数据库迁移、接口语义或权限规则。
- 已写入：
  - UX1 计划：`handoff/main-agent/ux1-frontend-routing-visual-plan.md`
  - UX1 Gemini 开发 prompt：`handoff/dev-agent/ux1-gemini-frontend-routing-visual-prompt.md`
  - UX1 测试 prompt：`handoff/test-agent/ux1-frontend-routing-visual-test-prompt.md`

## 2026-05-21：M2A 新增文件夹误报当前项目缺陷修复

- 用户反馈：真实使用 M2A 新增文件夹时，页面提示 `请先切换到当前项目`。
- 根因确认：
  - 项目文件管理页使用 `/data-steward/assets/{projectId}?tab=files` 路由项目作为上下文。
  - M2A 后端 `ControlledNasController` 额外强制 `JWT currentProjectId == 路由 projectId`。
  - 当用户有该项目权限但全局当前项目未切换时，会误报 `CORE_PROJECT_CONTEXT_MISMATCH`。
- 修复动作：
  - `ControlledNasController` 不再调用 `ProjectContextApplicationService.requireCurrentProject(...)`。
  - Controller 只获取当前登录 principal。
  - 项目权限、角色权限、路径安全和写操作权限仍由 `ControlledNasApplicationService` 基于 `projectId` 校验。
- 回归保护：
  - `scripts/dev/check-m2a-controlled-nas-write.sh` 增加断言：不先切换 JWT 当前项目，也能在项目工作台路由项目上新建文件夹。
- 验证：
  - 后端构建通过。
  - M2A 专项脚本通过，结果 `PASS=21 FAIL=0`。
- 修复报告：`handoff/main-agent/m2a-current-project-context-bugfix-report.md`。

## 2026-05-21：M2A NAS 受控文件操作安全底座收口

- 测试 agent 已完成 M2A 复核。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - M2A 专项脚本 `scripts/dev/check-m2a-controlled-nas-write.sh` 通过，结果 `PASS=20 FAIL=0`。
  - M1F / M1E / M1D / M1C / Phase2 batch4 回归均通过。
  - 项目内新建文件夹、上传、重命名、移动、删除到回收站、恢复、操作记录、审计和元数据同步均在隔离测试项目内通过。
  - 查看者写操作被拒绝，未授权用户写操作被拒绝，路径穿越被拒绝。
  - 自动化写操作只发生在脚本创建的 `/tmp` 隔离项目根目录内。
  - 真实项目页面只读检查了 `回收站` 和 `操作记录` 抽屉，未在真实业务 NAS 目录执行上传、移动、删除、重命名等写操作。
  - 未发现 raw NAS 路径、`storage_uri`、`storage_path`、raw row、SQL、token、secret、password 泄露。
- 主 agent 裁决：
  - M2A 可以正式收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - M2A 只是安全底座通过，不代表全量开放完整 NAS CRUD。
  - 永久删除、跨项目移动、批量不可逆操作、Agent / Hermes 触发 NAS 写操作仍禁止。
  - 下一步建议为 `M2B：受控 NAS 写操作真实项目灰度试运行与安全开关`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m2a-controlled-nas-write-closure.md`。

## 2026-05-21：M2A NAS 受控文件操作安全底座启动

- 用户确认希望开放真实项目的 NAS 增删改查能力，即可以通过平台直接操作 NAS 里的真实数据。
- 主 agent 裁决：
  - 不能一次性开放完整 CRUD。
  - 当前 active 批次固定为 `M2A：NAS 受控文件操作安全底座`。
  - M2A 第一批只做 `低风险写操作 + 删除到回收站，不开放永久删除与批量操作`。
  - 本批必须先建立权限、路径、审计、隔离和恢复边界。
- 允许范围：
  - 项目内新建文件夹、上传、重命名、移动。
  - 删除到回收站、回收站恢复。
  - 操作记录、审计和元数据同步。
- 当前禁止：
  - 永久物理删除、跨项目移动、批量不可逆操作。
  - Agent / Hermes 触发 NAS 写操作。
  - 前端传入或展示真实 NAS 绝对路径。
  - 文件正文读取、真实 BIM 轻量化、构件解析。
  - G4 / Hermes 新能力 / 8B / 8C / 9A。
- 已写入：
  - M2A 计划：`handoff/main-agent/m2a-controlled-nas-write-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-21：M1F 员工注册、项目权限管理与局域网试运行收口

- 测试 agent 已完成 M1F 复核。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - 员工手机号注册可用。
  - 无项目授权员工进入等待授权。
  - 管理员可管理员工启停 / 删除 / 授权。
  - 普通员工越权拒绝。
  - 停用 / 删除后不可继续使用。
  - 局域网 `*:5173` 监听与 `192.168.1.66:5173` 访问可用。
  - M1C / M1D / M1E 回归通过。
  - 未触碰真实 NAS 文件，未修改 `docs/**`。
- 主 agent 裁决：
  - M1F 可以收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - 下一步可进入 M2A，但 M2A 必须以受控 NAS 写操作安全底座开局，不能直接开放完整 CRUD。

## 2026-05-21：M1E 文件管理连续工作体验与后台任务可追踪性收口

- 测试 agent 已完成 M1E 验收。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - 503/105 与 506/93 文件管理页面均可正常打开。
  - 文件管理可恢复目录、筛选、分页和最近文件上下文。
  - `重置视图` 可恢复默认文件视图。
  - 文件详情和任务区已将平台文件 ID、项目内部 ID 和 checksum 任务 ID 业务化展示。
  - checksum 后台任务可见、可追踪、可查看脱敏失败原因、可受控重试。
  - 普通用户不能跨项目读取任务或任务详情。
  - 未发现真实 NAS 路径、raw row、SQL、token、secret、password 泄露。
  - 未发生真实 NAS 写操作，未误进入 Hermes/G4/8B/8C/9A。
- 主 agent 裁决：
  - M1E 可以收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - M1A-M1E 已形成 M1 平台本体稳定期的完整阶段成果。
  - 历史下一步建议：`M1 阶段复盘与 M2A 准备`；现已由 M1F 收口和 M2A 启动取代。
- 收口报告：`handoff/main-agent/m1e-file-task-continuity-closure.md`。

## 2026-05-20：M1E 文件管理连续工作体验与后台任务可追踪性启动

- 用户选择 M1E。
- 主 agent 裁决：
  - M1E 是 M1 平台本体稳定期的连续工作体验收口批次，不是 M2A。
  - 本批目标是让文件管理按项目记住目录、筛选、分页和最近文件上下文，并让 checksum 后台任务在项目内可见、可理解、可重试。
  - M1E 不新增大模块，不开放真实 NAS 写操作。
- 当前禁止：
  - Hermes 新能力、G4、8B / 8C / 9A、真实 NAS 写操作、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 已写入：
  - M1E 计划：`handoff/main-agent/m1e-file-task-continuity-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-20：M1D 标准驱动交付闭环强化收口

- 测试 agent 已完成 M1D 复核。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - 项目工作台信息结构符合 `项目资产 -> 工程主数据 -> 交付工作中心`。
  - 工作中心不是工程主数据子功能，但位于项目工作台下并排在工程主数据之后。
  - 工程主数据未就绪时，工作中心提示 `请先生成 / 确认工程主数据草案`。
  - 文档/图纸交付闭环、缺失项解释、远程分页补交、审核整改闭环和 dry-run 导出预检查均通过。
  - 未发现真实 NAS 路径、raw row、SQL、token、secret、password 泄露。
  - 未发生真实 NAS 写操作，未误进入 Hermes/G4/8B/8C/9A。
- 主 agent 裁决：
  - M1D 可以收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - M1 平台本体稳定期可以进入阶段复盘。
  - 下一步建议：`M1E：文件管理连续工作体验与后台任务可追踪性收口`，或 `M1 阶段复盘与 M2A 准备`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1d-standard-delivery-loop-closure.md`。

## 2026-05-20：M1D 标准驱动交付闭环强化启动

- 用户确认进入 M1D。
- 主 agent 裁决：
  - M1D 是 M1 平台本体稳定期的闭环强化批次，不是 Hermes/G4 分支。
  - 本批目标是让员工不依赖 Hermes，也能跑通“应交项 -> 缺失项 -> 补交 -> 审核 -> 整改 -> 复审/重新补交 -> 完整率刷新 -> 导出预检查”。
  - M1D 不新增大模块，优先收束现有文档交付、图纸交付、批量挂接、审核整改和导出预检查能力。
  - M1D 补充吸收用户提出的信息结构纠偏：项目工作台必须呈现 `项目资产 -> 工程主数据 -> 交付工作中心`，工作中心不是工程主数据子功能，但必须在工程主数据之后；工程主数据未就绪时，工作中心提示先生成 / 确认工程主数据草案。
  - 105 项目仅作为样本之一，不能写死；必须至少抽查另一个真实 NAS 项目。
- 当前禁止：
  - Hermes 新能力、G4、8B / 8C / 9A、真实 NAS 写操作、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 已写入：
  - M1D 计划：`handoff/main-agent/m1d-standard-delivery-loop-plan.md`
  - M1D 信息结构补充裁决：`handoff/main-agent/m1d-workspace-ia-correction.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-20：M1C 工程主数据真实项目落地收口

- 测试 agent 已完成 M1C 验收。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - 105/503 与 93/506 两个真实 NAS 项目均通过接入评估与草案预览。
  - assessment 可展示资产登记、文件类型统计、主数据状态、缺口和下一步动作。
  - preview 可展示 catalog-only、证据来源、置信度、风险提示和人工确认要求。
  - 未确认 apply 被拒绝。
  - 确认 apply 后保持幂等，不覆盖已有主数据，并产生审计事件。
  - 部位树、节点类型、交付物标准、文档交付和图纸交付入口不回归。
  - 未发现真实 NAS 路径、raw row、SQL、token、secret、password 泄露。
  - 未新增 Hermes/G4/8B/8C/9A 能力，未触碰真实 NAS 写操作。
- 主 agent 裁决：
  - M1C 可以收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - 下一批建议为 `M1D：标准驱动交付闭环强化`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1c-real-project-masterdata-closure.md`。

## 2026-05-20：M1C 工程主数据真实项目落地启动

- 用户确认进入 M1C。
- 主 agent 裁决：
  - M1C 是平台本体稳定批次，不是 Hermes/G4 分支。
  - 本批目标是让真实 NAS 项目通过“资产证据 -> 接入评估 -> 草案预览 -> 人工确认 -> 工程主数据可维护”进入交付闭环。
  - 模板只能是草案/建议，不能冒充真实工程结构。
  - 105 项目仅作为样本之一，不能写死；必须至少抽查另一个真实 NAS 项目。
- 当前禁止：
  - Hermes 新能力、G4、8B / 8C / 9A、真实 NAS 写操作、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 已写入：
  - M1C 计划：`handoff/main-agent/m1c-real-project-masterdata-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-20：M1B 项目工作台与数据管家可用性收口

- 测试 agent 已完成 M1B 验收。
- 结论：通过，当前无 P0 / P1 / P2。
- 已确认：
  - Fresh login 后进入 `/data-steward/assets`。
  - 资产总览默认真实项目统计不再为 0。
  - 503 / 105 与 506 / 93 项目工作台抽查通过。
  - 工程主数据、文档交付、图纸交付页面抽查通过。
  - 页面主路径没有错误指向 Hermes / G4。
  - 未发现 raw path、SQL、token、secret 泄露。
  - 未触发真实 NAS 写操作。
- 主 agent 裁决：
  - M1B 可以收口。
  - 主线继续保持 `绿灯可继续主线开发`。
  - 下一批建议为 `M1C：工程主数据真实项目落地`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1b-project-workbench-usability-closure.md`。

## 2026-05-20：M1B 项目工作台与数据管家可用性收口启动

- 用户确认进入 M1B。
- 主 agent 裁决：
  - M1B 不是新功能扩张批次，而是可用性收口批次。
  - 目标是让普通员工能理解资产总览、项目状态、项目工作台入口和下一步动作。
  - M1B 优先处理 M1A 测试报告中记录的 `/data-steward/assets` 默认真实项目统计显示为 0 等可用性问题。
- 当前范围：
  - 资产总览 Hero 区。
  - 项目卡片 / 列表状态。
  - 项目工作台顶部说明。
  - 数据管家 / 工程主数据 / 工作中心入口解释。
  - 空状态、提示文案、下一步按钮。
- 当前禁止：
  - Hermes 新能力、G4、8B / 8C / 9A、真实 NAS 写操作、正文抽取、真实 BIM 轻量化、Agent 自动治理。
- 已写入：
  - M1B 计划：`handoff/main-agent/m1b-project-workbench-usability-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`

## 2026-05-20：M1A 平台主线功能基线审计正式收口

- 测试 agent 已完成 M1A 登录超时修复后极短复验。
- 结论：通过，当前无 P0 / P1。
- 已关闭：
  - `P1-M1A-LOGIN-TIMEOUT`。
  - Catalog 详情路径脱敏 P1。
  - 旧文件资源路径脱敏 P1。
- 验证通过：
  - Fresh login 可进入 `/data-steward/assets`。
  - 503 / 105 项目页面抽查通过。
  - 506 / 93 项目页面抽查通过。
  - 文件访问安全脚本 `PASS=18 FAIL=0`。
  - 前端构建、健康检查、`git diff --check` 通过。
- 主 agent 裁决：
  - M1A 可以收口。
  - 主线健康度从 `黄灯可控` 调整为 `绿灯可继续主线开发`。
  - 这不代表进入 9A，也不代表客户交付准备完成。
  - 下一批建议为 `M1B：项目工作台与数据管家可用性收口`，需用户确认后再启动。
- 收口报告：`handoff/main-agent/m1a-platform-baseline-closure.md`。

## 2026-05-20：M1A 登录超时 P1 修复

- 测试 agent 首轮 M1A 验收不通过，阻塞项为 `P1-M1A-LOGIN-TIMEOUT`。
- 主 agent 复现：
  - 命令行登录接口请求很快。
  - 浏览器 fresh login 会卡住并出现 `timeout of 10000ms exceeded`。
  - 带 `Origin: http://127.0.0.1:5173` 的请求在旧后端运行态下无响应。
- 修复动作：
  - 将 Vite 本地代理目标从 `http://localhost:8080` 固定为 `http://127.0.0.1:8080`。
  - 重启后端后，带 Origin 请求恢复正常。
- 验证结果：
  - Fresh login 约 1.6 秒进入 `/data-steward/assets`。
  - `POST /api/core/auth/login` 与 `GET /api/core/users/me` 均返回 200。
- 报告文件：`handoff/main-agent/m1a-login-timeout-fix-report.md`。
- 测试 agent 当前 prompt 已改为 M1A 极短复验，重点复验 fresh login、503/506 页面和路径脱敏。

## 2026-05-20：M 系列主线健康恢复正式启动

- 用户确认：现在开始进入 M 系列，目标是将主线健康度从 `黄灯可控` 提升为 `绿灯`。
- 主 agent 裁决：
  - M 系列不是继续扩功能，而是优先恢复平台本体健康。
  - 当前 active 批次固定为 `M1A：平台主线功能基线审计与交付闭环缺口收束`。
  - M1A 先审计和修补项目工作台、资产总览、文件管理、工程主数据、文档 / 图纸交付、审核整改、导出预检查、权限审计和路径脱敏。
  - M1A 不进入 Hermes 新能力、G4、8B、8C、9A、真实 NAS 增删改查、正文抽取、真实 BIM 轻量化或 Agent 自动治理。
- 当前交接状态：
  - 开发 agent 当前 prompt 已指向 M1A。
  - 测试 agent 当前 prompt 已指向 M1A。
  - M 系列启动说明已写入 `handoff/main-agent/mainline-m-series-kickoff.md`。
- 绿灯最低判定：
  - M1A 开发审计完成。
  - M1A 测试验收完成。
  - 105 与至少一个非 105 真实 NAS 项目基础链路无 P0 / P1。
  - 6A、6B、7A、8A 回归脚本通过。
  - 未引入被禁止的新能力或路线偏移。

## 2026-05-20：暂停 G4，冻结 Hermes，恢复 main 主线

- 用户裁决：
  - 当前暂停 G4 开发。
  - Hermes 定位已经出现偏移，必须立刻冻结。
  - 后续 Hermes 重新对齐后，通过独立分支继续完善。
  - 主线需要继续完善平台功能，不应一直卡死在 Hermes 功能中。
- Git 治理已执行：
  - 暂存本地运行态修改，避免污染主线治理。
  - 创建并推送冻结分支 `codex/hermes-g3-g4-freeze`，保留当前 Hermes/G3/G4 状态。
  - 切回 `main`。
  - 将 `main` 快进到当前平台成果点 `ac3baa3`。
- 当前决策：
  - G4 计划保留为历史方案，但不再作为 active 开发批次。
  - `codex/nas-real-project-import-pr` 保留为历史开发分支，不再作为主线继续推进。
  - 后续平台功能默认从 `main` 拉分支。
  - 后续 Hermes 重新启动必须从 `main` 拉独立 `codex/hermes-*` 分支，并先重新定义定位。
- 当前建议 active 主线：
  - `M1A：平台主线功能基线审计与交付闭环缺口收束`。
- M1A 重点：
  - 项目工作台、资产总览、文件管理、工程主数据、标准驱动交付、文档/图纸交付、审核整改、导出预检查、权限审计和路径脱敏。
- M1A 禁止：
  - 新增 Hermes 能力、继续 G4、进入 8B / 8C / 9A、真实 NAS 增删改查、正文抽取、BIM 真实轻量化、Agent 自动治理。

## 2026-05-20：G4 真实项目交付闭环试运行启动

- 状态：已被后续主线治理裁决暂停。以下为历史记录。
- 前置裁决：
  - G3 已通过测试 agent 验收并完成 Git checkpoint。
  - 当前不自动进入 8B / 8C / 9A。
  - 8B BIM 轻量化任务编排继续后置。
- 用户确认：
  - 下一步进入真实项目交付闭环试运行。
  - 当前不继续扩大平台内容，优先验证真实员工能否把既有项目跑入数字化交付闭环。
- 主 agent 裁决：
  - 当时曾进入 `G4：真实项目交付闭环试运行与问题修补`。
  - G4 不是新功能扩张批次，而是真实项目试运行纠偏批次。
  - 105 项目作为主样本，但必须至少抽查另一个真实 NAS 项目，不允许为 105 写死逻辑。
- G4 主链路：
  - 真实项目资产入口。
  - 项目工作台。
  - Hermes 工程主数据补齐计划。
  - Hermes 交付缺失项补交 / 文件挂接推荐方案。
  - 人工确认推荐挂接。
  - 文档 / 图纸交付状态刷新。
  - 审核 / 整改。
  - 导出预检查 dry-run。
  - 审计留痕。
- G4 允许修补：
  - 项目上下文、真实项目入口、页面刷新、交互提示、空状态、权限/Missing Evidence 解释、专项脚本和小范围只读聚合。
- G4 禁止：
  - 真实 BIM 轻量化、真实模型转换、构件级解析、正文抽取、selective indexing、真实 NAS 增删改查、Hermes memory / 向量库写入、Agent 未确认自动治理、为 105 写死逻辑。
- 已写入：
  - G4 规划：`handoff/main-agent/phase2-g4-real-project-delivery-trial-plan.md`
  - 开发 agent prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 agent prompt：`handoff/test-agent/current-prompt.md`
  - 路线状态：`handoff/main-agent/phase2-current-roadmap.md`
  - 主状态：`handoff/main-agent/status.md`

## 2026-05-18：文件管理页目录树超时修复任务收束

- 用户反馈项目详情 `文件管理` 中目录结构与右侧文件对应关系异常，并出现 `timeout of 10000ms exceeded`。
- 主 agent 复核代码后确认根因方向：`CatalogApplicationService.listCatalogDirectories(...)` 先从 `data_file_resources.logical_path` 聚合目录，随后又调用 `mergePhysicalDirectories(...)`；该方法会基于 NAS 路径映射同步执行 `File.listFiles(...)` 递归遍历真实 NAS。真实项目 `506` 下，右侧 `catalog/files` 查询很快，而左侧 `catalog/directories` 被同步物理目录扫描拖超时。
- 同时确认目录树和文件表存在口径风险：文件表按数据库 `logical_path` 的目录表达式过滤，但目录树混入真实物理目录后，左侧节点可能来自真实 NAS 绝对路径，右侧表按资产元数据过滤，导致点击目录后不对应。
- 本轮修复范围已冻结为 P0/P1 修复，不进入二期 6B，不做新的目录同步系统，不修改 `docs/**`，不引入真实 NAS 写操作，不通过调大前端全局 timeout 掩盖问题。
- 后端修复方向：`GET /api/data-steward/catalog/directories` 默认只返回数据库已登记文件资产推导出的目录；保留项目权限校验；目录 `directoryPath` 与 `catalog/files` 的 `directoryPath` 查询使用同一套 `logical_path` 口径；默认请求链路不再同步扫真实 NAS。
- 前端兜底方向：目录加载失败时给中文业务提示，右侧文件表继续可用；不删除文件表分页、更多菜单、预览/下载、详情、治理、补 checksum 等既有操作。
- 开发 agent prompt 已写入 `handoff/dev-agent/current-prompt.md`，完成承诺为 `<promise>FILE_BROWSER_DIRECTORY_TIMEOUT_FIXED</promise>`，并要求 Claude Code 使用 Ralph Loop。
- 测试 agent 极短回归 prompt 已写入 `handoff/test-agent/current-prompt.md`，重点复验项目 `506` 的目录接口耗时、目录/文件对应关系、禁止真实路径泄露和文件表操作不回归。

## 2026-05-13：一期文件预览安全外壳完成

- 用户确认继续向后开发，但要求不要拓展太多功能，先完整一期主线。
- 本轮按一期边界补齐“文件预览状态与入口”，没有进入二期真实文件预览、模型轻量化、正文抽取、向量库、自动治理或自动审批。
- 后端新增 `GET /api/data-steward/assets/files/{fileId}/preview`，复用现有文件资产权限，返回预览元数据，不返回真实 `storagePath`。
- 预览状态规则：
  - PDF/图片：`AVAILABLE / BROWSER_NATIVE / NOT_REQUIRED`。
  - Office：`NEEDS_CONVERSION / OFFICE_CONVERSION / NOT_STARTED`。
  - CAD：`NEEDS_CONVERSION / CAD_CONVERSION / NOT_STARTED`。
  - BIM/模型：`NEEDS_CONVERSION / BIM_LIGHTWEIGHT / NOT_STARTED`。
  - 归档包/未知格式：`UNSUPPORTED`。
  - 缺路径、已删除或隔离：`BLOCKED`。
- 前端 `/data-steward/assets/:projectId` 新增文件表格“预览”入口、文件详情抽屉“预览能力”区和“文件预览状态”弹窗。
- 新增专项脚本 `scripts/dev/check-file-preview-shell.sh`，覆盖 PDF 和 RVT 两类边界，并断言预览接口不暴露 `storagePath`。
- 独立验证结果：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-file-preview-shell.sh` 通过。
  - `git diff --check` 通过。
  - Headless Chrome 冒烟通过：`/data-steward/assets/2` 可打开，预览按钮和弹窗可用，弹窗明确“不读取文件正文”，页面宽度 `1440/1440/1440` 未横向撑爆。
- 本轮报告已写入 `handoff/dev-agent/latest-report.md`。

## 2026-05-13：一期试运行短回归 Prompt 与验收清单更新

- 用户询问“一期主线剩余可用性/验收完整度”含义后，主 agent 已明确该范围不是继续扩二期功能，而是把一期内部试点做到可用、可验收、可交接。
- 用户确认后，主 agent 将测试 agent 当前任务更新为 `一期内部试运行可用性与验收完整度短回归`。
- 新测试 prompt 已写入 `handoff/test-agent/current-prompt.md`，覆盖：
  - 真实 NAS 项目与文件基线。
  - 资产总览、项目详情、扫描任务、数据质量、非标准资料治理页面。
  - 文件预览安全外壳。
  - 权限与路径安全。
  - 企业 agent 稳定读模型准备状态。
  - 必跑脚本：`check-scan-task-control.sh`、`check-asset-quality-overview.sh`、`check-agent-db2-contract.sh`、`check-file-preview-shell.sh`。
- 同步更新 `handoff/main-agent/phase1-internal-trial-acceptance-checklist.md`，补入文件预览安全外壳验收项和阻塞失败标准。
- 当前裁决不变：不做真实 BIM 轻量化、不做 Office/CAD 转换、不读取文件正文、不写向量库、不自动治理或审批。

## 2026-05-13：一期短回归 P1/P2 修复

- 已读取测试 agent 最新报告：短回归无 P0，但存在 1 个 P1 和 1 个 P2。
- P1：`check-agent-db2-contract.sh` 因 `hermes_agent_ro` 密码口径不一致失败。
  - 已重新生成本机 dev 只读密码。
  - 已同步更新 MySQL `hermes_agent_ro` 密码和 macOS 钥匙串。
  - 密码未写入仓库或报告。
  - 已复跑 `scripts/dev/check-agent-db2-contract.sh`，结果 `agent db2 contract ok`。
- P2：首页项目下拉残留 `SCANCTRL-*` 测试项目。
  - 已将残留 `SCANCTRL-*` 项目归档软删除，并删除其用户可见授权。
  - 已修复 `scripts/dev/check-scan-task-control.sh`，增加退出清理逻辑，后续复跑不会再次污染首页项目下拉。
  - 已复跑 `scripts/dev/check-scan-task-control.sh`，结果 `scan task control regression passed`。
  - 管理员 `/api/core/users/me` 当前项目数为 `18`，`SCANCTRL-*` 可见数量为 `0`。
- 其他回归：
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `check-asset-quality-overview.sh` 通过。
  - `check-file-preview-shell.sh` 通过。
  - `git diff --check` 通过。
- 修复报告：`handoff/main-agent/phase1-short-regression-remediation-report.md`。

## 2026-05-13：一期内部试运行可用性验收收口

- 测试 agent 已完成极短复验，并在 `handoff/test-agent/latest-report.md` 中确认：
  - `check-agent-db2-contract.sh` 已通过。
  - 首页项目下拉为 `18` 个项目，且无 `SCANCTRL-*`。
  - `check-scan-task-control.sh` 复跑后不再留下可见 `SCANCTRL-*` 测试项目。
  - 当前未发现新的 P0/P1/P2。
- 主 agent 裁决：一期内部试运行可用性与验收完整度正式收口。
- 已更新 `handoff/main-agent/status.md`，将当前状态标记为一期内部试运行验收通过。
- 已新增最终收口报告：`handoff/main-agent/phase1-internal-trial-final-closure.md`。
- 下一步进入“用户试运行 + 只修 P0/P1”或“二期后续批次规划”二选一；在没有新指令前，不继续扩一期后端治理，不进入真实 BIM 轻量化，不做正文抽取、向量库、Agent 自动治理或自动审批。

## 2026-05-12：二期批次一规划固化

- 用户确认可以开始规划二期，但要求批次一只做：
  - 前端资产目录。
  - REST 权限证明。
  - 只读 catalog API。
  - Agent preview / audit-ready 页面。
- 用户明确禁止本批涉入：
  - Agent 直接增删改数据库。
  - Agent 自动移动、删除、修复 NAS 文件。
  - Agent 自动把 BIM 或文件正文写入向量库。
  - Agent 自动下结论或自动审批。
  - 多 Agent 调度真实业务动作。
  - 面向客户的生产级权限体系承诺。
- 主 agent 已完成规划文档：`handoff/main-agent/phase2-batch1-readonly-catalog-plan.md`。
- 已同步更新：
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/decisions.md`
- 本轮未启动开发 agent，未修改业务代码，未改数据库迁移，未触碰真实 NAS 文件。

## 2026-05-12：二期批次一开发与测试 Prompt 派发

- 用户确认进入下一步。
- 主 agent 已将二期批次一规划转写为：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 开发 prompt 已明确要求：
  - 使用 Claude Code Ralph skill 拆 story。
  - 使用 Ralph Loop 执行。
  - 完成承诺为 `<promise>PHASE2_BATCH1_READONLY_CATALOG_COMPLETE</promise>`。
  - 只做前端资产目录、REST 权限证明、只读 catalog API、Agent preview / audit-ready 页面。
  - 不做 Agent 写库、NAS 文件移动/删除/修复、正文抽取、向量化、自动审批、模型轻量化或构件级解析。
- 测试 prompt 已明确要求：
  - 验证 OpenAPI、专项脚本、前端页面、权限边界、路径泄漏、数据不变性和禁止边界。
  - 发现越权、数据变更、敏感信息泄漏、页面白屏或 OpenAPI 缺失时按 P0 阻塞。
- 本次仅更新交接 prompt 和日志，业务代码尚未由主 agent 直接修改。

## 2026-05-08：Claude Code 与 Ralph Loop 连通性检查

- 工作目录：`/Users/vc/Documents/数字化交付平台`
- Claude Code CLI：`/Users/vc/.local/bin/claude`
- Claude Code 版本：`2.1.133`
- 普通非交互探针通过：Claude 可在项目目录回复 `CLAUDE_CONNECTED`。
- 项目 `/skills` 中可见 `ralph` skill；该 skill 主要用于将 PRD/任务转换为 Ralph 可执行故事。
- 已安装项目级插件：`ralph-loop@claude-plugins-official`。
- 插件配置写入：`.claude/settings.json`。
- 交互会话中已确认 `/ralph-loop` 自动补全可见：
  - `/ralph-loop:help`
  - `/ralph-loop:ralph-loop`
  - `/ralph-loop:cancel-ralph`
- 普通 Claude 会话执行 `/ralph-loop:ralph-loop` 时会触发 setup 脚本权限审批，无法满足无人值守开发。
- 使用 `claude --permission-mode bypassPermissions` 启动后，Ralph Loop 连通性测试通过。
- 连通性测试完成后 `.claude/ralph-loop.local.md` 已自动清理，说明完成承诺检测生效。

## 后续开发监控规则

- 主 agent 通过 PTY 终端启动 Claude Code 开发 agent，并持续读取输出。
- 正式开发首选启动方式：

```text
claude --permission-mode bypassPermissions
/ralph-loop:ralph-loop "<批次 prompt>" --completion-promise "<PROMISE>" --max-iterations <N>
```

- 批次 1 完成承诺固定为：

```text
<promise>BATCH1_DEV_COMPLETE</promise>
```

- 只有在开发 agent 写入 `handoff/dev-agent/latest-report.md`，且构建、脚本、OpenAPI、权限、审计、SQL View 和范围检查全部满足后，才允许接受完成承诺。
- 若发现偏离主线，主 agent 立即中断 Claude Code 会话，必要时执行 `/ralph-loop:cancel-ralph` 或删除 `.claude/ralph-loop.local.md`，修订 prompt 后重新启动。

## 2026-05-08：批次 1 开发 Agent 启动

- 启动方式：`script -qF <log> claude --permission-mode bypassPermissions`
- Ralph 命令：`/ralph-loop:ralph-loop ... --completion-promise BATCH1_DEV_COMPLETE --max-iterations 12`
- 旁路日志：`handoff/main-agent/claude-logs/dev-agent-batch1-20260508-125353.log`
- 最新日志快捷入口：`handoff/main-agent/claude-logs/latest-dev-agent.log`
- 用户本地监听命令：

```bash
tail -f /Users/vc/Documents/数字化交付平台/handoff/main-agent/claude-logs/latest-dev-agent.log
```

- 当前状态：Ralph Loop 第 1 次迭代已启动，开发 agent 正在读取文档与探查现有后端代码。

## 2026-05-08：批次 1 第一次主 Agent 审计中断

- 开发 agent 已新增 V8/V9 迁移、资产仓储、服务层、REST Controller 和批次 1 验收脚本草稿。
- 主 agent 在脚本验收前审计发现 P0 风险：Controller 直接暴露部分仓储查询，路径映射、扫描任务和待审核队列存在绕过用户项目授权的可能。
- 同时发现正式资产入库方法命名和参数可能导致 `.dwg/.dxf/.pdf` 默认分类不能稳定写入 `DRAWING`，以及扫描递归能力需要明确落地。
- 已通过 PTY 中断 Claude Code，并要求继续 Ralph Loop，优先修复权限边界、正式资产分类、递归扫描，再执行构建和验收。
- 完成承诺仍保持 `<promise>BATCH1_DEV_COMPLETE</promise>`，未通过上述审计前不得接受完成。

## 2026-05-08：批次 1 第二次主 Agent 审计中断

- 开发 agent 修复了 Controller 直连仓储、正式资产 `fileKind` 写入、`fileId + userId` 文件详情查询和递归扫描，并完成 `delivery-data-steward` 与 `delivery-app` 构建。
- 主 agent 在报告前复查发现剩余 P0：`runScan` 仍使用全量路径映射，待审核候选的列表、修改、审批、驳回仍未按当前用户项目范围过滤。
- 已再次中断 Claude Code，要求补齐 `accessibleProjectIds`、`requireProjectAccess`、`canAccessProject` 三个 helper，并收紧扫描运行、候选列表和审批入库权限。
- 明确要求修完前不得写 `handoff/dev-agent/latest-report.md`，不得输出 `<promise>BATCH1_DEV_COMPLETE</promise>`。

## 2026-05-08：批次 1 第三次主 Agent 审计中断

- 开发 agent 完成了项目权限收紧、正式资产列表/详情 API、扫描与待审核权限过滤，并写入初版 `handoff/dev-agent/latest-report.md`。
- 主 agent 独立复查发现剩余 P0：V8 Flyway 在 MySQL 下存在索引长度超限和 `recursive` 保留字问题，导致全新库迁移不可用。
- 同时发现导入链路权限缺口：项目 CSV 导入后未给当前用户授权，路径映射导入在 `requireProjectAccess` 前未授权，导致导入项目不可见或路径映射失败。
- 已要求开发 agent 继续 Ralph Loop，只追加/修正当前批次必要实现，不改旧迁移，不扩前端，不进入批次 2/3。

## 2026-05-08：批次 1 第四次主 Agent 审计中断

- 开发 agent 修复 V8 索引为 `nas_path_hash` 计算列唯一约束，并对 `recursive` 列做 MySQL 反引号处理。
- 开发 agent 修复 `createProject`、项目导入和路径映射导入后的项目授权，以及 `upsertProject` 对 null 覆盖非空字段的问题。
- 主 agent 继续按 `handoff/dev-agent/current-prompt.md` 审计，发现批次 1 明确要求 `CSV/xlsx` 导入和 `multipart/form-data`，但实现只覆盖 text/csv。
- 已写入 `handoff/main-agent/audit-blocker-batch1-xlsx.md` 并要求开发 agent 补齐 xlsx 多部件上传、脚本验收和报告。

## 2026-05-08：批次 1 主 Agent 最终审计通过

- Claude Code 通过项目目录下 Ralph Loop 插件运行，旁路监听日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 开发 agent 输出 `<promise>BATCH1_DEV_COMPLETE</promise>` 前，已完成项目资产、NAS 路径映射、CSV/xlsx 导入、扫描任务、待审核队列、正式资产库、SQL View 和 OpenAPI 注册。
- 主 agent 独立复跑后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 主 agent 独立检查后端健康：`/actuator/health` 返回 `{"status":"UP"}`。
- 主 agent 独立确认 Flyway V8/V9 已成功应用，`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 在 MySQL 中存在。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1.sh`，覆盖创建项目、CSV/xlsx 导入、NAS 扫描、自动入库、待审核、人工审核、文件分类、审计、OpenAPI，最终输出 `bim asset batch1 ok`。
- 清理了开发过程中误落在 `backend/` 目录下的临时 xlsx 文件：`code,name`、`code,name,industryType,projectStage`、`projectCode,projectName,nasPath`。
- 结论：批次 1 可接受，下一步应准备 `批次 2：异步任务、checksum、统计与事件流` 的开发 agent prompt。

## 2026-05-08：批次 1 测试报告 P0/P1/P2 修复

- 已阅读 `handoff/test-agent/latest-report.md`，确认测试 agent 发现的阻塞项为：全局扫描任务越权可见、未归属待审核候选可被第二用户接管、自动入库缺少 `confidenceLevel`、OpenAPI 扫描任务参数名不一致。
- 修复扫描任务权限：项目型扫描按项目权限可见；全局扫描只允许创建人查看列表、详情和执行。`ScanTaskResponse` 增加 `createdBy`、`updatedAt` 用于接口可观测和权限判断。
- 修复待审核候选权限：新增统一候选访问判断。已归属项目候选按项目权限；未归属项目候选只允许原扫描任务创建人查看、修改、审批、驳回或批量审批。
- 修复正式文件置信度：`insertFileAsset` 增加 `confidenceLevel` 入参，自动入库写 `HIGH`，人工审核入库透传候选置信度。
- 修复 OpenAPI 合同：扫描详情和执行接口路径参数从 `{taskId}` 统一为 `{scanTaskId}`。
- 回归脚本 `scripts/dev/check-bim-asset-batch1.sh` 新增第二用户权限断言：不能看到管理员全局扫描任务，不能读取详情，不能看到未归属候选，不能 patch/approve；新增自动入库文件 `confidenceLevel` 非空断言；新增 `{scanTaskId}` OpenAPI 精确路径断言。
- 验证结果：`./mvnw -pl delivery-app -am -DskipTests package` 通过，后端重启后 `/actuator/health` 返回 `{"status":"UP"}`，增强后的 `check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。

## 2026-05-08：批次 1 复验通过并切入批次 2

- 已阅读测试 agent 最新复验报告，确认 `P0-1` 扫描任务越权、`P0-2` 待审核候选越权、`P1` `confidenceLevel`、`P2` OpenAPI 路径参数名均已通过复验。
- 复验报告结论为：构建通过、健康检查通过、增强脚本通过、当前无 P0，建议批次 1 通过并进入批次 2。
- 用户已明确批准“验证后，可以继续进入批次 2”。
- 主 agent 将此前补充的 Batch 1.1/1.2 真实 NAS 适配增强调整为后续增强候选，不作为批次 2 前置条件。
- 下一步派发 `批次 2：异步任务、checksum、统计与事件流` 的开发 agent prompt，并同步测试 agent 验收 prompt。

## 2026-05-08：用户改判，先收口批次 1 尾巴

- 用户最新指示为：“先完善批次 1 的尾巴，先不急进入批次 2”。
- 主 agent 暂停批次 2 prompt 派发，恢复批次 1 后置增强优先级。
- 批次 1 尾巴当前定义为：真实 NAS 目录发现、扩展文件分类、低价值文件治理、重扫幂等、历史 `confidence_level` 补齐和对应脚本验收。
- 已发现代码中存在 `/api/data-steward/assets/nas-projects:discover` 雏形，后续开发 agent 应先审计复用，不重复造新接口。

## 2026-05-08：批次 1 尾巴主 Agent 审计通过

- Claude Code 按 Ralph Loop 执行批次 1 尾巴开发，主 agent 在审计中要求补齐短数字项目编码匹配风险，防止 `98` 被普通路径子串误命中。
- 已将扫描阶段项目编码匹配从 `contains` 改为路径段/token 精确匹配：仅接受路径段等于项目编码，或以 `编码 + 分隔符(-/_/.)` 开头。
- `scripts/dev/check-bim-asset-batch1-tail.sh` 已增加确定性回归：模拟 NAS 根路径包含 `98`，未映射根目录下的 `stray.rvt` 必须进入 LOW/PENDING 待审核，不能自动入库。
- 主 agent 独立复核 `git diff --check` 通过；未发现批次 2/3 范围误入后端代码。
- 主 agent 独立构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 首次 `start-backend.sh` 因旧 Java 进程占用 8080 失败，确认是端口冲突而非代码问题；停止旧进程后重新启动成功，`/actuator/health` 返回 `{"status":"UP"}`。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1.sh`，输出 `bim asset batch1 ok`。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1-tail.sh`，输出 `bim asset batch1 tail ok`。
- 结论：批次 1 尾巴可接受；按用户要求暂不进入批次 2，等待明确开工指令。

## 2026-05-08：批次 1 尾巴测试 Agent 复验通过，进入批次 2

- 测试 agent 已写回 `handoff/test-agent/latest-report.md`，结论为批次 1 尾巴通过，当前无 P0，建议进入批次 2。
- 复验覆盖构建、后端启动、健康检查、`check-bim-asset-batch1.sh`、`check-bim-asset-batch1-tail.sh`、静态检查、低价值文件、临时目录降级、短数字项目编码 token 匹配和 OpenAPI。
- 用户已明确“继续下一步”，主 agent 将当前阶段切入 `批次 2：异步任务、checksum、统计与事件流`。
- 已更新 `handoff/dev-agent/current-prompt.md`：要求 Claude Code 使用 Ralph Loop，完成数据库任务表、应用内 worker、checksum 异步补齐、容量统计 API、事件流 API、OpenAPI 和 `check-bim-asset-batch2.sh`。
- 已更新 `handoff/test-agent/current-prompt.md`：锁定批次 2 验收范围，并要求复跑批次 1 与批次 1 尾巴，防止回归。
- 批次 2 明确禁止提前实现批次 3：不做企业 agent API Key、不做删除审批、不做隔离恢复、不做真实物理删除。

## 2026-05-08：批次 2 开发 Agent 交付与主 Agent 审计通过

- Claude Code 已按 Ralph Loop 完成批次 2 开发，交付报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 审计中发现并要求修复的关键问题：
  - 全局事件只允许原 operator 可见，避免无项目事件被其他用户读取。
  - 容量统计必须真正按 `projectId` 过滤，`totalSizeBytes` 统计全部文件容量而不是只统计模型容量。
  - 任务列表按 `projectId` 查询时，`AssetJobRepository.listForUser` 必须追加 `AND project_id = :projectId`。
  - `AssetJobWorker` 所有失败路径必须记录 `JOB/job.fail`，不能只有 `checksum.fail`。
  - 脏数据环境下待审核队列默认 `ORDER BY c.id ASC LIMIT 200` 会让新候选不可见，已改为 `ORDER BY c.id DESC` 最新优先。
- 验收脚本 `scripts/dev/check-bim-asset-batch2.sh` 已增强：
  - 增加 `projectId` 过滤断言，A 项目查询不能返回 B 项目任务。
  - 增加失败 checksum 任务的 `JOB/job.fail` 事件流断言。
  - 继续带批次 1 与批次 1 尾巴回归。
- 主 agent 独立复跑结果：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。
  - `scripts/dev/check-bim-asset-batch1-tail.sh` 输出 `bim asset batch1 tail ok`。
  - `scripts/dev/check-bim-asset-batch2.sh` 输出 `bim asset batch2 ok`。
- 静态范围检查未发现 API Key、删除审批、隔离恢复或真实物理删除实现混入批次 2。
- 用户提醒后，主 agent 已将后续 Claude Code 正式开发模型策略调整为 `deepseek-pro`，并写入 `handoff/main-agent/status.md`。当前这轮已经运行中的开发会话不因模型名重启；下一轮开发 agent 启动时使用 `claude --permission-mode bypassPermissions --model deepseek-pro`。
- 当前建议：先交给测试 agent 复验批次 2；测试报告无 P0 后，再由主 agent 派发批次 3 prompt。

## 2026-05-09：确认进入批次 3

- 已读取测试 agent 最新报告：`批次 2 验收通过，当前无 P0，可等待主 agent/用户确认后进入批次 3`。
- 用户已明确确认进入批次 3。
- 已更新 `handoff/dev-agent/current-prompt.md`：本轮只做企业 agent、API Key、agent 只读/任务/申请能力、删除申请审批、逻辑删除、物理隔离、恢复和受控永久删除。
- 已更新 `handoff/test-agent/current-prompt.md`：锁定批次 3 验收范围，并要求复跑批次 1、批次 1 尾巴、批次 2，防止回归。
- 批次 3 安全红线：
  - API Key 不得保存明文。
  - agent key 不得复用普通 JWT 权限。
  - agent 不得直接修改正式资产、审批、隔离、恢复或永久删除。
  - 未审批物理删除不得移动或删除文件。
  - 逻辑删除不得触碰 NAS 文件。
  - 永久删除只能作用于回收站文件，且必须满足保留期。
  - 验收脚本只能移动/删除 `/tmp` 测试文件，不能触碰真实 NAS `/Volumes/zyzn/卓羽智能项目`。
- 后续 Claude Code 开发 agent 必须使用 Pro 模型和 Ralph Loop，完成承诺为 `<promise>BATCH3_DEV_COMPLETE</promise>`。
- 实际启动时 Claude CLI 拒绝 `deepseek-pro`，提示可用 Pro 型号为 `deepseek-v4-pro`；已按用户“切换为 pro”的意图改用 `deepseek-v4-pro`。
- 批次 3 开发 agent 已启动，旁路日志软链为 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 主 agent 监控时发现 Claude Code 触发 `Explore` 内部探索任务；已中断并补充开发 prompt，明确禁止 `Explore`、`Task`、`Subagent` 等独立上下文委派，要求所有阅读、实现、验证保留在主 Claude Code 会话。

## 2026-05-09：批次 3 主 Agent 审计打回

- Claude 开发 agent 完成批次 3 初版并写入 `handoff/dev-agent/latest-report.md`，专项脚本曾通过。
- 主 agent 复查发现 3 个必须修复的安全边界：
  - Agent NAS 扫描允许 `projectId=null` 或任意 `rootPath`，未绑定项目路径映射。
  - Agent 查询任务详情时对 `projectId=null` 的全局任务缺少访问校验。
  - Agent 发起的删除申请可由 API Key 所属用户审批，绕过自审批限制。
- 另发现 1 个 P1：Agent 标注未校验目标文件是否属于声明项目。
- 已重写 `handoff/dev-agent/current-prompt.md` 为“批次 3 主 Agent 审计修复”提示词，要求开发 agent 用 Ralph Loop 修复并补回归断言。

## 2026-05-09：批次 3 主 Agent 审计修复通过

- Claude Code 已按 Ralph Loop 和 `deepseek-v4-pro` 完成批次 3 P0/P1 安全修复，旁路日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 修复范围保持在批次 3 后端安全边界，没有扩前端、没有进入二期 BIM 能力：
  - Agent 触发 NAS 扫描必须提供 `projectId`，且 `rootPath` 必须落在该项目启用的 NAS 路径映射下。
  - Agent 查询任务详情时，项目任务按 API Key 授权项目隔离；全局任务按创建人隔离。
  - Agent 删除申请的 `requested_by` 改为记录 `apiKeyId`，审批时阻止 API Key 创建人自审批。
  - Agent 标注仅允许 `FILE_RESOURCE`，并校验目标文件真实存在且属于声明项目。
- 回归脚本 `scripts/dev/check-bim-asset-batch3.sh` 已补充 P0-1/P0-2/P0-3/P1 断言，并在脚本末尾自动回归批次 1、批次 1 尾巴和批次 2。
- 主 agent 独立复验：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch3.sh http://localhost:8080 platform.admin Admin@123 2` 通过，并输出 `bim asset batch3 ok`。
- 非阻塞记录：脚本中为了给 `delivery.engineer` 授权当前临时测试项目，使用了当前 Docker MySQL 的测试库直连写入；这是验收脚本准备动作，不属于业务逻辑。
- 结论：批次 3 已通过主 agent 审计，可交测试 agent 做独立复验。若测试 agent 无 P0，一期后端三批次可进入阶段总验收与企业 agent 对接清单确认。

## 2026-05-09：批次 3 测试 Agent P0 打回与修复

- 测试 agent 首轮批次 3 验收不通过，唯一 P0 为：普通项目用户 `delivery.engineer` 可成功创建 `ALL_PROJECTS` 类型 API Key。
- 主 agent 已通过 Claude Code 开发 agent 使用 Ralph Loop 和 `deepseek-v4-pro` 执行专项修复；旁路日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 修复策略：
  - `ALL_PROJECTS` 创建不再直接放行。
  - 现阶段平台级管理员口径暂定为：用户必须在所有 `deleted=0 AND status='ACTIVE'` 的活动项目上都具备 `PROJECT_ADMIN` 角色。
  - 无活动项目时 fail closed。
  - 不按用户名硬编码。
  - 普通项目用户失败时返回 `AGENT_KEY_ALL_PROJECTS_FORBIDDEN` 和 403。
- 触碰范围：
  - `AgentApiKeyRepository.java` 新增 `hasProjectAdminRoleOnAllActiveProjects(Long userId)`。
  - `AgentApiKeyApplicationService.java` 在 `ALL_PROJECTS` 分支调用权限检查。
  - `scripts/dev/check-bim-asset-batch3.sh` 新增 step 4b：`delivery.engineer` 创建失败、`platform.admin` 创建成功、创建后立即撤销。
  - `handoff/dev-agent/latest-report.md` 已写入专项修复报告。
- 主 agent 独立复验：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch3.sh http://localhost:8080 platform.admin Admin@123 2` 通过，并输出 `bim asset batch3 ok`。
- 结论：该 P0 已修复并通过主 agent 复验；下一步交测试 agent 对 P0 修复做独立复验。测试 agent 原失败报告保留，不覆盖历史验收事实。

## 2026-05-09：批次 3 复验通过，一期后端数据治理收口

- 测试 agent 已再次完成批次 3 全量验收，并写入 `handoff/test-agent/latest-report.md`。
- 复验结论：通过，当前无 P0/P1/P2。
- 复验覆盖：
  - 后端 Maven 构建通过。
  - 后端重新启动和健康检查通过。
  - `check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。
  - `check-bim-asset-batch1-tail.sh` 输出 `bim asset batch1 tail ok`。
  - `check-bim-asset-batch2.sh` 输出 `bim asset batch2 ok`。
  - `check-bim-asset-batch3.sh` 输出 `bim asset batch3 ok`。
  - `delivery.engineer` 创建 `ALL_PROJECTS` API Key 返回 403 和 `AGENT_KEY_ALL_PROJECTS_FORBIDDEN`。
  - `platform.admin` 仍可创建并立即撤销 `ALL_PROJECTS` API Key。
  - 静态检查确认修复基于“覆盖全部活跃项目的 PROJECT_ADMIN 权限”，不是用户名硬编码。
- 主 agent 裁决：一期后端数据治理正式收口。后续除缺陷修复外，不继续扩大一期后端范围；下一步应进入一期企业 agent 对接前确认、真实 NAS 小批量试点导入准备，或转入二期客户交付完整版规划。

## 2026-05-09：真实 NAS 小批量试点只读发现

- 用户提供真实 NAS 路径：`smb://192.168.1.181/zyzn/卓羽智能项目`，并明确只能读取、禁止修改删除 NAS 数据。
- 本机已通过 `/Volumes/zyzn/卓羽智能项目` 访问该目录；本轮只执行目录与文件元数据读取，没有写入平台正式资产库，也没有改动 NAS 文件。
- 一级目录共 `27` 个：
  - 高置信正式项目目录 `17` 个。
  - 重复编号冲突目录 `4` 个，涉及 `95` 与 `99`。
  - 非标准或需人工确认目录 `6` 个。
- 抽样统计发现：
  - `105-启航华居项目`：4 层内白名单文件 `35` 个，适合低风险冒烟。
  - `100-深圳市二十八高项目`：4 层内白名单文件 `123` 个，标准目录清晰，适合作为第一批正式小样本。
  - `101-C塔`：4 层内白名单文件 `217` 个，资料混合，适合作为复杂业务样本。
  - `98-深圳口岸项目`：4 层内白名单文件 `163` 个，但像多口岸项目集合，需先确认拆分口径。
  - `99-丰图既有建模项目`：4 层内白名单文件 `4,268` 个，且存在大量 `临时文件`、`新建文件夹`、`转换` 等路径，暂不建议第一批正式写库。
- 已从代码确认 `NAS 项目发现 dryRun=true` 不会创建项目、路径映射、导入任务、审计事件或事件流；可作为下一步安全预检。
- 已实际调用平台 `nas-projects:discover` 接口执行真实 NAS 根目录 dry-run：
  - 返回 `OK / success`。
  - `totalDirectories=27`。
  - `createdProjects=0`、`updatedProjects=0`、`createdMappings=0`、`existingMappings=0`。
  - 状态分布：`READY 17`、`CONFLICT 4`、`REFERENCE 3`、`NEEDS_CODE_REVIEW 3`。
  - 需人工复核总数 `10`，其中编号冲突为 `95` 与 `99` 两组。
- 报告已写入 `handoff/main-agent/real-nas-pilot-discovery-report.md`。
- 下一步建议：用 `105`、`100`、`101` 做第一批数据库元数据写入；`99` 暂不进入第一批正式写库，只作为压力 dry-run 与低置信审核策略样本。

## 2026-05-09：真实 NAS 第一批元数据写入

- 用户确认继续下一步后，主 agent 按“只写数据库元数据、不修改 NAS 文件”的边界执行第一批真实 NAS 项目扫描。
- 已完成 `105-启航华居项目`：
  - 项目 ID：`503`
  - 路径映射 ID：`681`
  - 扫描任务 ID：`414`
  - 扫描状态：`SUCCEEDED`
  - 扫描文件数：`4052`
  - 自动入库：`2927`
  - 待审核：`0`
  - 失败：`0`
- 已完成 `100-深圳市二十八高项目`：
  - 项目 ID：`504`
  - 路径映射 ID：`682`
  - 扫描任务 ID：`415`
  - 扫描状态：`SUCCEEDED`
  - 扫描文件数：`4634`
  - 自动入库：`3668`
  - 待审核：`293`
  - 失败：`0`
- `101-C塔` 暂停导入：
  - 当前开发库已有历史测试项目编码 `101`，项目 ID `99`，且包含大量 `/tmp` 测试路径和样板文件资产。
  - 为避免真实 NAS 数据污染旧测试项目，未执行真实 `101` 扫描。
  - 已取消误建扫描任务 `416 / REAL_NAS_101`，状态为 `CANCELED`。
  - 已删除误建真实 NAS 路径映射 `683`。
  - 已验证 `101` 下真实 NAS 文件资产写入数为 `0`。
- 验证结果：
  - 统计接口可返回 `100`、`105` 的文件数、模型数、图纸数和容量。
  - `ProjectAssetView`、`FileAssetView`、`AuditEventView` 可查询本轮导入结果。
  - `ModelAssetView` 当前基于 `data_model_integrations`，不会直接覆盖 NAS 扫描自动入库的模型文件；企业 agent 当前应使用 `FileAssetView WHERE file_kind='MODEL'` 检索一期 NAS 模型文件。该点已记录为后续开发优化项。
- 报告已写入 `handoff/main-agent/real-nas-pilot-import-report.md`。
- 下一步建议：先处理 `100` 的 `293` 条待审核候选，再扩大试点；暂不全量扫描 27 个一级目录。

## 2026-05-09：真实 NAS 待审核处理与 P1 修复

- 用户要求继续处理待审核候选，并修复 P1。
- 已处理 `100-深圳市二十八高项目` 扫描任务 `415` 的全部待审核候选：
  - 处理前：`293` 条待审核，其中图纸 `285`、模型 `8`。
  - 处理策略：全部审核通过，但保留 `LOW` 置信度和 `REVIEW` 来源，便于后续数据治理。
  - 处理后：待审核 `0`，审核通过 `293`。
  - `100` 当前正式资产：文件 `3961`，模型 `156`，图纸 `3805`，总容量 `41,863,763,688 bytes`。
- 已修复企业 agent 模型读模型 P1：
  - 新增迁移 `backend/delivery-app/src/main/resources/db/migration/V13__model_asset_view_include_nas_files.sql`。
  - 不修改旧迁移。
  - `ModelAssetView` 现在同时覆盖模型集成记录和 NAS 扫描自动/审核入库的 `file_kind='MODEL'` 文件。
- 验证：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端启动成功，Flyway 已应用 `version=13`。
  - `/actuator/health` 返回 `UP`。
  - `ModelAssetView` 可查 `100` 的 `156` 个模型：`rvt 144`、`nwc 8`、`nwd 4`。
  - `ModelAssetView` 可查 `105` 的 `198` 个模型。
- 已更新 `docs/08-acceptance-and-agent-integration.md` 与 `handoff/main-agent/enterprise-agent-db-mcp-integration-prompt.md`，明确 `ModelAssetView` 必须覆盖 NAS 扫描模型文件。
- 报告已写入 `handoff/main-agent/real-nas-review-and-p1-report.md`。

## 2026-05-09：真实 NAS 试点验收通过

- 用户明确企业 agent 协议尚未确定，先做真实试点验收。
- 后端启动成功，健康检查返回 `UP`。
- Flyway 当前版本为 `13`。
- 路径追溯验收：
  - `100` 抽查 `25` 条文件资产。
  - `105` 抽查 `25` 条文件资产。
  - 合计 `50` 条，NAS 路径存在、指向文件、大小一致均为 `50/50`。
- 检索与统计验收：
  - `100` 当前文件 `3961`、模型 `156`、图纸 `3805`、总容量 `41,863,763,688 bytes`。
  - `105` 当前文件 `2927`、模型 `198`、图纸 `2729`、总容量 `35,338,680,331 bytes`。
- SQL View 验收：
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均可查到真实试点数据。
  - `ModelAssetView` 可查 `100` 的 `156` 个模型和 `105` 的 `198` 个模型。
- 权限验收：
  - `platform.admin` 可查项目 `100` 和文件资产。
  - `delivery.engineer` 查询项目 `100` 返回 `0`，查询项目 `100` 文件资产返回 `0`。
- 审计与事件验收：
  - `100` 有自动入库审计 `3668`、审核通过审计 `293`、文件创建事件 `3961`。
  - `105` 有自动入库审计 `2927`、文件创建事件 `2927`。
- 暂停项复核：
  - `101` 扫描任务 `416` 为 `CANCELED`。
  - 真实 `101` 文件资产写入数为 `0`。
- 验收结论：真实 NAS 试点通过。报告已写入 `handoff/main-agent/real-nas-pilot-acceptance-report.md`。

## 2026-05-09：清理历史 101 测试数据并导入真实 101-C塔

- 用户确认可以删除平台内 `101` 历史测试数据并复用编码 `101`；主 agent 按“只改平台数据库元数据，不修改 NAS 文件”的边界执行。
- 清理前确认历史项目 ID `99` 是测试污染：
  - 原编码 `101`。
  - 活跃文件 `525`，总大小仅 `8,920 bytes`。
  - 活跃路径映射 `57`，主要指向 `/tmp/bim-nas-*`。
  - 真实 NAS 文件记录 `0`。
- 已将旧项目归档为 `TEST-101-ARCHIVED-99`，设置 `INACTIVE` 和 `deleted=1`。
- 已软删除旧项目关联测试文件、路径映射、扫描候选、扫描任务等活跃记录。
- 已通过正式接口创建真实项目：
  - 项目 ID：`505`
  - 项目编码：`101`
  - 项目名称：`C塔`
  - 资产来源：`NAS_REAL_PILOT`
- 已创建真实路径映射：
  - 路径映射 ID：`684`
  - NAS 路径：`/Volumes/zyzn/卓羽智能项目/101-C塔`
- 已完成真实扫描：
  - 扫描任务 ID：`417`
  - 扫描文件数：`5666`
  - 自动入库：`5457`
  - 待审核：`0`
  - 失败：`0`
  - 扫描耗时约 `7 分 24 秒`
- 入库结果：
  - 文件总数：`5457`
  - 模型：`114`
  - 图纸：`5343`
  - 总容量：`28,486,737,158 bytes`
- 验收：
  - 路径抽样 `30/30` 存在且大小一致。
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均可查询真实 `101` 数据。
  - `delivery.engineer` 无法访问 `101` 项目和文件，权限隔离通过。
- 报告已写入 `handoff/main-agent/real-101-cleanup-and-import-report.md`。

## 2026-05-09：真实 NAS 第二批小样本导入

- 用户确认继续下一步，主 agent 按“扩大真实试点但不盲目全量”的策略执行。
- 本轮避开重复编号 `95/99`、可能需要拆分的 `98`、投标/参考/未知目录。
- 选择第二批低风险 READY 项目：
  - `93-中建八局国交酒店项目`
  - `96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`
  - `104-佛山顺德妇幼医院项目`
- 已通过平台接口完成项目建档、路径映射、扫描和自动入库：
  - `93`：项目 ID `506`，路径映射 `685`，扫描任务 `418`，扫描 `6305`，自动入库 `5912`，待审核 `0`，失败 `0`。
  - `96`：项目 ID `507`，路径映射 `686`，扫描任务 `419`，扫描 `367`，自动入库 `243`，待审核 `0`，失败 `0`。
  - `104`：项目 ID `508`，路径映射 `687`，扫描任务 `420`，扫描 `1780`，自动入库 `1699`，待审核 `0`，失败 `0`。
- 入库资产：
  - `93`：文件 `5912`，模型 `588`，图纸 `5324`，容量 `77,923,356,233 bytes`。
  - `96`：文件 `243`，模型 `23`，图纸 `220`，容量 `3,039,265,913 bytes`。
  - `104`：文件 `1699`，模型 `187`，图纸 `1512`，容量 `30,194,811,835 bytes`。
- 验收：
  - 每项目抽样 `20` 条，共 `60` 条，路径存在且大小一致 `60/60`。
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView` 均可查询第二批真实项目数据。
  - `delivery.engineer` 查询第二批项目文件资产均返回 `0`，权限隔离通过。
  - 三个项目均有审计和事件流记录。
- 发现：当前同步扫描会先递归遍历全目录再过滤白名单，`93` 扫描耗时约 `3 分 25 秒`。后续应优先补强扫描任务进度、取消、续扫和低价值目录跳过。
- 报告已写入 `handoff/main-agent/real-nas-second-batch-import-report.md`。

## 2026-05-09：补强扫描任务进度、取消、续扫和报告

- 用户确认优先补强扫描任务，主 agent 在不修改真实 NAS 文件的前提下完成后端增强。
- 已追加 Flyway `V14__scan_task_control_and_progress.sql`，为扫描任务增加进度、取消标记、跳过统计、最后扫描路径、低价值目录跳过配置和扫描报告字段。
- 已增强扫描任务接口：
  - 创建扫描时可选择跳过低价值目录，并可传入额外跳过关键词。
  - 运行中的扫描可查询进度、当前处理数量、总量、百分比和最后扫描路径。
  - 运行中的扫描可取消。
  - 已取消或失败的扫描可续扫。
  - 续扫会跳过同一任务已生成的候选记录，避免重复入库。
  - 扫描完成、取消或失败后可查询扫描报告。
- 已新增专项验收脚本 `scripts/dev/check-scan-task-control.sh`，覆盖进度可见、取消、续扫、跳过低价值目录、跳过自定义目录、空文件不入库、扫描报告和正式资产去重。
- 验证结果：
  - 原生 Maven 后端构建通过。
  - 后端健康检查通过。
  - `check-scan-task-control.sh` 通过。
- 报告已写入 `handoff/main-agent/scan-task-control-upgrade-report.md`。

## 2026-05-09：最大真实 NAS 项目扫描控制测试

- 用户要求直接使用当前最大的真实 NAS 项目进行测试。
- 已确认当前真实试点中容量最大的项目为 `93-中建八局国交酒店项目`：
  - 项目 ID：`506`
  - NAS 路径：`/Volumes/zyzn/卓羽智能项目/93-中建八局国交酒店项目`
  - 正式资产文件数：`5912`
  - 正式资产总容量：`77,923,356,233 bytes`
- 已创建真实扫描任务 `423`，仅读取 NAS 文件元数据，不修改 NAS 原文件。
- 取消验证：
  - 扫描运行中可见进度。
  - 扫描到约 `1110` 个文件时成功取消。
  - 任务进入 `CANCELED`，最后扫描路径已记录。
- 续扫验证：
  - 对任务 `423` 执行续扫。
  - 最终状态 `SUCCEEDED`。
  - 进度 `6305/6305`，失败 `0`。
  - 低价值文件跳过 `15`，跳过目录 `1`。
  - 扫描报告可查询。
- 去重验证：
  - 续扫前文件数 `5912`，续扫后文件数 `5912`。
  - 续扫前总容量 `77,923,356,233 bytes`，续扫后总容量不变。
  - 确认续扫没有重复生成正式资产。
- 报告已写入 `handoff/main-agent/real-largest-nas-scan-control-report.md`。

## 2026-05-09：真实 NAS 第三批导入

- 用户要求先跳过 `95` 和 `99` 这类重复编号项目，导入其他未导入项目。
- 本轮选择编号清晰、非参考/投标目录的 `10` 个项目：
  - `97-水务利源既有建筑`
  - `108-福城南产业片区11-20-02宗地`
  - `109-华润三九银湖科创中心项目`
  - `110-龙华区观湖街道观城城市更新单元第一期10地块`
  - `111-蛇口影剧院项目`
  - `112-歌剧院项目`
  - `113-宝安洪桥头项目`
  - `114-香港项目`
  - `115-深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段`
  - `116-港中文（深圳）医学院智能化`
- 本轮暂缓：
  - `98-深圳口岸项目`：可能需要拆分口径，且库里已有小体量历史测试记录。
  - `95`、`99`：重复编号，需后续人工确认唯一编码。
  - 投标、参考、未知命名目录：不进入正式资产库。
- 导入结果：
  - 新增真实项目 `10` 个。
  - 扫描任务 `424-433` 全部 `SUCCEEDED`。
  - 新增正式文件资产 `20736`。
  - 模型 `1338`，图纸 `19398`。
  - 总容量 `101,002,506,275 bytes`。
  - 扫描失败 `0`，待审核 `0`。
- 验收：
  - 每项目抽样 `10` 条，共 `100` 条，路径存在且大小一致 `100/100`。
  - `ProjectAssetView`、`ModelAssetView` 可查询新项目数据。
  - `platform.admin` 可查；`delivery.engineer` 对 10 个新项目均不可见，权限隔离通过。
- 报告已写入 `handoff/main-agent/real-nas-third-batch-import-report.md`。

## 2026-05-09：一期真实资产接管总览

- 用户确认下一步先做资产总览和暂缓清单。
- 已生成业务可读报告 `handoff/main-agent/phase1-real-asset-overview.md`。
- 当前真实资产接管总览：
  - 已接管真实项目 `16` 个。
  - 正式登记文件 `40,935` 个。
  - 模型 `2,604` 个。
  - 图纸 `38,331` 个。
  - 总容量 `317,849,121,433 bytes`，约 `296.02 GiB`。
- 报告中已固化暂缓目录：
  - `98-深圳口岸项目`
  - 两个 `95-*`
  - 两个 `99-*`
  - `2024.9.3-大水灌`
  - `深城交`
  - `清华斯维尔围标项目`
  - 投标和参考资料目录
- 下一步建议：业务负责人核对 16 个项目编码/名称；企业 agent 团队确认读取方式、事件同步方式和非标准资料治理流程。

## 2026-05-09：资产总览与项目资产详情前端

- 用户确认企业 agent 暂缓，先做平台其他功能；本轮落地“资产总览”和“项目资产详情”。
- 前端新增：
  - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
  - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- 前端能力：
  - 默认只展示 `NAS_REAL*` 来源的真实 NAS 接管项目，避免批次验收测试项目污染业务视图。
  - 资产总览展示真实项目数、文件数、模型数、图纸数、容量和最近更新时间。
  - 项目详情展示项目级统计、文件资产列表、扫描任务、路径映射。
  - 文件资产支持按文件名/路径、类型、专业、扩展名筛选，并可复制登记路径。
- 后端菜单新增“数据管家 / 资产总览”入口，路由为 `/data-steward/assets`。
- 验证：
  - `corepack pnpm build` 通过。
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端健康检查通过。
  - 管理员菜单返回“资产总览”和 `/data-steward/assets`。
  - 真实 NAS 口径聚合结果：`16` 个项目、`40,935` 个文件、`2,604` 个模型、`38,331` 个图纸、`317,849,121,433 bytes`。

## 2026-05-09：一期后端资产查询增强

- 用户要求继续严格按一期边界开发，不进入二期能力；本轮只增强一期后端资产治理查询能力。
- 新增/增强接口：
  - `GET /api/data-steward/assets/projects?assetSource=NAS_REAL*`：项目资产支持来源过滤。
  - `GET /api/data-steward/assets/statistics?assetSource=NAS_REAL*`：容量统计支持来源过滤。
  - `GET /api/data-steward/assets/files:page`：文件资产分页查询，返回 `items/pageNo/pageSize/total`。
- 查询增强：
  - 文件资产分页支持项目、文件类型、专业、文件名、扩展名、来源类型、关键字和资产来源过滤。
  - 扩展名过滤兼容 `.rvt` 和 `rvt` 两种写法。
  - 关键字检索覆盖项目编码、项目名称、文件名和登记路径。
- 前端资产总览已改为直接调用后端 `assetSource=NAS_REAL*` 口径，不再在前端聚合真实 NAS 统计。
- 验证：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `corepack pnpm build` 通过。
  - 后端健康检查通过。
  - 真实 NAS 来源过滤返回 `16` 个项目。
  - 真实 NAS 来源统计返回 `40,935` 文件、`2,604` 模型、`38,331` 图纸、`317,849,121,433 bytes`。
  - `files:page` 使用 `projectId=504&fileKind=MODEL&fileExt=.rvt&pageNo=1&pageSize=5` 返回 `total=144`、`items=5`。

## 2026-05-09：企业 Agent DB-2 握手文档补齐

- 用户要求在下一步开发前补齐 `handoff/main-agent/enterprise-agent-db2-coupling-handoff.md` 中主 agent 信息，重点是 DSN、企业 Agent 只读账号、样本读取和脱敏策略。
- 本机 dev 已创建 MySQL 只读账号 `hermes_agent_ro`。
- 授权范围仅限：
  - `ProjectAssetView`
  - `FileAssetView`
  - `ModelAssetView`
  - `AuditEventView`
- 验证结果：
  - `hermes_agent_ro` 可执行 `SELECT DATABASE()`。
  - `hermes_agent_ro` 可查询四个稳定 View。
  - `hermes_agent_ro` 查询 `core_projects` 被拒绝，确认不能读取业务底表。
- 文档中已明确：
  - 本机 dev DSN。
  - 本机 dev 只读账号；密码通过安全渠道交付，不写入交接文档。
  - 本机 dev 允许内部授权 `LIMIT 30` 样例读取。
  - 样例会暴露真实项目名、文件名和 NAS 路径。
  - shared-dev / staging / 客户环境默认 `STRUCTURE_ONLY`；未完成平台/运维 DSN 执行单、只读账号安全交付和业务负责人、数据负责人书面确认前，不允许真实样例读取。
  - shared-dev / staging 后置执行单：`handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md`。
  - Hermes mirror 层在四个 View 缺少稳定权限字段时必须默认 `DENIED`，真实项目名、文件名和 NAS 路径不得外发或写入外部云服务、向量库、搜索库、长期 memory 或外部观测日志。

## 2026-05-11：企业 Agent 本机同环境联调口径更新

- 用户确认：企业 Agent 后续会更新到本地 Hermes，并与卓羽智能数据中台数据库运行在同一台本机环境中。
- 主 agent 已将 DB-2 对接口径调整为：
  - 本机 `delivery-mysql` + `hermes_agent_ro` 是一期 DB-2 首轮正式联调路径。
  - 当前不再要求立即获取 shared-dev / staging 账号。
  - shared-dev / staging 改为后置触发项：多人远程协作、持续测试、演示或客户交付前类生产验证时再由平台/运维开通。
  - 企业 Agent 仍必须使用专用只读账号，只能读四个稳定 View，不得读业务底表、不得写数据库、不得写 NAS。
  - 交接文档中的明文数据库密码已改为占位符，密码需通过安全渠道单独交付。

## 2026-05-11：本机只读账号轮换与扫描任务运维页启动

- 已轮换本机 MySQL 只读账号 `hermes_agent_ro` 密码。
- 新密码未写入仓库文档，已保存到 macOS 钥匙串：
  - `service: delivery-platform-hermes-agent-ro-local-dev`
  - `account: hermes_agent_ro`
- 权限复核结果：
  - `hermes_agent_ro` 可读取 `ProjectAssetView`。
  - `hermes_agent_ro` 读取 `core_projects` 被拒绝，仍不能读取业务底表。
- 已启动一期扫描任务运维页开发，保持在一期范围内：只做任务列表、进度、创建、运行、取消、续扫和报告查看，不做二期模型预览或构件解析。

## 2026-05-11：扫描任务运维页完成

- 新增前端页面：
  - `frontend/src/modules/data-steward/pages/AssetScanOperationsPage.vue`
  - 路由：`/data-steward/scans`
  - 菜单：数据管家 / 扫描任务
- 新增/补齐前端 API：
  - 创建扫描任务
  - 运行扫描任务
  - 取消扫描任务
  - 续扫扫描任务
  - 查看扫描报告
- 页面能力：
  - 按项目、状态、关键字查看扫描任务。
  - 查看进度、扫描数、自动入库数、待审核数、失败数、跳过目录数、最后扫描路径和失败原因。
  - 创建任务时只登记任务，不自动运行，避免误扫真实 NAS。
  - 运行、取消、续扫均需要人工点击确认。
  - 报告抽屉可查看任务汇总和 `scanReportJson`。
- 修正项目资产详情页扫描状态标签，使 `SUCCEEDED` 正确显示为成功。
- 验证：
  - `corepack pnpm build` 通过。
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端健康检查通过。
  - 菜单接口已返回 `data-steward-scans:/data-steward/scans`。
  - 扫描任务接口返回现有任务。
  - `scripts/dev/check-scan-task-control.sh` 通过。
- 未做：
  - 未新增二期 BIM 预览、构件解析或模型轻量化能力。
  - 未修改 NAS 文件。

## 2026-05-12：二期批次一只读资产目录与 Agent 预览层开发审计

- 用户确认开始规划二期，但二期批次一只允许做：前端资产目录、REST 权限证明、只读 catalog API、Agent preview / audit-ready 页面。
- 主 agent 已调用 Claude Code 开发 agent，使用 `deepseek-v4-pro` 和 Ralph Loop 执行开发，开发日志旁路写入 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 开发 agent 已完成：
  - 只读 Catalog API：项目、目录、文件列表、文件详情、审计上下文。
  - REST 权限证明：单文件权限证明与批量权限检查。
  - 前端资产目录：`/data-steward/catalog`。
  - Agent 预览页：`/data-steward/agent-preview`。
- 主 agent 审计发现并要求修复一个权限边界缺陷：
  - 问题：无权限普通用户访问真实 NAS 文件的 `audit-context` 曾返回 `500 CORE_INTERNAL_ERROR`。
  - 修复：`CatalogApplicationService.getFileAuditContext()` 改为无结果返回空，Controller 转为业务 `404 FILE_NOT_FOUND`，避免内部异常泄漏。
  - 回归保护：`scripts/dev/check-phase2-batch1-readonly-catalog.sh` 新增无权限 `audit-context` 不得返回 500 的断言。
- 主 agent 独立复核结果：
  - 专项脚本 `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`20/20 PASS`。
  - 真实 NAS 样本手工验证：管理员访问 `audit-context` 返回 `200`，普通用户返回 `404`，不再返回 `500`。
  - 后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建：`corepack pnpm build` 通过。
  - 后端健康检查：`UP`。
- 开发 agent 报告：`handoff/dev-agent/latest-report.md`。
- Ralph 进度记录：`.claude/ralph/progress.txt`。
- 主 agent 判断：二期批次一开发侧可进入测试 agent 验收；在测试 agent 通过前，不进入 selective indexing、正文抽取、Agent workflow 或受控写操作。

## 2026-05-12：二期批次一 P1 尾巴修复

- 测试 agent 反馈：本轮无 P0，但前端资产目录页缺少“按目录浏览 + 版本筛选”的完整页面交互，判定 P1，暂不建议收口。
- 主 agent 已按最小范围修复：
  - 后端 `GET /api/data-steward/catalog/files` 新增 `directoryPath` 与 `version` 查询参数。
  - 前端 `/data-steward/catalog` 接入目录聚合接口，新增左侧目录浏览、目录点击过滤、当前目录标签和版本筛选输入框。
  - 专项脚本补充目录接口、目录过滤和版本筛选断言。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，当前为 `24/24 PASS`。
- 边界确认：未新增 Agent 写库、NAS 修改、正文抽取、向量化、模型轻量化、自动审批或删除能力。
- 下一步：测试 agent 针对 P1 做短回归；若通过，可收口二期批次一。

## 2026-05-12：二期批次一 P1 二次修复

- 测试 agent 二次短回归反馈：无 P0，但真实页面仍不通过，剩余 2 个 P1：
  - 前端分页字段读取错误：真实后端返回 `items/pageNo/pageSize/total`，前端仍按 `rows/page/pageSize/total` 读取，导致总数显示但表格空白。
  - 目录接口语义错误：`catalog/directories` 仍返回完整文件路径级记录，不是真正目录聚合。
- 主 agent 已完成修复：
  - `fetchCatalogFiles(...)` 改为读取真实分页字段 `items/pageNo/pageSize/total`。
  - `catalog/directories` 改为按父目录聚合。
  - 父目录截取从 `LENGTH` 改为 `CHAR_LENGTH`，修复中文路径下按字节长度截取失败的问题。
  - `catalog/files?directoryPath=...` 使用同一父目录表达式过滤，保证目录节点和文件过滤语义一致。
  - 专项脚本新增“目录路径不得以文件扩展名结尾”的断言。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，当前 `25/25 PASS`。
  - 浏览器真实页面验证通过：选择 `105-启航华居项目` 后目录侧栏 `559` 个目录节点，点击目录后表格 `5` 行，版本 `V1` 筛选后表格仍 `5` 行。
- 边界确认：未新增 Agent 写库、NAS 修改、正文抽取、向量化、模型轻量化、自动审批或删除能力。
- 下一步：测试 agent 再做一次针对这 2 个 P1 的短回归；若通过，可收口二期批次一。

## 2026-05-13：二期批次一正式收口

- 测试 agent 已完成针对两个 P1 的短回归，报告：`handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前无 P0/P1/P2。
- 通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端启动与健康检查通过。
  - 专项脚本 `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，结果 `25 PASS / 0 FAIL`。
  - 真实页面 `/data-steward/catalog` 不再出现“总数有值但表格空白”。
  - `catalog/directories` 已返回目录级聚合，项目 `503` 返回 `559` 个目录项。
  - 目录点击过滤可将文件列表收敛到具体目录。
  - 版本筛选 `version=V1` 可用。
- 主 agent 已写入收口报告：`handoff/main-agent/phase2-batch1-readonly-catalog-closure.md`。
- 收口裁决：二期批次一正式收口。
- 后续限制：在新的规划和用户确认前，不进入 selective indexing、NAS 文件正文抽取、Agent workflow、受控写操作、模型轻量化或构件级能力。

## 2026-05-13：Agent 预览页横向扩张热修复

- 用户反馈：进入 `/data-steward/agent-preview` 后页面持续向右扩张，导致浏览器卡死。
- 主 agent 定位：Agent 预览页内表格、权限证明长字段和字段合同标签未限制最小宽度与换行，触发主框架横向撑开。
- 修复范围：仅调整 `frontend/src/modules/data-steward/pages/AgentPreviewPage.vue` 的页面级 CSS，不改后端、不改业务接口、不改 NAS 数据。
- 修复内容：
  - 限制 Agent 预览页面、区块、字段合同、长文本和 Element Plus 表格/描述组件不向外撑宽。
  - 对长 traceId、证据值、标签内容开启安全换行。
  - 过滤区允许换行，避免窄屏或长选项时撑开页面。
- 验证：
  - 浏览器自动检查连续 8 次采样，`bodyScrollWidth/docScrollWidth/clientWidth` 均稳定为 `1440`，页面宽度稳定为 `1172`，未再横向增长。
  - `corepack pnpm build` 通过。
- 边界确认：未进入二期后续能力，未新增 Agent 写库、正文抽取、向量化、受控写操作、模型轻量化或构件级能力。

## 2026-05-13：文件资源页大项目卡顿修复

- 用户反馈：进入 `/data-steward/files` 文件资源页后，大项目会一次性加载当前项目全部文件，页面在加载期间明显卡顿，左侧菜单和其他按钮几乎无法响应。
- 主 agent 先尝试按要求调用 Claude Code 开发 agent，并要求使用项目内 Ralph Loop；但 Claude 会话长时间无有效进展且一度误读 Ralph 路径，已中断并由主 agent 直接接管修复。
- 根因确认：
  - 旧文件资源接口默认返回裸 `List<FileResourceResponse>`，前端进入页面立即拉取当前项目全部文件。
  - 前端把整项目大量文件直接放入单个 `el-table`，大项目场景下首屏渲染和请求都过重。
  - 切换筛选或离开页面时，旧请求缺少取消/过期保护。
- 修复内容：
  - 后端在保留旧接口兼容的前提下，为同一路径增加 `pageNo/pageSize` 分页查询合同。
  - SQL 下推分页和计数，支持 `fileKind`、`keyword`、`processStatus` 过滤。
  - 前端新增 `fetchFileResourcesPage(...)`，`/data-steward/files` 改为首屏只加载 50 条并显示分页器。
  - 前端增加请求取消和请求序号保护，避免快速切换项目、筛选或离开页面时旧响应回写状态。
  - `ModelIntegrationsPage.vue` 与 `DeliveryViewPanel.vue` 继续使用旧 helper，避免本轮扩大改动面。
- 验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查 `http://localhost:8080/actuator/health` 返回 `UP`。
  - 真实接口验证：项目 `503` 分页请求第一页返回 `20/2927`，模型筛选第一页返回 `20/198`，旧列表接口仍返回数组以兼容旧调用。
  - 浏览器回归：`/data-steward/files` 首屏只渲染 50 行，分页和文件类型筛选可用，加载中可立即跳转 `/data-steward/catalog`，`/data-steward/models` 和 `/work/document-delivery` 可打开。
- 报告：`handoff/dev-agent/latest-report.md`。

## 2026-05-14：二期批次二规划与 agent prompt 交接

- 用户确认 checksum 任务短回归通过，可以进入下一步。
- 主 agent 裁决：下一步进入二期批次二，但不直接做 selective indexing、正文抽取、Agent workflow、受控写操作、模型轻量化或构件级能力。
- 二期批次二名称固定为：`标准驱动交付闭环最小可用版`。
- 核心验收链路固定为：`项目上下文 -> 部位树 -> 节点类型锁定 -> 交付物标准 -> 交付物类型 -> 文件资源 -> 文档/图纸挂接 -> 交付完整率 -> 缺失项清单 -> 审计留痕`。
- 已新增主 agent 规划文档：`handoff/main-agent/phase2-batch2-standard-delivery-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
  - 要求开发 agent 使用 Ralph Loop。
  - 完成承诺：`<promise>PHASE2_BATCH2_STANDARD_DELIVERY_COMPLETE</promise>`。
  - 明确禁止修改 `docs/**`、旧 Flyway 迁移、真实 NAS 文件，以及越界到 Agent 写操作/模型轻量化/正文抽取/审核流。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
  - 要求验收交付完整率接口、标准未就绪提示、文档/图纸缺失项、挂接后完整率变化、文件选择性能、权限审计和 OpenAPI。
- 已更新主状态：`handoff/main-agent/status.md`。
- 边界确认：本轮只修文件资源页性能与交互问题，未改权限模型、未新增 NAS 写操作、未进入正文抽取/向量化/Agent 工作流/模型轻量化能力。

## 2026-05-14：二期批次二开发完成与主 agent 审计

- Claude Code 开发 agent 已按 `handoff/dev-agent/current-prompt.md` 使用 Ralph Loop 执行二期批次二，完成承诺为 `PHASE2_BATCH2_STANDARD_DELIVERY_COMPLETE`。
- 主 agent 监控中断过两次偏移：
  - 阻止开发 agent 将本批范围扩到 core 项目切换接口，要求撤回该方向。
  - 发现专项脚本把节点类型锁定失败从 `fail` 改成 `pass`，已要求恢复真实失败断言。
- 本批实现重点：
  - 后端新增交付完整率查询，按标准前置条件输出 `standardReady`、`readinessIssues`、总要求数、已完成数、缺失数、完成率和缺失项。
  - 文档交付与图纸交付可基于部位、交付物类型、文件资源进行挂接和删除，关键写动作写审计。
  - 前端文档/图纸交付页补充标准状态、完整率概览、缺失项列表、缺失项快捷挂接和分页文件选择。
  - 新增专项脚本 `scripts/dev/check-phase2-batch2-standard-delivery.sh`。
- 主 agent 审计补充：
  - 补回 `frontend/src/modules/work-center/api/delivery.ts` 中缺失的 `DashboardSummary` 类型定义，避免前端构建依赖未声明类型。
  - 加固 `scripts/dev/check-phase2-batch2-standard-delivery.sh`：复跑时优先复用已有节点类型，并清理本轮创建的部位节点，避免专项脚本继续堆积测试主数据。
- 主 agent 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查返回 `UP`。
  - 专项脚本 `check-phase2-batch2-standard-delivery.sh` 通过，`38/38 PASS`。
  - `git diff --check` 通过。
- 剩余注意：
  - 当前验收脚本不会修改、移动或删除真实 NAS 文件。
  - `targetType=OBJECT` 当前只验证空结果不 500，真实管理对象维度待后续批次有真实对象数据后再扩展。
  - 主 agent 认为可交给测试 agent 做正式验收；是否收口二期批次二以测试 agent 报告为准。

## 2026-05-14：二期批次二正式收口

- 测试 agent 已完成二期批次二正式验收，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前无 `P0 / P1 / P2`。
- 已通过验证：
  - 后端构建。
  - 前端构建。
  - 后端健康检查。
  - `git diff --check`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-agent-db2-contract.sh`。
  - `scripts/dev/check-scan-task-control.sh`。
- 页面短回归通过：
  - `/work/document-delivery`。
  - `/work/drawing-delivery`。
- 已确认文件选择继续使用分页远程请求，不回退到全量加载。
- 已确认没有越界到模型轻量化、构件级解析、正文抽取、向量化、Agent 自动审批/写库、真实 NAS 文件移动/删除/改名。
- 主 agent 已创建收口报告：`handoff/main-agent/phase2-batch2-standard-delivery-closure.md`。
- 下一步建议：进入二期批次三规划，优先做 `审核流、整改闭环、报表导出`，继续保持小批次边界。

## 2026-05-14：交付页整体向右溢出热修复

- 用户反馈：
  - `/work/document-delivery` 页面会整体向右延伸。
  - `/work/drawing-delivery` 页面会整体向右延伸。
- 本轮边界：
  - 只修页面级横向溢出。
  - 不改业务逻辑。
  - 不改权限逻辑。
  - 不改后端接口。
  - 不修改 `docs/**`。
- 根因：
  - 交付页新增完整率、缺失项 Tab 和 Element Plus 表格后，表格列宽与 Tab 宽内容形成较大的内在宽度。
  - `.app-layout__content`、`.mvp-page`、`.mvp-page > *` 和 `.master-table` 缺少必要的 `min-width: 0`、`max-width: 100%` 等收敛约束。
  - 宽内容没有停在局部容器内，而是把页面主内容区和 header 一起撑宽。
- 修复文件：
  - `frontend/src/styles/index.css`。
- 修复内容：
  - 收紧 app 主内容区、header、项目选择区、MVP 页面容器和表格容器的宽度约束。
  - 允许 `.mvp-page` 直接子项在 grid/flex 场景下收缩。
  - 限制 `.master-table` 和 Element Plus 内部 wrapper 不把页面撑宽。
- 验证：
  - 修复前：`/work/document-delivery` 在 1440px 视口下页面滚动宽度约 `2406px`，`/work/drawing-delivery` 约 `2446px`。
  - 修复后：在 `1440px`、`1200px`、`1024px` 三种视口下，两页初始状态、缺失项页签和弹窗状态均为页面级横向溢出 `0`。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
- 报告：
  - `handoff/dev-agent/latest-report.md` 已更新为本轮热修复报告。
- 建议：
  - 测试 agent 做极短复验，只看两页是否仍整体横向溢出、缺失项页签、去挂接弹窗和文件选择分页远程加载是否正常。
- 复验收口：
  - 测试 agent 已完成极短复验，报告：`handoff/test-agent/latest-report.md`。
  - `/work/document-delivery` 与 `/work/drawing-delivery` 初始状态、缺失项页签和去挂接弹窗状态的页面级横向溢出均为 `0`。
  - 文件选择器仍为分页远程加载，未回退到全量加载。
  - 当前无 `P0 / P1 / P2`，本热修复正式收口。

## 2026-05-14：二期批次三规划与 agent prompt 交接

- 用户在二期批次二与交付页横向溢出热修复均收口后确认继续。
- 主 agent 裁决：进入二期批次三，但继续保持小批次边界，不进入模型轻量化、构件级解析、正文抽取、Agent workflow 或受控写操作。
- 二期批次三名称固定为：`人工审核、整改闭环与基础报表导出`。
- 核心验收链路固定为：`交付挂接 -> 人工审核 -> 驳回产生整改 -> 整改处理 -> 复审关闭 -> 报表导出 -> 审计留痕`。
- 已新增主 agent 规划文档：`handoff/main-agent/phase2-batch3-review-rectification-report-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
  - 要求开发 agent 使用 Ralph Loop。
  - 完成承诺：`<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`。
  - 明确禁止修改 `docs/**`、旧 Flyway 迁移、真实 NAS 文件，以及越界到 Agent 自动审批/自动整改/自动写库、模型轻量化、构件级解析、正文抽取、向量库或完整 BPM。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
  - 要求验收审核状态流转、驳回生成整改项、整改状态闭环、CSV 导出、权限审计、OpenAPI 和 NAS 安全。
- 已更新主状态：`handoff/main-agent/status.md`。
- 边界确认：本轮只是规划和交接 prompt，未写业务代码，未改 `docs/**`。

## 2026-05-13：Agent 预览页权限证明文件 ID 交互优化

- 用户反馈：`/data-steward/agent-preview` 的权限证明要求手动输入文件 ID，但页面没有明显告诉用户文件 ID 是什么，也不知道 ID 对应哪个文件。
- 主 agent 判断：现有前端 `CatalogFile` 已包含 `fileId/fileName/projectCode/projectName`，`fetchFilePermissionProof(fileId)` 已存在，本轮可只改前端，不需要改后端权限逻辑。
- 修复内容：
  - Agent 预览页资产目录样例表格新增 `文件ID` 列。
  - 样例表格每行新增 `验证权限` 操作，可直接基于当前行文件发起权限证明。
  - 权限证明结果区新增 `当前验证对象`：表格触发时显示文件 ID、文件名、项目编号、项目名称；手动输入时显示手动输入和文件 ID。
  - 手动改 ID 时会清理旧行上下文，避免旧文件名继续显示造成误导。
  - 资产目录页文件详情抽屉新增 `文件ID`。
- 验证：
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - 浏览器回归使用项目 `105 启航华居项目`，表格点击验证、手动输入 `46` 验证、目录详情显示文件 ID 均通过。
  - 页面宽度稳定为 `1440/1440/1440`，未横向撑爆。
- 报告：`handoff/dev-agent/latest-report.md`。
- 边界确认：未改后端，未修改 `docs/**`，未新增 Agent 写库、NAS 写操作、正文抽取、向量化、自动审批、模型轻量化或构件级能力。

## 2026-05-13：一期资产详情与数据质量人工治理补强

- 用户确认暂时跳过企业 agent 合并，继续补强一期主线。
- 主 agent 将范围锁定为内部资产治理能力，不进入企业 agent 合并、正文抽取、向量库、自动治理、自动审批、NAS 文件写操作、模型轻量化或构件级解析。
- 后端改动：
  - 新增 `PATCH /api/data-steward/assets/files/{fileId}/metadata`。
  - 可人工更新文件类型、专业、版本、置信度、审核状态。
  - 更新前校验项目权限，更新后写审计日志和事件流 `file.metadata.update`。
  - 未新增 Flyway 迁移，未修改 NAS 文件。
- 前端改动：
  - `/data-steward/assets/:projectId` 文件列表改用服务端分页。
  - 文件表格新增文件 ID、质量问题、详情、治理、补 checksum 任务入口。
  - 新增文件详情抽屉，展示识别信息、治理状态、来源与路径。
  - 新增人工治理弹窗，可补录专业、版本、置信度、审核状态和文件类型。
  - `补 checksum` 只创建后端任务，不同步计算、不读取正文。
- 新增脚本：`scripts/dev/check-asset-detail-governance.sh`。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查返回 `UP`。
  - 新增脚本 `check-asset-detail-governance.sh` 通过。
  - 既有脚本 `check-asset-quality-overview.sh` 通过。
  - 浏览器回归：`/data-steward/assets/2` 文件详情抽屉、人工治理弹窗、分页器可用，页面宽度稳定。
- 报告：`handoff/dev-agent/latest-report.md`。

## 2026-05-14：二期批次三开发 agent 完成与主 agent 审计

- 用户确认进入二期批次三后，主 agent 通过项目目录下 Claude Code CLI 调用开发 agent，模型使用 `deepseek-v4-pro`。
- 开发 agent 已按 Ralph Loop 完成 `人工审核、整改闭环与基础报表导出`，完成承诺为 `<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`。
- 本批新增能力：
  - 交付绑定提交审核、审核通过、审核驳回。
  - 审核记录查询。
  - 驳回后自动生成整改项。
  - 整改项列表、详情、更新、标记已处理、关闭、重新打开。
  - 交付完整率、审核汇总、整改项三类 CSV 导出。
  - 前端交付页审核动作、审核记录抽屉、驳回弹窗、整改中心页面和导出入口。
  - Flyway 追加 `V19__batch3_review_rectification_report.sql`，未修改旧迁移。
- 主 agent 审计边界：
  - 未发现进入完整 BPM、多级会签、Agent 自动审批/自动整改/自动写库、模型轻量化、构件级解析、正文抽取、向量库或真实 NAS 文件操作。
  - 新增接口均校验当前项目上下文；本批仍只证明项目隔离，不承诺客户生产级细粒度审批角色体系。
- 主 agent 补丁：
  - 调整 `scripts/dev/check-phase2-batch3-review-rectification-report.sh`。
  - 原脚本验证 `reopen` 后会留下隐藏开放整改测试数据；现改为 `reopen` 后再执行 `resolve + close` 并断言通过，减少复跑污染。
- 主 agent 验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查 `/actuator/health` 返回 `UP`。
  - `git diff --check` 通过。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
- 当前裁决：
  - 主 agent 审计通过，当前无已知 P0/P1。
  - 可交给测试 agent 按 `handoff/test-agent/current-prompt.md` 做正式验收。
  - 二期批次三是否正式收口以测试 agent 报告为准。

## 2026-05-14：二期批次三正式收口

- 测试 agent 已完成二期批次三正式验收，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过。
- 当前问题分级：
  - P0：无。
  - P1：无。
  - P2：前端构建仍有 Vite chunk size warning，不阻塞收口。
- 已通过验证：
  - 后端构建。
  - 前端构建。
  - 后端健康检查。
  - `git diff --check`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-agent-db2-contract.sh`。
- 页面正式验收通过：
  - `/work/document-delivery`：提交审核、审核通过、驳回、审核记录、CSV 导出可用，页面级横向溢出为 `0`。
  - `/work/drawing-delivery`：页面可打开，缺失项和新增挂接弹窗不撑宽，文件选择仍为分页远程加载。
  - `/work/rectifications`：整改项标记已处理、关闭、重新打开和 CSV 导出可用，页面级横向溢出为 `0`。
- 安全边界：
  - 普通用户跨项目审核、整改、导出均被受控拒绝。
  - 审计覆盖审核、驳回、整改处理、关闭、重开、报表导出。
  - 未触碰真实 NAS 文件，未读取文件正文，未破坏 DB-2 四个稳定 View。
- 主 agent 已创建收口报告：`handoff/main-agent/phase2-batch3-review-rectification-report-closure.md`。
- 下一步建议：
  - 进入二期批次四规划，优先考虑 `文件预览与下载权限分离最小闭环`。
  - 继续保持小批次边界，不进入模型轻量化、构件级解析、正文抽取、向量库、Agent workflow 或受控写操作。

## 2026-05-14：标准驱动交付流程新手可用性补丁

- 用户要求先不进入二期批次四规划，先修复测试报告中指出的文档问题。
- 主 agent 判断：测试 agent 最新报告实际是“标准驱动交付流程新手视角可用性复核报告”，其中 P1 指向页面说明、顺序引导和修复入口不足；本轮应修前端页面，不修改正式 `docs/**`。
- 本轮边界：
  - 只改五个目标页面的新手可用性说明。
  - 不改后端模型。
  - 不改数据库。
  - 不触碰真实 NAS 文件。
  - 不进入二期批次四。
- 修改文件：
  - `frontend/src/modules/master-data/components/StandardStatusPanel.vue`
  - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
  - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
  - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
  - `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
  - `frontend/src/styles/index.css`
- 修复内容：
  - 工程管理部位树页面增加第 1 步说明、业务示例和字段提示。
  - 节点类型页面增加第 2 步说明、锁定含义、锁定确认提示和适用层级解释。
  - 交付物标准页面增加第 3 步说明、推荐配置顺序、四块配置关系和字段提示。
  - `StandardStatusPanel` 增加按当前状态变化的下一步提示。
  - 文档/图纸交付页面增加交付执行说明、标准未就绪修复入口、缺失项解释和补交弹窗字段说明。
  - 将“去挂接”类关键动作调整为更业务化的“选择文件补交”。
- 未做项：
  - 未清理样板项目历史测试命名数据，避免混入数据治理动作。
  - 未补齐样板项目文档类标准，避免把本轮页面可用性补丁扩成样板数据维护。
  - 未做标准配置向导、帮助中心或客户版完整新手引导。
- 验证：
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - 五个目标路由均返回 HTTP 200。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `git diff --check` 通过。
- 报告：
  - 主报告：`handoff/main-agent/standard-delivery-usability-patch-report.md`。
  - 开发报告：`handoff/dev-agent/latest-report.md`。
  - 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 当前裁决：
  - 本轮 P1 已完成开发修复。
  - 建议测试 agent 做短回归后再正式收口本补丁。

## 2026-05-14：标准驱动交付流程新手可用性补丁正式收口

- 测试 agent 已完成短回归，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过。
- 当前问题分级：
  - P0：无。
  - P1：无。
  - P2：前端构建仍有既有 Vite chunk size warning。
- 上一轮 P1 已关闭：
  - `标准驱动交付链路对新手缺少业务解释、顺序引导和修复入口提示`。
- 五个页面逐项验收通过：
  - `/master-data/sections`：第 1 步说明、部位树作用、业务示例、弹窗辅助说明通过。
  - `/master-data/node-types`：第 2 步说明、节点类型与部位层级关系、锁定意义和确认说明通过。
  - `/master-data/deliverable-standard`：第 3 步说明、推荐配置顺序、四块配置说明、关键字段提示通过。
  - `/work/document-delivery`：前置条件缺失时可看到修复入口，页面级横向溢出为 `0`。
  - `/work/drawing-delivery`：缺失项解释、`选择文件补交`、补交弹窗字段说明、分页远程文件选择和横向溢出检查通过。
- 脚本回归：
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
- 非阻塞观察：
  - 样板项目里仍有若干 `PH2B2-*` 测试节点命名，这属于样板数据整理或演示观感优化，不影响本轮 P1 收口。
- 主 agent 已创建收口报告：`handoff/main-agent/standard-delivery-usability-patch-closure.md`。
- 下一步建议：
  - 先做 `样板项目演示数据清理与标准闭环补齐`。
  - 不建议立即进入二期批次四，因为当前更需要把样板项目调整到适合内部验收和领导演示的状态。

## 2026-05-15：样板项目清理暂缓，恢复二期主线

- 用户最新裁决：样板项目仍需继续用于业务链路测试，因此暂不清理、暂不重置、暂不隐藏 `PH2B2-*` 等历史测试命名。
- 主 agent 调整：
  - 样板项目整理降级为后续演示观感优化项，不再作为进入下一批次前的阻塞项。
  - 真实 NAS 数据仍保持只读保护，不因样板项目决策发生任何移动、删除或改名。
  - 开发和测试 agent 当前仍保持等待状态，不能自行开工。
- 下一步建议：
  - 恢复二期主线推进，进入 `二期批次四：文件预览与下载权限分离最小闭环` 规划。
  - 批次四仍保持小范围：只处理文件预览入口、下载权限与查看权限分离、访问审计和最小回归脚本。
  - 暂不进入模型轻量化、构件级解析、正文抽取、向量库、Agent workflow、自动审批或真实 NAS 写操作。

## 2026-05-15：二期批次四规划与 prompt 就绪

- 主 agent 已创建二期批次四规划文档：`handoff/main-agent/phase2-batch4-file-preview-download-permission-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
- 批次四固定范围：
  - 文件预览与下载权限分离。
  - 短时访问票据。
  - 平台受控读取文件。
  - PDF/图片浏览器原生预览。
  - 下载/预览/拒绝/失败审计。
  - 专项脚本 `scripts/dev/check-phase2-batch4-file-access.sh`。
- 批次四禁止范围：
  - 样板项目清理。
  - 模型轻量化。
  - 构件级解析。
  - Office/CAD/BIM 转换。
  - 正文抽取或向量库。
  - Agent 自动业务动作。
  - 真实 NAS 写操作。
- 当前状态：规划和 prompt 已就绪，尚未启动开发 agent。

## 2026-05-15：二期批次四主 agent 收拢完成，待测试复验

- 主 agent 已启动 Claude Code 开发 agent 两次：
  - 第一次长 prompt 通过 `script` 包装运行，长期无输出，但留下早期后端雏形。
  - 第二次短 prompt 直接调用 `claude --print`，仍无完成输出、无报告。
- 主 agent 中断卡住的 Claude 会话后接管开发，完成批次四最小闭环。
- 已完成改动：
  - 新增 `V20__batch4_file_access_tickets.sql`。
  - 新增文件预览/下载权限：`DATA_STEWARD_FILE_PREVIEW`、`DATA_STEWARD_FILE_DOWNLOAD`。
  - 新增短时访问票据表 `data_file_access_tickets`。
  - 新增票据创建接口和票据访问接口。
  - 允许票据访问 URL 免 JWT，但创建票据必须先经登录用户权限校验。
  - 支持 `nas:///Volumes/...` 和 `nas:///tmp/...` 只读流式读取。
  - 文件详情页和资产目录抽屉增加预览/下载权限显示与受控访问入口。
  - 新增专项脚本 `scripts/dev/check-phase2-batch4-file-access.sh`。
- 关键修复：
  - 首轮专项脚本发现拒绝/失败审计被事务回滚吞掉。
  - 已移除票据创建/打开方法的大事务包裹，使权限拒绝和路径失败审计能落库。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=12 FAIL=0`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `git diff --check`：通过。
- 边界确认：
  - 未清理样板项目。
  - 未做模型轻量化、构件级解析、Office/CAD/BIM 转换、正文抽取、向量库或 Agent 自动动作。
  - 未写入、移动、删除或改名真实 NAS `/Volumes/zyzn/卓羽智能项目`。
- 当前裁决：
  - 主 agent 自测通过。
  - 建议测试 agent 按 `handoff/test-agent/current-prompt.md` 做正式验收。

## 2026-05-15：二期批次四 P0 返修完成，待测试复验

- 测试 agent 正式验收首轮结论：不通过，存在明确 P0，不能收口二期批次四。
- P0 内容：
  - 普通项目用户可在 `/data-steward/catalog` 文件详情和目录详情接口中看到真实 `nas:///...` 路径。
  - 受影响角色包括 `phase2.viewer` 和 `delivery.engineer`。
- 根因判断：
  - 文件预览/下载票据链路已经受控，但只读目录接口仍沿用旧的路径可见性逻辑，把“本地开发环境路径可见”错误扩大到了普通项目用户。
- 修复文件：
  - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
  - `scripts/dev/check-phase2-batch4-file-access.sh`
- 修复内容：
  - 目录列表与目录详情统一按项目角色判断路径可见性。
  - 仅 `PROJECT_ADMIN` 返回真实 `storagePath`。
  - `PROJECT_VIEWER`、`DELIVERY_ENGINEER` 等普通项目角色返回 `storagePathVisible=false`、`storagePath=null`、`storagePathVisibilityReason=PATH_HIDDEN_BY_PERMISSION`。
  - `agentContractView` 在路径隐藏时不再声明 `storagePath` 字段可用。
  - 专项脚本新增查看者和交付工程师目录详情路径隐藏断言。
- 回归结果：
  - 后端构建通过。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=14 FAIL=0`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - `git diff --check` 通过。
- 当前裁决：
  - 主 agent 认为测试报告中的 P0 已修复。
  - 二期批次四仍不直接收口，需测试 agent 做正式复验后再关闭。

## 2026-05-15：暂停继续扩功能，转入导航与项目工作台重组规划

- 用户最新反馈：
  - 当前平台前端对各项目的管理和跳转混乱。
  - 首页不是最常用工作入口。
  - 文件资产、工程主数据、工作中心割裂。
  - 项目详情页没有形成真正项目工作台。
  - 顶部全局“当前项目”切换器造成“全局项目”和“页面项目”并存的困惑。
  - 项目详情里的文件资产应改成类似文件管理器的左目录、右文件结构。
- 主 agent 已停止继续推进新业务功能，不启动开发 agent。
- 已阅读并确认当前结构：
  - `frontend/src/router/index.ts`：`/`、登录后已认证跳转、兜底路由都指向 `/home`。
  - `frontend/src/modules/core/layout/AppLayout.vue`：顶部存在全局“当前项目”下拉，切换后跳回 `/home`。
  - `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/user/application/CurrentUserApplicationService.java`：工程主数据和工作中心作为全局菜单返回。
  - `AssetProjectDetailPage.vue`：当前是项目资产详情，文件资产为平铺表格。
  - `AssetCatalogPage.vue`：已有可复用的目录浏览能力，包括 `fetchCatalogDirectories` 和 `fetchCatalogFiles`。
  - 主数据和工作中心页面主要依赖 `authStore.currentProjectId`，不是路由中的 `projectId`。
- 根因判断：
  - 平台当前同时存在 token 当前项目、顶部全局项目切换器、项目详情路由和全局主数据/工作中心菜单。
  - 用户进入项目详情后，后续主数据和工作中心仍像全局页面，缺少“当前项目工作台”的壳层。
  - 文件资产页没有复用已完成的目录浏览能力，导致大项目下仍像平铺资产表。
- 改造范围收束：
  - 默认入口改为 `/data-steward/assets`。
  - `/data-steward/assets/:projectId` 升级为项目工作台。
  - 项目工作台顶部提供工程主数据和工作中心入口。
  - 新增项目内路由别名，保留旧路由兼容。
  - 进入项目内页面时同步 route `projectId` 与 token currentProject，避免 `CORE_PROJECT_CONTEXT_MISMATCH`。
  - 文件资产区复用 catalog 目录能力，改为左目录右文件。
  - 顶部全局项目切换器移除或隐藏。
  - 不修改 `docs/**`，不改数据库迁移，不碰真实 NAS 文件，不做模型轻量化、正文抽取、向量库或 Agent 自动动作。
- 已写入交接文件：
  - 规划文件：`handoff/main-agent/navigation-project-workbench-reorg-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- 当前裁决：
  - 可以在用户确认后启动开发 agent 执行本轮“项目壳层改造”。
  - 本轮完成后必须让测试 agent 做专项回归，重点验收默认入口、项目工作台、目录文件浏览、项目 A/B 上下文切换和既有批次一至四能力不回归。

## 2026-05-15：导航与项目工作台重组已完成主 agent 审计，待测试 agent 复验

- 执行方式：
  - 已通过 Claude Code 开发 agent 执行本轮前端改造，要求使用 Ralph Loop，未创建子 agent。
  - 第一次 Claude 会话因尝试长时间整文件重写 `AssetProjectDetailPage.vue` 且迟迟未落盘，被主 agent 中断。
  - 第二次改为续跑 prompt，以“小组件 + 小补丁”方式完成，便于追踪和审计。
- 已落地范围：
  - `/`、登录成功、兜底路由都进入 `/data-steward/assets`。
  - `/data-steward/assets/:projectId` 已升级为项目工作台。
  - 项目工作台顶部增加项目内导航：文件资产、工程主数据、工作中心。
  - 新增项目内路由：主数据三页、文档交付、图纸交付、整改闭环、项目驾驶舱。
  - 顶部全局“当前项目”切换器已移除；全局侧边栏隐藏旧 `首页`、`工程主数据`、`工作中心` 顶级入口。
  - 文件资产区改为左目录树、右文件表，复用 catalog 目录/文件 API。
- 主 agent 审计补丁：
  - 修复登录页仍跳 `/home` 的遗漏。
  - 增加路由守卫，进入项目内页面前先同步 route `projectId` 到当前项目，避免主数据/工作中心页面抢先使用旧项目上下文。
  - 修复 `AssetProjectFileBrowser` 首屏只加载目录、不加载第一页文件的问题。
  - 增加目录/文件请求序号保护，快速切换项目或筛选时旧响应不会覆盖新状态。
  - 保留 `?qualityIssue=` 进入项目详情后的质量问题筛选能力。
  - 治理保存、checksum 成功后刷新目录式文件表，不再刷新旧的隐藏平铺表。
  - 清理 `AssetProjectDetailPage.vue` 中已废弃的旧平铺文件表状态，避免后续维护误判。
- 修改文件：
  - `.claude/ralph/progress.txt`
  - `frontend/src/modules/auth/pages/LoginPage.vue`
  - `frontend/src/modules/core/layout/AppLayout.vue`
  - `frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`
  - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
  - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
  - `frontend/src/router/index.ts`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/dev-agent/latest-report.md`
  - `handoff/dev-agent/project-workbench-continuation-prompt.md`
  - `handoff/main-agent/navigation-project-workbench-reorg-plan.md`
  - `handoff/test-agent/current-prompt.md`
- 边界确认：
  - 未修改 `docs/**`。
  - 未修改后端代码和数据库迁移。
  - 未写入、移动、删除或改名真实 NAS 文件。
  - 未扩展模型轻量化、正文抽取、向量库或 Agent 自动动作。
- 验证结果：
  - `corepack pnpm --dir frontend build`：通过，仅既有 Vite chunk size warning。
  - `curl -fsS http://localhost:8080/actuator/health`：`{"status":"UP"}`。
  - `git diff --check`：通过。
- 当前裁决：
  - 主 agent 审计通过。
  - 不能直接收口，建议测试 agent 按 `handoff/test-agent/current-prompt.md` 做专项复验。
  - 重点验收默认入口、项目工作台、左目录右文件、项目 A/B 上下文切换、批次四路径隐藏不回归。

## 2026-05-15：导航与项目工作台测试报告 P1/P2 返修完成，待短回归

- 测试 agent 最新报告结论：
  - 当前无 P0。
  - 存在 1 个 P1：左侧全局菜单仍显示 `工程主数据 / 工作中心`，项目内外导航边界仍有混淆。
  - 存在 1 个 P2：项目工作台顶栏没有明确展示负责人。
- P1 修复：
  - 修复文件：`frontend/src/modules/core/layout/AppLayout.vue`。
  - 修复方式：侧边栏过滤从单纯路径精确匹配升级为 `菜单 key + 路径前缀` 双重过滤。
  - 当前效果：全局侧边栏不再显示 `首页 / 工程主数据 / 工作中心` 这些会造成混淆的顶级入口，保留 `数据管家` 作为平台级主入口；工程主数据和工作中心继续通过项目工作台顶栏进入。
  - 兼容性：旧 `/master-data/*`、`/work/*` 路由仍保留，访问时仍会兼容跳转到当前项目内页面。
- P2 修复：
  - 修复文件：
    - `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/project/domain/AccessibleProject.java`
    - `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/project/dto/ProjectSummaryResponse.java`
    - `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/project/application/ProjectAccessApplicationService.java`
    - `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/project/repository/ProjectAccessRepository.java`
    - `frontend/src/modules/core/api/types.ts`
    - `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
  - 修复方式：当前用户项目摘要增加 `projectManagerName` 字段，项目工作台顶栏显示 `负责人：xxx`；为空时显示 `负责人：待维护`。
- 主 agent 验证结果：
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 后端构建通过，8 个模块 SUCCESS。
  - `git diff --check` 通过。
  - 临时 `18080` 后端接口验证通过：`/api/core/users/me` 返回项目摘要包含 `projectManagerName`，按前端过滤逻辑后侧边栏只剩 `数据管家`；临时后端已停止。
- 当前裁决：
  - 主 agent 认为测试报告中的 P1/P2 已修复。
  - 不直接收口，建议测试 agent 做极短回归：重点看左侧菜单是否只保留平台级入口、项目工作台顶栏是否显示负责人、项目内工程主数据/工作中心导航是否不回归。

## 2026-05-15：阶段二前端批次一转入数据管家资产驾驶舱

- 用户重新明确阶段二前端重点：
  - 当前前端仍混乱，需要优先围绕数据管家做项目级数据可视化。
  - 已参考 RealBIM 数据管家的文件管理、模型集成、事项列表、任务列表、导出列表、管理对象、文件服务和智慧大屏形态。
  - 本批次不做二期深水区能力，不做真实 NAS 写操作，不做模型轻量化、构件级解析、正文抽取、向量库或 agent 自动动作。
- 本轮落地范围：
  - `/data-steward/assets/:projectId` 默认进入“资产驾驶舱”，第一屏展示项目资产统计和治理风险。
  - 驾驶舱接入文件总数、模型数、图纸数、文档数、总容量、路径映射、文件类型分布、专业分布、最近扫描、最近入库、治理动态。
  - 文件管理区继续复刻 RealBIM 核心结构：左目录树、面包屑、右文件表、高级搜索、文件 ID、行内“更多”操作。
  - 大项目目录树默认只展开根和一级目录，避免真实 NAS 项目一次性展开过多节点导致卡顿。
  - 资产项目路由增加 `assetProjectContext`，避免资产项目 ID 被当成核心项目 ID 同步，修复 `/data-steward/assets/506` 这类真实 NAS 项目上下文混淆。
- 后端最小修复：
  - `AssetQualityRepository` 修复 SQL 文本拼接问题，避免 `:projectId` 与 `HAVING` 粘连。
  - 质量概览接口已恢复：`/api/data-steward/assets/quality/overview?projectId=506` 返回 `200 OK`，当前真实数据 `riskSignalCount=5923`、`missingChecksumCount=5912`。
- 验证结果：
  - `corepack pnpm --dir frontend build` 通过，仅既有 Vite chunk size warning。
  - `cd backend && ./mvnw -pl delivery-app -am -DskipTests package` 通过，8 个模块 SUCCESS。
  - `curl -fsS http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
  - `git diff --check` 通过。
  - Chrome 页面验证：`http://localhost:5173/data-steward/assets/506` 可显示真实项目 `93 中建八局国交酒店项目`，驾驶舱显示 5,912 个文件、588 个模型、5,324 张图纸、72.57 GB、5,912 个缺 checksum 风险；文件管理页显示目录树和文件表。
- 当前裁决：
  - 主 agent 认为阶段二前端批次一核心实现已完成，可以进入测试 agent 专项回归。
  - 测试重点：驾驶舱真实统计、风险卡片跳转、文件管理目录筛选、更多操作不丢失、大项目不卡死、页面不横向撑爆。

## 2026-05-15：文件管理目录树可用性问题已修复

- 用户反馈：
  - 文件管理左侧目录结构下方有大量留白，但目录内容被裁切。
  - 空文件夹也必须保留在目录结构中，保证目录结构真实。
  - 需要增加双击文件夹进入目录的交互。
  - 新建文件夹按钮不可用，需要明确原因。
  - 左侧 tab/menu 区背景为黑色、字体也发黑，导致不可见。
- 修复范围：
  - `DirectoryTreePanel.vue`：目录面板改为自适应高度布局，滚动发生在目录面板内部，不再出现下方留白但内容被截断。
  - `DirectoryTreeNodeItem.vue`、`AssetProjectFileBrowser.vue`：目录节点支持双击进入，同时保留单击选择目录。
  - `CatalogApplicationService.java`：目录接口在原有文件元数据目录基础上，只读合并项目路径映射下的真实物理目录，保留 0 文件空目录；增加项目权限校验、最大目录数和最大深度保护。
  - `directoryTree.ts`：避免项目根目录重复展示。
  - `AssetProjectFileBrowser.vue`：新建、上传、导入等按钮增加只读阶段说明；面包屑只显示项目内相对目录，不展示本机挂载路径分段。
  - `index.css`：补齐深色侧边栏菜单文字、图标、hover 和选中态颜色。
- 产品边界裁决：
  - 当前创建不了文件夹是正确行为：一期和本批次仍是 NAS 只读接管，不向真实 NAS 写入、移动或删除文件夹。
  - 新建文件夹、上传、移动、删除、更新版本应留到后续“受控写操作”阶段，并且必须有审批、审计和回滚策略。
- 验证结果：
  - 前端构建通过，仅既有 Vite chunk size warning。
  - 后端构建通过，8 个模块 SUCCESS。
  - `curl -fsS http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
  - `git diff --check` 通过。
  - `/api/data-steward/catalog/directories?projectId=506` 返回 673 个目录，其中 88 个为 0 文件目录。
  - Chrome 页面验证：项目 506 文件管理页可见空目录 `00_工作进度`，双击可进入，右侧显示空目录状态；左侧菜单文字可读，目录树未再裁切。
- 当前裁决：
  - 主 agent 认为本轮可用性修复已完成。
  - 建议测试 agent 做极短回归：目录树高度、空目录展示、双击进入、禁用按钮提示、侧边栏可读性。

## 2026-05-15：文件管理左右缩放、checksum 入口与重复操作收口

- 用户反馈：
  - 文件管理左侧目录树需要像文件管理器一样自由调整宽度，右侧文件表要跟随缩放，不能出现点击区域或表格布局错位。
  - 当前大量文件缺 checksum，需要明确怎么改善。
  - 文件行“更多”里的 `预览` 和 `受控访问` 打开的页面相同，容易造成用户困惑。
- 主 agent 裁决：
  - 左右拖拽属于阶段二数据管家可用性主线，可以当前批次处理。
  - checksum 不应一次性强推全量 10TB 补算，先在 `缺 checksum` 筛选场景提供受控批量补算入口，单次最多 500 个任务，由用户确认后执行。
  - `受控访问` 与 `预览` 入口重复，应合并为一个更清楚的 `预览/下载`，实际受控打开/下载继续留在弹窗内。
- 开发改动：
  - `AssetProjectFileBrowser.vue` 增加目录树/文件表之间的可拖拽分隔条，支持拖拽、键盘调整、双击恢复默认，并在宽度变化后触发表格重新布局。
  - `AssetProjectFileBrowser.vue` 在 `缺 checksum` 筛选下展示说明和“创建本项目补算任务”入口。
  - `dataSteward.ts` 增加 `createBatchChecksumJobs(projectId)`，复用既有批量 checksum 后端接口。
  - `AssetProjectDetailPage.vue` 增加批量 checksum 二次确认和执行逻辑，确认文案明确“不修改、不移动、不删除 NAS 文件”。
  - 文件行更多菜单删除重复 `受控访问`，将 `预览` 调整为 `预览/下载`。
- 验证结果：
  - `corepack pnpm --dir frontend build` 通过，仅既有 Vite chunk size warning。
  - `curl -fsS http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
  - `git diff --check` 通过。
  - Chrome 手动确认：项目 506 文件管理页可拖动目录树宽度，右侧表格同步收缩；更多菜单不再出现重复的 `受控访问`。
- 风险与边界：
  - 未在真实 NAS 项目上点击批量创建 checksum 任务，避免误触发大量后台读取任务。
  - 本轮没有新增统一任务中心，也没有改后端 checksum 算法。
- 当前裁决：
  - 主 agent 认为用户提出的三项优化已完成。
  - 建议测试 agent 做短回归：拖拽目录树、缺 checksum 筛选入口、更多菜单去重、预览/详情/治理/补 checksum 不回归。

## 2026-05-15：二期主视角切换与路线文档完成

- 用户要求：
  - 一期已经收口，后端数据库已经完成。
  - 当前 docs 多数仍以一期为主，需要减少上下文占用。
  - 在围绕 PRD 和现有文档的前提下，把项目主视角切换到二期，并基于当前二期开发进度规划后续路线。
- 本轮已盘点：
  - `docs/07-complete-delivery-prd.md`
  - `docs/08-acceptance-and-agent-integration.md`
  - `docs/03-architecture-and-system-design.md`
  - `docs/README.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/decisions.md`
  - 二期批次一到四相关计划和收口文件
  - 最新开发报告和测试报告
  - 前端模块、后端模块、Flyway 迁移和验收脚本清单
- 当前判断：
  - 一期后端数据库、NAS 资产接管、稳定读模型、权限审计和删除隔离能力已收口。
  - 二期批次一到三已收口。
  - 项目工作台导航重组、数据管家资产驾驶舱、文件管理重做和三项优化已形成当前二期前端基线。
  - 二期批次四文件访问安全闭环仍应作为下一道客户交付安全闸门重新复验和正式收口。
- 新增文档：
  - `docs/10-phase2-development-roadmap.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
- 更新文档：
  - `docs/README.md` 增加二期路线入口和上下文减负建议。
  - `handoff/main-agent/status.md` 增加当前主视角和下一步路线。
  - `handoff/main-agent/decisions.md` 增加二期主视角切换裁决。
- 后续推荐路线：
  1. 数据管家前端重做批次一收口。
  2. 批次 4R：文件访问安全闭环复验与收口。
  3. 批次 5：数据管家客户版模块补齐。
  4. 批次 6：客户项目初始化与标准模板化。
  5. 批次 7：文件预览转换与批量交付增强。
  6. 批次 8：BIM 轻量化适配层。
  7. 批次 9：构件级解析、搜索与交付联动。
  8. 批次 10：客户部署、运维和交付文档包。
- 自检：
  - 新增和修改的路线文档未发现 `TODO/TBD/待定/占位`。
  - 本轮新增和修改的路线文档 `git diff --check` 通过。
  - 仓库整体 `git diff --check` 当前仍会报告 `handoff/test-agent/latest-report.md` 中测试 agent 写入的行尾空格；该文件不是本轮规划文档修改范围，暂未改动。

## 2026-05-15：Hermes / 贾维斯耦合文档已纳入二期路线

- 用户提供：
  - `/Users/vc/Downloads/HERMES_DATA_STEWARD_PLATFORM_COUPLING_GUIDE.md`
- 文档结论：
  - 当前可以开始在平台内嵌 Hermes 数据管家，但第一阶段只能做 `catalog-only / read-only / permission-aware / Missing Evidence / operation plan draft`。
  - 当前不能做 Agent DB CRUD、NAS CRUD、真实正文问答、全量 BIM 解析、OpenSearch/Qdrant/MinIO 写入、自动审批或 production rollout。
  - 平台前端不能直连 Hermes；必须通过平台后端 Agent Gateway。
  - 平台负责权限证明，Hermes 负责遵守权限。
- 当前代码基础复核：
  - 后端已有 `AgentGatewayController`、`AgentGatewayApplicationService`、`AgentPermissionProofService`、`AgentAssetContextResolver`、`HermesAgentClient` 和 `HermesGatewayProperties`。
  - 后端已有 `/api/agent/hermes/capabilities` 与 `/api/agent/hermes/chat`。
  - 前端已有 `DataStewardPanel`、`DataStewardAnswerCard`、`EvidenceModeBadge`、`MissingEvidenceNotice`、`OperationPlanPreview`。
  - 项目详情和资产目录已有 `问数据管家` 入口。
- 主 agent 裁决：
  - 用户可见产品名收束为 `贾维斯数据管家`，技术内核仍称 Hermes。
  - 新增近期优先批次 `批次 5A：贾维斯数据管家内嵌 v0`。
  - 批次 5A 位置：在文件访问安全闭环 `4R` 之后，在数据管家客户版模块补齐 `5B` 之前。
  - 批次 5A 重点：产品化命名、前端亮点入口、平台侧只读审计、Hermes 不可用 fail closed、专项脚本、安全验收。
- 新增文档：
  - `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
- 更新文档：
  - `docs/10-phase2-development-roadmap.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/decisions.md`
  - `docs/README.md`

## 2026-05-18：二期批次 4R 文件访问安全闭环由 Claude Code 完成并通过主 agent 审计

- 用户要求：
  - 写完批次 4R 开发 agent prompt 后，调用项目目录下的 Claude Code 完成开发。
  - Claude Code 必须按 Ralph Loop 执行，主 agent 负责实时监控、防偏航和审计。
- 执行情况：
  - 已写入并执行 `handoff/dev-agent/current-prompt.md`。
  - 已通过 `claude --permission-mode bypassPermissions` 启动 Claude Code。
  - 已要求 Claude Code 读取 prompt 而不是把 prompt 当 shell 执行，并在早期路径误用后纠偏。
  - Ralph 进度已写入 `.claude/ralph/progress.txt`。
- 本轮实际改动：
  - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
    - 文件详情抽屉的“存储路径”从硬编码隐藏改为条件显示。
    - 后端返回路径时展示路径；后端不返回路径时展示“路径已隐藏，请使用平台受控预览或下载入口”。
  - `scripts/dev/check-phase2-batch4-file-access.sh`
    - 新增交付工程师可预览和下载断言。
    - 新增 catalog 文件列表不暴露 NAS 路径断言。
    - 新增预览票据不返回下载响应断言。
  - `handoff/dev-agent/latest-report.md`
    - 已写入本轮开发报告和完成承诺。
- Claude Code 验证结果：
  - 前端构建通过。
  - 后端 Maven 构建通过，8 个模块 `SUCCESS`。
  - 临时后端 `http://localhost:18080` 健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`18/18 PASS`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
- 主 agent 审计：
  - 已复核 `handoff/dev-agent/latest-report.md` 和 `.claude/ralph/progress.txt`。
  - 已对本轮关键文件执行 `bash -n` 与局部 `git diff --check`，通过。
  - 未发现本轮新增 `docs/**`、Hermes/Jarvis、模型/Office/CAD 转换或真实 NAS 写操作。
  - Claude Code 结束时 Ralph stop hook 出现 CLI 层面的余额错误，但报告、进度和完成承诺均已落盘，主 agent 不判定为业务阻塞。
- 当前裁决：
  - 批次 4R 开发侧可以进入测试 agent 专项复验。
  - 后续测试应重点覆盖：路径隐藏、预览/下载权限分离、短时票据、cross-action 防护、审计无敏感路径、前端详情抽屉路径展示。

## 2026-05-18：二期批次 4R 测试复验通过并正式收口

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 测试 agent 总结论：
  - 通过。
  - 当前无 `P0 / P1`。
  - 仅有既有 `Vite chunk size warning` P2，不影响本轮收口。
- 核心验收结论：
  - 普通项目用户不会在 catalog 列表、catalog 详情、项目文件详情中看到真实 NAS 路径。
  - 管理员在有权限项目详情场景下仍可看到必要真实路径。
  - `PROJECT_VIEWER` 可预览、不可下载。
  - `DELIVERY_ENGINEER` 可预览、可下载。
  - 预览票据不会跨动作变成下载响应。
  - 文件访问成功、拒绝、失败都有审计，且未发现票据 secret、真实路径或 NAS 绝对路径泄露。
  - 前端详情抽屉路径展示与后端返回一致，未新增文件表路径暴露列。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过，实际验收端口为 `8080`。
  - 局部 `git diff --check` 通过。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`18/18 PASS`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：通过。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：通过。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：通过。
- 主 agent 裁决：
  - 二期批次四/4R 正式收口。
  - 下一步可进入 `批次 5A：贾维斯数据管家内嵌 v0`，或如需先补齐 RealBIM 数据管家业务模块，则进入 `批次 5B：数据管家客户版模块补齐`。

## 2026-05-18：二期批次 5A 贾维斯数据管家内嵌启动

- 用户裁决：
  - 先做 `批次 5A：贾维斯数据管家内嵌`。
- 主 agent 边界确认：
  - 本批只做 `catalog-only / read-only / permission-aware / Missing Evidence / operation plan draft`。
  - 不做正文抽取、向量库、OpenSearch/Qdrant/MinIO 写入、NAS 写操作、Agent 自动审批、自动整改、自动删除、模型轻量化或构件级解析。
  - 继续沿用 4R 文件访问安全基线，不能暴露真实 NAS 路径。
- 已完成准备：
  - 复核 `handoff/main-agent/hermes-jarvis-coupling-roadmap.md` 和 `docs/10-phase2-development-roadmap.md`。
  - 复核现有后端 Hermes Gateway：`AgentGatewayController`、`AgentGatewayApplicationService`、`AgentAssetContextResolver`、`AgentPermissionProofService`、`HermesAgentClient`、`HermesGatewayProperties`。
  - 复核现有前端问答组件：`DataStewardPanel`、`DataStewardAnswerCard`、`EvidenceModeBadge`、`MissingEvidenceNotice`、`OperationPlanPreview`。
  - 已将开发 agent 当前 prompt 更新为 `二期批次 5A 贾维斯数据管家内嵌 v0`：`handoff/dev-agent/current-prompt.md`。
- 开发要求：
  - 开发 agent 必须使用 Ralph Loop。
  - 完成承诺固定为 `<promise>PHASE2_BATCH5A_JARVIS_DATA_STEWARD_EMBEDDED_COMPLETE</promise>`。
  - 必须新增专项脚本 `scripts/dev/check-hermes-jarvis-gateway.sh`。
  - 完成后必须写入 `handoff/dev-agent/latest-report.md`。

## 2026-05-18：二期批次 5A 贾维斯数据管家内嵌完成开发侧验证

- 执行说明：
  - 主 agent 已启动 Claude Code 并发送 `handoff/dev-agent/current-prompt.md`。
  - Claude Code 当前返回 `API Error: 402 Insufficient Balance`，无法实际执行开发。
  - 为避免主线阻塞，本轮由主 agent 按同一份 5A prompt 直接完成实现、验证和报告。
- 本轮改动：
  - 后端 `AgentGatewayApplicationService`：
    - `capabilities.agentName` 收束为 `贾维斯数据管家`。
    - chat 响应增加平台侧只读审计。
    - 外部 Hermes 响应进入平台前做敏感路径和密钥净化。
    - 正文类问题强制降级为 Missing Evidence，避免把 catalog metadata 伪装为正文 evidence。
    - Hermes 启用但不可用时返回 `agent_unavailable`，不返回 500。
  - 后端 `AgentPermissionProofService`：
    - `allowedActions` 只保留只读白名单：`catalog_query`、`agent_catalog_assist`、`operation_plan_draft`。
  - 前端：
    - 项目工作台和文件详情入口改为 `问贾维斯`。
    - 抽屉标题改为 `贾维斯数据管家`。
    - 问答面板展示资产目录辅助、正文问答未开放、写操作不会执行、生产发布未开放。
    - 回答卡片展示权限、项目范围、权限标签和 fail closed 状态。
    - 操作建议明确为草案，需要人工审批。
  - 脚本：
    - 新增 `scripts/dev/check-hermes-jarvis-gateway.sh`。
- 开发侧验证：
  - 后端构建通过，8 个模块 `SUCCESS`。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 临时后端 `http://localhost:18080` 健康检查 `UP`。
  - `BASE_URL=http://localhost:18080 bash scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=7 FAIL=0`。
  - `BASE_URL=http://localhost:18080 bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - 额外启动 `HERMES_AGENT_GATEWAY_ENABLED=true` 且 `HERMES_MEMORY_BASE_URL=http://127.0.0.1:1` 的临时后端，验证 Hermes 不可用时返回 `agent_unavailable`。
  - 局部 `git diff --check` 通过。
- 报告与测试交接：
  - 开发侧报告已写入 `handoff/dev-agent/latest-report.md`。
  - 测试 agent 当前 prompt 已更新为 5A 专项复验：`handoff/test-agent/current-prompt.md`。
- 当前裁决：
  - 批次 5A 开发侧可进入测试 agent 专项复验。
  - 5A 尚未正式收口，需测试 agent 确认无 P0/P1 后再收口。

## 2026-05-18：二期批次 5A 验收通过并吸收 Hermes V3 联调口径

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 测试 agent 结论：
  - 通过。
  - 当前无 `P0 / P1`。
  - 仅有既有 `Vite chunk size warning` P2，不影响收口。
- 5A 收口结论：
  - 平台对外展示已完成 `Hermes` 到 `贾维斯数据管家` 的用户侧包装。
  - 项目页与文件详情入口均显示 `问贾维斯`。
  - 能力边界清楚：只做 `catalog-only / read-only / permission-aware / Missing Evidence / operation plan draft`。
  - 普通目录问题可返回目录级辅助回答。
  - 正文类问题返回 `missing_evidence`，不编造正文。
  - 无效项目范围 fail closed，不返回 500。
  - Hermes 不可用时返回 `agent_unavailable`。
  - 审计落到 `agent.jarvis.chat.*`，未发现 `nas://`、`/Volumes/`、`token`、`secret` 泄露。
  - 4R 文件访问安全闭环未回归。
- 用户新增输入：
  - `/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`
- V3 路线调整：
  - Hermes 当前口径更新为 `phase-2.89-test-machine-runtime-preflight-handoff-baseline`。
  - V3 允许继续做前端嵌入、平台后端 Gateway、Hermes health、只读 chat/catalog lookup、权限上下文传递和审计 trace 对齐。
  - V3 仍不授权真实 DB 写入、parser、NAS copy、index write、Agent answer integration 或 production rollout。
  - 新增建议小批次 `5A.1：贾维斯 Gateway 合同对齐与联调增强`。
  - 5A.1 仅补 health、平台语义别名、只读 catalog search 壳、trace 关联和 feature flag 状态展示，不进入正文抽取、索引写入、Agent CRUD 或 NAS 写操作。
- 更新文件：
  - `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/jarvis-v3-gateway-alignment-plan.md`
  - `docs/10-phase2-development-roadmap.md`
  - `handoff/main-agent/decisions.md`
  - `handoff/main-agent/status.md`

## 2026-05-18：二期批次 5A.1 贾维斯 Gateway 合同对齐完成开发侧验证

- 执行结论：
  - 批次 5A.1 已按 Hermes V3 联调口径完成开发侧实现。
  - 本批定位为薄网关合同对齐，不进入批次 5B，不做数据库结构扩展。
- 本轮新增和调整：
  - 新增平台语义健康接口：`GET /api/data-steward/hermes/health`。
  - 新增平台语义聊天入口：`POST /api/data-steward/chat`。
  - 保留兼容入口：`GET /api/agent/hermes/health` 和既有 `/api/agent/hermes/chat`。
  - 前端 `贾维斯数据管家` 面板展示健康状态、只读网关模式、运行时写入未开放、正式正文回答未开放和不可用原因。
  - `scripts/dev/check-hermes-jarvis-gateway.sh` 增加 health 和平台语义 chat 断言。
- 安全边界：
  - health 响应不返回 baseUrl、DB 连接串、secret 或真实 NAS 路径。
  - Hermes 不可用时 health 返回降级状态，chat 返回统一响应体并携带 `agent_unavailable`，不执行写操作。
  - 继续不开放正文抽取、selective indexing、OpenSearch/Qdrant/MinIO 写入、Agent CRUD、NAS 写操作、Agent answer integration 和 production rollout。
- 开发侧验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过，仅有既有 Vite chunk size warning。
  - 临时后端 `http://localhost:18080` 健康检查 `UP`。
  - `BASE_URL=http://localhost:18080 bash scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=9 FAIL=0`。
  - `BASE_URL=http://localhost:18080 bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - 额外用不可达 Hermes 地址启动临时后端，验证 health 降级和 chat fail closed。
- 当前裁决：
  - 批次 5A.1 开发侧完成，可交给测试 agent 专项复验。
  - 测试通过前不正式收口 5A.1。

## 2026-05-18：二期批次 5A.1 测试复验通过并正式收口

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 测试 agent 结论：
  - 通过。
  - 当前无 `P0 / P1`。
  - 仅有既有 `Vite chunk size warning` P2，以及本轮明确不在范围内的 `/api/data-steward/catalog/search` 未实现说明，不影响收口。
- 核心验收结论：
  - `GET /api/data-steward/hermes/health` 可用，不泄露 baseUrl、secret、数据库连接串或真实 NAS 路径。
  - `POST /api/data-steward/chat` 可用，行为与既有只读贾维斯边界一致。
  - 前端 `问贾维斯` 面板已展示健康状态、只读网关模式、运行时写入未开放、正式正文回答未开放。
  - Hermes 不可用时，health 与 chat 都安全降级，不返回 500。
  - 平台侧审计可关联 `pageType / requestId / projectRef / sourceView / evidenceMode / permissionStatus`。
  - 4R 文件访问安全闭环未回归。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过，实际验收端口为 `8080`。
  - `scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=9 FAIL=0`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - 局部 `git diff --check` 通过。
- 主 agent 裁决：
  - 二期批次 5A.1 正式收口。
  - 下一步进入 `批次 5B：数据管家客户版模块补齐`，优先补齐模型集成、管理对象、事项列表、任务列表、导出列表和文件服务。

## 2026-05-18：二期批次 5B 规划与开发/测试交接完成

- 用户确认继续下一步。
- 主 agent 判断：
  - 真实 NAS 增删改查仍不适合开放，应放到二期后段或三期，以受控写操作形式逐步开放。
  - 当前下一步应执行 `批次 5B：数据管家客户版模块补齐`。
- 已梳理当前基础：
  - 模型集成和管理对象已有早期 MVP 页面与后端 API。
  - 资产驾驶舱和文件管理已是当前数据管家前端基线。
  - 任务、扫描、事件、质量、文件访问、贾维斯只读能力可复用。
  - 事项列表、任务列表、导出列表、文件服务尚未形成客户可见模块。
- 已新增规划文件：
  - `handoff/main-agent/phase2-batch5b-data-steward-modules-plan.md`
- 已更新开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH5B_DATA_STEWARD_MODULES_COMPLETE</promise>`
  - 要求使用 Ralph Loop，不创建子 agent，不修改 `docs/**`。
- 已更新测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 5B 明确范围：
  - 资产驾驶舱、文件管理、模型集成、管理对象、事项列表、任务列表、导出列表、文件服务。
- 5B 明确禁止：
  - 真实 NAS 写入、移动、重命名、删除、新建文件夹。
  - 模型轻量化、构件级解析、正文抽取。
  - Agent 自动治理、自动审批、自动修复。
  - 写 OpenSearch/Qdrant/MinIO。
- 当前裁决：
  - 5B 已具备派发给开发 agent 的条件。
  - 已尝试启动项目目录下 Claude Code 并派发 `handoff/dev-agent/current-prompt.md`。
  - Claude Code 当前仍返回 `API Error: 402 Insufficient Balance`，未能实际进入开发。
  - 因 5B 属于较大前端/接口整合批次，实际代码开发尚未开始；后续可在 Claude Code 余额恢复后继续派发，或由主 agent 直接接手开发。

## 2026-05-18：二期批次 5B 主 agent 直接实现完成开发侧验证

- 执行说明：
  - 用户确认先由主 agent 开发实现 5B。
  - Claude Code 仍因 `402 Insufficient Balance` 不可用，本轮由主 agent 直接实现。
  - 未创建子 agent，未修改 `docs/**`。
- 本轮改动：
  - 新增项目内数据管家路由：模型集成、管理对象、事项列表、任务列表、导出列表、文件服务。
  - 项目工作台导航新增 `数据管家` 分组。
  - 项目资产详情页新增 8 个数据管家模块入口卡片。
  - `ModelIntegrationsPage.vue` 客户化，模型文件选择改为分页检索，页面明确不做真实轻量化。
  - `ManagedObjectsPage.vue` 客户化，展示关联模型和部位，支持创建、编辑和停用平台对象记录。
  - 新增 `DataStewardIssuesPage.vue`，聚合质量缺口、失败扫描和待审核事项。
  - 新增 `DataStewardTasksPage.vue`，聚合扫描任务和后台任务，支持失败后台任务重试。
  - 新增 `DataStewardExportsPage.vue`，提供不含真实 NAS 路径的文件清单 CSV 导出。
  - 新增 `DataStewardFileServicePage.vue`，展示已开放文件服务和禁用的 NAS 写操作。
  - 新增专项脚本 `scripts/dev/check-phase2-batch5b-data-steward-modules.sh`。
- 边界确认：
  - 未开放真实 NAS 写入、移动、重命名、删除或新建文件夹。
  - 未做模型轻量化、构件级解析、正文抽取、Agent 自动治理或索引写入。
  - 未新增数据库迁移。
- 开发侧验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过，仅有既有 Vite chunk size warning。
  - 健康检查 `http://localhost:8080/actuator/health` 返回 `UP`。
  - `bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh`：`PASS=15 FAIL=0`。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - `bash scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=9 FAIL=0`。
  - 局部 `git diff --check` 通过。
- 当前裁决：
  - 5B 开发侧完成，可交给测试 agent 按 `handoff/test-agent/current-prompt.md` 做页面专项验收。
  - 测试通过前不正式收口 5B。

## 2026-05-18：二期批次 5B 测试 P0 返修完成

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 测试 agent 判定：
  - 5B 暂不通过。
  - P0：导出列表页面和 `catalog/files` 接口的 `logicalPath` 泄露真实 NAS 绝对路径 `/Volumes/...`。
  - P2：任务列表失败原因直接展示真实绝对路径。
- 本轮修复：
  - 后端 `CatalogApplicationService` 将 `catalog/files` 与文件详情返回的 `logicalPath` 统一转为项目内路径。
  - 转换策略：优先按项目路径映射剥离 NAS 根路径；再按项目编码/名称定位项目目录；仍无法确认时只返回文件名。
  - 真实路径继续仅在受权限控制的 `storagePath` 详情字段中返回，不再进入导出来源。
  - 前端 `DataStewardExportsPage.vue` 将导出字段改为 `项目内路径`，并增加 `/Volumes/`、`nas://`、`//` 兜底脱敏。
  - 前端 `DataStewardTasksPage.vue` 将任务进度说明和失败原因里的真实路径替换为 `[受控存储路径]`。
  - `scripts/dev/check-phase2-batch5b-data-steward-modules.sh` 增加真实 NAS 项目样本断言。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 健康检查 `http://localhost:8080/actuator/health` 返回 `UP`。
  - `bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh`：`PASS=16 FAIL=0`，新增真实 NAS 脱敏断言通过。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - `bash scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=9 FAIL=0`。
  - 直接抽查 `projectId=506` 的 `catalog/files`，返回 `logicalPath` 不再包含 `/Volumes/` 且不再以 `/` 开头。
- 当前裁决：
  - 5B P0 已完成开发侧返修。
  - 建议测试 agent 对导出列表、`catalog/files`、任务列表失败原因做短回归。
  - 短回归通过前仍不正式收口 5B。

## 2026-05-18：二期批次 5B 短回归通过并正式收口

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 测试 agent 结论：
  - 通过。
  - 当前未发现新的 `P0 / P1 / P2`。
  - 建议收口 5B。
- 关键验证：
  - 后端健康检查 `http://localhost:8080/actuator/health` 返回 `UP`。
  - `bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh`：`PASS=16 FAIL=0`。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`。
  - 抽查 `GET /api/data-steward/catalog/files?projectId=506&pageNo=1&pageSize=5`，`logicalPath` 已为项目内相对路径，不含 `/Volumes/`，不以 `/` 开头。
  - 导出列表 `/data-steward/assets/506/data-steward/exports` 已展示 `项目内路径`，页面可见样本不再出现 `/Volumes/`。
  - 任务列表失败原因已脱敏为 `[受控存储路径]`。
- 主 agent 裁决：
  - 二期批次 5B 正式收口。
  - 当前二期已完成：4R 文件访问安全闭环、5A 贾维斯内嵌、5A.1 贾维斯 Gateway 合同对齐、5B 数据管家客户版模块补齐。
  - 下一步进入 `批次 6：客户项目初始化与标准模板化`。

## 2026-05-18：二期批次 6A 规划与开发/测试交接完成

- 用户确认进入下一步。
- 主 agent 将批次 6 拆为 `6A：项目初始化评估 + 建筑机电/BIM 交付基础模板预览/套用`。
- 拆分原因：
  - 当前工程主数据和交付标准底座已具备 CRUD 与状态接口。
  - 客户缺的是“从空项目到标准就绪”的向导式流程。
  - 不适合一次性混入模板市场、文件转换、BIM 轻量化、构件级能力或真实 NAS 写操作。
- 已新增规划文件：
  - `handoff/main-agent/phase2-batch6a-project-initialization-plan.md`
- 已更新开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH6A_PROJECT_INITIALIZATION_TEMPLATE_COMPLETE</promise>`
  - 要求使用 Ralph Loop，不创建子 agent，不修改 `docs/**`。
- 已更新测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 6A 范围：
  - 初始化状态接口。
  - 内置 `MEP_BIM_BASIC` 建筑机电/BIM交付基础模板。
  - 模板列表、详情、预览、套用接口。
  - 幂等套用，不覆盖、不删除已有客户数据。
  - 套用审计。
  - 项目工作台内 `初始化向导` 页面。
  - 专项脚本 `scripts/dev/check-phase2-batch6a-project-initialization.sh`。
- 6A 禁止：
  - 真实 NAS 上传、新建文件夹、移动、重命名、删除。
  - Office/CAD/BIM 文件转换。
  - BIM 轻量化。
  - 构件级解析、构件级搜索。
  - 正文抽取、向量库、selective indexing。
  - Agent 自动初始化、自动审批、自动写库。
  - 多行业模板市场。
- 当前裁决：
  - 6A 已具备派发给开发 agent 的条件。
  - 开发完成后必须先跑专项脚本和主数据/交付标准/5B 回归，再交给测试 agent。

## 2026-05-18：Catalog-only / Hermes 架构建议评审

- 已阅读外部评审输入 `/Users/vc/Downloads/catalog_only_architecture_pr_review_prompt.md`。
- 本轮定位为架构建议评审，不作为开发指令执行；未修改底表、SQL View、Flyway 迁移、API 或前端实现。
- 已核对当前稳定读模型和 Jarvis/Hermes 网关：
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 已具备 v1.1 catalog-only 治理字段。
  - `AgentAssetContextResolver` 只允许四个稳定 View 作为 Agent 上下文来源。
  - `AgentGatewayApplicationService` 已约束 catalog-only、Missing Evidence、路径脱敏、只读、禁止执行动作。
  - `AgentPermissionProofService` 仅允许 `catalog_query`、`agent_catalog_assist`、`operation_plan_draft`。
- 评审结论已写入 `handoff/main-agent/catalog-only-architecture-suggestion-review.md`：
  - 当前架构健康等级：Green。
  - 不需要刹停。
  - 不需要底层重构。
  - 建议将 `file_id` 主关联键、路径脱敏、mtime 区分、checksum 状态、能力标记、query feedback 和未来 semantic index 字段进入 backlog。
- 已更新 `handoff/main-agent/backlog.md`，新增 `Catalog-only / Jarvis 契约补强项`。
- 当前主线不变：继续推进 `批次 6A：项目初始化评估 + 建筑机电/BIM交付基础模板预览/套用`。

## 2026-05-18：Hermes V3 平台接入补强完成

- 用户要求先根据 `/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md` 进行 Hermes 接入平台开发。
- 本轮保持 V3 边界：
  - 允许：前端入口、平台后端 Gateway、Hermes health、只读 chat/catalog lookup、权限上下文、审计 trace。
  - 禁止：Agent DB CRUD、NAS CRUD、正文抽取、写 `documents/chunks`、写 OpenSearch/Qdrant/MinIO、Agent 自动审批、production rollout。
- 后端完成：
  - 新增 `POST /api/data-steward/catalog/search`，返回权限过滤后的只读资产目录 metadata preview。
  - `/api/data-steward/chat` 兼容 V3 请求格式：`session_id / message / project_filters / mode`，同时保留旧页面请求格式。
  - `project_filters` 可按当前用户权限解析项目 ID 或项目编码，解析不到则 fail closed。
  - 新增 `agent.jarvis.catalog.search` 审计，只记录安全摘要，不记录真实路径、raw row 或 secret。
- 前端完成：
  - `DataStewardPanel.vue` 在问贾维斯时同步展示“资产目录预览”。
  - 预览明确标注仅为目录元数据，不是文件正文证据。
- 验证通过：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package`。
  - 前端构建 `corepack pnpm --dir frontend build`。
  - 后端健康检查 `UP`。
  - `bash scripts/dev/check-hermes-jarvis-gateway.sh`：`PASS=11 FAIL=0`。
- 当前本地后端已通过 `screen` 会话 `delivery-backend` 以新版 jar 运行，供前端和测试 agent 继续验证。
## 2026-05-18：数据库 / 平台 Agent 风险提醒评审

- 已阅读外部风险提醒 `/Users/vc/Downloads/database_platform_agent_risk_notes.md`。
- 评审结论：值得采纳，作为后续 Hermes / 贾维斯、catalog-only、selective indexing 和客户演示文案的安全边界；当前不需要修改底表、Flyway migration、SQL View 或核心 API。
- 已形成评审文件：`handoff/main-agent/database-platform-agent-risk-notes-review.md`。
- 风险优先级裁决：
  - P0 guardrail：不得把 catalog metadata 当文件正文证据；不得默认暴露真实 NAS 路径。
  - P1：不得混淆 `updated_at` 与 NAS 文件 mtime；不得把 `process_status/component_index_status` 当 semantic index；不得把 `checksum=null` 解释为文件无变化或确定异常。
  - Phase gate：未来 selective indexing、全文索引、语义索引、BIM 构件索引必须单独设计，不能混入 Hermes long-term memory。
- 已补充 backlog：前端和客户材料不得过度承诺 DWG/RVT 内容理解能力。
## 2026-05-18：Hermes 只读前端网关脱敏接入评审

- 已按 Hermes Phase 2.89 / V3 对接文档完成接口设计与边界评审。
- 已确认当前允许范围：平台后端 Gateway、前端内嵌 Hermes、health check、只读 catalog metadata preview、project_scope、permission proof、审计 trace、Missing Evidence。
- 已确认当前禁止范围：Agent DB CRUD、生成 SQL、扫描 NAS、读取 DWG/RVT 正文、返回真实 `storage_path`、写 `documents/chunks/OpenSearch/Qdrant/MinIO`、前端直连 Hermes。
- 已形成评审文件：`handoff/main-agent/hermes-readonly-frontend-gateway-access-review.md`。
- 当前 Go / Pause / No-Go 裁决：
  - Go：只读 Gateway 与前端嵌入继续。
  - Pause：selective indexing、正文抽取、evidence write、query feedback。
  - No-Go：任何 Agent 写库、写 NAS、生成 SQL、真实路径泄露。
- 当前进入下一轮代码前的 P1：用户可见命名仍残留 `贾维斯`，必须统一改为 `Hermes`。
## 2026-05-18：Hermes 正式命名收束

- 测试 agent 已完成 Hermes V3 短回归，结论通过，无 P0/P1，仅保留既有 Vite chunk size warning P2。
- 主 agent 复核发现：测试报告仍出现 `贾维斯` 用户侧命名，与用户新裁决“企业 Agent 正式名称统一为 Hermes”冲突。
- 已完成极小命名收束：
  - 后端 `capabilities.agentName` 改为 `Hermes`。
  - 前端入口、按钮、抽屉标题和提示文案统一为 `Hermes / Hermes 数据管家 / 问 Hermes`。
  - 平台审计 action 从 `agent.jarvis.*` 收束为 `agent.hermes.*`。
  - 目录预览审计 action 改为 `agent.hermes.catalog.search`。
  - 内部 Controller 类名收束为 `HermesDataStewardGatewayController`。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-hermes-jarvis-gateway.sh` 通过，`PASS=11 FAIL=0`。
  - `git diff --check` 通过。
- 当前裁决：Hermes V3 只读 Gateway 接入可以收口；下一步可以继续二期主线，但仍不得进入 Agent DB CRUD、SQL 生成、NAS 扫描、DWG/RVT 正文读取、真实路径返回或索引写入。
## 2026-05-18：Hermes 命名收束 + 只读 Gateway 前端复验完成

- 已按用户补充要求完成极小批次。
- 用户可见文案已从 `贾维斯` 收束为 `Hermes / Hermes 数据管家`；代码侧 `frontend/` 与 `backend/delivery-data-steward/` 已无 `贾维斯/Jarvis/jarvis` 残留。
- 前端能力查询改为 `/api/data-steward/hermes/capabilities`，不再使用 `/api/agent/hermes/capabilities`。
- `/api/agent/hermes/*` 已标记为内部/兼容接口；当前只保留脚本兼容验证，不新增前端使用点。
- 前端 catalog 预览不再发送可信 `project_scope`，只发送普通 `projectFilters`；后端把 `project_filters/projectFilters` 当作不可信筛选条件，并由平台后端 Gateway 生成可信 project scope。
- `operationPlan` 收束为 `manual_review_required / draft_only`，专项脚本断言 action 不包含 `ingestion / writer / index`。
- forbidden field 断言已补强：响应不得包含 `storage_path/storage_uri/storagePath/storageUri`、`/Volumes`、`nas://`、`smb://`、raw row、SQL 片段、token、secret、bearer。
- Missing Evidence 断言已补强：正文类问题返回 `missing_evidence`，catalog-only 不伪装成正文 evidence。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-hermes-jarvis-gateway.sh` 通过，`PASS=11 FAIL=0`。
  - `git diff --check` 通过。

## 2026-05-18：二期 6A 项目初始化与标准模板化完成开发自测

- 已恢复二期主线，进入 `批次 6A：项目初始化评估 + 建筑机电/BIM交付基础模板预览/套用`。
- 本轮只做客户项目开局标准底座，不做 NAS 写操作、模型轻量化、正文抽取、向量库、Hermes 自动写操作或多行业模板市场。
- 后端新增初始化接口：
  - `GET /api/master-data/projects/{projectId}/initialization/status`
  - `GET /api/master-data/standard-templates`
  - `GET /api/master-data/standard-templates/{templateCode}`
  - `POST /api/master-data/projects/{projectId}/initialization:preview-template`
  - `POST /api/master-data/projects/{projectId}/initialization:apply-template`
- 内置模板：
  - `MEP_BIM_BASIC / 建筑机电 BIM 交付基础模板`
  - 覆盖部位树、节点类型、交付物定义、交付物类型、交付物属性和目录模板。
- 安全与验收边界：
  - 模板应用幂等，不覆盖、不删除、不改名已有项目数据。
  - 应用前必须确认。
  - 已锁定节点类型且仍需新增节点类型时会阻塞。
  - 应用成功写审计 `masterdata.initialization.template-apply`。
  - 未新增 Flyway migration，未修改旧 migration。
- 前端新增：
  - `/data-steward/assets/:projectId/master-data/initialization`
  - 项目工作台 `工程主数据` 分组新增 `初始化向导`。
  - 初始化页面展示标准状态、模板详情、预览影响、应用结果和后续入口。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
  - `scripts/dev/check-hermes-jarvis-gateway.sh` 回归通过，`PASS=11 FAIL=0`。
  - `git diff --check` 通过。
- 当前裁决：进入测试 agent 专项验收。验收通过后再规划 6B/7，不提前进入文件转换、BIM 轻量化或 Agent 写操作。

## 2026-05-18：二期 6A 测试 P0 导航断裂修复

- 测试 agent 报告结论：6A 不通过，唯一 P0 是从 `/data-steward/assets` 进入项目详情后，项目详情页缺少 `工程主数据 -> 初始化向导` 主链路入口。
- 根因：项目详情页 `data-steward-asset-detail` 使用 `assetProjectContext=true`，`AppLayout` 会隐藏全局 `ProjectWorkspaceNav`；而 `AssetProjectDetailPage.vue` 自身没有补项目工作台顶部导航。
- 修复：
  - 在 `AssetProjectDetailPage.vue` 顶部加入 `ProjectWorkspaceNav`。
  - 保持项目详情页原有数据管家驾驶舱、文件管理、扫描任务、路径映射等功能不变。
  - 后端无改动。
- 验证：
  - `corepack pnpm --dir frontend build` 通过，仅既有 Vite chunk size warning。
- 当前裁决：请测试 agent 做极短回归，只看项目详情页是否能从 `工程主数据 -> 初始化向导` 进入当前项目初始化页，以及页面是否无横向溢出。

## 2026-05-18：二期 6A 正式收口

- 测试 agent 已完成 P0 导航断裂极短复验。
- 复验结果：
  - `/data-steward/assets` 可正常打开。
  - 从项目列表进入 `/data-steward/assets/503` 成功。
  - 项目详情页顶部已显示统一项目工作台导航。
  - `工程主数据 -> 初始化向导` 入口存在。
  - 点击后进入 `/data-steward/assets/503/master-data/initialization`。
  - 项目上下文保持为 `503`。
  - 项目详情页和初始化页页面级横向溢出均为 `0`。
- 上轮唯一 P0 已关闭，当前未发现新的 P0。
- 收口报告已写入：`handoff/main-agent/phase2-batch6a-project-initialization-closure.md`。
- 当前裁决：二期 6A 正式收口。下一步建议进入 `6B：文件预览与批量交付增强` 规划，仍不提前进入 BIM 轻量化、构件级解析、正文抽取、Hermes 写操作或真实 NAS 写操作。

## 2026-05-18：文件管理页目录树超时修复完成并通过主 Agent 审计

- 用户问题：项目详情进入 `文件管理` 后目录结构请求超时，且目录树与右侧文件表对应关系不稳定。
- 本轮按用户确认使用 Claude Code 开发 agent 修复，开发 agent 已调用 Ralph Loop，并写回 `handoff/dev-agent/latest-report.md`。
- 修复范围保持在本轮边界内：未修改 `docs/**`，未新增真实 NAS 写操作，未修改全局前端超时配置。
- 后端修复结论：`catalog/directories` 默认不再同步递归扫描真实 NAS，改为只基于数据库已登记文件资产推导目录；目录路径统一转为项目内相对路径。
- 目录/文件口径修复：`catalog/files` 的 `directoryPath` 过滤已能接受目录树返回的相对路径，并映射回数据库中的原始 `logical_path` 做匹配，支持当前目录及子目录文件。
- 前端兜底：目录树加载失败时显示中文业务提示与重试入口，右侧文件表不再被目录失败拖死。
- 主 agent 独立复核结果：
  - `projectId=506` 的 `catalog/directories` 返回 585 个目录，耗时约 `0.16s`。
  - 目录响应中未检出 `/Volumes`、`smb://`、`nas://`。
  - 选取目录树返回路径再查 `catalog/files`，返回 `45` 个匹配文件，分页首屏 `20` 条，说明左树右表口径已打通。
  - `catalog/files?projectId=506&page=1&pageSize=50` 返回总数 `5912`，耗时约 `0.04s`。
  - 后端 Maven 构建通过，8 个模块 `SUCCESS`。
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `git diff --check` 通过。
- 当前裁决：开发修复通过主 agent 审计。建议测试 agent 做极短浏览器专项复验，只看项目 506 文件管理页是否不再 timeout、目录树是否加载、点击目录后右侧文件是否对应、文件表更多菜单/预览/详情/治理/补 checksum 是否不回归。

## 2026-05-18：二期 6B 批量挂接交付与交付包准备视图启动

- 用户确认回到主线开发，当前进入 `6B：批量挂接交付 + 交付包准备视图 + 最小预览状态增强`。
- 主 agent 已完成现状判断：
  - 当前交付绑定后端为单文件接口 `POST /api/work-center/projects/{projectId}/delivery-bindings`。
  - 当前 `DeliveryViewPanel.vue` 的“选择文件补交”为单选远程搜索。
  - 当前已具备完整率、交付视图、审核记录、文件预览和受控访问票据接口，可复用为本批基础。
- 本批边界：
  - 做批量挂接接口、前端多选补交、只读交付包准备视图。
  - 不做 BIM 轻量化、构件级解析、正文抽取、Hermes 写操作、真实 NAS 写操作、压缩包生成或批量下载。
  - 新增接口不得返回真实 NAS 路径。
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH6B_BATCH_DELIVERY_PACKAGE_COMPLETE</promise>`
  - 要求 Claude Code 使用 Ralph Loop，不创建子 agent，不修改 `docs/**`。
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
  - 重点验收批量挂接幂等、跨项目/类型权限、交付包准备视图、禁止路径字段、前端多选补交和审核回归。
- 当前裁决：启动 Claude Code 开发 agent 执行 6B，并由主 agent 监控偏航。

## 2026-05-18：二期 6B 开发完成并通过主 Agent 审计

- 开发方式：使用项目目录下 Claude Code 开发 agent 执行，已要求调用 Ralph Loop；后续因 Claude CLI 中断后输入未正常提交，主 agent 接手完成脚本修补、编译修复和验收报告写回。
- 后端完成：
  - 新增 `POST /api/work-center/projects/{projectId}/delivery-bindings:batch`，支持文档/图纸批量挂接、逐行结果、幂等跳过、失败计数和审计。
  - 新增 `GET /api/work-center/projects/{projectId}/delivery-package/summary`，输出文档/图纸交付包准备汇总和缺失/已挂接行。
  - 主 agent 审计时补强 `targetType` 口径，交付包汇总按 `SECTION` / `OBJECT` 过滤，不把对象/部位交付混算。
- 前端完成：
  - `DeliveryViewPanel.vue` 的“去挂接”弹窗由单文件选择升级为远程分页多选。
  - 保留文件 ID、文件名、版本、处理状态、轻量预览状态和已有审核/整改操作。
  - 新增交付包准备汇总区域，展示文档/图纸应交、已挂接、缺失、待审、已通过、已驳回和可进入交付包数量。
- 验收脚本：
  - 新增 `scripts/dev/check-phase2-batch6b-delivery-package.sh`。
  - 脚本自包含生成 `mock://batch6b/{runId}/...` 元数据测试文件，不依赖真实 NAS，不写真实文件。
  - 覆盖批量挂接、重复幂等、错误文件类型拒绝、跨项目文件拒绝、交付包汇总、敏感路径禁止、OpenAPI 和审计事件。
- 主 agent 验证结果：
  - `bash scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `cd backend && ./mvnw -pl delivery-app -am -DskipTests package` 通过，8 个模块 `SUCCESS`。
  - `corepack pnpm --dir frontend build` 通过，仅既有 Vite chunk size warning。
  - `curl -fsS http://127.0.0.1:8080/actuator/health` 返回 `UP`。
  - `git diff --check` 通过。
  - 已用最新构建包重启本地后端，并再次执行 6B 脚本，仍为 `PASS=17 FAIL=0`。
- 开发报告已写回：
  - `handoff/dev-agent/latest-report.md`
- 当前裁决：6B 开发通过主 agent 审计。建议测试 agent 按 `handoff/test-agent/current-prompt.md` 做专项短回归，重点看文档/图纸交付页批量选择、结果摘要、交付包准备视图、权限/类型/路径泄漏断言和横向溢出。

## 2026-05-19：二期 6B 正式收口

- 测试 agent 已完成 6B 专项短回归，报告写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前没有新的 `P0 / P1`。
- 保留既有非阻塞 `P2`：前端生产构建仍有 Vite chunk size warning。
- 验收通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过。
  - `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
  - `git diff --check` 通过。
- 页面短回归通过：
  - `/data-steward/assets/1/work/document-delivery`
  - `/data-steward/assets/1/work/drawing-delivery`
- 已确认能力：
  - 文档交付页可批量选择文件补交。
  - 文件选择器仍按分页远程加载。
  - 交付包准备视图可展开并显示汇总与明细。
  - 批量挂接幂等、跨项目拦截、类型校验未回归。
  - 新增响应未发现真实 NAS 路径或存储字段泄露。
- 收口报告已写入：
  - `handoff/main-agent/phase2-batch6b-batch-delivery-package-closure.md`
- 当前裁决：二期 6B 正式收口。下一步建议进入 `7A：文件预览策略与交付包导出前置能力` 规划，继续禁止真实 NAS 写操作、模型轻量化、正文抽取和 Hermes 写操作，除非进入后续明确批次。

## 2026-05-19：二期 7A 文件预览策略与交付包导出预检查启动

- 用户确认进入下一步，当前启动 `7A：文件预览策略与交付包导出前置能力`。
- 主 agent 已完成现状判断：
  - 数据管家已有 `GET /api/data-steward/assets/files/{fileId}/preview` 和访问票据能力。
  - 6B 已有交付包准备视图和批量挂接。
  - 当前预览状态判断散落在数据管家后端和工作中心前端，需要统一口径。
- 本批边界：
  - 做共享预览策略。
  - 做只读交付包导出预检查接口。
  - 做前端导出预检查面板。
  - 不生成真实 ZIP，不批量下载，不读取文件正文，不做 BIM 轻量化，不写 NAS，不做 Hermes 写操作。
- 已写入规划：
  - `handoff/main-agent/phase2-batch7a-preview-export-precheck-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH7A_PREVIEW_EXPORT_PRECHECK_COMPLETE</promise>`
  - 要求 Claude Code 使用 Ralph Loop，不创建子 agent，不修改 `docs/**`。
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：启动 Claude Code 开发 agent 执行 7A，并由主 agent 监控偏航。

## 2026-05-19：二期 7A 正式收口

- 测试 agent 已完成 7A 专项验收，报告写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前未发现新的 `P0 / P1`。
- 保留既有非阻塞 `P2`：前端生产构建仍有 Vite chunk size warning。
- 验收通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
  - `git diff --check` 通过。
- 已确认能力：
  - `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck` 已纳入 OpenAPI。
  - DOCUMENT / DRAWING / ALL 三种口径均可查。
  - 预检查响应明确 `dryRun=true`、`packageGenerated=false`。
  - 页面能展示导出预检查统计卡片和明细表。
  - 文档/图纸交付页仍保留项目工作台导航、批量补交、审核、驳回、记录和分页远程文件选择能力。
  - 响应未发现真实 NAS 路径或底层存储字段泄露。
- 收口报告已写入：
  - `handoff/main-agent/phase2-batch7a-preview-export-precheck-closure.md`
- 当前裁决：二期 7A 正式收口。下一步建议进入 `7B：文件预览转换增强` 规划，继续禁止真实 NAS 写操作、真实交付包生成、批量下载、正文抽取、BIM 轻量化和 Hermes 写操作，除非进入后续明确批次。

## 2026-05-19：二期 7B 文件预览转换体验增强启动

- 用户确认继续下一步，当前启动 `7B：文件预览转换体验增强`。
- 主 agent 已完成边界收束：
  - 本批做预览状态、转换提示、不可预览交互和交付包预检查表达增强。
  - 本批不做真实 Office/CAD/BIM 转换，不做 BIM 轻量化，不生成真实预览缓存，不读文件正文，不写 NAS，不做真实交付包或批量下载。
- 已写入规划：
  - `handoff/main-agent/phase2-batch7b-preview-conversion-experience-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH7B_PREVIEW_CONVERSION_EXPERIENCE_COMPLETE</promise>`
  - 当前开发方式为独立开发 Codex 会话主导开发，禁止创建子 agent，禁止调用 Claude Code，禁止修改 `docs/**`。
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动开发 Codex 会话执行 7B。主 agent 后续只做监控、审计和收口判断。

## 2026-05-19：二期 7B 正式收口

- 开发 agent 已完成 7B 开发并写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 已完成 7B 专项验收并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前未发现新的 `P0 / P1`。
- 保留既有非阻塞 `P2`：前端生产构建仍有 Vite chunk size warning。
- 验收通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh` 通过，`PASS=20 FAIL=0`。
  - `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
  - `git diff --check` 通过。
- 已确认能力：
  - PDF / 图片显示可在线预览。
  - Office / CAD / BIM 显示需要对应转换或轻量化服务。
  - 压缩包显示仅下载原文件。
  - 未知格式显示暂不支持在线预览。
  - 不可直接在线预览文件不会被误创建 `PREVIEW` 访问票据。
  - 文档/图纸交付页继续区分预览状态与导出状态。
  - 响应未发现真实 NAS 路径或底层存储字段泄露。
- 收口报告已写入：
  - `handoff/main-agent/phase2-batch7b-preview-conversion-experience-closure.md`
- 当前裁决：二期 7B 正式收口。下一步建议进入 `8A：BIM 轻量化适配层规划`，先做适配层、状态契约、Mock/占位能力和页面入口，不直接绑定具体厂商或执行真实轻量化转换，除非用户另行确认。

## 2026-05-19：二期 8A BIM 轻量化适配层与 Mock 预览入口启动

- 用户确认继续下一步，当前启动 `8A：BIM 轻量化适配层与 Mock 预览入口`。
- 主 agent 已完成边界收束：
  - 本批做可插拔 BIM 轻量化适配层合同、Mock 状态、轻量化准备计划和模型集成页入口。
  - 本批不接入真实 BIM 引擎，不绑定厂商 SDK，不执行真实模型转换，不生成轻量化产物，不做构件级解析，不读取模型正文，不写 NAS。
- 已写入规划：
  - `handoff/main-agent/phase2-batch8a-bim-lightweight-adapter-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_BATCH8A_BIM_LIGHTWEIGHT_ADAPTER_COMPLETE</promise>`
  - 当前开发方式为独立开发 Codex 会话主导开发，禁止创建子 agent，禁止调用 Claude Code，禁止修改 `docs/**`。
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动开发 Codex 会话执行 8A。主 agent 后续只做监控、审计和收口判断。

## 2026-05-19：二期 8A 正式收口并暂停后续批次

- 开发 agent 已完成 8A 开发并写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 已完成 8A 专项验收并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前未发现新的 `P0 / P1`。
- 保留既有非阻塞 `P2`：前端生产构建仍有 Vite chunk size warning。
- 验收通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh` 通过，`PASS=11 FAIL=0`。
  - `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh` 通过，`PASS=20 FAIL=0`。
  - `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
  - `git diff --check` 通过。
- 已确认能力：
  - 新增 BIM 轻量化 Mock 状态接口和 dry-run 准备计划接口。
  - 模型集成页展示轻量化状态、适配模式、准备检查和 3D 预览入口。
  - 3D 入口只提示 Mock 状态，不跳转伪造三维页面。
  - 未执行真实转换，未触碰 NAS，未读取模型正文，未写 Hermes memory，未泄露真实路径。
- 收口报告已写入：
  - `handoff/main-agent/phase2-batch8a-bim-lightweight-adapter-closure.md`
- 用户最新裁决：8A 收口后暂时暂停，不进入 8B。
- 当前裁决：不启动新的开发批次；后续仅处理用户明确提出的 P0/P1 回归、局部修复或规划核对。

## 2026-05-19：二期插入批次 G1 Agent 引导式交付治理 MVP 启动

- 用户明确要求：8A 收口后暂不进入 8B，也不立即开放真实 NAS 增删改查。
- 当前临时插入高优先级模块：`Agent 引导式交付治理 MVP`。
- 主 agent 判断：该模块用于降低真实 NAS 项目进入数字化交付闭环的门槛，优先级高于真实 BIM 轻量化和 NAS 增删改查。
- 本批核心链路：`项目交付体检 -> 缺失项解释 -> 候选文件推荐 -> 人工确认 -> 批量挂接 -> 审计留痕 -> 完整率刷新`。
- 本批只做：
  - 只读交付体检。
  - 文档/图纸缺失项解释。
  - 基于元数据的候选文件推荐。
  - 推荐挂接方案表格。
  - 人工确认后复用既有批量挂接能力。
  - Hermes / Agent 只读辅助安全降级。
- 本批严禁：
  - Agent 自动写库、自动挂接、自动审核、自动整改。
  - Agent 自动修改部位树、节点类型、交付物标准。
  - Agent 移动、删除、重命名、上传 NAS 文件。
  - 读取 PDF / Office / DWG / RVT / IFC 正文。
  - 写向量库、长期 memory、OpenSearch、Qdrant、MinIO documents/chunks。
  - 返回真实 NAS 绝对路径给普通用户。
  - 破坏 8A BIM Mock 预览入口。
- 已写入规划：
  - `handoff/main-agent/phase2-insert-g1-agent-delivery-governance-mvp-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_INSERT_G1_AGENT_DELIVERY_GOVERNANCE_MVP_COMPLETE</promise>`
  - 当前开发方式为独立开发 Codex 会话主导开发，禁止创建子 agent，禁止调用 Claude Code，禁止修改 `docs/**`。
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动开发 Codex 会话执行 G1。主 agent 后续只做监控、审计和收口判断。

## 2026-05-19：二期插入批次 G1 正式收口

- 开发 agent 已完成 G1 开发并写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 已完成 G1 专项验收并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前未发现 `P0 / P1`。
- 保留非阻塞 `P2`：
  - 前端生产构建仍有既有 Vite chunk size warning。
  - G1 专项脚本和页面验收会留下 `PHASE2-G1-*` 元数据与测试挂接，连续复跑会影响样板项目缺失项、完整率和交付包预检查数字；当前不触碰真实 NAS，后续可补脚本自清理或隔离测试项目。
- 验收通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `git diff --check` 通过。
  - `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh` 通过，`PASS=34 FAIL=0`。
  - `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh` 通过，`PASS=11 FAIL=0`。
  - `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
  - `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh` 通过，`通过=52 失败=0`。
- 已确认能力：
  - 新增 4 个 G1 接口：overview、missing-items、recommend-bindings、recommendations:apply。
  - 项目工作台已有 `交付治理助手` 入口，项目详情页已有 `开始交付治理` 入口。
  - 页面可展示项目体检、Agent 总结、工程主数据状态、文档/图纸完整率、缺失项解释和推荐挂接方案。
  - 未选择推荐、未勾选人工确认时不能挂接。
  - 勾选人工确认后可调用既有批量挂接能力并显示创建、跳过、失败结果。
  - 审计包含 `work.agent-governance.recommend` 和 `work.agent-governance.apply`。
  - 未发现真实 NAS 路径泄露、真实文件访问、Hermes 写操作或 Agent 自动治理。
- 收口报告已写入：
  - `handoff/main-agent/phase2-insert-g1-agent-delivery-governance-mvp-closure.md`
- 当前裁决：G1 正式收口，当前不自动进入下一批次。后续候选方向由用户确认。

## 2026-05-19：G1-P2 测试数据自清理小修复启动

- 用户确认下一步先处理 G1 非阻塞 P2：专项脚本和页面验收留下 `PHASE2-G1-*` 元数据与测试挂接。
- 主 agent 已确认污染点：`scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh` 会在当前项目创建测试部位、节点类型、交付物定义/类型/属性、目录模板、mock 文件元数据和测试挂接，当前无自清理。
- 本轮目标：
  - G1 脚本仍通过。
  - G1 脚本连续复跑仍通过。
  - 本轮创建的测试挂接和 mock 文件元数据可清理。
  - 可清理的主数据资源尽量清理。
  - 支持 `G1_TEST_PROJECT_ID` 作为隔离项目。
  - 脚本失败时通过 `trap cleanup EXIT` 尽量清理。
- 本轮严禁：
  - 修改 G1 产品功能。
  - 修改 `docs/**`。
  - 新增危险生产删除能力。
  - 硬删除真实业务数据。
  - 触碰真实 NAS 文件。
  - 进入真实 BIM 轻量化或 NAS 增删改查。
- 已写入规划：
  - `handoff/main-agent/phase2-g1-p2-test-fixture-cleanup-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_G1_P2_TEST_FIXTURE_CLEANUP_COMPLETE</promise>`
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动开发 Codex 会话执行 G1-P2 小修复。主 agent 后续只做审计和收口判断。

## 2026-05-19：插入批次 G2 真实项目接入与工程主数据映射修复 MVP 启动

- 用户反馈当前工程主数据初始化仍偏模板套用，容易脱离真实 NAS 项目资产；资产总览中真实项目、样例项目、测试项目和历史数据的区分也不够清晰。
- 用户同时指出交付治理助手 Hermes 面板仍有产品契约问题：
  - 有权限项目可能被感知为权限拒绝。
  - 正文证据缺失和权限拒绝文案混杂。
  - 项目级问题不应强制要求 assetId。
  - 前端不应直接展示裸 requestId。
- 主 agent 判断：这是高于 G1-P2 的产品契约修复。当前暂停 G1-P2，不进入 8B，不开放真实 NAS 增删改查。
- 当前插入批次：
  - `G2：真实项目接入与工程主数据映射修复 MVP`
- 本批只做：
  - 资产总览项目归类与默认筛选。
  - 真实 NAS 项目接入状态展示。
  - 初始化向导升级为真实项目接入向导。
  - 模板草案 / 待确认语义表达。
  - 基于 metadata/catalog 的接入评估和草案预览。
  - 交付治理助手 Hermes project-level 权限、Missing Evidence 和诊断编号表达修复。
- 本批严禁：
  - 真实 NAS 增删改查。
  - 文件移动、删除、重命名、上传。
  - PDF / Office / DWG / RVT / IFC 正文读取。
  - BIM 构件级解析。
  - selective indexing。
  - Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks 写入。
  - Agent 自动审批、自动整改、自动创建真实交付结论。
  - 前端直连 Hermes。
  - 修改 `docs/**`。
- 已写入规划：
  - `handoff/main-agent/phase2-insert-g2-real-project-onboarding-masterdata-mapping-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_INSERT_G2_REAL_PROJECT_ONBOARDING_MASTERDATA_MAPPING_COMPLETE</promise>`
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动独立开发 Codex 会话执行 G2。主 agent 后续只做审计、纠偏和收口判断。

## 2026-05-19：G2 批次命名冻结

- 用户明确要求立即冻结批次命名。
- 冻结结论：
  - 当前批次固定为 `G2：真实项目接入与工程主数据映射修复 MVP`。
  - G2 是 G1 后的真实项目接入纠偏批次。
  - G2 不代表 9A 已完成，也不代表进入了 9A 后续治理。
  - 8B、8C、9A 均尚未正式进入。
  - 不再新增 `H1` / `R1` 等临时命名，避免进一步混乱。
  - 后续所有 prompt、报告、脚本、状态文档必须统一使用 `G2`。
- 已新增命名冻结文件：
  - `handoff/main-agent/phase2-g2-naming-freeze.md`
- 已修正 G2 新专项脚本命名：
  - 固定为 `scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
  - 历史回归脚本 `check-hermes-jarvis-gateway.sh`、`check-phase2-insert-g1-agent-delivery-governance.sh`、`check-phase2-batch8a-bim-lightweight-adapter.sh` 保持原名，仅作为 G2 回归依赖。
- 已同步更新：
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/phase2-insert-g2-real-project-onboarding-masterdata-mapping-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 本轮未修改 `docs/**`。

## 2026-05-20：G2-A 收口判断与 G2-B 启动

- 测试 agent 已完成 `G2-A：真实项目接入与工程主数据映射修复` 验收。
- 结论：通过。
- 当前无 P0/P1。
- 剩余 P2：
  - 外部 Hermes 真服务未启用，`EXPECT_HERMES_AGENT_AVAILABLE=true` 因环境未启用或 token 未注入失败；本地 Gateway 降级链路通过。
  - `/api/data-steward/chat` 直接响应仍包含结构化 `data.trace.requestId`，但前端不裸展示，仅弱化为诊断编号。
  - “全部”筛选里历史 `B6A-SMOKE-*` 项目分类仍不准，默认真实项目视图不受影响。
- 已确认：
  - 105 项目业务编码对应内部 `projectId=503`，被识别为真实 NAS 项目。
  - 105 文件数 `2927`、模型 `198`、图纸 `2729`。
  - 真实项目接入向导可用。
  - 模板草案 / 待确认语义可用。
  - 未确认 apply 被拒绝，未污染 105。
  - Hermes 对有权限项目不误报权限拒绝。
  - 正文 / BIM / 构件类问题返回 Missing Evidence。
  - 未发现真实 NAS 路径、storage 字段、raw row、SQL、token、cookie、password 泄露。
- 主 agent 裁决：
  - G2-A 可收口。
  - 用户要求后续能力不能只解决 105，105 仅作为当前测试样本。
  - 当前启动 `G2-B：既有真实项目治理可用性补丁`。
- G2-B 范围：
  - 资产总览 Hero 区父子结构重排。
  - 真实项目通用治理路径表达。
  - Hermes 常驻入口 MVP。
  - smoke 项目分类修复。
  - 105 作为样本，同时至少验证另一个真实 NAS 项目。
- 已写入规划：
  - `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
- 已写入开发 agent prompt：
  - `handoff/dev-agent/current-prompt.md`
  - 完成承诺：`<promise>PHASE2_G2B_EXISTING_PROJECT_GOVERNANCE_USABILITY_COMPLETE</promise>`
- 已写入测试 agent prompt：
  - `handoff/test-agent/current-prompt.md`
- 当前裁决：可以启动独立开发 Codex 会话执行 G2-B。G2-B 通过后，整个 G2 应收口并进入 Git checkpoint，再恢复二期后续主线候选。

## 2026-05-20：读取 Hermes 能力边界并按四层模型更新 G2-B

- 已读取共享文档：
  - `DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
  - `DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
  - `DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
  - `DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`
  - `DigitalDeliveryProject/docs/01_capability_matrix.md`
  - `DigitalDeliveryProject/integration-contracts/feedback_contract.md`
  - `DigitalDeliveryProject/adr/ADR-002-no-nas-data-in-hermes-memory.md`
- Hermes 接入口径确认：
  - `Catalog Layer -> Evidence Layer -> Memory Layer -> Orchestration Layer`
  - 当前平台只应把 G2-B 做成 Catalog Layer 强化和常驻入口。
  - Evidence Layer、Memory Layer、Orchestration Layer 不在 G2-B 中实现。
- 已新增主 agent 裁决：
  - `handoff/main-agent/hermes-layered-integration-decision.md`
- 已更新：
  - `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
- 当前 G2-B 允许：
  - Hermes 常驻入口。
  - 当前页面 / 当前项目上下文。
  - catalog-only 问答。
  - 页面用途、下一步动作、治理路径解释。
  - Missing Evidence 解释。
- 当前 G2-B 禁止：
  - `document_evidence_search`。
  - PDF / Office 正文问答。
  - DWG / RVT / BIM 内容理解。
  - Hermes long-term memory 写入正文、raw path、raw catalog row。
  - 多 Agent 编排或自动治理。

## 2026-05-20：修复本机 Hermes Gateway 外部服务接入环境

- 问题：
  - 测试报告第 15 项显示本机 `8642` 曾有 Hermes 服务监听，但平台后端处于本地目录兜底模式。
  - 平台侧 `/api/data-steward/hermes/health` 期望返回外部 Hermes 可用，而不是 `GATEWAY_DISABLED_LOCAL_FALLBACK`。
- 处理：
  - 确认平台配置已支持 `HERMES_AGENT_GATEWAY_*` 环境变量。
  - 从 macOS Keychain 的本机安全条目注入 Hermes service token，未在聊天、日志或命令输出中打印 token 值。
  - 恢复本机 Hermes Gateway：`http://127.0.0.1:8642/health` 返回 `status=ok`。
  - 停止旧的未启用 Gateway 的平台后端进程，使用 Hermes Gateway 配置重新启动平台后端。
- 验证结果：
  - `GET /api/data-steward/hermes/health` 已返回：
    - `status=ok`
    - `hermesAvailable=true`
    - `gatewayEnabled=true`
    - `agentAnswerIntegrationEnabled=true`
    - `unavailableReason=""`
    - `runtimeWriteEnabled=false`
    - `readonly=true`
  - `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh` 通过，`PASS=13 FAIL=0`。
- 当前状态：
  - Hermes Gateway PID 已写入 `tmp/run-logs/hermes-gateway.pid`。
  - 平台后端 PID 已写入 `tmp/run-logs/backend.pid`。
  - 本次仅修复本机运行环境，未修改业务代码，未修改 `docs/**`。

## 2026-05-20：Hermes 真实回复链路热修

- 用户问题：
  - 平台中任意提问都显示“仅基于资产目录和权限上下文 / 密级 UNKNOWN / catalog-only”等固定话术。
  - 只有少数预设问题看似可回复，且不像真实 LLM 思考结果。
- 根因：
  - 之前 health 只检查 Hermes `/health`，该接口不校验 token。
  - 实际 `/v1/chat/completions` 因 token 不匹配返回 401，平台后端静默走本地 catalog-only 兜底。
  - 平台未向 Hermes 传入安全的项目目录摘要，真实 Agent 即使接通，也无法回答项目文件数量、类型分布等目录级问题。
- 修复：
  - 修正本机安全存储中的 Hermes service token。
  - 后端 health 增加带认证的 `/v1/models` 校验，避免“绿灯但聊天不可用”。
  - 后端 Gateway 在 `page_context` 注入安全 `catalog_summary`，只包含目录级字段，不包含真实 NAS 路径或正文。
  - 外部 Hermes 正常回复时标记 `trace.agentMode=openai_compatible_catalog_only`。
  - 前端回答卡片明确显示“真实 Hermes 回答”或“平台安全兜底”。
  - 普通目录级问题不再强制展示正文 Missing Evidence；正文、DWG、RVT、BIM、构件类问题仍返回 Missing Evidence。
  - Hermes 专项脚本在 `EXPECT_HERMES_AGENT_AVAILABLE=true` 时强制验证真实外部 Hermes 回复。
- 验证：
  - 后端构建通过。
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 后端健康检查通过。
  - `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh` 通过，`PASS=13 FAIL=0`。
  - `你好` 和 `这个项目有哪些已登记文件？` 已返回真实 Hermes 自然语言回答。
  - `这张 DWG 图纸里有哪些设备？` 仍返回 Missing Evidence，不编造正文或图纸内容。

## 2026-05-20：G2-B Hermes 前端问答超时修复短回归通过

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 结论：
  - G2-B Hermes 问答超时修复通过。
  - 当前无 P0 / P1 / 新增 P2。
  - 上一轮 `timeout of 10000ms exceeded` P1 已关闭。
- 已验证：
  - 全局 Axios 超时仍保持 `10000ms`，未粗暴放大全局超时。
  - `askHermes()` 单独配置 `HERMES_CHAT_TIMEOUT_MS = 45_000`。
  - 前端仍通过平台后端 `/api/data-steward/chat`，未直连 Hermes。
  - 浏览器实测 `这个页面是干什么的？`、`我下一步应该做什么？` 等 10 秒以上真实 Hermes 回答可正常展示。
  - 等待期间输入框和提交按钮禁用，并显示“真实 Hermes 正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。”
  - 正文 / DWG / RVT / BIM / 构件类问题仍返回 Missing Evidence，不编造文件正文或模型内部内容。
  - 未泄露 `/Volumes`、`smb://`、`nas://`、`storage_path`、SQL、raw row、token / secret / password。
- 必跑命令：
  - 前端构建通过，仅保留既有 Vite chunk size warning。
  - 后端健康检查通过。
  - `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh` 通过，`PASS=13 FAIL=0`。
  - `bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh` 通过，`PASS=11 FAIL=0`。
  - `git diff --check` 通过。
- 主 agent 裁决：
  - G2-B 可以收口。
  - 建议进入 G2 整体收口与 Git checkpoint 前的变更分组审计。
  - 由于当前工作区存在较多历史未提交改动和未跟踪文件，Git checkpoint 不应直接全量提交；需要先按 G2、Hermes、历史批次、运行态临时文件分组确认。

## 2026-05-20：G3 Hermes 工程主数据交付路径完善 MVP 启动

- 用户裁决：
  - 当前不应继续增加平台内容，避免平台越来越大而空。
  - 当前首要任务是完善 Hermes 已有功能，让 Hermes 接入真实项目交付路径，尤其是工程主数据相关数字化交付功能。
  - 8B BIM 轻量化任务编排后置。
- 主 agent 裁决：
  - 当前进入 `G3：Hermes 工程主数据交付路径完善 MVP`。
  - G3 是已有功能可用性修复，不是新模块扩张。
  - G3 不进入 `8B / 8C / 9A`。
- 已写入：
  - G3 规划：`handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
  - 开发 agent prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 agent prompt：`handoff/test-agent/current-prompt.md`
  - 路线状态：`handoff/main-agent/phase2-current-roadmap.md`
  - 主状态：`handoff/main-agent/status.md`
- G3 核心验收：
  - 一个普通员工进入 105 或其他真实 NAS 项目后，只问 Hermes，也能知道工程主数据怎么配置、交付缺什么、下一步该点哪里。
- G3 禁止：
  - BIM 轻量化引擎。
  - 真实模型转换。
  - 构件级解析。
  - 文件正文抽取。
  - selective indexing。
  - NAS 增删改查。
  - Agent 自动写库、自动挂接、自动审批、自动整改。

## 2026-05-20：G3 重新定义为 Hermes 平台工作型 Agent MVP

- 用户进一步裁决：
  - Hermes 不能一直只是平台问答 AI。
  - Hermes 必须作为平台内嵌 Agent，真实替员工完成数字化交付工作。
  - 当前重点开发 Hermes 功能，不能一直停留在 catalog-only 问答阶段。
- 主 agent 修正：
  - G3 名称调整为 `G3：Hermes 平台工作型 Agent MVP`。
  - G3 不再只是“问答引导增强”，而是建立受控 Agent 工作能力。
  - Hermes 可以通过平台已有 API 做事，但不能绕过平台做事。
- G3 当前允许：
  - 读取项目状态、工程主数据状态、交付缺失项和候选文件。
  - 生成工程主数据补齐计划。
  - 生成缺失项补交 / 文件挂接推荐方案。
  - 用户人工确认后，调用平台已有推荐挂接能力执行。
  - 展示执行结果并留审计。
- G3 仍禁止：
  - 直接写数据库。
  - 未确认自动挂接、自动审批、自动整改。
  - 真实 NAS 增删改查。
  - 文件正文抽取、DWG/RVT/BIM 内容理解。
  - Hermes memory、OpenSearch、Qdrant、MinIO documents/chunks 写入。
- 已更新：
  - `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`

## 2026-05-20：G3 Hermes 平台工作型 Agent MVP 验收通过

- 测试报告：
  - `handoff/test-agent/latest-report.md`
- 结论：
  - G3 验收通过。
  - 当前无 P0 / P1。
  - 仅有既有前端 Vite chunk size warning，不阻塞。
- 已确认能力：
  - Hermes 面板新增 Action Center。
  - Action Center 区分回答、操作草案、待人工确认、执行结果。
  - 可生成工程主数据补齐计划。
  - 可生成交付缺失项补交 / 文件挂接推荐方案。
  - 未人工确认时不能执行写动作。
  - 人工确认后通过平台既有推荐挂接能力执行。
  - 执行结果展示创建、跳过、失败。
  - 审计日志包含 apply 记录。
- 边界确认：
  - 未进入 8B / 8C / 9A。
  - 未做真实 NAS 写操作。
  - 未读取 PDF / Office / DWG / RVT / IFC 正文。
  - 未做 BIM 构件级解析。
  - 未写 Hermes memory、OpenSearch、Qdrant、MinIO documents/chunks。
  - 未发现真实 NAS 路径、raw storage 字段、raw row、SQL、secret / token / password 泄露。
- 主 agent 裁决：
  - G3 可以收口。
  - 下一阶段不自动进入 8B / 8C / 9A，需用户另行确认。

## 2026-05-22 UX1 验收 P1 处理

- 测试 agent 正式验收结论：UX1 不通过但无 P0，唯一 P1 为 `frontend/src/styles/tokens.css` 仍未跟踪。
- 主 agent 已确认该文件是 `frontend/src/styles/index.css` 引用的运行期样式 token 文件，属于 UX1 必须交付内容。
- 已将处理原则写入 `handoff/main-agent/status.md`：纳入 `frontend/src/styles/tokens.css`，不纳入 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付文件；UX2 仍待 UX1 极短复验通过后再启动。

## 2026-05-22 UX1 收口

- 测试 agent 极短复验已确认 UX1 tokens.css P1 修复通过。
- 当前 UX1 无 P0 / P1 / P2。
- 主 agent 已写入 UX1 收口报告：`handoff/main-agent/ux1-frontend-routing-visual-closure.md`。
- UX1 可正式收口；下一步建议先做 Git checkpoint，再进入 UX2 前端使用逻辑与体验重构专项。

## 2026-05-22 UX2 启动

- 用户确认进入 UX2。
- 已从 UX1 收口提交后切出分支：`codex/ux2-user-experience-refactor`。
- 已将 `handoff/dev-agent/current-prompt.md` 切换为 UX2 当前开发任务。
- 已将 `handoff/test-agent/current-prompt.md` 切换为 UX2 验收任务。
- UX2 仍由 Claude Code 主导开发，主 agent 负责监控和审计。
- UX2 只做前端使用逻辑、信息层级、字段减负、下一步动作提示和视觉体验，不改后端业务逻辑。

## 2026-05-22 UX2 续接安排

- Claude Code 已完成 UX2 视觉子批次，并在 `handoff/dev-agent/latest-report.md` 顶部追加报告。
- 用户确认：UX2 后续功能由原 Codex 开发 agent 继续完成，完善 UX2 后再进入整体验收。
- 主 agent 已将 `handoff/dev-agent/current-prompt.md` 改为 Codex 开发 agent 续接任务。
- 续接重点：
  - 保留 Claude 视觉升级。
  - 继续完成资产总览入口逻辑、项目工作台下一步引导、文件管理字段减负、工程主数据和交付页业务解释。
  - 不修改后端、数据库迁移、接口语义、权限规则和 `docs/**`。

## 2026-05-22 UX2 开发完成，进入整体验收

- Codex 开发 agent 已完成 UX2 使用逻辑与字段减负续接，并更新 `handoff/dev-agent/latest-report.md`。
- 主 agent 已核对开发报告和变更清单：
  - 改动集中在 `frontend/**` 和 handoff 文件。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 变更。
  - 前端新增运行期文件需纳入交付范围，测试 prompt 已补充“运行期前端文件不得保持未跟踪”的 P1 检查，避免重复 UX1 tokens.css 问题。
- 当前建议：进入 UX2 测试 agent 整体验收。

# 2026-05-22 UX3 启动

- 用户确认进入 `UX3：主视图聚焦与认知减负`。
- 已从 UX2 收口提交后切出分支：`codex/ux3-main-view-focus`。
- UX3 不进入 M2C，不新增业务能力，只做 UX 分支内的小批次。
- 已写入：
  - UX3 主计划：`handoff/main-agent/ux3-main-view-focus-plan.md`
  - 开发 agent prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 agent prompt：`handoff/test-agent/current-prompt.md`
  - 主状态：`handoff/main-agent/status.md`
- UX3 核心目标：
  - 资产总览从教程式入口改成项目启动台。
  - 项目工作台首屏聚焦文件管理、项目资产驾驶舱 / 可视化、交付状态。
  - 技术标签和大段说明降级，不再抢主视觉。
  - 旧路由和现有功能保留。
- 边界：
  - 只允许修改 `frontend/**` 和 `handoff/dev-agent/latest-report.md`。
  - 禁止修改后端、数据库迁移、接口语义、权限规则、`docs/**`。

# 2026-05-22 UX3 开发完成，进入测试验收

- 开发 agent 已完成 UX3 并写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 已核对变更范围：
  - 前端页面改动集中在资产总览、项目工作台、项目内导航和壳层。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 变更。
- 开发报告中的自测通过：
  - 前端构建。
  - 后端健康检查。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归。
  - `git diff --check`。
- 当前建议：进入 UX3 测试 agent 验收。

# 2026-05-22 UX3 收口

- UX3 测试 agent 验收通过，报告已写入 `handoff/test-agent/latest-report.md`。
- 当前无 P0 / P1。
- 已确认：
  - Fresh login 后资产总览可在约 3.1 秒内展示项目启动台和核心入口。
  - 503 / 506 项目工作台首屏聚焦文件管理、项目可视化 / 资产驾驶舱、交付状态。
  - 旧链接兼容、文件管理、项目可视化入口和多分辨率布局通过。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归通过。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 改动。
  - 未发现真实 NAS 路径或敏感字段泄露。
- P2 后续打磨项：
  - 既有 Vite chunk size warning。
  - 项目工作台仍有少量偏技术文本。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 非交付未跟踪文件继续排除。
- 主 agent 裁决：UX3 可以正式收口，下一步建议做 Git checkpoint。

## 2026-05-22 UX2 收口

- UX2 测试 agent 整体验收通过，报告已写入 `handoff/test-agent/latest-report.md`。
- 当前无 P0 / P1。
- 已确认：
  - 资产总览、503 / 506 项目工作台、文件管理、工程主数据、交付工作中心、旧链接兼容和多分辨率检查通过。
  - M2B / M2A / M1F / M1E / M1D / M1C 回归通过。
  - 未发现 `backend/**`、数据库迁移、`docs/**` 改动。
  - 未发现真实 NAS 路径或敏感字段泄露。
- P2 后续打磨项：
  - 既有 Vite chunk size warning。
  - 项目工作台少量技术字段仍偏显眼。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 非交付未跟踪文件继续排除。
- 主 agent 裁决：UX2 可以正式收口，下一步建议做 Git checkpoint。

## 2026-05-22 M2E P1 修复完成，进入极短复验

- 开发 agent 已完成 `selectedDraftItemIds` 契约修复，并更新 `handoff/dev-agent/latest-report.md`。
- 主 agent 已审计关键实现：
  - 后端确认接口不再忽略 `selectedDraftItemIds`。
  - 后端会基于重新生成的合法草案集合校验前端传入 ID。
  - 空选择、非法选择均被拒绝。
  - 合法小选择只处理所选草案项及必要依赖，依赖项通过 `DEPENDENCY+...` 标识。
- 主 agent 已运行：
  - 健康检查：通过。
  - `scripts/dev/check-m2e-real-project-masterdata-confirmation.sh`：`PASS=11 FAIL=0`。
  - `git diff --check`：通过。
- 已把 `handoff/test-agent/current-prompt.md` 收窄为 M2E P1 极短复验，避免再次做全量浏览器测试。
- 当前判断：
  - P1 初验已修复。
  - 等测试 agent 极短复验后再决定是否收口 M2E。

## 2026-05-22 M2E 正式收口

- 测试 agent 已完成 `M2E P1 selectedDraftItemIds 契约极短复验`，报告写入 `handoff/test-agent/latest-report.md`。
- 结论：通过。
- 当前无 P0 / P1。
- P2 仅保留既有 Vite chunk size warning。
- 已确认：
  - 空选择、非法选择、合法小选择三类接口契约均通过。
  - 后端不再忽略 `selectedDraftItemIds`。
  - 合法小选择不再全量返回 40 项。
  - 依赖补齐通过 `DEPENDENCY+...` 表达。
  - 页面文案和“查看节点类型”入口已到位。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing 能力。
- 主 agent 裁决：`M2E：真实项目工程主数据人工确认与交付规则落地` 正式收口。
- 下一批次暂不自动启动，等待用户确认。

## 2026-05-22 M2F 启动

- 用户确认主 agent 提出的下一步路线，并允许执行。
- 主 agent 已将当前 active 批次切换为 `M2F：真实项目交付闭环试运行`。
- 已新增：
  - M2F 计划：`handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- M2F 不是新功能堆叠，而是检验 105 真实项目是否能从正式工程主数据进入交付闭环：
  - 文档 / 图纸应交项。
  - 缺失项解释。
  - 人工文件挂接。
  - 审核、驳回、整改、复审。
  - 交付包草案 / 档案目录。
- 仍然禁止：
  - 真实 NAS 文件写入、移动、删除、重命名、复制。
  - 文件正文读取。
  - Hermes / BIM / parser / indexing 新能力。
  - 自动挂接、自动审核、自动整改。

## 2026-05-23 M2F 开发完成，进入测试验收

- 开发 agent 已完成 M2F，并更新 `handoff/dev-agent/latest-report.md`。
- 主 agent 已审计关键变更：
  - `DeliveryApplicationService` 将缺失项和交付包阻塞原因从“尚未挂接文件”补强为业务可读说明。
  - `AgentGovernanceApplicationService` 的缺失项解释补充交付定义，避免只展示交付类型。
  - 新增 `scripts/dev/check-m2f-real-project-delivery-loop.sh`。
- 主 agent 初验：
  - 健康检查通过。
  - M2F 专项脚本通过，`PASS=6 FAIL=0`。
  - `git diff --check` 通过。
- 当前 105 状态：
  - 正式工程主数据已驱动文档 / 图纸交付。
  - 文档 22 个应交项、图纸 22 个应交项。
  - 当前均待人工补交文件。
  - 交付包草案 dry-run 共 44 条，未生成真实物理包。
- 边界确认：
  - 未触碰真实 NAS 文件。
  - 未读取正文。
  - 未新增 Hermes / BIM / parser / indexing。
  - 未自动挂接、自动审核或自动整改。
- 当前建议：进入测试 agent 轻量验收，暂不收口。

## 2026-05-23 M2F 正式收口

- 测试 agent 已完成 M2F 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- P2 仅为既有 Vite chunk size warning 和非交付未跟踪文件提示。
- 已确认：
  - 105 / 503 正式工程主数据能驱动文档 / 图纸交付视图。
  - 缺失项解释包含目标部位、交付定义、交付类型和需要补交的文件类型。
  - 推荐挂接仍需要人工确认。
  - 审核 / 整改查询和交付包草案 dry-run 链路不回归。
  - 未触碰真实 NAS 文件，未读取正文，未新增 Hermes / BIM / parser / indexing 能力。
- 主 agent 裁决：`M2F：真实项目交付闭环试运行` 正式收口。
- 下一批次暂不自动启动，等待用户确认。

## 2026-05-23 数字孪生 PR 并入兼容性检查

- 用户说明数字孪生分支已通过 GitHub PR 合并回主线，要求进入下一批次前检查兼容性和并入情况。
- 主 agent 已确认：
  - 本地 `main` 与 `origin/main` 一致。
  - 当前 `HEAD` 为 `a5f7064 feat: add digital twin collaboration dashboard (#1)`。
  - 数字孪生 PR 已并入主线。
- 已执行兼容检查：
  - 后端构建通过。
  - 前端构建通过。
  - 健康检查通过。
  - M2F / M2E / M2C / M2B 回归通过。
  - 数字孪生聚合接口在登录和项目上下文下返回 `code=OK`。
  - 未登录访问数字孪生接口返回 `401`。
  - 未发现真实 NAS 路径或敏感字段泄露。
- 已写入报告：
  - `handoff/main-agent/digital-twin-pr-compatibility-check.md`
- 当前裁决：
  - 数字孪生 PR 可视为兼容并入。
  - 可以继续进入下一批次。
  - 下一批次建议仍为 `M2G：真实 NAS 文件管理器灰度完善`，不要在 M2G 中继续扩展数字孪生。

## 2026-05-23 M2G 启动

- 用户确认进入 `M2G：真实 NAS 文件管理器灰度完善`。
- 主 agent 已新增：
  - M2G 计划：`handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
  - 开发 prompt：`handoff/dev-agent/current-prompt.md`
  - 测试 prompt：`handoff/test-agent/current-prompt.md`
- M2G 批次产品定位：
  - 类 Windows / macOS 文件管理器。
  - 融合工程资料字段。
  - 核心目标是让员工日常好用，而不是新增大模块。
- 本批重点：
  - 修复文件管理器项目上下文误判。
  - 打磨上传、新建、重命名、移动、删除到回收站、恢复闭环。
  - 强化当前目录可写状态、操作反馈、回收站和操作记录。
- 本批边界：
  - 不开放普通用户永久删除。
  - 不绕过灰度和权限。
  - 不读取正文。
  - 不扩展 Hermes / BIM / parser / indexing。
  - 不继续扩展数字孪生。

## 2026-05-23 M2G 开发完成，进入测试前审计

- 开发 agent 已完成 M2G 并更新 `handoff/dev-agent/latest-report.md`。
- 主 agent 审计结论：
  - M2G 文件管理器核心改动存在，集中于 `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`。
  - 新增 M2G 专项脚本 `scripts/dev/check-m2g-nas-file-manager-polish.sh`。
  - 专项脚本已通过，`PASS=26 FAIL=0`。
  - `git diff --check` 通过。
- M2G 已实现的文件管理器体验：
  - NAS 写操作前端只按路由项目判断权限，不再用全局 `currentProject` 兜底。
  - 新增当前文件夹可写 / 不可写状态说明。
  - 当前文件夹重命名、移动、移入回收站收进下拉。
  - 移动目标增加项目内相对目录校验。
  - 操作记录和回收站状态改成业务语言。
- 并行分支改动：
  - 当前工作区还包含数字孪生 / 可视化相关改动：
    - `backend/delivery-visualization-adapter/**`
    - `frontend/src/modules/visualization/**`
  - 用户已确认这些改动来自另一个 agent 在另一个分支上的数字孪生平台可视化界面开发。
  - 这些改动不是 M2G 文件管理器交付内容；不因其存在阻塞 M2G，但如果造成构建或回归失败，仍按回归问题处理。
  - 主 agent 已将测试 prompt 调整为“并行分支改动观察项”，要求测试 agent 记录但不误判为 M2G 本身问题。
- 当前裁决：
  - 允许 M2G 进入测试 agent 轻量验收。
  - M2G 收口提交时只选择性纳入 M2G 相关文件，不把数字孪生 / 可视化改动混入同一个 M2G 提交。

## 2026-05-23 M2G 正式收口

- 测试 agent 已完成 M2G 轻量验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- P2：
  - 既有 Vite chunk size warning。
  - 非交付未跟踪文件继续排除。
  - 数字孪生 / 可视化并行分支改动继续作为观察项，不混入 M2G 提交。
- 已通过：
  - 后端构建。
  - 前端构建。
  - 健康检查。
  - M2G 专项脚本，`PASS=26 FAIL=0`。
  - M2B 回归，`PASS=18 FAIL=0`。
  - 文件访问安全回归，`PASS=18 FAIL=0`。
  - `git diff --check`。
- 已确认：
  - 文件管理页可打开，无白屏、无横向溢出。
  - 当前文件夹状态、可写 / 不可写原因、回收站、操作记录均可见且业务语言清楚。
  - 上传、新建、重命名、移动、移入回收站、恢复闭环在隔离测试项目和临时目录中通过。
  - 真实业务目录未被脚本写入、移动、删除、改名或复制。
  - 未泄露真实 NAS 路径或敏感字段。
  - 未新增 Hermes / BIM / parser / indexing 能力。
- 主 agent 裁决：`M2G：真实 NAS 文件管理器灰度完善` 正式收口。
- 当前 active 批次：`待用户确认`。
- 下一步建议：进入 `M2H：多真实项目复制试点`，验证当前能力不是为单一项目特化。

## 2026-05-23 M2H 开发完成，进入测试验收

- 用户提出文件管理器要接近 Windows 文件资源管理器：
  - 左键高亮选中。
  - Ctrl / Command 多选。
  - Shift 连选。
  - 右键重命名、移入回收站等操作。
  - 双击文件夹进入目录。
  - PDF / 图片 / Office / CAD 按现有预览策略处理。
  - RVT / BIM 等模型进入预览占位，等待后续引擎接入。
- 主 agent 曾误入直接开发路径，用户纠正后已恢复为“开发 agent 开发、主 agent 监控审计”的模式。
- 开发 agent 已完成 `M2H：Windows 风格文件管理器交互升级`，报告写入 `handoff/dev-agent/latest-report.md`。
- 本批实际改动范围：
  - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
  - `scripts/dev/check-m2h-windows-file-manager.sh`
  - `handoff/dev-agent/latest-report.md`
- 本批新增能力：
  - 文件夹 + 文件统一列表。
  - 单选、多选、连选、空白处取消选择。
  - 右键上下文菜单。
  - 双击文件夹进入。
  - 双击文件按预览策略分流。
  - 模型预览占位。
  - 批量移动、批量移入回收站、批量下载入口清单。
- 本批明确没做：
  - 未改后端、数据库、`docs/**`。
  - 未新增 Hermes / BIM / parser / indexing / 正文读取能力。
  - 未开放永久删除。
  - 未在真实业务目录执行破坏性写操作。
- 主 agent 初审与回归：
  - 前端构建通过，仅既有 Vite chunk warning。
  - 健康检查通过。
  - M2H 专项脚本通过，`PASS=23 FAIL=0`。
  - M2G 回归通过，`PASS=26 FAIL=0`。
  - M2B 回归通过，`PASS=18 FAIL=0`。
  - 文件访问安全回归通过，`PASS=18 FAIL=0`。
  - `git diff --check` 通过。
- 当前裁决：
  - 进入测试 agent 轻量验收。
  - M2H 尚未收口，不提交不推送。
  - 已将 `handoff/test-agent/current-prompt.md` 更新为 M2H 验收 prompt。

## 2026-05-23 M2H 测试不通过，进入 P1 修复

- 测试 agent 报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：不通过。
- P0：无。
- P1：
  - PDF / 图片等浏览器原生预览文件双击未打开平台受控预览入口。
  - 浏览器日志出现 `Unhandled error during execution of native event handler` 和 `cancel`。
- 测试已确认：
  - 文件夹双击进入目录正常。
  - RVT 模型双击打开占位弹窗正常。
  - 自动化脚本和主线回归通过。
- 主 agent 判断：
  - 问题集中在 `AssetProjectFileBrowser.vue` 的双击预览 / access ticket 打开路径。
  - 不需要扩展后端，不需要新增能力。
  - 修复范围应限制在前端文件管理器和 M2H 专项脚本。
- 已更新：
  - `handoff/dev-agent/current-prompt.md`
- 下一步：
  - 开发 agent 修复 P1。
  - 修复后测试 agent 做极短复验：PDF 双击、右键预览、RVT 占位、文件夹双击、M2H 脚本和 `git diff --check`。

## 2026-05-23 M2H P1 修复完成，进入极短复验

- 开发 agent 已完成 `M2H P1：PDF / 图片双击受控预览入口修复`。
- 报告写入 `handoff/dev-agent/latest-report.md`。
- 本轮修复集中在：
  - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
  - `scripts/dev/check-m2h-windows-file-manager.sh`
  - `handoff/dev-agent/latest-report.md`
- 修复要点：
  - PDF / 图片双击进入平台受控 `file-access` 预览地址。
  - 右键 `打开 / 预览` 与双击共用受控预览路径。
  - 异步点击事件统一通过保护函数执行。
  - 用户取消类 `cancel` 被识别并安静处理。
  - 新窗口被浏览器拦截时提供 fallback 弹窗和手动打开入口。
- 主 agent 初验：
  - `curl -fsS http://127.0.0.1:8080/actuator/health`：通过。
  - `bash scripts/dev/check-m2h-windows-file-manager.sh`：`PASS=30 FAIL=0`。
  - `git diff --check`：通过。
- 边界确认：
  - 未改后端、数据库、`docs/**`。
  - 未新增 Hermes / BIM / parser / indexing / 正文读取。
  - 未执行真实业务目录写操作。
- 已将 `handoff/test-agent/current-prompt.md` 更新为 `M2H P1 PDF / 图片受控预览极短复验`。

## 2026-05-23 M2H 正式收口

- 测试 agent 已完成 M2H P1 极短复验，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- P2：
  - 既有 Vite chunk warning。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 收口提交时需纳入 Git。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件继续排除。
- 已确认：
  - PDF 双击打开受控 `file-access` 入口。
  - 右键 `打开 / 预览` 打开同类受控入口。
  - `cancel` / native event handler 未处理错误已关闭。
  - 文件夹双击和模型预览占位未回归。
  - 未执行真实业务目录写操作。
  - 未泄露真实 NAS 路径。
  - 未新增 Hermes、真实 BIM、parser、indexing 或正文读取能力。
- 主 agent 裁决：`M2H：Windows 风格文件管理器交互升级` 正式收口。
- 当前 active 批次：`待用户确认`。
- 收口提交建议：
  - 纳入 `AssetProjectFileBrowser.vue`、`scripts/dev/check-m2h-windows-file-manager.sh` 和相关 handoff 文件。
  - 排除 `.claude/**`、`CLAUDE.md`、`tmp/**`。

## 2026-05-23 M2H 返工：文件管理器目录直达子项口径

- 用户在 105 项目文件管理器中发现真实使用缺陷：
  - 左侧目录不完整，只显示 `01`、`03`、`05`。
  - 根目录右侧混入了深层文件。
  - 其他目录也应只显示当前目录的直接子项。
- 主 agent 初步代码判断：
  - `AssetProjectFileBrowser.vue` 根目录 `activeDir` 为空时没有向 `fetchCatalogFiles` 传目录过滤，所以根目录可能显示整个项目文件。
  - `CatalogApplicationService.appendDirectoryFilter(...)` 当前使用 `LIKE 当前目录/%` 这类递归匹配，适合“目录及子目录”检索，不适合资源管理器式“当前目录直接子项”。
  - 目录树需要检查 `catalog/directories` 与 `buildDirectoryTree(...)` 是否遗漏空目录、目录记录或祖先节点。
- 决策：
  - 作为 M2H 返工 bugfix 处理。
  - 开发 agent 负责修复，主 agent 不直接改业务代码。
  - 允许开发 agent 增加 `directOnly` / `directoryScope=DIRECT` 等显式参数，以保留旧递归语义兼容。
  - 禁止前端全量拉取后假过滤。
  - 禁止恢复页面打开时同步递归扫描 NAS。
- 已更新：
  - `handoff/dev-agent/current-prompt.md`

## 2026-05-24 M2H 目录直达子项返工完成，进入极短验收

- 开发 agent 已完成 M2H 目录直达子项返工，报告写入 `handoff/dev-agent/latest-report.md`。
- 本轮改动范围：
  - `CatalogController.java`
  - `CatalogApplicationService.java`
  - `dataSteward.ts`
  - `AssetProjectFileBrowser.vue`
  - `directoryTree.ts`
  - `scripts/dev/check-m2h-windows-file-manager.sh`
  - `handoff/dev-agent/latest-report.md`
- 核心修复：
  - `/api/data-steward/catalog/files` 新增 `directOnly`。
  - 默认不传 `directOnly` 时保持旧递归目录检索。
  - 文件管理器传 `directOnly=true`，只取当前目录直接文件。
  - 根目录 direct-only 只返回映射根或空父目录下的直接文件。
  - 子目录 direct-only 只返回父目录等于当前目录的文件。
  - `directoryTree.ts` 不再公共前缀压缩，避免真实目录层级被隐藏。
- 主 agent 初验：
  - 健康检查通过。
  - M2H 专项脚本通过，`PASS=51 FAIL=0`。
  - `git diff --check` 通过。
- 边界确认：
  - 未修改 `docs/**`。
  - 未新增数据库迁移。
  - 未执行真实业务 NAS 破坏性写操作。
  - 未新增 Hermes / 真实 BIM / parser / indexing / 正文读取。
- 已更新：
  - `handoff/test-agent/current-prompt.md`
- 下一步：
  - 测试 agent 做极短验收，重点看 105 / 503 根目录和子目录是否只显示直接子项，左侧目录是否完整。

## 2026-05-24 M2H 返工正式收口

- 测试 agent 已完成 M2H 目录直达子项返工极短验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- P2：
  - 既有 Vite chunk warning。
  - `scripts/dev/check-m2h-windows-file-manager.sh` 仍是未跟踪交付脚本，提交时必须纳入 Git。
  - `.claude/**`、`CLAUDE.md`、`tmp/**` 继续排除。
- 已确认：
  - `503 / 105 启航华居项目` 根目录右侧为 `4 个文件夹 / 1 个文件`，未再显示全项目深层文件。
  - `01_文件收发` 右侧仅显示直接子文件夹 `01_图纸`。
  - `01_文件收发/01_图纸/01_收` 右侧只显示当前目录直接子文件夹。
  - `03_过程文件/02_BIM模型/04_机电/地下三层` 右侧显示当前目录直接 RVT 模型文件，未混入其他目录文件。
  - 左侧目录树完整展示真实层级。
  - PDF 受控预览、模型预览占位、文件夹双击进入目录均未回归。
  - `directOnly=true` 新语义与旧递归兼容均通过脚本验证。
  - 未执行真实业务 NAS 写操作，未暴露真实 NAS 路径，未新增 Hermes / 真实 BIM / parser / indexing / 正文读取。
- 主 agent 裁决：M2H 返工正式收口。
- 收口提交建议：
  - 纳入本批后端 catalog files direct-only 改动、前端文件管理器改动、目录树工具改动、M2H 专项脚本和 handoff 文件。
  - 排除 `.claude/**`、`CLAUDE.md`、`tmp/**`。

## 2026-05-26 M3E 启动

- M3D 已合并回 `main` 并推送，`main` 最新进度为 M3D。
- 已创建新分支：`codex/m3e-preview-artifacts-object-storage`。
- 当前 active 批次：`M3E：预览与转换产物对象化`。
- 主 agent 已写入：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/main-agent/m3e-preview-artifacts-object-storage-plan.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- M3E 开发边界：
  - 只处理预览 / 转换产物与对象存储关系。
  - 不做真实转换器。
  - 不做 Hermes 正文问答。
  - 不写语义索引。
  - 不读取文件正文。
  - 不暴露真实路径或对象定位。

## 2026-05-27 M3E 正式收口

- 测试 agent 已完成 M3E 验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- 已确认：
  - `scripts/dev/check-m3e-preview-artifacts-object-storage.sh` 通过，`PASS=8 FAIL=0`。
  - M3D / M3C / M3C-1 / M3B / M3A / file-access 回归通过。
  - PDF / 图片对象化预览产物状态可查。
  - DWG / RVT / Office 只生成转换占位，不伪造可预览。
  - file-access 与交付包预检查未回归。
  - staged 范围不含 `docs/**`、Hermes 正文问答、parser/indexing、真实 NAS 写操作扩展。
  - 禁出字段扫描通过。
- 主 agent 裁决：`M3E：预览与转换产物对象化` 正式收口。
- 已更新：
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
  - `handoff/main-agent/m3e-preview-artifacts-object-storage-closure.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
- 原下一步建议为 `M4A：documents / chunks 语义证据契约`；2026-05-27 已根据用户新裁决插入 `M3F：新文件对象存储优先写入` 与后续 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件`，M4A 顺延。

## 2026-05-27 M3G-1 准备：任务图与 NAS 侧 MinIO 配置交接

- 用户要求进入 `M3G-1`，先规划任务图并配置 NAS 端 MinIO。
- 主 agent 已冻结本批定位：
  - `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`
  - 本批不执行真实历史文件对象化迁移。
  - 本批不移动、删除、重命名真实 NAS 原项目资料。
- 已做非侵入式探测：
  - `192.168.1.181:9000` 未响应。
  - `192.168.1.181:9001` 未响应。
  - 当前不能宣称 NAS 侧 MinIO 已部署。
- 已新增：
  - `handoff/main-agent/m3g1-task-graph.md`
  - `handoff/main-agent/m3g1-nas-minio-ops-preparation.md`
- 已更新：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 当前裁决：
  - 先由运维 / NAS 负责人完成 NAS 侧 MinIO 部署与 service account 准备。
  - 开发 agent 只实现 readiness、全项目对象化盘点和 dry-run。
  - M3G-1 未通过前，不进入 M3G-2 历史文件真实对象化执行。

## 2026-05-27 M3G-1 环境就绪：NAS 侧 MinIO 已启动

- 用户已在 Synology NAS 上通过 Container Manager 启动 MinIO。
- 当前验证：
  - `http://192.168.1.181:9000/minio/health/ready` 可达。
  - `http://192.168.1.181:9001` Console 端口可达。
  - bucket `zy-datahub-assets-prod` 已创建。
  - 平台后端已临时注入 NAS MinIO 环境变量并启动，`/actuator/health` 返回 `UP`。
- 当前边界：
  - 这只代表 NAS 侧 MinIO 服务和平台运行环境已具备 M3G-1 开发验证条件。
  - 不代表历史项目文件已经对象化。
  - 不允许跳过 M3G-1 直接执行全量迁移。
- 下一步：
  - 开发 agent 按 `handoff/dev-agent/current-prompt.md` 执行 M3G-1。
  - 重点实现 readiness、全项目对象化盘点和 dry-run 计划。

## 2026-05-27 M3G-1 正式收口

- 开发 agent 已完成 M3G-1，并完成 P1 修复批次。
- 测试 agent 复验通过，报告写入 `handoff/test-agent/latest-report.md`。
- 已关闭上一轮 2 个 P1：
  - M3E：NAS 侧 MinIO 下旧对象 metadata 指向旧对象存储导致 file-access 失败。
  - M3F：脚本按本机 Docker MinIO 模拟对象存储不可用，不适配 NAS 侧 MinIO。
- 当前通过：
  - M3G-1 专项：`PASS=9 FAIL=0`
  - M3E：`PASS=8 FAIL=0`
  - M3F：`PASS=11 FAIL=0`
  - M3C：`PASS=9 FAIL=0`
  - file-access：`PASS=18 FAIL=0`
- 主 agent 裁决：
  - `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划` 正式收口。
  - 当前不进入真实历史文件对象化执行。
  - 下一步如继续 M3G，应单独启动 `M3G-2：历史文件对象化执行与读取链路切换灰度`。

## 2026-05-27 M3G-2 启动

- 用户确认可以开始做文件上传 MinIO，范围先限定 105 项目。
- 主 agent 已核对当前 `StorageService` 读取策略：
  - 已有 active object version：优先读取对象存储。
  - 无 active object version：读取原 NAS 台账路径。
  - active object version 存在但对象副本不可读：fail-closed，不静默回退 NAS。
- 主 agent 裁决当前批次：
  - `M3G-2：105 项目历史文件对象化上传灰度`
- 已新增 / 更新：
  - `handoff/main-agent/m3g2-105-objectification-gray-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- 本批继续禁止：
  - 全量迁移所有项目。
  - 移动、删除、改名真实 NAS 原项目文件。
  - Hermes 正文问答、语义索引、文件正文读取。
  - 暴露真实路径、bucket、object key、storage URI、SQL、raw row、token、secret。

## 2026-05-28 M3G-2 正式收口

- 开发 agent 已完成 `M3G-2：105 项目历史文件对象化上传灰度`。
- 测试 agent 已完成只读验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结论：通过。
- 当前无 P0 / P1。
- 已完成：
  - 105 首批对象化 `fileId=936/937/938`，`success=3 / skipped=0 / failure=0`。
  - 重复任务 `taskId=144` 幂等跳过。
  - 主 agent 复验追加对象化 `fileId=939/940/941`。
  - 当前 105 约为 `OBJECT_STORED=9 / NAS_ONLY=2919 / coverage=0.31%`。
  - 已对象化文件通过受控 `file-access` 读取。
  - 未对象化文件仍为 `NAS_ONLY` 并继续可读。
- 已确认：
  - NAS 原文件未移动、未删除、未改名、未覆盖。
  - 未读取正文。
  - 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
  - 未触发 Hermes。
  - 未接入 BIM 引擎。
  - 未修改 `docs/**`。
- 主 agent 裁决：
  - M3G-2 正式收口。
  - 下一步如继续 M3G，应进入 `M3G-3：多真实项目分批对象化策略与任务中心增强`。
  - M4A 语义证据契约继续后置。

## 2026-05-28 M3G-3 启动

- 用户确认进入下一步。
- 主 agent 裁决当前 active 批次：
  - `M3G-3：多真实项目分批对象化策略与任务中心增强`
- 本批目标：
  - 增强多项目对象化盘点。
  - 增强多项目 dry-run。
  - 增强任务中心策略展示。
  - 支持文件数 / 容量 / 项目范围限制。
- 本批不做：
  - 全量迁移所有项目。
  - 默认执行多项目真实对象化。
  - 文件正文读取。
  - Hermes 正文问答。
  - 语义索引。
  - 真实 BIM 引擎。
- 已新增 / 更新：
  - `handoff/main-agent/m3g3-multi-project-objectification-task-center-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`

## 2026-05-28 M3G-3 正式收口

- 开发 agent 完成 M3G-3，报告写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 完成 M3G-3 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结果：
  - 当前 P0：无。
  - 当前 P1：无。
  - M3G-3 专项脚本通过，`PASS=11 FAIL=0`。
  - M3G-1 / M3E / M3F / M3C / file-access 回归全部通过。
- 主 agent 审计确认：
  - 多项目对象化只停留在 dry-run 规划阶段。
  - 未创建真实迁移任务。
  - 未运行 M3G-2 执行型脚本。
  - 未复制、移动、删除、重命名、覆盖真实 NAS 文件。
  - 未暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
  - 未新增 Hermes 正文问答、documents / chunks、Qdrant / OpenSearch、BIM 引擎、parser / indexing。
- 主 agent 裁决：
  - `M3G-3：多真实项目分批对象化策略与任务中心增强` 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 下一步候选为 `M3G-4：受控多项目小批对象化执行` 或 `M4A：documents / chunks 语义证据契约`。

## 2026-05-28 M3G-4 启动

- 用户确认进入下一步。
- 主 agent 裁决当前 active 批次：
  - `M3G-4：受控多项目小批对象化执行`
- 本批目标：
  - 基于 M3G-3 dry-run 计划，进入真实小批对象化执行。
  - 执行范围必须很小，默认不超过 3 个真实项目、每项目 3 个文件、总文件数 9 个、总容量 100MB。
  - 必须人工确认，后端必须强制上限。
- 本批不做：
  - 全量迁移所有项目。
  - 迁移 NAS 根目录。
  - 迁移测试 / 样例 / 归档项目。
  - 文件正文读取。
  - Hermes 正文问答。
  - 语义索引。
  - 真实 BIM 引擎。
- 已新增 / 更新：
  - `handoff/main-agent/m3g4-controlled-multi-project-objectification-plan.md`
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/phase2-current-roadmap.md`
  - `handoff/main-agent/m3-storage-evidence-chain-todo.md`

## 2026-05-28 M3G-4 正式收口

- 开发 agent 完成 M3G-4，报告写入 `handoff/dev-agent/latest-report.md`。
- 测试 agent 完成 M3G-4 正式验收，报告写入 `handoff/test-agent/latest-report.md`。
- 验收结果：
  - 当前 P0：无。
  - 当前 P1：无。
  - M3G-4 专项脚本通过，`PASS=21 FAIL=0`。
  - M3G-3 / M3G-1 / M3F / M3E / M3C / file-access 回归全部通过。
- 主 agent 审计确认：
  - `confirmed=false` 被拒绝。
  - 超限执行被拒绝。
  - 小批真实对象化成功。
  - 重复执行幂等，不污染 active object version。
  - 已对象化文件可通过受控 `file-access` 读取。
  - NAS 原文件 `size/mtime` 未变化。
  - 未暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
  - 未新增 Hermes 正文问答、documents / chunks、Qdrant / OpenSearch、BIM 引擎、parser / indexing。
- 主 agent 裁决：
  - `M3G-4：受控多项目小批对象化执行` 正式收口。
  - 当前 active 批次切换为 `待用户确认`。
  - 下一步优先处理文件管理器搜索与路径展示体验问题，再评估扩大对象化或进入 M4A。

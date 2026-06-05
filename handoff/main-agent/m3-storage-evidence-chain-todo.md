# M3-M5：对象存储与 Hermes 语义证据链任务图

更新时间：2026-06-04

## 0. 总目标

把当前 `NAS + MySQL 台账` 分阶段升级为：

```text
NAS 原始文件保留
-> 对象存储镜像
-> MySQL 业务台账
-> documents / chunks 语义契约
-> 向量 / 关键词索引
-> Hermes 受控证据问答
-> 工程主数据与交付治理 Agent 化
```

总原则：

- NAS 原文件仍是源头，不移动、不删除、不改名。
- 不一次性搬空 NAS。
- 不跳过权限、对象存储、证据契约直接做 Hermes 正文问答。
- 不把 catalog-only 元数据伪装成正文证据。
- 不把对象存储迁移说成语义理解完成。
- 前端和 API 不得泄露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
- Hermes 不直连 MySQL、NAS、MinIO/S3、Qdrant、OpenSearch。
- 每完成一项，由主 agent 更新本文件复选框，并同步对应 handoff / 共享契约文档。

## 1. 总任务图

主任务图已拆到：

- `handoff/main-agent/m3-m5-storage-evidence-task-graph.md`

本文件保留详细历史记录和阶段说明；后续每完成一项，两个文件都要同步打勾。

- [x] 0. 契约与边界冻结
- [x] 1. 平台资产 UUID 与文件状态统一（M3C-1 已收口）
- [x] 2. 对象存储迁移任务中心（M3C 已收口）
- [x] 3. 真实 NAS 小范围灰度镜像（M3D 已收口）
- [x] 4. 预览与转换产物对象化（M3E 已收口）
- [x] 4.5 新文件对象存储优先写入（M3F 已收口）
- [x] 4.6 NAS 侧 MinIO 对象存储接管真实项目文件（M3G-1 至 M3G-9 已验收通过，等待 M3 整体收口判断）
- [ ] 5. documents / chunks 语义契约
- [ ] 6. 向量库与关键词索引试点
- [ ] 7. Hermes 受控 Evidence API
- [ ] 8. 工程主数据与交付治理 Agent 化

## 2. 阶段 0：契约与边界冻结

状态：`已完成`

建议批次名：`M3C-0：资产存储与证据链契约冻结`

交付要求：

- 建立或更新共享契约文档：资产对象存储与 Hermes 证据链契约。
- 明确 NAS 原文件仍是源头，不移动、不删除、不改名。
- 明确 Hermes 不直连 MySQL、NAS、MinIO/S3、Qdrant、OpenSearch。
- 明确 API 和前端不得泄露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
- 明确对象存储负责文件本体治理，向量库负责语义检索，二者不能混为一个完成项。

验收条件：

- [x] 共享契约文档已同步。
- [x] 主线 handoff 中有明确边界。
- [x] 未修改业务代码。

完成记录：

- 共享契约：
  - `integration-contracts/asset_storage_evidence_chain_contract.md`
- ADR：
  - `adr/ADR-007-object-storage-evidence-chain-boundary.md`
- 同步更新：
  - `integration-contracts/platform_to_hermes_contract.md`
  - `integration-contracts/gateway_response_contract.md`
  - `docs/01_capability_matrix.md`

## 3. 阶段 1：平台资产 UUID 与文件状态统一

状态：`已完成`

建议批次名：`M3C-1：资产 UUID 与存储状态统一`

交付要求：

- 为文件资产建立稳定 UUID，作为平台公开资产 ID。
- 保留现有数值 `file_id` 作为内部主键和排障 ID。
- 扫描入库、手工创建、对象迁移都必须生成 UUID。
- 已有文件补齐 UUID。
- 前端展示“平台资产ID”，必要时辅助展示内部文件 ID。
- 统一状态：
  - `NAS_ONLY`
  - `MIGRATION_PENDING`
  - `OBJECT_STORED`
  - `MIGRATION_FAILED`

验收条件：

- [x] 新旧文件均有稳定 UUID。
- [x] API / 前端优先展示平台资产 ID。
- [x] 内部 file_id 不被误当作对外稳定 ID。
- [x] 状态口径与 storage-status 一致。
- [x] 禁出字段扫描通过。

## 4. 阶段 2：对象存储迁移任务中心

状态：`已完成`

建议批次名：`M3C：对象存储迁移任务中心与批量策略`

交付要求：

- 基于 M3A/M3B 补成任务中心：任务列表、详情、重试、失败原因、进度、操作者、审计。
- 迁移入口必须受控：
  - 明确 `fileIds`
  - 项目灰度清单
  - 数量上限
  - 大小上限
- 每个文件流程固定：
  - 权限校验
  - 生命周期校验
  - 读取 NAS
  - checksum
  - 上传对象存储
  - 校验
  - 写对象记录
  - 写文件对象版本
- 迁移必须幂等，重复执行不能污染 active 对象版本。
- 响应只返回业务状态，不返回底层路径或 object key。

验收条件：

- [x] 任务可创建、可重试、可审计。
- [x] 任务详情可解释成功 / 失败 / 跳过。
- [x] 幂等验证通过。
- [x] 禁出字段扫描通过。
- [x] M3A/M3B 回归通过。

完成记录：

- M3C 专项脚本：
  - `scripts/dev/check-m3c-storage-migration-task-center.sh`
- 前端入口：
  - `/data-steward/assets/:projectId/data-steward/storage-migration`
- 收口记录：
  - `handoff/main-agent/m3c-storage-migration-task-center-closure.md`

## 5. 阶段 3：真实 NAS 小范围灰度镜像

状态：`已完成`

建议批次名：`M3D：真实 NAS 小范围灰度镜像`

交付要求：

- 选择 105 项目少量真实业务文件灰度，覆盖 PDF / DWG / RVT 小样本。
- 不做全量迁移。
- NAS 原文件必须保留。
- 已镜像文件显示 `OBJECT_STORED`。
- preview / download 继续走受控 `file-access`。
- 灰度报告记录：
  - 成功数
  - 失败数
  - 跳过数
  - checksum 覆盖率
  - 禁出字段扫描结果

验收条件：

- [x] 真实业务文件只读镜像成功。
- [x] NAS 原文件未被改动。
- [x] PDF / DWG / RVT 小样本状态可查。
- [x] file-access 受控访问不回归。
- [x] 禁出字段扫描通过。

完成记录：

- M3D 专项脚本：
  - `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`
- 灰度样本：
  - PDF：`fileId=993`
  - DWG：`fileId=935`
  - RVT / MODEL：`fileId=1257`
- 收口记录：
  - `handoff/main-agent/m3d-real-nas-object-mirror-gray-closure.md`

## 6. 阶段 4：预览与转换产物对象化

状态：`已完成`

建议批次名：`M3E：预览与转换产物对象化`

启动记录：

- 分支：`codex/m3e-preview-artifacts-object-storage`
- M3D 已合并回 `main` 后启动。
- 当前开发 prompt：`handoff/dev-agent/current-prompt.md`

交付要求：

- PDF 预览、图片缩略图、Office 转换产物、CAD 转换产物、BIM 轻量化产物都应进入对象存储。
- 建立原件、衍生产物、预览产物之间的版本关系。
- DWG / RVT 可先显示“需转换”，不阻塞主链路。
- 预览 URL 必须受权限控制、可过期、不暴露 object key。

验收条件：

- [x] 衍生产物与源文件关系可查。
- [x] 预览产物走受控 `file-access` 或受控 preview ticket。
- [x] 不暴露底层对象定位。
- [x] DWG / RVT / Office 未转换时能安全展示转换占位。

完成记录：

- M3E 专项脚本：
  - `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- 已覆盖：
  - PDF / 图片：`BROWSER_NATIVE_PREVIEW / AVAILABLE / OBJECT_STORED`
  - DWG：`CAD_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / PENDING`
  - RVT：`BIM_LIGHTWEIGHT_PLACEHOLDER / NEEDS_CONVERSION / PENDING`
  - Office：`OFFICE_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / PENDING`
- 收口记录：
  - `handoff/main-agent/m3e-preview-artifacts-object-storage-closure.md`

## 7. 阶段 4.5：新文件对象存储优先写入

状态：`已完成`

建议批次名：`M3F：新文件对象存储优先写入与 NAS 兼容回退`

交付要求：

- 文件管理器新增上传的文件优先写入对象存储。
- 新上传文件仍进入 `data_file_resources` 业务台账。
- 新上传文件生成稳定 `assetUuid`。
- 新上传文件写入 `data_storage_objects` 与 active `data_file_object_versions`。
- 新上传文件 `storage-status=OBJECT_STORED`。
- 新上传文件预览 / 下载继续通过受控 `file-access`。
- 对象存储不可用时返回业务错误，或在显式配置下进入可审计 NAS fallback；不得假成功。
- 不影响历史 NAS 文件访问，不影响 M3C 迁移任务中心。

验收条件：

- [x] 新上传文件默认对象存储化。
- [x] 受控 `file-access` 可读取对象存储新增文件。
- [x] API / 前端不暴露真实 NAS 路径、bucket、object key、`storage_uri`。
- [x] M3E / M3D / M3C / M3B / M3A / file-access 回归通过。

完成记录：

- M3F 专项脚本：
  - `scripts/dev/check-m3f-object-storage-first-write.sh`
- 收口记录：
  - `handoff/main-agent/m3f-object-storage-first-write-closure.md`

## 8. 阶段 4.6：NAS 侧 MinIO 对象存储接管真实项目文件

状态：`M3G-1` 至 `M3G-9` 已完成验收；当前进入 `DOC-BASE：文档基线收口`，之后再做 `M3-CLOSE：M3 主线整体收口判断`。

建议批次名：`M3G：NAS 侧 MinIO 对象存储接管真实项目文件`

已完成子批次：

`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`

`M3G-2：105 项目历史文件对象化上传灰度`

`M3G-3：多真实项目分批对象化策略与任务中心增强`

`M3G-4：受控多项目小批对象化执行`

`M3G-5：文件管理器项目全局搜索与存储展示修复`

当前子批次：

`DOC-BASE：文档基线收口`

M3G 后续任务图：

- [x] M3G-6：105 项目全量对象化试运行。
- [x] M3G-6R：105 对象化长跑执行与进度控制。
- [x] M3G-6S：105 对象化持续跑批至治理边界。
- [x] M3G-6S-F1：105 历史 active object version 与当前 NAS 侧 MinIO 对齐修复。
- [x] M3G-6T：对象化后文件业务视图与工程树交付映射。
- [x] M3G-7：多真实项目对象化 Wave 1。
- [x] M3G-7R：全项目对象化扩大跑批。
- [x] M3G-8-F1：M3G-7R 回归脚本饱和环境修复。
- [x] M3G-8：对象优先读取与 NAS fallback 收口。
- [x] M3G-9：全项目对象化覆盖率报告与 M3 收口依据。
- [x] DOC-BASE：更新 PRD、架构设计、路线图、API 契约与后续任务图。
- [ ] M3-CLOSE：基于文档基线、测试报告和任务图做 M3 整体收口判断。
- [ ] M3X：全项目对象化独立批次，按覆盖率报告分批推进，不能与 M3 收口混做。

M3 收口前不得进入 Hermes 正文问答、documents/chunks 写入、向量库试点或 BIM 深度解析。

交付要求：

- M3G 只能在 M3F 正式收口后启动。
- 目标架构调整为：
  - NAS 运行 MinIO 服务，并提供 MinIO 专用对象数据区。
  - NAS 原项目资料区冻结为只读备份和回滚来源。
  - NAS 侧 MinIO 存真实文件本体，作为平台后续文件主读取入口。
  - MySQL 继续存业务台账、权限、版本、checksum、交付关系。
  - 本机 Hermes 后续通过平台授权，从 NAS 侧 MinIO 复制文件副本到本机工作区解析。
- 平台对象存储配置从本机 MinIO 切换到 NAS 侧 MinIO endpoint。
- 正式 bucket 名称必须脱离测试语义，不能继续使用 `delivery-m3a-smoke` 这类名称。
- 对当前所有已登记真实项目做对象化盘点，输出容量、文件类型、checksum 覆盖率、对象化覆盖率和失败风险。
- 按项目、目录、文件类型、文件大小、checksum 覆盖情况、最近更新时间分批对象化。
- 支持 dry-run、暂停、继续、重试、跳过、失败原因查看、断点续跑、速率限制和并发上限。
- 历史文件对象 key 必须使用 opaque 规则，例如 `objects/{assetUuid}/{objectVersion}/{checksum}`，不得包含真实业务路径、项目名、楼栋、专业目录或用户名。
- 平台读取链路优先使用 active object version；NAS_ONLY 文件继续可用但必须明确显示“未对象化”。
- 大文件下载需要支持平台审计后从 NAS 侧 MinIO 直连或等价加速方案。
- Hermes 副本取用只做契约预留，不做正文问答，不写 documents / chunks / 向量库。

验收条件：

- M3F 已正式收口。
- NAS 侧 MinIO 健康检查通过。
- 平台对象存储 endpoint 已切换到 NAS 侧 MinIO。
- 正式 bucket 已创建并可读写。
- 全项目对象化盘点报告已生成。
- 至少完成主 agent 确认的一批真实项目对象化，且方案可复用到全部真实项目。
- 每个对象化文件都有 `assetUuid`、checksum 和 active object version。
- 已对象化文件默认从 NAS 侧 MinIO 读取，并显示 `OBJECT_STORED`。
- 未镜像文件仍按 NAS 台账链路可用。
- NAS 原文件未被移动、删除、改名。
- 大文件下载支持平台审计后的 NAS 侧 MinIO 直连或等价加速方案。
- Hermes 文件副本取用契约已预留，但未写正文 evidence。
- 禁出字段扫描通过。
- M3F / M3E / M3D / M3C / file-access 回归通过。

M3G-1 交付要求：

- 提供 NAS 侧 MinIO readiness，能区分本机开发 MinIO 与 NAS 侧 MinIO。
- 提供全项目对象化覆盖率盘点。
- 提供单项目对象化 dry-run 计划。
- dry-run 不复制文件、不启动真实迁移任务。
- 页面或接口清楚提示当前是否具备正式 NAS 侧 MinIO 条件。
- 运维 / NAS 管理员配置依据：
  - `handoff/main-agent/m3g1-nas-minio-ops-preparation.md`
- M3G-1 子任务跟踪：
  - `handoff/main-agent/m3g1-task-graph.md`

M3G-1 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g1-nas-minio-readiness-inventory-closure.md`
- 专项脚本：
  - `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- 复验结果：
  - readiness：`NAS_SIDE_MINIO / READY`
  - M3G-1 专项：`PASS=9 FAIL=0`
  - M3E 回归：`PASS=8 FAIL=0`
  - M3F 回归：`PASS=11 FAIL=0`
  - M3C 回归：`PASS=9 FAIL=0`
  - file-access 回归：`PASS=18 FAIL=0`

M3G-2 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g2-105-objectification-gray-closure.md`
- 专项脚本：
  - `scripts/dev/check-m3g2-105-objectification-gray.sh`
- 完成内容：
  - 对 105 / `projectId=503` 做受控小批历史文件对象化。
  - 已对象化文件默认从 NAS 侧 MinIO 读取。
  - NAS_ONLY 文件继续按 NAS 台账链路可用。
  - 未移动、删除、重命名真实 NAS 原文件。

M3G-3 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g3-multi-project-objectification-task-center-closure.md`
- 专项脚本：
  - `scripts/dev/check-m3g3-multi-project-objectification-planning.sh`
- 完成内容：
  - 全项目对象化盘点增强，区分真实项目与测试 / 样例项目。
  - 新增多项目对象化 dry-run 计划接口。
  - dry-run 支持项目范围、真实项目过滤、总量限制、单项目限制、并发和限速策略字段。
  - dry-run 不创建真实迁移任务、不复制文件、不修改 NAS。

M3G-4 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g4-controlled-multi-project-objectification-closure.md`
- 专项脚本：
  - `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`
- 完成内容：
  - 新增多项目小批对象化执行接口。
  - 强制 `confirmed=true`、项目数、文件数和容量后端硬上限。
  - 支持受控小批真实对象化。
  - 重复执行幂等。
  - 已对象化文件可通过受控 `file-access` 读取。
  - NAS 原文件未移动、未删除、未改名、未覆盖。

M3G-5 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g5-file-manager-search-storage-display-closure.md`
- 专项脚本：
  - `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`
- 完成内容：
  - 文件管理器关键词搜索默认改为项目全局搜索。
  - 保留“仅当前文件夹及子目录”范围作为可选收窄。
  - 未搜索时继续保持当前目录 direct-only 浏览。
  - 文件表与详情清楚区分 `OBJECT_STORED` 与 `NAS_ONLY`。
  - 本批未创建对象化迁移任务，未触碰真实 NAS 原文件。

M3G-5-F1 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g5-f1-file-manager-search-query-fix-closure.md`
- 完成内容：
  - 修复带 `fileKeyword` 进入文件管理器时仍显示根目录文件夹的问题。
  - 搜索模式下右侧只显示匹配文件和所在位置。
  - 清空关键词后恢复当前目录 direct-only 浏览。

M3G-6 纠偏启动记录：

- 计划：
  - `handoff/main-agent/m3g6-105-full-objectification-trial-plan.md`
- 目标：
  - 以 105 / `projectId=503` 为第一个完整样板项目，生成全量对象化计划。
  - 按批次持续推进 105 已登记文件对象化。
  - 支持暂停、继续、重试、失败原因和治理清单。
  - 验证已对象化文件通过 NAS 侧 MinIO + 受控 `file-access` 读取。

M3G-6R 启动记录：

- 目标：
  - 在 M3G-6 机制打通基础上，补齐 105 对象化长跑执行与进度控制。
  - 支持开始、暂停、继续、失败重试和治理清单。
  - 支持批大小、容量、连续批次数和失败后继续策略。
  - 前端文件服务页展示长跑状态、覆盖率、剩余可执行项和治理项。
- 验收：
  - `scripts/dev/check-m3g6r-105-objectification-long-run.sh`
  - M3G-6 / M3G-5 / file-access 回归通过。

M3G-6R 完成记录：

- 收口记录：
  - `handoff/main-agent/m3g6r-105-objectification-long-run-closure.md`
- 完成内容：
  - 105 长跑状态可查。
  - 支持开始、暂停、继续和失败项重试入口。
  - 支持批大小、容量、单文件大小、连续批次数硬上限。
  - 前端文件服务页可查看长跑控制区。
  - 最新验收确认 105 已对象化数量从 `38` 推进到 `57`。
  - NAS 原文件未移动、删除、重命名或覆盖。
  - 已对象化文件可通过受控 `file-access` 读取。
  - 本批不代表 105 已全量对象化完成，不代表 M3 收口。

M3G-6S 启动记录：

- 目标：
  - 尽快把 105 项目的可执行文件全部对象化到 NAS 侧 MinIO。
  - 将剩余不可执行文件收敛为治理清单。
  - 为后续所有 NAS 项目对象化升级打基础。
- 要求：
  - 按小文件 / 中文件 / 大文件分层推进。
  - 不一次性无边界硬冲。
  - 不移动、删除、重命名、覆盖真实 NAS 原文件。
  - 输出最终覆盖率报告和治理项原因分组。
- 验收：
  - `scripts/dev/check-m3g6s-105-objectification-to-governance-boundary.sh`
  - M3G-6R / M3G-6 / M3G-5 / file-access 回归通过。

M3G-6S 完成记录：

- 105 / `projectId=503` 已达到 `2928 / 2928` 全量对象化。
- 未对象化数量 `0`。
- 剩余可执行数量 `0`。
- 迁移失败数量 `0`。
- 治理项数量 `0`。
- 对象化覆盖率 `100.0%`。
- checksum 覆盖率 `100.0%`。
- 已对象化文件可通过受控 `file-access` 读取。
- NAS 原文件抽样 `size / mtime` 未变化。
- 禁出字段扫描通过。

M3G-6S-F1 插入记录：

- 插入原因：
  - M3G-6T 功能开发后，M3G-6S 回归发现 105 数据库仍显示 `2928 / 2928` 已对象化，但当前 NAS 侧 MinIO 中部分 historical active object body 不可读。
  - 这会导致工程树或交付候选点击文件时出现 `ASSET_FILE_NOT_READABLE`。
- 目标：
  - 校验 active object version 对应对象实体真实存在。
  - 对缺失对象执行受控修复，从 NAS 台账原文件重新复制副本到当前 NAS 侧 MinIO。
  - 修复后受控 `file-access` 可读取对象化文件。
  - active object version 不重复污染，无法修复的文件进入治理清单。
- 禁止：
  - 不通过 `file-access` 静默 fallback 读 NAS 冒充对象读取成功。
  - 不移动、删除、重命名、覆盖真实 NAS 原文件。
  - 不读取正文，不写语义索引，不触发 Hermes。

M3G-6S-F1 完成记录：

- 105 / `projectId=503` active object version 已与当前 NAS 侧 MinIO 对象实体对齐。
- 完整性检查显示 `2928 / 2928` 可读。
- governance item 为 `0`。
- 受控 `file-access` 可读取抽样对象化文件。
- M3G-6S 回归通过。

M3G-6T 完成记录：

- 工程树草案、模型 / 图纸缺口分析、待交付候选能力已通过专项验收。
- 草案应用和交付候选应用仍要求 `confirmed=true`。
- 不自动覆盖正式工程树，不自动挂接，不自动审核，不自动整改。
- 不读取正文，不写语义索引，不触发 Hermes。

M3G-6T 建议插入：

- 目标：把 105 对象化成果转为员工能理解和使用的业务视图。
- 文件管理默认展示文件名、所属工程节点、资料用途、交付状态、专业、版本、大小和打开/预览操作。
- 技术字段如内部文件 ID、checksum、对象版本、迁移状态进入“技术信息 / 诊断信息”折叠区。
- 工程树节点展示文件数、模型数、图纸数、正式交付数、待确认数和缺失数。
- 从文件可反查所属工程节点；从工程节点可反查节点下文件和交付候选。
- 生成“待交付候选文件 / 待挂接应交项”，用户确认后仍走现有批量挂接能力。
- 不读取正文，不写语义索引，不触发 Hermes 正文问答。

M3G-6T 启动记录：

- 目标：
  - 基于 105 已对象化文件和当前文件夹树生成工程树优化草案。
  - 明确显示哪些节点有模型有图纸、哪些有图纸缺模型、哪些有模型缺图纸。
  - 将工程树、文件归属、文档 / 图纸交付、交付包预检查关联起来。
  - 让员工能从工程树看项目结构，从缺失项找到候选文件，从文件反查所属工程节点。
- 要求：
  - 草案不得自动覆盖正式工程树。
  - 所有应用和挂接动作必须要求 `confirmed=true`。
  - 不读取正文，不写语义索引，不触发 Hermes 正文问答。
- 验收：
  - `scripts/dev/check-m3g6t-105-engineering-tree-delivery-mapping.sh`
  - M3G-6S / M2J / M2I / M2H / M2F / file-access 回归通过。

M3G-7 计划：

- 目标：从 105 的方法复制到更多真实项目，形成第一批真实项目对象化 Wave 1。
- 范围：优先选择目录清晰、路径可读、风险低的真实项目；95 / 98 / 99 等不标准治理项目暂不纳入，除非用户单独指定。
- 验收：至少一批非 105 真实项目完成对象化，输出覆盖率报告和失败治理清单。

M3G-7 启动记录：

- 用户确认进入下一步。
- 当前批次只做多真实项目 Wave 1，不做全量项目硬迁移。
- 默认排除 105、95、98、99、样例 / 测试 / 归档 / 未完成接入项目。
- 必须先生成候选清单和 dry-run，再执行小批对象化。
- 执行接口必须要求 `confirmed=true`，并受项目数量、文件数量、容量、单文件大小硬上限约束。
- 不移动、删除、重命名、覆盖真实 NAS 原文件。
- 不读取正文，不写语义索引，不触发 Hermes。

M3G-7 完成记录：

- 非 105 项目 `507 / 96` 已完成 5 个小文件对象化。
- 对象化文件均为 `OBJECT_STORED`。
- 受控 `file-access` 可读。
- 重复执行幂等跳过，未重复污染 active object version。
- NAS 原文件抽样 `size / mtime` 未变化。
- 105 / 95 / 98 / 99 未进入可执行候选。
- dry-run 不写入，`confirmed=false` 被拒绝。
- 禁出字段扫描通过。

M3G-7R 启动记录：

- 用户明确要求尽快将全项目文件对象化。
- 本批从 M3G-7 的 5 文件小批验证，升级为全项目对象化持续跑批。
- 目标是推进所有合格真实项目对象化；无法安全执行的项目进入治理清单。
- 仍必须保留暂停、继续、失败重试、硬上限、禁出字段扫描和 NAS 原文件保护。
- 本批不读取正文，不写语义索引，不触发 Hermes。

M3G-7R 收口记录：

- M3G-7R 专项脚本通过，`PASS=48 FAIL=0`。
- 全项目 overview 可查，当前覆盖率约 `7.22%`。
- 本轮实际推进 2 个非 105 真实项目：`projectId=519`、`projectId=520`。
- 6 个样本文件均为 `OBJECT_STORED`，受控 `file-access` 可读。
- 105 保持已完成，95 / 98 / 99 未进入可执行队列。
- NAS 原文件抽样 `size / mtime` 未变化。
- M3G-7、M3G-6S-F1、M3G-6T、file-access 回归均通过。
- 后续已进入并收口 `M3G-8：对象优先读取与 NAS fallback 收口`。

M3G-8 计划：

- 目标：收口对象优先读取与 NAS fallback。
- 已对象化文件默认走 NAS 侧 MinIO active object version。
- NAS_ONLY 文件保持可用，但必须明确说明“历史 NAS 链路 / 尚未对象化”。
- 对象读取失败不得静默伪装成功，fallback 必须可配置、可审计、可提示。
- 大文件下载应支持平台审计后的 NAS 侧 MinIO 直连或等价加速方案。

M3G-8 / M3G-8-F1 收口记录：

- M3G-8 主专项通过，`OBJECT_STORED` 默认对象存储读取、`NAS_ONLY` 历史 NAS 链路读取、对象不可读不静默 fallback 均成立。
- M3G-8-F1 已修复 M3G-7R 回归脚本在样本不足 / 对象化接近饱和环境下的误失败。
- 样本充足时脚本仍验证真实小批推进；样本不足时验证 dry-run 不写入、`confirmed=false` 拒绝、已有对象可读、NAS 原文件不变和禁出字段不泄露。
- M3G-6S-F1、M3G-6T 和 Phase2 file-access 回归通过。
- 主 agent 已裁决 `M3G-8` 和 `M3G-8-F1` 正式收口。

M3G-9 计划：

- 目标：形成全项目对象化覆盖率总报表并判断 M3 是否可收口。
- 报表应覆盖：项目、总文件数、已对象化、未对象化、失败、跳过、容量、checksum 覆盖率、失败原因。
- M3 收口条件详见：
  - `handoff/main-agent/m3-m5-storage-evidence-task-graph.md`

## 9. 阶段 5：documents / chunks 语义契约

状态：`待启动`

建议批次名：`M4A：documents / chunks 语义证据契约`

交付要求：

- 先设计 documents / chunks 契约，不直接写 Hermes memory。
- Chunk 必须绑定：
  - `projectId`
  - `assetUuid`
  - `fileId`
  - `objectVersion`
  - `pageNo` / 图号 / 定位
  - `sectionNodeId`
  - `deliverableTypeId`
  - `permissionScope`
  - `evidenceHash`
- catalog-only 元数据和正文 evidence 必须分开。
- PDF / Office 可先试点正文抽取。
- DWG / RVT / BIM 深度解析后置。

验收条件：

- 语义契约文档和数据结构冻结。
- Missing Evidence 口径清楚。
- 不写 Hermes memory。
- 不把 catalog-only 当正文 evidence。

## 10. 阶段 6：向量库与关键词索引试点

状态：`待启动`

建议批次名：`M4B：向量库与关键词索引试点`

交付要求：

- 选择 Qdrant 或等价向量库做语义试点。
- OpenSearch / 关键词索引用于文件名、图号、专业、阶段、楼栋等结构化检索。
- 所有索引写入必须带项目权限范围和 evidence hash。
- 删除、隔离、权限变化时，索引必须可失效或重建。
- 未通过权限过滤的 chunk 不得返回 Hermes。

验收条件：

- 小样本 chunk 可索引。
- 权限过滤生效。
- 索引失效 / 重建策略可验证。
- 不暴露未授权 evidence。

## 11. 阶段 7：Hermes 受控 Evidence API

状态：`待启动`

建议批次名：`M5A：Hermes 受控 Evidence API`

交付要求：

- 平台后端建立 evidence API。
- Hermes 只调用 evidence API，不直连底层系统。
- 返回内容包括：
  - 脱敏证据
  - 文件名
  - 平台资产 UUID
  - 页码 / 图号 / 节点
  - 置信度
  - 证据类型
- 无证据返回“缺少证据”。
- 权限不足返回“权限拒绝”。
- 不得编造。
- 前端 Hermes 面板展示回答依据和证据边界。

验收条件：

- Hermes 不能绕过平台 Gateway。
- evidence API 权限过滤通过。
- 缺证据和权限拒绝口径明确。
- API 不返回底层路径、object key、raw row、SQL。

## 12. 阶段 8：工程主数据与交付治理 Agent 化

状态：`待启动`

建议批次名：`M5B：工程主数据与交付治理 Agent 化`

交付要求：

- Agent 基于目录线索、文件名、业务元数据、授权语义证据生成工程主数据草案。
- 草案必须人工审批后落库。
- Agent 可推荐：
  - 文件挂接
  - 缺失资料
  - 交付进度
  - 整改证据
- Agent 不得自动审批、自动整改、自动删除。
- 交付工作中心只消费已授权证据和已确认主数据。

验收条件：

- Agent 推荐有 evidence。
- 草案人工确认后才能落库。
- 不自动执行危险动作。
- 交付治理仍可追溯。

## 13. 统一验收红线

每阶段都必须满足：

- 真实 NAS 文件未被移动、删除、改名。
- API / 前端禁出字段扫描通过。
- 对象存储迁移可创建、可重试、可审计、幂等。
- 已迁移文件显示 `OBJECT_STORED`，并可通过受控 `file-access` 读取。
- 未迁移文件仍按 NAS 台账链路可用。
- Hermes 回答必须有 evidence 或明确缺证据 / 权限拒绝。
- 每阶段专项脚本、回归脚本、健康检查、禁出字段扫描通过。

## 14. 当前完成标记

已完成并收口：

- [x] M3A：对象存储与 StorageService 基线
- [x] M3B：105 小样本对象存储镜像迁移
- [x] M3C-0：资产存储与证据链契约冻结
- [x] M3C-1：资产 UUID 与存储状态统一
- [x] M3C：对象存储迁移任务中心与批量策略
- [x] M3D：真实 NAS 小范围灰度镜像
- [x] M3E：预览与转换产物对象化
- [x] M3F：新文件对象存储优先写入
- [x] M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划

已完成并收口：

- [x] M3G-2：105 项目历史文件对象化上传灰度
- [x] M3G-3：多真实项目分批对象化策略与任务中心增强
- [x] M3G-4：受控多项目小批对象化执行
- [x] M3G-5：文件管理器项目全局搜索与存储展示修复

已完成并收口：

- [x] M3G-6：105 项目全量对象化试运行
- [x] M3G-6R：105 对象化长跑执行与进度控制
- [x] M3G-6S：105 对象化持续跑批至治理边界
- [x] M3G-6S-F1：105 历史 active object version 与当前 NAS 侧 MinIO 对齐修复
- [x] M3G-6T：对象化后文件业务视图与工程树交付映射
- [x] M3G-7：多真实项目对象化 Wave 1
- [x] M3G-7R：全项目对象化扩大跑批
- [x] M3G-8：对象优先读取与 NAS fallback 收口
- [x] M3G-9：全项目对象化覆盖率报告与 M3 收口依据

下一步候选：

- [ ] DOC-BASE：文档基线收口
- [ ] M3-CLOSE：M3 整体收口判断
- [ ] M3X：全项目对象化独立批次
- [ ] M4A：documents / chunks 语义证据契约

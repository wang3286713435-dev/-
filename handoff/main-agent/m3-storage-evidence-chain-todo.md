# M3-M5：对象存储与 Hermes 语义证据链任务图

更新时间：2026-05-26

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

- [x] 0. 契约与边界冻结
- [x] 1. 平台资产 UUID 与文件状态统一（M3C-1 已收口）
- [x] 2. 对象存储迁移任务中心（M3C 已收口）
- [ ] 3. 真实 NAS 小范围灰度镜像
- [ ] 4. 预览与转换产物对象化
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

状态：`待启动`

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

- 真实业务文件只读镜像成功。
- NAS 原文件未被改动。
- PDF / DWG / RVT 小样本状态可查。
- file-access 受控访问不回归。
- 禁出字段扫描通过。

## 6. 阶段 4：预览与转换产物对象化

状态：`待启动`

建议批次名：`M3E：预览与转换产物对象化`

交付要求：

- PDF 预览、图片缩略图、Office 转换产物、CAD 转换产物、BIM 轻量化产物都应进入对象存储。
- 建立原件、衍生产物、预览产物之间的版本关系。
- DWG / RVT 可先显示“需转换”，不阻塞主链路。
- 预览 URL 必须受权限控制、可过期、不暴露 object key。

验收条件：

- 衍生产物与源文件关系可查。
- 预览产物走受控 `file-access` 或受控 preview ticket。
- 不暴露底层对象定位。
- DWG / RVT 未转换时能安全展示 Missing Conversion。

## 7. 阶段 5：documents / chunks 语义契约

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

## 8. 阶段 6：向量库与关键词索引试点

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

## 9. 阶段 7：Hermes 受控 Evidence API

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

## 10. 阶段 8：工程主数据与交付治理 Agent 化

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

## 11. 统一验收红线

每阶段都必须满足：

- 真实 NAS 文件未被移动、删除、改名。
- API / 前端禁出字段扫描通过。
- 对象存储迁移可创建、可重试、可审计、幂等。
- 已迁移文件显示 `OBJECT_STORED`，并可通过受控 `file-access` 读取。
- 未迁移文件仍按 NAS 台账链路可用。
- Hermes 回答必须有 evidence 或明确缺证据 / 权限拒绝。
- 每阶段专项脚本、回归脚本、健康检查、禁出字段扫描通过。

## 12. 当前完成标记

已完成并收口：

- [x] M3A：对象存储与 StorageService 基线
- [x] M3B：105 小样本对象存储镜像迁移
- [x] M3C-0：资产存储与证据链契约冻结
- [x] M3C-1：资产 UUID 与存储状态统一
- [x] M3C：对象存储迁移任务中心与批量策略

下一步候选：

- [ ] M3D：真实 NAS 小范围灰度镜像

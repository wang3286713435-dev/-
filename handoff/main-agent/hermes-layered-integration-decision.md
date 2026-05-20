# Hermes 分层接入裁决：Catalog -> Evidence -> Memory -> Orchestration

更新时间：2026-05-20

## 1. 已读取共享文档

本轮已读取共享文档空间：

- `DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
- `DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
- `DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
- `DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`
- `DigitalDeliveryProject/docs/01_capability_matrix.md`
- `DigitalDeliveryProject/integration-contracts/feedback_contract.md`
- `DigitalDeliveryProject/adr/ADR-002-no-nas-data-in-hermes-memory.md`

## 2. 总体裁决

Hermes 不是普通聊天框，也不是 SQL / DB / NAS 工具。平台侧应把 Hermes 理解为：

`Evidence-first enterprise memory kernel + permission-aware catalog agent`

中文产品口径：

`证据先行，权限闭环；让企业数据可问、可信、可控。`

工程接入口径：

`先目录，后正文；先证据，后智能；先受控，后自动化。`

## 3. 四层能力边界

### Layer 1：Catalog Layer

当前平台可用，也是 G2-B 可以继续强化的层。

能力：

- 只读资产目录查询。
- 基于 `ProjectAssetView / FileAssetView / ModelAssetView / AuditEventView` 或平台 Gateway 返回目录级信息。
- 返回 `file_id / model_id / project_id / source_view`。
- 返回 `query_id / trace_id` 供审计和反馈。
- 返回 `asset_catalog_only=true`。
- 由 Platform Gateway 生成或校验 `project_scope / permission_decision`。
- 返回 `display_path / path_hint`，不得返回 raw path。
- 对目录级问题做自然语言解释。
- 对正文、DWG/RVT、BIM 构件问题返回 Missing Evidence。

G2-B 当前允许做：

- Hermes 常驻入口。
- 当前页面 / 当前项目上下文注入。
- 当前项目目录级问答。
- 页面用途、下一步动作、治理路径解释。
- Missing Evidence 解释。
- 低敏 `query_id / trace_id / related_file_ids` 的展示或后续反馈预留。

### Layer 2：Evidence Layer

下一阶段，不属于 G2-B current 能力。

前置条件：

- 文件已通过受控 ingestion / parser / evidence write。
- 有稳定 `file_id / document_id / version_id / citation metadata`。
- 有权限证明。
- 可返回 chunk / sheet / slide / paragraph 级 citation。
- 不暴露 raw storage path。

后续建议能力名：

`document_evidence_search`

禁止在 G2-B 中伪实现：

- PDF / Office 正文问答。
- DWG 图层、图框、外参、块属性问答。
- RVT Level / Grid / Sheet / View / Family / Type 问答。
- BIM 构件参数问答。

### Layer 3：Memory Layer

Evidence 边界明确后再产品化。当前只能做低敏连续性规划，不把内容写入 memory。

允许的低敏记忆对象：

- `related_file_ids`
- `related_model_ids`
- `query_id`
- `trace_id`
- 用户反馈标签：`relevant / not_relevant / needs_review`
- 低敏偏好：常用项目、常看专业、偏好展示字段

禁止：

- raw `storage_path`
- raw catalog row
- NAS 原始路径
- DWG/RVT 内容
- PDF/Office 正文
- 客户敏感材料
- secret / token / password / API key

正确解释：

`related_file_ids` 只表示文件曾在受控交互中被引用，不代表 Hermes 已读取、解析、索引或长期记住该文件正文。

### Layer 4：Orchestration Layer

后续阶段，不属于 G2-B。

目标：

- 调度子 Agent。
- 发起人工复核。
- 生成缺证据任务。
- 追踪治理状态。

前置要求：

- 审批流成熟。
- 审计流成熟。
- 任务状态成熟。
- 权限模型成熟。

G2-B 严禁做 Agent 自动执行治理动作。

## 4. G2-B 接入边界

G2-B 仍属于 Catalog Layer 强化，不进入 Evidence / Memory / Orchestration。

G2-B 可以做：

- Hermes 常驻浮窗 / 全局入口。
- 当前项目、当前页面上下文注入。
- 目录级资产问答。
- 页面用途和下一步动作解释。
- 105 和其他真实项目的治理路径解释。
- Missing Evidence 说明。
- 低敏反馈 / memory continuity 的 UI 预留，但不能写入 Hermes long-term memory，除非后端已有符合 feedback contract 的低敏接口。

G2-B 不做：

- `document_evidence_search`。
- 文件正文检索。
- DWG/RVT/BIM 内容理解。
- NAS semantic collection。
- Hermes memory 写入正文或 raw catalog。
- 多 Agent 编排。
- 自动审批 / 自动整改 / 自动挂接。

## 5. 后续接入路线

G2-B 收口后，应按以下路线继续：

1. `Catalog Layer 收口`
   - 常驻入口。
   - 项目 / 页面上下文。
   - 目录级问答。
   - Missing Evidence。
   - feedback 预留。

2. `Evidence Availability Contract`
   - 增加或明确 `catalog_only / full_text_available / evidence_indexed / parser_required / unsupported_type / permission_denied / manual_review_required`。
   - 不代表已可正文问答，只是判断证据可用性。

3. `Evidence Search Gateway`
   - 只读 `document_evidence_search`。
   - 仅对已治理、已授权、已索引文件开放。
   - 返回 citation。

4. `Memory Continuity`
   - 低敏 `related_file_ids / related_model_ids / query_id / trace_id / feedback labels`。
   - 不写正文、不写 raw path、不写 raw row。

5. `Orchestration`
   - 人工复核任务。
   - 缺证据任务。
   - 后续受控治理动作。

## 6. 必须同步维护的共享文档

如后续平台新增或修改下列能力，需要同步共享文档：

- Gateway response 字段。
- capability matrix。
- Missing Evidence reason。
- feedback contract。
- evidence availability 字段。
- platform_to_hermes_contract。
- standard_to_platform_mapping。

当前本轮只更新 handoff，不修改共享文档。

# Hermes 纠偏 0A：原生 Endpoint 契约冻结与字段对齐报告

更新时间：2026-05-20

## 1. 分支与目标

当前 Hermes 专项分支：

`codex/hermes-alignment-0a-contract-freeze`

本轮目标：

- 暂停 runtime 接入扩展。
- 不再把 OpenAI-compatible `/v1/chat/completions` 当作 Hermes 企业 Agent 内核正式接入口。
- 通过共享文档冻结 `session_id / thread_id / previous_response_id / sanitized_context_refs / safe_memory_candidates / response_id` 的字段语义。
- 等 Hermes 侧明确原生 endpoint 路径、请求 schema、响应 schema 后，再进入平台 runtime 开发。

## 2. 已读取共享文档

- `agent-briefings/hermes_capability_handoff.md`
- `integration-contracts/hermes_kernel_authority_contract.md`
- `integration-contracts/platform_to_hermes_contract.md`
- `integration-contracts/gateway_response_contract.md`
- `integration-contracts/catalog_tool_contract.md`
- `integration-contracts/missing_evidence_policy.md`
- `docs/01_capability_matrix.md`

## 3. 契约裁决

已新增共享契约：

`integration-contracts/hermes_native_gateway_contract.md`

已同步补充共享契约引用：

- `integration-contracts/platform_to_hermes_contract.md`
- `integration-contracts/gateway_response_contract.md`
- `docs/01_capability_matrix.md`

注意：共享文档仓库在本轮开始前已有大量其他会话未提交改动。本轮只修改上述 Hermes 契约相关文件，不回滚、不整理其他共享文档改动。

核心裁决：

- OpenAI-compatible 仅保留为临时 smoke / catalog-only 问答通道。
- OpenAI-compatible 不满足 architecture aligned。
- Hermes native endpoint 是正式连续会话接入前置条件。
- Platform Gateway 继续负责权限、项目范围、路径脱敏、forbidden-field scan 和审计。
- Hermes 负责 session continuity、reasoning state、tool orchestration、Missing Evidence、低敏 memory continuity 和 response trace。

## 4. 字段语义

- `session_id`：平台生成或认可的安全会话引用，不包含隐私、token、路径、正文。
- `thread_id`：Hermes 侧连续对话 / 任务线程引用，Platform 只保存和回传引用。
- `previous_response_id`：上一轮 Hermes 响应引用；项目或权限变化后必须失效或重校验。
- `sanitized_context_refs`：低敏上下文引用，只允许 project/file/model/query/trace/source/page refs。
- `safe_memory_candidates`：Hermes 可建议记忆的低敏候选，只允许 related ids、反馈标签、低敏偏好。
- `response_id`：Hermes 返回的安全响应引用，前端只显示为诊断编号。

## 5. 当前不做

本轮未修改平台 runtime 代码。

本轮不做：

- Agent DB CRUD。
- Agent SQL。
- raw NAS path 透传。
- raw row 透传。
- NAS scan。
- DWG/RVT 内容理解承诺。
- 写 Hermes memory 正文。
- 写 documents/chunks/OpenSearch/Qdrant/MinIO。
- 假造 Hermes native endpoint。

## 6. 下一步

等待 Hermes 侧补充：

1. Hermes native endpoint 路径。
2. Platform -> Hermes native request schema。
3. Hermes -> Platform native response schema。
4. `session_id / thread_id / previous_response_id` 的生成与生命周期规则。
5. `safe_memory_candidates` 是否由 Hermes 生成、Platform 审核后回传，还是先只展示不写入。

在上述信息明确前，平台侧只允许保留 OpenAI-compatible smoke，不进入正式连续会话开发。

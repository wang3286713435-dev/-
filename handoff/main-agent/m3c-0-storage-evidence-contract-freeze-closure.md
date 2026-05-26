# M3C-0 收口：资产存储与证据链契约冻结

日期：2026-05-26

## 结论

`M3C-0：资产存储与证据链契约冻结` 已完成。

本批只做契约冻结和交接同步，不修改业务代码、不改数据库、不进入 Hermes 正文问答、不启动向量库、不做全量 NAS 迁移。

## 已同步的共享文档

- `DigitalDeliveryProject/integration-contracts/asset_storage_evidence_chain_contract.md`
- `DigitalDeliveryProject/adr/ADR-007-object-storage-evidence-chain-boundary.md`
- `DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
- `DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
- `DigitalDeliveryProject/docs/01_capability_matrix.md`

## 已同步的本仓库 handoff

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/development-log.md`

## 冻结边界

- NAS 原文件仍是源头，不移动、不删除、不改名。
- 对象存储负责文件本体镜像、预览产物、转换产物和未来交付归档。
- MySQL 仍是项目、资产、权限、交付、任务和审计中心。
- documents / chunks 才能作为正文 evidence 来源。
- catalog metadata 不能伪装成正文 evidence。
- `OBJECT_STORED` 只表示对象存储镜像已存在，不能解释为 semantic index 已完成。
- Hermes 不直连 MySQL、NAS、MinIO/S3、Qdrant、OpenSearch。
- 前端 / API / Hermes 响应不得返回 raw NAS path、bucket、object key、`storage_uri`、raw row、SQL、token、secret。

## 下一步建议

优先进入：

1. `M3C-1：资产 UUID 与存储状态统一`
2. `M3C：对象存储迁移任务中心与批量策略`

不要跳过上述批次直接进入 Hermes 正文问答。

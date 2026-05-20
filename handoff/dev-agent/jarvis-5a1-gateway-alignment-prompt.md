# 开发 Agent 备用任务：批次 5A.1 贾维斯 Gateway 合同对齐与联调增强

你是数字化交付平台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本任务只有在主 agent 明确派发时执行。不要自行启动。

## 0. 背景

`批次 5A：贾维斯数据管家内嵌 v0` 已通过测试 agent 验收并收口。

用户提供的 V3 文档：

`/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`

确认 Hermes 当前可做前端嵌入、平台 Gateway、只读 catalog lookup、health、权限上下文传递和审计 trace 对齐，但仍不授权写入、索引、正文 answer integration 或 production rollout。

## 1. Ralph Loop 要求

- 使用 Pro 代码模型。
- 不要创建子 agent、Task、Explore 或其他独立上下文。
- 必须使用 Ralph Loop skill。
- 推荐：
  - `/ralph-loop:ralph-loop "批次 5A.1 贾维斯 Gateway 合同对齐与联调增强" --completion-promise "PHASE2_BATCH5A1_JARVIS_GATEWAY_ALIGNMENT_COMPLETE" --max-iterations 6`
- 完成承诺固定为：
  - `<promise>PHASE2_BATCH5A1_JARVIS_GATEWAY_ALIGNMENT_COMPLETE</promise>`

## 2. 必须先阅读

1. `handoff/main-agent/jarvis-v3-gateway-alignment-plan.md`
2. `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`
3. `handoff/main-agent/status.md`
4. `handoff/main-agent/decisions.md`
5. `handoff/dev-agent/latest-report.md`
6. `handoff/test-agent/latest-report.md`
7. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/*`
8. `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
9. `scripts/dev/check-hermes-jarvis-gateway.sh`

## 3. 本批目标

只做 Gateway 合同增强：

1. 新增平台侧 Hermes health 接口。
2. 可选增加平台语义 chat 别名。
3. 可选增加只读 catalog search 壳。
4. 前端展示贾维斯健康状态、当前模式、contract version 和 runtime flags。
5. 审计中关联 platform trace 与 Hermes trace 或 unavailable reason。
6. 脚本覆盖 health、chat、catalog-only、unavailable、审计和 4R 回归。

## 4. 建议接口

优先实现：

```http
GET /api/data-steward/hermes/health
```

返回建议：

```json
{
  "status": "ok",
  "hermesAvailable": true,
  "mode": "read_only_gateway",
  "contractVersion": "delivery_platform.asset_views.v1.1",
  "runtimeWriteEnabled": false,
  "agentAnswerIntegrationEnabled": false
}
```

可选实现：

```http
POST /api/data-steward/chat
POST /api/data-steward/catalog/search
```

`catalog/search` 只能返回用户有权看到的 asset metadata preview，不返回 raw DB row，不返回真实 NAS path，不把 metadata 当正文 evidence。

## 5. 明确禁止

本批严禁：

1. Agent DB CRUD。
2. Agent NAS CRUD。
3. 自动扫描 NAS。
4. 自动解析大批文件。
5. 自动写 Hermes `documents / chunks`。
6. 自动写 OpenSearch / Qdrant / MinIO。
7. 自动 selective indexing。
8. 正文 evidence 回答进入生产前端。
9. production rollout。
10. 真实 NAS 路径泄露。

## 6. 验收标准

1. health 接口可用，Hermes 不可用时也不 500。
2. health 不返回内部路径、secret、`.env`、DB 连接串。
3. 前端能展示贾维斯健康状态和只读运行模式。
4. chat 仍保持 catalog-only / Missing Evidence / denied / unavailable。
5. 只读 catalog search 如实现，必须权限过滤并隐藏真实路径。
6. 审计 trace 可关联 platform request 与 Hermes trace 或 unavailable reason。
7. `scripts/dev/check-hermes-jarvis-gateway.sh` 更新并通过。
8. `scripts/dev/check-phase2-batch4-file-access.sh` 回归通过。
9. 前后端构建通过。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须写清：

1. 5A.1 实现了哪些 V3 合同点。
2. health 接口和不可用兜底如何工作。
3. 是否实现 chat 别名和 catalog search。
4. 前端如何展示健康状态与 runtime flags。
5. 审计 trace 如何关联。
6. 哪些 V3 能力仍未开放。
7. 验证结果。

末尾必须包含：

`<promise>PHASE2_BATCH5A1_JARVIS_GATEWAY_ALIGNMENT_COMPLETE</promise>`

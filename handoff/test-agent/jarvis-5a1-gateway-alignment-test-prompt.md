# 测试 Agent 备用任务：批次 5A.1 贾维斯 Gateway 合同对齐专项复验

你是数字化交付平台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本任务仅在主 agent 明确派发 5A.1 后执行。

## 0. 必须先阅读

1. `handoff/main-agent/jarvis-v3-gateway-alignment-plan.md`
2. `handoff/dev-agent/latest-report.md`
3. `scripts/dev/check-hermes-jarvis-gateway.sh`
4. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/*`
5. `frontend/src/modules/data-steward/components/DataStewardPanel.vue`

## 1. 验收目标

确认 V3 Gateway 合同增强成立：

1. Hermes health 接口可用。
2. Hermes 不可用时 health 和 chat 都安全返回，不 500。
3. 前端展示贾维斯健康状态和只读运行模式。
4. chat 仍保持 catalog-only、Missing Evidence、权限拒绝和人工审批草案。
5. 如实现 catalog search，只能返回权限过滤后的 metadata preview。
6. 审计 trace 可关联 platform trace 与 Hermes trace 或 unavailable reason。
7. 4R 文件访问安全不回归。

## 2. 必做验证

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
```

如使用临时端口，报告里写清实际端口。

## 3. Health 验收

检查：

```http
GET /api/data-steward/hermes/health
```

必须确认：

1. 返回不包含内部路径、secret、`.env`、DB 连接串。
2. `runtimeWriteEnabled=false`。
3. `agentAnswerIntegrationEnabled=false`。
4. Hermes 不可用时返回清晰不可用状态，不 500。

## 4. Catalog Search 验收

如果开发实现了：

```http
POST /api/data-steward/catalog/search
```

必须验证：

1. 有权限项目可返回 metadata preview。
2. 无权限项目 fail closed。
3. 不返回 raw DB row。
4. 不返回真实 NAS path。
5. 不把 metadata 标记为正文 evidence。

## 5. P0

任一出现即 P0：

1. health 或 chat 泄露 secret、DB 连接串或真实 NAS 路径。
2. Agent 绕过 project_scope。
3. metadata 被展示为正文 evidence。
4. Agent 触发写库、写索引、NAS 操作或 production rollout。
5. 4R 路径隐藏回归失败。

## 6. 报告

写入：

`handoff/test-agent/latest-report.md`

报告包含：

1. 总结论。
2. P0/P1/P2。
3. 构建和脚本结果。
4. health 验证。
5. unavailable 验证。
6. catalog search 验证（如实现）。
7. trace / audit 验证。
8. 4R 回归。
9. 是否可以收口 5A.1。

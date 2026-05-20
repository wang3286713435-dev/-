# 贾维斯 V3 Gateway 合同对齐计划

更新时间：2026-05-18

## 1. 来源

本计划基于：

- `/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/hermes-jarvis-coupling-roadmap.md`

## 2. 当前裁决

`批次 5A：贾维斯数据管家内嵌 v0` 已通过测试 agent 专项复验，可以正式收口。

5A 已完成：

- 前端用户侧名称：`问贾维斯 / 贾维斯数据管家`
- 后端 Gateway：`GET /api/agent/hermes/capabilities`、`POST /api/agent/hermes/chat`
- catalog-only / read-only / permission-aware
- Missing Evidence
- operation plan draft
- Hermes 不可用 `agent_unavailable`
- 平台侧 `agent.jarvis.chat.*` 审计
- 4R 文件访问安全闭环回归通过

## 3. V3 对当前边界的影响

V3 文档确认 Hermes 已更新到：

`phase-2.89-test-machine-runtime-preflight-handoff-baseline`

这允许平台继续推进：

- 前端内嵌数据管家入口
- 平台后端 Gateway
- Hermes health
- 只读 chat / catalog lookup
- 权限上下文传递
- 审计 trace 对齐
- asset catalog metadata preview

但仍不授权：

- Agent DB CRUD
- Agent NAS CRUD
- 自动扫描 / 移动 / 删除 / 覆盖 NAS 文件
- 自动写 Hermes `documents / chunks`
- 自动写 OpenSearch / Qdrant / MinIO
- 自动 selective indexing
- 正文 answer integration 进入生产前端
- production rollout

## 4. 5A 与 V3 差距

当前 5A 已满足 V3 的安全主线，但尚未完整补齐 V3 推荐的联调便利能力：

| 能力 | 5A 状态 | 建议 |
| --- | --- | --- |
| 前端贾维斯入口 | 已完成 | 收口 |
| 后端 Gateway | 已完成 | 继续沿用 |
| capabilities | 已完成 | 收口 |
| chat catalog-only | 已完成 | 收口 |
| Missing Evidence | 已完成 | 收口 |
| permission proof / fail closed | 已完成 | 收口 |
| 平台侧审计 | 已完成 | 后续补 trace 关联 |
| Hermes health | 未单独开放 | 5A.1 补齐 |
| `/api/data-steward/chat` 平台语义别名 | 未开放 | 5A.1 可选 |
| catalog search 壳 | 未开放 | 5A.1 或 5B 补齐 |
| platform trace / hermes trace 关联 | 部分具备 requestId | 5A.1 补齐 |
| feature flag 状态展示 | 前端展示能力状态，未接运行时 flag | 5A.1 补齐 |

## 5. 推荐下一批：5A.1

批次名称：

`贾维斯 Gateway 合同对齐与联调增强`

### 5A.1 范围

后端：

- 新增 `GET /api/data-steward/hermes/health`
- 可选新增 `POST /api/data-steward/chat`，作为现有 `/api/agent/hermes/chat` 的平台语义别名
- 可选新增 `POST /api/data-steward/catalog/search`，只返回权限过滤后的 asset catalog metadata preview
- Hermes health 返回：
  - availability
  - mode
  - contract version
  - runtime write enabled=false
  - agent answer integration enabled=false
- 审计补充：
  - platform trace id
  - hermes trace id 或 unavailable reason
  - health check 不记录 secret/path/raw row

前端：

- 贾维斯抽屉展示 Hermes health 状态
- 展示当前只读模式和 runtime flags
- 对 catalog preview / Missing Evidence / Permission Denied / Human Review 做更清晰的标签
- 不新增自动执行按钮

脚本：

- 新增或扩展 `scripts/dev/check-hermes-jarvis-gateway.sh`
- 覆盖 health、chat、catalog search、unavailable、审计、4R 回归

### 5A.1 禁止

- 不读文件正文
- 不写 Hermes evidence tables
- 不写索引
- 不触发 NAS 扫描、移动、删除、改名、复制
- 不做 Agent CRUD
- 不做 production rollout

## 6. 与 5B 的取舍

如果当前目标是“尽快让客户看到完整数据管家业务页面”，优先进入 5B。

如果当前目标是“把贾维斯作为亮点继续打磨”，先做 5A.1。

主 agent 建议：

1. 先用 5A.1 做一个很薄的 Gateway 合同增强，时间控制在小批次。
2. 不在 5A.1 中开发复杂搜索体验。
3. 完成 5A.1 后进入 5B，补齐数据管家的客户版模块。

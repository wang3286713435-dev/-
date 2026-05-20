# Hermes 只读前端网关脱敏接入评审报告

日期：2026-05-18

来源：

- `/Users/vc/Downloads/DB_TEAM_HERMES_FRONTEND_GATEWAY_INTEGRATION_V3.md`
- `/Users/vc/Downloads/database_platform_agent_risk_notes.md`
- `handoff/main-agent/database-platform-agent-risk-notes-review.md`
- 当前平台代码与 handoff 状态

## 1. 评审结论

当前可以进入 `Hermes 只读前端网关接入阶段`。

Go 范围仅限：

- 平台后端 Gateway。
- 前端内嵌 Hermes 面板。
- Hermes health check。
- 只读资产目录 catalog lookup / metadata preview。
- project_scope / permission proof / audit trace 对齐。
- Missing Evidence、权限拒绝、catalog-only 状态展示。

继续禁止：

- Agent DB CRUD。
- Agent 生成 SQL。
- Agent 扫描 NAS。
- Agent 读取 DWG / RVT 正文。
- Agent 返回真实 `storage_path / storage_uri`。
- 写 `documents / chunks / OpenSearch / Qdrant / MinIO`。
- Hermes memory 混入 NAS 内容索引。
- 前端直连 Hermes。

用户可见名称必须统一为 `Hermes`。当前前端仍存在 `问贾维斯` 文案，后续进入代码修复时必须先改为 `问 Hermes` 或 `Hermes 数据管家`。

## 2. 计划复用 / 新增 Gateway Endpoint

### 2.1 复用：Hermes 能力声明

```http
GET /api/agent/hermes/capabilities
```

定位：

- 平台后端公开的 Hermes 能力边界接口。
- 前端可用来展示当前只读模式和未开放能力。

当前状态：

- 已存在。
- 建议继续复用。
- 不直接透传 Hermes 内部能力，只返回平台裁剪后的能力。

输出 schema：

```json
{
  "agentName": "Hermes",
  "mode": "catalog_only",
  "contractVersion": "delivery_platform.asset_views.v1.1",
  "supports": {
    "catalogQuery": true,
    "missingEvidence": true,
    "operationPlanDraft": true,
    "documentContentAnswer": false,
    "dbCrud": false,
    "nasCrud": false,
    "fullBimParse": false,
    "productionRollout": false
  },
  "safety": {
    "failClosed": true,
    "requiresProjectScope": true,
    "requiresCitationForContentAnswer": true
  }
}
```

### 2.2 复用：Hermes 健康检查

```http
GET /api/data-steward/hermes/health
```

定位：

- 前端只访问平台后端。
- 平台后端调用 Hermes `/health` 或等价接口。
- 返回脱敏健康状态，不返回 Hermes 内部 URL、环境变量、secret、DB DSN 或路径。

当前状态：

- 已存在。
- 后端通过 `HermesAgentClient.health()` 调用受控本机 / 内网 allowlist 地址。

输出 schema：

```json
{
  "status": "ok",
  "hermesAvailable": true,
  "mode": "read_only_gateway",
  "contractVersion": "delivery_platform.asset_views.v1.1",
  "gatewayEnabled": true,
  "readonly": true,
  "runtimeWriteEnabled": false,
  "agentAnswerIntegrationEnabled": false,
  "unavailableReason": null,
  "checkedAt": "2026-05-18T00:00:00Z"
}
```

前端展示要求：

- 只展示 `Hermes 可用 / 暂不可用`、`只读网关`、`正式写入未开放`。
- 不展示 Hermes base URL。
- 不展示服务 token。

### 2.3 复用：平台语义 Hermes Chat

```http
POST /api/data-steward/chat
```

定位：

- 前端主入口。
- 前端不得调用 Hermes 原始服务。
- Gateway 负责用户身份、project_scope、permission proof、审计和脱敏。

输入 schema：

```json
{
  "session_id": "frontend-session-id",
  "message": "帮我查一下这个项目有哪些 RVT 模型",
  "project_filters": ["project-code-or-id"],
  "mode": "catalog_lookup",
  "pageType": "project_asset_workspace",
  "projectId": 506,
  "assetId": 123,
  "sourceView": "FileAssetView",
  "question": "兼容旧字段，可为空"
}
```

说明：

- `message` 是 V3 推荐字段。
- `question/pageType/projectId/assetId/sourceView` 用于兼容当前平台已实现的旧调用。
- 如果 `projectId` 为空，Gateway 根据 `project_filters` 和当前用户权限解析项目。
- 如果解析不到有权项目，必须 fail closed。

输出 schema：

```json
{
  "status": "catalog_only",
  "evidenceMode": "catalog_only",
  "answer": "Hermes 当前仅基于资产目录和权限上下文回答，不包含文件正文证据。",
  "citations": [
    {
      "citationType": "catalog_metadata",
      "sourceView": "FileAssetView",
      "assetRef": "asset:123",
      "projectRef": "project:506",
      "displayLabel": "由平台前端按当前用户权限渲染",
      "safeToOpen": true
    }
  ],
  "permission": {
    "permissionStatus": "allowed",
    "projectScopeChecked": true,
    "permissionTagsChecked": true,
    "failClosedApplied": false,
    "reasonCode": null
  },
  "missingEvidence": [
    {
      "reason": "asset_catalog_only",
      "message": "当前只有资产目录信息，缺少可引用正文证据。"
    }
  ],
  "operationPlan": {
    "available": true,
    "requiresHumanApproval": true,
    "actions": [
      {
        "actionType": "request_evidence_ingestion_review",
        "status": "draft_only"
      }
    ]
  },
  "trace": {
    "requestId": "platform-trace-id",
    "agentMode": "catalog_only",
    "productionRollout": false
  }
}
```

### 2.4 复用：只读资产目录搜索

```http
POST /api/data-steward/catalog/search
```

定位：

- 只读 catalog metadata preview。
- 可供前端 Hermes 面板展示“资产目录预览”。
- 当前由平台后端基于平台数据库查询，已经遵守用户项目权限。
- 是否再转发给 Hermes，应由 Gateway 控制；前端不关心 Hermes 内部细节。

输入 schema：

```json
{
  "query": "C塔 RVT",
  "project_scope": {
    "type": "SPECIFIC_PROJECTS",
    "project_ids": ["506"]
  },
  "filters": {
    "asset_kind": ["MODEL", "FILE"],
    "file_ext": ["rvt", "dwg", "pdf"],
    "lifecycle_status": ["active"],
    "index_eligibility": ["catalog_only"]
  },
  "page": {
    "limit": 20,
    "cursor": null
  }
}
```

输出 schema：

```json
{
  "results": [
    {
      "assetRef": "file:123",
      "assetKind": "MODEL",
      "fileId": 123,
      "projectId": 506,
      "projectCode": "P-001",
      "projectName": "项目名称",
      "fileName": "模型文件.rvt",
      "fileExt": "rvt",
      "disciplineCode": "HVAC",
      "version": "V1",
      "sizeBucket": "gte_1gb",
      "lifecycleStatus": "active",
      "indexEligibility": "catalog_only",
      "contentEvidenceAvailable": false,
      "updatedAt": "2026-05-18T00:00:00Z"
    }
  ],
  "nextCursor": null,
  "safety": {
    "rawRowsOutput": false,
    "trueNasPathOutput": false,
    "secretPrinted": false
  }
}
```

### 2.5 保留但前端不推荐直接使用：底层 Agent Hermes Chat

```http
POST /api/agent/hermes/chat
GET /api/agent/hermes/health
```

定位：

- 当前代码中存在。
- 可作为平台内部测试 / 兼容接口。
- 前端业务应优先使用 `/api/data-steward/chat` 和 `/api/data-steward/hermes/health`。

要求：

- 不新增前端直连使用点。
- 如果后续正式收束，可将其降级为内部接口或只保留专项脚本使用。

## 3. project_scope / permission proof 生成方式

当前应由平台后端生成，不由前端或 Hermes 自己生成。

生成来源：

- 当前登录用户：`SecurityPrincipalAccessor.requireCurrentPrincipal()`。
- 项目过滤：请求中的 `projectId` 或 `project_filters`。
- 项目权限：平台 `core_user_project_roles` 和当前业务权限判断。
- 资产上下文：`AgentAssetContextResolver` 从稳定读模型 / 文件资产上下文解析。
- 权限证明：`AgentPermissionProofService` 生成 `project_scope`、`allowed_actions`、`permission_tags_checked`、`expires_at` 等。

Fail closed 条件：

- 无登录态。
- 无 project_scope。
- project_scope 解析不到当前用户有权项目。
- asset 无 project_id。
- 权限标签不匹配。
- lifecycle 不允许。
- 用户要求正文回答，但当前资产仅 `catalog_only`。

## 4. Hermes Health Check 接入方式

前端流程：

```text
前端 DataStewardPanel
  -> GET /api/data-steward/hermes/health
  -> 平台后端 Gateway
  -> HermesAgentClient.health()
  -> Hermes /health
```

返回策略：

- Hermes 可用：返回 `hermesAvailable=true`。
- Hermes 不可用：返回 `hermesAvailable=false`，前端显示“Hermes 暂不可用”。
- 不向前端返回内部异常堆栈、Hermes 地址、token、env、DB DSN。
- health 失败不应导致页面白屏。

## 5. Catalog Lookup 如何转发给 Hermes

推荐分两层：

1. 平台本地只读 catalog search：
   - `/api/data-steward/catalog/search`
   - 由平台数据库按权限返回 metadata preview。
   - 不返回正文 evidence。

2. Hermes chat / catalog lookup：
   - `/api/data-steward/chat`
   - Gateway 将用户问题、project_scope、permission proof、catalog-only response requirements 发给 Hermes。
   - Hermes 可以返回 catalog-only answer、Missing Evidence、permission denied、operation plan draft。
   - Gateway 对 Hermes 返回再做二次脱敏和状态收口。

关键约束：

- Hermes 不接收 DB 凭证。
- Hermes 不生成 SQL。
- Hermes 不直接读业务底表。
- Hermes 不扫描 NAS。
- Hermes 不写 documents/chunks。
- Hermes 不把 catalog metadata 伪装成正文 evidence。

## 6. 前端展示字段脱敏策略

允许展示：

- `Hermes` 名称。
- 当前模式：`资产目录辅助 / catalog-only`。
- 文件 ID / asset ref。
- 项目编号、项目名称。
- 文件名。
- 扩展名。
- 专业。
- 版本。
- 大小区间。
- 生命周期状态。
- `indexEligibility = catalog_only`。
- `contentEvidenceAvailable = false`。
- 权限证明状态。
- Missing Evidence 原因。
- 审计 trace id。

展示文案要求：

- 必须写清“仅目录元数据，不代表已读取文件正文”。
- 正文类问题必须显示 Missing Evidence。
- 操作建议必须标注“草案，需要人工审核”。

## 7. 绝不返回前端的字段

以下字段不得出现在 Hermes 面板、chat 响应、catalog preview、错误提示或普通审计详情中：

- `storage_path`
- `storage_uri`
- 真实 NAS 绝对路径，例如 `/Volumes/...`
- SMB 地址，例如 `smb://...`
- DB DSN
- DB 用户名 / 密码
- Hermes service token
- `.env` 内容
- raw DB row
- SQL 文本
- 文件正文内容
- DWG / RVT 解析内容
- documents / chunks 写入内容
- OpenSearch / Qdrant / MinIO 内部地址

例外：

- 平台已有的受控文件访问 / 下载接口可在权限校验后返回授权访问入口，但不应由 Hermes 回答文本输出真实路径。

## 8. 当前代码差距

1. 用户可见命名仍有 `问贾维斯`：
   - `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - 后续必须改为 `问 Hermes` 或 `Hermes 数据管家`。

2. 后端类名仍有 `JarvisDataStewardGatewayController`：
   - 这是内部类名，不直接影响接口契约。
   - 建议后续代码整理时改名为 `HermesDataStewardGatewayController`，但不作为当前接口评审阻塞。

3. `/api/agent/hermes/*` 仍可被前端调用：
   - 当前主前端使用的是 `/api/data-steward/chat` 和 `/api/data-steward/hermes/health`。
   - 建议后续明确 `/api/agent/hermes/*` 为内部 / 兼容接口，不作为新前端入口。

4. SQL View 仍有 `storage_path`：
   - 本机内部只读联调可接受。
   - shared-dev、staging、客户环境默认不得对前端和 Hermes 暴露。

## 9. Go / Pause / No-Go

### Go

允许继续：

- 平台后端 Gateway。
- 前端内嵌 Hermes。
- Hermes health check。
- 只读 catalog metadata preview。
- Missing Evidence 展示。
- Permission proof / project_scope / audit trace。
- Hermes 不可用时安全降级。

### Pause

需要等后续单独授权：

- selective indexing。
- NAS 文件正文只读抽取。
- PDF / Office 正文 evidence 写入。
- Hermes answer integration 到正式生产问答。
- query feedback 模型。
- `source_modified_at / file_modified_at`、`checksum_status`、capability flags 等增强字段。

### No-Go

当前明确禁止：

- Agent DB CRUD。
- Agent 生成 SQL。
- Agent NAS CRUD。
- Agent 读取 DWG/RVT 正文。
- Agent 直接扫描 NAS。
- Agent 返回真实 `storage_path`。
- 写 `documents / chunks / OpenSearch / Qdrant / MinIO`。
- 将 Hermes memory 作为 NAS 内容索引。
- 前端直连 Hermes。
- 宣称已具备图纸理解、BIM 模型理解、构件级问答。

## 10. 当前阻塞点

当前没有阻塞平台继续做只读 Hermes 前端网关接入的 P0。

进入下一轮代码前必须处理的 P1：

1. 用户可见名称从 `贾维斯` 全部改为 `Hermes`。
2. 前端文案继续强调 catalog metadata 不是正文 evidence。
3. 测试脚本继续断言 `/api/data-steward/catalog/search` 不返回 `storage_path / storage_uri / /Volumes / smb://`。
4. 测试脚本继续断言 chat 正文类问题返回 Missing Evidence。

建议下一步：

- 做一个极小批次：`Hermes 命名收束 + 只读 Gateway 前端复验`。
- 不碰业务数据库结构。
- 不碰 NAS。
- 不新增 Hermes 写入能力。

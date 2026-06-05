# 8B-GD1：平台侧葛兰岱尔适配骨架计划

更新时间：2026-05-27

## 批次定位

`8B-GD1` 是葛兰岱尔真实接入前的最小平台适配骨架批次。

本批目标不是做 RVT 真转换，而是让平台具备：

- `GLANDAR` provider 配置开关。
- Station API client 骨架。
- 创建轻量化任务 / 查询任务 / 获取 viewer ticket 的平台语义接口。
- 默认 `MOCK` 模式不回归。
- GLANDAR 未配置时业务化不可用，不 500。

## 必须保持的主线隔离

- 工作目录：`/Users/vc/Documents/数字化交付平台-8b-gd`
- 分支：`codex/8b-gd-lightweight-engine-adapter`
- 不在当前 M3G 分支叠加代码。
- 不修改 M3G 对象存储任务中心。
- 不提交到 main，等待 PR。

## 允许范围

允许修改：

- `backend/delivery-visualization-adapter/**`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/visualization/**`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`
- `handoff/dev-agent/latest-report.md`

如确实需要修改其他前端 API 文件，必须在报告说明原因。

## 禁止范围

- 不修改 `docs/**`。
- 不新增 Flyway 迁移。
- 不改 `backend/delivery-data-steward/**` 对象存储迁移主线。
- 不真实调用 `SplitUploadFile`。
- 不上传真实 RVT。
- 不读取模型正文。
- 不移动、删除、重命名 NAS 文件。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不把 Station token 写入任何文件。

## 后端目标

### 1. Provider 配置

默认：

```text
BIM_ENGINE_PROVIDER=MOCK
```

可配置：

```text
BIM_ENGINE_PROVIDER=GLANDAR
GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
GLANDAR_TOKEN=<secure-injection-only>
GLANDAR_DEFAULT_ACCURACY=5
GLANDAR_DEFAULT_STYLE=1
GLANDAR_UPLOAD_CHUNK_SIZE_MB=2
```

实现建议：

- 用 Spring 配置读取环境变量或 property。
- 默认 MOCK。
- 不要在响应中返回 token。
- 不要在响应中返回 Station API base URL，除非是后续 viewer launch 且已授权；GD1 不需要返回真实 URL。

### 2. 现有状态接口兼容

保留：

```http
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan
```

默认 MOCK 应明确：

- `engineMode=MOCK`
- `engineConnected=false`
- `viewerAvailable=false`
- `realConversionExecuted=false`
- `taskCreated=false`

如当前代码仍返回 `METADATA_ADAPTER`，本批可以修正为 `MOCK`，但必须确认 8A 回归通过。

### 3. 新增最小任务接口

新增平台语义接口：

```http
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

GD1 行为：

- `MOCK` 模式：不创建真实任务，返回 `taskCreated=false` 或 `blockedReason=ENGINE_PROVIDER_MOCK`。
- `GLANDAR` 但 token / base URL 缺失：返回业务化不可用原因，不 500。
- `GLANDAR` 配置完整：只允许返回“已具备提交条件 / 待 GD2 实现真实上传”，不得真实调用 `SplitUploadFile`。

### 4. Station client 骨架

可新增内部类 / service：

- GlandarEngineProperties
- GlandarEngineClient
- GlandarEngineAdapter

GD1 只允许：

- 参数校验。
- health 轻量探测可选。
- 构造请求模型。
- 不发送真实分片上传。

## 前端目标

最小更新即可：

- 模型集成页能显示 `葛兰岱尔` / `Mock` / `未配置` 状态。
- “创建轻量化任务”按钮或入口存在，但 MOCK / 未配置时显示业务化提示。
- “获取 Viewer 入口”未 ready 时不可误导为可预览。
- 不展示 Station token、真实存储路径、bucket、object key。

如果本批前端改动风险较高，可以只补 API helper 和状态文案，保持页面结构不大改。

## 专项脚本

新增：

```text
scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
```

至少验证：

1. 管理员登录。
2. 创建或复用安全模型集成元数据。
3. `lightweight-status` 默认 `engineMode=MOCK`。
4. `lightweight-plan` 仍 dry-run。
5. `POST lightweight-jobs` 在 MOCK 下不创建真实任务、不 500。
6. `GET lightweight-jobs/{jobId}` 或等价 mock 响应不 500。
7. `viewer-ticket` 未 ready 时返回不可用原因。
8. OpenAPI 包含新增接口。
9. 响应不包含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、真实 bucket、真实 object key、vendor token、raw row、SQL。
10. 8A 脚本继续通过。

## 验收标准

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- 8B-GD1 专项脚本通过。
- 8A 回归通过。
- file-access 回归通过。
- `git diff --check` 通过。
- 不修改 `docs/**`。
- 不新增迁移。
- 不触发真实 Station 转换。

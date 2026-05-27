# 开发 Agent 当前任务：8B-GD1 平台侧葛兰岱尔适配骨架

你是卓羽智能数据中台 v1 的开发 agent。当前任务由 Codex 主 agent 监控和验收。

工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前分支：

`codex/8b-gd-lightweight-engine-adapter`

## 0. 必须使用 Ralph Loop

开始后请先使用 `Ralph Loop` skill / 工作流：

1. 先阅读上下文。
2. 列出最小实现计划。
3. 执行一小步。
4. 自检。
5. 继续下一小步。
6. 最后输出报告。

不要创建子 agent。不要修改 main。不要合并分支。

## 1. 本批定位

本批是：

`8B-GD1：平台侧葛兰岱尔适配骨架`

目标是给葛兰岱尔 Station Management 接入建立平台侧骨架：

- `GLANDAR` provider 配置开关。
- Station API client / adapter 骨架。
- 创建轻量化任务、查询任务、获取 viewer ticket 的平台语义接口。
- 默认 `MOCK` 不回归。
- `GLANDAR` 未配置时返回业务化不可用原因，不 500。

本批不做真实 RVT 转换，不调用 `SplitUploadFile` 上传真实文件。

## 2. 必读文件

开始前先阅读：

- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd-roadmap.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `handoff/main-agent/8b-gd1-glandar-adapter-skeleton-plan.md`
- `handoff/main-agent/phase2-batch8a-bim-lightweight-adapter-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`

## 3. 允许修改

允许修改：

- `backend/delivery-visualization-adapter/**`
- `frontend/src/modules/data-steward/pages/ModelIntegrationsPage.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/visualization/**`
- `scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh`
- `handoff/dev-agent/latest-report.md`

如果确实需要修改其他前端 API 文件，请在报告里说明原因。

## 4. 禁止修改 / 禁止事项

禁止：

- 禁止修改 `docs/**`。
- 禁止新增 Flyway 迁移。
- 禁止修改 `backend/delivery-data-steward/**` 对象存储迁移主线。
- 禁止真实调用 `SplitUploadFile`。
- 禁止上传真实 RVT。
- 禁止读取 RVT / IFC / DWG / PDF / Office 正文。
- 禁止移动、删除、重命名 NAS 文件。
- 禁止访问 MinIO 底层目录。
- 禁止写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 禁止把 Station token、secret、password 写入文件、日志、报告或前端响应。
- 禁止把 Mock 状态包装成真实可预览。

## 5. 后端实现要求

### A. Provider 配置

默认必须是：

```text
BIM_ENGINE_PROVIDER=MOCK
```

支持未来配置：

```text
BIM_ENGINE_PROVIDER=GLANDAR
GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
GLANDAR_TOKEN=<secure-injection-only>
GLANDAR_DEFAULT_ACCURACY=5
GLANDAR_DEFAULT_STYLE=1
GLANDAR_UPLOAD_CHUNK_SIZE_MB=2
```

真实 token 只能从环境或安全配置读取，不能返回前端。

### B. 现有 8A 接口兼容

保留：

```http
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status
GET /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan
```

默认 MOCK 下应返回：

- `engineMode=MOCK`
- `engineConnected=false`
- `viewerAvailable=false`
- `lightweightStatus=NOT_CONNECTED` 或 `NOT_STARTED`
- `taskStatus=NOT_CREATED`
- 不执行真实转换。

如果当前代码仍返回 `METADATA_ADAPTER`，请修正为 `MOCK`，并确保 8A 回归脚本通过。

### C. 新增最小任务接口

新增：

```http
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

GD1 行为：

- `MOCK` 模式：不创建真实任务，返回业务化状态，例如 `taskCreated=false`、`blockedReason=ENGINE_PROVIDER_MOCK`。
- `GLANDAR` 但 base URL / token 缺失：返回业务化不可用原因，例如 `ENGINE_TOKEN_MISSING` 或 `ENGINE_CONFIG_INCOMPLETE`，不 500。
- `GLANDAR` 配置完整：只返回“具备提交条件 / 待 GD2 实现真实上传”，不得调用 `SplitUploadFile`。

### D. Station client / adapter 骨架

可新增内部类：

- `GlandarEngineProperties`
- `GlandarEngineClient`
- `GlandarEngineAdapter`

GD1 只允许做：

- 配置读取。
- 参数校验。
- health 轻量探测可选。
- 构造请求模型。
- 不发送真实分片上传。

## 6. 前端实现要求

最小更新即可，不要重构 UI：

- 模型集成页能展示 `Mock / 葛兰岱尔 / 未配置` 状态。
- 行操作可出现“创建轻量化任务”或“查看任务状态”的安全入口。
- MOCK / 未配置时展示友好提示，不误导为可预览。
- viewer ticket 未 ready 时显示不可用原因。
- 不展示 Station token、真实存储路径、真实 bucket、真实 object key。

如果前端改动风险高，可以先只补 API helper 与状态文案。

## 7. 专项脚本

新增：

```text
scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
```

至少覆盖：

1. 管理员登录。
2. 创建或复用安全模型集成元数据。
3. `lightweight-status` 默认 `engineMode=MOCK`。
4. `lightweight-plan` 仍 dry-run。
5. `POST lightweight-jobs` 在 MOCK 下不创建真实任务、不 500。
6. `viewer-ticket` 未 ready 时返回不可用原因。
7. OpenAPI 包含新增接口。
8. 响应不包含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、真实 bucket、真实 object key、vendor token、raw row、SQL。
9. `check-phase2-batch8a-bim-lightweight-adapter.sh` 继续通过。

## 8. 必跑验证

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如后端未运行，请按项目已有方式启动；不要改主 M 系列分支。

## 9. 报告要求

完成后写入：

```text
handoff/dev-agent/latest-report.md
```

报告必须包含：

- 改动文件清单。
- 后端实现点。
- 前端实现点。
- 为什么没有真实调用 Station。
- GLANDAR 未配置时如何降级。
- 验证命令结果。
- 禁出字段扫描结果。
- 是否触碰 `docs/**`、迁移、M3G 对象存储主线。

最后输出：

```text
READY_FOR_CODEX_MONITOR 8B-GD1
```

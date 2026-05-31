# 开发 Agent 当前任务：8B-GD2-min 105 RVT 最小真实转换闭环

你是数字化交付平台 3D 引擎接入开发 agent。本轮只做葛兰岱尔最小可用闭环，不扩功能。

工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前分支必须是：

`codex/8b-gd2-rvt-poc`

如不是该分支，停止并报告。

## 0. 本轮目标

只实现这一条最小链路：

```text
105 项目 RVT 样本 fileId=1257
-> 平台校验权限
-> 后端读取受控文件流
-> 后端调用葛兰岱尔 SplitUploadFile
-> 保存 lightweightName / uniqueCode / 状态
-> 查询 query-model-info
-> 成功后返回平台受控 viewer 启动信息
```

不要做全量 BIM 能力，不要扩展构件、测量、剖切、图模联动。

## 1. 配置前置

用户已建立外部安全配置文件：

`~/.zhuoyu-delivery/glandar.env`

启动后端时必须这样加载：

```bash
set -a
source ~/.zhuoyu-delivery/glandar.env
set +a
```

当前配置应包含：

```text
BIM_ENGINE_PROVIDER=GLANDAR
GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
GLANDAR_TOKEN=<secure>
```

绝对禁止把 token 写入：

- 聊天
- Git
- handoff
- 脚本
- 日志
- 前端
- API 响应

## 2. 必读文件

- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `handoff/main-agent/8b-gd1-glandar-adapter-skeleton-closure.md`
- `handoff/main-agent/8b-gd2-glandar-rvt-poc-plan.md`
- `/Users/vc/Downloads/葛兰岱尔StationManagement平台接入API文档.md`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`

## 3. 样本锁定

本轮只用这个样本：

```text
projectId=503
fileId=1257
RVT
约 10MB
已有 active object version
```

如该文件不可用，停止并报告，不要自行扩大到全量模型。

## 4. 必做实现

### A. 任务持久化

新增轻量化任务映射能力，保存：

- projectId
- integrationId，可为空或由脚本临时创建
- fileId
- assetUuid
- engineProvider=GLANDAR
- engineType=2
- lightweightName
- uniqueCode
- status
- progressPercent
- modelAccessAddress
- viewerAvailable
- lastErrorCode
- lastErrorMessage
- stationRecordJson
- createdBy / createdAt / updatedAt

不得保存 token。

如需要新增 Flyway 迁移，必须先检查最新迁移号。只追加新迁移，不改旧迁移。

### B. Station 客户端

实现最小葛兰岱尔客户端：

- `SplitUploadFile`
- `query-model-info`

要求：

- Header 名使用 `Token`。
- token 只从 `GlandarEngineSettings`/环境变量读取。
- 默认分片大小 2MB。
- 上传失败要记录安全失败原因。
- 不把 Station 原始响应完整透给前端。

### C. 平台接口

沿用 8B-GD1 接口：

```text
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

也可以新增一个 fileId 直达别名，便于最小 PoC：

```text
POST /api/visualization-adapter/projects/{projectId}/files/{fileId}/lightweight-jobs
```

但前端不得直连 Station。

### D. Viewer 最小入口

本轮只要求返回平台受控 viewer 启动信息：

- viewerAvailable
- launchUrl 或 modelAccessAddress 的受控封装
- engineProvider
- lightweightName
- status

如葛兰岱尔返回的地址不可访问，返回业务化错误。

## 5. 禁止事项

严禁：

- 不做全量模型转换。
- 不做 DWG / IFC / NWD / NWC 支持。
- 不做构件级解析。
- 不写 Hermes memory / documents / chunks / Qdrant / OpenSearch。
- 不移动、删除、重命名真实 NAS 文件。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、Token、secret、raw row、SQL。
- 不修改 `docs/**`。

## 6. 验收脚本

新增或完善：

`scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`

脚本必须：

1. 不打印 token。
2. 检查缺配置时业务化阻断。
3. 检查配置完整时 `fileId=1257` 可提交任务。
4. 检查可拿到 lightweightName 或明确 Station 失败原因。
5. 检查任务可查询。
6. 检查 viewer-ticket 在 ready 时可返回受控启动信息。
7. 检查禁出字段。
8. 回归 8B-GD1、8A、file-access。

## 7. 自测命令

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
set -a
source ~/.zhuoyu-delivery/glandar.env
set +a
SERVER_PORT=18088 java -jar backend/delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar

# 另一个终端：
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd2-glandar-rvt-poc.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-8b-gd1-glandar-adapter-skeleton.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
BASE_URL=http://127.0.0.1:18088 bash scripts/dev/check-phase2-batch4-file-access.sh
corepack pnpm --dir frontend build
git diff --check
```

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

必须说明：

- 是否真实调用 Station。
- 使用样本 fileId。
- 是否拿到 lightweightName。
- Station 状态如何映射。
- 是否返回 viewer 受控启动信息。
- token 是否未泄露。
- 是否触碰 NAS。
- 自测结果。
- 未完成事项。

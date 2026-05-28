# 开发 Agent 当前任务：8B-GD2 葛兰岱尔 RVT PoC 转换闭环

你是数字化交付平台 3D 引擎接入开发 agent。本轮只在独立 8B-GD worktree 开发，不碰 M 系列对象存储主线。

工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前分支必须是：

`codex/8b-gd2-rvt-poc`

如果不是该分支，先停止并报告。

## 0. 本轮目标

实现 `8B-GD2：105 RVT PoC 转换闭环`。

目标链路：

```text
平台校验权限
-> 读取 105 项目 RVT 样本受控文件流
-> 按葛兰岱尔 Station SplitUploadFile 协议分片上传
-> 保存 lightweightName / uniqueCode / 状态映射
-> 查询 query-model-info
-> status=100 后返回平台受控 viewer 状态
```

本批只做 105 项目 1-3 个 RVT 小样本 PoC，不做全量模型转换。

## 1. 必读文件

开始前先阅读：

- `handoff/main-agent/8b-gd-task-graph.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `handoff/main-agent/8b-gd1-glandar-adapter-skeleton-closure.md`
- `handoff/main-agent/8b-gd2-glandar-rvt-poc-plan.md`
- `handoff/dev-agent/latest-report.md`
- `/Users/vc/Downloads/葛兰岱尔StationManagement平台接入API文档.md`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/application/VisualizationAdapterApplicationService.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/engine/GlandarEngineSettings.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/controller/VisualizationAdapterController.java`
- `backend/delivery-visualization-adapter/src/main/java/com/zhuoyu/delivery/visualization/dto/VisualizationDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`

## 2. 当前事实

- Station API：`http://192.168.1.37:18086`
- Station Web / static：`http://192.168.1.37:18087`
- 成功状态：`status == 100`
- 查询接口：`POST /api/app/model/query-model-info?LightweightName=...`
- 大文件上传接口：`POST /api/app/model/SplitUploadFile`
- BIM `engineType=2`
- 当前 105 项目本地 ID：`503`
- 优先样本：`fileId=1257`，RVT，约 10MB，已有 active object version。

## 3. 安全配置要求

真实转换只能在以下环境变量全部存在时执行：

```bash
BIM_ENGINE_PROVIDER=GLANDAR
GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
GLANDAR_TOKEN=<secure-injection-only>
```

不要把 token 写入：

- 聊天
- Git
- handoff
- 脚本
- 日志
- 前端
- API 响应

缺配置时接口必须业务化阻断，不 500。

## 4. 允许改动范围

允许：

- `backend/delivery-visualization-adapter/**`
- 必要的 `backend/delivery-app/src/main/resources/db/migration/**`，但新增迁移前必须确认当前最新版本号，避免与 M 系列冲突。
- `scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`
- `handoff/dev-agent/latest-report.md`

谨慎允许：

- 如果必须读取对象存储 active version，可只读调用已有 data-steward/storage service，不改其语义。

禁止：

- 不修改 `docs/**`
- 不修改 M 系列对象存储主线 worktree
- 不写 Hermes memory / documents / chunks / Qdrant / OpenSearch
- 不做 DWG/IFC/NWD 全量支持
- 不做全量模型转换
- 不移动、删除、重命名 NAS 文件
- 不让前端直连 Station API

## 5. 实现要求

### A. 任务表 / 映射

新增或复用平台侧轻量化任务映射，至少保存：

- projectId
- integrationId
- fileId
- assetUuid
- engineProvider = GLANDAR
- engineType = 2
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

### B. Station 客户端

实现最小 Station client：

- `SplitUploadFile`
- `query-model-info`

要求：

- Header `Token` 从后端安全配置读取。
- 分片大小可配置，默认 2MB。
- 失败时保留业务化失败原因。
- 不把 Station 原始响应直接透给前端。

### C. 平台接口

沿用 8B-GD1 接口：

```text
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

行为：

- POST 创建真实 Station 上传任务。
- GET 查询平台任务 + Station 状态。
- viewer-ticket 在 `READY` 时返回受控 viewer 状态；本批可以先返回平台 launch 信息，不必完成完整前端 viewer。

### D. 权限与安全

必须先校验：

- 当前用户项目权限。
- integration 属于当前项目。
- model file 属于当前项目。

响应不得包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- bucket
- object_key
- Token
- secret
- raw row
- SQL

## 6. 验收脚本

新增：

`scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`

脚本至少验证：

1. 当前配置缺失时接口业务化阻断，不 500。
2. 配置完整时可对 105 RVT 样本提交任务。
3. 平台保存任务记录。
4. 查询任务状态不 500。
5. 成功/失败都能返回明确状态。
6. 禁出字段扫描通过。
7. 8B-GD1 / 8A / file-access 回归通过。

脚本不得打印 token。

## 7. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台-8b-gd
# 后端建议用 18088，避免影响主线 8080
SERVER_PORT=18088 java -jar backend/delivery-app/target/delivery-app-1.0.0-SNAPSHOT.jar

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

报告必须包含：

- 是否真实调用 Station。
- 使用了哪些样本 fileId。
- 是否成功拿到 lightweightName。
- Station 查询结果如何映射。
- 是否生成 viewer 可用状态。
- 禁出字段扫描结果。
- 是否有 token / raw path 泄露风险。
- 自测命令结果。
- 未完成事项。

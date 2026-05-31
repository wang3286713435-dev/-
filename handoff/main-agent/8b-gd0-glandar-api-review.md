# 8B-GD0 葛兰岱尔 API 文档评审与平台接入裁决

更新时间：2026-05-27

评审文档：

```text
/Users/vc/Downloads/葛兰岱尔StationManagement平台接入API文档.md
```

## 总体结论

文档已经足够支撑进入 `8B-GD1：平台侧葛兰岱尔适配骨架` 的设计与开发准备，但还不建议直接进入 RVT 正式 PoC 转换。

原因：

- Station API / Web 地址、分片上传、状态查询、viewer 加载方式已经明确。
- 平台侧需要先做适配骨架、任务表、状态映射、token 安全注入和 viewer ticket。
- `modelAccessAddress` 端口存在关键配置风险：返回 `18087` 时可能 404，平台必须做可访问性校验。
- Token 不能进入前端和 Git，必须由后端安全配置注入。

## 已确认的 Station 侧信息

| 项 | 当前结论 |
| --- | --- |
| Station API | `http://192.168.1.37:18086` |
| Station Web / 静态资源 | `http://192.168.1.37:18087` |
| 前端引擎资源 | 可从 `18087/static/ThreeJsEngine` 加载 |
| 模型结果推荐地址 | `18086/Tools/output/model/{lightweightName}/root.glt` |
| 成功状态 | `status == 100` |
| 查询接口 | `POST /api/app/model/query-model-info?LightweightName=...` |
| 大文件上传接口 | `POST /api/app/model/SplitUploadFile` |
| 小文件上传接口 | `POST /api/app/model/upload-file?input=...` |
| 认证 | Header `Token`，真实值从 Station token 文件或安全配置注入 |
| BIM engineType | `2` |
| CAD engineType | `1` |
| PoC 首选 | RVT，分片上传 |

## 现场连通性轻测

本轮只做了无密钥、非转换类探测：

```text
GET  http://192.168.1.37:18087/config.json -> 200
HEAD http://192.168.1.37:18086/            -> 200
HEAD http://192.168.1.37:18087/            -> 200
```

`config.json` 中 `BASE_URL` 指向 `http://192.168.1.37:18086`，这与文档建议一致。

未执行：

- 未调用 `SplitUploadFile`。
- 未调用 `upload-file`。
- 未读取真实模型文件。
- 未使用 Station Token。
- 未创建真实轻量化任务。

## 与原 8B-GD 计划的关键差异

原计划写法是：

```text
平台生成短时模型取用票据 -> 引擎通过授权链接拉取模型
```

但当前 Station API 文档给出的确定实现路径是：

```text
平台后端从 MinIO / StorageService 读取模型文件流
-> 平台后端分片上传给 Station API
-> Station 返回 lightweightName
-> 平台保存 fileId / assetUuid / objectVersion 与 lightweightName 的映射
```

裁决：

- `8B-GD1 / 8B-GD2` 优先按 Station 文档的“平台后端分片上传”实现。
- `ModelUploadUrl` 字段保留为后续“引擎主动拉取短时链接”的扩展位。
- 安全边界不变：引擎仍然不能直连 NAS、不能直连 MinIO 底层目录、不能绕过平台权限。

## 推荐平台侧数据流

```text
用户点击模型预览
-> 前端调用平台 visualization-adapter 接口
-> 平台校验用户、项目和文件权限
-> 平台查询 lightweight task 映射
-> 若无映射：平台通过 StorageService 读取 active object version / 受控文件流
-> 平台分片上传 Station API
-> 平台保存 lightweightName 与平台资产映射
-> 前端轮询平台任务状态
-> 平台查询 Station query-model-info
-> status=100 时平台生成 viewer ticket / launch 参数
-> 前端加载 Glendale 引擎资源和 modelAccessAddress
```

## 平台对前端接口建议

继续保持平台语义，避免直接暴露 Station 细节。

优先映射到既有 visualization-adapter 路由：

```http
GET  /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status
GET  /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

如果文件管理器需要直接从 `fileId` 打开模型，可新增平台语义别名：

```http
GET  /api/visualization-adapter/projects/{projectId}/files/{fileId}/lightweight-launch
POST /api/visualization-adapter/projects/{projectId}/files/{fileId}/lightweight-jobs
```

但前端不应调用 Station API。

## 后端实现建议

### 1. 配置项

建议使用环境变量或配置文件，真实 token 只在本地安全注入：

```text
BIM_ENGINE_PROVIDER=GLANDAR
GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
GLANDAR_TOKEN=<secure-injection-only>
GLANDAR_DEFAULT_ACCURACY=5
GLANDAR_DEFAULT_STYLE=1
GLANDAR_UPLOAD_CHUNK_SIZE_MB=2
```

默认仍应为：

```text
BIM_ENGINE_PROVIDER=MOCK
```

### 2. 数据表 / 映射

需要新增平台侧轻量化任务映射表，命名可按仓库风格调整。

核心字段建议：

- `project_id`
- `integration_id`
- `file_id`
- `asset_uuid`
- `object_version`
- `engine_provider`
- `engine_type`
- `lightweight_name`
- `unique_code`
- `status`
- `status_description`
- `model_access_address`
- `viewer_available`
- `last_error_code`
- `last_error_message`
- `station_record_json`
- `created_by`
- `created_at`
- `updated_at`

注意：

- 不建议保存真实 token。
- 不建议向前端返回 Station 原始记录。
- `model_access_address` 是 viewer 加载必须字段，但对外返回前需要 viewer ticket 和可访问性校验。

### 3. Station 状态映射

| Station 状态 | 平台语义 |
| --- | --- |
| `status == 100` | `READY` |
| `status < 0` | `FAILED` |
| 查不到任务 | `QUEUED / NOT_FOUND_PENDING` |
| 其他状态 | `RUNNING` |

### 4. 地址可访问性校验

平台不能盲信 `modelAccessAddress`。

必须校验：

- 不能是 `localhost` / `127.0.0.1`。
- 必须是业务客户端可访问的局域网地址或平台代理地址。
- 如果返回 `18087/Tools/output/model/...` 且访问失败，应返回 `ENGINE_MODEL_URL_INVALID`。
- 无代理模式优先使用 `18086/Tools/output/model/.../root.glt`。

## 前端实现建议

### 1. 文件管理器

- RVT / RFA / IFC / NWD / NWC：右键或双击进入“模型轻量化预览”。
- 若未转换：展示“提交转换 / 转换中 / 失败原因”。
- 若已成功：打开平台 viewer 页面。

### 2. 模型集成页

- 展示葛兰岱尔状态：
  - 未提交
  - 转换中
  - 可预览
  - 转换失败
  - 引擎不可达
  - viewer 地址不可访问
- 不显示 Station token、底层存储位置、对象 key。

### 3. Viewer 页面

- 动态加载 Glendale engine script。
- 模型地址来自平台 launch API。
- 最小工具：
  - 视角切换
  - 鼠标模式
  - 剖切
  - 爆炸
  - 透明度
  - 构件拾取
  - 测量
- PoC 阶段先保证打开模型和回到项目上下文。

## 风险与阻塞

### R1：Token 安全

Station API 使用 Header `Token`。平台必须后端保管，不得返回前端。

### R2：模型结果地址

文档记录历史任务返回过 `18087/Tools/output/model/...` 且访问 404。平台必须校验并给出业务错误。

### R3：原计划的“引擎拉取”与文档的“平台分片上传”不同

PoC 优先采用平台分片上传。引擎拉取短时链接后置，除非引擎团队确认 `ModelUploadUrl` 可用。

### R4：大文件性能

RVT 可能较大。8B-GD2 只选 1-3 个样本，先验证链路，不承诺全项目批量转换。

### R5：产物归属

轻量化产物先由 Station 管理；平台只保存映射、状态和 viewer 入口。后续是否把产物再入对象存储，放到后续批次裁决。

## 进入 8B-GD1 的条件

当前文档已满足大部分 8B-GD1 设计输入。进入 8B-GD1 前还需确认：

- Station Token 安全注入方式。
- 是否允许平台后端直接访问 `18086` API。
- `SplitUploadFile` 对大 RVT 的实际 chunk size / 并发建议。
- `query-model-info` 是否所有任务都可查。
- `modelAccessAddress` 最终是否稳定返回 `18086`。
- viewer script 路径是否固定为 `18087/static/ThreeJsEngine/glendale.v1.umd.js`。

## 主 agent 裁决

- 可以把 8B-GD0 从“等待接口文档”推进到“接口文档已评审”。
- 下一步建议：要求开发 agent 在 8B-GD 独立 worktree 内补写 `handoff/dev-agent/latest-report.md`，确认无业务代码改动后，由测试 agent 做 8B-GD0 极短验收。
- 验收通过后，再由主 agent 决定是否进入 `8B-GD1：平台侧葛兰岱尔适配骨架`。


# 8B-GD2 葛兰岱尔 RVT PoC 转换闭环计划

日期：2026-05-28

## 批次定位

`8B-GD2：105 RVT PoC 转换闭环`

本批从已收口的 `8B-GD1` 骨架进入真实转换 PoC，但仍然只做 105 项目 1-3 个 RVT 小样本，不扩大到全量模型，不进入完整 Viewer 业务联动。

## 当前前置检查

- 葛兰岱尔 Station API 可达：`http://192.168.1.37:18086`。
- 葛兰岱尔 Web / 静态资源可达：`http://192.168.1.37:18087`。
- `config.json` 指向 `BASE_URL=http://192.168.1.37:18086`。
- 当前 shell 未注入：
  - `BIM_ENGINE_PROVIDER=GLANDAR`
  - `GLANDAR_STATION_API_BASE`
  - `GLANDAR_STATION_WEB_BASE`
  - `GLANDAR_TOKEN`
- 105 本地项目 ID 仍按当前库使用 `503`。
- 优先 PoC 样本候选：`fileId=1257`，RVT，约 10MB，当前已有 active object version。

## 核心目标

```text
用户/测试脚本提交 105 RVT 样本
-> 平台校验项目和文件权限
-> 平台读取受控文件流
-> 平台按葛兰岱尔 SplitUploadFile 协议分片上传
-> Station 返回 lightweightName / uniqueCode
-> 平台保存任务映射
-> 平台查询 query-model-info
-> status=100 后返回 viewer ticket 骨架或受控 launch 信息
```

## 允许范围

- 可在 `backend/delivery-visualization-adapter/**` 中新增葛兰岱尔客户端、任务服务、DTO。
- 可新增 Flyway 迁移保存轻量化任务映射，但迁移号必须先检查当前主线最新版本，避免和 M 系列冲突。
- 可新增专项脚本 `scripts/dev/check-8b-gd2-glandar-rvt-poc.sh`。
- 可更新 `handoff/**`。

## 禁止范围

- 不修改 `docs/**`。
- 不上传全量模型。
- 不读取 PDF/Office/DWG/RVT 正文语义。
- 不写 documents/chunks/Qdrant/OpenSearch/Hermes memory。
- 不移动、删除、重命名真实 NAS 文件。
- 不向前端/日志/报告输出 Station Token、真实 NAS 路径、bucket、object key、storage_uri、raw row、SQL。
- 不让前端直连 Station API。

## 8B-GD2 实现拆分

### 1. 配置与安全启动

- 显式要求 `BIM_ENGINE_PROVIDER=GLANDAR` 才允许真实提交。
- `GLANDAR_STATION_API_BASE`、`GLANDAR_STATION_WEB_BASE`、`GLANDAR_TOKEN` 必须通过安全环境注入。
- 缺任一配置时返回业务化阻断，不 500。

### 2. 任务映射

建议新增轻量化任务表，字段至少包含：

- `id`
- `project_id`
- `integration_id`
- `file_id`
- `asset_uuid`
- `engine_provider`
- `engine_type`
- `lightweight_name`
- `unique_code`
- `status`
- `progress_percent`
- `model_access_address`
- `viewer_available`
- `last_error_code`
- `last_error_message`
- `station_record_json`
- `created_by`
- `created_at`
- `updated_at`
- `deleted`

不得保存 token。

### 3. Station 客户端

按文档实现最小客户端：

- 分片上传：`POST /api/app/model/SplitUploadFile`
- 状态查询：`POST /api/app/model/query-model-info?LightweightName=...`
- Header 使用 `Token`，值只从后端环境读取。
- 上传时使用文件名、分片序号、总分片、配置 JSON 等 Station 文档要求字段。
- 需要记录失败原因，但不记录 token。

### 4. 文件流来源

优先从平台已授权对象版本读取；若样本仍是 NAS_ONLY，可通过现有 `file-access / StorageService` 的受控读取链路读取。

本批必须保证：

- 先校验用户 project 权限。
- 再校验 integration 与 file 属于同项目。
- 再读取文件流。

### 5. 状态与 viewer ticket

- `GET lightweight-jobs/{jobId}` 查询平台任务表，再查 Station 状态。
- `status=100` 映射为 `READY`。
- 生成 viewer ticket 时不返回 token。
- `modelAccessAddress` 必须做安全检查：不能是 localhost / 127.0.0.1；不得含真实本地路径；访问失败时返回业务错误。

## 验收标准

- 105 项目一个 RVT 样本可提交转换任务。
- Station 返回任务标识，平台有任务记录。
- 任务状态可查询。
- Station 成功时平台返回 `READY` 或 viewer 可用状态。
- Station 失败时平台返回明确失败原因。
- 无 token / raw path / bucket / object key 泄露。
- 8B-GD1 / 8A / file-access 回归通过。

## 当前阻塞

真实转换前必须由本机安全注入：

```bash
export BIM_ENGINE_PROVIDER=GLANDAR
export GLANDAR_STATION_API_BASE=http://192.168.1.37:18086
export GLANDAR_STATION_WEB_BASE=http://192.168.1.37:18087
export GLANDAR_TOKEN='<secure-token-from-engine-team>'
```

不要把 token 发到聊天、handoff、Git 或脚本中。

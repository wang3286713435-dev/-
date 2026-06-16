# M3G-9 前置检查：BIM 协同与葛兰岱尔分支兼容性

检查时间：2026-06-01

## 结论

M3G-9 暂不建议直接启动。当前 5173 主线前端的 `BIM协同管理` 页面可以正常打开，105 项目模型也能以“元数据适配 / RVT 待轻量化”的方式展示，但当前 M3G 分支尚未合入葛兰岱尔真实轻量化适配代码。

换成更直白的话：

- “BIM 协同页不白屏、不报错”：通过。
- “默认 MOCK / 元数据适配不报错”：页面通过，但旧 8A 脚本仍期待 `engineMode=MOCK`，当前接口实际返回 `METADATA_ADAPTER`，脚本口径需要更新或恢复兼容别名。
- “注入葛兰岱尔配置后 105 模型可真实预览”：当前不具备验证条件，因为当前分支没有葛兰岱尔适配器代码入口。

## 已检查内容

### 1. 当前运行服务

- 前端 `5173` 来自当前仓库：`/Users/vc/Documents/数字化交付平台/frontend`
- 后端 `8080` 健康检查正常：`UP`
- 当前分支：`codex/m3g-nas-minio-real-project-object-storage`

### 2. BIM 协同页面

浏览器访问：

`http://127.0.0.1:5173/bim-collaboration`

结果：

- 页面正常打开。
- 可在项目选择中切换到 `105 / 启航华居项目`。
- 105 页面显示：
  - 资产文件：`2,928`
  - 模型文件：`198`
  - 已发布模型：`1`
  - 模型：`全楼层 / V1 · RVT`
  - 状态：`真实 Viewer 未接入`
  - 轻量化：`NOT_CONNECTED`
  - 说明：当前只展示模型元数据和适配状态，未执行真实轻量化转换。

页面没有发现白屏、500、明显前端运行时报错。

### 3. 后端接口

接口：

`GET /api/visualization-adapter/projects/503/digital-twin-dashboard`

结果：

- HTTP 200
- 统一响应 `code=OK`
- 不含 `/Volumes`、`smb://`、`nas://`、`storage_uri`、`bucket`、`object_key`、SQL、token、secret 等禁出字段。
- `modelSummary.engineMode=METADATA_ADAPTER`
- `viewerAvailable=false`
- `lightweightStatus=NOT_CONNECTED`

### 4. 旧 8A 脚本兼容性

脚本：

`scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`

结果：

- 当前失败点是接口返回 `engineMode=METADATA_ADAPTER`，而脚本仍断言 `engineMode=MOCK`。
- 前端已经把 `MOCK` 和 `METADATA_ADAPTER` 都展示为“元数据适配”。

判断：

- 这不是页面崩溃问题。
- 这是历史脚本和当前接口口径不一致。
- 如果后续承认 `METADATA_ADAPTER` 是新默认 mock-like 模式，应更新脚本。
- 如果必须保持旧合同，应让后端继续返回 `MOCK` 或额外返回兼容字段。

### 5. 葛兰岱尔分支合入状态

发现两个相关分支：

- `codex/8b-gd-lightweight-engine-adapter`
- `codex/8b-gd2-rvt-poc`

当前 M3G 分支没有包含这些分支的提交。当前代码中也没有实际的：

- `GlandarEngineSettings`
- `GlandarStationClient`
- `GlandarModelPreviewPage`
- 葛兰岱尔真实预览接口/路由

因此当前不能通过“注入葛兰岱尔配置”来打开 105 的真实模型预览。

注意：直接把 `codex/8b-gd2-rvt-poc` 合入当前 M3G 分支有风险。它基于较旧代码，差异中会影响/删除部分 M3 脚本，因此不应盲目 merge，应该新开兼容批次做“摘取式迁移”。

## 建议

在 M3G-9 前插入一个小修复批次：

`DT-F1：BIM 协同 / 葛兰岱尔适配兼容收口`

建议目标：

1. 明确默认模式口径：`METADATA_ADAPTER` 是否正式替代 `MOCK`。
2. 修复或更新 8A 脚本，避免历史脚本误报。
3. 将葛兰岱尔分支按当前 M3G 主线重新适配，而不是直接整分支合入。
4. 保证未配置葛兰岱尔时页面仍为元数据适配，不报错。
5. 配置葛兰岱尔后，105 的 RVT 模型至少能进入真实预览入口；如果真实引擎不可用，应返回清晰的业务错误，而不是白屏或伪预览。
6. 不得破坏当前 M3G 对象存储链路、file-access、安全禁出字段和 5173 前端主线。

## 当前裁决

- 可以确认：BIM 协同页当前可用，105 元数据模型展示可用。
- 不能确认：葛兰岱尔真实预览已接入。
- 不建议：直接进入 M3G-9。
- 建议：先做 `DT-F1`，再进入 M3G-9。

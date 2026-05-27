# 8B-GD 葛兰岱尔轻量化引擎接入路线

更新时间：2026-05-27

## 当前定位

`8B-GD` 是真实 BIM 轻量化引擎接入支线，厂商为 `葛兰岱尔轻量化引擎`。

本支线独立于当前 `M3G：NAS 侧 MinIO 对象存储接管真实项目文件` 主线推进，使用独立 worktree：

```text
/Users/vc/Documents/数字化交付平台-8b-gd
```

当前分支：

```text
codex/8b-gd-lightweight-engine-adapter
```

## 总原则

- 不在 M3G 分支上叠加 3D 引擎代码。
- 不修改对象存储迁移主线。
- 不让葛兰岱尔引擎直连 NAS、MinIO 底层目录或 MySQL。
- 模型文件只能由平台授权取用；首轮 PoC 采用平台后端分片上传，短时授权链接 / 票据拉取作为后续扩展。
- 前端只拿平台 viewer ticket，不拿真实 NAS 路径、bucket、object key、vendor token。
- RVT 优先 PoC，DWG / IFC / NWD / NWC 后续再扩展。

## 批次定义

### 8B-GD0：葛兰岱尔引擎对接握手

状态：`当前 active`

目标：

- 向引擎团队收集最小 HTTP API 契约。
- 明确健康检查、提交转换、查询任务、viewer 入口、认证、错误码、限制和样例。
- 锁定平台到引擎的数据流。
- 不写业务代码，不做真实转换。

交付物：

- `handoff/main-agent/8b-gd0-glandar-engine-handshake-plan.md`
- `handoff/main-agent/8b-gd0-glandar-engine-api-handoff-template.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`

当前接口文档评审结论：

- Station API 使用 `18086`。
- Station Web / 前端静态资源使用 `18087`。
- 文档确定支持 `SplitUploadFile` 分片上传和 `query-model-info` 状态查询。
- PoC 优先按“平台后端读取 StorageService 文件流并分片上传 Station”的方式接入。
- 原计划中的“引擎通过短时链接主动拉取模型”保留为后续扩展，除非引擎团队确认 `ModelUploadUrl` 可用。

### 8B-GD1：平台侧葛兰岱尔适配骨架

启动条件：

- 8B-GD0 接口清单完整。
- 引擎团队提供可访问的 LAN base URL、health API、任务 API、viewer API 或最小 curl。
- M3G 主线不被本支线工作区污染。

目标：

- 在 `delivery-visualization-adapter` 内增加厂商适配层，内部 engine code 使用 `GLANDAR`。
- 默认 `BIM_ENGINE_PROVIDER=MOCK`，只有显式配置为 `GLANDAR` 才启用真实对接。
- 保持现有 8A Mock / lightweight-status / lightweight-plan 兼容。
- 增加最小任务接口：创建转换任务、查询状态、获取受控 viewer 入口。
- 平台对 Station 的首选文件传递方式为分片上传，不让 Station 直连 NAS / MinIO 底层目录。

### 8B-GD2：105 RVT PoC 转换闭环

启动条件：

- 8B-GD1 通过测试。
- 105 项目中选定 1-3 个已登记、可授权的 RVT 样本。
- 平台可生成模型短时取用票据。

目标：

- 平台校验用户项目权限和文件查看权限。
- 平台提交葛兰岱尔转换任务。
- 平台后端通过 StorageService 读取模型文件流，并调用 Station `SplitUploadFile`。
- 转换结果登记为轻量化 / 预览产物元数据。
- viewer 入口通过平台授权页面或短时 viewer ticket 打开。

### 8C-GD：Viewer 嵌入与业务联动

启动条件：

- 8B-GD2 PoC 成功。
- 葛兰岱尔 viewer 可稳定内嵌或跳转。

目标：

- 模型集成页和项目工作台显示真实葛兰岱尔 viewer 入口。
- 支持打开模型、查看转换状态、回到项目上下文。
- 构件定位、高亮、剖切、测量、视点保存和图模联动后置。

## 禁止事项

- 不在 8B-GD0 写业务代码。
- 不修改 `docs/**`，除非主 agent 单独授权。
- 不修改 M3G 对象存储迁移逻辑。
- 不直接读取模型正文。
- 不移动、删除、重命名 NAS 文件。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不把 Mock 或 PoC 状态包装成生产级真实 3D 能力。

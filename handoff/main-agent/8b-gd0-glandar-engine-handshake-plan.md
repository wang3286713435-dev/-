# 8B-GD0：葛兰岱尔引擎对接握手计划

更新时间：2026-05-27

## 批次定位

`8B-GD0` 是真实葛兰岱尔轻量化引擎接入前的接口握手批次。

本批只做对接清单、边界冻结和交接模板，不写业务代码、不提交真实转换任务、不改 M3G 对象存储主线。

## 当前已知信息

- 引擎厂商：`葛兰岱尔轻量化引擎`
- 部署位置：公司局域网另一台电脑
- 对接方式：`HTTP API`
- PoC 格式：`RVT` 优先
- 模型来源：平台后端授权读取 StorageService 文件流后分片上传 Station；`ModelUploadUrl` 拉取模式后置确认
- 平台侧隔离分支：

```text
codex/8b-gd-lightweight-engine-adapter
```

- 平台侧隔离 worktree：

```text
/Users/vc/Documents/数字化交付平台-8b-gd
```

## 目标数据流

```text
用户在平台点击模型轻量化
-> 平台校验项目权限和文件查看权限
-> 平台后端通过 StorageService 读取 active object version / 受控文件流
-> 平台后端分片上传 Station API
-> 葛兰岱尔执行转换
-> 平台轮询或接收回调更新任务状态
-> 平台生成短时 viewer ticket
-> 前端通过平台 viewer 入口打开模型
```

## 必须向引擎团队确认的接口项

### 1. 基础服务

- 引擎服务内网 base URL
- health API 路径、方法和样例响应
- 服务版本 API 或版本字段
- 是否需要平台 IP 白名单
- HTTP / HTTPS 支持情况
- CORS / iframe / CSP 限制

### 2. 认证

- 认证方式：无认证 / Basic / Bearer token / API Key / 自定义签名
- Header 名称
- token 有效期
- token 刷新方式
- 是否区分转换 API 和 viewer API 的权限
- 是否支持平台服务账号

密钥、token、密码不得写入聊天、handoff 或 Git。

### 3. 提交转换任务

- endpoint method/path
- request content type
- 请求字段：
  - 平台任务 ID
  - 文件下载 URL / ticket
  - 文件名
  - 文件格式
  - 文件大小
  - checksum
  - 回调 URL
  - viewer 配置
- RVT 支持版本
- 是否支持压缩包 / 链接 / 分片
- 最大文件大小
- 并发限制
- 超时建议

### 4. 任务状态查询

- endpoint method/path
- 任务 ID 字段
- 状态枚举：
  - queued
  - downloading
  - converting
  - ready
  - failed
  - canceled
- 进度字段
- 错误码与错误消息
- 是否返回 viewer session / model id
- 状态保留时间

### 5. Viewer 入口

- viewer URL / session API
- 是否支持 iframe 内嵌
- 是否需要单独 viewer token
- viewer token 有效期
- viewer 是否支持内网访问
- viewer 入口是否依赖浏览器直连引擎机器
- viewer 失败时的错误码

### 6. 回调机制

- 是否支持 callback webhook
- 回调事件：
  - 下载成功
  - 下载失败
  - 转换开始
  - 转换进度
  - 转换成功
  - 转换失败
- 回调签名方式
- 回调重试策略
- 平台是否仍需要轮询兜底

### 7. 产物与清理

- 轻量化产物由引擎保存，还是平台保存
- 产物保留时长
- 是否可导出产物
- 清理 API
- 是否支持重新转换覆盖
- 是否支持同一文件版本幂等转换

## 已评审 API 文档结论

见：

```text
handoff/main-agent/8b-gd0-glandar-api-review.md
```

关键裁决：

- Station API：`18086`。
- Station Web / 前端静态资源：`18087`。
- 大模型使用 `POST /api/app/model/SplitUploadFile` 分片上传。
- 任务查询使用 `POST /api/app/model/query-model-info?LightweightName=...`。
- `status == 100` 表示可预览。
- `modelAccessAddress` 必须做可访问性校验，不能盲信历史 `18087` 地址。
- Station Token 只允许后端安全注入，不能进入前端、handoff 或 Git。

## 平台侧接口原则

前端只调用平台接口，不能直接调用葛兰岱尔 API。

平台对前端保持平台语义：

```http
GET  /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-status
GET  /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-plan
POST /api/visualization-adapter/projects/{projectId}/model-integrations/{integrationId}/lightweight-jobs
GET  /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}
POST /api/visualization-adapter/projects/{projectId}/lightweight-jobs/{jobId}:viewer-ticket
```

平台内部 adapter 再调用葛兰岱尔 API。

## 禁出字段

任何 handoff、API 响应、前端展示、审计日志、测试输出不得包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `bucket`
- `object_key`
- vendor token
- raw row
- SQL
- token
- secret
- password

## 8B-GD0 完成定义

- 引擎团队接口清单完整。
- 至少拿到 health API 和一个 submit/status/viewer 的最小样例，或明确缺口。
- 数据流和权限边界冻结。
- 没有业务代码改动。
- 没有 M3G 分支污染。
- 没有密钥写入仓库。

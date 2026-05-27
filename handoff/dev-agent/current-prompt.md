# 开发 Agent 当前任务：8B-GD0 葛兰岱尔引擎对接握手

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前分支：

`codex/8b-gd-lightweight-engine-adapter`

## 0. 本批定位

本批是：

`8B-GD0：葛兰岱尔引擎对接握手`

这是 3D / BIM 轻量化引擎接入支线，独立于当前 M3G 对象存储主线。

本批只做接口握手、边界冻结和 handoff 交接，不写业务代码。

引擎厂商已确定为：

`葛兰岱尔轻量化引擎`

对接方式：

`HTTP API`

PoC 范围：

`105 项目 RVT 模型优先`

数据流锁定为：

```text
平台校验权限
-> 平台生成短时模型取用票据 / 授权链接
-> 平台提交葛兰岱尔转换任务
-> 引擎通过平台授权链接拉取模型
-> 引擎转换
-> 平台查询 / 接收任务状态
-> 平台生成短时 viewer ticket
-> 前端通过平台 viewer 入口打开
```

## 1. 必须先阅读

开始前先阅读：

- `handoff/main-agent/8b-gd-roadmap.md`
- `handoff/main-agent/8b-gd0-glandar-engine-handshake-plan.md`
- `handoff/main-agent/8b-gd0-glandar-engine-api-handoff-template.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`
- `handoff/main-agent/phase2-batch8a-bim-lightweight-adapter-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

## 2. 严格边界

本批允许：

- 整理葛兰岱尔接口对接清单。
- 如果用户或引擎团队提供了接口文档，可把非密钥信息摘录到 handoff。
- 如果用户提供了内网 base URL，可只执行安全 health curl。
- 更新 `handoff/dev-agent/latest-report.md`。
- 必要时更新 `handoff/main-agent/8b-gd0-glandar-engine-api-handoff-template.md` 中的非密钥字段。

本批禁止：

- 禁止修改 `backend/**`。
- 禁止修改 `frontend/**`。
- 禁止修改 `scripts/**`。
- 禁止新增 Flyway 迁移。
- 禁止修改 `docs/**`。
- 禁止提交真实转换任务。
- 禁止读取 RVT / IFC / DWG / PDF / Office 正文。
- 禁止移动、删除、重命名 NAS 文件。
- 禁止访问 MinIO 底层目录。
- 禁止写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 禁止把 vendor token、secret、password 写入聊天、报告、Git 或日志。

## 3. 你要完成的内容

### A. 对接清单核对

根据 `8b-gd0-glandar-engine-api-handoff-template.md` 和 `8b-gd0-glandar-api-review.md`，确认是否已拿到以下信息：

- 引擎内网 base URL。
- health API。
- 认证方式。
- 提交转换任务 API。
- 查询任务状态 API。
- viewer URL / session API。
- callback 机制。
- 支持格式，尤其 RVT 版本。
- 文件大小、并发、超时限制。
- 错误码。
- 最小 curl 样例。

如果缺失，请在报告中列出缺口，不要自行猜测。

当前 API 文档已确认：

- Station API 端口为 `18086`。
- Station Web / 引擎静态资源端口为 `18087`。
- 大 RVT 优先走 `SplitUploadFile` 分片上传。
- 状态查询走 `query-model-info`。
- 成功口径为 `status == 100`。
- `modelAccessAddress` 必须校验客户端可访问性。

### B. 数据流边界确认

报告必须明确：

- 引擎不直接访问 NAS 共享目录。
- 引擎不直接访问 MinIO 底层对象目录。
- 8B-GD1 / GD2 优先由平台后端读取 StorageService 文件流并分片上传 Station。
- `ModelUploadUrl` / 引擎主动拉取短时链接仅作为后续扩展位。
- 前端只拿平台 viewer ticket。
- 平台对前端不暴露厂商 token、真实路径、bucket、object key。

### C. 最小健康检查

只有在用户提供了可访问的 base URL 且不需要密钥时，才允许执行：

```bash
curl -fsS <engine-health-url>
```

如果需要密钥，只记录“待安全注入”，不要要求用户把密钥发到聊天。

### D. 最新报告

完成后写入：

```text
handoff/dev-agent/latest-report.md
```

报告必须包含：

1. 已确认的引擎接口信息。
2. 仍缺失的信息。
3. 是否执行了 health curl。
4. 是否发现安全风险。
5. 是否修改了业务代码。
6. 是否可以进入 8B-GD1。
7. 是否接受“平台后端分片上传”为 PoC 首选链路。

## 4. 完成定义

只有以下条件满足，才能标记完成：

- `handoff/dev-agent/latest-report.md` 已写。
- 无 `backend/**`、`frontend/**`、`scripts/**`、`docs/**` 改动。
- 无密钥、token、密码写入仓库。
- 对接缺口清晰。
- 主 agent 可以基于报告判断是否进入 `8B-GD1`。

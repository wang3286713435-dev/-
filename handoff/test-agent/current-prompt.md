# 测试 Agent 当前任务：8B-GD0 葛兰岱尔引擎对接握手验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台-8b-gd`

当前验收批次：

`8B-GD0：葛兰岱尔引擎对接握手`

## 0. 验收目标

本轮只验收 8B-GD0 是否完成葛兰岱尔引擎对接握手准备。

8B-GD0 是文档 / handoff 批次，不应出现业务代码变更。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/8b-gd-roadmap.md`
- `handoff/main-agent/8b-gd0-glandar-engine-handshake-plan.md`
- `handoff/main-agent/8b-gd0-glandar-engine-api-handoff-template.md`
- `handoff/main-agent/8b-gd0-glandar-api-review.md`

## 2. 必跑命令

请执行：

```bash
cd /Users/vc/Documents/数字化交付平台-8b-gd
git status --short
git diff --name-only
git diff --cached --name-status
git diff --check
```

本批不要求跑后端构建、前端构建或业务回归，因为理论上不应改业务代码。

## 3. Git 范围检查

8B-GD0 允许包含：

- `handoff/main-agent/8b-gd-roadmap.md`
- `handoff/main-agent/8b-gd0-glandar-engine-handshake-plan.md`
- `handoff/main-agent/8b-gd0-glandar-engine-api-handoff-template.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`

8B-GD0 不允许包含：

- `docs/**`
- `backend/**`
- `frontend/**`
- `scripts/**`
- Flyway 迁移
- vendor token / secret / password
- 真实 NAS 路径
- 真实 bucket 名 / 真实 object key 值
- 真实转换任务调用代码

## 4. 核心验收点

重点确认：

1. 对接清单覆盖 Station API/Web、认证、分片上传、任务状态、viewer、callback、错误码、格式限制。
2. 明确平台后端分片上传为 PoC 首选链路，不让引擎直连 NAS / MinIO 底层目录。
3. 明确前端只拿平台 viewer ticket，不拿厂商 token。
4. 明确 8B-GD0 不写业务代码。
5. 明确 8B-GD1 的启动条件。
6. 开发报告写清已确认项和缺失项。

## 5. 禁出字段扫描

所有 handoff 和报告中不得出现真实敏感值：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- 真实 bucket 名
- 真实 object key 值
- raw row
- SQL
- token
- secret
- password

说明性文案中可以出现“短时授权链接”“对象存储”“viewer ticket”“bucket / object key 字段不得暴露”等概念，但不能出现真实密钥或底层定位值。

## 6. P0 / P1 判定

P0：

- 修改了 `backend/**`、`frontend/**` 或 `scripts/**`。
- 写入了 vendor token / secret / password。
- 引擎被要求直连 NAS、MinIO 底层目录或 MySQL。
- 把 8B-GD0 写成真实转换上线。

P1：

- 对接清单缺 health、submit、status、viewer 四类核心接口之一，且报告未说明。
- 未明确权限和短时取用票据边界。
- 未明确 8B-GD1 启动条件。
- 开发报告未写。

P2：

- 文案细节可读性一般，但不影响后续对接。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论：通过 / 不通过。
- P0 / P1 / P2。
- 必跑命令结果。
- Git 范围检查。
- 对接清单完整性判断。
- 安全边界检查。
- 禁出字段扫描结果。
- 是否建议主 agent 收口 8B-GD0。

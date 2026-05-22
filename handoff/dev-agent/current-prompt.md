# 开发 Agent 当前任务：M2F 真实项目交付闭环试运行

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前批次

`M2F：真实项目交付闭环试运行`

本批不是继续堆新功能，而是把 `105 / 503` 真实 NAS 项目中已人工确认的工程主数据真正放进交付链路里跑通。

核心链路：

`真实资产 -> 工程主数据 -> 交付标准 -> 文档/图纸应交项 -> 文件挂接 -> 审核/整改 -> 交付包草案`

## 1. 必须先阅读

- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
- `handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`
- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`

## 2. 本轮目标

确认并修复 105 真实项目从正式工程主数据进入交付闭环时的断点。

至少覆盖：

1. 105 的部位树、节点类型、交付物定义、交付物类型是否已经被交付页面正确识别。
2. 文档交付和图纸交付是否能基于正式规则展示应交项、缺失项或合理阻塞原因。
3. 缺失项解释是否面向业务用户，而不是只展示技术字段。
4. 文件补交 / 挂接是否仍需人工确认。
5. 审核、驳回、整改、复审是否不回归。
6. 交付包 / 档案目录草案是否能反映当前交付状态。

## 3. 开发策略

先审计链路，再做最小修复。

请先用接口或脚本确认当前 105 链路状态：

- 标准状态。
- 部位树。
- 节点类型。
- 交付物定义 / 类型。
- 文档交付视图。
- 图纸交付视图。
- 缺失项。
- 文件补交候选。
- 审核 / 整改。
- 交付包草案 / 档案目录。

发现断点时只修断点，不扩展新业务能力。

## 4. 必须新增或增强的脚本

新增：

`scripts/dev/check-m2f-real-project-delivery-loop.sh`

脚本至少验证：

1. 管理员登录并切换到 `503 / 105`。
2. 105 标准状态为人工确认后的正式状态。
3. 部位树、节点类型、交付物定义、交付物类型可查。
4. 文档 / 图纸交付视图能基于正式规则返回应交项或合理阻塞说明。
5. 缺失项解释包含交付定义、交付类型、目标和需要补的文件类型。
6. 文件补交 / 批量挂接接口仍要求明确人工确认或合法参数。
7. 审核 / 整改 / 交付包草案接口不回归。
8. 响应不包含 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、raw row、SQL、token、secret。

## 5. 允许修改范围

允许：

- `backend/**` 中与交付闭环断点直接相关的最小修复。
- `frontend/**` 中与 105 交付闭环可用性直接相关的文案、入口、空状态和提示修复。
- `scripts/dev/**` 新增或增强 M2F 专项脚本。
- `handoff/dev-agent/latest-report.md`。

## 6. 明确禁止

禁止：

- 真实 NAS 文件写入、移动、删除、重命名、复制。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 新增 Hermes 能力。
- 接入 BIM 引擎、构件解析、parser、writer、indexing、向量库。
- Agent 自动挂接、自动审核、自动整改。
- 让模板数据冒充真实工程结构。
- 启动 8B / 8C / 9A。
- 修改 `docs/**`。

## 7. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh
bash scripts/dev/check-m2c-delivery-package-archive.sh
git diff --check
```

除非你改动了明显影响文件权限或 NAS 写操作的代码，否则不要扩展成全量脚本回归。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 105 交付闭环审计结果。
2. 修复了哪些断点。
3. 是否新增 M2F 专项脚本。
4. 文档 / 图纸交付、缺失项、挂接、审核整改、交付包草案的当前状态。
5. 自测命令结果。
6. 是否触碰真实 NAS 文件。
7. 是否读取正文。
8. 是否新增 Hermes / BIM / parser / indexing 能力。
9. 已知风险和未完成事项。

## 9. 完成定义

只有同时满足以下条件，才能标记完成：

- 105 正式工程主数据能驱动文档 / 图纸交付链路。
- 缺失项和阻塞说明对员工可理解。
- 人工挂接、审核整改、交付包草案链路不回归。
- 未触碰真实 NAS 文件。
- 未读取正文。
- 未泄露真实路径或敏感字段。
- 构建、健康检查、M2F 专项脚本和必要回归通过。

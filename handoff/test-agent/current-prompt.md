# 测试 Agent 当前任务：M2F 真实项目交付闭环试运行验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 测试策略

本轮继续采用轻量测试策略，先阅读：

- `handoff/main-agent/lightweight-test-strategy.md`

不要做大范围浏览器逐页点击，不做多分辨率视觉巡检。只验证代码可用、接口契约、专项脚本、主线红线和是否偏离 M2F 目标。

## 0. 当前验收批次

`M2F：真实项目交付闭环试运行`

目标是验证 `105 / 503` 真实 NAS 项目已确认的工程主数据，能进入文档 / 图纸交付、缺失项、人工挂接、审核整改和交付包草案链路。

## 1. 必须先阅读

- `handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`

## 2. 必跑命令

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

如果没有 `scripts/dev/check-m2f-real-project-delivery-loop.sh`，记 P1。

除非发现 P0/P1，本轮不跑全量历史脚本。

## 3. 专项验收

以 `503 / 105` 为重点项目，确认：

1. 标准状态已处于 M2E 人工确认后的正式规则状态。
2. 部位树、节点类型、交付物定义、交付物类型可查。
3. 文档交付 / 图纸交付能基于正式规则返回应交项，或返回合理阻塞说明。
4. 缺失项解释必须包含：
   - 目标或部位。
   - 交付定义。
   - 交付类型。
   - 需要补什么文件。
   - 为什么现在缺失或阻塞。
5. 文件补交 / 挂接仍需要人工确认或合法参数，不能自动挂接。
6. 审核、驳回、整改、复审接口不回归。
7. 交付包草案 / 档案目录能反映当前交付状态。

如开发 agent 修改了关键 Vue 页面，可以做一次极短浏览器验收，只打开 105 文档交付或图纸交付页面确认不白屏、缺失项和入口清晰即可。

## 4. 红线检查

接口响应、页面文本、日志和新增脚本不得泄露：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storage_path`
- `storageUri`
- `storagePath`
- raw row
- SQL
- token
- secret
- password

## 5. 禁止通过的情况

P0：

- 真实 NAS 文件被移动、删除、改名、复制或读取正文。
- 真实 NAS 路径泄露。
- 权限绕过。
- 自动挂接、自动审核、自动整改。
- 启动 Hermes / BIM / parser / indexing 新能力。

P1：

- 没有 M2F 专项脚本。
- 105 已确认主数据不能驱动文档 / 图纸交付。
- 缺失项没有可理解解释。
- 挂接、审核整改或交付包草案主链路断裂。
- M2E 或 M2C 回归失败。

P2：

- 文案或视觉细节粗糙但不影响主链路。
- 既有 Vite chunk warning。

## 6. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论。
- P0/P1/P2。
- 必跑命令结果。
- M2F 专项脚本结果。
- 105 真实项目交付闭环状态。
- forbidden field 扫描结果。
- 是否做了浏览器短验；如果未做，说明按轻量策略跳过。
- 是否建议主 agent 收口 M2F。

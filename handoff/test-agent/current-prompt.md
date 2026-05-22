# 测试 Agent 当前任务：M2D 真实项目工程主数据接入草案增强验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 测试策略

本轮采用轻量测试策略，先阅读：

- `handoff/main-agent/lightweight-test-strategy.md`

本轮不要做大范围浏览器逐页点击，不要多分辨率视觉巡检。只验证代码可用、接口契约、专项脚本、主线红线和是否偏离 M2D 目标。

## 0. 当前验收批次

`M2D：真实项目工程主数据接入草案增强`

本轮目标是验证 105 真实 NAS 项目不再被模板演示数据误导，而是展示真实资产接入评估和工程主数据草案。

## 1. 必须先阅读

- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/project-105-template-reset-report.md`
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
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

如果开发 agent 新增：

`scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`

必须执行。

另外执行一条与交付包依赖直接相关的轻量回归：

```bash
bash scripts/dev/check-m2c-delivery-package-archive.sh
```

除非发现 P0/P1 或主 agent 另行要求，本轮不需要跑 M2B/M2A/M1F/M1E/M1D 全量脚本。

## 3. 专项验收

以 `503 / 105` 为重点项目，至少验证：

1. `GET /api/master-data/projects/503/standard-status`
   - 不应显示 `deliverableStandardReady=true`。
2. `GET /api/master-data/projects/503/initialization/status`
   - 应显示需要确认部位树、节点类型、交付标准。
3. `GET /api/master-data/projects/503/onboarding/assessment`
   - 返回真实资产统计。
   - 返回专业分布、扩展名分布、治理风险。
   - `evidenceMode=catalog_only`。
4. `GET /api/master-data/projects/503/onboarding/preview`
   - 草案项包含 evidenceSource / confidenceLevel / riskHint / pendingConfirmation。
   - 能看出哪些来自资产线索，哪些来自模板骨架。
5. 真实 NAS 项目不能一键应用模板后直接变成标准已就绪。
6. 文档交付 / 图纸交付在主数据未确认时不得显示虚假的应交完成状态。
7. 105 文档交付 / 图纸交付 / 交付包接口在主数据未确认时，不得显示演示模板产生的虚假应交项。
8. 如做浏览器短验，只打开 105 工程主数据页确认不白屏、文案不明显误导即可，不要逐页点击。

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
- 105 再次被一键模板误判为交付标准已就绪。
- 项目权限绕过。
- M2C / M2B / M2A / M1F / M1E / M1D / M1C 回归失败。

P1：

- 105 看不到真实资产统计。
- 草案项没有证据来源、置信度、风险提示或人工确认标记。
- 文档/图纸交付仍显示虚假应交项。
- 代码或接口仍把模板表达成真实项目结构。

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
- M2D 专项脚本或接口结果。
- 是否做了浏览器短验；如果未做，说明按轻量策略跳过。
- forbidden field 扫描结果。
- 是否建议主 agent 收口 M2D。

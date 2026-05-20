# 测试 Agent 当前任务：M1D 标准驱动交付闭环强化验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`M1D：标准驱动交付闭环强化`

注意：

- M1A/M1B/M1C 已收口。
- G4 已暂停。
- Hermes 定位已冻结。
- 本轮不进入 8B / 8C / 9A。
- 不测试真实 BIM 轻量化。
- 不测试真实 NAS 增删改查。
- 不测试正文抽取、selective indexing 或 Hermes memory。

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/test-agent/latest-report.md`
4. `handoff/main-agent/status.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
7. `handoff/main-agent/m1c-real-project-masterdata-closure.md`
8. `handoff/main-agent/m1d-standard-delivery-loop-plan.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

## 1. 验收目标

确认 M1D 把标准驱动交付能力串成完整员工可用闭环：

1. 文档交付和图纸交付能清楚展示标准状态、应交项、缺失项、待审、驳回、完整率和下一步动作。
2. 缺失项可以解释为什么缺、缺什么类型文件、目标是什么。
3. 补交文件仍使用分页远程选择。
4. 挂接、提交审核、审核通过、审核驳回、整改处理、关闭、重开、复审或重新补交链路可验证。
5. 关键动作后完整率和状态刷新稳定。
6. 导出预检查仍是 dry-run，不生成包、不触碰 NAS、不泄露路径。
7. 105 与至少另一个真实 NAS 项目都可打开并理解页面。
8. 不依赖 Hermes，也不新增 Hermes 能力。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch2-standard-delivery.sh
bash scripts/dev/check-phase2-batch3-review-rectification-report.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

如开发 agent 新增 M1D 专项脚本，例如 `scripts/dev/check-m1d-standard-delivery-loop.sh`，必须执行并记录。

## 3. API 验收

至少抽查项目：

- 105 对应内部项目 ID `503`。
- 93 对应内部项目 ID `506`，或当前环境另一个真实 NAS 项目。

检查：

1. 文档交付完整率 / 缺失项。
2. 图纸交付完整率 / 缺失项。
3. 批量挂接接口。
4. 提交审核 / 审核通过 / 审核驳回。
5. 整改列表、处理、关闭、重开。
6. 交付包 summary。
7. 交付包 export-precheck。

要求：

- 响应包含统一结构和 `traceId`。
- 权限按项目校验。
- 状态变更后完整率和缺失项可刷新。
- export-precheck 返回 `dryRun=true`、`packageGenerated=false`。
- 不返回真实 NAS 路径或底层存储字段。

## 4. 页面验收

必须使用 fresh browser context / 清空本地登录态后验证：

1. 打开 `/login`。
2. 使用样板账号登录。
3. 确认进入 `/data-steward/assets`。

然后至少打开：

- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/506/work/document-delivery`
- `/data-steward/assets/506/work/drawing-delivery`

页面检查：

- 能看到标准状态、完整率、缺失、待审、驳回和下一步动作。
- 缺失项说明清楚。
- 补交弹窗可打开，文件选择仍为分页远程查询。
- 审核、整改、记录和导出预检查入口仍可用。
- 导出预检查文案明确 dry-run，不生成包，不触碰 NAS。
- 项目工作台导航不丢失。
- 页面无白屏、500、横向撑爆。

## 5. 安全边界

不得出现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`

不得新增：

- Hermes 能力。
- 真实 NAS 写操作。
- 文件正文读取。
- 真实 BIM 轻量化。
- Agent 自动治理。

## 6. P0 / P1 / P2 判定

P0：

- Fresh login 失败。
- 文档交付或图纸交付无法打开。
- 挂接、提交审核、审核通过/驳回、整改链路主流程不可用。
- 未确认或未授权可以跨项目操作。
- 泄露真实 NAS 路径、raw row、SQL、token。
- 本轮继续新增 Hermes 或继续 G4。

P1：

- 交付页面仍看不懂应交、缺失、待审、驳回和下一步动作。
- 缺失项解释缺失或误导。
- 补交文件选择回退成全量加载。
- 审核/整改后完整率或状态不刷新。
- 导出预检查不能反映阻塞原因。
- 只对 503 / 105 有效，其他真实项目不可用。

P2：

- 文案、间距、视觉层级仍有粗糙感，但不阻塞标准交付闭环使用。

## 7. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 M1D 当前 active。
4. 是否确认 G4 暂停、Hermes 冻结。
5. P0 / P1 / P2 列表。
6. 必跑命令结果。
7. API 验收结果。
8. Fresh login 结果。
9. 503 / 506 页面抽查结果。
10. 挂接、审核、整改、完整率刷新验证结果。
11. 导出预检查 dry-run 验证结果。
12. 安全边界检查结果。
13. 是否建议收口 M1D。

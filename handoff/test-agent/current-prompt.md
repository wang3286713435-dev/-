# 测试 Agent 当前任务：M1E 文件管理连续工作体验与后台任务可追踪性验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`M1E：文件管理连续工作体验与后台任务可追踪性收口`

注意：

- M1A/M1B/M1C/M1D 已收口。
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
7. `handoff/main-agent/m1d-standard-delivery-loop-closure.md`
8. `handoff/main-agent/m1e-file-task-continuity-plan.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

## 1. 验收目标

确认 M1E 解决真实文件管理连续工作和后台任务追踪问题：

1. 文件管理能按项目恢复当前目录、筛选、分页和最近文件上下文。
2. 用户可重置文件管理视图。
3. 文件 ID、项目内部 ID、checksum 任务 ID 文案业务化。
4. 点击补 checksum 后，平台内能看到任务状态和对应文件。
5. 失败任务能显示脱敏失败原因，并可重试。
6. 任务列表 / 详情按项目权限过滤。
7. 105/503 与 93/506 文件管理页面可用。
8. 不泄露真实 NAS 路径，不触碰真实 NAS 文件。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
git diff --check
```

如开发 agent 新增 M1E 专项脚本，例如 `scripts/dev/check-m1e-file-task-continuity.sh`，必须执行并记录。

## 3. 页面验收

必须使用 fresh browser context / 清空本地登录态后验证：

1. 打开 `/login`。
2. 使用样板账号登录。
3. 确认进入 `/data-steward/assets`。

然后至少打开：

- `/data-steward/assets/503?tab=files`
- `/data-steward/assets/506?tab=files`

检查：

- 进入多层目录后离开页面，再回来能恢复目录位置。
- 设置关键词、文件类型、专业、扩展名、质量问题、分页后，离开再回来能恢复关键状态。
- 点击重置视图后，目录、筛选、分页恢复默认。
- 打开文件详情，能看到“平台文件ID”与文件名、扩展名、大小、更新时间一起展示。
- 项目标题优先展示业务项目编码和项目名称；如出现数字 ID，应标注为“平台内部ID”。
- 点击补 checksum 后，能看到任务状态、后台任务编号、文件名、平台文件 ID、进度、创建/更新时间。
- 失败任务能看到脱敏失败原因，并可重试。
- 页面无白屏、500、横向撑爆。

## 4. API 验收

检查：

1. `POST /api/data-steward/assets/checksum-jobs`
2. `GET /api/data-steward/assets/jobs?projectId=...&jobType=CHECKSUM_CALC`
3. `GET /api/data-steward/assets/jobs/{jobId}`
4. `POST /api/data-steward/assets/jobs/{jobId}:retry`

要求：

- 响应包含统一结构和 `traceId`。
- 任务能关联到项目和文件。
- 普通用户不能跨项目读取任务。
- 失败原因脱敏。
- 不返回真实 NAS 路径或底层存储字段。

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
- 文件管理无法打开。
- checksum 任务创建后完全不可见。
- 普通用户可跨项目读取任务。
- 泄露真实 NAS 路径、raw row、SQL、token。
- 本轮继续新增 Hermes 或继续 G4。

P1：

- 离开再回来无法恢复目录或筛选状态。
- 没有重置视图能力。
- 文件 ID / 任务 ID 仍是孤立数字，未绑定文件名和业务上下文。
- 失败任务看不到失败原因或无法重试。
- 只对 503 / 105 有效，其他真实项目不可用。

P2：

- 文件管理视图仍偏表格化、图标和布局还可继续优化，但不阻塞连续工作和任务追踪。

## 7. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 M1E 当前 active。
4. 是否确认 G4 暂停、Hermes 冻结。
5. P0 / P1 / P2 列表。
6. 必跑命令结果。
7. Fresh login 结果。
8. 503 / 506 页面抽查结果。
9. 文件管理状态恢复和重置视图验收结果。
10. checksum 任务状态、失败原因和重试验收结果。
11. API 验收结果。
12. 安全边界检查结果。
13. 是否建议收口 M1E。

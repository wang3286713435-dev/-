# 测试 Agent 当前任务：M1C 工程主数据真实项目落地验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`M1C：工程主数据真实项目落地`

注意：

- M1A/M1B 已收口。
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
7. `handoff/main-agent/m1b-project-workbench-usability-closure.md`
8. `handoff/main-agent/m1c-real-project-masterdata-plan.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

## 1. 验收目标

确认 M1C 把工程主数据从“模板演示”收敛为“真实项目接入流程”：

1. 真实项目接入向导能解释项目资产证据、缺口和下一步。
2. 草案预览不是单纯模板清单，而能说明证据来源、catalog-only 边界、风险和人工确认要求。
3. 未确认时不能应用草案。
4. 确认应用时保持幂等，不覆盖、不重复创建。
5. 应用后能继续进入部位树、节点类型、交付物标准和文档/图纸交付。
6. 105 与至少另一个真实 NAS 项目都适用，不允许为 105 写死。
7. 不依赖 Hermes，也不新增 Hermes 能力。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6a-project-initialization.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-mock.sh
git diff --check
```

如开发 agent 新增 M1C 专项脚本，例如 `scripts/dev/check-m1c-real-project-masterdata.sh`，必须执行并记录。

## 3. API 验收

至少抽查项目：

- 105 对应内部项目 ID `503`。
- 93 对应内部项目 ID `506`，或当前环境另一个真实 NAS 项目。

检查：

1. `GET /api/master-data/projects/{projectId}/initialization/status`
2. `GET /api/master-data/projects/{projectId}/onboarding/assessment`
3. `GET /api/master-data/projects/{projectId}/onboarding/preview`
4. `POST /api/master-data/projects/{projectId}/onboarding/apply`

要求：

- 响应包含统一结构和 `traceId`。
- assessment 能看到资产登记、文件类型统计、主数据状态、缺口和下一步。
- preview 能看到 catalog-only、人工确认、风险/警告。
- 不传确认字段时 apply 必须拒绝。
- 如果执行确认应用，只能在受控测试项目或确认不会破坏数据的项目上执行；对 105/503 和 93/506 优先做只读评估与预览。
- 重复应用不得重复创建同名同编码数据。
- 响应不得泄露真实 NAS 路径或底层存储字段。

## 4. 页面验收

必须使用 fresh browser context / 清空本地登录态后验证：

1. 打开 `/login`。
2. 使用样板账号登录。
3. 确认进入 `/data-steward/assets`。

然后至少打开：

- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/506/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/master-data/node-types`
- `/data-steward/assets/503/master-data/deliverable-standard`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`

页面检查：

- 页面标题和文案明确是“真实项目接入”，不是“套模板即完成”。
- 能看到真实资产摘要。
- 能看到目录线索、缺口、下一步动作。
- 能看到草案预览。
- 草案预览说明 catalog-only、不读取正文、不触碰 NAS。
- 应用按钮必须在预览后才可用。
- 应用前有明确人工确认。
- 应用后仍提示草案需要项目负责人复核。
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

## 6. 回归检查

确认不回归：

- 文件访问安全 4R。
- 项目初始化 6A。
- 批量挂接与交付包准备 6B。
- 导出预检查 7A。
- BIM Mock 入口 8A。
- 文档交付和图纸交付页面。

## 7. P0 / P1 / P2 判定

P0：

- Fresh login 失败。
- 真实项目接入向导无法打开。
- 未确认也能应用草案。
- 应用草案重复创建大量重复标准项。
- 泄露真实 NAS 路径、raw row、SQL、token。
- 本轮继续新增 Hermes 或继续 G4。

P1：

- 接入评估仍看不出真实项目资产证据。
- 草案预览仍像普通模板清单，没有证据、风险和 catalog-only 边界。
- 只对 503 / 105 有效，其他真实项目不可用。
- 应用后无法进入部位树、节点类型或交付物标准继续维护。
- 节点类型已锁定仍能被自动破坏。
- 页面让用户误以为模板草案等同真实工程结构。

P2：

- 文案、间距、视觉层级仍有粗糙感，但不阻塞真实项目接入理解。

## 8. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 M1C 当前 active。
4. 是否确认 G4 暂停、Hermes 冻结。
5. P0 / P1 / P2 列表。
6. 必跑命令结果。
7. API 验收结果。
8. Fresh login 结果。
9. 503 / 506 页面抽查结果。
10. 应用草案确认与幂等验证结果。
11. 安全边界检查结果。
12. 是否建议收口 M1C。

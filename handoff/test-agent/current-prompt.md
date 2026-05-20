# 测试 Agent 当前任务：M1B 项目工作台与数据管家可用性验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`M1B：项目工作台与数据管家可用性收口`

注意：

- M1A 已收口。
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
7. `handoff/main-agent/m1a-platform-baseline-closure.md`
8. `handoff/main-agent/m1b-project-workbench-usability-plan.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

## 1. 验收目标

确认 M1B 让普通员工更容易理解平台入口和下一步动作：

1. Fresh login 后进入资产总览，页面用途清楚。
2. 资产总览真实项目统计不再明显错误。
3. 真实 NAS 项目、样例 / 测试 / 历史项目不会混淆。
4. 项目卡片 / 列表能看出项目来源、接入状态、主数据状态、交付状态和下一步动作。
5. 项目工作台能解释数据管家、工程主数据、工作中心三类入口。
6. 文件管理、工程主数据、文档交付、图纸交付入口不回归。
7. 不依赖 Hermes 也能完成基础使用判断。

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
git diff --check
```

如开发 agent 新增 M1B smoke，必须执行并记录。

## 3. 页面验收

必须使用 fresh browser context / 清空本地登录态后验证：

1. 打开 `/login`。
2. 使用样板账号登录。
3. 确认进入 `/data-steward/assets`。

然后至少打开：

- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/master-data/deliverable-standard`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/506`

说明：业务编码 `105` 对应内部项目 ID `503`，业务编码 `93` 对应内部项目 ID `506`。

## 4. 重点检查

### A. 资产总览

确认：

- 页面第一屏能说明这是项目资产入口。
- 真实项目统计不为明显错误值。
- 项目来源、接入状态、主数据状态、交付状态可读。
- 用户能看懂下一步动作。
- 测试 / 样例 / 历史项目不会默认干扰真实项目判断。

### B. 项目工作台

确认：

- 顶部显示当前项目上下文。
- 数据管家 / 工程主数据 / 工作中心的用途说明清楚。
- 不需要依赖 Hermes 才能理解基础操作。
- 项目内导航不丢失。

### C. 文件管理与交付入口

确认：

- 文件管理仍可进入。
- 目录树和文件表不回归。
- 工程主数据入口仍可进入。
- 文档 / 图纸交付仍可进入。
- 页面无白屏、500、横向撑爆。

### D. 安全边界

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

## 5. P0 / P1 判定

P0：

- Fresh login 失败。
- 资产总览无法打开。
- 真实项目无法进入项目工作台。
- 文档 / 图纸交付主链路不可用。
- 工程主数据主链路不可用。
- 泄露真实 NAS 路径、raw row、SQL、token。
- 本轮继续新增 Hermes 或继续 G4。

P1：

- 真实项目统计仍明显错误。
- 只对 503 / 105 有效，其他真实项目不可用。
- 项目来源 / 接入状态 / 主数据状态 / 交付状态表达混乱。
- 文件管理目录和文件表不对应。
- 审核、整改、导出预检查入口回归。
- 页面必须依赖 Hermes 才能理解基础操作。

P2：

- 文案、间距、视觉层级仍有粗糙感，但不阻塞主线平台使用。

## 6. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 M1A 已收口、M1B 当前 active。
4. 是否确认 G4 暂停、Hermes 冻结。
5. P0 / P1 / P2 列表。
6. 必跑命令结果。
7. Fresh login 结果。
8. 资产总览验收结果。
9. 503 / 506 页面抽查结果。
10. 安全边界检查结果。
11. 是否建议收口 M1B。

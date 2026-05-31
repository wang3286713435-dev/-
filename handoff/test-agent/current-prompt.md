# 测试 Agent 当前任务：DT-F1 origin/main 葛兰岱尔能力同步验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只验收：

`DT-F1：BIM 协同 / 葛兰岱尔适配兼容收口`

## 1. 背景

开发 agent 应已完成：

- 在当前 M3G 分支做 checkpoint commit。
- 将 `origin/main` 的葛兰岱尔轻量化试点能力合并到当前分支。
- 保留 M3G 对象存储、file-access、文件管理和迁移任务能力。

本轮验收重点不是新开发 BIM 大功能，而是确认“合并无损”。

## 2. 必读

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/m3g9-pre-bim-collab-compatibility-check.md`

## 3. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health || curl -fsS http://127.0.0.1:18088/actuator/health

bash scripts/dev/check-phase2-batch4-file-access.sh
```

如果存在这些脚本，也必须执行：

```bash
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh
```

如脚本不存在，记录“不存在，跳过原因”。不要临时创建假脚本。

最后执行：

```bash
git diff --check
git diff --cached --check
```

## 4. Git 验收

必须确认：

1. 当前分支包含 `origin/main` 最新葛兰岱尔提交 `d69cbc2`。
2. 开发 agent 报告中有 checkpoint commit hash。
3. 没有提交 `.claude/**`、`CLAUDE.md`、`tmp/**`、本地 jar / pid / log。
4. 没有丢失 M3G 脚本。

建议命令：

```bash
git merge-base --is-ancestor d69cbc2 HEAD && echo OK
git status --short --branch
```

## 5. 浏览器短验

启动或复用前端后检查：

- `http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`
- BIM 协同页能打开。
- 105 项目可见。
- 10 个 RVT 试点模型或轻量化模型列表可见。
- 可选择模型。
- 可打开 Viewer，或在配置不可用时显示明确业务错误。
- Viewer 不是旧的“暂无模型预览”。
- M3G 文件管理页仍能打开。
- 文件管理对象存储状态仍显示正常。

## 6. 安全与禁出字段

抽查 BIM / Viewer / file-access / storage-status 响应，不得出现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- bucket
- object key
- token
- secret
- SQL
- raw row

## 7. P0 / P1 / P2 判定

P0：

- 合并导致 M3G 对象存储主链路不可用。
- BIM 协同页白屏。
- 105 试点模型完全不可见。
- file-access 回归失败。
- 泄露路径、bucket、object key、token、secret。
- 丢失 M3G 关键脚本或对象化能力。

P1：

- `d69cbc2` 未进入当前分支。
- 没有 checkpoint commit，M3G 未提交改动处于高风险状态。
- 10 个 RVT 试点模型不可见，但页面仍可用。
- Viewer 入口不可用且无明确业务错误。
- M3G-8 或 M3A/M3B/M3C 关键回归失败。
- 新增关键交付文件未纳入 Git 跟踪。

P2：

- 既有 Vite chunk warning。
- 页面文案或视觉可优化但不影响链路。
- 非交付未跟踪项仍存在但未被纳入 Git。

## 8. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 结论：通过 / 不通过。
- P0 / P1 / P2。
- 是否包含 `d69cbc2`。
- checkpoint commit 是否存在。
- BIM 协同页验证结果。
- 105 试点模型 / Viewer 验证结果。
- M3G 对象存储与 file-access 回归结果。
- 禁出字段扫描结果。
- 是否建议主 agent 收口 DT-F1。

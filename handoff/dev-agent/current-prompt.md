# 开发 Agent 当前任务：DT-F1 无损同步 origin/main 葛兰岱尔能力到当前 M3G 分支

你是卓羽智能数据中台的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮批次：

`DT-F1：BIM 协同 / 葛兰岱尔适配兼容收口`

## 0. 已确认事实

主 agent 已执行 `git fetch origin main --prune` 并确认：

- `origin/main` 最新提交为：`d69cbc2 feat: integrate Glandar RVT lightweight preview pilot...`
- `d69cbc2` 已存在于 `origin/main`。
- 当前工作分支仍是：`codex/m3g-nas-minio-real-project-object-storage`。
- 当前工作区有大量 M3G 未提交改动，不能覆盖、不能重置、不能丢。

所以本轮不是继续开发新 BIM 功能，而是：

> 把已经合入 `origin/main` 的葛兰岱尔轻量化试点能力，无损同步到当前 M3G 分支，同时保护 M3G 对象存储、file-access、文件管理和迁移任务能力。

## 1. 必须先阅读

- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3g9-pre-bim-collab-compatibility-check.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

如果当前分支缺少 `origin/main` 中新增的葛兰岱尔 handoff 文件，不要直接判定失败；以 `origin/main` 为准查看。

## 2. 第一阶段：合并前保护工作区

先执行并记录：

```bash
git status --short --branch
git log --oneline --decorate -8
git log --oneline --decorate origin/main -8
git merge-base --is-ancestor d69cbc2 origin/main && echo "Glandar PR is in origin/main"
```

当前工作区如果仍有未提交 M3G 改动，必须先做 checkpoint commit。

### checkpoint 要求

允许提交：

- M3G / M3G-6 / M3G-7 / M3G-8 相关后端、前端、脚本、handoff 文件。
- DT-F1 当前 prompt / 状态文件。

禁止提交：

- `.claude/**`
- `CLAUDE.md`
- `tmp/**`
- `tmp/run-logs/**`
- 本地 jar / pid / log
- 本机密钥、token、license、NAS 真实路径配置

checkpoint commit 建议信息：

`chore: checkpoint M3G before Glandar mainline sync`

注意：

- 不允许 `git reset --hard`。
- 不允许 `git checkout -- .`。
- 不允许为了合并方便丢弃当前 M3G 改动。

## 3. 第二阶段：同步 origin/main

checkpoint 完成后，执行：

```bash
git merge origin/main
```

如果发生冲突，按下面规则解决。

### A. M3G 对象存储 / 文件管理 / file-access 冲突

优先保留当前 M3G 实现。

重点保护：

- NAS 侧 MinIO provider health
- storage status
- objectification / migration task
- file-access 对象优先读取
- 文件管理器对象化状态展示
- M3G 相关脚本

不得删除葛兰岱尔新增入口。

### B. BIM 协同 / 葛兰岱尔冲突

优先保留 `origin/main` 中葛兰岱尔能力。

重点保护：

- `glandar/rvt-pilot-files`
- `lightweight-jobs`
- `viewer-ticket`
- `GlandarViewerCanvas`
- `GlandarModelPreviewPage`
- BIM 协同页内嵌 Viewer
- 105 项目 10 个 RVT 试点模型列表

不得把 Viewer 恢复成旧的“暂无模型预览”占位。

### C. 路由冲突

重点检查：

- `frontend/src/router/index.ts`

必须保证：

- `/bim-collaboration`
- `/visualization/glandar-viewer`
- 旧 BIM 协同入口
- 项目工作台入口

都可正常访问。

## 4. 合并后验收目标

### 默认模式

不注入葛兰岱尔配置时：

- 后端可启动。
- BIM 协同页可打开。
- 页面不白屏。
- 默认 MOCK / 元数据适配不报错。
- 没有真实引擎配置时返回业务化提示，不返回 500。

### 葛兰岱尔模式

注入 GLANDAR 配置后：

- 105 / `projectId=503` 能看到 10 个 RVT 试点模型或轻量化模型列表。
- 已轻量化模型能打开平台内 Viewer。
- Viewer 不暴露 token。
- Viewer 不暴露 NAS 路径、bucket、object key、storage_uri。

### M3G 回归

必须确认：

- 对象存储 provider health 不回归。
- storage-status 不回归。
- storage migration task 不回归。
- file-access 不回归。
- NAS 侧 MinIO 配置不回归。
- 文件管理器对象化状态不回归。

## 5. 必跑命令

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health || curl -fsS http://127.0.0.1:18088/actuator/health

bash scripts/dev/check-phase2-batch4-file-access.sh
```

如果当前分支存在以下脚本，也必须跑：

```bash
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-8c-gd1-ten-rvt-platform-preview.sh
```

如果某个脚本不存在，记录“不存在，跳过原因”，不要临时乱造同名脚本。

最后执行：

```bash
git diff --check
git diff --cached --check
```

## 6. 浏览器短验

合并后启动或复用前端，检查：

- `http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`
- 能看到 BIM 协同管理页。
- 能看到 105 项目 10 个 RVT 试点模型或轻量化模型列表。
- 能选择已轻量化模型。
- 能打开 Viewer 或得到明确业务错误。
- Viewer 不是旧的“暂无模型预览”。
- M3G 文件管理页仍能打开。
- 文件管理对象存储状态仍显示正常。

## 7. 收口报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前分支名。
2. checkpoint commit hash。
3. 合并来源：`origin/main`。
4. 是否包含 `d69cbc2`。
5. 是否保留 M3G 对象存储能力。
6. 冲突文件与解决策略。
7. 路由验证结果。
8. BIM 协同页验证结果。
9. 105 试点模型 / Viewer 验证结果。
10. M3G 回归结果。
11. 禁出字段检查结果。
12. 未完成事项。

## 8. 完成定义

只有同时满足以下条件，才能标记完成：

- 当前 M3G 工作成果已 checkpoint，不丢改动。
- `origin/main` 已合并到当前分支。
- 葛兰岱尔能力在当前分支可见。
- BIM 协同页可打开。
- 105 RVT 试点模型或轻量化模型列表可见。
- M3G 对象存储、file-access、文件管理不回归。
- 不泄露 NAS 路径、bucket、object key、storage_uri、token、secret。
- `handoff/dev-agent/latest-report.md` 已写。

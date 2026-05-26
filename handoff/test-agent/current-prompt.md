# 测试 Agent 当前任务：M3C 对象存储迁移任务中心与批量策略验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮验收批次：

`M3C：对象存储迁移任务中心与批量策略`

本轮只验收对象存储迁移任务中心、批量策略、幂等、禁出字段和回归。不验收全量 NAS 搬迁，不验收 Hermes 正文问答，不验收语义索引，不验收 BIM/parser。

## 0. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`
- `handoff/main-agent/m3c-object-storage-migration-task-center-plan.md`

## 1. 必跑命令

请执行并记录：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 2. 专项验收重点

### A. 任务中心能力

确认：

- 任务可创建。
- 任务列表可查。
- 任务详情可查。
- 行级结果可查。
- 失败或可重试任务可重试。
- 任务行包含 `assetUuid`。
- 成功、失败、跳过、已对象化原因可解释。

### B. 批量策略

确认：

- 只能显式选择 `fileIds`。
- 不存在目录一键全量迁移。
- 不存在项目全量自动迁移。
- 单次数量上限生效。
- 单文件大小上限生效或有清晰配置。
- 跨项目文件被拒绝。
- 已删除 / 回收站 / 路径不可读文件被拒绝或跳过且有业务原因。

### C. 幂等

确认：

- 已对象化文件重复迁移返回 `ALREADY_STORED` / `SKIPPED` 或等价业务状态。
- 重复迁移不会重复污染 active 对象版本。
- `storage-status` 与任务行结果一致。

### D. 前端

如有新增页面或入口，浏览器短验：

- 能打开任务中心。
- 能看到任务列表。
- 能打开任务详情。
- 能看到平台资产ID。
- 页面文案明确“NAS 原文件保留 / 只做对象存储镜像 / 不代表 Hermes 已理解正文”。
- 不展示真实 NAS 路径、bucket、object key。

### E. Git 跟踪状态

必须检查：

- 本轮新增 Flyway migration 如存在，不能是 `??`。
- 本轮新增专项脚本不能是 `??`。
- 本轮新增前端/后端关键文件不能是 `??`。
- 使用 `git status --short` 和 `git ls-files` 双重确认。

这是 M3C-1 曾出现过的 P1，本轮必须作为固定验收项。

## 3. 禁出字段扫描

抽查新增/修改响应和前端展示，不得包含：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- bucket
- object key
- raw row
- SQL
- token
- secret
- password

注意：对象存储任务中心可以展示“对象已存储 / MinIO / S3-compatible”等业务状态，但不能展示 bucket、object key 或真实底层 URI。

## 4. 回归边界

必须确认：

- M3C-1 assetUuid / storage-status 不回归。
- M3B 小样本对象存储镜像不回归。
- M3A StorageService 不回归。
- M2J / M2I / M2H 文件治理与文件管理器不回归。
- file-access 权限、票据、路径脱敏不回归。
- 未触碰真实业务 NAS 文件。
- 未新增 Hermes 正文问答。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未修改仓库 `docs/**`。

## 5. P0 / P1 / P2 判定

### P0

- 任务中心触发全量 NAS 搬迁。
- 真实业务 NAS 文件被移动、删除、重命名或改写。
- API / 前端泄露真实 NAS 路径、bucket、object key、`storage_uri`、token、secret。
- file-access 权限链路被破坏。
- 重复迁移污染 active 对象版本。
- 跨项目文件可被迁移。

### P1

- 任务列表 / 详情 / 重试任一核心能力不可用。
- 任务行缺少 `assetUuid`。
- 成功 / 失败 / 跳过原因不可解释。
- 数量上限或项目归属校验不生效。
- M3C 专项脚本失败。
- M3C-1 / M3B / M3A 回归失败。
- 本轮新增关键文件仍为 `??`。

### P2

- 既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。
- 轻微文案优化项，不影响任务中心安全和可用性。

## 6. 报告要求

写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2 列表。
- 必跑命令结果。
- M3C 专项脚本结果。
- 任务创建 / 列表 / 详情 / 重试验证结果。
- 批量策略和幂等验证结果。
- `assetUuid` 验证结果。
- 禁出字段扫描结果。
- Git 跟踪状态检查结果。
- 是否建议收口 M3C。

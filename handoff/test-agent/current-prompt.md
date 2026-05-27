# 测试 Agent 当前任务：M3G-1 NAS 侧 MinIO 就绪检查与对象化盘点验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收批次：

`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`

## 0. 验收目标

本轮只验收 M3G-1：

- 平台能业务化展示 NAS 侧 MinIO readiness。
- 当前对象存储是本机开发 MinIO 还是 NAS 侧 MinIO 不被混淆。
- 平台能查询全项目对象化覆盖率。
- 平台能对单项目生成对象化 dry-run 计划。
- dry-run 不启动真实迁移，不复制文件。

本轮不验收：

- 历史文件真实批量迁移。
- 全量 NAS 搬迁。
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch。
- 文件正文读取。
- 真实 BIM 引擎。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md`
- `handoff/main-agent/m3g1-task-graph.md`
- `handoff/main-agent/m3g1-nas-minio-ops-preparation.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`

## 2. 必跑命令

请执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如后端未运行，可按项目已有方式启动后重试健康检查和专项脚本。

## 3. Git 范围检查

请检查：

```bash
git status --short
git diff --name-only
git diff --cached --name-status
```

M3G-1 允许包含：

- `backend/**` 中 readiness、inventory、dry-run、storage migration 查询相关最小改动。
- `frontend/**` 中对象存储状态、覆盖率、dry-run 计划展示相关最小改动。
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `handoff/dev-agent/latest-report.md`
- 必要的新迁移。

M3G-1 不允许包含：

- `docs/**`
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch / parser / indexing。
- 真实 BIM 引擎。
- 历史文件真实批量迁移执行入口绕过 dry-run。
- 真实 NAS 批量移动、删除、重命名能力扩展。

## 4. 核心验收点

重点确认：

1. NAS 侧 MinIO readiness 接口可用。
2. readiness 响应不泄露 endpoint、bucket、object key、secret。
3. 如果当前仍是本机 MinIO，响应必须清楚表达 `LOCAL_DEV_MINIO` 或等价业务状态，不能冒充 NAS 侧 MinIO。
4. 全项目对象化覆盖率可查。
5. 至少包含一个真实项目聚合，例如 105 / 503 或 93 / 506。
6. 单项目 dry-run 可生成计划。
7. dry-run 返回 `dryRun=true`、`migrationStarted=false`。
8. dry-run 不创建真实迁移任务、不复制文件。
9. dry-run sampleItems 不含真实 NAS 路径、bucket、object key。
10. M3F 新文件对象存储优先写入不回归。
11. file-access 不回归。

## 5. 禁出字段扫描

所有 M3G-1 响应和脚本输出中不得出现：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- bucket 真实值
- `object_key`
- `objectKey`
- raw row
- SQL
- token
- secret
- password

说明性文案可以出现“bucket / object key 不展示”这类安全说明，但不能出现真实值。

## 6. 浏览器轻量检查

本轮不要求全量浏览器逐页点击，只做轻量检查：

- 打开对象存储 / 文件服务 / 迁移任务相关页面。
- 页面不白屏。
- 能看到对象存储就绪状态或覆盖率入口。
- dry-run 入口如果已做前端，则点击后只生成计划，不启动迁移。
- 不需要在真实业务目录执行写操作。

## 7. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、覆盖或改名。
- dry-run 实际启动了迁移或复制文件。
- 响应泄露真实路径、bucket、object key、token、secret。
- 引入 Hermes 正文问答、parser、indexing、documents / chunks。
- file-access 权限链路回归失败。

P1：

- readiness 无法区分本机 MinIO 和 NAS 侧 MinIO。
- 全项目对象化覆盖率不可查。
- 单项目 dry-run 不可用。
- dry-run 响应缺少 `dryRun=true` 或 `migrationStarted=false`。
- M3G-1 专项脚本失败。
- M3F / M3E / M3C / file-access 关键回归失败。
- M3G-1 专项脚本未纳入 Git。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项，只记录，不阻塞。
- UI 文案细节粗糙但不影响主链路。

## 8. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论：通过 / 不通过。
- P0 / P1 / P2。
- 必跑命令结果。
- M3G-1 专项脚本结果。
- readiness 验证结果。
- 全项目对象化覆盖率验证结果。
- dry-run 计划验证结果。
- 是否发生真实迁移，必须说明。
- 禁出字段扫描结果。
- Git 范围检查结果。
- 是否建议主 agent 收口 M3G-1。

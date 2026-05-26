# M3C 对象存储迁移任务中心与批量策略验收报告

生成时间：2026-05-26

## 1. 测试结论

结论：M3C 验收通过，建议主 agent 进入 M3C 收口判断。

本轮重新按 `handoff/test-agent/current-prompt.md` 执行 M3C 正式验收。后端构建、前端构建、健康检查、M3C 专项脚本、M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归和 `git diff --check` 均通过。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮对象存储迁移任务中心验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项仍存在，未纳入本轮判断。
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`、`handoff/main-agent/m3c-object-storage-migration-task-center-plan.md`、`handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md` 当前仍为未跟踪交接文档；它们不是运行期关键文件，但建议主 agent 确认是否需要纳入 checkpoint。
- 浏览器页面安全提示中出现了 `bucket` / `object key` 字面词，语义是“不会展示底层路径、bucket 或 object key”；未发现实际 bucket 名称、object key 值或底层对象定位泄露。如后续禁出字段扫描改为纯字符串零命中，可把文案调整为“底层对象定位信息”。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`
- `handoff/main-agent/m3c-object-storage-migration-task-center-plan.md`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh`：通过，`PASS=15 FAIL=0`。
- `bash scripts/dev/check-m3b-object-storage-mirror-trial.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3a-storage-service-foundation.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m2j-105-ownership-review.sh`：通过，`PASS=6 FAIL=0`。
- `bash scripts/dev/check-m2i-105-file-ownership-governance.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m2h-windows-file-manager.sh`：通过，`PASS=53 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 5. M3C 专项脚本结果

`scripts/dev/check-m3c-storage-migration-task-center.sh` 通过，`PASS=9 FAIL=0`。

覆盖点：

- 管理员登录并切换项目成功。
- 对象存储迁移总览接口可访问，响应不暴露底层路径。
- 使用 `/tmp` 隔离测试文件资源，不触碰真实业务 NAS 目录。
- 显式 `fileIds` 创建迁移任务成功。
- 任务详情行级结果返回 `assetUuid` 和对象化状态。
- 任务列表包含新建任务，且只展示业务状态。
- 重复迁移被幂等跳过，未重复创建 active 对象版本。
- 缺失源文件失败返回业务化原因，未暴露真实路径。
- 失败任务可重试，重试结果仍只返回安全字段。
- M3C 专项脚本已纳入 Git 跟踪。

## 6. 任务创建 / 列表 / 详情 / 重试验证结果

通过。

- 任务可创建：专项脚本通过受控显式文件选择创建迁移任务。
- 任务列表可查：列表中可见新建任务，仅展示业务状态。
- 任务详情可查：详情包含行级结果、`assetUuid`、`storageState`、`resultCode`。
- 失败任务可重试：缺失源文件场景失败原因清晰，重试后仍安全返回，不暴露真实路径。

## 7. 批量策略和幂等验证结果

通过。

- 只能通过显式 `fileIds` 创建任务。
- 未发现目录一键全量迁移或项目全量自动迁移入口。
- 单次文件数量上限在前端提示为 10 个，并由脚本覆盖策略行为。
- 单文件大小上限在前端提示为 10 MB。
- 幂等验证通过：已对象化文件重复迁移返回跳过/已对象化语义，未重复污染 active 对象版本。
- `storage-status` 与任务行结果保持一致。

## 8. assetUuid 验证结果

通过。

- M3C 任务行包含 `assetUuid`。
- M3C-1 回归脚本再次验证新建文件资源返回稳定 `assetUuid`。
- 数据库 `asset_uuid` 与 API 返回一致。
- Catalog 文件列表、详情、搜索和 `storage-status` 均可返回 `assetUuid`。
- `FileAssetView` / `ModelAssetView` 继续输出 `asset_uuid`。

## 9. 浏览器短验结果

已打开：

- `http://127.0.0.1:5173/data-steward/assets/503/data-steward/storage-migration`

结果：

- 页面可正常打开，无白屏。
- 可见项目内导航中的 `对象存储` 入口。
- 页面标题为“文件服务与对象存储”。
- 可见 `对象存储迁移` tab。
- 可见对象存储汇总：文件总数、已对象化、仍在 NAS、异常任务、覆盖率。
- 可见“创建迁移任务”区域，说明“只能显式选择文件；单次最多 10 个文件，单文件上限 10 MB”。
- 可见项目文件选择表，展示“平台资产ID”、文件名、类型、大小、状态。
- 可见任务列表区域。
- 可见安全文案：NAS 原文件保留、当前只做对象存储镜像、不生成语义证据、不代表 Hermes 已理解文件正文。
- 页面未发现 `/Volumes`、`/Users`、`smb://`、`nas://`、`storage_uri`、`storageUri`、raw row、SQL、token、secret、password。

补充说明：页面安全文案中有 `bucket` / `object key` 字面词，用于说明“不展示这些底层定位”。本轮未发现实际 bucket 名称或 object key 值泄露。

## 10. 禁出字段扫描结果

通过。

M3C 专项脚本对 summary、create、list、detail、rerun、failure、retry 响应做了 forbidden-field scan，未发现：

- `/Volumes`
- `/Users`
- `nas://`
- `smb://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- bucket / object key 实际底层定位
- raw DB row
- SQL
- secret / password / token

浏览器短验也未发现真实 NAS 路径或底层对象定位值泄露。

## 11. Git 跟踪状态检查结果

通过。

`git status --short` 与 `git ls-files --error-unmatch` 双重确认：

- `scripts/dev/check-m3c-storage-migration-task-center.sh`：状态为 `A`，已纳入 Git 跟踪。
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh`：状态为 `A`，已纳入 Git 跟踪。
- `backend/delivery-app/src/main/resources/db/migration/V30__m3c1_asset_uuid_storage_status.sql`：状态为 `A`，已纳入 Git 跟踪。
- `StorageMigrationApplicationService.java`、`StorageMigrationController.java`、`dataSteward.ts`、`DataStewardFileServicePage.vue`、`frontend/src/router/index.ts` 均为已跟踪文件的修改，不是 `??`。

本轮 M3C 未新增 Flyway 迁移，复用既有 M3A/M3B/M3C-1 表结构。

## 12. 回归边界

已确认：

- M3C-1 assetUuid / storage-status 不回归。
- M3B 小样本对象存储镜像不回归。
- M3A StorageService 不回归。
- M2J / M2I / M2H 文件治理与文件管理器不回归。
- file-access 权限、票据、路径脱敏不回归。
- 未发现真实业务 NAS 文件被移动、删除、重命名或改写。
- 未新增 Hermes 正文问答。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未修改仓库 `docs/**`。

## 13. 是否建议收口 M3C

建议主 agent 进入 M3C 收口判断。

M3C 核心能力、批量策略、幂等、安全字段、Git 跟踪和回归脚本均通过；仅有 P2/记录项，不阻塞收口。

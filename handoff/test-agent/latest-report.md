# M3G-2 105 项目历史文件对象化上传灰度验收报告

生成时间：2026-05-28

## 1. 测试结论

结论：M3G-2 验收通过，建议主 agent 进入 M3G-2 收口判断。

本轮严格按 `handoff/test-agent/current-prompt.md` 执行。由于 `scripts/dev/check-m3g2-105-objectification-gray.sh` 是执行型脚本，会继续从 105 项目 `NAS_ONLY` 文件中追加对象化副本，本轮测试 agent 未再次运行该脚本；改为基于开发报告、只读 API、数据库状态、任务详情和非执行型回归脚本完成验收。

核心结果：

- M3G-2 专项脚本已纳入 Git，开发 agent 最近运行记录为 `PASS=23 FAIL=0`。
- 开发报告记录的 105 小批灰度任务 `taskId=143` 成功对象化 3 个文件，`success=3 / skipped=0 / failure=0`。
- 重复任务 `taskId=144` 幂等跳过 3 个文件，`success=0 / skipped=3 / failure=0`。
- `fileId=936/937/938` 当前均为 `OBJECT_STORED`，且通过受控 `file-access` 下载验证。
- 当前另选 `fileId=942` 仍为 `NAS_ONLY`，且通过受控 `file-access` 读取，证明 NAS_ONLY 链路保留。
- 本轮测试未再次触发 M3G-2 执行型对象化脚本，未追加真实 105 历史文件对象化批次。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项未纳入本轮判断。
- 开发报告中作为 NAS_ONLY 对照的 `fileId=939` 当前已是 `OBJECT_STORED`，不再适合作为 NAS_ONLY 对照；本轮另选当前仍为 `NAS_ONLY` 的 `fileId=942` 验证 NAS 链路可用。
- 105 当前库存已是 `OBJECT_STORED=9 / NAS_ONLY=2919 / coverage=0.31%`，高于开发报告记录的 `OBJECT_STORED=6 / coverage=0.2%`。推断与后续对象存储回归脚本或环境已有对象化记录有关；M3G-2 核心任务 `143/144` 本身结果清晰，不阻塞收口。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g2-105-objectification-gray-plan.md`
- `scripts/dev/check-m3g2-105-objectification-gray.sh`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m3f-object-storage-first-write.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

未运行：

- `bash scripts/dev/check-m3g2-105-objectification-gray.sh`：按 prompt 安全边界，本轮默认不追加对象化，因此未运行。

## 5. M3G-2 专项脚本状态

已确认：

- `scripts/dev/check-m3g2-105-objectification-gray.sh` 已纳入 Git 跟踪。
- 开发 agent 报告中最近运行结果为 `PASS=23 FAIL=0`。
- 脚本逻辑确认为执行型：会从 105 的 dry-run `NAS_ONLY` 样本中选择最多 3 个文件并创建真实对象化迁移任务。

本轮测试未再次运行该脚本，以避免继续推进 105 新一批对象化。

## 6. 105 实际对象化数量、跳过数量、失败数量

开发报告记录的 M3G-2 首批执行结果：

- `taskId=143`
- `success=3`
- `skipped=0`
- `failure=0`

只读 API 核验 `taskId=143`：

- `status=COMPLETED`
- `totalCount=3`
- `successCount=3`
- `skippedCount=0`
- `failureCount=0`
- 行级结果：
  - `fileId=936`：`OBJECT_STORED / MIRRORED / hasAssetUuid=true`
  - `fileId=937`：`OBJECT_STORED / MIRRORED / hasAssetUuid=true`
  - `fileId=938`：`OBJECT_STORED / MIRRORED / hasAssetUuid=true`

重复执行任务核验：

- `taskId=144`
- `status=COMPLETED`
- `totalCount=3`
- `successCount=0`
- `skippedCount=3`
- `failureCount=0`
- 行级结果均为 `OBJECT_STORED / ALREADY_STORED / hasAssetUuid=true`

结论：首批对象化成功，重复执行幂等跳过，未发现 active object version 污染。

## 7. 105 覆盖率变化

开发报告记录的 M3G-2 首批覆盖率变化：

- `OBJECT_STORED: 3 -> 6`
- `NAS_ONLY: 2925 -> 2922`
- `覆盖率: 0.1% -> 0.2%`

本轮只读 API 当前库存：

- `projectId=503`
- `projectCode=105`
- `projectName=启航华居项目`
- `totalFiles=2928`
- `objectStoredFiles=9`
- `nasOnlyFiles=2919`
- `objectificationCoverageRate=0.31`

说明：当前对象化数量高于开发报告记录，说明后续环境已有额外对象化记录；本轮测试未运行 M3G-2 执行型脚本，不应把本轮测试视为追加了新批次。

## 8. 已对象化文件 file-access 验证

只读状态与受控下载验证：

- `fileId=936`：`storageState=OBJECT_STORED`，active object version 数量为 1，受控 `file-access` 下载成功，NAS 原文件当前仍存在且可读。
- `fileId=937`：`storageState=OBJECT_STORED`，active object version 数量为 1，受控 `file-access` 下载成功，NAS 原文件当前仍存在且可读。
- `fileId=938`：`storageState=OBJECT_STORED`，active object version 数量为 1，受控 `file-access` 下载成功，NAS 原文件当前仍存在且可读。

结论：已对象化文件读取路径通过受控 `file-access`，没有发现对象副本失败后静默 fallback 到 NAS 并仍声称 `OBJECT_STORED` 的迹象。

## 9. 未对象化 NAS_ONLY 链路验证

开发报告中的 `fileId=939` 当前已经变为 `OBJECT_STORED`，因此本轮另选当前仍未对象化的 105 文件：

- `fileId=942`
- `storageState=NAS_ONLY`
- `objectStored=false`
- 受控 `file-access` 下载成功

结论：未对象化文件仍显示 / 解释为 `NAS_ONLY`，并可继续通过 NAS 链路访问。

## 10. 是否触碰真实 NAS 原文件

本轮测试 agent 未运行 M3G-2 执行型脚本，未追加对象化、未复制新批次、未移动、未删除、未改名、未覆盖真实 NAS 原项目文件。

对开发报告中的首批样本，本轮只读核验结果为：

- `fileId=936/937/938` 的 NAS 原文件当前仍存在且可读。
- 开发 agent 报告记录其脚本在执行前后校验过 `size/mtime` 未变化。

结论：未发现真实 NAS 原文件被移动、删除、改名或覆盖。

## 11. 禁出字段与安全边界

通过。

本轮脚本与 API 响应未发现以下敏感信息泄露：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- bucket 真值
- `object_key`
- `objectKey`
- endpoint 原文
- raw row
- SQL 语句
- token / secret / password / access key

确认未进入以下越界范围：

- 未全量迁移全部项目。
- 未一键迁移 NAS 根目录。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未触发 Hermes。
- 未接入真实 BIM 引擎。
- 未修改 `docs/**`。

## 12. Git 范围检查

当前 staged / tracked 变更主要包括：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/current-prompt.md`
- `scripts/dev/check-m3g2-105-objectification-gray.sh`

确认：

- 未发现 `docs/**` 修改。
- 未发现 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、indexing 文件纳入本轮 staged。
- 未发现真实 BIM 引擎能力纳入本轮 staged。
- M3G-2 专项脚本已纳入 Git 跟踪。

记录：

- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为未跟踪非交付项。

## 13. 是否建议主 agent 收口 M3G-2

建议主 agent 进入 M3G-2 收口判断。

理由：

- M3G-2 首批 105 小批对象化任务 `143` 成功，重复任务 `144` 幂等跳过。
- 已对象化样本 `936/937/938` 状态、active object version 和受控 `file-access` 均通过。
- 当前 NAS_ONLY 对照文件 `942` 仍可通过 NAS 链路访问。
- M3G-1 / M3E / M3F / M3C / file-access 回归均通过。
- 未发现 P0 / P1，当前仅有 P2 / 记录项。

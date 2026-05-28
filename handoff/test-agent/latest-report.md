# M3G-4 受控多项目小批对象化执行验收报告

生成时间：2026-05-28

## 1. 测试结论

结论：M3G-4 验收通过，建议主 agent 进入 M3G-4 收口判断。

本轮按 `handoff/test-agent/current-prompt.md` 执行，允许真实小批对象化，但仅在 M3G-4 脚本选择的明确项目和文件范围内执行。专项脚本验证了 `confirmed=false` 拒绝、超限拒绝、小批执行成功、重复执行幂等、`OBJECT_STORED` 文件可通过受控 `file-access` 读取，以及 NAS 原文件 `size/mtime` 未变化。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项未纳入本轮判断。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`：通过，`PASS=21 FAIL=0`。
- `bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3f-object-storage-first-write.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 5. M3G-4 执行摘要

专项脚本本轮选择并执行：

- `projectId=512 / code=108 / 福城南产业片区11-20-02宗地 / fileId=33476 / size=2694949`
- `projectId=506 / code=93 / 中建八局国交酒店项目 / fileId=18653 / size=88476`
- `projectId=505 / code=101 / C塔 / fileId=13197 / size=1383285`

合计：

- 项目数：3
- 文件数：3
- 容量：4,166,710 bytes
- 首次执行：`created=3 / skipped=0 / failed=0`
- 重复执行：同一批文件按幂等策略跳过

说明：文件与容量和开发 agent 报告中的自测样本不同，原因是本轮测试再次执行了 M3G-4 小批脚本，脚本会从当前真实项目 `NAS_ONLY` 小样本中选择下一批最多 3 个文件。这符合本轮 prompt “允许真实小批对象化”的范围。

## 6. confirmed=false 是否拒绝

通过。

M3G-4 专项脚本调用 `POST /api/data-steward/storage-objectification-plans:execute` 时使用 `confirmed=false`，接口返回非 OK，并且响应通过 forbidden-field scan。

结论：未确认不能执行。

## 7. 超限是否拒绝

通过。

M3G-4 专项脚本使用超过总文件数硬上限的执行请求，接口返回非 OK，并且响应通过 forbidden-field scan。

结论：超限不会执行。

## 8. 小批执行是否成功

通过。

M3G-4 专项脚本在 `confirmed=true` 且明确项目 / 文件范围内执行小批对象化，结果：

- `dryRun=false`
- `executionStarted=true`
- `taskSource=MULTI_PROJECT_CONTROLLED_EXECUTION`
- `selectedFileCount=3`
- `failedCount=0`
- `created=3`
- `skipped=0`

结论：小批执行成功，范围限定在脚本明确选择的 3 个真实 NAS 项目和 3 个文件。

## 9. 重复执行是否幂等

通过。

M3G-4 专项脚本对同一批 `fileId=33476,18653,13197` 再次执行，返回幂等跳过。

只读数据库抽查：

- `fileId=33476` active object version 数量为 1。
- `fileId=18653` active object version 数量为 1。
- `fileId=13197` active object version 数量为 1。

结论：重复执行未污染 active object version。

## 10. OBJECT_STORED 文件 file-access 验证

通过。

只读 API 与受控下载抽查：

- `fileId=33476`：`storageState=OBJECT_STORED`，`objectStored=true`，受控 `file-access` 下载成功。
- `fileId=18653`：`storageState=OBJECT_STORED`，`objectStored=true`，受控 `file-access` 下载成功。
- `fileId=13197`：`storageState=OBJECT_STORED`，`objectStored=true`，受控 `file-access` 下载成功。

结论：已对象化文件可通过受控 `file-access` 读取。

## 11. NAS 原文件 size/mtime 是否未变化

通过。

M3G-4 专项脚本在执行前记录 3 个样本 NAS 原文件 `size/mtime`，执行后逐一校验：

- `fileId=33476` NAS 原文件仍存在且 `size/mtime` 未变化。
- `fileId=18653` NAS 原文件仍存在且 `size/mtime` 未变化。
- `fileId=13197` NAS 原文件仍存在且 `size/mtime` 未变化。

只读抽查也确认 3 个文件的 NAS 原文件当前仍存在且可读。

结论：未发现真实 NAS 原文件被移动、删除、改名或覆盖。

## 12. 禁出字段检查

通过。

M3G-4 专项脚本和回归脚本均包含 forbidden-field scan，未发现以下敏感信息泄露：

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

## 13. 回归结果

全部通过：

- M3G-3 多项目对象化规划：`PASS=11 FAIL=0`。
- M3G-1 readiness / inventory / dry-run：`PASS=9 FAIL=0`。
- M3F 新文件对象存储优先写入：`PASS=11 FAIL=0`。
- M3E 预览与转换产物对象化：`PASS=8 FAIL=0`。
- M3C 对象存储迁移任务中心：`PASS=9 FAIL=0`。
- Phase2 batch4 file-access：`PASS=18 FAIL=0`。

## 14. Git 范围检查

当前 staged / tracked 变更主要包括：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `handoff/dev-agent/latest-report.md`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

确认：

- 未发现 `docs/**` 修改。
- 未发现 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、indexing 文件纳入本轮 staged。
- 未发现真实 BIM 引擎能力纳入本轮 staged。
- M3G-4 专项脚本已纳入 Git 跟踪。

记录：

- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为未跟踪非交付项。

## 15. 是否建议主 agent 收口 M3G-4

建议主 agent 进入 M3G-4 收口判断。

理由：

- 未确认拒绝、超限拒绝、小批执行、重复幂等均通过。
- 3 个真实项目小批对象化成功，且未越过明确项目范围。
- 已对象化文件均为 `OBJECT_STORED`，并可通过受控 `file-access` 读取。
- NAS 原文件仍存在且 `size/mtime` 未变化。
- M3G-3 / M3G-1 / M3F / M3E / M3C / file-access 回归全部通过。
- 未发现 P0 / P1，当前仅有 P2 / 记录项。

# M3G-5 文件管理器项目全局搜索与存储展示修复验收报告

生成时间：2026-05-28 17:27:37 CST

## 1. 测试结论

结论：M3G-5 验收通过，建议主 agent 进入 M3G-5 收口判断。

本轮按 `handoff/test-agent/current-prompt.md` 验收 `M3G-5：文件管理器项目全局搜索与存储展示修复`。测试确认：

- 文件管理器未搜索时仍保持当前目录 direct-only 浏览。
- 输入关键词后默认切换为项目全局搜索。
- “仅当前文件夹及子目录”开关可限制搜索范围。
- 搜索结果展示所在位置 / 项目内路径。
- 105 / 503 仍是混合存储状态，接口与页面可区分 `OBJECT_STORED` 与 `NAS_ONLY`。
- 本轮未创建新的对象化迁移任务，未触碰真实 NAS 原文件。
- 接口响应和页面检查未发现禁出字段泄露。

说明：当前 prompt 的必跑命令列出 `bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`，但本轮边界明确“不执行对象化迁移”。为避免测试本身越界，M3G-4 回归按开发 agent 已提供的只读模式执行：`M3G4_READ_ONLY=1 bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`。该模式仍覆盖 readiness、样本选择、`confirmed=false` 拒绝、超限拒绝、未触发迁移写入和脚本跟踪。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项仍存在，未纳入本轮阻塞判断。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g5-file-manager-search-storage-display-plan.md`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh`：通过，`PASS=12 FAIL=0`。
- `M3G4_READ_ONLY=1 bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

未运行：M3G-2 执行型脚本。

## 5. 搜索默认范围验证

通过。

专项脚本选择 105 / 503 项目内嵌套文件样本，调用：

- `GET /api/data-steward/catalog/files?projectId=503&keyword=...`

验证默认关键词搜索不携带当前目录限制，可以命中项目内深层样本，并且结果包含 `logicalPath` / 项目内位置提示。

浏览器轻量检查：

- 打开 `http://127.0.0.1:5173/data-steward/assets/503?tab=files`。
- 在文件管理器搜索 `pdf`。
- 页面显示“正在整个项目中搜索，结果会显示所在位置；不会暴露真实 NAS 路径。”
- 表格出现“所在位置”列，并显示项目内相对路径。

## 6. 当前文件夹搜索验证

通过。

专项脚本验证：

- 带 `directoryPath=样本父目录` 时可命中该目录下样本。
- 带不存在目录 `__m3g5_no_such_dir__` 时结果总数为 0。

结论：“仅当前文件夹及子目录”范围限制生效。

## 7. 未搜索目录浏览 direct-only 验证

通过。

专项脚本调用：

- `GET /api/data-steward/catalog/directory-children?projectId=503&page=1&pageSize=50`

验证根目录 direct children 返回正常且脱敏。

浏览器轻量检查：

- 未搜索时右侧显示“当前文件夹：10 个文件夹 / 2 个文件”。
- 根目录右侧列表为直接子文件夹和直接文件，未铺开深层搜索结果。

## 8. 存储状态展示验证

通过。

专项脚本确认 105 / 503 并非 100% 对象化，仍有 `OBJECT_STORED` 与 `NAS_ONLY` 混合状态。

接口验证：

- `catalog/files` 对已对象化文件返回 `storageState=OBJECT_STORED`、`accessSource=NAS_SIDE_MINIO`。
- `catalog/files` 对历史 NAS 文件返回 `storageState=NAS_ONLY`。
- `catalog/files/{fileId}` 不返回真实 `storagePath`，只返回存储状态。

浏览器轻量检查：

- 文件表有“存储”列。
- 搜索结果中可同时看到“已对象化 / NAS 侧 MinIO”和“历史 NAS / 尚未对象化”。

## 9. 是否创建对象化任务

未创建。

M3G-5 专项脚本在执行前后比对 `data_object_migration_tasks` 数量，结果未变化。

M3G-4 回归使用只读模式，输出确认：

- `M3G4_READ_ONLY 已开启，跳过真实对象化执行和重复执行验证`
- `只读模式未触发对象化迁移写入`

## 10. 是否触碰真实 NAS 原文件

未触碰。

M3G-5 专项脚本对 NAS 原文件记录并复核 `size/mtime`，结果未变化。

本轮没有执行上传、移动、删除、重命名、覆盖真实 NAS 文件等写操作。

## 11. 禁出字段检查

通过。

M3G-5 专项脚本对 inventory、directory-children、global search、current-folder search、file detail 等响应执行 forbidden-field scan，未发现：

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
- raw row / raw DB row
- SQL 语句
- token / secret / password / access key

浏览器 DOM 轻量检查也未发现 `/Volumes`、`smb://`、`nas://`、`storage_uri`、`storageUri`、bucket、object key、token、secret。

## 12. 回归结果

全部通过：

- M3G-4 只读回归：`PASS=11 FAIL=0`。
- M3G-3 多项目对象化规划：`PASS=11 FAIL=0`。
- M3E 预览与转换产物对象化：`PASS=8 FAIL=0`。
- Phase2 batch4 file-access：`PASS=18 FAIL=0`。

## 13. Git 范围检查

当前 staged 交付范围：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `handoff/dev-agent/latest-report.md`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

确认：

- M3G-5 专项脚本已纳入 Git 跟踪。
- 未发现 `docs/**` 纳入本轮 staged。
- 未发现 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、indexing、真实 BIM 引擎等越界文件纳入本轮 staged。

记录：

- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为未跟踪非交付项。

## 14. 是否建议主 agent 收口 M3G-5

建议主 agent 进入 M3G-5 收口判断。

理由：

- 项目全局搜索、当前文件夹范围搜索、清空后 direct-only 浏览的核心契约已通过专项脚本与页面轻量检查。
- `OBJECT_STORED` 与 `NAS_ONLY` 可在接口和页面区分。
- 未创建对象化迁移任务，未触碰真实 NAS 原文件。
- 未发现禁出字段泄露。
- M3G-4 / M3G-3 / M3E / file-access 回归通过。
- 当前无 P0 / P1。

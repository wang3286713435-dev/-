# 开发 Agent 报告：M3G-5 文件管理器项目全局搜索与存储展示修复

时间：2026-05-28 CST

## 1. 本轮目标

本轮执行 `M3G-5：文件管理器项目全局搜索与存储展示修复`。

目标是修复文件管理器关键词搜索默认被当前目录限制的问题，并让 105/503 这类未完全对象化项目清楚展示文件当前处于 `OBJECT_STORED` 还是 `NAS_ONLY`，同时保持只读、脱敏和不迁移边界。

## 2. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移。

## 3. 后端改动

- `catalog/files`、`catalog/directory-children`、`catalog/files/{fileId}` 增加脱敏业务字段：
  - `storageState`
  - `accessSource`
- 存储状态由只读 SQL 汇总推导：
  - `OBJECT_STORED`：存在 active `data_file_object_versions`
  - `MIGRATION_PENDING`：存在 pending/running 迁移任务
  - `MIGRATION_FAILED`：存在 failed 迁移任务
  - `NAS_ONLY`：历史 NAS 文件，尚未对象化
- 关键词搜索不再匹配 `storage_uri`，改为匹配 `logical_path`。
- 文件详情不返回真实 `storagePath`，只返回状态和项目内逻辑位置提示。

## 4. 前端改动

- 文件管理器关键词搜索默认调用 `catalog/files` 做全项目搜索，不再被当前目录限制。
- 新增“仅当前文件夹及子目录”开关；开启后才携带 `directoryPath` 做目录范围搜索。
- 搜索结果展示“所在位置 / 项目内路径”，不展示真实 NAS 路径。
- 文件表新增“存储”列：
  - `已对象化 / NAS 侧 MinIO`
  - `历史 NAS / 尚未对象化`
  - `对象化中 / 等待平台处理`
  - `对象化失败 / 仍按历史 NAS`
  - `未登记 / 需扫描入库`
- 搜索范围、分页和开关状态会保存到项目文件管理器状态与 URL query。

## 5. 脚本改动

新增：

`scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

覆盖：

1. 105/503 不是 100% 对象化，能区分 `OBJECT_STORED` 与 `NAS_ONLY`。
2. 根目录 direct children 仍保持 direct-only。
3. 默认关键词搜索为全项目搜索。
4. 当前文件夹及子目录搜索开关有效。
5. 搜索结果包含项目内位置提示。
6. 接口响应不泄露 raw path、bucket、object key、`storage_uri`。
7. 本轮不创建对象化迁移任务。
8. NAS 原文件 `size/mtime` 未变化。

同时给 M3G-4 脚本新增 `M3G4_READ_ONLY=1` 模式，用于 M3G-5 回归时跳过真实对象化执行；默认执行模式仍保持原有严格要求。

## 6. 边界确认

- 是否执行真实历史文件迁移：否。
- 是否创建对象化迁移任务：否。
- 是否移动、删除、重命名、覆盖真实 NAS 原文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否写 documents / chunks / Qdrant / OpenSearch / Hermes memory：否。
- 是否触发 Hermes / BIM / parser / indexing：否。
- 是否暴露 raw NAS path、`storage_uri`、bucket、object key、SQL、raw row、token / secret：否。

## 7. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-5 文件管理器搜索与存储展示专项                           PASS=12 FAIL=0
M3G-4 只读回归                                               PASS=11 FAIL=0
M3G-3 多项目对象化规划回归                                   PASS=11 FAIL=0
M3E 预览与转换产物对象化回归                                  PASS=8 FAIL=0
Phase2 batch4 文件访问安全回归                                PASS=18 FAIL=0
浏览器短验（Chrome 105/503 文件管理搜索 pdf）                  PASS
git diff --check                                             PASS
git diff --cached --check                                    PASS
```

已执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
M3G4_READ_ONLY=1 bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

## 8. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`

本轮完成后按要求未关闭项目服务。

## 9. 未完成事项和风险

- 本轮只修复搜索口径与展示，不扩大对象化范围。
- 105/503 当前仍是混合状态：部分 `OBJECT_STORED`，多数历史文件仍为 `NAS_ONLY`。
- 若后续要继续提升覆盖率，需要进入单独对象化执行批次，并明确项目范围、执行窗口、样本上限和失败处理策略。

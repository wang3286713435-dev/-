# 开发 Agent 当前任务：M3G-5 文件管理器项目全局搜索与存储展示修复

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3g-nas-minio-real-project-object-storage`

## 0. 当前结论与目标

M3G-4 已正式收口：

- 平台具备受控多项目小批对象化执行能力。
- 但 105 项目尚未整体对象化，大多数文件仍是 `NAS_ONLY`。

当前用户反馈两个真实使用问题：

1. 文件管理器搜索只在当前视图 / 当前目录下检索，不符合文件管理器预期。用户期望默认搜索整个项目。
2. 平台主界面仍让用户感觉在看真实 NAS 路径，无法清楚感知哪些文件已对象化、哪些仍在历史 NAS 链路。

当前进入：

`M3G-5：文件管理器项目全局搜索与存储展示修复`

本批不执行任何对象化迁移，不修改真实 NAS 文件，只修检索和展示体验。

## 1. 必须先阅读

- `handoff/main-agent/m3g5-file-manager-search-storage-display-plan.md`
- `handoff/main-agent/m3g4-controlled-multi-project-objectification-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`

## 2. 严格边界

本轮允许：

- 修改文件管理器搜索交互。
- 默认把关键词搜索改为项目全局搜索。
- 增加“仅当前文件夹及子目录”搜索范围开关。
- 优化文件表和详情中的存储状态、访问来源、项目内路径展示。
- 最小补充只读脱敏字段。
- 新增 M3G-5 专项脚本。
- 更新 `handoff/dev-agent/latest-report.md`。

本轮禁止：

- 不执行对象化迁移。
- 不扩大对象化范围。
- 不移动、删除、重命名、覆盖真实 NAS 原项目文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做 Hermes 正文问答。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不接入真实 BIM 引擎。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object key、endpoint 原文、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 3. 本轮必须完成

### A. 默认项目全局搜索

当前问题：

- `AssetProjectFileBrowser.vue` 在 `loadFiles()` 中调用目录子项接口时，会同时传 `activeDir` 和 `keyword`。
- 结果导致搜索被限制在当前目录视图里。

目标行为：

- 未输入关键词时：保持当前目录直接子项浏览。
- 输入关键词时：默认在整个项目内搜索文件。
- 搜索结果不展示当前目录子文件夹，只展示匹配文件。
- 搜索结果展示“所在位置 / 项目内路径”。
- 页面提示：“正在整个项目中搜索”。

推荐实现：

- 当 `filters.keyword.trim()` 非空且未启用“仅当前文件夹及子目录”时，使用 `fetchCatalogFiles(...)`：
  - 传 `projectId`
  - 传 `keyword`
  - 不传 `directoryPath`
  - 不传 `directOnly`
- 当未输入关键词时，继续使用 `fetchCatalogDirectoryChildren(...)`。

### B. 当前文件夹搜索开关

增加一个轻量开关：

`仅当前文件夹及子目录`

行为：

- 默认关闭。
- 只有输入关键词时可见或有效。
- 开启后搜索才带 `directoryPath=activeDir`。
- 不做物理 NAS 递归扫描，只查已登记资产。

### C. 存储状态与访问来源

文件表或详情必须让用户看懂：

- `OBJECT_STORED`：已对象化 / NAS 侧 MinIO。
- `NAS_ONLY`：历史 NAS / 尚未对象化。
- `MIGRATION_PENDING`：对象化中。
- `MIGRATION_FAILED`：对象化失败。

要求：

- 不把 raw storage path 作为主界面展示。
- 默认展示项目内路径 / 所在位置。
- 如果现有详情中显示真实存储地址，必须移除、脱敏或折叠到管理员诊断信息，并确认普通用户不可见。

### D. 不回退目录浏览

必须保持 M2H-F1 后的文件管理器口径：

- 当前目录右侧只显示直接子文件夹和直接文件。
- 左侧目录树可展开 / 折叠。
- 双击文件夹进入目录。
- PDF / 图片受控预览、模型占位不回归。

### E. 新增专项脚本

新增：

`scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

至少验证：

1. 105 / 503 当前对象化覆盖率不是 100%，能区分 `OBJECT_STORED` 与 `NAS_ONLY`。
2. 根目录 direct children 口径不回归。
3. 有关键词时默认项目全局搜索，不带当前目录限制。
4. 当前文件夹搜索开关生效。
5. 搜索结果包含项目内路径 / 所在位置。
6. 响应与页面接口不泄露 raw NAS path、bucket、object key、`storage_uri`。
7. 不创建对象化迁移任务。
8. 不修改 NAS 原文件。

## 4. 自测要求

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
git diff --cached --check
```

注意：

- 不要运行 M3G-2 执行型脚本。
- 如 M3G-4 脚本会再次执行小批对象化，请在报告里明确新增对象化数量；如能调整为轻量只读回归更好。

## 5. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

1. 搜索默认范围如何变化。
2. 当前文件夹搜索开关如何工作。
3. 105 当前对象化覆盖率是否仍非 100%。
4. 文件表 / 详情如何展示 `OBJECT_STORED` 与 `NAS_ONLY`。
5. 是否创建对象化任务，必须明确回答。
6. 是否触碰真实 NAS 原文件，必须明确回答“否”。
7. 是否泄露真实路径 / bucket / object key / `storage_uri`，必须明确回答。
8. 自测结果。

## 6. 完成定义

只有同时满足以下条件，才能标记完成：

- 搜索默认变成项目全局搜索。
- 当前文件夹搜索可选且有效。
- 清空搜索后恢复当前目录直接子项浏览。
- 用户能看懂文件是否已对象化。
- 主界面不暴露真实 NAS 路径。
- 未执行对象化迁移。
- 未修改 `docs/**`。

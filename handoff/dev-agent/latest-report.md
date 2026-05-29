# 开发 Agent 报告：M3G-5-F1 文件管理器搜索仍显示文件夹修复

时间：2026-05-28 CST

## 1. 本轮目标

本轮执行 `M3G-5-F1：修复文件管理器搜索仍显示文件夹`，属于 M3G-5 返工修复，不是新功能批次。

目标是让 `/data-steward/assets/503?tab=files&fileKeyword=宝安` 这类带 `fileKeyword` 的入口直接进入项目全局搜索模式，右侧只显示匹配文件和所在位置，不再混入根目录文件夹。

## 2. 根因

根因有两处：

1. 文件管理器右侧统一列表由 `directoryEntries + fileEntries` 合并而来，即使已有关键词，也可能继续把当前目录直接子文件夹渲染到表格。
2. 组件初始化后主要监听项目 ID / 初始质量问题变化；同项目内 URL query 出现或变化 `fileKeyword` 时，没有稳定按 query 重新应用浏览状态并触发搜索。

另外，开发短验时发现 5173 前端服务一度未运行，浏览器看到的是旧页面残留。已按当前源码重新拉起 5173，并保持运行。

## 3. 修改文件

- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `backend/**`、`docs/**`、数据库迁移或 Hermes / BIM / parser / indexing 相关能力。

## 4. 前端修复

- 搜索模式和目录浏览模式明确分离：
  - 有关键词时，`browserEntries` 只返回 `fileEntries`。
  - 无关键词时，仍返回 `directoryEntries + fileEntries`，保持当前目录 direct-only 浏览。
- 增加 route query 监听：
  - `fileKeyword`
  - `fileSearchScope`
  - `fileDir`
  - `fileKind`
  - `discipline`
  - `fileExt`
  - `qualityIssue`
  - `ownershipStatus`
  - `filePage`
  - `filePageSize`
- query 与当前状态不一致时，会重新应用浏览状态、清空选择并加载文件列表。
- 增加状态等价判断，避免内部 `router.replace` 和 watcher 互相触发死循环。
- 同项目 URL 查询变化时保留目录树展开记忆，避免搜索条件变化把左侧视图状态清空。

## 5. 脚本补强

`scripts/dev/check-m3g5-file-manager-search-storage-display.sh` 增加前端契约检查：

- 搜索模式必须排除 directory entries。
- `fileKeyword` query 必须被 watcher 覆盖。
- query 变化必须重新应用浏览状态。

专项脚本结果从 `PASS=12` 增强为 `PASS=13`。

## 6. 浏览器短验

已在 Chrome 验证：

1. 打开 `http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileKeyword=宝安`。
2. 页面自动进入搜索模式。
3. 搜索框显示 `宝安`。
4. 页面显示“正在整个项目中搜索，结果会显示所在位置”。
5. 表格显示“所在位置”和“存储”列。
6. 右侧表格只显示匹配文件，未出现 `00_工作进度`、`01_文件收发` 等文件夹行。
7. 点击“重置视图”后 URL 回到 `?tab=files`，根目录文件夹恢复显示。

## 7. 自测结果

```text
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-5 文件管理器搜索与存储展示专项                           PASS=13 FAIL=0
浏览器短验（Chrome 105/503 fileKeyword=宝安）                 PASS
浏览器短验（重置视图恢复根目录文件夹）                         PASS
git diff --check                                             PASS
```

已执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
git diff --check
```

## 8. 边界确认

- 是否改动后端：否。
- 是否新增数据库迁移：否。
- 是否执行对象化迁移：否。
- 是否创建对象化迁移任务：否。
- 是否移动、删除、重命名、覆盖真实 NAS 文件：否。
- 是否读取文件正文：否。
- 是否写 documents / chunks / Qdrant / OpenSearch / Hermes memory：否。
- 是否新增 Hermes / BIM / parser / indexing 能力：否。
- 是否暴露 raw NAS path、`storage_uri`、bucket、object key、SQL、raw row、token / secret：否。

## 9. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 已按当前源码重新拉起并保持运行：`http://127.0.0.1:5173`

## 10. 未完成事项和风险

- 未发现本轮未完成事项。
- 本轮只修复搜索模式显示口径和 query 同步，不扩大对象化范围。
- 105/503 仍保留 M3G-5 已确认的混合存储状态：部分 `OBJECT_STORED`，多数历史文件仍为 `NAS_ONLY`。

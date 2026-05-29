# 开发 Agent 当前任务：M3G-5-F1 修复文件管理器搜索仍显示文件夹

你是卓羽智能数据中台的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮是 `M3G-5` 返工修复，不是新功能批次。

## 0. 问题现象

用户在浏览器访问：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileKeyword=宝安`

页面搜索框里显示 `宝安`，但右侧文件管理器仍展示项目根目录文件夹，例如：

- `00_工作进度`
- `01_文件收发`
- `02_项目资源`
- `03_过程文件`

这不符合预期。

用户理解里的搜索是：输入关键词后，应在整个项目中搜索相关文件，右侧应该列出和关键词相关的文件，并显示所在位置；不应该继续展示当前目录的文件夹直达子项。

## 1. 本轮目标

修复文件管理器搜索模式：

1. 带 `fileKeyword` URL 直接进入页面时，必须立即进入搜索模式。
2. 在搜索模式下，右侧表格默认只展示匹配文件，不展示当前目录文件夹。
3. 搜索模式下应显示“正在整个项目中搜索”提示和“所在位置”列。
4. “仅当前文件夹及子目录”仍作为可选搜索范围。
5. 清空关键词后，恢复当前目录 direct-only 浏览，即右侧显示当前目录直接文件夹和直接文件。
6. 不改变后端对象化、NAS 写入、Hermes、语义索引等能力边界。

## 2. 必查文件

请先阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/m3g5-file-manager-search-storage-display-closure.md`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

## 3. 已知根因线索

重点检查这些点：

1. `AssetProjectFileBrowser.vue` 当前只在 `props.projectId / props.initialQualityIssue` 变化时初始化浏览状态。
2. 如果同一个项目页面内 URL query 改为 `fileKeyword=宝安`，组件可能不会重新按 query 初始化并触发搜索。
3. 搜索框值可能已经显示关键词，但右侧仍保留上一次 direct-only 目录浏览结果。
4. `browserEntries` 当前由 `directoryEntries + fileEntries` 合并而来。搜索模式下如果没有强制排除 `directoryEntries`，右侧就会继续显示文件夹。
5. 输入关键词时，如果前端已经把关键词同步进 URL，就不能让页面仍停留在“当前目录浏览模式”。

## 4. 推荐修复方向

优先做最小修复：

### A. 搜索模式不展示目录项

当 `filters.keyword.trim()` 非空时，右侧表格应该只使用 `fileEntries`。

未搜索时，仍使用：

`directoryEntries + fileEntries`

也就是说，搜索模式和目录浏览模式要清晰分开。

### B. URL query 必须驱动状态

当路由 query 中出现或变化以下字段时：

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

同项目内也要能同步到组件状态，并触发正确加载。

注意避免死循环：

- 内部 `router.replace` 同步状态时不要反复触发重复请求。
- 可以比较当前状态和 query state，只有变化时才应用。

### C. 用户输入行为要一致

当前搜索框 `v-model` 会改变页面状态。只要 URL / 状态已经表现为“有关键词”，结果区就必须进入搜索模式。

你可以选择：

- 保持点击“查询”/回车触发加载，但不要提前把未执行搜索的 keyword 写进 URL；或者
- 输入关键词后 debounce 触发搜索。

无论选择哪种，最终用户看到的状态必须一致：

- URL 有 `fileKeyword=宝安` 时，右侧就是“宝安”的搜索结果。
- 搜索框有关键词且页面提示搜索时，右侧不能还显示当前目录文件夹。

### D. 测试脚本补强

增强 `scripts/dev/check-m3g5-file-manager-search-storage-display.sh` 或新增极短返工脚本，至少覆盖：

1. 带 `fileKeyword` 的页面/接口场景不返回目录项作为搜索结果。
2. 搜索模式下 `browserEntries` 或页面 DOM 不应出现“文件夹”行。
3. 清空关键词后 direct-only 目录浏览恢复。

如果脚本不方便用浏览器测 DOM，可以在报告中补浏览器短验步骤。

## 5. 禁止事项

本轮严禁：

1. 不要执行对象化迁移。
2. 不要移动、删除、重命名、覆盖真实 NAS 文件。
3. 不要读取文件正文。
4. 不要新增 Hermes 能力。
5. 不要写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
6. 不要接入真实 BIM 引擎。
7. 不要修改 `docs/**`。
8. 不要改数据库迁移。
9. 除非确认后端接口确实有问题，否则不要改后端。

## 6. 验收标准

必须同时满足：

1. 打开 `http://127.0.0.1:5173/data-steward/assets/503?tab=files&fileKeyword=宝安` 后，页面自动进入搜索模式。
2. 搜索模式右侧表格不显示文件夹行。
3. 搜索模式显示项目全局搜索提示。
4. 搜索结果文件能显示所在位置。
5. 勾选“仅当前文件夹及子目录”后，搜索范围能收窄。
6. 清空关键词后恢复当前目录 direct-only 浏览。
7. `OBJECT_STORED` / `NAS_ONLY` 展示不回归。
8. 禁出字段扫描仍通过。
9. 不创建迁移任务，不触碰真实 NAS 原文件。

## 7. 必跑验证

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
git diff --check
```

浏览器短验：

1. 打开 `/data-steward/assets/503?tab=files&fileKeyword=宝安`。
2. 确认右侧不出现文件夹行。
3. 确认页面提示正在整个项目中搜索。
4. 清空关键词后确认根目录文件夹恢复。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须说明：

- 根因。
- 修改文件。
- 搜索模式如何和目录浏览模式分离。
- URL query 如何驱动页面状态。
- 验证结果。
- 是否改动后端。
- 是否有未完成事项。

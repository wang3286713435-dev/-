# UX4-B-R3 启动台状态卡片、主数据布局与葛兰岱尔 Viewer 接入修复报告

## 1. 本轮目标

本轮按用户最新反馈补齐三类问题：

- 修复项目启动台“项目总体状态”圆环内数字与说明文字重叠。
- 修复任意项目进入工程主数据页后，前端卡片/表格可能横向无限延长的问题。
- 修复项目工作台 BIM 协同区域仍使用静态占位壳层、未读取葛兰岱尔轻量化 Viewer 状态的问题。

同时保留上一轮已完成内容：

- 项目启动台作为平台级父入口，不绑定具体项目。
- BIM 协同作为平台级父入口，不自动绑定当前项目。
- 文件详情面板改为不侵占文件列表的可调整大小浮窗。

## 2. 改动文件列表

本轮修改：

- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `handoff/dev-agent/latest-report.md`

上一轮相关未回退改动仍保留：

- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/router/index.ts`
- `frontend/src/modules/visualization/pages/DigitalTwinPortalPage.vue`

本轮未修改：

- `backend/**`
- 数据库迁移
- `docs/**`
- NAS 文件

## 3. 项目总体状态卡片修复

已调整启动台圆环中心内容结构：

- 数字和“总项目数”说明改为同一个居中内容容器。
- 移除原来靠 `margin-top` 硬推文字的方式。
- 避免项目数量变大后和说明文字叠在一起。

## 4. 工程主数据页面无限延长修复

已二次定位并修复项目内 `工程主数据` 页真正的撑宽源：

- 根因不是后端数据，而是项目工作台 `tab=master-data` 内嵌的 `FileOwnershipTreePanel` 有多张多列表格。
- Element Plus 表格列宽总和大于中间列时，会把组件最小宽度撑大，进而把项目工作台整体向右顶开。
- 已在 `AssetProjectDetailPage.vue` 的工程主数据三栏布局上增加 `max-width: 100%`、`overflow: hidden`、`contain: inline-size` 约束。
- 已在 `FileOwnershipTreePanel.vue` 内部给面板、tabs、工程树布局、详情区、表格外壳和表格滚动容器补齐 `min-width: 0` / `max-width: 100%`。
- 结果是宽表格只会在自身区域内处理横向内容，不再把整个页面无限向右延伸。

上一轮对独立初始化向导页 `ProjectInitializationPage.vue` 的宽度约束仍保留，用于防止独立向导页面被长文本或表格撑宽。

## 5. 葛兰岱尔 Viewer 接入修复

已将项目工作台 `BIM 协同` 区域从静态占位改为读取现有葛兰岱尔模型清单：

- 前端调用现有只读接口 `fetchGlandarModelFiles(projectId)`。
- 只在 `viewerAvailable=true` 且存在 `latestJobId` 时嵌入 `/visualization/glandar-viewer-embed`。
- “打开独立 Viewer”会带上 `projectId / jobId / fileName / modelFileId`。
- 模型列表显示真实葛兰岱尔状态、文件类型、大小、操作提示。
- 无可预览产物时显示清楚空状态，不再显示假的模型示意图。

当前 503 项目后端返回的葛兰岱尔模型清单为 0 条，因此页面会真实显示“暂无可嵌入轻量化 Viewer”。这不是前端失败，而是当前项目暂无可展示轻量化结果。

## 6. 安全边界确认

本轮未触碰真实 NAS 文件：

- 未读取文件正文。
- 未移动、删除、重命名 NAS 文件。
- 未新增上传、parser、indexing。
- 未启动新的模型轻量化转换任务。

本轮未暴露：

- 真实 NAS 绝对路径
- `storage_path`
- `storage_uri`
- SQL
- token / secret

## 7. 自测结果

已执行：

```bash
corepack pnpm --dir frontend build
```

结果：通过。仅保留既有 Vite 大 chunk 提示。

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

结果：通过，返回 `{"status":"UP"}`。

```bash
curl -fsS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:5173/
```

结果：通过，返回 `200`。

```bash
curl -fsS -o /dev/null -w "%{http_code}\n" 'http://127.0.0.1:5173/data-steward/assets/503?tab=master-data'
```

结果：通过，返回 `200`。

```bash
git diff --check
```

结果：通过。

葛兰岱尔清单接口抽查：

- `GET /api/visualization-adapter/projects/503/glandar/model-files`
- 返回成功。
- 当前 `count=0`、`ready=0`。
- 未发现 `storage_path`、`storage_uri`、`/Volumes`、`smb://`、SQL、token/secret 真值泄露。

## 8. 风险和未完成事项

- 当前 503 项目没有可嵌入 Viewer 的葛兰岱尔轻量化结果，所以只能验证空状态和接入逻辑，无法在该项目直接看到真实模型画面。
- 如果后续导入了带 `latestJobId` 的轻量化模型，当前 BIM 协同区域会自动嵌入 Viewer。
- 文件详情浮窗当前支持调整大小，但没有做拖拽移动位置；如后续需要可单独加“拖动标题栏移动”。
- 本轮没有关闭前端或后端服务，便于用户继续在浏览器检查效果。

# 开发 Agent 续跑任务：项目工作台导航重组，小步实现版

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮是上一轮“导航与项目工作台重组”的续跑。上一轮会话已被主 agent 中断，原因是它尝试一次性重写 `AssetProjectDetailPage.vue`，长时间没有落盘，审计风险过高。

## 必须遵守

- 使用 `deepseek-v4-pro`。
- 使用 Ralph Loop / Ralph 工作流，持续更新 `.claude/ralph/progress.txt`。
- 不要创建子 agent，不要调用 Task / Explore 等独立上下文。
- 不要修改 `docs/**`。
- 不要修改后端、数据库迁移或真实 NAS 文件。
- 不要一次性重写 `AssetProjectDetailPage.vue`。
- 只能用小补丁、小组件方式推进，保证每一步可审计。

## 当前已落地的部分

- `/` 和兜底路由已改向 `/data-steward/assets`。
- 路由里已新增项目内入口：
  - `/data-steward/assets/:projectId/master-data/sections`
  - `/data-steward/assets/:projectId/master-data/node-types`
  - `/data-steward/assets/:projectId/master-data/deliverable-standard`
  - `/data-steward/assets/:projectId/work/document-delivery`
  - `/data-steward/assets/:projectId/work/drawing-delivery`
  - `/data-steward/assets/:projectId/work/rectifications`
  - `/data-steward/assets/:projectId/work/dashboard`
- 顶部全局“当前项目”选择器已从 `AppLayout.vue` 移除。
- 已新增 `frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`，但还需要确认它被实际调用。

## 本轮目标

完成剩余可验收项：

1. `AppLayout.vue` 必须实际调用 `useProjectWorkspaceContext()`，让带 `:projectId` 的项目内路由自动同步当前项目上下文，避免 `CORE_PROJECT_CONTEXT_MISMATCH`。
2. `/data-steward/assets/:projectId` 顶部形成项目工作台导航：
   - 文件资产
   - 工程主数据：部位树、节点类型、交付物标准
   - 工作中心：文档交付、图纸交付、整改闭环、项目驾驶舱
3. 文件资产区域改成“左目录树 + 右文件表”，优先新增小组件，避免重写整页。
4. 文件表仍保留现有关键列与操作：
   - 文件ID、文件名、类型、扩展名、专业、版本、大小、置信度、质量问题、更新时间
   - 预览、详情、治理、补 checksum、复制路径
5. 现有详情抽屉、预览弹窗、checksum 任务弹窗、治理弹窗不能丢。

## 推荐实现方式

### 1. 新增小组件

新增：

`frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`

组件职责：

- props:
  - `projectId: number`
  - `disciplineOptions: AssetDiscipline[]`
- emits:
  - `open-preview(fileId: number)`
  - `open-detail(fileId: number)`
  - `open-metadata(fileId: number)`
  - `create-checksum(fileId: number)`
  - `copy-path(fileId: number)`
- 内部使用：
  - `fetchCatalogDirectories(projectId)`
  - `fetchCatalogFiles({ projectId, directoryPath, keyword, fileKind, disciplineCode, fileExt, qualityIssue, page, pageSize })`
- 左侧展示目录列表，包含“全部文件/根目录”入口。
- 点击目录后，右侧只查询该目录下文件。
- 右侧表格用 `CatalogFile`，不要直接暴露真实 NAS 路径。
- 操作按钮只把 `fileId` 交回父组件，由父组件继续复用原有 `fetchFileAsset -> openPreview/openFileDetail/openMetadata/createChecksum/copyPath` 流程。

### 2. 小改 `AssetProjectDetailPage.vue`

只做这些小改：

- 导入新组件。
- 增加项目工作台顶部导航数组和跳转函数。
- 在“文件资产”页签里用新组件替换现有平铺文件表区域。
- 保留扫描任务、路径映射、所有弹窗和业务方法。
- 增加几个按 `fileId` 操作的 wrapper：
  - `openPreviewById(fileId)`
  - `openFileDetailById(fileId)`
  - `openMetadataById(fileId)`
  - `createChecksumById(fileId)`
  - `copyPathById(fileId)`
- 这些 wrapper 内部先 `fetchFileAsset(fileId)`，再调用现有方法。

不要删除旧函数，除非 TypeScript 明确提示未使用且删除很小。

### 3. 小改 `AppLayout.vue`

- 导入并调用 `useProjectWorkspaceContext()`。
- 当前已隐藏全局项目切换器，请保留。

### 4. 不要做

- 不要重写 `AssetProjectDetailPage.vue` 全文件。
- 不要修改 `docs/**`。
- 不要修改后端。
- 不要重做权限体系。
- 不要引入二期模型预览/构件级解析。

## 自测

必须执行并记录：

```bash
corepack pnpm --dir frontend build
curl -fsS http://localhost:8080/actuator/health
git diff --check
```

如果没有后端改动，不需要跑 Maven。

## 报告

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 为什么上次重写式实现被中断。
- 本轮改动文件。
- 项目工作台导航如何实现。
- 左目录右文件如何实现。
- 旧操作如何保留。
- 自测结果。
- `<promise>PROJECT_WORKBENCH_NAV_REORG_COMPLETE</promise>`

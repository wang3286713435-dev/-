# 导航与项目工作台重组计划

时间：2026-05-15

## 1. 当前判断

本轮暂停继续扩展二期批次四功能，先处理前端信息架构问题。用户反馈的核心不是视觉细节，而是平台目前没有形成“先选项目，再围绕项目工作”的清晰路径。

当前根因：

1. 登录后默认进入 `/home`，而 `/home` 只是当前 token 项目的概览，不是公司级项目入口。
2. `/data-steward/assets` 已经更接近真实主入口，但还没有被设为默认入口。
3. `/data-steward/assets/:projectId` 当前只是项目资产详情页，不是项目工作台。
4. 顶部全局“当前项目”切换器由 `AppLayout.vue` 实现，切换后还会跳回 `/home`，强化了“全局项目上下文”的旧交互。
5. 工程主数据和工作中心来自后端菜单 `CurrentUserApplicationService.buildMenus(...)`，目前作为全局侧边栏入口存在。
6. 主数据和工作中心页面主要依赖 `authStore.currentProjectId`，不是路由中的 `projectId`。
7. 项目详情页的文件资产是平铺表格；已有的左目录右文件能力在 `AssetCatalogPage.vue`，但还没有落到项目详情页。

## 2. 收束目标

本轮改造目标固定为：

`项目资产总览 -> 进入某个项目 -> 在项目工作台内完成文件资产、工程主数据、工作中心操作`

必须做到：

1. 登录后默认进入 `/data-steward/assets`。
2. `/data-steward/assets/:projectId` 升级为项目工作台。
3. 项目工作台顶部提供项目内导航：
   - 文件资产
   - 工程主数据：工程管理部位树、节点类型、交付物标准
   - 工作中心：文档交付、图纸交付、整改中心、智慧大屏等已有项目内能力
4. 项目详情的文件资产改为“左目录树 + 右文件表”。
5. 文件表保留类型、扩展名、版本、大小和已有操作：预览、详情、治理、补 checksum、复制路径或受控访问入口。
6. 全局顶部不再展示“当前项目”切换器。
7. 全局侧边栏弱化为平台级入口，不再把工程主数据、工作中心作为分散的全局工作入口。

## 3. 推荐方案

采用“项目壳层改造”方案，不做完整 IA 重写。

### 路由

保留旧路由兼容，但新增项目内路由别名：

- `/data-steward/assets/:projectId`
- `/data-steward/assets/:projectId/master-data/sections`
- `/data-steward/assets/:projectId/master-data/node-types`
- `/data-steward/assets/:projectId/master-data/deliverable-standard`
- `/data-steward/assets/:projectId/work/document-delivery`
- `/data-steward/assets/:projectId/work/drawing-delivery`
- `/data-steward/assets/:projectId/work/rectifications`
- `/data-steward/assets/:projectId/work/dashboard`

旧的 `/master-data/**`、`/work/**` 可暂时保留，避免深链路突然断裂；但主导航不再强调它们。

### 项目上下文

项目工作台内以路由 `projectId` 为主。由于当前后端仍要求 token currentProject 与路径项目一致，前端进入项目工作台或项目内子页面时，需要确保：

1. 如果路由 `projectId` 与 `authStore.currentProjectId` 不一致，先调用 `authStore.changeProject(projectId)`。
2. 再加载主数据、交付、整改等页面。
3. 旧全局页面仍可按 `authStore.currentProjectId` 兜底运行。

推荐抽一个前端 composable，例如：

`frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`

职责：

- 读取路由中的 `projectId`。
- 返回当前项目 ID。
- 必要时同步 token 项目上下文。
- 提供项目标签。

### 项目工作台

`AssetProjectDetailPage.vue` 作为项目工作台壳层：

- 顶部显示返回资产总览、项目编码、项目名称、阶段、负责人。
- 顶部项目内导航用 tabs/segmented/nav buttons，不复制全局侧边栏。
- 文件资产作为默认区域。
- 扫描任务、路径映射可以继续保留在项目工作台下，作为项目运维信息。

### 文件资产目录化

优先复用 `AssetCatalogPage.vue` 已有能力：

- 使用 `fetchCatalogDirectories(projectId)` 获取左侧目录。
- 使用 `fetchCatalogFiles({ projectId, directoryPath, ...filters })` 获取右侧当前目录文件。
- 右侧表格使用 `CatalogFile` 渲染基本列。
- 对需要治理、预览、checksum 的操作，按 `fileId` 再调用现有 `fetchFileAsset(fileId)` / `fetchFilePreview(fileId)` / `createChecksumJob(fileId)` 等接口。

这样可以不改后端核心模型，也不重新发明目录系统。

### 全局导航

前端层先收敛：

- `/` redirect 到 `/data-steward/assets`。
- 登录后 guestOnly 已登录跳转到 `/data-steward/assets`。
- 404 fallback 跳转到 `/data-steward/assets`。
- `AppLayout.vue` 移除或隐藏顶部“当前项目”选择器。
- `SidebarMenu.vue` 或菜单数据渲染层过滤/弱化 `home`、`master-data`、`work-center`。
- 侧边栏保留平台级入口：项目资产总览、扫描任务、数据质量、非标准资料治理、Agent 预览等。

## 4. 明确不做

本轮不做：

1. 不改数据库迁移。
2. 不改后端核心业务模型。
3. 不重做权限体系。
4. 不改真实 NAS 数据。
5. 不做模型轻量化。
6. 不做文件正文抽取、向量库或 Agent 自动动作。
7. 不清理样板项目。
8. 不修改 `docs/**`。

## 5. 风险点

1. 主数据和工作中心当前依赖 token 项目上下文，直接改成路由项目可能触发 `CORE_PROJECT_CONTEXT_MISMATCH`。必须先同步 token 项目或保持旧路由兜底。
2. `AssetProjectDetailPage.vue` 已经很大，开发 agent 不应在里面继续无边界膨胀；如改动明显变大，应抽一个项目文件浏览组件。
3. 文件资产目录化不能丢掉治理、预览、下载、checksum 和详情抽屉。
4. 全局菜单不能一次性删到用户无法找到扫描、数据质量、Agent 预览等平台级功能。

## 6. 开发 agent 执行 prompt

见 `handoff/dev-agent/current-prompt.md`。

## 7. 测试 agent 专项回归建议

需要测试 agent 做专项回归。本轮不是普通样式调整，而是入口、路由、项目上下文和文件资产交互结构调整，必须专项验收。

见 `handoff/test-agent/current-prompt.md`。

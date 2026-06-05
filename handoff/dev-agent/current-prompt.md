# 开发 Agent 当前任务：UX4-A 前端壳层与项目工作台导航重构

你是卓羽智能数据中台的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/ux4-frontend-shell-routing`

## 0. 本批定位

本批是 `UX4-A：壳层和路由基线`。

本批不是新增业务功能，不是重做后端，不是继续对象化迁移，不是新增 Hermes / BIM 能力。

目标是把当前平台前端从“早期 MVP 功能堆叠”升级为“成熟 SaaS 项目管理仪表盘”的壳层与导航基线。

一句话目标：

> 员工进入平台后，能清楚知道：先选项目，再进入项目工作台，然后在当前项目内做文件管理、工程主数据、交付闭环、BIM 协同和档案目录。

## 1. 必须先读

开始前必须阅读：

- `docs/13-ux4-frontend-architecture-baseline.md`
- `docs/11-current-baseline-and-next-roadmap.md`
- `docs/12-api-contract-and-maintenance.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

重点检查当前前端：

- `frontend/src/router/index.ts`
- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/core/components/SidebarMenu.vue`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/core/composables/useProjectWorkspaceContext.ts`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/styles/index.css`

## 2. 必须看视觉参考图

用户明确要求：开发 agent 必须看预览图，不允许只按文字想象。

请打开并查看以下 5 张参考图：

1. `/Users/vc/Downloads/ChatGPT Image Jun 5, 2026, 10_37_44 AM.png`
2. `/Users/vc/Downloads/ChatGPT Image Jun 5, 2026, 10_37_41 AM.png`
3. `/Users/vc/Downloads/ChatGPT Image Jun 5, 2026, 10_37_38 AM.png`
4. `/Users/vc/Downloads/ChatGPT Image Jun 5, 2026, 10_37_36 AM.png`
5. `/Users/vc/Downloads/ChatGPT Image Jun 5, 2026, 10_37_31 AM.png`

你需要从图中提炼，而不是照抄品牌视觉：

- 左侧深色品牌导航。
- 顶部项目上下文、项目选择、搜索、通知、用户区。
- 项目身份区：项目封面、项目名、编码、负责人、当前角色、当前阶段。
- 项目内一级 tab：概览、文件管理、工程主数据、交付闭环、BIM 协同、档案目录。
- 页面主体按当前 tab 展示，不再重复堆多个导航模块。
- 右侧辅助栏用于进度、风险、快捷操作、最近动态。
- 表格和卡片保持企业中后台可读性。

报告中必须写明：

- 你实际查看了哪些参考图。
- 从参考图复用了哪些结构。
- 哪些地方没有照搬，原因是什么。

## 3. 本批允许范围

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

如确实需要改 `docs/**`、`backend/**`、数据库迁移、接口语义、权限规则，必须停止并报告，不得自行修改。

## 4. 本批严禁

严禁：

- 修改 `backend/**`。
- 修改数据库迁移。
- 新增后端接口。
- 修改权限或 file-access 规则。
- 修改对象存储读取规则。
- 新增 Hermes evidence 能力。
- 新增 BIM 引擎能力。
- 读取文件正文。
- 暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、token、secret。
- 删除旧路由导致旧链接白屏。

## 5. 本批必须完成

### A. 全局壳层重构

参考图方向重构 `AppLayout.vue`：

- 左侧保留深色品牌导航。
- 顶部栏改成成熟 SaaS 项目管理布局：
  - 面包屑。
  - 当前项目选择器或项目上下文。
  - 全局搜索入口。
  - 通知 / 帮助占位。
  - 当前用户信息。
- 顶部栏左侧的“返回资产总览”保留，但必须融入面包屑或返回区域，不要像临时按钮。
- 去掉“当前项目工作台”这类技术胶囊。
- 左侧与顶部在 1280 / 1440 / 1920 宽度下不能挤压、不能横向溢出。

### B. 左侧导航收敛

重构 `SidebarMenu.vue` 和菜单展示逻辑：

左侧只保留全局入口：

```text
项目启动台
资产治理
BIM 协同
管理中心
我的项目
帮助中心
Hermes 助手
```

要求：

- 项目内功能不要继续作为左侧一级入口铺满。
- `我的项目` 用于展示最近/常用项目入口，如当前数据不足，可先用用户授权项目列表。
- 管理中心只对有权限用户展示。
- 旧菜单权限不能被破坏。

### C. 项目工作台一级 tab 基线

项目内一级 tab 固定为：

```text
概览
文件管理
工程主数据
交付闭环
BIM 协同
档案目录
```

要求：

- 在项目详情页顶部只保留一套项目内 tab，不再重复显示多个项目工作台导航模块。
- tab 样式参考图，轻量、清晰、有当前选中状态。
- tab 点击规则：
  - 概览 -> `/data-steward/assets/:projectId?tab=dashboard`
  - 文件管理 -> `/data-steward/assets/:projectId?tab=files`
  - 工程主数据 -> `/data-steward/assets/:projectId?tab=ownership`
  - 交付闭环 -> `/data-steward/assets/:projectId/work/document-delivery`
  - BIM 协同 -> `/bim-collaboration?projectId=:projectId`
  - 档案目录 -> `/data-steward/assets/:projectId/work/delivery-package`

### D. 项目身份区重构

重构 `AssetProjectDetailPage.vue` 项目头部：

必须展示：

- 项目封面图或默认建筑封面。
- 项目名称。
- 项目编码。
- 负责人。
- 当前角色。
- 当前阶段。
- 当前主状态。
- 收藏或标记图标可占位。

必须隐藏到折叠区：

- 平台内部 ID。
- assetSource。
- assetStatus。
- 复杂技术标签。
- 对象存储 provider。

### E. 旧链接兼容

必须保留旧链接兼容跳转：

- `/master-data/*`
- `/work/*`
- `/data-steward/models`
- `/data-steward/objects`
- `/digital-twin`

旧链接不能白屏。

如果没有当前项目上下文：

- 跳回 `/data-steward/assets`
- 提示“请先选择项目”

### F. 样式基线

样式方向：

- 精致企业 SaaS。
- 左侧深蓝导航。
- 浅色内容背景。
- 白色卡片。
- 细边框。
- 柔和阴影。
- 蓝色主色。
- 绿 / 橙 / 红用于状态。

禁止：

- 大面积毛玻璃影响可读性。
- 营销风 hero。
- 大面积深色内容区。
- 过度动画。
- 多套不同风格混用。

## 6. 当前批次不做

本批不迁移具体业务页面主体，不重做完整文件管理器表格，不重做工程主数据内容，不重做交付闭环表格。

本批只做：

- 壳层。
- 左侧导航。
- 顶部栏。
- 项目身份区。
- 项目内一级 tab。
- 旧路由兼容。

后续 `UX4-B` 到 `UX4-F` 再逐页重做业务页面。

## 7. 验收标准

必须满足：

1. Fresh login 后仍进入 `/data-steward/assets`。
2. 左侧导航更接近参考图，不再铺满项目内细碎功能。
3. 顶部栏包含项目上下文、搜索、用户区，视觉上不再像临时开发页。
4. 进入 `503 / 105` 项目，项目身份区清楚展示项目名称、编码、负责人、阶段、角色。
5. 项目内一级 tab 清楚展示：概览、文件管理、工程主数据、交付闭环、BIM 协同、档案目录。
6. 页面不再出现重复项目工作台大导航模块。
7. 旧链接兼容跳转不白屏。
8. 1280 / 1440 / 1920 宽度无横向溢出。
9. 不泄露真实 NAS 路径、bucket、object key。
10. 不修改后端、docs、数据库迁移。

## 8. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
git diff --check
```

浏览器自测：

- 打开 `http://127.0.0.1:5173/data-steward/assets`
- 进入 `105 / projectId=503`
- 检查顶部栏、左侧导航、项目身份区、项目内 tab
- 打开旧链接 `/master-data/sections`
- 打开旧链接 `/work/document-delivery`
- 打开 `/bim-collaboration?projectId=503`
- 分别在 1280 / 1440 / 1920 宽度检查无横向溢出

## 9. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 实际查看了哪些参考图。
2. 从参考图复用了哪些结构。
3. 修改了哪些文件。
4. 全局壳层如何变化。
5. 左侧导航如何变化。
6. 项目内 tab 如何变化。
7. 旧链接兼容策略。
8. 自测结果。
9. 是否改动后端 / docs / 数据库。
10. 未完成项和风险。

## 10. 完成定义

只有同时满足以下条件，才能认为 UX4-A 完成：

- 平台壳层明显从 MVP 形态升级为成熟 SaaS 项目管理界面。
- 项目工作台不再重复导航。
- 用户能清楚从左侧、顶部、项目 tab 理解自己在哪里。
- 旧链接不白屏。
- 前端构建通过。
- `handoff/dev-agent/latest-report.md` 已写。

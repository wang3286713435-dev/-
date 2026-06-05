# 开发 Agent 当前任务：UX4 平台视觉与员工使用效率修复

你是卓羽智能数据中台的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前批次：

`UX4：平台视觉与员工使用效率修复`

## 0. 批次定位

本批不是新增业务能力，不是继续对象化迁移，不是做 Hermes / BIM 新功能。

本批目标是把当前已经开发出来的核心功能做得更容易用、更清楚、更适合员工日常试用。

一句话目标：

> 员工进入平台后，能快速知道先选哪个项目、在哪里管文件、在哪里看项目状态、在哪里处理交付，不被技术字段和复杂入口干扰。

## 1. 必读文件

开始前先读：

- `docs/11-current-baseline-and-next-roadmap.md`
- `docs/12-api-contract-and-maintenance.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

重点检查前端：

- `frontend/src/router/index.ts`
- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/core/components/SidebarMenu.vue`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/modules/visualization/bim-collab/**`
- `frontend/src/styles/**`

## 2. 允许范围

只允许修改：

- `frontend/**`
- `handoff/dev-agent/latest-report.md`

如确实需要修改 `handoff/dev-agent/current-prompt.md` 以记录执行状态，可以说明原因后最小改动。

## 3. 禁止范围

严禁：

- 修改 `backend/**`
- 新增 Flyway / 数据库迁移
- 修改接口语义或权限规则
- 修改真实 NAS 文件
- 执行对象化迁移任务
- 新增 Hermes 正文问答、parser、documents/chunks、Qdrant、OpenSearch
- 新增 BIM 构件级能力
- 修改 `docs/**`
- 把低频工具做成新的主入口
- 删除旧路由导致历史链接失效

如果你认为必须改后端，立即停止并在报告里说明，不要擅自改。

## 4. 视觉与体验原则

本批按“精致企业工具”处理，不做炫酷营销页。

要求：

- 视觉服务于员工效率，不为装饰而装饰。
- 主色只用于当前选中、主操作和状态强调。
- 少用大段说明文字，用结构和操作引导用户。
- 不使用大面积玻璃、强模糊、霓虹、渐变文字。
- 表格、文件管理、权限、写操作必须清晰可靠。
- 技术字段默认折叠，例如内部 ID、checksum、object version、diagnostic、trace。
- 每个关键页面必须让用户知道：
  - 当前在哪里
  - 现在能做什么
  - 下一步最推荐做什么

## 5. 本批优先修复点

请先做一次前端现状巡检，再按优先级修复。不要一次性大重构。

### A. 项目入口与资产总览

目标：

- 员工打开平台后，第一眼知道“先选项目”。
- 真实项目、最近项目、待处理项目清晰。
- 搜索、排序、进入项目、文件管理、项目可视化入口明显。

请修复：

- 过多说明文字。
- 过多平级入口。
- 过度技术化标签。
- 主操作不突出。
- 项目卡片/列表信息噪音过多。

### B. 项目工作台

目标：

- 项目工作台围绕三个核心动作：
  - 文件管理
  - 项目可视化 / BIM 协同
  - 交付状态
- 工程主数据、文件服务、对象存储、低频工具不要抢首屏。

请修复：

- “项目资产、工程主数据、交付工作中心、BIM、对象存储”入口层级混乱。
- 工作台中仍显眼展示内部技术状态。
- 用户不知道下一步该点哪里。

### C. 文件管理器

目标：

- 文件管理器像 Windows / macOS 文件管理器一样顺手。
- 当前目录、搜索模式、选中数量、存储状态、预览状态要清楚。
- 全项目搜索和当前目录浏览不能混淆。

请重点检查并修复：

- 搜索状态提示不清楚。
- 目录模式和搜索模式视觉混在一起。
- 对象存储状态展示过技术化。
- 文件夹 / 文件行操作过密。
- 右键菜单、顶部工具栏和行内操作重复。
- 未入库文件、NAS_ONLY、OBJECT_STORED、UNREGISTERED 的业务说明不清楚。

### D. 工程树与交付工作中心

目标：

- 用户能理解：先看文件归属，再看交付缺什么，再人工补交。
- 不要让工程树像一个孤立功能。

请修复：

- 工程树、文件归属、文档交付、图纸交付之间的跳转提示。
- 缺失项说明过技术化。
- 待交付候选入口不明显。
- “正式交付资料”和“过程资料/归档资料”区分不够清楚。

### E. 文件服务与对象存储

目标：

- 让用户看懂对象化覆盖率，但不被存储术语淹没。
- 105 完成样板要清楚，其他项目待对象化要清楚。

请修复：

- `OBJECT_STORED`、`NAS_ONLY`、`MIGRATION_FAILED` 等状态可以保留，但要有业务化解释。
- 不展示 bucket、object key、真实 NAS 路径。
- 避免把对象化说成“AI 已理解文件”。

### F. BIM 协同 / 综合驾驶舱

目标：

- 默认首屏是综合驾驶舱。
- READY Viewer 模型入口清楚。
- 轻量化模型分页清晰。
- 不把未完成的构件级能力暗示为已完成。

请修复：

- “Viewer 暂不可用”类过时提示。
- 模型列表分页、状态、打开预览入口不够清楚。
- 视觉和数据管家主界面风格割裂。

## 6. 必须保持的兼容

- Fresh login 仍进入 `/data-steward/assets`。
- 旧链接必须兼容，不白屏：
  - `/master-data/*`
  - `/work/*`
  - `/data-steward/models`
  - `/data-steward/objects`
  - `/data-steward/assets/:projectId?tab=files`
  - `/bim-collaboration?projectId=503&preview=glandar`
- 503 / 105 和 506 / 93 项目都要能打开。
- 文件管理器 direct-only 浏览、全项目搜索、PDF 受控预览、模型 Viewer 入口不能回归。
- 对象存储 file-access 和 M3G-9 覆盖率页面不能回归。

## 7. 建议开发方式

请按小步提交思路开发，不要一次性推翻整个前端：

1. 先做视觉 / 可用性巡检，列出你准备修的 5-10 个点。
2. 优先修影响员工理解和效率的点。
3. 每修完一组页面，浏览器短验一次。
4. 不要修改后端。
5. 完成后写报告。

## 8. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g9-objectification-coverage-report.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

浏览器至少检查：

- `http://127.0.0.1:5173/data-steward/assets`
- `http://127.0.0.1:5173/data-steward/assets/503?tab=files`
- `http://127.0.0.1:5173/data-steward/assets/503?tab=file-service`
- `http://127.0.0.1:5173/data-steward/assets/503/work/document-delivery`
- `http://127.0.0.1:5173/data-steward/assets/503/work/drawing-delivery`
- `http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`

检查宽度：

- 1280
- 1440
- 1920

## 9. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 本轮修复了哪些员工使用问题。
2. 修改了哪些文件。
3. 是否改动后端，预期应为否。
4. 旧路由兼容情况。
5. 浏览器检查结果。
6. 构建和回归脚本结果。
7. 仍需后续 UX 批次处理的问题。

## 10. 完成定义

只有同时满足以下条件，才算完成：

- 员工主路径更清楚。
- 文件管理器更好用。
- 工程树和交付工作中心关系更清楚。
- 对象存储 / 文件服务状态更容易理解。
- BIM 协同入口和 Viewer 状态更清楚。
- 没有后端、数据库、权限、API 语义改动。
- 前端构建通过。
- 必要回归脚本通过。
- `handoff/dev-agent/latest-report.md` 已写。

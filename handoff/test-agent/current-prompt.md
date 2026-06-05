# 测试 Agent 当前任务：UX4 平台视觉与员工使用效率修复验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收批次：

`UX4：平台视觉与员工使用效率修复`

本批是前端体验修复，不是后端新功能批次。测试目标是确认：

- 员工主路径是否更清楚。
- 关键页面是否可用、无明显视觉/交互问题。
- 旧功能、旧路由和安全边界没有被破坏。
- 开发 agent 没有越界修改后端、数据库、docs 或新增业务能力。

## 1. 必读文件

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `docs/11-current-baseline-and-next-roadmap.md`

## 2. 必跑命令

执行并记录结果：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g9-objectification-coverage-report.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如果本批没有改后端，可以不跑后端构建；如发现后端文件被改动，直接判 P1 或 P0，并补跑后端构建。

## 3. 越界检查

必须检查：

- 不应修改 `backend/**`。
- 不应新增 Flyway / 数据库迁移。
- 不应修改 `docs/**`。
- 不应新增 Hermes 正文问答。
- 不应新增 BIM 构件级能力。
- 不应新增 parser、documents/chunks、Qdrant、OpenSearch。
- 不应执行真实对象化迁移或真实 NAS 文件破坏性操作。

如出现后端业务逻辑、权限或数据库改动，按 P1 起判；如导致权限泄露、路径泄露或真实文件风险，按 P0 判。

## 4. 浏览器轻验

本轮不要求逐页深度点击，但必须覆盖主路径。

### A. 资产总览

打开：

`http://127.0.0.1:5173/data-steward/assets`

检查：

- 用户能清楚知道先选项目。
- 真实项目、推荐项目、最近项目或项目列表入口清楚。
- 不应被大段教程文字和技术标签淹没。
- 搜索、刷新、进入项目入口可见。

### B. 105 文件管理

打开：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files`

检查：

- 目录浏览和搜索模式能区分。
- 当前目录、选中数量、存储状态说明清楚。
- 右键菜单 / 顶部工具栏 / 行内操作没有明显重复混乱。
- 不展示真实 NAS 路径、bucket、object key、`storage_uri`。
- PDF 受控预览、模型 Viewer 入口不回归。

### C. 文件服务 / 对象存储

打开：

`http://127.0.0.1:5173/data-steward/assets/503?tab=file-service`

检查：

- 105 对象化完成状态清楚。
- 非 105 项目仍未全部完成的口径清楚。
- 对象存储状态不应误导为“AI 已理解文件”。
- 不展示底层路径和对象 key。

### D. 交付工作中心

打开：

- `http://127.0.0.1:5173/data-steward/assets/503/work/document-delivery`
- `http://127.0.0.1:5173/data-steward/assets/503/work/drawing-delivery`

检查：

- 文档/图纸交付、缺失项、补交、审核、预检查关系更清楚。
- 页面不应出现横向溢出。
- 关键操作没有被隐藏到找不到。

### E. BIM 协同

打开：

`http://127.0.0.1:5173/bim-collaboration?projectId=503&preview=glandar`

检查：

- 默认首屏为综合驾驶舱。
- READY Viewer 状态清楚。
- 轻量化模型列表分页和打开预览入口不回归。
- 不展示“构件级能力已完成”的误导性内容。

### F. 多分辨率

至少检查：

- 1280
- 1440
- 1920

重点看：

- 无横向撑爆。
- 主操作不丢失。
- 文字不严重挤压。
- 侧边栏、顶部栏和表格区域不互相覆盖。

## 5. 旧链接兼容

抽查以下旧链接不白屏、不丢项目上下文：

- `/master-data/sections`
- `/work/document-delivery`
- `/data-steward/models`
- `/data-steward/objects`

允许它们跳转到当前项目工作台内对应页面。

## 6. P0 / P1 / P2 判定

P0：

- 权限泄露。
- 真实 NAS 路径、bucket、object key、token、secret 泄露。
- 后端业务逻辑或数据库被改坏。
- 主路径白屏，项目进不去，文件管理器无法使用。
- file-access 受控访问回归失败。

P1：

- 开发 agent 修改了后端或 docs，但未说明必要性。
- 旧链接白屏或项目上下文丢失。
- 文件管理器搜索/目录浏览明显错乱。
- BIM 协同 READY Viewer 入口回归。
- 对象化状态展示误导用户。
- 页面主要操作不可见或严重挤压。

P2：

- 少量文案粗糙。
- 轻微间距问题。
- 既有 Vite chunk warning。
- 个别低频页面仍有技术字段偏显眼，但不影响主路径。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

1. 结论：通过 / 不通过。
2. P0 / P1 / P2 列表。
3. 必跑命令结果。
4. 越界检查结果。
5. 浏览器轻验结果。
6. 是否建议主 agent 收口 UX4。

# 二期 6A 收口报告：项目初始化与标准模板化

收口时间：2026-05-18

## 1. 收口结论

二期 6A 正式收口。

测试 agent 已完成专项验收和 P0 极短复验。上轮唯一 P0 “项目详情页缺少 `工程主数据 -> 初始化向导` 主链路入口”已关闭。当前无新的 P0/P1，仅保留既有 Vite chunk size warning P2，不影响本批收口。

本批达成目标：

`项目进入 -> 初始化状态评估 -> 标准模板查询 -> 模板影响预览 -> 确认应用 -> 标准底座就绪 -> 进入部位树/交付物标准`

## 2. 已完成能力

### 后端

新增项目初始化与标准模板接口：

- `GET /api/master-data/projects/{projectId}/initialization/status`
- `GET /api/master-data/standard-templates`
- `GET /api/master-data/standard-templates/{templateCode}`
- `POST /api/master-data/projects/{projectId}/initialization:preview-template`
- `POST /api/master-data/projects/{projectId}/initialization:apply-template`

内置模板：

- 编码：`MEP_BIM_BASIC`
- 名称：`建筑机电 BIM 交付基础模板`
- 行业：`建筑机电/BIM交付`

模板覆盖：

- 部位树
- 节点类型
- 交付物定义
- 交付物类型
- 交付物属性
- 目录模板

实现规则：

- 应用前必须确认。
- 模板应用幂等。
- 只补齐缺失项，不覆盖、不删除、不改名已有项目数据。
- 模板创建的节点类型会自动锁定。
- 如果已有锁定节点类型且模板仍需新增节点类型，则阻塞应用。
- 应用成功写审计：`masterdata.initialization.template-apply`。
- 未新增 Flyway migration，未修改旧 migration。

### 前端

新增项目内初始化向导：

- `/data-steward/assets/:projectId/master-data/initialization`

项目工作台导航新增入口：

- `工程主数据 -> 初始化向导`

页面能力：

- 当前项目标准状态卡片。
- 内置模板列表。
- 模板详情表格。
- 模板影响预览。
- 应用前确认。
- 应用结果展示。
- 跳转部位树和交付物标准。

## 3. 关键修复记录

### P0：项目详情页导航断裂

问题：

- 测试 agent 首轮验收发现，直接访问初始化页可用，但从 `/data-steward/assets` 进入项目详情页后，看不到 `工程主数据 -> 初始化向导`。

根因：

- 项目详情页 `data-steward-asset-detail` 使用 `assetProjectContext=true`，全局布局不会显示 `ProjectWorkspaceNav`。
- `AssetProjectDetailPage.vue` 自身也没有补统一项目工作台导航。

修复：

- 在 `AssetProjectDetailPage.vue` 顶部加入 `ProjectWorkspaceNav`。
- 现在从项目列表进入项目详情后，可直接看到 `工程主数据 -> 初始化向导`。

复验：

- 测试 agent 极短复验通过，P0 已关闭。
- 项目详情页和初始化页页面级横向溢出均为 `0`。

## 4. 验收结果

测试 agent 报告：

- `handoff/test-agent/latest-report.md`

首轮专项验收：

- 后端构建：通过。
- 前端构建：通过。
- 健康检查：通过。
- `git diff --check`：通过。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh`：通过。
- OpenAPI 新接口：通过。
- 匿名访问拒绝：通过。
- 当前项目不匹配拒绝：通过。
- 跨项目无权限拒绝：通过。
- 模板预览/应用/幂等：通过。
- 审计：通过。
- 节点类型锁定阻塞：通过。
- 不覆盖/不删除/不改名：通过。
- 初始化向导页面本身：通过。

P0 极短复验：

- 从 `/data-steward/assets` 进入项目详情：通过。
- 项目详情页顶部显示统一项目工作台导航：通过。
- `工程主数据 -> 初始化向导` 入口存在：通过。
- 点击后进入 `/data-steward/assets/:projectId/master-data/initialization`：通过。
- 保持同一项目上下文：通过。
- 项目详情页横向溢出：`0`。
- 初始化页横向溢出：`0`。

## 5. 边界确认

本批未进入以下范围：

- 未做真实 NAS 新建、上传、移动、删除、改名。
- 未做 BIM 轻量化在线预览。
- 未做构件级解析、构件级搜索、构件定位或高亮。
- 未做 PDF/Office/CAD/BIM 正文抽取。
- 未写入向量库、OpenSearch、Qdrant、MinIO 或 documents/chunks。
- 未让 Hermes 自动初始化、自动审批或写库。
- 未做多行业模板市场。
- 未承诺客户生产级复杂权限体系。

## 6. 已知非阻塞项

- 前端生产构建仍有既有 Vite chunk size warning，归类为 P2。
- `MEP_BIM_BASIC` 当前是基础模板，不是完整企业标准库；后续可在模板体系成熟后扩展更多模板。
- 专项脚本会创建隔离验收项目，测试结束后由测试 agent 软删除，避免污染真实项目。

## 7. 下一步建议

6A 收口后，建议进入二期下一批规划。

优先建议：

`6B：文件预览与批量交付增强`

建议范围：

- PDF/Office/图片等文件预览策略完善。
- 下载权限和预览权限继续分离。
- 批量挂接、批量导出、交付目录导出。
- 交付视图可用性增强。

仍建议暂缓：

- BIM 轻量化。
- 构件级解析。
- 正文抽取和 selective indexing。
- Hermes 写操作。
- 真实 NAS 写操作。

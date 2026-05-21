# 开发 Agent 报告：UX2 使用逻辑与字段减负续接

时间：2026-05-22

`<promise>MAINLINE_UX2_USER_EXPERIENCE_REFACTOR_COMPLETE</promise>`

## 1. 本轮目标

本轮在 Claude Code 已完成的 UX2 高级感视觉升级基础上继续开发，没有回滚其 lightfield / spotlight / 粒子 / glass-lite 视觉底座。

本轮目标不是继续单纯美化 UI，而是围绕员工主路径减负：

`选择项目 -> 看资产 -> 确认主数据 -> 做交付 -> 查整改 / 预检查`

目标口径：

- 普通员工进入平台后能看懂当前在哪个项目。
- 能理解下一步该做什么。
- 默认不被平台内部 ID、checksum、扫描诊断等技术字段干扰。
- 旧链接继续兼容，不白屏、不丢项目上下文。

## 2. 读取的关键材料

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/ux2-user-experience-refactor-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

## 3. 改动文件列表

本轮修改：

- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
- `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
- `frontend/src/modules/work-center/pages/RectificationsPage.vue`
- `frontend/src/styles/index.css`
- `handoff/dev-agent/latest-report.md`

已保留 Claude 视觉升级基线，包括但不限于：

- `frontend/src/styles/effects.css`
- `frontend/src/styles/tokens.css`
- `frontend/src/modules/core/components/ParticleField.vue`
- `frontend/src/modules/core/composables/useSpotlight.ts`
- `frontend/src/modules/auth/pages/LoginPage.vue`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`

当前工作区仍存在主 agent / Claude 留下的其他未提交文件或临时目录，例如 `.claude/**`、`CLAUDE.md`、`tmp/**`、部分 handoff 状态文件。本轮未编辑这些文件，建议后续提交时单独筛选。

## 4. 使用逻辑优化

### 4.1 资产总览：项目入口台

资产总览从“统计和项目表”前移为“项目入口台”：

- 增加“推荐进入”，优先引导真实项目或当前最需要处理的项目。
- 增加“待处理项目”，把主数据、交付标准、接入状态未就绪的项目放到员工视线前面。
- 增加“最近进入”，使用浏览器本地记录最近打开项目，方便员工续接工作。
- 项目列表仍保留，默认弱化测试 / 样例 / 历史项目，筛选能力不删除。
- 统计、扫描、导出、文件服务等低频信息不抢主视觉。

### 4.2 项目工作台：下一步动作前置

项目详情页保留三段链路：

- `项目资产`
- `工程主数据`
- `交付工作中心`

并新增“推荐下一步动作”：

- 文件管理
- 初始化向导 / 工程主数据
- 文档交付
- 图纸交付
- 整改闭环

低频入口进入“更多工具”折叠区，不再一屏平铺所有入口。

### 4.3 工程主数据：解释“先定义规则”

在初始化向导、部位树、节点类型、交付物标准页面补充下一步说明：

- 初始化向导说明：接入向导只生成草案，真实交付前要复核规则。
- 部位树说明：先确认部位树，再锁定节点类型。
- 节点类型说明：节点类型锁定后，平台才能按规则计算应交项。
- 交付物标准说明：标准确认后，再进入文档 / 图纸交付。

业务逻辑未改变，仅增强说明和动作入口。

### 4.4 交付与整改：闭环说明

整改中心新增“整改闭环”说明：

- 审核驳回后在这里处理原因。
- 处理中 / 已处理用于记录处理进度。
- 已关闭代表本轮整改确认完成，可按需重新打开。

文档 / 图纸交付业务逻辑、批量补交、审核、整改、dry-run 导出预检查均未改动。

## 5. 字段减负

文件管理默认展示员工最常用字段：

- 文件名
- 类型
- 版本
- 大小
- 专业
- 状态
- 操作

以下技术字段默认进入“技术信息 / 诊断”展开列：

- 平台文件 ID
- 扩展名
- 置信度
- 更新时间

没有删除数据能力，也没有影响预览、详情、治理、补 checksum、更多操作。未新增真实 NAS 路径展示。

项目列表也新增“显示详细字段 / 收起详细字段”，默认隐藏阶段、模型数、主要类型、模型容量、底座、状态等低频字段。

## 6. 旧链接兼容

浏览器抽查旧入口均能跳转到当前项目工作台内对应页面，未出现白屏：

- `/master-data/initialization`
- `/master-data/sections`
- `/work/document-delivery`
- `/work/rectifications`
- `/data-steward/models`

现有路由兼容逻辑未删除。如果当前用户没有当前项目，仍按既有逻辑回到 `/data-steward/assets`。

## 7. 浏览器自测结果

使用本地浏览器登录 `platform.admin` 后抽查：

- `/data-steward/assets` 可见“项目入口台 / 推荐进入 / 待处理项目 / 最近进入 / 项目列表”。
- 资产总览默认表头聚焦项目、接入状态、负责人、下一步动作、文件数、最近扫描/更新、操作。
- `/data-steward/assets/503?tab=files` 可见“推荐下一步动作”和“更多工具”。
- 文件管理默认表头为“文件名 / 类型 / 版本 / 大小 / 专业 / 状态 / 操作”，技术信息需手动展开。
- 页面显示“真实 NAS 写入灰度未开启”状态条，未出现误导性写入入口。
- 1280 / 1440 / 1920 宽度抽查未发现横向溢出。

## 8. 自测命令与结果

```text
corepack pnpm --dir frontend build                   PASS（仅既有 Vite chunk size 警告）
curl -fsS http://127.0.0.1:8080/actuator/health       PASS：{"status":"UP"}
bash scripts/dev/check-m2b-nas-write-trial.sh         PASS=18 FAIL=0
bash scripts/dev/check-m2a-controlled-nas-write.sh    PASS=21 FAIL=0
bash scripts/dev/check-m1f-employee-access-control.sh PASS=20 FAIL=0
bash scripts/dev/check-m1e-file-task-continuity.sh    PASS=10 FAIL=0
bash scripts/dev/check-m1d-standard-delivery-loop.sh  PASS=29 FAIL=0
bash scripts/dev/check-m1c-real-project-masterdata.sh PASS=14 FAIL=0
git diff --check                                      PASS
```

## 9. 边界确认

- 是否修改 `backend/**`：否。
- 是否修改数据库迁移：否。
- 是否修改 `docs/**`：否。
- 是否引入 Hermes 新能力：否。
- 是否引入 BIM 轻量化：否。
- 是否新增真实 NAS 写能力：否。
- 是否读取文件正文：否。
- 是否写 OpenSearch / Qdrant / MinIO / documents / chunks：否。
- 是否写死 105 / 503 / 93 / 506 逻辑：否。
- 是否删除旧路由：否。

## 10. P0 / P1 / P2

- P0：无。
- P1：无。
- P2：
  - 前端构建仍有历史 Vite chunk size 警告，不影响本轮功能。
  - 工作区存在其他 agent 留下的 `.claude/**`、`CLAUDE.md`、`tmp/**` 和部分 handoff 状态文件，建议提交时只纳入本轮允许范围。

## 11. 是否建议进入 UX2 整体验收

建议进入 UX2 测试 agent 整体验收。

建议重点验收：

1. 普通员工是否能从资产总览自然进入真实项目。
2. 项目工作台是否能理解“先看资产、再确认规则、再做交付、最后查整改”。
3. 文件管理默认字段是否足够清楚，技术信息展开是否仍可用。
4. 旧链接是否全部可用且不白屏。
5. 真实 NAS 路径、文件正文、Hermes 能力、后端权限规则是否无意外扩展。

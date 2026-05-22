# 开发 Agent 报告：UX3 主视图聚焦与认知减负

时间：2026-05-22

`<promise>MAINLINE_UX3_MAIN_VIEW_FOCUS_COMPLETE</promise>`

## 1. 本轮目标

本轮执行 `UX3：主视图聚焦与认知减负`。

目标不是新增业务能力，而是做主视图减法：

- 资产总览改成更直接的“项目启动台”。
- 项目工作台首屏聚焦三个核心入口：文件管理、项目可视化、交付状态。
- 大段教程、偏技术标签和低频入口降级到折叠区或更多入口。
- 保留 UX2 lightfield / spotlight / glass-lite 视觉升级。
- 保留旧路由兼容。

## 2. 改动文件列表

本轮修改：

- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/core/layout/AppLayout.vue`
- `handoff/dev-agent/latest-report.md`

未修改：

- `backend/**`
- `docs/**`
- 数据库迁移
- 后端接口
- 权限规则

当前工作区仍存在其他 agent / 主 agent 留下的 handoff 状态文件、`.claude/**`、`CLAUDE.md`、`tmp/**` 等非本轮交付内容。本轮未编辑这些文件。

## 3. 主视图减法

### 3.1 资产总览

资产总览标题从“资产总览”改为“项目启动台”。

首屏去掉原先占据主视觉的 `FLOW / STATE / ALERT` 大块教程式区域，改为：

- 简短一句说明：先选项目，再进入文件管理、项目可视化或交付状态。
- 推荐项目卡片。
- 推荐项目的三个直接动作：进入项目、文件管理、项目可视化。
- 待处理项目。
- 最近项目。
- 项目概况小摘要。
- 项目列表。

统计和风险没有删除，但进入“统计和风险摘要”折叠区，不再抢首屏。

### 3.2 项目工作台

项目工作台首屏从“命令中心 + 三段教程 + 5 个下一步动作”收敛为：

- 顶部命令中心直接提供：文件管理、项目可视化、交付状态、项目详情、刷新。
- 核心入口固定为三张卡：项目可视化、文件管理、交付状态。
- 工程主数据仍保留，但不再比三个核心入口更抢眼。
- 未就绪提示移到核心入口之后，作为交付前置条件提示。

`平台内部ID`、`NAS_REAL_PILOT`、`ACTIVE` 等偏技术标签从顶部强展示移入“项目详情 / 技术信息”折叠区。

### 3.3 项目内导航

`ProjectWorkspaceNav` 继续保留三段语义：

- 项目资产
- 工程主数据
- 交付工作中心

但视觉上压缩为主入口 + 更多入口：

- 项目资产主入口：项目可视化、文件管理。
- 工程主数据主入口：工程主数据。
- 交付工作中心主入口：交付状态、文档交付。
- 模型集成、管理对象、事项、任务、导出、文件服务、部位树、节点类型、交付物标准、图纸交付、整改闭环、交付治理助手进入“更多入口”。

低频入口没有删除，旧页面仍可访问。

## 4. 文件管理和项目可视化入口

文件管理入口更明显：

- 资产总览推荐项目卡片有“文件管理”按钮。
- 项目列表每行有“文件”快捷操作。
- 项目工作台顶部命令中心有“文件管理”按钮。
- 项目工作台核心入口卡片包含“文件管理”。
- 项目内导航项目资产段保留“文件管理”主入口。

项目可视化入口更明显：

- 资产总览推荐项目卡片有“项目可视化”按钮。
- 项目列表每行有“可视化”快捷操作。
- 项目工作台顶部命令中心有“项目可视化”按钮。
- 项目工作台核心入口卡片包含“项目可视化”。
- 文案使用“项目可视化 / 资产驾驶舱”，没有承诺真实 BIM 轻量化。

## 5. 旧链接兼容

浏览器检查以下旧入口，均未白屏、未 404，并跳转到当前项目工作台对应页面：

- `/master-data/sections`
- `/master-data/node-types`
- `/master-data/initialization`
- `/master-data/deliverable-standard`
- `/work/document-delivery`
- `/work/drawing-delivery`
- `/work/rectifications`
- `/work/agent-governance`
- `/work/dashboard`
- `/data-steward/models`
- `/data-steward/objects`

## 6. 浏览器自测结果

使用本地浏览器登录 `platform.admin` 后检查：

- Fresh login 进入 `/data-steward/assets`，首屏可见“项目启动台 / 推荐项目 / 进入项目 / 文件管理 / 项目可视化 / 项目列表”。
- 资产总览首屏未再出现 `FLOW`、`ALERT` 大块教程。
- `/data-steward/assets/503` 和 `/data-steward/assets/506` 首屏核心卡片均为：项目可视化、文件管理、交付状态。
- 工作台顶部不再直接展示 `平台内部ID`、`NAS_REAL_PILOT`、`ACTIVE`。
- “更多入口”默认折叠，低频按钮不再铺满主视图。
- 1280 / 1440 / 1920 宽度下检查资产总览、503 工作台、506 工作台，未发现横向溢出。

## 7. 自测命令结果

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

## 8. 边界确认

- 是否修改 `backend/**`：否。
- 是否修改数据库迁移：否。
- 是否修改 `docs/**`：否。
- 是否新增或修改后端接口：否。
- 是否改变权限规则：否。
- 是否新增 Hermes 能力：否。
- 是否新增 BIM 轻量化能力：否。
- 是否新增真实 NAS 写能力：否。
- 是否读取文件正文：否。
- 是否删除旧路由：否。
- 是否为 105 / 503 / 93 / 506 写死逻辑：否。

## 9. P0 / P1 / P2

- P0：无。
- P1：无。
- P2：
  - 前端构建仍有历史 Vite chunk size warning，不影响本轮功能。
  - 工作区仍存在非本轮交付的 `.claude/**`、`CLAUDE.md`、`tmp/**` 和部分 handoff 状态文件，建议提交时继续排除或由主 agent 单独处理。

## 10. 是否建议进入 UX3 测试验收

建议进入 UX3 测试 agent 验收。

建议重点验收：

1. Fresh login 后员工是否能在 5 秒内理解“先选项目”。
2. 资产总览首屏是否聚焦项目搜索、推荐项目、文件管理、项目可视化和项目列表。
3. 503 / 506 项目工作台首屏是否最突出文件管理、项目可视化、交付状态。
4. 技术信息和低频入口是否不再抢主视图。
5. 旧链接、回归脚本和多分辨率布局是否稳定。

# 开发 Agent 最新报告：PLM-2 项目属性与经营台账 MVP

## 1. 本轮目标

本轮执行 `PLM-2：项目属性与经营台账 MVP`。

目标是在现有项目生命周期、项目权限和项目工作台基础上，补齐项目级经营台账，让项目管理员维护预算、合同金额、已回款、回款状态、计划交付时间和经营备注，并在项目启动台 / 项目工作台展示关键摘要。

本轮没有做完整财务系统，没有做合同附件、发票、收款流水或利润计算。

## 2. 改动文件列表

本轮 PLM-2 相关改动：

- `backend/delivery-app/src/main/resources/db/migration/V35__plm2_project_business_profile.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/project/application/ProjectBusinessProfileApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/project/controller/ProjectBusinessProfileController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AgentApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-plm2-project-business-profile.sh`
- `handoff/dev-agent/latest-report.md`

未修改：

- `docs/**`
- Hermes / Agent 能力
- BIM / 葛兰岱尔能力
- 对象化后台队列执行逻辑
- NAS 文件真实增删改查逻辑
- 交付标准、审核整改、交付包核心逻辑

## 3. 数据库迁移

新增 Flyway 迁移：

- `V35__plm2_project_business_profile.sql`

新增表：

- `core_project_business_profiles`

字段覆盖：

- `project_id`
- `budget_amount`
- `contract_amount`
- `received_amount`
- `payment_status`
- `expected_payment_date`
- `planned_start_date`
- `planned_delivery_date`
- `actual_delivery_date`
- `currency_code`
- `business_remark`
- `created_by / updated_by / created_at / updated_at / deleted`

回款比例没有落库，由后端按 `received_amount / contract_amount` 动态计算。

## 4. 新增接口

新增项目经营台账接口：

- `GET /api/data-steward/projects/{projectId}/business-profile`
- `PUT /api/data-steward/projects/{projectId}/business-profile`
- `GET /api/data-steward/projects/{projectId}/members-summary`

同时增强项目启动台已有接口：

- `GET /api/data-steward/assets/projects`

该接口现在在项目响应中返回：

- `businessProfile`
- `membersSummary`

用于启动台展示少量关键指标。

## 5. 权限规则

后端规则：

- 有项目访问权限的用户可以读取经营台账。
- `PROJECT_ADMIN` 可以编辑自己项目的经营台账。
- 超级管理员 `admin` 可以读取 / 编辑所有项目经营台账。
- `DELIVERY_ENGINEER` / `PROJECT_VIEWER` 只读。
- 未授权项目读取 / 更新均拒绝。
- 更新项目属性写入 `core_audit_logs`，action 为 `project.business-profile.update`。

校验规则：

- 金额不能为负。
- 已回款金额不能大于合同金额。
- 回款状态限定为 `UNSET / NOT_STARTED / PARTIAL / COMPLETED / OVERDUE`。
- 币种默认 `CNY`。

## 6. 前端展示与编辑入口

项目启动台：

- 推荐项目卡片展示：
  - 计划交付时间
  - 回款进度
- 项目列表展示：
  - 计划交付
  - 回款进度
  - 合同金额摘要
  - 项目成员数

项目工作台：

- 概览页新增 `项目属性 / 经营信息` 卡片。
- 展示合同金额、预算金额、已回款 / 回款比例、计划交付、预计回款、成员摘要、经营备注。
- 项目管理员 / 超级管理员可点击 `编辑项目属性`。
- 编辑使用弹窗，不新增一级页面。
- 普通成员只读，前端不显示可执行编辑入口；后端仍做最终权限校验。

## 7. 项目成员摘要

本批没有新增员工关系表。

成员摘要复用现有：

- `core_user_project_roles`
- `core_users`
- `core_roles`

统计：

- 成员总数
- 项目管理员数量
- 交付工程师数量
- 查看者数量

## 8. 自测结果

- 后端构建：通过
  - `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 后端健康检查：通过
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - 返回 `{"status":"UP"}`
- PLM-2 专项脚本：通过
  - `bash scripts/dev/check-plm2-project-business-profile.sh`
  - `PASS=12 FAIL=0`
- PLM-1 项目生命周期回归：通过
  - `bash scripts/dev/check-plm1-project-lifecycle.sh`
  - `PASS=11 FAIL=0`
- M3G-8 对象优先读取回归：通过
  - `bash scripts/dev/check-m3g8-object-first-read-fallback.sh`
  - `PASS=7 FAIL=0`
- Phase2 file-access 安全回归：通过
  - `bash scripts/dev/check-phase2-batch4-file-access.sh http://localhost:8080 admin 123456 503 506`
  - `PASS=18 FAIL=0`
- `git diff --check`：通过
- `git diff --cached --check`：通过

## 9. 安全边界

本轮保持：

- 未触碰真实 NAS 文件。
- 未移动、删除、重命名、覆盖任何 NAS 原文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未新增 parser / indexing / 向量库能力。
- 未写 Hermes memory、OpenSearch、Qdrant 或 documents/chunks。
- 响应未包含 raw NAS path、bucket、object key、`storage_uri`、SQL、raw row、token、secret。

PLM-2 专项脚本已做禁出字段扫描。

## 10. 服务状态

由于新增后端迁移和接口，本轮已重启后端服务并确认健康：

- 后端：`127.0.0.1:8080`，健康检查通过。
- 前端：`127.0.0.1:5173`，保持运行，未关闭。

## 11. 未完成事项与合并前提醒

- 本轮只做项目经营台账 MVP，不做完整财务系统。
- 项目成员只做摘要展示，不做成员列表、添加 / 移除、权限变更历史。
- 当前工作区仍存在多批历史未提交 / 未跟踪改动，包括对象化、葛兰岱尔、全局搜索和 handoff 草稿。主 agent 合并 PLM-2 前需要按文件范围隔离确认，不要把非 PLM-2 历史脏改动混入本批提交。

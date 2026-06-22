# Dev Agent 当前任务：PLM-2 项目属性与经营台账 MVP

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

先阅读并遵守：

- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`

不要修改 `docs/**`。

## 0. 本批目标

用户要求：每个项目都应该能维护不同属性值，例如预算、交付时间、项目员工、是否回款、回款进度等，方便后续做项目可视化。这些值由项目管理员填写，后续再优化员工权限设置界面。

本批名称：

`PLM-2：项目属性与经营台账 MVP`

一句话目标：

> 在现有项目生命周期、员工权限和项目工作台基础上，补齐项目经营信息台账，让项目管理员能维护预算、合同、交付时间、回款进度，并让项目启动台 / 项目工作台能展示这些关键指标。

## 1. 批次合并规则

本项目后续每个批次必须保持小步可回滚：

1. 本批只做 PLM-2，不夹带 UX、Hermes、BIM、对象化队列或无关重构。
2. 开发完成后写清报告，不长期停在支线。
3. 测试通过、主 agent 收口后，应由主 agent 将本批清晰提交并合并回 `main`。
4. 不允许把非本批历史脏改动混进 PLM-2 提交。

## 2. 范围边界

允许修改：

- `backend/**` 中与项目属性、项目台账、权限校验、审计相关的最小代码。
- `frontend/**` 中项目启动台、项目工作台、项目详情 / 项目属性编辑相关页面。
- `scripts/dev/**` 新增 PLM-2 专项脚本。
- `handoff/dev-agent/latest-report.md`。

不要修改：

- `docs/**`
- Hermes / Agent 能力
- BIM / 葛兰岱尔能力
- 对象化后台队列执行逻辑
- NAS 文件真实增删改查逻辑
- 交付标准、审核整改、交付包核心逻辑

## 3. 后端建议

### A. 新增项目经营属性表

不要把所有经营字段都塞进 `core_projects` 主表。建议新增迁移：

`V35__plm2_project_business_profile.sql`

建议表名：

`core_project_business_profiles`

建议字段：

- `id`
- `project_id`，唯一，关联 `core_projects.id`
- `budget_amount`，预算金额
- `contract_amount`，合同金额
- `received_amount`，已回款金额
- `payment_status`，回款状态：`UNSET / NOT_STARTED / PARTIAL / COMPLETED / OVERDUE`
- `expected_payment_date`，预计回款时间
- `planned_start_date`，计划开始时间
- `planned_delivery_date`，计划交付时间
- `actual_delivery_date`，实际交付时间，可为空
- `currency_code`，默认 `CNY`
- `business_remark`，经营备注
- `created_by / updated_by / created_at / updated_at / deleted`

说明：

- 回款比例可由 `received_amount / contract_amount` 计算，不一定落库。
- 项目员工继续复用现有 `core_user_project_roles`，本批不要新建员工关系表。

### B. 新增 / 复用接口

建议新增：

```text
GET /api/data-steward/projects/{projectId}/business-profile
PUT /api/data-steward/projects/{projectId}/business-profile
GET /api/data-steward/projects/{projectId}/members-summary
```

最小返回：

- 项目基础信息：projectId、projectCode、projectName
- 经营信息：预算、合同、已回款、回款比例、回款状态、预计回款、计划交付、实际交付、备注
- 成员摘要：成员数量、项目管理员数量、交付工程师数量、查看者数量

### C. 权限规则

- 有项目访问权限的用户可以读取项目属性。
- 当前项目 `PROJECT_ADMIN` 可以编辑当前项目属性。
- 超级管理员 `admin` 可以编辑所有项目属性。
- `DELIVERY_ENGINEER / VIEWER` 只读。
- 未授权项目必须拒绝。
- 更新项目属性必须写审计日志。

## 4. 前端建议

### A. 项目启动台

在项目卡片或项目列表中展示少量关键字段：

- 计划交付时间
- 回款进度
- 合同金额 / 预算金额，择一或折叠展示
- 项目成员数量

不要让项目列表变成财务明细表。默认只展示最关键的 2-3 个指标。

### B. 项目工作台

在项目工作台增加一个清晰的 `项目属性 / 经营信息` 入口或折叠区：

- 当前预算
- 合同金额
- 已回款 / 回款比例
- 计划交付时间
- 项目成员摘要
- `编辑项目属性` 按钮

编辑入口仅项目管理员 / 超级管理员可用。

### C. 项目属性编辑

使用弹窗或抽屉，不要新增复杂一级页面。

字段：

- 预算金额
- 合同金额
- 已回款金额
- 回款状态
- 预计回款日期
- 计划开始日期
- 计划交付日期
- 实际交付日期
- 经营备注

表单要做基本校验：

- 金额不能为负。
- 已回款金额不能大于合同金额；如确实允许超付，先不做。
- 日期格式合法。

### D. 员工权限界面

本批只补项目成员摘要，不重构员工权限界面。

后续另开批次优化：

- 项目成员列表。
- 项目成员添加 / 移除。
- 用户按项目授权视图。
- 权限变更历史。

## 5. 禁止事项

本批严禁：

- 不要做完整财务系统。
- 不要做合同附件管理。
- 不要做发票 / 收款流水。
- 不要自动计算利润。
- 不要修改真实 NAS 文件。
- 不要改对象化队列。
- 不要引入 Hermes / BIM / parser / indexing。
- 不要把技术字段、SQL、raw row、真实 NAS 路径、bucket、object key 暴露给前端。

## 6. 专项脚本

新增：

`scripts/dev/check-plm2-project-business-profile.sh`

至少验证：

1. 管理员登录。
2. 获取一个真实项目。
3. `GET business-profile` 返回默认 profile。
4. 项目管理员可更新预算、合同、回款、交付时间。
5. `DELIVERY_ENGINEER / VIEWER` 更新被拒绝。
6. 未授权项目读取 / 更新被拒绝。
7. 回款比例计算正确。
8. 已回款大于合同金额时被拒绝。
9. 更新后项目启动台 / 项目工作台 API 字段可见。
10. 响应不包含 raw path、bucket、object key、`storage_uri`、SQL、token、secret。

## 7. 必跑验证

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-plm2-project-business-profile.sh
bash scripts/dev/check-plm1-project-lifecycle.sh
bash scripts/dev/check-m3g8-object-first-read-fallback.sh
bash scripts/dev/check-phase2-batch4-file-access.sh http://localhost:8080 admin 123456 503 506
git diff --check
git diff --cached --check
```

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告写清：

1. 改了哪些文件。
2. 新增表 / 字段 / 接口。
3. 权限规则如何实现。
4. 前端展示与编辑入口在哪里。
5. 项目成员摘要如何复用现有权限关系。
6. 必跑验证结果。
7. 是否有未完成项。
8. 是否存在需要主 agent 合并回 `main` 前特别确认的文件。

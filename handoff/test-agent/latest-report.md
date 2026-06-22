# PLM-2 项目属性与经营台账 MVP 验收报告

测试时间：2026-06-22

## 1. 结论

PLM-2 功能验收通过。

本轮验证 `PLM-2：项目属性与经营台账 MVP` 已实现：

- 项目经营台账可读取与维护，覆盖预算、合同、已回款、回款状态、预计回款、计划开始、计划交付、实际交付和备注。
- 超级管理员 / 项目管理员可编辑，`DELIVERY_ENGINEER` / `PROJECT_VIEWER` 只读。
- 未授权用户读取和更新均被拒绝。
- 回款比例由后端计算，已回款金额大于合同金额会被拒绝。
- 项目启动台和项目工作台已展示关键经营摘要。
- 本批未扩成完整财务系统，未触碰真实 NAS 文件，未引入 Hermes / BIM / parser / indexing 新能力。

建议主 agent 判定 PLM-2 功能收口；但提交 / 合并前必须按文件范围隔离当前工作区，避免把非 PLM-2 历史改动混入本批提交。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 前端构建仍有既有 Vite chunk size warning。
- 查看者账号头像区域显示名出现中文乱码：`Ä äºŒæœŸ... phase2.viewer`。这是测试账号显示名编码/历史数据表现问题，不影响 PLM-2 经营台账权限和功能。
- 当前工作区存在较多非 PLM-2 未暂存 / 未跟踪 / 历史暂存改动。PLM-2 关键文件已纳入 Git，但主 agent 合并前必须隔离提交范围。

## 3. 接口验收结果

专项脚本：

- `bash scripts/dev/check-plm2-project-business-profile.sh`：通过，`PASS=12 FAIL=0`。

覆盖结果：

- `GET /api/data-steward/projects/{projectId}/business-profile` 可返回默认 / 当前项目属性。
- `PUT /api/data-steward/projects/{projectId}/business-profile` 可更新经营信息。
- 回款比例计算正确。
- 已回款金额大于合同金额被拒绝。
- `DELIVERY_ENGINEER / PROJECT_VIEWER` 可读取但不可更新。
- 未授权用户读取 / 更新均被拒绝。
- 更新经营信息已写入审计日志。
- 项目启动台 API 已返回 `businessProfile` 和 `membersSummary`。
- 项目工作台成员摘要接口可见。

手动 API 抽查：

- `GET /api/data-steward/projects/503/business-profile`：返回 `OK`，含 `budgetAmount`、`contractAmount`、`receivedAmount`、`paymentProgressPercent`、`paymentStatus`、`membersSummary`。
- `GET /api/data-steward/projects/506/business-profile`：返回 `OK`，含同样字段。
- 响应带 `traceId`。
- 抽查响应未发现禁出字段。

说明：字段名为 `paymentProgressPercent`，不是 `paymentRatio`；语义符合“回款比例 / 回款进度”。

## 4. 页面轻验结果

管理员视角：

- 打开 `http://127.0.0.1:5173/data-steward/assets`：页面不白屏，无横向溢出。
- 项目启动台展示计划交付、回款进度、成员等关键经营摘要。
- 项目启动台未堆完整财务明细。
- 打开 `http://127.0.0.1:5173/data-steward/assets/503`：页面不白屏，无横向溢出。
- 项目工作台可见 `项目属性 / 经营信息` 卡片。
- 可见合同金额、预算、回款进度、计划交付、预计回款、项目成员、经营备注。
- 超级管理员可见 `编辑项目属性` 入口。

普通查看者视角：

- 使用 `phase2.viewer / Viewer@123` 打开 `http://127.0.0.1:5173/data-steward/assets/503`。
- 页面不白屏，无横向溢出。
- 可见经营信息摘要。
- 显示 `只读` 状态。
- 不显示 `编辑项目属性` 入口。

边界文案：

- 页面出现 `合同附件`、`发票`、`收款流水` 字样，但上下文是“这里仅记录项目级台账信息，不管理合同附件、发票或收款流水”，属于边界说明，不是新增完整财务系统入口。

## 5. 权限与安全结果

权限：

- 超级管理员可编辑项目经营台账。
- 项目管理员可编辑自己管理项目。
- `DELIVERY_ENGINEER / PROJECT_VIEWER` 更新被拒绝。
- 未授权项目读取 / 更新被拒绝。

安全边界：

- 未修改真实 NAS 文件。
- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未修改对象化后台队列策略。
- 未引入 Hermes / BIM / parser / indexing。
- 未发现合同附件、发票、利润核算、收款流水等完整财务系统能力。
- 未大改员工权限界面。

禁出字段：

- API 抽查和页面轻验未发现 `/Volumes`、`smb://`、`nas://`、`storage_uri`、`storageUri`、bucket、object key、raw row、SQL、token、secret 泄露。

## 6. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过。
- `corepack pnpm --dir frontend build`：通过，仅既有 Vite chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-plm2-project-business-profile.sh`：通过，`PASS=12 FAIL=0`。
- `bash scripts/dev/check-plm1-project-lifecycle.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3g8-object-first-read-fallback.sh`：通过，`PASS=7 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh http://localhost:8080 admin 123456 503 506`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。
- `git diff --cached --check`：通过。

## 7. Git 范围检查

PLM-2 关键文件已纳入 Git 跟踪：

- `backend/delivery-app/src/main/resources/db/migration/V35__plm2_project_business_profile.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/project/application/ProjectBusinessProfileApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/project/controller/ProjectBusinessProfileController.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-plm2-project-business-profile.sh`

收口前必须注意：

- 当前暂存区仍包含 M3X-Q、8C-GD、对象化队列等非 PLM-2 历史文件。
- 当前未暂存区包含 Glandar / 全局搜索 / handoff 等非 PLM-2 改动。
- 当前未跟踪区包含 `backend/.../search/**`、`frontend/src/modules/core/api/search.ts`、`scripts/dev/check-gs1-global-search.sh` 等非 PLM-2 文件。
- 本轮不把这些非 PLM-2 内容判为功能失败，但不建议直接把完整当前工作区作为 PLM-2 提交合并。

## 8. 是否建议主 agent 收口并合并回 main

建议主 agent 收口 PLM-2 功能。

合并建议：

- 可以进入 PLM-2 收口判断。
- 合并回 `main` 前，请先按 PLM-2 文件范围做干净提交。
- 不要把当前工作区里非 PLM-2 的历史脏改动、未跟踪搜索/Glandar 脚本或 handoff 草稿混入 PLM-2 提交。

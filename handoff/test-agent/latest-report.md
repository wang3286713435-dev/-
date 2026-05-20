# M1E 文件管理连续工作体验与后台任务可追踪性验收报告

生成时间：2026-05-21 00:22 CST

## 1. 测试结论

结论：通过。

本轮只验收 `M1E：文件管理连续工作体验与后台任务可追踪性`，未进入 G4 / Hermes / 8B / 8C / 9A，未做生产部署，未触发真实 NAS 写操作、正文解析、BIM 构件解析、parser / writer / indexing 或 Hermes 自动治理。

当前未发现 P0 / P1 / P2。建议主 agent 进入 M1E 收口判断。

## 2. 当前分支与范围

- 当前目录：`/Users/vc/Documents/数字化交付平台`
- 当前分支：`codex/platform-m1e-file-task-continuity`
- 当前提交：`d2d8006`
- 工作区状态：存在开发 agent 本轮未提交改动和临时文件；测试 agent 未修改业务代码，未修改 `docs/**`，仅更新本测试报告。
- 已确认 `handoff/main-agent/status.md` 当前主线为 M1E，G4 / Hermes / 8B / 8C / 9A 仍冻结。

## 3. 必读文件检查

已阅读并按要求执行：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1d-standard-delivery-loop-closure.md`
- `handoff/main-agent/m1e-file-task-continuity-plan.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

开发报告中声明的 `<promise>MAINLINE_M1E_FILE_TASK_CONTINUITY_COMPLETE</promise>` 已在本轮构建、脚本、接口和浏览器验收中复核。

## 4. 构建、健康检查与脚本结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M1E 专项脚本：`bash scripts/dev/check-m1e-file-task-continuity.sh`，通过，`PASS=10 FAIL=0`。
- 文件访问回归：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`。
- 数据管家模块回归：`bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh`，通过，`PASS=16 FAIL=0`。
- M1C 回归：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- M1D 回归：`bash scripts/dev/check-m1d-standard-delivery-loop.sh`，通过，`PASS=29 FAIL=0`。
- checksum 任务可见性脚本：`bash scripts/dev/check-checksum-job-visibility.sh`，通过，包含 `success job visible`、`success job completed`、`checksum written back`、`failure reason sanitized`、`checksum job visibility ok`。
- 空白字符检查：`git diff --check`，通过。

## 5. Fresh Login 与资产入口

结果：通过。

- 已清理浏览器会话后从 `/login` 使用 `platform.admin / Admin@123` 登录。
- 登录后进入 `/data-steward/assets`。
- 资产总览显示真实 NAS 项目入口和 M1B 项目资产说明。
- 当前统计可见：真实项目数 16、已登记文件 40,935、模型文件 2,604、图纸文件 38,331、登记容量 296 GB。
- 页面无白屏、无 500、无页面级横向溢出。

## 6. 503 / 506 文件管理连续状态验收

结果：通过。

已重点打开：

- `/data-steward/assets/503?tab=files`
- `/data-steward/assets/506?tab=files`

确认结果：

- 两个项目均能正常打开文件管理，不出现 403 / 404 / 500。
- 页面显示文件管理状态连续提示、`重置视图`、checksum 后台任务区、平台内部项目 ID。
- 文件行和详情中可见 `平台文件ID`，普通用户可以把任务、文件和页面记录对应起来。
- 右侧文件表仍保留文件名、类型、扩展名、版本、大小、更新时间、操作等关键字段。
- 文件行 `更多` 菜单保留 `预览/下载`、`详情`、`治理`、`补 checksum`，未恢复单独重复的 `受控访问`。
- 文件详情可打开，详情中展示平台文件 ID、文件名、扩展名、大小、更新时间和受控访问说明。
- 页面无整体横向撑爆。

## 7. 查询状态恢复与重置验收

结果：通过。

503 文件管理页已验证：

- 目录选择和关键字查询会写入 URL 查询参数。
- 从文件管理切到其他页签再回到文件管理，已选择目录和关键字能够恢复。
- 直接访问带查询参数的 URL 时，`fileKeyword`、`fileKind`、`discipline`、`fileExt`、`qualityIssue`、`filePage`、`filePageSize` 等状态会被保留。
- `重置视图` 会清空文件管理筛选条件并回到 `?tab=files`。
- 未发现刷新后状态错乱、页面白屏或过滤条件异常丢失。

说明：在部分组合筛选条件下，因为当前样本没有匹配文件，页面正文不会显示对应筛选值；但 URL 状态和恢复逻辑仍保留完整查询条件。

## 8. checksum 后台任务可追踪性验收

结果：通过。

页面验收：

- 文件管理页可见 `checksum 后台任务` 区域。
- 任务列表展示后台任务编号、对应文件、平台文件 ID、状态、进度、创建时间、更新时间。
- 成功任务可查看详情。
- 人为制造的受控失败任务 `#2041` 在 503 文件管理页可见，状态为 `已失败`，进度为 `0%`，失败原因显示为 `文件不存在，底层路径已隐藏`，并提供 `查看` 和 `重试` 操作。
- 失败原因未暴露真实底层路径。

接口验收：

- `POST /api/data-steward/assets/checksum-jobs`：可创建 checksum 任务，响应统一结构包含 `traceId`。
- `GET /api/data-steward/assets/jobs?projectId=...&jobType=CHECKSUM_CALC`：可按项目查看任务列表。
- `GET /api/data-steward/assets/jobs/{jobId}`：可查看任务详情。
- `POST /api/data-steward/assets/jobs/{jobId}:retry`：失败任务可受控重试。
- 管理员创建的受控失败任务 `#2040` 返回 `status=FAILED`、`failureReason=文件不存在，底层路径已隐藏`、`requestPayload={"sanitized":true,"reason":"path_not_exposable"}`。
- 第二用户 `delivery.engineer / Engineer@123` 读取无权任务详情返回 403，错误码 `ASSET_JOB_ACCESS_DENIED`。
- 第二用户查询无权项目任务列表返回 403，错误码 `ASSET_PROJECT_ACCESS_DENIED`。

测试夹具清理：

- 本轮创建的 `M1E-API-*` 和 `M1E-UIFAIL-*` 文件资源已清理，当前可见文件资源计数为 0。
- 对应任务历史作为任务/审计记录保留，属于预期行为。

## 9. 安全边界与敏感字段检查

结果：通过。

浏览器页面、接口响应和脚本输出中未发现以下泄露：

- 真实 NAS 绝对路径
- `storage_path`
- `storage_uri`
- `storageUri`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`

本轮未发现：

- 真实 NAS 文件创建、移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- BIM 构件级解析。
- parser / writer / indexing。
- Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks 写入。
- Hermes / G4 自动审批、自动整改、自动挂接或自动删除。
- 把 catalog metadata 当正文 evidence。

## 10. P0 / P1 / P2 问题清单

- P0：无。
- P1：无。
- P2：无。

非阻塞观察：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。
- 工作区存在开发 agent 本轮未提交文件和 `tmp/**` 临时文件；本轮测试未回退、未修改这些业务改动。

## 11. 收口建议

建议主 agent 判定 M1E 收口。

M1E 已满足：

- 文件管理页可恢复目录、筛选、分页等连续工作状态。
- 用户可通过重置入口回到项目默认视图。
- 文件详情和任务区都能明确展示平台文件 ID。
- checksum 任务可见、可追踪、可查看失败原因、可受控重试。
- 无权用户不能读取无权项目任务或任务详情。
- 失败原因已脱敏，不暴露真实底层路径。
- 未突破 G4 / Hermes / 8B / 8C / 9A 冻结边界。

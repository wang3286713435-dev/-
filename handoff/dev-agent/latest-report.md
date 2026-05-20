# 开发 Agent 报告：M1E 文件管理连续工作体验与后台任务可追踪性收口

时间：2026-05-20

## 1. 本轮目标

本轮按 `handoff/dev-agent/current-prompt.md` 执行 `M1E：文件管理连续工作体验与后台任务可追踪性收口`。

目标：

- 文件管理按项目记住目录、筛选、分页、展开目录和最近文件。
- 文件 ID、项目内部 ID、checksum 任务 ID 改成业务可理解文案。
- checksum 后台任务在项目文件管理页可见、可理解、可重试。
- 任务详情、失败原因、文件详情继续脱敏，不暴露真实 NAS 路径。

完成承诺：

`<promise>MAINLINE_M1E_FILE_TASK_CONTINUITY_COMPLETE</promise>`

## 2. Git 与路线边界

- 当前分支：`codex/platform-m1e-file-task-continuity`
- 当前基线提交：`d2d8006`
- 当前 active 批次：确认是 `M1E`
- G4 / Hermes / 8B / 8C / 9A：确认冻结，未扩展
- 未修改 `docs/**`
- 未新增数据库迁移
- 未读取 PDF / Office / DWG / RVT / IFC 正文
- 未做 BIM 轻量化、构件解析、selective indexing
- 未触碰真实 NAS 增删改查
- 未写 Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks
- 未创建子 agent，未调用 Claude Code

## 3. 读取材料

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1d-standard-delivery-loop-closure.md`
- `handoff/main-agent/m1e-file-task-continuity-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

## 4. 文件管理状态记忆

实现方式：

- URL query 保存可恢复关键状态：
  - `tab=files`
  - `fileDir`
  - `fileKeyword`
  - `fileKind`
  - `discipline`
  - `fileExt`
  - `qualityIssue`
  - `filePage`
  - `filePageSize`
  - `lastFileId`
- localStorage 按项目保存完整最近状态：
  - 当前目录
  - 搜索关键词
  - 文件类型、专业、扩展名、质量问题
  - 页码、页大小
  - 展开的目录
  - 最近打开的文件 ID 和文件名

前端新增：

- 文件管理状态条：说明当前恢复的目录、筛选、页码和最近文件。
- `打开最近文件` 入口。
- `重置视图` 入口，清空 query 和 localStorage 中该项目的文件管理状态。
- 目录树展开状态受控化，活动目录的父级会自动展开。

## 5. ID 文案业务化

- 文件表格从 `文件ID` 改为 `平台文件ID`。
- 文件详情抽屉展示 `平台文件ID`、文件名、项目编码 / 名称、`项目平台内部ID`。
- 项目工作台顶部如展示项目数字 ID，标注为 `平台内部ID`。
- checksum 任务统一展示为 `后台任务编号 #xxx`。
- checksum 任务始终绑定文件名和 `平台文件ID` 展示。

## 6. checksum 任务可追踪

项目文件管理页新增轻量任务区域：

- 展示最近 checksum 后台任务。
- 展示后台任务编号、对应文件、平台文件 ID、状态、进度、失败原因、更新时间。
- 失败任务可直接重试。
- 点击“查看”打开任务详情弹窗。

任务详情弹窗补齐：

- 后台任务编号。
- 对应文件名。
- 平台文件 ID。
- 状态。
- 进度。
- 创建时间、更新时间、开始时间、完成时间。
- 脱敏失败原因。
- 失败任务重试入口。

后端补齐：

- 新增 `AssetJobResponseSanitizer`。
- `JobApplicationService` 的任务列表和任务详情返回脱敏结果。
- `ChecksumApplicationService` 创建 checksum 任务后返回脱敏任务。
- `AgentApplicationService` 的任务列表、详情和触发 checksum 返回也复用脱敏结果。
- DB 内部任务 payload 仍供 worker 使用，API response 不返回 raw storage path。

## 7. 路径脱敏

前端：

- 文件详情不再展示 `logicalPath` 原文；改为 `path_hint` 文案。
- 存储路径继续显示为“底层路径已隐藏，请使用平台受控预览或下载入口”。
- 任务失败原因通过 `safePathText` 脱敏。

后端：

- 任务 `requestPayload` 返回为脱敏占位：
  - `{"sanitized":true,"reason":"path_not_exposable"}`
- 任务 `failureReason` 如包含底层路径，返回：
  - `文件不存在，底层路径已隐藏`
  - 或等价脱敏说明

已验证 response 与前端可见文本未发现：

- raw NAS path
- `/Volumes`
- `/Users`
- `/tmp`
- `nas://`
- `smb://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- raw row
- SQL
- token / secret / password

## 8. 105/503 与 93/506 抽查

浏览器抽查：

- `/data-steward/assets/503?tab=files`
  - checksum 后台任务区可见。
  - 文件管理状态条可见。
  - 搜索关键词写入 URL query。
  - 离开文件管理再回来可恢复关键词。
  - 点击目录树目录后，`fileDir` 写入 URL query。
  - 离开后再回到文件管理，可恢复目录上下文。
  - 重置视图可清除关键词和 query。
- `/data-steward/assets/506?tab=files`
  - checksum 后台任务区可见。
  - 文件管理状态条可见。
  - 页面可见文本未发现路径泄露。

写链路使用隔离测试文件资源验证，没有对真实项目执行 NAS 写操作。

## 9. 修改文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AgentApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/ChecksumApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/JobApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetJobResponseSanitizer.java`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreePanel.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreeNodeItem.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-m1e-file-task-continuity.sh`
- `scripts/dev/check-checksum-job-visibility.sh`
- `handoff/dev-agent/latest-report.md`

注意：

- `handoff/dev-agent/real-nas-100-employee-flow.md` 是本轮开始前已存在的未跟踪文件，不属于 M1E 代码改动。
- `tmp/**` 下未跟踪文件为本机运行态残留，本轮未纳入交付。

## 10. 自测结果

- 后端构建：通过
  - `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过
  - `corepack pnpm --dir frontend build`
  - 仅有既存 Vite chunk size warning
- 后端健康检查：通过
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - 返回 `{"status":"UP"}`
- M1E 专项脚本：通过
  - `bash scripts/dev/check-m1e-file-task-continuity.sh`
  - `PASS=10 FAIL=0`
- Batch4 文件访问回归：通过
  - `PASS=18 FAIL=0`
- Batch5B 数据管家模块回归：通过
  - `PASS=16 FAIL=0`
- M1C 真实项目主数据回归：通过
  - `PASS=14 FAIL=0`
- M1D 标准驱动交付回归：通过
  - `PASS=29 FAIL=0`
- checksum 任务可见性脚本：通过
  - 已更新为失败原因脱敏口径
- 脚本语法检查：通过
  - `bash -n scripts/dev/check-m1e-file-task-continuity.sh scripts/dev/check-checksum-job-visibility.sh`
- 浏览器抽查：通过
  - `browser-m1e-ok`
  - `browser-m1e-directory-ok`
- `git diff --check`：通过

当前本地服务：

- 后端：`127.0.0.1:8080`
- 前端：`127.0.0.1:5173`，screen 会话 `frontend-m1e`

## 11. P0 / P1 / P2

P0：

- 暂无。

P1：

- 暂无。

P2：

- 前端构建仍有既存 chunk size warning，本轮未处理。
- 文件管理视图模式仍是现有表格视图；本轮只收口连续工作状态和任务追踪，没有扩展图标视图或多视图切换。

## 12. 是否建议进入 M1E 测试验收

建议进入测试 agent 验收。

本轮开发侧自测已通过，但不自行宣布 M1E 最终收口；最终是否收口由主 agent 和测试 agent 判断。

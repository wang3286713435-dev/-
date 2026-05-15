# 主 Agent 开发监控日志

## 2026-05-13：一期文件预览安全外壳完成

- 用户确认继续向后开发，但要求不要拓展太多功能，先完整一期主线。
- 本轮按一期边界补齐“文件预览状态与入口”，没有进入二期真实文件预览、模型轻量化、正文抽取、向量库、自动治理或自动审批。
- 后端新增 `GET /api/data-steward/assets/files/{fileId}/preview`，复用现有文件资产权限，返回预览元数据，不返回真实 `storagePath`。
- 预览状态规则：
  - PDF/图片：`AVAILABLE / BROWSER_NATIVE / NOT_REQUIRED`。
  - Office：`NEEDS_CONVERSION / OFFICE_CONVERSION / NOT_STARTED`。
  - CAD：`NEEDS_CONVERSION / CAD_CONVERSION / NOT_STARTED`。
  - BIM/模型：`NEEDS_CONVERSION / BIM_LIGHTWEIGHT / NOT_STARTED`。
  - 归档包/未知格式：`UNSUPPORTED`。
  - 缺路径、已删除或隔离：`BLOCKED`。
- 前端 `/data-steward/assets/:projectId` 新增文件表格“预览”入口、文件详情抽屉“预览能力”区和“文件预览状态”弹窗。
- 新增专项脚本 `scripts/dev/check-file-preview-shell.sh`，覆盖 PDF 和 RVT 两类边界，并断言预览接口不暴露 `storagePath`。
- 独立验证结果：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-file-preview-shell.sh` 通过。
  - `git diff --check` 通过。
  - Headless Chrome 冒烟通过：`/data-steward/assets/2` 可打开，预览按钮和弹窗可用，弹窗明确“不读取文件正文”，页面宽度 `1440/1440/1440` 未横向撑爆。
- 本轮报告已写入 `handoff/dev-agent/latest-report.md`。

## 2026-05-13：一期试运行短回归 Prompt 与验收清单更新

- 用户询问“一期主线剩余可用性/验收完整度”含义后，主 agent 已明确该范围不是继续扩二期功能，而是把一期内部试点做到可用、可验收、可交接。
- 用户确认后，主 agent 将测试 agent 当前任务更新为 `一期内部试运行可用性与验收完整度短回归`。
- 新测试 prompt 已写入 `handoff/test-agent/current-prompt.md`，覆盖：
  - 真实 NAS 项目与文件基线。
  - 资产总览、项目详情、扫描任务、数据质量、非标准资料治理页面。
  - 文件预览安全外壳。
  - 权限与路径安全。
  - 企业 agent 稳定读模型准备状态。
  - 必跑脚本：`check-scan-task-control.sh`、`check-asset-quality-overview.sh`、`check-agent-db2-contract.sh`、`check-file-preview-shell.sh`。
- 同步更新 `handoff/main-agent/phase1-internal-trial-acceptance-checklist.md`，补入文件预览安全外壳验收项和阻塞失败标准。
- 当前裁决不变：不做真实 BIM 轻量化、不做 Office/CAD 转换、不读取文件正文、不写向量库、不自动治理或审批。

## 2026-05-13：一期短回归 P1/P2 修复

- 已读取测试 agent 最新报告：短回归无 P0，但存在 1 个 P1 和 1 个 P2。
- P1：`check-agent-db2-contract.sh` 因 `hermes_agent_ro` 密码口径不一致失败。
  - 已重新生成本机 dev 只读密码。
  - 已同步更新 MySQL `hermes_agent_ro` 密码和 macOS 钥匙串。
  - 密码未写入仓库或报告。
  - 已复跑 `scripts/dev/check-agent-db2-contract.sh`，结果 `agent db2 contract ok`。
- P2：首页项目下拉残留 `SCANCTRL-*` 测试项目。
  - 已将残留 `SCANCTRL-*` 项目归档软删除，并删除其用户可见授权。
  - 已修复 `scripts/dev/check-scan-task-control.sh`，增加退出清理逻辑，后续复跑不会再次污染首页项目下拉。
  - 已复跑 `scripts/dev/check-scan-task-control.sh`，结果 `scan task control regression passed`。
  - 管理员 `/api/core/users/me` 当前项目数为 `18`，`SCANCTRL-*` 可见数量为 `0`。
- 其他回归：
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `check-asset-quality-overview.sh` 通过。
  - `check-file-preview-shell.sh` 通过。
  - `git diff --check` 通过。
- 修复报告：`handoff/main-agent/phase1-short-regression-remediation-report.md`。

## 2026-05-13：一期内部试运行可用性验收收口

- 测试 agent 已完成极短复验，并在 `handoff/test-agent/latest-report.md` 中确认：
  - `check-agent-db2-contract.sh` 已通过。
  - 首页项目下拉为 `18` 个项目，且无 `SCANCTRL-*`。
  - `check-scan-task-control.sh` 复跑后不再留下可见 `SCANCTRL-*` 测试项目。
  - 当前未发现新的 P0/P1/P2。
- 主 agent 裁决：一期内部试运行可用性与验收完整度正式收口。
- 已更新 `handoff/main-agent/status.md`，将当前状态标记为一期内部试运行验收通过。
- 已新增最终收口报告：`handoff/main-agent/phase1-internal-trial-final-closure.md`。
- 下一步进入“用户试运行 + 只修 P0/P1”或“二期后续批次规划”二选一；在没有新指令前，不继续扩一期后端治理，不进入真实 BIM 轻量化，不做正文抽取、向量库、Agent 自动治理或自动审批。

## 2026-05-12：二期批次一规划固化

- 用户确认可以开始规划二期，但要求批次一只做：
  - 前端资产目录。
  - REST 权限证明。
  - 只读 catalog API。
  - Agent preview / audit-ready 页面。
- 用户明确禁止本批涉入：
  - Agent 直接增删改数据库。
  - Agent 自动移动、删除、修复 NAS 文件。
  - Agent 自动把 BIM 或文件正文写入向量库。
  - Agent 自动下结论或自动审批。
  - 多 Agent 调度真实业务动作。
  - 面向客户的生产级权限体系承诺。
- 主 agent 已完成规划文档：`handoff/main-agent/phase2-batch1-readonly-catalog-plan.md`。
- 已同步更新：
  - `handoff/main-agent/status.md`
  - `handoff/main-agent/decisions.md`
- 本轮未启动开发 agent，未修改业务代码，未改数据库迁移，未触碰真实 NAS 文件。

## 2026-05-12：二期批次一开发与测试 Prompt 派发

- 用户确认进入下一步。
- 主 agent 已将二期批次一规划转写为：
  - `handoff/dev-agent/current-prompt.md`
  - `handoff/test-agent/current-prompt.md`
- 开发 prompt 已明确要求：
  - 使用 Claude Code Ralph skill 拆 story。
  - 使用 Ralph Loop 执行。
  - 完成承诺为 `<promise>PHASE2_BATCH1_READONLY_CATALOG_COMPLETE</promise>`。
  - 只做前端资产目录、REST 权限证明、只读 catalog API、Agent preview / audit-ready 页面。
  - 不做 Agent 写库、NAS 文件移动/删除/修复、正文抽取、向量化、自动审批、模型轻量化或构件级解析。
- 测试 prompt 已明确要求：
  - 验证 OpenAPI、专项脚本、前端页面、权限边界、路径泄漏、数据不变性和禁止边界。
  - 发现越权、数据变更、敏感信息泄漏、页面白屏或 OpenAPI 缺失时按 P0 阻塞。
- 本次仅更新交接 prompt 和日志，业务代码尚未由主 agent 直接修改。

## 2026-05-08：Claude Code 与 Ralph Loop 连通性检查

- 工作目录：`/Users/vc/Documents/数字化交付平台`
- Claude Code CLI：`/Users/vc/.local/bin/claude`
- Claude Code 版本：`2.1.133`
- 普通非交互探针通过：Claude 可在项目目录回复 `CLAUDE_CONNECTED`。
- 项目 `/skills` 中可见 `ralph` skill；该 skill 主要用于将 PRD/任务转换为 Ralph 可执行故事。
- 已安装项目级插件：`ralph-loop@claude-plugins-official`。
- 插件配置写入：`.claude/settings.json`。
- 交互会话中已确认 `/ralph-loop` 自动补全可见：
  - `/ralph-loop:help`
  - `/ralph-loop:ralph-loop`
  - `/ralph-loop:cancel-ralph`
- 普通 Claude 会话执行 `/ralph-loop:ralph-loop` 时会触发 setup 脚本权限审批，无法满足无人值守开发。
- 使用 `claude --permission-mode bypassPermissions` 启动后，Ralph Loop 连通性测试通过。
- 连通性测试完成后 `.claude/ralph-loop.local.md` 已自动清理，说明完成承诺检测生效。

## 后续开发监控规则

- 主 agent 通过 PTY 终端启动 Claude Code 开发 agent，并持续读取输出。
- 正式开发首选启动方式：

```text
claude --permission-mode bypassPermissions
/ralph-loop:ralph-loop "<批次 prompt>" --completion-promise "<PROMISE>" --max-iterations <N>
```

- 批次 1 完成承诺固定为：

```text
<promise>BATCH1_DEV_COMPLETE</promise>
```

- 只有在开发 agent 写入 `handoff/dev-agent/latest-report.md`，且构建、脚本、OpenAPI、权限、审计、SQL View 和范围检查全部满足后，才允许接受完成承诺。
- 若发现偏离主线，主 agent 立即中断 Claude Code 会话，必要时执行 `/ralph-loop:cancel-ralph` 或删除 `.claude/ralph-loop.local.md`，修订 prompt 后重新启动。

## 2026-05-08：批次 1 开发 Agent 启动

- 启动方式：`script -qF <log> claude --permission-mode bypassPermissions`
- Ralph 命令：`/ralph-loop:ralph-loop ... --completion-promise BATCH1_DEV_COMPLETE --max-iterations 12`
- 旁路日志：`handoff/main-agent/claude-logs/dev-agent-batch1-20260508-125353.log`
- 最新日志快捷入口：`handoff/main-agent/claude-logs/latest-dev-agent.log`
- 用户本地监听命令：

```bash
tail -f /Users/vc/Documents/数字化交付平台/handoff/main-agent/claude-logs/latest-dev-agent.log
```

- 当前状态：Ralph Loop 第 1 次迭代已启动，开发 agent 正在读取文档与探查现有后端代码。

## 2026-05-08：批次 1 第一次主 Agent 审计中断

- 开发 agent 已新增 V8/V9 迁移、资产仓储、服务层、REST Controller 和批次 1 验收脚本草稿。
- 主 agent 在脚本验收前审计发现 P0 风险：Controller 直接暴露部分仓储查询，路径映射、扫描任务和待审核队列存在绕过用户项目授权的可能。
- 同时发现正式资产入库方法命名和参数可能导致 `.dwg/.dxf/.pdf` 默认分类不能稳定写入 `DRAWING`，以及扫描递归能力需要明确落地。
- 已通过 PTY 中断 Claude Code，并要求继续 Ralph Loop，优先修复权限边界、正式资产分类、递归扫描，再执行构建和验收。
- 完成承诺仍保持 `<promise>BATCH1_DEV_COMPLETE</promise>`，未通过上述审计前不得接受完成。

## 2026-05-08：批次 1 第二次主 Agent 审计中断

- 开发 agent 修复了 Controller 直连仓储、正式资产 `fileKind` 写入、`fileId + userId` 文件详情查询和递归扫描，并完成 `delivery-data-steward` 与 `delivery-app` 构建。
- 主 agent 在报告前复查发现剩余 P0：`runScan` 仍使用全量路径映射，待审核候选的列表、修改、审批、驳回仍未按当前用户项目范围过滤。
- 已再次中断 Claude Code，要求补齐 `accessibleProjectIds`、`requireProjectAccess`、`canAccessProject` 三个 helper，并收紧扫描运行、候选列表和审批入库权限。
- 明确要求修完前不得写 `handoff/dev-agent/latest-report.md`，不得输出 `<promise>BATCH1_DEV_COMPLETE</promise>`。

## 2026-05-08：批次 1 第三次主 Agent 审计中断

- 开发 agent 完成了项目权限收紧、正式资产列表/详情 API、扫描与待审核权限过滤，并写入初版 `handoff/dev-agent/latest-report.md`。
- 主 agent 独立复查发现剩余 P0：V8 Flyway 在 MySQL 下存在索引长度超限和 `recursive` 保留字问题，导致全新库迁移不可用。
- 同时发现导入链路权限缺口：项目 CSV 导入后未给当前用户授权，路径映射导入在 `requireProjectAccess` 前未授权，导致导入项目不可见或路径映射失败。
- 已要求开发 agent 继续 Ralph Loop，只追加/修正当前批次必要实现，不改旧迁移，不扩前端，不进入批次 2/3。

## 2026-05-08：批次 1 第四次主 Agent 审计中断

- 开发 agent 修复 V8 索引为 `nas_path_hash` 计算列唯一约束，并对 `recursive` 列做 MySQL 反引号处理。
- 开发 agent 修复 `createProject`、项目导入和路径映射导入后的项目授权，以及 `upsertProject` 对 null 覆盖非空字段的问题。
- 主 agent 继续按 `handoff/dev-agent/current-prompt.md` 审计，发现批次 1 明确要求 `CSV/xlsx` 导入和 `multipart/form-data`，但实现只覆盖 text/csv。
- 已写入 `handoff/main-agent/audit-blocker-batch1-xlsx.md` 并要求开发 agent 补齐 xlsx 多部件上传、脚本验收和报告。

## 2026-05-08：批次 1 主 Agent 最终审计通过

- Claude Code 通过项目目录下 Ralph Loop 插件运行，旁路监听日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 开发 agent 输出 `<promise>BATCH1_DEV_COMPLETE</promise>` 前，已完成项目资产、NAS 路径映射、CSV/xlsx 导入、扫描任务、待审核队列、正式资产库、SQL View 和 OpenAPI 注册。
- 主 agent 独立复跑后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 主 agent 独立检查后端健康：`/actuator/health` 返回 `{"status":"UP"}`。
- 主 agent 独立确认 Flyway V8/V9 已成功应用，`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 在 MySQL 中存在。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1.sh`，覆盖创建项目、CSV/xlsx 导入、NAS 扫描、自动入库、待审核、人工审核、文件分类、审计、OpenAPI，最终输出 `bim asset batch1 ok`。
- 清理了开发过程中误落在 `backend/` 目录下的临时 xlsx 文件：`code,name`、`code,name,industryType,projectStage`、`projectCode,projectName,nasPath`。
- 结论：批次 1 可接受，下一步应准备 `批次 2：异步任务、checksum、统计与事件流` 的开发 agent prompt。

## 2026-05-08：批次 1 测试报告 P0/P1/P2 修复

- 已阅读 `handoff/test-agent/latest-report.md`，确认测试 agent 发现的阻塞项为：全局扫描任务越权可见、未归属待审核候选可被第二用户接管、自动入库缺少 `confidenceLevel`、OpenAPI 扫描任务参数名不一致。
- 修复扫描任务权限：项目型扫描按项目权限可见；全局扫描只允许创建人查看列表、详情和执行。`ScanTaskResponse` 增加 `createdBy`、`updatedAt` 用于接口可观测和权限判断。
- 修复待审核候选权限：新增统一候选访问判断。已归属项目候选按项目权限；未归属项目候选只允许原扫描任务创建人查看、修改、审批、驳回或批量审批。
- 修复正式文件置信度：`insertFileAsset` 增加 `confidenceLevel` 入参，自动入库写 `HIGH`，人工审核入库透传候选置信度。
- 修复 OpenAPI 合同：扫描详情和执行接口路径参数从 `{taskId}` 统一为 `{scanTaskId}`。
- 回归脚本 `scripts/dev/check-bim-asset-batch1.sh` 新增第二用户权限断言：不能看到管理员全局扫描任务，不能读取详情，不能看到未归属候选，不能 patch/approve；新增自动入库文件 `confidenceLevel` 非空断言；新增 `{scanTaskId}` OpenAPI 精确路径断言。
- 验证结果：`./mvnw -pl delivery-app -am -DskipTests package` 通过，后端重启后 `/actuator/health` 返回 `{"status":"UP"}`，增强后的 `check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。

## 2026-05-08：批次 1 复验通过并切入批次 2

- 已阅读测试 agent 最新复验报告，确认 `P0-1` 扫描任务越权、`P0-2` 待审核候选越权、`P1` `confidenceLevel`、`P2` OpenAPI 路径参数名均已通过复验。
- 复验报告结论为：构建通过、健康检查通过、增强脚本通过、当前无 P0，建议批次 1 通过并进入批次 2。
- 用户已明确批准“验证后，可以继续进入批次 2”。
- 主 agent 将此前补充的 Batch 1.1/1.2 真实 NAS 适配增强调整为后续增强候选，不作为批次 2 前置条件。
- 下一步派发 `批次 2：异步任务、checksum、统计与事件流` 的开发 agent prompt，并同步测试 agent 验收 prompt。

## 2026-05-08：用户改判，先收口批次 1 尾巴

- 用户最新指示为：“先完善批次 1 的尾巴，先不急进入批次 2”。
- 主 agent 暂停批次 2 prompt 派发，恢复批次 1 后置增强优先级。
- 批次 1 尾巴当前定义为：真实 NAS 目录发现、扩展文件分类、低价值文件治理、重扫幂等、历史 `confidence_level` 补齐和对应脚本验收。
- 已发现代码中存在 `/api/data-steward/assets/nas-projects:discover` 雏形，后续开发 agent 应先审计复用，不重复造新接口。

## 2026-05-08：批次 1 尾巴主 Agent 审计通过

- Claude Code 按 Ralph Loop 执行批次 1 尾巴开发，主 agent 在审计中要求补齐短数字项目编码匹配风险，防止 `98` 被普通路径子串误命中。
- 已将扫描阶段项目编码匹配从 `contains` 改为路径段/token 精确匹配：仅接受路径段等于项目编码，或以 `编码 + 分隔符(-/_/.)` 开头。
- `scripts/dev/check-bim-asset-batch1-tail.sh` 已增加确定性回归：模拟 NAS 根路径包含 `98`，未映射根目录下的 `stray.rvt` 必须进入 LOW/PENDING 待审核，不能自动入库。
- 主 agent 独立复核 `git diff --check` 通过；未发现批次 2/3 范围误入后端代码。
- 主 agent 独立构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
- 首次 `start-backend.sh` 因旧 Java 进程占用 8080 失败，确认是端口冲突而非代码问题；停止旧进程后重新启动成功，`/actuator/health` 返回 `{"status":"UP"}`。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1.sh`，输出 `bim asset batch1 ok`。
- 主 agent 独立复跑 `scripts/dev/check-bim-asset-batch1-tail.sh`，输出 `bim asset batch1 tail ok`。
- 结论：批次 1 尾巴可接受；按用户要求暂不进入批次 2，等待明确开工指令。

## 2026-05-08：批次 1 尾巴测试 Agent 复验通过，进入批次 2

- 测试 agent 已写回 `handoff/test-agent/latest-report.md`，结论为批次 1 尾巴通过，当前无 P0，建议进入批次 2。
- 复验覆盖构建、后端启动、健康检查、`check-bim-asset-batch1.sh`、`check-bim-asset-batch1-tail.sh`、静态检查、低价值文件、临时目录降级、短数字项目编码 token 匹配和 OpenAPI。
- 用户已明确“继续下一步”，主 agent 将当前阶段切入 `批次 2：异步任务、checksum、统计与事件流`。
- 已更新 `handoff/dev-agent/current-prompt.md`：要求 Claude Code 使用 Ralph Loop，完成数据库任务表、应用内 worker、checksum 异步补齐、容量统计 API、事件流 API、OpenAPI 和 `check-bim-asset-batch2.sh`。
- 已更新 `handoff/test-agent/current-prompt.md`：锁定批次 2 验收范围，并要求复跑批次 1 与批次 1 尾巴，防止回归。
- 批次 2 明确禁止提前实现批次 3：不做企业 agent API Key、不做删除审批、不做隔离恢复、不做真实物理删除。

## 2026-05-08：批次 2 开发 Agent 交付与主 Agent 审计通过

- Claude Code 已按 Ralph Loop 完成批次 2 开发，交付报告写入 `handoff/dev-agent/latest-report.md`。
- 主 agent 审计中发现并要求修复的关键问题：
  - 全局事件只允许原 operator 可见，避免无项目事件被其他用户读取。
  - 容量统计必须真正按 `projectId` 过滤，`totalSizeBytes` 统计全部文件容量而不是只统计模型容量。
  - 任务列表按 `projectId` 查询时，`AssetJobRepository.listForUser` 必须追加 `AND project_id = :projectId`。
  - `AssetJobWorker` 所有失败路径必须记录 `JOB/job.fail`，不能只有 `checksum.fail`。
  - 脏数据环境下待审核队列默认 `ORDER BY c.id ASC LIMIT 200` 会让新候选不可见，已改为 `ORDER BY c.id DESC` 最新优先。
- 验收脚本 `scripts/dev/check-bim-asset-batch2.sh` 已增强：
  - 增加 `projectId` 过滤断言，A 项目查询不能返回 B 项目任务。
  - 增加失败 checksum 任务的 `JOB/job.fail` 事件流断言。
  - 继续带批次 1 与批次 1 尾巴回归。
- 主 agent 独立复跑结果：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。
  - `scripts/dev/check-bim-asset-batch1-tail.sh` 输出 `bim asset batch1 tail ok`。
  - `scripts/dev/check-bim-asset-batch2.sh` 输出 `bim asset batch2 ok`。
- 静态范围检查未发现 API Key、删除审批、隔离恢复或真实物理删除实现混入批次 2。
- 用户提醒后，主 agent 已将后续 Claude Code 正式开发模型策略调整为 `deepseek-pro`，并写入 `handoff/main-agent/status.md`。当前这轮已经运行中的开发会话不因模型名重启；下一轮开发 agent 启动时使用 `claude --permission-mode bypassPermissions --model deepseek-pro`。
- 当前建议：先交给测试 agent 复验批次 2；测试报告无 P0 后，再由主 agent 派发批次 3 prompt。

## 2026-05-09：确认进入批次 3

- 已读取测试 agent 最新报告：`批次 2 验收通过，当前无 P0，可等待主 agent/用户确认后进入批次 3`。
- 用户已明确确认进入批次 3。
- 已更新 `handoff/dev-agent/current-prompt.md`：本轮只做企业 agent、API Key、agent 只读/任务/申请能力、删除申请审批、逻辑删除、物理隔离、恢复和受控永久删除。
- 已更新 `handoff/test-agent/current-prompt.md`：锁定批次 3 验收范围，并要求复跑批次 1、批次 1 尾巴、批次 2，防止回归。
- 批次 3 安全红线：
  - API Key 不得保存明文。
  - agent key 不得复用普通 JWT 权限。
  - agent 不得直接修改正式资产、审批、隔离、恢复或永久删除。
  - 未审批物理删除不得移动或删除文件。
  - 逻辑删除不得触碰 NAS 文件。
  - 永久删除只能作用于隔离区文件，且必须满足隔离期。
  - 验收脚本只能移动/删除 `/tmp` 测试文件，不能触碰真实 NAS `/Volumes/zyzn/卓羽智能项目`。
- 后续 Claude Code 开发 agent 必须使用 Pro 模型和 Ralph Loop，完成承诺为 `<promise>BATCH3_DEV_COMPLETE</promise>`。
- 实际启动时 Claude CLI 拒绝 `deepseek-pro`，提示可用 Pro 型号为 `deepseek-v4-pro`；已按用户“切换为 pro”的意图改用 `deepseek-v4-pro`。
- 批次 3 开发 agent 已启动，旁路日志软链为 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 主 agent 监控时发现 Claude Code 触发 `Explore` 内部探索任务；已中断并补充开发 prompt，明确禁止 `Explore`、`Task`、`Subagent` 等独立上下文委派，要求所有阅读、实现、验证保留在主 Claude Code 会话。

## 2026-05-09：批次 3 主 Agent 审计打回

- Claude 开发 agent 完成批次 3 初版并写入 `handoff/dev-agent/latest-report.md`，专项脚本曾通过。
- 主 agent 复查发现 3 个必须修复的安全边界：
  - Agent NAS 扫描允许 `projectId=null` 或任意 `rootPath`，未绑定项目路径映射。
  - Agent 查询任务详情时对 `projectId=null` 的全局任务缺少访问校验。
  - Agent 发起的删除申请可由 API Key 所属用户审批，绕过自审批限制。
- 另发现 1 个 P1：Agent 标注未校验目标文件是否属于声明项目。
- 已重写 `handoff/dev-agent/current-prompt.md` 为“批次 3 主 Agent 审计修复”提示词，要求开发 agent 用 Ralph Loop 修复并补回归断言。

## 2026-05-09：批次 3 主 Agent 审计修复通过

- Claude Code 已按 Ralph Loop 和 `deepseek-v4-pro` 完成批次 3 P0/P1 安全修复，旁路日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 修复范围保持在批次 3 后端安全边界，没有扩前端、没有进入二期 BIM 能力：
  - Agent 触发 NAS 扫描必须提供 `projectId`，且 `rootPath` 必须落在该项目启用的 NAS 路径映射下。
  - Agent 查询任务详情时，项目任务按 API Key 授权项目隔离；全局任务按创建人隔离。
  - Agent 删除申请的 `requested_by` 改为记录 `apiKeyId`，审批时阻止 API Key 创建人自审批。
  - Agent 标注仅允许 `FILE_RESOURCE`，并校验目标文件真实存在且属于声明项目。
- 回归脚本 `scripts/dev/check-bim-asset-batch3.sh` 已补充 P0-1/P0-2/P0-3/P1 断言，并在脚本末尾自动回归批次 1、批次 1 尾巴和批次 2。
- 主 agent 独立复验：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch3.sh http://localhost:8080 platform.admin Admin@123 2` 通过，并输出 `bim asset batch3 ok`。
- 非阻塞记录：脚本中为了给 `delivery.engineer` 授权当前临时测试项目，使用了当前 Docker MySQL 的测试库直连写入；这是验收脚本准备动作，不属于业务逻辑。
- 结论：批次 3 已通过主 agent 审计，可交测试 agent 做独立复验。若测试 agent 无 P0，一期后端三批次可进入阶段总验收与企业 agent 对接清单确认。

## 2026-05-09：批次 3 测试 Agent P0 打回与修复

- 测试 agent 首轮批次 3 验收不通过，唯一 P0 为：普通项目用户 `delivery.engineer` 可成功创建 `ALL_PROJECTS` 类型 API Key。
- 主 agent 已通过 Claude Code 开发 agent 使用 Ralph Loop 和 `deepseek-v4-pro` 执行专项修复；旁路日志保留在 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 修复策略：
  - `ALL_PROJECTS` 创建不再直接放行。
  - 现阶段平台级管理员口径暂定为：用户必须在所有 `deleted=0 AND status='ACTIVE'` 的活动项目上都具备 `PROJECT_ADMIN` 角色。
  - 无活动项目时 fail closed。
  - 不按用户名硬编码。
  - 普通项目用户失败时返回 `AGENT_KEY_ALL_PROJECTS_FORBIDDEN` 和 403。
- 触碰范围：
  - `AgentApiKeyRepository.java` 新增 `hasProjectAdminRoleOnAllActiveProjects(Long userId)`。
  - `AgentApiKeyApplicationService.java` 在 `ALL_PROJECTS` 分支调用权限检查。
  - `scripts/dev/check-bim-asset-batch3.sh` 新增 step 4b：`delivery.engineer` 创建失败、`platform.admin` 创建成功、创建后立即撤销。
  - `handoff/dev-agent/latest-report.md` 已写入专项修复报告。
- 主 agent 独立复验：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `/actuator/health` 返回 `{"status":"UP"}`。
  - `scripts/dev/check-bim-asset-batch3.sh http://localhost:8080 platform.admin Admin@123 2` 通过，并输出 `bim asset batch3 ok`。
- 结论：该 P0 已修复并通过主 agent 复验；下一步交测试 agent 对 P0 修复做独立复验。测试 agent 原失败报告保留，不覆盖历史验收事实。

## 2026-05-09：批次 3 复验通过，一期后端数据治理收口

- 测试 agent 已再次完成批次 3 全量验收，并写入 `handoff/test-agent/latest-report.md`。
- 复验结论：通过，当前无 P0/P1/P2。
- 复验覆盖：
  - 后端 Maven 构建通过。
  - 后端重新启动和健康检查通过。
  - `check-bim-asset-batch1.sh` 输出 `bim asset batch1 ok`。
  - `check-bim-asset-batch1-tail.sh` 输出 `bim asset batch1 tail ok`。
  - `check-bim-asset-batch2.sh` 输出 `bim asset batch2 ok`。
  - `check-bim-asset-batch3.sh` 输出 `bim asset batch3 ok`。
  - `delivery.engineer` 创建 `ALL_PROJECTS` API Key 返回 403 和 `AGENT_KEY_ALL_PROJECTS_FORBIDDEN`。
  - `platform.admin` 仍可创建并立即撤销 `ALL_PROJECTS` API Key。
  - 静态检查确认修复基于“覆盖全部活跃项目的 PROJECT_ADMIN 权限”，不是用户名硬编码。
- 主 agent 裁决：一期后端数据治理正式收口。后续除缺陷修复外，不继续扩大一期后端范围；下一步应进入一期企业 agent 对接前确认、真实 NAS 小批量试点导入准备，或转入二期客户交付完整版规划。

## 2026-05-09：真实 NAS 小批量试点只读发现

- 用户提供真实 NAS 路径：`smb://192.168.1.181/zyzn/卓羽智能项目`，并明确只能读取、禁止修改删除 NAS 数据。
- 本机已通过 `/Volumes/zyzn/卓羽智能项目` 访问该目录；本轮只执行目录与文件元数据读取，没有写入平台正式资产库，也没有改动 NAS 文件。
- 一级目录共 `27` 个：
  - 高置信正式项目目录 `17` 个。
  - 重复编号冲突目录 `4` 个，涉及 `95` 与 `99`。
  - 非标准或需人工确认目录 `6` 个。
- 抽样统计发现：
  - `105-启航华居项目`：4 层内白名单文件 `35` 个，适合低风险冒烟。
  - `100-深圳市二十八高项目`：4 层内白名单文件 `123` 个，标准目录清晰，适合作为第一批正式小样本。
  - `101-C塔`：4 层内白名单文件 `217` 个，资料混合，适合作为复杂业务样本。
  - `98-深圳口岸项目`：4 层内白名单文件 `163` 个，但像多口岸项目集合，需先确认拆分口径。
  - `99-丰图既有建模项目`：4 层内白名单文件 `4,268` 个，且存在大量 `临时文件`、`新建文件夹`、`转换` 等路径，暂不建议第一批正式写库。
- 已从代码确认 `NAS 项目发现 dryRun=true` 不会创建项目、路径映射、导入任务、审计事件或事件流；可作为下一步安全预检。
- 已实际调用平台 `nas-projects:discover` 接口执行真实 NAS 根目录 dry-run：
  - 返回 `OK / success`。
  - `totalDirectories=27`。
  - `createdProjects=0`、`updatedProjects=0`、`createdMappings=0`、`existingMappings=0`。
  - 状态分布：`READY 17`、`CONFLICT 4`、`REFERENCE 3`、`NEEDS_CODE_REVIEW 3`。
  - 需人工复核总数 `10`，其中编号冲突为 `95` 与 `99` 两组。
- 报告已写入 `handoff/main-agent/real-nas-pilot-discovery-report.md`。
- 下一步建议：用 `105`、`100`、`101` 做第一批数据库元数据写入；`99` 暂不进入第一批正式写库，只作为压力 dry-run 与低置信审核策略样本。

## 2026-05-09：真实 NAS 第一批元数据写入

- 用户确认继续下一步后，主 agent 按“只写数据库元数据、不修改 NAS 文件”的边界执行第一批真实 NAS 项目扫描。
- 已完成 `105-启航华居项目`：
  - 项目 ID：`503`
  - 路径映射 ID：`681`
  - 扫描任务 ID：`414`
  - 扫描状态：`SUCCEEDED`
  - 扫描文件数：`4052`
  - 自动入库：`2927`
  - 待审核：`0`
  - 失败：`0`
- 已完成 `100-深圳市二十八高项目`：
  - 项目 ID：`504`
  - 路径映射 ID：`682`
  - 扫描任务 ID：`415`
  - 扫描状态：`SUCCEEDED`
  - 扫描文件数：`4634`
  - 自动入库：`3668`
  - 待审核：`293`
  - 失败：`0`
- `101-C塔` 暂停导入：
  - 当前开发库已有历史测试项目编码 `101`，项目 ID `99`，且包含大量 `/tmp` 测试路径和样板文件资产。
  - 为避免真实 NAS 数据污染旧测试项目，未执行真实 `101` 扫描。
  - 已取消误建扫描任务 `416 / REAL_NAS_101`，状态为 `CANCELED`。
  - 已删除误建真实 NAS 路径映射 `683`。
  - 已验证 `101` 下真实 NAS 文件资产写入数为 `0`。
- 验证结果：
  - 统计接口可返回 `100`、`105` 的文件数、模型数、图纸数和容量。
  - `ProjectAssetView`、`FileAssetView`、`AuditEventView` 可查询本轮导入结果。
  - `ModelAssetView` 当前基于 `data_model_integrations`，不会直接覆盖 NAS 扫描自动入库的模型文件；企业 agent 当前应使用 `FileAssetView WHERE file_kind='MODEL'` 检索一期 NAS 模型文件。该点已记录为后续开发优化项。
- 报告已写入 `handoff/main-agent/real-nas-pilot-import-report.md`。
- 下一步建议：先处理 `100` 的 `293` 条待审核候选，再扩大试点；暂不全量扫描 27 个一级目录。

## 2026-05-09：真实 NAS 待审核处理与 P1 修复

- 用户要求继续处理待审核候选，并修复 P1。
- 已处理 `100-深圳市二十八高项目` 扫描任务 `415` 的全部待审核候选：
  - 处理前：`293` 条待审核，其中图纸 `285`、模型 `8`。
  - 处理策略：全部审核通过，但保留 `LOW` 置信度和 `REVIEW` 来源，便于后续数据治理。
  - 处理后：待审核 `0`，审核通过 `293`。
  - `100` 当前正式资产：文件 `3961`，模型 `156`，图纸 `3805`，总容量 `41,863,763,688 bytes`。
- 已修复企业 agent 模型读模型 P1：
  - 新增迁移 `backend/delivery-app/src/main/resources/db/migration/V13__model_asset_view_include_nas_files.sql`。
  - 不修改旧迁移。
  - `ModelAssetView` 现在同时覆盖模型集成记录和 NAS 扫描自动/审核入库的 `file_kind='MODEL'` 文件。
- 验证：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端启动成功，Flyway 已应用 `version=13`。
  - `/actuator/health` 返回 `UP`。
  - `ModelAssetView` 可查 `100` 的 `156` 个模型：`rvt 144`、`nwc 8`、`nwd 4`。
  - `ModelAssetView` 可查 `105` 的 `198` 个模型。
- 已更新 `docs/08-acceptance-and-agent-integration.md` 与 `handoff/main-agent/enterprise-agent-db-mcp-integration-prompt.md`，明确 `ModelAssetView` 必须覆盖 NAS 扫描模型文件。
- 报告已写入 `handoff/main-agent/real-nas-review-and-p1-report.md`。

## 2026-05-09：真实 NAS 试点验收通过

- 用户明确企业 agent 协议尚未确定，先做真实试点验收。
- 后端启动成功，健康检查返回 `UP`。
- Flyway 当前版本为 `13`。
- 路径追溯验收：
  - `100` 抽查 `25` 条文件资产。
  - `105` 抽查 `25` 条文件资产。
  - 合计 `50` 条，NAS 路径存在、指向文件、大小一致均为 `50/50`。
- 检索与统计验收：
  - `100` 当前文件 `3961`、模型 `156`、图纸 `3805`、总容量 `41,863,763,688 bytes`。
  - `105` 当前文件 `2927`、模型 `198`、图纸 `2729`、总容量 `35,338,680,331 bytes`。
- SQL View 验收：
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均可查到真实试点数据。
  - `ModelAssetView` 可查 `100` 的 `156` 个模型和 `105` 的 `198` 个模型。
- 权限验收：
  - `platform.admin` 可查项目 `100` 和文件资产。
  - `delivery.engineer` 查询项目 `100` 返回 `0`，查询项目 `100` 文件资产返回 `0`。
- 审计与事件验收：
  - `100` 有自动入库审计 `3668`、审核通过审计 `293`、文件创建事件 `3961`。
  - `105` 有自动入库审计 `2927`、文件创建事件 `2927`。
- 暂停项复核：
  - `101` 扫描任务 `416` 为 `CANCELED`。
  - 真实 `101` 文件资产写入数为 `0`。
- 验收结论：真实 NAS 试点通过。报告已写入 `handoff/main-agent/real-nas-pilot-acceptance-report.md`。

## 2026-05-09：清理历史 101 测试数据并导入真实 101-C塔

- 用户确认可以删除平台内 `101` 历史测试数据并复用编码 `101`；主 agent 按“只改平台数据库元数据，不修改 NAS 文件”的边界执行。
- 清理前确认历史项目 ID `99` 是测试污染：
  - 原编码 `101`。
  - 活跃文件 `525`，总大小仅 `8,920 bytes`。
  - 活跃路径映射 `57`，主要指向 `/tmp/bim-nas-*`。
  - 真实 NAS 文件记录 `0`。
- 已将旧项目归档为 `TEST-101-ARCHIVED-99`，设置 `INACTIVE` 和 `deleted=1`。
- 已软删除旧项目关联测试文件、路径映射、扫描候选、扫描任务等活跃记录。
- 已通过正式接口创建真实项目：
  - 项目 ID：`505`
  - 项目编码：`101`
  - 项目名称：`C塔`
  - 资产来源：`NAS_REAL_PILOT`
- 已创建真实路径映射：
  - 路径映射 ID：`684`
  - NAS 路径：`/Volumes/zyzn/卓羽智能项目/101-C塔`
- 已完成真实扫描：
  - 扫描任务 ID：`417`
  - 扫描文件数：`5666`
  - 自动入库：`5457`
  - 待审核：`0`
  - 失败：`0`
  - 扫描耗时约 `7 分 24 秒`
- 入库结果：
  - 文件总数：`5457`
  - 模型：`114`
  - 图纸：`5343`
  - 总容量：`28,486,737,158 bytes`
- 验收：
  - 路径抽样 `30/30` 存在且大小一致。
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView` 均可查询真实 `101` 数据。
  - `delivery.engineer` 无法访问 `101` 项目和文件，权限隔离通过。
- 报告已写入 `handoff/main-agent/real-101-cleanup-and-import-report.md`。

## 2026-05-09：真实 NAS 第二批小样本导入

- 用户确认继续下一步，主 agent 按“扩大真实试点但不盲目全量”的策略执行。
- 本轮避开重复编号 `95/99`、可能需要拆分的 `98`、投标/参考/未知目录。
- 选择第二批低风险 READY 项目：
  - `93-中建八局国交酒店项目`
  - `96-深圳市前海蛇口自贸区医院科技大厦三期改造项目`
  - `104-佛山顺德妇幼医院项目`
- 已通过平台接口完成项目建档、路径映射、扫描和自动入库：
  - `93`：项目 ID `506`，路径映射 `685`，扫描任务 `418`，扫描 `6305`，自动入库 `5912`，待审核 `0`，失败 `0`。
  - `96`：项目 ID `507`，路径映射 `686`，扫描任务 `419`，扫描 `367`，自动入库 `243`，待审核 `0`，失败 `0`。
  - `104`：项目 ID `508`，路径映射 `687`，扫描任务 `420`，扫描 `1780`，自动入库 `1699`，待审核 `0`，失败 `0`。
- 入库资产：
  - `93`：文件 `5912`，模型 `588`，图纸 `5324`，容量 `77,923,356,233 bytes`。
  - `96`：文件 `243`，模型 `23`，图纸 `220`，容量 `3,039,265,913 bytes`。
  - `104`：文件 `1699`，模型 `187`，图纸 `1512`，容量 `30,194,811,835 bytes`。
- 验收：
  - 每项目抽样 `20` 条，共 `60` 条，路径存在且大小一致 `60/60`。
  - `ProjectAssetView`、`FileAssetView`、`ModelAssetView` 均可查询第二批真实项目数据。
  - `delivery.engineer` 查询第二批项目文件资产均返回 `0`，权限隔离通过。
  - 三个项目均有审计和事件流记录。
- 发现：当前同步扫描会先递归遍历全目录再过滤白名单，`93` 扫描耗时约 `3 分 25 秒`。后续应优先补强扫描任务进度、取消、续扫和低价值目录跳过。
- 报告已写入 `handoff/main-agent/real-nas-second-batch-import-report.md`。

## 2026-05-09：补强扫描任务进度、取消、续扫和报告

- 用户确认优先补强扫描任务，主 agent 在不修改真实 NAS 文件的前提下完成后端增强。
- 已追加 Flyway `V14__scan_task_control_and_progress.sql`，为扫描任务增加进度、取消标记、跳过统计、最后扫描路径、低价值目录跳过配置和扫描报告字段。
- 已增强扫描任务接口：
  - 创建扫描时可选择跳过低价值目录，并可传入额外跳过关键词。
  - 运行中的扫描可查询进度、当前处理数量、总量、百分比和最后扫描路径。
  - 运行中的扫描可取消。
  - 已取消或失败的扫描可续扫。
  - 续扫会跳过同一任务已生成的候选记录，避免重复入库。
  - 扫描完成、取消或失败后可查询扫描报告。
- 已新增专项验收脚本 `scripts/dev/check-scan-task-control.sh`，覆盖进度可见、取消、续扫、跳过低价值目录、跳过自定义目录、空文件不入库、扫描报告和正式资产去重。
- 验证结果：
  - 原生 Maven 后端构建通过。
  - 后端健康检查通过。
  - `check-scan-task-control.sh` 通过。
- 报告已写入 `handoff/main-agent/scan-task-control-upgrade-report.md`。

## 2026-05-09：最大真实 NAS 项目扫描控制测试

- 用户要求直接使用当前最大的真实 NAS 项目进行测试。
- 已确认当前真实试点中容量最大的项目为 `93-中建八局国交酒店项目`：
  - 项目 ID：`506`
  - NAS 路径：`/Volumes/zyzn/卓羽智能项目/93-中建八局国交酒店项目`
  - 正式资产文件数：`5912`
  - 正式资产总容量：`77,923,356,233 bytes`
- 已创建真实扫描任务 `423`，仅读取 NAS 文件元数据，不修改 NAS 原文件。
- 取消验证：
  - 扫描运行中可见进度。
  - 扫描到约 `1110` 个文件时成功取消。
  - 任务进入 `CANCELED`，最后扫描路径已记录。
- 续扫验证：
  - 对任务 `423` 执行续扫。
  - 最终状态 `SUCCEEDED`。
  - 进度 `6305/6305`，失败 `0`。
  - 低价值文件跳过 `15`，跳过目录 `1`。
  - 扫描报告可查询。
- 去重验证：
  - 续扫前文件数 `5912`，续扫后文件数 `5912`。
  - 续扫前总容量 `77,923,356,233 bytes`，续扫后总容量不变。
  - 确认续扫没有重复生成正式资产。
- 报告已写入 `handoff/main-agent/real-largest-nas-scan-control-report.md`。

## 2026-05-09：真实 NAS 第三批导入

- 用户要求先跳过 `95` 和 `99` 这类重复编号项目，导入其他未导入项目。
- 本轮选择编号清晰、非参考/投标目录的 `10` 个项目：
  - `97-水务利源既有建筑`
  - `108-福城南产业片区11-20-02宗地`
  - `109-华润三九银湖科创中心项目`
  - `110-龙华区观湖街道观城城市更新单元第一期10地块`
  - `111-蛇口影剧院项目`
  - `112-歌剧院项目`
  - `113-宝安洪桥头项目`
  - `114-香港项目`
  - `115-深圳宝安国际机场T2航站区及配套设施工程航站区工程施工总承包2标段`
  - `116-港中文（深圳）医学院智能化`
- 本轮暂缓：
  - `98-深圳口岸项目`：可能需要拆分口径，且库里已有小体量历史测试记录。
  - `95`、`99`：重复编号，需后续人工确认唯一编码。
  - 投标、参考、未知命名目录：不进入正式资产库。
- 导入结果：
  - 新增真实项目 `10` 个。
  - 扫描任务 `424-433` 全部 `SUCCEEDED`。
  - 新增正式文件资产 `20736`。
  - 模型 `1338`，图纸 `19398`。
  - 总容量 `101,002,506,275 bytes`。
  - 扫描失败 `0`，待审核 `0`。
- 验收：
  - 每项目抽样 `10` 条，共 `100` 条，路径存在且大小一致 `100/100`。
  - `ProjectAssetView`、`ModelAssetView` 可查询新项目数据。
  - `platform.admin` 可查；`delivery.engineer` 对 10 个新项目均不可见，权限隔离通过。
- 报告已写入 `handoff/main-agent/real-nas-third-batch-import-report.md`。

## 2026-05-09：一期真实资产接管总览

- 用户确认下一步先做资产总览和暂缓清单。
- 已生成业务可读报告 `handoff/main-agent/phase1-real-asset-overview.md`。
- 当前真实资产接管总览：
  - 已接管真实项目 `16` 个。
  - 正式登记文件 `40,935` 个。
  - 模型 `2,604` 个。
  - 图纸 `38,331` 个。
  - 总容量 `317,849,121,433 bytes`，约 `296.02 GiB`。
- 报告中已固化暂缓目录：
  - `98-深圳口岸项目`
  - 两个 `95-*`
  - 两个 `99-*`
  - `2024.9.3-大水灌`
  - `深城交`
  - `清华斯维尔围标项目`
  - 投标和参考资料目录
- 下一步建议：业务负责人核对 16 个项目编码/名称；企业 agent 团队确认读取方式、事件同步方式和非标准资料治理流程。

## 2026-05-09：资产总览与项目资产详情前端

- 用户确认企业 agent 暂缓，先做平台其他功能；本轮落地“资产总览”和“项目资产详情”。
- 前端新增：
  - `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
  - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- 前端能力：
  - 默认只展示 `NAS_REAL*` 来源的真实 NAS 接管项目，避免批次验收测试项目污染业务视图。
  - 资产总览展示真实项目数、文件数、模型数、图纸数、容量和最近更新时间。
  - 项目详情展示项目级统计、文件资产列表、扫描任务、路径映射。
  - 文件资产支持按文件名/路径、类型、专业、扩展名筛选，并可复制登记路径。
- 后端菜单新增“数据管家 / 资产总览”入口，路由为 `/data-steward/assets`。
- 验证：
  - `corepack pnpm build` 通过。
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端健康检查通过。
  - 管理员菜单返回“资产总览”和 `/data-steward/assets`。
  - 真实 NAS 口径聚合结果：`16` 个项目、`40,935` 个文件、`2,604` 个模型、`38,331` 个图纸、`317,849,121,433 bytes`。

## 2026-05-09：一期后端资产查询增强

- 用户要求继续严格按一期边界开发，不进入二期能力；本轮只增强一期后端资产治理查询能力。
- 新增/增强接口：
  - `GET /api/data-steward/assets/projects?assetSource=NAS_REAL*`：项目资产支持来源过滤。
  - `GET /api/data-steward/assets/statistics?assetSource=NAS_REAL*`：容量统计支持来源过滤。
  - `GET /api/data-steward/assets/files:page`：文件资产分页查询，返回 `items/pageNo/pageSize/total`。
- 查询增强：
  - 文件资产分页支持项目、文件类型、专业、文件名、扩展名、来源类型、关键字和资产来源过滤。
  - 扩展名过滤兼容 `.rvt` 和 `rvt` 两种写法。
  - 关键字检索覆盖项目编码、项目名称、文件名和登记路径。
- 前端资产总览已改为直接调用后端 `assetSource=NAS_REAL*` 口径，不再在前端聚合真实 NAS 统计。
- 验证：
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - `corepack pnpm build` 通过。
  - 后端健康检查通过。
  - 真实 NAS 来源过滤返回 `16` 个项目。
  - 真实 NAS 来源统计返回 `40,935` 文件、`2,604` 模型、`38,331` 图纸、`317,849,121,433 bytes`。
  - `files:page` 使用 `projectId=504&fileKind=MODEL&fileExt=.rvt&pageNo=1&pageSize=5` 返回 `total=144`、`items=5`。

## 2026-05-09：企业 Agent DB-2 握手文档补齐

- 用户要求在下一步开发前补齐 `handoff/main-agent/enterprise-agent-db2-coupling-handoff.md` 中主 agent 信息，重点是 DSN、企业 Agent 只读账号、样本读取和脱敏策略。
- 本机 dev 已创建 MySQL 只读账号 `hermes_agent_ro`。
- 授权范围仅限：
  - `ProjectAssetView`
  - `FileAssetView`
  - `ModelAssetView`
  - `AuditEventView`
- 验证结果：
  - `hermes_agent_ro` 可执行 `SELECT DATABASE()`。
  - `hermes_agent_ro` 可查询四个稳定 View。
  - `hermes_agent_ro` 查询 `core_projects` 被拒绝，确认不能读取业务底表。
- 文档中已明确：
  - 本机 dev DSN。
  - 本机 dev 只读账号；密码通过安全渠道交付，不写入交接文档。
  - 本机 dev 允许内部授权 `LIMIT 30` 样例读取。
  - 样例会暴露真实项目名、文件名和 NAS 路径。
  - shared-dev / staging / 客户环境默认 `STRUCTURE_ONLY`；未完成平台/运维 DSN 执行单、只读账号安全交付和业务负责人、数据负责人书面确认前，不允许真实样例读取。
  - shared-dev / staging 后置执行单：`handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md`。
  - Hermes mirror 层在四个 View 缺少稳定权限字段时必须默认 `DENIED`，真实项目名、文件名和 NAS 路径不得外发或写入外部云服务、向量库、搜索库、长期 memory 或外部观测日志。

## 2026-05-11：企业 Agent 本机同环境联调口径更新

- 用户确认：企业 Agent 后续会更新到本地 Hermes，并与数字化交付平台数据库运行在同一台本机环境中。
- 主 agent 已将 DB-2 对接口径调整为：
  - 本机 `delivery-mysql` + `hermes_agent_ro` 是一期 DB-2 首轮正式联调路径。
  - 当前不再要求立即获取 shared-dev / staging 账号。
  - shared-dev / staging 改为后置触发项：多人远程协作、持续测试、演示或客户交付前类生产验证时再由平台/运维开通。
  - 企业 Agent 仍必须使用专用只读账号，只能读四个稳定 View，不得读业务底表、不得写数据库、不得写 NAS。
  - 交接文档中的明文数据库密码已改为占位符，密码需通过安全渠道单独交付。

## 2026-05-11：本机只读账号轮换与扫描任务运维页启动

- 已轮换本机 MySQL 只读账号 `hermes_agent_ro` 密码。
- 新密码未写入仓库文档，已保存到 macOS 钥匙串：
  - `service: delivery-platform-hermes-agent-ro-local-dev`
  - `account: hermes_agent_ro`
- 权限复核结果：
  - `hermes_agent_ro` 可读取 `ProjectAssetView`。
  - `hermes_agent_ro` 读取 `core_projects` 被拒绝，仍不能读取业务底表。
- 已启动一期扫描任务运维页开发，保持在一期范围内：只做任务列表、进度、创建、运行、取消、续扫和报告查看，不做二期模型预览或构件解析。

## 2026-05-11：扫描任务运维页完成

- 新增前端页面：
  - `frontend/src/modules/data-steward/pages/AssetScanOperationsPage.vue`
  - 路由：`/data-steward/scans`
  - 菜单：数据管家 / 扫描任务
- 新增/补齐前端 API：
  - 创建扫描任务
  - 运行扫描任务
  - 取消扫描任务
  - 续扫扫描任务
  - 查看扫描报告
- 页面能力：
  - 按项目、状态、关键字查看扫描任务。
  - 查看进度、扫描数、自动入库数、待审核数、失败数、跳过目录数、最后扫描路径和失败原因。
  - 创建任务时只登记任务，不自动运行，避免误扫真实 NAS。
  - 运行、取消、续扫均需要人工点击确认。
  - 报告抽屉可查看任务汇总和 `scanReportJson`。
- 修正项目资产详情页扫描状态标签，使 `SUCCEEDED` 正确显示为成功。
- 验证：
  - `corepack pnpm build` 通过。
  - `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 后端健康检查通过。
  - 菜单接口已返回 `data-steward-scans:/data-steward/scans`。
  - 扫描任务接口返回现有任务。
  - `scripts/dev/check-scan-task-control.sh` 通过。
- 未做：
  - 未新增二期 BIM 预览、构件解析或模型轻量化能力。
  - 未修改 NAS 文件。

## 2026-05-12：二期批次一只读资产目录与 Agent 预览层开发审计

- 用户确认开始规划二期，但二期批次一只允许做：前端资产目录、REST 权限证明、只读 catalog API、Agent preview / audit-ready 页面。
- 主 agent 已调用 Claude Code 开发 agent，使用 `deepseek-v4-pro` 和 Ralph Loop 执行开发，开发日志旁路写入 `handoff/main-agent/claude-logs/latest-dev-agent.log`。
- 开发 agent 已完成：
  - 只读 Catalog API：项目、目录、文件列表、文件详情、审计上下文。
  - REST 权限证明：单文件权限证明与批量权限检查。
  - 前端资产目录：`/data-steward/catalog`。
  - Agent 预览页：`/data-steward/agent-preview`。
- 主 agent 审计发现并要求修复一个权限边界缺陷：
  - 问题：无权限普通用户访问真实 NAS 文件的 `audit-context` 曾返回 `500 CORE_INTERNAL_ERROR`。
  - 修复：`CatalogApplicationService.getFileAuditContext()` 改为无结果返回空，Controller 转为业务 `404 FILE_NOT_FOUND`，避免内部异常泄漏。
  - 回归保护：`scripts/dev/check-phase2-batch1-readonly-catalog.sh` 新增无权限 `audit-context` 不得返回 500 的断言。
- 主 agent 独立复核结果：
  - 专项脚本 `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`20/20 PASS`。
  - 真实 NAS 样本手工验证：管理员访问 `audit-context` 返回 `200`，普通用户返回 `404`，不再返回 `500`。
  - 后端构建：`./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建：`corepack pnpm build` 通过。
  - 后端健康检查：`UP`。
- 开发 agent 报告：`handoff/dev-agent/latest-report.md`。
- Ralph 进度记录：`.claude/ralph/progress.txt`。
- 主 agent 判断：二期批次一开发侧可进入测试 agent 验收；在测试 agent 通过前，不进入 selective indexing、正文抽取、Agent workflow 或受控写操作。

## 2026-05-12：二期批次一 P1 尾巴修复

- 测试 agent 反馈：本轮无 P0，但前端资产目录页缺少“按目录浏览 + 版本筛选”的完整页面交互，判定 P1，暂不建议收口。
- 主 agent 已按最小范围修复：
  - 后端 `GET /api/data-steward/catalog/files` 新增 `directoryPath` 与 `version` 查询参数。
  - 前端 `/data-steward/catalog` 接入目录聚合接口，新增左侧目录浏览、目录点击过滤、当前目录标签和版本筛选输入框。
  - 专项脚本补充目录接口、目录过滤和版本筛选断言。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，当前为 `24/24 PASS`。
- 边界确认：未新增 Agent 写库、NAS 修改、正文抽取、向量化、模型轻量化、自动审批或删除能力。
- 下一步：测试 agent 针对 P1 做短回归；若通过，可收口二期批次一。

## 2026-05-12：二期批次一 P1 二次修复

- 测试 agent 二次短回归反馈：无 P0，但真实页面仍不通过，剩余 2 个 P1：
  - 前端分页字段读取错误：真实后端返回 `items/pageNo/pageSize/total`，前端仍按 `rows/page/pageSize/total` 读取，导致总数显示但表格空白。
  - 目录接口语义错误：`catalog/directories` 仍返回完整文件路径级记录，不是真正目录聚合。
- 主 agent 已完成修复：
  - `fetchCatalogFiles(...)` 改为读取真实分页字段 `items/pageNo/pageSize/total`。
  - `catalog/directories` 改为按父目录聚合。
  - 父目录截取从 `LENGTH` 改为 `CHAR_LENGTH`，修复中文路径下按字节长度截取失败的问题。
  - `catalog/files?directoryPath=...` 使用同一父目录表达式过滤，保证目录节点和文件过滤语义一致。
  - 专项脚本新增“目录路径不得以文件扩展名结尾”的断言。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，当前 `25/25 PASS`。
  - 浏览器真实页面验证通过：选择 `105-启航华居项目` 后目录侧栏 `559` 个目录节点，点击目录后表格 `5` 行，版本 `V1` 筛选后表格仍 `5` 行。
- 边界确认：未新增 Agent 写库、NAS 修改、正文抽取、向量化、模型轻量化、自动审批或删除能力。
- 下一步：测试 agent 再做一次针对这 2 个 P1 的短回归；若通过，可收口二期批次一。

## 2026-05-13：二期批次一正式收口

- 测试 agent 已完成针对两个 P1 的短回归，报告：`handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前无 P0/P1/P2。
- 通过项：
  - 后端构建通过。
  - 前端构建通过。
  - 后端启动与健康检查通过。
  - 专项脚本 `scripts/dev/check-phase2-batch1-readonly-catalog.sh` 通过，结果 `25 PASS / 0 FAIL`。
  - 真实页面 `/data-steward/catalog` 不再出现“总数有值但表格空白”。
  - `catalog/directories` 已返回目录级聚合，项目 `503` 返回 `559` 个目录项。
  - 目录点击过滤可将文件列表收敛到具体目录。
  - 版本筛选 `version=V1` 可用。
- 主 agent 已写入收口报告：`handoff/main-agent/phase2-batch1-readonly-catalog-closure.md`。
- 收口裁决：二期批次一正式收口。
- 后续限制：在新的规划和用户确认前，不进入 selective indexing、NAS 文件正文抽取、Agent workflow、受控写操作、模型轻量化或构件级能力。

## 2026-05-13：Agent 预览页横向扩张热修复

- 用户反馈：进入 `/data-steward/agent-preview` 后页面持续向右扩张，导致浏览器卡死。
- 主 agent 定位：Agent 预览页内表格、权限证明长字段和字段合同标签未限制最小宽度与换行，触发主框架横向撑开。
- 修复范围：仅调整 `frontend/src/modules/data-steward/pages/AgentPreviewPage.vue` 的页面级 CSS，不改后端、不改业务接口、不改 NAS 数据。
- 修复内容：
  - 限制 Agent 预览页面、区块、字段合同、长文本和 Element Plus 表格/描述组件不向外撑宽。
  - 对长 traceId、证据值、标签内容开启安全换行。
  - 过滤区允许换行，避免窄屏或长选项时撑开页面。
- 验证：
  - 浏览器自动检查连续 8 次采样，`bodyScrollWidth/docScrollWidth/clientWidth` 均稳定为 `1440`，页面宽度稳定为 `1172`，未再横向增长。
  - `corepack pnpm build` 通过。
- 边界确认：未进入二期后续能力，未新增 Agent 写库、正文抽取、向量化、受控写操作、模型轻量化或构件级能力。

## 2026-05-13：文件资源页大项目卡顿修复

- 用户反馈：进入 `/data-steward/files` 文件资源页后，大项目会一次性加载当前项目全部文件，页面在加载期间明显卡顿，左侧菜单和其他按钮几乎无法响应。
- 主 agent 先尝试按要求调用 Claude Code 开发 agent，并要求使用项目内 Ralph Loop；但 Claude 会话长时间无有效进展且一度误读 Ralph 路径，已中断并由主 agent 直接接管修复。
- 根因确认：
  - 旧文件资源接口默认返回裸 `List<FileResourceResponse>`，前端进入页面立即拉取当前项目全部文件。
  - 前端把整项目大量文件直接放入单个 `el-table`，大项目场景下首屏渲染和请求都过重。
  - 切换筛选或离开页面时，旧请求缺少取消/过期保护。
- 修复内容：
  - 后端在保留旧接口兼容的前提下，为同一路径增加 `pageNo/pageSize` 分页查询合同。
  - SQL 下推分页和计数，支持 `fileKind`、`keyword`、`processStatus` 过滤。
  - 前端新增 `fetchFileResourcesPage(...)`，`/data-steward/files` 改为首屏只加载 50 条并显示分页器。
  - 前端增加请求取消和请求序号保护，避免快速切换项目、筛选或离开页面时旧响应回写状态。
  - `ModelIntegrationsPage.vue` 与 `DeliveryViewPanel.vue` 继续使用旧 helper，避免本轮扩大改动面。
- 验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查 `http://localhost:8080/actuator/health` 返回 `UP`。
  - 真实接口验证：项目 `503` 分页请求第一页返回 `20/2927`，模型筛选第一页返回 `20/198`，旧列表接口仍返回数组以兼容旧调用。
  - 浏览器回归：`/data-steward/files` 首屏只渲染 50 行，分页和文件类型筛选可用，加载中可立即跳转 `/data-steward/catalog`，`/data-steward/models` 和 `/work/document-delivery` 可打开。
- 报告：`handoff/dev-agent/latest-report.md`。

## 2026-05-14：二期批次二规划与 agent prompt 交接

- 用户确认 checksum 任务短回归通过，可以进入下一步。
- 主 agent 裁决：下一步进入二期批次二，但不直接做 selective indexing、正文抽取、Agent workflow、受控写操作、模型轻量化或构件级能力。
- 二期批次二名称固定为：`标准驱动交付闭环最小可用版`。
- 核心验收链路固定为：`项目上下文 -> 部位树 -> 节点类型锁定 -> 交付物标准 -> 交付物类型 -> 文件资源 -> 文档/图纸挂接 -> 交付完整率 -> 缺失项清单 -> 审计留痕`。
- 已新增主 agent 规划文档：`handoff/main-agent/phase2-batch2-standard-delivery-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
  - 要求开发 agent 使用 Ralph Loop。
  - 完成承诺：`<promise>PHASE2_BATCH2_STANDARD_DELIVERY_COMPLETE</promise>`。
  - 明确禁止修改 `docs/**`、旧 Flyway 迁移、真实 NAS 文件，以及越界到 Agent 写操作/模型轻量化/正文抽取/审核流。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
  - 要求验收交付完整率接口、标准未就绪提示、文档/图纸缺失项、挂接后完整率变化、文件选择性能、权限审计和 OpenAPI。
- 已更新主状态：`handoff/main-agent/status.md`。
- 边界确认：本轮只修文件资源页性能与交互问题，未改权限模型、未新增 NAS 写操作、未进入正文抽取/向量化/Agent 工作流/模型轻量化能力。

## 2026-05-14：二期批次二开发完成与主 agent 审计

- Claude Code 开发 agent 已按 `handoff/dev-agent/current-prompt.md` 使用 Ralph Loop 执行二期批次二，完成承诺为 `PHASE2_BATCH2_STANDARD_DELIVERY_COMPLETE`。
- 主 agent 监控中断过两次偏移：
  - 阻止开发 agent 将本批范围扩到 core 项目切换接口，要求撤回该方向。
  - 发现专项脚本把节点类型锁定失败从 `fail` 改成 `pass`，已要求恢复真实失败断言。
- 本批实现重点：
  - 后端新增交付完整率查询，按标准前置条件输出 `standardReady`、`readinessIssues`、总要求数、已完成数、缺失数、完成率和缺失项。
  - 文档交付与图纸交付可基于部位、交付物类型、文件资源进行挂接和删除，关键写动作写审计。
  - 前端文档/图纸交付页补充标准状态、完整率概览、缺失项列表、缺失项快捷挂接和分页文件选择。
  - 新增专项脚本 `scripts/dev/check-phase2-batch2-standard-delivery.sh`。
- 主 agent 审计补充：
  - 补回 `frontend/src/modules/work-center/api/delivery.ts` 中缺失的 `DashboardSummary` 类型定义，避免前端构建依赖未声明类型。
  - 加固 `scripts/dev/check-phase2-batch2-standard-delivery.sh`：复跑时优先复用已有节点类型，并清理本轮创建的部位节点，避免专项脚本继续堆积测试主数据。
- 主 agent 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查返回 `UP`。
  - 专项脚本 `check-phase2-batch2-standard-delivery.sh` 通过，`38/38 PASS`。
  - `git diff --check` 通过。
- 剩余注意：
  - 当前验收脚本不会修改、移动或删除真实 NAS 文件。
  - `targetType=OBJECT` 当前只验证空结果不 500，真实管理对象维度待后续批次有真实对象数据后再扩展。
  - 主 agent 认为可交给测试 agent 做正式验收；是否收口二期批次二以测试 agent 报告为准。

## 2026-05-14：二期批次二正式收口

- 测试 agent 已完成二期批次二正式验收，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过，当前无 `P0 / P1 / P2`。
- 已通过验证：
  - 后端构建。
  - 前端构建。
  - 后端健康检查。
  - `git diff --check`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-agent-db2-contract.sh`。
  - `scripts/dev/check-scan-task-control.sh`。
- 页面短回归通过：
  - `/work/document-delivery`。
  - `/work/drawing-delivery`。
- 已确认文件选择继续使用分页远程请求，不回退到全量加载。
- 已确认没有越界到模型轻量化、构件级解析、正文抽取、向量化、Agent 自动审批/写库、真实 NAS 文件移动/删除/改名。
- 主 agent 已创建收口报告：`handoff/main-agent/phase2-batch2-standard-delivery-closure.md`。
- 下一步建议：进入二期批次三规划，优先做 `审核流、整改闭环、报表导出`，继续保持小批次边界。

## 2026-05-14：交付页整体向右溢出热修复

- 用户反馈：
  - `/work/document-delivery` 页面会整体向右延伸。
  - `/work/drawing-delivery` 页面会整体向右延伸。
- 本轮边界：
  - 只修页面级横向溢出。
  - 不改业务逻辑。
  - 不改权限逻辑。
  - 不改后端接口。
  - 不修改 `docs/**`。
- 根因：
  - 交付页新增完整率、缺失项 Tab 和 Element Plus 表格后，表格列宽与 Tab 宽内容形成较大的内在宽度。
  - `.app-layout__content`、`.mvp-page`、`.mvp-page > *` 和 `.master-table` 缺少必要的 `min-width: 0`、`max-width: 100%` 等收敛约束。
  - 宽内容没有停在局部容器内，而是把页面主内容区和 header 一起撑宽。
- 修复文件：
  - `frontend/src/styles/index.css`。
- 修复内容：
  - 收紧 app 主内容区、header、项目选择区、MVP 页面容器和表格容器的宽度约束。
  - 允许 `.mvp-page` 直接子项在 grid/flex 场景下收缩。
  - 限制 `.master-table` 和 Element Plus 内部 wrapper 不把页面撑宽。
- 验证：
  - 修复前：`/work/document-delivery` 在 1440px 视口下页面滚动宽度约 `2406px`，`/work/drawing-delivery` 约 `2446px`。
  - 修复后：在 `1440px`、`1200px`、`1024px` 三种视口下，两页初始状态、缺失项页签和弹窗状态均为页面级横向溢出 `0`。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
- 报告：
  - `handoff/dev-agent/latest-report.md` 已更新为本轮热修复报告。
- 建议：
  - 测试 agent 做极短复验，只看两页是否仍整体横向溢出、缺失项页签、去挂接弹窗和文件选择分页远程加载是否正常。
- 复验收口：
  - 测试 agent 已完成极短复验，报告：`handoff/test-agent/latest-report.md`。
  - `/work/document-delivery` 与 `/work/drawing-delivery` 初始状态、缺失项页签和去挂接弹窗状态的页面级横向溢出均为 `0`。
  - 文件选择器仍为分页远程加载，未回退到全量加载。
  - 当前无 `P0 / P1 / P2`，本热修复正式收口。

## 2026-05-14：二期批次三规划与 agent prompt 交接

- 用户在二期批次二与交付页横向溢出热修复均收口后确认继续。
- 主 agent 裁决：进入二期批次三，但继续保持小批次边界，不进入模型轻量化、构件级解析、正文抽取、Agent workflow 或受控写操作。
- 二期批次三名称固定为：`人工审核、整改闭环与基础报表导出`。
- 核心验收链路固定为：`交付挂接 -> 人工审核 -> 驳回产生整改 -> 整改处理 -> 复审关闭 -> 报表导出 -> 审计留痕`。
- 已新增主 agent 规划文档：`handoff/main-agent/phase2-batch3-review-rectification-report-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
  - 要求开发 agent 使用 Ralph Loop。
  - 完成承诺：`<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`。
  - 明确禁止修改 `docs/**`、旧 Flyway 迁移、真实 NAS 文件，以及越界到 Agent 自动审批/自动整改/自动写库、模型轻量化、构件级解析、正文抽取、向量库或完整 BPM。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
  - 要求验收审核状态流转、驳回生成整改项、整改状态闭环、CSV 导出、权限审计、OpenAPI 和 NAS 安全。
- 已更新主状态：`handoff/main-agent/status.md`。
- 边界确认：本轮只是规划和交接 prompt，未写业务代码，未改 `docs/**`。

## 2026-05-13：Agent 预览页权限证明文件 ID 交互优化

- 用户反馈：`/data-steward/agent-preview` 的权限证明要求手动输入文件 ID，但页面没有明显告诉用户文件 ID 是什么，也不知道 ID 对应哪个文件。
- 主 agent 判断：现有前端 `CatalogFile` 已包含 `fileId/fileName/projectCode/projectName`，`fetchFilePermissionProof(fileId)` 已存在，本轮可只改前端，不需要改后端权限逻辑。
- 修复内容：
  - Agent 预览页资产目录样例表格新增 `文件ID` 列。
  - 样例表格每行新增 `验证权限` 操作，可直接基于当前行文件发起权限证明。
  - 权限证明结果区新增 `当前验证对象`：表格触发时显示文件 ID、文件名、项目编号、项目名称；手动输入时显示手动输入和文件 ID。
  - 手动改 ID 时会清理旧行上下文，避免旧文件名继续显示造成误导。
  - 资产目录页文件详情抽屉新增 `文件ID`。
- 验证：
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - 浏览器回归使用项目 `105 启航华居项目`，表格点击验证、手动输入 `46` 验证、目录详情显示文件 ID 均通过。
  - 页面宽度稳定为 `1440/1440/1440`，未横向撑爆。
- 报告：`handoff/dev-agent/latest-report.md`。
- 边界确认：未改后端，未修改 `docs/**`，未新增 Agent 写库、NAS 写操作、正文抽取、向量化、自动审批、模型轻量化或构件级能力。

## 2026-05-13：一期资产详情与数据质量人工治理补强

- 用户确认暂时跳过企业 agent 合并，继续补强一期主线。
- 主 agent 将范围锁定为内部资产治理能力，不进入企业 agent 合并、正文抽取、向量库、自动治理、自动审批、NAS 文件写操作、模型轻量化或构件级解析。
- 后端改动：
  - 新增 `PATCH /api/data-steward/assets/files/{fileId}/metadata`。
  - 可人工更新文件类型、专业、版本、置信度、审核状态。
  - 更新前校验项目权限，更新后写审计日志和事件流 `file.metadata.update`。
  - 未新增 Flyway 迁移，未修改 NAS 文件。
- 前端改动：
  - `/data-steward/assets/:projectId` 文件列表改用服务端分页。
  - 文件表格新增文件 ID、质量问题、详情、治理、补 checksum 任务入口。
  - 新增文件详情抽屉，展示识别信息、治理状态、来源与路径。
  - 新增人工治理弹窗，可补录专业、版本、置信度、审核状态和文件类型。
  - `补 checksum` 只创建后端任务，不同步计算、不读取正文。
- 新增脚本：`scripts/dev/check-asset-detail-governance.sh`。
- 验证：
  - 后端构建通过。
  - 前端构建通过。
  - 后端健康检查返回 `UP`。
  - 新增脚本 `check-asset-detail-governance.sh` 通过。
  - 既有脚本 `check-asset-quality-overview.sh` 通过。
  - 浏览器回归：`/data-steward/assets/2` 文件详情抽屉、人工治理弹窗、分页器可用，页面宽度稳定。
- 报告：`handoff/dev-agent/latest-report.md`。

## 2026-05-14：二期批次三开发 agent 完成与主 agent 审计

- 用户确认进入二期批次三后，主 agent 通过项目目录下 Claude Code CLI 调用开发 agent，模型使用 `deepseek-v4-pro`。
- 开发 agent 已按 Ralph Loop 完成 `人工审核、整改闭环与基础报表导出`，完成承诺为 `<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`。
- 本批新增能力：
  - 交付绑定提交审核、审核通过、审核驳回。
  - 审核记录查询。
  - 驳回后自动生成整改项。
  - 整改项列表、详情、更新、标记已处理、关闭、重新打开。
  - 交付完整率、审核汇总、整改项三类 CSV 导出。
  - 前端交付页审核动作、审核记录抽屉、驳回弹窗、整改中心页面和导出入口。
  - Flyway 追加 `V19__batch3_review_rectification_report.sql`，未修改旧迁移。
- 主 agent 审计边界：
  - 未发现进入完整 BPM、多级会签、Agent 自动审批/自动整改/自动写库、模型轻量化、构件级解析、正文抽取、向量库或真实 NAS 文件操作。
  - 新增接口均校验当前项目上下文；本批仍只证明项目隔离，不承诺客户生产级细粒度审批角色体系。
- 主 agent 补丁：
  - 调整 `scripts/dev/check-phase2-batch3-review-rectification-report.sh`。
  - 原脚本验证 `reopen` 后会留下隐藏开放整改测试数据；现改为 `reopen` 后再执行 `resolve + close` 并断言通过，减少复跑污染。
- 主 agent 验证：
  - 后端构建 `./mvnw -pl delivery-app -am -DskipTests package` 通过。
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查 `/actuator/health` 返回 `UP`。
  - `git diff --check` 通过。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
- 当前裁决：
  - 主 agent 审计通过，当前无已知 P0/P1。
  - 可交给测试 agent 按 `handoff/test-agent/current-prompt.md` 做正式验收。
  - 二期批次三是否正式收口以测试 agent 报告为准。

## 2026-05-14：二期批次三正式收口

- 测试 agent 已完成二期批次三正式验收，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过。
- 当前问题分级：
  - P0：无。
  - P1：无。
  - P2：前端构建仍有 Vite chunk size warning，不阻塞收口。
- 已通过验证：
  - 后端构建。
  - 前端构建。
  - 后端健康检查。
  - `git diff --check`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-agent-db2-contract.sh`。
- 页面正式验收通过：
  - `/work/document-delivery`：提交审核、审核通过、驳回、审核记录、CSV 导出可用，页面级横向溢出为 `0`。
  - `/work/drawing-delivery`：页面可打开，缺失项和新增挂接弹窗不撑宽，文件选择仍为分页远程加载。
  - `/work/rectifications`：整改项标记已处理、关闭、重新打开和 CSV 导出可用，页面级横向溢出为 `0`。
- 安全边界：
  - 普通用户跨项目审核、整改、导出均被受控拒绝。
  - 审计覆盖审核、驳回、整改处理、关闭、重开、报表导出。
  - 未触碰真实 NAS 文件，未读取文件正文，未破坏 DB-2 四个稳定 View。
- 主 agent 已创建收口报告：`handoff/main-agent/phase2-batch3-review-rectification-report-closure.md`。
- 下一步建议：
  - 进入二期批次四规划，优先考虑 `文件预览与下载权限分离最小闭环`。
  - 继续保持小批次边界，不进入模型轻量化、构件级解析、正文抽取、向量库、Agent workflow 或受控写操作。

## 2026-05-14：标准驱动交付流程新手可用性补丁

- 用户要求先不进入二期批次四规划，先修复测试报告中指出的文档问题。
- 主 agent 判断：测试 agent 最新报告实际是“标准驱动交付流程新手视角可用性复核报告”，其中 P1 指向页面说明、顺序引导和修复入口不足；本轮应修前端页面，不修改正式 `docs/**`。
- 本轮边界：
  - 只改五个目标页面的新手可用性说明。
  - 不改后端模型。
  - 不改数据库。
  - 不触碰真实 NAS 文件。
  - 不进入二期批次四。
- 修改文件：
  - `frontend/src/modules/master-data/components/StandardStatusPanel.vue`
  - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
  - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
  - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
  - `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
  - `frontend/src/styles/index.css`
- 修复内容：
  - 工程管理部位树页面增加第 1 步说明、业务示例和字段提示。
  - 节点类型页面增加第 2 步说明、锁定含义、锁定确认提示和适用层级解释。
  - 交付物标准页面增加第 3 步说明、推荐配置顺序、四块配置关系和字段提示。
  - `StandardStatusPanel` 增加按当前状态变化的下一步提示。
  - 文档/图纸交付页面增加交付执行说明、标准未就绪修复入口、缺失项解释和补交弹窗字段说明。
  - 将“去挂接”类关键动作调整为更业务化的“选择文件补交”。
- 未做项：
  - 未清理样板项目历史测试命名数据，避免混入数据治理动作。
  - 未补齐样板项目文档类标准，避免把本轮页面可用性补丁扩成样板数据维护。
  - 未做标准配置向导、帮助中心或客户版完整新手引导。
- 验证：
  - 前端构建 `corepack pnpm --dir frontend build` 通过。
  - 后端健康检查返回 `UP`。
  - 五个目标路由均返回 HTTP 200。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `git diff --check` 通过。
- 报告：
  - 主报告：`handoff/main-agent/standard-delivery-usability-patch-report.md`。
  - 开发报告：`handoff/dev-agent/latest-report.md`。
  - 测试 prompt：`handoff/test-agent/current-prompt.md`。
- 当前裁决：
  - 本轮 P1 已完成开发修复。
  - 建议测试 agent 做短回归后再正式收口本补丁。

## 2026-05-14：标准驱动交付流程新手可用性补丁正式收口

- 测试 agent 已完成短回归，并写入 `handoff/test-agent/latest-report.md`。
- 测试结论：通过。
- 当前问题分级：
  - P0：无。
  - P1：无。
  - P2：前端构建仍有既有 Vite chunk size warning。
- 上一轮 P1 已关闭：
  - `标准驱动交付链路对新手缺少业务解释、顺序引导和修复入口提示`。
- 五个页面逐项验收通过：
  - `/master-data/sections`：第 1 步说明、部位树作用、业务示例、弹窗辅助说明通过。
  - `/master-data/node-types`：第 2 步说明、节点类型与部位层级关系、锁定意义和确认说明通过。
  - `/master-data/deliverable-standard`：第 3 步说明、推荐配置顺序、四块配置说明、关键字段提示通过。
  - `/work/document-delivery`：前置条件缺失时可看到修复入口，页面级横向溢出为 `0`。
  - `/work/drawing-delivery`：缺失项解释、`选择文件补交`、补交弹窗字段说明、分页远程文件选择和横向溢出检查通过。
- 脚本回归：
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
- 非阻塞观察：
  - 样板项目里仍有若干 `PH2B2-*` 测试节点命名，这属于样板数据整理或演示观感优化，不影响本轮 P1 收口。
- 主 agent 已创建收口报告：`handoff/main-agent/standard-delivery-usability-patch-closure.md`。
- 下一步建议：
  - 先做 `样板项目演示数据清理与标准闭环补齐`。
  - 不建议立即进入二期批次四，因为当前更需要把样板项目调整到适合内部验收和领导演示的状态。

## 2026-05-15：样板项目清理暂缓，恢复二期主线

- 用户最新裁决：样板项目仍需继续用于业务链路测试，因此暂不清理、暂不重置、暂不隐藏 `PH2B2-*` 等历史测试命名。
- 主 agent 调整：
  - 样板项目整理降级为后续演示观感优化项，不再作为进入下一批次前的阻塞项。
  - 真实 NAS 数据仍保持只读保护，不因样板项目决策发生任何移动、删除或改名。
  - 开发和测试 agent 当前仍保持等待状态，不能自行开工。
- 下一步建议：
  - 恢复二期主线推进，进入 `二期批次四：文件预览与下载权限分离最小闭环` 规划。
  - 批次四仍保持小范围：只处理文件预览入口、下载权限与查看权限分离、访问审计和最小回归脚本。
  - 暂不进入模型轻量化、构件级解析、正文抽取、向量库、Agent workflow、自动审批或真实 NAS 写操作。

## 2026-05-15：二期批次四规划与 prompt 就绪

- 主 agent 已创建二期批次四规划文档：`handoff/main-agent/phase2-batch4-file-preview-download-permission-plan.md`。
- 已更新开发 agent prompt：`handoff/dev-agent/current-prompt.md`。
- 已更新测试 agent prompt：`handoff/test-agent/current-prompt.md`。
- 批次四固定范围：
  - 文件预览与下载权限分离。
  - 短时访问票据。
  - 平台受控读取文件。
  - PDF/图片浏览器原生预览。
  - 下载/预览/拒绝/失败审计。
  - 专项脚本 `scripts/dev/check-phase2-batch4-file-access.sh`。
- 批次四禁止范围：
  - 样板项目清理。
  - 模型轻量化。
  - 构件级解析。
  - Office/CAD/BIM 转换。
  - 正文抽取或向量库。
  - Agent 自动业务动作。
  - 真实 NAS 写操作。
- 当前状态：规划和 prompt 已就绪，尚未启动开发 agent。

## 2026-05-15：二期批次四主 agent 收拢完成，待测试复验

- 主 agent 已启动 Claude Code 开发 agent 两次：
  - 第一次长 prompt 通过 `script` 包装运行，长期无输出，但留下早期后端雏形。
  - 第二次短 prompt 直接调用 `claude --print`，仍无完成输出、无报告。
- 主 agent 中断卡住的 Claude 会话后接管开发，完成批次四最小闭环。
- 已完成改动：
  - 新增 `V20__batch4_file_access_tickets.sql`。
  - 新增文件预览/下载权限：`DATA_STEWARD_FILE_PREVIEW`、`DATA_STEWARD_FILE_DOWNLOAD`。
  - 新增短时访问票据表 `data_file_access_tickets`。
  - 新增票据创建接口和票据访问接口。
  - 允许票据访问 URL 免 JWT，但创建票据必须先经登录用户权限校验。
  - 支持 `nas:///Volumes/...` 和 `nas:///tmp/...` 只读流式读取。
  - 文件详情页和资产目录抽屉增加预览/下载权限显示与受控访问入口。
  - 新增专项脚本 `scripts/dev/check-phase2-batch4-file-access.sh`。
- 关键修复：
  - 首轮专项脚本发现拒绝/失败审计被事务回滚吞掉。
  - 已移除票据创建/打开方法的大事务包裹，使权限拒绝和路径失败审计能落库。
- 验证结果：
  - 后端构建通过。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=12 FAIL=0`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - `scripts/dev/check-phase2-batch2-standard-delivery.sh`：`38/38 PASS`。
  - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`：`52/52 PASS`。
  - `git diff --check`：通过。
- 边界确认：
  - 未清理样板项目。
  - 未做模型轻量化、构件级解析、Office/CAD/BIM 转换、正文抽取、向量库或 Agent 自动动作。
  - 未写入、移动、删除或改名真实 NAS `/Volumes/zyzn/卓羽智能项目`。
- 当前裁决：
  - 主 agent 自测通过。
  - 建议测试 agent 按 `handoff/test-agent/current-prompt.md` 做正式验收。

## 2026-05-15：二期批次四 P0 返修完成，待测试复验

- 测试 agent 正式验收首轮结论：不通过，存在明确 P0，不能收口二期批次四。
- P0 内容：
  - 普通项目用户可在 `/data-steward/catalog` 文件详情和目录详情接口中看到真实 `nas:///...` 路径。
  - 受影响角色包括 `phase2.viewer` 和 `delivery.engineer`。
- 根因判断：
  - 文件预览/下载票据链路已经受控，但只读目录接口仍沿用旧的路径可见性逻辑，把“本地开发环境路径可见”错误扩大到了普通项目用户。
- 修复文件：
  - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
  - `scripts/dev/check-phase2-batch4-file-access.sh`
- 修复内容：
  - 目录列表与目录详情统一按项目角色判断路径可见性。
  - 仅 `PROJECT_ADMIN` 返回真实 `storagePath`。
  - `PROJECT_VIEWER`、`DELIVERY_ENGINEER` 等普通项目角色返回 `storagePathVisible=false`、`storagePath=null`、`storagePathVisibilityReason=PATH_HIDDEN_BY_PERMISSION`。
  - `agentContractView` 在路径隐藏时不再声明 `storagePath` 字段可用。
  - 专项脚本新增查看者和交付工程师目录详情路径隐藏断言。
- 回归结果：
  - 后端构建通过。
  - 后端健康检查 `UP`。
  - `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=14 FAIL=0`。
  - `scripts/dev/check-file-preview-shell.sh`：通过。
  - `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
  - 前端构建通过，仅有既有 Vite chunk size warning。
  - `git diff --check` 通过。
- 当前裁决：
  - 主 agent 认为测试报告中的 P0 已修复。
  - 二期批次四仍不直接收口，需测试 agent 做正式复验后再关闭。

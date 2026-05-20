# 开发 Agent 当前任务：M1E 文件管理连续工作体验与后台任务可追踪性收口

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 当前路线

M1A、M1B、M1C、M1D 均已收口。主线当前为绿灯，但这只代表可以继续平台本体开发，不代表可以进入 9A 客户交付。

当前 active 批次：

`M1E：文件管理连续工作体验与后台任务可追踪性收口`

完成承诺固定为：

`<promise>MAINLINE_M1E_FILE_TASK_CONTINUITY_COMPLETE</promise>`

本批目标不是新增 NAS 写能力，也不是进入 M2A，而是解决真实员工在复杂项目资产目录中工作时的连续性断点。

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
5. `handoff/main-agent/m1d-standard-delivery-loop-closure.md`
6. `handoff/main-agent/m1e-file-task-continuity-plan.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

重点检查：

1. 文件管理：
   - `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
   - `frontend/src/modules/data-steward/components/DirectoryTreePanel.vue`
   - `frontend/src/modules/data-steward/utils/directoryTree.ts`
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
2. 数据管家 API：
   - `frontend/src/modules/data-steward/api/dataSteward.ts`
3. 后台任务后端：
   - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/**`
4. 既有脚本：
   - `scripts/dev/check-bim-asset-batch2.sh`
   - `scripts/dev/check-bim-asset-batch3.sh`
   - `scripts/dev/check-phase2-batch4-file-access.sh`
   - `scripts/dev/check-phase2-batch5b-data-steward-modules.sh`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮必须解决的问题

1. 真实 NAS 项目目录层级深，文件管理离开后回来容易丢失目录位置、筛选、分页和选中文件。
2. 文件 ID、项目内部 ID、checksum 任务 ID 对业务用户不可理解。
3. 点击补 checksum 后，如果只看到任务 ID，用户不知道任务在哪里、是否完成、是否失败。
4. 文件管理右侧区域已经有表格，但连续工作体验和后台任务追踪仍不够像真实资产管理工具。

## 3. 本轮目标

完成两条主线：

1. 文件管理按项目记忆工作状态。
2. checksum 后台任务在项目内可追踪、可理解、可重试。

### A. 文件管理按项目记忆工作状态

文件管理需要按项目记住：

- 当前目录。
- 关键筛选：关键词、文件类型、专业、扩展名、质量问题。
- 分页页码、页大小。
- 最近选中的文件或最近打开的文件详情。
- 如果当前目录树支持，记住展开状态。

建议：

- 用 URL query 保存可分享/可恢复的关键状态。
- 用 localStorage 或前端 store 保存每个项目最近浏览状态。
- 提供“重置视图”入口，避免记忆状态无法清除。

### B. 文件与项目 ID 业务化解释

页面保留内部 ID，但必须绑定可读对象展示：

- 文件 ID 显示为“平台文件ID”，并和文件名、扩展名、大小、更新时间一起展示。
- 项目内部 ID 如出现，应标注为“平台内部ID”，业务上优先展示项目编码和项目名称。
- checksum 任务 ID 显示为“后台任务编号”，并始终和文件名 / 文件 ID / 状态绑定。

### C. checksum 任务可追踪

点击“补 checksum”后，用户必须看到：

- 后台任务编号。
- 对应文件名。
- 平台文件 ID。
- 当前状态。
- 进度。
- 创建时间 / 更新时间。
- 失败原因，且必须脱敏。

如果任务失败，用户能看到失败原因并可重试。

本批不要求做全局任务中心；可以在项目文件管理页提供轻量任务抽屉、弹窗或项目内任务区域。

### D. 安全边界

任务失败原因、文件详情、任务详情不得泄露：

- 真实 NAS 路径。
- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- raw row。
- SQL。
- token / secret / password。

## 4. 允许做什么

允许：

- 优化文件管理页面状态记忆和恢复。
- 小范围调整 URL query / localStorage / store。
- 优化文件 ID / 项目内部 ID / 任务 ID 文案。
- 在项目文件管理页增加轻量 checksum 任务状态抽屉、弹窗或区域。
- 补齐任务失败原因脱敏展示。
- 补充重试入口，复用既有 `retryAssetJob(jobId)`。
- 新增或增强专项脚本，例如 `scripts/dev/check-m1e-file-task-continuity.sh`。

## 5. 禁止做什么

禁止：

1. 真实 NAS 上传、新建文件夹、移动、删除、重命名、更新版本。
2. 读取 PDF / Office / DWG / RVT / IFC 正文。
3. 做真实 BIM 轻量化。
4. 做构件级解析。
5. 做 selective indexing。
6. 新增 Hermes 能力。
7. 继续 G4。
8. 进入 8B / 8C / 9A。
9. Agent 自动审批、自动整改、自动挂接。
10. 把 catalog metadata 冒充正文 evidence。
11. 返回真实 NAS 路径、raw row、SQL、token、secret、password。
12. 为 105 / 503 写死特殊逻辑。
13. 修改 `docs/**`，除非主 agent 单独授权。

## 6. 建议执行方式

建议按这个顺序做：

1. 审计 `AssetProjectFileBrowser.vue` 当前目录、筛选、分页、文件详情和 checksum 操作。
2. 设计最小状态恢复：URL query + localStorage 兜底。
3. 添加“恢复上次位置 / 重置视图”类提示或按钮。
4. 优化文件 ID 和项目内部 ID 文案。
5. 点击补 checksum 后打开任务状态区域，展示任务和文件关系。
6. 失败任务能显示脱敏失败原因，并可重试。
7. 新增 M1E 专项脚本，覆盖状态记忆、任务可见、权限和 forbidden 字段。

## 7. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch5b-data-steward-modules.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
bash scripts/dev/check-m1d-standard-delivery-loop.sh
git diff --check
```

如果新增 `scripts/dev/check-m1e-file-task-continuity.sh`，必须执行并记录。

浏览器自测至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets/503?tab=files`
- 进入多层目录，设置筛选和分页，离开后回到文件管理，确认状态恢复。
- 点击重置视图，确认目录和筛选恢复默认。
- 打开文件详情，确认平台文件 ID 与文件名绑定展示。
- 点击补 checksum，确认任务状态区域展示后台任务编号、文件名、平台文件 ID、状态和失败原因。
- `/data-steward/assets/506?tab=files` 同样可用。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 M1E 当前 active。
3. 是否确认 G4 / Hermes / 8B / 9A 冻结。
4. 文件管理状态记忆实现方式。
5. 文件 ID / 项目内部 ID / checksum 任务 ID 文案如何业务化。
6. checksum 任务如何展示状态、失败原因和重试。
7. 105/503 与 93/506 抽查结果。
8. 是否触碰后端；如果触碰，说明是否只在 data-steward asset job 范围。
9. 修改文件清单。
10. 自测命令结果。
11. P0 / P1 / P2 列表。
12. 是否建议进入 M1E 测试验收。

# 开发 Agent 报告：M1A 平台主线功能基线审计与交付闭环缺口收束

时间：2026-05-20

## 1. 本轮目标

本轮执行 `M1A：平台主线功能基线审计与交付闭环缺口收束`。

目标是暂停 G4 / Hermes 扩展开发后，回到平台主线，对真实 NAS 项目的资产总览、项目工作台、文件管理、工程主数据、交付页、整改闭环、导出预检查、BIM Mock 安全入口、权限/审计/路径脱敏做基线审计，并只修复 P0/P1 或很小的 P2 缺口。

完成标记：`<promise>MAINLINE_M1A_PLATFORM_BASELINE_AUDIT_COMPLETE</promise>`

## 2. Git 基线

- 当前分支：`codex/platform-m1a-baseline-fixes`
- 本轮主线基线提交：`0c97419 docs: start m-series mainline stabilization`
- 主 agent 迁移说明：开发 agent 首次执行时误在 `codex/hermes-alignment-0a-contract-freeze` 分支完成本轮改动；主 agent 已将本轮代码改动和报告迁移到从 `main` 拉出的平台分支 `codex/platform-m1a-baseline-fixes`，避免继续污染 Hermes 冻结线。
- 本轮未提交 git。
- 工作区原有未跟踪项仍保留：
  - `tmp/jar-inspect/`
  - `tmp/run-logs/hermes-gateway.pid`

## 3. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

未修改 `docs/**`，未修改共享文档空间。

## 4. G4 / Hermes 冻结确认

- 已确认当前主线要求：G4 暂停，Hermes 冻结。
- 本轮没有修改 `AgentGatewayApplicationService`、Hermes Gateway、Hermes Chat、Hermes memory 或外部 Hermes 项目。
- 没有新增 Hermes 能力，没有进入 8B / 8C / 9A。

## 5. 审计页面与接口

页面抽查：

- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/503` 的“文件管理”页签
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/master-data/node-types`
- `/data-steward/assets/503/master-data/deliverable-standard`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/503/data-steward/models`
- `/data-steward/assets/506`
- `/data-steward/assets/506/master-data/initialization`
- `/data-steward/assets/506/work/document-delivery`
- `/data-steward/assets/506/work/drawing-delivery`
- `/data-steward/files`

接口抽查：

- `GET /api/core/users/me`
- `POST /api/core/projects/{projectId}:switch`
- `GET /api/master-data/projects/{projectId}/onboarding/assessment`
- `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck`
- `GET /api/work-center/projects/{projectId}/delivery-completeness`
- `GET /api/work-center/projects/{projectId}/rectifications`
- `GET /api/data-steward/catalog/directories`
- `GET /api/data-steward/catalog/files`
- `GET /api/data-steward/catalog/files/{fileId}`
- `GET /api/data-steward/projects/{projectId}/file-resources`

## 6. 真实项目抽查结果

抽查项目：

- `503 / 105 / 启航华居项目`
- `506 / 93 / 中建八局国交酒店项目`

结果：

- 两个项目均可通过平台项目切换接口进入当前项目上下文。
- 主数据初始化评估、交付完整性、导出预检查、整改列表、目录树、目录文件列表均返回 200。
- 105 项目导出预检查已有图纸侧数据；93 项目当前交付侧为空数据但接口正常。
- 前端页面均能打开，无 403/500/加载失败提示。
- 项目文件管理页签可见只读/受控预览下载类文案。
- 前端可见文本未命中 `nas://`、`smb://`、`/Volumes/`、`/Users/`、`storage_path/storage_uri` 等底层路径痕迹。

## 7. P0 / P1 / P2

P0：

- 未发现 P0。

P1：

- 发现 catalog 文件详情对项目管理员仍可返回底层存储路径。已修复为 catalog-only 场景一律不返回真实 `storagePath`，并返回 `PATH_NOT_EXPOSABLE_CATALOG_ONLY`。
- 发现旧“文件资源”页面列表会展示存储地址。已修复为前端只显示“底层路径已隐藏”，后端文件资源列表/创建/处理响应清空 `storageUri` 值。

P2：

- 前端构建仍有既有 Vite chunk size warning，未在本轮处理。

## 8. 改动文件

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
  - 移除项目管理员可见 raw catalog storage path 的分支。
  - catalog 详情统一返回 `storagePath=null`、`storagePathVisible=false`、`storagePathVisibilityReason=PATH_NOT_EXPOSABLE_CATALOG_ONLY`。
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/file/FileResourceApplicationService.java`
  - 对文件资源接口响应做输出脱敏，`storageUri` 值不再返回给前端。
  - 内部 `requireFile` 保持原有能力，避免影响文件预览、下载、模型集成等内部校验。
- `frontend/src/modules/data-steward/api/dataSteward.ts`
  - `FileResource.storageUri` 类型允许为空。
- `frontend/src/modules/data-steward/pages/FileResourcesPage.vue`
  - 列表不再展示存储地址，改为显示“底层路径已隐藏”。
- `handoff/dev-agent/latest-report.md`
  - 写入本轮报告。

未新增数据库迁移，未修改旧 Flyway migration。

## 9. 安全边界

确认保持：

- 未触碰真实 NAS 文件。
- 未创建、移动、删除、重命名、上传真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未做真实 BIM 轻量化、模型转换或构件解析。
- 未启用 parser / writer / indexing。
- 未写 OpenSearch / Qdrant / MinIO documents / chunks。
- 未让 Agent 自动审批、自动整改、自动挂接或自动删除。
- 未暴露 raw NAS path、raw DB row、SQL、secret、token、password。
- Hermes / Agent 边界保持冻结，未新增 Hermes 能力。

## 10. 路径脱敏复核

最终复扫：

- 105 catalog list/detail：未发现 raw path；详情 `storagePath=null`，`storagePathVisible=false`。
- 93 catalog list/detail：未发现 raw path；详情 `storagePath=null`，`storagePathVisible=false`。
- 105 / 93 file-resources API：`storageUri` 值均为 `null`，未发现 raw path。
- `/data-steward/files` 页面：未发现 raw path，可见“底层路径已隐藏”。

## 11. 验证结果

- 后端构建：通过。
  - `./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过。
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 健康检查：通过。
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - `{"status":"UP"}`
- 文件访问安全回归：通过。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`
  - `PASS=18 FAIL=0`
- 6A 回归：通过。
  - `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
- 6B 回归：通过。
  - `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
  - `PASS=17 FAIL=0`
- 7A 回归：通过。
  - `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
  - `PASS=18 FAIL=0`
- 8A 回归：通过。
  - `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
  - `PASS=11 FAIL=0`
- `git diff --check`：通过。

## 12. 已知风险与未完成事项

- M1A 只做主线基线审计和小修，未重做前端信息架构。
- 105/93 的交付数据密度不同，93 项目当前多个交付接口为空结果但接口和页面状态正常。
- 旧文件资源登记弹窗仍允许输入存储地址用于登记文件资源；本轮只收束输出侧展示和 API 响应，不扩大到重构文件登记模型。
- 前端主包体积 warning 仍是既有 P2。

## 13. 是否建议进入下一步主线开发

建议进入下一步主线平台开发，但不建议进入 Hermes/G4 扩展。

建议下一步仍围绕平台主线做：

- 真实项目主数据映射质量补齐。
- 交付标准与真实项目目录的映射体验优化。
- 文件管理只读状态和受控预览/下载入口继续打磨。
- 针对 105/93 等真实项目补更细的验收脚本。

## 14. 给测试 Agent 的测试 Prompt

```md
# 测试 Agent Prompt：M1A 平台主线功能基线审计验收

工作目录：`/Users/vc/Documents/数字化交付平台`

本轮只验收 `M1A：平台主线功能基线审计与交付闭环缺口收束`。G4 / Hermes 已暂停，不测试新增 Hermes 能力，不进入 8B / 8C / 9A，不测试真实 NAS 写操作、正文解析、BIM 构件解析、parser/writer/indexing 或 production rollout。

必须先读：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

重点验收：

1. G4 / Hermes 冻结
   - 确认本轮未新增 Hermes 能力。
   - 确认未修改 Hermes Gateway / memory / external Hermes 项目。
2. 真实项目页面
   - 抽查 105 项目：`/data-steward/assets/503`、文件管理页签、初始化、部位、节点类型、交付物标准、文档交付、图纸交付、整改、BIM Mock。
   - 抽查 93 项目：`/data-steward/assets/506`、初始化、文档交付、图纸交付。
   - 页面不能出现 403/500/加载失败。
3. 真实项目接口
   - 对 503/506 分别通过 `/api/core/projects/{projectId}:switch` 切换上下文。
   - 验证 onboarding assessment、delivery completeness、export precheck、rectifications、catalog directories/files 均可用。
4. 路径脱敏
   - catalog list/detail 不得暴露 `nas://`、`smb://`、`/Volumes/`、`/Users/`、`storage_path/storage_uri` 真值。
   - catalog detail 应返回 `storagePath=null`、`storagePathVisible=false`。
   - `/data-steward/files` 列表应显示“底层路径已隐藏”，不得展示真实存储地址。
5. 只读与安全边界
   - 不得触碰真实 NAS 文件。
   - 不得读取 PDF/Office/DWG/RVT/IFC 正文。
   - 不得启用 parser/writer/indexing。
   - 不得做真实 BIM 轻量化或模型转换。
6. 回归脚本
   - `./mvnw -pl delivery-app -am -DskipTests package`
   - `corepack pnpm --dir frontend build`
   - `curl -fsS http://127.0.0.1:8080/actuator/health`
   - `bash scripts/dev/check-phase2-batch4-file-access.sh`
   - `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
   - `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
   - `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
   - `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
   - `git diff --check`

通过标准：

- 构建、健康检查、脚本均通过。
- 105/93 页面和接口可用。
- 没有 raw NAS path / raw storage path / SQL / secret/token/password 泄露。
- 没有真实 NAS 写操作。
- 没有 Hermes/G4 扩展开发。
- 报告事实完整，不宣布最终收口。
```

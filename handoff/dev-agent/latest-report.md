# 开发 Agent 报告：M1C 工程主数据真实项目落地

时间：2026-05-20

## 1. 本轮目标

本轮执行 `M1C：工程主数据真实项目落地`。

目标是把真实项目资产目录接入工程主数据初始化链路：基于 catalog-only 目录证据生成真实项目接入评估与草案预览，并通过人工确认后创建/补齐工程部位、节点类型、交付定义、交付类型、交付属性和目录模板。草案必须明确“模板只是基础骨架，不代表真实工程结构已识别”。

完成标记：`<promise>MAINLINE_M1C_REAL_PROJECT_MASTERDATA_COMPLETE</promise>`

## 2. Git 基线

- 当前分支：`codex/platform-m1c-real-masterdata`
- 本轮基线提交：`0e73eae`
- 本轮未提交 git。
- 工作区原有/临时未跟踪项仍保留：
  - `tmp/jar-inspect/`
  - `tmp/run-logs/backend-m1b-test.pid`
  - `tmp/run-logs/backend-m1c.pid`
  - `tmp/run-logs/hermes-gateway.pid`

## 3. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1b-project-workbench-usability-closure.md`
- `handoff/main-agent/m1c-real-project-masterdata-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

未修改 `docs/**`，未修改共享文档空间。

## 4. 命名与冻结边界

- 本轮只做 M1C。
- 未继续 G4 / Hermes 开发。
- 未进入 8B / 8C / 9A。
- 未新增 H1 / R1 等临时批次命名。
- 未修改 Hermes Gateway、Hermes memory、外部 Hermes 项目或 Agent 自动治理能力。
- 未硬编码 105 / 503 作为业务逻辑；仅在专项脚本中作为真实项目验收样本。

## 5. 7A/Hermes 以外边界确认

本轮没有做以下事项：

- 未让 Hermes 裸连 DB。
- 未让 Agent 生成 SQL。
- 未做 Agent DB CRUD。
- 未做 NAS scan。
- 未启用 parser / writer / indexing。
- 未做 BIM 轻量化、模型转换、构件解析。
- 未写 OpenSearch / Qdrant / MinIO / documents / chunks。
- 未做 production rollout。

## 6. 后端改动

改动文件：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`

主要改动：

- `GET /api/master-data/projects/{projectId}/onboarding/assessment`
  - 增加 `projectCode`、`projectName`、`assetSource`、`realNasProject`。
  - `assetSummary` 增加 `scanTaskCount`、`dominantFileExtensions`、`dominantDisciplines`、`directoryClues`。
  - 评估证据增加项目来源、扩展名、专业、脱敏目录线索、扫描任务记录。
  - 非 `NAS_REAL*` 项目返回 `REAL_NAS_SOURCE_NOT_CONFIRMED` gap。
  - 对模型/图纸相关问题保持 Missing Evidence 口径：缺少 `rvt_parse_evidence_missing,dwg_parse_evidence_missing,component_evidence_missing,model_parse_evidence_missing`。

- `GET /api/master-data/projects/{projectId}/onboarding/preview`
  - 增加 `assetCatalogOnly=true`。
  - `draftItems` 增加 `evidenceMode`、`evidenceSource`、`confidenceLevel`、`riskHint`。
  - 草案项区分 `EXISTING_PROJECT_MASTERDATA`、`CATALOG_DIRECTORY_CLUE`、`CATALOG_FILE_KIND_CLUE`、`TEMPLATE_SKELETON`。
  - 草案只做 dry-run，不读文件正文，不触碰 NAS。

- `POST /api/master-data/projects/{projectId}/onboarding/apply`
  - 保持 `confirmed=true` 人工确认闸门。
  - 继续复用既有模板应用逻辑，重复应用只跳过已有项，不覆盖项目已有主数据。
  - next action 去掉 Agent/G4 指向，改为进入文档交付和图纸交付继续人工复核。

- 路径脱敏
  - `directoryClues` 只返回脱敏后的项目内目录线索。
  - 过滤 `storage_path`、`storage_uri`、`nas://`、`smb://`、`afp://`、`/Volumes/`、`/Users/`，以及孤立的 `Volumes`、`Users`、`storage`、`nas`、`smb`。
  - 若无法安全展示目录线索，返回 `项目内目录线索已脱敏`。

## 7. 前端改动

改动文件：

- `frontend/src/modules/master-data/api/masterData.ts`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
- `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
- `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`

主要改动：

- 初始化页标题统一为 `M1C 真实项目接入`。
- 接入评估页展示：
  - 真实 NAS 项目 / 来源待确认。
  - 项目来源、文件计数、模型/图纸/文档计数、路径映射、扫描记录、最近扫描。
  - 主要扩展名、主要专业线索、主要目录线索。
  - 缺口 severity 与 Missing Evidence reason。
- 草案预览增加“草案证据与风险”表：
  - 类别、草案项、来源、证据模式、置信度、风险提示、确认状态。
  - 明确 catalog-only 线索不能替代真实工程结构、正文证据或模型解析证据。
- 草案应用后的入口增加：
  - 查看部位树。
  - 查看节点类型。
  - 查看交付物标准。
  - 进入文档交付。
  - 进入图纸交付。
- 部位树、节点类型、交付物标准页新增接入草案复核提醒。

## 8. 脚本改动

新增：

- `scripts/dev/check-m1c-real-project-masterdata.sh`

脚本覆盖：

- 登录平台。
- 切换并验证真实项目 `503 / 105`。
- 切换并验证真实项目 `506 / 93`。
- 检查 assessment / preview 的 catalog-only 合同。
- 检查 `assetCatalogOnly=true`、`evidenceMode=catalog_only`、`dryRun=true`、`confirmedRequired=true`、`nasTouched=false`、`contentRead=false`。
- 检查 `draftItems` 的 evidence source、confidence、risk hint、pending confirmation。
- 检查未确认时禁止 apply。
- 创建隔离 smoke 项目，插入目录级测试数据，验证确认应用与重复应用幂等。
- 检查 `masterdata.initialization.template-apply` 审计事件。
- 检查响应无 raw path / storage / SQL / secret / token / password 等禁出字段。

## 9. 数据库与迁移

- 未新增数据库迁移。
- 未修改旧 Flyway migration。
- M1C 专项脚本创建隔离 smoke 项目与目录级测试数据，仅用于验收。
- 未创建、移动、删除、重命名、上传真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。

## 10. 路径与敏感字段自查

已自查：

- 未把项目路径、NAS 路径、`storage_path`、`storage_uri` 透传到前端或 Hermes。
- `directoryClues` 已改为脱敏线索或 `项目内目录线索已脱敏`。
- 前端可见文本和专项脚本响应扫描均未发现：
  - raw `storage_path`
  - `storage_uri`
  - NAS 原始路径
  - raw DB row
  - SQL
  - secret / token / password
  - DWG/RVT 内部内容
  - PDF/Office 正文内容

## 11. 构建与脚本结果

- 后端构建：通过。
  - `./mvnw -pl delivery-app -am -DskipTests package`
  - `BUILD SUCCESS`
- 前端构建：通过。
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 健康检查：通过。
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - `{"status":"UP"}`
- M1C 专项脚本：通过。
  - `bash scripts/dev/check-m1c-real-project-masterdata.sh`
  - `PASS=14 FAIL=0`
- 文件访问安全回归：通过。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`
  - `PASS=18 FAIL=0`
- 6A 项目初始化回归：通过。
  - `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
  - `phase2 batch6a project initialization ok`
- 6B 交付包回归：通过。
  - `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
  - `PASS=17 FAIL=0`
- 7A 预览/导出预检查回归：通过。
  - `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
  - `PASS=18 FAIL=0`
- 8A 只读 Mock 轻量化适配层回归：通过。
  - `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
  - `PASS=11 FAIL=0`
- `git diff --check`：通过。

## 12. 浏览器自测结果

已使用新登录会话完成页面自测：

- `/data-steward/assets/503/master-data/initialization`
  - 通过。
  - 点击“预览草案”后可见 `M1C 真实项目接入`、`真实 NAS 项目`、`草案证据与风险`、`catalog_only`。
- `/data-steward/assets/506/master-data/initialization`
  - 通过。
  - 点击“预览草案”后可见 `M1C 真实项目接入`、`真实 NAS 项目`、`草案证据与风险`、`模板骨架`。
- `/data-steward/assets/503/master-data/sections`
  - 通过。
  - 可见接入草案复核提醒。
- `/data-steward/assets/503/master-data/node-types`
  - 通过。
  - 可见接入草案复核提醒。
- `/data-steward/assets/503/master-data/deliverable-standard`
  - 通过。
  - 可见接入草案复核提醒。
- `/data-steward/assets/503/work/document-delivery`
  - 通过。
- `/data-steward/assets/503/work/drawing-delivery`
  - 通过。
- 390px 小屏初始化页检查通过。
- 页面可见文本未命中 `/Volumes/`、`/Users/`、`nas://`、`smb://`、`storage_path`、`storage_uri`、`storageUri`、`raw_path`、`root_path`、`nas_path`。

## 13. 真实项目抽查

- `503 / 105 / 启航华居项目`
  - `assetSource=NAS_REAL_PILOT`
  - `realNasProject=true`
  - 文件扩展名线索包含 `DWG / PDF / RVT`
  - 专业线索包含 `ARCHITECTURE / ELECTRICAL / FIRE_PROTECTION / GAS / GENERAL / HVAC / INTELLIGENT / PLUMBING`
  - raw `Volumes` 目录片段已脱敏为 `项目内目录线索已脱敏`
  - 主数据底座已就绪，草案预览为幂等跳过但仍展示证据与风险。
- `506 / 93 / 中建八局国交酒店项目`
  - `assetSource=NAS_REAL_PILOT`
  - `realNasProject=true`
  - 文件扩展名线索包含 `DWG / IFC / PDF / RVT`
  - 未确认应用时返回非 OK。
  - 草案预览同时体现 `TEMPLATE_SKELETON` 与 catalog 资产线索。

## 14. 已知风险与未完成事项

- M1C 只完成真实项目接入到工程主数据草案/人工确认/交付入口的 MVP，没有进入 8B / 8C / 9A。
- 真实工程结构仍需项目负责人复核；平台没有识别真实楼栋、楼层、系统和 BIM 构件。
- catalog metadata 仍不能替代 PDF/Office 正文、DWG 图层/标题栏、RVT Family/Type/构件参数证据。
- 前端主包体积 warning 为既有 P2，本轮未处理。
- smoke 脚本会创建隔离测试项目和目录级测试数据，未做自动清理，便于测试 agent 复核。

## 15. 是否建议测试 Agent 验收

建议进入测试 Agent 对 M1C 做验收。

重点请测试：

- 105/503 与 93/506 的 assessment / preview 是否满足 catalog-only 合同。
- 草案预览是否能清楚表达证据来源、置信度、风险提示和人工确认。
- 未确认时是否禁止 apply。
- 确认 apply 后是否幂等且不覆盖已有主数据。
- 前端是否没有 raw path 残留。
- 是否保持 G4/Hermes/8B/9A 冻结边界。

## 16. 给测试 Agent 的测试 Prompt

````md
# 测试 Agent Prompt：M1C 工程主数据真实项目落地验收

工作目录：`/Users/vc/Documents/数字化交付平台`

本轮只验收 `M1C：工程主数据真实项目落地`。

M1C 是 M1B 后的真实项目接入与工程主数据映射修复 MVP，不代表进入 8B / 8C / 9A，也不代表恢复 G4 / Hermes 开发。不要测试或推进 Hermes 新能力，不测试真实 NAS 写操作、正文解析、BIM 构件解析、parser/writer/indexing 或 production rollout。

必须先读：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1b-project-workbench-usability-closure.md`
- `handoff/main-agent/m1c-real-project-masterdata-plan.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

重点验收：

1. 真实项目接入评估接口
   - `GET /api/master-data/projects/503/onboarding/assessment`
   - `GET /api/master-data/projects/506/onboarding/assessment`
   - 应返回 `assetCatalogOnly=true`、`evidenceMode=catalog_only`。
   - 503 应对应项目编码 105，506 应对应项目编码 93。
   - 应返回 `assetSource`、`realNasProject=true`、文件数量、路径映射数量、扫描记录数量、主要扩展名、主要专业线索、脱敏目录线索。
   - 不得返回 raw NAS path、`storage_path`、`storage_uri`、SQL、raw DB row、secret/token/password。

2. 草案预览接口
   - `GET /api/master-data/projects/503/onboarding/preview?templateCode=MEP_BIM_BASIC`
   - `GET /api/master-data/projects/506/onboarding/preview?templateCode=MEP_BIM_BASIC`
   - 应返回 `dryRun=true`、`confirmedRequired=true`、`nasTouched=false`、`contentRead=false`、`assetCatalogOnly=true`、`evidenceMode=catalog_only`。
   - `draftItems` 每项应包含 `evidenceMode`、`evidenceSource`、`confidenceLevel`、`riskHint`、`pendingConfirmation=true`。
   - 506 草案应能看到模板骨架和资产目录线索并存。

3. 人工确认闸门与幂等
   - `POST /api/master-data/projects/506/onboarding/apply` 传 `confirmed=false` 必须失败。
   - 使用隔离测试项目验证 `confirmed=true` 后可应用草案。
   - 重复应用不应覆盖已有主数据，应表现为 created=0、skipped>0。
   - 应产生 `masterdata.initialization.template-apply` 审计事件。

4. 前端验收
   - 打开 `/data-steward/assets/503/master-data/initialization`。
   - 点击“预览草案”。
   - 应看到 `M1C 真实项目接入`、`真实 NAS 项目`、主要扩展名、主要专业线索、脱敏目录线索、`草案证据与风险`、`catalog_only`。
   - 打开 `/data-steward/assets/506/master-data/initialization` 并点击“预览草案”，应看到模板骨架与风险提示。
   - 打开：
     - `/data-steward/assets/503/master-data/sections`
     - `/data-steward/assets/503/master-data/node-types`
     - `/data-steward/assets/503/master-data/deliverable-standard`
   - 这些页面应显示草案复核提醒。
   - 打开：
     - `/data-steward/assets/503/work/document-delivery`
     - `/data-steward/assets/503/work/drawing-delivery`
   - 页面应正常进入，不应 403/404/500。

5. 安全边界
   - 前端可见文本和接口响应不得出现 raw `storage_path`、`storage_uri`、真实 NAS 路径、SQL、secret、token、password。
   - 不允许触发真实 NAS 文件创建、移动、删除、重命名、上传。
   - 不允许读取 PDF / Office / DWG / RVT / IFC 正文。
   - 不允许启用 parser / writer / indexing。
   - 不允许 Hermes 自动审批、自动整改、自动挂接或自动删除。

建议执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m1c-real-project-masterdata.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6a-project-initialization.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

测试报告需明确：

- M1C 是否通过。
- 105/503 与 93/506 是否都通过。
- 是否发现 raw path 或敏感字段残留。
- 是否发现未确认也能 apply。
- 是否发现重复应用覆盖已有主数据。
- 是否发现页面缺失草案证据/风险/复核提醒。
- 是否发现 G4/Hermes/8B/9A 边界被突破。
- 是否建议主 agent 判定 M1C 收口。
````

# M2D 真实项目工程主数据接入草案增强验收报告

生成时间：2026-05-22 17:04 CST

## 1. 测试结论

结论：通过。

M2D 已达到本轮验收目标：105 / 503 在工程主数据和交付标准被重置后仍保持“真实资产已接入、工程主数据未确认、交付标准未就绪”的状态；接入评估和草案预览只基于 catalog metadata，返回真实资产统计、证据来源、置信度、风险提示和人工确认标记；文档 / 图纸交付与交付包接口没有生成虚假应交项或虚假交付包。

当前未发现 P0 / P1。发现 3 个 P2，不阻塞 M2D 收口判断。

建议主 agent 收口 M2D。

## 2. 本轮范围和已读文档

本轮按 `handoff/test-agent/current-prompt.md` 执行，采用轻量测试策略，不做全量浏览器逐页回归，不进入 Hermes / G4 / 8B / 8C / 9A。

已阅读：

- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/project-105-template-reset-report.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`

重点确认：

- 105 / 503 文件资产保留，工程主数据和交付标准重置为空。
- 105 / 503 不应再被表现为“模板已套用完成”或“交付标准已就绪”。
- M2D 只做真实资产目录线索驱动的草案增强，不读取正文，不触碰 NAS，不生成真实交付包。

## 3. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2D 专项：`bash scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`，通过，`PASS=8 FAIL=0`。
- M1C 回归：`bash scripts/dev/check-m1c-real-project-masterdata.sh`，通过，`PASS=14 FAIL=0`。
- M2C 回归：`bash scripts/dev/check-m2c-delivery-package-archive.sh`，通过，`PASS=11 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

未按当前轻量策略额外全量复跑 M2B / M2A / M1F / M1E / M1D。

## 4. 105 / 503 接口抽查结果

### 标准与初始化状态

`GET /api/master-data/projects/503/standard-status` 返回成功，统一响应带 `traceId`，未发现 forbidden fields。关键状态：

- `deliverableStandardReady=false`
- `hasSectionTree=false`
- `hasNodeTypes=false`
- `nodeTypesLocked=false`

`GET /api/master-data/projects/503/initialization/status` 返回成功，统一响应带 `traceId`。关键状态：

- `ready=false`
- `currentStep=SECTION_TREE`
- blockers 包含：`尚未建立工程部位树`、`尚未配置节点类型`、`交付物标准尚未完整配置`

结论：105 / 503 未被错误标记为主数据或交付标准已就绪。

### 接入评估

`GET /api/master-data/projects/503/onboarding/assessment` 返回成功，统一响应带 `traceId`，未发现 forbidden fields。关键结果：

- `projectCode=105`
- `realNasProject=true`
- `evidenceMode=catalog_only`
- `fileCount=2928`
- 模型文件：`198`
- 图纸文件：`2729`
- 文档文件：`242`
- 表格文件：`1`
- 主要扩展名：DWG `2487`、PDF `242`、RVT `198`、XLSX `1`
- risks 数量：`2`
- Missing Evidence 包含：`ASSET_CATALOG_ONLY`、`MODEL_PARSE_EVIDENCE_MISSING`、`DRAWING_PARSE_EVIDENCE_MISSING`、`DOCUMENT_TEXT_EVIDENCE_MISSING`

结论：接入评估基于真实资产目录统计，不把 catalog metadata 伪装成正文、模型构件或图纸解析证据。

### 草案预览

`GET /api/master-data/projects/503/onboarding/preview?templateCode=MEP_BIM_BASIC` 返回成功，统一响应带 `traceId`，未发现 forbidden fields。关键结果：

- `dryRun=true`
- `confirmedRequired=true`
- `nasTouched=false`
- `contentRead=false`
- `evidenceMode=catalog_only`
- `draftItems` 数量：`42`
- 草案类别同时包含真实资产线索和模板骨架：`DISCIPLINE_CANDIDATE`、`DELIVERABLE_TYPE_CANDIDATE`、`TARGET_CANDIDATE`、`SECTION_NODE`、`NODE_TYPE`、`DELIVERABLE_DEFINITION`、`DELIVERABLE_TYPE`、`DELIVERABLE_ATTRIBUTE`、`DIRECTORY_TEMPLATE`
- 草案项包含：`evidenceSource`、`confidenceLevel`、`riskHint`、`pendingConfirmation=true`
- 示例草案项：`category=DISCIPLINE_CANDIDATE`、`evidenceSource=CATALOG_DISCIPLINE_DISTRIBUTION`、`confidenceLevel=HIGH`、`fromRealAssetClue=true`、`fromTemplateSkeleton=false`

结论：草案预览没有直接写库，没有触碰 NAS，且明确要求人工确认。

## 5. 应交项和交付包回归

`GET /api/work-center/projects/503/delivery-completeness?viewType=DOCUMENT&targetType=SECTION`：

- 返回成功，统一响应带 `traceId`。
- `missingCount=0`
- `rows=0`
- `nextActionCode=COMPLETE_STANDARD`
- `nextActionText=先补齐工程主数据和交付物标准，再生成应交项。`

`GET /api/work-center/projects/503/delivery-completeness?viewType=DRAWING&targetType=SECTION`：

- 返回成功，统一响应带 `traceId`。
- `missingCount=0`
- `rows=0`
- `nextActionCode=COMPLETE_STANDARD`
- `nextActionText=先补齐工程主数据和交付物标准，再生成应交项。`

`GET /api/work-center/projects/503/delivery-package/prepare?viewType=DOCUMENT&targetType=SECTION` 和 `DRAWING`：

- 均返回成功，统一响应带 `traceId`。
- `totalCount=0`
- `readyCount=0`
- `blockedCount=0`
- `missingCount=0`
- `dryRun=true`
- `physicalPackageGenerated=false`
- `nasFileCopied=false`
- `rows=0`

结论：105 / 503 在主数据和标准未确认前，没有产生虚假缺失项、虚假应交项或虚假交付包。

## 6. 浏览器轻量检查

按轻量测试策略仅抽查核心页面：

- `/data-steward/assets/503/master-data/initialization`

结果：

- 页面可打开，非白屏，非 404。
- 可见 `M2D 真实项目主数据草案`。
- 可见 `真实资产已接入，工程主数据未确认`。
- 可见“不通过一键模板直接变成就绪标准”的说明。
- 可见接入向导只读取 catalog metadata、不读取文件正文、不访问或复制 NAS 文件的说明。
- 页面展示 105 / 503 真实资产统计：文件 `2928`、模型 `198`、图纸 `2729`、文档 `242`、清单 `1`。
- 页面展示 DWG / PDF / RVT / XLSX 扩展名分布、专业线索、`catalog-only` 和 Missing Evidence 说明。
- 未发现页面把 105 表现为 `模板已就绪`、`标准已完成`、`交付标准已就绪` 或 `已完成交付标准`。

备注：本轮未做全量浏览器逐页、多视口、交互式点击验收，符合 `lightweight-test-strategy.md` 对 M2D 的要求。

## 7. forbidden field 与安全边界

接口响应与页面核心文本检查未发现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- raw row
- SQL
- token
- secret
- password

静态扫描 M2D 相关后端、前端和脚本，命中的敏感词仅出现在测试脚本的断言列表、脱敏检查函数或登录测试参数中，未发现实际泄露或真实文件操作调用。

本轮未发现：

- 真实 NAS 创建、移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- BIM 构件级解析、parser / writer / indexing。
- Hermes memory / OpenSearch / Qdrant / MinIO documents/chunks 写入。
- Agent 自动审批、自动整改、自动挂接。
- 真实交付包生成、复制或移动 NAS 文件。

## 8. 工作区状态备注

当前工作区仍有多批次既有改动和未跟踪文件，`git diff --check` 已通过。

M2D 预期相关改动包括：

- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/application/ProjectInitializationApplicationService.java`
- `backend/delivery-master-data/src/main/java/com/zhuoyu/delivery/masterdata/initialization/dto/InitializationDtos.java`
- `frontend/src/modules/master-data/api/masterData.ts`
- `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
- `scripts/dev/check-m2d-real-project-masterdata-onboarding.sh`

同时仍可见 M2A / M2B / M2C 既有改动、M2C 相关新增迁移，以及 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件。建议主 agent 收口提交时确认归属并排除非交付文件。

未发现本轮修改 `docs/**`。

## 9. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 既有 Vite chunk size warning，未阻塞前端构建。
- 本轮浏览器只按轻量策略抽查核心初始化页，未做全量逐页 / 多视口浏览器验收。
- 工作区仍存在 `.claude/**`、`CLAUDE.md`、`tmp/**` 非交付未跟踪文件，以及 M2A / M2B / M2C 既有改动；不影响 M2D 测试结论，但主 agent 收口提交时需要确认归属。

## 10. 是否建议主 agent 收口 M2D

建议主 agent 收口 M2D。

理由：后端构建、前端构建、健康检查、M2D 专项脚本、M1C 回归、M2C 回归和 `git diff --check` 均通过；105 / 503 保持标准未就绪和主数据未确认状态；assessment / preview 返回真实资产目录线索、catalog-only 证据、置信度、风险和人工确认标记；文档 / 图纸应交项和交付包没有虚假数据；未发现 raw path、敏感字段泄露或真实 NAS / 正文 / Hermes / indexing 越界行为。

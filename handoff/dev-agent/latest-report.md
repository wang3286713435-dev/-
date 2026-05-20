# 开发 Agent 报告：G2-B 既有真实项目治理可用性补丁

时间：2026-05-20

## 1. 本轮目标

本轮按 `G2-B：既有真实项目治理可用性补丁` 收口 G2 尾项：重排资产总览 Hero 区、强化真实项目治理路径、补齐 Hermes 常驻入口 MVP、修复历史 smoke 项目分类。

命名保持 `G2 / G2-B`，未进入 8B / 8C / 9A，未新增 H1/R1/A9 等临时命名。

## 2. 修改文件

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/HermesGatewayDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/AgentGatewayApplicationService.java`
- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`
- `scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移，未修改旧 Flyway migration。

## 3. Hero 区与父子结构

`/data-steward/assets` 顶部改为四层治理 Hero：

1. 平台目标：真实项目接入、工程主数据准备、交付治理闭环。
2. 项目状态：真实 NAS 项目、已登记资产、已初始化主数据、已进入交付治理、待接入 / 待治理。
3. 下一步动作：进入真实项目、查看接入评估、完善工程主数据、进入交付治理助手。
4. 风险提醒：缺工程主数据、缺交付标准、待审核、缺 checksum、低置信度。

页面仍是业务工作台，不是营销页；结构按“目标 -> 状态 -> 动作 -> 风险”组织，浏览器验证未出现横向撑爆。

## 4. 通用真实项目治理路径

资产总览表格新增治理路径列，按项目状态展示：

`资产目录 -> 接入评估 -> 工程主数据草案 -> 交付治理助手 -> 缺失项解释 -> 人工确认挂接`

每行会显示当前步骤、下一步动作和原因提示。逻辑基于 `onboardingStatus / hasMasterData / hasDeliveryStandard / governanceReady` 等通用字段，没有写死 105、503 或“启航华居项目”。

浏览器验证：

- 代码 `105` 的真实项目对应 `projectId=503`，可进入项目工作台、接入向导和交付治理助手。
- 另一个真实项目 `projectId=506 / code=93 / 中建八局国交酒店项目` 也可进入同一套项目工作台和 Hermes 常驻入口。

## 5. Hermes 常驻入口

`AppLayout.vue` 增加右下角 Hermes 常驻入口，覆盖：

- 资产总览。
- 项目工作台。
- 真实项目接入向导。
- 交付治理助手。

入口打开 `DataStewardPanel`，仍走平台后端 `/api/data-steward/chat`，前端不直连 Hermes。

Hermes 请求新增并透传：

- `currentRoute`
- `projectId`
- `projectCode`
- `projectName`
- `pageType`
- `pageTitle`

后端 Gateway 在 `AgentGatewayApplicationService.outboundRequest` 中把这些字段写入 `page_context`，并继续由后端生成 / 校验 `project_scope` 和权限证明，不信任前端伪造项目范围。

外部 Hermes 调用不可用时，已通过平台本地 catalog-only 兜底回答页面用途、下一步动作、项目路径提示或 Missing Evidence，不白屏、不 500。

## 6. Catalog Layer 边界

本轮只强化 Hermes Catalog Layer：

- 目录级项目路径自然语言查询。
- 页面用途与下一步动作解释。
- catalog-only 资产目录辅助。
- Missing Evidence 解释。

未触碰 Evidence Layer、Memory Layer、Orchestration Layer：

- 未实现 `document_evidence_search`。
- 未做 PDF / Office 正文问答。
- 未做 DWG / RVT / BIM 内容理解。
- 未做 BIM 构件参数检索。
- 未写 Hermes memory。
- 未做多 Agent 编排或自动治理。

代码级安全保持：

- 不读文件正文。
- 不做 parser / writer / indexing。
- 不写 OpenSearch / Qdrant / MinIO documents/chunks。
- 不做 Agent DB CRUD。
- 不做 Hermes 写操作。
- 不触碰真实 NAS 文件。

## 7. smoke 项目分类修复

`BimAssetRepository` 修复项目分类：

- `B6A-SMOKE-*`
- `PHASE2-*`
- `PH2*`
- 包含 `SMOKE`
- 包含 `TEST`
- 包含 `测试`

这些项目在全量视图归为 `projectSource=TEST`、`projectCategory=TEST_PROJECT`。`assetSource=NAS_REAL*` 默认真实项目查询会排除这些历史测试项目。

G2 脚本已增强断言：

- 默认真实项目至少两个，且不混入测试 / 样例。
- 全量视图中的历史 smoke 命名必须归为测试分类。
- 前端包含 Hero 文案、治理路径、Hermes 常驻入口和上下文透传。

## 8. 路径脱敏自查

项目工作台中：

- 路径映射列表不再展示 `nasPath`，改为 `NAS/PREFIX/底层路径已隐藏` 形式。
- 文件详情不再展示 `selectedFile.storagePath` 真值，只提示使用受控预览 / 下载入口。
- 最近扫描路径展示改为状态提示，不显示 `lastScannedPath` 真值。

接口验证：

- `projectId=503 / code=105` 问“这个项目路径在哪里？”返回 `displayPath=项目目录：启航华居项目`、`pathHint=NAS/PREFIX/底层路径已隐藏`。
- 响应未发现 `nas://`、`smb://`、`/Volumes/`、`/Users/`、`storage_path/storage_uri/storagePath/storageUri`、raw row、SQL、secret/token/password 真值。

## 9. 自测命令结果

- 后端构建：通过。
  - `./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过。
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 健康检查：通过。
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - `{"status":"UP"}`
- G2 / G2-B 专项脚本：通过。
  - `bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
  - `PASS=11 FAIL=0`
- Hermes Gateway 脚本：通过。
  - `bash scripts/dev/check-hermes-jarvis-gateway.sh`
  - `PASS=13 FAIL=0`
- G1 交付治理回归：通过。
  - `bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`
  - `PASS=34 FAIL=0`
- 8A BIM 轻量化只读适配回归：通过。
  - `bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`
  - `PASS=11 FAIL=0`
- `git diff --check`：通过。

## 10. 浏览器回归结果

通过 Codex in-app browser 验证：

- `/data-steward/assets`
  - Hero 区父子结构清晰，包含平台目标、项目状态、下一步动作、风险提醒。
  - 默认真实项目表格 16 行，未发现 `B6A-SMOKE / PHASE2 / PH2 / SMOKE / TEST / 测试项目` 行混入。
  - Hermes 常驻入口可见并可打开。
- 代码 `105` 真实项目
  - `projectId=503 / 启航华居项目` 可进入项目工作台。
  - 工作台可见 Hermes 入口，无 raw path 泄露。
- 另一个真实 NAS 项目
  - `projectId=506 / code=93 / 中建八局国交酒店项目` 可进入项目工作台。
  - 同样可见 Hermes 入口，无 raw path 泄露。
- 真实项目接入向导
  - `/data-steward/assets/503/master-data/initialization`
  - 可见“真实项目接入向导”和“模板内容只是草案”文案。
  - Hermes 常驻入口可见，无 raw path 泄露。
- 交付治理助手
  - `/data-steward/assets/503/work/agent-governance`
  - 可见缺失项解释、人工确认挂接语义。
  - Hermes 常驻入口可见，无 raw path 泄露。

## 11. 禁止项确认

- 未进入 8B / 8C / 9A。
- 未触碰真实 NAS 文件。
- 未创建、移动、删除、重命名或上传 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未做 BIM 构件级解析。
- 未做 selective indexing。
- 未写 Hermes memory。
- 未写 OpenSearch / Qdrant / MinIO documents/chunks。
- 未让 Agent 自动审批、自动整改或自动创建真实交付结论。
- 前端未直连 Hermes。
- 未为 105、503、启航华居项目写死业务逻辑。

## 12. 已知风险与未完成事项

- Hermes 外部服务如偶发不可用，平台会走本地 catalog-only 兜底；这属于安全降级，不代表外部 Hermes production rollout。
- `AssetProjectDetailPage.vue` 仍保留 checksum 后台任务入口，这是既有功能，不属于 G2-B 新增能力；本轮未触发真实 NAS 文件读取任务。
- 当前 worktree 仍包含大量历史/其他批次改动和未跟踪文件，本轮未回退这些既有改动。

## 13. 给测试 Agent 的测试 Prompt

````md
# 测试 Agent Prompt：G2-B 既有真实项目治理可用性补丁验收

工作目录：`/Users/vc/Documents/数字化交付平台`

本轮只验收 `G2-B：既有真实项目治理可用性补丁`。仍属于 G2，不进入 8B / 8C / 9A，不测试真实 BIM 轻量化，不测试真实 NAS 增删改查，不测试正文抽取、selective indexing 或 Agent 自动治理。105 只是验收样本，不能作为硬编码能力。

必须先读：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
- `handoff/main-agent/phase2-g2-naming-freeze.md`
- `handoff/main-agent/hermes-layered-integration-decision.md`
- 共享文档中的 Hermes capability handoff、platform_to_hermes_contract、gateway_response_contract、missing_evidence_policy

重点验收：

1. `/data-steward/assets`
   - Hero 区说明平台目标：真实项目接入、工程主数据准备、交付治理闭环。
   - Hero 区展示项目状态、下一步动作和风险提醒。
   - 页面无横向撑爆，文案能让普通员工理解入口作用。
2. 通用真实项目治理路径
   - 验证代码 `105` 的真实项目，并再选至少一个其他真实 NAS 项目。
   - 两个项目都应看到同一套路径：`资产目录 -> 接入评估 -> 工程主数据草案 -> 交付治理助手 -> 缺失项解释 -> 人工确认挂接`。
   - 不得发现 105 / 503 / 启航华居项目硬编码。
3. Hermes 常驻入口
   - 在资产总览、项目工作台、真实项目接入向导、交付治理助手均可见并可打开。
   - 请求必须走平台后端 Gateway，不允许前端直连 Hermes。
   - 提问应带当前路由、当前项目 ID、项目编码/名称、页面类型。
   - Hermes 外部不可用时必须安全降级，不白屏、不 500。
4. Hermes 边界
   - 当前只做 Catalog Layer。
   - 正文 / DWG / RVT / BIM / 构件类问题仍返回 Missing Evidence。
   - 不得实现 Evidence / Memory / Orchestration 层能力。
   - 不得写 Hermes memory、OpenSearch、Qdrant、MinIO。
5. smoke 项目分类
   - 全部视图中 `B6A-SMOKE-* / PHASE2-* / PH2* / SMOKE / TEST / 测试` 归为测试项目。
   - 默认真实项目视图不得混入这些项目。
6. 安全扫描
   - 不得暴露 raw `storage_path`、`storage_uri`、NAS 原始路径、raw DB row、SQL、secret/token/password、DWG/RVT 内部内容、PDF/Office 正文内容。

必须执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh
bash scripts/dev/check-hermes-jarvis-gateway.sh
bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
git diff --check
```

报告写入 `handoff/test-agent/latest-report.md`，必须包含：

- 测试结论。
- P0 / P1 / P2 列表。
- 必跑命令结果。
- Hero 区验收结果。
- 105 项目和另一个真实 NAS 项目的治理路径验收结果。
- Hermes 常驻入口和上下文验收结果。
- Hermes 分层边界验收结果。
- smoke 项目分类验收结果。
- 禁止项检查结果。
- 是否建议主 agent 收口 G2-B。
- 是否建议整个 G2 收口并进入 Git checkpoint。
````

<promise>PHASE2_G2B_EXISTING_PROJECT_GOVERNANCE_USABILITY_COMPLETE</promise>

## 14. 主 Agent 热修：Hermes 真实 Agent 回复与兜底误判修复

时间：2026-05-20

### 问题

- 平台 `/api/data-steward/hermes/health` 只检查了 Hermes `/health`，该接口不校验 token，因此曾出现“health 正常但真实聊天接口 401”的假阳性。
- `/api/data-steward/chat` 在外部 Hermes 聊天接口 401 后静默退回本地 catalog-only 兜底，导致用户任意提问都看到固定安全话术。
- 平台没有把安全的项目目录摘要传给 Hermes，导致真实 Agent 即使接通，也无法回答“这个项目有哪些已登记文件”这类目录级问题。
- 前端回答卡片没有明确区分“真实 Hermes 回答”和“平台安全兜底”。

### 修复

- 更新 Hermes health 探活：openai-compatible 模式下除 `/health` 外，还必须用 service token 验证 `/v1/models`，避免聊天认证失败却显示已接入。
- 修正本机安全存储中的 Hermes service token，并重启平台后端。
- 后端 Gateway 向 Hermes `page_context` 增加安全的 `catalog_summary`：
  - 当前授权项目文件总数。
  - 文件类型分布。
  - 最多 8 条最近/关键词匹配文件样例。
  - 仅包含文件 ID、文件名、类型、扩展名、专业、版本、状态、大小桶等目录级字段。
  - 不包含真实 NAS 路径、storage 字段、raw row 或正文内容。
- 外部 Hermes 正常回复时，trace 标记为 `openai_compatible_catalog_only`。
- 前端回答卡片显示：
  - `真实 Hermes 回答`
  - 或 `平台安全兜底`
- 普通目录级问题不再强行显示正文证据缺失提示；正文 / DWG / RVT / BIM / 构件类问题仍返回 Missing Evidence。
- Hermes 专项脚本增强：当 `EXPECT_HERMES_AGENT_AVAILABLE=true` 时，必须验证真实外部 Hermes 回答，不允许本地固定兜底冒充通过。

### 验证

- `./mvnw -pl delivery-app -am -DskipTests package`：通过。
- `corepack pnpm --dir frontend build`：通过，仅保留既有 Vite chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过。
- `EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh`：通过，`PASS=13 FAIL=0`。
- 真实问题抽查：
  - `你好`：返回真实 Hermes 自然语言问候和可协助事项，`trace.agentMode=openai_compatible_catalog_only`。
  - `这个项目有哪些已登记文件？`：返回 105 / 503 项目的文件总数和类型分布，`trace.agentMode=openai_compatible_catalog_only`。
  - `这张 DWG 图纸里有哪些设备？`：返回 Missing Evidence，不编造图纸内容，`trace.agentMode=catalog_only`。

### 当前结论

- 平台 Hermes 已从“本地规则兜底为主”修正为“真实外部 Hermes 回复为主，平台安全兜底为备”。
- 仍保持 catalog-only / read-only / permission-aware 边界。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 Hermes memory、OpenSearch、Qdrant、MinIO。
- 未触碰真实 NAS 文件。

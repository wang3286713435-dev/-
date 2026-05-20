# 开发 Agent 当前任务：G2-B 既有真实项目治理可用性补丁

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

本轮是 G2 的收尾补丁：

`G2-B：既有真实项目治理可用性补丁`

命名冻结：

- 仍属于 `G2`。
- 不新增 `H1 / R1 / A9 / 9A` 等临时命名。
- 不进入 8B / 8C / 9A。
- G2-B 通过后，整个 G2 应收口，后续再由用户决定是否恢复 8B / 8C / 9A。

## 0. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/phase2-g2-naming-freeze.md`
5. `handoff/main-agent/phase2-insert-g2-real-project-onboarding-masterdata-mapping-plan.md`
6. `handoff/main-agent/phase2-g2b-existing-project-governance-usability-plan.md`
7. `handoff/main-agent/hermes-layered-integration-decision.md`
8. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/agent-briefings/hermes_capability_handoff.md`
9. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/platform_to_hermes_contract.md`
10. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/gateway_response_contract.md`
11. `/Users/vc/Library/Mobile Documents/com~apple~CloudDocs/数字化交付平台/DigitalDeliveryProject/integration-contracts/missing_evidence_policy.md`
12. `handoff/dev-agent/latest-report.md`
13. `handoff/test-agent/latest-report.md`

重点检查：

1. `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
2. `frontend/src/modules/data-steward/components/DataStewardPanel.vue`
3. `frontend/src/modules/data-steward/components/DataStewardAnswerCard.vue`
4. `frontend/src/modules/data-steward/api/dataSteward.ts`
5. `frontend/src/modules/core/layout/AppLayout.vue`
6. `frontend/src/router/index.ts`
7. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/hermes/*`
8. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
9. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 1. 背景

G2-A 已通过测试 agent 验收，当前无 P0/P1。

G2-A 已解决：

- 真实 NAS 项目分类。
- 真实项目接入向导。
- 模板草案 / 待确认语义。
- 105 项目识别为真实 NAS 项目。
- Hermes project-level 问答、Missing Evidence 和路径脱敏。

但资产总览 Hero 区和入口结构仍不够清楚，普通员工无法快速理解：

- 平台现在管理什么。
- 当前真实项目处于什么状态。
- 下一步该点哪里。
- 每个入口有什么用。

本轮不是新增大功能，而是把既有真实项目治理路径讲清楚、组织清楚、入口清楚。

## 2. 核心原则

105 项目只是验收样本，不允许硬编码。

所有能力必须适用于已接管的真实 NAS 项目，包括但不限于：

- `93`
- `96`
- `97`
- `100`
- `101`
- `104`
- `105`
- `108`
- `109`
- `110`
- `111`
- `112`
- `113`
- `114`
- `115`
- `116`

禁止在前端或后端写死：

- `105`
- `503`
- `启航华居项目`

如需测试样本，只能在脚本或测试报告中使用，不能作为业务逻辑条件。

## 3. 本轮必须完成

### Hermes 分层接入边界

本轮必须按共享文档的四层模型理解 Hermes：

`Catalog Layer -> Evidence Layer -> Memory Layer -> Orchestration Layer`

G2-B 只做 Catalog Layer 强化：

- 常驻入口。
- 当前项目 / 当前页面上下文。
- catalog-only 问答。
- 页面用途和下一步动作解释。
- Missing Evidence 解释。

G2-B 不做：

- Evidence Layer 的 `document_evidence_search`。
- PDF / Office 正文问答。
- DWG / RVT / BIM 内容理解。
- BIM 构件参数检索。
- Memory Layer 的长期记忆写入。
- Orchestration Layer 的多 Agent 编排或自动治理。

如需要做低敏 feedback / related_file_ids 展示，只能作为 UI 预留或复用已有合规接口；不得把 raw path、raw catalog row、文件正文或客户敏感内容写入 Hermes memory。

### A. 资产总览 Hero 区重排

重排 `/data-steward/assets` 的 Hero / 顶部概览区。

目标是让用户一眼看懂：

1. 这里是做真实 NAS 项目数字化交付治理的入口。
2. 现在有多少真实项目。
3. 哪些项目已登记资产。
4. 哪些项目已初始化主数据。
5. 哪些项目已进入交付治理。
6. 哪些项目还有风险。
7. 下一步应该做什么。

Hero 区建议形成四层结构：

第一层：平台目标

- 真实项目接入。
- 工程主数据准备。
- 交付治理闭环。

第二层：项目状态

- 真实 NAS 项目。
- 已登记资产。
- 已初始化主数据。
- 已进入交付治理。
- 待接入 / 待治理。

第三层：下一步动作

- 进入真实项目。
- 查看接入评估。
- 完善工程主数据。
- 进入交付治理助手。

第四层：风险提醒

- 缺工程主数据。
- 缺交付标准。
- 待审核。
- 缺 checksum。
- 低置信度。

要求：

- 不做营销页。
- 不做复杂大屏。
- 不堆技术字段。
- 入口文案必须说明“这个功能有什么用”。
- 父子结构必须比现在清晰。
- 页面不能横向撑爆。

### B. 真实项目治理路径

在资产总览和项目卡片 / 表格中强化通用治理路径：

`资产目录 -> 接入评估 -> 工程主数据草案 -> 交付治理助手 -> 缺失项解释 -> 人工确认挂接`

对每个真实 NAS 项目，用户应能看懂：

- 当前在哪一步。
- 下一步是什么。
- 为什么要做。
- 缺什么。

不能只为 105 展示。至少另一个真实 NAS 项目也必须看到同样结构。

### C. Hermes 常驻入口 MVP

Hermes 从局部页面组件升级为平台常驻入口 MVP。

要求：

- 在核心工作区提供常驻入口，例如右下角浮窗或全局按钮。
- 至少覆盖：
  - 资产总览。
  - 项目工作台。
  - 真实项目接入向导。
  - 交付治理助手。
- 常驻入口可打开 Hermes 面板。
- Hermes 请求仍走平台后端 Gateway。
- 前端不直连 Hermes。

Hermes 提问时必须带当前上下文：

- 当前路由。
- 当前项目 ID。
- 当前项目编码 / 名称。
- 当前页面类型。

Hermes 至少能回答或引导：

- 这个页面是干什么的？
- 我下一步应该做什么？
- 当前项目处于哪一步？
- 为什么模板只是草案？
- 工程主数据缺什么？
- 交付治理助手能做什么？
- 哪些资料还缺证据？

如果 Hermes 外部服务不可用，必须安全降级，不白屏、不 500。

### D. 修正历史 smoke 项目分类

测试报告指出：

- “全部”筛选中历史 `B6A-SMOKE-*` 项目仍显示为“手工/API”而不是“测试”分类。

本轮应修复分类规则：

- `B6A-SMOKE-*`
- `PHASE2-*`
- `PH2*`
- `SMOKE`
- `TEST`
- `测试`

这类项目在全部视图中应归为测试项目。

默认真实项目视图不得混入这些项目。

## 4. 明确禁止

本轮禁止：

1. 修改 `docs/**`。
2. 进入 8B / 8C / 9A。
3. 真实 NAS 增删改查。
4. 文件移动、删除、重命名、上传。
5. 读取 PDF / Office / DWG / RVT / IFC 正文。
6. BIM 构件级解析。
7. selective indexing。
8. 写 Hermes memory。
9. 写 OpenSearch / Qdrant / MinIO documents/chunks。
10. Agent 自动审批。
11. Agent 自动整改。
12. Agent 自动创建真实交付结论。
13. 前端直连 Hermes。
14. 为 105 写死特殊逻辑。
15. 实现或伪实现 Evidence Layer / Memory Layer / Orchestration Layer。

## 5. 必做验证

至少执行：

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

如修改了 G2 脚本，应增强它覆盖：

- Hero 区核心文案或 API 支撑字段。
- smoke 项目分类。
- 至少两个真实 NAS 项目治理入口。
- Hermes 常驻入口对应的前端结构或 API 上下文。

## 6. 手动回归

至少用浏览器验证：

1. `/data-steward/assets`
   - Hero 区父子结构清晰。
   - 用户能看懂平台目标、项目状态、下一步动作和风险提醒。
   - 默认真实项目视图不混入测试项目。
2. 105 项目
   - 可以作为样本进入治理路径。
   - 不出现 105 专属硬编码痕迹。
3. 另一个真实 NAS 项目
   - 也能看到同样的治理路径和入口。
4. Hermes 常驻入口
   - 资产总览可打开。
   - 项目工作台可打开。
   - 接入向导可打开。
   - 交付治理助手可打开。
   - 能带当前项目和页面上下文。
5. smoke 项目分类
   - “全部”视图里 `B6A-SMOKE-*` 归为测试项目。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 修改文件。
2. Hero 区如何重排。
3. 父子结构如何表达。
4. 通用真实项目治理路径如何体现。
5. 105 是否只是样本，有无硬编码。
6. 另一个真实 NAS 项目验证结果。
7. Hermes 常驻入口实现方式。
8. Hermes 如何携带当前项目 / 当前页面上下文。
9. 本轮如何保持 Catalog Layer 边界。
10. 是否触碰 Evidence / Memory / Orchestration，答案必须为否。
11. smoke 项目分类修复。
12. 自测命令结果。
13. 禁止项确认。
14. 未完成事项。

## 8. 完成定义

只有同时满足以下条件，才能标记完成：

- 资产总览 Hero 区看得懂。
- 真实项目治理路径对所有真实 NAS 项目通用。
- 105 只是样本，没有被硬编码。
- 至少另一个真实项目也可走同样入口。
- Hermes 常驻入口可用。
- Hermes 不直连、不写库、不自动治理。
- smoke 项目分类修复。
- 构建和回归脚本通过。
- `handoff/dev-agent/latest-report.md` 已写。

完成承诺：

`<promise>PHASE2_G2B_EXISTING_PROJECT_GOVERNANCE_USABILITY_COMPLETE</promise>`

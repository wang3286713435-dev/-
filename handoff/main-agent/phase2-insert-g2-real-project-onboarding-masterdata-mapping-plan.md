# 二期插入批次 G2：真实项目接入与工程主数据映射修复 MVP

更新时间：2026-05-19

## 1. 主 Agent 判断

当前二期主线已经具备真实 NAS 资产目录、项目工作台、标准驱动交付、批量挂接、交付治理助手、Hermes catalog-only 网关和 8A BIM Mock 入口。

但用户最新反馈指出一个更底层的产品契约问题：工程主数据初始化仍偏“模板套用”，没有充分从真实 NAS 项目资产、目录结构、文件元数据和交付目标出发。这样会让页面看起来已经具备部位树和交付标准，但这些数据并不一定能代表真实项目。

因此，当前暂停 `G1-P2 测试数据自清理` 小修复，不进入 8B，不开放真实 NAS 增删改查，临时插入高优先级批次：

`G2：真实项目接入与工程主数据映射修复 MVP`

本批目标不是新增大而全功能，而是修正真实项目接入、项目分类、初始化向导和 Hermes 交付治理上下文四条关键契约。

## 1.1 命名冻结

本批命名固定为：

`G2 = 真实项目接入与工程主数据映射修复 MVP`

G2 是 `G1 Agent 引导式交付治理 MVP` 之后的真实项目接入纠偏批次。

G2 不代表 9A 已完成，也不代表进入了 9A 后续治理。`8B / 8C / 9A` 均尚未正式进入。

不再新增 `H1` / `R1` 等临时命名，避免进一步混乱。

后续所有 prompt、报告、脚本、状态文档必须统一使用 G2；历史回归脚本保留原批次命名，仅作为回归依赖。

## 2. 当前基线

已确认的可用基线：

- 资产总览当前已支持 `REAL_NAS` 默认筛选，但项目分类信息仍不够清晰。
- 项目初始化页面当前仍以模板预览/套用为主。
- G1 交付治理助手已可显示项目体检、缺失项、推荐挂接并要求人工确认。
- Hermes Gateway 已完成 catalog-only 路径查询脱敏收口：
  - Gateway 生成可信 project scope。
  - 不返回 raw `nasPath`、`storage_path`、`/Volumes`、`smb://`。
  - 正文、BIM、构件类问题已统一收敛为 Missing Evidence。
  - 最新 dev/test 报告均显示 Hermes catalog-only 前端路径查询通过。

仍需修复的契约问题：

- 真实项目、样例项目、测试项目、历史/归档项目在资产总览中仍不够易懂。
- 初始化向导容易让用户理解成“套模板即可完成真实工程主数据”。
- 模板创建的数据未充分表达“草案 / 待确认”，容易被误认为真实结构。
- 交付治理助手的 Hermes 面板需要更清楚地区分权限拒绝、缺正文证据和 catalog-only 辅助回答。
- 前端不应直接展示裸 requestId，应改成折叠的“诊断编号”。

## 3. 本批目标

让真实 NAS 项目可以被平台明确识别、分类、接入、评估，并进入工程主数据初始化和交付治理助手。

目标体验：

1. 用户进入资产总览后，默认看到真实 NAS active 项目，而不是测试/样例/历史项目混在一起。
2. 用户能一眼看出项目来源、接入状态、资产数量、是否已有工程主数据、是否已有交付标准、是否可进入交付治理。
3. 初始化向导改为“真实项目接入向导”，先选择真实资产项目并查看接入评估，再人工确认项目类型和交付目标。
4. 部位树、节点类型、交付物标准只能基于 metadata/catalog 形成草案或待确认推荐，模板只能作为辅助骨架。
5. 交付治理助手在当前项目上下文中构建权限证明，有项目权限时不能误报权限拒绝。
6. Hermes 正文类问题继续 Missing Evidence；目录级问题可以 catalog-only 辅助回答。
7. 不直接展示裸 requestId；如需排查，用折叠“诊断编号”展示。

## 4. 资产总览项目归类

资产总览必须让用户区分：

- 真实 NAS 项目。
- 样例 / 模板项目。
- 测试项目。
- 历史 / 归档项目。
- 未完成接入项目。

默认筛选：

- 默认展示 `真实 NAS 项目 + active 项目`。
- 测试、样例、模板、归档项目默认隐藏，但可通过筛选查看。

项目卡片或列表至少展示：

- 项目来源。
- 接入状态。
- 文件数量。
- 主要文件类型。
- 最近扫描时间。
- 是否已有工程主数据。
- 是否已有交付标准。
- 是否可进入交付治理。

建议接入状态：

- `未接入`：项目存在，但没有有效 NAS 文件元数据或路径映射。
- `已登记资产`：已有真实文件资产或模型/图纸登记。
- `已初始化主数据`：已有部位树、节点类型或交付物标准。
- `已进入交付治理`：已有交付缺失项、推荐或挂接记录。

分类规则应优先用现有字段和现有事实推断。若需要新增字段或 View/API，必须保持小范围，并追加 Flyway 迁移，不改旧迁移。

## 5. 初始化向导重构

当前“初始化向导”要升级为：

`真实项目接入向导`

推荐流程：

1. 选择真实资产项目。
2. 查看该项目已登记资产概况。
3. 平台基于目录和文件元数据生成接入评估。
4. 用户确认项目类型和交付目标。
5. 系统推荐部位树草案、节点类型、交付物标准。
6. 用户人工确认后创建工程主数据。
7. 创建完成后进入交付治理助手。

关键约束：

- 当前阶段只允许基于 metadata/catalog 推断。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不解析 BIM 构件。
- 不把模板骨架包装成真实项目结构。
- 证据不足时，应显示“证据不足 / 待确认”，而不是自动生成大量看似完整的节点。
- 模板套用仍可保留，但要降级为辅助步骤，并清楚标识“草案 / 待确认”。

## 6. 部位树与标准草案口径

部位树不能再被用户理解为“模板树等于真实工程结构”。

推荐口径：

- 如果文件名、目录、路径中有建筑、楼栋、楼层、系统、专业等线索，可生成映射建议。
- 如果只有 NAS 目录结构，只能生成待确认草案。
- 如果证据不足，不自动生成大量真实节点。
- 模板只能提供行业默认骨架，例如“建筑机电 BIM 基础模板”。
- 模板生成的数据在 UI 中必须标注“草案 / 待确认”。

如当前数据库没有草案字段，优先在前端和接口响应中表达该状态；若确实需要持久化，必须做追加迁移并保持兼容。

## 7. Hermes 与交付治理助手边界

Hermes 仍保持：

- catalog-only。
- read-only。
- permission-aware。
- Missing Evidence。
- draft-only operation plan。

本批需要修复：

- Hermes 请求使用当前项目工作台可信 `projectId`。
- 项目级问题不强制要求 `assetId`。
- 用户对项目有权限时，不误报“权限拒绝”。
- 缺正文证据时显示“缺少正文证据”，不要混合显示权限拒绝。
- 目录级问题可以返回 catalog-only 辅助答案。
- 前端不直接展示裸 requestId，改为“诊断编号”，默认折叠或弱化。
- 面板清楚说明：当前只能基于资产目录、文件名、扩展名、项目权限和交付标准状态回答。

绝对禁止：

- 前端直连 Hermes。
- Hermes 直接查库或生成 SQL。
- Agent DB CRUD。
- Agent 读取文件正文。
- Agent 写 memory、OpenSearch、Qdrant、MinIO documents/chunks。
- Agent 自动审批、自动整改、自动创建真实交付结论。

## 8. 建议接口范围

优先复用现有后端接口。若现有接口不足，可新增小范围只读或确认式接口：

- `GET /api/data-steward/assets/projects`
  - 增强项目分类、接入状态、主数据/标准状态字段。
- `GET /api/master-data/projects/{projectId}/onboarding/assessment`
  - 返回真实项目接入评估。
- `GET /api/master-data/projects/{projectId}/onboarding/preview`
  - 返回基于 metadata/catalog 的草案预览。
- `POST /api/master-data/projects/{projectId}/onboarding/apply`
  - 用户确认后应用草案。
  - 必须要求 `confirmed=true`。
  - 不得读取正文，不得触碰 NAS。
- `POST /api/data-steward/chat`
  - 保持 Gateway 统一处理权限、脱敏、Missing Evidence 与诊断编号。

接口必须：

- 校验当前用户项目权限。
- 使用统一响应和 traceId。
- 不返回真实 NAS 绝对路径。
- 不返回 raw row、SQL、token、cookie、密码。
- 写动作必须审计。

## 9. 禁止范围

本批不做：

- 真实 NAS 增删改查。
- 文件移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- BIM 构件级解析。
- selective indexing。
- 写 Hermes memory。
- 写 OpenSearch / Qdrant / MinIO documents/chunks。
- Agent 自动审批。
- Agent 自动整改。
- Agent 自动创建真实交付结论。
- 前端直连 Hermes。
- 面向客户生产级权限体系承诺。

## 10. 验收标准

必须同时满足：

1. 资产总览默认不再混杂测试 / 样例 / 真实项目。
2. 用户能清楚看到哪些项目是真实 NAS 项目。
3. 初始化向导变成真实项目接入向导，不再只是模板套用。
4. 模板创建的数据标识为草案 / 待确认，不表现为真实项目已完成初始化。
5. 交付治理助手进入项目后，Hermes 不对有权限项目误报权限拒绝。
6. Hermes 正文类问题仍返回 Missing Evidence。
7. Hermes 目录级问题可返回 catalog-only 辅助回答。
8. 页面不直接暴露裸 requestId。
9. 所有推荐挂接仍需人工确认。
10. 不触碰真实 NAS 文件。
11. 后端构建、前端构建、Hermes Gateway 脚本、G1 交付治理脚本通过。

## 11. 专项脚本

建议新增或增强：

- `scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`
- `scripts/dev/check-hermes-jarvis-gateway.sh`
- `scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`

覆盖：

- 真实项目分类筛选。
- 初始化向导不默认生成脱离真实项目的数据。
- 模板数据标识草案 / 待确认。
- Hermes project-level 权限证明。
- catalog-only 回答。
- 正文问题 Missing Evidence。
- 不泄露 NAS 路径、token、SQL、raw row、裸 requestId。

## 12. 收口条件

开发 agent 完成后必须写入：

`handoff/dev-agent/latest-report.md`

测试 agent 完成后必须写入：

`handoff/test-agent/latest-report.md`

主 agent 只有在以下条件满足时才收口：

- 本批专项脚本通过。
- Hermes Gateway 回归通过。
- G1 交付治理回归通过。
- 8A BIM Mock 入口不回归。
- 无 P0/P1。
- 所有禁止项均未触碰。

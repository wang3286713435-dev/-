# 二期批次三规划：人工审核、整改闭环与基础报表导出

更新时间：2026-05-14 13:32 CST

## 1. 结论

二期批次三建议正式命名为：

`人工审核、整改闭环与基础报表导出`

本批目标不是做完整 BPM 工作流、客户生产级权限体系、Agent 自动审批或模型轻量化，而是在二期批次二“标准驱动交付完整率”基础上，补齐一个最小但可验收的人工闭环：

`交付挂接 -> 人工审核 -> 驳回产生整改 -> 整改处理 -> 复审关闭 -> 报表导出 -> 审计留痕`

## 2. 与前置批次的关系

已完成能力：

1. 一期内部 BIM/CAD 资产库与真实 NAS 元数据接管。
2. 二期批次一只读资产目录、权限证明和 Agent preview。
3. 二期批次二标准驱动交付完整率、缺失项和快捷挂接。

本批只补齐当前缺口：

1. 已挂接资料目前只有 `reviewStatus` 展示，缺少人工审核动作。
2. 审核驳回后缺少明确的整改项和责任闭环。
3. 项目交付状态缺少基础导出，无法给项目负责人或客户验收留档。

## 3. 本批范围

### 3.1 后端

建议新增或增强以下能力：

1. 交付绑定审核接口：
   - `POST /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:submit-review`
   - `POST /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:approve`
   - `POST /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}:reject`
   - `GET /api/work-center/projects/{projectId}/delivery-bindings/{bindingId}/review-records`
2. 整改项接口：
   - `GET /api/work-center/projects/{projectId}/rectifications`
   - `GET /api/work-center/projects/{projectId}/rectifications/{rectificationId}`
   - `PATCH /api/work-center/projects/{projectId}/rectifications/{rectificationId}`
   - `POST /api/work-center/projects/{projectId}/rectifications/{rectificationId}:resolve`
   - `POST /api/work-center/projects/{projectId}/rectifications/{rectificationId}:close`
   - `POST /api/work-center/projects/{projectId}/rectifications/{rectificationId}:reopen`
3. 报表导出接口：
   - `GET /api/work-center/projects/{projectId}/reports/delivery-completeness.csv`
   - `GET /api/work-center/projects/{projectId}/reports/review-summary.csv`
   - `GET /api/work-center/projects/{projectId}/reports/rectifications.csv`

说明：

- 导出优先做 CSV，不做复杂 Excel、PDF、图表模板。
- 如果项目已有 `Issue` / `Task` 模型，应优先复用；如果复用会牵连过大，可新增轻量 `work_rectifications`。
- 可以追加 Flyway 新迁移，但不得修改旧迁移。

### 3.2 前端

建议增强现有工作中心，不做新产品大模块：

1. `/work/document-delivery`
2. `/work/drawing-delivery`
3. 可新增 `/work/rectifications` 作为整改中心。

页面应具备：

1. 已挂接列表展示审核状态。
2. 已挂接行可执行：
   - 提交审核
   - 通过
   - 驳回
3. 驳回时必须填写原因，可生成整改项。
4. 整改中心可查看整改项列表、状态、责任人、来源绑定、驳回原因和处理说明。
5. 整改项可执行：
   - 标记已处理
   - 关闭
   - 重新打开
6. 页面提供基础报表导出按钮。

### 3.3 脚本

新增专项验收脚本：

`scripts/dev/check-phase2-batch3-review-rectification-report.sh`

脚本至少覆盖：

1. 准备最小标准和文件。
2. 创建交付绑定。
3. 提交审核。
4. 审核通过后状态变化并写审计。
5. 另一条绑定驳回后生成整改项。
6. 整改项标记已处理、关闭、重新打开。
7. 三类 CSV 导出可下载且包含核心字段。
8. 普通用户不能跨项目审核、整改或导出。
9. OpenAPI 包含新增接口。
10. 测试数据尽量清理，不修改真实 NAS 文件。

## 4. 状态与字段建议

审核状态建议：

- `DRAFT`
- `PENDING`
- `APPROVED`
- `REJECTED`

整改状态建议：

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`
- `REOPENED`

整改项核心字段建议：

- `id`
- `projectId`
- `sourceType`
- `sourceId`
- `bindingId`
- `title`
- `description`
- `reason`
- `status`
- `severity`
- `assigneeUserId`
- `dueDate`
- `resolvedAt`
- `closedAt`
- `createdBy`
- `updatedBy`
- `createdAt`
- `updatedAt`

## 5. 明确禁止事项

本批不做：

1. 完整 BPM / 流程引擎。
2. 多级会签、加签、转签、抄送。
3. 客户生产级角色权限承诺。
4. Agent 自动审批、自动关闭、自动整改。
5. Agent 直接改数据库或改 NAS 文件。
6. PDF、Office、CAD、BIM 正文抽取。
7. 向量库、搜索引擎写入。
8. 模型轻量化在线预览。
9. 构件级解析、构件搜索、碰撞检查。
10. 真实 NAS 文件移动、删除、改名或内容读取。

## 6. 开发 Agent 交付要求

开发 agent 必须使用 Ralph Loop 推进，并把 story 拆小：

1. 审核记录与状态流转。
2. 驳回生成整改项。
3. 整改项状态闭环。
4. 基础 CSV 导出。
5. 前端审核动作与整改中心。
6. 专项验收脚本。
7. 报告。

完成承诺：

`<promise>PHASE2_BATCH3_REVIEW_RECTIFICATION_REPORT_COMPLETE</promise>`

只有当后端构建、前端构建、健康检查、专项脚本、页面冒烟、权限检查、OpenAPI 检查和报告全部完成后，才允许标记完成。

## 7. 测试 Agent 验收要求

测试 agent 必须重点验证：

1. 审核状态不会跳错。
2. 驳回必须有原因，并能生成整改项。
3. 整改项可处理、关闭、重新打开。
4. 导出 CSV 可打开且字段稳定。
5. 权限隔离不回归。
6. 创建审核、驳回、整改、导出动作写审计。
7. 本批没有越界进入模型轻量化、正文抽取、Agent 写操作或真实 NAS 修改。

P0：

1. 无权限用户可审核、整改或导出其他项目数据。
2. 驳回不留原因或不留审计。
3. 整改项状态无法闭环。
4. 导出泄漏无权限项目数据。
5. 本批动作读取、移动、删除、改名或修改真实 NAS 原文件。

P1：

1. 审核状态与页面展示不一致。
2. 驳回后整改项缺少来源绑定。
3. 整改关闭后仍显示为待处理。
4. CSV 缺少核心字段或格式不可用。
5. 专项脚本不可重复运行或留下明显测试污染。

## 8. 收口标准

本批通过后，平台应具备一个可向项目负责人解释的最小交付审核闭环：

`资料已挂接 -> 审核人能判断通过/驳回 -> 驳回有整改项 -> 整改有处理和关闭状态 -> 项目可导出交付与整改报表 -> 全程可审计`

批次三通过后，再讨论下一批是否进入：

`文件预览增强、选择性索引、NAS 文件正文只读抽取、Agent review suggestion`

# DOC-BASE 文档基线收口报告

时间：2026-06-04

## 1. 批次定位

当前批次：`DOC-BASE：文档基线收口`。

本批只更新文档，不写业务代码，不执行对象化迁移，不修改数据库，不触碰真实 NAS 文件。

启动原因：

- `M3G-9` 已通过测试，平台已接近 M3 整体收口。
- 105 样板项目已完成 `2928 / 2928` 全量对象化。
- 葛兰岱尔 READY Viewer 口径已通过 `8C-GD-F3` 修复。
- 旧 `docs/**` 仍保留大量 M1/M2/8B-0 旧口径，容易让后续 agent 误判当前阶段。
- 用户要求保留 API 接口文档，并建立后续实时维护文档内容的规则。

## 2. 本批新增文档

- `docs/11-current-baseline-and-next-roadmap.md`
  - 固化当前平台真实基线。
  - 明确已完成能力、不能承诺能力、关键数据口径和后续任务图。
  - 明确全项目对象化后续单独开 `M3X` 批次。

- `docs/12-api-contract-and-maintenance.md`
  - 保留运行期 Swagger / OpenAPI 入口。
  - 明确 API 分组、禁出字段、接口变更同步规则和批次验收要求。

## 3. 本批更新文档

- `docs/README.md`
  - 加入 `11` / `12` 两份新文档。
  - 更新当前版本结论：当前主线为 M3 对象存储主链路收口。

- `docs/07-complete-delivery-prd.md`
  - 增加 `2026-06-04 当前基线更新`。
  - 明确 105 已 100% 对象化，非 105 未全部对象化。
  - 明确 Hermes 正文问答未启动，BIM 构件级能力未完成。

- `docs/03-architecture-and-system-design.md`
  - 增加 M3 对象存储架构基线。
  - 增加 M4/M5 语义证据与 Hermes 架构目标。

- `docs/10-phase2-development-roadmap.md`
  - 更新为 2026-06-04 路线。
  - 将后续主线改为：`DOC-BASE -> M3-CLOSE -> M3X -> M4 -> M5 -> 8D/8E -> 9A`。

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
  - 把 M3G-9 标记为已完成。
  - 把当前行动改为 DOC-BASE。

- `handoff/main-agent/m3-m5-storage-evidence-task-graph.md`
  - 把当前行动从 M3G-6T 改为 DOC-BASE。
  - 明确 DOC-BASE 后再做 M3-CLOSE 和 M3X。

- `handoff/main-agent/status.md`
  - 当前 active 批次更新为 DOC-BASE。
  - 记录 M3G-9 和 8C-GD-F3 当前状态。

## 4. 当前对外口径

可以说：

> 平台已经完成对象存储主链路建设，105 样板项目已完成全量对象化，已对象化文件通过 NAS 侧 MinIO 与平台权限链路受控访问。

不能说：

> 全部项目已经完成对象化。

不能说：

> Hermes 已经理解文件正文。

不能说：

> BIM 构件级搜索、图模联动、碰撞检查已经完成。

## 5. 后续批次裁决

本批之后建议顺序：

1. `M3-CLOSE`：M3 整体收口判断。
2. `M3X`：全项目对象化独立批次。
3. `M4A`：documents / chunks 语义证据契约。
4. `M4B`：PDF / Office / OCR 小样本 evidence 抽取。
5. `M4C`：向量库与关键词索引试点。
6. `M5A`：Hermes Evidence API。
7. `M5B`：工程主数据与交付治理 Agent 化。

## 6. 文档维护规则

后续任何批次只要修改以下内容，必须同步文档：

- 对外能力口径。
- API / OpenAPI 契约。
- `FileAssetView` / `ModelAssetView` / Gateway response。
- 对象存储状态、Evidence 字段、Hermes 返回证据类型。
- 权限、脱敏、审计和 file-access 规则。
- BIM provider / Storage provider。
- 批次完成、冻结、后置或分支路线变化。

## 7. 边界确认

- 未修改业务代码。
- 未新增数据库迁移。
- 未执行对象化迁移。
- 未触碰真实 NAS 文件。
- 未新增 Hermes、BIM、parser、indexing 能力。
- 未修改运行期 API 契约。

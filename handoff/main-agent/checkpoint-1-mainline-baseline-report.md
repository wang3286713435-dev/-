
# CHECKPOINT-1：主线基线准备与工作区归类报告

时间：2026-06-05

## 1. 结论

当前已完成进入下一轮交互优化前的基线盘点。

主 agent 判断：

- `M3G-9` 已通过测试 agent 验收，可以作为 M3 整体收口依据之一。
- `8C-GD-F4` 已通过极短验证，构件拾取、模型爆炸、构件属性代理和 worker 静态资源链路可作为当前轻量化 Viewer 基线。
- 当前工作区不是干净状态，仍有大量已暂存或未暂存改动；后续必须先做 checkpoint/提交策略，再插入新的平台交互逻辑优化批次。
- 本轮只做收口和报告，不新增业务功能，不执行对象化迁移，不触碰真实 NAS 文件。

## 2. 当前分支与状态

当前分支：

```text
codex/m3g-nas-minio-real-project-object-storage
```

当前相对远端：

```text
ahead 4
```

说明：本地已有 4 个提交未推送到该远端分支；当前工作区还有后续批次改动，不能直接视为可合并主线的干净状态。

## 3. 交付改动归类

### 3.1 M3 / 对象存储主线

归属范围：

- 对象存储任务中心、对象优先读取、覆盖率报告、M3G-9 脚本。
- `docs/11-current-baseline-and-next-roadmap.md`
- `docs/12-api-contract-and-maintenance.md`
- `handoff/main-agent/m3-m5-storage-evidence-task-graph.md`
- `scripts/dev/check-m3g9-objectification-coverage-report.sh`

当前口径：

- 可作为 M3 收口基础。
- 不代表全项目对象化完成。
- 105 已完成对象化，非 105 项目仍需 M3X 继续推进。

### 3.2 8C-GD-F4 / 葛兰岱尔轻量化 Viewer 修复

归属范围：

- 葛兰岱尔后端适配增强。
- Viewer 前端构件拾取、属性代理、模型爆炸。
- `/glandar-engine` 静态 worker / wasm 资源。
- `scripts/dev/check-8c-gd-f4-component-pick-blow-properties.sh`
- `handoff/main-agent/glandar-lightweight-engine-reuse-report.md`

当前口径：

- 当前 Viewer 已具备真实模型打开、构件拾取、高亮、构件属性代理和模型爆炸能力。
- 不代表完整 BIM 构件级平台完成。
- 仍需后续 `8D / 8E` 做构件搜索、图模联动、碰撞结果等业务化能力。

### 3.3 DOC-BASE / 文档基线

归属范围：

- `docs/03-architecture-and-system-design.md`
- `docs/07-complete-delivery-prd.md`
- `docs/10-phase2-development-roadmap.md`
- `docs/README.md`
- `docs/11-current-baseline-and-next-roadmap.md`
- `docs/12-api-contract-and-maintenance.md`
- `handoff/main-agent/doc-base-current-baseline-docs-closure.md`

当前口径：

- 文档已开始转向当前真实基线。
- 后续每进入 M3X / M4 / M5 / 8D / 8E，都必须同步维护任务图和 API 文档入口。

### 3.4 UX4 / 视觉与可用性修复

归属范围：

- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `frontend/src/modules/visualization/pages/DigitalTwinDashboardPage.vue`
- `frontend/src/styles/index.css`
- `handoff/main-agent/ux4-visual-usability-fix-plan.md`

当前口径：

- 该类改动属于平台视觉和员工使用效率优化。
- 用户已要求“完成 1/2/3 后，经批准再插入批次优化平台交互逻辑”。
- 因此 UX4 后续不应在未经用户确认前继续扩大。

### 3.5 本地非交付项

当前仍存在不应提交的本地项：

```text
.claude/**
CLAUDE.md
tmp/**
docs/13-frontend-refactor-design-draft.md
```

处理原则：

- 不纳入主线提交。
- 不作为 M3 / 8C / UX 收口依据。
- 如后续确需保留，必须单独说明用途后再决定是否纳入。

## 4. 当前风险

| 风险 | 等级 | 说明 | 处理建议 |
| --- | --- | --- | --- |
| 工作区混合了 M3、8C、DOC、UX 多类改动 | P1 | 直接提交会让历史难以追踪 | 下一步先按批次拆分提交或至少在提交信息中清楚归类 |
| 非 105 项目对象化覆盖率低 | P2 | 平台主链路完成，但公司全项目还没完成 | 后续 M3X 继续推进 |
| 8C-GD-F4 缺少测试 agent 正式报告 | P2 | 开发报告和极短脚本通过，但 latest test 仍是 M3G-9 | 可由测试 agent 后续补正式短验 |
| UX4 已有改动但未正式批准继续扩大 | P2 | 用户希望先基线准备，再批准交互优化 | 当前暂停扩大 UX4 |

## 5. 基线准备状态

```text
CHECKPOINT-1: READY
```

含义：

- 已完成工作区盘点。
- 已区分对象存储、BIM Viewer、文档基线、UX 修复和非交付项。
- 已明确下一步不能直接继续堆功能，必须先由用户批准是否插入平台交互逻辑优化批次。
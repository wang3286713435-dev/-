# 主线 Git 治理与 Hermes 冻结裁决

更新时间：2026-05-20

## 1. 背景

当前平台开发长期停留在 `codex/nas-real-project-import-pr` 分支，`main` 仍停在早期交接状态，导致主线与真实成果倒挂。

同时，Hermes 从只读辅助逐步扩展到 G3 / G4，已经出现“平台主线被 Hermes 吸住”的风险。用户明确要求：

- 暂停 G4。
- 冻结 Hermes 定位。
- 不允许继续在支线长期开发。
- 恢复平台主线开发。
- Hermes 后续必须重新对齐定位，并通过独立分支继续完善。

## 2. Git 决策

已执行：

1. 在当前成果点创建冻结分支：
   - `codex/hermes-g3-g4-freeze`
2. 将 `main` 快进到当前平台成果点：
   - 当前基线提交：`ac3baa3 docs: plan g4 real project delivery trial`
3. `codex/nas-real-project-import-pr` 保留为历史开发分支，不再作为主线继续推进。

待本治理提交完成后：

- `main` 是当前平台主线。
- 后续平台功能开发默认从 `main` 拉新分支。
- Hermes 继续开发必须从 `main` 单独开分支，例如：
  - `codex/hermes-realignment-*`
  - `codex/hermes-agent-*`

## 3. Hermes 冻结口径

Hermes 当前保留为已接入能力，但主线不继续扩张 Hermes。

当前允许保留：

- 平台内嵌入口。
- catalog-only 问答。
- Missing Evidence 兜底。
- 已有受控 Action Center 能力。
- 已有人工确认后调用平台既有接口的能力。

当前冻结：

- G4 不继续开发。
- 不继续扩展 Hermes Agent 能力。
- 不进入 Evidence Layer / Memory Layer / Orchestration Layer。
- 不做正文抽取。
- 不做长期 memory。
- 不做 Agent 自动治理。
- 不做多 Agent 调度。

后续 Hermes 重新启动前，必须先完成定位重审：

1. Hermes 在平台中的边界是什么。
2. 哪些动作由平台主功能负责，哪些由 Hermes 引导。
3. Hermes 能不能执行动作，执行动作前需要哪些人工确认。
4. Hermes 是否进入正文证据层，何时进入，验收标准是什么。
5. Hermes 是否接入 memory / workflow，何时接入，安全红线是什么。

## 4. 主线恢复方向

主线后续不再围绕 Hermes 堆功能，而是回到平台自身能力完善。

建议当前主线进入：

`M1A：平台主线功能基线审计与交付闭环缺口收束`

重点看：

- 项目工作台是否清晰。
- 资产总览和文件管理是否适合真实项目使用。
- 工程主数据是否能被普通员工理解和维护。
- 标准驱动交付链路是否能跑通。
- 文档 / 图纸交付、审核、整改、导出预检查是否稳定。
- BIM Mock 入口是否只是安全占位，未误导成真实轻量化。
- 权限、审计、路径脱敏是否没有回归。

M1A 只做主线平台功能审计与小范围修补，不启动新的大模块。

## 5. 禁止事项

主线恢复阶段禁止：

- 继续 G4。
- 新增 Hermes 能力。
- 将 Hermes 作为主线开发核心。
- 进入 8B / 8C / 9A。
- 开放真实 NAS 增删改查。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 写 Hermes memory、OpenSearch、Qdrant、MinIO documents/chunks。
- Agent 自动审批、自动整改、自动挂接。

如发现 Hermes 现有能力阻塞平台主线，只允许做 P0/P1 级别修复，不允许借修复继续扩功能。

## 6. 后续分支规则

- 平台主线功能：从 `main` 拉 `codex/platform-*`。
- Hermes 重新对齐：从 `main` 拉 `codex/hermes-*`。
- BIM 轻量化：用户确认后从 `main` 拉 `codex/bim-*`。
- NAS 受控写操作：用户确认后从 `main` 拉 `codex/nas-*`。

每条分支必须有明确批次名、边界、验收 prompt 和收口报告。

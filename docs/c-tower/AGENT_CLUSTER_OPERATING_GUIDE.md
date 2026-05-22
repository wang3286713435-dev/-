# C塔 Claude 开发集群与 Codex 监控运行手册

版本：v1.0
日期：2026-05-22

## 1. 目标

本手册说明 C塔项目如何实际使用 Agent 集群开发：

- Claude 作为开发 worker 集群。
- Codex 作为监控、调度、验收和合并门禁。
- Git worktree 作为隔离执行环境。
- `TASK_GRAPH.yaml` 作为任务来源。
- `handoff/c-tower/<task_id>/` 作为 worker 交付物目录。

重点不是让多个 Agent 同时随意写代码，而是把研发过程工程化：任务可追踪、分支可隔离、修改可审查、测试可复现、合并可控制。

## 2. Agent 集群功能如何开发

建议分三步建设。

### 2.1 MVP：本机手动调度版

目标：先跑通真实工作流，不急着做 Web 控制台。

能力：

- Codex Monitor 读取 `TASK_GRAPH.yaml`。
- Codex Monitor 找出 `status: ready` 且依赖完成的任务。
- Codex Monitor 生成人类可读的 Claude worker prompt。
- Codex Monitor 创建或指定 git worktree。
- 人工或 Codex 在对应 worktree 中启动 Claude。
- Claude worker 输出 `report.json`、`test.log`、`risk.md`、`diff.patch`。
- Codex Monitor 检查 diff、写集、禁止路径和门禁命令。
- Codex Monitor 给出返工、通过、挂起或合并建议。

MVP 不需要：

- Web 控制台。
- 自动排队系统。
- 自动合并 main。
- 多模型路由。
- 成本统计。

### 2.2 V1：脚本化调度版

目标：把重复动作脚本化。

建议新增能力：

- `agent-cluster list-ready`：列出可执行任务。
- `agent-cluster prepare <task_id>`：创建 artifact 目录和 worker prompt。
- `agent-cluster worktree <task_id>`：创建任务 worktree 和分支。
- `agent-cluster check <task_id>`：检查 write_set、forbidden_paths 和 artifact。
- `agent-cluster gate <task_id>`：执行任务门禁。
- `agent-cluster report <task_id>`：生成 Codex Monitor 审查报告。

第一版任务状态仍可存在 YAML 中，后续再升级 SQLite。

### 2.3 V2：平台化调度版

目标：当任务和 worker 数量变大时，建设控制台和任务队列。

建议新增：

- SQLite 或 Postgres 任务库。
- Redis/BullMQ/Celery/Temporal 队列。
- Worker 租约和锁。
- write_set 冲突检测。
- 运行成本统计。
- Agent 状态看板。
- PR 自动生成。
- CI 集成。
- 审计日志。

## 3. 实际使用流程

### 3.1 启动前检查

Codex Monitor 先检查：

```bash
git status --short
git branch --show-current
git worktree list
claude --help
```

检查目的：

- 确认当前工作树是否有未处理改动。
- 确认是否已有 integration 分支。
- 确认 Claude 命令可用。
- 确认当前任务不会覆盖他人改动。

### 3.2 选择 ready 任务

Codex Monitor 从 `TASK_GRAPH.yaml` 中选择：

- `status: ready`
- `depends_on` 已完成或为空
- `write_set` 与其他运行中任务不冲突
- 不触碰高风险目录

当前第一批建议先跑：

```text
CT-P0-REQ-001
```

完成后再并行：

```text
CT-P0-ARCH-001
CT-P0-DATA-001
CT-P0-VIS-001
CT-P0-SEC-001
```

### 3.3 准备 artifact 目录

每个任务一个目录：

```text
handoff/c-tower/CT-P0-REQ-001/
  prompt.txt
  report.json
  test.log
  risk.md
  diff.patch
```

`prompt.txt` 必须由 `WORKER_PROMPT_TEMPLATE.md` 和 `TASK_GRAPH.yaml` 合成。

### 3.4 创建 worktree

推荐由 Codex Monitor 创建 worktree，因为 Codex 负责隔离和合并控制。

示例：

```bash
git worktree add worktrees/CT-P0-REQ-001 -b codex/ctower/CT-P0-REQ-001-requirement-matrix
```

进入任务 worktree：

```bash
cd worktrees/CT-P0-REQ-001
```

### 3.5 启动 Claude worker

交互式方式适合复杂任务：

```bash
claude --name CT-P0-REQ-001 --permission-mode acceptEdits
```

然后把 `handoff/c-tower/CT-P0-REQ-001/prompt.txt` 作为任务输入给 Claude。

非交互式方式适合短任务：

```bash
claude -p "$(cat handoff/c-tower/CT-P0-REQ-001/prompt.txt)" --output-format json
```

建议权限：

- 默认使用 `acceptEdits` 或更保守模式。
- 根据任务限制允许工具范围。
- 不默认使用跳过权限检查模式。

### 3.6 Claude worker 执行要求

Claude worker 必须：

- 只读必要文档。
- 只改 `write_set`。
- 不改 `forbidden_paths`。
- 不主动合并分支。
- 不扩大需求。
- 输出四个 artifact。
- 对未完成事项和风险诚实报告。

### 3.7 Codex Monitor 验收

Codex Monitor 收到 worker 完成信号后检查：

```bash
git diff --name-only
git diff --check
```

再检查：

- 修改文件是否全部在 `write_set`。
- 是否触碰 `forbidden_paths`。
- `report.json` 是否合法。
- `test.log` 是否包含 gate commands 结果。
- `risk.md` 是否列出残余风险。
- `diff.patch` 是否存在并与当前 diff 一致。

然后运行 `TASK_GRAPH.yaml` 中的 `gate_commands`。

### 3.8 返工、挂起、通过

Codex Monitor 裁决：

| 结果 | 条件 | 处理 |
| --- | --- | --- |
| 通过 | 验收项完成、无越界、门禁通过 | 标记任务完成，可进入后续依赖 |
| 返工 | 有缺陷但范围清楚 | 生成返工 prompt 给同一 worker |
| 挂起 | 需要用户、业主或架构裁决 | 写入 `needs_lead_decision.md` |
| 拒绝 | 越界修改、误删、过度承诺、无法验收 | 不合并，要求重做 |

### 3.9 合并

只允许进入：

```text
integration/c-tower
```

不得直接进入 main。

建议合并前记录：

- 任务 ID。
- worker 分支。
- 修改文件。
- 测试结果。
- 审查结论。
- 残余风险。
- 是否建议回灌主线。

## 4. Codex Monitor 的核心职责

Codex Monitor 要像项目经理、架构师、测试负责人和发布经理的组合：

- 维护 PRD、TECH_SPEC、DELIVERY_PLAN 和 TASK_GRAPH。
- 把自然语言需求转成可验收任务。
- 控制分支、worktree 和写集。
- 对 Claude worker 的输出做独立判断。
- 保护 main 和当前工作树。
- 把需要用户判断的事项讲清楚。

Codex Monitor 不应该：

- 盲信 worker 的“已完成”。
- 让 worker 直接合 main。
- 让多个 worker 修改同一写集。
- 在需求未冻结前派大量代码任务。
- 把 C塔定制能力无选择地回灌主线。

## 5. Claude worker 的核心职责

Claude worker 要像被明确授权的研发成员：

- 接任务。
- 读上下文。
- 在隔离 worktree 内工作。
- 自测。
- 写报告。
- 等待 Codex Monitor 验收。

Claude worker 不应该：

- 重新解释项目目标。
- 自行调整阶段范围。
- 修改没有授权的文件。
- 直接处理合并。
- 隐瞒测试失败。

## 6. 推荐目录结构

```text
docs/c-tower/
  PRD.md
  TECH_SPEC.md
  DELIVERY_PLAN.md
  TASK_GRAPH.yaml
  WORKER_PROMPT_TEMPLATE.md
  TEST_ACCEPTANCE_CHECKLIST.md
  AGENT_CLUSTER_OPERATING_GUIDE.md
  REQUIREMENT_MATRIX.md
  DATA_PLATFORM_POC_PLAN.md
  VISUALIZATION_POC_PLAN.md
  SECURITY_AND_COMPLIANCE_PLAN.md

handoff/c-tower/
  CT-P0-REQ-001/
    prompt.txt
    report.json
    test.log
    risk.md
    diff.patch

worktrees/
  CT-P0-REQ-001/
  CT-P0-DATA-001/
```

## 7. 第一次真实运行建议

第一轮不要直接让 Claude 写业务代码。

建议顺序：

1. Codex Monitor 派发 `CT-P0-REQ-001`。
2. Claude requirements worker 生成 `REQUIREMENT_MATRIX.md`。
3. Codex Monitor 审查矩阵是否覆盖两份文件。
4. 再并行派发架构、数据、可视化、安全四个专项文档任务。
5. Codex Monitor 汇总后生成 P1 backlog。
6. 用户确认 P1 backlog 后，再让 Claude worker 开始业务代码开发。

这一步的目标是证明集群机制可靠，而不是抢先写代码。

## 8. 最小可用命令清单

查看任务：

```bash
rg -n "status: ready|task_id:" docs/c-tower/TASK_GRAPH.yaml
```

查看当前改动：

```bash
git status --short
```

查看 worktree：

```bash
git worktree list
```

创建 worktree：

```bash
git worktree add worktrees/CT-P0-REQ-001 -b codex/ctower/CT-P0-REQ-001-requirement-matrix
```

启动 Claude：

```bash
claude --name CT-P0-REQ-001 --permission-mode acceptEdits
```

生成 diff：

```bash
git diff > handoff/c-tower/CT-P0-REQ-001/diff.patch
```

检查空白和冲突标记：

```bash
git diff --check
```

工程门禁：

```bash
cd backend && ./mvnw -pl delivery-app -am package
corepack pnpm --dir frontend typecheck
corepack pnpm --dir frontend build
```

## 9. 成功标准

Agent 集群第一阶段成功，不是看写了多少代码，而是看：

- 任务拆得清楚。
- Claude 不越界。
- Codex 能发现问题。
- artifact 完整。
- 测试和审查能复现。
- 用户能看到每个任务为什么做、做了什么、还缺什么。

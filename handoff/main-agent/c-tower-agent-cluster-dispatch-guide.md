# C塔 Claude 集群调度手册

本手册给 Codex Monitor 使用。Claude 是开发 worker 集群，Codex 负责监控、验收、审查和合并建议。

## 1. 调度总则

Codex Monitor 是唯一调度入口。Claude worker 不直接互相分配任务，不直接合并主线，不自行扩大范围。

调度依据：

- `docs/c-tower/PRD.md`
- `docs/c-tower/TECH_SPEC.md`
- `docs/c-tower/DELIVERY_PLAN.md`
- `docs/c-tower/TASK_GRAPH.yaml`
- `docs/c-tower/WORKER_PROMPT_TEMPLATE.md`
- `docs/c-tower/TEST_ACCEPTANCE_CHECKLIST.md`
- `docs/c-tower/AGENT_CLUSTER_OPERATING_GUIDE.md`

正式需求来源：

- `/Users/vc/Downloads/数据中台.docx`
- `/Users/vc/Downloads/智慧建筑平台清单及系统配置.xlsx`

## 2. 启动顺序

1. Codex Monitor 启动并读取 `c-tower-lead-agent-startup-prompt.md`。
2. Codex Monitor 执行环境盘点。
3. Codex Monitor 读取 `TASK_GRAPH.yaml`。
4. Codex Monitor 找出 `status: ready` 且依赖满足的任务。
5. Codex Monitor 检查这些任务的 `write_set` 是否冲突。
6. Codex Monitor 为可执行任务生成 Claude worker prompt。
7. Claude worker 在独立 worktree 中执行任务。
8. Claude worker 输出 artifact。
9. Codex Monitor 汇总并决定返工、挂起、通过或合并建议。

## 3. 并行规则

可并行：

- 没有依赖关系。
- `write_set` 不重叠。
- 不触碰全局高风险区。
- 不同时修改同一文档或同一业务模块。

必须串行：

- 后续任务依赖前置任务。
- 任一任务修改 Flyway 迁移。
- 任一任务修改权限、菜单、路由、公共 DTO。
- 任一任务改数据库主约束或主数据唯一性规则。
- 任一任务需要接口契约裁决。

## 4. Worktree 规则

推荐路径：

```text
worktrees/
  CT-P0-REQ-001/
  CT-P0-ARCH-001/
  CT-P0-DATA-001/
  CT-P0-VIS-001/
  CT-P0-SEC-001/
```

推荐分支名使用 `TASK_GRAPH.yaml` 的 `branch` 字段，例如：

```text
codex/ctower/CT-P0-REQ-001-requirement-matrix
```

## 5. Claude worker 派工格式

Codex Monitor 给 Claude worker 的 prompt 必须包含：

- 任务 ID。
- 任务标题。
- 任务阶段。
- worker 角色。
- 允许写集。
- 禁止路径。
- 验收标准。
- 必跑命令。
- artifact 目录。
- 上游任务产物摘要。
- 需求来源编号。

Codex Monitor 不得只发送自然语言需求给 Claude worker。

## 6. 验收与返工

worker 完成后，Codex Monitor 必须检查：

- `report.json` 是否存在且为合法 JSON。
- `test.log` 是否存在。
- `risk.md` 是否存在。
- `diff.patch` 是否存在。
- diff 是否越过 `write_set`。
- 是否触碰 `forbidden_paths`。
- gate commands 是否通过。
- 是否出现过度承诺或隐藏缺口。

返工条件：

- 缺少 artifact。
- 越界修改。
- 测试失败且未解释。
- 文档或页面宣称完整平台已完成。
- Flyway 修改已应用迁移。
- 未经批准修改权限、菜单、路由、公共 DTO。
- 未经批准变更主数据、数据库主约束或 API 契约。

## 7. 合并规则

合并顺序：

```text
Claude worker branch
  -> Codex Monitor 检查 artifact
  -> QA worker 验收
  -> review worker 审查
  -> Codex Monitor 裁决
  -> integration/c-tower
  -> 全量测试
  -> 用户决定 main / 独立仓库 / 保留定制线
```

Codex Monitor 必须保留合并记录：

- 合并任务。
- 合并来源分支。
- 测试结果。
- review 结果。
- 残余风险。
- 是否建议回灌主线。

## 8. 第一轮推荐派工

第一轮只做 P0，不让 Claude worker 直接写业务代码。

顺序：

1. `CT-P0-REQ-001`：两份正式需求文件覆盖矩阵。
2. `CT-P0-ARCH-001`：当前平台与完整需求差距评估。
3. `CT-P0-DATA-001`：数据中台与国产数据库 PoC 方案。
4. `CT-P0-VIS-001`：数字孪生与云渲染专项方案。
5. `CT-P0-SEC-001`：等保二级、隐私保护与信创适配方案。
6. `CT-P0-PLAN-001`：P1 backlog、预算口径与集群运行报告。
7. `CT-P0-QA-001`：P0 文档和任务图审查。

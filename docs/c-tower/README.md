# C塔平台规划与 Agent 集群启动包

本目录是 C塔智慧建筑数字底座、数据中台与可视化管理平台的规划和 Agent 集群调度入口。

当前口径：

- 平台目标不再是“能力验证切片”，而是满足两份正式需求文件的完整平台规划。
- Claude 作为开发 worker 集群。
- Codex 作为 Monitor，负责需求、任务、写集、验收、审查和合并建议。
- 当前仓库可作为 P0/P1 起点，但完整 C塔平台需要新增 IoT、数据中台、集成、AI、位置、可视化、移动端、安全和运维专项能力。

正式需求来源：

- `/Users/vc/Downloads/数据中台.docx`
- `/Users/vc/Downloads/智慧建筑平台清单及系统配置.xlsx`

## 文件说明

| 文件 | 用途 |
| --- | --- |
| `PRD.md` | 总业务需求文档，定义完整平台目标、范围、验收和风险 |
| `TECH_SPEC.md` | 总技术方案，定义架构、模块、数据、接口、安全、部署和专项 PoC |
| `DELIVERY_PLAN.md` | 交付计划，定义 P0 到 P4 阶段、组织分工和门禁 |
| `TASK_GRAPH.yaml` | Claude worker 任务图，由 Codex Monitor 调度 |
| `WORKER_PROMPT_TEMPLATE.md` | Claude worker 派工 prompt 模板 |
| `TEST_ACCEPTANCE_CHECKLIST.md` | Agent 集群、P0 到 P4、工程和合并验收清单 |
| `AGENT_CLUSTER_OPERATING_GUIDE.md` | Claude 集群与 Codex 监控的开发和实际使用手册 |

## 当前下一步

第一轮只做 P0，不直接写业务代码：

1. 派发 `CT-P0-REQ-001`，生成两份正式需求文件的覆盖矩阵。
2. Codex Monitor 审查覆盖矩阵。
3. 并行派发架构、数据、可视化、安全专项方案。
4. 汇总 P1 backlog、预算口径和第一轮集群运行报告。
5. 用户确认 P1 后，再启动业务代码开发。

## 关键边界

- 当前 C塔原型代码只作为参考样例，不作为正式交付成果。
- 不得把演示样例宣称为完整平台已交付。
- Claude worker 只能在自己的 `write_set` 内工作。
- 所有通过验收的任务先合入 `integration/c-tower`。
- 是否进入 main、转独立仓库或保留定制线必须由用户确认。

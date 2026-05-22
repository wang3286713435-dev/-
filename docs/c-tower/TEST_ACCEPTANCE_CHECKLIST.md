# C塔平台与 Agent 集群验收清单

版本：v2.0
日期：2026-05-22

## 1. Codex Monitor 调度验收

- Codex Monitor 已读取 `docs/c-tower/PRD.md`。
- Codex Monitor 已读取 `docs/c-tower/TECH_SPEC.md`。
- Codex Monitor 已读取 `docs/c-tower/DELIVERY_PLAN.md`。
- Codex Monitor 已读取 `docs/c-tower/TASK_GRAPH.yaml`。
- Codex Monitor 已检查当前分支、工作树状态和 worktree 列表。
- Codex Monitor 能识别现有 C塔原型和正式交付范围的差异。
- Codex Monitor 能按 `depends_on` 找出可执行任务。
- Codex Monitor 能按 `write_set` 阻止冲突任务并行。
- Codex Monitor 能按 `forbidden_paths` 拦截越界修改。
- Codex Monitor 能为 Claude worker 生成完整 prompt。
- Codex Monitor 不直接信任 worker 自报完成，必须审查 diff 和 artifact。

## 2. Claude worker 交付验收

- worker 只修改授权 `write_set`。
- worker 未修改 `forbidden_paths`。
- worker 输出 `report.json`。
- worker 输出 `test.log`。
- worker 输出 `risk.md`。
- worker 输出 `diff.patch`。
- `report.json` 是合法 JSON。
- `test.log` 记录 gate commands 的执行结果。
- `risk.md` 列出残余风险和需裁决事项。
- `diff.patch` 可被 Codex Monitor 审查。

## 3. P0 文档验收

- `REQUIREMENT_MATRIX.md` 覆盖 `数据中台.docx`。
- `REQUIREMENT_MATRIX.md` 覆盖 `智慧建筑平台清单及系统配置.xlsx`。
- 每条需求有来源文件、来源位置、需求描述、平台模块、优先级、阶段、验收方式和风险。
- PRD、TECH_SPEC、DELIVERY_PLAN、TASK_GRAPH 口径一致。
- 文档没有把“能力验证切片”作为最终平台目标。
- 文档明确当前仓库可复用边界和必须新增的服务或专项。
- 文档明确国产数据库、时序数据库、云渲染、AI 中台和等保二级为专项能力。

## 4. P1 数字底座验收

- 统一身份、角色、菜单、权限和审计基础可用。
- 数据源目录可管理系统、数据库、接口、Excel、IoT 和时序源。
- 主数据目录覆盖 One ID、One Thing、One Map。
- API 服务目录覆盖设备状态、历史数据、控制指令、楼层能耗和空间占用。
- DQC 看板展示上报率、完整性、及时性、异常率和补数情况。
- IoT 样例接入可展示设备、点位、最新值和状态。
- 时序数据库 PoC 有写入、查询、压缩、降采样和容量测算报告。
- 国产关系数据库 PoC 有核心表、核心接口和兼容性报告。
- 基础驾驶舱能展示运营、点位、告警、能耗和数据质量概览。

## 5. P2 业务应用验收

- 智慧安防支持监控台、告警处置、通行权限、研判、巡更和安全分析。
- 智慧交通支持交通监控、融合权限、智能调度、无感通行、充电和交通分析。
- 设施设备支持机电监控、资产、工单、巡检、AI 运维、备件、健康监测和工程分析。
- 能碳支持能源监控、计量、供冷、照明、环境、碳资产和能碳分析。
- 应急指挥支持沙盘、预案、疏散、协同、演练和复盘。
- 风险管理支持风险源、智能监测、动态识别和处置分析。
- 通行、机器人、门户、租户后台和移动端形成业务闭环。

## 6. P3 可视化验收

- 驾驶舱、大屏、中屏和专题可视化按需求清单交付。
- BIM/GLTF 模型加载、楼层钻取、对象拾取和实时数据绑定可用。
- 云渲染支持合同约定并发、分辨率、故障恢复和资源调度。
- 云推送支持 MQTT、WebSocket 或 HTTP 长轮询，具备权限、审计和断线重连。
- 可视化专题覆盖能碳、消防、楼宇、照明、安防、导航、应急、交通、建造、巡检、健康监测和碳排。
- 数字孪生场景在指定终端上运行流畅。

## 7. P4 生产验收

- 最大接入点位不低于 200,000 点，或提供合同认可的容量验证报告。
- 最大并发用户不低于 2,000 人，或提供合同认可的压测报告。
- 数据吞吐量不低于 50,000 TPS，或提供合同认可的专项压测报告。
- 报警响应时间小于等于 1 秒。
- 控制指令下发小于等于 1 秒。
- BIM 模型加载小于等于 5 秒。
- 系统年可用率设计不低于 99.99%，并提供高可用验证材料。
- 灾备恢复 RTO 小于等于 4 小时，并提供演练材料。
- 等保二级测评和整改完成。
- 源代码、部署手册、运维手册、用户手册、测试报告和验收意见齐备。

## 8. 工程门禁

默认命令：

- `git diff --check`
- `cd backend && ./mvnw -pl delivery-app -am package`
- `corepack pnpm --dir frontend typecheck`
- `corepack pnpm --dir frontend build`

任务级命令以 `TASK_GRAPH.yaml` 中的 `gate_commands` 为准。

## 9. 合并门禁

- 所有 worker artifact 完整。
- Codex Monitor 完成 write_set 检查。
- Codex Monitor 完成 forbidden_paths 检查。
- QA worker 无 P0 阻塞项。
- review worker 无越界修改和未解释架构变更。
- 通过任务只允许进入 `integration/c-tower`。
- 是否进入 main、独立仓库或保留定制线必须由用户确认。

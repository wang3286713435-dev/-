# M 系列主线健康恢复启动

更新时间：2026-05-20

## 启动结论

当前正式进入 `M 系列：平台主线健康恢复与本体能力补齐`。

M 系列的目标不是继续扩功能，而是把主线健康度从 `黄灯可控` 拉到 `绿灯可持续开发`。

当前 active 批次固定为：

`M1A：平台主线功能基线审计与交付闭环缺口收束`

## 为什么先做 M1A

当前平台已经完成大量能力，但最近主线被 Hermes / G 线持续牵引，存在三个风险：

1. 用户可能仍然不知道如何不用 Agent 完成真实项目交付。
2. 工程主数据、文件管理、文档 / 图纸交付之间的链路需要重新验收。
3. 8B / 9A 等后续路线不能在平台本体未稳定前启动。

因此，M1A 先做主线基线审计，不做大模块开发。

## M1A 必须确认的内容

- 资产总览可以作为真实项目入口。
- 项目工作台项目上下文稳定。
- 文件管理目录树、文件表、详情、预览、治理、checksum 入口不回归。
- 工程主数据初始化、部位树、节点类型、交付物标准可打开、可理解、可维护。
- 文档 / 图纸交付、审核、整改、导出预检查可用。
- 8A BIM Mock 入口仍然只是安全占位，不冒充真实轻量化。
- 权限、审计、路径脱敏没有回归。
- 105 与至少一个非 105 真实 NAS 项目均可抽查。

## M1A 禁止事项

- 不继续 G4。
- 不新增 Hermes 能力。
- 不进入 8B / 8C / 9A。
- 不开放真实 NAS 增删改查。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做 selective indexing、Hermes memory、OpenSearch / Qdrant / MinIO documents/chunks 写入。
- 不做 Agent 自动审批、自动整改、自动挂接。
- 不为 105 写死特殊逻辑。

## 绿灯判定

主线从 `黄灯可控` 转为 `绿灯` 的最低条件：

1. M1A 开发 agent 审计完成，报告写入 `handoff/dev-agent/latest-report.md`。
2. M1A 测试 agent 验收完成，报告写入 `handoff/test-agent/latest-report.md`。
3. 后端构建、前端构建、健康检查、6A、6B、7A、8A 回归脚本通过。
4. 105 和至少一个非 105 真实 NAS 项目无 P0 / P1。
5. 未新增 Hermes、8B、9A、NAS 写操作或正文抽取能力。

## 下一步

当前 `handoff/dev-agent/current-prompt.md` 和 `handoff/test-agent/current-prompt.md` 已指向 M1A。

建议立即让开发 agent 执行 M1A 审计；开发报告返回后，由主 agent 审计是否可以交给测试 agent 做 M1A 验收。


# 二期插入批次 G1：Agent 引导式交付治理 MVP 规划

时间：2026-05-19

## 1. 主 Agent 判断

`二期 8A：BIM 轻量化适配层与 Mock 预览入口` 已正式收口。按用户最新裁决，8A 后暂不进入 8B，不启动真实 BIM 轻量化，也不开放真实 NAS 增删改查。

当前临时插入高优先级模块：`Agent 引导式交付治理 MVP`。

本批的价值不是扩展 AI 自动化，而是降低真实 NAS 项目进入数字化交付闭环的使用门槛。平台已经具备项目资产、工程主数据、交付标准、文档/图纸交付、批量挂接、审核整改、交付包准备和导出预检查，但普通员工仍难以判断下一步该做什么。本批要把这些既有能力组织成一个“交付体检 + 缺失解释 + 候选推荐 + 人工确认挂接”的项目内工作流。

## 2. 批次定位

批次名称：`Agent 引导式交付治理 MVP`

批次代号：`Phase2-G1`

主入口建议：

- 项目工作台顶部或工作中心分组新增 `交付治理助手`。
- 项目详情页可增加主按钮 `开始交付治理`。
- 路由必须带项目上下文，建议：`/data-steward/assets/:projectId/work/agent-governance`。

核心链路：

`只读体检 -> 缺失项解释 -> 候选文件推荐 -> 人工勾选确认 -> 复用批量挂接 -> 审计留痕 -> 刷新交付完整率`

本批完成后，用户应能在真实 NAS 项目里看到当前项目离交付闭环还差什么，并在人工确认后批量补交候选文件。

## 3. 后端范围

建议新增工作中心聚合接口：

- `GET /api/work-center/projects/{projectId}/agent-governance/overview`
- `GET /api/work-center/projects/{projectId}/agent-governance/missing-items`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommend-bindings`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommendations:apply`

必须复用现有能力：

- `GET /api/master-data/projects/{projectId}/standard-status`
- `GET /api/work-center/projects/{projectId}/delivery-completeness`
- `GET /api/work-center/projects/{projectId}/delivery-package/summary`
- `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck`
- `POST /api/work-center/projects/{projectId}/delivery-bindings:batch`
- 现有资产目录 / 文件资源只读查询能力。

推荐算法限定为元数据规则，不接 LLM 正文理解：

- 文件 ID。
- 文件名。
- 文件类型。
- 扩展名。
- 专业。
- 版本。
- 逻辑路径。
- 处理状态。
- 项目归属。
- 质量标记。
- 预览状态。

推荐结果必须包含：

- 缺失项标识。
- 推荐文件 ID。
- 文件名。
- 推荐挂接目标。
- 推荐原因。
- 置信度：`HIGH / MEDIUM / LOW`。
- 风险提示。
- 是否需要先治理元数据。

应用推荐时必须要求：

- `confirmed=true`
- 当前登录用户拥有项目权限。
- 推荐文件仍属于当前项目。
- 推荐目标仍存在。
- 推荐类型仍匹配文档/图纸交付口径。

## 4. 前端范围

新增页面：

- `frontend/src/modules/work-center/pages/AgentDeliveryGovernancePage.vue`

建议页面结构：

1. 项目工作台导航。
2. 项目交付体检卡片。
3. Agent 总结区。
4. 文档/图纸缺失项概览。
5. 推荐挂接方案表格。
6. 批量确认挂接。
7. 执行结果。
8. 后续动作入口：
   - 去部位树。
   - 去节点类型。
   - 去交付物标准。
   - 去文件管理。
   - 去文档交付。
   - 去图纸交付。
   - 去整改闭环。
   - 查看交付包预检查。

文案必须面向业务小白，避免只展示技术字段。例如：

- `部位树还没建好，平台还不知道资料应该交到哪个楼层或系统。`
- `节点类型尚未锁定，说明交付规则还可能变化，建议先锁定后再批量挂接。`
- `这份文件名和交付物类型较接近，但缺少专业字段，建议先治理元数据。`

如复用 Hermes 对话区，必须保持只读、catalog-only、Missing Evidence、draft-only，不允许让 Hermes 触发写操作。

## 5. 禁止事项

本批严禁：

- Agent 自动写库。
- Agent 自动挂接文件。
- Agent 自动提交审核。
- Agent 自动通过审核。
- Agent 自动驳回或整改。
- Agent 自动修改部位树。
- Agent 自动锁定节点类型。
- Agent 自动修改交付物标准。
- Agent 自动移动、删除、重命名、上传 NAS 文件。
- Agent 读取 PDF / Office / DWG / RVT / IFC 正文。
- Agent 写入向量库、长期 memory、OpenSearch、Qdrant、MinIO documents/chunks。
- 返回真实 NAS 绝对路径给普通用户。
- 破坏 8A BIM Mock 预览入口和后续 BIM 主线接口。

本批只能做：

`只读分析 + 推荐方案 + 人工确认 + 调用既有平台批量挂接能力`

## 6. 验收口径

测试 agent 至少验证：

1. 项目工作台存在 `交付治理助手` 入口。
2. 从真实 NAS 项目详情进入，不丢失 `projectId`。
3. 页面显示工程主数据状态。
4. 页面显示文档/图纸完整率和缺失项。
5. 能生成候选文件推荐。
6. 推荐结果不包含真实 NAS 绝对路径。
7. 用户未确认时不能批量挂接。
8. 用户确认后可以调用批量挂接。
9. 批量挂接结果显示创建、跳过、失败。
10. 挂接后文档/图纸交付完整率刷新。
11. 审核、整改、交付包准备视图不回归。
12. 8A BIM Mock 预览入口不回归。
13. Hermes / Agent 不可用时页面安全降级，不白屏、不 500。
14. 普通用户看不到真实 NAS 路径。
15. `git diff --check`、前后端构建、健康检查通过。

## 7. 收口标准

只有当以下条件同时满足，主 agent 才能收口：

- 真实 NAS 项目可打开交付治理助手。
- 项目交付体检可读、可解释。
- 缺失项解释可用。
- 候选文件推荐可用且不读正文。
- 用户必须人工确认后才能批量挂接。
- 批量挂接复用现有平台能力并留审计。
- 不泄露真实 NAS 路径。
- 不破坏交付、整改、导出预检查和 8A BIM Mock 能力。

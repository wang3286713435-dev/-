# 二期插入批次 G1 Agent 引导式交付治理 MVP 收口报告

时间：2026-05-19

## 1. 主 Agent 收口结论

二期插入批次 G1 `Agent 引导式交付治理 MVP` 正式收口。

本批目标是把平台从“人工配置型系统”向“Agent 引导式交付治理系统”推进一步，让真实 NAS 项目可以通过项目交付体检、缺失项解释、候选文件推荐和人工确认批量挂接，更顺畅地进入数字化交付闭环。

开发 agent 已完成实现并写入 `handoff/dev-agent/latest-report.md`。测试 agent 已完成专项验收并写入 `handoff/test-agent/latest-report.md`。当前未发现 P0/P1。

## 2. 验收通过项

已通过：

- 后端构建：`./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：`corepack pnpm --dir frontend build`
- 后端健康检查：`/actuator/health`
- `git diff --check`
- G1 专项脚本：`scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`，`PASS=34 FAIL=0`
- 8A 回归：`scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`，`PASS=11 FAIL=0`
- 7A 回归：`scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`，`PASS=18 FAIL=0`
- 6B 回归：`scripts/dev/check-phase2-batch6b-delivery-package.sh`，`PASS=17 FAIL=0`
- 批次 3 回归：`scripts/dev/check-phase2-batch3-review-rectification-report.sh`，`通过=52 失败=0`

## 3. 本批确认能力

后端已新增：

- `GET /api/work-center/projects/{projectId}/agent-governance/overview`
- `GET /api/work-center/projects/{projectId}/agent-governance/missing-items`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommend-bindings`
- `POST /api/work-center/projects/{projectId}/agent-governance/recommendations:apply`

前端已新增：

- 项目工作台 `交付治理助手` 入口。
- 项目详情页 `开始交付治理` 入口。
- `/data-steward/assets/{projectId}/work/agent-governance` 页面。

已确认能力：

- 项目交付体检可展示。
- 工程主数据状态可展示。
- 文档/图纸完整率和缺失项可展示。
- 缺失项解释可展示。
- 元数据候选文件推荐可生成。
- 推荐方案包含推荐理由、置信度和风险提示。
- 未人工确认时不能批量挂接。
- 人工确认后可复用既有批量挂接能力。
- 挂接后完整率可刷新。
- 审计包含 `work.agent-governance.recommend` 和 `work.agent-governance.apply`。

## 4. 权限与安全边界复核

已确认：

- 未登录访问 G1 接口返回 `401`。
- 错误项目上下文访问返回 `403`。
- 普通项目用户不能越权访问其他项目。
- `confirmed=false` 时 `recommendations:apply` 返回 `WORK_AGENT_GOVERNANCE_CONFIRM_REQUIRED`，且不创建挂接。
- `confirmed=true` 后才进入批量挂接。

已确认未发生：

- Agent 自动写库。
- Agent 自动挂接文件。
- Agent 自动提交审核、通过审核、驳回或整改。
- Agent 自动修改部位树、节点类型或交付物标准。
- 真实 NAS 文件移动、删除、重命名、上传。
- PDF / Office / DWG / RVT / IFC 正文读取。
- Hermes memory、OpenSearch、Qdrant、MinIO documents/chunks 写入。
- 真实 BIM 轻量化或真实交付包导出。

专项脚本和手工抽查均确认响应不包含：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storageUri`
- `raw row`
- `SQL`

## 5. 非阻塞 P2

保留两个非阻塞 P2：

1. 前端生产构建仍有既有 Vite chunk size warning。
2. G1 专项脚本和页面验收会留下 `PHASE2-G1-*` 元数据与测试挂接，连续复跑会影响样板项目缺失项、完整率和交付包预检查数字。

第二项不触碰真实 NAS，不构成本批阻塞；但后续建议补一个脚本自清理或隔离测试项目，避免长期污染演示项目数据。

## 6. 主 Agent 裁决

G1 可正式收口，作为 8A 后插入的高价值可用性模块基线。

当前不自动进入下一批次。后续候选方向由用户确认：

- 若优先稳定演示与试运行：先补 G1 测试数据自清理 / 隔离测试项目。
- 若回到原二期主线：再规划 NAS 受控增删改查、BIM 真实轻量化、构件级解析或客户部署交付文档包。

在用户确认前，不启动新的开发批次。

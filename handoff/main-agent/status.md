# 主 Agent 项目状态

## 当前阶段

文档与验收口径已升级为三期路线，下一步业务开发应进入 `一期：内部 BIM 资产管理试点`。

## 当前判断

平台基础工程、`master-data`、数据管家 MVP、工作中心 MVP、智慧大屏和 3D 适配层 mock 已形成可运行闭环。现在项目重点不再是继续扩早期 MVP，而是把公司内部几百个 BIM 项目和约 10TB 模型资产先接入平台，并提前为企业内核级 agent 做数据库检索和接口对接准备。

## 最新文档基线

- 完整 PRD 与三期路线：`docs/07-complete-delivery-prd.md`
- 验收矩阵与企业 agent 对接：`docs/08-acceptance-and-agent-integration.md`
- 总体架构与系统设计：`docs/03-architecture-and-system-design.md`
- 实施路线与 agent 手册：`docs/04-rollout-and-agent-prompts.md`

## 一期业务目标

- 项目资产台账：几百个 BIM 项目统一建档，前端可增删改查。
- NAS 原地接管：不搬迁大文件，只登记元数据和路径。
- 模型资源库：记录文件名、格式、大小、版本、专业、checksum、状态、更新时间。
- 搜索与看板：支持按项目、文件名、专业、阶段检索，展示容量统计。
- 审计：导入、扫描、下载、修改、删除必须留痕。
- 企业 agent：提供稳定读模型或只读宽表，支持 `元数据 + 路径` 检索。

## 二期客户版目标

- 标准驱动交付完整闭环。
- 文件预览、下载权限分离、审核流。
- 模型轻量化在线预览。
- 构件级解析、搜索、定位、高亮和基础查看工具。
- 项目驾驶舱、报表、大屏、移动端查看。
- 客户私有化部署、备份恢复、日志诊断、健康检查和完整交付文档。

## 企业 Agent 对接裁决

- agent 权限高，可以读取数据库。
- agent 不应直接耦合平台业务底表。
- 一期必须提供稳定读模型：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
- 平台保留 REST/OpenAPI 作为长期集成边界。
- 一期落地前主 agent 必须再次确认企业 agent 技术栈、读库方式、索引方式、路径访问方式和增量同步策略。

## 当前协作规则

- 主 agent 维护需求、架构、验收、prompt 和交接文档。
- 开发 agent 和测试 agent 仍作为长期独立会话运行。
- 主 agent 不通过创建临时子 agent 代替开发 agent。
- 后续开发 agent prompt 必须引用 `docs/07` 和 `docs/08`，不能只按旧 v1 MVP 口径继续开发。

## 样板信息

- 样板账号：`platform.admin`
- 样板密码：`Admin@123`
- 样板项目：`SAMPLE-MEP-001 / 机电交付样板项目`

## Windows 迁移注意事项

迁移前必须整理项目基线、数据库迁移说明、启动命令、已实现能力、未实现能力、企业 agent 对接待确认清单，避免跨系统开发断层。

## Windows 端交接入口

Windows 端 Codex 请从以下文件开始：

- `handoff/windows-agent/project-context.md`
- `handoff/windows-agent/current-prompt.md`
- `handoff/windows-agent/migration-checklist.md`

完成后必须回写：

- `handoff/windows-agent/latest-report.md`

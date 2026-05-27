# 一期后端数据治理收口报告

日期：2026-05-09

## 1. 收口结论

一期后端数据治理正式收口。

当前批次状态：

- 批次 1：数据底座与 NAS 扫描入库，通过。
- 批次 1 尾巴：真实 NAS 目录发现、扩展分类、低价值文件治理、重扫幂等，通过。
- 批次 2：异步任务、checksum、容量统计、事件流，通过。
- 批次 3：企业 agent、API Key、审批与受控物理删除，通过。

最新测试报告：`handoff/test-agent/latest-report.md`

测试 agent 结论：批次 3 再次验证通过，当前无 P0/P1/P2，可收口一期后端数据治理。

## 2. 已完成能力

一期后端已具备以下能力：

- 项目资产台账 API。
- NAS 路径映射 API。
- CSV/xlsx 批量导入。
- NAS 扫描任务与待审核队列。
- 高置信自动入库、低置信人工审核。
- 文件分类、版本、专业、路径、大小、checksum 元数据管理。
- 真实 NAS 一级目录发现 dry-run 与受控导入。
- 低价值文件过滤、临时目录降级、重扫幂等。
- 数据库任务表与应用内 worker。
- checksum 异步补齐。
- 容量统计。
- 审计与事件流增量同步。
- MySQL 稳定 SQL View：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
- 企业 agent API Key 接入。
- API Key 项目范围授权，支持 `SPECIFIC_PROJECTS` 与受控 `ALL_PROJECTS`。
- agent 只读查询、触发任务、提交标注、提交删除申请。
- 删除申请、人工审批、逻辑删除、物理隔离、恢复、受控永久删除。
- OpenAPI/Swagger 接口文档覆盖。

## 3. 验收结果

最近一次测试 agent 再次验证结果：

- Maven 构建：通过。
- 后端启动：通过。
- 健康检查：`{"status":"UP"}`。
- `check-bim-asset-batch1.sh`：通过。
- `check-bim-asset-batch1-tail.sh`：通过。
- `check-bim-asset-batch2.sh`：通过。
- `check-bim-asset-batch3.sh`：通过。
- `delivery.engineer` 创建 `ALL_PROJECTS` API Key：已拒绝，返回 403 与 `AGENT_KEY_ALL_PROJECTS_FORBIDDEN`。
- `platform.admin` 创建并撤销 `ALL_PROJECTS` API Key：通过。
- 静态检查：未发现用户名硬编码、明文 API Key 落库、旧 Flyway 迁移修改、前端扩张、二期 BIM 能力混入。

## 4. 收口边界

本次收口只代表一期后端数据治理闭环完成，不代表完整卓羽智能数据中台客户版完成。

不包含：

- 前端资产管理完整页面。
- 真实 BIM 模型轻量化在线预览。
- 构件级解析、构件搜索、定位、高亮。
- 文件正文解析和全文检索。
- 客户交付安装包、授权许可、备份恢复完整工具链。
- 移动端。
- 二期客户交付完整版验收。

## 5. 后续建议

下一步不要继续扩大一期后端范围，建议在以下方向中选择：

1. 真实 NAS 小批量试点导入：先导入 20 个真实项目，验证路径、权限、审计和容量统计。
2. 企业 agent 对接确认：确认 agent 技术栈、读库方式、索引方式、路径访问方式、事件流同步策略。
3. 一期阶段总验收材料：整理演示步骤、接口清单、数据库视图说明、样例 SQL、运维启动说明。
4. 二期客户交付完整版规划：开始拆解轻量化预览、构件级能力、文件预览、审核流、部署运维。

## 6. 主 Agent 裁决

一期后端数据治理已收口。

除 P0/P1 缺陷修复外，不再继续向一期后端治理追加新能力。后续新能力必须进入明确的新阶段或新批次，并先更新 PRD、验收标准和开发/测试 agent prompt。

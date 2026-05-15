# 真实 NAS 待审核处理与 P1 修复报告

日期：2026-05-09  
主 agent：Codex

## 1. 本轮边界

- 本轮继续只处理平台数据库元数据，不修改、不移动、不删除 NAS 原文件。
- 本轮处理对象为 `100-深圳市二十八高项目` 的待审核候选，以及企业 agent 模型读模型 P1。
- 本轮未重新扫描 `101-C塔`。

## 2. 待审核候选处理

项目：`100-深圳市二十八高项目`  
项目 ID：`504`  
扫描任务：`415 / REAL_NAS_100`

处理前：

- 待审核总数：`293`
- 低置信图纸：`285`
- 低置信模型：`8`

处理策略：

- 这批候选都属于项目 `100` 的真实 NAS 项目路径。
- 它们被降级为待审核的主要原因是路径中包含 `新建文件夹` 等低置信目录。
- 按一期“资产先找得到、路径可追溯、后续再治理”的目标，本轮将 293 条全部审核通过。
- 审核后仍保留 `LOW` 置信度和 `REVIEW` 来源，便于后续继续筛查。

处理后：

- 待审核：`0`
- 审核通过：`293`
- 正式文件资产新增：`293`
- 其中图纸：`285`
- 其中模型：`8`

`100` 项目当前正式资产：

- 文件资产总数：`3961`
- 模型文件：`156`
- 图纸文件：`3805`
- 总容量：`41,863,763,688 bytes`

## 3. P1 修复：ModelAssetView 覆盖 NAS 模型

问题：

- 修复前 `ModelAssetView` 主要基于 `data_model_integrations`。
- 一期 NAS 扫描登记的 `.rvt/.ifc/.nwd/.nwc` 模型文件，如果尚未进入模型集成流程，企业 agent 直接查 `ModelAssetView` 会漏掉。

修复：

- 新增 Flyway 迁移：`V13__model_asset_view_include_nas_files.sql`
- 不修改旧迁移。
- `ModelAssetView` 现在覆盖：
  - 已进入模型集成流程的模型。
  - `data_file_resources.file_kind='MODEL'` 且尚未进入模型集成流程的 NAS 模型文件。
- 继续保留原字段：`model_id`、`file_id`、`project_code`、`model_name`、`model_format`、`discipline`、`version_no`、`preview_available`、`lightweight_status`、`component_index_status`、`storage_path`、`updated_at`。

验证：

- Maven 构建通过：`./mvnw -pl delivery-app -am -DskipTests package`
- 后端启动成功，Flyway 已应用 `version=13`
- 健康检查返回 `UP`
- `ModelAssetView` 查询结果：
  - `100`：`156` 个模型，其中 `rvt 144`、`nwc 8`、`nwd 4`
  - `105`：`198` 个模型，均为 `rvt`

## 4. 文档更新

已更新：

- `docs/08-acceptance-and-agent-integration.md`
- `handoff/main-agent/enterprise-agent-db-mcp-integration-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/backlog.md`

## 5. 当前结论

- `100` 的待审核候选已经清零。
- `100`、`105` 的 NAS 模型文件现在可以通过 `ModelAssetView` 被企业 agent 稳定检索。
- P1 已修复并验证通过。
- `101-C塔` 仍保持暂停状态，后续需先解决开发库历史测试编码冲突。

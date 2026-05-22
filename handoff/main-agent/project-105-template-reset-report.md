# 105 项目演示模板数据重置报告

时间：2026-05-22

## 背景

用户确认：当前工程主数据仍处于架空状态，105 项目中通过初始化向导载入的模板属于演示数据，没有真实数字化交付作用，继续保留会误导员工认为该项目已经完成交付规则配置。

## 处理对象

- 真实项目：`503 / 105`
- 项目名称：深圳市二十八高项目
- 处理范围：仅软删除该项目下的演示工程主数据和由其派生的交付记录。

## 处理方式

已在处理前备份相关数据：

- `tmp/backups/project-503-before-template-reset-20260522153500.sql`

本次采用软删除方式：

- 不物理删除数据库记录。
- 不触碰真实 NAS 文件。
- 不删除真实资产文件元数据。
- 不修改项目资产台账。

## 已软删除的有效业务数据

- `masterdata_section_nodes`
- `masterdata_node_types`
- `masterdata_deliverable_definitions`
- `masterdata_deliverable_types`
- `masterdata_deliverable_attributes`
- `masterdata_directory_templates`
- `work_delivery_bindings`
- `work_rectifications`
- `work_delivery_package_drafts`

## 保留的数据

- `data_file_resources`：105 项目仍保留 2928 条资产文件记录。
- 审计记录保留。
- 历史 review record 不做物理删除，仅因 binding 已软删除而不再参与有效交付链路。

## 验证结果

处理后 105 项目有效数据计数：

- 工程部位树：0
- 节点类型：0
- 交付物定义：0
- 交付物类型：0
- 交付物属性：0
- 目录模板：0
- 交付挂接：0
- 整改记录：0
- 交付包草案：0
- 文件资产：2928

接口验证：

- `GET /api/master-data/projects/503/standard-status`
  - `hasSectionTree=false`
  - `hasNodeTypes=false`
  - `nodeTypesLocked=false`
  - `deliverableStandardReady=false`
- `GET /api/master-data/projects/503/initialization/status`
  - `ready=false`
  - 当前步骤回到 `SECTION_TREE`
  - 阻塞项提示需要建立部位树、配置节点类型、补齐交付物标准和目录模板。
- `GET /api/work-center/projects/503/delivery-package/prepare?viewType=DOCUMENT&targetType=SECTION`
  - `totalCount=0`
- `GET /api/work-center/projects/503/delivery-package/prepare?viewType=DRAWING&targetType=SECTION`
  - `totalCount=0`

## 审计

已写入审计：

- `module_code=main-agent`
- `action_code=master-data.template-demo.reset`
- `target_type=PROJECT`
- `target_id=503`
- `trace_id=manual-reset-105-template`

## 主 Agent 裁决

105 项目不再显示为已完成工程主数据或交付标准配置。后续应将 105 作为真实项目接入样本，重新从：

`真实资产目录 -> 工程主数据草案 -> 人工确认 -> 标准驱动交付`

这条链路建立可解释、可维护的工程主数据，而不是重新套用演示模板。

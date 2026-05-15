# 企业 Agent DB-2 数据库连接与视图合同确认

日期：2026-05-09

用途：用于 Hermes_memory / 企业 Agent 开发团队在 DB-2 阶段进行数据库协议握手，确认连接方式、只读权限、稳定 View、字段清单、权限默认值和增量游标语义。

说明：以下信息基于当前项目文档与仓库配置整理。未在项目文档中明确提供的信息，会标记为“当前未提供”，避免后续产生协议误解或实现返工。

## 0. 四项最小联调信息

本节为数据库团队给 Hermes_memory / 企业 Agent 团队的最小握手口径。

| 项目 | 当前可给出的内容 | 如果当前没有，握手规定 |
|---|---|---|
| dev DSN | 当前一期正式联调路径为本机同环境：平台数据库、企业 Agent / 本地 Hermes 均运行在同一台机器。DSN 已确认：`jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8`。 | 企业 Agent 在同机联调时不得自行猜测其他库名、端口或账号，不得使用应用主账号。 |
| shared-dev / staging DSN | 当前不是 DB-2 首轮联调前置条件。 | 只有多人远程协作、持续测试、演示或客户交付前类生产验证时，才按 `enterprise-agent-db2-staging-shared-dev-ops-request.md` 由平台/运维交付。 |
| 只读账号 | 本机 dev 已创建 `hermes_agent_ro`。 | 企业 Agent 必须使用该专用只读账号，只授予四个稳定 View 的 `SELECT` 权限，不授予业务底表权限和任何写权限；密码通过安全渠道交付，不写入文档。 |
| 允许访问的 database/schema 名 | 当前项目数据库名：`delivery_platform`。 | 如未来 shared-dev / staging 使用不同库名，必须由数据库团队在 DSN 中明确；Hermes_memory 配置不得硬编码多个候选库名自动探测。 |
| 是否允许 `LIMIT 30` 读取四个 View | 本机 dev 同环境联调允许内部授权 `LIMIT 30`。shared-dev / staging 默认 `STRUCTURE_ONLY`。 | 业务负责人和数据负责人未书面确认前，不允许 shared-dev / staging 读取真实样例；如需样例，必须提供脱敏样例或单独审批。 |

建议创建只读账号 SQL：

```sql
CREATE USER IF NOT EXISTS 'hermes_agent_ro'@'%' IDENTIFIED BY '<replace-with-secret>';

GRANT SELECT ON delivery_platform.ProjectAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.FileAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.ModelAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.AuditEventView TO 'hermes_agent_ro'@'%';

FLUSH PRIVILEGES;
```

字段握手顺序：

```sql
SELECT 1;
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type = 'VIEW';
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
```

shared-dev / staging 默认字段结构握手：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

## 1. dev / shared-dev / staging DSN 或连接方式

当前一期 DB-2 首轮联调按本机同环境执行：平台数据库运行在本机 Docker MySQL，企业 Agent 后续更新到本地 Hermes 后也运行在同一环境。该模式下不需要单独获取 shared-dev 账号，也不需要等待运维提供远程 DSN。

shared-dev / staging 不再作为当前 DB-2 联调阻塞项，只在以下场景触发：

- 多人需要同时远程接入同一套数据库。
- 需要脱离主开发机做持续测试或演示。
- 需要客户交付前的类生产验证。
- 需要验证部署、备份恢复、升级和环境隔离。

触发后，shared-dev / staging 必须按以下执行单由平台/运维提供：

```text
handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md
```

dev 默认配置：

```text
DB: MySQL 8.0.39
JDBC: jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
username: delivery
password: <application-dev-password-hidden>
```

对应配置来源：

```text
backend/delivery-app/src/main/resources/application.yml
infra/docker-compose.yml
```

注意：

- 平台应用主账号是本地应用开发账号，不等同于企业 Agent 联调只读账号。
- shared-dev / staging 联调 DSN 只有在触发远程协作或交付验证时，才需要由平台侧或运维侧按执行单另行提供。
- 企业 Agent 不应使用应用主账号直连。

## 2. 只读账号和权限范围

当前项目文档明确要求：

- 企业 Agent 数据库连接必须使用只读账号。
- 只读账号只允许读取稳定 SQL View。
- 默认不开放业务底表权限。
- REST 动作必须使用平台 API Key。
- API Key 按项目范围授权。

本机 dev 已创建 `hermes_agent_ro`。企业 Agent / 本地 Hermes 同机联调使用该账号。shared-dev / staging 账号不得写入 migration 或 Flyway 历史迁移，由平台/运维在目标库执行授权。

建议只读账号权限范围：

```sql
GRANT SELECT ON delivery_platform.ProjectAssetView TO '<agent_readonly_user>'@'%';
GRANT SELECT ON delivery_platform.FileAssetView TO '<agent_readonly_user>'@'%';
GRANT SELECT ON delivery_platform.ModelAssetView TO '<agent_readonly_user>'@'%';
GRANT SELECT ON delivery_platform.AuditEventView TO '<agent_readonly_user>'@'%';
```

不得授予：

```text
INSERT / UPDATE / DELETE / DROP / ALTER
业务底表 SELECT
NAS 文件写权限
```

## 3. 允许访问的 View / table 名称

当前一期稳定读模型只允许访问以下 SQL View：

```text
ProjectAssetView
FileAssetView
ModelAssetView
AuditEventView
```

不建议企业 Agent 直接访问业务底表，例如：

```text
core_projects
data_file_resources
data_model_integrations
core_audit_logs
data_asset_scan_tasks
data_asset_scan_candidates
```

如需写入操作、触发任务、提交删除申请、提交标注，应走平台 REST/OpenAPI + API Key，不走 DB 写入。

## 4. source_system 固定值

确认使用：

```text
delivery_platform
```

同一环境内不得混用 `platform`、`digital_delivery`、`delivery` 等其他值。

## 5. source_view 枚举确认

当前 DB-2 阶段确认枚举为：

```text
ProjectAssetView
FileAssetView
ModelAssetView
AuditEventView
```

新增 View 必须走 contract version review。

## 6. 字段清单与当前 contract version

当前代码中 SQL View 字段如下。

### ProjectAssetView

```text
project_id
project_code
project_name
project_stage
discipline_scope
manager_name
owner_org_name
asset_status
model_file_count
total_size_bytes
last_asset_updated_at
```

### FileAssetView

```text
file_id
project_id
project_code
project_name
file_name
file_ext
file_kind
discipline
version_no
size_bytes
checksum
storage_provider
storage_path
logical_path
source_type
process_status
created_at
updated_at
```

### ModelAssetView

```text
model_id
file_id
project_code
model_name
model_format
discipline
version_no
preview_available
lightweight_status
component_index_status
storage_path
updated_at
```

### AuditEventView

```text
event_id
project_id
module_code
action_code
target_type
target_id
operator_id
summary
created_at
```

当前项目代码未内置 `contract_version` 字段。

建议 DB-2 mirror / checkpoint 使用以下 contract version 作为握手值：

```text
delivery_platform.asset_views.v1
```

注意：这是建议冻结值，不是当前 SQL View 内已有字段。

## 7. 脱敏样例 / limit 查询

当前项目文档未提供 10-30 条固定脱敏样例。

本机 dev 允许企业 Agent 在公司内部授权范围内使用只读账号对四个 View 做 `LIMIT` 查询获取样例：

```sql
SELECT * FROM ProjectAssetView LIMIT 30;
SELECT * FROM FileAssetView LIMIT 30;
SELECT * FROM ModelAssetView LIMIT 30;
SELECT * FROM AuditEventView LIMIT 30;
```

shared-dev / staging 默认不允许真实样例读取，只允许 `STRUCTURE_ONLY`：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

若 shared-dev / staging 需要 `LIMIT 30`，必须由业务负责人和数据负责人书面确认；否则提供脱敏样例或脱敏 staging 数据。

真实项目名、文件名、NAS 路径不得外发，不得写入外部云服务，不得写入 Qdrant / OpenSearch / 向量库 / 搜索库 / 长期 memory / 外部观测日志。

当前一期内部文档允许高权限企业 Agent 读取真实 NAS 路径，但客户版后续默认不直接暴露底层路径，应改为平台授权下载或访问入口。

## 8. 权限字段缺失时是否默认 DENIED

确认：必须默认 `DENIED`。

当前四个 View 里没有稳定提供：

```text
permission_tags
project_scope
confidentiality_level
```

因此 Hermes_memory / 企业 Agent mirror 层必须 fail closed：

```text
permission_status = DENIED
```

只有当平台 API Key、项目范围授权或后续权限字段明确证明用户可见时，才允许进入 prompt / evidence / answer。

不得因为字段缺失而默认可见。

## 9. event_id / updated_at / created_at 游标语义

### event_id

`AuditEventView.event_id` 是当前最适合作为增量同步主游标的字段。

来源：

```text
core_audit_logs.id
```

建议查询方式：

```sql
SELECT *
FROM AuditEventView
WHERE event_id > :last_event_id
ORDER BY event_id ASC
LIMIT :limit;
```

### created_at

`AuditEventView.created_at` 表示审计事件发生时间，可作为时间窗口补偿游标或 overlap window 使用。

建议不要单独依赖 `created_at`，因为同一时间可能有多条事件。

推荐组合：

```text
event_id 主游标 + created_at overlap window
```

### updated_at

`FileAssetView.updated_at` 和 `ModelAssetView.updated_at` 表示对应资产记录更新时间，可用于快照校验、补偿查询和首次全量同步后的差异检查。

但项目文档明确：增量同步不应只依赖 `updated_at`，应以审计 / 事件流为主。

### ProjectAssetView.last_asset_updated_at

`ProjectAssetView.last_asset_updated_at` 是聚合字段，来自项目下模型文件的最大更新时间。

它适合用于项目资产概览刷新，不建议作为精确增量同步游标。

## 10. 当前可承诺边界

当前可承诺：

```text
MySQL 8.0.39
dev 本地 JDBC 配置
本机 dev 只读账号 hermes_agent_ro
shared-dev / staging 后置执行单与默认 STRUCTURE_ONLY 策略
四个稳定 SQL View
source_system = delivery_platform
source_view 四枚举
字段清单如上
权限缺失默认 DENIED
event_id 作为主增量游标
updated_at / created_at 作为辅助窗口
```

当前不能承诺：

```text
shared-dev / staging DSN 已实际交付
shared-dev / staging 只读 DB 账号密码已实际交付
生产权限字段 permission_tags / project_scope / confidentiality_level
View 内置 contract_version 字段
shared-dev / staging 真实样例读取已获业务负责人和数据负责人审批
企业 Agent 可直接读业务底表
企业 Agent 可直接写 DB 或操作 NAS
```

请 Hermes_memory / 企业 Agent 团队按以上边界实现 DB-2 proof-of-contract，不要进入 DB-3 retrieval / embedding / OpenSearch / Qdrant 写入，直到平台侧单独授权。

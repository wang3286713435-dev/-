# 企业 Agent DB-2 shared-dev / staging 后置运维执行单

日期：2026-05-09

用途：当 DB-2 从本机同环境联调升级到 shared-dev / staging 时，关闭 DSN、只读账号、样例读取许可、外发限制和权限默认值的悬空项。本文件不包含 shared-dev / staging / production 密码；目标环境密码必须通过安全渠道交付。

## 1. 当前裁决

```yaml
local_same_host_db2_status: "CURRENT_PHASE1_PATH"
staging_shared_dev_status: "DEFERRED_UNTIL_REMOTE_COLLAB_OR_STAGING_REQUIRED"
sample_read_policy_default: "STRUCTURE_ONLY"
real_sample_read_policy: "DENIED_UNTIL_BUSINESS_AND_DATA_OWNER_APPROVE"
external_persistence_policy: "FORBIDDEN"
permission_default: "DENIED"
business_table_access: "FORBIDDEN"
write_access: "FORBIDDEN"
nas_write_access: "FORBIDDEN"
```

结论：

- 当前一期 DB-2 联调采用本机同环境：平台数据库运行在本机 Docker MySQL，企业 Agent 后续更新到本地 Hermes 后也运行在同一环境。
- 因此当前不强制获取 shared-dev / staging 账号，企业 Agent 使用本机 `hermes_agent_ro` 只读账号即可进行 DB-2 首轮联调。
- shared-dev / staging 未提供 DSN 和只读密钥前，企业 Agent 团队不得自行猜测连接地址、扫描端口、复用本机 dev 密码或复用应用主账号。
- shared-dev / staging 默认只允许字段结构握手，不允许 `LIMIT 30` 真实样例读取。
- 如业务负责人和数据负责人没有书面确认允许真实样例，Hermes_memory 必须使用 `WHERE 1 = 0` 或 `SHOW COLUMNS`。
- 当前四个 View 没有稳定权限字段，Hermes mirror 层必须 fail closed，默认 `permission_status = DENIED`。
- 真实项目名、文件名、NAS 路径不得外发，不得写入外部云服务，不得进入客户材料。

## 2. 触发条件与开通顺序

当前本机同环境联调不需要开通 shared-dev / staging。只有出现以下任一情况时，才进入本节流程：

- 企业 Agent、平台、测试或运维需要多人远程协作。
- 需要脱离主开发机做持续测试、自动化验证或演示。
- 需要客户交付前的类生产环境验证。
- 需要验证部署、备份恢复、升级、网络隔离和权限隔离。

触发 shared-dev / staging 后，必须按以下顺序开通。任一步未完成，企业 Agent 不得进入目标环境 DB-2 对接：

1. 准备 shared-dev 或 staging MySQL 环境。
2. 部署或迁移当前平台数据库结构，确保四个稳定 View 已创建。
3. 创建 `hermes_agent_ro` 或平台/运维指定的等价只读账号。
4. 只授权四个稳定 View 的 `SELECT`，不得授权业务底表或写权限。
5. 验证业务底表不可读，至少覆盖 `core_projects`、`data_file_resources`、`core_audit_logs`。
6. 填写 DSN 交付单，明确 host、port、database、JDBC URL、网络访问方式和 TLS 要求。
7. 密码通过安全渠道单独交给企业 Agent，不得写入仓库、文档或群聊明文。
8. 测试 agent 复验 DSN、View 可读、业务底表拒绝和样例读取策略均通过后，企业 Agent 才能开始 DB-2 对接。

## 3. 平台/运维必须提供的 DSN 交付单

平台/运维提供 shared-dev / staging 时，必须按以下格式交付，不能只口头提供 host：

```yaml
environment: "shared-dev 或 staging"
db_type: "mysql"
db_version: "<target-mysql-version>"
host: "<provided-by-platform-ops>"
port: 3306
database: "<target-database-name>"
jdbc_url: "jdbc:mysql://<host>:<port>/<database>?useSSL=<true|false>&allowPublicKeyRetrieval=<true|false>&serverTimezone=UTC&characterEncoding=utf8"
network_access: "内网 / VPN / 跳板机 / SSH tunnel / k8s service / docker network"
tls_required: true
readonly_user: "hermes_agent_ro 或平台运维指定账号名"
readonly_secret_delivery: "通过安全渠道交付；不得写入仓库"
allowed_schema: "<target-database-name>"
allowed_views:
  - ProjectAssetView
  - FileAssetView
  - ModelAssetView
  - AuditEventView
sample_read_policy: "STRUCTURE_ONLY 或 ALLOW_LIMIT_30_AFTER_BUSINESS_AND_DATA_OWNER_SIGNOFF"
sample_data_contains_real_project_names: "<true|false>"
sample_data_contains_real_nas_paths: "<true|false>"
contract_version: "delivery_platform.asset_views.v1"
source_system: "delivery_platform"
permission_default: "DENIED"
```

## 4. shared-dev / staging 只读账号授权 SQL

以下 SQL 由平台/运维在目标 MySQL 上执行。`<target_database>` 和 `<strong-secret-from-vault>` 必须替换为目标环境真实值；不要复用本机 dev 密码。

```sql
CREATE USER IF NOT EXISTS 'hermes_agent_ro'@'%' IDENTIFIED BY '<strong-secret-from-vault>';
ALTER USER 'hermes_agent_ro'@'%' IDENTIFIED BY '<strong-secret-from-vault>';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'hermes_agent_ro'@'%';

GRANT SELECT ON <target_database>.ProjectAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON <target_database>.FileAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON <target_database>.ModelAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON <target_database>.AuditEventView TO 'hermes_agent_ro'@'%';

FLUSH PRIVILEGES;
```

禁止授予：

```text
SELECT ON 业务底表
INSERT
UPDATE
DELETE
CREATE
ALTER
DROP
GRANT
FILE
SUPER / SYSTEM_USER / ROLE_ADMIN 等管理权限
NAS 文件写权限
```

## 5. shared-dev / staging 验证 SQL

用 `hermes_agent_ro` 在目标环境执行：

```sql
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type = 'VIEW';
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
SELECT COUNT(*) FROM ProjectAssetView;
SELECT COUNT(*) FROM FileAssetView;
SELECT COUNT(*) FROM ModelAssetView;
SELECT COUNT(*) FROM AuditEventView;
```

必须拒绝：

```sql
SELECT COUNT(*) FROM core_projects;
SELECT COUNT(*) FROM data_file_resources;
SELECT COUNT(*) FROM core_audit_logs;
```

如任一业务底表可读，平台/运维必须立即撤销权限，重新执行授权 SQL，并重新验证。

## 6. 样例读取裁决表

默认裁决：

```yaml
local-dev: "ALLOW_LIMIT_30_INTERNAL_ONLY"
shared-dev: "STRUCTURE_ONLY"
staging: "STRUCTURE_ONLY"
production: "STRUCTURE_ONLY_UNLESS_SEPARATE_SECURITY_APPROVAL"
```

shared-dev / staging 若需开放 `LIMIT 30`，必须补齐以下记录：

```yaml
environment: "<shared-dev 或 staging>"
decision: "ALLOW_LIMIT_30"
approver_business_owner: "<name>"
approver_data_owner: "<name>"
approval_date: "YYYY-MM-DD"
allowed_views:
  - ProjectAssetView
  - FileAssetView
  - ModelAssetView
  - AuditEventView
allowed_usage: "仅限公司内部授权 DB-2 字段握手和只读联调"
forbidden_usage: "不得外发；不得写入外部云服务；不得进入客户材料；不得持久化到向量库、搜索库或观测日志"
```

未补齐审批记录时，只允许：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

## 7. Hermes mirror 权限默认值

当前四个稳定 View 不包含以下字段：

```text
permission_tags
project_scope
confidentiality_level
```

因此 Hermes mirror 层必须写入以下默认值：

```yaml
permission_status: "DENIED"
permission_reason: "NO_STABLE_PERMISSION_FIELDS_IN_SOURCE_VIEW"
can_enter_prompt: false
can_enter_evidence: false
can_enter_answer: false
```

只有平台 API Key、项目授权范围或后续稳定权限字段明确证明用户可见时，才能把资产进入 prompt、evidence 或 answer。

## 8. 外发与外部持久化禁止清单

企业 Agent / Hermes_memory 读取本机 dev、shared-dev 或 staging 样例时，以下行为一律禁止，除非另有单独书面审批：

- 把真实项目名、文件名、NAS 路径发送到外部云服务。
- 把真实样例写入外部日志、外部观测系统、外部调试面板。
- 把真实样例写入 Qdrant、OpenSearch、向量库、搜索库或长期 memory。
- 把真实样例放入客户演示材料、客户交付文档或截图。
- 绕过平台权限把 catalog-only 资产作为正文 evidence。

DB-2 阶段只做数据库协议握手、字段映射和只读 proof-of-contract，不进入 DB-3 retrieval / embedding / external indexing。

## 9. 关闭标准

这些问题只有在以下条件全部满足后才算对 shared-dev / staging 真正关闭：

- shared-dev 或 staging MySQL 环境已准备完成。
- 当前平台数据库结构已部署或迁移完成，四个稳定 View 已存在。
- 平台/运维提供目标环境 DSN 交付单。
- 平台/运维创建只读账号，并通过安全渠道交付密码。
- 四个 View 正向验证通过。
- 三张业务底表反向拒绝验证通过。
- 业务负责人和数据负责人确认样例读取策略；默认 `STRUCTURE_ONLY`。
- Hermes mirror 层确认权限缺失默认 `DENIED`。
- 企业 Agent 团队确认不外发、不外部云持久化、不写 NAS、不写数据库。
- 测试 agent 复验通过。

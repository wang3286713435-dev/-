# DB-2 只读账号运维报告与企业 Agent 接入手册

执行日期：2026-05-09

适用对象：Hermes_memory / 企业 Agent DB-2 开发团队。

本文是企业 Agent 只读接入卓羽智能数据中台 MySQL View 的主入口文档。DB-2 阶段只做数据库连接、字段握手、只读 proof-of-contract 和 mirror schema 准备；不得进入 DB-3 retrieval、embedding、OpenSearch、Qdrant 或外部持久化。

## 1. 当前结论

```yaml
environment: local-dev
db_type: mysql
db_version: 8.0.39
database: delivery_platform
container: delivery-mysql
container_status: "Up, healthy"
contract_version_current_verified: "delivery_platform.asset_views.v1.1"
contract_version_next_ready_in_code: "delivery_platform.asset_views.v1.1"
v1_1_migration: "V17__asset_views_v1_1.sql"
v1_1_db_apply_status: "APPLIED_TO_TARGET_DB"
backend_deploy_status: "DEPLOYED_TO_TEST_MACHINE_OR_TARGET_ENV"
contract_version_available: "delivery_platform.asset_views.v1.1"
env_contract_version_update_allowed: true
structure_only_smoke_allowed: true
real_rows_allowed: false
writes_allowed: false
v1_1_structure_only_smoke_status: "WHERE_1_EQ_0_PASSED"
source_system: "delivery_platform"
readonly_user: "hermes_agent_ro"
readonly_password_dev_only: "<dev-secret-from-safe-channel>"
permission_default: "DENIED"
business_table_access: "FORBIDDEN"
write_access: "FORBIDDEN"
nas_write_access: "FORBIDDEN"
local_dev_ready_for_db2: true
local_same_host_db2_status: "CURRENT_PHASE1_PATH"
staging_shared_dev_status: "DEFERRED_UNTIL_REMOTE_COLLAB_OR_STAGING_REQUIRED"
```

本机 dev 可以交给企业 Agent 团队继续 DB-2 开发。当前规划下，平台数据库与企业 Agent / 本地 Hermes 运行在同一台本机环境中，本机同环境联调就是一期 DB-2 的正式路径。范围仅限只读连接、字段握手、结构验证和授权样例读取。

2026-05-12 已完成 `delivery_platform.asset_views.v1.1` 目标环境部署：`V17__asset_views_v1_1.sql` 已由 Flyway 应用到目标 DB，后端测试实例已启动并通过健康检查，Hermes 测试机安全 env 的 contract version 已更新到 v1.1 期望值。部署过程未读取真实行、未执行 `LIMIT 30`、未输出 secret / raw row / 真实项目名 / 文件名 / NAS 路径。

密码说明：`readonly_password_dev_only` 不写入仓库文档，必须通过安全渠道交付。若后续开通 shared-dev / staging / production，必须生成环境专用密码，不得复用本机密码。

本机当前密码已完成轮换，并保存于 macOS 钥匙串：

```yaml
keychain_service: "delivery-platform-hermes-agent-ro-local-dev"
keychain_account: "hermes_agent_ro"
```

## 2. 本机 dev DSN

JDBC URL：

```text
jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
```

连接参数：

```yaml
host: "localhost"
port: 3306
database: "delivery_platform"
jdbc_params:
  useSSL: false
  allowPublicKeyRetrieval: true
  serverTimezone: "UTC"
  characterEncoding: "utf8"
network_access: "本机 Docker MySQL 暴露 3306；容器名 delivery-mysql；不需要 VPN"
tls_required: false
```

企业 Agent 应使用：

```text
username: hermes_agent_ro
password: <dev-secret-from-safe-channel>
```

企业 Agent 禁止使用平台应用主账号：

```text
username: delivery
password: <application-dev-password-hidden>
```

平台应用主账号只用于本机应用开发验证，不是企业 Agent 联调账号。

## 3. 允许访问的稳定 View

只允许读取以下四个 View：

```text
delivery_platform.ProjectAssetView
delivery_platform.FileAssetView
delivery_platform.ModelAssetView
delivery_platform.AuditEventView
```

企业 Agent 不得读取业务底表，不得写数据库，不得写 NAS 文件。

## 4. 授权 SQL 摘要

本轮已重置 `hermes_agent_ro` 本机 dev 临时密码，并先撤销旧权限，再只授予四个稳定 View 的 `SELECT`。

```sql
CREATE USER IF NOT EXISTS 'hermes_agent_ro'@'%' IDENTIFIED BY '<dev-only-password>';
ALTER USER 'hermes_agent_ro'@'%' IDENTIFIED BY '<dev-only-password>';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'hermes_agent_ro'@'%';

GRANT SELECT ON delivery_platform.ProjectAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.FileAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.ModelAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.AuditEventView TO 'hermes_agent_ro'@'%';

FLUSH PRIVILEGES;
```

当前 grants 摘要：

```text
GRANT USAGE ON *.* TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`AuditEventView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`FileAssetView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`ModelAssetView` TO `hermes_agent_ro`@`%`
GRANT SELECT ON `delivery_platform`.`ProjectAssetView` TO `hermes_agent_ro`@`%`
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

## 5. 企业 Agent 首次握手流程

企业 Agent 拿到 DSN 和只读账号后，按以下顺序执行。

### 5.1 连通性与当前库

```sql
SELECT 1;
SELECT DATABASE();
```

期望结果：

```text
SELECT 1 -> 1
SELECT DATABASE() -> delivery_platform
```

### 5.2 View 发现

```sql
SHOW FULL TABLES WHERE Table_type = 'VIEW';
```

本机 dev 期望至少包含：

```text
AuditEventView
FileAssetView
ModelAssetView
ProjectAssetView
```

只读账号不应看到或读取业务底表。若发现可以读取业务底表，应立即停止联调并通知平台/运维撤权。

### 5.3 字段握手

```sql
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
```

### 5.4 行数验证

```sql
SELECT COUNT(*) FROM ProjectAssetView;
SELECT COUNT(*) FROM FileAssetView;
SELECT COUNT(*) FROM ModelAssetView;
SELECT COUNT(*) FROM AuditEventView;
```

本机 dev 当前验证结果：

```text
ProjectAssetView -> 518
FileAssetView    -> 46,342
ModelAssetView   -> 7,822
AuditEventView   -> 57,301
```

行数会随本机 dev 数据变化而变化。企业 Agent 不应把这些数字硬编码为断言，只应把查询成功作为权限和 View 存在性验证。

## 6. 字段合同

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

用途：项目资产概览、项目级检索入口、资产统计快照。

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

用途：文件资产 catalog、路径检索、文件类型和专业过滤、metadata mirror。

注意：`storage_path` 在本机 dev 可能是真实 NAS 路径，属于内部敏感元数据。

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

用途：模型资产 catalog、模型格式过滤、轻量化和构件索引状态占位。

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

用途：事件流增量同步、mirror checkpoint、审计追踪。

### 6.1 v1.1 新增字段合同

`delivery_platform.asset_views.v1.1` 在四个稳定 View 上新增以下结构字段：

```text
permission_tags
confidentiality_level
last_seen_at
lifecycle_status
index_eligibility
```

`ModelAssetView` 额外补齐：

```text
project_id
```

REST contract 同步暴露以下字段：

```text
permissionTags
projectScope
confidentialityLevel
lastSeenAt
lifecycleStatus
indexEligibility
```

字段落点：

```yaml
permission_tags: "View + REST；静态粗粒度治理标签，不表达最终用户可见性"
project_scope: "REST / API Key 调用者上下文；不在静态 View 中伪造"
confidentiality_level: "View + REST；默认 UNKNOWN"
last_seen_at: "View + REST；由 last_verified_at / updated_at / created_at 派生"
lifecycle_status: "View + REST；短期枚举 active / archived / unknown / deleted_candidate / stale_unverified"
index_eligibility: "View + REST；默认 catalog_only"
ModelAssetView.project_id: "View + REST；用于模型资产项目归属"
```

`permission_tags` 固定格式：

```text
SOURCE_SYSTEM:delivery_platform
SOURCE_VIEW:<ProjectAssetView|FileAssetView|ModelAssetView|AuditEventView>
ASSET_KIND:<PROJECT|FILE|MODEL|AUDIT_EVENT>
PROJECT:<project_id>
CONFIDENTIALITY:<UNKNOWN|INTERNAL>
INDEX_ELIGIBILITY:<catalog_only|preview_allowed|full_text_allowed|semantic_allowed>
```

v1.1 不新增真实样例授权，不授权业务底表，不授权写入，不触发 Hermes mirror / indexing。Hermes 测试机在目标环境完成迁移和部署后，可以重新执行：

```text
/Users/Weishengsu/Hermes_memory/docs/CODEX_DB_V11_STRUCTURE_ONLY_SMOKE_PROMPT.md
```

## 7. 增量同步与 checkpoint

主游标：

```text
AuditEventView.event_id
```

推荐查询：

```sql
SELECT *
FROM AuditEventView
WHERE event_id > :last_event_id
ORDER BY event_id ASC
LIMIT :limit;
```

辅助字段：

```text
AuditEventView.created_at
FileAssetView.updated_at
ModelAssetView.updated_at
ProjectAssetView.last_asset_updated_at
```

规则：

- 增量同步以 `AuditEventView.event_id` 为主。
- `created_at` 只作为 overlap window 辅助，不应单独作为游标。
- `FileAssetView.updated_at` 和 `ModelAssetView.updated_at` 可用于首次快照后的补偿查询和校验。
- `ProjectAssetView.last_asset_updated_at` 是聚合更新时间，适合刷新项目概览，不适合作为精确同步游标。

Hermes_memory 应维护自己的 sync checkpoint，不得回写平台数据库。

## 8. 样本读取策略

本机 dev：允许企业 Agent / Hermes_memory 在公司内部授权联调中执行四个 View 的 `LIMIT 30` 样例读取。

```sql
SELECT * FROM ProjectAssetView LIMIT 30;
SELECT * FROM FileAssetView LIMIT 30;
SELECT * FROM ModelAssetView LIMIT 30;
SELECT * FROM AuditEventView LIMIT 30;
```

当前本机 dev 已导入真实公司内部 NAS 元数据，样例可能包含：

- 真实项目名。
- 真实文件名。
- 真实 NAS 路径。

限制：

- 不得外发。
- 不得写入外部云服务。
- 不得进入客户材料。
- 不得写入 Qdrant、OpenSearch、向量库、搜索库、长期 memory 或外部观测日志。
- 如果企业 Agent 的日志、调试面板、向量化或观测链路会保存样本内容，应改为 `STRUCTURE_ONLY`。

shared-dev / staging：当前不是一期 DB-2 首轮联调前置条件；只有多人远程协作、持续测试、演示或客户交付前类生产验证时才开通。开通后默认 `STRUCTURE_ONLY`。业务负责人和数据负责人未书面确认前，不允许 `LIMIT 30` 真实样例。

## 9. STRUCTURE_ONLY 模式

当不允许读取真实样例时，企业 Agent 只能执行字段结构握手：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

`STRUCTURE_ONLY` 允许企业 Agent 建立 schema、类型映射、mirror 表结构和序列化适配，但不允许获取真实项目名、文件名或 NAS 路径。

## 10. 权限默认值与 mirror 规则

`delivery_platform.asset_views.v1` 当前已验证 View 没有稳定提供：

```text
permission_tags
project_scope
confidentiality_level
```

`delivery_platform.asset_views.v1.1` 补丁已补齐 `permission_tags`、`confidentiality_level`、`last_seen_at`、`lifecycle_status`、`index_eligibility`，并在 REST / Agent API 上提供 `project_scope` 调用者上下文。即便如此，Hermes mirror 层仍必须 fail closed：

```yaml
permission_status: "DENIED"
permission_reason: "NO_REST_API_KEY_PROJECT_SCOPE_OR_PERMISSION_PROOF"
can_enter_prompt: false
can_enter_evidence: false
can_enter_answer: false
```

只有平台 API Key、项目授权范围和 v1.1 结构字段共同证明资产可见时，才允许资产进入 prompt、evidence 或 answer。静态 View 的 `PROJECT:<project_id>` 只表达资产归属，不表达调用者有权访问。

不得因为权限字段缺失而默认可见。不得把 catalog-only 资产当作正文 evidence。

## 11. 业务底表拒绝验证

用 `hermes_agent_ro` 已验证以下查询全部被拒绝：

```text
SELECT COUNT(*) FROM core_projects;        -> ERROR 1142 SELECT command denied
SELECT COUNT(*) FROM data_file_resources;  -> ERROR 1142 SELECT command denied
SELECT COUNT(*) FROM core_audit_logs;      -> ERROR 1142 SELECT command denied
```

企业 Agent 侧应把 `ERROR 1142` 视为正确的安全结果。不得为了绕过该错误改用应用账号或 root 账号。

如果任一业务底表可读：

1. 立即停止联调。
2. 通知平台/运维撤销权限。
3. 重新执行只授予四个 View 的授权 SQL。
4. 重新验证业务底表拒绝访问。

## 12. 错误处理建议

企业 Agent 可按以下方式处理常见错误：

```text
Access denied for user -> 账号、密码或来源 host 不匹配；联系平台/运维，不要改用应用账号。
Unknown database -> DSN database 与交付单不一致；停止联调并确认环境。
Table does not exist -> View 未部署或库名错误；不要探测业务底表。
SELECT command denied -> 对业务底表是预期结果；对四个 View 则说明授权不完整。
Communications link failure -> 网络、端口、VPN、跳板或容器状态问题。
Public Key Retrieval is not allowed -> JDBC URL 必须包含 allowPublicKeyRetrieval=true，或由目标环境启用 TLS 策略。
```

## 13. shared-dev / staging 后置门禁

本机同环境联调已经可用，不需要等待 shared-dev / staging。若后续触发 shared-dev / staging，未完成以下交付前，企业 Agent 不得进入目标环境 DB-2 联调：

- shared-dev 或 staging MySQL 环境已准备完成。
- 当前平台数据库结构已部署或迁移完成，四个稳定 View 已存在；如接入 v1.1，则必须包含 `V17__asset_views_v1_1.sql` 的 View 结构。
- 平台/运维提供目标环境 DSN 交付单。
- 平台/运维创建 `hermes_agent_ro` 或等价只读账号，并通过安全渠道交付密码。
- 只读账号只授权四个稳定 View，不授权业务底表或写权限。
- 四个 View 正向验证通过。
- 三张业务底表反向拒绝验证通过。
- 业务负责人和数据负责人确认样例读取策略；默认 `STRUCTURE_ONLY`。
- Hermes mirror 层确认无 REST / API Key 项目授权证明时默认 `DENIED`。
- Hermes 测试机完成 v1.1 structure-only smoke，且不读取真实行。
- 企业 Agent 团队确认不外发、不外部云持久化、不写 NAS、不写数据库。
- 测试 agent 复验 DSN、View 可读、业务底表拒绝和样例读取策略均通过。

正式执行单：

```text
handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md
```

## 14. 企业 Agent 禁止事项

企业 Agent / Hermes_memory 在 DB-2 阶段不得执行：

- 使用平台应用主账号。
- 使用 root 账号。
- 读取业务底表。
- 自动探测多个 database/schema。
- 执行 `INSERT`、`UPDATE`、`DELETE`、`CREATE`、`ALTER`、`DROP`、`GRANT`。
- 写入或修改 NAS 文件。
- 移动、删除、覆盖真实文件。
- 把真实项目名、文件名、NAS 路径外发。
- 把真实样例写入外部云服务、向量库、搜索库、长期 memory 或外部观测日志。
- 在权限字段缺失时默认可见。
- 绕过平台权限把 catalog-only 资产进入 prompt、evidence 或 answer。

## 15. 交付状态

本机 dev DB-2 状态：

```text
READY_FOR_ENTERPRISE_AGENT_DB2_LOCAL_DEV
```

shared-dev / staging 状态：

```text
DEFERRED_UNTIL_REMOTE_COLLAB_OR_STAGING_REQUIRED
```

企业 Agent 团队可以基于本机 dev 继续数据库连接、字段握手、mirror schema proof-of-contract 和只读同步逻辑开发。shared-dev / staging 不再阻塞当前主线，只有在远程多人协作、持续测试、演示或客户交付前验证时，才由平台/运维按执行单开通。

v1.1 补丁交付状态：

```text
READY_FOR_HERMES_STRUCTURE_ONLY_SMOKE_AFTER_TARGET_DB_MIGRATION
```

# 企业 Agent DB-2 耦合交付包

日期：2026-05-09

v1.1 最小实现补丁更新：2026-05-11

v1.1 目标部署更新：2026-05-12

责任方：数字化交付平台一期数据库后端团队 / 主 agent

接收方：Hermes_memory / Hermes 企业 Agent 开发团队

主 agent 口径来源：

- `handoff/main-agent/status.md`
- `handoff/main-agent/decisions.md`
- `handoff/main-agent/phase1-backend-data-governance-closure.md`
- `handoff/main-agent/phase1-real-asset-overview.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`

## 1. 交付原则

企业 Agent DB-2 阶段需要的数据库连接、只读账号、schema 名称、View 字段、权限默认值、游标语义和样例读取许可，均由数字化交付平台一期数据库后端团队与主 agent 提供。

企业 Agent 团队不需要、也不应自行猜测以下信息：

- staging/dev DSN。
- 只读数据库账号。
- 可访问 database/schema。
- 可访问 View/table 白名单。
- 权限缺失时的默认策略。
- 增量同步游标语义。
- 是否允许读取样例数据。

企业 Agent 团队只负责基于本交付包做只读连接、字段握手、mirror schema proof-of-contract 和后续 Hermes_memory 接入验证。

## 1.1 主 agent 当前确认结论

根据主 agent 文档，当前已经确认：

- 一期后端数据治理已经收口，当前无 P0/P1/P2。
- MySQL 稳定 SQL View 已完成：`ProjectAssetView`、`FileAssetView`、`ModelAssetView`、`AuditEventView`。
- 企业 Agent 一期优先通过 MySQL SQL View 获取稳定读模型。
- 企业 Agent 不得直接耦合业务底表。
- REST/OpenAPI 接入使用 API Key，API Key 按项目范围授权，可授权 `SPECIFIC_PROJECTS` 或受控 `ALL_PROJECTS`。
- 增量同步采用审计/事件流，不只依赖 `updated_at`。
- 真实 NAS 采用只读影子导入，不移动、不改名、不删除原文件。
- 当前真实资产已接管 `16` 个项目，正式登记文件 `40,935` 个，其中模型 `2,604` 个、图纸 `38,331` 个。
- 非标准目录在人工确认前不得进入 `ProjectAssetView`、`FileAssetView`、`ModelAssetView`。

主 agent 当前对本机同环境与 shared-dev / staging 的裁决：

- 当前一期 DB-2 联调采用本机同环境：平台数据库运行在本机 Docker MySQL，企业 Agent 后续更新到本地 Hermes 后也运行在同一环境。
- 因此当前不强制获取 shared-dev / staging 账号，本机 `delivery-mysql` + `hermes_agent_ro` 是一期 DB-2 首轮联调路径。
- shared-dev / staging 保留为后置触发项：多人远程协作、持续测试、演示或客户交付前类生产验证时再开通。
- shared-dev / staging 只读账号和密码必须由平台/运维在目标库创建并通过安全渠道交付，不写入仓库。
- shared-dev / staging 默认 `STRUCTURE_ONLY`；业务负责人和数据负责人未书面确认前，不允许 `LIMIT 30` 真实样例。
- 当前 View 样例包含真实内部项目名、文件名和 NAS 路径，不得外发，不得写入外部云服务或外部持久化系统。
- 已完成 `delivery_platform.asset_views.v1.1` 目标环境部署：View 提供静态 `permission_tags`、`confidentiality_level`、`last_seen_at`、`lifecycle_status`、`index_eligibility`，`ModelAssetView` 补齐 `project_id`；REST / Agent API 提供调用者上下文 `project_scope`。

对应运维执行单：

```text
handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md
```

本机 dev MySQL 已由主 agent 补齐只读联调账号，账号只允许读取四个稳定 SQL View，不允许读取业务底表。shared-dev / staging 如后续需要，仍必须由主 agent 协调平台/运维提供，不能交由企业 Agent 团队自行推断。

## 1.2 四项最小联调信息

| 项目 | 当前主 agent 已确认 | 主 agent 必须补齐或裁决 |
| --- | --- | --- |
| dev DSN | 本机同环境 DSN 已确认：`jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8`。 | 当前一期 DB-2 首轮联调不需要 shared-dev / staging DSN。 |
| shared-dev / staging DSN | 当前不是阻塞项。 | 多人远程协作、持续测试、演示或客户交付前验证时，按 `enterprise-agent-db2-staging-shared-dev-ops-request.md` 由平台/运维交付。 |
| 只读账号 | 本机 dev 已创建 `hermes_agent_ro`，仅授予四个稳定 View 的 `SELECT` 权限；验证其不能读取 `core_projects` 等业务底表。 | shared-dev / staging 如后续开通，也必须创建等价只读账号，密码通过安全渠道交付，不复用应用主账号或本机 dev 密码。 |
| 允许访问的 database/schema 名 | 当前项目数据库名为 `delivery_platform`。 | 如 shared-dev / staging 使用不同库名，必须由主 agent / 运维在 DSN 交付单中明确；Hermes_memory 不得自动探测多个库名。 |
| 是否允许 `LIMIT 30` 读取四个 View | 本机 dev 同环境联调允许内部授权 `LIMIT 30` 读取四个 View；样本会暴露真实项目名和 NAS 路径。 | shared-dev / staging 默认 `STRUCTURE_ONLY`；业务负责人和数据负责人未书面确认前，不允许真实样例 `LIMIT 30`。 |

## 1.2.1 DB-2 对接裁决矩阵

以下矩阵为当前一期 DB-2 对接的冻结口径。企业 Agent / Hermes_memory 团队不得在未重新评审的情况下改变读取方式、权限默认值、正文读取范围或持久化范围。

| 问题 | 当前裁决 |
| --- | --- |
| Agent 是否直连 MySQL View | 是。当前首轮联调用本机 `delivery-mysql`，通过 `hermes_agent_ro` 只读账号读取四个稳定 View。 |
| 是否通过同步库、搜索引擎或向量库读取 | 否。当前不写同步库、不写 OpenSearch、不写 Qdrant、不做 embedding；只做 MySQL View 字段握手和只读读取。 |
| 事件流是平台推送还是 Agent 拉取 | Agent 拉取。主游标是 `AuditEventView.event_id`，`created_at` 仅作为 overlap window。 |
| 权限字段如何传递 | v1.1 View 提供静态治理标签，REST / Agent API 提供 API Key 调用者 `project_scope`。Hermes mirror 层仍必须 fail closed：没有 REST / API Key 项目授权证明时默认 `permission_status = DENIED`，不得仅凭静态 View 的 `PROJECT:<project_id>` 判定可见。 |
| 文件路径是否允许返回 | 本机内部联调允许读取 `storage_path` 中的真实 NAS 路径。shared-dev、staging、客户环境默认不直接暴露底层路径，优先使用平台授权访问入口或脱敏路径。 |
| 真实项目名、文件名是否允许进入 Agent memory | 不允许进入长期 memory、向量库、搜索库、外部日志、外部云服务或客户材料。本机样例只允许临时用于字段握手和内部调试。 |
| 是否允许读取 PDF/Office 正文 | 不允许。DB-2 阶段只读元数据和路径，不读取正文，不做 OCR，不做全文索引。 |
| 是否允许触发扫描任务 | 可以，但必须通过平台 API Key 和项目授权范围调用平台接口；不能通过数据库写入或绕过平台任务接口。 |
| 是否允许调用下载接口 | 默认不放开真实下载。确需调用时必须走平台授权接口、权限校验和审计；客户环境默认不返回真实 NAS 路径。 |
| shared-dev / staging 如何交付只读账号 | 当前后置。触发远程协作、持续测试、演示或客户交付前验证时，由平台/运维按执行单创建只读账号，只授权四个 View，密码走安全渠道。 |
| 客户环境如何执行数据脱敏和安全确认 | 默认 `STRUCTURE_ONLY`。先 `SHOW COLUMNS` 或 `WHERE 1 = 0` 握手；真实样例必须经业务负责人和数据负责人书面确认，否则使用脱敏 View 或脱敏样本。 |

## 1.3 主 agent 联调交付单

主 agent 在发给企业 Agent 团队前，必须把以下交付单补齐为具体值。

```yaml
environment: "local-dev"
db_type: "mysql"
db_version: "8.0.39"
host: "localhost"
port: 3306
database: "delivery_platform"
jdbc_url: "jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
network_access: "本机 Docker MySQL 暴露端口；不需要 VPN；容器内访问可用 delivery-mysql:3306"
tls_required: false
readonly_user: "hermes_agent_ro"
readonly_password_dev_only: "<dev-secret-from-safe-channel>"
readonly_secret_delivery: "本机 dev 密码不得写入本文或仓库，必须通过安全渠道交付；shared-dev/staging/生产密码必须使用环境专用密码"
allowed_views:
  - ProjectAssetView
  - FileAssetView
  - ModelAssetView
  - AuditEventView
allowed_schema: "delivery_platform"
sample_read_policy: "ALLOW_LIMIT_30"
sample_data_contains_real_project_names: true
sample_data_contains_real_nas_paths: true
sample_data_usage_scope: "仅限公司内部授权 Hermes_memory / 企业 Agent DB-2 字段握手和只读联调；不得外发、不得写入外部云服务、不得进入客户演示材料"
contract_version_current_verified: "delivery_platform.asset_views.v1.1"
contract_version_next_ready_in_code: "delivery_platform.asset_views.v1.1"
v1_1_migration: "V17__asset_views_v1_1.sql"
v1_1_db_apply_status: "APPLIED_TO_TARGET_DB"
backend_deploy_status: "DEPLOYED_TO_TEST_MACHINE_OR_TARGET_ENV"
contract_version_available: "delivery_platform.asset_views.v1.1"
v1_1_structure_only_smoke_prompt: "/Users/Weishengsu/Hermes_memory/docs/CODEX_DB_V11_STRUCTURE_ONLY_SMOKE_PROMPT.md"
source_system: "delivery_platform"
source_view_enum:
  - ProjectAssetView
  - FileAssetView
  - ModelAssetView
  - AuditEventView
cursor_policy:
  primary_cursor: "AuditEventView.event_id"
  overlap_field: "AuditEventView.created_at"
  asset_update_fields:
    - FileAssetView.updated_at
    - ModelAssetView.updated_at
permission_default: "DENIED"
business_table_access: "FORBIDDEN"
write_access: "FORBIDDEN"
nas_write_access: "FORBIDDEN"
local_same_host_db2_status: "CURRENT_PHASE1_PATH"
staging_dsn_status: "DEFERRED_UNTIL_REMOTE_COLLAB_OR_STAGING_REQUIRED"
staging_ops_request: "handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md"
staging_sample_policy: "STRUCTURE_ONLY"
real_sample_read_policy: "DENIED_UNTIL_BUSINESS_AND_DATA_OWNER_APPROVE_LIMIT_30"
external_persistence_policy: "FORBIDDEN"
```

说明：

- 上述交付单只对当前本机 dev 环境生效。
- 本机 dev 当前包含真实内部项目名和真实 NAS 路径，允许 `LIMIT 30` 的前提是企业 Agent 团队在公司内部授权环境中联调，且不将样本写入外部系统。
- 如果企业 Agent 的日志、向量化、调试面板或云端观测链路会保存样本内容，则不得读取真实样本，必须先改用 `WHERE 1 = 0` 字段握手或脱敏样本。
- 真实样例不得写入 Qdrant、OpenSearch、向量库、搜索库、长期 memory 或外部观测日志。
- shared-dev / staging / 客户环境不得复用本机 dev 密码。

## 2. 当前已可提供的信息

### dev DSN

当前本地 dev 数据库连接为：

```text
jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
```

本地 dev 应用账号：

```text
username: delivery
password: <application-dev-password-hidden>
```

注意：这是应用开发账号，只用于本地开发验证，不作为企业 Agent 联调只读账号。

本机 dev 企业 Agent 只读账号：

```text
username: hermes_agent_ro
password: <dev-secret-from-safe-channel>
```

注意：

- 该密码仅限当前本机 dev MySQL 联调使用。
- shared-dev / staging、客户环境不得复用该密码。
- 企业 Agent 正式环境密码必须通过安全渠道交付，不写入仓库。

只读账号验证结果：

```text
SELECT DATABASE();                         -- 通过，返回 delivery_platform
SHOW FULL TABLES WHERE Table_type='VIEW';  -- 通过，仅用于发现可见 View
SELECT COUNT(*) FROM ProjectAssetView;     -- 通过，518
SELECT COUNT(*) FROM FileAssetView;        -- 通过，46,342
SELECT COUNT(*) FROM ModelAssetView;       -- 通过，7,822
SELECT COUNT(*) FROM AuditEventView;       -- 通过，57,301
SELECT COUNT(*) FROM core_projects;        -- 拒绝，ERROR 1142
SELECT COUNT(*) FROM data_file_resources;  -- 拒绝，ERROR 1142
SELECT COUNT(*) FROM core_audit_logs;      -- 拒绝，ERROR 1142
```

### database/schema

当前项目数据库名：

```text
delivery_platform
```

### source_system

固定为：

```text
delivery_platform
```

同一环境不得混用 `platform`、`delivery`、`digital_delivery` 等其他值。

### source_view 枚举

DB-2 阶段固定为：

```text
ProjectAssetView
FileAssetView
ModelAssetView
AuditEventView
```

新增 View 必须走 contract version review。

### contract version

建议 DB-2 mirror / checkpoint 使用：

```text
delivery_platform.asset_views.v1
```

当前 SQL View 内没有 `contract_version` 字段，该值由对接合同和 Hermes mirror/checkpoint 保存。

## 3. 联调前由数据库后端团队 / 主 agent 必须补齐的信息

### staging/dev DSN

已核对主 agent 状态、裁决和收口报告：当前一期 DB-2 联调采用本机同环境，本机 DSN 已可用；当前仓库和主 agent 文档没有 shared-dev / staging DSN。

shared-dev / staging 只有在多人远程协作、持续测试、演示或客户交付前类生产验证时，才由主 agent 协调平台/运维提供。企业 Agent 团队不得自行猜测、扫描或复用本地应用账号。

正式交付格式：

```text
environment: shared-dev 或 staging
db_type: mysql
version: 8.0.39 或目标环境实际版本
host: <provided-by-platform-ops>
port: <provided-by-platform-ops>
database: delivery_platform 或目标环境实际库名
jdbc_url: jdbc:mysql://<host>:<port>/<database>?useSSL=<true|false>&allowPublicKeyRetrieval=<true|false>&serverTimezone=UTC&characterEncoding=utf8
network: VPN / 内网 / 跳板机 / SSH tunnel / 本机 docker
tls_required: true 或 false
readonly_user: hermes_agent_ro 或实际账号名
secret_delivery: 由平台/运维通过安全渠道发送，不写入仓库
```

约束：

- 不得把 staging 密码提交进仓库。
- 不得让企业 Agent 团队复用应用主账号。
- 不得让企业 Agent 团队自动扫描多个库名或多个 schema。

### 只读账号

已核对并执行：本机 dev MySQL 已创建企业 Agent 只读账号：

```text
hermes_agent_ro
```

授权范围仅限四个稳定 SQL View 的 `SELECT`。该账号已验证不能读取业务底表。

shared-dev / staging / 客户环境如后续开通，仍需由数据库后端团队在目标环境创建等价账号，可继续使用同名账号，也可使用平台运维指定账号名，但权限范围必须一致。

本机 dev 已执行授权 SQL：

```sql
CREATE USER IF NOT EXISTS 'hermes_agent_ro'@'%' IDENTIFIED BY '<dev-secret-from-safe-channel>';
ALTER USER 'hermes_agent_ro'@'%' IDENTIFIED BY '<dev-secret-from-safe-channel>';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'hermes_agent_ro'@'%';

GRANT SELECT ON delivery_platform.ProjectAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.FileAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.ModelAssetView TO 'hermes_agent_ro'@'%';
GRANT SELECT ON delivery_platform.AuditEventView TO 'hermes_agent_ro'@'%';

FLUSH PRIVILEGES;
```

shared-dev / staging / 客户环境执行时必须替换密码，不得复用本机 dev 密码。

禁止授权：

```text
INSERT
UPDATE
DELETE
DROP
ALTER
CREATE
业务底表 SELECT
NAS 文件写权限
```

### 允许访问的 database/schema 名

当前默认：

```text
delivery_platform
```

如果 staging 使用不同库名，必须由主 agent / 运维在 DSN 交付单中明确。

Hermes_memory 不应硬编码多个候选库名，也不应尝试跨库发现。

### 是否允许 LIMIT 30 读取四个 View

本机 dev 环境允许内部授权联调读取四个 View 的 `LIMIT 30` 样例。必须注意：当前本机 dev 已导入真实内部项目资产，样例会暴露真实项目名、真实文件名和真实 NAS 路径。

允许范围：

```text
仅限公司内部授权 Hermes_memory / 企业 Agent DB-2 字段握手和只读联调。
```

禁止范围：

```text
不得外发。
不得写入外部云服务。
不得进入客户演示材料。
不得在未确认权限的情况下进入 prompt、evidence、answer 或向量索引。
```

本机 dev 允许以下只读查询做字段握手和样例验证：

```sql
SELECT * FROM ProjectAssetView LIMIT 30;
SELECT * FROM FileAssetView LIMIT 30;
SELECT * FROM ModelAssetView LIMIT 30;
SELECT * FROM AuditEventView LIMIT 30;
```

shared-dev / staging / 客户环境默认先按 `STRUCTURE_ONLY` 处理。是否允许 `LIMIT 30` 读取真实样例，必须由业务负责人和数据负责人书面确认。

如果目标环境不允许暴露真实客户名、真实项目名、真实 NAS 路径或敏感项目信息，则由数据库后端团队提供二选一方案：

1. 允许 `WHERE 1 = 0` 只做字段结构握手。
2. 提供脱敏 staging 数据或脱敏样例结果。

字段结构握手 SQL：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

## 4. 稳定 View 字段清单

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

## 5. 权限字段缺失时的默认策略

当前四个 View 没有稳定提供：

```text
permission_tags
project_scope
confidentiality_level
```

因此 Hermes_memory / 企业 Agent mirror 层必须默认：

```text
permission_status = DENIED
```

只有平台 API Key、项目授权范围或后续明确权限字段证明用户可见时，才允许进入 prompt、evidence 或 answer。

不得因为权限字段缺失而默认可见。

## 6. 增量同步游标语义

### 主游标

主游标使用：

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

### 辅助时间窗口

`AuditEventView.created_at` 可作为 overlap window 的辅助字段。

不要单独依赖 `created_at`，因为同一时间可能存在多条事件。

推荐组合：

```text
event_id 主游标 + created_at overlap window
```

### 资产更新时间

`FileAssetView.updated_at` 与 `ModelAssetView.updated_at` 可用于：

- 首次快照后的补偿查询。
- mirror 数据校验。
- 资产更新时间展示。

但增量同步不能只依赖 `updated_at`。

### 项目聚合更新时间

`ProjectAssetView.last_asset_updated_at` 是聚合字段，适合刷新项目资产概览，不适合作为精确同步游标。

## 7. 推荐字段握手流程

企业 Agent 团队拿到 DSN 和只读账号后，按以下顺序执行：

```sql
SELECT 1;
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type = 'VIEW';
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
```

若允许读取样例：

```sql
SELECT * FROM ProjectAssetView LIMIT 30;
SELECT * FROM FileAssetView LIMIT 30;
SELECT * FROM ModelAssetView LIMIT 30;
SELECT * FROM AuditEventView LIMIT 30;
```

若不允许读取样例：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

## 8. DB-2 阶段禁止事项

企业 Agent / Hermes_memory 在 DB-2 阶段不得执行：

- 连接业务底表。
- 写入平台数据库。
- 写入或修改 NAS 文件。
- 删除、移动、覆盖真实文件。
- 全量解析 10TB NAS。
- 全量 embedding。
- 写入 Qdrant / OpenSearch。
- 绕过平台权限读取文件内容。
- 将 catalog-only 资产进入正文 evidence。
- 在无授权情况下启动 DB-3 retrieval。

## 9. 主 agent 后续交付清单

### 9.1 本机 dev 已完成

本机 dev DB-2 握手已完成以下交付项：

- 已填完本文第 `1.3` 节的本机 dev 主 agent 联调交付单。
- 已提供本机 dev DSN。
- 已创建 `hermes_agent_ro` 只读账号。
- 已验证只读账号可读四个稳定 View。
- 已验证只读账号不能读取业务底表 `core_projects`。
- 已确认本机 dev database/schema 为 `delivery_platform`。
- 已确认本机 dev 可在内部授权范围内执行四个 View 的 `LIMIT 30` 样例读取。
- 已明确本机 dev 样例会暴露真实项目名、文件名和 NAS 路径。
- 已确认 `delivery_platform.asset_views.v1` 合同、`event_id` 主游标和权限缺失默认 `DENIED`。

### 9.2 shared-dev / staging 处理结论

当前一期企业 Agent 与数据库规划为本机同环境运行，因此 shared-dev / staging 不再阻塞 DB-2 首轮联调。若后续进入多人远程协作、持续测试、演示或客户交付前类生产验证，平台/运维必须按以下执行单完成交付：

```text
handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md
```

必须提供：

- shared-dev 或 staging DSN 交付单。
- `hermes_agent_ro` 或等价只读账号。
- 只读账号授权确认截图或 SQL 执行记录。
- 可访问 database/schema 确认。
- 默认 `STRUCTURE_ONLY` 的样例读取裁决。
- 如需 `LIMIT 30` 真实样例，必须提供业务负责人和数据负责人的书面确认。
- 当前 View 字段清单与 `delivery_platform.asset_views.v1` 合同确认。
- `event_id` 主游标与 overlap window 策略确认。
- 权限缺失默认 `DENIED` 的确认。
- 企业 Agent 团队不外发、不外部云持久化、不写库、不写 NAS 的确认。

以上交付物由数字化交付平台一期数据库后端团队和主 agent 对企业 Agent 团队提供，不应转嫁给企业 Agent 团队自行推断。

本文件现在可作为本机 dev DB-2 联调边界文档使用；本机同环境是当前一期正式联调路径。但本文不能作为 shared-dev、staging 或生产环境的最终连接凭据。shared-dev / staging 只有在触发远程协作或交付验证时再按运维执行单开通，状态为 `DEFERRED_UNTIL_REMOTE_COLLAB_OR_STAGING_REQUIRED`。

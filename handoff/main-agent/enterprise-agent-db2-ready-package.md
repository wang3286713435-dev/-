# 企业 Agent DB-2 Ready Package

执行日期：2026-05-09

v1.1 补丁更新：2026-05-11

v1.1 目标部署更新：2026-05-12

适用环境：本机 dev，同机企业 Agent 联调。本文不保存任何数据库密码；密码必须通过安全渠道单独交付。

## 1. 本机 dev DSN

```text
jdbc:mysql://localhost:3306/delivery_platform?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
```

连接信息：

```yaml
environment: local-dev
db_type: mysql
db_version: 8.0.39
host: localhost
port: 3306
database: delivery_platform
network_access: 本机 Docker MySQL 暴露 3306；容器名 delivery-mysql
tls_required: false
```

## 2. 本机 dev 只读账号

```text
username: hermes_agent_ro
password: <dev-secret-from-safe-channel>
```

密码说明：当前规划下，平台数据库与企业 Agent / 本地 Hermes 将运行在同一台本机环境中，`hermes_agent_ro` 是本机 DB-2 联调账号。密码不得写入仓库、文档或群聊明文，只能通过安全渠道交付。后续若创建 shared-dev、staging 或生产环境，必须重新生成环境专用密码，不能复用本机密码。

本机当前密码已完成轮换，并保存于 macOS 钥匙串条目：

```text
service: delivery-platform-hermes-agent-ro-local-dev
account: hermes_agent_ro
```

企业 Agent 禁止使用平台应用主账号；只能使用专用只读账号。

## 3. 允许访问的 View

只允许读取以下四个稳定 View：

```text
delivery_platform.ProjectAssetView
delivery_platform.FileAssetView
delivery_platform.ModelAssetView
delivery_platform.AuditEventView
```

禁止读取业务底表，禁止写数据库，禁止写 NAS。

## 3.1 v1.1 字段补丁

已完成 `delivery_platform.asset_views.v1.1` 目标环境部署。四个 View / REST contract 已补齐以下字段：

```text
permission_tags / permissionTags
confidentiality_level / confidentialityLevel
last_seen_at / lastSeenAt
lifecycle_status / lifecycleStatus
index_eligibility / indexEligibility
```

REST / Agent API 额外提供：

```text
projectScope
```

`ModelAssetView` 额外补齐：

```text
project_id
```

Hermes 测试机现在可执行：

```text
/Users/Weishengsu/Hermes_memory/docs/CODEX_DB_V11_STRUCTURE_ONLY_SMOKE_PROMPT.md
```

该 smoke 仍只允许 structure-only，不授权 `LIMIT 30`、真实行读取、DB 写入、NAS scan、mirror migration 或 indexing。

## 4. 权限验证结果

正向验证均已通过：

```text
SELECT DATABASE();                         -> delivery_platform
SHOW FULL TABLES WHERE Table_type='VIEW';  -> 仅可见四个稳定 View
SELECT COUNT(*) FROM ProjectAssetView;     -> 518
SELECT COUNT(*) FROM FileAssetView;        -> 46,342
SELECT COUNT(*) FROM ModelAssetView;       -> 7,822
SELECT COUNT(*) FROM AuditEventView;       -> 57,301
```

反向验证均已拒绝：

```text
SELECT COUNT(*) FROM core_projects;        -> ERROR 1142 SELECT command denied
SELECT COUNT(*) FROM data_file_resources;  -> ERROR 1142 SELECT command denied
SELECT COUNT(*) FROM core_audit_logs;      -> ERROR 1142 SELECT command denied
```

## 5. 样本读取策略

本机 dev：允许公司内部授权联调用 `LIMIT 30` 读取样例。当前本机 dev 已导入真实公司内部 NAS 元数据，样例可能包含真实项目名、真实文件名、真实 NAS 路径。

限制：不得外发，不得写入外部云服务，不得进入客户材料；如果企业 Agent 的日志、向量化、调试面板或云端观测链路会保存样本内容，则不得读取真实样本。

shared-dev / staging：当前不是本机 DB-2 首轮联调前置条件；后续若开通，默认 `STRUCTURE_ONLY`，除非业务负责人和数据负责人书面确认允许真实样例。若不允许暴露真实项目名或 NAS 路径，后续必须提供脱敏 View 或脱敏样本。

## 6. 字段握手 SQL

```sql
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type = 'VIEW';
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
```

## 7. 样例读取 SQL

仅限本机 dev 内部授权联调：

```sql
SELECT * FROM ProjectAssetView LIMIT 30;
SELECT * FROM FileAssetView LIMIT 30;
SELECT * FROM ModelAssetView LIMIT 30;
SELECT * FROM AuditEventView LIMIT 30;
```

## 8. STRUCTURE_ONLY SQL

当不允许读取真实样例时，使用以下 SQL 只做字段结构握手：

```sql
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

## 9. shared-dev / staging 处理结论

当前一期规划已调整：企业 Agent 未来会更新到本地 Hermes，并与平台数据库运行在同一台本机环境中。因此 DB-2 首轮联调不再等待 shared-dev / staging；本机 `delivery-mysql` + `hermes_agent_ro` 即为当前正式联调路径。

shared-dev / staging 保留为后置触发项，仅在以下情况需要开通：

- 企业 Agent、测试、运维或平台开发需要多人同时远程协作。
- 需要脱离主开发机做持续联调、自动化测试或演示。
- 准备客户交付前的类生产验证。
- 需要验证部署、备份恢复、升级、权限和环境隔离。

已新增正式运维执行单：

```text
handoff/main-agent/enterprise-agent-db2-staging-shared-dev-ops-request.md
```

该执行单固定以下结论：

- 本机同环境联调不需要 shared-dev / staging DSN。
- 只有触发 shared-dev / staging 后，DSN 才必须由平台/运维按交付单提供。
- shared-dev / staging 只读账号必须由平台/运维在目标库创建，密码通过安全渠道交付，不写入仓库。
- shared-dev / staging 默认 `STRUCTURE_ONLY`，不允许真实样例读取。
- `LIMIT 30` 真实样例读取必须经业务负责人和数据负责人书面确认。
- v1.1 静态 View 只提供粗粒度治理标签；最终可见性必须结合 REST / API Key 项目授权上下文。没有 REST / API Key 项目授权证明时，Hermes mirror 层必须默认 `DENIED`。
- 真实项目名、文件名、NAS 路径不得外发，不得写入外部云服务或外部持久化系统。

## 10. 企业 Agent 团队禁止事项

- 不得使用平台应用主账号。
- 不得读取业务底表。
- 不得执行 `INSERT`、`UPDATE`、`DELETE`、`CREATE`、`ALTER`、`DROP`、`GRANT`。
- 不得移动、修改、删除 NAS 文件。
- 不得把本机 dev 密码用于 staging / production。
- 不得把真实项目名、文件名、NAS 路径外发或写入外部云服务。
- 不得把真实样例写入 Qdrant、OpenSearch、向量库、搜索库、长期 memory 或外部观测日志。
- 不得在 REST / API Key 项目授权证明缺失时默认可见；权限默认值必须为 `DENIED`。

# Codex Test-machine DB Structure-only Smoke Prompt

数据库团队确认口径：

- 当前授权范围仅为 `structure_only`。
- 当前 contract version 仍为 `delivery_platform.asset_views.v1`。
- 禁止读取或输出真实项目名、文件名、NAS 路径、`asset_uid`、`source_id`、raw row 或任何真实业务行值。
- 禁止输出 secret、token、password、API key、`.env` 值或任何凭据材料。
- 禁止写平台 DB、Hermes Memory DB、OpenSearch、Qdrant、MinIO、NAS 或任何索引/正文存储。
- 测试机 DB host / port / database / readonly credential 必须通过安全渠道在测试机本地注入，不得写入本文件、聊天、Git 或报告。

## Purpose

This prompt is for the test-machine Codex operator that will run the Data Steward DB `structure_only` smoke.

It validates only database reachability, selected database name, View existence, View column metadata, and empty-structure query shape. It must not read real business rows.

## Preconditions

Before running anything, confirm:

1. Hermes_memory is installed on the test machine at the reviewed version approved by Codex B.
2. The operator has received DB host / port / database / readonly credential through a secure channel.
3. The credential is a readonly account or readonly API key.
4. Contract version is expected to be `delivery_platform.asset_views.v1`.
5. The current authorization is `structure_only` only.

If any item is missing, stop with `Pause`.

## Hard Boundaries

Do not:

1. Print secret, token, password, API key, `.env` values, or credential material.
2. Print true project names, file names, NAS paths, `asset_uid`, `source_id`, raw rows, SQL stderr, or row values.
3. Run `LIMIT 1`, `LIMIT 30`, `COUNT(*)` over business rows, or any real row sample query.
4. Scan NAS.
5. Write platform DB.
6. Write Hermes Memory DB.
7. Write `documents` / `chunks`.
8. Write OpenSearch / Qdrant / MinIO.
9. Run migration.
10. Enable Data Steward runtime feature flags.
11. Implement DB CRUD.
12. Let Agent directly modify platform database or NAS.
13. Enter production rollout.

## Allowed SQL Shapes

Only these structure-only SQL shapes are allowed:

```sql
SELECT 1;
SELECT DATABASE();
SHOW FULL TABLES WHERE Table_type = 'VIEW';
SHOW COLUMNS FROM ProjectAssetView;
SHOW COLUMNS FROM FileAssetView;
SHOW COLUMNS FROM ModelAssetView;
SHOW COLUMNS FROM AuditEventView;
SELECT * FROM ProjectAssetView WHERE 1 = 0;
SELECT * FROM FileAssetView WHERE 1 = 0;
SELECT * FROM ModelAssetView WHERE 1 = 0;
SELECT * FROM AuditEventView WHERE 1 = 0;
```

Do not add other SQL unless a later phase explicitly authorizes it.

## Recommended Execution Shape

Prefer existing Hermes_memory readonly structure tooling when available. If using direct MySQL CLI, credentials must be injected through the local process environment or operator-approved secure local mechanism, never as command-line password values.

Acceptable local checks:

1. Confirm required key names exist without printing values.
2. Confirm DB host / port / database are configured without printing password.
3. Confirm readonly credential is available without printing it.

## Report Fields

Return only a sanitized report:

1. `status`: `go` / `pause` / `no_go`.
2. `db_reachable`: boolean.
3. `database_name_matches_expected`: boolean.
4. `contract_version_expected`: `delivery_platform.asset_views.v1`.
5. `views_found`: list containing only View names.
6. `views_missing`: list containing only View names.
7. `columns_by_view`: field metadata summary only.
8. `where_1_eq_0_ok_by_view`: boolean per View.
9. `permission_errors`: sanitized code/message category only.
10. `secret_printed`: must be `false`.
11. `real_rows_read`: must be `false`.
12. `true_business_data_output`: must be `false`.
13. `writes_performed`: must be `false`.
14. `go_pause_no_go_reason`: short sanitized reason.

Column metadata may include field name, type, nullable, key, default presence, and extra metadata. Do not include row values.

## Go / Pause / No-Go

Go:

1. Secure DB config exists.
2. `SELECT 1` succeeds.
3. `SELECT DATABASE()` returns expected database.
4. Four expected Views exist.
5. `SHOW COLUMNS` works for all four Views.
6. `WHERE 1 = 0` works for all four Views.
7. No secret or real business data is printed.
8. No writes are performed.

Pause:

1. Host / port / database / readonly credential is missing.
2. View names differ.
3. Contract version cannot be confirmed.
4. Readonly user lacks permission for structure-only checks.
5. Tooling path is unclear.

No-Go:

1. Any real row was read.
2. Any secret or business data was printed.
3. Any write, migration, backfill, reindex, DB CRUD, NAS scan, or rollout was attempted.
4. The task requires real sample reads to continue.

## Final Response Format

Return:

1. DB connection mode used, without secrets.
2. Go / Pause / No-Go.
3. View existence summary.
4. Column metadata summary.
5. `WHERE 1 = 0` summary.
6. Confirmation that secrets / rows / true business data / writes were not output or performed.
7. Minimal blockers if Pause or No-Go.

Do not commit, tag, push, upload files, scan NAS, or mutate any DB/index.

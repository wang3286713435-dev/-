# Platform 到 Hermes 对接契约

## 平台向 Hermes 提供

- `file_id`
- `model_id`
- `project_id`
- `source_view`
- `asset_catalog_only`
- `permission_decision`
- `missing_evidence_reason`
- `display_path` / `path_hint`
- `capabilities`
- `query_id` / `trace_id`

## 可信边界

- `project_scope`、`permission_decision` 和权限相关上下文必须由 Platform Gateway 服务端生成或校验，不能信任前端传入值。
- `display_path` / `path_hint` 必须是脱敏展示字段，不得等同 raw `storage_path`。
- catalog metadata 只能作为目录级证据，不得作为文件正文、DWG/RVT 内容或 BIM 构件证据。

## 平台默认不提供

- raw `storage_path`
- raw `storage_uri`
- raw DB row
- file content
- DWG/RVT internals
- unredacted secret

## Hermes 允许做

- 调用只读 Catalog Tool。
- 解释 catalog-only 结果。
- 返回 Missing Evidence。
- 记录低敏 `related_file_ids`。
- 基于标准库解释当前证据等级。

## Hermes 禁止做

- 直接查底表。
- 生成 SQL。
- 写 DB。
- 写 NAS。
- 把 catalog metadata 当正文证据。
- 把 NAS 内容写入 memory。
- 默认暴露 `storage_path`。

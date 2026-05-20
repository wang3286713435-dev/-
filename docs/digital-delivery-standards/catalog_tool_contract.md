# Catalog Tool 契约

建议工具名：`asset_catalog_search`

当前状态：推荐/目标工具契约。真实 runtime 接入应按 Hermes 主线阶段逐步落地，不得被误读为已具备 NAS 全文检索、语义检索、DWG/RVT 内容理解或 BIM 构件级查询能力。工具只能通过 Platform Gateway / View Contract 访问目录级资产信息，不裸连 DB，不返回 raw catalog row。

```json
{
  "name": "asset_catalog_search",
  "description": "Search catalog-only asset metadata through Platform Gateway/View Contract. Does not read file content, connect directly to DB, or return raw catalog rows.",
  "input_schema": {
    "query": "string",
    "project_scope": "object",
    "filters": {
      "project_id": "string",
      "file_kind": "string",
      "discipline": "string",
      "model_format": "string",
      "lifecycle_status": "string",
      "index_eligibility": "catalog_only"
    }
  },
  "output_schema": {
    "query_id": "string",
    "asset_catalog_only": true,
    "source_view": "FileAssetView | ModelAssetView",
    "items": [],
    "missing_evidence": []
  },
  "permission": "read-only, fail-closed",
  "path_policy": "no raw storage_path by default"
}
```

## 行为要求

- 工具只通过 Platform Gateway / View Contract 查询 catalog-only asset metadata。
- 工具不读取文件正文。
- 工具不解析 DWG/RVT internals。
- 工具不裸连 DB，不生成 SQL，不返回 raw catalog row。
- 工具必须遵循 read-only、fail-closed 和默认路径脱敏策略。
- 输出必须可追踪到 `query_id`，但不得泄露 raw `storage_path`。
- `project_scope` 必须由 Platform Gateway 服务端生成或校验，不能信任前端传入值。
- `display_path`、`path_hint` 或任何路径类输出都必须是脱敏展示字段，不得等同 raw `storage_path`。
- catalog metadata 只能作为目录级证据，不得作为文件正文、DWG/RVT 内容或 BIM 构件证据。

# Missing Evidence 政策

当用户问题需要文件正文、DWG 图层、DWG 图框、RVT sheets/views/levels/families、BIM 构件参数、工程语义证据时，如果当前只有 catalog metadata，则必须返回 Missing Evidence。

## 必须返回 Missing Evidence 的典型场景

- 用户询问图纸正文、图签、图框、图层、标注或坐标内容。
- 用户询问 RVT sheets/views/levels/families。
- 用户询问 BIM 构件参数、构件清单或构件级属性。
- 用户要求基于 PDF/Office 正文内容做结论。
- 用户要求工程语义判断，但当前只有目录元数据。
- 用户要求把 `updated_at` 当作 NAS 文件本体 mtime。
- 用户要求把 `process_status` 当作 semantic index status。

## 证据等级

- `catalog_evidence`
- `filename_evidence`
- `path_evidence`
- `metadata_evidence`
- `preview_evidence`
- `full_text_evidence`
- `dwg_parse_evidence`
- `rvt_parse_evidence`
- `component_evidence`
- `manual_evidence`
- `missing_evidence`

## 回答要求

Hermes 可以说明当前可见的是资产目录信息，但不能用 catalog metadata 伪装成文件正文或工程语义证据。缺失证据时，应明确说明需要哪类证据补齐。

## 标准回复模板

PDF / Office 正文问题：

```text
当前只有 catalog metadata / filename evidence / metadata evidence，缺少 `full_text_evidence`，因此不能判断该文件正文内容。该问题应标记为 Missing Evidence / manual review。
```

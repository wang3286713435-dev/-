# M3E 计划：预览与转换产物对象化

日期：2026-05-26

## 结论

当前已从最新 `main` 创建分支：

`codex/m3e-preview-artifacts-object-storage`

M3D 已合并回 `main`，主线最新进度为 M3D。M3E 作为下一批次启动。

## 批次目标

M3E 不做真实转换器，不做 Hermes 正文问答，不做语义索引。

本批只把“预览产物 / 转换产物 / 未来 BIM 轻量化产物”的关系接入对象存储底座，让平台能清楚表达：

- 哪些文件已有对象化预览产物。
- 哪些文件需要转换。
- 哪些文件暂不支持预览。
- 所有预览仍必须通过平台受控 file-access 或 preview ticket。

## 主要开发内容

- 复用 `data_file_derivatives`。
- 复用 `data_preview_artifacts`。
- 新增预览产物查询接口。
- 新增预览产物受控 prepare 接口。
- 文件管理器展示预览产物状态。
- 交付包预检查与 preview artifact 口径对齐。
- 新增 M3E 专项脚本。

## 禁止事项

- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做真实 CAD / BIM 转换。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不新增 Hermes 正文问答。
- 不暴露真实 NAS 路径、bucket、object_key、storage_uri、raw row、SQL、token、secret。
- 不修改 `docs/**`。

## 验收重点

- 105 项目已对象化 PDF 可生成 `BROWSER_NATIVE_PREVIEW / AVAILABLE` artifact。
- DWG / RVT 只能生成 `NEEDS_CONVERSION` placeholder。
- 受控 file-access 不回归。
- 交付包预检查不回归。
- 禁出字段扫描通过。
- M3D / M3C / M3B / M3A / file-access 回归通过。

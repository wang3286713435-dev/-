# M3E 收口：预览与转换产物对象化

日期：2026-05-27

## 结论

`M3E：预览与转换产物对象化` 已通过开发自测和测试 agent 正式验收，主 agent 判定：正式收口。

当前 P0：无。

当前 P1：无。

当前 P2：

- 既有 Vite chunk size warning。
- 工作区存在未暂存的品牌命名口径修改，以及 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非本批内容；这些不属于 M3E。

## 完成内容

M3E 已完成：

- 新增 `GET /api/data-steward/assets/files/{fileId}/preview-artifacts`。
- 新增 `POST /api/data-steward/assets/files/{fileId}/preview-artifacts:prepare`。
- PDF / 图片可登记为 `BROWSER_NATIVE_PREVIEW / AVAILABLE / OBJECT_STORED`。
- DWG / RVT / Office 仅登记转换占位，不伪造可预览状态。
- 文件管理器增加“预览产物”状态列。
- 文件管理器右键菜单增加“准备预览产物状态”。
- 新增 M3E 专项脚本。

## 验收依据

测试报告：

- `handoff/test-agent/latest-report.md`

通过项：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh` 通过，`PASS=8 FAIL=0`。
- M3D / M3C / M3C-1 / M3B / M3A / file-access 回归通过。
- `git diff --cached --check` 通过。
- `git diff --check` 通过。

## 关键验证结果

- PDF / 图片样本：`BROWSER_NATIVE_PREVIEW / AVAILABLE / COMPLETED / OBJECT_STORED`。
- DWG 样本：`CAD_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- RVT 样本：`BIM_LIGHTWEIGHT_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- Office 样本：`OFFICE_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- 原生预览仍通过受控 `file-access` 打开。
- 交付包导出预检查字段未回归。

## 安全边界

已确认未发生：

- 真实 NAS 文件移动、删除、重命名或覆盖。
- PDF / Office / DWG / RVT / IFC 正文读取。
- 真实 PDF / Office / CAD / BIM 转换。
- BIM parser / 真实轻量化。
- documents / chunks 写入。
- Qdrant / OpenSearch / Hermes memory 写入。
- Hermes 正文问答。
- 全量 NAS 迁移。
- `docs/**` 修改纳入 M3E staged 范围。
- raw NAS path、bucket、object key、`storage_uri`、raw row、SQL、token、secret 泄露。

## 下一步

按 M3-M5 任务图，下一步建议进入：

`M4A：documents / chunks 语义证据契约`

M4A 应先做契约与边界，不要直接写向量库或接 Hermes 正文问答。重点是定义 documents / chunks 如何绑定：

- `projectId`
- `assetUuid`
- `fileId`
- `objectVersion`
- `pageNo` / 图号 / 定位
- `sectionNodeId`
- `deliverableTypeId`
- `permissionScope`
- `evidenceHash`

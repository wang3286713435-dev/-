# M3G-2：105 项目历史文件对象化上传灰度计划

更新时间：2026-05-27

## 批次定位

`M3G-2` 承接已收口的 `M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划`。

本批开始执行真实历史文件对象化上传，但范围只限 `105 项目` 当前本地项目 ID `503`，并且必须小批量、可追踪、可重试、可审计。

这里的“上传 MinIO”不是把 NAS 原文件搬走，而是：

```text
真实 NAS 原文件保留
-> 平台按权限读取一份
-> 上传副本到 NAS 侧 MinIO
-> 写入 MySQL 对象台账与 active object version
-> 后续读取优先走 MinIO
```

## 当前读取降级策略

当前平台已有对象存储优先读取与 NAS 兜底策略，但不是无条件兜底：

1. 文件已有 active object version，且状态为 `OBJECT_STORED`：`file-access` 优先读取对象存储。
2. 文件没有 active object version：`file-access` 继续读取原 NAS 台账路径。
3. 文件标记为 `OBJECT_STORED` 但对象存储副本不可读：必须 fail-closed，并提示对象副本异常；不能偷偷读 NAS 并伪装对象化成功。

第 3 条是安全边界：避免平台显示“已对象化”，实际却绕回 NAS。

## M3G-2 目标

- 只在 105 / `projectId=503` 上做灰度。
- 从 dry-run 计划中选择少量真实历史文件上传到 NAS 侧 MinIO。
- 迁移任务必须有进度、失败原因、幂等与审计。
- 上传完成的文件应显示 `OBJECT_STORED`。
- 上传完成的文件通过受控 `file-access` 读取对象存储。
- 未上传文件仍显示 `NAS_ONLY`，并继续通过现有 NAS 链路可用。
- 用户能在文件服务 / 对象存储页面看到 105 项目的对象化覆盖率变化。

## 批次边界

本批允许：

- 扩展已有对象迁移任务中心，让 105 项目 dry-run 结果可以生成受控迁移任务。
- 默认每个任务保持小批量，建议不超过现有后端安全上限。
- 增加前端“执行 105 灰度对象化”入口或等价操作。
- 增加 105 对象化灰度专项脚本。

本批禁止：

- 不全量迁移所有项目。
- 不一键迁移 NAS 根目录。
- 不移动、删除、重命名、覆盖真实 NAS 原文件。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不做 Hermes 正文问答。
- 不接入真实 BIM 引擎。
- 不暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
- 不修改 `docs/**`。

## 任务图

- [x] 0. M3G-1 readiness / inventory / dry-run 已收口。
- [x] 1. 确认当前降级策略：对象版本优先，无对象版本走 NAS，坏对象版本 fail-closed。
- [x] 2. 输出 M3G-2 开发 agent prompt。
- [x] 3. 输出 M3G-2 测试 agent prompt。
- [x] 4. 开发 agent 实现 105 dry-run 到迁移任务的受控执行链路。
- [x] 5. 开发 agent 增加前端灰度执行与覆盖率变化展示。
- [x] 6. 开发 agent 增加 M3G-2 专项脚本。
- [x] 7. 测试 agent 验证 105 小批量对象化真实执行。
- [x] 8. 主 agent 审计并收口 M3G-2。

## 验收口径

M3G-2 通过时，平台应能回答：

1. 105 项目当前多少文件已对象化，多少仍在 NAS_ONLY。
2. 本次灰度上传了哪些文件，成功 / 跳过 / 失败多少。
3. 已对象化文件是否确实通过对象存储读取。
4. 未对象化文件是否仍按 NAS 链路可用。
5. NAS 原文件是否保持未移动、未删除、未改名。

M3G-2 不验收：

- 全公司项目全量对象化。
- Hermes 正文问答。
- 语义索引。
- 文件正文解析。
- BIM 轻量化真实转换。

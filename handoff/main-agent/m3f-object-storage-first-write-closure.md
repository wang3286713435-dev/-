# M3F：新文件对象存储优先写入收口记录

收口时间：2026-05-27

## 1. 收口结论

`M3F：新文件对象存储优先写入与 NAS 兼容回退` 已通过开发自测和测试 agent 正式验收，主 agent 判定：正式收口。

当前 P0：无。

当前 P1：无。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- M3G 计划文件随本次 checkpoint 一并提交，用于记录后续 NAS 侧 MinIO 接管真实项目文件的路线，不代表 M3G 已启动。

## 2. 本批完成内容

M3F 完成：

- 保留原 `nas/files:upload` 上传入口兼容。
- 新上传文件本体改为优先写入对象存储。
- 新上传文件同步写入：
  - `data_file_resources`
  - `data_storage_objects`
  - active `data_file_object_versions`
- 新上传文件生成稳定 `assetUuid`。
- 新上传文件 `storage-status=OBJECT_STORED`。
- 新上传文件可通过受控 `file-access` 读取。
- 对象存储不可用时 fail-closed，不静默回退真实 NAS。
- 前端文件管理器显示对象存储状态。

M3F 明确未做：

- 未全量迁移历史 NAS 文件。
- 未移动、删除、重命名、覆盖真实业务 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未新增 Hermes 正文问答。
- 未接入真实 BIM 引擎。

## 3. 验收依据

开发报告：

- `handoff/dev-agent/latest-report.md`

测试报告：

- `handoff/test-agent/latest-report.md`

专项脚本：

- `scripts/dev/check-m3f-object-storage-first-write.sh`

测试 agent 已确认：

- 后端构建通过。
- 前端构建通过。
- 后端健康检查通过。
- M3F 专项脚本通过，`PASS=10 FAIL=0`。
- M3E / M3D / M3C / M3B / M3A / file-access 回归均通过。
- 新上传文件有 `assetUuid`、active object version、`OBJECT_STORED` 状态。
- 受控 `file-access` 可读取对象存储新增文件。
- 对象存储不可用时失败关闭，不假成功，不写 NAS fallback。
- 禁出字段扫描通过。

## 4. 后续裁决

M3F 收口后，下一候选批次为：

`M3G：NAS 侧 MinIO 对象存储接管真实项目文件`

M3G 必须单独启动，不随 M3F 自动进入。

M3G 目标是把对象存储主链路从本机 MinIO 验证升级为 NAS 侧 MinIO 正式接管真实项目文件本体。Hermes 仍不得直接扫 NAS 或直连 MinIO 底层目录，只能在后续通过平台授权复制文件副本到本机工作区解析。

# M3D 收口：真实 NAS 小范围灰度镜像

日期：2026-05-26

## 结论

`M3D：真实 NAS 小范围灰度镜像` 已通过开发自测和测试 agent 正式验收，主 agent 判定：正式收口。

当前 P0：无。

当前 P1：无。

当前 P2：

- 既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- `handoff/main-agent/m3d-real-nas-object-mirror-gray-plan.md` 为交接文档，建议纳入本批提交。

## 灰度样本

项目：`105 / projectId=503`

本批显式选择少量真实业务文件，不做项目全量、目录全量或后缀全量自动迁移。

已覆盖：

- PDF：`fileId=993`
- DWG：`fileId=935`
- RVT / MODEL：`fileId=1257`

三份样本均已进入 `OBJECT_STORED`。

## 验收依据

测试报告：

- `handoff/test-agent/latest-report.md`

通过项：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh` 通过，`PASS=19 FAIL=0`。
- M3C / M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
- `git diff --check` 通过。

## 关键结果

- PDF / DWG / RVT 模型类样本均覆盖。
- 本轮复跑结果为 `success=0 skipped=3 failure=0`，原因是三份样本已对象化，复跑按幂等策略跳过。
- 开发首次灰度记录为 `success=3 failure=0 skipped=0`。
- checksum 覆盖率：`3/3`。
- 三份样本 `storage-status` 均为 `OBJECT_STORED`。
- 三份样本均可通过受控 `file-access` 读取对象镜像。

## 安全边界

已确认未发生：

- 项目全量迁移。
- 目录全量迁移。
- 后缀全量迁移。
- 真实 NAS 文件移动、删除、改名或写入。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / Qdrant / OpenSearch / Hermes memory 写入。
- Hermes 正文问答。
- BIM parser / 真实轻量化。
- raw NAS path、bucket、object key、`storage_uri`、raw row、SQL、token、secret 泄露。

## 下一步

按 M3-M5 任务图，下一步建议进入：

`M3E：预览与转换产物对象化`

M3E 应继续保持：

- 不做 Hermes 正文问答。
- 不写语义索引。
- 不读取 DWG / RVT 深层内容。
- 只处理预览 / 转换产物与对象存储的关系。
- file-access 继续作为受控访问入口。

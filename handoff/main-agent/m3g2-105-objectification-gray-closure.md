# M3G-2：105 项目历史文件对象化上传灰度收口

收口时间：2026-05-28

## 收口结论

`M3G-2：105 项目历史文件对象化上传灰度` 正式收口。

本批完成了 105 / `projectId=503` 的首轮历史文件小批对象化验证。平台已经证明：

- 可从 105 dry-run 计划选择小批真实历史文件。
- 可将 NAS 原文件复制副本到 NAS 侧 MinIO。
- 可写入对象台账和 active object version。
- 已对象化文件通过受控 `file-access` 读取。
- 未对象化文件仍保持 `NAS_ONLY`，并继续通过原 NAS 链路可用。
- 重复执行同一批文件会幂等跳过，不污染 active object version。

## 实际执行结果

开发 agent 首批执行：

- `taskId=143`
- `fileId=936`
- `fileId=937`
- `fileId=938`
- `success=3`
- `skipped=0`
- `failure=0`

重复执行验证：

- `taskId=144`
- `success=0`
- `skipped=3`
- `failure=0`
- 行级结果为 `ALREADY_STORED`

主 agent 复验时又运行了一次执行型脚本，因此当前 105 对象化覆盖数进一步增加：

- 当前对象化状态：`OBJECT_STORED=9`
- 当前 NAS_ONLY 状态：`NAS_ONLY=2919`
- 当前覆盖率约：`0.31%`

测试 agent 已按安全边界没有再次运行执行型脚本，没有继续追加新的对象化批次。

## 安全边界确认

已确认：

- NAS 原文件未移动、未删除、未改名、未覆盖。
- 本批只是复制副本到 NAS 侧 MinIO。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未触发 Hermes。
- 未接入真实 BIM 引擎。
- 未全量迁移全部项目。
- 未一键迁移 NAS 根目录。
- API / 前端响应未暴露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。

## 验收结果

测试 agent 报告：

- `handoff/test-agent/latest-report.md`

验收结论：

- P0：无。
- P1：无。
- P2：仅既有 Vite chunk warning、非交付未跟踪项、以及测试 agent 未再次运行执行型脚本的说明。

已通过：

- 后端构建。
- 前端构建。
- 后端健康检查。
- M3G-1 readiness / inventory / dry-run 回归。
- M3E 预览与转换产物对象化回归。
- M3F 新文件对象存储优先写入回归。
- M3C 对象存储迁移任务中心回归。
- Phase2 batch4 文件访问安全回归。
- `git diff --check`。

## 收口后路线

下一步建议：

`M3G-3：多真实项目分批对象化策略与任务中心增强`

M3G-3 不应立刻全量迁移所有项目，应先解决：

- 批量策略。
- 速率限制。
- 并发上限。
- 容量预估。
- 运维观察窗口。
- 失败重试与跳过策略。
- 项目级覆盖率报表。

M4A 语义证据契约继续后置，不能跳过对象存储和权限链路直接进入 Hermes 正文问答。

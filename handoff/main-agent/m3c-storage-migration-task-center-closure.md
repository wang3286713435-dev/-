# M3C 收口：对象存储迁移任务中心与批量策略

日期：2026-05-26

## 结论

`M3C：对象存储迁移任务中心与批量策略` 已通过开发自测和测试 agent 正式验收，主 agent 判定：正式收口。

当前 P0：无。

当前 P1：无。

当前 P2：

- 既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。
- 页面安全文案中出现 `bucket / object key` 字面词，但语义是“不会展示底层定位”；未发现真实 bucket 名称或 object key 值泄露。

## 已完成能力

- 新增项目内对象存储迁移任务中心。
- 支持显式选择项目文件创建迁移任务。
- 支持迁移任务列表、详情、行级结果和失败任务重试。
- 支持对象存储迁移汇总。
- 任务行展示 `assetUuid`、文件名、类型、大小、迁移状态、存储状态、结果码和业务化说明。
- 前端入口：
  - `/data-steward/assets/:projectId/data-steward/storage-migration`
- 项目工作台导航中加入“对象存储”入口。
- 文件服务页面增加“对象存储迁移 / 文件访问安全”双 tab。

## 批量策略

- 只能通过显式 `fileIds` 创建迁移任务。
- 文件必须属于当前项目。
- 当前用户必须有项目权限。
- 单次最多 10 个文件。
- 单文件上限 10 MB。
- 已删除、回收站、不可读源文件返回业务化失败原因。
- 已有 active 对象版本的文件返回已对象化 / 跳过语义，不重复污染 active 对象版本。

## 验收依据

测试报告：

- `handoff/test-agent/latest-report.md`

通过项：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- `scripts/dev/check-m3c-storage-migration-task-center.sh` 通过，`PASS=9 FAIL=0`。
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh` 通过，`PASS=15 FAIL=0`。
- M3B / M3A / M2J / M2I / M2H / file-access 回归通过。
- `git diff --check` 通过。
- 浏览器短验通过。

## 安全边界

已确认未发生：

- 全量 NAS 迁移。
- 目录一键全量迁移。
- 真实 NAS 文件移动、删除、重命名或改写。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / Qdrant / OpenSearch / Hermes memory 写入。
- Hermes 正文问答。
- BIM parser / 轻量化扩展。
- raw NAS path、bucket、object key、`storage_uri`、raw row、SQL、token、secret 泄露。

## 下一步

按 M3-M5 任务图，下一步建议进入：

`M3D：真实 NAS 小范围灰度镜像`

M3D 仍应保持：

- 不做全量 NAS 搬迁。
- 只选择 105 项目少量真实业务文件。
- 覆盖 PDF / DWG / RVT 小样本。
- NAS 原文件保留。
- file-access 继续作为受控访问入口。
- 灰度报告记录成功数、失败数、跳过数、checksum 覆盖率和禁出字段扫描结果。

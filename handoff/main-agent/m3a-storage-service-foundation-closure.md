# M3A：对象存储与 StorageService 基线收口

时间：2026-05-25

## 1. 收口结论

`M3A：对象存储与 StorageService 基线` 正式收口。

测试 agent 已完成完整验收和 P1 极短复验：

- 功能验收通过。
- P1“核心交付文件未纳入 Git 跟踪”已关闭。
- 当前无 P0 / P1。
- M3A 专项脚本通过，`PASS=8 FAIL=0`。

## 2. 已完成能力

本批完成了对象存储底座的第一步：

- 追加 `V28__m3a_storage_objects_foundation.sql`。
- 新增对象存储相关元数据模型。
- 新增 `StorageService` 基线。
- 现有 `file-access` 预览 / 下载链路已能通过 StorageService 读取。
- NAS 原有受控访问未回归。
- MinIO provider health 可查。
- S3-compatible 未配置时返回业务化不可用状态，不 500。
- MinIO 测试对象可通过受控预览票据读取。
- 新增 `storage providers health` 和 `file storage status` 只读接口。
- 新增 `scripts/dev/check-m3a-storage-service-foundation.sh`。

## 3. 验收依据

完整验收通过项：

- 后端构建通过。
- 前端构建通过，仅保留既有 Vite chunk size warning。
- 健康检查通过。
- M3A 专项脚本通过。
- M2J / M2I / M2H / M2F / M2B / file-access 回归通过。
- provider health、storage-status、MinIO 受控读取、NAS 既有预览 / 下载链路均未回归。
- 禁出字段扫描通过。
- `git diff --check` 通过。

P1 极短复验通过项：

- M3A 核心新增文件均已纳入 Git 暂存，状态为 `A`。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 未被纳入暂存。
- M3A 专项脚本继续通过。

## 4. 边界确认

本批未做：

- 全量 NAS 迁移。
- 真实 NAS 文件移动、删除、重命名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答。
- 真实 BIM 引擎接入。
- 语义解析或向量索引。

本批仍坚持：

- NAS 是源头和过渡来源。
- 对象存储是镜像承接、预览产物、交付归档和后续语义处理底座。
- MySQL 仍是业务、权限、交付、审计中心。
- Hermes 后续只能通过平台 Gateway 获取脱敏证据。
- 前端不得直接获得真实 NAS 路径、bucket、object key、storage URI。

## 5. 剩余提醒

P2：

- 前端构建仍有既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为非交付未跟踪项，提交时继续排除。

提交提醒：

- M3A 核心新增文件已暂存。
- M3A 的已跟踪修改仍需在正式提交时统一纳入。

## 6. 下一步建议

下一批建议进入：

`M3B：105 小样本对象存储镜像迁移`

M3B 只做小样本镜像迁移，不做全量搬迁：

- 选取 105 项目少量安全样本文件。
- 计算 checksum。
- 上传到 MinIO / S3-compatible。
- 校验 etag / checksum。
- 写入对象版本记录。
- 标记 `OBJECT_STORED`。
- NAS 原文件保留不动。
- 迁移任务必须可重试、可审计、可查看失败原因。

M3B 未经用户确认不得自动启动。

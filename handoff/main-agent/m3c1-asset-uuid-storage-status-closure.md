# M3C-1 收口报告：资产 UUID 与存储状态统一

时间：2026-05-26 CST

## 1. 收口结论

`M3C-1：资产 UUID 与存储状态统一` 已通过开发自测、测试 agent 验收和 P1 极短复核，主 agent 判定：正式收口。

当前 P0：无。

当前 P1：无。

当前 P2：

- 既有 Vite chunk size warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项继续排除。

## 2. 已完成能力

- `data_file_resources` 新增稳定公开资产标识 `asset_uuid`。
- 既有文件已回填 UUID。
- `asset_uuid` 已加唯一索引。
- 新建文件入口由后端生成 `assetUuid`，数据库默认值兜底直接 SQL fixture。
- `FileAssetView` / `ModelAssetView` 已输出 `asset_uuid`。
- 文件资源、文件资产、模型资产、catalog list/detail/search、storage-status、M3B 迁移任务行已输出 `assetUuid`。
- 前端主展示改为“平台资产ID”，内部数字 `fileId` 保留为“内部文件ID”。
- 单文件 storage status 统一为：
  - `NAS_ONLY`
  - `MIGRATION_PENDING`
  - `OBJECT_STORED`
  - `MIGRATION_FAILED`

## 3. 验收依据

开发报告：

- `handoff/dev-agent/latest-report.md`

测试报告：

- `handoff/test-agent/latest-report.md`

核心验证结果：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh` 通过，`PASS=15 FAIL=0`。
- M3B / M3A / M2J / M2I / M2H / M2F / file-access 回归通过。
- `git diff --check` 通过。
- 禁出字段扫描通过。

## 4. P1 复核说明

测试 agent 初验发现 P1：

- `V30__m3c1_asset_uuid_storage_status.sql` 为 `??`。
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh` 为 `??`。

极短复核结果：

- 两个文件均已从 `??` 变为 `A`。
- `git ls-files` 可识别。
- `git diff --check` 通过。

后续测试 prompt 固定加入检查：本轮新增 Flyway 迁移、专项脚本和关键代码文件必须纳入 Git 跟踪，不允许仍是 `??`。

## 5. 安全边界

已确认未发生：

- 全量 NAS 迁移。
- 真实 NAS 文件移动、删除、重命名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答扩展。
- 真实 BIM 轻量化。
- raw NAS path、bucket、object key、`storage_uri`、SQL、raw row、token、secret 泄露。

## 6. 下一步

按 M3-M5 任务图，下一步建议进入：

`M3C：对象存储迁移任务中心与批量策略`

定位：

- 在 M3A / M3B / M3C-1 基础上补迁移任务中心、批量策略、进度、失败原因、重试、审计和前端入口。
- 仍不做全量 NAS 搬迁。
- 仍不做 Hermes 正文问答。
- 仍不做 parser / indexing / BIM 轻量化。

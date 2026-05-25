# 测试 Agent 当前任务：M3A 对象存储与 StorageService 基线验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮测试批次：

`M3A：对象存储与 StorageService 基线`

本轮只验收存储底座，不验收全量 NAS 迁移、语义解析、Hermes 正文问答或真实 BIM 引擎。

## 0. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/m3a-storage-service-foundation-plan.md`
- `handoff/main-agent/status.md`

## 1. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 2. M3A 专项验收

必须验证：

1. `GET /api/data-steward/storage/providers/health`
   - 返回统一响应。
   - 包含 NAS / MinIO 或 S3-compatible 的配置状态。
   - 未配置对象存储时不 500。
   - 不返回 endpoint 密钥、access key、secret key、bucket key、真实路径。

2. `GET /api/data-steward/assets/files/{fileId}/storage-status`
   - 返回统一响应。
   - 能表达 `NAS_ONLY` / `OBJECT_STORED` / `MIGRATION_PENDING` / `MIGRATION_FAILED` 等状态之一。
   - 不返回真实 NAS 路径、bucket、object key、storage_uri。

3. `file-access` 回归
   - 原有 NAS 文件预览 / 下载仍可用。
   - 权限校验、生命周期校验、审计链路不回归。
   - 响应契约不破坏前端。

4. 对象存储受控读取
   - 如果开发 agent 提供了 minio/s3 测试对象，需验证可通过后端受控读取。
   - 若本地 MinIO 不可用，必须确认接口返回业务化不可用原因，不 500。

## 3. 禁出字段扫描

对 M3A 新接口和 `file-access` 相关响应扫描，禁止出现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- `storage_path`
- `storagePath`
- `bucket`
- `object_key`
- `objectKey`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`
- `access_key`
- `secret_key`

如果出现在内部日志不直接返回前端，可记录但不直接判失败；如果出现在 API 响应中，按 P0/P1 判定。

## 4. 越界检查

执行：

```bash
git diff --name-only
git status --short
```

重点检查是否出现以下越界：

- 全量 NAS 迁移。
- 真实 NAS 文件移动、删除、重命名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答能力。
- 真实 BIM 引擎接入。
- `docs/**` 未授权修改。

发现上述越界，按影响判 P0/P1。

`.claude/**`、`CLAUDE.md`、`tmp/**` 如仍为既有非交付未跟踪项，不直接判失败，但报告中提醒收口时排除。

## 5. 判定标准

P0：

- API 响应泄露真实 NAS 路径、bucket/object key、token、secret、raw row 或 SQL。
- `file-access` 权限绕过。
- 测试或实现移动、删除、重命名真实 NAS 文件。
- Hermes 直接访问 MySQL / NAS / MinIO / 向量库 / 搜索引擎。
- 新增正文解析、索引写入或真实 BIM 解析能力。

P1：

- 后端构建、前端构建、健康检查、M3A 专项脚本、核心回归脚本失败。
- Provider health 接口不可用或未纳入 OpenAPI。
- File storage status 接口不可用或返回底层路径。
- NAS 原有预览 / 下载回归。
- `minio://` / `s3://` 仍只有生硬 `STORAGE_PROVIDER_UNSUPPORTED`，没有受控业务化处理路径。

P2：

- 既有 Vite chunk warning。
- 非阻塞文案、状态命名或交互粗糙。
- 非交付未跟踪文件提醒。

## 6. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 总结论：通过 / 不通过。
- P0 / P1 / P2。
- 必跑命令结果。
- M3A 专项脚本结果。
- Provider health 接口抽查。
- File storage status 接口抽查。
- NAS file-access 回归结果。
- 对象存储测试对象受控读取结果或不可用原因。
- 禁出字段扫描结果。
- 是否发现越界。
- 是否建议主 agent 收口 M3A。

# 测试 Agent 当前任务：M3F 新文件对象存储优先写入验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收批次：

`M3F：新文件对象存储优先写入与 NAS 兼容回退`

## 0. 验收目标

本轮只验收 M3F：

- 通过平台上传的新文件默认写入对象存储。
- 新文件仍进入 MySQL 业务台账。
- 新文件有 `assetUuid` 和 active object version。
- 新文件 `storage-status=OBJECT_STORED`。
- 新文件可通过受控 `file-access` 读取。
- 历史 NAS 文件、迁移任务、预览产物不回归。

本轮不验收：

- 全量 NAS 搬迁。
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch。
- 文件正文读取。
- 真实 BIM 引擎。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3f-object-storage-first-write-plan.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `scripts/dev/check-m3f-object-storage-first-write.sh`

## 2. 必跑命令

请执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如后端未运行，可按项目已有方式启动后重试健康检查和专项脚本。

## 3. Git 范围检查

请检查：

```bash
git status --short
git diff --name-only
git diff --cached --name-status
```

M3F 允许包含：

- `backend/**` 中与 StorageService、文件上传、对象版本、受控访问相关的最小改动。
- `frontend/**` 中文件管理器上传后状态展示的最小改动。
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- `handoff/dev-agent/latest-report.md`
- 必要的 Flyway 新迁移。

M3F 不允许包含：

- `docs/**`
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch / parser / indexing。
- 真实 BIM 引擎。
- 全量 NAS 迁移入口。
- 真实 NAS 批量移动、删除、重命名能力扩展。

## 4. 核心验收点

重点确认：

1. 新上传文件不是先写真实业务 NAS 目录。
2. 新上传文件创建 `data_file_resources` 记录。
3. 新上传文件有 `assetUuid`。
4. 新上传文件创建 `data_storage_objects`。
5. 新上传文件创建 active `data_file_object_versions`。
6. `GET /api/data-steward/assets/files/{fileId}/storage-status` 返回 `OBJECT_STORED`。
7. `file-access` 可读取对象存储新增文件内容。
8. 对象存储不可用时不 500、不假成功。
9. 历史 NAS 文件仍可走原有受控访问。
10. M3E 预览产物、M3D 灰度镜像、M3C 迁移任务不回归。

## 5. 禁出字段扫描

所有 M3F 响应和脚本输出中不得出现：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- `bucket`
- `object_key`
- `objectKey`
- raw row
- SQL
- token
- secret
- password

说明性文案里可以出现“对象存储”这个业务词，但不能出现真实 bucket / object key 值。

## 6. 浏览器轻量检查

本轮不要求全量浏览器逐页点击，只做轻量检查：

- 打开 `http://127.0.0.1:5173/data-steward/assets/503?tab=files`
- 文件管理器页面不白屏。
- 上传入口仍可见。
- 上传成功后的文件能显示在当前目录。
- 新文件的存储状态或详情可体现对象存储状态。
- 不需要在真实业务目录执行破坏性操作。

## 7. P0 / P1 判定

P0：

- 新上传文件泄露真实 NAS 路径、bucket、object key、storage URI、token、secret。
- 真实业务 NAS 文件被移动、删除、覆盖或改名。
- file-access 权限链路回归失败。
- 引入 Hermes 正文问答、parser、indexing、documents / chunks。
- 全量历史 NAS 迁移被误开启。

P1：

- 新上传文件仍默认写 NAS，未写对象存储。
- 新文件无 `assetUuid`。
- 新文件无 active object version。
- 新文件 storage-status 不是 `OBJECT_STORED`。
- 新文件无法通过受控 file-access 读取。
- 对象存储不可用时假成功或 500。
- M3F 专项脚本失败。
- M3E / M3D / M3C / M3B / M3A 关键回归失败。
- M3F 专项脚本未纳入 Git。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项，只记录，不阻塞。
- 文案细节粗糙但不影响主链路。

## 8. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论：通过 / 不通过。
- P0 / P1 / P2。
- 必跑命令结果。
- M3F 专项脚本结果。
- 新上传文件对象存储验证结果。
- `assetUuid` / active object version / storage-status 验证结果。
- file-access 读取验证结果。
- 对象存储不可用场景验证结果。
- 回归脚本结果。
- 禁出字段扫描结果。
- Git 范围检查结果。
- 是否建议主 agent 收口 M3F。

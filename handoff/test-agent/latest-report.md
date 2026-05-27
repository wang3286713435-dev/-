# M3F 新文件对象存储优先写入验收报告

生成时间：2026-05-27

## 1. 测试结论

结论：M3F 验收通过，建议主 agent 进入 M3F 收口判断。

本轮按用户提供的 M3F prompt 执行，只验收“新文件对象存储优先写入与 NAS 兼容回退”。后端构建、前端构建、健康检查、M3F 专项脚本、M3E / M3D / M3C / M3B / M3A / file-access 回归、轻量浏览器检查和 `git diff --check` 均通过。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响 M3F 主链路。
- 工作区仍存在 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项；本轮按 prompt 记录，不作为 M3F 阻塞。
- staged 范围中包含 `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md`。该文件不是运行时代码，也未触发 M3F 禁止项；建议主 agent 在 checkpoint 前确认是否与 M3F 一并提交。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3f-object-storage-first-write-plan.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `scripts/dev/check-m3f-object-storage-first-write.sh`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3f-object-storage-first-write.sh`：通过，`PASS=10 FAIL=0`。
- `bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`：通过，`PASS=19 FAIL=0`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3b-object-storage-mirror-trial.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3a-storage-service-foundation.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 5. M3F 专项脚本结果

`scripts/dev/check-m3f-object-storage-first-write.sh` 通过，`PASS=10 FAIL=0`。

覆盖点：

- 创建隔离测试项目和安全测试目录。
- 管理员登录、切换项目、开启安全目录写入灰度。
- 通过旧上传入口验证兼容链路，但新文件优先写入对象存储。
- 上传响应返回 `OBJECT_STORED`。
- 新上传文件生成 `assetUuid=d6549a46-6910-45a2-9618-d9bb6a9ef4f8`。
- 新上传文件正文未出现在隔离 NAS 目录中。
- `storage-status` 返回 `OBJECT_STORED` 且 `assetUuid` 稳定。
- 数据库存在且仅存在 1 条 active object version。
- 受控 file-access 可读取对象存储新增文件内容。
- 文件管理器 / catalog 目录列表能看到新文件和对象存储业务状态。
- 对象存储不可用场景失败关闭，不假成功，也不静默回退写 NAS。

## 6. 新上传文件对象存储验证结果

通过。

专项脚本验证新上传文件默认进入对象存储，不是先写真实业务 NAS 目录。上传后：

- 创建了 `data_file_resources` 业务台账记录。
- 生成了 `assetUuid`。
- 创建了 `data_storage_objects`。
- 创建了 active `data_file_object_versions`。
- `GET /api/data-steward/assets/files/{fileId}/storage-status` 返回 `OBJECT_STORED`。
- 隔离 NAS 目录中未出现上传文件正文。

## 7. assetUuid / active object version / storage-status

通过。

- `assetUuid`：已生成并在 storage-status 中稳定返回。
- active object version：脚本确认 exactly one active object version。
- storage-status：返回 `OBJECT_STORED`。

未发现新文件缺失 `assetUuid`、缺失 active object version 或 storage-status 非 `OBJECT_STORED` 的问题。

## 8. file-access 读取验证结果

通过。

专项脚本通过受控 file-access 读取对象存储新增文件，读取内容与上传内容一致，说明：

- 新对象文件没有绕过受控访问。
- 现有 file-access 权限链路未回归。
- 历史 NAS 文件受控访问由 `check-phase2-batch4-file-access.sh` 回归通过，`PASS=18 FAIL=0`。

## 9. 对象存储不可用场景验证结果

通过。

专项脚本临时暂停对象存储服务后验证上传行为：

- 上传失败受控返回业务错误。
- 未出现 500 白屏式失败。
- 未出现“对象存储不可用但接口假成功”。
- 未静默回退写入 NAS 文件。
- 对象存储恢复后，后续回归脚本继续通过。

## 10. 回归脚本结果

通过。

- M3E 预览产物对象化：通过，`PASS=8 FAIL=0`。
- M3D 真实 NAS 对象镜像灰度：通过，`PASS=19 FAIL=0`。
- M3C 迁移任务中心：通过，`PASS=9 FAIL=0`。
- M3B 对象存储镜像试运行：通过，`PASS=11 FAIL=0`。
- M3A 存储服务底座：通过，`PASS=8 FAIL=0`。
- Phase2 batch4 file-access：通过，`PASS=18 FAIL=0`。

未发现历史 NAS 文件、迁移任务、预览产物或受控访问回归。

## 11. 浏览器轻量检查结果

已打开：

- `http://127.0.0.1:5173/data-steward/assets/503?tab=files`

结果：

- 文件管理器页面正常渲染，无白屏。
- 上传入口可见。
- 页面能看到当前目录、目录树、文件表和对象存储相关状态文案。
- 当前真实项目页面未执行上传、移动、删除、重命名等写操作。
- 页面文本 forbidden-field scan 未命中禁出字段。

新文件上传成功、目录列表可见和对象存储状态由 M3F 专项脚本在隔离测试项目中验证；未在真实业务项目 UI 上做破坏性写操作。

## 12. 禁出字段扫描结果

通过。

M3F 专项脚本、回归脚本和浏览器页面检查未发现以下敏感字段或路径泄露：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- bucket
- `object_key`
- `objectKey`
- raw row
- SQL
- token / secret / password

说明性文案中可见“对象存储”业务词，但未暴露真实 bucket / object key 值。

## 13. Git 范围检查结果

已执行：

- `git status --short`
- `git diff --name-only`
- `git diff --cached --name-status`
- `git diff --check`

当前 tracked/staged 范围主要包括：

- `backend/delivery-data-steward/.../ControlledNasApplicationService.java`
- `backend/delivery-data-steward/.../ControlledNasDtos.java`
- `backend/delivery-data-steward/.../ControlledNasRepository.java`
- `backend/delivery-data-steward/.../StorageService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- M3F 相关 handoff / roadmap / status 报告文件

确认：

- 未发现 `docs/**` 纳入本轮 staged。
- 未发现 Hermes 正文问答、documents / chunks、Qdrant、OpenSearch、parser、indexing 文件纳入本轮 staged。
- 未发现真实 BIM 引擎能力纳入本轮 staged。
- 未发现全量 NAS 迁移入口纳入本轮 staged。
- 未发现真实 NAS 批量移动、删除、重命名能力扩展纳入本轮 staged。
- M3F 专项脚本已纳入 Git staged 范围。

记录：

- `.claude/**`、`CLAUDE.md`、`tmp/**` 仍为未跟踪非交付项。
- `handoff/main-agent/m3g-nas-minio-real-project-object-storage-plan.md` 已 staged，建议主 agent 在提交前确认是否保留在同一 checkpoint。

## 14. 越界能力检查

未发现本批交付引入以下越界内容：

- 全量 NAS 搬迁。
- Hermes 正文问答。
- documents / chunks / Qdrant / OpenSearch。
- 文件正文读取。
- 真实 BIM 引擎。
- parser / writer / indexing。
- 真实业务 NAS 文件移动、删除、覆盖或改名。

## 15. 是否建议收口 M3F

建议主 agent 进入 M3F 收口判断。

M3F 的对象存储优先写入、MySQL 业务台账、`assetUuid`、active object version、`OBJECT_STORED`、受控 file-access、对象存储不可用失败关闭、历史回归和禁出字段扫描均已通过；当前仅有 P2 / 记录项，不阻塞 M3F 收口。

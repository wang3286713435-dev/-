# 开发 Agent 报告：M3F 新文件对象存储优先写入与 NAS 兼容回退

时间：2026-05-27 CST

## 1. 本轮目标

本轮按 `M3F：新文件对象存储优先写入与 NAS 兼容回退` 执行。

目标是在保留既有上传入口兼容性的前提下，让新上传文件本体优先写入对象存储，并同步写入文件台账、对象台账和活动对象版本；空文件夹等目录能力继续沿用现有 NAS 管理链路。

本轮不做真实业务 NAS 文件本体写入，不做 parser / indexing / Hermes / BIM 能力，不修改 `docs/**`。

## 2. 已阅读关键文件

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3f-object-storage-first-write-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageProperties.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/controller/ControlledNasController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/repository/ControlledNasRepository.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`

## 3. 改动文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/application/ControlledNasApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/dto/ControlledNasDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/nas/repository/ControlledNasRepository.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`。

## 4. 后端改动

保留既有兼容入口：

- `POST /api/data-steward/projects/{projectId}/nas/files:upload`

内部上传语义调整为对象存储优先：

- 校验项目权限、写入灰度、目标目录和同名冲突。
- 计算上传内容 SHA-256。
- 生成平台 `assetUuid`。
- 文件本体写入活动对象存储 provider。
- `data_file_resources` 写入安全台账，`storage_provider=OBJECT_STORAGE`，`storage_uri=object://asset/{assetUuid}`。
- `data_storage_objects` 写入对象台账，底层 bucket / object_key 只留在后端表内，不返回前端。
- `data_file_object_versions` 写入 active 对象版本。
- 上传响应返回 `storageStatus=OBJECT_STORED`、`storageProvider=OBJECT_STORAGE`、`assetUuid`、`checksum` 等安全字段。

对象存储不可用时：

- 不静默回退到真实业务 NAS。
- 返回业务化错误，前端/脚本可识别。
- MinIO 客户端增加连接/读取/调用超时，避免网关长时间挂起。

兼容边界：

- 创建空文件夹、目录树等目录能力仍走现有 NAS 管理链路。
- 对象存储文件不允许被 NAS 文件移动/重命名/移入回收站接口当成 NAS 路径处理，后端返回清晰业务错误。

## 5. 前端改动

- `NasOperationResponse` 类型补充安全存储字段。
- 文件列表对对象存储文件展示 `对象存储` 标签。
- 上传成功提示追加对象存储状态说明。
- 未新增 raw path、bucket、object_key、storage_uri 展示。

## 6. 数据库迁移

未新增数据库迁移。

本轮复用 M3A-M3E 已有表：

- `data_file_resources`
- `data_storage_objects`
- `data_file_object_versions`

## 7. 安全边界

- 未写入真实业务 NAS 文件本体。
- 未移动、删除、重命名、覆盖真实业务 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未做 parser / indexing / BIM / Hermes 新能力。
- 未暴露真实 NAS 路径、`storage_path`、底层 `storage_uri`、bucket、object_key、raw DB row、SQL、secret、token、password。
- M3F 专项脚本只使用隔离 `/tmp` 测试目录，并在结束时清理。

## 8. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3F 新文件对象存储优先写入专项                                PASS=10 FAIL=0
M3E 预览与转换产物对象化回归                                  PASS=8 FAIL=0
M3D 真实 NAS 小范围灰度镜像回归                               PASS=19 FAIL=0
M3C 对象存储迁移任务中心回归                                  PASS=9 FAIL=0
M3B 对象存储镜像迁移回归                                      PASS=11 FAIL=0
M3A StorageService 回归                                       PASS=8 FAIL=0
Phase2 batch4 文件访问安全回归                                PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

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

## 9. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`
- MinIO 容器已确认处于运行状态。

按用户要求，本轮完成后未关闭项目服务。

## 10. 已知风险与后续建议

- 既有接口路径仍包含 `nas/files:upload`，这是为了兼容前端和历史调用；实际新文件本体已改为对象存储优先写入。
- 对象存储文件的移动、重命名、回收站能力尚未扩展，本轮先禁止被 NAS 操作链路误处理。
- 若后续要支持对象存储文件改名/移动，应新增对象文件管理语义，继续保持审计、权限、灰度和禁出字段扫描。
- 对象存储不可用时本轮按 fail-closed 处理，不建议加入静默 NAS 回退。

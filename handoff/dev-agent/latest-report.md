# 开发 Agent 报告：M3E 预览与转换产物对象化

时间：2026-05-26 CST

## 1. 本轮目标

本轮按 `M3E：预览与转换产物对象化` 执行。

目标是在 M3A-M3D 对象存储基础上，把 PDF / 图片的浏览器原生预览产物登记到对象化链路，并为 Office / CAD / BIM 等需要转换的格式建立只读转换占位状态。

本轮不做真实转换，不读取 PDF / Office / DWG / RVT / IFC 正文，不写 documents / chunks / Qdrant / OpenSearch / Hermes memory，不做 BIM 轻量化，不触碰真实 NAS 文件。

## 2. 已阅读关键文件

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3d-real-nas-object-mirror-gray-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-app/src/main/resources/db/migration/V28__m3a_storage_objects_foundation.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-shared/src/main/java/com/zhuoyu/delivery/shared/preview/FilePreviewPolicy.java`
- `backend/delivery-shared/src/main/java/com/zhuoyu/delivery/shared/preview/PreviewDecision.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/utils/previewStatus.ts`
- `frontend/src/modules/work-center/pages/DeliveryPackageArchivePage.vue`

## 3. 改动文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`。

## 4. 后端改动

新增只读/受控预览产物接口：

- `GET /api/data-steward/assets/files/{fileId}/preview-artifacts`
- `POST /api/data-steward/assets/files/{fileId}/preview-artifacts:prepare`

接口返回：

- `fileId`
- `assetUuid`
- `projectId`
- `artifactType`
- `previewStatus`
- `conversionRequired`
- `generationStatus`
- `storageState`
- `contentType`
- `sizeBytes`
- `lastVerifiedAt`
- `message`

实现规则：

- PDF / 图片：若存在 active `OBJECT_STORED` 对象版本，则登记 `BROWSER_NATIVE_PREVIEW / AVAILABLE / COMPLETED / OBJECT_STORED`，引用同一个 storage object，不重复上传对象。
- Office：登记 `OFFICE_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- CAD：登记 `CAD_PREVIEW_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- BIM / 模型：登记 `BIM_LIGHTWEIGHT_PLACEHOLDER / NEEDS_CONVERSION / NOT_STARTED / PENDING`。
- 不支持格式：登记跳过/不需要对象化的占位状态。
- `prepare` 会校验当前用户对文件所在项目的访问权限，并写审计日志。
- 响应不返回 bucket、object_key、storage_path、storage_uri、NAS 绝对路径或 SQL。

## 5. 前端改动

文件管理器新增“预览产物”列：

- 已对象化 PDF / 图片显示“对象化预览”。
- 需转换格式显示“需转换占位”或后端返回的占位结果。
- 未登记文件显示“未登记 / 需扫描入库”。
- 点击“准备状态”会调用后端 prepare 接口，只登记产物状态或占位，不读取文件正文。
- 右键菜单新增“准备预览产物状态”，与同一后端接口对齐。

PDF / 图片的真实打开仍走原有受控 `file-access` 票据，不直接暴露对象存储定位。

## 6. 数据库迁移

未新增数据库迁移。

本轮复用 M3A 既有表：

- `data_file_derivatives`
- `data_preview_artifacts`
- `data_file_object_versions`
- `data_storage_objects`

## 7. 安全边界

- 未触碰真实 NAS 文件内容。
- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未做真实转换。
- 未做 BIM 轻量化。
- 未写 OpenSearch / Qdrant / MinIO documents / chunks。
- 未写 Hermes memory。
- 未暴露真实 NAS 路径、`storage_path`、`storage_uri`、bucket、object_key、raw DB row、SQL、secret、token、password。

## 8. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3E 预览与转换产物对象化专项                                  PASS=8 FAIL=0
M3D 真实 NAS 小范围灰度镜像回归                               PASS=19 FAIL=0
M3C 对象存储迁移任务中心回归                                  PASS=9 FAIL=0
M3C-1 asset UUID / storage status 回归                         PASS=15 FAIL=0
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
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 9. 服务状态

- 后端已重启并保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`

按用户要求，本轮完成后未关闭项目服务。

## 10. 已知风险与后续建议

- 当前只是对象化状态登记和转换占位，尚未接入真实 Office / CAD / BIM 转换器。
- `data_preview_artifacts` 目前没有 `(file_id, artifact_type)` 唯一约束，本轮在应用层按最新记录更新；后续如要高并发准备，可追加迁移补唯一约束。
- 后续若进入真实转换，应继续沿用受控队列、灰度开关、审计、禁出字段扫描，并继续禁止把 catalog metadata 伪装成正文证据。

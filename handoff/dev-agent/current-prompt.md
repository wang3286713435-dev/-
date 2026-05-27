# 开发 Agent 当前任务：M3E 预览与转换产物对象化

你是卓羽智能数据中台 v1 的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3e-preview-artifacts-object-storage`

## 0. 本批定位

本批是：

`M3E：预览与转换产物对象化`

M3A/M3B/M3C/M3D 已经完成：

- StorageService 与对象存储基础表。
- assetUuid / storage-status。
- 对象存储迁移任务中心。
- 105 项目 PDF / DWG / RVT 小范围真实 NAS 对象镜像。

M3E 的目标不是做真实转换器，而是把“预览产物 / 转换产物 / 未来 BIM 轻量化产物”的关系接入对象存储底座，让平台能清楚回答：

- 这个文件有没有可用预览产物？
- 预览产物是否已经对象化？
- 如果不能预览，是缺转换、格式不支持，还是权限不足？
- 预览/转换产物是否仍通过平台受控 file-access / preview ticket 访问？

## 1. 必须先阅读

开始前先阅读：

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3d-real-nas-object-mirror-gray-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `backend/delivery-app/src/main/resources/db/migration/V28__m3a_storage_objects_foundation.sql`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-shared/src/main/java/com/zhuoyu/delivery/shared/preview/FilePreviewPolicy.java`
- `backend/delivery-shared/src/main/java/com/zhuoyu/delivery/shared/preview/PreviewDecision.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/utils/previewStatus.ts`
- `frontend/src/modules/work-center/pages/DeliveryPackageArchivePage.vue`

## 2. 严格边界

本批允许：

- 后端接入已有 `data_file_derivatives`、`data_preview_artifacts` 表。
- 如确实缺字段，可追加新 Flyway 迁移；不得修改已应用迁移。
- 新增只读 / 受控 prepare 接口。
- 前端展示预览产物状态、转换状态、对象化状态。
- 新增专项脚本。

本批禁止：

- 不做真实 PDF / Office / CAD / BIM 转换器。
- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不新增 Hermes 正文问答。
- 不进入 BIM 引擎真实接入。
- 不移动、删除、重命名、覆盖真实 NAS 文件。
- 不全量迁移 NAS。
- 不暴露 `/Volumes`、`smb://`、`nas://`、`storage_uri`、bucket、object_key、raw row、SQL、token、secret。
- 不修改 `docs/**`。

## 3. 推荐实现口径

### A. 产物模型

优先复用 M3A 已有表：

- `data_file_derivatives`
- `data_preview_artifacts`

建议最小状态口径：

- `artifactType`
  - `BROWSER_NATIVE_PREVIEW`
  - `OFFICE_PREVIEW_PLACEHOLDER`
  - `CAD_PREVIEW_PLACEHOLDER`
  - `BIM_LIGHTWEIGHT_PLACEHOLDER`
  - `THUMBNAIL_PLACEHOLDER`
- `previewStatus`
  - `AVAILABLE`
  - `NEEDS_CONVERSION`
  - `UNSUPPORTED`
  - `BLOCKED`
  - `NOT_STARTED`
- `storageState`
  - `OBJECT_STORED`
  - `PENDING`
  - `NOT_REQUIRED`
- `generationStatus`
  - `COMPLETED`
  - `NOT_STARTED`
  - `SKIPPED`
  - `FAILED`

### B. 浏览器原生预览

对 PDF / 图片这类浏览器原生可预览文件：

- 如果文件已有 active 对象版本，可创建或更新 `BROWSER_NATIVE_PREVIEW` artifact。
- artifact 可指向同一个 active storage object，不需要重复上传一份对象。
- `file-access` 仍是唯一受控访问入口。
- 前端不得拿到 bucket / object_key。

### C. 需要转换的文件

对 Office / DWG / RVT / IFC / NWD / NWC 等需要后续转换的文件：

- 本批只创建或返回 placeholder 状态。
- 不调用转换器。
- 不读取正文。
- 不伪造可预览。
- 页面应明确显示“需要转换产物，当前未生成”。

### D. 交付包预检查

交付包 / 档案目录中已有 `previewStatus`、`conversionStatus`、`conversionRequired`。

本批需要让这些字段与 preview artifact 口径保持一致：

- 已有浏览器原生预览 artifact 的条目可显示可预览。
- 需要转换但没有产物的条目继续作为阻塞 / 需转换。
- 不生成真实 ZIP。
- 不复制 NAS 文件。

## 4. 建议新增接口

命名按现有代码风格微调，但语义必须稳定：

- `GET /api/data-steward/assets/files/{fileId}/preview-artifacts`
  - 查询某个文件的预览 / 转换产物状态。
- `POST /api/data-steward/assets/files/{fileId}/preview-artifacts:prepare`
  - 受控创建或刷新该文件的预览产物记录。
  - 对 PDF / 图片：如已有对象版本，创建 `AVAILABLE` artifact。
  - 对 DWG / RVT / Office：只创建 `NEEDS_CONVERSION` placeholder。

响应必须包含：

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

响应绝不包含：

- 真实 NAS 路径
- bucket
- object key
- `storage_uri`
- raw row
- SQL
- token / secret

## 5. 前端要求

最小前端接入即可，不要大改 UI：

- 文件管理器中预览状态可显示“对象化预览 / 需转换 / 暂不支持”。
- 文件详情或预览状态区域能看到预览产物状态。
- 对 `NEEDS_CONVERSION` 的文件，按钮或提示要明确“后续接入转换服务后可生成预览”，不能假装可预览。
- PDF / 图片继续通过受控 `file-access` 打开。
- DWG / RVT 仍走模型/转换占位，不跳转伪 3D。

## 6. 审计与权限

- `prepare` 接口必须校验当前用户项目权限。
- 不允许跨项目 fileId。
- 创建 / 刷新 artifact 记录必须写审计日志。
- 普通查看不会全量审计，按现有风格即可。
- 无权限返回业务化错误，不泄露文件存在性和底层路径。

## 7. 专项脚本

新增：

`scripts/dev/check-m3e-preview-artifacts-object-storage.sh`

脚本至少验证：

1. 登录管理员。
2. 选择 105 / projectId=503。
3. 找到已 `OBJECT_STORED` 的 PDF 样本。
4. 调用 prepare，返回 `BROWSER_NATIVE_PREVIEW / AVAILABLE / OBJECT_STORED`。
5. 查询 preview-artifacts，能看到同一状态。
6. 通过受控 file-access 仍可打开 PDF，不暴露底层定位。
7. 找到 DWG 或 RVT 样本。
8. 调用 prepare，只返回 `NEEDS_CONVERSION` placeholder，不读取正文、不生成真实转换文件。
9. 交付包预检查字段不回归。
10. 禁出字段扫描通过。

必须回归：

- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`
- `scripts/dev/check-m3c1-asset-uuid-storage-status.sh`
- `scripts/dev/check-m3b-object-storage-mirror-trial.sh`
- `scripts/dev/check-m3a-storage-service-foundation.sh`
- `scripts/dev/check-phase2-batch4-file-access.sh`

## 8. 自测要求

完成后至少执行并记录：

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

## 9. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改动文件清单。
- 是否新增迁移。
- 是否复用 `data_file_derivatives` / `data_preview_artifacts`。
- PDF / 图片 preview artifact 口径。
- DWG / RVT / Office placeholder 口径。
- file-access 是否回归。
- 交付包预检查是否回归。
- 禁出字段扫描结果。
- 自测命令结果。
- 未完成事项和风险。

## 10. 完成定义

只有同时满足以下条件，才可交给测试 agent：

- PDF / 图片对象化预览产物状态可查。
- DWG / RVT / Office 只返回需转换状态，不伪造预览。
- file-access 仍是唯一受控访问入口。
- API / 前端不泄露真实路径或对象定位。
- 不读取正文、不写语义索引、不动 Hermes。
- 前后端构建、专项脚本、关键回归和 `git diff --check` 通过。

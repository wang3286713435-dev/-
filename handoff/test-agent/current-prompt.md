# 测试 Agent 当前任务：M3E 预览与转换产物对象化验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收批次：

`M3E：预览与转换产物对象化`

## 0. 重要上下文

本轮 M3E 开发文件已经 staged。请重点检查 staged M3E 交付，不要把工作区里未暂存的品牌命名口径修改、`.claude/**`、`CLAUDE.md`、`tmp/**` 当作 M3E 越界。

M3E 目标：

- PDF / 图片等浏览器原生可预览文件，可以登记 `BROWSER_NATIVE_PREVIEW / AVAILABLE / OBJECT_STORED`。
- Office / CAD / BIM / RVT / DWG 等需要转换的文件，只登记转换占位，不做真实转换。
- 预览打开仍走受控 `file-access`。
- 不暴露真实 NAS 路径、bucket、object_key、storage_uri。

M3E 不做：

- 不读取 PDF / Office / DWG / RVT / IFC 正文。
- 不做真实转换器。
- 不写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 不做 Hermes 正文问答。
- 不移动、删除、重命名、覆盖真实 NAS 文件。
- 不修改 `docs/**`。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3e-preview-artifacts-object-storage-plan.md`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`

## 2. 必跑命令

请执行：

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
git diff --cached --check
git diff --check
```

如后端未运行，可按项目既有脚本启动后重试健康检查和专项脚本。

## 3. Git 范围检查

请检查 staged 文件：

```bash
git diff --cached --name-status
```

M3E staged 允许包含：

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`
- `handoff/dev-agent/latest-report.md`
- 测试报告本身

不允许 staged 出现：

- `docs/**`
- `backend/delivery-app/src/main/resources/db/migration/*.sql` 新迁移，除非报告明确说明原因
- Hermes 正文问答相关大改
- parser / indexing / Qdrant / OpenSearch / documents / chunks
- 真实 NAS 写操作扩展

## 4. API / 脚本验收重点

重点确认：

1. `GET /api/data-steward/assets/files/{fileId}/preview-artifacts` 可用。
2. `POST /api/data-steward/assets/files/{fileId}/preview-artifacts:prepare` 可用。
3. 已对象化 PDF / 图片样本返回：
   - `artifactType=BROWSER_NATIVE_PREVIEW`
   - `previewStatus=AVAILABLE`
   - `generationStatus=COMPLETED`
   - `storageState=OBJECT_STORED`
4. DWG / RVT / Office 样本返回转换占位：
   - `previewStatus=NEEDS_CONVERSION`
   - `generationStatus=NOT_STARTED`
   - `storageState=PENDING`
5. `file-access` 仍可读取对象镜像。
6. 交付包导出预检查不回归。
7. 所有响应禁出字段扫描通过，不包含：
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
   - token / secret / password

## 5. 浏览器检查

本轮不要求全量浏览器逐页点击。

只做轻量检查：

- 打开 `http://127.0.0.1:5173/data-steward/assets/503?tab=files`
- 文件管理器页面不白屏。
- 文件列表能看到“预览产物”列或对应入口。
- 点击/右键“准备预览产物状态”不导致页面崩溃。
- 不需要执行真实 NAS 写操作。

## 6. P0 / P1 判定

P0：

- 真实 NAS 文件被移动、删除、重命名或覆盖。
- API / 前端泄露真实 NAS 路径、bucket、object_key、storage_uri、token、secret。
- Hermes 正文问答、parser、indexing、documents/chunks 被误引入。
- file-access 权限链路回归失败。

P1：

- PDF / 图片无法登记对象化预览产物。
- DWG / RVT / Office 被误标记为已可预览，或伪造转换完成。
- M3E 专项脚本失败。
- staged 文件遗漏 M3E 专项脚本。
- staged 中出现 `docs/**` 或无关大改。

P2：

- 既有 Vite chunk warning。
- 非本批 unstaged 命名口径修改或 `.claude/**`、`CLAUDE.md`、`tmp/**` 未跟踪项，只记录，不阻塞 M3E。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论：通过 / 不通过。
- P0 / P1 / P2。
- 必跑命令结果。
- M3E 专项脚本结果。
- PDF / 图片对象化预览产物验证结果。
- DWG / RVT / Office 转换占位验证结果。
- file-access 和交付包预检查回归结果。
- staged 文件范围检查结果。
- 禁出字段扫描结果。
- 是否建议主 agent 收口 M3E。

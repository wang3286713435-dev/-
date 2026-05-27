# 开发 Agent 报告：M3G-1 P1 回归修复

时间：2026-05-27 CST

## 1. 本轮目标

本轮按当前修复批次执行，目标是修复测试 agent 在 M3G-1 后发现的回归点：

1. M3E 预览产物对象化回归在 NAS 侧 MinIO 下失败。
2. M3F 对象存储优先写入脚本仍按本机 Docker MinIO 模拟故障。
3. 单项目对象化盘点缺少顶层 `projectCode` / `projectName`。

本轮不执行真实历史文件迁移，不移动、删除、重命名或覆盖真实 NAS 项目文件。

## 2. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `scripts/dev/check-m3f-object-storage-first-write.sh`
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`。

## 3. M3E file-access 失败根因

测试失败样本 `fileId=993` 已有 active object metadata，但该记录指向旧对象存储环境生成的对象。后端切到 NAS 侧 MinIO 后，当前对象存储里没有对应 object body。

之前流程是：

- `preview-artifacts:prepare` 看到 active object metadata 就认为可复用。
- 后续受控 `file-access` 严格从对象存储读取。
- 当前 provider 读不到 body，于是返回 `ASSET_FILE_NOT_READABLE`。

这个失败不是权限问题，也不是 file-access 应该兜底读 NAS 的问题，而是预览准备阶段没有校验 active object 在当前 provider 下是否真实可读。

## 4. M3E 修复方式

在 `AssetApplicationService.preparePreviewArtifact` 中，仅对 PDF / 图片等 `BROWSER_NATIVE_PREVIEW` 场景增加 active object 可读性确认。

修复规则：

- 如果 active object 在当前对象存储可读，继续复用。
- 如果 active object 不可读，且错误是 `ASSET_FILE_NOT_READABLE`，在 `preview-artifacts:prepare` 阶段执行一次受控对象修复。
- 修复使用既有 `StorageService.mirrorNasFileToObject`，将受控源文件重新对象化到当前 provider。
- 写入或更新 `data_storage_objects`、`data_file_object_versions`，并记录审计动作 `asset.file.preview_artifact.object_repaired`。
- 受控 `file-access` 本身仍保持对象存储优先和 fail-closed，不做静默 NAS fallback。

本轮没有读取文件正文、没有解析 PDF/Office/DWG/RVT/IFC 内容；修复是二进制流式对象修复，不产生正文 evidence。

## 5. M3F 脚本修复方式

`scripts/dev/check-m3f-object-storage-first-write.sh` 现在先读取：

- `endpointType`
- `readinessStatus`

脚本分支：

- `LOCAL_DEV_MINIO`：如果发现本机 MinIO 容器，继续暂停容器模拟对象存储不可用，验证 fail-closed。
- `NAS_SIDE_MINIO`：不暂停本机容器，改为验证 readiness 已识别且对象优先上传链路通过。
- 其他 endpoint：跳过本机容器暂停模拟，并输出当前 endpointType。

这样不会再用本机 Docker 容器去模拟 NAS 侧 MinIO 的故障。

## 6. 单项目盘点字段修复

`StorageObjectificationInventoryResponse` 顶层补齐：

- `projectCode`
- `projectName`

当请求为单项目盘点时，这两个字段来自该项目聚合行；全项目盘点时保持 `null`。前端类型 `StorageObjectificationInventory` 已同步更新。

M3G-1 专项脚本已新增断言，确认单项目接口顶层和 `projects[0]` 均返回项目编码和项目名称。

## 7. 是否执行真实历史迁移

否。

本轮没有启动历史文件批量迁移任务，没有执行全量对象化，没有移动、删除、重命名、覆盖真实 NAS 文件。

M3E 修复只在用户显式调用预览产物准备时，对不可读的原生预览对象做受控对象修复；这不是历史文件迁移批处理。

## 8. 是否触碰真实 NAS 文件

未对真实 NAS 做任何写操作。

本轮没有修改、移动、删除、重命名真实 NAS 文件。M3E 预览修复可能以只读流式方式读取受控源文件以补齐当前对象存储缺失的 body，但不解析正文、不写回 NAS、不改变 NAS 目录结构。

## 9. 禁出字段扫描

通过。

脚本与接口响应未发现以下内容泄露：

- `/Volumes`
- `/Users`
- `/tmp`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- bucket 真值
- object key
- endpoint 原文
- raw row
- SQL
- token / secret / password / access key

## 10. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-1 NAS 侧 MinIO readiness / inventory / dry-run 专项       PASS=9 FAIL=0
M3E 预览与转换产物对象化回归                                  PASS=8 FAIL=0
M3F 新文件对象存储优先写入回归                                PASS=11 FAIL=0
M3C 对象存储迁移任务中心回归                                  PASS=9 FAIL=0
Phase2 batch4 文件访问安全回归                                PASS=18 FAIL=0
```

已执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
```

## 11. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`

按用户要求，本轮完成后不关闭项目服务。

## 12. 未完成事项和风险

- M3E 的受控修复目前只挂在原生预览准备链路上，不代表可以对所有历史对象做批量迁移。
- 若后续要对历史文件做正式对象化迁移，仍需单独确认范围、批量策略、回滚方案和 NAS 原文件冻结边界。
- 当前修复不改变 file-access 的 fail-closed 策略；如果对象存储不可读且未经过受控修复，访问仍会失败，这是预期安全行为。

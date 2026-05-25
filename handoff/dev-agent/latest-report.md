# 开发 Agent 报告：M3A 对象存储与 StorageService 基线

时间：2026-05-25 CST

## 1. 本轮目标

本轮按 `M3A：对象存储与 StorageService 基线` 执行。

目标是先建立对象存储元数据基线和统一 `StorageService` 抽象，让现有文件预览 / 下载链路可以在不改变前端入口、不破坏 NAS 访问的前提下，同时支持 NAS 源文件和 MinIO/S3-compatible 对象引用。

本轮不做全量 NAS 迁移、不做后台搬迁任务、不做 parser/indexing、不读取文件正文、不改 Hermes/BIM、不开放新的真实 NAS 写能力。

## 2. 改动文件列表

- `backend/delivery-app/src/main/resources/db/migration/V28__m3a_storage_objects_foundation.sql`
- `backend/delivery-app/src/main/resources/application.yml`
- `backend/delivery-data-steward/pom.xml`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageProperties.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
- `scripts/dev/check-m3a-storage-service-foundation.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未修改前端业务代码。

## 3. 数据库迁移说明

新增 Flyway 迁移 `V28__m3a_storage_objects_foundation.sql`，只追加新表，不修改旧迁移：

- `data_storage_objects`：对象存储对象元数据。
- `data_file_object_versions`：文件与对象版本关系。
- `data_object_migration_tasks`：后续迁移任务记录基线。
- `data_file_derivatives`：衍生文件元数据基线。
- `data_preview_artifacts`：预览产物元数据基线。

迁移中保留内部定位字段用于平台服务端访问，但本轮新增 API 不向前端返回底层对象定位信息、真实 NAS 路径或存储 URI。

## 4. 后端实现

- 新增 `StorageProperties`，支持 `delivery.storage.minio` 与 `delivery.storage.s3-compatible` 配置。
- 新增 `StorageService`：
  - `ensureReadable(file)`：访问票据创建前校验 NAS / 对象存储可读。
  - `openReadable(file)`：统一打开 NAS 文件或对象存储对象，返回 Spring `Resource`。
  - `providerHealth()`：返回 NAS / MinIO / S3-compatible 健康状态。
  - `fileStorageStatus(file)`：返回单文件当前存储状态。
- 现有文件访问链路已改为内部调用 `StorageService`：
  - `POST /api/data-steward/assets/files/{fileId}/access-tickets`
  - `GET /api/data-steward/assets/file-access/{ticket}`
- 新增接口：
  - `GET /api/data-steward/storage/providers/health`
  - `GET /api/data-steward/assets/files/{fileId}/storage-status`

## 5. 安全与脱敏边界

- API 响应不返回：
  - 真实 NAS 绝对路径
  - `storage_path`
  - `storage_uri`
  - 对象存储底层定位信息
  - raw DB row
  - SQL
  - secret / password
- MinIO/S3-compatible 当前只作为受控只读访问源接入。
- 未做对象存储写入业务接口。
- 未做真实 NAS 文件移动、删除、重命名、上传。
- 未改 Hermes、BIM、parser、indexing。

## 6. 专项脚本

新增：

```bash
bash scripts/dev/check-m3a-storage-service-foundation.sh
```

覆盖内容：

- 管理员登录并切换 105 / projectId=503。
- 存储 provider health 返回 NAS / MinIO / S3-compatible 统一结构。
- health 响应通过 forbidden-field scan。
- 既有 NAS 文件 `storage-status` 可查且不泄露路径。
- MinIO 容器中创建临时 smoke 对象，登记为临时测试文件。
- 通过现有预览票据读取 MinIO 对象内容。
- 临时测试文件资源和 MinIO smoke 对象已清理。

## 7. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3A StorageService 专项脚本                                  PASS=8 FAIL=0
M2J 105 归属复核回归                                         PASS=6 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
M2F 真实项目交付闭环回归                                     PASS=6 FAIL=0
M2B NAS 写入灰度回归                                         PASS=18 FAIL=0
Phase2 batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

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

## 8. 已知风险与未完成事项

- M3A 只完成 StorageService 基线和 MinIO/S3-compatible 只读访问能力，不代表完成 NAS 到对象存储迁移。
- `data_object_migration_tasks` 等迁移相关表只是后续 M3B/M3C 的元数据基座，本轮没有后台搬迁 worker。
- 前端尚未新增存储状态展示，本轮只提供后端接口。
- S3-compatible 默认未启用；本轮健康接口可表达未配置状态，实际 S3-compatible 环境需后续配置后再验。
- 生产 rollout 未做。

## 9. 服务状态

- 后端已重启到本轮最新构建，运行在 `http://127.0.0.1:8080`
- 前端 dev server 保持运行，地址为 `http://127.0.0.1:5173`
- 按用户要求，本轮完成后未关闭项目服务。

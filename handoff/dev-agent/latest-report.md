# 开发 Agent 报告：M3G-2 105 项目历史文件对象化上传灰度

时间：2026-05-28 CST

## 1. 本轮目标

本轮执行 `M3G-2：105 项目历史文件对象化上传灰度`。

目标是在已验证 `NAS_SIDE_MINIO / READY` 的前提下，只对 105 项目本地 `projectId=503` 做小批量真实历史文件对象化上传，验证对象存储读取链路，同时保持 NAS 原文件不移动、不删除、不改名、不覆盖。

## 2. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3g2-105-objectification-gray.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`。

## 3. 后端改动

修复 `storage-objectification-plans:dry-run` 多筛选条件组合时的 SQL 拼接空格问题。

本轮专项脚本用到了：

- `storageState=NAS_ONLY`
- `extensions=pdf/png/jpg/jpeg/webp/docx/xlsx/pptx/dwg`
- `maxSizeBytes=10485760`
- `maxTotalBytes=31457280`

首次执行时发现后端 SQL 拼接成 `... = 0ORDER BY ...`，已改为每个条件换行拼接，并在 `ORDER BY` 前补稳定换行。修复后 dry-run 可正常生成小批候选计划。

未新增数据库迁移。

## 4. 前端改动

文件服务 / 对象存储页面的 dry-run 结果区新增：

- `加入小批清单`
- `执行小批灰度`

入口规则：

- 只从 dry-run 样本里选择 `NAS_ONLY` 文件。
- 只选择 `ELIGIBLE_DRY_RUN` / `MISSING_CHECKSUM` 原因的文件。
- 遵守后端 `maxFilesPerTask` 与 `maxFileSizeBytes`。
- 仅当 readiness 为 `NAS_SIDE_MINIO / READY` 且可写时允许执行。
- 文案明确说明 NAS 原文件保留，只复制副本到对象存储，不读取正文、不写语义索引。

执行成功后刷新对象化盘点、任务列表和项目迁移摘要。

## 5. 新增专项脚本

新增：

`scripts/dev/check-m3g2-105-objectification-gray.sh`

脚本覆盖：

1. 管理员登录并切换 105 / projectId=503。
2. readiness 必须是 `NAS_SIDE_MINIO / READY`。
3. 105 对象化盘点可查。
4. dry-run 可生成计划且不启动迁移任务。
5. 从 dry-run 计划选取可读小样本。
6. 创建真实小批对象化迁移任务。
7. 验证已对象化文件为 `OBJECT_STORED`。
8. 验证已对象化文件可通过受控 `file-access` 读取。
9. 验证未对象化对照文件仍为 `NAS_ONLY` 且可通过 NAS 链路读取。
10. 验证重复执行幂等跳过，active object version 不重复污染。
11. 验证 NAS 原文件仍存在且 size / mtime 未变化。
12. 响应 forbidden-field scan 通过。
13. 专项脚本已纳入 Git 跟踪。

## 6. 本轮实际对象化结果

M3G-2 专项执行结果：

```text
taskId=143
rerunTaskId=144
success=3
skipped=0
failure=0
```

实际对象化的 105 历史文件：

```text
fileId=936
fileId=937
fileId=938
```

读取链路验证：

- `fileId=936`：`OBJECT_STORED`，受控 `file-access` 可读取。
- `fileId=937`：`OBJECT_STORED`，受控 `file-access` 可读取。
- `fileId=938`：`OBJECT_STORED`，受控 `file-access` 可读取。

未对象化对照：

- `fileId=939` 仍为 `NAS_ONLY`，受控 `file-access` 可读取。

## 7. 105 覆盖率变化

专项脚本记录：

```text
OBJECT_STORED: 3 -> 6
NAS_ONLY:      2925 -> 2922
覆盖率:        0.1% -> 0.2%
```

说明：本轮只做 105 小批灰度，不代表 105 全量历史文件已对象化。

## 8. NAS 原文件状态

未移动、未删除、未改名、未覆盖真实 NAS 原项目文件。

脚本对本轮对象化样本记录对象化前后的 `size/mtime`，结果：

- `fileId=936`：NAS 原文件仍存在且 `size/mtime` 未变化。
- `fileId=937`：NAS 原文件仍存在且 `size/mtime` 未变化。
- `fileId=938`：NAS 原文件仍存在且 `size/mtime` 未变化。

本轮“上传 MinIO”仅表示复制副本到对象存储，并写入对象台账与 active object version。

## 9. 安全边界

已确认：

- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未触发 Hermes。
- 未接入 BIM 引擎。
- 未全量迁移全部项目。
- 未一键迁移 NAS 根目录。
- 未把对象副本异常静默 fallback 到 NAS。

禁出字段扫描通过，接口响应未暴露：

- `/Volumes`
- `/Users`
- `/tmp`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- bucket
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
M3G-2 105 对象化灰度专项                                      PASS=23 FAIL=0
M3G-1 readiness / inventory / dry-run 回归                    PASS=9 FAIL=0
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
bash scripts/dev/check-m3g2-105-objectification-gray.sh
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

- 本轮只完成 105 小批灰度，不是全量历史文件对象化。
- M3G-2 专项脚本每次运行会从当前 dry-run 的 `NAS_ONLY` 小样本里选择最多 3 个文件，因此重复执行可能继续推进 105 少量文件对象化；这是灰度执行脚本，不是只读脚本。
- 后续如要扩大批量，需要另行确认批次大小、文件类型、失败处理、容量上限和运维观察窗口。

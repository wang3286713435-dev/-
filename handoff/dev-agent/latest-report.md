# 开发 Agent 报告：M3G-4 受控多项目小批对象化执行

时间：2026-05-28 CST

## 1. 本轮目标

本轮执行 `M3G-4：受控多项目小批对象化执行`。

目标是在 M3G-3 多项目 dry-run 已收口后，新增一个必须人工确认、受后端硬上限约束、可审计且可幂等的真实小批对象化执行闭环。

本批允许真实复制小批 NAS 文件副本到 NAS 侧 MinIO，但不做全量迁移、不迁移 NAS 根目录、不读取正文、不改动 NAS 原文件。

## 2. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移。

## 3. 新增 / 修改接口

新增接口：

```http
POST /api/data-steward/storage-objectification-plans:execute
```

请求新增 DTO：

- `MultiProjectStorageObjectificationExecuteRequest`
- 支持 `projectIds`、`fileIds`、`confirmed`、`targetProvider`
- 复用 M3G-3 dry-run 的筛选和策略字段

响应新增 DTO：

- `MultiProjectStorageObjectificationExecuteResponse`
- `taskSource=MULTI_PROJECT_CONTROLLED_EXECUTION`
- 返回项目数、文件数、容量、任务 ID、成功 / 跳过 / 失败数、项目级执行结果和安全提示

现有 M3G-3 dry-run request 兼容新增 `fileIds` 字段，但 dry-run 语义仍保持只读。

## 4. 后端硬上限

后端强制实现：

- `confirmed=true` 缺失或为 false：拒绝。
- 必须显式传入 `projectIds` 和 dry-run 选中的 `fileIds`。
- 单次最多 3 个真实项目。
- 总文件数最多 9 个。
- 单项目最多 3 个文件。
- 单项目容量最多 50MB。
- 总容量最多 100MB。
- 只允许当前账号可访问且 `realNasProject=true` 的项目。
- 非受控存储引用、不可读风险、超出小样本单文件大小限制的文件会被拒绝。

执行内部复用现有对象迁移任务中心和 `StorageService.mirrorNasFileToObject` 流程，因此继续保留：

- 权限校验。
- 生命周期校验。
- NAS 文件可读性校验。
- checksum / size / etag 校验。
- `data_storage_objects` 写入。
- active `data_file_object_versions` 写入。
- 重复执行幂等跳过。
- 审计记录。

## 5. 前端入口

文件服务页“多项目对象化规划”区域新增：

- “确认执行小批对象化”按钮。
- 执行前确认提示。
- 执行结果摘要。
- 项目级任务结果表。

前端只从 dry-run 样本中选择：

- 真实 NAS 项目。
- `NAS_ONLY` 文件。
- `ELIGIBLE_DRY_RUN` / `MISSING_CHECKSUM` 原因。
- 单文件不超过当前小样本限制。
- 总数、单项目数、总容量、单项目容量均在 M3G-4 上限内。

页面文案明确：只复制副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件，不读取正文，不写语义索引。

## 6. 本轮真实执行结果

M3G-4 专项脚本实际选择并执行：

```text
projectId=512 / code=108 / 福城南产业片区11-20-02宗地
fileId=33475
fileName=ZS-000 图纸目录.dwg
size=232970

projectId=506 / code=93 / 中建八局国交酒店项目
fileId=18652
fileName=卓羽智能-BIM智慧建造服务.pdf
size=6325776

projectId=505 / code=101 / C塔
fileId=13196
fileName=消水施_X-C-A001_V3.0_B座消火栓系统原理图(一).pdf
size=1152819
```

总计：

```text
真实项目数：3
文件数：3
总容量：7,711,565 bytes
```

首次执行任务：

```text
taskId=171 projectId=505 fileId=13196 COMPLETED OBJECT_STORED
taskId=172 projectId=506 fileId=18652 COMPLETED OBJECT_STORED
taskId=173 projectId=512 fileId=33475 COMPLETED OBJECT_STORED
```

重复执行幂等验证：

```text
taskId=174 projectId=505 fileId=13196 SKIPPED OBJECT_STORED
taskId=175 projectId=506 fileId=18652 SKIPPED OBJECT_STORED
taskId=176 projectId=512 fileId=33475 SKIPPED OBJECT_STORED
```

## 7. NAS 与安全边界

明确回答：

- 是否移动真实 NAS 原文件：否。
- 是否删除真实 NAS 原文件：否。
- 是否重命名真实 NAS 原文件：否。
- 是否覆盖真实 NAS 原文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否写 documents / chunks / Qdrant / OpenSearch / Hermes memory：否。
- 是否触发 Hermes：否。
- 是否接入 BIM / parser / indexing：否。
- 是否暴露 raw NAS path、`storage_uri`、bucket、object key、SQL、raw row、token / secret：否。

专项脚本对 3 个样本记录对象化前后 NAS 原文件 `size/mtime`，结果均未变化。

## 8. 新增专项脚本

新增：

`scripts/dev/check-m3g4-controlled-multi-project-objectification.sh`

覆盖：

1. 管理员登录。
2. `NAS_SIDE_MINIO / READY` 且 writable。
3. 自动选择最多 3 个真实项目、每项目最多 1 个可读 `NAS_ONLY` 小样本。
4. `confirmed=false` 被拒绝。
5. 超出总文件数硬上限被拒绝。
6. `confirmed=true` 小批真实对象化成功。
7. 重复执行同一批文件幂等跳过。
8. 已对象化文件 `storage-status=OBJECT_STORED`。
9. 已对象化文件可通过受控 `file-access` 下载读取。
10. NAS 原文件仍存在且 `size/mtime` 未变化。
11. forbidden-field scan 通过。
12. 脚本已纳入 Git 跟踪。

结果：

```text
M3G-4 专项 PASS=21 FAIL=0
```

## 9. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-4 受控多项目小批对象化专项                               PASS=21 FAIL=0
M3G-3 多项目对象化规划回归                                   PASS=11 FAIL=0
M3G-1 readiness / inventory / dry-run 回归                    PASS=9 FAIL=0
M3F 新文件对象存储优先写入回归                                PASS=11 FAIL=0
M3E 预览与转换产物对象化回归                                  PASS=8 FAIL=0
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
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
```

## 10. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`

按用户要求，本轮完成后不关闭项目服务。

## 11. 未完成事项和后续风险

- M3G-4 只证明多项目小批真实对象化可控执行，不代表全量迁移完成。
- 当前执行入口要求显式 fileIds，避免基于宽泛筛选条件重复执行时继续选中新文件。
- 并发、限速字段仍是策略回显和前端约束，后台 worker 级调度仍未开放。
- 后续如扩大批量，需要单独确认项目范围、批量上限、执行窗口、失败处理和运维观察策略。

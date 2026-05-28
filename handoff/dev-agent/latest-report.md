# 开发 Agent 报告：M3G-3 多真实项目分批对象化策略与任务中心增强

时间：2026-05-28 CST

## 1. 本轮目标

本轮执行 `M3G-3：多真实项目分批对象化策略与任务中心增强`。

目标是在 `NAS_SIDE_MINIO / READY` 已识别、105 小批灰度已完成的前提下，补齐多真实项目对象化前的只读盘点和 dry-run 规划能力：

- 平台能区分真实 NAS 项目、测试/样例项目、归档/待确认项目。
- 全项目对象化盘点能展示待对象化容量、路径风险、文件类型/扩展名分布。
- 新增多项目对象化 dry-run 计划接口，支持总量、单项目、并发、限速字段。
- 前端文件服务页增加“多项目对象化规划”视图。
- 不执行真实历史文件迁移，不创建迁移任务。

## 2. 读取材料

已读取：

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g3-multi-project-objectification-task-center-plan.md`
- `handoff/main-agent/m3g2-105-objectification-gray-closure.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- M3G / M3E / M3F / M3C 相关脚本和存储服务代码

## 3. 改动文件清单

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3g3-multi-project-objectification-planning.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移。

## 4. 后端改动

增强 `storage-objectification-inventory`：

- 项目级盘点新增 `projectStage`、`assetSource`、`projectCategory`、`realNasProject`。
- 新增 `nasOnlyBytes`、`estimatedObjectificationBytes`、`unreadablePathFiles`。
- 新增 `fileKindDistribution`、`extensionDistribution`。
- 风险等级计算纳入 unreadable/path risk。

新增只读接口：

```http
POST /api/data-steward/storage-objectification-plans:dry-run
```

能力：

- 支持 `projectIds`、`realProjectsOnly`、`storageState`、`checksumState`、`extensions` 等筛选。
- 支持 `maxTotalBytes`、`maxFilesPerProject`、`maxBytesPerProject`。
- 回显 `concurrencyLimit`、`rateLimitBytesPerMinute`，为后续任务中心调度策略预留。
- 按项目分组返回计划结果、风险说明和样本文件。
- 固定 `dryRun=true`、`migrationStarted=false`、`taskSource=MULTI_PROJECT_DRY_RUN`。

## 5. 前端改动

文件服务 / 对象存储页面新增：

- 全项目盘点表增加“分类”“待对象化容量”“路径风险”。
- 新增“多项目对象化规划”区块。
- 支持输入项目 ID，或默认按当前可访问项目生成计划。
- 支持“仅真实项目”开关。
- 支持总量上限、单项目文件数、单项目容量、并发预留字段。
- 结果展示规划项目数、选中文件数、预估容量、预估批次和项目级风险。

页面文案明确：本区块只生成 dry-run 计划，不创建迁移任务、不复制文件。

## 6. 新增专项脚本

新增：

`scripts/dev/check-m3g3-multi-project-objectification-planning.sh`

脚本覆盖：

1. 管理员登录。
2. `storage-provider-readiness` 识别 `NAS_SIDE_MINIO` 且不泄露底层配置。
3. 全项目对象化盘点包含真实/测试分类、容量、风险和分布字段。
4. 自动挑选真实项目做多项目 dry-run。
5. dry-run 返回分项目计划并遵守总量/单项目限额。
6. dry-run 回显并发、限速、总量和单项目限制策略字段。
7. dry-run 未创建迁移任务。
8. `realProjectsOnly=true` 只返回真实项目计划。
9. forbidden-field scan 通过。
10. 脚本已纳入 Git 跟踪。

## 7. 安全边界

已确认：

- 未执行真实历史文件迁移。
- 未运行 M3G-2 灰度执行脚本。
- 未创建对象化迁移任务。
- 未移动、删除、重命名、覆盖真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未改 Hermes、BIM、parser、indexing。
- 响应未暴露 raw NAS path、`storage_path`、`storage_uri`、bucket、object key、SQL、token / secret / password。

## 8. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M3G-3 多项目对象化规划专项                                   PASS=11 FAIL=0
M3G-1 readiness / inventory / dry-run 回归                    PASS=9 FAIL=0
M3E 预览与转换产物对象化回归                                  PASS=8 FAIL=0
M3F 新文件对象存储优先写入回归                                PASS=11 FAIL=0
M3C 对象存储迁移任务中心回归                                  PASS=9 FAIL=0
Phase2 batch4 文件访问安全回归                                PASS=18 FAIL=0
git diff --check                                             PASS
```

已执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3g-nas-minio-readiness-inventory.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-m3f-object-storage-first-write.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 9. 服务状态

- 后端保持运行：`http://127.0.0.1:8080`
- 前端 dev server 保持运行：`http://127.0.0.1:5173`

按用户要求，本轮完成后不关闭项目服务。

## 10. 未完成事项和风险

- M3G-3 只做规划和任务中心增强，不包含从多项目计划直接执行对象化迁移。
- 并发、限速、暂停/继续当前为策略字段和前端展示预留，未接入后台 worker 调度。
- 后续如进入真实多项目迁移，仍需单独确认项目范围、批量上限、执行窗口、失败回滚和运维观察策略。

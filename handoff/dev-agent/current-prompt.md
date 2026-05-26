# 开发 Agent 当前任务：M3D 真实 NAS 小范围灰度镜像

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前分支：

`codex/m3d-real-nas-object-mirror-gray`

本轮批次：

`M3D：真实 NAS 小范围灰度镜像`

本轮不是全量 NAS 搬迁，也不是语义解析或 Hermes 正文问答。目标是在 M3C 任务中心已经收口的基础上，用 105 项目少量真实业务文件做一次受控对象存储镜像灰度，验证真实文件链路可用、可追踪、可回滚、不可越界。

## 0. 必须先阅读

- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c-storage-migration-task-center-closure.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`
- `handoff/main-agent/m3b-object-storage-mirror-trial-closure.md`
- `handoff/main-agent/m3a-storage-service-foundation-closure.md`
- `handoff/main-agent/status.md`
- `handoff/dev-agent/latest-report.md`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageMigrationController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/storage/StorageService.java`
- `frontend/src/modules/data-steward/pages/DataStewardFileServicePage.vue`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`

## 1. 本轮目标

在 105 项目，也就是当前库中的 `projectId=503`，选择少量真实业务文件执行对象存储镜像灰度。

最低覆盖：

- PDF 小样本。
- DWG 小样本。
- RVT / BIM 模型类小样本，如果当前 105 项目存在且单文件大小不超过灰度上限。

验收链路：

```text
真实 NAS 文件台账
-> 显式选择少量 fileIds / assetUuid
-> M3C 迁移任务中心创建任务
-> 读取 NAS 源文件
-> checksum
-> 上传 MinIO
-> 校验
-> 写对象版本
-> storage-status 显示 OBJECT_STORED
-> file-access 继续受控读取
-> NAS 原文件保持不变
-> 灰度报告可查
```

## 2. 优先实现策略

优先复用 M3C 已有能力，不要为了 M3D 大改后端。

如果现有 M3C 任务中心已经足以完成真实文件灰度，则本轮主要新增：

- M3D 专项脚本。
- 灰度报告或 handoff 记录。
- 必要的前端提示微调。

只有在发现真实 NAS 文件灰度无法被现有 M3C 安全验收时，才做最小代码补强。

## 3. 灰度选择规则

必须使用显式选择，不允许项目全量、目录全量、后缀全量自动迁移。

建议脚本逻辑：

1. 登录管理员。
2. 切换到 `projectId=503`。
3. 查询 105 项目中真实文件候选。
4. 从 PDF / DWG / RVT 或模型类文件中各挑少量候选。
5. 跳过已删除、回收站、路径不可读、超过单文件大小上限、不属于 105 项目的文件。
6. 创建迁移任务。
7. 查询任务详情。
8. 查询 `storage-status`。
9. 用受控 `file-access` 做 PREVIEW 或 DOWNLOAD 验证。
10. 输出灰度摘要。

如果找不到符合大小上限的 RVT / BIM 文件，脚本不能失败成 P0，应记录为 `SKIPPED_NO_ELIGIBLE_MODEL_SAMPLE`，并继续验证 PDF / DWG。

## 4. 必做能力

### A. M3D 专项脚本

新增：

`scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`

脚本必须覆盖：

- 105 / 503 真实项目登录与权限。
- 真实业务文件候选选择。
- PDF / DWG / RVT 或模型类覆盖情况。
- 显式 fileIds 创建迁移任务。
- 迁移后 `OBJECT_STORED` 状态。
- 受控 file-access 可访问。
- 重跑幂等。
- NAS 原文件未被移动、删除、改名。
- 灰度报告字段：
  - 成功数
  - 失败数
  - 跳过数
  - 文件类型覆盖
  - checksum 覆盖率
  - 禁出字段扫描结果

### B. 灰度报告

完成后在 `handoff/dev-agent/latest-report.md` 写清：

- 选择了哪些类型的样本。
- 是否覆盖 PDF / DWG / RVT。
- 如果某类未覆盖，原因是什么。
- 成功 / 失败 / 跳过数量。
- 是否触碰真实 NAS 文件。
- 是否发生真实 NAS 写操作。
- 是否出现 raw path / bucket / object key 泄露。

### C. UI / API 最小补强

如现有页面已经足够，本轮可以不改 UI。

如需要微调，仅允许：

- 增加“真实 NAS 小范围灰度”提示。
- 增加灰度执行前的风险说明。
- 增加按文件类型 / storageState 筛选，帮助选择 PDF / DWG / 模型小样本。

不要新增大页面，不要重做文件服务页面。

## 5. 明确禁止

严禁：

1. 不做全量 NAS 迁移。
2. 不做目录一键全量迁移。
3. 不自动扫描并迁移整个项目。
4. 不移动、删除、重命名真实 NAS 文件。
5. 不读取 PDF / Office / DWG / RVT / IFC 正文。
6. 不写 documents / chunks / OpenSearch / Qdrant / Hermes memory。
7. 不新增 Hermes 正文问答。
8. 不启动 BIM parser 或真实轻量化。
9. 不开放 bucket、object key、`storage_uri`、真实 NAS 路径。
10. 不修改仓库 `docs/**`。
11. 不把对象存储镜像说成语义理解完成。

## 6. 必跑验证

完成后至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh
bash scripts/dev/check-m3c-storage-migration-task-center.sh
bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 7. 报告要求

写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 改动文件清单。
- 是否复用 M3C，是否新增代码。
- 灰度样本选择规则。
- PDF / DWG / RVT 覆盖情况。
- 成功 / 失败 / 跳过数量。
- checksum 覆盖率。
- `OBJECT_STORED` 验证结果。
- file-access 回归结果。
- 禁出字段扫描结果。
- 是否触碰真实 NAS 文件。
- 是否修改 `docs/**`。
- 已知风险和 M3E 建议。

## 8. 完成定义

同时满足才算完成：

- M3D 专项脚本通过。
- 105 真实业务小样本完成对象存储镜像。
- NAS 原文件未被移动、删除、改名。
- 已镜像文件显示 `OBJECT_STORED`。
- file-access 受控访问不回归。
- 失败 / 跳过原因可解释。
- 禁出字段扫描通过。
- M3C / M3C-1 / M3B / M3A 回归通过。
- `handoff/dev-agent/latest-report.md` 已写。

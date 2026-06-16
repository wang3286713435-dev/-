# M3G-1 收口记录：NAS 侧 MinIO readiness、对象化盘点与 dry-run

收口时间：2026-05-27

## 结论

`M3G-1：NAS 侧 MinIO 就绪检查、全项目对象化盘点与 dry-run 计划` 正式收口。

当前无 P0 / P1。

## 已完成能力

- 平台 readiness 可识别：
  - `LOCAL_DEV_MINIO`
  - `NAS_SIDE_MINIO`
  - `UNKNOWN`
- 当前 NAS 侧 MinIO 已验证为：
  - `endpointType=NAS_SIDE_MINIO`
  - `readinessStatus=READY`
- 全项目对象化覆盖率盘点可查。
- 单项目对象化覆盖率盘点可查，已补齐 `projectCode / projectName`。
- 单项目对象化 dry-run 计划可生成。
- dry-run 保持只读：
  - `dryRun=true`
  - `migrationStarted=false`
  - 不创建真实迁移任务
  - 不复制文件
  - 不修改 NAS
- 文件服务页已展示 readiness、覆盖率、项目盘点和 dry-run 结果。

## P1 修复记录

上一轮验收发现 2 个 P1，均已关闭：

1. M3E 回归失败：NAS 侧 MinIO 下旧对象 metadata 指向旧对象存储，file-access 读取失败。
   - 修复：在原生预览产物准备阶段，对当前 provider 下不可读的对象执行受控对象修复。
   - 保持：file-access 不做静默 NAS fallback。

2. M3F 脚本失败：脚本仍假设对象存储是本机 Docker MinIO。
   - 修复：脚本先读取 readiness。
   - `LOCAL_DEV_MINIO` 下保留本机容器不可用模拟。
   - `NAS_SIDE_MINIO` 下不再暂停本机 MinIO，改为验证 NAS readiness 与对象优先上传链路。

## 验收结果

测试 agent 已完成复验，报告：

- `handoff/test-agent/latest-report.md`

通过项：

- 后端构建通过。
- 前端构建通过。
- 健康检查通过。
- `scripts/dev/check-m3g-nas-minio-readiness-inventory.sh`：`PASS=9 FAIL=0`
- `scripts/dev/check-m3e-preview-artifacts-object-storage.sh`：`PASS=8 FAIL=0`
- `scripts/dev/check-m3f-object-storage-first-write.sh`：`PASS=11 FAIL=0`
- `scripts/dev/check-m3c-storage-migration-task-center.sh`：`PASS=9 FAIL=0`
- `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=18 FAIL=0`
- `git diff --check` 通过。

## 明确未做

- 未执行历史文件真实批量迁移。
- 未复制、移动、删除、重命名或覆盖真实 NAS 原项目文件。
- 未做 Hermes 正文问答。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未接入真实 BIM 引擎。

## 当前边界

M3G-1 只证明：

1. NAS 侧 MinIO 服务可被平台识别并读写探测。
2. 当前项目资产对象化覆盖率可盘点。
3. 后续对象化迁移可先 dry-run 估算范围和风险。

M3G-1 不代表：

- 历史项目文件已经全面对象化。
- 平台已经完成全量 NAS 到 MinIO 切换。
- Hermes 已具备正文理解能力。

## 下一步建议

进入 `M3G-2：历史文件对象化执行与读取链路切换灰度` 前，必须单独确认：

- 试点项目范围。
- 文件类型范围。
- 单批文件数量 / 容量上限。
- 失败回滚策略。
- NAS 原文件冻结边界。
- 对象化后读取优先级与 fallback 审计策略。

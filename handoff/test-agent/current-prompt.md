# 测试 Agent 当前任务：M3D 真实 NAS 小范围灰度镜像验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮验收批次：

`M3D：真实 NAS 小范围灰度镜像`

本轮只验收 105 项目少量真实业务文件的对象存储镜像灰度，不验收全量 NAS 搬迁、语义解析、Hermes 正文问答、BIM parser 或真实轻量化。

## 0. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c-storage-migration-task-center-closure.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`

## 1. 必跑命令

请执行并记录：

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

## 2. M3D 专项验收重点

### A. 灰度范围

- 项目必须是 105 / `projectId=503`。
- 必须使用显式 `fileIds` / `assetUuid`。
- 不得项目全量迁移。
- 不得目录全量迁移。
- 不得自动迁移整个后缀类型。

### B. 样本覆盖

检查灰度报告是否说明：

- PDF 是否覆盖。
- DWG 是否覆盖。
- RVT / BIM 模型类是否覆盖。
- 若某类未覆盖，是否给出合理原因，例如无符合大小上限样本。

### C. 对象存储状态

- 成功样本 `storage-status` 应显示 `OBJECT_STORED`。
- `activeProvider` 可显示业务化 `MINIO`。
- 不得返回 bucket、object key、底层 URI。

### D. file-access

- 已镜像文件仍通过平台受控 `file-access` 访问。
- 权限、票据、审计、路径脱敏不回归。

### E. NAS 原文件保护

必须确认：

- 未移动真实 NAS 文件。
- 未删除真实 NAS 文件。
- 未重命名真实 NAS 文件。
- 未写入真实 NAS 文件内容。

### F. 灰度报告

必须有：

- 成功数。
- 失败数。
- 跳过数。
- 文件类型覆盖。
- checksum 覆盖率。
- 禁出字段扫描结果。

## 3. 禁出字段扫描

抽查 M3D 脚本和接口响应，不得包含：

- `/Volumes`
- `/Users`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- bucket 实际值
- object key 实际值
- raw row
- SQL
- token
- secret
- password

说明文案中出现“不会展示 bucket / object key”这类字面词可作为 P2 记录，不等同真实泄露；但如果出现真实 bucket 名称或 object key 值，必须判 P0 / P1。

## 4. P0 / P1 判定

### P0

- 真实 NAS 文件被移动、删除、改名或写入。
- 发生项目全量 / 目录全量 / 后缀全量自动迁移。
- API 或前端泄露真实 NAS 路径、bucket、object key、`storage_uri`、token、secret。
- file-access 权限链路被破坏。

### P1

- M3D 专项脚本未通过。
- 未覆盖任何真实业务文件。
- 未解释 PDF / DWG / RVT 覆盖情况。
- 已迁移文件未显示 `OBJECT_STORED`。
- 失败 / 跳过原因不可解释。
- M3D 专项脚本未纳入 Git 跟踪。
- M3C / M3B / M3A 核心回归失败。

### P2

- Vite chunk warning。
- 非交付未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**`。
- 页面安全说明中出现“bucket / object key”字面词但没有真实值。

## 5. 报告要求

写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2 列表。
- 必跑命令结果。
- M3D 专项脚本结果。
- 样本覆盖情况。
- 成功 / 失败 / 跳过数量。
- checksum 覆盖率。
- `OBJECT_STORED` 验证结果。
- file-access 验证结果。
- NAS 原文件保护验证。
- 禁出字段扫描结果。
- 是否建议收口 M3D。

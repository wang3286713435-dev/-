# 测试 Agent 当前任务：M3B 105 小样本对象存储镜像迁移验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前测试批次：

`M3B：105 小样本对象存储镜像迁移`

本轮只验收小样本对象存储镜像迁移，不验收全量 NAS 搬迁、语义解析、Hermes 正文问答或真实 BIM 引擎。

## 0. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/m3b-object-storage-mirror-trial-plan.md`
- `handoff/main-agent/m3a-storage-service-foundation-closure.md`
- `handoff/main-agent/status.md`

## 1. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3b-object-storage-mirror-trial.sh
bash scripts/dev/check-m3a-storage-service-foundation.sh
bash scripts/dev/check-m2j-105-ownership-review.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2f-real-project-delivery-loop.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 2. 专项验收

必须验证：

1. 小样本迁移任务可创建。
2. 创建任务必须显式传入少量 `fileIds`。
3. 任务数量上限生效，不能一键全量迁移。
4. 文件必须属于当前项目。
5. 已删除、回收站、路径不可读文件不能迁移。
6. 成功后 `storage-status` 显示 `OBJECT_STORED` 或等价对象已存储状态。
7. `data_storage_objects` 与 `data_file_object_versions` 有可解释记录。
8. 重跑同一文件不会重复污染对象记录。
9. 失败场景有失败原因。
10. 迁移后 file-access 仍通过受控 ticket 访问。
11. NAS 原文件未被移动、删除、重命名。

## 3. 禁出字段扫描

对 M3B 新接口、任务详情、storage-status、file-access 响应扫描，禁止出现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storageUri`
- `storage_path`
- `storagePath`
- `bucket`
- `object_key`
- `objectKey`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`
- `access_key`
- `secret_key`

服务端内部日志或数据库字段不是本轮禁出对象；API 响应泄露才按 P0/P1。

## 4. 越界检查

执行：

```bash
git diff --name-only
git status --short
```

重点确认没有：

- 全量 NAS 迁移。
- 真实 NAS 文件移动、删除、重命名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- documents / chunks / OpenSearch / Qdrant / Hermes memory 写入。
- Hermes 正文问答能力。
- 真实 BIM 引擎接入。
- `docs/**` 未授权修改。

`.claude/**`、`CLAUDE.md`、`tmp/**` 如仍为非交付未跟踪项，不直接判失败，但报告中提醒收口时排除。

## 5. 判定标准

P0：

- API 响应泄露真实 NAS 路径、bucket/object key、token、secret、raw row 或 SQL。
- `file-access` 权限绕过。
- 迁移过程移动、删除、重命名真实 NAS 文件。
- 出现全量 NAS 迁移入口。
- 新增 Hermes 正文问答、parser/indexing、真实 BIM 解析。

P1：

- M3B 专项脚本失败。
- 后端构建、前端构建、健康检查、核心回归失败。
- 小样本迁移任务不可创建。
- storage-status 不能表达对象已存储。
- 重跑迁移重复污染记录。
- 迁移失败无失败原因。
- M3A provider health / file-access 受控读取回归。

P2：

- 既有 Vite chunk warning。
- 非阻塞文案或状态命名问题。
- 非交付未跟踪文件提醒。

## 6. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 总结论。
- P0 / P1 / P2。
- 必跑命令结果。
- M3B 专项脚本结果。
- 小样本迁移任务验证结果。
- storage-status 结果。
- 幂等验证结果。
- file-access 回归结果。
- NAS 原文件未被改动的确认。
- 禁出字段扫描结果。
- 是否发现越界。
- 是否建议主 agent 收口 M3B。

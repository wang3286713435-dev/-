# M3D 真实 NAS 小范围灰度镜像验收报告

生成时间：2026-05-26

## 1. 测试结论

结论：M3D 验收通过，建议主 agent 进入 M3D 收口判断。

本轮按 `handoff/test-agent/current-prompt.md` 执行 `M3D：真实 NAS 小范围灰度镜像` 验收。后端构建、前端构建、健康检查、M3D 专项脚本、M3C / M3C-1 / M3B / M3A / M2J / M2I / M2H / file-access 回归和 `git diff --check` 均通过。

未发现 P0 / P1。

## 2. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2 / 记录项：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮 M3D 验收。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项仍存在，未纳入本轮判断。
- `handoff/main-agent/m3d-real-nas-object-mirror-gray-plan.md` 当前仍为未跟踪交接文档；它不是运行期关键文件，但建议主 agent 确认是否纳入 checkpoint。

## 3. 必读文件检查

已阅读：

- `handoff/dev-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3-storage-evidence-chain-todo.md`
- `handoff/main-agent/m3c-storage-migration-task-center-closure.md`
- `handoff/main-agent/m3c1-asset-uuid-storage-status-closure.md`

## 4. 必跑命令结果

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `corepack pnpm --dir frontend build`：通过，仅有既有 chunk size warning。
- `curl -fsS http://127.0.0.1:8080/actuator/health`：通过，返回 `{"status":"UP"}`。
- `bash scripts/dev/check-m3d-real-nas-object-mirror-gray.sh`：通过，`PASS=19 FAIL=0`。
- `bash scripts/dev/check-m3c-storage-migration-task-center.sh`：通过，`PASS=9 FAIL=0`。
- `bash scripts/dev/check-m3c1-asset-uuid-storage-status.sh`：通过，`PASS=15 FAIL=0`。
- `bash scripts/dev/check-m3b-object-storage-mirror-trial.sh`：通过，`PASS=11 FAIL=0`。
- `bash scripts/dev/check-m3a-storage-service-foundation.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m2j-105-ownership-review.sh`：通过，`PASS=6 FAIL=0`。
- `bash scripts/dev/check-m2i-105-file-ownership-governance.sh`：通过，`PASS=8 FAIL=0`。
- `bash scripts/dev/check-m2h-windows-file-manager.sh`：通过，`PASS=53 FAIL=0`。
- `bash scripts/dev/check-phase2-batch4-file-access.sh`：通过，`PASS=18 FAIL=0`。
- `git diff --check`：通过。

## 5. M3D 专项脚本结果

`scripts/dev/check-m3d-real-nas-object-mirror-gray.sh` 通过，`PASS=19 FAIL=0`。

本轮执行摘要：

- `grayTaskId=70`
- `rerunTaskId=71`
- `success=0`
- `skipped=3`
- `failure=0`
- `coverage=DWG,MODEL,PDF`
- `checksum=3/3`
- `forbiddenScan=PASS`

说明：三份真实业务样本已在开发自测 / 前序灰度中完成对象化，本轮测试再次执行时按幂等策略返回已对象化 / 跳过，未重复污染 active 对象版本。脚本随后逐个验证三份样本仍为 `OBJECT_STORED`，并通过受控 file-access 读取对象镜像。

## 6. 样本覆盖情况

项目：105 / `projectId=503`。

本轮显式选择真实业务文件，不做项目全量、目录全量或后缀全量自动迁移。

样本覆盖：

- PDF：`fileId=993`，`assetUuid=72f19096-58b2-11f1-aae9-1e851063bccc`，大小 `1452359`，当前状态 `OBJECT_STORED`。
- DWG：`fileId=935`，`assetUuid=72f0e8c1-58b2-11f1-aae9-1e851063bccc`，大小 `291104`，当前状态 `OBJECT_STORED`。
- MODEL / RVT：`fileId=1257`，`assetUuid=72f4838a-58b2-11f1-aae9-1e851063bccc`，大小 `10014720`，当前状态 `OBJECT_STORED`。

PDF / DWG / RVT 模型类均已覆盖。

## 7. 成功 / 失败 / 跳过数量

本轮测试任务：

- 成功数：0。
- 失败数：0。
- 跳过数：3。

跳过原因：三份样本已经对象化，本轮重复执行被 M3C 幂等策略安全跳过。该结果符合灰度复跑预期，不视为失败。

开发报告中记录的首次真实灰度执行结果：

- 成功数：3。
- 失败数：0。
- 跳过数：0。

## 8. checksum 覆盖率

通过。

- checksum 覆盖率：`3/3`。
- 三份样本均具备 checksum。

## 9. OBJECT_STORED 验证结果

通过。

逐项验证：

- `fileId=993`：`storage-status` 为 `OBJECT_STORED`。
- `fileId=935`：`storage-status` 为 `OBJECT_STORED`。
- `fileId=1257`：`storage-status` 为 `OBJECT_STORED`。

任务详情行级结果均为 `OBJECT_STORED`，原因可解释。

## 10. file-access 验证结果

通过。

- `fileId=993`：受控 file-access 可读取对象镜像。
- `fileId=935`：受控 file-access 可读取对象镜像。
- `fileId=1257`：受控 file-access 可读取对象镜像。

Phase2 batch4 文件访问安全回归也通过，说明预览 / 下载票据、权限、审计和路径脱敏链路未回归。

## 11. NAS 原文件保护验证

通过。

脚本在迁移前后对三份真实 NAS 源文件做只读校验：

- 原文件仍存在。
- 原文件仍可读。
- size 未变化。
- mtime 未变化。

未发现真实 NAS 文件被移动、删除、改名、覆盖或写入。

## 12. 禁出字段扫描结果

通过。

M3D 专项脚本对迁移创建、任务详情、storage-status、access ticket、幂等复跑响应执行 forbidden-field scan，未发现：

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

说明：M3D 脚本内部为了选择真实样本，会读取数据库中的存储引用并在本机做只读存在性校验；脚本不输出真实路径，也不通过 API 或前端暴露真实 NAS 路径。

## 13. Git 跟踪状态

通过。

- `scripts/dev/check-m3d-real-nas-object-mirror-gray.sh` 当前状态为 `A`，已纳入 Git 跟踪。
- `git ls-files --error-unmatch scripts/dev/check-m3d-real-nas-object-mirror-gray.sh` 可识别该脚本。
- `docs/**` 无修改。

## 14. 回归边界

已确认：

- M3C 对象存储迁移任务中心回归通过。
- M3C-1 assetUuid / storage-status 回归通过。
- M3B 小样本对象存储镜像回归通过。
- M3A StorageService 回归通过。
- M2J / M2I / M2H 文件治理与文件管理器回归通过。
- file-access 权限、票据、路径脱敏回归通过。
- 未发现项目全量 / 目录全量 / 后缀全量自动迁移。
- 未新增 Hermes 正文问答。
- 未写 documents / chunks / Qdrant / OpenSearch / Hermes memory。
- 未启动 BIM parser 或真实轻量化。
- 未修改 `docs/**`。

## 15. 是否建议收口 M3D

建议主 agent 进入 M3D 收口判断。

M3D 真实 NAS 小范围灰度镜像已覆盖 PDF / DWG / RVT 模型类样本；对象存储状态、file-access、checksum、幂等、NAS 原文件保护、禁出字段扫描和核心回归均通过。当前仅有 P2 / 记录项，不阻塞收口。

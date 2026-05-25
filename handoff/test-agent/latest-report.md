# 测试 Agent 报告：M2J 105 工程树人工复核与批量调整体验验收

时间：2026-05-25 CST

## 1. 验收结论

结论：通过。

本轮只验收 `M2J：105 工程树人工复核与批量调整体验`，未做全量浏览器逐页点击，未修改业务代码，未修改 `docs/**`，未触碰真实 NAS 文件。

已确认：

- 工程树节点可查看对应文件。
- 节点文件支持多选。
- 批量确认归属可用。
- 批量驳回归属可用。
- 批量修改归属类型可用。
- 批量移动到其他工程节点可用，并可恢复原归属。
- 文件管理器中“归属节点”具备跳转到工程树对应节点的入口。
- 批量写操作必须 `confirmed=true`。
- 批量复核动作写入审计。
- 响应未发现 raw NAS 路径泄露。
- 不污染正式文档 / 图纸交付应交项。

当前无 P0 / P1，建议主 agent 进入 M2J 收口判断。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：

- `scripts/dev/check-m2j-105-ownership-review.sh` 当前仍是未跟踪文件。该问题不影响本轮功能验收，但建议收口提交时纳入 Git，避免 M2J 回归脚本丢失。
- 前端构建仍有既有 Vite chunk size warning，不影响本轮验收。

## 3. 必跑命令结果

```text
cd backend && ./mvnw -pl delivery-app -am -DskipTests package
结果：PASS，BUILD SUCCESS

corepack pnpm --dir frontend build
结果：PASS，仅既有 Vite chunk size warning

curl -fsS http://127.0.0.1:8080/actuator/health
结果：PASS，{"status":"UP"}

bash scripts/dev/check-m2j-105-ownership-review.sh
结果：PASS=6 FAIL=0

bash scripts/dev/check-m2i-105-file-ownership-governance.sh
结果：PASS=8 FAIL=0

bash scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh
结果：PASS=11 FAIL=0

bash scripts/dev/check-m2h-windows-file-manager.sh
结果：PASS=53 FAIL=0

bash scripts/dev/check-m2f-real-project-delivery-loop.sh
结果：PASS=6 FAIL=0

bash scripts/dev/check-phase2-batch4-file-access.sh
结果：PASS=18 FAIL=0，phase2 batch4 file access check ok

git diff --check
结果：PASS
```

## 4. M2J 专项脚本覆盖

`check-m2j-105-ownership-review.sh` 已验证：

- 管理员登录并切换到 `105 / projectId=503`。
- 工程树文件列表支持状态筛选、待复核筛选，并通过 forbidden-field scan。
- 未带 `confirmed=true` 的批量复核写入被拒绝。
- 批量改类型、移动节点、驳回和恢复接口可用。
- 写操作响应 `requestedCount=1`、`updatedCount=1`、`failedCount=0`。
- 复核动作可恢复原始归属，并写入 `data.file-ownership.review-batch` 审计。

## 5. 补充跨节点移动复核

官方脚本的 `MOVE_NODE` 主要验证同节点移动参数链路。为覆盖“批量移动到其他工程节点”，本轮额外做了 1 条受控 API 复核：

```text
fileId=1244
从：启航华居项目/BIM/模型资料/建筑专业/地下室
移到：启航华居项目
再恢复：启航华居项目/BIM/模型资料/建筑专业/地下室
结果：PASS，moveUpdatedCount=1，restoreUpdatedCount=1
```

该复核只更新平台归属元数据，未移动、删除、重命名或读取 NAS 文件。复核后已恢复原归属节点和原归属类型。

## 6. 前端静态复核

已检查：

- `frontend/src/modules/data-steward/components/FileOwnershipTreePanel.vue`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`

确认点：

- `FileOwnershipTreePanel.vue` 当前节点文件表包含 `type="selection"` 多选列。
- 工具栏存在 `批量确认`、`批量驳回`、`批量改归属类型`、`批量移到其他节点`。
- 批量确认 / 驳回 / 改类型 / 移动节点均有人工确认弹窗，文案明确“不移动、不删除、不重命名、不读取 NAS 文件”。
- 前端调用 `reviewFileOwnershipAssignments()` 时传入 `confirmed: true`。
- `AssetProjectFileBrowser.vue` 中已登记文件的“归属节点”可点击，并通过 `open-ownership-node` 事件跳转。
- `AssetProjectDetailPage.vue` 支持 `tab=ownership&ownershipNode=...`，可联动工程树聚焦节点。
- 未登记文件仍显示“需扫描入库后治理”，不伪造归属入口。

## 7. 回归结果

M2I 回归：

- 105 文件归属覆盖率、工程树、未归属列表、推荐、人工确认、节点文件查询仍通过。
- 2928 个归属记录未污染正式交付包应交项。

M2H-F1 / M2H 回归：

- 105 根目录 direct-only、未登记文件、目录树交互和文件管理器静态能力未回归。
- 文件管理器未发现 raw path 硬编码输出。

M2F 回归：

- 105 正式工程主数据和交付标准可查。
- 文档 / 图纸交付视图仍基于正式规则返回缺失项。
- 审核、整改、交付包草案 dry-run 链路不回归。

Phase2 batch4 回归：

- 文件预览 / 下载 ticket、权限隔离、catalog 路径脱敏、OpenAPI 和审计均通过。

## 8. 安全边界

未发现：

- 真实 NAS 文件上传、移动、重命名、删除。
- 文件正文读取。
- PDF / Office / DWG / RVT / IFC 内容解析。
- parser / writer / indexing / 向量库能力。
- Hermes 写库、自动挂接、自动治理。
- raw NAS path、`/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、SQL、raw row、token、secret、password 泄露。
- `docs/**` 改动。

## 9. Git 状态提醒

当前状态：

```text
?? scripts/dev/check-m2j-105-ownership-review.sh
```

建议收口提交时纳入该脚本；本轮不因该 P2 阻断 M2J 功能通过。

## 10. 最终建议

M2J 验收通过，当前无 P0 / P1。建议主 agent 进入 M2J 收口判断，并在提交前把 `scripts/dev/check-m2j-105-ownership-review.sh` 纳入 Git。

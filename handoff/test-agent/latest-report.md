# M2F 真实项目交付闭环试运行验收报告

生成时间：2026-05-23 00:06 CST

## 1. 测试结论

结论：通过。

M2F 已达到本轮验收目标：`105 / 503` 已确认的正式工程主数据能驱动文档 / 图纸交付视图，缺失项解释能说明目标部位、交付定义、交付类型和需要补交的文件类型；推荐挂接仍需要人工确认；审核 / 整改查询和交付包草案 dry-run 链路未回归。

当前未发现 P0 / P1。仅有既有 Vite chunk size warning 作为 P2，不阻塞收口判断。

建议主 agent 收口 M2F。

## 2. 必读文件

已阅读：

- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`

本轮按轻量策略执行，不做大范围浏览器逐页点击，不做多分辨率视觉巡检。

## 3. 必跑命令结果

- M2F 专项脚本存在：`scripts/dev/check-m2f-real-project-delivery-loop.sh`。
- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2F 专项脚本：`bash scripts/dev/check-m2f-real-project-delivery-loop.sh`，通过，`PASS=6 FAIL=0`。
- M2E 回归：`bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh`，通过，`PASS=11 FAIL=0`。
- M2C 回归：`bash scripts/dev/check-m2c-delivery-package-archive.sh`，通过，`PASS=11 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 4. M2F 专项脚本结果

专项脚本通过并覆盖：

- 管理员登录并切换到 `503 / 105`。
- 105 正式工程主数据和交付标准可查，且节点类型已锁定。
- 文档 / 图纸交付视图能基于正式规则返回业务可读缺失项。
- 缺失项解释、候选推荐和人工确认 / 合法参数防线可用。
- 审核 / 整改查询与交付包草案 dry-run 链路不回归。

## 5. 105 真实项目交付闭环状态

核心接口抽查结果：

- 标准状态：`deliverableStandardReady=true`。
- 部位节点：`11`。
- 节点类型：`3`，全部锁定，包含 `PROJECT`、`DISCIPLINE`、`DELIVERY_ITEM`。
- 交付定义：`3`，包含 `DRAWING_DELIVERY`、`DOCUMENT_DELIVERY`、`MODEL_DELIVERY`。
- 交付类型：`5`，覆盖 `DOCUMENT`、`DRAWING`、`MODEL`。

文档交付：

- `standardReady=true`。
- `totalRequired=22`。
- `missingCount=22`。
- `nextActionCode=BIND_MISSING_FILES`。
- 示例缺失原因：`工程部位“建筑专业”缺少“文档交付 / PDF 文档”，需要人工选择当前项目资产目录中的文档文件完成补交。`

图纸交付：

- `standardReady=true`。
- `totalRequired=22`。
- `missingCount=22`。
- `nextActionCode=BIND_MISSING_FILES`。
- 示例缺失原因：`工程部位“建筑专业”缺少“图纸交付 / DWG 图纸”，需要人工选择当前项目资产目录中的图纸文件完成补交。`

缺失项解释：

- `totalCount=44`。
- 示例解释：`建筑专业还缺少“文档交付 / PDF 文档”。平台会推荐当前项目内登记为文档的候选文件，用户确认后才会挂接。`

人工挂接防线：

- 未确认推荐挂接返回 `WORK_AGENT_GOVERNANCE_CONFIRM_REQUIRED`。
- 错误批量挂接参数被拒绝，未出现自动挂接。

交付包草案 / 档案目录：

- `dryRun=true`。
- `physicalPackageGenerated=false`。
- `nasFileCopied=false`。
- `totalCount=44`。
- `blockedCount=44`。
- `missingCount=44`。

结论：105 当前仍是“全部待人工补交”的真实试运行状态，但正式工程主数据已经能驱动应交项、缺失项、推荐候选、整改查询和交付包草案链路。

## 6. forbidden field 与红线检查

接口响应和页面相关文本抽查未发现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `storagePath`
- `storageUri`
- raw row
- SQL
- token
- secret
- password

静态扫描 M2F 相关后端代码和专项脚本，命中项仅出现在：

- 专项脚本 forbidden-field 断言列表。
- 登录测试参数。
- `DeliveryApplicationService` 的脱敏 / forbidden 判断逻辑。
- 既有数据库字段名 `delete_token`。

未发现：

- 真实 NAS 文件写入、移动、删除、复制、改名。
- PDF / Office / DWG / RVT / IFC 正文读取。
- Hermes / BIM / parser / writer / indexing / 向量库新能力。
- Agent 自动挂接、自动审核、自动整改。
- 真实交付包生成或 NAS 文件复制。

## 7. 浏览器短验

未做浏览器短验。

原因：本轮开发报告显示改动集中在后端 `DeliveryApplicationService`、`AgentGovernanceApplicationService` 和专项脚本，没有修改关键 Vue 页面；专项脚本和接口抽查已覆盖本轮目标。按 `lightweight-test-strategy.md`，本轮不做大范围浏览器逐页点击。

## 8. 工作区状态备注

当前工作区有 M2F 预期改动：

- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/AgentGovernanceApplicationService.java`
- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/DeliveryApplicationService.java`
- `scripts/dev/check-m2f-real-project-delivery-loop.sh`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`

仍有既有非交付未跟踪文件：

- `.claude/**`
- `CLAUDE.md`
- `tmp/**`

建议主 agent 收口提交时确认归属并排除非交付文件。

## 9. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 既有 Vite chunk size warning，未阻塞前端构建和本轮验收。
- 工作区仍有 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件，收口提交时需确认归属。

## 10. 是否建议主 agent 收口 M2F

建议主 agent 收口 M2F。

理由：后端构建、前端构建、健康检查、M2F 专项脚本、M2E 回归、M2C 回归和 `git diff --check` 均通过；105 / 503 正式工程主数据能驱动文档 / 图纸应交项、缺失项解释、人工挂接防线、整改查询和交付包草案 dry-run；未发现真实 NAS、正文读取、Hermes / BIM / parser / indexing 或敏感字段泄露越界。

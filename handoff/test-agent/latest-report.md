# G3 Hermes 平台工作型 Agent MVP 验收报告

生成时间：2026-05-20 11:36 CST

## 1. 测试结论

结论：通过。

本轮只验收 `G3：Hermes 平台工作型 Agent MVP`。G3 Action Center、操作草案、待人工确认、执行结果、Missing Evidence 边界和 G2/G1/8A 回归均通过。

当前无 P0 / P1。

是否建议主 agent 收口 G3：建议收口。

是否建议进入 8B / 8C / 9A：不建议自动进入，仍需用户另行确认。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：仅有既有前端 Vite chunk size warning，不影响本轮 G3 收口。

## 3. 必读与范围确认

已阅读：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/phase2-g3-hermes-masterdata-delivery-guidance-plan.md`
- `handoff/main-agent/hermes-layered-integration-decision.md`
- Hermes capability handoff
- platform_to_hermes_contract
- gateway_response_contract
- missing_evidence_policy

范围确认：

- 未进入 8B / 8C / 9A。
- 未测试生产发布。
- 未测试真实 NAS 写操作。
- 未测试正文解析、BIM 构件解析、selective indexing 或 Hermes memory 写入。
- 未触碰真实 NAS 文件。

## 4. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- Hermes Gateway 回归：`EXPECT_HERMES_AGENT_AVAILABLE=true bash scripts/dev/check-hermes-jarvis-gateway.sh`，通过，`PASS=13 FAIL=0`。
- G3 专项：`bash scripts/dev/check-phase2-insert-g3-hermes-working-agent.sh`，通过，`PASS=21 FAIL=0`。
- G2 回归：`bash scripts/dev/check-phase2-insert-g2-real-project-onboarding.sh`，通过，`PASS=11 FAIL=0`。
- G1 回归：`bash scripts/dev/check-phase2-insert-g1-agent-delivery-governance.sh`，通过，`PASS=34 FAIL=0`，本轮测试夹具已清理，`PHASE2-G1` 可见资源计数未增长。
- 8A 回归：`bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh`，通过，`PASS=11 FAIL=0`。
- 格式检查：`git diff --check`，通过。

## 5. Hermes 面板与 Action Center

页面：`/data-steward/assets/503/work/agent-governance`

结果：通过。

浏览器验收记录：

- Hermes 抽屉可打开。
- 可见 `Hermes Action Center`。
- 面板明确说明：通过平台受控接口生成计划，只有人工确认后才会执行挂接。
- 可见四个分区：`回答 / 操作草案 / 待人工确认 / 执行结果`。
- 页面显示 `Hermes真实回答 已接入`、`运行时写入 未开放`、`数据库 / NAS 写操作 不会执行`。
- 页面未白屏、未出现 500、未出现 `timeout of 10000ms exceeded`。

## 6. 操作草案验收

结果：通过。

主数据补齐计划：

- 点击 `操作草案 -> 生成主数据补齐计划` 后可生成计划。
- 计划包含接入状态：`可进入交付治理`。
- 计划包含部位树状态：已建立节点数。
- 计划包含节点类型状态：节点类型数量和是否锁定。
- 计划包含交付标准状态：定义数量和类型数量。
- 计划包含文档 / 图纸准备度。
- 计划包含下一步建议。
- 计划包含页面入口：真实项目接入向导、部位树、节点类型、交付物标准、文档交付、图纸交付。
- 未发现自动创建主数据动作。

缺失交付方案：

- 文档方案可生成；当前样本项目文档缺失项为 0，页面提示没有可推荐候选文件。
- 图纸方案可生成；页面展示缺失项和推荐文件。
- 图纸方案包含目标、推荐文件、版本、推荐原因、置信度、风险提示和“建议先治理元数据”等提示。
- 方案仍声明只依据当前项目元数据，不读取文件正文。

## 7. 人工确认与执行边界

结果：通过。

浏览器验收记录：

- 进入 `待人工确认` 后可见提示：不会自动挂接，必须勾选推荐、勾选人工确认，再由平台后端二次校验后执行。
- 初始状态 `确认执行` 按钮禁用。
- 推荐项展示置信度和风险提示。
- 未在浏览器中对真实 503 项目点击执行，避免改变真实项目交付数据。

脚本验收记录：

- G3 专项脚本验证未人工确认时拒绝执行挂接。
- G3 专项脚本验证人工确认后才调用平台挂接并返回执行结果。
- G3 专项脚本验证审计日志包含 G3 复用的 apply 记录。
- G1 回归脚本验证 recommend / apply 审计仍存在。

## 8. Hermes 边界与 Missing Evidence

结果：通过。

浏览器提问：

`这个 RVT 里面有哪些构件参数、DWG 图层和模型内容？`

实测结果：

- 返回 Missing Evidence / 缺少证据。
- 明确缺少 RVT/Revit 解析证据。
- 明确缺少 DWG/图纸解析证据。
- 明确缺少模型解析证据。
- 明确缺少构件级证据。
- 未编造模型内容、DWG 图层、构件参数、PDF/Office 正文。
- 响应仍是 catalog-only。

## 9. 安全与泄露检查

结果：通过。

脚本和浏览器响应未发现：

- `/Volumes`
- `smb://`
- `nas://`
- raw `storage_path`
- raw `storage_uri`
- raw DB row
- SQL
- secret / token / password

禁止项检查：

- 未发现真实 NAS 增删改查。
- 未发现文件移动、删除、重命名或上传。
- 未发现 PDF / Office / DWG / RVT / IFC 正文读取。
- 未发现 BIM 构件级解析。
- 未发现 selective indexing。
- 未发现 Hermes memory 写入。
- 未发现 OpenSearch / Qdrant / MinIO documents/chunks 写入。
- 未发现生产发布能力。
- 未发现 Hermes 绕过平台权限或直接写库。

## 10. 回归结论

G2 回归通过。

G1 回归通过。

8A 回归通过。

Hermes Gateway 真实外部接入回归通过。

## 11. 收口建议

建议主 agent 收口 G3。

建议继续保持当前路线边界：G3 收口不等于进入 8B / 8C / 9A；下一阶段仍需用户明确裁决。

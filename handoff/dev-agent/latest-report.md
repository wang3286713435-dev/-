# 开发 Agent 报告：M2F 真实项目交付闭环试运行

时间：2026-05-22

## 1. 本轮目标

本轮执行 `M2F：真实项目交付闭环试运行`。

目标是以 `105 / 503` 真实 NAS 项目为样本，确认 M2E 已人工确认的正式工程主数据能驱动文档 / 图纸交付、缺失项解释、人工挂接防线、整改查询和交付包草案 dry-run。

## 2. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2f-real-project-delivery-loop-trial-plan.md`
- `handoff/main-agent/m2e-real-project-masterdata-confirmation-plan.md`
- `handoff/main-agent/m2d-real-project-masterdata-onboarding-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`

## 3. 当前 105 交付闭环审计结果

105 / 503 当前正式工程主数据状态：

- `deliverableStandardReady=true`
- 部位节点：11
- 节点类型：3，均已锁定
- 交付定义：3
- 交付类型：5
- 交付属性：15
- 目录模板：3

交付链路状态：

- 文档交付：`standardReady=true`，`totalRequired=22`，`missingCount=22`，当前尚未人工挂接文件。
- 图纸交付：`standardReady=true`，`totalRequired=22`，`missingCount=22`，当前尚未人工挂接文件。
- 缺失项解释：包含目标、交付定义、交付类型、期望文件类型，并提示必须人工选择项目资产目录文件补交。
- 候选推荐：图纸侧可生成候选推荐；应用推荐接口仍要求用户明确确认。
- 整改：接口可查，当前 105 无打开整改项。
- 交付包准备：`dryRun=true`，`physicalPackageGenerated=false`，`nasFileCopied=false`，`totalCount=44`，`missingCount=44`。

## 4. 修复的断点

发现并修复了一个小断点：

- 交付页面前端已有业务化解释，但后端 `delivery-completeness` 的 `missingReason` 和交付包缺失行的 `blockReason` 仍只返回“尚未挂接文件”，对员工不够明确。

本轮修复：

- `DeliveryApplicationService` 中缺失项原因改为：
  - `工程部位“建筑专业”缺少“文档交付 / PDF 文档”，需要人工选择当前项目资产目录中的文档文件完成补交。`
- 交付包草案 / 预检查中的缺失阻塞原因同步使用同一业务化说明。
- `AgentGovernanceApplicationService` 缺失项解释补充交付定义，避免只写交付类型。

未新增业务能力，只补齐解释口径和测试脚本。

## 5. 新增 M2F 专项脚本

新增：

- `scripts/dev/check-m2f-real-project-delivery-loop.sh`

脚本覆盖：

1. 管理员登录并切换到 `503 / 105`。
2. 标准状态、部位树、节点类型、交付定义、交付类型可查。
3. 文档 / 图纸交付基于正式规则返回应交缺失项。
4. 缺失项说明包含目标、交付定义、交付类型和文件类型。
5. 缺失项解释接口和图纸候选推荐可用。
6. 应用推荐必须人工确认，非法批量挂接参数会被拒绝。
7. 审核 / 整改查询接口可用。
8. 交付包草案 dry-run 可生成快照，不生成真实包、不复制 NAS 文件。
9. forbidden-field scan 未发现 raw path / SQL / secret 等敏感内容。

## 6. 当前链路状态

文档 / 图纸交付：

- 已能由正式工程主数据生成应交项。
- 当前 105 仍处于“全部待人工补交”状态。
- 下一步动作是从缺失项中人工选择项目资产目录文件补交。

文件挂接：

- 批量挂接接口要求合法目标、交付类型和文件参数。
- Agent 推荐应用接口必须 `confirmed=true`，否则拒绝。
- 本轮未自动挂接文件。

审核 / 整改：

- 相关查询接口可用。
- 本轮未在 105 上自动提交审核、自动驳回或自动整改。

交付包 / 档案目录：

- 当前 105 可生成交付包草案快照。
- 快照总计 44 条，全部因缺失挂接文件而阻塞。
- 档案目录为语义目录，不是真实 NAS 路径。

## 7. 改动文件列表

本轮修改 / 新增：

- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/DeliveryApplicationService.java`
- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/agentgovernance/AgentGovernanceApplicationService.java`
- `scripts/dev/check-m2f-real-project-delivery-loop.sh`
- `handoff/dev-agent/latest-report.md`

工作区仍存在主 agent / 其他批次留下的 `.claude/**`、`CLAUDE.md`、`tmp/**` 等未跟踪文件；本轮未触碰。

## 8. 数据库迁移

本轮未新增数据库迁移，未修改旧迁移。

## 9. 安全边界

- 是否触碰真实 NAS 文件：否。
- 是否复制 / 移动 / 删除 / 重命名真实 NAS 文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否新增 Hermes 能力：否。
- 是否接入 BIM 引擎 / parser / writer / indexing / 向量库：否。
- 是否 Agent 自动挂接、自动审核、自动整改：否。
- 是否修改 `docs/**`：否。
- 是否暴露 `storage_path` / `storage_uri` / raw NAS path / raw DB row / SQL / secret：专项脚本 forbidden-field scan 未发现。

## 10. 自测结果

```text
cd backend && ./mvnw -pl delivery-work-center -am -DskipTests compile      PASS
cd backend && ./mvnw -pl delivery-app -am -DskipTests package              PASS
corepack pnpm --dir frontend build                                        PASS（仅既有 Vite chunk size warning）
curl -fsS http://127.0.0.1:8080/actuator/health                           PASS：{"status":"UP"}
bash scripts/dev/check-m2f-real-project-delivery-loop.sh                  PASS=6 FAIL=0
bash scripts/dev/check-m2e-real-project-masterdata-confirmation.sh         PASS=11 FAIL=0
bash scripts/dev/check-m2c-delivery-package-archive.sh                    PASS=11 FAIL=0
git diff --check                                                          PASS
```

后端已用最新构建包重启，当前 `127.0.0.1:8080` 健康检查为 UP。

## 11. 已知风险

- 105 当前尚未进行真实人工文件挂接，因此文档 / 图纸交付仍是 44 个缺失项；本轮只确认正式规则能生成应交项和阻塞说明。
- 本轮未在 105 上执行真实审核驳回写入流程，避免向真实项目制造测试整改；审核 / 整改查询和人工确认防线已通过脚本检查。
- 交付包草案是 DB 快照和清单 dry-run，不代表真实物理交付包。
- 图纸候选推荐可用，文档侧候选是否充足取决于当前资产目录中 `PROCESSED` 文档文件和元数据质量。

## 12. 是否建议进入测试复核

建议进入测试 agent 复核 M2F。

重点复核：

1. 105 正式工程主数据是否能驱动文档 / 图纸应交项。
2. 缺失项和交付包阻塞说明是否业务可读。
3. 人工挂接确认防线是否保持。
4. 交付包草案是否仍为 dry-run 且不触碰真实 NAS。
5. 是否未新增 Hermes / BIM / parser / indexing 或真实 NAS 写能力。

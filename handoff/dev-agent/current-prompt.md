# 开发 Agent 当前任务：M1D 标准驱动交付闭环强化

你是数字化交付平台二期开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前开发方式：独立开发 Codex 会话主导开发。禁止创建子 agent，禁止调用 Claude Code。

## 0. 当前路线

M1A、M1B、M1C 均已收口。主线当前为绿灯，但这只代表可以继续平台本体开发，不代表可以进入 9A 客户交付。

当前 active 批次：

`M1D：标准驱动交付闭环强化`

完成承诺固定为：

`<promise>MAINLINE_M1D_STANDARD_DELIVERY_LOOP_COMPLETE</promise>`

本批目标不是新增大模块，而是把当前已有的工程主数据、文档交付、图纸交付、批量挂接、审核、整改、导出预检查等能力串成普通员工可执行的完整闭环。

## 1. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-current-roadmap.md`
4. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
5. `handoff/main-agent/m1c-real-project-masterdata-closure.md`
6. `handoff/main-agent/m1d-standard-delivery-loop-plan.md`
7. `handoff/dev-agent/latest-report.md`
8. `handoff/test-agent/latest-report.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

重点检查：

1. 交付页面与组件：
   - `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
   - `frontend/src/modules/work-center/pages/RectificationsPage.vue`
   - `frontend/src/modules/work-center/pages/DashboardPage.vue`
2. 项目工作台入口：
   - `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
   - `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
3. 工程主数据页面：
   - `frontend/src/modules/master-data/pages/ProjectInitializationPage.vue`
   - `frontend/src/modules/master-data/pages/SectionNodesPage.vue`
   - `frontend/src/modules/master-data/pages/NodeTypesPage.vue`
   - `frontend/src/modules/master-data/pages/DeliverableStandardPage.vue`
4. 工作中心后端：
   - `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/**`
   - `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/dto/WorkCenterDtos.java`
   - `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/report/**`
5. 既有验收脚本：
   - `scripts/dev/check-phase2-batch2-standard-delivery.sh`
   - `scripts/dev/check-phase2-batch3-review-rectification-report.sh`
   - `scripts/dev/check-phase2-batch6b-delivery-package.sh`
   - `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`

如实际文件位置不同，用 `rg` 定位，禁止凭空重复造模块。

## 2. 本轮必须解决的问题

当前平台已经有标准驱动交付的零件，但员工仍可能不知道完整操作顺序：

1. 应交项、缺失项、待审、驳回、可导出之间的关系不够直观。
2. 文档交付和图纸交付需要更清楚提示下一步动作。
3. 补交文件后，提交审核、审核通过、审核驳回、整改处理、复审或重新补交、完整率刷新要稳定。
4. 导出预检查要和当前交付状态连起来，但仍保持 dry-run。

## 3. 本轮目标

完成一条不依赖 Hermes 的标准驱动交付闭环：

`工程主数据就绪 -> 文档/图纸应交项 -> 缺失项 -> 补交文件 -> 提交审核 -> 审核通过/驳回 -> 整改处理 -> 复审或重新补交 -> 完整率刷新 -> 导出预检查`

### A. 交付页面流程引导

文档交付和图纸交付页面需要清楚展示：

- 标准底座是否就绪。
- 应交总数、已交、缺失、待审、已驳回、完整率。
- 当前下一步动作：
  - 去补标准。
  - 去补交文件。
  - 提交审核。
  - 处理整改。
  - 做导出预检查。

页面文案面向业务用户，不要只堆技术字段。

### B. 缺失项与补交闭环

缺失项必须让用户看懂：

- 缺的是哪个交付物定义。
- 缺的是哪种文件类型。
- 目标是部位还是对象。
- 为什么当前算缺失。
- 可以从当前项目资产目录选择哪些文件补交。

文件选择必须保持分页远程查询，不能回退成全量加载。

### C. 审核、整改、复审闭环

必须验证并必要时修复：

- 挂接后状态可提交审核。
- 审核通过后完整率刷新。
- 审核驳回后产生整改项。
- 整改项可处理、关闭、重新打开。
- 重新补交或复审后状态能回到交付链路。
- 审核记录和整改记录可追踪。

### D. 导出预检查联动

导出预检查仍保持 dry-run：

- 不生成真实文件包。
- 不访问、不复制、不移动 NAS 文件。
- 不泄露真实路径。

页面和接口需要能显示：

- 可纳入导出。
- 阻塞。
- 缺失。
- 待审。
- 已驳回。
- 需转换预览。
- 暂不支持预览。

### E. 多真实项目适用

至少抽查：

- 105/503。
- 93/506 或当前环境另一个真实 NAS 项目。

如果真实项目不适合制造写状态，用隔离 smoke 项目验证写链路；真实项目页面和只读状态必须通过。

## 4. 允许做什么

允许：

- 优化文档/图纸交付页面的信息结构、状态说明、下一步按钮和空状态。
- 修复完整率、缺失项、待审/驳回状态刷新问题。
- 修复审核、整改、复审链路里的 P0/P1/P2。
- 增强只读汇总字段或 DTO，但不要大改数据库。
- 新增或增强专项脚本，例如 `scripts/dev/check-m1d-standard-delivery-loop.sh`。

## 5. 禁止做什么

禁止：

1. 新增 Hermes 能力。
2. 继续 G4。
3. 进入 8B / 8C / 9A。
4. 开放真实 NAS 增删改查。
5. 读取 PDF / Office / DWG / RVT / IFC 正文。
6. 做真实 BIM 轻量化。
7. 做构件级解析。
8. 做 selective indexing。
9. 写 Hermes memory。
10. 写 OpenSearch / Qdrant / MinIO documents/chunks。
11. Agent 自动审批、自动整改、自动挂接。
12. 把 catalog metadata 冒充正文 evidence。
13. 返回真实 NAS 路径、raw row、SQL、token、secret、password。
14. 为 105 / 503 写死特殊逻辑。
15. 修改 `docs/**`，除非主 agent 单独授权。

## 6. 建议执行方式

建议按这个顺序做：

1. 审计 `DeliveryViewPanel.vue` 当前应交项、缺失项、审核、导出预检查展示。
2. 对照 105/503 与 93/506 页面，看普通员工是否能理解下一步动作。
3. 小范围补齐页面引导、状态说明、按钮语义和刷新逻辑。
4. 复查审核通过、审核驳回、整改状态变化后完整率是否刷新。
5. 新增 M1D 专项脚本，串联既有标准交付、审核整改、交付包预检查核心链路。
6. 不要扩大到新模块或 Hermes。

## 7. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch2-standard-delivery.sh
bash scripts/dev/check-phase2-batch3-review-rectification-report.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

如果新增 `scripts/dev/check-m1d-standard-delivery-loop.sh`，必须执行并记录。

浏览器自测至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/506/work/document-delivery`
- `/data-steward/assets/506/work/drawing-delivery`
- 文件补交弹窗、提交审核、审核通过/驳回、整改处理、导出预检查。

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. 当前 Git 分支和基线 commit。
2. 是否确认 M1D 当前 active。
3. 是否确认 G4 / Hermes / 8B / 9A 冻结。
4. 交付页面流程引导改了什么。
5. 缺失项、补交、审核、整改、复审和完整率刷新如何验证。
6. 导出预检查 dry-run 如何保持安全边界。
7. 105/503 与 93/506 抽查结果。
8. 是否触碰后端；如果触碰，说明是否只在 work-center/delivery 范围。
9. 修改文件清单。
10. 自测命令结果。
11. P0 / P1 / P2 列表。
12. 是否建议进入 M1D 测试验收。

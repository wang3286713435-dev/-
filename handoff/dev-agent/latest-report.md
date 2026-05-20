# 开发 Agent 报告：M1D 标准驱动交付闭环强化

时间：2026-05-20

## 1. 本轮目标

本轮按更新后的 `handoff/dev-agent/current-prompt.md` 继续 M1D，不进入 G4 / Hermes / 8B / 8C / 9A。

目标是补齐普通员工能看懂的标准驱动交付闭环信息结构，并并入项目工作台纠偏：

`项目资产 -> 工程主数据 -> 交付工作中心`

工作中心不是工程主数据的子功能，但必须在项目工作台下，并排在工程主数据之后；工程主数据未就绪时，工作中心入口和交付页面必须提示“请先生成 / 确认工程主数据草案”。

完成承诺仍为：

`<promise>MAINLINE_M1D_STANDARD_DELIVERY_LOOP_COMPLETE</promise>`

## 2. Git 与边界

- 当前分支：`codex/platform-m1d-delivery-loop`
- 当前基线提交：`b6bbb14`
- 当前 active 批次：确认是 `M1D：标准驱动交付闭环强化`
- G4 / Hermes / 8B / 8C / 9A：确认冻结，未扩展
- 本轮未提交 git
- 未修改 `docs/**`
- 未新增数据库迁移
- 未读取 PDF / Office / DWG / RVT / IFC 正文
- 未做 BIM 轻量化、parser、writer、indexing
- 未触碰真实 NAS 增删改查
- 未暴露 raw storage_path / storage_uri / NAS 原始路径 / SQL / raw row / token / secret / password
- 未创建子 agent，未调用 Claude Code

## 3. 已阅读关键材料

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1c-real-project-masterdata-closure.md`
- `handoff/main-agent/m1d-standard-delivery-loop-plan.md`
- `handoff/main-agent/m1d-workspace-ia-correction.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

## 4. 7 个信息结构补齐点

1. 项目工作台标题从“数据管家工作台”调整为“项目工作台”，首屏明确三段顺序。
2. 项目工作台工作流卡片改为：`01 项目资产`、`02 工程主数据`、`03 交付工作中心`。
3. 模块入口按三段重新分组，不再把工作中心视觉上混在数据管家模块里。
4. 工程主数据未就绪时，工作中心分组和入口显示“需先确认工程主数据 / 先确认主数据”。
5. 项目内导航同步改为 `项目资产 -> 工程主数据 -> 交付工作中心`，并显示主数据就绪状态。
6. 文档交付 / 图纸交付页面未就绪状态标题改为“请先生成 / 确认工程主数据草案”。
7. 交付页面保留可查看状态，但不表现为已经可以正常交付。

## 5. 交付页面流程引导

后端交付完整率接口已补充流程字段：

- `draftCount`
- `pendingReviewCount`
- `approvedCount`
- `rejectedCount`
- `reviewReadyCount`
- `approvedRate`
- `nextActionCode`
- `nextActionText`

前端文档交付 / 图纸交付页面已补齐：

- 标准驱动交付闭环说明
- 当前下一步动作
- 应交、已补交、缺失、草稿、待审、已通过、已驳回
- 补交完整率与审核通过率
- 缺失项原因业务化说明
- 文件选择远程分页提示
- 导出预检查仍作为 dry-run 动作展示

下一步动作口径：

- `COMPLETE_STANDARD`：生成/确认主数据
- `DEFINE_DELIVERABLES`：去补标准
- `BIND_MISSING_FILES`：补交文件
- `SUBMIT_REVIEW`：提交审核
- `REVIEW_PENDING`：等待/执行审核
- `HANDLE_RECTIFICATION`：处理整改
- `EXPORT_PRECHECK`：导出预检查

## 6. 工程主数据未就绪行为

项目工作台：

- 顶部主按钮在主数据未就绪时指向“生成 / 确认主数据草案”。
- 工作中心区块显示“交付工作中心暂不能作为正常交付流程使用”。
- 工作中心入口卡片显示“需先生成 / 确认工程主数据草案”。

项目内导航：

- 交付工作中心分组显示 `先确认主数据`。
- 点击工作中心入口时提示：`请先生成 / 确认工程主数据草案；工作中心页面会保留阻塞提示。`

交付页面：

- 未就绪 alert 标题为：`请先生成 / 确认工程主数据草案`。
- 页面提供“生成/确认工程主数据草案”和“去配置交付物标准”入口。
- 下一步动作的 `COMPLETE_STANDARD` 会回到项目初始化页面。

## 7. 后端改动

后端只触碰 work-center/delivery 范围：

- `DeliveryApplicationService`：计算交付闭环状态计数、审核通过率和下一步动作。
- `WorkCenterDtos`：扩展 `DeliveryCompletenessResponse` 返回字段。

未新增 migration，未改数据库结构，未接触 Hermes 逻辑。

## 8. 前端改动

- `DeliveryViewPanel.vue`：补齐闭环说明、状态卡、下一步动作、主数据未就绪引导、缺失项解释和 dry-run 预检查入口。
- `delivery.ts`：同步交付完整率 DTO 字段。
- `AssetProjectDetailPage.vue`：项目工作台按 `项目资产 -> 工程主数据 -> 交付工作中心` 重组。
- `ProjectWorkspaceNav.vue`：项目内导航按三段结构重组，并接入工程主数据就绪状态提示。

## 9. 脚本改动

新增：

- `scripts/dev/check-m1d-standard-delivery-loop.sh`

脚本覆盖：

- 标准未就绪
- 标准就绪
- 缺失项
- 批量挂接
- 提交审核
- 审核通过
- 审核驳回
- 整改生成
- 导出预检查 dry-run
- 前端信息结构静态检查

本轮已把 M1D 信息结构断言加入脚本，检查页面中存在：

- `项目资产`
- `工程主数据`
- `交付工作中心`
- `请先生成 / 确认工程主数据草案`

## 10. 浏览器抽查

当前本地服务：

- 后端：`127.0.0.1:8080`，screen 会话 `delivery-backend`
- 前端：`127.0.0.1:5173`，screen 会话 `frontend-m1d-ia`

浏览器已抽查：

- `/data-steward/assets/503`：项目工作台可见 `项目资产`、`工程主数据`、`交付工作中心`
- `/data-steward/assets/503/work/document-delivery`：项目内导航可见三段结构与 `标准驱动交付闭环`
- `/data-steward/assets/506/work/document-delivery`：工程主数据未就绪时可见 `请先生成 / 确认工程主数据草案`
- 页面可见文本中未发现 raw path / storage path / NAS path / SQL / secret / token 泄露

## 11. 自测结果

- 后端构建：通过
  - `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
- 前端构建：通过
  - `corepack pnpm --dir frontend build`
  - 仅有既存 chunk size warning
- 健康检查：通过
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - 返回 `{"status":"UP"}`
- M1D 专项脚本：通过
  - `bash scripts/dev/check-m1d-standard-delivery-loop.sh`
  - `PASS=29 FAIL=0`
- Batch2 标准交付回归：通过
  - 使用隔离 admin/project 执行，避免默认项目历史标准污染未就绪前置条件
  - `PASS=38 FAIL=0`
- Batch3 审核整改报表回归：通过
  - `PASS=52 FAIL=0`
- Batch4 文件访问回归：通过
  - `PASS=18 FAIL=0`
- Batch6B 交付包回归：通过
  - `PASS=17 FAIL=0`
- Batch7A 预览/导出预检查回归：通过
  - `PASS=18 FAIL=0`
- Batch8A BIM 轻量化 adapter 回归：通过
  - `PASS=11 FAIL=0`
- M1C 真实项目主数据回归：通过
  - `PASS=14 FAIL=0`
- `git diff --check`：通过

## 12. 105/503 与 93/506 抽查

- 105/503：页面抽查看到三段信息结构，文档交付页可见标准驱动闭环提示。
- 93/506：页面抽查看到工程主数据未就绪阻塞提示，交付页面提示先生成 / 确认工程主数据草案。
- 写链路验证使用隔离 smoke 项目和脚本完成，未对真实项目做 NAS 写操作。

## 13. 修改文件清单

- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/delivery/DeliveryApplicationService.java`
- `backend/delivery-work-center/src/main/java/com/zhuoyu/delivery/workcenter/dto/WorkCenterDtos.java`
- `frontend/src/modules/work-center/api/delivery.ts`
- `frontend/src/modules/work-center/components/DeliveryViewPanel.vue`
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
- `scripts/dev/check-m1d-standard-delivery-loop.sh`
- `handoff/dev-agent/latest-report.md`

注意：`handoff/test-agent/latest-report.md` 当前也在工作区显示为已修改，但不是本轮开发改动；本轮未覆盖或回退该文件。

## 14. P0 / P1 / P2

P0：

- 暂无未关闭 P0。

P1：

- 真实项目 503 / 506 已做浏览器抽查，但补交、提交审核、审核通过/驳回、整改复审的写链路主要通过隔离脚本验证；测试 agent 应按权限和数据条件决定是否追加 UI 级人工复核。

P2：

- 工作中心入口当前未强制禁用，而是提示后允许进入页面查看阻塞状态；这符合“页面保留状态可见，但不表现为正常交付”的口径。
- 前端构建仍有既存 chunk size warning，本轮未处理。

## 15. 安全边界

- 导出预检查仍为 dry-run。
- 未生成真实 ZIP 或交付包。
- 未访问、复制、移动、删除、上传、改名 NAS 文件。
- 未读取文件正文。
- 未做 DWG / RVT / IFC parser。
- 未做索引写入。
- 未做 Hermes 写入或 Hermes 能力扩展。
- 未把 catalog metadata 当正文 evidence。
- 未暴露真实路径。

## 16. 给测试 Agent 的补充验收 Prompt

````md
# 测试 Agent Prompt：M1D 信息结构与标准驱动交付闭环复核

工作目录：/Users/vc/Documents/数字化交付平台

本轮只复核 M1D：标准驱动交付闭环强化。不要进入 G4 / Hermes / 8B / 8C / 9A，不做生产部署。

重点验证：

1. 项目工作台是否清楚呈现：
   - 项目资产
   - 工程主数据
   - 交付工作中心
   顺序必须是 `项目资产 -> 工程主数据 -> 交付工作中心`。

2. 工作中心是否不是工程主数据子功能，但排在工程主数据之后。

3. 工程主数据未就绪时，以下位置是否提示：
   - 项目工作台入口
   - 项目内导航
   - 文档交付页
   - 图纸交付页
   文案核心必须包含：`请先生成 / 确认工程主数据草案`。

4. 文档交付 / 图纸交付页面是否能看懂：
   - 标准底座是否就绪
   - 应交总数
   - 已补交
   - 缺失
   - 草稿
   - 待审
   - 已通过
   - 已驳回
   - 补交完整率
   - 审核通过率
   - 当前下一步动作

5. 缺失项是否说明：
   - 缺哪个交付定义
   - 缺哪种文件类型
   - 目标是部位还是对象
   - 为什么缺失
   - 补交文件选择仍是远程分页，不得回退全量加载

6. 审核整改闭环是否仍稳定：
   - 挂接后可提交审核
   - 审核通过后完整率刷新
   - 审核驳回后产生整改项
   - 整改项可处理 / 关闭 / 重新打开
   - 重新补交或复审后能回到交付链路

7. 导出预检查仍必须是 dry-run：
   - 不生成真实文件包
   - 不访问、不复制、不移动 NAS 文件
   - 不泄露 raw storage_path / storage_uri / NAS path

建议执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m1d-standard-delivery-loop.sh
bash scripts/dev/check-phase2-batch2-standard-delivery.sh
bash scripts/dev/check-phase2-batch3-review-rectification-report.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh
bash scripts/dev/check-phase2-batch8a-bim-lightweight-adapter.sh
bash scripts/dev/check-m1c-real-project-masterdata.sh
git diff --check
```

浏览器抽查至少覆盖：

- Fresh login -> `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/503/work/rectifications`
- `/data-steward/assets/506/work/document-delivery`
- `/data-steward/assets/506/work/drawing-delivery`

验收报告请明确：

- 是否存在 raw path / storage_path / storage_uri / NAS path 泄露
- 是否发生真实 NAS 写操作
- 是否误进入 Hermes / G4 / 8B / 8C / 9A
- 是否建议 M1D 进入主 agent 收口判断
````

## 17. 是否建议进入 M1D 测试验收

建议进入测试 agent 验收。

本轮开发侧自测已通过，但不自行宣布 M1D 最终收口；最终是否收口由主 agent 和测试 agent 判断。

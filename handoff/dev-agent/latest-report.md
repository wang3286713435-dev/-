# 开发 Agent 报告：M1B 项目工作台与数据管家可用性收口

时间：2026-05-20

## 1. 本轮目标

本轮执行 `M1B：项目工作台与数据管家可用性收口`。

目标是在 M1A 平台主线基线已收口后，继续把普通员工进入真实项目的路径做清楚：先看真实 NAS 项目资产，再进入工程主数据和文档/图纸交付工作区。重点修复 `/data-steward/assets` 默认真实项目统计信息过弱、项目详情页过度突出 Hermes/治理助手、项目工作区导航缺少业务分区说明的问题。

完成标记：`<promise>MAINLINE_M1B_WORKBENCH_USABILITY_COMPLETE</promise>`

## 2. Git 基线

- 当前分支：`codex/platform-m1b-usability`
- 本轮主线基线提交：`cf53ad1 docs: start m1b workbench usability`
- 本轮未提交 git。
- 工作区原有未跟踪项仍保留：
  - `tmp/jar-inspect/`
  - `tmp/run-logs/hermes-gateway.pid`

## 3. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1a-platform-baseline-closure.md`
- `handoff/main-agent/m1b-project-workbench-usability-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

未修改 `docs/**`，未修改共享文档空间。

## 4. 命名与冻结边界

- 本轮只做 M1B。
- 未继续 G4 / Hermes 开发。
- 未进入 8B / 8C / 9A。
- 未新增 H1 / R1 等临时批次命名。
- 未修改 Hermes Gateway、Hermes memory、外部 Hermes 项目或 Agent 自动治理能力。

## 5. 改动文件

- `frontend/src/modules/data-steward/pages/AssetOverviewPage.vue`
  - 默认真实项目入口文案改为 M1B 项目资产入口。
  - 入口目标从“交付治理助手”改为“资产、主数据、文档/图纸交付”的工作顺序。
  - 接入 `fetchAssetStatistics`，真实 NAS 默认视图统计展示项目数、已登记文件、模型文件、图纸文件、登记容量等真实汇总。
  - 项目列表“治理路径”改为“下一步动作”，就绪项目优先引导到文档交付页。
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
  - 顶部主按钮从“开始交付治理”改为“查看文档交付”。
  - 增加“初始化主数据”入口，Hermes 保留为普通辅助入口。
  - 增加三段工作区说明：数据管家、工程主数据、工作中心。
  - 模块卡片改为围绕资产驾驶舱、文件管理、初始化向导、部位树、交付物标准、文档交付、图纸交付、整改闭环、模型集成、文件服务展开。
  - 项目副标题补充项目来源、接入状态、阶段、负责人。
- `frontend/src/modules/core/components/ProjectWorkspaceNav.vue`
  - 三组导航增加说明文字：文件资产/预览/治理风险，部位/节点类型/交付标准，文档图纸交付/审核整改/预检查。
  - “交付治理助手”导航文案收敛为“交付治理”，避免把 Hermes 作为主工作入口。
  - 调整导航布局，桌面三组并列，小屏单列，不产生横向滚动。
- `handoff/dev-agent/latest-report.md`
  - 写入本轮报告。

## 6. 后端与数据库

- 未修改后端代码。
- 未新增数据库迁移。
- 未修改旧 Flyway migration。
- 未新增、修改或删除生产数据脚本。
- 回归脚本运行过程中按既有 smoke 逻辑创建/复用测试数据，未做生产部署。

## 7. 安全边界

确认保持：

- 未触碰真实 NAS 文件。
- 未创建、移动、删除、重命名、上传真实 NAS 文件。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未做 BIM 轻量化、模型转换或构件解析。
- 未启用 parser / writer / indexing。
- 未写 OpenSearch / Qdrant / MinIO documents / chunks。
- 未让 Agent 自动审批、自动整改、自动挂接或自动删除。
- 未暴露 raw NAS path、raw DB row、SQL、secret、token、password。
- 未把 catalog metadata 当正文 evidence。

## 8. 路径脱敏复核

- 浏览器自测覆盖 `/data-steward/assets`、`/data-steward/assets/503`、`/data-steward/assets/506`、初始化、文档交付、图纸交付页面，可见文本未命中 `/Volumes/`、`/Users/vc/`、`nas://`、`smb://`、`storage_path`、`storage_uri`、`storagePath`、`storageUri` 等 raw path 模式。
- 代码复扫发现 `AssetProjectDetailPage.vue` 中仍有 `storagePath` 字段名引用，但用途是既有“底层路径已隐藏”展示、路径隐藏检测和缺路径质量标记，不展示 raw path 值。
- 本轮新增文案没有加入真实路径、SQL、secret、token。

## 9. 构建与脚本结果

- 后端构建：通过。
  - `./mvnw -pl delivery-app -am -DskipTests package`
  - `BUILD SUCCESS`
- 前端构建：通过。
  - `corepack pnpm --dir frontend build`
  - 仅保留既有 Vite chunk size warning。
- 健康检查：通过。
  - `curl -fsS http://127.0.0.1:8080/actuator/health`
  - `{"status":"UP"}`
- 文件访问安全回归：通过。
  - `bash scripts/dev/check-phase2-batch4-file-access.sh`
  - `PASS=18 FAIL=0`
- 6A 项目初始化回归：通过。
  - `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
  - 脚本返回 `phase2 batch6a project initialization ok`
- 6B 交付包回归：通过。
  - `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
  - `PASS=17 FAIL=0`
- `git diff --check`：通过。

## 10. 浏览器自测结果

已使用新登录会话完成关键路径自测：

- `/data-steward/assets`：通过，能看到 `M1B 项目资产入口`、`当前视图统计`、`已登记文件`。
- `/data-steward/assets/503`：通过，能看到数据管家、工程主数据、工作中心说明，以及“查看文档交付”“初始化主数据”。
- `/data-steward/assets/506`：通过，能看到同样的工作区说明和主入口。
- `/data-steward/assets/503/master-data/initialization`：通过。
- `/data-steward/assets/503/work/document-delivery`：通过。
- `/data-steward/assets/503/work/drawing-delivery`：通过。
- 浏览器 console 未发现 error/warning。
- 390px 小屏检查通过：`/data-steward/assets` 与 `/data-steward/assets/503` 均未产生横向滚动。

## 11. 真实项目抽查

- `503 / 105 / 启航华居项目`
  - 项目详情、初始化向导、文档交付、图纸交付页面均可打开。
  - 导航展示三段业务分区，主按钮进入文档交付。
- `506 / 93 / 中建八局国交酒店项目`
  - 项目详情页可打开。
  - 可见真实项目工作区说明，未因主数据/交付数据稀疏导致页面错误。

## 12. 已知风险与未完成事项

- 本轮只做 M1B 可用性收口，没有重做整个前端信息架构。
- `AssetProjectDetailPage.vue` 仍保留 Hermes 抽屉和“问 Hermes”入口，但已降级为辅助入口，没有作为主路径。
- 前端主包体积 warning 为既有 P2，本轮未处理。
- 93 项目交付数据仍相对稀疏，这是数据现状，不是本轮前端问题。

## 13. 是否建议测试 Agent 验收

建议进入测试 Agent 对 M1B 做验收。

重点请测试：

- 真实项目默认统计是否不再显示为 0。
- 普通员工是否能从资产总览进入项目、主数据初始化、文档交付、图纸交付。
- 503/506 两个真实项目是否都可用。
- 页面是否仍保持路径脱敏、只读、安全边界。

## 14. 给测试 Agent 的测试 Prompt

```md
# 测试 Agent Prompt：M1B 项目工作台与数据管家可用性验收

工作目录：`/Users/vc/Documents/数字化交付平台`

本轮只验收 `M1B：项目工作台与数据管家可用性收口`。M1A 已收口，G4 / Hermes 冻结。不要测试或推进 Hermes 新能力，不进入 8B / 8C / 9A，不测试真实 NAS 写操作、正文解析、BIM 构件解析、parser/writer/indexing 或 production rollout。

必须先读：

- `handoff/dev-agent/current-prompt.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/development-log.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
- `handoff/main-agent/m1a-platform-baseline-closure.md`
- `handoff/main-agent/m1b-project-workbench-usability-plan.md`
- `docs/07-complete-delivery-prd.md`
- `docs/08-acceptance-and-agent-integration.md`
- `docs/10-phase2-development-roadmap.md`

重点验收：

1. 资产总览默认视图
   - fresh login 后进入 `/data-steward/assets`。
   - 默认应聚焦真实 NAS 项目。
   - 应显示 M1B 项目资产入口、工作顺序、当前视图统计、下一步动作、风险提醒。
   - 真实项目统计不应全部为 0，至少应能看到真实项目数、已登记文件、模型文件、图纸文件、登记容量。

2. 项目详情可用性
   - 打开 `/data-steward/assets/503` 和 `/data-steward/assets/506`。
   - 页面应清楚展示三组入口：数据管家、工程主数据、工作中心。
   - 顶部主按钮应优先指向“查看文档交付”和“初始化主数据”，Hermes 只作为辅助入口。
   - 不应把用户主路径引到 Hermes/G4。

3. 工作区导航
   - 在 503 项目的以下页面间切换：
     - `/data-steward/assets/503/master-data/initialization`
     - `/data-steward/assets/503/work/document-delivery`
     - `/data-steward/assets/503/work/drawing-delivery`
   - 导航说明应保持可见，页面不应 403/404/500。
   - 小屏或窄窗口下不应出现横向滚动或文字明显遮挡。

4. 安全边界
   - 前端可见文本和接口响应不得出现 raw `storage_path`、`storage_uri`、真实 NAS 路径、SQL、secret、token、password。
   - 不允许触发真实 NAS 文件创建、移动、删除、重命名、上传。
   - 不允许读取 PDF / Office / DWG / RVT 正文。
   - 不允许启用 parser / writer / indexing。
   - 不允许 Hermes 自动审批、自动整改、自动挂接或自动删除。

建议执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-phase2-batch6a-project-initialization.sh
bash scripts/dev/check-phase2-batch6b-delivery-package.sh
git diff --check
```

浏览器验收建议：

- fresh login
- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/506`
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`

测试报告需明确：

- M1B 是否通过。
- 是否发现 raw path 残留。
- 是否发现页面主路径仍错误指向 Hermes/G4。
- 是否发现 503/506 路由或页面加载失败。
- 是否发现构建或回归脚本失败。
- 是否建议主 agent 判定 M1B 收口。
```

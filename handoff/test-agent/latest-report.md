# M1B 项目工作台与数据管家可用性验收报告

生成时间：2026-05-20 15:32 CST

## 1. 测试结论

结论：通过。

本轮只验收 `M1B：项目工作台与数据管家可用性收口`。M1A 已收口，G4 / Hermes 冻结；本轮未测试或推进 Hermes 新能力，未进入 8B / 8C / 9A，未触发真实 NAS 写操作、正文解析、BIM 构件解析、parser / writer / indexing 或 production rollout。

建议：未发现 P0 / P1 / P2，建议主 agent 判定 M1B 收口。

## 2. 当前分支与提交

- 当前分支：`codex/platform-m1b-usability`
- 当前提交：`cf53ad1`
- 工作区状态：存在开发 agent 本轮未提交前端改动与临时目录；测试 agent 未修改业务代码，未修改 `docs/**`，仅更新本测试报告。

## 3. 必读文件

已阅读并按要求参考：

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

## 4. P0 / P1 / P2

P0：未发现。

P1：未发现。

P2：未发现。

非阻塞观察：

- 前端构建仍有既有 Vite chunk size warning，不影响构建通过。
- 浏览器验收期间，本地后端进程曾因测试会话结束触发正常 shutdown，重启后页面与脚本均恢复正常；判断为本地测试进程保活问题，不是产品缺陷。

## 5. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 chunk size warning。
- 后端健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- 文件访问安全回归：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`，输出 `phase2 batch4 file access check ok`。
- 项目初始化回归：`bash scripts/dev/check-phase2-batch6a-project-initialization.sh`，通过，输出 `phase2 batch6a project initialization ok`。
- 交付包回归：`bash scripts/dev/check-phase2-batch6b-delivery-package.sh`，通过，`PASS=17 FAIL=0`，输出 `ALL PASS`。
- 空白字符检查：`git diff --check`，通过。

## 6. 资产总览默认视图

结果：通过。

浏览器 fresh login 后进入 `/data-steward/assets`，默认筛选为真实项目视图，页面 Hero 区已明确表达：

- 平台入口是“先选真实项目，再进入数字化交付工作区”。
- 工作顺序是进入真实项目、查看接入评估、完善工程主数据、进入文档交付。
- 风险提醒明确说明只展示目录级治理线索，不读取文件正文，也不触碰 NAS 文件。
- 页面文案明确普通员工不需要先问 Hermes。

真实项目统计已加载，未再全部为 0：

- 项目：`16`
- 已登记文件：`40,935`
- 模型文件：`2,604`
- 图纸文件：`38,331`
- 登记容量：`296 GB`
- 主数据已建：`2/16`
- 待补底座：`15`
- 有文件项目：`16`

默认项目列表可见真实 NAS 项目，例如 `105 启航华居项目`、`100 深圳市二十八高项目`、`101 C塔`、`93 中建八局国交酒店项目`。未发现默认视图混入明显测试 / 样例 / 归档项目。

## 7. 项目详情可用性

结果：通过。

已抽查：

- `/data-steward/assets/503`：业务编码 `105`，项目名 `启航华居项目`。
- `/data-steward/assets/506`：业务编码 `93`，项目名 `中建八局国交酒店项目`。

两个项目均能看到统一项目工作台结构，三组入口清晰可见：

- 数据管家：文件资产、预览和治理风险。
- 工程主数据：部位、节点类型和交付标准。
- 工作中心：文档图纸交付、审核整改和预检查。

顶部主按钮优先指向 `查看文档交付` 和 `初始化主数据`。Hermes 仅作为辅助入口，未发现页面主路径错误引导到 Hermes / G4。

## 8. 工作区导航

结果：通过。

已在 503 项目下抽查：

- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`

验证结果：

- 三个页面均保留项目工作台顶部导航。
- 项目上下文持续显示为 `启航华居项目 / 105`。
- 页面说明保持可见，能解释当前页面用途和推荐操作顺序。
- 未出现 403 / 404 / 500。
- 桌面浏览器视口下未发现页面级横向撑爆或明显文字遮挡。

## 9. 安全边界

结果：通过。

本轮页面与脚本抽查未发现：

- raw `storage_path`
- `storage_uri`
- `/Volumes`
- `smb://`
- `nas://`
- SQL
- raw DB row
- secret / token / password

本轮未触发：

- 真实 NAS 文件创建、移动、删除、重命名或上传。
- PDF / Office / DWG / RVT 正文读取。
- parser / writer / indexing。
- Hermes 自动审批、自动整改、自动挂接或自动删除。

说明：506 项目导出页可见的是项目内相对路径提示，不是 raw NAS 绝对路径；未作为 raw path 泄露。

## 10. 回答验收问题

- M1B 是否通过：通过。
- 是否发现 raw path 残留：未发现。
- 是否发现页面主路径仍错误指向 Hermes / G4：未发现。
- 是否发现 503 / 506 路由或页面加载失败：未发现；本地后端测试进程短暂退出后重启恢复，未判定为产品路由问题。
- 是否发现构建或回归脚本失败：未发现。
- 是否建议主 agent 判定 M1B 收口：建议收口。

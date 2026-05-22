# M2G 真实 NAS 文件管理器灰度完善验收报告

生成时间：2026-05-23 03:00 CST

## 1. 测试结论

结论：通过。

M2G 已达到本轮验收目标：专项脚本在隔离测试项目和临时 NAS 根目录中验证了默认关闭、灰度开启、上传、新建、重命名、移动、移入回收站、恢复、权限边界、范围边界、操作记录、回收站和审计；真实项目文件管理页短验通过，未执行真实业务目录写操作。

当前未发现 P0 / P1。P2 为既有 Vite chunk size warning、非交付未跟踪文件、以及数字孪生 / 可视化并行分支改动观察项，不阻塞 M2G 收口判断。

建议主 agent 收口 M2G，但收口提交时应只选择 M2G 文件管理器、M2G 脚本和 handoff 报告，不要混入数字孪生 / 可视化改动。

## 2. 必读文件

已阅读：

- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`

本轮按轻量测试策略执行，但按 current prompt 要求做了 M2G 专项脚本和一次真实项目文件管理页短浏览器验收。

## 3. 必跑命令结果

- 后端构建：`cd backend && ./mvnw -pl delivery-app -am -DskipTests package`，通过，`BUILD SUCCESS`。
- 前端构建：`corepack pnpm --dir frontend build`，通过，仅有既有 Vite chunk size warning。
- 健康检查：`curl -fsS http://127.0.0.1:8080/actuator/health`，通过，返回 `{"status":"UP"}`。
- M2G 专项脚本：`bash scripts/dev/check-m2g-nas-file-manager-polish.sh`，通过，`PASS=26 FAIL=0`。
- M2B 回归：`bash scripts/dev/check-m2b-nas-write-trial.sh`，通过，`PASS=18 FAIL=0`。
- 文件访问安全回归：`bash scripts/dev/check-phase2-batch4-file-access.sh`，通过，`PASS=18 FAIL=0`。
- 空白字符检查：`git diff --check`，通过。

## 4. M2G 专项脚本结果

脚本存在：`scripts/dev/check-m2g-nas-file-manager-polish.sh`。

脚本通过并覆盖：

- 自动创建隔离测试项目和 `/tmp/delivery-m2g-nas-*` 临时 NAS 根目录。
- 默认无灰度配置时，写操作关闭且不落盘。
- 开启灰度后，只允许 `trial-zone` 相对目录范围。
- 隔离目录内文件闭环通过：上传、重命名、移动、移入回收站、恢复。
- 隔离目录内文件夹闭环通过：新建、重命名、移动、移入回收站、恢复。
- 查看者不能写。
- 允许范围外新建目录被拒绝。
- 允许范围内文件移出白名单被拒绝。
- 文件夹不能移动到自身或子文件夹内。
- 操作记录和回收站同时覆盖文件、文件夹记录，且不泄露真实路径。
- 灰度配置和文件管理操作已写入审计日志。

结论：通过。

## 5. 浏览器短验结果

短验页面：

- `http://127.0.0.1:5173/data-steward/assets/503?tab=files`

结果：

- 页面可打开，非白屏，非 404。
- 左侧目录树可见。
- 右侧文件表可见，包含文件名、类型、版本、大小、专业、状态、操作等列。
- 顶部工具栏可见：`上传文件`、`新建文件夹`、`刷新`、`回收站`、`操作记录`。
- 当前文件夹状态可见。
- 页面提示 `真实 NAS 写入灰度已开启`。
- 页面提示 `当前页面按项目工作台项目执行，不受全局当前项目显示影响`。
- 当前目录说明清楚：`当前目录允许操作`、`当前目录可写`、`可以上传、新建、重命名和移动；删除会进入回收站，不会永久删除`。
- 页面级横向溢出为 `0`。
- 页面可见文本未发现真实 NAS 路径、`storage_path`、`storage_uri`、SQL、token、secret、password。
- 本轮没有在真实业务目录点击上传、新建、移动、删除、恢复等写按钮。

结论：通过。

## 6. 安全与红线检查

接口响应、脚本断言、页面可见文本和静态扫描未发现 M2G 引入以下问题：

- 真实业务目录被脚本写入、移动、删除、改名或复制。
- 真实 NAS 路径泄露。
- 权限绕过。
- 普通用户永久删除能力开放。
- 新增 Hermes / BIM / parser / indexing 能力。
- 读取 PDF / Office / DWG / RVT / IFC 正文。
- 写 OpenSearch / Qdrant / MinIO documents/chunks。

本轮脚本真实写操作仅发生在隔离测试项目和 `/tmp/delivery-m2g-nas-*` 临时目录。

静态扫描命中说明：

- `scripts/dev/check-m2g-nas-file-manager-polish.sh` 中的 `/Volumes`、`nas://`、`smb://`、`storage_path`、`storage_uri`、token、secret、password 命中均为 forbidden-field 断言、测试登录参数或请求头变量。
- `AssetProjectFileBrowser.vue` 命中 `真实 NAS`、`不会永久删除` 等用户提示文案，未发现真实路径输出。
- 受控 NAS 后端既有 `Files.copy` / `Files.move` 调用来自受控写能力本身，本轮 M2G 未修改后端核心写逻辑，专项脚本已验证灰度、权限、范围和审计边界。

## 7. 工作区改动范围检查

M2G 相关改动：

- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m2g-nas-file-manager-polish.sh`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`

并行分支改动观察项：

- `backend/delivery-visualization-adapter/**`
- `frontend/src/modules/visualization/**`
- `frontend/src/modules/core/layout/AppLayout.vue`
- `frontend/src/router/index.ts`
- `backend/delivery-core/src/main/java/com/zhuoyu/delivery/core/user/application/CurrentUserApplicationService.java`

判断：

- 这些数字孪生 / 可视化相关改动未导致本轮后端构建、前端构建、M2G 专项、M2B 回归、文件访问安全回归失败。
- 按 current prompt 规则，记录为并行分支改动观察项，不阻塞 M2G。
- M2G 收口提交时应选择性提交文件管理器、M2G 脚本和 handoff 文件，不应把数字孪生 / 可视化改动混入 M2G 提交。

既有非交付未跟踪文件：

- `.claude/**`
- `CLAUDE.md`
- `tmp/**`

不判定为失败，但收口提交时应排除。

## 8. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 既有 Vite chunk size warning，未阻塞构建和本轮验收。
- 工作区仍有 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪文件，收口提交时需排除。
- 数字孪生 / 可视化相关文件处于修改状态，未导致本轮回归失败；建议作为并行分支改动单独收口，不混入 M2G 提交。

## 9. 是否建议主 agent 收口 M2G

建议主 agent 收口 M2G。

理由：后端构建、前端构建、健康检查、M2G 专项脚本、M2B 回归、文件访问安全回归和 `git diff --check` 均通过；专项脚本完整覆盖隔离目录内文件 / 文件夹写操作闭环、权限边界、范围边界、回收站恢复、操作记录和审计；浏览器短验确认真实项目文件管理页可用、按钮和可写状态清楚、无横向溢出；未发现真实路径或敏感字段泄露，未发现真实业务目录被脚本写入。

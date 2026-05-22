# 开发 Agent 报告：M2G 真实 NAS 文件管理器灰度完善

时间：2026-05-23 02:38 CST

## 1. 本轮目标

本轮执行 `M2G：真实 NAS 文件管理器灰度完善`。

目标是把已具备的受控 NAS 写入能力打磨成更像日常文件管理器的体验：项目工作台内按路由项目执行、当前目录可写状态清楚、上传 / 新建 / 重命名 / 移动 / 移入回收站 / 恢复形成闭环，且所有脚本写操作只发生在隔离临时目录。

## 2. 读取的关键文档

- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`
- `handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- `handoff/main-agent/m2b-nas-write-trial-plan.md`
- `handoff/main-agent/lightweight-test-strategy.md`
- `handoff/main-agent/digital-twin-pr-compatibility-check.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/latest-report.md`

## 3. 当前文件管理器问题审计

已有能力：

- 左侧目录树、右侧文件表、上传文件、新建文件夹。
- 文件 / 文件夹重命名、移动、移入回收站、恢复。
- 真实 NAS 写入灰度状态、回收站、操作记录。
- 后端已按项目权限、灰度开关、允许目录和审计校验。

发现的体验断点：

- 前端项目角色判断会在路由项目找不到时兜底使用全局 `currentProject`，容易把全局项目状态带入当前项目工作台。
- 当前目录是否可写和不可写原因主要藏在按钮 tooltip 和灰度提示中，不够直观。
- 当前文件夹重命名 / 移动 / 移入回收站入口堆在顶部，文件管理器主操作区略拥挤。
- 移动目标原来是自由输入，缺少“只能项目内相对目录、不能真实 NAS 路径、目标目录必须存在”的前端校验。
- 操作记录和回收站里仍显示部分原始状态码 / 类型码，需要转成业务语言。

## 4. 前端修复内容

修改：

- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`

完成内容：

- NAS 写按钮、回收站恢复权限只按当前路由 `projectId` 对应的项目角色判断，不再用全局 `currentProject` 兜底。
- 如果全局当前项目和工作台路由项目不一致，页面显示“按本页面项目执行”，后端仍继续做真实权限和灰度校验。
- 新增“当前文件夹状态”区域，直接展示：
  - 当前文件夹。
  - 当前目录可写 / 不可写。
  - 不可写原因或可执行操作说明。
- 顶部工具栏收敛为：
  - 上传文件。
  - 新建文件夹。
  - 刷新。
  - 当前文件夹下拉操作。
  - 回收站。
  - 操作记录。
- 当前文件夹的重命名 / 移动 / 移入回收站移入下拉菜单。
- 新建文件夹成功后自动进入新文件夹，并刷新目录树、文件列表和当前目录状态。
- 上传、重命名、移动、移入回收站、恢复后统一刷新必要视图。
- 文件 / 文件夹移动目标增加前端校验：
  - 只允许项目内相对目录。
  - 禁止真实 NAS 地址、绝对路径、`:`、反斜杠、`//`。
  - 禁止 `.` / `..`。
  - 移动文件夹时禁止移动到自身或子文件夹。
  - 目标目录必须存在于当前目录树。
- 操作记录状态从 `SUCCEEDED` 等码值转为“成功 / 失败 / 处理中”。
- 回收站类型从 `FILE / DIRECTORY` 转为“文件 / 文件夹”，状态转为“在回收站 / 已恢复 / 恢复失败”。

## 5. 后端修复内容

本轮未修改后端业务代码。

审计结论：

- 受控 NAS 后端仍通过 `projectId` 路由参数、JWT 当前用户、项目角色、灰度配置和允许目录执行校验。
- 后端操作响应只返回 `displayPath/pathHint`、操作编号、traceId 等安全字段。
- 操作记录和回收站列表未返回 `storage_uri` / `storage_path` / 真实 NAS 绝对路径。

## 6. 新增 M2G 专项脚本

新增：

- `scripts/dev/check-m2g-nas-file-manager-polish.sh`

脚本特点：

- 自动创建隔离测试项目和 `/tmp/delivery-m2g-nas-*` 临时 NAS 根目录。
- 默认无灰度配置时验证写操作被拒绝。
- 开启灰度后只允许 `trial-zone` 相对目录范围。
- 覆盖文件闭环：
  - 上传文件。
  - 重命名文件。
  - 移动文件。
  - 移入回收站。
  - 恢复。
- 覆盖文件夹闭环：
  - 新建文件夹。
  - 重命名文件夹。
  - 移动文件夹。
  - 移入回收站。
  - 恢复。
- 覆盖边界：
  - 查看者不能写。
  - 允许范围外写操作被拒绝。
  - 文件不能移出白名单。
  - 文件夹不能移动到自身或子文件夹内。
  - 操作记录 / 回收站 / 目录树 / 文件列表 forbidden-field scan。
  - 审计日志写入。

脚本结果：

```text
bash scripts/dev/check-m2g-nas-file-manager-polish.sh  PASS=26 FAIL=0
```

## 7. 操作链路状态

- 上传文件：隔离目录脚本通过，成功后文件列表可查。
- 新建文件夹：隔离目录脚本通过，前端成功后进入新文件夹。
- 重命名文件：隔离目录脚本通过。
- 移动文件：隔离目录脚本通过，前端增加目录输入校验。
- 删除到回收站：隔离目录脚本通过；用户可见文案统一为“回收站 / 移入回收站”。
- 恢复：隔离目录脚本通过，恢复冲突仍由后端返回业务错误。
- 文件夹重命名 / 移动 / 移入回收站 / 恢复：隔离目录脚本通过。

## 8. 浏览器短验

已用 Chrome 打开真实项目文件管理页：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files`

短验结果：

- 页面可打开，无白屏。
- 左侧目录树、右侧文件表、顶部工具栏可见。
- 新增“当前文件夹状态”区域可见。
- 当前目录可写状态和“按本页面项目执行”提示可见。
- 未执行任何真实 NAS 写按钮。
- 未在页面可见内容中发现真实 NAS 绝对路径。

## 9. 安全边界

- 是否触碰真实业务目录：否。
- 是否写真实业务 NAS 文件：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否新增 Hermes 能力：否。
- 是否新增 BIM / parser / writer / indexing / 向量库能力：否。
- 是否开放普通用户永久删除：否。
- 是否绕过灰度开关、项目权限、角色权限或审计：否。
- 是否暴露 `storage_path` / `storage_uri` / raw NAS path / raw DB row / SQL / secret：专项脚本和回归脚本未发现。
- 是否修改 `docs/**`：否。

## 10. 自测结果

```text
corepack pnpm --dir frontend build                                  PASS（仅既有 Vite chunk size warning）
cd backend && ./mvnw -pl delivery-app -am -DskipTests package        PASS
curl -fsS http://127.0.0.1:8080/actuator/health                     PASS：{"status":"UP"}
bash scripts/dev/check-m2g-nas-file-manager-polish.sh                PASS=26 FAIL=0
bash scripts/dev/check-m2b-nas-write-trial.sh                        PASS=18 FAIL=0
bash scripts/dev/check-phase2-batch4-file-access.sh                  PASS=18 FAIL=0
git diff --check                                                     PASS
```

## 11. 改动文件列表

本轮修改 / 新增：

- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `scripts/dev/check-m2g-nas-file-manager-polish.sh`
- `handoff/dev-agent/latest-report.md`

工作区已有但非本轮触碰：

- `frontend/src/modules/visualization/pages/DigitalTwinPortalPage.vue`
- `.claude/**`
- `CLAUDE.md`
- `tmp/**`

## 12. 已知风险和未做项

- 本轮没有修改后端核心 NAS 写逻辑，只做前端体验和专项脚本补强。
- 浏览器短验没有执行真实业务目录写操作，只确认页面可打开和提示可见。
- 移动目标本轮采用“明确相对目录输入校验”，尚未做完整图形化目录选择器。
- `503 / 105` 当前页面显示已有真实 NAS 灰度状态，但本轮没有点击真实写按钮，也没有创建、移动、删除真实业务文件。
- 前端构建仍有既有 chunk size warning，主要与当前主线已有大 chunk 和数字孪生依赖有关，不阻塞本轮。

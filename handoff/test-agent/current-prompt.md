# 测试 Agent 当前任务：M3G-5 文件管理器项目全局搜索与存储展示修复验收

你是卓羽智能数据中台的测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

当前验收目标：

`M3G-5：文件管理器项目全局搜索与存储展示修复`

## 0. 本轮只验收什么

本轮不执行对象化迁移，只验收文件管理器检索和展示体验修复：

- 搜索默认项目全局。
- 可选限制为当前文件夹及子目录。
- 未搜索时目录浏览仍只显示当前目录直接子项。
- 文件表 / 详情能区分 `OBJECT_STORED` 与 `NAS_ONLY`。
- 主界面不泄露真实 NAS 路径或底层对象信息。

## 1. 必读文件

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `handoff/dev-agent/current-prompt.md`
- `handoff/main-agent/m3g5-file-manager-search-storage-display-plan.md`
- `scripts/dev/check-m3g5-file-manager-search-storage-display.sh`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m3g5-file-manager-search-storage-display.sh
bash scripts/dev/check-m3g4-controlled-multi-project-objectification.sh
bash scripts/dev/check-m3g3-multi-project-objectification-planning.sh
bash scripts/dev/check-m3e-preview-artifacts-object-storage.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

不要运行 M3G-2 执行型脚本。

## 3. 重点断言

必须确认：

1. 105 / 503 并非 100% 对象化，页面/接口能区分 `OBJECT_STORED` 与 `NAS_ONLY`。
2. 未输入关键词时，右侧只显示当前目录直接子文件夹和直接文件。
3. 输入关键词时，默认在整个项目内搜索。
4. 搜索结果展示所在位置 / 项目内路径。
5. 开启“仅当前文件夹及子目录”后，搜索范围被限制。
6. 清空关键词后恢复当前目录直接子项浏览。
7. 主界面和接口响应不泄露真实 NAS 路径、bucket、object key、`storage_uri`、SQL、raw row、token、secret。
8. 未创建新的对象化迁移任务。
9. 未移动、删除、重命名、覆盖真实 NAS 原文件。
10. M3G-4 / M3G-3 / M3E / file-access 回归通过。

## 4. P0 / P1 判定

P0：

- 搜索或页面操作触发真实 NAS 文件移动 / 删除 / 改名 / 覆盖。
- 搜索接口泄露真实 NAS 路径、bucket、object key、secret、token。
- 搜索误创建对象化任务。
- file-access 权限链路回归失败。

P1：

- 项目全局搜索不可用。
- 搜索仍默认局限在当前目录。
- 当前目录直接子项浏览回归。
- `OBJECT_STORED` 与 `NAS_ONLY` 无法区分。
- M3G-4 / M3G-3 / M3E / file-access 关键回归失败。

P2：

- 既有 Vite chunk warning。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。
- 文案细节可继续优化但不影响主链路。

## 5. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告必须包含：

- 测试结论。
- P0 / P1 / P2。
- 必跑命令结果。
- 搜索默认范围验证。
- 当前文件夹搜索验证。
- 存储状态展示验证。
- 是否创建对象化任务。
- 是否触碰真实 NAS 原文件。
- 是否出现禁出字段。
- 是否建议主 agent 收口 M3G-5。

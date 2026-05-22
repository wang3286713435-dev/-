# 测试 Agent 当前任务：M2G 真实 NAS 文件管理器灰度完善验收

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 测试策略

本轮采用轻量测试策略，但 M2G 涉及文件管理器真实写操作，所以必须跑专项脚本。浏览器只做短验，不做全站巡检。

先阅读：

- `handoff/main-agent/lightweight-test-strategy.md`

## 0. 当前验收批次

`M2G：真实 NAS 文件管理器灰度完善`

目标是验证文件管理器的上传、新建、重命名、移动、删除到回收站、恢复、操作记录和可写状态提示是否形成可用闭环。

## 1. 必须先阅读

- `handoff/main-agent/m2g-real-nas-file-manager-polish-plan.md`
- `handoff/dev-agent/latest-report.md`
- `handoff/main-agent/status.md`
- `handoff/main-agent/phase2-current-roadmap.md`

## 2. 必跑命令

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如果没有 `scripts/dev/check-m2g-nas-file-manager-polish.sh`，记 P1。

## 3. 专项验收

脚本必须确认：

1. 默认无灰度配置时写操作被拒绝。
2. 开启灰度后，隔离目录内可完成：
   - 新建文件夹。
   - 上传文件。
   - 重命名文件。
   - 移动文件。
   - 删除到回收站。
   - 恢复。
3. 目录操作可完成：
   - 新建目录。
   - 重命名目录。
   - 移动目录。
   - 删除到回收站。
   - 恢复。
4. 查看者不能写。
5. 越过允许范围的操作被拒绝。
6. 操作记录和回收站响应不泄露真实 NAS 路径。
7. 审计日志有记录。

## 4. 浏览器短验

本轮需要做一次短浏览器验收，只打开一个真实项目文件管理页：

- 文件管理页可打开。
- 左目录树、右文件表、工具栏不白屏。
- 当前目录可写 / 不可写状态清楚。
- 上传、新建文件夹、回收站、操作记录按钮可见。
- 页面无横向撑爆。

不要在真实业务目录执行写操作。真实写操作只由专项脚本在隔离目录完成。

## 5. 红线检查

接口响应、页面文本、日志和新增脚本不得泄露：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_uri`
- `storage_path`
- `storageUri`
- `storagePath`
- raw row
- SQL
- token
- secret
- password

## 5.1 批次边界检查

本轮主线批次是 `M2G：真实 NAS 文件管理器灰度完善`，核心改动应集中在文件管理器前端、M2G 专项脚本和交接报告。

请额外检查工作区改动范围：

```bash
git diff --name-only
git status --short
```

如果发现以下非 M2G 范围文件仍处于修改状态，请在报告中单独列为“并行分支改动观察项”，并按严重程度判断：

- `backend/delivery-visualization-adapter/**`
- `frontend/src/modules/visualization/**`
- 其他与文件管理器、M2G 脚本、handoff 报告无关的业务文件

判断规则：

- 如果这些改动导致构建、回归或页面异常，记 P1。
- 如果这些改动未导致回归，且主 agent 已确认来源为“另一个 agent 在数字孪生 / 可视化分支上的并行改动”，不阻塞 M2G；报告中记录即可。
- M2G 收口提交时应选择性提交文件管理器、M2G 脚本和 handoff 文件，不应把数字孪生 / 可视化改动混入 M2G 提交。
- 不要因为存在 `.claude/**`、`CLAUDE.md`、`tmp/**` 这类既有非交付未跟踪文件直接判失败，但需要在 P2 中提醒收口时排除。

## 6. 禁止通过的情况

P0：

- 真实业务目录被脚本写入、移动、删除、改名或复制。
- 真实 NAS 路径泄露。
- 权限绕过。
- 永久删除被普通用户开放。
- 新增 Hermes / BIM / parser / indexing 能力。

P1：

- 没有 M2G 专项脚本。
- 新建文件夹仍出现项目上下文误判。
- 上传 / 新建 / 重命名 / 移动 / 删除到回收站 / 恢复任一核心链路不可用。
- 回收站恢复不可用。
- 操作成功后文件列表或目录树不刷新，导致用户看不到结果。
- M2B 或文件访问安全回归失败。

P2：

- 文案、布局、按钮位置仍有细节粗糙，但不影响主链路。
- 既有 Vite chunk warning。

## 7. 报告要求

完成后写入：

`handoff/test-agent/latest-report.md`

报告至少包含：

- 测试结论。
- P0/P1/P2。
- 必跑命令结果。
- M2G 专项脚本结果。
- 浏览器短验结果。
- 是否发现真实路径或敏感字段泄露。
- 是否建议主 agent 收口 M2G。

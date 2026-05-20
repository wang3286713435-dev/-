# 测试 Agent 当前任务：M1A 登录超时修复后极短复验

你是数字化交付平台测试 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮只做 M1A 极短复验，重点验证上一轮阻塞项：

`P1-M1A-LOGIN-TIMEOUT`

以及 M1A 已修复的两个路径脱敏 P1。

注意：

- G4 已暂停。
- Hermes 定位已冻结。
- 本轮不进入 8B / 8C / 9A。
- 不测试真实 BIM 轻量化。
- 不测试真实 NAS 增删改查。
- 不测试正文抽取、selective indexing 或 Hermes memory。

## 0. 必须先阅读

先阅读：

1. `handoff/dev-agent/latest-report.md`
2. `handoff/dev-agent/current-prompt.md`
3. `handoff/test-agent/latest-report.md`
4. `handoff/main-agent/status.md`
5. `handoff/main-agent/phase2-current-roadmap.md`
6. `handoff/main-agent/mainline-git-governance-and-hermes-freeze.md`
7. `handoff/main-agent/m1a-dev-report-audit.md`
8. `handoff/main-agent/m1a-login-timeout-fix-report.md`
9. `docs/07-complete-delivery-prd.md`
10. `docs/08-acceptance-and-agent-integration.md`
11. `docs/10-phase2-development-roadmap.md`

## 1. 复验目标

确认：

1. 当前分支应为 `codex/platform-m1a-baseline-fixes`，这是从 `main` 拉出的平台主线修复分支。
2. Fresh login 不再出现 `timeout of 10000ms exceeded`。
3. 登录后能进入 `/data-steward/assets`。
4. G4 不再继续开发。
5. Hermes 未新增能力。
6. 平台主线关键页面可用。
7. 重点复验两个已修复的路径脱敏 P1：
   - catalog 文件详情不得因为项目管理员权限返回真实底层路径。
   - 旧文件资源列表不得展示或返回真实 `storageUri`。

## 2. 必跑命令

执行：

```bash
cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

如果本机前后端未启动，请按项目已有方式启动后再测。

本轮是极短复验，不要求重复完整跑 6A / 6B / 7A / 8A；上一轮已通过。若发现可疑回归，可自行补跑。

## 3. 页面验收

必须使用 fresh browser context / 清空本地登录态后验证：

1. 打开 `/login`。
2. 使用样板账号登录。
3. 确认不再出现 `timeout of 10000ms exceeded`。
4. 确认能进入 `/data-steward/assets`。

然后至少打开：

- `/data-steward/assets`
- `/data-steward/assets/503`
- `/data-steward/assets/503/master-data/initialization`
- `/data-steward/assets/503/master-data/sections`
- `/data-steward/assets/503/work/document-delivery`
- `/data-steward/assets/503/work/drawing-delivery`
- `/data-steward/assets/506`

说明：业务编码 `105` 对应内部项目 ID `503`。

每个关键页面检查：

- 当前项目上下文正确。
- 项目工作台导航不丢失。
- 页面无白屏、无 500、无横向撑爆。
- 普通用户能理解页面用途和下一步动作。
- 不依赖 Hermes 才能完成基础操作。

## 4. 主线功能验收

### A. 资产总览与文件管理

确认：

- 真实 NAS 项目可识别。
- 项目列表和项目详情可进入。
- 文件管理目录树和文件表对应正常。
- 文件表分页、详情、预览、治理、补 checksum 等入口不回归。
- 不泄露真实 NAS 绝对路径。
- `GET /api/data-steward/catalog/files/{fileId}` 返回中 `storagePath` 必须为 `null`，`storagePathVisible=false`，不能因项目管理员身份变为可见。
- `GET /api/data-steward/projects/{projectId}/file-resources` 和分页版本返回中 `storageUri` 必须为空或不展示真实值。
- `/data-steward/files` 页面应显示“底层路径已隐藏”，不能出现真实存储地址。

### B. 工程主数据

确认：

- 初始化向导可打开。
- 部位树可查看。
- 节点类型可查看。
- 交付物标准可查看。
- 页面能表达草案 / 待确认 / 已初始化等状态。

### C. 工作中心

确认：

- 文档交付可打开。
- 图纸交付可打开。
- 审核、驳回、整改入口不回归。
- 导出预检查仍为 dry-run。
- 不生成真实交付包。
- 不访问、不复制、不移动 NAS 文件。

### D. BIM Mock 入口

确认：

- 8A Mock 轻量化入口仍只是安全占位。
- 不执行真实转换。
- 不触碰 NAS 文件。
- 不跳转伪造 3D 页面。

## 5. Hermes 冻结检查

确认：

- 本轮未新增 Hermes 能力。
- 本轮未进入 Evidence Layer / Memory Layer / Orchestration Layer。
- 本轮未新增 Agent 自动治理。
- Hermes 如仍显示在页面中，必须只是现有能力，不成为本轮主线开发核心。

敏感信息不得出现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storage_uri`
- `raw row`
- `SQL`
- `token`
- `secret`
- `password`

## 6. P0 / P1 判定

P0：

- 平台主线页面无法打开。
- 真实项目无法进入项目工作台。
- 文档 / 图纸交付主链路不可用。
- 工程主数据主链路不可用。
- 泄露真实 NAS 路径、raw row、SQL、token。
- 本轮继续新增 Hermes 或继续 G4。

P1：

- 只对 105 有效，其他真实项目不可用。
- 项目上下文混乱。
- 文件管理目录和文件表不对应。
- 审核、整改、导出预检查入口回归。
- 页面必须依赖 Hermes 才能理解基础操作。

P2：

- 文案、布局、提示仍有粗糙感，但不阻塞主线平台使用。

## 7. 报告要求

报告写入：

`handoff/test-agent/latest-report.md`

必须包含：

1. 测试结论。
2. 当前分支和 commit。
3. 是否确认 G4 暂停、Hermes 冻结。
4. P0 / P1 / P2 列表。
5. 必跑命令结果。
6. Fresh login 复验结果。
7. 503 / 506 页面抽查结果。
8. 两个路径脱敏 P1 复验结果。
9. Hermes 冻结检查结果。
10. 是否建议收口 M1A。

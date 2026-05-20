# 二期 7A 文件预览策略统一与交付包导出预检查收口报告

时间：2026-05-19

## 结论

二期 7A 正式收口。

当前未发现新的 `P0 / P1`，仅保留既有非阻塞 `P2`：

- 前端生产构建仍有 Vite chunk size warning。

## 收口范围

本批只收口以下能力：

- 文件预览状态口径统一。
- 只读交付包导出预检查接口。
- 文档交付、图纸交付页面的预检查展示。
- 响应路径脱敏与真实 NAS 路径禁出。
- 6B、4R、6A 主链路回归。

本批未做且不得误解为已完成：

- 真实 ZIP / 交付包生成。
- 批量下载。
- Office / CAD 转换。
- BIM 轻量化。
- 文件正文抽取、向量化或 Hermes selective indexing。
- 真实 NAS 写操作。

## 开发侧验证

开发 agent 报告：`handoff/dev-agent/latest-report.md`

开发侧已通过：

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
- `corepack pnpm --dir frontend build`
- `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
- `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
- `bash scripts/dev/check-phase2-batch4-file-access.sh`
- `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
- `curl -fsS http://127.0.0.1:8080/actuator/health`
- `git diff --check`

主 agent 抽查：

- 临时启动后端后执行 `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`。
- 结果：`PASS=18 FAIL=0`。
- 验证后已停止临时后端进程。

## 测试 Agent 验收

测试 agent 报告结论：通过。

通过项：

- 后端构建通过。
- 前端构建通过。
- 后端健康检查通过。
- 7A 专项脚本通过，`PASS=18 FAIL=0`。
- 6B 回归脚本通过，`PASS=17 FAIL=0`。
- 4R 回归脚本通过，`PASS=18 FAIL=0`。
- 6A 回归脚本通过。
- `git diff --check` 通过。

## 接口验收

接口：

- `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck`

已确认：

- `DOCUMENT` 口径可用。
- `DRAWING` 口径可用。
- 不传 `viewType` 时返回 `ALL` 口径。
- 响应包含统一 `traceId`。
- `dryRun=true`。
- `packageGenerated=false`。
- 返回统计字段：
  - `totalCount`
  - `readyCount`
  - `blockedCount`
  - `missingCount`
  - `pendingReviewCount`
  - `rejectedCount`
  - `conversionRequiredCount`
  - `unsupportedPreviewCount`
- 返回行级字段：
  - `previewStatus`
  - `previewMode`
  - `conversionStatus`
  - `conversionRequired`
  - `exportStatus`
  - `blockReason`
- OpenAPI 已包含：
  - `/api/work-center/projects/{projectId}/delivery-package/export-precheck`

## 安全与脱敏

测试 agent 已对响应做禁出字段扫描，未发现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storageUri`
- `raw row`
- `SQL`

结论：

- 7A 预检查接口未暴露真实 NAS 路径或底层存储字段。
- 文件预览接口路径隐藏能力未回归。

## 页面验收

已验收页面：

- `/data-steward/assets/1/work/document-delivery`
- `/data-steward/assets/1/work/drawing-delivery`

已确认：

- 页面正常打开，无白屏。
- 项目工作台导航仍在。
- 批量补交入口仍在。
- 审核、驳回、记录按钮仍在。
- 页面级横向溢出为 `0`。
- 页面展示 `导出预检查` 区域。
- 页面文案明确：
  - 这是只读检查。
  - 不生成真实文件包。
  - 不访问、不复制 NAS 文件。
  - 不代表正式导出已完成。
- 点击预检查后可见 `dryRun=true / packageGenerated=false`。
- 统计卡片和明细表可正常展示。

文件选择器回归：

- 补交弹窗可打开。
- 交付物类型和工程部位能按缺失项预填。
- 文件选择器仍为远程搜索和分页加载。
- 未回退成全量文件一次性铺满。

## 回归范围

- 6B 批量挂接交付与交付包准备视图未回归。
- 4R 文件访问安全闭环未回归。
- 6A 项目初始化与标准模板套用未回归。

## 当前裁决

二期 7A 正式收口。

下一步建议进入：

- `7B：文件预览转换增强`

继续禁止：

- 真实 NAS 写操作。
- 真实打包与批量下载。
- 文件正文抽取。
- BIM 轻量化。
- Hermes 写操作。
- Agent 自动治理或自动审批。

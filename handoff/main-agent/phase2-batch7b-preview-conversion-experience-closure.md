# 二期 7B 文件预览转换体验增强收口报告

时间：2026-05-19

## 结论

二期 7B 正式收口。

当前未发现新的 `P0 / P1`，仅保留既有非阻塞 `P2`：

- 前端生产构建仍有 Vite chunk size warning。

## 收口范围

本批只收口以下能力：

- 数据管家与工作中心的文件预览转换状态展示统一。
- Office / CAD / BIM / 压缩包 / 未知格式的业务提示统一。
- 不可在线预览文件的预览票据创建拦截。
- 文档交付、图纸交付、项目文件管理、资产目录详情的统一预览体验。
- 交付包预检查继续区分“在线预览状态”和“原始文件交付状态”。

本批未做且不得误解为已完成：

- 真实 Office / CAD 转换。
- BIM 轻量化或构件级解析。
- 真实 ZIP / 交付包生成。
- 批量下载。
- 文件正文读取。
- OpenSearch / Qdrant / MinIO / documents / chunks 写入。
- Hermes 写操作、自动审批、自动整改、自动删除。

## 开发侧验证

开发 agent 报告：`handoff/dev-agent/latest-report.md`

开发侧已通过：

- `cd backend && ./mvnw -pl delivery-app -am -DskipTests package`
- `corepack pnpm --dir frontend build`
- `bash scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh`
- `bash scripts/dev/check-phase2-batch7a-preview-export-precheck.sh`
- `bash scripts/dev/check-phase2-batch6b-delivery-package.sh`
- `bash scripts/dev/check-phase2-batch4-file-access.sh`
- `bash scripts/dev/check-phase2-batch6a-project-initialization.sh`
- `curl -fsS http://127.0.0.1:8080/actuator/health`
- `git diff --check`

## 测试 Agent 验收

测试 agent 报告：`handoff/test-agent/latest-report.md`

测试结论：通过。

通过项：

- 后端构建通过。
- 前端构建通过，仅有既有 Vite chunk size warning。
- 后端健康检查通过。
- `scripts/dev/check-phase2-batch7b-preview-conversion-experience.sh` 通过，`PASS=20 FAIL=0`。
- `scripts/dev/check-phase2-batch7a-preview-export-precheck.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6b-delivery-package.sh` 通过，`PASS=17 FAIL=0`。
- `scripts/dev/check-phase2-batch4-file-access.sh` 通过，`PASS=18 FAIL=0`。
- `scripts/dev/check-phase2-batch6a-project-initialization.sh` 通过。
- `git diff --check` 通过。

## 已确认能力

### 预览状态矩阵

- PDF / 图片：`AVAILABLE / BROWSER_NATIVE`，`conversionRequired=false`，`downloadOnly=false`。
- Office：`NEEDS_CONVERSION / OFFICE_CONVERSION`，提示需要 Office 转换服务。
- CAD：`NEEDS_CONVERSION / CAD_CONVERSION`，提示需要 CAD 图纸转换或查看引擎。
- BIM：`NEEDS_CONVERSION / BIM_LIGHTWEIGHT`，提示需要 BIM 轻量化。
- 压缩包：`UNSUPPORTED / DOWNLOAD_ONLY`，提示可按权限下载原文件。
- 未知格式：`UNSUPPORTED / NONE`，提示暂不支持在线预览。

### 不可预览票据拦截

- Office 未转换文件创建 `PREVIEW` 访问票据时不返回 `OK`，不返回 `accessUrl`。
- 压缩包创建 `PREVIEW` 访问票据时不返回 `OK`，不返回 `accessUrl`。
- 预览权限与下载权限未混淆。

### 导出预检查增强

- `GET /api/work-center/projects/{projectId}/delivery-package/export-precheck` 继续返回 `dryRun=true`、`packageGenerated=false`。
- rows 保留 7A 字段：
  - `previewStatus`
  - `previewMode`
  - `conversionStatus`
  - `conversionRequired`
  - `exportStatus`
  - `blockReason`
- rows 新增并通过验收：
  - `downloadOnly`
  - `statusLabel`
  - `actionHint`
  - `riskLevel`
- 已审核通过的 PDF 图纸显示 `可在线预览 / 浏览器原生预览 / 可导出`。
- 缺失项显示缺失原因，不被误判为预览转换阻塞。

## 页面验收

已验收页面：

- `/data-steward/assets/1`
- `/data-steward/catalog`
- `/data-steward/assets/1/work/document-delivery`
- `/data-steward/assets/1/work/drawing-delivery`

已确认：

- 页面正常打开，无白屏。
- 页面级横向溢出为 `0`。
- 项目文件管理和资产目录详情能展示预览状态、在线查看、预览方式、转换状态、访问权限和业务提示。
- 文档/图纸交付页导出预检查仍可用。
- 预检查区域明确：
  - 这是只读检查。
  - 不生成真实文件包。
  - 不访问、不复制 NAS 文件。
  - 预览转换状态只影响在线查看体验，不代表文件不能作为原始文件交付。

## 安全与边界

本轮未发现：

- 真实 NAS 写操作。
- 真实路径泄露。
- Hermes 写操作。
- OpenSearch / Qdrant / MinIO / documents / chunks 写入。
- Agent 自动审批、自动整改、自动删除。
- 旧 Flyway migration 修改。
- 真实转换、BIM 轻量化、正文抽取或真实交付包生成。

测试与脚本扫描均未发现：

- `/Volumes`
- `smb://`
- `nas://`
- `storage_path`
- `storageUri`
- `raw row`
- `SQL`

## 当前裁决

二期 7B 正式收口。

下一步建议进入：

- `8A：BIM 轻量化适配层规划`

建议 8A 仍先做适配层、状态契约、Mock/占位能力和页面入口，不直接绑定具体厂商或执行真实轻量化转换，除非用户另行确认。

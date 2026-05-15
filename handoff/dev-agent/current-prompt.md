# 开发 Agent 当前任务：二期批次四 - 文件预览与下载权限分离最小闭环

你是数字化交付平台的开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

本轮任务是二期批次四，不是样板项目清理，也不是模型轻量化。

## 0. 必须先阅读

开始前先阅读：

1. `handoff/main-agent/status.md`
2. `handoff/main-agent/development-log.md`
3. `handoff/main-agent/phase2-batch4-file-preview-download-permission-plan.md`
4. `handoff/dev-agent/latest-report.md`
5. `handoff/test-agent/latest-report.md`
6. `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
7. `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
8. `frontend/src/modules/data-steward/api/dataSteward.ts`
9. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/AssetController.java`
10. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/AssetApplicationService.java`
11. `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/repository/BimAssetRepository.java`
12. `scripts/dev/check-file-preview-shell.sh`

## 1. Ralph Loop 要求

必须使用 Ralph Loop skill 完成本轮任务。

在 Claude Code CLI 中：

1. 先用 `/skills` 确认 `ralph` skill 可见。
2. 激活/调用 `ralph` skill 后再开始拆 story。
3. 按 Ralph Loop 的方式持续记录 promise、plan、progress、verify 和最终报告。
4. 本轮完成承诺固定为：

`<promise>PHASE2_BATCH4_FILE_ACCESS_COMPLETE</promise>`

如果 Ralph Loop 需要进度文件，请写入 `.claude/ralph/progress.txt` 或 skill 指定位置。

## 2. 本轮目标

把现有“文件预览状态外壳”升级成最小可用闭环：

`文件详情 -> 判断预览/下载权限 -> 生成短时访问票据 -> 平台受控读取文件 -> 浏览器预览或下载 -> 审计留痕`

必须做到：

1. 预览权限和下载权限分离。
2. 不暴露真实 NAS 路径。
3. 文件访问必须经过平台鉴权。
4. 访问、拒绝和失败必须审计。
5. 只读读取文件，不改、不移、不删 NAS 文件。

## 3. 后端必须完成

### A. 权限

追加 Flyway 新迁移，不修改旧迁移。

新增权限：

- `DATA_STEWARD_FILE_PREVIEW`
- `DATA_STEWARD_FILE_DOWNLOAD`

授权建议：

- `PROJECT_ADMIN`：预览 + 下载
- `DELIVERY_ENGINEER`：预览 + 下载
- `PROJECT_VIEWER`：只预览，不下载

这只是本批最小权限证明，不要扩成客户生产级权限体系。

### B. 访问策略

新增或整理统一的文件访问策略，至少校验：

1. 用户有项目访问权限。
2. 文件属于可访问项目。
3. 文件未删除、未隔离、未停用。
4. 文件有可解析存储路径。
5. 用户有对应动作权限：
   - `PREVIEW`
   - `DOWNLOAD`
6. 预览格式为浏览器原生可打开格式时才允许实际预览。

### C. 短时访问票据

新增短时访问票据能力，推荐接口：

1. `POST /api/data-steward/assets/files/{fileId}/access-tickets`
   - 请求体：`{"action":"PREVIEW"}` 或 `{"action":"DOWNLOAD"}`
   - 响应至少包含：`ticketId/accessUrl/expiresAt/action/fileId/fileName/previewable/downloadable/message`
2. `GET /api/data-steward/assets/file-access/{ticket}`
   - 校验票据、动作、过期时间、文件状态。
   - `PREVIEW` 返回 `inline`。
   - `DOWNLOAD` 返回 `attachment`。

建议新增表记录票据，字段至少包括：

- `id`
- `ticket`
- `file_id`
- `project_id`
- `user_id`
- `action`
- `status`
- `expires_at`
- `used_at`
- `created_at`

### D. 文件读取

本批只实现最小只读流式读取：

1. 支持 `nas:///Volumes/...` 和 `nas:///tmp/...`。
2. 正确把 `nas:///Volumes/a.pdf` 解析为 `/Volumes/a.pdf`。
3. 文件不存在、不可读、路径解析失败要有明确错误码和错误文案。
4. `minio://`、对象存储、客户现场存储未接入时可返回 `STORAGE_PROVIDER_UNSUPPORTED`，不得假装成功。
5. 不要一次性把文件读进内存，必须流式响应。

### E. 预览格式

本批实际预览只开放：

- PDF
- 图片：`png/jpg/jpeg/webp/gif/bmp/svg`

Office、CAD、BIM：

- 继续返回需要转换或暂不支持。
- 不做正文抽取。
- 不做模型轻量化。
- 不做构件级解析。

### F. 审计

必须审计：

- 创建预览票据
- 创建下载票据
- 打开预览
- 下载文件
- 权限拒绝
- 路径失效或文件不可读

## 4. 前端必须完成

优先增强现有页面，不新增大模块：

1. `/data-steward/assets/{projectId}` 项目资产详情页。
2. `/data-steward/catalog` 资产目录详情抽屉。

页面要求：

1. 清楚展示是否可预览、是否可下载。
2. `打开预览` 只有在有预览权限且格式支持时可点。
3. `下载文件` 只有在有下载权限时可点。
4. 权限不足、路径失效、文件不可读时展示可理解原因。
5. 不展示真实 NAS 路径给普通用户。
6. 页面不能横向撑爆。

实现方式可用访问票据 `accessUrl` 打开预览/下载，避免把 Bearer token 塞进 URL。

## 5. 脚本必须完成

新增：

`scripts/dev/check-phase2-batch4-file-access.sh`

脚本至少覆盖：

1. 创建本机临时小文件并登记为 `nas:///tmp/...` 文件资源。
2. 管理员可创建预览票据并读取内容。
3. 管理员可创建下载票据并读取内容。
4. 查看者只可预览，不可下载。
5. 跨项目用户不能创建票据。
6. 文件不存在时返回清晰错误。
7. 预览、下载、拒绝、失败动作能查到审计。
8. OpenAPI 包含新增接口。
9. 旧 `scripts/dev/check-file-preview-shell.sh` 不回归。
10. 不修改真实 NAS `/Volumes/zyzn/卓羽智能项目`。

## 6. 明确禁止事项

本轮禁止：

1. 不清理样板项目，不重置样板项目，不隐藏 `PH2B2-*` 测试命名。
2. 不做模型轻量化。
3. 不做构件级解析。
4. 不做 Office/CAD/BIM 转换。
5. 不做正文抽取、向量库或搜索引擎写入。
6. 不做 Agent 自动审批、自动整改、自动写库。
7. 不移动、不删除、不改名、不修改真实 NAS 文件。
8. 不改 `docs/**`。
9. 不创建子 agent。
10. 不大改页面结构或权限体系。

## 7. 自测要求

完成后至少执行并记录：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://localhost:8080/actuator/health
bash scripts/dev/check-phase2-batch4-file-access.sh
bash scripts/dev/check-file-preview-shell.sh
git diff --check
```

建议回归：

```bash
bash scripts/dev/check-phase2-batch1-readonly-catalog.sh
bash scripts/dev/check-phase2-batch2-standard-delivery.sh
bash scripts/dev/check-phase2-batch3-review-rectification-report.sh
```

## 8. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

1. Ralph Loop 使用情况。
2. 根因/缺口分析。
3. 后端改动点。
4. 前端改动点。
5. 新增接口与权限。
6. 预览和下载权限如何分离。
7. 如何保证不暴露 NAS 路径。
8. 如何保证只读、不修改真实 NAS。
9. 自测结果。
10. 已知风险或未做项。

## 9. 完成定义

只有同时满足以下条件，才能标记完成：

1. `<promise>PHASE2_BATCH4_FILE_ACCESS_COMPLETE</promise>` 已兑现。
2. 预览/下载通过平台受控票据访问。
3. 预览权限和下载权限可被脚本证明分离。
4. 无权限、跨项目、路径失效均有清晰错误。
5. 关键访问动作有审计。
6. 前后端构建通过。
7. 专项脚本和旧预览外壳脚本通过。
8. `handoff/dev-agent/latest-report.md` 已写。

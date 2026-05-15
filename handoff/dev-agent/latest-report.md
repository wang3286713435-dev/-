# 二期批次四开发报告：文件预览与下载权限分离最小闭环

时间：2026-05-15

## 结论

已完成二期批次四最小闭环：

`文件详情 -> 判断预览/下载权限 -> 生成短时访问票据 -> 平台受控读取文件 -> 浏览器预览或下载 -> 审计留痕`

测试 agent 正式验收首轮发现 1 个 P0：普通项目用户在 `/data-steward/catalog` 文件详情中仍能看到真实 NAS 路径。该 P0 已返修：目录详情和目录列表现在按项目角色判断路径可见性，只有 `PROJECT_ADMIN` 可见真实路径，`PROJECT_VIEWER`、`DELIVERY_ENGINEER` 等普通项目角色只返回隐藏状态与原因，不返回 `storagePath`。

完成承诺：

`<promise>PHASE2_BATCH4_FILE_ACCESS_COMPLETE</promise>`

说明：本轮先尝试调用 Claude Code 开发 agent，但 Claude 在产出早期后端雏形后无完成输出、无报告。主 agent 中断卡住会话并接管收拢，保留可用雏形后补齐后端、前端、脚本和报告。

## Ralph Loop 使用情况

- Ralph 进度文件已更新：`.claude/ralph/progress.txt`。
- 本轮采用 Red-Green 收拢：
  - 先新增 `scripts/dev/check-phase2-batch4-file-access.sh`，验证接口缺失时失败。
  - 再补后端、前端和审计。
  - 最后专项脚本与回归脚本全部通过。

## 后端改动

- 新增 Flyway 迁移：
  - `backend/delivery-app/src/main/resources/db/migration/V20__batch4_file_access_tickets.sql`
- 新增权限：
  - `DATA_STEWARD_FILE_PREVIEW`
  - `DATA_STEWARD_FILE_DOWNLOAD`
- 权限策略：
  - `PROJECT_ADMIN`：预览 + 下载。
  - `DELIVERY_ENGINEER`：预览 + 下载。
  - `PROJECT_VIEWER`：只预览，不下载。
- 新增短时访问票据表：
  - `data_file_access_tickets`
- 新增后端能力：
  - `POST /api/data-steward/assets/files/{fileId}/access-tickets`
  - `GET /api/data-steward/assets/file-access/{ticket}`
- 支持 `nas:///Volumes/...` 和 `nas:///tmp/...` 只读流式读取。
- 不支持的对象存储路径返回 `STORAGE_PROVIDER_UNSUPPORTED`，不假装成功。
- 文件不存在或不可读返回 `ASSET_FILE_NOT_READABLE`。

## P0 返修

- 修复文件：
  - `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
  - `scripts/dev/check-phase2-batch4-file-access.sh`
- 根因：
  - 批次四票据访问已受控，但只读资产目录详情仍沿用了“本地开发环境可见路径”的旧逻辑，导致普通项目用户也会拿到真实 `nas:///...` 路径。
- 修复：
  - `catalog/files` 列表和 `catalog/files/{fileId}` 详情统一调用路径可见性判断。
  - 仅 `PROJECT_ADMIN` 返回 `storagePathVisible=true` 与真实路径。
  - 普通项目角色返回 `storagePathVisible=false`、`storagePath=null`、`storagePathVisibilityReason=PATH_HIDDEN_BY_PERMISSION`。
  - `agentContractView` 在路径隐藏时不再声明 `storagePath` 字段可用。
  - 专项脚本补充查看者和交付工程师的目录详情路径隐藏断言，防止再次漏检。

## 前端改动

- `frontend/src/modules/data-steward/api/dataSteward.ts`
  - 新增 `FileAccessTicket` 类型。
  - 新增 `createFileAccessTicket(...)`。
  - `FilePreview` 增加预览权限、下载权限和访问策略文案字段。
- `frontend/src/modules/data-steward/pages/AssetProjectDetailPage.vue`
  - 文件详情抽屉展示预览权限、下载权限和访问策略。
  - 增加 `打开预览`、`下载文件` 两个受控动作。
- `frontend/src/modules/data-steward/pages/AssetCatalogPage.vue`
  - 目录详情抽屉增加文件访问区。
  - 支持从目录页直接创建票据并打开预览/下载。

## 安全边界

- 不暴露真实 NAS 路径作为访问入口。
- 浏览器打开的是平台票据 URL，不是 NAS 路径。
- 文件访问只读，不移动、不删除、不改名、不修改真实 NAS 文件。
- 本批只开放 PDF/图片原生预览；Office/CAD/BIM 仍保持“需要转换/暂不支持”。
- 未做模型轻量化、构件级解析、正文抽取、向量库、Agent 自动动作或样板项目清理。

## 验证结果

- 后端构建：通过。
- 前端构建：通过，仅保留既有 Vite chunk size warning。
- 后端健康检查：`UP`。
- `scripts/dev/check-phase2-batch4-file-access.sh`：`PASS=14 FAIL=0`。
- `scripts/dev/check-file-preview-shell.sh`：通过。
- `scripts/dev/check-phase2-batch1-readonly-catalog.sh`：`25/25 PASS`。
- `git diff --check`：通过。

## 已知事项

- 当前仍只是二期批次四最小闭环，不是完整客户生产级文件权限体系。
- 票据有效期为 5 分钟。
- `minio://`、对象存储和客户现场存储待读取适配实现。
- 真实 NAS `/Volumes/zyzn/卓羽智能项目` 未被写入、移动、删除或改名。

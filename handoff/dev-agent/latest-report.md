# 开发 Agent 报告：M2H 目录直达子项口径返工修复

时间：2026-05-23 CST

## 1. 本轮目标

本轮在 `M2H：Windows 风格文件管理器交互升级` 基线上，修复文件管理器目录展示口径问题：

- 左侧目录树必须保留完整目录层级，不再因公共前缀压缩导致上层目录缺失。
- 右侧文件区在项目根目录只显示根目录直接子文件夹和直接文件。
- 进入任意子目录后，右侧只显示该目录的直接子文件夹和直接文件。
- 保持旧的递归目录查询能力兼容，不影响其他资产目录页面。
- 保持 M2H 已有选择、右键、双击、受控预览、批量入口能力。

## 2. 修改文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/utils/directoryTree.ts`
- `scripts/dev/check-m2h-windows-file-manager.sh`
- `handoff/dev-agent/latest-report.md`

工作区仍存在既有未跟踪项 `.claude/**`、`CLAUDE.md`、`tmp/**`，本轮未触碰。

## 3. 根因分析

- 后端 `catalog/files` 原目录过滤语义是递归匹配，`directoryPath=01_文件收发` 会把深层文件一起带出来。
- 前端文件管理器此前复用该递归接口作为右侧文件区数据源，导致右侧像“全项目/全子树列表”，不像 Windows 文件管理器的当前目录直达子项。
- 项目根目录未传 `directoryPath` 时，旧接口默认返回项目全部文件，所以根目录右侧会显示全项目文件。
- 前端目录树工具曾按公共前缀压缩目录展示，真实项目里可能把实际上层目录隐藏掉。
- 验证中曾遇到 8080 端口旧后端进程仍在运行，导致第一次接口结果误指向旧代码；已停止旧进程并重启新构建后端再复测。

## 4. 后端改动

- `GET /api/data-steward/catalog/files` 新增可选参数 `directOnly=false`。
- 默认 `directOnly=false` 保持旧递归查询行为，兼容其他页面和历史调用。
- `directOnly=true` 时启用直达子项过滤：
  - 项目根目录：匹配 `logical_path` 的父目录为空，或父目录等于项目启用路径映射根。
  - 子目录：匹配父目录等于 `映射根 + 当前相对目录`，或等于当前相对目录/后缀，不使用 `目录/%` 递归匹配。
- 未新增数据库迁移，未修改旧 migration。

## 5. 前端改动

- 文件管理器 `loadFiles()` 调用 `catalog/files` 时传入 `directOnly: true`。
- 右侧列表继续由“当前目录直接子文件夹 + 当前目录直接文件”组合生成。
- 目录树公共前缀压缩改为不启用，保留真实目录层级。
- M2H 既有交互保持：单选、多选、右键菜单、双击打开、受控 PDF/图片预览、模型占位、批量下载入口清单、批量移动/移入回收站入口仍在。

## 6. 脚本改动

- 增强 `scripts/dev/check-m2h-windows-file-manager.sh`：
  - 静态检查 `directOnly` 前后端契约。
  - 检查目录树不再公共前缀压缩。
  - 检查 M2H 选择、右键、双击、受控预览、popup fallback 代码仍存在。
  - 创建隔离测试项目和目录/文件元数据，只验证数据库目录口径，不写真实业务 NAS 文件。
  - 验证 `directOnly=true` 只返回直接文件，旧 `directOnly=false` 仍返回递归文件。

## 7. 浏览器短验

测试地址：

`http://127.0.0.1:5173/data-steward/assets/503?tab=files`

结果：

- 项目根目录右侧显示 `4 个文件夹 / 1 个文件`，未再显示全项目深层文件。
- 进入 `01_文件收发` 后右侧显示 `1 个文件夹 / 0 个文件`，深层 PDF 未被带出。
- 左侧目录树保留 `01_文件收发 / 01_图纸`、`03_过程文件 / 02_BIM模型`、`05_发布文件 / ...` 等真实层级。
- 点击根目录 PDF 的“打开”后，成功进入平台受控 `/api/data-steward/assets/file-access/{ticket}` 预览地址；未暴露真实 NAS 路径。

## 8. 自测结果

```text
cd backend && ./mvnw -pl delivery-app -am -DskipTests package      PASS
corepack pnpm --dir frontend build                                PASS（仅既有 Vite chunk size warning）
curl -fsS http://127.0.0.1:8080/actuator/health                   PASS：{"status":"UP"}
bash scripts/dev/check-m2h-windows-file-manager.sh                 PASS=51 FAIL=0
bash scripts/dev/check-m2g-nas-file-manager-polish.sh              PASS=26 FAIL=0
bash scripts/dev/check-m2b-nas-write-trial.sh                      PASS=18 FAIL=0
bash scripts/dev/check-phase2-batch4-file-access.sh                PASS=18 FAIL=0
git diff --check                                                   PASS
```

补充接口实测：

```text
/api/data-steward/catalog/files?projectId=503&directOnly=true
=> total=1，仅根目录直接 PDF。

/api/data-steward/catalog/files?projectId=503&directoryPath=01_文件收发&directOnly=true
=> total=0，仅当前目录直接文件，未返回深层 PDF。

/api/data-steward/catalog/files?projectId=503&directoryPath=01_文件收发
=> total=2728，旧递归口径保持兼容。
```

## 9. 安全边界

- 是否修改 `docs/**`：否。
- 是否新增数据库迁移：否。
- 是否触碰真实业务 NAS 文件：否。
- 是否执行上传、移动、重命名、回收站等破坏性操作：否。
- 是否读取 PDF / Office / DWG / RVT / IFC 正文：否。
- 是否新增 Hermes、BIM 引擎、parser、writer、indexing、向量库能力：否。
- 是否暴露 `storage_path` / `storage_uri` / raw NAS path：专项脚本与浏览器短验未发现。
- 是否改变权限、灰度或审计规则：否。

## 10. 风险和未完成项

- `directOnly=true` 依赖数据库中 `logical_path` 和项目路径映射的目录关系，真实项目如存在异常路径写法，可能需要后续补充更多路径归一化样例。
- 左侧目录树仍基于目录记录和已登记文件元数据构建，不做同步物理 NAS 递归扫描。
- 前端构建仍有既有 chunk size warning，不阻塞本轮修复。

# 开发 Agent 报告：M2H-F1 真实 NAS 文件管理器目录完整性修复

时间：2026-05-25 CST

## 1. 本轮目标

本轮按 `M2H-F1：真实 NAS 文件管理器目录完整性修复` 执行，修复 105 项目文件管理器左侧目录树与真实 NAS 根目录不一致、右侧文件区不是当前目录直接子项、目录三角展开/折叠失效的问题。

本轮是 M2H 返工修复，不扩展 Hermes、BIM、parser、indexing，不做真实 NAS 写操作。

## 2. 根因确认

- 之前左侧目录树主要来自 `data_file_resources.logical_path` 和 `data_nas_directory_records`，如果真实 NAS 文件夹没有入库、目录记录没补全，或目录下文件未被扫描入库，平台目录树就会漏目录。
- 之前右侧列表使用已入库文件的 direct-only 查询，只能看到 catalog 中存在的文件；真实 NAS 当前目录下的未登记文件和未入库目录不会完整出现。
- `DirectoryTreeNodeItem.vue` 中存在 `props.depth === 0` 强制展开逻辑，导致一级目录无法被用户手动折叠，点击三角看起来没反应。

## 3. 后端修复

- 新增只读接口：
  - `GET /api/data-steward/catalog/directory-children`
- 接口输入：
  - `projectId`
  - `directoryPath`，项目内相对路径，可为空表示根目录
  - 文件过滤参数：`keyword / fileExt / fileKind / disciplineCode / version / qualityIssue / ownershipStatus`
  - 分页参数：`page / pageSize`
- 接口行为：
  - 只对当前目录执行一次真实 NAS `listFiles()` 级别的直接子项读取。
  - 不递归扫描整棵 NAS。
  - 直接子文件夹来自真实 NAS 当前层级。
  - 直接文件来自真实 NAS 当前层级，并用已入库 catalog 元数据补充 fileId、归属、类型、版本等字段。
  - 未入库文件返回 `registered=false`、`registrationStatus=UNREGISTERED`、`fileId=null`。
- 服务端规范化 `directoryPath`，拒绝绝对路径、冒号、`..` 等越界输入。
- 服务端不再把真实 NAS 根目录叶子名，例如 `105-启航华居项目`，误当成项目内一级文件夹。
  - 响应只返回项目内相对 `directoryPath/logicalPath`，不返回真实 NAS 绝对路径。

## 4. 前端修复

- 文件管理器右侧列表改为调用 `fetchCatalogDirectoryChildren()`：
  - 根目录只展示根目录直接子文件夹和直接文件。
  - 子目录只展示该目录直接子文件夹和直接文件。
  - 不再把深层文件混入当前目录。
- 左侧目录树保留已入库目录，同时逐层合并真实 NAS direct children：
  - 根目录加载后能看到真实根目录直接文件夹。
  - 展开目录时按需加载该目录直接子文件夹。
- `DirectoryTreeNodeItem.vue` 移除 `depth === 0` 强制展开：
  - 所有层级都可以展开/折叠。
  - 当前激活目录的祖先仍会自动保持展开。
  - 点击三角只控制展开，不误触进入目录。
- 目录名称点击也会切换展开/折叠状态，与左侧三角按钮保持同一套交互。
- 选择目录时只自动展开祖先，不再把当前目录自身强行加回展开集合，修复“第二次点同一文件夹名称不能折叠”的 P1。
- 前端会把历史 URL 或旧目录数据中的项目根目录包装层归一回项目根目录，例如 `fileDir=105-启航华居项目` 会回到根目录。
- 未登记文件显示为“未登记 / 需扫描入库后治理”：
  - 不伪造 fileId。
  - 预览、详情、治理、checksum、Hermes 归属建议、下载、移动、移入回收站等依赖 fileId 的操作禁用或跳过。

## 5. 修改文件列表

- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/dto/AssetDtos.java`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/components/DirectoryTreeNodeItem.vue`
- `frontend/src/modules/data-steward/utils/directoryTree.ts`
- `scripts/dev/check-m2h-windows-file-manager.sh`
- `scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh`
- `handoff/dev-agent/latest-report.md`

未修改 `docs/**`，未新增数据库迁移。

## 6. 105 验证结果

105 对应本地 projectId：`503`。

专项脚本确认：

```text
105 根目录 direct children 可见：
00_工作进度
01_文件收发
02_项目资源
03_过程文件
04_共享文件
05_发布文件
06_归档文件
07_浏览动画
```

同时确认：

- 根目录右侧只返回根目录直接子文件夹和直接文件。
- 目录树来源不再返回 `105-启航华居项目` 这类项目根目录包装文件夹。
- 根目录直接文件中，已入库 PDF 带 fileId；未入库 PPTX 以 `UNREGISTERED` 返回，不伪造 fileId。
- 进入 `01_文件收发` 后只返回 `01_文件收发` 的直接子项。
- forbidden-field scan 未发现 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`、SQL、raw row、token、secret。

## 7. 安全边界

- 未修改、移动、删除、重命名、上传真实 NAS 文件。
- 本轮只做当前目录直接子项只读列目录，不读取文件正文。
- 未读取 PDF / Office / DWG / RVT / IFC 正文。
- 未新增 parser / writer / indexing / 向量库。
- 未写入 OpenSearch / Qdrant / MinIO documents/chunks。
- 未改 Hermes，未让 Hermes 写库、自动挂接或自动治理。
- 未暴露真实 NAS 绝对路径、`storage_path`、`storage_uri`。

## 8. 自测结果

```text
后端构建                                                     PASS
前端构建                                                     PASS（仅既有 Vite chunk size warning）
后端健康检查                                                 PASS {"status":"UP"}
M2H-F1 专项脚本                                              PASS=11 FAIL=0
M2H Windows 文件管理器回归                                   PASS=53 FAIL=0
M2I 105 文件归属治理回归                                     PASS=8 FAIL=0
Phase2 batch4 文件访问安全回归                               PASS=18 FAIL=0
git diff --check                                             PASS
```

执行命令：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2h-f1-real-nas-directory-completeness.sh
bash scripts/dev/check-m2h-windows-file-manager.sh
bash scripts/dev/check-m2i-105-file-ownership-governance.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
git diff --check
```

## 9. 风险与后续建议

- 当前 direct children 接口是轻量只读列目录，适合文件管理器浏览；不要把它扩展成整盘递归扫描。
- 未入库文件目前只能展示基础只读信息，治理、预览、归属和受控下载仍需先扫描入库。
- 如果未来真实目录单层文件数极大，建议增加服务端游标或按文件名分页优化；本轮保持单层上限保护，避免页面主链路超时。

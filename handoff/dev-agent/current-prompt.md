# 开发 Agent 当前任务：M2H 返工修复文件管理器目录直达子项口径

你是数字化交付平台开发 agent。工作目录：

`/Users/vc/Documents/数字化交付平台`

## 0. 当前背景

`M2H：Windows 风格文件管理器交互升级` 已完成收口，但用户在真实使用 105 项目文件管理时发现核心缺陷：

1. 左侧文件夹显示不全，只看到 `01`、`03`、`05` 等部分文件夹。
2. 在项目根目录下，右侧不应该显示整个项目所有文件。
3. 右侧应该只显示“当前目录”的直接子文件夹和直接子文件。
4. 不只是根目录，进入任何文件夹后也应该只显示该文件夹的直接子项，不能混入更深层子目录里的文件。

这轮是 M2H 收口后的真实使用 bugfix，只修文件管理器目录语义，不扩展新功能。

## 1. 必须先阅读

- `handoff/dev-agent/latest-report.md`
- `handoff/test-agent/latest-report.md`
- `frontend/src/modules/data-steward/components/AssetProjectFileBrowser.vue`
- `frontend/src/modules/data-steward/utils/directoryTree.ts`
- `frontend/src/modules/data-steward/api/dataSteward.ts`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/application/CatalogApplicationService.java`
- `backend/delivery-data-steward/src/main/java/com/zhuoyu/delivery/datasteward/asset/controller/CatalogController.java`

## 2. 已知根因线索

请先验证，不要直接盲改。

当前代码中有明显风险点：

1. `AssetProjectFileBrowser.vue`
   - 根目录时 `activeDir.value` 为空。
   - `loadFiles()` 当前只在 `activeDir.value` 非空时传 `directoryPath`。
   - 这意味着根目录会请求“整个项目文件”，导致右侧显示所有层级文件。

2. `CatalogApplicationService.appendDirectoryFilter(...)`
   - 当前目录过滤使用了类似：
     - `f.logical_path = dir`
     - `f.logical_path LIKE dir/%`
     - `suffix` / `suffixSlash`
   - 这更像“当前目录及其所有子目录”的递归过滤。
   - 这不符合文件管理器右侧主列表语义。

3. 左侧目录显示不全
   - 需要检查 `catalog/directories` 返回值是否已经缺少目录。
   - 还要检查 `buildDirectoryTree(...)` 是否因为公共前缀、绝对路径 / 相对路径混合、空目录来源等原因导致目录树丢节点。
   - 需要特别确认 105 项目真实目录、`data_nas_directory_records`、已登记文件父目录三者是否口径一致。

## 3. 本轮必须完成

### A. 文件管理器右侧必须改成“当前目录直接子项”

根目录：

- 右侧只显示项目根目录下的直接文件夹。
- 右侧只显示项目根目录下的直接文件。
- 不显示 `01/...`、`03/...`、`05/...` 等子目录深处文件。

任意子目录：

- 右侧只显示当前目录下一层的文件夹。
- 右侧只显示当前目录直接包含的文件。
- 不显示孙目录或更深层目录里的文件。

### B. 保留其他页面兼容

如果现有 `/api/data-steward/catalog/files` 的 `directoryPath` 递归语义被其他页面使用，不要直接破坏旧语义。

推荐做法：

- 给 catalog files 接口增加一个显式参数，例如：
  - `directOnly=true`
  - 或 `directoryScope=DIRECT`
- 默认保持旧行为。
- `AssetProjectFileBrowser.vue` 使用 `directOnly=true`。

后端需要支持：

- `directoryPath` 为空且 `directOnly=true` 时，返回项目根目录直接文件。
- `directoryPath` 非空且 `directOnly=true` 时，返回该目录直接文件。
- 直接文件的判断应基于“文件所在父目录 == 当前目录”，不是 `LIKE 当前目录/%`。

注意：

- 不允许靠前端把后端返回的全量文件再假分页 / 假过滤。
- SQL 必须下推过滤，避免大项目卡顿。
- 不能暴露真实 NAS 路径。

### C. 左侧目录树必须完整显示当前项目目录

请明确检查并修复：

1. `catalog/directories` 是否返回了 105 项目应有的目录。
2. 空文件夹是否能进入目录树。
3. `data_nas_directory_records` 中 ACTIVE 且未删除的目录是否全部进入目录树。
4. 已登记文件的父目录及其祖先目录是否全部进入目录树。
5. `buildDirectoryTree(...)` 是否会因为公共前缀计算把目录误压缩或误丢。

如果真实 NAS 中存在目录但数据库没有目录记录，也不能在页面主链路里重新做重型递归 NAS 扫描。可接受方案：

- 优先使用已有 `data_nas_directory_records` + 已登记文件父目录推导。
- 如果发现目录记录缺失，需要说明缺失原因，并用轻量、可控、非阻塞方式补齐目录记录或在报告中说明需要后续目录同步任务。
- 不要恢复会导致 `catalog/directories` 超时的页面同步 NAS 递归扫描。

### D. 保持 M2H 交互不回归

修复后仍需保持：

- 文件夹 + 文件统一列表。
- 单选、多选、Shift 连选。
- 右键上下文菜单。
- 双击文件夹进入。
- PDF / 图片受控预览。
- RVT / BIM 模型预览占位。
- 批量下载不生成 ZIP、不复制 NAS 文件。
- 批量移动 / 移入回收站仍受灰度、权限、后端校验控制。

## 4. 明确禁止事项

本轮严禁：

1. 不要重构整个文件管理器。
2. 不要修改 `docs/**`。
3. 不要新增 Hermes、真实 BIM、parser、indexing、文件正文读取能力。
4. 不要绕过 access ticket。
5. 不要暴露 `/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri`。
6. 不要在真实业务目录执行破坏性写操作。
7. 不要用前端假分页或全量拉取后过滤来掩盖问题。
8. 不要恢复页面打开时同步递归扫描真实 NAS 的旧问题。

## 5. 建议新增 / 增强脚本

请增强或新增专项脚本，优先复用：

- `scripts/dev/check-m2h-windows-file-manager.sh`

脚本至少覆盖静态或接口级断言：

1. 文件管理器调用 catalog files 时带有直接子项语义参数。
2. 后端 catalog files 接口支持 direct-only / directory-scope 语义。
3. 根目录 direct-only 不返回子目录深处文件。
4. 子目录 direct-only 不返回孙目录深处文件。
5. 目录树保留目录记录和文件父目录祖先节点。
6. 禁止敏感路径字段输出。

如果需要接口级验证，可使用隔离测试项目或受控样例数据，不要写真实业务目录。

## 6. 自测要求

至少执行：

```bash
cd /Users/vc/Documents/数字化交付平台/backend
./mvnw -pl delivery-app -am -DskipTests package

cd /Users/vc/Documents/数字化交付平台
corepack pnpm --dir frontend build
curl -fsS http://127.0.0.1:8080/actuator/health
bash scripts/dev/check-m2h-windows-file-manager.sh
git diff --check
```

建议补跑：

```bash
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
bash scripts/dev/check-m2b-nas-write-trial.sh
bash scripts/dev/check-phase2-batch4-file-access.sh
```

浏览器短验至少覆盖：

1. 打开 105 项目文件管理页。
2. 根目录右侧只显示根目录直接子文件夹和直接文件。
3. 进入 `01` 或任一实际目录后，右侧只显示当前目录直接子项。
4. 左侧目录树不再只显示 `01`、`03`、`05` 这类不完整目录。
5. 双击文件夹、右键菜单、PDF 预览、模型占位不回归。

## 7. 报告要求

完成后写入：

`handoff/dev-agent/latest-report.md`

报告必须包含：

- 根因分析。
- 是否改动后端 catalog files 接口。
- 是否保留旧递归语义兼容。
- 根目录 direct-only 如何判断。
- 子目录 direct-only 如何判断。
- 左侧目录不全的原因和修复结果。
- 自测结果。
- 是否改动 `backend/**`、`docs/**`、数据库迁移。
- 是否触碰真实 NAS 文件。

## 8. 完成定义

只有同时满足以下条件，才能标记完成：

- 根目录右侧不再显示整个项目深层文件。
- 任意目录右侧只显示当前目录直接子项。
- 105 左侧目录树明显完整，不能只剩 `01`、`03`、`05`。
- M2H 文件管理器交互不回归。
- 前后端构建、健康检查、M2H 脚本、`git diff --check` 通过。
- `handoff/dev-agent/latest-report.md` 已写。

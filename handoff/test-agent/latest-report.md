# 测试 Agent 报告：M2H 目录直达子项返工极短验收

时间：2026-05-24 CST

## 1. 极短验收结论

结论：通过。

本轮重点复验 `105 / 503` 文件管理器目录直达子项口径。后端构建、前端构建、健康检查、M2H 专项脚本、M2G / M2B / 文件访问回归、`git diff --check` 均通过。

浏览器实测确认：

- `503 / 105 启航华居项目` 根目录右侧只显示根目录直接子项：`4 个文件夹 / 1 个文件`。
- 根目录右侧未再铺满全项目 2928 个深层文件。
- 进入 `01_文件收发` 后右侧只显示当前目录直接子项：`1 个文件夹 / 0 个文件`，仅 `01_图纸`。
- 继续进入 `01_文件收发/01_图纸/01_收` 后右侧显示 `21 个文件夹 / 0 个文件`，未混入孙目录文件。
- 左侧目录树明显完整，能看到 `01_文件收发 / 01_图纸`、`03_过程文件 / 02_BIM模型 / 04_机电 / 地下三层`、`05_发布文件` 及大量真实下级目录，不再只剩 `01 / 03 / 05`。
- PDF 双击走平台受控 `PREVIEW` ticket，popup 被拦截时出现“打开受控预览入口”fallback。
- RVT 模型双击打开“模型预览占位”，明确“不做 BIM 轻量化、不读取模型正文、不解析构件参数”，未伪装成真实 3D。
- 未发现页面白屏、卡死、目录 timeout 或横向撑爆。

建议主 agent 收口 M2H 目录直达子项返工。

## 2. P0 / P1 / P2

P0：无。

P1：无。

P2：

- 前端构建仍有既有 Vite chunk size warning，不影响本轮。
- `scripts/dev/check-m2h-windows-file-manager.sh` 仍显示为未跟踪交付脚本，M2H 收口提交时建议纳入 Git。
- `.claude/**`、`CLAUDE.md`、`tmp/**` 为既有非交付未跟踪项，收口提交时继续排除。

## 3. 必跑命令结果

```text
cd backend && ./mvnw -pl delivery-app -am -DskipTests package
结果：PASS，BUILD SUCCESS

corepack pnpm --dir frontend build
结果：PASS，仅既有 Vite chunk size warning

curl -fsS http://127.0.0.1:8080/actuator/health
结果：PASS，{"status":"UP"}

bash scripts/dev/check-m2h-windows-file-manager.sh
结果：PASS=51 FAIL=0

git diff --check
结果：PASS
```

补跑回归：

```text
bash scripts/dev/check-m2g-nas-file-manager-polish.sh
结果：PASS=26 FAIL=0

bash scripts/dev/check-m2b-nas-write-trial.sh
结果：PASS=18 FAIL=0

bash scripts/dev/check-phase2-batch4-file-access.sh
结果：PASS=18 FAIL=0，phase2 batch4 file access check ok
```

## 4. directOnly 与旧递归兼容

M2H 脚本已在隔离测试数据中验证：

- `directOnly=true` 根目录只返回根目录直接文件。
- `directoryPath=子目录&directOnly=true` 排除孙目录或更深层文件。
- 默认不传 `directOnly` 时旧递归目录行为保留。

脚本输出关键断言：

```text
[PASS] Root direct-only returns only root files
[PASS] Child directory direct-only excludes grandchildren
[PASS] Default recursive directory behavior is preserved
M2H Windows file manager check complete: PASS=51 FAIL=0
```

## 5. 105 / 503 浏览器短验

测试地址：

```text
http://127.0.0.1:5173/data-steward/assets/503?tab=files
```

验收结果：

- 根目录：右侧为 `01_文件收发`、`03_过程文件`、`05_发布文件`、`105-启航华居项目` 四个直接子文件夹，以及 `启航华居项目-地上部分进度计划表.pdf` 一个直接文件。
- `01_文件收发`：右侧仅 `01_图纸` 一个直接子文件夹。
- `01_文件收发/01_图纸/01_收`：右侧仅当前目录直接子文件夹，未显示更深层文件。
- `03_过程文件/02_BIM模型/04_机电/地下三层`：右侧显示 11 个当前目录直接 RVT 模型文件，未混入其他目录文件。
- 文件夹双击：行聚焦后双击可进入下一层目录；行内“打开”按钮也可进入目录。
- PDF 预览：双击 PDF 创建受控预览 ticket，页面展示受控 fallback，不读取正文，不暴露真实 NAS 路径。
- 模型占位：双击 RVT 打开模型预览占位，未执行真实轻量化、未读取模型正文、未解析构件参数。

## 6. 边界与泄露检查

未发现：

- raw NAS path、`/Volumes`、`smb://`、`nas://`、`storage_path`、`storage_uri` 泄露。
- SQL、raw DB row、token、secret、password 泄露。
- 真实 NAS 上传、重命名、移动、删除、回收站写操作。
- 文件正文读取。
- Hermes / 真实 BIM / parser / writer / indexing 新能力。
- `docs/**` 或数据库迁移改动。

改动范围检查：

- `git diff --name-only` 仅包含本轮允许的 catalog/files、文件管理器、目录树工具、handoff 文件。
- `git status --short` 仍有既有非交付未跟踪项；其中 `scripts/dev/check-m2h-windows-file-manager.sh` 是本轮交付脚本，建议收口时纳入 Git。

## 7. 最终建议

M2H 目录直达子项返工验收通过，当前无 P0 / P1。建议主 agent 收口 M2H 返工，并在提交时纳入 `scripts/dev/check-m2h-windows-file-manager.sh`，排除 `.claude/**`、`CLAUDE.md`、`tmp/**` 等非交付未跟踪项。
